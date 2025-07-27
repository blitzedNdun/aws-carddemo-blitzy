-- ==============================================================================
-- Liquibase Migration: V26_1__fix_disclosure_groups_duplicate_key.sql
-- Description: Fix duplicate key violations in disclosure groups data loading
-- Author: Blitzy agent
-- Version: 26.1
-- Migration Type: DATA CLEANUP
-- Purpose: Handle partial migration state and duplicate key issues
-- ==============================================================================

--comment: Clean up disclosure groups table and ensure safe data loading

-- Step 1: Drop foreign key constraints temporarily to allow cleanup
-- Get all foreign key constraints referencing disclosure_groups
DO $$
DECLARE
    fk_record RECORD;
BEGIN
    -- Store foreign key constraints for later restoration
    CREATE TEMP TABLE IF NOT EXISTS temp_fk_constraints AS
    SELECT 
        conname as constraint_name,
        conrelid::regclass as table_name,
        pg_get_constraintdef(oid) as constraint_definition
    FROM pg_constraint 
    WHERE confrelid = 'disclosure_groups'::regclass 
    AND contype = 'f';
    
    -- Drop all foreign key constraints referencing disclosure_groups
    FOR fk_record IN 
        SELECT constraint_name, table_name 
        FROM temp_fk_constraints
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', 
                      fk_record.table_name, fk_record.constraint_name);
    END LOOP;
    
    RAISE NOTICE 'Temporarily dropped % foreign key constraints', 
                 (SELECT COUNT(*) FROM temp_fk_constraints);
END $$;

-- Step 2: Truncate disclosure_groups table to ensure clean state
TRUNCATE TABLE disclosure_groups RESTART IDENTITY CASCADE;

-- Step 3: Reset any sequences or indexes
REINDEX TABLE disclosure_groups;

-- Step 4: Verify table is empty
DO $$
DECLARE
    row_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO row_count FROM disclosure_groups;
    IF row_count > 0 THEN
        RAISE EXCEPTION 'Failed to clean disclosure_groups table: % rows remain', row_count;
    END IF;
    RAISE NOTICE 'Successfully cleaned disclosure_groups table';
END $$;

-- Step 5: Re-enable foreign key constraints
DO $$
DECLARE
    fk_record RECORD;
BEGIN
    -- Restore foreign key constraints
    FOR fk_record IN 
        SELECT table_name, constraint_definition
        FROM temp_fk_constraints
    LOOP
        EXECUTE format('ALTER TABLE %I ADD %s', 
                      fk_record.table_name, fk_record.constraint_definition);
    END LOOP;
    
    -- Clean up temporary table
    DROP TABLE IF EXISTS temp_fk_constraints;
    
    RAISE NOTICE 'Successfully restored foreign key constraints';
END $$;

-- Step 6: Add safeguard comments
COMMENT ON TABLE disclosure_groups IS 
'Disclosure groups table cleaned in V26.1 to resolve duplicate key issues. Ready for V26 data loading.';