-- =====================================================================
-- Liquibase Changeset: Create Test Transactions Table
-- Description: PostgreSQL partitioned transactions table for test environment
-- Author: Blitzy agent
-- Version: CardDemo_v1.0-15-g27d6c6f-68
-- =====================================================================
--
-- Maps COBOL TRAN-RECORD structure from CVTRA05Y.cpy to PostgreSQL:
-- 01 TRAN-RECORD.
--   05 TRAN-ID                    PIC X(16)       -> transaction_id VARCHAR(16) PRIMARY KEY
--   05 TRAN-TYPE-CD               PIC X(02)       -> transaction_type VARCHAR(2) FOREIGN KEY
--   05 TRAN-CAT-CD                PIC 9(04)       -> transaction_category VARCHAR(4) FOREIGN KEY
--   05 TRAN-SOURCE                PIC X(10)       -> transaction_source VARCHAR(10)
--   05 TRAN-DESC                  PIC X(100)      -> transaction_description VARCHAR(100)
--   05 TRAN-AMT                   PIC S9(09)V99   -> transaction_amount DECIMAL(11,2)
--   05 TRAN-MERCHANT-ID           PIC 9(09)       -> merchant_id VARCHAR(9)
--   05 TRAN-MERCHANT-NAME         PIC X(50)       -> merchant_name VARCHAR(50)
--   05 TRAN-MERCHANT-CITY         PIC X(50)       -> merchant_city VARCHAR(50)
--   05 TRAN-MERCHANT-ZIP          PIC X(10)       -> merchant_zip VARCHAR(10)
--   05 TRAN-CARD-NUM              PIC X(16)       -> card_number VARCHAR(16) FOREIGN KEY
--   05 TRAN-ORIG-TS               PIC X(26)       -> original_timestamp TIMESTAMP
--   05 TRAN-PROC-TS               PIC X(26)       -> processing_timestamp TIMESTAMP
--   05 FILLER                     PIC X(20)       -> (not mapped - COBOL filler)
--
-- Spring Boot Integration Features:
-- - JPA Entity mapping for transaction processing microservices
-- - Monthly RANGE partitioning for high-volume transaction processing
-- - BigDecimal precision for financial calculations with DECIMAL(11,2)
-- - Composite foreign key constraints for referential integrity
-- - Optimized for TestContainers integration testing
-- - Support for Spring Data JPA repository operations
-- - Spring Batch integration for batch processing test validation
-- =====================================================================

-- liquibase formatted sql

-- changeset blitzy-agent:003-create-test-transactions-table
-- comment: Create partitioned transactions table for test environment with exact decimal precision and monthly partitioning

