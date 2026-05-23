import express from 'express';
import { config } from './config';
import { pool } from './infrastructure/persistence/database';
import { InventoryRepository } from './infrastructure/persistence/InventoryRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { OrderEventConsumer } from './infrastructure/messaging/OrderEventConsumer';
import { InventoryServiceImpl } from './application/InventoryService';
import { InventoryController } from './infrastructure/web/InventoryController';

/**
 * Inventory Service - SOA Entry Point.
 * 
 * Wiring: Repository + KafkaProducer → Service → Controller + Consumer
 * 
 * This service participates in the Order Saga:
 * - Consumes order.created → reserves stock
 * - Consumes order.cancelled → releases stock (compensation)
 * - Publishes stock.reserved / stock.insufficient / stock.released
 */
async function main() {
  const app = express();
  app.use(express.json());

  // Infrastructure
  const repository = new InventoryRepository(pool);
  const producer = new KafkaProducer(config.kafka.brokers, config.kafka.clientId);

  // Application service
  const inventoryService = new InventoryServiceImpl(repository, producer);

  // Web layer
  const controller = new InventoryController(inventoryService);
  app.use('/api/v1/inventory', controller.router);

  // Health check
  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: 'inventory-service', architecture: 'SOA' });
  });

  // Start Kafka producer
  try {
    await producer.connect();
  } catch (err) {
    console.warn('[Kafka] Producer connection failed (will retry on first publish):', (err as Error).message);
  }

  // Start Kafka consumer (Saga participant)
  try {
    const consumer = new OrderEventConsumer(config.kafka.brokers, config.kafka.groupId, inventoryService);
    await consumer.start();
  } catch (err) {
    console.warn('[Kafka] Consumer connection failed (will retry):', (err as Error).message);
  }

  app.listen(config.port, () => {
    console.log(`📦 Inventory Service (SOA) running on port ${config.port}`);
    console.log(`   Endpoints: http://localhost:${config.port}/api/v1/inventory`);
  });
}

main().catch(console.error);
