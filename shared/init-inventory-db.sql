-- Inventory Database Initialization Script
-- This script is executed when the inventory-db container starts

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID UNIQUE NOT NULL,
    quantity_available INT NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    quantity_reserved INT NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inventory Transactions Table
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL CHECK (transaction_type IN ('RESERVE', 'RELEASE', 'DEPLETE', 'RESTOCK', 'ADJUSTMENT')),
    quantity INT NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(50),
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Low Stock Alerts Table
CREATE TABLE IF NOT EXISTS low_stock_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL,
    threshold INT NOT NULL,
    current_quantity INT NOT NULL,
    alert_status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (alert_status IN ('ACTIVE', 'RESOLVED', 'IGNORED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_transactions_product_id ON inventory_transactions(product_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON inventory_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON inventory_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON inventory_transactions(reference_id, reference_type);
CREATE INDEX IF NOT EXISTS idx_low_stock_product_id ON low_stock_alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_low_stock_status ON low_stock_alerts(alert_status);

-- View for current inventory status
CREATE OR REPLACE VIEW inventory_status AS
SELECT 
    i.product_id,
    i.quantity_available,
    i.quantity_reserved,
    (i.quantity_available + i.quantity_reserved) as total_quantity,
    i.last_updated
FROM inventory i;
