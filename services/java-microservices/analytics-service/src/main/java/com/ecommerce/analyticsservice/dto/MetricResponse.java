package com.ecommerce.analyticsservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricResponse {

    private LocalDate metricDate;
    private String metricType;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private Integer totalUsers;
    private Integer newUsers;
    private Integer activeUsers;
    private BigDecimal averageOrderValue;
    private BigDecimal conversionRate;
    private BigDecimal cartAbandonmentRate;
    private Integer totalPageViews;
    private Integer totalSessions;
}
