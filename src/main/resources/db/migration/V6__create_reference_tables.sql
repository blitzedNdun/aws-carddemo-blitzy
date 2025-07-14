-- ============================================================================
-- Liquibase Migration: V6__create_reference_tables.sql
-- Description: Create reference tables for transaction types, categories, 
--              disclosure groups, and transaction category balances
-- Author: Blitzy agent
-- Version: 6.0
-- Dependencies: V3__create_accounts_table.sql (foreign key reference)
-- ============================================================================

-- Create transaction_types table from trantype.txt ASCII data
-- Supports transaction classification with debit/credit indicators
CREATE TABLE transaction_types (
    -- Primary key: 2-character transaction type code from trantype.txt
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Transaction type description supporting business rule identification
    type_description VARCHAR(60) NOT NULL,
    
    -- Debit/Credit indicator for proper transaction classification
    -- TRUE = Debit transaction, FALSE = Credit transaction
    debit_credit_indicator BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields for tracking reference data changes
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_types PRIMARY KEY (transaction_type)
);

-- Create transaction_categories table from trancatg.txt ASCII data
-- Supports hierarchical transaction categorization with 4-character codes
CREATE TABLE transaction_categories (
    -- Primary key: 4-character transaction category code from trancatg.txt
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Category description supporting business operations
    category_description VARCHAR(60) NOT NULL,
    
    -- Parent transaction type for hierarchical support (first 2 characters)
    parent_transaction_type VARCHAR(2) NOT NULL,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields for tracking reference data changes
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_categories PRIMARY KEY (transaction_category)
);

-- Create disclosure_groups table from discgrp.txt ASCII data
-- Supports interest rate management with DECIMAL(5,4) precision
CREATE TABLE disclosure_groups (
    -- Primary key: 10-character group identifier from discgrp.txt
    group_id VARCHAR(10) NOT NULL,
    
    -- Interest rate with DECIMAL(5,4) precision supporting percentage calculations
    -- Range: 0.0001 to 9.9999 (0.01% to 999.99%)
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    
    -- Legal disclosure text for regulatory compliance
    disclosure_text TEXT,
    
    -- Effective date for rate changes and regulatory tracking
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields for tracking reference data changes
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_disclosure_groups PRIMARY KEY (group_id)
);

-- Create transaction_category_balances table from tcatbal.txt ASCII data
-- Supports composite primary key (account_id, transaction_category)
CREATE TABLE transaction_category_balances (
    -- Composite primary key part 1: Account ID with foreign key reference
    account_id VARCHAR(11) NOT NULL,
    
    -- Composite primary key part 2: Transaction category code
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Category balance with DECIMAL(12,2) precision for financial calculations
    category_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Last updated timestamp for balance tracking
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit fields for tracking balance changes
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Composite primary key constraint
    CONSTRAINT pk_transaction_category_balances PRIMARY KEY (account_id, transaction_category)
);

-- Create foreign key constraints for referential integrity
-- Foreign key from transaction_categories to transaction_types
ALTER TABLE transaction_categories ADD CONSTRAINT fk_transaction_categories_parent_type
    FOREIGN KEY (parent_transaction_type) REFERENCES transaction_types(transaction_type)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Foreign key from transaction_category_balances to accounts table
ALTER TABLE transaction_category_balances ADD CONSTRAINT fk_transaction_category_balances_account_id
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Foreign key from transaction_category_balances to transaction_categories
ALTER TABLE transaction_category_balances ADD CONSTRAINT fk_transaction_category_balances_category
    FOREIGN KEY (transaction_category) REFERENCES transaction_categories(transaction_category)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create check constraints for business rule validation
-- Transaction type must be exactly 2 characters
ALTER TABLE transaction_types ADD CONSTRAINT chk_transaction_types_code_format
    CHECK (transaction_type ~ '^[0-9]{2}$');

-- Transaction category must be exactly 4 characters
ALTER TABLE transaction_categories ADD CONSTRAINT chk_transaction_categories_code_format
    CHECK (transaction_category ~ '^[0-9]{4}$');

-- Category parent type must match first 2 characters of category code
ALTER TABLE transaction_categories ADD CONSTRAINT chk_transaction_categories_parent_match
    CHECK (parent_transaction_type = LEFT(transaction_category, 2));

-- Group ID must be exactly 10 characters (alphanumeric)
ALTER TABLE disclosure_groups ADD CONSTRAINT chk_disclosure_groups_id_format
    CHECK (group_id ~ '^[A-Za-z0-9]{10}$');

