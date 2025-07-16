# Multi-stage Docker build for CardDemo Spring Boot microservices
# Implements Eclipse Temurin OpenJDK 21 base images per Section 8.3.2
# Provides optimized Maven build and runtime stages per Section 8.3.3

# =============================================================================
# BUILD STAGE - Maven Compilation Environment
# =============================================================================
FROM eclipse-temurin:21-jdk-jammy AS build

# Set build environment metadata
LABEL stage=build
LABEL description="CardDemo Spring Boot microservices Maven build environment"
LABEL java.version="21"
LABEL build.tool="maven"

# Set working directory for build operations
WORKDIR /app

# Install Maven 3.9.x for multi-module project builds per Section 3.4.1
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Download and install Maven 3.9.6
ENV MAVEN_VERSION=3.9.6
ENV MAVEN_HOME=/opt/maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzC /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME}

# Add Maven to PATH
ENV PATH=${MAVEN_HOME}/bin:${PATH}

# Copy Maven configuration files first for optimal layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies in separate layer for better caching
RUN ./mvnw dependency:go-offline -B

# Copy source code and build application
COPY src ./src

# Build Spring Boot fat JAR with optimizations
# Skip tests in Docker build - tests run in CI/CD pipeline
RUN ./mvnw clean package -DskipTests -B \
    && mkdir -p target/dependency \
    && cd target/dependency \
    && jar -xf ../*.jar

# =============================================================================
# RUNTIME STAGE - Optimized Production Environment
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Set runtime environment metadata
LABEL stage=runtime
LABEL description="CardDemo Spring Boot microservices production runtime"
LABEL java.version="21"
LABEL base.image="eclipse-temurin:21-jre-alpine"
LABEL maintainer="CardDemo Development Team"
LABEL version="2.0.0"

# Create non-root user for security compliance per Section 8.3.6
RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Create application directory and set permissions
RUN mkdir -p /app/logs /app/tmp && \
    chown -R appuser:appuser /app

# Set working directory
WORKDIR /app

# Copy application JAR from build stage
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# JVM tuning parameters maintaining memory usage within 10% increase limit per Section 8.3.5
# Configured for G1GC with 200ms pause time target and heap dump capabilities
ENV JAVA_OPTS="-server \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/tmp/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+ShowCodeDetailsInExceptionMessages \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Dspring.profiles.active=prod"

# Memory configuration based on CICS baseline preservation per Section 8.3.5
# Default heap size can be overridden via Kubernetes deployment resources
ENV JAVA_MEMORY_OPTS="-Xms512m -Xmx768m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# Spring Boot configuration for production environment
ENV SPRING_CONFIG_LOCATION="classpath:/application.yml,classpath:/application-prod.yml"
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="health,info,metrics,prometheus"
ENV MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS="always"
ENV MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="true"
ENV MANAGEMENT_HEALTH_LIVENESSSTATE_ENABLED="true"
ENV MANAGEMENT_HEALTH_READINESSSTATE_ENABLED="true"

# Expose port 8080 for Spring Boot application and health checks per Section 8.3.7
EXPOSE 8080

# Switch to non-root user for security
USER appuser

# Health check configuration for Kubernetes liveness and readiness probes per Section 8.3.7
# Implements Spring Boot Actuator endpoints for container orchestration integration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Create volume mount points for logs and temporary files
VOLUME ["/app/logs", "/app/tmp"]

# Container startup command with optimized JVM parameters
# Combines memory settings, GC configuration, and Spring Boot properties
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} ${JAVA_MEMORY_OPTS} -jar app.jar"]

# =============================================================================
# Container Build Instructions and Usage
# =============================================================================
# 
# Build command:
#   docker build -t carddemo/spring-boot-service:2.0.0 .
#
# Run command:
#   docker run -p 8080:8080 \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e JAVA_MEMORY_OPTS="-Xms1g -Xmx1g" \
#     carddemo/spring-boot-service:2.0.0
#
# Kubernetes deployment example:
#   resources:
#     requests:
#       memory: "768Mi"
#       cpu: "200m"
#     limits:
#       memory: "768Mi"
#       cpu: "1000m"
#   livenessProbe:
#     httpGet:
#       path: /actuator/health/liveness
#       port: 8080
#     initialDelaySeconds: 60
#     periodSeconds: 30
#   readinessProbe:
#     httpGet:
#       path: /actuator/health/readiness
#       port: 8080
#     initialDelaySeconds: 30
#     periodSeconds: 10
#
# =============================================================================
# Security and Performance Features
# =============================================================================
#
# Security Features:
# - Non-root user execution (appuser:1000)
# - Read-only root filesystem compatible
# - Minimal Alpine Linux attack surface
# - No privilege escalation
# - Secure JVM random number generation
#
# Performance Features:
# - Multi-stage build with layer caching optimization
# - G1GC garbage collector for consistent response times
# - Compressed OOPs for reduced memory footprint
# - Optimized for Spring Boot fat JAR execution
# - Memory limits aligned with CICS baseline + 10% overhead
#
# Monitoring and Observability:
# - Spring Boot Actuator health endpoints
# - Prometheus metrics exposure
# - Heap dump generation on OOM
# - JVM metrics collection support
# - Distributed tracing ready
#
# =============================================================================