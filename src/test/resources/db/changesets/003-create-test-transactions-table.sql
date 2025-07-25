--liquibase formatted sql

--changeset blitzy-agent:003-create-test-transactions-table splitStatements:false rollbackSplitStatements:false
--comment: Create PostgreSQL transactions table for test environment based on CVTRA05Y.cpy TRAN-RECORD structure with monthly RANGE partitioning
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'transactions' AND table_schema = current_schema()

-- ================================================================
-- CardDemo Test Transactions Table Creation
-- 
-- Purpose: Create PostgreSQL transactions table for test environment with
--          exact COBOL TRAN-RECORD structure mapping and monthly RANGE partitioning
--          to support high-volume transaction processing integration tests
--
-- Source Mapping: COBOL copybook app/cpy/CVTRA05Y.cpy TRAN-RECORD structure
--   TRAN-ID                    PIC X(16) → transaction_id VARCHAR(16) PRIMARY KEY
--   TRAN-TYPE-CD               PIC X(02) → transaction_type VARCHAR(2) FOREIGN KEY
--   TRAN-CAT-CD                PIC 9(04) → transaction_category INTEGER FOREIGN KEY
--   TRAN-SOURCE                PIC X(10) → transaction_source VARCHAR(10) NOT NULL
--   TRAN-DESC                  PIC X(100) → transaction_description VARCHAR(100) NOT NULL
--   TRAN-AMT                   PIC S9(09)V99 → transaction_amount DECIMAL(11,2) NOT NULL
--   TRAN-MERCHANT-ID           PIC 9(09) → merchant_id VARCHAR(9) NULL
--   TRAN-MERCHANT-NAME         PIC X(50) → merchant_name VARCHAR(50) NULL
--   TRAN-MERCHANT-CITY         PIC X(50) → merchant_city VARCHAR(50) NULL
--   TRAN-MERCHANT-ZIP          PIC X(10) → merchant_zip VARCHAR(10) NULL
--   TRAN-CARD-NUM              PIC X(16) → card_number VARCHAR(16) FOREIGN KEY
--   TRAN-ORIG-TS               PIC X(26) → transaction_timestamp TIMESTAMP WITH TIME ZONE
--   TRAN-PROC-TS               PIC X(26) → processing_timestamp TIMESTAMP WITH TIME ZONE
--   FILLER                     PIC X(20) → Not mapped (legacy padding)
--
-- Additional fields for modern integration testing and audit requirements:
--   account_id VARCHAR(11) - Derived from card_number for efficient querying
--   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
--   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
--
-- Test Environment Specifications:
-- - Monthly RANGE partitioning on transaction_timestamp as per Database Design 6.2.1.4
-- - DECIMAL(11,2) precision for transaction amounts (exact COBOL COMP-3 equivalence)
-- - Foreign key constraints to cards and trancatg tables for relational integrity testing
-- - Enhanced constraints for comprehensive transaction processing validation
-- - Support for BigDecimal precision validation in Spring Boot microservices
-- - Partition pruning optimization for date-range queries in batch processing tests
-- ================================================================

