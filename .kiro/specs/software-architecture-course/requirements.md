# Requirements Document: Software Architecture Course - MVC Architecture Patterns

## Introduction

This course teaches software architecture through practical implementation of an ecommerce domain using three distinct architecture patterns: **Hexagonal Architecture (Ports & Adapters)**, **Vertical Layer Architecture (Layered)**, and **Clean Architecture**. Each pattern will be implemented in four programming languages (Java, .NET, Node.js, Python) to demonstrate how architectural decisions affect code organization, dependencies, and testability.

The ecommerce domain covers User Management, Product Catalog, Shopping Cart, and Checkout functionality. The focus is on understanding how each architecture handles the same business requirements differently, emphasizing separation of concerns, dependency management, and architectural trade-offs.

## Glossary

- **Hexagonal Architecture**: Architecture pattern that separates core domain logic from external concerns (UI, databases, frameworks) through ports and adapters
- **Vertical Layer Architecture**: Traditional layered architecture with Presentation, Business Logic, and Data layers, where dependencies flow downward
- **Clean Architecture**: Architecture pattern following the Dependency Rule where inner layers (business logic) don't depend on outer layers (frameworks, UI)
- **Port**: Interface that defines how the core domain communicates with the outside world
- **Adapter**: Implementation that connects ports to external systems (database, web framework, messaging)
- **Entity**: Business object that contains business logic and invariants
- **Use Case**: Business operation that coordinates entities to accomplish a goal
- **Controller**: Component that receives input and translates it into use case calls
- **Presenter**: Component that formats data from use cases for display in the UI
- **Framework**: External libraries and tools (Spring Boot, ASP.NET Core, Express.js, FastAPI)
- **Database**: Persistence mechanism (PostgreSQL, Redis)
## Functional Requirements

### FR-1: User Management

#### FR-1.1: User Registration
- **Description**: Users can create new accounts with email and password
- **Actors**: Anonymous user
- **Preconditions**: User has valid email address
- **Steps**:
  1. User provides email, password, first name, last name
  2. System validates email format and password strength
  3. System checks email uniqueness
  4. System creates user account
  5. System sends verification email
- **Postconditions**: User account created, verification email sent
- **Acceptance Criteria**:
  - Email must be unique
  - Password must be at least 8 characters with uppercase, lowercase, number, special char
  - Verification email sent within 5 seconds
  - User cannot login until email verified

#### FR-1.2: User Login
- **Description**: Users can authenticate with email and password
- **Actors**: Registered user
- **Preconditions**: User account exists and is verified
- **Steps**:
  1. User provides email and password
  2. System validates credentials
  3. System generates JWT token
  4. System returns token to client
- **Postconditions**: User authenticated, token issued
- **Acceptance Criteria**:
  - Login succeeds with correct credentials
  - Login fails with incorrect credentials
  - Token valid for 15 minutes
  - Refresh token valid for 7 days

#### FR-1.3: User Profile Management
- **Description**: Users can view and update their profile information
- **Actors**: Authenticated user
- **Preconditions**: User logged in
- **Steps**:
  1. User requests profile information
  2. System retrieves user data
  3. User updates profile fields
  4. System validates and saves changes
- **Postconditions**: Profile updated
- **Acceptance Criteria**:
  - User can view all profile fields
  - User can update first name, last name, phone
  - Email cannot be changed
  - Changes saved within 2 seconds

#### FR-1.4: Address Management
- **Description**: Users can manage multiple delivery addresses
- **Actors**: Authenticated user
- **Preconditions**: User logged in
- **Steps**:
  1. User adds new address (street, city, state, postal code, country)
  2. System validates address format
  3. System saves address
  4. User can set default address
- **Postconditions**: Address saved
- **Acceptance Criteria**:
  - User can add up to 5 addresses
  - User can set one default address
  - User can delete addresses
  - Address validation prevents invalid entries
### FR-2: Product Catalog

#### FR-2.1: Browse Products
- **Description**: Users can view product catalog with pagination
- **Actors**: Any user (authenticated or anonymous)
- **Preconditions**: None
- **Steps**:
  1. User requests product list
  2. System retrieves products with pagination
  3. System returns product data
- **Postconditions**: Product list displayed
- **Acceptance Criteria**:
  - Default page size: 20 products
  - Maximum page size: 100 products
  - Response time < 200ms
  - Products sorted by relevance or date

#### FR-2.2: Search Products
- **Description**: Users can search products by name, category, or price range
- **Actors**: Any user
- **Preconditions**: None
- **Steps**:
  1. User enters search criteria
  2. System searches product database
  3. System returns matching products
- **Postconditions**: Search results displayed
- **Acceptance Criteria**:
  - Search by product name (partial match)
  - Search by category
  - Filter by price range
  - Response time < 500ms
  - Results sorted by relevance

