-- ==============================================================================
-- PostgreSQL Schema Structure Validation Script
-- ==============================================================================
-- Description: Comprehensive schema validation ensuring PostgreSQL table definitions
--              match COBOL copybook specifications with exact precision for financial
--              data types and complete constraint validation
-- 
-- Purpose: Validates the CardDemo modernization database migration from VSAM datasets
--          to PostgreSQL tables with precise COBOL-to-PostgreSQL data type mapping
--
-- Author: Blitzy agent
-- Version: 1.0
-- Migration Validation for: V1-V7 Liquibase migrations
-- ==============================================================================

-- Set validation parameters and error handling
\set ON_ERROR_STOP on
\set QUIET on

-- Create validation results schema if not exists
CREATE SCHEMA IF NOT EXISTS validation_results;

-- ==============================================================================
-- SCHEMA STRUCTURE VALIDATION RESULTS TABLE
-- ==============================================================================

-- Drop and recreate validation results table for fresh validation run
DROP TABLE IF EXISTS validation_results.schema_validation_summary CASCADE;

CREATE TABLE validation_results.schema_validation_summary (
    validation_id SERIAL PRIMARY KEY,
    validation_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    table_name VARCHAR(50) NOT NULL,
    validation_category VARCHAR(50) NOT NULL,
    validation_description TEXT NOT NULL,
    expected_value TEXT,
    actual_value TEXT,
    validation_status VARCHAR(10) NOT NULL CHECK (validation_status IN ('PASS', 'FAIL', 'WARNING')),
    error_message TEXT,
    copybook_reference VARCHAR(100)
);

-- ==============================================================================
-- HELPER FUNCTIONS FOR VALIDATION
-- ==============================================================================

-- Function to log validation results
CREATE OR REPLACE FUNCTION validation_results.log_validation(
    p_table_name VARCHAR(50),
    p_category VARCHAR(50),
    p_description TEXT,
    p_expected TEXT DEFAULT NULL,
    p_actual TEXT DEFAULT NULL,
    p_status VARCHAR(10) DEFAULT 'PASS',
    p_error TEXT DEFAULT NULL,
    p_copybook TEXT DEFAULT NULL
) RETURNS VOID AS $$
BEGIN
    INSERT INTO validation_results.schema_validation_summary (
        table_name, validation_category, validation_description,
        expected_value, actual_value, validation_status, error_message, copybook_reference
    ) VALUES (
        p_table_name, p_category, p_description,
        p_expected, p_actual, p_status, p_error, p_copybook
    );
END;
$$ LANGUAGE plpgsql;

-- Function to check if table exists
CREATE OR REPLACE FUNCTION validation_results.table_exists(p_table_name VARCHAR(50)) 
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = p_table_name AND table_schema = 'public'
    );
END;
$$ LANGUAGE plpgsql;

-- Function to validate column exists with specific data type
CREATE OR REPLACE FUNCTION validation_results.validate_column_type(
    p_table_name VARCHAR(50),
    p_column_name VARCHAR(50),
    p_expected_type VARCHAR(100),
    p_copybook_ref VARCHAR(100) DEFAULT NULL
) RETURNS VOID AS $$
DECLARE
    v_actual_type VARCHAR(100);
    v_column_exists BOOLEAN;
BEGIN
    -- Check if column exists and get its type
    SELECT data_type || 
           CASE 
               WHEN character_maximum_length IS NOT NULL THEN '(' || character_maximum_length || ')'
               WHEN numeric_precision IS NOT NULL AND numeric_scale IS NOT NULL THEN '(' || numeric_precision || ',' || numeric_scale || ')'
               WHEN numeric_precision IS NOT NULL THEN '(' || numeric_precision || ')'
               ELSE ''
           END,
           TRUE
    INTO v_actual_type, v_column_exists
    FROM information_schema.columns
    WHERE table_name = p_table_name 
      AND column_name = p_column_name
      AND table_schema = 'public';
    
    IF NOT FOUND THEN
        PERFORM validation_results.log_validation(
            p_table_name, 'COLUMN_TYPE', 
            'Column ' || p_column_name || ' existence check',
            'Column should exist', 'Column does not exist', 
            'FAIL', 'Column not found in table', p_copybook_ref
        );
        RETURN;
    END IF;
    
    -- Normalize type comparison (handle PostgreSQL type variations)
    v_actual_type := REPLACE(REPLACE(v_actual_type, 'character varying', 'varchar'), 'timestamp with time zone', 'timestamptz');
    p_expected_type := REPLACE(REPLACE(p_expected_type, 'character varying', 'varchar'), 'timestamp with time zone', 'timestamptz');
    
    IF LOWER(v_actual_type) = LOWER(p_expected_type) THEN
        PERFORM validation_results.log_validation(
            p_table_name, 'COLUMN_TYPE', 
            'Column ' || p_column_name || ' data type validation',
            p_expected_type, v_actual_type, 'PASS', NULL, p_copybook_ref
        );
    ELSE
        PERFORM validation_results.log_validation(
            p_table_name, 'COLUMN_TYPE', 
            'Column ' || p_column_name || ' data type validation',
            p_expected_type, v_actual_type, 'FAIL', 
            'Data type mismatch', p_copybook_ref
        );
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- 1. USERS TABLE VALIDATION (VSAM USRSEC Dataset)
-- ==============================================================================

