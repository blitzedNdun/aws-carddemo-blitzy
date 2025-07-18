-- =====================================================================================
-- Liquibase Migration: V8__create_materialized_views.sql
-- Description: Creates materialized views for cross-reference optimization, replacing
--              VSAM alternate index functionality with pre-computed aggregations and
--              automatic refresh scheduling for optimal query performance
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 8.0
-- Dependencies: V2__create_customers_table.sql, V3__create_accounts_table.sql,
--               V4__create_cards_table.sql, V5__create_transactions_table.sql,
--               V6__create_reference_tables.sql, V7__create_indexes.sql
-- =====================================================================================

-- changeset blitzy:V8-create-materialized-views
-- comment: Create materialized views for cross-reference optimization with sub-millisecond access to frequently queried data

-- =============================================================================
-- 1. CREATE CUSTOMER ACCOUNT SUMMARY MATERIALIZED VIEW
-- =============================================================================

-- Create comprehensive customer account summary view replacing VSAM cross-reference lookups
CREATE MATERIALIZED VIEW mv_customer_account_summary AS
SELECT 
    -- Customer identification and demographics
    c.customer_id,
    CONCAT(c.first_name, ' ', COALESCE(c.middle_name || ' ', ''), c.last_name) AS customer_name,
    c.first_name,
    c.last_name,
    c.fico_credit_score,
    CASE 
        WHEN c.fico_credit_score >= 800 THEN 'Excellent'
        WHEN c.fico_credit_score >= 740 THEN 'Very Good'
        WHEN c.fico_credit_score >= 670 THEN 'Good'
        WHEN c.fico_credit_score >= 580 THEN 'Fair'
        ELSE 'Poor'
    END AS credit_rating,
    c.date_of_birth,
    EXTRACT(YEAR FROM AGE(c.date_of_birth)) AS age,
    c.primary_cardholder_indicator,
    
    -- Account portfolio aggregation metrics
    COUNT(a.account_id) AS total_accounts,
    COUNT(CASE WHEN a.active_status = 'Y' THEN 1 END) AS active_accounts,
    COUNT(CASE WHEN a.active_status = 'N' THEN 1 END) AS inactive_accounts,
    
    -- Financial portfolio metrics with DECIMAL precision
    COALESCE(SUM(CASE WHEN a.active_status = 'Y' THEN a.current_balance ELSE 0 END), 0.00) AS total_balance,
    COALESCE(SUM(CASE WHEN a.active_status = 'Y' THEN a.credit_limit ELSE 0 END), 0.00) AS total_credit_limit,
    COALESCE(SUM(CASE WHEN a.active_status = 'Y' THEN a.cash_credit_limit ELSE 0 END), 0.00) AS total_cash_credit_limit,
    COALESCE(SUM(CASE WHEN a.active_status = 'Y' THEN (a.credit_limit - a.current_balance) ELSE 0 END), 0.00) AS total_available_credit,
    
    -- Average balance and credit metrics
    COALESCE(AVG(CASE WHEN a.active_status = 'Y' THEN a.current_balance END), 0.00) AS average_balance,
    COALESCE(AVG(CASE WHEN a.active_status = 'Y' THEN a.credit_limit END), 0.00) AS average_credit_limit,
    
    -- Account status categorization
    JSON_BUILD_OBJECT(
        'Normal', COUNT(CASE WHEN a.active_status = 'Y' AND a.current_balance <= a.credit_limit * 0.8 THEN 1 END),
        'Near_Limit', COUNT(CASE WHEN a.active_status = 'Y' AND a.current_balance > a.credit_limit * 0.8 AND a.current_balance <= a.credit_limit THEN 1 END),
        'Over_Limit', COUNT(CASE WHEN a.active_status = 'Y' AND a.current_balance > a.credit_limit THEN 1 END),
        'Credit_Balance', COUNT(CASE WHEN a.active_status = 'Y' AND a.current_balance < 0 THEN 1 END)
    ) AS account_count_by_status,
    
    -- Cycle activity aggregation
    COALESCE(SUM(CASE WHEN a.active_status = 'Y' THEN a.current_cycle_credit ELSE 0 END), 0.00) AS total_cycle_credit,
    COALESCE(SUM(CASE WHEN a.active_status = 'Y' THEN a.current_cycle_debit ELSE 0 END), 0.00) AS total_cycle_debit,
    
    -- Account lifecycle metrics
    MIN(a.open_date) AS first_account_open_date,
    MAX(a.open_date) AS latest_account_open_date,
    MIN(a.expiration_date) AS earliest_expiration_date,
    MAX(a.expiration_date) AS latest_expiration_date,
    
    -- Refresh tracking
    CURRENT_TIMESTAMP AS last_updated
