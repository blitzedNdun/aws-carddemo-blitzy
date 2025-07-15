-- ============================================================================
-- Liquibase Migration: V8__create_materialized_views.sql
-- Description: Create materialized views for cross-reference optimization, 
--              replacing VSAM alternate index functionality with pre-computed 
--              aggregations and automatic refresh scheduling
-- Author: Blitzy agent
-- Version: 8.0
-- Dependencies: V2__create_customers_table.sql, V3__create_accounts_table.sql,
--               V4__create_cards_table.sql, V5__create_transactions_table.sql,
--               V6__create_reference_tables.sql, V7__create_indexes.sql
-- ============================================================================

-- ============================================================================
-- MATERIALIZED VIEW STRATEGY
-- This migration creates three materialized views to replace VSAM alternate index
-- functionality with pre-computed aggregations providing sub-millisecond access
-- to frequently queried cross-reference data. Views are optimized for the
-- 4-hour batch processing window with automatic refresh scheduling.
-- ============================================================================

-- ============================================================================
-- MATERIALIZED VIEW 1: mv_customer_account_summary
-- Purpose: Aggregated customer portfolio queries replacing CXACAIX functionality
-- Provides: Customer demographics, account counts, financial summaries
-- ============================================================================

CREATE MATERIALIZED VIEW mv_customer_account_summary AS
SELECT 
    -- Customer identification and demographics
    c.customer_id,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    c.first_name,
    c.last_name,
    c.fico_credit_score,
    c.date_of_birth,
    c.address_state,
    c.address_zip,
    
    -- Account portfolio aggregations
    COUNT(a.account_id) AS total_accounts,
    COUNT(CASE WHEN a.active_status = TRUE THEN 1 END) AS active_accounts,
    COUNT(CASE WHEN a.active_status = FALSE THEN 1 END) AS inactive_accounts,
    
    -- Financial aggregations with DECIMAL precision
    COALESCE(SUM(a.current_balance), 0.00) AS total_balance,
    COALESCE(SUM(a.credit_limit), 0.00) AS total_credit_limit,
    COALESCE(SUM(a.cash_credit_limit), 0.00) AS total_cash_credit_limit,
    COALESCE(SUM(a.current_cycle_credit), 0.00) AS total_cycle_credit,
    COALESCE(SUM(a.current_cycle_debit), 0.00) AS total_cycle_debit,
    
    -- Calculated metrics for analytics
    CASE 
        WHEN COUNT(a.account_id) > 0 THEN 
            ROUND(SUM(a.current_balance) / COUNT(a.account_id), 2)
        ELSE 0.00 
    END AS average_balance,
    
    CASE 
        WHEN SUM(a.credit_limit) > 0 THEN 
            ROUND((SUM(a.current_balance) / SUM(a.credit_limit)) * 100, 2)
        ELSE 0.00 
    END AS credit_utilization_percentage,
    
    -- Account status breakdown for portfolio analysis
    JSONB_BUILD_OBJECT(
        'active_accounts', COUNT(CASE WHEN a.active_status = TRUE THEN 1 END),
        'inactive_accounts', COUNT(CASE WHEN a.active_status = FALSE THEN 1 END),
        'total_accounts', COUNT(a.account_id)
    ) AS account_count_by_status,
    
    -- Portfolio metrics for dashboard and reporting
    JSONB_BUILD_OBJECT(
        'total_available_credit', COALESCE(SUM(a.credit_limit - a.current_balance), 0.00),
        'total_cash_available', COALESCE(SUM(a.cash_credit_limit), 0.00),
        'cycle_net_amount', COALESCE(SUM(a.current_cycle_credit - a.current_cycle_debit), 0.00),
        'oldest_account_date', MIN(a.open_date),
        'newest_account_date', MAX(a.open_date)
    ) AS aggregated_portfolio_metrics,
    
    -- Refresh tracking fields
    CURRENT_TIMESTAMP AS last_refresh_time,
    CURRENT_TIMESTAMP AS created_at
FROM customers c
LEFT JOIN accounts a ON c.customer_id = a.customer_id
GROUP BY 
    c.customer_id,
    c.first_name,
    c.last_name,
    c.fico_credit_score,
    c.date_of_birth,
    c.address_state,
    c.address_zip;