DO $$
BEGIN
    -- Table existence check
    IF validation_results.table_exists('users') THEN
        PERFORM validation_results.log_validation(
            'users', 'TABLE_EXISTENCE', 'Users table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'VSAM USRSEC'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'users', 'TABLE_EXISTENCE', 'Users table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Users table missing from schema', 'VSAM USRSEC'
        );
        RETURN;
    END IF;
    
    -- Column structure validation for VSAM USRSEC (8-byte key, 80-byte record)
    PERFORM validation_results.validate_column_type('users', 'user_id', 'varchar(8)', 'VSAM USRSEC KEY (8 bytes)');
    PERFORM validation_results.validate_column_type('users', 'password_hash', 'varchar(60)', 'BCrypt password hash');
    PERFORM validation_results.validate_column_type('users', 'user_type', 'varchar(1)', 'VSAM USRSEC user type flag');
    PERFORM validation_results.validate_column_type('users', 'first_name', 'varchar(20)', 'VSAM USRSEC first name field');
    PERFORM validation_results.validate_column_type('users', 'last_name', 'varchar(20)', 'VSAM USRSEC last name field');
    PERFORM validation_results.validate_column_type('users', 'created_at', 'timestamp without time zone', 'Audit timestamp');
    PERFORM validation_results.validate_column_type('users', 'last_login', 'timestamp without time zone', 'Session tracking');
    
    -- Primary key validation
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_type = 'PRIMARY KEY'
          AND constraint_name = 'pk_users'
    ) THEN
        PERFORM validation_results.log_validation(
            'users', 'PRIMARY_KEY', 'Primary key constraint validation',
            'pk_users constraint exists', 'pk_users exists', 'PASS', NULL, 'VSAM USRSEC'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'users', 'PRIMARY_KEY', 'Primary key constraint validation',
            'pk_users constraint exists', 'pk_users missing', 'FAIL',
            'Primary key constraint not found', 'VSAM USRSEC'
        );
    END IF;
    
    -- Check constraint validation for user_type
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'chk_users_user_type'
    ) THEN
        PERFORM validation_results.log_validation(
            'users', 'CHECK_CONSTRAINT', 'User type check constraint validation',
            'chk_users_user_type exists', 'chk_users_user_type exists', 'PASS', NULL, 'VSAM USRSEC'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'users', 'CHECK_CONSTRAINT', 'User type check constraint validation',
            'chk_users_user_type exists', 'chk_users_user_type missing', 'FAIL',
            'User type check constraint missing', 'VSAM USRSEC'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 2. CUSTOMERS TABLE VALIDATION (CVCUS01Y.cpy - 500 byte CUSTOMER-RECORD)
-- ==============================================================================

DO $$
BEGIN
    -- Table existence check
    IF validation_results.table_exists('customers') THEN
        PERFORM validation_results.log_validation(
            'customers', 'TABLE_EXISTENCE', 'Customers table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'CVCUS01Y.cpy'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'customers', 'TABLE_EXISTENCE', 'Customers table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Customers table missing from schema', 'CVCUS01Y.cpy'
        );
        RETURN;
    END IF;
    
    -- Column structure validation matching COBOL CUSTOMER-RECORD layout
    PERFORM validation_results.validate_column_type('customers', 'customer_id', 'varchar(9)', 'CVCUS01Y.cpy CUST-ID PIC 9(09)');
    PERFORM validation_results.validate_column_type('customers', 'first_name', 'varchar(20)', 'CVCUS01Y.cpy CUST-FIRST-NAME PIC X(25)');
    PERFORM validation_results.validate_column_type('customers', 'middle_name', 'varchar(20)', 'CVCUS01Y.cpy CUST-MIDDLE-NAME PIC X(25)');
    PERFORM validation_results.validate_column_type('customers', 'last_name', 'varchar(20)', 'CVCUS01Y.cpy CUST-LAST-NAME PIC X(25)');
    PERFORM validation_results.validate_column_type('customers', 'address_line_1', 'varchar(50)', 'CVCUS01Y.cpy CUST-ADDR-LINE-1 PIC X(50)');
    PERFORM validation_results.validate_column_type('customers', 'address_line_2', 'varchar(50)', 'CVCUS01Y.cpy CUST-ADDR-LINE-2 PIC X(50)');
    PERFORM validation_results.validate_column_type('customers', 'address_line_3', 'varchar(50)', 'CVCUS01Y.cpy CUST-ADDR-LINE-3 PIC X(50)');
    PERFORM validation_results.validate_column_type('customers', 'state_code', 'varchar(2)', 'CVCUS01Y.cpy CUST-ADDR-STATE-CD PIC X(02)');
    PERFORM validation_results.validate_column_type('customers', 'country_code', 'varchar(3)', 'CVCUS01Y.cpy CUST-ADDR-COUNTRY-CD PIC X(03)');
    PERFORM validation_results.validate_column_type('customers', 'zip_code', 'varchar(10)', 'CVCUS01Y.cpy CUST-ADDR-ZIP PIC X(10)');
    PERFORM validation_results.validate_column_type('customers', 'phone_number_1', 'varchar(15)', 'CVCUS01Y.cpy CUST-PHONE-NUM-1 PIC X(15)');
    PERFORM validation_results.validate_column_type('customers', 'phone_number_2', 'varchar(15)', 'CVCUS01Y.cpy CUST-PHONE-NUM-2 PIC X(15)');
    PERFORM validation_results.validate_column_type('customers', 'ssn', 'varchar(9)', 'CVCUS01Y.cpy CUST-SSN PIC 9(09)');
    PERFORM validation_results.validate_column_type('customers', 'government_id', 'varchar(20)', 'CVCUS01Y.cpy CUST-GOVT-ISSUED-ID PIC X(20)');
    PERFORM validation_results.validate_column_type('customers', 'date_of_birth', 'date', 'CVCUS01Y.cpy CUST-DOB-YYYY-MM-DD PIC X(10)');
    PERFORM validation_results.validate_column_type('customers', 'eft_account_id', 'varchar(10)', 'CVCUS01Y.cpy CUST-EFT-ACCOUNT-ID PIC X(10)');
    PERFORM validation_results.validate_column_type('customers', 'primary_cardholder_indicator', 'varchar(1)', 'CVCUS01Y.cpy CUST-PRI-CARD-HOLDER-IND PIC X(01)');
    PERFORM validation_results.validate_column_type('customers', 'fico_credit_score', 'numeric(3)', 'CVCUS01Y.cpy CUST-FICO-CREDIT-SCORE PIC 9(03)');
    
    -- Primary key validation
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'customers' AND constraint_type = 'PRIMARY KEY'
          AND constraint_name = 'pk_customers'
    ) THEN
        PERFORM validation_results.log_validation(
            'customers', 'PRIMARY_KEY', 'Primary key constraint validation',
            'pk_customers constraint exists', 'pk_customers exists', 'PASS', NULL, 'CVCUS01Y.cpy'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'customers', 'PRIMARY_KEY', 'Primary key constraint validation',
            'pk_customers constraint exists', 'pk_customers missing', 'FAIL',
            'Primary key constraint not found', 'CVCUS01Y.cpy'
        );
    END IF;
    
    -- FICO score range validation
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'chk_fico_score_range'
    ) THEN
        PERFORM validation_results.log_validation(
            'customers', 'CHECK_CONSTRAINT', 'FICO score range validation',
            'chk_fico_score_range exists', 'chk_fico_score_range exists', 'PASS', NULL, 'CVCUS01Y.cpy'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'customers', 'CHECK_CONSTRAINT', 'FICO score range validation',
            'chk_fico_score_range exists', 'chk_fico_score_range missing', 'FAIL',
            'FICO score constraint missing', 'CVCUS01Y.cpy'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 3. ACCOUNTS TABLE VALIDATION (CVACT01Y.cpy - 300 byte ACCOUNT-RECORD)
-- ==============================================================================

DO $$
BEGIN
    -- Table existence check
    IF validation_results.table_exists('accounts') THEN
        PERFORM validation_results.log_validation(
            'accounts', 'TABLE_EXISTENCE', 'Accounts table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'CVACT01Y.cpy'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'accounts', 'TABLE_EXISTENCE', 'Accounts table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Accounts table missing from schema', 'CVACT01Y.cpy'
        );
        RETURN;
    END IF;
    
    -- Critical COBOL COMP-3 to DECIMAL precision validation
    -- CVACT01Y.cpy PIC S9(10)V99 fields must map to DECIMAL(12,2)
    PERFORM validation_results.validate_column_type('accounts', 'account_id', 'varchar(11)', 'CVACT01Y.cpy ACCT-ID PIC 9(11)');
    PERFORM validation_results.validate_column_type('accounts', 'customer_id', 'varchar(9)', 'Foreign key to customers');
    PERFORM validation_results.validate_column_type('accounts', 'active_status', 'boolean', 'CVACT01Y.cpy ACCT-ACTIVE-STATUS PIC X(01)');
    
    -- CRITICAL: Financial precision validation - COBOL COMP-3 to PostgreSQL DECIMAL mapping
    PERFORM validation_results.validate_column_type('accounts', 'current_balance', 'numeric(12,2)', 'CVACT01Y.cpy ACCT-CURR-BAL PIC S9(10)V99');
    PERFORM validation_results.validate_column_type('accounts', 'credit_limit', 'numeric(12,2)', 'CVACT01Y.cpy ACCT-CREDIT-LIMIT PIC S9(10)V99');
    PERFORM validation_results.validate_column_type('accounts', 'cash_credit_limit', 'numeric(12,2)', 'CVACT01Y.cpy ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99');
    PERFORM validation_results.validate_column_type('accounts', 'current_cycle_credit', 'numeric(12,2)', 'CVACT01Y.cpy ACCT-CURR-CYC-CREDIT PIC S9(10)V99');
    PERFORM validation_results.validate_column_type('accounts', 'current_cycle_debit', 'numeric(12,2)', 'CVACT01Y.cpy ACCT-CURR-CYC-DEBIT PIC S9(10)V99');
    
    PERFORM validation_results.validate_column_type('accounts', 'open_date', 'date', 'CVACT01Y.cpy ACCT-OPEN-DATE PIC X(10)');
    PERFORM validation_results.validate_column_type('accounts', 'expiration_date', 'date', 'CVACT01Y.cpy ACCT-EXPIRAION-DATE PIC X(10)');
    PERFORM validation_results.validate_column_type('accounts', 'reissue_date', 'date', 'CVACT01Y.cpy ACCT-REISSUE-DATE PIC X(10)');
    PERFORM validation_results.validate_column_type('accounts', 'address_zip', 'varchar(10)', 'CVACT01Y.cpy ACCT-ADDR-ZIP PIC X(10)');
    PERFORM validation_results.validate_column_type('accounts', 'group_id', 'varchar(10)', 'CVACT01Y.cpy ACCT-GROUP-ID PIC X(10)');
    
    -- Foreign key constraints validation
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'accounts' AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'customer_id'
    ) THEN
        PERFORM validation_results.log_validation(
            'accounts', 'FOREIGN_KEY', 'Customer foreign key constraint validation',
            'fk_accounts_customer exists', 'Foreign key exists', 'PASS', NULL, 'CVACT01Y.cpy'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'accounts', 'FOREIGN_KEY', 'Customer foreign key constraint validation',
            'fk_accounts_customer exists', 'Foreign key missing', 'FAIL',
            'Customer foreign key constraint not found', 'CVACT01Y.cpy'
        );
    END IF;
    
    -- Balance limit validation
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'chk_current_balance_limit'
    ) THEN
        PERFORM validation_results.log_validation(
            'accounts', 'CHECK_CONSTRAINT', 'Balance limit constraint validation',
            'chk_current_balance_limit exists', 'chk_current_balance_limit exists', 'PASS', NULL, 'CVACT01Y.cpy'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'accounts', 'CHECK_CONSTRAINT', 'Balance limit constraint validation',
            'chk_current_balance_limit exists', 'chk_current_balance_limit missing', 'FAIL',
            'Balance limit constraint missing', 'CVACT01Y.cpy'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 4. CARDS TABLE VALIDATION (VSAM CARDDAT Dataset)
-- ==============================================================================

DO $$
BEGIN
    -- Table existence check
    IF validation_results.table_exists('cards') THEN
        PERFORM validation_results.log_validation(
            'cards', 'TABLE_EXISTENCE', 'Cards table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'VSAM CARDDAT'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'cards', 'TABLE_EXISTENCE', 'Cards table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Cards table missing from schema', 'VSAM CARDDAT'
        );
        RETURN;
    END IF;
    
    -- Column structure validation
    PERFORM validation_results.validate_column_type('cards', 'card_number', 'varchar(16)', 'VSAM CARDDAT 16-digit card number');
    PERFORM validation_results.validate_column_type('cards', 'account_id', 'varchar(11)', 'VSAM CARDDAT account relationship');
    PERFORM validation_results.validate_column_type('cards', 'customer_id', 'varchar(9)', 'VSAM CARDDAT customer relationship');
    PERFORM validation_results.validate_column_type('cards', 'cvv_code', 'varchar(3)', 'VSAM CARDDAT CVV security code');
    PERFORM validation_results.validate_column_type('cards', 'embossed_name', 'varchar(50)', 'VSAM CARDDAT cardholder name');
    PERFORM validation_results.validate_column_type('cards', 'expiration_date', 'date', 'VSAM CARDDAT expiry date');
    PERFORM validation_results.validate_column_type('cards', 'active_status', 'varchar(1)', 'VSAM CARDDAT status flag');
    
    -- Composite foreign key validation (account_id + customer_id)
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'cards' AND tc.constraint_type = 'FOREIGN KEY'
          AND tc.constraint_name = 'fk_cards_account_customer'
    ) THEN
        PERFORM validation_results.log_validation(
            'cards', 'FOREIGN_KEY', 'Composite foreign key constraint validation',
            'fk_cards_account_customer exists', 'Composite foreign key exists', 'PASS', NULL, 'VSAM CARDDAT'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'cards', 'FOREIGN_KEY', 'Composite foreign key constraint validation',
            'fk_cards_account_customer exists', 'Composite foreign key missing', 'FAIL',
            'Composite foreign key constraint not found', 'VSAM CARDDAT'
        );
    END IF;
    
    -- Luhn algorithm validation constraint
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'chk_cards_luhn_valid'
    ) THEN
        PERFORM validation_results.log_validation(
            'cards', 'CHECK_CONSTRAINT', 'Luhn algorithm validation constraint',
            'chk_cards_luhn_valid exists', 'chk_cards_luhn_valid exists', 'PASS', NULL, 'VSAM CARDDAT'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'cards', 'CHECK_CONSTRAINT', 'Luhn algorithm validation constraint',
            'chk_cards_luhn_valid exists', 'chk_cards_luhn_valid missing', 'WARNING',
            'Luhn algorithm validation constraint missing', 'VSAM CARDDAT'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 5. TRANSACTIONS TABLE VALIDATION (VSAM TRANSACT Dataset)
-- ==============================================================================

DO $$
BEGIN
    -- Table existence check
    IF validation_results.table_exists('transactions') THEN
        PERFORM validation_results.log_validation(
            'transactions', 'TABLE_EXISTENCE', 'Transactions table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'VSAM TRANSACT'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'transactions', 'TABLE_EXISTENCE', 'Transactions table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Transactions table missing from schema', 'VSAM TRANSACT'
        );
        RETURN;
    END IF;
    
    -- Column structure validation with financial precision
    PERFORM validation_results.validate_column_type('transactions', 'transaction_id', 'varchar(16)', 'VSAM TRANSACT transaction identifier');
    PERFORM validation_results.validate_column_type('transactions', 'account_id', 'varchar(11)', 'VSAM TRANSACT account relationship');
    PERFORM validation_results.validate_column_type('transactions', 'card_number', 'varchar(16)', 'VSAM TRANSACT card relationship');
    PERFORM validation_results.validate_column_type('transactions', 'transaction_type', 'varchar(2)', 'VSAM TRANSACT type classification');
    PERFORM validation_results.validate_column_type('transactions', 'transaction_category', 'varchar(4)', 'VSAM TRANSACT category code');
    
    -- CRITICAL: Transaction amount precision validation - COBOL COMP-3 to DECIMAL(12,2)
    PERFORM validation_results.validate_column_type('transactions', 'transaction_amount', 'numeric(12,2)', 'VSAM TRANSACT financial amount PIC S9(10)V99');
    
    PERFORM validation_results.validate_column_type('transactions', 'description', 'varchar(100)', 'VSAM TRANSACT description field');
    PERFORM validation_results.validate_column_type('transactions', 'transaction_timestamp', 'timestamp with time zone', 'VSAM TRANSACT timestamp');
    
    -- Foreign key constraints validation
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'transactions' AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'account_id'
    ) THEN
        PERFORM validation_results.log_validation(
            'transactions', 'FOREIGN_KEY', 'Account foreign key constraint validation',
            'fk_transactions_account exists', 'Account foreign key exists', 'PASS', NULL, 'VSAM TRANSACT'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'transactions', 'FOREIGN_KEY', 'Account foreign key constraint validation',
            'fk_transactions_account exists', 'Account foreign key missing', 'FAIL',
            'Account foreign key constraint not found', 'VSAM TRANSACT'
        );
    END IF;
    
    -- Transaction amount range validation
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
        WHERE constraint_name = 'chk_transaction_amount_range'
    ) THEN
        PERFORM validation_results.log_validation(
            'transactions', 'CHECK_CONSTRAINT', 'Transaction amount range validation',
            'chk_transaction_amount_range exists', 'chk_transaction_amount_range exists', 'PASS', NULL, 'VSAM TRANSACT'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'transactions', 'CHECK_CONSTRAINT', 'Transaction amount range validation',
            'chk_transaction_amount_range exists', 'chk_transaction_amount_range missing', 'FAIL',
            'Transaction amount range constraint missing', 'VSAM TRANSACT'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 6. REFERENCE TABLES VALIDATION
-- ==============================================================================

DO $$
BEGIN
    -- Transaction Types Table Validation
    IF validation_results.table_exists('transaction_types') THEN
        PERFORM validation_results.log_validation(
            'transaction_types', 'TABLE_EXISTENCE', 'Transaction Types table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'trantype.txt'
        );
        
        PERFORM validation_results.validate_column_type('transaction_types', 'transaction_type', 'varchar(2)', 'trantype.txt 2-character type code');
        PERFORM validation_results.validate_column_type('transaction_types', 'type_description', 'varchar(60)', 'trantype.txt description field');
        PERFORM validation_results.validate_column_type('transaction_types', 'debit_credit_indicator', 'boolean', 'trantype.txt debit/credit flag');
    ELSE
        PERFORM validation_results.log_validation(
            'transaction_types', 'TABLE_EXISTENCE', 'Transaction Types table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Transaction Types table missing from schema', 'trantype.txt'
        );
    END IF;
    
    -- Transaction Categories Table Validation
    IF validation_results.table_exists('transaction_categories') THEN
        PERFORM validation_results.log_validation(
            'transaction_categories', 'TABLE_EXISTENCE', 'Transaction Categories table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'trancatg.txt'
        );
        
        PERFORM validation_results.validate_column_type('transaction_categories', 'transaction_category', 'varchar(4)', 'trancatg.txt 4-character category code');
        PERFORM validation_results.validate_column_type('transaction_categories', 'parent_transaction_type', 'varchar(2)', 'trancatg.txt parent type reference');
        PERFORM validation_results.validate_column_type('transaction_categories', 'category_description', 'varchar(60)', 'trancatg.txt category description');
    ELSE
        PERFORM validation_results.log_validation(
            'transaction_categories', 'TABLE_EXISTENCE', 'Transaction Categories table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Transaction Categories table missing from schema', 'trancatg.txt'
        );
    END IF;
    
    -- Disclosure Groups Table Validation
    IF validation_results.table_exists('disclosure_groups') THEN
        PERFORM validation_results.log_validation(
            'disclosure_groups', 'TABLE_EXISTENCE', 'Disclosure Groups table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'discgrp.txt'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'disclosure_groups', 'TABLE_EXISTENCE', 'Disclosure Groups table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Disclosure Groups table missing from schema', 'discgrp.txt'
        );
    END IF;
    
    -- Transaction Category Balances Table Validation (CVTRA01Y.cpy)
    IF validation_results.table_exists('transaction_category_balances') THEN
        PERFORM validation_results.log_validation(
            'transaction_category_balances', 'TABLE_EXISTENCE', 'Transaction Category Balances table existence check',
            'Table should exist', 'Table exists', 'PASS', NULL, 'CVTRA01Y.cpy'
        );
        
        -- Critical COBOL COMP-3 precision validation for balance field
        PERFORM validation_results.validate_column_type('transaction_category_balances', 'balance', 'numeric(11,2)', 'CVTRA01Y.cpy TRAN-CAT-BAL PIC S9(09)V99');
    ELSE
        PERFORM validation_results.log_validation(
            'transaction_category_balances', 'TABLE_EXISTENCE', 'Transaction Category Balances table existence check',
            'Table should exist', 'Table does not exist', 'FAIL', 
            'Transaction Category Balances table missing from schema', 'CVTRA01Y.cpy'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 7. INDEX VALIDATION (VSAM Alternate Index Equivalence)
-- ==============================================================================

DO $$
DECLARE
    v_expected_indexes TEXT[] := ARRAY[
        'idx_cards_account_id',
        'idx_customer_account_xref', 
        'idx_transactions_date_range',
        'idx_account_balance',
        'idx_cards_customer_account',
        'idx_transactions_account_lookup',
        'idx_transactions_card_lookup'
    ];
    v_index_name TEXT;
    v_index_exists BOOLEAN;
BEGIN
    -- Validate each expected index exists
    FOREACH v_index_name IN ARRAY v_expected_indexes
    LOOP
        SELECT EXISTS (
            SELECT 1 FROM pg_indexes 
            WHERE indexname = v_index_name AND schemaname = 'public'
        ) INTO v_index_exists;
        
        IF v_index_exists THEN
            PERFORM validation_results.log_validation(
                'indexes', 'INDEX_EXISTENCE', 'Index ' || v_index_name || ' existence check',
                'Index should exist', 'Index exists', 'PASS', NULL, 'VSAM Alternate Index'
            );
        ELSE
            PERFORM validation_results.log_validation(
                'indexes', 'INDEX_EXISTENCE', 'Index ' || v_index_name || ' existence check',
                'Index should exist', 'Index does not exist', 'FAIL',
                'Required index for VSAM alternate index equivalence missing', 'VSAM Alternate Index'
            );
        END IF;
    END LOOP;
    
    -- Validate primary indexes on all tables
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'pk_users' AND schemaname = 'public') THEN
        PERFORM validation_results.log_validation(
            'indexes', 'PRIMARY_INDEX', 'Users primary key index validation',
            'pk_users index exists', 'pk_users exists', 'PASS', NULL, 'VSAM Primary Key'
        );
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'pk_customers' AND schemaname = 'public') THEN
        PERFORM validation_results.log_validation(
            'indexes', 'PRIMARY_INDEX', 'Customers primary key index validation',
            'pk_customers index exists', 'pk_customers exists', 'PASS', NULL, 'VSAM Primary Key'
        );
    END IF;
