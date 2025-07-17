-- =====================================================================================
-- Liquibase Migration: V3__create_accounts_table.sql
-- Description: Creates accounts table from VSAM ACCTDAT dataset with precise financial 
--              field mapping, customer relationships, and disclosure group associations
-- Author: Blitzy Agent  
-- Date: 2024
-- Version: 3.0
-- Dependencies: V2__create_customers_table.sql (customers table must exist)
-- =====================================================================================

-- changeset blitzy:V3-create-accounts-table
-- comment: Create accounts table migrated from ACCTDAT VSAM dataset preserving exact field layouts and financial precision

-- Create accounts table with all required fields from VSAM ACCTDAT structure
CREATE TABLE accounts (
    -- Primary key: account_id as VARCHAR(11) matching fixed-width 11-digit record structure
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key to customers table establishing customer-account relationship
    customer_id VARCHAR(9) NOT NULL,
    
    -- Account status tracking for active/inactive lifecycle management
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- Financial fields using DECIMAL(12,2) for exact COBOL COMP-3 arithmetic equivalence
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    cash_credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Account lifecycle management date fields
    open_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    reissue_date DATE NOT NULL,
    
    -- Current cycle financial tracking with DECIMAL precision for financial calculations
    current_cycle_credit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_cycle_debit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- ZIP code field for account-specific addressing
    address_zip VARCHAR(10),
    
    -- Foreign key to disclosure_groups table for interest rate management
    group_id VARCHAR(10) NOT NULL,
    
    -- Audit and tracking fields for compliance and operational oversight
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT accounts_pkey PRIMARY KEY (account_id)
);

-- Create foreign key constraint to customers table ensuring referential integrity
ALTER TABLE accounts 
ADD CONSTRAINT accounts_customer_id_fkey 
FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create foreign key constraint to disclosure_groups table for interest rate associations
-- Note: This will be activated after disclosure_groups table is created in subsequent migration
-- ALTER TABLE accounts 
-- ADD CONSTRAINT accounts_group_id_fkey 
-- FOREIGN KEY (group_id) REFERENCES disclosure_groups(group_id)
-- ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create indexes for optimized query performance supporting microservices architecture
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_active_status ON accounts(active_status);
CREATE INDEX idx_accounts_group_id ON accounts(group_id);
CREATE INDEX idx_accounts_balance ON accounts(account_id, current_balance);
CREATE INDEX idx_accounts_credit_limit ON accounts(credit_limit);
CREATE INDEX idx_accounts_open_date ON accounts(open_date);
CREATE INDEX idx_accounts_expiration_date ON accounts(expiration_date);

-- Create composite index for customer-account cross-reference queries
CREATE INDEX idx_customer_account_xref ON accounts(customer_id, account_id);

-- Add CHECK constraints for data validation and business rules
ALTER TABLE accounts 
ADD CONSTRAINT accounts_account_id_check 
CHECK (account_id ~ '^[0-9]{11}$');

ALTER TABLE accounts 
ADD CONSTRAINT accounts_active_status_check 
CHECK (active_status IN ('Y', 'N'));

ALTER TABLE accounts 
ADD CONSTRAINT accounts_current_balance_check 
CHECK (current_balance >= -99999999.99 AND current_balance <= 99999999.99);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_credit_limit_check 
CHECK (credit_limit >= 0.00 AND credit_limit <= 99999999.99);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_cash_credit_limit_check 
CHECK (cash_credit_limit >= 0.00 AND cash_credit_limit <= credit_limit);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_current_cycle_credit_check 
CHECK (current_cycle_credit >= 0.00 AND current_cycle_credit <= 99999999.99);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_current_cycle_debit_check 
CHECK (current_cycle_debit >= 0.00 AND current_cycle_debit <= 99999999.99);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_open_date_check 
CHECK (open_date <= CURRENT_DATE AND open_date >= '1900-01-01');

ALTER TABLE accounts 
ADD CONSTRAINT accounts_expiration_date_check 
CHECK (expiration_date > open_date);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_reissue_date_check 
CHECK (reissue_date >= open_date);

