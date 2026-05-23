import { Router, Request, Response } from 'express';
import { CreateProductUseCase } from '../../usecases/CreateProductUseCase';
import { GetProductUseCase } from '../../usecases/GetProductUseCase';
import { ListProductsUseCase } from '../../usecases/ListProductsUseCase';
import { SearchProductsUseCase } from '../../usecases/SearchProductsUseCase';
import { UpdateProductUseCase } from '../../usecases/UpdateProductUseCase';
import { DeleteProductUseCase } from '../../usecases/DeleteProductUseCase';
import { ManageStockUseCase } from '../../usecases/ManageStockUseCase';
import { ProductNotFoundError, DuplicateSkuError } from '../../usecases/errors/UseCaseErrors';

/**
 * @layer Interface Adapters (Controllers)
 * @description Converts HTTP requests into use case input and use case output into HTTP responses.
 * In Clean Architecture, controllers belong to the Interface Adapters layer.
 *
 * The controller:
 * - Receives raw HTTP data
 * - Converts it to use case input format
 * - Invokes the appropriate use case
 * - Converts use case output to HTTP response format
 *
 * It does NOT contain business logic - that belongs to entities and use cases.
 */
export class ProductController {
  public readonly router: Router;

  constructor(
    private readonly createProduct: CreateProductUseCase,
    private readonly getProduct: GetProductUseCase,
    private readonly listProducts: ListProductsUseCase,
    private readonly searchProducts: SearchProductsUseCase,
    private readonly updateProduct: UpdateProductUseCase,
    private readonly deleteProduct: DeleteProductUseCase,
    private readonly manageStock: ManageStockUseCase
  ) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/', this.handleCreate.bind(this));
    this.router.get('/search', this.handleSearch.bind(this));
    this.router.get('/', this.handleList.bind(this));
    this.router.get('/:id', this.handleGetById.bind(this));
    this.router.put('/:id', this.handleUpdate.bind(this));
    this.router.delete('/:id', this.handleDelete.bind(this));
    this.router.patch('/:id/stock/decrease', this.handleDecreaseStock.bind(this));
    this.router.patch('/:id/stock/increase', this.handleIncreaseStock.bind(this));
  }

  private async handleCreate(req: Request, res: Response): Promise<void> {
    try {
      const { name, description, price, category, stockQuantity, sku } = req.body;

      if (!name || !description || price === undefined || !category || !sku || stockQuantity === undefined) {
        res.status(400).json(this.errorResponse('Bad Request', 'Missing required fields'));
        return;
      }

      const output = await this.createProduct.execute({ name, description, price, category, stockQuantity, sku });
      res.status(201).json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleGetById(req: Request, res: Response): Promise<void> {
    try {
      const output = await this.getProduct.execute(req.params.id);
      res.json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleList(req: Request, res: Response): Promise<void> {
    try {
      const page = parseInt(req.query.page as string) || 0;
      const size = parseInt(req.query.size as string) || 20;
      const output = await this.listProducts.execute(page, size);
      res.json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleSearch(req: Request, res: Response): Promise<void> {
    try {
      const { query, category, minPrice, maxPrice } = req.query;
      const output = await this.searchProducts.execute({
        query: query as string,
        category: category as string,
        minPrice: minPrice ? parseFloat(minPrice as string) : undefined,
        maxPrice: maxPrice ? parseFloat(maxPrice as string) : undefined,
      });
      res.json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleUpdate(req: Request, res: Response): Promise<void> {
    try {
      const output = await this.updateProduct.execute(req.params.id, req.body);
      res.json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleDelete(req: Request, res: Response): Promise<void> {
    try {
      await this.deleteProduct.execute(req.params.id);
      res.status(204).send();
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleDecreaseStock(req: Request, res: Response): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json(this.errorResponse('Bad Request', 'Quantity must be a positive number'));
        return;
      }
      const output = await this.manageStock.decrease(req.params.id, quantity);
      res.json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private async handleIncreaseStock(req: Request, res: Response): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json(this.errorResponse('Bad Request', 'Quantity must be a positive number'));
        return;
      }
      const output = await this.manageStock.increase(req.params.id, quantity);
      res.json(output);
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private handleError(error: unknown, res: Response): void {
    if (error instanceof ProductNotFoundError) {
      res.status(404).json(this.errorResponse('Not Found', error.message));
    } else if (error instanceof DuplicateSkuError) {
      res.status(409).json(this.errorResponse('Conflict', error.message));
    } else if (error instanceof Error && (error.message.includes('must be') || error.message.includes('cannot') || error.message.includes('Insufficient') || error.message.includes('required'))) {
      res.status(400).json(this.errorResponse('Bad Request', error.message));
    } else {
      res.status(500).json(this.errorResponse('Internal Server Error', 'An unexpected error occurred'));
    }
  }

  private errorResponse(error: string, message: string) {
    return { error, message, timestamp: new Date().toISOString() };
  }
}
