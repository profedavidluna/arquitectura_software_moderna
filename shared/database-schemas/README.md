# Database Schemas

This directory contains SQL schema definitions for all microservices databases.

## Database Overview

The ecommerce platform uses a "database per service" pattern with 7 PostgreSQL databases:

| Database | Service | Purpose |
|----------|---------|---------|
| user_db | User Service | User profiles, addresses, authentication |
| product_db | Product Service | Products, categories, reviews |
| order_db | Order Service | Orders, order items, payments |
| inventory_db | Inventory Service | Stock levels, reservations, transactions |
| cart_db | Cart Service | Shopping carts, cart items |
| payment_db | Payment Service | Payment transactions, refunds, methods |
| analytics_db | Analytics Service | Events, metrics, user behavior |

## Schema Files

- **user-db-schema.sql**: User and address management
- **product-db-schema.sql**: Product catalog and reviews
- **order-db-schema.sql**: Order management and history
- **inventory-db-schema.sql**: Stock tracking and reservations
- **cart-db-schema.sql**: Shopping cart management
- **payment-db-schema.sql**: Payment processing and refunds
- **analytics-db-schema.sql**: Analytics and reporting

## Initialization

### Option 1: Using Docker Compose

The schemas are automatically initialized when Docker Compose starts:

```bash
cd shared
docker-compose up -d
```

### Option 2: Manual Initialization

1. Connect to each database:
   ```bash
   psql -h localhost -p 5432 -U postgres -d user_db
   ```

2. Run the schema file:
   ```bash
   \i user-db-schema.sql
   ```

3. Repeat for each database with appropriate port and schema file

### Option 3: Using Migration Tools

For production, use migration tools like:
- **Java**: Flyway or Liquibase
- **.NET**: Entity Framework Migrations
- **Node.js**: db-migrate or Knex.js
- **Python**: Alembic or SQLAlchemy

## Key Design Patterns

### 1. UUID Primary Keys
All tables use UUID as primary key for distributed systems:
```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

### 2. Timestamps
All tables include created_at and updated_at for audit trails:
```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
```

### 3. Soft Deletes (Optional)
Some tables include is_active flag instead of hard deletes:
```sql
is_active BOOLEAN DEFAULT true
```

### 4. Indexes
Strategic indexes for common queries:
- Foreign key columns
- Status/state columns
- Date columns for range queries
- Full-text search indexes

### 5. Constraints
Data integrity constraints:
- NOT NULL for required fields
- CHECK constraints for valid values
- UNIQUE constraints for unique fields
- FOREIGN KEY constraints for relationships

## Data Relationships

### User Service
- Users have multiple addresses
- Audit log tracks user changes

### Product Service
- Products belong to categories
- Products have multiple reviews

### Order Service
- Orders contain multiple order items
- Orders have payment records
- Order status history tracks changes

### Inventory Service
- One inventory record per product
- Inventory transactions track all changes
- Low stock alerts for monitoring

### Cart Service
- Carts belong to users
- Carts contain multiple items
- Abandoned carts tracked separately

### Payment Service
- Transactions linked to orders
- Refunds linked to transactions
- Payment methods stored per user
- Retry log for failed payments

### Analytics Service
- Events from all services
- Daily metrics aggregation
- Product performance tracking
- User behavior analysis
- Cohort analysis for retention

## Backup & Recovery

### Backup

```bash
# Backup single database
pg_dump -h localhost -U postgres user_db > user_db_backup.sql

# Backup all databases
for db in user_db product_db order_db inventory_db cart_db payment_db analytics_db; do
  pg_dump -h localhost -U postgres $db > ${db}_backup.sql
done
```

### Restore

```bash
# Restore single database
psql -h localhost -U postgres user_db < user_db_backup.sql

# Restore all databases
for db in user_db product_db order_db inventory_db cart_db payment_db analytics_db; do
  psql -h localhost -U postgres $db < ${db}_backup.sql
done
```

## Performance Optimization

### Query Optimization
- Use indexes for WHERE, JOIN, and ORDER BY clauses
- Analyze query plans with EXPLAIN
- Monitor slow queries with pg_stat_statements

### Connection Pooling
- Use PgBouncer for connection pooling
- Configure appropriate pool size (typically 20-100)
- Monitor connection usage

### Maintenance
- Regular VACUUM to reclaim space
- ANALYZE to update statistics
- REINDEX for index maintenance

## Monitoring

### Check Database Size
```sql
SELECT datname, pg_size_pretty(pg_database_size(datname)) 
FROM pg_database 
WHERE datname LIKE '%_db';
```

### Check Table Size
```sql
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) 
FROM pg_tables 
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Check Index Usage
```sql
SELECT schemaname, tablename, indexname, idx_scan 
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

## Troubleshooting

### Connection Issues
- Check PostgreSQL is running: `docker ps | grep postgres`
- Verify port mappings: `docker port <container_name>`
- Check credentials in connection string

### Schema Issues
- Verify schema exists: `\dt` in psql
- Check for constraint violations
- Review error logs: `docker logs <container_name>`

### Performance Issues
- Check slow query log
- Analyze query plans
- Review index usage
- Monitor connection pool

## References

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Database Design Best Practices](https://en.wikipedia.org/wiki/Database_design)
- [Microservices Data Management](https://microservices.io/patterns/data/database-per-service.html)
