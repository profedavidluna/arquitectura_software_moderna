-- Add Indexes Migration
-- This file creates indexes for all tables to optimize query performance
-- Run this migration after 001-initial-schema.sql

-- Migration: 002-add-indexes
-- Description: Create indexes for all database tables
-- Created: 2026-11-05
-- Author: Software Architecture Course

-- ============================================
-- USER DATABASE INDEXES
-- ============================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON user_schema.users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON user_schema.users(username);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON user_schema.users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON user_schema.users(is_active);

-- Addresses table indexes
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON user_schema.addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_addresses_default ON user_schema.addresses(user_id, is_default);
CREATE INDEX IF NOT EXISTS idx_addresses_country ON user_schema.addresses(country);
CREATE INDEX IF NOT EXISTS idx_addresses_city ON user_schema.addresses(city);

-- User audit log indexes
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON user_schema.user_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON user_schema.user_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON user_schema.user_audit_log(action);

-- ============================================
-- PRODUCT DATABASE INDEXES
-- ============================================

-- Categories table indexes
CREATE INDEX IF NOT EXISTS idx_categories_name ON product_schema.categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_created_at ON product_schema.categories(created_at);

-- Products table indexes
CREATE INDEX IF NOT EXISTS idx_products_category_id ON product_schema.products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON product_schema.products(sku);
CREATE INDEX IF NOT EXISTS idx_products_name ON product_schema.products(name);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON product_schema.products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_price ON product_schema.products(price);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON product_schema.products(created_at);

-- Full-text search index for product search
CREATE INDEX IF NOT EXISTS idx_products_search ON product_schema.products 
USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- Product reviews indexes
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON product_schema.product_reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON product_schema.product_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON product_schema.product_reviews(rating);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON product_schema.product_reviews(created_at);

-- ============================================
-- ORDER DATABASE INDEXES
-- ============================================

-- Orders table indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON order_schema.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON order_schema.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON order_schema.orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_total_amount ON order_schema.orders(total_amount);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON order_schema.orders(updated_at);

-- Order items indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_schema.order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_schema.order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_created_at ON order_schema.order_items(created_at);

-- Payments table indexes
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON order_schema.payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON order_schema.payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON order_schema.payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON order_schema.payments(transaction_id);

-- Order status history indexes
CREATE INDEX IF NOT EXISTS idx_status_history_order_id ON order_schema.order_status_history(order_id);
CREATE INDEX IF NOT EXISTS idx_status_history_new_status ON order_schema.order_status_history(new_status);
CREATE INDEX IF NOT EXISTS idx_status_history_created_at ON order_schema.order_status_history(created_at);

-- ============================================
-- INVENTORY DATABASE INDEXES
-- ============================================

-- Inventory table indexes
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory_schema.inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_quantity_available ON inventory_schema.inventory(quantity_available);
CREATE INDEX IF NOT EXISTS idx_inventory_last_updated ON inventory_schema.inventory(last_updated);

