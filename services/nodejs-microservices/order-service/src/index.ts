import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { OrderRepository } from './infrastructure/persistence/OrderRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { OrderEventConsumer } from './infrastructure/messaging/OrderEventConsumer';
import { OrderServiceImpl } from './application/OrderServiceImpl';
import { OrderController } from './infrastructure/web/OrderController';

async function bootstrap(): Promise<void> {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  await initializeDatabase();

  const kafkaProducer = new KafkaProducer();
  await kafkaProducer.connect();

  const orderRepository = new OrderRepository();
  const orderService = new OrderServiceImpl(orderRepository, kafkaProducer);
  const orderController = new OrderController(orderService);

  // Start event consumer for Saga responses
  const eventConsumer = new OrderEventConsumer(orderRepository);
  await eventConsumer.start();

  app.use('/api/v1', orderController.router);

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
