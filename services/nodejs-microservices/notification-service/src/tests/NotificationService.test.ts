import { NotificationServiceImpl } from '../application/NotificationServiceImpl';
import { NotificationType, NotificationStatus } from '../domain/models/Notification';

describe('NotificationServiceImpl', () => {
  let service: NotificationServiceImpl;

  beforeEach(() => {
    service = new NotificationServiceImpl();
  });

  describe('sendNotification', () => {
    it('should send notification and mark as SENT', async () => {
      const result = await service.sendNotification({
        type: NotificationType.WELCOME,
        recipient: 'test@example.com',
        subject: 'Welcome!',
        body: 'Hello, welcome to our platform.',
      });

      expect(result.status).toBe(NotificationStatus.SENT);
      expect(result.recipient).toBe('test@example.com');
      expect(result.sentAt).toBeDefined();
    });

    it('should store notification in history', async () => {
      await service.sendNotification({
        type: NotificationType.WELCOME,
        recipient: 'test@example.com',
        subject: 'Test',
        body: 'Test body',
      });

      const notifications = service.getNotifications();
      expect(notifications).toHaveLength(1);
    });
  });

  describe('handleUserCreated', () => {
    it('should send welcome email', async () => {
      await service.handleUserCreated({ email: 'new@user.com', firstName: 'Alice' });

      const notifications = service.getNotifications();
      expect(notifications).toHaveLength(1);
      expect(notifications[0].type).toBe(NotificationType.WELCOME);
      expect(notifications[0].recipient).toBe('new@user.com');
      expect(notifications[0].subject).toContain('Welcome');
    });
  });

  describe('handleOrderConfirmed', () => {
    it('should send order confirmation email', async () => {
      await service.handleOrderConfirmed({ orderId: 'order-123', userId: 'user@test.com', totalAmount: 99.99 });

      const notifications = service.getNotifications();
      expect(notifications[0].type).toBe(NotificationType.ORDER_CONFIRMATION);
      expect(notifications[0].subject).toContain('order-123');
    });
  });

  describe('handlePaymentCompleted', () => {
    it('should send payment receipt', async () => {
      await service.handlePaymentCompleted({ orderId: 'order-1', userId: 'user@test.com', amount: 50.00, transactionId: 'txn_abc' });

      const notifications = service.getNotifications();
      expect(notifications[0].type).toBe(NotificationType.PAYMENT_RECEIVED);
      expect(notifications[0].body).toContain('50');
    });
  });

  describe('handlePaymentRefunded', () => {
    it('should send refund notification', async () => {
      await service.handlePaymentRefunded({ orderId: 'order-1', userId: 'user@test.com', amount: 25.00 });

      const notifications = service.getNotifications();
      expect(notifications[0].type).toBe(NotificationType.PAYMENT_REFUNDED);
      expect(notifications[0].subject).toContain('Refund');
    });
  });
});
