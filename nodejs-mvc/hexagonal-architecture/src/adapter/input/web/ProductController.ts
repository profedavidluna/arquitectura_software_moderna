import { Router, Request, Response, NextFunction } from 'express';
import { ProductServicePort } from '../../../domain/port/input/ProductServicePort';
import { CreateProductRequest, ProductResponse, UpdateProductRequest } from './dto';
import { Product } from '../../../domain/model/Product';

/**
 * @layer Adapter - Input (Primary/Driving)
 * @description Express router that adapts HTTP requests to domain service calls.
 * This is a PRIMARY ADAPTER in Hexagonal Architecture.
 *
 * It translates HTTP concerns (request parsing, response formatting, status codes)
 * into domain operations via the input port (ProductServicePort).
 *
 * The controller does NOT contain business logic - it only:
 * 1. Parses and validates HTTP input
 * 2. Calls the appropriate service method
 * 3. Formats the response
 */
export class ProductController {
  public readonly router: Router;

  constructor(private readonly productService: ProductServicePort) {
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

      // Input validation
      if (!body.name || !body.description || !body.price || !body.category || !body.sku) {
        res.status(400).json({ error: 'Bad Request', message: 'Missing required fields: name, description, price, category, sku', timestamp: new Date().toISOString() });
        return;
      }

      if (body.stockQuantity === undefined || body.stockQuantity === null) {
        res.status(400).json({ error: 'Bad Request', message: 'Missing required field: stockQuantity', timestamp: new Date().toISOString() });
        return;
      }

      const product = await this.productService.createProduct({
        name: body.name,
        description: body.description,
        price: body.price,
        category: body.category,
        stockQuantity: body.stockQuantity,
        sku: body.sku,
      });

      res.status(201).json(this.toResponse(product));
    } catch (error) {
      next(error);
    }
  }

  private async listProducts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const page = parseInt(req.query.page as string) || 0;
      const size = parseInt(req.query.size as string) || 20;

      const result = await this.productService.listProducts(page, size);

      res.json({
        content: result.content.map(this.toResponse),
        page: result.page,
        size: result.size,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
      });
    } catch (error) {
      next(error);
    }
  }

  private async getProductById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const product = await this.productService.getProductById(req.params.id);
      res.json(this.toResponse(product));
    } catch (error) {
      next(error);
    }
  }

  private async searchProducts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { query, category, minPrice, maxPrice } = req.query;

      const products = await this.productService.searchProducts({
        query: query as string,
        category: category as string,
        minPrice: minPrice ? parseFloat(minPrice as string) : undefined,
        maxPrice: maxPrice ? parseFloat(maxPrice as string) : undefined,
      });

      res.json(products.map(this.toResponse));
    } catch (error) {
      next(error);
    }
  }

  private async updateProduct(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const body: UpdateProductRequest = req.body;
      const product = await this.productService.updateProduct(req.params.id, body);
      res.json(this.toResponse(product));
    } catch (error) {
      next(error);
    }
  }

  private async deleteProduct(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      await this.productService.deleteProduct(req.params.id);
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  }

  private async decreaseStock(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json({ error: 'Bad Request', message: 'Quantity must be a positive number', timestamp: new Date().toISOString() });
        return;
      }

      const product = await this.productService.decreaseStock(req.params.id, quantity);
      res.json(this.toResponse(product));
    } catch (error) {
      next(error);
    }
  }

  private async increaseStock(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json({ error: 'Bad Request', message: 'Quantity must be a positive number', timestamp: new Date().toISOString() });
        return;
      }

      const product = await this.productService.increaseStock(req.params.id, quantity);
      res.json(this.toResponse(product));
    } catch (error) {
      next(error);
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
}
