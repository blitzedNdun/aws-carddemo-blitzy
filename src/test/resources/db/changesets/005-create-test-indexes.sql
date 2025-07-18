-- liquibase formatted sql

-- ============================================================================
-- CardDemo Test Environment B-Tree Indexes Creation
-- ============================================================================
-- Purpose: Create PostgreSQL B-tree indexes for test environment based on 
--          VSAM alternate index functionality with performance optimization
--          for sub-200ms response time validation in integration testing
-- Environment: Test environment with comprehensive query performance validation
-- Source: Database Design 6.2.1.3 - Indexing Strategy
-- Dependencies: 001-create-test-users-table.sql, 002-create-test-accounts-table.sql,
--               003-create-test-transactions-table.sql, 004-create-test-reference-tables.sql
-- ============================================================================

-- changeset carddemo:005-create-test-indexes
-- comment: Create B-tree indexes for test environment with VSAM alternate index replication
-- labels: test-environment, performance-indexes, query-optimization
-- preconditions: onFail:HALT onError:HALT
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users' AND table_schema = 'public';
-- expected-result: 1
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'accounts' AND table_schema = 'public';
-- expected-result: 1
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'transactions' AND table_schema = 'public';
-- expected-result: 1
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'transaction_types' AND table_schema = 'public';
-- expected-result: 1

-- ============================================================================
-- VSAM ALTERNATE INDEX REPLICATION STRATEGY
-- ============================================================================
-- Purpose: Replicate VSAM alternate index performance characteristics through
--          PostgreSQL B-tree indexes with exact query optimization patterns
-- Performance Target: Support sub-200ms response time at 95th percentile
-- Integration Point: Spring Boot microservices with JPA repository queries
-- ============================================================================

-- ============================================================================
-- 1. CARD-ACCOUNT RELATIONSHIP INDEXES
-- ============================================================================
-- Replicates: VSAM CARDAIX alternate index functionality
-- Purpose: Optimize account-based card lookup queries for AccountViewService
-- Performance: Sub-5ms account card lookup validation
-- ============================================================================

-- Primary card-account relationship index for account-based queries
-- Maps to: CARDAIX alternate index from VSAM CARDDAT file
-- Usage: AccountViewService.findCardsByAccountId() queries
-- Performance: Index-only scan capability for active status filtering
CREATE INDEX IF NOT EXISTS idx_test_cards_account_id 
    ON cards (account_id, active_status)
    WHERE active_status IN ('A', 'S');

-- Secondary card number lookup index for direct card access
-- Maps to: Primary key access pattern from VSAM CARDDAT
-- Usage: CardListService.findByCardNumber() queries
-- Performance: Unique constraint enforcement with B-tree optimization
CREATE UNIQUE INDEX IF NOT EXISTS idx_test_cards_card_number 
    ON cards (card_number);

-- Composite index for card-customer relationship queries
-- Maps to: VSAM cross-reference pattern for customer-card relationships
-- Usage: CustomerService.findCardsByCustomerId() queries
-- Performance: Supports customer portfolio queries under 10ms
CREATE INDEX IF NOT EXISTS idx_test_cards_customer_id 
    ON cards (customer_id, active_status, account_id)
    WHERE active_status = 'A';

-- ============================================================================
-- 2. CUSTOMER-ACCOUNT CROSS-REFERENCE INDEXES
-- ============================================================================
-- Replicates: VSAM CXACAIX cross-reference index functionality
-- Purpose: Optimize customer-account relationship queries for portfolio views
-- Performance: Customer-account queries optimized for sub-10ms response
-- ============================================================================

-- Primary customer-account cross-reference index
-- Maps to: CXACAIX alternate index from VSAM ACCTDAT file
-- Usage: AccountViewService.findAccountsByCustomerId() queries
-- Performance: Composite index with covering optimization
CREATE INDEX IF NOT EXISTS idx_test_customer_account_xref 
    ON accounts (customer_id, account_id, active_status)
    INCLUDE (current_balance, credit_limit);

-- Account status filtering index for active account queries
-- Maps to: VSAM file access pattern for status-based filtering
-- Usage: AccountService.findActiveAccountsByCustomer() queries
-- Performance: Partial index for active accounts only
CREATE INDEX IF NOT EXISTS idx_test_accounts_active_status 
    ON accounts (active_status, customer_id)
    WHERE active_status = 'A';

