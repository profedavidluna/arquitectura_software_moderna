# Repository Structure and Implementation Guide

## Repository Organization

```
software-architecture-course/
│
├── README.md                          # Main project documentation
├── CONTRIBUTING.md                    # Contribution guidelines
├── LICENSE                            # MIT License
│
├── .github/
│   └── workflows/
│       ├── build.yml                  # Build and test workflow
│       ├── quality.yml                # Code quality checks
│       └── deploy.yml                 # Deployment workflow
│
├── docs/
│   ├── README.md                      # Documentation index
│   ├── GETTING_STARTED.md             # Quick start guide
│   ├── ARCHITECTURE.md                # Architecture overview
│   │
│   ├── architecture/
│   │   ├── C4-CONTEXT.md              # C4 Level 1 diagrams
│   │   ├── C4-CONTAINER.md            # C4 Level 2 diagrams
│   │   ├── C4-COMPONENT.md            # C4 Level 3 diagrams
│   │   ├── MICROSERVICES.md           # Microservices architecture
│   │   ├── DATA-FLOW.md               # Data flow diagrams
│   │   └── DEPLOYMENT.md              # Deployment architecture
│   │
│   ├── adrs/
│   │   ├── ADR-001-microservices.md
│   │   ├── ADR-002-database-per-service.md
│   │   ├── ADR-003-kafka.md
│   │   ├── ADR-004-keycloak.md
│   │   ├── ADR-005-api-gateway.md
│   │   ├── ADR-006-rest-apis.md
│   │   ├── ADR-007-circuit-breaker.md
│   │   ├── ADR-008-saga-pattern.md
│   │   ├── ADR-009-docker-compose.md
│   │   ├── ADR-010-elk-stack.md
│   │   ├── ADR-011-prometheus.md
│   │   ├── ADR-012-jaeger.md
│   │   ├── ADR-013-github-actions.md
│   │   ├── ADR-014-three-languages.md
│   │   └── ADR-015-code-coverage.md
│   │
│   ├── diagrams/
│   │   ├── system-context.png
│   │   ├── container-diagram.png
│   │   ├── component-diagram.png
│   │   ├── sequence-order-creation.png
│   │   ├── sequence-payment.png
│   │   ├── er-diagram.png
│   │   ├── deployment-diagram.png
│   │   └── kafka-topics.png
│   │
│   ├── guides/
│   │   ├── LOCAL_SETUP.md             # Local development setup
│   │   ├── TESTING_GUIDE.md           # Testing strategies
│   │   ├── CI_CD_GUIDE.md             # CI/CD pipeline guide
│   │   ├── MONITORING_GUIDE.md        # Monitoring and observability
│   │   ├── SECURITY_GUIDE.md          # Security best practices
│   │   └── TROUBLESHOOTING.md         # Common issues and solutions
│   │
│   └── api/
│       ├── openapi.yaml               # OpenAPI specification
│       ├── user-service-api.md
│       ├── product-service-api.md
│       ├── cart-service-api.md
│       ├── order-service-api.md
│       ├── payment-service-api.md
│       ├── inventory-service-api.md
│       ├── notification-service-api.md
│       └── analytics-service-api.md
│
├── shared/
│   ├── docker-compose.yml             # Local development stack
│   ├── docker-compose.prod.yml        # Production stack (optional)
│   │
│   ├── keycloak/
│   │   ├── Dockerfile
│   │   ├── realm-config.json          # Keycloak realm configuration
│   │   └── users.json                 # Initial users
│   │
│   ├── kafka/
│   │   ├── docker-compose.kafka.yml
│   │   └── topics-init.sh             # Topic creation script
│   │
│   ├── database/
│   │   ├── init-user-db.sql
│   │   ├── init-product-db.sql
│   │   ├── init-cart-db.sql
│   │   ├── init-order-db.sql
│   │   ├── init-payment-db.sql
│   │   ├── init-inventory-db.sql
│   │   └── init-analytics-db.sql
│   │
│   ├── monitoring/
│   │   ├── prometheus.yml
│   │   ├── grafana-dashboards/
│   │   │   ├── system-dashboard.json
│   │   │   ├── application-dashboard.json
│   │   │   ├── business-dashboard.json
│   │   │   └── service-health.json
│   │   └── alerting-rules.yml
│   │
│   ├── logging/
│   │   ├── logstash.conf
│   │   └── kibana-dashboards/
│   │       ├── error-logs.json
│   │       ├── request-logs.json
│   │       └── performance-logs.json
│   │
│   ├── tracing/
│   │   └── jaeger-config.yml
│   │
│   └── scripts/
│       ├── setup-local-env.sh         # Setup script
│       ├── start-stack.sh             # Start Docker Compose
│       ├── stop-stack.sh              # Stop Docker Compose
│       ├── reset-stack.sh             # Reset all data
│       └── load-test-data.sh          # Load sample data
│
├── soa-architecture/                  # SOA branch
│   ├── java-soa/
│   │   ├── pom.xml
│   │   ├── src/
│   │   │   ├── main/java/com/ecommerce/
│   │   │   │   ├── user/
│   │   │   │   ├── product/
│   │   │   │   ├── cart/
│   │   │   │   ├── order/
│   │   │   │   ├── payment/
│   │   │   │   └── common/
│   │   │   └── test/java/com/ecommerce/
│   │   ├── Dockerfile
│   │   └── README.md
│   │
│   ├── dotnet-soa/
│   │   ├── EcommerceSoa.sln
│   │   ├── src/
│   │   │   ├── EcommerceSoa/
│   │   │   │   ├── Controllers/
│   │   │   │   ├── Services/
│   │   │   │   ├── Models/
│   │   │   │   └── Data/
│   │   │   └── EcommerceSoa.Tests/
│   │   ├── Dockerfile
│   │   └── README.md
│   │
│   └── nodejs-soa/
│       ├── package.json
│       ├── src/
│       │   ├── controllers/
│       │   ├── services/
│       │   ├── models/
│       │   ├── routes/
│       │   └── middleware/
│       ├── tests/
│       ├── Dockerfile
│       └── README.md
│
├── mvc-architecture/                  # MVC branch
│   ├── java-mvc/
│   │   ├── pom.xml
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ecommerce/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── model/
│   │   │   │   │   └── repository/
│   │   │   │   └── resources/
│   │   │   │       ├── templates/
│   │   │   │       └── static/
│   │   │   └── test/
│   │   ├── Dockerfile
│   │   └── README.md
│   │
│   ├── dotnet-mvc/
│   │   ├── EcommerceMvc.sln
│   │   ├── src/
│   │   │   ├── EcommerceMvc/
│   │   │   │   ├── Controllers/
│   │   │   │   ├── Models/
│   │   │   │   ├── Views/
│   │   │   │   └── Data/
│   │   │   └── EcommerceMvc.Tests/
│   │   ├── Dockerfile
│   │   └── README.md
│   │
│   └── nodejs-mvc/
│       ├── package.json
│       ├── src/
│       │   ├── controllers/
│       │   ├── models/
│       │   ├── views/
│       │   ├── routes/
│       │   └── middleware/
│       ├── public/
│       ├── tests/
│       ├── Dockerfile
│       └── README.md
│
└── microservices-architecture/        # Microservices branch
    ├── java-microservices/
    │   ├── api-gateway/
    │   │   ├── pom.xml
    │   │   ├── src/
    │   │   │   ├── main/java/com/ecommerce/gateway/
    │   │   │   └── test/
    │   │   ├── Dockerfile
    │   │   └── README.md
    │   │
    │   ├── user-service/
    │   │   ├── pom.xml
    │   │   ├── src/
    │   │   │   ├── main/java/com/ecommerce/user/
    │   │   │   │   ├── controller/
    │   │   │   │   ├── service/
    │   │   │   │   ├── model/
    │   │   │   │   ├── repository/
    │   │   │   │   └── event/
    │   │   │   └── test/
    │   │   ├── Dockerfile
    │   │   └── README.md
    │   │
    │   ├── product-service/
    │   ├── cart-service/
    │   ├── order-service/
    │   ├── payment-service/
    │   ├── inventory-service/
    │   ├── notification-service/
    │   ├── analytics-service/
    │   │
    │   ├── common-lib/
    │   │   ├── pom.xml
    │   │   └── src/
    │   │       ├── main/java/com/ecommerce/common/
    │   │       │   ├── dto/
    │   │       │   ├── exception/
    │   │       │   ├── util/
    │   │       │   ├── kafka/
    │   │       │   └── security/
    │   │       └── test/
    │   │
    │   └── pom.xml (parent)
    │
    ├── dotnet-microservices/
    │   ├── Ecommerce.sln
    │   ├── ApiGateway/
    │   ├── UserService/
    │   ├── ProductService/
    │   ├── CartService/
    │   ├── OrderService/
    │   ├── PaymentService/
    │   ├── InventoryService/
    │   ├── NotificationService/
    │   ├── AnalyticsService/
    │   │
    │   ├── Common/
    │   │   ├── Dtos/
    │   │   ├── Exceptions/
    │   │   ├── Utilities/
    │   │   ├── Kafka/
    │   │   └── Security/
    │   │
    │   └── Tests/
    │
    └── nodejs-microservices/
        ├── package.json (root)
        ├── lerna.json (monorepo config)
        │
        ├── packages/
        │   ├── api-gateway/
        │   │   ├── package.json
        │   │   ├── src/
        │   │   │   ├── routes/
        │   │   │   ├── middleware/
        │   │   │   └── index.js
        │   │   └── tests/
        │   │
        │   ├── user-service/
        │   ├── product-service/
        │   ├── cart-service/
        │   ├── order-service/
        │   ├── payment-service/
        │   ├── inventory-service/
        │   ├── notification-service/
        │   ├── analytics-service/
        │   │
        │   └── common/
        │       ├── package.json
        │       ├── src/
        │       │   ├── dto/
        │       │   ├── exception/
        │       │   ├── util/
        │       │   ├── kafka/
        │       │   └── security/
        │       └── tests/
        │
        └── tests/
            ├── integration/
            ├── e2e/
            └── performance/
```

