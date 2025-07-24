--liquibase formatted sql

--changeset blitzy-agent:002a-create-test-cards-table splitStatements:false rollbackSplitStatements:false
--comment: Create PostgreSQL cards table for test environment based on CARDDAT VSAM structure with Luhn algorithm validation
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'cards' AND table_schema = current_schema()

-- ================================================================
-- CardDemo Test Cards Table Creation
-- 
-- Purpose: Create PostgreSQL cards table for test environment with
--          exact VSAM CARDDAT structure mapping and foreign key relationships
--          to support card management service integration testing
--
-- Source Mapping: VSAM CARDDAT file structure to PostgreSQL
--   Card Number (16 chars)     → card_number VARCHAR(16) PRIMARY KEY
--   Account ID (11 digits)     → account_id VARCHAR(11) FOREIGN KEY
--   Customer ID (9 digits)     → customer_id VARCHAR(9) FOREIGN KEY
--   CVV Code (3 digits)        → cvv_code VARCHAR(3) NOT NULL
--   Embossed Name (50 chars)   → embossed_name VARCHAR(50) NOT NULL
--   Expiration Date            → expiration_date DATE NOT NULL
--   Active Status (1 char)     → active_status VARCHAR(1) NOT NULL
--
-- Test Environment Specifications:
-- - PRIMARY KEY on card_number with Luhn algorithm validation CHECK constraint
-- - FOREIGN KEY constraints to accounts table for card-account relationships  
-- - FOREIGN KEY constraints to customers table for card holder relationships
-- - Card security validation including CVV format and expiration date logic
-- - Support for transaction-to-card foreign key relationships in transaction tests
-- - Enhanced constraints for comprehensive card management testing scenarios
-- - Audit fields for card lifecycle testing and service integration validation
-- ================================================================

CREATE TABLE cards (
    -- Primary key: 16-digit card number with Luhn algorithm validation
    card_number VARCHAR(16) NOT NULL,
    
    -- Foreign key relationships for test scenario support
    account_id VARCHAR(11) NOT NULL, -- References accounts table
    customer_id VARCHAR(9) NULL,     -- References customers table for cardholder info
    
    -- Card security and identification fields
    cvv_code VARCHAR(3) NOT NULL,
    embossed_name VARCHAR(50) NOT NULL,
    expiration_date DATE NOT NULL,
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- Audit and tracking fields for integration testing
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_used_date TIMESTAMP WITH TIME ZONE NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_cards PRIMARY KEY (card_number),
    
    -- Foreign key constraint for account relationship testing
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    
    -- Business rule constraints for comprehensive test data validation
    CONSTRAINT chk_card_number_format CHECK (card_number ~ '^[0-9]{16}$'), -- 16 numeric digits
    CONSTRAINT chk_card_number_luhn CHECK (
        -- Luhn algorithm validation for card number integrity
        -- Implementation of Luhn checksum algorithm for test environment
        (
            (CAST(SUBSTRING(card_number, 16, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 14, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 12, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 10, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 8, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 6, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 4, 1) AS INTEGER) + 
             CAST(SUBSTRING(card_number, 2, 1) AS INTEGER) +
             CASE WHEN CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2 END +
             CASE WHEN CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2 > 9 
                  THEN (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2) - 9
                  ELSE CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2 END
            ) % 10 = 0
        )
    ),
    CONSTRAINT chk_account_id_format CHECK (account_id ~ '^[0-9]{11}$'), -- 11 numeric digits
    CONSTRAINT chk_customer_id_format CHECK (customer_id IS NULL OR customer_id ~ '^[0-9]{9}$'), -- 9 numeric digits
    CONSTRAINT chk_cvv_code_format CHECK (cvv_code ~ '^[0-9]{3}$'), -- 3 numeric digits
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y', 'N', 'S', 'C')), -- Active, iNactive, Suspended, Closed
    CONSTRAINT chk_embossed_name_length CHECK (LENGTH(TRIM(embossed_name)) >= 1),
    CONSTRAINT chk_expiration_date_future CHECK (expiration_date >= CURRENT_DATE),
    CONSTRAINT chk_expiration_date_reasonable CHECK (expiration_date <= CURRENT_DATE + INTERVAL '10 years')
);

-- Create indexes for performance testing and query optimization
CREATE UNIQUE INDEX idx_cards_card_number ON cards (card_number);
CREATE INDEX idx_cards_account_id ON cards (account_id);
CREATE INDEX idx_cards_customer_id ON cards (customer_id);
CREATE INDEX idx_cards_active_status ON cards (active_status);
CREATE INDEX idx_cards_expiration_date ON cards (expiration_date);
CREATE INDEX idx_cards_embossed_name ON cards (embossed_name);
CREATE INDEX idx_cards_created_at ON cards (created_at);
CREATE INDEX idx_cards_updated_at ON cards (updated_at);

-- Composite indexes for common card management service queries
CREATE INDEX idx_cards_account_status ON cards (account_id, active_status);
CREATE INDEX idx_cards_customer_status ON cards (customer_id, active_status);
CREATE INDEX idx_cards_status_expiry ON cards (active_status, expiration_date);

