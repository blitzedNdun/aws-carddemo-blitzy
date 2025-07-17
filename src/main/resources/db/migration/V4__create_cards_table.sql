-- =====================================================================================
-- Liquibase Migration: V4__create_cards_table.sql
-- Description: Creates cards table from VSAM CARDDAT with security validation, 
--              composite foreign key relationships, and integrated cross-reference 
--              functionality for comprehensive card management
-- Author: Blitzy Agent  
-- Date: 2024
-- Version: 4.0
-- Dependencies: V2__create_customers_table.sql, V3__create_accounts_table.sql
-- =====================================================================================

-- changeset blitzy:V4-create-cards-table
-- comment: Create cards table migrated from CARDDAT VSAM dataset with security field preservation and Luhn validation

-- Create cards table with all required fields from VSAM CARDDAT structure
CREATE TABLE cards (
    -- Primary key: card_number as VARCHAR(16) with Luhn algorithm validation
    card_number VARCHAR(16) NOT NULL,
    
    -- Composite foreign key relationships to accounts and customers tables
    account_id VARCHAR(11) NOT NULL,
    customer_id VARCHAR(9) NOT NULL,
    
    -- CVV code field with secure storage requirements and length constraints
    cvv_code VARCHAR(3) NOT NULL,
    
    -- Embossed name field for cardholder name storage with proper length validation
    embossed_name VARCHAR(50) NOT NULL,
    
    -- Card expiration_date and active_status fields for lifecycle management
    expiration_date DATE NOT NULL,
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- Audit and tracking fields for compliance and operational oversight
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT cards_pkey PRIMARY KEY (card_number)
);

