-- =====================================================================================
-- Liquibase Migration: V7__create_indexes.sql
-- Description: Creates comprehensive B-tree index strategy replicating VSAM alternate
--              index functionality with covering indexes and optimized access patterns
--              for high-performance microservices operations supporting sub-200ms
--              response times and Spring Data JPA query optimization
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 7.0
-- Dependencies: V2__create_customers_table.sql, V3__create_accounts_table.sql,
--               V4__create_cards_table.sql, V5__create_transactions_table.sql,
--               V6__create_reference_tables.sql
-- =====================================================================================

-- changeset blitzy:V7-create-indexes
-- comment: Create comprehensive B-tree index strategy replicating VSAM alternate index functionality with covering indexes

-- =============================================================================
-- 1. VSAM ALTERNATE INDEX REPLICATION STRATEGY
-- =============================================================================

-- Replicate CARDDATA.VSAM.AIX alternate index functionality
-- Original VSAM: KEYLEN=11, RKP=5, AXRKP=16 (account_id as alternate key)
-- Purpose: Enable rapid card lookups by account ID and active status
CREATE INDEX idx_cards_account_id_active_status ON cards(account_id, active_status)
INCLUDE (card_number, customer_id, expiration_date, embossed_name);

-- Replicate TRANSACT.VSAM.AIX alternate index functionality  
-- Original VSAM: KEYLEN=26, RKP=5, AXRKP=304 (timestamp + account_id as alternate key)
-- Purpose: Enable efficient time-range queries with account filtering
CREATE INDEX idx_transactions_timestamp_account_id ON transactions(transaction_timestamp, account_id)
INCLUDE (transaction_id, transaction_amount, transaction_type, transaction_category, description);

-- Replicate CARDXREF.VSAM.AIX alternate index functionality
-- Original VSAM: KEYLEN=11, RKP=5, AXRKP=25 (customer_id as alternate key)
-- Purpose: Enable rapid card lookups by customer ID for cross-reference operations
CREATE INDEX idx_cards_customer_id_active_status ON cards(customer_id, active_status)
INCLUDE (card_number, account_id, expiration_date, embossed_name);

-- =============================================================================
-- 2. CUSTOMER-ACCOUNT CROSS-REFERENCE OPTIMIZATION
-- =============================================================================

-- Create B-tree index for customer-account relationship queries
-- Purpose: Support rapid customer-account cross-reference lookups matching VSAM XREF functionality
CREATE INDEX idx_customer_account_xref ON accounts(customer_id, account_id)
INCLUDE (active_status, current_balance, credit_limit, open_date, expiration_date);

-- Create reverse lookup index for account-customer relationships
-- Purpose: Support bi-directional customer-account relationship queries
CREATE INDEX idx_account_customer_xref ON accounts(account_id, customer_id)
INCLUDE (active_status, current_balance, credit_limit, cash_credit_limit);

-- =============================================================================
-- 3. COVERING INDEXES FOR BALANCE QUERIES
-- =============================================================================

-- Create covering index for account balance queries (index-only scans)
-- Purpose: Enable index-only scans for frequent balance queries without table access
CREATE INDEX idx_account_balance_covering ON accounts(account_id)
INCLUDE (current_balance, credit_limit, cash_credit_limit, active_status, customer_id);

-- Create covering index for card-account balance queries
-- Purpose: Support card-based balance inquiries with index-only scans
CREATE INDEX idx_cards_balance_covering ON cards(card_number)
INCLUDE (account_id, customer_id, active_status, expiration_date);

-- =============================================================================
-- 4. FOREIGN KEY OPTIMIZATION INDEXES
-- =============================================================================

-- Optimize accounts table foreign key relationships
CREATE INDEX idx_accounts_customer_id_btree ON accounts(customer_id);
CREATE INDEX idx_accounts_group_id_btree ON accounts(group_id);

-- Optimize cards table foreign key relationships
CREATE INDEX idx_cards_account_id_btree ON cards(account_id);
CREATE INDEX idx_cards_customer_id_btree ON cards(customer_id);

