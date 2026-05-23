-- ============================================
-- USER DATABASE SCHEMA (user_db)
-- ============================================
-- Service: User Service
-- Purpose: User profiles, addresses, authentication data
-- Port: 5432
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
-- ============================================

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('ADMIN', 'CUSTOMER', 'SUPPORT')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS 'Core user accounts table storing profile and authentication information';
COMMENT ON COLUMN users.id IS 'Unique identifier for the user (UUID v4)';
COMMENT ON COLUMN users.email IS 'User email address, used for login and notifications';
COMMENT ON COLUMN users.username IS 'Unique display name for the user';
COMMENT ON COLUMN users.password_hash IS 'Bcrypt hashed password (managed by Keycloak in production)';
COMMENT ON COLUMN users.first_name IS 'User first name';
COMMENT ON COLUMN users.last_name IS 'User last name';
COMMENT ON COLUMN users.phone IS 'Optional phone number in international format';
COMMENT ON COLUMN users.avatar_url IS 'URL to user profile image';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, CUSTOMER, or SUPPORT';
COMMENT ON COLUMN users.is_active IS 'Soft delete flag - false means account is deactivated';
COMMENT ON COLUMN users.email_verified IS 'Whether the user has verified their email address';
COMMENT ON COLUMN users.last_login_at IS 'Timestamp of the last successful login';
COMMENT ON COLUMN users.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN users.updated_at IS 'Record last update timestamp';

-- Addresses Table
CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL DEFAULT 'Home' CHECK (label IN ('Home', 'Work', 'Other')),
    street VARCHAR(255) NOT NULL,
    street_line_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE addresses IS 'User delivery and billing addresses';
COMMENT ON COLUMN addresses.id IS 'Unique identifier for the address';
COMMENT ON COLUMN addresses.user_id IS 'Reference to the owning user';
COMMENT ON COLUMN addresses.label IS 'Address label: Home, Work, or Other';
COMMENT ON COLUMN addresses.street IS 'Primary street address line';
COMMENT ON COLUMN addresses.street_line_2 IS 'Secondary address line (apt, suite, etc.)';
COMMENT ON COLUMN addresses.city IS 'City name';
COMMENT ON COLUMN addresses.state IS 'State or province';
COMMENT ON COLUMN addresses.postal_code IS 'Postal/ZIP code';
COMMENT ON COLUMN addresses.country IS 'Country name or ISO code';
COMMENT ON COLUMN addresses.is_default IS 'Whether this is the default shipping address';
COMMENT ON COLUMN addresses.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN addresses.updated_at IS 'Record last update timestamp';

-- User Audit Log Table
CREATE TABLE IF NOT EXISTS user_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'PASSWORD_CHANGE', 'EMAIL_VERIFY')),
    ip_address VARCHAR(45),
    user_agent TEXT,
    changes JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE user_audit_log IS 'Audit trail for all user account changes and actions';
COMMENT ON COLUMN user_audit_log.action IS 'Type of action performed';
COMMENT ON COLUMN user_audit_log.ip_address IS 'IP address from which the action was performed';
COMMENT ON COLUMN user_audit_log.user_agent IS 'Browser/client user agent string';
COMMENT ON COLUMN user_audit_log.changes IS 'JSON object containing old and new values for changed fields';

-- ============================================
-- INDEXES
-- ============================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login_at);

-- Addresses table indexes
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_addresses_default ON addresses(user_id, is_default);
CREATE INDEX IF NOT EXISTS idx_addresses_country ON addresses(country);
CREATE INDEX IF NOT EXISTS idx_addresses_city ON addresses(city);

-- Audit log indexes
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON user_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON user_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON user_audit_log(created_at);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_users_active_role ON users(is_active, role);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_action ON user_audit_log(user_id, action, created_at DESC);

-- ============================================
-- CONSTRAINTS
-- ============================================

-- Email format validation
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS chk_email_format 
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Phone format validation (optional field)
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS chk_phone_format 
    CHECK (phone IS NULL OR phone ~ '^\+?[0-9\-() ]{7,20}$');

-- Username format (alphanumeric + underscore, 3-100 chars)
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS chk_username_format 
    CHECK (username ~ '^[a-zA-Z0-9_]{3,100}$');

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

CREATE TRIGGER trg_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_addresses_updated_at 
    BEFORE UPDATE ON addresses 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Ensure only one default address per user
CREATE OR REPLACE FUNCTION ensure_single_default_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = true THEN
        UPDATE addresses 
        SET is_default = false 
        WHERE user_id = NEW.user_id 
          AND id != NEW.id 
          AND is_default = true;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_default_address 
    BEFORE INSERT OR UPDATE ON addresses 
    FOR EACH ROW EXECUTE FUNCTION ensure_single_default_address();

-- ============================================
-- VIEWS
-- ============================================

-- User profiles with default address
CREATE OR REPLACE VIEW user_profiles AS
SELECT 
    u.id,
    u.email,
    u.username,
    u.first_name,
    u.last_name,
    u.phone,
    u.role,
    u.is_active,
    u.email_verified,
    u.last_login_at,
    u.created_at,
    u.updated_at,
    a.street AS default_street,
    a.city AS default_city,
    a.state AS default_state,
    a.postal_code AS default_postal_code,
    a.country AS default_country
FROM users u
LEFT JOIN addresses a ON u.id = a.user_id AND a.is_default = true;

COMMENT ON VIEW user_profiles IS 'User profiles joined with their default address';

-- Active users summary
CREATE OR REPLACE VIEW active_users_summary AS
SELECT 
    u.id,
    u.email,
    u.username,
    u.first_name || ' ' || u.last_name AS full_name,
    u.role,
    u.last_login_at,
    u.created_at,
    COUNT(a.id) AS address_count,
    COUNT(ual.id) AS audit_entries
FROM users u
LEFT JOIN addresses a ON u.id = a.user_id
LEFT JOIN user_audit_log ual ON u.id = ual.user_id
WHERE u.is_active = true
GROUP BY u.id, u.email, u.username, u.first_name, u.last_name, u.role, u.last_login_at, u.created_at;

COMMENT ON VIEW active_users_summary IS 'Summary of active users with address and audit counts';
