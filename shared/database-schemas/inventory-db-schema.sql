-- ============================================
-- INVENTORY DATABASE SCHEMA (inventory_db)
-- ============================================
-- Service: Inventory Service
-- Purpose: Stock management, reservations, and inventory transactions
-- Port: 5435
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
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

COMMENT ON TABLE inventory IS 'Current stock levels for each product with reorder thresholds';
COMMENT ON COLUMN inventory.id IS 'Unique identifier for the inventory record';
COMMENT ON COLUMN inventory.product_id IS 'Reference to the product (cross-service, one record per product)';
COMMENT ON COLUMN inventory.sku IS 'Stock Keeping Unit identifier (denormalized from product service)';
COMMENT ON COLUMN inventory.warehouse_location IS 'Physical location in warehouse (aisle-shelf-bin)';
COMMENT ON COLUMN inventory.quantity_available IS 'Units available for sale (not reserved)';
COMMENT ON COLUMN inventory.quantity_reserved IS 'Units reserved for pending orders';
COMMENT ON COLUMN inventory.reorder_point IS 'Threshold below which a restock alert is triggered';
COMMENT ON COLUMN inventory.reorder_quantity IS 'Suggested quantity to reorder';
COMMENT ON COLUMN inventory.max_quantity IS 'Maximum stock capacity (optional)';
COMMENT ON COLUMN inventory.last_restocked_at IS 'Timestamp of the last restock event';
COMMENT ON COLUMN inventory.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN inventory.updated_at IS 'Record last update timestamp';

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

COMMENT ON TABLE inventory_transactions IS 'Complete audit trail of all inventory changes';
COMMENT ON COLUMN inventory_transactions.id IS 'Unique identifier for the transaction';
COMMENT ON COLUMN inventory_transactions.product_id IS 'Reference to the product affected';
COMMENT ON COLUMN inventory_transactions.transaction_type IS 'Type: RESERVE, RELEASE, DEPLETE, RESTOCK, ADJUSTMENT, RETURN';
COMMENT ON COLUMN inventory_transactions.quantity IS 'Quantity changed (positive for additions, negative for removals)';
COMMENT ON COLUMN inventory_transactions.quantity_before IS 'Available quantity before the transaction';
COMMENT ON COLUMN inventory_transactions.quantity_after IS 'Available quantity after the transaction';
COMMENT ON COLUMN inventory_transactions.reference_id IS 'ID of the related entity (order_id, return_id, etc.)';
COMMENT ON COLUMN inventory_transactions.reference_type IS 'Type of the referenced entity';
COMMENT ON COLUMN inventory_transactions.reason IS 'Human-readable reason for the transaction';
COMMENT ON COLUMN inventory_transactions.performed_by IS 'User who performed the action (NULL for system actions)';
COMMENT ON COLUMN inventory_transactions.created_at IS 'Transaction timestamp';

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

COMMENT ON TABLE low_stock_alerts IS 'Alerts generated when inventory falls below reorder point';
COMMENT ON COLUMN low_stock_alerts.product_id IS 'Reference to the product with low stock';
COMMENT ON COLUMN low_stock_alerts.sku IS 'Product SKU for quick identification';
COMMENT ON COLUMN low_stock_alerts.threshold IS 'The reorder point that was breached';
COMMENT ON COLUMN low_stock_alerts.current_quantity IS 'Quantity at the time the alert was created';
COMMENT ON COLUMN low_stock_alerts.alert_status IS 'Alert lifecycle: ACTIVE → ACKNOWLEDGED → RESOLVED/IGNORED';
COMMENT ON COLUMN low_stock_alerts.acknowledged_by IS 'User who acknowledged the alert';
COMMENT ON COLUMN low_stock_alerts.acknowledged_at IS 'Timestamp when alert was acknowledged';
COMMENT ON COLUMN low_stock_alerts.resolved_at IS 'Timestamp when stock was replenished';

-- ============================================
-- INDEXES
-- ============================================

-- Inventory indexes
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sku ON inventory(sku);
CREATE INDEX IF NOT EXISTS idx_inventory_quantity_available ON inventory(quantity_available);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse ON inventory(warehouse_location);
CREATE INDEX IF NOT EXISTS idx_inventory_updated_at ON inventory(updated_at);