-- Create the master transactions table with partitioning
CREATE TABLE transactions (
    -- Primary key: Transaction ID mapping from COBOL TRAN-ID
    transaction_id VARCHAR(16) NOT NULL,
    
    -- Foreign key relationships for test scenario support
    card_number VARCHAR(16) NOT NULL, -- References cards table
    account_id VARCHAR(11) NULL,      -- Derived from card for efficient queries
    
    -- Transaction classification fields referencing lookup tables
    transaction_type VARCHAR(2) NOT NULL,     -- References trantype table
    transaction_category INTEGER NOT NULL,    -- References trancatg table (composite FK)
    
    -- Core transaction data from COBOL structure
    transaction_source VARCHAR(10) NOT NULL,
    transaction_description VARCHAR(100) NOT NULL,
    transaction_amount DECIMAL(11,2) NOT NULL, -- Exact COBOL S9(09)V99 precision
    
    -- Merchant information for transaction context
    merchant_id VARCHAR(9) NULL,
    merchant_name VARCHAR(50) NULL,
    merchant_city VARCHAR(50) NULL,
    merchant_zip VARCHAR(10) NULL,
    
    -- Timestamp fields for transaction lifecycle tracking
    transaction_timestamp TIMESTAMP WITH TIME ZONE NOT NULL, -- Partitioning key
    processing_timestamp TIMESTAMP WITH TIME ZONE NULL,
    
    -- Audit and tracking fields for integration testing
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id, transaction_timestamp),
    
    -- Foreign key constraint for card relationship testing
    CONSTRAINT fk_transactions_card FOREIGN KEY (card_number) REFERENCES cards (card_number),
    
    -- Composite foreign key constraint for transaction classification
    CONSTRAINT fk_transactions_category FOREIGN KEY (transaction_type, transaction_category) 
        REFERENCES trancatg (transaction_type_code, transaction_category_code),
    
    -- Business rule constraints for comprehensive test data validation
    CONSTRAINT chk_transaction_id_format CHECK (transaction_id ~ '^[A-Z0-9]{16}$'), -- 16 alphanumeric characters
    CONSTRAINT chk_card_number_format CHECK (card_number ~ '^[0-9]{16}$'), -- 16 numeric digits
    CONSTRAINT chk_account_id_format CHECK (account_id IS NULL OR account_id ~ '^[0-9]{11}$'), -- 11 numeric digits
    CONSTRAINT chk_transaction_type_format CHECK (transaction_type ~ '^[A-Z0-9]{2}$'), -- 2 alphanumeric characters
    CONSTRAINT chk_transaction_category_range CHECK (transaction_category >= 0 AND transaction_category <= 9999), -- 4-digit range
    CONSTRAINT chk_transaction_source_not_empty CHECK (TRIM(transaction_source) <> ''),
    CONSTRAINT chk_transaction_description_not_empty CHECK (TRIM(transaction_description) <> ''),
    CONSTRAINT chk_transaction_amount_precision CHECK (transaction_amount = ROUND(transaction_amount, 2)),
    CONSTRAINT chk_merchant_id_format CHECK (merchant_id IS NULL OR merchant_id ~ '^[0-9]{9}$'), -- 9 numeric digits
    CONSTRAINT chk_merchant_name_length CHECK (merchant_name IS NULL OR LENGTH(TRIM(merchant_name)) >= 1),
    CONSTRAINT chk_merchant_city_length CHECK (merchant_city IS NULL OR LENGTH(TRIM(merchant_city)) >= 1),
    CONSTRAINT chk_merchant_zip_format CHECK (merchant_zip IS NULL OR merchant_zip ~ '^[0-9]{5}(-[0-9]{4})?$'), -- US ZIP format
    CONSTRAINT chk_processing_after_transaction CHECK (processing_timestamp IS NULL OR processing_timestamp >= transaction_timestamp)
) PARTITION BY RANGE (transaction_timestamp);

-- Create monthly partitions for the next 24 months to support test scenarios
-- Current month partition
CREATE TABLE transactions_2024_01 PARTITION OF transactions
    FOR VALUES FROM ('2024-01-01 00:00:00+00') TO ('2024-02-01 00:00:00+00');

CREATE TABLE transactions_2024_02 PARTITION OF transactions
    FOR VALUES FROM ('2024-02-01 00:00:00+00') TO ('2024-03-01 00:00:00+00');

CREATE TABLE transactions_2024_03 PARTITION OF transactions
    FOR VALUES FROM ('2024-03-01 00:00:00+00') TO ('2024-04-01 00:00:00+00');

CREATE TABLE transactions_2024_04 PARTITION OF transactions
    FOR VALUES FROM ('2024-04-01 00:00:00+00') TO ('2024-05-01 00:00:00+00');

CREATE TABLE transactions_2024_05 PARTITION OF transactions
    FOR VALUES FROM ('2024-05-01 00:00:00+00') TO ('2024-06-01 00:00:00+00');

CREATE TABLE transactions_2024_06 PARTITION OF transactions
    FOR VALUES FROM ('2024-06-01 00:00:00+00') TO ('2024-07-01 00:00:00+00');

CREATE TABLE transactions_2024_07 PARTITION OF transactions
    FOR VALUES FROM ('2024-07-01 00:00:00+00') TO ('2024-08-01 00:00:00+00');

CREATE TABLE transactions_2024_08 PARTITION OF transactions
    FOR VALUES FROM ('2024-08-01 00:00:00+00') TO ('2024-09-01 00:00:00+00');

