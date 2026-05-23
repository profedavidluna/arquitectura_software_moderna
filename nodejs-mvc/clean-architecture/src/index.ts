import { InMemoryProductGateway } from './frameworks/persistence/InMemoryProductGateway';
import { PostgresProductGateway } from './frameworks/persistence/PostgresProductGateway';
import { ProductGateway } from './usecases/interfaces/ProductGateway';
import { CreateProductUseCase } from './usecases/CreateProductUseCase';
import { GetProductUseCase } from './usecases/GetProductUseCase';
import { ListProductsUseCase } from './usecases/ListProductsUseCase';
import { SearchProductsUseCase } from './usecases/SearchProductsUseCase';
import { UpdateProductUseCase } from './usecases/UpdateProductUseCase';
import { DeleteProductUseCase } from './usecases/DeleteProductUseCase';
import { ManageStockUseCase } from './usecases/ManageStockUseCase';
import { ProductController } from './adapters/controllers/ProductController';
import { createExpressApp } from './frameworks/web/ExpressApp';
import { pool } from './frameworks/persistence/database';

/**
 * @layer Main / Composition Root
 * @description Wires all layers together following the Dependency Rule.
 *
 * Clean Architecture layers (inside → outside):
 *   Entities → Use Cases → Interface Adapters → Frameworks & Drivers
 *
 * Dependencies always point INWARD:
 *   Frameworks → Adapters → Use Cases → Entities
 *
 * Uses the USE_POSTGRES environment variable to select the persistence implementation:
 * - USE_POSTGRES=true → PostgreSQL gateway (requires running database)
 * - Otherwise → In-memory gateway (default, for development/testing)
 */

const PORT = 3083;

// Frameworks & Drivers layer (outermost) - select gateway based on environment
let productGateway: ProductGateway;

if (process.env.USE_POSTGRES === 'true') {
  productGateway = new PostgresProductGateway(pool);
  console.log('   Using PostgreSQL persistence gateway');
} else {
  productGateway = new InMemoryProductGateway();
  console.log('   Using In-Memory persistence gateway');
}

// Use Cases layer (application business rules)
const createProductUseCase = new CreateProductUseCase(productGateway);
const getProductUseCase = new GetProductUseCase(productGateway);
const listProductsUseCase = new ListProductsUseCase(productGateway);
const searchProductsUseCase = new SearchProductsUseCase(productGateway);
const updateProductUseCase = new UpdateProductUseCase(productGateway);
const deleteProductUseCase = new DeleteProductUseCase(productGateway);
const manageStockUseCase = new ManageStockUseCase(productGateway);

// Interface Adapters layer (controllers)
const productController = new ProductController(
  createProductUseCase,
  getProductUseCase,
  listProductsUseCase,
  searchProductsUseCase,
  updateProductUseCase,
  deleteProductUseCase,
  manageStockUseCase
);

// Frameworks & Drivers layer (Express app)
const app = createExpressApp(productController);

app.listen(PORT, () => {
  console.log(`🧅 Clean Architecture - Product Catalog API running on port ${PORT}`);
  console.log(`   Endpoints: http://localhost:${PORT}/api/v1/products`);
});

export default app;
