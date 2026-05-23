-- ============================================
-- ANALYTICS DATABASE SCHEMA (analytics_db)
-- ============================================
-- Service: Analytics Service
-- Purpose: Event tracking, business metrics, and reporting
-- Port: 5438
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
-- ============================================

-- Events Table (append-only event store)
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    event_source VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    user_id UUID,
    session_id VARCHAR(255),
    order_id UUID,
    product_id UUID,
    event_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE events IS 'Append-only event store for all system events and user actions';
COMMENT ON COLUMN events.id IS 'Unique identifier for the event';
COMMENT ON COLUMN events.event_type IS 'Event type (e.g., PAGE_VIEW, ADD_TO_CART, PURCHASE, USER_LOGIN)';
COMMENT ON COLUMN events.event_source IS 'Service or component that generated the event';
COMMENT ON COLUMN events.user_id IS 'User who triggered the event (NULL for anonymous)';
COMMENT ON COLUMN events.session_id IS 'Browser/app session identifier';
COMMENT ON COLUMN events.order_id IS 'Related order ID (if applicable)';
COMMENT ON COLUMN events.product_id IS 'Related product ID (if applicable)';
COMMENT ON COLUMN events.event_data IS 'Event-specific payload data';
COMMENT ON COLUMN events.metadata IS 'Additional metadata (device, location, etc.)';
COMMENT ON COLUMN events.ip_address IS 'Client IP address';
COMMENT ON COLUMN events.user_agent IS 'Client user agent string';
COMMENT ON COLUMN events.created_at IS 'Event timestamp';

-- Daily Metrics Table (pre-aggregated)
CREATE TABLE IF NOT EXISTS metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL,
    metric_type VARCHAR(50) NOT NULL 
        CHECK (metric_type IN ('DAILY_SUMMARY', 'HOURLY_SUMMARY', 'WEEKLY_SUMMARY')),
    total_orders INT NOT NULL DEFAULT 0 CHECK (total_orders >= 0),
    total_revenue DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (total_revenue >= 0),
    total_users INT NOT NULL DEFAULT 0 CHECK (total_users >= 0),
    new_users INT NOT NULL DEFAULT 0 CHECK (new_users >= 0),
    active_users INT NOT NULL DEFAULT 0 CHECK (active_users >= 0),
    average_order_value DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (average_order_value >= 0),
    conversion_rate DECIMAL(5, 4) NOT NULL DEFAULT 0 CHECK (conversion_rate >= 0 AND conversion_rate <= 1),
    cart_abandonment_rate DECIMAL(5, 4) NOT NULL DEFAULT 0 CHECK (cart_abandonment_rate >= 0 AND cart_abandonment_rate <= 1),
    total_page_views INT NOT NULL DEFAULT 0 CHECK (total_page_views >= 0),
    total_sessions INT NOT NULL DEFAULT 0 CHECK (total_sessions >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(metric_date, metric_type)
);

COMMENT ON TABLE metrics IS 'Pre-aggregated business metrics for dashboards and reporting';
COMMENT ON COLUMN metrics.metric_date IS 'Date for which metrics are calculated';
COMMENT ON COLUMN metrics.metric_type IS 'Aggregation level: DAILY, HOURLY, or WEEKLY';
COMMENT ON COLUMN metrics.total_orders IS 'Number of orders placed';
COMMENT ON COLUMN metrics.total_revenue IS 'Total revenue from completed orders';
COMMENT ON COLUMN metrics.total_users IS 'Total registered users as of this date';
COMMENT ON COLUMN metrics.new_users IS 'New user registrations';
COMMENT ON COLUMN metrics.active_users IS 'Users who performed at least one action';
COMMENT ON COLUMN metrics.average_order_value IS 'Average order value (revenue / orders)';
COMMENT ON COLUMN metrics.conversion_rate IS 'Conversion rate (orders / sessions) as decimal 0-1';
COMMENT ON COLUMN metrics.cart_abandonment_rate IS 'Cart abandonment rate as decimal 0-1';
COMMENT ON COLUMN metrics.total_page_views IS 'Total page views';
COMMENT ON COLUMN metrics.total_sessions IS 'Total unique sessions';

