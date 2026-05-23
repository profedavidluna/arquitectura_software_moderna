# Centralized Logging Configuration Guide

## Overview

Centralized logging is implemented using the ELK Stack (Elasticsearch, Logstash, Kibana) to collect, process, and visualize logs from all microservices. This enables unified log management and analysis across the entire system.

## Architecture

### Log Flow

```
Service → Log Agent → Logstash → Elasticsearch → Kibana
```

### Components

| Component | Port | Description |
|-----------|------|-------------|
| Elasticsearch | 9200 | Log storage and indexing |
| Logstash | 5044 | Log collection and transformation (TCP) |
| Logstash | 5000 | Log collection (UDP) |
| Kibana | 5601 | Log visualization and analysis |

## Log Format

### Standard JSON Format

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "user-service",
  "traceId": "abc123def456789",
  "spanId": "xyz789",
  "userId": "user-123",
  "message": "User profile updated successfully",
  "duration_ms": 45,
  "method": "PUT",
  "endpoint": "/api/v1/users/{id}",
  "statusCode": 200,
  "host": "user-service-1",
  "version": "1.0.0"
}
```

### Log Levels

| Level | Description | Use Case |
|-------|-------------|----------|
| TRACE | Detailed debugging | Deep debugging, development only |
| DEBUG | Debug information | Development and troubleshooting |
| INFO | General information | Normal operations |
| WARN | Warning messages | Potential issues |
| ERROR | Error messages | Errors that need attention |
| FATAL | Critical errors | System failures |

## Service Integration

### Java (Spring Boot)

Add dependency:
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

Logback configuration (`logback-spring.xml`):
```xml
<configuration>
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5044</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOGSTASH" />
    </root>
</configuration>
```

### .NET (ASP.NET Core)

Add package:
```bash
dotnet add package Serilog.Sinks.Console
dotnet add package Serilog.Sinks.Http
```

Configuration:
```csharp
Log.Logger = new LoggerConfiguration()
    .Enrich.FromLogContext()
    .Enrich.WithProperty("Service", "user-service")
    .WriteTo.Console()
    .WriteTo.Http("http://logstash:5044")
    .CreateLogger();
```

### Node.js (Express)

Add packages:
```bash
npm install winston winston-daily-rotate-file
npm install @logdna/logger
```

Configuration:
```javascript
const winston = require('winston');
const { LogdnaTransport } = require('@logdna/logger');

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  transports: [
    new LogdnaTransport({
      key: process.env.LOGDNA_KEY,
      hostname: 'user-service',
      app: 'user-service'
    })
  ]
});
```

## Logstash Configuration

### Input Configuration

```conf
input {
  tcp {
    port => 5044
    codec => json
  }
  udp {
    port => 5000
    codec => json
  }
}
```

### Filter Configuration

```conf
filter {
  # Parse JSON
  json {
    source => "message"
  }
  
  # Convert timestamp
  date {
    match => ["timestamp", "ISO8601"]
    target => "@timestamp"
  }
  
  # Add tags
  mutate {
    add_tag => ["microservices", "ecommerce"]
  }
}
```

### Output Configuration

```conf
output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "ecommerce-%{service}-%{+YYYY.MM.dd}"
  }
}
```

## Kibana Usage

### Index Pattern

Create index pattern: `ecommerce-*`

### Discover
1. Access http://localhost:5601
2. Navigate to "Discover"
3. Select index pattern
4. Filter logs by service, level, time range
5. Search using KQL

### Visualizations
- Log count over time
- Log level distribution
- Service error rates
- Response time distribution

### Dashboards
- Service health dashboard
- Error tracking dashboard
- Performance dashboard

## Log Aggregation

### Service-Specific Logs

```bash
# All logs for a service
service:user-service

# Error logs
level:ERROR

# Logs with specific trace ID
traceId:abc123def456

# Logs from specific time range
timestamp:[2024-01-15T10:00 TO 2024-01-15T11:00]
```

### Common Queries

```bash
# Errors in last hour
level:ERROR AND timestamp:>now-1h

# Slow requests (>500ms)
duration_ms:>500

# Service down events
level:FATAL
```

## Best Practices

1. **Consistent Format**: Use JSON format for all logs
2. **Trace Context**: Include traceId in all logs
3. **Structured Data**: Use structured logging instead of strings
4. **Appropriate Levels**: Use log levels correctly
5. **Sensitive Data**: Never log sensitive information
6. **Performance**: Avoid excessive logging in production

## Troubleshooting

### Logs Not Showing in Kibana
- Check Logstash is running: `docker-compose ps logstash`
- Verify Elasticsearch is healthy
- Check log format is JSON
- Verify network connectivity

### Missing Trace Context
- Verify traceId is included in logs
- Check MDC configuration
- Review logging setup

### High Log Volume
- Increase log level in production
- Implement log sampling
- Review log frequency

## References

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [JSON Log Format](https://www.loggly.com/ultimate-guide/json-logging/)

---

**Status**: ✅ Configured  
**Last Updated**: April 2024  
**Version**: 1.0.0