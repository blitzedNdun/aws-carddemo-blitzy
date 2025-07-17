-- =====================================================================================
-- Liquibase Rollback Script: R2__rollback_customers_table.sql
-- Description: Comprehensive rollback script that reverses V2__create_customers_table.sql
--              Removes customers table and all associated database objects including
--              materialized views, indexes, triggers, and functions created during
--              CUSTDAT VSAM to PostgreSQL customers table transformation
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 2.0
-- =====================================================================================

-- rollback changeset blitzy:V2-create-customers-table
-- comment: Complete rollback of customers table creation and all associated database objects

-- =====================================================================================
-- STEP 1: Drop materialized view indexes (must be dropped before materialized view)
-- =====================================================================================

-- Drop indexes on materialized view mv_customer_summary
DROP INDEX IF EXISTS idx_mv_customer_summary_primary_phone;
DROP INDEX IF EXISTS idx_mv_customer_summary_age;
DROP INDEX IF EXISTS idx_mv_customer_summary_credit_rating;
DROP INDEX IF EXISTS idx_mv_customer_summary_full_name;

-- =====================================================================================
-- STEP 2: Drop materialized view and associated function
-- =====================================================================================

-- Drop customer summary materialized view (created for cross-reference queries)
DROP MATERIALIZED VIEW IF EXISTS mv_customer_summary CASCADE;

-- Drop function for refreshing materialized view
DROP FUNCTION IF EXISTS refresh_customer_summary_view();

-- =====================================================================================
-- STEP 3: Drop table triggers and trigger functions
-- =====================================================================================

-- Drop trigger for automatic updated_at timestamp updates
DROP TRIGGER IF EXISTS customers_updated_at_trigger ON customers;

-- Drop trigger function for updating updated_at timestamp
DROP FUNCTION IF EXISTS update_customers_updated_at();

-- =====================================================================================
-- STEP 4: Drop main customers table with CASCADE to remove all constraints
-- =====================================================================================

-- Drop customers table and all dependent objects (indexes, constraints, etc.)
-- CASCADE ensures all foreign key relationships and dependent objects are removed
-- This reverses the complete CUSTDAT VSAM to PostgreSQL transformation
DROP TABLE IF EXISTS customers CASCADE;

-- =====================================================================================
-- ROLLBACK VALIDATION AND CLEANUP
-- =====================================================================================

-- Verify all customer-related database objects have been removed
-- The following objects should no longer exist after this rollback:
--
-- Tables:
--   - customers (main customer data table)
--
-- Materialized Views:
--   - mv_customer_summary (customer summary view for reporting)
--
-- Indexes:
--   - idx_customers_ssn
--   - idx_customers_name  
--   - idx_customers_address_zip
--   - idx_customers_fico_score
--   - idx_customers_date_of_birth
--   - idx_mv_customer_summary_full_name
--   - idx_mv_customer_summary_credit_rating
--   - idx_mv_customer_summary_age
--   - idx_mv_customer_summary_primary_phone
--
-- Check Constraints:
--   - customers_customer_id_check
--   - customers_ssn_check
--   - customers_government_id_check
--   - customers_fico_score_check
--   - customers_address_state_check
--   - customers_address_country_check
--   - customers_address_zip_check
--   - customers_phone_home_check
--   - customers_phone_work_check
--   - customers_date_of_birth_check
--   - customers_first_name_not_empty
--   - customers_last_name_not_empty
--   - customers_address_line_1_not_empty
--
-- Triggers:
--   - customers_updated_at_trigger
--
-- Functions:
--   - update_customers_updated_at()
--   - refresh_customer_summary_view()
--
-- Comments:
--   - All table and column comments for customers table
--
-- PII Protection Features:
--   - SSN field encryption requirements
--   - Government ID field protection
--   - Normalized address structure
--   - FICO credit score validation (300-850 range)
--
-- This rollback script provides complete reversal of the CUSTDAT VSAM dataset
-- migration to PostgreSQL customers table, restoring the database to its state
-- prior to the V2 migration and enabling recovery from customer data migration
-- failures while maintaining referential integrity during rollback operations.

-- =====================================================================================
-- ROLLBACK COMPLETION CONFIRMATION
-- =====================================================================================

-- Log rollback completion for audit purposes
-- Note: In production environments, consider adding explicit logging to a
-- migration audit table to track rollback operations

-- End of rollback script - customers table and all associated objects removed