FROM customers c
LEFT JOIN accounts a ON c.customer_id = a.customer_id
GROUP BY 
    c.customer_id, 
    c.first_name, 
    c.middle_name, 
    c.last_name, 
    c.fico_credit_score, 
    c.date_of_birth, 
    c.primary_cardholder_indicator;

-- Create composite B-tree indexes for optimal query performance
CREATE UNIQUE INDEX idx_mv_customer_account_summary_pk ON mv_customer_account_summary(customer_id);
CREATE INDEX idx_mv_customer_account_summary_name ON mv_customer_account_summary(customer_name);
CREATE INDEX idx_mv_customer_account_summary_credit_rating ON mv_customer_account_summary(credit_rating);
CREATE INDEX idx_mv_customer_account_summary_total_balance ON mv_customer_account_summary(total_balance);
CREATE INDEX idx_mv_customer_account_summary_total_credit_limit ON mv_customer_account_summary(total_credit_limit);
CREATE INDEX idx_mv_customer_account_summary_fico_score ON mv_customer_account_summary(fico_credit_score);
CREATE INDEX idx_mv_customer_account_summary_age ON mv_customer_account_summary(age);
CREATE INDEX idx_mv_customer_account_summary_active_accounts ON mv_customer_account_summary(active_accounts);

-- =============================================================================
-- 2. CREATE CARD TRANSACTION SUMMARY MATERIALIZED VIEW
-- =============================================================================