-- =============================================================================
-- Create Parent Transactions Table with Partitioning
-- =============================================================================
CREATE TABLE transactions (
    -- Primary key mapping from COBOL TRAN-ID (PIC X(16))
    -- 16-character transaction identifier for unique transaction identification
    transaction_id VARCHAR(16) NOT NULL,
    
    -- Account reference via card relationship
    -- Derived from card_number to accounts table foreign key linkage
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key to cards table mapping from COBOL TRAN-CARD-NUM (PIC X(16))
    -- Links transactions to specific credit cards for transaction processing
    card_number VARCHAR(16) NOT NULL,
    
    -- Foreign key to transaction_types table mapping from COBOL TRAN-TYPE-CD (PIC X(02))
    -- Transaction type classification (01=Purchase, 02=Cash Advance, 03=Payment, etc.)
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Foreign key to transaction_categories table mapping from COBOL TRAN-CAT-CD (PIC 9(04))
    -- Transaction category code for detailed transaction classification
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Transaction source mapping from COBOL TRAN-SOURCE (PIC X(10))
    -- Source system or channel where transaction originated
    transaction_source VARCHAR(10) NOT NULL,
    
    -- Transaction description mapping from COBOL TRAN-DESC (PIC X(100))
    -- Detailed transaction description for display and reporting
    transaction_description VARCHAR(100) NOT NULL,
    
    -- Transaction amount with exact COBOL precision
    -- Maps from COBOL TRAN-AMT (PIC S9(09)V99)
    -- DECIMAL(11,2) ensures exact BigDecimal precision for financial calculations
    transaction_amount DECIMAL(11,2) NOT NULL,
    
    -- Merchant identification mapping from COBOL TRAN-MERCHANT-ID (PIC 9(09))
    -- Merchant identifier for transaction processing
    merchant_id VARCHAR(9) NOT NULL,
    
    -- Merchant name mapping from COBOL TRAN-MERCHANT-NAME (PIC X(50))
    -- Merchant business name for transaction display
    merchant_name VARCHAR(50) NOT NULL,
    
    -- Merchant city mapping from COBOL TRAN-MERCHANT-CITY (PIC X(50))
    -- Merchant location city for transaction processing
    merchant_city VARCHAR(50) NOT NULL,
    
    -- Merchant ZIP code mapping from COBOL TRAN-MERCHANT-ZIP (PIC X(10))
    -- Merchant location ZIP code for geographic processing
    merchant_zip VARCHAR(10) NOT NULL,
    
    -- Original timestamp mapping from COBOL TRAN-ORIG-TS (PIC X(26))
    -- Original transaction timestamp from source system
    original_timestamp TIMESTAMP NOT NULL,
    
    -- Processing timestamp mapping from COBOL TRAN-PROC-TS (PIC X(26))
    -- System processing timestamp for transaction lifecycle tracking
    processing_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Audit timestamps for test environment tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id, processing_timestamp),
    
    -- Foreign key constraint to accounts table via card relationship
    CONSTRAINT fk_transactions_account_id FOREIGN KEY (account_id) 
        REFERENCES accounts(account_id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Foreign key constraint to cards table for card-transaction relationship
    CONSTRAINT fk_transactions_card_number FOREIGN KEY (card_number) 
        REFERENCES cards(card_number) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Foreign key constraint to transaction_types table for type classification
    CONSTRAINT fk_transactions_transaction_type FOREIGN KEY (transaction_type) 
        REFERENCES transaction_types(transaction_type) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Foreign key constraint to transaction_categories table for category classification
    CONSTRAINT fk_transactions_transaction_category FOREIGN KEY (transaction_category) 
        REFERENCES transaction_categories(transaction_category) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Check constraints for data integrity (test environment validation)
    CONSTRAINT chk_transactions_transaction_id_format CHECK (
        transaction_id ~ '^[A-Z0-9]{16}$' -- 16 alphanumeric characters exactly
    ),
    
    CONSTRAINT chk_transactions_account_id_format CHECK (
        account_id ~ '^[0-9]{11}$' -- 11 numeric digits exactly
    ),
    
    CONSTRAINT chk_transactions_card_number_format CHECK (
        card_number ~ '^[0-9]{16}$' -- 16 numeric digits exactly
    ),
    
    CONSTRAINT chk_transactions_transaction_type_format CHECK (
        transaction_type ~ '^[A-Z0-9]{2}$' -- 2 alphanumeric characters exactly
    ),
    
    CONSTRAINT chk_transactions_transaction_category_format CHECK (
        transaction_category ~ '^[0-9]{4}$' -- 4 numeric digits exactly
    ),
    
    CONSTRAINT chk_transactions_transaction_source_format CHECK (
        LENGTH(TRIM(transaction_source)) > 0 -- Non-empty source required
    ),
    
    CONSTRAINT chk_transactions_transaction_description_format CHECK (
        LENGTH(TRIM(transaction_description)) > 0 -- Non-empty description required
    ),
    
    CONSTRAINT chk_transactions_transaction_amount_precision CHECK (
        transaction_amount >= -999999999.99 AND transaction_amount <= 999999999.99
    ),
    
    CONSTRAINT chk_transactions_merchant_id_format CHECK (
        merchant_id ~ '^[0-9]{9}$' -- 9 numeric digits exactly
    ),
    
    CONSTRAINT chk_transactions_merchant_name_format CHECK (
        LENGTH(TRIM(merchant_name)) > 0 -- Non-empty merchant name required
    ),
    
    CONSTRAINT chk_transactions_merchant_city_format CHECK (
        LENGTH(TRIM(merchant_city)) > 0 -- Non-empty merchant city required
    ),
    
    CONSTRAINT chk_transactions_merchant_zip_format CHECK (
        merchant_zip ~ '^[0-9]{5}(-[0-9]{4})?$' -- 5-digit ZIP or ZIP+4 format
    ),
    
    CONSTRAINT chk_transactions_original_timestamp_valid CHECK (
        original_timestamp <= CURRENT_TIMESTAMP
    ),
    
    CONSTRAINT chk_transactions_processing_timestamp_valid CHECK (
        processing_timestamp >= original_timestamp
    )
) PARTITION BY RANGE (processing_timestamp);

-- =============================================================================
-- Create Monthly Partitions for Performance Validation
-- =============================================================================