## Branch Strategy

### Main Branch
- **Purpose**: Documentation, setup, theory
- **Content**:
  - README and getting started guides
  - Architecture documentation
  - ADRs
  - Shared configuration (Docker Compose, Keycloak, Kafka)
  - CI/CD workflows
- **Protection**: Requires PR review, all checks pass

### SOA Architecture Branch
- **Branch Name**: `soa-architecture`
- **Content**: SOA implementation in Java, .NET, Node.js
- **Duration**: 8 hours of course
- **Key Concepts**:
  - Service-oriented architecture
  - Enterprise Service Bus (ESB) patterns
  - SOAP/REST services
  - Centralized governance

### MVC Architecture Branch
- **Branch Name**: `mvc-architecture`
- **Content**: MVC implementation in Java, .NET, Node.js
- **Duration**: 8 hours of course
- **Key Concepts**:
  - Model-View-Controller pattern
  - Web application development
  - Template engines
  - Form handling and validation

### Microservices Architecture Branch
- **Branch Name**: `microservices-architecture`
- **Content**: Microservices implementation in Java, .NET, Node.js
- **Duration**: 8 hours of course
- **Key Concepts**:
  - Microservices patterns
  - API Gateway
  - Event-driven architecture
  - Distributed systems
  - Resilience patterns

