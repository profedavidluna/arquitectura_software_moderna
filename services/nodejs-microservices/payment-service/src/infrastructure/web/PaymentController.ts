import { Router, Request, Response } from 'express';
import { PaymentServiceImpl } from '../../application/PaymentServiceImpl';
import { toPaymentResponse, toRefundResponse } from './dto';

export class PaymentController {
  public readonly router: Router;

  constructor(private readonly paymentService: PaymentServiceImpl) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/payments/process', this.processPayment.bind(this));
    this.router.get('/payments/:id', this.getPayment.bind(this));
    this.router.get('/payments/order/:orderId', this.getPaymentByOrder.bind(this));
    this.router.post('/payments/:id/refund', this.refundPayment.bind(this));
  }

  private async processPayment(req: Request, res: Response): Promise<void> {
    try {
      if (!req.body.orderId || !req.body.userId || !req.body.amount || !req.body.method) {
        res.status(400).json({ error: 'Missing required fields: orderId, userId, amount, method' });
        return;
      }
      const payment = await this.paymentService.processPayment(req.body);
      res.status(201).json(toPaymentResponse(payment));
    } catch (error: any) {
      console.error('[PaymentController] Process error:', error.message);
      res.status(402).json({ error: error.message });
    }
  }

  private async getPayment(req: Request, res: Response): Promise<void> {
    try {
      const payment = await this.paymentService.getPaymentById(req.params.id);
      if (!payment) { res.status(404).json({ error: 'Payment not found' }); return; }
      res.json(toPaymentResponse(payment));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async getPaymentByOrder(req: Request, res: Response): Promise<void> {
    try {
      const payment = await this.paymentService.getPaymentByOrderId(req.params.orderId);
      if (!payment) { res.status(404).json({ error: 'Payment not found' }); return; }
      res.json(toPaymentResponse(payment));
    } catch (error) {
      res.status(500).json({ error: 'Internal server error' });
    }
  }

  private async refundPayment(req: Request, res: Response): Promise<void> {
    try {
      const refund = await this.paymentService.refundPayment(req.params.id, req.body);
      res.status(201).json(toRefundResponse(refund));
    } catch (error: any) {
      if (error.message.includes('not found')) {
        res.status(404).json({ error: error.message });
      } else {
        res.status(400).json({ error: error.message });
      }
    }
  }
}
