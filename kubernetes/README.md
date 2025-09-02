# CardDemo Kubernetes Deployment Guide

This guide provides comprehensive instructions for deploying and operating the modernized CardDemo application in Kubernetes environments. The CardDemo system represents a complete transformation from IBM mainframe COBOL/CICS architecture to a modern cloud-native Java 21/Spring Boot platform.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Architecture Overview](#architecture-overview)
- [Quick Start](#quick-start)
- [Deployment Options](#deployment-options)
- [Kubernetes Resources](#kubernetes-resources)
- [Configuration Management](#configuration-management)
- [Scaling and Performance](#scaling-and-performance)
- [Monitoring Integration](#monitoring-integration)
- [Security Configuration](#security-configuration)
- [Troubleshooting](#troubleshooting)
- [Maintenance Procedures](#maintenance-procedures)
- [Environment-Specific Deployment](#environment-specific-deployment)

## Overview

CardDemo is a modernized credit card management system that processes financial transactions, user management, and batch operations. The Kubernetes deployment supports:

- **Interactive Transaction Processing**: Sub-200ms response time SLA for REST API operations
- **Batch Processing**: Daily processing jobs completing within 4-hour windows
- **High Availability**: 99.9% uptime with multi-pod deployment and automatic recovery
- **Horizontal Scaling**: Dynamic scaling based on CPU, memory, and request metrics
- **Financial Compliance**: PCI DSS, SOX, and Basel III regulatory requirements

### System Components

| Component | Technology | Purpose | Scaling Strategy |
|-----------|------------|---------|------------------|
| **Backend Services** | Spring Boot 3.2.x, Java 21 | REST API and business logic | HPA 3-10 replicas |
| **API Gateway** | Spring Cloud Gateway | Request routing and security | HPA 2-6 replicas |
| **Database** | PostgreSQL 15.x | Data persistence | Vertical scaling |
| **Session Store** | Redis 7.x | Session management | Redis Cluster mode |
| **Frontend** | React 18.x (served via nginx) | User interface | HPA 2-5 replicas |

## Prerequisites

### Kubernetes Cluster Requirements

- **Kubernetes Version**: 1.28+ (required for latest resource management features)
- **Architecture**: x86-64 Linux nodes with Java 21 compatibility
- **Minimum Configuration**: 
  - 3 worker nodes for high availability
  - 4 vCPU, 16 GiB RAM per node (recommended)
  - 100GB+ persistent storage per node
- **Container Runtime**: Docker Engine 27.x or containerd
- **Network Plugin**: Any CNI-compatible network (Calico, Flannel, Weave)

### Required Tools

```bash
# kubectl (version 1.28+)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# Optional: kustomize for configuration management
curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash

# Optional: helm for package management
curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | sudo tee /usr/share/keyrings/helm.gpg > /dev/null
```

### Cluster Access Verification

```bash
# Verify cluster connectivity
kubectl cluster-info

# Check node readiness
kubectl get nodes

# Verify required storage classes
kubectl get storageclass

# Confirm RBAC permissions
kubectl auth can-i create deployments
kubectl auth can-i create services
kubectl auth can-i create configmaps
kubectl auth can-i create secrets
```

### Infrastructure Dependencies

- **Load Balancer**: Cloud provider load balancer or ingress controller
- **DNS**: Ability to configure DNS for external access
- **Storage**: Dynamic persistent volume provisioning
- **Monitoring** (Optional): Prometheus and Grafana for observability

## Architecture Overview

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Ingress   │  │     CDN     │  │   External  │        │
│  │ Controller  │  │   (Static   │  │     DNS     │        │
│  │             │  │  Assets)    │  │             │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   React     │  │   Spring    │  │    Spring   │        │
│  │  Frontend   │  │   Cloud     │  │    Boot     │        │
│  │   (nginx)   │  │   Gateway   │  │   Backend   │        │
│  │   2-5 pods  │  │   2-6 pods  │  │   3-10 pods │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ PostgreSQL  │  │    Redis    │  │   Spring    │        │
│  │ StatefulSet │  │   Session   │  │    Batch    │        │
│  │   1 primary │  │   Store     │  │    Jobs     │        │
│  │ + replicas  │  │  2-3 pods   │  │  (CronJobs) │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Prometheus  │  │   Grafana   │  │    ELK      │        │
│  │ Monitoring  │  │ Dashboards  │  │   Stack     │        │
│  │ (Optional)  │  │ (Optional)  │  │ (Optional)  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Network Flow

1. **External Traffic** → Ingress Controller → Services
2. **API Requests** → Spring Cloud Gateway → Backend Services
3. **Database Access** → JPA Repositories → PostgreSQL
4. **Session Management** → Spring Session → Redis
5. **Static Assets** → nginx → React Frontend
6. **Batch Processing** → Kubernetes CronJobs → Spring Batch

## Quick Start

### 1. Clone and Navigate to Kubernetes Manifests

```bash
git clone <repository-url>
cd carddemo/kubernetes
```

### 2. Create Namespace and Apply Base Configuration

```bash
# Create dedicated namespace
kubectl create namespace carddemo

# Apply secrets (update with actual values)
kubectl apply -f secrets/ -n carddemo

# Apply configuration maps
kubectl apply -f configmaps/ -n carddemo
```

### 3. Deploy Core Infrastructure

```bash
# Deploy PostgreSQL database
kubectl apply -f postgres/ -n carddemo

# Deploy Redis session store
kubectl apply -f redis/ -n carddemo

# Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app=postgresql -n carddemo --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n carddemo --timeout=300s
```

### 4. Deploy Application Services

```bash
# Deploy backend services
kubectl apply -f backend/ -n carddemo

# Deploy API gateway
kubectl apply -f gateway/ -n carddemo

# Deploy frontend
kubectl apply -f frontend/ -n carddemo

# Wait for application readiness
kubectl wait --for=condition=ready pod -l app=carddemo-backend -n carddemo --timeout=300s
```

### 5. Configure External Access

```bash
# Apply ingress configuration
kubectl apply -f ingress/ -n carddemo

# Get external IP/hostname
kubectl get ingress carddemo-ingress -n carddemo
```

### 6. Verify Deployment

```bash
# Check all pod status
kubectl get pods -n carddemo

# Verify services
kubectl get services -n carddemo

# Test application health
kubectl port-forward svc/carddemo-gateway-service 8080:8080 -n carddemo
curl http://localhost:8080/actuator/health
```

## Deployment Options

### Option 1: kubectl with Raw Manifests

**Best for**: Development environments, simple deployments

```bash
# Apply all manifests in order
kubectl apply -f namespace.yaml
kubectl apply -f secrets/
kubectl apply -f configmaps/
kubectl apply -f postgres/
kubectl apply -f redis/
kubectl apply -f backend/
kubectl apply -f gateway/
kubectl apply -f frontend/
kubectl apply -f ingress/
kubectl apply -f monitoring/ # if monitoring is enabled
```

### Option 2: kustomize for Environment Management

**Best for**: Multiple environments with configuration variations

```bash
# Development environment
kubectl apply -k overlays/development

# QA environment
kubectl apply -k overlays/qa

# Production environment
kubectl apply -k overlays/production
```

#### kustomize Directory Structure

```
kubernetes/
├── base/
│   ├── kustomization.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── configmap.yaml
└── overlays/
    ├── development/
    │   ├── kustomization.yaml
    │   ├── replica-patch.yaml
    │   └── resource-patch.yaml
    ├── qa/
    │   ├── kustomization.yaml
    │   └── scaling-patch.yaml
    └── production/
        ├── kustomization.yaml
        ├── production-config.yaml
        └── security-patch.yaml
```

### Option 3: Helm Chart Deployment

**Best for**: Complex configurations, parameter management

```bash
# Install from chart repository
helm repo add carddemo https://charts.carddemo.com
helm install carddemo carddemo/carddemo --namespace carddemo --create-namespace

# Install from local chart
helm install carddemo ./helm-chart --namespace carddemo --create-namespace --values values-production.yaml
```

## Kubernetes Resources

### Core Application Resources

#### Backend Service Deployment

**File**: `backend/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: carddemo-backend
  labels:
    app: carddemo-backend
    tier: application
    version: v2.1.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: carddemo-backend
  template:
    metadata:
      labels:
        app: carddemo-backend
        version: v2.1.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 2000
      containers:
      - name: backend-service
        image: carddemo/backend-service:v2.1.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 2
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: JAVA_OPTS
          value: "-Xmx768m -Xms512m -XX:+UseG1GC"
        envFrom:
        - configMapRef:
            name: carddemo-backend-config
        - secretRef:
            name: carddemo-backend-secret
        volumeMounts:
        - name: tmp-volume
          mountPath: /tmp
        - name: logs-volume
          mountPath: /app/logs
      volumes:
      - name: tmp-volume
        emptyDir: {}
      - name: logs-volume
        emptyDir: {}
      terminationGracePeriodSeconds: 30
```

**Purpose**: Deploys the main Spring Boot application containing all business logic translated from COBOL programs.

**Key Features**:
- **Rolling Updates**: Zero-downtime deployments with maxUnavailable: 0
- **Health Probes**: Spring Boot Actuator integration for readiness/liveness
- **Resource Management**: Proper CPU/memory limits for JVM optimization
- **Security**: Non-root execution with proper security context
- **Observability**: Prometheus metrics exposure for monitoring

#### PostgreSQL StatefulSet

**File**: `postgres/statefulset.yaml`

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql
  labels:
    app: postgresql
    tier: database
spec:
  serviceName: postgresql-service
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
        tier: database
    spec:
      securityContext:
        fsGroup: 999
      containers:
      - name: postgresql
        image: postgres:15.4
        ports:
        - containerPort: 5432
          name: postgresql
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        env:
        - name: POSTGRES_DB
          value: "carddemo"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: password
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        volumeMounts:
        - name: postgresql-storage
          mountPath: /var/lib/postgresql/data
        - name: postgresql-config
          mountPath: /etc/postgresql
        - name: init-scripts
          mountPath: /docker-entrypoint-initdb.d
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - $(POSTGRES_DB)
          initialDelaySeconds: 30
          periodSeconds: 30
          timeoutSeconds: 5
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - $(POSTGRES_DB)
          initialDelaySeconds: 15
          periodSeconds: 10
          timeoutSeconds: 3
      volumes:
      - name: postgresql-config
        configMap:
          name: postgresql-config
      - name: init-scripts
        configMap:
          name: postgresql-init-scripts
  volumeClaimTemplates:
  - metadata:
      name: postgresql-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 100Gi
```

**Purpose**: Provides persistent data storage for all CardDemo financial data, replacing VSAM datasets.

**Key Features**:
- **StatefulSet**: Ensures stable network identity and persistent storage
- **Data Persistence**: Persistent volume claims for data durability
- **Initialization**: Database schema and data loading via init scripts
- **Performance**: Optimized configuration for financial transaction processing
- **Backup Ready**: Configuration supports automated backup procedures

#### Redis Session Store

**File**: `redis/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  labels:
    app: redis
    tier: cache
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
        tier: cache
    spec:
      containers:
      - name: redis
        image: redis:7.2-alpine
        ports:
        - containerPort: 6379
          name: redis
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "200m"
        command:
        - redis-server
        - /etc/redis/redis.conf
        volumeMounts:
        - name: redis-config
          mountPath: /etc/redis
        - name: redis-data
          mountPath: /data
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 15
          periodSeconds: 10
      volumes:
      - name: redis-config
        configMap:
          name: redis-config
      - name: redis-data
        persistentVolumeClaim:
          claimName: redis-data-pvc
```

**Purpose**: Manages session state for user authentication and transaction context, replacing CICS COMMAREA.

### Auto-Scaling Configuration

#### Horizontal Pod Autoscaler

**File**: `autoscaling/backend-hpa.yaml`

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: carddemo-backend-hpa
  labels:
    app: carddemo-backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: carddemo-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
```

**Purpose**: Automatically scales backend services based on CPU, memory, and request metrics to maintain sub-200ms response times.

### Batch Processing

#### CronJob for Daily Batch Processing

**File**: `batch/daily-transaction-job.yaml`

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: daily-transaction-processing
  labels:
    app: carddemo-batch
    job-type: daily-processing
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  timeZone: "America/New_York"
  successfulJobsHistoryLimit: 7
  failedJobsHistoryLimit: 3
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      backoffLimit: 2
      activeDeadlineSeconds: 14400  # 4 hours maximum
      template:
        metadata:
          labels:
            app: carddemo-batch
            job-type: daily-processing
        spec:
          restartPolicy: OnFailure
          containers:
          - name: batch-processor
            image: carddemo/batch-processor:v2.1.0
            resources:
              requests:
                memory: "1Gi"
                cpu: "500m"
              limits:
                memory: "2Gi"
                cpu: "1000m"
            env:
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes,batch"
            - name: BATCH_JOB_NAME
              value: "dailyTransactionProcessingJob"
            envFrom:
            - configMapRef:
                name: carddemo-batch-config
            - secretRef:
                name: carddemo-batch-secret
            volumeMounts:
            - name: batch-reports
              mountPath: /app/reports
          volumes:
          - name: batch-reports
            persistentVolumeClaim:
              claimName: batch-reports-pvc
```

**Purpose**: Executes daily batch processing jobs equivalent to mainframe JCL, including transaction processing, interest calculations, and statement generation.

## Configuration Management

### ConfigMaps

#### Backend Service Configuration

**File**: `configmaps/backend-config.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: carddemo-backend-config
data:
  application.yml: |
    server:
      port: 8080
      servlet:
        context-path: /api
      shutdown: graceful
    
    spring:
      application:
        name: carddemo-backend
      profiles:
        active: kubernetes
      
      datasource:
        url: jdbc:postgresql://postgresql-service:5432/carddemo
        driver-class-name: org.postgresql.Driver
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000
          leak-detection-threshold: 60000
      
      jpa:
        hibernate:
          ddl-auto: validate
        show-sql: false
        properties:
          hibernate:
            dialect: org.hibernate.dialect.PostgreSQLDialect
            format_sql: false
            jdbc:
              batch_size: 50
      
      session:
        store-type: redis
        redis:
          namespace: carddemo:sessions
          flush-mode: on_save
        timeout: 30m
      
      data:
        redis:
          host: redis-service
          port: 6379
          timeout: 2000ms
          lettuce:
            pool:
              max-active: 10
              max-idle: 10
              min-idle: 1
      
      security:
        jwt:
          secret: ${JWT_SECRET}
          expiration: 86400000  # 24 hours
      
      actuator:
        endpoints:
          web:
            exposure:
              include: "health,metrics,info,prometheus"
            base-path: /actuator
        endpoint:
          health:
            show-details: when-authorized
            probes:
              enabled: true
        metrics:
          export:
            prometheus:
              enabled: true
    
    management:
      health:
        readiness-state:
          enabled: true
        liveness-state:
          enabled: true
      metrics:
        export:
          prometheus:
            enabled: true
    
    logging:
      level:
        com.carddemo: INFO
        org.springframework.security: WARN
        org.hibernate.SQL: WARN
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
      file:
        name: /app/logs/carddemo-backend.log
    
    carddemo:
      transaction:
        timeout: 30000
        retry-attempts: 3
      batch:
        chunk-size: 1000
        skip-limit: 10
      security:
        cors:
          allowed-origins: "*"
          allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
          allowed-headers: "*"
          max-age: 3600
```

**Purpose**: Provides comprehensive configuration for Spring Boot backend services, including database connections, session management, security settings, and monitoring.

### Secrets Management

#### Database Credentials

**File**: `secrets/postgresql-secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-secret
type: Opaque
stringData:
  username: "carddemo_user"
  password: "CHANGE_THIS_PASSWORD_IN_PRODUCTION"
  postgres-password: "CHANGE_THIS_ROOT_PASSWORD_IN_PRODUCTION"
```

#### Application Secrets

**File**: `secrets/backend-secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: carddemo-backend-secret
type: Opaque
stringData:
  DATABASE_URL: "jdbc:postgresql://postgresql-service:5432/carddemo"
  DATABASE_USERNAME: "carddemo_user"
  DATABASE_PASSWORD: "CHANGE_THIS_PASSWORD_IN_PRODUCTION"
  REDIS_URL: "redis://redis-service:6379"
  JWT_SECRET: "CHANGE_THIS_JWT_SECRET_IN_PRODUCTION_AT_LEAST_64_CHARS_LONG"
  ENCRYPTION_KEY: "CHANGE_THIS_ENCRYPTION_KEY_IN_PRODUCTION"
```

**Security Notes**:
- Always update default passwords before production deployment
- Use external secret management systems (Vault, AWS Secrets Manager) for production
- Rotate secrets regularly according to security policies
- Never commit actual secrets to version control

## Scaling and Performance

### Performance Targets

| Metric | Target | Monitoring Method |
|--------|--------|-------------------|
| **Response Time** | < 200ms (95th percentile) | Prometheus + Actuator metrics |
| **Throughput** | 1000+ TPS per service instance | HTTP request metrics |
| **Availability** | 99.9% business hours | Health probe success rate |
| **Batch Processing** | < 4 hours completion | Spring Batch job duration |
| **Error Rate** | < 1% of total requests | HTTP error status tracking |

### Scaling Strategies

#### Horizontal Scaling Configuration

```yaml
# Backend Service Scaling
spec:
  minReplicas: 3      # Minimum for high availability
  maxReplicas: 10     # Maximum based on cluster capacity
  
  # Scale up quickly under load
  scaleUp:
    stabilizationWindowSeconds: 60    # Wait 1 minute before additional scaling
    policies:
    - type: Percent
      value: 100      # Double pods if needed
      periodSeconds: 15
  
  # Scale down gradually
  scaleDown:
    stabilizationWindowSeconds: 300   # Wait 5 minutes before scale down
    policies:
    - type: Percent
      value: 10       # Reduce by 10% at a time
      periodSeconds: 60
```

#### Resource Optimization

**Development Environment**:
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "125m"
  limits:
    memory: "512Mi"
    cpu: "250m"
```

**Production Environment**:
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

**High-Load Environment**:
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

### Performance Optimization

#### JVM Tuning

```yaml
env:
- name: JAVA_OPTS
  value: "-Xmx768m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

#### Database Connection Optimization

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # Max connections
      minimum-idle: 5              # Min idle connections
      connection-timeout: 30000    # 30 seconds
      idle-timeout: 600000         # 10 minutes
      max-lifetime: 1800000        # 30 minutes
      leak-detection-threshold: 60000  # 1 minute
```

#### Redis Session Optimization

```yaml
spring:
  session:
    timeout: 30m                   # Session timeout
    redis:
      flush-mode: on_save          # When to persist sessions
      namespace: carddemo:sessions # Key namespace
  data:
    redis:
      lettuce:
        pool:
          max-active: 10           # Max active connections
          max-idle: 10             # Max idle connections
          min-idle: 1              # Min idle connections
```

## Monitoring Integration

CardDemo integrates with cloud-native monitoring solutions for comprehensive observability:

### Spring Boot Actuator Integration

#### Health Endpoints

```bash
# Application health status
curl http://<service>/actuator/health

# Detailed health with components
curl http://<service>/actuator/health/readiness
curl http://<service>/actuator/health/liveness

# Database connectivity
curl http://<service>/actuator/health/db

# Redis connectivity  
curl http://<service>/actuator/health/redis
```

#### Metrics Endpoints

```bash
# Prometheus-compatible metrics
curl http://<service>/actuator/prometheus

# JVM metrics
curl http://<service>/actuator/metrics/jvm.memory.used
curl http://<service>/actuator/metrics/jvm.gc.pause

# HTTP request metrics
curl http://<service>/actuator/metrics/http.server.requests

# Business metrics
curl http://<service>/actuator/metrics/carddemo.transactions.count
curl http://<service>/actuator/metrics/carddemo.authentication.success
```

### Prometheus Integration

#### Service Monitor Configuration

**File**: `monitoring/servicemonitor.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: carddemo-backend
  labels:
    app: carddemo-backend
spec:
  selector:
    matchLabels:
      app: carddemo-backend
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

#### Alert Rules

**File**: `monitoring/alerts.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: carddemo-alerts
spec:
  groups:
  - name: carddemo.performance
    rules:
    - alert: HighResponseTime
      expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/.*"}[5m])) by (le)) > 0.2
      for: 2m
      labels:
        severity: warning
      annotations:
        summary: "CardDemo response time exceeding 200ms SLA"
        description: "95th percentile response time is {{ $value }}s"

    - alert: HighErrorRate
      expr: sum(rate(http_server_requests_total{status=~"5.."}[5m])) / sum(rate(http_server_requests_total[5m])) > 0.01
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "CardDemo error rate exceeding 1% threshold"
        description: "Error rate is {{ $value | humanizePercentage }}"

    - alert: DatabaseConnectionFailure
      expr: up{job="postgresql"} == 0
      for: 30s
      labels:
        severity: critical
      annotations:
        summary: "PostgreSQL database is down"
        description: "PostgreSQL database has been down for more than 30 seconds"
```

### Grafana Dashboards

#### Application Performance Dashboard

Key panels include:
- **Response Time Trends**: HTTP request duration over time
- **Throughput Metrics**: Requests per second by endpoint
- **Error Rate Tracking**: Error percentage and error count
- **JVM Performance**: Memory usage, GC activity, thread count
- **Database Metrics**: Connection pool status, query performance
- **Business Metrics**: Transaction counts, authentication success rate

#### Infrastructure Dashboard

Key panels include:
- **Pod Resource Usage**: CPU and memory utilization
- **Container Health**: Pod status and restart counts
- **Network Performance**: Ingress/egress traffic
- **Storage Utilization**: Persistent volume usage
- **Cluster Overview**: Node status and capacity

### Custom Metrics

#### Business Process Metrics

```java
// Example custom metrics in Spring Boot application
@Component
public class TransactionMetrics {
    
    private final Counter transactionCounter = Metrics.counter("carddemo.transactions.total",
        "type", "all");
    
    private final Timer transactionTimer = Metrics.timer("carddemo.transactions.duration");
    
    private final Gauge activeSessionsGauge = Metrics.gauge("carddemo.sessions.active",
        this, TransactionMetrics::getActiveSessionCount);
    
    public void recordTransaction(String type) {
        Metrics.counter("carddemo.transactions.total", "type", type).increment();
    }
    
    public void recordTransactionDuration(Duration duration) {
        transactionTimer.record(duration);
    }
}
```

#### Financial Accuracy Metrics

```java
@Component
public class FinancialMetrics {
    
    private final Counter balanceVerificationCounter = Metrics.counter(
        "carddemo.balance.verification.total", "result", "unknown");
    
    private final Counter calculationErrorCounter = Metrics.counter(
        "carddemo.calculation.errors.total", "type", "unknown");
    
    public void recordBalanceVerification(boolean passed) {
        Metrics.counter("carddemo.balance.verification.total", 
            "result", passed ? "passed" : "failed").increment();
    }
    
    public void recordCalculationError(String errorType) {
        Metrics.counter("carddemo.calculation.errors.total", 
            "type", errorType).increment();
    }
}
```

## Security Configuration

### Pod Security Standards

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: carddemo-backend
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    runAsGroup: 2000
    fsGroup: 2000
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: backend
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
    resources:
      limits:
        memory: "1Gi"
        cpu: "500m"
      requests:
        memory: "512Mi"
        cpu: "250m"
```

### Network Policies

#### Backend Service Network Policy

**File**: `security/backend-network-policy.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: carddemo-backend-netpol
spec:
  podSelector:
    matchLabels:
      app: carddemo-backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: carddemo-gateway
    - podSelector:
        matchLabels:
          app: prometheus
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgresql
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
  - to: []  # Allow DNS
    ports:
    - protocol: UDP
      port: 53
```

### RBAC Configuration

#### Service Account and Roles

**File**: `security/rbac.yaml`

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: carddemo-backend
  namespace: carddemo
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: carddemo
  name: carddemo-backend-role
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: carddemo-backend-rolebinding
  namespace: carddemo
subjects:
- kind: ServiceAccount
  name: carddemo-backend
  namespace: carddemo
roleRef:
  kind: Role
  name: carddemo-backend-role
  apiGroup: rbac.authorization.k8s.io
```

### TLS Configuration

#### Ingress TLS

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: carddemo-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - carddemo.example.com
    secretName: carddemo-tls-secret
  rules:
  - host: carddemo.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: carddemo-frontend-service
            port:
              number: 80
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: carddemo-gateway-service
            port:
              number: 8080
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Pod Startup Failures

**Symptoms**:
- Pods stuck in `Pending` or `CrashLoopBackOff` state
- Application fails to start or immediately exits

**Diagnostics**:
```bash
# Check pod status and events
kubectl describe pod <pod-name> -n carddemo

# View pod logs
kubectl logs <pod-name> -n carddemo --previous

# Check resource availability
kubectl top nodes
kubectl describe nodes
```

**Common Causes and Solutions**:

| Issue | Cause | Solution |
|-------|-------|----------|
| **Insufficient Resources** | Node doesn't have enough CPU/memory | Reduce resource requests or add more nodes |
| **Image Pull Errors** | Container image not available | Verify image name, tag, and registry access |
| **Configuration Errors** | Invalid ConfigMap or Secret references | Check ConfigMap/Secret names and keys |
| **Storage Issues** | PVC not bound or storage class missing | Verify storage class and PVC status |
| **Database Connection** | PostgreSQL not ready or wrong credentials | Check database pod status and secrets |

#### 2. Database Connection Issues

**Symptoms**:
- Application logs show database connection errors
- Health checks failing for database connectivity
- Transaction processing failures

**Diagnostics**:
```bash
# Check PostgreSQL pod status
kubectl get pods -l app=postgresql -n carddemo

# View PostgreSQL logs
kubectl logs -l app=postgresql -n carddemo

# Test database connectivity from application pod
kubectl exec -it <backend-pod> -n carddemo -- \
  psql postgresql://carddemo_user:password@postgresql-service:5432/carddemo -c "SELECT 1;"

# Check service discovery
kubectl get svc postgresql-service -n carddemo
kubectl get endpoints postgresql-service -n carddemo
```

**Solutions**:
- Verify PostgreSQL StatefulSet is running and ready
- Check database credentials in secrets
- Ensure network policies allow database communication
- Verify service name resolution within cluster

#### 3. Performance Issues

**Symptoms**:
- High response times (> 200ms)
- Timeout errors in application logs
- CPU or memory usage at limits

**Diagnostics**:
```bash
# Check resource usage
kubectl top pods -n carddemo

# Review HPA status
kubectl get hpa -n carddemo

# Check Prometheus metrics
curl http://<pod-ip>:8080/actuator/prometheus | grep http_server_requests_seconds

# View detailed performance metrics
kubectl exec -it <backend-pod> -n carddemo -- \
  curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**Performance Optimization Actions**:

1. **Scale Up Resources**:
```bash
# Increase HPA max replicas
kubectl patch hpa carddemo-backend-hpa -n carddemo -p '{"spec":{"maxReplicas":15}}'

# Update resource limits
kubectl patch deployment carddemo-backend -n carddemo -p '{"spec":{"template":{"spec":{"containers":[{"name":"backend-service","resources":{"limits":{"memory":"2Gi","cpu":"1000m"}}}]}}}}'
```

2. **Optimize Application Configuration**:
```yaml
# Increase database connection pool
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

3. **Database Performance**:
```bash
# Check PostgreSQL performance
kubectl exec -it postgresql-0 -n carddemo -- \
  psql -U carddemo_user -d carddemo -c "SELECT * FROM pg_stat_activity;"
```

#### 4. Session Management Issues

**Symptoms**:
- Users getting logged out unexpectedly
- Session data not persisting across requests
- Redis connection errors

**Diagnostics**:
```bash
# Check Redis pod status
kubectl get pods -l app=redis -n carddemo

# Test Redis connectivity
kubectl exec -it <backend-pod> -n carddemo -- \
  redis-cli -h redis-service -p 6379 ping

# View Redis statistics
kubectl exec -it <redis-pod> -n carddemo -- \
  redis-cli info stats
```

**Solutions**:
- Verify Redis deployment is healthy and accessible
- Check session timeout configuration
- Ensure Redis persistence is configured correctly
- Verify network connectivity between backend and Redis

#### 5. Batch Job Failures

**Symptoms**:
- CronJobs not executing on schedule
- Batch jobs failing or taking too long
- Incomplete data processing

**Diagnostics**:
```bash
# Check CronJob status
kubectl get cronjobs -n carddemo

# View job execution history
kubectl get jobs -n carddemo

# Check job pod logs
kubectl logs job/<job-name> -n carddemo

# Review batch processing duration
kubectl describe job <job-name> -n carddemo
```

**Solutions**:
- Verify CronJob schedule syntax and timezone
- Check job resource limits and deadlines
- Review batch processing logic for errors
- Ensure database and file system access

#### 6. Ingress and External Access Issues

**Symptoms**:
- Website not accessible from external browsers
- SSL certificate errors
- 404 or 502 errors from load balancer

**Diagnostics**:
```bash
# Check ingress status
kubectl get ingress -n carddemo

# View ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx

# Test internal service connectivity
kubectl exec -it <test-pod> -n carddemo -- \
  curl http://carddemo-gateway-service:8080/actuator/health

# Check DNS resolution
nslookup carddemo.example.com
```

**Solutions**:
- Verify DNS configuration and domain mapping
- Check TLS certificate provisioning and validity
- Ensure ingress controller is properly configured
- Test internal service connectivity before external access

### Debugging Commands Reference

#### Pod Troubleshooting

```bash
# Get all pods with wide output
kubectl get pods -n carddemo -o wide

# Watch pod status changes
kubectl get pods -n carddemo -w

# Describe specific pod for events and configuration
kubectl describe pod <pod-name> -n carddemo

# Get pod logs (current container)
kubectl logs <pod-name> -n carddemo

# Get pod logs (previous container instance)
kubectl logs <pod-name> -n carddemo --previous

# Get logs from all containers in a pod
kubectl logs <pod-name> -n carddemo --all-containers

# Follow logs in real-time
kubectl logs -f <pod-name> -n carddemo

# Execute commands in running pod
kubectl exec -it <pod-name> -n carddemo -- /bin/bash

# Copy files to/from pod
kubectl cp <local-file> carddemo/<pod-name>:/path/to/destination
kubectl cp carddemo/<pod-name>:/path/to/source <local-file>
```

#### Service and Network Troubleshooting

```bash
# Get all services and their endpoints
kubectl get svc -n carddemo
kubectl get endpoints -n carddemo

# Test service connectivity from within cluster
kubectl run debug-pod --image=nicolaka/netshoot -n carddemo --rm -it -- /bin/bash

# Port forward for local testing
kubectl port-forward svc/carddemo-backend-service 8080:8080 -n carddemo

# Check network policies
kubectl get networkpolicies -n carddemo

# Describe network policy rules
kubectl describe networkpolicy <policy-name> -n carddemo
```

#### Resource and Performance Troubleshooting

```bash
# Check resource usage
kubectl top nodes
kubectl top pods -n carddemo

# Get HPA status
kubectl get hpa -n carddemo

# Describe HPA for scaling events
kubectl describe hpa carddemo-backend-hpa -n carddemo

# Check persistent volumes and claims
kubectl get pv
kubectl get pvc -n carddemo

# View storage class configurations
kubectl get storageclass
```

#### Configuration Troubleshooting

```bash
# View ConfigMap contents
kubectl get configmap <configmap-name> -n carddemo -o yaml

# View Secret contents (base64 encoded)
kubectl get secret <secret-name> -n carddemo -o yaml

# Check RBAC permissions
kubectl auth can-i <verb> <resource> --as=system:serviceaccount:carddemo:carddemo-backend

# View role bindings
kubectl get rolebindings -n carddemo
kubectl describe rolebinding <binding-name> -n carddemo
```

### Emergency Recovery Procedures

#### 1. Complete System Restore

```bash
# Restore from known good configuration
kubectl apply -f backup-manifests/ -n carddemo

# Force restart all deployments
kubectl rollout restart deployment -n carddemo

# Wait for all pods to be ready
kubectl wait --for=condition=ready pod --all -n carddemo --timeout=600s
```

#### 2. Database Recovery

```bash
# Scale down all applications
kubectl scale deployment --all --replicas=0 -n carddemo

# Restore database from backup
kubectl exec -it postgresql-0 -n carddemo -- \
  pg_restore -U carddemo_user -d carddemo /backup/carddemo-backup.sql

# Scale applications back up
kubectl scale deployment --all --replicas=3 -n carddemo
```

#### 3. Rolling Back Deployments

```bash
# View deployment history
kubectl rollout history deployment/carddemo-backend -n carddemo

# Rollback to previous version
kubectl rollout undo deployment/carddemo-backend -n carddemo

# Rollback to specific revision
kubectl rollout undo deployment/carddemo-backend --to-revision=2 -n carddemo

# Check rollback status
kubectl rollout status deployment/carddemo-backend -n carddemo
```

## Maintenance Procedures

### Regular Maintenance Tasks

#### Daily Operations

```bash
# Check cluster health
kubectl get nodes
kubectl get pods --all-namespaces | grep -v Running

# Verify backup completion
kubectl get jobs -n carddemo | grep backup

# Review application logs for errors
kubectl logs -l app=carddemo-backend -n carddemo --since=24h | grep ERROR

# Check resource usage trends
kubectl top nodes
kubectl top pods -n carddemo
```

#### Weekly Maintenance

```bash
# Update container images (if using latest)
kubectl set image deployment/carddemo-backend backend-service=carddemo/backend-service:v2.1.1 -n carddemo

# Clean up completed jobs
kubectl delete job --field-selector=status.successful=1 -n carddemo

# Review and rotate secrets if needed
kubectl create secret generic new-secret --from-literal=key=newvalue -n carddemo
kubectl patch deployment carddemo-backend -p '{"spec":{"template":{"spec":{"containers":[{"name":"backend-service","envFrom":[{"secretRef":{"name":"new-secret"}}]}]}}}}' -n carddemo

# Check storage usage
kubectl exec -it postgresql-0 -n carddemo -- df -h /var/lib/postgresql/data
```

#### Monthly Maintenance

```bash
# Update Kubernetes cluster (if managed)
# Follow cloud provider specific procedures

# Review and update resource quotas
kubectl get resourcequota -n carddemo

# Audit RBAC permissions
kubectl get rolebindings -n carddemo -o yaml

# Review and update network policies
kubectl get networkpolicies -n carddemo

# Performance optimization review
kubectl describe hpa -n carddemo
```

### Backup Procedures

#### Database Backup

**Automated Backup CronJob**:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgresql-backup
  namespace: carddemo
spec:
  schedule: "0 1 * * *"  # Daily at 1 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: postgres-backup
            image: postgres:15.4
            command:
            - /bin/bash
            - -c
            - |
              pg_dump postgresql://carddemo_user:${POSTGRES_PASSWORD}@postgresql-service:5432/carddemo \
                | gzip > /backup/carddemo-$(date +%Y%m%d).sql.gz
              
              # Keep only last 7 days of backups
              find /backup -name "carddemo-*.sql.gz" -mtime +7 -delete
            env:
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgresql-secret
                  key: password
            volumeMounts:
            - name: backup-storage
              mountPath: /backup
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: backup-pvc
          restartPolicy: OnFailure
```

#### Configuration Backup

```bash
# Backup all Kubernetes manifests
kubectl get all,configmap,secret,pvc,pv,networkpolicy,rbac -n carddemo -o yaml > carddemo-backup-$(date +%Y%m%d).yaml

# Backup specific configurations
kubectl get configmap,secret -n carddemo -o yaml > carddemo-config-backup-$(date +%Y%m%d).yaml

# Store in version control or secure backup location
git add carddemo-backup-$(date +%Y%m%d).yaml
git commit -m "CardDemo backup $(date +%Y%m%d)"
```

### Update Procedures

#### Application Updates

```bash
# Rolling update with zero downtime
kubectl set image deployment/carddemo-backend \
  backend-service=carddemo/backend-service:v2.1.1 -n carddemo

# Monitor rollout progress
kubectl rollout status deployment/carddemo-backend -n carddemo

# Verify new version
kubectl get pods -n carddemo -l app=carddemo-backend
kubectl logs -l app=carddemo-backend -n carddemo | tail -20
```

#### Database Schema Updates

```bash
# Apply database migrations
kubectl create job --from=cronjob/database-migration migration-$(date +%Y%m%d) -n carddemo

# Monitor migration progress
kubectl logs job/migration-$(date +%Y%m%d) -n carddemo -f

# Verify schema changes
kubectl exec -it postgresql-0 -n carddemo -- \
  psql -U carddemo_user -d carddemo -c "\dt"
```

#### Configuration Updates

```bash
# Update ConfigMap
kubectl create configmap carddemo-backend-config \
  --from-file=application.yml=new-application.yml \
  --dry-run=client -o yaml | kubectl apply -f - -n carddemo

# Restart pods to pick up new configuration
kubectl rollout restart deployment/carddemo-backend -n carddemo

# Verify configuration change
kubectl exec -it <pod-name> -n carddemo -- \
  cat /etc/config/application.yml
```

## Environment-Specific Deployment

### Development Environment

**Configuration Characteristics**:
- Minimal resource allocation
- Single replica deployments
- Local storage volumes
- Relaxed security policies
- Debug logging enabled

**Deployment Command**:
```bash
kubectl apply -k overlays/development
```

**kustomization.yaml**:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: carddemo-dev

resources:
- ../../base

patchesStrategicMerge:
- replica-patch.yaml
- resource-patch.yaml
- config-patch.yaml

images:
- name: carddemo/backend-service
  newTag: latest

configMapGenerator:
- name: carddemo-backend-config
  files:
  - application-dev.yml=application.yml
  behavior: replace
```

### QA Environment

**Configuration Characteristics**:
- Production-like resource allocation
- Multiple replicas for testing
- Persistent storage
- Standard security policies
- INFO level logging

**Deployment Command**:
```bash
kubectl apply -k overlays/qa
```

**Key Configurations**:
- Database with production-like data volume
- Load testing capabilities
- Monitoring and alerting enabled
- Performance testing tools integrated

### Production Environment

**Configuration Characteristics**:
- Full resource allocation
- High availability setup
- Enterprise storage solutions
- Strict security policies
- Comprehensive monitoring

**Deployment Command**:
```bash
kubectl apply -k overlays/production
```

**Production Checklist**:

- [ ] Secrets updated with strong passwords
- [ ] TLS certificates configured
- [ ] Network policies applied
- [ ] Resource quotas set
- [ ] Monitoring alerts configured
- [ ] Backup procedures tested
- [ ] Disaster recovery plan validated
- [ ] Performance testing completed
- [ ] Security scanning passed
- [ ] Documentation updated

### Multi-Region Deployment

For disaster recovery and high availability across geographic regions:

**Primary Region**:
```bash
# Deploy to primary region
kubectl config use-context primary-region
kubectl apply -k overlays/production-primary
```

**Secondary Region**:
```bash
# Deploy to secondary region
kubectl config use-context secondary-region
kubectl apply -k overlays/production-secondary
```

**Cross-Region Considerations**:
- Database replication between regions
- Session data synchronization
- DNS failover configuration
- Cross-region network policies
- Monitoring across regions

---

## Support and Troubleshooting

For additional support:

1. **Check Application Logs**: Use `kubectl logs` to review application output
2. **Review Health Endpoints**: Access `/actuator/health` for application status
3. **Monitor Metrics**: Use Prometheus/Grafana dashboards for performance insights
4. **Consult Documentation**: Refer to Spring Boot and Kubernetes documentation
5. **Community Support**: Check CardDemo GitHub repository for issues and discussions

Remember to follow your organization's specific operational procedures and security policies when deploying and managing CardDemo in production environments.