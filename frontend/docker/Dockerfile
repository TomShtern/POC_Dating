# Frontend - Docker Configuration
#
# PURPOSE: Build and run React application in containerized environment
#
# MULTI-STAGE BUILD:
# Stage 1: Build - Installs dependencies, builds production bundle
# Stage 2: Runtime - Serves static files with lightweight server
#
# WHY MULTI-STAGE:
# - Build artifacts and node_modules don't ship to production
# - Reduces final image size from 500MB+ to ~50MB
# - No npm/node/build tools exposed in production
# - Security: no source maps or sensitive data in production
#
# RUNTIME:
# - Uses Node.js slim image for serving static files
# - Could also use nginx for even lighter image
# - Exposes port 3000 for local development
# - Set NODE_ENV=production for optimization

# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build production bundle
RUN npm run build

# Stage 2: Runtime
FROM node:18-alpine

WORKDIR /app

# Install lightweight static server
RUN npm install -g serve

# Copy built application from builder stage
COPY --from=builder /app/dist ./dist

# Non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

USER appuser

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget -q -O- http://localhost:3000/ || exit 1

# Serve static files on port 3000
ENTRYPOINT ["serve", "-s", "dist", "-l", "3000"]
