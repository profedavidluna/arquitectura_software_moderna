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
