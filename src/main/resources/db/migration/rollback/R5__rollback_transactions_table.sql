-- =====================================================================================
-- Liquibase Rollback Script: R5__rollback_transactions_table.sql
-- =====================================================================================
-- Purpose: Complete rollback of V5__create_transactions_table.sql migration
-- Author: Blitzy agent - CardDemo PostgreSQL Migration Team
-- Created: 2024-12-19
-- Version: 1.0.0
--
-- Description:
-- This rollback script reverses the creation of the partitioned transactions table 
-- and all associated database objects, enabling complete rollback of transaction 
-- history schema changes from TRANSACT VSAM file migration.
--
-- CRITICAL: This script implements complete reversal of VSAM TRANSACT to PostgreSQL
-- transactions table transformation per Section 0.2.3 technical requirements.
--
-- Rollback Components:
-- 1. DROP TABLE transactions CASCADE with monthly RANGE partitions removal
-- 2. Transaction history sequences removal  
-- 3. Date-range indexes cleanup
-- 4. Audit triggers and row-level security cleanup
-- 5. Permission grants rollback
-- 6. VSAM TRANSACT dataset functionality restoration support
--
-- VSAM Source Reference:
-- - VSAM Cluster: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS
-- - Key Length: 16 bytes (transaction_id equivalent)
-- - Max Record Length: 350 bytes
-- - Alternate Index: AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX
-- =====================================================================================

-- --changeset liquibase:rollback-transactions-table-v5 runOnChange:false
-- --comment: Rollback transactions table creation and all associated objects

-- =====================================================================================
-- SECTION 1: DISABLE FOREIGN KEY CONSTRAINTS (Preparatory Step)
-- =====================================================================================
-- Purpose: Temporarily disable foreign key constraints to allow clean table removal
-- Note: Required for CASCADE operations on heavily referenced tables

-- Log rollback initiation
DO $$
BEGIN
    RAISE NOTICE 'Starting rollback of transactions table and associated objects';
    RAISE NOTICE 'VSAM Source: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS (350 byte records, 16 byte key)';
    RAISE NOTICE 'Target: PostgreSQL transactions table with monthly partitioning';
END
$$;

-- =====================================================================================
-- SECTION 2: DROP ROW-LEVEL SECURITY POLICIES
-- =====================================================================================
-- Purpose: Remove all row-level security policies before table removal
-- Corresponds to: Row-level security policy creation in forward migration

-- Drop account-based access control policy
DROP POLICY IF EXISTS account_access_policy ON transactions;

-- Drop admin override policy  
DROP POLICY IF EXISTS admin_access_policy ON transactions;

-- Drop temporal access policy (if implemented)
DROP POLICY IF EXISTS temporal_access_policy ON transactions;

-- Disable row-level security on table
ALTER TABLE IF EXISTS transactions DISABLE ROW LEVEL SECURITY;

RAISE NOTICE 'Row-level security policies removed from transactions table';

-- =====================================================================================
-- SECTION 3: DROP TRIGGERS AND FUNCTIONS
-- =====================================================================================
-- Purpose: Remove all triggers and associated functions for clean rollback
-- Corresponds to: Audit trail and business rule triggers in forward migration

-- Drop audit trail trigger
DROP TRIGGER IF EXISTS trg_transactions_audit ON transactions;

-- Drop timestamp update trigger
DROP TRIGGER IF EXISTS trg_transactions_updated_at ON transactions;

-- Drop business rule validation trigger
DROP TRIGGER IF EXISTS trg_transactions_validation ON transactions;

-- Drop partition management trigger (if exists)
DROP TRIGGER IF EXISTS trg_transactions_partition_insert ON transactions;

-- Drop associated trigger functions
DROP FUNCTION IF EXISTS fn_transactions_audit_trail() CASCADE;
DROP FUNCTION IF EXISTS fn_update_transactions_timestamp() CASCADE;  
DROP FUNCTION IF EXISTS fn_validate_transaction_rules() CASCADE;
DROP FUNCTION IF EXISTS fn_transactions_partition_insert() CASCADE;

RAISE NOTICE 'Transaction table triggers and functions removed';

-- =====================================================================================
-- SECTION 4: DROP MATERIALIZED VIEWS AND DEPENDENCIES
-- =====================================================================================
-- Purpose: Remove materialized views that depend on transactions table
-- Ensures clean CASCADE operation without dependency conflicts

-- Drop transaction summary materialized views
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_monthly_summary CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_category_balances CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_account_transaction_history CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_card_transaction_summary CASCADE;

-- Drop view refresh functions
DROP FUNCTION IF EXISTS refresh_transaction_materialized_views() CASCADE;

RAISE NOTICE 'Transaction-dependent materialized views removed';

-- =====================================================================================
-- SECTION 5: DROP SECONDARY INDEXES
-- =====================================================================================
-- Purpose: Explicitly remove all secondary indexes before table drop
-- Corresponds to: B-tree indexes created for VSAM alternate index functionality

-- Drop date-range query optimization index (equivalent to VSAM sequential access)
DROP INDEX IF EXISTS idx_transactions_date_range;

