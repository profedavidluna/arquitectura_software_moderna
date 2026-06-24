-- =============================================================================
-- SOA Architecture - Database Initialization Script
-- =============================================================================
-- SOA Principle: Each service owns its data (Database per Service pattern).
-- This script creates isolated databases for each service, ensuring
-- loose coupling at the data layer.
--
-- Services NEVER access each other's databases directly.
-- All inter-service communication happens through the ESB (Kafka).
-- =============================================================================

-- =============================================================================
-- Create databases for each service
-- =============================================================================
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;

-- =============================================================================
-- Product Service Database Schema
-- =============================================================================
\c product_db;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    sku VARCHAR(100) UNIQUE NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for common queries
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_active ON products(active);

-- Sample data for testing
INSERT INTO products (id, name, description, price, category, sku) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 'Electronics', 'ELEC-LP15-001'),
    ('550e8400-e29b-41d4-a716-446655440002', 'Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 'Accessories', 'ACC-WM-001'),
    ('550e8400-e29b-41d4-a716-446655440003', 'USB-C Hub', '7-in-1 USB-C hub with HDMI output', 49.99, 'Accessories', 'ACC-HUB-001'),
    ('550e8400-e29b-41d4-a716-446655440004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 'ELEC-KB-001'),
    ('550e8400-e29b-41d4-a716-446655440005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 'ELEC-MON-001');

-- =============================================================================
-- Order Service Database Schema
-- =============================================================================
\c order_db;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL
);

-- Indexes for common queries
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- =============================================================================
-- Inventory Service Database Schema
-- =============================================================================
\c inventory_db;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID UNIQUE NOT NULL,
    product_name VARCHAR(255),
    quantity_available INT NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    quantity_reserved INT NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    reference_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_transactions_product_id ON inventory_transactions(product_id);
CREATE INDEX idx_inventory_transactions_reference_id ON inventory_transactions(reference_id);

-- Sample inventory data (matches product data)
INSERT INTO inventory (product_id, product_name, quantity_available, quantity_reserved) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Laptop Pro 15', 50, 0),
    ('550e8400-e29b-41d4-a716-446655440002', 'Wireless Mouse', 200, 0),
    ('550e8400-e29b-41d4-a716-446655440003', 'USB-C Hub', 150, 0),
    ('550e8400-e29b-41d4-a716-446655440004', 'Mechanical Keyboard', 75, 0),
    ('550e8400-e29b-41d4-a716-446655440005', 'Monitor 27" 4K', 30, 0);

-- Sample inventory transactions
INSERT INTO inventory_transactions (product_id, transaction_type, quantity, reference_id) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'STOCK_IN', 50, null),
    ('550e8400-e29b-41d4-a716-446655440002', 'STOCK_IN', 200, null),
    ('550e8400-e29b-41d4-a716-446655440003', 'STOCK_IN', 150, null),
    ('550e8400-e29b-41d4-a716-446655440004', 'STOCK_IN', 75, null),
    ('550e8400-e29b-41d4-a716-446655440005', 'STOCK_IN', 30, null);

-- =============================================================================
-- Seed Orders and Order Items
-- =============================================================================
\c order_db;

INSERT INTO orders (id, user_id, status, total_amount) VALUES
    ('aaaaaaaa-bbbb-cccc-dddd-000000000001', 'eeeeeeee-ffff-0000-1111-000000000001', 'CONFIRMED', 1329.98),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000002', 'eeeeeeee-ffff-0000-1111-000000000002', 'CONFIRMED', 139.98),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000003', 'eeeeeeee-ffff-0000-1111-000000000001', 'PENDING', 499.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000004', 'eeeeeeee-ffff-0000-1111-000000000002', 'CONFIRMED', 119.98),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000005', 'eeeeeeee-ffff-0000-1111-000000000001', 'CANCELLED', 1299.99);

INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal) VALUES
    ('aaaaaaaa-bbbb-cccc-dddd-000000000001', '550e8400-e29b-41d4-a716-446655440001', 'Laptop Pro 15', 1, 1299.99, 1299.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000001', '550e8400-e29b-41d4-a716-446655440002', 'Wireless Mouse', 1, 29.99, 29.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000002', '550e8400-e29b-41d4-a716-446655440003', 'USB-C Hub', 1, 49.99, 49.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000002', '550e8400-e29b-41d4-a716-446655440004', 'Mechanical Keyboard', 1, 89.99, 89.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000003', '550e8400-e29b-41d4-a716-446655440005', 'Monitor 27" 4K', 1, 499.99, 499.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000004', '550e8400-e29b-41d4-a716-446655440002', 'Wireless Mouse', 1, 29.99, 29.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000004', '550e8400-e29b-41d4-a716-446655440004', 'Mechanical Keyboard', 1, 89.99, 89.99),
    ('aaaaaaaa-bbbb-cccc-dddd-000000000005', '550e8400-e29b-41d4-a716-446655440001', 'Laptop Pro 15', 1, 1299.99, 1299.99);
