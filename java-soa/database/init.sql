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
    ('550e8400-e29b-41d4-a716-446655440003', 'USB-C Hub', '7-in-1 USB-C hub with HDMI output', 49.99, 'Accessories', 'ACC-HUB-001');

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
    ('550e8400-e29b-41d4-a716-446655440003', 'USB-C Hub', 150, 0);
