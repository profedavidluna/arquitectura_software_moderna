import express from 'express';
import { ProductRepository } from './data/repositories/ProductRepository';
import { PostgresProductRepository } from './data/repositories/PostgresProductRepository';
import { IProductRepository, ProductService } from './business/services/ProductService';
import { ProductController } from './presentation/controllers/ProductController';
import { pool } from './data/database';

/**
 * @layer Application Entry Point
 * @description Bootstraps the Layered Architecture application.
 *
 * Wiring follows the dependency direction:
 *   Data → Business → Presentation
 *
 * Each layer only knows about the layer directly below it.
 *
 * Uses the USE_POSTGRES environment variable to select the persistence implementation:
 * - USE_POSTGRES=true → PostgreSQL repository (requires running database)
 * - Otherwise → In-memory repository (default, for development/testing)
 */

const app = express();
const PORT = 3082;

// Middleware
app.use(express.json());

// Wire layers (bottom-up) - select repository based on environment
let productRepository: IProductRepository;

if (process.env.USE_POSTGRES === 'true') {
  productRepository = new PostgresProductRepository(pool);
  console.log('   Using PostgreSQL persistence');
} else {
  productRepository = new ProductRepository();
  console.log('   Using In-Memory persistence');
}

const productService = new ProductService(productRepository);
const productController = new ProductController(productService);

// Register routes
app.use('/api/v1/products', productController.router);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'UP', architecture: 'Layered (Vertical Layer)', timestamp: new Date().toISOString() });
});

app.listen(PORT, () => {
  console.log(`📐 Layered Architecture - Product Catalog API running on port ${PORT}`);
  console.log(`   Endpoints: http://localhost:${PORT}/api/v1/products`);
});

export default app;
