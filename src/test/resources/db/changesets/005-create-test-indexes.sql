-- =====================================================================
-- Liquibase Changeset: Create Test PostgreSQL B-tree Indexes
-- Description: PostgreSQL B-tree indexes for test tables replicating VSAM alternate index performance
-- Author: Blitzy agent
-- Version: CardDemo_v1.0-15-g27d6c6f-68
-- =====================================================================
--
-- This changeset creates comprehensive B-tree indexes for test tables that replicate
-- VSAM alternate index functionality, supporting sub-200ms response time validation
-- and integration testing performance requirements. These indexes are specifically
-- designed for test environments to validate query performance and support
-- comprehensive integration testing scenarios.
--
-- Index Strategy:
-- - Account-card relationship indexes for customer portfolio queries
-- - Transaction date-range indexes for historical data access
-- - Composite indexes for complex join operations
-- - Covering indexes for index-only scan optimization
-- - Performance validation indexes for integration testing
-- =====================================================================

-- liquibase formatted sql

--changeset blitzy-agent:005-create-test-indexes
--comment: Create PostgreSQL B-tree indexes for test tables replicating VSAM alternate index performance characteristics

-- =============================================================================
-- Test Users Table Indexes (Supporting Authentication Performance)
-- =============================================================================

-- Index for user type filtering (Admin/User role-based queries)
-- Replicates RACF group profile access patterns
CREATE INDEX IF NOT EXISTS idx_test_users_user_type_lookup
ON users (user_type, user_id)
WHERE user_type IN ('A', 'U');

-- Index for session management queries (last login tracking)
-- Supports authentication service performance validation
CREATE INDEX IF NOT EXISTS idx_test_users_last_login_active
ON users (last_login DESC, user_id)
WHERE last_login IS NOT NULL;

-- Composite index for user authentication validation
-- Optimizes login service performance for integration testing
CREATE INDEX IF NOT EXISTS idx_test_users_auth_validation
ON users (user_id, user_type, created_at)
WHERE LENGTH(user_id) = 8;

-- =============================================================================
-- Test Accounts Table Indexes (Supporting Account Management Performance)
-- =============================================================================

-- Primary account-user relationship index
-- Replicates CXACAIX cross-reference functionality for customer-account queries
CREATE INDEX IF NOT EXISTS idx_test_customer_account_xref
ON accounts (user_id, account_id, active_status)
WHERE active_status IN ('A', 'S');

-- Account balance covering index for balance inquiry optimization
-- Supports sub-200ms response time validation for account view service
CREATE INDEX IF NOT EXISTS idx_test_account_balance_covering
ON accounts (account_id, active_status) 
INCLUDE (current_balance, credit_limit, cash_credit_limit)
WHERE active_status = 'A';

-- Group-based account filtering for interest rate application
-- Supports financial calculation service performance testing
CREATE INDEX IF NOT EXISTS idx_test_accounts_group_processing
ON accounts (group_id, active_status, account_id)
WHERE group_id IS NOT NULL AND active_status = 'A';

-- Date-range account opening analysis index
-- Supports reporting service integration testing
CREATE INDEX IF NOT EXISTS idx_test_accounts_date_analysis
ON accounts (open_date, active_status, account_id)
WHERE open_date >= '2020-01-01';

-- Financial metrics composite index for account management
-- Optimizes credit limit and balance analysis queries
CREATE INDEX IF NOT EXISTS idx_test_accounts_financial_metrics
ON accounts (active_status, credit_limit DESC, current_balance DESC)
WHERE active_status = 'A' AND credit_limit > 0;

-- =============================================================================
-- Test Cards Table Indexes (Supporting Card Management Performance)
-- =============================================================================

-- Primary card-account relationship index
-- Replicates CARDAIX alternate index for account-based card lookup
CREATE INDEX IF NOT EXISTS idx_test_cards_account_relationship
ON cards (account_id, active_status, card_number)
WHERE active_status IN ('A', 'I');

