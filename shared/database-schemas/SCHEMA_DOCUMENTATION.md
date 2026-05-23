# Database Schema Documentation

## Overview

This document provides comprehensive documentation for the ecommerce microservices database schemas. The system follows a **database-per-service** pattern with 7 PostgreSQL databases, each serving a specific microservice.

## Architecture Summary

| Database | Service | Port | Tables | Purpose |
|----------|---------|------|--------|---------|
| `user_db` | User Service | 5432 | 3 | User profiles, addresses, audit log |
| `product_db` | Product Service | 5433 | 3 | Products, categories, reviews |
| `order_db` | Order Service | 5434 | 4 | Orders, items, payments, status history |
| `inventory_db` | Inventory Service | 5435 | 3 | Stock levels, transactions, alerts |
| `cart_db` | Cart Service | 5436 | 3 | Shopping carts, items, abandoned carts |
| `payment_db` | Payment Service | 5437 | 4 | Transactions, refunds, methods, retry log |
| `analytics_db` | Analytics Service | 5438 | 5 | Events, metrics, performance, behavior, cohorts |

**Total: 25 tables across 7 databases**

## Design Principles

1. **Loose Coupling**: Each service owns its data; no cross-database foreign keys
2. **Data Denormalization**: Product names/SKUs stored in order items and cart items for independence
3. **UUID Primary Keys**: Globally unique, no sequential gaps, distributed-system friendly
4. **Eventual Consistency**: Services communicate via Kafka events, not shared databases
5. **Audit Trail**: All critical tables have created_at/updated_at; status changes are logged

---

## 1. User Database (user_db)

### Tables

#### users
Primary user accounts table.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Unique user identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Login email address |
| username | VARCHAR(100) | UNIQUE, NOT NULL | Display username (alphanumeric + underscore) |
| password_hash | VARCHAR(255) | NOT NULL | Bcrypt hashed password |
| first_name | VARCHAR(100) | NOT NULL | First name |
| last_name | VARCHAR(100) | NOT NULL | Last name |
| phone | VARCHAR(20) | CHECK format | Optional phone number |
| avatar_url | VARCHAR(500) | | Profile image URL |
| role | VARCHAR(50) | NOT NULL, CHECK IN | ADMIN, CUSTOMER, SUPPORT |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Soft delete flag |
| email_verified | BOOLEAN | NOT NULL, DEFAULT false | Email verification status |
| last_login_at | TIMESTAMP | | Last successful login |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Record creation |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last modification (auto-updated) |

#### addresses
User delivery and billing addresses.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique address identifier |
| user_id | UUID | FK -> users(id) CASCADE | Owning user |
| label | VARCHAR(50) | NOT NULL, CHECK IN | Home, Work, Other |
| street | VARCHAR(255) | NOT NULL | Primary street line |
| street_line_2 | VARCHAR(255) | | Secondary address line |
| city | VARCHAR(100) | NOT NULL | City |
| state | VARCHAR(100) | | State/province |
| postal_code | VARCHAR(20) | NOT NULL | ZIP/postal code |
| country | VARCHAR(100) | NOT NULL | Country |
| is_default | BOOLEAN | NOT NULL, DEFAULT false | Default address (one per user) |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### user_audit_log
Audit trail for user account changes.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique log entry identifier |
| user_id | UUID | FK -> users(id) CASCADE | Affected user |
| action | VARCHAR(50) | NOT NULL, CHECK IN | CREATE, UPDATE, DELETE, LOGIN, LOGOUT, PASSWORD_CHANGE, EMAIL_VERIFY |
| ip_address | VARCHAR(45) | | Client IP (IPv4/IPv6) |
| user_agent | TEXT | | Browser user agent |
| changes | JSONB | | Old/new values for changed fields |
| created_at | TIMESTAMP | NOT NULL | When the action occurred |

### Key Features
- Email format validation via regex constraint
- Username format: alphanumeric + underscore, 3-100 chars
- Trigger ensures only one default address per user
- Auto-update of `updated_at` on modifications

---

## 2. Product Database (product_db)

### Tables

