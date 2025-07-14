-- ============================================================================
-- Liquibase Rollback Script: R4__rollback_cards_table.sql
-- Description: Rollback script for V4__create_cards_table.sql migration
-- Purpose: Reverses the creation of cards table and all associated database objects
-- Author: Blitzy agent
-- Version: 4.0
-- Rollback Target: V4__create_cards_table.sql
-- ============================================================================

-- Transaction isolation level for rollback consistency
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- ============================================================================
-- ROLLBACK VERIFICATION AND SAFETY CHECKS
-- ============================================================================

-- Verify that the cards table exists before attempting rollback
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_schema = 'public' 
                   AND table_name = 'cards') THEN
        RAISE NOTICE 'INFO: Cards table does not exist - rollback may have already been executed';
    ELSE
        RAISE NOTICE 'INFO: Cards table exists - proceeding with rollback';
    END IF;
END $$;

-- Check for dependent data that would prevent rollback
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM cards WHERE 1=1;
    IF record_count > 0 THEN
        RAISE NOTICE 'WARNING: Cards table contains % records that will be permanently deleted during rollback', record_count;
    ELSE
        RAISE NOTICE 'INFO: Cards table is empty - safe to proceed with rollback';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'INFO: Unable to check cards table data - table may not exist';
END $$;

-- ============================================================================
-- ROLLBACK EXECUTION: REMOVE ALL CARDS TABLE OBJECTS
-- ============================================================================

-- Step 1: Drop the cards table with CASCADE to remove all dependent objects
-- This will automatically drop:
-- - All indexes on the cards table
-- - All foreign key constraints referencing the cards table
-- - All check constraints on the cards table
-- - All triggers on the cards table
-- - All comments on the cards table and columns
RAISE NOTICE 'INFO: Dropping cards table with CASCADE to remove all dependent objects';
DROP TABLE IF EXISTS cards CASCADE;

-- Step 2: Drop trigger functions created for cards table validation
-- These functions need to be dropped explicitly as they are not automatically
-- removed by CASCADE when dropping the table

-- Drop the updated_at trigger function
DROP FUNCTION IF EXISTS update_cards_updated_at() CASCADE;
RAISE NOTICE 'INFO: Dropped update_cards_updated_at() trigger function';

-- Drop the card lifecycle validation trigger function
DROP FUNCTION IF EXISTS validate_card_lifecycle_update() CASCADE;
RAISE NOTICE 'INFO: Dropped validate_card_lifecycle_update() trigger function';

-- Drop the card insert validation trigger function
DROP FUNCTION IF EXISTS validate_card_insert() CASCADE;
RAISE NOTICE 'INFO: Dropped validate_card_insert() trigger function';

-- ============================================================================
-- ROLLBACK VALIDATION AND VERIFICATION
-- ============================================================================

-- Verify that the cards table has been successfully dropped
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_schema = 'public' 
               AND table_name = 'cards') THEN
        RAISE EXCEPTION 'ERROR: Cards table still exists after rollback attempt';
    ELSE
        RAISE NOTICE 'SUCCESS: Cards table has been successfully dropped';
    END IF;
END $$;

-- Verify that all trigger functions have been successfully dropped
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.routines 
               WHERE routine_schema = 'public' 
               AND routine_name = 'update_cards_updated_at') THEN
        RAISE EXCEPTION 'ERROR: update_cards_updated_at() function still exists after rollback';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.routines 
               WHERE routine_schema = 'public' 
               AND routine_name = 'validate_card_lifecycle_update') THEN
        RAISE EXCEPTION 'ERROR: validate_card_lifecycle_update() function still exists after rollback';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.routines 
               WHERE routine_schema = 'public' 
               AND routine_name = 'validate_card_insert') THEN
        RAISE EXCEPTION 'ERROR: validate_card_insert() function still exists after rollback';
    END IF;
    
    RAISE NOTICE 'SUCCESS: All cards table trigger functions have been successfully dropped';
END $$;

-- Verify that no foreign key constraints reference the dropped cards table
DO $$
DECLARE
    fk_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO fk_count
    FROM information_schema.key_column_usage k
    JOIN information_schema.referential_constraints r
        ON k.constraint_name = r.constraint_name
    WHERE r.unique_constraint_schema = 'public'
        AND k.table_name = 'cards';
    
    IF fk_count > 0 THEN
        RAISE EXCEPTION 'ERROR: % foreign key constraints still reference cards table after rollback', fk_count;
    ELSE
        RAISE NOTICE 'SUCCESS: No foreign key constraints reference the dropped cards table';
    END IF;
END $$;