#### FR-2.3: View Product Details
- **Description**: Users can view detailed product information
- **Actors**: Any user
- **Preconditions**: Product exists
- **Steps**:
  1. User selects product
  2. System retrieves product details
  3. System displays product information
- **Postconditions**: Product details displayed
- **Acceptance Criteria**:
  - Display product name, description, price, images
  - Display stock availability
  - Display customer reviews and ratings
  - Display related products
  - Response time < 200ms

#### FR-2.4: Product Ratings and Reviews
- **Description**: Users can rate and review products they purchased
- **Actors**: Authenticated user who purchased product
- **Preconditions**: User purchased product, order delivered
- **Steps**:
  1. User submits rating (1-5 stars) and review text
  2. System validates review
  3. System saves review
  4. System updates product rating
- **Postconditions**: Review saved, product rating updated
- **Acceptance Criteria**:
  - Rating must be 1-5 stars
  - Review text max 1000 characters
  - Only verified purchasers can review
  - Reviews moderated before display (optional)
### FR-3: Shopping Cart

#### FR-3.1: Add to Cart
- **Description**: Users can add products to shopping cart
- **Actors**: Any user
- **Preconditions**: Product exists and has stock
- **Steps**:
  1. User selects product and quantity
  2. System validates quantity against stock
  3. System adds item to cart
  4. System updates cart total
- **Postconditions**: Item added to cart
- **Acceptance Criteria**:
  - Quantity must be positive integer
  - Quantity cannot exceed available stock
  - Cart persists across sessions
  - Cart updated within 1 second

#### FR-3.2: View Cart
- **Description**: Users can view their shopping cart
- **Actors**: Any user
- **Preconditions**: None
- **Steps**:
  1. User requests cart
  2. System retrieves cart items
  3. System calculates totals
  4. System displays cart
- **Postconditions**: Cart displayed
- **Acceptance Criteria**:
  - Display all items with quantity and price
  - Display subtotal, tax, shipping, total
  - Display estimated delivery date
  - Response time < 200ms

#### FR-3.3: Update Cart Items
- **Description**: Users can modify quantities or remove items from cart
- **Actors**: Authenticated user
- **Preconditions**: Item in cart
- **Steps**:
  1. User updates quantity or removes item
  2. System validates changes
  3. System updates cart
  4. System recalculates totals
- **Postconditions**: Cart updated
- **Acceptance Criteria**:
  - User can increase/decrease quantity
  - User can remove items
  - Cart updated within 1 second
  - Totals recalculated correctly

#### FR-3.4: Apply Coupon
- **Description**: Users can apply discount coupons to cart
- **Actors**: Authenticated user
- **Preconditions**: Valid coupon code exists
- **Steps**:
  1. User enters coupon code
  2. System validates coupon (active, not expired, applicable)
  3. System applies discount
  4. System recalculates total
- **Postconditions**: Discount applied
- **Acceptance Criteria**:
  - Coupon validation within 1 second
  - Discount applied correctly
  - Cannot apply expired coupons
  - Cannot apply multiple coupons (or limit to 1)
### FR-4: Checkout and Orders

#### FR-4.1: Checkout Process
- **Description**: Users can proceed through multi-step checkout
- **Actors**: Authenticated user with items in cart
- **Preconditions**: Cart not empty, user logged in
- **Steps**:
  1. User reviews cart
  2. User selects delivery address
  3. User selects shipping method
  4. User reviews order summary
  5. User proceeds to payment
- **Postconditions**: Order created in PENDING state
- **Acceptance Criteria**:
  - All steps must be completed
  - User can go back to previous steps
  - Order summary accurate
  - Checkout completes within 5 seconds

#### FR-4.2: Create Order
- **Description**: System creates order from cart
- **Actors**: System (triggered by user checkout)
- **Preconditions**: Checkout completed, payment authorized
- **Steps**:
  1. System reserves inventory
  2. System creates order record
  3. System clears cart
  4. System publishes order.created event
- **Postconditions**: Order created, inventory reserved
- **Acceptance Criteria**:
  - Order ID generated
  - Order status set to PENDING
  - Inventory reserved
  - Order confirmation email sent

#### FR-4.3: View Order Details
- **Description**: Users can view their order information
- **Actors**: Authenticated user
- **Preconditions**: Order exists
- **Steps**:
  1. User requests order details
  2. System retrieves order information
  3. System displays order
- **Postconditions**: Order details displayed
- **Acceptance Criteria**:
  - Display order ID, date, status
  - Display items, quantities, prices
  - Display delivery address
  - Display tracking information (if shipped)
  - Response time < 200ms

