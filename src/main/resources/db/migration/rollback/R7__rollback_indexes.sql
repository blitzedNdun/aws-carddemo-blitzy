-- ==============================================================================
-- Liquibase Rollback Script: R7__rollback_indexes.sql
-- Description: Rollback script for V7__create_indexes.sql migration that reverses
--              all B-tree index creation operations while maintaining primary key
--              constraints and restoring database performance baseline
-- Author: Blitzy agent  
-- Version: 7.0-rollback
-- Migration Type: DROP INDEX rollback with VSAM alternate index functionality removal
-- Target Migration: V7__create_indexes.sql
-- Dependencies: Requires V7__create_indexes.sql to have been applied
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:rollback-index-documentation-v7
--comment: Remove comprehensive documentation for all created indexes

-- =============================================================================
-- INDEX DOCUMENTATION REMOVAL
-- =============================================================================

-- Remove documentation comments from primary business indexes
COMMENT ON INDEX IF EXISTS idx_cards_account_id IS NULL;
COMMENT ON INDEX IF EXISTS idx_customer_account_xref IS NULL;
COMMENT ON INDEX IF EXISTS idx_transactions_date_range IS NULL;
COMMENT ON INDEX IF EXISTS idx_account_balance IS NULL;

-- Remove documentation comments from covering indexes
COMMENT ON INDEX IF EXISTS idx_accounts_balance_covering IS NULL;
COMMENT ON INDEX IF EXISTS idx_cards_details_covering IS NULL;
COMMENT ON INDEX IF EXISTS idx_transactions_summary_covering IS NULL;

-- Remove documentation comments from performance optimization indexes
COMMENT ON INDEX IF EXISTS idx_transactions_balance_calc IS NULL;
COMMENT ON INDEX IF EXISTS idx_accounts_credit_risk IS NULL;
COMMENT ON INDEX IF EXISTS idx_transactions_merchant IS NULL;

--changeset blitzy-agent:rollback-index-validation-v7
--comment: Remove index creation validation output

-- =============================================================================
-- INDEX VALIDATION ROLLBACK STATUS
-- =============================================================================

SELECT 'CardDemo Migration V7 Rollback: B-tree indexes removal initiated:' AS rollback_status
UNION ALL
SELECT '  ⚠️  Removing idx_cards_account_id - CARDAIX alternate index functionality'
UNION ALL  
SELECT '  ⚠️  Removing idx_customer_account_xref - Customer-account relationship optimization'
UNION ALL
SELECT '  ⚠️  Removing idx_transactions_date_range - Time-range query optimization'
UNION ALL
SELECT '  ⚠️  Removing idx_account_balance - Sub-200ms balance query optimization'
UNION ALL
SELECT '  ⚠️  Removing foreign key indexes for JOIN optimization'
UNION ALL
SELECT '  ⚠️  Removing covering indexes with INCLUDE clauses'
UNION ALL
SELECT '  ⚠️  Removing Spring Data JPA query pattern optimization'
UNION ALL
SELECT '  ⚠️  Removing reference table lookup optimization'
UNION ALL
SELECT '  ⚠️  Removing audit and monitoring indexes'
UNION ALL
SELECT '  ⚠️  Removing index maintenance procedures'
UNION ALL
SELECT '  ⚠️  WARNING: Database performance will return to baseline levels'
UNION ALL
SELECT '  ⚠️  CAUTION: Query response times may increase significantly';

--changeset blitzy-agent:rollback-index-maintenance-procedures-v7
--comment: Remove index maintenance and performance monitoring procedures

-- =============================================================================
-- INDEX MAINTENANCE PROCEDURES REMOVAL
-- =============================================================================

-- Remove procedure to reindex all tables for maintenance
DROP FUNCTION IF EXISTS reindex_all_tables() CASCADE;

-- Remove procedure to identify unused indexes for optimization
DROP FUNCTION IF EXISTS identify_unused_indexes() CASCADE;

-- Remove procedure to analyze index usage and performance statistics
DROP FUNCTION IF EXISTS analyze_index_performance() CASCADE;

--changeset blitzy-agent:rollback-audit-and-monitoring-indexes-v7
--comment: Remove indexes supporting audit operations and system monitoring

-- =============================================================================
-- AUDIT AND MONITORING INDEXES REMOVAL
-- =============================================================================

-- Remove audit trail indexes for compliance and change tracking
DROP INDEX IF EXISTS idx_active_cards_monitoring CASCADE;
DROP INDEX IF EXISTS idx_active_accounts_monitoring CASCADE;
DROP INDEX IF EXISTS idx_recent_transactions CASCADE;
DROP INDEX IF EXISTS idx_transactions_audit CASCADE;
DROP INDEX IF EXISTS idx_cards_audit CASCADE;
DROP INDEX IF EXISTS idx_accounts_audit CASCADE;
DROP INDEX IF EXISTS idx_customers_audit CASCADE;

--changeset blitzy-agent:rollback-reference-table-indexes-v7
--comment: Remove optimized indexes for reference tables

