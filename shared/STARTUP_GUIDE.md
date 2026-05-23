# Ecommerce Microservices Platform - Startup Guide

## Overview

This guide provides comprehensive instructions for setting up and running the ecommerce microservices platform for local development. The platform consists of 10 microservices, 7 databases, and supporting infrastructure services.

## System Requirements

### Minimum Hardware Requirements
- **CPU**: 4 cores (8 threads recommended)
- **RAM**: 16 GB (8 GB minimum, 32 GB recommended for full stack)
- **Disk Space**: 20 GB free space
- **Operating System**: Windows 10/11, macOS 10.15+, or Linux (Ubuntu 20.04+)

### Software Requirements
- **Docker Desktop**: 4.0+ (with WSL2 on Windows)
- **Docker Compose**: 2.0+
- **Git**: 2.30+
- **Terminal**: PowerShell 7+ (Windows), Terminal (macOS/Linux)
- **Memory Allocation for Docker**: Minimum 8 GB RAM, 4 CPU cores

### Development Tools (Optional)
- **IDE**: VS Code, IntelliJ IDEA, or Visual Studio
- **Database Client**: DBeaver, pgAdmin, or Adminer (included)
- **API Testing**: Postman, Insomnia, or curl
- **Kafka Tools**: Kafka UI (included), kcat

### Resource Requirements by Stack Profile

The development override (`docker-compose.override.yml`) reduces resource consumption significantly compared to production settings. Below is the estimated resource usage per profile:

| Profile | Services | RAM Usage | CPU Cores |
|---------|----------|-----------|-----------|
| **Infrastructure Only** | 7 DBs + Redis + Kafka + Zookeeper + Keycloak | ~4 GB | 3.5 |
| **Infrastructure + Monitoring** | Above + ELK + Prometheus + Grafana + Jaeger | ~6 GB | 5.0 |
| **Full Stack (Development)** | All services including microservices | ~10 GB | 8.0 |
| **Minimal (Core Only)** | 2 DBs + Redis + Kafka + Zookeeper | ~2 GB | 1.5 |

#### Per-Service Resource Limits (Development Override)

| Service Category | Memory Limit | CPU Limit | Instances |
|-----------------|-------------|-----------|-----------|
| PostgreSQL (each) | 256 MB | 0.5 | 7 |
| Redis | 256 MB | 0.3 | 1 |
| Kafka Broker | 512 MB | 0.5 | 1 (brokers 2-3 disabled) |
| Zookeeper | 384 MB | 0.3 | 1 |
| Keycloak | 512 MB | 0.5 | 1 |
| Elasticsearch | 512 MB | 0.5 | 1 |
| Logstash | 256 MB | 0.3 | 1 |
| Kibana | 256 MB | 0.3 | 1 |
| Prometheus | 256 MB | 0.3 | 1 |
| Grafana | 256 MB | 0.3 | 1 |
| Jaeger | 256 MB | 0.3 | 1 |
| API Gateway (Kong) | 256 MB | 0.3 | 1 |
| Microservices (each) | 512 MB | 0.5 | 1 |
| Kafka UI | 256 MB | 0.2 | 1 |
| Adminer | 64 MB | 0.1 | 1 |

## Startup Procedure

### 1. Prerequisites Setup

#### Windows
```powershell
# Enable WSL2 (if not already enabled)
wsl --install

# Install Docker Desktop from https://www.docker.com/products/docker-desktop/
# Configure Docker Desktop:
# - Allocate 8 GB RAM
# - Allocate 4 CPU cores
# - Enable WSL2 integration

# Verify installation
docker --version
docker-compose --version
```

#### macOS/Linux
```bash
# Install Docker Desktop (macOS) or Docker Engine (Linux)
# Verify installation
docker --version
docker-compose --version
```

### 2. Clone and Navigate to Project

```bash
# Clone the repository
git clone <repository-url>
cd software-architecture-course

# Navigate to shared directory
cd shared
```

### 3. Start the Development Stack

#### Full Stack Startup (All Services)
```bash
# Start all services with development configuration
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

# Monitor startup progress
docker-compose logs -f
```