END;
$$;

-- ==============================================================================
-- 8. COBOL DATA TYPE MAPPING VALIDATION SUMMARY
-- ==============================================================================

DO $$
DECLARE
    v_decimal_precision_count INTEGER;
    v_varchar_mapping_count INTEGER;
    v_date_mapping_count INTEGER;
    v_boolean_mapping_count INTEGER;
BEGIN
    -- Count DECIMAL(12,2) fields for COBOL COMP-3 precision validation
    SELECT COUNT(*) INTO v_decimal_precision_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND data_type = 'numeric'
      AND numeric_precision = 12
      AND numeric_scale = 2;
    
    -- Count VARCHAR mappings for COBOL PIC X fields
    SELECT COUNT(*) INTO v_varchar_mapping_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND data_type = 'character varying';
    
    -- Count DATE mappings for COBOL date fields
    SELECT COUNT(*) INTO v_date_mapping_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND data_type = 'date';
    
    -- Count BOOLEAN mappings for COBOL Y/N flags
    SELECT COUNT(*) INTO v_boolean_mapping_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND data_type = 'boolean';
    
    -- Log summary validation results
    PERFORM validation_results.log_validation(
        'data_types', 'COBOL_MAPPING_SUMMARY', 'COBOL COMP-3 to DECIMAL(12,2) precision count',
        'Multiple DECIMAL(12,2) fields expected', v_decimal_precision_count::TEXT, 
        CASE WHEN v_decimal_precision_count >= 5 THEN 'PASS' ELSE 'WARNING' END,
        CASE WHEN v_decimal_precision_count < 5 THEN 'Fewer DECIMAL(12,2) fields than expected' ELSE NULL END,
        'COBOL Data Type Mapping'
    );
    
    PERFORM validation_results.log_validation(
        'data_types', 'COBOL_MAPPING_SUMMARY', 'COBOL PIC X to VARCHAR mapping count',
        'Multiple VARCHAR fields expected', v_varchar_mapping_count::TEXT, 
        CASE WHEN v_varchar_mapping_count >= 10 THEN 'PASS' ELSE 'WARNING' END,
        CASE WHEN v_varchar_mapping_count < 10 THEN 'Fewer VARCHAR fields than expected' ELSE NULL END,
        'COBOL Data Type Mapping'
    );
    
    PERFORM validation_results.log_validation(
        'data_types', 'COBOL_MAPPING_SUMMARY', 'COBOL date field to DATE mapping count',
        'Multiple DATE fields expected', v_date_mapping_count::TEXT, 
        CASE WHEN v_date_mapping_count >= 3 THEN 'PASS' ELSE 'WARNING' END,
        CASE WHEN v_date_mapping_count < 3 THEN 'Fewer DATE fields than expected' ELSE NULL END,
        'COBOL Data Type Mapping'
    );
