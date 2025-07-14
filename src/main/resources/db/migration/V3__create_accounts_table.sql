-- ============================================================================
-- Liquibase Migration: V3__create_accounts_table.sql
-- Description: Create accounts table migrated from VSAM ACCTDAT dataset
-- Author: Blitzy agent
-- Version: 3.0
-- Dependencies: V2__create_customers_table.sql (foreign key reference)
-- ============================================================================

-- Create accounts table with comprehensive field mapping from ACCTDAT VSAM dataset
-- Implements exact financial field precision and customer relationship management
CREATE TABLE accounts (
    -- Primary identifier: 11-digit account ID maintaining fixed-width structure from acctdata.txt
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key reference to customers table establishing customer-account relationship
    customer_id VARCHAR(9) NOT NULL,
    
    -- Account status indicator supporting active/inactive lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Financial fields with exact DECIMAL(12,2) precision for COBOL COMP-3 arithmetic equivalence
    -- Current account balance with precise financial calculation support
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Maximum credit limit allowed for the account
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Cash advance credit limit with separate tracking from general credit limit
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Account lifecycle management date fields
    -- Account opening date for lifecycle tracking
    open_date DATE NOT NULL,
    
    -- Account expiration date for renewal management
    expiration_date DATE,
    
    -- Card reissue date for security and maintenance operations
    reissue_date DATE,
    
    -- Financial calculation fields for current billing cycle with DECIMAL precision
    -- Current cycle credit transactions total
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Current cycle debit transactions total
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Address ZIP code field for account location tracking
    address_zip VARCHAR(10),
    
    -- Foreign key reference to disclosure_groups table for interest rate management
    group_id VARCHAR(10) NOT NULL,
    
    -- Audit fields for tracking record lifecycle
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_accounts PRIMARY KEY (account_id)
);

-- Create foreign key constraints for referential integrity
-- Foreign key constraint to customers table establishing customer-account relationship
ALTER TABLE accounts ADD CONSTRAINT fk_accounts_customer_id 
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Foreign key constraint to disclosure_groups table for interest rate management
-- Note: This constraint will be enabled after disclosure_groups table is created
-- ALTER TABLE accounts ADD CONSTRAINT fk_accounts_group_id 
--     FOREIGN KEY (group_id) REFERENCES disclosure_groups(group_id)
--     ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create check constraints for business rule validation
-- Account ID must be exactly 11 numeric digits
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_id_format 
    CHECK (account_id ~ '^[0-9]{11}$');

-- Current balance must be within reasonable limits (prevent negative balances beyond credit limit)
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_balance_limit 
    CHECK (current_balance >= (credit_limit * -1));

-- Credit limit must be non-negative
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_credit_limit_positive 
    CHECK (credit_limit >= 0);

-- Cash credit limit must be non-negative and not exceed general credit limit
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_cash_credit_limit_valid 
    CHECK (cash_credit_limit >= 0 AND cash_credit_limit <= credit_limit);

-- Open date must be reasonable (not in future, not too historical)
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_open_date_range 
    CHECK (open_date <= CURRENT_DATE AND open_date >= '1990-01-01');

-- Expiration date must be after open date when specified
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_expiration_after_open 
    CHECK (expiration_date IS NULL OR expiration_date >= open_date);

-- Reissue date must be after open date when specified
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_reissue_after_open 
    CHECK (reissue_date IS NULL OR reissue_date >= open_date);

-- Current cycle credit must be non-negative
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_cycle_credit_positive 
    CHECK (current_cycle_credit >= 0);

-- Current cycle debit must be non-negative
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_cycle_debit_positive 
    CHECK (current_cycle_debit >= 0);

