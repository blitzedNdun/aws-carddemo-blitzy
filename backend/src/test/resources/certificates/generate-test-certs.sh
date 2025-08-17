#!/bin/bash

# =============================================================================
# CardDemo Test Certificate Generation Script
# =============================================================================
# 
# Generates complete PKI certificate infrastructure for CardDemo test environment
# including CA certificates, server certificates, client certificates, and PKCS12
# keystores/truststores for Spring Boot HTTPS, mutual TLS, PostgreSQL SSL, and
# Redis TLS connections.
#
# This script implements the certificate generation requirements from the
# Security Architecture (Section 6.4) supporting:
# - HTTPS/TLS for secure communication between React SPA, Spring Cloud Gateway, and Spring Boot services
# - Mutual TLS for service-to-service communication
# - PostgreSQL SSL/TLS encrypted client connections  
# - Redis TLS for session store connections
# - Certificate management through cloud provider or Kubernetes Secrets
#
# Generated Artifacts:
# - ca.crt, ca.key           : Certificate Authority (RSA 4096-bit, 10-year validity)
# - server.crt, server.key   : Server certificate (RSA 2048-bit, 365-day validity)
# - client.crt, client.key   : Client certificate (RSA 2048-bit, 365-day validity)
# - postgresql-server.crt/key: PostgreSQL SSL certificate (RSA 2048-bit)
# - redis-server.crt/key     : Redis TLS certificate (RSA 2048-bit)
# - keystore.p12             : PKCS12 keystore for Spring Boot SSL configuration
# - truststore.p12           : PKCS12 truststore for mutual TLS authentication
#
# Usage: ./generate-test-certs.sh
# Dependencies: openssl 3.0+, bash 5.0+
# =============================================================================

set -euo pipefail

# =============================================================================
# CONFIGURATION VARIABLES
# =============================================================================

# Certificate validity periods
CA_VALIDITY_DAYS=3650      # 10 years for CA certificate
CERT_VALIDITY_DAYS=365     # 1 year for server/client certificates

# Key strengths
CA_KEY_SIZE=4096           # Enhanced security for CA key
CERT_KEY_SIZE=2048         # Standard strength for server/client keys

# Certificate subjects
CA_SUBJECT="/C=US/ST=Test/L=TestCity/O=CardDemo/OU=Test CA/CN=CardDemo Test CA"
SERVER_SUBJECT="/C=US/ST=Test/L=TestCity/O=CardDemo/OU=Backend Services/CN=localhost"
CLIENT_SUBJECT="/C=US/ST=Test/L=TestCity/O=CardDemo/OU=Test Client/CN=test-client"
POSTGRESQL_SUBJECT="/C=US/ST=Test/L=TestCity/O=CardDemo/OU=Database/CN=postgres"
REDIS_SUBJECT="/C=US/ST=Test/L=TestCity/O=CardDemo/OU=Session Store/CN=redis"

# PKCS12 keystore passwords (test environment)
KEYSTORE_PASSWORD="changeit"
TRUSTSTORE_PASSWORD="changeit"

# Output directory
CERT_DIR="$(dirname "$0")"

# =============================================================================
# UTILITY FUNCTIONS
# =============================================================================

# Function to print status messages
log_info() {
    echo "[INFO] $1"
}

# Function to print error messages
log_error() {
    echo "[ERROR] $1" >&2
}

# Function to check if OpenSSL is available
check_dependencies() {
    if ! command -v openssl &> /dev/null; then
        log_error "OpenSSL is not installed or not in PATH"
        exit 1
    fi
    
    # Check OpenSSL version (requires 3.0+)
    local openssl_version
    openssl_version=$(openssl version | awk '{print $2}' | cut -d. -f1)
    if [[ $openssl_version -lt 3 ]]; then
        log_error "OpenSSL version 3.0+ is required (found version: $(openssl version))"
        exit 1
    fi
    
    log_info "Dependencies verified - OpenSSL $(openssl version | awk '{print $2}')"
}

# Function to create certificate directory structure
setup_directories() {
    mkdir -p "$CERT_DIR"
    log_info "Certificate directory prepared: $CERT_DIR"
}

