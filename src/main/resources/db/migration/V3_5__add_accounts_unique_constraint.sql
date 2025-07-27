/*
 * V3_5__add_accounts_unique_constraint.sql
 * 
 * CardDemo Database Migration Script V3.5
 * 
 * Adds unique constraint on (account_id, customer_id) combination in accounts table
 * to support foreign key references from cards table per V4 migration requirements.
 * 
 * This migration addresses the foreign key constraint error:
 * "ERROR: there is no unique constraint matching given keys for referenced table 'accounts'"
 * 
 * The constraint enables proper referential integrity for the cards table foreign key:
 * CONSTRAINT fk_cards_account_customer FOREIGN KEY (account_id, customer_id) REFERENCES accounts(account_id, customer_id)
 * 
 * Business Logic:
 * - Ensures account_id and customer_id combination is unique in accounts table
 * - Enables foreign key relationships from dependent tables (cards, transactions)
 * - Maintains data integrity for account-customer associations
 * - Supports COBOL-to-Java migration preserving VSAM key structures
 * 
 * Performance Impact:
 * - Creates supporting index for unique constraint verification
 * - Enables efficient foreign key validation for referenced tables
 * - Maintains query performance for account lookups by composite key
 * 
 * Security Considerations:
 * - Prevents duplicate account-customer associations
 * - Ensures referential integrity across related entities
 * - Maintains audit trail consistency through proper relationships
 * 
 * Author: Blitzy agent
 * Created: Database constraint migration for foreign key support (V3.5 ordering)
 * Version: 1.0 - Unique constraint addition for accounts table before V4 execution
 * =============================================================================
 */

--liquibase formatted sql

--changeset blitzy-agent:add-accounts-unique-constraint-v3-5
--comment: Add unique constraint on accounts (account_id, customer_id) to support V4 foreign key creation

-- Add unique constraint on account_id and customer_id combination
-- This constraint is required for foreign key references from cards table in V4 migration
ALTER TABLE accounts 
ADD CONSTRAINT uk_accounts_account_customer UNIQUE (account_id, customer_id);

-- Add comment for documentation and maintenance
COMMENT ON CONSTRAINT uk_accounts_account_customer ON accounts IS 
'Unique constraint on account_id and customer_id combination to support foreign key references from cards and other dependent tables. Ensures referential integrity while preserving COBOL-to-Java VSAM key structure equivalence.';

--rollback ALTER TABLE accounts DROP CONSTRAINT uk_accounts_account_customer;