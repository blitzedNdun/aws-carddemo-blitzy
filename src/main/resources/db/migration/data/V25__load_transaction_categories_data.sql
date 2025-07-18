-- =====================================================================================
-- Liquibase Migration: V25__load_transaction_categories_data.sql
-- Description: Load transaction category reference data from trancatg.txt with 6-digit 
--              category codes mapped to 4-digit format for hierarchical categorization
--              and transaction analytics operations
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 25.0
-- Dependencies: V6__create_reference_tables.sql (transaction_categories table must exist)
-- =====================================================================================

-- changeset blitzy:V25-load-transaction-categories-data
-- comment: Load transaction category reference data from trancatg.txt with proper 6-digit to 4-digit mapping for sub-millisecond lookup performance

-- =============================================================================
-- 1. CLEAR ANY EXISTING INCORRECT DATA
-- =============================================================================
-- Clear any existing transaction category data that may have been incorrectly loaded
DELETE FROM transaction_categories WHERE transaction_category IS NOT NULL;

-- =============================================================================
-- 2. LOAD TRANSACTION CATEGORY DATA FROM TRANCATG.TXT
-- =============================================================================
-- Load all 18 transaction category records from trancatg.txt with proper 6-digit to 4-digit 
-- category code mapping, maintaining unique category identifiers for business rule enforcement

-- Category Group 01: Sales and Advances (010001-010005)
INSERT INTO transaction_categories (transaction_category, category_description, active_status) VALUES
('0001', 'Regular Sales Draft', true),
('0002', 'Regular Cash Advance', true),
('0003', 'Convenience Check Debit', true),
('0004', 'ATM Cash Advance', true),
('0005', 'Interest Amount', true),

-- Category Group 02: Payments (020001-020003)
('0006', 'Cash payment', true),
('0007', 'Electronic payment', true),
('0008', 'Check payment', true),

-- Category Group 03: Credits (030001-030003)
('0009', 'Credit to Account', true),
('0010', 'Credit to Purchase balance', true),
('0011', 'Credit to Cash balance', true),

-- Category Group 04: Authorizations (040001-040003)
('0012', 'Zero dollar authorization', true),
('0013', 'Online purchase authorization', true),
('0014', 'Travel booking authorization', true),

-- Category Group 05: Refunds (050001)
('0015', 'Refund credit', true),

-- Category Group 06: Reversals (060001-060002)
('0016', 'Fraud reversal', true),
('0017', 'Non-fraud reversal', true),

-- Category Group 07: Adjustments (070001)
('0018', 'Sales draft credit adjustment', true);

-- =============================================================================
-- 3. CREATE CATEGORY CODE MAPPING VIEW
-- =============================================================================
-- Create view to maintain mapping between original 6-digit codes and 4-digit categories
-- for reference and reporting purposes
CREATE OR REPLACE VIEW vw_transaction_category_mapping AS
SELECT 
    transaction_category,
    category_description,
    CASE transaction_category
        WHEN '0001' THEN '010001'
        WHEN '0002' THEN '010002'
        WHEN '0003' THEN '010003'
        WHEN '0004' THEN '010004'
        WHEN '0005' THEN '010005'
        WHEN '0006' THEN '020001'
        WHEN '0007' THEN '020002'
        WHEN '0008' THEN '020003'
        WHEN '0009' THEN '030001'
        WHEN '0010' THEN '030002'
        WHEN '0011' THEN '030003'
        WHEN '0012' THEN '040001'
        WHEN '0013' THEN '040002'
        WHEN '0014' THEN '040003'
        WHEN '0015' THEN '050001'
        WHEN '0016' THEN '060001'
        WHEN '0017' THEN '060002'
        WHEN '0018' THEN '070001'
    END AS original_category_code,
    CASE 
        WHEN transaction_category BETWEEN '0001' AND '0005' THEN '01'
        WHEN transaction_category BETWEEN '0006' AND '0008' THEN '02'
        WHEN transaction_category BETWEEN '0009' AND '0011' THEN '03'
        WHEN transaction_category BETWEEN '0012' AND '0014' THEN '04'
        WHEN transaction_category = '0015' THEN '05'
        WHEN transaction_category BETWEEN '0016' AND '0017' THEN '06'
        WHEN transaction_category = '0018' THEN '07'
    END AS category_group,
    CASE 
        WHEN transaction_category BETWEEN '0001' AND '0005' THEN 'Sales and Advances'
        WHEN transaction_category BETWEEN '0006' AND '0008' THEN 'Payments'
        WHEN transaction_category BETWEEN '0009' AND '0011' THEN 'Credits'
        WHEN transaction_category BETWEEN '0012' AND '0014' THEN 'Authorizations'
        WHEN transaction_category = '0015' THEN 'Refunds'
        WHEN transaction_category BETWEEN '0016' AND '0017' THEN 'Reversals'
        WHEN transaction_category = '0018' THEN 'Adjustments'
    END AS category_group_description,
    active_status,
    created_at,
    updated_at
FROM transaction_categories
ORDER BY transaction_category;