## Development Workflow

### Local Development Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/your-org/software-architecture-course.git
   cd software-architecture-course
   ```

2. **Checkout Branch**
   ```bash
   git checkout microservices-architecture
   ```

3. **Setup Environment**
   ```bash
   cd shared
   ./setup-local-env.sh
   ```

4. **Start Docker Compose Stack**
   ```bash
   ./start-stack.sh
   ```

5. **Verify Services**
   ```bash
   curl http://localhost:8080/health
   ```

### Service Development

#### Java Microservice Template

```
user-service/
├── pom.xml
├── src/
│   ├── main/java/com/ecommerce/user/
│   │   ├── UserServiceApplication.java
│   │   ├── controller/
│   │   │   └── UserController.java
│   │   ├── service/
│   │   │   └── UserService.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── model/
│   │   │   └── User.java
│   │   ├── dto/
│   │   │   ├── UserRequest.java
│   │   │   └── UserResponse.java
│   │   ├── event/
│   │   │   ├── UserCreatedEvent.java
│   │   │   └── UserEventPublisher.java
│   │   ├── exception/
│   │   │   └── UserNotFoundException.java
│   │   └── config/
│   │       ├── SecurityConfig.java
│   │       └── KafkaConfig.java
│   └── test/java/com/ecommerce/user/
│       ├── controller/
│       │   └── UserControllerTest.java
│       ├── service/
│       │   └── UserServiceTest.java
│       └── integration/
│           └── UserServiceIntegrationTest.java
├── Dockerfile
└── README.md
```

#### .NET Microservice Template

```
UserService/
├── UserService.csproj
├── Program.cs
├── Controllers/
│   └── UserController.cs
├── Services/
│   └── UserService.cs
├── Models/
│   └── User.cs
├── Data/
│   ├── UserDbContext.cs
│   └── UserRepository.cs
├── Dtos/
│   ├── UserRequest.cs
│   └── UserResponse.cs
├── Events/
│   ├── UserCreatedEvent.cs
│   └── UserEventPublisher.cs
├── Exceptions/
│   └── UserNotFoundException.cs
├── Configuration/
│   ├── SecurityConfig.cs
│   └── KafkaConfig.cs
├── Tests/
│   ├── UserControllerTests.cs
│   ├── UserServiceTests.cs
│   └── UserServiceIntegrationTests.cs
├── Dockerfile
└── README.md
```

#### Node.js Microservice Template

```
packages/user-service/
├── package.json
├── src/
│   ├── index.js
│   ├── controllers/
│   │   └── userController.js
│   ├── services/
│   │   └── userService.js
│   ├── models/
│   │   └── User.js
│   ├── routes/
│   │   └── userRoutes.js
│   ├── middleware/
│   │   ├── auth.js
│   │   └── errorHandler.js
│   ├── events/
│   │   ├── userCreatedEvent.js
│   │   └── userEventPublisher.js
│   ├── exceptions/
│   │   └── UserNotFoundException.js
│   └── config/
│       ├── security.js
│       └── kafka.js
├── tests/
│   ├── unit/
│   │   ├── userController.test.js
│   │   └── userService.test.js
│   └── integration/
│       └── userService.integration.test.js
├── Dockerfile
└── README.md
```

## Testing Structure

### Unit Tests
- **Location**: `src/test/` (Java), `Tests/` (.NET), `tests/unit/` (Node.js)
- **Framework**: JUnit 5 (Java), xUnit (.NET), Jest (Node.js)
- **Coverage Target**: 80%
- **Scope**: Business logic, validators, utilities

### Integration Tests
- **Location**: `src/test/integration/` (Java), `Tests/` (.NET), `tests/integration/` (Node.js)
- **Framework**: TestContainers (Java), xUnit (.NET), Jest (Node.js)
- **Scope**: Service + Database interactions

### Contract Tests
- **Location**: `src/test/contract/` (Java), `Tests/` (.NET), `tests/contract/` (Node.js)
- **Framework**: Pact or Spring Cloud Contract
- **Scope**: Service-to-service API contracts

### E2E Tests
- **Location**: `tests/e2e/`
- **Framework**: Postman/Newman or Cypress
- **Scope**: Complete user workflows

## CI/CD Pipeline

### GitHub Actions Workflows

#### Build Workflow (`.github/workflows/build.yml`)
- Trigger: Push to any branch
- Steps:
  1. Checkout code
  2. Setup SDK (Java/Node/.NET)
  3. Build application
  4. Run unit tests
  5. Generate coverage report

#### Quality Workflow (`.github/workflows/quality.yml`)
- Trigger: Push to main/develop
- Steps:
  1. SonarQube analysis
  2. Code coverage check (80% minimum)
  3. Security scanning (SAST)
  4. Dependency vulnerability check

#### Integration Test Workflow (`.github/workflows/integration.yml`)
- Trigger: Push to main/develop
- Steps:
  1. Start Docker Compose stack
  2. Run integration tests
  3. Run contract tests
  4. Collect coverage

#### Docker Build Workflow (`.github/workflows/docker.yml`)
- Trigger: Push to main/develop
- Steps:
  1. Build Docker image
  2. Push to Docker Registry
  3. Scan image for vulnerabilities

#### Deploy Workflow (`.github/workflows/deploy.yml`)
- Trigger: Manual approval on main
- Steps:
  1. Deploy to staging
  2. Run smoke tests
  3. Deploy to production
  4. Run health checks

## Documentation Standards

### Service README Template

```markdown
# [Service Name]

