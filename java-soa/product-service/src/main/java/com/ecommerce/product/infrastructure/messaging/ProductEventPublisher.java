package com.ecommerce.product.infrastructure.messaging;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.infrastructure.messaging.events.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Product Event Publisher - Infrastructure Layer
 * 
 * <p><b>Observer Pattern</b>: This publisher sends events to the Kafka topic,
 * acting as the "Subject" in the Observer pattern. Any service subscribed to
 * the topic will receive the notification.</p>
 * 
 * <p><b>SOA - Enterprise Service Bus</b>: Kafka acts as the ESB in this architecture.
 * This publisher is the interface between the Product Service and the ESB.</p>
 * 
 * <p><b>Loose Coupling</b>: The Product Service doesn't know which services
 * consume its events. It simply publishes to a topic and moves on.</p>
 */
@Component
public class ProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);
    private static final String TOPIC_PRODUCT_CREATED = "product.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a ProductCreatedEvent to the Kafka topic.
     * 
     * <p>The event is published asynchronously. The product ID is used as the
     * message key to ensure all events for the same product go to the same partition,
     * maintaining ordering guarantees.</p>
     *
     * @param product the product that was created
     */
    public void publishProductCreated(Product product) {
        ProductCreatedEvent event = ProductCreatedEvent.create(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getSku()
        );

        String key = product.getId().toString();

        log.info("Publishing ProductCreatedEvent: eventId={}, productId={}",
                event.getEventId(), product.getId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_PRODUCT_CREATED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: topic={}, partition={}, offset={}",
                        TOPIC_PRODUCT_CREATED,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: topic={}, error={}",
                        TOPIC_PRODUCT_CREATED, ex.getMessage(), ex);
            }
        });
    }
}
