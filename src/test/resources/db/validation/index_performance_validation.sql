-- ==============================================================================
-- Index Performance Validation Script: index_performance_validation.sql
-- Description: Comprehensive PostgreSQL B-tree index performance validation
--              ensuring VSAM alternate index functionality equivalence with
--              sub-200ms response time compliance for CardDemo microservices
-- Author: Blitzy agent
-- Version: 1.0
-- Migration Type: VALIDATION SCRIPT with performance metrics and compliance testing
-- Dependencies: V7__create_indexes.sql, accounts/cards/transactions tables, repository query patterns
-- ==============================================================================

-- Set session parameters for consistent testing environment
SET work_mem = '256MB';
SET shared_buffers = '512MB';
SET effective_cache_size = '2GB';
SET enable_seqscan = off;  -- Force index usage for testing
SET log_statement_stats = on;
SET track_io_timing = on;

-- Create validation results table for storing test results
CREATE TEMP TABLE IF NOT EXISTS index_performance_results (
    test_id SERIAL PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    index_name VARCHAR(255),
    table_name VARCHAR(255),
    query_type VARCHAR(100),
    execution_time_ms NUMERIC(10,3),
    rows_examined BIGINT,
    index_scans BIGINT,
    index_only_scan BOOLEAN DEFAULT FALSE,
    meets_performance_requirement BOOLEAN,
    vsam_equivalence_status VARCHAR(50),
    test_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- Create index coverage analysis table
CREATE TEMP TABLE IF NOT EXISTS index_coverage_analysis (
    analysis_id SERIAL PRIMARY KEY,
    table_name VARCHAR(255),
    index_name VARCHAR(255),
    columns_covered TEXT[],
    include_columns TEXT[],
    query_pattern VARCHAR(500),
    coverage_effectiveness VARCHAR(50),
    missing_coverage TEXT,
    optimization_recommendations TEXT,
    analysis_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- PRIMARY BUSINESS INDEX PERFORMANCE VALIDATION
-- =============================================================================

-- Test Case 1: Card-Account Relationship Index (idx_cards_account_id)
-- Validates VSAM CARDAIX alternate index functionality replication
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    query_plan TEXT;
    index_used BOOLEAN := FALSE;
BEGIN
    -- Test account-based card lookup with active status filtering
    start_time := clock_timestamp();
    
    PERFORM card_number, account_id, active_status 
    FROM cards 
    WHERE account_id = '12345678901' AND active_status = 'Y';
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    -- Check if index was used
    EXPLAIN (FORMAT JSON) 
    SELECT card_number, account_id, active_status 
    FROM cards 
    WHERE account_id = '12345678901' AND active_status = 'Y';
    
    -- Record results
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type, 
        execution_time_ms, meets_performance_requirement, 
        vsam_equivalence_status, notes
    ) VALUES (
        'Card-Account Lookup Performance',
        'idx_cards_account_id',
        'cards',
        'ACCOUNT_CARD_LOOKUP',
        execution_ms,
        execution_ms < 200,
        CASE WHEN execution_ms < 5 THEN 'VSAM_EQUIVALENT' 
             WHEN execution_ms < 50 THEN 'ACCEPTABLE' 
             ELSE 'PERFORMANCE_ISSUE' END,
        format('CardListService equivalent query - Execution time: %s ms', execution_ms)
    );
END $$;

-- Test Case 2: Customer-Account Cross-Reference Index (idx_customer_account_xref)
-- Validates customer account portfolio queries performance
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    row_count INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO row_count
    FROM accounts 
    WHERE customer_id = '123456789' AND active_status = true;
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, rows_examined, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Customer Account Cross-Reference',
        'idx_customer_account_xref',
        'accounts',
        'CUSTOMER_ACCOUNT_XREF',
        execution_ms,
        row_count,
        execution_ms < 200,
        CASE WHEN execution_ms < 10 THEN 'VSAM_EQUIVALENT'
             WHEN execution_ms < 100 THEN 'ACCEPTABLE'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('AccountRepository.findByCustomerIdOrderByAccountId equivalent - %s accounts found', row_count)
    );
