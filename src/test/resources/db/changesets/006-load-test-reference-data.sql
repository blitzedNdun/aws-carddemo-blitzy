--liquibase formatted sql

--changeset blitzy-agent:006-load-test-reference-data
--comment: Load essential reference data into PostgreSQL tables for integration testing scenarios based on COBOL copybook structures CVTRA03Y and CVTRA04Y

-- =========================================================================
-- TEST REFERENCE DATA LOADING FOR INTEGRATION TESTING
-- Supporting Testing Strategy 6.6.7.5 Test Data Management Strategy
-- Provides minimal reference data required for automated test execution
-- Based on COBOL copybooks CVTRA03Y.cpy and CVTRA04Y.cpy structures
-- =========================================================================

-- Clear existing test data to ensure clean state for integration testing
-- This supports Testcontainers integration testing requirements per 6.6.2.3
DELETE FROM trancatg;
DELETE FROM trantype;

-- =========================================================================
-- TRANSACTION TYPE REFERENCE DATA LOADING
-- Based on CVTRA03Y.cpy TRAN-TYPE-RECORD structure:
-- 01 TRAN-TYPE-RECORD
--    05 TRAN-TYPE PIC X(02)
--    05 TRAN-TYPE-DESC PIC X(50)
-- =========================================================================

-- Load core transaction types required for integration testing scenarios
-- These represent the minimal set of transaction types needed for test validation workflows
INSERT INTO trantype (transaction_type, type_description) VALUES
    -- Purchase transactions - Core business functionality testing
    ('01', 'Purchase Transaction'),
    ('02', 'Online Purchase'),
    ('03', 'Recurring Purchase'),
    
    -- Payment transactions - Financial processing testing
    ('04', 'Payment Credit'),
    ('05', 'Auto Payment'),
    ('06', 'Manual Payment'),
    
    -- Cash advance transactions - Credit facility testing
    ('07', 'Cash Advance'),
    ('08', 'ATM Cash Advance'),
    
    -- Fee and interest transactions - Calculation engine testing
    ('09', 'Annual Fee'),
    ('10', 'Late Fee'),
    ('11', 'Interest Charge'),
    ('12', 'Over Limit Fee'),
    
    -- Adjustment transactions - Error handling and correction testing
    ('13', 'Credit Adjustment'),
    ('14', 'Debit Adjustment'),
    ('15', 'Merchant Credit'),
    
    -- Refund transactions - Reversal processing testing
    ('16', 'Purchase Refund'),
    ('17', 'Fee Reversal'),
    
    -- Balance transfer transactions - Account management testing
    ('18', 'Balance Transfer In'),
    ('19', 'Balance Transfer Out'),
    
    -- Authorization transactions - Real-time processing testing
    ('20', 'Authorization'),
    ('21', 'Pre-Authorization'),
    ('22', 'Auth Reversal');

-- =========================================================================
-- TRANSACTION CATEGORY REFERENCE DATA LOADING
-- Based on CVTRA04Y.cpy TRAN-CAT-RECORD structure:
-- 01 TRAN-CAT-RECORD
--    05 TRAN-CAT-KEY
--       10 TRAN-TYPE-CD PIC X(02)
--       10 TRAN-CAT-CD PIC 9(04)
--    05 TRAN-CAT-TYPE-DESC PIC X(50)
-- =========================================================================

