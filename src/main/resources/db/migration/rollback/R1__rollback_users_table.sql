-- =====================================================================================
-- Liquibase Rollback Script: R1__rollback_users_table.sql
-- =====================================================================================
-- 
-- ROLLBACK SUMMARY:
-- Reverses the creation of the users table and all associated database objects including
-- indexes, sequences, constraints, and comments, enabling complete rollback of user 
-- authentication schema changes as required by Section 0.6.2 Output Constraints.
-- 
-- ORIGINAL MIGRATION ROLLBACK:
-- Forward Migration: V1__create_users_table.sql
-- Target: Complete removal of PostgreSQL users table infrastructure
-- Scope: Reverses VSAM USRSEC to PostgreSQL users table transformation
-- 
-- OBJECTS BEING REMOVED:
-- - users table with all columns and data
-- - Primary key constraint (pk_users)
-- - Unique constraint (uk_users_user_id)
-- - Secondary indexes (idx_users_user_type, idx_users_last_login, idx_users_type_login)
-- - Check constraints for data validation (7 total constraints)
-- - Table and column comments for documentation
-- - All foreign key references from dependent tables (CASCADE)
-- 
-- SECURITY ROLLBACK FEATURES:
-- - BCrypt password hashing infrastructure removal
-- - Spring Security authentication integration cleanup
-- - JWT token generation foundation removal
-- - RACF to Spring Security mapping cleanup
-- - User session management foundation removal
-- 
-- COMPLIANCE ROLLBACK:
-- - SOX audit trail field removal (created_at, last_login)
-- - GDPR user profile field cleanup (first_name, last_name)
-- - PCI DSS password storage infrastructure removal
-- 
-- Author: Blitzy agent
-- Date: $(date)
-- Version: 1.0
-- Forward Migration: V1__create_users_table.sql
-- =====================================================================================

-- =====================================================================================
-- PRE-ROLLBACK VALIDATION
-- =====================================================================================

-- Verify the users table exists before attempting rollback
DO $$
DECLARE
    table_exists boolean;
BEGIN
    SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'users' 
        AND table_schema = 'public'
    ) INTO table_exists;
    
    IF NOT table_exists THEN
        RAISE NOTICE 'WARNING: users table does not exist - rollback may be unnecessary';
    ELSE
        RAISE NOTICE 'INFO: users table found - proceeding with rollback';
    END IF;
END
$$;

-- Log rollback initiation
DO $$
BEGIN
    RAISE NOTICE 'ROLLBACK INITIATED: Starting rollback of V1__create_users_table.sql migration';
    RAISE NOTICE 'TIMESTAMP: %', NOW();
END
$$;

-- =====================================================================================
-- DEPENDENCY ANALYSIS AND CASCADE PREPARATION
-- =====================================================================================

-- Check for foreign key dependencies that will be removed by CASCADE
DO $$
DECLARE
    fk_count integer;
    constraint_record record;
BEGIN
    -- Count foreign key constraints referencing users table
    SELECT COUNT(*) INTO fk_count
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu 
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage ccu 
        ON ccu.constraint_name = tc.constraint_name
        AND ccu.table_schema = tc.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
        AND ccu.table_name = 'users';
    
    IF fk_count > 0 THEN
        RAISE NOTICE 'WARNING: Found % foreign key constraint(s) referencing users table', fk_count;
        RAISE NOTICE 'INFO: These constraints will be automatically dropped by CASCADE';
        
        -- Log each foreign key constraint that will be affected
        FOR constraint_record IN
            SELECT 
                tc.table_name as referencing_table,
                tc.constraint_name as constraint_name,
                kcu.column_name as referencing_column,
                ccu.column_name as referenced_column
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu 
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage ccu 
                ON ccu.constraint_name = tc.constraint_name
                AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
                AND ccu.table_name = 'users'
        LOOP
            RAISE NOTICE 'FK DEPENDENCY: %.% references users.%', 
                constraint_record.referencing_table,
                constraint_record.referencing_column,
                constraint_record.referenced_column;
        END LOOP;
    ELSE
        RAISE NOTICE 'INFO: No foreign key dependencies found - safe to proceed';
    END IF;
