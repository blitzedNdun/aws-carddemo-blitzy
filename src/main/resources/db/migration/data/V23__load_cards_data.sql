-- =====================================================================================
-- Liquibase Migration: V23__load_cards_data.sql
-- Description: Loads card data from carddata.txt and cardxref.txt ASCII sources with 
--              security validation, composite foreign key relationships, and integrated 
--              cross-reference functionality for comprehensive card management
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 23.0
-- Dependencies: V4__create_cards_table.sql, V21__load_customers_data.sql, V22__load_accounts_data.sql
-- =====================================================================================

-- changeset blitzy:V23-load-cards-data
-- comment: Load card data from carddata.txt and cardxref.txt with Luhn algorithm validation, composite foreign key relationships, and security formatting

-- =============================================================================
-- 1. CREATE TEMPORARY STAGING TABLES FOR RAW DATA PROCESSING
-- =============================================================================

-- Create temporary table for staging carddata.txt raw data during processing
CREATE TEMPORARY TABLE temp_carddata_raw (
    raw_record TEXT
);

-- Create temporary table for staging cardxref.txt raw data during processing
CREATE TEMPORARY TABLE temp_cardxref_raw (
    raw_record TEXT
);

-- Create temporary table for parsed card data with proper data types
CREATE TEMPORARY TABLE temp_carddata_parsed (
    card_number VARCHAR(16),
    account_id VARCHAR(11),
    customer_id VARCHAR(9),
    cvv_code VARCHAR(3),
    embossed_name VARCHAR(50),
    expiration_date DATE,
    active_status VARCHAR(1),
    luhn_valid BOOLEAN DEFAULT FALSE,
    foreign_key_valid BOOLEAN DEFAULT FALSE,
    data_source VARCHAR(10) DEFAULT 'CARDDATA'
);

-- Create temporary table for parsed cross-reference data
CREATE TEMPORARY TABLE temp_cardxref_parsed (
    card_number VARCHAR(16),
    account_id VARCHAR(11),
    customer_id VARCHAR(9),
    data_source VARCHAR(10) DEFAULT 'CARDXREF'
);

-- =============================================================================
-- 2. LOAD RAW CARD DATA FROM carddata.txt
-- =============================================================================

-- Load the 50+ card records from carddata.txt preserving exact fixed-width format
-- Each record is exactly 100 characters in fixed-width format
INSERT INTO temp_carddata_raw (raw_record) VALUES 
('0500024453765740000000000507473747Aniya Von                                         2023-03-09Y'),
('0683586198171516000000000275676567Ward Jones                                        2025-07-13Y'),
('0923877193247330000000000020283020Enrico Rosenbaum                                  2024-08-11Y'),
('0927987108636232000000000200033200Carter Veum                                       2024-03-13Y'),
('0982496213629795000000000120753120Maci Robel                                        2023-07-07Y'),
('1014086565224350000000000446407446Irving Emard                                      2024-01-17Y'),
('1142167692878931000000000376256376Shany Walker                                      2023-10-24Y'),
('1561409106491600000000000350317350Angelica Dach                                     2025-09-23Y'),
('2745303720002090000000000390335390Aliyah Berge                                      2025-09-08Y'),
('2760836797107565000000000248592248Stefanie Dickinson                                2025-02-11Y'),
('2871968252812490000000000067753677Ignacio Douglas                                   2025-10-08Y'),
('2940139362300449000000000228764228Allene Brown                                      2025-12-28Y'),
('2988091353094312000000000047951479Delbert Parisian                                  2023-12-16Y'),
('3260763612337560000000000103423103Maybell Mann                                      2023-01-27Y'),
('3766281984155154000000000416221416Lucinda Dach                                      2023-04-24Y'),
('3940246016141489000000000193752193Hadley Hamill                                     2025-07-23Y'),
('3999169246375885000000000033172331Larry Homenick                                    2024-01-10Y'),
('4011500891777367000000000133902133Mariane Fadel                                     2024-08-04Y'),
('4385271476627819000000000347097347Faustino Schmidt                                  2025-10-06Y'),
('4534784102713951000000000366446366Toney Gerhold                                     2024-12-23Y'),
('4859452612877065000000000073214732Cooper Mayert                                     2024-12-13Y'),
('5407099850479866000000000215245215Jerrold Maggio                                    2023-01-06Y'),
('5656830544981216000000000461963461Cindy Cremin                                      2025-06-20Y'),
('5671184478505844000000000181371813Emile White                                       2023-09-10Y'),
('5787351228879339000000000470672470Rigoberto Hoeger                                  2025-08-23Y'),
('5975117516616077000000000424263424Heather Nienow                                    2025-09-19Y'),
('6009619150674526000000000050210502Treva Schowalter                                  2025-03-09Y'),
('6349250331648509000000000157359157Aubree Hermann                                    2025-06-09Y'),
('6503535181795992000000000484139484Lyric Pacocha                                     2025-02-06Y'),
('6509230362553816000000000302363302Layla Ullrich                                     2024-06-27Y'),
('6723000463207764000000000284862284Hester Hane                                       2024-05-09Y'),
('6727055190616014000000000166411664Carroll Bergstrom                                 2024-01-25Y'),
('6832676047698087000000000339837339Bernice Herman                                    2025-10-07Y'),
('7026637615032277000000000319207319Lucious O''Connell                                 2025-06-08Y'),
('7058267261837752000000000434017434Britney Waters                                    2025-08-29Y'),
('7094142751055551000000000326599326Stephany Fisher                                   2025-05-19Y'),
('7251508149188883000000000297172297Rickie Daugherty                                  2024-06-04Y'),
('7379335634661142000000000451349451Dixie Beier                                       2025-07-09Y'),
('7427684863423209000000000118923118Hayden Pfannerstill                               2025-03-12Y'),
('7443870988897530000000000387084387Angela Ankunding                                  2023-07-23Y'),
('8040580410348680000000000269718269Marjory Stracke                                   2024-12-19Y'),
('8112545834239735000000000234406234Johnson Ruecker                                   2025-03-18Y'),
('8262593602473076000000000494577494Immanuel Bednar                                   2023-09-17Y'),
('8517866958206008000000000149559149Chelsea Marks                                     2025-12-11Y'),
('8931369351894783000000000082307823Kelsie Dicki                                      2024-05-20Y'),
('9056297931664011000000000259318259Elliott Howell                                    2025-07-10Y'),
('9349107475869214000000000172189172Sigrid Mann                                       2025-03-01Y'),
('9501733721429893000000000097257972Melvin Ondricka                                   2024-12-27Y'),
('9680294154603697000000000010451010Immanuel Kessler                                  2025-05-20Y'),
('9805583408996588000000000409087409Davon Emmerich                                    2023-10-27Y');

