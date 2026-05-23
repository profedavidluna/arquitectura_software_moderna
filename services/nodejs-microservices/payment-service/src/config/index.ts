export const config = {
  port: parseInt(process.env.PORT || '3086', 10),
  serviceName: 'payment-service',

  database: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5440', 10),
    name: process.env.DB_NAME || 'payment_db',
    user: process.env.DB_USER || 'admin',
    password: process.env.DB_PASSWORD || 'admin123',
  },

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9096').split(','),
    clientId: 'payment-service',
    groupId: 'payment-service-group',
  },
};
