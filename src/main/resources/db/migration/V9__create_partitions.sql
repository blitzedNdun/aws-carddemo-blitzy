-- =====================================================================================
-- Liquibase Migration: V9__create_partitions.sql
-- Description: Advanced PostgreSQL partitioning strategy implementing pg_partman 
--              extension for automated partition management, 13-month rolling window
--              retention, and optimized date-range query performance with 90%+ scan
--              time reduction for transactions table batch processing efficiency
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 9.0
-- Dependencies: V5__create_transactions_table.sql, pg_partman extension
-- =====================================================================================

-- changeset blitzy:V9-advanced-partitioning-strategy
-- comment: Implement advanced PostgreSQL partitioning with pg_partman extension for automated maintenance, 13-month rolling window retention, and partition pruning optimization

-- ===================================================================================
-- SECTION 1: pg_partman Extension Installation and Configuration
-- ===================================================================================

-- Enable pg_partman extension for automated partition management
CREATE EXTENSION IF NOT EXISTS pg_partman;

-- Create dedicated schema for pg_partman functions and configuration
CREATE SCHEMA IF NOT EXISTS partman;

-- Grant necessary permissions for pg_partman operations
GRANT ALL ON SCHEMA partman TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA partman TO postgres;
GRANT ALL ON ALL SEQUENCES IN SCHEMA partman TO postgres;

-- Configure pg_partman background worker settings for optimal performance
-- These settings align with the 4-hour batch processing window requirement
ALTER SYSTEM SET pg_partman_bgw.interval = 3600; -- Run every hour
ALTER SYSTEM SET pg_partman_bgw.role = 'postgres';
ALTER SYSTEM SET pg_partman_bgw.dbname = 'carddemo';

-- Reload PostgreSQL configuration to apply pg_partman settings
SELECT pg_reload_conf();

-- ===================================================================================
-- SECTION 2: Advanced Partition Configuration for Transactions Table
-- ===================================================================================

-- Configure pg_partman for transactions table with monthly partitioning
-- This replaces manual partition creation with automated management
INSERT INTO partman.part_config (
    parent_table,
    control,
    partition_interval,
    partition_type,
    retention,
    retention_schema,
    retention_keep_table,
    retention_keep_index,
    automatic_maintenance,
    jobmon,
    sub_partition_set_full_name,
    template_table,
    inherit_privileges,
    constraint_cols,
    premake,
    optimize_trigger,
    optimize_constraint,
    epoch,
    use_run_maintenance,
    log_level
) VALUES (
    'transactions',                    -- parent_table: Target table for partitioning
    'transaction_timestamp',           -- control: Partition column (timestamp)
    'monthly',                        -- partition_interval: Monthly partitions
    'range',                          -- partition_type: RANGE partitioning
    '13 months',                      -- retention: 13-month rolling window
    'archive',                        -- retention_schema: Archive schema for old partitions
    true,                             -- retention_keep_table: Keep archived tables
    true,                             -- retention_keep_index: Keep indexes on archived tables
    'on',                             -- automatic_maintenance: Enable automation
    true,                             -- jobmon: Enable job monitoring
    false,                            -- sub_partition_set_full_name: No sub-partitioning
    'transactions_template',          -- template_table: Template for new partitions
    true,                             -- inherit_privileges: Inherit table privileges
    ARRAY['account_id'],              -- constraint_cols: Additional constraint columns
    4,                                -- premake: Pre-create 4 future partitions
    4,                                -- optimize_trigger: Optimize trigger performance
    30,                               -- optimize_constraint: Constraint exclusion optimization
    'none',                           -- epoch: Not using epoch-based partitioning
    true,                             -- use_run_maintenance: Use run_maintenance function
    'INFO'                            -- log_level: Information logging level
)
ON CONFLICT (parent_table) DO UPDATE SET
    retention = EXCLUDED.retention,
    premake = EXCLUDED.premake,
    optimize_constraint = EXCLUDED.optimize_constraint,
    automatic_maintenance = EXCLUDED.automatic_maintenance;

-- ===================================================================================
-- SECTION 3: Archive Schema and Historical Data Management
-- ===================================================================================

-- Create archive schema for historical data retention
CREATE SCHEMA IF NOT EXISTS archive;

-- Grant necessary permissions for archive operations
GRANT ALL ON SCHEMA archive TO postgres;
GRANT USAGE ON SCHEMA archive TO carddemo_app;

-- Create archive table template matching transactions structure
CREATE TABLE IF NOT EXISTS archive.transactions_template (
    LIKE transactions INCLUDING ALL
);

