-- Payment Database Schema

-- Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    payment_method VARCHAR(50) NOT NULL,
    payment_gateway VARCHAR(50),
    gateway_transaction_id VARCHAR(255),
    gateway_response JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Refunds Table
CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    gateway_refund_id VARCHAR(255),
    gateway_response JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment Methods Table
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    method_type VARCHAR(50) NOT NULL CHECK (method_type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY')),
    token VARCHAR(255) NOT NULL,
    last_four VARCHAR(4),
    expiry_month INT,
    expiry_year INT,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment Retry Log Table
CREATE TABLE payment_retry_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    retry_count INT DEFAULT 0,
    last_error TEXT,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_transactions_order_id ON transactions(order_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_gateway_id ON transactions(gateway_transaction_id);
CREATE INDEX idx_refunds_transaction_id ON refunds(transaction_id);
CREATE INDEX idx_refunds_order_id ON refunds(order_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_default ON payment_methods(user_id, is_default);
CREATE INDEX idx_retry_log_transaction_id ON payment_retry_log(transaction_id);
