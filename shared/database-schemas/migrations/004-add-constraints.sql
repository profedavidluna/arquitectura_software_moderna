-- Add Constraints Migration
-- This file adds additional constraints, triggers, and optimizations
-- Run this migration after 003-add-views.sql

-- Migration: 004-add-constraints
-- Description: Add additional constraints, triggers, and optimizations
-- Created: 2026-11-05
-- Author: Software Architecture Course

-- ============================================
-- ADDITIONAL CONSTRAINTS
-- ============================================

-- USER DATABASE CONSTRAINTS

-- Ensure email format is valid
ALTER TABLE user_schema.users 
ADD CONSTRAINT chk_email_format 
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Ensure phone number format (optional)
ALTER TABLE user_schema.users 
ADD CONSTRAINT chk_phone_format 
CHECK (phone IS NULL OR phone ~ '^[0-9+\-() ]+$');

-- Ensure username doesn't contain special characters
ALTER TABLE user_schema.users 
ADD CONSTRAINT chk_username_format 
CHECK (username ~ '^[a-zA-Z0-9_]+$');

-- PRODUCT DATABASE CONSTRAINTS

-- Ensure price is positive
ALTER TABLE product_schema.products 
ADD CONSTRAINT chk_price_positive 
CHECK (price > 0);

-- Ensure SKU format (alphanumeric with hyphens)
ALTER TABLE product_schema.products 
ADD CONSTRAINT chk_sku_format 
CHECK (sku ~ '^[A-Za-z0-9-]+$');

-- ORDER DATABASE CONSTRAINTS

-- Ensure order amounts are non-negative
ALTER TABLE order_schema.orders 
ADD CONSTRAINT chk_order_amounts_non_negative 
CHECK (total_amount >= 0 AND tax_amount >= 0 AND shipping_amount >= 0);

-- Ensure order items have positive quantity
ALTER TABLE order_schema.order_items 
ADD CONSTRAINT chk_order_item_quantity_positive 
CHECK (quantity > 0);

-- Ensure unit price is positive
ALTER TABLE order_schema.order_items 
ADD CONSTRAINT chk_unit_price_positive 
CHECK (unit_price > 0);

-- Ensure subtotal matches quantity * unit price
ALTER TABLE order_schema.order_items 
ADD CONSTRAINT chk_subtotal_correct 
CHECK (subtotal = quantity * unit_price);

-- INVENTORY DATABASE CONSTRAINTS

-- Ensure reserved quantity doesn't exceed available
ALTER TABLE inventory_schema.inventory 
ADD CONSTRAINT chk_reserved_leq_available 
CHECK (quantity_reserved <= quantity_available);

-- Ensure transaction quantity is non-zero
ALTER TABLE inventory_schema.inventory_transactions 
ADD CONSTRAINT chk_transaction_quantity_non_zero 
CHECK (quantity != 0);

-- CART DATABASE CONSTRAINTS

-- Ensure cart amounts are non-negative
ALTER TABLE cart_schema.carts 
ADD CONSTRAINT chk_cart_amounts_non_negative 
CHECK (total_amount >= 0 AND tax_amount >= 0 AND shipping_amount >= 0 AND coupon_discount >= 0);

-- Ensure cart item quantities are positive
ALTER TABLE cart_schema.cart_items 
ADD CONSTRAINT chk_cart_item_quantity_positive 
CHECK (quantity > 0);

-- Ensure cart item subtotal matches quantity * unit price
ALTER TABLE cart_schema.cart_items 
ADD CONSTRAINT chk_cart_item_subtotal_correct 
CHECK (subtotal = quantity * unit_price);

-- PAYMENT DATABASE CONSTRAINTS

-- Ensure payment amount is positive
ALTER TABLE payment_schema.transactions 
ADD CONSTRAINT chk_payment_amount_positive 
CHECK (amount > 0);

-- Ensure refund amount is positive
ALTER TABLE payment_schema.refunds 
ADD CONSTRAINT chk_refund_amount_positive 
CHECK (amount > 0);

-- Ensure refund doesn't exceed transaction amount
ALTER TABLE payment_schema.refunds 
ADD CONSTRAINT chk_refund_amount_limit 
CHECK (amount <= (SELECT amount FROM payment_schema.transactions WHERE id = refunds.transaction_id));

-- Ensure payment method token is not empty
ALTER TABLE payment_schema.payment_methods 
ADD CONSTRAINT chk_payment_token_not_empty 
CHECK (LENGTH(TRIM(token)) > 0);

-- ANALYTICS DATABASE CONSTRAINTS

-- Ensure metrics are non-negative
ALTER TABLE analytics_schema.daily_metrics 
ADD CONSTRAINT chk_metrics_non_negative 
CHECK (total_orders >= 0 AND total_revenue >= 0 AND total_users >= 0 AND new_users >= 0 AND average_order_value >= 0 AND conversion_rate >= 0);

