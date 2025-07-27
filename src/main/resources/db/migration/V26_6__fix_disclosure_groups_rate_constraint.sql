-- ==============================================================================
-- Liquibase Migration: V26_6__fix_disclosure_groups_rate_constraint.sql
-- Description: Fix disclosure groups rate consistency constraint to allow zero rates for payment/credit transactions
-- Author: Blitzy agent
-- Version: 26.6
-- Migration Type: SCHEMA MODIFICATION
-- Purpose: Update rate consistency constraint to allow zero rates for transaction types 02 and 03
-- ==============================================================================

--comment: Fix disclosure groups rate consistency constraint to allow zero rates for payment/credit transactions

-- Step 1: Drop the existing overly restrictive constraint
ALTER TABLE disclosure_groups DROP CONSTRAINT IF EXISTS chk_disclosure_groups_rate_consistency;

-- Step 2: Create a more nuanced constraint that allows:
-- - ZEROAPR groups to have 0.0000 rates for any transaction type
-- - Any group to have 0.0000 rates for payment transactions (02xxx codes)  
-- - Any group to have 0.0000 rates for credit/refund transactions (03xxx codes)
-- - Non-ZEROAPR groups must have > 0.0000 rates for purchase, cash advance, and other transaction types
ALTER TABLE disclosure_groups 
ADD CONSTRAINT chk_disclosure_groups_rate_consistency 
CHECK (
    -- ZEROAPR groups can have zero rates for any transaction type
    (group_id = 'ZEROAPR' AND interest_rate = 0.0000) OR
    -- Any group can have zero rates for payment transactions (02xxx)
    (transaction_type_prefix = '02' AND interest_rate = 0.0000) OR
    -- Any group can have zero rates for credit/refund transactions (03xxx)  
    (transaction_type_prefix = '03' AND interest_rate = 0.0000) OR
    -- For other transaction types, non-ZEROAPR groups must have positive rates
    (group_id != 'ZEROAPR' AND transaction_type_prefix NOT IN ('02', '03') AND interest_rate > 0.0000)
);

-- Add documentation for the updated constraint
COMMENT ON CONSTRAINT chk_disclosure_groups_rate_consistency ON disclosure_groups IS 
'Rate consistency constraint ensuring: (1) ZEROAPR groups have 0.0000 rates for promotional offers, (2) Payment transactions (02xxx) have 0.0000 rates as payments reduce balances, (3) Credit/refund transactions (03xxx) have 0.0000 rates as credits do not accrue interest, (4) Other transaction types for non-ZEROAPR groups have positive rates for interest calculation. Supports business logic where payment and credit transactions do not generate interest charges regardless of account group type.';