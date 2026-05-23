export interface AnalyticsEvent {
  id: string;
  eventType: string;
  sourceService: string;
  payload: any;
  createdAt: Date;
}

export interface DailyMetrics {
  id: string;
  metricDate: Date;
  totalOrders: number;
  totalRevenue: number;
  newUsers: number;
  activeCarts: number;
  updatedAt: Date;
}

export interface ProductMetrics {
  id: string;
  productId: string;
  totalViews: number;
  totalPurchases: number;
  totalRevenue: number;
  avgRating: number;
  updatedAt: Date;
}

export interface DashboardData {
  todayMetrics: DailyMetrics | null;
  recentEvents: AnalyticsEvent[];
  topProducts: ProductMetrics[];
  totalEventsProcessed: number;
}

export interface RevenueReport {
  daily: { date: string; revenue: number; orders: number }[];
  totalRevenue: number;
  totalOrders: number;
  averageOrderValue: number;
}
