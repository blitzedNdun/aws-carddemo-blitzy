-- =====================================================================================
-- Liquibase Data Migration: V24__load_transaction_types_data.sql
-- Description: Loads transaction type reference data from trantype.txt with 2-character
--              type codes (01-07), transaction type descriptions, and debit/credit 
--              indicator boolean field mapping for proper transaction classification
--              and validation across the payment processing system
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 24.0
-- Dependencies: V6__create_reference_tables.sql (transaction_types table must exist)
-- Source Data: app/data/ASCII/trantype.txt
-- =====================================================================================

-- changeset blitzy:V24-load-transaction-types-data
-- comment: Load transaction type reference data from trantype.txt with high-performance lookup optimization and caching support

-- =============================================================================
-- 1. DATA VALIDATION AND PREPARATION
-- =============================================================================

-- Validate that the transaction_types table exists and has the correct structure
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'transaction_types' 
                   AND table_schema = CURRENT_SCHEMA()) THEN
        RAISE EXCEPTION 'transaction_types table does not exist. Please run V6__create_reference_tables.sql first.';
    END IF;
END $$;

-- Validate table structure compatibility
DO $$
BEGIN
    -- Check for required columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'transaction_types' 
                   AND column_name = 'transaction_type' 
                   AND data_type = 'character varying') THEN
        RAISE EXCEPTION 'transaction_types table missing required transaction_type VARCHAR column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'transaction_types' 
                   AND column_name = 'type_description' 
                   AND data_type = 'character varying') THEN
        RAISE EXCEPTION 'transaction_types table missing required type_description VARCHAR column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'transaction_types' 
                   AND column_name = 'debit_credit_indicator' 
                   AND data_type = 'boolean') THEN
        RAISE EXCEPTION 'transaction_types table missing required debit_credit_indicator BOOLEAN column';
    END IF;
END $$;

-- =============================================================================
-- 2. CLEAR EXISTING DATA (IF ANY)
-- =============================================================================

-- Clear any existing transaction type data to ensure clean data load
-- This handles scenarios where the migration might be re-run
TRUNCATE TABLE transaction_types RESTART IDENTITY CASCADE;

-- =============================================================================
-- 3. LOAD TRANSACTION TYPE REFERENCE DATA
-- =============================================================================

-- Load transaction type reference data from trantype.txt with exact field mapping
-- Source format: 2-char code + description + 8-digit placeholder (00000000)
-- Target mapping: transaction_type, type_description, debit_credit_indicator, active_status

-- Transaction Type 01: Purchase
-- Description: Purchase transactions representing customer purchases
-- Classification: Debit (true) - increases amount owed, money goes out
-- Financial Logic: Customer makes a purchase, account balance increases (debit)
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '01',
    'Purchase',
    true,  -- Debit transaction: increases amount owed
    true,  -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Transaction Type 02: Payment
-- Description: Payment transactions reducing account balances
-- Classification: Credit (false) - reduces amount owed, money comes in
-- Financial Logic: Customer makes payment, account balance decreases (credit)
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '02',
    'Payment',
    false,  -- Credit transaction: reduces amount owed
    true,   -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Transaction Type 03: Credit
-- Description: Credit transactions adding positive balance
-- Classification: Credit (false) - reduces amount owed, money comes in
-- Financial Logic: Credit applied to account, account balance decreases (credit)
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '03',
    'Credit',
    false,  -- Credit transaction: reduces amount owed
    true,   -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Transaction Type 04: Authorization
-- Description: Authorization transactions for purchase validation
-- Classification: Debit (true) - reserves funds, hold amount
-- Financial Logic: Authorization holds funds, treated as debit reservation
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '04',
    'Authorization',
    true,  -- Debit transaction: reserves funds (hold)
    true,  -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Transaction Type 05: Refund
-- Description: Refund transactions returning money to customer
-- Classification: Credit (false) - reduces amount owed, money comes back
-- Financial Logic: Refund returns money, account balance decreases (credit)
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '05',
    'Refund',
    false,  -- Credit transaction: reduces amount owed
    true,   -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Transaction Type 06: Reversal
-- Description: Reversal transactions undoing previous transactions
-- Classification: Credit (false) - typically reverses previous debit
-- Financial Logic: Reversal undoes previous transaction, typically credit
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '06',
    'Reversal',
    false,  -- Credit transaction: typically reverses debit
    true,   -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Transaction Type 07: Adjustment
