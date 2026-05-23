package com.ecommerce.analyticsservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportResponse {

    private String period;
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private BigDecimal averageOrderValue;
    private List<RevenueDataPoint> dataPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueDataPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private Integer orders;
    }
}
