--liquibase formatted sql

--changeset blitzy-agent:002-create-test-accounts-table splitStatements:false rollbackSplitStatements:false
--comment: Create PostgreSQL accounts table for test environment based on CVACT01Y.cpy ACCOUNT-RECORD structure
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'accounts' AND table_schema = current_schema()

-- ================================================================
-- CardDemo Test Accounts Table Creation
-- 
-- Purpose: Create PostgreSQL accounts table for test environment with
--          exact COBOL COMP-3 decimal precision using DECIMAL(12,2) for all
--          financial amounts to support BigDecimal integration testing
--
-- Source Mapping: COBOL copybook app/cpy/CVACT01Y.cpy ACCOUNT-RECORD
--   ACCT-ID                    PIC 9(11) → account_id VARCHAR(11) PRIMARY KEY
--   ACCT-ACTIVE-STATUS         PIC X(01) → active_status VARCHAR(1) NOT NULL
--   ACCT-CURR-BAL              PIC S9(10)V99 → current_balance DECIMAL(12,2) NOT NULL
--   ACCT-CREDIT-LIMIT          PIC S9(10)V99 → credit_limit DECIMAL(12,2) NOT NULL
--   ACCT-CASH-CREDIT-LIMIT     PIC S9(10)V99 → cash_credit_limit DECIMAL(12,2) NOT NULL
--   ACCT-OPEN-DATE             PIC X(10) → open_date DATE NOT NULL
--   ACCT-EXPIRAION-DATE        PIC X(10) → expiration_date DATE NULL
--   ACCT-REISSUE-DATE          PIC X(10) → reissue_date DATE NULL
--   ACCT-CURR-CYC-CREDIT       PIC S9(10)V99 → current_cycle_credit DECIMAL(12,2) DEFAULT 0.00
--   ACCT-CURR-CYC-DEBIT        PIC S9(10)V99 → current_cycle_debit DECIMAL(12,2) DEFAULT 0.00
--   ACCT-ADDR-ZIP              PIC X(10) → address_zip VARCHAR(10) NULL
--   ACCT-GROUP-ID              PIC X(10) → group_id VARCHAR(10) NULL
--   FILLER                     PIC X(178) → Not mapped (legacy padding)
--
-- Additional fields for modern integration testing and audit requirements:
--   customer_id VARCHAR(9) - Foreign key to customers table for relationship testing
--   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--   last_transaction_date TIMESTAMP NULL - For integration testing scenarios
--
-- Test Environment Specifications:
-- - DECIMAL(12,2) precision for all financial amounts (exact COBOL COMP-3 equivalence)
-- - Foreign key constraints for relational integrity testing
-- - Enhanced constraints for comprehensive data validation testing
-- - Audit fields for account management service testing scenarios
-- - Support for BigDecimal precision validation in Spring Boot microservices
-- ================================================================

CREATE TABLE accounts (
    -- Primary key: Account ID mapping from COBOL ACCT-ID
    account_id VARCHAR(11) NOT NULL,
    
    -- Customer relationship for foreign key constraint testing
    customer_id VARCHAR(9) NULL, -- References users.user_id for test scenarios
    
    -- Account status and lifecycle information from COBOL structure
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- Financial balance fields with exact COBOL COMP-3 precision
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Current cycle financial tracking with BigDecimal precision
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Date fields for account lifecycle management
    open_date DATE NOT NULL,
    expiration_date DATE NULL,
    reissue_date DATE NULL,
    
    -- Additional COBOL fields for address and grouping
    address_zip VARCHAR(10) NULL,
    group_id VARCHAR(10) NULL,
    
    -- Audit and tracking fields for integration testing
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_transaction_date TIMESTAMP WITH TIME ZONE NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    
    -- Foreign key constraint for customer relationship testing
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES users (user_id),
    
    -- Business rule constraints for comprehensive test data validation
    CONSTRAINT chk_account_id_format CHECK (account_id ~ '^[0-9]{11}$'), -- 11 numeric digits
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y', 'N', 'S', 'C')), -- Active, iNactive, Suspended, Closed
    CONSTRAINT chk_current_balance_precision CHECK (current_balance = ROUND(current_balance, 2)),
    CONSTRAINT chk_credit_limit_precision CHECK (credit_limit = ROUND(credit_limit, 2)),
    CONSTRAINT chk_cash_credit_limit_precision CHECK (cash_credit_limit = ROUND(cash_credit_limit, 2)),
    CONSTRAINT chk_current_cycle_credit_precision CHECK (current_cycle_credit = ROUND(current_cycle_credit, 2)),
    CONSTRAINT chk_current_cycle_debit_precision CHECK (current_cycle_debit = ROUND(current_cycle_debit, 2)),
    CONSTRAINT chk_credit_limit_positive CHECK (credit_limit >= 0.00),
    CONSTRAINT chk_cash_credit_limit_positive CHECK (cash_credit_limit >= 0.00),
    CONSTRAINT chk_current_balance_range CHECK (current_balance >= -credit_limit),
    CONSTRAINT chk_expiration_after_open CHECK (expiration_date IS NULL OR expiration_date >= open_date),
    CONSTRAINT chk_reissue_after_open CHECK (reissue_date IS NULL OR reissue_date >= open_date),
    CONSTRAINT chk_address_zip_format CHECK (address_zip IS NULL OR address_zip ~ '^[0-9]{5}(-[0-9]{4})?$'), -- US ZIP code format
    CONSTRAINT chk_group_id_format CHECK (group_id IS NULL OR LENGTH(TRIM(group_id)) >= 1)
);

