--liquibase formatted sql

--changeset blitzy-agent:005-create-test-indexes splitStatements:false rollbackSplit:false
--comment: Create PostgreSQL B-tree indexes for test tables replicating VSAM alternate index performance characteristics for integration testing

-- ======================================================================================
-- PostgreSQL B-tree Index Creation for CardDemo Test Environment
-- ======================================================================================
-- Purpose: Create comprehensive B-tree indexes on test tables to replicate VSAM alternate
--          index functionality, supporting sub-200ms response time validation and 
--          optimizing integration test query performance for account-card-transaction
--          relationships.
--
-- VSAM Equivalent Mapping:
-- - CARDAIX alternate index → idx_test_cards_account_id
-- - CXACAIX cross-reference → idx_test_customer_account_xref  
-- - Sequential TRANSACT access → idx_test_transactions_date_range
-- - ACCTDAT direct access → idx_test_account_balance
-- ======================================================================================

-- ======================================================================================
-- USERS TABLE INDEXES
-- ======================================================================================
-- Performance indexes for authentication service testing and user management operations
-- Supports JWT authentication testing and role-based access validation

-- Index for user type filtering and role-based queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_users_type_status
    ON users (user_type, last_login)
    WHERE user_type IS NOT NULL;

-- Index for authentication timestamp queries and session management testing
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_users_login_activity
    ON users (last_login DESC, created_at)
    WHERE last_login IS NOT NULL;

-- Covering index for user profile queries with frequently accessed fields
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_users_profile_lookup
    ON users (user_id) 
    INCLUDE (first_name, last_name, user_type, created_at);

-- ======================================================================================
-- ACCOUNTS TABLE INDEXES  
-- ======================================================================================
-- Core performance indexes for account management operations replicating VSAM ACCTDAT
-- access patterns and supporting COBOL-to-Java service testing

-- Primary customer-account relationship index (replicates CXACAIX functionality)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_customer_account_xref
    ON accounts (customer_id, account_id)
    WHERE active_status = 'A';

-- Account balance covering index for balance inquiry optimization (sub-5ms target)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_account_balance
    ON accounts (account_id) 
    INCLUDE (current_balance, credit_limit, active_status, customer_id);

-- Multi-column index for account status and balance range queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_accounts_status_balance
    ON accounts (active_status, current_balance DESC)
    WHERE active_status IN ('A', 'S', 'C');

-- Index for credit limit analysis and account grouping operations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_accounts_credit_group
    ON accounts (group_id, credit_limit DESC, current_balance)
    WHERE group_id IS NOT NULL AND active_status = 'A';

-- Date-based index for account lifecycle management and expiration queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_accounts_date_range
    ON accounts (open_date, expiration_date)
    WHERE expiration_date > CURRENT_DATE;

-- Partial index for high-balance accounts (performance testing scenarios)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_accounts_high_balance
    ON accounts (current_balance DESC, credit_limit DESC)
    WHERE current_balance > 10000.00;

-- ======================================================================================
-- CARDS TABLE INDEXES
-- ======================================================================================
-- Indexes supporting card management operations and account-card relationship queries
-- Replicates VSAM CARDDAT alternate index functionality

-- Primary account-card relationship index (replicates CARDAIX alternate index)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_cards_account_id
    ON cards (account_id, active_status)
    WHERE active_status IN ('A', 'S');

-- Customer-card relationship index for customer card portfolio queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_cards_customer_portfolio
    ON cards (customer_id, expiration_date DESC)
    WHERE active_status = 'A';

-- Card expiration monitoring index for batch processing testing
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_cards_expiration_tracking
    ON cards (expiration_date, active_status)
    WHERE expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '6 months';

-- Covering index for card details lookup optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_cards_details_lookup
    ON cards (card_number)
    INCLUDE (account_id, customer_id, embossed_name, expiration_date, active_status);

-- ======================================================================================
-- TRANSACTIONS TABLE INDEXES
-- ======================================================================================
-- Comprehensive indexing strategy for transaction processing and reporting operations
-- Supports monthly RANGE partitioning and date-range query optimization

-- Primary date-range transaction index (replicates sequential TRANSACT access)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_date_range
    ON transactions (transaction_timestamp DESC, account_id)
    WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '13 months';

-- Account transaction history index for account statement generation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_account_history
    ON transactions (account_id, transaction_timestamp DESC)
    INCLUDE (transaction_amount, transaction_type, description);

-- Card-based transaction lookup for card activity analysis
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_card_activity
    ON transactions (card_number, transaction_timestamp DESC)
    WHERE card_number IS NOT NULL;

-- Transaction type and category analysis index for reporting
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_type_category
    ON transactions (transaction_type, transaction_category, transaction_timestamp DESC)
    WHERE transaction_type IS NOT NULL;

