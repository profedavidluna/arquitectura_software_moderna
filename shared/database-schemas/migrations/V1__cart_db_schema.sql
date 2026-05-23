-- ============================================
-- Migration: V1__cart_db_schema.sql
-- Database: cart_db
-- Description: Initial schema for Cart Service
-- Created: 2026-11-05
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- SCHEMA CREATION
-- ============================================

-- Carts Table
CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    session_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (status IN ('ACTIVE', 'ABANDONED', 'CONVERTED', 'MERGED')),
    item_count INT NOT NULL DEFAULT 0 CHECK (item_count >= 0),
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    shipping_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (shipping_amount >= 0),
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    coupon_code VARCHAR(50),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days')
);

-- Cart Items Table
CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    product_image_url VARCHAR(500),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price > 0),
    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cart_id, product_id)
);

-- Abandoned Carts Table
CREATE TABLE IF NOT EXISTS abandoned_carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    item_count INT NOT NULL DEFAULT 0,
    abandoned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recovery_email_sent BOOLEAN NOT NULL DEFAULT false,
    recovery_email_sent_at TIMESTAMP,
    recovery_email_opened BOOLEAN NOT NULL DEFAULT false,
    recovered BOOLEAN NOT NULL DEFAULT false,
    recovered_at TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_session_id ON carts(session_id);
CREATE INDEX idx_carts_status ON carts(status);
CREATE INDEX idx_carts_expires_at ON carts(expires_at);
CREATE INDEX idx_carts_user_active ON carts(user_id, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_carts_expiring ON carts(expires_at, status) WHERE status = 'ACTIVE';

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);

CREATE INDEX idx_abandoned_carts_user_id ON abandoned_carts(user_id);
CREATE INDEX idx_abandoned_carts_abandoned_at ON abandoned_carts(abandoned_at);
CREATE INDEX idx_abandoned_not_recovered ON abandoned_carts(abandoned_at, recovered) 
    WHERE recovered = false AND recovery_email_sent = false;

-- ============================================
-- CONSTRAINTS
-- ============================================

ALTER TABLE cart_items ADD CONSTRAINT chk_cart_item_subtotal CHECK (subtotal = quantity * unit_price);

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

CREATE TRIGGER trg_carts_updated_at BEFORE UPDATE ON carts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_cart_items_updated_at BEFORE UPDATE ON cart_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION mark_cart_abandoned()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'ABANDONED' AND OLD.status = 'ACTIVE' THEN
        INSERT INTO abandoned_carts (cart_id, user_id, total_amount, item_count, abandoned_at)
        VALUES (NEW.id, NEW.user_id, NEW.total_amount, NEW.item_count, CURRENT_TIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cart_abandoned AFTER UPDATE OF status ON carts 
    FOR EACH ROW EXECUTE FUNCTION mark_cart_abandoned();

-- ============================================
-- VIEWS
-- ============================================

CREATE OR REPLACE VIEW active_carts AS
SELECT 
    c.id AS cart_id, c.user_id, c.item_count, c.subtotal, c.tax_amount,
    c.shipping_amount, c.discount_amount, c.total_amount, c.coupon_code,
    c.created_at, c.updated_at, c.expires_at,
    AGE(c.expires_at, CURRENT_TIMESTAMP) AS time_until_expiry
FROM carts c
WHERE c.status = 'ACTIVE' AND c.expires_at > CURRENT_TIMESTAMP;

CREATE OR REPLACE VIEW abandoned_carts_pending_recovery AS
SELECT 
    ac.id, ac.cart_id, ac.user_id, ac.total_amount, ac.item_count,
    ac.abandoned_at, AGE(CURRENT_TIMESTAMP, ac.abandoned_at) AS time_since_abandoned,
    ac.recovery_email_sent, ac.recovery_email_sent_at
FROM abandoned_carts ac
WHERE ac.recovered = false
  AND (ac.recovery_email_sent = false 
       OR ac.recovery_email_sent_at < CURRENT_TIMESTAMP - INTERVAL '3 days')
ORDER BY ac.total_amount DESC;

-- ============================================
-- MIGRATION TRACKING
-- ============================================

CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_migrations (migration_name) VALUES ('V1__cart_db_schema');