#### Minimal Stack Startup (Core Services Only)
```bash
# Start only essential services for development
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d \
  user-db product-db order-db \
  redis kafka-broker-1 zookeeper \
  keycloak

# Start additional services as needed
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d \
  payment-db inventory-db cart-db analytics-db \
  elasticsearch logstash kibana \
  prometheus grafana jaeger
```

#### Profile-based Startup
```bash
# Start with specific profiles
docker-compose --profile monitoring up -d  # Monitoring services only
docker-compose --profile databases up -d   # Databases only
docker-compose --profile kafka up -d       # Kafka only
```

### 4. Verify Service Health

#### Check All Services Status
```bash
# List all running containers
docker-compose ps

# Check service health
docker-compose ps --services | ForEach-Object {
  $service = $_
  $status = docker-compose ps --services --filter "status=running" | Select-String $service
  if ($status) {
    Write-Host "✓ $service is running" -ForegroundColor Green
  } else {
    Write-Host "✗ $service is not running" -ForegroundColor Red
  }
}
```

#### Individual Service Health Checks
```bash
# Check PostgreSQL databases
docker-compose exec user-db pg_isready -U dev_user

# Check Redis
docker-compose exec redis redis-cli ping

# Check Kafka
docker-compose exec kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092

# Check Keycloak
curl -f http://localhost:8180/health/ready

# Check Elasticsearch
curl -f http://localhost:9200/_cluster/health

# Check Prometheus
curl -f http://localhost:9090/-/healthy

# Check Grafana
curl -f http://localhost:3000/api/health

# Check Jaeger
curl -f http://localhost:16686/api/services
```

### 5. Access Services

#### Web Interfaces
| Service | URL | Default Credentials |
|---------|-----|-------------------|
| **Keycloak Admin Console** | http://localhost:8180 | admin/admin |
| **Kibana** | http://localhost:5601 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **Jaeger UI** | http://localhost:16686 | - |
| **Kafka UI** | http://localhost:8090 | - |
| **Adminer (Database UI)** | http://localhost:8091 | - |
| **Kong Admin API** | http://localhost:8001 | - |

#### API Endpoints
| Service | Port | Description |
|---------|------|-------------|
| **Keycloak** | 8180 | OAuth2/OIDC endpoints |
| **Elasticsearch** | 9200 | Search and analytics |
| **Prometheus** | 9090 | Metrics collection |
| **Jaeger** | 16686 | Tracing API |
| **Kafka Broker** | 9092 | Message broker |

#### Database Connections
| Database | Port | Database | Username | Password |
|----------|------|----------|----------|----------|
| **User DB** | 5432 | user_db | dev_user | dev_password |
| **Product DB** | 5433 | product_db | dev_user | dev_password |
| **Order DB** | 5434 | order_db | dev_user | dev_password |
| **Payment DB** | 5435 | payment_db | dev_user | dev_password |
| **Inventory DB** | 5436 | inventory_db | dev_user | dev_password |
| **Cart DB** | 5437 | cart_db | dev_user | dev_password |
| **Analytics DB** | 5438 | analytics_db | dev_user | dev_password |
| **Redis** | 6379 | - | - | - |

## Development Override Configuration

The `docker-compose.override.yml` file is automatically applied when running `docker-compose up` and provides the following development-specific features:

### Key Development Features

1. **Reduced Resource Limits**: All services run with lower memory and CPU limits suitable for development machines.
2. **Single Kafka Broker**: Brokers 2 and 3 are scaled to 0 replicas to save resources while remaining defined for dependency resolution.
3. **DEBUG Logging**: All microservices and infrastructure services are configured with DEBUG-level logging.
4. **Hot-Reload Volume Mounts**: Source code directories are mounted into containers for live code changes.
5. **Debug Ports Exposed**: Each Java microservice exposes a JDWP debug port for remote debugging from your IDE.
6. **Development Tools**: Kafka UI and Adminer are included for visual management of Kafka and databases.

### Debug Port Mapping

Connect your IDE debugger to these ports for remote debugging:

| Service | Application Port | Debug Port | Protocol |
|---------|-----------------|------------|----------|
| User Service | 8081 | 5005 | JDWP |
| Product Service | 8082 | 5006 | JDWP |
| Cart Service | 8083 | 5007 | JDWP |
| Payment Service | 8084 | 5009 | JDWP |
| Order Service | 8085 | 5008 | JDWP |
| Inventory Service | 8086 | 5010 | JDWP |
| Notification Service | 8087 | 5011 | JDWP |
| Analytics Service | 8088 | 5012 | JDWP |

#### IntelliJ IDEA Remote Debug Configuration
1. Go to **Run → Edit Configurations → + → Remote JVM Debug**
2. Set **Host**: `localhost`
3. Set **Port**: corresponding debug port from table above
4. Set **Use module classpath**: select the service module
5. Click **Debug** to attach

#### VS Code Remote Debug Configuration
Add to `.vscode/launch.json`:
```json
{
  "type": "java",
  "name": "Debug User Service",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

### Volume Mounts for Hot-Reload

Source code is mounted read-only into containers for services that support hot-reload:

```
../services/java-microservices/<service>/src  → /app/src (read-only)
../services/java-microservices/<service>/target/classes → /app/classes
```

For Spring Boot with DevTools, changes to classes trigger automatic restart inside the container.

### Overriding the Override

To run with production-like settings (e.g., 3 Kafka brokers), use explicit file specification:

```bash
# Production-like configuration (ignores override)
docker-compose -f docker-compose.yml up -d

# Include production profile services
docker-compose -f docker-compose.yml -f docker-compose.override.yml --profile production up -d
```

## Service Health Check Verification

### Automated Health Check Script
Create a health check script `check-health.ps1` (Windows) or `check-health.sh` (Linux/macOS):

```powershell
# check-health.ps1
$services = @(
    @{Name="user-db"; Check="docker-compose exec user-db pg_isready -U dev_user"},
    @{Name="redis"; Check="docker-compose exec redis redis-cli ping"},
    @{Name="kafka-broker-1"; Check="docker-compose exec kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092"},
    @{Name="keycloak"; Check="curl -f http://localhost:8180/health/ready"},
    @{Name="elasticsearch"; Check="curl -f http://localhost:9200/_cluster/health"},
    @{Name="prometheus"; Check="curl -f http://localhost:9090/-/healthy"},
    @{Name="grafana"; Check="curl -f http://localhost:3000/api/health"}
)

Write-Host "`n=== Service Health Check ===" -ForegroundColor Cyan

foreach ($service in $services) {
    try {
        Invoke-Expression $service.Check 2>&1 | Out-Null
        Write-Host "✓ $($service.Name) is healthy" -ForegroundColor Green
    } catch {
        Write-Host "✗ $($service.Name) is unhealthy" -ForegroundColor Red
    }
}
```

### Manual Verification Steps

#### 1. Database Connectivity
```bash
# Connect to PostgreSQL databases
docker-compose exec user-db psql -U dev_user -d user_db -c "SELECT version();"
docker-compose exec product-db psql -U dev_user -d product_db -c "\dt"
```

#### 2. Kafka Functionality
```bash
# Create test topic
docker-compose exec kafka-broker-1 kafka-topics --create \
  --topic test-topic \
  --partitions 1 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092

# List topics
docker-compose exec kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092

# Produce test message
echo '{"message": "test"}' | docker-compose exec -T kafka-broker-1 kafka-console-producer \
  --topic test-topic \
  --bootstrap-server localhost:9092

# Consume test message
docker-compose exec kafka-broker-1 kafka-console-consumer \
  --topic test-topic \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --timeout-ms 5000
```

#### 3. Keycloak Setup
```bash
# Verify Keycloak realm
curl -H "Authorization: Bearer $(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" | jq -r '.access_token')" \
  http://localhost:8180/admin/realms/ecommerce
```

#### 4. Monitoring Stack
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.health == "up") | .labels.job'

# Check Grafana datasources
curl -u admin:admin http://localhost:3000/api/datasources
```

## Troubleshooting Common Issues

### 1. Docker Resource Issues

