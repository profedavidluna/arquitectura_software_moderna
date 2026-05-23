# =============================================================================
# Order Service - Dockerfile
# =============================================================================
# This service orchestrates the Saga pattern for distributed transactions.
# It coordinates between order creation and inventory reservation.
# =============================================================================

# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY tsconfig.json ./
COPY src/ ./src/
RUN npm run build

# Stage 2: Production
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install --only=production

COPY --from=builder /app/dist ./dist

EXPOSE 4092

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4092/health || exit 1

CMD ["node", "dist/index.js"]
