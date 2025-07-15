-- ============================================================================
-- Liquibase Rollback Script: R8__rollback_materialized_views.sql
-- Description: Rollback script for V8__create_materialized_views.sql
--              Removes all materialized views that replaced VSAM cross-reference
--              functionality, restoring the system to pre-optimization state
-- Author: Blitzy agent  
-- Version: 8.0 (rollback)
-- Target Migration: V8__create_materialized_views.sql
-- ============================================================================

-- ============================================================================
-- ROLLBACK STRATEGY
-- This rollback script reverses the creation of materialized views that provided
-- cross-reference optimization functionality equivalent to VSAM XREFFILE and 
-- CXACAIX datasets. The rollback removes all materialized views, their indexes,
-- supporting functions, and scheduled refresh jobs to restore the original
-- query performance baseline before optimization.
-- ============================================================================

-- ============================================================================
-- ROLLBACK VALIDATION AND SAFETY CHECKS
-- ============================================================================

-- Function to validate rollback prerequisites
CREATE OR REPLACE FUNCTION validate_rollback_prerequisites()
RETURNS BOOLEAN AS $$
DECLARE
    view_count INTEGER;
    function_count INTEGER;
    dependency_count INTEGER;
BEGIN
    -- Check if materialized views exist
    SELECT COUNT(*) INTO view_count
    FROM pg_matviews 
    WHERE schemaname = current_schema() 
    AND matviewname IN ('mv_customer_account_summary', 'mv_card_transaction_summary', 'mv_account_balance_history');
    
    -- Check if supporting functions exist
    SELECT COUNT(*) INTO function_count
    FROM pg_proc p
    JOIN pg_namespace n ON p.pronamespace = n.oid
    WHERE n.nspname = current_schema()
    AND p.proname IN ('refresh_materialized_views', 'refresh_materialized_views_with_dependencies', 
                      'check_materialized_view_freshness', 'analyze_materialized_view_performance');
    
    -- Check for any dependencies on the materialized views
    SELECT COUNT(*) INTO dependency_count
    FROM pg_depend d
    JOIN pg_class c ON d.refobjid = c.oid
    WHERE c.relname IN ('mv_customer_account_summary', 'mv_card_transaction_summary', 'mv_account_balance_history')
    AND d.deptype = 'n'  -- Normal dependencies
    AND d.objid NOT IN (
        SELECT oid FROM pg_class WHERE relname LIKE 'idx_mv_%'  -- Exclude indexes
    );
    
    RAISE NOTICE 'Rollback validation: Found % materialized views, % functions, % external dependencies', 
                 view_count, function_count, dependency_count;
    
    -- Return true if views exist and no external dependencies
    RETURN view_count > 0 AND dependency_count = 0;
END;
$$ LANGUAGE plpgsql;

-- Execute validation
DO $$
BEGIN
    IF NOT validate_rollback_prerequisites() THEN
        RAISE EXCEPTION 'Rollback validation failed: Either materialized views do not exist or external dependencies detected. Please verify the migration state before proceeding.';
    END IF;
    
    RAISE NOTICE 'Rollback validation passed: Safe to proceed with materialized views removal';
END;
$$;

-- ============================================================================
-- STEP 1: REMOVE AUTOMATIC REFRESH SCHEDULING
-- ============================================================================

-- Remove pg_cron scheduled jobs for materialized view refresh
-- Note: This will only execute if pg_cron extension is available
DO $$
BEGIN
    -- Check if pg_cron extension is available
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        RAISE NOTICE 'pg_cron extension detected, removing scheduled refresh jobs';
        
        -- Remove the scheduled refresh job
        BEGIN
            PERFORM cron.unschedule('refresh_materialized_views');
            RAISE NOTICE 'Successfully removed scheduled job: refresh_materialized_views';
        EXCEPTION
            WHEN OTHERS THEN
                RAISE WARNING 'Failed to remove scheduled job refresh_materialized_views: %', SQLERRM;
        END;
        
        -- Remove any additional scheduled jobs that might exist
        BEGIN
            PERFORM cron.unschedule('refresh_materialized_views_with_dependencies');
            RAISE NOTICE 'Successfully removed scheduled job: refresh_materialized_views_with_dependencies';
        EXCEPTION
            WHEN OTHERS THEN
                RAISE WARNING 'Failed to remove scheduled job refresh_materialized_views_with_dependencies: %', SQLERRM;
        END;
        
    ELSE
        RAISE NOTICE 'pg_cron extension not found, skipping scheduled job removal';
    END IF;
