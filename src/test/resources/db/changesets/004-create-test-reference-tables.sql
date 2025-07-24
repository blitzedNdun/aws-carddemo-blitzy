--liquibase formatted sql

--changeset blitzy-agent:004-create-test-reference-tables
--comment: Create PostgreSQL reference tables for transaction types and categories based on COBOL copybook structures for test environment support

-- =========================================================================
-- TRANSACTION TYPE REFERENCE TABLE (trantype)
-- Based on COBOL copybook CVTRA03Y.cpy TRAN-TYPE-RECORD structure
-- Supports transaction type validation testing as per Testing Strategy 6.6.3.3
-- =========================================================================

CREATE TABLE trantype (
    -- Primary key field corresponding to TRAN-TYPE PIC X(02)
    transaction_type VARCHAR(2) NOT NULL,
    
    -- Description field corresponding to TRAN-TYPE-DESC PIC X(50)
    type_description VARCHAR(50) NOT NULL,
    
    -- Audit fields for test data management and validation
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Version field for optimistic locking in integration tests
    version INTEGER DEFAULT 0 NOT NULL,
    
    CONSTRAINT pk_trantype PRIMARY KEY (transaction_type)
);

-- Create index for performance optimization in test scenarios
CREATE INDEX idx_trantype_description ON trantype (type_description);

-- Add check constraint for transaction type code validation
ALTER TABLE trantype ADD CONSTRAINT chk_trantype_code_format 
    CHECK (transaction_type ~ '^[A-Z0-9]{2}$');

-- Add check constraint for description validation
ALTER TABLE trantype ADD CONSTRAINT chk_trantype_desc_not_empty 
    CHECK (TRIM(type_description) <> '');

-- =========================================================================
-- TRANSACTION CATEGORY REFERENCE TABLE (trancatg)
-- Based on COBOL copybook CVTRA04Y.cpy TRAN-CAT-RECORD structure
-- Supports transaction classification testing as per Core Services Architecture 6.1.2.4
-- =========================================================================

CREATE TABLE trancatg (
    -- Composite primary key fields corresponding to TRAN-CAT-KEY
    -- TRAN-TYPE-CD PIC X(02)
    transaction_type_code VARCHAR(2) NOT NULL,
    
    -- TRAN-CAT-CD PIC 9(04) - 4-digit numeric category code
    transaction_category_code INTEGER NOT NULL,
    
    -- Description field corresponding to TRAN-CAT-TYPE-DESC PIC X(50)
    category_type_description VARCHAR(50) NOT NULL,
    
    -- Audit fields for test data management and validation
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Version field for optimistic locking in integration tests
    version INTEGER DEFAULT 0 NOT NULL,
    
    CONSTRAINT pk_trancatg PRIMARY KEY (transaction_type_code, transaction_category_code)
);

-- Foreign key constraint linking to transaction types table
-- Ensures referential integrity as per Database Design 6.2.2.1
ALTER TABLE trancatg ADD CONSTRAINT fk_trancatg_trantype 
    FOREIGN KEY (transaction_type_code) REFERENCES trantype(transaction_type)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- Performance indexes for test query optimization
CREATE INDEX idx_trancatg_type_code ON trancatg (transaction_type_code);
CREATE INDEX idx_trancatg_category_code ON trancatg (transaction_category_code);
CREATE INDEX idx_trancatg_description ON trancatg (category_type_description);

-- Composite index for efficient lookups matching VSAM key structure
CREATE INDEX idx_trancatg_composite ON trancatg (transaction_type_code, transaction_category_code);

-- Add check constraints for data validation in test environment
ALTER TABLE trancatg ADD CONSTRAINT chk_trancatg_type_code_format 
    CHECK (transaction_type_code ~ '^[A-Z0-9]{2}$');

ALTER TABLE trancatg ADD CONSTRAINT chk_trancatg_category_code_range 
    CHECK (transaction_category_code >= 0 AND transaction_category_code <= 9999);

ALTER TABLE trancatg ADD CONSTRAINT chk_trancatg_desc_not_empty 
    CHECK (TRIM(category_type_description) <> '');

-- =========================================================================
-- TEST DATA SETUP FOR INTEGRATION TESTING
-- Provides baseline reference data for transaction validation testing
-- Supporting Integration Architecture 6.3 and Testing Strategy 6.6.3.3
-- =========================================================================

