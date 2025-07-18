-- =====================================================================================
-- Liquibase Rollback Script: R6__rollback_reference_tables.sql
-- Description: Rollback script to reverse V6__create_reference_tables.sql migration
--              Removes all reference tables, functions, triggers, and related objects
--              created for transaction types, categories, disclosure groups, and
--              transaction category balances, enabling complete rollback of VSAM
--              to PostgreSQL reference data transformation
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 6.0
-- Rollback Target: V6__create_reference_tables.sql
-- =====================================================================================

-- rollback changeset blitzy:V6-create-reference-tables
-- comment: Rollback reference tables creation and restore VSAM reference data functionality

-- =============================================================================
-- 1. DROP MATERIALIZED VIEW AND OPTIMIZATION STRUCTURES
-- =============================================================================

-- Drop materialized view indexes first to avoid dependency issues
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_interest_rate;
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_group_id;
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_transaction_category;
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_transaction_type;

-- Drop materialized view refresh function
DROP FUNCTION IF EXISTS refresh_reference_data_lookup();

-- Drop materialized view with CASCADE to handle any remaining dependencies
DROP MATERIALIZED VIEW IF EXISTS mv_reference_data_lookup CASCADE;

-- =============================================================================
-- 2. DROP BUSINESS OPERATION FUNCTIONS
-- =============================================================================

-- Drop business functions for reference data operations
DROP FUNCTION IF EXISTS update_category_balance(VARCHAR(11), VARCHAR(4), DECIMAL(12,2));
DROP FUNCTION IF EXISTS get_account_interest_rate(VARCHAR(11));
DROP FUNCTION IF EXISTS get_active_transaction_categories();
DROP FUNCTION IF EXISTS get_active_transaction_types();

-- =============================================================================
-- 3. DROP TRIGGERS AND TRIGGER FUNCTIONS
-- =============================================================================

-- Drop triggers first to avoid dependency issues with functions
DROP TRIGGER IF EXISTS transaction_category_balances_last_updated_trigger ON transaction_category_balances;
DROP TRIGGER IF EXISTS disclosure_groups_updated_at_trigger ON disclosure_groups;
DROP TRIGGER IF EXISTS transaction_categories_updated_at_trigger ON transaction_categories;
DROP TRIGGER IF EXISTS transaction_types_updated_at_trigger ON transaction_types;

-- Drop trigger functions after triggers are removed
DROP FUNCTION IF EXISTS update_transaction_category_balances_last_updated();
DROP FUNCTION IF EXISTS update_disclosure_groups_updated_at();
DROP FUNCTION IF EXISTS update_transaction_categories_updated_at();
DROP FUNCTION IF EXISTS update_transaction_types_updated_at();

-- =============================================================================
-- 4. DROP REFERENCE TABLES IN DEPENDENCY ORDER
-- =============================================================================

-- Drop transaction_category_balances table first (has foreign key dependencies)
-- This table depends on both accounts and transaction_categories tables
DROP TABLE IF EXISTS transaction_category_balances CASCADE;

-- Drop disclosure_groups table (referenced by accounts table group_id)
-- Use CASCADE to handle any remaining foreign key references
DROP TABLE IF EXISTS disclosure_groups CASCADE;

-- Drop transaction_categories table (referenced by transaction_category_balances)
-- Use CASCADE to handle any remaining foreign key references
DROP TABLE IF EXISTS transaction_categories CASCADE;

-- Drop transaction_types table (standalone reference table)
-- Use CASCADE to handle any remaining foreign key references
DROP TABLE IF EXISTS transaction_types CASCADE;

-- =============================================================================
-- 5. ROLLBACK VALIDATION AND CLEANUP
-- =============================================================================

-- Verify all reference tables have been dropped
DO $$
BEGIN
    -- Check if any reference tables still exist
    IF EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_name IN ('transaction_types', 'transaction_categories', 
                                   'disclosure_groups', 'transaction_category_balances')
               AND table_schema = 'public') THEN
        RAISE EXCEPTION 'Reference tables rollback incomplete - some tables still exist';
    END IF;
    
    -- Check if any trigger functions still exist
    IF EXISTS (SELECT 1 FROM information_schema.routines 
               WHERE routine_name LIKE '%transaction_types%' 
               OR routine_name LIKE '%transaction_categories%'
               OR routine_name LIKE '%disclosure_groups%'
               OR routine_name LIKE '%transaction_category_balances%') THEN
        RAISE EXCEPTION 'Reference tables rollback incomplete - some functions still exist';
    END IF;
    
    -- Log successful rollback
    RAISE NOTICE 'Reference tables rollback completed successfully';
    RAISE NOTICE 'VSAM reference data functionality can now be restored';
    RAISE NOTICE 'Tables dropped: transaction_types, transaction_categories, disclosure_groups, transaction_category_balances';
    RAISE NOTICE 'Functions and triggers cleaned up successfully';
END $$;

-- =============================================================================
-- 6. VSAM REFERENCE DATA RESTORATION NOTES
-- =============================================================================

-- Post-rollback restoration requirements for VSAM reference data functionality:
-- 
-- 1. TRANTYPE VSAM Dataset Restoration:
--    - Restore original TRANTYPE VSAM KSDS with 2-character keys
--    - Verify transaction type codes 01-07 are available
--    - Restore COBOL program access patterns for transaction classification
--
-- 2. TRANCATG VSAM Dataset Restoration:
--    - Restore original TRANCATG VSAM KSDS with 4-character keys
--    - Verify hierarchical category codes 0100-0700 are available
--    - Restore COBOL program access patterns for transaction categorization
--
-- 3. DISCGRP VSAM Dataset Restoration:
--    - Restore original DISCGRP VSAM KSDS with group identifiers
--    - Verify interest rate management functionality
--    - Restore COBOL program access patterns for disclosure group processing
--
-- 4. TCATBAL VSAM Dataset Restoration:
--    - Restore original TCATBAL VSAM KSDS with composite keys
--    - Verify account-category balance tracking functionality
--    - Restore COBOL program access patterns for category balance management
--
-- 5. Cross-Reference Index Restoration:
--    - Restore VSAM alternate index structures for lookup optimization
--    - Verify sub-millisecond response time requirements are met
--    - Restore COBOL program access patterns for reference data queries

-- =============================================================================
-- 7. ROLLBACK COMPLETION CONFIRMATION
-- =============================================================================

-- Log rollback completion for audit trail
INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, 
                               exectype, md5sum, description, comments, tag, liquibase)
VALUES ('R6-rollback-reference-tables', 'blitzy', 'rollback/R6__rollback_reference_tables.sql', 
        CURRENT_TIMESTAMP, 
        (SELECT COALESCE(MAX(orderexecuted), 0) + 1 FROM databasechangelog),
        'ROLLBACK', 
        'rollback-reference-tables-md5', 
        'Rollback reference tables creation - restore VSAM reference data functionality',
        'Complete rollback of V6__create_reference_tables.sql migration with CASCADE cleanup',
        'rollback-v6', 
        '4.25.0')
ON CONFLICT (id, author, filename) DO UPDATE SET
    dateexecuted = CURRENT_TIMESTAMP,
    exectype = 'ROLLBACK',
    comments = 'Complete rollback of V6__create_reference_tables.sql migration with CASCADE cleanup';

-- Final confirmation message
SELECT 
    'Reference tables rollback completed successfully' as status,
    'VSAM reference data functionality ready for restoration' as next_action,
    'All PostgreSQL reference tables and dependencies removed' as result,
    CURRENT_TIMESTAMP as rollback_timestamp;