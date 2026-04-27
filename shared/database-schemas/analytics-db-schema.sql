-- Analytics Database Schema

-- Events Table
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    user_id UUID,
    order_id UUID,
    product_id UUID,
    event_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Daily Metrics Table
CREATE TABLE daily_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL,
    total_orders INT DEFAULT 0,
    total_revenue DECIMAL(12, 2) DEFAULT 0,
    total_users INT DEFAULT 0,
    new_users INT DEFAULT 0,
    average_order_value DECIMAL(10, 2) DEFAULT 0,
    conversion_rate DECIMAL(5, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product Performance Table
CREATE TABLE product_performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    views INT DEFAULT 0,
    purchases INT DEFAULT 0,
    revenue DECIMAL(12, 2) DEFAULT 0,
    average_rating DECIMAL(3, 2),
    review_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Behavior Table
CREATE TABLE user_behavior (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    total_orders INT DEFAULT 0,
    total_spent DECIMAL(12, 2) DEFAULT 0,
    average_order_value DECIMAL(10, 2) DEFAULT 0,
    last_order_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Cohort Analysis Table
CREATE TABLE cohort_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cohort_date DATE NOT NULL,
    cohort_size INT DEFAULT 0,
    retention_day_0 INT DEFAULT 0,
    retention_day_7 INT DEFAULT 0,
    retention_day_30 INT DEFAULT 0,
    retention_day_90 INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_events_type ON events(event_type);
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_order_id ON events(order_id);
CREATE INDEX idx_events_product_id ON events(product_id);
CREATE INDEX idx_events_created_at ON events(created_at);
CREATE INDEX idx_daily_metrics_date ON daily_metrics(metric_date);
CREATE INDEX idx_product_performance_product_id ON product_performance(product_id);
CREATE INDEX idx_product_performance_date ON product_performance(metric_date);
CREATE INDEX idx_user_behavior_user_id ON user_behavior(user_id);
CREATE INDEX idx_user_behavior_date ON user_behavior(metric_date);
CREATE INDEX idx_cohort_analysis_date ON cohort_analysis(cohort_date);

-- Views for analytics
CREATE VIEW daily_sales_summary AS
SELECT 
    metric_date,
    total_orders,
    total_revenue,
    total_users,
    new_users,
    average_order_value,
    conversion_rate
FROM daily_metrics
ORDER BY metric_date DESC;

CREATE VIEW top_products AS
SELECT 
    product_id,
    SUM(purchases) as total_purchases,
    SUM(revenue) as total_revenue,
    AVG(average_rating) as avg_rating,
    SUM(review_count) as total_reviews
FROM product_performance
GROUP BY product_id
ORDER BY total_revenue DESC;