-- Card expiration monitoring index for maintenance operations
-- Supports card lifecycle management testing
CREATE INDEX IF NOT EXISTS idx_test_cards_expiration_monitoring
ON cards (expiration_date, active_status, card_number)
WHERE expiration_date >= CURRENT_DATE;

-- Customer card portfolio index for comprehensive card queries
-- Supports customer service integration testing
CREATE INDEX IF NOT EXISTS idx_test_cards_customer_portfolio
ON cards (customer_id, active_status, account_id)
WHERE active_status = 'A';

-- Card security validation index for authentication
-- Optimizes card verification service performance
CREATE INDEX IF NOT EXISTS idx_test_cards_security_validation
ON cards (card_number, active_status) 
INCLUDE (customer_id, account_id, expiration_date)
WHERE active_status = 'A';

-- =============================================================================
-- Test Transactions Table Indexes (Supporting Transaction Processing Performance)
-- =============================================================================

-- Primary transaction date-range index for historical queries
-- Replicates sequential TRANSACT access patterns with date partitioning
CREATE INDEX IF NOT EXISTS idx_test_transactions_date_range
ON transactions (processing_timestamp DESC, account_id, transaction_id)
WHERE processing_timestamp >= '2024-01-01';

-- Account transaction history index for account view service
-- Supports transaction listing with pagination for integration testing
CREATE INDEX IF NOT EXISTS idx_test_transactions_account_history
ON transactions (account_id, processing_timestamp DESC)
INCLUDE (transaction_amount, transaction_type, merchant_name);

-- Card transaction processing index for card-based queries
-- Optimizes transaction service performance validation
CREATE INDEX IF NOT EXISTS idx_test_transactions_card_processing
ON transactions (card_number, processing_timestamp DESC, transaction_id)
WHERE processing_timestamp >= '2024-01-01';

-- Transaction type analysis index for reporting
-- Supports transaction classification service testing
CREATE INDEX IF NOT EXISTS idx_test_transactions_type_analysis
ON transactions (transaction_type, transaction_category, processing_timestamp DESC)
INCLUDE (transaction_amount, account_id);

-- Merchant transaction aggregation index for merchant analysis
-- Supports merchant processing service integration testing
CREATE INDEX IF NOT EXISTS idx_test_transactions_merchant_analysis
ON transactions (merchant_id, processing_timestamp DESC)
INCLUDE (transaction_amount, transaction_type, account_id);

-- Transaction amount range index for financial analysis
-- Supports amount-based filtering and aggregation queries
CREATE INDEX IF NOT EXISTS idx_test_transactions_amount_range
ON transactions (transaction_amount DESC, processing_timestamp DESC)
WHERE ABS(transaction_amount) >= 1.00;

-- Original timestamp index for transaction lifecycle tracking
-- Supports audit trail and transaction processing validation
CREATE INDEX IF NOT EXISTS idx_test_transactions_lifecycle_tracking
ON transactions (original_timestamp, processing_timestamp, transaction_id)
WHERE original_timestamp IS NOT NULL;

-- =============================================================================
-- Test Reference Tables Indexes (Supporting Lookup Performance)
-- =============================================================================

-- Transaction types active lookup index
-- Optimizes transaction type validation service performance
CREATE INDEX IF NOT EXISTS idx_test_transaction_types_active_lookup
ON transaction_types (active_status, transaction_type)
WHERE active_status = true;

-- Transaction categories processing priority index
-- Supports transaction categorization service optimization
CREATE INDEX IF NOT EXISTS idx_test_transaction_categories_priority
ON transaction_categories (active_status, processing_priority, transaction_category)
WHERE active_status = true;

-- Transaction type-category relationship index
-- Optimizes transaction classification validation
CREATE INDEX IF NOT EXISTS idx_test_transaction_type_category_mapping
ON transaction_categories (transaction_type, transaction_category, active_status)
WHERE active_status = true;

