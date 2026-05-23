# Local Development Guide

This guide provides detailed instructions for setting up and running the ecommerce platform locally for development purposes.

## Prerequisites

### Required Software

1. **Docker & Docker Compose**
   ```bash
   # Install Docker Desktop or Docker Engine
   # Verify installation
   docker --version
   docker-compose --version
   ```

2. **Git**
   ```bash
   # Install Git
   git --version
   ```

3. **Java Development Kit (JDK 17+)**
   ```bash
   # For Java services
   java --version
   ```

4. **.NET SDK 6+**
   ```bash
   # For .NET services
   dotnet --version
   ```

5. **Node.js 18+**
   ```bash
   # For Node.js services
   node --version
   npm --version
   ```

6. **Python 3.9+**
   ```bash
   # For Python services
   python --version
   pip --version
   ```

### Optional Tools

1. **IDE Recommendations**:
   - **IntelliJ IDEA** or **VS Code** for Java/.NET/Node.js/Python
   - **Postman** or **Insomnia** for API testing
   - **DBeaver** or **pgAdmin** for database management
   - **Kafka Tool** or **Kowl** for Kafka management

2. **Browser Extensions**:
   - **JSON Formatter** for better JSON viewing
   - **ModHeader** for modifying HTTP headers
   - **React Developer Tools** for React debugging

## Development Environment Setup

### Step 1: Clone the Repository

```bash
# Clone the repository
git clone https://github.com/yourusername/software-architecture-course.git
cd software-architecture-course

# Checkout the desired branch
git checkout microservices-architecture
```

### Step 2: Start Infrastructure Services

The infrastructure services (databases, Kafka, Keycloak, monitoring) run in Docker containers.

```bash
# Navigate to shared directory
cd shared

# Start all infrastructure services
docker-compose up -d

# Check service status
docker-compose ps

# View logs for specific service
docker-compose logs -f postgres
```

### Step 3: Verify Infrastructure Services

Wait for all services to be ready (2-3 minutes), then verify:

```bash
# Check PostgreSQL
docker-compose exec user-db psql -U postgres -d user_db -c "\dt"

# Check Kafka
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Keycloak
curl http://localhost:8180/health

# Check Elasticsearch
curl http://localhost:9200/_cluster/health

# Check Prometheus
curl http://localhost:9090/-/healthy
```

### Step 4: Initialize Databases

Database schemas are automatically initialized. To verify:

```bash
# List all databases
docker-compose exec user-db psql -U postgres -c "\l"

# Check user database tables
docker-compose exec user-db psql -U postgres -d user_db -c "\dt"

# Check order database tables
docker-compose exec order-db psql -U postgres -d order_db -c "\dt"
```

### Step 5: Configure Keycloak

1. Open http://localhost:8180 in your browser
2. Click "Administration Console"
3. Login with admin/admin
4. Create realm "ecommerce"
5. Create clients:
   - web-client (public)
   - service-account (confidential)
6. Create users:
   - customer@ecommerce.com / customer123
   - admin@ecommerce.com / admin123
7. Create roles:
   - customer
   - admin
   - support

## Service Development

### Choosing Your Implementation Language

The course supports implementations in four languages:

#### Option 1: Java (Spring Boot)
```bash
# Navigate to Java services
cd microservices-architecture/java-microservices

# List available services
ls -la

# Build and run a service
cd user-service
mvn clean package
mvn spring-boot:run
```

#### Option 2: .NET (ASP.NET Core)
```bash
# Navigate to .NET services
cd microservices-architecture/dotnet-microservices

# List available services
ls -la

# Build and run a service
cd user-service
dotnet build
dotnet run
```

#### Option 3: Node.js (Express)
```bash
# Navigate to Node.js services
cd microservices-architecture/nodejs-microservices

# List available services
ls -la

# Build and run a service
cd user-service
npm install
npm start
```

#### Option 4: Python (FastAPI)
```bash
# Navigate to Python services
cd microservices-architecture/python-microservices

# List available services
ls -la

# Build and run a service
cd user-service
pip install -r requirements.txt
python main.py
```

### Service Configuration

Each service has configuration files:

#### Environment Variables
```bash
# Example .env file for user service
DB_HOST=localhost
DB_PORT=5432
DB_NAME=user_db
DB_USER=postgres
DB_PASSWORD=postgres

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=ecommerce
KEYCLOAK_CLIENT_ID=service-account
KEYCLOAK_CLIENT_SECRET=your-secret

SERVER_PORT=8082
LOGGING_LEVEL=INFO
```

#### Application Properties (Java/Spring Boot)
```properties
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

server:
  port: 8082

logging:
  level:
    com.ecommerce: DEBUG

keycloak:
  auth-server-url: http://localhost:8180
  realm: ecommerce
  resource: service-account
  credentials:
    secret: your-secret
```

### Running Multiple Services

#### Using Docker Compose for Services
```bash
# Start all services together
cd microservices-architecture
docker-compose -f docker-compose.services.yml up -d

# Check service status
docker-compose -f docker-compose.services.yml ps

# View logs
docker-compose -f docker-compose.services.yml logs -f user-service
```

#### Manual Service Startup
```bash
# Terminal 1: Start user service
cd user-service && mvn spring-boot:run

# Terminal 2: Start product service  
cd product-service && mvn spring-boot:run

# Terminal 3: Start cart service
cd cart-service && mvn spring-boot:run

# Terminal 4: Start order service
cd order-service && mvn spring-boot:run
```

## Development Workflow

### 1. Code Changes

#### Making Changes to a Service
```bash
# 1. Navigate to service directory
cd user-service

# 2. Make code changes
# 3. Build the service
mvn clean compile

# 4. Run tests
mvn test

# 5. Start the service
mvn spring-boot:run
```

#### Hot Reload (Development Mode)
```bash
# Java/Spring Boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# .NET
dotnet watch run

# Node.js
npm run dev

# Python
uvicorn main:app --reload
```

### 2. Testing

#### Unit Tests
```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

#### Integration Tests
```bash
# Run integration tests
mvn verify -Pintegration

# Run with test containers
mvn test -Dspring.profiles.active=test
```

#### API Testing
```bash
# Using curl
curl -X GET http://localhost:8082/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"

# Using httpie (recommended)
http GET http://localhost:8082/api/v1/users/me \
  Authorization:"Bearer $TOKEN"
```

### 3. Debugging

#### Java Debugging
```bash
# Start service in debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Connect from IDE on port 5005
```

#### .NET Debugging
```bash
# Start with debugger
dotnet run --launch-profile Development
```

#### Node.js Debugging
```bash
# Start with inspector
node --inspect=9229 main.js
```

#### Python Debugging
```bash
# Start with debugger
python -m debugpy --listen 5678 --wait-for-client main.py
```

### 4. Database Development

#### Database Migrations
```bash
# Generate migration (Java/Flyway)
mvn flyway:migrate

# Generate migration (.NET/EF Core)
dotnet ef migrations add AddUserProfile
dotnet ef database update

# Generate migration (Node.js/TypeORM)
npm run typeorm migration:generate -- -n AddUserProfile
npm run typeorm migration:run
```

#### Database Exploration
```bash
# Connect to database
docker-compose exec user-db psql -U postgres -d user_db

# Common commands
\dt                    # List tables
\d+ users              # Describe table
SELECT * FROM users;   # Query data
\q                     # Exit
```

### 5. Kafka Development

#### Kafka Topics
```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker-compose exec kafka kafka-topics --create \
  --topic user-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Describe topic
docker-compose exec kafka kafka-topics --describe \
  --topic user-events \
  --bootstrap-server localhost:9092
```

#### Produce/Consume Messages
```bash
# Produce message
docker-compose exec kafka kafka-console-producer \
  --topic user-events \
  --bootstrap-server localhost:9092

# Consume messages
docker-compose exec kafka kafka-console-consumer \
  --topic user-events \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

## Development Tools

### IDE Setup

#### IntelliJ IDEA
1. Install plugins:
   - Spring Boot Assistant
   - Lombok
   - Docker
   - Kubernetes
2. Configure run configurations
3. Enable annotation processing

#### VS Code
1. Install extensions:
   - Java Extension Pack
   - C# for Visual Studio Code
   - Python
   - Docker
   - Kubernetes
2. Configure launch.json for debugging

### API Testing Tools

#### Postman Collection
Import the Postman collection from `docs/postman/` directory.

