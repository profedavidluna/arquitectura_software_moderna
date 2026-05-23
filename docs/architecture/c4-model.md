# C4 Model Architecture Documentation

## Overview

The C4 model is a hierarchical approach to software architecture documentation that provides four levels of abstraction:

1. **System Context Diagram** - High-level system overview
2. **Container Diagram** - Major containers and their interactions
3. **Component Diagram** - Internal components of each container
4. **Code Diagram** - Code-level details (classes, methods)

## Level 1: System Context Diagram

### Description
Shows the ecommerce platform as a single box and its relationships with external systems and users.

### Diagram Elements

**System**: Ecommerce Platform
- **Purpose**: Online shopping platform for customers
- **Scope**: User management, product catalog, shopping cart, orders, payments, inventory, notifications, analytics

**External Systems**:
1. **Payment Gateway** (Stripe/PayPal) - Processes credit card payments
2. **Email Service** (SendGrid/Mailgun) - Sends transactional emails
3. **SMS Service** (Twilio) - Sends order notifications via SMS
4. **Shipping Provider API** (FedEx/UPS) - Calculates shipping costs and tracks shipments
5. **Analytics Service** (Google Analytics) - Tracks user behavior

**Actors**:
1. **Customer** - Browses products, adds to cart, places orders
2. **Admin** - Manages products, inventory, orders, users
3. **Support Agent** - Views order details, assists customers

### Relationships
- Customer interacts with Ecommerce Platform via web/mobile app
- Ecommerce Platform integrates with Payment Gateway for payments
- Ecommerce Platform sends emails via Email Service
- Ecommerce Platform sends SMS via SMS Service
- Ecommerce Platform calculates shipping via Shipping Provider API
- Ecommerce Platform sends analytics data to Analytics Service

## Level 2: Container Diagram

### Description
Shows the high-level technology choices and how responsibilities are distributed across containers.

### Containers

#### 1. Web Application (React/Next.js)
- **Technology**: React 18, Next.js 14, TypeScript
- **Purpose**: Customer-facing web interface
- **Responsibilities**: Product browsing, cart management, checkout, user account
- **Deployment**: Docker container, served via Nginx

#### 2. Mobile App (React Native)
- **Technology**: React Native, TypeScript
- **Purpose**: Mobile application for customers
- **Responsibilities**: Same as web application, optimized for mobile
- **Deployment**: App Store, Google Play Store

#### 3. API Gateway (Kong)
- **Technology**: Kong API Gateway
- **Purpose**: Single entry point for all API requests
- **Responsibilities**: Request routing, rate limiting, authentication, request/response transformation
- **Deployment**: Docker container

#### 4. Microservices (8 services)
Each service is a separate container:

##### 4.1 User Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: User management and authentication
- **Database**: PostgreSQL (user_db)
- **Port**: 8082

##### 4.2 Product Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Product catalog management
- **Database**: PostgreSQL (product_db)
- **Port**: 8083

##### 4.3 Cart Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Shopping cart management
- **Database**: Redis
- **Port**: 8084

##### 4.4 Order Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Order processing and management
- **Database**: PostgreSQL (order_db)
- **Port**: 8085

##### 4.5 Payment Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Payment processing
- **Database**: PostgreSQL (payment_db)
- **Port**: 8086

##### 4.6 Inventory Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Inventory management
- **Database**: PostgreSQL (inventory_db)
- **Port**: 8087

##### 4.7 Notification Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Sending notifications (email, SMS)
- **Database**: PostgreSQL (notification_db)
- **Port**: 8088

##### 4.8 Analytics Service
- **Technology**: Java/Spring Boot or .NET/ASP.NET Core or Node.js/Express or Python/FastAPI
- **Purpose**: Business analytics and reporting
- **Database**: Elasticsearch
- **Port**: 8089

#### 5. Message Broker (Apache Kafka)
- **Technology**: Apache Kafka with 3 brokers
- **Purpose**: Asynchronous communication between services
- **Responsibilities**: Event streaming, message queuing, event sourcing
- **Deployment**: Docker container cluster

#### 6. Authentication Server (Keycloak)
- **Technology**: Keycloak
- **Purpose**: Centralized authentication and authorization
- **Database**: PostgreSQL (keycloak_db)
- **Port**: 8180

#### 7. Monitoring Stack
- **Prometheus**: Metrics collection (port 9090)
- **Grafana**: Metrics visualization (port 3000)
- **Jaeger**: Distributed tracing (port 16686)
- **ELK Stack**: Centralized logging (ports 9200, 5601, 5000)

