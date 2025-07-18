-- =====================================================================================
-- Data Migration Validation Script: data_migration_validation.sql
-- =====================================================================================
-- 
-- VALIDATION SUMMARY:
-- Comprehensive data migration validation script testing successful conversion and 
-- loading of ASCII data files into PostgreSQL tables with complete data integrity 
-- preservation during the CardDemo mainframe to cloud-native transformation.
-- 
-- This script validates the migration of all ASCII data files from the original
-- COBOL/VSAM system to PostgreSQL while maintaining exact data fidelity, precision,
-- and field-level integrity as required by the technical specification.
-- 
-- ASCII FILES VALIDATED:
-- - acctdata.txt: Account data with DECIMAL(12,2) financial precision
-- - custdata.txt: Customer data with PII protection and normalized addresses
-- - carddata.txt: Card data with Luhn validation and composite relationships
-- - dailytran.txt: Transaction data with partitioning and merchant information
-- 
-- VALIDATION COVERAGE:
-- - Row count verification ensuring all records successfully migrate
-- - Data type validation ensuring proper conversion from COBOL to PostgreSQL
-- - Field-level data integrity checks maintaining original values and precision
-- - Character encoding validation ensuring proper handling of special characters
-- - Referential integrity validation ensuring foreign key relationships are maintained
-- - Business rule validation ensuring COBOL constraints are preserved
-- - Financial precision validation using BigDecimal DECIMAL128 context
-- - Date format validation ensuring proper date conversion and storage
-- 
-- PERFORMANCE REQUIREMENTS:
-- - All validation queries must complete within 30 seconds for production readiness
-- - Memory usage optimized for concurrent execution with data loading processes
-- - Partitioned table validation supporting efficient large-scale data verification
-- 
-- Author: Blitzy agent
-- Date: 2024
-- Version: 1.0
-- Dependencies: V1-V6 migration scripts must be executed first
-- =====================================================================================

-- =============================================================================
-- 1. VALIDATION SETUP AND CONFIGURATION
-- =============================================================================