END;
$$;

-- ============================================================================
-- STEP 2: DROP MATERIALIZED VIEWS IN REVERSE ORDER
-- ============================================================================

-- Drop materialized views in reverse dependency order with CASCADE
-- This ensures all dependent objects (indexes, constraints) are removed

-- Drop mv_account_balance_history materialized view
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_account_balance_history') THEN
        RAISE NOTICE 'Dropping materialized view: mv_account_balance_history';
        DROP MATERIALIZED VIEW mv_account_balance_history CASCADE;
        RAISE NOTICE 'Successfully dropped mv_account_balance_history and all dependent objects';
    ELSE
        RAISE NOTICE 'Materialized view mv_account_balance_history does not exist, skipping';
    END IF;
END;
$$;

-- Drop mv_card_transaction_summary materialized view
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_card_transaction_summary') THEN
        RAISE NOTICE 'Dropping materialized view: mv_card_transaction_summary';
        DROP MATERIALIZED VIEW mv_card_transaction_summary CASCADE;
        RAISE NOTICE 'Successfully dropped mv_card_transaction_summary and all dependent objects';
    ELSE
        RAISE NOTICE 'Materialized view mv_card_transaction_summary does not exist, skipping';
    END IF;
END;
$$;

-- Drop mv_customer_account_summary materialized view
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_customer_account_summary') THEN
        RAISE NOTICE 'Dropping materialized view: mv_customer_account_summary';
        DROP MATERIALIZED VIEW mv_customer_account_summary CASCADE;
        RAISE NOTICE 'Successfully dropped mv_customer_account_summary and all dependent objects';
    ELSE
        RAISE NOTICE 'Materialized view mv_customer_account_summary does not exist, skipping';
    END IF;
END;
$$;

-- ============================================================================
-- STEP 3: DROP SUPPORTING FUNCTIONS
-- ============================================================================

-- Drop refresh_materialized_views function
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'refresh_materialized_views' AND pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema())) THEN
        RAISE NOTICE 'Dropping function: refresh_materialized_views()';
        DROP FUNCTION refresh_materialized_views() CASCADE;
        RAISE NOTICE 'Successfully dropped refresh_materialized_views() function';
    ELSE
        RAISE NOTICE 'Function refresh_materialized_views() does not exist, skipping';
    END IF;
END;
$$;

-- Drop refresh_materialized_views_with_dependencies function
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'refresh_materialized_views_with_dependencies' AND pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema())) THEN
        RAISE NOTICE 'Dropping function: refresh_materialized_views_with_dependencies()';
        DROP FUNCTION refresh_materialized_views_with_dependencies() CASCADE;
        RAISE NOTICE 'Successfully dropped refresh_materialized_views_with_dependencies() function';
    ELSE
        RAISE NOTICE 'Function refresh_materialized_views_with_dependencies() does not exist, skipping';
    END IF;
END;
$$;

-- Drop check_materialized_view_freshness function
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'check_materialized_view_freshness' AND pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema())) THEN
        RAISE NOTICE 'Dropping function: check_materialized_view_freshness()';
        DROP FUNCTION check_materialized_view_freshness() CASCADE;
        RAISE NOTICE 'Successfully dropped check_materialized_view_freshness() function';
    ELSE
        RAISE NOTICE 'Function check_materialized_view_freshness() does not exist, skipping';
    END IF;
