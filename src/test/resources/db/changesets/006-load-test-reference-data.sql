-- =====================================================================================
-- Liquibase Changeset: 006-load-test-reference-data.sql
-- Purpose: Load essential test reference data for integration testing scenarios
-- Supporting: CardDemo Spring Boot microservices testing infrastructure
-- Based on: COBOL copybooks CVTRA03Y.cpy (transaction types) and CVTRA04Y.cpy (transaction categories)
-- Dependencies: 004-create-test-reference-tables.sql (creates target tables)
-- Environment: Test environment for automated integration testing validation
-- =====================================================================================

--liquibase formatted sql

--changeset liquibase:006-load-test-reference-data splitStatements:true endDelimiter:;

-- =====================================================================================
-- TEST REFERENCE DATA LOADING STRATEGY
-- Purpose: Provides comprehensive test reference data for integration testing
-- Testing Focus: Transaction validation workflows and business logic testing
-- Data Coverage: Minimal but complete dataset for automated test execution
-- =====================================================================================

-- =====================================================================================
-- TRANSACTION TYPES TEST DATA
-- Maps from: COBOL copybook CVTRA03Y.cpy (TRAN-TYPE-RECORD structure)
-- Fields: TRAN-TYPE PIC X(02), TRAN-TYPE-DESC PIC X(50)
-- Business Purpose: Transaction type classification for test scenarios
-- Testing Support: Integration test validation of transaction processing workflows
-- =====================================================================================

-- Insert comprehensive transaction types for testing scenarios
-- Note: These supplement the base reference data with test-specific scenarios
INSERT INTO transaction_types (transaction_type, type_description, debit_credit_flag, active_status) 
VALUES 
    -- Standard production-equivalent transaction types
    ('11', 'ATM Withdrawal', 'D', TRUE),
    ('12', 'Point of Sale Purchase', 'D', TRUE),
    ('13', 'Online Transaction', 'D', TRUE),
    ('14', 'Phone Order Transaction', 'D', TRUE),
    ('15', 'Mail Order Transaction', 'D', TRUE),
    ('16', 'Recurring Payment', 'D', TRUE),
    ('17', 'Balance Transfer Debit', 'D', TRUE),
    ('18', 'Cash Advance ATM', 'D', TRUE),
    ('19', 'Cash Advance Counter', 'D', TRUE),
    ('20', 'Foreign Transaction', 'D', TRUE),
    
    -- Credit transaction types for testing
    ('21', 'Payment Posted', 'C', TRUE),
    ('22', 'Refund Credit', 'C', TRUE),
    ('23', 'Adjustment Credit', 'C', TRUE),
    ('24', 'Interest Reversal', 'C', TRUE),
    ('25', 'Fee Reversal', 'C', TRUE),
    ('26', 'Chargeback Credit', 'C', TRUE),
    ('27', 'Dispute Resolution Credit', 'C', TRUE),
    ('28', 'Promotional Credit', 'C', TRUE),
    ('29', 'Cashback Reward', 'C', TRUE),
    ('30', 'Statement Credit', 'C', TRUE),
    
    -- Fee transaction types for testing
    ('31', 'Annual Fee', 'D', TRUE),
    ('32', 'Late Payment Fee', 'D', TRUE),
    ('33', 'Over Limit Fee', 'D', TRUE),
    ('34', 'Cash Advance Fee', 'D', TRUE),
    ('35', 'Foreign Transaction Fee', 'D', TRUE),
    ('36', 'Balance Transfer Fee', 'D', TRUE),
    ('37', 'Returned Payment Fee', 'D', TRUE),
    ('38', 'Expedited Payment Fee', 'D', TRUE),
    ('39', 'Account Maintenance Fee', 'D', TRUE),
    ('40', 'Overlimit Protection Fee', 'D', TRUE),
    
    -- Interest transaction types for testing
    ('41', 'Purchase Interest', 'D', TRUE),
    ('42', 'Cash Advance Interest', 'D', TRUE),
    ('43', 'Balance Transfer Interest', 'D', TRUE),
    ('44', 'Penalty Interest', 'D', TRUE),
    ('45', 'Promotional Interest', 'D', TRUE),
    
    -- Test-specific transaction types (inactive for edge case testing)
    ('91', 'Test Transaction Type A', 'D', FALSE),
    ('92', 'Test Transaction Type B', 'C', FALSE),
    ('93', 'Test Transaction Type C', 'D', FALSE),
    ('94', 'Test Edge Case Type', 'C', FALSE),
    ('95', 'Test Validation Type', 'D', FALSE),
    
    -- System transaction types for testing
    ('96', 'System Adjustment', 'D', TRUE),
    ('97', 'System Correction', 'C', TRUE),
    ('98', 'System Reversal', 'C', TRUE),
    ('99', 'System Test Transaction', 'D', FALSE)