#### FR-4.4: Order History
- **Description**: Users can view their order history
- **Actors**: Authenticated user
- **Preconditions**: User has placed orders
- **Steps**:
  1. User requests order history
  2. System retrieves orders with pagination
  3. System displays order list
- **Postconditions**: Order history displayed
- **Acceptance Criteria**:
  - Display all user orders
  - Pagination with 10 orders per page
  - Filter by status (pending, confirmed, shipped, delivered)
  - Sort by date (newest first)
  - Response time < 200ms

#### FR-4.5: Cancel Order
- **Description**: Users can cancel orders in certain states
- **Actors**: Authenticated user
- **Preconditions**: Order in PENDING or CONFIRMED state
- **Steps**:
  1. User requests order cancellation
  2. System validates order state
  3. System cancels order
  4. System releases inventory reservation
  5. System initiates refund
- **Postconditions**: Order cancelled, refund initiated
- **Acceptance Criteria**:
  - Can only cancel PENDING or CONFIRMED orders
  - Cannot cancel SHIPPED or DELIVERED orders
  - Refund processed within 24 hours
  - Inventory released immediately
### FR-5: Payment Processing

#### FR-5.1: Process Payment
- **Description**: System processes payment for order
- **Actors**: System (triggered by user)
- **Preconditions**: Order created, payment details provided
- **Steps**:
  1. System validates payment details
  2. System calls payment gateway (Stripe/PayPal)
  3. Payment gateway processes transaction
  4. System records payment result
  5. System publishes payment event
- **Postconditions**: Payment processed, order status updated
- **Acceptance Criteria**:
  - Payment processed within 5 seconds
  - Payment result recorded
  - Order status updated to CONFIRMED if successful
  - Order status updated to PAYMENT_FAILED if unsuccessful
  - Retry failed payments up to 3 times

#### FR-5.2: Payment Methods
- **Description**: Users can use multiple payment methods
- **Actors**: Authenticated user
- **Preconditions**: None
- **Steps**:
  1. User selects payment method (credit card, PayPal, etc.)
  2. System processes payment with selected method
- **Postconditions**: Payment processed
- **Acceptance Criteria**:
  - Support credit/debit cards
  - Support PayPal
  - Support digital wallets (Apple Pay, Google Pay)
  - Securely store payment methods (PCI compliance)

#### FR-5.3: Refund Processing
- **Description**: System processes refunds for cancelled orders
- **Actors**: System or Admin
- **Preconditions**: Order cancelled or returned
- **Steps**:
  1. System initiates refund to original payment method
  2. Payment gateway processes refund
  3. System records refund
  4. System publishes refund event
- **Postconditions**: Refund processed
- **Acceptance Criteria**:
  - Refund processed within 24 hours
  - Refund amount matches original payment
  - Refund status tracked
  - Customer notified of refund
### FR-6: Inventory Management

#### FR-6.1: Track Stock Levels
- **Description**: System tracks product stock levels
- **Actors**: System
- **Preconditions**: Product exists
- **Steps**:
  1. System maintains stock count
  2. System updates stock on order
  3. System updates stock on return
- **Postconditions**: Stock levels accurate
- **Acceptance Criteria**:
  - Stock updated in real-time
  - Stock cannot go negative
  - Low stock alerts triggered at 10% of max stock
  - Stock reconciliation daily

#### FR-6.2: Reserve Inventory
- **Description**: System reserves inventory when order created
- **Actors**: System
- **Preconditions**: Order created, stock available
- **Steps**:
  1. System checks available stock
  2. System reserves stock for order
  3. System updates available count
- **Postconditions**: Inventory reserved
- **Acceptance Criteria**:
  - Reservation succeeds if stock available
  - Reservation fails if insufficient stock
  - Reservation expires after 30 minutes if not confirmed
  - Reserved stock not available for other orders

#### FR-6.3: Deplete Inventory
- **Description**: System depletes inventory when order confirmed
- **Actors**: System
- **Preconditions**: Order confirmed, inventory reserved
- **Steps**:
  1. System removes reserved stock from available
  2. System records depletion transaction
  3. System publishes inventory.depleted event
- **Postconditions**: Inventory depleted
- **Acceptance Criteria**:
  - Depletion recorded with order ID
  - Stock count accurate
  - Audit trail maintained
### FR-7: Notifications

#### FR-7.1: Order Confirmation Email
- **Description**: System sends order confirmation email
- **Actors**: System
- **Preconditions**: Order created
- **Steps**:
  1. System publishes order.created event
  2. Notification Service receives event
  3. Notification Service sends confirmation email
- **Postconditions**: Email sent
- **Acceptance Criteria**:
  - Email sent within 5 seconds
  - Email contains order details
  - Email includes tracking link
  - Email sent to correct address

