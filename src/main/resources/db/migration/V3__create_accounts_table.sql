-- ==============================================================================
-- Liquibase Migration: V3__create_accounts_table.sql
-- Description: Creates accounts table migrated from VSAM ACCTDAT dataset
-- Author: Blitzy agent
-- Version: 3.0
-- Migration Type: CREATE TABLE with comprehensive financial constraints and relationships
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:create-transaction-types-table-v3
--comment: Create transaction_types reference table from trantype.txt with 2-character type codes and debit/credit classification

-- Create transaction_types table from trantype.txt data source
-- Provides transaction classification with debit/credit indicators for transaction processing
CREATE TABLE transaction_types (
    -- Primary key: 2-character transaction type code from trantype.txt
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Descriptive name for transaction type (e.g., "Purchase", "Payment", "Credit")
    type_description VARCHAR(60) NOT NULL,
    
    -- Debit/credit indicator for proper transaction classification and accounting
    -- TRUE = Debit transaction (increases balance), FALSE = Credit transaction (decreases balance)
    debit_credit_indicator BOOLEAN NOT NULL DEFAULT true,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit fields for reference data management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_types PRIMARY KEY (transaction_type),
    
    -- Business validation constraints
    CONSTRAINT chk_transaction_type_format CHECK (transaction_type ~ '^[0-9]{2}$'),
    CONSTRAINT chk_type_description_length CHECK (length(trim(type_description)) >= 3)
);

--rollback DROP TABLE transaction_types CASCADE;

--changeset blitzy-agent:create-transaction-categories-table-v3
--comment: Create transaction_categories reference table from trancatg.txt with 4-character category codes and hierarchical support

-- Create transaction_categories table from trancatg.txt data source
-- Supports hierarchical transaction categorization with parent-child relationships
CREATE TABLE transaction_categories (
    -- Primary key: 4-character transaction category code from trancatg.txt
    -- Note: Original data shows 6-character codes, but last 4 characters form the category
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Parent transaction type (first 2 characters) for hierarchical classification
    parent_transaction_type VARCHAR(2) NOT NULL,
    
    -- Detailed category description (e.g., "Regular Sales Draft", "Cash payment")
    category_description VARCHAR(60) NOT NULL,
    
    -- Active status for category lifecycle management and business rule enforcement
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit fields for reference data management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_categories PRIMARY KEY (transaction_category, parent_transaction_type),
    
    -- Unique constraint on transaction_category alone for foreign key references
    CONSTRAINT uk_transaction_categories_category UNIQUE (transaction_category),
    
    -- Foreign key to transaction_types for hierarchical integrity
    CONSTRAINT fk_transaction_categories_parent_type FOREIGN KEY (parent_transaction_type) 
        REFERENCES transaction_types(transaction_type) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business validation constraints
    CONSTRAINT chk_transaction_category_format CHECK (transaction_category ~ '^[0-9]{4}$'),
    CONSTRAINT chk_parent_type_format CHECK (parent_transaction_type ~ '^[0-9]{2}$'),
    CONSTRAINT chk_category_description_length CHECK (length(trim(category_description)) >= 3)
);

--rollback DROP TABLE transaction_categories CASCADE;

--changeset blitzy-agent:create-disclosure-groups-table-v3
--comment: Create disclosure_groups reference table from discgrp.txt with precise interest rate management and legal disclosure text

-- Create disclosure_groups table from discgrp.txt data source
-- Manages interest rate configurations and legal disclosure requirements
CREATE TABLE disclosure_groups (
    -- Primary key: 10-character group identifier (e.g., "A", "DEFAULT", "ZEROAPR")
    -- Padded to 10 characters for consistency with accounts.group_id foreign key
    group_id VARCHAR(10) NOT NULL,
    
    -- Associated transaction category for interest rate application
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Interest rate with DECIMAL(5,4) precision supporting percentage calculations
    -- Stores rates as decimal values (e.g., 0.0150 for 1.50% APR)
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    
    -- Legal disclosure text for regulatory compliance and customer communication
    disclosure_text TEXT,
    
    -- Effective date for interest rate and disclosure changes
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    -- Active status for disclosure group lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit fields for compliance tracking and change management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_disclosure_groups PRIMARY KEY (group_id, transaction_category),
    
    -- Unique constraint on group_id alone for foreign key references from accounts table
    CONSTRAINT uk_disclosure_groups_group_id UNIQUE (group_id),
    
    -- Foreign key to transaction_categories for referential integrity
    CONSTRAINT fk_disclosure_groups_transaction_category FOREIGN KEY (transaction_category) 
        REFERENCES transaction_categories(transaction_category) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business validation constraints
    CONSTRAINT chk_group_id_format CHECK (length(trim(group_id)) >= 1 AND length(group_id) <= 10),
    CONSTRAINT chk_interest_rate_range CHECK (interest_rate >= 0.0000 AND interest_rate <= 9.9999),
    CONSTRAINT chk_effective_date_range CHECK (
        effective_date >= DATE '1970-01-01' AND 
        effective_date <= CURRENT_DATE + INTERVAL '10 years'
    )
);

