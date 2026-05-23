export interface Cart {
  id: string;
  userId: string;
  status: CartStatus;
  couponCode?: string;
  discountPercent: number;
  createdAt: Date;
  updatedAt: Date;
}

export enum CartStatus {
  ACTIVE = 'ACTIVE',
  CHECKED_OUT = 'CHECKED_OUT',
  ABANDONED = 'ABANDONED',
}

export interface CartItem {
  id: string;
  cartId: string;
  productId: string;
  productName?: string;
  quantity: number;
  unitPrice: number;
  createdAt: Date;
}
