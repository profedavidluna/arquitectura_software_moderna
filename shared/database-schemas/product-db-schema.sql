-- ============================================
-- PRODUCT DATABASE SCHEMA (product_db)
-- ============================================
-- Service: Product Service
-- Purpose: Product catalog, categories, and reviews
-- Port: 5433
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLES
-- ============================================

-- Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    image_url VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE categories IS 'Product categories with hierarchical support via parent_id';
COMMENT ON COLUMN categories.id IS 'Unique identifier for the category';
COMMENT ON COLUMN categories.name IS 'Display name of the category';
COMMENT ON COLUMN categories.slug IS 'URL-friendly slug for the category';
COMMENT ON COLUMN categories.description IS 'Optional description of the category';
COMMENT ON COLUMN categories.parent_id IS 'Reference to parent category for hierarchy (NULL = root)';
COMMENT ON COLUMN categories.image_url IS 'URL to category image';
COMMENT ON COLUMN categories.sort_order IS 'Display order within the same level';
COMMENT ON COLUMN categories.is_active IS 'Whether the category is visible in the catalog';
COMMENT ON COLUMN categories.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN categories.updated_at IS 'Record last update timestamp';

-- Products Table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(280) NOT NULL UNIQUE,
    description TEXT,
    short_description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    compare_at_price DECIMAL(10, 2) CHECK (compare_at_price IS NULL OR compare_at_price > 0),
    cost_price DECIMAL(10, 2) CHECK (cost_price IS NULL OR cost_price >= 0),
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    sku VARCHAR(100) UNIQUE NOT NULL,
    barcode VARCHAR(50),
    weight DECIMAL(8, 2) CHECK (weight IS NULL OR weight >= 0),
    weight_unit VARCHAR(10) DEFAULT 'kg' CHECK (weight_unit IN ('kg', 'lb', 'g', 'oz')),
    image_url VARCHAR(500),
    images JSONB DEFAULT '[]'::jsonb,
    tags TEXT[] DEFAULT '{}',
    attributes JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE products IS 'Product catalog with pricing, categorization, and metadata';
COMMENT ON COLUMN products.id IS 'Unique identifier for the product';
COMMENT ON COLUMN products.name IS 'Product display name';
COMMENT ON COLUMN products.slug IS 'URL-friendly slug for the product';
COMMENT ON COLUMN products.description IS 'Full product description (supports HTML/Markdown)';
COMMENT ON COLUMN products.short_description IS 'Brief product summary for listings';
COMMENT ON COLUMN products.price IS 'Current selling price';
COMMENT ON COLUMN products.compare_at_price IS 'Original price for showing discounts (strikethrough price)';
COMMENT ON COLUMN products.cost_price IS 'Cost price for margin calculations';
COMMENT ON COLUMN products.category_id IS 'Reference to the product category';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - unique product identifier';
COMMENT ON COLUMN products.barcode IS 'Product barcode (EAN, UPC, etc.)';
COMMENT ON COLUMN products.weight IS 'Product weight for shipping calculations';
COMMENT ON COLUMN products.weight_unit IS 'Unit of weight measurement';
COMMENT ON COLUMN products.image_url IS 'Primary product image URL';
COMMENT ON COLUMN products.images IS 'JSON array of additional image URLs';
COMMENT ON COLUMN products.tags IS 'Array of searchable tags';
COMMENT ON COLUMN products.attributes IS 'JSON object of product attributes (color, size, material, etc.)';
COMMENT ON COLUMN products.is_active IS 'Whether the product is visible in the catalog';
COMMENT ON COLUMN products.is_featured IS 'Whether the product appears in featured sections';
COMMENT ON COLUMN products.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN products.updated_at IS 'Record last update timestamp';

-- Product Reviews Table
CREATE TABLE IF NOT EXISTS product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(200),
    review_text TEXT,
    is_verified_purchase BOOLEAN NOT NULL DEFAULT false,
    is_approved BOOLEAN NOT NULL DEFAULT false,
    helpful_count INT NOT NULL DEFAULT 0 CHECK (helpful_count >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, user_id)
);