CREATE TABLE transactions_2024_09 PARTITION OF transactions
    FOR VALUES FROM ('2024-09-01 00:00:00+00') TO ('2024-10-01 00:00:00+00');

CREATE TABLE transactions_2024_10 PARTITION OF transactions
    FOR VALUES FROM ('2024-10-01 00:00:00+00') TO ('2024-11-01 00:00:00+00');

CREATE TABLE transactions_2024_11 PARTITION OF transactions
    FOR VALUES FROM ('2024-11-01 00:00:00+00') TO ('2024-12-01 00:00:00+00');

CREATE TABLE transactions_2024_12 PARTITION OF transactions
    FOR VALUES FROM ('2024-12-01 00:00:00+00') TO ('2025-01-01 00:00:00+00');

-- Create partitions for 2025 to support forward-looking test scenarios
CREATE TABLE transactions_2025_01 PARTITION OF transactions
    FOR VALUES FROM ('2025-01-01 00:00:00+00') TO ('2025-02-01 00:00:00+00');

CREATE TABLE transactions_2025_02 PARTITION OF transactions
    FOR VALUES FROM ('2025-02-01 00:00:00+00') TO ('2025-03-01 00:00:00+00');

CREATE TABLE transactions_2025_03 PARTITION OF transactions
    FOR VALUES FROM ('2025-03-01 00:00:00+00') TO ('2025-04-01 00:00:00+00');

CREATE TABLE transactions_2025_04 PARTITION OF transactions
    FOR VALUES FROM ('2025-04-01 00:00:00+00') TO ('2025-05-01 00:00:00+00');

CREATE TABLE transactions_2025_05 PARTITION OF transactions
    FOR VALUES FROM ('2025-05-01 00:00:00+00') TO ('2025-06-01 00:00:00+00');

CREATE TABLE transactions_2025_06 PARTITION OF transactions
    FOR VALUES FROM ('2025-06-01 00:00:00+00') TO ('2025-07-01 00:00:00+00');

CREATE TABLE transactions_2025_07 PARTITION OF transactions
    FOR VALUES FROM ('2025-07-01 00:00:00+00') TO ('2025-08-01 00:00:00+00');

CREATE TABLE transactions_2025_08 PARTITION OF transactions
    FOR VALUES FROM ('2025-08-01 00:00:00+00') TO ('2025-09-01 00:00:00+00');

CREATE TABLE transactions_2025_09 PARTITION OF transactions
    FOR VALUES FROM ('2025-09-01 00:00:00+00') TO ('2025-10-01 00:00:00+00');

CREATE TABLE transactions_2025_10 PARTITION OF transactions
    FOR VALUES FROM ('2025-10-01 00:00:00+00') TO ('2025-11-01 00:00:00+00');

CREATE TABLE transactions_2025_11 PARTITION OF transactions
    FOR VALUES FROM ('2025-11-01 00:00:00+00') TO ('2025-12-01 00:00:00+00');

CREATE TABLE transactions_2025_12 PARTITION OF transactions
    FOR VALUES FROM ('2025-12-01 00:00:00+00') TO ('2026-01-01 00:00:00+00');

-- Create indexes for performance testing and query optimization on master table
-- Primary key index is created automatically
CREATE INDEX idx_transactions_card_number ON transactions (card_number);
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_transaction_type ON transactions (transaction_type);
CREATE INDEX idx_transactions_transaction_category ON transactions (transaction_category);
CREATE INDEX idx_transactions_transaction_source ON transactions (transaction_source);
CREATE INDEX idx_transactions_transaction_amount ON transactions (transaction_amount);
CREATE INDEX idx_transactions_merchant_id ON transactions (merchant_id);
CREATE INDEX idx_transactions_processing_timestamp ON transactions (processing_timestamp);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_updated_at ON transactions (updated_at);

-- Composite indexes for common transaction processing queries
CREATE INDEX idx_transactions_card_timestamp ON transactions (card_number, transaction_timestamp);
CREATE INDEX idx_transactions_account_timestamp ON transactions (account_id, transaction_timestamp);
CREATE INDEX idx_transactions_type_category ON transactions (transaction_type, transaction_category);
CREATE INDEX idx_transactions_amount_timestamp ON transactions (transaction_amount, transaction_timestamp);
CREATE INDEX idx_transactions_merchant_lookup ON transactions (merchant_id, merchant_name);

