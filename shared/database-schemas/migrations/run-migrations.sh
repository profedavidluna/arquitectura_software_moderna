#!/bin/bash

# Database Migration Runner
# This script runs database migrations in the correct order

set -e

# Configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}

# Database names
USER_DB=${USER_DB:-user_db}
PRODUCT_DB=${PRODUCT_DB:-product_db}
ORDER_DB=${ORDER_DB:-order_db}
INVENTORY_DB=${INVENTORY_DB:-inventory_db}
CART_DB=${CART_DB:-cart_db}
PAYMENT_DB=${PAYMENT_DB:-payment_db}
ANALYTICS_DB=${ANALYTICS_DB:-analytics_db}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to run migration on a specific database
run_migration() {
    local db_name=$1
    local migration_file=$2
    
    log_info "Running migration $migration_file on database $db_name"
    
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db_name -f $migration_file
    
    if [ $? -eq 0 ]; then
        log_info "Migration $migration_file completed successfully on $db_name"
    else
        log_error "Migration $migration_file failed on $db_name"
        exit 1
    fi
}

# Function to create database if it doesn't exist
create_database() {
    local db_name=$1
    
    log_info "Checking if database $db_name exists"
    
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -lqt | cut -d \| -f 1 | grep -qw $db_name; then
        log_info "Database $db_name already exists"
    else
        log_info "Creating database $db_name"
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -c "CREATE DATABASE $db_name;"
        
        if [ $? -eq 0 ]; then
            log_info "Database $db_name created successfully"
        else
            log_error "Failed to create database $db_name"
            exit 1
        fi
    fi
}

# Main migration function
run_all_migrations() {
    log_info "Starting database migrations"
    log_info "========================================="
    
    # Create all databases
    create_database $USER_DB
    create_database $PRODUCT_DB
    create_database $ORDER_DB
    create_database $INVENTORY_DB
    create_database $CART_DB
    create_database $PAYMENT_DB
    create_database $ANALYTICS_DB
    
    log_info "========================================="
    log_info "All databases created/verified"
    log_info "========================================="
    
    # Run migrations in order
    local migrations=(
        "001-initial-schema.sql"
        "002-add-indexes.sql"
        "003-add-views.sql"
        "004-add-constraints.sql"
    )
    
    for migration in "${migrations[@]}"; do
        log_info "========================================="
        log_info "Running migration: $migration"
        log_info "========================================="
        
        # Run migration on each database
        for db in $USER_DB $PRODUCT_DB $ORDER_DB $INVENTORY_DB $CART_DB $PAYMENT_DB $ANALYTICS_DB; do
            run_migration $db $migration
        done
        
        log_info "Migration $migration completed on all databases"
    done
    
    log_info "========================================="
    log_info "All migrations completed successfully!"
    log_info "========================================="
}

# Function to rollback a specific migration
rollback_migration() {
    local migration_file=$1
    
    log_warn "Rolling back migration: $migration_file"
    log_warn "This will drop all tables, indexes, and views created by this migration"
    
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Rollback cancelled"
        exit 0
    fi
    
    # Note: In a real implementation, you would have rollback scripts
    # For now, we'll just show a warning
    log_warn "Rollback functionality not implemented in this version"
    log_warn "Please restore from backup or manually drop objects"
}

# Function to check migration status
check_migration_status() {
    log_info "Checking migration status"
    
    # Check if migration tracking table exists in each database
    for db in $USER_DB $PRODUCT_DB $ORDER_DB $INVENTORY_DB $CART_DB $PAYMENT_DB $ANALYTICS_DB; do
        log_info "Database: $db"
        
        # Check for key tables to verify migration status
        tables_to_check=("users" "products" "orders" "inventory" "carts" "transactions" "events")
        
        for table in "${tables_to_check[@]}"; do
            if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db -c "\dt" | grep -q $table; then
                log_info "  ✓ Table $table exists"
            else
                log_warn "  ✗ Table $table not found"
            fi
        done
    done
}

# Function to create migration tracking table (for future migrations)
create_migration_tracking() {
    log_info "Creating migration tracking table"
    
    # This would create a table to track which migrations have been run
    # For now, we'll just create a simple version
    for db in $USER_DB $PRODUCT_DB $ORDER_DB $INVENTORY_DB $CART_DB $PAYMENT_DB $ANALYTICS_DB; do
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db << EOF
CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(migration_name)
);
EOF
        log_info "Migration tracking table created/verified in $db"
    done
}

# Parse command line arguments
case "$1" in
    "run")
        run_all_migrations
        ;;
    "rollback")
        if [ -z "$2" ]; then
            log_error "Please specify migration file to rollback"
            exit 1
        fi
        rollback_migration "$2"
        ;;
    "status")
        check_migration_status
        ;;
    "create-tracking")
        create_migration_tracking
        ;;
    "help"|"--help"|"-h")
        echo "Database Migration Script"
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  run              Run all migrations"
        echo "  rollback [file]  Rollback a specific migration"
        echo "  status           Check migration status"
        echo "  create-tracking  Create migration tracking tables"
        echo "  help             Show this help message"
        echo ""
        echo "Environment variables:"
        echo "  DB_HOST          Database host (default: localhost)"
        echo "  DB_PORT          Database port (default: 5432)"
        echo "  DB_USER          Database user (default: postgres)"
        echo "  DB_PASSWORD      Database password (default: postgres)"
        echo "  USER_DB          User database name (default: user_db)"
        echo "  PRODUCT_DB       Product database name (default: product_db)"
        echo "  ORDER_DB         Order database name (default: order_db)"
        echo "  INVENTORY_DB     Inventory database name (default: inventory_db)"
        echo "  CART_DB          Cart database name (default: cart_db)"
        echo "  PAYMENT_DB       Payment database name (default: payment_db)"
        echo "  ANALYTICS_DB     Analytics database name (default: analytics_db)"
        ;;
    *)
        log_info "Running all migrations (default action)"
        run_all_migrations
        ;;
esac