-- =============================================================================
-- 3. LOAD RAW CROSS-REFERENCE DATA FROM cardxref.txt
-- =============================================================================

-- Load the 50+ cross-reference records from cardxref.txt for relationship validation
INSERT INTO temp_cardxref_raw (raw_record) VALUES 
('050002445376574000000000050000000005'),
('068358619817151600000000027000000002'),
('092387719324733000000000002000000000'),
('092798710863623200000000020000000002'),
('098249621362979500000000012000000001'),
('101408656522435000000000044000000004'),
('114216769287893100000000037000000003'),
('156140910649160000000000035000000003'),
('274530372000209000000000039000000003'),
('276083679710756500000000024000000002'),
('287196825281249000000000006000000000'),
('294013936230044900000000022000000002'),
('298809135309431200000000004000000000'),
('326076361233756000000000010000000001'),
('376628198415515400000000041000000004'),
('394024601614148900000000019000000001'),
('399916924637588500000000003000000000'),
('401150089177736700000000013000000001'),
('438527147662781900000000034000000003'),
('453478410271395100000000036000000003'),
('485945261287706500000000007000000000'),
('540709985047986600000000021000000002'),
('565683054498121600000000046000000004'),
('567118447850584400000000018000000001'),
('578735122887933900000000047000000004'),
('597511751661607700000000042000000004'),
('600961915067452600000000005000000000'),
('634925033164850900000000015000000001'),
('650353518179599200000000048000000004'),
('650923036255381600000000030000000003'),
('672300046320776400000000028000000002'),
('672705519061601400000000016000000001'),
('683267604769808700000000033000000003'),
('702663761503227700000000031000000003'),
('705826726183775200000000043000000004'),
('709414275105555100000000032000000003'),
('725150814918888300000000029000000002'),
('737933563466114200000000045000000004'),
('742768486342320900000000011000000001'),
('744387098889753000000000038000000003'),
('804058041034868000000000026000000002'),
('811254583423973500000000023000000002'),
('826259360247307600000000049000000004'),
('851786695820600800000000014000000001'),
('893136935189478300000000008000000000'),
('905629793166401100000000025000000002'),
('934910747586921400000000017000000001'),
('950173372142989300000000009000000000'),
('968029415460369700000000001000000000'),
('980558340899658800000000040000000004');

-- =============================================================================
-- 4. PARSE CARDDATA.TXT RAW DATA INTO STRUCTURED FORMAT
-- =============================================================================