COMMENT ON TABLE product_reviews IS 'Customer reviews and ratings for products';
COMMENT ON COLUMN product_reviews.id IS 'Unique identifier for the review';
COMMENT ON COLUMN product_reviews.product_id IS 'Reference to the reviewed product';
COMMENT ON COLUMN product_reviews.user_id IS 'Reference to the user who wrote the review (cross-service)';
COMMENT ON COLUMN product_reviews.rating IS 'Star rating from 1 to 5';
COMMENT ON COLUMN product_reviews.title IS 'Review title/headline';
COMMENT ON COLUMN product_reviews.review_text IS 'Full review text';
COMMENT ON COLUMN product_reviews.is_verified_purchase IS 'Whether the reviewer purchased the product';
COMMENT ON COLUMN product_reviews.is_approved IS 'Whether the review has been moderated and approved';
COMMENT ON COLUMN product_reviews.helpful_count IS 'Number of users who found this review helpful';
COMMENT ON COLUMN product_reviews.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN product_reviews.updated_at IS 'Record last update timestamp';

-- ============================================
-- INDEXES
-- ============================================

-- Categories indexes
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_is_active ON categories(is_active);
CREATE INDEX IF NOT EXISTS idx_categories_sort_order ON categories(sort_order);

-- Products indexes
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_is_featured ON products(is_featured);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);
CREATE INDEX IF NOT EXISTS idx_products_tags ON products USING GIN(tags);

-- Full-text search index
CREATE INDEX IF NOT EXISTS idx_products_search ON products 
    USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '') || ' ' || COALESCE(short_description, '')));

-- Product reviews indexes
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON product_reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON product_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON product_reviews(rating);
CREATE INDEX IF NOT EXISTS idx_reviews_is_approved ON product_reviews(is_approved);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON product_reviews(created_at);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_products_category_active ON products(category_id, is_active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_active_price ON products(is_active, price) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_reviews_product_approved ON product_reviews(product_id, is_approved, created_at DESC);

-- ============================================
-- CONSTRAINTS
-- ============================================

-- SKU format (alphanumeric with hyphens)
ALTER TABLE products ADD CONSTRAINT IF NOT EXISTS chk_sku_format 
    CHECK (sku ~ '^[A-Za-z0-9\-]+$');

-- Slug format (lowercase alphanumeric with hyphens)
ALTER TABLE products ADD CONSTRAINT IF NOT EXISTS chk_product_slug_format 
    CHECK (slug ~ '^[a-z0-9\-]+$');

ALTER TABLE categories ADD CONSTRAINT IF NOT EXISTS chk_category_slug_format 
    CHECK (slug ~ '^[a-z0-9\-]+$');

-- Compare price must be greater than selling price
ALTER TABLE products ADD CONSTRAINT IF NOT EXISTS chk_compare_price 
    CHECK (compare_at_price IS NULL OR compare_at_price > price);

-- Prevent self-referencing category
ALTER TABLE categories ADD CONSTRAINT IF NOT EXISTS chk_no_self_parent 
    CHECK (parent_id != id);

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

CREATE TRIGGER trg_products_updated_at 
    BEFORE UPDATE ON products 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_categories_updated_at 
    BEFORE UPDATE ON categories 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_reviews_updated_at 
    BEFORE UPDATE ON product_reviews 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VIEWS
-- ============================================

-- Product catalog view with category and rating info
CREATE OR REPLACE VIEW product_catalog AS
SELECT 
    p.id,
    p.name,
    p.slug,
    p.short_description,
    p.price,
    p.compare_at_price,
    p.sku,
    p.image_url,
    p.is_active,
    p.is_featured,
    p.tags,
    p.created_at,
    c.name AS category_name,
    c.slug AS category_slug,
    COALESCE(r.average_rating, 0) AS average_rating,
    COALESCE(r.review_count, 0) AS review_count
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN (
    SELECT product_id, 
           AVG(rating)::DECIMAL(3,2) AS average_rating, 
           COUNT(*) AS review_count
    FROM product_reviews 
    WHERE is_approved = true
    GROUP BY product_id
) r ON p.id = r.product_id;

COMMENT ON VIEW product_catalog IS 'Product catalog with category info and aggregated ratings';

-- Top rated products
CREATE OR REPLACE VIEW top_rated_products AS
SELECT 
    p.id,
    p.name,
    p.slug,
    p.price,
    c.name AS category,
    AVG(pr.rating)::DECIMAL(3,2) AS average_rating,
    COUNT(pr.id) AS review_count
FROM products p
JOIN categories c ON p.category_id = c.id
JOIN product_reviews pr ON p.id = pr.product_id AND pr.is_approved = true
WHERE p.is_active = true
GROUP BY p.id, p.name, p.slug, p.price, c.name
HAVING COUNT(pr.id) >= 3
ORDER BY average_rating DESC, review_count DESC;

COMMENT ON VIEW top_rated_products IS 'Products with 3+ approved reviews, sorted by rating';
