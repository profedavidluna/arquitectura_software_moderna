/**
 * Analytics Service Implementation
 * 
 * Aggregates events from all services into meaningful metrics.
 * Demonstrates the CQRS read-model pattern: events are the source of truth,
 * and this service builds optimized read models (metrics) from them.
 */
import { v4 as uuidv4 } from 'uuid';
import { DashboardData, RevenueReport } from '../domain/models/Analytics';
import { IAnalyticsService } from '../domain/interfaces/IAnalyticsService';
import { AnalyticsRepository } from '../infrastructure/persistence/AnalyticsRepository';

export class AnalyticsServiceImpl implements IAnalyticsService {
  private eventsProcessed = 0;

  constructor(private readonly analyticsRepository: AnalyticsRepository) {}

  async processEvent(eventType: string, sourceService: string, payload: any): Promise<void> {
    this.eventsProcessed++;

    // Store raw event
    await this.analyticsRepository.saveEvent({
      id: uuidv4(),
      eventType,
      sourceService,
      payload,
    });

    // Update aggregated metrics based on event type
    const today = new Date().toISOString().split('T')[0];

    switch (eventType) {
      case 'ORDER_CONFIRMED':
        await this.analyticsRepository.upsertDailyMetrics(today, {
          totalOrders: 1,
          totalRevenue: payload.totalAmount || 0,
        });
        // Update product metrics for each item
        if (payload.items) {
          for (const item of payload.items) {
            await this.analyticsRepository.upsertProductMetrics(item.productId, {
              purchases: item.quantity || 1,
              revenue: (item.unitPrice || 0) * (item.quantity || 1),
            });
          }
        }
        break;

      case 'USER_CREATED':
        await this.analyticsRepository.upsertDailyMetrics(today, { newUsers: 1 });
        break;

      case 'PAYMENT_REFUNDED':
        await this.analyticsRepository.upsertDailyMetrics(today, {
          totalRevenue: -(payload.amount || 0),
        });
        break;
    }

    if (this.eventsProcessed % 100 === 0) {
      console.log(`[Analytics] Processed ${this.eventsProcessed} events total`);
    }
  }

  async getDashboard(): Promise<DashboardData> {
    const [todayMetrics, recentEvents, topProducts, totalEvents] = await Promise.all([
      this.analyticsRepository.getTodayMetrics(),
      this.analyticsRepository.getRecentEvents(20),
      this.analyticsRepository.getTopProducts(10),
      this.analyticsRepository.getEventCount(),
    ]);

    return {
      todayMetrics,
      recentEvents,
      topProducts,
      totalEventsProcessed: totalEvents,
    };
  }

  async getRevenueReport(days: number): Promise<RevenueReport> {
    const metrics = await this.analyticsRepository.getMetricsRange(days);

    const daily = metrics.map(m => ({
      date: m.metricDate.toISOString().split('T')[0],
      revenue: m.totalRevenue,
      orders: m.totalOrders,
    }));

    const totalRevenue = daily.reduce((sum, d) => sum + d.revenue, 0);
    const totalOrders = daily.reduce((sum, d) => sum + d.orders, 0);

    return {
      daily,
      totalRevenue,
      totalOrders,
      averageOrderValue: totalOrders > 0 ? totalRevenue / totalOrders : 0,
    };
  }
}
