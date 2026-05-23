import { Router, Request, Response, NextFunction } from 'express';
import { ProductService } from '../../business/services/ProductService';
import {
  CreateProductRequest,
  ProductResponse,
  UpdateProductRequest,
  PaginatedResponse,
} from '../dto';
import { Product } from '../../data/models/Product';
import { ConflictError, InsufficientStockError, NotFoundError, ValidationError } from '../../business/errors';

/**
 * @layer Presentation Layer
 * @description Controller that handles HTTP requests and delegates to the Business layer.
 * In Layered Architecture, the Presentation layer is the topmost layer.
 *
 * Dependencies flow DOWNWARD:
 *   Presentation → Business → Data
 *
 * The controller:
 * 1. Parses HTTP input
 * 2. Calls the business service
 * 3. Maps domain objects to response DTOs
 * 4. Handles errors and sets HTTP status codes
 */
export class ProductController {
  public readonly router: Router;

  constructor(private readonly productService: ProductService) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/', this.createProduct.bind(this));
    this.router.get('/search', this.searchProducts.bind(this));
    this.router.get('/', this.listProducts.bind(this));
    this.router.get('/:id', this.getProductById.bind(this));
    this.router.put('/:id', this.updateProduct.bind(this));
    this.router.delete('/:id', this.deleteProduct.bind(this));
    this.router.patch('/:id/stock/decrease', this.decreaseStock.bind(this));
    this.router.patch('/:id/stock/increase', this.increaseStock.bind(this));
  }

  private async createProduct(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const body: CreateProductRequest = req.body;

      if (!body.name || !body.description || !body.price || !body.category || !body.sku) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required fields: name, description, price, category, sku',
          timestamp: new Date().toISOString(),
        });
        return;
      }

      if (body.stockQuantity === undefined || body.stockQuantity === null) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required field: stockQuantity',
          timestamp: new Date().toISOString(),
        });
        return;
      }

      const product = await this.productService.createProduct(body);
      res.status(201).json(this.toResponse(product));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async listProducts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const page = parseInt(req.query.page as string) || 0;
      const size = parseInt(req.query.size as string) || 20;

      const result = await this.productService.listProducts(page, size);

      const response: PaginatedResponse<ProductResponse> = {
        content: result.content.map(this.toResponse),
        page: result.page,
        size: result.size,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
      };

      res.json(response);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async getProductById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const product = await this.productService.getProductById(req.params.id);
      res.json(this.toResponse(product));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async searchProducts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { query, category, minPrice, maxPrice } = req.query;

      const products = await this.productService.searchProducts(
        query as string,
        category as string,
        minPrice ? parseFloat(minPrice as string) : undefined,
        maxPrice ? parseFloat(maxPrice as string) : undefined
      );

      res.json(products.map(this.toResponse));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async updateProduct(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const body: UpdateProductRequest = req.body;
      const product = await this.productService.updateProduct(req.params.id, body);
      res.json(this.toResponse(product));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async deleteProduct(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      await this.productService.deleteProduct(req.params.id);
      res.status(204).send();
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async decreaseStock(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Quantity must be a positive number',
          timestamp: new Date().toISOString(),
        });
        return;
      }

      const product = await this.productService.decreaseStock(req.params.id, quantity);
      res.json(this.toResponse(product));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async increaseStock(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Quantity must be a positive number',
          timestamp: new Date().toISOString(),
        });
        return;
      }

      const product = await this.productService.increaseStock(req.params.id, quantity);
      res.json(this.toResponse(product));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private toResponse(product: Product): ProductResponse {
    return {
      id: product.id,
      name: product.name,
      description: product.description,
      price: product.price,
      category: product.category,
      stockQuantity: product.stockQuantity,
      sku: product.sku,
      active: product.active,
      createdAt: product.createdAt.toISOString(),
      updatedAt: product.updatedAt.toISOString(),
    };
  }

  private handleError(error: unknown, res: Response): void {
    if (error instanceof NotFoundError) {
      res.status(404).json({ error: 'Not Found', message: error.message, timestamp: new Date().toISOString() });
    } else if (error instanceof ValidationError || error instanceof InsufficientStockError) {
      res.status(400).json({ error: 'Bad Request', message: error.message, timestamp: new Date().toISOString() });
    } else if (error instanceof ConflictError) {
      res.status(409).json({ error: 'Conflict', message: error.message, timestamp: new Date().toISOString() });
    } else {
      res.status(500).json({ error: 'Internal Server Error', message: 'An unexpected error occurred', timestamp: new Date().toISOString() });
    }
  }
}