-- Create comprehensive card transaction analytics view for usage patterns and reporting
CREATE MATERIALIZED VIEW mv_card_transaction_summary AS
SELECT 
    -- Card identification and relationships
    c.card_number,
    c.account_id,
    c.customer_id,
    c.embossed_name,
    c.active_status AS card_status,
    c.expiration_date,
    
    -- Account and customer context
    a.current_balance AS account_balance,
    a.credit_limit AS account_credit_limit,
    a.active_status AS account_status,
    cust.customer_name,
    cust.fico_credit_score,
    
    -- Transaction volume metrics
    COUNT(t.transaction_id) AS total_transactions,
    COUNT(CASE WHEN t.transaction_amount > 0 THEN 1 END) AS total_debit_transactions,
    COUNT(CASE WHEN t.transaction_amount < 0 THEN 1 END) AS total_credit_transactions,
    
    -- Transaction amount aggregations with DECIMAL precision
    COALESCE(SUM(t.transaction_amount), 0.00) AS total_amount,
    COALESCE(SUM(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount ELSE 0 END), 0.00) AS total_debit_amount,
    COALESCE(SUM(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) ELSE 0 END), 0.00) AS total_credit_amount,
    
    -- Average transaction metrics
    COALESCE(AVG(ABS(t.transaction_amount)), 0.00) AS average_transaction_amount,
    COALESCE(AVG(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount END), 0.00) AS average_debit_amount,
    COALESCE(AVG(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) END), 0.00) AS average_credit_amount,
    
    -- Monthly transaction volume analysis
    JSON_BUILD_OBJECT(
        'current_month', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE) THEN 1 END),
        'previous_month', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') 
                                AND t.transaction_timestamp < DATE_TRUNC('month', CURRENT_DATE) THEN 1 END),
        'last_3_months', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '3 months') THEN 1 END),
        'last_6_months', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '6 months') THEN 1 END),
        'last_12_months', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '12 months') THEN 1 END)
    ) AS monthly_volume,
    
    -- Transaction type distribution
    JSON_BUILD_OBJECT(
        'type_01', COUNT(CASE WHEN t.transaction_type = '01' THEN 1 END),
        'type_02', COUNT(CASE WHEN t.transaction_type = '02' THEN 1 END),
        'type_03', COUNT(CASE WHEN t.transaction_type = '03' THEN 1 END),
        'type_04', COUNT(CASE WHEN t.transaction_type = '04' THEN 1 END),
        'type_05', COUNT(CASE WHEN t.transaction_type = '05' THEN 1 END),
        'type_06', COUNT(CASE WHEN t.transaction_type = '06' THEN 1 END),
        'type_07', COUNT(CASE WHEN t.transaction_type = '07' THEN 1 END)
    ) AS transaction_count_by_type,
    
    -- Transaction activity patterns
    MIN(t.transaction_timestamp) AS first_transaction_date,
    MAX(t.transaction_timestamp) AS last_transaction_date,
    COUNT(DISTINCT DATE(t.transaction_timestamp)) AS active_days,
    COUNT(DISTINCT t.merchant_name) AS unique_merchants,
    
    -- Spending patterns analysis
    MODE() WITHIN GROUP (ORDER BY t.transaction_category) AS most_common_category,
    MODE() WITHIN GROUP (ORDER BY t.merchant_name) AS most_frequent_merchant,
    
    -- Card usage analytics
    CASE 
        WHEN COUNT(t.transaction_id) = 0 THEN 'Inactive'
        WHEN MAX(t.transaction_timestamp) < CURRENT_DATE - INTERVAL '90 days' THEN 'Dormant'
        WHEN COUNT(t.transaction_id) > 50 AND MAX(t.transaction_timestamp) >= CURRENT_DATE - INTERVAL '30 days' THEN 'High_Usage'
        WHEN COUNT(t.transaction_id) > 20 AND MAX(t.transaction_timestamp) >= CURRENT_DATE - INTERVAL '30 days' THEN 'Medium_Usage'
        ELSE 'Low_Usage'
    END AS usage_pattern,
    
    -- Refresh tracking
    CURRENT_TIMESTAMP AS last_updated
FROM cards c
LEFT JOIN accounts a ON c.account_id = a.account_id
LEFT JOIN mv_customer_account_summary cust ON c.customer_id = cust.customer_id
LEFT JOIN transactions t ON c.card_number = t.card_number
GROUP BY 
    c.card_number, 
    c.account_id, 
    c.customer_id, 
    c.embossed_name, 
    c.active_status, 
    c.expiration_date,
    a.current_balance, 
    a.credit_limit, 
    a.active_status, 
    cust.customer_name, 
    cust.fico_credit_score;

-- Create composite B-tree indexes for optimal query performance
CREATE UNIQUE INDEX idx_mv_card_transaction_summary_pk ON mv_card_transaction_summary(card_number);
CREATE INDEX idx_mv_card_transaction_summary_account_id ON mv_card_transaction_summary(account_id);
CREATE INDEX idx_mv_card_transaction_summary_customer_id ON mv_card_transaction_summary(customer_id);
CREATE INDEX idx_mv_card_transaction_summary_usage_pattern ON mv_card_transaction_summary(usage_pattern);
CREATE INDEX idx_mv_card_transaction_summary_total_amount ON mv_card_transaction_summary(total_amount);
CREATE INDEX idx_mv_card_transaction_summary_total_transactions ON mv_card_transaction_summary(total_transactions);
CREATE INDEX idx_mv_card_transaction_summary_card_status ON mv_card_transaction_summary(card_status);
CREATE INDEX idx_mv_card_transaction_summary_last_transaction ON mv_card_transaction_summary(last_transaction_date);

-- =============================================================================
-- 3. CREATE ACCOUNT BALANCE HISTORY MATERIALIZED VIEW
-- =============================================================================

