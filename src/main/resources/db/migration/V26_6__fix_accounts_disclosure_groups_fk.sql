-- ==============================================================================
-- Liquibase Migration: V26_6__fix_accounts_disclosure_groups_fk.sql
-- Description: Fix accounts table foreign key to disclosure_groups after schema changes
-- Author: Blitzy agent
-- Version: 26.6
-- Migration Type: SCHEMA MODIFICATION
-- Purpose: Update foreign key constraints to work with new disclosure_groups schema
-- ==============================================================================

--comment: Fix accounts table foreign key constraint for disclosure_groups

-- Step 1: Drop the existing foreign key constraint
ALTER TABLE accounts DROP CONSTRAINT fk_accounts_disclosure_group;

-- Step 2: Create a view for default disclosure groups that accounts can reference
-- This view shows one record per group_id using the default transaction type (01) and category (0001)
CREATE OR REPLACE VIEW v_disclosure_groups_defaults AS
SELECT 
    group_id,
    transaction_type_prefix,
    transaction_category,
    interest_rate,
    disclosure_text,
    effective_date,
    active_status,
    created_at,
    updated_at
FROM disclosure_groups 
WHERE transaction_type_prefix = '01' 
AND transaction_category = '0001'
AND active_status = true;

-- Step 3: Foreign key constraint cannot be recreated because disclosure_groups 
-- no longer has unique constraint on group_id alone. Instead, we rely on the
-- validation trigger below to ensure referential integrity.

-- Step 4: Create a function to validate the foreign key reference
-- This ensures the referenced group_id has a default configuration
CREATE OR REPLACE FUNCTION validate_accounts_disclosure_group_reference()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if the referenced group_id has a default configuration
    IF NOT EXISTS (
        SELECT 1 FROM disclosure_groups 
        WHERE group_id = NEW.group_id 
        AND transaction_type_prefix = '01' 
        AND transaction_category = '0001'
        AND active_status = true
    ) THEN
        RAISE EXCEPTION 'Invalid group_id reference: No default disclosure group configuration found for group_id %', NEW.group_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 5: Create trigger to validate accounts foreign key references
CREATE TRIGGER trg_accounts_disclosure_group_validation
    BEFORE INSERT OR UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION validate_accounts_disclosure_group_reference();

-- Add documentation for the new view
COMMENT ON VIEW v_disclosure_groups_defaults IS 'Default disclosure group configurations for accounts table foreign key references. Shows one record per group_id using transaction_type_prefix=01 and transaction_category=0001 as the default configuration for account association and interest rate determination.';

-- Add documentation for the validation function
COMMENT ON FUNCTION validate_accounts_disclosure_group_reference() IS 'Validation function ensuring accounts table group_id references have corresponding default disclosure group configurations. Prevents orphaned account records and ensures interest rate calculation integrity.';