-- =============================================================================
-- 4. CREATE OPTIMIZED LOOKUP FUNCTIONS
-- =============================================================================
-- Function to get transaction category by original 6-digit code
CREATE OR REPLACE FUNCTION get_transaction_category_by_original_code(p_original_code VARCHAR(6))
RETURNS VARCHAR(4) AS $$
BEGIN
    RETURN CASE p_original_code
        WHEN '010001' THEN '0001'
        WHEN '010002' THEN '0002'
        WHEN '010003' THEN '0003'
        WHEN '010004' THEN '0004'
        WHEN '010005' THEN '0005'
        WHEN '020001' THEN '0006'
        WHEN '020002' THEN '0007'
        WHEN '020003' THEN '0008'
        WHEN '030001' THEN '0009'
        WHEN '030002' THEN '0010'
        WHEN '030003' THEN '0011'
        WHEN '040001' THEN '0012'
        WHEN '040002' THEN '0013'
        WHEN '040003' THEN '0014'
        WHEN '050001' THEN '0015'
        WHEN '060001' THEN '0016'
        WHEN '060002' THEN '0017'
        WHEN '070001' THEN '0018'
        ELSE NULL
    END;
END;
$$ LANGUAGE plpgsql;

-- Function to get category group information
CREATE OR REPLACE FUNCTION get_category_group_info(p_transaction_category VARCHAR(4))
RETURNS TABLE (
    category_group VARCHAR(2),
    group_description VARCHAR(50),
    is_debit_category BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        CASE 
            WHEN p_transaction_category BETWEEN '0001' AND '0005' THEN '01'
            WHEN p_transaction_category BETWEEN '0006' AND '0008' THEN '02'
            WHEN p_transaction_category BETWEEN '0009' AND '0011' THEN '03'
            WHEN p_transaction_category BETWEEN '0012' AND '0014' THEN '04'
            WHEN p_transaction_category = '0015' THEN '05'
            WHEN p_transaction_category BETWEEN '0016' AND '0017' THEN '06'
            WHEN p_transaction_category = '0018' THEN '07'
        END,
        CASE 
            WHEN p_transaction_category BETWEEN '0001' AND '0005' THEN 'Sales and Advances'
            WHEN p_transaction_category BETWEEN '0006' AND '0008' THEN 'Payments'
            WHEN p_transaction_category BETWEEN '0009' AND '0011' THEN 'Credits'
            WHEN p_transaction_category BETWEEN '0012' AND '0014' THEN 'Authorizations'
            WHEN p_transaction_category = '0015' THEN 'Refunds'
            WHEN p_transaction_category BETWEEN '0016' AND '0017' THEN 'Reversals'
            WHEN p_transaction_category = '0018' THEN 'Adjustments'
        END,
        CASE 
            WHEN p_transaction_category BETWEEN '0001' AND '0005' THEN true   -- Sales and Advances are debits
            WHEN p_transaction_category BETWEEN '0006' AND '0008' THEN false  -- Payments are credits
            WHEN p_transaction_category BETWEEN '0009' AND '0011' THEN false  -- Credits are credits
            WHEN p_transaction_category BETWEEN '0012' AND '0014' THEN true   -- Authorizations are debits
            WHEN p_transaction_category = '0015' THEN false                   -- Refunds are credits
            WHEN p_transaction_category BETWEEN '0016' AND '0017' THEN false  -- Reversals are credits
            WHEN p_transaction_category = '0018' THEN false                   -- Adjustments are credits
        END;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 5. CREATE PERFORMANCE OPTIMIZATION INDEXES
-- =============================================================================
-- Additional indexes for sub-millisecond lookup performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_categories_lookup_optimized 
ON transaction_categories(transaction_category, active_status, category_description);

-- Partial index for active categories only (most common lookups)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_categories_active_only 
ON transaction_categories(transaction_category, category_description) 
WHERE active_status = true;

-- =============================================================================
-- 6. VALIDATE DATA INTEGRITY
-- =============================================================================
-- Verify all 18 records were loaded correctly
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM transaction_categories WHERE active_status = true;
    
    IF record_count != 18 THEN
        RAISE EXCEPTION 'Transaction categories data validation failed. Expected 18 records, found %', record_count;
    END IF;
    
    RAISE NOTICE 'Transaction categories data validation successful: % records loaded', record_count;
END;
$$;

-- =============================================================================
-- 7. ADD COMPREHENSIVE COMMENTS FOR DOCUMENTATION
-- =============================================================================
-- View and function comments
COMMENT ON VIEW vw_transaction_category_mapping IS 'Mapping view between 4-digit transaction categories and original 6-digit codes from trancatg.txt for reporting and reference';

COMMENT ON FUNCTION get_transaction_category_by_original_code(VARCHAR(6)) IS 'Function to convert original 6-digit category codes to 4-digit category identifiers for transaction processing';

COMMENT ON FUNCTION get_category_group_info(VARCHAR(4)) IS 'Function to retrieve category group information and debit/credit classification for business rule enforcement';

-- =============================================================================
-- 8. ROLLBACK CHANGESET INSTRUCTIONS
-- =============================================================================
-- rollback changeset blitzy:V25-load-transaction-categories-data
-- DROP INDEX IF EXISTS idx_transaction_categories_active_only;
-- DROP INDEX IF EXISTS idx_transaction_categories_lookup_optimized;
-- DROP FUNCTION IF EXISTS get_category_group_info(VARCHAR(4));
-- DROP FUNCTION IF EXISTS get_transaction_category_by_original_code(VARCHAR(6));
-- DROP VIEW IF EXISTS vw_transaction_category_mapping;
-- DELETE FROM transaction_categories WHERE transaction_category BETWEEN '0001' AND '0018';