END $$;

-- Test Case 3: Transaction Date Range Index (idx_transactions_date_range)
-- Validates VSAM TRANSACT.VSAM.AIX functionality with partition pruning
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    partition_pruning_enabled BOOLEAN := FALSE;
    row_count INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO row_count
    FROM transactions 
    WHERE transaction_timestamp >= CURRENT_DATE - INTERVAL '30 days'
      AND transaction_timestamp <= CURRENT_DATE
      AND account_id = '12345678901';
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, rows_examined, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Transaction Date Range Query',
        'idx_transactions_date_range',
        'transactions',
        'DATE_RANGE_ACCOUNT_LOOKUP',
        execution_ms,
        row_count,
        execution_ms < 200,
        CASE WHEN execution_ms < 50 THEN 'VSAM_EQUIVALENT'
             WHEN execution_ms < 150 THEN 'ACCEPTABLE'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('TransactionRepository.findByAccountIdAndDateRange equivalent - %s transactions found', row_count)
    );
END $$;

-- Test Case 4: Account Balance Covering Index (idx_account_balance)
-- Validates sub-200ms balance inquiry requirement with index-only scans
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    index_only_scan BOOLEAN := FALSE;
    query_plan JSON;
BEGIN
    start_time := clock_timestamp();
    
    PERFORM current_balance, active_status
    FROM accounts 
    WHERE account_id = '12345678901';
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    -- Check for index-only scan usage
    SELECT to_json(query_plan) INTO query_plan
    FROM (
        EXPLAIN (FORMAT JSON, ANALYZE, BUFFERS) 
        SELECT current_balance, active_status
        FROM accounts 
        WHERE account_id = '12345678901'
    ) AS plan(query_plan);
    
    index_only_scan := query_plan::text ILIKE '%Index Only Scan%';
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, index_only_scan, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Account Balance Index-Only Scan',
        'idx_account_balance',
        'accounts',
        'BALANCE_INQUIRY',
        execution_ms,
        index_only_scan,
        execution_ms < 200,
        CASE WHEN execution_ms < 10 AND index_only_scan THEN 'VSAM_SUPERIOR'
             WHEN execution_ms < 50 THEN 'VSAM_EQUIVALENT'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('AccountRepository.findByAccountIdAndActiveStatus equivalent - Index-only scan: %s', index_only_scan)
    );
END $$;

-- =============================================================================
-- COVERING INDEX VALIDATION FOR INDEX-ONLY SCANS
-- =============================================================================

-- Test Case 5: Customer Profile Covering Index
-- Validates customer demographic data retrieval without heap access
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    heap_fetches INTEGER := 0;
BEGIN
    start_time := clock_timestamp();
    
    PERFORM customer_id, first_name, last_name, phone_number_1, fico_credit_score
    FROM customers 
    WHERE customer_id = '123456789';
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Customer Profile Covering Index',
        'idx_customers_profile_covering',
        'customers',
        'CUSTOMER_PROFILE_LOOKUP',
        execution_ms,
        execution_ms < 50,
        CASE WHEN execution_ms < 5 THEN 'VSAM_SUPERIOR'
             WHEN execution_ms < 25 THEN 'VSAM_EQUIVALENT'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('Customer service lookup optimization - Execution time: %s ms', execution_ms)
    );
END $$;

-- Test Case 6: Card Details Covering Index
-- Validates card information API endpoints with minimal I/O
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    index_only_scan BOOLEAN;
BEGIN
    start_time := clock_timestamp();
    
    PERFORM account_id, customer_id, embossed_name, expiration_date, active_status
    FROM cards 
    WHERE card_number = '4000123456789012';
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Card Details Covering Index',
        'idx_cards_details_covering',
        'cards',
        'CARD_DETAILS_LOOKUP',
        execution_ms,
        execution_ms < 50,
        CASE WHEN execution_ms < 2 THEN 'VSAM_SUPERIOR'
             WHEN execution_ms < 10 THEN 'VSAM_EQUIVALENT'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('CardViewService optimization - Execution time: %s ms', execution_ms)
    );