#### categories
Hierarchical product categories.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Category identifier |
| name | VARCHAR(100) | UNIQUE, NOT NULL | Display name |
| slug | VARCHAR(120) | UNIQUE, NOT NULL | URL-friendly slug |
| description | TEXT | | Category description |
| parent_id | UUID | FK -> categories(id) SET NULL | Parent for hierarchy |
| image_url | VARCHAR(500) | | Category image |
| sort_order | INT | NOT NULL, DEFAULT 0 | Display ordering |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Visibility flag |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### products
Product catalog.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Product identifier |
| name | VARCHAR(255) | NOT NULL | Product name |
| slug | VARCHAR(280) | UNIQUE, NOT NULL | URL-friendly slug |
| description | TEXT | | Full description |
| short_description | VARCHAR(500) | | Brief summary for listings |
| price | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Selling price |
| compare_at_price | DECIMAL(10,2) | CHECK > price | Original/strikethrough price |
| cost_price | DECIMAL(10,2) | CHECK >= 0 | Cost for margin calculation |
| category_id | UUID | FK -> categories(id) RESTRICT | Product category |
| sku | VARCHAR(100) | UNIQUE, NOT NULL | Stock Keeping Unit |
| barcode | VARCHAR(50) | | EAN/UPC barcode |
| weight | DECIMAL(8,2) | CHECK >= 0 | Shipping weight |
| weight_unit | VARCHAR(10) | CHECK IN | kg, lb, g, oz |
| image_url | VARCHAR(500) | | Primary image URL |
| images | JSONB | DEFAULT [] | Additional images array |
| tags | TEXT[] | DEFAULT {} | Searchable tags |
| attributes | JSONB | DEFAULT {} | Custom attributes (color, size, etc.) |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Catalog visibility |
| is_featured | BOOLEAN | NOT NULL, DEFAULT false | Featured product flag |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### product_reviews
Customer reviews and ratings.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Review identifier |
| product_id | UUID | FK -> products(id) CASCADE | Reviewed product |
| user_id | UUID | NOT NULL | Reviewer (cross-service ref) |
| rating | INT | NOT NULL, CHECK 1-5 | Star rating |
| title | VARCHAR(200) | | Review headline |
| review_text | TEXT | | Full review |
| is_verified_purchase | BOOLEAN | NOT NULL, DEFAULT false | Verified buyer |
| is_approved | BOOLEAN | NOT NULL, DEFAULT false | Moderation status |
| helpful_count | INT | NOT NULL, DEFAULT 0 | Helpfulness votes |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

**Unique constraint**: One review per user per product (product_id, user_id)

### Key Features
- Full-text search index on name + description
- GIN index on tags array for tag-based filtering
- Hierarchical categories via self-referencing parent_id
- Compare-at-price must exceed selling price (discount validation)

---

## 3. Order Database (order_db)

### Tables

#### orders
Master order records.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Order identifier |
| order_number | VARCHAR(20) | UNIQUE, NOT NULL | Human-readable (ORD-YYYYMMDD-NNN) |
| user_id | UUID | NOT NULL | Customer (cross-service ref) |
| status | VARCHAR(50) | NOT NULL, CHECK IN | PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED |
| subtotal | DECIMAL(10,2) | NOT NULL, CHECK >= 0 | Sum of line items |
| tax_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Tax |
| shipping_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Shipping cost |
| discount_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Discounts applied |
| total_amount | DECIMAL(10,2) | NOT NULL, CHECK >= 0 | Final total |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | ISO 4217 code |
| shipping_address | JSONB | NOT NULL | Delivery address snapshot |
| billing_address | JSONB | | Billing address (if different) |
| notes | TEXT | | Customer notes |
| cancelled_reason | TEXT | | Cancellation reason |
| shipped_at | TIMESTAMP | | Auto-set on SHIPPED status |
| delivered_at | TIMESTAMP | | Auto-set on DELIVERED status |
| cancelled_at | TIMESTAMP | | Auto-set on CANCELLED status |
| created_at | TIMESTAMP | NOT NULL | Order placement time |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### order_items
Line items within an order.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Item identifier |
| order_id | UUID | FK -> orders(id) CASCADE | Parent order |
| product_id | UUID | NOT NULL | Product (cross-service ref) |
| product_name | VARCHAR(255) | NOT NULL | Name at time of purchase |
| product_sku | VARCHAR(100) | NOT NULL | SKU at time of purchase |
| quantity | INT | NOT NULL, CHECK > 0 | Units ordered |
| unit_price | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Price per unit |
| subtotal | DECIMAL(10,2) | NOT NULL, CHECK = qty * price | Line total |
| created_at | TIMESTAMP | NOT NULL | Record creation |

