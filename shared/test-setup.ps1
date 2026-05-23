# Test script to verify docker-compose setup
Write-Host "=== Testing Docker Compose Setup ===" -ForegroundColor Cyan

# Test 1: Check docker-compose files exist
Write-Host "`n1. Checking docker-compose files..." -ForegroundColor Yellow
if (Test-Path "docker-compose.yml") {
    Write-Host "   ✓ docker-compose.yml exists" -ForegroundColor Green
} else {
    Write-Host "   ✗ docker-compose.yml missing" -ForegroundColor Red
}

if (Test-Path "docker-compose.override.yml") {
    Write-Host "   ✓ docker-compose.override.yml exists" -ForegroundColor Green
} else {
    Write-Host "   ✗ docker-compose.override.yml missing" -ForegroundColor Red
}

if (Test-Path "STARTUP_GUIDE.md") {
    Write-Host "   ✓ STARTUP_GUIDE.md exists" -ForegroundColor Green
} else {
    Write-Host "   ✗ STARTUP_GUIDE.md missing" -ForegroundColor Red
}

# Test 2: Validate docker-compose configuration
Write-Host "`n2. Validating docker-compose configuration..." -ForegroundColor Yellow
try {
    $config = docker-compose -f docker-compose.yml -f docker-compose.override.yml config 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✓ docker-compose configuration is valid" -ForegroundColor Green
        
        # Count services
        $serviceCount = ($config | Select-String -Pattern "^  [a-zA-Z]" | Measure-Object).Count
        Write-Host "   ✓ Found $serviceCount services in configuration" -ForegroundColor Green
    } else {
        Write-Host "   ✗ docker-compose configuration validation failed" -ForegroundColor Red
        Write-Host "   Error output: $config" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Failed to validate configuration: $_" -ForegroundColor Red
}

# Test 3: Check key directories
Write-Host "`n3. Checking required directories..." -ForegroundColor Yellow
$directories = @(
    "monitoring/grafana-dashboards",
    "monitoring/grafana-datasources",
    "keycloak-config",
    "database-schemas",
    "kafka",
    "logging",
    "tracing"
)

foreach ($dir in $directories) {
    if (Test-Path $dir) {
        Write-Host "   ✓ $dir exists" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ $dir missing (may be created later)" -ForegroundColor Yellow
    }
}

# Test 4: Check key files
Write-Host "`n4. Checking key configuration files..." -ForegroundColor Yellow
$files = @(
    "logstash.conf",
    "prometheus.yml",
    "monitoring/grafana-dashboards/dashboard.yml",
    "monitoring/grafana-datasources/datasource.yml"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "   ✓ $file exists" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ $file missing" -ForegroundColor Yellow
    }
}

# Summary
Write-Host "`n=== Setup Test Complete ===" -ForegroundColor Cyan
Write-Host "`nNext steps:" -ForegroundColor White
Write-Host "1. Review STARTUP_GUIDE.md for detailed instructions" -ForegroundColor White
Write-Host "2. Start the stack: docker-compose up -d" -ForegroundColor White
Write-Host "3. Verify services: docker-compose ps" -ForegroundColor White
Write-Host "4. Access web interfaces:" -ForegroundColor White
Write-Host "   - Keycloak: http://localhost:8180" -ForegroundColor White
Write-Host "   - Grafana: http://localhost:3000" -ForegroundColor White
Write-Host "   - Kafka UI: http://localhost:8080" -ForegroundColor White
Write-Host "   - Adminer: http://localhost:8081" -ForegroundColor White