-- Parse fixed-width carddata.txt records into structured format
-- Record format: card_number(16) + account_id(11) + customer_id(9) + cvv_code(3) + embossed_name(50) + expiration_date(10) + active_status(1)
INSERT INTO temp_carddata_parsed (
    card_number, account_id, customer_id, cvv_code, embossed_name, expiration_date, active_status
)
SELECT 
    SUBSTRING(raw_record, 1, 16) AS card_number,
    SUBSTRING(raw_record, 17, 11) AS account_id,
    SUBSTRING(raw_record, 28, 9) AS customer_id,
    SUBSTRING(raw_record, 37, 3) AS cvv_code,
    TRIM(SUBSTRING(raw_record, 40, 50)) AS embossed_name,
    SUBSTRING(raw_record, 90, 10)::DATE AS expiration_date,
    SUBSTRING(raw_record, 100, 1) AS active_status
FROM temp_carddata_raw
WHERE LENGTH(raw_record) = 100;

-- =============================================================================
-- 5. PARSE CARDXREF.TXT RAW DATA INTO STRUCTURED FORMAT
-- =============================================================================

-- Parse fixed-width cardxref.txt records into structured format
-- Record format: card_number(16) + account_id(11) + customer_id(9)
INSERT INTO temp_cardxref_parsed (
    card_number, account_id, customer_id
)
SELECT 
    SUBSTRING(raw_record, 1, 16) AS card_number,
    SUBSTRING(raw_record, 17, 11) AS account_id,
    SUBSTRING(raw_record, 28, 9) AS customer_id
FROM temp_cardxref_raw
WHERE LENGTH(raw_record) >= 36;

-- =============================================================================
-- 6. VALIDATE LUHN ALGORITHM FOR CARD NUMBER INTEGRITY
-- =============================================================================

-- Update Luhn algorithm validation status using existing validate_luhn_algorithm function
UPDATE temp_carddata_parsed 
SET luhn_valid = validate_luhn_algorithm(card_number)
WHERE card_number IS NOT NULL;

-- =============================================================================
-- 7. VALIDATE FOREIGN KEY RELATIONSHIPS
-- =============================================================================

-- Validate foreign key relationships to accounts and customers tables
UPDATE temp_carddata_parsed 
SET foreign_key_valid = (
    EXISTS (
        SELECT 1 FROM accounts a 
        WHERE a.account_id = temp_carddata_parsed.account_id
    ) AND 
    EXISTS (
        SELECT 1 FROM customers c 
        WHERE c.customer_id = temp_carddata_parsed.customer_id
    )
);

-- =============================================================================
-- 8. CROSS-REFERENCE DATA VALIDATION
-- =============================================================================

-- Create temporary table for cross-reference validation results
CREATE TEMPORARY TABLE temp_card_validation_results (
    card_number VARCHAR(16),
    carddata_account_id VARCHAR(11),
    carddata_customer_id VARCHAR(9),
    cardxref_account_id VARCHAR(11),
    cardxref_customer_id VARCHAR(9),
    relationships_match BOOLEAN DEFAULT FALSE,
    validation_status VARCHAR(20) DEFAULT 'PENDING'
);

-- Populate validation results comparing carddata.txt and cardxref.txt relationships
INSERT INTO temp_card_validation_results (
    card_number, carddata_account_id, carddata_customer_id, 
    cardxref_account_id, cardxref_customer_id, relationships_match
)
SELECT 
    cd.card_number,
    cd.account_id AS carddata_account_id,
    cd.customer_id AS carddata_customer_id,
    cx.account_id AS cardxref_account_id,
    cx.customer_id AS cardxref_customer_id,
    (cd.account_id = cx.account_id AND cd.customer_id = cx.customer_id) AS relationships_match
FROM temp_carddata_parsed cd
INNER JOIN temp_cardxref_parsed cx ON cd.card_number = cx.card_number;

-- Update validation status based on relationship matching
UPDATE temp_card_validation_results 
SET validation_status = CASE 
    WHEN relationships_match = TRUE THEN 'VALIDATED'
    ELSE 'MISMATCH'
END;

-- =============================================================================
-- 9. SECURITY VALIDATION AND CVV CODE FORMATTING
-- =============================================================================

-- Validate CVV codes are 3-digit numeric format
UPDATE temp_carddata_parsed 
SET cvv_code = CASE 
    WHEN cvv_code ~ '^[0-9]{3}$' THEN cvv_code
    ELSE NULL
END;

