--liquibase formatted sql

--changeset blitzy-agent:006-load-test-reference-data
--comment: Load essential test reference data for integration testing scenarios

-- ====================================================================
-- LIQUIBASE CHANGESET: 006-load-test-reference-data.sql
-- Purpose: Load minimal reference data required for integration testing functionality
-- Dependencies: 004-create-test-reference-tables.sql (creates target tables)
-- Source: COBOL copybooks CVTRA03Y.cpy and CVTRA04Y.cpy
-- ====================================================================

-- ====================================================================
-- Load Additional Test Transaction Types for Integration Testing
-- Supporting edge cases and specific test scenarios not covered by base data
-- Based on CVTRA03Y.cpy: TRAN-TYPE-RECORD (PIC X(02), PIC X(50))
-- ====================================================================

INSERT INTO transaction_types (transaction_type, type_description, debit_credit_indicator, active_status, created_at, updated_at) VALUES
    -- Edge Case Transaction Types for Integration Testing
    ('20', 'Void Transaction - Payment Reversal', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('21', 'Dispute Transaction - Chargeback Processing', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22', 'Recurring Payment - Subscription Processing', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('23', 'Foreign Exchange Transaction - Currency Conversion', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('24', 'Installment Payment - Loan Processing', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test-Specific Transaction Types for Validation Workflows
    ('T1', 'Integration Test Type - Account Creation', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T2', 'Integration Test Type - Balance Verification', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T3', 'Integration Test Type - Card Activation', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T4', 'Integration Test Type - Transaction Validation', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T5', 'Integration Test Type - Error Handling', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Boundary Value Testing Transaction Types
    ('ZZ', 'Boundary Test - Maximum Type Value', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00', 'Boundary Test - Minimum Type Value', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Inactive Transaction Types for Status Testing
    ('X1', 'Inactive Test Type - Status Testing', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('X2', 'Inactive Test Type - Filter Testing', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    
ON CONFLICT (transaction_type) DO UPDATE SET
    type_description = EXCLUDED.type_description,
    debit_credit_indicator = EXCLUDED.debit_credit_indicator,
    active_status = EXCLUDED.active_status,
    updated_at = CURRENT_TIMESTAMP;

-- ====================================================================
-- Load Additional Test Transaction Categories for Integration Testing
-- Supporting comprehensive transaction validation scenarios
-- Based on CVTRA04Y.cpy: TRAN-CAT-RECORD (PIC X(02), PIC 9(04), PIC X(50))
-- ====================================================================

INSERT INTO transaction_categories (transaction_category, transaction_type, category_description, active_status, processing_priority, created_at, updated_at) VALUES
    -- Edge Case Categories for Advanced Transaction Types
    ('2001', '20', 'Void Payment - Full Reversal', true, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2002', '20', 'Void Payment - Partial Reversal', true, 55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2101', '21', 'Merchant Dispute - Product Return', true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2102', '21', 'Merchant Dispute - Service Issue', true, 105, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2201', '22', 'Recurring Payment - Monthly Subscription', true, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2202', '22', 'Recurring Payment - Annual Subscription', true, 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2301', '23', 'Foreign Exchange - EUR to USD', true, 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2302', '23', 'Foreign Exchange - GBP to USD', true, 155, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2401', '24', 'Installment Payment - Auto Loan', true, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2402', '24', 'Installment Payment - Personal Loan', true, 125, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Integration Test Categories for Automated Testing
    ('T101', 'T1', 'Integration Test - New Account Setup', true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T102', 'T1', 'Integration Test - Account Modification', true, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T201', 'T2', 'Integration Test - Balance Inquiry', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T202', 'T2', 'Integration Test - Balance Update', true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T301', 'T3', 'Integration Test - Card Activation', true, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T302', 'T3', 'Integration Test - Card Deactivation', true, 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T401', 'T4', 'Integration Test - Transaction Posting', true, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T402', 'T4', 'Integration Test - Transaction Approval', true, 35, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T501', 'T5', 'Integration Test - Error Simulation', true, 999, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('T502', 'T5', 'Integration Test - Exception Handling', true, 998, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Boundary Value Testing Categories
    ('9999', 'ZZ', 'Boundary Test - Maximum Category Value', true, 999, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0001', '00', 'Boundary Test - Minimum Category Value', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Inactive Categories for Status Testing
    ('X101', 'X1', 'Inactive Test Category - Status Validation', false, 500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('X201', 'X2', 'Inactive Test Category - Filter Validation', false, 501, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Test Categories for Existing Transaction Types (complement base data)
    ('0106', '01', 'Test Purchase - Hardware Store', true, 105, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0107', '01', 'Test Purchase - Pharmacy', true, 110, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0108', '01', 'Test Purchase - Bookstore', true, 115, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0204', '02', 'Test Cash Advance - Foreign ATM', true, 210, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0205', '02', 'Test Cash Advance - Emergency Withdrawal', true, 220, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0306', '03', 'Test Payment - Mobile App Payment', true, 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0307', '03', 'Test Payment - Bank Transfer', true, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0404', '04', 'Test Refund - Warranty Return', true, 140, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0405', '04', 'Test Refund - Duplicate Payment', true, 145, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0504', '05', 'Test Balance Transfer - Promotional Rate', true, 280, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0607', '06', 'Test Fee - Overlimit Prevention', true, 320, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0608', '06', 'Test Fee - Card Replacement', true, 310, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0705', '07', 'Test Interest - Penalty Interest', true, 520, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0804', '08', 'Test Adjustment - Interest Correction', true, 620, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0904', '09', 'Test Reversal - Authorization Reversal', true, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1004', '10', 'Test Authorization - Contactless Payment', true, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1104', '11', 'Test Settlement - Cross-Border Settlement', true, 820, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('1204', '12', 'Test Inquiry - Credit Limit Inquiry', true, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    
ON CONFLICT (transaction_category) DO UPDATE SET
    transaction_type = EXCLUDED.transaction_type,
    category_description = EXCLUDED.category_description,
    active_status = EXCLUDED.active_status,
    processing_priority = EXCLUDED.processing_priority,
    updated_at = CURRENT_TIMESTAMP;

-- ====================================================================
-- Create Test Data Validation Support Functions
-- Support integration testing with validation utilities
-- ====================================================================

-- Function to validate test transaction classification combinations
CREATE OR REPLACE FUNCTION validate_test_transaction_classification(
    p_transaction_type VARCHAR(2),
    p_transaction_category VARCHAR(4)
) RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    -- Validate that test transaction type and category combination exists and is active
    RETURN EXISTS (
        SELECT 1 
        FROM transaction_types tt
        INNER JOIN transaction_categories tc ON tt.transaction_type = tc.transaction_type
        WHERE tt.transaction_type = p_transaction_type 
          AND tc.transaction_category = p_transaction_category
          AND tt.active_status = true 
          AND tc.active_status = true
    );
END;
$$;

-- Function to get test transaction categories by type
CREATE OR REPLACE FUNCTION get_test_transaction_categories_by_type(
    p_transaction_type VARCHAR(2)
) RETURNS TABLE(
    transaction_category VARCHAR(4),
    category_description VARCHAR(60),
    processing_priority INTEGER
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tc.transaction_category,
        tc.category_description,
        tc.processing_priority
    FROM transaction_categories tc
    INNER JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
    WHERE tt.transaction_type = p_transaction_type
      AND tt.active_status = true
      AND tc.active_status = true
    ORDER BY tc.processing_priority, tc.transaction_category;
END;
$$;

-- ====================================================================
-- Create Integration Test Support Views
-- Provide convenient access to test reference data
-- ====================================================================

-- View for active test transaction types and categories
CREATE OR REPLACE VIEW v_test_transaction_reference AS
SELECT 
    tt.transaction_type,
    tt.type_description,
    tt.debit_credit_indicator,
    tc.transaction_category,
    tc.category_description,
    tc.processing_priority,
    tc.active_status as category_active,
    tt.active_status as type_active,
    CASE 
        WHEN tt.debit_credit_indicator = true THEN 'CREDIT'
        ELSE 'DEBIT'
    END as transaction_flow,
    CASE 
        WHEN tt.transaction_type LIKE 'T%' THEN 'INTEGRATION_TEST'
        WHEN tt.transaction_type LIKE 'X%' THEN 'INACTIVE_TEST'
        WHEN tt.transaction_type LIKE '0%' THEN 'BOUNDARY_TEST'
        WHEN tt.transaction_type LIKE 'Z%' THEN 'BOUNDARY_TEST'
        ELSE 'PRODUCTION_EQUIVALENT'
    END as test_category_type,
    CONCAT(tt.transaction_type, '-', tc.transaction_category) as test_classification_code
FROM transaction_types tt
INNER JOIN transaction_categories tc ON tt.transaction_type = tc.transaction_type
ORDER BY tt.transaction_type, tc.processing_priority, tc.transaction_category;

-- View for integration test specific data
CREATE OR REPLACE VIEW v_integration_test_data AS
SELECT 
    tt.transaction_type,
    tt.type_description,
    tc.transaction_category,
    tc.category_description,
    tc.processing_priority,
    CASE 
        WHEN tt.transaction_type = 'T1' THEN 'ACCOUNT_MANAGEMENT'
        WHEN tt.transaction_type = 'T2' THEN 'BALANCE_OPERATIONS'
        WHEN tt.transaction_type = 'T3' THEN 'CARD_OPERATIONS'
        WHEN tt.transaction_type = 'T4' THEN 'TRANSACTION_PROCESSING'
        WHEN tt.transaction_type = 'T5' THEN 'ERROR_HANDLING'
        ELSE 'GENERAL_TESTING'
    END as test_functional_area,
    CASE 
        WHEN tc.processing_priority < 50 THEN 'HIGH_PRIORITY'
        WHEN tc.processing_priority < 200 THEN 'MEDIUM_PRIORITY'
        ELSE 'LOW_PRIORITY'
    END as test_execution_priority
FROM transaction_types tt
INNER JOIN transaction_categories tc ON tt.transaction_type = tc.transaction_type
WHERE tt.transaction_type LIKE 'T%'
  AND tt.active_status = true
  AND tc.active_status = true
ORDER BY tc.processing_priority, tt.transaction_type, tc.transaction_category;

-- ====================================================================
-- Update Table Statistics for Query Optimization
-- Ensure optimal query performance for integration testing
-- ====================================================================

-- Update PostgreSQL statistics for test data
ANALYZE transaction_types;
ANALYZE transaction_categories;

-- ====================================================================
-- Add Documentation Comments for Test Data
-- Support integration testing understanding and maintenance
-- ====================================================================

-- Document test transaction types
COMMENT ON VIEW v_test_transaction_reference IS 
    'Comprehensive view of test transaction types and categories for integration testing. ' ||
    'Includes test classification codes and functional area mapping for automated test execution.';

COMMENT ON VIEW v_integration_test_data IS 
    'Integration test specific transaction reference data with functional area mapping. ' ||
    'Supports automated test execution with priority-based test scenario organization.';

COMMENT ON FUNCTION validate_test_transaction_classification IS 
    'Validates transaction type and category combination for integration testing. ' ||
    'Returns true if combination exists and is active, false otherwise. ' ||
    'Supports test data validation in automated testing workflows.';

COMMENT ON FUNCTION get_test_transaction_categories_by_type IS 
    'Retrieves active transaction categories for a specific transaction type. ' ||
    'Returns categories sorted by processing priority for integration test execution. ' ||
    'Supports test scenario generation and validation workflows.';

-- ====================================================================
-- Test Data Summary Information
-- Document the test data loaded by this changeset
-- ====================================================================

-- Log test data loading completion
DO $$
DECLARE
    test_types_count INTEGER;
    test_categories_count INTEGER;
    integration_test_types_count INTEGER;
    integration_test_categories_count INTEGER;
BEGIN
    -- Count test transaction types
    SELECT COUNT(*) INTO test_types_count 
    FROM transaction_types 
    WHERE transaction_type IN ('20', '21', '22', '23', '24', 'T1', 'T2', 'T3', 'T4', 'T5', 'ZZ', '00', 'X1', 'X2');
    
    -- Count test transaction categories
    SELECT COUNT(*) INTO test_categories_count 
    FROM transaction_categories 
    WHERE transaction_category LIKE '20%' OR transaction_category LIKE '21%' OR transaction_category LIKE '22%' 
       OR transaction_category LIKE '23%' OR transaction_category LIKE '24%' OR transaction_category LIKE 'T%'
       OR transaction_category LIKE 'X%' OR transaction_category IN ('9999', '0001');
    
    -- Count integration test specific types
    SELECT COUNT(*) INTO integration_test_types_count 
    FROM transaction_types 
    WHERE transaction_type LIKE 'T%' AND active_status = true;
    
    -- Count integration test specific categories
    SELECT COUNT(*) INTO integration_test_categories_count 
    FROM transaction_categories 
    WHERE transaction_category LIKE 'T%' AND active_status = true;
    
    -- Log summary information
    RAISE NOTICE 'CardDemo Test Reference Data Load Summary:';
    RAISE NOTICE '  - Test Transaction Types Loaded: %', test_types_count;
    RAISE NOTICE '  - Test Transaction Categories Loaded: %', test_categories_count;
    RAISE NOTICE '  - Integration Test Types: %', integration_test_types_count;
    RAISE NOTICE '  - Integration Test Categories: %', integration_test_categories_count;
    RAISE NOTICE '  - Test validation functions created: 2';
    RAISE NOTICE '  - Test support views created: 2';
    RAISE NOTICE 'Test reference data loading completed successfully for integration testing scenarios.';
END $$;

-- ====================================================================
-- Rollback Instructions
-- Support database migration rollback procedures
-- ====================================================================

--rollback DROP VIEW IF EXISTS v_integration_test_data;
--rollback DROP VIEW IF EXISTS v_test_transaction_reference;
--rollback DROP FUNCTION IF EXISTS get_test_transaction_categories_by_type(VARCHAR(2));
--rollback DROP FUNCTION IF EXISTS validate_test_transaction_classification(VARCHAR(2), VARCHAR(4));
--rollback DELETE FROM transaction_categories WHERE transaction_category IN ('2001', '2002', '2101', '2102', '2201', '2202', '2301', '2302', '2401', '2402', 'T101', 'T102', 'T201', 'T202', 'T301', 'T302', 'T401', 'T402', 'T501', 'T502', '9999', '0001', 'X101', 'X201', '0106', '0107', '0108', '0204', '0205', '0306', '0307', '0404', '0405', '0504', '0607', '0608', '0705', '0804', '0904', '1004', '1104', '1204');
--rollback DELETE FROM transaction_types WHERE transaction_type IN ('20', '21', '22', '23', '24', 'T1', 'T2', 'T3', 'T4', 'T5', 'ZZ', '00', 'X1', 'X2');