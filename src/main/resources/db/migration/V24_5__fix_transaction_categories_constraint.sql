-- ==============================================================================
-- Liquibase Migration: V24_5__fix_transaction_categories_constraint.sql
-- Description: Remove problematic unique constraint on transaction_category alone
--              to allow same category codes across different parent transaction types
-- Author: Blitzy agent
-- Version: 24.5
-- Migration Type: SCHEMA MODIFICATION
-- Purpose: Fix constraint preventing valid business data in V25 migration
-- ==============================================================================

--comment: Remove unique constraint on transaction_category to allow same category codes across different parent types

-- Drop the problematic unique constraint that prevents same category codes
-- across different parent transaction types (e.g., '0001' under both '01' and '02')
ALTER TABLE transaction_categories 
DROP CONSTRAINT IF EXISTS uk_transaction_categories_category;

-- The primary key constraint (transaction_category, parent_transaction_type) 
-- remains to ensure proper uniqueness within each parent type group
-- This allows:
-- - ('0001', '01', 'Regular Sales Draft')
-- - ('0001', '02', 'Cash payment') 
-- - ('0001', '03', 'Credit to Account')
-- etc.