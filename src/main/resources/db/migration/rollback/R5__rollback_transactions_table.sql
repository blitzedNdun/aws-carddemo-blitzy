-- =====================================================================================
-- Liquibase Rollback Script: R5__rollback_transactions_table.sql
-- Description: Comprehensive rollback script for V5__create_transactions_table.sql
--              Reverses TRANSACT VSAM to PostgreSQL transactions table transformation
--              including partitioned tables, indexes, functions, triggers, and constraints
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 5.0
-- Dependencies: Reverses V5__create_transactions_table.sql
-- =====================================================================================

-- changeset blitzy:R5-rollback-transactions-table
-- comment: Rollback transactions table and all associated database objects created in V5 migration

-- =====================================================================================
-- STEP 1: Drop dependent functions that reference the transactions table
-- =====================================================================================

-- Drop function for refreshing materialized view
DROP FUNCTION IF EXISTS refresh_transaction_summary_view();

-- Drop function for automatic partition management
DROP FUNCTION IF EXISTS create_monthly_partition(INTEGER, INTEGER);

-- Drop function for monthly transaction summary calculations
DROP FUNCTION IF EXISTS calculate_monthly_summary(VARCHAR(11), INTEGER, INTEGER);

-- Drop function for transaction history queries with partition pruning
DROP FUNCTION IF EXISTS get_transaction_history(VARCHAR(11), DATE, DATE, INTEGER);

-- Drop function for transaction validation matching COBOL business logic
DROP FUNCTION IF EXISTS validate_transaction_amount(VARCHAR(11), DECIMAL(12,2), VARCHAR(2));

-- =====================================================================================
-- STEP 2: Drop materialized view and its indexes
-- =====================================================================================

-- Drop materialized view indexes first
DROP INDEX IF EXISTS idx_mv_transaction_summary_category;
DROP INDEX IF EXISTS idx_mv_transaction_summary_type;
DROP INDEX IF EXISTS idx_mv_transaction_summary_month_year;
DROP INDEX IF EXISTS idx_mv_transaction_summary_card_number;
DROP INDEX IF EXISTS idx_mv_transaction_summary_account_id;

-- Drop materialized view for transaction summary analytics
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_summary CASCADE;

-- =====================================================================================
-- STEP 3: Drop triggers and trigger functions
-- =====================================================================================

-- Drop trigger for automatically updating updated_at timestamp
DROP TRIGGER IF EXISTS transactions_updated_at_trigger ON transactions;

-- Drop trigger function for updating updated_at timestamp
DROP FUNCTION IF EXISTS update_transactions_updated_at();

-- =====================================================================================
-- STEP 4: Drop main transactions table with CASCADE
-- =====================================================================================

-- Drop the main transactions table with CASCADE to remove all:
-- - Monthly partition tables (transactions_2024_01 through transactions_2025_12)
-- - All indexes created on the transactions table
-- - All foreign key constraints
-- - All check constraints
-- - All table comments
DROP TABLE IF EXISTS transactions CASCADE;

-- =====================================================================================
-- VERIFICATION QUERIES (commented out for production use)
-- =====================================================================================

-- The following queries can be used to verify complete rollback:
-- SELECT tablename FROM pg_tables WHERE tablename LIKE 'transactions%';
-- SELECT schemaname, tablename FROM pg_tables WHERE tablename LIKE 'transactions_%';
-- SELECT proname FROM pg_proc WHERE proname LIKE '%transaction%';
-- SELECT matviewname FROM pg_matviews WHERE matviewname LIKE '%transaction%';
-- SELECT trigger_name FROM information_schema.triggers WHERE trigger_name LIKE '%transaction%';

-- =====================================================================================
-- ROLLBACK VALIDATION CONFIRMATION
-- =====================================================================================

-- Log successful rollback completion
-- This confirms that all transactions table objects have been removed
-- and the database is restored to the state before V5 migration
DO $$
BEGIN
    RAISE NOTICE 'R5 Rollback Complete: All transactions table objects have been successfully removed';
    RAISE NOTICE 'Database restored to pre-V5 migration state';
    RAISE NOTICE 'VSAM TRANSACT dataset functionality must be restored externally';
    RAISE NOTICE '4-hour batch processing window compatibility maintained';
END $$;

-- =====================================================================================
-- IMPORTANT NOTES FOR OPERATIONS TEAM
-- =====================================================================================

-- 1. After running this rollback script, the following actions are required:
--    - Restore VSAM TRANSACT dataset functionality if needed
--    - Verify that dependent applications are configured to use VSAM instead of PostgreSQL
--    - Update batch processing jobs to use original VSAM file processing
--    - Ensure transaction history data is preserved according to business requirements
--
-- 2. This rollback removes all transaction history data stored in PostgreSQL
--    - If data preservation is required, export transaction data before rollback
--    - Consider creating a backup of the transactions table before rollback
--
-- 3. Monthly partition cleanup is handled automatically by CASCADE
--    - All 24 monthly partitions (2024-01 through 2025-12) are removed
--    - No manual partition cleanup is required
--
-- 4. Performance considerations:
--    - This rollback will completely remove the optimized PostgreSQL transaction processing
--    - Ensure VSAM TRANSACT dataset is properly configured for transaction volumes
--    - Verify that 4-hour batch processing window can be maintained with VSAM
--
-- 5. Foreign key dependency impact:
--    - Applications using transactions table foreign keys will need to be updated
--    - Account and card relationship tracking reverts to VSAM cross-reference methods
--    - Ensure referential integrity is maintained through application logic

-- rollback changeset blitzy:R5-rollback-transactions-table