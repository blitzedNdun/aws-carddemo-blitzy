-- ==============================================================================
-- Liquibase Data Migration: V22__load_accounts_data.sql
-- Description: Populates accounts table from acctdata.txt with precise financial field mapping, customer relationships, and disclosure group associations supporting comprehensive account management operations
-- Author: Blitzy agent
-- Version: 22.0
-- Migration Type: DATA LOADING with DECIMAL(12,2) precision, foreign key integrity, and COBOL COMP-3 equivalence
-- ==============================================================================

-- This file is now included via XML changeset in liquibase-changelog.xml
-- Liquibase-specific comments have been moved to the XML changeset definition
--comment: Load account data from acctdata.txt with exact 11-digit account_id preservation, DECIMAL(12,2) precision for monetary fields, customer-account relationships, and disclosure group associations

-- =============================================================================
-- PHASE 1: Data Validation and Preparation
-- =============================================================================

-- Create temporary working table for raw acctdata.txt parsing
CREATE TEMPORARY TABLE temp_acctdata_raw (
    line_number SERIAL,
    raw_data TEXT NOT NULL,
    processed BOOLEAN DEFAULT FALSE
);

-- Create temporary table for parsed account records with validation
CREATE TEMPORARY TABLE temp_accounts_parsed (
    line_number INTEGER,
    account_id VARCHAR(11),
    active_status_char CHAR(1),
    active_status BOOLEAN,
    customer_id VARCHAR(9),
    current_balance_cents BIGINT,
    current_balance DECIMAL(12,2),
    credit_limit_cents BIGINT,
    credit_limit DECIMAL(12,2),
    cash_credit_limit_cents BIGINT,
    cash_credit_limit DECIMAL(12,2),
    open_date_str VARCHAR(10),
    open_date DATE,
    expiration_date_str VARCHAR(10),
    expiration_date DATE,
    reissue_date_str VARCHAR(10),
    reissue_date DATE,
    current_cycle_credit_cents BIGINT,
    current_cycle_credit DECIMAL(12,2),
    current_cycle_debit_cents BIGINT,
    current_cycle_debit DECIMAL(12,2),
    group_id VARCHAR(10),
    validation_errors TEXT DEFAULT '',
    is_valid BOOLEAN DEFAULT TRUE
);

-- Create temporary table for data loading statistics
CREATE TEMPORARY TABLE temp_load_statistics (
    total_records INTEGER DEFAULT 0,
    valid_records INTEGER DEFAULT 0,
    invalid_records INTEGER DEFAULT 0,
    skipped_records INTEGER DEFAULT 0,
    loaded_records INTEGER DEFAULT 0,
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE
);

-- Initialize statistics record
INSERT INTO temp_load_statistics (total_records) VALUES (0);

-- =============================================================================
-- PHASE 2: Raw Data Loading from acctdata.txt
-- =============================================================================

-- Load raw account data from acctdata.txt using COPY command for optimal performance
-- Note: In production, this would use COPY FROM file, but for this migration we'll use INSERT statements
DO $$ BEGIN RAISE NOTICE 'Loading account data from acctdata.txt...'; END $$;

