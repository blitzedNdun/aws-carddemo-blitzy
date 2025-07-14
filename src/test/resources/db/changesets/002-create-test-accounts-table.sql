-- =====================================================================
-- Liquibase Changeset: Create Test Accounts Table
-- Description: PostgreSQL accounts table creation for test environment
-- Author: Blitzy agent
-- Version: CardDemo_v1.0-15-g27d6c6f-68
-- =====================================================================
--
-- Maps COBOL ACCOUNT-RECORD structure from CVACT01Y.cpy to PostgreSQL:
-- 01 ACCOUNT-RECORD.
--   05 ACCT-ID                    PIC 9(11)       -> account_id VARCHAR(11) PRIMARY KEY
--   05 ACCT-ACTIVE-STATUS         PIC X(01)       -> active_status VARCHAR(1)
--   05 ACCT-CURR-BAL              PIC S9(10)V99   -> current_balance DECIMAL(12,2)
--   05 ACCT-CREDIT-LIMIT          PIC S9(10)V99   -> credit_limit DECIMAL(12,2)
--   05 ACCT-CASH-CREDIT-LIMIT     PIC S9(10)V99   -> cash_credit_limit DECIMAL(12,2)
--   05 ACCT-OPEN-DATE             PIC X(10)       -> open_date DATE
--   05 ACCT-EXPIRAION-DATE        PIC X(10)       -> expiration_date DATE
--   05 ACCT-REISSUE-DATE          PIC X(10)       -> reissue_date DATE
--   05 ACCT-CURR-CYC-CREDIT       PIC S9(10)V99   -> current_cycle_credit DECIMAL(12,2)
--   05 ACCT-CURR-CYC-DEBIT        PIC S9(10)V99   -> current_cycle_debit DECIMAL(12,2)
--   05 ACCT-ADDR-ZIP              PIC X(10)       -> address_zip VARCHAR(10)
--   05 ACCT-GROUP-ID              PIC X(10)       -> group_id VARCHAR(10)
--   05 FILLER                     PIC X(178)      -> (not mapped - COBOL filler)
--
-- Spring Boot Integration:
-- - JPA Entity mapping for account management microservices
-- - BigDecimal precision for financial calculations with DECIMAL(12,2)
-- - Foreign key relationships for referential integrity
-- - Optimized for TestContainers integration testing
-- - Support for Spring Data JPA repository operations
-- =====================================================================

-- liquibase formatted sql

-- changeset blitzy-agent:002-create-test-accounts-table
-- comment: Create accounts table for test environment with BigDecimal precision and foreign key constraints

