/**
 * Notification Service Implementation
 * 
 * This service is purely event-driven - it has no REST API endpoints.
 * It listens to events from multiple services and sends notifications.
 * 
 * In production, this would integrate with:
 * - SendGrid/SES for email
 * - Twilio for SMS
 * - Firebase for push notifications
 * 
 * For this demo, notifications are logged to console (simulated).
 */
import { v4 as uuidv4 } from 'uuid';
import { Notification, NotificationType, NotificationStatus } from '../domain/models/Notification';
import { INotificationService, SendNotificationDTO } from '../domain/interfaces/INotificationService';

export class NotificationServiceImpl implements INotificationService {
  // In-memory store for demo (production would use a database)
  private notifications: Notification[] = [];

  async sendNotification(dto: SendNotificationDTO): Promise<Notification> {
    const notification: Notification = {
      id: uuidv4(),
      type: dto.type,
      recipient: dto.recipient,
      subject: dto.subject,
      body: dto.body,
      status: NotificationStatus.PENDING,
      createdAt: new Date(),
    };

    // Simulate sending email
    try {
      await this.simulateSendEmail(notification);
      notification.status = NotificationStatus.SENT;
      notification.sentAt = new Date();
      console.log(`[📧 Email Sent] To: ${dto.recipient} | Subject: ${dto.subject}`);
    } catch (error) {
      notification.status = NotificationStatus.FAILED;
      console.error(`[📧 Email Failed] To: ${dto.recipient} | Error: ${error}`);
    }

    this.notifications.push(notification);
    return notification;
  }

  async handleUserCreated(data: any): Promise<void> {
    await this.sendNotification({
      type: NotificationType.WELCOME,
      recipient: data.email || 'user@example.com',
      subject: 'Welcome to our E-Commerce Platform!',
      body: `Hello ${data.firstName || 'User'},\n\nWelcome! Your account has been created successfully.\n\nHappy shopping!`,
    });
  }

  async handleOrderConfirmed(data: any): Promise<void> {
    await this.sendNotification({
      type: NotificationType.ORDER_CONFIRMATION,
      recipient: data.userId || 'customer@example.com',
      subject: `Order Confirmed - #${data.orderId}`,
      body: `Your order #${data.orderId} has been confirmed!\n\nTotal: $${data.totalAmount || '0.00'}\n\nThank you for your purchase.`,
    });
  }

  async handleOrderCancelled(data: any): Promise<void> {
    await this.sendNotification({
      type: NotificationType.ORDER_CANCELLED,
      recipient: data.userId || 'customer@example.com',
      subject: `Order Cancelled - #${data.orderId}`,
      body: `Your order #${data.orderId} has been cancelled.\n\nIf you have any questions, please contact support.`,
    });
  }

  async handlePaymentCompleted(data: any): Promise<void> {
    await this.sendNotification({
      type: NotificationType.PAYMENT_RECEIVED,
      recipient: data.userId || 'customer@example.com',
      subject: `Payment Received - $${data.amount}`,
      body: `We've received your payment of $${data.amount} for order #${data.orderId}.\n\nTransaction ID: ${data.transactionId || 'N/A'}`,
    });
  }

  async handlePaymentRefunded(data: any): Promise<void> {
    await this.sendNotification({
      type: NotificationType.PAYMENT_REFUNDED,
      recipient: data.userId || 'customer@example.com',
      subject: `Refund Processed - $${data.amount}`,
      body: `A refund of $${data.amount} has been processed for order #${data.orderId}.\n\nPlease allow 5-10 business days for the refund to appear.`,
    });
  }

  /**
   * Simulated email sending.
   * In production, replace with actual SMTP/API call.
   */
  private async simulateSendEmail(notification: Notification): Promise<void> {
    // Simulate network latency
    await new Promise(resolve => setTimeout(resolve, 50));

    // Log the "sent" email for demonstration
    console.log('─'.repeat(60));
    console.log(`📧 NOTIFICATION [${notification.type}]`);
    console.log(`   To: ${notification.recipient}`);
    console.log(`   Subject: ${notification.subject}`);
    console.log(`   Body: ${notification.body.substring(0, 100)}...`);
    console.log('─'.repeat(60));
  }

  getNotifications(): Notification[] {
    return this.notifications;
  }
}
