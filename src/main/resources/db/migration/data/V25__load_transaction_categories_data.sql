-- ============================================================================
-- Liquibase Data Loading Migration: V25__load_transaction_categories_data.sql
-- Description: Load transaction category reference data from trancatg.txt ASCII file
--              Parse 6-digit category codes into 4-digit format with hierarchical structure
-- Author: Blitzy agent
-- Version: 25.0
-- Dependencies: V6__create_reference_tables.sql (transaction_categories table)
-- ============================================================================

-- Clear existing data to ensure clean state for data loading
DELETE FROM transaction_categories;

-- Load transaction category data from trancatg.txt with 6-digit to 4-digit conversion
-- Source format: 6-digit codes (010001, 010002, etc.) with descriptions and terminators
-- Target format: 4-digit category codes with parent transaction type relationships

INSERT INTO transaction_categories (
    transaction_category, 
    category_description, 
    parent_transaction_type, 
    active_status
) VALUES 
    -- Purchase transaction categories (parent type '01')
    ('1001', 'Regular Sales Draft', '01', TRUE),
    ('1002', 'Regular Cash Advance', '01', TRUE),
    ('1003', 'Convenience Check Debit', '01', TRUE),
    ('1004', 'ATM Cash Advance', '01', TRUE),
    ('1005', 'Interest Amount', '01', TRUE),
    
    -- Payment transaction categories (parent type '02')
    ('2001', 'Cash payment', '02', TRUE),
    ('2002', 'Electronic payment', '02', TRUE),
    ('2003', 'Check payment', '02', TRUE),
    
    -- Credit transaction categories (parent type '03')
    ('3001', 'Credit to Account', '03', TRUE),
    ('3002', 'Credit to Purchase balance', '03', TRUE),
    ('3003', 'Credit to Cash balance', '03', TRUE),
    
    -- Authorization transaction categories (parent type '04')
    ('4001', 'Zero dollar authorization', '04', TRUE),
    ('4002', 'Online purchase authorization', '04', TRUE),
    ('4003', 'Travel booking authorization', '04', TRUE),
    
    -- Refund transaction categories (parent type '05')
    ('5001', 'Refund credit', '05', TRUE),
    
    -- Reversal transaction categories (parent type '06')
    ('6001', 'Fraud reversal', '06', TRUE),
    ('6002', 'Non-fraud reversal', '06', TRUE),
    
    -- Adjustment transaction categories (parent type '07')
    ('7001', 'Sales draft credit adjustment', '07', TRUE);

-- Insert transaction category balance tracking records for system initialization
-- This establishes the foundation for transaction_category foreign key relationships
INSERT INTO transaction_category_balances (
    account_id, 
    transaction_category, 
    category_balance, 
    last_updated
)
SELECT 
    a.account_id,
    tc.transaction_category,
    0.00 AS category_balance,
    CURRENT_TIMESTAMP AS last_updated
FROM accounts a
CROSS JOIN transaction_categories tc
WHERE a.active_status = TRUE
  AND tc.active_status = TRUE
  AND EXISTS (
      SELECT 1 FROM transaction_types tt 
      WHERE tt.transaction_type = tc.parent_transaction_type 
      AND tt.active_status = TRUE
  );

-- Create performance optimization indexes for sub-millisecond response times
-- Index for hierarchical category lookup by parent transaction type
CREATE INDEX IF NOT EXISTS idx_transaction_categories_parent_lookup 
ON transaction_categories (parent_transaction_type, transaction_category, active_status);

-- Index for category description text search and reporting
CREATE INDEX IF NOT EXISTS idx_transaction_categories_description_search 
ON transaction_categories (category_description, active_status);

-- Index for active status filtering with category ordering
CREATE INDEX IF NOT EXISTS idx_transaction_categories_active_order 
ON transaction_categories (active_status, transaction_category);

