-- ============================================
-- ORDER DATABASE SCHEMA (order_db)
-- ============================================
-- Service: Order Service
-- Purpose: Order management, order items, and payment tracking
-- Port: 5434
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
-- ============================================

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(20) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED')),
    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal >= 0),
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    shipping_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (shipping_amount >= 0),
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    shipping_address JSONB NOT NULL,
    billing_address JSONB,
    notes TEXT,
    cancelled_reason TEXT,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE orders IS 'Master order records with financial totals and status tracking';
COMMENT ON COLUMN orders.id IS 'Unique identifier for the order (UUID v4)';
COMMENT ON COLUMN orders.order_number IS 'Human-readable order number (e.g., ORD-20261105-001)';
COMMENT ON COLUMN orders.user_id IS 'Reference to the user who placed the order (cross-service)';
COMMENT ON COLUMN orders.status IS 'Current order status in the lifecycle';
COMMENT ON COLUMN orders.subtotal IS 'Sum of all order items before tax and shipping';
COMMENT ON COLUMN orders.tax_amount IS 'Calculated tax amount';
COMMENT ON COLUMN orders.shipping_amount IS 'Shipping cost';
COMMENT ON COLUMN orders.discount_amount IS 'Total discount applied (coupons, promotions)';
COMMENT ON COLUMN orders.total_amount IS 'Final amount: subtotal + tax + shipping - discount';
COMMENT ON COLUMN orders.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN orders.shipping_address IS 'JSON object with shipping address details';
COMMENT ON COLUMN orders.billing_address IS 'JSON object with billing address (if different from shipping)';
COMMENT ON COLUMN orders.notes IS 'Customer notes or special instructions';
COMMENT ON COLUMN orders.cancelled_reason IS 'Reason for cancellation (if applicable)';
COMMENT ON COLUMN orders.shipped_at IS 'Timestamp when order was shipped';
COMMENT ON COLUMN orders.delivered_at IS 'Timestamp when order was delivered';
COMMENT ON COLUMN orders.cancelled_at IS 'Timestamp when order was cancelled';
COMMENT ON COLUMN orders.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN orders.updated_at IS 'Record last update timestamp';

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price > 0),
    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE order_items IS 'Individual line items within an order';
COMMENT ON COLUMN order_items.id IS 'Unique identifier for the order item';
COMMENT ON COLUMN order_items.order_id IS 'Reference to the parent order';
COMMENT ON COLUMN order_items.product_id IS 'Reference to the product (cross-service, denormalized)';
COMMENT ON COLUMN order_items.product_name IS 'Product name at time of purchase (denormalized)';
COMMENT ON COLUMN order_items.product_sku IS 'Product SKU at time of purchase (denormalized)';
COMMENT ON COLUMN order_items.quantity IS 'Number of units ordered';
COMMENT ON COLUMN order_items.unit_price IS 'Price per unit at time of purchase';
COMMENT ON COLUMN order_items.subtotal IS 'Line item total: quantity * unit_price';
COMMENT ON COLUMN order_items.created_at IS 'Record creation timestamp';

-- Payments Table (order-level payment tracking)
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255),
    gateway_response JSONB,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE payments IS 'Payment records associated with orders';
COMMENT ON COLUMN payments.id IS 'Unique identifier for the payment record';
COMMENT ON COLUMN payments.order_id IS 'Reference to the order being paid';
COMMENT ON COLUMN payments.amount IS 'Payment amount';
COMMENT ON COLUMN payments.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN payments.status IS 'Payment processing status';
COMMENT ON COLUMN payments.payment_method IS 'Method used (CREDIT_CARD, DEBIT_CARD, PAYPAL, etc.)';
COMMENT ON COLUMN payments.transaction_id IS 'External payment gateway transaction ID';
COMMENT ON COLUMN payments.gateway_response IS 'Raw response from payment gateway';
COMMENT ON COLUMN payments.paid_at IS 'Timestamp when payment was confirmed';
COMMENT ON COLUMN payments.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN payments.updated_at IS 'Record last update timestamp';

-- Order Status History Table
CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID,
    reason TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE order_status_history IS 'Complete audit trail of order status transitions';
