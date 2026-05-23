export const config = {
  port: parseInt(process.env.PORT || '3085', 10),
  serviceName: 'order-service',

  database: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5440', 10),
    name: process.env.DB_NAME || 'order_db',
    user: process.env.DB_USER || 'admin',
    password: process.env.DB_PASSWORD || 'admin123',
  },

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9096').split(','),
    clientId: 'order-service',
    groupId: 'order-service-group',
  },

  // URLs for Saga orchestration - calls to other services
  cartServiceUrl: process.env.CART_SERVICE_URL || 'http://localhost:3084',
  inventoryServiceUrl: process.env.INVENTORY_SERVICE_URL || 'http://localhost:3087',
  paymentServiceUrl: process.env.PAYMENT_SERVICE_URL || 'http://localhost:3086',
};