-- Description: Adjustment transactions for account corrections
-- Classification: Debit (true) - typically increases amount owed
-- Financial Logic: Adjustment typically increases balance (debit)
INSERT INTO transaction_types (
    transaction_type,
    type_description,
    debit_credit_indicator,
    active_status,
    created_at,
    updated_at
) VALUES (
    '07',
    'Adjustment',
    true,  -- Debit transaction: typically increases amount owed
    true,  -- Active status for current operations
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- 4. DATA VALIDATION AND VERIFICATION
-- =============================================================================

-- Verify that all 7 transaction types were loaded successfully
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM transaction_types WHERE active_status = true;
    
    IF record_count != 7 THEN
        RAISE EXCEPTION 'Expected 7 transaction type records, but found %', record_count;
    END IF;
    
    RAISE NOTICE 'Successfully loaded % transaction type records', record_count;
END $$;

-- Verify specific transaction type codes exist
DO $$
DECLARE
    missing_codes TEXT[];
    expected_codes TEXT[] := ARRAY['01', '02', '03', '04', '05', '06', '07'];
    code TEXT;
BEGIN
    FOREACH code IN ARRAY expected_codes
    LOOP
        IF NOT EXISTS (SELECT 1 FROM transaction_types WHERE transaction_type = code) THEN
            missing_codes := array_append(missing_codes, code);
        END IF;
    END LOOP;
    
    IF array_length(missing_codes, 1) > 0 THEN
        RAISE EXCEPTION 'Missing transaction type codes: %', array_to_string(missing_codes, ', ');
    END IF;
    
    RAISE NOTICE 'All required transaction type codes are present';
END $$;

-- Verify debit/credit indicator distribution
DO $$
DECLARE
    debit_count INTEGER;
    credit_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO debit_count FROM transaction_types WHERE debit_credit_indicator = true;
    SELECT COUNT(*) INTO credit_count FROM transaction_types WHERE debit_credit_indicator = false;
    
    RAISE NOTICE 'Debit transactions: % (Purchase, Authorization, Adjustment)', debit_count;
    RAISE NOTICE 'Credit transactions: % (Payment, Credit, Refund, Reversal)', credit_count;
    
    IF debit_count + credit_count != 7 THEN
        RAISE EXCEPTION 'Debit/credit indicator validation failed';
    END IF;
END $$;

-- =============================================================================
-- 5. PERFORMANCE OPTIMIZATION FOR HIGH-FREQUENCY LOOKUPS
-- =============================================================================

-- Update table statistics for query planner optimization
ANALYZE transaction_types;

-- Verify index existence for sub-millisecond lookup performance
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'transaction_types' 
                   AND indexname = 'transaction_types_pkey') THEN
        RAISE WARNING 'Primary key index missing on transaction_types table';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'transaction_types' 
                   AND indexname = 'idx_transaction_types_active_status') THEN
        RAISE WARNING 'Active status index missing on transaction_types table';
    END IF;
    
    RAISE NOTICE 'Index verification completed for high-performance lookups';
END $$;

-- =============================================================================
-- 6. CACHING OPTIMIZATION SUPPORT
-- =============================================================================