#### FR-7.2: Payment Receipt Email
- **Description**: System sends payment receipt email
- **Actors**: System
- **Preconditions**: Payment processed successfully
- **Steps**:
  1. System publishes payment.processed event
  2. Notification Service receives event
  3. Notification Service sends receipt email
- **Postconditions**: Email sent
- **Acceptance Criteria**:
  - Email sent within 5 seconds
  - Email contains payment details
  - Email includes invoice
  - Email sent to correct address

#### FR-7.3: Shipping Notification
- **Description**: System sends shipping notification email
- **Actors**: System or Admin
- **Preconditions**: Order shipped
- **Steps**:
  1. System publishes order.shipped event
  2. Notification Service receives event
  3. Notification Service sends shipping email
- **Postconditions**: Email sent
- **Acceptance Criteria**:
  - Email sent within 5 seconds
  - Email contains tracking number
  - Email includes estimated delivery date
  - Email sent to correct address
## Non-Functional Requirements

### NFR-1: Architecture Pattern Compliance

#### NFR-1.1: Hexagonal Architecture Compliance
- **Requirement**: All implementations must follow Hexagonal Architecture principles
- **Measurement**: Code structure and dependency direction
- **Implementation**:
  - Core domain entities in separate package/module
  - Ports defined as interfaces in core layer
  - Adapters implement ports and depend on core
  - No dependencies from core to adapters
- **Rationale**: Demonstrate separation of concerns and testability

#### NFR-1.2: Vertical Layer Architecture Compliance
- **Requirement**: All implementations must follow Vertical Layer Architecture principles
- **Measurement**: Code structure and dependency direction
- **Implementation**:
  - Clear separation: Presentation, Business, Data layers
  - Dependencies flow downward (Presentation → Business → Data)
  - Each layer only depends on the layer below
  - No cross-layer dependencies
- **Rationale**: Demonstrate traditional layered architecture patterns

#### NFR-1.3: Clean Architecture Compliance
- **Requirement**: All implementations must follow Clean Architecture principles
- **Measurement**: Code structure and dependency direction
- **Implementation**:
  - Entities (business objects) in innermost layer
  - Use cases (business operations) in middle layer
  - Frameworks and drivers in outermost layer
  - Dependencies flow inward (outer → inner)
  - Inner layers unaware of outer layers
- **Rationale**: Demonstrate dependency inversion and business logic isolation

#### NFR-1.4: Architecture Comparison
- **Requirement**: Each architecture must demonstrate its strengths and trade-offs
- **Measurement**: Code organization, testability, maintainability
- **Implementation**:
  - Hexagonal: Emphasize testability through port/adapter separation
  - Vertical Layer: Emphasize simplicity and clear layer boundaries
  - Clean Architecture: Emphasize business logic independence from frameworks
- **Rationale**: Help students understand architectural decision trade-offs
### NFR-2: Code Organization

#### NFR-2.1: Package/Module Structure
- **Requirement**: Each architecture must have a clear package/module structure
- **Measurement**: File and folder organization
- **Implementation**:
  - Hexagonal: `core/`, `ports/`, `adapters/`
  - Vertical Layer: `presentation/`, `business/`, `data/`
  - Clean Architecture: `entities/`, `usecases/`, `interfaces/`, `adapters/`
- **Rationale**: Visual clarity of architectural boundaries

#### NFR-2.2: Dependency Direction
- **Requirement**: Dependencies must flow in the correct direction for each architecture
- **Measurement**: Import/dependency analysis
- **Implementation**:
  - Hexagonal: Core → Ports, Adapters → Ports
  - Vertical Layer: Presentation → Business → Data
  - Clean Architecture: Outer → Inner (inward dependency flow)
- **Rationale**: Demonstrate architectural discipline

#### NFR-2.3: Separation of Concerns
- **Requirement**: Business logic must be separated from infrastructure concerns
- **Measurement**: Code review and static analysis
- **Implementation**:
  - Business rules in core/use case classes
  - Framework-specific code in adapters/presenters
  - No framework imports in business logic
- **Rationale**: Testability and maintainability
### NFR-3: Testability

#### NFR-3.1: Unit Test Coverage
- **Requirement**: Minimum 80% code coverage with unit tests
- **Measurement**: Code coverage percentage
- **Implementation**:
  - JUnit 5 (Java), xUnit (.NET), Jest (Node), pytest (Python)
  - Mocking frameworks (Mockito, Moq, Jest mocks, unittest.mock)
  - Test automation in CI/CD pipeline
- **Rationale**: Catch bugs early and verify architectural compliance

#### NFR-3.2: Business Logic Testing
- **Requirement**: All business logic must be testable without framework dependencies
- **Measurement**: Test execution without framework startup
- **Implementation**:
  - Test entities and use cases in isolation
  - Mock ports/adapters for business logic tests
  - No database or framework startup required for unit tests