-- Current month partition (adjust dates as needed for test environment)
CREATE TABLE transactions_2024_01 PARTITION OF transactions
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Next month partition
CREATE TABLE transactions_2024_02 PARTITION OF transactions
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- Additional test partitions for comprehensive testing
CREATE TABLE transactions_2024_03 PARTITION OF transactions
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE transactions_2024_04 PARTITION OF transactions
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

-- Future partition for ongoing test data
CREATE TABLE transactions_2024_05 PARTITION OF transactions
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

-- =============================================================================
-- Indexes for Performance Optimization (Test Environment)
-- =============================================================================

-- Primary access pattern: transaction lookup by transaction_id
-- Composite index on partitioned table for optimal query performance
CREATE INDEX idx_transactions_transaction_id 
ON transactions (transaction_id, processing_timestamp);

-- Secondary access pattern: account-based transaction queries
CREATE INDEX idx_transactions_account_id 
ON transactions (account_id, processing_timestamp);

-- Access pattern: card-based transaction lookup for card processing services
CREATE INDEX idx_transactions_card_number 
ON transactions (card_number, processing_timestamp);

-- Access pattern: transaction type analysis for reporting
CREATE INDEX idx_transactions_transaction_type 
ON transactions (transaction_type, processing_timestamp);

-- Access pattern: transaction category filtering for classification
CREATE INDEX idx_transactions_transaction_category 
ON transactions (transaction_category, processing_timestamp);

-- Access pattern: date range queries for transaction history
CREATE INDEX idx_transactions_processing_timestamp 
ON transactions (processing_timestamp);

-- Access pattern: original timestamp for transaction lifecycle tracking
CREATE INDEX idx_transactions_original_timestamp 
ON transactions (original_timestamp, processing_timestamp);

-- Access pattern: amount-based queries for financial analysis
CREATE INDEX idx_transactions_amount 
ON transactions (transaction_amount, processing_timestamp);

-- Access pattern: merchant-based transaction queries
CREATE INDEX idx_transactions_merchant_id 
ON transactions (merchant_id, processing_timestamp);

-- Composite index for comprehensive transaction queries
CREATE INDEX idx_transactions_account_card_type 
ON transactions (account_id, card_number, transaction_type, processing_timestamp);

-- Composite index for transaction processing pipeline
CREATE INDEX idx_transactions_type_category_amount 
ON transactions (transaction_type, transaction_category, transaction_amount, processing_timestamp);

-- =============================================================================
-- Trigger for Updated Timestamp Management
-- =============================================================================

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_transactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to call the function on UPDATE
CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_transactions_updated_at();

-- =============================================================================
-- Comments for Documentation and Maintenance
-- =============================================================================

COMMENT ON TABLE transactions IS 
'CardDemo transactions table for test environment with monthly RANGE partitioning. Maps COBOL TRAN-RECORD structure from CVTRA05Y.cpy to PostgreSQL schema with exact financial precision using DECIMAL(11,2) for BigDecimal compatibility. Optimized for Spring Boot microservices integration testing with high-volume transaction processing validation.';

COMMENT ON COLUMN transactions.transaction_id IS 
'Primary key. Maps from COBOL TRAN-ID (PIC X(16)). 16-character unique transaction identifier.';

COMMENT ON COLUMN transactions.account_id IS 
'Foreign key to accounts table. Links transactions to account through card relationship for transaction processing.';

COMMENT ON COLUMN transactions.card_number IS 
'Foreign key to cards table. Maps from COBOL TRAN-CARD-NUM (PIC X(16)). Links transactions to specific credit cards.';

COMMENT ON COLUMN transactions.transaction_type IS 
'Foreign key to transaction_types table. Maps from COBOL TRAN-TYPE-CD (PIC X(02)). Transaction type classification.';

COMMENT ON COLUMN transactions.transaction_category IS 
'Foreign key to transaction_categories table. Maps from COBOL TRAN-CAT-CD (PIC 9(04)). Transaction category code.';

COMMENT ON COLUMN transactions.transaction_source IS 
'Transaction source. Maps from COBOL TRAN-SOURCE (PIC X(10)). Source system or channel identifier.';

COMMENT ON COLUMN transactions.transaction_description IS 
'Transaction description. Maps from COBOL TRAN-DESC (PIC X(100)). Detailed transaction description.';

COMMENT ON COLUMN transactions.transaction_amount IS 
'Transaction amount. Maps from COBOL TRAN-AMT (PIC S9(09)V99). DECIMAL(11,2) ensures exact BigDecimal precision.';

COMMENT ON COLUMN transactions.merchant_id IS 
'Merchant identifier. Maps from COBOL TRAN-MERCHANT-ID (PIC 9(09)). Merchant identification number.';