-- Create a function to support caching strategies for transaction type lookups
CREATE OR REPLACE FUNCTION get_transaction_type_info(p_transaction_type VARCHAR(2))
RETURNS TABLE (
    transaction_type VARCHAR(2),
    type_description VARCHAR(60),
    debit_credit_indicator BOOLEAN,
    is_debit BOOLEAN,
    is_credit BOOLEAN,
    display_name VARCHAR(100)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tt.transaction_type,
        tt.type_description,
        tt.debit_credit_indicator,
        tt.debit_credit_indicator AS is_debit,
        NOT tt.debit_credit_indicator AS is_credit,
        CONCAT(tt.transaction_type, ' - ', tt.type_description) AS display_name
    FROM transaction_types tt
    WHERE tt.transaction_type = p_transaction_type
    AND tt.active_status = true;
END;
$$ LANGUAGE plpgsql;

-- Create a function to get all active transaction types (for cache warming)
CREATE OR REPLACE FUNCTION get_all_active_transaction_types()
RETURNS TABLE (
    transaction_type VARCHAR(2),
    type_description VARCHAR(60),
    debit_credit_indicator BOOLEAN,
    transaction_direction VARCHAR(6),
    sort_order INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tt.transaction_type,
        tt.type_description,
        tt.debit_credit_indicator,
        CASE 
            WHEN tt.debit_credit_indicator = true THEN 'DEBIT'
            ELSE 'CREDIT'
        END AS transaction_direction,
        tt.transaction_type::INTEGER AS sort_order
    FROM transaction_types tt
    WHERE tt.active_status = true
    ORDER BY tt.transaction_type;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 7. BUSINESS LOGIC VALIDATION FUNCTIONS
-- =============================================================================

-- Create validation function for transaction type classification
CREATE OR REPLACE FUNCTION validate_transaction_classification(
    p_transaction_type VARCHAR(2),
    p_transaction_amount DECIMAL(12,2)
) RETURNS BOOLEAN AS $$
DECLARE
    v_debit_credit_indicator BOOLEAN;
    v_type_description VARCHAR(60);
BEGIN
    -- Get transaction type information
    SELECT debit_credit_indicator, type_description
    INTO v_debit_credit_indicator, v_type_description
    FROM transaction_types
    WHERE transaction_type = p_transaction_type
    AND active_status = true;
    
    -- Validate transaction type exists
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid transaction type: %', p_transaction_type;
    END IF;
    
    -- Business rule validation based on transaction type
    CASE p_transaction_type
        WHEN '01' THEN  -- Purchase
            IF p_transaction_amount <= 0 THEN
                RAISE EXCEPTION 'Purchase transactions must have positive amounts';
            END IF;
        WHEN '02' THEN  -- Payment
            IF p_transaction_amount <= 0 THEN
                RAISE EXCEPTION 'Payment transactions must have positive amounts';
            END IF;
        WHEN '05' THEN  -- Refund
            IF p_transaction_amount <= 0 THEN
                RAISE EXCEPTION 'Refund transactions must have positive amounts';
            END IF;
        ELSE
            -- Standard validation for other transaction types
            IF p_transaction_amount = 0 THEN
                RAISE EXCEPTION 'Transaction amount cannot be zero for type %', v_type_description;
            END IF;
    END CASE;
    
    RETURN true;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 8. SYSTEM INTEGRATION VERIFICATION
-- =============================================================================

-- Test the caching support functions
DO $$
DECLARE
    test_record RECORD;
    function_count INTEGER;
BEGIN
    -- Test individual transaction type lookup
    SELECT * INTO test_record FROM get_transaction_type_info('01');
    
    IF test_record.transaction_type IS NULL THEN
        RAISE EXCEPTION 'Transaction type lookup function failed';
    END IF;
    
    -- Test all active transaction types function
    SELECT COUNT(*) INTO function_count FROM get_all_active_transaction_types();
    
    IF function_count != 7 THEN
        RAISE EXCEPTION 'Expected 7 active transaction types, got %', function_count;
    END IF;
    
    RAISE NOTICE 'System integration verification completed successfully';
END $$;

-- =============================================================================
-- 9. DOCUMENTATION AND METADATA
-- =============================================================================

-- Add comprehensive comments for transaction type reference data
COMMENT ON TABLE transaction_types IS 'Transaction type reference table populated from trantype.txt with 2-character type codes, descriptions, and debit/credit classification for payment processing system validation and categorization';

-- Add column-specific comments for data dictionary
COMMENT ON COLUMN transaction_types.transaction_type IS 'Primary key: 2-character transaction type code (01-07) from trantype.txt for system-wide transaction classification';
COMMENT ON COLUMN transaction_types.type_description IS 'Transaction type description from trantype.txt: Purchase, Payment, Credit, Authorization, Refund, Reversal, Adjustment';
COMMENT ON COLUMN transaction_types.debit_credit_indicator IS 'Boolean field for transaction classification: true=debit (increases amount owed), false=credit (reduces amount owed)';
COMMENT ON COLUMN transaction_types.active_status IS 'Active status flag for reference data lifecycle management and business rule enforcement';

-- Add function comments for system documentation
COMMENT ON FUNCTION get_transaction_type_info(VARCHAR(2)) IS 'Retrieves complete transaction type information for caching and validation with sub-millisecond lookup performance';
COMMENT ON FUNCTION get_all_active_transaction_types() IS 'Returns all active transaction types for cache warming and system initialization with optimized ordering';
COMMENT ON FUNCTION validate_transaction_classification(VARCHAR(2), DECIMAL(12,2)) IS 'Validates transaction type and amount combinations according to business rules for payment processing integrity';

-- =============================================================================
-- 10. COMPLETION STATUS AND LOGGING
-- =============================================================================

-- Log successful completion
DO $$
BEGIN
    RAISE NOTICE '========================================================================';
    RAISE NOTICE 'V24__load_transaction_types_data.sql completed successfully';
    RAISE NOTICE 'Loaded 7 transaction type records from trantype.txt';
    RAISE NOTICE 'Transaction types: 01-Purchase, 02-Payment, 03-Credit, 04-Authorization';
    RAISE NOTICE '                   05-Refund, 06-Reversal, 07-Adjustment';
    RAISE NOTICE 'Debit/Credit classification implemented for financial transaction processing';
    RAISE NOTICE 'High-performance lookup functions created for caching optimization';
    RAISE NOTICE 'Business validation functions implemented for transaction integrity';
    RAISE NOTICE 'System ready for transaction type validation and categorization';
    RAISE NOTICE '========================================================================';
END $$;

-- =============================================================================
-- 11. ROLLBACK CHANGESET INSTRUCTIONS
-- =============================================================================

-- rollback changeset blitzy:V24-load-transaction-types-data
-- DROP FUNCTION IF EXISTS validate_transaction_classification(VARCHAR(2), DECIMAL(12,2));
-- DROP FUNCTION IF EXISTS get_all_active_transaction_types();
-- DROP FUNCTION IF EXISTS get_transaction_type_info(VARCHAR(2));
-- TRUNCATE TABLE transaction_types RESTART IDENTITY CASCADE;
-- DELETE FROM transaction_types WHERE transaction_type IN ('01', '02', '03', '04', '05', '06', '07');