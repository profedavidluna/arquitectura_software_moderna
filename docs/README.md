# Documentation

Welcome to the Software Architecture Course documentation! This directory contains comprehensive documentation for the ecommerce platform project, designed for educational purposes to teach modern software architecture patterns and practices.

## Directory Structure

```
docs/
├── README.md                          # This file
├── architecture/                      # Architecture documentation
│   ├── README.md                      # Architecture overview
│   ├── c4-model.md                    # C4 model documentation
│   └── microservices-architecture.md  # Microservices architecture details
├── adrs/                              # Architecture Decision Records
│   ├── README.md                      # ADRs overview and template
│   ├── adr-001-microservices-architecture.md
│   ├── adr-002-database-per-service.md
│   └── [more ADRs...]
├── diagrams/                          # Architecture diagrams
│   ├── README.md                      # Diagrams overview
│   ├── architecture-overview.md       # Diagram types and conventions
│   └── [diagram files...]
└── guides/                            # Guides and tutorials
    ├── README.md                      # Guides overview
    ├── getting-started.md             # Quick start guide
    ├── local-development.md           # Local development setup
    ├── monitoring.md                  # Monitoring stack documentation
    └── [more guides...]
```

## Quick Start

### For Students
1. **Start Here**: Read the [Getting Started Guide](./guides/getting-started.md)
2. **Understand Architecture**: Review the [C4 Model Documentation](./architecture/c4-model.md)
3. **Set Up Development**: Follow the [Local Development Guide](./guides/local-development.md)
4. **Explore Decisions**: Read the [Architecture Decision Records](./adrs/)

### For Developers
1. **Clone Repository**: `git clone https://github.com/yourusername/software-architecture-course.git`
2. **Start Infrastructure**: `cd shared && docker-compose up -d`
3. **Choose Service**: Navigate to your preferred language implementation
4. **Run Service**: Follow language-specific instructions

### For Instructors
1. **Review Architecture**: Examine the [Architecture Documentation](./architecture/)
2. **Check ADRs**: Review [Architecture Decision Records](./adrs/)
3. **Prepare Labs**: Use the [Guides](./guides/) for lab exercises
4. **Monitor Progress**: Use the [Monitoring Documentation](./guides/monitoring.md)

## Documentation Overview

### 1. Architecture Documentation
The architecture documentation provides a comprehensive view of the system design using the C4 model:

- **System Context**: High-level overview and external dependencies
- **Container Diagram**: Technology choices and service boundaries
- **Component Diagram**: Internal structure of each service
- **Code Diagram**: Implementation details and patterns

**Key Files**:
- [C4 Model Documentation](./architecture/c4-model.md)
- [Microservices Architecture](./architecture/microservices-architecture.md)

### 2. Architecture Decision Records (ADRs)
ADRs document important architectural decisions, their context, and consequences:

- **Decision Process**: Why certain choices were made
- **Alternatives Considered**: Other options evaluated
- **Consequences**: Positive and negative impacts
- **Status Tracking**: Proposed, Accepted, Deprecated, Superseded