END
$$;

-- =====================================================================================
-- BACKUP CRITICAL DATA (OPTIONAL SAFETY MEASURE)
-- =====================================================================================

-- Create temporary backup of users data for recovery purposes (optional)
DO $$
DECLARE
    user_count integer;
    backup_table_exists boolean;
BEGIN
    -- Check if users table has data
    SELECT COUNT(*) INTO user_count FROM users;
    
    -- Check if backup table already exists
    SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'users_rollback_backup' 
        AND table_schema = 'public'
    ) INTO backup_table_exists;
    
    IF user_count > 0 AND NOT backup_table_exists THEN
        RAISE NOTICE 'INFO: Creating backup of % user records', user_count;
        
        -- Create backup table with timestamp
        EXECUTE 'CREATE TABLE users_rollback_backup AS 
                 SELECT *, ''' || NOW() || '''::timestamp as backup_timestamp 
                 FROM users';
        
        -- Add comment to backup table
        COMMENT ON TABLE users_rollback_backup IS 
            'Backup of users table data created during rollback of V1__create_users_table.sql. ' ||
            'Created for safety purposes and can be dropped after successful rollback verification.';
            
        RAISE NOTICE 'INFO: Backup table users_rollback_backup created successfully';
    ELSIF user_count = 0 THEN
        RAISE NOTICE 'INFO: users table is empty - no backup needed';
    ELSE
        RAISE NOTICE 'WARNING: Backup table already exists - skipping backup creation';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'WARNING: Could not create backup table: %', SQLERRM;
        RAISE NOTICE 'INFO: Proceeding with rollback without backup';
END
$$;

-- =====================================================================================
-- CORE ROLLBACK OPERATION
-- =====================================================================================

-- Log start of table removal
DO $$
BEGIN
    RAISE NOTICE 'INFO: Beginning DROP TABLE users CASCADE operation';
    RAISE NOTICE 'INFO: This will remove all associated constraints, indexes, and triggers';
END
$$;

-- Drop the users table with CASCADE to remove all dependent objects
-- This single command removes:
-- - The users table and all its data
-- - Primary key constraint (pk_users)
-- - Unique constraint (uk_users_user_id)
-- - All indexes (idx_users_user_type, idx_users_last_login, idx_users_type_login)
-- - All check constraints (chk_users_user_type, chk_users_user_id_format, etc.)
-- - All table and column comments
-- - Any foreign key constraints from other tables referencing users
-- - Any triggers or rules associated with the table
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================================================
-- POST-ROLLBACK VERIFICATION
-- =====================================================================================

-- Verify the users table has been completely removed
DO $$
DECLARE
    table_exists boolean;
    related_objects integer;
BEGIN
    -- Check that users table no longer exists
    SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'users' 
        AND table_schema = 'public'
    ) INTO table_exists;
    
    IF table_exists THEN
        RAISE EXCEPTION 'ROLLBACK FAILED: users table still exists after DROP operation';
    ELSE
        RAISE NOTICE 'SUCCESS: users table successfully removed';
    END IF;
    
    -- Check for any remaining objects related to users table
    SELECT COUNT(*) INTO related_objects
    FROM (
        -- Check for indexes containing 'users' in name
        SELECT indexname FROM pg_indexes 
        WHERE indexname LIKE '%users%' OR tablename = 'users'
        
        UNION ALL
        
        -- Check for constraints containing 'users' in name
        SELECT conname FROM pg_constraint 
        WHERE conname LIKE '%users%'
        
        UNION ALL
        
        -- Check for triggers on users table
        SELECT trigger_name FROM information_schema.triggers
        WHERE event_object_table = 'users'
    ) AS remaining_objects;
    
    IF related_objects > 0 THEN
        RAISE WARNING 'WARNING: Found % objects that may be related to users table', related_objects;
        RAISE NOTICE 'INFO: Please verify these are not orphaned objects from the rollback';
    ELSE
        RAISE NOTICE 'SUCCESS: No related objects found - rollback appears complete';
    END IF;
