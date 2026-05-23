# =============================================================================
# Product Service - Dockerfile
# =============================================================================
# Multi-stage build for optimized production image
# Stage 1: Build TypeScript → JavaScript
# Stage 2: Run compiled JavaScript with minimal image
# =============================================================================

# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

# Copy dependency files first (Docker layer caching optimization)
COPY package*.json ./
RUN npm install

# Copy source code and build
COPY tsconfig.json ./
COPY src/ ./src/
RUN npm run build

# Stage 2: Production
FROM node:18-alpine

WORKDIR /app

# Copy only production dependencies and compiled code
COPY package*.json ./
RUN npm install --only=production

COPY --from=builder /app/dist ./dist

# Expose service port
EXPOSE 4091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4091/health || exit 1

# Start the service
CMD ["node", "dist/index.js"]
