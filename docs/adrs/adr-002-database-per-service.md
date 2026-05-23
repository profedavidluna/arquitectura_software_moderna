# ADR-002: Database Per Service Pattern

**Status**: Accepted

**Context**
In a microservices architecture, we need to decide how to manage data persistence. Each service has its own data requirements and should be independently deployable and scalable.

**Decision**
Each microservice will have its own dedicated database. Services will not share databases, and database schemas will be owned and managed by the service team.

**Consequences**

*Positive*:
- **Loose Coupling**: Services are independent and can evolve their schemas without coordination
- **Technology Flexibility**: Each service can choose the database technology that best fits its needs
- **Scalability**: Databases can be scaled independently based on service load
- **Fault Isolation**: Database failures affect only one service
- **Team Autonomy**: Teams can manage their own database schemas and migrations
- **Performance**: Optimized queries and indexes for specific service needs

*Negative*:
- **Distributed Transactions**: Cannot use ACID transactions across services
- **Data Consistency**: Eventual consistency instead of strong consistency
- **Data Duplication**: Some data may need to be duplicated across services
- **Query Complexity**: Cannot join data across service boundaries
- **Operational Overhead**: More databases to manage, backup, and monitor

**Alternatives Considered**

1. **Shared Database**: All services share a single database
   - *Why rejected*: Creates tight coupling, violates service boundaries, single point of failure, difficult to scale

2. **Database per Business Domain**: Group of related services share a database
   - *Why rejected*: Still creates coupling between services, less flexible than per-service approach

3. **Polyglot Persistence with Shared Read Models**: Each service has its own write database but shares read-optimized views
   - *Why rejected*: Too complex for educational purposes, adds significant operational overhead

**Implementation Details**

- **Database Technologies**:
  - PostgreSQL for relational data (users, products, orders, payments)
  - Redis for caching and session management
  - Elasticsearch for search and analytics
  - Each service can choose the most appropriate database

- **Data Synchronization**:
  - Use Kafka events for data synchronization between services
  - Implement event sourcing and CQRS patterns where appropriate
  - Use materialized views for read-optimized data

- **Database Management**:
  - Each service includes database migration scripts
  - Use Flyway or Liquibase for schema versioning
  - Database credentials managed per service

- **Backup Strategy**:
  - Each database backed up independently
  - Point-in-time recovery for critical services

**Related ADRs**
- ADR-001: Use Microservices Architecture
- ADR-003: Kafka for Asynchronous Communication
- ADR-008: Saga Pattern for Distributed Transactions

**Last Updated**: April 2024