-- Account balance range index for financial queries
-- Maps to: VSAM sequential processing pattern for balance analysis
-- Usage: AccountService.findAccountsByBalanceRange() queries
-- Performance: B-tree range scan optimization for financial reporting
CREATE INDEX IF NOT EXISTS idx_test_accounts_balance_range 
    ON accounts (current_balance, account_id)
    WHERE active_status = 'A';

-- ============================================================================
-- 3. TRANSACTION DATE-RANGE INDEXES
-- ============================================================================
-- Replicates: VSAM TRANSACT sequential access patterns
-- Purpose: Optimize date-range transaction queries for reporting and batch processing
-- Performance: Date-range queries with automatic partition pruning
-- ============================================================================

-- Primary date-range index for transaction history queries
-- Maps to: VSAM TRANSACT file sequential processing by date
-- Usage: TransactionService.findTransactionsByDateRange() queries
-- Performance: Supports batch processing 4-hour window requirement
CREATE INDEX IF NOT EXISTS idx_test_transactions_date_range 
    ON transactions (processing_timestamp, account_id)
    INCLUDE (transaction_amount, transaction_type);

-- Transaction type and category composite index
-- Maps to: VSAM cross-reference pattern for transaction classification
-- Usage: TransactionService.findTransactionsByTypeAndCategory() queries
-- Performance: Supports transaction type filtering with sub-50ms response
CREATE INDEX IF NOT EXISTS idx_test_transactions_type_category 
    ON transactions (transaction_type, transaction_category, processing_timestamp)
    INCLUDE (transaction_amount, account_id);

-- Card-based transaction history index
-- Maps to: VSAM alternate index for card transaction lookups
-- Usage: CardService.findTransactionsByCardNumber() queries
-- Performance: Card transaction history queries under 100ms
CREATE INDEX IF NOT EXISTS idx_test_transactions_card_number 
    ON transactions (card_number, processing_timestamp)
    WHERE processing_timestamp >= CURRENT_DATE - INTERVAL '13 months';

-- Account transaction summary index for balance calculations
-- Maps to: VSAM file processing pattern for account balance updates
-- Usage: AccountService.calculateAccountBalance() queries
-- Performance: Real-time balance calculation support under 50ms
CREATE INDEX IF NOT EXISTS idx_test_transactions_account_summary 
    ON transactions (account_id, processing_timestamp)
    INCLUDE (transaction_amount, transaction_type)
    WHERE processing_timestamp >= CURRENT_DATE - INTERVAL '1 month';

-- ============================================================================
-- 4. ACCOUNT BALANCE COVERING INDEXES
-- ============================================================================
-- Replicates: VSAM ACCTDAT direct access patterns
-- Purpose: Optimize account balance queries with index-only scans
-- Performance: Sub-10ms balance inquiry validation
-- ============================================================================

-- Covering index for account balance inquiries
-- Maps to: VSAM ACCTDAT primary key access with balance fields
-- Usage: AccountService.getAccountBalance() queries
-- Performance: Index-only scan for balance inquiries under 1ms
CREATE INDEX IF NOT EXISTS idx_test_account_balance_covering 
    ON accounts (account_id) 
    INCLUDE (current_balance, credit_limit, cash_credit_limit, active_status);

-- Account financial summary index for credit limit validation
-- Maps to: VSAM file processing for credit authorization
-- Usage: AuthorizationService.validateCreditLimit() queries
-- Performance: Credit authorization under 200ms at 95th percentile
CREATE INDEX IF NOT EXISTS idx_test_account_credit_summary 
    ON accounts (account_id, active_status) 
    INCLUDE (current_balance, credit_limit, cash_credit_limit)
    WHERE active_status = 'A';

-- ============================================================================
-- 5. REFERENCE TABLE PERFORMANCE INDEXES
-- ============================================================================
-- Replicates: VSAM reference file access patterns
-- Purpose: Optimize transaction type and category lookup queries
-- Performance: Reference data lookup under 5ms
-- ============================================================================

-- Transaction type lookup index for validation
-- Maps to: VSAM TRANTYPE file access patterns
-- Usage: TransactionService.validateTransactionType() queries
-- Performance: Transaction type validation under 1ms
CREATE INDEX IF NOT EXISTS idx_test_transaction_types_lookup 
    ON transaction_types (transaction_type, active_status)
    WHERE active_status = TRUE;

