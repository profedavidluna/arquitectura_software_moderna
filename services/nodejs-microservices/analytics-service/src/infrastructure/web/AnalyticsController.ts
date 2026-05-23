import { Router, Request, Response } from 'express';
import { AnalyticsServiceImpl } from '../../application/AnalyticsServiceImpl';

export class AnalyticsController {
  public readonly router: Router;

  constructor(private readonly analyticsService: AnalyticsServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.get('/analytics/dashboard', this.getDashboard.bind(this));
    this.router.get('/analytics/revenue', this.getRevenueReport.bind(this));
  }

  private async getDashboard(req: Request, res: Response): Promise<void> {
    try {
      const dashboard = await this.analyticsService.getDashboard();
      res.json(dashboard);
    } catch (error) {
      console.error('[AnalyticsController] Dashboard error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async getRevenueReport(req: Request, res: Response): Promise<void> {
    try {
      const days = parseInt(req.query.days as string) || 30;
      const report = await this.analyticsService.getRevenueReport(days);
      res.json(report);
    } catch (error) {
      console.error('[AnalyticsController] Revenue report error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }
}
