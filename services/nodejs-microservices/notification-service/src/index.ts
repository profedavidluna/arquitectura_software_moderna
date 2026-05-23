/**
 * Notification Service - Entry Point
 * 
 * This service is unique: it's purely event-driven with no REST API.
 * It only exposes a health check endpoint for monitoring.
 * All work is triggered by Kafka events from other services.
 */
import express from 'express';
import { config } from './config';
import { NotificationServiceImpl } from './application/NotificationServiceImpl';
import { NotificationConsumer } from './infrastructure/messaging/NotificationConsumer';

async function bootstrap(): Promise<void> {
  const app = express();

  // Only health check - no REST API
  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  const notificationService = new NotificationServiceImpl();
  const consumer = new NotificationConsumer(notificationService);
  await consumer.start();

  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port} (event-driven, no REST API)`);
    console.log(`[${config.serviceName}] Listening for events from: user-events, order-events, payment-events`);
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