**Key Files**:
- [ADR-001: Microservices Architecture](./adrs/adr-001-microservices-architecture.md)
- [ADR-002: Database Per Service Pattern](./adrs/adr-002-database-per-service.md)
- [ADR Template](./adrs/README.md#adr-template)

### 3. Diagrams
Visual representations of the architecture at different levels of abstraction:

- **C4 Model Diagrams**: System context, containers, components, code
- **UML Diagrams**: Class, sequence, state, use case diagrams
- **Database Diagrams**: ER diagrams, schema diagrams
- **Other Diagrams**: Component, deployment, data flow diagrams

**Key Files**:
- [Diagram Overview](./diagrams/architecture-overview.md)
- [Diagram Conventions](./diagrams/README.md)

### 4. Guides and Tutorials
Step-by-step instructions for various tasks:

- **Getting Started**: Quick setup and first run
- **Local Development**: Complete development environment setup
- **API Usage**: How to use the microservices APIs
- **Testing**: Running tests and achieving code coverage
- **Monitoring**: Using the monitoring stack
- **Troubleshooting**: Common issues and solutions

**Key Files**:
- [Getting Started Guide](./guides/getting-started.md)
- [Local Development Guide](./guides/local-development.md)
- [Monitoring Documentation](./guides/monitoring.md)

## Learning Objectives

### Core Concepts
1. **Microservices Architecture**: Decomposing a system into independent services
2. **Domain-Driven Design**: Aligning software with business domains
3. **Distributed Systems**: Communication, consistency, and coordination
4. **Event-Driven Architecture**: Using events for loose coupling
5. **Database Patterns**: Database per service, event sourcing, CQRS

### Technical Skills
1. **Service Implementation**: Building services in multiple languages
2. **API Design**: RESTful APIs, OpenAPI/Swagger documentation
3. **Message Brokers**: Kafka for asynchronous communication
4. **Authentication/Authorization**: OAuth2, OpenID Connect with Keycloak
5. **Monitoring/Observability**: Metrics, logging, tracing
6. **Containerization**: Docker, Docker Compose
7. **Orchestration**: Kubernetes basics

### Architectural Patterns
1. **API Gateway**: Single entry point for client requests
2. **Circuit Breaker**: Preventing cascading failures
3. **Saga Pattern**: Managing distributed transactions
4. **CQRS**: Separating read and write operations
5. **Event Sourcing**: Storing state changes as events

## Project Structure

### Microservices
The platform consists of 8 microservices, each implementing a business capability:

1. **User Service** (`:8082`): User management and authentication
2. **Product Service** (`:8083`): Product catalog management
3. **Cart Service** (`:8084`): Shopping cart management
4. **Order Service** (`:8085`): Order processing and management
5. **Payment Service** (`:8086`): Payment processing
6. **Inventory Service** (`:8087`): Inventory management
7. **Notification Service** (`:8088`): Sending notifications
8. **Analytics Service** (`:8089`): Business analytics

### Infrastructure Services
Supporting services required for the platform:

1. **API Gateway** (`:8080`): Kong API Gateway
2. **Authentication** (`:8180`): Keycloak OAuth2 server
3. **Message Broker**: Apache Kafka cluster
4. **Databases**: PostgreSQL, Redis, Elasticsearch
5. **Monitoring**: Prometheus, Grafana, Jaeger, ELK Stack

### Implementation Languages
Each service is implemented in four languages for comparison:

1. **Java**: Spring Boot, JPA, Spring Security
2. **.NET**: ASP.NET Core, Entity Framework, Identity
3. **Node.js**: Express.js, TypeScript, Prisma
4. **Python**: FastAPI, SQLAlchemy, Pydantic

## Development Workflow

### 1. Environment Setup
```bash
# Clone repository
git clone https://github.com/yourusername/software-architecture-course.git

# Start infrastructure
cd shared
docker-compose up -d

# Choose implementation
cd ../microservices-architecture/java-microservices/user-service
```

### 2. Service Development
```bash
# Build and run service
mvn clean package
mvn spring-boot:run

# Or for other languages
dotnet run      # .NET
npm start       # Node.js
python main.py  # Python
```

### 3. Testing
```bash
# Run tests
mvn test        # Java
dotnet test     # .NET
npm test        # Node.js
pytest          # Python

# Run integration tests
mvn verify -Pintegration
```

### 4. API Testing
```bash
# Get access token
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "client_id=web-client" \
  -d "grant_type=password" \
  -d "username=customer@ecommerce.com" \
  -d "password=customer123"

# Call API
curl -X GET http://localhost:8082/api/v1/users/me \
  -H "Authorization: Bearer <token>"
```

## Monitoring and Observability

### Access URLs
- **Prometheus**: http://localhost:9090 (metrics)
- **Grafana**: http://localhost:3000 (dashboards) - admin/admin
- **Jaeger**: http://localhost:16686 (tracing)
- **Kibana**: http://localhost:5601 (logs)
- **Keycloak**: http://localhost:8180 (auth) - admin/admin

### Key Metrics
1. **Application Metrics**: Request rate, error rate, latency
2. **Business Metrics**: Orders per hour, revenue, conversion rate
3. **Infrastructure Metrics**: CPU, memory, disk, network
4. **Database Metrics**: Connections, queries, replication lag

### Logging
- **Format**: Structured JSON logging
- **Fields**: timestamp, level, service, traceId, message, metadata
- **Search**: Use Kibana to search and analyze logs
- **Levels**: DEBUG, INFO, WARN, ERROR, FATAL

## Educational Value

### For Students
- **Hands-on Experience**: Build and deploy real microservices
- **Pattern Comparison**: Compare implementations across languages
- **Problem Solving**: Debug distributed systems issues
- **Best Practices**: Learn industry-standard patterns and tools

### For Instructors
- **Comprehensive Material**: Ready-to-use course materials
- **Flexible Curriculum**: Adapt to different skill levels
- **Real-world Scenarios**: Based on actual ecommerce patterns
- **Assessment Tools**: Built-in monitoring and testing

### Learning Paths

#### Beginner Path
1. Single service implementation
2. Basic API development
3. Simple database operations
4. Basic testing

#### Intermediate Path
1. Multiple service coordination
2. Event-driven communication
3. Distributed transactions
4. API gateway configuration

#### Advanced Path
1. Performance optimization
2. Security implementation
3. Monitoring and observability
4. Deployment and scaling

## Contributing

### Adding Documentation
1. **Identify Need**: What information is missing?
2. **Choose Location**: Which directory is most appropriate?
3. **Follow Conventions**: Use existing templates and styles
4. **Submit Pull Request**: Include clear description

### Updating Documentation
1. **Check Accuracy**: Ensure documentation matches implementation
2. **Update Examples**: Keep code examples current
3. **Improve Clarity**: Make complex concepts understandable
4. **Add References**: Link to related documentation

### Reporting Issues
1. **Documentation Bugs**: Inaccuracies, typos, broken links
2. **Missing Content**: Topics that need coverage
3. **Improvement Suggestions**: Better organization, examples, etc.
4. **Use GitHub Issues**: Tag with "documentation" label

## Resources

### External References
- [C4 Model](https://c4model.com/) - Architecture documentation framework
- [Microservices.io](https://microservices.io/) - Microservices patterns
- [12-Factor App](https://12factor.net/) - Methodology for SaaS applications
- [Domain-Driven Design](https://domainlanguage.com/ddd/) - Eric Evans' DDD
- [OAuth 2.0](https://oauth.net/2/) - Authorization framework
- [OpenID Connect](https://openid.net/connect/) - Identity layer on OAuth 2.0

### Tools Documentation
- [Docker Documentation](https://docs.docker.com/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [ASP.NET Core Documentation](https://docs.microsoft.com/en-us/aspnet/core/)
- [Express.js Documentation](https://expressjs.com/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

### Books
- "Building Microservices" by Sam Newman
- "Domain-Driven Design" by Eric Evans
- "Designing Data-Intensive Applications" by Martin Kleppmann
- "The Pragmatic Programmer" by David Thomas and Andrew Hunt
- "Clean Architecture" by Robert C. Martin

## Support

### Getting Help
1. **Check Documentation**: Search for your issue in existing docs
2. **Review Examples**: Look at implementation examples
3. **Check Issues**: Search GitHub issues for similar problems
4. **Ask Questions**: Use discussion forum or GitHub issues

### Common Issues
- **Setup Problems**: Check [Getting Started Guide](./guides/getting-started.md#troubleshooting)
- **Database Issues**: Verify Docker containers are running
- **Authentication Issues**: Check Keycloak configuration
- **Service Communication**: Verify Kafka and network connectivity

### Contact
- **GitHub Issues**: For bugs and feature requests
- **Discussion Forum**: For questions and discussions
- **Email**: For private or sensitive issues

## License

This documentation is part of the Software Architecture Course project. All documentation is licensed under [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).

## Acknowledgments

- **Instructors**: For creating and maintaining this course material
- **Students**: For providing feedback and improvements
- **Open Source Community**: For the amazing tools and libraries used
- **Contributors**: Everyone who has helped improve this documentation

---

**Happy Learning!** 🚀

Start your journey by reading the [Getting Started Guide](./guides/getting-started.md) or exploring the [Architecture Documentation](./architecture/).

**Last Updated**: April 2024
**Version**: 1.0.0