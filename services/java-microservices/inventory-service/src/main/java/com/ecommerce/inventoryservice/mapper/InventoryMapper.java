package com.ecommerce.inventoryservice.mapper;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import com.ecommerce.inventoryservice.entity.LowStockAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InventoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantityReserved", constant = "0")
    @Mapping(target = "lastRestockedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Inventory toEntity(CreateInventoryRequest request);

    @Mapping(target = "totalQuantity", expression = "java(inventory.getQuantityAvailable() + inventory.getQuantityReserved())")
    @Mapping(target = "stockStatus", expression = "java(computeStockStatus(inventory))")
    InventoryResponse toResponse(Inventory inventory);

    TransactionResponse toTransactionResponse(InventoryTransaction transaction);

    LowStockAlertResponse toAlertResponse(LowStockAlert alert);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "quantityAvailable", ignore = true)
    @Mapping(target = "quantityReserved", ignore = true)
    @Mapping(target = "lastRestockedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdateInventoryRequest request, @MappingTarget Inventory inventory);

    default String computeStockStatus(Inventory inventory) {
        if (inventory.getQuantityAvailable() <= 0) {
            return "OUT_OF_STOCK";
        } else if (inventory.getQuantityAvailable() <= inventory.getReorderPoint()) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }
}
