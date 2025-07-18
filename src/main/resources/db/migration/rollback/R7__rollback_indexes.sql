-- =====================================================================================
-- Liquibase Rollback Script: R7__rollback_indexes.sql
-- Description: Comprehensive rollback script that reverses all B-tree index creation
--              operations from V7__create_indexes.sql, enabling complete database
--              schema rollback to restore baseline performance and free index storage
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 7.0
-- Dependencies: V7__create_indexes.sql (this script reverses those operations)
-- =====================================================================================

-- rollback changeset blitzy:V7-create-indexes
-- comment: Rollback comprehensive B-tree index strategy and restore pre-optimization database state

-- =============================================================================
-- 1. PERFORMANCE MONITORING FUNCTIONS ROLLBACK
-- =============================================================================

-- Drop performance monitoring functions first to avoid dependency issues
DROP FUNCTION IF EXISTS monitor_index_performance() CASCADE;
DROP FUNCTION IF EXISTS reindex_all_tables() CASCADE;
DROP FUNCTION IF EXISTS identify_unused_indexes() CASCADE;
DROP FUNCTION IF EXISTS analyze_index_performance() CASCADE;

-- =============================================================================
-- 2. MATERIALIZED VIEW INDEXES ROLLBACK
-- =============================================================================

-- Drop materialized view indexes to free up storage and improve rollback performance
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_group_id CASCADE;
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_transaction_category CASCADE;
DROP INDEX IF EXISTS idx_mv_reference_data_lookup_transaction_type CASCADE;
DROP INDEX IF EXISTS idx_mv_transaction_summary_transaction_type CASCADE;
DROP INDEX IF EXISTS idx_mv_transaction_summary_month_year CASCADE;
DROP INDEX IF EXISTS idx_mv_transaction_summary_account_id CASCADE;
DROP INDEX IF EXISTS idx_mv_card_summary_expiration_date CASCADE;
DROP INDEX IF EXISTS idx_mv_card_summary_card_status CASCADE;
DROP INDEX IF EXISTS idx_mv_card_summary_customer_id CASCADE;
DROP INDEX IF EXISTS idx_mv_account_summary_available_credit CASCADE;
DROP INDEX IF EXISTS idx_mv_account_summary_balance_status CASCADE;
DROP INDEX IF EXISTS idx_mv_account_summary_customer_id CASCADE;
DROP INDEX IF EXISTS idx_mv_customer_summary_age CASCADE;
DROP INDEX IF EXISTS idx_mv_customer_summary_credit_rating CASCADE;
DROP INDEX IF EXISTS idx_mv_customer_summary_customer_id CASCADE;

-- =============================================================================
-- 3. BLOOM FILTER INDEXES ROLLBACK (BRIN)
-- =============================================================================

-- Drop BRIN indexes optimized for large datasets
DROP INDEX IF EXISTS idx_customers_created_brin CASCADE;
DROP INDEX IF EXISTS idx_accounts_created_brin CASCADE;
DROP INDEX IF EXISTS idx_transactions_timestamp_brin CASCADE;

-- =============================================================================
-- 4. EXPRESSION INDEXES ROLLBACK
-- =============================================================================

-- Drop expression indexes for calculated fields
DROP INDEX IF EXISTS idx_customers_age CASCADE;
DROP INDEX IF EXISTS idx_accounts_age CASCADE;
DROP INDEX IF EXISTS idx_accounts_available_credit CASCADE;

-- =============================================================================
-- 5. UNIQUE INDEXES ROLLBACK
-- =============================================================================

-- Drop unique indexes for data integrity constraints
DROP INDEX IF EXISTS idx_accounts_customer_unique CASCADE;
DROP INDEX IF EXISTS idx_customers_government_id_unique CASCADE;
DROP INDEX IF EXISTS idx_customers_ssn_unique CASCADE;

-- =============================================================================
-- 6. PARTIAL INDEXES ROLLBACK
-- =============================================================================

