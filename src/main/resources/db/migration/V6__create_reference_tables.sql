-- ==============================================================================
-- Liquibase Migration: V6__create_reference_tables.sql
-- Description: Creates comprehensive reference tables for transaction classification,
--              disclosure groups, and category balance tracking from ASCII data sources
-- Author: Blitzy agent
-- Version: 6.0
-- Migration Type: CREATE TABLES with precise data types and high-performance indexes
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:create-transaction-types-table-v6
--comment: Create transaction_types reference table from trantype.txt with 2-character type codes and debit/credit classification

-- Create transaction_types table from trantype.txt data source
-- Provides transaction classification with debit/credit indicators for transaction processing
CREATE TABLE transaction_types (
    -- Primary key: 2-character transaction type code from trantype.txt
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Descriptive name for transaction type (e.g., "Purchase", "Payment", "Credit")
    type_description VARCHAR(60) NOT NULL,
    
    -- Debit/credit indicator for proper transaction classification and accounting
    -- TRUE = Debit transaction (increases balance), FALSE = Credit transaction (decreases balance)
    debit_credit_indicator BOOLEAN NOT NULL DEFAULT true,
    
    -- Active status for reference data lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit fields for reference data management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_types PRIMARY KEY (transaction_type),
    
    -- Business validation constraints
    CONSTRAINT chk_transaction_type_format CHECK (transaction_type ~ '^[0-9]{2}$'),
    CONSTRAINT chk_type_description_length CHECK (length(trim(type_description)) >= 3)
);

--rollback DROP TABLE transaction_types CASCADE;

--changeset blitzy-agent:create-transaction-categories-table-v6
--comment: Create transaction_categories reference table from trancatg.txt with 4-character category codes and hierarchical support

-- Create transaction_categories table from trancatg.txt data source
-- Supports hierarchical transaction categorization with parent-child relationships
CREATE TABLE transaction_categories (
    -- Primary key: 4-character transaction category code from trancatg.txt
    -- Note: Original data shows 6-character codes, but last 4 characters form the category
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Parent transaction type (first 2 characters) for hierarchical classification
    parent_transaction_type VARCHAR(2) NOT NULL,
    
    -- Detailed category description (e.g., "Regular Sales Draft", "Cash payment")
    category_description VARCHAR(60) NOT NULL,
    
    -- Active status for category lifecycle management and business rule enforcement
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit fields for reference data management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_categories PRIMARY KEY (transaction_category, parent_transaction_type),
    
    -- Foreign key to transaction_types for hierarchical integrity
    CONSTRAINT fk_transaction_categories_parent_type FOREIGN KEY (parent_transaction_type) 
        REFERENCES transaction_types(transaction_type) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business validation constraints
    CONSTRAINT chk_transaction_category_format CHECK (transaction_category ~ '^[0-9]{4}$'),
    CONSTRAINT chk_parent_type_format CHECK (parent_transaction_type ~ '^[0-9]{2}$'),
    CONSTRAINT chk_category_description_length CHECK (length(trim(category_description)) >= 3)
);

--rollback DROP TABLE transaction_categories CASCADE;

--changeset blitzy-agent:create-disclosure-groups-table-v6
--comment: Create disclosure_groups reference table from discgrp.txt with precise interest rate management and legal disclosure text

-- Create disclosure_groups table from discgrp.txt data source
-- Manages interest rate configurations and legal disclosure requirements
CREATE TABLE disclosure_groups (
    -- Primary key: 10-character group identifier (e.g., "A", "DEFAULT", "ZEROAPR")
    -- Padded to 10 characters for consistency with accounts.group_id foreign key
    group_id VARCHAR(10) NOT NULL,
    
    -- Associated transaction category for interest rate application
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Interest rate with DECIMAL(5,4) precision supporting percentage calculations
    -- Stores rates as decimal values (e.g., 0.0150 for 1.50% APR)
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    
    -- Legal disclosure text for regulatory compliance and customer communication
    disclosure_text TEXT,
    
    -- Effective date for interest rate and disclosure changes
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    -- Active status for disclosure group lifecycle management
    active_status BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit fields for compliance tracking and change management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_disclosure_groups PRIMARY KEY (group_id, transaction_category),
    
    -- Foreign key to transaction_categories for referential integrity
    CONSTRAINT fk_disclosure_groups_transaction_category FOREIGN KEY (transaction_category) 
        REFERENCES transaction_categories(transaction_category) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    
    -- Business validation constraints
    CONSTRAINT chk_group_id_format CHECK (length(trim(group_id)) >= 1 AND length(group_id) <= 10),
    CONSTRAINT chk_interest_rate_range CHECK (interest_rate >= 0.0000 AND interest_rate <= 9.9999),
    CONSTRAINT chk_effective_date_range CHECK (
        effective_date >= DATE '1970-01-01' AND 
        effective_date <= CURRENT_DATE + INTERVAL '10 years'
    )
);

