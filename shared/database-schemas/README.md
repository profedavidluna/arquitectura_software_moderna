# Database Schemas

This directory contains the PostgreSQL database schemas for the ecommerce microservices platform.

## Quick Start

```bash
# Create databases and run migrations
cd migrations
./run-migrations.sh run    # Linux/Mac
.\run-migrations.ps1 run   # Windows
```

Or run per-database migrations individually:

```bash
createdb -U postgres user_db
psql -U postgres -d user_db -f migrations/V1__user_db_schema.sql
```

## Directory Structure

```
database-schemas/
├── user-db-schema.sql          # User Service schema (users, addresses, audit)
├── product-db-schema.sql       # Product Service schema (products, categories, reviews)
├── order-db-schema.sql         # Order Service schema (orders, items, payments, history)
├── inventory-db-schema.sql     # Inventory Service schema (inventory, transactions, alerts)
├── cart-db-schema.sql          # Cart Service schema (carts, items, abandoned)
├── payment-db-schema.sql       # Payment Service schema (transactions, refunds, methods)
├── analytics-db-schema.sql     # Analytics Service schema (events, metrics, behavior)
├── SCHEMA_DOCUMENTATION.md     # Comprehensive schema documentation
├── README.md                   # This file
└── migrations/
    ├── V1__user_db_schema.sql      # Per-database migration (recommended)
    ├── V1__product_db_schema.sql
    ├── V1__order_db_schema.sql
    ├── V1__inventory_db_schema.sql
    ├── V1__cart_db_schema.sql
    ├── V1__payment_db_schema.sql
    ├── V1__analytics_db_schema.sql
    ├── 001-initial-schema.sql      # Consolidated migration (all databases)
    ├── 002-add-indexes.sql
    ├── 003-add-views.sql
    ├── 004-add-constraints.sql
    ├── run-migrations.sh           # Migration runner (Linux/Mac)
    ├── run-migrations.ps1          # Migration runner (Windows)
    └── README.md                   # Migration documentation
```

## Database Overview

| Database | Service | Port | Tables | Key Features |
|----------|---------|------|--------|--------------|
| user_db | User Service | 5432 | 3 | Email validation, audit log, default address trigger |
| product_db | Product Service | 5433 | 3 | Full-text search, hierarchical categories, review moderation |
| order_db | Order Service | 5434 | 4 | Status state machine, auto-history, order number sequence |
| inventory_db | Inventory Service | 5435 | 3 | Low stock alerts, transaction audit, BRIN indexes |
| cart_db | Cart Service | 5436 | 3 | 30-day expiration, abandoned cart recovery |
| payment_db | Payment Service | 5437 | 4 | Idempotency, retry logic, PCI tokenization |
| analytics_db | Analytics Service | 5438 | 5 | Event store, pre-aggregated metrics, data retention |

## Schema Features

All schemas include:

- **UUID primary keys** via `gen_random_uuid()`
- **Timestamps**: `created_at` and `updated_at` on all tables
- **Auto-update triggers**: `updated_at` automatically maintained
- **CHECK constraints**: Status enums, positive amounts, format validation
- **Indexes**: B-tree, GIN, BRIN, composite, and partial indexes
- **Views**: Pre-built queries for common access patterns
- **Comments**: COMMENT ON TABLE/COLUMN for documentation
- **Migration tracking**: `schema_migrations` table in each database

## Documentation

For detailed schema documentation including:
- Complete table definitions with all columns
- Entity relationship diagrams
- Cross-service relationships
- Index strategy
- Performance considerations
- Security notes

See [SCHEMA_DOCUMENTATION.md](./SCHEMA_DOCUMENTATION.md)

## Design Decisions

See [ADR-002: Database Per Service](../../docs/adrs/adr-002-database-per-service.md) for the architectural decision record.

Key decisions:
- **Database per service**: Each microservice owns its database
- **No cross-database FKs**: Services reference each other by UUID only
- **Data denormalization**: Product names stored in order_items and cart_items
- **Eventual consistency**: Kafka events synchronize data between services
- **PostgreSQL**: Chosen for JSONB support, full-text search, and reliability
