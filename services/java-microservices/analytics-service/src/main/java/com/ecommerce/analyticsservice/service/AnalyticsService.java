package com.ecommerce.analyticsservice.service;

import com.ecommerce.analyticsservice.dto.*;

import java.time.LocalDate;

public interface AnalyticsService {

    DashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to);

    RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to);

    PagedResponse<TopSellingProductResponse> getTopSellingProducts(LocalDate from, LocalDate to, int limit);

    UserActivityResponse getUserActivity(LocalDate from, LocalDate to);

    PagedResponse<EventResponse> getEvents(String eventType, LocalDate from, LocalDate to, int page, int size);

    void processEvent(String eventType, String source, java.util.Map<String, Object> eventData);
}