-- Optimize transactions table foreign key relationships
CREATE INDEX idx_transactions_account_id_btree ON transactions(account_id);
CREATE INDEX idx_transactions_card_number_btree ON transactions(card_number);
CREATE INDEX idx_transactions_type_btree ON transactions(transaction_type);
CREATE INDEX idx_transactions_category_btree ON transactions(transaction_category);

-- Optimize transaction_category_balances table foreign key relationships
CREATE INDEX idx_tcb_account_id_btree ON transaction_category_balances(account_id);
CREATE INDEX idx_tcb_transaction_category_btree ON transaction_category_balances(transaction_category);

-- =============================================================================
-- 5. DATE AND TIME-RANGE QUERY OPTIMIZATION
-- =============================================================================

-- Create B-tree index for time-range queries with partition pruning
-- Purpose: Support efficient date-range queries across partitioned transaction data
CREATE INDEX idx_transactions_date_range ON transactions(transaction_timestamp, account_id, transaction_amount)
INCLUDE (transaction_id, transaction_type, transaction_category, description, merchant_name);

-- Create index for account lifecycle date queries
-- Purpose: Support account expiration and renewal management queries
CREATE INDEX idx_accounts_date_range ON accounts(open_date, expiration_date, account_id)
INCLUDE (active_status, current_balance, customer_id);

-- Create index for card lifecycle date queries
-- Purpose: Support card expiration and renewal management queries
CREATE INDEX idx_cards_date_range ON cards(expiration_date, active_status, card_number)
INCLUDE (account_id, customer_id, embossed_name);

-- =============================================================================
-- 6. COMPOSITE INDEXES FOR MICROSERVICES QUERY PATTERNS
-- =============================================================================

-- Create composite index for account status and balance queries
-- Purpose: Support microservices queries filtering by status and balance ranges
CREATE INDEX idx_accounts_status_balance ON accounts(active_status, current_balance, account_id)
INCLUDE (customer_id, credit_limit, cash_credit_limit, group_id);

-- Create composite index for card status and expiration queries
-- Purpose: Support microservices queries for active cards and expiration management
CREATE INDEX idx_cards_status_expiration ON cards(active_status, expiration_date, card_number)
INCLUDE (account_id, customer_id, embossed_name);

-- Create composite index for transaction amount and type queries
-- Purpose: Support microservices queries for transaction analysis and reporting
CREATE INDEX idx_transactions_amount_type ON transactions(transaction_amount, transaction_type, transaction_timestamp)
INCLUDE (account_id, transaction_id, transaction_category, description);

-- =============================================================================
-- 7. REFERENCE DATA OPTIMIZATION INDEXES
-- =============================================================================

-- Create B-tree indexes for reference table optimization
-- Purpose: Support sub-millisecond reference data lookups for microservices

-- Transaction types reference optimization
CREATE INDEX idx_transaction_types_active_btree ON transaction_types(active_status, transaction_type)
INCLUDE (type_description, debit_credit_indicator);

-- Transaction categories reference optimization
CREATE INDEX idx_transaction_categories_active_btree ON transaction_categories(active_status, transaction_category)
INCLUDE (category_description);

-- Disclosure groups reference optimization
CREATE INDEX idx_disclosure_groups_active_btree ON disclosure_groups(active_status, group_id)
INCLUDE (interest_rate, effective_date, disclosure_text);

-- =============================================================================
-- 8. CUSTOMER DATA OPTIMIZATION INDEXES
-- =============================================================================

-- Create B-tree index for customer name searches
-- Purpose: Support customer name lookup queries for customer service operations
CREATE INDEX idx_customers_name_search ON customers(last_name, first_name, customer_id)
INCLUDE (middle_name, ssn, date_of_birth, fico_credit_score, primary_cardholder_indicator);

-- Create B-tree index for customer demographic queries
-- Purpose: Support customer segmentation and demographic analysis queries
CREATE INDEX idx_customers_demographics ON customers(address_state, fico_credit_score, date_of_birth)
INCLUDE (customer_id, primary_cardholder_indicator, address_zip);

