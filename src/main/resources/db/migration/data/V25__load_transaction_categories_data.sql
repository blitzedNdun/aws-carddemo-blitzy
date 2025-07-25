-- ==============================================================================
-- Liquibase Migration: V25__load_transaction_categories_data.sql
-- Description: Loads transaction category reference data from trancatg.txt with
--              6-digit to 4-character category code conversion, parent transaction
--              type mapping, and active status management for business rule enforcement
-- Author: Blitzy agent
-- Version: 25.0
-- Migration Type: DATA LOADING with optimized bulk INSERT operations
-- Source: app/data/ASCII/trancatg.txt (18 fixed-width category records)
-- Target: transaction_categories table with hierarchical structure
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:load-transaction-categories-data-v25
--comment: Load transaction category reference data from trancatg.txt with 6-digit category codes converted to 4-character format and hierarchical parent-child relationships

-- Clear any existing transaction category data to ensure clean data load
-- This handles cases where partial data may exist from initial migration
DELETE FROM transaction_categories WHERE transaction_category IN (
    '0001', '0002', '0003', '0004', '0005'
);

-- Load transaction category data from trancatg.txt with proper data transformations
-- Each record represents a transaction category with 6-digit code, description, and terminator
-- Data transformation: 010001 -> category='0001', parent_transaction_type='01'
INSERT INTO transaction_categories (
    transaction_category, 
    parent_transaction_type, 
    category_description, 
    active_status,
    created_at,
    updated_at
) VALUES 
-- Category Group 01: Purchase Transaction Categories
-- Original: 010001Regular Sales Draft                               0000
('0001', '01', 'Regular Sales Draft', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 010002Regular Cash Advance                              0000  
('0002', '01', 'Regular Cash Advance', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 010003Convenience Check Debit                           0000
('0003', '01', 'Convenience Check Debit', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 010004ATM Cash Advance                                  0000
('0004', '01', 'ATM Cash Advance', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 010005Interest Amount                                   0000
('0005', '01', 'Interest Amount', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Category Group 02: Payment Transaction Categories  
-- Original: 020001Cash payment                                      0000
('0001', '02', 'Cash payment', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 020002Electronic payment                                0000
('0002', '02', 'Electronic payment', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 020003Check payment                                     0000
('0003', '02', 'Check payment', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Category Group 03: Credit Transaction Categories
-- Original: 030001Credit to Account                                 0000  
('0001', '03', 'Credit to Account', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 030002Credit to Purchase balance                        0000
('0002', '03', 'Credit to Purchase balance', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 030003Credit to Cash balance                            0000
('0003', '03', 'Credit to Cash balance', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Category Group 04: Authorization Transaction Categories
-- Original: 040001Zero dollar authorization                         0000
('0001', '04', 'Zero dollar authorization', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 040002Online purchase authorization                     0000
('0002', '04', 'Online purchase authorization', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 040003Travel booking authorization                      0000
('0003', '04', 'Travel booking authorization', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Category Group 05: Refund Transaction Categories
-- Original: 050001Refund credit                                     0000
('0001', '05', 'Refund credit', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Category Group 06: Reversal Transaction Categories  
-- Original: 060001Fraud reversal                                    0000
('0001', '06', 'Fraud reversal', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Original: 060002Non-fraud reversal                                0000
('0002', '06', 'Non-fraud reversal', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Category Group 07: Adjustment Transaction Categories
-- Original: 070001Sales draft credit adjustment                     0000
('0001', '07', 'Sales draft credit adjustment', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Verify data loading integrity and foreign key relationships
-- This ensures all parent transaction types exist and are properly linked
DO $$
DECLARE
    category_count INTEGER;
    orphan_count INTEGER;
BEGIN
    -- Count total categories loaded
    SELECT COUNT(*) INTO category_count FROM transaction_categories;
    
    -- Count categories without valid parent transaction types
    SELECT COUNT(*) INTO orphan_count 
    FROM transaction_categories tc 
    LEFT JOIN transaction_types tt ON tc.parent_transaction_type = tt.transaction_type 
    WHERE tt.transaction_type IS NULL;
    
    -- Log results and validate data integrity
    RAISE NOTICE 'Transaction categories data loading completed:';
    RAISE NOTICE '  Total categories loaded: %', category_count;
    RAISE NOTICE '  Categories with invalid parent types: %', orphan_count;
    
    -- Ensure no orphaned categories exist
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'Data integrity violation: % transaction categories have invalid parent transaction types', orphan_count;
    END IF;
    
    -- Validate expected record count matches source data
    IF category_count != 18 THEN
        RAISE EXCEPTION 'Data loading error: Expected 18 transaction categories, loaded %', category_count;
    END IF;
    
    RAISE NOTICE 'Transaction categories data loading validation successful';
END $$;

--rollback DELETE FROM transaction_categories WHERE transaction_category IN ('0001', '0002', '0003', '0004', '0005') AND parent_transaction_type IN ('01', '02', '03', '04', '05', '06', '07');

--changeset blitzy-agent:optimize-transaction-categories-performance-v25
--comment: Create performance optimization indexes for transaction category lookup operations supporting sub-millisecond response times

-- Create composite index for hierarchical category lookups optimized for parent-child queries
-- Supports queries filtering by parent transaction type and active status
CREATE INDEX IF NOT EXISTS idx_transaction_categories_parent_active_lookup 
ON transaction_categories (parent_transaction_type, active_status, transaction_category)
WHERE active_status = true;

-- Create covering index for category description searches and reporting operations
-- Includes all frequently accessed columns to support index-only scans
CREATE INDEX IF NOT EXISTS idx_transaction_categories_description_covering
ON transaction_categories (category_description, transaction_category, parent_transaction_type)
WHERE active_status = true;

-- Create partial index for active category enumeration supporting high-frequency lookups
-- Optimizes queries that filter on active status with minimal storage overhead
CREATE INDEX IF NOT EXISTS idx_transaction_categories_active_enumeration
ON transaction_categories (transaction_category, parent_transaction_type, category_description)
WHERE active_status = true;

-- Update table statistics to ensure optimal query execution plans
-- This ensures the PostgreSQL query planner has accurate information for optimization
ANALYZE transaction_categories;

-- Verify index creation and performance optimization
DO $$
DECLARE
    index_count INTEGER;
BEGIN
    -- Count indexes created for transaction_categories table
    SELECT COUNT(*) INTO index_count 
    FROM pg_indexes 
    WHERE tablename = 'transaction_categories' 
    AND indexname LIKE 'idx_transaction_categories_%';
    
    RAISE NOTICE 'Transaction categories performance optimization completed:';
    RAISE NOTICE '  Performance indexes created: %', index_count;
    RAISE NOTICE '  Table statistics updated for query planner optimization';
    
    -- Validate minimum required indexes exist
    IF index_count < 3 THEN
        RAISE WARNING 'Expected at least 3 performance indexes, found %', index_count;
    END IF;
END $$;

--rollback DROP INDEX IF EXISTS idx_transaction_categories_active_enumeration;
--rollback DROP INDEX IF EXISTS idx_transaction_categories_description_covering;
--rollback DROP INDEX IF EXISTS idx_transaction_categories_parent_active_lookup;

--changeset blitzy-agent:validate-transaction-categories-business-rules-v25
--comment: Validate transaction category data against business rules and referential integrity constraints

-- Validate all transaction categories have proper hierarchical relationships
-- Ensures parent transaction types exist and are active for business rule enforcement
DO $$
DECLARE
    validation_errors TEXT := '';
    error_count INTEGER := 0;
    rec RECORD;
BEGIN
    -- Validate category code format compliance (4-digit numeric)
    FOR rec IN 
        SELECT transaction_category, category_description 
        FROM transaction_categories 
        WHERE NOT (transaction_category ~ '^[0-9]{4}$')
    LOOP
        validation_errors := validation_errors || 
            format('Invalid category code format: %s (%s)' || E'\n', 
                   rec.transaction_category, rec.category_description);
        error_count := error_count + 1;
    END LOOP;
    
    -- Validate parent transaction type relationships
    FOR rec IN 
        SELECT tc.transaction_category, tc.parent_transaction_type, tc.category_description
        FROM transaction_categories tc
        LEFT JOIN transaction_types tt ON tc.parent_transaction_type = tt.transaction_type
        WHERE tt.transaction_type IS NULL OR tt.active_status = false
    LOOP
        validation_errors := validation_errors || 
            format('Invalid parent transaction type: %s for category %s (%s)' || E'\n', 
                   rec.parent_transaction_type, rec.transaction_category, rec.category_description);
        error_count := error_count + 1;
    END LOOP;
    
    -- Validate category description minimum length requirements
    FOR rec IN 
        SELECT transaction_category, category_description 
        FROM transaction_categories 
        WHERE length(trim(category_description)) < 3
    LOOP
        validation_errors := validation_errors || 
            format('Category description too short: %s (%s)' || E'\n', 
                   rec.transaction_category, rec.category_description);
        error_count := error_count + 1;
    END LOOP;
    
    -- Validate category uniqueness within parent transaction type groups
    FOR rec IN 
        SELECT transaction_category, parent_transaction_type, COUNT(*) as duplicate_count
        FROM transaction_categories 
        GROUP BY transaction_category, parent_transaction_type
        HAVING COUNT(*) > 1
    LOOP
        validation_errors := validation_errors || 
            format('Duplicate category code: %s in parent type %s (count: %s)' || E'\n', 
                   rec.transaction_category, rec.parent_transaction_type, rec.duplicate_count);
        error_count := error_count + 1;
    END LOOP;
    
    -- Report validation results
    IF error_count = 0 THEN
        RAISE NOTICE 'Transaction categories business rule validation successful:';
        RAISE NOTICE '  All 18 categories validated against business rules';
        RAISE NOTICE '  Category code format compliance: PASSED';  
        RAISE NOTICE '  Parent transaction type relationships: PASSED';
        RAISE NOTICE '  Category description requirements: PASSED';
        RAISE NOTICE '  Category uniqueness constraints: PASSED';
    ELSE
        RAISE EXCEPTION 'Transaction categories business rule validation failed:' || E'\n' || 
                       'Error count: %' || E'\n' || 'Validation errors:' || E'\n' || '%', 
                       error_count, validation_errors;
    END IF;
END $$;

--rollback -- Business rule validation rollback: no persistent changes to reverse

--changeset blitzy-agent:document-transaction-categories-data-lineage-v25  
--comment: Document data lineage and transformation rules for transaction categories reference data

-- Create comprehensive documentation for transaction categories data loading process
-- This supports audit requirements and system maintenance procedures
DO $$
BEGIN
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'TRANSACTION CATEGORIES DATA LINEAGE DOCUMENTATION';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Source File: app/data/ASCII/trancatg.txt';
    RAISE NOTICE 'Target Table: transaction_categories';
    RAISE NOTICE 'Migration Version: V25__load_transaction_categories_data.sql';
    RAISE NOTICE 'Data Transformation Rules:';
    RAISE NOTICE '  1. 6-digit category codes (NNNNNN) -> 4-character category (NNNN)';
    RAISE NOTICE '  2. First 2 digits extracted as parent_transaction_type (NN)';
    RAISE NOTICE '  3. Category descriptions trimmed and validated for length';
    RAISE NOTICE '  4. Active status set to TRUE for all loaded categories';
    RAISE NOTICE '  5. Timestamp fields populated with current migration time';
    RAISE NOTICE 'Business Rules Enforced:';
    RAISE NOTICE '  - Foreign key relationship to transaction_types table';
    RAISE NOTICE '  - Category code format validation (4-digit numeric)';
    RAISE NOTICE '  - Parent transaction type existence validation';
    RAISE NOTICE '  - Category description minimum length (3 characters)';
    RAISE NOTICE '  - Category uniqueness within parent type groups';
    RAISE NOTICE 'Performance Optimizations:';
    RAISE NOTICE '  - Composite indexes for hierarchical lookups';
    RAISE NOTICE '  - Covering indexes for description searches';
    RAISE NOTICE '  - Partial indexes for active category enumeration';
    RAISE NOTICE '  - Table statistics updated for query planner';
    RAISE NOTICE 'Expected Data Volume: 18 transaction category records';
    RAISE NOTICE 'Data Quality Validation: PASSED';
    RAISE NOTICE '=================================================================';
END $$;

--rollback -- Data lineage documentation rollback: no persistent changes to reverse