#### payments
Payment records for orders.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Payment identifier |
| order_id | UUID | FK -> orders(id) CASCADE | Associated order |
| amount | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Payment amount |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | Currency code |
| status | VARCHAR(50) | NOT NULL, CHECK IN | PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED |
| payment_method | VARCHAR(50) | NOT NULL | Method used |
| transaction_id | VARCHAR(255) | | Gateway transaction ID |
| gateway_response | JSONB | | Raw gateway response |
| paid_at | TIMESTAMP | | Confirmation timestamp |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### order_status_history
Audit trail of status transitions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Entry identifier |
| order_id | UUID | FK -> orders(id) CASCADE | Associated order |
| old_status | VARCHAR(50) | | Previous status |
| new_status | VARCHAR(50) | NOT NULL | New status |
| changed_by | UUID | | User/system that made the change |
| reason | TEXT | | Reason for change |
| metadata | JSONB | | Additional context |
| created_at | TIMESTAMP | NOT NULL | When the change occurred |

### Key Features
- Order number format: `ORD-YYYYMMDD-NNN` (validated by constraint)
- Status change trigger auto-records history and sets timestamp fields
- Subtotal constraint ensures `subtotal = quantity * unit_price`
- Product data denormalized in order_items (name, SKU) for independence

---

## 4. Inventory Database (inventory_db)

### Tables

#### inventory
Current stock levels per product.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Record identifier |
| product_id | UUID | UNIQUE, NOT NULL | Product (one record per product) |
| sku | VARCHAR(100) | NOT NULL | Product SKU (denormalized) |
| warehouse_location | VARCHAR(100) | | Physical location (aisle-shelf-bin) |
| quantity_available | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Available for sale |
| quantity_reserved | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Reserved for pending orders |
| reorder_point | INT | NOT NULL, DEFAULT 10, CHECK >= 0 | Low stock threshold |
| reorder_quantity | INT | NOT NULL, DEFAULT 50, CHECK > 0 | Suggested reorder amount |
| max_quantity | INT | CHECK > 0 | Maximum capacity |
| last_restocked_at | TIMESTAMP | | Last restock event |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### inventory_transactions
Complete audit trail of stock changes.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Transaction identifier |
| product_id | UUID | NOT NULL | Affected product |
| transaction_type | VARCHAR(50) | NOT NULL, CHECK IN | RESERVE, RELEASE, DEPLETE, RESTOCK, ADJUSTMENT, RETURN |
| quantity | INT | NOT NULL, CHECK != 0 | Change amount |
| quantity_before | INT | NOT NULL | Stock before change |
| quantity_after | INT | NOT NULL | Stock after change |
| reference_id | UUID | | Related entity ID |
| reference_type | VARCHAR(50) | CHECK IN | ORDER, RETURN, MANUAL, SYSTEM |
| reason | TEXT | | Human-readable reason |
| performed_by | UUID | | User who performed action |
| created_at | TIMESTAMP | NOT NULL | Transaction timestamp |

#### low_stock_alerts
Automated alerts when stock falls below threshold.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Alert identifier |
| product_id | UUID | NOT NULL | Low-stock product |
| sku | VARCHAR(100) | NOT NULL | Product SKU |
| threshold | INT | NOT NULL | Reorder point breached |
| current_quantity | INT | NOT NULL | Quantity when alert created |
| alert_status | VARCHAR(50) | NOT NULL, CHECK IN | ACTIVE, ACKNOWLEDGED, RESOLVED, IGNORED |
| acknowledged_by | UUID | | User who acknowledged |
| acknowledged_at | TIMESTAMP | | Acknowledgment time |
| resolved_at | TIMESTAMP | | Resolution time |
| created_at | TIMESTAMP | NOT NULL | Alert creation |

### Key Features
- Trigger auto-creates alerts when stock falls below reorder_point
- Trigger auto-resolves alerts when stock is replenished
- BRIN index on transactions for efficient time-range queries
- `quantity_before`/`quantity_after` in transactions for full auditability