-- =============================================================================
-- Composite Multi-Table Relationship Indexes
-- =============================================================================

-- Account-card-transaction composite relationship index
-- Supports comprehensive customer transaction analysis
-- This index enables efficient joins across the three main operational tables
CREATE INDEX IF NOT EXISTS idx_test_account_card_transaction_composite
ON transactions (account_id, card_number, transaction_type)
INCLUDE (transaction_amount, processing_timestamp, merchant_name);

-- =============================================================================
-- Performance Validation Indexes for Integration Testing
-- =============================================================================

-- Sub-200ms response time validation index for account operations
-- Optimized for account service performance testing
CREATE INDEX IF NOT EXISTS idx_test_performance_account_operations
ON accounts (account_id) 
INCLUDE (user_id, active_status, current_balance, credit_limit, cash_credit_limit, 
         open_date, expiration_date, current_cycle_credit, current_cycle_debit);

-- Transaction processing performance validation index
-- Supports transaction service sub-300ms response time testing
CREATE INDEX IF NOT EXISTS idx_test_performance_transaction_processing
ON transactions (transaction_id, processing_timestamp)
INCLUDE (account_id, card_number, transaction_type, transaction_amount, 
         merchant_name, original_timestamp);

-- Authentication performance validation index
-- Optimizes authentication service response time testing
CREATE INDEX IF NOT EXISTS idx_test_performance_authentication
ON users (user_id)
INCLUDE (password_hash, user_type, first_name, last_name, last_login);

-- =============================================================================
-- VSAM Alternate Index Replication Summary
-- =============================================================================

-- The following indexes replicate specific VSAM alternate index functionality:
--
-- 1. idx_test_customer_account_xref -> Replicates CXACAIX cross-reference
-- 2. idx_test_cards_account_relationship -> Replicates CARDAIX alternate index
-- 3. idx_test_transactions_date_range -> Replicates sequential TRANSACT access
-- 4. idx_test_account_balance_covering -> Replicates ACCTDAT direct access
-- 5. idx_test_performance_* -> Support sub-200ms response time validation
--
-- These indexes collectively ensure that PostgreSQL query performance meets
-- or exceeds the original VSAM access patterns while supporting comprehensive
-- integration testing scenarios for the modernized Spring Boot microservices.

-- =============================================================================
-- Index Performance Monitoring
-- =============================================================================

-- Add comments to key indexes for monitoring and maintenance
COMMENT ON INDEX idx_test_customer_account_xref IS 
'VSAM CXACAIX equivalent - Customer account cross-reference for portfolio queries. Critical for sub-200ms account service performance validation.';

COMMENT ON INDEX idx_test_cards_account_relationship IS 
'VSAM CARDAIX equivalent - Account-based card lookup index. Essential for card management service integration testing.';

COMMENT ON INDEX idx_test_transactions_date_range IS 
'VSAM TRANSACT sequential equivalent - Date-range transaction queries with timestamp partitioning. Supports 4-hour batch processing window validation.';

COMMENT ON INDEX idx_test_account_balance_covering IS 
'VSAM ACCTDAT direct access equivalent - Covering index for balance inquiries. Enables index-only scans for optimal performance.';

COMMENT ON INDEX idx_test_performance_account_operations IS 
'Performance validation index for account operations. Designed to support sub-200ms response time testing in integration environments.';

-- =============================================================================
-- Test Data Volume Considerations
-- =============================================================================

-- These indexes are optimized for test data volumes:
-- - Development: 1,000 customers, 5,000 transactions
-- - Integration: 10,000 customers, 100,000 transactions  
-- - UAT: 50,000 customers, 1,000,000 transactions
-- - Performance: 100,000 customers, 10,000,000 transactions
--
-- Index maintenance is minimal in test environments due to lower data volumes
-- and frequent environment refresh cycles. Performance testing validates
-- that these indexes scale appropriately for production-like data volumes.

