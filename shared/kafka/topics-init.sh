#!/bin/bash

# Kafka Topics Initialization Script
# This script creates all required Kafka topics for the ecommerce platform

set -e

KAFKA_BROKER="localhost:9092"

echo "=== Kafka Topics Initialization ==="
echo "Kafka Broker: $KAFKA_BROKER"
echo ""

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
for i in {1..30}; do
  if docker exec -it kafka-1 kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "Kafka is ready!"
    break
  fi
  echo "Waiting... ($i/30)"
  sleep 2
done

# Check if Kafka is running
if ! docker exec -it kafka-1 kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; then
  echo "ERROR: Kafka is not running. Please start it with: docker-compose up -d"
  exit 1
fi

# Create Order Topics
echo "Creating Order Topics..."
kafka-topics --create \
  --topic order.created \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

kafka-topics --create \
  --topic order.confirmed \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

kafka-topics --create \
  --topic order.shipped \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

kafka-topics --create \
  --topic order.cancelled \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

echo "Order Topics created successfully!"
echo ""

# Create Payment Topics
echo "Creating Payment Topics..."
kafka-topics --create \
  --topic payment.processed \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

kafka-topics --create \
  --topic payment.failed \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

echo "Payment Topics created successfully!"
echo ""

# Create Inventory Topics
echo "Creating Inventory Topics..."
kafka-topics --create \
  --topic inventory.reserved \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

kafka-topics --create \
  --topic inventory.depleted \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

echo "Inventory Topics created successfully!"
echo ""

# Create User Topics
echo "Creating User Topics..."
kafka-topics --create \
  --topic user.registered \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

kafka-topics --create \
  --topic user.updated \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server $KAFKA_BROKER \
  --config retention.ms=604800000

echo "User Topics created successfully!"
echo ""

# List all topics
echo "=== All Kafka Topics ==="
kafka-topics --list --bootstrap-server $KAFKA_BROKER
echo ""

echo "=== Kafka Topics Initialization Complete ==="
echo ""
echo "Topics created:"
echo "  Order Topics: order.created, order.confirmed, order.shipped, order.cancelled"
echo "  Payment Topics: payment.processed, payment.failed"
echo "  Inventory Topics: inventory.reserved, inventory.depleted"
echo "  User Topics: user.registered, user.updated"
echo ""
echo "Configuration:"
echo "  Partitions: 3 per topic"
echo "  Replication Factor: 2"
echo "  Retention: 7 days (604800000 ms)"
echo ""