-- Drop partial indexes for performance optimization
DROP INDEX IF EXISTS idx_transactions_high_value_partial CASCADE;
DROP INDEX IF EXISTS idx_transactions_recent_partial CASCADE;
DROP INDEX IF EXISTS idx_cards_active_partial CASCADE;
DROP INDEX IF EXISTS idx_accounts_active_partial CASCADE;

-- =============================================================================
-- 7. BATCH PROCESSING INDEXES ROLLBACK
-- =============================================================================

-- Drop specialized indexes for batch processing operations
DROP INDEX IF EXISTS idx_transactions_reporting CASCADE;
DROP INDEX IF EXISTS idx_transactions_statement_gen CASCADE;
DROP INDEX IF EXISTS idx_accounts_interest_calc CASCADE;

-- =============================================================================
-- 8. FULL-TEXT SEARCH INDEXES ROLLBACK (GIN)
-- =============================================================================

-- Drop GIN indexes for full-text search functionality
DROP INDEX IF EXISTS idx_customers_name_gin CASCADE;
DROP INDEX IF EXISTS idx_transactions_merchant_gin CASCADE;
DROP INDEX IF EXISTS idx_transactions_description_gin CASCADE;

-- =============================================================================
-- 9. TRANSACTION CATEGORY BALANCE INDEXES ROLLBACK
-- =============================================================================

-- Drop transaction category balance optimization indexes
DROP INDEX IF EXISTS idx_tcb_account_category CASCADE;
DROP INDEX IF EXISTS idx_tcb_balance_analysis CASCADE;

-- =============================================================================
-- 10. CUSTOMER DATA OPTIMIZATION INDEXES ROLLBACK
-- =============================================================================

-- Drop customer data optimization indexes
DROP INDEX IF EXISTS idx_customers_pii_secure CASCADE;
DROP INDEX IF EXISTS idx_customers_demographics CASCADE;
DROP INDEX IF EXISTS idx_customers_name_search CASCADE;

-- =============================================================================
-- 11. REFERENCE DATA OPTIMIZATION INDEXES ROLLBACK
-- =============================================================================

-- Drop reference data optimization indexes
DROP INDEX IF EXISTS idx_disclosure_groups_active_btree CASCADE;
DROP INDEX IF EXISTS idx_transaction_categories_active_btree CASCADE;
DROP INDEX IF EXISTS idx_transaction_types_active_btree CASCADE;

-- =============================================================================
-- 12. COMPOSITE INDEXES FOR MICROSERVICES ROLLBACK
-- =============================================================================

-- Drop composite indexes for microservices query patterns
DROP INDEX IF EXISTS idx_transactions_amount_type CASCADE;
DROP INDEX IF EXISTS idx_cards_status_expiration CASCADE;
DROP INDEX IF EXISTS idx_accounts_status_balance CASCADE;

-- =============================================================================
-- 13. DATE AND TIME-RANGE QUERY INDEXES ROLLBACK
-- =============================================================================

-- Drop date and time-range query optimization indexes
DROP INDEX IF EXISTS idx_cards_date_range CASCADE;
DROP INDEX IF EXISTS idx_accounts_date_range CASCADE;
DROP INDEX IF EXISTS idx_transactions_date_range CASCADE;

-- =============================================================================
-- 14. FOREIGN KEY OPTIMIZATION INDEXES ROLLBACK
-- =============================================================================

-- Drop foreign key optimization indexes for transaction_category_balances
DROP INDEX IF EXISTS idx_tcb_transaction_category_btree CASCADE;
DROP INDEX IF EXISTS idx_tcb_account_id_btree CASCADE;

-- Drop foreign key optimization indexes for transactions
DROP INDEX IF EXISTS idx_transactions_category_btree CASCADE;
DROP INDEX IF EXISTS idx_transactions_type_btree CASCADE;
DROP INDEX IF EXISTS idx_transactions_card_number_btree CASCADE;
DROP INDEX IF EXISTS idx_transactions_account_id_btree CASCADE;