-- Create materialized view for optimized category lookup operations
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_category_lookup AS
SELECT 
    tc.transaction_category,
    tc.category_description,
    tc.parent_transaction_type,
    tt.type_description AS parent_type_description,
    tt.debit_credit_indicator,
    tc.active_status AS category_active,
    tt.active_status AS type_active,
    CASE 
        WHEN tc.active_status = TRUE AND tt.active_status = TRUE THEN 'ACTIVE'
        WHEN tc.active_status = FALSE THEN 'INACTIVE_CATEGORY'
        WHEN tt.active_status = FALSE THEN 'INACTIVE_TYPE'
        ELSE 'UNKNOWN'
    END AS status_code
FROM transaction_categories tc
JOIN transaction_types tt ON tc.parent_transaction_type = tt.transaction_type
WHERE tc.active_status = TRUE AND tt.active_status = TRUE
ORDER BY tc.transaction_category;

-- Create unique index on materialized view for optimal query performance
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_transaction_category_lookup_unique 
ON mv_transaction_category_lookup (transaction_category);

-- Create secondary index for parent type hierarchy queries
CREATE INDEX IF NOT EXISTS idx_mv_transaction_category_lookup_parent 
ON mv_transaction_category_lookup (parent_transaction_type, transaction_category);

-- Add comprehensive table and column comments for documentation
COMMENT ON TABLE transaction_categories IS 'Transaction category reference table loaded from trancatg.txt with 6-digit to 4-digit category code conversion supporting hierarchical transaction categorization for business rule enforcement and reporting';

COMMENT ON COLUMN transaction_categories.transaction_category IS 'Primary key: 4-character transaction category code converted from 6-digit trancatg.txt format (010001->1001, 020001->2001, etc.)';
COMMENT ON COLUMN transaction_categories.category_description IS 'Human-readable transaction category description from trancatg.txt supporting user interface display and business reporting';
COMMENT ON COLUMN transaction_categories.parent_transaction_type IS 'Foreign key to transaction_types table creating hierarchical relationship derived from first 2 digits of 6-digit trancatg.txt codes';
COMMENT ON COLUMN transaction_categories.active_status IS 'Boolean flag for category lifecycle management enabling activation/deactivation of categories for business rule enforcement';