## Overview
[2-3 sentences describing the service]

## Responsibilities
- Responsibility 1
- Responsibility 2
- Responsibility 3

## API Endpoints
- GET /api/v1/[resource]
- POST /api/v1/[resource]
- GET /api/v1/[resource]/{id}
- PUT /api/v1/[resource]/{id}
- DELETE /api/v1/[resource]/{id}

## Database Schema
[Link to schema documentation]

## Configuration
[Configuration options and environment variables]

## Running Locally
[Instructions for running service locally]

## Testing
[Testing instructions and coverage]

## Deployment
[Deployment instructions]

## Troubleshooting
[Common issues and solutions]
```

### Code Documentation Standards

#### Java
```java
/**
 * Creates a new user account.
 *
 * @param request the user creation request containing email, password, etc.
 * @return the created user response with ID and details
 * @throws UserAlreadyExistsException if email already registered
 * @throws InvalidPasswordException if password doesn't meet requirements
 */
public UserResponse createUser(UserRequest request) {
    // Implementation
}
```

#### C#
```csharp
/// <summary>
/// Creates a new user account.
/// </summary>
/// <param name="request">The user creation request containing email, password, etc.</param>
/// <returns>The created user response with ID and details</returns>
/// <exception cref="UserAlreadyExistsException">Thrown if email already registered</exception>
/// <exception cref="InvalidPasswordException">Thrown if password doesn't meet requirements</exception>
public UserResponse CreateUser(UserRequest request)
{
    // Implementation
}
```

#### Node.js
```javascript
/**
 * Creates a new user account.
 *
 * @param {UserRequest} request - The user creation request containing email, password, etc.
 * @returns {Promise<UserResponse>} The created user response with ID and details
 * @throws {UserAlreadyExistsException} If email already registered
 * @throws {InvalidPasswordException} If password doesn't meet requirements
 */
async function createUser(request) {
    // Implementation
}
```

## Deployment Checklist

- [ ] All tests passing (unit, integration, contract)
- [ ] Code coverage > 80%
- [ ] SonarQube rating A
- [ ] Security scanning passed
- [ ] Docker image built and scanned
- [ ] Documentation updated
- [ ] ADRs updated if needed
- [ ] Performance tests passed
- [ ] Monitoring dashboards configured
- [ ] Alerting rules configured
- [ ] Rollback plan documented

