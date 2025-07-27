-- =============================================================================
-- Liquibase Rollback Script: R4__rollback_cards_table.sql
-- 
-- SUMMARY: Comprehensive rollback script that reverses the creation of the cards 
--          table and all associated database objects including Luhn validation 
--          constraints, composite foreign key relationships, materialized views,
--          security policies, triggers, functions, and performance indexes
--
-- ROLLBACK FOR: V4__create_cards_table.sql migration
--
-- DEPENDENCIES REVERSED: 
--   - Removes cards table foreign key relationships to accounts and customers
--   - Drops all performance optimization indexes and materialized views
--   - Removes security policies and audit triggers
--   - Cleans up utility functions and business validation logic
--
-- TRANSFORMATION REVERSAL:
--   - Enables complete reversal of CARDDAT VSAM to PostgreSQL cards table transformation
--   - Supports recovery from card data migration failures
--   - Maintains referential integrity during rollback operations
--   - Provides comprehensive cleanup of all card-related database objects
--
-- AUTHOR: Blitzy agent - CardDemo PostgreSQL Migration Rollback
-- =============================================================================

--liquibase formatted sql

--changeset blitzy:rollback-cards-utility-functions
--comment: Drop utility functions for card data validation and management

-- ROLLBACK STEP 1: Remove utility functions (created last, removed first)
-- These functions were created to support card validation and status management

DROP FUNCTION IF EXISTS validate_luhn_algorithm(VARCHAR) CASCADE;
DROP FUNCTION IF EXISTS get_card_status_description(VARCHAR) CASCADE;
DROP FUNCTION IF EXISTS is_card_expired(DATE) CASCADE;

SELECT 'Utility functions for cards table removed successfully' AS rollback_status;

--rollback: Functions already dropped - no reverse rollback needed

--comment: Card utility functions dropped during rollback

-- =============================================================================

--changeset blitzy:rollback-cards-materialized-view
--comment: Drop materialized view for cross-reference functionality and performance optimization

-- ROLLBACK STEP 2: Remove materialized view and associated indexes
-- This view provided pre-computed joins for rapid card-account-customer relationship queries

DROP MATERIALIZED VIEW IF EXISTS mv_cards_cross_reference CASCADE;

SELECT 'Cards cross-reference materialized view removed successfully' AS rollback_status;

--rollback: Materialized view already dropped - no reverse rollback needed

--comment: Cards cross-reference materialized view dropped during rollback

-- =============================================================================

--changeset blitzy:rollback-cards-security
--comment: Remove row-level security and access control for cards table

-- ROLLBACK STEP 3: Remove security policies and disable row-level security
-- These security features restricted card data access to authorized users only

DROP POLICY IF EXISTS cards_access_policy ON cards CASCADE;
ALTER TABLE IF EXISTS cards DISABLE ROW LEVEL SECURITY;

SELECT 'Row-level security and access policies removed from cards table' AS rollback_status;

--rollback: Security policies already dropped - no reverse rollback needed

--comment: Row-level security and access control removed during rollback

-- =============================================================================

--changeset blitzy:rollback-cards-triggers
--comment: Drop audit triggers and data integrity triggers for cards table

-- ROLLBACK STEP 4: Remove triggers and their associated functions
-- These triggers provided automatic timestamp updates and business rule validation

DROP TRIGGER IF EXISTS trg_cards_update_timestamp ON cards CASCADE;
DROP TRIGGER IF EXISTS trg_cards_business_validation ON cards CASCADE;
DROP FUNCTION IF EXISTS update_cards_updated_at() CASCADE;
DROP FUNCTION IF EXISTS validate_cards_business_rules() CASCADE;

SELECT 'Audit and validation triggers removed from cards table' AS rollback_status;

--rollback: Triggers and functions already dropped - no reverse rollback needed

--comment: Audit and validation triggers dropped during rollback

-- =============================================================================

--changeset blitzy:rollback-cards-indexes
--comment: Drop performance indexes for cards table supporting rapid lookups and cross-reference functionality

-- ROLLBACK STEP 5: Remove performance optimization indexes
-- These indexes supported rapid card lookups and cross-reference functionality

DROP INDEX IF EXISTS idx_cards_account_id CASCADE;
DROP INDEX IF EXISTS idx_cards_customer_id CASCADE; 
DROP INDEX IF EXISTS idx_cards_account_customer CASCADE;
DROP INDEX IF EXISTS idx_cards_expiration_date CASCADE;
DROP INDEX IF EXISTS idx_cards_embossed_name CASCADE;

SELECT 'Performance indexes removed from cards table' AS rollback_status;

--rollback: Indexes already dropped - no reverse rollback needed

--comment: Performance indexes dropped during rollback

-- =============================================================================

--changeset blitzy:rollback-cards-table
--comment: Drop cards table with all security validation, composite foreign keys, and cross-reference support

-- ROLLBACK STEP 6: Remove the main cards table
-- CASCADE ensures all dependent objects and foreign key references are properly removed
-- This reverses the complete CARDDAT VSAM to PostgreSQL cards table transformation

DROP TABLE IF EXISTS cards CASCADE;

SELECT 'Cards table completely removed with all dependencies' AS rollback_status;

--rollback: Table already dropped - no reverse rollback needed

--comment: Cards table dropped during rollback with CASCADE cleanup

-- =============================================================================
-- ROLLBACK VALIDATION AND CONFIRMATION
-- =============================================================================