-- Insert account records from acctdata.txt with exact line preservation
-- Format: account_id(11)active_status(1)customer_id(9){current_balance{credit_limit{cash_credit_limit{dates(30)current_cycle_credit{current_cycle_debit{group_id(10)
INSERT INTO temp_acctdata_raw (raw_data) VALUES
('00000000001Y00000001940{00000020200{00000010200{2014-11-202025-05-202025-05-2000000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000002Y00000001580{00000061300{00000054480{2013-06-192024-08-112024-08-1100000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000003Y00000001470{00000049090{00000005380{2013-08-232024-01-102024-01-1000000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000004Y00000000400{00000035030{00000027890{2012-11-172023-12-162023-12-1600000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000005Y00000003450{00000038190{00000024300{2012-10-032025-03-092025-03-0900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000006Y00000002180{00000035840{00000029480{2017-12-232025-10-082025-10-0800000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000007Y00000001930{00000020650{00000002640{2012-10-122024-12-132024-12-1300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000008Y00000006050{00000061040{00000013180{2012-01-042024-05-202024-05-2000000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000009Y00000005600{00000082010{00000020650{2016-08-272024-12-272024-12-2700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000010Y00000001590{00000054010{00000044420{2015-09-132023-01-272023-01-2700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000011Y00000002120{00000049980{00000031750{2014-09-122025-03-122025-03-1200000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000012Y00000001760{00000046360{00000003880{2009-06-172023-07-072023-07-0700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000013Y00000000410{00000075420{00000049220{2017-10-012024-08-042024-08-0400000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000014Y00000000150{00000022540{00000002120{2010-12-042025-12-112025-12-1100000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000015Y00000004890{00000084410{00000038330{2009-10-062025-06-092025-06-0900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000016Y00000007330{00000089220{00000026320{2014-09-112024-01-252024-01-2500000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000017Y00000000330{00000005680{00000005100{2014-05-172025-03-012025-03-0100000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000018Y00000001440{00000029030{00000014960{2018-11-152023-09-102023-09-1000000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000019Y00000004800{00000069860{00000037230{2011-12-142025-07-232025-07-2300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000020Y00000003690{00000037670{00000010400{2014-02-272024-03-132024-03-1300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000021Y00000001120{00000012640{00000001800{2011-10-192023-01-062023-01-0600000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000022Y00000000550{00000085990{00000047120{2016-11-212025-12-282025-12-2800000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000023Y00000001040{00000033770{00000029040{2012-03-152025-03-182025-03-1800000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000024Y00000004000{00000051740{00000041290{2015-08-082025-02-112025-02-1100000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000025Y00000000610{00000081940{00000065820{2012-10-262025-07-102025-07-1000000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000026Y00000000460{00000021810{00000013750{2009-04-202024-12-192024-12-1900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000027Y00000002840{00000055720{00000020750{2012-09-302025-07-132025-07-1300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000028Y00000000680{00000008680{00000005470{2015-05-202024-05-092024-05-0900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000029Y00000003390{00000055110{00000043610{2015-11-032024-06-042024-06-0400000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000030Y00000000020{00000001200{00000000930{2011-08-262024-06-272024-06-2700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000031Y00000000310{00000011400{00000010770{2017-02-252025-06-082025-06-0800000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000032Y00000000300{00000011750{00000008460{2013-11-102025-05-192025-05-1900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000033Y00000004100{00000064040{00000009510{2012-10-112025-10-072025-10-0700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000034Y00000002530{00000036420{00000027700{2009-05-102025-10-062025-10-0600000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000035Y00000001660{00000019470{00000015250{2018-02-022025-09-232025-09-2300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000036Y00000001100{00000033280{00000008390{2018-07-182024-12-232024-12-2300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000037Y00000000070{00000004460{00000001660{2016-09-102023-10-242023-10-2400000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000038Y00000006120{00000065050{00000034760{2010-08-122023-07-232023-07-2300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000039Y00000008430{00000097500{00000062120{2018-08-262025-09-082025-09-0800000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000040Y00000000430{00000058230{00000016740{2010-02-132023-10-272023-10-2700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000041Y00000003750{00000067210{00000034290{2015-02-072023-04-242023-04-2400000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000042Y00000003020{00000065630{00000051030{2016-09-192025-09-192025-09-1900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000043Y00000006100{00000061680{00000012060{2012-04-092025-08-292025-08-2900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000044Y00000002630{00000068990{00000044320{2018-12-012024-01-172024-01-1700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000045Y00000001860{00000027190{00000006880{2010-12-312025-07-092025-07-0900000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000046Y00000003960{00000070070{00000054380{2013-09-062025-06-202025-06-2000000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000047Y00000000320{00000023380{00000001590{2014-04-032025-08-232025-08-2300000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000048Y00000002260{00000023060{00000006120{2017-03-182025-02-062025-02-0600000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000049Y00000001000{00000090480{00000048070{2019-04-062023-09-172023-09-1700000000000{00000000000{A000000000                                                                                                                                                                                            '),
('00000000050Y00000004920{00000061690{00000045870{2011-04-222023-03-092023-03-0900000000000{00000000000{A000000000                                                                                                                                                                                            ');

-- Update total record count
UPDATE temp_load_statistics SET total_records = (SELECT COUNT(*) FROM temp_acctdata_raw);

-- =============================================================================
-- PHASE 3: Data Parsing and Field Extraction
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Parsing account data with "{" delimiter processing...'; END $$;

-- Parse account records with comprehensive field extraction and validation
-- Record format: account_id(11)active_status(1)customer_id(9){current_balance{credit_limit{cash_credit_limit{dates(30)current_cycle_credit{current_cycle_debit{group_id(10)
INSERT INTO temp_accounts_parsed (
    line_number,
    account_id,
    active_status_char,
    customer_id,
    current_balance_cents,
    credit_limit_cents,
    cash_credit_limit_cents,
    open_date_str,
    expiration_date_str,
    reissue_date_str,
    current_cycle_credit_cents,
    current_cycle_debit_cents,
    group_id
)
SELECT
    tar.line_number,
    -- Extract 11-digit account_id from positions 1-11
    SUBSTRING(tar.raw_data, 1, 11) as account_id,
    -- Extract active status flag from position 12
    SUBSTRING(tar.raw_data, 12, 1) as active_status_char,
    -- Extract 9-digit customer_id from positions 13-21
    SUBSTRING(tar.raw_data, 13, 9) as customer_id,
    -- Parse using position-based extraction to handle embedded delimiters
    -- current_balance: positions 22-32 (11 chars)
    CAST(SUBSTRING(tar.raw_data, 22, 11) AS BIGINT) as current_balance_cents,
    -- credit_limit: positions 34-44 (11 chars, skip delimiter at 33)
    CAST(SUBSTRING(tar.raw_data, 34, 11) AS BIGINT) as credit_limit_cents,
    -- cash_credit_limit: positions 46-56 (11 chars, skip delimiter at 45)
    CAST(SUBSTRING(tar.raw_data, 46, 11) AS BIGINT) as cash_credit_limit_cents,
    -- Dates are embedded in field 3 without delimiters: positions 57-86 (30 chars)
    -- open_date: positions 57-66 (10 chars)
    SUBSTRING(tar.raw_data, 57, 10) as open_date_str,
    -- expiration_date: positions 67-76 (10 chars)
    SUBSTRING(tar.raw_data, 67, 10) as expiration_date_str,
    -- reissue_date: positions 77-86 (10 chars)
    SUBSTRING(tar.raw_data, 77, 10) as reissue_date_str,
    -- current_cycle_credit: positions 87-97 (11 chars)
    CAST(SUBSTRING(tar.raw_data, 87, 11) AS BIGINT) as current_cycle_credit_cents,
    -- current_cycle_debit: positions 99-109 (11 chars, skip delimiter at 98)
    CAST(SUBSTRING(tar.raw_data, 99, 11) AS BIGINT) as current_cycle_debit_cents,
    -- group_id: positions 111-120 (10 chars, skip delimiter at 110)
    TRIM(SUBSTRING(tar.raw_data, 111, 10)) as group_id
FROM temp_acctdata_raw tar
WHERE NOT tar.processed;

-- Mark raw records as processed
UPDATE temp_acctdata_raw SET processed = TRUE;

-- =============================================================================
-- PHASE 4: Data Type Conversion and Validation
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Converting data types and validating field formats...'; END $$;

-- Convert parsed string fields to appropriate data types with comprehensive validation
UPDATE temp_accounts_parsed SET
    -- Convert active status from Y/N to BOOLEAN
    active_status = CASE 
        WHEN active_status_char = 'Y' THEN TRUE
        WHEN active_status_char = 'N' THEN FALSE
        ELSE NULL
    END,
    
    -- Convert monetary amounts from cents to DECIMAL(12,2) with exact precision
    current_balance = CAST(current_balance_cents AS DECIMAL(12,2)) / 100.00,
    credit_limit = CAST(credit_limit_cents AS DECIMAL(12,2)) / 100.00,
    cash_credit_limit = CAST(cash_credit_limit_cents AS DECIMAL(12,2)) / 100.00,
    current_cycle_credit = CAST(current_cycle_credit_cents AS DECIMAL(12,2)) / 100.00,
    current_cycle_debit = CAST(current_cycle_debit_cents AS DECIMAL(12,2)) / 100.00,
    
    -- Convert date strings to DATE type with ISO format validation
    open_date = CASE
        WHEN open_date_str ~ '^\d{4}-\d{2}-\d{2}$' THEN CAST(open_date_str AS DATE)
        ELSE NULL
    END,
    expiration_date = CASE
        WHEN expiration_date_str ~ '^\d{4}-\d{2}-\d{2}$' THEN CAST(expiration_date_str AS DATE)
        ELSE NULL
    END,
    reissue_date = CASE
        WHEN reissue_date_str ~ '^\d{4}-\d{2}-\d{2}$' THEN CAST(reissue_date_str AS DATE)
        ELSE NULL
    END;

-- =============================================================================
-- PHASE 5: Comprehensive Data Validation
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Performing comprehensive data validation...'; END $$;

-- Validate parsed records and accumulate validation errors
UPDATE temp_accounts_parsed SET
    validation_errors = CONCAT_WS('; ',
        -- Account ID validation
        CASE WHEN account_id IS NULL OR account_id !~ '^[0-9]{11}$' 
             THEN 'Invalid account_id format: must be 11 digits' 
             ELSE NULL END,
        
        -- Customer ID validation
        CASE WHEN customer_id IS NULL OR customer_id !~ '^[0-9]{9}$' 
             THEN 'Invalid customer_id format: must be 9 digits' 
             ELSE NULL END,
        
        -- Active status validation
        CASE WHEN active_status IS NULL 
             THEN 'Invalid active_status: must be Y or N' 
             ELSE NULL END,
        
        -- Current balance validation
        CASE WHEN current_balance IS NULL OR current_balance < -999999999.99 OR current_balance > 999999999.99
             THEN 'Invalid current_balance: must be within DECIMAL(12,2) range' 
             ELSE NULL END,
        
        -- Credit limit validation
        CASE WHEN credit_limit IS NULL OR credit_limit < 0.00 OR credit_limit > 999999999.99
             THEN 'Invalid credit_limit: must be positive and within DECIMAL(12,2) range' 
             ELSE NULL END,
        
        -- Cash credit limit validation
        CASE WHEN cash_credit_limit IS NULL OR cash_credit_limit < 0.00 OR cash_credit_limit > 999999999.99
             THEN 'Invalid cash_credit_limit: must be positive and within DECIMAL(12,2) range' 
             ELSE NULL END,
        
        -- Date validations
        CASE WHEN open_date IS NULL 
             THEN 'Invalid open_date: must be valid ISO date format' 
             ELSE NULL END,
        CASE WHEN expiration_date IS NULL 
             THEN 'Invalid expiration_date: must be valid ISO date format' 
             ELSE NULL END,
        CASE WHEN reissue_date IS NULL 
             THEN 'Invalid reissue_date: must be valid ISO date format' 
             ELSE NULL END,
        
        -- Date relationship validations
        CASE WHEN open_date IS NOT NULL AND expiration_date IS NOT NULL AND expiration_date <= open_date
             THEN 'Invalid date relationship: expiration_date must be after open_date' 
             ELSE NULL END,
        CASE WHEN open_date IS NOT NULL AND reissue_date IS NOT NULL AND reissue_date < open_date
             THEN 'Invalid date relationship: reissue_date must be on or after open_date' 
             ELSE NULL END,
        
        -- Current cycle validation
        CASE WHEN current_cycle_credit IS NULL OR current_cycle_credit < 0.00 OR current_cycle_credit > 999999999.99
             THEN 'Invalid current_cycle_credit: must be positive and within DECIMAL(12,2) range' 
             ELSE NULL END,
        CASE WHEN current_cycle_debit IS NULL OR current_cycle_debit < 0.00 OR current_cycle_debit > 999999999.99
             THEN 'Invalid current_cycle_debit: must be positive and within DECIMAL(12,2) range' 
             ELSE NULL END,
        
        -- Group ID validation
        CASE WHEN group_id IS NULL OR LENGTH(TRIM(group_id)) = 0 OR LENGTH(group_id) > 10
             THEN 'Invalid group_id: must be non-empty and max 10 characters' 
             ELSE NULL END
    ),
    
    -- Mark record as valid only if no validation errors
    is_valid = (
        account_id ~ '^[0-9]{11}$' AND
        customer_id ~ '^[0-9]{9}$' AND
        active_status IS NOT NULL AND
        current_balance IS NOT NULL AND current_balance >= -999999999.99 AND current_balance <= 999999999.99 AND
        credit_limit IS NOT NULL AND credit_limit >= 0.00 AND credit_limit <= 999999999.99 AND
        cash_credit_limit IS NOT NULL AND cash_credit_limit >= 0.00 AND cash_credit_limit <= 999999999.99 AND
        open_date IS NOT NULL AND
        expiration_date IS NOT NULL AND
        reissue_date IS NOT NULL AND
        expiration_date > open_date AND
        reissue_date >= open_date AND
        current_cycle_credit IS NOT NULL AND current_cycle_credit >= 0.00 AND current_cycle_credit <= 999999999.99 AND
        current_cycle_debit IS NOT NULL AND current_cycle_debit >= 0.00 AND current_cycle_debit <= 999999999.99 AND
        group_id IS NOT NULL AND LENGTH(TRIM(group_id)) > 0 AND LENGTH(group_id) <= 10
    );

-- Update validation statistics
UPDATE temp_load_statistics SET
    valid_records = (SELECT COUNT(*) FROM temp_accounts_parsed WHERE is_valid = TRUE),
    invalid_records = (SELECT COUNT(*) FROM temp_accounts_parsed WHERE is_valid = FALSE);

-- =============================================================================
-- PHASE 6: Foreign Key Validation
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Validating foreign key relationships...'; END $$;

-- Validate customer_id references exist in customers table
UPDATE temp_accounts_parsed tap SET
    validation_errors = CONCAT_WS('; ', validation_errors, 
        CASE WHEN NOT EXISTS (SELECT 1 FROM customers c WHERE c.customer_id = tap.customer_id)
             THEN 'Foreign key violation: customer_id does not exist in customers table'
             ELSE NULL END
    ),
    is_valid = (is_valid AND EXISTS (SELECT 1 FROM customers c WHERE c.customer_id = tap.customer_id))
WHERE tap.is_valid = TRUE;

-- Validate group_id references exist in disclosure_groups table
-- Note: Due to composite primary key, we'll check if any record exists for the group_id
UPDATE temp_accounts_parsed tap SET
    validation_errors = CONCAT_WS('; ', validation_errors, 
        CASE WHEN NOT EXISTS (SELECT 1 FROM disclosure_groups dg WHERE dg.group_id = tap.group_id)
             THEN 'Foreign key violation: group_id does not exist in disclosure_groups table'
             ELSE NULL END
    ),
    is_valid = (is_valid AND EXISTS (SELECT 1 FROM disclosure_groups dg WHERE dg.group_id = tap.group_id))
WHERE tap.is_valid = TRUE;

-- Update final validation statistics
UPDATE temp_load_statistics SET
    valid_records = (SELECT COUNT(*) FROM temp_accounts_parsed WHERE is_valid = TRUE),
    invalid_records = (SELECT COUNT(*) FROM temp_accounts_parsed WHERE is_valid = FALSE);

-- =============================================================================
-- PHASE 7: Data Loading into Accounts Table
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Loading validated account data into accounts table...'; END $$;

-- Insert valid account records into the accounts table with comprehensive field mapping
INSERT INTO accounts (
    account_id,
    customer_id,
    active_status,
    current_balance,
    credit_limit,
    cash_credit_limit,
    open_date,
    expiration_date,
    reissue_date,
    current_cycle_credit,
    current_cycle_debit,
    address_zip,  -- Not available in source data, setting to NULL
    group_id,
    created_at,
    updated_at
)
SELECT
    tap.account_id,
    tap.customer_id,
    tap.active_status,
    tap.current_balance,
    tap.credit_limit,
    tap.cash_credit_limit,
    tap.open_date,
    tap.expiration_date,
    tap.reissue_date,
    tap.current_cycle_credit,
    tap.current_cycle_debit,
    NULL as address_zip,  -- Address ZIP not available in acctdata.txt
    tap.group_id,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM temp_accounts_parsed tap
WHERE tap.is_valid = TRUE
ORDER BY tap.account_id;

-- Update final loading statistics
UPDATE temp_load_statistics SET
    loaded_records = (SELECT COUNT(*) FROM accounts WHERE created_at >= start_time),
    end_time = CURRENT_TIMESTAMP;

-- =============================================================================
-- PHASE 8: Data Validation and Integrity Verification
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Performing post-load data integrity verification...'; END $$;

-- Verify account data integrity after loading
DO $$
DECLARE
    total_loaded INTEGER;
    integrity_errors INTEGER DEFAULT 0;
    error_details TEXT DEFAULT '';
BEGIN
    -- Get total loaded records
    SELECT loaded_records INTO total_loaded FROM temp_load_statistics;
    
    -- Verify all foreign key relationships are valid
    SELECT COUNT(*) INTO integrity_errors
    FROM accounts a
    WHERE NOT EXISTS (SELECT 1 FROM customers c WHERE c.customer_id = a.customer_id)
       OR NOT EXISTS (SELECT 1 FROM disclosure_groups dg WHERE dg.group_id = a.group_id);
    
    IF integrity_errors > 0 THEN
        error_details := error_details || 'Foreign key integrity violations: ' || integrity_errors || ' records; ';
    END IF;
    
    -- Verify financial field precision
    SELECT COUNT(*) INTO integrity_errors
    FROM accounts a
    WHERE SCALE(a.current_balance) > 2 
       OR SCALE(a.credit_limit) > 2
       OR SCALE(a.cash_credit_limit) > 2
       OR SCALE(a.current_cycle_credit) > 2
       OR SCALE(a.current_cycle_debit) > 2;
    
    IF integrity_errors > 0 THEN
        error_details := error_details || 'Decimal precision violations: ' || integrity_errors || ' records; ';
    END IF;
    
    -- Verify date relationships
    SELECT COUNT(*) INTO integrity_errors
    FROM accounts a
    WHERE a.expiration_date <= a.open_date
       OR a.reissue_date < a.open_date;
    
    IF integrity_errors > 0 THEN
        error_details := error_details || 'Date relationship violations: ' || integrity_errors || ' records; ';
    END IF;
    
    -- Log integrity verification results
    IF LENGTH(error_details) > 0 THEN
        RAISE EXCEPTION 'Data integrity verification failed: %', error_details;
    ELSE
        RAISE NOTICE 'Data integrity verification passed for % account records', total_loaded;
    END IF;
END $$;

-- =============================================================================
-- PHASE 9: Loading Summary and Audit Trail
-- =============================================================================

DO $$ BEGIN RAISE NOTICE 'Generating loading summary and audit trail...'; END $$;

-- Display comprehensive loading summary
DO $$
DECLARE
    stats_record RECORD;
    error_summary TEXT DEFAULT '';
    duration_seconds NUMERIC;
BEGIN
    SELECT * INTO stats_record FROM temp_load_statistics;
    
    duration_seconds := EXTRACT(EPOCH FROM (stats_record.end_time - stats_record.start_time));
    
    -- Generate error summary for invalid records
    SELECT string_agg(
        'Line ' || line_number || ': ' || validation_errors, 
        E'\n'
    ) INTO error_summary
    FROM temp_accounts_parsed 
    WHERE is_valid = FALSE
    LIMIT 10;  -- Limit to first 10 errors for readability
    
    -- Display loading summary
    RAISE NOTICE 'Account Data Loading Summary:';
    RAISE NOTICE '================================';
    RAISE NOTICE 'Total records processed: %', stats_record.total_records;
    RAISE NOTICE 'Valid records: %', stats_record.valid_records;
    RAISE NOTICE 'Invalid records: %', stats_record.invalid_records;
    RAISE NOTICE 'Successfully loaded: %', stats_record.loaded_records;
    RAISE NOTICE 'Processing time: % seconds', ROUND(duration_seconds, 2);
    RAISE NOTICE 'Loading rate: % records/second', 
        CASE WHEN duration_seconds > 0 THEN ROUND(stats_record.loaded_records / duration_seconds, 1) ELSE 0 END;
    
    IF stats_record.invalid_records > 0 THEN
        RAISE NOTICE 'Validation Errors (first 10):';
        RAISE NOTICE '%', COALESCE(error_summary, 'No detailed errors available');
    END IF;
    
    -- Verify expected record count matches loaded count
    IF stats_record.loaded_records != stats_record.valid_records THEN
        RAISE EXCEPTION 'Loading verification failed: Expected % records but loaded %', 
            stats_record.valid_records, stats_record.loaded_records;
    END IF;
    
    RAISE NOTICE 'Account data loading completed successfully!';
END $$;

-- Create permanent audit record for the loading operation
INSERT INTO migration_audit_log (
    migration_version,
    migration_name,
    table_name,
    operation_type,
    records_processed,
    records_loaded,
    records_failed,
    start_time,
    end_time,
    duration_seconds,
    success_status,
    notes
) 
SELECT 
    'V22',
    'load_accounts_data',
    'accounts',
    'DATA_LOAD',
    total_records,
    loaded_records,
    invalid_records,
    start_time,
    end_time,
    EXTRACT(EPOCH FROM (end_time - start_time)),
    CASE WHEN loaded_records = valid_records THEN TRUE ELSE FALSE END,
    'Loaded account data from acctdata.txt with DECIMAL(12,2) precision and foreign key validation'
FROM temp_load_statistics;

-- =============================================================================
-- PHASE 10: Cleanup and Final Verification
-- =============================================================================

-- Clean up temporary tables
DROP TABLE IF EXISTS temp_acctdata_raw;
DROP TABLE IF EXISTS temp_accounts_parsed;
DROP TABLE IF EXISTS temp_load_statistics;

-- Final verification: Ensure all loaded accounts have valid relationships
DO $$
DECLARE
    account_count INTEGER;
    relationship_errors INTEGER;
BEGIN
    SELECT COUNT(*) INTO account_count FROM accounts;
    
    -- Verify customer relationships
    SELECT COUNT(*) INTO relationship_errors
    FROM accounts a
    LEFT JOIN customers c ON a.customer_id = c.customer_id
    WHERE c.customer_id IS NULL;
    
    IF relationship_errors > 0 THEN
        RAISE EXCEPTION 'Final verification failed: % accounts have invalid customer references', relationship_errors;
    END IF;
    
    -- Verify disclosure group relationships  
    SELECT COUNT(*) INTO relationship_errors
    FROM accounts a
    LEFT JOIN disclosure_groups dg ON a.group_id = dg.group_id
    WHERE dg.group_id IS NULL;
    
    IF relationship_errors > 0 THEN
        RAISE EXCEPTION 'Final verification failed: % accounts have invalid disclosure group references', relationship_errors;
    END IF;
    
    RAISE NOTICE 'Final verification passed: % accounts loaded with valid relationships', account_count;
END $$;

-- Rollback directives have been moved to the XML changeset definition

DO $$ BEGIN RAISE NOTICE 'Account data loading migration V22 completed successfully!'; END $$;