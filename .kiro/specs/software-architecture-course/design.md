# Design Document: Software Architecture Course - Ecommerce Microservices

## Overview

This is a comprehensive 32-hour software architecture course featuring a practical ecommerce project implemented across three architectural patterns (SOA, MVC, Microservices) and three programming languages (Java, .NET, NodeJS). The course emphasizes hands-on learning with production-grade patterns, resilience strategies, and modern DevOps practices. The microservices architecture serves as the capstone, demonstrating distributed system design with asynchronous messaging, API gateways, centralized authentication, and comprehensive monitoring.

## Course Structure

### Duration Breakdown
- **8 hours**: SOA (Service-Oriented Architecture) fundamentals and implementation
- **8 hours**: MVC (Model-View-Controller) pattern and web application design
- **8 hours**: Microservices architecture with distributed systems patterns
- **8 hours**: Advanced theory covering SOLID principles, design patterns, resilience, and observability

### Repository Structure

```
software-architecture-course/
├── main (default branch - documentation, setup, theory)
├── soa-architecture (SOA implementation branch)
│   ├── java-soa/
│   ├── dotnet-soa/
│   ├── nodejs-soa/
│   └── python-soa/
├── mvc-architecture (MVC implementation branch)
│   ├── java-mvc/
│   ├── dotnet-mvc/
│   ├── nodejs-mvc/
│   └── python-mvc/
├── microservices-architecture (Microservices implementation branch)
│   ├── java-microservices/
│   ├── dotnet-microservices/
│   ├── nodejs-microservices/
│   └── python-microservices/
├── docs/
│   ├── architecture/
│   ├── adrs/
│   ├── diagrams/
│   └── guides/
└── shared/
    ├── docker-compose.yml
    ├── keycloak-config/
    └── database-schemas/
```

## Ecommerce Project Scope

### Core Features
1. **Authentication & Authorization**: Centralized via Keycloak
2. **Product Catalog**: Browse and search products
3. **Shopping Cart**: Add/remove items, manage quantities
4. **Checkout Process**: Multi-step order creation
5. **Payment Processing**: Integration with payment gateway
6. **Inventory Management**: Stock tracking and updates
7. **Order Management**: Order creation, tracking, and fulfillment
8. **User Management**: Profile, preferences, order history

### Non-Functional Requirements
- **Scalability**: Support up to 10,000 concurrent users
- **Performance**: API response time < 200ms (p95)
- **Availability**: 99.5% uptime SLA
- **Reliability**: Graceful degradation, circuit breakers, retry logic
- **Security**: OAuth2/OIDC via Keycloak, encrypted data in transit and at rest
- **Observability**: Centralized logging, distributed tracing, metrics collection
- **Testing**: Minimum 80% code coverage, automated CI/CD pipeline

## Microservices Architecture (Capstone)

### Microservices Definition

#### 1. **API Gateway Service**
- **Responsibility**: Single entry point for all client requests
- **Port**: 8080
- **Technology**: Kong or custom implementation
- **Capabilities**:
  - Request routing to appropriate microservices
  - Authentication token validation
  - Rate limiting and throttling
  - Request/response logging
  - CORS handling

#### 2. **Authentication Service**
- **Responsibility**: User authentication and token management
- **Port**: 8081
- **Technology**: Keycloak (centralized)
- **Capabilities**:
  - User registration and login
  - OAuth2/OIDC token generation
  - Token validation and refresh
  - Role-based access control (RBAC)
  - Multi-tenant support

#### 3. **User Service**
- **Responsibility**: User profile and account management
- **Port**: 8082
- **Database**: PostgreSQL (user_db)
- **Capabilities**:
  - User profile CRUD operations
  - Preference management
  - Address management
  - Account settings

#### 4. **Product Service**
- **Responsibility**: Product catalog and inventory
- **Port**: 8083
- **Database**: PostgreSQL (product_db)
- **Capabilities**:
  - Product CRUD operations
  - Category management
  - Search and filtering
  - Stock level tracking
  - Product images and metadata