-- Product Performance Table
CREATE TABLE IF NOT EXISTS product_performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    views INT NOT NULL DEFAULT 0 CHECK (views >= 0),
    add_to_cart_count INT NOT NULL DEFAULT 0 CHECK (add_to_cart_count >= 0),
    purchases INT NOT NULL DEFAULT 0 CHECK (purchases >= 0),
    revenue DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (revenue >= 0),
    units_sold INT NOT NULL DEFAULT 0 CHECK (units_sold >= 0),
    average_rating DECIMAL(3, 2) CHECK (average_rating IS NULL OR (average_rating >= 1 AND average_rating <= 5)),
    review_count INT NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    return_count INT NOT NULL DEFAULT 0 CHECK (return_count >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, metric_date)
);

COMMENT ON TABLE product_performance IS 'Daily product-level performance metrics';
COMMENT ON COLUMN product_performance.product_id IS 'Reference to the product';
COMMENT ON COLUMN product_performance.metric_date IS 'Date for which metrics are calculated';
COMMENT ON COLUMN product_performance.views IS 'Number of product page views';
COMMENT ON COLUMN product_performance.add_to_cart_count IS 'Number of add-to-cart actions';
COMMENT ON COLUMN product_performance.purchases IS 'Number of completed purchases';
COMMENT ON COLUMN product_performance.revenue IS 'Revenue generated from this product';
COMMENT ON COLUMN product_performance.units_sold IS 'Total units sold';
COMMENT ON COLUMN product_performance.average_rating IS 'Average customer rating (1-5)';
COMMENT ON COLUMN product_performance.review_count IS 'Number of reviews received';
COMMENT ON COLUMN product_performance.return_count IS 'Number of returns/refunds';

-- User Behavior Table
CREATE TABLE IF NOT EXISTS user_behavior (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    sessions INT NOT NULL DEFAULT 0 CHECK (sessions >= 0),
    page_views INT NOT NULL DEFAULT 0 CHECK (page_views >= 0),
    products_viewed INT NOT NULL DEFAULT 0 CHECK (products_viewed >= 0),
    add_to_cart_count INT NOT NULL DEFAULT 0 CHECK (add_to_cart_count >= 0),
    total_orders INT NOT NULL DEFAULT 0 CHECK (total_orders >= 0),
    total_spent DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (total_spent >= 0),
    average_order_value DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (average_order_value >= 0),
    last_order_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, metric_date)
);

COMMENT ON TABLE user_behavior IS 'Daily user engagement and purchase behavior metrics';
COMMENT ON COLUMN user_behavior.user_id IS 'Reference to the user';
COMMENT ON COLUMN user_behavior.metric_date IS 'Date for which metrics are calculated';
COMMENT ON COLUMN user_behavior.sessions IS 'Number of sessions on this date';
COMMENT ON COLUMN user_behavior.page_views IS 'Total page views';
COMMENT ON COLUMN user_behavior.products_viewed IS 'Distinct products viewed';
COMMENT ON COLUMN user_behavior.add_to_cart_count IS 'Items added to cart';
COMMENT ON COLUMN user_behavior.total_orders IS 'Orders placed on this date';
COMMENT ON COLUMN user_behavior.total_spent IS 'Total amount spent';
COMMENT ON COLUMN user_behavior.average_order_value IS 'Average order value for this date';
COMMENT ON COLUMN user_behavior.last_order_date IS 'Most recent order date';

-- Cohort Analysis Table
CREATE TABLE IF NOT EXISTS cohort_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cohort_date DATE NOT NULL,
    cohort_size INT NOT NULL DEFAULT 0 CHECK (cohort_size >= 0),
    retention_day_1 INT NOT NULL DEFAULT 0 CHECK (retention_day_1 >= 0),
    retention_day_7 INT NOT NULL DEFAULT 0 CHECK (retention_day_7 >= 0),
    retention_day_14 INT NOT NULL DEFAULT 0 CHECK (retention_day_14 >= 0),
    retention_day_30 INT NOT NULL DEFAULT 0 CHECK (retention_day_30 >= 0),
    retention_day_60 INT NOT NULL DEFAULT 0 CHECK (retention_day_60 >= 0),
    retention_day_90 INT NOT NULL DEFAULT 0 CHECK (retention_day_90 >= 0),
    revenue_day_30 DECIMAL(12, 2) NOT NULL DEFAULT 0,
    revenue_day_90 DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cohort_date)
);

