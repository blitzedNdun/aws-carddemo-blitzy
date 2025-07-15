-- ============================================================================
-- Liquibase Rollback Script: R7__rollback_indexes.sql
-- Description: Rollback script for V7__create_indexes.sql - Removes all B-tree indexes
-- Author: Blitzy agent
-- Version: 7.0
-- Rollback target: V7__create_indexes.sql
-- ============================================================================

-- ============================================================================
-- ROLLBACK STRATEGY
-- This script reverses the comprehensive B-tree index creation performed by
-- V7__create_indexes.sql, removing all secondary indexes while preserving
-- primary key constraints. The rollback is performed in reverse order to
-- avoid dependency conflicts and maintain database integrity.
-- ============================================================================

-- ============================================================================
-- ROLLBACK VALIDATION - Pre-rollback checks
-- ============================================================================

-- Verify database state before rollback
-- This section can be uncommented for debugging purposes
-- SELECT 'Pre-rollback index count: ' || COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname NOT LIKE '%_pkey';

-- ============================================================================
-- COMPOSITE INDEXES ROLLBACK - Remove complex relationship indexes first
-- ============================================================================

-- Remove customer financial summary index supporting dashboard queries
DROP INDEX IF EXISTS idx_customer_financial_summary CASCADE;

-- Remove account-card-transaction relationship index supporting transaction analysis
DROP INDEX IF EXISTS idx_account_card_transaction_analysis CASCADE;

-- Remove customer-account-card relationship index supporting complete portfolio queries
DROP INDEX IF EXISTS idx_customer_account_card_portfolio CASCADE;

-- ============================================================================
-- SPECIALIZED INDEXES ROLLBACK - Spring Data JPA and microservices optimization
-- ============================================================================

-- Remove Spring Data JPA sort operations index on customer queries
DROP INDEX IF EXISTS idx_customers_sort_optimized CASCADE;

-- Remove Spring Data JPA Pageable query indexes
DROP INDEX IF EXISTS idx_cards_pageable CASCADE;
DROP INDEX IF EXISTS idx_transactions_pageable CASCADE;
DROP INDEX IF EXISTS idx_accounts_pageable CASCADE;

-- ============================================================================
-- FOREIGN KEY INDEXES ROLLBACK - Join optimization removal
-- ============================================================================

-- Remove transaction categories foreign key indexes
DROP INDEX IF EXISTS idx_transaction_categories_fk_parent_type CASCADE;

-- Remove transaction category balances foreign key indexes
DROP INDEX IF EXISTS idx_tcatbal_fk_transaction_category CASCADE;
DROP INDEX IF EXISTS idx_tcatbal_fk_account_id CASCADE;

-- Remove accounts table foreign key indexes
DROP INDEX IF EXISTS idx_accounts_fk_group_id CASCADE;
DROP INDEX IF EXISTS idx_accounts_fk_customer_id CASCADE;

-- Remove transactions table foreign key indexes
DROP INDEX IF EXISTS idx_transactions_fk_transaction_category CASCADE;
DROP INDEX IF EXISTS idx_transactions_fk_transaction_type CASCADE;
DROP INDEX IF EXISTS idx_transactions_fk_card_number CASCADE;
DROP INDEX IF EXISTS idx_transactions_fk_account_id CASCADE;

-- Remove cards table foreign key indexes
DROP INDEX IF EXISTS idx_cards_fk_customer_id CASCADE;
DROP INDEX IF EXISTS idx_cards_fk_account_id CASCADE;

-- ============================================================================
-- REFERENCE TABLES INDEXES ROLLBACK - High-frequency lookup optimization removal
-- ============================================================================

-- Remove transaction category balances table indexes
DROP INDEX IF EXISTS idx_transaction_category_balances_category CASCADE;
DROP INDEX IF EXISTS idx_transaction_category_balances_account CASCADE;

-- Remove disclosure groups table indexes
DROP INDEX IF EXISTS idx_disclosure_groups_rates CASCADE;

-- Remove transaction categories table indexes
DROP INDEX IF EXISTS idx_transaction_categories_hierarchy CASCADE;

-- Remove transaction types table indexes
DROP INDEX IF EXISTS idx_transaction_types_lookup CASCADE;

-- ============================================================================
-- CUSTOMERS TABLE INDEXES ROLLBACK - Customer lookup and demographic queries removal
-- ============================================================================

-- Remove SSN-based customer lookup index with PII protection
DROP INDEX IF EXISTS idx_customers_ssn_lookup CASCADE;

-- Remove partial index for customers with high FICO scores
DROP INDEX IF EXISTS idx_customers_high_fico CASCADE;

-- Remove customer demographic analysis index
DROP INDEX IF EXISTS idx_customers_demographics CASCADE;

-- Remove customer location-based queries index
DROP INDEX IF EXISTS idx_customers_geographic CASCADE;

-- Remove customer name-based searches index
DROP INDEX IF EXISTS idx_customers_name_search CASCADE;

-- ============================================================================
-- TRANSACTIONS TABLE INDEXES ROLLBACK - Time-range and account-based access patterns removal
-- ============================================================================

-- Remove partial index for high-value transactions
DROP INDEX IF EXISTS idx_transactions_high_value_optimized CASCADE;

-- Remove merchant-based transaction queries index
DROP INDEX IF EXISTS idx_transactions_merchant_analysis CASCADE;

-- Remove transaction type and category analysis index
DROP INDEX IF EXISTS idx_transactions_type_category_analysis CASCADE;

