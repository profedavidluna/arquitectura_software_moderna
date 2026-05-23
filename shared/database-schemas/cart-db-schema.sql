-- ============================================
-- CART DATABASE SCHEMA (cart_db)
-- ============================================
-- Service: Cart Service
-- Purpose: Shopping cart management and abandoned cart recovery
-- Port: 5436
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
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

COMMENT ON TABLE carts IS 'Shopping carts with financial totals and expiration management';
COMMENT ON COLUMN carts.id IS 'Unique identifier for the cart';
COMMENT ON COLUMN carts.user_id IS 'Reference to the owning user (cross-service)';
COMMENT ON COLUMN carts.session_id IS 'Browser session ID for guest carts';
COMMENT ON COLUMN carts.status IS 'Cart lifecycle: ACTIVE → CONVERTED/ABANDONED/MERGED';
COMMENT ON COLUMN carts.item_count IS 'Total number of distinct items in the cart';
COMMENT ON COLUMN carts.subtotal IS 'Sum of all cart item subtotals';
COMMENT ON COLUMN carts.tax_amount IS 'Estimated tax amount';
COMMENT ON COLUMN carts.shipping_amount IS 'Estimated shipping cost';
COMMENT ON COLUMN carts.discount_amount IS 'Discount from applied coupon';
COMMENT ON COLUMN carts.total_amount IS 'Final estimated total: subtotal + tax + shipping - discount';
COMMENT ON COLUMN carts.coupon_code IS 'Applied coupon code (if any)';
COMMENT ON COLUMN carts.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN carts.created_at IS 'Cart creation timestamp';
COMMENT ON COLUMN carts.updated_at IS 'Last modification timestamp';
COMMENT ON COLUMN carts.expires_at IS 'Cart expiration timestamp (30 days from creation by default)';

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

COMMENT ON TABLE cart_items IS 'Individual items within a shopping cart';
COMMENT ON COLUMN cart_items.id IS 'Unique identifier for the cart item';
COMMENT ON COLUMN cart_items.cart_id IS 'Reference to the parent cart';
COMMENT ON COLUMN cart_items.product_id IS 'Reference to the product (cross-service)';
COMMENT ON COLUMN cart_items.product_name IS 'Product name at time of adding (denormalized for display)';
COMMENT ON COLUMN cart_items.product_sku IS 'Product SKU (denormalized)';
COMMENT ON COLUMN cart_items.product_image_url IS 'Product image URL (denormalized for display)';
COMMENT ON COLUMN cart_items.quantity IS 'Number of units in cart';
COMMENT ON COLUMN cart_items.unit_price IS 'Price per unit at time of adding';
COMMENT ON COLUMN cart_items.subtotal IS 'Line item total: quantity * unit_price';
COMMENT ON COLUMN cart_items.created_at IS 'When item was added to cart';
COMMENT ON COLUMN cart_items.updated_at IS 'Last modification (quantity change, etc.)';

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

COMMENT ON TABLE abandoned_carts IS 'Tracking table for abandoned cart recovery campaigns';
COMMENT ON COLUMN abandoned_carts.cart_id IS 'Reference to the abandoned cart';
COMMENT ON COLUMN abandoned_carts.user_id IS 'User who abandoned the cart';
COMMENT ON COLUMN abandoned_carts.total_amount IS 'Cart value at time of abandonment';
COMMENT ON COLUMN abandoned_carts.item_count IS 'Number of items in the abandoned cart';
COMMENT ON COLUMN abandoned_carts.abandoned_at IS 'When the cart was marked as abandoned';
COMMENT ON COLUMN abandoned_carts.recovery_email_sent IS 'Whether a recovery email was sent';
COMMENT ON COLUMN abandoned_carts.recovery_email_sent_at IS 'When the recovery email was sent';
COMMENT ON COLUMN abandoned_carts.recovery_email_opened IS 'Whether the recovery email was opened';
COMMENT ON COLUMN abandoned_carts.recovered IS 'Whether the cart was recovered (user returned and purchased)';
COMMENT ON COLUMN abandoned_carts.recovered_at IS 'When the cart was recovered';