-- Inventory transactions indexes
CREATE INDEX IF NOT EXISTS idx_transactions_product_id ON inventory_schema.inventory_transactions(product_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON inventory_schema.inventory_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON inventory_schema.inventory_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON inventory_schema.inventory_transactions(reference_id, reference_type);

-- Low stock alerts indexes
CREATE INDEX IF NOT EXISTS idx_low_stock_product_id ON inventory_schema.low_stock_alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_low_stock_status ON inventory_schema.low_stock_alerts(alert_status);
CREATE INDEX IF NOT EXISTS idx_low_stock_created_at ON inventory_schema.low_stock_alerts(created_at);

-- ============================================
-- CART DATABASE INDEXES
-- ============================================

-- Carts table indexes
CREATE INDEX IF NOT EXISTS idx_carts_user_id ON cart_schema.carts(user_id);
CREATE INDEX IF NOT EXISTS idx_carts_status ON cart_schema.carts(status);
CREATE INDEX IF NOT EXISTS idx_carts_expires_at ON cart_schema.carts(expires_at);
CREATE INDEX IF NOT EXISTS idx_carts_created_at ON cart_schema.carts(created_at);
CREATE INDEX IF NOT EXISTS idx_carts_total_amount ON cart_schema.carts(total_amount);

-- Cart items indexes
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_schema.cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_schema.cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_created_at ON cart_schema.cart_items(created_at);

-- Abandoned carts indexes
CREATE INDEX IF NOT EXISTS idx_abandoned_carts_user_id ON cart_schema.abandoned_carts(user_id);
CREATE INDEX IF NOT EXISTS idx_abandoned_carts_abandoned_at ON cart_schema.abandoned_carts(abandoned_at);
CREATE INDEX IF NOT EXISTS idx_abandoned_carts_recovery_sent ON cart_schema.abandoned_carts(recovery_email_sent);

-- ============================================
-- PAYMENT DATABASE INDEXES
-- ============================================

-- Transactions table indexes
CREATE INDEX IF NOT EXISTS idx_transactions_order_id ON payment_schema.transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON payment_schema.transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON payment_schema.transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_gateway_id ON payment_schema.transactions(gateway_transaction_id);
CREATE INDEX IF NOT EXISTS idx_transactions_payment_method ON payment_schema.transactions(payment_method);

-- Refunds table indexes
CREATE INDEX IF NOT EXISTS idx_refunds_transaction_id ON payment_schema.refunds(transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_order_id ON payment_schema.refunds(order_id);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON payment_schema.refunds(status);
CREATE INDEX IF NOT EXISTS idx_refunds_created_at ON payment_schema.refunds(created_at);

-- Payment methods indexes
CREATE INDEX IF NOT EXISTS idx_payment_methods_user_id ON payment_schema.payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_methods_default ON payment_schema.payment_methods(user_id, is_default);
CREATE INDEX IF NOT EXISTS idx_payment_methods_method_type ON payment_schema.payment_methods(method_type);
CREATE INDEX IF NOT EXISTS idx_payment_methods_created_at ON payment_schema.payment_methods(created_at);

-- Payment retry log indexes
CREATE INDEX IF NOT EXISTS idx_retry_log_transaction_id ON payment_schema.payment_retry_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_retry_log_next_retry_at ON payment_schema.payment_retry_log(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_retry_log_created_at ON payment_schema.payment_retry_log(created_at);

-- ============================================
-- ANALYTICS DATABASE INDEXES
-- ============================================

-- Events table indexes
CREATE INDEX IF NOT EXISTS idx_events_type ON analytics_schema.events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON analytics_schema.events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_order_id ON analytics_schema.events(order_id);
CREATE INDEX IF NOT EXISTS idx_events_product_id ON analytics_schema.events(product_id);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON analytics_schema.events(created_at);

-- Daily metrics indexes
CREATE INDEX IF NOT EXISTS idx_daily_metrics_date ON analytics_schema.daily_metrics(metric_date);
CREATE INDEX IF NOT EXISTS idx_daily_metrics_created_at ON analytics_schema.daily_metrics(created_at);

-- Product performance indexes
CREATE INDEX IF NOT EXISTS idx_product_performance_product_id ON analytics_schema.product_performance(product_id);
CREATE INDEX IF NOT EXISTS idx_product_performance_date ON analytics_schema.product_performance(metric_date);
CREATE INDEX IF NOT EXISTS idx_product_performance_revenue ON analytics_schema.product_performance(revenue);

-- User behavior indexes
CREATE INDEX IF NOT EXISTS idx_user_behavior_user_id ON analytics_schema.user_behavior(user_id);
CREATE INDEX IF NOT EXISTS idx_user_behavior_date ON analytics_schema.user_behavior(metric_date);
CREATE INDEX IF NOT EXISTS idx_user_behavior_total_spent ON analytics_schema.user_behavior(total_spent);

-- Cohort analysis indexes
CREATE INDEX IF NOT EXISTS idx_cohort_analysis_date ON analytics_schema.cohort_analysis(cohort_date);
CREATE INDEX IF NOT EXISTS idx_cohort_analysis_created_at ON analytics_schema.cohort_analysis(created_at);

-- ============================================
-- COMPOSITE INDEXES FOR COMMON QUERY PATTERNS
-- ============================================

-- Composite index for user orders query
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON order_schema.orders(user_id, status, created_at DESC);

-- Composite index for product search
CREATE INDEX IF NOT EXISTS idx_products_category_active ON product_schema.products(category_id, is_active, created_at DESC);

-- Composite index for inventory status
CREATE INDEX IF NOT EXISTS idx_inventory_status ON inventory_schema.inventory(quantity_available, quantity_reserved);

-- Composite index for cart expiration
CREATE INDEX IF NOT EXISTS idx_carts_expiration_status ON cart_schema.carts(expires_at, status) WHERE status = 'ACTIVE';

-- Composite index for payment processing
CREATE INDEX IF NOT EXISTS idx_transactions_status_created ON payment_schema.transactions(status, created_at) WHERE status IN ('PENDING', 'PROCESSING');

-- ============================================
-- MIGRATION COMPLETE
-- ============================================

-- Migration completed successfully
-- All indexes created for optimal query performance
-- Next step: Run 003-add-views.sql to create views