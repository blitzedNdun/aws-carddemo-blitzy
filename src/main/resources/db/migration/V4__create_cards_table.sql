-- ============================================================================
-- Liquibase Migration: V4__create_cards_table.sql
-- Description: Create cards table migrated from VSAM CARDDAT dataset
-- Author: Blitzy agent
-- Version: 4.0
-- Dependencies: V2__create_customers_table.sql, V3__create_accounts_table.sql
-- ============================================================================

-- Create cards table with comprehensive field mapping from CARDDAT VSAM dataset
-- Implements security validation, composite foreign key relationships, and integrated cross-reference functionality
CREATE TABLE cards (
    -- Primary identifier: 16-digit card number with Luhn algorithm validation
    card_number VARCHAR(16) NOT NULL,
    
    -- Foreign key reference to accounts table establishing card-to-account relationship
    account_id VARCHAR(11) NOT NULL,
    
    -- Foreign key reference to customers table establishing card-to-customer relationship
    customer_id VARCHAR(9) NOT NULL,
    
    -- CVV code field with secure storage requirements and appropriate length constraints
    cvv_code VARCHAR(3) NOT NULL,
    
    -- Embossed name field for cardholder name storage with proper length validation
    embossed_name VARCHAR(50) NOT NULL,
    
    -- Card expiration date for lifecycle management
    expiration_date DATE NOT NULL,
    
    -- Active status field for lifecycle management (Y/N -> TRUE/FALSE)
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields for tracking record lifecycle
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_cards PRIMARY KEY (card_number)
);

-- Create foreign key constraints for referential integrity
-- Foreign key constraint to accounts table maintaining referential integrity
ALTER TABLE cards ADD CONSTRAINT fk_cards_account_id 
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Foreign key constraint to customers table maintaining referential integrity  
ALTER TABLE cards ADD CONSTRAINT fk_cards_customer_id 
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create check constraints for business rule validation
-- Card number must be exactly 16 numeric digits with Luhn algorithm validation
ALTER TABLE cards ADD CONSTRAINT chk_cards_number_format 
    CHECK (card_number ~ '^[0-9]{16}$');

-- Luhn algorithm validation for card number data integrity
-- This constraint implements the Luhn checksum algorithm for credit card validation
ALTER TABLE cards ADD CONSTRAINT chk_cards_luhn_algorithm 
    CHECK (
        (
            -- Calculate Luhn checksum for 16-digit card number
            (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 2, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 4, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 6, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 8, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 10, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 12, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 14, 1) AS INTEGER) +
            (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2 % 10) +
            (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2 / 10) +
            CAST(SUBSTRING(card_number, 16, 1) AS INTEGER)
        ) % 10 = 0
    );

-- CVV code must be exactly 3 numeric digits
ALTER TABLE cards ADD CONSTRAINT chk_cards_cvv_format 
    CHECK (cvv_code ~ '^[0-9]{3}$');

-- Embossed name cannot be empty string
ALTER TABLE cards ADD CONSTRAINT chk_cards_embossed_name_not_empty 
    CHECK (LENGTH(TRIM(embossed_name)) > 0);

-- Expiration date must be in the future (card must be valid)
ALTER TABLE cards ADD CONSTRAINT chk_cards_expiration_future 
    CHECK (expiration_date > CURRENT_DATE);

-- Expiration date must be within reasonable range (not too far in future)
ALTER TABLE cards ADD CONSTRAINT chk_cards_expiration_range 
    CHECK (expiration_date <= CURRENT_DATE + INTERVAL '10 years');

-- Composite foreign key constraint ensuring card-account-customer relationship integrity
-- This constraint validates that the account_id and customer_id combination exists in accounts table
ALTER TABLE cards ADD CONSTRAINT chk_cards_account_customer_relationship 
    CHECK (
        EXISTS (
            SELECT 1 FROM accounts 
            WHERE accounts.account_id = cards.account_id 
            AND accounts.customer_id = cards.customer_id
        )
    );

-- Create indexes for performance optimization and cross-reference functionality
-- Primary index on card_number is automatically created with PRIMARY KEY

-- Index for account-based card lookup (replicating CARDAIX functionality)
-- This index supports rapid lookup operations for account-based card queries
CREATE INDEX idx_cards_account_id ON cards (account_id, active_status);

-- Index for customer-based card lookup supporting customer card portfolio queries
CREATE INDEX idx_cards_customer_id ON cards (customer_id, active_status);

-- Index for card expiration date queries supporting lifecycle management
CREATE INDEX idx_cards_expiration_date ON cards (expiration_date, active_status);

-- Index for active card status queries supporting operational filtering
CREATE INDEX idx_cards_active_status ON cards (active_status, expiration_date);

-- Composite index for customer-account cross-reference queries (cardxref.txt functionality)
-- This index supports the cross-reference functionality from cardxref.txt data
CREATE INDEX idx_cards_customer_account_xref ON cards (customer_id, account_id, card_number);

-- Index for embossed name searches supporting customer service operations
CREATE INDEX idx_cards_embossed_name ON cards (embossed_name, active_status);

-- Partial index for active cards only (performance optimization)
CREATE INDEX idx_cards_active_only ON cards (card_number, account_id, customer_id) 
    WHERE active_status = TRUE;

-- Create trigger for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_cards_updated_at();

