/**
 * Configuration - Environment variables with defaults for local development.
 */
export const config = {
  port: parseInt(process.env.PORT || '4093'),
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5437'),
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres',
    database: process.env.DB_NAME || 'inventory_db',
  },
  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9093').split(','),
    clientId: 'inventory-service',
    groupId: 'inventory-service-group',
  },
};