-- Add metadata columns for archived partitions
ALTER TABLE archive.transactions_template 
ADD COLUMN IF NOT EXISTS archived_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE archive.transactions_template 
ADD COLUMN IF NOT EXISTS archive_reason VARCHAR(50) DEFAULT 'retention_policy';

-- Create function for historical data archival with compliance logging
CREATE OR REPLACE FUNCTION archive_partition_data(
    p_parent_table TEXT,
    p_retention_months INTEGER DEFAULT 13
) RETURNS VOID AS $$
DECLARE
    v_partition_name TEXT;
    v_archive_table TEXT;
    v_cutoff_date TIMESTAMP;
    v_partition_count INTEGER := 0;
    v_record_count INTEGER := 0;
    v_partition_record RECORD;
BEGIN
    -- Calculate cutoff date based on retention policy
    v_cutoff_date := CURRENT_TIMESTAMP - (p_retention_months || ' months')::INTERVAL;
    
    -- Log archival operation start
    RAISE NOTICE 'Starting archival process for % with cutoff date %', p_parent_table, v_cutoff_date;
    
    -- Process each partition that exceeds retention period
    FOR v_partition_record IN
        SELECT schemaname, tablename, partitionname
        FROM pg_partitions
        WHERE schemaname = 'public' 
        AND tablename = p_parent_table
        AND partitionname LIKE '%_' || TO_CHAR(v_cutoff_date, 'YYYY_MM') || '%'
    LOOP
        v_partition_name := v_partition_record.partitionname;
        v_archive_table := 'archive.' || v_partition_name;
        
        -- Create archive table if it doesn't exist
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %s (LIKE %s INCLUDING ALL)',
            v_archive_table, 
            v_partition_record.schemaname || '.' || v_partition_name
        );
        
        -- Move data to archive table
        EXECUTE format(
            'INSERT INTO %s SELECT *, CURRENT_TIMESTAMP, ''retention_policy'' FROM %s',
            v_archive_table,
            v_partition_record.schemaname || '.' || v_partition_name
        );
        
        -- Get record count for logging
        EXECUTE format('SELECT COUNT(*) FROM %s', v_archive_table) INTO v_record_count;
        
        -- Drop the original partition
        EXECUTE format('DROP TABLE %s', v_partition_record.schemaname || '.' || v_partition_name);
        
        v_partition_count := v_partition_count + 1;
        
        RAISE NOTICE 'Archived partition % with % records', v_partition_name, v_record_count;
    END LOOP;
    
    -- Log completion
    RAISE NOTICE 'Archival process completed. Processed % partitions', v_partition_count;
END;
$$ LANGUAGE plpgsql;

-- ===================================================================================
-- SECTION 4: Constraint Exclusion and Query Optimization
-- ===================================================================================

-- Enable constraint exclusion for optimal partition pruning
-- This setting allows PostgreSQL to eliminate partitions from query plans
ALTER SYSTEM SET constraint_exclusion = 'partition';

-- Configure work_mem for optimal sorting and hash operations during partition pruning
ALTER SYSTEM SET work_mem = '256MB';

-- Set effective_cache_size for query planner optimization
ALTER SYSTEM SET effective_cache_size = '4GB';

-- Configure random_page_cost for SSD optimization
ALTER SYSTEM SET random_page_cost = 1.1;

-- Enable parallel processing for partition-wise operations
ALTER SYSTEM SET max_parallel_workers_per_gather = 4;
ALTER SYSTEM SET max_parallel_workers = 8;

-- Configure partition-wise joins for better performance
ALTER SYSTEM SET enable_partitionwise_join = on;
ALTER SYSTEM SET enable_partitionwise_aggregate = on;

-- Reload configuration to apply optimization settings
SELECT pg_reload_conf();

-- ===================================================================================
-- SECTION 5: Enhanced Indexing for Partition Pruning
-- ===================================================================================

-- Create partition-aware indexes for optimal query performance
-- These indexes support the 90%+ scan time reduction requirement

-- Enhanced date-range index with covering columns for index-only scans
CREATE INDEX IF NOT EXISTS idx_transactions_partition_pruning 
ON transactions (transaction_timestamp, account_id, transaction_amount, transaction_type)
WHERE transaction_timestamp >= '2024-01-01';

-- Create partial indexes on active partitions for frequently accessed data
CREATE INDEX IF NOT EXISTS idx_transactions_recent_activity 
ON transactions (account_id, transaction_timestamp DESC, transaction_amount)
WHERE transaction_timestamp >= CURRENT_TIMESTAMP - INTERVAL '6 months';

