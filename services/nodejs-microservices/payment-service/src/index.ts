import express from 'express';
import { config } from './config';
import { initializeDatabase } from './infrastructure/persistence/database';
import { PaymentRepository } from './infrastructure/persistence/PaymentRepository';
import { KafkaProducer } from './infrastructure/messaging/KafkaProducer';
import { PaymentServiceImpl } from './application/PaymentServiceImpl';
import { PaymentController } from './infrastructure/web/PaymentController';

async function bootstrap(): Promise<void> {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: config.serviceName, timestamp: new Date().toISOString() });
  });

  await initializeDatabase();

  const kafkaProducer = new KafkaProducer();
  await kafkaProducer.connect();

  const paymentRepository = new PaymentRepository();
  const paymentService = new PaymentServiceImpl(paymentRepository, kafkaProducer);
  const paymentController = new PaymentController(paymentService);

  app.use('/api/v1', paymentController.router);

  app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
    console.error('[Global Error]', err);
    res.status(500).json({ error: 'Internal server error' });
  });

  app.listen(config.port, () => {
    console.log(`[${config.serviceName}] Running on port ${config.port}`);
  });

  process.on('SIGTERM', async () => {
    await kafkaProducer.disconnect();
    process.exit(0);
  });
}

bootstrap().catch((error) => {
  console.error('[Bootstrap] Fatal error:', error);
  process.exit(1);
});
