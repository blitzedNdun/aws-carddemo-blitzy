-- =====================================================================================
-- Liquibase Changeset: 004-create-test-reference-tables.sql
-- Purpose: Create PostgreSQL reference tables for transaction types and categories
-- Supporting: CardDemo Spring Boot microservices testing infrastructure
-- Based on: COBOL copybooks CVTRA03Y.cpy and CVTRA04Y.cpy
-- Environment: Test environment for integration testing and validation
-- =====================================================================================

--liquibase formatted sql

--changeset liquibase:004-create-test-reference-tables splitStatements:true endDelimiter:;

-- =====================================================================================
-- TRANSACTION TYPES REFERENCE TABLE
-- Maps from: COBOL copybook CVTRA03Y.cpy (TRAN-TYPE-RECORD)
-- Purpose: Static lookup table for transaction type classification
-- Test Support: Integration testing for transaction validation workflows
-- =====================================================================================

CREATE TABLE IF NOT EXISTS transaction_types (
    -- Primary key: 2-character transaction type code
    -- Maps from: TRAN-TYPE PIC X(02) in CVTRA03Y.cpy
    transaction_type    VARCHAR(2) NOT NULL,
    
    -- Transaction type description
    -- Maps from: TRAN-TYPE-DESC PIC X(50) in CVTRA03Y.cpy
    type_description    VARCHAR(50) NOT NULL,
    
    -- Debit/Credit indicator for transaction processing
    -- Business logic flag for account balance calculations
    debit_credit_flag   CHAR(1) NOT NULL CHECK (debit_credit_flag IN ('D', 'C')),
    
    -- Active status for transaction type
    -- Supports testing of active/inactive transaction scenarios
    active_status       BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields for test data management
    created_timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_transaction_types PRIMARY KEY (transaction_type)
);

-- =====================================================================================
-- TRANSACTION CATEGORIES REFERENCE TABLE  
-- Maps from: COBOL copybook CVTRA04Y.cpy (TRAN-CAT-RECORD)
-- Purpose: Transaction category classification with composite key structure
-- Test Support: Complex transaction categorization testing scenarios
-- =====================================================================================