#### Insufficient Memory
**Symptoms**: Containers fail to start, OOM errors
**Solution**:
```bash
# Check Docker resource allocation
docker stats --no-stream

# Increase Docker Desktop memory allocation
# Windows/macOS: Docker Desktop Settings → Resources → Memory
# Linux: Edit /etc/docker/daemon.json
{
  "memory": "8g",
  "cpus": 4
}

# Restart Docker
docker-compose down
docker system prune -a
docker-compose up -d
```

#### Port Conflicts
**Symptoms**: "Port already in use" errors
**Solution**:
```bash
# Find process using port
netstat -ano | findstr :5432  # Windows
lsof -i :5432                 # macOS/Linux

# Kill conflicting process or change port in docker-compose.override.yml
# Example: Change PostgreSQL port mapping
# ports:
#   - "5433:5432"  # Instead of 5432:5432
```

### 2. Service Startup Failures

#### Database Initialization Issues
**Symptoms**: PostgreSQL containers restarting, connection refused
**Solution**:
```bash
# Check database logs
docker-compose logs user-db

# Reset database volumes
docker-compose down -v
docker-compose up -d user-db

# Wait for database initialization
sleep 30
docker-compose up -d
```

#### Kafka/Zookeeper Issues
**Symptoms**: Kafka brokers not starting, connection to Zookeeper failed
**Solution**:
```bash
# Ensure Zookeeper starts first
docker-compose up -d zookeeper
sleep 30
docker-compose up -d kafka-broker-1

# Check Zookeeper status
docker-compose exec zookeeper zkServer.sh status

# Reset Kafka data
docker-compose down -v
docker volume rm $(docker volume ls -q | grep kafka)
docker-compose up -d
```

### 3. Network Connectivity Issues

#### Container-to-Container Communication
**Symptoms**: Services can't connect to databases or other services
**Solution**:
```bash
# Check network configuration
docker network ls
docker network inspect ecommerce-dev-network

# Test connectivity between containers
docker-compose exec user-db ping product-db
docker-compose exec order-service curl http://user-service:8082/health

# Recreate network
docker-compose down
docker network rm ecommerce-dev-network
docker-compose up -d
```

#### Host-to-Container Communication
**Symptoms**: Can't access services from host machine
**Solution**:
```bash
# Check port mappings
docker-compose ps
docker port keycloak 8080

# Verify firewall settings
# Windows: Check Windows Defender Firewall
# macOS: Check System Preferences → Security & Privacy → Firewall
# Linux: Check iptables or ufw

# Test connectivity
curl -v http://localhost:8180
telnet localhost 5432
```

### 4. Performance Issues

#### Slow Startup Times
**Solution**:
```bash
# Use cached images
docker-compose pull

# Start services in sequence
docker-compose up -d databases
sleep 60
docker-compose up -d kafka keycloak
sleep 30
docker-compose up -d monitoring

# Disable non-essential services
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d \
  --scale kafka-broker-2=0 \
  --scale kafka-broker-3=0
```

#### High Memory Usage
**Solution**:
```bash
# Monitor resource usage
docker stats

# Reduce resource limits in docker-compose.override.yml
# Example: Reduce Elasticsearch memory
# environment:
#   - "ES_JAVA_OPTS=-Xms128m -Xmx128m"

# Restart with reduced resources
docker-compose down
docker-compose up -d
```

### 5. Data Persistence Issues

#### Volume Mount Problems
**Symptoms**: Data not persisting between container restarts
**Solution**:
```bash
# Check volume mounts
docker volume ls
docker volume inspect shared_user-db-data

# Verify volume permissions
docker-compose exec user-db ls -la /var/lib/postgresql/data

# Recreate volumes with proper permissions
docker-compose down -v
docker volume create --name=shared_user-db-data
docker-compose up -d
```

#### Database Corruption
**Symptoms**: Database errors, corrupted tables
**Solution**:
```bash
# Backup data before reset
docker-compose exec user-db pg_dump -U dev_user user_db > user_db_backup.sql

# Reset database
docker-compose down -v
docker-compose up -d user-db
sleep 30

# Restore data
docker-compose exec -T user-db psql -U dev_user user_db < user_db_backup.sql
```

## Development Workflow

### 1. Local Development Setup

