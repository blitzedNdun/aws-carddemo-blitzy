-- liquibase formatted sql

-- ============================================================================
-- CardDemo Test Transactions Table Creation
-- ============================================================================
-- Purpose: Create PostgreSQL transactions table for test environment based on 
--          CVTRA05Y.cpy TRAN-RECORD structure with monthly RANGE partitioning
--          for high-volume transaction processing integration tests
-- Environment: Test environment with comprehensive transaction validation
-- Source: app/cpy/CVTRA05Y.cpy (TRAN-RECORD structure - 350 bytes)
-- Dependencies: 002-create-test-accounts-table.sql, 002a-create-test-cards-table.sql,
--               004-create-test-reference-tables.sql
-- ============================================================================

-- changeset carddemo:003-create-test-transactions-table
-- comment: Create partitioned transactions table for test environment with BigDecimal precision support
-- labels: test-environment, transaction-data, performance-validation
-- preconditions: onFail:HALT onError:HALT
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'transactions' AND table_schema = 'public';
-- expected-result: 0

-- ============================================================================
-- MAIN TRANSACTIONS TABLE WITH RANGE PARTITIONING
-- ============================================================================
-- Maps from: COBOL TRAN-RECORD in CVTRA05Y.cpy (350 bytes total)
-- Partitioning: Monthly RANGE partitioning on processing_timestamp
-- Purpose: High-volume transaction processing integration tests
-- ============================================================================

CREATE TABLE transactions (
    -- Primary key - maps to TRAN-ID from CVTRA05Y.cpy (PIC X(16))
    -- 16-character transaction identifier with unique constraint
    transaction_id VARCHAR(16) NOT NULL,
    
    -- Transaction type code - maps to TRAN-TYPE-CD (PIC X(02))
    -- 2-character transaction type code, foreign key to transaction_types
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Transaction category code - maps to TRAN-CAT-CD (PIC 9(04))
    -- 4-digit category code, foreign key to transaction_categories
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Transaction source - maps to TRAN-SOURCE (PIC X(10))
    -- Source system or channel identifier
    transaction_source VARCHAR(10) NOT NULL,
    
    -- Transaction description - maps to TRAN-DESC (PIC X(100))
    -- Descriptive text for transaction details
    transaction_description VARCHAR(100),
    
    -- Transaction amount - maps to TRAN-AMT (PIC S9(09)V99)
    -- Using DECIMAL(12,2) to match COBOL signed decimal with exact precision
    -- Supports negative amounts with exact penny precision
    transaction_amount DECIMAL(12,2) NOT NULL,
    
    -- Merchant identifier - maps to TRAN-MERCHANT-ID (PIC 9(09))
    -- 9-digit merchant identification number
    merchant_id VARCHAR(9),
    
    -- Merchant name - maps to TRAN-MERCHANT-NAME (PIC X(50))
    -- Business name where transaction occurred
    merchant_name VARCHAR(50),
    
    -- Merchant city - maps to TRAN-MERCHANT-CITY (PIC X(50))
    -- City location of merchant
    merchant_city VARCHAR(50),
    
    -- Merchant ZIP code - maps to TRAN-MERCHANT-ZIP (PIC X(10))
    -- ZIP code of merchant location
    merchant_zip VARCHAR(10),
    
    -- Card number - maps to TRAN-CARD-NUM (PIC X(16))
    -- 16-character card number, foreign key to cards table
    card_number VARCHAR(16) NOT NULL,
    
    -- Original timestamp - maps to TRAN-ORIG-TS (PIC X(26))
    -- Original transaction timestamp from source system
    original_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Processing timestamp - maps to TRAN-PROC-TS (PIC X(26))
    -- System processing timestamp for partitioning
    processing_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit fields for test environment tracking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Test environment flags
    is_test_transaction BOOLEAN DEFAULT TRUE,
    test_scenario VARCHAR(50),
    
    -- Primary key constraint
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id, processing_timestamp)
) PARTITION BY RANGE (processing_timestamp);

-- ============================================================================
-- MONTHLY RANGE PARTITIONS FOR TEST PERFORMANCE VALIDATION
-- ============================================================================
-- Purpose: Monthly RANGE partitioning strategy for test performance validation
-- Supporting: High-volume transaction processing integration tests
-- Performance: Automatic partition pruning for date-range queries
-- ============================================================================