-- =============================================================================
-- Main Accounts Table Creation
-- =============================================================================
CREATE TABLE accounts (
    -- Primary key mapping from COBOL ACCT-ID (PIC 9(11))
    -- 11-digit account identifier for unique account identification
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key to users table for account ownership
    -- Links accounts to authenticated users for access control
    user_id VARCHAR(8) NOT NULL,
    
    -- Active status mapping from COBOL ACCT-ACTIVE-STATUS (PIC X(01))
    -- Values: 'A' = Active, 'C' = Closed, 'S' = Suspended
    active_status VARCHAR(1) NOT NULL DEFAULT 'A',
    
    -- Current balance with exact COBOL COMP-3 precision
    -- Maps from COBOL ACCT-CURR-BAL (PIC S9(10)V99)
    -- DECIMAL(12,2) ensures penny-perfect financial accuracy
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Credit limit with exact COBOL COMP-3 precision
    -- Maps from COBOL ACCT-CREDIT-LIMIT (PIC S9(10)V99)
    -- DECIMAL(12,2) for maximum credit allowed
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Cash credit limit with exact COBOL COMP-3 precision
    -- Maps from COBOL ACCT-CASH-CREDIT-LIMIT (PIC S9(10)V99)
    -- DECIMAL(12,2) for cash advance limits
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Account opening date mapping from COBOL ACCT-OPEN-DATE (PIC X(10))
    -- PostgreSQL DATE type for proper date handling
    open_date DATE NOT NULL,
    
    -- Account expiration date mapping from COBOL ACCT-EXPIRAION-DATE (PIC X(10))
    -- Note: Preserves original COBOL field name spelling
    expiration_date DATE DEFAULT NULL,
    
    -- Card reissue date mapping from COBOL ACCT-REISSUE-DATE (PIC X(10))
    -- Tracks when cards were last reissued for this account
    reissue_date DATE DEFAULT NULL,
    
    -- Current cycle credit with exact COBOL COMP-3 precision
    -- Maps from COBOL ACCT-CURR-CYC-CREDIT (PIC S9(10)V99)
    -- DECIMAL(12,2) for current billing cycle credit transactions
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Current cycle debit with exact COBOL COMP-3 precision
    -- Maps from COBOL ACCT-CURR-CYC-DEBIT (PIC S9(10)V99)
    -- DECIMAL(12,2) for current billing cycle debit transactions
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Address ZIP code mapping from COBOL ACCT-ADDR-ZIP (PIC X(10))
    -- Used for account location tracking and validation
    address_zip VARCHAR(10) DEFAULT NULL,
    
    -- Group identifier mapping from COBOL ACCT-GROUP-ID (PIC X(10))
    -- Links to disclosure groups for interest rate determination
    group_id VARCHAR(10) DEFAULT NULL,
    
    -- Audit timestamps for test environment tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    
    -- Foreign key constraint to users table for account ownership
    CONSTRAINT fk_accounts_user_id FOREIGN KEY (user_id) 
        REFERENCES users(user_id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Check constraints for data integrity (test environment validation)
    CONSTRAINT chk_accounts_account_id_format CHECK (
        account_id ~ '^[0-9]{11}$' -- 11 numeric digits exactly
    ),
    
    CONSTRAINT chk_accounts_active_status_valid CHECK (
        active_status IN ('A', 'C', 'S') -- Active, Closed, Suspended
    ),
    
    CONSTRAINT chk_accounts_current_balance_precision CHECK (
        current_balance >= -9999999999.99 AND current_balance <= 9999999999.99
    ),
    
    CONSTRAINT chk_accounts_credit_limit_positive CHECK (
        credit_limit >= 0.00 AND credit_limit <= 9999999999.99
    ),
    
    CONSTRAINT chk_accounts_cash_credit_limit_positive CHECK (
        cash_credit_limit >= 0.00 AND cash_credit_limit <= 9999999999.99
    ),
    
    CONSTRAINT chk_accounts_cash_limit_within_credit_limit CHECK (
        cash_credit_limit <= credit_limit
    ),
    
    CONSTRAINT chk_accounts_open_date_valid CHECK (
        open_date <= CURRENT_DATE
    ),
    
    CONSTRAINT chk_accounts_expiration_after_open CHECK (
        expiration_date IS NULL OR expiration_date > open_date
    ),
    
    CONSTRAINT chk_accounts_cycle_credit_precision CHECK (
        current_cycle_credit >= -9999999999.99 AND current_cycle_credit <= 9999999999.99
    ),
    
    CONSTRAINT chk_accounts_cycle_debit_precision CHECK (
        current_cycle_debit >= -9999999999.99 AND current_cycle_debit <= 9999999999.99
    ),
    
    CONSTRAINT chk_accounts_address_zip_format CHECK (
        address_zip IS NULL OR address_zip ~ '^[0-9]{5}(-[0-9]{4})?$'
    ),
    
    CONSTRAINT chk_accounts_group_id_format CHECK (
        group_id IS NULL OR LENGTH(TRIM(group_id)) > 0
    )
);

-- =============================================================================
-- Indexes for Performance Optimization (Test Environment)
-- =============================================================================

-- Primary access pattern: account lookup by account_id
-- B-tree index automatically created by PRIMARY KEY constraint

-- Secondary access pattern: user account lookup for account management services
CREATE INDEX idx_accounts_user_id 
ON accounts (user_id, active_status);

-- Access pattern: account status filtering for administrative operations
CREATE INDEX idx_accounts_active_status 
ON accounts (active_status);

-- Access pattern: account opening date for reporting and analytics
CREATE INDEX idx_accounts_open_date 
ON accounts (open_date);

-- Access pattern: balance queries for financial operations
CREATE INDEX idx_accounts_current_balance 
ON accounts (current_balance) 
WHERE active_status = 'A';

-- Access pattern: credit limit analysis for risk management
CREATE INDEX idx_accounts_credit_limit 
ON accounts (credit_limit, cash_credit_limit) 
WHERE active_status = 'A';

-- Access pattern: group-based account filtering for interest rate application
CREATE INDEX idx_accounts_group_id 
ON accounts (group_id) 
WHERE group_id IS NOT NULL;

-- Composite index for account management service queries
CREATE INDEX idx_accounts_user_status_balance 
ON accounts (user_id, active_status, current_balance);

-- =============================================================================
-- Trigger for Updated Timestamp Management
-- =============================================================================

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to call the function on UPDATE
CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- =============================================================================
-- Comments for Documentation and Maintenance
-- =============================================================================

COMMENT ON TABLE accounts IS 
'CardDemo accounts table for test environment. Maps COBOL ACCOUNT-RECORD structure from CVACT01Y.cpy to PostgreSQL schema with exact financial precision using DECIMAL(12,2) for BigDecimal compatibility. Optimized for Spring Boot microservices integration testing with TestContainers.';

COMMENT ON COLUMN accounts.account_id IS 
'Primary key. Maps from COBOL ACCT-ID (PIC 9(11)). 11-digit unique account identifier.';

COMMENT ON COLUMN accounts.user_id IS 
'Foreign key to users table. Links accounts to authenticated users for access control and ownership.';

COMMENT ON COLUMN accounts.active_status IS 
'Account status. Maps from COBOL ACCT-ACTIVE-STATUS (PIC X(01)). Values: A=Active, C=Closed, S=Suspended.';

COMMENT ON COLUMN accounts.current_balance IS 
'Current account balance. Maps from COBOL ACCT-CURR-BAL (PIC S9(10)V99). DECIMAL(12,2) ensures exact BigDecimal precision for financial calculations.';

COMMENT ON COLUMN accounts.credit_limit IS 
'Maximum credit limit. Maps from COBOL ACCT-CREDIT-LIMIT (PIC S9(10)V99). DECIMAL(12,2) for precise financial limits.';

COMMENT ON COLUMN accounts.cash_credit_limit IS 
'Cash advance limit. Maps from COBOL ACCT-CASH-CREDIT-LIMIT (PIC S9(10)V99). DECIMAL(12,2) for cash advance restrictions.';

COMMENT ON COLUMN accounts.open_date IS 
'Account opening date. Maps from COBOL ACCT-OPEN-DATE (PIC X(10)). PostgreSQL DATE type for proper date handling.';

COMMENT ON COLUMN accounts.expiration_date IS 
'Account expiration date. Maps from COBOL ACCT-EXPIRAION-DATE (PIC X(10)). Preserves original COBOL field name spelling.';

COMMENT ON COLUMN accounts.reissue_date IS 
'Card reissue date. Maps from COBOL ACCT-REISSUE-DATE (PIC X(10)). Tracks when cards were last reissued.';

COMMENT ON COLUMN accounts.current_cycle_credit IS 
'Current billing cycle credits. Maps from COBOL ACCT-CURR-CYC-CREDIT (PIC S9(10)V99). DECIMAL(12,2) for exact cycle tracking.';

COMMENT ON COLUMN accounts.current_cycle_debit IS 
'Current billing cycle debits. Maps from COBOL ACCT-CURR-CYC-DEBIT (PIC S9(10)V99). DECIMAL(12,2) for exact cycle tracking.';

COMMENT ON COLUMN accounts.address_zip IS 
'Account address ZIP code. Maps from COBOL ACCT-ADDR-ZIP (PIC X(10)). Used for location tracking and validation.';

COMMENT ON COLUMN accounts.group_id IS 
'Disclosure group identifier. Maps from COBOL ACCT-GROUP-ID (PIC X(10)). Links to disclosure groups for interest rate determination.';

COMMENT ON COLUMN accounts.created_at IS 
'Account creation timestamp. Added for audit trail and test environment tracking.';

COMMENT ON COLUMN accounts.updated_at IS 
'Last update timestamp. Automatically updated by trigger for audit purposes.';

-- =============================================================================
-- Test Data Seeding for Integration Testing
-- =============================================================================

-- Test account for ADMIN001 user with active status and positive balance
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '12345678901', 'ADMIN001', 'A', 1500.75, 5000.00, 1000.00,
    '2024-01-01', '2027-01-01', '2024-01-01', 250.00, 180.25,
    '12345', 'STANDARD', '2024-01-01 09:00:00'
);

