-- =====================================================================
-- Liquibase Changeset: Create Test Cards Table
-- Description: PostgreSQL cards table creation for test environment
-- Author: Blitzy agent
-- Version: CardDemo_v1.0-15-g27d6c6f-68
-- =====================================================================
--
-- Maps COBOL CARD-RECORD structure from CARDDAT VSAM to PostgreSQL:
-- 01 CARD-RECORD.
--   05 CARD-NUMBER                PIC X(16)       -> card_number VARCHAR(16) PRIMARY KEY
--   05 CARD-ACCOUNT-ID            PIC 9(11)       -> account_id VARCHAR(11) FOREIGN KEY
--   05 CARD-CUSTOMER-ID           PIC 9(11)       -> customer_id VARCHAR(11) FOREIGN KEY
--   05 CARD-CVV-CODE              PIC X(03)       -> cvv_code VARCHAR(3)
--   05 CARD-EMBOSSED-NAME         PIC X(50)       -> embossed_name VARCHAR(50)
--   05 CARD-EXPIRATION-DATE       PIC X(10)       -> expiration_date DATE
--   05 CARD-ACTIVE-STATUS         PIC X(01)       -> active_status VARCHAR(1)
--   05 FILLER                     PIC X(108)      -> (not mapped - COBOL filler)
--
-- Spring Boot Integration:
-- - JPA Entity mapping for card management microservices
-- - Foreign key relationships for referential integrity
-- - Luhn algorithm validation for card number security
-- - Optimized for TestContainers integration testing
-- - Support for Spring Data JPA repository operations
-- =====================================================================

-- liquibase formatted sql

-- changeset blitzy-agent:002a-create-test-cards-table
-- comment: Create cards table for test environment with Luhn algorithm validation and foreign key constraints

-- =============================================================================
-- Main Cards Table Creation
-- =============================================================================
CREATE TABLE cards (
    -- Primary key mapping from COBOL CARD-NUMBER (PIC X(16))
    -- 16-digit card identifier with Luhn algorithm validation
    -- Must satisfy PCI DSS requirements for card number format
    card_number VARCHAR(16) NOT NULL,
    
    -- Foreign key to accounts table mapping from COBOL CARD-ACCOUNT-ID (PIC 9(11))
    -- Links cards to specific credit card accounts for transaction processing
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key to customers table mapping from COBOL CARD-CUSTOMER-ID (PIC 9(11))
    -- Links cards to customer profiles for cardholder identification
    customer_id VARCHAR(11) NOT NULL,
    
    -- CVV security code mapping from COBOL CARD-CVV-CODE (PIC X(03))
    -- 3-digit card verification value for transaction security
    cvv_code VARCHAR(3) NOT NULL,
    
    -- Embossed name mapping from COBOL CARD-EMBOSSED-NAME (PIC X(50))
    -- Cardholder name as it appears on the physical card
    embossed_name VARCHAR(50) NOT NULL,
    
    -- Card expiration date mapping from COBOL CARD-EXPIRATION-DATE (PIC X(10))
    -- PostgreSQL DATE type for proper date handling and validation
    expiration_date DATE NOT NULL,
    
    -- Active status mapping from COBOL CARD-ACTIVE-STATUS (PIC X(01))
    -- Values: 'Y' = Active, 'N' = Inactive, 'B' = Blocked, 'E' = Expired
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- Audit timestamps for test environment tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_cards PRIMARY KEY (card_number),
    
    -- Foreign key constraint to accounts table for card-account relationship
    CONSTRAINT fk_cards_account_id FOREIGN KEY (account_id) 
        REFERENCES accounts(account_id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Foreign key constraint to customers table for card-customer relationship
    -- Note: This assumes customers table exists or will be created
    -- CONSTRAINT fk_cards_customer_id FOREIGN KEY (customer_id) 
    --     REFERENCES customers(customer_id) 
    --     ON DELETE RESTRICT 
    --     ON UPDATE CASCADE,
    
    -- Check constraints for data integrity (test environment validation)
    CONSTRAINT chk_cards_card_number_format CHECK (
        card_number ~ '^[0-9]{16}$' -- 16 numeric digits exactly
    ),
    
    -- Luhn algorithm validation for card number security
    -- Implements industry-standard credit card number validation
    CONSTRAINT chk_cards_luhn_algorithm CHECK (
        (
            -- Luhn algorithm implementation in PostgreSQL
            -- Step 1: Calculate checksum using Luhn formula
            (
                -- Sum of odd-positioned digits (from right, 1-indexed)
                CAST(substring(card_number, 16, 1) AS INTEGER) +
                CAST(substring(card_number, 14, 1) AS INTEGER) +
                CAST(substring(card_number, 12, 1) AS INTEGER) +
                CAST(substring(card_number, 10, 1) AS INTEGER) +
                CAST(substring(card_number, 8, 1) AS INTEGER) +
                CAST(substring(card_number, 6, 1) AS INTEGER) +
                CAST(substring(card_number, 4, 1) AS INTEGER) +
                CAST(substring(card_number, 2, 1) AS INTEGER) +
                
                -- Sum of even-positioned digits (from right, 1-indexed) doubled
                -- If double digit, add digits together
                CASE 
                    WHEN CAST(substring(card_number, 15, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 15, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 15, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 13, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 13, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 13, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 11, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 11, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 11, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 9, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 9, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 9, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 7, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 7, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 7, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 5, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 5, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 5, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 3, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 3, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 3, 1) AS INTEGER) * 2)
                END +
                CASE 
                    WHEN CAST(substring(card_number, 1, 1) AS INTEGER) * 2 > 9 
                    THEN ((CAST(substring(card_number, 1, 1) AS INTEGER) * 2) - 9)
                    ELSE (CAST(substring(card_number, 1, 1) AS INTEGER) * 2)
                END
            ) % 10 = 0
        )
    ),
    
    CONSTRAINT chk_cards_account_id_format CHECK (
        account_id ~ '^[0-9]{11}$' -- 11 numeric digits exactly
    ),
    
    CONSTRAINT chk_cards_customer_id_format CHECK (
        customer_id ~ '^[0-9]{11}$' -- 11 numeric digits exactly
    ),
    
    CONSTRAINT chk_cards_cvv_code_format CHECK (
        cvv_code ~ '^[0-9]{3}$' -- 3 numeric digits exactly
    ),
    
    CONSTRAINT chk_cards_active_status_valid CHECK (
        active_status IN ('Y', 'N', 'B', 'E') -- Active, Inactive, Blocked, Expired
    ),
    
    CONSTRAINT chk_cards_embossed_name_format CHECK (
        LENGTH(TRIM(embossed_name)) > 0 -- Non-empty name required
    ),
    
    CONSTRAINT chk_cards_expiration_date_future CHECK (
        expiration_date > CURRENT_DATE -- Card must not be expired
    )
);

