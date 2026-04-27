# Architecture Decision Records (ADRs)

## ADR-001: Use Microservices Architecture for Capstone

**Status**: Accepted

**Context**
The course requires teaching three architectural patterns (SOA, MVC, Microservices) with a practical ecommerce project. The microservices architecture serves as the capstone, demonstrating distributed systems complexity and modern DevOps practices. The system must support 10,000 concurrent users with independent scaling of services.

**Decision**
Implement the ecommerce platform using microservices architecture with the following characteristics:
- 10 independent microservices (User, Product, Cart, Order, Payment, Inventory, Notification, Analytics, API Gateway, Auth)
- API Gateway as single entry point
- Kafka for asynchronous event-driven communication
- Database per service pattern (PostgreSQL)
- Centralized authentication via Keycloak

**Consequences**

*Positive*:
- Independent scaling of services based on demand
- Technology diversity (Java, .NET, Node.js per service)
- Fault isolation - failure in one service doesn't cascade
- Clear separation of concerns
- Enables teaching distributed systems concepts
- Supports 10,000 concurrent users requirement

*Negative*:
- Operational complexity (10 services to manage)
- Distributed tracing overhead
- Eventual consistency challenges
- Network latency between services
- Debugging distributed issues is harder
- Requires sophisticated monitoring and logging

**Alternatives Considered**
1. **Monolithic Architecture**: Rejected - cannot demonstrate independent scaling, doesn't meet course objectives
2. **SOA with ESB**: Rejected - too complex for learning, ESB becomes bottleneck
3. **Serverless**: Rejected - hides infrastructure complexity, not suitable for teaching

---

## ADR-002: Database Per Service Pattern

**Status**: Accepted

**Context**
Microservices architecture requires decisions about data management. Options include shared database, database per service, or event sourcing. The course emphasizes loose coupling and independent scaling.

**Decision**
Each microservice owns its own PostgreSQL database. Services communicate through APIs and events, never directly accessing other service databases.

**Database Allocation**:
- User Service → user_db
- Product Service → product_db
- Cart Service → cart_db (+ Redis cache)
- Order Service → order_db
- Payment Service → payment_db
- Inventory Service → inventory_db
- Analytics Service → analytics_db

**Consequences**

*Positive*:
- Loose coupling between services
- Independent database scaling
- Technology flexibility (could use different DB types)
- Clear data ownership
- Easier to understand data flow

*Negative*:
- Data consistency challenges (eventual consistency)
- Complex queries across services
- Distributed transaction complexity
- Data duplication may be necessary
- Requires event-driven architecture for sync

**Alternatives Considered**
1. **Shared Database**: Rejected - creates tight coupling, violates microservices principles
2. **Event Sourcing**: Rejected - too complex for initial learning, can be added later
3. **CQRS**: Rejected - adds complexity, can be optional pattern

---

## ADR-003: Kafka for Asynchronous Communication

**Status**: Accepted

**Context**
Microservices need to communicate asynchronously for events like order creation, payment processing, and inventory updates. Options include Kafka, RabbitMQ, AWS SQS, or direct REST calls.

**Decision**
Use Apache Kafka as the message broker for asynchronous event-driven communication between services.

**Key Topics**:
- order.created
- order.confirmed
- order.shipped
- payment.processed
- payment.failed
- inventory.reserved
- inventory.depleted
- user.registered
- user.updated

**Configuration**:
- 3 broker nodes (production resilience)
- 3 partitions per topic (parallelism)
- Replication factor: 2
- Consumer groups: one per service

**Consequences**

*Positive*:
- Decouples services completely
- Enables event-driven architecture
- Scales to high throughput (100k+ messages/sec)
- Provides event history/audit trail
- Supports multiple consumers per event
- Teaches event-driven patterns

*Negative*:
- Operational complexity (Kafka cluster management)
- Eventual consistency (not immediate)
- Consumer lag monitoring required
- Message ordering only within partition
- Requires idempotent consumers

