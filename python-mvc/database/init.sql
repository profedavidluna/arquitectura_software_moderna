-- ============================================================
-- Database initialization script for Python Product Catalog APIs
-- Creates 3 separate databases (one per architecture pattern)
-- ============================================================

-- Create databases
CREATE DATABASE hexagonal_db;
CREATE DATABASE layered_db;
CREATE DATABASE clean_db;

-- ============================================================
-- Hexagonal Architecture Database
-- ============================================================
\c hexagonal_db;

CREATE TABLE products (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  category VARCHAR(100),
  stock_quantity INT NOT NULL DEFAULT 0,
  sku VARCHAR(100) UNIQUE NOT NULL,
  active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);

-- ============================================================
-- Layered Architecture Database
-- ============================================================
\c layered_db;

CREATE TABLE products (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  category VARCHAR(100),
  stock_quantity INT NOT NULL DEFAULT 0,
  sku VARCHAR(100) UNIQUE NOT NULL,
  active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);

-- ============================================================
-- Clean Architecture Database
-- ============================================================
\c clean_db;

CREATE TABLE products (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  category VARCHAR(100),
  stock_quantity INT NOT NULL DEFAULT 0,
  sku VARCHAR(100) UNIQUE NOT NULL,
  active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);

-- ============================================================
-- Seed Data
-- ============================================================
\c hexagonal_db;

INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active) VALUES
  ('77777777-7777-7777-7777-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  ('77777777-7777-7777-7777-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  ('77777777-7777-7777-7777-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  ('77777777-7777-7777-7777-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  ('77777777-7777-7777-7777-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);

\c layered_db;

INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active) VALUES
  ('88888888-8888-8888-8888-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  ('88888888-8888-8888-8888-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  ('88888888-8888-8888-8888-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  ('88888888-8888-8888-8888-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  ('88888888-8888-8888-8888-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);

\c clean_db;

INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active) VALUES
  ('99999999-9999-9999-9999-000000000001', 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  ('99999999-9999-9999-9999-000000000002', 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  ('99999999-9999-9999-9999-000000000003', 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  ('99999999-9999-9999-9999-000000000004', 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  ('99999999-9999-9999-9999-000000000005', 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);
