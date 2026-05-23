// =============================================================================
// Order Controller - HTTP API Layer
// =============================================================================
// Handles HTTP requests for order management.
// Delegates business logic to the IOrderService interface (DIP).
// =============================================================================

import { Router, Request, Response } from 'express';
import { IOrderService } from '../../domain/service/IOrderService';
import { validateCreateOrderDto } from './dto';

export class OrderController {
  public readonly router: Router;

  constructor(private readonly orderService: IOrderService) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.get('/', this.findAll.bind(this));
    this.router.get('/:id', this.findById.bind(this));
    this.router.post('/', this.create.bind(this));
    this.router.put('/:id/cancel', this.cancel.bind(this));
    this.router.get('/user/:userId', this.findByUserId.bind(this));
  }

  /**
   * GET /api/orders - List all orders
   */
  private async findAll(_req: Request, res: Response): Promise<void> {
    try {
      const orders = await this.orderService.findAll();
      res.json({ success: true, data: orders });
    } catch (error) {
      console.error('[OrderController] Error in findAll:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * GET /api/orders/:id - Get order by ID
   */
  private async findById(req: Request, res: Response): Promise<void> {
    try {
      const order = await this.orderService.findById(req.params.id);
      if (!order) {
        res.status(404).json({ success: false, error: 'Order not found' });
        return;
      }
      res.json({ success: true, data: order });
    } catch (error) {
      console.error('[OrderController] Error in findById:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * POST /api/orders - Create a new order (starts Saga)
   */
  private async create(req: Request, res: Response): Promise<void> {
    try {
      const validationError = validateCreateOrderDto(req.body);
      if (validationError) {
        res.status(400).json({ success: false, error: validationError });
        return;
      }

      const order = await this.orderService.create(req.body);
      res.status(201).json({
        success: true,
        data: order,
        message: 'Order created in PENDING state. Waiting for inventory confirmation.',
      });
    } catch (error) {
      console.error('[OrderController] Error in create:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * PUT /api/orders/:id/cancel - Cancel an order (triggers compensation)
   */
  private async cancel(req: Request, res: Response): Promise<void> {
    try {
      const order = await this.orderService.cancel(req.params.id);
      if (!order) {
        res.status(404).json({ success: false, error: 'Order not found' });
        return;
      }
      res.json({
        success: true,
        data: order,
        message: 'Order cancelled. Compensating transaction initiated.',
      });
    } catch (error) {
      console.error('[OrderController] Error in cancel:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }

  /**
   * GET /api/orders/user/:userId - Get orders by user
   */
  private async findByUserId(req: Request, res: Response): Promise<void> {
    try {
      const orders = await this.orderService.findByUserId(req.params.userId);
      res.json({ success: true, data: orders });
    } catch (error) {
      console.error('[OrderController] Error in findByUserId:', error);
      res.status(500).json({ success: false, error: 'Internal server error' });
    }
  }
}
