package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.entity.Payment;
import com.ecommerce.orderservice.entity.PaymentStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.PaymentRepository;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrderEventPublisher eventPublisher;

    @KafkaListener(topics = "payment.processed", groupId = "order-service-group")
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment.processed event for order: {}", event.getOrderId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

        // Record payment
        Payment payment = Payment.builder()
                .order(order)
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .paymentMethod(event.getPaymentMethod())
                .transactionId(event.getTransactionId())
                .status("COMPLETED".equals(event.getStatus()) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED)
                .paidAt(event.getProcessedAt())
                .build();
        paymentRepository.save(payment);

        // Advance order status if payment successful
        if ("COMPLETED".equals(event.getStatus()) && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            eventPublisher.publishOrderConfirmed(order);
            log.info("Order {} confirmed after payment", order.getOrderNumber());
        } else if ("FAILED".equals(event.getStatus())) {
            log.warn("Payment failed for order {}. Initiating cancellation.", order.getOrderNumber());
            orderService.cancelOrder(order.getId(), "Payment failed", null);
        }
    }

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received inventory.reserved event for order: {}", event.getOrderId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

        if (event.isReserved()) {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                log.info("Order {} moved to PROCESSING after inventory reserved", order.getOrderNumber());
            }
        } else {
            log.warn("Inventory reservation failed for order {}: {}", order.getOrderNumber(), event.getReason());
            orderService.cancelOrder(order.getId(), "Inventory reservation failed: " + event.getReason(), null);
        }
    }
}