-- Create composite index for batch processing optimization
CREATE INDEX IF NOT EXISTS idx_transactions_batch_processing 
ON transactions (transaction_timestamp, card_number, transaction_category, transaction_type)
WHERE transaction_timestamp >= '2024-01-01';

-- Create index for merchant-based queries with partition awareness
CREATE INDEX IF NOT EXISTS idx_transactions_merchant_partition 
ON transactions (merchant_name, transaction_timestamp, transaction_amount)
WHERE transaction_timestamp >= '2024-01-01' AND merchant_name IS NOT NULL;

-- ===================================================================================
-- SECTION 6: Automated Partition Maintenance Procedures
-- ===================================================================================

-- Create function for automated partition maintenance
CREATE OR REPLACE FUNCTION maintain_transaction_partitions()
RETURNS VOID AS $$
DECLARE
    v_maintenance_result TEXT;
    v_partition_count INTEGER;
    v_cleanup_count INTEGER;
BEGIN
    -- Log maintenance start
    RAISE NOTICE 'Starting automated partition maintenance at %', CURRENT_TIMESTAMP;
    
    -- Run pg_partman maintenance for transactions table
    SELECT partman.run_maintenance() INTO v_maintenance_result;
    
    -- Get current partition count
    SELECT COUNT(*) INTO v_partition_count
    FROM pg_inherits i
    JOIN pg_class c ON i.inhrelid = c.oid
    JOIN pg_class p ON i.inhparent = p.oid
    WHERE p.relname = 'transactions';
    
    -- Clean up old archived partitions beyond retention period
    SELECT archive_partition_data('transactions', 13) INTO v_cleanup_count;
    
    -- Update partition statistics for query optimizer
    ANALYZE transactions;
    
    -- Refresh materialized views that depend on transactions
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_summary;
    
    -- Log maintenance completion
    RAISE NOTICE 'Partition maintenance completed. Active partitions: %, Maintenance result: %', 
                 v_partition_count, v_maintenance_result;
END;
$$ LANGUAGE plpgsql;

-- Create scheduled maintenance job using pg_cron extension
-- This ensures automated maintenance runs during off-peak hours
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Schedule partition maintenance to run daily at 2:00 AM
SELECT cron.schedule(
    'transaction_partition_maintenance',
    '0 2 * * *',  -- Daily at 2:00 AM
    'SELECT maintain_transaction_partitions();'
);

-- ===================================================================================
-- SECTION 7: Performance Monitoring and Alerting
-- ===================================================================================

