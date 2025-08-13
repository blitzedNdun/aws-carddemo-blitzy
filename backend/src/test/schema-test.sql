-- =============================================================================
-- CardDemo Test Database Schema
-- =============================================================================
-- Purpose: Test database schema initialization script that creates simplified 
--          versions of the PostgreSQL tables for unit testing, including 
--          account, transaction, customer, and card tables with test indexes 
--          and constraints.
--
-- Description: This schema mirrors the production PostgreSQL structure but is
--              optimized for test performance with simplified indexes, reduced
--              constraints for test flexibility, and sample test data insertion
--              scripts based on VSAM dataset definitions from LISTCAT.txt
--
-- VSAM Dataset Mapping Reference:
-- - user_security       <- AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS (KEYLEN: 8)
-- - customer_data       <- AWS.M2.CARDDEMO.CUSTDAT.VSAM.KSDS (KEYLEN: 9)  
-- - account_data        <- AWS.M2.CARDDEMO.ACCTDAT.VSAM.KSDS (KEYLEN: 11)
-- - card_data           <- AWS.M2.CARDDEMO.CARDDAT.VSAM.KSDS (KEYLEN: 16)
-- - transactions        <- AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (KEYLEN: 16)
-- - transaction_category_balance <- AWS.M2.CARDDEMO.TRAN.CATG.BAL.VSAM.KSDS (KEYLEN: 54)
-- - disclosure_groups   <- AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS (KEYLEN: 5)
-- - transaction_types   <- AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS (KEYLEN: 2)
-- - card_xref           <- AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS (KEYLEN: 19)
-- =============================================================================

-- Clean up existing test schema if it exists
DROP SCHEMA IF EXISTS test_carddemo CASCADE;
CREATE SCHEMA test_carddemo;
SET search_path TO test_carddemo;

-- =============================================================================
-- 1. USER SECURITY TABLE
-- =============================================================================
-- Maps to USRSEC VSAM dataset (KEYLEN: 8, MAXLRECL: 80)
-- Simplified for testing - no encryption, basic constraints
CREATE TABLE user_security (
    user_id VARCHAR(8) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    user_type CHAR(1) NOT NULL DEFAULT 'R', -- 'A'=Admin, 'R'=Regular
    first_name VARCHAR(20),
    last_name VARCHAR(20),
    created_date DATE DEFAULT CURRENT_DATE,
    last_login_date DATE,
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    CONSTRAINT pk_user_security PRIMARY KEY (user_id),
    CONSTRAINT chk_user_type CHECK (user_type IN ('A', 'R')),
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y', 'N'))
);

-- Test-optimized index for authentication lookups
CREATE INDEX idx_user_security_active ON user_security (user_id) WHERE active_status = 'Y';

-- =============================================================================
-- 2. CUSTOMER DATA TABLE
-- =============================================================================
-- Maps to CUSTDAT VSAM dataset (KEYLEN: 9, MAXLRECL: 500)
-- Simplified structure for testing flexibility
CREATE TABLE customer_data (
    customer_id BIGINT NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    middle_name VARCHAR(20),
    last_name VARCHAR(20) NOT NULL,
    address_line_1 VARCHAR(50),
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    state_code VARCHAR(2),
    country_code VARCHAR(3) DEFAULT 'USA',
    zip_code VARCHAR(10),
    phone_number_1 VARCHAR(15),
    phone_number_2 VARCHAR(15),
    ssn VARCHAR(9), -- Simplified for testing - no encryption
    government_id VARCHAR(20),
    date_of_birth DATE,
    eft_account_id VARCHAR(10),
    primary_card_holder CHAR(1) DEFAULT 'Y',
    fico_score SMALLINT,
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_customer_data PRIMARY KEY (customer_id),
    CONSTRAINT chk_primary_card_holder CHECK (primary_card_holder IN ('Y', 'N')),
    CONSTRAINT chk_customer_active CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_fico_score CHECK (fico_score BETWEEN 300 AND 850)
);

-- Test-optimized indexes for customer searches
CREATE INDEX idx_customer_name ON customer_data (last_name, first_name);
CREATE INDEX idx_customer_ssn ON customer_data (ssn) WHERE ssn IS NOT NULL;

-- =============================================================================
-- 3. DISCLOSURE GROUPS TABLE
-- =============================================================================
-- Maps to DISCGRP VSAM dataset (KEYLEN: 5, MAXLRECL: 100)
-- Reference data for interest rates and terms
CREATE TABLE disclosure_groups (
    disclosure_group_id BIGINT NOT NULL,
    group_name VARCHAR(50) NOT NULL,
    interest_rate NUMERIC(5,4) NOT NULL DEFAULT 0.0000,
    terms_text VARCHAR(500), -- Simplified for testing
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_disclosure_groups PRIMARY KEY (disclosure_group_id),
    CONSTRAINT chk_disclosure_active CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_interest_rate CHECK (interest_rate >= 0.0000 AND interest_rate <= 1.0000)
);

-- =============================================================================
-- 4. ACCOUNT DATA TABLE
-- =============================================================================
-- Maps to ACCTDAT VSAM dataset (KEYLEN: 11, MAXLRECL: 300)
-- Simplified constraints for test flexibility
CREATE TABLE account_data (
    account_id BIGINT NOT NULL,
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    current_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    credit_limit NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    cash_credit_limit NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    open_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiration_date DATE,
    reissue_date DATE,
    current_cycle_credit NUMERIC(12,2) DEFAULT 0.00,
    current_cycle_debit NUMERIC(12,2) DEFAULT 0.00,
    zip_code VARCHAR(10),
    group_id VARCHAR(10) DEFAULT 'DEFAULT',
    customer_id BIGINT NOT NULL,
    disclosure_group_id BIGINT NOT NULL DEFAULT 1,
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_account_data PRIMARY KEY (account_id),
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer_data(customer_id),
    CONSTRAINT fk_account_disclosure FOREIGN KEY (disclosure_group_id) REFERENCES disclosure_groups(disclosure_group_id),
    CONSTRAINT chk_account_active CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_balance_positive CHECK (current_balance >= 0.00),
    CONSTRAINT chk_credit_limits CHECK (credit_limit >= 0.00 AND cash_credit_limit >= 0.00)
);

-- Test-optimized indexes for account operations
CREATE INDEX idx_account_customer ON account_data (customer_id);
CREATE INDEX idx_account_active ON account_data (account_id) WHERE active_status = 'Y';

-- =============================================================================
-- 5. CARD DATA TABLE
-- =============================================================================
-- Maps to CARDDAT VSAM dataset (KEYLEN: 16, MAXLRECL: 150)
-- Simplified for testing - no card number encryption
CREATE TABLE card_data (
    card_number VARCHAR(16) NOT NULL,
    account_id BIGINT NOT NULL,
    cvv_code VARCHAR(3) NOT NULL, -- Simplified for testing
    embossed_name VARCHAR(50) NOT NULL,
    expiration_date DATE NOT NULL,
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    customer_id BIGINT NOT NULL,
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_card_data PRIMARY KEY (card_number),
    CONSTRAINT fk_card_account FOREIGN KEY (account_id) REFERENCES account_data(account_id),
    CONSTRAINT fk_card_customer FOREIGN KEY (customer_id) REFERENCES customer_data(customer_id),
    CONSTRAINT chk_card_active CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_cvv_format CHECK (cvv_code ~ '^[0-9]{3}$'),
    CONSTRAINT chk_card_number_format CHECK (card_number ~ '^[0-9]{16}$')
);

-- Test-optimized indexes for card operations (replicating CARDAIX)
CREATE INDEX idx_card_account ON card_data (account_id);
CREATE INDEX idx_card_customer ON card_data (customer_id);

-- =============================================================================
-- 6. TRANSACTION TYPES TABLE
-- =============================================================================
-- Maps to TRANTYPE VSAM dataset (KEYLEN: 2, MAXLRECL: 60)
-- Reference data for transaction classification
CREATE TABLE transaction_types (
    transaction_type_code VARCHAR(2) NOT NULL,
    description VARCHAR(50) NOT NULL,
    debit_credit_flag CHAR(1) NOT NULL,
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_transaction_types PRIMARY KEY (transaction_type_code),
    CONSTRAINT chk_debit_credit CHECK (debit_credit_flag IN ('D', 'C')),
    CONSTRAINT chk_trantype_active CHECK (active_status IN ('Y', 'N'))
);

-- =============================================================================
-- 7. TRANSACTION CATEGORIES TABLE
-- =============================================================================
-- Reference data for transaction categorization (composite key)
CREATE TABLE transaction_categories (
    category_code VARCHAR(4) NOT NULL,
    subcategory_code VARCHAR(2) NOT NULL,
    description VARCHAR(50) NOT NULL,
    category_name VARCHAR(25) NOT NULL,
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_transaction_categories PRIMARY KEY (category_code, subcategory_code),
    CONSTRAINT chk_catg_active CHECK (active_status IN ('Y', 'N'))
);

-- =============================================================================
-- 8. TRANSACTIONS TABLE
-- =============================================================================
-- Maps to TRANSACT VSAM dataset (KEYLEN: 16, MAXLRECL: 350)
-- Simplified partitioning for testing - single table without date partitioning
CREATE TABLE transactions (
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    card_number VARCHAR(16) NOT NULL,
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    transaction_time TIME NOT NULL DEFAULT CURRENT_TIME,
    amount NUMERIC(12,2) NOT NULL,
    transaction_type_code VARCHAR(2) NOT NULL,
    category_code VARCHAR(4) NOT NULL DEFAULT '9999',
    subcategory_code VARCHAR(2) NOT NULL DEFAULT '99',
    description VARCHAR(100),
    merchant_name VARCHAR(50),
    processed_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account_data(account_id),
    CONSTRAINT fk_transaction_card FOREIGN KEY (card_number) REFERENCES card_data(card_number),
    CONSTRAINT fk_transaction_type FOREIGN KEY (transaction_type_code) REFERENCES transaction_types(transaction_type_code),
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_code, subcategory_code) REFERENCES transaction_categories(category_code, subcategory_code),
    CONSTRAINT chk_amount_not_zero CHECK (amount != 0.00)
);

-- Test-optimized indexes for transaction operations (replicating TRANSACT AIX)
CREATE INDEX idx_transaction_account_date ON transactions (account_id, transaction_date);
CREATE INDEX idx_transaction_card ON transactions (card_number);
CREATE INDEX idx_transaction_date ON transactions (transaction_date);

-- =============================================================================
-- 9. TRANSACTION CATEGORY BALANCE TABLE
-- =============================================================================
-- Maps to TRAN.CATG.BAL VSAM dataset (KEYLEN: 54, MAXLRECL: 100)
-- Composite primary key matching VSAM structure
CREATE TABLE transaction_category_balance (
    account_id BIGINT NOT NULL,
    category_code VARCHAR(4) NOT NULL,
    subcategory_code VARCHAR(2) NOT NULL,
    balance_date DATE NOT NULL,
    balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_transaction_category_balance PRIMARY KEY (account_id, category_code, subcategory_code, balance_date),
    CONSTRAINT fk_tcb_account FOREIGN KEY (account_id) REFERENCES account_data(account_id),
    CONSTRAINT fk_tcb_category FOREIGN KEY (category_code, subcategory_code) REFERENCES transaction_categories(category_code, subcategory_code)
);

-- Test-optimized index for balance queries
CREATE INDEX idx_tcb_account_date ON transaction_category_balance (account_id, balance_date);

-- =============================================================================
-- 10. CARD CROSS-REFERENCE TABLE
-- =============================================================================
-- Maps to CARDXREF VSAM dataset (KEYLEN: 19, MAXLRECL: 50)
-- Simplified for testing - tracks card-to-account relationships
CREATE TABLE card_xref (
    card_number VARCHAR(16) NOT NULL,
    account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    xref_type CHAR(1) NOT NULL DEFAULT 'P', -- 'P'=Primary, 'S'=Secondary
    active_status CHAR(1) NOT NULL DEFAULT 'Y',
    created_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT pk_card_xref PRIMARY KEY (card_number, account_id),
    CONSTRAINT fk_xref_card FOREIGN KEY (card_number) REFERENCES card_data(card_number),
    CONSTRAINT fk_xref_account FOREIGN KEY (account_id) REFERENCES account_data(account_id),
    CONSTRAINT fk_xref_customer FOREIGN KEY (customer_id) REFERENCES customer_data(customer_id),
    CONSTRAINT chk_xref_type CHECK (xref_type IN ('P', 'S')),
    CONSTRAINT chk_xref_active CHECK (active_status IN ('Y', 'N'))
);

-- =============================================================================
-- TEST DATA INSERTION SCRIPTS
-- =============================================================================

-- Insert test disclosure groups
INSERT INTO disclosure_groups (disclosure_group_id, group_name, interest_rate, terms_text) VALUES
(1, 'Standard Credit Terms', 0.1999, 'Standard credit card terms and conditions'),
(2, 'Premium Credit Terms', 0.1499, 'Premium card terms with lower interest rate'),
(3, 'Student Credit Terms', 0.2199, 'Student credit card promotional terms');

-- Insert test transaction types
INSERT INTO transaction_types (transaction_type_code, description, debit_credit_flag) VALUES
('01', 'Purchase', 'D'),
('02', 'Cash Advance', 'D'),
('03', 'Payment', 'C'),
('04', 'Fee', 'D'),
('05', 'Interest', 'D'),
('06', 'Credit Adjustment', 'C'),
('07', 'Debit Adjustment', 'D');

