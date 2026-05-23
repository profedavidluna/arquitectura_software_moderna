package com.ecommerce.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory Service - SOA Microservice
 * 
 * <p>This service manages product stock levels and handles stock reservations
 * as part of the order saga. It is a key participant in the distributed
 * transaction pattern.</p>
 * 
 * <p><b>Saga Participant Role</b>:</p>
 * <ul>
 *   <li>Listens for order.created → Attempts to reserve stock</li>
 *   <li>Publishes stock.reserved or stock.insufficient</li>
 *   <li>Listens for order.cancelled → Releases reserved stock</li>
 *   <li>Listens for product.created → Creates initial inventory entry</li>
 * </ul>
 * 
 * <p><b>SOA Principles</b>:</p>
 * <ul>
 *   <li>Service Autonomy: Owns all inventory data and stock logic</li>
 *   <li>Statelessness: Each request is independent</li>
 *   <li>Discoverability: Health endpoint for service registry</li>
 * </ul>
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