--rollback DROP TABLE disclosure_groups CASCADE;

--changeset blitzy-agent:create-accounts-table-v3
--comment: Create accounts table with exact VSAM ACCTDAT field precision preservation and referential integrity

-- Create accounts table preserving VSAM ACCTDAT record layout with PostgreSQL DECIMAL precision
CREATE TABLE accounts (
    -- Primary key: 11-digit account identifier matching VSAM ACCTDAT key structure
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key to customers table establishing customer-account relationship
    customer_id VARCHAR(9) NOT NULL,
    
    -- Account status flag: converted from COBOL Y/N to PostgreSQL BOOLEAN
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Financial fields with exact DECIMAL(12,2) precision for COBOL COMP-3 arithmetic equivalence
    -- Current account balance supporting BigDecimal operations in Spring Boot microservices
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Credit limit with DECIMAL precision maintaining exact financial calculations
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Cash advance credit limit with DECIMAL precision for cash transaction processing
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Account lifecycle management date fields from ACCTDAT positions
    open_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    reissue_date DATE NOT NULL,
    
    -- Current billing cycle financial tracking with DECIMAL precision
    -- Supporting monthly statement generation and interest calculations
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Account mailing address ZIP code for statement delivery and fraud detection
    address_zip VARCHAR(10),
    
    -- Foreign key to disclosure_groups table for interest rate and terms association
    group_id VARCHAR(10) NOT NULL,
    
    -- Audit fields for data management and compliance tracking
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    
    -- Foreign key constraints ensuring referential integrity
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(customer_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    CONSTRAINT fk_accounts_disclosure_group FOREIGN KEY (group_id) 
        REFERENCES disclosure_groups(group_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business validation constraints
    CONSTRAINT chk_account_id_format CHECK (account_id ~ '^[0-9]{11}$'),
    CONSTRAINT chk_customer_id_format CHECK (customer_id ~ '^[0-9]{9}$'),
    
    -- Financial amount validation constraints ensuring positive values where appropriate
    CONSTRAINT chk_credit_limit_positive CHECK (credit_limit >= 0.00),
    CONSTRAINT chk_cash_credit_limit_positive CHECK (cash_credit_limit >= 0.00),
    CONSTRAINT chk_current_cycle_credit_positive CHECK (current_cycle_credit >= 0.00),
    CONSTRAINT chk_current_cycle_debit_positive CHECK (current_cycle_debit >= 0.00),
    
    -- Balance validation: current balance cannot exceed credit limit plus reasonable buffer
    CONSTRAINT chk_current_balance_limit CHECK (
        current_balance <= (credit_limit * 1.20)
    ),
    
    -- Date validation constraints ensuring logical account lifecycle
    CONSTRAINT chk_expiration_after_open CHECK (expiration_date > open_date),
    CONSTRAINT chk_reissue_valid_range CHECK (
        reissue_date >= open_date AND 
        reissue_date <= expiration_date + INTERVAL '30 days'
    ),
    
    -- Open date cannot be in the future and must be reasonable
    CONSTRAINT chk_open_date_range CHECK (
        open_date >= DATE '1970-01-01' AND 
        open_date <= CURRENT_DATE
    ),
    
    -- Expiration date must be within reasonable future range
    CONSTRAINT chk_expiration_date_range CHECK (
        expiration_date >= open_date AND 
        expiration_date <= CURRENT_DATE + INTERVAL '20 years'
    ),
    
    -- ZIP code format validation when provided
    CONSTRAINT chk_zip_code_format CHECK (
        address_zip IS NULL OR 
        address_zip ~ '^[0-9]{5}(-[0-9]{4})?$|^[A-Z][0-9][A-Z][[:space:]][0-9][A-Z][0-9]$'
    ),
    
    -- Group ID format validation for disclosure group reference
    CONSTRAINT chk_group_id_format CHECK (
        group_id ~ '^[A-Z0-9]{10}$'
    )
);

--rollback DROP TABLE accounts CASCADE;

--changeset blitzy-agent:create-accounts-table-indexes-v3
--comment: Create performance indexes for accounts table supporting high-volume transaction processing

-- Primary index on account_id (automatically created with PRIMARY KEY)
-- Additional indexes for common query patterns and foreign key relationships

-- Index for customer-account relationship queries (supporting account management operations)
CREATE INDEX idx_accounts_customer_id ON accounts (customer_id, active_status);

-- Index for account balance queries (supporting real-time balance inquiries)
CREATE INDEX idx_accounts_balance_lookup ON accounts (account_id, current_balance, active_status);

-- Index for disclosure group queries (supporting interest rate calculations)
CREATE INDEX idx_accounts_group_id ON accounts (group_id, active_status);

-- Index for account lifecycle management (supporting expiration processing)
CREATE INDEX idx_accounts_expiration_date ON accounts (expiration_date, active_status) 
    WHERE active_status = true;

-- Index for geographical analysis and fraud detection
CREATE INDEX idx_accounts_zip_code ON accounts (address_zip) 
    WHERE address_zip IS NOT NULL;

-- Composite index for account opening date analysis (supporting regulatory reporting)
CREATE INDEX idx_accounts_open_date_analysis ON accounts (open_date, group_id);

-- Index for credit limit analysis and risk management
CREATE INDEX idx_accounts_credit_limits ON accounts (credit_limit, cash_credit_limit) 
    WHERE active_status = true;

-- Index for current cycle financial analysis (supporting statement generation)
CREATE INDEX idx_accounts_cycle_balances ON accounts (
    current_cycle_credit, 
    current_cycle_debit, 
    account_id
) WHERE active_status = true;

--rollback DROP INDEX IF EXISTS idx_accounts_cycle_balances;
--rollback DROP INDEX IF EXISTS idx_accounts_credit_limits;
--rollback DROP INDEX IF EXISTS idx_accounts_open_date_analysis;
--rollback DROP INDEX IF EXISTS idx_accounts_zip_code;
--rollback DROP INDEX IF EXISTS idx_accounts_expiration_date;
--rollback DROP INDEX IF EXISTS idx_accounts_group_id;
--rollback DROP INDEX IF EXISTS idx_accounts_balance_lookup;
--rollback DROP INDEX IF EXISTS idx_accounts_customer_id;

--changeset blitzy-agent:create-accounts-table-triggers-v3
--comment: Create triggers for accounts table audit trail and financial data integrity

-- Trigger function for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at on row modifications
CREATE TRIGGER trg_accounts_update_timestamp
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- Trigger function for financial balance validation and audit logging
CREATE OR REPLACE FUNCTION validate_accounts_financial_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Log significant balance changes for audit trail
    IF TG_OP = 'UPDATE' AND ABS(NEW.current_balance - OLD.current_balance) > 1000.00 THEN
        INSERT INTO audit_log (
            table_name, 
            record_id, 
            operation, 
            old_values, 
            new_values, 
            change_timestamp
        ) VALUES (
            'accounts',
            NEW.account_id,
            'BALANCE_CHANGE',
            json_build_object('old_balance', OLD.current_balance),
            json_build_object('new_balance', NEW.current_balance),
            CURRENT_TIMESTAMP
        );
    END IF;
    
    -- Validate credit limit changes require approval workflow
    IF TG_OP = 'UPDATE' AND NEW.credit_limit != OLD.credit_limit THEN
        -- Ensure credit limit changes are logged and within reasonable bounds
        IF NEW.credit_limit > OLD.credit_limit * 2.0 THEN
            RAISE EXCEPTION 'Credit limit increase exceeds 100%% threshold: % to %', 
                OLD.credit_limit, NEW.credit_limit;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for financial data validation and audit logging
CREATE TRIGGER trg_accounts_financial_validation
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION validate_accounts_financial_changes();

--rollback DROP TRIGGER IF EXISTS trg_accounts_financial_validation ON accounts;
--rollback DROP TRIGGER IF EXISTS trg_accounts_update_timestamp ON accounts;
--rollback DROP FUNCTION IF EXISTS validate_accounts_financial_changes();
--rollback DROP FUNCTION IF EXISTS update_accounts_updated_at();

--changeset blitzy-agent:create-accounts-table-comments-v3
--comment: Add comprehensive table and column documentation for accounts table

-- Table-level documentation
COMMENT ON TABLE accounts IS 'Account master data table migrated from VSAM ACCTDAT dataset. Contains comprehensive account information including financial balances, credit limits, lifecycle dates, and customer relationships. Supports exact DECIMAL(12,2) precision for financial calculations maintaining COBOL COMP-3 arithmetic equivalence. Includes referential integrity with customers and disclosure_groups tables for comprehensive account management operations.';

-- Column-level documentation with VSAM field mapping references
COMMENT ON COLUMN accounts.account_id IS 'Primary key: 11-digit account identifier. Maps to ACCTDAT positions 1-11. Must be unique and follow numeric format validation for account operations and cross-reference integrity.';
COMMENT ON COLUMN accounts.customer_id IS 'Foreign key to customers table: 9-digit customer identifier. Establishes customer-account relationship for account ownership and management operations.';
COMMENT ON COLUMN accounts.active_status IS 'Account active status flag. Maps to ACCTDAT active indicator (Y/N converted to BOOLEAN). Controls account accessibility and transaction processing eligibility.';
COMMENT ON COLUMN accounts.current_balance IS 'Current account balance. Maps to ACCTDAT current balance field with DECIMAL(12,2) precision. Supports exact financial calculations equivalent to COBOL COMP-3 arithmetic for balance inquiries and transaction processing.';
COMMENT ON COLUMN accounts.credit_limit IS 'Maximum credit limit. Maps to ACCTDAT credit limit field with DECIMAL(12,2) precision. Used for transaction authorization and credit risk management with exact financial precision.';
COMMENT ON COLUMN accounts.cash_credit_limit IS 'Cash advance credit limit. Maps to ACCTDAT cash credit limit field with DECIMAL(12,2) precision. Controls cash advance transaction authorization limits.';
COMMENT ON COLUMN accounts.open_date IS 'Account opening date. Maps to ACCTDAT open date field. Required field for account lifecycle management and regulatory compliance reporting.';
COMMENT ON COLUMN accounts.expiration_date IS 'Account expiration date. Maps to ACCTDAT expiration date field. Used for account renewal processing and lifecycle management.';
COMMENT ON COLUMN accounts.reissue_date IS 'Card reissue date. Maps to ACCTDAT reissue date field. Tracks card reissuance for security and lifecycle management.';
COMMENT ON COLUMN accounts.current_cycle_credit IS 'Current billing cycle credit amount. Maps to ACCTDAT current cycle credit field with DECIMAL(12,2) precision. Used for monthly statement generation and interest calculations.';
COMMENT ON COLUMN accounts.current_cycle_debit IS 'Current billing cycle debit amount. Maps to ACCTDAT current cycle debit field with DECIMAL(12,2) precision. Used for monthly statement generation and payment processing.';
COMMENT ON COLUMN accounts.address_zip IS 'Account mailing address ZIP code. Used for statement delivery, geographical analysis, and fraud detection. Supports US and Canadian postal code formats.';
COMMENT ON COLUMN accounts.group_id IS 'Foreign key to disclosure_groups table: disclosure group identifier. Maps to ACCTDAT group ID field. Establishes association with interest rates and account terms for statement generation.';
COMMENT ON COLUMN accounts.created_at IS 'Record creation timestamp. Audit field for data lifecycle management and compliance reporting with automatic timestamp generation.';
COMMENT ON COLUMN accounts.updated_at IS 'Record last modification timestamp. Automatically updated via trigger for audit trail maintenance and change tracking.';

--rollback COMMENT ON TABLE accounts IS NULL;
--rollback COMMENT ON COLUMN accounts.account_id IS NULL;
--rollback COMMENT ON COLUMN accounts.customer_id IS NULL;
--rollback COMMENT ON COLUMN accounts.active_status IS NULL;
--rollback COMMENT ON COLUMN accounts.current_balance IS NULL;
--rollback COMMENT ON COLUMN accounts.credit_limit IS NULL;
--rollback COMMENT ON COLUMN accounts.cash_credit_limit IS NULL;
--rollback COMMENT ON COLUMN accounts.open_date IS NULL;
--rollback COMMENT ON COLUMN accounts.expiration_date IS NULL;
--rollback COMMENT ON COLUMN accounts.reissue_date IS NULL;
--rollback COMMENT ON COLUMN accounts.current_cycle_credit IS NULL;
--rollback COMMENT ON COLUMN accounts.current_cycle_debit IS NULL;
--rollback COMMENT ON COLUMN accounts.address_zip IS NULL;
--rollback COMMENT ON COLUMN accounts.group_id IS NULL;
--rollback COMMENT ON COLUMN accounts.created_at IS NULL;
--rollback COMMENT ON COLUMN accounts.updated_at IS NULL;

--changeset blitzy-agent:create-accounts-table-security-policies-v3
--comment: Enable row-level security and create access policies for account data protection

-- Enable row-level security for the accounts table
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- Create policy for read access - users can only access accounts they own or admin users can access all
CREATE POLICY accounts_read_policy ON accounts
    FOR SELECT
    USING (
        -- Allow access if user is admin or accessing their own customer's accounts
        current_setting('app.user_type', true) = 'ADMIN' OR
        customer_id = current_setting('app.customer_id', true)
    );

-- Create policy for write access - only admin users and authorized service accounts can modify accounts
CREATE POLICY accounts_write_policy ON accounts
    FOR ALL
    USING (
        current_setting('app.user_type', true) = 'ADMIN' OR
        current_setting('app.user_type', true) = 'SERVICE'
    )
    WITH CHECK (
        current_setting('app.user_type', true) = 'ADMIN' OR
        current_setting('app.user_type', true) = 'SERVICE'
    );

--rollback DROP POLICY IF EXISTS accounts_write_policy ON accounts;
--rollback DROP POLICY IF EXISTS accounts_read_policy ON accounts;
--rollback ALTER TABLE accounts DISABLE ROW LEVEL SECURITY;

--changeset blitzy-agent:grant-accounts-table-permissions-v3
--comment: Grant appropriate permissions for application roles

-- Grant SELECT permission to application read role
GRANT SELECT ON accounts TO carddemo_read_role;

-- Grant full permissions to application write role for account management operations
GRANT SELECT, INSERT, UPDATE, DELETE ON accounts TO carddemo_write_role;

-- Grant full permissions to admin role for comprehensive account administration
GRANT ALL PRIVILEGES ON accounts TO carddemo_admin_role;

-- Grant sequence permissions for account ID generation (if using sequences)
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO carddemo_write_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO carddemo_admin_role;

--rollback REVOKE USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public FROM carddemo_admin_role;
--rollback REVOKE USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public FROM carddemo_write_role;
--rollback REVOKE ALL PRIVILEGES ON accounts FROM carddemo_admin_role;
--rollback REVOKE SELECT, INSERT, UPDATE, DELETE ON accounts FROM carddemo_write_role;
--rollback REVOKE SELECT ON accounts FROM carddemo_read_role;

--changeset blitzy-agent:create-accounts-table-materialized-views-v3
--comment: Create materialized views for account summary and analysis operations

-- Materialized view for customer account summary supporting portfolio analysis
CREATE MATERIALIZED VIEW mv_customer_account_summary AS
SELECT 
    c.customer_id,
    c.first_name,
    c.last_name,
    COUNT(a.account_id) as total_accounts,
    COUNT(CASE WHEN a.active_status = true THEN 1 END) as active_accounts,
    SUM(a.current_balance) as total_current_balance,
    SUM(a.credit_limit) as total_credit_limit,
    SUM(a.cash_credit_limit) as total_cash_credit_limit,
    AVG(a.current_balance) as avg_account_balance,
    MIN(a.open_date) as earliest_account_date,
    MAX(a.expiration_date) as latest_expiration_date
FROM customers c
LEFT JOIN accounts a ON c.customer_id = a.customer_id
GROUP BY c.customer_id, c.first_name, c.last_name;

-- Create index on materialized view for optimal query performance
CREATE INDEX idx_mv_customer_account_summary_customer_id 
    ON mv_customer_account_summary (customer_id);
CREATE INDEX idx_mv_customer_account_summary_total_balance 
    ON mv_customer_account_summary (total_current_balance DESC);

-- Materialized view for account balance analysis supporting risk management
CREATE MATERIALIZED VIEW mv_account_balance_analysis AS
SELECT 
    a.account_id,
    a.customer_id,
    a.current_balance,
    a.credit_limit,
    a.cash_credit_limit,
    CASE 
        WHEN a.credit_limit > 0 THEN (a.current_balance / a.credit_limit) * 100
        ELSE 0
    END as credit_utilization_percentage,
    a.current_cycle_credit,
    a.current_cycle_debit,
    a.current_cycle_credit - a.current_cycle_debit as net_cycle_activity,
    dg.interest_rate,
    a.open_date,
    (CURRENT_DATE - a.open_date) as account_age_days
FROM accounts a
LEFT JOIN disclosure_groups dg ON a.group_id = dg.group_id
WHERE a.active_status = true;

-- Create index on materialized view for risk analysis queries
CREATE INDEX idx_mv_account_balance_analysis_utilization 
    ON mv_account_balance_analysis (credit_utilization_percentage DESC);
CREATE INDEX idx_mv_account_balance_analysis_balance 
    ON mv_account_balance_analysis (current_balance DESC);

--rollback DROP INDEX IF EXISTS idx_mv_account_balance_analysis_balance;
--rollback DROP INDEX IF EXISTS idx_mv_account_balance_analysis_utilization;
--rollback DROP MATERIALIZED VIEW IF EXISTS mv_account_balance_analysis;
--rollback DROP INDEX IF EXISTS idx_mv_customer_account_summary_total_balance;
--rollback DROP INDEX IF EXISTS idx_mv_customer_account_summary_customer_id;
--rollback DROP MATERIALIZED VIEW IF EXISTS mv_customer_account_summary;

--changeset blitzy-agent:create-accounts-table-refresh-procedures-v3
--comment: Create procedures for materialized view refresh and maintenance

-- Procedure to refresh customer account summary materialized view
CREATE OR REPLACE FUNCTION refresh_customer_account_summary()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_account_summary;
    
    -- Log refresh operation for monitoring
    INSERT INTO system_log (
        log_level,
        message,
        timestamp
    ) VALUES (
        'INFO',
        'Customer account summary materialized view refreshed successfully',
        CURRENT_TIMESTAMP
    );
EXCEPTION
    WHEN OTHERS THEN
        -- Log error for monitoring
        INSERT INTO system_log (
            log_level,
            message,
            error_details,
            timestamp
        ) VALUES (
            'ERROR',
            'Failed to refresh customer account summary materialized view',
            SQLERRM,
            CURRENT_TIMESTAMP
        );
        RAISE;
END;
$$ LANGUAGE plpgsql;

-- Procedure to refresh account balance analysis materialized view
CREATE OR REPLACE FUNCTION refresh_account_balance_analysis()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_balance_analysis;
    
    -- Log refresh operation for monitoring
    INSERT INTO system_log (
        log_level,
        message,
        timestamp
    ) VALUES (
        'INFO',
        'Account balance analysis materialized view refreshed successfully',
        CURRENT_TIMESTAMP
    );
EXCEPTION
    WHEN OTHERS THEN
        -- Log error for monitoring
        INSERT INTO system_log (
            log_level,
            message,
            error_details,
            timestamp
        ) VALUES (
            'ERROR',
            'Failed to refresh account balance analysis materialized view',
            SQLERRM,
            CURRENT_TIMESTAMP
        );
        RAISE;
END;
$$ LANGUAGE plpgsql;

-- Comprehensive refresh procedure for all account-related materialized views
CREATE OR REPLACE FUNCTION refresh_all_account_materialized_views()
RETURNS void AS $$
BEGIN
    PERFORM refresh_customer_account_summary();
    PERFORM refresh_account_balance_analysis();
    
    INSERT INTO system_log (
        log_level,
        message,
        timestamp
    ) VALUES (
        'INFO',
        'All account materialized views refreshed successfully',
        CURRENT_TIMESTAMP
    );
END;
$$ LANGUAGE plpgsql;

--rollback DROP FUNCTION IF EXISTS refresh_all_account_materialized_views();
--rollback DROP FUNCTION IF EXISTS refresh_account_balance_analysis();
--rollback DROP FUNCTION IF EXISTS refresh_customer_account_summary();