-- Create B-tree index for customer PII queries (with row-level security)
-- Purpose: Support secure customer PII lookups with proper access controls
CREATE INDEX idx_customers_pii_secure ON customers(customer_id, ssn)
INCLUDE (first_name, last_name, date_of_birth, government_id);

-- =============================================================================
-- 9. TRANSACTION CATEGORY BALANCE OPTIMIZATION
-- =============================================================================

-- Create composite index for category balance queries
-- Purpose: Support transaction category balance reporting and analysis
CREATE INDEX idx_tcb_balance_analysis ON transaction_category_balances(transaction_category, category_balance, account_id)
INCLUDE (last_updated);

-- Create composite index for account-category balance queries
-- Purpose: Support account-specific category balance lookups
CREATE INDEX idx_tcb_account_category ON transaction_category_balances(account_id, transaction_category, category_balance)
INCLUDE (last_updated);

-- =============================================================================
-- 10. FULL-TEXT SEARCH OPTIMIZATION
-- =============================================================================

-- Create GIN index for transaction description full-text search
-- Purpose: Support transaction description search functionality
CREATE INDEX idx_transactions_description_gin ON transactions USING GIN(to_tsvector('english', description));

-- Create GIN index for merchant name full-text search
-- Purpose: Support merchant name search functionality
CREATE INDEX idx_transactions_merchant_gin ON transactions USING GIN(to_tsvector('english', merchant_name));

-- Create GIN index for customer name full-text search
-- Purpose: Support customer name search functionality
CREATE INDEX idx_customers_name_gin ON customers USING GIN(to_tsvector('english', first_name || ' ' || COALESCE(middle_name, '') || ' ' || last_name));

-- =============================================================================
-- 11. SPECIALIZED INDEXES FOR BATCH PROCESSING
-- =============================================================================

-- Create index for batch interest calculation jobs
-- Purpose: Support monthly interest calculation batch processing
CREATE INDEX idx_accounts_interest_calc ON accounts(group_id, current_balance, active_status)
INCLUDE (account_id, customer_id, open_date, current_cycle_debit, current_cycle_credit);

-- Create index for batch statement generation jobs
-- Purpose: Support monthly statement generation batch processing
CREATE INDEX idx_transactions_statement_gen ON transactions(account_id, transaction_timestamp, transaction_type)
INCLUDE (transaction_id, transaction_amount, transaction_category, description, merchant_name);

-- Create index for batch reporting jobs
-- Purpose: Support monthly reporting and analytics batch processing
CREATE INDEX idx_transactions_reporting ON transactions(transaction_timestamp, transaction_category, transaction_amount)
INCLUDE (account_id, transaction_id, transaction_type, merchant_name, merchant_city);

-- =============================================================================
-- 12. PARTIAL INDEXES FOR PERFORMANCE OPTIMIZATION
-- =============================================================================

-- Create partial index for active accounts only
-- Purpose: Optimize queries that only access active accounts
CREATE INDEX idx_accounts_active_partial ON accounts(account_id, current_balance, credit_limit)
WHERE active_status = 'Y';

-- Create partial index for active cards only
-- Purpose: Optimize queries that only access active cards
CREATE INDEX idx_cards_active_partial ON cards(card_number, account_id, customer_id, expiration_date)
WHERE active_status = 'Y';

-- Create partial index for recent transactions only (last 2 years)
-- Purpose: Optimize queries that focus on recent transaction history
CREATE INDEX idx_transactions_recent_partial ON transactions(transaction_timestamp, account_id, transaction_amount)
WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '2 years';

-- Create partial index for high-value transactions only
-- Purpose: Optimize queries for fraud detection and high-value transaction analysis
CREATE INDEX idx_transactions_high_value_partial ON transactions(transaction_amount, transaction_timestamp, account_id)
WHERE ABS(transaction_amount) >= 1000.00;

-- =============================================================================
-- 13. UNIQUE INDEXES FOR DATA INTEGRITY
-- =============================================================================