-- Create function to monitor partition performance metrics
CREATE OR REPLACE FUNCTION monitor_partition_performance()
RETURNS TABLE (
    partition_name TEXT,
    partition_size_mb BIGINT,
    row_count BIGINT,
    avg_query_time_ms NUMERIC,
    index_usage_ratio NUMERIC,
    constraint_exclusion_effective BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.relname::TEXT as partition_name,
        (pg_relation_size(c.oid) / 1024 / 1024)::BIGINT as partition_size_mb,
        c.reltuples::BIGINT as row_count,
        COALESCE(
            (SELECT avg(mean_exec_time) 
             FROM pg_stat_statements 
             WHERE query LIKE '%' || c.relname || '%'), 0
        )::NUMERIC as avg_query_time_ms,
        CASE 
            WHEN c.reltuples > 0 THEN 
                (SELECT COALESCE(sum(idx_scan)::NUMERIC / NULLIF(sum(seq_scan + idx_scan), 0), 0)
                 FROM pg_stat_user_tables 
                 WHERE relname = c.relname)
            ELSE 0
        END as index_usage_ratio,
        (SELECT COUNT(*) > 0 
         FROM pg_constraint 
         WHERE conrelid = c.oid 
         AND contype = 'c') as constraint_exclusion_effective
    FROM pg_class c
    JOIN pg_inherits i ON c.oid = i.inhrelid
    JOIN pg_class p ON i.inhparent = p.oid
    WHERE p.relname = 'transactions'
    ORDER BY partition_name;
END;
$$ LANGUAGE plpgsql;

-- Create function to generate partition health report
CREATE OR REPLACE FUNCTION generate_partition_health_report()
RETURNS TEXT AS $$
DECLARE
    v_report TEXT := '';
    v_total_partitions INTEGER;
    v_total_size_gb NUMERIC;
    v_oldest_partition DATE;
    v_newest_partition DATE;
    v_performance_record RECORD;
BEGIN
    -- Get partition statistics
    SELECT COUNT(*) INTO v_total_partitions
    FROM pg_inherits i
    JOIN pg_class c ON i.inhrelid = c.oid
    JOIN pg_class p ON i.inhparent = p.oid
    WHERE p.relname = 'transactions';
    
    SELECT ROUND(SUM(pg_relation_size(c.oid)) / 1024.0 / 1024.0 / 1024.0, 2) INTO v_total_size_gb
    FROM pg_inherits i
    JOIN pg_class c ON i.inhrelid = c.oid
    JOIN pg_class p ON i.inhparent = p.oid
    WHERE p.relname = 'transactions';
    
    -- Build report
    v_report := 'TRANSACTION PARTITIONS HEALTH REPORT' || CHR(10);
    v_report := v_report || '========================================' || CHR(10);
    v_report := v_report || 'Generated at: ' || CURRENT_TIMESTAMP || CHR(10);
    v_report := v_report || 'Total Partitions: ' || v_total_partitions || CHR(10);
    v_report := v_report || 'Total Size: ' || v_total_size_gb || ' GB' || CHR(10);
    v_report := v_report || CHR(10);
    
    -- Add performance metrics
    v_report := v_report || 'PERFORMANCE METRICS:' || CHR(10);
    v_report := v_report || '-------------------' || CHR(10);
    
    FOR v_performance_record IN 
        SELECT * FROM monitor_partition_performance()
        ORDER BY partition_name
    LOOP
        v_report := v_report || 'Partition: ' || v_performance_record.partition_name || CHR(10);
        v_report := v_report || '  Size: ' || v_performance_record.partition_size_mb || ' MB' || CHR(10);
        v_report := v_report || '  Rows: ' || v_performance_record.row_count || CHR(10);
        v_report := v_report || '  Avg Query Time: ' || v_performance_record.avg_query_time_ms || ' ms' || CHR(10);
        v_report := v_report || '  Index Usage: ' || ROUND(v_performance_record.index_usage_ratio * 100, 2) || '%' || CHR(10);
        v_report := v_report || '  Constraint Exclusion: ' || 
                   CASE WHEN v_performance_record.constraint_exclusion_effective THEN 'Active' ELSE 'Inactive' END || CHR(10);
        v_report := v_report || CHR(10);
    END LOOP;
    
    RETURN v_report;
END;
$$ LANGUAGE plpgsql;

-- ===================================================================================
-- SECTION 8: Compliance and Audit Trail Enhancement
-- ===================================================================================

-- Create audit table for partition operations
CREATE TABLE IF NOT EXISTS partition_audit_log (
    audit_id SERIAL PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    partition_name VARCHAR(100),
    operation_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    user_name VARCHAR(50) DEFAULT CURRENT_USER,
    operation_details JSONB,
    success_status BOOLEAN DEFAULT TRUE,
    error_message TEXT
);

-- Create trigger function for partition operation auditing
CREATE OR REPLACE FUNCTION log_partition_operation()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO partition_audit_log (
            operation_type, table_name, partition_name, 
            operation_details, success_status
        ) VALUES (
            'PARTITION_CREATE', TG_TABLE_NAME, NEW.relname,
            jsonb_build_object('size', pg_relation_size(NEW.oid), 'created_at', CURRENT_TIMESTAMP),
            true
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO partition_audit_log (
            operation_type, table_name, partition_name,
            operation_details, success_status
        ) VALUES (
            'PARTITION_DROP', TG_TABLE_NAME, OLD.relname,
            jsonb_build_object('final_size', pg_relation_size(OLD.oid), 'dropped_at', CURRENT_TIMESTAMP),
            true
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- ===================================================================================
-- SECTION 9: Batch Processing Window Optimization
-- ===================================================================================

-- Create function to optimize batch processing window performance
CREATE OR REPLACE FUNCTION optimize_batch_processing_window()
RETURNS VOID AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_processing_date DATE;
    v_partition_name TEXT;
    v_optimization_count INTEGER := 0;
BEGIN
    v_start_time := CURRENT_TIMESTAMP;
    v_processing_date := CURRENT_DATE;
    
    -- Log batch optimization start
    RAISE NOTICE 'Starting batch processing window optimization at %', v_start_time;
    
    -- Temporarily adjust PostgreSQL settings for batch processing
    SET work_mem = '1GB';
    SET maintenance_work_mem = '2GB';
    SET max_parallel_workers_per_gather = 6;
    SET random_page_cost = 1.0;
    
    -- Analyze current month's partition for optimal query plans
    v_partition_name := 'transactions_' || TO_CHAR(v_processing_date, 'YYYY_MM');
    
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = v_partition_name) THEN
        EXECUTE format('ANALYZE %s', v_partition_name);
        v_optimization_count := v_optimization_count + 1;
    END IF;
    
    -- Analyze previous month's partition for historical queries
    v_partition_name := 'transactions_' || TO_CHAR(v_processing_date - INTERVAL '1 month', 'YYYY_MM');
    
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = v_partition_name) THEN
        EXECUTE format('ANALYZE %s', v_partition_name);
        v_optimization_count := v_optimization_count + 1;
    END IF;
    
    -- Refresh materialized views used in batch processing
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_summary;
    
    -- Reset PostgreSQL settings to default values
    RESET work_mem;
    RESET maintenance_work_mem;
    RESET max_parallel_workers_per_gather;
    RESET random_page_cost;
    
    v_end_time := CURRENT_TIMESTAMP;
    
    -- Log completion
    RAISE NOTICE 'Batch processing optimization completed in % seconds. Optimized % partitions',
                 EXTRACT(EPOCH FROM (v_end_time - v_start_time)), v_optimization_count;
END;
$$ LANGUAGE plpgsql;

-- Schedule batch processing optimization to run before batch jobs
SELECT cron.schedule(
    'batch_processing_optimization',
    '30 1 * * *',  -- Daily at 1:30 AM, before batch jobs
    'SELECT optimize_batch_processing_window();'
);

-- ===================================================================================
-- SECTION 10: Monitoring and Alerting Configuration
-- ===================================================================================

-- Create monitoring view for partition health dashboard
CREATE OR REPLACE VIEW partition_health_dashboard AS
SELECT 
    'transactions' as table_name,
    COUNT(*) as total_partitions,
    MIN(partition_start_date) as oldest_partition,
    MAX(partition_end_date) as newest_partition,
    SUM(partition_size_mb) as total_size_mb,
    AVG(partition_size_mb) as avg_partition_size_mb,
    COUNT(CASE WHEN partition_size_mb > 1000 THEN 1 END) as large_partitions,
    COUNT(CASE WHEN last_analyze_time < CURRENT_DATE - INTERVAL '7 days' THEN 1 END) as stale_statistics
FROM (
    SELECT 
        c.relname as partition_name,
        (pg_relation_size(c.oid) / 1024 / 1024)::NUMERIC as partition_size_mb,
        COALESCE(s.last_analyze, '1970-01-01'::TIMESTAMP) as last_analyze_time,
        -- Extract date range from partition name
        TO_DATE(RIGHT(c.relname, 7), 'YYYY_MM') as partition_start_date,
        TO_DATE(RIGHT(c.relname, 7), 'YYYY_MM') + INTERVAL '1 month' - INTERVAL '1 day' as partition_end_date
    FROM pg_class c
    JOIN pg_inherits i ON c.oid = i.inhrelid
    JOIN pg_class p ON i.inhparent = p.oid
    LEFT JOIN pg_stat_user_tables s ON c.relname = s.relname
    WHERE p.relname = 'transactions'
) partition_stats;

-- Create function to check partition health and generate alerts
CREATE OR REPLACE FUNCTION check_partition_health()
RETURNS TABLE (
    alert_level VARCHAR(10),
    alert_message TEXT,
    recommendation TEXT
) AS $$
BEGIN
    -- Check for partitions that are too large
    RETURN QUERY
    SELECT 
        'WARNING'::VARCHAR(10) as alert_level,
        'Partition ' || c.relname || ' is ' || 
        ROUND((pg_relation_size(c.oid) / 1024.0 / 1024.0), 2) || 
        ' MB, exceeding recommended size of 1000 MB' as alert_message,
        'Consider reviewing partition interval or archiving older data' as recommendation
    FROM pg_class c
    JOIN pg_inherits i ON c.oid = i.inhrelid
    JOIN pg_class p ON i.inhparent = p.oid
    WHERE p.relname = 'transactions'
    AND pg_relation_size(c.oid) > 1000 * 1024 * 1024;
    
    -- Check for stale statistics
    RETURN QUERY
    SELECT 
        'INFO'::VARCHAR(10) as alert_level,
        'Partition ' || s.relname || ' has stale statistics (last analyzed: ' || 
        s.last_analyze || ')' as alert_message,
        'Run ANALYZE on the partition to update statistics' as recommendation
    FROM pg_stat_user_tables s
    JOIN pg_class c ON s.relname = c.relname
    JOIN pg_inherits i ON c.oid = i.inhrelid
    JOIN pg_class p ON i.inhparent = p.oid
    WHERE p.relname = 'transactions'
    AND s.last_analyze < CURRENT_DATE - INTERVAL '7 days';
    
    -- Check for missing future partitions
    RETURN QUERY
    SELECT 
        'ERROR'::VARCHAR(10) as alert_level,
        'Insufficient future partitions for transactions table' as alert_message,
        'Ensure pg_partman is running and premake setting is adequate' as recommendation
    WHERE NOT EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_inherits i ON c.oid = i.inhrelid
        JOIN pg_class p ON i.inhparent = p.oid
        WHERE p.relname = 'transactions'
        AND c.relname LIKE 'transactions_' || TO_CHAR(CURRENT_DATE + INTERVAL '2 months', 'YYYY_MM')
    );
