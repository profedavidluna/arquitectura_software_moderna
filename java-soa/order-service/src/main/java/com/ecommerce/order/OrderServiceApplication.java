package com.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service - SOA Microservice
 * 
 * <p>This service manages the order lifecycle in the e-commerce platform.
 * It implements the <b>Saga Pattern</b> for distributed transactions:</p>
 * 
 * <ol>
 *   <li>User places an order → Order created with PENDING status</li>
 *   <li>OrderCreatedEvent published to Kafka ESB</li>
 *   <li>Inventory Service reserves stock (or reports insufficient)</li>
 *   <li>Order Service confirms or cancels based on inventory response</li>
 * </ol>
 * 
 * <p><b>SOA Principles Applied</b>:</p>
 * <ul>
 *   <li>Service Autonomy: Owns order data and order lifecycle logic</li>
 *   <li>Loose Coupling: Communicates via events, not direct calls</li>
 *   <li>Service Composability: Part of the larger order fulfillment saga</li>
 * </ul>
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