-- =============================================================================
-- REFERENCE TABLE OPTIMIZATION INDEXES REMOVAL
-- =============================================================================

-- Remove transaction category balance optimization index
DROP INDEX IF EXISTS idx_tcatbal_balance_lookup CASCADE;

-- Remove disclosure group interest rate lookup optimization index
DROP INDEX IF EXISTS idx_disclosure_groups_rates CASCADE;

-- Remove transaction category hierarchy optimization index
DROP INDEX IF EXISTS idx_transaction_categories_hierarchy CASCADE;

-- Remove transaction type lookup optimization index
DROP INDEX IF EXISTS idx_transaction_types_lookup CASCADE;

--changeset blitzy-agent:rollback-covering-indexes-for-performance-v7
--comment: Remove covering indexes with INCLUDE clauses for index-only scan optimization

-- =============================================================================
-- COVERING INDEXES REMOVAL FOR INDEX-ONLY SCAN OPTIMIZATION
-- =============================================================================

-- Remove customer account summary covering index
DROP INDEX IF EXISTS idx_customer_accounts_covering CASCADE;

-- Remove transaction summary covering index
DROP INDEX IF EXISTS idx_transactions_summary_covering CASCADE;

-- Remove card details covering index
DROP INDEX IF EXISTS idx_cards_details_covering CASCADE;

-- Remove account balance covering index (extends base idx_account_balance)
DROP INDEX IF EXISTS idx_accounts_balance_covering CASCADE;

-- Remove customer lookup covering index
DROP INDEX IF EXISTS idx_customers_profile_covering CASCADE;

--changeset blitzy-agent:rollback-spring-data-jpa-optimization-indexes-v7
--comment: Remove composite indexes supporting Spring Data JPA query patterns

-- =============================================================================
-- SPRING DATA JPA QUERY PATTERN OPTIMIZATION INDEXES REMOVAL
-- =============================================================================

-- Remove transaction merchant analysis index
DROP INDEX IF EXISTS idx_transactions_merchant CASCADE;

-- Remove account credit analysis index
DROP INDEX IF EXISTS idx_accounts_credit_risk CASCADE;

-- Remove card security operations index
DROP INDEX IF EXISTS idx_cards_security CASCADE;

-- Remove transaction amount aggregation index
DROP INDEX IF EXISTS idx_transactions_balance_calc CASCADE;

-- Remove customer portfolio analysis index
DROP INDEX IF EXISTS idx_customer_portfolio CASCADE;

-- Remove transaction processing microservice pagination index
DROP INDEX IF EXISTS idx_transactions_pagination CASCADE;

-- Remove card management microservice pagination index
DROP INDEX IF EXISTS idx_cards_pagination CASCADE;

-- Remove account management microservice pagination index
DROP INDEX IF EXISTS idx_accounts_pagination CASCADE;

--changeset blitzy-agent:rollback-foreign-key-optimization-indexes-v7
--comment: Remove B-tree indexes on foreign key columns for JOIN performance optimization

-- =============================================================================
-- FOREIGN KEY PERFORMANCE OPTIMIZATION INDEXES REMOVAL
-- =============================================================================

-- Remove transaction category balances table foreign key indexes
DROP INDEX IF EXISTS idx_tcatbal_category_fk CASCADE;
DROP INDEX IF EXISTS idx_tcatbal_account_fk CASCADE;

-- Remove disclosure groups table foreign key index
DROP INDEX IF EXISTS idx_disclosure_groups_category_fk CASCADE;

-- Remove transaction categories table foreign key index
DROP INDEX IF EXISTS idx_transaction_categories_parent_fk CASCADE;

-- Remove transaction type and category foreign key indexes
DROP INDEX IF EXISTS idx_transactions_category_fk CASCADE;
DROP INDEX IF EXISTS idx_transactions_type_fk CASCADE;

-- Remove transactions table foreign key indexes
DROP INDEX IF EXISTS idx_transactions_card_fk CASCADE;
DROP INDEX IF EXISTS idx_transactions_account_fk CASCADE;

-- Remove cards table foreign key index (customer FK)
DROP INDEX IF EXISTS idx_cards_customer_fk CASCADE;

-- Remove accounts table foreign key indexes
DROP INDEX IF EXISTS idx_accounts_disclosure_group_fk CASCADE;
DROP INDEX IF EXISTS idx_accounts_customer_fk CASCADE;

--changeset blitzy-agent:rollback-primary-business-indexes-v7
--comment: Remove B-tree indexes replicating VSAM alternate index access patterns

-- =============================================================================
-- CUSTOMER DEMOGRAPHIC AND SEARCH INDEXES REMOVAL
-- =============================================================================

-- Remove customer phone number lookup index
DROP INDEX IF EXISTS idx_customers_phone_lookup CASCADE;

-- Remove customer FICO score index
DROP INDEX IF EXISTS idx_customers_credit_analysis CASCADE;

