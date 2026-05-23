-- ============================================
-- PAYMENT DATABASE SCHEMA (payment_db)
-- ============================================
-- Service: Payment Service
-- Purpose: Payment processing, refunds, and payment method management
-- Port: 5437
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
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

COMMENT ON TABLE transactions IS 'Payment transaction records with gateway integration details';
COMMENT ON COLUMN transactions.id IS 'Unique identifier for the transaction';
COMMENT ON COLUMN transactions.transaction_number IS 'Human-readable transaction number (e.g., TXN-20261105-001)';
COMMENT ON COLUMN transactions.order_id IS 'Reference to the order being paid (cross-service)';
COMMENT ON COLUMN transactions.user_id IS 'Reference to the paying user (cross-service)';
COMMENT ON COLUMN transactions.amount IS 'Transaction amount in the specified currency';
COMMENT ON COLUMN transactions.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN transactions.status IS 'Transaction lifecycle status';
COMMENT ON COLUMN transactions.payment_method IS 'Payment method used';
COMMENT ON COLUMN transactions.payment_gateway IS 'Payment gateway provider (STRIPE, PAYPAL, etc.)';
COMMENT ON COLUMN transactions.gateway_transaction_id IS 'Transaction ID from the payment gateway';
COMMENT ON COLUMN transactions.gateway_response IS 'Raw JSON response from the payment gateway';
COMMENT ON COLUMN transactions.failure_reason IS 'Reason for failure (if status is FAILED)';
COMMENT ON COLUMN transactions.idempotency_key IS 'Unique key to prevent duplicate transactions';
COMMENT ON COLUMN transactions.ip_address IS 'IP address of the payer for fraud detection';
COMMENT ON COLUMN transactions.processed_at IS 'Timestamp when payment was processed by gateway';
COMMENT ON COLUMN transactions.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN transactions.updated_at IS 'Record last update timestamp';

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

COMMENT ON TABLE refunds IS 'Refund records linked to original transactions';
COMMENT ON COLUMN refunds.id IS 'Unique identifier for the refund';
COMMENT ON COLUMN refunds.refund_number IS 'Human-readable refund number (e.g., RFN-20261105-001)';
COMMENT ON COLUMN refunds.transaction_id IS 'Reference to the original transaction being refunded';
COMMENT ON COLUMN refunds.order_id IS 'Reference to the order (cross-service)';
COMMENT ON COLUMN refunds.amount IS 'Refund amount (can be partial)';
COMMENT ON COLUMN refunds.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN refunds.reason IS 'Detailed reason for the refund';
COMMENT ON COLUMN refunds.reason_category IS 'Categorized reason for analytics';
COMMENT ON COLUMN refunds.status IS 'Refund processing status';
COMMENT ON COLUMN refunds.gateway_refund_id IS 'Refund ID from the payment gateway';
COMMENT ON COLUMN refunds.gateway_response IS 'Raw JSON response from the payment gateway';
COMMENT ON COLUMN refunds.approved_by IS 'Admin/support user who approved the refund';
COMMENT ON COLUMN refunds.processed_at IS 'Timestamp when refund was processed';
COMMENT ON COLUMN refunds.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN refunds.updated_at IS 'Record last update timestamp';

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

COMMENT ON TABLE payment_methods IS 'Stored payment methods (tokenized, PCI compliant)';
COMMENT ON COLUMN payment_methods.id IS 'Unique identifier for the payment method';
COMMENT ON COLUMN payment_methods.user_id IS 'Reference to the owning user (cross-service)';
COMMENT ON COLUMN payment_methods.method_type IS 'Type of payment method';
COMMENT ON COLUMN payment_methods.provider IS 'Payment provider that tokenized this method';
COMMENT ON COLUMN payment_methods.token IS 'Tokenized payment method (never store raw card data)';
COMMENT ON COLUMN payment_methods.last_four IS 'Last 4 digits of card number for display';
COMMENT ON COLUMN payment_methods.card_brand IS 'Card brand (VISA, MASTERCARD, AMEX, etc.)';
COMMENT ON COLUMN payment_methods.expiry_month IS 'Card expiration month (1-12)';
COMMENT ON COLUMN payment_methods.expiry_year IS 'Card expiration year';
COMMENT ON COLUMN payment_methods.billing_name IS 'Name on the payment method';
COMMENT ON COLUMN payment_methods.billing_address IS 'Billing address JSON';
COMMENT ON COLUMN payment_methods.is_default IS 'Whether this is the default payment method';
COMMENT ON COLUMN payment_methods.is_active IS 'Whether the method is still valid/active';
COMMENT ON COLUMN payment_methods.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN payment_methods.updated_at IS 'Record last update timestamp';

-- Payment Retry Log Table
CREATE TABLE IF NOT EXISTS payment_retry_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL CHECK (attempt_number > 0),
    status VARCHAR(50) NOT NULL CHECK (status IN ('ATTEMPTED', 'SUCCESS', 'FAILED')),
    error_code VARCHAR(50),
    error_message TEXT,
    gateway_response JSONB,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE payment_retry_log IS 'Log of payment retry attempts with exponential backoff';
COMMENT ON COLUMN payment_retry_log.transaction_id IS 'Reference to the transaction being retried';
COMMENT ON COLUMN payment_retry_log.attempt_number IS 'Sequential attempt number (1, 2, 3...)';
COMMENT ON COLUMN payment_retry_log.status IS 'Result of this retry attempt';
COMMENT ON COLUMN payment_retry_log.error_code IS 'Error code from the gateway';
COMMENT ON COLUMN payment_retry_log.error_message IS 'Human-readable error message';
COMMENT ON COLUMN payment_retry_log.gateway_response IS 'Raw gateway response for this attempt';
COMMENT ON COLUMN payment_retry_log.next_retry_at IS 'Scheduled time for next retry (exponential backoff)';