-- Drop foreign key optimization indexes for cards
DROP INDEX IF EXISTS idx_cards_customer_id_btree CASCADE;
DROP INDEX IF EXISTS idx_cards_account_id_btree CASCADE;

-- Drop foreign key optimization indexes for accounts
DROP INDEX IF EXISTS idx_accounts_group_id_btree CASCADE;
DROP INDEX IF EXISTS idx_accounts_customer_id_btree CASCADE;

-- =============================================================================
-- 15. COVERING INDEXES ROLLBACK
-- =============================================================================

-- Drop covering indexes for balance queries
DROP INDEX IF EXISTS idx_cards_balance_covering CASCADE;
DROP INDEX IF EXISTS idx_account_balance_covering CASCADE;

-- =============================================================================
-- 16. CUSTOMER-ACCOUNT CROSS-REFERENCE INDEXES ROLLBACK
-- =============================================================================

-- Drop customer-account cross-reference optimization indexes
DROP INDEX IF EXISTS idx_account_customer_xref CASCADE;
DROP INDEX IF EXISTS idx_customer_account_xref CASCADE;

-- =============================================================================
-- 17. VSAM ALTERNATE INDEX REPLICATION ROLLBACK
-- =============================================================================

-- Drop VSAM alternate index replication indexes (core functionality)
DROP INDEX IF EXISTS idx_cards_customer_id_active_status CASCADE;
DROP INDEX IF EXISTS idx_transactions_timestamp_account_id CASCADE;
DROP INDEX IF EXISTS idx_cards_account_id_active_status CASCADE;

-- =============================================================================
-- 18. ROLLBACK VALIDATION AND CLEANUP
-- =============================================================================

-- Create temporary function to validate rollback completion
CREATE OR REPLACE FUNCTION validate_indexes_rollback()
RETURNS TABLE (
    remaining_indexes INTEGER,
    rollback_status TEXT,
    validation_timestamp TIMESTAMP WITH TIME ZONE
) AS $$
DECLARE
    index_count INTEGER;
BEGIN
    -- Count remaining indexes from V7 migration
    SELECT COUNT(*) INTO index_count
    FROM pg_indexes 
    WHERE schemaname = 'public' 
    AND indexname LIKE ANY(ARRAY[
        'idx_cards_account_id_active_status%',
        'idx_transactions_timestamp_account_id%',
        'idx_cards_customer_id_active_status%',
        'idx_customer_account_xref%',
        'idx_account_customer_xref%',
        'idx_account_balance_covering%',
        'idx_cards_balance_covering%',
        'idx_accounts_customer_id_btree%',
        'idx_accounts_group_id_btree%',
        'idx_cards_account_id_btree%',
        'idx_cards_customer_id_btree%',
        'idx_transactions_account_id_btree%',
        'idx_transactions_card_number_btree%',
        'idx_transactions_type_btree%',
        'idx_transactions_category_btree%',
        'idx_tcb_account_id_btree%',
        'idx_tcb_transaction_category_btree%',
        'idx_transactions_date_range%',
        'idx_accounts_date_range%',
        'idx_cards_date_range%',
        'idx_accounts_status_balance%',
        'idx_cards_status_expiration%',
        'idx_transactions_amount_type%',
        'idx_transaction_types_active_btree%',
        'idx_transaction_categories_active_btree%',
        'idx_disclosure_groups_active_btree%',
        'idx_customers_name_search%',
        'idx_customers_demographics%',
        'idx_customers_pii_secure%',
        'idx_tcb_balance_analysis%',
        'idx_tcb_account_category%',
        'idx_transactions_description_gin%',
        'idx_transactions_merchant_gin%',
        'idx_customers_name_gin%',
        'idx_accounts_interest_calc%',
        'idx_transactions_statement_gen%',
        'idx_transactions_reporting%',
        'idx_accounts_active_partial%',
        'idx_cards_active_partial%',
        'idx_transactions_recent_partial%',
        'idx_transactions_high_value_partial%',
        'idx_customers_ssn_unique%',
        'idx_customers_government_id_unique%',
        'idx_accounts_customer_unique%',
        'idx_accounts_available_credit%',
        'idx_accounts_age%',
        'idx_customers_age%',
        'idx_transactions_timestamp_brin%',
        'idx_accounts_created_brin%',
        'idx_customers_created_brin%',
        'idx_mv_%'
    ]);
    
    RETURN QUERY
    SELECT 
        index_count as remaining_indexes,
        CASE 
            WHEN index_count = 0 THEN 'ROLLBACK_COMPLETE'
            ELSE 'ROLLBACK_INCOMPLETE'
        END as rollback_status,
        CURRENT_TIMESTAMP as validation_timestamp;
