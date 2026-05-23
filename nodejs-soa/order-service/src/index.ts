// =============================================================================
// Order Service - Entry Point
// =============================================================================
// The Order Service is the Saga Orchestrator in this SOA system.
// It coordinates the distributed transaction between Order and Inventory services.
//
// Components:
// - HTTP API: Receives order requests from clients
// - Kafka Producer: Publishes order events
// - Kafka Consumer: Listens for inventory responses
// =============================================================================

import express from 'express';
import { config } from './config';
import { pool, testConnection } from './infrastructure/persistence/database';
import { OrderRepository } from './infrastructure/persistence/OrderRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { InventoryEventConsumer } from './infrastructure/messaging/InventoryEventConsumer';
import { OrderService } from './application/OrderService';
import { OrderController } from './infrastructure/web/OrderController';

async function bootstrap(): Promise<void> {
  const app = express();

  // Middleware
  app.use(express.json());

  // Health check
  app.get('/health', (_req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  // --- Dependency Injection (Composition Root) ---
  const orderRepository = new OrderRepository(pool);
  const kafkaProducer = new KafkaProducer(config.kafka.brokers, config.kafka.clientId);

  // Application layer
  const orderService = new OrderService(orderRepository, kafkaProducer);

  // Web layer
  const orderController = new OrderController(orderService);
  app.use('/api/orders', orderController.router);

  // Kafka Consumer - listens for inventory responses (Saga pattern)
  const inventoryEventConsumer = new InventoryEventConsumer(
    config.kafka.brokers,
    config.kafka.groupId,
    config.kafka.clientId,
    orderService
  );

  // --- Start Service ---
  await connectWithRetry(async () => {
    await testConnection();
  }, 'PostgreSQL');

  await connectWithRetry(async () => {
    await kafkaProducer.connect();
  }, 'Kafka Producer');

  await connectWithRetry(async () => {
    await inventoryEventConsumer.start();
  }, 'Kafka Consumer');

  const server = app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
    console.log(`[${config.serviceName}] Saga Orchestrator active - listening for inventory events`);
  });

  // --- Graceful Shutdown ---
  const shutdown = async () => {
    console.log(`[${config.serviceName}] Shutting down gracefully...`);
    server.close();
    await inventoryEventConsumer.stop();
    await kafkaProducer.disconnect();
    await pool.end();
    process.exit(0);
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

async function connectWithRetry(
  connectFn: () => Promise<void>,
  serviceName: string,
  maxRetries: number = 10,
  delay: number = 3000
): Promise<void> {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      await connectFn();
      return;
    } catch (error) {
      console.warn(
        `[${config.serviceName}] Failed to connect to ${serviceName} (attempt ${attempt}/${maxRetries})`
      );
      if (attempt === maxRetries) {
        throw new Error(`Failed to connect to ${serviceName} after ${maxRetries} attempts`);
      }
      await new Promise((resolve) => setTimeout(resolve, delay));
    }
  }
}

bootstrap().catch((error) => {
  console.error('[order-service] Fatal error during startup:', error);
  process.exit(1);
});
