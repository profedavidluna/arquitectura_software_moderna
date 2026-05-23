import { Router, Request, Response } from 'express';
import { ProductServiceImpl } from '../../application/ProductServiceImpl';
import { toProductResponse, toCategoryResponse, toReviewResponse } from './dto';

export class ProductController {
  public readonly router: Router;

  constructor(private readonly productService: ProductServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    // Products
    this.router.post('/products', this.createProduct.bind(this));
    this.router.get('/products', this.listProducts.bind(this));
    this.router.get('/products/search', this.searchProducts.bind(this));
    this.router.get('/products/:id', this.getProductById.bind(this));
    this.router.put('/products/:id', this.updateProduct.bind(this));
    this.router.delete('/products/:id', this.deleteProduct.bind(this));

    // Categories
    this.router.post('/categories', this.createCategory.bind(this));
    this.router.get('/categories', this.listCategories.bind(this));

    // Reviews
    this.router.post('/products/:id/reviews', this.addReview.bind(this));
    this.router.get('/products/:id/reviews', this.getReviews.bind(this));
  }

  private async createProduct(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.name || !req.body.price) {
        res.status(400).json({ error: 'Missing required fields: name, price' });
        return;
      }
      const product = await this.productService.createProduct(req.body);
      res.status(201).json(toProductResponse(product));
    } catch (error: any) {
      console.error('[ProductController] Create error:', error);
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async getProductById(req: Request, res: Response): Promise<void> {
    try {
      const product = await this.productService.getProductById(req.params.id);
      if (!product) { res.status(404).json({ error: 'Product not found' }); return; }
      res.json(toProductResponse(product));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async listProducts(req: Request, res: Response): Promise<void> {
    try {
      const page = parseInt(req.query.page as string) || 1;
      const limit = parseInt(req.query.limit as string) || 20;
      const products = await this.productService.listProducts(page, limit);
      res.json(products.map(toProductResponse));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async searchProducts(req: Request, res: Response): Promise<void> {
    try {
      const query = req.query.q as string;
      if (!query) { res.status(400).json({ error: 'Query parameter "q" is required' }); return; }
      const products = await this.productService.searchProducts(query);
      res.json(products.map(toProductResponse));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async updateProduct(req: Request, res: Response): Promise<void> {
    try {
      const product = await this.productService.updateProduct(req.params.id, req.body);
      if (!product) { res.status(404).json({ error: 'Product not found' }); return; }
      res.json(toProductResponse(product));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async deleteProduct(req: Request, res: Response): Promise<void> {
    try {
      const deleted = await this.productService.deleteProduct(req.params.id);
      if (!deleted) { res.status(404).json({ error: 'Product not found' }); return; }
      res.status(204).send();
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async createCategory(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.name) { res.status(400).json({ error: 'Name is required' }); return; }
      const category = await this.productService.createCategory(req.body);
      res.status(201).json(toCategoryResponse(category));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async listCategories(req: Request, res: Response): Promise<void> {
    try {
      const categories = await this.productService.listCategories();
      res.json(categories.map(toCategoryResponse));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async addReview(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.userId || !req.body.rating) {
        res.status(400).json({ error: 'Missing required fields: userId, rating' });
        return;
      }
      const review = await this.productService.addReview(req.params.id, req.body);
      res.status(201).json(toReviewResponse(review));
    } catch (error: any) {
      if (error.message === 'Product not found') {
        res.status(404).json({ error: error.message });
      } else {
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async getReviews(req: Request, res: Response): Promise<void> {
    try {
      const reviews = await this.productService.getReviews(req.params.id);
      res.json(reviews.map(toReviewResponse));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
}