-- Remove card-based transaction queries index
DROP INDEX IF EXISTS idx_transactions_card_activity CASCADE;

-- Remove account-based transaction history index
DROP INDEX IF EXISTS idx_transactions_account_history CASCADE;

-- Remove B-tree index for time-range transaction queries (primary VSAM AIX equivalent)
DROP INDEX IF EXISTS idx_transactions_date_range CASCADE;

-- ============================================================================
-- ACCOUNTS TABLE INDEXES ROLLBACK - Customer-Account relationship optimization removal
-- ============================================================================

-- Remove disclosure group relationship index
DROP INDEX IF EXISTS idx_accounts_disclosure_group CASCADE;

-- Remove account lifecycle management index
DROP INDEX IF EXISTS idx_accounts_lifecycle_dates CASCADE;

-- Remove account financial summary queries index
DROP INDEX IF EXISTS idx_accounts_financial_summary CASCADE;

-- Remove covering index for account balance queries (critical for sub-200ms response times)
DROP INDEX IF EXISTS idx_account_balance CASCADE;

-- Remove B-tree index replicating customer-account cross-reference functionality (CXACAIX equivalent)
DROP INDEX IF EXISTS idx_customer_account_xref CASCADE;

-- ============================================================================
-- CARDS TABLE INDEXES ROLLBACK - Replicating CARDAIX and CARDXREF functionality removal
-- ============================================================================

-- Remove partial index for active cards only
DROP INDEX IF EXISTS idx_cards_active_only_optimized CASCADE;

-- Remove card expiration management index
DROP INDEX IF EXISTS idx_cards_expiration_management CASCADE;

-- Remove covering index for card-account relationship queries
DROP INDEX IF EXISTS idx_cards_account_covering CASCADE;

-- Remove primary B-tree index replicating CARDAIX alternate index functionality
DROP INDEX IF EXISTS idx_cards_account_id CASCADE;

-- ============================================================================
-- ROLLBACK VALIDATION - Post-rollback verification
-- ============================================================================

-- Verify successful rollback by checking remaining indexes
-- All secondary indexes should be removed, leaving only primary keys and unique constraints
-- This section can be uncommented for debugging purposes
-- SELECT 'Post-rollback index count: ' || COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname NOT LIKE '%_pkey' AND indexname NOT LIKE '%_key';

-- ============================================================================
-- PERFORMANCE BASELINE RESTORATION
-- ============================================================================

-- After rollback, the database returns to its pre-V7 performance baseline
-- Query performance will revert to primary key and unique constraint access patterns
-- This enables testing of performance improvements provided by the index layer

-- Critical performance implications of rollback:
-- 1. Card lookups by account ID will use sequential scans instead of B-tree traversal
-- 2. Customer-account relationship queries will perform join operations without index optimization
-- 3. Transaction date-range queries will scan entire partitions instead of using time-based pruning
-- 4. Account balance queries will require full table access instead of index-only scans

-- ============================================================================
-- ROLLBACK COMPLETION NOTIFICATION
-- ============================================================================

-- Log successful rollback completion
-- This can be used for monitoring and deployment verification
SELECT 'R7__rollback_indexes.sql completed successfully. All B-tree indexes from V7__create_indexes.sql have been removed.' AS rollback_status;

-- ============================================================================
-- RECOVERY INSTRUCTIONS
-- ============================================================================

-- To restore the indexes after rollback, re-run the forward migration:
-- 1. Execute V7__create_indexes.sql migration
-- 2. Verify index creation with EXPLAIN ANALYZE on critical queries
-- 3. Validate performance meets sub-200ms response time requirements
-- 4. Monitor index usage statistics for optimization opportunities

-- For production rollback scenarios:
-- 1. Schedule rollback during low-traffic periods
-- 2. Monitor query performance degradation after rollback
-- 3. Have forward migration ready for immediate re-application if needed
-- 4. Consider using DROP INDEX CONCURRENTLY for large tables in production

-- ============================================================================
-- MAINTENANCE PROCEDURES POST-ROLLBACK
-- ============================================================================

-- After rollback, consider the following maintenance procedures:
-- 1. Update query execution plans that previously relied on dropped indexes
-- 2. Monitor slow query log for queries that may need alternative optimization
-- 3. Review application code for queries that may require adjustment
-- 4. Consider temporary query hints if immediate re-indexing is not possible

-- ============================================================================
-- ROLLBACK VERIFICATION QUERIES
-- ============================================================================

-- Use these queries to verify successful rollback:

-- 1. Verify no secondary indexes remain on cards table
-- SELECT indexname FROM pg_indexes WHERE tablename = 'cards' AND schemaname = 'public' AND indexname NOT LIKE '%_pkey';

-- 2. Verify no secondary indexes remain on accounts table  
-- SELECT indexname FROM pg_indexes WHERE tablename = 'accounts' AND schemaname = 'public' AND indexname NOT LIKE '%_pkey';

-- 3. Verify no secondary indexes remain on transactions table
-- SELECT indexname FROM pg_indexes WHERE tablename = 'transactions' AND schemaname = 'public' AND indexname NOT LIKE '%_pkey';

-- 4. Verify no secondary indexes remain on customers table
-- SELECT indexname FROM pg_indexes WHERE tablename = 'customers' AND schemaname = 'public' AND indexname NOT LIKE '%_pkey';

-- Expected result: Only primary key indexes should remain after successful rollback