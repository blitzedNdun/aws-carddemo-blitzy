-- ==============================================================================
-- Liquibase Migration: V6__create_reference_tables.sql
-- Description: Creates comprehensive reference tables for transaction classification,
--              disclosure groups, and category balance tracking from ASCII data sources
-- Author: Blitzy agent
-- Version: 6.0
-- Migration Type: CREATE TABLES with precise data types and high-performance indexes
-- ==============================================================================

--liquibase formatted sql

-- NOTE: transaction_types table is created in V3__create_accounts_table.sql
-- This changeset was removed to prevent duplicate table creation error

-- NOTE: transaction_categories table is created in V3__create_accounts_table.sql
-- This changeset was removed to prevent duplicate table creation error

-- NOTE: disclosure_groups table is created in V3__create_accounts_table.sql
-- This changeset was removed to prevent duplicate table creation error

--changeset blitzy-agent:create-transaction-category-balances-table-v6
--comment: Create transaction_category_balances table from tcatbal.txt with composite primary key for account-category balance tracking

-- Create transaction_category_balances table from tcatbal.txt data source
-- Maintains category-specific balance tracking with composite primary key structure
CREATE TABLE transaction_category_balances (
    -- Composite primary key part 1: 11-digit account identifier
    account_id VARCHAR(11) NOT NULL,
    
    -- Composite primary key part 2: 4-character transaction category
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Category-specific balance with DECIMAL(12,2) precision for financial accuracy
    category_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    
    -- Last balance update timestamp for tracking and audit purposes
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Version number for optimistic locking in high-concurrency scenarios
    version_number INTEGER NOT NULL DEFAULT 1,
    
    -- Audit fields for balance change tracking
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Composite primary key constraint
    CONSTRAINT pk_transaction_category_balances PRIMARY KEY (account_id, transaction_category),
    
    -- Foreign key to accounts table for account relationship integrity
    CONSTRAINT fk_tcatbal_account_id FOREIGN KEY (account_id) 
        REFERENCES accounts(account_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Foreign key to transaction_categories for category integrity
    CONSTRAINT fk_tcatbal_transaction_category FOREIGN KEY (transaction_category) 
        REFERENCES transaction_categories(transaction_category) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business validation constraints
    CONSTRAINT chk_tcatbal_account_id_format CHECK (account_id ~ '^[0-9]{11}$'),
    CONSTRAINT chk_tcatbal_transaction_category_format CHECK (transaction_category ~ '^[0-9]{4}$'),
    CONSTRAINT chk_tcatbal_version_positive CHECK (version_number > 0)
);

--rollback DROP TABLE transaction_category_balances CASCADE;

--changeset blitzy-agent:create-reference-tables-indexes-v6
--comment: Create high-performance indexes for reference tables supporting sub-millisecond lookup operations

-- Index for transaction_types table supporting type lookup operations
CREATE INDEX idx_transaction_types_active ON transaction_types (active_status, transaction_type);

-- Index for transaction_types debit/credit classification queries
CREATE INDEX idx_transaction_types_debit_credit ON transaction_types (debit_credit_indicator, transaction_type);

-- Index for transaction_categories hierarchical queries
CREATE INDEX idx_transaction_categories_parent_type ON transaction_categories (parent_transaction_type, active_status);

-- Index for transaction_categories active status filtering
CREATE INDEX idx_transaction_categories_active ON transaction_categories (active_status, transaction_category);

-- Index for disclosure_groups interest rate queries
CREATE INDEX idx_disclosure_groups_interest_rate ON disclosure_groups (interest_rate, active_status);

-- Index for disclosure_groups effective date range queries
CREATE INDEX idx_disclosure_groups_effective_date ON disclosure_groups (effective_date, active_status);

-- Index for disclosure_groups transaction category lookups
CREATE INDEX idx_disclosure_groups_transaction_category ON disclosure_groups (transaction_category, active_status);

-- Index for transaction_category_balances account queries
CREATE INDEX idx_tcatbal_account_id ON transaction_category_balances (account_id, last_updated DESC);

-- Index for transaction_category_balances category aggregation
CREATE INDEX idx_tcatbal_category_balance ON transaction_category_balances (transaction_category, category_balance DESC);

-- Index for transaction_category_balances last updated tracking
CREATE INDEX idx_tcatbal_last_updated ON transaction_category_balances (last_updated DESC) 
    WHERE category_balance != 0.00;

--rollback DROP INDEX IF EXISTS idx_tcatbal_last_updated;
--rollback DROP INDEX IF EXISTS idx_tcatbal_category_balance;
--rollback DROP INDEX IF EXISTS idx_tcatbal_account_id;
--rollback DROP INDEX IF EXISTS idx_disclosure_groups_transaction_category;
--rollback DROP INDEX IF EXISTS idx_disclosure_groups_effective_date;
--rollback DROP INDEX IF EXISTS idx_disclosure_groups_interest_rate;
--rollback DROP INDEX IF EXISTS idx_transaction_categories_active;
--rollback DROP INDEX IF EXISTS idx_transaction_categories_parent_type;
--rollback DROP INDEX IF EXISTS idx_transaction_types_debit_credit;
--rollback DROP INDEX IF EXISTS idx_transaction_types_active;

--changeset blitzy-agent:create-reference-tables-triggers-v6
--comment: Create triggers for reference tables audit trail and automated timestamp management

-- Trigger function for automatic updated_at timestamp maintenance across all reference tables
CREATE OR REPLACE FUNCTION update_reference_tables_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for automatic updated_at timestamp updates
CREATE TRIGGER trg_transaction_types_update_timestamp
    BEFORE UPDATE ON transaction_types
    FOR EACH ROW
    EXECUTE FUNCTION update_reference_tables_updated_at();

CREATE TRIGGER trg_transaction_categories_update_timestamp
    BEFORE UPDATE ON transaction_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_reference_tables_updated_at();

CREATE TRIGGER trg_disclosure_groups_update_timestamp
    BEFORE UPDATE ON disclosure_groups
    FOR EACH ROW
    EXECUTE FUNCTION update_reference_tables_updated_at();

-- Trigger function for transaction_category_balances optimistic locking and audit
CREATE OR REPLACE FUNCTION update_tcatbal_version_and_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    -- Increment version number for optimistic locking
    NEW.version_number = OLD.version_number + 1;
    NEW.last_updated = CURRENT_TIMESTAMP;
    
    -- Log significant balance changes for audit trail
    IF ABS(NEW.category_balance - OLD.category_balance) > 100.00 THEN
        INSERT INTO audit_log (
            table_name, 
            record_id, 
            operation, 
            old_values, 
            new_values, 
            change_timestamp
        ) VALUES (
            'transaction_category_balances',
            NEW.account_id || '-' || NEW.transaction_category,
            'BALANCE_CHANGE',
            json_build_object('old_balance', OLD.category_balance, 'old_version', OLD.version_number),
            json_build_object('new_balance', NEW.category_balance, 'new_version', NEW.version_number),
            CURRENT_TIMESTAMP
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for transaction_category_balances version control and audit
CREATE TRIGGER trg_tcatbal_version_update
    BEFORE UPDATE ON transaction_category_balances
    FOR EACH ROW
    EXECUTE FUNCTION update_tcatbal_version_and_timestamp();

--rollback DROP TRIGGER IF EXISTS trg_tcatbal_version_update ON transaction_category_balances;
--rollback DROP TRIGGER IF EXISTS trg_disclosure_groups_update_timestamp ON disclosure_groups;
--rollback DROP TRIGGER IF EXISTS trg_transaction_categories_update_timestamp ON transaction_categories;
--rollback DROP TRIGGER IF EXISTS trg_transaction_types_update_timestamp ON transaction_types;
--rollback DROP FUNCTION IF EXISTS update_tcatbal_version_and_timestamp();
--rollback DROP FUNCTION IF EXISTS update_reference_tables_updated_at();

-- NOTE: Reference tables initial data loading moved to separate migration scripts:
-- - transaction_types data: V24__load_transaction_types_data.sql
-- - transaction_categories data: V25__load_transaction_categories_data.sql  
-- - disclosure_groups data: V26__load_disclosure_groups_data.sql
-- This follows Liquibase best practices separating table creation from data loading

--changeset blitzy-agent:create-reference-tables-constraints-v6
--comment: Add additional business constraints and validation rules for reference data integrity

-- Add constraint to ensure disclosure groups have valid configurations
ALTER TABLE disclosure_groups 
ADD CONSTRAINT chk_disclosure_groups_rate_consistency 
CHECK (
    (group_id = 'ZEROAPR' AND interest_rate = 0.0000) OR
    (group_id != 'ZEROAPR' AND interest_rate > 0.0000)
);

-- NOTE: Hierarchical constraint for transaction_categories moved to V3 migration
-- where the transaction_categories table is actually created

-- Add unique constraint to ensure one active configuration per group-category combination
CREATE UNIQUE INDEX idx_disclosure_groups_active_unique 
ON disclosure_groups (group_id, transaction_category) 
WHERE active_status = true;

--rollback DROP INDEX IF EXISTS idx_disclosure_groups_active_unique;
--rollback ALTER TABLE transaction_categories DROP CONSTRAINT IF EXISTS chk_transaction_categories_hierarchy;
--rollback ALTER TABLE disclosure_groups DROP CONSTRAINT IF EXISTS chk_disclosure_groups_rate_consistency;

--changeset blitzy-agent:grant-reference-tables-permissions-v6
--comment: Grant appropriate permissions for application roles to access reference tables

-- Grant SELECT permission to application read role for all reference tables
GRANT SELECT ON transaction_types TO carddemo_read_role;
GRANT SELECT ON transaction_categories TO carddemo_read_role;
GRANT SELECT ON disclosure_groups TO carddemo_read_role;
GRANT SELECT ON transaction_category_balances TO carddemo_read_role;

-- Grant full permissions to application write role for reference data management
GRANT SELECT, INSERT, UPDATE, DELETE ON transaction_types TO carddemo_write_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON transaction_categories TO carddemo_write_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON disclosure_groups TO carddemo_write_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON transaction_category_balances TO carddemo_write_role;

-- Grant full permissions to admin role for comprehensive reference data administration
GRANT ALL PRIVILEGES ON transaction_types TO carddemo_admin_role;
GRANT ALL PRIVILEGES ON transaction_categories TO carddemo_admin_role;
GRANT ALL PRIVILEGES ON disclosure_groups TO carddemo_admin_role;
GRANT ALL PRIVILEGES ON transaction_category_balances TO carddemo_admin_role;

--rollback REVOKE ALL PRIVILEGES ON transaction_category_balances FROM carddemo_admin_role;
--rollback REVOKE ALL PRIVILEGES ON disclosure_groups FROM carddemo_admin_role;
--rollback REVOKE ALL PRIVILEGES ON transaction_categories FROM carddemo_admin_role;
--rollback REVOKE ALL PRIVILEGES ON transaction_types FROM carddemo_admin_role;
--rollback REVOKE SELECT, INSERT, UPDATE, DELETE ON transaction_category_balances FROM carddemo_write_role;
--rollback REVOKE SELECT, INSERT, UPDATE, DELETE ON disclosure_groups FROM carddemo_write_role;
--rollback REVOKE SELECT, INSERT, UPDATE, DELETE ON transaction_categories FROM carddemo_write_role;
--rollback REVOKE SELECT, INSERT, UPDATE, DELETE ON transaction_types FROM carddemo_write_role;
--rollback REVOKE SELECT ON transaction_category_balances FROM carddemo_read_role;
--rollback REVOKE SELECT ON disclosure_groups FROM carddemo_read_role;
--rollback REVOKE SELECT ON transaction_categories FROM carddemo_read_role;
--rollback REVOKE SELECT ON transaction_types FROM carddemo_read_role;

--changeset blitzy-agent:create-reference-tables-comments-v6
--comment: Add comprehensive table and column documentation for reference tables

-- Table-level documentation
COMMENT ON TABLE transaction_types IS 'Transaction type reference table migrated from trantype.txt ASCII data. Contains 2-character transaction type codes with descriptions and debit/credit classification for transaction processing. Supports sub-millisecond lookup operations through optimized B-tree indexes for high-performance transaction classification in Spring Boot microservices architecture.';

COMMENT ON TABLE transaction_categories IS 'Transaction category reference table migrated from trancatg.txt ASCII data. Contains 4-character category codes with hierarchical parent-child relationships to transaction types. Enables detailed transaction categorization for reporting and balance tracking with referential integrity constraints supporting comprehensive financial transaction management.';

COMMENT ON TABLE disclosure_groups IS 'Disclosure group reference table migrated from discgrp.txt ASCII data. Manages interest rate configurations and legal disclosure requirements with DECIMAL(5,4) precision for accurate percentage calculations. Associates with accounts through group_id foreign key relationships supporting regulatory compliance and customer communication requirements.';

COMMENT ON TABLE transaction_category_balances IS 'Transaction category balance tracking table migrated from tcatbal.txt ASCII data. Maintains category-specific balance information with composite primary key (account_id, transaction_category) structure. Features optimistic locking through version numbers and automated audit trail generation for financial data integrity and high-concurrency balance management operations.';

-- Column-level documentation for transaction_types
COMMENT ON COLUMN transaction_types.transaction_type IS 'Primary key: 2-character transaction type code from trantype.txt. Maps directly to COBOL transaction classification with format validation ensuring numeric-only values for integration with legacy systems and modern Spring Boot transaction processing microservices.';
COMMENT ON COLUMN transaction_types.type_description IS 'Human-readable transaction type description (e.g., Purchase, Payment, Credit). Limited to 60 characters for consistency with legacy COBOL field lengths while supporting comprehensive transaction type identification and user interface display requirements.';
COMMENT ON COLUMN transaction_types.debit_credit_indicator IS 'Boolean flag indicating transaction impact on account balance. TRUE for debit transactions (increases balance), FALSE for credit transactions (decreases balance). Essential for accurate financial calculation and accounting integration with Spring Boot transaction processing services.';

-- Column-level documentation for transaction_categories
COMMENT ON COLUMN transaction_categories.transaction_category IS 'Primary key: 4-character transaction category code extracted from trancatg.txt 6-character format. Provides granular transaction classification for detailed reporting and balance tracking with format validation ensuring numeric-only values for database integrity.';
COMMENT ON COLUMN transaction_categories.parent_transaction_type IS 'Foreign key to transaction_types table establishing hierarchical relationship. Enables transaction category grouping and rollup reporting capabilities while maintaining referential integrity through CASCADE update operations for data consistency.';
COMMENT ON COLUMN transaction_categories.category_description IS 'Detailed category description (e.g., Regular Sales Draft, Cash payment). Supports user interface display and reporting requirements with length constraints matching legacy COBOL field specifications for system compatibility.';

-- Column-level documentation for disclosure_groups
COMMENT ON COLUMN disclosure_groups.group_id IS 'Primary key component: 10-character group identifier (e.g., A, DEFAULT, ZEROAPR). Maps to accounts.group_id foreign key relationship enabling interest rate and disclosure association with account records for regulatory compliance and customer communication.';
COMMENT ON COLUMN disclosure_groups.interest_rate IS 'Annual interest rate with DECIMAL(5,4) precision supporting accurate percentage calculations. Stores rates as decimal values (e.g., 0.0150 for 1.50% APR) ensuring exact financial arithmetic equivalent to COBOL COMP-3 precision for regulatory compliance and customer billing accuracy.';
COMMENT ON COLUMN disclosure_groups.disclosure_text IS 'Legal disclosure text for regulatory compliance and customer communication. Variable-length TEXT field supporting comprehensive disclosure requirements for different account types and promotional rate programs with unlimited text capacity for regulatory compliance.';

-- Column-level documentation for transaction_category_balances
COMMENT ON COLUMN transaction_category_balances.account_id IS 'Composite primary key component: 11-digit account identifier. Foreign key to accounts table with CASCADE update operations ensuring referential integrity. Format validation ensures numeric-only values for compatibility with legacy account numbering systems and Spring Boot account management services.';
COMMENT ON COLUMN transaction_category_balances.transaction_category IS 'Composite primary key component: 4-character transaction category code. Foreign key to transaction_categories table enabling category-specific balance tracking with referential integrity constraints for accurate financial reporting and account management operations.';
COMMENT ON COLUMN transaction_category_balances.category_balance IS 'Category-specific balance amount with DECIMAL(12,2) precision. Maintains exact financial calculations equivalent to COBOL COMP-3 arithmetic for accurate balance tracking and reporting requirements. Supports positive and negative balance scenarios for comprehensive account management.';
COMMENT ON COLUMN transaction_category_balances.version_number IS 'Optimistic locking version number incremented on each update. Prevents concurrent modification conflicts in high-throughput transaction processing environments while maintaining data integrity and consistency across Spring Boot microservices architecture.';

--rollback COMMENT ON COLUMN transaction_category_balances.version_number IS NULL;
--rollback COMMENT ON COLUMN transaction_category_balances.category_balance IS NULL;
--rollback COMMENT ON COLUMN transaction_category_balances.transaction_category IS NULL;
--rollback COMMENT ON COLUMN transaction_category_balances.account_id IS NULL;
--rollback COMMENT ON COLUMN disclosure_groups.disclosure_text IS NULL;
--rollback COMMENT ON COLUMN disclosure_groups.interest_rate IS NULL;
--rollback COMMENT ON COLUMN disclosure_groups.group_id IS NULL;
--rollback COMMENT ON COLUMN transaction_categories.category_description IS NULL;
--rollback COMMENT ON COLUMN transaction_categories.parent_transaction_type IS NULL;
--rollback COMMENT ON COLUMN transaction_categories.transaction_category IS NULL;
--rollback COMMENT ON COLUMN transaction_types.debit_credit_indicator IS NULL;
--rollback COMMENT ON COLUMN transaction_types.type_description IS NULL;
--rollback COMMENT ON COLUMN transaction_types.transaction_type IS NULL;
--rollback COMMENT ON TABLE transaction_category_balances IS NULL;
--rollback COMMENT ON TABLE disclosure_groups IS NULL;
--rollback COMMENT ON TABLE transaction_categories IS NULL;
--rollback COMMENT ON TABLE transaction_types IS NULL;