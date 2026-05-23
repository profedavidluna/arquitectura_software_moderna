package com.ecommerce.analyticsservice.controller;

import com.ecommerce.analyticsservice.dto.*;
import com.ecommerce.analyticsservice.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and reporting operations")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary", description = "Retrieves aggregated dashboard metrics for the specified date range")
    @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @Parameter(description = "Start date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("GET /api/v1/analytics/dashboard - from: {}, to: {}", from, to);
        return ResponseEntity.ok(analyticsService.getDashboardSummary(from, to));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue report", description = "Retrieves detailed revenue report with daily data points")
    @ApiResponse(responseCode = "200", description = "Revenue report retrieved")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @Parameter(description = "Start date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("GET /api/v1/analytics/revenue - from: {}, to: {}", from, to);
        return ResponseEntity.ok(analyticsService.getRevenueReport(from, to));
    }

    @GetMapping("/products/top-selling")
    @Operation(summary = "Get top selling products", description = "Retrieves top selling products ranked by units sold")
    @ApiResponse(responseCode = "200", description = "Top selling products retrieved")
    public ResponseEntity<PagedResponse<TopSellingProductResponse>> getTopSellingProducts(
            @Parameter(description = "Start date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Number of products to return")
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("GET /api/v1/analytics/products/top-selling - from: {}, to: {}, limit: {}", from, to, limit);
        return ResponseEntity.ok(analyticsService.getTopSellingProducts(from, to, limit));
    }

    @GetMapping("/users/activity")
    @Operation(summary = "Get user activity", description = "Retrieves user activity metrics for the specified date range")
    @ApiResponse(responseCode = "200", description = "User activity retrieved")
    public ResponseEntity<UserActivityResponse> getUserActivity(
            @Parameter(description = "Start date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("GET /api/v1/analytics/users/activity - from: {}, to: {}", from, to);
        return ResponseEntity.ok(analyticsService.getUserActivity(from, to));
    }

    @GetMapping("/events")
    @Operation(summary = "List events", description = "Retrieves paginated list of tracked events with optional filtering")
    @ApiResponse(responseCode = "200", description = "Events retrieved")
    public ResponseEntity<PagedResponse<EventResponse>> getEvents(
            @Parameter(description = "Filter by event type")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Start date (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/v1/analytics/events - type: {}, from: {}, to: {}, page: {}, size: {}",
                eventType, from, to, page, size);
        return ResponseEntity.ok(analyticsService.getEvents(eventType, from, to, page, size));
    }
}