-- Insert test transaction categories
INSERT INTO transaction_categories (category_code, subcategory_code, description, category_name) VALUES
('0001', '01', 'General Merchandise', 'Shopping'),
('0002', '01', 'Gasoline Service Stations', 'Gas'),
('0003', '01', 'Grocery Stores', 'Groceries'),
('0004', '01', 'Restaurants', 'Dining'),
('0005', '01', 'Travel and Entertainment', 'Travel'),
('9999', '99', 'Other/Miscellaneous', 'Other');

-- Insert test customers
INSERT INTO customer_data (customer_id, first_name, last_name, address_line_1, state_code, zip_code, phone_number_1, ssn, date_of_birth, fico_score) VALUES
(1000000001, 'John', 'Smith', '123 Main St', 'TX', '75001', '214-555-0001', '123456789', '1980-01-15', 720),
(1000000002, 'Jane', 'Johnson', '456 Oak Ave', 'CA', '90210', '310-555-0002', '987654321', '1985-06-20', 680),
(1000000003, 'Mike', 'Wilson', '789 Pine Rd', 'NY', '10001', '212-555-0003', '456789123', '1990-12-10', 750),
(1000000004, 'Sarah', 'Davis', '321 Elm St', 'FL', '33101', '305-555-0004', '789123456', '1975-03-25', 620),
(1000000005, 'Test', 'Admin', '999 Test Blvd', 'TX', '78701', '512-555-0005', '111223333', '1970-01-01', 800);

-- Insert test accounts
INSERT INTO account_data (account_id, customer_id, current_balance, credit_limit, cash_credit_limit, disclosure_group_id, group_id) VALUES
(11111111111, 1000000001, 1250.75, 5000.00, 1000.00, 1, 'STANDARD'),
(22222222222, 1000000002, 2100.50, 10000.00, 2000.00, 2, 'PREMIUM'),
(33333333333, 1000000003, 850.25, 3000.00, 500.00, 3, 'STUDENT'),
(44444444444, 1000000004, 3250.80, 7500.00, 1500.00, 1, 'STANDARD'),
(55555555555, 1000000005, 0.00, 25000.00, 5000.00, 2, 'ADMIN');

-- Insert test cards
INSERT INTO card_data (card_number, account_id, customer_id, cvv_code, embossed_name, expiration_date) VALUES
('4111111111111111', 11111111111, 1000000001, '123', 'JOHN SMITH', '2026-12-31'),
('4222222222222222', 22222222222, 1000000002, '456', 'JANE JOHNSON', '2027-06-30'),
('4333333333333333', 33333333333, 1000000003, '789', 'MIKE WILSON', '2025-09-30'),
('4444444444444444', 44444444444, 1000000004, '321', 'SARAH DAVIS', '2026-03-31'),
('4555555555555555', 55555555555, 1000000005, '555', 'TEST ADMIN', '2028-12-31');

-- Insert test card cross-references
INSERT INTO card_xref (card_number, account_id, customer_id, xref_type) VALUES
('4111111111111111', 11111111111, 1000000001, 'P'),
('4222222222222222', 22222222222, 1000000002, 'P'),
('4333333333333333', 33333333333, 1000000003, 'P'),
('4444444444444444', 44444444444, 1000000004, 'P'),
('4555555555555555', 55555555555, 1000000005, 'P');

-- Insert test users
INSERT INTO user_security (user_id, password_hash, user_type, first_name, last_name) VALUES
('ADMIN', '$2a$10$dummyhashfortest1234567890123456789012345678', 'A', 'Test', 'Admin'),
('USER01', '$2a$10$dummyhashfortest1234567890123456789012345679', 'R', 'John', 'Smith'),
('USER02', '$2a$10$dummyhashfortest1234567890123456789012345680', 'R', 'Jane', 'Johnson'),
('USER03', '$2a$10$dummyhashfortest1234567890123456789012345681', 'R', 'Mike', 'Wilson'),
('TESTUSER', '$2a$10$dummyhashfortest1234567890123456789012345682', 'R', 'Test', 'User');