- **Rationale**: Fast feedback and architectural validation

#### NFR-3.3: Adapter Testing
- **Requirement**: All adapters must have integration tests
- **Measurement**: Integration test count and coverage
- **Implementation**:
  - Test database adapters with real database
  - Test web framework adapters with HTTP requests
  - Test messaging adapters with message broker
- **Rationale**: Verify integration with external systems

#### NFR-3.4: Architecture Validation Tests
- **Requirement**: Tests must verify architectural constraints
- **Measurement**: Architecture test coverage
- **Implementation**:
  - Test that core layer has no dependencies on adapters
  - Test that dependencies flow in correct direction
  - Test that business logic is framework-independent
- **Rationale**: Prevent architectural drift
### NFR-4: Performance

#### NFR-4.1: Response Time
- **Requirement**: API response time must be < 200ms (p95) for all endpoints
- **Measurement**: Response time from API endpoint to client
- **Exceptions**: 
  - Search operations: < 500ms (p95)
  - Report generation: < 5 seconds
- **Rationale**: User experience, system responsiveness

#### NFR-4.2: Database Query Performance
- **Requirement**: Database queries must complete within 10 seconds
- **Measurement**: Query execution time
- **Rationale**: Prevent slow queries from blocking system

#### NFR-4.3: Cache Performance
- **Requirement**: Cache hit rate must be > 80% for frequently accessed data
- **Measurement**: Cache hits / total requests
- **Rationale**: Reduce database load
### NFR-5: Security

#### NFR-5.1: Authentication
- **Requirement**: All API endpoints must require authentication (except public endpoints)
- **Measurement**: Unauthorized requests rejected
- **Implementation**:
  - JWT token validation
  - Token expiry enforcement
- **Rationale**: Prevent unauthorized access

#### NFR-5.2: Authorization
- **Requirement**: Users can only access their own data
- **Measurement**: Authorization checks on all endpoints
- **Implementation**:
  - Role-based access control (RBAC)
  - Resource ownership verification
- **Rationale**: Prevent data leakage

#### NFR-5.3: Data Encryption
- **Requirement**: Sensitive data must be encrypted
- **Measurement**: Encryption verification
- **Implementation**:
  - TLS 1.3 for data in transit
  - AES-256 for data at rest
- **Rationale**: Protect sensitive data

#### NFR-5.4: PCI Compliance
- **Requirement**: Payment data must comply with PCI DSS
- **Measurement**: PCI compliance audit
- **Implementation**:
  - No storage of full credit card numbers
  - Tokenization of payment methods
  - Secure payment gateway integration
- **Rationale**: Legal requirement
### NFR-6: Maintainability

#### NFR-6.1: Code Quality
- **Requirement**: Code must meet quality standards
- **Measurement**: Code coverage > 80%, SonarQube rating A
- **Implementation**:
  - Automated testing
  - Code reviews
  - Static analysis
- **Rationale**: Reduce bugs and technical debt

#### NFR-6.2: Documentation
- **Requirement**: Code must be well documented
- **Measurement**: Documentation completeness
- **Implementation**:
  - Class-level documentation
  - Method-level documentation
  - README files
  - Architecture Decision Records
- **Rationale**: Facilitate maintenance

#### NFR-6.3: Logging
- **Requirement**: All significant events must be logged
- **Measurement**: Log coverage
- **Implementation**:
  - Structured JSON logging
  - Log retention: 7 days
- **Rationale**: Facilitate debugging
### NFR-7: Deployability

#### NFR-7.1: Containerization
- **Requirement**: All services must be containerized
- **Measurement**: Docker image availability
- **Implementation**:
  - Dockerfile per service
  - Docker Compose for local development
- **Rationale**: Consistent deployment

#### NFR-7.2: Infrastructure as Code
- **Requirement**: Infrastructure must be defined as code
- **Measurement**: IaC coverage
- **Implementation**:
  - Docker Compose for local
  - Kubernetes manifests for production (optional)
- **Rationale**: Reproducible infrastructure

#### NFR-7.3: Automated Deployment
- **Requirement**: Deployment must be automated
- **Measurement**: Deployment automation coverage
- **Implementation**:
  - GitHub Actions CI/CD
  - Automated testing before deployment
- **Rationale**: Reduce deployment errors

#### NFR-7.4: Rollback Capability
- **Requirement**: System must support quick rollback
- **Measurement**: Rollback time < 5 minutes
- **Implementation**:
  - Version control for all artifacts
  - Docker image versioning
  - Database migration rollback
- **Rationale**: Minimize impact of failed deployments
### NFR-8: Architecture-Specific Requirements

#### NFR-8.1: Hexagonal Architecture Requirements