-- Create trigger for card lifecycle validation on updates
CREATE OR REPLACE FUNCTION validate_card_lifecycle_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Prevent activation of expired cards
    IF NEW.active_status = TRUE AND NEW.expiration_date <= CURRENT_DATE THEN
        RAISE EXCEPTION 'Cannot activate expired card: Card % expires on %', 
            NEW.card_number, NEW.expiration_date;
    END IF;
    
    -- Validate account-customer relationship consistency
    IF NOT EXISTS (
        SELECT 1 FROM accounts 
        WHERE accounts.account_id = NEW.account_id 
        AND accounts.customer_id = NEW.customer_id
    ) THEN
        RAISE EXCEPTION 'Invalid account-customer relationship for card %: Account % does not belong to customer %', 
            NEW.card_number, NEW.account_id, NEW.customer_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cards_lifecycle_validation
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION validate_card_lifecycle_update();

-- Create trigger for insert validation ensuring data integrity
CREATE OR REPLACE FUNCTION validate_card_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate account-customer relationship on insert
    IF NOT EXISTS (
        SELECT 1 FROM accounts 
        WHERE accounts.account_id = NEW.account_id 
        AND accounts.customer_id = NEW.customer_id
    ) THEN
        RAISE EXCEPTION 'Invalid account-customer relationship for new card %: Account % does not belong to customer %', 
            NEW.card_number, NEW.account_id, NEW.customer_id;
    END IF;
    
    -- Validate account is active
    IF NOT EXISTS (
        SELECT 1 FROM accounts 
        WHERE accounts.account_id = NEW.account_id 
        AND accounts.active_status = TRUE
    ) THEN
        RAISE EXCEPTION 'Cannot create card for inactive account %', NEW.account_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cards_insert_validation
    BEFORE INSERT ON cards
    FOR EACH ROW
    EXECUTE FUNCTION validate_card_insert();

-- Add table and column comments for documentation
COMMENT ON TABLE cards IS 'Credit card master data migrated from VSAM CARDDAT dataset with security validation, composite foreign key relationships, and integrated cross-reference functionality for comprehensive card management';

COMMENT ON COLUMN cards.card_number IS 'Primary key: 16-digit card number with Luhn algorithm validation for data integrity';
COMMENT ON COLUMN cards.account_id IS 'Foreign key reference to accounts table establishing card-to-account relationship';
COMMENT ON COLUMN cards.customer_id IS 'Foreign key reference to customers table establishing card-to-customer relationship';
COMMENT ON COLUMN cards.cvv_code IS '3-digit card verification value for security validation and fraud prevention';
COMMENT ON COLUMN cards.embossed_name IS 'Cardholder name as embossed on physical card, maximum 50 characters';
COMMENT ON COLUMN cards.expiration_date IS 'Card expiration date for lifecycle management and security validation';
COMMENT ON COLUMN cards.active_status IS 'Card active status for lifecycle management (TRUE=Active, FALSE=Inactive)';
COMMENT ON COLUMN cards.created_at IS 'Record creation timestamp with timezone';
COMMENT ON COLUMN cards.updated_at IS 'Record last modification timestamp with timezone, automatically updated';

-- Grant appropriate permissions for application access
-- Note: Specific role permissions should be configured based on deployment environment
-- Example permissions (adjust based on actual role names in deployment):
-- GRANT SELECT, INSERT, UPDATE ON cards TO carddemo_app_role;
-- GRANT SELECT ON cards TO carddemo_read_role;
-- GRANT ALL PRIVILEGES ON cards TO carddemo_admin_role;

-- Create row-level security policy for card data access
-- Enable row-level security for the cards table
-- ALTER TABLE cards ENABLE ROW LEVEL SECURITY;

-- Example RLS policy (uncomment and adjust based on security requirements):
-- CREATE POLICY cards_access_policy ON cards
--     FOR ALL TO carddemo_app_role
--     USING (customer_id = current_setting('app.current_customer_id', true));

-- Security note: For production deployment, implement additional security measures:
-- 1. Column-level encryption for CVV codes using pgcrypto extension
-- 2. Row-level security policies for customer data isolation
-- 3. Audit triggers for sensitive card data access logging
-- 4. Regular security reviews and PCI DSS compliance audits
-- 5. CVV code field should be encrypted at rest and in transit

-- Performance optimization notes:
-- 1. Monitor query patterns for card-account-customer joins
-- 2. Consider partitioning by customer_id for large datasets
-- 3. Regular VACUUM and ANALYZE operations for optimal performance
-- 4. Configure appropriate PostgreSQL work_mem for complex queries

-- Migration validation notes:
-- 1. Verify all card data from carddata.txt loads correctly
-- 2. Validate cardxref.txt cross-reference functionality through indexes
-- 3. Test Luhn algorithm validation with sample card numbers
-- 4. Confirm foreign key relationships with accounts and customers tables
-- 5. Test card lifecycle management through active_status updates

-- Cross-reference functionality notes:
-- 1. idx_cards_customer_account_xref index provides cardxref.txt equivalent functionality
-- 2. Composite foreign key constraints ensure referential integrity
-- 3. Triggers validate account-customer relationship consistency
-- 4. Rapid lookup operations supported through optimized B-tree indexes

-- Rollback instructions:
-- To rollback this migration:
-- 1. DROP TABLE cards CASCADE;
-- 2. DROP FUNCTION update_cards_updated_at() CASCADE;
-- 3. DROP FUNCTION validate_card_lifecycle_update() CASCADE;
-- 4. DROP FUNCTION validate_card_insert() CASCADE;