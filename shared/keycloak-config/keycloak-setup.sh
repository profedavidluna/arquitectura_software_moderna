#!/bin/bash

# Keycloak Setup Script
# This script sets up the Keycloak realm, roles, clients, and test users

set -e

KEYCLOAK_URL="http://localhost:8180"
REALM_NAME="ecommerce"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"

echo "=== Keycloak Setup Script ==="
echo "Keycloak URL: $KEYCLOAK_URL"
echo "Realm: $REALM_NAME"
echo ""

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
for i in {1..30}; do
  if curl -s "$KEYCLOAK_URL" > /dev/null 2>&1; then
    echo "Keycloak is ready!"
    break
  fi
  echo "Waiting... ($i/30)"
  sleep 2
done

# Check if Keycloak is running
if ! curl -s "$KEYCLOAK_URL" > /dev/null 2>&1; then
  echo "ERROR: Keycloak is not running. Please start it with: docker-compose up -d keycloak"
  exit 1
fi

# Get admin token
echo "Getting admin token..."
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
  echo "ERROR: Failed to get admin token"
  exit 1
fi

echo "Token obtained successfully!"
echo ""

# Import realm using Keycloak Admin API
echo "Importing realm: $REALM_NAME..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d @realm-config.json > /dev/null
echo "Realm imported successfully!"
echo ""

# Get realm ID
REALM_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.id')

if [ -z "$REALM_ID" ] || [ "$REALM_ID" == "null" ]; then
  echo "ERROR: Failed to get realm ID"
  exit 1
fi

echo "Realm ID: $REALM_ID"
echo ""

# Create roles if they don't exist
echo "Creating roles..."
for role in ADMIN USER CUSTOMER SUPPORT; do
  if curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/$role" \
    -H "Authorization: Bearer $TOKEN" | grep -q "Not Found"; then
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "{\"name\": \"$role\"}" > /dev/null
    echo "Role '$role' created successfully!"
  else
    echo "Role '$role' already exists!"
  fi
done
echo ""

# Create clients if they don't exist
echo "Creating clients..."
for client in web-client mobile-client service-account; do
  CLIENT_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients?clientId=$client" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id // empty')
  
  if [ -z "$CLIENT_ID" ]; then
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d @<(cat realm-config.json | jq -c ".clients[] | select(.clientId == \"$client\")") > /dev/null
    echo "Client '$client' created successfully!"
  else
    echo "Client '$client' already exists (ID: $CLIENT_ID)!"
  fi
done
echo ""

# Create users if they don't exist
echo "Creating users..."
for user in admin user customer support; do
  USER_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=$user" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id // empty')
  
  if [ -z "$USER_ID" ]; then
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d @<(cat users.json | jq -c ".users[] | select(.username == \"$user\")") > /dev/null
    echo "User '$user' created successfully!"
  else
    echo "User '$user' already exists (ID: $USER_ID)!"
  fi
done
echo ""

# Assign roles to users
echo "Assigning roles to users..."
for user in admin user customer support; do
  USER_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=$user" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')
  
  if [ -z "$USER_ID" ]; then
    echo "WARNING: User '$user' not found, skipping role assignment"
    continue
  fi
  
  # Get role IDs
  ADMIN_ROLE_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/ADMIN" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.id // empty')
  USER_ROLE_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/USER" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.id // empty')
  CUSTOMER_ROLE_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/CUSTOMER" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.id // empty')
  SUPPORT_ROLE_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/SUPPORT" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.id // empty')
  
  # Assign roles based on user type
  case $user in
    admin)
      curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "[{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"},{\"id\":\"$USER_ROLE_ID\",\"name\":\"USER\"},{\"id\":\"$CUSTOMER_ROLE_ID\",\"name\":\"CUSTOMER\"}]" > /dev/null
      ;;
    user)
      curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "[{\"id\":\"$USER_ROLE_ID\",\"name\":\"USER\"},{\"id\":\"$CUSTOMER_ROLE_ID\",\"name\":\"CUSTOMER\"}]" > /dev/null
      ;;
    customer)
      curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "[{\"id\":\"$CUSTOMER_ROLE_ID\",\"name\":\"CUSTOMER\"}]" > /dev/null
      ;;
    support)
      curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "[{\"id\":\"$SUPPORT_ROLE_ID\",\"name\":\"SUPPORT\"},{\"id\":\"$USER_ROLE_ID\",\"name\":\"USER\"}]" > /dev/null
      ;;
  esac
  echo "Roles assigned to user '$user'!"
done
echo ""

echo "=== Keycloak Setup Complete ==="
echo ""
echo "Access Keycloak at: $KEYCLOAK_URL"
echo "Realm: $REALM_NAME"
echo ""
echo "Test Users:"
echo "  - Admin: admin / admin"
echo "  - User: user / user"
echo "  - Customer: customer / customer"
echo "  - Support: support / support"
echo ""
echo "Clients:"
echo "  - web-client (public)"
echo "  - mobile-client (public)"
echo "  - service-account (confidential)"
echo ""