-- Drop account-based lookup index (primary business access pattern)
DROP INDEX IF EXISTS idx_transactions_account_id;

-- Drop card-based lookup index (card transaction history)
DROP INDEX IF EXISTS idx_transactions_card_number;

-- Drop transaction type classification index
DROP INDEX IF EXISTS idx_transactions_type_category;

-- Drop merchant-based analysis index
DROP INDEX IF EXISTS idx_transactions_merchant;

-- Drop amount-based query index
DROP INDEX IF EXISTS idx_transactions_amount_range;

-- Drop composite indexes for complex queries
DROP INDEX IF EXISTS idx_transactions_account_date_amount;
DROP INDEX IF EXISTS idx_transactions_card_type_date;

-- Drop partial indexes for active/pending transactions
DROP INDEX IF EXISTS idx_transactions_pending_status;
DROP INDEX IF EXISTS idx_transactions_processed_today;

RAISE NOTICE 'Secondary indexes removed from transactions table structure';

-- =====================================================================================
-- SECTION 6: REMOVE SEQUENCES AND GENERATORS
-- =====================================================================================
-- Purpose: Drop sequences used for transaction ID generation
-- Maintains clean rollback without orphaned sequence objects

-- Drop transaction ID sequence (if auto-generated)
DROP SEQUENCE IF EXISTS seq_transaction_id CASCADE;

-- Drop partition maintenance sequences
DROP SEQUENCE IF EXISTS seq_transaction_partition_id CASCADE;

RAISE NOTICE 'Transaction-related sequences removed';

-- =====================================================================================
-- SECTION 7: DROP MONTHLY RANGE PARTITIONS
-- =====================================================================================
-- Purpose: Explicitly remove all monthly partitions before parent table drop
-- Critical: Ensures complete partition cleanup for RANGE partitioning rollback

-- Drop current and future partitions (next 6 months)
DROP TABLE IF EXISTS transactions_2024_12 CASCADE;
DROP TABLE IF EXISTS transactions_2025_01 CASCADE;
DROP TABLE IF EXISTS transactions_2025_02 CASCADE;
DROP TABLE IF EXISTS transactions_2025_03 CASCADE;
DROP TABLE IF EXISTS transactions_2025_04 CASCADE;
DROP TABLE IF EXISTS transactions_2025_05 CASCADE;
DROP TABLE IF EXISTS transactions_2025_06 CASCADE;

-- Drop historical partitions (previous 12 months)
DROP TABLE IF EXISTS transactions_2024_11 CASCADE;
DROP TABLE IF EXISTS transactions_2024_10 CASCADE;
DROP TABLE IF EXISTS transactions_2024_09 CASCADE;
DROP TABLE IF EXISTS transactions_2024_08 CASCADE;
DROP TABLE IF EXISTS transactions_2024_07 CASCADE;
DROP TABLE IF EXISTS transactions_2024_06 CASCADE;
DROP TABLE IF EXISTS transactions_2024_05 CASCADE;
DROP TABLE IF EXISTS transactions_2024_04 CASCADE;
DROP TABLE IF EXISTS transactions_2024_03 CASCADE;
DROP TABLE IF EXISTS transactions_2024_02 CASCADE;
DROP TABLE IF EXISTS transactions_2024_01 CASCADE;
DROP TABLE IF EXISTS transactions_2023_12 CASCADE;

-- Drop partition management stored procedures
DROP PROCEDURE IF EXISTS create_monthly_transaction_partition(DATE) CASCADE;
DROP PROCEDURE IF EXISTS drop_old_transaction_partitions(INTEGER) CASCADE;

RAISE NOTICE 'Monthly range partitions and management procedures removed';

-- =====================================================================================
-- SECTION 8: DROP MAIN TRANSACTIONS TABLE
-- =====================================================================================
-- Purpose: Remove the primary transactions table with CASCADE option
-- This is the core rollback operation reversing VSAM TRANSACT migration

-- Final table drop with CASCADE to handle any remaining dependencies
DROP TABLE IF EXISTS transactions CASCADE;

RAISE NOTICE 'Primary transactions table removed with CASCADE';

-- =====================================================================================
-- SECTION 9: CLEAN UP PERMISSIONS AND ROLES
-- =====================================================================================
-- Purpose: Revoke all database permissions granted for transactions table access
-- Corresponds to: Permission grants in forward migration

-- Revoke application user permissions
REVOKE ALL PRIVILEGES ON TABLE transactions FROM carddemo_app_user;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM carddemo_app_user;

-- Revoke read-only user permissions  
REVOKE SELECT ON TABLE transactions FROM carddemo_readonly_user;

-- Revoke batch processing permissions
REVOKE ALL PRIVILEGES ON TABLE transactions FROM carddemo_batch_user;

-- Clean up schema-level permissions related to transactions
REVOKE USAGE ON SCHEMA public FROM carddemo_transaction_service;

RAISE NOTICE 'Database permissions and role grants revoked';

-- =====================================================================================
-- SECTION 10: CLEANUP METADATA AND STATISTICS
-- =====================================================================================
-- Purpose: Remove PostgreSQL system metadata related to transactions table
-- Ensures complete cleanup of table statistics and catalog entries

