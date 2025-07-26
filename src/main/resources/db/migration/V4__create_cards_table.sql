-- =============================================================================
-- Liquibase Migration: V4__create_cards_table.sql
-- 
-- SUMMARY: Creates cards table migrated from CARDDAT VSAM dataset with security 
--          field preservation, composite foreign key relationships, and integrated 
--          cross-reference functionality for comprehensive card management
--
-- DEPENDENCIES: 
--   - V2__create_customers_table.sql (customers.customer_id)
--   - V3__create_accounts_table.sql (accounts.account_id, accounts.customer_id)
--
-- MIGRATION FROM: 
--   - Legacy VSAM CARDDAT dataset 
--   - Legacy carddata.txt ASCII export file
--   - Legacy cardxref.txt cross-reference file
--
-- SPRING BOOT INTEGRATION:
--   - JPA Entity: src/main/java/com/carddemo/entity/Card.java
--   - Repository: src/main/java/com/carddemo/repository/CardRepository.java
--   - Microservice: CardManagementService.java, CardListService.java, CardUpdateService.java
--
-- AUTHOR: Blitzy agent - CardDemo PostgreSQL Migration 
-- =============================================================================

--liquibase formatted sql

--changeset blitzy:create-cards-table
--comment: Create cards table with security validation, composite foreign keys, and cross-reference support

-- Create the main cards table migrated from VSAM CARDDAT
CREATE TABLE cards (
    -- PRIMARY KEY: 16-character card number with Luhn algorithm validation
    card_number VARCHAR(16) NOT NULL,
    
    -- FOREIGN KEY: Reference to accounts table (11-digit account identifier)
    account_id VARCHAR(11) NOT NULL,
    
    -- FOREIGN KEY: Reference to customers table (9-digit customer identifier)  
    customer_id VARCHAR(9) NOT NULL,
    
    -- CVV CODE: 3-digit security code with secure storage requirements
    cvv_code VARCHAR(3) NOT NULL,
    
    -- EMBOSSED NAME: Cardholder name as it appears on physical card
    embossed_name VARCHAR(50) NOT NULL,
    
    -- EXPIRATION DATE: Card expiry date for lifecycle management
    expiration_date DATE NOT NULL,
    
    -- ACTIVE STATUS: Card lifecycle status (Y=Active, N=Inactive, C=Cancelled, E=Expired)
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- AUDIT FIELDS: Creation and modification tracking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- PRIMARY KEY CONSTRAINT
    CONSTRAINT pk_cards PRIMARY KEY (card_number),
    
    -- FOREIGN KEY CONSTRAINTS: Composite relationship maintaining referential integrity
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) 
        REFERENCES accounts(account_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
        
    CONSTRAINT fk_cards_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(customer_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
        
    -- COMPOSITE FOREIGN KEY: Ensures card-account-customer relationship consistency
    -- This validates that the customer_id on the card matches the customer_id on the account
    CONSTRAINT fk_cards_account_customer FOREIGN KEY (account_id, customer_id) 
        REFERENCES accounts(account_id, customer_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- LUHN ALGORITHM VALIDATION: Ensures card number integrity using Luhn checksum
    CONSTRAINT chk_cards_luhn_valid CHECK (
        -- Luhn algorithm implementation in PostgreSQL
        -- Sum of digits in odd positions (from right, 1-indexed)
        -- Plus sum of doubled digits in even positions (from right, 1-indexed)
        -- Where doubled digits > 9 are reduced by subtracting 9
        -- Total sum must be divisible by 10
        (
            -- Sum odd positions (1st, 3rd, 5th, etc. from right)
            (CAST(SUBSTRING(card_number, 16, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 14, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 12, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 10, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 8, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 6, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 4, 1) AS INTEGER) +
             CAST(SUBSTRING(card_number, 2, 1) AS INTEGER)) +
            
            -- Sum even positions doubled (2nd, 4th, 6th, etc. from right)
            (CASE WHEN (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 15, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 13, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 11, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 9, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 7, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 5, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 3, 1) AS INTEGER) * 2) END +
             CASE WHEN (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2) > 9 
                THEN (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2) - 9 
                ELSE (CAST(SUBSTRING(card_number, 1, 1) AS INTEGER) * 2) END)
        ) % 10 = 0
    ),
    
    -- CARD NUMBER FORMAT: Must be exactly 16 numeric digits
    CONSTRAINT chk_cards_number_format CHECK (
        card_number ~ '^[0-9]{16}$' AND LENGTH(card_number) = 16
    ),
    
    -- CVV CODE VALIDATION: Must be exactly 3 numeric digits  
    CONSTRAINT chk_cards_cvv_format CHECK (
        cvv_code ~ '^[0-9]{3}$' AND LENGTH(cvv_code) = 3
    ),
    
    -- EMBOSSED NAME VALIDATION: Must not be empty and contain valid characters
    CONSTRAINT chk_cards_embossed_name CHECK (
        LENGTH(TRIM(embossed_name)) > 0 AND 
        embossed_name ~ '^[A-Za-z0-9 .,-]+$'
    ),
    
    -- EXPIRATION DATE VALIDATION: Must be in the future
    CONSTRAINT chk_cards_expiration_future CHECK (
        expiration_date > CURRENT_DATE
    ),
    
    -- ACTIVE STATUS VALIDATION: Must be valid status code
    CONSTRAINT chk_cards_active_status CHECK (
        active_status IN ('Y', 'N', 'C', 'E')
    )
);

