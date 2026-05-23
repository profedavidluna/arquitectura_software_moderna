package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Product Service - SOA Microservice
 * 
 * <p>This service is responsible for managing the product catalog in the e-commerce platform.
 * It follows SOA principles:</p>
 * <ul>
 *   <li><b>Loose Coupling</b>: Communicates with other services only through Kafka events</li>
 *   <li><b>Service Contract</b>: Exposes a well-defined REST API</li>
 *   <li><b>Service Autonomy</b>: Owns its own database and business logic</li>
 *   <li><b>Service Statelessness</b>: No session state between requests</li>
 * </ul>
 * 
 * @author SOA Architecture Course
 * @version 1.0.0
 */
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