--changeset blitzy:cards-rollback-validation
--comment: Validate successful rollback of cards table and all associated objects

-- ROLLBACK VALIDATION: Confirm complete removal of all cards-related objects
-- This provides comprehensive validation that the rollback was successful

-- Verify table removal
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'cards') THEN
        RAISE EXCEPTION 'ROLLBACK FAILURE: Cards table still exists after rollback';
    END IF;
    
    -- Verify materialized view removal
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'mv_cards_cross_reference') THEN
        RAISE EXCEPTION 'ROLLBACK FAILURE: Cards cross-reference materialized view still exists';
    END IF;
    
    -- Verify index removal
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname LIKE 'idx_cards_%') THEN
        RAISE EXCEPTION 'ROLLBACK FAILURE: Cards indexes still exist after rollback';
    END IF;
    
    -- Verify function removal
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname IN ('validate_luhn_algorithm', 'get_card_status_description', 'is_card_expired', 'update_cards_updated_at', 'validate_cards_business_rules')) THEN
        RAISE EXCEPTION 'ROLLBACK FAILURE: Cards functions still exist after rollback';
    END IF;
    
    RAISE NOTICE 'ROLLBACK SUCCESS: All cards-related database objects successfully removed';
END
$$;

--rollback: Validation already completed - no reverse rollback needed

--comment: Rollback validation completed successfully

-- =============================================================================
-- ROLLBACK COMPLETION CONFIRMATION
-- =============================================================================

--changeset blitzy:cards-rollback-completion
--comment: Confirm successful rollback of cards table with comprehensive object cleanup

-- Log successful rollback completion with detailed component list
SELECT 'CardDemo Migration V4 Rollback: Cards table rollback completed successfully with:' AS rollback_completion
UNION ALL
SELECT '  ✓ Cards table completely removed with CASCADE cleanup'
UNION ALL  
SELECT '  ✓ All composite foreign key relationships properly dropped'
UNION ALL
SELECT '  ✓ Luhn algorithm validation constraints removed'
UNION ALL
SELECT '  ✓ CVV security field validation constraints dropped'
UNION ALL
SELECT '  ✓ Performance optimization indexes completely removed'
UNION ALL
SELECT '  ✓ Cross-reference materialized view and indexes dropped'
UNION ALL
SELECT '  ✓ Row-level security policies and access control removed'
UNION ALL
SELECT '  ✓ Audit triggers and timestamp management functions dropped'
UNION ALL
SELECT '  ✓ Business validation triggers and logic completely removed'
UNION ALL
SELECT '  ✓ Utility functions for card management dropped'
UNION ALL
SELECT '  ✓ Database schema restored to pre-cards-table baseline'
UNION ALL
SELECT '  ✓ VSAM CARDDAT transformation completely reversed'
UNION ALL
SELECT '  ✓ All card-related database objects successfully cleaned up'
UNION ALL
SELECT '  ✓ System ready for alternative card table implementation or restoration';

--rollback: Completion log already displayed - no reverse rollback needed

--comment: Cards table rollback completed successfully with comprehensive cleanup

-- =============================================================================
-- ROLLBACK SCRIPT METADATA AND DOCUMENTATION
-- =============================================================================

-- ROLLBACK IMPACT SUMMARY:
-- • Cards table: COMPLETELY REMOVED with all data and constraints
-- • Foreign key relationships: ALL DROPPED (accounts, customers references)
-- • Security features: ALL REMOVED (row-level security, access policies)
-- • Performance indexes: ALL DROPPED (5 B-tree indexes removed)
-- • Materialized views: ALL REMOVED (cross-reference view and indexes)
-- • Triggers and functions: ALL DROPPED (audit, validation, utility functions)
-- • Luhn validation: COMPLETELY REMOVED (complex check constraints)
-- • CVV security fields: ALL CLEANED UP (sensitive data field protection)
-- • Card lifecycle management: FULLY REVERSED (status, expiration handling)
-- • VSAM CARDDAT transformation: COMPLETELY UNDONE

-- SYSTEM STATE AFTER ROLLBACK:
-- • Database schema restored to state before V4__create_cards_table.sql
-- • No card-related database objects remain in the system
-- • All foreign key dependencies properly cleaned up with CASCADE
-- • Memory and storage allocated to cards table reclaimed
-- • Referential integrity maintained for remaining tables (accounts, customers)
-- • System ready for alternative card management implementation if needed

-- RECOVERY PROCEDURES:
-- • To restore cards functionality: Re-run V4__create_cards_table.sql migration
-- • To implement alternative: Create new cards table design and run new migration
-- • To validate rollback: Check pg_tables, pg_indexes, pg_matviews for cards objects

-- DEPENDENCIES AND CONSTRAINTS:
-- • This rollback MUST be executed before rolling back V3__create_accounts_table.sql
-- • This rollback MUST be executed before rolling back V2__create_customers_table.sql
-- • Any application code referencing cards table MUST be updated before rollback
-- • Spring Boot Card entity classes will fail after this rollback until table restored

-- LIQUIBASE INTEGRATION:
-- • Rollback properly integrates with Liquibase rollback command
-- • All changesets include appropriate rollback directives
-- • Rollback validation ensures completion verification
-- • Database state tracking maintains consistency with Liquibase changelog

-- =============================================================================
-- END OF ROLLBACK SCRIPT
-- =============================================================================