# Multi-stage Docker build for CardDemo Spring Boot microservices
# Based on Section 8.3.3 Multi-Stage Build Architecture
# Using Eclipse Temurin OpenJDK 21 per Section 8.3.2 Base Image Strategy

# Build stage - Maven compilation using Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-jammy AS build

# Set working directory for build operations
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
# This allows dependency resolution to be cached separately from source code
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies in separate layer for optimal caching
# Dependencies change less frequently than source code
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the application
COPY src ./src

# Build the application producing fat-jar artifact per Section 8.3.3
# Skip tests during Docker build as they should run in CI/CD pipeline
RUN ./mvnw clean package -DskipTests -B

# Runtime stage - Minimal production container using Eclipse Temurin JRE 21 Alpine
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user for security compliance per Section 8.3.6
# Non-root execution required for enterprise security standards
RUN addgroup -g 1000 carddemo && \
    adduser -D -s /bin/sh -u 1000 -G carddemo carddemo

# Install wget for health checks and ca-certificates for HTTPS
RUN apk add --no-cache wget ca-certificates

# Set working directory for runtime operations
WORKDIR /app

# Create directories with proper ownership for non-root execution
RUN mkdir -p /app/logs /tmp/heapdump && \
    chown -R carddemo:carddemo /app /tmp/heapdump

# Copy the fat-jar from build stage
COPY --from=build --chown=carddemo:carddemo /app/target/*.jar app.jar

# JVM tuning parameters for production per Section 8.3.5
# Memory optimization to maintain usage within 10% overhead of CICS baseline
# G1GC with 200ms pause time target for consistent response times
ENV JAVA_OPTS="-server \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod \
    -Dmanagement.endpoints.web.exposure.include=health,metrics,info,prometheus \
    -Dmanagement.endpoint.health.show-details=when-authorized \
    -Dmanagement.server.port=8080"

# Default JVM memory settings - can be overridden at runtime
# Configured to meet <30 second startup time and <85% memory utilization targets
ENV JVM_MEMORY_OPTS="-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# Combine JVM options for runtime execution
ENV FULL_JAVA_OPTS="$JVM_MEMORY_OPTS $JAVA_OPTS"

# Expose port 8080 for Spring Boot application and health checks
# Health check integration for Kubernetes liveness and readiness probes per Section 8.3.7
EXPOSE 8080

# Switch to non-root user for security compliance
USER carddemo

# Health check configuration for container orchestration
# Spring Boot Actuator health endpoint for Kubernetes probe integration
# 60s start period to accommodate <30s startup time requirement from Section 8.3.8
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Application entry point with JVM options
# Using exec form to ensure proper signal handling in Kubernetes
ENTRYPOINT ["sh", "-c", "exec java $FULL_JAVA_OPTS -jar app.jar"]

# Container metadata for operational transparency and traceability
LABEL maintainer="Blitzy Platform Engineering Team" \
      version="1.0.0" \
      description="CardDemo Spring Boot Microservice - Cloud-Native Financial Processing System" \
      org.opencontainers.image.title="CardDemo Spring Boot Container" \
      org.opencontainers.image.description="Multi-stage Docker container for CardDemo microservices transformation from COBOL/CICS to Java/Spring Boot" \
      org.opencontainers.image.vendor="Blitzy Platform" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.source="https://github.com/blitzy-public-samples/carddemo" \
      org.opencontainers.image.documentation="Implements enterprise-grade containerization with Eclipse Temurin OpenJDK 21" \
      org.opencontainers.image.licenses="MIT" \
      carddemo.performance.target-response-time="200ms" \
      carddemo.performance.target-throughput="10000TPS" \
      carddemo.compliance.memory-overhead="<10%" \
      carddemo.architecture.base-image="eclipse-temurin:21-jre-alpine" \
      carddemo.security.non-root-user="carddemo:1000"