-- Date-range query optimization indexes for batch processing
CREATE INDEX idx_transactions_timestamp_range ON transactions (transaction_timestamp, transaction_amount);
CREATE INDEX idx_transactions_processing_range ON transactions (processing_timestamp, transaction_type);

-- Add table comments for test documentation
COMMENT ON TABLE transactions IS 'Test environment transactions table for CardDemo transaction processing service testing. Mapped from COBOL TRAN-RECORD structure in CVTRA05Y.cpy copybook with monthly RANGE partitioning for high-volume integration testing and exact DECIMAL(11,2) precision for financial calculations.';
COMMENT ON COLUMN transactions.transaction_id IS 'Primary key: 16-character transaction identifier from COBOL TRAN-ID field';
COMMENT ON COLUMN transactions.card_number IS 'Foreign key to cards table: 16-digit card number from COBOL TRAN-CARD-NUM field';
COMMENT ON COLUMN transactions.account_id IS 'Derived account identifier for efficient querying, populated from card-account relationship';
COMMENT ON COLUMN transactions.transaction_type IS 'Transaction type code from COBOL TRAN-TYPE-CD, references trantype table';
COMMENT ON COLUMN transactions.transaction_category IS 'Transaction category code from COBOL TRAN-CAT-CD, references trancatg table';
COMMENT ON COLUMN transactions.transaction_source IS 'Transaction source from COBOL TRAN-SOURCE field (ATM, POS, Online, etc.)';
COMMENT ON COLUMN transactions.transaction_description IS 'Transaction description from COBOL TRAN-DESC field';
COMMENT ON COLUMN transactions.transaction_amount IS 'Transaction amount from COBOL TRAN-AMT with DECIMAL(11,2) precision for BigDecimal testing';
COMMENT ON COLUMN transactions.merchant_id IS 'Merchant identifier from COBOL TRAN-MERCHANT-ID field';
COMMENT ON COLUMN transactions.merchant_name IS 'Merchant name from COBOL TRAN-MERCHANT-NAME field';
COMMENT ON COLUMN transactions.merchant_city IS 'Merchant city from COBOL TRAN-MERCHANT-CITY field';
COMMENT ON COLUMN transactions.merchant_zip IS 'Merchant ZIP code from COBOL TRAN-MERCHANT-ZIP field';
COMMENT ON COLUMN transactions.transaction_timestamp IS 'Transaction timestamp from COBOL TRAN-ORIG-TS field - PARTITIONING KEY';
COMMENT ON COLUMN transactions.processing_timestamp IS 'Processing timestamp from COBOL TRAN-PROC-TS field';
COMMENT ON COLUMN transactions.created_at IS 'Transaction creation timestamp for audit trail in test scenarios';
COMMENT ON COLUMN transactions.updated_at IS 'Last update timestamp for integration testing';