COMMENT ON COLUMN order_status_history.old_status IS 'Previous order status (NULL for initial creation)';
COMMENT ON COLUMN order_status_history.new_status IS 'New order status after transition';
COMMENT ON COLUMN order_status_history.changed_by IS 'User or system that triggered the change';
COMMENT ON COLUMN order_status_history.reason IS 'Reason for the status change';
COMMENT ON COLUMN order_status_history.metadata IS 'Additional context (tracking number, carrier, etc.)';

-- ============================================
-- INDEXES
-- ============================================

-- Orders indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at);

-- Order items indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- Payments indexes
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

-- Status history indexes
CREATE INDEX IF NOT EXISTS idx_status_history_order_id ON order_status_history(order_id);
CREATE INDEX IF NOT EXISTS idx_status_history_created_at ON order_status_history(created_at);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_pending ON orders(status, created_at) WHERE status IN ('PENDING', 'CONFIRMED', 'PROCESSING');

-- ============================================
-- CONSTRAINTS
-- ============================================

-- Subtotal must equal quantity * unit_price
ALTER TABLE order_items ADD CONSTRAINT IF NOT EXISTS chk_subtotal_correct 
    CHECK (subtotal = quantity * unit_price);

-- Order number format
ALTER TABLE orders ADD CONSTRAINT IF NOT EXISTS chk_order_number_format 
    CHECK (order_number ~ '^ORD-[0-9]{8}-[0-9]{3,}$');

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

CREATE TRIGGER trg_orders_updated_at 
    BEFORE UPDATE ON orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at 
    BEFORE UPDATE ON payments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Record order status changes automatically
CREATE OR REPLACE FUNCTION record_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_status_history (order_id, old_status, new_status, created_at)
        VALUES (NEW.id, OLD.status, NEW.status, CURRENT_TIMESTAMP);
        
        -- Update timestamp fields based on status
        IF NEW.status = 'SHIPPED' THEN
            NEW.shipped_at = CURRENT_TIMESTAMP;
        ELSIF NEW.status = 'DELIVERED' THEN
            NEW.delivered_at = CURRENT_TIMESTAMP;
        ELSIF NEW.status = 'CANCELLED' THEN
            NEW.cancelled_at = CURRENT_TIMESTAMP;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_status_change 
    BEFORE UPDATE OF status ON orders 
    FOR EACH ROW EXECUTE FUNCTION record_order_status_change();

-- ============================================
-- VIEWS
-- ============================================

-- Order summary with item count
CREATE OR REPLACE VIEW order_summary AS
SELECT 
    o.id AS order_id,
    o.order_number,
    o.user_id,
    o.status,
    o.subtotal,
    o.tax_amount,
    o.shipping_amount,
    o.discount_amount,
    o.total_amount,
    o.currency,
    o.created_at,
    o.updated_at,
    COUNT(oi.id) AS item_count,
    SUM(oi.quantity) AS total_quantity,
    p.status AS payment_status
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
LEFT JOIN payments p ON o.id = p.order_id
GROUP BY o.id, o.order_number, o.user_id, o.status, o.subtotal, o.tax_amount, 
         o.shipping_amount, o.discount_amount, o.total_amount, o.currency, 
         o.created_at, o.updated_at, p.status;

COMMENT ON VIEW order_summary IS 'Order overview with item counts and payment status';

-- Pending orders requiring attention
CREATE OR REPLACE VIEW pending_orders AS
SELECT 
    o.id,
    o.order_number,
    o.user_id,
    o.status,
    o.total_amount,
    o.created_at,
    AGE(CURRENT_TIMESTAMP, o.created_at) AS age,
    p.status AS payment_status
FROM orders o
LEFT JOIN payments p ON o.id = p.order_id
WHERE o.status IN ('PENDING', 'CONFIRMED')
ORDER BY o.created_at ASC;

COMMENT ON VIEW pending_orders IS 'Orders awaiting processing, sorted by age';

-- ============================================
-- SEQUENCES
-- ============================================

-- Sequence for order number generation
CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 1 INCREMENT BY 1;
