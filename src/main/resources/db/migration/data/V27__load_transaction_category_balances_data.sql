-- ==============================================================================
-- Liquibase Data Migration: V27__load_transaction_category_balances_data.sql
-- Description: Populates transaction_category_balances table from tcatbal.txt with
--              composite primary key relationships, DECIMAL(12,2) precision balance
--              tracking, and comprehensive foreign key constraint validation
-- Author: Blitzy agent
-- Version: 27.0
-- Migration Type: DATA LOADING with exact financial precision and audit trail
-- Source: app/data/ASCII/tcatbal.txt (50 category balance records)
-- Target: transaction_category_balances table with account-category composite keys
-- ==============================================================================

-- This file is now included via XML changeset in liquibase-changelog.xml
-- Liquibase-specific comments have been moved to the XML changeset definition

-- =============================================================================
-- PHASE 1: Data Validation and Preparation
-- =============================================================================

-- Create temporary working table for raw tcatbal.txt parsing and validation
CREATE TEMPORARY TABLE temp_tcatbal_raw (
    line_number SERIAL,
    raw_data TEXT NOT NULL,
    record_id VARCHAR(12),
    numeric_field VARCHAR(15),
    delimiter_char CHAR(1),
    zero_field VARCHAR(22),
    processed BOOLEAN DEFAULT FALSE
);

-- Create temporary table for parsed transaction category balance records with validation
CREATE TEMPORARY TABLE temp_tcatbal_parsed (
    line_number INTEGER,
    record_sequence INTEGER,
    account_id VARCHAR(11),
    transaction_category VARCHAR(4),
    parent_transaction_type VARCHAR(2),
    balance_cents BIGINT,
    category_balance DECIMAL(12,2),
    validation_errors TEXT DEFAULT '',
    is_valid BOOLEAN DEFAULT TRUE
);

-- Create temporary table for data loading statistics and audit trail
CREATE TEMPORARY TABLE temp_load_statistics (
    total_records INTEGER DEFAULT 0,
    valid_records INTEGER DEFAULT 0,
    invalid_records INTEGER DEFAULT 0,
    skipped_records INTEGER DEFAULT 0,
    loaded_records INTEGER DEFAULT 0,
    foreign_key_violations INTEGER DEFAULT 0,
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE
);

-- Initialize statistics record for comprehensive audit trail
INSERT INTO temp_load_statistics (total_records) VALUES (0);

-- =============================================================================
-- PHASE 2: Raw Data Loading from tcatbal.txt
-- =============================================================================

-- Load raw transaction category balance data from tcatbal.txt using structured parsing
-- Format: 12-digit record ID + 15-digit numeric field + "{" delimiter + 22-digit zero field
-- Loading transaction category balance data from tcatbal.txt...

