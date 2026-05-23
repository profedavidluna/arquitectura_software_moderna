// =============================================================================
// Product Service - Configuration
// =============================================================================
// Centralized configuration using environment variables with sensible defaults.
// In SOA, each service manages its own configuration independently.
// =============================================================================

export const config = {
  // Service configuration
  port: parseInt(process.env.PORT || '4091'),
  serviceName: 'product-service',

  // Database configuration (Database per Service pattern)
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5437'),
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres',
    database: process.env.DB_NAME || 'product_db',
  },

  // Kafka configuration (Enterprise Service Bus)
  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9093').split(','),
    clientId: 'product-service',
    groupId: 'product-service-group',
  },
};