-- Amount-based index for transaction analysis and fraud detection testing
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_amount_analysis
    ON transactions (transaction_amount DESC, transaction_timestamp DESC)
    WHERE ABS(transaction_amount) > 100.00;

-- Merchant-based transaction clustering for location analysis
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_merchant_location
    ON transactions (merchant_name, merchant_city, transaction_timestamp DESC)
    WHERE merchant_name IS NOT NULL;

-- Monthly partition-aware index for batch processing optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transactions_monthly_batch
    ON transactions (DATE_TRUNC('month', transaction_timestamp), transaction_type)
    WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '13 months';

-- ======================================================================================
-- REFERENCE TABLES INDEXES
-- ======================================================================================
-- Indexes for transaction type and category lookup optimization
-- Supports cached reference data queries and validation operations

-- Transaction type lookup optimization (frequently accessed during transaction processing)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_trantype_lookup
    ON transaction_types (type_description)
    WHERE debit_credit_indicator IS NOT NULL;

-- Transaction category lookup with active status filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_trancatg_lookup
    ON transaction_categories (category_description, active_status)
    WHERE active_status = true;

-- ======================================================================================
-- COMPOSITE RELATIONSHIP INDEXES
-- ======================================================================================
-- Advanced multi-table relationship indexes for complex query optimization
-- Supporting customer-account-card-transaction integration testing scenarios

-- Customer financial overview composite index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_customer_financial_overview
    ON accounts (customer_id, current_balance DESC, credit_limit DESC, open_date)
    WHERE active_status = 'A';

-- Account transaction summary index for statement processing
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_account_transaction_summary
    ON transactions (account_id, DATE_TRUNC('month', transaction_timestamp), transaction_type)
    WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '13 months';

-- ======================================================================================
-- PERFORMANCE MONITORING INDEXES
-- ======================================================================================
-- Specialized indexes for performance testing and query optimization validation
-- Supporting sub-200ms response time SLA validation in integration tests

-- Response time optimization index for account balance inquiries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_balance_inquiry_performance
    ON accounts (account_id)
    INCLUDE (current_balance, available_credit_calculated)
    WHERE active_status = 'A';

-- Transaction processing performance index for real-time operations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_transaction_processing_performance
    ON transactions (account_id, transaction_timestamp DESC)
    WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '1 day';

-- ======================================================================================
-- ANALYTICAL INDEXES FOR REPORTING
-- ======================================================================================
-- Indexes supporting business intelligence queries and reporting operations
-- Optimized for integration test scenarios requiring complex data analysis

-- Financial metrics calculation index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_financial_metrics
    ON transactions (account_id, transaction_type, DATE_TRUNC('month', transaction_timestamp))
    WHERE transaction_amount != 0.00
    AND transaction_timestamp >= CURRENT_DATE - INTERVAL '24 months';

-- Customer activity analysis index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_customer_activity_analysis
    ON transactions (account_id, DATE_TRUNC('week', transaction_timestamp), merchant_city)
    WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '6 months';

-- ======================================================================================
-- INDEX MAINTENANCE AND STATISTICS
-- ======================================================================================
-- Ensure optimal query planner statistics for all created indexes

-- Update table statistics for optimal query planning
ANALYZE users;
ANALYZE accounts;  
ANALYZE cards;
ANALYZE transactions;
ANALYZE transaction_types;
ANALYZE transaction_categories;

-- ======================================================================================
-- INDEX VALIDATION QUERIES
-- ======================================================================================
-- Validation queries to ensure indexes are properly created and functional

-- Verify index creation and usage statistics
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes 
WHERE indexname LIKE 'idx_test_%'
ORDER BY tablename, indexname;

-- Check index sizes and efficiency
SELECT 
    schemaname,
    tablename, 
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    idx_scan,
    round(100.0 * idx_scan / GREATEST(seq_scan + idx_scan, 1), 2) as index_usage_pct
FROM pg_stat_user_indexes 
WHERE indexname LIKE 'idx_test_%'
ORDER BY pg_relation_size(indexrelid) DESC;

--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_users_type_status;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_users_login_activity;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_users_profile_lookup;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_customer_account_xref;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_account_balance;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_accounts_status_balance;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_accounts_credit_group;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_accounts_date_range;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_accounts_high_balance;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_cards_account_id;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_cards_customer_portfolio;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_cards_expiration_tracking;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_cards_details_lookup;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_date_range;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_account_history;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_card_activity;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_type_category;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_amount_analysis;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_merchant_location;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transactions_monthly_batch;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_trantype_lookup;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_trancatg_lookup;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_customer_financial_overview;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_account_transaction_summary;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_balance_inquiry_performance;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_transaction_processing_performance;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_financial_metrics;
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_test_customer_activity_analysis;