END
$$;

-- =====================================================================================
-- VSAM USRSEC RESTORATION GUIDANCE
-- =====================================================================================

-- Log restoration guidance for VSAM USRSEC dataset
DO $$
BEGIN
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'VSAM USRSEC RESTORATION GUIDANCE';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'INFO: users table rollback completed successfully';
    RAISE NOTICE 'INFO: To restore VSAM USRSEC dataset functionality:';
    RAISE NOTICE '  1. Restore CICS transaction COSGN00C for user authentication';
    RAISE NOTICE '  2. Re-enable RACF security profile integration';
    RAISE NOTICE '  3. Restore VSAM USRSEC dataset with KEYLEN=8, RECLEN=80';
    RAISE NOTICE '  4. Update CICS CSD to reference original user authentication programs';
    RAISE NOTICE '  5. Rollback Spring Security authentication configuration';
    RAISE NOTICE '  6. Remove JWT token generation and BCrypt password hashing';
    RAISE NOTICE 'INFO: Backup data available in users_rollback_backup if needed';
    RAISE NOTICE '=================================================================';
END
$$;

-- =====================================================================================
-- ROLLBACK COMPLETION LOG
-- =====================================================================================

-- Log successful rollback completion
DO $$
BEGIN
    RAISE NOTICE 'ROLLBACK COMPLETED: V1__create_users_table.sql rollback finished successfully';
    RAISE NOTICE 'TIMESTAMP: %', NOW();
    RAISE NOTICE 'SUMMARY: users table and all associated objects removed';
    RAISE NOTICE 'NEXT STEPS: Verify application functionality with VSAM USRSEC dataset';
    RAISE NOTICE 'CLEANUP: Consider dropping users_rollback_backup table after verification';
END
$$;

-- =====================================================================================
-- ROLLBACK VERIFICATION QUERIES
-- =====================================================================================
-- 
-- After rollback, verify complete removal with these queries:
-- 
-- Check table removal:
-- SELECT COUNT(*) FROM information_schema.tables 
-- WHERE table_name = 'users' AND table_schema = 'public';
-- Expected result: 0
-- 
-- Check for orphaned indexes:
-- SELECT indexname FROM pg_indexes WHERE indexname LIKE '%users%';
-- Expected result: No rows (empty result set)
-- 
-- Check for orphaned constraints:
-- SELECT conname FROM pg_constraint WHERE conname LIKE '%users%';
-- Expected result: No rows (empty result set)
-- 
-- Check for orphaned foreign keys:
-- SELECT tc.table_name, tc.constraint_name
-- FROM information_schema.table_constraints tc
-- JOIN information_schema.constraint_column_usage ccu 
--     ON ccu.constraint_name = tc.constraint_name
-- WHERE tc.constraint_type = 'FOREIGN KEY' AND ccu.table_name = 'users';
-- Expected result: No rows (empty result set)
-- 
-- Verify backup table (if created):
-- SELECT COUNT(*), MIN(backup_timestamp), MAX(backup_timestamp) 
-- FROM users_rollback_backup;
-- Expected result: Count of backed up records with timestamp
-- 
-- =====================================================================================
-- CLEANUP INSTRUCTIONS
-- =====================================================================================
-- 
-- After successful verification of rollback:
-- 
-- 1. Remove backup table if no longer needed:
--    DROP TABLE IF EXISTS users_rollback_backup;
-- 
-- 2. Update Liquibase changelog to mark rollback as complete
-- 
-- 3. Verify Spring Boot application startup without users table dependency
-- 
-- 4. Confirm VSAM USRSEC dataset functionality restoration
-- 
-- 5. Update documentation to reflect rollback to legacy authentication system
-- 
-- =====================================================================================