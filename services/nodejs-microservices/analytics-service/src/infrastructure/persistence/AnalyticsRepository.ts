import { pool } from './database';
import { AnalyticsEvent, DailyMetrics, ProductMetrics } from '../../domain/models/Analytics';

export class AnalyticsRepository {
  async saveEvent(event: Omit<AnalyticsEvent, 'createdAt'>): Promise<AnalyticsEvent> {
    const query = `
      INSERT INTO events (id, event_type, source_service, payload)
      VALUES ($1, $2, $3, $4)
      RETURNING *
    `;
    const result = await pool.query(query, [event.id, event.eventType, event.sourceService, JSON.stringify(event.payload)]);
    return this.mapToEvent(result.rows[0]);
  }

  async getRecentEvents(limit: number): Promise<AnalyticsEvent[]> {
    const result = await pool.query('SELECT * FROM events ORDER BY created_at DESC LIMIT $1', [limit]);
    return result.rows.map(this.mapToEvent);
  }

  async getEventCount(): Promise<number> {
    const result = await pool.query('SELECT COUNT(*) as count FROM events');
    return parseInt(result.rows[0].count);
  }

  // Daily Metrics
  async upsertDailyMetrics(date: string, updates: Partial<DailyMetrics>): Promise<void> {
    const existing = await pool.query('SELECT * FROM daily_metrics WHERE metric_date = $1', [date]);

    if (existing.rows.length > 0) {
      const setClauses: string[] = ['updated_at = CURRENT_TIMESTAMP'];
      const values: any[] = [];
      let idx = 1;

      if (updates.totalOrders !== undefined) { setClauses.push(`total_orders = total_orders + $${idx++}`); values.push(updates.totalOrders); }
      if (updates.totalRevenue !== undefined) { setClauses.push(`total_revenue = total_revenue + $${idx++}`); values.push(updates.totalRevenue); }
      if (updates.newUsers !== undefined) { setClauses.push(`new_users = new_users + $${idx++}`); values.push(updates.newUsers); }

      values.push(date);
      await pool.query(`UPDATE daily_metrics SET ${setClauses.join(', ')} WHERE metric_date = $${idx}`, values);
    } else {
      const { v4: uuidv4 } = require('uuid');
      await pool.query(
        `INSERT INTO daily_metrics (id, metric_date, total_orders, total_revenue, new_users) VALUES ($1, $2, $3, $4, $5)`,
        [uuidv4(), date, updates.totalOrders || 0, updates.totalRevenue || 0, updates.newUsers || 0]
      );
    }
  }

  async getTodayMetrics(): Promise<DailyMetrics | null> {
    const today = new Date().toISOString().split('T')[0];
    const result = await pool.query('SELECT * FROM daily_metrics WHERE metric_date = $1', [today]);
    return result.rows.length > 0 ? this.mapToDailyMetrics(result.rows[0]) : null;
  }

  async getMetricsRange(days: number): Promise<DailyMetrics[]> {
    const result = await pool.query(
      'SELECT * FROM daily_metrics WHERE metric_date >= CURRENT_DATE - $1 ORDER BY metric_date DESC',
      [days]
    );
    return result.rows.map(this.mapToDailyMetrics);
  }

  // Product Metrics
  async upsertProductMetrics(productId: string, updates: { purchases?: number; revenue?: number }): Promise<void> {
    const existing = await pool.query('SELECT * FROM product_metrics WHERE product_id = $1', [productId]);

    if (existing.rows.length > 0) {
      await pool.query(
        `UPDATE product_metrics SET total_purchases = total_purchases + $1, total_revenue = total_revenue + $2, updated_at = CURRENT_TIMESTAMP WHERE product_id = $3`,
        [updates.purchases || 0, updates.revenue || 0, productId]
      );
    } else {
      const { v4: uuidv4 } = require('uuid');
      await pool.query(
        `INSERT INTO product_metrics (id, product_id, total_purchases, total_revenue) VALUES ($1, $2, $3, $4)`,
        [uuidv4(), productId, updates.purchases || 0, updates.revenue || 0]
      );
    }
  }

  async getTopProducts(limit: number): Promise<ProductMetrics[]> {
    const result = await pool.query('SELECT * FROM product_metrics ORDER BY total_revenue DESC LIMIT $1', [limit]);
    return result.rows.map(this.mapToProductMetrics);
  }

  private mapToEvent(row: any): AnalyticsEvent {
    return { id: row.id, eventType: row.event_type, sourceService: row.source_service, payload: row.payload, createdAt: row.created_at };
  }

  private mapToDailyMetrics(row: any): DailyMetrics {
    return {
      id: row.id, metricDate: row.metric_date, totalOrders: row.total_orders,
      totalRevenue: parseFloat(row.total_revenue), newUsers: row.new_users,
      activeCarts: row.active_carts, updatedAt: row.updated_at,
    };
  }

  private mapToProductMetrics(row: any): ProductMetrics {
    return {
      id: row.id, productId: row.product_id, totalViews: row.total_views,
      totalPurchases: row.total_purchases, totalRevenue: parseFloat(row.total_revenue),
      avgRating: parseFloat(row.avg_rating), updatedAt: row.updated_at,
    };
  }
}