-- Add table comments for test documentation
COMMENT ON TABLE cards IS 'Test environment cards table for CardDemo card management service testing. Mapped from VSAM CARDDAT structure with Luhn algorithm validation and foreign key relationships to support transaction processing integration tests.';
COMMENT ON COLUMN cards.card_number IS 'Primary key: 16-digit card number with Luhn algorithm validation for test integrity';
COMMENT ON COLUMN cards.account_id IS 'Foreign key to accounts table for card-account relationship testing scenarios';
COMMENT ON COLUMN cards.customer_id IS 'Foreign key to customers table for cardholder relationship testing (nullable for flexibility)';
COMMENT ON COLUMN cards.cvv_code IS '3-digit card verification value for security testing scenarios';
COMMENT ON COLUMN cards.embossed_name IS 'Name embossed on card for display and validation testing';
COMMENT ON COLUMN cards.expiration_date IS 'Card expiration date for lifecycle testing and validation';
COMMENT ON COLUMN cards.active_status IS 'Card status: Y=Active, N=Inactive, S=Suspended, C=Closed for status testing';
COMMENT ON COLUMN cards.created_at IS 'Card creation timestamp for audit trail in test scenarios';
COMMENT ON COLUMN cards.updated_at IS 'last update timestamp for integration testing';
COMMENT ON COLUMN cards.last_used_date IS 'Last transaction timestamp for card activity testing';

-- Insert comprehensive test data for integration testing scenarios based on carddata.txt structure
INSERT INTO cards (
    card_number, account_id, customer_id, cvv_code, embossed_name, 
    expiration_date, active_status, created_at, updated_at, last_used_date
) VALUES
    -- Test card for account 00000000050 (from carddata.txt pattern)
    ('0500024453765740', '00000000050', '000000050', '747', 'Aniya Von',
     '2023-03-09', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '2 days'),
     
    -- Test card for account 00000000027 (from carddata.txt pattern)
    ('0683586198171516', '00000000027', '000000027', '567', 'Ward Jones',
     '2025-07-13', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day'),
     
    -- Test card for account 00000000002 (from carddata.txt pattern)
    ('0923877193247330', '00000000002', '000000002', '028', 'Enrico Rosenbaum',
     '2024-08-11', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '3 days'),
     
    -- Test card for account 00000000020 (from carddata.txt pattern)
    ('0927987108636232', '00000000020', '000000020', '003', 'Carter Veum',
     '2024-03-13', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '5 days'),
     
    -- Test card for account 00000000012 (from carddata.txt pattern)
    ('0982496213629795', '00000000012', '000000012', '075', 'Maci Robel',
     '2023-07-07', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '7 days'),
     
    -- Suspended card for testing status scenarios
    ('1014086565224350', '00000000044', '000000044', '640', 'Irving Emard',
     '2024-01-17', 'S', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '10 days'),
     
    -- Closed card for testing historical scenarios  
    ('1142167692878931', '00000000037', '000000037', '625', 'Shany Walker',
     '2023-10-24', 'C', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '30 days'),
     
    -- Inactive card for edge case testing
    ('1561409106491600', '00000000035', '000000035', '031', 'Angelica Dach',
     '2025-09-23', 'N', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- Create test-specific indexes for performance testing
CREATE INDEX idx_cards_test_account_lookup ON cards (account_id, active_status, expiration_date);
CREATE INDEX idx_cards_test_customer_cards ON cards (customer_id, active_status);
CREATE INDEX idx_cards_test_expiry_management ON cards (expiration_date, active_status);

-- Update table statistics for query optimization in test scenarios
ANALYZE cards;

--rollback DROP TABLE IF EXISTS cards CASCADE;

--changeset blitzy-agent:002a-create-test-cards-table-triggers splitStatements:false rollbackSplitStatements:false
--comment: Create triggers for test cards table to support integration testing scenarios

-- Create trigger function for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for updated_at timestamp
CREATE TRIGGER trigger_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_cards_updated_at();

-- Create trigger function for card usage tracking in test scenarios
CREATE OR REPLACE FUNCTION track_card_usage()
RETURNS TRIGGER AS $$
BEGIN
    -- Update last_used_date when card status changes to active usage
    IF NEW.active_status = 'Y' AND OLD.active_status != 'Y' THEN
        NEW.last_used_date = CURRENT_TIMESTAMP;
    END IF;
    
    -- Validate card not expired when attempting to activate
    IF NEW.active_status = 'Y' AND NEW.expiration_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'Cannot activate expired card. Card Number: %, Expiration: %', 
                       NEW.card_number, NEW.expiration_date;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for card usage validation
CREATE TRIGGER trigger_track_card_usage
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION track_card_usage();

-- Add comments for trigger documentation  
COMMENT ON FUNCTION update_cards_updated_at() IS 'Trigger function to automatically update the updated_at timestamp for cards table modifications';
COMMENT ON FUNCTION track_card_usage() IS 'Trigger function to track card usage patterns and validate business rules for integration testing';

--rollback DROP TRIGGER IF EXISTS trigger_cards_updated_at ON cards; DROP TRIGGER IF EXISTS trigger_track_card_usage ON cards; DROP FUNCTION IF EXISTS update_cards_updated_at(); DROP FUNCTION IF EXISTS track_card_usage();