-- Create foreign key constraint to accounts table ensuring referential integrity
ALTER TABLE cards 
ADD CONSTRAINT cards_account_id_fkey 
FOREIGN KEY (account_id) REFERENCES accounts(account_id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create foreign key constraint to customers table ensuring referential integrity
ALTER TABLE cards 
ADD CONSTRAINT cards_customer_id_fkey 
FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Create indexes for optimized query performance supporting rapid lookup operations
CREATE INDEX idx_cards_account_id ON cards(account_id, active_status);
CREATE INDEX idx_cards_customer_id ON cards(customer_id, active_status);
CREATE INDEX idx_cards_expiration_date ON cards(expiration_date);
CREATE INDEX idx_cards_active_status ON cards(active_status);

-- Create composite index for card-account-customer cross-reference queries
CREATE INDEX idx_cards_composite_xref ON cards(account_id, customer_id, card_number);

-- Create Luhn algorithm validation function for card number integrity
CREATE OR REPLACE FUNCTION validate_luhn_algorithm(card_number VARCHAR(16))
RETURNS BOOLEAN AS $$
DECLARE
    digit_sum INTEGER := 0;
    current_digit INTEGER;
    doubled_digit INTEGER;
    i INTEGER;
    card_length INTEGER;
BEGIN
    -- Remove any non-digit characters and get length
    card_number := regexp_replace(card_number, '[^0-9]', '', 'g');
    card_length := length(card_number);
    
    -- Card number must be exactly 16 digits for this implementation
    IF card_length != 16 THEN
        RETURN FALSE;
    END IF;
    
    -- Process digits from right to left (reverse order)
    FOR i IN 1..card_length LOOP
        current_digit := substring(card_number, card_length - i + 1, 1)::INTEGER;
        
        -- Double every second digit from the right
        IF i % 2 = 0 THEN
            doubled_digit := current_digit * 2;
            -- If doubled digit is > 9, subtract 9 (equivalent to adding digits)
            IF doubled_digit > 9 THEN
                doubled_digit := doubled_digit - 9;
            END IF;
            digit_sum := digit_sum + doubled_digit;
        ELSE
            digit_sum := digit_sum + current_digit;
        END IF;
    END LOOP;
    
    -- Valid if sum is divisible by 10
    RETURN (digit_sum % 10 = 0);
END;
$$ LANGUAGE plpgsql;

-- Add CHECK constraints for data validation and business rules
ALTER TABLE cards 
ADD CONSTRAINT cards_card_number_check 
CHECK (card_number ~ '^[0-9]{16}$' AND validate_luhn_algorithm(card_number));

ALTER TABLE cards 
ADD CONSTRAINT cards_cvv_code_check 
CHECK (cvv_code ~ '^[0-9]{3}$');

ALTER TABLE cards 
ADD CONSTRAINT cards_active_status_check 
CHECK (active_status IN ('Y', 'N'));

ALTER TABLE cards 
ADD CONSTRAINT cards_expiration_date_check 
CHECK (expiration_date > CURRENT_DATE);

-- Add NOT NULL constraints for required fields
ALTER TABLE cards 
ADD CONSTRAINT cards_embossed_name_not_empty 
CHECK (LENGTH(TRIM(embossed_name)) > 0);

ALTER TABLE cards 
ADD CONSTRAINT cards_embossed_name_format 
CHECK (embossed_name ~ '^[A-Za-z0-9\s\.\-'']+$');

-- Create trigger function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on row changes
CREATE TRIGGER cards_updated_at_trigger
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_cards_updated_at();

-- Add comments to table and columns for documentation
COMMENT ON TABLE cards IS 'Credit card master data table migrated from VSAM CARDDAT dataset with security validation, composite foreign key relationships, and integrated cross-reference functionality';

COMMENT ON COLUMN cards.card_number IS 'Primary key: 16-digit card number with Luhn algorithm validation for data integrity';
COMMENT ON COLUMN cards.account_id IS 'Foreign key: 11-digit account identifier establishing card-to-account relationship';
COMMENT ON COLUMN cards.customer_id IS 'Foreign key: 9-digit customer identifier establishing card-to-customer relationship';
COMMENT ON COLUMN cards.cvv_code IS 'Card Verification Value: 3-digit security code with secure storage requirements';
COMMENT ON COLUMN cards.embossed_name IS 'Cardholder name embossed on card (50 characters max) with format validation';
COMMENT ON COLUMN cards.expiration_date IS 'Card expiration date for lifecycle management (must be future date)';
COMMENT ON COLUMN cards.active_status IS 'Card status indicator: Y=Active, N=Inactive for lifecycle management';
COMMENT ON COLUMN cards.created_at IS 'Timestamp when card record was created';
COMMENT ON COLUMN cards.updated_at IS 'Timestamp when card record was last updated';

-- Create materialized view for card summary queries (optimized for cross-reference operations)
CREATE MATERIALIZED VIEW mv_card_summary AS
SELECT 
    c.card_number,
    c.account_id,
    c.customer_id,
    cust.first_name,
    cust.last_name,
    CONCAT(cust.first_name, ' ', COALESCE(cust.middle_name || ' ', ''), cust.last_name) AS customer_full_name,
    c.embossed_name,
    c.expiration_date,
    c.active_status,
    CASE 
        WHEN c.expiration_date < CURRENT_DATE THEN 'Expired'
        WHEN c.expiration_date < CURRENT_DATE + INTERVAL '30 days' THEN 'Expiring Soon'
        WHEN c.active_status = 'N' THEN 'Inactive'
        ELSE 'Active'
    END AS card_status,
    EXTRACT(YEAR FROM AGE(c.expiration_date, CURRENT_DATE)) AS years_to_expiry,
    EXTRACT(MONTH FROM AGE(c.expiration_date, CURRENT_DATE)) AS months_to_expiry,
    acc.current_balance,
    acc.credit_limit,
    acc.active_status AS account_status,
    cust.fico_credit_score,
    CASE 
        WHEN cust.fico_credit_score >= 800 THEN 'Excellent'
        WHEN cust.fico_credit_score >= 740 THEN 'Very Good'
        WHEN cust.fico_credit_score >= 670 THEN 'Good'
        WHEN cust.fico_credit_score >= 580 THEN 'Fair'
        ELSE 'Poor'
    END AS credit_rating,
    c.created_at,
    c.updated_at
FROM cards c
INNER JOIN customers cust ON c.customer_id = cust.customer_id
INNER JOIN accounts acc ON c.account_id = acc.account_id;

-- Create indexes on materialized view for performance optimization
CREATE INDEX idx_mv_card_summary_customer_id ON mv_card_summary(customer_id);
CREATE INDEX idx_mv_card_summary_account_id ON mv_card_summary(account_id);
CREATE INDEX idx_mv_card_summary_card_status ON mv_card_summary(card_status);
CREATE INDEX idx_mv_card_summary_credit_rating ON mv_card_summary(credit_rating);
CREATE INDEX idx_mv_card_summary_expiration_date ON mv_card_summary(expiration_date);
CREATE INDEX idx_mv_card_summary_customer_full_name ON mv_card_summary(customer_full_name);

-- Add comment to materialized view
COMMENT ON MATERIALIZED VIEW mv_card_summary IS 'Optimized card summary view with customer and account information for cross-reference queries and comprehensive card management operations';

-- Create function to refresh materialized view (scheduled via cron or application)
CREATE OR REPLACE FUNCTION refresh_card_summary_view()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_card_summary;
END;
$$ LANGUAGE plpgsql;

-- Create function for card validation matching VSAM CARDDAT validation logic
CREATE OR REPLACE FUNCTION validate_card_status(
    p_card_number VARCHAR(16)
) RETURNS BOOLEAN AS $$
DECLARE
    v_active_status VARCHAR(1);
    v_expiration_date DATE;
    v_account_status VARCHAR(1);
BEGIN
    -- Retrieve card status and expiration date
    SELECT c.active_status, c.expiration_date, a.active_status
    INTO v_active_status, v_expiration_date, v_account_status
    FROM cards c
    INNER JOIN accounts a ON c.account_id = a.account_id
    WHERE c.card_number = p_card_number;
    
    -- Validate card is active, not expired, and account is active
    RETURN (v_active_status = 'Y' AND 
            v_expiration_date > CURRENT_DATE AND 
            v_account_status = 'Y');
END;
$$ LANGUAGE plpgsql;

-- Create function for card-account-customer relationship validation
CREATE OR REPLACE FUNCTION validate_card_relationships(
    p_card_number VARCHAR(16),
    p_account_id VARCHAR(11),
    p_customer_id VARCHAR(9)
) RETURNS BOOLEAN AS $$
DECLARE
    v_relationship_count INTEGER;
BEGIN
    -- Check if card-account-customer relationship exists and is valid
    SELECT COUNT(*)
    INTO v_relationship_count
    FROM cards c
    INNER JOIN accounts a ON c.account_id = a.account_id
    INNER JOIN customers cust ON c.customer_id = cust.customer_id
    WHERE c.card_number = p_card_number
    AND c.account_id = p_account_id
    AND c.customer_id = p_customer_id
    AND c.active_status = 'Y'
    AND a.active_status = 'Y';
    
    -- Valid if exactly one matching relationship found
    RETURN v_relationship_count = 1;
END;
$$ LANGUAGE plpgsql;

-- Create function for secure card number masking (PCI DSS compliance)
CREATE OR REPLACE FUNCTION mask_card_number(
    p_card_number VARCHAR(16)
) RETURNS VARCHAR(16) AS $$
BEGIN
    -- Mask middle 8 digits, showing only first 4 and last 4 digits
    RETURN CONCAT(
        SUBSTRING(p_card_number, 1, 4),
        '********',
        SUBSTRING(p_card_number, 13, 4)
    );
END;
$$ LANGUAGE plpgsql;

-- Create row-level security policy for card data access (PCI DSS compliance)
ALTER TABLE cards ENABLE ROW LEVEL SECURITY;

-- Create policy for card data access based on user context
CREATE POLICY cards_access_policy ON cards
    FOR ALL TO public
    USING (
        -- Allow access if user has admin role or is accessing own customer data
        current_setting('app.user_type', true) = 'A' OR
        customer_id = current_setting('app.customer_id', true)
    );

-- rollback changeset blitzy:V4-create-cards-table
-- DROP POLICY IF EXISTS cards_access_policy ON cards;
-- ALTER TABLE cards DISABLE ROW LEVEL SECURITY;
-- DROP FUNCTION IF EXISTS mask_card_number(VARCHAR(16));
-- DROP FUNCTION IF EXISTS validate_card_relationships(VARCHAR(16), VARCHAR(11), VARCHAR(9));
-- DROP FUNCTION IF EXISTS validate_card_status(VARCHAR(16));
-- DROP FUNCTION IF EXISTS refresh_card_summary_view();
-- DROP MATERIALIZED VIEW IF EXISTS mv_card_summary CASCADE;
-- DROP TRIGGER IF EXISTS cards_updated_at_trigger ON cards;
-- DROP FUNCTION IF EXISTS update_cards_updated_at();
-- DROP FUNCTION IF EXISTS validate_luhn_algorithm(VARCHAR(16));
-- DROP TABLE IF EXISTS cards CASCADE;