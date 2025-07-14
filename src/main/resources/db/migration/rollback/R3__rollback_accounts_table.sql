-- ============================================================================
-- Liquibase Rollback Migration: R3__rollback_accounts_table.sql
-- Description: Complete rollback of V3__create_accounts_table.sql migration
-- Author: Blitzy agent
-- Version: 3.0-rollback
-- Target Migration: V3__create_accounts_table.sql
-- Purpose: Reverse ACCTDAT VSAM to PostgreSQL accounts table transformation
-- ============================================================================

-- ============================================================================
-- ROLLBACK VALIDATION AND SAFETY CHECKS
-- ============================================================================

-- Ensure this script only runs if the accounts table exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                  WHERE table_schema = 'public' AND table_name = 'accounts') THEN
        RAISE EXCEPTION 'ROLLBACK ERROR: accounts table does not exist. Migration V3 may not have been applied.';
    END IF;
    
    -- Log rollback initiation for audit trail
    RAISE NOTICE 'ROLLBACK INITIATED: Rolling back V3__create_accounts_table.sql migration';
    RAISE NOTICE 'ROLLBACK TARGET: Removing accounts table and all associated database objects';
END $$;

-- ============================================================================
-- STEP 1: DROP TRIGGERS (Highest Dependency Level)
-- ============================================================================

-- Drop account balance validation trigger
-- Removes business rule enforcement for balance and cycle validation
DROP TRIGGER IF EXISTS trg_accounts_balance_validation ON accounts;
RAISE NOTICE 'ROLLBACK STEP 1A: Dropped trigger trg_accounts_balance_validation';

-- Drop automatic timestamp update trigger
-- Removes automatic updated_at timestamp maintenance
DROP TRIGGER IF EXISTS trg_accounts_updated_at ON accounts;
RAISE NOTICE 'ROLLBACK STEP 1B: Dropped trigger trg_accounts_updated_at';

-- ============================================================================
-- STEP 2: DROP FUNCTIONS (Second Highest Dependency Level)
-- ============================================================================

-- Drop account balance validation function
-- Removes financial validation logic for account balance updates
DROP FUNCTION IF EXISTS validate_account_balance_update() CASCADE;
RAISE NOTICE 'ROLLBACK STEP 2A: Dropped function validate_account_balance_update()';

-- Drop timestamp update function
-- Removes automatic timestamp update functionality
DROP FUNCTION IF EXISTS update_accounts_updated_at() CASCADE;
RAISE NOTICE 'ROLLBACK STEP 2B: Dropped function update_accounts_updated_at()';

-- ============================================================================
-- STEP 3: DROP INDEXES (Third Dependency Level)
-- ============================================================================

-- Drop composite indexes for account analysis and reporting
DROP INDEX IF EXISTS idx_accounts_credit_limits;
RAISE NOTICE 'ROLLBACK STEP 3A: Dropped index idx_accounts_credit_limits';

DROP INDEX IF EXISTS idx_accounts_balance_status;
RAISE NOTICE 'ROLLBACK STEP 3B: Dropped index idx_accounts_balance_status';

-- Drop disclosure group index for interest rate management
DROP INDEX IF EXISTS idx_accounts_group_id;
RAISE NOTICE 'ROLLBACK STEP 3C: Dropped index idx_accounts_group_id';

-- Drop date-based lifecycle management indexes
DROP INDEX IF EXISTS idx_accounts_expiration_date;
RAISE NOTICE 'ROLLBACK STEP 3D: Dropped index idx_accounts_expiration_date';

DROP INDEX IF EXISTS idx_accounts_open_date;
RAISE NOTICE 'ROLLBACK STEP 3E: Dropped index idx_accounts_open_date';

-- Drop financial analysis indexes
DROP INDEX IF EXISTS idx_accounts_balance_range;
RAISE NOTICE 'ROLLBACK STEP 3F: Dropped index idx_accounts_balance_range';

-- Drop account status index for active account filtering
DROP INDEX IF EXISTS idx_accounts_active_status;
RAISE NOTICE 'ROLLBACK STEP 3G: Dropped index idx_accounts_active_status';

-- Drop customer-account relationship index (CXACAIX functionality)
DROP INDEX IF EXISTS idx_accounts_customer_id;
RAISE NOTICE 'ROLLBACK STEP 3H: Dropped index idx_accounts_customer_id';

-- ============================================================================
-- STEP 4: DROP TABLE WITH CASCADE (Final Step)
-- ============================================================================