#### Environment Configuration
```bash
# Copy environment template
cp .env.example .env

# Edit environment variables
# Set development-specific values
DATABASE_URL=postgresql://dev_user:dev_password@localhost:5432/user_db
KAFKA_BROKERS=localhost:9092
KEYCLOAK_URL=http://localhost:8180
```

#### Service Development
```bash
# Start infrastructure only
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d \
  --scale user-service=0 \
  --scale product-service=0 \
  --scale order-service=0

# Run microservices locally with hot reload
# Java: ./mvnw spring-boot:run
# .NET: dotnet watch run
# Node: npm run dev
# Python: uvicorn main:app --reload
```

### 2. Testing Workflow

#### Unit Tests
```bash
# Run tests for specific service
cd ../user-service
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

#### Integration Tests
```bash
# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run integration tests
./mvnw verify -P integration-test

# Cleanup
docker-compose -f docker-compose.test.yml down -v
```

#### End-to-End Tests
```bash
# Start full stack
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

# Run E2E tests
npm run test:e2e

# Generate test report
open test-results/report.html
```

### 3. Debugging Workflow

#### Log Analysis
```bash
# View logs for specific service
docker-compose logs -f user-service

# Filter logs by level
docker-compose logs user-service | grep ERROR

# View structured logs in Kibana
# Access http://localhost:5601
# Create index pattern: logstash-*
```

#### Distributed Tracing
```bash
# View traces in Jaeger UI
# Access http://localhost:16686
# Search for traces by service or operation

# Add custom trace spans
# Java: @NewSpan, @ContinueSpan
# .NET: ActivitySource
# Node: OpenTelemetry
```

#### Metrics Monitoring
```bash
# View metrics in Prometheus
# Access http://localhost:9090
# Query: rate(http_requests_total[5m])

# View dashboards in Grafana
# Access http://localhost:3000
# Default dashboards: Spring Boot, JVM, PostgreSQL
```

### 4. Database Management

#### Schema Migrations
```bash
# Apply migrations
docker-compose exec user-db psql -U dev_user -d user_db -f /docker-entrypoint-initdb.d/init.sql

# Generate migration script
# Java: Liquibase diff
# .NET: Entity Framework migrations
# Node: Sequelize migrations
```

#### Data Seeding
```bash
# Seed development data
docker-compose exec user-db psql -U dev_user -d user_db -f /docker-entrypoint-initdb.d/seed.sql

# Export/import data
docker-compose exec user-db pg_dump -U dev_user user_db > user_db_dump.sql
docker-compose exec -T user-db psql -U dev_user user_db < user_db_dump.sql
```

### 5. Deployment Workflow

#### Build Images
```bash
# Build service images
docker-compose build user-service

# Push to registry
docker tag user-service:latest myregistry/user-service:dev
docker push myregistry/user-service:dev
```

#### Update Stack
```bash
# Pull latest images
docker-compose pull

# Update services
docker-compose up -d --no-deps --build user-service

# Rollback if needed
docker-compose up -d --no-deps user-service:previous-version
```

## Maintenance Tasks

### Regular Maintenance

#### Weekly Tasks
```bash
# Clean up unused resources
docker system prune -a --volumes

# Update images
docker-compose pull

# Backup databases
./scripts/backup-databases.sh

# Check disk usage
docker system df
```

#### Monthly Tasks
```bash
# Update Docker Compose configuration
# Review and update docker-compose.override.yml

# Update service dependencies
# Check for security updates

# Performance review
# Analyze logs and metrics for optimization opportunities
```

### Emergency Procedures

#### Service Outage
```bash
# Identify affected service
docker-compose ps --services --filter "status=exited"

# Check logs
docker-compose logs --tail=100 <service-name>

# Restart service
docker-compose restart <service-name>

# If restart fails, recreate
docker-compose up -d --force-recreate <service-name>
```

#### Data Recovery
```bash
# Stop affected services
docker-compose stop <service-name>

# Restore from backup
docker-compose exec -T <db-service> psql -U <user> <database> < backup.sql

# Verify data integrity
docker-compose exec <db-service> psql -U <user> <database> -c "SELECT COUNT(*) FROM <table>"

