-- =====================================================
-- SOA Architecture - Database Initialization Script
-- Creates separate databases for each service
-- following the Database-per-Service pattern
-- =====================================================

-- Create databases for each service
CREATE DATABASE dotnet_product_db;
CREATE DATABASE dotnet_order_db;
CREATE DATABASE dotnet_inventory_db;

-- =====================================================
-- Product Service Database
-- =====================================================
\c dotnet_product_db;

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Order Service Database
-- =====================================================
\c dotnet_order_db;

CREATE TYPE order_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'STOCK_RESERVED', 'INSUFFICIENT_STOCK');

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Inventory Service Database
-- =====================================================
\c dotnet_inventory_db;

CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Seed Data
-- =====================================================
\c dotnet_product_db;

INSERT INTO products (id, name, description, price, category) VALUES
  ('cccccccc-0000-0000-0000-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics'),
  ('cccccccc-0000-0000-0000-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories'),
  ('cccccccc-0000-0000-0000-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories'),
  ('cccccccc-0000-0000-0000-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics'),
  ('cccccccc-0000-0000-0000-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics');

\c dotnet_order_db;

INSERT INTO orders (id, customer_name, customer_email, status, total_amount) VALUES
  ('dddddddd-0000-0000-0000-000000000001', 'Alice Johnson', 'alice@example.com', 'CONFIRMED', 1329.98),
  ('dddddddd-0000-0000-0000-000000000002', 'Bob Smith', 'bob@example.com', 'CONFIRMED', 139.98),
  ('dddddddd-0000-0000-0000-000000000003', 'Carol Davis', 'carol@example.com', 'PENDING', 499.99),
  ('dddddddd-0000-0000-0000-000000000004', 'David Lee', 'david@example.com', 'CONFIRMED', 119.98),
  ('dddddddd-0000-0000-0000-000000000005', 'Eva Martinez', 'eva@example.com', 'CANCELLED', 1299.99);

INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price) VALUES
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000001', 'Laptop Pro 15', 1, 1299.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000002', 'Wireless Mouse', 1, 29.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000002', 'cccccccc-0000-0000-0000-000000000003', 'USB-C Hub 7-in-1', 1, 49.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000002', 'cccccccc-0000-0000-0000-000000000004', 'Mechanical Keyboard', 1, 89.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000003', 'cccccccc-0000-0000-0000-000000000005', 'Monitor 27" 4K', 1, 499.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000004', 'cccccccc-0000-0000-0000-000000000002', 'Wireless Mouse', 1, 29.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000004', 'cccccccc-0000-0000-0000-000000000004', 'Mechanical Keyboard', 1, 89.99),
  (gen_random_uuid(), 'dddddddd-0000-0000-0000-000000000005', 'cccccccc-0000-0000-0000-000000000001', 'Laptop Pro 15', 1, 1299.99);

\c dotnet_inventory_db;

INSERT INTO inventory (id, product_id, product_name, quantity, reserved_quantity) VALUES
  (gen_random_uuid(), 'cccccccc-0000-0000-0000-000000000001', 'Laptop Pro 15', 50, 0),
  (gen_random_uuid(), 'cccccccc-0000-0000-0000-000000000002', 'Wireless Mouse', 200, 0),
  (gen_random_uuid(), 'cccccccc-0000-0000-0000-000000000003', 'USB-C Hub 7-in-1', 150, 0),
  (gen_random_uuid(), 'cccccccc-0000-0000-0000-000000000004', 'Mechanical Keyboard', 75, 0),
  (gen_random_uuid(), 'cccccccc-0000-0000-0000-000000000005', 'Monitor 27" 4K', 30, 0);