-- ============================================================================
-- MATERIALIZED VIEW 2: mv_card_transaction_summary
-- Purpose: Card usage analytics and reporting for transaction patterns
-- Provides: Transaction counts, amounts, merchant analysis, spending patterns
-- ============================================================================

CREATE MATERIALIZED VIEW mv_card_transaction_summary AS
SELECT 
    -- Card identification and account relationship
    cd.card_number,
    cd.account_id,
    cd.customer_id,
    cd.embossed_name,
    cd.active_status AS card_active,
    cd.expiration_date,
    
    -- Transaction volume metrics
    COUNT(t.transaction_id) AS total_transactions,
    COUNT(CASE WHEN t.transaction_amount > 0 THEN 1 END) AS debit_transactions,
    COUNT(CASE WHEN t.transaction_amount < 0 THEN 1 END) AS credit_transactions,
    
    -- Transaction amount aggregations with DECIMAL precision
    COALESCE(SUM(t.transaction_amount), 0.00) AS total_amount,
    COALESCE(SUM(CASE WHEN t.transaction_amount > 0 THEN t.transaction_amount END), 0.00) AS total_debit_amount,
    COALESCE(SUM(CASE WHEN t.transaction_amount < 0 THEN ABS(t.transaction_amount) END), 0.00) AS total_credit_amount,
    
    -- Monthly volume analysis for trend identification
    JSONB_BUILD_OBJECT(
        'current_month_count', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE) THEN 1 END),
        'current_month_amount', COALESCE(SUM(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE) THEN t.transaction_amount END), 0.00),
        'previous_month_count', COUNT(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month' 
                                           AND t.transaction_timestamp < DATE_TRUNC('month', CURRENT_DATE) THEN 1 END),
        'previous_month_amount', COALESCE(SUM(CASE WHEN t.transaction_timestamp >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month' 
                                                  AND t.transaction_timestamp < DATE_TRUNC('month', CURRENT_DATE) THEN t.transaction_amount END), 0.00)
    ) AS monthly_volume,
    
    -- Transaction type breakdown for pattern analysis
    JSONB_BUILD_OBJECT(
        'type_01_count', COUNT(CASE WHEN t.transaction_type = '01' THEN 1 END),
        'type_02_count', COUNT(CASE WHEN t.transaction_type = '02' THEN 1 END),
        'type_03_count', COUNT(CASE WHEN t.transaction_type = '03' THEN 1 END),
        'type_04_count', COUNT(CASE WHEN t.transaction_type = '04' THEN 1 END),
        'type_05_count', COUNT(CASE WHEN t.transaction_type = '05' THEN 1 END),
        'type_06_count', COUNT(CASE WHEN t.transaction_type = '06' THEN 1 END),
        'type_07_count', COUNT(CASE WHEN t.transaction_type = '07' THEN 1 END)
    ) AS transaction_count_by_type,
    
    -- Calculated metrics for analytics
    CASE 
        WHEN COUNT(t.transaction_id) > 0 THEN 
            ROUND(SUM(t.transaction_amount) / COUNT(t.transaction_id), 2)
        ELSE 0.00 
    END AS average_transaction_amount,
    
    -- High-value transaction analysis
    COUNT(CASE WHEN ABS(t.transaction_amount) > 1000.00 THEN 1 END) AS high_value_transactions,
    COALESCE(MAX(t.transaction_amount), 0.00) AS largest_transaction_amount,
    COALESCE(MIN(t.transaction_amount), 0.00) AS smallest_transaction_amount,
    
    -- Merchant and location analysis
    COUNT(DISTINCT t.merchant_name) AS unique_merchants,
    COUNT(DISTINCT t.merchant_city) AS unique_merchant_cities,
    COUNT(DISTINCT t.merchant_zip) AS unique_merchant_zips,
    
    -- Card usage analytics for behavior patterns
    JSONB_BUILD_OBJECT(
        'avg_daily_transactions', ROUND(COUNT(t.transaction_id)::DECIMAL / GREATEST(EXTRACT(DAYS FROM (MAX(t.transaction_timestamp) - MIN(t.transaction_timestamp))), 1), 2),
        'most_frequent_merchant', MODE() WITHIN GROUP (ORDER BY t.merchant_name),
        'most_frequent_category', MODE() WITHIN GROUP (ORDER BY t.transaction_category),
        'first_transaction_date', MIN(t.transaction_timestamp),
        'last_transaction_date', MAX(t.transaction_timestamp)
    ) AS card_usage_analytics,
    
    -- Spending pattern analysis
    JSONB_BUILD_OBJECT(
        'weekend_transactions', COUNT(CASE WHEN EXTRACT(DOW FROM t.transaction_timestamp) IN (0, 6) THEN 1 END),
        'weekday_transactions', COUNT(CASE WHEN EXTRACT(DOW FROM t.transaction_timestamp) IN (1, 2, 3, 4, 5) THEN 1 END),
        'morning_transactions', COUNT(CASE WHEN EXTRACT(HOUR FROM t.transaction_timestamp) BETWEEN 6 AND 11 THEN 1 END),
        'afternoon_transactions', COUNT(CASE WHEN EXTRACT(HOUR FROM t.transaction_timestamp) BETWEEN 12 AND 17 THEN 1 END),
        'evening_transactions', COUNT(CASE WHEN EXTRACT(HOUR FROM t.transaction_timestamp) BETWEEN 18 AND 23 THEN 1 END),
        'night_transactions', COUNT(CASE WHEN EXTRACT(HOUR FROM t.transaction_timestamp) BETWEEN 0 AND 5 THEN 1 END)
    ) AS spending_patterns,
    
    -- Refresh tracking fields
    CURRENT_TIMESTAMP AS last_refresh_time,
    CURRENT_TIMESTAMP AS created_at
