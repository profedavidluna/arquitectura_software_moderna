# Monitoring & Observability Guide

## Overview

This document describes the monitoring and observability stack for the ecommerce platform, including metrics collection, alerting, dashboards, logging, and distributed tracing.

## Stack Components

| Component | Purpose | Technology |
|-----------|---------|------------|
| Metrics Collection | Time-series metrics | Prometheus |
| Visualization | Dashboards & graphs | Grafana |
| Alerting | Threshold-based alerts | Prometheus Alertmanager |
| Centralized Logging | Log aggregation | ELK Stack (Elasticsearch, Logstash, Kibana) |
| Distributed Tracing | Request flow tracking | Jaeger |
| System Metrics | Host-level monitoring | Node Exporter |
| Container Metrics | Docker container stats | cAdvisor |

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Alertmanager | http://localhost:9093 | - |
| Kibana | http://localhost:5601 | - |
| Jaeger | http://localhost:16686 | - |

---

## Prometheus Configuration

### Configuration Files

| File | Purpose |
|------|---------|
| `shared/prometheus.yml` | Main Prometheus configuration with scrape targets |
| `shared/monitoring/alert_rules.yml` | Alert rule definitions |
| `shared/monitoring/grafana-datasources/datasource.yml` | Grafana datasource provisioning |
| `shared/monitoring/grafana-dashboards/dashboard.yml` | Grafana dashboard provisioning |

### Global Settings

| Setting | Value |
|---------|-------|
| Scrape Interval | 15s (services), 30s (infrastructure) |
| Evaluation Interval | 15s |
| Scrape Timeout | 5s (services), 10s (infrastructure) |
| Retention | 15 days |

### Scrape Targets

#### Microservices (8 services)

All microservices expose metrics via Spring Boot Actuator at `/actuator/prometheus`:

| Service | Host:Port | Job Name | Domain |
|---------|-----------|----------|--------|
| User Service | user-service:8081 | user-service | identity |
| Product Service | product-service:8082 | product-service | catalog |
| Cart Service | cart-service:8083 | cart-service | shopping |
| Payment Service | payment-service:8084 | payment-service | finance |
| Order Service | order-service:8085 | order-service | fulfillment |
| Inventory Service | inventory-service:8086 | inventory-service | warehouse |
| Notification Service | notification-service:8087 | notification-service | communication |
| Analytics Service | analytics-service:8088 | analytics-service | analytics |

#### Infrastructure Services

| Service | Host:Port | Job Name | Metrics Path |
|---------|-----------|----------|--------------|
| Prometheus | localhost:9090 | prometheus | /metrics |
| Kafka Exporter | kafka-exporter:9308 | kafka | /metrics |
| Zookeeper Exporter | zookeeper-exporter:9141 | zookeeper | /metrics |
| Redis Exporter | redis-exporter:9121 | redis | /metrics |
| Node Exporter | node-exporter:9100 | node | /metrics |
| cAdvisor | cadvisor:8080 | cadvisor | /metrics |
| Keycloak | keycloak:8080 | keycloak | /realms/ecommerce/metrics |
| API Gateway (Kong) | api-gateway:8001 | api-gateway | /metrics |
| Elasticsearch | elasticsearch:9200 | elasticsearch | /_prometheus/metrics |
| Logstash | logstash:9600 | logstash | /metrics |
| Kibana | kibana:5601 | kibana | /api/status |
| Jaeger | jaeger:14269 | jaeger | /metrics |
| Grafana | grafana:3000 | grafana | /metrics |

#### PostgreSQL Exporters (7 databases)

| Database | Job Name |
|----------|----------|
| User DB | postgres-user-db |
| Product DB | postgres-product-db |
| Order DB | postgres-order-db |
| Payment DB | postgres-payment-db |
| Inventory DB | postgres-inventory-db |
| Cart DB | postgres-cart-db |
| Analytics DB | postgres-analytics-db |

