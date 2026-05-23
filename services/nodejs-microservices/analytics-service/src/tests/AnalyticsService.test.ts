import { AnalyticsServiceImpl } from '../application/AnalyticsServiceImpl';
import { AnalyticsRepository } from '../infrastructure/persistence/AnalyticsRepository';

jest.mock('../infrastructure/persistence/AnalyticsRepository');

describe('AnalyticsServiceImpl', () => {
  let service: AnalyticsServiceImpl;
  let mockRepo: jest.Mocked<AnalyticsRepository>;

  beforeEach(() => {
    mockRepo = new AnalyticsRepository() as jest.Mocked<AnalyticsRepository>;
    service = new AnalyticsServiceImpl(mockRepo);
  });

  afterEach(() => jest.clearAllMocks());

  describe('processEvent', () => {
    it('should save event to repository', async () => {
      mockRepo.saveEvent.mockResolvedValue({
        id: 'evt-1', eventType: 'ORDER_CONFIRMED', sourceService: 'order-service',
        payload: { orderId: 'order-1' }, createdAt: new Date(),
      });
      mockRepo.upsertDailyMetrics.mockResolvedValue();

      await service.processEvent('ORDER_CONFIRMED', 'order-service', { orderId: 'order-1', totalAmount: 100 });

      expect(mockRepo.saveEvent).toHaveBeenCalledWith(expect.objectContaining({
        eventType: 'ORDER_CONFIRMED',
        sourceService: 'order-service',
      }));
    });

    it('should update daily metrics for ORDER_CONFIRMED', async () => {
      mockRepo.saveEvent.mockResolvedValue({
        id: 'evt-1', eventType: 'ORDER_CONFIRMED', sourceService: 'order-service',
        payload: {}, createdAt: new Date(),
      });
      mockRepo.upsertDailyMetrics.mockResolvedValue();

      await service.processEvent('ORDER_CONFIRMED', 'order-service', { totalAmount: 150 });

      expect(mockRepo.upsertDailyMetrics).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({ totalOrders: 1, totalRevenue: 150 })
      );
    });

    it('should update daily metrics for USER_CREATED', async () => {
      mockRepo.saveEvent.mockResolvedValue({
        id: 'evt-1', eventType: 'USER_CREATED', sourceService: 'user-service',
        payload: {}, createdAt: new Date(),
      });
      mockRepo.upsertDailyMetrics.mockResolvedValue();

      await service.processEvent('USER_CREATED', 'user-service', { email: 'new@user.com' });

      expect(mockRepo.upsertDailyMetrics).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({ newUsers: 1 })
      );
    });
  });

  describe('getDashboard', () => {
    it('should aggregate dashboard data', async () => {
      mockRepo.getTodayMetrics.mockResolvedValue({
        id: 'dm-1', metricDate: new Date(), totalOrders: 10,
        totalRevenue: 1500, newUsers: 3, activeCarts: 5, updatedAt: new Date(),
      });
      mockRepo.getRecentEvents.mockResolvedValue([]);
      mockRepo.getTopProducts.mockResolvedValue([]);
      mockRepo.getEventCount.mockResolvedValue(500);

      const dashboard = await service.getDashboard();

      expect(dashboard.todayMetrics?.totalOrders).toBe(10);
      expect(dashboard.totalEventsProcessed).toBe(500);
    });
  });

  describe('getRevenueReport', () => {
    it('should calculate revenue report', async () => {
      mockRepo.getMetricsRange.mockResolvedValue([
        { id: '1', metricDate: new Date('2024-01-01'), totalOrders: 5, totalRevenue: 500, newUsers: 2, activeCarts: 0, updatedAt: new Date() },
        { id: '2', metricDate: new Date('2024-01-02'), totalOrders: 8, totalRevenue: 800, newUsers: 1, activeCarts: 0, updatedAt: new Date() },
      ]);

      const report = await service.getRevenueReport(7);

      expect(report.totalRevenue).toBe(1300);
      expect(report.totalOrders).toBe(13);
      expect(report.averageOrderValue).toBe(100);
    });
  });
});