-- Create historical balance snapshots for trend analysis and reporting
CREATE MATERIALIZED VIEW mv_account_balance_history AS
SELECT 
    -- Account identification and relationships
    a.account_id,
    a.customer_id,
    cust.customer_name,
    cust.fico_credit_score,
    
    -- Current balance and credit information
    a.current_balance,
    a.credit_limit,
    a.cash_credit_limit,
    (a.credit_limit - a.current_balance) AS available_credit,
    
    -- Balance status categorization
    CASE 
        WHEN a.current_balance > a.credit_limit THEN 'Over_Limit'
        WHEN a.current_balance > (a.credit_limit * 0.9) THEN 'Near_Limit'
        WHEN a.current_balance < 0 THEN 'Credit_Balance'
        ELSE 'Normal'
    END AS balance_status,
    
    -- Cycle-to-date balance information
    a.current_cycle_credit,
    a.current_cycle_debit,
    (a.current_cycle_debit - a.current_cycle_credit) AS cycle_to_date_balance,
    
    -- Historical balance trend indicators
    CASE 
        WHEN a.current_balance > a.current_cycle_debit THEN 'Increasing'
        WHEN a.current_balance < a.current_cycle_debit THEN 'Decreasing'
        ELSE 'Stable'
    END AS balance_trend,
    
    -- Monthly balance snapshots (last 12 months)
    JSON_BUILD_OBJECT(
        'current_month', a.current_balance,
        'utilization_rate', CASE WHEN a.credit_limit > 0 THEN ROUND((a.current_balance / a.credit_limit * 100), 2) ELSE 0 END,
        'payment_behavior', CASE 
            WHEN a.current_cycle_credit > a.current_cycle_debit THEN 'Overpayment'
            WHEN a.current_cycle_credit = a.current_cycle_debit THEN 'Full_Payment'
            WHEN a.current_cycle_credit > (a.current_cycle_debit * 0.5) THEN 'Partial_Payment'
            WHEN a.current_cycle_credit > 0 THEN 'Minimum_Payment'
            ELSE 'No_Payment'
        END
    ) AS balance_trend_indicators,
    
    -- Account lifecycle information
    a.open_date,
    a.expiration_date,
    a.reissue_date,
    a.active_status,
    EXTRACT(YEAR FROM AGE(a.expiration_date, CURRENT_DATE)) AS years_to_expiry,
    EXTRACT(YEAR FROM AGE(CURRENT_DATE, a.open_date)) AS account_age_years,
    
    -- Transaction activity summary
    COALESCE(ts.total_transactions, 0) AS total_transactions,
    COALESCE(ts.total_debit_amount, 0.00) AS total_debit_amount,
    COALESCE(ts.total_credit_amount, 0.00) AS total_credit_amount,
    COALESCE(ts.average_transaction_amount, 0.00) AS average_transaction_amount,
    
    -- Risk assessment indicators
    CASE 
        WHEN a.current_balance > a.credit_limit * 1.1 THEN 'High_Risk'
        WHEN a.current_balance > a.credit_limit THEN 'Medium_Risk'
        WHEN a.current_balance > a.credit_limit * 0.9 THEN 'Low_Risk'
        ELSE 'Normal_Risk'
    END AS risk_level,
    
    -- Account group and disclosure information
    a.group_id,
    
    -- Refresh tracking
    DATE_TRUNC('day', CURRENT_TIMESTAMP) AS snapshot_date,
    CURRENT_TIMESTAMP AS last_updated
FROM accounts a
LEFT JOIN mv_customer_account_summary cust ON a.customer_id = cust.customer_id
LEFT JOIN (
    SELECT 
        t.account_id,
        COUNT(t.transaction_id) AS total_transactions,
        SUM(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount ELSE 0 END) AS total_debit_amount,
        SUM(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) ELSE 0 END) AS total_credit_amount,
        AVG(ABS(t.transaction_amount)) AS average_transaction_amount
    FROM transactions t
    WHERE t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '3 months')
    GROUP BY t.account_id
) ts ON a.account_id = ts.account_id
WHERE a.active_status = 'Y';

