# Getting Started Guide

Welcome to the Software Architecture Course! This guide will help you set up your development environment and run the ecommerce platform locally.

## Prerequisites

Before you begin, ensure you have the following installed:

### Required
- **Docker**: [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose**: [Install Docker Compose](https://docs.docker.com/compose/install/)
- **Git**: [Install Git](https://git-scm.com/downloads)

### Optional (for specific language implementations)
- **Java 17+**: [Install JDK](https://www.oracle.com/java/technologies/downloads/)
- **.NET 6+**: [Install .NET SDK](https://dotnet.microsoft.com/download)
- **Node.js 18+**: [Install Node.js](https://nodejs.org/)
- **Python 3.9+**: [Install Python](https://www.python.org/downloads/)

## Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/software-architecture-course.git
cd software-architecture-course
```

## Step 2: Start the Infrastructure Stack

The Docker Compose stack includes all required services (databases, Kafka, Keycloak, monitoring, etc.).

```bash
cd shared
docker-compose up -d
```

This will start:
- 7 PostgreSQL databases
- Redis cache
- Kafka cluster (3 brokers)
- Keycloak authentication server
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Prometheus + Grafana
- Jaeger distributed tracing

### Verify Services are Running

```bash
docker-compose ps
```

You should see all services with status "Up".

### Wait for Services to be Ready

Some services take time to initialize. Check health:

```bash
# Check Keycloak
curl http://localhost:8180/health

# Check Elasticsearch
curl http://localhost:9200/_cluster/health

# Check Prometheus
curl http://localhost:9090/-/healthy
```

## Step 3: Access the Services

Once all services are running, you can access them:

| Service | URL | Credentials |
|---------|-----|-------------|
| Keycloak Admin | http://localhost:8180 | admin / admin |
| Kibana | http://localhost:5601 | - |
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger | http://localhost:16686 | - |
| Prometheus | http://localhost:9090 | - |

## Step 4: Configure Keycloak

1. Navigate to http://localhost:8180
2. Click "Administration Console"
3. Login with admin/admin
4. Follow the [Keycloak Configuration Guide](../keycloak-config/README.md)

## Step 5: Initialize Databases

The database schemas are automatically initialized when Docker Compose starts. To verify:

```bash
# Connect to user database
psql -h localhost -p 5432 -U postgres -d user_db

# List tables
\dt

# Exit
\q
```

## Step 6: Choose Your Architecture & Language

The course provides implementations in three architectures and four languages:

### Microservices Architecture (Recommended for Learning)

```bash
# Switch to microservices branch
git checkout microservices-architecture

# Choose your language:
# Java
cd java-microservices/user-service
mvn spring-boot:run

# .NET
cd dotnet-microservices/user-service
dotnet run

# Node.js
cd nodejs-microservices/user-service
npm install
npm start

# Python
cd python-microservices/user-service
pip install -r requirements.txt
python main.py
```

### SOA Architecture

```bash
git checkout soa-architecture
# Follow same language selection as above
```

### MVC Architecture

```bash
git checkout mvc-architecture
# Follow same language selection as above
```

## Step 7: Run Your First Service

Let's start with the User Service in Java:

```bash
# Navigate to the service
cd microservices-architecture/java-microservices/user-service

# Build the project
mvn clean package

# Run the service
mvn spring-boot:run
```

The service should start on http://localhost:8082

### Verify the Service is Running

```bash
curl http://localhost:8082/actuator/health
```

You should see:
```json
{
  "status": "UP"
}
```

## Step 8: Test the API

Get an access token from Keycloak:

```bash
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web-client" \
  -d "grant_type=password" \
  -d "username=customer@ecommerce.com" \
  -d "password=customer123"
```

Use the token to call the API:

```bash
curl -X GET http://localhost:8082/api/v1/users/me \
  -H "Authorization: Bearer <access_token>"
```

## Step 9: View Logs

### Using Kibana

1. Navigate to http://localhost:5601
2. Create an index pattern: `logs-*`
3. View logs in the Discover tab

### Using Docker Logs

```bash
# View logs for a specific service
docker logs user-service

# Follow logs in real-time
docker logs -f user-service
```

## Step 10: Monitor the System

### Grafana Dashboards

1. Navigate to http://localhost:3000
2. Login with admin/admin
3. Add Prometheus as data source
4. Create dashboards to monitor:
   - Request rates
   - Response times
   - Error rates
   - Service health

### Jaeger Tracing

1. Navigate to http://localhost:16686
2. Select a service from the dropdown
3. View distributed traces
4. Analyze request flow across services

## Troubleshooting

### Services Won't Start

**Problem**: Docker containers fail to start

**Solution**:
```bash
# Check Docker logs
docker logs <container_name>

# Restart services
docker-compose restart

# Full reset (WARNING: deletes data)
docker-compose down -v
docker-compose up -d
```

### Port Already in Use

**Problem**: Port 5432 (or another port) is already in use

**Solution**:
```bash
# Find process using port
lsof -i :5432

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

### Database Connection Failed

**Problem**: Cannot connect to PostgreSQL

**Solution**:
```bash
# Verify database is running
docker ps | grep postgres

# Check database logs
docker logs user-db

# Verify credentials
# Default: postgres / postgres
```

### Keycloak Not Responding

**Problem**: Keycloak admin console not accessible

**Solution**:
```bash
# Check Keycloak logs
docker logs keycloak

# Wait for database to be ready
docker logs user-db

# Restart Keycloak
docker-compose restart keycloak
```

## Next Steps

1. **Explore the Architecture**: Read the [Architecture Documentation](../architecture/)
2. **Review ADRs**: Check the [Architecture Decision Records](../adrs/)
3. **Run Tests**: Execute the test suite for your chosen service
4. **Deploy Locally**: Follow the [Local Development Guide](./local-development.md)
5. **Learn the Patterns**: Study the [Design Patterns Guide](./design-patterns.md)

## Common Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View running services
docker-compose ps

# View logs
docker-compose logs -f <service_name>

# Execute command in container
docker-compose exec <service_name> <command>

# Rebuild images
docker-compose build

# Remove volumes (WARNING: deletes data)
docker-compose down -v
```

## Resources

- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [ASP.NET Core Documentation](https://docs.microsoft.com/en-us/aspnet/core/)
- [Express.js Documentation](https://expressjs.com/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

## Support

If you encounter issues:

1. Check the [Troubleshooting Guide](./troubleshooting.md)
2. Review the [Architecture Documentation](../architecture/)
3. Check service logs: `docker logs <service_name>`
4. Open an issue on GitHub

---

**Ready to start?** Run `docker-compose up -d` in the `shared` directory and begin exploring!