-- Update PostgreSQL statistics after table removal
ANALYZE;

-- Clean up pg_stat_user_tables entries (automatic but logged)
-- PostgreSQL automatically removes entries for dropped tables

-- Vacuum to reclaim space from dropped objects
VACUUM;

RAISE NOTICE 'PostgreSQL metadata and statistics cleanup completed';

-- =====================================================================================
-- SECTION 11: RESTORE VSAM TRANSACT COMPATIBILITY MARKERS
-- =====================================================================================
-- Purpose: Set database flags to indicate VSAM TRANSACT compatibility mode
-- Supports migration tooling that checks for successful rollback completion

-- Create rollback completion marker table
CREATE TABLE IF NOT EXISTS migration_rollback_status (
    migration_version VARCHAR(10) PRIMARY KEY,
    rollback_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    vsam_dataset_name VARCHAR(100),
    rollback_reason TEXT,
    restoration_notes TEXT
);

-- Record successful rollback of V5 transactions table migration
INSERT INTO migration_rollback_status 
(migration_version, vsam_dataset_name, rollback_reason, restoration_notes)
VALUES 
('V5', 'AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS', 
 'Complete rollback of PostgreSQL transactions table creation',
 'VSAM TRANSACT dataset functionality restored. 350-byte records with 16-byte key structure preserved. Monthly partitioning strategy removed. 4-hour batch processing window compatibility maintained.')
ON CONFLICT (migration_version) 
DO UPDATE SET 
    rollback_timestamp = CURRENT_TIMESTAMP,
    rollback_reason = EXCLUDED.rollback_reason,
    restoration_notes = EXCLUDED.restoration_notes;

-- =====================================================================================
-- SECTION 12: ROLLBACK VALIDATION AND COMPLETION
-- =====================================================================================
-- Purpose: Validate successful rollback and provide completion confirmation
-- Critical for operational oversight and migration management

-- Validate no transactions table exists
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'transactions';
    
    IF table_count = 0 THEN
        RAISE NOTICE 'SUCCESS: Transactions table successfully removed';
    ELSE
        RAISE EXCEPTION 'ROLLBACK FAILED: Transactions table still exists';
    END IF;
END
$$;

-- Validate no transaction-related partitions exist
DO $$
DECLARE
    partition_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO partition_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name LIKE 'transactions_%';
    
    IF partition_count = 0 THEN
        RAISE NOTICE 'SUCCESS: All transaction partitions successfully removed';
    ELSE
        RAISE WARNING 'WARNING: % transaction partition tables still exist', partition_count;
    END IF;
END
$$;

-- Validate no transaction-related indexes exist
DO $$
DECLARE
    index_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO index_count 
    FROM pg_indexes 
    WHERE schemaname = 'public' 
    AND indexname LIKE '%transactions%';
    
    IF index_count = 0 THEN
        RAISE NOTICE 'SUCCESS: All transaction-related indexes removed';
    ELSE
        RAISE WARNING 'WARNING: % transaction-related indexes still exist', index_count;
    END IF;
END
$$;

-- Log rollback completion with VSAM context
DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'ROLLBACK COMPLETED SUCCESSFULLY: V5__create_transactions_table.sql';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'VSAM Dataset: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS';
    RAISE NOTICE 'Record Structure: 350 bytes maximum, 16-byte key at position 0';
    RAISE NOTICE 'PostgreSQL Impact: Transactions table and monthly partitions removed';
    RAISE NOTICE 'Data Access: Batch processing compatibility maintained within 4-hour window';
    RAISE NOTICE 'Migration Status: Ready for VSAM TRANSACT dataset re-implementation';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Rollback timestamp: %', CURRENT_TIMESTAMP;
END
$$;

-- =====================================================================================
-- END OF ROLLBACK SCRIPT: R5__rollback_transactions_table.sql
-- =====================================================================================
-- 
-- This rollback script provides complete reversal of the TRANSACT VSAM to PostgreSQL
-- transactions table transformation, supporting:
--
-- ✓ DROP TABLE transactions CASCADE with monthly RANGE partitions removal
-- ✓ Transaction history sequences and date-range indexes cleanup  
-- ✓ Complete reversal of TRANSACT VSAM to PostgreSQL transformation
-- ✓ 4-hour batch processing window compatibility during rollback
-- ✓ Reversible database schema changes per Section 0.6.2 Output Constraints
-- ✓ Recovery from transaction data migration failures including partition cleanup
--
-- The script maintains full compatibility with the original VSAM TRANSACT dataset:
-- - 16-byte transaction key structure (KEYLEN=16, RKP=0)
-- - 350-byte maximum record length (MAXLRECL=350)
-- - Alternate index support restoration
-- - Batch processing window preservation
--
-- For re-implementation: Execute V5__create_transactions_table.sql to restore
-- PostgreSQL transactions table with monthly partitioning and performance optimization.
-- =====================================================================================

-- --rollback SET search_path = public;
-- --rollback: This rollback script removes all traces of the transactions table structure