-- ZIP code format validation (supports XXXXX or XXXXX-XXXX format)
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_zip_format 
    CHECK (address_zip IS NULL OR address_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

-- Group ID must be exactly 10 characters
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_group_id_format 
    CHECK (group_id ~ '^[A-Za-z0-9]{10}$');

-- Create indexes for performance optimization
-- Primary index on account_id is automatically created with PRIMARY KEY

-- Index for customer-account relationship queries (replicating CXACAIX functionality)
CREATE INDEX idx_accounts_customer_id ON accounts (customer_id, account_id);

-- Index for account status queries supporting active account filtering
CREATE INDEX idx_accounts_active_status ON accounts (active_status, account_id);

-- Index for balance range queries supporting account management operations
CREATE INDEX idx_accounts_balance_range ON accounts (current_balance, credit_limit);

-- Index for date-based account lifecycle queries
CREATE INDEX idx_accounts_open_date ON accounts (open_date, account_id);

-- Index for expiration date queries supporting account renewal operations
CREATE INDEX idx_accounts_expiration_date ON accounts (expiration_date) 
    WHERE expiration_date IS NOT NULL;

-- Index for disclosure group queries supporting interest rate management
CREATE INDEX idx_accounts_group_id ON accounts (group_id, account_id);

-- Composite index for account balance and status queries
CREATE INDEX idx_accounts_balance_status ON accounts (current_balance, active_status, account_id);

-- Index for credit limit analysis and reporting
CREATE INDEX idx_accounts_credit_limits ON accounts (credit_limit, cash_credit_limit, account_id);

-- Create trigger for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- Create trigger for account balance validation on updates
CREATE OR REPLACE FUNCTION validate_account_balance_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Prevent balance changes that would exceed credit limit restrictions
    IF NEW.current_balance < (NEW.credit_limit * -1) THEN
        RAISE EXCEPTION 'Account balance cannot exceed credit limit: Balance % exceeds limit %', 
            NEW.current_balance, NEW.credit_limit;
    END IF;
    
    -- Validate cycle balance consistency
    IF NEW.current_cycle_credit < 0 OR NEW.current_cycle_debit < 0 THEN
        RAISE EXCEPTION 'Cycle balances cannot be negative: Credit % Debit %', 
            NEW.current_cycle_credit, NEW.current_cycle_debit;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_accounts_balance_validation
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION validate_account_balance_update();

-- Add table and column comments for documentation
COMMENT ON TABLE accounts IS 'Account master data migrated from VSAM ACCTDAT dataset with precise financial field mapping, customer relationships, and disclosure group associations supporting comprehensive account management operations';

COMMENT ON COLUMN accounts.account_id IS 'Primary key: 11-digit account identifier maintaining fixed-width structure from acctdata.txt';
COMMENT ON COLUMN accounts.customer_id IS 'Foreign key reference to customers table establishing customer-account relationship';
COMMENT ON COLUMN accounts.active_status IS 'Account status indicator supporting active/inactive lifecycle management';
COMMENT ON COLUMN accounts.current_balance IS 'Current account balance with DECIMAL(12,2) precision for exact financial calculations';
COMMENT ON COLUMN accounts.credit_limit IS 'Maximum credit limit allowed for the account with DECIMAL(12,2) precision';
COMMENT ON COLUMN accounts.cash_credit_limit IS 'Cash advance credit limit with separate tracking from general credit limit';
COMMENT ON COLUMN accounts.open_date IS 'Account opening date for lifecycle tracking and business rule validation';
COMMENT ON COLUMN accounts.expiration_date IS 'Account expiration date for renewal management and lifecycle operations';
COMMENT ON COLUMN accounts.reissue_date IS 'Card reissue date for security and maintenance operations';
COMMENT ON COLUMN accounts.current_cycle_credit IS 'Current cycle credit transactions total with DECIMAL(12,2) precision';
COMMENT ON COLUMN accounts.current_cycle_debit IS 'Current cycle debit transactions total with DECIMAL(12,2) precision';
COMMENT ON COLUMN accounts.address_zip IS 'Account ZIP code field for location tracking and validation';
COMMENT ON COLUMN accounts.group_id IS 'Foreign key reference to disclosure_groups table for interest rate management';
COMMENT ON COLUMN accounts.created_at IS 'Record creation timestamp with timezone';
COMMENT ON COLUMN accounts.updated_at IS 'Record last modification timestamp with timezone, automatically updated';

-- Grant appropriate permissions for application access
-- Note: Specific role permissions should be configured based on deployment environment
-- Example permissions (adjust based on actual role names in deployment):
-- GRANT SELECT, INSERT, UPDATE ON accounts TO carddemo_app_role;
-- GRANT SELECT ON accounts TO carddemo_read_role;
-- GRANT ALL PRIVILEGES ON accounts TO carddemo_admin_role;

-- Create row-level security policy for account data access
-- Enable row-level security for the accounts table
-- ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- Example RLS policy (uncomment and adjust based on security requirements):
-- CREATE POLICY accounts_access_policy ON accounts
--     FOR ALL TO carddemo_app_role
--     USING (customer_id = current_setting('app.current_customer_id', true));

-- Security note: For production deployment, implement additional security measures:
-- 1. Column-level encryption for sensitive financial data using pgcrypto
-- 2. Row-level security policies for customer data isolation
-- 3. Audit triggers for sensitive data access logging
-- 4. Regular security reviews and access audits

-- Performance optimization notes:
-- 1. Consider partitioning by customer_id for large datasets
-- 2. Monitor query patterns and add additional indexes as needed
-- 3. Regular VACUUM and ANALYZE operations for optimal performance
-- 4. Configure appropriate PostgreSQL shared_buffers and effective_cache_size settings

-- Migration validation notes:
-- 1. Verify all account data from acctdata.txt loads correctly
-- 2. Validate foreign key relationships with customers table
-- 3. Test financial precision calculations match COBOL COMP-3 results
-- 4. Confirm index performance meets sub-200ms response time requirements

-- Rollback instructions:
-- To rollback this migration:
-- 1. DROP TABLE accounts CASCADE;
-- 2. DROP FUNCTION update_accounts_updated_at() CASCADE;
-- 3. DROP FUNCTION validate_account_balance_update() CASCADE;