-- ===================================================================
-- Liquibase Rollback Script: R8__rollback_materialized_views.sql
-- Description: Rollback script for V8__create_materialized_views.sql
--              Removes all materialized views, functions, indexes, and 
--              scheduled jobs that replaced VSAM cross-reference functionality
-- Purpose: Enable complete reversal of COBOL mainframe modernization
--          cross-reference optimization changes and restore system to pre-V8 
--          migration state with traditional VSAM-equivalent table access patterns
-- Context: CardDemo COBOL-to-Java modernization project - PostgreSQL rollback
-- ===================================================================

-- ===================================================================
-- REMOVE SCHEDULED REFRESH JOBS
-- Purpose: Clean up pg_cron scheduled jobs before dropping objects
-- ===================================================================

-- Remove card transaction summary refresh job
SELECT cron.unschedule('refresh-card-transaction-summary');

-- Remove account balance history refresh job  
SELECT cron.unschedule('refresh-account-balance-history');

-- Remove customer account summary refresh job
SELECT cron.unschedule('refresh-customer-account-summary');

-- ===================================================================
-- DROP PERFORMANCE VALIDATION AND MAINTENANCE FUNCTIONS
-- Purpose: Remove all functions that support materialized view operations
-- ===================================================================

-- Drop performance testing function
DROP FUNCTION IF EXISTS test_materialized_view_performance();

-- Drop materialized view statistics function
DROP FUNCTION IF EXISTS get_materialized_view_stats();

-- Drop refresh logging function
DROP FUNCTION IF EXISTS log_materialized_view_refresh(
    TEXT, TIMESTAMP, TIMESTAMP, BIGINT, TEXT, TEXT
);

-- Drop manual refresh function
DROP FUNCTION IF EXISTS refresh_all_materialized_views();

-- ===================================================================
-- DROP MONITORING AND LOGGING INFRASTRUCTURE
-- Purpose: Remove materialized view refresh logging table and indexes
-- ===================================================================

-- Drop monitoring table indexes first
DROP INDEX IF EXISTS idx_mv_refresh_log_view_time;

-- Drop the monitoring table
DROP TABLE IF EXISTS materialized_view_refresh_log;

-- ===================================================================
-- DROP CROSS-REFERENCE OPTIMIZATION VIEWS
-- Purpose: Remove views that replaced VSAM alternate index functionality
-- ===================================================================

-- Drop cross-reference view
DROP VIEW IF EXISTS v_card_account_customer_xref;

-- Drop supporting index for cross-reference lookups
DROP INDEX IF EXISTS idx_cards_xref_lookup;

-- ===================================================================
-- DROP MATERIALIZED VIEW: mv_account_balance_history
-- Purpose: Remove account balance history materialized view and all indexes
-- ===================================================================

-- Drop indexes for mv_account_balance_history
DROP INDEX IF EXISTS idx_mv_account_balance_hist_monthly;
DROP INDEX IF EXISTS idx_mv_account_balance_hist_trend;
DROP INDEX IF EXISTS idx_mv_account_balance_hist_utilization;
DROP INDEX IF EXISTS idx_mv_account_balance_hist_customer;
DROP INDEX IF EXISTS idx_mv_account_balance_hist_account_date;

-- Drop the materialized view
DROP MATERIALIZED VIEW IF EXISTS mv_account_balance_history CASCADE;

-- ===================================================================
-- DROP MATERIALIZED VIEW: mv_card_transaction_summary  
-- Purpose: Remove card transaction summary materialized view and all indexes
-- ===================================================================

-- Drop indexes for mv_card_transaction_summary
DROP INDEX IF EXISTS idx_mv_card_trans_summary_activity;
DROP INDEX IF EXISTS idx_mv_card_trans_summary_volume;
DROP INDEX IF EXISTS idx_mv_card_trans_summary_account_id;
DROP INDEX IF EXISTS idx_mv_card_trans_summary_card_number;

-- Drop the materialized view
DROP MATERIALIZED VIEW IF EXISTS mv_card_transaction_summary CASCADE;

-- ===================================================================
-- REMOVE ADDITIONAL INDEXES ON EXISTING VIEWS
-- Purpose: Remove indexes added to mv_customer_account_summary in V8
-- Note: Only removes indexes added in V8, not the view itself (from V3)
-- ===================================================================