-- Interest rate must be non-negative and within reasonable range
ALTER TABLE disclosure_groups ADD CONSTRAINT chk_disclosure_groups_interest_rate_range
    CHECK (interest_rate >= 0.0000 AND interest_rate <= 9.9999);

-- Account ID must be exactly 11 numeric digits
ALTER TABLE transaction_category_balances ADD CONSTRAINT chk_tcatbal_account_id_format
    CHECK (account_id ~ '^[0-9]{11}$');

-- Category balance must be within reasonable limits
ALTER TABLE transaction_category_balances ADD CONSTRAINT chk_tcatbal_balance_limits
    CHECK (category_balance >= -9999999999.99 AND category_balance <= 9999999999.99);

-- Create performance indexes for high-speed lookup operations
-- Index on transaction_types for sub-millisecond type lookups
CREATE INDEX idx_transaction_types_active_status ON transaction_types (active_status, transaction_type);

-- Index on transaction_categories for hierarchical queries
CREATE INDEX idx_transaction_categories_parent_type ON transaction_categories (parent_transaction_type, transaction_category);

-- Index on transaction_categories for active status filtering
CREATE INDEX idx_transaction_categories_active_status ON transaction_categories (active_status, transaction_category);

-- Index on disclosure_groups for interest rate queries
CREATE INDEX idx_disclosure_groups_interest_rate ON disclosure_groups (interest_rate, group_id);

-- Index on disclosure_groups for effective date queries
CREATE INDEX idx_disclosure_groups_effective_date ON disclosure_groups (effective_date, group_id);

-- Index on transaction_category_balances for account-based queries
CREATE INDEX idx_tcatbal_account_id ON transaction_category_balances (account_id, category_balance);

-- Index on transaction_category_balances for category-based queries
CREATE INDEX idx_tcatbal_category ON transaction_category_balances (transaction_category, category_balance);

-- Index on transaction_category_balances for balance range queries
CREATE INDEX idx_tcatbal_balance_range ON transaction_category_balances (category_balance, last_updated);

-- Composite index for category balance reporting
CREATE INDEX idx_tcatbal_category_balance_summary ON transaction_category_balances (transaction_category, category_balance, last_updated);

-- Create triggers for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_transaction_types_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transaction_types_updated_at
    BEFORE UPDATE ON transaction_types
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_types_updated_at();

CREATE OR REPLACE FUNCTION update_transaction_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transaction_categories_updated_at
    BEFORE UPDATE ON transaction_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_categories_updated_at();

CREATE OR REPLACE FUNCTION update_disclosure_groups_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_disclosure_groups_updated_at
    BEFORE UPDATE ON disclosure_groups
    FOR EACH ROW
    EXECUTE FUNCTION update_disclosure_groups_updated_at();

CREATE OR REPLACE FUNCTION update_transaction_category_balances_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transaction_category_balances_updated_at
    BEFORE UPDATE ON transaction_category_balances
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_category_balances_updated_at();

-- Create validation triggers for business rule enforcement
CREATE OR REPLACE FUNCTION validate_transaction_category_balance_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate that the account exists and is active
    IF NOT EXISTS (
        SELECT 1 FROM accounts 
        WHERE account_id = NEW.account_id 
        AND active_status = TRUE
    ) THEN
        RAISE EXCEPTION 'Account % does not exist or is not active', NEW.account_id;
    END IF;
    
    -- Validate that the transaction category exists and is active
    IF NOT EXISTS (
        SELECT 1 FROM transaction_categories 
        WHERE transaction_category = NEW.transaction_category 
        AND active_status = TRUE
    ) THEN
        RAISE EXCEPTION 'Transaction category % does not exist or is not active', NEW.transaction_category;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transaction_category_balances_validation
    BEFORE INSERT OR UPDATE ON transaction_category_balances
    FOR EACH ROW
    EXECUTE FUNCTION validate_transaction_category_balance_update();

-- Create sequences for reference data management
CREATE SEQUENCE IF NOT EXISTS transaction_types_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS transaction_categories_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

-- Insert initial reference data from ASCII files
-- Transaction types from trantype.txt
INSERT INTO transaction_types (transaction_type, type_description, debit_credit_indicator, active_status) VALUES
('01', 'Purchase', TRUE, TRUE),
('02', 'Payment', FALSE, TRUE),
('03', 'Credit', FALSE, TRUE),
('04', 'Authorization', TRUE, TRUE),
('05', 'Refund', FALSE, TRUE),
('06', 'Reversal', FALSE, TRUE),
('07', 'Adjustment', TRUE, TRUE);

