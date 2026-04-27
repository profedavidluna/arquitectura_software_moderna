# Architecture Decision Records (ADRs)

This directory contains all Architecture Decision Records for the Software Architecture Course project.

## What is an ADR?

An Architecture Decision Record (ADR) is a document that captures an important architectural decision made along with its context and consequences.

## ADR Format

Each ADR follows this structure:

1. **Status**: Proposed, Accepted, Deprecated, Superseded
2. **Context**: The issue we're addressing
3. **Decision**: What we decided to do
4. **Consequences**: What becomes easier or harder as a result
5. **Alternatives Considered**: Other options we evaluated

## ADRs

- [ADR-001: Use Microservices Architecture](./adr-001-microservices-architecture.md)
- [ADR-002: Database Per Service Pattern](./adr-002-database-per-service.md)
- [ADR-003: Kafka for Asynchronous Communication](./adr-003-kafka-async-communication.md)
- [ADR-004: Keycloak for Centralized Authentication](./adr-004-keycloak-authentication.md)
- [ADR-005: API Gateway Pattern](./adr-005-api-gateway.md)
- [ADR-006: REST APIs for Synchronous Communication](./adr-006-rest-apis.md)
- [ADR-007: Circuit Breaker Pattern](./adr-007-circuit-breaker.md)
- [ADR-008: Saga Pattern for Distributed Transactions](./adr-008-saga-pattern.md)
- [ADR-009: Docker Compose for Local Development](./adr-009-docker-compose.md)
- [ADR-010: ELK Stack for Centralized Logging](./adr-010-elk-logging.md)
- [ADR-011: Prometheus + Grafana for Metrics](./adr-011-prometheus-grafana.md)
- [ADR-012: Jaeger for Distributed Tracing](./adr-012-jaeger-tracing.md)
- [ADR-013: GitHub Actions for CI/CD](./adr-013-github-actions.md)
- [ADR-014: Four-Language Implementation](./adr-014-four-languages.md)
- [ADR-015: 80% Code Coverage Requirement](./adr-015-code-coverage.md)

## How to Add a New ADR

1. Create a new file: `adr-NNN-title.md`
2. Use the template below
3. Update this README with a link to the new ADR

### ADR Template

```markdown
# ADR-NNN: Title

**Status**: Proposed | Accepted | Deprecated | Superseded

**Context**
[Describe the issue we're addressing]

**Decision**
[Describe what we decided to do]

**Consequences**

*Positive*:
- [Benefit 1]
- [Benefit 2]

*Negative*:
- [Drawback 1]
- [Drawback 2]

**Alternatives Considered**
1. [Alternative 1]: [Why rejected]
2. [Alternative 2]: [Why rejected]
```