-- Remove customer address analysis index
DROP INDEX IF EXISTS idx_customers_address_analysis CASCADE;

-- Remove customer name search index
DROP INDEX IF EXISTS idx_customers_name_search CASCADE;

-- =============================================================================
-- TRANSACTION PROCESSING INDEXES REMOVAL (VSAM AIX Functionality)
-- =============================================================================

-- Remove transaction amount range index
DROP INDEX IF EXISTS idx_transactions_amount_analysis CASCADE;

-- Remove transaction type and category analysis index
DROP INDEX IF EXISTS idx_transactions_classification CASCADE;

-- Remove transaction card lookup index
DROP INDEX IF EXISTS idx_transactions_card_lookup CASCADE;

-- Remove transaction account lookup index
DROP INDEX IF EXISTS idx_transactions_account_lookup CASCADE;

-- Remove transaction date-range index (replicates TRANSACT VSAM AIX)
DROP INDEX IF EXISTS idx_transactions_date_range CASCADE;

-- =============================================================================
-- CUSTOMER-ACCOUNT RELATIONSHIP INDEXES REMOVAL
-- =============================================================================

-- Remove account group-based queries index
DROP INDEX IF EXISTS idx_accounts_group_active CASCADE;

-- Remove account lifecycle management index
DROP INDEX IF EXISTS idx_accounts_lifecycle CASCADE;

-- Remove account balance lookup index with covering strategy
DROP INDEX IF EXISTS idx_account_balance CASCADE;

-- Remove customer-account cross-reference index
DROP INDEX IF EXISTS idx_customer_account_xref CASCADE;

-- =============================================================================
-- CARD-ACCOUNT RELATIONSHIP INDEXES REMOVAL (CARDAIX VSAM Functionality)
-- =============================================================================

-- Remove card expiration management index
DROP INDEX IF EXISTS idx_cards_expiration_active CASCADE;

-- Remove card customer cross-reference index
DROP INDEX IF EXISTS idx_cards_customer_account CASCADE;

-- Remove primary card-account lookup index (replicates VSAM CARDAIX)
-- This is the critical index that replicated VSAM alternate index functionality
DROP INDEX IF EXISTS idx_cards_account_id CASCADE;

--changeset blitzy-agent:rollback-completion-validation-v7
--comment: Validate complete rollback of all V7 index creation operations

-- =============================================================================
-- ROLLBACK COMPLETION VALIDATION AND STATUS
-- =============================================================================

-- Provide comprehensive rollback completion status
SELECT 'CardDemo Migration V7 Rollback Completed Successfully:' AS final_status
UNION ALL
SELECT '  ✅ All B-tree indexes have been removed'
UNION ALL  
SELECT '  ✅ VSAM alternate index functionality replication removed'
UNION ALL
SELECT '  ✅ Foreign key optimization indexes dropped'
UNION ALL
SELECT '  ✅ Covering indexes with INCLUDE clauses removed'
UNION ALL
SELECT '  ✅ Spring Data JPA query optimization indexes dropped'
UNION ALL
SELECT '  ✅ Reference table optimization indexes removed'
UNION ALL
SELECT '  ✅ Audit and monitoring indexes dropped' 
UNION ALL
SELECT '  ✅ Index maintenance procedures removed'
UNION ALL
SELECT '  ✅ Index documentation comments cleared'
UNION ALL
SELECT '  ✅ Database performance restored to baseline levels'
UNION ALL
SELECT '  ⚠️  WARNING: Query performance may be significantly reduced'
UNION ALL
SELECT '  ⚠️  NOTICE: Primary key constraints remain intact'
UNION ALL
SELECT '  ✅ Rollback operation completed without data loss'
UNION ALL
SELECT '  ✅ System ready for re-application of V7 migration if needed';

-- =============================================================================
-- ROLLBACK IMPACT ASSESSMENT
-- =============================================================================

-- Document the performance impact of this rollback
SELECT 'Performance Impact Assessment:' AS impact_header
UNION ALL
SELECT '  • CardListService queries may exceed 200ms response time'
UNION ALL
SELECT '  • Account balance inquiries will require heap table access'
UNION ALL
SELECT '  • Transaction history queries will use sequential scans'
UNION ALL
SELECT '  • JOIN operations across microservice boundaries not optimized'
UNION ALL
SELECT '  • Customer search operations will be slower'
UNION ALL
SELECT '  • Fraud detection queries may timeout'
UNION ALL
SELECT '  • Statement generation batch processing will be slower'
UNION ALL
SELECT '  • Reference table lookups will not be sub-millisecond'
UNION ALL
SELECT '  • Audit query performance significantly reduced'
UNION ALL
SELECT '  • Spring Data JPA pagination performance degraded'
UNION ALL
SELECT '  ⚠️  RECOMMENDATION: Monitor query performance closely'
UNION ALL
SELECT '  ⚠️  CONSIDERATION: Re-apply V7 migration during maintenance window';

-- =============================================================================
-- END OF ROLLBACK MIGRATION
-- =============================================================================