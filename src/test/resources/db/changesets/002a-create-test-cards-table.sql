-- liquibase formatted sql

-- ============================================================================
-- CardDemo Test Cards Table Creation
-- ============================================================================
-- Purpose: Create PostgreSQL cards table for test environment based on 
--          CARDDAT VSAM structure with Luhn algorithm validation and foreign 
--          key relationships to support transaction processing integration tests
-- Environment: Test environment with comprehensive card management validation
-- Source: app/data/ASCII/carddata.txt (CARDDAT VSAM structure)
-- Dependencies: 002-create-test-accounts-table.sql (accounts table must exist)
-- ============================================================================

-- changeset carddemo:002a-create-test-cards-table
-- comment: Create cards table for test environment with Luhn algorithm validation
-- labels: test-environment, card-management, security-validation
-- preconditions: onFail:HALT onError:HALT
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'cards' AND table_schema = 'public';
-- expected-result: 0

CREATE TABLE cards (
    -- Primary key - 16-digit card number with Luhn algorithm validation
    -- Maps to CARDDAT VSAM key structure (16 characters)
    card_number VARCHAR(16) NOT NULL,
    
    -- Account association - foreign key to accounts table
    -- Maps to CARDDAT account relationship (11 digits)
    account_id VARCHAR(11) NOT NULL,
    
    -- Customer association - foreign key for card ownership
    -- Maps to CARDDAT customer relationship (9 digits)
    customer_id VARCHAR(9) NOT NULL,
    
    -- Card security information
    -- CVV code for transaction authorization (3 digits)
    cvv_code VARCHAR(3) NOT NULL,
    
    -- Cardholder name embossed on card
    -- Maps to CARDDAT embossed name field (up to 50 characters)
    embossed_name VARCHAR(50) NOT NULL,
    
    -- Card expiration date - critical for transaction processing
    -- Maps to CARDDAT expiration date field
    expiration_date DATE NOT NULL,
    
    -- Card status - maps to CARDDAT active status field
    -- 'Y' = Active, 'N' = Inactive, 'B' = Blocked, 'E' = Expired
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- Audit fields for test environment tracking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Test environment flags
    is_test_card BOOLEAN DEFAULT TRUE,
    test_scenario VARCHAR(50),
    
    -- Primary key constraint
    CONSTRAINT pk_cards PRIMARY KEY (card_number)
);

-- ============================================================================
-- Test Environment Constraints and Validation
-- ============================================================================

-- Card number format constraint (16 numeric characters)
ALTER TABLE cards ADD CONSTRAINT chk_card_number_format 
    CHECK (LENGTH(card_number) = 16 AND card_number ~ '^[0-9]{16}$');

-- Luhn algorithm validation for card numbers as specified in Security Architecture 6.4
-- This constraint ensures all card numbers pass Luhn checksum validation
ALTER TABLE cards ADD CONSTRAINT chk_card_number_luhn
    CHECK (
        -- Luhn algorithm implementation for PostgreSQL
        -- Sum of odd-positioned digits (from right) + sum of even-positioned digits (doubled, with digit sum)
        (
            -- Odd positions (1st, 3rd, 5th, etc. from right)
            CAST(SUBSTRING(card_number, 16, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 14, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 12, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 10, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 8, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 6, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 4, 1) AS INTEGER) +
            CAST(SUBSTRING(card_number, 2, 1) AS INTEGER) +
            
            -- Even positions (2nd, 4th, 6th, etc. from right) - doubled and digit sum calculated
            CASE 
                WHEN CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2) - 9
            END +
            CASE 
                WHEN CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2 <= 9 
                THEN CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2
                ELSE (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2) - 9
            END
        ) % 10 = 0
    );

-- Account ID format constraint (11 numeric characters)
ALTER TABLE cards ADD CONSTRAINT chk_card_account_id_format
    CHECK (LENGTH(account_id) = 11 AND account_id ~ '^[0-9]{11}$');

-- Customer ID format constraint (9 numeric characters)
ALTER TABLE cards ADD CONSTRAINT chk_card_customer_id_format
    CHECK (LENGTH(customer_id) = 9 AND customer_id ~ '^[0-9]{9}$');

