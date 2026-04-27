# Keycloak Configuration

This directory contains Keycloak configuration files for the ecommerce platform.

## Setup Instructions

### 1. Access Keycloak Admin Console

1. Navigate to http://localhost:8180
2. Click "Administration Console"
3. Login with credentials:
   - Username: `admin`
   - Password: `admin`

### 2. Create Realm

1. Click on "Master" dropdown in top-left
2. Click "Create Realm"
3. Enter realm name: `ecommerce`
4. Click "Create"

### 3. Create Roles

1. Go to "Realm Roles"
2. Create the following roles:
   - `ADMIN`: Full system access
   - `USER`: Regular user access
   - `CUSTOMER`: Customer-specific access
   - `SUPPORT`: Support staff access

### 4. Create Clients

#### Web Client (Frontend SPA)
1. Go to "Clients"
2. Click "Create client"
3. Client ID: `web-client`
4. Client Protocol: `openid-connect`
5. Configure:
   - Access Type: `public`
   - Valid Redirect URIs: `http://localhost:3000/*`
   - Web Origins: `http://localhost:3000`

#### Mobile Client
1. Create new client
2. Client ID: `mobile-client`
3. Client Protocol: `openid-connect`
4. Configure:
   - Access Type: `public`
   - Valid Redirect URIs: `com.ecommerce.mobile://*`

#### Service Account (Inter-service Communication)
1. Create new client
2. Client ID: `service-account`
3. Client Protocol: `openid-connect`
4. Configure:
   - Access Type: `confidential`
   - Service Accounts Enabled: `ON`
   - Generate client secret

### 5. Create Test Users

1. Go to "Users"
2. Create users with the following roles:

**Admin User**
- Username: `admin@ecommerce.com`
- Email: `admin@ecommerce.com`
- Password: `admin123`
- Roles: `ADMIN`

**Customer User**
- Username: `customer@ecommerce.com`
- Email: `customer@ecommerce.com`
- Password: `customer123`
- Roles: `CUSTOMER`

**Support User**
- Username: `support@ecommerce.com`
- Email: `support@ecommerce.com`
- Password: `support123`
- Roles: `SUPPORT`

### 6. Configure Token Settings

1. Go to "Realm Settings"
2. Click "Tokens" tab
3. Configure:
   - Access Token Lifespan: `15 minutes`
   - Refresh Token Lifespan: `7 days`
   - Refresh Token Max Reuse: `0` (unlimited)

## API Integration

### Getting Access Token

```bash
curl -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web-client" \
  -d "grant_type=password" \
  -d "username=customer@ecommerce.com" \
  -d "password=customer123"
```

### Using Access Token

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access_token>"
```

## Troubleshooting

### Keycloak not starting
- Check Docker logs: `docker logs keycloak`
- Ensure user-db is running: `docker ps | grep user-db`
- Wait for database to be ready (check healthcheck)

### Cannot login
- Verify user exists in Keycloak admin console
- Check user password is correct
- Ensure user has required roles

### Token validation fails
- Verify token signature with Keycloak public key
- Check token expiry time
- Ensure client is configured correctly

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth2/OIDC Specification](https://openid.net/connect/)
- [JWT Tokens](https://jwt.io/)