END;
$$;

-- ==============================================================================
-- 9. SCHEMA COMPLETENESS VALIDATION
-- ==============================================================================

DO $$
DECLARE
    v_expected_table_count INTEGER := 8; -- users, customers, accounts, cards, transactions, transaction_types, transaction_categories, disclosure_groups
    v_actual_table_count INTEGER;
    v_missing_tables TEXT[];
    v_expected_tables TEXT[] := ARRAY[
        'users', 'customers', 'accounts', 'cards', 'transactions',
        'transaction_types', 'transaction_categories', 'disclosure_groups'
    ];
    v_table_name TEXT;
BEGIN
    -- Count actual tables in public schema
    SELECT COUNT(*) INTO v_actual_table_count
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
    
    -- Check for missing expected tables
    FOREACH v_table_name IN ARRAY v_expected_tables
    LOOP
        IF NOT validation_results.table_exists(v_table_name) THEN
            v_missing_tables := array_append(v_missing_tables, v_table_name);
        END IF;
    END LOOP;
    
    -- Log schema completeness results
    IF array_length(v_missing_tables, 1) IS NULL THEN
        PERFORM validation_results.log_validation(
            'schema', 'COMPLETENESS', 'Schema completeness validation',
            'All expected tables present', 'All tables exist', 'PASS', NULL, 'Database Schema'
        );
    ELSE
        PERFORM validation_results.log_validation(
            'schema', 'COMPLETENESS', 'Schema completeness validation',
            'All expected tables present', 'Missing tables: ' || array_to_string(v_missing_tables, ', '), 'FAIL',
            'Schema incomplete - missing required tables', 'Database Schema'
        );
    END IF;
    
    PERFORM validation_results.log_validation(
        'schema', 'TABLE_COUNT', 'Total table count validation',
        v_expected_table_count::TEXT || ' tables expected', v_actual_table_count::TEXT || ' tables found', 
        CASE WHEN v_actual_table_count >= v_expected_table_count THEN 'PASS' ELSE 'FAIL' END,
        CASE WHEN v_actual_table_count < v_expected_table_count THEN 'Table count mismatch' ELSE NULL END,
        'Database Schema'
    );