-- CVV code format constraint (3 numeric characters)
ALTER TABLE cards ADD CONSTRAINT chk_cvv_code_format
    CHECK (LENGTH(cvv_code) = 3 AND cvv_code ~ '^[0-9]{3}$');

-- Embossed name constraint (non-empty, valid characters)
ALTER TABLE cards ADD CONSTRAINT chk_embossed_name_format
    CHECK (LENGTH(TRIM(embossed_name)) > 0 AND LENGTH(embossed_name) <= 50);

-- Card status constraint (valid status codes)
ALTER TABLE cards ADD CONSTRAINT chk_card_status
    CHECK (active_status IN ('Y', 'N', 'B', 'E'));

-- Expiration date constraint (must be in future for active cards)
ALTER TABLE cards ADD CONSTRAINT chk_expiration_date_future
    CHECK (expiration_date > CURRENT_DATE);

-- Test scenario constraint for test environment
ALTER TABLE cards ADD CONSTRAINT chk_test_scenario
    CHECK (test_scenario IS NULL OR LENGTH(test_scenario) <= 50);

-- ============================================================================
-- Foreign Key Constraints for Card-Account Relationships
-- ============================================================================

-- Foreign key constraint to accounts table
-- This establishes the card-to-account relationship for transaction processing
ALTER TABLE cards ADD CONSTRAINT fk_cards_accounts
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ============================================================================
-- Indexes for Card Management Service Testing
-- ============================================================================

-- Unique index on card_number for primary key performance
CREATE UNIQUE INDEX idx_cards_card_number ON cards (card_number);

-- Index for account-based card lookups (frequently used in card services)
CREATE INDEX idx_cards_account_id ON cards (account_id);

-- Index for customer-based card lookups (support multiple cards per customer)
CREATE INDEX idx_cards_customer_id ON cards (customer_id);

-- Index for active status filtering (frequently used in card queries)
CREATE INDEX idx_cards_active_status ON cards (active_status);

-- Index for expiration date queries (card renewal processing)
CREATE INDEX idx_cards_expiration_date ON cards (expiration_date);

-- Index for test scenario filtering in test environment
CREATE INDEX idx_cards_test_scenario ON cards (test_scenario) 
    WHERE test_scenario IS NOT NULL;

-- Composite index for card management service testing
CREATE INDEX idx_cards_management_lookup ON cards (account_id, active_status, expiration_date);

-- Index for security validation (CVV and status combination)
CREATE INDEX idx_cards_security_validation ON cards (card_number, cvv_code, active_status);

-- ============================================================================
-- Comments for Test Environment Documentation
-- ============================================================================

COMMENT ON TABLE cards IS 'Test environment cards table for Spring Boot microservices testing. Maps CARDDAT VSAM structure with Luhn algorithm validation and foreign key relationships to accounts table for transaction processing integration tests.';

COMMENT ON COLUMN cards.card_number IS 'Primary key - 16 digit card number with Luhn algorithm validation (maps to CARDDAT key structure)';
COMMENT ON COLUMN cards.account_id IS 'Foreign key to accounts table - establishes card-account relationship (11 digits)';
COMMENT ON COLUMN cards.customer_id IS 'Customer identifier for card ownership (9 digits)';
COMMENT ON COLUMN cards.cvv_code IS 'Card verification value for transaction authorization (3 digits)';
COMMENT ON COLUMN cards.embossed_name IS 'Cardholder name embossed on physical card (up to 50 characters)';
COMMENT ON COLUMN cards.expiration_date IS 'Card expiration date - critical for transaction processing validation';
COMMENT ON COLUMN cards.active_status IS 'Card status - Y=Active, N=Inactive, B=Blocked, E=Expired';
COMMENT ON COLUMN cards.created_at IS 'Card creation timestamp for audit trail';
COMMENT ON COLUMN cards.updated_at IS 'Last update timestamp for audit trail';
COMMENT ON COLUMN cards.is_test_card IS 'Test environment flag - always TRUE for test cards';
COMMENT ON COLUMN cards.test_scenario IS 'Test scenario identifier for integration testing';