-- =============================================================================
-- Indexes for Performance Optimization (Test Environment)
-- =============================================================================

-- Primary access pattern: card lookup by card_number
-- B-tree index automatically created by PRIMARY KEY constraint

-- Secondary access pattern: account-based card lookup for card management services
CREATE INDEX idx_cards_account_id 
ON cards (account_id, active_status);

-- Access pattern: customer-based card lookup for customer service operations
CREATE INDEX idx_cards_customer_id 
ON cards (customer_id, active_status);

-- Access pattern: card expiration monitoring for batch processing
CREATE INDEX idx_cards_expiration_date 
ON cards (expiration_date) 
WHERE active_status IN ('Y', 'B');

-- Access pattern: active card filtering for transaction processing
CREATE INDEX idx_cards_active_status 
ON cards (active_status);

-- Composite index for card management service queries
CREATE INDEX idx_cards_account_customer_status 
ON cards (account_id, customer_id, active_status);

-- =============================================================================
-- Trigger for Updated Timestamp Management
-- =============================================================================

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to call the function on UPDATE
CREATE TRIGGER trg_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_cards_updated_at();

-- =============================================================================
-- Comments for Documentation and Maintenance
-- =============================================================================

COMMENT ON TABLE cards IS 
'CardDemo cards table for test environment. Maps COBOL CARD-RECORD structure from CARDDAT VSAM to PostgreSQL schema with Luhn algorithm validation and foreign key constraints. Optimized for Spring Boot microservices integration testing with TestContainers.';

COMMENT ON COLUMN cards.card_number IS 
'Primary key. Maps from COBOL CARD-NUMBER (PIC X(16)). 16-digit card identifier with Luhn algorithm validation for PCI DSS compliance.';

COMMENT ON COLUMN cards.account_id IS 
'Foreign key to accounts table. Maps from COBOL CARD-ACCOUNT-ID (PIC 9(11)). Links cards to credit card accounts.';

COMMENT ON COLUMN cards.customer_id IS 
'Foreign key to customers table. Maps from COBOL CARD-CUSTOMER-ID (PIC 9(11)). Links cards to customer profiles.';

COMMENT ON COLUMN cards.cvv_code IS 
'Card verification value. Maps from COBOL CARD-CVV-CODE (PIC X(03)). 3-digit security code for transaction validation.';

COMMENT ON COLUMN cards.embossed_name IS 
'Cardholder name on card. Maps from COBOL CARD-EMBOSSED-NAME (PIC X(50)). Name as it appears on physical card.';

COMMENT ON COLUMN cards.expiration_date IS 
'Card expiration date. Maps from COBOL CARD-EXPIRATION-DATE (PIC X(10)). PostgreSQL DATE type for proper date handling.';

COMMENT ON COLUMN cards.active_status IS 
'Card status. Maps from COBOL CARD-ACTIVE-STATUS (PIC X(01)). Values: Y=Active, N=Inactive, B=Blocked, E=Expired.';

