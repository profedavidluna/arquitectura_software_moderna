# Kafka Configuration Guide

## Overview

Apache Kafka is used as the message broker for asynchronous event-driven communication between microservices. This document describes the topic structure, event schemas, consumer groups, and cluster configuration.

## Architecture

### Event-Driven Communication

```
Service A → Kafka Topic → Service B (Consumer)
                       → Service C (Consumer)
                       → Service D (Consumer)
```

### Kafka Cluster Configuration

| Component | Configuration |
|-----------|---------------|
| Brokers | 3 nodes (kafka-broker-1, kafka-broker-2, kafka-broker-3) |
| Zookeeper | 1 node (zookeeper) |
| Partitions | 3 per topic |
| Replication Factor | 2 |
| Log Retention | 168 hours (7 days) |
| Log Retention Size | 1 GB per topic |
| Consumer Groups | One per service |

### Broker Configuration

| Broker | Container Name | Internal Port | External Port |
|--------|---------------|---------------|---------------|
| 1 | kafka-broker-1 | 29092 | 9092 |
| 2 | kafka-broker-2 | 29092 | 9093 |
| 3 | kafka-broker-3 | 29092 | 9094 |

### Kafka Configuration

Key configuration settings:
- **num.partitions**: 3 (default partitions per topic)
- **default.replication.factor**: 2 (ensures data durability)
- **log.retention.hours**: 168 (7 days)
- **log.retention.bytes**: 1073741824 (1 GB per topic)
- **offsets.topic.replication.factor**: 2
- **transaction.state.log.replication.factor**: 2

## Topics

### Order Topics

#### order.created
**Description**: Published when a new order is created

**Producer**: Order Service

**Consumers**: Inventory Service, Payment Service, Notification Service

**Schema**:
```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "cartId": "uuid",
  "status": "PENDING",
  "totalAmount": 99.99,
  "items": [...],
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### order.confirmed
**Description**: Published when an order is confirmed

**Producer**: Order Service

**Consumers**: Inventory Service, Notification Service

**Schema**:
```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "status": "CONFIRMED",
  "confirmedAt": "2024-01-15T10:35:00Z"
}
```

#### order.shipped
**Description**: Published when an order is shipped

**Producer**: Order Service

**Consumers**: Notification Service

**Schema**:
```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "trackingNumber": "123456",
  "carrier": "FedEx",
  "shippedAt": "2024-01-15T14:00:00Z"
}
```

#### order.cancelled
**Description**: Published when an order is cancelled

**Producer**: Order Service

**Consumers**: Inventory Service, Payment Service, Notification Service

**Schema**:
```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "cancelledAt": "2024-01-15T11:00:00Z",
  "reason": "Customer requested cancellation"
}
```

### Payment Topics

#### payment.processed
**Description**: Published when a payment is processed

**Producer**: Payment Service

**Consumers**: Order Service, Notification Service

**Schema**:
```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "amount": 99.99,
  "status": "SUCCESS",
  "paymentMethod": "credit_card",
  "processedAt": "2024-01-15T10:36:00Z"
}
```

#### payment.failed
**Description**: Published when a payment fails

**Producer**: Payment Service

**Consumers**: Order Service, Notification Service

**Schema**:
```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "amount": 99.99,
  "error": "Insufficient funds",
  "failedAt": "2024-01-15T10:36:00Z"
}
```

### Inventory Topics

#### inventory.reserved
**Description**: Published when inventory is reserved

**Producer**: Inventory Service

**Consumers**: Order Service

**Schema**:
```json
{
  "reservationId": "uuid",
  "orderId": "uuid",
  "productId": "uuid",
  "quantity": 2,
  "reservedAt": "2024-01-15T10:34:00Z",
  "expiresAt": "2024-01-15T11:04:00Z"
}
```

#### inventory.depleted
**Description**: Published when inventory is depleted

**Producer**: Inventory Service

**Consumers**: Product Service, Analytics Service

**Schema**:
```json
{
  "productId": "uuid",
  "quantityDepleted": 2,
  "orderId": "uuid",
  "depletedAt": "2024-01-15T10:37:00Z"
}
```

### User Topics

#### user.registered
**Description**: Published when a new user is registered

**Producer**: User Service

**Consumers**: Notification Service

**Schema**:
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "username": "username",
  "registeredAt": "2024-01-15T10:30:00Z"
}
```

#### user.updated
**Description**: Published when a user profile is updated

**Producer**: User Service

**Consumers**: Notification Service

**Schema**:
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "username": "username",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

## Consumer Groups

### user-service-group
**Consumes**: user.registered, user.updated

### product-service-group
**Consumes**: (none)

### cart-service-group
**Consumes**: (none)

### order-service-group
**Consumes**: order.created, order.confirmed, order.shipped, order.cancelled, payment.processed, payment.failed, inventory.reserved, inventory.depleted