-- Create unique index for customer SSN integrity
-- Purpose: Ensure SSN uniqueness across customer records
CREATE UNIQUE INDEX idx_customers_ssn_unique ON customers(ssn);

-- Create unique index for customer government ID integrity
-- Purpose: Ensure government ID uniqueness across customer records
CREATE UNIQUE INDEX idx_customers_government_id_unique ON customers(government_id);

-- Create unique index for account-customer relationship integrity
-- Purpose: Ensure one-to-one account-customer relationship integrity
CREATE UNIQUE INDEX idx_accounts_customer_unique ON accounts(customer_id, account_id);

-- =============================================================================
-- 14. EXPRESSION INDEXES FOR CALCULATED FIELDS
-- =============================================================================

-- Create expression index for available credit calculations
-- Purpose: Support queries involving available credit calculations
CREATE INDEX idx_accounts_available_credit ON accounts((credit_limit - current_balance), account_id)
INCLUDE (active_status, customer_id, group_id);

-- Create expression index for account age calculations
-- Purpose: Support queries involving account age analysis
CREATE INDEX idx_accounts_age ON accounts(EXTRACT(YEAR FROM AGE(CURRENT_DATE, open_date)), account_id)
INCLUDE (active_status, customer_id, current_balance);

-- Create expression index for customer age calculations
-- Purpose: Support queries involving customer age demographics
CREATE INDEX idx_customers_age ON customers(EXTRACT(YEAR FROM AGE(CURRENT_DATE, date_of_birth)), customer_id)
INCLUDE (fico_credit_score, primary_cardholder_indicator, address_state);

-- =============================================================================
-- 15. BLOOM FILTER INDEXES FOR LARGE DATA SETS
-- =============================================================================

-- Create BRIN index for transaction timestamp (optimized for time-series data)
-- Purpose: Support efficient range queries on large transaction datasets
CREATE INDEX idx_transactions_timestamp_brin ON transactions USING BRIN(transaction_timestamp);

-- Create BRIN index for account creation dates (optimized for time-series data)
-- Purpose: Support efficient range queries on account creation patterns
CREATE INDEX idx_accounts_created_brin ON accounts USING BRIN(created_at);

-- Create BRIN index for customer creation dates (optimized for time-series data)
-- Purpose: Support efficient range queries on customer registration patterns
CREATE INDEX idx_customers_created_brin ON customers USING BRIN(created_at);

-- =============================================================================
-- 16. MATERIALIZED VIEW INDEXES
-- =============================================================================

-- Create indexes on existing materialized views for performance optimization
-- Note: These materialized views were created in previous migrations

-- Customer summary materialized view indexes
CREATE INDEX idx_mv_customer_summary_customer_id ON mv_customer_summary(customer_id);
CREATE INDEX idx_mv_customer_summary_credit_rating ON mv_customer_summary(credit_rating);
CREATE INDEX idx_mv_customer_summary_age ON mv_customer_summary(age);

-- Account summary materialized view indexes  
CREATE INDEX idx_mv_account_summary_customer_id ON mv_account_summary(customer_id);
CREATE INDEX idx_mv_account_summary_balance_status ON mv_account_summary(balance_status);
CREATE INDEX idx_mv_account_summary_available_credit ON mv_account_summary(available_credit);

-- Card summary materialized view indexes
CREATE INDEX idx_mv_card_summary_customer_id ON mv_card_summary(customer_id);
CREATE INDEX idx_mv_card_summary_card_status ON mv_card_summary(card_status);
CREATE INDEX idx_mv_card_summary_expiration_date ON mv_card_summary(expiration_date);

-- Transaction summary materialized view indexes
CREATE INDEX idx_mv_transaction_summary_account_id ON mv_transaction_summary(account_id);
CREATE INDEX idx_mv_transaction_summary_month_year ON mv_transaction_summary(month_year);
CREATE INDEX idx_mv_transaction_summary_transaction_type ON mv_transaction_summary(transaction_type);