---

## 5. Cart Database (cart_db)

### Tables

#### carts
Shopping cart master records.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Cart identifier |
| user_id | UUID | NOT NULL | Cart owner (cross-service ref) |
| session_id | VARCHAR(255) | | Browser session for guest carts |
| status | VARCHAR(50) | NOT NULL, CHECK IN | ACTIVE, ABANDONED, CONVERTED, MERGED |
| item_count | INT | NOT NULL, DEFAULT 0 | Distinct items in cart |
| subtotal | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Sum of item subtotals |
| tax_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Estimated tax |
| shipping_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Estimated shipping |
| discount_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Coupon discount |
| total_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | Estimated total |
| coupon_code | VARCHAR(50) | | Applied coupon |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | Currency code |
| created_at | TIMESTAMP | NOT NULL | Cart creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |
| expires_at | TIMESTAMP | NOT NULL, DEFAULT +30 days | Expiration time |

#### cart_items
Items within a cart.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Item identifier |
| cart_id | UUID | FK -> carts(id) CASCADE | Parent cart |
| product_id | UUID | NOT NULL | Product (cross-service ref) |
| product_name | VARCHAR(255) | NOT NULL | Product name (denormalized) |
| product_sku | VARCHAR(100) | NOT NULL | Product SKU (denormalized) |
| product_image_url | VARCHAR(500) | | Product image (denormalized) |
| quantity | INT | NOT NULL, CHECK > 0 | Units in cart |
| unit_price | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Price per unit |
| subtotal | DECIMAL(10,2) | NOT NULL, CHECK = qty * price | Line total |
| created_at | TIMESTAMP | NOT NULL | When added |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

**Unique constraint**: One entry per product per cart (cart_id, product_id)

#### abandoned_carts
Recovery tracking for abandoned carts.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Record identifier |
| cart_id | UUID | FK -> carts(id) CASCADE | Abandoned cart |
| user_id | UUID | NOT NULL | Cart owner |
| total_amount | DECIMAL(10,2) | NOT NULL | Cart value at abandonment |
| item_count | INT | NOT NULL, DEFAULT 0 | Items in cart |
| abandoned_at | TIMESTAMP | NOT NULL | When abandoned |
| recovery_email_sent | BOOLEAN | NOT NULL, DEFAULT false | Email sent flag |
| recovery_email_sent_at | TIMESTAMP | | When email was sent |
| recovery_email_opened | BOOLEAN | NOT NULL, DEFAULT false | Email opened flag |
| recovered | BOOLEAN | NOT NULL, DEFAULT false | Whether cart was recovered |
| recovered_at | TIMESTAMP | | Recovery timestamp |

### Key Features
- 30-day cart expiration by default
- Trigger auto-creates abandoned_carts record on status change to ABANDONED
- Partial index for active carts (most common query)
- Product data denormalized for display without cross-service calls

---

## 6. Payment Database (payment_db)

### Tables