FROM cards cd
LEFT JOIN transactions t ON cd.card_number = t.card_number
GROUP BY 
    cd.card_number,
    cd.account_id,
    cd.customer_id,
    cd.embossed_name,
    cd.active_status,
    cd.expiration_date;

-- ============================================================================
-- MATERIALIZED VIEW 3: mv_account_balance_history
-- Purpose: Historical balance snapshots and trend analysis
-- Provides: Balance tracking, trend indicators, cycle analysis
-- ============================================================================

CREATE MATERIALIZED VIEW mv_account_balance_history AS
SELECT 
    -- Account identification and relationship
    a.account_id,
    a.customer_id,
    a.active_status AS account_active,
    a.open_date,
    a.expiration_date,
    a.group_id,
    
    -- Current balance snapshot with DECIMAL precision
    a.current_balance,
    a.credit_limit,
    a.cash_credit_limit,
    (a.credit_limit - a.current_balance) AS available_credit,
    
    -- Current cycle information
    a.current_cycle_credit,
    a.current_cycle_debit,
    (a.current_cycle_credit - a.current_cycle_debit) AS cycle_net_amount,
    
    -- Calculated utilization metrics
    CASE 
        WHEN a.credit_limit > 0 THEN 
            ROUND((a.current_balance / a.credit_limit) * 100, 2)
        ELSE 0.00 
    END AS credit_utilization_percentage,
    
    CASE 
        WHEN a.cash_credit_limit > 0 THEN 
            ROUND((a.current_balance / a.cash_credit_limit) * 100, 2)
        ELSE 0.00 
    END AS cash_utilization_percentage,
    
    -- Balance trend analysis based on transaction history
    COALESCE(
        (SELECT SUM(t.transaction_amount) 
         FROM transactions t 
         WHERE t.account_id = a.account_id 
         AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days'), 
        0.00
    ) AS balance_change_30_days,
    
    COALESCE(
        (SELECT SUM(t.transaction_amount) 
         FROM transactions t 
         WHERE t.account_id = a.account_id 
         AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '90 days'), 
        0.00
    ) AS balance_change_90_days,
    
    -- Transaction volume for balance context
    COALESCE(
        (SELECT COUNT(*) 
         FROM transactions t 
         WHERE t.account_id = a.account_id 
         AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days'), 
        0
    ) AS transaction_count_30_days,
    
    -- Historical balance snapshots for trend analysis
    JSONB_BUILD_OBJECT(
        'current_snapshot', JSONB_BUILD_OBJECT(
            'balance', a.current_balance,
            'available_credit', (a.credit_limit - a.current_balance),
            'utilization_pct', CASE WHEN a.credit_limit > 0 THEN ROUND((a.current_balance / a.credit_limit) * 100, 2) ELSE 0.00 END,
            'snapshot_date', CURRENT_DATE
        ),
        'cycle_snapshot', JSONB_BUILD_OBJECT(
            'cycle_credit', a.current_cycle_credit,
            'cycle_debit', a.current_cycle_debit,
            'cycle_net', (a.current_cycle_credit - a.current_cycle_debit),
            'snapshot_date', CURRENT_DATE
        )
    ) AS historical_balance_snapshots,
    
    -- Balance trend indicators for analytics
    JSONB_BUILD_OBJECT(
        'trend_30_days', CASE 
            WHEN (SELECT SUM(t.transaction_amount) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') > 0 THEN 'increasing'
            WHEN (SELECT SUM(t.transaction_amount) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') < 0 THEN 'decreasing'
            ELSE 'stable'
        END,
        'trend_90_days', CASE 
            WHEN (SELECT SUM(t.transaction_amount) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '90 days') > 0 THEN 'increasing'
            WHEN (SELECT SUM(t.transaction_amount) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '90 days') < 0 THEN 'decreasing'
            ELSE 'stable'
        END,
        'volatility_indicator', CASE 
            WHEN (SELECT COUNT(*) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') > 50 THEN 'high'
            WHEN (SELECT COUNT(*) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') > 20 THEN 'medium'
            ELSE 'low'
        END
    ) AS balance_trend_indicators,
    
    -- Account lifecycle metrics for analysis
    EXTRACT(DAYS FROM (CURRENT_DATE - a.open_date)) AS account_age_days,
    CASE 
        WHEN a.expiration_date IS NOT NULL THEN 
            EXTRACT(DAYS FROM (a.expiration_date - CURRENT_DATE))
        ELSE NULL 
    END AS days_until_expiration,
    
    -- Cycle-to-date balance for reporting
    CASE 
        WHEN a.current_cycle_credit + a.current_cycle_debit > 0 THEN 
            ROUND((a.current_cycle_credit / (a.current_cycle_credit + a.current_cycle_debit)) * 100, 2)
        ELSE 0.00 
    END AS cycle_credit_percentage,
    
    -- Trend analysis metrics for dashboard
    JSONB_BUILD_OBJECT(
        'balance_stability', CASE 
            WHEN (SELECT STDDEV(t.transaction_amount) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') < 100 THEN 'stable'
            WHEN (SELECT STDDEV(t.transaction_amount) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') < 500 THEN 'moderate'
            ELSE 'volatile'
        END,
        'payment_pattern', CASE 
            WHEN (SELECT COUNT(*) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_type = '02' AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') >= 1 THEN 'regular'
            ELSE 'irregular'
        END,
        'spending_pattern', CASE 
            WHEN (SELECT COUNT(*) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_type = '01' AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') > 20 THEN 'high'
            WHEN (SELECT COUNT(*) FROM transactions t WHERE t.account_id = a.account_id AND t.transaction_type = '01' AND t.transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days') > 5 THEN 'medium'
            ELSE 'low'
        END
    ) AS trend_analysis_metrics,
    
    -- Refresh tracking fields
    CURRENT_TIMESTAMP AS last_refresh_time,
    CURRENT_TIMESTAMP AS created_at
FROM accounts a;

-- ============================================================================
-- MATERIALIZED VIEW INDEXES - Optimized for sub-millisecond access
-- ============================================================================

-- Primary indexes for mv_customer_account_summary
CREATE UNIQUE INDEX idx_mv_customer_account_summary_pk ON mv_customer_account_summary (customer_id);
CREATE INDEX idx_mv_customer_account_summary_name ON mv_customer_account_summary (customer_name, total_balance);
CREATE INDEX idx_mv_customer_account_summary_balance ON mv_customer_account_summary (total_balance DESC, customer_id);
CREATE INDEX idx_mv_customer_account_summary_fico ON mv_customer_account_summary (fico_credit_score, total_balance DESC);
CREATE INDEX idx_mv_customer_account_summary_location ON mv_customer_account_summary (address_state, address_zip, customer_id);
CREATE INDEX idx_mv_customer_account_summary_portfolio ON mv_customer_account_summary (total_accounts, total_balance DESC);

-- Primary indexes for mv_card_transaction_summary
CREATE UNIQUE INDEX idx_mv_card_transaction_summary_pk ON mv_card_transaction_summary (card_number);
CREATE INDEX idx_mv_card_transaction_summary_account ON mv_card_transaction_summary (account_id, total_amount DESC);
CREATE INDEX idx_mv_card_transaction_summary_customer ON mv_card_transaction_summary (customer_id, total_transactions DESC);
CREATE INDEX idx_mv_card_transaction_summary_volume ON mv_card_transaction_summary (total_transactions DESC, total_amount DESC);
CREATE INDEX idx_mv_card_transaction_summary_active ON mv_card_transaction_summary (card_active, total_amount DESC);
CREATE INDEX idx_mv_card_transaction_summary_expiration ON mv_card_transaction_summary (expiration_date, card_active);

-- Primary indexes for mv_account_balance_history
CREATE UNIQUE INDEX idx_mv_account_balance_history_pk ON mv_account_balance_history (account_id);
CREATE INDEX idx_mv_account_balance_history_customer ON mv_account_balance_history (customer_id, current_balance DESC);
CREATE INDEX idx_mv_account_balance_history_balance ON mv_account_balance_history (current_balance DESC, available_credit DESC);
CREATE INDEX idx_mv_account_balance_history_utilization ON mv_account_balance_history (credit_utilization_percentage DESC, account_id);
CREATE INDEX idx_mv_account_balance_history_trend ON mv_account_balance_history (balance_change_30_days, balance_change_90_days);
CREATE INDEX idx_mv_account_balance_history_activity ON mv_account_balance_history (transaction_count_30_days DESC, account_id);

-- Composite indexes for cross-reference queries
CREATE INDEX idx_mv_customer_account_xref ON mv_customer_account_summary (customer_id, total_accounts, total_balance DESC);
CREATE INDEX idx_mv_card_account_xref ON mv_card_transaction_summary (account_id, customer_id, total_amount DESC);
CREATE INDEX idx_mv_account_balance_xref ON mv_account_balance_history (account_id, customer_id, current_balance DESC);

-- ============================================================================
-- AUTOMATIC REFRESH SCHEDULING - Aligned with 4-hour batch processing window
-- ============================================================================

-- Enable pg_cron extension for automatic refresh scheduling
-- Note: This extension must be installed and configured at the database level
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Function to refresh all materialized views with logging
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS void AS $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    refresh_duration INTERVAL;
BEGIN
    start_time := CURRENT_TIMESTAMP;
    
    -- Refresh mv_customer_account_summary
    RAISE NOTICE 'Refreshing mv_customer_account_summary at %', start_time;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_account_summary;
    
    -- Refresh mv_card_transaction_summary
    RAISE NOTICE 'Refreshing mv_card_transaction_summary at %', CURRENT_TIMESTAMP;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_card_transaction_summary;
    
    -- Refresh mv_account_balance_history
    RAISE NOTICE 'Refreshing mv_account_balance_history at %', CURRENT_TIMESTAMP;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_balance_history;
    
    end_time := CURRENT_TIMESTAMP;
    refresh_duration := end_time - start_time;
    
    RAISE NOTICE 'Materialized view refresh completed in % at %', refresh_duration, end_time;
END;
$$ LANGUAGE plpgsql;

-- Schedule automatic refresh every 4 hours aligned with batch processing window
-- This schedule runs at 02:00, 06:00, 10:00, 14:00, 18:00, and 22:00 daily
-- SELECT cron.schedule('refresh_materialized_views', '0 2,6,10,14,18,22 * * *', 'SELECT refresh_materialized_views();');

-- Alternative manual refresh commands for immediate execution
-- SELECT refresh_materialized_views();

-- ============================================================================
-- MATERIALIZED VIEW REFRESH STRATEGY - Performance optimization
-- ============================================================================

-- Function to refresh materialized views with dependency management
CREATE OR REPLACE FUNCTION refresh_materialized_views_with_dependencies()
RETURNS void AS $$
DECLARE
    start_time TIMESTAMP;
    view_name TEXT;
    refresh_count INTEGER := 0;
BEGIN
    start_time := CURRENT_TIMESTAMP;
    
    -- Array of materialized views in dependency order
    FOR view_name IN SELECT unnest(ARRAY[
        'mv_customer_account_summary',
        'mv_card_transaction_summary', 
        'mv_account_balance_history'
    ]) LOOP
        RAISE NOTICE 'Refreshing materialized view: % at %', view_name, CURRENT_TIMESTAMP;
        
        -- Use CONCURRENTLY to avoid blocking queries during refresh
        EXECUTE format('REFRESH MATERIALIZED VIEW CONCURRENTLY %I', view_name);
        refresh_count := refresh_count + 1;
        
        RAISE NOTICE 'Completed refresh of %', view_name;
    END LOOP;
    
    RAISE NOTICE 'Refreshed % materialized views in %', 
                 refresh_count, 
                 CURRENT_TIMESTAMP - start_time;
END;
$$ LANGUAGE plpgsql;

-- Function to check materialized view freshness
CREATE OR REPLACE FUNCTION check_materialized_view_freshness()
RETURNS TABLE(
    view_name TEXT,
    last_refresh_time TIMESTAMP,
    hours_since_refresh NUMERIC,
    needs_refresh BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'mv_customer_account_summary'::TEXT,
        (SELECT max(last_refresh_time) FROM mv_customer_account_summary),
        EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT max(last_refresh_time) FROM mv_customer_account_summary))) / 3600,
        (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT max(last_refresh_time) FROM mv_customer_account_summary))) / 3600) > 4
    UNION ALL
    SELECT 
        'mv_card_transaction_summary'::TEXT,
        (SELECT max(last_refresh_time) FROM mv_card_transaction_summary),
        EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT max(last_refresh_time) FROM mv_card_transaction_summary))) / 3600,
        (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT max(last_refresh_time) FROM mv_card_transaction_summary))) / 3600) > 4
    UNION ALL
    SELECT 
        'mv_account_balance_history'::TEXT,
        (SELECT max(last_refresh_time) FROM mv_account_balance_history),
        EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT max(last_refresh_time) FROM mv_account_balance_history))) / 3600,
        (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT max(last_refresh_time) FROM mv_account_balance_history))) / 3600) > 4;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- MATERIALIZED VIEW MAINTENANCE AND MONITORING
