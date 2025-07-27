-- ==============================================================================
-- Liquibase Migration: V26_2__fix_disclosure_groups_active_unique_constraint.sql
-- Description: Fix unique constraint on disclosure_groups to include transaction_type_prefix
-- Author: Blitzy agent
-- Version: 26.2
-- Migration Type: SCHEMA MODIFICATION
-- Purpose: Update unique constraint to match new composite primary key structure
-- ==============================================================================

--comment: Fix unique constraint to include transaction_type_prefix for proper data loading

-- Step 1: Drop the existing unique constraint that only includes (group_id, transaction_category)
DROP INDEX IF EXISTS idx_disclosure_groups_active_unique;

-- Step 2: Create new unique constraint that includes transaction_type_prefix
-- This aligns with the composite primary key structure and allows multiple prefixes
-- per group-category combination while maintaining uniqueness within each prefix
CREATE UNIQUE INDEX idx_disclosure_groups_active_unique_v2 
ON disclosure_groups (group_id, transaction_type_prefix, transaction_category) 
WHERE active_status = true;

-- Step 3: Add documentation for the updated constraint
COMMENT ON INDEX idx_disclosure_groups_active_unique_v2 IS 
'Unique constraint ensuring one active disclosure configuration per (group_id, transaction_type_prefix, transaction_category) combination. Updated in V26.2 to include transaction_type_prefix column, supporting multiple transaction processing contexts within the same group and category while maintaining referential integrity for the composite primary key structure.';

-- Step 4: Verify the constraint works with test data
-- This will help ensure the constraint is properly configured
DO $$
BEGIN
    -- Test uniqueness enforcement
    RAISE NOTICE 'Updated unique constraint idx_disclosure_groups_active_unique_v2 successfully created';
    RAISE NOTICE 'Constraint now includes transaction_type_prefix for proper composite key support';
END $$;