package com.ecommerce.analyticsservice.repository;

import com.ecommerce.analyticsservice.entity.Metric;
import com.ecommerce.analyticsservice.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricRepository extends JpaRepository<Metric, UUID> {

    Optional<Metric> findByMetricDateAndMetricType(LocalDate metricDate, MetricType metricType);

    List<Metric> findByMetricDateBetweenAndMetricType(
            LocalDate from, LocalDate to, MetricType metricType);

    @Query("SELECT m FROM Metric m WHERE m.metricType = :type ORDER BY m.metricDate DESC LIMIT 1")
    Optional<Metric> findLatestByType(@Param("type") MetricType type);

    @Query("SELECT COALESCE(SUM(m.totalRevenue), 0) FROM Metric m WHERE m.metricDate BETWEEN :from AND :to AND m.metricType = :type")
    java.math.BigDecimal sumRevenueByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") MetricType type);

    @Query("SELECT COALESCE(SUM(m.totalOrders), 0) FROM Metric m WHERE m.metricDate BETWEEN :from AND :to AND m.metricType = :type")
    Integer sumOrdersByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") MetricType type);
}