#### Insomnia
Import the Insomnia workspace from `docs/insomnia/` directory.

#### curl Examples
```bash
# Get access token
TOKEN=$(curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web-client" \
  -d "grant_type=password" \
  -d "username=customer@ecommerce.com" \
  -d "password=customer123" | jq -r '.access_token')

# Use token in API calls
curl -X GET http://localhost:8082/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Monitoring Tools

#### Prometheus
- URL: http://localhost:9090
- Check targets: http://localhost:9090/targets
- Query metrics: http://localhost:9090/graph

#### Grafana
- URL: http://localhost:3000
- Credentials: admin/admin
- Import dashboards from `docs/grafana/`

#### Jaeger
- URL: http://localhost:16686
- Search traces by service name

#### Kibana
- URL: http://localhost:5601
- Create index pattern: `ecommerce-*`
- Time field: `@timestamp`

## Common Development Tasks

### Adding a New Service

1. **Create Service Structure**
```bash
# Use template
./scripts/create-service.sh user-profile-service

# Or manually
mkdir user-profile-service
cd user-profile-service
# Copy from template
```

2. **Configure Service**
   - Update application properties
   - Configure database connection
   - Set up Kafka integration
   - Configure Keycloak client

3. **Implement Business Logic**
   - Create entities
   - Implement repositories
   - Create services
   - Add controllers

4. **Add to Docker Compose**
```yaml
user-profile-service:
  build: ./user-profile-service
  ports:
    - "8090:8080"
  environment:
    - DB_HOST=user-profile-db
    - DB_PORT=5432
  depends_on:
    - user-profile-db
```

### Adding a New API Endpoint

1. **Define API Contract**
```java
@RestController
@RequestMapping("/api/v1/user-profiles")
public class UserProfileController {
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable UUID userId) {
        // Implementation
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfile> updateUserProfile(
            @PathVariable UUID userId,
            @RequestBody UpdateUserProfileRequest request) {
        // Implementation
    }
}
```

2. **Implement Service Layer**
```java
@Service
public class UserProfileService {
    
    public UserProfile getUserProfile(UUID userId) {
        // Business logic
    }
    
    public UserProfile updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        // Business logic
    }
}
```

3. **Add Tests**
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileControllerTest {
    
    @Test
    void getUserProfile_ReturnsUserProfile() {
        // Test implementation
    }
}
```

### Publishing Events

1. **Define Event Schema**
```java
public class UserProfileUpdatedEvent {
    private UUID userId;
    private String oldEmail;
    private String newEmail;
    private LocalDateTime timestamp;
    // Getters, setters
}
```

2. **Publish Event**
```java
@Service
public class UserProfileService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public UserProfile updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        // Update logic
        
        // Publish event
        UserProfileUpdatedEvent event = new UserProfileUpdatedEvent();
        event.setUserId(userId);
        event.setOldEmail(oldEmail);
        event.setNewEmail(newEmail);
        event.setTimestamp(LocalDateTime.now());
        
        kafkaTemplate.send("user-profile-events", event);
        
        return updatedProfile;
    }
}
```

3. **Consume Event**
```java
@Component
public class UserProfileEventListener {
    
    @KafkaListener(topics = "user-profile-events")
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event) {
        // Handle event
        log.info("User profile updated: {}", event.getUserId());
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Find process using port
lsof -i :8082

# Kill process
kill -9 <PID>

# Or change port in application.properties
server.port=8083
```

#### 2. Database Connection Failed
```bash
# Check if database is running
docker-compose ps | grep postgres

# Check database logs
docker-compose logs postgres

# Test connection
psql -h localhost -p 5432 -U postgres -d user_db
```

#### 3. Kafka Not Working
```bash
# Check Kafka status
docker-compose ps | grep kafka

# Check Kafka logs
docker-compose logs kafka

# Test Kafka connectivity
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### 4. Keycloak Authentication Failed
```bash
# Check Keycloak status
curl http://localhost:8180/health

# Check Keycloak logs
docker-compose logs keycloak

# Verify realm configuration
# Open http://localhost:8180 and check admin console
```

#### 5. Service Not Starting
```bash
# Check service logs
docker-compose logs user-service

