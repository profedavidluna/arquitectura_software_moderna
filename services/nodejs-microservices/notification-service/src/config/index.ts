export const config = {
  port: parseInt(process.env.PORT || '3088', 10),
  serviceName: 'notification-service',

  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9096').split(','),
    clientId: 'notification-service',
    groupId: 'notification-service-group',
  },

  smtp: {
    host: process.env.SMTP_HOST || 'smtp.example.com',
    port: parseInt(process.env.SMTP_PORT || '587', 10),
    user: process.env.SMTP_USER || '',
    password: process.env.SMTP_PASSWORD || '',
  },
};
