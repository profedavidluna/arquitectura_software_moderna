import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { AnalyticsRepository } from './infrastructure/persistence/AnalyticsRepository';
import { AnalyticsConsumer } from './infrastructure/messaging/AnalyticsConsumer';
import { AnalyticsServiceImpl } from './application/AnalyticsServiceImpl';
import { AnalyticsController } from './infrastructure/web/AnalyticsController';

async function bootstrap(): Promise<void> {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  await initializeDatabase();

  const analyticsRepository = new AnalyticsRepository();
  const analyticsService = new AnalyticsServiceImpl(analyticsRepository);
  const analyticsController = new AnalyticsController(analyticsService);

  // Start consuming ALL events
  const consumer = new AnalyticsConsumer(analyticsService);
  await consumer.start();

  app.use('/api/v1', analyticsController.router);

  app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
    console.error('[Global Error]', err);
    res.status(500).json({ error: 'Internal server error' });
  });

  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
    console.log(`[${config.serviceName}] Dashboard: http://localhost:${config.port}/api/v1/analytics/dashboard`);
  });

  process.on('SIGTERM', async () => {
    await consumer.stop();
    process.exit(0);
  });
}

bootstrap().catch((error) => {
  console.error('[Bootstrap] Fatal error:', error);
  process.exit(1);
});