-- ============================================
-- INDEXES
-- ============================================

-- Transactions indexes
CREATE INDEX IF NOT EXISTS idx_txn_order_id ON transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_txn_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_txn_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_txn_gateway_id ON transactions(gateway_transaction_id);
CREATE INDEX IF NOT EXISTS idx_txn_idempotency ON transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_txn_created_at ON transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_txn_payment_method ON transactions(payment_method);

-- Refunds indexes
CREATE INDEX IF NOT EXISTS idx_refunds_transaction_id ON refunds(transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_order_id ON refunds(order_id);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON refunds(status);
CREATE INDEX IF NOT EXISTS idx_refunds_reason_category ON refunds(reason_category);
CREATE INDEX IF NOT EXISTS idx_refunds_created_at ON refunds(created_at);

-- Payment methods indexes
CREATE INDEX IF NOT EXISTS idx_pm_user_id ON payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_pm_method_type ON payment_methods(method_type);
CREATE INDEX IF NOT EXISTS idx_pm_is_active ON payment_methods(is_active);

-- Retry log indexes
CREATE INDEX IF NOT EXISTS idx_retry_transaction_id ON payment_retry_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_retry_next_at ON payment_retry_log(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_retry_status ON payment_retry_log(status);

-- Composite indexes
CREATE INDEX IF NOT EXISTS idx_txn_user_status ON transactions(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pm_user_default ON payment_methods(user_id, is_default) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_txn_pending ON transactions(status, created_at) 
    WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX IF NOT EXISTS idx_retry_pending ON payment_retry_log(next_retry_at, status) 
    WHERE status = 'FAILED';

-- ============================================
-- CONSTRAINTS
-- ============================================

-- Refund amount cannot exceed original transaction amount
-- (enforced at application level due to partial refunds)

-- Payment token must not be empty
ALTER TABLE payment_methods ADD CONSTRAINT IF NOT EXISTS chk_token_not_empty 
    CHECK (LENGTH(TRIM(token)) > 0);

-- Max 3 retry attempts per transaction
ALTER TABLE payment_retry_log ADD CONSTRAINT IF NOT EXISTS chk_max_retries 
    CHECK (attempt_number <= 5);

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

CREATE TRIGGER trg_transactions_updated_at 
    BEFORE UPDATE ON transactions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_refunds_updated_at 
    BEFORE UPDATE ON refunds 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payment_methods_updated_at 
    BEFORE UPDATE ON payment_methods 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Ensure only one default payment method per user
CREATE OR REPLACE FUNCTION ensure_single_default_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = true THEN
        UPDATE payment_methods 
        SET is_default = false 
        WHERE user_id = NEW.user_id 
          AND id != NEW.id 
          AND is_default = true;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_default_payment 
    BEFORE INSERT OR UPDATE ON payment_methods 
    FOR EACH ROW EXECUTE FUNCTION ensure_single_default_payment();

-- ============================================
-- VIEWS
-- ============================================

-- Transaction summary with refund totals
CREATE OR REPLACE VIEW transaction_summary AS
SELECT 
    t.id AS transaction_id,
    t.transaction_number,
    t.order_id,
    t.user_id,
    t.amount,
    t.currency,
    t.status,
    t.payment_method,
    t.payment_gateway,
    t.processed_at,
    t.created_at,
    COALESCE(SUM(r.amount), 0) AS total_refunded,
    COUNT(r.id) AS refund_count,
    (t.amount - COALESCE(SUM(r.amount), 0)) AS net_amount
FROM transactions t
LEFT JOIN refunds r ON t.id = r.transaction_id AND r.status = 'COMPLETED'
GROUP BY t.id, t.transaction_number, t.order_id, t.user_id, t.amount, t.currency, 
         t.status, t.payment_method, t.payment_gateway, t.processed_at, t.created_at;

COMMENT ON VIEW transaction_summary IS 'Transactions with aggregated refund information and net amounts';

-- Failed payments pending retry
CREATE OR REPLACE VIEW failed_payments_pending_retry AS
SELECT 
    t.id AS transaction_id,
    t.transaction_number,
    t.order_id,
    t.amount,
    t.payment_method,
    t.failure_reason,
    t.created_at,
    COALESCE(MAX(prl.attempt_number), 0) AS retry_attempts,
    MAX(prl.next_retry_at) AS next_retry_at,
    MAX(prl.error_message) AS last_error
FROM transactions t
LEFT JOIN payment_retry_log prl ON t.id = prl.transaction_id
WHERE t.status = 'FAILED'
GROUP BY t.id, t.transaction_number, t.order_id, t.amount, t.payment_method, t.failure_reason, t.created_at
HAVING COALESCE(MAX(prl.attempt_number), 0) < 3
ORDER BY t.created_at ASC;

COMMENT ON VIEW failed_payments_pending_retry IS 'Failed transactions eligible for retry (less than 3 attempts)';

-- Daily revenue summary
CREATE OR REPLACE VIEW daily_revenue AS
SELECT 
    DATE(processed_at) AS revenue_date,
    COUNT(*) AS transaction_count,
    SUM(amount) AS gross_revenue,
    currency
FROM transactions
WHERE status = 'COMPLETED' AND processed_at IS NOT NULL
GROUP BY DATE(processed_at), currency
ORDER BY revenue_date DESC;

COMMENT ON VIEW daily_revenue IS 'Daily revenue aggregation from completed transactions';