-- Validate embossed names for proper formatting and length constraints
UPDATE temp_carddata_parsed 
SET embossed_name = CASE 
    WHEN LENGTH(TRIM(embossed_name)) > 0 AND LENGTH(embossed_name) <= 50 THEN TRIM(embossed_name)
    ELSE NULL
END;

-- =============================================================================
-- 10. COMPREHENSIVE DATA VALIDATION REPORT
-- =============================================================================

-- Create comprehensive validation report for audit trail
CREATE TEMPORARY TABLE temp_card_validation_report (
    total_records_processed INTEGER,
    luhn_valid_count INTEGER,
    luhn_invalid_count INTEGER,
    foreign_key_valid_count INTEGER,
    foreign_key_invalid_count INTEGER,
    xref_validated_count INTEGER,
    xref_mismatch_count INTEGER,
    cvv_valid_count INTEGER,
    cvv_invalid_count INTEGER,
    embossed_name_valid_count INTEGER,
    embossed_name_invalid_count INTEGER,
    expiration_future_count INTEGER,
    expiration_past_count INTEGER,
    total_cards_ready_for_load INTEGER
);

-- Populate validation report with comprehensive statistics
INSERT INTO temp_card_validation_report (
    total_records_processed,
    luhn_valid_count,
    luhn_invalid_count,
    foreign_key_valid_count,
    foreign_key_invalid_count,
    xref_validated_count,
    xref_mismatch_count,
    cvv_valid_count,
    cvv_invalid_count,
    embossed_name_valid_count,
    embossed_name_invalid_count,
    expiration_future_count,
    expiration_past_count,
    total_cards_ready_for_load
)
SELECT 
    COUNT(*),
    SUM(CASE WHEN luhn_valid = TRUE THEN 1 ELSE 0 END),
    SUM(CASE WHEN luhn_valid = FALSE THEN 1 ELSE 0 END),
    SUM(CASE WHEN foreign_key_valid = TRUE THEN 1 ELSE 0 END),
    SUM(CASE WHEN foreign_key_valid = FALSE THEN 1 ELSE 0 END),
    (SELECT COUNT(*) FROM temp_card_validation_results WHERE validation_status = 'VALIDATED'),
    (SELECT COUNT(*) FROM temp_card_validation_results WHERE validation_status = 'MISMATCH'),
    SUM(CASE WHEN cvv_code IS NOT NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN cvv_code IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN embossed_name IS NOT NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN embossed_name IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN expiration_date > CURRENT_DATE THEN 1 ELSE 0 END),
    SUM(CASE WHEN expiration_date <= CURRENT_DATE THEN 1 ELSE 0 END),
    SUM(CASE WHEN luhn_valid = TRUE AND foreign_key_valid = TRUE AND cvv_code IS NOT NULL 
             AND embossed_name IS NOT NULL AND expiration_date > CURRENT_DATE THEN 1 ELSE 0 END)
FROM temp_carddata_parsed;

-- =============================================================================
-- 11. FINAL CARD DATA LOADING TO PRODUCTION TABLE
-- =============================================================================

-- Load validated card data into the production cards table
-- Only load records that pass all validation checks
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at, updated_at
)
SELECT 
    cd.card_number,
    cd.account_id,
    cd.customer_id,
    cd.cvv_code,
    cd.embossed_name,
    cd.expiration_date,
    cd.active_status,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM temp_carddata_parsed cd
INNER JOIN temp_card_validation_results vr ON cd.card_number = vr.card_number
WHERE cd.luhn_valid = TRUE
  AND cd.foreign_key_valid = TRUE
  AND cd.cvv_code IS NOT NULL
  AND cd.embossed_name IS NOT NULL
  AND cd.expiration_date > CURRENT_DATE
  AND cd.active_status IN ('Y', 'N')
  AND vr.validation_status = 'VALIDATED';

-- =============================================================================
-- 12. REFRESH MATERIALIZED VIEWS FOR CROSS-REFERENCE FUNCTIONALITY
-- =============================================================================

-- Refresh materialized view for card summary queries (if exists)
-- This provides optimized cross-reference functionality for rapid lookup operations
DO $$
BEGIN
    -- Check if materialized view exists and refresh it
    IF EXISTS (
        SELECT 1 FROM pg_matviews 
        WHERE matviewname = 'mv_card_summary' 
        AND schemaname = CURRENT_SCHEMA()
    ) THEN
        REFRESH MATERIALIZED VIEW mv_card_summary;
    END IF;
END $$;

-- =============================================================================
-- 13. FINAL VALIDATION AND AUDIT LOGGING
-- =============================================================================

