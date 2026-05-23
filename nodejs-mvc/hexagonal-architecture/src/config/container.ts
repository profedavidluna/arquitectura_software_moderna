import { ProductService } from '../domain/service/ProductService';
import { InMemoryProductRepository } from '../adapter/output/persistence/InMemoryProductRepository';
import { PostgresProductRepository } from '../adapter/output/persistence/PostgresProductRepository';
import { ProductController } from '../adapter/input/web/ProductController';
import { ProductRepositoryPort } from '../domain/port/output/ProductRepositoryPort';
import { pool } from './database';

/**
 * @layer Configuration / Composition Root
 * @description Manual Dependency Injection wiring.
 * This is where all the pieces of the hexagonal architecture are assembled.
 *
 * The composition root is the ONLY place that knows about all concrete implementations.
 * It wires:
 * - Output adapters (repositories) → injected into domain services
 * - Domain services (implement input ports) → injected into input adapters
 * - Input adapters (controllers) → registered with the web framework
 *
 * Uses the USE_POSTGRES environment variable to select the persistence adapter:
 * - USE_POSTGRES=true → PostgreSQL adapter (requires running database)
 * - Otherwise → In-memory adapter (default, for development/testing)
 */
export function createContainer() {
  // Output adapter (secondary/driven) - selected based on environment
  let productRepository: ProductRepositoryPort;

  if (process.env.USE_POSTGRES === 'true') {
    productRepository = new PostgresProductRepository(pool);
    console.log('   Using PostgreSQL persistence adapter');
  } else {
    productRepository = new InMemoryProductRepository();
    console.log('   Using In-Memory persistence adapter');
  }

  // Domain service (implements input port, depends on output port)
  const productService = new ProductService(productRepository);

  // Input adapter (primary/driving, depends on input port)
  const productController = new ProductController(productService);

  return {
    productRepository,
    productService,
    productController,
  };
}