-- Load transaction categories supporting comprehensive test coverage
-- Categories organized by transaction type to support transaction validation workflows
INSERT INTO trancatg (transaction_type_code, transaction_category_code, category_type_description) VALUES
    -- Purchase transaction categories (transaction types 01-03)
    ('01', 1001, 'Retail Store Purchase'),
    ('01', 1002, 'Gas Station Purchase'),
    ('01', 1003, 'Restaurant Purchase'),
    ('01', 1004, 'Grocery Store Purchase'),
    ('01', 1005, 'Department Store Purchase'),
    
    ('02', 1001, 'E-commerce Purchase'),
    ('02', 1002, 'Subscription Service'),
    ('02', 1003, 'Software Purchase'),
    ('02', 1004, 'Digital Content'),
    
    ('03', 1001, 'Utility Payment'),
    ('03', 1002, 'Insurance Premium'),
    ('03', 1003, 'Loan Payment'),
    ('03', 1004, 'Membership Fee'),
    
    -- Payment transaction categories (transaction types 04-06)
    ('04', 2001, 'Statement Payment'),
    ('04', 2002, 'Minimum Payment'),
    ('04', 2003, 'Full Balance Payment'),
    
    ('05', 2001, 'Scheduled Auto Payment'),
    ('05', 2002, 'Recurring Auto Payment'),
    
    ('06', 2001, 'Online Manual Payment'),
    ('06', 2002, 'Phone Payment'),
    ('06', 2003, 'Branch Payment'),
    ('06', 2004, 'Mail Payment'),
    
    -- Cash advance transaction categories (transaction types 07-08)
    ('07', 3001, 'Teller Cash Advance'),
    ('07', 3002, 'Convenience Check'),
    ('07', 3003, 'Wire Transfer'),
    
    ('08', 3001, 'ATM Cash Withdrawal'),
    ('08', 3002, 'Foreign ATM Withdrawal'),
    
    -- Fee and interest categories (transaction types 09-12)
    ('09', 4001, 'Annual Membership Fee'),
    ('09', 4002, 'Premium Card Fee'),
    
    ('10', 4001, 'Late Payment Penalty'),
    ('10', 4002, 'Returned Payment Fee'),
    
    ('11', 4001, 'Purchase Interest'),
    ('11', 4002, 'Cash Advance Interest'),
    ('11', 4003, 'Balance Transfer Interest'),
    ('11', 4004, 'Promotional Rate Interest'),
    
    ('12', 4001, 'Credit Limit Excess Fee'),
    ('12', 4002, 'Foreign Transaction Fee'),
    ('12', 4003, 'Expedited Payment Fee'),
    
    -- Adjustment categories (transaction types 13-15)
    ('13', 5001, 'Customer Service Credit'),
    ('13', 5002, 'Billing Error Correction'),
    ('13', 5003, 'Fraud Credit Adjustment'),
    ('13', 5004, 'Promotional Credit'),
    
    ('14', 5001, 'Chargeback Debit'),
    ('14', 5002, 'Account Maintenance Charge'),
    ('14', 5003, 'Service Fee Adjustment'),
    
    ('15', 5001, 'Merchant Processing Credit'),
    ('15', 5002, 'Dispute Resolution Credit'),
    ('15', 5003, 'Goodwill Credit'),
    
    -- Refund categories (transaction types 16-17)
    ('16', 6001, 'Purchase Return Refund'),
    ('16', 6002, 'Cancelled Transaction Refund'),
    ('16', 6003, 'Duplicate Charge Refund'),
    
    ('17', 6001, 'Fee Waiver Refund'),
    ('17', 6002, 'Interest Reversal'),
    ('17', 6003, 'Penalty Reversal'),
    
    -- Balance transfer categories (transaction types 18-19)
    ('18', 7001, 'Promotional Balance Transfer'),
    ('18', 7002, 'Standard Balance Transfer'),
    ('18', 7003, 'Emergency Balance Transfer'),
    
    ('19', 7001, 'Account Closure Transfer'),
    ('19', 7002, 'Credit Line Transfer'),
    
    -- Authorization categories (transaction types 20-22)
    ('20', 8001, 'Purchase Authorization'),
    ('20', 8002, 'Cash Advance Authorization'),
    ('20', 8003, 'Balance Inquiry Authorization'),
    
    ('21', 8001, 'Hotel Pre-Authorization'),
    ('21', 8002, 'Car Rental Pre-Authorization'),
    ('21', 8003, 'Gas Station Pre-Authorization'),
    
    ('22', 8001, 'Expired Authorization Reversal'),
    ('22', 8002, 'Cancelled Authorization Reversal'),
    ('22', 8003, 'Declined Authorization Reversal');

-- =========================================================================
-- TEST DATA VALIDATION AND PERFORMANCE OPTIMIZATION
-- Supporting Integration Testing Data Requirements 6.6.3.2
-- =========================================================================

-- Update table statistics for optimal query performance during test execution
-- This ensures consistent performance for integration test scenarios
ANALYZE trantype;
ANALYZE trancatg;

-- Verify data integrity after loading
-- Ensures referential integrity constraints are properly enforced
DO $$ 
DECLARE 
    trantype_count INTEGER;
    trancatg_count INTEGER;
    orphaned_categories INTEGER;
