# Distributed Tracing Configuration Guide

## Overview

Distributed tracing is implemented using Jaeger to track request flows across microservices. This enables visibility into request latency, identify bottlenecks, and debug issues in the service mesh.

## Architecture

### Trace Flow

```
Client Request
    ↓
API Gateway (Trace ID generated)
    ↓
Service A → Service B → Service C
    ↓
Jaeger Collector → Jaeger UI
```

### Trace Context Propagation

Traces are propagated using W3C Trace Context standard:
- `traceparent`: Trace context header
- `tracestate`: Vendor-specific trace information

## Jaeger Configuration

### Components

| Component | Port | Description |
|-----------|------|-------------|
| Jaeger UI | 16686 | Web interface for trace visualization |
| Collector | 14268 | Receives traces from agents |
| Agent | 6831 | Agent that collects traces and sends to collector |
| Query | 16686 | Query service for UI |

### Configuration

```yaml
# Jaeger configuration (jaeger-config.yml)
collector:
  host: jaeger
  port: 14268

agent:
  host: jaeger-agent
  port: 6831

sampling:
  strategy: probabilistic
  rate: 0.1  # 10% sampling rate
```

## Service Integration

### Java (Spring Boot)

Add dependency:
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-spring-jaeger</artifactId>
    <version>3.3.1</version>
</dependency>
```

Configuration:
```yaml
opentracing:
  jaeger:
    enabled: true
    service-name: user-service
    collector:
      endpoint: http://jaeger:14268/api/traces
    sampler:
      type: probabilistic
      param: 0.1
```

### .NET (ASP.NET Core)

Add package:
```bash
dotnet add package OpenTelemetry.Exporter.Jaeger
```

Configuration:
```csharp
builder.Services.AddOpenTelemetry()
    .WithTracing(tracerProviderBuilder => {
        tracerProviderBuilder
            .AddJaegerExporter(options => {
                options.Endpoint = new Uri("http://jaeger:14268/api/traces");
                options.ServiceName = "user-service";
            });
    });
```

### Node.js (Express)

Add package:
```bash
npm install jaeger-client opentracing
```

Configuration:
```javascript
const jaeger = require('jaeger-client');

const config = {
  serviceName: 'user-service',
  sampler: {
    type: 'probabilistic',
    param: 0.1
  },
  reporter: {
    collectorEndpoint: 'http://jaeger:14268/api/traces',
  }
};

const tracer = jaeger.initTracer(config);
```

## Trace Information

### Trace ID
- Unique identifier for the entire request flow
- 128-bit random value
- Propagated across all services

### Span ID
- Unique identifier for each operation
- 64-bit random value
- Nested spans represent sub-operations

### Tags
- Key-value pairs for metadata
- Examples: service name, endpoint, status code
- Used for filtering and search

### Logs
- Event logs within a span
- Timestamped key-value pairs
- Used for debugging and auditing

## Jaeger UI Usage

### Access
```
http://localhost:16686
```

### Search Options
- **Service**: Filter by service name
- **Operation**: Filter by operation name
- **Tags**: Filter by key-value pairs
- **Start Time**: Time range for search
- **Duration**: Filter by trace duration

### Trace Details
- View span hierarchy
- See timing information
- Review logs and tags
- Identify bottlenecks

## Best Practices

1. **Consistent Sampling**: Use consistent sampling rate across services
2. **Trace Propagation**: Ensure trace context is propagated correctly
3. **Error Tracking**: Mark error spans appropriately
4. **Metadata**: Include relevant tags for debugging
5. **Performance**: Monitor tracing overhead

## Troubleshooting

### Traces Not Showing
- Verify Jaeger is running: `docker-compose ps jaeger`
- Check service configuration
- Verify network connectivity
- Check sampling rate

### Incomplete Traces
- Verify trace context propagation
- Check for missing span creation
- Review service integration

### High Overhead
- Increase sampling interval
- Review span creation frequency
- Optimize trace propagation

## References

- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)
- [OpenTelemetry](https://opentelemetry.io/)

---

**Status**: ✅ Configured  
**Last Updated**: April 2024  
**Version**: 1.0.0