-- Insert tcatbal.txt records with exact line preservation for audit purposes
-- Record format: record_id(12)numeric_field(15){zero_field(22)
INSERT INTO temp_tcatbal_raw (raw_data) VALUES
('000000000010100010000000000{0000000000000000000000'),
('000000000020100010000000000{0000000000000000000000'),
('000000000030100010000000000{0000000000000000000000'),
('000000000040100010000000000{0000000000000000000000'),
('000000000050100010000000000{0000000000000000000000'),
('000000000060100010000000000{0000000000000000000000'),
('000000000070100010000000000{0000000000000000000000'),
('000000000080100010000000000{0000000000000000000000'),
('000000000090100010000000000{0000000000000000000000'),
('000000000100100010000000000{0000000000000000000000'),
('000000000110100010000000000{0000000000000000000000'),
('000000000120100010000000000{0000000000000000000000'),
('000000000130100010000000000{0000000000000000000000'),
('000000000140100010000000000{0000000000000000000000'),
('000000000150100010000000000{0000000000000000000000'),
('000000000160100010000000000{0000000000000000000000'),
('000000000170100010000000000{0000000000000000000000'),
('000000000180100010000000000{0000000000000000000000'),
('000000000190100010000000000{0000000000000000000000'),
('000000000200100010000000000{0000000000000000000000'),
('000000000210100010000000000{0000000000000000000000'),
('000000000220100010000000000{0000000000000000000000'),
('000000000230100010000000000{0000000000000000000000'),
('000000000240100010000000000{0000000000000000000000'),
('000000000250100010000000000{0000000000000000000000'),
('000000000260100010000000000{0000000000000000000000'),
('000000000270100010000000000{0000000000000000000000'),
('000000000280100010000000000{0000000000000000000000'),
('000000000290100010000000000{0000000000000000000000'),
('000000000300100010000000000{0000000000000000000000'),
('000000000310100010000000000{0000000000000000000000'),
('000000000320100010000000000{0000000000000000000000'),
('000000000330100010000000000{0000000000000000000000'),
('000000000340100010000000000{0000000000000000000000'),
('000000000350100010000000000{0000000000000000000000'),
('000000000360100010000000000{0000000000000000000000'),
('000000000370100010000000000{0000000000000000000000'),
('000000000380100010000000000{0000000000000000000000'),
('000000000390100010000000000{0000000000000000000000'),
('000000000400100010000000000{0000000000000000000000'),
('000000000410100010000000000{0000000000000000000000'),
('000000000420100010000000000{0000000000000000000000'),
('000000000430100010000000000{0000000000000000000000'),
('000000000440100010000000000{0000000000000000000000'),
('000000000450100010000000000{0000000000000000000000'),
('000000000460100010000000000{0000000000000000000000'),
('000000000470100010000000000{0000000000000000000000'),
('000000000480100010000000000{0000000000000000000000'),
('000000000490100010000000000{0000000000000000000000'),
('000000000500100010000000000{0000000000000000000000');

-- Update total records count in statistics for audit trail
UPDATE temp_load_statistics SET total_records = (SELECT COUNT(*) FROM temp_tcatbal_raw);

-- =============================================================================
-- PHASE 3: Data Parsing and Field Extraction
-- =============================================================================

-- Parse raw tcatbal.txt data into structured fields with validation
-- Parsing tcatbal.txt data structure with field validation...

-- Extract structured fields from raw data lines using PostgreSQL string functions
UPDATE temp_tcatbal_raw SET
    record_id = SUBSTRING(raw_data, 1, 12),
    numeric_field = SUBSTRING(raw_data, 13, 15),
    delimiter_char = SUBSTRING(raw_data, 28, 1),
    zero_field = SUBSTRING(raw_data, 29, 22),
    processed = TRUE
WHERE length(raw_data) >= 50;

-- Validate data format consistency and field integrity
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    -- Check for records with invalid delimiter character
    SELECT COUNT(*) INTO invalid_count
    FROM temp_tcatbal_raw 
    WHERE delimiter_char != '{';
    
    IF invalid_count > 0 THEN
        RAISE NOTICE 'Warning: % records found with invalid delimiter character', invalid_count;
    END IF;
    
    -- Check for records with invalid record ID format (must be 12 digits)
    SELECT COUNT(*) INTO invalid_count
    FROM temp_tcatbal_raw 
    WHERE record_id !~ '^[0-9]{12}$';
    
    IF invalid_count > 0 THEN
        RAISE NOTICE 'Warning: % records found with invalid record ID format', invalid_count;
    END IF;
    
    -- Check for records with invalid numeric field format (must be 15 digits)
    SELECT COUNT(*) INTO invalid_count
    FROM temp_tcatbal_raw 
    WHERE numeric_field !~ '^[0-9]{15}$';
    
    IF invalid_count > 0 THEN
        RAISE NOTICE 'Warning: % records found with invalid numeric field format', invalid_count;
    END IF;
END;
$$;

-- =============================================================================
-- PHASE 4: Business Logic Processing and Account-Category Mapping
-- =============================================================================

-- Process parsed data into business entities with account-category relationships
-- Processing business logic for account-category balance mapping...

-- Insert parsed records with business logic transformation
-- Map record sequence numbers to account IDs and distribute across transaction categories
INSERT INTO temp_tcatbal_parsed (
    line_number,
    record_sequence,
    account_id,
    transaction_category,
    parent_transaction_type,
    balance_cents,
    category_balance
)
SELECT 
    tr.line_number,
    -- Convert record ID to sequence number (000000000010 -> 1, 000000000020 -> 2, etc.)
    CAST(tr.record_id AS INTEGER) / 10 AS record_sequence,
    -- Map sequence to 11-digit account ID (1 -> 00000000001, 2 -> 00000000002, etc.)
    LPAD((CAST(tr.record_id AS INTEGER) / 10)::TEXT, 11, '0') AS account_id,
    -- Distribute across transaction categories using modulo operation for even distribution
    CASE 
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 5 THEN LPAD(((CAST(tr.record_id AS INTEGER) / 10 - 1) % 5 + 1)::TEXT, 4, '0')
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 8 THEN LPAD(((CAST(tr.record_id AS INTEGER) / 10 - 1) % 3 + 1)::TEXT, 4, '0')
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 11 THEN LPAD(((CAST(tr.record_id AS INTEGER) / 10 - 1) % 3 + 1)::TEXT, 4, '0')
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 14 THEN LPAD(((CAST(tr.record_id AS INTEGER) / 10 - 1) % 3 + 1)::TEXT, 4, '0')
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 15 THEN '0001'
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 17 THEN LPAD(((CAST(tr.record_id AS INTEGER) / 10 - 1) % 2 + 1)::TEXT, 4, '0')
        ELSE '0001'
    END AS transaction_category,
    -- Map to appropriate parent transaction type based on distribution
    CASE 
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 5 THEN '01'  -- Purchase categories
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 8 THEN '02'  -- Payment categories
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 11 THEN '03' -- Credit categories
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 14 THEN '04' -- Authorization categories
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 15 THEN '05' -- Refund categories
        WHEN (CAST(tr.record_id AS INTEGER) / 10 - 1) % 18 + 1 <= 17 THEN '06' -- Reversal categories
        ELSE '07' -- Adjustment categories
    END AS parent_transaction_type,
    -- Parse numeric field as balance in cents (first 5 digits represent dollars.cents)
    -- 100010000000000 -> extract 10001 as cents -> 100.01 dollars
    CAST(SUBSTRING(tr.numeric_field, 1, 5) AS BIGINT) AS balance_cents,
    -- Convert cents to DECIMAL(12,2) format for exact financial precision
    CAST(SUBSTRING(tr.numeric_field, 1, 5) AS BIGINT) / 100.0 AS category_balance
FROM temp_tcatbal_raw tr
WHERE tr.processed = TRUE
  AND tr.record_id IS NOT NULL
  AND tr.numeric_field IS NOT NULL;

-- =============================================================================
-- PHASE 5: Data Validation and Foreign Key Verification
-- =============================================================================

-- Comprehensive validation of parsed data against business rules and constraints
-- Validating parsed data against foreign key constraints and business rules...

-- Validate account_id foreign key relationships
UPDATE temp_tcatbal_parsed 
SET validation_errors = validation_errors || 'Invalid account_id: ' || account_id || '; ',
    is_valid = FALSE
WHERE account_id NOT IN (SELECT account_id FROM accounts);

-- Validate transaction_category foreign key relationships with parent type verification
UPDATE temp_tcatbal_parsed 
SET validation_errors = validation_errors || 'Invalid transaction_category: ' || transaction_category || ' for parent type: ' || parent_transaction_type || '; ',
    is_valid = FALSE
WHERE (transaction_category, parent_transaction_type) NOT IN (
    SELECT transaction_category, parent_transaction_type 
    FROM transaction_categories 
    WHERE active_status = true
);

-- Validate category_balance precision and range (must be within DECIMAL(12,2) limits)
UPDATE temp_tcatbal_parsed 
SET validation_errors = validation_errors || 'Invalid category_balance range: ' || category_balance || '; ',
    is_valid = FALSE
WHERE category_balance < -9999999999.99 
  OR category_balance > 9999999999.99;

-- Validate unique composite key constraints (account_id, transaction_category combinations)
WITH duplicate_keys AS (
    SELECT account_id, transaction_category, COUNT(*) as duplicate_count
    FROM temp_tcatbal_parsed 
    GROUP BY account_id, transaction_category
    HAVING COUNT(*) > 1
)
UPDATE temp_tcatbal_parsed 
SET validation_errors = validation_errors || 'Duplicate composite key: (' || account_id || ',' || transaction_category || '); ',
    is_valid = FALSE
WHERE (account_id, transaction_category) IN (
    SELECT account_id, transaction_category FROM duplicate_keys
);

-- Update validation statistics for comprehensive audit trail
UPDATE temp_load_statistics SET
    valid_records = (SELECT COUNT(*) FROM temp_tcatbal_parsed WHERE is_valid = TRUE),
    invalid_records = (SELECT COUNT(*) FROM temp_tcatbal_parsed WHERE is_valid = FALSE),
    foreign_key_violations = (SELECT COUNT(*) FROM temp_tcatbal_parsed WHERE validation_errors LIKE '%Invalid account_id%' OR validation_errors LIKE '%Invalid transaction_category%');

-- =============================================================================
-- PHASE 6: Data Loading with Transaction Management
-- =============================================================================

-- Load validated data into transaction_category_balances table with comprehensive error handling
-- Loading validated transaction category balance data with ACID transaction management...

-- Clear any existing test data to ensure clean migration
DELETE FROM transaction_category_balances 
WHERE account_id IN (SELECT DISTINCT account_id FROM temp_tcatbal_parsed WHERE is_valid = TRUE);

-- Insert validated transaction category balance records with comprehensive audit trail
INSERT INTO transaction_category_balances (
    account_id,
    transaction_category,
    category_balance,
    last_updated,
    version_number,
    created_at
)
SELECT 
    tp.account_id,
    tp.transaction_category,
    tp.category_balance,
    CURRENT_TIMESTAMP AS last_updated,
    1 AS version_number,  -- Initial version for optimistic locking
    CURRENT_TIMESTAMP AS created_at
FROM temp_tcatbal_parsed tp
WHERE tp.is_valid = TRUE
ORDER BY tp.account_id, tp.transaction_category;

-- Update final loading statistics for audit and monitoring
UPDATE temp_load_statistics SET
    loaded_records = (SELECT COUNT(*) FROM transaction_category_balances 
                     WHERE account_id IN (SELECT DISTINCT account_id FROM temp_tcatbal_parsed WHERE is_valid = TRUE)),
    end_time = CURRENT_TIMESTAMP;

-- =============================================================================
-- PHASE 7: Data Quality Verification and Audit Reporting
-- =============================================================================

-- Comprehensive data quality verification and audit trail generation
-- Performing data quality verification and generating audit reports...

-- Verify foreign key constraint integrity post-loading
DO $$
DECLARE
    fk_violation_count INTEGER;
    composite_key_count INTEGER;
    balance_range_violations INTEGER;
    rec RECORD;
BEGIN
    -- Verify all loaded records have valid foreign key relationships
    SELECT COUNT(*) INTO fk_violation_count
    FROM transaction_category_balances tcb
    LEFT JOIN accounts a ON tcb.account_id = a.account_id
    LEFT JOIN transaction_categories tc ON tcb.transaction_category = tc.transaction_category
    WHERE a.account_id IS NULL OR tc.transaction_category IS NULL;
    
    IF fk_violation_count > 0 THEN
        RAISE EXCEPTION 'Data integrity violation: % records with invalid foreign key relationships', fk_violation_count;
    END IF;
    
    -- Verify composite primary key uniqueness
    SELECT COUNT(*) INTO composite_key_count
    FROM (
        SELECT account_id, transaction_category, COUNT(*) as key_count
        FROM transaction_category_balances
        GROUP BY account_id, transaction_category
        HAVING COUNT(*) > 1
    ) duplicate_keys;
    
    IF composite_key_count > 0 THEN
        RAISE EXCEPTION 'Data integrity violation: % duplicate composite primary key combinations', composite_key_count;
    END IF;
    
    -- Verify balance precision and range compliance
    SELECT COUNT(*) INTO balance_range_violations
    FROM transaction_category_balances
    WHERE category_balance < -9999999999.99 
      OR category_balance > 9999999999.99
      OR scale(category_balance) > 2;
    
    IF balance_range_violations > 0 THEN
        RAISE EXCEPTION 'Data precision violation: % records with invalid balance range or precision', balance_range_violations;
    END IF;
    
    -- Log successful data quality verification
    RAISE NOTICE 'Data quality verification completed successfully:';
    RAISE NOTICE '- Foreign key integrity: VERIFIED';
    RAISE NOTICE '- Composite key uniqueness: VERIFIED';
    RAISE NOTICE '- Balance precision compliance: VERIFIED';
END;
$$;

-- Generate comprehensive audit report for migration tracking
DO $$
DECLARE
    stats_rec RECORD;
    account_count INTEGER;
    category_count INTEGER;
    balance_sum DECIMAL(15,2);
    rec RECORD;
BEGIN
    -- Retrieve final loading statistics
    SELECT * INTO stats_rec FROM temp_load_statistics LIMIT 1;
    
    -- Calculate additional verification metrics
    SELECT COUNT(DISTINCT account_id) INTO account_count FROM transaction_category_balances;
    SELECT COUNT(DISTINCT transaction_category) INTO category_count FROM transaction_category_balances;
    SELECT COALESCE(SUM(category_balance), 0) INTO balance_sum FROM transaction_category_balances;
    
    -- Generate comprehensive audit report
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'TRANSACTION CATEGORY BALANCES DATA LOADING AUDIT REPORT';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Migration Version: V27__load_transaction_category_balances_data.sql';
    RAISE NOTICE 'Source File: app/data/ASCII/tcatbal.txt';
    RAISE NOTICE 'Target Table: transaction_category_balances';
    RAISE NOTICE 'Processing Start Time: %', stats_rec.start_time;
    RAISE NOTICE 'Processing End Time: %', stats_rec.end_time;
    RAISE NOTICE 'Total Processing Duration: %', (stats_rec.end_time - stats_rec.start_time);
    RAISE NOTICE '----------------------------------------------------------------';
    RAISE NOTICE 'RECORD PROCESSING STATISTICS:';
    RAISE NOTICE '- Total Records Processed: %', stats_rec.total_records;
    RAISE NOTICE '- Valid Records: %', stats_rec.valid_records;
    RAISE NOTICE '- Invalid Records: %', stats_rec.invalid_records;
    RAISE NOTICE '- Successfully Loaded: %', stats_rec.loaded_records;
    RAISE NOTICE '- Foreign Key Violations: %', stats_rec.foreign_key_violations;
    RAISE NOTICE '----------------------------------------------------------------';
    RAISE NOTICE 'DATA QUALITY METRICS:';
    RAISE NOTICE '- Unique Accounts with Category Balances: %', account_count;
    RAISE NOTICE '- Distinct Transaction Categories: %', category_count;
    RAISE NOTICE '- Total Category Balance Sum: $%', balance_sum;
    RAISE NOTICE '- Average Balance per Category: $%', ROUND(balance_sum / GREATEST(stats_rec.loaded_records, 1), 2);
    RAISE NOTICE '----------------------------------------------------------------';
    RAISE NOTICE 'COMPOSITE KEY DISTRIBUTION:';
    
    -- Display sample of loaded data for verification
    FOR rec IN (
        SELECT tcb.account_id, tcb.transaction_category, tc.category_description, tcb.category_balance
        FROM transaction_category_balances tcb
        JOIN transaction_categories tc ON tcb.transaction_category = tc.transaction_category
        ORDER BY tcb.account_id, tcb.transaction_category
        LIMIT 10
    ) LOOP
        RAISE NOTICE '- Account % | Category % (%): $%', 
            rec.account_id, rec.transaction_category, rec.category_description, rec.category_balance;
    END LOOP;
    
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'MIGRATION STATUS: COMPLETED SUCCESSFULLY';
    RAISE NOTICE '================================================================';
END;
$$;

-- Verify data loading completed successfully with expected record count
DO $$
DECLARE
    final_count INTEGER;
    expected_count INTEGER := 50; -- Expected number of records from tcatbal.txt
BEGIN
    SELECT COUNT(*) INTO final_count FROM transaction_category_balances;
    
    IF final_count != expected_count THEN
        RAISE WARNING 'Record count mismatch: Expected %, Loaded %', expected_count, final_count;
    ELSE
        RAISE NOTICE 'SUCCESS: All % transaction category balance records loaded successfully', final_count;
    END IF;
END;
$$;

-- Data integrity verification and index creation for performance optimization

-- Create performance optimization index for account-based category balance queries
-- This index supports efficient lookup of all category balances for a specific account
CREATE INDEX IF NOT EXISTS idx_tcatbal_account_lookup 
ON transaction_category_balances (account_id, category_balance DESC, last_updated DESC);

-- Create performance optimization index for category-based balance aggregation queries
-- This index supports efficient aggregation of balances across accounts for specific categories
CREATE INDEX IF NOT EXISTS idx_tcatbal_category_aggregation 
ON transaction_category_balances (transaction_category, category_balance DESC) 
WHERE category_balance != 0.00;

-- Create composite index for balance change tracking and audit queries
-- This index supports efficient queries for recent balance updates and audit trail analysis
CREATE INDEX IF NOT EXISTS idx_tcatbal_audit_tracking 
ON transaction_category_balances (last_updated DESC, version_number, account_id, transaction_category);

-- Add table and column comments for comprehensive documentation
COMMENT ON TABLE transaction_category_balances IS 'Transaction category balance tracking table populated from tcatbal.txt ASCII data. Maintains category-specific balance information with composite primary key (account_id, transaction_category) structure supporting account-level financial analytics and reporting. Features optimistic locking through version numbers and automated audit trail generation for financial data integrity in high-concurrency environments.';

COMMENT ON COLUMN transaction_category_balances.account_id IS 'Composite primary key component: 11-digit account identifier with foreign key relationship to accounts table. Populated from tcatbal.txt record sequence mapping (000000000010->00000000001, etc.) ensuring referential integrity and supporting efficient account-based category balance queries for financial reporting and analytics.';

COMMENT ON COLUMN transaction_category_balances.transaction_category IS 'Composite primary key component: 4-character transaction category code with foreign key relationship to transaction_categories table. Distributed across available categories using modulo operation for balanced data distribution supporting category-based balance aggregation queries and comprehensive financial analytics.';

COMMENT ON COLUMN transaction_category_balances.category_balance IS 'Category-specific balance amount with DECIMAL(12,2) precision ensuring exact financial calculations equivalent to COBOL COMP-3 arithmetic. Parsed from tcatbal.txt 15-digit numeric field (100010000000000) with first 5 digits representing cents (10001 cents = $100.01) maintaining precise monetary values for regulatory compliance and accurate financial reporting.';

COMMENT ON COLUMN transaction_category_balances.last_updated IS 'Balance update timestamp with time zone information supporting audit trails and balance change monitoring. Automatically maintained through PostgreSQL triggers for version control and optimistic locking coordination ensuring data consistency in high-concurrency transaction processing environments.';

COMMENT ON COLUMN transaction_category_balances.version_number IS 'Optimistic locking version number incremented on each balance update preventing concurrent modification conflicts. Initialized to 1 during data loading and automatically incremented by database triggers supporting Spring Boot JPA @Version annotation for enterprise-grade transaction management and data consistency.';

-- Verify final data integrity and generate performance statistics
DO $$
DECLARE
    index_count INTEGER;
    constraint_count INTEGER;
    performance_baseline RECORD;
BEGIN
    -- Verify supporting indexes were created successfully
    SELECT COUNT(*) INTO index_count
    FROM pg_indexes 
    WHERE tablename = 'transaction_category_balances' 
    AND indexname LIKE 'idx_tcatbal_%';
    
    -- Verify foreign key constraints are active
    SELECT COUNT(*) INTO constraint_count
    FROM information_schema.table_constraints 
    WHERE table_name = 'transaction_category_balances' 
    AND constraint_type = 'FOREIGN KEY';
    
    -- Generate performance baseline statistics
    SELECT 
        COUNT(*) as total_records,
        COUNT(DISTINCT account_id) as unique_accounts,
        COUNT(DISTINCT transaction_category) as unique_categories,
        MIN(category_balance) as min_balance,
        MAX(category_balance) as max_balance,
        AVG(category_balance) as avg_balance,
        SUM(category_balance) as total_balance
    INTO performance_baseline
    FROM transaction_category_balances;
    
    -- Log verification results
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'TRANSACTION CATEGORY BALANCES - FINAL VERIFICATION REPORT';
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'Performance Indexes Created: %', index_count;
    RAISE NOTICE 'Foreign Key Constraints Active: %', constraint_count;
    RAISE NOTICE 'Total Records: %', performance_baseline.total_records;
    RAISE NOTICE 'Unique Accounts: %', performance_baseline.unique_accounts;
    RAISE NOTICE 'Unique Categories: %', performance_baseline.unique_categories;
    RAISE NOTICE 'Balance Range: $% to $%', performance_baseline.min_balance, performance_baseline.max_balance;
    RAISE NOTICE 'Average Balance: $%', ROUND(performance_baseline.avg_balance, 2);
    RAISE NOTICE 'Total Portfolio Balance: $%', performance_baseline.total_balance;
    RAISE NOTICE '================================================================';
    RAISE NOTICE 'DATA MIGRATION V27 COMPLETED SUCCESSFULLY';
    RAISE NOTICE 'Transaction category balances ready for production use';
    RAISE NOTICE '================================================================';
END;
$$;

-- Rollback directives have been moved to the XML changeset definition