-- Create composite B-tree indexes for optimal query performance
CREATE UNIQUE INDEX idx_mv_account_balance_history_pk ON mv_account_balance_history(account_id);
CREATE INDEX idx_mv_account_balance_history_customer_id ON mv_account_balance_history(customer_id);
CREATE INDEX idx_mv_account_balance_history_balance_status ON mv_account_balance_history(balance_status);
CREATE INDEX idx_mv_account_balance_history_current_balance ON mv_account_balance_history(current_balance);
CREATE INDEX idx_mv_account_balance_history_available_credit ON mv_account_balance_history(available_credit);
CREATE INDEX idx_mv_account_balance_history_balance_trend ON mv_account_balance_history(balance_trend);
CREATE INDEX idx_mv_account_balance_history_risk_level ON mv_account_balance_history(risk_level);
CREATE INDEX idx_mv_account_balance_history_snapshot_date ON mv_account_balance_history(snapshot_date);
CREATE INDEX idx_mv_account_balance_history_account_age ON mv_account_balance_history(account_age_years);
CREATE INDEX idx_mv_account_balance_history_years_to_expiry ON mv_account_balance_history(years_to_expiry);

-- =============================================================================
-- 4. MATERIALIZED VIEW REFRESH STRATEGY AND SCHEDULING
-- =============================================================================

-- Create comprehensive refresh function for all materialized views
CREATE OR REPLACE FUNCTION refresh_all_materialized_views()
RETURNS VOID AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    refresh_duration INTERVAL;
BEGIN
    start_time := CURRENT_TIMESTAMP;
    
    -- Refresh views in dependency order
    RAISE NOTICE 'Starting materialized view refresh at %', start_time;
    
    -- Refresh customer account summary first (no dependencies)
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_account_summary;
    RAISE NOTICE 'Refreshed mv_customer_account_summary at %', CURRENT_TIMESTAMP;
    
    -- Refresh card transaction summary (depends on customer summary)
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_card_transaction_summary;
    RAISE NOTICE 'Refreshed mv_card_transaction_summary at %', CURRENT_TIMESTAMP;
    
    -- Refresh account balance history (depends on customer summary)
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_balance_history;
    RAISE NOTICE 'Refreshed mv_account_balance_history at %', CURRENT_TIMESTAMP;
    
    end_time := CURRENT_TIMESTAMP;
    refresh_duration := end_time - start_time;
    
    RAISE NOTICE 'Completed materialized view refresh in % at %', refresh_duration, end_time;
END;
$$ LANGUAGE plpgsql;

-- Create individual refresh functions for each materialized view
CREATE OR REPLACE FUNCTION refresh_customer_account_summary()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_account_summary;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh_card_transaction_summary()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_card_transaction_summary;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh_account_balance_history()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_balance_history;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 5. AUTOMATIC REFRESH SCHEDULING WITH PG_CRON
-- =============================================================================

-- Schedule automatic refresh of materialized views to align with 4-hour batch processing windows
-- Note: Requires pg_cron extension to be installed and enabled

-- Schedule full refresh during low-traffic periods (every 4 hours)
-- This aligns with the 4-hour batch processing window requirement
SELECT cron.schedule(
    'refresh-materialized-views', 
    '0 */4 * * *', -- Every 4 hours at the top of the hour
    'SELECT refresh_all_materialized_views();'
);

-- Schedule more frequent refresh for high-priority customer account summary (every hour)
SELECT cron.schedule(
    'refresh-customer-summary', 
    '0 * * * *', -- Every hour at the top of the hour
    'SELECT refresh_customer_account_summary();'
);

-- Schedule card transaction summary refresh every 2 hours for real-time analytics
SELECT cron.schedule(
    'refresh-card-transaction-summary', 
    '0 */2 * * *', -- Every 2 hours at the top of the hour
    'SELECT refresh_card_transaction_summary();'
);

-- Schedule account balance history refresh every 6 hours for trend analysis
SELECT cron.schedule(
    'refresh-account-balance-history', 
    '0 */6 * * *', -- Every 6 hours at the top of the hour
    'SELECT refresh_account_balance_history();'
);

-- =============================================================================
-- 6. PERFORMANCE MONITORING AND OPTIMIZATION
-- =============================================================================

