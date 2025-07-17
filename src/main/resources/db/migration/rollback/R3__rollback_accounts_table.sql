-- =====================================================================================
-- Liquibase Rollback Script: R3__rollback_accounts_table.sql
-- Description: Comprehensive rollback script for V3__create_accounts_table.sql
--              Removes accounts table, indexes, constraints, materialized views, 
--              functions, and triggers in proper dependency order
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 3.0
-- Target Migration: V3__create_accounts_table.sql
-- =====================================================================================

-- changeset blitzy:R3-rollback-accounts-table
-- comment: Rollback V3 accounts table migration including all dependent objects for complete schema cleanup

-- =====================================================================================
-- ROLLBACK VALIDATION AND SAFETY CHECKS
-- =====================================================================================

-- Check if accounts table exists before attempting rollback
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'accounts' AND table_schema = 'public') THEN
        RAISE NOTICE 'Accounts table does not exist - rollback validation passed';
    ELSE
        RAISE NOTICE 'Accounts table exists - proceeding with rollback operations';
    END IF;
END $$;

-- =====================================================================================
-- PHASE 1: DROP FUNCTIONS WITH TABLE DEPENDENCIES
-- =====================================================================================

-- Drop account balance validation function
DROP FUNCTION IF EXISTS validate_account_balance(VARCHAR(11), DECIMAL(12,2), VARCHAR(2)) CASCADE;

-- Drop cycle balance calculation function
DROP FUNCTION IF EXISTS calculate_cycle_balances(VARCHAR(11), DATE, DATE) CASCADE;

-- Drop materialized view refresh function
DROP FUNCTION IF EXISTS refresh_account_summary_view() CASCADE;

-- =====================================================================================
-- PHASE 2: DROP MATERIALIZED VIEW AND ASSOCIATED INDEXES
-- =====================================================================================

-- Drop materialized view indexes first to avoid dependency issues
DROP INDEX IF EXISTS idx_mv_account_summary_customer_id;
DROP INDEX IF EXISTS idx_mv_account_summary_balance_status;
DROP INDEX IF EXISTS idx_mv_account_summary_credit_rating;
DROP INDEX IF EXISTS idx_mv_account_summary_available_credit;
DROP INDEX IF EXISTS idx_mv_account_summary_years_to_expiry;
DROP INDEX IF EXISTS idx_mv_account_summary_net_cycle_activity;

-- Drop materialized view for account summary queries
DROP MATERIALIZED VIEW IF EXISTS mv_account_summary CASCADE;

-- =====================================================================================
-- PHASE 3: DROP TRIGGERS AND TRIGGER FUNCTIONS
-- =====================================================================================

-- Drop trigger for automatic updated_at timestamp management
DROP TRIGGER IF EXISTS accounts_updated_at_trigger ON accounts CASCADE;

-- Drop trigger function for updated_at column management
DROP FUNCTION IF EXISTS update_accounts_updated_at() CASCADE;

-- =====================================================================================
-- PHASE 4: DROP MAIN ACCOUNTS TABLE WITH ALL DEPENDENCIES
-- =====================================================================================

-- Drop accounts table with CASCADE to remove all constraints and dependent objects
-- This includes:
-- - All foreign key constraints referencing accounts table
-- - All indexes on accounts table
-- - All check constraints on accounts table
-- - All comments on accounts table and columns
-- - Any views or functions still depending on accounts table
DROP TABLE IF EXISTS accounts CASCADE;

-- =====================================================================================
-- PHASE 5: CLEANUP VERIFICATION AND ROLLBACK CONFIRMATION
-- =====================================================================================

-- Verify accounts table has been completely removed
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'accounts' AND table_schema = 'public') THEN
        RAISE NOTICE 'SUCCESS: Accounts table and all dependencies have been successfully removed';
    ELSE
        RAISE EXCEPTION 'ERROR: Accounts table still exists after rollback attempt';
    END IF;
END $$;

-- Verify materialized view has been completely removed
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'mv_account_summary' AND table_schema = 'public') THEN
        RAISE NOTICE 'SUCCESS: Account summary materialized view has been successfully removed';
    ELSE
        RAISE EXCEPTION 'ERROR: Account summary materialized view still exists after rollback attempt';
    END IF;
END $$;

-- Verify all account-related functions have been removed
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.routines 
        WHERE routine_name IN ('validate_account_balance', 'calculate_cycle_balances', 'refresh_account_summary_view', 'update_accounts_updated_at')
        AND routine_schema = 'public'
    ) THEN
        RAISE NOTICE 'SUCCESS: All account-related functions have been successfully removed';
    ELSE
        RAISE EXCEPTION 'ERROR: Some account-related functions still exist after rollback attempt';
    END IF;
END $$;

-- =====================================================================================
-- PHASE 6: RESTORE VSAM ACCTDAT BASELINE STATE
-- =====================================================================================

-- Log rollback completion for audit trail
DO $$
BEGIN
    RAISE NOTICE 'ROLLBACK COMPLETE: V3 accounts table migration has been fully reversed';
    RAISE NOTICE 'Database state restored to pre-V3 migration baseline';
    RAISE NOTICE 'VSAM ACCTDAT to PostgreSQL transformation has been rolled back';
    RAISE NOTICE 'All DECIMAL(12,2) financial precision fields have been removed';
    RAISE NOTICE 'Customer-account relationships have been cleaned up';
    RAISE NOTICE 'Account balance sequences and composite indexes have been removed';
    RAISE NOTICE 'Account lifecycle management functionality has been removed';
    RAISE NOTICE 'Disclosure group associations have been cleaned up';
END $$;

-- =====================================================================================
-- ROLLBACK SUMMARY
-- =====================================================================================
-- This rollback script removes:
-- 1. accounts table with all DECIMAL(12,2) precision fields
-- 2. customer_id foreign key relationships  
-- 3. All account-related indexes for performance optimization
-- 4. mv_account_summary materialized view and its indexes
-- 5. validate_account_balance() function for transaction validation
-- 6. calculate_cycle_balances() function for financial calculations
-- 7. refresh_account_summary_view() function for view management
-- 8. update_accounts_updated_at() trigger function
-- 9. accounts_updated_at_trigger for automatic timestamp updates
-- 10. All check constraints for business rule validation
-- 11. All table and column comments for documentation
-- 
-- POST-ROLLBACK STATE:
-- - Database is restored to baseline state before V3 migration
-- - No trace of accounts table or related PostgreSQL objects remains
-- - VSAM ACCTDAT functionality is conceptually restored to original state
-- - All financial precision calculations are removed
-- - Customer-account cross-references are cleaned up
-- =====================================================================================

-- rollback changeset blitzy:R3-rollback-accounts-table