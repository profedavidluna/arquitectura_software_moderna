import { DashboardData, RevenueReport } from '../models/Analytics';

export interface IAnalyticsService {
  processEvent(eventType: string, sourceService: string, payload: any): Promise<void>;
  getDashboard(): Promise<DashboardData>;
  getRevenueReport(days: number): Promise<RevenueReport>;
}
