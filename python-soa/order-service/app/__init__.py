"""
Order Service - SOA Architecture
==================================
This service manages customer orders and orchestrates the order creation saga.
It coordinates with the Inventory Service via Kafka events to ensure
distributed transaction consistency.

SOA Principle: Service Composability
- Composes functionality from multiple services (Product, Inventory)
- Orchestrates the order saga across service boundaries
- Maintains eventual consistency through event-driven communication

Design Pattern: Saga (Orchestration)
- Order Service acts as the saga orchestrator
- Publishes 'order.created' → waits for 'stock.reserved' or 'stock.insufficient'
- Compensates by publishing 'order.cancelled' if stock is insufficient
"""
