# Database Migration Runner for Windows
# This script runs database migrations in the correct order

param(
    [string]$Command = "run",
    [string]$MigrationFile = ""
)

# Configuration
$DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
$DB_USER = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
$DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }

# Database names
$USER_DB = if ($env:USER_DB) { $env:USER_DB } else { "user_db" }
$PRODUCT_DB = if ($env:PRODUCT_DB) { $env:PRODUCT_DB } else { "product_db" }
$ORDER_DB = if ($env:ORDER_DB) { $env:ORDER_DB } else { "order_db" }
$INVENTORY_DB = if ($env:INVENTORY_DB) { $env:INVENTORY_DB } else { "inventory_db" }
$CART_DB = if ($env:CART_DB) { $env:CART_DB } else { "cart_db" }
$PAYMENT_DB = if ($env:PAYMENT_DB) { $env:PAYMENT_DB } else { "payment_db" }
$ANALYTICS_DB = if ($env:ANALYTICS_DB) { $env:ANALYTICS_DB } else { "analytics_db" }

# Logging functions
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Function to run migration on a specific database
function Run-Migration {
    param(
        [string]$DatabaseName,
        [string]$MigrationFile
    )
    
    Write-Info "Running migration $MigrationFile on database $DatabaseName"
    
    $env:PGPASSWORD = $DB_PASSWORD
    $result = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DatabaseName -f $MigrationFile
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Migration $MigrationFile completed successfully on $DatabaseName"
    } else {
        Write-Error "Migration $MigrationFile failed on $DatabaseName"
        exit 1
    }
}

# Function to create database if it doesn't exist
function Create-Database {
    param([string]$DatabaseName)
    
    Write-Info "Checking if database $DatabaseName exists"
    
    $env:PGPASSWORD = $DB_PASSWORD
    $databases = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -lqt
    
    if ($databases -match $DatabaseName) {
        Write-Info "Database $DatabaseName already exists"
    } else {
        Write-Info "Creating database $DatabaseName"
        $result = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -c "CREATE DATABASE $DatabaseName;"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Info "Database $DatabaseName created successfully"
        } else {
            Write-Error "Failed to create database $DatabaseName"
            exit 1
        }
    }
}

# Function to run all migrations
function Run-All-Migrations {
    Write-Info "Starting database migrations"
    Write-Info "========================================="
    
    # Create all databases
    Create-Database $USER_DB
    Create-Database $PRODUCT_DB
    Create-Database $ORDER_DB
    Create-Database $INVENTORY_DB
    Create-Database $CART_DB
    Create-Database $PAYMENT_DB
    Create-Database $ANALYTICS_DB
    
    Write-Info "========================================="
    Write-Info "All databases created/verified"
    Write-Info "========================================="
    
    # Run migrations in order
    $migrations = @(
        "001-initial-schema.sql",
        "002-add-indexes.sql",
        "003-add-views.sql",
        "004-add-constraints.sql"
    )
    
    foreach ($migration in $migrations) {
        Write-Info "========================================="
        Write-Info "Running migration: $migration"
        Write-Info "========================================="
        
        # Run migration on each database
        $databases = @($USER_DB, $PRODUCT_DB, $ORDER_DB, $INVENTORY_DB, $CART_DB, $PAYMENT_DB, $ANALYTICS_DB)
        foreach ($db in $databases) {
            Run-Migration -DatabaseName $db -MigrationFile $migration
        }
        
        Write-Info "Migration $migration completed on all databases"
    }
    
    Write-Info "========================================="
    Write-Info "All migrations completed successfully!"
    Write-Info "========================================="
}

# Function to check migration status
function Check-Migration-Status {
    Write-Info "Checking migration status"
    
    # Check if migration tracking table exists in each database
    $databases = @($USER_DB, $PRODUCT_DB, $ORDER_DB, $INVENTORY_DB, $CART_DB, $PAYMENT_DB, $ANALYTICS_DB)
    
    foreach ($db in $databases) {
        Write-Info "Database: $db"
        
        # Check for key tables to verify migration status
        $tablesToCheck = @("users", "products", "orders", "inventory", "carts", "transactions", "events")
        
        foreach ($table in $tablesToCheck) {
            $env:PGPASSWORD = $DB_PASSWORD
            $tables = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db -c "\dt"
            
            if ($tables -match $table) {
                Write-Info "  ✓ Table $table exists"
            } else {
                Write-Warn "  ✗ Table $table not found"
            }
        }
    }
}

# Function to create migration tracking table
function Create-Migration-Tracking {
    Write-Info "Creating migration tracking table"
    
    $databases = @($USER_DB, $PRODUCT_DB, $ORDER_DB, $INVENTORY_DB, $CART_DB, $PAYMENT_DB, $ANALYTICS_DB)
    
    foreach ($db in $databases) {
        $env:PGPASSWORD = $DB_PASSWORD
        $query = @"
CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(migration_name)
);
"@
        
        $result = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $db -c $query
        
        if ($LASTEXITCODE -eq 0) {
            Write-Info "Migration tracking table created/verified in $db"
        } else {
            Write-Warn "Failed to create migration tracking table in $db"
        }
    }
}

# Main script execution
switch ($Command.ToLower()) {
    "run" {
        Run-All-Migrations
    }
    "rollback" {
        if ([string]::IsNullOrEmpty($MigrationFile)) {
            Write-Error "Please specify migration file to rollback"
            exit 1
        }
        Write-Warn "Rollback functionality not implemented in this version"
        Write-Warn "Please restore from backup or manually drop objects"
    }
    "status" {
        Check-Migration-Status
    }
    "create-tracking" {
        Create-Migration-Tracking
    }
    "help" {
        Write-Host "Database Migration Script for Windows" -ForegroundColor Cyan
        Write-Host "Usage: .\run-migrations.ps1 [command] [migration_file]" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Commands:" -ForegroundColor Cyan
        Write-Host "  run              Run all migrations"
        Write-Host "  rollback [file]  Rollback a specific migration"
        Write-Host "  status           Check migration status"
        Write-Host "  create-tracking  Create migration tracking tables"
        Write-Host "  help             Show this help message"
        Write-Host ""
        Write-Host "Environment variables:" -ForegroundColor Cyan
        Write-Host "  DB_HOST          Database host (default: localhost)"
        Write-Host "  DB_PORT          Database port (default: 5432)"
        Write-Host "  DB_USER          Database user (default: postgres)"
        Write-Host "  DB_PASSWORD      Database password (default: postgres)"
        Write-Host "  USER_DB          User database name (default: user_db)"
        Write-Host "  PRODUCT_DB       Product database name (default: product_db)"
        Write-Host "  ORDER_DB         Order database name (default: order_db)"
        Write-Host "  INVENTORY_DB     Inventory database name (default: inventory_db)"
        Write-Host "  CART_DB          Cart database name (default: cart_db)"
        Write-Host "  PAYMENT_DB       Payment database name (default: payment_db)"
        Write-Host "  ANALYTICS_DB     Analytics database name (default: analytics_db)"
    }
    default {
        Write-Info "Running all migrations (default action)"
        Run-All-Migrations
    }
}