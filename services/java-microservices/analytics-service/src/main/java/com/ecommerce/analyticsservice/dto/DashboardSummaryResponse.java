package com.ecommerce.analyticsservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private BigDecimal averageOrderValue;
    private BigDecimal conversionRate;
    private BigDecimal cartAbandonmentRate;
    private Integer activeUsers;
    private Integer newUsers;
    private Integer totalSessions;
    private Integer totalPageViews;
    private BigDecimal revenueChangePercent;
    private Integer ordersChangePercent;
}