-- ============================================================================

-- Function to analyze materialized view performance
CREATE OR REPLACE FUNCTION analyze_materialized_view_performance()
RETURNS TABLE(
    view_name TEXT,
    size_mb NUMERIC,
    row_count BIGINT,
    index_count INTEGER,
    total_index_size_mb NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'mv_customer_account_summary'::TEXT,
        ROUND(pg_total_relation_size('mv_customer_account_summary'::regclass) / 1024.0 / 1024.0, 2),
        (SELECT COUNT(*) FROM mv_customer_account_summary),
        (SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'mv_customer_account_summary'),
        ROUND(COALESCE(SUM(pg_relation_size(indexrelid)), 0) / 1024.0 / 1024.0, 2)
    FROM pg_stat_user_indexes WHERE relname = 'mv_customer_account_summary'
    
    UNION ALL
    
    SELECT 
        'mv_card_transaction_summary'::TEXT,
        ROUND(pg_total_relation_size('mv_card_transaction_summary'::regclass) / 1024.0 / 1024.0, 2),
        (SELECT COUNT(*) FROM mv_card_transaction_summary),
        (SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'mv_card_transaction_summary'),
        ROUND(COALESCE(SUM(pg_relation_size(indexrelid)), 0) / 1024.0 / 1024.0, 2)
    FROM pg_stat_user_indexes WHERE relname = 'mv_card_transaction_summary'
    
    UNION ALL
    
    SELECT 
        'mv_account_balance_history'::TEXT,
        ROUND(pg_total_relation_size('mv_account_balance_history'::regclass) / 1024.0 / 1024.0, 2),
        (SELECT COUNT(*) FROM mv_account_balance_history),
        (SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'mv_account_balance_history'),
        ROUND(COALESCE(SUM(pg_relation_size(indexrelid)), 0) / 1024.0 / 1024.0, 2)
    FROM pg_stat_user_indexes WHERE relname = 'mv_account_balance_history';
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- MATERIALIZED VIEW COMMENTS AND DOCUMENTATION
-- ============================================================================

-- Add comprehensive comments for materialized views
COMMENT ON MATERIALIZED VIEW mv_customer_account_summary IS 'Materialized view providing aggregated customer portfolio queries with demographics, account counts, financial summaries, and portfolio metrics. Replaces CXACAIX cross-reference functionality with sub-millisecond access times.';

COMMENT ON MATERIALIZED VIEW mv_card_transaction_summary IS 'Materialized view providing card usage analytics and reporting including transaction counts, amounts, merchant analysis, and spending patterns. Optimized for card transaction history and analytics queries.';

COMMENT ON MATERIALIZED VIEW mv_account_balance_history IS 'Materialized view providing historical balance snapshots and trend analysis including balance tracking, utilization metrics, and trend indicators. Supports balance history queries and analytics reporting.';

-- Add column comments for key materialized view columns
COMMENT ON COLUMN mv_customer_account_summary.customer_id IS 'Primary key: Customer identifier from customers table';
COMMENT ON COLUMN mv_customer_account_summary.total_balance IS 'Aggregated current balance across all customer accounts';
COMMENT ON COLUMN mv_customer_account_summary.credit_utilization_percentage IS 'Calculated credit utilization percentage across all customer accounts';
COMMENT ON COLUMN mv_customer_account_summary.aggregated_portfolio_metrics IS 'JSONB object containing comprehensive portfolio metrics and KPIs';

COMMENT ON COLUMN mv_card_transaction_summary.card_number IS 'Primary key: Card number from cards table';
COMMENT ON COLUMN mv_card_transaction_summary.total_transactions IS 'Total count of transactions for the card';
COMMENT ON COLUMN mv_card_transaction_summary.card_usage_analytics IS 'JSONB object containing card usage patterns and analytics';
COMMENT ON COLUMN mv_card_transaction_summary.spending_patterns IS 'JSONB object containing temporal spending pattern analysis';

COMMENT ON COLUMN mv_account_balance_history.account_id IS 'Primary key: Account identifier from accounts table';
COMMENT ON COLUMN mv_account_balance_history.balance_trend_indicators IS 'JSONB object containing balance trend analysis and indicators';
COMMENT ON COLUMN mv_account_balance_history.trend_analysis_metrics IS 'JSONB object containing comprehensive trend analysis metrics';

-- ============================================================================
-- PERFORMANCE VALIDATION AND MONITORING QUERIES
-- ============================================================================

-- Query to validate materialized view performance
-- SELECT * FROM analyze_materialized_view_performance();

-- Query to check refresh status
-- SELECT * FROM check_materialized_view_freshness();

-- Query to validate cross-reference functionality
-- SELECT customer_id, customer_name, total_accounts, total_balance 
-- FROM mv_customer_account_summary 
-- WHERE total_balance > 10000 
-- ORDER BY total_balance DESC 
-- LIMIT 10;

-- Query to validate card transaction analytics
-- SELECT card_number, total_transactions, total_amount, average_transaction_amount
-- FROM mv_card_transaction_summary 
-- WHERE total_transactions > 0 
-- ORDER BY total_amount DESC 
-- LIMIT 10;

-- Query to validate account balance trends
-- SELECT account_id, current_balance, credit_utilization_percentage, balance_change_30_days
-- FROM mv_account_balance_history 
-- WHERE credit_utilization_percentage > 50 
-- ORDER BY credit_utilization_percentage DESC 
-- LIMIT 10;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS
-- ============================================================================

-- To rollback this migration:
-- 1. DROP MATERIALIZED VIEW mv_account_balance_history CASCADE;
-- 2. DROP MATERIALIZED VIEW mv_card_transaction_summary CASCADE;
-- 3. DROP MATERIALIZED VIEW mv_customer_account_summary CASCADE;
-- 4. DROP FUNCTION refresh_materialized_views() CASCADE;
-- 5. DROP FUNCTION refresh_materialized_views_with_dependencies() CASCADE;
-- 6. DROP FUNCTION check_materialized_view_freshness() CASCADE;
-- 7. DROP FUNCTION analyze_materialized_view_performance() CASCADE;
-- 8. Remove cron job: SELECT cron.unschedule('refresh_materialized_views');

-- ============================================================================
-- DEPLOYMENT VALIDATION CHECKLIST
-- ============================================================================

-- □ All materialized views created successfully
-- □ All indexes created and optimized for query performance
-- □ Refresh functions created and tested
-- □ Automatic refresh scheduling configured (if pg_cron available)
-- □ Performance validation queries execute under 1ms
-- □ View freshness monitoring functions operational
-- □ Cross-reference functionality validates against VSAM patterns
-- □ Memory usage and storage requirements within acceptable limits
-- □ Documentation and comments complete for maintenance
-- □ Rollback procedures tested and documented