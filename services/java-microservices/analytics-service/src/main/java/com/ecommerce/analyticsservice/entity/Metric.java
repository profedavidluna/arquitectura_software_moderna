package com.ecommerce.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "metrics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"metric_date", "metric_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "metric_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MetricType metricType;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_users", nullable = false)
    @Builder.Default
    private Integer totalUsers = 0;

    @Column(name = "new_users", nullable = false)
    @Builder.Default
    private Integer newUsers = 0;

    @Column(name = "active_users", nullable = false)
    @Builder.Default
    private Integer activeUsers = 0;

    @Column(name = "average_order_value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal averageOrderValue = BigDecimal.ZERO;

    @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Column(name = "cart_abandonment_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal cartAbandonmentRate = BigDecimal.ZERO;

    @Column(name = "total_page_views", nullable = false)
    @Builder.Default
    private Integer totalPageViews = 0;

    @Column(name = "total_sessions", nullable = false)
    @Builder.Default
    private Integer totalSessions = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