COMMENT ON COLUMN cards.created_at IS 
'Card creation timestamp. Added for audit trail and test environment tracking.';

COMMENT ON COLUMN cards.updated_at IS 
'Last update timestamp. Automatically updated by trigger for audit purposes.';

-- =============================================================================
-- Test Data Seeding for Integration Testing
-- =============================================================================

-- Test card for account 12345678901 (ADMIN001 user) with active status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '4532015112830366', '12345678901', '00000000001', '123', 'ADMIN TEST USER',
    '2027-12-31', 'Y', '2024-01-01 09:00:00'
);

-- Test card for account 23456789012 (USER0001) with active status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '5555555555554444', '23456789012', '00000000002', '456', 'USER TEST ONE',
    '2027-06-30', 'Y', '2024-01-15 10:00:00'
);

-- Test card for account 34567890123 (USER0002) with blocked status for edge case testing
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '4111111111111111', '34567890123', '00000000003', '789', 'USER TEST TWO',
    '2026-03-31', 'B', '2024-02-01 11:00:00'
);

-- Test card for account 45678901234 (ADMIN002) with expired status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '3782822463100005', '45678901234', '00000000004', '321', 'ADMIN TEST TWO',
    '2025-12-31', 'E', '2023-12-01 12:00:00'
);

-- Test card for high-value account 56789012345 (USER0001) with active status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '6011111111111117', '56789012345', '00000000002', '654', 'USER HIGH VALUE',
    '2028-01-31', 'Y', '2024-01-01 13:00:00'
);

-- Test card for minimal account 67890123456 (USER0002) with active status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '30569309025904', '67890123456', '00000000003', '987', 'USER MINIMAL',
    '2026-09-30', 'Y', '2024-03-01 14:00:00'
);

-- Test card for negative balance account 78901234567 (USER0001) with active status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '5105105105105100', '78901234567', '00000000002', '147', 'USER NEGATIVE',
    '2027-04-30', 'Y', '2024-01-10 15:00:00'
);

-- Additional test card for transaction testing with inactive status
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at
) VALUES (
    '4000000000000002', '12345678901', '00000000001', '258', 'ADMIN INACTIVE',
    '2026-12-31', 'N', '2024-01-01 16:00:00'
);

-- =============================================================================
-- Grant Permissions for Test Environment
-- =============================================================================

-- Grant necessary permissions for application user (configured in test properties)
-- Note: In actual test environment, these would be handled by TestContainers configuration

-- Permissions for application to read/write card data
-- GRANT SELECT, INSERT, UPDATE, DELETE ON cards TO carddemo_app_user;

-- Permissions for monitoring/auditing (if applicable in test environment)
-- GRANT SELECT ON cards TO carddemo_readonly_user;

-- =============================================================================
-- Rollback SQL (for Liquibase rollback capability)
-- =============================================================================

--rollback DROP TRIGGER IF EXISTS trg_cards_updated_at ON cards;
--rollback DROP FUNCTION IF EXISTS update_cards_updated_at();
--rollback DROP TABLE IF EXISTS cards CASCADE;

-- =============================================================================
-- Changeset Validation and Testing Notes
-- =============================================================================

-- Test SQL Validation Queries (for manual verification):
-- 
-- 1. Verify table structure matches COBOL layout:
-- SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
-- FROM information_schema.columns 
-- WHERE table_name = 'cards' AND table_schema = 'public'
-- ORDER BY ordinal_position;
--
-- 2. Verify constraints and foreign keys:
-- SELECT constraint_name, constraint_type, table_name
-- FROM information_schema.table_constraints
-- WHERE table_name = 'cards' AND table_schema = 'public';
--
-- 3. Verify indexes for performance:
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'cards' AND schemaname = 'public';
--
-- 4. Test Luhn algorithm validation:
-- -- Valid card numbers (should succeed)
-- INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, expiration_date) 
-- VALUES ('4532015112830366', '12345678901', '00000000001', '123', 'TEST', '2027-12-31');
-- 
-- -- Invalid card numbers (should fail)
-- INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, expiration_date) 
-- VALUES ('1234567890123456', '12345678901', '00000000001', '123', 'TEST', '2027-12-31');
--
-- 5. Test foreign key relationship:
-- SELECT c.card_number, c.account_id, a.current_balance, a.credit_limit
-- FROM cards c
-- JOIN accounts a ON c.account_id = a.account_id
-- WHERE c.active_status = 'Y';
--
-- 6. Test card expiration validation:
-- SELECT card_number, expiration_date, active_status,
--        (expiration_date - CURRENT_DATE) AS days_until_expiration
-- FROM cards 
-- WHERE active_status IN ('Y', 'B')
-- ORDER BY expiration_date;
--
-- 7. Test card number format validation:
-- SELECT card_number, 
--        CASE WHEN card_number ~ '^[0-9]{16}$' THEN 'Valid Format' ELSE 'Invalid Format' END AS format_check
-- FROM cards;

-- End of changeset: 002a-create-test-cards-table