-- Transaction category lookup index for classification
-- Maps to: VSAM TRANCATG file access patterns
-- Usage: TransactionService.validateTransactionCategory() queries
-- Performance: Transaction category validation under 1ms
CREATE INDEX IF NOT EXISTS idx_test_transaction_categories_lookup 
    ON transaction_categories (transaction_category, active_status)
    WHERE active_status = TRUE;

-- Composite reference lookup index for transaction validation
-- Maps to: VSAM cross-reference validation pattern
-- Usage: TransactionService.validateTransactionTypeAndCategory() queries
-- Performance: Combined validation under 5ms
CREATE INDEX IF NOT EXISTS idx_test_transaction_validation 
    ON transaction_categories (transaction_type, transaction_category, active_status)
    WHERE active_status = TRUE;

-- ============================================================================
-- 6. USER AUTHENTICATION PERFORMANCE INDEXES
-- ============================================================================
-- Replicates: VSAM USRSEC file access patterns
-- Purpose: Optimize user authentication and authorization queries
-- Performance: Authentication response under 100ms
-- ============================================================================

-- User authentication index for login validation
-- Maps to: VSAM USRSEC file primary key access
-- Usage: AuthenticationService.validateUserCredentials() queries
-- Performance: User lookup under 10ms
CREATE INDEX IF NOT EXISTS idx_test_users_authentication 
    ON users (user_id, user_type, password_hash)
    WHERE LENGTH(password_hash) = 60;

-- User type filtering index for role-based access
-- Maps to: VSAM USRSEC file type-based filtering
-- Usage: AuthenticationService.findUsersByType() queries
-- Performance: Role-based queries under 20ms
CREATE INDEX IF NOT EXISTS idx_test_users_type_filter 
    ON users (user_type, user_id)
    WHERE user_type IN ('A', 'U');

-- ============================================================================
-- INDEX PERFORMANCE VALIDATION QUERIES
-- ============================================================================
-- Purpose: Validate index creation and performance characteristics
-- Performance: Ensure all queries meet sub-200ms response time requirements
-- ============================================================================

-- Validate card-account relationship query performance
-- Expected: Index-only scan using idx_test_cards_account_id
-- Performance target: < 5ms
EXPLAIN (ANALYZE, BUFFERS) 
SELECT card_number, active_status 
FROM cards 
WHERE account_id = '10000000001' 
AND active_status = 'A';

-- Validate customer-account cross-reference query performance
-- Expected: Index-only scan using idx_test_customer_account_xref
-- Performance target: < 10ms
EXPLAIN (ANALYZE, BUFFERS) 
SELECT account_id, current_balance, credit_limit 
FROM accounts 
WHERE customer_id = '100000001' 
AND active_status = 'A';

-- Validate transaction date-range query performance
-- Expected: Index scan using idx_test_transactions_date_range with partition pruning
-- Performance target: < 50ms
EXPLAIN (ANALYZE, BUFFERS) 
SELECT transaction_id, transaction_amount, transaction_type 
FROM transactions 
WHERE processing_timestamp >= CURRENT_DATE - INTERVAL '30 days' 
AND account_id = '10000000001';

-- Validate account balance query performance
-- Expected: Index-only scan using idx_test_account_balance_covering
-- Performance target: < 1ms
EXPLAIN (ANALYZE, BUFFERS) 
SELECT current_balance, credit_limit, cash_credit_limit 
FROM accounts 
WHERE account_id = '10000000001';

-- Validate transaction type lookup performance
-- Expected: Index-only scan using idx_test_transaction_types_lookup
-- Performance target: < 1ms
EXPLAIN (ANALYZE, BUFFERS) 
SELECT type_description, debit_credit_flag 
FROM transaction_types 
WHERE transaction_type = 'DB' 
AND active_status = TRUE;

-- ============================================================================
-- INDEX STATISTICS AND MAINTENANCE
-- ============================================================================
-- Purpose: Collect index statistics and set up maintenance procedures
-- Performance: Ensure optimal query planner statistics for test environment
-- ============================================================================

-- Collect statistics on all newly created indexes
ANALYZE cards;
ANALYZE accounts;
ANALYZE transactions;
ANALYZE transaction_types;
ANALYZE transaction_categories;
ANALYZE users;

-- Validate index usage statistics
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
AND indexname LIKE 'idx_test_%'
ORDER BY tablename, indexname;

