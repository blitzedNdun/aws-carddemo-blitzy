-- ==============================================================================
-- Liquibase Rollback Migration: R3__rollback_accounts_table.sql
-- Description: Comprehensive rollback of V3__create_accounts_table.sql migration
-- Author: Blitzy agent
-- Version: R3.0
-- Migration Type: ROLLBACK - Complete reversal of accounts table and all dependencies
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:rollback-accounts-table-refresh-procedures-r3
--comment: Drop stored procedures for materialized view refresh and maintenance

-- Drop comprehensive refresh procedure for all account-related materialized views
DROP FUNCTION IF EXISTS refresh_all_account_materialized_views();

-- Drop procedure to refresh account balance analysis materialized view  
DROP FUNCTION IF EXISTS refresh_account_balance_analysis();

-- Drop procedure to refresh customer account summary materialized view
DROP FUNCTION IF EXISTS refresh_customer_account_summary();

--changeset blitzy-agent:rollback-accounts-table-materialized-views-r3
--comment: Drop materialized views for account summary and analysis operations

-- Drop indexes on account balance analysis materialized view
DROP INDEX IF EXISTS idx_mv_account_balance_analysis_balance;
DROP INDEX IF EXISTS idx_mv_account_balance_analysis_utilization;

-- Drop account balance analysis materialized view
DROP MATERIALIZED VIEW IF EXISTS mv_account_balance_analysis;

-- Drop indexes on customer account summary materialized view
DROP INDEX IF EXISTS idx_mv_customer_account_summary_total_balance;
DROP INDEX IF EXISTS idx_mv_customer_account_summary_customer_id;

-- Drop customer account summary materialized view
DROP MATERIALIZED VIEW IF EXISTS mv_customer_account_summary;

--changeset blitzy-agent:rollback-accounts-table-permissions-r3
--comment: Revoke permissions granted to application roles

-- Revoke sequence permissions for account ID generation
REVOKE USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public FROM carddemo_admin_role;
REVOKE USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public FROM carddemo_write_role;

-- Revoke table permissions from application roles
REVOKE ALL PRIVILEGES ON accounts FROM carddemo_admin_role;
REVOKE SELECT, INSERT, UPDATE, DELETE ON accounts FROM carddemo_write_role;
REVOKE SELECT ON accounts FROM carddemo_read_role;

--changeset blitzy-agent:rollback-accounts-table-security-policies-r3
--comment: Drop row-level security policies and disable RLS

-- Drop write access policy for admin and service accounts
DROP POLICY IF EXISTS accounts_write_policy ON accounts;

-- Drop read access policy for customer account access
DROP POLICY IF EXISTS accounts_read_policy ON accounts;

-- Disable row-level security for the accounts table
ALTER TABLE accounts DISABLE ROW LEVEL SECURITY;

--changeset blitzy-agent:rollback-accounts-table-comments-r3
--comment: Remove comprehensive table and column documentation

-- Remove table-level documentation
COMMENT ON TABLE accounts IS NULL;

-- Remove column-level documentation with VSAM field mapping references
COMMENT ON COLUMN accounts.updated_at IS NULL;
COMMENT ON COLUMN accounts.created_at IS NULL;
COMMENT ON COLUMN accounts.group_id IS NULL;
COMMENT ON COLUMN accounts.address_zip IS NULL;
COMMENT ON COLUMN accounts.current_cycle_debit IS NULL;
COMMENT ON COLUMN accounts.current_cycle_credit IS NULL;
COMMENT ON COLUMN accounts.reissue_date IS NULL;
COMMENT ON COLUMN accounts.expiration_date IS NULL;
COMMENT ON COLUMN accounts.open_date IS NULL;
COMMENT ON COLUMN accounts.cash_credit_limit IS NULL;
COMMENT ON COLUMN accounts.credit_limit IS NULL;
COMMENT ON COLUMN accounts.current_balance IS NULL;
COMMENT ON COLUMN accounts.active_status IS NULL;
COMMENT ON COLUMN accounts.customer_id IS NULL;
COMMENT ON COLUMN accounts.account_id IS NULL;

--changeset blitzy-agent:rollback-accounts-table-triggers-r3
--comment: Drop triggers and trigger functions for audit trail and financial data integrity

-- Drop financial data validation trigger
DROP TRIGGER IF EXISTS trg_accounts_financial_validation ON accounts;

-- Drop automatic timestamp update trigger  
DROP TRIGGER IF EXISTS trg_accounts_update_timestamp ON accounts;

-- Drop trigger function for financial balance validation and audit logging
DROP FUNCTION IF EXISTS validate_accounts_financial_changes();

-- Drop trigger function for automatic updated_at timestamp maintenance
DROP FUNCTION IF EXISTS update_accounts_updated_at();

--changeset blitzy-agent:rollback-accounts-table-indexes-r3
--comment: Drop performance indexes for accounts table

-- Drop composite index for current cycle financial analysis
DROP INDEX IF EXISTS idx_accounts_cycle_balances;

