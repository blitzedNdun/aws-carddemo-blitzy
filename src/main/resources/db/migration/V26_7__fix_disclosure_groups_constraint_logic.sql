-- ==============================================================================
-- Liquibase Migration: V26_7__fix_disclosure_groups_constraint_logic.sql
-- Description: Fix disclosure groups rate consistency constraint with simplified logic
-- Author: Blitzy agent
-- Version: 26.7
-- Migration Type: SCHEMA MODIFICATION
-- Purpose: Replace overly complex constraint with simple business rule validation
-- ==============================================================================

--comment: Fix disclosure groups rate consistency constraint with simplified business logic

-- Step 1: Drop the existing constraint that's causing issues
ALTER TABLE disclosure_groups DROP CONSTRAINT IF EXISTS chk_disclosure_groups_rate_consistency;

-- Step 2: Create a much simpler constraint that allows:
-- - Zero rates for payment transactions (transaction_type_prefix = '02')
-- - Zero rates for credit transactions (transaction_type_prefix = '03') 
-- - Zero rates for ZEROAPR groups (any transaction type)
-- - Positive rates for all other scenarios
ALTER TABLE disclosure_groups 
ADD CONSTRAINT chk_disclosure_groups_rate_consistency 
CHECK (
    interest_rate >= 0.0000 AND
    interest_rate <= 9.9999 AND
    (
        -- Allow zero rates for these specific cases
        (interest_rate = 0.0000 AND 
         (group_id = 'ZEROAPR' OR 
          transaction_type_prefix IN ('02', '03'))
        ) OR
        -- Allow positive rates for any scenario
        (interest_rate > 0.0000)
    )
);

-- Add comprehensive documentation
COMMENT ON CONSTRAINT chk_disclosure_groups_rate_consistency ON disclosure_groups IS 
'Simplified rate consistency constraint: (1) Interest rates must be between 0.0000 and 9.9999, (2) Zero rates are allowed for ZEROAPR groups (promotional accounts), payment transactions (02xxx), and credit transactions (03xxx), (3) Positive rates are allowed for all scenarios. This supports the business logic where payments and credits do not accrue interest charges.';