--rollback DROP TABLE disclosure_groups CASCADE;

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

--changeset blitzy-agent:populate-reference-tables-initial-data-v6
--comment: Populate reference tables with initial data from ASCII source files

-- NOTE: Transaction types data loading moved to separate migration V24__load_transaction_types_data.sql
-- This follows Liquibase best practices separating table creation from data loading

-- Populate transaction_categories table from trancatg.txt data
-- Extracting 4-character category codes and mapping to parent transaction types
INSERT INTO transaction_categories (transaction_category, parent_transaction_type, category_description, active_status) VALUES
('0001', '01', 'Regular Sales Draft', true),
('0002', '01', 'Regular Cash Advance', true),
('0003', '01', 'Convenience Check Debit', true),
('0004', '01', 'ATM Cash Advance', true),
('0005', '01', 'Interest Amount', true),
('0001', '02', 'Cash payment', true),
('0002', '02', 'Electronic payment', true),
('0003', '02', 'Check payment', true),
('0001', '03', 'Credit to Account', true),
('0002', '03', 'Credit to Purchase balance', true),
('0003', '03', 'Credit to Cash balance', true),
('0001', '04', 'Zero dollar authorization', true),
('0002', '04', 'Online purchase authorization', true),
('0003', '04', 'Travel booking authorization', true),
('0001', '05', 'Refund credit', true),
('0001', '06', 'Fraud reversal', true),
('0002', '06', 'Non-fraud reversal', true),
('0001', '07', 'Sales draft credit adjustment', true);

-- Populate disclosure_groups table from discgrp.txt data
-- Extracting group names, transaction categories, and interest rates
INSERT INTO disclosure_groups (group_id, transaction_category, interest_rate, disclosure_text, effective_date, active_status) VALUES
-- Group A configurations
('A', '0001', 0.0150, 'Standard Purchase APR for Group A accounts', CURRENT_DATE, true),
('A', '0002', 0.0250, 'Cash Advance APR for Group A accounts', CURRENT_DATE, true),
('A', '0003', 0.0250, 'Convenience Check APR for Group A accounts', CURRENT_DATE, true),
('A', '0004', 0.0250, 'ATM Cash Advance APR for Group A accounts', CURRENT_DATE, true),
-- DEFAULT group configurations
('DEFAULT', '0001', 0.0150, 'Standard Purchase APR for Default accounts', CURRENT_DATE, true),
('DEFAULT', '0002', 0.0250, 'Cash Advance APR for Default accounts', CURRENT_DATE, true),
('DEFAULT', '0003', 0.0250, 'Convenience Check APR for Default accounts', CURRENT_DATE, true),
('DEFAULT', '0004', 0.0250, 'ATM Cash Advance APR for Default accounts', CURRENT_DATE, true),
-- ZEROAPR group configurations (promotional rates)
('ZEROAPR', '0001', 0.0000, 'Promotional Zero APR for Purchases', CURRENT_DATE, true),
('ZEROAPR', '0002', 0.0000, 'Promotional Zero APR for Cash Advances', CURRENT_DATE, true),
('ZEROAPR', '0003', 0.0000, 'Promotional Zero APR for Convenience Checks', CURRENT_DATE, true),
('ZEROAPR', '0004', 0.0000, 'Promotional Zero APR for ATM Cash Advances', CURRENT_DATE, true);

--rollback DELETE FROM disclosure_groups;
--rollback DELETE FROM transaction_categories;
--rollback DELETE FROM transaction_types;

--changeset blitzy-agent:create-reference-tables-constraints-v6
--comment: Add additional business constraints and validation rules for reference data integrity

-- Add constraint to ensure disclosure groups have valid configurations
ALTER TABLE disclosure_groups 
ADD CONSTRAINT chk_disclosure_groups_rate_consistency 
CHECK (
    (group_id = 'ZEROAPR' AND interest_rate = 0.0000) OR
    (group_id != 'ZEROAPR' AND interest_rate > 0.0000)
);

-- Add constraint to ensure transaction categories follow hierarchical naming
ALTER TABLE transaction_categories 
ADD CONSTRAINT chk_transaction_categories_hierarchy 
CHECK (
    substring(transaction_category, 1, 2) = '00' OR 
    EXISTS (
        SELECT 1 FROM transaction_types tt 
        WHERE tt.transaction_type = parent_transaction_type
    )
);

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