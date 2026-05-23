/**
 * User Service - Entry Point
 * 
 * Bootstraps the Express application, initializes infrastructure
 * (database, Kafka), and wires up dependencies.
 * 
 * This is the Composition Root - where all dependencies are assembled.
 */
import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { UserRepository } from './infrastructure/persistence/UserRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { UserServiceImpl } from './application/UserServiceImpl';
import { UserController } from './infrastructure/web/UserController';

async function bootstrap(): Promise<void> {
  const app = express();

  // Middleware
  app.use(express.json());

  // Health check endpoint
  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  // Initialize infrastructure
  await initializeDatabase();

  const kafkaProducer = new KafkaProducer();
  await kafkaProducer.connect();

  // Wire dependencies (manual DI - in production, use a DI container like tsyringe)
  const userRepository = new UserRepository();
  const userService = new UserServiceImpl(userRepository, kafkaProducer);
  const userController = new UserController(userService);

  // Register routes
  app.use('/api/v1', userController.router);

  // Global error handler
  app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
    console.error('[Global Error]', err);
    res.status(500).json({ error: 'Internal server error' });
  });

  // Start server
  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
    console.log(`[${config.serviceName}] Health: http://localhost:${config.port}/health`);
  });

  // Graceful shutdown
  process.on('SIGTERM', async () => {
    console.log(`[${config.serviceName}] Shutting down...`);
    await kafkaProducer.disconnect();
    process.exit(0);
  });
}

bootstrap().catch((error) => {
  console.error('[Bootstrap] Fatal error:', error);
  process.exit(1);
});
