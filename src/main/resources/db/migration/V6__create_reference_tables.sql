-- =====================================================================================
-- Liquibase Migration: V6__create_reference_tables.sql
-- Description: Creates reference tables for transaction types, categories, disclosure
--              groups, and transaction category balances from ASCII data sources with
--              precise data types and relationships for comprehensive system configuration
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 6.0
-- Dependencies: V3__create_accounts_table.sql (accounts table must exist)
-- =====================================================================================

-- changeset blitzy:V6-create-reference-tables
-- comment: Create reference tables from ASCII data sources supporting lookup operations with sub-millisecond response times

-- =============================================================================
-- 1. CREATE TRANSACTION_TYPES TABLE
-- =============================================================================
-- Create transaction_types table from trantype.txt with 2-character type codes
CREATE TABLE transaction_types (
    -- Primary key: 2-character transaction type code
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Transaction type description from ASCII data
    type_description VARCHAR(60) NOT NULL,
    
    -- Debit/credit indicator for proper transaction classification
    debit_credit_indicator BOOLEAN NOT NULL DEFAULT false,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit and tracking fields for compliance
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT transaction_types_pkey PRIMARY KEY (transaction_type)
);

-- Create index for high-performance lookups
CREATE INDEX idx_transaction_types_active_status ON transaction_types(active_status);
CREATE INDEX idx_transaction_types_description ON transaction_types(type_description);

-- Add CHECK constraints for data validation
ALTER TABLE transaction_types 
ADD CONSTRAINT transaction_types_transaction_type_check 
CHECK (transaction_type ~ '^[0-9]{2}$');

ALTER TABLE transaction_types 
ADD CONSTRAINT transaction_types_type_description_check 
CHECK (LENGTH(TRIM(type_description)) > 0);

-- =============================================================================
-- 2. CREATE TRANSACTION_CATEGORIES TABLE
-- =============================================================================
-- Create transaction_categories table from trancatg.txt with 4-character category codes
CREATE TABLE transaction_categories (
    -- Primary key: 4-character transaction category code
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Transaction category description from ASCII data
    category_description VARCHAR(60) NOT NULL,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit and tracking fields for compliance
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT transaction_categories_pkey PRIMARY KEY (transaction_category)
);

-- Create index for high-performance lookups
CREATE INDEX idx_transaction_categories_active_status ON transaction_categories(active_status);
CREATE INDEX idx_transaction_categories_description ON transaction_categories(category_description);

-- Add CHECK constraints for data validation
ALTER TABLE transaction_categories 
ADD CONSTRAINT transaction_categories_transaction_category_check 
CHECK (transaction_category ~ '^[0-9]{4}$');

ALTER TABLE transaction_categories 
ADD CONSTRAINT transaction_categories_category_description_check 
CHECK (LENGTH(TRIM(category_description)) > 0);

-- =============================================================================
-- 3. CREATE DISCLOSURE_GROUPS TABLE
-- =============================================================================
-- Create disclosure_groups table from discgrp.txt with interest rate management
CREATE TABLE disclosure_groups (
    -- Primary key: group identifier
    group_id VARCHAR(10) NOT NULL,
    
    -- Legal disclosure text for compliance
    disclosure_text TEXT NOT NULL DEFAULT 'Standard disclosure terms apply.',
    
    -- Interest rate with DECIMAL(5,4) precision for percentage calculations
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    
    -- Effective date for rate management
    effective_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit and tracking fields for compliance
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT disclosure_groups_pkey PRIMARY KEY (group_id)
);

-- Create indexes for high-performance lookups and financial calculations
CREATE INDEX idx_disclosure_groups_active_status ON disclosure_groups(active_status);
CREATE INDEX idx_disclosure_groups_interest_rate ON disclosure_groups(interest_rate);
CREATE INDEX idx_disclosure_groups_effective_date ON disclosure_groups(effective_date);

-- Add CHECK constraints for data validation and business rules
ALTER TABLE disclosure_groups 
ADD CONSTRAINT disclosure_groups_group_id_check 
CHECK (LENGTH(TRIM(group_id)) > 0);

ALTER TABLE disclosure_groups 
ADD CONSTRAINT disclosure_groups_interest_rate_check 
CHECK (interest_rate >= 0.0000 AND interest_rate <= 9.9999);

ALTER TABLE disclosure_groups 
ADD CONSTRAINT disclosure_groups_effective_date_check 
CHECK (effective_date >= '1900-01-01' AND effective_date <= CURRENT_TIMESTAMP + INTERVAL '10 years');