#### 5. **Cart Service**
- **Responsibility**: Shopping cart management
- **Port**: 8084
- **Database**: Redis (cache) + PostgreSQL (persistence)
- **Capabilities**:
  - Add/remove items from cart
  - Update quantities
  - Calculate totals and taxes
  - Cart persistence
  - Cart expiration (30 days)

#### 6. **Order Service**
- **Responsibility**: Order creation and management
- **Port**: 8085
- **Database**: PostgreSQL (order_db)
- **Capabilities**:
  - Order creation from cart
  - Order status tracking
  - Order history retrieval
  - Order cancellation
  - Order notifications

#### 7. **Payment Service**
- **Responsibility**: Payment processing and transactions
- **Port**: 8086
- **Database**: PostgreSQL (payment_db)
- **Capabilities**:
  - Payment processing (Stripe/PayPal integration)
  - Transaction recording
  - Refund handling
  - Payment status tracking
  - PCI compliance

#### 8. **Inventory Service**
- **Responsibility**: Stock management and reservations
- **Port**: 8087
- **Database**: PostgreSQL (inventory_db)
- **Capabilities**:
  - Stock level updates
  - Inventory reservations
  - Stock depletion on order confirmation
  - Low stock alerts
  - Inventory reconciliation

#### 9. **Notification Service**
- **Responsibility**: Email and SMS notifications
- **Port**: 8088
- **Technology**: Message queue consumer (Kafka)
- **Capabilities**:
  - Order confirmation emails
  - Payment receipts
  - Shipping notifications
  - Account alerts
  - Template management

#### 10. **Analytics Service**
- **Responsibility**: Business intelligence and reporting
- **Port**: 8089
- **Database**: PostgreSQL (analytics_db)
- **Capabilities**:
  - Sales metrics
  - User behavior tracking
  - Product performance analysis
  - Revenue reporting
  - Trend analysis

### Service Communication Patterns

#### Synchronous Communication (REST APIs)

```
Client → API Gateway → Target Service
         ↓
    Authentication Check
         ↓
    Route to Service
         ↓
    Service Response
```

**API Endpoints Structure**:
```
GET    /api/v1/products              - List products
GET    /api/v1/products/{id}         - Get product details
POST   /api/v1/cart                  - Create cart
GET    /api/v1/cart/{cartId}         - Get cart
PUT    /api/v1/cart/{cartId}/items   - Update cart items
POST   /api/v1/orders                - Create order
GET    /api/v1/orders/{orderId}      - Get order details
POST   /api/v1/payments              - Process payment
GET    /api/v1/users/{userId}        - Get user profile
```

#### Asynchronous Communication (Kafka)

**Event-Driven Architecture**:

```
Service A → Kafka Topic → Service B (Consumer)
                       → Service C (Consumer)
                       → Service D (Consumer)
```

**Key Events**:

1. **order.created**: Order Service → Inventory, Payment, Notification Services
2. **payment.processed**: Payment Service → Order, Notification Services
3. **inventory.reserved**: Inventory Service → Order Service
4. **inventory.depleted**: Inventory Service → Product Service
5. **user.registered**: User Service → Notification Service
6. **order.shipped**: Order Service → Notification Service

**Kafka Configuration**:
- **Brokers**: 3 nodes (for production resilience)
- **Partitions**: 3 per topic (for parallelism)
- **Replication Factor**: 2
- **Consumer Groups**: One per service

### Database Strategy

#### Database Per Service Pattern

Each microservice has its own PostgreSQL database to ensure loose coupling and independent scaling:

```
User Service       → user_db
Product Service    → product_db
Cart Service       → cart_db (+ Redis cache)
Order Service      → order_db
Payment Service    → payment_db
Inventory Service  → inventory_db
Analytics Service  → analytics_db
```

#### Database Schema Highlights

