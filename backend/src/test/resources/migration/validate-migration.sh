#!/bin/bash

# =============================================================================
# validate-migration.sh - VSAM to PostgreSQL Migration Validation Script
# =============================================================================
# 
# PURPOSE:
# This script executes comprehensive migration validation testing for the
# CardDemo COBOL-to-Java Spring Boot migration project. It performs automated
# comparison between VSAM exported data and PostgreSQL imported data, validates
# data integrity through parallel testing, and generates accuracy reports.
#
# TECHNICAL REQUIREMENTS:
# - Execute Spring Batch migration validation jobs
# - Run PostgreSQL queries against migrated data
# - Compare with expected VSAM output files
# - Generate CSV difference reports with field-level accuracy
# - Calculate migration accuracy percentages
# - Validate row counts and checksums for data integrity
# - Produce pass/fail status for CI/CD pipeline integration
#
# DEPENDENCIES:
# - Java 21 JRE for Spring Boot application execution
# - PostgreSQL 15+ client (psql) for database operations
# - bash 5.0+ for shell script execution
# - Core Unix utilities (grep, awk, sed, sort, uniq, wc, diff, cut, md5sum)
# - curl for REST API communication with BatchJobLauncher
# - jq for JSON processing of batch job responses
#
# CONFIGURATION FILES:
# - migration-validation.yml: Spring Batch job configuration
# - parallel-run-config.yml: Parallel comparison settings  
# - batch-etl-test.properties: ETL performance properties
# - performance-baseline.yml: Performance threshold definitions
# - vsam-key-mappings.json: VSAM to PostgreSQL key mappings
# - field-conversion-rules.json: Data type conversion specifications
#
# =============================================================================

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# =============================================================================
# GLOBAL CONFIGURATION
# =============================================================================

# Script metadata
readonly SCRIPT_NAME="validate-migration.sh"
readonly SCRIPT_VERSION="1.0.0"
readonly SCRIPT_AUTHOR="CardDemo Migration Team"

# Directory paths
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../../../.." && pwd)"
readonly BACKEND_DIR="${PROJECT_ROOT}/backend"
readonly MIGRATION_DIR="${SCRIPT_DIR}"
readonly REPORTS_DIR="${MIGRATION_DIR}/reports"
readonly LOGS_DIR="${MIGRATION_DIR}/logs"

# Application configuration
readonly SPRING_BOOT_JAR="${BACKEND_DIR}/target/carddemo-1.0.0.jar"
readonly SPRING_PROFILES="migration-validation,test"
readonly SPRING_CONFIG_LOCATION="${MIGRATION_DIR}/"

# Database configuration
readonly DB_HOST="${DB_HOST:-localhost}"
readonly DB_PORT="${DB_PORT:-5432}"
readonly DB_NAME="${DB_NAME:-carddemo_validation}"
readonly DB_USER="${DB_USER:-carddemo_test}"
readonly DB_PASSWORD="${DB_PASSWORD:-testpass}"

# REST API configuration  
readonly BATCH_API_BASE_URL="${BATCH_API_BASE_URL:-http://localhost:8080/api/batch}"
readonly API_TIMEOUT="${API_TIMEOUT:-300}"

# Performance thresholds from performance-baseline.yml
readonly MAX_PROCESSING_TIME_HOURS="${MAX_PROCESSING_TIME_HOURS:-4}"
readonly MAX_API_RESPONSE_TIME_MS="${MAX_API_RESPONSE_TIME_MS:-200}"
readonly MIN_ACCURACY_PERCENTAGE="${MIN_ACCURACY_PERCENTAGE:-99.99}"
readonly MAX_ERROR_RATE_PERCENT="${MAX_ERROR_RATE_PERCENT:-0.01}"

# File locations
readonly VSAM_EXPORT_FILE="${MIGRATION_DIR}/vsam-export-samples.txt"
readonly POSTGRESQL_EXPECTED_FILE="${MIGRATION_DIR}/postgresql-expected-data.sql"
readonly VSAM_MAPPINGS_FILE="${MIGRATION_DIR}/vsam-key-mappings.json"
readonly CONVERSION_RULES_FILE="${MIGRATION_DIR}/field-conversion-rules.json"

# Report configuration
readonly ACCURACY_REPORT="${REPORTS_DIR}/migration-accuracy-report.csv"
readonly DIFFERENCE_REPORT="${REPORTS_DIR}/data-differences.csv"
readonly SUMMARY_REPORT="${REPORTS_DIR}/validation-summary.json"
readonly PERFORMANCE_REPORT="${REPORTS_DIR}/performance-metrics.csv"

# Logging configuration
readonly LOG_FILE="${LOGS_DIR}/validate-migration.log"
readonly ERROR_LOG="${LOGS_DIR}/validation-errors.log"

# Color codes for output formatting
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# =============================================================================
# LOGGING AND UTILITY FUNCTIONS
# =============================================================================

# Initialize directories and logging
init_environment() {
    # Create required directories
    mkdir -p "${REPORTS_DIR}" "${LOGS_DIR}"
    
    # Initialize log files
    echo "Migration validation started at $(date)" > "${LOG_FILE}"
    echo "Error log initialized at $(date)" > "${ERROR_LOG}"
    
    # Set appropriate permissions
    chmod 755 "${REPORTS_DIR}" "${LOGS_DIR}"
    chmod 644 "${LOG_FILE}" "${ERROR_LOG}"
}

# Logging functions
log_info() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${GREEN}[INFO]${NC} ${message}"
    echo "[${timestamp}] INFO: ${message}" >> "${LOG_FILE}"
}

log_warn() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${YELLOW}[WARN]${NC} ${message}"
    echo "[${timestamp}] WARN: ${message}" >> "${LOG_FILE}"
}

log_error() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${RED}[ERROR]${NC} ${message}" >&2
    echo "[${timestamp}] ERROR: ${message}" >> "${ERROR_LOG}"
    echo "[${timestamp}] ERROR: ${message}" >> "${LOG_FILE}"
}

log_debug() {
    local message="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    if [[ "${DEBUG:-0}" == "1" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} ${message}"
    fi
    echo "[${timestamp}] DEBUG: ${message}" >> "${LOG_FILE}"
}

# Cleanup function for script exit
cleanup_on_exit() {
    local exit_code=$?
    log_info "Migration validation completed with exit code: ${exit_code}"
    
    # Stop Spring Boot application if running
    if [[ -n "${SPRING_PID:-}" ]]; then
        log_info "Stopping Spring Boot application (PID: ${SPRING_PID})"
        kill "${SPRING_PID}" 2>/dev/null || true
        wait "${SPRING_PID}" 2>/dev/null || true
    fi
    
    exit $exit_code
}

trap cleanup_on_exit EXIT

# =============================================================================
# VALIDATION PREREQUISITES
# =============================================================================

