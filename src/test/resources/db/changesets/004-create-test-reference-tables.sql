--liquibase formatted sql

--changeset blitzy-agent:004-create-test-reference-tables
--comment: Create PostgreSQL reference tables for transaction types and categories in test environment

-- ====================================================================
-- Transaction Types Reference Table
-- Based on COBOL copybook CVTRA03Y.cpy: TRAN-TYPE-RECORD (RECLN = 60)
-- Original structure: TRAN-TYPE (PIC X(02)), TRAN-TYPE-DESC (PIC X(50))
-- ====================================================================

CREATE TABLE IF NOT EXISTS transaction_types (
    transaction_type        VARCHAR(2) NOT NULL,
    type_description        VARCHAR(60) NOT NULL,
    debit_credit_indicator  BOOLEAN NOT NULL DEFAULT false,
    active_status          BOOLEAN NOT NULL DEFAULT true,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_transaction_types PRIMARY KEY (transaction_type),
    CONSTRAINT chk_transaction_type_format CHECK (transaction_type ~ '^[A-Z0-9]{2}$'),
    CONSTRAINT chk_type_description_length CHECK (LENGTH(TRIM(type_description)) >= 3)
);

-- Create indexes for optimal query performance in test scenarios
CREATE INDEX IF NOT EXISTS idx_transaction_types_active 
    ON transaction_types (active_status, transaction_type);

CREATE INDEX IF NOT EXISTS idx_transaction_types_debit_credit 
    ON transaction_types (debit_credit_indicator, transaction_type);

-- ====================================================================
-- Transaction Categories Reference Table  
-- Based on COBOL copybook CVTRA04Y.cpy: TRAN-CAT-RECORD (RECLN = 60)
-- Original structure: TRAN-TYPE-CD (PIC X(02)), TRAN-CAT-CD (PIC 9(04)), TRAN-CAT-TYPE-DESC (PIC X(50))
-- ====================================================================

CREATE TABLE IF NOT EXISTS transaction_categories (
    transaction_category    VARCHAR(4) NOT NULL,
    transaction_type       VARCHAR(2) NOT NULL,
    category_description   VARCHAR(60) NOT NULL,
    active_status         BOOLEAN NOT NULL DEFAULT true,
    processing_priority   INTEGER NOT NULL DEFAULT 100,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_transaction_categories PRIMARY KEY (transaction_category),
    CONSTRAINT fk_transaction_categories_type 
        FOREIGN KEY (transaction_type) REFERENCES transaction_types(transaction_type) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_transaction_category_format CHECK (transaction_category ~ '^[0-9]{4}$'),
    CONSTRAINT chk_category_description_length CHECK (LENGTH(TRIM(category_description)) >= 3),
    CONSTRAINT chk_processing_priority_range CHECK (processing_priority BETWEEN 1 AND 999)
);

-- Create indexes for transaction classification and validation
CREATE INDEX IF NOT EXISTS idx_transaction_categories_type 
    ON transaction_categories (transaction_type, active_status);

CREATE INDEX IF NOT EXISTS idx_transaction_categories_active 
    ON transaction_categories (active_status, transaction_category);

CREATE INDEX IF NOT EXISTS idx_transaction_categories_priority 
    ON transaction_categories (processing_priority, transaction_category);

-- ====================================================================
-- Insert Test Reference Data for Transaction Types
-- Supporting transaction classification testing and validation workflows
-- ====================================================================

INSERT INTO transaction_types (transaction_type, type_description, debit_credit_indicator, active_status) VALUES
    ('01', 'Purchase Transaction - Retail Point of Sale', false, true),
    ('02', 'Cash Advance Transaction - ATM Withdrawal', false, true),
    ('03', 'Payment Transaction - Customer Payment', true, true),
    ('04', 'Refund Transaction - Merchant Credit', true, true),
    ('05', 'Transfer Transaction - Balance Transfer', false, true),
    ('06', 'Fee Transaction - Service Charges', false, true),
    ('07', 'Interest Transaction - Finance Charges', false, true),
    ('08', 'Adjustment Transaction - Manual Correction', true, true),
    ('09', 'Reversal Transaction - Transaction Cancellation', true, true),
    ('10', 'Authorization Transaction - Pre-Authorization Hold', false, true),
    ('11', 'Settlement Transaction - Batch Processing', false, true),
    ('12', 'Inquiry Transaction - Balance Inquiry', false, true),
    ('99', 'Test Transaction - Integration Testing Only', false, false)
ON CONFLICT (transaction_type) DO NOTHING;

-- ====================================================================
-- Insert Test Reference Data for Transaction Categories
-- Supporting comprehensive transaction validation scenarios
-- ====================================================================