-- Check index sizes and bloat
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    pg_size_pretty(pg_total_relation_size(indexrelid)) as total_size
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
AND indexname LIKE 'idx_test_%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- ============================================================================
-- PERFORMANCE MONITORING SETUP
-- ============================================================================
-- Purpose: Set up monitoring for index performance and query optimization
-- Integration: Prometheus metrics collection for performance validation
-- ============================================================================

-- Create extension for performance monitoring if not exists
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Reset statistics for clean performance measurement
SELECT pg_stat_statements_reset();

-- ============================================================================
-- COMMENTS FOR TEST ENVIRONMENT DOCUMENTATION
-- ============================================================================

COMMENT ON INDEX idx_test_cards_account_id IS 'B-tree index replicating VSAM CARDAIX alternate index for account-based card lookups with sub-5ms performance target';
COMMENT ON INDEX idx_test_customer_account_xref IS 'Composite index replicating VSAM CXACAIX cross-reference for customer-account relationships with covering optimization';
COMMENT ON INDEX idx_test_transactions_date_range IS 'Date-range index for transaction history queries supporting batch processing 4-hour window requirement';
COMMENT ON INDEX idx_test_account_balance_covering IS 'Covering index for account balance inquiries with index-only scan capability under 1ms';
COMMENT ON INDEX idx_test_transaction_types_lookup IS 'Reference table index for transaction type validation with sub-1ms lookup performance';
COMMENT ON INDEX idx_test_transaction_categories_lookup IS 'Reference table index for transaction category validation with sub-1ms lookup performance';
COMMENT ON INDEX idx_test_users_authentication IS 'User authentication index for login validation with sub-10ms performance target';

-- ============================================================================
-- ROLLBACK PROCEDURES
-- ============================================================================
-- Purpose: Provide rollback capability for index removal if needed
-- Safety: Ensure clean environment reset for testing scenarios
-- ============================================================================

-- rollback DROP INDEX IF EXISTS idx_test_cards_account_id;
-- rollback DROP INDEX IF EXISTS idx_test_cards_card_number;
-- rollback DROP INDEX IF EXISTS idx_test_cards_customer_id;
-- rollback DROP INDEX IF EXISTS idx_test_customer_account_xref;
-- rollback DROP INDEX IF EXISTS idx_test_accounts_active_status;
-- rollback DROP INDEX IF EXISTS idx_test_accounts_balance_range;
-- rollback DROP INDEX IF EXISTS idx_test_transactions_date_range;
-- rollback DROP INDEX IF EXISTS idx_test_transactions_type_category;
-- rollback DROP INDEX IF EXISTS idx_test_transactions_card_number;
-- rollback DROP INDEX IF EXISTS idx_test_transactions_account_summary;
-- rollback DROP INDEX IF EXISTS idx_test_account_balance_covering;
-- rollback DROP INDEX IF EXISTS idx_test_account_credit_summary;
-- rollback DROP INDEX IF EXISTS idx_test_transaction_types_lookup;
-- rollback DROP INDEX IF EXISTS idx_test_transaction_categories_lookup;
-- rollback DROP INDEX IF EXISTS idx_test_transaction_validation;
-- rollback DROP INDEX IF EXISTS idx_test_users_authentication;
-- rollback DROP INDEX IF EXISTS idx_test_users_type_filter;
-- rollback ANALYZE cards;
-- rollback ANALYZE accounts;
-- rollback ANALYZE transactions;
-- rollback ANALYZE transaction_types;
-- rollback ANALYZE transaction_categories;
-- rollback ANALYZE users;

-- ============================================================================
-- PERFORMANCE VALIDATION SUMMARY
-- ============================================================================
-- Integration Test Performance Targets:
-- - Card-account relationship queries: < 5ms (idx_test_cards_account_id)
-- - Customer-account cross-reference: < 10ms (idx_test_customer_account_xref)
-- - Transaction date-range queries: < 50ms (idx_test_transactions_date_range)
-- - Account balance inquiries: < 1ms (idx_test_account_balance_covering)
-- - Reference table lookups: < 1ms (transaction type/category indexes)
-- - User authentication: < 10ms (idx_test_users_authentication)
-- 
-- Overall Performance Goal: Sub-200ms response time at 95th percentile
-- VSAM Alternate Index Replication: Complete functional equivalence
-- Spring Boot Integration: JPA repository query optimization
-- Test Environment: Full PostgreSQL B-tree index coverage
-- ============================================================================