COMMENT ON TABLE cohort_analysis IS 'User cohort retention and revenue analysis';
COMMENT ON COLUMN cohort_analysis.cohort_date IS 'Registration date defining the cohort';
COMMENT ON COLUMN cohort_analysis.cohort_size IS 'Number of users who registered on this date';
COMMENT ON COLUMN cohort_analysis.retention_day_1 IS 'Users active 1 day after registration';
COMMENT ON COLUMN cohort_analysis.retention_day_7 IS 'Users active 7 days after registration';
COMMENT ON COLUMN cohort_analysis.retention_day_14 IS 'Users active 14 days after registration';
COMMENT ON COLUMN cohort_analysis.retention_day_30 IS 'Users active 30 days after registration';
COMMENT ON COLUMN cohort_analysis.retention_day_60 IS 'Users active 60 days after registration';
COMMENT ON COLUMN cohort_analysis.retention_day_90 IS 'Users active 90 days after registration';
COMMENT ON COLUMN cohort_analysis.revenue_day_30 IS 'Revenue from cohort within first 30 days';
COMMENT ON COLUMN cohort_analysis.revenue_day_90 IS 'Revenue from cohort within first 90 days';

-- ============================================
-- INDEXES
-- ============================================

-- Events indexes
CREATE INDEX IF NOT EXISTS idx_events_type ON events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_source ON events(event_source);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_session_id ON events(session_id);
CREATE INDEX IF NOT EXISTS idx_events_order_id ON events(order_id);
CREATE INDEX IF NOT EXISTS idx_events_product_id ON events(product_id);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events(created_at);
CREATE INDEX IF NOT EXISTS idx_events_data ON events USING GIN(event_data);

-- BRIN index for time-series event data (very efficient for append-only)
CREATE INDEX IF NOT EXISTS idx_events_created_at_brin ON events USING BRIN(created_at);

-- Metrics indexes
CREATE INDEX IF NOT EXISTS idx_metrics_date ON metrics(metric_date);
CREATE INDEX IF NOT EXISTS idx_metrics_type ON metrics(metric_type);

-- Product performance indexes
CREATE INDEX IF NOT EXISTS idx_pp_product_id ON product_performance(product_id);
CREATE INDEX IF NOT EXISTS idx_pp_metric_date ON product_performance(metric_date);
CREATE INDEX IF NOT EXISTS idx_pp_revenue ON product_performance(revenue DESC);

-- User behavior indexes
CREATE INDEX IF NOT EXISTS idx_ub_user_id ON user_behavior(user_id);
CREATE INDEX IF NOT EXISTS idx_ub_metric_date ON user_behavior(metric_date);
CREATE INDEX IF NOT EXISTS idx_ub_total_spent ON user_behavior(total_spent DESC);

-- Cohort analysis indexes
CREATE INDEX IF NOT EXISTS idx_cohort_date ON cohort_analysis(cohort_date);

