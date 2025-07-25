--liquibase formatted sql

--changeset blitzy-agent:003-create-test-transactions-table splitStatements:false rollbackSplitStatements:false
--comment: Create partitioned PostgreSQL transactions table for test environment based on CVTRA05Y.cpy TRAN-RECORD structure with monthly RANGE partitioning
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'transactions' AND table_schema = current_schema()

-- ================================================================
-- CardDemo Test Transactions Table Creation
-- 
-- Purpose: Create PostgreSQL transactions table for test environment with
--          exact COBOL TRAN-RECORD structure mapping, monthly RANGE partitioning
--          for high-volume transaction processing integration tests, and foreign
--          key relationships supporting Spring Batch testing scenarios
--
-- Source Mapping: COBOL copybook app/cpy/CVTRA05Y.cpy TRAN-RECORD structure
--   TRAN-ID                        PIC X(16) → transaction_id VARCHAR(16) PRIMARY KEY
--   TRAN-TYPE-CD                   PIC X(02) → transaction_type_code VARCHAR(2) FOREIGN KEY
--   TRAN-CAT-CD                    PIC 9(04) → transaction_category_code INTEGER FOREIGN KEY
--   TRAN-SOURCE                    PIC X(10) → transaction_source VARCHAR(10) NOT NULL
--   TRAN-DESC                      PIC X(100) → transaction_description VARCHAR(100) NOT NULL
--   TRAN-AMT                       PIC S9(09)V99 → transaction_amount DECIMAL(11,2) NOT NULL
--   TRAN-MERCHANT-ID               PIC 9(09) → merchant_id VARCHAR(9) NULL
--   TRAN-MERCHANT-NAME             PIC X(50) → merchant_name VARCHAR(50) NULL
--   TRAN-MERCHANT-CITY             PIC X(50) → merchant_city VARCHAR(50) NULL
--   TRAN-MERCHANT-ZIP              PIC X(10) → merchant_zip VARCHAR(10) NULL
--   TRAN-CARD-NUM                  PIC X(16) → card_number VARCHAR(16) FOREIGN KEY
--   TRAN-ORIG-TS                   PIC X(26) → original_timestamp TIMESTAMP WITH TIME ZONE
--   TRAN-PROC-TS                   PIC X(26) → processing_timestamp TIMESTAMP WITH TIME ZONE
--   FILLER                         PIC X(20) → Not mapped (legacy padding)
--
-- Additional fields for modern integration testing requirements:
--   account_id VARCHAR(11) - Derived from cards.account_id for direct account relationships
--   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--   transaction_status VARCHAR(1) - 'P'ending, 'C'ompleted, 'F'ailed, 'R'eversed
--
-- Test Environment Specifications:
-- - Monthly RANGE partitioning on processing_timestamp for performance validation
-- - DECIMAL(11,2) precision for transaction amounts (exact COBOL COMP-3 equivalence)
-- - Composite foreign key constraints to accounts, cards, and reference tables
-- - Support for high-volume transaction processing integration tests (10,000+ TPS)
-- - Enhanced constraints for comprehensive transaction validation testing
-- - Audit fields for Spring Batch integration testing scenarios
-- ================================================================