ON CONFLICT (transaction_type) DO UPDATE SET
    type_description = EXCLUDED.type_description,
    debit_credit_flag = EXCLUDED.debit_credit_flag,
    active_status = EXCLUDED.active_status,
    last_updated = CURRENT_TIMESTAMP;

-- =====================================================================================
-- TRANSACTION CATEGORIES TEST DATA
-- Maps from: COBOL copybook CVTRA04Y.cpy (TRAN-CAT-RECORD structure)
-- Fields: TRAN-CAT-KEY (TRAN-TYPE-CD + TRAN-CAT-CD), TRAN-CAT-TYPE-DESC PIC X(50)
-- Composite Key: transaction_type + transaction_category
-- Business Purpose: Detailed transaction categorization for test scenarios
-- Testing Support: Complex transaction classification and validation testing
-- =====================================================================================

-- Insert comprehensive transaction categories for testing scenarios
INSERT INTO transaction_categories (transaction_type, transaction_category, category_description, active_status, risk_level, processing_priority)
VALUES 
    -- ATM Withdrawal categories (Type 11)
    ('11', '1001', 'ATM Withdrawal - Own Bank', TRUE, 'L', 7),
    ('11', '1002', 'ATM Withdrawal - Other Bank', TRUE, 'L', 6),
    ('11', '1003', 'ATM Withdrawal - International', TRUE, 'H', 4),
    ('11', '1004', 'ATM Withdrawal - Surcharge', TRUE, 'L', 6),
    
    -- Point of Sale categories (Type 12)
    ('12', '1201', 'POS - Grocery Store', TRUE, 'L', 8),
    ('12', '1202', 'POS - Gas Station', TRUE, 'L', 8),
    ('12', '1203', 'POS - Restaurant', TRUE, 'L', 7),
    ('12', '1204', 'POS - Retail Store', TRUE, 'L', 7),
    ('12', '1205', 'POS - Pharmacy', TRUE, 'L', 7),
    ('12', '1206', 'POS - Department Store', TRUE, 'L', 6),
    ('12', '1207', 'POS - Electronics Store', TRUE, 'M', 5),
    ('12', '1208', 'POS - Jewelry Store', TRUE, 'H', 4),
    
    -- Online Transaction categories (Type 13)
    ('13', '1301', 'Online - E-commerce', TRUE, 'M', 5),
    ('13', '1302', 'Online - Subscription Service', TRUE, 'M', 6),
    ('13', '1303', 'Online - Digital Download', TRUE, 'L', 6),
    ('13', '1304', 'Online - Gaming', TRUE, 'M', 5),
    ('13', '1305', 'Online - Travel Booking', TRUE, 'M', 5),
    ('13', '1306', 'Online - Utility Payment', TRUE, 'L', 7),
    ('13', '1307', 'Online - International', TRUE, 'H', 3),
    
    -- Phone Order categories (Type 14)
    ('14', '1401', 'Phone Order - Retail', TRUE, 'M', 5),
    ('14', '1402', 'Phone Order - Catalog', TRUE, 'L', 6),
    ('14', '1403', 'Phone Order - Service', TRUE, 'L', 6),
    ('14', '1404', 'Phone Order - International', TRUE, 'H', 4),
    
    -- Mail Order categories (Type 15)
    ('15', '1501', 'Mail Order - Catalog', TRUE, 'L', 6),
    ('15', '1502', 'Mail Order - Magazine', TRUE, 'L', 7),
    ('15', '1503', 'Mail Order - Charity', TRUE, 'L', 7),
    ('15', '1504', 'Mail Order - Government', TRUE, 'L', 8),
    
    -- Recurring Payment categories (Type 16)
    ('16', '1601', 'Recurring - Utility Bill', TRUE, 'L', 8),
    ('16', '1602', 'Recurring - Insurance Premium', TRUE, 'L', 8),
    ('16', '1603', 'Recurring - Subscription', TRUE, 'L', 7),
    ('16', '1604', 'Recurring - Membership', TRUE, 'L', 7),
    ('16', '1605', 'Recurring - Loan Payment', TRUE, 'L', 8),
    
    -- Balance Transfer categories (Type 17)
    ('17', '1701', 'Balance Transfer - Promotional', TRUE, 'L', 5),
    ('17', '1702', 'Balance Transfer - Standard', TRUE, 'L', 5),
    ('17', '1703', 'Balance Transfer - Emergency', TRUE, 'M', 4),
    
    -- Cash Advance ATM categories (Type 18)
    ('18', '1801', 'Cash Advance - ATM Own Bank', TRUE, 'M', 6),
    ('18', '1802', 'Cash Advance - ATM Other Bank', TRUE, 'M', 5),
    ('18', '1803', 'Cash Advance - ATM International', TRUE, 'H', 3),
    
    -- Cash Advance Counter categories (Type 19)
    ('19', '1901', 'Cash Advance - Bank Counter', TRUE, 'M', 6),
    ('19', '1902', 'Cash Advance - Credit Union', TRUE, 'M', 6),
    ('19', '1903', 'Cash Advance - Check Cashing', TRUE, 'H', 4),
    
    -- Foreign Transaction categories (Type 20)
    ('20', '2001', 'Foreign - Travel Purchase', TRUE, 'M', 5),
    ('20', '2002', 'Foreign - Online Purchase', TRUE, 'H', 4),
    ('20', '2003', 'Foreign - ATM Withdrawal', TRUE, 'H', 4),
    ('20', '2004', 'Foreign - Cash Advance', TRUE, 'H', 3),
    
    -- Payment Posted categories (Type 21)
    ('21', '2101', 'Payment - Online Banking', TRUE, 'L', 8),
    ('21', '2102', 'Payment - Phone Banking', TRUE, 'L', 8),
    ('21', '2103', 'Payment - Bank Branch', TRUE, 'L', 8),
    ('21', '2104', 'Payment - Mail Check', TRUE, 'L', 7),
    ('21', '2105', 'Payment - Electronic Transfer', TRUE, 'L', 8),
    ('21', '2106', 'Payment - Auto Pay', TRUE, 'L', 9),
    
    -- Refund Credit categories (Type 22)
    ('22', '2201', 'Refund - Merchant Return', TRUE, 'L', 6),
    ('22', '2202', 'Refund - Service Cancellation', TRUE, 'L', 6),
    ('22', '2203', 'Refund - Duplicate Charge', TRUE, 'L', 7),
    ('22', '2204', 'Refund - Billing Error', TRUE, 'L', 7),
    
    -- Fee categories (Types 31-40)
    ('31', '3101', 'Annual Fee - Standard', TRUE, 'L', 3),
    ('31', '3102', 'Annual Fee - Premium', TRUE, 'L', 3),
    ('32', '3201', 'Late Fee - First Occurrence', TRUE, 'L', 3),
    ('32', '3202', 'Late Fee - Repeat', TRUE, 'L', 3),
    ('33', '3301', 'Over Limit Fee - Standard', TRUE, 'L', 3),
    ('34', '3401', 'Cash Advance Fee - ATM', TRUE, 'L', 4),
    ('34', '3402', 'Cash Advance Fee - Counter', TRUE, 'L', 4),
    ('35', '3501', 'Foreign Transaction Fee', TRUE, 'L', 4),
    ('36', '3601', 'Balance Transfer Fee', TRUE, 'L', 4),
    ('37', '3701', 'Returned Payment Fee', TRUE, 'L', 3),
    ('38', '3801', 'Expedited Payment Fee', TRUE, 'L', 4),
    ('39', '3901', 'Account Maintenance Fee', TRUE, 'L', 3),
    ('40', '4001', 'Overlimit Protection Fee', TRUE, 'L', 3),
    
    -- Interest categories (Types 41-45)
    ('41', '4101', 'Purchase Interest - Standard', TRUE, 'L', 2),
    ('41', '4102', 'Purchase Interest - Penalty', TRUE, 'L', 2),
    ('42', '4201', 'Cash Advance Interest', TRUE, 'L', 2),
    ('43', '4301', 'Balance Transfer Interest', TRUE, 'L', 2),
    ('44', '4401', 'Penalty Interest', TRUE, 'L', 1),
    ('45', '4501', 'Promotional Interest', TRUE, 'L', 2),
    
    -- Test-specific categories (Types 91-95)
    ('91', '9101', 'Test Category A1', FALSE, 'L', 1),
    ('91', '9102', 'Test Category A2', FALSE, 'L', 1),
    ('92', '9201', 'Test Category B1', FALSE, 'L', 1),
    ('92', '9202', 'Test Category B2', FALSE, 'L', 1),
    ('93', '9301', 'Test Category C1', FALSE, 'L', 1),
    ('94', '9401', 'Test Edge Case Category', FALSE, 'L', 1),
    ('95', '9501', 'Test Validation Category', FALSE, 'L', 1),
    
    -- System categories (Types 96-99)
    ('96', '9601', 'System Adjustment - Balance', TRUE, 'L', 2),
    ('97', '9701', 'System Correction - Error', TRUE, 'L', 2),
    ('98', '9801', 'System Reversal - Transaction', TRUE, 'L', 9),
    ('99', '9901', 'System Test Category', FALSE, 'L', 1)
