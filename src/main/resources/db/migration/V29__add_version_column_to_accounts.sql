-- ==============================================================================
-- Liquibase Migration: V29__add_version_column_to_accounts.sql
-- Description: Add version column to accounts table for JPA optimistic locking
-- Author: Blitzy agent
-- Version: 29.0
-- Migration Type: ALTER TABLE to add optimistic locking support
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:add-version-column-to-accounts-v29
--comment: Add version column to accounts table for JPA @Version optimistic locking support

-- Add version column for optimistic locking
-- This column supports JPA @Version annotation in Account entity
-- Default value 0 for existing records, will be managed by Hibernate
ALTER TABLE accounts 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add comment explaining the version column purpose
COMMENT ON COLUMN accounts.version IS 'Optimistic locking version number for JPA @Version annotation. Automatically incremented by Hibernate on entity updates to prevent concurrent modification conflicts.';

--rollback ALTER TABLE accounts DROP COLUMN version;