-- Create indexes for performance testing and query optimization
CREATE UNIQUE INDEX idx_accounts_account_id ON accounts (account_id);
CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX idx_accounts_active_status ON accounts (active_status);
CREATE INDEX idx_accounts_current_balance ON accounts (current_balance);
CREATE INDEX idx_accounts_credit_limit ON accounts (credit_limit);
CREATE INDEX idx_accounts_open_date ON accounts (open_date);
CREATE INDEX idx_accounts_group_id ON accounts (group_id);
CREATE INDEX idx_accounts_created_at ON accounts (created_at);
CREATE INDEX idx_accounts_updated_at ON accounts (updated_at);

-- Composite indexes for common account management service queries
CREATE INDEX idx_accounts_customer_status ON accounts (customer_id, active_status);
CREATE INDEX idx_accounts_balance_range ON accounts (current_balance, credit_limit);
CREATE INDEX idx_accounts_date_range ON accounts (open_date, expiration_date);

-- Add table comments for test documentation
COMMENT ON TABLE accounts IS 'Test environment accounts table for CardDemo account management service testing. Mapped from COBOL ACCOUNT-RECORD structure in CVACT01Y.cpy copybook with exact DECIMAL(12,2) precision for financial calculations.';
COMMENT ON COLUMN accounts.account_id IS 'Primary key: 11-digit account identifier from COBOL ACCT-ID field';
COMMENT ON COLUMN accounts.customer_id IS 'Foreign key to users table for customer relationship testing scenarios';
COMMENT ON COLUMN accounts.active_status IS 'Account status from COBOL ACCT-ACTIVE-STATUS field: Y=Active, N=Inactive, S=Suspended, C=Closed';
COMMENT ON COLUMN accounts.current_balance IS 'Current account balance from COBOL ACCT-CURR-BAL with DECIMAL(12,2) precision for BigDecimal testing';
COMMENT ON COLUMN accounts.credit_limit IS 'Credit limit from COBOL ACCT-CREDIT-LIMIT with exact financial precision';
COMMENT ON COLUMN accounts.cash_credit_limit IS 'Cash advance limit from COBOL ACCT-CASH-CREDIT-LIMIT field';
COMMENT ON COLUMN accounts.current_cycle_credit IS 'Current cycle credits from COBOL ACCT-CURR-CYC-CREDIT for billing cycle testing';
COMMENT ON COLUMN accounts.current_cycle_debit IS 'Current cycle debits from COBOL ACCT-CURR-CYC-DEBIT for billing cycle testing';
COMMENT ON COLUMN accounts.open_date IS 'Account opening date from COBOL ACCT-OPEN-DATE field';
COMMENT ON COLUMN accounts.expiration_date IS 'Account expiration date from COBOL ACCT-EXPIRAION-DATE field';
COMMENT ON COLUMN accounts.reissue_date IS 'Account reissue date from COBOL ACCT-REISSUE-DATE field';
COMMENT ON COLUMN accounts.address_zip IS 'ZIP code from COBOL ACCT-ADDR-ZIP field for address validation testing';
COMMENT ON COLUMN accounts.group_id IS 'Account group identifier from COBOL ACCT-GROUP-ID field';
COMMENT ON COLUMN accounts.created_at IS 'Account creation timestamp for audit trail in test scenarios';
COMMENT ON COLUMN accounts.updated_at IS 'Last update timestamp for integration testing';
COMMENT ON COLUMN accounts.last_transaction_date IS 'Last transaction timestamp for account activity testing';