-- Transaction categories from trancatg.txt
INSERT INTO transaction_categories (transaction_category, category_description, parent_transaction_type, active_status) VALUES
('0001', 'Regular Sales Draft', '01', TRUE),
('0002', 'Regular Cash Advance', '01', TRUE),
('0003', 'Convenience Check Debit', '01', TRUE),
('0004', 'ATM Cash Advance', '01', TRUE),
('0005', 'Interest Amount', '01', TRUE),
('0001', 'Cash payment', '02', TRUE),
('0002', 'Electronic payment', '02', TRUE),
('0003', 'Check payment', '02', TRUE),
('0001', 'Credit to Account', '03', TRUE),
('0002', 'Credit to Purchase balance', '03', TRUE),
('0003', 'Credit to Cash balance', '03', TRUE),
('0001', 'Zero dollar authorization', '04', TRUE),
('0002', 'Online purchase authorization', '04', TRUE),
('0003', 'Travel booking authorization', '04', TRUE),
('0001', 'Refund credit', '05', TRUE),
('0001', 'Fraud reversal', '06', TRUE),
('0002', 'Non-fraud reversal', '06', TRUE),
('0001', 'Sales draft credit adjustment', '07', TRUE);

-- Disclosure groups from discgrp.txt with interest rate precision
INSERT INTO disclosure_groups (group_id, interest_rate, disclosure_text, effective_date, active_status) VALUES
('A         ', 0.0150, 'Standard disclosure group with 1.5% interest rate', CURRENT_DATE, TRUE),
('DEFAULT   ', 0.0250, 'Default disclosure group with 2.5% interest rate', CURRENT_DATE, TRUE),
('ZEROAPR   ', 0.0000, 'Zero APR promotional disclosure group', CURRENT_DATE, TRUE);

-- Add table and column comments for comprehensive documentation
COMMENT ON TABLE transaction_types IS 'Transaction type reference table providing classification codes for all transaction processing operations with debit/credit indicators and active status management';

COMMENT ON COLUMN transaction_types.transaction_type IS 'Primary key: 2-character transaction type code from trantype.txt ASCII data';
COMMENT ON COLUMN transaction_types.type_description IS 'Business-friendly transaction type description supporting user interface display and reporting';
COMMENT ON COLUMN transaction_types.debit_credit_indicator IS 'Boolean flag indicating transaction direction: TRUE for debit transactions, FALSE for credit transactions';
COMMENT ON COLUMN transaction_types.active_status IS 'Active status flag for reference data lifecycle management enabling soft deletion of transaction types';

COMMENT ON TABLE transaction_categories IS 'Transaction category reference table providing hierarchical 4-character codes linked to transaction types for detailed transaction classification';

COMMENT ON COLUMN transaction_categories.transaction_category IS 'Primary key: 4-character transaction category code from trancatg.txt ASCII data';
COMMENT ON COLUMN transaction_categories.category_description IS 'Detailed category description supporting business operations and reporting requirements';
COMMENT ON COLUMN transaction_categories.parent_transaction_type IS 'Foreign key reference to transaction_types table creating hierarchical relationship';
COMMENT ON COLUMN transaction_categories.active_status IS 'Active status flag for reference data lifecycle management enabling category activation/deactivation';

COMMENT ON TABLE disclosure_groups IS 'Disclosure group reference table managing interest rates and legal disclosure text for regulatory compliance with DECIMAL(5,4) precision';

COMMENT ON COLUMN disclosure_groups.group_id IS 'Primary key: 10-character group identifier from discgrp.txt ASCII data';
COMMENT ON COLUMN disclosure_groups.interest_rate IS 'Interest rate with DECIMAL(5,4) precision supporting percentage calculations (0.01% to 999.99%)';
COMMENT ON COLUMN disclosure_groups.disclosure_text IS 'Legal disclosure text for regulatory compliance and customer communication';
COMMENT ON COLUMN disclosure_groups.effective_date IS 'Effective date for rate changes supporting regulatory tracking and historical analysis';

COMMENT ON TABLE transaction_category_balances IS 'Transaction category balance tracking table with composite primary key (account_id, transaction_category) supporting category-specific balance management';

COMMENT ON COLUMN transaction_category_balances.account_id IS 'Foreign key reference to accounts table establishing account-balance relationship';
COMMENT ON COLUMN transaction_category_balances.transaction_category IS 'Foreign key reference to transaction_categories table for category-specific balance tracking';
COMMENT ON COLUMN transaction_category_balances.category_balance IS 'Category balance with DECIMAL(12,2) precision for exact financial calculations';
COMMENT ON COLUMN transaction_category_balances.last_updated IS 'Last updated timestamp for balance tracking and audit trail maintenance';