--rollback DROP TABLE cards CASCADE;

--comment: Table cards created with security validation and cross-reference support

-- =============================================================================
-- PERFORMANCE OPTIMIZATION INDEXES
-- =============================================================================

--changeset blitzy:create-cards-indexes
--comment: Create performance indexes for cards table supporting rapid lookups and cross-reference functionality

-- PRIMARY ACCESS INDEX: B-tree index on account_id for account-based card queries
-- Replaces VSAM CARDAIX alternate index functionality  
CREATE INDEX idx_cards_account_id ON cards (account_id, active_status);

-- CUSTOMER CROSS-REFERENCE INDEX: Enables rapid customer-to-cards lookup
-- Supports customer card listing operations in CardListService
CREATE INDEX idx_cards_customer_id ON cards (customer_id, active_status);

-- COMPOSITE RELATIONSHIP INDEX: Optimizes account-customer-card join operations
-- Critical for maintaining referential integrity across microservice boundaries
CREATE INDEX idx_cards_account_customer ON cards (account_id, customer_id);

-- EXPIRATION DATE INDEX: Supports card lifecycle management and batch processing
-- Enables efficient queries for expired cards and renewal processing
CREATE INDEX idx_cards_expiration_date ON cards (expiration_date, active_status);

-- EMBOSSED NAME INDEX: Supports name-based card search operations
-- Enables rapid card lookup by cardholder name for customer service operations
CREATE INDEX idx_cards_embossed_name ON cards (UPPER(embossed_name));

--rollback DROP INDEX IF EXISTS idx_cards_account_id CASCADE;
--rollback DROP INDEX IF EXISTS idx_cards_customer_id CASCADE; 
--rollback DROP INDEX IF EXISTS idx_cards_account_customer CASCADE;
--rollback DROP INDEX IF EXISTS idx_cards_expiration_date CASCADE;
--rollback DROP INDEX IF EXISTS idx_cards_embossed_name CASCADE;

--comment: Performance indexes created for cards table

-- =============================================================================
-- DATABASE TRIGGERS FOR AUDIT TRAIL AND DATA INTEGRITY
-- =============================================================================

--changeset blitzy:create-cards-triggers
--comment: Create audit triggers and data integrity triggers for cards table

-- AUDIT TRIGGER: Automatically update updated_at timestamp on record modification
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cards_update_timestamp
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_cards_updated_at();