ON CONFLICT (transaction_type, transaction_category) DO UPDATE SET
    category_description = EXCLUDED.category_description,
    active_status = EXCLUDED.active_status,
    risk_level = EXCLUDED.risk_level,
    processing_priority = EXCLUDED.processing_priority,
    last_updated = CURRENT_TIMESTAMP;

-- =====================================================================================
-- TEST DATA VALIDATION AND INTEGRITY CHECKS
-- Purpose: Ensure data quality and referential integrity for test scenarios
-- Supporting: Integration testing validation and error handling testing
-- =====================================================================================

-- Validate that all transaction categories have valid parent transaction types
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM transaction_categories tc
    LEFT JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
    WHERE tt.transaction_type IS NULL;
    
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'Test data integrity violation: % orphaned transaction categories found', orphan_count;
    END IF;
    
    RAISE NOTICE 'Test data validation passed: All transaction categories have valid parent types';
END $$;

-- Validate debit/credit flag consistency
DO $$
DECLARE
    inconsistent_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO inconsistent_count
    FROM transaction_categories tc
    JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
    WHERE (tt.debit_credit_flag = TRUE AND tc.risk_level = 'L' AND tc.processing_priority > 8)
    OR (tt.debit_credit_flag = FALSE AND tc.processing_priority < 5);
    
    IF inconsistent_count > 0 THEN
        RAISE NOTICE 'Warning: % potentially inconsistent debit/credit configurations found', inconsistent_count;
    END IF;
