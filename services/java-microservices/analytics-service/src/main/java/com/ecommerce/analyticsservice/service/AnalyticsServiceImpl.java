package com.ecommerce.analyticsservice.service;

import com.ecommerce.analyticsservice.dto.*;
import com.ecommerce.analyticsservice.entity.Event;
import com.ecommerce.analyticsservice.entity.Metric;
import com.ecommerce.analyticsservice.entity.MetricType;
import com.ecommerce.analyticsservice.entity.ProductPerformance;
import com.ecommerce.analyticsservice.mapper.EventMapper;
import com.ecommerce.analyticsservice.repository.EventRepository;
import com.ecommerce.analyticsservice.repository.MetricRepository;
import com.ecommerce.analyticsservice.repository.ProductPerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final EventRepository eventRepository;
    private final MetricRepository metricRepository;
    private final ProductPerformanceRepository productPerformanceRepository;
    private final EventMapper eventMapper;

    @Override
    public DashboardSummaryResponse getDashboardSummary(LocalDate from, LocalDate to) {
        log.debug("Getting dashboard summary from {} to {}", from, to);

        List<Metric> metrics = metricRepository.findByMetricDateBetweenAndMetricType(
                from, to, MetricType.DAILY_SUMMARY);

        BigDecimal totalRevenue = metrics.stream()
                .map(Metric::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = metrics.stream()
                .mapToInt(Metric::getTotalOrders)
                .sum();

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgConversionRate = metrics.isEmpty() ? BigDecimal.ZERO :
                metrics.stream()
                        .map(Metric::getConversionRate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(metrics.size()), 4, RoundingMode.HALF_UP);

        BigDecimal avgCartAbandonmentRate = metrics.isEmpty() ? BigDecimal.ZERO :
                metrics.stream()
                        .map(Metric::getCartAbandonmentRate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(metrics.size()), 4, RoundingMode.HALF_UP);

        int activeUsers = metrics.stream()
                .mapToInt(Metric::getActiveUsers)
                .max().orElse(0);

        int newUsers = metrics.stream()
                .mapToInt(Metric::getNewUsers)
                .sum();

        int totalSessions = metrics.stream()
                .mapToInt(Metric::getTotalSessions)
                .sum();

        int totalPageViews = metrics.stream()
                .mapToInt(Metric::getTotalPageViews)
                .sum();

        // Calculate change percentages compared to previous period
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(periodDays);
        LocalDate prevTo = from.minusDays(1);

        BigDecimal prevRevenue = metricRepository.sumRevenueByDateRange(prevFrom, prevTo, MetricType.DAILY_SUMMARY);
        Integer prevOrders = metricRepository.sumOrdersByDateRange(prevFrom, prevTo, MetricType.DAILY_SUMMARY);

        BigDecimal revenueChangePercent = calculateChangePercent(totalRevenue, prevRevenue);
        int ordersChangePercent = prevOrders != null && prevOrders > 0
                ? ((totalOrders - prevOrders) * 100) / prevOrders
                : 0;

        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .conversionRate(avgConversionRate)
                .cartAbandonmentRate(avgCartAbandonmentRate)
                .activeUsers(activeUsers)
                .newUsers(newUsers)
                .totalSessions(totalSessions)
                .totalPageViews(totalPageViews)
                .revenueChangePercent(revenueChangePercent)
                .ordersChangePercent(ordersChangePercent)
                .build();
    }

    @Override
    public RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to) {
        log.debug("Getting revenue report from {} to {}", from, to);

        List<Metric> metrics = metricRepository.findByMetricDateBetweenAndMetricType(
                from, to, MetricType.DAILY_SUMMARY);

        BigDecimal totalRevenue = metrics.stream()
                .map(Metric::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = metrics.stream()
                .mapToInt(Metric::getTotalOrders)
                .sum();

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<RevenueReportResponse.RevenueDataPoint> dataPoints = metrics.stream()
                .map(m -> RevenueReportResponse.RevenueDataPoint.builder()
                        .date(m.getMetricDate())
                        .revenue(m.getTotalRevenue())
                        .orders(m.getTotalOrders())
                        .build())
                .collect(Collectors.toList());

        String period = from.toString() + " to " + to.toString();

        return RevenueReportResponse.builder()
                .period(period)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .dataPoints(dataPoints)
                .build();
    }

    @Override
    public PagedResponse<TopSellingProductResponse> getTopSellingProducts(LocalDate from, LocalDate to, int limit) {
        log.debug("Getting top selling products from {} to {}, limit {}", from, to, limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<ProductPerformance> topProducts = productPerformanceRepository
                .findTopSellingProducts(from, to, pageable);

        // Aggregate by productId
        Map<UUID, List<ProductPerformance>> grouped = topProducts.stream()
                .collect(Collectors.groupingBy(ProductPerformance::getProductId));

        List<TopSellingProductResponse> products = grouped.entrySet().stream()
                .map(entry -> {
                    List<ProductPerformance> perfs = entry.getValue();
                    int totalUnitsSold = perfs.stream().mapToInt(ProductPerformance::getUnitsSold).sum();
                    BigDecimal totalRev = perfs.stream().map(ProductPerformance::getRevenue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int totalPurchases = perfs.stream().mapToInt(ProductPerformance::getPurchases).sum();
                    int totalViews = perfs.stream().mapToInt(ProductPerformance::getViews).sum();
                    BigDecimal avgRating = perfs.stream()
                            .map(ProductPerformance::getAverageRating)
                            .filter(r -> r != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long ratingCount = perfs.stream()
                            .filter(p -> p.getAverageRating() != null)
                            .count();
                    BigDecimal finalRating = ratingCount > 0
                            ? avgRating.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP)
                            : null;

                    return TopSellingProductResponse.builder()
                            .productId(entry.getKey())
                            .totalUnitsSold(totalUnitsSold)
                            .totalRevenue(totalRev)
                            .totalPurchases(totalPurchases)
                            .totalViews(totalViews)
                            .averageRating(finalRating)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getTotalUnitsSold(), a.getTotalUnitsSold()))
                .limit(limit)
                .collect(Collectors.toList());

        return PagedResponse.<TopSellingProductResponse>builder()
                .content(products)
                .page(0)
                .size(limit)
                .totalElements(products.size())
                .totalPages(1)
                .last(true)
                .build();
    }

    @Override
    public UserActivityResponse getUserActivity(LocalDate from, LocalDate to) {
        log.debug("Getting user activity from {} to {}", from, to);

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        long activeUsers = eventRepository.countDistinctActiveUsers(fromDateTime, toDateTime);
        long totalSessions = eventRepository.countDistinctSessions(fromDateTime, toDateTime);
        long newRegistrations = eventRepository.countByEventTypeAndDateRange(
                "user.registered", fromDateTime, toDateTime);
        long totalEvents = eventRepository.countByEventTypeAndDateRange(
                "user.registered", fromDateTime, toDateTime)
                + eventRepository.countByEventTypeAndDateRange("user.login", fromDateTime, toDateTime)
                + eventRepository.countByEventTypeAndDateRange("user.updated", fromDateTime, toDateTime);

        // Count all events in the period for total events
        long allEvents = eventRepository.findByCreatedAtBetween(fromDateTime, toDateTime,
                PageRequest.of(0, 1)).getTotalElements();

        BigDecimal avgSessionsPerUser = activeUsers > 0
                ? BigDecimal.valueOf(totalSessions).divide(BigDecimal.valueOf(activeUsers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return UserActivityResponse.builder()
                .totalActiveUsers(activeUsers)
                .totalSessions(totalSessions)
                .newRegistrations(newRegistrations)
                .averageSessionsPerUser(avgSessionsPerUser)
                .totalEvents(allEvents)
                .build();
    }

    @Override
    public PagedResponse<EventResponse> getEvents(String eventType, LocalDate from, LocalDate to, int page, int size) {
        log.debug("Getting events - type: {}, from: {}, to: {}, page: {}, size: {}", eventType, from, to, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Event> eventPage;

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        if (eventType != null && fromDateTime != null && toDateTime != null) {
            eventPage = eventRepository.findByEventTypeAndCreatedAtBetween(eventType, fromDateTime, toDateTime, pageable);
        } else if (eventType != null) {
            eventPage = eventRepository.findByEventType(eventType, pageable);
        } else if (fromDateTime != null && toDateTime != null) {
            eventPage = eventRepository.findByCreatedAtBetween(fromDateTime, toDateTime, pageable);
        } else {
            eventPage = eventRepository.findAll(pageable);
        }

        List<EventResponse> content = eventMapper.toEventResponseList(eventPage.getContent());

        return PagedResponse.<EventResponse>builder()
                .content(content)
                .page(eventPage.getNumber())
                .size(eventPage.getSize())
                .totalElements(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
                .last(eventPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void processEvent(String eventType, String source, Map<String, Object> eventData) {
        log.debug("Processing event: type={}, source={}", eventType, source);

        UUID userId = extractUUID(eventData, "userId");
        UUID orderId = extractUUID(eventData, "orderId");
        UUID productId = extractUUID(eventData, "productId");

        Event event = Event.builder()
                .eventType(eventType)
                .eventSource(source)
                .userId(userId)
                .orderId(orderId)
                .productId(productId)
                .eventData(eventData)
                .createdAt(LocalDateTime.now())
                .build();

        eventRepository.save(event);

        // Update daily metrics
        updateDailyMetrics(eventType, eventData);

        // Update product performance if applicable
        if (productId != null) {
            updateProductPerformance(productId, eventType, eventData);
        }
    }

    private void updateDailyMetrics(String eventType, Map<String, Object> eventData) {
        LocalDate today = LocalDate.now();
        Metric metric = metricRepository.findByMetricDateAndMetricType(today, MetricType.DAILY_SUMMARY)
                .orElseGet(() -> Metric.builder()
                        .metricDate(today)
                        .metricType(MetricType.DAILY_SUMMARY)
                        .build());

        switch (eventType) {
            case "order.created":
                metric.setTotalOrders(metric.getTotalOrders() + 1);
                BigDecimal amount = extractBigDecimal(eventData, "totalAmount");
                if (amount != null) {
                    metric.setTotalRevenue(metric.getTotalRevenue().add(amount));
                    int orders = metric.getTotalOrders();
                    metric.setAverageOrderValue(
                            metric.getTotalRevenue().divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP));
                }
                break;
            case "user.registered":
                metric.setNewUsers(metric.getNewUsers() + 1);
                metric.setTotalUsers(metric.getTotalUsers() + 1);
                break;
            case "user.login":
                metric.setActiveUsers(metric.getActiveUsers() + 1);
                metric.setTotalSessions(metric.getTotalSessions() + 1);
                break;
            default:
                metric.setTotalPageViews(metric.getTotalPageViews() + 1);
                break;
        }

        metricRepository.save(metric);
    }

    private void updateProductPerformance(UUID productId, String eventType, Map<String, Object> eventData) {
        LocalDate today = LocalDate.now();
        ProductPerformance perf = productPerformanceRepository
                .findByProductIdAndMetricDate(productId, today)
                .orElseGet(() -> ProductPerformance.builder()
                        .productId(productId)
                        .metricDate(today)
                        .build());

        switch (eventType) {
            case "product.viewed":
                perf.setViews(perf.getViews() + 1);
                break;
            case "cart.item.added":
                perf.setAddToCartCount(perf.getAddToCartCount() + 1);
                break;
            case "order.created":
                perf.setPurchases(perf.getPurchases() + 1);
                Integer quantity = extractInteger(eventData, "quantity");
                if (quantity != null) {
                    perf.setUnitsSold(perf.getUnitsSold() + quantity);
                } else {
                    perf.setUnitsSold(perf.getUnitsSold() + 1);
                }
                BigDecimal revenue = extractBigDecimal(eventData, "amount");
                if (revenue != null) {
                    perf.setRevenue(perf.getRevenue().add(revenue));
                }
                break;
            default:
                break;
        }

        productPerformanceRepository.save(perf);
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private UUID extractUUID(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof UUID) return (UUID) value;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal extractBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
