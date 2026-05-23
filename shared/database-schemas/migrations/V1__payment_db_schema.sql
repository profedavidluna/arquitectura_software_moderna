-- ============================================
-- Migration: V1__payment_db_schema.sql
-- Database: payment_db
-- Description: Initial schema for Payment Service
-- Created: 2026-11-05
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- SCHEMA CREATION
-- ============================================

-- Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_number VARCHAR(30) UNIQUE NOT NULL,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    payment_method VARCHAR(50) NOT NULL 
        CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY', 'BANK_TRANSFER')),
    payment_gateway VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    gateway_transaction_id VARCHAR(255),
    gateway_response JSONB,
    failure_reason TEXT,
    idempotency_key VARCHAR(255) UNIQUE,
    ip_address VARCHAR(45),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Refunds Table
CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refund_number VARCHAR(30) UNIQUE NOT NULL,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,
    order_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    reason VARCHAR(255) NOT NULL,
    reason_category VARCHAR(50) NOT NULL 
        CHECK (reason_category IN ('CUSTOMER_REQUEST', 'DEFECTIVE_PRODUCT', 'WRONG_ITEM', 'LATE_DELIVERY', 'DUPLICATE_CHARGE', 'OTHER')),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REJECTED')),
    gateway_refund_id VARCHAR(255),
    gateway_response JSONB,
    approved_by UUID,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment Methods Table
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    method_type VARCHAR(50) NOT NULL 
        CHECK (method_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY', 'BANK_TRANSFER')),
    provider VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    token VARCHAR(255) NOT NULL,
    last_four VARCHAR(4),
    card_brand VARCHAR(20),
    expiry_month INT CHECK (expiry_month IS NULL OR (expiry_month >= 1 AND expiry_month <= 12)),
    expiry_year INT CHECK (expiry_year IS NULL OR expiry_year >= 2024),
    billing_name VARCHAR(200),
    billing_address JSONB,
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment Retry Log Table
CREATE TABLE IF NOT EXISTS payment_retry_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL CHECK (attempt_number > 0 AND attempt_number <= 5),
    status VARCHAR(50) NOT NULL CHECK (status IN ('ATTEMPTED', 'SUCCESS', 'FAILED')),
    error_code VARCHAR(50),
    error_message TEXT,
    gateway_response JSONB,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_txn_order_id ON transactions(order_id);
CREATE INDEX idx_txn_user_id ON transactions(user_id);
CREATE INDEX idx_txn_status ON transactions(status);
CREATE INDEX idx_txn_gateway_id ON transactions(gateway_transaction_id);
CREATE INDEX idx_txn_idempotency ON transactions(idempotency_key);
CREATE INDEX idx_txn_created_at ON transactions(created_at);
CREATE INDEX idx_txn_user_status ON transactions(user_id, status, created_at DESC);
CREATE INDEX idx_txn_pending ON transactions(status, created_at) WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_refunds_transaction_id ON refunds(transaction_id);
CREATE INDEX idx_refunds_order_id ON refunds(order_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_reason_category ON refunds(reason_category);

CREATE INDEX idx_pm_user_id ON payment_methods(user_id);
CREATE INDEX idx_pm_user_default ON payment_methods(user_id, is_default) WHERE is_active = true;

CREATE INDEX idx_retry_transaction_id ON payment_retry_log(transaction_id);
CREATE INDEX idx_retry_pending ON payment_retry_log(next_retry_at, status) WHERE status = 'FAILED';

-- ============================================
-- CONSTRAINTS
-- ============================================

ALTER TABLE payment_methods ADD CONSTRAINT chk_token_not_empty CHECK (LENGTH(TRIM(token)) > 0);

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

CREATE TRIGGER trg_transactions_updated_at BEFORE UPDATE ON transactions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_refunds_updated_at BEFORE UPDATE ON refunds 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_payment_methods_updated_at BEFORE UPDATE ON payment_methods 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION ensure_single_default_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = true THEN
        UPDATE payment_methods SET is_default = false 
        WHERE user_id = NEW.user_id AND id != NEW.id AND is_default = true;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_default_payment BEFORE INSERT OR UPDATE ON payment_methods 
    FOR EACH ROW EXECUTE FUNCTION ensure_single_default_payment();

-- ============================================
-- VIEWS
-- ============================================

CREATE OR REPLACE VIEW transaction_summary AS
SELECT 
    t.id AS transaction_id, t.transaction_number, t.order_id, t.user_id,
    t.amount, t.currency, t.status, t.payment_method, t.payment_gateway,
    t.processed_at, t.created_at,
    COALESCE(SUM(r.amount), 0) AS total_refunded,
    COUNT(r.id) AS refund_count,
    (t.amount - COALESCE(SUM(r.amount), 0)) AS net_amount
FROM transactions t
LEFT JOIN refunds r ON t.id = r.transaction_id AND r.status = 'COMPLETED'
GROUP BY t.id, t.transaction_number, t.order_id, t.user_id, t.amount, t.currency,
         t.status, t.payment_method, t.payment_gateway, t.processed_at, t.created_at;

CREATE OR REPLACE VIEW failed_payments_pending_retry AS
SELECT 
    t.id AS transaction_id, t.transaction_number, t.order_id, t.amount,
    t.payment_method, t.failure_reason, t.created_at,
    COALESCE(MAX(prl.attempt_number), 0) AS retry_attempts,
    MAX(prl.next_retry_at) AS next_retry_at
FROM transactions t
LEFT JOIN payment_retry_log prl ON t.id = prl.transaction_id
WHERE t.status = 'FAILED'
GROUP BY t.id, t.transaction_number, t.order_id, t.amount, t.payment_method, t.failure_reason, t.created_at
HAVING COALESCE(MAX(prl.attempt_number), 0) < 3
ORDER BY t.created_at ASC;

-- ============================================
-- MIGRATION TRACKING
-- ============================================

CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_migrations (migration_name) VALUES ('V1__payment_db_schema');