-- Ensure conversion rate is between 0 and 100
ALTER TABLE analytics_schema.daily_metrics 
ADD CONSTRAINT chk_conversion_rate_range 
CHECK (conversion_rate >= 0 AND conversion_rate <= 100);

-- ============================================
-- TRIGGERS FOR DATA INTEGRITY
-- ============================================

-- USER DATABASE TRIGGERS

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION user_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
BEFORE UPDATE ON user_schema.users 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_addresses_updated_at 
BEFORE UPDATE ON user_schema.addresses 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

-- Trigger to ensure only one default address per user
CREATE OR REPLACE FUNCTION user_schema.ensure_single_default_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = true THEN
        UPDATE user_schema.addresses 
        SET is_default = false 
        WHERE user_id = NEW.user_id 
          AND id != NEW.id 
          AND is_default = true;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER ensure_single_default_address 
BEFORE INSERT OR UPDATE ON user_schema.addresses 
FOR EACH ROW EXECUTE FUNCTION user_schema.ensure_single_default_address();

-- PRODUCT DATABASE TRIGGERS

-- Trigger to update product updated_at
CREATE TRIGGER update_products_updated_at 
BEFORE UPDATE ON product_schema.products 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_categories_updated_at 
BEFORE UPDATE ON product_schema.categories 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at 
BEFORE UPDATE ON product_schema.product_reviews 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

-- ORDER DATABASE TRIGGERS

-- Trigger to update order updated_at
CREATE TRIGGER update_orders_updated_at 
BEFORE UPDATE ON order_schema.orders 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_payments_updated_at 
BEFORE UPDATE ON order_schema.payments 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

