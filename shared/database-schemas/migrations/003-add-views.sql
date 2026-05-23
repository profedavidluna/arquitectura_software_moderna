-- Add Views Migration
-- This file creates views for common queries and reporting
-- Run this migration after 002-add-indexes.sql

-- Migration: 003-add-views
-- Description: Create views for common queries and reporting
-- Created: 2026-11-05
-- Author: Software Architecture Course

-- ============================================
-- INVENTORY DATABASE VIEWS
-- ============================================

-- View for current inventory status
CREATE OR REPLACE VIEW inventory_schema.inventory_status AS
SELECT 
    i.product_id,
    i.quantity_available,
    i.quantity_reserved,
    (i.quantity_available + i.quantity_reserved) as total_quantity,
    i.last_updated
FROM inventory_schema.inventory i;

-- View for low stock products
CREATE OR REPLACE VIEW inventory_schema.low_stock_products AS
SELECT 
    i.product_id,
    i.quantity_available,
    i.quantity_reserved,
    lsa.threshold,
    lsa.alert_status,
    lsa.created_at as alert_created_at
FROM inventory_schema.inventory i
LEFT JOIN inventory_schema.low_stock_alerts lsa ON i.product_id = lsa.product_id
WHERE i.quantity_available <= 10 OR lsa.alert_status = 'ACTIVE';

-- View for inventory transaction summary
CREATE OR REPLACE VIEW inventory_schema.transaction_summary AS
SELECT 
    transaction_type,
    COUNT(*) as transaction_count,
    SUM(quantity) as total_quantity,
    MIN(created_at) as first_transaction,
    MAX(created_at) as last_transaction
FROM inventory_schema.inventory_transactions
GROUP BY transaction_type;

-- ============================================
-- ANALYTICS DATABASE VIEWS
-- ============================================

-- View for daily sales summary
CREATE OR REPLACE VIEW analytics_schema.daily_sales_summary AS
SELECT 
    metric_date,
    total_orders,
    total_revenue,
    total_users,
    new_users,
    average_order_value,
    conversion_rate
FROM analytics_schema.daily_metrics
ORDER BY metric_date DESC;

-- View for top products
CREATE OR REPLACE VIEW analytics_schema.top_products AS
SELECT 
    product_id,
    SUM(purchases) as total_purchases,
    SUM(revenue) as total_revenue,
    AVG(average_rating) as avg_rating,
    SUM(review_count) as total_reviews
FROM analytics_schema.product_performance
GROUP BY product_id
ORDER BY total_revenue DESC;

-- View for user lifetime value
CREATE OR REPLACE VIEW analytics_schema.user_lifetime_value AS
SELECT 
    user_id,
    COUNT(DISTINCT metric_date) as active_days,
    SUM(total_orders) as total_orders,
    SUM(total_spent) as total_spent,
    AVG(average_order_value) as avg_order_value,
    MIN(metric_date) as first_activity,
    MAX(metric_date) as last_activity
FROM analytics_schema.user_behavior
GROUP BY user_id
ORDER BY total_spent DESC;

-- View for monthly revenue trend
CREATE OR REPLACE VIEW analytics_schema.monthly_revenue_trend AS
SELECT 
    DATE_TRUNC('month', metric_date) as month,
    SUM(total_revenue) as monthly_revenue,
    SUM(total_orders) as monthly_orders,
    COUNT(DISTINCT user_id) as active_users
FROM analytics_schema.daily_metrics dm
LEFT JOIN analytics_schema.user_behavior ub ON dm.metric_date = ub.metric_date
GROUP BY DATE_TRUNC('month', metric_date)
ORDER BY month DESC;

-- ============================================
-- ORDER DATABASE VIEWS
-- ============================================

-- View for order summary
CREATE OR REPLACE VIEW order_schema.order_summary AS
SELECT 
    o.id as order_id,
    o.user_id,
    o.status,
    o.total_amount,
    o.tax_amount,
    o.shipping_amount,
    o.created_at,
    COUNT(oi.id) as item_count,
    SUM(oi.quantity) as total_quantity
