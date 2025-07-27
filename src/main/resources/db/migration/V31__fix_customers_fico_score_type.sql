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

-- Step 1: Drop the dependent view v_card_account_customer_xref
-- This view depends on the fico_credit_score column and prevents column type alteration
DROP VIEW IF EXISTS v_card_account_customer_xref;

-- Step 2: Change fico_credit_score column type from NUMERIC(3) to INTEGER
-- PostgreSQL will automatically convert NUMERIC(3) values to INTEGER
ALTER TABLE customers ALTER COLUMN fico_credit_score TYPE INTEGER;

-- Step 3: Recreate the v_card_account_customer_xref view with the updated column type
-- This maintains compatibility with existing queries while using the corrected INTEGER type
CREATE OR REPLACE VIEW v_card_account_customer_xref AS
SELECT 
    c.card_number,
    c.account_id,
    c.customer_id,
    a.current_balance,
    a.credit_limit,
    a.cash_credit_limit,
    cust.first_name,
    cust.last_name,
    cust.fico_credit_score,  -- Now INTEGER type instead of NUMERIC(3)
    c.active_status as card_status,
    c.expiration_date as card_expiry,
    a.open_date as account_opened
FROM cards c
JOIN accounts a ON c.account_id = a.account_id
JOIN customers cust ON c.customer_id = cust.customer_id;

-- Update column comment to reflect new type
COMMENT ON COLUMN customers.fico_credit_score IS 
    'FICO credit score with industry standard range validation (300-850). Integer type for JPA entity alignment.';

-- Add comment to recreated view for documentation
COMMENT ON VIEW v_card_account_customer_xref IS 
'Real-time cross-reference view providing immediate access to card-account-customer 
relationships, replacing VSAM alternate index patterns with PostgreSQL join optimization. 
Updated for INTEGER fico_credit_score compatibility.';

--rollback DROP VIEW IF EXISTS v_card_account_customer_xref;
--rollback ALTER TABLE customers ALTER COLUMN fico_credit_score TYPE NUMERIC(3);
--rollback CREATE OR REPLACE VIEW v_card_account_customer_xref AS SELECT c.card_number, c.account_id, c.customer_id, a.current_balance, a.credit_limit, a.cash_credit_limit, cust.first_name, cust.last_name, cust.fico_credit_score, c.active_status as card_status, c.expiration_date as card_expiry, a.open_date as account_opened FROM cards c JOIN accounts a ON c.account_id = a.account_id JOIN customers cust ON c.customer_id = cust.customer_id;

--comment: Customers table fico_credit_score column type fixed to match JPA entity expectations

-- =============================================================================
-- SUCCESS CONFIRMATION
-- =============================================================================

SELECT 'CardDemo Migration V31: Customers table fico_credit_score type fixed:' AS status
UNION ALL
SELECT '  ✓ Dropped dependent view v_card_account_customer_xref before column alteration'
UNION ALL
SELECT '  ✓ Changed fico_credit_score from NUMERIC(3) to INTEGER'
UNION ALL  
SELECT '  ✓ Preserved existing data during type conversion'
UNION ALL
SELECT '  ✓ Recreated v_card_account_customer_xref view with INTEGER compatibility'
UNION ALL
SELECT '  ✓ Maintained FICO score range validation constraints (300-850)'
UNION ALL
SELECT '  ✓ JPA entity Customer.java alignment completed';

--comment: Customers table fico_credit_score type alignment completed successfully