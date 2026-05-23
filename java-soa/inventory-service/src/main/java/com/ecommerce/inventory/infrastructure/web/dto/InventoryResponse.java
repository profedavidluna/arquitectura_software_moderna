package com.ecommerce.inventory.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        String productName,
        int quantityAvailable,
        int quantityReserved,
        LocalDateTime updatedAt
) {}