#### 8. Databases (PostgreSQL Cluster)
- 7 PostgreSQL instances (one per service that needs relational data)
- 1 PostgreSQL instance for Keycloak
- Each with separate schema and credentials

### Communication Patterns

1. **Synchronous**: REST APIs via API Gateway
2. **Asynchronous**: Kafka events for cross-service communication
3. **Database**: Each service connects to its own database
4. **Caching**: Redis for session management and cart data

## Level 3: Component Diagram

### User Service Components

#### 1. API Layer
- **UserController**: REST endpoints for user operations
- **AuthController**: Authentication endpoints (login, register, token refresh)
- **ProfileController**: User profile management

#### 2. Business Logic Layer
- **UserService**: User management business logic
- **AuthService**: Authentication and authorization logic
- **ProfileService**: Profile management logic

#### 3. Data Access Layer
- **UserRepository**: Database operations for users
- **RoleRepository**: Database operations for roles and permissions
- **SessionRepository**: Session management

#### 4. Integration Layer
- **KeycloakClient**: Integration with Keycloak for authentication
- **EventPublisher**: Publishes user events to Kafka

#### 5. Cross-Cutting Concerns
- **ExceptionHandler**: Global exception handling
- **RequestValidator**: Input validation
- **MetricsCollector**: Collects metrics for monitoring

### Product Service Components

#### 1. API Layer
- **ProductController**: REST endpoints for product operations
- **CategoryController**: Product category management
- **ReviewController**: Product reviews and ratings

#### 2. Business Logic Layer
- **ProductService**: Product management business logic
- **CategoryService**: Category management logic
- **ReviewService**: Review management logic
- **SearchService**: Product search functionality

#### 3. Data Access Layer
- **ProductRepository**: Database operations for products
- **CategoryRepository**: Database operations for categories
- **ReviewRepository**: Database operations for reviews
- **SearchRepository**: Elasticsearch operations for search

#### 4. Integration Layer
- **EventPublisher**: Publishes product events to Kafka
- **EventConsumer**: Consumes events from other services

#### 5. Cross-Cutting Concerns
- **CacheManager**: Manages product caching
- **ImageProcessor**: Handles product image processing
- **MetricsCollector**: Collects metrics for monitoring

## Level 4: Code Diagram

### User Service - User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Address> addresses;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PaymentMethod> paymentMethods;
    
    // Getters, setters, constructors
}

