import { Router, Request, Response } from 'express';
import { OrderServiceImpl } from '../../application/OrderServiceImpl';
import { toOrderResponse } from './dto';

export class OrderController {
  public readonly router: Router;

  constructor(private readonly orderService: OrderServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/orders', this.createOrder.bind(this));
    this.router.get('/orders/:id', this.getOrderById.bind(this));
    this.router.get('/orders', this.getOrdersByUser.bind(this));
    this.router.post('/orders/:id/cancel', this.cancelOrder.bind(this));
  }

  private async createOrder(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.userId || !req.body.cartId || !req.body.paymentMethod) {
        res.status(400).json({ error: 'Missing required fields: userId, cartId, paymentMethod' });
        return;
      }
      const order = await this.orderService.createOrder(req.body);
      res.status(201).json(toOrderResponse(order));
    } catch (error: any) {
      console.error('[OrderController] Create order error:', error.message);
      if (error.message.includes('Cart') || error.message.includes('empty')) {
        res.status(400).json({ error: error.message });
      } else if (error.message.includes('failed')) {
        res.status(503).json({ error: error.message });
      } else {
        res.status(500).json({ error: 'Internal server error' });
      }
    }
  }

  private async getOrderById(req: Request, res: Response): Promise<void> {
    try {
      const order = await this.orderService.getOrderById(req.params.id);
      if (!order) { res.status(404).json({ error: 'Order not found' }); return; }
      res.json(toOrderResponse(order));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async getOrdersByUser(req: Request, res: Response): Promise<void> {
    try {
      const userId = req.query.userId as string;
      if (!userId) { res.status(400).json({ error: 'userId query parameter required' }); return; }
      const page = parseInt(req.query.page as string) || 1;
      const limit = parseInt(req.query.limit as string) || 20;
      const orders = await this.orderService.getOrdersByUserId(userId, page, limit);
      res.json(orders.map(toOrderResponse));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async cancelOrder(req: Request, res: Response): Promise<void> {
    try {
      const order = await this.orderService.cancelOrder(req.params.id);
      if (!order) { res.status(404).json({ error: 'Order not found' }); return; }
      res.json(toOrderResponse(order));
    } catch (error: any) {
      res.status(400).json({ error: error.message });
    }
  }
}