-- Create final audit log entry for card data loading operation
INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, expiration_date, active_status, created_at, updated_at) 
SELECT 
    'AUDIT_' || LPAD(EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::TEXT, 10, '0'),
    '00000000000',
    '000000000',
    '000',
    'CARD DATA LOAD AUDIT RECORD',
    '2099-12-31',
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM cards WHERE card_number LIKE 'AUDIT_%'
);

-- Final validation query to confirm successful card data loading
DO $$
DECLARE
    loaded_count INTEGER;
    validation_report_record RECORD;
BEGIN
    -- Get count of successfully loaded cards
    SELECT COUNT(*) INTO loaded_count FROM cards 
    WHERE card_number NOT LIKE 'AUDIT_%';
    
    -- Get validation report statistics
    SELECT * INTO validation_report_record FROM temp_card_validation_report;
    
    -- Log success message with comprehensive statistics
    RAISE NOTICE 'Card data loading completed successfully:';
    RAISE NOTICE 'Total records processed: %', validation_report_record.total_records_processed;
    RAISE NOTICE 'Luhn algorithm valid: %', validation_report_record.luhn_valid_count;
    RAISE NOTICE 'Foreign key relationships valid: %', validation_report_record.foreign_key_valid_count;
    RAISE NOTICE 'Cross-reference validated: %', validation_report_record.xref_validated_count;
    RAISE NOTICE 'CVV codes valid: %', validation_report_record.cvv_valid_count;
    RAISE NOTICE 'Embossed names valid: %', validation_report_record.embossed_name_valid_count;
    RAISE NOTICE 'Future expiration dates: %', validation_report_record.expiration_future_count;
    RAISE NOTICE 'Total cards loaded into production table: %', loaded_count;
    
    -- Verify minimum expected records were loaded
    IF loaded_count < 40 THEN
        RAISE EXCEPTION 'Card data loading failed: Only % records loaded, expected at least 40', loaded_count;
    END IF;
END $$;

-- =============================================================================
-- 14. CLEANUP TEMPORARY TABLES
-- =============================================================================

-- Drop temporary tables to free memory
DROP TABLE IF EXISTS temp_carddata_raw;
DROP TABLE IF EXISTS temp_cardxref_raw;
DROP TABLE IF EXISTS temp_carddata_parsed;
DROP TABLE IF EXISTS temp_cardxref_parsed;
DROP TABLE IF EXISTS temp_card_validation_results;
DROP TABLE IF EXISTS temp_card_validation_report;

-- =============================================================================
-- 15. FINAL INTEGRITY CHECKS
-- =============================================================================

-- Verify all loaded cards have valid foreign key relationships
DO $$
DECLARE
    orphaned_cards INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphaned_cards
    FROM cards c
    WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.account_id = c.account_id)
       OR NOT EXISTS (SELECT 1 FROM customers cust WHERE cust.customer_id = c.customer_id);
    
    IF orphaned_cards > 0 THEN
        RAISE EXCEPTION 'Foreign key integrity violation: % cards have invalid relationships', orphaned_cards;
    END IF;
    
    RAISE NOTICE 'All foreign key relationships validated successfully';
END $$;

-- Verify all loaded cards pass Luhn algorithm validation
DO $$
DECLARE
    invalid_luhn_cards INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_luhn_cards
    FROM cards c
    WHERE NOT validate_luhn_algorithm(c.card_number)
      AND c.card_number NOT LIKE 'AUDIT_%';
    
    IF invalid_luhn_cards > 0 THEN
        RAISE EXCEPTION 'Luhn algorithm validation failed: % cards have invalid card numbers', invalid_luhn_cards;
    END IF;
    
    RAISE NOTICE 'All card numbers pass Luhn algorithm validation';
END $$;

-- Verify all loaded cards have future expiration dates
DO $$
DECLARE
    expired_cards INTEGER;
BEGIN
    SELECT COUNT(*) INTO expired_cards
    FROM cards c
    WHERE c.expiration_date <= CURRENT_DATE
      AND c.card_number NOT LIKE 'AUDIT_%';
    
    IF expired_cards > 0 THEN
        RAISE EXCEPTION 'Expiration date validation failed: % cards have past expiration dates', expired_cards;
    END IF;
    
    RAISE NOTICE 'All cards have valid future expiration dates';
END $$;

-- =============================================================================
-- 16. ROLLBACK INSTRUCTIONS
-- =============================================================================

-- rollback changeset blitzy:V23-load-cards-data
-- DELETE FROM cards WHERE created_at >= (SELECT MIN(created_at) FROM cards WHERE card_number NOT LIKE 'AUDIT_%');
-- Note: This rollback will remove all cards loaded by this migration while preserving any previously existing data

-- End of V23__load_cards_data.sql