-- Insert standard transaction types for test scenarios
INSERT INTO trantype (transaction_type, type_description) VALUES
    ('01', 'Purchase Transaction'),
    ('02', 'Cash Advance'),
    ('03', 'Payment Transaction'),
    ('04', 'Balance Transfer'),
    ('05', 'Fee Transaction'),
    ('06', 'Interest Charge'),
    ('07', 'Credit Adjustment'),
    ('08', 'Debit Adjustment'),
    ('09', 'Refund Transaction'),
    ('10', 'Authorization Transaction');

-- Insert transaction categories for comprehensive testing coverage
INSERT INTO trancatg (transaction_type_code, transaction_category_code, category_type_description) VALUES
    -- Purchase transaction categories
    ('01', 0001, 'Retail Purchase'),
    ('01', 0002, 'Online Purchase'),
    ('01', 0003, 'Recurring Payment'),
    ('01', 0004, 'ATM Purchase'),
    
    -- Cash advance categories
    ('02', 0001, 'ATM Cash Advance'),
    ('02', 0002, 'Over Counter Cash Advance'),
    ('02', 0003, 'Convenience Check'),
    
    -- Payment transaction categories
    ('03', 0001, 'Online Payment'),
    ('03', 0002, 'Phone Payment'),
    ('03', 0003, 'Mail Payment'),
    ('03', 0004, 'Branch Payment'),
    ('03', 0005, 'Auto Payment'),
    
    -- Balance transfer categories
    ('04', 0001, 'Promotional Balance Transfer'),
    ('04', 0002, 'Standard Balance Transfer'),
    
    -- Fee transaction categories
    ('05', 0001, 'Annual Fee'),
    ('05', 0002, 'Late Payment Fee'),
    ('05', 0003, 'Over Limit Fee'),
    ('05', 0004, 'Foreign Transaction Fee'),
    ('05', 0005, 'Cash Advance Fee'),
    
    -- Interest charge categories
    ('06', 0001, 'Purchase Interest'),
    ('06', 0002, 'Cash Advance Interest'),
    ('06', 0003, 'Balance Transfer Interest'),
    
    -- Adjustment categories
    ('07', 0001, 'Merchant Credit'),
    ('07', 0002, 'Customer Service Credit'),
    ('07', 0003, 'Billing Error Credit'),
    ('08', 0001, 'Chargeback Reversal'),
    ('08', 0002, 'Account Maintenance Charge'),
    
    -- Refund categories
    ('09', 0001, 'Purchase Refund'),
    ('09', 0002, 'Fee Refund'),
    ('09', 0003, 'Interest Refund'),
    
    -- Authorization categories
    ('10', 0001, 'Pre-Authorization'),
    ('10', 0002, 'Authorization Reversal'),
    ('10', 0003, 'Authorization Completion');

-- =========================================================================
-- PERFORMANCE OPTIMIZATION FOR TEST ENVIRONMENT
-- Supporting high-volume transaction testing scenarios
-- =========================================================================

-- Update table statistics for query planner optimization
ANALYZE trantype;
ANALYZE trancatg;

-- Add comments for documentation and test scenario identification
COMMENT ON TABLE trantype IS 'Transaction type reference table for test environment - maps COBOL TRAN-TYPE-RECORD structure to PostgreSQL';
COMMENT ON TABLE trancatg IS 'Transaction category reference table for test environment - maps COBOL TRAN-CAT-RECORD structure to PostgreSQL';

COMMENT ON COLUMN trantype.transaction_type IS 'Primary key: 2-character transaction type code (TRAN-TYPE PIC X(02))';
COMMENT ON COLUMN trantype.type_description IS 'Transaction type description (TRAN-TYPE-DESC PIC X(50))';

COMMENT ON COLUMN trancatg.transaction_type_code IS 'Foreign key to trantype: 2-character transaction type code (TRAN-TYPE-CD PIC X(02))';
COMMENT ON COLUMN trancatg.transaction_category_code IS 'Category code: 4-digit numeric identifier (TRAN-CAT-CD PIC 9(04))';
COMMENT ON COLUMN trancatg.category_type_description IS 'Category description (TRAN-CAT-TYPE-DESC PIC X(50))';

--rollback DROP TABLE IF EXISTS trancatg CASCADE;
--rollback DROP TABLE IF EXISTS trantype CASCADE;