-- ============================================
-- INDEXES
-- ============================================

-- Carts indexes
CREATE INDEX IF NOT EXISTS idx_carts_user_id ON carts(user_id);
CREATE INDEX IF NOT EXISTS idx_carts_session_id ON carts(session_id);
CREATE INDEX IF NOT EXISTS idx_carts_status ON carts(status);
CREATE INDEX IF NOT EXISTS idx_carts_expires_at ON carts(expires_at);
CREATE INDEX IF NOT EXISTS idx_carts_created_at ON carts(created_at);
CREATE INDEX IF NOT EXISTS idx_carts_updated_at ON carts(updated_at);

-- Cart items indexes
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_items(product_id);

-- Abandoned carts indexes
CREATE INDEX IF NOT EXISTS idx_abandoned_carts_user_id ON abandoned_carts(user_id);
CREATE INDEX IF NOT EXISTS idx_abandoned_carts_abandoned_at ON abandoned_carts(abandoned_at);
CREATE INDEX IF NOT EXISTS idx_abandoned_carts_recovery ON abandoned_carts(recovery_email_sent, recovered);

-- Composite indexes
CREATE INDEX IF NOT EXISTS idx_carts_user_active ON carts(user_id, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_carts_expiring ON carts(expires_at, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_abandoned_not_recovered ON abandoned_carts(abandoned_at, recovered) 
    WHERE recovered = false AND recovery_email_sent = false;

-- ============================================
-- CONSTRAINTS
-- ============================================

-- Subtotal must equal quantity * unit_price
ALTER TABLE cart_items ADD CONSTRAINT IF NOT EXISTS chk_cart_item_subtotal 
    CHECK (subtotal = quantity * unit_price);

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

CREATE TRIGGER trg_carts_updated_at 
    BEFORE UPDATE ON carts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_cart_items_updated_at 
    BEFORE UPDATE ON cart_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Mark cart as abandoned and create recovery record
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

CREATE TRIGGER trg_cart_abandoned 
    AFTER UPDATE OF status ON carts 
    FOR EACH ROW EXECUTE FUNCTION mark_cart_abandoned();

-- ============================================
-- VIEWS
-- ============================================

-- Active carts with item details
CREATE OR REPLACE VIEW active_carts AS
SELECT 
    c.id AS cart_id,
    c.user_id,
    c.item_count,
    c.subtotal,
    c.tax_amount,
    c.shipping_amount,
    c.discount_amount,
    c.total_amount,
    c.coupon_code,
    c.created_at,
    c.updated_at,
    c.expires_at,
    AGE(c.expires_at, CURRENT_TIMESTAMP) AS time_until_expiry
FROM carts c
WHERE c.status = 'ACTIVE' AND c.expires_at > CURRENT_TIMESTAMP;

COMMENT ON VIEW active_carts IS 'Currently active, non-expired carts';

-- Abandoned carts pending recovery
CREATE OR REPLACE VIEW abandoned_carts_pending_recovery AS
SELECT 
    ac.id,
    ac.cart_id,
    ac.user_id,
    ac.total_amount,
    ac.item_count,
    ac.abandoned_at,
    AGE(CURRENT_TIMESTAMP, ac.abandoned_at) AS time_since_abandoned,
    ac.recovery_email_sent,
    ac.recovery_email_sent_at
FROM abandoned_carts ac
WHERE ac.recovered = false
  AND (ac.recovery_email_sent = false 
       OR ac.recovery_email_sent_at < CURRENT_TIMESTAMP - INTERVAL '3 days')
ORDER BY ac.total_amount DESC;

COMMENT ON VIEW abandoned_carts_pending_recovery IS 'Abandoned carts eligible for recovery email, sorted by value';
