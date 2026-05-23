/**
 * Configuration Module
 * 
 * Centralizes all environment-based configuration.
 * In microservices, each service has its own configuration
 * that can be overridden via environment variables (12-Factor App principle).
 */
export const config = {
  port: parseInt(process.env.PORT || '3082', 10),
  serviceName: 'user-service',

  database: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5440', 10),
    name: process.env.DB_NAME || 'user_db',
    user: process.env.DB_USER || 'admin',
    password: process.env.DB_PASSWORD || 'admin123',
  },

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9096').split(','),
    clientId: 'user-service',
    groupId: 'user-service-group',
  },
};