**user_db**:
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  username VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  phone VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN DEFAULT true
);

CREATE TABLE addresses (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  street VARCHAR(255),
  city VARCHAR(100),
  state VARCHAR(50),
  postal_code VARCHAR(20),
  country VARCHAR(100),
  is_default BOOLEAN DEFAULT false
);
```

**product_db**:
```sql
CREATE TABLE products (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  category_id UUID,
  sku VARCHAR(100) UNIQUE,
  image_url VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT
);
```

**order_db**:
```sql
CREATE TABLE orders (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  status VARCHAR(50) DEFAULT 'PENDING',
  total_amount DECIMAL(10, 2),
  tax_amount DECIMAL(10, 2),
  shipping_amount DECIMAL(10, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
  id UUID PRIMARY KEY,
  order_id UUID REFERENCES orders(id),
  product_id UUID,
  quantity INT,
  unit_price DECIMAL(10, 2),
  subtotal DECIMAL(10, 2)
);
```

**inventory_db**:
```sql
CREATE TABLE inventory (
  id UUID PRIMARY KEY,
  product_id UUID UNIQUE NOT NULL,
  quantity_available INT,
  quantity_reserved INT,
  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_transactions (
  id UUID PRIMARY KEY,
  product_id UUID,
  transaction_type VARCHAR(50),
  quantity INT,
  reference_id UUID,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Infrastructure Components

#### API Gateway

**Technology**: Kong or custom Spring Cloud Gateway / ASP.NET Core / Express middleware

**Responsibilities**:
- Request routing based on path patterns
- Authentication token validation
- Rate limiting (100 req/sec per user)
- Request/response transformation
- CORS handling
- Request logging and tracing

**Configuration**:
```yaml
routes:
  - path: /api/v1/products
    service: product-service:8083
  - path: /api/v1/cart
    service: cart-service:8084
  - path: /api/v1/orders
    service: order-service:8085
  - path: /api/v1/payments
    service: payment-service:8086
  - path: /api/v1/users
    service: user-service:8082
```

#### Keycloak (Centralized Authentication)

**Configuration**:
- **Port**: 8180
- **Realm**: ecommerce
- **Clients**: 
  - web-client (frontend SPA)
  - mobile-client (mobile app)
  - service-account (inter-service communication)
- **User Roles**: ADMIN, USER, CUSTOMER, SUPPORT
- **Token Expiry**: 15 minutes (access), 7 days (refresh)

**Integration Pattern**:
```
Client → Keycloak (Login) → Get JWT Token
         ↓
    Include Token in Authorization Header
         ↓
    API Gateway validates token
         ↓
    Forward request to service
```

#### Kafka Message Broker

**Configuration**:
- **Version**: 3.x
- **Brokers**: 3 nodes
- **Zookeeper**: 3 nodes (or KRaft mode)
- **Topics**: 10+ topics for different event types

**Topics**:
```
order.created
order.confirmed
order.shipped
order.cancelled
payment.processed
payment.failed
inventory.reserved
inventory.depleted
user.registered
user.updated
```

#### Redis Cache

**Purpose**: Cart caching and session management

**Configuration**:
- **Port**: 6379
- **Memory**: 2GB
- **Eviction Policy**: allkeys-lru
- **Persistence**: RDB snapshots every 60 seconds

**Usage**:
```
cart:{cartId} → Cart data (TTL: 30 days)
session:{sessionId} → Session data (TTL: 24 hours)
```

#### Docker Compose Stack

**Services**:
- 10 microservices (Java/Node/C# implementations)
- PostgreSQL (7 instances)
- Redis
- Kafka + Zookeeper
- Keycloak
- API Gateway
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Prometheus + Grafana
- Jaeger (distributed tracing)

**Resource Limits**:
- Each service: 512MB RAM, 0.5 CPU
- Databases: 1GB RAM each
- Total stack: ~15GB RAM

### Design Patterns

#### 1. **API Gateway Pattern**
- Single entry point for all client requests
- Handles cross-cutting concerns (auth, logging, rate limiting)
- Implementation: Kong, Spring Cloud Gateway, or custom

#### 2. **Service-to-Service Communication**
- **Synchronous**: REST APIs with circuit breakers
- **Asynchronous**: Kafka event streaming
- **Resilience**: Retry logic, timeouts, fallbacks

#### 3. **Database Per Service**
- Each service owns its data
- No direct database access between services
- Data consistency via events

#### 4. **Event Sourcing** (Optional for Order Service)
- Store all state changes as events
- Rebuild state from event log
- Enables audit trail and temporal queries

#### 5. **CQRS** (Command Query Responsibility Segregation)
- Separate read and write models
- Optimize queries independently
- Eventual consistency

#### 6. **Circuit Breaker Pattern**
- Prevent cascading failures
- Fail fast when service is down
- Automatic recovery attempts
- Implementation: Resilience4j, Polly, or custom

#### 7. **Retry Pattern**
- Exponential backoff: 100ms, 200ms, 400ms, 800ms
- Max retries: 3
- Idempotent operations only

#### 8. **Timeout Pattern**
- Service-to-service call timeout: 5 seconds
- Database query timeout: 10 seconds
- Kafka consumer timeout: 30 seconds

#### 9. **Bulkhead Pattern**
- Thread pool isolation per service
- Prevent resource exhaustion
- Thread pool size: 20 threads per service

#### 10. **Saga Pattern** (Distributed Transactions)
- Choreography-based for simple flows
- Orchestration-based for complex flows
- Compensating transactions for rollback

**Example: Order Creation Saga**
```
1. Order Service creates order (PENDING)
2. Inventory Service reserves stock
   - If fails: Order Service cancels order
3. Payment Service processes payment
   - If fails: Inventory Service releases reservation, Order Service cancels
4. Order Service confirms order (CONFIRMED)
5. Notification Service sends confirmation email
```

### SOLID Principles Implementation

#### Single Responsibility Principle (SRP)
- Each service has one reason to change
- Each class has one responsibility
- Example: OrderService only handles order logic, not payments

#### Open/Closed Principle (OCP)
- Open for extension, closed for modification
- Use interfaces and abstract classes
- Example: PaymentProcessor interface with multiple implementations

#### Liskov Substitution Principle (LSP)
- Subtypes must be substitutable for base types
- Maintain contract consistency
- Example: All payment processors implement same interface

#### Interface Segregation Principle (ISP)
- Clients should not depend on interfaces they don't use
- Create focused interfaces
- Example: Separate interfaces for read and write operations

#### Dependency Inversion Principle (DIP)
- Depend on abstractions, not concretions
- Use dependency injection
- Example: Inject repositories through constructor

### Testing Strategy

#### Unit Testing (40% of effort)
- **Framework**: JUnit 5 (Java), xUnit (.NET), Jest (Node), pytest (Python)
- **Mocking**: Mockito (Java), Moq (.NET), Jest mocks (Node), unittest.mock (Python)
- **Coverage Target**: 80% per service
- **Scope**: Business logic, validators, utilities

**Example Test Structure**:
```
src/
├── main/
│   └── java/com/ecommerce/order/
│       ├── OrderService.java
│       ├── OrderRepository.java
│       └── OrderValidator.java
└── test/
    └── java/com/ecommerce/order/
        ├── OrderServiceTest.java
        ├── OrderRepositoryTest.java
        └── OrderValidatorTest.java
```

#### Integration Testing (30% of effort)
- **Framework**: TestContainers (Docker-based)
- **Scope**: Service + Database interactions
- **Coverage**: Happy path, error scenarios, edge cases

**Example**:
```java
@Testcontainers
class OrderServiceIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>("postgres:14");
  
  @Test
  void testCreateOrder() {
    // Test with real database
  }
}
```

#### Contract Testing (15% of effort)
- **Framework**: Pact or Spring Cloud Contract
- **Scope**: Service-to-service API contracts
- **Verification**: Both provider and consumer sides

#### End-to-End Testing (10% of effort)
- **Framework**: Postman/Newman or Cypress
- **Scope**: Complete user workflows
- **Frequency**: Nightly runs

#### Performance Testing (5% of effort)
- **Framework**: JMeter or Gatling
- **Scenarios**: 
  - 1000 concurrent users
  - 100 orders/second
  - 10,000 product searches/second

### Observability & Monitoring

#### Centralized Logging (ELK Stack)
- **Elasticsearch**: Log storage and indexing
- **Logstash**: Log collection and transformation
- **Kibana**: Log visualization and analysis

**Log Format** (JSON):
```json
{
  "timestamp": "2024-01-15T10:30:45Z",
  "service": "order-service",
  "level": "INFO",
  "traceId": "abc123def456",
  "spanId": "xyz789",
  "message": "Order created successfully",
  "userId": "user-123",
  "orderId": "order-456",
  "duration_ms": 245
}
```

#### Distributed Tracing (Jaeger)
- **Trace ID**: Unique identifier for request flow
- **Span ID**: Individual operation within trace
- **Sampling**: 10% of requests in production

**Trace Example**:
```
Request → API Gateway (span 1)
        → Order Service (span 2)
        → Inventory Service (span 3)
        → Payment Service (span 4)
        → Kafka Producer (span 5)
```

#### Metrics Collection (Prometheus)
- **Scrape Interval**: 15 seconds
- **Retention**: 15 days
- **Key Metrics**:
  - Request count (by endpoint, status)
  - Request latency (p50, p95, p99)
  - Error rate
  - Service availability
  - Database connection pool usage
  - Kafka consumer lag

#### Alerting (Prometheus AlertManager)
- **High Error Rate**: > 5% errors in 5 minutes
- **High Latency**: p95 > 500ms
- **Service Down**: No heartbeat for 2 minutes
- **Database Connection Pool**: > 80% utilization

#### Dashboards (Grafana)
- **System Dashboard**: CPU, memory, disk usage
- **Application Dashboard**: Request rates, latencies, errors
- **Business Dashboard**: Orders/hour, revenue, conversion rate
- **Service Health**: Uptime, availability, SLA compliance

### CI/CD Pipeline (GitHub Actions)

#### Pipeline Stages

**1. Trigger**: On push to any branch or PR

**2. Build Stage**:
```yaml
- Checkout code
- Setup JDK/Node/.NET SDK
- Build application
- Run unit tests
- Generate coverage report
```

**3. Quality Gate**:
```yaml
- SonarQube analysis
- Code coverage check (minimum 80%)
- Security scanning (SAST)
- Dependency vulnerability check
```

**4. Integration Test**:
```yaml
- Start Docker Compose stack
- Run integration tests
- Run contract tests
- Collect coverage
```

**5. Build Docker Image**:
```yaml
- Build Docker image
- Push to Docker Registry
- Scan image for vulnerabilities
```

**6. Deploy to Staging** (on main branch):
```yaml
- Deploy to staging environment
- Run smoke tests
- Run performance tests
```

**7. Deploy to Production** (manual approval):
```yaml
- Deploy to production
- Run health checks
- Monitor error rates
- Rollback if needed
```

#### GitHub Actions Workflow Example

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn clean package
      - run: mvn test
      - uses: codecov/codecov-action@v3
  
  quality:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - uses: actions/checkout@v3
      - uses: SonarSource/sonarcloud-github-action@master
  
  docker:
    runs-on: ubuntu-latest
    needs: [build, quality]
    steps:
      - uses: actions/checkout@v3
      - uses: docker/build-push-action@v4
        with:
          push: true
          tags: myregistry/service:${{ github.sha }}
```

### Error Handling & Resilience

#### HTTP Status Codes
- **200**: Success
- **201**: Created
- **400**: Bad Request (validation error)
- **401**: Unauthorized (missing/invalid token)
- **403**: Forbidden (insufficient permissions)
- **404**: Not Found
- **409**: Conflict (duplicate, state conflict)
- **500**: Internal Server Error
- **503**: Service Unavailable

#### Error Response Format
```json
{
  "error": {
    "code": "INVALID_ORDER_STATE",
    "message": "Cannot cancel order in SHIPPED state",
    "details": {
      "orderId": "order-123",
      "currentState": "SHIPPED",
      "allowedStates": ["PENDING", "CONFIRMED"]
    },
    "timestamp": "2024-01-15T10:30:45Z",
    "traceId": "abc123def456"
  }
}
```

#### Resilience Patterns

**Circuit Breaker States**:
- **CLOSED**: Normal operation, requests pass through
- **OPEN**: Service down, requests fail immediately
- **HALF_OPEN**: Testing if service recovered, limited requests

**Configuration**:
```
Failure threshold: 50% (5 failures out of 10 requests)
Success threshold: 80% (4 successes out of 5 requests)
Timeout: 30 seconds before attempting recovery
```

**Retry Strategy**:
```
Max retries: 3
Backoff: Exponential (100ms, 200ms, 400ms)
Idempotent: Only retry idempotent operations (GET, PUT, DELETE)
```

### Security Considerations

#### Authentication & Authorization
- **Protocol**: OAuth2 / OIDC via Keycloak
- **Token Type**: JWT (JSON Web Token)
- **Token Validation**: Signature verification, expiry check
- **Scope**: Fine-grained permissions per endpoint

#### Data Protection
- **In Transit**: TLS 1.3 for all communications
- **At Rest**: AES-256 encryption for sensitive data
- **Database**: Row-level security for multi-tenant data

#### API Security
- **Rate Limiting**: 100 requests/second per user
- **Input Validation**: Whitelist approach, sanitize all inputs
- **CORS**: Restrict to known domains
- **CSRF**: Token-based protection for state-changing operations

#### Secrets Management
- **Storage**: HashiCorp Vault or AWS Secrets Manager
- **Rotation**: Automatic rotation every 90 days
- **Access**: Audit logging for all secret access

### Correctness Properties

#### Order Creation Flow
```
PROPERTY: Order can only be created from valid cart
  ∀ order ∈ Order: order.cartId ∈ ValidCarts
  
PROPERTY: Order total equals sum of items + tax + shipping
  ∀ order ∈ Order: order.total = Σ(items.price) + tax + shipping
  
PROPERTY: Order status transitions are valid
  ∀ order ∈ Order: order.status ∈ {PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED}
  ∧ PENDING → CONFIRMED → SHIPPED → DELIVERED
  ∧ Any state → CANCELLED
```

#### Payment Processing
```
PROPERTY: Payment amount matches order total
  ∀ payment ∈ Payment: payment.amount = order.total
  
PROPERTY: Payment can only be processed once per order
  ∀ order ∈ Order: count(payments where order.id = payment.orderId) ≤ 1
  
PROPERTY: Refund amount cannot exceed original payment
  ∀ refund ∈ Refund: refund.amount ≤ original_payment.amount
```

#### Inventory Management
```
PROPERTY: Reserved quantity cannot exceed available quantity
  ∀ inventory ∈ Inventory: inventory.reserved ≤ inventory.available
  
PROPERTY: Total inventory = available + reserved
  ∀ inventory ∈ Inventory: inventory.total = inventory.available + inventory.reserved
  
PROPERTY: Inventory transactions are immutable
  ∀ transaction ∈ InventoryTransaction: transaction is read-only after creation
```

## Documentation Structure

### Architecture Decision Records (ADRs)

**Location**: `docs/adrs/`

**ADR Template**:
```
# ADR-001: Use Microservices Architecture

## Status
Accepted

## Context
The ecommerce platform needs to support 10,000 concurrent users with independent scaling of services.

## Decision
We will use microservices architecture with API Gateway, Kafka for async communication, and database per service pattern.

## Consequences
- Positive: Independent scaling, technology diversity, fault isolation
- Negative: Operational complexity, distributed tracing overhead, eventual consistency

## Alternatives Considered
1. Monolithic architecture - rejected due to scaling limitations
2. SOA - rejected due to complexity of centralized ESB
```

### C4 Model Diagrams

**Level 1: System Context**
```
[User] → [Ecommerce System] → [Payment Gateway]
                            → [Email Service]
                            → [Keycloak]
```

**Level 2: Container**
```
[Web Browser] → [API Gateway] → [Order Service]
                             → [Product Service]
                             → [Cart Service]
                             → [Payment Service]
                             → [Inventory Service]
```

**Level 3: Component**
```
[Order Service]
├── OrderController
├── OrderService
├── OrderRepository
├── OrderValidator
└── OrderEventPublisher
```

**Level 4: Code**
```
class OrderService {
  - orderRepository: OrderRepository
  - inventoryClient: InventoryClient
  - paymentClient: PaymentClient
  
  + createOrder(cartId): Order
  + confirmOrder(orderId): void
  + cancelOrder(orderId): void
}
```

### Class Diagrams

**Order Domain Model**:
```
Order
├── id: UUID
├── userId: UUID
├── status: OrderStatus
├── items: List<OrderItem>
├── total: BigDecimal
├── createdAt: LocalDateTime
└── methods:
    ├── addItem(product, quantity)
    ├── removeItem(productId)
    ├── calculateTotal()
    └── confirm()

OrderItem
├── id: UUID
├── productId: UUID
├── quantity: Int
├── unitPrice: BigDecimal
└── subtotal: BigDecimal
```

### Sequence Diagrams

**Order Creation Sequence**:
```
User → API Gateway → Order Service → Inventory Service
                  → Payment Service
                  → Kafka (publish event)
                  → Notification Service
```

### UML Diagrams

**State Machine: Order Status**:
```
PENDING → CONFIRMED → SHIPPED → DELIVERED
   ↓
CANCELLED (from any state)
```

**Use Cases**:
```
User
├── Browse Products
├── Add to Cart
├── Checkout
├── Process Payment
└── Track Order

Admin
├── Manage Products
├── View Orders
├── Process Refunds
└── View Analytics
```

### Database Diagrams

**ER Diagram** (Order Service):
```
users (1) ──→ (N) orders
orders (1) ──→ (N) order_items
products (1) ──→ (N) order_items
orders (1) ──→ (N) payments
```

### README Structure

**Main README**:
- Project overview
- Quick start guide
- Architecture overview
- Branch structure
- Contributing guidelines
- License

**Service-Specific README**:
- Service purpose and responsibilities
- API endpoints documentation
- Database schema
- Configuration options
- Running locally
- Testing instructions
- Deployment guide

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- Set up repository structure
- Create Docker Compose stack
- Implement API Gateway
- Set up Keycloak
- Create base service templates

### Phase 2: Core Services (Week 3-4)
- Implement User Service
- Implement Product Service
- Implement Cart Service
- Implement Order Service
- Set up Kafka topics

### Phase 3: Advanced Services (Week 5-6)
- Implement Payment Service
- Implement Inventory Service
- Implement Notification Service
- Implement Analytics Service
- Set up distributed tracing

### Phase 4: Quality & Deployment (Week 7-8)
- Complete test coverage
- Set up CI/CD pipeline
- Performance testing
- Security audit
- Documentation completion

## Conclusion

This design provides a comprehensive blueprint for a production-grade microservices ecommerce platform. It emphasizes scalability, resilience, observability, and maintainability through proven architectural patterns and best practices. The course structure allows students to understand each architectural paradigm (SOA, MVC, Microservices) while implementing the same business domain across multiple languages and technologies.
