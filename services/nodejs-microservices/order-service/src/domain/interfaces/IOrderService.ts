import { Order } from '../models/Order';

export interface CreateOrderDTO {
  userId: string;
  cartId: string;
  shippingAddressId?: string;
  notes?: string;
  paymentMethod: string;
}

export interface IOrderService {
  createOrder(dto: CreateOrderDTO): Promise<Order>;
  getOrderById(id: string): Promise<Order | null>;
  getOrdersByUserId(userId: string, page: number, limit: number): Promise<Order[]>;
  cancelOrder(id: string): Promise<Order | null>;
}
