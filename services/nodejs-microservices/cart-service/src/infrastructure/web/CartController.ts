import { Router, Request, Response } from 'express';
import { CartServiceImpl } from '../../application/CartServiceImpl';
import { toCartSummaryResponse } from './dto';

export class CartController {
  public readonly router: Router;

  constructor(private readonly cartService: CartServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/carts', this.createCart.bind(this));
    this.router.get('/carts/:id', this.getCart.bind(this));
    this.router.post('/carts/:id/items', this.addItem.bind(this));
    this.router.put('/carts/:cartId/items/:itemId', this.updateItemQuantity.bind(this));
    this.router.delete('/carts/:cartId/items/:itemId', this.removeItem.bind(this));
    this.router.post('/carts/:id/coupon', this.applyCoupon.bind(this));
    this.router.delete('/carts/:id', this.clearCart.bind(this));
  }

  private async createCart(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.userId) { res.status(400).json({ error: 'userId is required' }); return; }
      const cart = await this.cartService.createCart(req.body);
      res.status(201).json(cart);
    } catch (error: any) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async getCart(req: Request, res: Response): Promise<void> {
    try {
      const summary = await this.cartService.getCartById(req.params.id);
      if (!summary) { res.status(404).json({ error: 'Cart not found' }); return; }
      res.json(toCartSummaryResponse(summary));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async addItem(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.productId || !req.body.quantity) {
        res.status(400).json({ error: 'productId and quantity are required' }); return;
      }
      const item = await this.cartService.addItem(req.params.id, req.body);
      res.status(201).json(item);
    } catch (error: any) {
      if (error.message.includes('not found') || error.message.includes('not active')) {
        res.status(400).json({ error: error.message });
      } else {
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async updateItemQuantity(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.quantity) { res.status(400).json({ error: 'quantity is required' }); return; }
      const item = await this.cartService.updateItemQuantity(req.params.cartId, req.params.itemId, req.body.quantity);
      if (!item) { res.status(404).json({ error: 'Item not found' }); return; }
      res.json(item);
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  }

  private async removeItem(req: Request, res: Response): Promise<void> {
    try {
      const deleted = await this.cartService.removeItem(req.params.cartId, req.params.itemId);
      if (!deleted) { res.status(404).json({ error: 'Item not found' }); return; }
      res.status(204).send();
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async applyCoupon(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.couponCode) { res.status(400).json({ error: 'couponCode is required' }); return; }
      const cart = await this.cartService.applyCoupon(req.params.id, req.body);
      res.json(cart);
    } catch (error: any) {
      if (error.message.includes('Invalid') || error.message.includes('not found')) {
        res.status(400).json({ error: error.message });
      } else {
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async clearCart(req: Request, res: Response): Promise<void> {
    try {
      await this.cartService.clearCart(req.params.id);
      res.status(204).send();
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }
}