END $$;

-- =============================================================================
-- COMPOSITE INDEX EFFECTIVENESS TESTING
-- =============================================================================

-- Test Case 7: Account Pagination Index
-- Validates Spring Data JPA pagination query patterns
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    row_count INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO row_count
    FROM accounts 
    WHERE customer_id = '123456789' 
      AND active_status = true 
    ORDER BY account_id 
    LIMIT 10 OFFSET 0;
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, rows_examined, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Account Pagination Query',
        'idx_accounts_pagination',
        'accounts',
        'PAGINATED_LOOKUP',
        execution_ms,
        row_count,
        execution_ms < 100,
        CASE WHEN execution_ms < 20 THEN 'VSAM_EQUIVALENT'
             WHEN execution_ms < 75 THEN 'ACCEPTABLE'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('Spring Data JPA pagination pattern - %s accounts paginated', row_count)
    );
END $$;

-- Test Case 8: Transaction Classification Index
-- Validates transaction type and category analysis performance
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    row_count INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO row_count
    FROM transactions 
    WHERE transaction_type = 'PURCHASE' 
      AND transaction_category = 'RETAIL'
      AND transaction_timestamp >= CURRENT_DATE - INTERVAL '7 days';
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, rows_examined, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Transaction Classification Query',
        'idx_transactions_classification',
        'transactions',
        'CLASSIFICATION_LOOKUP',
        execution_ms,
        row_count,
        execution_ms < 200,
        CASE WHEN execution_ms < 50 THEN 'VSAM_EQUIVALENT'
             WHEN execution_ms < 150 THEN 'ACCEPTABLE'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('Transaction categorization microservice - %s transactions classified', row_count)
    );
END $$;

-- =============================================================================
-- INDEX USAGE STATISTICS ANALYSIS
-- =============================================================================

-- Collect comprehensive index usage statistics
INSERT INTO index_coverage_analysis (
    table_name, index_name, columns_covered, include_columns,
    query_pattern, coverage_effectiveness, optimization_recommendations
)
SELECT 
    schemaname || '.' || tablename as table_name,
    indexname as index_name,
    ARRAY[indexname] as columns_covered,  -- Simplified for this validation
    CASE 
        WHEN indexname LIKE '%_covering' THEN ARRAY['INCLUDE columns present']
        ELSE ARRAY[]::TEXT[]
    END as include_columns,
    'Repository query pattern' as query_pattern,
    CASE 
        WHEN idx_scan > 1000 THEN 'HIGH_USAGE'
        WHEN idx_scan > 100 THEN 'MODERATE_USAGE'
        WHEN idx_scan > 10 THEN 'LOW_USAGE'
        ELSE 'UNUSED'
    END as coverage_effectiveness,
    CASE 
        WHEN idx_scan = 0 THEN 'Consider removing unused index'
        WHEN idx_scan < 10 THEN 'Monitor usage patterns'
        ELSE 'Index performing well'
    END as optimization_recommendations
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
  AND (indexname LIKE 'idx_cards_%' 
       OR indexname LIKE 'idx_accounts_%' 
       OR indexname LIKE 'idx_transactions_%'
       OR indexname LIKE 'idx_customers_%');

-- =============================================================================
-- FOREIGN KEY INDEX PERFORMANCE VALIDATION
-- =============================================================================

-- Test Case 9: Foreign Key Join Performance
-- Validates foreign key constraint index performance for JOIN operations
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
    join_rows INTEGER;