# Check required dependencies
check_dependencies() {
    log_info "Checking system dependencies..."
    
    local missing_deps=()
    
    # Check Java 21
    if ! command -v java &> /dev/null; then
        missing_deps+=("java (OpenJDK 21)")
    else
        local java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
        if [[ ! "${java_version}" =~ ^21\. ]]; then
            log_warn "Java version ${java_version} detected, Java 21 recommended"
        fi
    fi
    
    # Check PostgreSQL client
    if ! command -v psql &> /dev/null; then
        missing_deps+=("psql (PostgreSQL client)")
    fi
    
    # Check required utilities
    local required_utils=("curl" "jq" "grep" "awk" "sed" "sort" "uniq" "wc" "diff" "cut" "md5sum")
    for util in "${required_utils[@]}"; do
        if ! command -v "${util}" &> /dev/null; then
            missing_deps+=("${util}")
        fi
    done
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        log_error "Please install missing dependencies before running validation"
        exit 1
    fi
    
    log_info "All system dependencies validated successfully"
}

# Validate configuration files
validate_config_files() {
    log_info "Validating configuration files..."
    
    local config_files=(
        "${MIGRATION_DIR}/migration-validation.yml"
        "${MIGRATION_DIR}/parallel-run-config.yml"
        "${MIGRATION_DIR}/batch-etl-test.properties"
        "${MIGRATION_DIR}/performance-baseline.yml"
        "${VSAM_MAPPINGS_FILE}"
        "${CONVERSION_RULES_FILE}"
    )
    
    for config_file in "${config_files[@]}"; do
        if [[ ! -f "${config_file}" ]]; then
            log_error "Configuration file not found: ${config_file}"
            exit 1
        fi
        log_debug "Configuration file validated: ${config_file}"
    done
    
    # Validate JSON configuration files
    if ! jq . "${VSAM_MAPPINGS_FILE}" > /dev/null 2>&1; then
        log_error "Invalid JSON format: ${VSAM_MAPPINGS_FILE}"
        exit 1
    fi
    
    if ! jq . "${CONVERSION_RULES_FILE}" > /dev/null 2>&1; then
        log_error "Invalid JSON format: ${CONVERSION_RULES_FILE}"
        exit 1
    fi
    
    log_info "All configuration files validated successfully"
}

# Test database connectivity
test_database_connection() {
    log_info "Testing database connectivity..."
    
    local db_url="postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
    
    if ! PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT 1;" &> /dev/null; then
        log_error "Failed to connect to PostgreSQL database: ${db_url}"
        log_error "Please verify database is running and credentials are correct"
        exit 1
    fi
    
    log_info "Database connectivity validated successfully"
}

# =============================================================================
# SPRING BOOT APPLICATION MANAGEMENT
# =============================================================================

# Start Spring Boot application for batch job execution
start_spring_application() {
    log_info "Starting Spring Boot application..."
    
    # Check if Spring Boot JAR exists
    if [[ ! -f "${SPRING_BOOT_JAR}" ]]; then
        log_error "Spring Boot JAR not found: ${SPRING_BOOT_JAR}"
        log_error "Please run 'mvn clean package' to build the application"
        exit 1
    fi
    
    # Set Spring configuration
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES}"
    export SPRING_CONFIG_ADDITIONAL_LOCATION="file:${SPRING_CONFIG_LOCATION}"
    
    # Start Spring Boot application in background
    java -jar "${SPRING_BOOT_JAR}" \
        --spring.profiles.active="${SPRING_PROFILES}" \
        --spring.config.additional-location="file:${SPRING_CONFIG_LOCATION}" \
        --logging.file.name="${LOGS_DIR}/spring-boot.log" \
        --server.port=8080 &
    
    SPRING_PID=$!
    log_info "Spring Boot application started (PID: ${SPRING_PID})"
    
    # Wait for application to be ready
    local max_wait=120
    local wait_count=0
    
    while [[ $wait_count -lt $max_wait ]]; do
        if curl -s -f "${BATCH_API_BASE_URL}/health" > /dev/null 2>&1; then
            log_info "Spring Boot application is ready for requests"
            return 0
        fi
        
        sleep 2
        ((wait_count += 2))
        
        if [[ $((wait_count % 20)) -eq 0 ]]; then
            log_info "Waiting for Spring Boot application... (${wait_count}/${max_wait}s)"
        fi
    done
    
    log_error "Spring Boot application failed to start within ${max_wait} seconds"
    exit 1
}

# =============================================================================
# BATCH JOB EXECUTION FUNCTIONS
# =============================================================================

# Execute a specific batch job via REST API
execute_batch_job() {
    local job_name="$1"
    local job_params="${2:-}"
    
    log_info "Executing batch job: ${job_name}"
    log_debug "Job parameters: ${job_params}"
    
    # Prepare job launch request
    local request_payload
    if [[ -n "${job_params}" ]]; then
        request_payload=$(jq -n --arg job "${job_name}" --arg params "${job_params}" \
            '{jobName: $job, jobParameters: $params}')
    else
        request_payload=$(jq -n --arg job "${job_name}" \
            '{jobName: $job, jobParameters: {}}')
    fi
    
    # Launch batch job
    local response
    response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "${request_payload}" \
        --max-time "${API_TIMEOUT}" \
        "${BATCH_API_BASE_URL}/jobs/launch" 2>/dev/null)
    
    if [[ $? -ne 0 ]]; then
        log_error "Failed to launch batch job: ${job_name}"
        return 1
    fi
    
    # Extract job execution ID
    local job_execution_id
    job_execution_id=$(echo "${response}" | jq -r '.jobExecutionId // empty')
    
    if [[ -z "${job_execution_id}" ]]; then
        log_error "Failed to get job execution ID for: ${job_name}"
        log_debug "API Response: ${response}"
        return 1
    fi
    
    log_info "Job launched successfully - Execution ID: ${job_execution_id}"
    
    # Monitor job execution
    monitor_job_execution "${job_execution_id}" "${job_name}"
}