-- Inventory transactions indexes
CREATE INDEX IF NOT EXISTS idx_inv_txn_product_id ON inventory_transactions(product_id);
CREATE INDEX IF NOT EXISTS idx_inv_txn_type ON inventory_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_inv_txn_created_at ON inventory_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_inv_txn_reference ON inventory_transactions(reference_id, reference_type);

-- Low stock alerts indexes
CREATE INDEX IF NOT EXISTS idx_low_stock_product_id ON low_stock_alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_low_stock_status ON low_stock_alerts(alert_status);
CREATE INDEX IF NOT EXISTS idx_low_stock_created_at ON low_stock_alerts(created_at);

-- Composite indexes
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock ON inventory(quantity_available, reorder_point) 
    WHERE quantity_available <= reorder_point;
CREATE INDEX IF NOT EXISTS idx_inv_txn_product_date ON inventory_transactions(product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_low_stock_active ON low_stock_alerts(alert_status, created_at) 
    WHERE alert_status = 'ACTIVE';

-- BRIN index for time-series transaction data
CREATE INDEX IF NOT EXISTS idx_inv_txn_created_at_brin ON inventory_transactions USING BRIN(created_at);

-- ============================================
-- TRIGGERS
-- ============================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inventory_updated_at 
    BEFORE UPDATE ON inventory 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Check and create low stock alert on inventory update
CREATE OR REPLACE FUNCTION check_low_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity_available <= NEW.reorder_point THEN
        -- Create alert if none exists for this product
        INSERT INTO low_stock_alerts (product_id, sku, threshold, current_quantity, alert_status)
        SELECT NEW.product_id, NEW.sku, NEW.reorder_point, NEW.quantity_available, 'ACTIVE'
        WHERE NOT EXISTS (
            SELECT 1 FROM low_stock_alerts 
            WHERE product_id = NEW.product_id AND alert_status = 'ACTIVE'
        );
    ELSIF NEW.quantity_available > NEW.reorder_point THEN
        -- Resolve active alerts when stock is replenished
        UPDATE low_stock_alerts 
        SET alert_status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP
        WHERE product_id = NEW.product_id AND alert_status IN ('ACTIVE', 'ACKNOWLEDGED');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_low_stock 
    AFTER INSERT OR UPDATE OF quantity_available ON inventory 
    FOR EACH ROW EXECUTE FUNCTION check_low_stock();

-- ============================================
-- VIEWS
-- ============================================

-- Current inventory status
CREATE OR REPLACE VIEW inventory_status AS
SELECT 
    i.id,
    i.product_id,
    i.sku,
    i.warehouse_location,
    i.quantity_available,
    i.quantity_reserved,
    (i.quantity_available + i.quantity_reserved) AS total_quantity,
    i.reorder_point,
    i.reorder_quantity,
    CASE 
        WHEN i.quantity_available <= 0 THEN 'OUT_OF_STOCK'
        WHEN i.quantity_available <= i.reorder_point THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status,
    i.last_restocked_at,
    i.updated_at
FROM inventory i;

COMMENT ON VIEW inventory_status IS 'Current inventory levels with computed stock status';

-- Transaction summary by product
CREATE OR REPLACE VIEW transaction_summary AS
SELECT 
    product_id,
    transaction_type,
    COUNT(*) AS transaction_count,
    SUM(ABS(quantity)) AS total_quantity,
    MIN(created_at) AS first_transaction,
    MAX(created_at) AS last_transaction
FROM inventory_transactions
GROUP BY product_id, transaction_type
ORDER BY product_id, transaction_type;

COMMENT ON VIEW transaction_summary IS 'Aggregated transaction statistics per product and type';

-- Active low stock alerts
CREATE OR REPLACE VIEW active_alerts AS
SELECT 
    lsa.id,
    lsa.product_id,
    lsa.sku,
    lsa.threshold,
    lsa.current_quantity,
    lsa.alert_status,
    lsa.created_at,
    AGE(CURRENT_TIMESTAMP, lsa.created_at) AS alert_age,
    i.quantity_available AS current_available
FROM low_stock_alerts lsa
LEFT JOIN inventory i ON lsa.product_id = i.product_id
WHERE lsa.alert_status IN ('ACTIVE', 'ACKNOWLEDGED')
ORDER BY lsa.created_at ASC;

COMMENT ON VIEW active_alerts IS 'Active and acknowledged low stock alerts with current quantities';
