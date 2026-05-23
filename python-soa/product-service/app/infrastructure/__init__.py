"""
Infrastructure Layer - Product Service
========================================
Contains all technical implementations:
- Persistence: Database access via asyncpg
- Messaging: Kafka event publishing via aiokafka
- Web: FastAPI routes and DTOs

SOA Principle: Service Loose Coupling
- Infrastructure details are isolated in this layer
- Changing the database or message broker doesn't affect business logic
- Each concern (persistence, messaging, web) is in its own package
"""