# Monitor batch job execution status
monitor_job_execution() {
    local job_execution_id="$1"
    local job_name="$2"
    
    log_info "Monitoring job execution: ${job_name} (ID: ${job_execution_id})"
    
    local status="UNKNOWN"
    local start_time=$(date +%s)
    local max_runtime=$((MAX_PROCESSING_TIME_HOURS * 3600))  # Convert hours to seconds
    
    while [[ "${status}" == "UNKNOWN" || "${status}" == "STARTING" || "${status}" == "STARTED" ]]; do
        # Check execution time limit
        local current_time=$(date +%s)
        local elapsed_time=$((current_time - start_time))
        
        if [[ $elapsed_time -gt $max_runtime ]]; then
            log_error "Job execution exceeded maximum runtime: ${MAX_PROCESSING_TIME_HOURS} hours"
            return 1
        fi
        
        # Query job status
        local response
        response=$(curl -s "${BATCH_API_BASE_URL}/jobs/status/${job_execution_id}" 2>/dev/null)
        
        if [[ $? -ne 0 ]]; then
            log_error "Failed to query job status for execution ID: ${job_execution_id}"
            return 1
        fi
        
        # Parse status and metrics
        status=$(echo "${response}" | jq -r '.status // "UNKNOWN"')
        local read_count=$(echo "${response}" | jq -r '.readCount // 0')
        local write_count=$(echo "${response}" | jq -r '.writeCount // 0')
        local skip_count=$(echo "${response}" | jq -r '.skipCount // 0')
        
        log_debug "Job Status: ${status}, Read: ${read_count}, Write: ${write_count}, Skip: ${skip_count}"
        
        # Log progress every 30 seconds
        if [[ $((elapsed_time % 30)) -eq 0 && $elapsed_time -gt 0 ]]; then
            log_info "Job progress - Status: ${status}, Processed: ${read_count}, Written: ${write_count}"
        fi
        
        sleep 10
    done
    
    # Final status check
    if [[ "${status}" == "COMPLETED" ]]; then
        log_info "Job completed successfully: ${job_name}"
        
        # Log final metrics
        local response
        response=$(curl -s "${BATCH_API_BASE_URL}/jobs/execution/${job_execution_id}" 2>/dev/null)
        
        if [[ $? -eq 0 ]]; then
            local duration=$(echo "${response}" | jq -r '.duration // 0')
            local total_items=$(echo "${response}" | jq -r '.readCount // 0')
            
            log_info "Job metrics - Duration: ${duration}ms, Total items: ${total_items}"
            
            # Record performance metrics
            echo "${job_name},${job_execution_id},${status},${duration},${total_items}" >> "${PERFORMANCE_REPORT}"
        fi
        
        return 0
    else
        log_error "Job failed or was stopped: ${job_name} (Status: ${status})"
        
        # Get failure details if available
        local failure_exceptions=$(echo "${response}" | jq -r '.failureExceptions // []')
        if [[ "${failure_exceptions}" != "[]" ]]; then
            log_error "Job failure details: ${failure_exceptions}"
        fi
        
        return 1
    fi
}

# Execute all migration validation batch jobs
execute_batch_jobs() {
    log_info "Starting batch job execution phase"
    
    # Initialize performance report
    echo "JobName,ExecutionId,Status,Duration,TotalItems" > "${PERFORMANCE_REPORT}"
    
    # Define jobs to execute in order
    local jobs=(
        "accountDataValidationJob"
        "customerDataValidationJob"
        "transactionDataValidationJob"
        "cardDataValidationJob"
    )
    
    local failed_jobs=()
    
    # Execute each job sequentially
    for job in "${jobs[@]}"; do
        if ! execute_batch_job "${job}"; then
            log_error "Batch job failed: ${job}"
            failed_jobs+=("${job}")
        fi
    done
    
    # Check overall batch execution success
    if [[ ${#failed_jobs[@]} -gt 0 ]]; then
        log_error "Failed batch jobs: ${failed_jobs[*]}"
        return 1
    fi
    
    log_info "All batch jobs completed successfully"
    return 0
}

# =============================================================================
# DATA INTEGRITY COMPARISON FUNCTIONS
# =============================================================================

# Compare data integrity between source and target
compare_data_integrity() {
    log_info "Starting data integrity comparison"
    
    # Initialize difference report
    echo "Table,Field,SourceValue,TargetValue,DifferenceType,RecordId" > "${DIFFERENCE_REPORT}"
    
    local overall_success=true
    
    # Compare each table
    if ! compare_account_data; then
        overall_success=false
    fi
    
    if ! compare_customer_data; then
        overall_success=false
    fi
    
    if ! compare_transaction_data; then
        overall_success=false
    fi
    
    if ! compare_card_data; then
        overall_success=false
    fi
    
    if [[ "${overall_success}" == "true" ]]; then
        log_info "Data integrity comparison completed successfully"
        return 0
    else
        log_error "Data integrity comparison found discrepancies"
        return 1
    fi
}

# Compare account data between source and target
compare_account_data() {
    log_info "Comparing account data integrity..."
    
    local temp_source_file="${REPORTS_DIR}/source_account_data.csv"
    local temp_target_file="${REPORTS_DIR}/target_account_data.csv"
    
    # Extract account data from VSAM export samples
    extract_account_data_from_vsam "${temp_source_file}"
    
    # Extract account data from PostgreSQL
    extract_account_data_from_postgresql "${temp_target_file}"
    
    # Compare the datasets
    compare_datasets "account_data" "${temp_source_file}" "${temp_target_file}"
    local result=$?
    
    # Cleanup temporary files
    rm -f "${temp_source_file}" "${temp_target_file}"
    
    return $result
}

# Extract account data from VSAM export format
extract_account_data_from_vsam() {
    local output_file="$1"
    
    log_debug "Extracting account data from VSAM export..."
    
    # Parse VSAM fixed-width records for ACCOUNT_DATA section
    # Based on CVACT01Y copybook structure from vsam-export-samples.txt
    awk '
    BEGIN { 
        OFS=","
        print "account_id,customer_id,active_status,current_balance,credit_limit,cash_credit_limit,open_date"
    }
    /^ACCOUNT_DATA:/ { in_account_section = 1; next }
    /^[A-Z_]+:/ && in_account_section { in_account_section = 0 }
    in_account_section && NF > 0 && !/^#/ && !/^-+$/ {
        # Parse fixed-width ACCOUNT_DATA record (289 characters total)
        account_id = substr($0, 1, 11)
        customer_id = substr($0, 12, 9)
        active_status = substr($0, 21, 1)
        current_balance = substr($0, 22, 12)
        credit_limit = substr($0, 34, 12)
        cash_credit_limit = substr($0, 46, 12)
        open_date = substr($0, 58, 10)
        
        # Clean up fields (remove leading zeros, handle COMP-3 format)
        gsub(/^0+/, "", account_id); if (account_id == "") account_id = "0"
        gsub(/^0+/, "", customer_id); if (customer_id == "") customer_id = "0"
        gsub(/^ +| +$/, "", active_status)
        
        # Convert COMP-3 packed decimal (indicated by { sign) to decimal
        gsub(/\{/, "", current_balance)
        gsub(/\{/, "", credit_limit)  
        gsub(/\{/, "", cash_credit_limit)
        current_balance = current_balance / 100  # Assume 2 decimal places
        credit_limit = credit_limit / 100
        cash_credit_limit = cash_credit_limit / 100
        
        # Format date (YYYY-MM-DD)
        if (length(open_date) == 8) {
            open_date = substr(open_date, 1, 4) "-" substr(open_date, 5, 2) "-" substr(open_date, 7, 2)
        }
        
        print account_id, customer_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date
    }
    ' "${VSAM_EXPORT_FILE}" > "${output_file}"
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} account records from VSAM export"
}

# Extract account data from PostgreSQL
extract_account_data_from_postgresql() {
    local output_file="$1"
    
    log_debug "Extracting account data from PostgreSQL..."
    
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -c "COPY (SELECT account_id, customer_id, active_status, current_balance, credit_limit, cash_credit_limit, open_date FROM account_data ORDER BY account_id) TO STDOUT WITH CSV HEADER;" \
        > "${output_file}" 2>/dev/null
    
    if [[ $? -ne 0 ]]; then
        log_error "Failed to extract account data from PostgreSQL"
        return 1
    fi
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} account records from PostgreSQL"
}

# Compare customer data between source and target
compare_customer_data() {
    log_info "Comparing customer data integrity..."
    
    local temp_source_file="${REPORTS_DIR}/source_customer_data.csv"
    local temp_target_file="${REPORTS_DIR}/target_customer_data.csv"
    
    # Extract customer data from VSAM export samples
    extract_customer_data_from_vsam "${temp_source_file}"
    
    # Extract customer data from PostgreSQL
    extract_customer_data_from_postgresql "${temp_target_file}"
    
    # Compare the datasets
    compare_datasets "customer_data" "${temp_source_file}" "${temp_target_file}"
    local result=$?
    
    # Cleanup temporary files
    rm -f "${temp_source_file}" "${temp_target_file}"
    
    return $result
}

# Extract customer data from VSAM export format
extract_customer_data_from_vsam() {
    local output_file="$1"
    
    log_debug "Extracting customer data from VSAM export..."
    
    # Parse VSAM fixed-width records for CUSTOMER_DATA section
    awk '
    BEGIN { 
        OFS=","
        print "customer_id,first_name,last_name,address_line_1,city,state_code,zip_code,phone_number,government_id,date_of_birth,fico_score"
    }
    /^CUSTOMER_DATA:/ { in_customer_section = 1; next }
    /^[A-Z_]+:/ && in_customer_section { in_customer_section = 0 }
    in_customer_section && NF > 0 && !/^#/ && !/^-+$/ {
        # Parse fixed-width CUSTOMER_DATA record (502 characters total)
        customer_id = substr($0, 1, 9)
        first_name = substr($0, 10, 20)
        last_name = substr($0, 30, 20)
        address_line_1 = substr($0, 50, 50)
        city = substr($0, 100, 25)
        state_code = substr($0, 125, 2)
        zip_code = substr($0, 127, 10)
        phone_number = substr($0, 137, 15)
        government_id = substr($0, 152, 20)
        date_of_birth = substr($0, 172, 10)
        fico_score = substr($0, 182, 3)
        
        # Clean up fields
        gsub(/^0+/, "", customer_id); if (customer_id == "") customer_id = "0"
        gsub(/^ +| +$/, "", first_name)
        gsub(/^ +| +$/, "", last_name)
        gsub(/^ +| +$/, "", address_line_1)
        gsub(/^ +| +$/, "", city)
        gsub(/^ +| +$/, "", state_code)
        gsub(/^ +| +$/, "", zip_code)
        gsub(/^ +| +$/, "", phone_number)
        gsub(/^ +| +$/, "", government_id)
        gsub(/^0+/, "", fico_score); if (fico_score == "") fico_score = "0"
        
        # Format date (YYYY-MM-DD)
        if (length(date_of_birth) == 8) {
            date_of_birth = substr(date_of_birth, 1, 4) "-" substr(date_of_birth, 5, 2) "-" substr(date_of_birth, 7, 2)
        }
        
        print customer_id, first_name, last_name, address_line_1, city, state_code, zip_code, phone_number, government_id, date_of_birth, fico_score
    }
    ' "${VSAM_EXPORT_FILE}" > "${output_file}"
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} customer records from VSAM export"
}

# Extract customer data from PostgreSQL
extract_customer_data_from_postgresql() {
    local output_file="$1"
    
    log_debug "Extracting customer data from PostgreSQL..."
    
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -c "COPY (SELECT customer_id, first_name, last_name, address_line_1, city, state_code, zip_code, phone_number, government_id, date_of_birth, fico_score FROM customer_data ORDER BY customer_id) TO STDOUT WITH CSV HEADER;" \
        > "${output_file}" 2>/dev/null
    
    if [[ $? -ne 0 ]]; then
        log_error "Failed to extract customer data from PostgreSQL"
        return 1
    fi
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} customer records from PostgreSQL"
}

