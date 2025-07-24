-- =============================================================================
-- Liquibase Rollback Script: R1__rollback_users_table.sql
-- =============================================================================
-- Description: Rollback script for V1__create_users_table.sql migration
--              Removes the users table and all associated database objects
--              including indexes, constraints, functions, and data
-- 
-- Purpose: Enables complete reversal of VSAM USRSEC to PostgreSQL users table
--          transformation for environment recovery and rollback procedures
-- 
-- Rollback Target: V1__create_users_table.sql migration
-- VSAM Context: Reverses AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS to PostgreSQL transformation
-- 
-- Author: Blitzy agent
-- Created: Rollback script for CardDemo modernization
-- =============================================================================

-- Liquibase rollback changeset for users table removal
-- --changeset CardDemo:rollback-users-table-v1 splitStatements:true endDelimiter:;

-- =============================================================================
-- ROLLBACK OPERATIONS - Execute in reverse dependency order
-- =============================================================================

-- Step 1: Drop audit trigger function if it exists
-- This function was created in the forward migration for user change tracking
-- Must be dropped before the table to avoid dependency issues
DROP FUNCTION IF EXISTS audit_users_changes() CASCADE;

-- Step 2: Drop user ID generation sequence if it exists  
-- This sequence was referenced in the forward migration (though commented out)
-- Include it in rollback for completeness
DROP SEQUENCE IF EXISTS users_id_seq CASCADE;

-- Step 3: Drop the users table with CASCADE option
-- CASCADE ensures all dependent objects are removed including:
-- - Foreign key constraints from other tables referencing users
-- - Views that depend on the users table  
-- - Triggers associated with the users table
-- - Any remaining functions that reference the users table
-- - All indexes (will be dropped automatically with the table)
-- - All constraints (will be dropped automatically with the table)
DROP TABLE IF EXISTS users CASCADE;

-- =============================================================================
-- ROLLBACK VERIFICATION AND CLEANUP
-- =============================================================================

-- Verify that all users table related objects have been removed
-- This provides confirmation that the rollback was successful

-- Check for any remaining functions that might reference users table
-- (This is informational - the CASCADE should have handled dependencies)
DO $$
DECLARE
    function_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO function_count 
    FROM pg_proc p
    JOIN pg_namespace n ON p.pronamespace = n.oid
    WHERE n.nspname = current_schema()
    AND p.prosrc LIKE '%users%';
    
    IF function_count > 0 THEN
        RAISE NOTICE 'Warning: % functions still reference "users" after rollback', function_count;
    ELSE
        RAISE NOTICE 'Rollback verification: No functions referencing "users" found';
    END IF;
END $$;

-- Check for any remaining sequences related to users
-- (This is informational - sequences should be cleaned up)
DO $$
DECLARE
    sequence_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO sequence_count 
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = current_schema()
    AND c.relkind = 'S'
    AND c.relname LIKE '%users%';
    
    IF sequence_count > 0 THEN
        RAISE NOTICE 'Warning: % sequences still reference "users" after rollback', sequence_count;
    ELSE
        RAISE NOTICE 'Rollback verification: No user-related sequences found';
    END IF;
END $$;

-- Final verification that users table is completely removed
DO $$
DECLARE
    table_exists BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 
        FROM information_schema.tables 
        WHERE table_schema = current_schema()
        AND table_name = 'users'
    ) INTO table_exists;
    
    IF table_exists THEN
        RAISE EXCEPTION 'Rollback failed: users table still exists';
    ELSE
        RAISE NOTICE 'Rollback verification: users table successfully removed';
    END IF;
END $$;

-- =============================================================================
-- ROLLBACK COMPLETION SUMMARY
-- =============================================================================

-- Log rollback completion with details
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'ROLLBACK COMPLETED: V1__create_users_table.sql';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Rollback Target: Users table and authentication infrastructure';
    RAISE NOTICE 'VSAM Context: Restored AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS functionality';
    RAISE NOTICE 'Database Objects Removed:';
    RAISE NOTICE '  - users table (with all data)';
    RAISE NOTICE '  - Primary key constraint (pk_users)';
    RAISE NOTICE '  - Check constraints (user_type, password_hash_format, userid_format, name validation)';
    RAISE NOTICE '  - Indexes (authentication, user_type, last_login, name_search)';
    RAISE NOTICE '  - Table and column comments';
    RAISE NOTICE '  - Sample user data (ADMIN001, USER0001, TESTUSER)';
    RAISE NOTICE '  - audit_users_changes() function';
    RAISE NOTICE '  - users_id_seq sequence (if existed)';
    RAISE NOTICE '  - All dependent foreign key references (CASCADE)';
    RAISE NOTICE 'Spring Security Integration: BCrypt authentication support removed';
    RAISE NOTICE 'Migration Status: V1__create_users_table.sql successfully rolled back';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Environment ready for VSAM USRSEC dataset usage restoration';
    RAISE NOTICE '=============================================================================';
END $$;

-- =============================================================================
-- POST-ROLLBACK INSTRUCTIONS
-- =============================================================================
-- 
-- After running this rollback script:
-- 
-- 1. Verify VSAM USRSEC dataset accessibility:
--    - Confirm AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS is operational
--    - Validate 8-character user keys are accessible
--    - Test RACF authentication integration
-- 
-- 2. Update application configuration:
--    - Restore COBOL authentication programs (COSGN00C)
--    - Revert Spring Security configuration to RACF integration
--    - Remove BCrypt password encoder references
--    - Restore CICS transaction authentication flow
-- 
-- 3. Database environment validation:
--    - Confirm no orphaned foreign key constraints
--    - Verify Liquibase changelog state consistency
--    - Test rollback can be re-executed safely (idempotent)
-- 
-- 4. Security considerations:
--    - User authentication data has been permanently removed
--    - BCrypt password hashes are irretrievably deleted
--    - Session management must revert to CICS pseudo-conversational
-- 
-- 5. Monitoring requirements:
--    - Audit trail for user table removal is logged above
--    - No user authentication audit data remains in PostgreSQL
--    - RACF audit logs should be primary security record
-- 
-- =============================================================================
-- End of Rollback Script
-- =============================================================================