**NFR-8.1.1: Port Definition**
- **Requirement**: All external interfaces must be defined as ports (interfaces)
- **Measurement**: Port interface count
- **Implementation**:
  - UserRepository port for database access
  - EmailSender port for notifications
  - PaymentProcessor port for payment processing
- **Rationale**: Enable multiple adapter implementations and testability

**NFR-8.1.2: Adapter Implementation**
- **Requirement**: All external systems must be implemented as adapters
- **Measurement**: Adapter implementation count
- **Implementation**:
  - JPAUserRepository adapter implementing UserRepository port
  - SMTPEmailSender adapter implementing EmailSender port
  - StripePaymentAdapter adapter implementing PaymentProcessor port
- **Rationale**: Separate infrastructure concerns from business logic

**NFR-8.1.3: Core Independence**
- **Requirement**: Core domain must have no dependencies on adapters
- **Measurement**: Dependency analysis
- **Implementation**:
  - Core package imports only core classes
  - No framework imports in core layer
  - Business logic tests run without framework startup
- **Rationale**: Testability and framework independence

#### NFR-8.2: Vertical Layer Architecture Requirements

**NFR-8.2.1: Layer Separation**
- **Requirement**: Clear separation between Presentation, Business, and Data layers
- **Measurement**: Layer dependency analysis
- **Implementation**:
  - Presentation layer: Controllers, Views, DTOs
  - Business layer: Services, Business Objects, Validators
  - Data layer: Repositories, Entity Framework models, Database context
- **Rationale**: Clear boundaries and separation of concerns

**NFR-8.2.2: Downward Dependencies**
- **Requirement**: Dependencies must flow downward (Presentation → Business → Data)
- **Measurement**: Dependency direction analysis
- **Implementation**:
  - Presentation layer references Business layer
  - Business layer references Data layer
  - No upward or cross-layer dependencies
- **Rationale**: Maintain architectural discipline

**NFR-8.2.3: Layer-Specific Responsibilities**
- **Requirement**: Each layer must have clearly defined responsibilities
- **Measurement**: Code review and static analysis
- **Implementation**:
  - Presentation: Request/response handling, validation
  - Business: Business logic, domain rules, use cases
  - Data: Data access, persistence, ORM mapping
- **Rationale**: Prevent layer confusion and maintainability issues

#### NFR-8.3: Clean Architecture Requirements

**NFR-8.3.1: Entities (Business Objects)**
- **Requirement**: Business objects must be in the innermost layer
- **Measurement**: Entity count and location
- **Implementation**:
  - User, Product, Order, Cart entities
  - Business logic and invariants in entities
  - No framework dependencies
- **Rationale**: Core business logic isolation

**NFR-8.3.2: Use Cases (Business Operations)**
- **Requirement**: Business operations must be in the use case layer
- **Measurement**: Use case count and location
- **Implementation**:
  - RegisterUser, Login, AddToCart, CreateOrder use cases
  - Coordinate entities to accomplish business goals
  - Depend on entities and interfaces (ports)
- **Rationale**: Business logic orchestration

**NFR-8.3.3: Framework Independence**
- **Requirement**: Business logic must be independent of frameworks
- **Measurement**: Framework import analysis
- **Implementation**:
  - No framework imports in entities or use cases
  - Framework-specific code in adapters/presenters
  - Business logic tests run without framework startup
- **Rationale**: Testability and long-term maintainability
## Architecture-Specific Requirements

### AR-1: Hexagonal Architecture Implementation Requirements

#### AR-1.1: Core Domain Layer
- **Requirement**: Core domain must be framework-independent
- **Components**:
  - Entities (User, Product, Order, Cart, Address)
  - Value objects (Email, Money, Quantity)
  - Business logic and invariants
- **Constraints**:
  - No framework imports
  - No database dependencies
  - No external service dependencies
- **Rationale**: Testability and business logic isolation

#### AR-1.2: Ports (Interfaces)
- **Requirement**: All external interfaces must be defined as ports
- **Ports**:
  - UserRepository (database access)
  - EmailSender (notification service)
  - PaymentProcessor (payment gateway)
  - CartRepository (cart persistence)
  - OrderRepository (order persistence)
- **Constraints**:
  - Ports defined in core layer
  - No implementation in core layer
  - Clear contract for each port
- **Rationale**: Enable multiple adapter implementations

#### AR-1.3: Adapters (Implementations)
- **Requirement**: All external systems must be implemented as adapters
- **Adapters**:
  - JPAUserRepository (implements UserRepository)
  - SMTPEmailSender (implements EmailSender)
  - StripePaymentAdapter (implements PaymentProcessor)
  - RedisCartRepository (implements CartRepository)
  - PostgreSQLOrderRepository (implements OrderRepository)
- **Constraints**:
  - Adapters implement ports
  - Adapters can depend on core (ports)
  - No core dependencies on adapters