---

## Alert Rules

### Alert Categories & Thresholds

#### Application Alerts

| Alert | Expression | Threshold | Duration | Severity |
|-------|-----------|-----------|----------|----------|
| HighErrorRate | 5xx / total requests | > 5% | 5m | critical |
| ErrorRateWarning | 5xx / total requests | > 2% | 5m | warning |
| HighLatencyP95 | p95 response time | > 2s | 5m | critical |
| HighLatencyWarning | p95 response time | > 1s | 5m | warning |

#### Availability Alerts

| Alert | Expression | Threshold | Duration | Severity |
|-------|-----------|-----------|----------|----------|
| ServiceDown | up == 0 (microservices) | service unreachable | 30s | critical |
| InfrastructureServiceDown | up == 0 (infra) | service unreachable | 1m | critical |
| MultipleServicesDown | count(down) > 2 | multiple failures | 30s | critical |

#### Resource Alerts

| Alert | Expression | Threshold | Duration | Severity |
|-------|-----------|-----------|----------|----------|
| HighDiskUsage | disk used % | > 80% | 5m | warning |
| CriticalDiskUsage | disk used % | > 90% | 2m | critical |
| HighMemoryUsage | memory used % | > 90% | 5m | critical |
| MemoryUsageWarning | memory used % | > 80% | 5m | warning |
| HighCPUUsage | CPU used % | > 80% | 5m | warning |
| CriticalCPUUsage | CPU used % | > 95% | 2m | critical |

#### Database Alerts

| Alert | Expression | Threshold | Duration | Severity |
|-------|-----------|-----------|----------|----------|
| HighDatabaseConnectionUsage | connections / max | > 80% | 5m | warning |
| DatabaseDeadlocks | deadlock rate | > 0 | 2m | critical |
| SlowDatabaseQueries | max tx duration | > 30s | 2m | warning |

#### Messaging Alerts

| Alert | Expression | Threshold | Duration | Severity |
|-------|-----------|-----------|----------|----------|
| HighKafkaConsumerLag | consumer lag | > 10,000 msgs | 5m | warning |
| KafkaConsumerStalled | lag not decreasing | stalled | 10m | critical |
| KafkaBrokerDown | broker count | < 3 | 1m | critical |

#### Business Alerts

| Alert | Expression | Threshold | Duration | Severity |
|-------|-----------|-----------|----------|----------|
| LowOrderVolume | orders/hour | < 10 | 1h | warning |
| HighPaymentFailureRate | failed / total | > 10% | 30m | critical |
| HighCartAbandonmentRate | abandoned / created | > 80% | 2h | warning |

### Alert Labels

All alerts include:
- `severity`: critical, warning
- `category`: application, performance, availability, resource, database, messaging, business
- `team`: backend, platform

### Alert Annotations

All alerts include:
- `summary`: Brief description of the alert
- `description`: Detailed description with current value and threshold
- `dashboard`: Link to relevant Grafana dashboard
- `runbook`: Link to remediation runbook

---

## Grafana Dashboards

### Dashboard Provisioning

Dashboards are automatically provisioned from JSON files:
- **Config**: `shared/monitoring/grafana-dashboards/dashboard.yml`
- **Files**: `shared/monitoring/grafana-dashboards/*.json`
- **Mount path**: `/var/lib/grafana/dashboards/`

### 1. System Dashboard (UID: `system-dashboard`)

Infrastructure and system resource monitoring.