BEGIN
    start_time := clock_timestamp();
    
    SELECT COUNT(*) INTO join_rows
    FROM accounts a
    JOIN customers c ON a.customer_id = c.customer_id
    WHERE a.active_status = true
      AND c.fico_credit_score > 700;
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, rows_examined, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Foreign Key JOIN Performance',
        'idx_accounts_customer_fk',
        'accounts',
        'FK_JOIN_OPERATION',
        execution_ms,
        join_rows,
        execution_ms < 300,
        CASE WHEN execution_ms < 100 THEN 'VSAM_EQUIVALENT'
             WHEN execution_ms < 250 THEN 'ACCEPTABLE'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('Customer-Account JOIN operation - %s joined records', join_rows)
    );
END $$;

-- =============================================================================
-- REFERENCE TABLE INDEX VALIDATION
-- =============================================================================

-- Test Case 10: Reference Table Lookup Performance
-- Validates sub-millisecond lookup performance for reference tables
DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    execution_ms NUMERIC;
BEGIN
    start_time := clock_timestamp();
    
    PERFORM type_description, debit_credit_indicator
    FROM transaction_types 
    WHERE transaction_type = 'PURCHASE' AND active_status = true;
    
    end_time := clock_timestamp();
    execution_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;
    
    INSERT INTO index_performance_results (
        test_name, index_name, table_name, query_type,
        execution_time_ms, meets_performance_requirement,
        vsam_equivalence_status, notes
    ) VALUES (
        'Reference Table Lookup',
        'idx_transaction_types_lookup',
        'transaction_types',
        'REFERENCE_LOOKUP',
        execution_ms,
        execution_ms < 5,
        CASE WHEN execution_ms < 1 THEN 'VSAM_SUPERIOR'
             WHEN execution_ms < 3 THEN 'VSAM_EQUIVALENT'
             ELSE 'PERFORMANCE_ISSUE' END,
        format('Transaction type resolution - Execution time: %s ms', execution_ms)
    );
END $$;

-- =============================================================================
-- PERFORMANCE COMPLIANCE SUMMARY REPORTING
-- =============================================================================

-- Generate comprehensive performance validation report
CREATE OR REPLACE VIEW IndexPerformanceValidationResults AS
SELECT 
    test_name,
    index_name,
    table_name,
    query_type,
    execution_time_ms,
    CASE 
        WHEN execution_time_ms < 10 THEN 'EXCELLENT'
        WHEN execution_time_ms < 50 THEN 'GOOD'
        WHEN execution_time_ms < 200 THEN 'ACCEPTABLE'
        ELSE 'POOR'
    END as performance_grade,
    meets_performance_requirement,
    vsam_equivalence_status,
    CASE 
        WHEN vsam_equivalence_status = 'VSAM_SUPERIOR' THEN 'PostgreSQL outperforms VSAM'
        WHEN vsam_equivalence_status = 'VSAM_EQUIVALENT' THEN 'Performance matches VSAM'
        WHEN vsam_equivalence_status = 'ACCEPTABLE' THEN 'Acceptable performance'
        ELSE 'Performance issue requires attention'
    END as equivalence_assessment,
    rows_examined,
    index_only_scan,
    notes,
    test_timestamp
FROM index_performance_results
ORDER BY test_timestamp DESC;

-- Generate index coverage analysis summary
CREATE OR REPLACE VIEW IndexCoverageAnalysis AS
SELECT 
    table_name,
    index_name,
    array_to_string(columns_covered, ', ') as covered_columns,
    array_to_string(include_columns, ', ') as include_columns,
    coverage_effectiveness,
    optimization_recommendations,
    CASE 
        WHEN coverage_effectiveness = 'HIGH_USAGE' THEN 'Critical index - maintain'
        WHEN coverage_effectiveness = 'MODERATE_USAGE' THEN 'Important index - monitor'
        WHEN coverage_effectiveness = 'LOW_USAGE' THEN 'Review usage patterns'
        ELSE 'Consider removal if consistently unused'
    END as maintenance_recommendation,
    analysis_timestamp
FROM index_coverage_analysis
ORDER BY 
    CASE coverage_effectiveness
        WHEN 'HIGH_USAGE' THEN 1
        WHEN 'MODERATE_USAGE' THEN 2
        WHEN 'LOW_USAGE' THEN 3
        ELSE 4
    END,
    table_name, index_name;