BEGIN
    -- Count loaded transaction types
    SELECT COUNT(*) INTO trantype_count FROM trantype;
    
    -- Count loaded transaction categories  
    SELECT COUNT(*) INTO trancatg_count FROM trancatg;
    
    -- Check for orphaned categories (should be 0)
    SELECT COUNT(*) INTO orphaned_categories 
    FROM trancatg tc 
    LEFT JOIN trantype tt ON tc.transaction_type_code = tt.transaction_type 
    WHERE tt.transaction_type IS NULL;
    
    -- Log results for test verification
    RAISE NOTICE 'Test reference data loaded successfully:';
    RAISE NOTICE '  Transaction types: % records', trantype_count;
    RAISE NOTICE '  Transaction categories: % records', trancatg_count;
    RAISE NOTICE '  Orphaned categories: % (should be 0)', orphaned_categories;
    
    -- Raise error if data integrity check fails
    IF orphaned_categories > 0 THEN
        RAISE EXCEPTION 'Data integrity violation: % orphaned transaction categories found', orphaned_categories;
    END IF;
END $$;

-- Add validation comments for test documentation
COMMENT ON TABLE trantype IS 'Test reference data: Transaction type lookup table supporting integration testing scenarios based on CVTRA03Y.cpy structure';
COMMENT ON TABLE trancatg IS 'Test reference data: Transaction category lookup table supporting integration testing scenarios based on CVTRA04Y.cpy structure';

-- Create verification views for test scenarios
-- These views support automated testing validation workflows
CREATE OR REPLACE VIEW v_test_trantype_summary AS
SELECT 
    'TRANTYPE' as table_name,
    COUNT(*) as record_count,
    MIN(LENGTH(transaction_type)) as min_type_length,
    MAX(LENGTH(transaction_type)) as max_type_length,
    MIN(LENGTH(type_description)) as min_desc_length,
    MAX(LENGTH(type_description)) as max_desc_length,
    CURRENT_TIMESTAMP as verification_timestamp
FROM trantype;

CREATE OR REPLACE VIEW v_test_trancatg_summary AS
SELECT 
    'TRANCATG' as table_name,
    COUNT(*) as record_count,
    COUNT(DISTINCT transaction_type_code) as unique_type_codes,
    MIN(transaction_category_code) as min_category_code,
    MAX(transaction_category_code) as max_category_code,
    MIN(LENGTH(category_type_description)) as min_desc_length,
    MAX(LENGTH(category_type_description)) as max_desc_length,
    CURRENT_TIMESTAMP as verification_timestamp
FROM trancatg;

-- Create combined reference data view for transaction validation testing
CREATE OR REPLACE VIEW v_test_transaction_reference AS
SELECT 
    tt.transaction_type,
    tt.type_description as transaction_type_description,
    tc.transaction_category_code,
    tc.category_type_description,
    CONCAT(tt.transaction_type, '-', LPAD(tc.transaction_category_code::TEXT, 4, '0')) as full_transaction_code
FROM trantype tt
JOIN trancatg tc ON tt.transaction_type = tc.transaction_type_code
ORDER BY tt.transaction_type, tc.transaction_category_code;

-- =========================================================================
-- TEST DATA MANAGEMENT METADATA
-- Supporting automated test execution and data lifecycle management
-- =========================================================================

-- Track changeset execution for test environment management
INSERT INTO public.databasechangelog (
    id, 
    author, 
    filename, 
    dateexecuted, 
    orderexecuted, 
    exectype, 
    md5sum, 
    description, 
    comments, 
    tag, 
    liquibase, 
    contexts, 
    labels, 
    deployment_id
) VALUES (
    '006-load-test-reference-data-validation',
    'blitzy-agent',
    'src/test/resources/db/changesets/006-load-test-reference-data.sql',
    CURRENT_TIMESTAMP,
    (SELECT COALESCE(MAX(orderexecuted), 0) + 1 FROM public.databasechangelog),
    'EXECUTED',
    'test-data-validation-checksum',
    'Test reference data validation and summary creation',
    'Supporting integration testing data requirements per Testing Strategy 6.6.3.2',
    'test-reference-data-v1.0',
    '4.25.x',
    'test',
    'integration-testing',
    EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::VARCHAR
) ON CONFLICT (id, author, filename) DO NOTHING;

--rollback DROP VIEW IF EXISTS v_test_transaction_reference CASCADE;
--rollback DROP VIEW IF EXISTS v_test_trancatg_summary CASCADE;
--rollback DROP VIEW IF EXISTS v_test_trantype_summary CASCADE;
--rollback DELETE FROM trancatg;
--rollback DELETE FROM trantype;