END;
$$;

-- Drop analyze_materialized_view_performance function
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'analyze_materialized_view_performance' AND pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema())) THEN
        RAISE NOTICE 'Dropping function: analyze_materialized_view_performance()';
        DROP FUNCTION analyze_materialized_view_performance() CASCADE;
        RAISE NOTICE 'Successfully dropped analyze_materialized_view_performance() function';
    ELSE
        RAISE NOTICE 'Function analyze_materialized_view_performance() does not exist, skipping';
    END IF;
END;
$$;

-- ============================================================================
-- STEP 4: CLEANUP VALIDATION AND VERIFICATION
-- ============================================================================

-- Function to validate complete rollback
CREATE OR REPLACE FUNCTION validate_rollback_completion()
RETURNS BOOLEAN AS $$
DECLARE
    remaining_views INTEGER;
    remaining_functions INTEGER;
    remaining_indexes INTEGER;
    rollback_success BOOLEAN := TRUE;
BEGIN
    -- Check for remaining materialized views
    SELECT COUNT(*) INTO remaining_views
    FROM pg_matviews 
    WHERE schemaname = current_schema() 
    AND matviewname IN ('mv_customer_account_summary', 'mv_card_transaction_summary', 'mv_account_balance_history');
    
    -- Check for remaining functions
    SELECT COUNT(*) INTO remaining_functions
    FROM pg_proc p
    JOIN pg_namespace n ON p.pronamespace = n.oid
    WHERE n.nspname = current_schema()
    AND p.proname IN ('refresh_materialized_views', 'refresh_materialized_views_with_dependencies', 
                      'check_materialized_view_freshness', 'analyze_materialized_view_performance');
    
    -- Check for remaining indexes related to materialized views
    SELECT COUNT(*) INTO remaining_indexes
    FROM pg_indexes 
    WHERE schemaname = current_schema()
    AND indexname LIKE 'idx_mv_%';
    
    -- Report rollback status
    RAISE NOTICE 'Rollback completion check: % materialized views, % functions, % indexes remaining', 
                 remaining_views, remaining_functions, remaining_indexes;
    
    -- Validate complete removal
    IF remaining_views > 0 THEN
        RAISE WARNING 'Rollback incomplete: % materialized views still exist', remaining_views;
        rollback_success := FALSE;
    END IF;
    
    IF remaining_functions > 0 THEN
        RAISE WARNING 'Rollback incomplete: % supporting functions still exist', remaining_functions;
        rollback_success := FALSE;
    END IF;
    
    IF remaining_indexes > 0 THEN
        RAISE WARNING 'Rollback incomplete: % materialized view indexes still exist', remaining_indexes;
        rollback_success := FALSE;
    END IF;
    
    IF rollback_success THEN
        RAISE NOTICE 'Rollback completed successfully: All materialized views, functions, and indexes removed';
    END IF;
    
    RETURN rollback_success;
END;
$$ LANGUAGE plpgsql;

-- Execute rollback validation
DO $$
BEGIN
    IF NOT validate_rollback_completion() THEN
        RAISE EXCEPTION 'Rollback validation failed: Some objects were not properly removed. Please check the logs and retry the rollback process.';
    END IF;
    
    RAISE NOTICE 'Rollback validation passed: V8 materialized views migration has been successfully reversed';
END;
$$;

-- ============================================================================
-- STEP 5: RESTORE BASELINE QUERY PERFORMANCE DOCUMENTATION
-- ============================================================================

