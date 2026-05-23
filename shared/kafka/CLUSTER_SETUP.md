# Kafka Cluster Setup Guide

## Overview

This document provides detailed information about the Kafka cluster configuration for the ecommerce platform.

## Cluster Architecture

### Components

| Component | Container Name | Image | Port |
|-----------|---------------|-------|------|
| Zookeeper | zookeeper | confluentinc/cp-zookeeper:7.4.0 | 2181 |
| Kafka Broker 1 | kafka-broker-1 | confluentinc/cp-kafka:7.4.0 | 9092 |
| Kafka Broker 2 | kafka-broker-2 | confluentinc/cp-kafka:7.4.0 | 9093 |
| Kafka Broker 3 | kafka-broker-3 | confluentinc/cp-kafka:7.4.0 | 9094 |

### Network Configuration

All services are connected to the `ecommerce-network` Docker network for inter-container communication.

## Configuration Details

### Zookeeper Configuration

- **Client Port**: 2181
- **Tick Time**: 2000ms
- **Data Directory**: /var/lib/zookeeper/data
- **Log Directory**: /var/lib/zookeeper/log

### Kafka Broker Configuration

All 3 brokers share the same configuration:

| Setting | Value | Description |
|---------|-------|-------------|
| num.partitions | 3 | Default partitions per topic |
| default.replication.factor | 2 | Default replication factor |
| log.retention.hours | 168 | 7 days retention |
| log.retention.bytes | 1073741824 | 1 GB per topic |
| log.segment.bytes | 1073741824 | 1 GB per segment |
| log.retention.check.interval.ms | 300000 | 5 minutes check interval |
| offsets.topic.replication.factor | 2 | Replication for offset topic |
| transaction.state.log.replication.factor | 2 | Replication for transaction log |
| transaction.state.log.min.isr | 1 | Minimum in-sync replicas |
| auto.create.topics.enable | true | Auto-create topics on first use |

### Broker-Specific Configuration

| Broker | Broker ID | Internal Listener | External Port |
|--------|-----------|-------------------|---------------|
| kafka-broker-1 | 1 | kafka-broker-1:29092 | 9092 |
| kafka-broker-2 | 2 | kafka-broker-2:29092 | 9093 |
| kafka-broker-3 | 3 | kafka-broker-3:29092 | 9094 |

## Usage

### Starting the Cluster

```bash
# Start the entire stack
docker-compose up -d

# Or start only Kafka and Zookeeper
docker-compose up -d zookeeper kafka-broker-1 kafka-broker-2 kafka-broker-3
```

### Verifying Cluster Status

```bash
# Check container status
docker-compose ps kafka

# Check Zookeeper
docker exec -it zookeeper echo stat | nc localhost 2181

# Check Kafka brokers
docker exec -it kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Creating Topics

```bash
# Create a topic manually
docker exec -it kafka-broker-1 kafka-topics --create \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server localhost:9092

# List topics
docker exec -it kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092

# Describe topic
docker exec -it kafka-broker-1 kafka-topics --describe \
  --topic test-topic \
  --bootstrap-server localhost:9092
```

### Using from Microservices

Microservices connect to Kafka using the internal Docker network:

```yaml
# Example from docker-compose.yml
environment:
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-broker-1:29092,kafka-broker-2:29093,kafka-broker-3:29094
```

## Monitoring

### Check Consumer Lag

```bash
kafka-consumer-groups --describe \
  --group order-service-group \
  --bootstrap-server kafka-broker-1:29092
```

### View Broker Metrics

```bash
# Check broker health
docker exec -it kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092

# List topics
docker exec -it kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092
```

## Troubleshooting

### Common Issues

1. **Kafka not starting**
   - Check Zookeeper is running: `docker-compose logs zookeeper`
   - Check Kafka logs: `docker-compose logs kafka-broker-1`

2. **Topics not created**
   - Verify Kafka is running: `docker-compose ps kafka`
   - Manually create topics using the kafka-topics command

3. **Consumer not receiving messages**
   - Verify consumer group configuration
   - Check topic name matches
   - Verify bootstrap servers are correct

## Best Practices

1. **Always use replication factor of 2** for data durability
2. **Use 3 partitions per topic** for parallelism
3. **Monitor consumer lag** to detect processing issues
4. **Set appropriate retention** based on business requirements
5. **Use internal Docker network** for service-to-service communication

## References

- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Docker Images](https://docs.confluent.io/platform/current/installation/docker/installation.html)