#### transactions
Payment processing records.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Transaction identifier |
| transaction_number | VARCHAR(30) | UNIQUE, NOT NULL | Human-readable (TXN-YYYYMMDD-NNN) |
| order_id | UUID | NOT NULL | Associated order (cross-service) |
| user_id | UUID | NOT NULL | Payer (cross-service) |
| amount | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Payment amount |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | Currency code |
| status | VARCHAR(50) | NOT NULL, CHECK IN | PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED |
| payment_method | VARCHAR(50) | NOT NULL, CHECK IN | CREDIT_CARD, DEBIT_CARD, PAYPAL, APPLE_PAY, GOOGLE_PAY, BANK_TRANSFER |
| payment_gateway | VARCHAR(50) | NOT NULL, DEFAULT 'STRIPE' | Gateway provider |
| gateway_transaction_id | VARCHAR(255) | | External transaction ID |
| gateway_response | JSONB | | Raw gateway response |
| failure_reason | TEXT | | Failure description |
| idempotency_key | VARCHAR(255) | UNIQUE | Prevents duplicate charges |
| ip_address | VARCHAR(45) | | Payer IP for fraud detection |
| processed_at | TIMESTAMP | | Gateway processing time |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### refunds
Refund records linked to transactions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Refund identifier |
| refund_number | VARCHAR(30) | UNIQUE, NOT NULL | Human-readable (RFN-YYYYMMDD-NNN) |
| transaction_id | UUID | FK -> transactions(id) RESTRICT | Original transaction |
| order_id | UUID | NOT NULL | Associated order |
| amount | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Refund amount (can be partial) |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | Currency code |
| reason | VARCHAR(255) | NOT NULL | Detailed reason |
| reason_category | VARCHAR(50) | NOT NULL, CHECK IN | CUSTOMER_REQUEST, DEFECTIVE_PRODUCT, WRONG_ITEM, LATE_DELIVERY, DUPLICATE_CHARGE, OTHER |
| status | VARCHAR(50) | NOT NULL, CHECK IN | PENDING, PROCESSING, COMPLETED, FAILED, REJECTED |
| gateway_refund_id | VARCHAR(255) | | Gateway refund ID |
| gateway_response | JSONB | | Raw gateway response |
| approved_by | UUID | | Approving admin |
| processed_at | TIMESTAMP | | Processing time |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### payment_methods
Tokenized payment methods (PCI compliant).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Method identifier |
| user_id | UUID | NOT NULL | Owner (cross-service) |
| method_type | VARCHAR(50) | NOT NULL, CHECK IN | Payment type |
| provider | VARCHAR(50) | NOT NULL, DEFAULT 'STRIPE' | Tokenization provider |
| token | VARCHAR(255) | NOT NULL, CHECK not empty | Tokenized reference |
| last_four | VARCHAR(4) | | Last 4 digits for display |
| card_brand | VARCHAR(20) | | VISA, MASTERCARD, AMEX, etc. |
| expiry_month | INT | CHECK 1-12 | Card expiry month |
| expiry_year | INT | CHECK >= 2024 | Card expiry year |
| billing_name | VARCHAR(200) | | Name on card |
| billing_address | JSONB | | Billing address |
| is_default | BOOLEAN | NOT NULL, DEFAULT false | Default method (one per user) |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Active/expired flag |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last modification |

#### payment_retry_log
Failed payment retry tracking.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Log entry identifier |
| transaction_id | UUID | FK -> transactions(id) CASCADE | Failed transaction |
| attempt_number | INT | NOT NULL, CHECK 1-5 | Sequential attempt |
| status | VARCHAR(50) | NOT NULL, CHECK IN | ATTEMPTED, SUCCESS, FAILED |
| error_code | VARCHAR(50) | | Gateway error code |
| error_message | TEXT | | Error description |
| gateway_response | JSONB | | Raw response |
| next_retry_at | TIMESTAMP | | Scheduled next retry |
| created_at | TIMESTAMP | NOT NULL | Attempt timestamp |

### Key Features
- Idempotency key prevents duplicate charges
- RESTRICT on refund -> transaction FK (cannot delete transaction with refunds)
- Max 5 retry attempts enforced by constraint
- Trigger ensures one default payment method per user
- No raw card data stored (PCI compliance via tokenization)

---

## 7. Analytics Database (analytics_db)

### Tables

#### events
Append-only event store for all system events.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Event identifier |
| event_type | VARCHAR(100) | NOT NULL | PAGE_VIEW, ADD_TO_CART, PURCHASE, USER_LOGIN, etc. |
| event_source | VARCHAR(50) | NOT NULL, DEFAULT 'SYSTEM' | Originating service |
| user_id | UUID | | User (NULL for anonymous) |
| session_id | VARCHAR(255) | | Browser session |
| order_id | UUID | | Related order |
| product_id | UUID | | Related product |
| event_data | JSONB | NOT NULL, DEFAULT {} | Event payload |
| metadata | JSONB | DEFAULT {} | Device, location, etc. |
| ip_address | VARCHAR(45) | | Client IP |
| user_agent | TEXT | | Client user agent |
| created_at | TIMESTAMP | NOT NULL | Event timestamp |

