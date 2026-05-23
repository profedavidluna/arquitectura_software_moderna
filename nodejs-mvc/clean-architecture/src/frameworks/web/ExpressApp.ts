import express from 'express';
import { ProductController } from '../../adapters/controllers/ProductController';

/**
 * @layer Frameworks & Drivers
 * @description Express application setup.
 * This is framework-specific code that belongs to the outermost layer.
 */
export function createExpressApp(productController: ProductController) {
  const app = express();

  app.use(express.json());

  // Routes
  app.use('/api/v1/products', productController.router);

  // Health check
  app.get('/health', (req, res) => {
    res.json({ status: 'UP', architecture: 'Clean Architecture', timestamp: new Date().toISOString() });
  });

  return app;
}