END;
$$;

-- ==============================================================================
-- 10. VALIDATION RESULTS SUMMARY REPORT
-- ==============================================================================

-- Generate comprehensive validation summary report
\echo '=============================================================================='
\echo 'SCHEMA STRUCTURE VALIDATION RESULTS SUMMARY'
\echo '=============================================================================='

-- Overall validation status
SELECT 
    validation_status,
    COUNT(*) as validation_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM validation_results.schema_validation_summary
GROUP BY validation_status
ORDER BY validation_status;

\echo '=============================================================================='
\echo 'VALIDATION RESULTS BY CATEGORY'
\echo '=============================================================================='

-- Validation results by category
SELECT 
    validation_category,
    table_name,
    COUNT(*) as total_validations,
    SUM(CASE WHEN validation_status = 'PASS' THEN 1 ELSE 0 END) as passed,
    SUM(CASE WHEN validation_status = 'FAIL' THEN 1 ELSE 0 END) as failed,
    SUM(CASE WHEN validation_status = 'WARNING' THEN 1 ELSE 0 END) as warnings
FROM validation_results.schema_validation_summary
GROUP BY validation_category, table_name
ORDER BY validation_category, table_name;

\echo '=============================================================================='
\echo 'CRITICAL FAILURES (MUST BE RESOLVED)'
\echo '=============================================================================='