**Panels:**
| Panel | Type | Metric |
|-------|------|--------|
| CPU Usage (%) | Gauge | node_cpu_seconds_total |
| Memory Usage (%) | Gauge | node_memory_MemAvailable_bytes |
| Disk Usage (%) | Gauge | node_filesystem_avail_bytes |
| Available Memory | Stat | node_memory_MemAvailable_bytes |
| CPU Usage Over Time | Time Series | node_cpu_seconds_total |
| Memory Usage Over Time | Time Series | node_memory_MemAvailable_bytes |
| Network I/O | Time Series | node_network_receive/transmit_bytes_total |
| Database Connections | Time Series | pg_stat_activity_count |
| System Load Average | Time Series | node_load1/5/15 |
| Disk I/O | Time Series | node_disk_read/written_bytes_total |
| Redis Memory Usage | Time Series | redis_memory_used_bytes |
| Redis Connected Clients | Stat | redis_connected_clients |
| Elasticsearch Cluster Health | Stat | elasticsearch_cluster_health_status |

### 2. Application Dashboard (UID: `application-dashboard`)

Application performance monitoring for all microservices.

**Panels:**
| Panel | Type | Metric |
|-------|------|--------|
| Request Rate (req/s) | Time Series | http_requests_total |
| Error Rate (%) | Time Series | http_requests_total{status=~"5.."} |
| Response Latency P95 | Time Series | http_request_duration_seconds_bucket |
| Response Latency P99 | Time Series | http_request_duration_seconds_bucket |
| Active Requests | Time Series | http_requests_in_progress |
| Kafka Consumer Lag | Time Series | kafka_consumer_lag |
| Service Health Status | Stat | up |
| HTTP Status Codes | Time Series | http_requests_total by status |
| JVM Heap Memory | Time Series | jvm_memory_used_bytes |
| JVM Thread Count | Time Series | jvm_threads_live_threads |
| Kafka Messages Produced/Consumed | Time Series | kafka_producer/consumer records |

**Features:**
- Service filter variable (multi-select)
- Alert threshold lines on error rate (5%) and latency (2s) panels
- Color-coded service health indicators

### 3. Business Dashboard (UID: `business-dashboard`)

Business KPIs and e-commerce metrics.

**Panels:**
| Panel | Type | Metric |
|-------|------|--------|
| Orders Created (per hour) | Time Series | orders_created_total |
| Total Revenue | Stat | orders_total_amount |
| Average Order Value | Stat | orders_total_amount / orders_created_total |
| Revenue Over Time | Time Series | orders_total_amount |
| Conversion Rate | Gauge | orders / carts |
| Active Users | Stat | users_active_total |
| Payment Success Rate | Time Series | payment_attempts_total |
| Payment Methods Distribution | Pie Chart | payment_attempts_total by method |
| User Registrations | Time Series | users_registered_total |
| Cart Abandonment Rate | Gauge | (carts - orders) / carts |
| Inventory Stock Levels | Time Series | inventory_quantity_available |
| Top Products by Sales | Table | orders_product_quantity |
| Order Status Distribution | Pie Chart | orders_by_status |

### 4. Service Health Dashboard (UID: `service-health-dashboard`)

Service availability, SLA compliance, and incident tracking.

**Panels:**
| Panel | Type | Metric |
|-------|------|--------|
| Service Uptime (24h) | Stat | avg_over_time(up[24h]) |
| Service Availability (30d) | Stat | avg_over_time(up[30d]) |
| Overall SLA Compliance | Gauge | avg(up) across services |
| Service Health Timeline | Time Series | up |
| Service Dependencies Health | Table | up (all services) |
| Error Rate by Service | Time Series | http_requests_total |
| Service Response Time vs SLA | Time Series | http_request_duration_seconds |
| Active Alerts / Incidents | Table | ALERTS{alertstate="firing"} |
| Health Check Response Time | Time Series | health_check_duration_seconds |
| Service Restarts | Time Series | process_start_time_seconds |
| Infrastructure Services Status | Stat | up (infrastructure) |

**Features:**
- Service filter variable (multi-select)
- SLA period selector (1h to 30d)
- Alert threshold lines on response time (2s) and error rate (5%)
- Color-coded status indicators (UP/DOWN)

---

## Datasource Configuration