-- Insert comprehensive test data for integration testing scenarios
INSERT INTO transactions (
    transaction_id, card_number, account_id, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount,
    merchant_id, merchant_name, merchant_city, merchant_zip,
    transaction_timestamp, processing_timestamp, created_at, updated_at
) VALUES
    -- Test transaction for card 0500024453765740 (Purchase)
    ('TXN00001PURCHASE', '0500024453765740', '00000000050', '01', 1,
     'POS', 'Grocery Store Purchase', 125.75,
     '123456789', 'WHOLE FOODS MARKET', 'Austin', '78701',
     '2024-01-15 14:30:00+00', '2024-01-15 14:30:05+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test transaction for card 0683586198171516 (Cash Advance)
    ('TXN00002CASHADVNC', '0683586198171516', '00000000027', '02', 1,
     'ATM', 'ATM Cash Advance', 200.00,
     '987654321', 'BANK OF AMERICA ATM', 'Dallas', '75201',
     '2024-02-10 16:45:00+00', '2024-02-10 16:45:03+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test transaction for card 0923877193247330 (Payment)
    ('TXN00003PAYMENT01', '0923877193247330', '00000000002', '03', 1,
     'ONLINE', 'Online Payment Transaction', -500.00,
     NULL, NULL, NULL, NULL,
     '2024-03-05 09:15:00+00', '2024-03-05 09:15:02+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test transaction for card 0927987108636232 (Balance Transfer)
    ('TXN00004BALXFER01', '0927987108636232', '00000000020', '04', 1,
     'PHONE', 'Promotional Balance Transfer', 1500.00,
     NULL, 'BALANCE TRANSFER', NULL, NULL,
     '2024-04-20 11:00:00+00', '2024-04-20 11:00:10+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test transaction for card 0982496213629795 (Fee)
    ('TXN00005FEE000001', '0982496213629795', '00000000012', '05', 1,
     'SYSTEM', 'Annual Fee Charge', 95.00,
     NULL, 'CARD SERVICES', NULL, NULL,
     '2024-05-01 00:05:00+00', '2024-05-01 00:05:01+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test transaction for high-volume scenario testing (multiple entries in different months)
    ('TXN00006RETAIL001', '0500024453765740', '00000000050', '01', 1,
     'POS', 'Department Store Purchase', 89.99,
     '555666777', 'TARGET STORE #1234', 'Houston', '77001',
     '2024-06-15 13:22:00+00', '2024-06-15 13:22:04+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('TXN00007ONLINE001', '0683586198171516', '00000000027', '01', 2,
     'ONLINE', 'E-commerce Purchase', 234.50,
     '888999000', 'AMAZON.COM', 'Seattle', '98101',
     '2024-07-08 20:15:00+00', '2024-07-08 20:15:06+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Edge case testing: Zero amount transaction (authorization)
    ('TXN00008AUTH0000', '1014086565224350', '00000000044', '10', 1,
     'POS', 'Authorization Pre-Check', 0.00,
     '111222333', 'GAS STATION PUMP #3', 'Phoenix', '85001',
     '2024-08-12 07:45:00+00', '2024-08-12 07:45:01+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Large amount testing for financial precision validation
    ('TXN00009BIGAMOUNT', '0500024453765740', '00000000050', '01', 1,
     'POS', 'High-Value Purchase Test', 9999.99,
     '444555666', 'LUXURY RETAILER', 'Beverly Hills', '90210',
     '2024-09-25 15:30:00+00', '2024-09-25 15:30:08+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Refund transaction testing
    ('TXN00010REFUND001', '0923877193247330', '00000000002', '09', 1,
     'POS', 'Purchase Refund Transaction', -150.25,
     '123456789', 'WHOLE FOODS MARKET', 'Austin', '78701',
     '2024-10-05 12:00:00+00', '2024-10-05 12:00:03+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Future dated transactions for 2025 testing
    ('TXN00011FUTURE001', '0927987108636232', '00000000020', '01', 1,
     'POS', 'Future Date Test Transaction', 75.50,
     '777888999', 'COFFEE SHOP DOWNTOWN', 'Austin', '78702',
     '2025-01-15 08:30:00+00', '2025-01-15 08:30:02+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('TXN00012FUTURE002', '0982496213629795', '00000000012', '03', 5,
     'ONLINE', 'Future Auto Payment', -300.00,
     NULL, 'AUTO PAYMENT', NULL, NULL,
     '2025-02-01 00:01:00+00', '2025-02-01 00:01:05+00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create test-specific indexes for performance testing scenarios
CREATE INDEX idx_transactions_test_high_volume ON transactions (transaction_timestamp, transaction_amount, card_number);
CREATE INDEX idx_transactions_test_merchant_analysis ON transactions (merchant_id, merchant_name, transaction_amount);
CREATE INDEX idx_transactions_test_financial_precision ON transactions (transaction_amount, transaction_type);
CREATE INDEX idx_transactions_test_date_range_queries ON transactions (transaction_timestamp, account_id, transaction_type);

-- Update table statistics for query optimization in test scenarios
ANALYZE transactions;

--rollback DROP TABLE IF EXISTS transactions CASCADE;

--changeset blitzy-agent:003-create-test-transactions-table-triggers splitStatements:false rollbackSplitStatements:false
--comment: Create triggers for test transactions table to support integration testing scenarios and audit trail maintenance

-- Create trigger function for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_transactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for updated_at timestamp
CREATE TRIGGER trigger_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_transactions_updated_at();

-- Create trigger function for account_id derivation from card_number
CREATE OR REPLACE FUNCTION derive_account_id_from_card()
RETURNS TRIGGER AS $$
BEGIN
    -- Automatically populate account_id from cards table relationship
    IF NEW.account_id IS NULL THEN
        SELECT account_id INTO NEW.account_id
        FROM cards
        WHERE card_number = NEW.card_number;
        
        -- Raise exception if card not found (data integrity check)
        IF NEW.account_id IS NULL THEN
            RAISE EXCEPTION 'Card number % not found in cards table. Cannot derive account_id.', NEW.card_number;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for account_id derivation
CREATE TRIGGER trigger_derive_account_id
    BEFORE INSERT ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION derive_account_id_from_card();

-- Create trigger function for transaction amount validation
CREATE OR REPLACE FUNCTION validate_transaction_amount()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate transaction amount precision for BigDecimal testing
    IF NEW.transaction_amount != ROUND(NEW.transaction_amount, 2) THEN
        RAISE EXCEPTION 'Transaction amount must have exactly 2 decimal places. Amount: %', NEW.transaction_amount;
    END IF;
    
    -- Validate reasonable transaction amount ranges for test scenarios
    IF ABS(NEW.transaction_amount) > 99999999.99 THEN
        RAISE EXCEPTION 'Transaction amount exceeds maximum allowed value. Amount: %', NEW.transaction_amount;
    END IF;
    
    -- Set processing timestamp if not provided (for integration testing)
    IF NEW.processing_timestamp IS NULL THEN
        NEW.processing_timestamp = NEW.transaction_timestamp + INTERVAL '1 second';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for transaction amount validation
CREATE TRIGGER trigger_validate_transaction_amount
    BEFORE INSERT OR UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION validate_transaction_amount();

-- Create trigger function for partition maintenance logging
CREATE OR REPLACE FUNCTION log_partition_access()
RETURNS TRIGGER AS $$
BEGIN
    -- Log partition access for performance testing analysis
    -- This helps identify partition pruning effectiveness in test scenarios
    PERFORM pg_notify('transaction_partition_access', 
                     FORMAT('Table: %I, Transaction ID: %s, Timestamp: %s', 
                            TG_TABLE_NAME, NEW.transaction_id, NEW.transaction_timestamp));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for partition access logging on each partition
-- Note: This will be applied to each partition individually for comprehensive monitoring
DO $$
DECLARE
    partition_name TEXT;
BEGIN
    FOR partition_name IN 
        SELECT schemaname||'.'||tablename 
        FROM pg_tables 
        WHERE tablename LIKE 'transactions_20%'
    LOOP
        EXECUTE FORMAT('CREATE TRIGGER trigger_log_partition_access_%s
                       AFTER INSERT ON %s
                       FOR EACH ROW
                       EXECUTE FUNCTION log_partition_access()', 
                       REPLACE(partition_name, '.', '_'), 
                       partition_name);
    END LOOP;
END $$;

-- Add comments for trigger documentation
COMMENT ON FUNCTION update_transactions_updated_at() IS 'Trigger function to automatically update the updated_at timestamp for transactions table modifications';
COMMENT ON FUNCTION derive_account_id_from_card() IS 'Trigger function to automatically derive account_id from card_number for data integrity';
COMMENT ON FUNCTION validate_transaction_amount() IS 'Trigger function to validate transaction amounts for BigDecimal precision and set processing timestamp';
COMMENT ON FUNCTION log_partition_access() IS 'Trigger function to log partition access patterns for performance testing analysis';

--rollback DROP TRIGGER IF EXISTS trigger_transactions_updated_at ON transactions;
--rollback DROP TRIGGER IF EXISTS trigger_derive_account_id ON transactions;
--rollback DROP TRIGGER IF EXISTS trigger_validate_transaction_amount ON transactions;
--rollback DO $$ DECLARE partition_name TEXT; BEGIN FOR partition_name IN SELECT schemaname||'.'||tablename FROM pg_tables WHERE tablename LIKE 'transactions_20%' LOOP EXECUTE FORMAT('DROP TRIGGER IF EXISTS trigger_log_partition_access_%s ON %s', REPLACE(partition_name, '.', '_'), partition_name); END LOOP; END $$;
--rollback DROP FUNCTION IF EXISTS update_transactions_updated_at();
--rollback DROP FUNCTION IF EXISTS derive_account_id_from_card();
--rollback DROP FUNCTION IF EXISTS validate_transaction_amount();
--rollback DROP FUNCTION IF EXISTS log_partition_access();