-- Create the parent transactions table with partitioning
CREATE TABLE transactions (
    -- Primary key: Transaction ID mapping from COBOL TRAN-ID
    transaction_id VARCHAR(16) NOT NULL,
    
    -- Foreign key relationships for integration testing
    account_id VARCHAR(11) NOT NULL, -- Derived relationship for direct account access
    card_number VARCHAR(16) NOT NULL, -- References cards table (TRAN-CARD-NUM)
    
    -- Transaction classification fields with foreign key constraints
    transaction_type_code VARCHAR(2) NOT NULL, -- References trantype table (TRAN-TYPE-CD)
    transaction_category_code INTEGER NOT NULL, -- References trancatg table (TRAN-CAT-CD)
    
    -- Core transaction data from COBOL structure
    transaction_source VARCHAR(10) NOT NULL, -- TRAN-SOURCE
    transaction_description VARCHAR(100) NOT NULL, -- TRAN-DESC
    transaction_amount DECIMAL(11,2) NOT NULL, -- TRAN-AMT with exact precision
    
    -- Merchant information fields (optional)
    merchant_id VARCHAR(9) NULL, -- TRAN-MERCHANT-ID
    merchant_name VARCHAR(50) NULL, -- TRAN-MERCHANT-NAME
    merchant_city VARCHAR(50) NULL, -- TRAN-MERCHANT-CITY
    merchant_zip VARCHAR(10) NULL, -- TRAN-MERCHANT-ZIP
    
    -- Timestamp fields for transaction lifecycle tracking
    original_timestamp TIMESTAMP WITH TIME ZONE NOT NULL, -- TRAN-ORIG-TS
    processing_timestamp TIMESTAMP WITH TIME ZONE NOT NULL, -- TRAN-PROC-TS (partitioning key)
    
    -- Additional fields for modern integration testing
    transaction_status VARCHAR(1) NOT NULL DEFAULT 'P', -- Status tracking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id, processing_timestamp),
    
    -- Foreign key constraint for account relationship testing
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) 
        REFERENCES accounts (account_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Foreign key constraint for card relationship testing
    CONSTRAINT fk_transactions_card FOREIGN KEY (card_number) 
        REFERENCES cards (card_number) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Foreign key constraint for transaction type validation
    CONSTRAINT fk_transactions_trantype FOREIGN KEY (transaction_type_code) 
        REFERENCES trantype (transaction_type) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Composite foreign key constraint for transaction category validation
    CONSTRAINT fk_transactions_trancatg FOREIGN KEY (transaction_type_code, transaction_category_code) 
        REFERENCES trancatg (transaction_type_code, transaction_category_code) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business rule constraints for comprehensive test data validation
    CONSTRAINT chk_transaction_id_format CHECK (transaction_id ~ '^[A-Z0-9]{16}$'), -- 16 alphanumeric chars
    CONSTRAINT chk_account_id_format CHECK (account_id ~ '^[0-9]{11}$'), -- 11 numeric digits
    CONSTRAINT chk_card_number_format CHECK (card_number ~ '^[0-9]{16}$'), -- 16 numeric digits
    CONSTRAINT chk_transaction_type_code_format CHECK (transaction_type_code ~ '^[A-Z0-9]{2}$'),
    CONSTRAINT chk_transaction_category_code_range CHECK (transaction_category_code >= 0 AND transaction_category_code <= 9999),
    CONSTRAINT chk_transaction_source_not_empty CHECK (TRIM(transaction_source) <> ''),
    CONSTRAINT chk_transaction_description_not_empty CHECK (TRIM(transaction_description) <> ''),
    CONSTRAINT chk_transaction_amount_precision CHECK (transaction_amount = ROUND(transaction_amount, 2)),
    CONSTRAINT chk_merchant_id_format CHECK (merchant_id IS NULL OR merchant_id ~ '^[0-9]{9}$'),
    CONSTRAINT chk_merchant_zip_format CHECK (merchant_zip IS NULL OR merchant_zip ~ '^[0-9]{5}(-[0-9]{4})?$'),
    CONSTRAINT chk_original_timestamp_not_future CHECK (original_timestamp <= CURRENT_TIMESTAMP + INTERVAL '1 hour'),
    CONSTRAINT chk_processing_after_original CHECK (processing_timestamp >= original_timestamp),
    CONSTRAINT chk_transaction_status CHECK (transaction_status IN ('P', 'C', 'F', 'R')) -- Pending, Completed, Failed, Reversed
    
) PARTITION BY RANGE (processing_timestamp);

-- ================================================================
-- MONTHLY PARTITIONING SETUP FOR PERFORMANCE VALIDATION
-- Supporting Database Design 6.2.1.4 partitioning strategy for test environment
-- ================================================================

-- Create monthly partitions for current year and next year (24 months total)
-- This supports high-volume transaction processing integration tests

-- 2024 Monthly Partitions
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

-- 2025 Monthly Partitions
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

-- ================================================================
-- PERFORMANCE INDEXES FOR TEST ENVIRONMENT OPTIMIZATION
-- Supporting high-volume transaction processing integration tests (10,000+ TPS)
-- ================================================================

-- Primary access indexes for transaction processing services
CREATE UNIQUE INDEX idx_transactions_transaction_id ON transactions (transaction_id, processing_timestamp);
CREATE INDEX idx_transactions_account_id ON transactions (account_id, processing_timestamp);
CREATE INDEX idx_transactions_card_number ON transactions (card_number, processing_timestamp);
CREATE INDEX idx_transactions_processing_timestamp ON transactions (processing_timestamp);
CREATE INDEX idx_transactions_original_timestamp ON transactions (original_timestamp);

-- Foreign key relationship indexes for join optimization
CREATE INDEX idx_transactions_account_card ON transactions (account_id, card_number);
CREATE INDEX idx_transactions_type_category ON transactions (transaction_type_code, transaction_category_code);