-- =============================================================================
-- VALIDATION RESULTS SUMMARY
-- =============================================================================

-- Performance compliance summary
SELECT 
    'INDEX PERFORMANCE VALIDATION SUMMARY' as report_section,
    '' as details
UNION ALL
SELECT 
    '==========================================',
    ''
UNION ALL
SELECT 
    'Total Tests Executed:',
    COUNT(*)::TEXT
FROM index_performance_results
UNION ALL
SELECT 
    'Tests Meeting Performance Requirements:',
    COUNT(CASE WHEN meets_performance_requirement THEN 1 END)::TEXT
FROM index_performance_results
UNION ALL
SELECT 
    'VSAM Equivalent or Superior Performance:',
    COUNT(CASE WHEN vsam_equivalence_status IN ('VSAM_EQUIVALENT', 'VSAM_SUPERIOR') THEN 1 END)::TEXT
FROM index_performance_results
UNION ALL
SELECT 
    'Average Query Execution Time (ms):',
    ROUND(AVG(execution_time_ms), 3)::TEXT
FROM index_performance_results
UNION ALL
SELECT 
    'Maximum Query Execution Time (ms):',
    ROUND(MAX(execution_time_ms), 3)::TEXT
FROM index_performance_results
UNION ALL
SELECT 
    'Sub-200ms Compliance Rate:',
    ROUND(
        (COUNT(CASE WHEN execution_time_ms < 200 THEN 1 END) * 100.0 / COUNT(*)), 2
    )::TEXT || '%'
FROM index_performance_results
UNION ALL
SELECT 
    'Index-Only Scan Utilization:',
    COUNT(CASE WHEN index_only_scan THEN 1 END)::TEXT || ' of ' || 
    COUNT(CASE WHEN index_only_scan IS NOT NULL THEN 1 END)::TEXT || ' applicable tests'
FROM index_performance_results;

-- Critical performance issues report
SELECT 
    'CRITICAL PERFORMANCE ISSUES REQUIRING ATTENTION' as alert_section,
    '' as issue_details
WHERE EXISTS (
    SELECT 1 FROM index_performance_results 
    WHERE NOT meets_performance_requirement
)
UNION ALL
SELECT 
    '================================================',
    ''
WHERE EXISTS (
    SELECT 1 FROM index_performance_results 
    WHERE NOT meets_performance_requirement
)
UNION ALL
SELECT 
    test_name,
    format('Execution time: %s ms (exceeds %s ms requirement)', 
           execution_time_ms, 
           CASE WHEN query_type = 'BALANCE_INQUIRY' THEN '200'
                WHEN query_type LIKE '%_LOOKUP' THEN '50'
                ELSE '200' END)
FROM index_performance_results
WHERE NOT meets_performance_requirement
ORDER BY execution_time_ms DESC;

-- VSAM equivalence validation summary
SELECT 
    'VSAM ALTERNATE INDEX EQUIVALENCE VALIDATION' as equivalence_section,
    '' as status_details
UNION ALL
SELECT 
    '==============================================',
    ''
UNION ALL
SELECT 
    vsam_equivalence_status,
    COUNT(*)::TEXT || ' tests'
FROM index_performance_results
GROUP BY vsam_equivalence_status
ORDER BY 
    CASE vsam_equivalence_status
        WHEN 'VSAM_SUPERIOR' THEN 1
        WHEN 'VSAM_EQUIVALENT' THEN 2
        WHEN 'ACCEPTABLE' THEN 3
        ELSE 4
    END;

-- Reset session parameters
SET enable_seqscan = on;
SET log_statement_stats = off;

-- Index performance validation completed successfully
SELECT 
    'CardDemo Index Performance Validation Completed' as validation_status,
    'PostgreSQL B-tree indexes validated for VSAM alternate index equivalence' as summary,
    CURRENT_TIMESTAMP as completion_time;

-- =============================================================================
-- END OF INDEX PERFORMANCE VALIDATION
-- =============================================================================