-- Create view to monitor materialized view refresh performance
CREATE VIEW v_materialized_view_stats AS
SELECT 
    schemaname,
    matviewname,
    matviewowner,
    tablespace,
    hasindexes,
    ispopulated,
    definition
FROM pg_matviews
WHERE schemaname = 'public'
AND matviewname LIKE 'mv_%';

-- Create function to analyze materialized view performance
CREATE OR REPLACE FUNCTION analyze_materialized_view_performance()
RETURNS TABLE (
    view_name TEXT,
    row_count BIGINT,
    size_mb NUMERIC,
    index_count INTEGER,
    last_refresh TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.table_name::TEXT,
        t.n_tup_ins AS row_count,
        ROUND(pg_total_relation_size(t.schemaname||'.'||t.tablename)::NUMERIC / 1024 / 1024, 2) AS size_mb,
        (SELECT COUNT(*) FROM pg_indexes WHERE tablename = t.tablename)::INTEGER AS index_count,
        CURRENT_TIMESTAMP AS last_refresh
    FROM pg_stat_user_tables t
    WHERE t.schemaname = 'public' 
    AND t.tablename LIKE 'mv_%'
    ORDER BY t.tablename;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 7. DOCUMENTATION AND COMMENTS
-- =============================================================================

-- Add comprehensive comments to all materialized views
COMMENT ON MATERIALIZED VIEW mv_customer_account_summary IS 'Comprehensive customer account portfolio summary with aggregated metrics, credit ratings, and financial indicators optimized for sub-millisecond cross-reference queries';

COMMENT ON MATERIALIZED VIEW mv_card_transaction_summary IS 'Card-centric transaction analytics with usage patterns, spending analysis, and monthly volume trends supporting real-time fraud detection and customer insights';

COMMENT ON MATERIALIZED VIEW mv_account_balance_history IS 'Historical account balance snapshots with trend analysis, risk assessment, and cycle-to-date metrics for predictive analytics and portfolio management';

-- Add column-level comments for key fields
COMMENT ON COLUMN mv_customer_account_summary.customer_id IS 'Primary customer identifier for cross-reference operations';
COMMENT ON COLUMN mv_customer_account_summary.total_balance IS 'Aggregated balance across all active accounts with DECIMAL precision';
COMMENT ON COLUMN mv_customer_account_summary.account_count_by_status IS 'JSON object containing account counts by balance status for rapid classification';

COMMENT ON COLUMN mv_card_transaction_summary.card_number IS 'Primary card identifier for transaction analytics';
COMMENT ON COLUMN mv_card_transaction_summary.monthly_volume IS 'JSON object containing transaction counts by month for trend analysis';
COMMENT ON COLUMN mv_card_transaction_summary.usage_pattern IS 'Calculated usage pattern classification for customer segmentation';

COMMENT ON COLUMN mv_account_balance_history.account_id IS 'Primary account identifier for balance history tracking';
COMMENT ON COLUMN mv_account_balance_history.balance_trend_indicators IS 'JSON object containing balance trend metrics and payment behavior analysis';
COMMENT ON COLUMN mv_account_balance_history.risk_level IS 'Calculated risk assessment based on balance utilization and payment patterns';

-- rollback changeset blitzy:V8-create-materialized-views
-- DROP VIEW IF EXISTS v_materialized_view_stats;
-- DROP FUNCTION IF EXISTS analyze_materialized_view_performance();
-- DROP FUNCTION IF EXISTS refresh_account_balance_history();
-- DROP FUNCTION IF EXISTS refresh_card_transaction_summary();
-- DROP FUNCTION IF EXISTS refresh_customer_account_summary();
-- DROP FUNCTION IF EXISTS refresh_all_materialized_views();
-- DROP MATERIALIZED VIEW IF EXISTS mv_account_balance_history CASCADE;
-- DROP MATERIALIZED VIEW IF EXISTS mv_card_transaction_summary CASCADE;
-- DROP MATERIALIZED VIEW IF EXISTS mv_customer_account_summary CASCADE;