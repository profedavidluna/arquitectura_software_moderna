-- =============================================================================
-- SOA Architecture - Database Initialization Script
-- =============================================================================
-- This script creates separate databases for each service following the
-- "Database per Service" pattern in SOA/Microservices architecture.
--
-- WHY separate databases?
-- 1. Loose coupling: services can evolve their schemas independently
-- 2. Data ownership: each service is the single source of truth for its data
-- 3. Technology freedom: each service could use a different DB technology
-- 4. Independent scaling: databases can be scaled based on service needs
-- =============================================================================

-- =============================================================================
-- Create databases for each service
-- =============================================================================
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;

-- =============================================================================
-- PRODUCT DATABASE - Product catalog management
-- =============================================================================
\c product_db;

CREATE TABLE products (
    id UUID PRIMARY KEY,
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
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 'Electronics', 'LAP-PRO-15'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 'Accessories', 'WRL-MOUSE-01'),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'USB-C Hub', '7-in-1 USB-C hub with HDMI output', 49.99, 'Accessories', 'USB-HUB-7IN1');

-- =============================================================================
-- ORDER DATABASE - Order management
-- =============================================================================
\c order_db;

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255),
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL
);

-- Indexes for common queries
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- =============================================================================
-- INVENTORY DATABASE - Stock management
-- =============================================================================
\c inventory_db;

CREATE TABLE inventory (
    id UUID PRIMARY KEY,
    product_id UUID UNIQUE NOT NULL,
    product_name VARCHAR(255),
    quantity_available INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for product lookups
CREATE INDEX idx_inventory_product_id ON inventory(product_id);

-- Sample inventory data (matches product_db products)
INSERT INTO inventory (id, product_id, product_name, quantity_available, quantity_reserved) VALUES
    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Laptop Pro 15', 50, 0),
    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Wireless Mouse', 200, 0),
    ('f6a7b8c9-d0e1-2345-fabc-456789012345', 'c3d4e5f6-a7b8-9012-cdef-123456789012', 'USB-C Hub', 100, 0);