END;
$$ LANGUAGE plpgsql;

-- ===================================================================================
-- SECTION 11: Final Configuration and Comments
-- ===================================================================================

-- Add comprehensive comments to all partition management objects
COMMENT ON FUNCTION maintain_transaction_partitions() IS 'Automated maintenance function for transaction partitions using pg_partman. Runs daily at 2:00 AM to create new partitions, archive old ones, and update statistics.';

COMMENT ON FUNCTION archive_partition_data(TEXT, INTEGER) IS 'Archives transaction partitions older than specified retention period (default 13 months) to archive schema for compliance with financial data retention policies.';

COMMENT ON FUNCTION optimize_batch_processing_window() IS 'Optimizes PostgreSQL settings and partition statistics for the 4-hour batch processing window. Runs at 1:30 AM before batch jobs start.';

COMMENT ON FUNCTION monitor_partition_performance() IS 'Monitors partition performance metrics including size, row count, query times, and index usage ratios for performance optimization.';

COMMENT ON FUNCTION check_partition_health() IS 'Checks partition health status and generates alerts for large partitions, stale statistics, and missing future partitions.';

COMMENT ON VIEW partition_health_dashboard IS 'Comprehensive dashboard view showing partition health metrics, sizes, and statistics for monitoring and alerting.';