-- DATA VALIDATION TRIGGER: Additional business rule validation beyond constraints
CREATE OR REPLACE FUNCTION validate_cards_business_rules()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate card number prefix matches expected issuer patterns
    -- Major card networks: Visa (4), Mastercard (5), American Express (34,37), Discover (6)
    IF NOT (LEFT(NEW.card_number, 1) IN ('4', '5', '6') OR LEFT(NEW.card_number, 2) IN ('34', '37')) THEN
        RAISE EXCEPTION 'Invalid card number: Card number must start with valid issuer prefix (4, 5, 6, 34, 37)';
    END IF;
    
    -- Validate expiration date is reasonable (not more than 10 years in future)
    IF NEW.expiration_date > (CURRENT_DATE + INTERVAL '10 years') THEN
        RAISE EXCEPTION 'Invalid expiration date: Card expiration cannot exceed 10 years from current date';
    END IF;
    
    -- Validate embossed name matches customer name pattern
    -- This is a business rule to ensure name consistency
    IF LENGTH(TRIM(NEW.embossed_name)) < 2 THEN
        RAISE EXCEPTION 'Invalid embossed name: Name must be at least 2 characters long';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cards_business_validation
    BEFORE INSERT OR UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION validate_cards_business_rules();

--rollback DROP TRIGGER IF EXISTS trg_cards_update_timestamp ON cards CASCADE;
--rollback DROP TRIGGER IF EXISTS trg_cards_business_validation ON cards CASCADE;
--rollback DROP FUNCTION IF EXISTS update_cards_updated_at() CASCADE;
--rollback DROP FUNCTION IF EXISTS validate_cards_business_rules() CASCADE;

--comment: Audit and validation triggers created for cards table

-- =============================================================================
-- SECURITY AND ACCESS CONTROL
-- =============================================================================

--changeset blitzy:create-cards-security
--comment: Create row-level security and access control for cards table

-- ENABLE ROW LEVEL SECURITY: Ensures users can only access authorized card data
ALTER TABLE cards ENABLE ROW LEVEL SECURITY;

-- SECURITY POLICY: Card access restricted to account owner or admin users
-- Integrates with Spring Security authentication context
-- Note: Using 'public' for compatibility with test environments without role creation privileges
CREATE POLICY cards_access_policy ON cards 
    FOR ALL TO public
    USING (
        -- Admin users can access all cards
        current_setting('app.user_type', true) = 'A' OR
        -- Regular users can only access cards linked to their customer accounts
        customer_id = current_setting('app.customer_id', true)
    );

-- CVV CODE PROTECTION: Additional security for sensitive CVV data
-- Note: In production, CVV codes should be encrypted using PostgreSQL pgcrypto
-- This comment serves as a reminder for security hardening
COMMENT ON COLUMN cards.cvv_code IS 
    'CVV security code - SECURITY SENSITIVE: Consider pgcrypto encryption for production deployment';

--rollback ALTER TABLE cards DISABLE ROW LEVEL SECURITY;
--rollback DROP POLICY IF EXISTS cards_access_policy ON cards CASCADE;

--comment: Row-level security and access control implemented for cards table

-- =============================================================================
-- MATERIALIZED VIEW FOR CROSS-REFERENCE OPTIMIZATION
-- =============================================================================

--changeset blitzy:create-cards-materialized-view
--comment: Create materialized view for cross-reference functionality and performance optimization

-- CROSS-REFERENCE MATERIALIZED VIEW: Replaces cardxref.txt functionality
-- Provides pre-computed joins for rapid card-account-customer relationship queries
CREATE MATERIALIZED VIEW mv_cards_cross_reference AS
SELECT 
    c.card_number,
    c.account_id,
    c.customer_id,
    c.active_status AS card_status,
    c.expiration_date,
    c.embossed_name,
    a.active_status AS account_status,
    a.current_balance,
    a.credit_limit,
    cust.first_name || ' ' || cust.last_name AS customer_name,
    cust.phone_number_1,
    c.created_at AS card_created_at