-- Current month partition (default partition for current testing)
CREATE TABLE transactions_current PARTITION OF transactions
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Monthly partitions for comprehensive test coverage
CREATE TABLE transactions_202401 PARTITION OF transactions
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE transactions_202402 PARTITION OF transactions
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE transactions_202403 PARTITION OF transactions
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE transactions_202404 PARTITION OF transactions
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE transactions_202405 PARTITION OF transactions
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE transactions_202406 PARTITION OF transactions
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE transactions_202407 PARTITION OF transactions
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE transactions_202408 PARTITION OF transactions
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE transactions_202409 PARTITION OF transactions
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE transactions_202410 PARTITION OF transactions
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE transactions_202411 PARTITION OF transactions
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE transactions_202412 PARTITION OF transactions
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Future partition for extended testing
CREATE TABLE transactions_202501 PARTITION OF transactions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- ============================================================================
-- TEST ENVIRONMENT CONSTRAINTS AND VALIDATION
-- ============================================================================

-- Transaction ID format constraint (16 alphanumeric characters)
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_id_format 
    CHECK (LENGTH(transaction_id) = 16 AND transaction_id ~ '^[A-Za-z0-9]{16}$');

-- Transaction type format constraint (2 alphanumeric characters)
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_type_format 
    CHECK (LENGTH(transaction_type) = 2 AND transaction_type ~ '^[A-Za-z0-9]{2}$');

-- Transaction category format constraint (4 numeric characters)
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_category_format 
    CHECK (LENGTH(transaction_category) = 4 AND transaction_category ~ '^[0-9]{4}$');

-- Transaction source format constraint (up to 10 alphanumeric characters)
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_source_format 
    CHECK (LENGTH(transaction_source) <= 10 AND transaction_source ~ '^[A-Za-z0-9]+$');

-- Transaction description constraint (non-empty if provided)
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_description_content 
    CHECK (transaction_description IS NULL OR LENGTH(TRIM(transaction_description)) > 0);

-- Transaction amount precision constraint for COBOL equivalence
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_amount_precision
    CHECK (transaction_amount >= -9999999.99 AND transaction_amount <= 9999999.99);

-- Merchant ID format constraint (9 numeric characters if provided)
ALTER TABLE transactions ADD CONSTRAINT chk_merchant_id_format
    CHECK (merchant_id IS NULL OR (LENGTH(merchant_id) = 9 AND merchant_id ~ '^[0-9]{9}$'));

-- Merchant name constraint (non-empty if provided)
ALTER TABLE transactions ADD CONSTRAINT chk_merchant_name_content
    CHECK (merchant_name IS NULL OR LENGTH(TRIM(merchant_name)) > 0);

-- Merchant city constraint (non-empty if provided)
ALTER TABLE transactions ADD CONSTRAINT chk_merchant_city_content
    CHECK (merchant_city IS NULL OR LENGTH(TRIM(merchant_city)) > 0);

