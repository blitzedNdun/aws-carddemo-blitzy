# Test Certificates Directory

This directory contains SSL/TLS certificates and related cryptographic assets for testing the CardDemo application's secure communication capabilities. The certificate infrastructure supports comprehensive SSL/TLS testing scenarios including Spring Boot HTTPS endpoints, mutual TLS authentication, PostgreSQL SSL connections, and Redis TLS session management.

## Table of Contents

1. [Certificate Structure Overview](#certificate-structure-overview)
2. [Certificate Authority (CA) Hierarchy](#certificate-authority-ca-hierarchy)
3. [Certificate Files Reference](#certificate-files-reference)
4. [Spring Boot SSL/TLS Configuration](#spring-boot-ssltls-configuration)
5. [Database SSL Setup](#database-ssl-setup)
6. [Redis TLS Configuration](#redis-tls-configuration)
7. [Certificate Generation Procedures](#certificate-generation-procedures)
8. [SSL/TLS Testing Instructions](#ssltls-testing-instructions)
9. [Security Considerations](#security-considerations)
10. [Troubleshooting](#troubleshooting)
11. [Certificate Renewal Procedures](#certificate-renewal-procedures)

## Certificate Structure Overview

The test certificate infrastructure implements a complete Public Key Infrastructure (PKI) supporting the modernized CardDemo application's cloud-native security architecture. This PKI replaces the legacy mainframe security model with enterprise-grade TLS/SSL encryption aligned with the Spring Security framework and containerized deployment patterns.

### Architecture Alignment

This certificate structure directly supports the technical requirements outlined in the system architecture:

- **Spring Security Integration**: Certificates enable HTTPS endpoints for Spring Boot REST controllers
- **Cloud-Native Security**: PKI infrastructure supports Kubernetes ingress TLS termination
- **Database Security**: PostgreSQL SSL certificates ensure encrypted database connections
- **Session Management**: Redis TLS certificates secure Spring Session data transmission
- **Service Mesh**: Client certificates enable mutual TLS for service-to-service communication

### Certificate Types and Purposes

```
📁 certificates/
├── 🔐 Certificate Authority (Root Trust)
│   ├── ca.crt              # Root CA certificate (4096-bit RSA, 10-year validity)
│   └── ca.key              # Root CA private key (encrypted with passphrase)
│
├── 🌐 Server Certificates (TLS Server Authentication)
│   ├── server.crt          # Spring Boot HTTPS server certificate
│   ├── server.key          # Spring Boot server private key
│   ├── postgresql-server.crt  # PostgreSQL SSL server certificate
│   ├── postgresql-server.key  # PostgreSQL SSL private key
│   ├── redis-server.crt    # Redis TLS server certificate
│   └── redis-server.key    # Redis TLS private key
│
├── 👤 Client Certificates (Mutual TLS Authentication)
│   ├── client.crt          # Service-to-service client certificate
│   └── client.key          # Service-to-service client private key
│
├── 📦 Keystores and Truststores (Java Integration)
│   ├── keystore.p12        # PKCS12 keystore (Spring Boot SSL configuration)
│   └── truststore.p12      # PKCS12 truststore (Certificate validation)
│
├── ⚙️ Configuration and Automation
│   ├── ssl-config.properties     # Spring Boot SSL property templates
│   ├── cert-config.cnf          # OpenSSL certificate generation config
│   ├── generate-test-certs.sh   # Automated certificate generation script
│   └── CertificateValidationTest.java  # Certificate validation utilities
│
└── 📚 Documentation
    └── README.md           # This comprehensive documentation
```

## Certificate Authority (CA) Hierarchy

### Root Certificate Authority

The test environment implements a self-signed Certificate Authority to establish a complete trust chain for all SSL/TLS communications:

```
Root CA (ca.crt)
├── Subject: CN=Test CA, O=CardDemo Test Environment
├── Validity: 10 years (long-term for development stability)
├── Key Size: RSA 4096-bit (enhanced security)
├── Extensions: CA:TRUE, Key Cert Sign, CRL Sign
└── Purpose: Signs all server and client certificates
```

### Certificate Chain Validation

All certificates in this PKI are signed by the root CA, creating a verifiable trust chain:

```
Trust Chain Flow:
Root CA → Server/Client Certificates → Application SSL/TLS
   ↓
Validates all secure connections in test environment
```

### CA Security Features

- **Enhanced Key Strength**: 4096-bit RSA keys provide future-proof security
- **Passphrase Protection**: CA private key encrypted for additional security
- **Certificate Extensions**: Proper CA extensions for compliant certificate signing
- **Long Validity**: 10-year validity reduces maintenance overhead in development

## Certificate Files Reference

### Server Certificates

#### Spring Boot Server Certificate (`server.crt` / `server.key`)

**Purpose**: Enables HTTPS endpoints for Spring Boot REST controllers and Spring Cloud Gateway

**Technical Specifications**:
- **Subject**: `CN=localhost`
- **Subject Alternative Names**: `localhost`, `127.0.0.1`, `carddemo.local`, `*.carddemo.local`
- **Key Algorithm**: RSA 2048-bit
- **Validity Period**: 365 days
- **Certificate Extensions**: 
  - Server Authentication (`serverAuth`)
  - Digital Signature
  - Key Encipherment

**Integration Points**:
```yaml
# Spring Boot application.yml
server:
  ssl:
    key-store: classpath:certificates/keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    enabled: true
```

**Usage Scenarios**:
- HTTPS endpoint testing for REST controllers
- Spring Cloud Gateway TLS termination
- React SPA secure API communication
- Integration test HTTPS validation

#### PostgreSQL Server Certificate (`postgresql-server.crt` / `postgresql-server.key`)

**Purpose**: Secures JDBC connections between Spring Boot application and PostgreSQL database

**Technical Specifications**:
- **Subject**: `CN=postgres`
- **Subject Alternative Names**: `postgres`, `localhost`, `postgresql.carddemo.local`
- **Key Algorithm**: RSA 2048-bit
- **Validity Period**: 365 days
- **Certificate Extensions**: Server Authentication

**Integration Points**:
```yaml
# Spring Boot Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/carddemo?ssl=true&sslmode=require&sslcert=certificates/postgresql-server.crt
    username: carddemo_user
    password: ${DB_PASSWORD}
```

**Usage Scenarios**:
- Encrypted database connections in integration tests
- Testcontainers PostgreSQL SSL validation
- JPA repository secure data access testing
- Database connection security verification

#### Redis Server Certificate (`redis-server.crt` / `redis-server.key`)

**Purpose**: Secures Spring Session data transmission to Redis cluster

**Technical Specifications**:
- **Subject**: `CN=redis`
- **Subject Alternative Names**: `redis`, `localhost`, `redis.carddemo.local`
- **Key Algorithm**: RSA 2048-bit
- **Validity Period**: 365 days
- **Certificate Extensions**: Server Authentication

**Integration Points**:
```yaml
# Spring Session Redis Configuration
spring:
  redis:
    ssl: true
    host: localhost
    port: 6380
    ssl-trust-store: classpath:certificates/truststore.p12
    ssl-trust-store-password: changeit
```

**Usage Scenarios**:
- Secure session state management testing
- Redis cluster TLS connection validation
- Spring Session security verification
- Distributed session testing with encryption

### Client Certificates

#### Service Client Certificate (`client.crt` / `client.key`)

**Purpose**: Enables mutual TLS authentication for service-to-service communication

**Technical Specifications**:
- **Subject**: `CN=test-client`
- **Key Algorithm**: RSA 2048-bit
- **Validity Period**: 365 days
- **Certificate Extensions**: 
  - Client Authentication (`clientAuth`)
  - Digital Signature

**Integration Points**:
```java
// Spring RestTemplate with Mutual TLS
@Bean
public RestTemplate mutualTlsRestTemplate() {
    // Configure client certificate authentication
    return new RestTemplate();
}
```

**Usage Scenarios**:
- Mutual TLS testing between microservices
- Service mesh security validation
- API-to-API secure communication testing
- Client certificate authentication verification

### Java Keystores and Truststores

#### Keystore (`keystore.p12`)

**Purpose**: PKCS12 keystore containing server certificate and private key for Spring Boot SSL configuration

**Contents**:
- Server certificate chain (ca.crt → server.crt)
- Server private key
- Password protected with default "changeit"

**Spring Boot Integration**:
```properties
server.ssl.key-store=classpath:certificates/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=server
```

#### Truststore (`truststore.p12`)

**Purpose**: PKCS12 truststore containing CA certificate and trusted client certificates for validation

**Contents**:
- Root CA certificate (ca.crt)
- Client certificates for mutual TLS validation
- Password protected with default "changeit"

**Spring Boot Integration**:
```properties
server.ssl.trust-store=classpath:certificates/truststore.p12
server.ssl.trust-store-password=changeit
server.ssl.trust-store-type=PKCS12
server.ssl.client-auth=need  # For mutual TLS
```

## Spring Boot SSL/TLS Configuration

### Basic HTTPS Configuration

Enable HTTPS for Spring Boot applications using the test certificates:

```yaml
# application-test.yml
server:
  ssl:
    enabled: true
    key-store: classpath:certificates/keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: server
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
    ciphers: 
      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256
      - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
      - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

### Mutual TLS Configuration

Configure mutual TLS for enhanced security testing:

```yaml
# application-integration.yml
server:
  ssl:
    enabled: true
    key-store: classpath:certificates/keystore.p12
    key-store-password: changeit
    trust-store: classpath:certificates/truststore.p12
    trust-store-password: changeit
    client-auth: need  # Require client certificates
    protocol: TLS
```

### Spring Security Integration

Integrate SSL configuration with Spring Security:

```java
@Configuration
@EnableWebSecurity
public class TestSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .requiresChannel(channel -> 
                channel.requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                       .requiresSecure())
            .and()
            // Additional security configuration
            ;
        return http.build();
    }
}
```

## Database SSL Setup

### PostgreSQL SSL Configuration

Configure PostgreSQL to use SSL certificates for secure database connections:

#### Server Configuration (`postgresql.conf`)

```conf
# SSL Configuration
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
ssl_ca_file = 'ca.crt'
ssl_ciphers = 'HIGH:MEDIUM:+3DES:!aNULL'
ssl_prefer_server_ciphers = on
ssl_protocols = 'TLSv1.2,TLSv1.3'
```

#### Client Configuration (`pg_hba.conf`)

```conf
# SSL Connections
hostssl carddemo carddemo_user localhost cert
hostssl carddemo carddemo_admin localhost cert
```

### Spring Boot PostgreSQL SSL Configuration

Configure Spring Data JPA for SSL database connections:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/carddemo?ssl=true&sslmode=require&sslcert=postgresql-server.crt&sslkey=postgresql-server.key&sslrootcert=ca.crt
    username: carddemo_user
    password: ${DB_PASSWORD}
    hikari:
      connection-test-query: SELECT 1
      maximum-pool-size: 10
      minimum-idle: 5
```

### Testcontainers SSL Setup

Configure Testcontainers for SSL database testing:

```java
@Testcontainers
class DatabaseSSLIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("carddemo")
            .withUsername("test_user")
            .withPassword("test_password")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("certificates/postgresql-server.crt"),
                "/var/lib/postgresql/server.crt")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("certificates/postgresql-server.key"),
                "/var/lib/postgresql/server.key")
            .withCommand("postgres", "-c", "ssl=on", "-c", "ssl_cert_file=/var/lib/postgresql/server.crt");
}
```

## Redis TLS Configuration

### Redis Server TLS Setup

Configure Redis server for TLS connections:

#### Redis Configuration (`redis.conf`)

```conf
# TLS Configuration
port 0
tls-port 6380
tls-cert-file /etc/redis/redis-server.crt
tls-key-file /etc/redis/redis-server.key
tls-ca-cert-file /etc/redis/ca.crt
tls-protocols "TLSv1.2 TLSv1.3"
tls-ciphers "ECDHE+AESGCM:ECDHE+CHACHA20:DHE+AESGCM:DHE+CHACHA20:!aNULL:!MD5:!DSS"
```

### Spring Session Redis TLS Configuration

Configure Spring Session for TLS Redis connections:

```yaml
spring:
  session:
    store-type: redis
  redis:
    ssl: true
    host: localhost
    port: 6380
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### Redis Client Configuration

Configure Redis client with SSL in Spring Boot:

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6380);
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .and()
                .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
}
```

## Certificate Generation Procedures

### Automated Certificate Generation

Use the provided script to generate all test certificates:

```bash
# Navigate to certificates directory
cd backend/src/test/resources/certificates/

# Make script executable
chmod +x generate-test-certs.sh

# Generate all certificates
./generate-test-certs.sh

# Verify generation
ls -la *.crt *.key *.p12
```

### Manual Certificate Generation

For custom certificate requirements, use OpenSSL directly:

#### 1. Generate Root CA

```bash
# Generate CA private key
openssl genrsa -aes256 -out ca.key 4096

# Generate CA certificate
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
    -config cert-config.cnf -extensions v3_ca
```

#### 2. Generate Server Certificate

```bash
# Generate server private key
openssl genrsa -out server.key 2048

# Generate certificate signing request
openssl req -new -key server.key -out server.csr \
    -config cert-config.cnf

# Sign server certificate with CA
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key \
    -CAcreateserial -out server.crt -days 365 \
    -extensions v3_server -extfile cert-config.cnf
```

#### 3. Generate Client Certificate

```bash
# Generate client private key
openssl genrsa -out client.key 2048

# Generate certificate signing request
openssl req -new -key client.key -out client.csr \
    -config cert-config.cnf

# Sign client certificate with CA
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key \
    -CAcreateserial -out client.crt -days 365 \
    -extensions v3_client -extfile cert-config.cnf
```

#### 4. Create PKCS12 Keystores

```bash
# Create keystore with server certificate
openssl pkcs12 -export -out keystore.p12 \
    -inkey server.key -in server.crt -certfile ca.crt \
    -password pass:changeit

# Create truststore with CA certificate
openssl pkcs12 -export -out truststore.p12 \
    -nokeys -in ca.crt -certfile client.crt \
    -password pass:changeit
```

### Certificate Validation

Verify certificate generation and properties:

```bash
# Check certificate details
openssl x509 -in server.crt -text -noout

# Verify certificate chain
openssl verify -CAfile ca.crt server.crt

# Check keystore contents
keytool -list -v -keystore keystore.p12 -storepass changeit

# Test certificate expiry
openssl x509 -in server.crt -checkend 86400
```

## SSL/TLS Testing Instructions

### Unit Testing SSL Configuration

Test SSL configuration without full application context:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SSLConfigurationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    void testHTTPSEndpoint() {
        // Configure trust store for test
        System.setProperty("javax.net.ssl.trustStore", 
            "src/test/resources/certificates/truststore.p12");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        
        String url = "https://localhost:" + port + "/api/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### Integration Testing with Mutual TLS

Test mutual TLS authentication between services:

```java
@SpringBootTest
@Testcontainers
class MutualTLSIntegrationTest {
    
    @Test
    void testMutualTLSAuthentication() {
        // Configure client certificate
        SSLContext sslContext = createSSLContextWithClientCert();
        
        // Create REST template with client certificate
        RestTemplate restTemplate = createMutualTlsRestTemplate(sslContext);
        
        // Test service-to-service communication
        String response = restTemplate.getForObject(
            "https://localhost:8443/api/secure-endpoint", String.class);
        
        assertThat(response).isNotNull();
    }
    
    private SSLContext createSSLContextWithClientCert() throws Exception {
        // Load client certificate and key
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getResourceAsStream("/certificates/keystore.p12")) {
            keyStore.load(is, "changeit".toCharArray());
        }
        
        // Create SSL context with client certificate
        SSLContextBuilder builder = SSLContexts.custom()
                .loadKeyMaterial(keyStore, "changeit".toCharArray())
                .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy());
        
        return builder.build();
    }
}
```

### Database SSL Connection Testing

Verify PostgreSQL SSL connections:

```java
@DataJpaTest
@Testcontainers
class DatabaseSSLConnectionTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("carddemo")
            .withUsername("test_user")
            .withPassword("test_password")
            // Configure SSL certificates
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("certificates/postgresql-server.crt"),
                "/var/lib/postgresql/server.crt");
    
    @Test
    void testSSLDatabaseConnection() {
        // Verify SSL connection
        String sslQuery = "SELECT ssl_is_used() as ssl_enabled;";
        
        // Execute query and verify SSL is enabled
        // Implementation depends on your testing framework
    }
}
```

### Redis TLS Connection Testing

Verify Redis TLS connections:

```java
@SpringBootTest
@Testcontainers
class RedisSSLConnectionTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6380)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("certificates/redis-server.crt"),
                "/etc/redis/server.crt");
    
    @Test
    void testRedisTLSConnection() {
        // Configure TLS connection
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost());
        config.setPort(redis.getMappedPort(6380));
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .and()
                .build();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.afterPropertiesSet();
        
        // Test connection
        RedisConnection connection = factory.getConnection();
        assertThat(connection.ping()).isEqualTo("PONG");
    }
}
```

## Security Considerations

### Test Environment Security

**Important**: These certificates are intended **ONLY** for testing and development environments.

#### Security Boundaries

- **Scope**: Limited to test and development environments
- **Trust**: Self-signed certificates not suitable for production
- **Exposure**: Test certificates should never be deployed to production systems
- **Access**: Certificates stored in source control are public by nature

#### Certificate Security Properties

- **Encryption Strength**: RSA 2048-bit minimum (4096-bit for CA)
- **Validity Period**: Limited to 365 days for regular certificates
- **Password Protection**: Default passwords ("changeit") for ease of testing
- **Algorithm Support**: Modern TLS protocols (TLSv1.2, TLSv1.3)

### Production Migration Guidelines

#### Certificate Authority

For production deployment:
- Use enterprise Certificate Authority (DigiCert, Let's Encrypt, internal CA)
- Implement proper certificate lifecycle management
- Use Hardware Security Modules (HSM) for key protection
- Establish certificate rotation procedures

#### Key Management

Production key management requirements:
- Store private keys in secure key management systems
- Use strong, unique passwords for all keystores
- Implement key rotation schedules
- Audit certificate access and usage

#### Monitoring and Alerting

Implement certificate monitoring:
- Certificate expiry alerts (30, 7, 1 day warnings)
- Certificate validation monitoring
- SSL/TLS handshake failure alerts
- Certificate chain verification monitoring

### Compliance Considerations

#### Regulatory Alignment

Certificate infrastructure supports:
- **PCI DSS**: Encryption of cardholder data in transit
- **SOX**: Audit trails for certificate access and usage
- **GDPR**: Protection of personal data in transmission
- **Banking Regulations**: Secure communication requirements

#### Audit Requirements

Maintain audit trails for:
- Certificate generation and signing
- Certificate deployment and configuration
- Certificate revocation and renewal
- SSL/TLS connection attempts and failures

## Troubleshooting

### Common SSL/TLS Issues

#### Certificate Validation Errors

**Problem**: `PKIX path building failed` or certificate validation errors

**Solutions**:
```bash
# Verify certificate chain
openssl verify -CAfile ca.crt server.crt

# Check certificate dates
openssl x509 -in server.crt -noout -dates

# Validate certificate format
openssl x509 -in server.crt -text -noout
```

#### Keystore/Truststore Issues

**Problem**: `java.security.KeyStoreException` or keystore loading failures

**Solutions**:
```bash
# List keystore contents
keytool -list -v -keystore keystore.p12 -storepass changeit

# Verify keystore integrity
keytool -list -keystore keystore.p12 -storepass changeit

# Test keystore password
openssl pkcs12 -in keystore.p12 -noout
```

#### SSL Handshake Failures

**Problem**: SSL handshake timeouts or protocol errors

**Solutions**:
```bash
# Test SSL connection
openssl s_client -connect localhost:8443 -servername localhost

# Check supported protocols
openssl s_client -connect localhost:8443 -tls1_2

# Verify cipher suites
openssl s_client -connect localhost:8443 -cipher 'ECDHE+AESGCM'
```

#### Spring Boot SSL Configuration Issues

**Problem**: Application fails to start with SSL enabled

**Solutions**:
```yaml
# Enable SSL debug logging
logging:
  level:
    javax.net.ssl: DEBUG
    org.springframework.boot.web.embedded.tomcat.TomcatWebServer: DEBUG

# Verify SSL configuration
server:
  ssl:
    enabled: true
    key-store: classpath:certificates/keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
```

### Database SSL Troubleshooting

#### PostgreSQL SSL Connection Issues

**Problem**: Database SSL connection failures

**Diagnostics**:
```sql
-- Check SSL status
SELECT ssl_is_used();

-- View SSL configuration
SHOW ssl;

-- Check SSL cipher
SELECT ssl_cipher FROM pg_stat_ssl WHERE pid = pg_backend_pid();
```

**Solutions**:
```bash
# Test PostgreSQL SSL connection
psql "host=localhost port=5432 dbname=carddemo user=test_user sslmode=require"

# Verify PostgreSQL SSL configuration
pg_isready -h localhost -p 5432
```

### Redis TLS Troubleshooting

#### Redis TLS Connection Issues

**Problem**: Redis TLS connection failures

**Diagnostics**:
```bash
# Test Redis TLS connection
redis-cli --tls --cert redis-server.crt --key redis-server.key --cacert ca.crt -h localhost -p 6380 ping
```

**Solutions**:
```bash
# Check Redis TLS configuration
redis-cli --tls -h localhost -p 6380 config get tls-*

# Verify TLS handshake
openssl s_client -connect localhost:6380 -servername redis
```

### Certificate Debugging Tools

#### OpenSSL Debugging Commands

```bash
# Detailed certificate analysis
openssl x509 -in server.crt -text -noout

# Certificate chain verification
openssl verify -verbose -CAfile ca.crt server.crt

# Certificate fingerprint
openssl x509 -in server.crt -noout -fingerprint -sha256

# Key pair validation
openssl rsa -in server.key -check

# Certificate and key matching
openssl x509 -noout -modulus -in server.crt | openssl md5
openssl rsa -noout -modulus -in server.key | openssl md5
```

#### Java Keytool Commands

```bash
# Import certificate to truststore
keytool -import -file ca.crt -alias testca -keystore truststore.p12 -storepass changeit

# List keystore aliases
keytool -list -keystore keystore.p12 -storepass changeit

# Change keystore password
keytool -storepasswd -keystore keystore.p12

# Export certificate from keystore
keytool -export -alias server -keystore keystore.p12 -file exported-server.crt -storepass changeit
```

## Certificate Renewal Procedures

### Automated Renewal Process

The test environment supports automated certificate renewal for continuous testing:

#### Renewal Script Enhancement

Extend the generation script for renewal scenarios:

```bash
#!/bin/bash
# Enhanced certificate renewal script

CERT_DIR="$(dirname "$0")"
DAYS_BEFORE_EXPIRY=30

# Check certificate expiry
check_expiry() {
    local cert_file="$1"
    if [ -f "$cert_file" ]; then
        if ! openssl x509 -checkend $((DAYS_BEFORE_EXPIRY * 86400)) -noout -in "$cert_file"; then
            echo "Certificate $cert_file expires within $DAYS_BEFORE_EXPIRY days"
            return 1
        fi
    fi
    return 0
}

# Renew certificates if needed
renew_if_needed() {
    for cert in server.crt client.crt postgresql-server.crt redis-server.crt; do
        if ! check_expiry "$cert"; then
            echo "Renewing $cert..."
            # Call certificate generation functions
            generate_certificates
            break
        fi
    done
}

# Schedule renewal check
renew_if_needed
```

#### CI/CD Integration

Integrate certificate renewal with continuous integration:

```yaml
# .github/workflows/certificate-renewal.yml
name: Certificate Renewal

on:
  schedule:
    - cron: '0 2 * * 0'  # Weekly check on Sunday 2 AM
  workflow_dispatch:

jobs:
  renew-certificates:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Check Certificate Expiry
        run: |
          cd backend/src/test/resources/certificates
          ./generate-test-certs.sh --check-expiry
      
      - name: Renew Certificates if Needed
        run: |
          cd backend/src/test/resources/certificates
          ./generate-test-certs.sh --renew-if-needed
      
      - name: Commit Updated Certificates
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add backend/src/test/resources/certificates/
          git commit -m "Auto-renew test certificates" || exit 0
          git push
```

### Manual Renewal Process

For manual certificate renewal:

#### 1. Backup Existing Certificates

```bash
# Create backup directory
mkdir -p certificates-backup-$(date +%Y%m%d)

# Backup current certificates
cp *.crt *.key *.p12 certificates-backup-$(date +%Y%m%d)/
```

#### 2. Generate New Certificates

```bash
# Remove old certificates
rm -f *.crt *.key *.p12 *.csr

# Generate fresh certificates
./generate-test-certs.sh
```

#### 3. Update Application Configuration

Update any hardcoded certificate references:

```bash
# Search for certificate references
grep -r "server.crt\|keystore.p12" src/

# Update configuration files as needed
```

#### 4. Validate New Certificates

```bash
# Run certificate validation tests
mvn test -Dtest=CertificateValidationTest

# Verify SSL endpoints
curl -k https://localhost:8443/actuator/health
```

### Expiry Monitoring

Implement certificate expiry monitoring:

#### Monitoring Script

```bash
#!/bin/bash
# Certificate expiry monitoring

CERT_DIR="backend/src/test/resources/certificates"
ALERT_DAYS=30

for cert in "$CERT_DIR"/*.crt; do
    if [ -f "$cert" ]; then
        expiry_date=$(openssl x509 -enddate -noout -in "$cert" | cut -d= -f2)
        expiry_epoch=$(date -d "$expiry_date" +%s)
        current_epoch=$(date +%s)
        days_remaining=$(( (expiry_epoch - current_epoch) / 86400 ))
        
        if [ $days_remaining -lt $ALERT_DAYS ]; then
            echo "WARNING: Certificate $cert expires in $days_remaining days"
        else
            echo "OK: Certificate $cert expires in $days_remaining days"
        fi
    fi
done
```

#### Spring Boot Health Check

Add certificate expiry to application health checks:

```java
@Component
public class CertificateHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check certificate expiry
            KeyStore keyStore = loadKeyStore();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate("server");
            
            Date expiry = cert.getNotAfter();
            long daysUntilExpiry = ChronoUnit.DAYS.between(
                LocalDate.now(), 
                expiry.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            );
            
            if (daysUntilExpiry < 30) {
                return Health.down()
                    .withDetail("certificate", "Expires in " + daysUntilExpiry + " days")
                    .build();
            }
            
            return Health.up()
                .withDetail("certificate", "Valid for " + daysUntilExpiry + " days")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## Additional Resources

### External Documentation

- [Spring Boot SSL Configuration Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server.server.ssl)
- [PostgreSQL SSL Documentation](https://www.postgresql.org/docs/current/ssl-tcp.html)
- [Redis TLS Configuration Guide](https://redis.io/docs/manual/security/encryption/)
- [OpenSSL Command Reference](https://www.openssl.org/docs/man3.0/man1/)

### Related Files

- `../application-test.yml` - Test environment Spring Boot configuration
- `../application-integration.yml` - Integration test configuration  
- `../../main/resources/application.yml` - Main application configuration
- `../../../main/java/com/carddemo/config/SecurityConfig.java` - Spring Security configuration

### Support

For certificate-related issues in the CardDemo project:

1. Check the troubleshooting section above
2. Verify certificate generation with validation tests
3. Review Spring Boot SSL logs with DEBUG level enabled
4. Consult the technical specification security architecture section 6.4

---

**Note**: This certificate infrastructure is designed specifically for testing the CardDemo application's SSL/TLS capabilities. All certificates are self-signed and intended for development use only. For production deployment, implement proper certificate management through enterprise Certificate Authorities and cloud provider security services.