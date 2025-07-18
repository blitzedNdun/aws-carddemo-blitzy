-- =====================================================================================
-- Liquibase Migration: V5__create_transactions_table.sql
-- Description: Creates transactions table from VSAM TRANSACT dataset with monthly
--              partitioning, precise financial fields, and comprehensive foreign key
--              relationships for high-performance transaction processing
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 5.0
-- Dependencies: V3__create_accounts_table.sql, V4__create_cards_table.sql
-- =====================================================================================

-- changeset blitzy:V5-create-transactions-table
-- comment: Create transactions table migrated from TRANSACT VSAM dataset with monthly RANGE partitioning and exact financial precision

-- Create transactions table with monthly RANGE partitioning
CREATE TABLE transactions (
    -- Primary key: transaction_id as VARCHAR(16) maintaining unique transaction identification
    transaction_id VARCHAR(16) NOT NULL,
    
    -- Foreign key to accounts table for account-transaction relationship
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key to cards table for card-transaction relationship
    card_number VARCHAR(16) NOT NULL,
    
    -- Foreign key to transaction_types table for transaction classification
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Foreign key to transaction_categories table for transaction categorization
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Transaction amount with DECIMAL(12,2) precision for exact financial calculations
    transaction_amount DECIMAL(12,2) NOT NULL,
    
    -- Transaction description field for business context
    description VARCHAR(100),
    
    -- Transaction timestamp for date-based partitioning and time-range queries
    transaction_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Merchant information fields for transaction location tracking
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(30),
    merchant_zip VARCHAR(10),
    
    -- Audit and tracking fields for compliance and operational oversight
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT transactions_pkey PRIMARY KEY (transaction_id, transaction_timestamp)
) PARTITION BY RANGE (transaction_timestamp);