-- Show all critical failures
SELECT 
    table_name,
    validation_category,
    validation_description,
    expected_value,
    actual_value,
    error_message,
    copybook_reference
FROM validation_results.schema_validation_summary
WHERE validation_status = 'FAIL'
ORDER BY table_name, validation_category;

\echo '=============================================================================='
\echo 'COBOL DATA TYPE MAPPING COMPLIANCE REPORT'
\echo '=============================================================================='

-- COBOL to PostgreSQL data type mapping summary
SELECT 
    'DECIMAL(12,2) Fields (COBOL COMP-3)' as mapping_type,
    COUNT(*) as field_count
FROM information_schema.columns
WHERE table_schema = 'public'
  AND data_type = 'numeric'
  AND numeric_precision = 12
  AND numeric_scale = 2

UNION ALL

SELECT 
    'VARCHAR Fields (COBOL PIC X)' as mapping_type,
    COUNT(*) as field_count
FROM information_schema.columns
WHERE table_schema = 'public'
  AND data_type = 'character varying'

UNION ALL

SELECT 
    'DATE Fields (COBOL Date)' as mapping_type,
    COUNT(*) as field_count
FROM information_schema.columns
WHERE table_schema = 'public'
  AND data_type = 'date'

UNION ALL

SELECT 
    'BOOLEAN Fields (COBOL Y/N)' as mapping_type,
    COUNT(*) as field_count
FROM information_schema.columns
WHERE table_schema = 'public'
  AND data_type = 'boolean';

\echo '=============================================================================='
\echo 'VALIDATION COMPLETED'
\echo '=============================================================================='

-- Drop helper functions
DROP FUNCTION IF EXISTS validation_results.log_validation(VARCHAR(50), VARCHAR(50), TEXT, TEXT, TEXT, VARCHAR(10), TEXT, TEXT);
DROP FUNCTION IF EXISTS validation_results.table_exists(VARCHAR(50));
DROP FUNCTION IF EXISTS validation_results.validate_column_type(VARCHAR(50), VARCHAR(50), VARCHAR(100), VARCHAR(100));

-- Export validation results for external reporting
\copy (SELECT * FROM validation_results.schema_validation_summary ORDER BY validation_timestamp, table_name, validation_category) TO 'schema_validation_results.csv' WITH CSV HEADER;

\echo 'Schema validation results exported to: schema_validation_results.csv'
\echo 'Validation completed at:' $(date)

-- ==============================================================================
-- End of Schema Structure Validation Script
-- ==============================================================================