-- Business query indexes for common test scenarios
CREATE INDEX idx_transactions_amount_range ON transactions (transaction_amount, processing_timestamp);
CREATE INDEX idx_transactions_status_timestamp ON transactions (transaction_status, processing_timestamp);
CREATE INDEX idx_transactions_merchant_lookup ON transactions (merchant_id, processing_timestamp);
CREATE INDEX idx_transactions_source_type ON transactions (transaction_source, transaction_type_code);

-- Audit and monitoring indexes for Spring Batch integration testing
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_updated_at ON transactions (updated_at);

-- Composite indexes for complex transaction queries in integration tests
CREATE INDEX idx_transactions_account_status_amount ON transactions (account_id, transaction_status, transaction_amount);
CREATE INDEX idx_transactions_card_type_timestamp ON transactions (card_number, transaction_type_code, processing_timestamp);
CREATE INDEX idx_transactions_merchant_city_zip ON transactions (merchant_city, merchant_zip);

-- Date range indexes for time-based queries and partitioning pruning
CREATE INDEX idx_transactions_date_range ON transactions (processing_timestamp, original_timestamp);
CREATE INDEX idx_transactions_daily_summary ON transactions (DATE(processing_timestamp), transaction_type_code, transaction_amount);

-- ================================================================
-- TABLE COMMENTS FOR TEST DOCUMENTATION
-- Supporting Testing Strategy 6.6.3.3 comprehensive transaction validation
-- ================================================================

COMMENT ON TABLE transactions IS 'Test environment transactions table for CardDemo transaction processing service testing. Mapped from COBOL TRAN-RECORD structure in CVTRA05Y.cpy copybook with monthly RANGE partitioning for high-volume integration testing and exact DECIMAL(11,2) precision for financial calculations.';

-- Primary key and relationship comments
COMMENT ON COLUMN transactions.transaction_id IS 'Primary key: 16-character transaction identifier from COBOL TRAN-ID field';
COMMENT ON COLUMN transactions.account_id IS 'Foreign key to accounts table derived from card-account relationship for direct access';
COMMENT ON COLUMN transactions.card_number IS 'Foreign key to cards table from COBOL TRAN-CARD-NUM field';

-- Transaction classification comments
COMMENT ON COLUMN transactions.transaction_type_code IS 'Foreign key to trantype table from COBOL TRAN-TYPE-CD field';
COMMENT ON COLUMN transactions.transaction_category_code IS 'Foreign key to trancatg table from COBOL TRAN-CAT-CD field';

-- Core transaction data comments
COMMENT ON COLUMN transactions.transaction_source IS 'Transaction source system from COBOL TRAN-SOURCE field';
COMMENT ON COLUMN transactions.transaction_description IS 'Transaction description from COBOL TRAN-DESC field';
COMMENT ON COLUMN transactions.transaction_amount IS 'Transaction amount from COBOL TRAN-AMT with DECIMAL(11,2) precision for BigDecimal testing';

-- Merchant information comments
COMMENT ON COLUMN transactions.merchant_id IS 'Merchant identifier from COBOL TRAN-MERCHANT-ID field';
COMMENT ON COLUMN transactions.merchant_name IS 'Merchant name from COBOL TRAN-MERCHANT-NAME field';
COMMENT ON COLUMN transactions.merchant_city IS 'Merchant city from COBOL TRAN-MERCHANT-CITY field';
COMMENT ON COLUMN transactions.merchant_zip IS 'Merchant ZIP code from COBOL TRAN-MERCHANT-ZIP field';

-- Timestamp and audit comments
COMMENT ON COLUMN transactions.original_timestamp IS 'Original transaction timestamp from COBOL TRAN-ORIG-TS field';
COMMENT ON COLUMN transactions.processing_timestamp IS 'Processing timestamp from COBOL TRAN-PROC-TS field (partitioning key)';
COMMENT ON COLUMN transactions.transaction_status IS 'Transaction processing status: P=Pending, C=Completed, F=Failed, R=Reversed';
COMMENT ON COLUMN transactions.created_at IS 'Transaction record creation timestamp for audit trail in test scenarios';
COMMENT ON COLUMN transactions.updated_at IS 'Last update timestamp for integration testing';

-- ================================================================
-- COMPREHENSIVE TEST DATA FOR INTEGRATION TESTING SCENARIOS
-- Supporting Spring Batch integration testing and financial precision validation
-- ================================================================