public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, DELETED
}
```

### User Service - UserService Class

```java
@Service
@Transactional
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, 
                      EventPublisher eventPublisher,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        // Validate email doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        
        // Create user entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // Save to database
        User savedUser = userRepository.save(user);
        
        // Publish user created event
        eventPublisher.publishUserCreatedEvent(savedUser);
        
        log.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }
    
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
    
    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = getUserById(userId);
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        
        // Publish user updated event
        eventPublisher.publishUserUpdatedEvent(updatedUser);
        
        return updatedUser;
    }
    
    public void deleteUser(UUID userId) {
        User user = getUserById(userId);
        user.setStatus(UserStatus.DELETED);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        // Publish user deleted event
        eventPublisher.publishUserDeletedEvent(userId);
    }
    
    public Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        return userRepository.searchUsers(criteria, pageable);
    }
}
```

## Deployment Architecture

### Development Environment
- **Orchestration**: Docker Compose
- **Networking**: Docker bridge network
- **Storage**: Docker volumes for persistent data
- **Configuration**: Environment variables in .env files

### Production Environment
- **Orchestration**: Kubernetes
- **Networking**: Service mesh (Istio or Linkerd)
- **Storage**: Persistent volumes (AWS EBS, Azure Disk, GCP Persistent Disk)
- **Configuration**: ConfigMaps and Secrets
- **CI/CD**: GitHub Actions, ArgoCD for GitOps

### Scaling Strategy
- **Horizontal Scaling**: Multiple replicas of stateless services
- **Vertical Scaling**: Database resources based on load
- **Auto-scaling**: Based on CPU, memory, and custom metrics
- **Load Balancing**: Round-robin with health checks

## Security Architecture

### Authentication
- **Protocol**: OAuth 2.0 / OpenID Connect
- **Provider**: Keycloak
- **Token Types**: JWT access tokens and refresh tokens
- **Token Validation**: Signature verification, expiration checks

### Authorization
- **Role-Based Access Control (RBAC)**: User roles (customer, admin, support)
- **Permission-Based Access**: Fine-grained permissions for API endpoints
- **Resource-Level Authorization**: Users can only access their own resources

### Network Security
- **TLS/SSL**: All external communication encrypted
- **API Gateway**: Rate limiting, IP whitelisting, request validation
- **Service Mesh**: Mutual TLS between services
- **Network Policies**: Restrict service-to-service communication

### Data Security
- **Encryption at Rest**: Database encryption
- **Encryption in Transit**: TLS for all communications
- **Secrets Management**: HashiCorp Vault or Kubernetes Secrets
- **Data Masking**: Sensitive data masked in logs

## Monitoring and Observability

### Metrics
- **Application Metrics**: Request rate, error rate, latency, throughput
- **Business Metrics**: Orders per hour, revenue, conversion rate
- **Infrastructure Metrics**: CPU, memory, disk, network
- **Database Metrics**: Connection pool, query performance, replication lag

### Logging
- **Structured Logging**: JSON format with consistent fields
- **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Log Levels**: DEBUG, INFO, WARN, ERROR, FATAL
- **Trace Context**: Trace IDs propagated across services

### Tracing
- **Distributed Tracing**: Jaeger for end-to-end request tracing
- **Sampling Rate**: 10% of requests in production
- **Span Tags**: Business context added to spans
- **Performance Analysis**: Identify bottlenecks and slow operations

### Alerting
- **Alert Rules**: Prometheus alert rules for critical conditions
- **Notification Channels**: Email, Slack, PagerDuty
- **Escalation Policies**: Multi-level escalation based on severity
- **Runbooks**: Documentation for responding to alerts

## Disaster Recovery

### Backup Strategy
- **Database Backups**: Daily full backups, hourly incremental
- **Configuration Backups**: Version-controlled in Git
- **Media Backups**: Product images and user uploads to object storage

### Recovery Objectives
- **Recovery Time Objective (RTO)**: 4 hours for critical services
- **Recovery Point Objective (RPO)**: 1 hour for transactional data
- **Data Retention**: 30 days for backups, 7 years for compliance data

### High Availability
- **Multi-AZ Deployment**: Services deployed across availability zones
- **Database Replication**: Master-slave replication with failover
- **Load Balancers**: Multi-region load balancing
- **CDN**: Static content served via CDN

## Cost Optimization

### Resource Optimization
- **Right-sizing**: Match instance types to workload requirements
- **Auto-scaling**: Scale down during low-traffic periods
- **Reserved Instances**: Commit to 1-3 year terms for predictable workloads
- **Spot Instances**: Use for batch processing and non-critical workloads

### Storage Optimization
- **Data Tiering**: Move cold data to cheaper storage
- **Compression**: Compress logs and backup data
- **Deduplication**: Remove duplicate data where possible
- **Lifecycle Policies**: Automatically delete old data

### Network Optimization
- **CDN**: Reduce origin server load
- **Compression**: Gzip/Brotli compression for API responses
- **Caching**: Cache frequently accessed data
- **Connection Pooling**: Reuse database connections

## Evolution and Maintenance

### Versioning Strategy
- **API Versioning**: URL path versioning (/api/v1/, /api/v2/)
- **Service Versioning**: Semantic versioning for services
- **Database Versioning**: Migration scripts for schema changes
- **Configuration Versioning**: Git for configuration management

### Deprecation Policy
- **Announcement Period**: 6 months notice for deprecated features
- **Migration Path**: Clear migration documentation
- **Backward Compatibility**: Maintain compatibility during transition
- **Sunset Schedule**: Gradual phase-out with monitoring

### Technical Debt Management
- **Regular Reviews**: Quarterly architecture reviews
- **Debt Tracking**: Track technical debt in issue tracker
- **Refactoring Sprints**: Dedicated time for refactoring
- **Quality Gates**: Code quality metrics and thresholds

## Conclusion

This C4 model documentation provides a comprehensive view of the ecommerce platform architecture at multiple levels of abstraction. The architecture is designed to be scalable, maintainable, and suitable for educational purposes while demonstrating modern software architecture patterns and practices.

The hierarchical approach allows different stakeholders to understand the system at the appropriate level of detail, from high-level system context to detailed code implementation.

**Last Updated**: April 2024
**Version**: 1.0.0