-- Test account for USER0001 with active status and different balance
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '23456789012', 'USER0001', 'A', 2750.50, 10000.00, 2000.00,
    '2024-01-15', '2027-01-15', '2024-01-15', 500.00, 325.75,
    '67890', 'PREMIUM', '2024-01-15 10:00:00'
);

-- Test account for USER0002 with suspended status for edge case testing
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '34567890123', 'USER0002', 'S', 125.00, 2500.00, 500.00,
    '2024-02-01', '2027-02-01', '2024-02-01', 75.00, 50.00,
    '54321', 'BASIC', '2024-02-01 11:00:00'
);

-- Test account for ADMIN002 with closed status for status testing
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '45678901234', 'ADMIN002', 'C', 0.00, 0.00, 0.00,
    '2023-12-01', '2024-12-01', '2023-12-01', 0.00, 0.00,
    '98765', 'STANDARD', '2023-12-01 12:00:00'
);

-- High-value test account for BigDecimal precision testing
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '56789012345', 'USER0001', 'A', 9999999999.99, 9999999999.99, 5000000000.00,
    '2024-01-01', '2027-01-01', '2024-01-01', 1000000.00, 2000000.00,
    '11111', 'PREMIUM', '2024-01-01 13:00:00'
);

-- Test account with minimal values for boundary testing
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '67890123456', 'USER0002', 'A', 0.01, 100.00, 50.00,
    '2024-03-01', '2027-03-01', '2024-03-01', 0.01, 0.00,
    '00001', 'BASIC', '2024-03-01 14:00:00'
);

