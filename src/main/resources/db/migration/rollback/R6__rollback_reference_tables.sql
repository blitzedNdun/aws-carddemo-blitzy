-- ============================================================================
-- Liquibase Rollback Script: R6__rollback_reference_tables.sql
-- Description: Rollback script for V6__create_reference_tables.sql migration
--              Reverses creation of all reference tables including transaction 
--              types, categories, disclosure groups, and transaction category balances
-- Author: Blitzy agent
-- Version: 6.0 (rollback)
-- Dependencies: Must be executed to rollback V6__create_reference_tables.sql
-- ============================================================================

-- Begin rollback transaction for atomic execution
BEGIN;

-- ============================================================================
-- Step 1: Drop materialized views with CASCADE to remove dependent objects
-- ============================================================================

-- Drop account category balance summary materialized view
DROP MATERIALIZED VIEW IF EXISTS mv_account_category_balance_summary CASCADE;

-- Drop transaction type category hierarchy materialized view
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_type_category_hierarchy CASCADE;

-- ============================================================================
-- Step 2: Drop all tables with CASCADE to remove foreign key constraints
-- ============================================================================

-- Drop transaction_category_balances table (composite primary key table)
-- CASCADE will remove all dependent constraints and indexes
DROP TABLE IF EXISTS transaction_category_balances CASCADE;

-- Drop disclosure_groups table (interest rate management)
-- CASCADE will remove all dependent constraints and indexes
DROP TABLE IF EXISTS disclosure_groups CASCADE;

-- Drop transaction_categories table (hierarchical categorization)
-- CASCADE will remove all dependent constraints and indexes
DROP TABLE IF EXISTS transaction_categories CASCADE;

-- Drop transaction_types table (base transaction classification)
-- CASCADE will remove all dependent constraints and indexes
DROP TABLE IF EXISTS transaction_types CASCADE;

-- ============================================================================
-- Step 3: Drop sequences used for reference data management
-- ============================================================================

-- Drop transaction categories sequence
DROP SEQUENCE IF EXISTS transaction_categories_seq CASCADE;

-- Drop transaction types sequence
DROP SEQUENCE IF EXISTS transaction_types_seq CASCADE;

-- ============================================================================
-- Step 4: Drop trigger functions for automatic timestamp maintenance
-- ============================================================================

-- Drop transaction category balances validation trigger function
DROP FUNCTION IF EXISTS validate_transaction_category_balance_update() CASCADE;

-- Drop transaction category balances updated_at trigger function
DROP FUNCTION IF EXISTS update_transaction_category_balances_updated_at() CASCADE;

-- Drop disclosure groups updated_at trigger function
DROP FUNCTION IF EXISTS update_disclosure_groups_updated_at() CASCADE;

-- Drop transaction categories updated_at trigger function
DROP FUNCTION IF EXISTS update_transaction_categories_updated_at() CASCADE;

-- Drop transaction types updated_at trigger function
DROP FUNCTION IF EXISTS update_transaction_types_updated_at() CASCADE;

-- ============================================================================
-- Step 5: Verify rollback completion and cleanup
-- ============================================================================

-- Verify all reference tables have been dropped
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    -- Count remaining reference tables
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name IN (
        'transaction_types',
        'transaction_categories', 
        'disclosure_groups',
        'transaction_category_balances'
    );
    
    -- Log rollback verification result
    IF table_count = 0 THEN
        RAISE NOTICE 'Rollback verification: All reference tables successfully dropped';
    ELSE
        RAISE WARNING 'Rollback verification: % reference tables still exist', table_count;
    END IF;
END $$;

-- Verify all sequences have been dropped
DO $$
DECLARE
    sequence_count INTEGER;
BEGIN
    -- Count remaining reference sequences
    SELECT COUNT(*) INTO sequence_count
    FROM information_schema.sequences 
    WHERE sequence_schema = 'public' 
    AND sequence_name IN (
        'transaction_types_seq',
        'transaction_categories_seq'
    );
    
    -- Log sequence cleanup verification result
    IF sequence_count = 0 THEN
        RAISE NOTICE 'Rollback verification: All reference sequences successfully dropped';
    ELSE
        RAISE WARNING 'Rollback verification: % reference sequences still exist', sequence_count;
    END IF;
END $$;

-- Verify all trigger functions have been dropped
DO $$
DECLARE
    function_count INTEGER;
BEGIN
    -- Count remaining trigger functions
    SELECT COUNT(*) INTO function_count
    FROM information_schema.routines 
    WHERE routine_schema = 'public' 
    AND routine_name IN (
        'update_transaction_types_updated_at',
        'update_transaction_categories_updated_at',
        'update_disclosure_groups_updated_at',
        'update_transaction_category_balances_updated_at',
        'validate_transaction_category_balance_update'
    );
    
    -- Log function cleanup verification result
    IF function_count = 0 THEN
        RAISE NOTICE 'Rollback verification: All trigger functions successfully dropped';
    ELSE
        RAISE WARNING 'Rollback verification: % trigger functions still exist', function_count;
    END IF;
END $$;

-- Verify all materialized views have been dropped
DO $$
DECLARE
    view_count INTEGER;
BEGIN
    -- Count remaining materialized views
    SELECT COUNT(*) INTO view_count
    FROM pg_matviews 
    WHERE schemaname = 'public' 
    AND matviewname IN (
        'mv_transaction_type_category_hierarchy',
        'mv_account_category_balance_summary'
    );
    
    -- Log materialized view cleanup verification result
    IF view_count = 0 THEN
        RAISE NOTICE 'Rollback verification: All materialized views successfully dropped';
    ELSE
        RAISE WARNING 'Rollback verification: % materialized views still exist', view_count;
    END IF;
END $$;

-- ============================================================================
-- Step 6: Log rollback completion
-- ============================================================================

-- Log successful rollback completion
RAISE NOTICE 'Reference tables rollback completed successfully';
RAISE NOTICE 'VSAM reference data functionality restored to baseline state';
RAISE NOTICE 'All TRANTYPE, TRANCATG, DISCGRP, and TCATBAL table transformations reversed';

-- Commit the rollback transaction
COMMIT;

-- ============================================================================
-- Rollback Summary
-- ============================================================================
-- This rollback script has successfully reversed the following V6 migration changes:
--
-- 1. Dropped all reference tables:
--    - transaction_types (VSAM TRANTYPE equivalent)
--    - transaction_categories (VSAM TRANCATG equivalent)
--    - disclosure_groups (VSAM DISCGRP equivalent)
--    - transaction_category_balances (VSAM TCATBAL equivalent)
--
-- 2. Removed all foreign key constraints and relationships
--
-- 3. Dropped all performance optimization indexes
--
-- 4. Removed all check constraints for business rule validation
--
-- 5. Dropped all trigger functions for automatic timestamp maintenance
--
-- 6. Removed all sequences for reference data management
--
-- 7. Dropped all materialized views for high-performance lookup operations
--
-- 8. Restored system to pre-V6 migration state
--
-- The database is now ready for alternative reference data implementations
-- or re-execution of the V6 migration with modifications.
-- ============================================================================