package com.ecommerce.analyticsservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponse {

    private Long totalActiveUsers;
    private Long totalSessions;
    private Long newRegistrations;
    private BigDecimal averageSessionsPerUser;
    private Long totalEvents;
}