-- Test account with negative balance for edge case testing
INSERT INTO accounts (
    account_id, user_id, active_status, current_balance, credit_limit, cash_credit_limit,
    open_date, expiration_date, reissue_date, current_cycle_credit, current_cycle_debit,
    address_zip, group_id, created_at
) VALUES (
    '78901234567', 'USER0001', 'A', -500.75, 3000.00, 600.00,
    '2024-01-10', '2027-01-10', '2024-01-10', 150.00, 650.75,
    '22222', 'STANDARD', '2024-01-10 15:00:00'
);

-- =============================================================================
-- Grant Permissions for Test Environment
-- =============================================================================

-- Grant necessary permissions for application user (configured in test properties)
-- Note: In actual test environment, these would be handled by TestContainers configuration

-- Permissions for application to read/write account data
-- GRANT SELECT, INSERT, UPDATE, DELETE ON accounts TO carddemo_app_user;

-- Permissions for monitoring/auditing (if applicable in test environment)
-- GRANT SELECT ON accounts TO carddemo_readonly_user;

-- =============================================================================
-- Rollback SQL (for Liquibase rollback capability)
-- =============================================================================

--rollback DROP TRIGGER IF EXISTS trg_accounts_updated_at ON accounts;
--rollback DROP FUNCTION IF EXISTS update_accounts_updated_at();
--rollback DROP TABLE IF EXISTS accounts CASCADE;

-- =============================================================================
-- Changeset Validation and Testing Notes
-- =============================================================================

-- Test SQL Validation Queries (for manual verification):
-- 
-- 1. Verify table structure matches COBOL layout:
-- SELECT column_name, data_type, character_maximum_length, numeric_precision, 
--        numeric_scale, is_nullable, column_default
-- FROM information_schema.columns 
-- WHERE table_name = 'accounts' AND table_schema = 'public'
-- ORDER BY ordinal_position;
--
-- 2. Verify constraints and foreign keys:
-- SELECT constraint_name, constraint_type, table_name
-- FROM information_schema.table_constraints
-- WHERE table_name = 'accounts' AND table_schema = 'public';
--
-- 3. Verify indexes for performance:
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'accounts' AND schemaname = 'public';
--
-- 4. Test BigDecimal precision validation:
-- SELECT account_id, current_balance, credit_limit, 
--        current_cycle_credit, current_cycle_debit
-- FROM accounts 
-- WHERE account_id = '12345678901';
--
-- 5. Test foreign key relationship:
-- SELECT a.account_id, a.user_id, u.first_name, u.last_name
-- FROM accounts a
-- JOIN users u ON a.user_id = u.user_id
-- WHERE a.active_status = 'A';
--
-- 6. Test balance calculations (BigDecimal precision):
-- SELECT account_id, 
--        current_balance + current_cycle_credit - current_cycle_debit AS calculated_balance,
--        (credit_limit - current_balance) AS available_credit
-- FROM accounts 
-- WHERE active_status = 'A';
--
-- 7. Test date constraints:
-- SELECT account_id, open_date, expiration_date,
--        (expiration_date - open_date) AS account_term_days
-- FROM accounts 
-- WHERE expiration_date IS NOT NULL;

-- End of changeset: 002-create-test-accounts-table