-- Create database functions for category validation and lookup operations
CREATE OR REPLACE FUNCTION get_transaction_category_by_code(p_category_code VARCHAR(4))
RETURNS TABLE(
    category_code VARCHAR(4),
    category_desc VARCHAR(60),
    parent_type VARCHAR(2),
    is_active BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tc.transaction_category,
        tc.category_description,
        tc.parent_transaction_type,
        tc.active_status
    FROM transaction_categories tc
    WHERE tc.transaction_category = p_category_code
      AND tc.active_status = TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to validate transaction category for business rule enforcement
CREATE OR REPLACE FUNCTION validate_transaction_category(p_category_code VARCHAR(4))
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 
        FROM transaction_categories tc
        JOIN transaction_types tt ON tc.parent_transaction_type = tt.transaction_type
        WHERE tc.transaction_category = p_category_code
          AND tc.active_status = TRUE
          AND tt.active_status = TRUE
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get hierarchical category information for reporting
CREATE OR REPLACE FUNCTION get_category_hierarchy(p_transaction_type VARCHAR(2))
RETURNS TABLE(
    category_code VARCHAR(4),
    category_desc VARCHAR(60),
    type_desc VARCHAR(60),
    debit_credit_flag BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tc.transaction_category,
        tc.category_description,
        tt.type_description,
        tt.debit_credit_indicator
    FROM transaction_categories tc
    JOIN transaction_types tt ON tc.parent_transaction_type = tt.transaction_type
    WHERE tc.parent_transaction_type = p_transaction_type
      AND tc.active_status = TRUE
      AND tt.active_status = TRUE
    ORDER BY tc.transaction_category;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for maintaining data integrity and audit trails
CREATE OR REPLACE FUNCTION audit_transaction_category_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Log category activation/deactivation changes
    IF TG_OP = 'UPDATE' AND OLD.active_status != NEW.active_status THEN
        INSERT INTO audit_log (
            table_name, 
            operation, 
            record_id, 
            old_values, 
            new_values, 
            changed_at
        ) VALUES (
            'transaction_categories',
            'STATUS_CHANGE',
            NEW.transaction_category,
            jsonb_build_object('active_status', OLD.active_status),
            jsonb_build_object('active_status', NEW.active_status),
            CURRENT_TIMESTAMP
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for audit logging (if audit_log table exists)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_log') THEN
        CREATE TRIGGER trg_transaction_categories_audit
            AFTER UPDATE ON transaction_categories
            FOR EACH ROW
            EXECUTE FUNCTION audit_transaction_category_changes();
    END IF;
END $$;

-- Verify data loading integrity and constraints
DO $$
DECLARE
    category_count INTEGER;
    active_count INTEGER;
    hierarchy_count INTEGER;
BEGIN
    -- Verify all 18 transaction categories were loaded
    SELECT COUNT(*) INTO category_count FROM transaction_categories;
    IF category_count != 18 THEN
        RAISE EXCEPTION 'Transaction category count mismatch: expected 18, got %', category_count;
    END IF;
    
    -- Verify all categories are active by default
    SELECT COUNT(*) INTO active_count FROM transaction_categories WHERE active_status = TRUE;
    IF active_count != 18 THEN
        RAISE EXCEPTION 'Active category count mismatch: expected 18, got %', active_count;
    END IF;
    
    -- Verify hierarchical relationships are valid
    SELECT COUNT(*) INTO hierarchy_count 
    FROM transaction_categories tc
    JOIN transaction_types tt ON tc.parent_transaction_type = tt.transaction_type
    WHERE tc.active_status = TRUE AND tt.active_status = TRUE;
    
    IF hierarchy_count != 18 THEN
        RAISE EXCEPTION 'Hierarchy validation failed: expected 18 valid relationships, got %', hierarchy_count;
    END IF;
    
    RAISE NOTICE 'Transaction categories data loading validation completed successfully: % categories loaded', category_count;
END $$;

-- Refresh materialized view for immediate availability
REFRESH MATERIALIZED VIEW mv_transaction_category_lookup;

-- Grant appropriate permissions for application access
-- Note: Adjust role names based on deployment environment
GRANT SELECT ON transaction_categories TO PUBLIC;
GRANT SELECT ON mv_transaction_category_lookup TO PUBLIC;
GRANT EXECUTE ON FUNCTION get_transaction_category_by_code(VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION validate_transaction_category(VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION get_category_hierarchy(VARCHAR) TO PUBLIC;

-- Performance optimization notes for sub-millisecond response times:
-- 1. All category lookups use B-tree indexes for O(log n) performance
-- 2. Materialized view provides pre-computed joins for complex queries
-- 3. Function-based validation eliminates multiple query round trips
-- 4. Composite indexes optimize hierarchical queries and reporting
-- 5. Active status filtering uses covering indexes for index-only scans

-- Data validation and business rule enforcement:
-- 1. All 18 transaction categories from trancatg.txt successfully loaded
-- 2. 6-digit to 4-digit category code conversion maintains uniqueness
-- 3. Hierarchical relationships preserved through parent_transaction_type
-- 4. Active status enables category lifecycle management
-- 5. Foreign key constraints ensure referential integrity
-- 6. Check constraints validate category code format and business rules

-- Migration rollback instructions:
-- To rollback this data loading migration:
-- 1. DROP TRIGGER IF EXISTS trg_transaction_categories_audit ON transaction_categories CASCADE;
-- 2. DROP FUNCTION IF EXISTS audit_transaction_category_changes() CASCADE;
-- 3. DROP FUNCTION IF EXISTS get_category_hierarchy(VARCHAR) CASCADE;
-- 4. DROP FUNCTION IF EXISTS validate_transaction_category(VARCHAR) CASCADE;
-- 5. DROP FUNCTION IF EXISTS get_transaction_category_by_code(VARCHAR) CASCADE;
-- 6. DROP MATERIALIZED VIEW IF EXISTS mv_transaction_category_lookup CASCADE;
-- 7. DELETE FROM transaction_category_balances WHERE transaction_category IN (SELECT transaction_category FROM transaction_categories);
-- 8. DELETE FROM transaction_categories;
-- 9. DROP INDEX IF EXISTS idx_transaction_categories_active_order;
-- 10. DROP INDEX IF EXISTS idx_transaction_categories_description_search;
-- 11. DROP INDEX IF EXISTS idx_transaction_categories_parent_lookup;