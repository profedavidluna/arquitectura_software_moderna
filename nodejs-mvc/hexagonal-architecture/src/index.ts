import express, { Request, Response, NextFunction } from 'express';
import { createContainer } from './config/container';

/**
 * @layer Infrastructure / Framework
 * @description Express application setup and error handling middleware.
 * This is the entry point that bootstraps the hexagonal architecture.
 */

const app = express();
const PORT = 3081;

// Middleware
app.use(express.json());

// Wire up the container
const container = createContainer();

// Register routes
app.use('/api/v1/products', container.productController.router);

// Global error handling middleware
app.use((err: Error, req: Request, res: Response, _next: NextFunction) => {
  console.error(`[ERROR] ${err.message}`);

  if (err.message.includes('not found')) {
    res.status(404).json({
      error: 'Not Found',
      message: err.message,
      timestamp: new Date().toISOString(),
    });
    return;
  }

  if (err.message.includes('already exists') || err.message.includes('Insufficient stock') || err.message.includes('must be') || err.message.includes('cannot')) {
    res.status(400).json({
      error: 'Bad Request',
      message: err.message,
      timestamp: new Date().toISOString(),
    });
    return;
  }

  res.status(500).json({
    error: 'Internal Server Error',
    message: 'An unexpected error occurred',
    timestamp: new Date().toISOString(),
  });
});

app.listen(PORT, () => {
  console.log(`🔷 Hexagonal Architecture - Product Catalog API running on port ${PORT}`);
  console.log(`   Endpoints: http://localhost:${PORT}/api/v1/products`);
});

export default app;
