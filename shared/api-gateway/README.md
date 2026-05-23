# API Gateway Configuration Guide

## Overview

The API Gateway serves as the single entry point for all client requests to the ecommerce microservices. It handles authentication, rate limiting, CORS, and request routing.

## Architecture

### Request Flow

```
Client → API Gateway (Port 8080) → Target Service
         ↓
    Authentication (Keycloak OIDC)
         ↓
    Rate Limiting (100 req/sec)
         ↓
    CORS Handling
         ↓
    Route to Service
```

## Configuration

### Gateway Type
- **Implementation**: Kong API Gateway
- **Port**: 8000 (HTTP), 8443 (HTTPS)
- **Admin Port**: 8001

### Services Configured

| Service | Port | Routes |
|---------|------|--------|
| User Service | 8082 | /api/v1/users |
| Product Service | 8083 | /api/v1/products |
| Cart Service | 8084 | /api/v1/cart |
| Order Service | 8085 | /api/v1/orders |
| Payment Service | 8086 | /api/v1/payments |
| Inventory Service | 8087 | /api/v1/inventory |
| Notification Service | 8088 | /api/v1/notifications |
| Analytics Service | 8089 | /api/v1/analytics |

## Plugins

### 1. Keycloak OIDC Authentication
- **Issuer**: http://keycloak:8180/realms/ecommerce
- **Client ID**: web-client
- **Credentials**: Authorization header

### 2. Rate Limiting
- **Limit**: 100 requests per minute per user
- **Policy**: Local (per gateway instance)
- **Headers**: X-RateLimit-Limit, X-RateLimit-Remaining

### 3. CORS
- **Allowed Origins**: localhost:3000, localhost:8080, localhost:8100, localhost:4200
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Allowed Headers**: Accept, Content-Type, Authorization, X-Request-ID
- **Exposed Headers**: X-Request-ID, X-RateLimit-Limit, X-RateLimit-Remaining
- **Credentials**: true
- **Max Age**: 3600 seconds

## API Endpoints

### Health Check
```bash
curl http://localhost:8080
```

### Service Endpoints

All service endpoints are accessible through the API Gateway:

```bash
# User Service
curl http://localhost:8080/api/v1/users

# Product Service
curl http://localhost:8080/api/v1/products

# Cart Service
curl http://localhost:8080/api/v1/cart

# Order Service
curl http://localhost:8080/api/v1/orders

# Payment Service
curl http://localhost:8080/api/v1/payments

# Inventory Service
curl http://localhost:8080/api/v1/inventory

# Notification Service
curl http://localhost:8080/api/v1/notifications

# Analytics Service
curl http://localhost:8080/api/v1/analytics
```

## Authentication

### Get Access Token
```bash
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web-client" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

### Use Token
```bash
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <access_token>"
```

## Rate Limiting

### Headers
- **X-RateLimit-Limit**: Maximum requests per minute
- **X-RateLimit-Remaining**: Remaining requests in current window
- **X-RateLimit-Reset**: Timestamp when window resets

### Response When Rate Limited
```json
{
  "message": "API rate limit exceeded"
}
```

## CORS

### Preflight Request
```bash
curl -X OPTIONS http://localhost:8080/api/v1/users \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
```

### Response Headers
- **Access-Control-Allow-Origin**: http://localhost:3000
- **Access-Control-Allow-Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Access-Control-Allow-Headers**: Accept, Content-Type, Authorization, X-Request-ID
- **Access-Control-Expose-Headers**: X-Request-ID, X-RateLimit-Limit, X-RateLimit-Remaining
- **Access-Control-Allow-Credentials**: true
- **Access-Control-Max-Age**: 3600

## Monitoring

### Gateway Health
```bash
curl http://localhost:8080
```

### Admin API
```bash
# List services
curl http://localhost:8001/services

# List routes
curl http://localhost:8001/routes

# List plugins
curl http://localhost:8001/plugins
```

## Setup Instructions

### Using Docker Compose
```bash
cd shared/api-gateway
docker-compose -f docker-compose.api-gateway.yml up -d
```

### Verify Gateway is Running
```bash
curl http://localhost:8080
```

## Troubleshooting

### Gateway Not Starting
```bash
# Check if gateway is running
docker-compose ps api-gateway

# View logs
docker-compose logs api-gateway

# Restart gateway
docker-compose restart api-gateway
```

### Authentication Fails
- Verify Keycloak is running
- Check issuer URL is correct
- Verify client ID and secret
- Check token is valid and not expired

### Rate Limiting Issues
- Check rate limit plugin configuration
- Verify headers are being set
- Check for multiple gateway instances

### CORS Errors
- Verify allowed origins configuration
- Check request headers
- Verify preflight request handling

## Best Practices

1. **Use HTTPS in Production**: Configure SSL/TLS
2. **Monitor Rate Limits**: Track usage patterns
3. **Enable Logging**: Log all requests for debugging
4. **Health Checks**: Monitor gateway health
5. **Security**: Keep Kong updated, use secrets management

## References

- [Kong Documentation](https://docs.konghq.com/)
- [Kong OIDC Plugin](https://docs.konghq.com/hub/kong-inc/keycloak-oidc/)
- [Rate Limiting Plugin](https://docs.konghq.com/hub/kong-inc/rate-limiting/)

---

**Status**: ✅ Configured
**Last Updated**: April 2024
**Version**: 1.0.0