INSERT INTO transaction_categories (transaction_category, transaction_type, category_description, active_status, processing_priority) VALUES
    -- Purchase Transaction Categories (Type 01)
    ('0101', '01', 'Grocery Store Purchase', true, 100),
    ('0102', '01', 'Gas Station Purchase', true, 100),
    ('0103', '01', 'Restaurant Purchase', true, 100),
    ('0104', '01', 'Online Retail Purchase', true, 100),
    ('0105', '01', 'Department Store Purchase', true, 100),
    
    -- Cash Advance Categories (Type 02)
    ('0201', '02', 'ATM Cash Withdrawal', true, 200),
    ('0202', '02', 'Bank Teller Cash Advance', true, 200),
    ('0203', '02', 'Cash Equivalent Purchase', true, 200),
    
    -- Payment Categories (Type 03)
    ('0301', '03', 'Online Payment', true, 50),
    ('0302', '03', 'Mail Payment', true, 60),
    ('0303', '03', 'Phone Payment', true, 55),
    ('0304', '03', 'Automatic Payment', true, 40),
    ('0305', '03', 'Branch Payment', true, 70),
    
    -- Refund Categories (Type 04)
    ('0401', '04', 'Merchant Credit Refund', true, 150),
    ('0402', '04', 'Disputed Transaction Refund', true, 120),
    ('0403', '04', 'Returned Item Refund', true, 150),
    
    -- Transfer Categories (Type 05)
    ('0501', '05', 'Balance Transfer In', true, 300),
    ('0502', '05', 'Balance Transfer Out', true, 300),
    ('0503', '05', 'Promotional Balance Transfer', true, 250),
    
    -- Fee Categories (Type 06)
    ('0601', '06', 'Annual Fee', true, 400),
    ('0602', '06', 'Late Payment Fee', true, 350),
    ('0603', '06', 'Over Limit Fee', true, 350),
    ('0604', '06', 'Cash Advance Fee', true, 300),
    ('0605', '06', 'Foreign Transaction Fee', true, 200),
    ('0606', '06', 'Balance Transfer Fee', true, 300),
    
    -- Interest Categories (Type 07)
    ('0701', '07', 'Purchase Interest', true, 500),
    ('0702', '07', 'Cash Advance Interest', true, 500),
    ('0703', '07', 'Balance Transfer Interest', true, 500),
    ('0704', '07', 'Promotional Interest', true, 450),
    
    -- Adjustment Categories (Type 08)
    ('0801', '08', 'Credit Adjustment', true, 600),
    ('0802', '08', 'Debit Adjustment', true, 600),
    ('0803', '08', 'System Correction', true, 650),
    
    -- Reversal Categories (Type 09)
    ('0901', '09', 'Purchase Reversal', true, 100),
    ('0902', '09', 'Payment Reversal', true, 150),
    ('0903', '09', 'Fee Reversal', true, 200),
    
    -- Authorization Categories (Type 10)
    ('1001', '10', 'Purchase Authorization', true, 10),
    ('1002', '10', 'Cash Advance Authorization', true, 10),
    ('1003', '10', 'Pre-Authorization Hold', true, 15),
    
    -- Settlement Categories (Type 11)
    ('1101', '11', 'Daily Settlement', true, 800),
    ('1102', '11', 'End of Month Settlement', true, 850),
    ('1103', '11', 'End of Year Settlement', true, 900),
    
    -- Inquiry Categories (Type 12)
    ('1201', '12', 'Balance Inquiry', true, 5),
    ('1202', '12', 'Transaction History Inquiry', true, 5),
    ('1203', '12', 'Account Status Inquiry', true, 5),
    
    -- Test Categories (Type 99)
    ('9901', '99', 'Unit Test Transaction', false, 999),
    ('9902', '99', 'Integration Test Transaction', false, 999),
    ('9903', '99', 'Performance Test Transaction', false, 999)
ON CONFLICT (transaction_category) DO NOTHING;

-- ====================================================================
-- Create Additional Support Objects for Integration Testing
-- ====================================================================

-- View for active transaction type and category combinations
CREATE OR REPLACE VIEW v_active_transaction_classification AS
SELECT 
    tt.transaction_type,
    tt.type_description,
    tt.debit_credit_indicator,
    tc.transaction_category,
    tc.category_description,
    tc.processing_priority,
    CASE 
        WHEN tt.debit_credit_indicator = true THEN 'CREDIT'
        ELSE 'DEBIT'
    END as transaction_flow,
    CONCAT(tt.transaction_type, '-', tc.transaction_category) as classification_code
FROM transaction_types tt
INNER JOIN transaction_categories tc ON tt.transaction_type = tc.transaction_type
WHERE tt.active_status = true 
  AND tc.active_status = true
ORDER BY tt.transaction_type, tc.processing_priority, tc.transaction_category;

-- Function to validate transaction type and category combination
CREATE OR REPLACE FUNCTION validate_transaction_classification(
    p_transaction_type VARCHAR(2),
    p_transaction_category VARCHAR(4)
) RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    -- Check if the transaction type and category combination exists and is active
    RETURN EXISTS (
        SELECT 1 
        FROM v_active_transaction_classification 
        WHERE transaction_type = p_transaction_type 
          AND transaction_category = p_transaction_category
    );
END;
$$;

-- ====================================================================
-- Performance and Statistics Maintenance
-- ====================================================================

-- Update table statistics for optimal query planning in test scenarios
ANALYZE transaction_types;
ANALYZE transaction_categories;

-- Create comments for documentation and test understanding
COMMENT ON TABLE transaction_types IS 
    'Reference table for transaction types based on COBOL copybook CVTRA03Y.cpy. ' ||
    'Supports transaction classification and validation in test environment.';

COMMENT ON TABLE transaction_categories IS 
    'Reference table for transaction categories based on COBOL copybook CVTRA04Y.cpy. ' ||
    'Provides detailed transaction classification for integration testing scenarios.';

COMMENT ON VIEW v_active_transaction_classification IS 
    'Active transaction type and category combinations for test validation workflows. ' ||
    'Includes classification codes and transaction flow indicators.';

COMMENT ON FUNCTION validate_transaction_classification IS 
    'Validates transaction type and category combination for test data validation. ' ||
    'Returns true if combination exists and is active, false otherwise.';

--rollback DROP FUNCTION IF EXISTS validate_transaction_classification(VARCHAR(2), VARCHAR(4));
--rollback DROP VIEW IF EXISTS v_active_transaction_classification;
--rollback DROP TABLE IF EXISTS transaction_categories;
--rollback DROP TABLE IF EXISTS transaction_types;