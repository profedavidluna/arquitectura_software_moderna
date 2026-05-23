// =============================================================================
// Product Service - Entry Point
// =============================================================================
// This is the bootstrap file that wires all components together.
// It demonstrates the Composition Root pattern where all dependencies
// are assembled in one place.
//
// SOA Service Lifecycle:
// 1. Load configuration
// 2. Connect to database
// 3. Connect to ESB (Kafka)
// 4. Start HTTP server
// 5. Handle graceful shutdown
// =============================================================================

import express from 'express';
import { config } from './config';
import { pool, testConnection } from './infrastructure/persistence/database';
import { ProductRepository } from './infrastructure/persistence/ProductRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { ProductService } from './application/ProductService';
import { ProductController } from './infrastructure/web/ProductController';

async function bootstrap(): Promise<void> {
  const app = express();

  // Middleware
  app.use(express.json());

  // Health check endpoint (used by Docker and load balancers)
  app.get('/health', (_req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  // --- Dependency Injection (Composition Root) ---
  // This is where we wire all layers together following DIP

  // Infrastructure layer
  const productRepository = new ProductRepository(pool);
  const kafkaProducer = new KafkaProducer(config.kafka.brokers, config.kafka.clientId);

  // Application layer (depends on infrastructure via constructor injection)
  const productService = new ProductService(productRepository, kafkaProducer);

  // Web layer (depends on application layer via interface)
  const productController = new ProductController(productService);

  // Register routes
  app.use('/api/products', productController.router);

  // --- Start Service ---
  // Connect to infrastructure with retry logic
  await connectWithRetry(async () => {
    await testConnection();
  }, 'PostgreSQL');

  await connectWithRetry(async () => {
    await kafkaProducer.connect();
  }, 'Kafka');

  // Start HTTP server
  const server = app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
    console.log(`[${config.serviceName}] Health check: http://localhost:${config.port}/health`);
  });

  // --- Graceful Shutdown ---
  const shutdown = async () => {
    console.log(`[${config.serviceName}] Shutting down gracefully...`);
    server.close();
    await kafkaProducer.disconnect();
    await pool.end();
    process.exit(0);
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

/**
 * Retry connection to infrastructure services.
 * In Docker, services may start before infrastructure is ready.
 */
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

// Start the service
bootstrap().catch((error) => {
  console.error('[product-service] Fatal error during startup:', error);
  process.exit(1);
});
