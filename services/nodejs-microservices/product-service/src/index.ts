import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { ProductRepository } from './infrastructure/persistence/ProductRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { RedisClient } from './infrastructure/cache/RedisClient';
import { ProductServiceImpl } from './application/ProductServiceImpl';
import { ProductController } from './infrastructure/web/ProductController';

async function bootstrap(): Promise<void> {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  await initializeDatabase();

  const kafkaProducer = new KafkaProducer();
  await kafkaProducer.connect();

  const redisClient = new RedisClient();
  const productRepository = new ProductRepository();
  const productService = new ProductServiceImpl(productRepository, kafkaProducer, redisClient);
  const productController = new ProductController(productService);

  app.use('/api/v1', productController.router);

  app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
    console.error('[Global Error]', err);
    res.status(500).json({ error: 'Internal server error' });
  });

  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
  });

  process.on('SIGTERM', async () => {
    await kafkaProducer.disconnect();
    await redisClient.disconnect();
    process.exit(0);
  });
}

bootstrap().catch((error) => {
  console.error('[Bootstrap] Fatal error:', error);
  process.exit(1);
});