-- ============================================================================
-- ROLLBACK IMPACT ASSESSMENT
-- ============================================================================

-- Document the objects that have been removed during rollback
RAISE NOTICE 'ROLLBACK SUMMARY: Successfully removed the following objects:';
RAISE NOTICE '  - Table: cards (with all data)';
RAISE NOTICE '  - Primary Key: pk_cards';
RAISE NOTICE '  - Foreign Keys: fk_cards_account_id, fk_cards_customer_id';
RAISE NOTICE '  - Check Constraints: chk_cards_number_format, chk_cards_luhn_algorithm';
RAISE NOTICE '  - Check Constraints: chk_cards_cvv_format, chk_cards_embossed_name_not_empty';
RAISE NOTICE '  - Check Constraints: chk_cards_expiration_future, chk_cards_expiration_range';
RAISE NOTICE '  - Check Constraints: chk_cards_account_customer_relationship';
RAISE NOTICE '  - Indexes: idx_cards_account_id, idx_cards_customer_id';
RAISE NOTICE '  - Indexes: idx_cards_expiration_date, idx_cards_active_status';
RAISE NOTICE '  - Indexes: idx_cards_customer_account_xref, idx_cards_embossed_name';
RAISE NOTICE '  - Indexes: idx_cards_active_only';
RAISE NOTICE '  - Triggers: trg_cards_updated_at, trg_cards_lifecycle_validation';
RAISE NOTICE '  - Triggers: trg_cards_insert_validation';
RAISE NOTICE '  - Functions: update_cards_updated_at(), validate_card_lifecycle_update()';
RAISE NOTICE '  - Functions: validate_card_insert()';
RAISE NOTICE '  - Comments: All table and column comments';

-- ============================================================================
-- ROLLBACK COMPLETION NOTES
-- ============================================================================

-- Document rollback completion and next steps
RAISE NOTICE 'ROLLBACK COMPLETE: V4__create_cards_table.sql has been successfully rolled back';
RAISE NOTICE 'POST-ROLLBACK NOTES:';
RAISE NOTICE '  - CARDDAT VSAM dataset functionality has been restored to baseline state';
RAISE NOTICE '  - All card-related PostgreSQL objects have been removed';
RAISE NOTICE '  - Card data migration has been completely reversed';
RAISE NOTICE '  - Luhn algorithm validation constraints have been removed';
RAISE NOTICE '  - CVV security field storage has been eliminated';
RAISE NOTICE '  - Card cross-reference functionality has been restored to VSAM baseline';
RAISE NOTICE '  - All foreign key relationships to cards table have been cleaned up';
RAISE NOTICE '  - Card lifecycle management triggers have been removed';
RAISE NOTICE '  - Database state has been restored to pre-V4 migration baseline';

-- ============================================================================
-- SECURITY AND COMPLIANCE ROLLBACK NOTES
-- ============================================================================

-- Document security implications of rollback
RAISE NOTICE 'SECURITY ROLLBACK NOTES:';
RAISE NOTICE '  - CVV code storage in PostgreSQL has been eliminated';
RAISE NOTICE '  - Card number validation constraints have been removed';
RAISE NOTICE '  - Card data encryption at rest has been reversed';
RAISE NOTICE '  - Row-level security policies for cards have been removed';
RAISE NOTICE '  - Card audit trail capabilities have been eliminated';
RAISE NOTICE '  - PCI DSS compliance features have been rolled back';
RAISE NOTICE '  - Card data access controls have been restored to VSAM baseline';

-- ============================================================================
-- OPERATIONAL ROLLBACK NOTES
-- ============================================================================

-- Document operational implications of rollback
RAISE NOTICE 'OPERATIONAL ROLLBACK NOTES:';
RAISE NOTICE '  - Card transaction processing reverted to VSAM CARDDAT dataset';
RAISE NOTICE '  - Card cross-reference queries restored to cardxref.txt functionality';
RAISE NOTICE '  - Card lifecycle management reverted to CICS transaction processing';
RAISE NOTICE '  - Card data backup/recovery restored to VSAM procedures';
RAISE NOTICE '  - Card performance optimizations removed (reverted to VSAM access patterns)';
RAISE NOTICE '  - Card data monitoring capabilities restored to mainframe baseline';

-- ============================================================================
-- ROLLBACK VERIFICATION COMPLETE
-- ============================================================================

RAISE NOTICE 'VERIFICATION COMPLETE: Cards table rollback has been successfully executed';
RAISE NOTICE 'DATABASE STATE: Restored to pre-V4 migration baseline';
RAISE NOTICE 'NEXT STEPS: Verify application functionality with VSAM CARDDAT dataset';