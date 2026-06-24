-- Create databases
CREATE DATABASE dotnet_hexagonal_db;
CREATE DATABASE dotnet_layered_db;
CREATE DATABASE dotnet_clean_db;

-- Create Products table in dotnet_hexagonal_db
\c dotnet_hexagonal_db;

CREATE TABLE "Products" (
  "Id" UUID PRIMARY KEY,
  "Name" VARCHAR(255) NOT NULL,
  "Description" TEXT,
  "Price" DECIMAL(10, 2) NOT NULL,
  "Category" VARCHAR(100),
  "StockQuantity" INT NOT NULL DEFAULT 0,
  "Sku" VARCHAR(100) UNIQUE,
  "Active" BOOLEAN DEFAULT true,
  "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Products table in dotnet_layered_db
\c dotnet_layered_db;

CREATE TABLE "Products" (
  "Id" UUID PRIMARY KEY,
  "Name" VARCHAR(255) NOT NULL,
  "Description" TEXT,
  "Price" DECIMAL(10, 2) NOT NULL,
  "Category" VARCHAR(100),
  "StockQuantity" INT NOT NULL DEFAULT 0,
  "Sku" VARCHAR(100) UNIQUE,
  "Active" BOOLEAN DEFAULT true,
  "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Products table in dotnet_clean_db
\c dotnet_clean_db;

CREATE TABLE "Products" (
  "Id" UUID PRIMARY KEY,
  "Name" VARCHAR(255) NOT NULL,
  "Description" TEXT,
  "Price" DECIMAL(10, 2) NOT NULL,
  "Category" VARCHAR(100),
  "StockQuantity" INT NOT NULL DEFAULT 0,
  "Sku" VARCHAR(100) UNIQUE,
  "Active" BOOLEAN DEFAULT true,
  "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Seed Data
-- =====================================================
\c dotnet_hexagonal_db;

INSERT INTO "Products" ("Id", "Name", "Description", "Price", "Category", "StockQuantity", "Sku", "Active") VALUES
  (gen_random_uuid(), 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  (gen_random_uuid(), 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  (gen_random_uuid(), 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  (gen_random_uuid(), 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  (gen_random_uuid(), 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);

\c dotnet_layered_db;

INSERT INTO "Products" ("Id", "Name", "Description", "Price", "Category", "StockQuantity", "Sku", "Active") VALUES
  (gen_random_uuid(), 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  (gen_random_uuid(), 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  (gen_random_uuid(), 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  (gen_random_uuid(), 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  (gen_random_uuid(), 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);

\c dotnet_clean_db;

INSERT INTO "Products" ("Id", "Name", "Description", "Price", "Category", "StockQuantity", "Sku", "Active") VALUES
  (gen_random_uuid(), 'Laptop Pro 15', 'High-performance laptop with 16GB RAM and SSD', 1299.99, 'Electronics', 50, 'ELEC-LP15-001', true),
  (gen_random_uuid(), 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Accessories', 200, 'ACC-WM-001', true),
  (gen_random_uuid(), 'USB-C Hub 7-in-1', 'Multi-port hub with HDMI, USB and card reader', 49.99, 'Accessories', 150, 'ACC-HUB-001', true),
  (gen_random_uuid(), 'Mechanical Keyboard', 'Compact RGB mechanical keyboard with blue switches', 89.99, 'Electronics', 75, 'ELEC-KB-001', true),
  (gen_random_uuid(), 'Monitor 27" 4K', 'Ultra HD 4K IPS monitor with HDR support', 499.99, 'Electronics', 30, 'ELEC-MON-001', true);
