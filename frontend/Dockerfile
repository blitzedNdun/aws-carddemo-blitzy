# CardDemo React Frontend - Multi-Stage Docker Build
# Migrates from BMS mapsets to React 18.x Single Page Application
# Optimized for production deployment with minimal image size and enterprise security

# ============================================================================
# BUILD STAGE - Node.js Environment for React Application Compilation
# ============================================================================
FROM node:20.18-alpine AS build-stage

# Set working directory for build operations
WORKDIR /workspace/app

# Install build dependencies for native modules compilation
# Required for some npm packages that include native bindings
RUN apk add --no-cache python3 make g++ && \
    ln -sf python3 /usr/bin/python

# ============================================================================
# DEPENDENCY LAYER - NPM Package Installation with Caching Optimization
# ============================================================================
# Copy package definition files for dependency resolution
# This layer is cached when package.json/package-lock.json remain unchanged
COPY package*.json ./

# Install all dependencies including devDependencies for build process
# Using npm ci for reproducible builds matching package-lock.json exactly
# --production=false ensures dev dependencies like build tools are installed
RUN npm ci --production=false && \
    npm cache clean --force

# ============================================================================
# SOURCE LAYER - React Application Build Process
# ============================================================================
# Copy React application source code
# Structured copy to optimize Docker layer caching
COPY public/ public/
COPY src/ src/

# Build React application for production
# Creates optimized build output in /workspace/app/build
# Includes minification, tree-shaking, and asset optimization
RUN npm run build

# ============================================================================
# RUNTIME STAGE - NGINX Web Server for Production Serving
# ============================================================================
FROM nginx:1.25-alpine AS runtime-stage

# Install curl for health check endpoints
# Using alpine package manager for minimal footprint
RUN apk add --no-cache curl

# ============================================================================
# APPLICATION DEPLOYMENT - React Build Artifacts
# ============================================================================
# Copy React build output from build stage
# Static assets deployed to nginx document root
COPY --from=build-stage /workspace/app/build /usr/share/nginx/html

# Deploy nginx configuration for React SPA
# Includes client-side routing support and API proxy configuration
COPY nginx.conf /etc/nginx/nginx.conf

# ============================================================================
# SECURITY HARDENING - Non-Root User Execution
# ============================================================================
# Create nginx runtime directories with proper ownership
# Required for non-root nginx operation
RUN mkdir -p /var/cache/nginx/client_temp && \
    mkdir -p /var/cache/nginx/proxy_temp && \
    mkdir -p /var/cache/nginx/fastcgi_temp && \
    mkdir -p /var/cache/nginx/uwsgi_temp && \
    mkdir -p /var/cache/nginx/scgi_temp && \
    mkdir -p /var/log/nginx && \
    mkdir -p /var/run && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /var/run && \
    chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /etc/nginx/nginx.conf

# Switch to non-root user for security compliance
# Follows enterprise security standards for container execution
USER nginx

# ============================================================================
# NETWORK CONFIGURATION - Port Exposure
# ============================================================================
# Expose HTTP port for nginx web server
# Used by Kubernetes services and Docker Compose networking
EXPOSE 80

# ============================================================================
# HEALTH CHECK - Kubernetes Integration
# ============================================================================
# Health check endpoint for container orchestration
# Validates nginx is serving content and responding to requests
# Configured for Kubernetes readiness and liveness probes
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:80/health || exit 1

# ============================================================================
# CONTAINER ENTRYPOINT - NGINX Daemon
# ============================================================================
# Start nginx in foreground mode for container lifecycle management
# Prevents container exit and enables proper signal handling
CMD ["nginx", "-g", "daemon off;"]

# ============================================================================
# IMAGE METADATA - Build and Version Information
# ============================================================================
LABEL maintainer="CardDemo Development Team" \
      description="React 18.x Frontend for CardDemo Credit Card Management System" \
      version="2.1.3" \
      architecture="React SPA replacing BMS 3270 Terminal Screens" \
      components="18 React Components from BMS Mapsets" \
      security="Non-root execution, Alpine Linux base" \
      performance="Multi-stage build, nginx optimization"