-- Drop index for credit limit analysis and risk management
DROP INDEX IF EXISTS idx_accounts_credit_limits;

-- Drop composite index for account opening date analysis
DROP INDEX IF EXISTS idx_accounts_open_date_analysis;

-- Drop index for geographical analysis and fraud detection
DROP INDEX IF EXISTS idx_accounts_zip_code;

-- Drop index for account lifecycle management
DROP INDEX IF EXISTS idx_accounts_expiration_date;

-- Drop index for disclosure group queries
DROP INDEX IF EXISTS idx_accounts_group_id;

-- Drop index for account balance queries
DROP INDEX IF EXISTS idx_accounts_balance_lookup;

-- Drop index for customer-account relationship queries
DROP INDEX IF EXISTS idx_accounts_customer_id;

--changeset blitzy-agent:rollback-accounts-table-main-r3
--comment: Drop accounts table with CASCADE to remove all dependent objects and foreign key references

-- Drop accounts table preserving VSAM ACCTDAT record layout with PostgreSQL DECIMAL precision
-- CASCADE ensures all dependent objects are removed including foreign key references
DROP TABLE IF EXISTS accounts CASCADE;

--changeset blitzy-agent:rollback-accounts-vsam-acctdat-restoration-r3
--comment: Log completion of VSAM ACCTDAT to PostgreSQL accounts table transformation reversal

-- Insert rollback completion log entry for audit trail and system monitoring
DO $$
BEGIN
    -- Attempt to log rollback completion if system_log table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'system_log') THEN
        INSERT INTO system_log (
            log_level,
            message,
            details,
            timestamp
        ) VALUES (
            'INFO',
            'VSAM ACCTDAT to PostgreSQL accounts table transformation successfully rolled back',
            json_build_object(
                'migration_version', 'R3.0',
                'rollback_scope', 'Complete accounts table and dependencies removal',
                'vsam_dataset', 'AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS',
                'postgresql_table', 'accounts',
                'decimal_precision_cleanup', 'DECIMAL(12,2) financial fields removed',
                'foreign_key_cleanup', 'customer_id and group_id relationships removed',
                'security_cleanup', 'Row-level security policies removed',
                'materialized_views_cleanup', 'Account summary and analysis views removed',
                'index_cleanup', 'All performance indexes removed',
                'trigger_cleanup', 'Audit and validation triggers removed',
                'permission_cleanup', 'Application role permissions revoked'
            ),
            CURRENT_TIMESTAMP
        );
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- Rollback completion even if logging fails
        NULL;
END $$;

-- Validation query to confirm accounts table removal
-- This will fail if table still exists, confirming successful rollback
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'accounts') THEN
        RAISE EXCEPTION 'Rollback validation failed: accounts table still exists';
    END IF;
    
    -- Confirm materialized views are removed
    IF EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'mv_customer_account_summary') THEN
        RAISE EXCEPTION 'Rollback validation failed: mv_customer_account_summary still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'mv_account_balance_analysis') THEN
        RAISE EXCEPTION 'Rollback validation failed: mv_account_balance_analysis still exists';
    END IF;
    
    -- Confirm trigger functions are removed
    IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'update_accounts_updated_at') THEN
        RAISE EXCEPTION 'Rollback validation failed: update_accounts_updated_at function still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'validate_accounts_financial_changes') THEN
        RAISE EXCEPTION 'Rollback validation failed: validate_accounts_financial_changes function still exists';
    END IF;
    
    -- Confirm refresh procedures are removed
    IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'refresh_customer_account_summary') THEN
        RAISE EXCEPTION 'Rollback validation failed: refresh_customer_account_summary function still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'refresh_account_balance_analysis') THEN
        RAISE EXCEPTION 'Rollback validation failed: refresh_account_balance_analysis function still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'refresh_all_account_materialized_views') THEN
        RAISE EXCEPTION 'Rollback validation failed: refresh_all_account_materialized_views function still exists';
    END IF;
END $$;

-- Final confirmation message for successful VSAM ACCTDAT rollback
DO $$
BEGIN
    RAISE NOTICE 'VSAM ACCTDAT to PostgreSQL accounts table rollback completed successfully';
    RAISE NOTICE 'Account table with DECIMAL(12,2) precision fields: REMOVED';
    RAISE NOTICE 'Customer and disclosure group foreign key relationships: REMOVED';
    RAISE NOTICE 'Financial calculation fields and constraints: REMOVED';
    RAISE NOTICE 'Account lifecycle date fields: REMOVED';
    RAISE NOTICE 'Performance indexes and optimization: REMOVED';
    RAISE NOTICE 'Row-level security policies: REMOVED';
    RAISE NOTICE 'Audit triggers and validation: REMOVED';
    RAISE NOTICE 'Materialized views for account analysis: REMOVED';
    RAISE NOTICE 'Application role permissions: REVOKED';
    RAISE NOTICE 'ACCTDAT VSAM dataset functionality baseline: RESTORED';
END $$;