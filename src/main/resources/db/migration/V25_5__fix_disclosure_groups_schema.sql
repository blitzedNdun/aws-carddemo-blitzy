-- ==============================================================================
-- Liquibase Migration: V26_5__fix_disclosure_groups_schema.sql
-- Description: Fix disclosure_groups table schema to properly accommodate 
--              multiple transaction type prefixes per group and category
-- Author: Blitzy agent
-- Version: 26.5
-- Migration Type: SCHEMA MODIFICATION
-- Purpose: Resolve constraint violations in V26 data load by restructuring table
-- ==============================================================================

--comment: Fix disclosure_groups table schema to support transaction type prefixes

-- Step 1: Add transaction_type_prefix column to distinguish different transaction types
-- within same group and category (01xxx, 02xxx, 03xxx, etc.)
ALTER TABLE disclosure_groups 
ADD COLUMN IF NOT EXISTS transaction_type_prefix VARCHAR(2);

-- Step 2: Drop the existing primary key constraint
ALTER TABLE disclosure_groups DROP CONSTRAINT pk_disclosure_groups;

-- Step 3: Drop the problematic unique constraint on group_id alone
-- This was preventing multiple records per group_id
ALTER TABLE disclosure_groups DROP CONSTRAINT uk_disclosure_groups_group_id;

-- Step 4: Create new composite primary key including transaction_type_prefix
-- This allows multiple records per (group_id, transaction_category) with different prefixes
ALTER TABLE disclosure_groups 
ADD CONSTRAINT pk_disclosure_groups_v2 
PRIMARY KEY (group_id, transaction_type_prefix, transaction_category);

-- Step 5: Add validation constraint for transaction_type_prefix format
ALTER TABLE disclosure_groups 
ADD CONSTRAINT chk_transaction_type_prefix_format 
CHECK (transaction_type_prefix ~ '^[0-9]{2}$');

-- Step 6: Create partial unique index for accounts table foreign key support
-- This allows accounts table to reference disclosure_groups by group_id
-- by ensuring there's always a default record (transaction_type_prefix = '01')
CREATE UNIQUE INDEX idx_disclosure_groups_default_group 
ON disclosure_groups (group_id) 
WHERE transaction_type_prefix = '01' AND transaction_category = '0001';

-- Step 7: Update NOT NULL constraint for the new column
ALTER TABLE disclosure_groups 
ALTER COLUMN transaction_type_prefix SET NOT NULL;

-- Update any existing data to have a default transaction_type_prefix
-- (This handles the case where the table might already have some data)
UPDATE disclosure_groups 
SET transaction_type_prefix = '01' 
WHERE transaction_type_prefix IS NULL;

-- Add documentation for the new column
COMMENT ON COLUMN disclosure_groups.transaction_type_prefix IS 'Transaction type prefix from discgrp.txt source data (01, 02, 03, etc.). Distinguishes different transaction processing contexts within the same group_id and transaction_category combination. Used in composite primary key to ensure uniqueness across all transaction type contexts for comprehensive interest rate management.';

-- Update table documentation to reflect the new schema
COMMENT ON TABLE disclosure_groups IS 'Disclosure groups reference data with transaction type prefix support. Manages interest rate configurations across multiple transaction processing contexts (purchase, payment, credit, authorization, etc.) within each group and category combination. Supports composite primary key (group_id, transaction_type_prefix, transaction_category) for comprehensive financial product configuration and regulatory compliance requirements.';