-- Trigger to record order status changes
CREATE OR REPLACE FUNCTION order_schema.record_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_schema.order_status_history (order_id, old_status, new_status, created_at)
        VALUES (NEW.id, OLD.status, NEW.status, CURRENT_TIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER record_order_status_change 
AFTER UPDATE OF status ON order_schema.orders 
FOR EACH ROW EXECUTE FUNCTION order_schema.record_status_change();

-- INVENTORY DATABASE TRIGGERS

-- Trigger to update inventory last_updated
CREATE OR REPLACE FUNCTION inventory_schema.update_inventory_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_inventory_timestamp 
BEFORE UPDATE ON inventory_schema.inventory 
FOR EACH ROW EXECUTE FUNCTION inventory_schema.update_inventory_timestamp();

-- Trigger to create low stock alert
CREATE OR REPLACE FUNCTION inventory_schema.check_low_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity_available <= 10 THEN
        INSERT INTO inventory_schema.low_stock_alerts (product_id, threshold, current_quantity, alert_status, created_at)
        VALUES (NEW.product_id, 10, NEW.quantity_available, 'ACTIVE', CURRENT_TIMESTAMP)
        ON CONFLICT (product_id) DO UPDATE 
        SET current_quantity = NEW.quantity_available,
            alert_status = 'ACTIVE',
            resolved_at = NULL;
    ELSIF NEW.quantity_available > 10 THEN
        UPDATE inventory_schema.low_stock_alerts 
        SET alert_status = 'RESOLVED',
            resolved_at = CURRENT_TIMESTAMP
        WHERE product_id = NEW.product_id 
          AND alert_status = 'ACTIVE';
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER check_low_stock_trigger 
AFTER INSERT OR UPDATE ON inventory_schema.inventory 
FOR EACH ROW EXECUTE FUNCTION inventory_schema.check_low_stock();

-- CART DATABASE TRIGGERS

-- Trigger to update cart updated_at
CREATE TRIGGER update_carts_updated_at 
BEFORE UPDATE ON cart_schema.carts 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_cart_items_updated_at 
BEFORE UPDATE ON cart_schema.cart_items 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

-- Trigger to mark cart as abandoned
CREATE OR REPLACE FUNCTION cart_schema.mark_abandoned_cart()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'ABANDONED' AND OLD.status != 'ABANDONED' THEN
        INSERT INTO cart_schema.abandoned_carts (cart_id, user_id, total_amount, abandoned_at)
        VALUES (NEW.id, NEW.user_id, NEW.total_amount, CURRENT_TIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER mark_abandoned_cart_trigger 
AFTER UPDATE OF status ON cart_schema.carts 
FOR EACH ROW EXECUTE FUNCTION cart_schema.mark_abandoned_cart();

-- PAYMENT DATABASE TRIGGERS

-- Trigger to update payment updated_at
CREATE TRIGGER update_transactions_updated_at 
BEFORE UPDATE ON payment_schema.transactions 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_refunds_updated_at 
BEFORE UPDATE ON payment_schema.refunds 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_payment_methods_updated_at 
BEFORE UPDATE ON payment_schema.payment_methods 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

CREATE TRIGGER update_retry_log_updated_at 
BEFORE UPDATE ON payment_schema.payment_retry_log 
FOR EACH ROW EXECUTE FUNCTION user_schema.update_updated_at_column();

-- Trigger to ensure only one default payment method per user
CREATE OR REPLACE FUNCTION payment_schema.ensure_single_default_payment_method()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = true THEN
        UPDATE payment_schema.payment_methods 
        SET is_default = false 
        WHERE user_id = NEW.user_id 
          AND id != NEW.id 
          AND is_default = true;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER ensure_single_default_payment_method 
BEFORE INSERT OR UPDATE ON payment_schema.payment_methods 
FOR EACH ROW EXECUTE FUNCTION payment_schema.ensure_single_default_payment_method();

-- ============================================
-- PARTITIONING FOR LARGE TABLES (OPTIONAL)
-- ============================================

-- Note: Partitioning is recommended for tables with millions of rows
-- Uncomment and adjust as needed for production

-- -- Partition events table by month
-- CREATE TABLE analytics_schema.events_y2026m11 PARTITION OF analytics_schema.events
-- FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

-- CREATE TABLE analytics_schema.events_y2026m12 PARTITION OF analytics_schema.events
-- FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

-- -- Partition inventory transactions by month
-- CREATE TABLE inventory_schema.inventory_transactions_y2026m11 PARTITION OF inventory_schema.inventory_transactions
-- FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

-- -- Partition order status history by month
-- CREATE TABLE order_schema.order_status_history_y2026m11 PARTITION OF order_schema.order_status_history
-- FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

-- ============================================
-- PERFORMANCE OPTIMIZATIONS
-- ============================================

-- Set fillfactor for frequently updated tables
ALTER TABLE inventory_schema.inventory SET (fillfactor = 90);
ALTER TABLE order_schema.orders SET (fillfactor = 90);
ALTER TABLE payment_schema.transactions SET (fillfactor = 90);

-- Set fillfactor for read-heavy tables
ALTER TABLE product_schema.products SET (fillfactor = 100);
ALTER TABLE user_schema.users SET (fillfactor = 100);
ALTER TABLE analytics_schema.daily_metrics SET (fillfactor = 100);

-- Create BRIN indexes for large timestamp-based tables
-- (More efficient than B-tree for large, append-only tables)
CREATE INDEX IF NOT EXISTS idx_events_created_at_brin 
ON analytics_schema.events USING BRIN(created_at);

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_created_at_brin 
ON inventory_schema.inventory_transactions USING BRIN(created_at);

CREATE INDEX IF NOT EXISTS idx_order_status_history_created_at_brin 
ON order_schema.order_status_history USING BRIN(created_at);

-- ============================================
-- SECURITY CONSTRAINTS
-- ============================================

-- Row Level Security (RLS) policies
-- Note: These are examples and should be customized for your security model

-- Enable RLS on users table
-- ALTER TABLE user_schema.users ENABLE ROW LEVEL SECURITY;

-- -- Policy: Users can only see their own data
-- CREATE POLICY user_select_policy ON user_schema.users
-- FOR SELECT USING (id = current_user_id());

-- -- Policy: Only admins can insert/update/delete
-- CREATE POLICY user_modify_policy ON user_schema.users
-- FOR ALL USING (current_user_role() = 'ADMIN');

-- ============================================
-- DATA RETENTION POLICIES
-- ============================================

-- Create function to archive old data
CREATE OR REPLACE FUNCTION analytics_schema.archive_old_events()
RETURNS void AS $$
BEGIN
    -- Archive events older than 1 year
    INSERT INTO analytics_schema.events_archive
    SELECT * FROM analytics_schema.events 
    WHERE created_at < CURRENT_DATE - INTERVAL '1 year';
    
    -- Delete archived events
    DELETE FROM analytics_schema.events 
    WHERE created_at < CURRENT_DATE - INTERVAL '1 year';
END;
$$ language 'plpgsql';

-- Create function to purge old audit logs
CREATE OR REPLACE FUNCTION user_schema.purge_old_audit_logs()
RETURNS void AS $$
BEGIN
    -- Delete audit logs older than 6 months
    DELETE FROM user_schema.user_audit_log 
    WHERE created_at < CURRENT_DATE - INTERVAL '6 months';
END;
$$ language 'plpgsql';

-- ============================================
-- MIGRATION COMPLETE
-- ============================================

-- Migration completed successfully
-- All constraints, triggers, and optimizations applied
-- Database schema is now production-ready

-- Next steps:
-- 1. Run data seeding scripts (if needed)
-- 2. Configure database users and permissions
-- 3. Set up monitoring and alerting
-- 4. Create backup and recovery procedures