END $$;

-- =====================================================================================
-- TEST SCENARIO SUPPORT DATA
-- Purpose: Additional test data for specific integration testing scenarios
-- Supporting: Edge case testing and boundary condition validation
-- =====================================================================================

-- Update statistics for query planner optimization
ANALYZE transaction_types;
ANALYZE transaction_categories;

-- Create test data summary for integration testing reference
CREATE OR REPLACE VIEW v_test_reference_data_summary AS
SELECT 
    'transaction_types' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN active_status = TRUE THEN 1 END) as active_records,
    COUNT(CASE WHEN active_status = FALSE THEN 1 END) as inactive_records,
    COUNT(CASE WHEN debit_credit_flag = TRUE THEN 1 END) as credit_types,
    COUNT(CASE WHEN debit_credit_flag = FALSE THEN 1 END) as debit_types,
    MIN(transaction_type) as min_type_code,
    MAX(transaction_type) as max_type_code
FROM transaction_types

UNION ALL

SELECT 
    'transaction_categories' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN active_status = TRUE THEN 1 END) as active_records,
    COUNT(CASE WHEN active_status = FALSE THEN 1 END) as inactive_records,
    COUNT(CASE WHEN risk_level = 'L' THEN 1 END) as low_risk_count,
    COUNT(CASE WHEN risk_level = 'M' THEN 1 END) as medium_risk_count,
    MIN(transaction_category) as min_category_code,
    MAX(transaction_category) as max_category_code
FROM transaction_categories;

-- =====================================================================================
-- COMMENTS AND DOCUMENTATION
-- Purpose: Provide comprehensive documentation for test reference data
-- Supporting: Test development and maintenance understanding
-- =====================================================================================

-- Table comments for test reference data
COMMENT ON VIEW v_test_reference_data_summary IS 'Summary view of test reference data for integration testing validation and test coverage analysis';

-- =====================================================================================
-- CHANGESET COMPLETION CONFIRMATION
-- Purpose: Confirm successful test reference data loading
-- Supporting: Test environment setup validation and troubleshooting
-- =====================================================================================

-- Verify test data loading success
SELECT 
    'Test reference data loaded successfully' as status,
    (SELECT COUNT(*) FROM transaction_types) as transaction_types_count,
    (SELECT COUNT(*) FROM transaction_categories) as transaction_categories_count,
    (SELECT COUNT(*) FROM transaction_types WHERE active_status = TRUE) as active_types_count,
    (SELECT COUNT(*) FROM transaction_categories WHERE active_status = TRUE) as active_categories_count,
    CURRENT_TIMESTAMP as loaded_at;

-- Grant appropriate permissions for test environment access
GRANT SELECT ON v_test_reference_data_summary TO PUBLIC;

-- =====================================================================================
-- ROLLBACK INSTRUCTIONS
-- Purpose: Provide rollback capability for test environment management
-- Supporting: Test environment reset and cleanup procedures
-- =====================================================================================

--rollback DROP VIEW IF EXISTS v_test_reference_data_summary;
--rollback DELETE FROM transaction_categories WHERE transaction_type IN ('11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','91','92','93','94','95','96','97','98','99');
--rollback DELETE FROM transaction_types WHERE transaction_type IN ('11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','91','92','93','94','95','96','97','98','99');