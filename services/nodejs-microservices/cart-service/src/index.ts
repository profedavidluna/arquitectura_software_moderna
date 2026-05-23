import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { CartRepository } from './infrastructure/persistence/CartRepository';
import { RedisClient } from './infrastructure/cache/RedisClient';
import { CartServiceImpl } from './application/CartServiceImpl';
import { CartController } from './infrastructure/web/CartController';

async function bootstrap(): Promise<void> {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  await initializeDatabase();

  const redisClient = new RedisClient();
  const cartRepository = new CartRepository();
  const cartService = new CartServiceImpl(cartRepository, redisClient);
  const cartController = new CartController(cartService);

  app.use('/api/v1', cartController.router);

  app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
    console.error('[Global Error]', err);
    res.status(500).json({ error: 'Internal server error' });
  });

  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
  });

  process.on('SIGTERM', async () => {
    await redisClient.disconnect();
    process.exit(0);
  });
}

bootstrap().catch((error) => {
  console.error('[Bootstrap] Fatal error:', error);
  process.exit(1);
});