# Function to clean existing certificates (for regeneration)
clean_existing_certs() {
    if [[ -d "$CERT_DIR" ]]; then
        rm -f "$CERT_DIR"/*.crt "$CERT_DIR"/*.key "$CERT_DIR"/*.p12 "$CERT_DIR"/*.csr
        log_info "Cleaned existing certificates"
    fi
}

# =============================================================================
# CERTIFICATE AUTHORITY GENERATION
# =============================================================================

generate_ca_certificate() {
    log_info "Generating Certificate Authority..."
    
    # Generate CA private key (RSA 4096-bit for enhanced security)
    openssl genrsa -out "$CERT_DIR/ca.key" $CA_KEY_SIZE
    chmod 600 "$CERT_DIR/ca.key"
    
    # Generate CA certificate (self-signed)
    openssl req -new -x509 -key "$CERT_DIR/ca.key" \
        -out "$CERT_DIR/ca.crt" \
        -days $CA_VALIDITY_DAYS \
        -subj "$CA_SUBJECT" \
        -extensions v3_ca \
        -config <(cat <<EOF
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_ca

[req_distinguished_name]

[v3_ca]
basicConstraints = critical,CA:TRUE
keyUsage = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer:always
EOF
)
    
    chmod 644 "$CERT_DIR/ca.crt"
    log_info "CA certificate generated successfully"
}

# =============================================================================
# SERVER CERTIFICATE GENERATION
# =============================================================================

generate_server_certificate() {
    log_info "Generating server certificate for HTTPS endpoints..."
    
    # Generate server private key
    openssl genrsa -out "$CERT_DIR/server.key" $CERT_KEY_SIZE
    chmod 600 "$CERT_DIR/server.key"
    
    # Generate server certificate signing request
    openssl req -new -key "$CERT_DIR/server.key" \
        -out "$CERT_DIR/server.csr" \
        -subj "$SERVER_SUBJECT"
    
    # Generate server certificate signed by CA
    openssl x509 -req -in "$CERT_DIR/server.csr" \
        -CA "$CERT_DIR/ca.crt" \
        -CAkey "$CERT_DIR/ca.key" \
        -CAcreateserial \
        -out "$CERT_DIR/server.crt" \
        -days $CERT_VALIDITY_DAYS \
        -extensions v3_server \
        -extfile <(cat <<EOF
[v3_server]
basicConstraints = CA:FALSE
keyUsage = critical,digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer

[alt_names]
DNS.1 = localhost
DNS.2 = carddemo-backend
DNS.3 = carddemo-gateway
DNS.4 = *.carddemo.local
IP.1 = 127.0.0.1
IP.2 = ::1
EOF
)
    
    # Clean up CSR
    rm -f "$CERT_DIR/server.csr"
    chmod 644 "$CERT_DIR/server.crt"
    log_info "Server certificate generated successfully"
}

# =============================================================================
# CLIENT CERTIFICATE GENERATION
# =============================================================================

generate_client_certificate() {
    log_info "Generating client certificate for mutual TLS..."
    
    # Generate client private key
    openssl genrsa -out "$CERT_DIR/client.key" $CERT_KEY_SIZE
    chmod 600 "$CERT_DIR/client.key"
    
    # Generate client certificate signing request
    openssl req -new -key "$CERT_DIR/client.key" \
        -out "$CERT_DIR/client.csr" \
        -subj "$CLIENT_SUBJECT"
    
    # Generate client certificate signed by CA
    openssl x509 -req -in "$CERT_DIR/client.csr" \
        -CA "$CERT_DIR/ca.crt" \
        -CAkey "$CERT_DIR/ca.key" \
        -CAcreateserial \
        -out "$CERT_DIR/client.crt" \
        -days $CERT_VALIDITY_DAYS \
        -extensions v3_client \
        -extfile <(cat <<EOF
[v3_client]
basicConstraints = CA:FALSE
keyUsage = critical,digitalSignature,keyEncipherment
extendedKeyUsage = clientAuth
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
EOF
)
    
    # Clean up CSR
    rm -f "$CERT_DIR/client.csr"
    chmod 644 "$CERT_DIR/client.crt"
    log_info "Client certificate generated successfully"
}

# =============================================================================
# POSTGRESQL SSL CERTIFICATE GENERATION
# =============================================================================

generate_postgresql_certificate() {
    log_info "Generating PostgreSQL SSL certificate..."
    
    # Generate PostgreSQL private key
    openssl genrsa -out "$CERT_DIR/postgresql-server.key" $CERT_KEY_SIZE
    chmod 600 "$CERT_DIR/postgresql-server.key"
    
    # Generate PostgreSQL certificate signing request
    openssl req -new -key "$CERT_DIR/postgresql-server.key" \
        -out "$CERT_DIR/postgresql-server.csr" \
        -subj "$POSTGRESQL_SUBJECT"
    
    # Generate PostgreSQL certificate signed by CA
    openssl x509 -req -in "$CERT_DIR/postgresql-server.csr" \
        -CA "$CERT_DIR/ca.crt" \
        -CAkey "$CERT_DIR/ca.key" \
        -CAcreateserial \
        -out "$CERT_DIR/postgresql-server.crt" \
        -days $CERT_VALIDITY_DAYS \
        -extensions v3_postgresql \
        -extfile <(cat <<EOF
[v3_postgresql]
basicConstraints = CA:FALSE
keyUsage = critical,digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names_pg
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer

[alt_names_pg]
DNS.1 = postgres
DNS.2 = postgresql
DNS.3 = carddemo-postgres
DNS.4 = localhost
IP.1 = 127.0.0.1
EOF
)
    
    # Clean up CSR
    rm -f "$CERT_DIR/postgresql-server.csr"
    chmod 644 "$CERT_DIR/postgresql-server.crt"
    log_info "PostgreSQL SSL certificate generated successfully"
}

# =============================================================================
# REDIS TLS CERTIFICATE GENERATION
# =============================================================================

generate_redis_certificate() {
    log_info "Generating Redis TLS certificate..."
    
    # Generate Redis private key
    openssl genrsa -out "$CERT_DIR/redis-server.key" $CERT_KEY_SIZE
    chmod 600 "$CERT_DIR/redis-server.key"
    
    # Generate Redis certificate signing request
    openssl req -new -key "$CERT_DIR/redis-server.key" \
        -out "$CERT_DIR/redis-server.csr" \
        -subj "$REDIS_SUBJECT"
    
    # Generate Redis certificate signed by CA
    openssl x509 -req -in "$CERT_DIR/redis-server.csr" \
        -CA "$CERT_DIR/ca.crt" \
        -CAkey "$CERT_DIR/ca.key" \
        -CAcreateserial \
        -out "$CERT_DIR/redis-server.crt" \
        -days $CERT_VALIDITY_DAYS \
        -extensions v3_redis \
        -extfile <(cat <<EOF
[v3_redis]
basicConstraints = CA:FALSE
keyUsage = critical,digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth,clientAuth
subjectAltName = @alt_names_redis
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer

[alt_names_redis]
DNS.1 = redis
DNS.2 = carddemo-redis
DNS.3 = localhost
IP.1 = 127.0.0.1
EOF
)
    
    # Clean up CSR
    rm -f "$CERT_DIR/redis-server.csr"
    chmod 644 "$CERT_DIR/redis-server.crt"
    log_info "Redis TLS certificate generated successfully"
}

# =============================================================================
# PKCS12 KEYSTORE GENERATION
# =============================================================================

generate_keystore() {
    log_info "Generating PKCS12 keystore for Spring Boot SSL configuration..."
    
    # Create keystore with server certificate and private key
    openssl pkcs12 -export \
        -in "$CERT_DIR/server.crt" \
        -inkey "$CERT_DIR/server.key" \
        -certfile "$CERT_DIR/ca.crt" \
        -out "$CERT_DIR/keystore.p12" \
        -name "carddemo-server" \
        -password "pass:$KEYSTORE_PASSWORD"
    
    chmod 644 "$CERT_DIR/keystore.p12"
    log_info "PKCS12 keystore generated successfully"
}

# =============================================================================
# PKCS12 TRUSTSTORE GENERATION
# =============================================================================

generate_truststore() {
    log_info "Generating PKCS12 truststore for mutual TLS authentication..."
    
    # Create truststore with CA certificate and client certificate
    # First, create CA truststore
    openssl pkcs12 -export \
        -nokeys \
        -in "$CERT_DIR/ca.crt" \
        -out "$CERT_DIR/truststore.p12" \
        -name "carddemo-ca" \
        -password "pass:$TRUSTSTORE_PASSWORD"
    
    # Add client certificate to truststore for mutual TLS validation
    # Note: This creates a new truststore with both CA and client cert
    cat "$CERT_DIR/ca.crt" "$CERT_DIR/client.crt" > "$CERT_DIR/truststore-bundle.crt"
    
    openssl pkcs12 -export \
        -nokeys \
        -in "$CERT_DIR/truststore-bundle.crt" \
        -out "$CERT_DIR/truststore.p12" \
        -name "carddemo-truststore" \
        -password "pass:$TRUSTSTORE_PASSWORD"
    
    # Clean up temporary bundle
    rm -f "$CERT_DIR/truststore-bundle.crt"
    
    chmod 644 "$CERT_DIR/truststore.p12"
    log_info "PKCS12 truststore generated successfully"
}

# =============================================================================
# CERTIFICATE CHAIN VALIDATION
# =============================================================================

validate_certificate_chain() {
    log_info "Validating certificate chain integrity..."
    
    # Validate server certificate against CA
    if openssl verify -CAfile "$CERT_DIR/ca.crt" "$CERT_DIR/server.crt" >/dev/null 2>&1; then
        log_info "✓ Server certificate chain validation passed"
    else
        log_error "✗ Server certificate chain validation failed"
        return 1
    fi
    
    # Validate client certificate against CA
    if openssl verify -CAfile "$CERT_DIR/ca.crt" "$CERT_DIR/client.crt" >/dev/null 2>&1; then
        log_info "✓ Client certificate chain validation passed"
    else
        log_error "✗ Client certificate chain validation failed"
        return 1
    fi
    
    # Validate PostgreSQL certificate against CA
    if openssl verify -CAfile "$CERT_DIR/ca.crt" "$CERT_DIR/postgresql-server.crt" >/dev/null 2>&1; then
        log_info "✓ PostgreSQL certificate chain validation passed"
    else
        log_error "✗ PostgreSQL certificate chain validation failed"
        return 1
    fi
    
    # Validate Redis certificate against CA
    if openssl verify -CAfile "$CERT_DIR/ca.crt" "$CERT_DIR/redis-server.crt" >/dev/null 2>&1; then
        log_info "✓ Redis certificate chain validation passed"
    else
        log_error "✗ Redis certificate chain validation failed"
        return 1
    fi
    
    # Validate PKCS12 keystore
    if openssl pkcs12 -in "$CERT_DIR/keystore.p12" -noout -passin "pass:$KEYSTORE_PASSWORD" >/dev/null 2>&1; then
        log_info "✓ PKCS12 keystore validation passed"
    else
        log_error "✗ PKCS12 keystore validation failed"
        return 1
    fi
    
    # Validate PKCS12 truststore
    if openssl pkcs12 -in "$CERT_DIR/truststore.p12" -noout -passin "pass:$TRUSTSTORE_PASSWORD" >/dev/null 2>&1; then
        log_info "✓ PKCS12 truststore validation passed"
    else
        log_error "✗ PKCS12 truststore validation failed"
        return 1
    fi
    
    log_info "All certificate chain validations completed successfully"
}

# =============================================================================
# CERTIFICATE INFORMATION DISPLAY
# =============================================================================

display_certificate_info() {
    log_info "Certificate generation summary:"
    echo ""
    echo "Generated certificates:"
    echo "  ca.crt                 - Certificate Authority (4096-bit RSA, 10-year validity)"
    echo "  server.crt             - Server certificate for HTTPS (2048-bit RSA, 1-year validity)"
    echo "  client.crt             - Client certificate for mutual TLS (2048-bit RSA, 1-year validity)"
    echo "  postgresql-server.crt  - PostgreSQL SSL certificate (2048-bit RSA, 1-year validity)"
    echo "  redis-server.crt       - Redis TLS certificate (2048-bit RSA, 1-year validity)"
    echo ""
    echo "Generated keystores:"
    echo "  keystore.p12           - PKCS12 keystore for Spring Boot SSL (password: $KEYSTORE_PASSWORD)"
    echo "  truststore.p12         - PKCS12 truststore for mutual TLS (password: $TRUSTSTORE_PASSWORD)"
    echo ""
    echo "All certificates are signed by the CardDemo Test CA and configured for:"
    echo "  - Spring Boot HTTPS endpoints (localhost, carddemo-backend, carddemo-gateway)"
    echo "  - Mutual TLS service-to-service communication"
    echo "  - PostgreSQL SSL connections (postgres, postgresql, carddemo-postgres)"
    echo "  - Redis TLS connections (redis, carddemo-redis)"
    echo ""
    echo "For Spring Boot SSL configuration, use:"
    echo "  server.ssl.key-store=classpath:certificates/keystore.p12"
    echo "  server.ssl.key-store-password=$KEYSTORE_PASSWORD"
    echo "  server.ssl.trust-store=classpath:certificates/truststore.p12"
    echo "  server.ssl.trust-store-password=$TRUSTSTORE_PASSWORD"
    echo ""
}

# =============================================================================
# MAIN EXECUTION FLOW
# =============================================================================

main() {
    log_info "Starting CardDemo test certificate generation..."
    
    # Prerequisites and setup
    check_dependencies
    setup_directories
    clean_existing_certs
    
    # Generate Certificate Authority
    generate_ca_certificate
    
    # Generate server and client certificates
    generate_server_certificate
    generate_client_certificate
    
    # Generate database and session store certificates
    generate_postgresql_certificate
    generate_redis_certificate
    
    # Generate PKCS12 keystores and truststores
    generate_keystore
    generate_truststore
    
    # Validate all generated certificates
    validate_certificate_chain
    
    # Display summary information
    display_certificate_info
    
    log_info "CardDemo test certificate generation completed successfully!"
}

# =============================================================================
# SCRIPT EXECUTION
# =============================================================================

# Execute main function if script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi