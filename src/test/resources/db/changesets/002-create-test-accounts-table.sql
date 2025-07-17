-- liquibase formatted sql

-- ============================================================================
-- CardDemo Test Accounts Table Creation
-- ============================================================================
-- Purpose: Create PostgreSQL accounts table for test environment based on 
--          CVACT01Y.cpy ACCOUNT-RECORD structure with exact financial precision
--          to match COBOL COMP-3 packed decimal fields for integration testing
-- Environment: Test environment with comprehensive financial validation
-- Source: app/cpy/CVACT01Y.cpy (ACCOUNT-RECORD structure)
-- Dependencies: 001-create-test-users-table.sql (users table must exist)
-- ============================================================================

-- changeset carddemo:002-create-test-accounts-table
-- comment: Create accounts table for test environment with BigDecimal precision support
-- labels: test-environment, financial-data, account-management
-- preconditions: onFail:HALT onError:HALT
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'accounts' AND table_schema = 'public';
-- expected-result: 0

CREATE TABLE accounts (
    -- Primary key - maps to ACCT-ID from CVACT01Y.cpy (PIC 9(11))
    -- 11-digit account identifier with unique constraint
    account_id VARCHAR(11) NOT NULL,
    
    -- Account status - maps to ACCT-ACTIVE-STATUS (PIC X(01))
    -- 'A' = Active, 'C' = Closed, 'S' = Suspended, 'P' = Pending
    active_status VARCHAR(1) NOT NULL,
    
    -- Financial amount fields - maps to COBOL COMP-3 fields with exact precision
    -- Using DECIMAL(12,2) to match COBOL PIC S9(10)V99 COMP-3 precision
    
    -- Current account balance - maps to ACCT-CURR-BAL (PIC S9(10)V99)
    -- Supports negative balances with exact penny precision
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Credit limit - maps to ACCT-CREDIT-LIMIT (PIC S9(10)V99)
    -- Maximum credit available to account holder
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Cash advance limit - maps to ACCT-CASH-CREDIT-LIMIT (PIC S9(10)V99)
    -- Maximum cash advance amount available
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Current cycle credit - maps to ACCT-CURR-CYC-CREDIT (PIC S9(10)V99)
    -- Credits applied during current billing cycle
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Current cycle debit - maps to ACCT-CURR-CYC-DEBIT (PIC S9(10)V99)
    -- Debits applied during current billing cycle
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Date fields - maps to COBOL PIC X(10) date fields
    -- Using DATE type for proper date handling and constraints
    
    -- Account opening date - maps to ACCT-OPEN-DATE (PIC X(10))
    -- Date when account was first opened
    open_date DATE NOT NULL,
    
    -- Account expiration date - maps to ACCT-EXPIRAION-DATE (PIC X(10))
    -- Note: Original COBOL field has typo "EXPIRAION" - preserving for compatibility
    expiration_date DATE,
    
    -- Account reissue date - maps to ACCT-REISSUE-DATE (PIC X(10))
    -- Date when account was last reissued or renewed
    reissue_date DATE,
    
    -- Address and grouping fields
    
    -- Address ZIP code - maps to ACCT-ADDR-ZIP (PIC X(10))
    -- ZIP code associated with account for billing/verification
    address_zip VARCHAR(10),
    
    -- Group identifier - maps to ACCT-GROUP-ID (PIC X(10))
    -- Links to disclosure groups for interest rates and terms
    group_id VARCHAR(10),
    
    -- Foreign key reference to users table for account ownership
    -- This establishes the relationship between accounts and authenticated users
    customer_id VARCHAR(9),
    
    -- Audit fields for test environment tracking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Test environment flags
    is_test_account BOOLEAN DEFAULT TRUE,
    test_scenario VARCHAR(50),
    
    -- Primary key constraint
    CONSTRAINT pk_accounts PRIMARY KEY (account_id)
);

-- ============================================================================
-- Test Environment Constraints and Validation
-- ============================================================================

-- Account ID format constraint (11 numeric characters)
ALTER TABLE accounts ADD CONSTRAINT chk_account_id_format 
    CHECK (LENGTH(account_id) = 11 AND account_id ~ '^[0-9]{11}$');

-- Account status constraint (valid status codes)
ALTER TABLE accounts ADD CONSTRAINT chk_account_status 
    CHECK (active_status IN ('A', 'C', 'S', 'P'));

-- Financial amount constraints for data integrity
ALTER TABLE accounts ADD CONSTRAINT chk_current_balance_precision
    CHECK (current_balance >= -99999999.99 AND current_balance <= 99999999.99);

ALTER TABLE accounts ADD CONSTRAINT chk_credit_limit_positive
    CHECK (credit_limit >= 0.00 AND credit_limit <= 99999999.99);

ALTER TABLE accounts ADD CONSTRAINT chk_cash_credit_limit_positive
    CHECK (cash_credit_limit >= 0.00 AND cash_credit_limit <= 99999999.99);

ALTER TABLE accounts ADD CONSTRAINT chk_current_cycle_credit_precision
    CHECK (current_cycle_credit >= -99999999.99 AND current_cycle_credit <= 99999999.99);

ALTER TABLE accounts ADD CONSTRAINT chk_current_cycle_debit_precision
    CHECK (current_cycle_debit >= -99999999.99 AND current_cycle_debit <= 99999999.99);

-- Date constraints for business logic validation
ALTER TABLE accounts ADD CONSTRAINT chk_open_date_not_future
    CHECK (open_date <= CURRENT_DATE);

ALTER TABLE accounts ADD CONSTRAINT chk_expiration_after_open
    CHECK (expiration_date IS NULL OR expiration_date > open_date);

ALTER TABLE accounts ADD CONSTRAINT chk_reissue_after_open
    CHECK (reissue_date IS NULL OR reissue_date >= open_date);

-- ZIP code format constraint (US ZIP code format)
ALTER TABLE accounts ADD CONSTRAINT chk_address_zip_format
    CHECK (address_zip IS NULL OR address_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

-- Group ID format constraint (alphanumeric, up to 10 characters)
ALTER TABLE accounts ADD CONSTRAINT chk_group_id_format
    CHECK (group_id IS NULL OR (LENGTH(group_id) <= 10 AND group_id ~ '^[A-Z0-9]+$'));

-- Customer ID format constraint (9 numeric characters)
ALTER TABLE accounts ADD CONSTRAINT chk_customer_id_format
    CHECK (customer_id IS NULL OR (LENGTH(customer_id) = 9 AND customer_id ~ '^[0-9]{9}$'));

-- Test scenario constraint for test environment
ALTER TABLE accounts ADD CONSTRAINT chk_test_scenario
    CHECK (test_scenario IS NULL OR LENGTH(test_scenario) <= 50);

-- Foreign key constraint to users table (if customer maps to user)
-- This supports account ownership validation in test scenarios
ALTER TABLE accounts ADD CONSTRAINT fk_accounts_users
    FOREIGN KEY (customer_id) REFERENCES users(user_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- ============================================================================
-- Indexes for Account Management Service Testing
-- ============================================================================

-- Unique index on account_id for primary key performance
CREATE UNIQUE INDEX idx_accounts_account_id ON accounts (account_id);

-- Index for active status filtering (frequently used in account queries)
CREATE INDEX idx_accounts_active_status ON accounts (active_status);

-- Index for customer account lookups (support account ownership queries)
CREATE INDEX idx_accounts_customer_id ON accounts (customer_id) 
    WHERE customer_id IS NOT NULL;

-- Index for group-based account queries (disclosure group processing)
CREATE INDEX idx_accounts_group_id ON accounts (group_id) 
    WHERE group_id IS NOT NULL;

-- Index for date-based queries (account lifecycle management)
CREATE INDEX idx_accounts_open_date ON accounts (open_date);

-- Index for ZIP code-based queries (geographic account analysis)
CREATE INDEX idx_accounts_address_zip ON accounts (address_zip) 
    WHERE address_zip IS NOT NULL;

-- Index for test scenario filtering in test environment
CREATE INDEX idx_accounts_test_scenario ON accounts (test_scenario) 
    WHERE test_scenario IS NOT NULL;

-- Composite index for account management service testing
CREATE INDEX idx_accounts_management_lookup ON accounts (account_id, active_status, customer_id);

-- Index for balance range queries (financial reporting)
CREATE INDEX idx_accounts_balance_range ON accounts (current_balance, active_status);

-- ============================================================================
-- Comments for Test Environment Documentation
-- ============================================================================

COMMENT ON TABLE accounts IS 'Test environment accounts table for Spring Boot microservices testing. Maps COBOL ACCOUNT-RECORD structure from CVACT01Y.cpy to PostgreSQL with exact financial precision using DECIMAL(12,2) for COMP-3 fields.';

COMMENT ON COLUMN accounts.account_id IS 'Primary key - 11 digit account identifier (maps to ACCT-ID PIC 9(11))';
COMMENT ON COLUMN accounts.active_status IS 'Account status - A=Active, C=Closed, S=Suspended, P=Pending (maps to ACCT-ACTIVE-STATUS PIC X(01))';
COMMENT ON COLUMN accounts.current_balance IS 'Current account balance with exact penny precision (maps to ACCT-CURR-BAL PIC S9(10)V99 COMP-3)';
COMMENT ON COLUMN accounts.credit_limit IS 'Maximum credit limit for account (maps to ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3)';
COMMENT ON COLUMN accounts.cash_credit_limit IS 'Maximum cash advance limit (maps to ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3)';
COMMENT ON COLUMN accounts.open_date IS 'Account opening date (maps to ACCT-OPEN-DATE PIC X(10))';
COMMENT ON COLUMN accounts.expiration_date IS 'Account expiration date (maps to ACCT-EXPIRAION-DATE PIC X(10) - preserving original typo)';
COMMENT ON COLUMN accounts.reissue_date IS 'Account reissue/renewal date (maps to ACCT-REISSUE-DATE PIC X(10))';
COMMENT ON COLUMN accounts.current_cycle_credit IS 'Credits in current billing cycle (maps to ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3)';
COMMENT ON COLUMN accounts.current_cycle_debit IS 'Debits in current billing cycle (maps to ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3)';
COMMENT ON COLUMN accounts.address_zip IS 'ZIP code for account billing address (maps to ACCT-ADDR-ZIP PIC X(10))';
COMMENT ON COLUMN accounts.group_id IS 'Disclosure group identifier for interest rates (maps to ACCT-GROUP-ID PIC X(10))';
COMMENT ON COLUMN accounts.customer_id IS 'Foreign key to users table for account ownership';
COMMENT ON COLUMN accounts.created_at IS 'Account creation timestamp for audit trail';
COMMENT ON COLUMN accounts.updated_at IS 'Last update timestamp for audit trail';
COMMENT ON COLUMN accounts.is_test_account IS 'Test environment flag - always TRUE for test accounts';
COMMENT ON COLUMN accounts.test_scenario IS 'Test scenario identifier for integration testing';

-- ============================================================================
-- Test Data Seeding for Account Management Service Testing
-- ============================================================================

-- Test account for admin user with comprehensive financial data
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000001', 'A', 1500.75, 5000.00, 1000.00,
    250.00, 125.50, '2023-01-15', '2025-01-31',
    '12345', 'STANDARD', NULL, 'admin-account-testing'
);

-- Test account for regular user with different balance scenario
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000002', 'A', -250.00, 2000.00, 500.00,
    100.00, 350.00, '2023-03-20', '2025-03-31',
    '67890', 'PREMIUM', NULL, 'user-account-testing'
);

-- Test account for JWT token testing with high balance
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000003', 'A', 25000.00, 10000.00, 2000.00,
    500.00, 200.00, '2023-02-10', '2025-02-28',
    '54321', 'GOLD', NULL, 'jwt-account-testing'
);

-- Test account for Spring Security integration testing
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000004', 'A', 750.25, 3000.00, 750.00,
    150.00, 100.75, '2023-04-05', '2025-04-30',
    '98765', 'STANDARD', NULL, 'spring-security-account-test'
);

-- Test account for role-based access control testing (closed account)
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000005', 'C', 0.00, 0.00, 0.00,
    0.00, 0.00, '2022-12-01', '2024-12-31',
    '11111', 'STANDARD', NULL, 'rbac-closed-account-testing'
);

-- Test account for BigDecimal precision validation
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000006', 'A', 123.45, 1000.00, 250.00,
    67.89, 34.56, '2023-05-15', '2025-05-31',
    '22222', 'STANDARD', NULL, 'bigdecimal-precision-testing'
);

-- Test account for financial calculation validation
INSERT INTO accounts (
    account_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date,
    address_zip, group_id, customer_id, test_scenario
)
VALUES (
    '10000000007', 'A', 9999.99, 15000.00, 3000.00,
    1000.00, 500.00, '2023-06-01', '2025-06-30',
    '33333', 'PREMIUM', NULL, 'financial-calculation-testing'
);

-- ============================================================================
-- Test Environment Validation Queries
-- ============================================================================

-- Validate table creation and structure
SELECT 
    table_name, 
    column_name, 
    data_type, 
    numeric_precision,
    numeric_scale,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'accounts' 
ORDER BY ordinal_position;

-- Validate constraints
SELECT 
    constraint_name, 
    constraint_type, 
    table_name
FROM information_schema.table_constraints 
WHERE table_name = 'accounts';

-- Validate foreign key relationships
SELECT 
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.key_column_usage AS kcu
JOIN information_schema.referential_constraints AS rc
    ON kcu.constraint_name = rc.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON rc.unique_constraint_name = ccu.constraint_name
WHERE kcu.table_name = 'accounts'
AND kcu.constraint_name LIKE 'fk_%';

-- Validate indexes
SELECT 
    indexname, 
    indexdef
FROM pg_indexes 
WHERE tablename = 'accounts';

-- Validate test data insertion and BigDecimal precision
SELECT 
    account_id,
    active_status,
    current_balance,
    credit_limit,
    cash_credit_limit,
    current_cycle_credit,
    current_cycle_debit,
    open_date,
    expiration_date,
    test_scenario,
    created_at
FROM accounts 
ORDER BY account_id;

-- Validate financial precision by checking decimal places
SELECT 
    account_id,
    current_balance,
    SCALE(current_balance) as balance_scale,
    PRECISION(current_balance) as balance_precision,
    test_scenario
FROM accounts
WHERE test_scenario LIKE '%precision%';

-- Validate constraint enforcement
SELECT 
    constraint_name,
    check_clause
FROM information_schema.check_constraints
WHERE constraint_name LIKE 'chk_%'
AND constraint_schema = 'public';

-- rollback DROP TABLE accounts CASCADE;