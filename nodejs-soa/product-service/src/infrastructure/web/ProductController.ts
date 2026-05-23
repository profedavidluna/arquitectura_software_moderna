// =============================================================================
// Product Controller - HTTP API Layer
// =============================================================================
// The controller handles HTTP requests and delegates to the service layer.
// It follows the DIP (Dependency Inversion Principle) by depending on the
// IProductService interface rather than the concrete implementation.
//
// Responsibilities:
// - Parse HTTP requests
// - Validate input (using DTOs)
// - Delegate to service layer
// - Format HTTP responses
// =============================================================================

import { Router, Request, Response } from 'express';
import { IProductService } from '../../domain/service/IProductService';
import { validateCreateProductDto } from './dto';

export class ProductController {
  public readonly router: Router;

  constructor(private readonly productService: IProductService) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.get('/', this.findAll.bind(this));
    this.router.get('/:id', this.findById.bind(this));
    this.router.post('/', this.create.bind(this));
    this.router.put('/:id', this.update.bind(this));
    this.router.delete('/:id', this.delete.bind(this));
  }

  /**
   * GET /api/products - List all products
   */
  private async findAll(_req: Request, res: Response): Promise<void> {
    try {
      const products = await this.productService.findAll();
      res.json({ success: true, data: products });
    } catch (error) {
      console.error('[ProductController] Error in findAll:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * GET /api/products/:id - Get product by ID
   */
  private async findById(req: Request, res: Response): Promise<void> {
    try {
      const product = await this.productService.findById(req.params.id);
      if (!product) {
        res.status(404).json({ success: false, error: 'Product not found' });
        return;
      }
      res.json({ success: true, data: product });
    } catch (error) {
      console.error('[ProductController] Error in findById:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * POST /api/products - Create a new product
   */
  private async create(req: Request, res: Response): Promise<void> {
    try {
      // Validate request body using DTO validation
      const validationError = validateCreateProductDto(req.body);
      if (validationError) {
        res.status(400).json({ success: false, error: validationError });
        return;
      }

      const product = await this.productService.create(req.body);
      res.status(201).json({ success: true, data: product });
    } catch (error: any) {
      // Handle unique constraint violation (duplicate SKU)
      if (error.code === '23505') {
        res.status(409).json({ success: false, error: 'Product with this SKU already exists' });
        return;
      }
      console.error('[ProductController] Error in create:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * PUT /api/products/:id - Update a product
   */
  private async update(req: Request, res: Response): Promise<void> {
    try {
      const product = await this.productService.update(req.params.id, req.body);
      if (!product) {
        res.status(404).json({ success: false, error: 'Product not found' });
        return;
      }
      res.json({ success: true, data: product });
    } catch (error) {
      console.error('[ProductController] Error in update:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * DELETE /api/products/:id - Soft delete a product
   */
  private async delete(req: Request, res: Response): Promise<void> {
    try {
      const deleted = await this.productService.delete(req.params.id);
      if (!deleted) {
        res.status(404).json({ success: false, error: 'Product not found' });
        return;
      }
      res.json({ success: true, message: 'Product deleted successfully' });
    } catch (error) {
      console.error('[ProductController] Error in delete:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }
}
