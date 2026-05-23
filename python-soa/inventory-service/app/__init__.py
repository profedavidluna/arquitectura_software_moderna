"""
Inventory Service - SOA Architecture
=======================================
This service manages product stock levels and participates in the
order creation saga as a saga participant.

SOA Principle: Service Statelessness
- Each request/event is processed independently
- State is persisted in the database, not in memory
- Service can be restarted without losing saga progress

Saga Role: Participant
- Receives 'order.created' events
- Attempts to reserve stock for all items
- Responds with 'stock.reserved' or 'stock.insufficient'
- Handles compensation via 'order.cancelled' (releases stock)
"""