- **Rationale**: Separate infrastructure concerns

#### AR-1.4: Testability
- **Requirement**: All business logic must be testable without framework
- **Test Types**:
  - Unit tests for entities and business logic
  - Mock adapter tests for use cases
  - Integration tests for adapters
- **Constraints**:
  - Business logic tests run without database
  - Business logic tests run without framework startup
  - Mock adapters for use case testing
- **Rationale**: Fast feedback and architectural validation
### AR-2: Vertical Layer Architecture Implementation Requirements

#### AR-2.1: Presentation Layer
- **Requirement**: Handle user interface concerns
- **Components**:
  - Controllers (API endpoints)
  - ViewModels/DTOs (data transfer objects)
  - Request validation
  - Response formatting
- **Constraints**:
  - No business logic in controllers
  - No direct database access
  - Controllers depend on Business layer
- **Rationale**: Separation of concerns

#### AR-2.2: Business Layer
- **Requirement**: Handle business logic and domain rules
- **Components**:
  - Services (use cases, business operations)
  - Business objects (domain models)
  - Validators (business rules)
  - Mappers (object-to-object mapping)
- **Constraints**:
  - No framework dependencies
  - No direct database access
  - Business layer depends on Data layer for persistence
- **Rationale**: Business logic isolation

#### AR-2.3: Data Layer
- **Requirement**: Handle data persistence
- **Components**:
  - Repositories (data access abstraction)
  - Entity Framework models (database entities)
  - Database context
  - Migrations
- **Constraints**:
  - No business logic in repositories
  - Repositories depend on Data layer models
  - Data layer has no dependencies on other layers
- **Rationale**: Data access abstraction

#### AR-2.4: Dependency Flow
- **Requirement**: Dependencies must flow downward
- **Constraints**:
  - Presentation → Business
  - Business → Data
  - No upward dependencies
  - No cross-layer dependencies
- **Rationale**: Maintain architectural discipline
### AR-3: Clean Architecture Implementation Requirements

#### AR-3.1: Entities Layer (Innermost)
- **Requirement**: Business objects with core business logic
- **Components**:
  - User entity (business rules, invariants)
  - Product entity (business rules, invariants)
  - Order entity (business rules, invariants)
  - Cart entity (business rules, invariants)
- **Constraints**:
  - No framework dependencies
  - No database dependencies
  - No external service dependencies
  - Business logic and invariants in entities
- **Rationale**: Core business logic isolation

#### AR-3.2: Use Cases Layer (Middle)
- **Requirement**: Business operations that coordinate entities
- **Components**:
  - RegisterUser use case
  - Login use case
  - AddToCart use case
  - CreateOrder use case
  - GetProductDetails use case
- **Constraints**:
  - Depend on entities
  - Depend on interfaces (ports) for external services
  - No framework dependencies
  - No direct database access
- **Rationale**: Business logic orchestration

#### AR-3.3: Interfaces Layer (Boundary)
- **Requirement**: Define interfaces for external services
- **Components**:
  - IUserRepository interface
  - IEmailSender interface
  - IPaymentProcessor interface
  - ICartRepository interface
- **Constraints**:
  - Define contracts for external services
  - No implementation
  - Depend on entities
- **Rationale**: Dependency inversion

#### AR-3.4: Adapters Layer (Outermost)
- **Requirement**: Implement interfaces and framework-specific code
- **Components**:
  - UserController (web framework adapter)
  - JPAUserRepository (database adapter)
  - SMTPEmailSender (notification adapter)
  - StripePaymentAdapter (payment adapter)
- **Constraints**:
  - Implement interfaces from interfaces layer
  - Can depend on all inner layers
  - Framework-specific code only
- **Rationale**: Framework isolation

#### AR-3.5: Dependency Rule
- **Requirement**: Dependencies must flow inward
- **Constraints**:
  - Adapters depend on interfaces
  - Interfaces depend on entities
  - Use cases depend on entities and interfaces
  - No outer layer dependencies on inner layers
- **Rationale**: Maintain architectural discipline
## Testing Requirements

### TR-1: Architecture Pattern Testing

#### TR-1.1: Hexagonal Architecture Tests
- **Requirement**: Tests must verify Hexagonal Architecture compliance
- **Test Types**:
  - Entity unit tests (business logic in isolation)
  - Port mock tests (use cases with mocked adapters)
  - Adapter integration tests (real database, email server)
  - Architecture validation tests (dependency direction)
- **Rationale**: Verify port/adapter separation and testability

#### TR-1.2: Vertical Layer Architecture Tests
- **Requirement**: Tests must verify Vertical Layer Architecture compliance
- **Test Types**:
  - Controller unit tests (request handling)
  - Service unit tests (business logic)
  - Repository integration tests (database access)
  - Architecture validation tests (layer dependencies)