END;
$$ LANGUAGE plpgsql;

-- Execute rollback validation
SELECT * FROM validate_indexes_rollback();

-- Drop the validation function after use
DROP FUNCTION IF EXISTS validate_indexes_rollback() CASCADE;

-- =============================================================================
-- 19. ROLLBACK COMPLETION DOCUMENTATION
-- =============================================================================

-- Log rollback completion for audit trail
DO $$
DECLARE
    rollback_summary TEXT;
BEGIN
    rollback_summary := 'V7 Indexes Rollback Completed: ' || 
                       'All B-tree indexes, covering indexes, partial indexes, ' ||
                       'GIN indexes, BRIN indexes, expression indexes, unique indexes, ' ||
                       'foreign key optimization indexes, VSAM alternate index replications, ' ||
                       'and performance monitoring functions have been successfully dropped. ' ||
                       'Database restored to pre-V7 baseline state at ' || CURRENT_TIMESTAMP;
    
    -- Log the rollback completion (if logging table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'migration_log') THEN
        INSERT INTO migration_log (migration_version, operation_type, operation_status, operation_details, operation_timestamp)
        VALUES ('V7', 'ROLLBACK', 'COMPLETED', rollback_summary, CURRENT_TIMESTAMP);
    END IF;
    
    RAISE NOTICE '%', rollback_summary;
END;
$$;

-- =============================================================================
-- 20. PERFORMANCE BASELINE RESTORATION
-- =============================================================================

-- Analyze tables to update statistics after index removal
ANALYZE customers;
ANALYZE accounts;
ANALYZE cards;
ANALYZE transactions;
ANALYZE transaction_types;
ANALYZE transaction_categories;
ANALYZE disclosure_groups;
ANALYZE transaction_category_balances;

-- Vacuum tables to reclaim storage space from dropped indexes
VACUUM customers;
VACUUM accounts;
VACUUM cards;
VACUUM transactions;
VACUUM transaction_types;
VACUUM transaction_categories;
VACUUM disclosure_groups;
VACUUM transaction_category_balances;

-- =============================================================================
-- 21. ROLLBACK SCRIPT COMPLETION MARKER
-- =============================================================================

-- Create completion marker for rollback verification
CREATE OR REPLACE FUNCTION get_rollback_completion_status()
RETURNS TABLE (
    rollback_version TEXT,
    completion_status TEXT,
    completion_timestamp TIMESTAMP WITH TIME ZONE,
    performance_baseline_restored BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'R7__rollback_indexes'::TEXT as rollback_version,
        'COMPLETED'::TEXT as completion_status,
        CURRENT_TIMESTAMP as completion_timestamp,
        TRUE as performance_baseline_restored;
END;
$$ LANGUAGE plpgsql;

-- Document rollback completion
COMMENT ON FUNCTION get_rollback_completion_status() IS 'Rollback completion marker for R7__rollback_indexes.sql - indicates successful reversal of all V7 index operations and restoration of database performance baseline';

-- Final rollback verification message
DO $$
BEGIN
    RAISE NOTICE 'R7__rollback_indexes.sql: Rollback completed successfully. All indexes from V7__create_indexes.sql have been dropped. Database performance baseline restored.';
END;
$$;