### Prometheus (Default)
- **URL**: http://prometheus:9090
- **Access**: Proxy
- **Scrape Interval**: 15s
- **HTTP Method**: POST

### Elasticsearch
- **URL**: http://elasticsearch:9200
- **Time Field**: @timestamp
- **Version**: 8.0.0
- **Index Pattern**: ecommerce-*

### Jaeger
- **URL**: http://jaeger:16686
- **Trace ID Field**: traceId
- **Span ID Field**: spanId

---

## Key Metrics Reference

### HTTP Metrics (Spring Boot Actuator)
| Metric | Description |
|--------|-------------|
| `http_requests_total` | Total HTTP requests by method, status, URI |
| `http_request_duration_seconds` | Request duration histogram |
| `http_requests_in_progress` | Currently active requests |

### JVM Metrics
| Metric | Description |
|--------|-------------|
| `jvm_memory_used_bytes` | JVM memory usage by area |
| `jvm_memory_max_bytes` | JVM max memory by area |
| `jvm_threads_live_threads` | Current live thread count |
| `jvm_threads_peak_threads` | Peak thread count |
| `jvm_gc_pause_seconds` | GC pause duration |

### Database Metrics (PostgreSQL Exporter)
| Metric | Description |
|--------|-------------|
| `pg_stat_activity_count` | Active connections |
| `pg_settings_max_connections` | Max allowed connections |
| `pg_stat_database_deadlocks` | Deadlock count |
| `pg_stat_activity_max_tx_duration` | Longest transaction |

### Kafka Metrics
| Metric | Description |
|--------|-------------|
| `kafka_consumer_lag` | Consumer lag per group/topic |
| `kafka_brokers` | Number of active brokers |
| `kafka_producer_records_sent_total` | Records produced |
| `kafka_consumer_records_consumed_total` | Records consumed |

### Redis Metrics
| Metric | Description |
|--------|-------------|
| `redis_memory_used_bytes` | Memory used by Redis |
| `redis_memory_max_bytes` | Max configured memory |
| `redis_connected_clients` | Connected client count |

### System Metrics (Node Exporter)
| Metric | Description |
|--------|-------------|
| `node_cpu_seconds_total` | CPU time by mode |
| `node_memory_MemAvailable_bytes` | Available memory |
| `node_memory_MemTotal_bytes` | Total memory |
| `node_filesystem_avail_bytes` | Available disk space |
| `node_filesystem_size_bytes` | Total disk space |
| `node_load1/5/15` | System load averages |
| `node_network_receive_bytes_total` | Network bytes received |
| `node_network_transmit_bytes_total` | Network bytes transmitted |

### Business Metrics (Custom)
| Metric | Description |
|--------|-------------|
| `orders_created_total` | Total orders created |
| `orders_total_amount` | Total order revenue |
| `orders_by_status` | Orders by status |
| `payment_attempts_total` | Payment attempts by status/method |
| `users_registered_total` | User registrations |
| `users_active_total` | Currently active users |
| `carts_created_total` | Carts created |
| `inventory_quantity_available` | Available inventory |

---

## Distributed Tracing (Jaeger)

### Configuration
| Setting | Value |
|---------|-------|
| Sampling Rate | 10% of requests |
| Retention | 72 hours |
| UI Port | 16686 |
| Collector Port | 14268 |

### Usage
1. Access http://localhost:16686
2. Select service from dropdown
3. Search by trace ID, duration, or tags
4. Analyze request flow across services
5. Identify performance bottlenecks

### Trace Propagation
All services propagate trace context via HTTP headers:
- `traceparent` (W3C Trace Context)
- `X-B3-TraceId` (Zipkin B3)

---

## Logging (ELK Stack)

### Log Format (Structured JSON)
```json
{
  "timestamp": "2025-01-15T10:30:45Z",
  "service": "order-service",
  "level": "INFO",
  "traceId": "abc123def456",
  "spanId": "xyz789",
  "message": "Order created successfully",
  "userId": "user-123",
  "orderId": "order-456",
  "duration_ms": 245
}
```

