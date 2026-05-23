# Keycloak Configuration Guide

## Overview

Keycloak is configured as the centralized authentication and authorization server for the ecommerce platform. It handles OAuth2/OIDC authentication, user management, and role-based access control (RBAC).

## Configuration Details

### Realm
- **Name**: ecommerce
- **Status**: Enabled

### User Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| ADMIN | Administrator with full access | All endpoints, all operations |
| USER | Regular user | User profile management, browsing |
| CUSTOMER | Customer role for shopping | Cart, checkout, order history |
| SUPPORT | Support agent | Order management, customer support |

### OAuth2/OIDC Clients

#### web-client (Public)
- **Type**: Public client
- **Redirect URIs**: http://localhost:3000/*, http://localhost:8080/*
- **Web Origins**: http://localhost:3000, http://localhost:8080
- **Use Case**: Web frontend applications

#### mobile-client (Public)
- **Type**: Public client
- **Redirect URIs**: http://localhost:8100/*, http://localhost:4200/*
- **Web Origins**: http://localhost:8100, http://localhost:4200
- **Use Case**: Mobile applications

#### service-account (Confidential)
- **Type**: Confidential client
- **Secret**: Generated automatically
- **Service Accounts**: Enabled
- **Use Case**: Service-to-service communication

### Token Configuration

| Token Type | Expiry | Description |
|------------|--------|-------------|
| Access Token | 15 minutes | Used for API authentication |
| Refresh Token | 7 days | Used to obtain new access tokens |

## Test Users

| Username | Password | Roles | Email |
|----------|----------|-------|-------|
| admin | admin | ADMIN, USER, CUSTOMER | admin@ecommerce.local |
| user | user | USER, CUSTOMER | user@ecommerce.local |
| customer | customer | CUSTOMER | customer@ecommerce.local |
| support | support | SUPPORT, USER | support@ecommerce.local |

## API Integration

### Get Access Token (Password Grant)

```bash
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web-client" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

### Use Access Token

```bash
curl http://localhost:8082/api/v1/users \
  -H "Authorization: Bearer <access_token>"
```

### Refresh Token

```bash
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web-client" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=<refresh_token>"
```

## Setup Instructions

### Using the Setup Script

```bash
# Make the script executable
chmod +x shared/keycloak-config/keycloak-setup.sh

# Run the setup script
./shared/keycloak-config/keycloak-setup.sh
```

### Manual Setup

1. **Access Keycloak**
   ```
   http://localhost:8180
   Login: admin / admin
   ```

2. **Create Realm**
   - Click "Create Realm"
   - Name: ecommerce
   - Click "Create"

3. **Create Roles**
   - Go to "Realm Roles"
   - Click "Create Role"
   - Create: ADMIN, USER, CUSTOMER, SUPPORT

4. **Create Clients**
   - Go to "Clients"
   - Click "Create Client"
   - Configure for web-client, mobile-client, service-account

5. **Create Users**
   - Go to "Users"
   - Click "Create User"
   - Create test users with appropriate roles

## Troubleshooting

### Keycloak Not Starting
```bash
# Check if Keycloak is running
docker-compose ps keycloak

# View logs
docker-compose logs keycloak

# Restart Keycloak
docker-compose restart keycloak
```

### Cannot Connect to Keycloak
- Verify Keycloak is running: `docker-compose ps`
- Check port 8180 is not blocked
- Verify network configuration in docker-compose.yml

### Token Validation Fails
- Check token expiry (15 minutes for access tokens)
- Verify client configuration matches
- Check token signature and issuer

## Security Best Practices

1. **Change Default Passwords**: Update admin password in production
2. **Use HTTPS**: Configure SSL/TLS for production
3. **Token Rotation**: Implement token refresh logic
4. **Rate Limiting**: Configure rate limiting for token endpoints
5. **Audit Logging**: Enable audit logging for security events

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth2 Specification](https://oauth.net/2/)
- [OIDC Specification](https://openid.net/specs/openid-connect-core-1_0.html)

---

**Status**: ✅ Configured
**Last Updated**: April 2024
**Version**: 1.0.0
