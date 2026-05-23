# Python Microservices - E-Commerce Platform

## Architecture Overview

This project implements an e-commerce platform using 8 microservices built with Python, FastAPI, and async patterns.

## Services

| Service | Port | Description |
|---------|------|-------------|
| user-service | 7082 | User management, profiles, addresses |
| product-service | 7083 | Product catalog, categories, search |
| cart-service | 7084 | Shopping cart, pricing, coupons |
| order-service | 7085 | Order lifecycle, Saga orchestration |
| payment-service | 7086 | Payment processing, refunds |
| inventory-service | 7087 | Stock management, reservations |
| notification-service | 7088 | Email notifications via Kafka |
| analytics-service | 7089 | Event aggregation, metrics |

## Technology Stack

- **Language**: Python 3.11
- **Framework**: FastAPI + Uvicorn
- **Messaging**: aiokafka (async Kafka)
- **Database**: asyncpg (async PostgreSQL)
- **Cache**: redis (async Redis)
- **Validation**: Pydantic v2
- **Testing**: pytest + pytest-asyncio

## Architecture Patterns

1. **Circuit Breaker** - Custom implementation with httpx for inter-service calls
2. **Saga Pattern** - Order service orchestrates distributed transactions with compensation
3. **Event-Driven** - aiokafka pub/sub for async communication
4. **Cache-Aside** - Redis caching in product-service and cart-service
5. **Repository Pattern** - asyncpg abstraction layer
6. **Retry with Backoff** - Payment processing resilience

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Python 3.11+ (for local development)

### Run with Docker Compose

```bash
docker-compose up -d
```

### Local Development

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows

# Install dependencies for a service
cd user-service
pip install -r requirements.txt

# Run service
uvicorn app.main:app --host 0.0.0.0 --port 7082 --reload
```

### Run Tests

```bash
cd <service-name>
pytest app/tests/ -v
```

## Infrastructure

- **Kafka**: 9097 (external), 29097 (internal)
- **Zookeeper**: 2186
- **PostgreSQL**: 5441 (7 databases)
- **Redis**: 6381

## API Documentation

Each service exposes Swagger UI at `http://localhost:<port>/docs`

## Project Structure

Each service follows Clean Architecture:
```
<service-name>/
├── requirements.txt
├── Dockerfile
└── app/
    ├── main.py              # FastAPI app + lifespan
    ├── config.py            # Settings
    ├── domain/              # Business logic
    │   ├── models.py        # Dataclasses
    │   └── interfaces.py    # Protocol classes
    ├── application/         # Use cases
    │   └── service.py
    ├── infrastructure/      # External concerns
    │   ├── persistence/     # Database
    │   ├── messaging/       # Kafka
    │   ├── cache/           # Redis
    │   └── web/             # HTTP (routers, DTOs)
    └── tests/               # Unit tests
```
