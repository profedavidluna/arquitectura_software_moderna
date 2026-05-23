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