# Compare transaction data between source and target
compare_transaction_data() {
    log_info "Comparing transaction data integrity..."
    
    local temp_source_file="${REPORTS_DIR}/source_transaction_data.csv"
    local temp_target_file="${REPORTS_DIR}/target_transaction_data.csv"
    
    # Extract transaction data from VSAM export samples
    extract_transaction_data_from_vsam "${temp_source_file}"
    
    # Extract transaction data from PostgreSQL
    extract_transaction_data_from_postgresql "${temp_target_file}"
    
    # Compare the datasets
    compare_datasets "transactions" "${temp_source_file}" "${temp_target_file}"
    local result=$?
    
    # Cleanup temporary files
    rm -f "${temp_source_file}" "${temp_target_file}"
    
    return $result
}

# Extract transaction data from VSAM export format
extract_transaction_data_from_vsam() {
    local output_file="$1"
    
    log_debug "Extracting transaction data from VSAM export..."
    
    # Parse VSAM fixed-width records for TRANSACTION_DATA section
    awk '
    BEGIN { 
        OFS=","
        print "transaction_id,account_id,card_number,transaction_date,amount,transaction_type_code,category_code,merchant_name"
    }
    /^TRANSACTION_DATA:/ { in_transaction_section = 1; next }
    /^[A-Z_]+:/ && in_transaction_section { in_transaction_section = 0 }
    in_transaction_section && NF > 0 && !/^#/ && !/^-+$/ {
        # Parse fixed-width TRANSACTION_DATA record (189 characters total)
        transaction_id = substr($0, 1, 16)
        account_id = substr($0, 17, 11)
        card_number = substr($0, 28, 16)
        transaction_date = substr($0, 44, 10)
        amount = substr($0, 54, 12)
        transaction_type_code = substr($0, 66, 2)
        category_code = substr($0, 68, 4)
        merchant_name = substr($0, 72, 100)
        
        # Clean up fields
        gsub(/^0+/, "", transaction_id); if (transaction_id == "") transaction_id = "0"
        gsub(/^0+/, "", account_id); if (account_id == "") account_id = "0"
        gsub(/^ +| +$/, "", card_number)
        gsub(/^ +| +$/, "", transaction_type_code)
        gsub(/^0+/, "", category_code); if (category_code == "") category_code = "0"
        gsub(/^ +| +$/, "", merchant_name)
        
        # Convert COMP-3 packed decimal to amount
        gsub(/\{/, "", amount)
        amount = amount / 100  # Assume 2 decimal places
        
        # Format date (YYYY-MM-DD)
        if (length(transaction_date) == 8) {
            transaction_date = substr(transaction_date, 1, 4) "-" substr(transaction_date, 5, 2) "-" substr(transaction_date, 7, 2)
        }
        
        print transaction_id, account_id, card_number, transaction_date, amount, transaction_type_code, category_code, merchant_name
    }
    ' "${VSAM_EXPORT_FILE}" > "${output_file}"
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} transaction records from VSAM export"
}