-- Create documentation about the rollback for future reference
DO $$
BEGIN
    RAISE NOTICE '========================================================================';
    RAISE NOTICE 'ROLLBACK COMPLETED: V8 Materialized Views Migration Reversed';
    RAISE NOTICE '========================================================================';
    RAISE NOTICE 'The following objects have been removed:';
    RAISE NOTICE '  - Materialized View: mv_customer_account_summary';
    RAISE NOTICE '  - Materialized View: mv_card_transaction_summary';
    RAISE NOTICE '  - Materialized View: mv_account_balance_history';
    RAISE NOTICE '  - All materialized view indexes (CASCADE)';
    RAISE NOTICE '  - Function: refresh_materialized_views()';
    RAISE NOTICE '  - Function: refresh_materialized_views_with_dependencies()';
    RAISE NOTICE '  - Function: check_materialized_view_freshness()';
    RAISE NOTICE '  - Function: analyze_materialized_view_performance()';
    RAISE NOTICE '  - Scheduled refresh jobs (if pg_cron was available)';
    RAISE NOTICE '';
    RAISE NOTICE 'System Impact:';
    RAISE NOTICE '  - Cross-reference queries will revert to baseline performance';
    RAISE NOTICE '  - VSAM cross-reference equivalent functionality removed';
    RAISE NOTICE '  - Query performance will return to pre-optimization levels';
    RAISE NOTICE '  - No sub-millisecond cross-reference access available';
    RAISE NOTICE '';
    RAISE NOTICE 'Next Steps:';
    RAISE NOTICE '  - Monitor query performance for cross-reference operations';
    RAISE NOTICE '  - Consider alternative optimization strategies if needed';
    RAISE NOTICE '  - Review application code for dependencies on materialized views';
    RAISE NOTICE '  - Update monitoring and alerting for baseline performance';
    RAISE NOTICE '========================================================================';
END;
$$;

-- ============================================================================
-- STEP 6: CLEANUP TEMPORARY ROLLBACK FUNCTIONS
-- ============================================================================

-- Remove temporary validation functions used during rollback
DROP FUNCTION IF EXISTS validate_rollback_prerequisites() CASCADE;
DROP FUNCTION IF EXISTS validate_rollback_completion() CASCADE;

-- ============================================================================
-- ROLLBACK COMPLETION VERIFICATION
-- ============================================================================

-- Final verification query to confirm rollback success
-- This should return no rows if rollback was successful
DO $$
DECLARE
    verification_result TEXT;
BEGIN
    -- Check for any remaining materialized view objects
    SELECT string_agg(object_type || ': ' || object_name, ', ') INTO verification_result
    FROM (
        SELECT 'Materialized View' AS object_type, matviewname AS object_name
        FROM pg_matviews 
        WHERE schemaname = current_schema() 
        AND matviewname IN ('mv_customer_account_summary', 'mv_card_transaction_summary', 'mv_account_balance_history')
        
        UNION ALL
        
        SELECT 'Function' AS object_type, proname AS object_name
        FROM pg_proc p
        JOIN pg_namespace n ON p.pronamespace = n.oid
        WHERE n.nspname = current_schema()
        AND p.proname IN ('refresh_materialized_views', 'refresh_materialized_views_with_dependencies', 
                          'check_materialized_view_freshness', 'analyze_materialized_view_performance')
        
        UNION ALL
        
        SELECT 'Index' AS object_type, indexname AS object_name
        FROM pg_indexes 
        WHERE schemaname = current_schema()
        AND indexname LIKE 'idx_mv_%'
    ) remaining_objects;
    
    IF verification_result IS NOT NULL THEN
        RAISE EXCEPTION 'Rollback verification failed: The following objects still exist: %', verification_result;
    ELSE
        RAISE NOTICE 'Rollback verification passed: All V8 materialized view objects have been successfully removed';
    END IF;
END;
$$;

-- ============================================================================
-- ROLLBACK SCRIPT COMPLETION
-- ============================================================================

-- Log successful completion
DO $$
BEGIN
    RAISE NOTICE 'R8__rollback_materialized_views.sql completed successfully at %', CURRENT_TIMESTAMP;
    RAISE NOTICE 'V8__create_materialized_views.sql migration has been fully reversed';
    RAISE NOTICE 'Database state restored to pre-materialized-views baseline';
END;
$$;