### payment-service-group
**Consumes**: order.created

### inventory-service-group
**Consumes**: order.created, order.cancelled

### notification-service-group
**Consumes**: order.created, order.confirmed, order.shipped, order.cancelled, payment.processed, payment.failed, user.registered

### analytics-service-group
**Consumes**: order.created, order.confirmed, order.shipped, order.cancelled, payment.processed, payment.failed, inventory.reserved, inventory.depleted, user.registered, user.updated

## Setup Instructions

### Prerequisites

- Docker and Docker Compose installed
- At least 8GB RAM available for Docker

### Starting the Kafka Cluster

```bash
# Start the entire stack including Kafka
docker-compose up -d

# Or start only Kafka and Zookeeper
docker-compose up -d zookeeper kafka-broker-1 kafka-broker-2 kafka-broker-3

# Verify Kafka brokers are running
docker-compose ps kafka
```

### Using the Setup Script

```bash
# Make the script executable
chmod +x shared/kafka/topics-init.sh

# Run the setup script
./shared/kafka/topics-init.sh
```

### Manual Setup

1. **Access Kafka Container**
   ```bash
   docker exec -it kafka-broker-1 bash
   ```

2. **Create Topics**
   ```bash
   kafka-topics --create \
     --topic order.created \
     --partitions 3 \
     --replication-factor 2 \
     --bootstrap-server localhost:9092
   ```

3. **List Topics**
   ```bash
   kafka-topics --list --bootstrap-server localhost:9092
   ```

4. **Describe Topic**
   ```bash
   kafka-topics --describe \
     --topic order.created \
     --bootstrap-server localhost:9092
   ```

## Event Processing

### Producer Pattern

```java
// Java example
kafkaTemplate.send("order.created", orderEvent);
```

### Consumer Pattern

```java
// Java example
@KafkaListener(topics = "order.created", groupId = "order-service-group")
public void handleOrderCreated(OrderEvent event) {
    // Process event
}
```

## Monitoring

### Check Consumer Lag

```bash
kafka-consumer-groups --describe \
  --group order-service-group \
  --bootstrap-server localhost:9092
```

### View Topic Details

```bash
kafka-topics --describe \
  --topic order.created \
  --bootstrap-server localhost:9092
```

## Troubleshooting

### Kafka Not Starting
```bash
# Check Zookeeper is running
docker-compose logs zookeeper

# Check Kafka broker logs
docker-compose logs kafka-broker-1
docker-compose logs kafka-broker-2
docker-compose logs kafka-broker-3

# Restart Kafka cluster
docker-compose restart kafka-broker-1 kafka-broker-2 kafka-broker-3
```

### Topics Not Created
```bash
# Check if Kafka is running
docker-compose ps kafka

# View Kafka logs
docker-compose logs kafka-broker-1

# Manually create topics
docker exec -it kafka-broker-1 kafka-topics --create ...
```

### Consumer Not Receiving Messages
- Verify consumer group is configured correctly
- Check topic name matches
- Verify consumer is subscribed to correct topic
- Check for errors in consumer logs
- Verify bootstrap servers: `kafka-broker-1:29092,kafka-broker-2:29093,kafka-broker-3:29094`

### Message Loss
- Verify replication factor is set correctly
- Check broker availability: `docker-compose ps kafka`
- Verify producer acks configuration
- Check for broker failures in logs

### Connection Issues from Microservices
- Ensure microservices use the correct bootstrap servers:
  ```
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-broker-1:29092,kafka-broker-2:29093,kafka-broker-3:29094
  ```
- Verify network connectivity between containers
- Check container logs for errors

## Best Practices

1. **Idempotent Consumers**: Handle duplicate messages
2. **Error Handling**: Implement dead letter queues
3. **Monitoring**: Track consumer lag and broker health
4. **Schema Evolution**: Use schema registry for versioning
5. **Backpressure**: Handle slow consumers gracefully
6. **Resource Management**: Monitor disk usage and log retention
7. **Backup**: Regularly backup Zookeeper data

## Monitoring

### Check Broker Health
```bash
# Check broker API versions
docker exec -it kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092

# List topics
docker exec -it kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092

# Describe topic
docker exec -it kafka-broker-1 kafka-topics --describe --bootstrap-server localhost:9092
```

### Check Consumer Lag
```bash
kafka-consumer-groups --describe \
  --group order-service-group \
  --bootstrap-server localhost:9092
```

### View Cluster Status
```bash
# Check Zookeeper
docker exec -it zookeeper echo stat | nc localhost 2181

# Check Kafka broker metrics
docker exec -it kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092
```

## References

- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Event-Driven Architecture](https://microservices.io/patterns/event-driven-architecture.html)
- [Kafka Patterns](https://www.confluent.io/blog/)

---

**Status**: ✅ Configured
**Last Updated**: April 2024
**Version**: 1.0.0