# Extract transaction data from PostgreSQL
extract_transaction_data_from_postgresql() {
    local output_file="$1"
    
    log_debug "Extracting transaction data from PostgreSQL..."
    
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -c "COPY (SELECT transaction_id, account_id, card_number, transaction_date, amount, transaction_type_code, category_code, merchant_name FROM transactions ORDER BY transaction_id) TO STDOUT WITH CSV HEADER;" \
        > "${output_file}" 2>/dev/null
    
    if [[ $? -ne 0 ]]; then
        log_error "Failed to extract transaction data from PostgreSQL"
        return 1
    fi
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} transaction records from PostgreSQL"
}

# Compare card data between source and target
compare_card_data() {
    log_info "Comparing card data integrity..."
    
    local temp_source_file="${REPORTS_DIR}/source_card_data.csv"
    local temp_target_file="${REPORTS_DIR}/target_card_data.csv"
    
    # Extract card data from VSAM export samples
    extract_card_data_from_vsam "${temp_source_file}"
    
    # Extract card data from PostgreSQL
    extract_card_data_from_postgresql "${temp_target_file}"
    
    # Compare the datasets
    compare_datasets "card_data" "${temp_source_file}" "${temp_target_file}"
    local result=$?
    
    # Cleanup temporary files
    rm -f "${temp_source_file}" "${temp_target_file}"
    
    return $result
}

# Extract card data from VSAM export format
extract_card_data_from_vsam() {
    local output_file="$1"
    
    log_debug "Extracting card data from VSAM export..."
    
    # Parse VSAM fixed-width records for CARD_DATA section
    awk '
    BEGIN { 
        OFS=","
        print "card_number,account_id,customer_id,card_type,expiration_date,active_status"
    }
    /^CARD_DATA:/ { in_card_section = 1; next }
    /^[A-Z_]+:/ && in_card_section { in_card_section = 0 }
    in_card_section && NF > 0 && !/^#/ && !/^-+$/ {
        # Parse fixed-width CARD_DATA record (150 characters total)
        card_number = substr($0, 1, 16)
        account_id = substr($0, 17, 11)
        customer_id = substr($0, 28, 9)
        card_type = substr($0, 37, 10)
        expiration_date = substr($0, 47, 10)
        active_status = substr($0, 57, 1)
        
        # Clean up fields
        gsub(/^ +| +$/, "", card_number)
        gsub(/^0+/, "", account_id); if (account_id == "") account_id = "0"
        gsub(/^0+/, "", customer_id); if (customer_id == "") customer_id = "0"
        gsub(/^ +| +$/, "", card_type)
        gsub(/^ +| +$/, "", active_status)
        
        # Format date (YYYY-MM-DD)
        if (length(expiration_date) == 8) {
            expiration_date = substr(expiration_date, 1, 4) "-" substr(expiration_date, 5, 2) "-" substr(expiration_date, 7, 2)
        }
        
        print card_number, account_id, customer_id, card_type, expiration_date, active_status
    }
    ' "${VSAM_EXPORT_FILE}" > "${output_file}"
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} card records from VSAM export"
}

# Extract card data from PostgreSQL
extract_card_data_from_postgresql() {
    local output_file="$1"
    
    log_debug "Extracting card data from PostgreSQL..."
    
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -c "COPY (SELECT card_number, account_id, customer_id, card_type, expiration_date, active_status FROM card_data ORDER BY card_number) TO STDOUT WITH CSV HEADER;" \
        > "${output_file}" 2>/dev/null
    
    if [[ $? -ne 0 ]]; then
        log_error "Failed to extract card data from PostgreSQL"
        return 1
    fi
    
    local record_count=$(wc -l < "${output_file}")
    record_count=$((record_count - 1))  # Subtract header
    log_debug "Extracted ${record_count} card records from PostgreSQL"
}

# Compare two datasets and identify differences
compare_datasets() {
    local table_name="$1"
    local source_file="$2"
    local target_file="$3"
    
    log_debug "Comparing datasets for table: ${table_name}"
    
    # Validate files exist and have content
    if [[ ! -f "${source_file}" ]] || [[ ! -s "${source_file}" ]]; then
        log_error "Source file is missing or empty: ${source_file}"
        return 1
    fi
    
    if [[ ! -f "${target_file}" ]] || [[ ! -s "${target_file}" ]]; then
        log_error "Target file is missing or empty: ${target_file}"
        return 1
    fi
    
    # Get record counts
    local source_count=$(($(wc -l < "${source_file}") - 1))  # Subtract header
    local target_count=$(($(wc -l < "${target_file}") - 1))  # Subtract header
    
    log_debug "Record counts - Source: ${source_count}, Target: ${target_count}"
    
    # Check record count match
    if [[ $source_count -ne $target_count ]]; then
        log_warn "Record count mismatch for ${table_name}: Source=${source_count}, Target=${target_count}"
        echo "${table_name},RECORD_COUNT,${source_count},${target_count},COUNT_MISMATCH," >> "${DIFFERENCE_REPORT}"
    fi
    
    # Perform field-by-field comparison
    local differences_found=0
    
    # Sort both files by primary key (first column) for comparison
    local sorted_source="${source_file}.sorted"
    local sorted_target="${target_file}.sorted"
    
    # Sort files (skip header, sort by first column, then add header back)
    (head -n1 "${source_file}"; tail -n+2 "${source_file}" | sort -t',' -k1,1n) > "${sorted_source}"
    (head -n1 "${target_file}"; tail -n+2 "${target_file}" | sort -t',' -k1,1n) > "${sorted_target}"
    
    # Compare sorted files line by line
    local line_num=2  # Start from line 2 (skip header)
    
    # Read both files simultaneously
    exec 3< "${sorted_source}"
    exec 4< "${sorted_target}"
    
    # Skip headers
    read -u 3 source_header
    read -u 4 target_header
    
    # Get field names from header
    IFS=',' read -ra field_names <<< "${source_header}"
    
    while IFS=',' read -u 3 -ra source_fields && IFS=',' read -u 4 -ra target_fields; do
        # Compare each field
        for i in "${!field_names[@]}"; do
            local field_name="${field_names[$i]}"
            local source_value="${source_fields[$i]:-}"
            local target_value="${target_fields[$i]:-}"
            
            # Apply tolerance for numeric fields (financial precision)
            if [[ "${field_name}" =~ (balance|limit|amount) ]]; then
                if ! compare_numeric_values "${source_value}" "${target_value}" "0.01"; then
                    echo "${table_name},${field_name},${source_value},${target_value},NUMERIC_DIFFERENCE,${source_fields[0]}" >> "${DIFFERENCE_REPORT}"
                    ((differences_found++))
                fi
            else
                # Exact string comparison for non-numeric fields
                if [[ "${source_value}" != "${target_value}" ]]; then
                    echo "${table_name},${field_name},${source_value},${target_value},VALUE_DIFFERENCE,${source_fields[0]}" >> "${DIFFERENCE_REPORT}"
                    ((differences_found++))
                fi
            fi
        done
        
        ((line_num++))
    done
    
    # Close file descriptors
    exec 3<&-
    exec 4<&-
    
    # Cleanup sorted files
    rm -f "${sorted_source}" "${sorted_target}"
    
    if [[ $differences_found -eq 0 ]]; then
        log_info "Dataset comparison successful for ${table_name}: No differences found"
        return 0
    else
        log_warn "Dataset comparison found ${differences_found} differences for ${table_name}"
        return 1
    fi
}

