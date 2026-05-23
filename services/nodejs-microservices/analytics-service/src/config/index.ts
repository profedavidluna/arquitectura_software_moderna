export const config = {
  port: parseInt(process.env.PORT || '3089', 10),
  serviceName: 'analytics-service',

  database: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5440', 10),
    name: process.env.DB_NAME || 'analytics_db',
    user: process.env.DB_USER || 'admin',
    password: process.env.DB_PASSWORD || 'admin123',
  },

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9096').split(','),
    clientId: 'analytics-service',
    groupId: 'analytics-service-group',
  },
};