COMMENT ON COLUMN transactions.merchant_name IS 
'Merchant name. Maps from COBOL TRAN-MERCHANT-NAME (PIC X(50)). Merchant business name.';

COMMENT ON COLUMN transactions.merchant_city IS 
'Merchant city. Maps from COBOL TRAN-MERCHANT-CITY (PIC X(50)). Merchant location city.';

COMMENT ON COLUMN transactions.merchant_zip IS 
'Merchant ZIP code. Maps from COBOL TRAN-MERCHANT-ZIP (PIC X(10)). Merchant location ZIP code.';

COMMENT ON COLUMN transactions.original_timestamp IS 
'Original timestamp. Maps from COBOL TRAN-ORIG-TS (PIC X(26)). Original transaction timestamp.';

COMMENT ON COLUMN transactions.processing_timestamp IS 
'Processing timestamp. Maps from COBOL TRAN-PROC-TS (PIC X(26)). System processing timestamp used for partitioning.';

COMMENT ON COLUMN transactions.created_at IS 
'Transaction creation timestamp. Added for audit trail and test environment tracking.';

COMMENT ON COLUMN transactions.updated_at IS 
'Last update timestamp. Automatically updated by trigger for audit purposes.';

-- =============================================================================
-- Test Data Seeding for Integration Testing
-- =============================================================================

-- Test transaction for account 12345678901 (ADMIN001 user) - Purchase
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000001', '12345678901', '4532015112830366', '01', '0101',
    'POS', 'Grocery Store Purchase', 45.67, '123456789',
    'KROGER #123', 'ATLANTA', '30309', '2024-01-15 10:30:00', '2024-01-15 10:30:15',
    '2024-01-15 10:30:15'
);

-- Test transaction for account 23456789012 (USER0001) - Cash Advance
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000002', '23456789012', '5555555555554444', '02', '0201',
    'ATM', 'ATM Cash Withdrawal', 200.00, '987654321',
    'BANK OF AMERICA ATM', 'CHICAGO', '60601', '2024-01-16 14:45:00', '2024-01-16 14:45:30',
    '2024-01-16 14:45:30'
);

-- Test transaction for account 12345678901 (ADMIN001 user) - Payment
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000003', '12345678901', '4532015112830366', '03', '0301',
    'ONLINE', 'Online Payment', -150.00, '111222333',
    'CARDDEMO ONLINE', 'ATLANTA', '30309', '2024-01-17 09:15:00', '2024-01-17 09:15:05',
    '2024-01-17 09:15:05'
);

-- Test transaction for account 56789012345 (USER0001) - High-value purchase
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000004', '56789012345', '6011111111111117', '01', '0104',
    'ONLINE', 'Online Retail Purchase', 1250.99, '444555666',
    'AMAZON.COM', 'SEATTLE', '98109', '2024-01-18 16:22:00', '2024-01-18 16:22:10',
    '2024-01-18 16:22:10'
);

-- Test transaction for account 67890123456 (USER0002) - Gas station purchase
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000005', '67890123456', '30569309025904', '01', '0102',
    'POS', 'Gas Station Purchase', 65.43, '777888999',
    'SHELL STATION #456', 'HOUSTON', '77001', '2024-01-19 08:00:00', '2024-01-19 08:00:05',
    '2024-01-19 08:00:05'
);

-- Test transaction for account 78901234567 (USER0001) - Restaurant purchase
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000006', '78901234567', '5105105105105100', '01', '0103',
    'POS', 'Restaurant Purchase', 87.52, '333444555',
    'OLIVE GARDEN #789', 'DENVER', '80202', '2024-01-20 19:30:00', '2024-01-20 19:30:12',
    '2024-01-20 19:30:12'
);

-- Test transaction for February partition testing
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000007', '23456789012', '5555555555554444', '01', '0105',
    'POS', 'Department Store Purchase', 234.56, '666777888',
    'MACY\'S STORE #101', 'NEW YORK', '10001', '2024-02-01 12:00:00', '2024-02-01 12:00:08',
    '2024-02-01 12:00:08'
);

-- Test transaction for March partition testing
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000008', '12345678901', '4532015112830366', '06', '0602',
    'SYSTEM', 'Late Payment Fee', 25.00, '999888777',
    'CARDDEMO SYSTEM', 'ATLANTA', '30309', '2024-03-01 00:05:00', '2024-03-01 00:05:00',
    '2024-03-01 00:05:00'
);