# Restart services
docker-compose start <service-name>
```

## Performance Optimization

### Resource Tuning

#### Memory Optimization
```yaml
# In docker-compose.override.yml
services:
  elasticsearch:
    environment:
      - "ES_JAVA_OPTS=-Xms256m -Xmx256m"  # Reduced from 512m
    
  keycloak:
    deploy:
      resources:
        limits:
          memory: 384M  # Reduced from 512M
```

#### CPU Optimization
```yaml
services:
  kafka-broker-1:
    deploy:
      resources:
        limits:
          cpus: '0.3'  # Reduced from 0.5
```

### Startup Optimization

#### Sequential Startup
```bash
# Start services in optimal order
./scripts/start-optimized.sh
```

#### Lazy Loading
```bash
# Disable non-essential services
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d \
  --scale grafana=0 \
  --scale jaeger=0 \
  --scale kafka-ui=0
```

## Appendix

### Useful Commands

#### Docker Management
```bash
# List containers
docker ps -a

# View logs
docker-compose logs -f --tail=50

# Execute commands
docker-compose exec <service> <command>

# Clean up
docker-compose down -v --remove-orphans

# View resource usage
docker stats
```

#### Database Operations
```bash
# Connect to database
docker-compose exec user-db psql -U dev_user -d user_db

# Backup all databases
./scripts/backup-all-databases.sh

# Restore database
docker-compose exec -T user-db psql -U dev_user user_db < backup.sql
```

#### Kafka Operations
```bash
# List topics
docker-compose exec kafka-broker-1 kafka-topics --list --bootstrap-server localhost:9092

# Describe topic
docker-compose exec kafka-broker-1 kafka-topics --describe --topic order.created --bootstrap-server localhost:9092

# Consumer groups
docker-compose exec kafka-broker-1 kafka-consumer-groups --list --bootstrap-server localhost:9092
```

### Configuration Reference

#### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_USER` | dev_user | Database username |
| `POSTGRES_PASSWORD` | dev_password | Database password |
| `KAFKA_BROKERS` | localhost:9092 | Kafka bootstrap servers |
| `KEYCLOAK_URL` | http://localhost:8180 | Keycloak server URL |
| `ELASTICSEARCH_URL` | http://localhost:9200 | Elasticsearch URL |
| `JAEGER_URL` | http://localhost:16686 | Jaeger UI URL |

#### Port Reference
| Service | Internal Port | External Port |
|---------|---------------|---------------|
| PostgreSQL | 5432 | 5432-5438 |
| Redis | 6379 | 6379 |
| Kafka | 9092 | 9092 |
| Keycloak | 8080 | 8180 |
| Elasticsearch | 9200 | 9200 |
| Kibana | 5601 | 5601 |
| Prometheus | 9090 | 9090 |
| Grafana | 3000 | 3000 |
| Jaeger | 16686 | 16686 |
| Kafka UI | 8080 | 8080 |
| Adminer | 8080 | 8081 |

### Troubleshooting Checklist

- [ ] Docker Desktop is running
- [ ] Sufficient memory allocated (8 GB+)
- [ ] Ports are not conflicting
- [ ] WSL2 is enabled (Windows)
- [ ] Docker Compose version is 2.0+
- [ ] .env file is configured (if required)
- [ ] Volume permissions are correct
- [ ] Network connectivity between containers
- [ ] Services are starting in correct order
- [ ] Health checks are passing

### Support Resources

- **Docker Documentation**: https://docs.docker.com/
- **Docker Compose Reference**: https://docs.docker.com/compose/compose-file/
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/
- **Kafka Documentation**: https://kafka.apache.org/documentation/
- **Keycloak Documentation**: https://www.keycloak.org/documentation
- **ELK Stack Documentation**: https://www.elastic.co/guide/index.html
- **Prometheus Documentation**: https://prometheus.io/docs/
- **Grafana Documentation**: https://grafana.com/docs/
- **Jaeger Documentation**: https://www.jaegertracing.io/docs/

## Conclusion

This startup guide provides comprehensive instructions for setting up and running the ecommerce microservices platform. Follow the steps sequentially, and refer to the troubleshooting section for common issues. For additional support, consult the documentation links provided or contact the development team.

Remember to regularly update Docker images and perform maintenance tasks to ensure optimal performance and security.