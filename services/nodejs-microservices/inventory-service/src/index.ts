import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { InventoryRepository } from './infrastructure/persistence/InventoryRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { InventoryEventConsumer } from './infrastructure/messaging/InventoryEventConsumer';
import { InventoryServiceImpl } from './application/InventoryServiceImpl';
import { InventoryController } from './infrastructure/web/InventoryController';

async function bootstrap(): Promise<void> {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  await initializeDatabase();

  const kafkaProducer = new KafkaProducer();
  await kafkaProducer.connect();

  const inventoryRepository = new InventoryRepository();
  const inventoryService = new InventoryServiceImpl(inventoryRepository, kafkaProducer);
  const inventoryController = new InventoryController(inventoryService);

  // Start event consumer
  const eventConsumer = new InventoryEventConsumer(inventoryService);
  await eventConsumer.start();

  app.use('/api/v1', inventoryController.router);

  app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
    console.error('[Global Error]', err);
    res.status(500).json({ error: 'Internal server error' });
  });

  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
  });

  process.on('SIGTERM', async () => {
    await eventConsumer.stop();
    await kafkaProducer.disconnect();
    process.exit(0);
  });
}

bootstrap().catch((error) => {
  console.error('[Bootstrap] Fatal error:', error);
  process.exit(1);
});
