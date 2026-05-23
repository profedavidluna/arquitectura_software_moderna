-- ============================================
-- Migration: V1__inventory_db_schema.sql
-- Database: inventory_db
-- Description: Initial schema for Inventory Service
-- Created: 2026-11-05
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- SCHEMA CREATION
-- ============================================

-- Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID UNIQUE NOT NULL,
    sku VARCHAR(100) NOT NULL,
    warehouse_location VARCHAR(100),
    quantity_available INT NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    quantity_reserved INT NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    reorder_point INT NOT NULL DEFAULT 10 CHECK (reorder_point >= 0),
    reorder_quantity INT NOT NULL DEFAULT 50 CHECK (reorder_quantity > 0),
    max_quantity INT CHECK (max_quantity IS NULL OR max_quantity > 0),
    last_restocked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inventory Transactions Table
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL 
        CHECK (transaction_type IN ('RESERVE', 'RELEASE', 'DEPLETE', 'RESTOCK', 'ADJUSTMENT', 'RETURN')),
    quantity INT NOT NULL CHECK (quantity != 0),
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(50) CHECK (reference_type IN ('ORDER', 'RETURN', 'MANUAL', 'SYSTEM')),
    reason TEXT,
    performed_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Low Stock Alerts Table
CREATE TABLE IF NOT EXISTS low_stock_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    threshold INT NOT NULL,
    current_quantity INT NOT NULL,
    alert_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (alert_status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'IGNORED')),
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_sku ON inventory(sku);
CREATE INDEX idx_inventory_quantity_available ON inventory(quantity_available);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_location);
CREATE INDEX idx_inventory_low_stock ON inventory(quantity_available, reorder_point) 
    WHERE quantity_available <= reorder_point;

CREATE INDEX idx_inv_txn_product_id ON inventory_transactions(product_id);
CREATE INDEX idx_inv_txn_type ON inventory_transactions(transaction_type);
CREATE INDEX idx_inv_txn_created_at ON inventory_transactions(created_at);
CREATE INDEX idx_inv_txn_reference ON inventory_transactions(reference_id, reference_type);
CREATE INDEX idx_inv_txn_product_date ON inventory_transactions(product_id, created_at DESC);
CREATE INDEX idx_inv_txn_created_at_brin ON inventory_transactions USING BRIN(created_at);

CREATE INDEX idx_low_stock_product_id ON low_stock_alerts(product_id);
CREATE INDEX idx_low_stock_status ON low_stock_alerts(alert_status);
CREATE INDEX idx_low_stock_active ON low_stock_alerts(alert_status, created_at) WHERE alert_status = 'ACTIVE';

-- ============================================
-- TRIGGERS
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inventory_updated_at BEFORE UPDATE ON inventory 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION check_low_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity_available <= NEW.reorder_point THEN
        INSERT INTO low_stock_alerts (product_id, sku, threshold, current_quantity, alert_status)
        SELECT NEW.product_id, NEW.sku, NEW.reorder_point, NEW.quantity_available, 'ACTIVE'
        WHERE NOT EXISTS (
            SELECT 1 FROM low_stock_alerts 
            WHERE product_id = NEW.product_id AND alert_status = 'ACTIVE'
        );
    ELSIF NEW.quantity_available > NEW.reorder_point THEN
        UPDATE low_stock_alerts SET alert_status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP
        WHERE product_id = NEW.product_id AND alert_status IN ('ACTIVE', 'ACKNOWLEDGED');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_low_stock AFTER INSERT OR UPDATE OF quantity_available ON inventory 
    FOR EACH ROW EXECUTE FUNCTION check_low_stock();

-- ============================================
-- VIEWS
-- ============================================

CREATE OR REPLACE VIEW inventory_status AS
SELECT 
    i.id, i.product_id, i.sku, i.warehouse_location,
    i.quantity_available, i.quantity_reserved,
    (i.quantity_available + i.quantity_reserved) AS total_quantity,
    i.reorder_point, i.reorder_quantity,
    CASE 
        WHEN i.quantity_available <= 0 THEN 'OUT_OF_STOCK'
        WHEN i.quantity_available <= i.reorder_point THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status,
    i.last_restocked_at, i.updated_at
FROM inventory i;

CREATE OR REPLACE VIEW active_alerts AS
SELECT 
    lsa.id, lsa.product_id, lsa.sku, lsa.threshold, lsa.current_quantity,
    lsa.alert_status, lsa.created_at, AGE(CURRENT_TIMESTAMP, lsa.created_at) AS alert_age,
    i.quantity_available AS current_available
FROM low_stock_alerts lsa
LEFT JOIN inventory i ON lsa.product_id = i.product_id
WHERE lsa.alert_status IN ('ACTIVE', 'ACKNOWLEDGED')
ORDER BY lsa.created_at ASC;

-- ============================================
-- MIGRATION TRACKING
-- ============================================

CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_migrations (migration_name) VALUES ('V1__inventory_db_schema');