CREATE TABLE IF NOT EXISTS transaction_categories (
    -- Composite primary key part 1: Transaction type code
    -- Maps from: TRAN-TYPE-CD PIC X(02) in CVTRA04Y.cpy TRAN-CAT-KEY
    transaction_type    VARCHAR(2) NOT NULL,
    
    -- Composite primary key part 2: Transaction category code
    -- Maps from: TRAN-CAT-CD PIC 9(04) in CVTRA04Y.cpy TRAN-CAT-KEY
    transaction_category VARCHAR(4) NOT NULL,
    
    -- Transaction category description
    -- Maps from: TRAN-CAT-TYPE-DESC PIC X(50) in CVTRA04Y.cpy
    category_description VARCHAR(50) NOT NULL,
    
    -- Category active status for testing active/inactive scenarios
    active_status       BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Risk level for transaction monitoring (test scenarios)
    risk_level          CHAR(1) NOT NULL DEFAULT 'L' CHECK (risk_level IN ('L', 'M', 'H')),
    
    -- Processing priority for transaction queue testing
    processing_priority INTEGER NOT NULL DEFAULT 5 CHECK (processing_priority BETWEEN 1 AND 10),
    
    -- Audit fields for test data lifecycle management
    created_timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Composite primary key constraint
    CONSTRAINT pk_transaction_categories PRIMARY KEY (transaction_type, transaction_category),
    
    -- Foreign key constraint linking to transaction types
    CONSTRAINT fk_transaction_categories_type 
        FOREIGN KEY (transaction_type) 
        REFERENCES transaction_types(transaction_type)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- =====================================================================================
-- PERFORMANCE OPTIMIZATION INDEXES
-- Supporting: Sub-200ms response time requirements for test scenarios
-- Purpose: Optimize lookup performance for integration testing
-- =====================================================================================

-- Index for transaction type description lookups
CREATE INDEX IF NOT EXISTS idx_transaction_types_description 
    ON transaction_types(type_description);

-- Index for active transaction types (most common test query pattern)
CREATE INDEX IF NOT EXISTS idx_transaction_types_active 
    ON transaction_types(active_status) 
    WHERE active_status = TRUE;

-- Index for transaction category description lookups
CREATE INDEX IF NOT EXISTS idx_transaction_categories_description 
    ON transaction_categories(category_description);

-- Index for active transaction categories with risk level
CREATE INDEX IF NOT EXISTS idx_transaction_categories_risk_active 
    ON transaction_categories(risk_level, active_status) 
    WHERE active_status = TRUE;

-- Index for processing priority ordering in test scenarios
CREATE INDEX IF NOT EXISTS idx_transaction_categories_priority 
    ON transaction_categories(processing_priority, transaction_type);

-- =====================================================================================
-- TEST REFERENCE DATA POPULATION
-- Purpose: Populate reference tables with comprehensive test data
-- Supporting: Integration testing scenarios and validation workflows
-- =====================================================================================

-- Insert standard transaction types for testing
INSERT INTO transaction_types (transaction_type, type_description, debit_credit_flag, active_status) 
VALUES 
    ('01', 'Purchase Transaction', 'D', TRUE),
    ('02', 'Cash Advance', 'D', TRUE),
    ('03', 'Payment Credit', 'C', TRUE),
    ('04', 'Balance Transfer', 'D', TRUE),
    ('05', 'Fee Assessment', 'D', TRUE),
    ('06', 'Interest Charge', 'D', TRUE),
    ('07', 'Refund Credit', 'C', TRUE),
    ('08', 'Dispute Credit', 'C', TRUE),
    ('09', 'Reversal Transaction', 'C', TRUE),
    ('10', 'Maintenance Fee', 'D', TRUE),
    ('99', 'Test Transaction', 'D', FALSE)
ON CONFLICT (transaction_type) DO NOTHING;

-- Insert transaction categories for comprehensive testing scenarios
INSERT INTO transaction_categories (transaction_type, transaction_category, category_description, active_status, risk_level, processing_priority)
VALUES 
    -- Purchase transaction categories
    ('01', '0001', 'Grocery Purchase', TRUE, 'L', 5),
    ('01', '0002', 'Gas Station Purchase', TRUE, 'L', 5),
    ('01', '0003', 'Restaurant Purchase', TRUE, 'L', 5),
    ('01', '0004', 'Online Purchase', TRUE, 'M', 4),
    ('01', '0005', 'International Purchase', TRUE, 'H', 3),
    
    -- Cash advance categories
    ('02', '0001', 'ATM Cash Advance', TRUE, 'M', 6),
    ('02', '0002', 'Bank Counter Cash Advance', TRUE, 'M', 6),
    ('02', '0003', 'International Cash Advance', TRUE, 'H', 4),
    
    -- Payment credit categories
    ('03', '0001', 'Online Payment', TRUE, 'L', 7),
    ('03', '0002', 'Bank Transfer Payment', TRUE, 'L', 7),
    ('03', '0003', 'Check Payment', TRUE, 'L', 6),
    ('03', '0004', 'Automatic Payment', TRUE, 'L', 8),
    
    -- Balance transfer categories
    ('04', '0001', 'Promotional Balance Transfer', TRUE, 'L', 5),
    ('04', '0002', 'Regular Balance Transfer', TRUE, 'L', 5),
    
    -- Fee assessment categories
    ('05', '0001', 'Late Payment Fee', TRUE, 'L', 3),
    ('05', '0002', 'Over Limit Fee', TRUE, 'L', 3),
    ('05', '0003', 'Cash Advance Fee', TRUE, 'L', 4),
    ('05', '0004', 'International Transaction Fee', TRUE, 'L', 4),
    
    -- Interest charge categories
    ('06', '0001', 'Purchase Interest', TRUE, 'L', 2),
    ('06', '0002', 'Cash Advance Interest', TRUE, 'L', 2),
    ('06', '0003', 'Balance Transfer Interest', TRUE, 'L', 2),
    
    -- Refund credit categories
    ('07', '0001', 'Merchant Refund', TRUE, 'L', 6),
    ('07', '0002', 'Fee Refund', TRUE, 'L', 6),
    
    -- Dispute credit categories
    ('08', '0001', 'Chargeback Credit', TRUE, 'M', 4),
    ('08', '0002', 'Dispute Resolution Credit', TRUE, 'M', 4),
    
    -- Reversal transaction categories
    ('09', '0001', 'Authorization Reversal', TRUE, 'L', 9),
    ('09', '0002', 'Settlement Reversal', TRUE, 'M', 8),
    
    -- Maintenance fee categories
    ('10', '0001', 'Annual Fee', TRUE, 'L', 3),
    ('10', '0002', 'Monthly Maintenance Fee', TRUE, 'L', 3),
    
    -- Test transaction categories
    ('99', '9999', 'Test Category - Inactive', FALSE, 'L', 1)
ON CONFLICT (transaction_type, transaction_category) DO NOTHING;

-- =====================================================================================
-- DATA INTEGRITY CONSTRAINTS AND VALIDATION
-- Purpose: Ensure data quality for integration testing scenarios
-- Supporting: Spring Boot microservices validation testing
-- =====================================================================================

-- Add check constraint for transaction type format (2 alphanumeric characters)
ALTER TABLE transaction_types 
ADD CONSTRAINT chk_transaction_type_format 
CHECK (transaction_type ~ '^[A-Za-z0-9]{2}$');

-- Add check constraint for transaction category format (4 numeric characters)
ALTER TABLE transaction_categories 
ADD CONSTRAINT chk_transaction_category_format 
CHECK (transaction_category ~ '^[0-9]{4}$');

-- Add check constraint for description content (non-empty, printable characters)
ALTER TABLE transaction_types 
ADD CONSTRAINT chk_type_description_content 
CHECK (LENGTH(TRIM(type_description)) > 0 AND type_description ~ '^[[:print:]]+$');

ALTER TABLE transaction_categories 
ADD CONSTRAINT chk_category_description_content 
CHECK (LENGTH(TRIM(category_description)) > 0 AND category_description ~ '^[[:print:]]+$');

-- =====================================================================================
-- TRIGGER FUNCTIONS FOR AUDIT TRAIL
-- Purpose: Maintain audit trail for test data changes
-- Supporting: Testing data lifecycle management and validation
-- =====================================================================================

-- Create trigger function for last_updated timestamp maintenance
CREATE OR REPLACE FUNCTION update_last_updated_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for automatic timestamp updates
CREATE TRIGGER trg_transaction_types_last_updated
    BEFORE UPDATE ON transaction_types
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_timestamp();

CREATE TRIGGER trg_transaction_categories_last_updated
    BEFORE UPDATE ON transaction_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_timestamp();

-- =====================================================================================
-- VIEWS FOR TESTING CONVENIENCE
-- Purpose: Simplify common test queries and validation scenarios
-- Supporting: Integration testing and data validation workflows
-- =====================================================================================

-- View for active transaction types with category counts
CREATE OR REPLACE VIEW v_active_transaction_types AS
SELECT 
    tt.transaction_type,
    tt.type_description,
    tt.debit_credit_flag,
    COUNT(tc.transaction_category) as category_count,
    tt.created_timestamp,
    tt.last_updated
FROM transaction_types tt
LEFT JOIN transaction_categories tc ON tt.transaction_type = tc.transaction_type 
    AND tc.active_status = TRUE
WHERE tt.active_status = TRUE
GROUP BY tt.transaction_type, tt.type_description, tt.debit_credit_flag, 
         tt.created_timestamp, tt.last_updated
ORDER BY tt.transaction_type;

-- View for transaction categories with type information
CREATE OR REPLACE VIEW v_transaction_categories_detailed AS
SELECT 
    tc.transaction_type,
    tt.type_description,
    tc.transaction_category,
    tc.category_description,
    tc.risk_level,
    tc.processing_priority,
    tt.debit_credit_flag,
    tc.active_status,
    tc.created_timestamp,
    tc.last_updated
FROM transaction_categories tc
JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
ORDER BY tc.transaction_type, tc.transaction_category;

-- View for high-risk transaction categories (testing fraud scenarios)
CREATE OR REPLACE VIEW v_high_risk_transaction_categories AS
SELECT 
    tc.transaction_type,
    tt.type_description,
    tc.transaction_category,
    tc.category_description,
    tc.risk_level,
    tc.processing_priority
FROM transaction_categories tc
JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
WHERE tc.risk_level = 'H' AND tc.active_status = TRUE
ORDER BY tc.processing_priority, tc.transaction_type;

-- =====================================================================================
-- TESTING UTILITY FUNCTIONS
-- Purpose: Support automated testing and validation procedures
-- Supporting: Spring Boot microservices integration testing
-- =====================================================================================

-- Function to validate transaction type and category combination
CREATE OR REPLACE FUNCTION validate_transaction_classification(
    p_transaction_type VARCHAR(2),
    p_transaction_category VARCHAR(4)
) RETURNS BOOLEAN AS $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM transaction_categories tc
    JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
    WHERE tc.transaction_type = p_transaction_type
    AND tc.transaction_category = p_transaction_category
    AND tc.active_status = TRUE
    AND tt.active_status = TRUE;
    
    RETURN v_count > 0;
END;
$$ LANGUAGE plpgsql;

-- Function to get transaction processing priority
CREATE OR REPLACE FUNCTION get_transaction_priority(
    p_transaction_type VARCHAR(2),
    p_transaction_category VARCHAR(4)
) RETURNS INTEGER AS $$
DECLARE
    v_priority INTEGER;
BEGIN
    SELECT processing_priority INTO v_priority
    FROM transaction_categories
    WHERE transaction_type = p_transaction_type
    AND transaction_category = p_transaction_category
    AND active_status = TRUE;
    
    RETURN COALESCE(v_priority, 5); -- Default priority if not found
END;
$$ LANGUAGE plpgsql;

-- Function to check if transaction type allows debits/credits
CREATE OR REPLACE FUNCTION is_transaction_type_debit(
    p_transaction_type VARCHAR(2)
) RETURNS BOOLEAN AS $$
DECLARE
    v_debit_flag CHAR(1);
BEGIN
    SELECT debit_credit_flag INTO v_debit_flag
    FROM transaction_types
    WHERE transaction_type = p_transaction_type
    AND active_status = TRUE;
    
    RETURN v_debit_flag = 'D';
END;
$$ LANGUAGE plpgsql;

-- =====================================================================================
-- PERFORMANCE MONITORING TABLES
-- Purpose: Track reference table usage for performance optimization
-- Supporting: Test performance analysis and optimization
-- =====================================================================================

-- Table to track reference table access patterns during testing
CREATE TABLE IF NOT EXISTS reference_table_usage_stats (
    table_name          VARCHAR(50) NOT NULL,
    access_type         VARCHAR(20) NOT NULL CHECK (access_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE')),
    access_count        BIGINT NOT NULL DEFAULT 0,
    last_accessed       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    avg_response_time   DECIMAL(10,3), -- Response time in milliseconds
    
    PRIMARY KEY (table_name, access_type)
);

-- Initialize usage statistics tracking
INSERT INTO reference_table_usage_stats (table_name, access_type, access_count)
VALUES 
    ('transaction_types', 'SELECT', 0),
    ('transaction_categories', 'SELECT', 0),
    ('transaction_types', 'INSERT', 0),
    ('transaction_categories', 'INSERT', 0)
ON CONFLICT (table_name, access_type) DO NOTHING;

-- =====================================================================================
-- COMMENTS AND DOCUMENTATION
-- Purpose: Provide comprehensive documentation for test reference tables
-- Supporting: Development team understanding and maintenance
-- =====================================================================================

-- Table comments
COMMENT ON TABLE transaction_types IS 'Reference table for transaction type classification. Maps from COBOL copybook CVTRA03Y.cpy. Supports transaction validation testing and integration scenarios.';

COMMENT ON TABLE transaction_categories IS 'Reference table for transaction category classification with composite key structure. Maps from COBOL copybook CVTRA04Y.cpy. Supports complex transaction categorization testing scenarios.';

-- Column comments for transaction_types
COMMENT ON COLUMN transaction_types.transaction_type IS 'Primary key: 2-character transaction type code. Maps from TRAN-TYPE in CVTRA03Y.cpy';
COMMENT ON COLUMN transaction_types.type_description IS 'Human-readable transaction type description. Maps from TRAN-TYPE-DESC in CVTRA03Y.cpy';
COMMENT ON COLUMN transaction_types.debit_credit_flag IS 'Indicates if transaction type represents debit (D) or credit (C) to account balance';
COMMENT ON COLUMN transaction_types.active_status IS 'Boolean flag indicating if transaction type is active for processing';

-- Column comments for transaction_categories
COMMENT ON COLUMN transaction_categories.transaction_type IS 'Foreign key reference to transaction_types.transaction_type. Part of composite primary key';
COMMENT ON COLUMN transaction_categories.transaction_category IS '4-digit numeric category code. Maps from TRAN-CAT-CD in CVTRA04Y.cpy';
COMMENT ON COLUMN transaction_categories.category_description IS 'Human-readable category description. Maps from TRAN-CAT-TYPE-DESC in CVTRA04Y.cpy';
COMMENT ON COLUMN transaction_categories.risk_level IS 'Risk assessment level: L(ow), M(edium), H(igh) for transaction monitoring';
COMMENT ON COLUMN transaction_categories.processing_priority IS 'Processing priority (1-10) for transaction queue management';

-- View comments
COMMENT ON VIEW v_active_transaction_types IS 'View showing active transaction types with category counts for testing dashboards';
COMMENT ON VIEW v_transaction_categories_detailed IS 'Comprehensive view joining transaction types and categories for detailed testing scenarios';
COMMENT ON VIEW v_high_risk_transaction_categories IS 'View filtering high-risk transaction categories for fraud testing scenarios';

-- Function comments
COMMENT ON FUNCTION validate_transaction_classification(VARCHAR, VARCHAR) IS 'Validates that transaction type and category combination is valid and active';
COMMENT ON FUNCTION get_transaction_priority(VARCHAR, VARCHAR) IS 'Returns processing priority for transaction type/category combination';
COMMENT ON FUNCTION is_transaction_type_debit(VARCHAR) IS 'Checks if transaction type represents a debit transaction';

-- =====================================================================================
-- CHANGESET COMPLETION
-- Purpose: Mark changeset as complete for Liquibase tracking
-- Supporting: Database migration management and rollback capability
-- =====================================================================================

-- Grant appropriate permissions for test environment
GRANT SELECT, INSERT, UPDATE, DELETE ON transaction_types TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON transaction_categories TO PUBLIC;
GRANT SELECT ON v_active_transaction_types TO PUBLIC;
GRANT SELECT ON v_transaction_categories_detailed TO PUBLIC;
GRANT SELECT ON v_high_risk_transaction_categories TO PUBLIC;
GRANT EXECUTE ON FUNCTION validate_transaction_classification(VARCHAR, VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION get_transaction_priority(VARCHAR, VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION is_transaction_type_debit(VARCHAR) TO PUBLIC;

-- Changeset completion marker
SELECT 'Reference tables created successfully for CardDemo testing environment' AS status;

--rollback DROP VIEW IF EXISTS v_high_risk_transaction_categories;
--rollback DROP VIEW IF EXISTS v_transaction_categories_detailed;
--rollback DROP VIEW IF EXISTS v_active_transaction_types;
--rollback DROP FUNCTION IF EXISTS is_transaction_type_debit(VARCHAR);
--rollback DROP FUNCTION IF EXISTS get_transaction_priority(VARCHAR, VARCHAR);
--rollback DROP FUNCTION IF EXISTS validate_transaction_classification(VARCHAR, VARCHAR);
--rollback DROP FUNCTION IF EXISTS update_last_updated_timestamp();
--rollback DROP TABLE IF EXISTS reference_table_usage_stats;
--rollback DROP TABLE IF EXISTS transaction_categories;
--rollback DROP TABLE IF EXISTS transaction_types;