-- Merchant ZIP format constraint (US ZIP code format)
ALTER TABLE transactions ADD CONSTRAINT chk_merchant_zip_format
    CHECK (merchant_zip IS NULL OR merchant_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

-- Card number format constraint (16 numeric characters)
ALTER TABLE transactions ADD CONSTRAINT chk_card_number_format
    CHECK (LENGTH(card_number) = 16 AND card_number ~ '^[0-9]{16}$');

-- Timestamp logical validation (processing >= original)
ALTER TABLE transactions ADD CONSTRAINT chk_timestamp_logical_order
    CHECK (processing_timestamp >= original_timestamp);

-- Test scenario constraint for test environment
ALTER TABLE transactions ADD CONSTRAINT chk_test_scenario
    CHECK (test_scenario IS NULL OR LENGTH(test_scenario) <= 50);

-- ============================================================================
-- FOREIGN KEY CONSTRAINTS FOR TRANSACTION INTEGRITY
-- ============================================================================

-- Foreign key constraint to cards table
-- This establishes the transaction-to-card relationship for transaction processing
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_cards
    FOREIGN KEY (card_number) REFERENCES cards(card_number)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- Foreign key constraint to transaction_types table
-- This validates transaction type classification
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_transaction_types
    FOREIGN KEY (transaction_type) REFERENCES transaction_types(transaction_type)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- Foreign key constraint to transaction_categories table
-- This validates transaction category classification
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_transaction_categories
    FOREIGN KEY (transaction_type, transaction_category) 
    REFERENCES transaction_categories(transaction_type, transaction_category)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- ============================================================================
-- INDEXES FOR TRANSACTION PROCESSING SERVICE TESTING
-- ============================================================================

-- Primary key index (automatically created by PostgreSQL)
-- Composite index on (transaction_id, processing_timestamp) for partition pruning

-- Index for card-based transaction queries (frequent access pattern)
CREATE INDEX idx_transactions_card_number ON transactions (card_number, processing_timestamp);

-- Index for transaction type and category queries (business logic validation)
CREATE INDEX idx_transactions_type_category ON transactions (transaction_type, transaction_category);

-- Index for date range queries (batch processing and reporting)
CREATE INDEX idx_transactions_processing_timestamp ON transactions (processing_timestamp);

-- Index for original timestamp queries (audit and reconciliation)
CREATE INDEX idx_transactions_original_timestamp ON transactions (original_timestamp);

-- Index for amount-based queries (financial analysis)
CREATE INDEX idx_transactions_amount ON transactions (transaction_amount, processing_timestamp);

-- Index for merchant-based queries (merchant analysis)
CREATE INDEX idx_transactions_merchant_id ON transactions (merchant_id) 
    WHERE merchant_id IS NOT NULL;

-- Index for test scenario filtering in test environment
CREATE INDEX idx_transactions_test_scenario ON transactions (test_scenario) 
    WHERE test_scenario IS NOT NULL;

-- Composite index for transaction processing service testing
CREATE INDEX idx_transactions_processing_lookup ON transactions (
    card_number, transaction_type, processing_timestamp
);

-- Index for transaction source analysis
CREATE INDEX idx_transactions_source ON transactions (transaction_source, processing_timestamp);

-- ============================================================================
-- COMMENTS FOR TEST ENVIRONMENT DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE transactions IS 'Test environment transactions table for Spring Boot microservices testing. Maps COBOL TRAN-RECORD structure from CVTRA05Y.cpy to PostgreSQL with monthly RANGE partitioning for high-volume transaction processing integration tests.';

COMMENT ON COLUMN transactions.transaction_id IS 'Primary key - 16 character transaction identifier (maps to TRAN-ID PIC X(16))';
COMMENT ON COLUMN transactions.transaction_type IS 'Transaction type code - 2 characters (maps to TRAN-TYPE-CD PIC X(02))';
COMMENT ON COLUMN transactions.transaction_category IS 'Transaction category code - 4 digits (maps to TRAN-CAT-CD PIC 9(04))';
COMMENT ON COLUMN transactions.transaction_source IS 'Transaction source system - up to 10 characters (maps to TRAN-SOURCE PIC X(10))';
COMMENT ON COLUMN transactions.transaction_description IS 'Transaction description - up to 100 characters (maps to TRAN-DESC PIC X(100))';
COMMENT ON COLUMN transactions.transaction_amount IS 'Transaction amount with exact penny precision (maps to TRAN-AMT PIC S9(09)V99)';
COMMENT ON COLUMN transactions.merchant_id IS 'Merchant identifier - 9 digits (maps to TRAN-MERCHANT-ID PIC 9(09))';
COMMENT ON COLUMN transactions.merchant_name IS 'Merchant business name (maps to TRAN-MERCHANT-NAME PIC X(50))';
COMMENT ON COLUMN transactions.merchant_city IS 'Merchant city location (maps to TRAN-MERCHANT-CITY PIC X(50))';
COMMENT ON COLUMN transactions.merchant_zip IS 'Merchant ZIP code (maps to TRAN-MERCHANT-ZIP PIC X(10))';
COMMENT ON COLUMN transactions.card_number IS 'Card number - 16 digits, foreign key to cards table (maps to TRAN-CARD-NUM PIC X(16))';
COMMENT ON COLUMN transactions.original_timestamp IS 'Original transaction timestamp from source system (maps to TRAN-ORIG-TS PIC X(26))';
COMMENT ON COLUMN transactions.processing_timestamp IS 'System processing timestamp for partitioning (maps to TRAN-PROC-TS PIC X(26))';
COMMENT ON COLUMN transactions.created_at IS 'Transaction creation timestamp for audit trail';
COMMENT ON COLUMN transactions.updated_at IS 'Last update timestamp for audit trail';
COMMENT ON COLUMN transactions.is_test_transaction IS 'Test environment flag - always TRUE for test transactions';
COMMENT ON COLUMN transactions.test_scenario IS 'Test scenario identifier for integration testing';

-- ============================================================================
-- TEST DATA SEEDING FOR TRANSACTION PROCESSING SERVICE TESTING
-- ============================================================================

-- Test transaction for admin account card (4000000000000002)
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000001', '01', '0001', 'ONLINE',
    'Test grocery purchase transaction for admin account', 125.67, '123456789',
    'Test Grocery Store', 'Test City', '12345', '4000000000000002',
    '2024-01-15 10:30:00+00', '2024-01-15 10:30:15+00',
    'admin-transaction-testing'
);

-- Test transaction for user account card (4000000000000010)
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000002', '01', '0002', 'POS',
    'Test gas station purchase for user account', 45.99, '987654321',
    'Test Gas Station', 'Test Town', '67890', '4000000000000010',
    '2024-01-16 14:22:00+00', '2024-01-16 14:22:10+00',
    'user-transaction-testing'
);

