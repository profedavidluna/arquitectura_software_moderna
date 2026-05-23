package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    Optional<Inventory> findBySku(String sku);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderPoint")
    List<Inventory> findLowStockItems();

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderPoint")
    Page<Inventory> findLowStockItems(Pageable pageable);

    List<Inventory> findByWarehouseLocation(String warehouseLocation);

    @Query("SELECT i FROM Inventory i WHERE i.warehouseLocation = :location")
    Page<Inventory> findByWarehouseLocation(@Param("location") String warehouseLocation, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable = 0")
    List<Inventory> findOutOfStockItems();

    boolean existsByProductId(UUID productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIdIn(@Param("productIds") List<UUID> productIds);
}