FROM order_schema.orders o
LEFT JOIN order_schema.order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.user_id, o.status, o.total_amount, o.tax_amount, o.shipping_amount, o.created_at;

-- View for order status timeline
CREATE OR REPLACE VIEW order_schema.order_status_timeline AS
SELECT 
    o.id as order_id,
    o.user_id,
    o.created_at as order_created,
    MIN(osh.created_at) as first_status_change,
    MAX(osh.created_at) as last_status_change,
    COUNT(osh.id) as status_changes,
    STRING_AGG(osh.new_status, ' → ' ORDER BY osh.created_at) as status_flow
FROM order_schema.orders o
LEFT JOIN order_schema.order_status_history osh ON o.id = osh.order_id
GROUP BY o.id, o.user_id, o.created_at;

-- View for pending orders
CREATE OR REPLACE VIEW order_schema.pending_orders AS
SELECT 
    o.*,
    p.status as payment_status,
    p.transaction_id
FROM order_schema.orders o
LEFT JOIN order_schema.payments p ON o.id = p.order_id
WHERE o.status IN ('PENDING', 'CONFIRMED')
ORDER BY o.created_at DESC;

-- ============================================
-- PRODUCT DATABASE VIEWS
-- ============================================

-- View for product catalog
CREATE OR REPLACE VIEW product_schema.product_catalog AS
SELECT 
    p.id,
    p.name,
    p.description,
    p.price,
    p.sku,
    p.image_url,
    p.is_active,
    p.created_at,
    c.name as category_name,
    c.description as category_description,
    COALESCE(AVG(pr.rating), 0) as average_rating,
    COUNT(pr.id) as review_count
FROM product_schema.products p
LEFT JOIN product_schema.categories c ON p.category_id = c.id
LEFT JOIN product_schema.product_reviews pr ON p.id = pr.product_id
GROUP BY p.id, p.name, p.description, p.price, p.sku, p.image_url, p.is_active, p.created_at, c.name, c.description;

-- View for top rated products
CREATE OR REPLACE VIEW product_schema.top_rated_products AS
SELECT 
    p.id,
    p.name,
    p.price,
    c.name as category,
    AVG(pr.rating) as average_rating,
    COUNT(pr.id) as review_count
FROM product_schema.products p
LEFT JOIN product_schema.categories c ON p.category_id = c.id
LEFT JOIN product_schema.product_reviews pr ON p.id = pr.product_id
WHERE p.is_active = true
GROUP BY p.id, p.name, p.price, c.name
HAVING AVG(pr.rating) >= 4.0 AND COUNT(pr.id) >= 5
ORDER BY average_rating DESC, review_count DESC;

-- ============================================
-- USER DATABASE VIEWS
-- ============================================

-- View for user profile with addresses
CREATE OR REPLACE VIEW user_schema.user_profiles AS
SELECT 
    u.id,
    u.email,
    u.username,
    u.first_name,
    u.last_name,
    u.phone,
    u.created_at,
    u.updated_at,
    u.is_active,
    COUNT(a.id) as address_count,
    STRING_AGG(
        CASE WHEN a.is_default THEN 'DEFAULT: ' || a.street || ', ' || a.city || ', ' || a.country
             ELSE a.street || ', ' || a.city || ', ' || a.country
        END, ' | '
    ) as addresses
FROM user_schema.users u
LEFT JOIN user_schema.addresses a ON u.id = a.user_id
GROUP BY u.id, u.email, u.username, u.first_name, u.last_name, u.phone, u.created_at, u.updated_at, u.is_active;

-- View for active users
CREATE OR REPLACE VIEW user_schema.active_users AS
SELECT 
    u.*,
    COUNT(ual.id) as audit_entries,
    MAX(ual.created_at) as last_audit_entry
FROM user_schema.users u
LEFT JOIN user_schema.user_audit_log ual ON u.id = ual.user_id
WHERE u.is_active = true
GROUP BY u.id, u.email, u.username, u.first_name, u.last_name, u.phone, u.created_at, u.updated_at, u.is_active;

-- ============================================
-- CART DATABASE VIEWS
-- ============================================

