# Software Architecture Course - Ecommerce Microservices

A comprehensive 32-hour software architecture course featuring a practical ecommerce project implemented across three architectural patterns (SOA, MVC, Microservices) and four programming languages (Java, .NET, Node.js, Python).

## 📚 Course Overview

This course teaches modern software architecture principles through hands-on implementation of a complete ecommerce platform. Students will learn:

- **Architectural Patterns**: SOA, MVC, and Microservices
- **Design Principles**: SOLID principles and design patterns
- **Distributed Systems**: Event-driven architecture, saga patterns, circuit breakers
- **DevOps & Infrastructure**: Docker, Kubernetes, CI/CD pipelines
- **Observability**: Logging, monitoring, distributed tracing
- **Testing**: Unit, integration, contract, and end-to-end testing
- **Security**: OAuth2/OIDC, API security, data protection

## 🎯 Course Structure

### Duration: 32 Hours

- **8 hours**: SOA (Service-Oriented Architecture) fundamentals and implementation
- **8 hours**: MVC (Model-View-Controller) pattern and web application design
- **8 hours**: Microservices architecture with distributed systems patterns
- **8 hours**: Advanced theory covering SOLID principles, design patterns, resilience, and observability

## 🏗️ Project: Ecommerce Platform

### Core Features

- **User Management**: Registration, authentication, profile management
- **Product Catalog**: Browse, search, filter products
- **Shopping Cart**: Add/remove items, manage quantities
- **Checkout Process**: Multi-step order creation
- **Payment Processing**: Stripe/PayPal integration
- **Inventory Management**: Stock tracking and reservations
- **Order Management**: Order creation, tracking, fulfillment
- **Notifications**: Email confirmations and updates
- **Analytics**: Sales metrics and business intelligence

### Non-Functional Requirements

- **Scalability**: Support 10,000 concurrent users
- **Performance**: API response time < 200ms (p95)
- **Availability**: 99.5% uptime SLA
- **Reliability**: Graceful degradation, circuit breakers, retry logic
- **Security**: OAuth2/OIDC via Keycloak, encrypted data
- **Observability**: Centralized logging, distributed tracing, metrics
- **Testing**: Minimum 80% code coverage, automated CI/CD

## 📁 Repository Structure

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
├── shared/
│   ├── docker-compose.yml
│   ├── keycloak-config/
│   └── database-schemas/
└── README.md
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Git
- Your preferred IDE (VS Code, IntelliJ, Visual Studio)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/software-architecture-course.git
   cd software-architecture-course
   ```

2. **Start the infrastructure stack**
   ```bash
   cd shared
   docker-compose up -d
   ```

3. **Verify services are running**
   ```bash
   docker-compose ps
   ```

4. **Access the services**
   - **Keycloak**: http://localhost:8180 (admin/admin)
   - **Kibana**: http://localhost:5601
   - **Grafana**: http://localhost:3000 (admin/admin)
   - **Jaeger**: http://localhost:16686
   - **Prometheus**: http://localhost:9090

### Running a Microservice

Each microservice can be run independently. For example, to run the Java User Service:

```bash
cd microservices-architecture/java-microservices/user-service
mvn spring-boot:run
```

## 📖 Documentation

- **[Architecture Documentation](./docs/architecture/)**: System design, C4 models, diagrams
- **[Architecture Decision Records](./docs/adrs/)**: Key architectural decisions
- **[Guides & Tutorials](./docs/guides/)**: Setup, deployment, troubleshooting
- **[Diagrams](./docs/diagrams/)**: Visual representations of the system

## 🔧 Technology Stack

### Languages & Frameworks
- **Java**: Spring Boot, Spring Cloud
- **.NET**: ASP.NET Core, Entity Framework
- **Node.js**: Express.js, TypeScript
- **Python**: FastAPI, SQLAlchemy

### Infrastructure
- **Containerization**: Docker, Docker Compose
- **Message Broker**: Apache Kafka
- **Databases**: PostgreSQL (7 instances)
- **Cache**: Redis
- **Authentication**: Keycloak (OAuth2/OIDC)
- **API Gateway**: Kong or custom implementation

### Observability
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Metrics**: Prometheus + Grafana
- **Tracing**: Jaeger
- **Monitoring**: Prometheus AlertManager

### CI/CD
- **Version Control**: Git/GitHub
- **CI/CD**: GitHub Actions
- **Code Quality**: SonarQube
- **Testing**: JUnit, xUnit, Jest, pytest

## 🎓 Learning Outcomes

After completing this course, students will be able to:

1. **Design scalable microservices architectures**
2. **Implement distributed systems patterns** (Saga, Circuit Breaker, etc.)
3. **Build resilient and observable systems**
4. **Apply SOLID principles and design patterns**
5. **Implement comprehensive testing strategies**
6. **Set up CI/CD pipelines**
7. **Deploy and monitor production systems**
8. **Work with multiple programming languages and frameworks**

## 📋 Implementation Phases

### Phase 1: Foundation & Infrastructure Setup
- Repository structure
- Docker Compose stack
- Keycloak configuration
- Database schemas
- Kafka topics
- API Gateway
- Monitoring stack

### Phase 2-5: Microservices Implementation
- User Service (Java, .NET, Node.js, Python)
- Product Service
- Cart Service
- Order Service
- Payment Service
- Inventory Service
- Notification Service
- Analytics Service

### Phase 6: Quality Assurance & Testing
- End-to-end testing
- Performance testing
- Security testing
- Load testing
- Chaos engineering

### Phase 7: CI/CD Pipeline & Deployment
- GitHub Actions workflows
- Docker image optimization
- Deployment automation
- Secrets management

### Phase 8: Documentation & Course Materials
- Architecture documentation
- API documentation
- Deployment guides
- Course materials and slides

## 🔐 Security

- **Authentication**: OAuth2/OIDC via Keycloak
- **Authorization**: Role-based access control (RBAC)
- **Data Protection**: TLS 1.3 for transit, AES-256 for at-rest
- **API Security**: Rate limiting, input validation, CORS
- **PCI Compliance**: Secure payment processing

## 📊 Monitoring & Observability

### Logging
- Centralized log collection with ELK Stack
- Structured JSON logging
- 7-day retention

### Metrics
- Prometheus metrics collection
- Grafana dashboards
- Alerting rules for anomalies

### Tracing
- Jaeger distributed tracing
- Request flow visualization
- Performance bottleneck identification

## 🧪 Testing Strategy

- **Unit Tests**: 40% of effort (target 80% coverage)
- **Integration Tests**: 30% of effort
- **Contract Tests**: 15% of effort
- **E2E Tests**: 10% of effort
- **Performance Tests**: 5% of effort

## 🤝 Contributing

This is an educational project. Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍🏫 Instructor

[Your Name/Organization]

## 📞 Support

For questions or issues:
- Check the [Troubleshooting Guide](./docs/guides/troubleshooting.md)
- Review the [Architecture Documentation](./docs/architecture/)
- Open an issue on GitHub

## 🎉 Getting Started

Ready to begin? Start with the [Getting Started Guide](./docs/guides/getting-started.md)!

---

**Last Updated**: April 2024
**Course Duration**: 32 hours
**Target Audience**: Intermediate to Advanced Developers
