-- =============================================================================
-- SOA Architecture - Database Initialization Script
-- =============================================================================
-- This script creates separate databases for each service following the
-- "Database per Service" pattern. Each service owns its data and schema,
-- ensuring loose coupling between services.
-- =============================================================================

-- Create databases for each service
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;

-- =============================================================================
-- Product Database Schema
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

-- =============================================================================
-- Order Database Schema
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

-- Indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- =============================================================================
-- Inventory Database Schema
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

-- =============================================================================
-- Seed Data
-- =============================================================================
\c product_db;

INSERT INTO products (id, name, description, price, category, sku, active) VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 'ELEC-LP15-001', true),
  ('aaaaaaaa-0000-0000-0000-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 'ACC-WM-001', true),
  ('aaaaaaaa-0000-0000-0000-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 'ACC-HUB-001', true),
  ('aaaaaaaa-0000-0000-0000-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 'ELEC-KB-001', true),
  ('aaaaaaaa-0000-0000-0000-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 'ELEC-MON-001', true);

\c order_db;

INSERT INTO orders (id, user_id, status, total_amount) VALUES
  ('bbbbbbbb-0000-0000-0000-000000000001', 'ffffffff-0000-0000-0000-000000000001', 'CONFIRMED', 1329.98),
  ('bbbbbbbb-0000-0000-0000-000000000002', 'ffffffff-0000-0000-0000-000000000002', 'CONFIRMED', 139.98),
  ('bbbbbbbb-0000-0000-0000-000000000003', 'ffffffff-0000-0000-0000-000000000001', 'PENDING', 499.99),
  ('bbbbbbbb-0000-0000-0000-000000000004', 'ffffffff-0000-0000-0000-000000000002', 'CONFIRMED', 119.98),
  ('bbbbbbbb-0000-0000-0000-000000000005', 'ffffffff-0000-0000-0000-000000000001', 'CANCELLED', 1299.99);

INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal) VALUES
  ('cccccccc-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Laptop Pro 15', 1, 1299.99, 1299.99),
  ('cccccccc-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', 'Wireless Mouse', 1, 29.99, 29.99),
  ('cccccccc-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000003', 'USB-C Hub 7-in-1', 1, 49.99, 49.99),
  ('cccccccc-0000-0000-0000-000000000004', 'bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000004', 'Mechanical Keyboard', 1, 89.99, 89.99),
  ('cccccccc-0000-0000-0000-000000000005', 'bbbbbbbb-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000005', 'Monitor 27" 4K', 1, 499.99, 499.99),
  ('cccccccc-0000-0000-0000-000000000006', 'bbbbbbbb-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000002', 'Wireless Mouse', 1, 29.99, 29.99),
  ('cccccccc-0000-0000-0000-000000000007', 'bbbbbbbb-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000004', 'Mechanical Keyboard', 1, 89.99, 89.99),
  ('cccccccc-0000-0000-0000-000000000008', 'bbbbbbbb-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000001', 'Laptop Pro 15', 1, 1299.99, 1299.99);

\c inventory_db;

INSERT INTO inventory (id, product_id, product_name, quantity_available, quantity_reserved) VALUES
  ('dddddddd-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Laptop Pro 15', 50, 0),
  ('dddddddd-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000002', 'Wireless Mouse', 200, 0),
  ('dddddddd-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000003', 'USB-C Hub 7-in-1', 150, 0),
  ('dddddddd-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000004', 'Mechanical Keyboard', 75, 0),
  ('dddddddd-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000005', 'Monitor 27" 4K', 30, 0);
