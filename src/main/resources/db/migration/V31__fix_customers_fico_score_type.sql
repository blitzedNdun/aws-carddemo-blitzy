-- =============================================================================
-- Liquibase Migration: V31__fix_customers_fico_score_type.sql
-- 
-- SUMMARY: Fixes customers table fico_credit_score column type to match JPA entity expectations
--          Changes NUMERIC(3) to INTEGER to align with Customer.java entity definition
--
-- CHANGES:
--   - Change fico_credit_score column type from NUMERIC(3) to INTEGER
--   - Preserve existing check constraints for FICO score range validation (300-850)
--   - Maintain all data during type conversion
--
-- DEPENDENCIES: 
--   - V2__create_customers_table.sql (customers table must exist)
--
-- AUTHOR: Blitzy agent - CardDemo JPA Entity Alignment
-- =============================================================================

--liquibase formatted sql

--changeset blitzy:fix-customers-fico-score-type
--comment: Fix customers table fico_credit_score column type for JPA entity alignment

-- Change fico_credit_score column type from NUMERIC(3) to INTEGER
-- PostgreSQL will automatically convert NUMERIC(3) values to INTEGER
ALTER TABLE customers ALTER COLUMN fico_credit_score TYPE INTEGER;

-- Update column comment to reflect new type
COMMENT ON COLUMN customers.fico_credit_score IS 
    'FICO credit score with industry standard range validation (300-850). Integer type for JPA entity alignment.';

--rollback ALTER TABLE customers ALTER COLUMN fico_credit_score TYPE NUMERIC(3);

--comment: Customers table fico_credit_score column type fixed to match JPA entity expectations

-- =============================================================================
-- SUCCESS CONFIRMATION
-- =============================================================================

SELECT 'CardDemo Migration V31: Customers table fico_credit_score type fixed:' AS status
UNION ALL
SELECT '  ✓ Changed fico_credit_score from NUMERIC(3) to INTEGER'
UNION ALL  
SELECT '  ✓ Preserved existing data during type conversion'
UNION ALL
SELECT '  ✓ Maintained FICO score range validation constraints (300-850)'
UNION ALL
SELECT '  ✓ JPA entity Customer.java alignment completed';

--comment: Customers table fico_credit_score type alignment completed successfully