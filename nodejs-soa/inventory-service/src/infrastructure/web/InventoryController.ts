import { Router, Request, Response } from 'express';
import { IInventoryService } from '../../domain/service/IInventoryService';
import { InventoryResponse } from './dto';
import { InventoryItem } from '../../domain/model/InventoryItem';

/**
 * Inventory REST Controller - SOA Service Endpoint.
 * DIP: Depends on IInventoryService interface, not implementation.
 */
export class InventoryController {
  public readonly router: Router;

  constructor(private readonly service: IInventoryService) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/', this.create.bind(this));
    this.router.get('/', this.getAll.bind(this));
    this.router.get('/:productId', this.getByProductId.bind(this));
    this.router.patch('/:productId/increase', this.addStock.bind(this));
  }

  private async create(req: Request, res: Response): Promise<void> {
    try {
      const { productId, productName, quantityAvailable } = req.body;
      if (!productId || !productName || quantityAvailable === undefined) {
        res.status(400).json({ error: 'Missing required fields' });
        return;
      }
      const item = await this.service.createItem(productId, productName, quantityAvailable);
      res.status(201).json(this.toResponse(item));
    } catch (error: any) {
      res.status(500).json({ error: error.message });
    }
  }

  private async getAll(req: Request, res: Response): Promise<void> {
    const items = await this.service.getAll();
    res.json(items.map(this.toResponse));
  }

  private async getByProductId(req: Request, res: Response): Promise<void> {
    const item = await this.service.getByProductId(req.params.productId);
    if (!item) { res.status(404).json({ error: 'Not found' }); return; }
    res.json(this.toResponse(item));
  }

  private async addStock(req: Request, res: Response): Promise<void> {
    try {
      const quantity = parseInt(req.query.quantity as string);
      if (!quantity || quantity <= 0) {
        res.status(400).json({ error: 'Quantity must be positive' });
        return;
      }
      const item = await this.service.addStock(req.params.productId, quantity);
      res.json(this.toResponse(item));
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  }

  private toResponse(item: InventoryItem): InventoryResponse {
    return {
      id: item.id,
      productId: item.productId,
      productName: item.productName,
      quantityAvailable: item.quantityAvailable,
      quantityReserved: item.quantityReserved,
      updatedAt: item.updatedAt.toISOString(),
    };
  }
}
