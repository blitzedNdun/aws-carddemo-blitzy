-- =============================================================================
-- Liquibase Migration: V30__fix_cards_table_columns.sql
-- 
-- SUMMARY: Fixes cards table column names to match JPA entity expectations
--          and adds missing version column for optimistic locking
--
-- CHANGES:
--   - Rename created_at to created_date to match Card.java entity
--   - Rename updated_at to modified_date to match Card.java entity  
--   - Add version column for JPA optimistic locking (@Version annotation)
--
-- DEPENDENCIES: 
--   - V4__create_cards_table.sql (cards table must exist)
--
-- AUTHOR: Blitzy agent - CardDemo JPA Entity Alignment
-- =============================================================================

--liquibase formatted sql

--changeset blitzy:fix-cards-table-columns
--comment: Fix cards table column names and add version column for JPA entity alignment

-- Add version column for JPA optimistic locking
ALTER TABLE cards ADD COLUMN version BIGINT NOT NULL DEFAULT 1;

-- Rename created_at to created_date to match JPA entity
ALTER TABLE cards RENAME COLUMN created_at TO created_date;

-- Rename updated_at to modified_date to match JPA entity
ALTER TABLE cards RENAME COLUMN updated_at TO modified_date;

-- Update trigger function to use new column name
CREATE OR REPLACE FUNCTION update_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified_date = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Update column comments to reflect new names
COMMENT ON COLUMN cards.created_date IS 
    'Audit field - Timestamp when card record was created. Automatically populated on INSERT. Used for audit trail and compliance reporting.';

COMMENT ON COLUMN cards.modified_date IS 
    'Audit field - Timestamp when card record was last modified. Automatically updated on UPDATE via trigger. Used for audit trail and change tracking.';

COMMENT ON COLUMN cards.version IS 
    'Version field for JPA optimistic locking. Automatically incremented on each update to prevent concurrent modification conflicts.';

--rollback ALTER TABLE cards DROP COLUMN version;
--rollback ALTER TABLE cards RENAME COLUMN created_date TO created_at;
--rollback ALTER TABLE cards RENAME COLUMN modified_date TO updated_at;

--comment: Cards table columns fixed to match JPA entity expectations

-- =============================================================================
-- SUCCESS CONFIRMATION
-- =============================================================================

SELECT 'CardDemo Migration V30: Cards table columns fixed:' AS status
UNION ALL
SELECT '  ✓ Added version column for JPA optimistic locking'
UNION ALL  
SELECT '  ✓ Renamed created_at to created_date'
UNION ALL
SELECT '  ✓ Renamed updated_at to modified_date'
UNION ALL
SELECT '  ✓ Updated trigger function for new column names'
UNION ALL
SELECT '  ✓ JPA entity Card.java alignment completed';

--comment: Cards table column alignment completed successfully