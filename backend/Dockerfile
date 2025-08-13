# =============================================================================
# CardDemo Backend Service - Multi-Stage Docker Build
# =============================================================================
# Multi-stage Docker build file for the Spring Boot backend service.
# Uses eclipse-temurin:21.0.8-jdk-alpine as base image for Java 21 runtime,
# implements a builder stage for Maven compilation, configures JVM memory 
# settings for containerized environments, exposes port 8080 for REST API 
# access, and defines health check endpoints via Spring Boot Actuator.
#
# This Dockerfile supports the complete transformation from COBOL/CICS 
# mainframe architecture to modern cloud-native Java 21 Spring Boot 
# microservices as specified in the CardDemo system migration.
# =============================================================================

# -----------------------------------------------------------------------------
# Build Stage - Java Compilation and Dependency Resolution
# -----------------------------------------------------------------------------
# Uses JDK for compilation and dependency resolution
FROM eclipse-temurin:21.0.8-jdk-alpine AS build-stage

# Set working directory for build operations
WORKDIR /workspace/app

# Install build dependencies and create build user for security
RUN apk add --no-cache \
    curl \
    && addgroup -S builduser \
    && adduser -S builduser -G builduser

# -----------------------------------------------------------------------------
# Dependency Layer - Cached Maven Dependencies
# -----------------------------------------------------------------------------
# Copy Maven configuration files first for optimal Docker layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Make Maven wrapper executable and resolve dependencies offline
# This layer will be cached and reused if pom.xml doesn't change
RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B --no-transfer-progress

# -----------------------------------------------------------------------------
# Source Layer - Application Source Code Compilation
# -----------------------------------------------------------------------------
# Copy application source code and compile
COPY src src

# Build the application JAR with Maven
# Skip tests for faster builds (tests should run in CI pipeline)
RUN ./mvnw clean package -DskipTests --no-transfer-progress \
    && ls -la target/

# -----------------------------------------------------------------------------
# Runtime Stage - Minimal Production Image
# -----------------------------------------------------------------------------
# Use JRE instead of JDK for reduced image size and attack surface
FROM eclipse-temurin:21.0.8-jre-alpine AS runtime-stage

# Install runtime dependencies for Spring Boot
RUN apk add --no-cache \
    curl \
    wget \
    && rm -rf /var/cache/apk/*

# Create dedicated Spring Boot user for non-root execution
# Following security best practices for container hardening
RUN addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

# Switch to non-root user for security compliance
USER spring:spring

# Set working directory for the application
WORKDIR /app

# Copy the compiled JAR from build stage
# Use the Spring Boot fat JAR for simplified deployment
COPY --from=build-stage --chown=spring:spring /workspace/app/target/*.jar app.jar

# -----------------------------------------------------------------------------
# Container Configuration
# -----------------------------------------------------------------------------

# Expose port 8080 for Spring Boot REST API access
# This port will be used by Kubernetes services and ingress controllers
EXPOSE 8080

# Configure JVM for containerized environment
# Memory settings optimized for container resource constraints
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Spring Boot specific environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# -----------------------------------------------------------------------------
# Health Check Configuration
# -----------------------------------------------------------------------------
# Define health check using Spring Boot Actuator endpoints
# This enables Kubernetes readiness and liveness probes
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# -----------------------------------------------------------------------------
# Application Entrypoint
# -----------------------------------------------------------------------------
# Start the Spring Boot application with optimized JVM settings
# Uses exec form to ensure proper signal handling in containers
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# -----------------------------------------------------------------------------
# Metadata Labels
# -----------------------------------------------------------------------------
# Add metadata labels for container identification and management
LABEL maintainer="CardDemo Development Team" \
      application="carddemo-backend" \
      description="Spring Boot backend service for CardDemo credit card management system" \
      java.version="21.0.8" \
      spring.boot.version="3.2.x" \
      base.image="eclipse-temurin:21.0.8-jre-alpine" \
      build.stage="multi-stage" \
      security.user="spring" \
      health.check="actuator"