# Compare numeric values with tolerance
compare_numeric_values() {
    local value1="$1"
    local value2="$2"
    local tolerance="$3"
    
    # Handle empty or non-numeric values
    if [[ -z "${value1}" ]] || [[ -z "${value2}" ]]; then
        [[ "${value1}" == "${value2}" ]]
        return
    fi
    
    # Check if values are numeric
    if ! [[ "${value1}" =~ ^-?[0-9]+(\.[0-9]+)?$ ]] || ! [[ "${value2}" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; then
        [[ "${value1}" == "${value2}" ]]
        return
    fi
    
    # Calculate absolute difference
    local diff=$(awk "BEGIN { print sqrt(($value1 - $value2)^2) }")
    
    # Compare with tolerance
    awk "BEGIN { exit !($diff <= $tolerance) }"
}

# =============================================================================
# REPORT GENERATION FUNCTIONS
# =============================================================================

# Generate comprehensive difference reports
generate_difference_reports() {
    log_info "Generating difference reports..."
    
    # Ensure difference report exists
    if [[ ! -f "${DIFFERENCE_REPORT}" ]]; then
        log_warn "Difference report not found, creating empty report"
        echo "Table,Field,SourceValue,TargetValue,DifferenceType,RecordId" > "${DIFFERENCE_REPORT}"
    fi
    
    # Generate summary statistics
    generate_difference_summary
    
    # Generate detailed HTML report
    generate_html_report
    
    # Generate Excel-compatible report
    generate_excel_report
    
    log_info "Difference reports generated successfully"
}

# Generate difference summary statistics
generate_difference_summary() {
    log_debug "Generating difference summary statistics..."
    
    local summary_file="${REPORTS_DIR}/difference-summary.txt"
    
    cat > "${summary_file}" << EOF
Migration Validation - Difference Summary Report
Generated: $(date)

TABLE COMPARISON STATISTICS:
EOF
    
    # Count differences by table
    if [[ -s "${DIFFERENCE_REPORT}" ]]; then
        echo "" >> "${summary_file}"
        echo "Differences by Table:" >> "${summary_file}"
        tail -n+2 "${DIFFERENCE_REPORT}" | cut -d',' -f1 | sort | uniq -c | \
        awk '{printf "  %-20s: %d differences\n", $2, $1}' >> "${summary_file}"
        
        echo "" >> "${summary_file}"
        echo "Differences by Type:" >> "${summary_file}"
        tail -n+2 "${DIFFERENCE_REPORT}" | cut -d',' -f5 | sort | uniq -c | \
        awk '{printf "  %-20s: %d occurrences\n", $2, $1}' >> "${summary_file}"
        
        local total_differences=$(tail -n+2 "${DIFFERENCE_REPORT}" | wc -l)
        echo "" >> "${summary_file}"
        echo "TOTAL DIFFERENCES FOUND: ${total_differences}" >> "${summary_file}"
    else
        echo "" >> "${summary_file}"
        echo "No differences found - Migration validation successful!" >> "${summary_file}"
    fi
    
    log_debug "Difference summary written to: ${summary_file}"
}

# Generate HTML report for web viewing
generate_html_report() {
    log_debug "Generating HTML difference report..."
    
    local html_report="${REPORTS_DIR}/migration-validation-report.html"
    
    cat > "${html_report}" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Migration Validation Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .summary { background-color: #e8f5e8; padding: 15px; margin: 20px 0; border-radius: 5px; }
        .error { background-color: #ffe8e8; padding: 15px; margin: 20px 0; border-radius: 5px; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .numeric-diff { background-color: #fff3cd; }
        .value-diff { background-color: #f8d7da; }
        .count-diff { background-color: #d4edda; }
    </style>
</head>
<body>
    <div class="header">
        <h1>CardDemo Migration Validation Report</h1>
        <p><strong>Generated:</strong> $(date)</p>
        <p><strong>Validation Script:</strong> ${SCRIPT_NAME} v${SCRIPT_VERSION}</p>
    </div>
EOF
    
    # Add differences table if any exist
    if [[ -s "${DIFFERENCE_REPORT}" ]]; then
        local total_differences=$(tail -n+2 "${DIFFERENCE_REPORT}" | wc -l)
        
        if [[ $total_differences -gt 0 ]]; then
            cat >> "${html_report}" << EOF
    <div class="error">
        <h2>⚠️ Migration Validation Issues Found</h2>
        <p><strong>Total Differences:</strong> ${total_differences}</p>
    </div>
    
    <h2>Detailed Differences</h2>
    <table>
        <thead>
            <tr>
                <th>Table</th>
                <th>Field</th>
                <th>Source Value</th>
                <th>Target Value</th>
                <th>Difference Type</th>
                <th>Record ID</th>
            </tr>
        </thead>
        <tbody>
EOF
            
            # Add table rows from CSV data
            tail -n+2 "${DIFFERENCE_REPORT}" | while IFS=',' read -r table field source target type record_id; do
                local css_class=""
                case "$type" in
                    "NUMERIC_DIFFERENCE") css_class="numeric-diff" ;;
                    "VALUE_DIFFERENCE") css_class="value-diff" ;;
                    "COUNT_MISMATCH") css_class="count-diff" ;;
                esac
                
                cat >> "${html_report}" << EOF
            <tr class="${css_class}">
                <td>${table}</td>
                <td>${field}</td>
                <td>${source}</td>
                <td>${target}</td>
                <td>${type}</td>
                <td>${record_id}</td>
            </tr>
EOF
            done
            
            echo "        </tbody>" >> "${html_report}"
            echo "    </table>" >> "${html_report}"
        else
            cat >> "${html_report}" << 'EOF'
    <div class="summary">
        <h2>✅ Migration Validation Successful</h2>
        <p>No differences found between source VSAM data and target PostgreSQL data.</p>
    </div>
EOF
        fi
    fi
    
    cat >> "${html_report}" << 'EOF'
</body>
</html>
EOF
    
    log_debug "HTML report generated: ${html_report}"
}

# Generate Excel-compatible CSV report
generate_excel_report() {
    log_debug "Generating Excel-compatible difference report..."
    
    local excel_report="${REPORTS_DIR}/migration-differences-excel.csv"
    
    # Copy difference report with Excel-friendly formatting
    if [[ -s "${DIFFERENCE_REPORT}" ]]; then
        # Add BOM for proper UTF-8 handling in Excel
        printf '\xEF\xBB\xBF' > "${excel_report}"
        cat "${DIFFERENCE_REPORT}" >> "${excel_report}"
    else
        echo "Table,Field,SourceValue,TargetValue,DifferenceType,RecordId" > "${excel_report}"
        echo "NO_DIFFERENCES,N/A,N/A,N/A,VALIDATION_SUCCESSFUL,N/A" >> "${excel_report}"
    fi
    
    log_debug "Excel report generated: ${excel_report}"
}

# =============================================================================
# ACCURACY CALCULATION FUNCTIONS
# =============================================================================

# Calculate overall migration accuracy percentage
calculate_accuracy_percentage() {
    log_info "Calculating migration accuracy percentage..."
    
    # Initialize accuracy report
    echo "Table,TotalRecords,AccurateRecords,DifferenceCount,AccuracyPercentage" > "${ACCURACY_REPORT}"
    
    local overall_total_records=0
    local overall_accurate_records=0
    local overall_success=true
    
    # Calculate accuracy for each table
    calculate_table_accuracy "account_data" || overall_success=false
    calculate_table_accuracy "customer_data" || overall_success=false
    calculate_table_accuracy "transactions" || overall_success=false
    calculate_table_accuracy "card_data" || overall_success=false
    
    # Read accuracy report to calculate overall accuracy
    while IFS=',' read -r table total_records accurate_records difference_count accuracy_pct; do
        if [[ "${table}" != "Table" ]]; then  # Skip header
            overall_total_records=$((overall_total_records + total_records))
            overall_accurate_records=$((overall_accurate_records + accurate_records))
        fi
    done < "${ACCURACY_REPORT}"
    
    # Calculate overall accuracy percentage
    local overall_accuracy=0
    if [[ $overall_total_records -gt 0 ]]; then
        overall_accuracy=$(awk "BEGIN { printf \"%.2f\", ($overall_accurate_records * 100.0) / $overall_total_records }")
    fi
    
    # Add overall summary to report
    echo "OVERALL,${overall_total_records},${overall_accurate_records},$((overall_total_records - overall_accurate_records)),${overall_accuracy}" >> "${ACCURACY_REPORT}"
    
    log_info "Overall migration accuracy: ${overall_accuracy}%"
    log_info "Total records processed: ${overall_total_records}"
    log_info "Accurate records: ${overall_accurate_records}"
    
    # Check if accuracy meets minimum threshold
    local accuracy_check=$(awk "BEGIN { print ($overall_accuracy >= $MIN_ACCURACY_PERCENTAGE) }")
    
    if [[ "${accuracy_check}" == "1" ]]; then
        log_info "✅ Migration accuracy meets minimum threshold (${MIN_ACCURACY_PERCENTAGE}%)"
        return 0
    else
        log_error "❌ Migration accuracy ${overall_accuracy}% below minimum threshold ${MIN_ACCURACY_PERCENTAGE}%"
        return 1
    fi
}

# Calculate accuracy for a specific table
calculate_table_accuracy() {
    local table_name="$1"
    
    log_debug "Calculating accuracy for table: ${table_name}"
    
    # Count total records in target database
    local total_records
    total_records=$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -t -c "SELECT COUNT(*) FROM ${table_name};" 2>/dev/null | tr -d ' ')
    
    if [[ -z "${total_records}" ]] || [[ "${total_records}" == "0" ]]; then
        log_warn "No records found in table: ${table_name}"
        echo "${table_name},0,0,0,0.00" >> "${ACCURACY_REPORT}"
        return 1
    fi
    
    # Count differences for this table
    local difference_count=0
    if [[ -s "${DIFFERENCE_REPORT}" ]]; then
        difference_count=$(grep "^${table_name}," "${DIFFERENCE_REPORT}" | wc -l)
    fi
    
    # Calculate accurate records (assuming each difference represents one inaccurate record)
    local accurate_records=$((total_records - difference_count))
    
    # Ensure accurate records is not negative
    if [[ $accurate_records -lt 0 ]]; then
        accurate_records=0
    fi
    
    # Calculate accuracy percentage
    local accuracy_percentage=0
    if [[ $total_records -gt 0 ]]; then
        accuracy_percentage=$(awk "BEGIN { printf \"%.2f\", ($accurate_records * 100.0) / $total_records }")
    fi
    
    # Add to accuracy report
    echo "${table_name},${total_records},${accurate_records},${difference_count},${accuracy_percentage}" >> "${ACCURACY_REPORT}"
    
    log_debug "Table ${table_name} accuracy: ${accuracy_percentage}% (${accurate_records}/${total_records} records)"
    
    # Return success if accuracy meets threshold
    local accuracy_check=$(awk "BEGIN { print ($accuracy_percentage >= $MIN_ACCURACY_PERCENTAGE) }")
    [[ "${accuracy_check}" == "1" ]]
}

# =============================================================================
# VALIDATION SUMMARY AND REPORTING
# =============================================================================

# Generate final validation summary report
generate_validation_summary() {
    log_info "Generating final validation summary..."
    
    local start_time="$1"
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    # Create JSON summary report
    cat > "${SUMMARY_REPORT}" << EOF
{
  "validationSummary": {
    "scriptVersion": "${SCRIPT_VERSION}",
    "executionDate": "$(date -Iseconds)",
    "totalDurationSeconds": ${total_duration},
    "overallStatus": "PLACEHOLDER_STATUS"
  },
  "performanceMetrics": {
    "batchJobExecutions": []
  },
  "accuracyResults": {
    "overallAccuracy": 0.0,
    "totalRecords": 0,
    "accurateRecords": 0,
    "differenceCount": 0
  },
  "dataIntegrity": {
    "tablesValidated": [],
    "criticalIssues": [],
    "warnings": []
  }
}
EOF
    
    # Parse accuracy results if available
    if [[ -f "${ACCURACY_REPORT}" ]]; then
        local overall_line=$(grep "^OVERALL," "${ACCURACY_REPORT}" || echo "")
        if [[ -n "${overall_line}" ]]; then
            IFS=',' read -r _ total_records accurate_records difference_count accuracy_pct <<< "${overall_line}"
            
            # Update JSON with actual values
            local temp_json=$(mktemp)
            jq --argjson total "${total_records}" \
               --argjson accurate "${accurate_records}" \
               --argjson differences "${difference_count}" \
               --argjson accuracy "${accuracy_pct}" \
               '.accuracyResults = {
                 "overallAccuracy": $accuracy,
                 "totalRecords": $total,
                 "accurateRecords": $accurate,
                 "differenceCount": $differences
               }' "${SUMMARY_REPORT}" > "${temp_json}"
            mv "${temp_json}" "${SUMMARY_REPORT}"
        fi
    fi
    
    # Add performance metrics if available
    if [[ -f "${PERFORMANCE_REPORT}" ]]; then
        local temp_json=$(mktemp)
        local job_executions="[]"
        
        # Build job executions array from performance report
        if [[ -s "${PERFORMANCE_REPORT}" ]]; then
            job_executions=$(tail -n+2 "${PERFORMANCE_REPORT}" | \
                awk -F',' '{printf "{\"jobName\":\"%s\",\"executionId\":\"%s\",\"status\":\"%s\",\"duration\":%s,\"totalItems\":%s},", $1, $2, $3, $4, $5}' | \
                sed 's/,$//' | sed 's/^/[/' | sed 's/$/]/')
        fi
        
        jq --argjson jobs "${job_executions}" \
           '.performanceMetrics.batchJobExecutions = $jobs' "${SUMMARY_REPORT}" > "${temp_json}"
        mv "${temp_json}" "${SUMMARY_REPORT}"
    fi
    
    log_info "Validation summary report generated: ${SUMMARY_REPORT}"
}

# Determine overall validation status
determine_validation_status() {
    local batch_success="$1"
    local integrity_success="$2"
    local accuracy_success="$3"
    
    local overall_status="FAILED"
    
    if [[ "${batch_success}" == "true" ]] && [[ "${integrity_success}" == "true" ]] && [[ "${accuracy_success}" == "true" ]]; then
        overall_status="PASSED"
        log_info "🎉 Overall validation status: ${overall_status}"
    else
        log_error "❌ Overall validation status: ${overall_status}"
        
        if [[ "${batch_success}" != "true" ]]; then
            log_error "  - Batch job execution failed"
        fi
        
        if [[ "${integrity_success}" != "true" ]]; then
            log_error "  - Data integrity validation failed"
        fi
        
        if [[ "${accuracy_success}" != "true" ]]; then
            log_error "  - Accuracy threshold not met"
        fi
    fi
    
    # Update summary report with final status
    local temp_json=$(mktemp)
    jq --arg status "${overall_status}" \
       '.validationSummary.overallStatus = $status' "${SUMMARY_REPORT}" > "${temp_json}"
    mv "${temp_json}" "${SUMMARY_REPORT}"
    
    echo "${overall_status}"
}

# =============================================================================
# MAIN VALIDATION FUNCTION
# =============================================================================

# Main validation function - orchestrates the entire migration validation process
validate_migration() {
    log_info "Starting CardDemo migration validation process..."
    log_info "Script: ${SCRIPT_NAME} v${SCRIPT_VERSION}"
    
    local start_time=$(date +%s)
    local validation_success=true
    
    # Phase 1: Environment Setup and Validation
    log_info "=== Phase 1: Environment Setup ==="
    init_environment
    check_dependencies
    validate_config_files
    test_database_connection
    start_spring_application
    
    # Phase 2: Batch Job Execution
    log_info "=== Phase 2: Batch Job Execution ==="
    local batch_success=true
    if ! execute_batch_jobs; then
        batch_success=false
        validation_success=false
        log_error "Batch job execution failed"
    fi
    
    # Phase 3: Data Integrity Comparison
    log_info "=== Phase 3: Data Integrity Comparison ==="
    local integrity_success=true
    if ! compare_data_integrity; then
        integrity_success=false
        validation_success=false
        log_error "Data integrity validation failed"
    fi
    
    # Phase 4: Difference Report Generation
    log_info "=== Phase 4: Report Generation ==="
    generate_difference_reports
    
    # Phase 5: Accuracy Calculation
    log_info "=== Phase 5: Accuracy Calculation ==="
    local accuracy_success=true
    if ! calculate_accuracy_percentage; then
        accuracy_success=false
        validation_success=false
        log_error "Migration accuracy below acceptable threshold"
    fi
    
    # Phase 6: Final Summary and Status
    log_info "=== Phase 6: Final Summary ==="
    generate_validation_summary "${start_time}"
    local overall_status=$(determine_validation_status "${batch_success}" "${integrity_success}" "${accuracy_success}")
    
    # Output final results
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    local duration_formatted=$(printf "%02d:%02d:%02d" $((total_duration/3600)) $(((total_duration%3600)/60)) $((total_duration%60)))
    
    echo ""
    echo "============================================================================="
    echo "                      MIGRATION VALIDATION COMPLETE"
    echo "============================================================================="
    echo "Overall Status: ${overall_status}"
    echo "Total Duration: ${duration_formatted}"
    echo "Reports Location: ${REPORTS_DIR}"
    echo "Logs Location: ${LOGS_DIR}"
    echo ""
    echo "Key Reports Generated:"
    echo "  - Validation Summary: ${SUMMARY_REPORT}"
    echo "  - Accuracy Report: ${ACCURACY_REPORT}"
    echo "  - Difference Report: ${DIFFERENCE_REPORT}"
    echo "  - Performance Report: ${PERFORMANCE_REPORT}"
    echo "============================================================================="
    
    if [[ "${overall_status}" == "PASSED" ]]; then
        log_info "✅ Migration validation completed successfully"
        return 0
    else
        log_error "❌ Migration validation failed - please review reports for details"
        return 1
    fi
}

# =============================================================================
# SCRIPT EXECUTION
# =============================================================================

# Main execution block
main() {
    # Handle command line arguments
    case "${1:-validate}" in
        "validate"|"")
            validate_migration
            ;;
        "batch-only")
            init_environment
            check_dependencies
            validate_config_files
            test_database_connection
            start_spring_application
            execute_batch_jobs
            ;;
        "compare-only")
            init_environment
            compare_data_integrity
            generate_difference_reports
            ;;
        "reports-only")
            init_environment
            generate_difference_reports
            calculate_accuracy_percentage
            ;;
        "help"|"-h"|"--help")
            cat << EOF
Usage: ${SCRIPT_NAME} [COMMAND]

COMMANDS:
  validate        Run complete migration validation (default)
  batch-only      Execute only batch job validation
  compare-only    Execute only data integrity comparison
  reports-only    Generate only reports from existing data
  help            Show this help message

ENVIRONMENT VARIABLES:
  DB_HOST         PostgreSQL host (default: localhost)
  DB_PORT         PostgreSQL port (default: 5432)
  DB_NAME         Database name (default: carddemo_validation)
  DB_USER         Database user (default: carddemo_test)
  DB_PASSWORD     Database password (default: testpass)
  DEBUG           Enable debug logging (default: 0)

EXAMPLES:
  ${SCRIPT_NAME}                    # Run full validation
  ${SCRIPT_NAME} batch-only         # Execute batch jobs only
  ${SCRIPT_NAME} compare-only       # Compare data integrity only
  DEBUG=1 ${SCRIPT_NAME} validate   # Run with debug logging

For more information, see the CardDemo migration documentation.
EOF
            ;;
        *)
            log_error "Unknown command: $1"
            echo "Use '${SCRIPT_NAME} help' for usage information."
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"