# Check application properties
cat user-service/src/main/resources/application.yml

# Check dependencies
mvn dependency:tree
```

### Debugging Tips

#### Enable Debug Logging
```properties
# application-dev.yml
logging:
  level:
    com.ecommerce: DEBUG
    org.springframework: DEBUG
    org.hibernate: DEBUG
```

#### Use Actuator Endpoints
```bash
# Health check
curl http://localhost:8082/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics

# Environment
curl http://localhost:8082/actuator/env

# Loggers
curl http://localhost:8082/actuator/loggers
```

#### Check Docker Resources
```bash
# Check resource usage
docker stats

# Check container logs
docker logs <container_id>

# Inspect container
docker inspect <container_id>
```

## Performance Tips

### Development Performance

#### Use Development Profile
```bash
# Java/Spring Boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# .NET
dotnet run --environment Development

# Node.js
NODE_ENV=development npm start

# Python
FASTAPI_ENV=development python main.py
```

#### Enable Hot Reload
```bash
# Java/Spring Boot DevTools
# Add spring-boot-devtools dependency

# .NET watch
dotnet watch run

# Node.js nodemon
npm run dev

# Python uvicorn reload
uvicorn main:app --reload
```

#### Optimize Build Times
```bash
# Skip tests during development
mvn spring-boot:run -DskipTests

# Use incremental compilation
mvn compile -DskipTests

# Use build cache
mvn clean compile -Dmaven.test.skip=true
```

### Database Performance

#### Use Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

#### Enable Query Caching
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.ehcache.EhCacheRegionFactory
```

#### Use Read Replicas for Development
```yaml
# docker-compose.yml
user-db-readonly:
  image: postgres:15
  environment:
    POSTGRES_DB: user_db
    POSTGRES_USER: readonly
    POSTGRES_PASSWORD: readonly
  command: postgres -c hot_standby=on
```

## Best Practices

### Code Quality

1. **Follow Style Guide**
   - Java: Google Java Style Guide
   - .NET: Microsoft .NET Coding Conventions
   - Node.js: Airbnb JavaScript Style Guide
   - Python: PEP 8

2. **Write Tests**
   - Unit tests for all business logic
   - Integration tests for API endpoints
   - End-to-end tests for critical workflows

3. **Code Reviews**
   - Review all changes before merging
   - Use pull requests with templates
   - Enforce code quality gates

### Development Workflow

1. **Use Feature Branches**
```bash
git checkout -b feature/user-profile-management
# Make changes
git add .
git commit -m "Add user profile management"
git push origin feature/user-profile-management
```

2. **Commit Convention**
```
feat: add user profile management
fix: resolve null pointer in user service
docs: update API documentation
test: add unit tests for user service
chore: update dependencies
```

3. **Continuous Integration**
   - Run tests on every commit
   - Build Docker images
   - Run security scans
   - Generate documentation

### Documentation

1. **Keep Documentation Updated**
   - Update README.md for each service
   - Document API changes
   - Update architecture diagrams
   - Maintain troubleshooting guide

2. **API Documentation**
   - Use OpenAPI/Swagger
   - Generate API documentation automatically
   - Include examples and error responses

3. **Architecture Documentation**
   - Update C4 model diagrams
   - Document design decisions (ADRs)
   - Maintain deployment guides

## Next Steps

1. **Explore the Codebase**
   - Review service implementations
   - Study architecture patterns
   - Examine test coverage

2. **Run the Complete System**
   - Start all services
   - Test end-to-end workflows
   - Monitor system performance

3. **Make Your First Change**
   - Add a new feature
   - Fix a bug
   - Improve documentation

4. **Learn Advanced Topics**
   - Distributed transactions (Saga pattern)
   - Event sourcing
   - CQRS pattern
   - Service mesh

## Getting Help

### Resources
- [Architecture Documentation](../architecture/)
- [API Documentation](../guides/api-documentation.md)
- [Troubleshooting Guide](../guides/troubleshooting.md)

### Support Channels
- GitHub Issues for bug reports
- Discussion forum for questions
- Slack channel for real-time help

### Common Questions
- Check the [FAQ](../guides/faq.md)
- Search existing issues
- Ask in discussion forum

---

**Ready to start developing?** Choose a service and language, and begin exploring the codebase!