-- Reference data lookup materialized view indexes
CREATE INDEX idx_mv_reference_data_lookup_transaction_type ON mv_reference_data_lookup(transaction_type);
CREATE INDEX idx_mv_reference_data_lookup_transaction_category ON mv_reference_data_lookup(transaction_category);
CREATE INDEX idx_mv_reference_data_lookup_group_id ON mv_reference_data_lookup(group_id);

-- =============================================================================
-- 17. COMMENTS AND DOCUMENTATION
-- =============================================================================

-- Add comments to indexes for maintenance and documentation
COMMENT ON INDEX idx_cards_account_id_active_status IS 'VSAM CARDAIX alternate index replication: Enables rapid card lookups by account ID matching VSAM KEYLEN=11, RKP=5 functionality';
COMMENT ON INDEX idx_transactions_timestamp_account_id IS 'VSAM TRANSACT.AIX alternate index replication: Enables efficient time-range queries matching VSAM KEYLEN=26, RKP=5 functionality';
COMMENT ON INDEX idx_cards_customer_id_active_status IS 'VSAM CARDXREF.AIX alternate index replication: Enables rapid card lookups by customer ID matching VSAM KEYLEN=11, RKP=5 functionality';
COMMENT ON INDEX idx_customer_account_xref IS 'Customer-account cross-reference optimization: Supports rapid customer-account relationship queries matching VSAM XREF functionality';
COMMENT ON INDEX idx_account_balance_covering IS 'Covering index for balance queries: Enables index-only scans for frequent balance inquiries without table access';
COMMENT ON INDEX idx_transactions_date_range IS 'Date-range query optimization: Supports efficient time-range queries with partition pruning for sub-200ms response times';
COMMENT ON INDEX idx_accounts_status_balance IS 'Microservices query optimization: Supports account status and balance range queries for REST API endpoints';
COMMENT ON INDEX idx_transactions_description_gin IS 'Full-text search optimization: Enables transaction description search functionality using GIN indexing';
COMMENT ON INDEX idx_accounts_interest_calc IS 'Batch processing optimization: Supports monthly interest calculation jobs with optimized data access patterns';
COMMENT ON INDEX idx_accounts_active_partial IS 'Partial index optimization: Optimizes queries for active accounts only, reducing index size and improving performance';
COMMENT ON INDEX idx_customers_ssn_unique IS 'Data integrity constraint: Ensures SSN uniqueness across customer records for regulatory compliance';
COMMENT ON INDEX idx_accounts_available_credit IS 'Expression index optimization: Supports queries involving available credit calculations with pre-computed values';
COMMENT ON INDEX idx_transactions_timestamp_brin IS 'BRIN index optimization: Supports efficient range queries on large transaction datasets using block range indexing';

-- =============================================================================
-- 18. INDEX MAINTENANCE FUNCTIONS
-- =============================================================================