**Alternatives Considered**
1. **RabbitMQ**: Rejected - less suitable for high-volume event streaming
2. **AWS SQS**: Rejected - cloud-specific, not suitable for local Docker development
3. **Direct REST Calls**: Rejected - creates tight coupling, doesn't teach async patterns

---

## ADR-004: Keycloak for Centralized Authentication

**Status**: Accepted

**Context**
The system requires centralized authentication and authorization across all microservices. Options include Keycloak, Auth0, custom JWT implementation, or OAuth2 provider.

**Decision**
Use Keycloak as the centralized authentication and authorization server for all services.

**Configuration**:
- Single Keycloak instance (Docker container)
- Realm: ecommerce
- Clients: web-client, mobile-client, service-account
- User Roles: ADMIN, USER, CUSTOMER, SUPPORT
- Token Type: JWT
- Token Expiry: 15 minutes (access), 7 days (refresh)

**Integration Pattern**:
1. Client authenticates with Keycloak
2. Keycloak returns JWT token
3. Client includes token in Authorization header
4. API Gateway validates token signature
5. API Gateway forwards request to service
6. Service can verify token locally (signature already validated)

**Consequences**

*Positive*:
- Centralized user management
- OAuth2/OIDC compliance
- Fine-grained role-based access control
- Token-based stateless authentication
- Scales horizontally
- Teaches OAuth2/OIDC concepts
- Supports multi-tenant scenarios

*Negative*:
- Additional service to manage
- Token validation overhead
- Keycloak becomes critical dependency
- Requires token refresh logic
- Learning curve for Keycloak configuration

**Alternatives Considered**
1. **Auth0**: Rejected - cloud-only, not suitable for local development
2. **Custom JWT**: Rejected - doesn't teach enterprise auth patterns
3. **Per-Service Auth**: Rejected - violates DRY principle, inconsistent auth

---

## ADR-005: API Gateway Pattern

**Status**: Accepted

**Context**
Microservices expose multiple endpoints. Clients need a single entry point. Options include Kong, Spring Cloud Gateway, custom implementation, or no gateway.

**Decision**
Implement an API Gateway as the single entry point for all client requests.

**Responsibilities**:
- Request routing to appropriate microservices
- Authentication token validation
- Rate limiting (100 req/sec per user)
- Request/response logging
- CORS handling
- Request transformation