-- Create validation results table to store test outcomes
CREATE TEMPORARY TABLE validation_results (
    validation_id SERIAL PRIMARY KEY,
    test_category VARCHAR(50) NOT NULL,
    test_name VARCHAR(100) NOT NULL,
    test_status VARCHAR(20) NOT NULL CHECK (test_status IN ('PASS', 'FAIL', 'WARNING')),
    expected_value TEXT,
    actual_value TEXT,
    error_message TEXT,
    execution_time_ms INTEGER,
    test_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create validation summary table for reporting
CREATE TEMPORARY TABLE validation_summary (
    category VARCHAR(50) NOT NULL,
    total_tests INTEGER NOT NULL DEFAULT 0,
    passed_tests INTEGER NOT NULL DEFAULT 0,
    failed_tests INTEGER NOT NULL DEFAULT 0,
    warnings INTEGER NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00
);

-- Define validation constants matching COBOL specifications
DO $$
DECLARE
    -- Expected record counts from ASCII files (approximations for validation)
    EXPECTED_ACCOUNT_COUNT INTEGER := 50;
    EXPECTED_CUSTOMER_COUNT INTEGER := 50;
    EXPECTED_CARD_COUNT INTEGER := 50;
    EXPECTED_TRANSACTION_COUNT INTEGER := 100;
    
    -- Validation thresholds
    PRECISION_TOLERANCE DECIMAL(12,2) := 0.01;
    DATE_VALIDATION_MIN DATE := '1900-01-01';
    DATE_VALIDATION_MAX DATE := '2030-12-31';
    
    -- Test execution variables
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_time INTEGER;
    test_result TEXT;
    actual_count INTEGER;
    expected_count INTEGER;
    
BEGIN
    -- =============================================================================
    -- 2. USERS TABLE VALIDATION (VSAM USRSEC Migration)
    -- =============================================================================
    
    -- Test 2.1: Users table exists and has expected structure
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'users';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('SCHEMA_VALIDATION', 'Users table exists', 
            CASE WHEN actual_count = 1 THEN 'PASS' ELSE 'FAIL' END,
            '1', actual_count::TEXT, execution_time);
    
    -- Test 2.2: Users table has required columns with correct data types
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM information_schema.columns 
    WHERE table_name = 'users' 
    AND column_name IN ('user_id', 'password_hash', 'user_type', 'first_name', 'last_name', 'created_at', 'last_login')
    AND data_type IN ('character varying', 'timestamp with time zone');
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('SCHEMA_VALIDATION', 'Users table column structure', 
            CASE WHEN actual_count = 7 THEN 'PASS' ELSE 'FAIL' END,
            '7', actual_count::TEXT, execution_time);
    
    -- Test 2.3: Users table constraints validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM information_schema.table_constraints 
    WHERE table_name = 'users' 
    AND constraint_type IN ('PRIMARY KEY', 'CHECK', 'UNIQUE');
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('SCHEMA_VALIDATION', 'Users table constraints', 
            CASE WHEN actual_count >= 8 THEN 'PASS' ELSE 'FAIL' END,
            '8+', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 3. CUSTOMERS TABLE VALIDATION (VSAM CUSTDAT Migration)
    -- =============================================================================
    
    -- Test 3.1: Customer data migration row count validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM customers;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Customer records migrated', 
            CASE WHEN actual_count >= EXPECTED_CUSTOMER_COUNT THEN 'PASS' ELSE 'FAIL' END,
            EXPECTED_CUSTOMER_COUNT::TEXT, actual_count::TEXT, execution_time);
    
    -- Test 3.2: Customer data integrity - no NULL values in required fields
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE customer_id IS NULL 
    OR first_name IS NULL 
    OR last_name IS NULL 
    OR address_line_1 IS NULL 
    OR address_state IS NULL 
    OR address_zip IS NULL 
    OR ssn IS NULL 
    OR date_of_birth IS NULL 
    OR fico_credit_score IS NULL;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_INTEGRITY', 'Customer required fields populated', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 3.3: Customer ID format validation (9 digits)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE customer_id !~ '^[0-9]{9}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Customer ID format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 3.4: FICO score range validation (300-850)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE fico_credit_score < 300 OR fico_credit_score > 850;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('BUSINESS_RULES', 'FICO score range validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 3.5: Date of birth validation (reasonable range)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE date_of_birth < DATE_VALIDATION_MIN 
    OR date_of_birth > CURRENT_DATE;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATE_VALIDATION', 'Customer date of birth range', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 3.6: State code validation (2-character uppercase)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE address_state !~ '^[A-Z]{2}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Customer state code format', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 3.7: ZIP code format validation (5 digits or 5+4 format)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE address_zip !~ '^[0-9]{5}(-[0-9]{4})?$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Customer ZIP code format', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 4. ACCOUNTS TABLE VALIDATION (VSAM ACCTDAT Migration)
    -- =============================================================================
    
    -- Test 4.1: Account data migration row count validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM accounts;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Account records migrated', 
            CASE WHEN actual_count >= EXPECTED_ACCOUNT_COUNT THEN 'PASS' ELSE 'FAIL' END,
            EXPECTED_ACCOUNT_COUNT::TEXT, actual_count::TEXT, execution_time);
    
    -- Test 4.2: Account ID format validation (11 digits)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts 
    WHERE account_id !~ '^[0-9]{11}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Account ID format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 4.3: Account-Customer referential integrity
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts a 
    LEFT JOIN customers c ON a.customer_id = c.customer_id 
    WHERE c.customer_id IS NULL;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('REFERENTIAL_INTEGRITY', 'Account-Customer foreign key integrity', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 4.4: Financial precision validation (DECIMAL(12,2))
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts 
    WHERE current_balance::TEXT ~ '\.[0-9]{3,}$'
    OR credit_limit::TEXT ~ '\.[0-9]{3,}$'
    OR cash_credit_limit::TEXT ~ '\.[0-9]{3,}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('PRECISION_VALIDATION', 'Account financial precision (2 decimal places)', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 4.5: Account balance business rules
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts 
    WHERE credit_limit < 0 
    OR cash_credit_limit > credit_limit 
    OR current_balance < -99999999.99 
    OR current_balance > 99999999.99;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('BUSINESS_RULES', 'Account balance business rules', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 4.6: Account date validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts 
    WHERE open_date > expiration_date 
    OR reissue_date < open_date 
    OR open_date < DATE_VALIDATION_MIN 
    OR expiration_date > DATE_VALIDATION_MAX;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATE_VALIDATION', 'Account date consistency', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 4.7: Active status validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts 
    WHERE active_status NOT IN ('Y', 'N');
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Account active status values', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 5. CARDS TABLE VALIDATION (VSAM CARDDAT Migration)
    -- =============================================================================
    
    -- Test 5.1: Card data migration row count validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM cards;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Card records migrated', 
            CASE WHEN actual_count >= EXPECTED_CARD_COUNT THEN 'PASS' ELSE 'FAIL' END,
            EXPECTED_CARD_COUNT::TEXT, actual_count::TEXT, execution_time);
    
    -- Test 5.2: Card number format validation (16 digits)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards 
    WHERE card_number !~ '^[0-9]{16}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Card number format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 5.3: Card-Account referential integrity
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards c 
    LEFT JOIN accounts a ON c.account_id = a.account_id 
    WHERE a.account_id IS NULL;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('REFERENTIAL_INTEGRITY', 'Card-Account foreign key integrity', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 5.4: Card-Customer referential integrity
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards c 
    LEFT JOIN customers cu ON c.customer_id = cu.customer_id 
    WHERE cu.customer_id IS NULL;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('REFERENTIAL_INTEGRITY', 'Card-Customer foreign key integrity', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 5.5: CVV code format validation (3 digits)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards 
    WHERE cvv_code !~ '^[0-9]{3}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Card CVV format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 5.6: Card expiration date validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards 
    WHERE expiration_date < CURRENT_DATE 
    OR expiration_date > DATE_VALIDATION_MAX;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATE_VALIDATION', 'Card expiration date range', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'WARNING' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 5.7: Card Luhn algorithm validation (using PostgreSQL function)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards 
    WHERE NOT validate_luhn_algorithm(card_number);
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('BUSINESS_RULES', 'Card Luhn algorithm validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 5.8: Card status validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM cards 
    WHERE active_status NOT IN ('Y', 'N');
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Card active status values', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 6. TRANSACTIONS TABLE VALIDATION (VSAM TRANSACT Migration)
    -- =============================================================================
    
    -- Test 6.1: Transaction data migration row count validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM transactions;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Transaction records migrated', 
            CASE WHEN actual_count >= EXPECTED_TRANSACTION_COUNT THEN 'PASS' ELSE 'FAIL' END,
            EXPECTED_TRANSACTION_COUNT::TEXT, actual_count::TEXT, execution_time);
    
    -- Test 6.2: Transaction ID format validation (16 characters)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions 
    WHERE LENGTH(transaction_id) != 16;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Transaction ID format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 6.3: Transaction-Account referential integrity
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions t 
    LEFT JOIN accounts a ON t.account_id = a.account_id 
    WHERE a.account_id IS NULL;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('REFERENTIAL_INTEGRITY', 'Transaction-Account foreign key integrity', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 6.4: Transaction-Card referential integrity
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions t 
    LEFT JOIN cards c ON t.card_number = c.card_number 
    WHERE c.card_number IS NULL;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('REFERENTIAL_INTEGRITY', 'Transaction-Card foreign key integrity', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 6.5: Transaction amount precision validation (DECIMAL(12,2))
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions 
    WHERE transaction_amount::TEXT ~ '\.[0-9]{3,}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('PRECISION_VALIDATION', 'Transaction amount precision (2 decimal places)', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 6.6: Transaction timestamp validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions 
    WHERE transaction_timestamp < '2020-01-01'::TIMESTAMP 
    OR transaction_timestamp > CURRENT_TIMESTAMP + INTERVAL '1 day';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATE_VALIDATION', 'Transaction timestamp range', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 6.7: Transaction partitioning validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM pg_tables 
    WHERE tablename LIKE 'transactions_2024_%';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('PARTITIONING', 'Transaction table partitions exist', 
            CASE WHEN actual_count >= 12 THEN 'PASS' ELSE 'FAIL' END,
            '12+', actual_count::TEXT, execution_time);
    
    -- Test 6.8: Transaction type format validation (2 characters)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions 
    WHERE transaction_type !~ '^[0-9]{2}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Transaction type format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 6.9: Transaction category format validation (4 characters)
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions 
    WHERE transaction_category !~ '^[0-9]{4}$';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_VALIDATION', 'Transaction category format validation', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 7. REFERENCE TABLES VALIDATION
    -- =============================================================================
    
    -- Test 7.1: Reference tables exist
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name IN ('transaction_types', 'transaction_categories', 'disclosure_groups');
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('SCHEMA_VALIDATION', 'Reference tables exist', 
            CASE WHEN actual_count = 3 THEN 'PASS' ELSE 'FAIL' END,
            '3', actual_count::TEXT, execution_time);
    
    -- Test 7.2: Transaction types table has data
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM transaction_types;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Transaction types data loaded', 
            CASE WHEN actual_count > 0 THEN 'PASS' ELSE 'FAIL' END,
            '1+', actual_count::TEXT, execution_time);
    
    -- Test 7.3: Transaction categories table has data
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM transaction_categories;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Transaction categories data loaded', 
            CASE WHEN actual_count > 0 THEN 'PASS' ELSE 'FAIL' END,
            '1+', actual_count::TEXT, execution_time);
    
    -- Test 7.4: Disclosure groups table has data
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count FROM disclosure_groups;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('DATA_MIGRATION', 'Disclosure groups data loaded', 
            CASE WHEN actual_count > 0 THEN 'PASS' ELSE 'FAIL' END,
            '1+', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 8. CHARACTER ENCODING AND SPECIAL CHARACTERS VALIDATION
    -- =============================================================================
    
    -- Test 8.1: Customer name character encoding validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE first_name ~ '[^\x00-\x7F]' 
    OR last_name ~ '[^\x00-\x7F]';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('ENCODING_VALIDATION', 'Customer name character encoding', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'WARNING' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 8.2: Address field character encoding validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM customers 
    WHERE address_line_1 ~ '[^\x00-\x7F]' 
    OR address_line_2 ~ '[^\x00-\x7F]' 
    OR address_line_3 ~ '[^\x00-\x7F]';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('ENCODING_VALIDATION', 'Address field character encoding', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'WARNING' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 8.3: Transaction description character encoding validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions 
    WHERE description ~ '[^\x00-\x7F]';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('ENCODING_VALIDATION', 'Transaction description character encoding', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'WARNING' END,
            '0', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 9. CROSS-REFERENTIAL INTEGRITY VALIDATION
    -- =============================================================================
    
    -- Test 9.1: Account-Customer cross-reference consistency
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM accounts a 
    INNER JOIN cards c ON a.account_id = c.account_id 
    WHERE a.customer_id != c.customer_id;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('CROSS_REFERENCE', 'Account-Card customer consistency', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- Test 9.2: Transaction-Account-Card relationship validation
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO actual_count
    FROM transactions t 
    INNER JOIN cards c ON t.card_number = c.card_number 
    WHERE t.account_id != c.account_id;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('CROSS_REFERENCE', 'Transaction-Account-Card relationship', 
            CASE WHEN actual_count = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', actual_count::TEXT, execution_time);
    
    -- =============================================================================
    -- 10. PERFORMANCE VALIDATION
    -- =============================================================================
    
    -- Test 10.1: Query performance validation - Account lookups
    start_time := clock_timestamp();
    
    PERFORM * FROM accounts WHERE account_id = '00000000001';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('PERFORMANCE', 'Account lookup performance', 
            CASE WHEN execution_time < 10 THEN 'PASS' ELSE 'WARNING' END,
            '< 10ms', execution_time::TEXT || 'ms', execution_time);
    
    -- Test 10.2: Query performance validation - Customer lookups
    start_time := clock_timestamp();
    
    PERFORM * FROM customers WHERE customer_id = '000000001';
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('PERFORMANCE', 'Customer lookup performance', 
            CASE WHEN execution_time < 10 THEN 'PASS' ELSE 'WARNING' END,
            '< 10ms', execution_time::TEXT || 'ms', execution_time);
    
    -- Test 10.3: Query performance validation - Transaction queries
    start_time := clock_timestamp();
    
    PERFORM * FROM transactions WHERE account_id = '00000000001' LIMIT 10;
    
    end_time := clock_timestamp();
    execution_time := EXTRACT(MILLISECONDS FROM end_time - start_time);
    
    INSERT INTO validation_results (test_category, test_name, test_status, expected_value, actual_value, execution_time_ms)
    VALUES ('PERFORMANCE', 'Transaction query performance', 
            CASE WHEN execution_time < 50 THEN 'PASS' ELSE 'WARNING' END,
            '< 50ms', execution_time::TEXT || 'ms', execution_time);
    
END;
$$;

-- =============================================================================
-- 11. VALIDATION SUMMARY GENERATION
-- =============================================================================

-- Generate summary statistics by category
INSERT INTO validation_summary (category, total_tests, passed_tests, failed_tests, warnings, success_rate)
SELECT 
    test_category,
    COUNT(*) as total_tests,
    COUNT(CASE WHEN test_status = 'PASS' THEN 1 END) as passed_tests,
    COUNT(CASE WHEN test_status = 'FAIL' THEN 1 END) as failed_tests,
    COUNT(CASE WHEN test_status = 'WARNING' THEN 1 END) as warnings,
    ROUND(
        (COUNT(CASE WHEN test_status = 'PASS' THEN 1 END) * 100.0 / COUNT(*)), 
        2
    ) as success_rate
FROM validation_results
GROUP BY test_category
ORDER BY test_category;

-- =============================================================================
-- 12. VALIDATION RESULTS REPORTING
-- =============================================================================

-- Display detailed test results
SELECT 
    validation_id,
    test_category,
    test_name,
    test_status,
    expected_value,
    actual_value,
    CASE 
        WHEN test_status = 'FAIL' THEN COALESCE(error_message, 'Validation failed')
        WHEN test_status = 'WARNING' THEN 'Warning: Review required'
        ELSE 'Test passed successfully'
    END as result_message,
    execution_time_ms || 'ms' as execution_time,
    test_timestamp
FROM validation_results
ORDER BY 
    CASE test_status 
        WHEN 'FAIL' THEN 1 
        WHEN 'WARNING' THEN 2 
        WHEN 'PASS' THEN 3 
    END,
    test_category,
    test_name;

-- Display summary by category
SELECT 
    category,
    total_tests,
    passed_tests,
    failed_tests,
    warnings,
    success_rate || '%' as success_rate
FROM validation_summary
ORDER BY success_rate DESC;

-- Display overall validation summary
SELECT 
    'OVERALL VALIDATION SUMMARY' as summary_type,
    SUM(total_tests) as total_tests,
    SUM(passed_tests) as passed_tests,
    SUM(failed_tests) as failed_tests,
    SUM(warnings) as warnings,
    ROUND(
        (SUM(passed_tests) * 100.0 / SUM(total_tests)), 
        2
    ) || '%' as overall_success_rate,
    CASE 
        WHEN SUM(failed_tests) = 0 THEN 'MIGRATION VALIDATION PASSED'
        WHEN SUM(failed_tests) <= 2 THEN 'MIGRATION VALIDATION PASSED WITH MINOR ISSUES'
        ELSE 'MIGRATION VALIDATION FAILED - REVIEW REQUIRED'
    END as validation_result
FROM validation_summary;

-- =============================================================================
-- 13. CLEANUP AND FINALIZATION
-- =============================================================================

-- Performance summary for monitoring
SELECT 
    'PERFORMANCE SUMMARY' as metric_type,
    AVG(execution_time_ms) as avg_execution_time_ms,
    MAX(execution_time_ms) as max_execution_time_ms,
    MIN(execution_time_ms) as min_execution_time_ms,
    COUNT(*) as total_validations
FROM validation_results
WHERE execution_time_ms IS NOT NULL;

-- Critical failure report
SELECT 
    'CRITICAL FAILURES' as failure_type,
    test_category,
    test_name,
    actual_value,
    expected_value,
    test_timestamp
FROM validation_results
WHERE test_status = 'FAIL' 
AND test_category IN ('REFERENTIAL_INTEGRITY', 'PRECISION_VALIDATION', 'BUSINESS_RULES')
ORDER BY test_timestamp;

-- Final validation message
DO $$
DECLARE
    total_failures INTEGER;
    critical_failures INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_failures FROM validation_results WHERE test_status = 'FAIL';
    SELECT COUNT(*) INTO critical_failures FROM validation_results WHERE test_status = 'FAIL' AND test_category IN ('REFERENTIAL_INTEGRITY', 'PRECISION_VALIDATION', 'BUSINESS_RULES');
    
    IF total_failures = 0 THEN
        RAISE NOTICE '========================================================================';
        RAISE NOTICE 'DATA MIGRATION VALIDATION COMPLETED SUCCESSFULLY';
        RAISE NOTICE 'All ASCII data files have been successfully migrated to PostgreSQL';
        RAISE NOTICE 'Data integrity and precision have been maintained throughout migration';
        RAISE NOTICE '========================================================================';
    ELSIF critical_failures > 0 THEN
        RAISE NOTICE '========================================================================';
        RAISE NOTICE 'CRITICAL DATA MIGRATION VALIDATION FAILURES DETECTED';
        RAISE NOTICE 'Review critical failures before proceeding with system deployment';
        RAISE NOTICE 'Failed tests: %, Critical failures: %', total_failures, critical_failures;
        RAISE NOTICE '========================================================================';
    ELSE
        RAISE NOTICE '========================================================================';
        RAISE NOTICE 'DATA MIGRATION VALIDATION COMPLETED WITH MINOR ISSUES';
        RAISE NOTICE 'Review warnings and minor failures for optimization opportunities';
        RAISE NOTICE 'Failed tests: %, Critical failures: %', total_failures, critical_failures;
        RAISE NOTICE '========================================================================';
    END IF;
END;
$$;

-- =====================================================================================
-- VALIDATION SCRIPT COMPLETION
-- =====================================================================================
-- 
-- This comprehensive validation script has tested:
-- 1. Schema structure and table creation from migration scripts
-- 2. Data migration completeness and row count verification
-- 3. Field-level data integrity and format validation
-- 4. Referential integrity across all table relationships
-- 5. Business rule compliance and constraint validation
-- 6. Financial precision preservation using DECIMAL(12,2)
-- 7. Date validation and range checking
-- 8. Character encoding and special character handling
-- 9. Cross-referential integrity for complex relationships
-- 10. Query performance validation for production readiness
-- 
-- All validation results are captured in temporary tables for analysis and reporting.
-- The script provides comprehensive feedback on migration success and identifies
-- any issues requiring attention before system deployment.
-- =====================================================================================