-- Grant appropriate permissions for application access
-- Note: Specific role permissions should be configured based on deployment environment
-- Example permissions (adjust based on actual role names in deployment):
-- GRANT SELECT ON transaction_types TO carddemo_app_role;
-- GRANT SELECT ON transaction_categories TO carddemo_app_role;
-- GRANT SELECT ON disclosure_groups TO carddemo_app_role;
-- GRANT SELECT, INSERT, UPDATE ON transaction_category_balances TO carddemo_app_role;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO carddemo_read_role;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO carddemo_admin_role;

-- Create materialized views for high-performance lookup operations
CREATE MATERIALIZED VIEW mv_transaction_type_category_hierarchy AS
SELECT 
    tt.transaction_type,
    tt.type_description,
    tt.debit_credit_indicator,
    tc.transaction_category,
    tc.category_description,
    tt.active_status AS type_active,
    tc.active_status AS category_active
FROM transaction_types tt
JOIN transaction_categories tc ON tt.transaction_type = tc.parent_transaction_type
WHERE tt.active_status = TRUE AND tc.active_status = TRUE;

-- Create unique index on materialized view for optimal query performance
CREATE UNIQUE INDEX idx_mv_transaction_hierarchy_unique ON mv_transaction_type_category_hierarchy (transaction_type, transaction_category);

-- Create materialized view for account category balance summary
CREATE MATERIALIZED VIEW mv_account_category_balance_summary AS
SELECT 
    tcb.account_id,
    a.customer_id,
    a.active_status AS account_active,
    COUNT(tcb.transaction_category) AS category_count,
    SUM(tcb.category_balance) AS total_category_balance,
    MAX(tcb.last_updated) AS last_balance_update
FROM transaction_category_balances tcb
JOIN accounts a ON tcb.account_id = a.account_id
GROUP BY tcb.account_id, a.customer_id, a.active_status;

-- Create index on materialized view for account queries
CREATE INDEX idx_mv_account_balance_summary_account ON mv_account_category_balance_summary (account_id, total_category_balance);

-- Create index on materialized view for customer queries
CREATE INDEX idx_mv_account_balance_summary_customer ON mv_account_category_balance_summary (customer_id, total_category_balance);

-- Schedule automatic refresh of materialized views
-- Note: In production, these should be scheduled via cron jobs or Kubernetes CronJobs
-- Example refresh commands:
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_type_category_hierarchy;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_category_balance_summary;

-- Performance optimization notes:
-- 1. Reference tables are designed for high-frequency read operations with minimal writes
-- 2. Indexes are optimized for lookup operations supporting sub-millisecond response times
-- 3. Composite primary key on transaction_category_balances enables efficient category-based queries
-- 4. Materialized views provide pre-computed joins for complex queries
-- 5. Check constraints ensure data integrity and business rule compliance

-- Migration validation notes:
-- 1. Verify all reference data from ASCII files loads correctly
-- 2. Validate foreign key relationships with accounts table
-- 3. Test interest rate precision calculations match financial requirements
-- 4. Confirm index performance meets sub-millisecond response time requirements
-- 5. Validate materialized view refresh operations complete within acceptable timeframes

-- Rollback instructions:
-- To rollback this migration:
-- 1. DROP MATERIALIZED VIEW mv_account_category_balance_summary CASCADE;
-- 2. DROP MATERIALIZED VIEW mv_transaction_type_category_hierarchy CASCADE;
-- 3. DROP TABLE transaction_category_balances CASCADE;
-- 4. DROP TABLE disclosure_groups CASCADE;
-- 5. DROP TABLE transaction_categories CASCADE;
-- 6. DROP TABLE transaction_types CASCADE;
-- 7. DROP SEQUENCE transaction_types_seq CASCADE;
-- 8. DROP SEQUENCE transaction_categories_seq CASCADE;
-- 9. DROP FUNCTION update_transaction_types_updated_at() CASCADE;
-- 10. DROP FUNCTION update_transaction_categories_updated_at() CASCADE;
-- 11. DROP FUNCTION update_disclosure_groups_updated_at() CASCADE;
-- 12. DROP FUNCTION update_transaction_category_balances_updated_at() CASCADE;
-- 13. DROP FUNCTION validate_transaction_category_balance_update() CASCADE;