-- Test payment transaction for JWT account card (4000000000000028)
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000003', '03', '0001', 'ONLINE',
    'Test payment credit for JWT account', -500.00, NULL,
    NULL, NULL, NULL, '4000000000000028',
    '2024-01-17 09:15:00+00', '2024-01-17 09:15:05+00',
    'jwt-payment-testing'
);

-- Test cash advance transaction for Spring Security account card (4000000000000036)
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000004', '02', '0001', 'ATM',
    'Test cash advance for Spring Security account', 200.00, '555666777',
    'Test ATM Location', 'Test Village', '98765', '4000000000000036',
    '2024-01-18 16:45:00+00', '2024-01-18 16:45:20+00',
    'spring-security-transaction-test'
);

-- Test fee transaction for BigDecimal precision validation
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000005', '05', '0001', 'SYSTEM',
    'Test late payment fee for precision validation', 39.99, NULL,
    NULL, NULL, NULL, '4000000000000051',
    '2024-01-19 08:00:00+00', '2024-01-19 08:00:01+00',
    'bigdecimal-precision-testing'
);

-- Test international transaction for high-risk validation
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000006', '01', '0005', 'INTL',
    'Test international purchase for risk validation', 1250.00, '444555666',
    'Test International Merchant', 'Test Foreign City', '00000', '4000000000000069',
    '2024-01-20 13:30:00+00', '2024-01-20 13:30:45+00',
    'transaction-processing-test'
);

-- Test refund transaction for transaction processing validation
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000007', '07', '0001', 'ONLINE',
    'Test merchant refund for processing validation', -75.50, '123456789',
    'Test Grocery Store', 'Test City', '12345', '4000000000000002',
    '2024-01-21 11:15:00+00', '2024-01-21 11:15:30+00',
    'refund-processing-testing'
);

-- Test high-value transaction for financial calculation testing
INSERT INTO transactions (
    transaction_id, transaction_type, transaction_category, transaction_source,
    transaction_description, transaction_amount, merchant_id, merchant_name,
    merchant_city, merchant_zip, card_number, original_timestamp,
    processing_timestamp, test_scenario
)
VALUES (
    'TXN0000000000008', '01', '0004', 'ONLINE',
    'Test high-value online purchase for calculation testing', 9999.99, '777888999',
    'Test High-Value Merchant', 'Test Metro', '33333', '4000000000000069',
    '2024-01-22 15:45:00+00', '2024-01-22 15:45:15+00',
    'financial-calculation-testing'
);

-- ============================================================================
-- TEST ENVIRONMENT VALIDATION QUERIES
-- ============================================================================

-- Validate table creation and partitioning structure
SELECT 
    schemaname,
    tablename,
    partitioned,
    hasindexes,
    hasrules,
    hastriggers
FROM pg_tables 
WHERE tablename LIKE 'transactions%'
ORDER BY tablename;

-- Validate partition pruning functionality
EXPLAIN (COSTS OFF, BUFFERS OFF)
SELECT transaction_id, transaction_amount, processing_timestamp
FROM transactions 
WHERE processing_timestamp >= '2024-01-01' 
AND processing_timestamp < '2024-02-01';

-- Validate foreign key relationships
SELECT 
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
JOIN information_schema.referential_constraints AS rc
    ON tc.constraint_name = rc.constraint_name
    AND tc.table_schema = rc.constraint_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
AND tc.table_name = 'transactions';

-- Validate indexes
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename LIKE 'transactions%'
ORDER BY tablename, indexname;

-- Validate test data insertion and decimal precision
SELECT 
    transaction_id,
    transaction_type,
    transaction_category,
    transaction_amount,
    SCALE(transaction_amount) as amount_scale,
    PRECISION(transaction_amount) as amount_precision,
    card_number,
    processing_timestamp,
    test_scenario
FROM transactions 
ORDER BY processing_timestamp;

-- Validate constraint enforcement
SELECT 
    constraint_name,
    table_name,
    constraint_type,
    check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc
    ON tc.constraint_name = cc.constraint_name
    AND tc.constraint_schema = cc.constraint_schema
WHERE tc.table_name = 'transactions'
ORDER BY constraint_type, constraint_name;

-- Validate partition distribution
SELECT 
    schemaname,
    tablename,
    COUNT(*) as row_count,
    MIN(processing_timestamp) as min_timestamp,
    MAX(processing_timestamp) as max_timestamp
FROM pg_tables pt
LEFT JOIN transactions t ON pt.tablename = 'transactions_' || TO_CHAR(t.processing_timestamp, 'YYYYMM')
WHERE pt.tablename LIKE 'transactions_%'
GROUP BY schemaname, tablename
ORDER BY tablename;

-- rollback DROP TABLE transactions CASCADE;