-- Insert comprehensive test data for integration testing scenarios
INSERT INTO accounts (
    account_id, customer_id, active_status, current_balance, credit_limit, cash_credit_limit,
    current_cycle_credit, current_cycle_debit, open_date, expiration_date, reissue_date,
    address_zip, group_id, created_at, updated_at, last_transaction_date
) VALUES
    -- Test account for ADMIN001 user with high credit limit
    ('12345678901', 'ADMIN001', 'Y', 1500.75, 10000.00, 2000.00, 
     500.25, 750.50, '2020-01-15', '2025-01-15', NULL, 
     '12345', 'PREMIUM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    
    -- Test account for USER0001 with standard credit limit
    ('23456789012', 'USER0001', 'Y', 2750.00, 5000.00, 1000.00,
     1200.75, 800.25, '2021-03-10', '2026-03-10', NULL,
     '54321', 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    
    -- Test account for VIEWER01 with low credit limit
    ('34567890123', 'VIEWER01', 'Y', 450.25, 2000.00, 500.00,
     200.00, 150.75, '2022-06-20', '2027-06-20', NULL,
     '67890', 'BASIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    
    -- Suspended account for testing status scenarios
    ('45678901234', 'TESTUS01', 'S', -250.50, 3000.00, 750.00,
     0.00, 500.25, '2021-09-05', '2026-09-05', '2023-01-15',
     '98765', 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '10 days'),
    
    -- Closed account for testing historical scenarios
    ('56789012345', 'TESTUS02', 'C', 0.00, 0.00, 0.00,
     0.00, 0.00, '2020-12-01', '2023-12-01', NULL,
     '11111', 'CLOSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '30 days'),
    
    -- High-balance account for financial precision testing
    ('67890123456', 'ADMIN001', 'Y', 9999999999.99, 10000000000.00, 1000000.00,
     5000000.25, 2500000.75, '2019-05-15', '2024-05-15', NULL,
     '22222', 'PLATINUM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    
    -- Zero-balance account for edge case testing
    ('78901234567', 'USER0001', 'Y', 0.00, 1000.00, 200.00,
     0.00, 0.00, '2023-01-01', '2028-01-01', NULL,
     '33333', 'STARTER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
    
    -- Account with exact penny precision for BigDecimal testing
    ('89012345678', 'VIEWER01', 'Y', 123.45, 1500.00, 300.00,
     67.89, 45.67, '2022-11-30', '2027-11-30', NULL,
     '44444', 'STANDARD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '3 days');

-- Create test-specific indexes for performance testing
CREATE INDEX idx_accounts_test_balance_search ON accounts (current_balance, active_status);
CREATE INDEX idx_accounts_test_date_created ON accounts (created_at, updated_at);
CREATE INDEX idx_accounts_test_financial_limits ON accounts (credit_limit, cash_credit_limit);

-- Update table statistics for query optimization in test scenarios
ANALYZE accounts;

--rollback DROP TABLE IF EXISTS accounts CASCADE;

--changeset blitzy-agent:002-create-test-accounts-table-triggers splitStatements:false rollbackSplitStatements:false
--comment: Create triggers for test accounts table to support integration testing scenarios

-- Create trigger function for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for updated_at timestamp
CREATE TRIGGER trigger_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- Create trigger function for balance validation in test scenarios
CREATE OR REPLACE FUNCTION validate_account_balance_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Log balance changes for integration testing audit trail
    IF NEW.current_balance != OLD.current_balance THEN
        NEW.last_transaction_date = CURRENT_TIMESTAMP;
    END IF;
    
    -- Validate balance doesn't exceed credit limit (allow overdraft up to credit limit)
    IF NEW.current_balance < -NEW.credit_limit THEN
        RAISE EXCEPTION 'Account balance cannot exceed credit limit. Balance: %, Credit Limit: %', 
                       NEW.current_balance, NEW.credit_limit;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for balance change validation
CREATE TRIGGER trigger_validate_account_balance
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION validate_account_balance_change();

-- Add comments for trigger documentation
COMMENT ON FUNCTION update_accounts_updated_at() IS 'Trigger function to automatically update the updated_at timestamp for accounts table modifications';
COMMENT ON FUNCTION validate_account_balance_change() IS 'Trigger function to validate account balance changes and update last_transaction_date for integration testing';

--rollback DROP TRIGGER IF EXISTS trigger_accounts_updated_at ON accounts; DROP TRIGGER IF EXISTS trigger_validate_account_balance ON accounts; DROP FUNCTION IF EXISTS update_accounts_updated_at(); DROP FUNCTION IF EXISTS validate_account_balance_change();