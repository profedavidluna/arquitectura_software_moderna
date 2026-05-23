// =============================================================================
// Order Service - Configuration
// =============================================================================

export const config = {
  port: parseInt(process.env.PORT || '4092'),
  serviceName: 'order-service',

  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5437'),
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres',
    database: process.env.DB_NAME || 'order_db',
  },

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9093').split(','),
    clientId: 'order-service',
    groupId: 'order-service-group',
  },
};