-- ============================================================================
-- Test Data Seeding for Card Management Service Testing
-- ============================================================================

-- Test card for admin account (linked to account 10000000001)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000002', '10000000001', '000000001', '123', 'ADMIN TEST USER',
    '2025-12-31', 'Y', 'admin-card-testing'
);

-- Test card for user account (linked to account 10000000002)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000010', '10000000002', '000000002', '456', 'USER TEST ACCOUNT',
    '2025-11-30', 'Y', 'user-card-testing'
);

-- Test card for JWT testing (linked to account 10000000003)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000028', '10000000003', '000000003', '789', 'JWT TOKEN TEST',
    '2025-10-31', 'Y', 'jwt-card-testing'
);

-- Test card for Spring Security testing (linked to account 10000000004)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000036', '10000000004', '000000004', '321', 'SPRING SECURITY TEST',
    '2025-09-30', 'Y', 'spring-security-card-test'
);

-- Test card for role-based access control (linked to closed account 10000000005)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000044', '10000000005', '000000005', '654', 'RBAC CLOSED TEST',
    '2025-08-31', 'N', 'rbac-card-testing'
);

-- Test card for Luhn algorithm validation (linked to account 10000000006)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000051', '10000000006', '000000006', '987', 'LUHN VALIDATION TEST',
    '2025-07-31', 'Y', 'luhn-algorithm-testing'
);

-- Test card for transaction processing (linked to account 10000000007)
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000069', '10000000007', '000000007', '147', 'TRANSACTION PROCESSING',
    '2025-06-30', 'Y', 'transaction-processing-test'
);

-- Test card for blocked status validation
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name,
    expiration_date, active_status, test_scenario
)
VALUES (
    '4000000000000077', '10000000001', '000000001', '258', 'BLOCKED CARD TEST',
    '2025-05-31', 'B', 'blocked-card-testing'
);

-- ============================================================================
-- Test Environment Validation Queries
-- ============================================================================

-- Validate table creation and structure
SELECT 
    table_name, 
    column_name, 
    data_type, 
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'cards' 
ORDER BY ordinal_position;

-- Validate constraints
SELECT 
    constraint_name, 
    constraint_type, 
    table_name
FROM information_schema.table_constraints 
WHERE table_name = 'cards';

-- Validate foreign key relationships
SELECT 
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.key_column_usage AS kcu
JOIN information_schema.referential_constraints AS rc
    ON kcu.constraint_name = rc.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON rc.unique_constraint_name = ccu.constraint_name
WHERE kcu.table_name = 'cards'
AND kcu.constraint_name LIKE 'fk_%';

-- Validate indexes
SELECT 
    indexname, 
    indexdef
FROM pg_indexes 
WHERE tablename = 'cards';

-- Validate test data insertion and card number format
SELECT 
    card_number,
    account_id,
    customer_id,
    cvv_code,
    embossed_name,
    expiration_date,
    active_status,
    test_scenario,
    created_at
FROM cards 
ORDER BY card_number;

-- Validate Luhn algorithm constraint enforcement
SELECT 
    card_number,
    CASE 
        WHEN (
            -- Simplified validation check for display
            LENGTH(card_number) = 16 
            AND card_number ~ '^[0-9]{16}$'
        ) THEN 'Valid Format'
        ELSE 'Invalid Format'
    END as format_validation,
    active_status,
    test_scenario
FROM cards
WHERE test_scenario LIKE '%luhn%';

-- Validate constraint enforcement
SELECT 
    constraint_name,
    check_clause
FROM information_schema.check_constraints
WHERE constraint_name LIKE 'chk_%'
AND constraint_schema = 'public'
AND constraint_name LIKE '%card%';

-- Validate foreign key relationships with accounts table
SELECT 
    c.card_number,
    c.account_id,
    a.account_id as account_exists,
    a.active_status as account_status,
    c.active_status as card_status,
    c.test_scenario
FROM cards c
LEFT JOIN accounts a ON c.account_id = a.account_id
ORDER BY c.card_number;

-- rollback DROP TABLE cards CASCADE;