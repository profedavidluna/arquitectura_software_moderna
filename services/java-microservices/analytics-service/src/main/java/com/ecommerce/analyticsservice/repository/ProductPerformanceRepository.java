package com.ecommerce.analyticsservice.repository;

import com.ecommerce.analyticsservice.entity.ProductPerformance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductPerformanceRepository extends JpaRepository<ProductPerformance, UUID> {

    Optional<ProductPerformance> findByProductIdAndMetricDate(UUID productId, LocalDate metricDate);

    List<ProductPerformance> findByProductIdAndMetricDateBetween(
            UUID productId, LocalDate from, LocalDate to);

    @Query("SELECT pp FROM ProductPerformance pp WHERE pp.metricDate BETWEEN :from AND :to " +
            "ORDER BY pp.unitsSold DESC")
    List<ProductPerformance> findTopSellingProducts(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("SELECT pp FROM ProductPerformance pp WHERE pp.metricDate BETWEEN :from AND :to " +
            "ORDER BY pp.revenue DESC")
    List<ProductPerformance> findTopRevenueProducts(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