-- Composite indexes for common analytics queries
CREATE INDEX IF NOT EXISTS idx_events_type_date ON events(event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_user_type ON events(user_id, event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pp_product_date ON product_performance(product_id, metric_date DESC);
CREATE INDEX IF NOT EXISTS idx_ub_user_date ON user_behavior(user_id, metric_date DESC);

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

CREATE TRIGGER trg_metrics_updated_at 
    BEFORE UPDATE ON metrics 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_pp_updated_at 
    BEFORE UPDATE ON product_performance 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_ub_updated_at 
    BEFORE UPDATE ON user_behavior 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_cohort_updated_at 
    BEFORE UPDATE ON cohort_analysis 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VIEWS
-- ============================================

-- Daily sales summary
CREATE OR REPLACE VIEW daily_sales_summary AS
SELECT 
    metric_date,
    total_orders,
    total_revenue,
    average_order_value,
    conversion_rate,
    cart_abandonment_rate,
    total_page_views,
    total_sessions,
    new_users,
    active_users
FROM metrics
WHERE metric_type = 'DAILY_SUMMARY'
ORDER BY metric_date DESC;

COMMENT ON VIEW daily_sales_summary IS 'Daily business KPIs from pre-aggregated metrics';

-- Top products by revenue
CREATE OR REPLACE VIEW top_products_by_revenue AS
SELECT 
    product_id,
    SUM(revenue) AS total_revenue,
    SUM(purchases) AS total_purchases,
    SUM(units_sold) AS total_units_sold,
    SUM(views) AS total_views,
    AVG(average_rating)::DECIMAL(3,2) AS avg_rating,
    SUM(review_count) AS total_reviews,
    CASE WHEN SUM(views) > 0 
         THEN (SUM(purchases)::DECIMAL / SUM(views))::DECIMAL(5,4)
         ELSE 0 
    END AS view_to_purchase_rate
FROM product_performance
GROUP BY product_id
ORDER BY total_revenue DESC;

COMMENT ON VIEW top_products_by_revenue IS 'Product ranking by total revenue with conversion metrics';

-- User lifetime value
CREATE OR REPLACE VIEW user_lifetime_value AS
SELECT 
    user_id,
    COUNT(DISTINCT metric_date) AS active_days,
    SUM(total_orders) AS lifetime_orders,
    SUM(total_spent) AS lifetime_value,
    AVG(average_order_value)::DECIMAL(10,2) AS avg_order_value,
    MIN(metric_date) AS first_activity,
    MAX(metric_date) AS last_activity,
    AGE(MAX(metric_date), MIN(metric_date)) AS customer_lifespan
FROM user_behavior
GROUP BY user_id
ORDER BY lifetime_value DESC;

COMMENT ON VIEW user_lifetime_value IS 'Customer lifetime value and engagement metrics';

-- Cohort retention rates
CREATE OR REPLACE VIEW cohort_retention_rates AS
SELECT 
    cohort_date,
    cohort_size,
    CASE WHEN cohort_size > 0 THEN (retention_day_1::DECIMAL / cohort_size * 100)::DECIMAL(5,2) ELSE 0 END AS day_1_pct,
    CASE WHEN cohort_size > 0 THEN (retention_day_7::DECIMAL / cohort_size * 100)::DECIMAL(5,2) ELSE 0 END AS day_7_pct,
    CASE WHEN cohort_size > 0 THEN (retention_day_14::DECIMAL / cohort_size * 100)::DECIMAL(5,2) ELSE 0 END AS day_14_pct,
    CASE WHEN cohort_size > 0 THEN (retention_day_30::DECIMAL / cohort_size * 100)::DECIMAL(5,2) ELSE 0 END AS day_30_pct,
    CASE WHEN cohort_size > 0 THEN (retention_day_60::DECIMAL / cohort_size * 100)::DECIMAL(5,2) ELSE 0 END AS day_60_pct,
    CASE WHEN cohort_size > 0 THEN (retention_day_90::DECIMAL / cohort_size * 100)::DECIMAL(5,2) ELSE 0 END AS day_90_pct,
    revenue_day_30,
    revenue_day_90
FROM cohort_analysis
ORDER BY cohort_date DESC;

COMMENT ON VIEW cohort_retention_rates IS 'Cohort retention percentages and revenue';

-- ============================================
-- DATA RETENTION
-- ============================================

-- Function to archive old events (call periodically)
CREATE OR REPLACE FUNCTION archive_old_events(retention_days INT DEFAULT 365)
RETURNS INT AS $$
DECLARE
    deleted_count INT;
BEGIN
    DELETE FROM events 
    WHERE created_at < CURRENT_DATE - (retention_days || ' days')::INTERVAL;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION archive_old_events IS 'Delete events older than specified retention period (default 365 days)';