-- Test transaction for BigDecimal precision testing
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000009', '56789012345', '6011111111111117', '01', '0101',
    'POS', 'Precision Test Transaction', 999999999.99, '555666777',
    'PRECISION TEST MERCHANT', 'TESTVILLE', '12345', '2024-01-21 15:45:00', '2024-01-21 15:45:15',
    '2024-01-21 15:45:15'
);

-- Test transaction with negative amount (refund)
INSERT INTO transactions (
    transaction_id, account_id, card_number, transaction_type, transaction_category,
    transaction_source, transaction_description, transaction_amount, merchant_id,
    merchant_name, merchant_city, merchant_zip, original_timestamp, processing_timestamp,
    created_at
) VALUES (
    'TXN0000000000010', '23456789012', '5555555555554444', '04', '0401',
    'POS', 'Merchant Credit Refund', -45.67, '123456789',
    'KROGER #123', 'ATLANTA', '30309', '2024-01-22 11:20:00', '2024-01-22 11:20:05',
    '2024-01-22 11:20:05'
);

-- =============================================================================
-- Grant Permissions for Test Environment
-- =============================================================================

-- Grant necessary permissions for application user (configured in test properties)
-- Note: In actual test environment, these would be handled by TestContainers configuration

-- Permissions for application to read/write transaction data
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions TO carddemo_app_user;

-- Permissions for monitoring/auditing (if applicable in test environment)
-- GRANT SELECT ON transactions TO carddemo_readonly_user;

-- =============================================================================
-- Rollback SQL (for Liquibase rollback capability)
-- =============================================================================

--rollback DROP TRIGGER IF EXISTS trg_transactions_updated_at ON transactions;
--rollback DROP FUNCTION IF EXISTS update_transactions_updated_at();
--rollback DROP TABLE IF EXISTS transactions CASCADE;

-- =============================================================================
-- Changeset Validation and Testing Notes
-- =============================================================================

-- Test SQL Validation Queries (for manual verification):
-- 
-- 1. Verify table structure matches COBOL layout:
-- SELECT column_name, data_type, character_maximum_length, numeric_precision, 
--        numeric_scale, is_nullable, column_default
-- FROM information_schema.columns 
-- WHERE table_name = 'transactions' AND table_schema = 'public'
-- ORDER BY ordinal_position;
--
-- 2. Verify partitioning configuration:
-- SELECT schemaname, tablename, partitionname, partitiontype, partitionkey, partitionboundary
-- FROM pg_partitions 
-- WHERE tablename = 'transactions';
--
-- 3. Verify constraints and foreign keys:
-- SELECT constraint_name, constraint_type, table_name
-- FROM information_schema.table_constraints
-- WHERE table_name = 'transactions' AND table_schema = 'public';
--
-- 4. Verify indexes for performance:
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'transactions' AND schemaname = 'public';
--
-- 5. Test BigDecimal precision validation:
-- SELECT transaction_id, transaction_amount, 
--        CASE WHEN transaction_amount = 999999999.99 THEN 'Max Precision OK' 
--             ELSE 'Check Precision' END as precision_check
-- FROM transactions 
-- WHERE transaction_id = 'TXN0000000000009';
--
-- 6. Test foreign key relationships:
-- SELECT t.transaction_id, t.account_id, t.card_number, a.current_balance, c.active_status
-- FROM transactions t
-- JOIN accounts a ON t.account_id = a.account_id
-- JOIN cards c ON t.card_number = c.card_number
-- WHERE t.transaction_type = '01';
--
-- 7. Test partitioning functionality:
-- SELECT tableoid::regclass as partition_name, transaction_id, processing_timestamp
-- FROM transactions 
-- ORDER BY processing_timestamp;
--
-- 8. Test transaction amount calculations (BigDecimal precision):
-- SELECT account_id, 
--        SUM(transaction_amount) as total_amount,
--        COUNT(*) as transaction_count,
--        AVG(transaction_amount) as average_amount
-- FROM transactions 
-- GROUP BY account_id
-- ORDER BY total_amount DESC;
--
-- 9. Test date range queries with partition pruning:
-- EXPLAIN (ANALYZE, BUFFERS) 
-- SELECT * FROM transactions 
-- WHERE processing_timestamp >= '2024-01-01' AND processing_timestamp < '2024-02-01';
--
-- 10. Test merchant analysis queries:
-- SELECT merchant_name, merchant_city, 
--        COUNT(*) as transaction_count,
--        SUM(transaction_amount) as total_amount
-- FROM transactions 
-- WHERE transaction_type = '01'
-- GROUP BY merchant_name, merchant_city
-- ORDER BY total_amount DESC;

-- End of changeset: 003-create-test-transactions-table