-- =============================================================================
-- Integration Test Performance Validation
-- =============================================================================

-- Query performance validation examples for integration testing:
--
-- 1. Account balance inquiry (target: <50ms):
-- SELECT account_id, current_balance, credit_limit 
-- FROM accounts 
-- WHERE account_id = '12345678901' AND active_status = 'A';
--
-- 2. Customer account portfolio (target: <100ms):
-- SELECT a.account_id, a.current_balance, c.card_number 
-- FROM accounts a 
-- JOIN cards c ON a.account_id = c.account_id 
-- WHERE a.user_id = 'USER0001' AND a.active_status = 'A';
--
-- 3. Transaction history query (target: <150ms):
-- SELECT transaction_id, transaction_amount, merchant_name 
-- FROM transactions 
-- WHERE account_id = '12345678901' 
-- AND processing_timestamp >= CURRENT_DATE - INTERVAL '30 days'
-- ORDER BY processing_timestamp DESC 
-- LIMIT 20;
--
-- 4. Account-card-transaction composite query (target: <200ms):
-- SELECT t.transaction_id, t.transaction_amount, t.merchant_name,
--        a.current_balance, c.card_number
-- FROM transactions t
-- JOIN accounts a ON t.account_id = a.account_id
-- JOIN cards c ON t.card_number = c.card_number
-- WHERE t.account_id = '12345678901'
-- AND t.processing_timestamp >= CURRENT_DATE - INTERVAL '7 days'
-- ORDER BY t.processing_timestamp DESC;

-- =============================================================================
-- Rollback Support
-- =============================================================================

--rollback DROP INDEX IF EXISTS idx_test_users_user_type_lookup;
--rollback DROP INDEX IF EXISTS idx_test_users_last_login_active;
--rollback DROP INDEX IF EXISTS idx_test_users_auth_validation;
--rollback DROP INDEX IF EXISTS idx_test_customer_account_xref;
--rollback DROP INDEX IF EXISTS idx_test_account_balance_covering;
--rollback DROP INDEX IF EXISTS idx_test_accounts_group_processing;
--rollback DROP INDEX IF EXISTS idx_test_accounts_date_analysis;
--rollback DROP INDEX IF EXISTS idx_test_accounts_financial_metrics;
--rollback DROP INDEX IF EXISTS idx_test_cards_account_relationship;
--rollback DROP INDEX IF EXISTS idx_test_cards_expiration_monitoring;
--rollback DROP INDEX IF EXISTS idx_test_cards_customer_portfolio;
--rollback DROP INDEX IF EXISTS idx_test_cards_security_validation;
--rollback DROP INDEX IF EXISTS idx_test_transactions_date_range;
--rollback DROP INDEX IF EXISTS idx_test_transactions_account_history;
--rollback DROP INDEX IF EXISTS idx_test_transactions_card_processing;
--rollback DROP INDEX IF EXISTS idx_test_transactions_type_analysis;
--rollback DROP INDEX IF EXISTS idx_test_transactions_merchant_analysis;
--rollback DROP INDEX IF EXISTS idx_test_transactions_amount_range;
--rollback DROP INDEX IF EXISTS idx_test_transactions_lifecycle_tracking;
--rollback DROP INDEX IF EXISTS idx_test_transaction_types_active_lookup;
--rollback DROP INDEX IF EXISTS idx_test_transaction_categories_priority;
--rollback DROP INDEX IF EXISTS idx_test_transaction_type_category_mapping;
--rollback DROP INDEX IF EXISTS idx_test_account_card_transaction_composite;
--rollback DROP INDEX IF EXISTS idx_test_performance_account_operations;
--rollback DROP INDEX IF EXISTS idx_test_performance_transaction_processing;
--rollback DROP INDEX IF EXISTS idx_test_performance_authentication;

-- End of changeset: 005-create-test-indexes