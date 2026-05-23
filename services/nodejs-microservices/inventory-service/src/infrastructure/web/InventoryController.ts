import { Router, Request, Response } from 'express';
import { InventoryServiceImpl } from '../../application/InventoryServiceImpl';
import { toInventoryResponse, toReservationResponse } from './dto';

export class InventoryController {
  public readonly router: Router;

  constructor(private readonly inventoryService: InventoryServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/inventory', this.createInventory.bind(this));
    this.router.get('/inventory', this.listAll.bind(this));
    this.router.get('/inventory/:productId', this.getByProductId.bind(this));
    this.router.put('/inventory/:productId', this.updateStock.bind(this));
    this.router.post('/inventory/:productId/reserve', this.reserveStock.bind(this));
    this.router.post('/inventory/:productId/release', this.releaseStock.bind(this));
  }

  private async createInventory(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.productId || req.body.quantity === undefined) {
        res.status(400).json({ error: 'productId and quantity are required' }); return;
      }
      const item = await this.inventoryService.createInventory(req.body);
      res.status(201).json(toInventoryResponse(item));
    } catch (error: any) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async getByProductId(req: Request, res: Response): Promise<void> {
    try {
      const item = await this.inventoryService.getByProductId(req.params.productId);
      if (!item) { res.status(404).json({ error: 'Inventory not found' }); return; }
      res.json(toInventoryResponse(item));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async listAll(req: Request, res: Response): Promise<void> {
    try {
      const items = await this.inventoryService.listAll();
      res.json(items.map(toInventoryResponse));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async updateStock(req: Request, res: Response): Promise<void> {
    try {
      if (req.body.quantity === undefined) { res.status(400).json({ error: 'quantity is required' }); return; }
      const item = await this.inventoryService.updateStock(req.params.productId, req.body.quantity);
      if (!item) { res.status(404).json({ error: 'Inventory not found' }); return; }
      res.json(toInventoryResponse(item));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async reserveStock(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.orderId || !req.body.quantity) {
        res.status(400).json({ error: 'orderId and quantity are required' }); return;
      }
      const reservation = await this.inventoryService.reserveStock(req.params.productId, req.body);
      res.status(201).json(toReservationResponse(reservation));
    } catch (error: any) {
      if (error.message.includes('Insufficient')) {
        res.status(409).json({ error: error.message });
      } else {
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async releaseStock(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.orderId) { res.status(400).json({ error: 'orderId is required' }); return; }
      const released = await this.inventoryService.releaseStock(req.params.productId, req.body.orderId);
      if (!released) { res.status(404).json({ error: 'Reservation not found' }); return; }
      res.json({ message: 'Stock released successfully' });
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
}