- **Rationale**: Verify layer separation and dependency flow

#### TR-1.3: Clean Architecture Tests
- **Requirement**: Tests must verify Clean Architecture compliance
- **Test Types**:
  - Entity unit tests (business logic in isolation)
  - Use case unit tests (business operations with mocked interfaces)
  - Adapter integration tests (framework and database)
  - Architecture validation tests (dependency direction)
- **Rationale**: Verify layer separation and dependency rule
### TR-2: Business Logic Testing

#### TR-2.1: Entity Tests
- **Requirement**: All business logic in entities must be tested
- **Test Types**:
  - Business rule validation
  - Invariant preservation
  - State transitions
- **Constraints**:
  - No framework dependencies
  - No database dependencies
  - Fast execution (< 100ms per test)
- **Rationale**: Verify business logic correctness

#### TR-2.2: Use Case Tests
- **Requirement**: All business operations must be tested
- **Test Types**:
  - Happy path scenarios
  - Error scenarios
  - Edge cases
- **Constraints**:
  - Mock external services (adapters)
  - No database dependencies
  - No framework dependencies
- **Rationale**: Verify business logic orchestration
### TR-3: Adapter Testing

#### TR-3.1: Database Adapter Tests
- **Requirement**: All database adapters must have integration tests
- **Test Types**:
  - CRUD operations
  - Query performance
  - Transaction handling
- **Constraints**:
  - Use TestContainers or in-memory database
  - Real database interactions
- **Rationale**: Verify database integration

#### TR-3.2: External Service Adapter Tests
- **Requirement**: All external service adapters must have integration tests
- **Test Types**:
  - Email sending
  - Payment processing
  - External API calls
- **Constraints**:
  - Use test doubles (mock servers, stubs)
  - Real external service interactions
- **Rationale**: Verify external service integration
### TR-4: Architecture Validation Tests

#### TR-4.1: Dependency Direction Tests
- **Requirement**: Tests must verify dependency direction
- **Test Types**:
  - Dependency analysis (no forbidden dependencies)
  - Layer boundary tests (layer-specific imports)
  - Architecture compliance tests
- **Tools**: ArchUnit (Java), NetArchTest (.NET), pytest-arch (Python)
- **Rationale**: Prevent architectural drift

#### TR-4.2: Framework Independence Tests
- **Requirement**: Tests must verify business logic is framework-independent
- **Test Types**:
  - Business logic tests without framework startup
  - No framework imports in business logic
  - Fast test execution
- **Rationale**: Verify architectural discipline
## Documentation Requirements

### DR-1: Architecture Documentation

#### DR-1.1: Architecture Overview
- **Requirement**: Each implementation must include architecture overview
- **Components**:
  - Architecture diagram (C4 model)
  - Layer/port descriptions
  - Component responsibilities
  - Dependency flow
- **Rationale**: Help students understand architecture

#### DR-1.2: Architecture Decision Records
- **Requirement**: All architectural decisions must be documented
- **Components**:
  - ADR template
  - Decision context
  - Decision rationale
  - Alternatives considered
- **Rationale**: Document architectural reasoning

#### DR-1.3: Architecture Comparison
- **Requirement**: Document differences between architectures
- **Components**:
  - Hexagonal vs Vertical Layer vs Clean Architecture
  - Trade-offs and benefits
  - When to use each pattern
- **Rationale**: Help students choose appropriate architecture
### DR-2: Code Documentation

#### DR-2.1: Class Documentation
- **Requirement**: All classes must have documentation
- **Components**:
  - Class purpose and responsibilities
  - Dependencies
  - Usage examples
- **Rationale**: Facilitate code understanding

#### DR-2.2: Method Documentation
- **Requirement**: All public methods must have documentation
- **Components**:
  - Method purpose
  - Parameters
  - Return values
  - Exceptions
- **Rationale**: Facilitate method usage

#### DR-2.3: Architecture-Specific Documentation
- **Requirement**: Document architecture-specific patterns
- **Components**:
  - Port/adapter patterns (Hexagonal)
  - Layer responsibilities (Vertical Layer)
  - Entities and use cases (Clean Architecture)
- **Rationale**: Demonstrate architectural patterns
### DR-3: README Documentation

#### DR-3.1: Main README
- **Requirement**: Each implementation must have a README
- **Components**:
  - Project overview
  - Architecture pattern used
  - Technology stack
  - Quick start guide
  - Running locally
  - Testing instructions
- **Rationale**: Help students get started

#### DR-3.2: Architecture README
- **Requirement**: Document architecture-specific details
- **Components**:
  - Architecture overview
  - Layer/port structure
  - Component responsibilities
  - Dependency flow
  - Architecture patterns used
- **Rationale**: Help students understand architecture
