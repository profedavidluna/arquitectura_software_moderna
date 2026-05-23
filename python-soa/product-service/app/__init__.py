"""
Product Service - SOA Architecture
====================================
This service is responsible for managing the product catalog.
It publishes events when products are created or updated,
allowing other services to react asynchronously via Kafka.

SOA Principle: Service Autonomy
- This service owns its data (product_db)
- Communicates only through well-defined contracts (REST API + Events)
- Can be deployed, scaled, and maintained independently
"""
