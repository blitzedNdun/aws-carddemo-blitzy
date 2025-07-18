-- =====================================================================================
-- Liquibase Rollback Script: R4__rollback_cards_table.sql
-- Description: Comprehensive rollback script for cards table migration that reverses
--              all database objects created in V4__create_cards_table.sql including
--              Luhn validation constraints, security policies, and cross-reference 
--              functionality to enable complete CARDDAT VSAM restoration
-- Author: Blitzy Agent
-- Date: 2024  
-- Version: 4.0
-- Rollback Target: V4__create_cards_table.sql
-- =====================================================================================

-- changeset blitzy:R4-rollback-cards-table
-- comment: Rollback cards table creation and all associated database objects to enable complete CARDDAT VSAM restoration

-- =====================================================================================
-- STEP 1: Drop Row Level Security Policy and Disable RLS
-- =====================================================================================

-- Drop row level security policy for card data access
DROP POLICY IF EXISTS cards_access_policy ON cards;

-- Disable row level security on cards table
ALTER TABLE cards DISABLE ROW LEVEL SECURITY;

-- =====================================================================================
-- STEP 2: Drop Functions and Stored Procedures
-- =====================================================================================

-- Drop card number masking function (PCI DSS compliance)
DROP FUNCTION IF EXISTS mask_card_number(VARCHAR(16));

-- Drop card-account-customer relationship validation function
DROP FUNCTION IF EXISTS validate_card_relationships(VARCHAR(16), VARCHAR(11), VARCHAR(9));

-- Drop card status validation function
DROP FUNCTION IF EXISTS validate_card_status(VARCHAR(16));

-- Drop materialized view refresh function
DROP FUNCTION IF EXISTS refresh_card_summary_view();

-- Drop trigger function for updated_at timestamp
DROP FUNCTION IF EXISTS update_cards_updated_at();

-- Drop Luhn algorithm validation function
DROP FUNCTION IF EXISTS validate_luhn_algorithm(VARCHAR(16));

-- =====================================================================================
-- STEP 3: Drop Materialized Views and Associated Indexes
-- =====================================================================================

-- Drop materialized view with CASCADE to remove dependent objects
DROP MATERIALIZED VIEW IF EXISTS mv_card_summary CASCADE;

-- =====================================================================================
-- STEP 4: Drop Triggers
-- =====================================================================================

-- Drop trigger for automatic updated_at timestamp updates
DROP TRIGGER IF EXISTS cards_updated_at_trigger ON cards;

-- =====================================================================================
-- STEP 5: Drop Main Cards Table with CASCADE
-- =====================================================================================

-- Drop cards table with CASCADE to remove all dependent objects including:
-- - Foreign key constraints to accounts and customers tables
-- - Indexes (idx_cards_account_id, idx_cards_customer_id, etc.)
-- - CHECK constraints (cards_card_number_check, cards_cvv_code_check, etc.)
-- - NOT NULL constraints and validation rules
-- - Primary key constraint
-- - Comments and documentation
DROP TABLE IF EXISTS cards CASCADE;

-- =====================================================================================
-- STEP 6: Validation and Cleanup Verification
-- =====================================================================================

-- Verify cards table has been completely removed
DO $$
DECLARE
    table_exists BOOLEAN;
    function_count INTEGER;
    trigger_count INTEGER;
    policy_count INTEGER;
BEGIN
    -- Check if cards table still exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_name = 'cards' 
        AND table_schema = 'public'
    ) INTO table_exists;
    
    -- Check for remaining functions
    SELECT COUNT(*) INTO function_count
    FROM information_schema.routines
    WHERE routine_name IN (
        'validate_luhn_algorithm',
        'update_cards_updated_at',
        'refresh_card_summary_view',
        'validate_card_status',
        'validate_card_relationships',
        'mask_card_number'
    );
    
    -- Check for remaining triggers
    SELECT COUNT(*) INTO trigger_count
    FROM information_schema.triggers
    WHERE trigger_name = 'cards_updated_at_trigger';
    
    -- Check for remaining policies
    SELECT COUNT(*) INTO policy_count
    FROM pg_policies
    WHERE tablename = 'cards';
    
    -- Report rollback status
    IF table_exists THEN
        RAISE NOTICE 'WARNING: Cards table still exists after rollback attempt';
    ELSE
        RAISE NOTICE 'SUCCESS: Cards table has been successfully removed';
    END IF;
    
    IF function_count > 0 THEN
        RAISE NOTICE 'WARNING: % card-related functions still exist', function_count;
    ELSE
        RAISE NOTICE 'SUCCESS: All card-related functions have been removed';
    END IF;
    
    IF trigger_count > 0 THEN
        RAISE NOTICE 'WARNING: % card-related triggers still exist', trigger_count;
    ELSE
        RAISE NOTICE 'SUCCESS: All card-related triggers have been removed';
    END IF;
    
    IF policy_count > 0 THEN
        RAISE NOTICE 'WARNING: % card-related policies still exist', policy_count;
    ELSE
        RAISE NOTICE 'SUCCESS: All card-related policies have been removed';
    END IF;
    
    RAISE NOTICE 'Rollback R4__rollback_cards_table.sql completed successfully';
END $$;

-- =====================================================================================
-- STEP 7: Reset Database State for VSAM CARDDAT Restoration
-- =====================================================================================

-- Log rollback completion for audit trail
INSERT INTO public.migration_audit (
    migration_name,
    operation_type,
    operation_status,
    completion_timestamp,
    affected_objects
) VALUES (
    'R4__rollback_cards_table',
    'ROLLBACK',
    'COMPLETED',
    CURRENT_TIMESTAMP,
    'cards table, mv_card_summary, card validation functions, security policies'
) ON CONFLICT DO NOTHING;

-- =====================================================================================
-- ROLLBACK SUMMARY
-- =====================================================================================
-- Objects Removed:
-- 1. cards table (main table with all constraints, indexes, and foreign keys)
-- 2. mv_card_summary materialized view with all indexes
-- 3. validate_luhn_algorithm function (Luhn validation)
-- 4. update_cards_updated_at function and trigger
-- 5. refresh_card_summary_view function
-- 6. validate_card_status function
-- 7. validate_card_relationships function
-- 8. mask_card_number function (PCI DSS compliance)
-- 9. cards_access_policy row level security policy
-- 10. All indexes, constraints, and triggers associated with cards table
--
-- Post-Rollback State:
-- - Database restored to pre-V4 migration state
-- - Ready for VSAM CARDDAT dataset restoration
-- - All card-related database objects completely removed
-- - Foreign key references from other tables cleaned up
-- - Security policies and validation functions removed
-- =====================================================================================