ALTER TABLE accounts 
ADD CONSTRAINT accounts_address_zip_check 
CHECK (address_zip IS NULL OR address_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

-- Create trigger function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on row changes
CREATE TRIGGER accounts_updated_at_trigger
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- Add comments to table and columns for documentation
COMMENT ON TABLE accounts IS 'Account master data table migrated from VSAM ACCTDAT dataset with precise financial field mapping and customer relationships';

COMMENT ON COLUMN accounts.account_id IS 'Primary key: 11-digit account identifier matching VSAM ACCTDAT key structure';
COMMENT ON COLUMN accounts.customer_id IS 'Foreign key: 9-digit customer identifier establishing customer-account relationship';
COMMENT ON COLUMN accounts.active_status IS 'Account status indicator: Y=Active, N=Inactive for lifecycle management';
COMMENT ON COLUMN accounts.current_balance IS 'Current account balance with DECIMAL(12,2) precision for exact financial calculations';
COMMENT ON COLUMN accounts.credit_limit IS 'Maximum credit limit with DECIMAL(12,2) precision for financial operations';
COMMENT ON COLUMN accounts.cash_credit_limit IS 'Cash advance limit with DECIMAL(12,2) precision (must not exceed credit_limit)';
COMMENT ON COLUMN accounts.open_date IS 'Account opening date for lifecycle tracking';
COMMENT ON COLUMN accounts.expiration_date IS 'Account expiration date for renewal management';
COMMENT ON COLUMN accounts.reissue_date IS 'Card reissue date for security and lifecycle operations';
COMMENT ON COLUMN accounts.current_cycle_credit IS 'Current billing cycle credit total with DECIMAL(12,2) precision';
COMMENT ON COLUMN accounts.current_cycle_debit IS 'Current billing cycle debit total with DECIMAL(12,2) precision';
COMMENT ON COLUMN accounts.address_zip IS 'ZIP code for account-specific addressing (5-digit or 5+4 format)';
COMMENT ON COLUMN accounts.group_id IS 'Foreign key: Disclosure group identifier for interest rate and terms management';
COMMENT ON COLUMN accounts.created_at IS 'Timestamp when account record was created';
COMMENT ON COLUMN accounts.updated_at IS 'Timestamp when account record was last updated';

-- Create materialized view for account summary queries (optimized for cross-reference operations)
CREATE MATERIALIZED VIEW mv_account_summary AS
SELECT 
    a.account_id,
    a.customer_id,
    c.first_name,
    c.last_name,
    CONCAT(c.first_name, ' ', COALESCE(c.middle_name || ' ', ''), c.last_name) AS customer_full_name,
    a.active_status,
    a.current_balance,
    a.credit_limit,
    a.cash_credit_limit,
    (a.credit_limit - a.current_balance) AS available_credit,
    CASE 
        WHEN a.current_balance > a.credit_limit THEN 'Over Limit'
        WHEN a.current_balance > (a.credit_limit * 0.9) THEN 'Near Limit'
        WHEN a.current_balance < 0 THEN 'Credit Balance'
        ELSE 'Normal'
    END AS balance_status,
    a.open_date,
    a.expiration_date,
    a.reissue_date,
    EXTRACT(YEAR FROM AGE(a.expiration_date, CURRENT_DATE)) AS years_to_expiry,
    a.current_cycle_credit,
    a.current_cycle_debit,
    (a.current_cycle_debit - a.current_cycle_credit) AS net_cycle_activity,
    a.address_zip,
    a.group_id,
    c.fico_credit_score,
    CASE 
        WHEN c.fico_credit_score >= 800 THEN 'Excellent'
        WHEN c.fico_credit_score >= 740 THEN 'Very Good'
        WHEN c.fico_credit_score >= 670 THEN 'Good'
        WHEN c.fico_credit_score >= 580 THEN 'Fair'
        ELSE 'Poor'
    END AS credit_rating,
    a.created_at,
    a.updated_at
FROM accounts a
INNER JOIN customers c ON a.customer_id = c.customer_id;

-- Create indexes on materialized view for performance optimization
CREATE INDEX idx_mv_account_summary_customer_id ON mv_account_summary(customer_id);
CREATE INDEX idx_mv_account_summary_balance_status ON mv_account_summary(balance_status);
CREATE INDEX idx_mv_account_summary_credit_rating ON mv_account_summary(credit_rating);
CREATE INDEX idx_mv_account_summary_available_credit ON mv_account_summary(available_credit);
CREATE INDEX idx_mv_account_summary_years_to_expiry ON mv_account_summary(years_to_expiry);
CREATE INDEX idx_mv_account_summary_net_cycle_activity ON mv_account_summary(net_cycle_activity);

-- Add comment to materialized view
COMMENT ON MATERIALIZED VIEW mv_account_summary IS 'Optimized account summary view with customer information for cross-reference queries and comprehensive account management operations';

-- Create function to refresh materialized view (scheduled via cron or application)
CREATE OR REPLACE FUNCTION refresh_account_summary_view()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_summary;
END;
$$ LANGUAGE plpgsql;

-- Create function for account balance validation matching COBOL COMP-3 arithmetic
CREATE OR REPLACE FUNCTION validate_account_balance(
    p_account_id VARCHAR(11),
    p_transaction_amount DECIMAL(12,2),
    p_transaction_type VARCHAR(2)
) RETURNS BOOLEAN AS $$
DECLARE
    v_current_balance DECIMAL(12,2);
    v_credit_limit DECIMAL(12,2);
    v_new_balance DECIMAL(12,2);
BEGIN
    -- Retrieve current account balance and credit limit
    SELECT current_balance, credit_limit 
    INTO v_current_balance, v_credit_limit
    FROM accounts 
    WHERE account_id = p_account_id AND active_status = 'Y';
    
    -- Calculate new balance based on transaction type
    IF p_transaction_type IN ('01', '02') THEN -- Debit transactions
        v_new_balance := v_current_balance + p_transaction_amount;
    ELSE -- Credit transactions
        v_new_balance := v_current_balance - p_transaction_amount;
    END IF;
    
    -- Validate new balance against credit limit
    RETURN v_new_balance <= v_credit_limit;
END;
$$ LANGUAGE plpgsql;

-- Create function for cycle balance calculations with DECIMAL precision
CREATE OR REPLACE FUNCTION calculate_cycle_balances(
    p_account_id VARCHAR(11),
    p_cycle_start_date DATE,
    p_cycle_end_date DATE
) RETURNS TABLE (
    cycle_credit DECIMAL(12,2),
    cycle_debit DECIMAL(12,2),
    net_activity DECIMAL(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COALESCE(SUM(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) ELSE 0 END), 0.00) AS cycle_credit,
        COALESCE(SUM(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount ELSE 0 END), 0.00) AS cycle_debit,
        COALESCE(SUM(t.transaction_amount), 0.00) AS net_activity
    FROM transactions t
    WHERE t.account_id = p_account_id
    AND t.transaction_timestamp >= p_cycle_start_date
    AND t.transaction_timestamp < p_cycle_end_date + INTERVAL '1 day';
END;
$$ LANGUAGE plpgsql;

-- rollback changeset blitzy:V3-create-accounts-table
-- DROP FUNCTION IF EXISTS calculate_cycle_balances(VARCHAR(11), DATE, DATE);
-- DROP FUNCTION IF EXISTS validate_account_balance(VARCHAR(11), DECIMAL(12,2), VARCHAR(2));
-- DROP FUNCTION IF EXISTS refresh_account_summary_view();
-- DROP MATERIALIZED VIEW IF EXISTS mv_account_summary CASCADE;
-- DROP TRIGGER IF EXISTS accounts_updated_at_trigger ON accounts;
-- DROP FUNCTION IF EXISTS update_accounts_updated_at();
-- DROP TABLE IF EXISTS accounts CASCADE;