-- View for active carts
CREATE OR REPLACE VIEW cart_schema.active_carts AS
SELECT 
    c.id as cart_id,
    c.user_id,
    c.total_amount,
    c.tax_amount,
    c.shipping_amount,
    c.coupon_discount,
    c.created_at,
    c.expires_at,
    COUNT(ci.id) as item_count,
    SUM(ci.quantity) as total_quantity
FROM cart_schema.carts c
LEFT JOIN cart_schema.cart_items ci ON c.id = ci.cart_id
WHERE c.status = 'ACTIVE' AND c.expires_at > CURRENT_TIMESTAMP
GROUP BY c.id, c.user_id, c.total_amount, c.tax_amount, c.shipping_amount, c.coupon_discount, c.created_at, c.expires_at;

-- View for abandoned carts
CREATE OR REPLACE VIEW cart_schema.abandoned_carts_summary AS
SELECT 
    ac.cart_id,
    ac.user_id,
    ac.total_amount,
    ac.abandoned_at,
    ac.recovery_email_sent,
    ac.recovery_email_sent_at,
    c.item_count,
    c.total_quantity,
    AGE(CURRENT_TIMESTAMP, ac.abandoned_at) as time_since_abandoned
FROM cart_schema.abandoned_carts ac
LEFT JOIN cart_schema.active_carts c ON ac.cart_id = c.cart_id
WHERE ac.recovery_email_sent = false OR ac.recovery_email_sent_at < CURRENT_TIMESTAMP - INTERVAL '7 days';

-- ============================================
-- PAYMENT DATABASE VIEWS
-- ============================================

-- View for payment transactions summary
CREATE OR REPLACE VIEW payment_schema.payment_transactions_summary AS
SELECT 
    t.id as transaction_id,
    t.order_id,
    t.amount,
    t.currency,
    t.status,
    t.payment_method,
    t.payment_gateway,
    t.created_at,
    t.updated_at,
    COALESCE(SUM(r.amount), 0) as total_refunded,
    COUNT(r.id) as refund_count
FROM payment_schema.transactions t
LEFT JOIN payment_schema.refunds r ON t.id = r.transaction_id
GROUP BY t.id, t.order_id, t.amount, t.currency, t.status, t.payment_method, t.payment_gateway, t.created_at, t.updated_at;

-- View for failed payments requiring retry
CREATE OR REPLACE VIEW payment_schema.failed_payments_retry AS
SELECT 
    t.*,
    prl.retry_count,
    prl.last_error,
    prl.next_retry_at
FROM payment_schema.transactions t
LEFT JOIN payment_schema.payment_retry_log prl ON t.id = prl.transaction_id
WHERE t.status = 'FAILED' 
  AND (prl.next_retry_at IS NULL OR prl.next_retry_at <= CURRENT_TIMESTAMP)
  AND (prl.retry_count IS NULL OR prl.retry_count < 3);

-- ============================================
-- CROSS-SCHEMA VIEWS (READ-ONLY FOR REPORTING)
-- ============================================

-- Note: These views require appropriate permissions and may need to be created
-- in a separate reporting database in production

-- View for complete order analytics (requires read access to multiple schemas)
-- CREATE OR REPLACE VIEW reporting.complete_order_analytics AS
-- SELECT 
--     o.id as order_id,
--     o.user_id,
--     o.status,
--     o.total_amount,
--     o.created_at,
--     u.email,
--     u.first_name,
--     u.last_name,
--     COUNT(oi.id) as item_count,
--     SUM(oi.quantity) as total_quantity
-- FROM order_schema.orders o
-- JOIN user_schema.users u ON o.user_id = u.id
-- LEFT JOIN order_schema.order_items oi ON o.id = oi.order_id
-- GROUP BY o.id, o.user_id, o.status, o.total_amount, o.created_at, u.email, u.first_name, u.last_name;

-- ============================================
-- MIGRATION COMPLETE
-- ============================================

-- Migration completed successfully
-- All views created for common queries and reporting
-- Next step: Run 004-add-constraints.sql for additional constraints