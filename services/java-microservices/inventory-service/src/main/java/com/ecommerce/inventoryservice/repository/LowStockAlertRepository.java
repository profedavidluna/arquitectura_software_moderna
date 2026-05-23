package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.AlertStatus;
import com.ecommerce.inventoryservice.entity.LowStockAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, UUID> {

    List<LowStockAlert> findByAlertStatus(AlertStatus alertStatus);

    Page<LowStockAlert> findByAlertStatusIn(List<AlertStatus> statuses, Pageable pageable);

    Optional<LowStockAlert> findByProductIdAndAlertStatus(UUID productId, AlertStatus alertStatus);

    boolean existsByProductIdAndAlertStatus(UUID productId, AlertStatus alertStatus);

    List<LowStockAlert> findByProductId(UUID productId);
}
