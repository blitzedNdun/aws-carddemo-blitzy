-- ================================================================
-- CardDemo H2 Test Database Schema
-- 
-- Purpose: Create H2 in-memory database schema for Spring Boot test execution
--          Supporting CustomerRepositoryTest and related test scenarios
--
-- Compatibility: H2 database with PostgreSQL mode compatibility
-- Source Mapping: Based on COBOL copybooks and entity mappings
-- ================================================================

-- Drop tables if they exist (for test cleanup)
DROP TABLE IF EXISTS account_data CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS disclosure_groups CASCADE;

-- ================================================================
-- Disclosure Groups Table (Reference Data)
-- Source: Reference table for account group disclosure information
-- ================================================================
CREATE TABLE disclosure_groups (
    group_id VARCHAR(10) NOT NULL,
    disclosure_text VARCHAR(255) NOT NULL,
    interest_rate DECIMAL(6,4) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    
    CONSTRAINT pk_disclosure_groups PRIMARY KEY (group_id)
);

-- ================================================================
-- Users Table (Authentication Testing)
-- Source: COBOL SEC-USER-DATA structure (CSUSR01Y.cpy)
-- ================================================================
CREATE TABLE users (
    user_id VARCHAR(8) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    user_type VARCHAR(1) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login TIMESTAMP NULL,
    
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT chk_user_type CHECK (user_type IN ('A', 'U', 'V'))
);

-- ================================================================
-- Customers Table (Customer Repository Testing)
-- Source: COBOL CUSTOMER-RECORD (VSAM CUSTDAT)
-- ================================================================
CREATE TABLE customers (
    customer_id VARCHAR(9) NOT NULL,
    first_name VARCHAR(25) NOT NULL,
    middle_name VARCHAR(25),
    last_name VARCHAR(25) NOT NULL,
    address_line_1 VARCHAR(50),
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    address_state VARCHAR(2),
    address_country VARCHAR(3),
    address_zip VARCHAR(10),
    ssn VARCHAR(9),
    date_of_birth DATE,
    fico_credit_score INTEGER,
    phone_home VARCHAR(15),
    phone_work VARCHAR(15),
    government_issued_id VARCHAR(20),
    eft_account_id VARCHAR(11),
    primary_card_holder_indicator VARCHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    CONSTRAINT chk_fico_score CHECK (fico_credit_score IS NULL OR (fico_credit_score >= 300 AND fico_credit_score <= 850)),
    CONSTRAINT chk_primary_indicator CHECK (primary_card_holder_indicator IN ('Y', 'N'))
);

-- ================================================================
-- Account Data Table (Account Repository Testing)
-- Source: COBOL ACCOUNT-RECORD (VSAM ACCTDAT)
-- ================================================================
CREATE TABLE account_data (
    account_id VARCHAR(11) NOT NULL,
    customer_id VARCHAR(9),
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    open_date DATE NOT NULL,
    expiration_date DATE,
    reissue_date DATE,
    address_zip VARCHAR(10),
    group_id VARCHAR(10),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_account_data PRIMARY KEY (account_id),
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y', 'N', 'S', 'C')),
    CONSTRAINT chk_current_balance CHECK (current_balance >= -999999999999.99),
    CONSTRAINT chk_credit_limit CHECK (credit_limit >= 0)
);

-- ================================================================
-- Accounts Table (Alternative Account Table for Test Compatibility)
-- Source: Test data compatibility for CustomerRepositoryTest
-- ================================================================
CREATE TABLE accounts (
    account_id VARCHAR(11) NOT NULL,
    customer_id VARCHAR(9),
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    open_date DATE NOT NULL,
    expiration_date DATE,
    address_zip VARCHAR(10),
    group_id VARCHAR(10),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT fk_accounts_group FOREIGN KEY (group_id) REFERENCES disclosure_groups(group_id),
    CONSTRAINT chk_accounts_active_status CHECK (active_status IN ('Y', 'N', 'S', 'C')),
    CONSTRAINT chk_accounts_current_balance CHECK (current_balance >= -999999999999.99),
    CONSTRAINT chk_accounts_credit_limit CHECK (credit_limit >= 0)
);

-- ================================================================
-- Create Indexes for Performance Testing
-- ================================================================
CREATE UNIQUE INDEX idx_disclosure_groups_group_id ON disclosure_groups (group_id);
CREATE INDEX idx_disclosure_groups_effective_date ON disclosure_groups (effective_date);

CREATE UNIQUE INDEX idx_customers_customer_id ON customers (customer_id);
CREATE INDEX idx_customers_name ON customers (last_name, first_name);
CREATE INDEX idx_customers_fico ON customers (fico_credit_score);
CREATE INDEX idx_customers_ssn ON customers (ssn);

CREATE UNIQUE INDEX idx_account_data_account_id ON account_data (account_id);
CREATE INDEX idx_account_data_customer_id ON account_data (customer_id);
CREATE INDEX idx_account_data_status ON account_data (active_status);
CREATE INDEX idx_account_data_balance ON account_data (current_balance);

CREATE UNIQUE INDEX idx_accounts_account_id ON accounts (account_id);
CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX idx_accounts_status ON accounts (active_status);
CREATE INDEX idx_accounts_balance ON accounts (current_balance);
CREATE INDEX idx_accounts_group_id ON accounts (group_id);

-- ================================================================
-- Test Data Insertion (Minimal set for test execution)
-- ================================================================

-- Insert test users for authentication testing
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, created_at) VALUES
    ('ADMIN001', 'Test', 'Administrator', '$2a$10$N.zmdr9vIw2K4qTyqFVHEOKlF/dSQdZJhXdTCMB1JF0w7FpO1EhC6', 'A', CURRENT_TIMESTAMP),
    ('USER0001', 'John', 'TestUser', '$2a$10$Sl.5eS4xHjf4VzPW8qKuUOZQj8Jt3OQFDtbxK6VT9T2h8EhJ5Gir6', 'U', CURRENT_TIMESTAMP),
    ('VIEWER01', 'Jane', 'TestViewer', '$2a$10$8RxLhE0tFJ2VZ5W7Q1K9qOyFjKlX3oN2DhG4M9SfP6vH1CzT8Pq5r', 'V', CURRENT_TIMESTAMP);

-- Test customers will be created by the test cases themselves
-- Test accounts will be created by the test cases themselves

-- ================================================================
-- H2 Database Specific Settings for PostgreSQL Compatibility
-- ================================================================
-- These commands ensure H2 behaves like PostgreSQL for testing
-- Note: Some settings are handled via JDBC URL parameters for compatibility