#### metrics
Pre-aggregated business metrics.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Record identifier |
| metric_date | DATE | NOT NULL | Aggregation date |
| metric_type | VARCHAR(50) | NOT NULL, CHECK IN | DAILY_SUMMARY, HOURLY_SUMMARY, WEEKLY_SUMMARY |
| total_orders | INT | NOT NULL, DEFAULT 0 | Orders placed |
| total_revenue | DECIMAL(12,2) | NOT NULL, DEFAULT 0 | Revenue |
| total_users | INT | NOT NULL, DEFAULT 0 | Total registered users |
| new_users | INT | NOT NULL, DEFAULT 0 | New registrations |
| active_users | INT | NOT NULL, DEFAULT 0 | Active users |
| average_order_value | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | AOV |
| conversion_rate | DECIMAL(5,4) | NOT NULL, CHECK 0-1 | Sessions to orders |
| cart_abandonment_rate | DECIMAL(5,4) | NOT NULL, CHECK 0-1 | Abandonment rate |
| total_page_views | INT | NOT NULL, DEFAULT 0 | Page views |
| total_sessions | INT | NOT NULL, DEFAULT 0 | Unique sessions |
| created_at | TIMESTAMP | NOT NULL | Record creation |
| updated_at | TIMESTAMP | NOT NULL | Last update |

**Unique constraint**: (metric_date, metric_type)

#### product_performance, user_behavior, cohort_analysis
See individual schema files for full column details.

### Key Features
- BRIN index on events.created_at for efficient time-range scans
- GIN index on event_data for JSON queries
- Data retention function to archive old events
- Pre-aggregated metrics avoid expensive real-time calculations
- Cohort retention rates view with percentage calculations

---

## Cross-Service Relationships

Since each service has its own database, relationships between services are maintained via **UUID references** (not foreign keys). Data consistency is achieved through Kafka events.

```
User Service ──(user_id)──> Order Service
User Service ──(user_id)──> Cart Service
User Service ──(user_id)──> Payment Service
Product Service ──(product_id)──> Order Service (denormalized)
Product Service ──(product_id)──> Cart Service (denormalized)
Product Service ──(product_id)──> Inventory Service
Order Service ──(order_id)──> Payment Service
Order Service ──(order_id)──> Inventory Service (via events)
```

## Index Strategy

| Index Type | Use Case | Example |
|-----------|----------|---------|
| B-tree | Equality, range, sorting | `idx_orders_created_at` |
| GIN | Full-text search, JSONB, arrays | `idx_products_search`, `idx_events_data` |
| BRIN | Time-series append-only data | `idx_events_created_at_brin` |
| Partial | Filtered queries | `WHERE status = 'ACTIVE'` |
| Composite | Multi-column queries | `(user_id, status, created_at DESC)` |

## Data Types Summary

| Purpose | Type | Rationale |
|---------|------|-----------|
| Primary keys | UUID | Globally unique, no coordination needed |
| Money | DECIMAL(10,2) or (12,2) | Exact arithmetic, no floating point errors |
| Timestamps | TIMESTAMP | Timezone-aware when needed |
| Status fields | VARCHAR + CHECK | Readable, queryable, validated |
| Flexible data | JSONB | Schema flexibility with indexing |
| Tags/arrays | TEXT[] | Native PostgreSQL array support |
| IP addresses | VARCHAR(45) | Supports both IPv4 and IPv6 |

## Migration Strategy

See `migrations/README.md` for detailed migration instructions. Key points:

- Per-database migration files: `V1__<database>_schema.sql`
- Flyway-compatible naming convention
- Migration tracking via `schema_migrations` table
- Idempotent with `IF NOT EXISTS` clauses

## Performance Considerations

1. **Connection Pooling**: Use PgBouncer or HikariCP (100 max connections per database)
2. **Read Replicas**: Analytics queries should target read replicas
3. **Partitioning**: Events table should be partitioned by month at scale
4. **Caching**: Product catalog and user sessions cached in Redis
5. **Vacuum**: Schedule regular VACUUM ANALYZE for frequently-updated tables
6. **Fillfactor**: Set to 90% for frequently-updated tables (inventory, orders)

## Security

1. **No raw card data**: Payment methods use tokenization
2. **Password hashing**: Bcrypt via Keycloak
3. **Audit logging**: All user changes tracked
4. **Soft deletes**: Users deactivated, not deleted
5. **Input validation**: CHECK constraints on all enum fields
6. **Row-Level Security**: Can be enabled per-table for multi-tenant scenarios