-- Create function to analyze index usage and performance
CREATE OR REPLACE FUNCTION analyze_index_performance()
RETURNS TABLE (
    index_name TEXT,
    table_name TEXT,
    index_size TEXT,
    index_scans BIGINT,
    tuples_read BIGINT,
    tuples_fetched BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        i.indexrelname::TEXT as index_name,
        t.relname::TEXT as table_name,
        pg_size_pretty(pg_relation_size(i.indexrelid))::TEXT as index_size,
        s.idx_scan as index_scans,
        s.idx_tup_read as tuples_read,
        s.idx_tup_fetch as tuples_fetched
    FROM pg_stat_user_indexes s
    JOIN pg_class i ON s.indexrelid = i.oid
    JOIN pg_class t ON s.relid = t.oid
    WHERE s.idx_scan > 0
    ORDER BY s.idx_scan DESC;
END;
$$ LANGUAGE plpgsql;

-- Create function to identify unused indexes
CREATE OR REPLACE FUNCTION identify_unused_indexes()
RETURNS TABLE (
    index_name TEXT,
    table_name TEXT,
    index_size TEXT,
    definition TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        i.indexrelname::TEXT as index_name,
        t.relname::TEXT as table_name,
        pg_size_pretty(pg_relation_size(i.indexrelid))::TEXT as index_size,
        pg_get_indexdef(i.indexrelid)::TEXT as definition
    FROM pg_stat_user_indexes s
    JOIN pg_class i ON s.indexrelid = i.oid
    JOIN pg_class t ON s.relid = t.oid
    WHERE s.idx_scan = 0
    AND i.indexrelname NOT LIKE '%_pkey'
    ORDER BY pg_relation_size(i.indexrelid) DESC;
END;
$$ LANGUAGE plpgsql;

-- Create function to reindex all tables for maintenance
CREATE OR REPLACE FUNCTION reindex_all_tables()
RETURNS VOID AS $$
DECLARE
    table_record RECORD;
BEGIN
    FOR table_record IN 
        SELECT tablename FROM pg_tables 
        WHERE schemaname = 'public' 
        AND tablename IN ('customers', 'accounts', 'cards', 'transactions', 'transaction_types', 'transaction_categories', 'disclosure_groups', 'transaction_category_balances')
    LOOP
        EXECUTE 'REINDEX TABLE ' || table_record.tablename;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 19. PERFORMANCE MONITORING SETUP
-- =============================================================================

-- Create function to monitor index performance metrics
CREATE OR REPLACE FUNCTION monitor_index_performance()
RETURNS TABLE (
    metric_name TEXT,
    metric_value NUMERIC,
    metric_unit TEXT,
    measurement_timestamp TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'total_index_size'::TEXT as metric_name,
        pg_total_relation_size('accounts'::regclass)::NUMERIC as metric_value,
        'bytes'::TEXT as metric_unit,
        CURRENT_TIMESTAMP as measurement_timestamp
    UNION ALL
    SELECT 
        'average_query_time'::TEXT as metric_name,
        avg(total_time)::NUMERIC as metric_value,
        'milliseconds'::TEXT as metric_unit,
        CURRENT_TIMESTAMP as measurement_timestamp
    FROM pg_stat_statements 
    WHERE query LIKE '%accounts%' OR query LIKE '%cards%' OR query LIKE '%transactions%';
END;
$$ LANGUAGE plpgsql;

-- Add comprehensive comments for maintenance documentation
COMMENT ON FUNCTION analyze_index_performance() IS 'Analyzes index usage patterns and performance metrics for optimization recommendations';
COMMENT ON FUNCTION identify_unused_indexes() IS 'Identifies unused indexes that may be candidates for removal to improve performance';
COMMENT ON FUNCTION reindex_all_tables() IS 'Performs maintenance reindexing of all core tables for optimal performance';
COMMENT ON FUNCTION monitor_index_performance() IS 'Monitors key index performance metrics for continuous optimization';

-- =============================================================================
-- 20. ROLLBACK CHANGESET INSTRUCTIONS
-- =============================================================================

-- rollback changeset blitzy:V7-create-indexes
-- DROP FUNCTION IF EXISTS monitor_index_performance();
-- DROP FUNCTION IF EXISTS reindex_all_tables();
-- DROP FUNCTION IF EXISTS identify_unused_indexes();
-- DROP FUNCTION IF EXISTS analyze_index_performance();
-- DROP INDEX IF EXISTS idx_mv_reference_data_lookup_group_id;
-- DROP INDEX IF EXISTS idx_mv_reference_data_lookup_transaction_category;
-- DROP INDEX IF EXISTS idx_mv_reference_data_lookup_transaction_type;
-- DROP INDEX IF EXISTS idx_mv_transaction_summary_transaction_type;
-- DROP INDEX IF EXISTS idx_mv_transaction_summary_month_year;
-- DROP INDEX IF EXISTS idx_mv_transaction_summary_account_id;
-- DROP INDEX IF EXISTS idx_mv_card_summary_expiration_date;
-- DROP INDEX IF EXISTS idx_mv_card_summary_card_status;
-- DROP INDEX IF EXISTS idx_mv_card_summary_customer_id;
-- DROP INDEX IF EXISTS idx_mv_account_summary_available_credit;
-- DROP INDEX IF EXISTS idx_mv_account_summary_balance_status;
-- DROP INDEX IF EXISTS idx_mv_account_summary_customer_id;
-- DROP INDEX IF EXISTS idx_mv_customer_summary_age;
-- DROP INDEX IF EXISTS idx_mv_customer_summary_credit_rating;
-- DROP INDEX IF EXISTS idx_mv_customer_summary_customer_id;
-- DROP INDEX IF EXISTS idx_customers_created_brin;
-- DROP INDEX IF EXISTS idx_accounts_created_brin;
-- DROP INDEX IF EXISTS idx_transactions_timestamp_brin;
-- DROP INDEX IF EXISTS idx_customers_age;
-- DROP INDEX IF EXISTS idx_accounts_age;
-- DROP INDEX IF EXISTS idx_accounts_available_credit;
-- DROP INDEX IF EXISTS idx_accounts_customer_unique;
-- DROP INDEX IF EXISTS idx_customers_government_id_unique;
-- DROP INDEX IF EXISTS idx_customers_ssn_unique;
-- DROP INDEX IF EXISTS idx_transactions_high_value_partial;
-- DROP INDEX IF EXISTS idx_transactions_recent_partial;
-- DROP INDEX IF EXISTS idx_cards_active_partial;
-- DROP INDEX IF EXISTS idx_accounts_active_partial;
-- DROP INDEX IF EXISTS idx_transactions_reporting;
-- DROP INDEX IF EXISTS idx_transactions_statement_gen;
-- DROP INDEX IF EXISTS idx_accounts_interest_calc;
-- DROP INDEX IF EXISTS idx_customers_name_gin;
-- DROP INDEX IF EXISTS idx_transactions_merchant_gin;
-- DROP INDEX IF EXISTS idx_transactions_description_gin;
-- DROP INDEX IF EXISTS idx_tcb_account_category;
-- DROP INDEX IF EXISTS idx_tcb_balance_analysis;
-- DROP INDEX IF EXISTS idx_customers_pii_secure;
-- DROP INDEX IF EXISTS idx_customers_demographics;
-- DROP INDEX IF EXISTS idx_customers_name_search;
-- DROP INDEX IF EXISTS idx_disclosure_groups_active_btree;
-- DROP INDEX IF EXISTS idx_transaction_categories_active_btree;
-- DROP INDEX IF EXISTS idx_transaction_types_active_btree;
-- DROP INDEX IF EXISTS idx_transactions_amount_type;
-- DROP INDEX IF EXISTS idx_cards_status_expiration;
-- DROP INDEX IF EXISTS idx_accounts_status_balance;
-- DROP INDEX IF EXISTS idx_cards_date_range;
-- DROP INDEX IF EXISTS idx_accounts_date_range;
-- DROP INDEX IF EXISTS idx_transactions_date_range;
-- DROP INDEX IF EXISTS idx_tcb_transaction_category_btree;
-- DROP INDEX IF EXISTS idx_tcb_account_id_btree;
-- DROP INDEX IF EXISTS idx_transactions_category_btree;
-- DROP INDEX IF EXISTS idx_transactions_type_btree;
-- DROP INDEX IF EXISTS idx_transactions_card_number_btree;
-- DROP INDEX IF EXISTS idx_transactions_account_id_btree;
-- DROP INDEX IF EXISTS idx_cards_customer_id_btree;
-- DROP INDEX IF EXISTS idx_cards_account_id_btree;
-- DROP INDEX IF EXISTS idx_accounts_group_id_btree;
-- DROP INDEX IF EXISTS idx_accounts_customer_id_btree;
-- DROP INDEX IF EXISTS idx_cards_balance_covering;
-- DROP INDEX IF EXISTS idx_account_balance_covering;
-- DROP INDEX IF EXISTS idx_account_customer_xref;
-- DROP INDEX IF EXISTS idx_customer_account_xref;
-- DROP INDEX IF EXISTS idx_cards_customer_id_active_status;
-- DROP INDEX IF EXISTS idx_transactions_timestamp_account_id;
-- DROP INDEX IF EXISTS idx_cards_account_id_active_status;