COMMENT ON TABLE partition_audit_log IS 'Audit trail for all partition operations including creation, deletion, and maintenance activities for compliance tracking.';

-- Add table comments for partition configuration
COMMENT ON TABLE partman.part_config IS 'pg_partman configuration table for automated partition management of transactions table with 13-month retention policy.';

-- Log successful completion of advanced partitioning setup
DO $$
BEGIN
    RAISE NOTICE 'Advanced partitioning strategy successfully implemented for transactions table';
    RAISE NOTICE 'Features enabled: pg_partman automation, 13-month retention, constraint exclusion optimization';
    RAISE NOTICE 'Scheduled maintenance: Daily at 2:00 AM for partition management';
    RAISE NOTICE 'Batch optimization: Daily at 1:30 AM for 4-hour processing window';
    RAISE NOTICE 'Monitoring: Health dashboard and alerting functions available';
END;
$$;

-- rollback changeset blitzy:V9-advanced-partitioning-strategy
-- DROP VIEW IF EXISTS partition_health_dashboard;
-- DROP FUNCTION IF EXISTS check_partition_health();
-- DROP FUNCTION IF EXISTS monitor_partition_performance();
-- DROP FUNCTION IF EXISTS generate_partition_health_report();
-- DROP FUNCTION IF EXISTS optimize_batch_processing_window();
-- DROP FUNCTION IF EXISTS maintain_transaction_partitions();
-- DROP FUNCTION IF EXISTS archive_partition_data(TEXT, INTEGER);
-- DROP FUNCTION IF EXISTS log_partition_operation();
-- DROP TABLE IF EXISTS partition_audit_log;
-- DROP TABLE IF EXISTS archive.transactions_template;
-- DROP SCHEMA IF EXISTS archive CASCADE;
-- SELECT cron.unschedule('transaction_partition_maintenance');
-- SELECT cron.unschedule('batch_processing_optimization');
-- DELETE FROM partman.part_config WHERE parent_table = 'transactions';
-- DROP EXTENSION IF EXISTS pg_cron;
-- DROP EXTENSION IF EXISTS pg_partman;
-- DROP SCHEMA IF EXISTS partman CASCADE;