**Technology Options**:
- Kong (production-grade)
- Spring Cloud Gateway (Java)
- ASP.NET Core middleware (C#)
- Express middleware (Node.js)

**Consequences**

*Positive*:
- Single entry point for clients
- Centralized cross-cutting concerns
- Rate limiting and throttling
- Request logging and tracing
- Easier API versioning
- Teaches API Gateway pattern

*Negative*:
- Additional service to manage
- Potential bottleneck (must scale horizontally)
- Adds latency to all requests
- Complex routing rules
- Debugging becomes harder

**Alternatives Considered**
1. **No Gateway**: Rejected - clients must know all service URLs, no centralized auth
2. **Load Balancer Only**: Rejected - doesn't handle cross-cutting concerns
3. **Service Mesh**: Rejected - too complex for learning, can be added later

---

## ADR-006: REST APIs for Synchronous Communication

**Status**: Accepted

**Context**
Services need to communicate synchronously for operations like cart checkout, payment processing, and inventory checks. Options include REST, gRPC, GraphQL, or SOAP.

**Decision**
Use REST APIs with JSON for synchronous service-to-service communication.

**API Design Principles**:
- RESTful endpoints (GET, POST, PUT, DELETE)
- JSON request/response format
- Semantic HTTP status codes
- Versioning: /api/v1/
- Pagination for list endpoints
- Filtering and sorting support

**Resilience Patterns**:
- Circuit breaker (fail fast when service down)
- Retry with exponential backoff (3 retries)
- Timeout (5 seconds per call)
- Bulkhead (thread pool isolation)

**Consequences**

*Positive*:
- Simple and well-understood
- Language-agnostic
- Easy to debug (HTTP tools)
- Stateless communication
- Teaches REST principles
- Good for learning

*Negative*:
- Synchronous (blocking)
- Tight coupling if not careful
- Network latency
- Cascading failures possible
- Requires resilience patterns

**Alternatives Considered**
1. **gRPC**: Rejected - too complex for learning, binary protocol harder to debug
2. **GraphQL**: Rejected - adds complexity, not suitable for service-to-service
3. **SOAP**: Rejected - outdated, verbose, not suitable for microservices

---

## ADR-007: Circuit Breaker Pattern for Resilience

**Status**: Accepted

**Context**
Synchronous REST calls between services can fail. Without resilience patterns, failures cascade. Options include circuit breaker, bulkhead, timeout, or retry.

**Decision**
Implement circuit breaker pattern for all synchronous service-to-service calls.

**Configuration**:
- Failure threshold: 50% (5 failures out of 10 requests)
- Success threshold: 80% (4 successes out of 5 requests)
- Timeout: 30 seconds before attempting recovery
- States: CLOSED (normal), OPEN (failing), HALF_OPEN (testing recovery)

**Implementation Libraries**:
- Java: Resilience4j
- C#: Polly
- Node.js: opossum or custom

**Consequences**

*Positive*:
- Prevents cascading failures
- Fails fast when service is down
- Automatic recovery attempts
- Teaches resilience patterns
- Improves system stability

*Negative*:
- Additional complexity
- Requires configuration tuning
- Debugging failures harder
- May mask underlying issues
- Requires monitoring

**Alternatives Considered**
1. **No Resilience**: Rejected - system becomes fragile
2. **Retry Only**: Rejected - doesn't prevent cascading failures
3. **Timeout Only**: Rejected - incomplete resilience strategy

---

## ADR-008: Saga Pattern for Distributed Transactions

**Status**: Accepted

**Context**
Order creation involves multiple services (Order, Inventory, Payment). Traditional ACID transactions don't work across services. Options include Saga (choreography or orchestration), event sourcing, or 2-phase commit.

**Decision**
Implement Saga pattern with choreography for simple flows and orchestration for complex flows.

**Order Creation Saga (Choreography)**:
1. Order Service creates order (PENDING)
2. Publishes "order.created" event
3. Inventory Service reserves stock
   - If fails: publishes "inventory.reservation_failed"
   - Order Service receives and cancels order
4. Payment Service processes payment
   - If fails: publishes "payment.failed"
   - Inventory Service releases reservation
   - Order Service cancels order
5. Order Service confirms order (CONFIRMED)
6. Notification Service sends confirmation email

**Consequences**

*Positive*:
- Handles distributed transactions
- Eventual consistency
- Teaches saga pattern
- Decoupled services
- Supports long-running processes

*Negative*:
- Complex to understand and debug
- Requires compensating transactions
- Eventual consistency (not immediate)
- Difficult to test
- Requires careful event design

**Alternatives Considered**
1. **2-Phase Commit**: Rejected - doesn't work well with microservices, creates tight coupling
2. **Event Sourcing**: Rejected - too complex for initial learning
3. **No Transactions**: Rejected - data consistency issues

---

## ADR-009: Docker Compose for Local Development

**Status**: Accepted

**Context**
Development environment must support 10 microservices, 7 databases, Kafka, Keycloak, and monitoring stack. Options include Docker Compose, Kubernetes, or manual setup.

**Decision**
Use Docker Compose for local development environment with all services containerized.

**Stack Composition**:
- 10 microservices (Java/Node/C# implementations)
- 7 PostgreSQL instances
- Redis cache
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

**Consequences**

*Positive*:
- Simple to set up and tear down
- Reproducible environment
- All services run locally
- Easy to debug
- Teaches containerization
- Good for learning

*Negative*:
- High resource usage
- Not suitable for production
- Scaling limitations
- Network simulation not realistic
- Requires Docker installation

**Alternatives Considered**
1. **Kubernetes**: Rejected - too complex for learning, overkill for local dev
2. **Manual Setup**: Rejected - error-prone, not reproducible
3. **Cloud Sandbox**: Rejected - requires internet, not suitable for offline learning

---

## ADR-010: ELK Stack for Centralized Logging

**Status**: Accepted

**Context**
Microservices generate logs across multiple services. Debugging requires centralized log collection. Options include ELK, Splunk, Datadog, or file-based logging.

**Decision**
Use ELK Stack (Elasticsearch, Logstash, Kibana) for centralized logging.

**Configuration**:
- Elasticsearch: Log storage and indexing
- Logstash: Log collection and transformation
- Kibana: Log visualization and analysis
- Log Format: JSON for structured logging
- Retention: 7 days

**Log Fields**:
- timestamp
- service
- level (INFO, WARN, ERROR)
- traceId (for distributed tracing)
- spanId
- message
- userId
- orderId
- duration_ms

**Consequences**

*Positive*:
- Centralized log collection
- Full-text search capabilities
- Real-time log analysis
- Visualization and dashboards
- Teaches observability
- Open-source (free)

*Negative*:
- Operational complexity
- High storage requirements
- Requires tuning for performance
- Learning curve for Kibana
- Elasticsearch resource-intensive

**Alternatives Considered**
1. **Splunk**: Rejected - expensive, overkill for learning
2. **Datadog**: Rejected - cloud-only, requires internet
3. **File-based Logging**: Rejected - not suitable for microservices

---

## ADR-011: Prometheus + Grafana for Metrics

**Status**: Accepted

**Context**
System requires monitoring of performance metrics, resource usage, and business metrics. Options include Prometheus, Datadog, New Relic, or custom metrics.

**Decision**
Use Prometheus for metrics collection and Grafana for visualization.

**Metrics Collection**:
- Scrape interval: 15 seconds
- Retention: 15 days
- Key metrics:
  - Request count (by endpoint, status)
  - Request latency (p50, p95, p99)
  - Error rate
  - Service availability
  - Database connection pool usage
  - Kafka consumer lag

**Dashboards**:
- System Dashboard: CPU, memory, disk usage
- Application Dashboard: Request rates, latencies, errors
- Business Dashboard: Orders/hour, revenue, conversion rate
- Service Health: Uptime, availability, SLA compliance

**Consequences**

*Positive*:
- Real-time metrics collection
- Powerful visualization
- Alerting capabilities
- Open-source (free)
- Teaches monitoring and observability
- Scalable architecture

*Negative*:
- Operational complexity
- Requires metric instrumentation
- Storage requirements
- Learning curve for PromQL
- Alerting rules can be complex

**Alternatives Considered**
1. **Datadog**: Rejected - expensive, cloud-only
2. **New Relic**: Rejected - expensive, cloud-only
3. **CloudWatch**: Rejected - AWS-specific, not suitable for local dev

---

## ADR-012: Jaeger for Distributed Tracing

**Status**: Accepted

**Context**
Debugging requests across 10 microservices is complex. Distributed tracing helps track request flow. Options include Jaeger, Zipkin, Datadog, or custom tracing.

**Decision**
Use Jaeger for distributed tracing across all microservices.

**Configuration**:
- Trace ID: Unique identifier for request flow
- Span ID: Individual operation within trace
- Sampling: 10% of requests in production
- Retention: 72 hours

**Trace Example**:
```
Request → API Gateway (span 1)
        → Order Service (span 2)
        → Inventory Service (span 3)
        → Payment Service (span 4)
        → Kafka Producer (span 5)
```

**Consequences**

*Positive*:
- Complete request flow visibility
- Performance bottleneck identification
- Debugging distributed issues
- Teaches distributed tracing
- Open-source (free)
- Supports multiple languages

*Negative*:
- Operational complexity
- Sampling overhead
- Storage requirements
- Learning curve
- Requires instrumentation

**Alternatives Considered**
1. **Zipkin**: Rejected - less feature-rich than Jaeger
2. **Datadog**: Rejected - expensive, cloud-only
3. **Custom Tracing**: Rejected - too complex to implement

---

## ADR-013: GitHub Actions for CI/CD

**Status**: Accepted

**Context**
Course requires automated testing and deployment. Options include GitHub Actions, Jenkins, GitLab CI, or CircleCI.

**Decision**
Use GitHub Actions for CI/CD pipeline with automated build, test, and deployment stages.

**Pipeline Stages**:
1. Build: Compile code, run unit tests
2. Quality Gate: SonarQube, code coverage, security scanning
3. Integration Test: Run integration tests with Docker Compose
4. Docker Build: Build and push Docker image
5. Deploy to Staging: Automated deployment
6. Deploy to Production: Manual approval required

**Consequences**

*Positive*:
- Native GitHub integration
- Free for public repositories
- Powerful workflow capabilities
- Teaches CI/CD concepts
- Easy to set up
- Good documentation

*Negative*:
- Limited to GitHub repositories
- Execution time limits
- Requires GitHub account
- Learning curve for YAML syntax
- Limited debugging capabilities

**Alternatives Considered**
1. **Jenkins**: Rejected - requires server setup, too complex for learning
2. **GitLab CI**: Rejected - requires GitLab, not GitHub
3. **CircleCI**: Rejected - requires separate account, not integrated with GitHub

---

## ADR-014: Four-Language Implementation (Java, .NET, Node.js, Python)

**Status**: Accepted

**Context**
Course teaches architecture patterns across different technology stacks. Options include single language, two languages, three languages, or four languages.

**Decision**
Implement each architecture (SOA, MVC, Microservices) in four languages: Java, .NET, Node.js, and Python.

**Language Selection**:
- **Java**: Enterprise standard, Spring Framework, mature ecosystem
- **.NET**: Microsoft stack, C#, modern async/await
- **Node.js**: JavaScript, event-driven, lightweight
- **Python**: Data science, rapid development, Django/FastAPI

**Branch Structure**:
- soa-architecture branch: java-soa/, dotnet-soa/, nodejs-soa/, python-soa/
- mvc-architecture branch: java-mvc/, dotnet-mvc/, nodejs-mvc/, python-mvc/
- microservices-architecture branch: java-microservices/, dotnet-microservices/, nodejs-microservices/, python-microservices/

**Consequences**

*Positive*:
- Teaches architecture independence from language
- Exposes students to different ecosystems
- Demonstrates polyglot development
- Increases course value
- Shows architecture patterns are universal
- Python adds data science and rapid development perspective

*Negative*:
- Significant development effort (4x code)
- Maintenance burden (4x testing)
- Larger repository size
- Longer course duration
- Requires expertise in all four languages

**Alternatives Considered**
1. **Single Language**: Rejected - doesn't demonstrate architecture independence
2. **Two Languages**: Rejected - incomplete coverage
3. **Three Languages**: Rejected - missing Python perspective
4. **Language-Agnostic Pseudocode**: Rejected - not practical for learning

---

## ADR-015: 80% Code Coverage Requirement

**Status**: Accepted

**Context**
Course emphasizes quality and testing. Options include no coverage requirement, 50%, 70%, 80%, or 100%.

**Decision**
Require minimum 80% code coverage for all services.

**Coverage Breakdown**:
- Unit Tests: 40% of effort (target 80% coverage)
- Integration Tests: 30% of effort
- Contract Tests: 15% of effort
- E2E Tests: 10% of effort
- Performance Tests: 5% of effort

**Tools**:
- Java: JaCoCo
- C#: OpenCover
- Node.js: Istanbul/nyc

**Consequences**

*Positive*:
- High code quality
- Catches bugs early
- Teaches testing importance
- Enables refactoring confidence
- Measurable quality metric

*Negative*:
- Significant time investment
- Can lead to test bloat
- Coverage doesn't guarantee correctness
- Difficult to achieve 100%
- May slow down development

**Alternatives Considered**
1. **No Coverage Requirement**: Rejected - doesn't emphasize quality
2. **50% Coverage**: Rejected - too low for production code
3. **100% Coverage**: Rejected - impractical, diminishing returns