-- Insert test transaction data with exact decimal precision for BigDecimal validation
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type_code, transaction_category_code,
    transaction_source, transaction_description, transaction_amount,
    merchant_id, merchant_name, merchant_city, merchant_zip,
    original_timestamp, processing_timestamp, transaction_status,
    created_at, updated_at
) VALUES
    -- Purchase transactions for financial precision testing
    ('TXN0000000000001', '00000000050', '0500024453765740', '01', 0001, 'ONLINE', 'Amazon Purchase - Electronics',
     1299.99, '123456789', 'Amazon.com', 'Seattle', '98109',
     '2024-07-15 14:30:00+00', '2024-07-15 14:30:15+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    ('TXN0000000000002', '00000000027', '0683586198171516', '01', 0002, 'POS', 'Grocery Store Purchase',
     87.45, '987654321', 'Whole Foods Market', 'Austin', '78701',
     '2024-07-20 09:15:30+00', '2024-07-20 09:15:45+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Cash advance transactions for exact precision testing
    ('TXN0000000000003', '00000000002', '0923877193247330', '02', 0001, 'ATM', 'ATM Cash Advance',
     500.00, '555666777', 'Chase ATM #1234', 'Dallas', '75201',
     '2024-08-05 16:45:00+00', '2024-08-05 16:45:10+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Payment transactions for negative amount testing
    ('TXN0000000000004', '00000000020', '0927987108636232', '03', 0001, 'ONLINE', 'Online Payment',
     -750.25, NULL, 'CardDemo Payment Portal', NULL, NULL,
     '2024-08-10 22:00:00+00', '2024-08-10 22:00:05+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Fee transactions for small amount precision
    ('TXN0000000000005', '00000000012', '0982496213629795', '05', 0002, 'SYSTEM', 'Late Payment Fee',
     39.99, NULL, 'CardDemo System', NULL, NULL,
     '2024-09-01 00:00:00+00', '2024-09-01 00:00:30+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Interest charge for financial calculation testing
    ('TXN0000000000006', '00000000044', '1014086565224350', '06', 0001, 'SYSTEM', 'Purchase Interest Charge',
     125.67, NULL, 'CardDemo Interest Calculation', NULL, NULL,
     '2024-09-15 03:00:00+00', '2024-09-15 03:00:15+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- High-value transaction for stress testing
    ('TXN0000000000007', '00000000037', '1142167692878931', '01', 0001, 'POS', 'High-Value Electronics Purchase',
     9999.99, '111222333', 'Best Buy Store #456', 'Los Angeles', '90210',
     '2024-10-01 13:20:00+00', '2024-10-01 13:20:30+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Pending transaction for status testing
    ('TXN0000000000008', '00000000035', '1561409106491600', '01', 0002, 'ONLINE', 'Pending Online Purchase',
     249.50, '444555666', 'Target.com', 'Minneapolis', '55401',
     '2024-10-15 11:45:00+00', '2024-10-15 11:45:05+00', 'P',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Refund transaction for negative amount validation
    ('TXN0000000000009', '00000000050', '0500024453765740', '09', 0001, 'ONLINE', 'Purchase Refund',
     -199.99, '123456789', 'Amazon.com', 'Seattle', '98109',
     '2024-11-01 10:30:00+00', '2024-11-01 10:30:20+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Failed transaction for error scenario testing
    ('TXN0000000000010', '00000000027', '0683586198171516', '01', 0001, 'POS', 'Failed Transaction - Insufficient Funds',
     5000.00, '777888999', 'Luxury Store', 'Beverly Hills', '90210',
     '2024-11-10 15:00:00+00', '2024-11-10 15:00:05+00', 'F',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Balance transfer for complex financial testing
    ('TXN0000000000011', '00000000002', '0923877193247330', '04', 0001, 'SYSTEM', 'Promotional Balance Transfer',
     2500.00, NULL, 'CardDemo Balance Transfer', NULL, NULL,
     '2024-12-01 08:00:00+00', '2024-12-01 08:00:10+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Exact penny precision for BigDecimal edge case testing
    ('TXN0000000000012', '00000000020', '0927987108636232', '01', 0003, 'RECURRING', 'Subscription Payment',
     12.34, '999000111', 'Netflix Subscription', 'Los Gatos', '95032',
     '2025-01-15 12:00:00+00', '2025-01-15 12:00:03+00', 'C',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ================================================================
-- PERFORMANCE OPTIMIZATION FOR TEST ENVIRONMENT
-- Supporting Testing Strategy 6.6.4.2 performance validation requirements
-- ================================================================

-- Update table statistics for query planner optimization across all partitions
ANALYZE transactions;

-- Update statistics for each monthly partition for optimal query planning
ANALYZE transactions_2024_01;
ANALYZE transactions_2024_02;
ANALYZE transactions_2024_03;
ANALYZE transactions_2024_04;
ANALYZE transactions_2024_05;
ANALYZE transactions_2024_06;
ANALYZE transactions_2024_07;
ANALYZE transactions_2024_08;
ANALYZE transactions_2024_09;
ANALYZE transactions_2024_10;
ANALYZE transactions_2024_11;
ANALYZE transactions_2024_12;
ANALYZE transactions_2025_01;
ANALYZE transactions_2025_02;
ANALYZE transactions_2025_03;
ANALYZE transactions_2025_04;
ANALYZE transactions_2025_05;
ANALYZE transactions_2025_06;
ANALYZE transactions_2025_07;
ANALYZE transactions_2025_08;
ANALYZE transactions_2025_09;
ANALYZE transactions_2025_10;
ANALYZE transactions_2025_11;
ANALYZE transactions_2025_12;

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

-- Create trigger for updated_at timestamp across all partitions
CREATE TRIGGER trigger_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_transactions_updated_at();

-- Create trigger function for transaction validation in test scenarios
CREATE OR REPLACE FUNCTION validate_transaction_business_rules()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate transaction amount precision for BigDecimal testing
    IF NEW.transaction_amount != ROUND(NEW.transaction_amount, 2) THEN
        RAISE EXCEPTION 'Transaction amount must have exactly 2 decimal places. Amount: %', NEW.transaction_amount;
    END IF;
    
    -- Validate processing timestamp is not before original timestamp
    IF NEW.processing_timestamp < NEW.original_timestamp THEN
        RAISE EXCEPTION 'Processing timestamp cannot be before original timestamp. Original: %, Processing: %', 
                       NEW.original_timestamp, NEW.processing_timestamp;
    END IF;
    
    -- Validate merchant information consistency for merchant transactions
    IF NEW.merchant_id IS NOT NULL AND (NEW.merchant_name IS NULL OR TRIM(NEW.merchant_name) = '') THEN
        RAISE EXCEPTION 'Merchant name is required when merchant ID is provided. Merchant ID: %', NEW.merchant_id;
    END IF;
    
    -- Log status changes for integration testing audit trail
    IF TG_OP = 'UPDATE' AND NEW.transaction_status != OLD.transaction_status THEN
        -- Status change logged via updated_at trigger
        NULL; -- Placeholder for additional status change logging if needed
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for transaction business rule validation
CREATE TRIGGER trigger_validate_transaction_rules
    BEFORE INSERT OR UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION validate_transaction_business_rules();

-- Create trigger function for account balance impact tracking (for future integration)
CREATE OR REPLACE FUNCTION track_transaction_account_impact()
RETURNS TRIGGER AS $$
BEGIN
    -- This trigger is designed for future integration with account balance updates
    -- Currently serves as a placeholder for transaction-to-account synchronization testing
    
    -- Log transaction completion for integration testing scenarios
    IF NEW.transaction_status = 'C' AND (TG_OP = 'INSERT' OR OLD.transaction_status != 'C') THEN
        -- Placeholder for future account balance update integration
        -- In full implementation, this would trigger account balance recalculation
        NULL;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for account impact tracking
CREATE TRIGGER trigger_track_transaction_account_impact
    AFTER INSERT OR UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION track_transaction_account_impact();

-- Add comments for trigger documentation and test scenario identification
COMMENT ON FUNCTION update_transactions_updated_at() IS 'Trigger function to automatically update the updated_at timestamp for transactions table modifications across all partitions';
COMMENT ON FUNCTION validate_transaction_business_rules() IS 'Trigger function to validate transaction business rules including amount precision, timestamp consistency, and merchant data validation for integration testing';
COMMENT ON FUNCTION track_transaction_account_impact() IS 'Trigger function to track transaction impacts on account balances for future integration with account management services';

--rollback DROP TRIGGER IF EXISTS trigger_transactions_updated_at ON transactions; DROP TRIGGER IF EXISTS trigger_validate_transaction_rules ON transactions; DROP TRIGGER IF EXISTS trigger_track_transaction_account_impact ON transactions; DROP FUNCTION IF EXISTS update_transactions_updated_at(); DROP FUNCTION IF EXISTS validate_transaction_business_rules(); DROP FUNCTION IF EXISTS track_transaction_account_impact();