-- Remove additional indexes that were added to existing customer account summary view
DROP INDEX IF EXISTS idx_mv_customer_acct_summary_accounts;
DROP INDEX IF EXISTS idx_mv_customer_acct_summary_balance;
DROP INDEX IF EXISTS idx_mv_customer_acct_summary_fico;

-- ===================================================================
-- ROLLBACK VALIDATION AND CLEANUP
-- Purpose: Verify successful rollback and restore baseline performance
-- ===================================================================

-- Verify that all materialized views have been dropped
DO $$
DECLARE
    remaining_views INTEGER;
    remaining_functions INTEGER;
    remaining_tables INTEGER;
BEGIN
    -- Check for remaining materialized views from V8
    SELECT COUNT(*) INTO remaining_views
    FROM pg_matviews 
    WHERE matviewname IN (
        'mv_card_transaction_summary',
        'mv_account_balance_history'
    );
    
    -- Check for remaining functions from V8
    SELECT COUNT(*) INTO remaining_functions
    FROM pg_proc p
    JOIN pg_namespace n ON p.pronamespace = n.oid
    WHERE n.nspname = 'public'
      AND p.proname IN (
          'refresh_all_materialized_views',
          'get_materialized_view_stats',
          'log_materialized_view_refresh',
          'test_materialized_view_performance'
      );
    
    -- Check for remaining monitoring tables
    SELECT COUNT(*) INTO remaining_tables
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename = 'materialized_view_refresh_log';
    
    -- Log rollback validation results
    RAISE NOTICE 'V8 Rollback Validation Complete:';
    RAISE NOTICE '  Remaining materialized views: %', remaining_views;
    RAISE NOTICE '  Remaining functions: %', remaining_functions;
    RAISE NOTICE '  Remaining monitoring tables: %', remaining_tables;
    
    -- Raise warning if cleanup incomplete
    IF remaining_views > 0 OR remaining_functions > 0 OR remaining_tables > 0 THEN
        RAISE WARNING 'Rollback may be incomplete. Manual cleanup may be required.';
    ELSE
        RAISE NOTICE 'V8 rollback completed successfully. All objects removed.';
    END IF;
END $$;

-- ===================================================================
-- PERFORMANCE BASELINE RESTORATION NOTES
-- Purpose: Document expectations after rollback completion
-- ===================================================================

/*
ROLLBACK COMPLETION NOTES:

1. VSAM Cross-Reference Functionality Restoration:
   - Materialized views mv_card_transaction_summary and mv_account_balance_history removed
   - Applications must revert to direct table queries for cross-reference operations
   - Query performance will return to pre-V8 baseline levels

2. Automated Refresh Infrastructure Removed:
   - All pg_cron scheduled jobs for materialized view refresh have been unscheduled
   - Manual refresh functions and monitoring capabilities no longer available
   - System resources previously used for refresh operations are freed

3. Performance Impact:
   - Complex analytical queries will revert to full table scans and joins
   - Sub-millisecond access to aggregated data no longer available via materialized views
   - Applications should implement appropriate caching strategies if needed

4. Monitoring and Observability:
   - Materialized view refresh logging and statistics collection removed
   - Performance testing functions for view optimization no longer available
   - Standard PostgreSQL monitoring tools should be used for query performance analysis

5. Cross-Reference Query Patterns:
   - Applications must use direct JOINs between cards, accounts, and customers tables
   - VSAM alternate index equivalent functionality no longer pre-computed
   - Consider query optimization and indexing on base tables if performance issues occur

6. Batch Processing Window Impact:
   - 4-hour batch processing window requirement still applicable to base operations
   - Materialized view refresh operations no longer consuming batch window resources
   - Overall batch processing may complete faster without view refresh overhead

VERIFICATION STEPS:
- Run EXPLAIN ANALYZE on queries that previously used materialized views
- Monitor query performance on base tables after rollback
- Verify all scheduled jobs have been properly removed from pg_cron
- Confirm no remaining dependencies on dropped objects exist

For re-implementation of similar functionality, refer to the original V8 migration
or consider alternative optimization strategies based on current system requirements.
*/

-- ===================================================================
-- ROLLBACK SCRIPT COMPLETION
-- ===================================================================

-- Final status message
SELECT 'R8 rollback completed: All materialized views and supporting infrastructure removed' as rollback_status;