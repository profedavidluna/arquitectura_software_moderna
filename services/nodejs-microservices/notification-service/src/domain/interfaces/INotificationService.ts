import { Notification, NotificationType } from '../models/Notification';

export interface SendNotificationDTO {
  type: NotificationType;
  recipient: string;
  subject: string;
  body: string;
}

export interface INotificationService {
  sendNotification(dto: SendNotificationDTO): Promise<Notification>;
  handleUserCreated(data: any): Promise<void>;
  handleOrderConfirmed(data: any): Promise<void>;
  handleOrderCancelled(data: any): Promise<void>;
  handlePaymentCompleted(data: any): Promise<void>;
  handlePaymentRefunded(data: any): Promise<void>;
}