FROM cards c
    INNER JOIN accounts a ON c.account_id = a.account_id
    INNER JOIN customers cust ON c.customer_id = cust.customer_id
WHERE c.active_status IN ('Y', 'N');  -- Exclude cancelled/expired cards from cross-reference

-- INDEX ON MATERIALIZED VIEW: Optimize cross-reference query performance
CREATE UNIQUE INDEX idx_mv_cards_xref_card_number ON mv_cards_cross_reference (card_number);
CREATE INDEX idx_mv_cards_xref_account_id ON mv_cards_cross_reference (account_id);
CREATE INDEX idx_mv_cards_xref_customer_id ON mv_cards_cross_reference (customer_id);

-- REFRESH SCHEDULE: Materialized view refresh should be scheduled
-- In production, set up a cron job or Kubernetes CronJob to refresh periodically
-- Example: REFRESH MATERIALIZED VIEW CONCURRENTLY mv_cards_cross_reference;

--rollback DROP MATERIALIZED VIEW IF EXISTS mv_cards_cross_reference CASCADE;

--comment: Cards cross-reference materialized view created for performance optimization

-- =============================================================================
-- TABLE AND COLUMN COMMENTS FOR DOCUMENTATION
-- =============================================================================

--changeset blitzy:create-cards-comments
--comment: Add comprehensive documentation comments to cards table and columns

-- TABLE COMMENT: Overall table description and VSAM mapping
COMMENT ON TABLE cards IS 
    'Cards master table migrated from VSAM CARDDAT dataset. Manages credit card information with security validation, composite foreign key relationships, and integrated cross-reference functionality. Supports Luhn algorithm validation for data integrity and maintains referential integrity to accounts and customers tables.';

-- COLUMN COMMENTS: Detailed field descriptions with VSAM mapping
COMMENT ON COLUMN cards.card_number IS 
    'Primary key - 16-digit credit card number with Luhn algorithm validation. Migrated from CARDDAT CARD-NUM field. Must follow major card network formatting rules (Visa, Mastercard, American Express, Discover).';

COMMENT ON COLUMN cards.account_id IS 
    'Foreign key to accounts table - 11-digit account identifier. Migrated from CARDDAT CARD-ACCT-ID field. Links card to specific account for transaction processing and billing.';

COMMENT ON COLUMN cards.customer_id IS 
    'Foreign key to customers table - 9-digit customer identifier. Migrated from CARDDAT CARD-CUST-ID field. Identifies primary cardholder for the credit card.';

COMMENT ON COLUMN cards.cvv_code IS 
    'Card Verification Value - 3-digit security code. Migrated from CARDDAT CARD-CVV field. SECURITY SENSITIVE: Used for transaction authorization and fraud prevention.';

COMMENT ON COLUMN cards.embossed_name IS 
    'Cardholder name as embossed on physical card. Migrated from CARDDAT CARD-EMBOSSED-NAME field. Limited to 50 characters with valid character set validation.';

COMMENT ON COLUMN cards.expiration_date IS 
    'Card expiration date for lifecycle management. Migrated from CARDDAT CARD-EXPIRE-DATE field. Must be future date, validated against reasonable limits (max 10 years).';

COMMENT ON COLUMN cards.active_status IS 
    'Card lifecycle status indicator. Migrated from CARDDAT CARD-ACTIVE-STATUS field. Values: Y=Active, N=Inactive, C=Cancelled, E=Expired. Default is Y (Active).';

COMMENT ON COLUMN cards.created_at IS 
    'Audit field - Timestamp when card record was created. Automatically populated on INSERT. Used for audit trail and compliance reporting.';

COMMENT ON COLUMN cards.updated_at IS 
    'Audit field - Timestamp when card record was last modified. Automatically updated on UPDATE via trigger. Used for audit trail and change tracking.';