-- Drop the accounts table with CASCADE to remove all dependent objects
-- This includes:
-- - All foreign key constraints (fk_accounts_customer_id, etc.)
-- - All check constraints (business rule validation)
-- - Primary key constraint (pk_accounts)
-- - Any remaining dependent objects
-- - All table comments and column comments
DROP TABLE IF EXISTS accounts CASCADE;
RAISE NOTICE 'ROLLBACK STEP 4: Dropped table accounts with CASCADE';

-- ============================================================================
-- ROLLBACK VALIDATION AND CLEANUP VERIFICATION
-- ============================================================================

-- Verify complete removal of accounts table and related objects
DO $$
DECLARE
    table_exists BOOLEAN;
    function_count INTEGER;
    trigger_count INTEGER;
BEGIN
    -- Check if accounts table still exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'accounts'
    ) INTO table_exists;
    
    IF table_exists THEN
        RAISE EXCEPTION 'ROLLBACK VERIFICATION FAILED: accounts table still exists after rollback';
    END IF;
    
    -- Check if related functions still exist
    SELECT COUNT(*) INTO function_count
    FROM information_schema.routines 
    WHERE routine_schema = 'public' 
    AND routine_name IN ('update_accounts_updated_at', 'validate_account_balance_update');
    
    IF function_count > 0 THEN
        RAISE EXCEPTION 'ROLLBACK VERIFICATION FAILED: % account-related functions still exist', function_count;
    END IF;
    
    -- Verification successful
    RAISE NOTICE 'ROLLBACK VERIFICATION SUCCESSFUL: All accounts table objects removed';
    RAISE NOTICE 'ROLLBACK COMPLETE: ACCTDAT VSAM to PostgreSQL transformation reversed';
END $$;

-- ============================================================================
-- ROLLBACK COMPLETION SUMMARY
-- ============================================================================

-- Log successful rollback completion for audit trail
DO $$
BEGIN
    RAISE NOTICE '================================================================================';
    RAISE NOTICE 'ROLLBACK SUMMARY: V3__create_accounts_table.sql Successfully Reversed';
    RAISE NOTICE '================================================================================';
    RAISE NOTICE 'Removed Components:';
    RAISE NOTICE '  - accounts table with DECIMAL(12,2) financial precision fields';
    RAISE NOTICE '  - Primary key constraint on account_id';
    RAISE NOTICE '  - Foreign key relationship to customers table (fk_accounts_customer_id)';
    RAISE NOTICE '  - 8 business rule validation check constraints';
    RAISE NOTICE '  - 8 performance optimization indexes';
    RAISE NOTICE '  - 2 database functions for validation and timestamp management';
    RAISE NOTICE '  - 2 database triggers for automatic field maintenance';
    RAISE NOTICE '  - All table and column documentation comments';
    RAISE NOTICE '================================================================================';
    RAISE NOTICE 'Financial Precision Rollback:';
    RAISE NOTICE '  - Removed DECIMAL(12,2) fields for COBOL COMP-3 equivalence';
    RAISE NOTICE '  - Reversed current_balance, credit_limit, cash_credit_limit fields';
    RAISE NOTICE '  - Removed current_cycle_credit and current_cycle_debit calculation fields';
    RAISE NOTICE '================================================================================';
    RAISE NOTICE 'VSAM ACCTDAT Dataset Restoration Notes:';
    RAISE NOTICE '  - PostgreSQL accounts table transformation completely reversed';
    RAISE NOTICE '  - Customer-account relationship cleanup completed';
    RAISE NOTICE '  - Disclosure group association cleanup completed';
    RAISE NOTICE '  - Account lifecycle management fields removed';
    RAISE NOTICE '  - Financial calculation baseline restored to pre-migration state';
    RAISE NOTICE '================================================================================';
    RAISE NOTICE 'Database State: Ready for V3 migration re-application if needed';
    RAISE NOTICE 'Next Steps: Verify application functionality with VSAM ACCTDAT dataset';
    RAISE NOTICE '================================================================================';
END $$;

-- ============================================================================
-- ROLLBACK METADATA FOR LIQUIBASE TRACKING
-- ============================================================================

-- This rollback script supports:
-- - Complete reversal of accounts table creation
-- - Cleanup of all financial precision DECIMAL(12,2) fields
-- - Removal of customer foreign key relationships
-- - Restoration of database baseline state for VSAM ACCTDAT functionality
-- - Comprehensive validation and error handling
-- - Detailed audit logging for rollback operations

-- Performance Impact: Minimal (removing objects is faster than creating them)
-- Data Loss Warning: This rollback will permanently delete all account data
-- Recovery: Data can be restored from VSAM ACCTDAT dataset if needed
-- Dependencies: Ensure no other tables reference accounts table before rollback