package com.ecommerce.inventoryservice.exception;

import java.util.UUID;

public class DuplicateInventoryException extends RuntimeException {

    public DuplicateInventoryException(UUID productId) {
        super("Inventory record already exists for product: " + productId);
    }
}
