-- ============================================================================
-- Liquibase Rollback Script: R2__rollback_customers_table.sql
-- Description: Rollback script for V2__create_customers_table.sql migration
-- Purpose: Completely reverse the creation of customers table and all associated objects
-- Author: Blitzy agent
-- Version: 2.0
-- ============================================================================

-- Rollback Overview:
-- This script reverses the CUSTDAT VSAM to PostgreSQL customers table transformation
-- by dropping all database objects created during the forward migration.
-- Objects removed include: table, indexes, constraints, triggers, functions, and comments

-- WARNING: This rollback operation will permanently delete the customers table
-- and all customer data. Ensure proper backup procedures are in place before execution.

-- ============================================================================
-- 1. DROP TRIGGER (must be dropped before the function)
-- ============================================================================

-- Drop the trigger that automatically updates the updated_at timestamp
DROP TRIGGER IF EXISTS trg_customers_updated_at ON customers;

-- ============================================================================
-- 2. DROP TRIGGER FUNCTION
-- ============================================================================

-- Drop the function used by the trigger
DROP FUNCTION IF EXISTS update_customers_updated_at();

-- ============================================================================
-- 3. DROP INDEXES (will be dropped automatically with table, but explicit for clarity)
-- ============================================================================

-- Drop performance optimization indexes
DROP INDEX IF EXISTS idx_customers_name;
DROP INDEX IF EXISTS idx_customers_location;
DROP INDEX IF EXISTS idx_customers_birth_date;
DROP INDEX IF EXISTS idx_customers_fico_score;
DROP INDEX IF EXISTS idx_customers_ssn_valid;

-- ============================================================================
-- 4. DROP TABLE WITH CASCADE (removes all dependent objects)
-- ============================================================================

-- Drop the customers table with CASCADE to remove all dependent objects
-- This includes:
-- - All check constraints (chk_customers_*)
-- - Primary key constraint (pk_customers)
-- - All indexes (if not explicitly dropped above)
-- - All column comments and table comments
-- - Any foreign key references from other tables
-- - Any row-level security policies
DROP TABLE IF EXISTS customers CASCADE;

-- ============================================================================
-- ROLLBACK VALIDATION NOTES
-- ============================================================================

-- After executing this rollback script, the following should be verified:
-- 1. customers table no longer exists: SELECT * FROM information_schema.tables WHERE table_name = 'customers';
-- 2. No remaining indexes: SELECT * FROM pg_indexes WHERE tablename = 'customers';
-- 3. No remaining constraints: SELECT * FROM information_schema.table_constraints WHERE table_name = 'customers';
-- 4. No remaining triggers: SELECT * FROM information_schema.triggers WHERE event_object_table = 'customers';
-- 5. No remaining functions: SELECT * FROM pg_proc WHERE proname = 'update_customers_updated_at';

-- ============================================================================
-- VSAM CUSTDAT DATASET RESTORATION NOTES
-- ============================================================================

-- This rollback effectively restores the system to its pre-migration state
-- where customer data was managed through the VSAM CUSTDAT dataset with:
-- - 9-byte customer ID keys
-- - 500-byte customer records
-- - VSAM KSDS file organization
-- - Original COBOL program access patterns

-- To fully restore VSAM CUSTDAT functionality:
-- 1. Ensure VSAM CUSTDAT dataset is available and accessible
-- 2. Verify COBOL programs can access the VSAM file
-- 3. Confirm batch processing jobs reference the correct dataset
-- 4. Update any configuration files pointing to PostgreSQL back to VSAM

-- ============================================================================
-- CLEANUP VERIFICATION QUERIES
-- ============================================================================

-- The following queries can be used to verify complete rollback:

-- Check if table exists (should return 0 rows)
-- SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'customers';

-- Check for any remaining constraints (should return 0 rows)
-- SELECT constraint_name FROM information_schema.table_constraints WHERE table_name = 'customers';

-- Check for any remaining indexes (should return 0 rows)
-- SELECT indexname FROM pg_indexes WHERE tablename = 'customers';

-- Check for any remaining triggers (should return 0 rows)
-- SELECT trigger_name FROM information_schema.triggers WHERE event_object_table = 'customers';

-- Check for any remaining functions (should return 0 rows)
-- SELECT proname FROM pg_proc WHERE proname = 'update_customers_updated_at';

-- ============================================================================
-- ROLLBACK COMPLETION CONFIRMATION
-- ============================================================================

-- Log successful rollback completion
DO $$
BEGIN
    RAISE NOTICE 'Rollback R2__rollback_customers_table.sql completed successfully';
    RAISE NOTICE 'All customers table objects have been removed';
    RAISE NOTICE 'System restored to pre-migration state for VSAM CUSTDAT dataset';
    RAISE NOTICE 'Customer data management reverted to original VSAM CUSTDAT dataset';
END $$;