-- Insert sample transactions for testing
INSERT INTO transactions (transaction_id, account_id, card_number, transaction_date, amount, transaction_type_code, category_code, subcategory_code, description, merchant_name) VALUES
(1001, 11111111111, '4111111111111111', CURRENT_DATE - INTERVAL '5 days', -125.50, '01', '0004', '01', 'Restaurant Purchase', 'ACME RESTAURANT'),
(1002, 11111111111, '4111111111111111', CURRENT_DATE - INTERVAL '3 days', -45.75, '01', '0002', '01', 'Gas Station Purchase', 'SHELL GAS STATION'),
(1003, 22222222222, '4222222222222222', CURRENT_DATE - INTERVAL '2 days', -89.25, '01', '0003', '01', 'Grocery Purchase', 'WALMART SUPERCENTER'),
(1004, 22222222222, '4222222222222222', CURRENT_DATE - INTERVAL '1 days', 500.00, '03', '9999', '99', 'Payment Received', 'ONLINE PAYMENT'),
(1005, 33333333333, '4333333333333333', CURRENT_DATE, -25.00, '01', '0001', '01', 'Online Purchase', 'AMAZON.COM');

-- Insert sample category balances for testing
INSERT INTO transaction_category_balance (account_id, category_code, subcategory_code, balance_date, balance) VALUES
(11111111111, '0001', '01', CURRENT_DATE, 275.50),
(11111111111, '0002', '01', CURRENT_DATE, 125.75),
(22222222222, '0003', '01', CURRENT_DATE, 189.25),
(22222222222, '9999', '99', CURRENT_DATE, -500.00),
(33333333333, '0001', '01', CURRENT_DATE, 25.00);

-- =============================================================================
-- TEST UTILITY FUNCTIONS
-- =============================================================================

-- Function to clean test data for fresh test runs
CREATE OR REPLACE FUNCTION reset_test_data() RETURNS VOID AS $$
BEGIN
    -- Delete data in dependency order
    DELETE FROM transaction_category_balance;
    DELETE FROM transactions;
    DELETE FROM card_xref;
    DELETE FROM card_data;
    DELETE FROM account_data;
    DELETE FROM customer_data;
    DELETE FROM user_security;
    DELETE FROM transaction_categories;
    DELETE FROM transaction_types;
    DELETE FROM disclosure_groups;
    
    -- Reset sequences if they exist
    -- Note: This is a simplified test environment, so we're not using sequences
    
    RAISE NOTICE 'Test data has been reset successfully.';
END;
$$ LANGUAGE plpgsql;

-- Function to validate test data integrity
CREATE OR REPLACE FUNCTION validate_test_data() RETURNS TABLE(
    table_name TEXT,
    record_count BIGINT,
    validation_status TEXT
) AS $$
BEGIN
    -- Check each table and return counts
    RETURN QUERY
    SELECT 'user_security'::TEXT, COUNT(*)::BIGINT, 
           CASE WHEN COUNT(*) >= 5 THEN 'PASS' ELSE 'FAIL' END::TEXT
    FROM user_security
    UNION ALL
    SELECT 'customer_data'::TEXT, COUNT(*)::BIGINT,
           CASE WHEN COUNT(*) >= 5 THEN 'PASS' ELSE 'FAIL' END::TEXT
    FROM customer_data
    UNION ALL
    SELECT 'account_data'::TEXT, COUNT(*)::BIGINT,
           CASE WHEN COUNT(*) >= 5 THEN 'PASS' ELSE 'FAIL' END::TEXT
    FROM account_data
    UNION ALL
    SELECT 'card_data'::TEXT, COUNT(*)::BIGINT,
           CASE WHEN COUNT(*) >= 5 THEN 'PASS' ELSE 'FAIL' END::TEXT
    FROM card_data
    UNION ALL
    SELECT 'transactions'::TEXT, COUNT(*)::BIGINT,
           CASE WHEN COUNT(*) >= 5 THEN 'PASS' ELSE 'FAIL' END::TEXT
    FROM transactions;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- FINAL SCHEMA VALIDATION
-- =============================================================================

-- Verify all tables were created successfully
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'test_carddemo';
    
    IF table_count >= 10 THEN
        RAISE NOTICE 'SUCCESS: Created % tables in test_carddemo schema', table_count;
    ELSE
        RAISE EXCEPTION 'FAILURE: Only % tables created, expected at least 10', table_count;
    END IF;
END;
$$;

-- Set final search path and display completion message
SET search_path TO test_carddemo, public;

SELECT 'CardDemo Test Schema Initialization Complete' AS status,
       'test_carddemo' AS schema_name,
       NOW() AS created_timestamp;

-- Display test data validation results
SELECT * FROM validate_test_data();

-- =============================================================================
-- END OF TEST SCHEMA INITIALIZATION
-- =============================================================================