### Log Levels
| Level | Usage |
|-------|-------|
| DEBUG | Detailed debugging (dev only) |
| INFO | Normal operations |
| WARN | Potential issues |
| ERROR | Errors requiring attention |
| FATAL | Critical failures |

### Kibana Setup
1. Access http://localhost:5601
2. Create index pattern: `ecommerce-*`
3. Use Discover for log search
4. Create visualizations and dashboards

---

## Alerting Workflow

### Alert Flow
```
Prometheus → Alertmanager → Notification Channels
                         ├── Email (alerts@ecommerce.local)
                         ├── Slack (#alerts channel)
                         └── PagerDuty (production)
```

### Alert Severity Routing
| Severity | Channel | Response Time |
|----------|---------|---------------|
| critical | PagerDuty + Slack + Email | Immediate |
| warning | Slack + Email | Within 1 hour |

### Alert Silencing
Use Alertmanager UI (http://localhost:9093) to silence alerts during maintenance windows.

---

## Troubleshooting

### Prometheus Not Scraping Services
1. Check target status: http://localhost:9090/targets
2. Verify service is running: `docker ps | grep <service>`
3. Test metrics endpoint: `curl http://localhost:<port>/actuator/prometheus`
4. Check network connectivity between containers
5. Review Prometheus logs: `docker logs prometheus`

### Grafana Dashboards Not Loading
1. Verify Prometheus datasource: Grafana → Configuration → Data Sources
2. Check dashboard provisioning: `docker logs grafana`
3. Validate JSON files: `python -m json.tool <dashboard>.json`
4. Check file permissions on mounted volumes

### Alerts Not Firing
1. Check rule evaluation: http://localhost:9090/rules
2. Verify Alertmanager is running: http://localhost:9093
3. Test alert expression in Prometheus UI
4. Check Alertmanager configuration and routing

### High Memory Usage in Prometheus
1. Check cardinality: http://localhost:9090/tsdb-status
2. Review metric_relabel_configs for high-cardinality drops
3. Reduce retention period if needed
4. Consider federation for large deployments

### Metrics Not Appearing
1. Verify metric name in Prometheus: http://localhost:9090/graph
2. Check label selectors match
3. Ensure time range is correct in Grafana
4. Verify scrape interval allows data to appear

---

## Best Practices

1. **Alert on symptoms, not causes** - Alert on user-facing impact (error rate, latency) rather than internal metrics
2. **Use recording rules** for complex queries that run frequently
3. **Label consistently** - Use standard labels (service, team, environment) across all targets
4. **Set appropriate thresholds** - Avoid alert fatigue with well-tuned thresholds
5. **Monitor the monitors** - Ensure Prometheus and Grafana are themselves monitored
6. **Review dashboards regularly** - Keep dashboards relevant and remove unused panels
7. **Document runbooks** - Every alert should link to a remediation runbook
8. **Use structured logging** - JSON format enables efficient searching and correlation

---

## File Structure

```
shared/
├── prometheus.yml                          # Main Prometheus configuration
└── monitoring/
    ├── README.md                           # This file
    ├── alert_rules.yml                     # Prometheus alert rules
    ├── grafana-dashboards/
    │   ├── dashboard.yml                   # Dashboard provisioning config
    │   ├── system-dashboard.json           # System/infrastructure dashboard
    │   ├── application-dashboard.json      # Application performance dashboard
    │   ├── business-dashboard.json         # Business KPI dashboard
    │   └── service-health-dashboard.json   # Service health/SLA dashboard
    └── grafana-datasources/
        └── datasource.yml                  # Datasource provisioning config
```

---

**Status**: ✅ Configured and Enhanced
**Last Updated**: June 2025
**Version**: 2.0.0