-- =============================================================================
-- 4. CREATE TRANSACTION_CATEGORY_BALANCES TABLE
-- =============================================================================
-- Create transaction_category_balances table from tcatbal.txt with composite primary key
CREATE TABLE transaction_category_balances (
    -- Composite primary key part 1: account_id foreign key
    account_id VARCHAR(11) NOT NULL,
    
    -- Composite primary key part 2: transaction_category foreign key
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Category balance with DECIMAL(12,2) precision for financial calculations
    category_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Last updated timestamp for tracking balance changes
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit and tracking fields for compliance
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Composite primary key constraint
    CONSTRAINT transaction_category_balances_pkey PRIMARY KEY (account_id, transaction_category)
);

-- Create foreign key constraint to accounts table
ALTER TABLE transaction_category_balances 
ADD CONSTRAINT transaction_category_balances_account_id_fkey 
FOREIGN KEY (account_id) REFERENCES accounts(account_id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- Create foreign key constraint to transaction_categories table
ALTER TABLE transaction_category_balances 
ADD CONSTRAINT transaction_category_balances_transaction_category_fkey 
FOREIGN KEY (transaction_category) REFERENCES transaction_categories(transaction_category)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create indexes for high-performance lookups
CREATE INDEX idx_transaction_category_balances_account_id ON transaction_category_balances(account_id);
CREATE INDEX idx_transaction_category_balances_transaction_category ON transaction_category_balances(transaction_category);
CREATE INDEX idx_transaction_category_balances_category_balance ON transaction_category_balances(category_balance);
CREATE INDEX idx_transaction_category_balances_last_updated ON transaction_category_balances(last_updated);

-- Add CHECK constraints for data validation and business rules
ALTER TABLE transaction_category_balances 
ADD CONSTRAINT transaction_category_balances_account_id_check 
CHECK (account_id ~ '^[0-9]{11}$');

ALTER TABLE transaction_category_balances 
ADD CONSTRAINT transaction_category_balances_category_balance_check 
CHECK (category_balance >= -99999999.99 AND category_balance <= 99999999.99);

ALTER TABLE transaction_category_balances 
ADD CONSTRAINT transaction_category_balances_last_updated_check 
CHECK (last_updated >= '1900-01-01' AND last_updated <= CURRENT_TIMESTAMP + INTERVAL '1 day');

-- =============================================================================
-- 5. CREATE TRIGGERS FOR AUTOMATIC TIMESTAMP UPDATES
-- =============================================================================

-- Create trigger function for transaction_types table
CREATE OR REPLACE FUNCTION update_transaction_types_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER transaction_types_updated_at_trigger
    BEFORE UPDATE ON transaction_types
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_types_updated_at();

-- Create trigger function for transaction_categories table
CREATE OR REPLACE FUNCTION update_transaction_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER transaction_categories_updated_at_trigger
    BEFORE UPDATE ON transaction_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_categories_updated_at();

-- Create trigger function for disclosure_groups table
CREATE OR REPLACE FUNCTION update_disclosure_groups_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER disclosure_groups_updated_at_trigger
    BEFORE UPDATE ON disclosure_groups
    FOR EACH ROW
    EXECUTE FUNCTION update_disclosure_groups_updated_at();

-- Create trigger function for transaction_category_balances table
CREATE OR REPLACE FUNCTION update_transaction_category_balances_last_updated()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER transaction_category_balances_last_updated_trigger
    BEFORE UPDATE ON transaction_category_balances
    FOR EACH ROW
    EXECUTE FUNCTION update_transaction_category_balances_last_updated();

-- =============================================================================
-- 6. INSERT INITIAL DATA FROM ASCII SOURCES
-- =============================================================================

-- Insert transaction types from trantype.txt
INSERT INTO transaction_types (transaction_type, type_description, debit_credit_indicator, active_status) VALUES
('01', 'Purchase', true, true),
('02', 'Payment', false, true),
('03', 'Credit', false, true),
('04', 'Authorization', true, true),
('05', 'Refund', false, true),
('06', 'Reversal', false, true),
('07', 'Adjustment', true, true);

-- Insert transaction categories from trancatg.txt (using first 4 characters of category codes)
INSERT INTO transaction_categories (transaction_category, category_description, active_status) VALUES
('0100', 'Regular Sales Draft', true),
('0100', 'Regular Cash Advance', true),
('0100', 'Convenience Check Debit', true),
('0100', 'ATM Cash Advance', true),
('0100', 'Interest Amount', true),
('0200', 'Cash payment', true),
('0200', 'Electronic payment', true),
('0200', 'Check payment', true),
('0300', 'Credit to Account', true),
('0300', 'Credit to Purchase balance', true),
('0300', 'Credit to Cash balance', true),
('0400', 'Zero dollar authorization', true),
('0400', 'Online purchase authorization', true),
('0400', 'Travel booking authorization', true),
('0500', 'Refund credit', true),
('0600', 'Fraud reversal', true),
('0600', 'Non-fraud reversal', true),
('0700', 'Sales draft credit adjustment', true);

-- Insert disclosure groups from discgrp.txt
INSERT INTO disclosure_groups (group_id, disclosure_text, interest_rate, effective_date, active_status) VALUES
('A000000000', 'Standard credit terms with variable APR based on creditworthiness and market conditions.', 0.0150, CURRENT_TIMESTAMP, true),
('DEFAULT', 'Default disclosure terms apply with standard interest rates and fees.', 0.0150, CURRENT_TIMESTAMP, true),
('ZEROAPR', 'Zero percent APR promotional offer for qualified customers.', 0.0000, CURRENT_TIMESTAMP, true);

-- =============================================================================
-- 7. CREATE FUNCTIONS FOR BUSINESS OPERATIONS
-- =============================================================================

-- Function to get active transaction types for caching
CREATE OR REPLACE FUNCTION get_active_transaction_types()
RETURNS TABLE (
    transaction_type VARCHAR(2),
    type_description VARCHAR(60),
    debit_credit_indicator BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tt.transaction_type,
        tt.type_description,
        tt.debit_credit_indicator
    FROM transaction_types tt
    WHERE tt.active_status = true
    ORDER BY tt.transaction_type;
END;
$$ LANGUAGE plpgsql;

-- Function to get active transaction categories for caching
CREATE OR REPLACE FUNCTION get_active_transaction_categories()
RETURNS TABLE (
    transaction_category VARCHAR(4),
    category_description VARCHAR(60)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tc.transaction_category,
        tc.category_description
    FROM transaction_categories tc
    WHERE tc.active_status = true
    ORDER BY tc.transaction_category;
END;
$$ LANGUAGE plpgsql;

-- Function to get current interest rate for account
CREATE OR REPLACE FUNCTION get_account_interest_rate(p_account_id VARCHAR(11))
RETURNS DECIMAL(5,4) AS $$
DECLARE
    v_interest_rate DECIMAL(5,4);
BEGIN
    SELECT dg.interest_rate 
    INTO v_interest_rate
    FROM accounts a
    INNER JOIN disclosure_groups dg ON a.group_id = dg.group_id
    WHERE a.account_id = p_account_id 
    AND dg.active_status = true
    AND dg.effective_date <= CURRENT_TIMESTAMP;
    
    RETURN COALESCE(v_interest_rate, 0.0150); -- Default to 1.5% if not found
END;
$$ LANGUAGE plpgsql;

-- Function to update category balance with audit trail
CREATE OR REPLACE FUNCTION update_category_balance(
    p_account_id VARCHAR(11),
    p_transaction_category VARCHAR(4),
    p_amount_change DECIMAL(12,2)
) RETURNS VOID AS $$
BEGIN
    INSERT INTO transaction_category_balances (account_id, transaction_category, category_balance, last_updated)
    VALUES (p_account_id, p_transaction_category, p_amount_change, CURRENT_TIMESTAMP)
    ON CONFLICT (account_id, transaction_category) 
    DO UPDATE SET 
        category_balance = transaction_category_balances.category_balance + p_amount_change,
        last_updated = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 8. ADD COMPREHENSIVE COMMENTS FOR DOCUMENTATION
-- =============================================================================

-- Table comments
COMMENT ON TABLE transaction_types IS 'Transaction type reference table from trantype.txt with 2-character type codes and descriptions';
COMMENT ON TABLE transaction_categories IS 'Transaction category reference table from trancatg.txt with 4-character category codes and hierarchical support';
COMMENT ON TABLE disclosure_groups IS 'Disclosure group reference table from discgrp.txt with interest rate management and legal disclosure text';
COMMENT ON TABLE transaction_category_balances IS 'Transaction category balance table from tcatbal.txt with composite primary key (account_id, transaction_category)';

-- Column comments for transaction_types
COMMENT ON COLUMN transaction_types.transaction_type IS 'Primary key: 2-character transaction type code for classification';
COMMENT ON COLUMN transaction_types.type_description IS 'Transaction type description from ASCII data source';
COMMENT ON COLUMN transaction_types.debit_credit_indicator IS 'Debit/credit indicator for proper transaction classification (true=debit, false=credit)';
COMMENT ON COLUMN transaction_types.active_status IS 'Active status for reference data lifecycle management and business rule enforcement';

-- Column comments for transaction_categories
COMMENT ON COLUMN transaction_categories.transaction_category IS 'Primary key: 4-character transaction category code for hierarchical categorization';
COMMENT ON COLUMN transaction_categories.category_description IS 'Transaction category description from ASCII data source';
COMMENT ON COLUMN transaction_categories.active_status IS 'Active status for reference data lifecycle management and business rule enforcement';

-- Column comments for disclosure_groups
COMMENT ON COLUMN disclosure_groups.group_id IS 'Primary key: Group identifier for interest rate and terms management';
COMMENT ON COLUMN disclosure_groups.disclosure_text IS 'Legal disclosure text for compliance with financial regulations';
COMMENT ON COLUMN disclosure_groups.interest_rate IS 'Interest rate with DECIMAL(5,4) precision supporting percentage calculations (0.0001 to 9.9999)';
COMMENT ON COLUMN disclosure_groups.effective_date IS 'Effective date for rate management and historical tracking';
COMMENT ON COLUMN disclosure_groups.active_status IS 'Active status for reference data lifecycle management and business rule enforcement';

-- Column comments for transaction_category_balances
COMMENT ON COLUMN transaction_category_balances.account_id IS 'Foreign key: Account identifier for composite primary key relationship';
COMMENT ON COLUMN transaction_category_balances.transaction_category IS 'Foreign key: Transaction category identifier for composite primary key relationship';
COMMENT ON COLUMN transaction_category_balances.category_balance IS 'Category balance with DECIMAL(12,2) precision for financial calculations';
COMMENT ON COLUMN transaction_category_balances.last_updated IS 'Last updated timestamp for tracking balance changes and audit trail';

-- =============================================================================
-- 9. CREATE MATERIALIZED VIEW FOR OPTIMIZED REFERENCE DATA QUERIES
-- =============================================================================

-- Create materialized view for reference data lookup optimization
CREATE MATERIALIZED VIEW mv_reference_data_lookup AS
SELECT 
    tt.transaction_type,
    tt.type_description,
    tt.debit_credit_indicator,
    tc.transaction_category,
    tc.category_description,
    dg.group_id,
    dg.interest_rate,
    dg.effective_date,
    CONCAT(tt.transaction_type, '-', tt.type_description) AS type_display,
    CONCAT(tc.transaction_category, '-', tc.category_description) AS category_display,
    CASE 
        WHEN tt.debit_credit_indicator = true THEN 'DEBIT'
        ELSE 'CREDIT'
    END AS transaction_direction
FROM transaction_types tt
CROSS JOIN transaction_categories tc
CROSS JOIN disclosure_groups dg
WHERE tt.active_status = true 
AND tc.active_status = true 
AND dg.active_status = true;

-- Create indexes on materialized view for sub-millisecond lookups
CREATE INDEX idx_mv_reference_data_lookup_transaction_type ON mv_reference_data_lookup(transaction_type);
CREATE INDEX idx_mv_reference_data_lookup_transaction_category ON mv_reference_data_lookup(transaction_category);
CREATE INDEX idx_mv_reference_data_lookup_group_id ON mv_reference_data_lookup(group_id);
CREATE INDEX idx_mv_reference_data_lookup_interest_rate ON mv_reference_data_lookup(interest_rate);

-- Function to refresh reference data materialized view
CREATE OR REPLACE FUNCTION refresh_reference_data_lookup()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_reference_data_lookup;
END;
$$ LANGUAGE plpgsql;

COMMENT ON MATERIALIZED VIEW mv_reference_data_lookup IS 'Optimized reference data lookup view for sub-millisecond response times with caching strategies';

-- =============================================================================
-- 10. ROLLBACK CHANGESET INSTRUCTIONS
-- =============================================================================

-- rollback changeset blitzy:V6-create-reference-tables
-- DROP FUNCTION IF EXISTS refresh_reference_data_lookup();
-- DROP MATERIALIZED VIEW IF EXISTS mv_reference_data_lookup CASCADE;
-- DROP FUNCTION IF EXISTS update_category_balance(VARCHAR(11), VARCHAR(4), DECIMAL(12,2));
-- DROP FUNCTION IF EXISTS get_account_interest_rate(VARCHAR(11));
-- DROP FUNCTION IF EXISTS get_active_transaction_categories();
-- DROP FUNCTION IF EXISTS get_active_transaction_types();
-- DROP TRIGGER IF EXISTS transaction_category_balances_last_updated_trigger ON transaction_category_balances;
-- DROP FUNCTION IF EXISTS update_transaction_category_balances_last_updated();
-- DROP TRIGGER IF EXISTS disclosure_groups_updated_at_trigger ON disclosure_groups;
-- DROP FUNCTION IF EXISTS update_disclosure_groups_updated_at();
-- DROP TRIGGER IF EXISTS transaction_categories_updated_at_trigger ON transaction_categories;
-- DROP FUNCTION IF EXISTS update_transaction_categories_updated_at();
-- DROP TRIGGER IF EXISTS transaction_types_updated_at_trigger ON transaction_types;
-- DROP FUNCTION IF EXISTS update_transaction_types_updated_at();
-- DROP TABLE IF EXISTS transaction_category_balances CASCADE;
-- DROP TABLE IF EXISTS disclosure_groups CASCADE;
-- DROP TABLE IF EXISTS transaction_categories CASCADE;
-- DROP TABLE IF EXISTS transaction_types CASCADE;