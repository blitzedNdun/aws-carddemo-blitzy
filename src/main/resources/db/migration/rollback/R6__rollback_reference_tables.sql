-- ==============================================================================
-- Liquibase Rollback Script: R6__rollback_reference_tables.sql
-- Description: Comprehensive rollback script for V6__create_reference_tables.sql
--              Reverses creation of all reference tables, indexes, triggers, functions,
--              permissions, comments, and data for VSAM to PostgreSQL migration
-- Author: Blitzy agent
-- Version: 6.0-ROLLBACK
-- Migration Type: ROLLBACK - Complete reversal of reference tables creation
-- Target: V6__create_reference_tables.sql changeset rollback
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:rollback-reference-tables-permissions-v6
--comment: Revoke all permissions granted to application roles for reference tables

-- Revoke ALL PRIVILEGES from admin role for comprehensive reference data administration
REVOKE ALL PRIVILEGES ON transaction_category_balances FROM carddemo_admin_role;
REVOKE ALL PRIVILEGES ON disclosure_groups FROM carddemo_admin_role;
REVOKE ALL PRIVILEGES ON transaction_categories FROM carddemo_admin_role;
REVOKE ALL PRIVILEGES ON transaction_types FROM carddemo_admin_role;

-- Revoke full permissions from application write role for reference data management
REVOKE SELECT, INSERT, UPDATE, DELETE ON transaction_category_balances FROM carddemo_write_role;
REVOKE SELECT, INSERT, UPDATE, DELETE ON disclosure_groups FROM carddemo_write_role;
REVOKE SELECT, INSERT, UPDATE, DELETE ON transaction_categories FROM carddemo_write_role;
REVOKE SELECT, INSERT, UPDATE, DELETE ON transaction_types FROM carddemo_write_role;

-- Revoke SELECT permission from application read role for all reference tables
REVOKE SELECT ON transaction_category_balances FROM carddemo_read_role;
REVOKE SELECT ON disclosure_groups FROM carddemo_read_role;
REVOKE SELECT ON transaction_categories FROM carddemo_read_role;
REVOKE SELECT ON transaction_types FROM carddemo_read_role;

--changeset blitzy-agent:rollback-reference-tables-comments-v6
--comment: Remove comprehensive table and column documentation for reference tables

-- Remove column-level documentation for transaction_category_balances
COMMENT ON COLUMN transaction_category_balances.version_number IS NULL;
COMMENT ON COLUMN transaction_category_balances.category_balance IS NULL;
COMMENT ON COLUMN transaction_category_balances.transaction_category IS NULL;
COMMENT ON COLUMN transaction_category_balances.account_id IS NULL;

-- Remove column-level documentation for disclosure_groups
COMMENT ON COLUMN disclosure_groups.disclosure_text IS NULL;
COMMENT ON COLUMN disclosure_groups.interest_rate IS NULL;
COMMENT ON COLUMN disclosure_groups.group_id IS NULL;

-- Remove column-level documentation for transaction_categories
COMMENT ON COLUMN transaction_categories.category_description IS NULL;
COMMENT ON COLUMN transaction_categories.parent_transaction_type IS NULL;
COMMENT ON COLUMN transaction_categories.transaction_category IS NULL;

-- Remove column-level documentation for transaction_types
COMMENT ON COLUMN transaction_types.debit_credit_indicator IS NULL;
COMMENT ON COLUMN transaction_types.type_description IS NULL;
COMMENT ON COLUMN transaction_types.transaction_type IS NULL;

-- Remove table-level documentation
COMMENT ON TABLE transaction_category_balances IS NULL;
COMMENT ON TABLE disclosure_groups IS NULL;
COMMENT ON TABLE transaction_categories IS NULL;
COMMENT ON TABLE transaction_types IS NULL;

--changeset blitzy-agent:rollback-reference-tables-constraints-v6
--comment: Remove additional business constraints and validation rules for reference data integrity

-- Drop unique constraint for active disclosure group configurations
DROP INDEX IF EXISTS idx_disclosure_groups_active_unique;

-- Remove hierarchical naming constraint from transaction categories
ALTER TABLE transaction_categories DROP CONSTRAINT IF EXISTS chk_transaction_categories_hierarchy;

-- Remove disclosure groups rate consistency constraint
ALTER TABLE disclosure_groups DROP CONSTRAINT IF EXISTS chk_disclosure_groups_rate_consistency;

--changeset blitzy-agent:rollback-reference-tables-initial-data-v6
--comment: Remove initial data populated from ASCII source files

-- Clear disclosure_groups table data from discgrp.txt
DELETE FROM disclosure_groups;

-- Clear transaction_categories table data from trancatg.txt
DELETE FROM transaction_categories;