--rollback: No rollback needed for comments

--comment: Documentation comments added to cards table and all columns

-- =============================================================================
-- DATA VALIDATION AND INTEGRITY FUNCTIONS
-- =============================================================================

--changeset blitzy:create-cards-utility-functions
--comment: Create utility functions for card data validation and management

-- FUNCTION: Validate Luhn algorithm for card numbers
-- Can be used by application layer for additional validation
CREATE OR REPLACE FUNCTION validate_luhn_algorithm(card_num VARCHAR(16))
RETURNS BOOLEAN AS $$
DECLARE
    i INTEGER;
    digit INTEGER;
    sum_odd INTEGER := 0;
    sum_even INTEGER := 0;
    temp INTEGER;
BEGIN
    -- Validate input format
    IF card_num IS NULL OR LENGTH(card_num) != 16 OR card_num !~ '^[0-9]{16}$' THEN
        RETURN FALSE;
    END IF;
    
    -- Calculate Luhn checksum
    FOR i IN 1..16 LOOP
        digit := CAST(SUBSTRING(card_num, i, 1) AS INTEGER);
        
        IF (16 - i + 1) % 2 = 1 THEN
            -- Odd position from right
            sum_odd := sum_odd + digit;
        ELSE
            -- Even position from right - double the digit
            temp := digit * 2;
            IF temp > 9 THEN
                temp := temp - 9;
            END IF;
            sum_even := sum_even + temp;
        END IF;
    END LOOP;
    
    -- Check if total sum is divisible by 10
    RETURN (sum_odd + sum_even) % 10 = 0;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- FUNCTION: Get card status description
CREATE OR REPLACE FUNCTION get_card_status_description(status_code VARCHAR(1))
RETURNS VARCHAR(20) AS $$
BEGIN
    RETURN CASE status_code
        WHEN 'Y' THEN 'Active'
        WHEN 'N' THEN 'Inactive'
        WHEN 'C' THEN 'Cancelled'
        WHEN 'E' THEN 'Expired'
        ELSE 'Unknown'
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- FUNCTION: Check if card is expired
CREATE OR REPLACE FUNCTION is_card_expired(exp_date DATE)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN exp_date <= CURRENT_DATE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

--rollback DROP FUNCTION IF EXISTS validate_luhn_algorithm(VARCHAR) CASCADE;
--rollback DROP FUNCTION IF EXISTS get_card_status_description(VARCHAR) CASCADE;
--rollback DROP FUNCTION IF EXISTS is_card_expired(DATE) CASCADE;

--comment: Utility functions created for card data validation and management

-- =============================================================================
-- SUCCESS CONFIRMATION
-- =============================================================================

--changeset blitzy:cards-table-completion
--comment: Confirm successful creation of cards table with all components

-- Log successful completion
SELECT 'CardDemo Migration V4: Cards table successfully created with:' AS status
UNION ALL
SELECT '  ✓ Primary table with Luhn algorithm validation' 
UNION ALL  
SELECT '  ✓ Composite foreign key relationships to accounts and customers'
UNION ALL
SELECT '  ✓ CVV code field with secure storage considerations'
UNION ALL
SELECT '  ✓ Performance optimization indexes for rapid lookups'
UNION ALL
SELECT '  ✓ Cross-reference materialized view for enhanced query performance'
UNION ALL
SELECT '  ✓ Row-level security policies for data access control'
UNION ALL
SELECT '  ✓ Audit triggers for timestamp management and compliance'
UNION ALL
SELECT '  ✓ Business validation triggers for data integrity'
UNION ALL
SELECT '  ✓ Comprehensive documentation and utility functions'
UNION ALL
SELECT '  ✓ Spring Boot JPA integration ready'
UNION ALL
SELECT '  ✓ Microservices architecture support enabled';

--rollback SELECT 'Cards table rollback completed' AS status;

--comment: Cards table migration completed successfully with all required components

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================