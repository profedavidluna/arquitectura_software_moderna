# Database Migrations

This directory contains database migration scripts for the ecommerce microservices platform.

## Migration Strategy

We use two migration approaches:

### 1. Per-Database Migrations (Recommended)

Each database has its own versioned migration file following the naming convention `V{version}__{database}_schema.sql`. This is the recommended approach for production deployments where each service manages its own database independently.

| File | Database | Description |
|------|----------|-------------|
| `V1__user_db_schema.sql` | user_db | Users, addresses, audit log |
| `V1__product_db_schema.sql` | product_db | Products, categories, reviews |
| `V1__order_db_schema.sql` | order_db | Orders, order items, payments, status history |
| `V1__inventory_db_schema.sql` | inventory_db | Inventory, transactions, low stock alerts |
| `V1__cart_db_schema.sql` | cart_db | Carts, cart items, abandoned carts |
| `V1__payment_db_schema.sql` | payment_db | Transactions, refunds, payment methods, retry log |
| `V1__analytics_db_schema.sql` | analytics_db | Events, metrics, product performance, user behavior, cohorts |

### 2. Consolidated Migrations (Development/Learning)

For development and learning purposes, consolidated migration files apply all schemas in sequence:

| File | Description |
|------|-------------|
| `001-initial-schema.sql` | Creates all tables across all 7 databases |
| `002-add-indexes.sql` | Creates indexes for query performance |
| `003-add-views.sql` | Creates views for common queries |
| `004-add-constraints.sql` | Adds constraints, triggers, and optimizations |

## Running Migrations

### Prerequisites

1. PostgreSQL 14+ installed and running
2. `psql` command-line tool available
3. Appropriate database permissions (CREATE, ALTER, DROP)

### Per-Database Migration (Recommended)

```bash
# Create the database first
createdb -h localhost -U postgres user_db

# Run the migration for that specific database
psql -h localhost -U postgres -d user_db -f V1__user_db_schema.sql

# Repeat for each database
createdb -h localhost -U postgres product_db
psql -h localhost -U postgres -d product_db -f V1__product_db_schema.sql

createdb -h localhost -U postgres order_db
psql -h localhost -U postgres -d order_db -f V1__order_db_schema.sql

createdb -h localhost -U postgres inventory_db
psql -h localhost -U postgres -d inventory_db -f V1__inventory_db_schema.sql

createdb -h localhost -U postgres cart_db
psql -h localhost -U postgres -d cart_db -f V1__cart_db_schema.sql

createdb -h localhost -U postgres payment_db
psql -h localhost -U postgres -d payment_db -f V1__payment_db_schema.sql

createdb -h localhost -U postgres analytics_db
psql -h localhost -U postgres -d analytics_db -f V1__analytics_db_schema.sql
```

### Using the Migration Runner Scripts

#### Linux/Mac
```bash
chmod +x run-migrations.sh
./run-migrations.sh run        # Run all migrations
./run-migrations.sh status     # Check migration status
./run-migrations.sh help       # Show help
```

#### Windows (PowerShell)
```powershell
.\run-migrations.ps1 run       # Run all migrations
.\run-migrations.ps1 status    # Check migration status
.\run-migrations.ps1 help      # Show help
```

### Environment Variables

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=postgres
export DB_PASSWORD=postgres
export USER_DB=user_db
export PRODUCT_DB=product_db
export ORDER_DB=order_db
export INVENTORY_DB=inventory_db
export CART_DB=cart_db
export PAYMENT_DB=payment_db
export ANALYTICS_DB=analytics_db
```

## Migration Tracking

Each per-database migration creates a `schema_migrations` table to track applied migrations:

```sql
SELECT * FROM schema_migrations;
-- id | migration_name          | applied_at
-- 1  | V1__user_db_schema      | 2026-11-05 10:00:00
```

## Migration Validation

After running migrations, verify the schema:

```sql
-- Check all tables were created
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- Check indexes
SELECT indexname, tablename FROM pg_indexes WHERE schemaname = 'public' ORDER BY tablename;

-- Check views
SELECT viewname FROM pg_views WHERE schemaname = 'public' ORDER BY viewname;

-- Check constraints
SELECT conname, conrelid::regclass FROM pg_constraint WHERE connamespace = 'public'::regnamespace;
```

## Adding New Migrations

When adding new migrations:

1. Create a new file with the next version number: `V2__description.sql`
2. Include `IF NOT EXISTS` for idempotency
3. Add the migration name to the `schema_migrations` table at the end
4. Test in a development environment first
5. Update this README

## Rollback Strategy

Each migration should be reversible. Create a corresponding rollback file:

```
V1__user_db_schema.sql          → V1__user_db_schema_rollback.sql
V2__add_user_preferences.sql    → V2__add_user_preferences_rollback.sql
```

For the current version, rollback by restoring from backup or dropping the database:

```bash
dropdb -h localhost -U postgres user_db
createdb -h localhost -U postgres user_db
psql -h localhost -U postgres -d user_db -f V1__user_db_schema.sql
```

## Best Practices

1. **Idempotent**: Use `IF NOT EXISTS` and `IF EXISTS` for safe re-runs
2. **Atomic**: Each migration should be a complete, self-contained unit
3. **Backward Compatible**: Avoid breaking changes; add columns as nullable first
4. **Tested**: Always test migrations in staging before production
5. **Documented**: Include comments explaining complex changes
6. **Versioned**: Never modify an already-applied migration; create a new one

## Compatibility with Flyway/Liquibase

The `V{version}__description.sql` naming convention is compatible with [Flyway](https://flywaydb.org/). To use with Flyway:

```yaml
# flyway.conf
flyway.url=jdbc:postgresql://localhost:5432/user_db
flyway.user=postgres
flyway.password=postgres
flyway.locations=filesystem:./migrations
```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Permission denied | `GRANT ALL ON DATABASE db_name TO postgres;` |
| Extension not found | `CREATE EXTENSION IF NOT EXISTS "uuid-ossp";` |
| Duplicate key | Migration already applied; check `schema_migrations` |
| Connection refused | Verify PostgreSQL is running on the expected port |

### Checking Migration Status

```sql
-- See which migrations have been applied
SELECT migration_name, applied_at FROM schema_migrations ORDER BY applied_at;

-- Check if a specific table exists
SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'users');
```