-- Create foreign key constraint to accounts table ensuring referential integrity
ALTER TABLE transactions 
ADD CONSTRAINT transactions_account_id_fkey 
FOREIGN KEY (account_id) REFERENCES accounts(account_id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create foreign key constraint to cards table ensuring referential integrity
ALTER TABLE transactions 
ADD CONSTRAINT transactions_card_number_fkey 
FOREIGN KEY (card_number) REFERENCES cards(card_number)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create foreign key constraints to reference tables for transaction classification
-- Note: These will be activated after reference tables are created in subsequent migrations
-- ALTER TABLE transactions 
-- ADD CONSTRAINT transactions_transaction_type_fkey 
-- FOREIGN KEY (transaction_type) REFERENCES transaction_types(transaction_type)
-- ON DELETE RESTRICT ON UPDATE CASCADE;

-- ALTER TABLE transactions 
-- ADD CONSTRAINT transactions_transaction_category_fkey 
-- FOREIGN KEY (transaction_category) REFERENCES transaction_categories(transaction_category)
-- ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create monthly partitions for current year and next year (24 months total)
-- January 2024 partition
CREATE TABLE transactions_2024_01 PARTITION OF transactions
FOR VALUES FROM ('2024-01-01 00:00:00+00') TO ('2024-02-01 00:00:00+00');

-- February 2024 partition
CREATE TABLE transactions_2024_02 PARTITION OF transactions
FOR VALUES FROM ('2024-02-01 00:00:00+00') TO ('2024-03-01 00:00:00+00');

-- March 2024 partition
CREATE TABLE transactions_2024_03 PARTITION OF transactions
FOR VALUES FROM ('2024-03-01 00:00:00+00') TO ('2024-04-01 00:00:00+00');

-- April 2024 partition
CREATE TABLE transactions_2024_04 PARTITION OF transactions
FOR VALUES FROM ('2024-04-01 00:00:00+00') TO ('2024-05-01 00:00:00+00');

-- May 2024 partition
CREATE TABLE transactions_2024_05 PARTITION OF transactions
FOR VALUES FROM ('2024-05-01 00:00:00+00') TO ('2024-06-01 00:00:00+00');

-- June 2024 partition
CREATE TABLE transactions_2024_06 PARTITION OF transactions
FOR VALUES FROM ('2024-06-01 00:00:00+00') TO ('2024-07-01 00:00:00+00');

-- July 2024 partition
CREATE TABLE transactions_2024_07 PARTITION OF transactions
FOR VALUES FROM ('2024-07-01 00:00:00+00') TO ('2024-08-01 00:00:00+00');

-- August 2024 partition
CREATE TABLE transactions_2024_08 PARTITION OF transactions
FOR VALUES FROM ('2024-08-01 00:00:00+00') TO ('2024-09-01 00:00:00+00');

-- September 2024 partition
CREATE TABLE transactions_2024_09 PARTITION OF transactions
FOR VALUES FROM ('2024-09-01 00:00:00+00') TO ('2024-10-01 00:00:00+00');

-- October 2024 partition
CREATE TABLE transactions_2024_10 PARTITION OF transactions
FOR VALUES FROM ('2024-10-01 00:00:00+00') TO ('2024-11-01 00:00:00+00');

-- November 2024 partition
CREATE TABLE transactions_2024_11 PARTITION OF transactions
FOR VALUES FROM ('2024-11-01 00:00:00+00') TO ('2024-12-01 00:00:00+00');

-- December 2024 partition
CREATE TABLE transactions_2024_12 PARTITION OF transactions
FOR VALUES FROM ('2024-12-01 00:00:00+00') TO ('2025-01-01 00:00:00+00');

-- January 2025 partition
CREATE TABLE transactions_2025_01 PARTITION OF transactions
FOR VALUES FROM ('2025-01-01 00:00:00+00') TO ('2025-02-01 00:00:00+00');

-- February 2025 partition
CREATE TABLE transactions_2025_02 PARTITION OF transactions
FOR VALUES FROM ('2025-02-01 00:00:00+00') TO ('2025-03-01 00:00:00+00');

-- March 2025 partition
CREATE TABLE transactions_2025_03 PARTITION OF transactions
FOR VALUES FROM ('2025-03-01 00:00:00+00') TO ('2025-04-01 00:00:00+00');

-- April 2025 partition
CREATE TABLE transactions_2025_04 PARTITION OF transactions
FOR VALUES FROM ('2025-04-01 00:00:00+00') TO ('2025-05-01 00:00:00+00');

-- May 2025 partition
CREATE TABLE transactions_2025_05 PARTITION OF transactions
FOR VALUES FROM ('2025-05-01 00:00:00+00') TO ('2025-06-01 00:00:00+00');

-- June 2025 partition
CREATE TABLE transactions_2025_06 PARTITION OF transactions
FOR VALUES FROM ('2025-06-01 00:00:00+00') TO ('2025-07-01 00:00:00+00');

-- July 2025 partition
CREATE TABLE transactions_2025_07 PARTITION OF transactions
FOR VALUES FROM ('2025-07-01 00:00:00+00') TO ('2025-08-01 00:00:00+00');

-- August 2025 partition
CREATE TABLE transactions_2025_08 PARTITION OF transactions
FOR VALUES FROM ('2025-08-01 00:00:00+00') TO ('2025-09-01 00:00:00+00');

-- September 2025 partition
CREATE TABLE transactions_2025_09 PARTITION OF transactions
FOR VALUES FROM ('2025-09-01 00:00:00+00') TO ('2025-10-01 00:00:00+00');

-- October 2025 partition
CREATE TABLE transactions_2025_10 PARTITION OF transactions
FOR VALUES FROM ('2025-10-01 00:00:00+00') TO ('2025-11-01 00:00:00+00');

-- November 2025 partition
CREATE TABLE transactions_2025_11 PARTITION OF transactions
FOR VALUES FROM ('2025-11-01 00:00:00+00') TO ('2025-12-01 00:00:00+00');

-- December 2025 partition
CREATE TABLE transactions_2025_12 PARTITION OF transactions
FOR VALUES FROM ('2025-12-01 00:00:00+00') TO ('2026-01-01 00:00:00+00');

-- Create indexes for optimized query performance supporting microservices architecture
CREATE INDEX idx_transactions_account_id ON transactions(account_id, transaction_timestamp);
CREATE INDEX idx_transactions_card_number ON transactions(card_number, transaction_timestamp);
CREATE INDEX idx_transactions_timestamp ON transactions(transaction_timestamp);
CREATE INDEX idx_transactions_type ON transactions(transaction_type, transaction_timestamp);
CREATE INDEX idx_transactions_category ON transactions(transaction_category, transaction_timestamp);
CREATE INDEX idx_transactions_amount ON transactions(transaction_amount, transaction_timestamp);

-- Create composite index for merchant-based queries
CREATE INDEX idx_transactions_merchant ON transactions(merchant_name, merchant_city, transaction_timestamp);

-- Create composite index for date-range queries with partition pruning optimization
CREATE INDEX idx_transactions_date_range ON transactions(transaction_timestamp, account_id, transaction_amount);

-- Add CHECK constraints for data validation and business rules
ALTER TABLE transactions 
ADD CONSTRAINT transactions_transaction_id_check 
CHECK (transaction_id ~ '^[A-Za-z0-9]{16}$');

ALTER TABLE transactions 
ADD CONSTRAINT transactions_account_id_check 
CHECK (account_id ~ '^[0-9]{11}$');

ALTER TABLE transactions 
ADD CONSTRAINT transactions_card_number_check 
CHECK (card_number ~ '^[0-9]{16}$');

ALTER TABLE transactions 
ADD CONSTRAINT transactions_transaction_type_check 
CHECK (transaction_type ~ '^[0-9]{2}$');

ALTER TABLE transactions 
ADD CONSTRAINT transactions_transaction_category_check 
CHECK (transaction_category ~ '^[A-Z0-9]{4}$');

ALTER TABLE transactions 
ADD CONSTRAINT transactions_transaction_amount_check 
CHECK (transaction_amount >= -99999999.99 AND transaction_amount <= 99999999.99);

ALTER TABLE transactions 
ADD CONSTRAINT transactions_description_check 
CHECK (description IS NULL OR LENGTH(TRIM(description)) > 0);

ALTER TABLE transactions 
ADD CONSTRAINT transactions_timestamp_check 
CHECK (transaction_timestamp >= '2000-01-01' AND transaction_timestamp <= '2099-12-31');

ALTER TABLE transactions 
ADD CONSTRAINT transactions_merchant_name_check 
CHECK (merchant_name IS NULL OR LENGTH(TRIM(merchant_name)) > 0);

ALTER TABLE transactions 
ADD CONSTRAINT transactions_merchant_city_check 
CHECK (merchant_city IS NULL OR LENGTH(TRIM(merchant_city)) > 0);

ALTER TABLE transactions 
ADD CONSTRAINT transactions_merchant_zip_check 
CHECK (merchant_zip IS NULL OR merchant_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

-- Create trigger function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_transactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on row changes
CREATE TRIGGER transactions_updated_at_trigger
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_transactions_updated_at();

-- Add comments to table and columns for documentation
COMMENT ON TABLE transactions IS 'Transaction history table migrated from VSAM TRANSACT dataset with monthly partitioning, precise financial field mapping, and comprehensive foreign key relationships';

COMMENT ON COLUMN transactions.transaction_id IS 'Primary key: 16-character transaction identifier maintaining unique transaction identification';
COMMENT ON COLUMN transactions.account_id IS 'Foreign key: 11-digit account identifier establishing transaction-to-account relationship';
COMMENT ON COLUMN transactions.card_number IS 'Foreign key: 16-digit card number establishing transaction-to-card relationship';
COMMENT ON COLUMN transactions.transaction_type IS 'Foreign key: 2-character transaction type code for transaction classification';
COMMENT ON COLUMN transactions.transaction_category IS 'Foreign key: 4-character transaction category code for transaction categorization';
COMMENT ON COLUMN transactions.transaction_amount IS 'Transaction amount with DECIMAL(12,2) precision for exact financial calculations';
COMMENT ON COLUMN transactions.description IS 'Transaction description field for business context (100 characters max)';
COMMENT ON COLUMN transactions.transaction_timestamp IS 'Transaction timestamp for date-based partitioning and time-range queries';
COMMENT ON COLUMN transactions.merchant_name IS 'Merchant name for transaction location tracking (50 characters max)';
COMMENT ON COLUMN transactions.merchant_city IS 'Merchant city for transaction location tracking (30 characters max)';
COMMENT ON COLUMN transactions.merchant_zip IS 'Merchant ZIP code for transaction location tracking (5-digit or 5+4 format)';
COMMENT ON COLUMN transactions.created_at IS 'Timestamp when transaction record was created';
COMMENT ON COLUMN transactions.updated_at IS 'Timestamp when transaction record was last updated';

-- Create function for transaction validation matching COBOL business logic
CREATE OR REPLACE FUNCTION validate_transaction_amount(
    p_account_id VARCHAR(11),
    p_transaction_amount DECIMAL(12,2),
    p_transaction_type VARCHAR(2)
) RETURNS BOOLEAN AS $$
DECLARE
    v_account_balance DECIMAL(12,2);
    v_credit_limit DECIMAL(12,2);
    v_available_credit DECIMAL(12,2);
BEGIN
    -- Retrieve account balance and credit limit
    SELECT current_balance, credit_limit 
    INTO v_account_balance, v_credit_limit
    FROM accounts 
    WHERE account_id = p_account_id AND active_status = 'Y';
    
    -- Calculate available credit
    v_available_credit := v_credit_limit - v_account_balance;
    
    -- Validate transaction amount based on type
    IF p_transaction_type IN ('01', '02') THEN -- Debit transactions
        RETURN ABS(p_transaction_amount) <= v_available_credit;
    ELSE -- Credit transactions
        RETURN p_transaction_amount >= 0;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create function for transaction history queries with partition pruning
CREATE OR REPLACE FUNCTION get_transaction_history(
    p_account_id VARCHAR(11),
    p_start_date DATE,
    p_end_date DATE,
    p_limit INTEGER DEFAULT 100
) RETURNS TABLE (
    transaction_id VARCHAR(16),
    transaction_amount DECIMAL(12,2),
    transaction_timestamp TIMESTAMP WITH TIME ZONE,
    description VARCHAR(100),
    merchant_name VARCHAR(50)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.transaction_id,
        t.transaction_amount,
        t.transaction_timestamp,
        t.description,
        t.merchant_name
    FROM transactions t
    WHERE t.account_id = p_account_id
    AND t.transaction_timestamp >= p_start_date::TIMESTAMP WITH TIME ZONE
    AND t.transaction_timestamp < (p_end_date + INTERVAL '1 day')::TIMESTAMP WITH TIME ZONE
    ORDER BY t.transaction_timestamp DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Create function for monthly transaction summary calculations
CREATE OR REPLACE FUNCTION calculate_monthly_summary(
    p_account_id VARCHAR(11),
    p_year INTEGER,
    p_month INTEGER
) RETURNS TABLE (
    total_debits DECIMAL(12,2),
    total_credits DECIMAL(12,2),
    transaction_count INTEGER,
    average_amount DECIMAL(12,2)
) AS $$
DECLARE
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    -- Calculate month boundaries
    v_start_date := DATE(p_year || '-' || LPAD(p_month::TEXT, 2, '0') || '-01');
    v_end_date := v_start_date + INTERVAL '1 month';
    
    RETURN QUERY
    SELECT 
        COALESCE(SUM(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount ELSE 0 END), 0.00) AS total_debits,
        COALESCE(SUM(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) ELSE 0 END), 0.00) AS total_credits,
        COUNT(*)::INTEGER AS transaction_count,
        COALESCE(AVG(ABS(t.transaction_amount)), 0.00) AS average_amount
    FROM transactions t
    WHERE t.account_id = p_account_id
    AND t.transaction_timestamp >= v_start_date::TIMESTAMP WITH TIME ZONE
    AND t.transaction_timestamp < v_end_date::TIMESTAMP WITH TIME ZONE;
END;
$$ LANGUAGE plpgsql;

-- Create function for automatic partition management
CREATE OR REPLACE FUNCTION create_monthly_partition(
    p_year INTEGER,
    p_month INTEGER
) RETURNS VOID AS $$
DECLARE
    v_partition_name TEXT;
    v_start_date TEXT;
    v_end_date TEXT;
    v_sql TEXT;
BEGIN
    -- Generate partition name
    v_partition_name := 'transactions_' || p_year || '_' || LPAD(p_month::TEXT, 2, '0');
    
    -- Generate date boundaries
    v_start_date := p_year || '-' || LPAD(p_month::TEXT, 2, '0') || '-01 00:00:00+00';
    
    -- Calculate end date (first day of next month)
    IF p_month = 12 THEN
        v_end_date := (p_year + 1) || '-01-01 00:00:00+00';
    ELSE
        v_end_date := p_year || '-' || LPAD((p_month + 1)::TEXT, 2, '0') || '-01 00:00:00+00';
    END IF;
    
    -- Create partition
    v_sql := 'CREATE TABLE IF NOT EXISTS ' || v_partition_name || 
             ' PARTITION OF transactions FOR VALUES FROM (''' || v_start_date || 
             ''') TO (''' || v_end_date || ''')';
    
    EXECUTE v_sql;
END;
$$ LANGUAGE plpgsql;

-- Create materialized view for transaction summary analytics
CREATE MATERIALIZED VIEW mv_transaction_summary AS
SELECT 
    t.account_id,
    t.card_number,
    DATE_TRUNC('month', t.transaction_timestamp) AS month_year,
    t.transaction_type,
    t.transaction_category,
    COUNT(*) AS transaction_count,
    SUM(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount ELSE 0 END) AS total_debits,
    SUM(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) ELSE 0 END) AS total_credits,
    AVG(ABS(t.transaction_amount)) AS average_amount,
    MIN(t.transaction_timestamp) AS first_transaction,
    MAX(t.transaction_timestamp) AS last_transaction
FROM transactions t
GROUP BY 
    t.account_id, 
    t.card_number, 
    DATE_TRUNC('month', t.transaction_timestamp), 
    t.transaction_type, 
    t.transaction_category;

-- Create indexes on materialized view for performance optimization
CREATE INDEX idx_mv_transaction_summary_account_id ON mv_transaction_summary(account_id);
CREATE INDEX idx_mv_transaction_summary_card_number ON mv_transaction_summary(card_number);
CREATE INDEX idx_mv_transaction_summary_month_year ON mv_transaction_summary(month_year);
CREATE INDEX idx_mv_transaction_summary_type ON mv_transaction_summary(transaction_type);
CREATE INDEX idx_mv_transaction_summary_category ON mv_transaction_summary(transaction_category);

-- Add comment to materialized view
COMMENT ON MATERIALIZED VIEW mv_transaction_summary IS 'Monthly transaction summary view with account, card, and category aggregations for reporting and analytics';

-- Create function to refresh materialized view (scheduled via cron or application)
CREATE OR REPLACE FUNCTION refresh_transaction_summary_view()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_summary;
END;
$$ LANGUAGE plpgsql;

-- rollback changeset blitzy:V5-create-transactions-table
-- DROP FUNCTION IF EXISTS refresh_transaction_summary_view();
-- DROP MATERIALIZED VIEW IF EXISTS mv_transaction_summary CASCADE;
-- DROP FUNCTION IF EXISTS create_monthly_partition(INTEGER, INTEGER);
-- DROP FUNCTION IF EXISTS calculate_monthly_summary(VARCHAR(11), INTEGER, INTEGER);
-- DROP FUNCTION IF EXISTS get_transaction_history(VARCHAR(11), DATE, DATE, INTEGER);
-- DROP FUNCTION IF EXISTS validate_transaction_amount(VARCHAR(11), DECIMAL(12,2), VARCHAR(2));
-- DROP TRIGGER IF EXISTS transactions_updated_at_trigger ON transactions;
-- DROP FUNCTION IF EXISTS update_transactions_updated_at();
-- DROP TABLE IF EXISTS transactions CASCADE;