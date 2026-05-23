-- Database initialization script for .NET Microservices
-- Creates all required databases and schemas

-- Create databases
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE cart_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE inventory_db;
CREATE DATABASE analytics_db;

-- User DB Schema
\c user_db;
CREATE TABLE IF NOT EXISTS "Users" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "Email" VARCHAR(255) UNIQUE NOT NULL,
    "FirstName" VARCHAR(100) NOT NULL,
    "LastName" VARCHAR(100) NOT NULL,
    "PasswordHash" VARCHAR(255) NOT NULL,
    "Phone" VARCHAR(20),
    "IsActive" BOOLEAN DEFAULT true,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "Addresses" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "UserId" UUID NOT NULL REFERENCES "Users"("Id") ON DELETE CASCADE,
    "Street" VARCHAR(255) NOT NULL,
    "City" VARCHAR(100) NOT NULL,
    "State" VARCHAR(100) NOT NULL,
    "ZipCode" VARCHAR(20) NOT NULL,
    "Country" VARCHAR(100) NOT NULL,
    "IsDefault" BOOLEAN DEFAULT false,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product DB Schema
\c product_db;
CREATE TABLE IF NOT EXISTS "Categories" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "Name" VARCHAR(100) NOT NULL,
    "Description" TEXT,
    "ParentId" UUID REFERENCES "Categories"("Id"),
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "Products" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "Name" VARCHAR(255) NOT NULL,
    "Description" TEXT,
    "Price" DECIMAL(10,2) NOT NULL,
    "CategoryId" UUID REFERENCES "Categories"("Id"),
    "ImageUrl" VARCHAR(500),
    "IsActive" BOOLEAN DEFAULT true,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "Reviews" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "ProductId" UUID NOT NULL REFERENCES "Products"("Id") ON DELETE CASCADE,
    "UserId" UUID NOT NULL,
    "Rating" INTEGER NOT NULL CHECK ("Rating" >= 1 AND "Rating" <= 5),
    "Comment" TEXT,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Cart DB Schema
\c cart_db;
CREATE TABLE IF NOT EXISTS "Carts" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "UserId" UUID NOT NULL,
    "Status" VARCHAR(20) DEFAULT 'active',
    "CouponCode" VARCHAR(50),
    "DiscountPercent" DECIMAL(5,2) DEFAULT 0,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "CartItems" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "CartId" UUID NOT NULL REFERENCES "Carts"("Id") ON DELETE CASCADE,
    "ProductId" UUID NOT NULL,
    "ProductName" VARCHAR(255) NOT NULL,
    "UnitPrice" DECIMAL(10,2) NOT NULL,
    "Quantity" INTEGER NOT NULL DEFAULT 1,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order DB Schema
\c order_db;
CREATE TABLE IF NOT EXISTS "Orders" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "UserId" UUID NOT NULL,
    "Status" VARCHAR(30) DEFAULT 'pending',
    "TotalAmount" DECIMAL(10,2) NOT NULL,
    "ShippingAddress" TEXT,
    "SagaState" VARCHAR(30) DEFAULT 'initiated',
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "OrderItems" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "OrderId" UUID NOT NULL REFERENCES "Orders"("Id") ON DELETE CASCADE,
    "ProductId" UUID NOT NULL,
    "ProductName" VARCHAR(255) NOT NULL,
    "UnitPrice" DECIMAL(10,2) NOT NULL,
    "Quantity" INTEGER NOT NULL
);

-- Payment DB Schema
\c payment_db;
CREATE TABLE IF NOT EXISTS "Payments" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "OrderId" UUID NOT NULL,
    "UserId" UUID NOT NULL,
    "Amount" DECIMAL(10,2) NOT NULL,
    "Currency" VARCHAR(3) DEFAULT 'USD',
    "Status" VARCHAR(30) DEFAULT 'pending',
    "PaymentMethod" VARCHAR(50),
    "TransactionId" VARCHAR(255),
    "FailureReason" TEXT,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "Refunds" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "PaymentId" UUID NOT NULL REFERENCES "Payments"("Id"),
    "Amount" DECIMAL(10,2) NOT NULL,
    "Reason" TEXT,
    "Status" VARCHAR(30) DEFAULT 'pending',
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inventory DB Schema
\c inventory_db;
CREATE TABLE IF NOT EXISTS "InventoryItems" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "ProductId" UUID NOT NULL UNIQUE,
    "ProductName" VARCHAR(255) NOT NULL,
    "Quantity" INTEGER NOT NULL DEFAULT 0,
    "ReservedQuantity" INTEGER NOT NULL DEFAULT 0,
    "MinStockLevel" INTEGER DEFAULT 10,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "UpdatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "Reservations" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "OrderId" UUID NOT NULL,
    "ProductId" UUID NOT NULL,
    "Quantity" INTEGER NOT NULL,
    "Status" VARCHAR(20) DEFAULT 'reserved',
    "ExpiresAt" TIMESTAMP,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Analytics DB Schema
\c analytics_db;
CREATE TABLE IF NOT EXISTS "Events" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "EventType" VARCHAR(100) NOT NULL,
    "Source" VARCHAR(100) NOT NULL,
    "Payload" JSONB,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "Metrics" (
    "Id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "MetricName" VARCHAR(100) NOT NULL,
    "MetricValue" DECIMAL(15,4) NOT NULL,
    "Dimensions" JSONB,
    "Timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
