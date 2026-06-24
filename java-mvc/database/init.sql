-- =====================================================
-- Java MVC - Database Initialization Script
-- Creates databases and tables for all 3 architectures
-- =====================================================

-- Create databases
CREATE DATABASE java_hexagonal_db;
CREATE DATABASE java_layered_db;
CREATE DATABASE java_clean_db;

-- =====================================================
-- Hexagonal Architecture Database
-- =====================================================
\c java_hexagonal_db;

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    stock_quantity INT NOT NULL DEFAULT 0,
    sku VARCHAR(100) UNIQUE,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Layered Architecture Database
-- =====================================================
\c java_layered_db;

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    stock_quantity INT NOT NULL DEFAULT 0,
    sku VARCHAR(100) UNIQUE,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Clean Architecture Database
-- =====================================================
\c java_clean_db;

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    stock_quantity INT NOT NULL DEFAULT 0,
    sku VARCHAR(100) UNIQUE,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Seed Data
-- =====================================================
\c java_hexagonal_db;

INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active) VALUES
  ('11111111-1111-1111-1111-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  ('11111111-1111-1111-1111-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  ('11111111-1111-1111-1111-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  ('11111111-1111-1111-1111-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  ('11111111-1111-1111-1111-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);

\c java_layered_db;

INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active) VALUES
  ('22222222-2222-2222-2222-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  ('22222222-2222-2222-2222-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  ('22222222-2222-2222-2222-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  ('22222222-2222-2222-2222-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  ('22222222-2222-2222-2222-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);

\c java_clean_db;

INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active) VALUES
  ('33333333-3333-3333-3333-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  ('33333333-3333-3333-3333-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  ('33333333-3333-3333-3333-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  ('33333333-3333-3333-3333-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  ('33333333-3333-3333-3333-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);