-- Clear transaction_types table data from trantype.txt
DELETE FROM transaction_types;

--changeset blitzy-agent:rollback-reference-tables-triggers-v6
--comment: Remove triggers for reference tables audit trail and automated timestamp management

-- Drop trigger for transaction_category_balances version control and audit
DROP TRIGGER IF EXISTS trg_tcatbal_version_update ON transaction_category_balances;

-- Drop triggers for automatic updated_at timestamp updates
DROP TRIGGER IF EXISTS trg_disclosure_groups_update_timestamp ON disclosure_groups;
DROP TRIGGER IF EXISTS trg_transaction_categories_update_timestamp ON transaction_categories;
DROP TRIGGER IF EXISTS trg_transaction_types_update_timestamp ON transaction_types;

-- Drop trigger functions for reference tables management
DROP FUNCTION IF EXISTS update_tcatbal_version_and_timestamp();
DROP FUNCTION IF EXISTS update_reference_tables_updated_at();

--changeset blitzy-agent:rollback-reference-tables-indexes-v6
--comment: Remove high-performance indexes for reference tables supporting sub-millisecond lookup operations

-- Drop indexes for transaction_category_balances table
DROP INDEX IF EXISTS idx_tcatbal_last_updated;
DROP INDEX IF EXISTS idx_tcatbal_category_balance;
DROP INDEX IF EXISTS idx_tcatbal_account_id;

-- Drop indexes for disclosure_groups table
DROP INDEX IF EXISTS idx_disclosure_groups_transaction_category;
DROP INDEX IF EXISTS idx_disclosure_groups_effective_date;
DROP INDEX IF EXISTS idx_disclosure_groups_interest_rate;

-- Drop indexes for transaction_categories table
DROP INDEX IF EXISTS idx_transaction_categories_active;
DROP INDEX IF EXISTS idx_transaction_categories_parent_type;

-- Drop indexes for transaction_types table
DROP INDEX IF EXISTS idx_transaction_types_debit_credit;
DROP INDEX IF EXISTS idx_transaction_types_active;

--changeset blitzy-agent:rollback-transaction-category-balances-table-v6
--comment: Remove transaction_category_balances table from tcatbal.txt with composite primary key for account-category balance tracking

-- Drop transaction_category_balances table (child table with foreign key dependencies)
-- CASCADE option ensures all dependent objects are removed
DROP TABLE IF EXISTS transaction_category_balances CASCADE;

--changeset blitzy-agent:rollback-disclosure-groups-table-v6
--comment: Remove disclosure_groups table from discgrp.txt with precise interest rate management and legal disclosure text

-- Drop disclosure_groups table (child table with foreign key to transaction_categories)
-- CASCADE option ensures all dependent objects are removed
DROP TABLE IF EXISTS disclosure_groups CASCADE;

--changeset blitzy-agent:rollback-transaction-categories-table-v6
--comment: Remove transaction_categories table from trancatg.txt with 4-character category codes and hierarchical support

-- Drop transaction_categories table (child table with foreign key to transaction_types)
-- CASCADE option ensures all dependent objects are removed
DROP TABLE IF EXISTS transaction_categories CASCADE;

--changeset blitzy-agent:rollback-transaction-types-table-v6
--comment: Remove transaction_types reference table from trantype.txt with 2-character type codes and debit/credit classification

-- Drop transaction_types table (parent table, no foreign key dependencies)
-- CASCADE option ensures all dependent objects are removed
DROP TABLE IF EXISTS transaction_types CASCADE;

-- ==============================================================================
-- ROLLBACK VERIFICATION QUERIES
-- ==============================================================================
-- These queries can be used to verify complete rollback of reference tables:
--
-- Verify all tables are removed:
-- SELECT table_name FROM information_schema.tables 
-- WHERE table_name IN ('transaction_types', 'transaction_categories', 'disclosure_groups', 'transaction_category_balances');
--
-- Verify all indexes are removed:
-- SELECT indexname FROM pg_indexes 
-- WHERE indexname LIKE 'idx_%transaction%' OR indexname LIKE 'idx_%disclosure%' OR indexname LIKE 'idx_%tcatbal%';
--
-- Verify all functions are removed:
-- SELECT routine_name FROM information_schema.routines 
-- WHERE routine_name LIKE '%reference_tables%' OR routine_name LIKE '%tcatbal%';
--
-- Verify all triggers are removed:
-- SELECT trigger_name FROM information_schema.triggers 
-- WHERE trigger_name LIKE '%transaction%' OR trigger_name LIKE '%disclosure%' OR trigger_name LIKE '%tcatbal%';
-- ==============================================================================