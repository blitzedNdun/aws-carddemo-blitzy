-- ============================================================================
-- Referential Integrity Validation Script
-- Description: Comprehensive PostgreSQL foreign key constraint validation
-- Author: Blitzy agent
-- Version: 1.0
-- Purpose: Test referential integrity constraints replacing VSAM XREF functionality
-- ============================================================================

-- Enable detailed error reporting for validation failures
\set ON_ERROR_STOP on
\set VERBOSITY verbose

-- Create temporary schema for validation test data
CREATE SCHEMA IF NOT EXISTS validation_test;

-- Create validation results table to track test outcomes
CREATE TABLE validation_test.referential_integrity_results (
    test_id SERIAL PRIMARY KEY,
    test_name VARCHAR(100) NOT NULL,
    test_category VARCHAR(50) NOT NULL,
    test_status VARCHAR(20) NOT NULL CHECK (test_status IN ('PASS', 'FAIL', 'SKIP')),
    error_message TEXT,
    execution_time INTERVAL,
    test_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    details JSONB
);

-- Create validation test tracking function
CREATE OR REPLACE FUNCTION validation_test.log_test_result(
    p_test_name VARCHAR(100),
    p_test_category VARCHAR(50),
    p_test_status VARCHAR(20),
    p_error_message TEXT DEFAULT NULL,
    p_execution_time INTERVAL DEFAULT NULL,
    p_details JSONB DEFAULT NULL
) RETURNS VOID AS $$
BEGIN
    INSERT INTO validation_test.referential_integrity_results (
        test_name, test_category, test_status, error_message, execution_time, details
    ) VALUES (
        p_test_name, p_test_category, p_test_status, p_error_message, p_execution_time, p_details
    );
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SECTION 1: FOREIGN KEY CONSTRAINT VALIDATION TESTS
-- ============================================================================

-- Test 1.1: Customer-Account Foreign Key Constraint Test
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_error_caught BOOLEAN := FALSE;
    v_error_message TEXT;
BEGIN
    -- Test valid customer-account relationship
    BEGIN
        -- Create test customer
        INSERT INTO customers (customer_id, first_name, last_name, address_line_1, 
                             address_state, address_country, address_zip, ssn, 
                             government_id, date_of_birth, fico_credit_score)
        VALUES ('100000001', 'John', 'Doe', '123 Test Street', 'CA', 'USA', '90210', 
                '123456789', 'DL123456789', '1985-01-01', 750);
        
        -- Create test account referencing valid customer
        INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                            credit_limit, cash_credit_limit, open_date, group_id)
        VALUES ('10000000001', '100000001', 'Y', 1000.00, 5000.00, 1000.00, 
                CURRENT_DATE, 'GROUP00001');
        
        -- Test invalid customer-account relationship (should fail)
        INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                            credit_limit, cash_credit_limit, open_date, group_id)
        VALUES ('10000000002', '999999999', 'Y', 1000.00, 5000.00, 1000.00, 
                CURRENT_DATE, 'GROUP00001');
        
        -- If we reach here, constraint is not working
        v_error_caught := FALSE;
        v_error_message := 'Foreign key constraint fk_accounts_customer_id not enforced';
        
    EXCEPTION
        WHEN foreign_key_violation THEN
            v_error_caught := TRUE;
            v_error_message := 'Foreign key constraint properly enforced';
        WHEN OTHERS THEN
            v_error_caught := FALSE;
            v_error_message := 'Unexpected error: ' || SQLERRM;
    END;
    
    v_test_end := clock_timestamp();
    
    IF v_error_caught THEN
        CALL validation_test.log_test_result(
            'Customer-Account Foreign Key Constraint',
            'Foreign Key Validation',
            'PASS',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_name": "fk_accounts_customer_id", "expected_behavior": "RESTRICT"}'::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Customer-Account Foreign Key Constraint',
            'Foreign Key Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_name": "fk_accounts_customer_id", "expected_behavior": "RESTRICT"}'::jsonb
        );
    END IF;
    
    -- Clean up test data
    DELETE FROM accounts WHERE account_id IN ('10000000001', '10000000002');
    DELETE FROM customers WHERE customer_id = '100000001';
END $$;

-- Test 1.2: Account-Card Foreign Key Constraint Test
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_error_caught BOOLEAN := FALSE;
    v_error_message TEXT;
BEGIN
    -- Setup test data
    INSERT INTO customers (customer_id, first_name, last_name, address_line_1, 
                         address_state, address_country, address_zip, ssn, 
                         government_id, date_of_birth, fico_credit_score)
    VALUES ('100000002', 'Jane', 'Smith', '456 Test Avenue', 'NY', 'USA', '10001', 
            '987654321', 'DL987654321', '1990-05-15', 680);
    
    INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                        credit_limit, cash_credit_limit, open_date, group_id)
    VALUES ('10000000003', '100000002', 'Y', 500.00, 3000.00, 500.00, 
            CURRENT_DATE, 'GROUP00002');
    
    -- Test valid account-card relationship
    INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, 
                      expiration_date, active_status)
    VALUES ('1234567890123456', '10000000003', '100000002', '123', 'JANE SMITH', 
            CURRENT_DATE + INTERVAL '3 years', 'Y');
    
    -- Test invalid account-card relationship (should fail)
    BEGIN
        INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, 
                          expiration_date, active_status)
        VALUES ('1234567890123457', '99999999999', '100000002', '456', 'JANE SMITH', 
                CURRENT_DATE + INTERVAL '3 years', 'Y');
        
        v_error_caught := FALSE;
        v_error_message := 'Foreign key constraint fk_cards_account_id not enforced';
        
    EXCEPTION
        WHEN foreign_key_violation THEN
            v_error_caught := TRUE;
            v_error_message := 'Foreign key constraint properly enforced';
        WHEN OTHERS THEN
            v_error_caught := FALSE;
            v_error_message := 'Unexpected error: ' || SQLERRM;
    END;
    
    v_test_end := clock_timestamp();
    
    IF v_error_caught THEN
        CALL validation_test.log_test_result(
            'Account-Card Foreign Key Constraint',
            'Foreign Key Validation',
            'PASS',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_name": "fk_cards_account_id", "expected_behavior": "RESTRICT"}'::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Account-Card Foreign Key Constraint',
            'Foreign Key Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_name": "fk_cards_account_id", "expected_behavior": "RESTRICT"}'::jsonb
        );
    END IF;
    
    -- Clean up test data
    DELETE FROM cards WHERE card_number IN ('1234567890123456', '1234567890123457');
    DELETE FROM accounts WHERE account_id = '10000000003';
    DELETE FROM customers WHERE customer_id = '100000002';
END $$;

-- Test 1.3: Customer-Card Foreign Key Constraint Test
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_error_caught BOOLEAN := FALSE;
    v_error_message TEXT;
BEGIN
    -- Setup test data
    INSERT INTO customers (customer_id, first_name, last_name, address_line_1, 
                         address_state, address_country, address_zip, ssn, 
                         government_id, date_of_birth, fico_credit_score)
    VALUES ('100000003', 'Bob', 'Johnson', '789 Test Boulevard', 'TX', 'USA', '75001', 
            '456789123', 'DL456789123', '1980-12-25', 720);
    
    INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                        credit_limit, cash_credit_limit, open_date, group_id)
    VALUES ('10000000004', '100000003', 'Y', 2000.00, 8000.00, 2000.00, 
            CURRENT_DATE, 'GROUP00003');
    
    -- Test valid customer-card relationship
    INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, 
                      expiration_date, active_status)
    VALUES ('2345678901234567', '10000000004', '100000003', '789', 'BOB JOHNSON', 
            CURRENT_DATE + INTERVAL '2 years', 'Y');
    
    -- Test invalid customer-card relationship (should fail)
    BEGIN
        INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, 
                          expiration_date, active_status)
        VALUES ('2345678901234568', '10000000004', '888888888', '012', 'BOB JOHNSON', 
                CURRENT_DATE + INTERVAL '2 years', 'Y');
        
        v_error_caught := FALSE;
        v_error_message := 'Foreign key constraint fk_cards_customer_id not enforced';
        
    EXCEPTION
        WHEN foreign_key_violation THEN
            v_error_caught := TRUE;
            v_error_message := 'Foreign key constraint properly enforced';
        WHEN OTHERS THEN
            v_error_caught := FALSE;
            v_error_message := 'Unexpected error: ' || SQLERRM;
    END;
    
    v_test_end := clock_timestamp();
    
    IF v_error_caught THEN
        CALL validation_test.log_test_result(
            'Customer-Card Foreign Key Constraint',
            'Foreign Key Validation',
            'PASS',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_name": "fk_cards_customer_id", "expected_behavior": "RESTRICT"}'::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Customer-Card Foreign Key Constraint',
            'Foreign Key Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_name": "fk_cards_customer_id", "expected_behavior": "RESTRICT"}'::jsonb
        );
    END IF;
    
    -- Clean up test data
    DELETE FROM cards WHERE card_number IN ('2345678901234567', '2345678901234568');
    DELETE FROM accounts WHERE account_id = '10000000004';
    DELETE FROM customers WHERE customer_id = '100000003';
END $$;

-- ============================================================================
-- SECTION 2: CROSS-REFERENCE FUNCTIONALITY VALIDATION (VSAM XREF EQUIVALENT)
-- ============================================================================

-- Test 2.1: Card-Account-Customer Cross-Reference Validation
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_test_passed BOOLEAN := TRUE;
    v_error_message TEXT := '';
    v_invalid_relationships INTEGER := 0;
    v_total_cards INTEGER := 0;
BEGIN
    -- Setup comprehensive test data for cross-reference validation
    INSERT INTO customers (customer_id, first_name, last_name, address_line_1, 
                         address_state, address_country, address_zip, ssn, 
                         government_id, date_of_birth, fico_credit_score)
    VALUES 
        ('100000010', 'Alice', 'Williams', '100 Main St', 'CA', 'USA', '90210', '111111111', 'DL111111111', '1975-03-20', 800),
        ('100000011', 'Charlie', 'Brown', '200 Oak Ave', 'NY', 'USA', '10001', '222222222', 'DL222222222', '1982-07-10', 650),
        ('100000012', 'Diana', 'Davis', '300 Pine Rd', 'TX', 'USA', '75001', '333333333', 'DL333333333', '1988-11-05', 720);
    
    INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                        credit_limit, cash_credit_limit, open_date, group_id)
    VALUES 
        ('10000000010', '100000010', 'Y', 1500.00, 6000.00, 1500.00, CURRENT_DATE, 'GROUP00010'),
        ('10000000011', '100000011', 'Y', 800.00, 4000.00, 800.00, CURRENT_DATE, 'GROUP00011'),
        ('10000000012', '100000012', 'Y', 2200.00, 10000.00, 2200.00, CURRENT_DATE, 'GROUP00012'),
        ('10000000013', '100000010', 'Y', 500.00, 2000.00, 500.00, CURRENT_DATE, 'GROUP00013');
    
    INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, 
                      expiration_date, active_status)
    VALUES 
        ('3456789012345678', '10000000010', '100000010', '111', 'ALICE WILLIAMS', CURRENT_DATE + INTERVAL '4 years', 'Y'),
        ('3456789012345679', '10000000011', '100000011', '222', 'CHARLIE BROWN', CURRENT_DATE + INTERVAL '3 years', 'Y'),
        ('3456789012345680', '10000000012', '100000012', '333', 'DIANA DAVIS', CURRENT_DATE + INTERVAL '2 years', 'Y'),
        ('3456789012345681', '10000000013', '100000010', '444', 'ALICE WILLIAMS', CURRENT_DATE + INTERVAL '1 year', 'Y');
    
    -- Test cross-reference integrity using equivalent VSAM XREF logic
    -- Check that every card has matching account-customer relationship
    SELECT COUNT(*) INTO v_total_cards FROM cards WHERE card_number LIKE '345678901234%';
    
    SELECT COUNT(*) INTO v_invalid_relationships
    FROM cards c
    WHERE c.card_number LIKE '345678901234%'
    AND NOT EXISTS (
        SELECT 1 FROM accounts a 
        WHERE a.account_id = c.account_id 
        AND a.customer_id = c.customer_id
    );
    
    IF v_invalid_relationships > 0 THEN
        v_test_passed := FALSE;
        v_error_message := 'Found ' || v_invalid_relationships || ' cards with invalid account-customer relationships';
    END IF;
    
    -- Test cross-reference lookup performance (should be sub-200ms per requirement)
    PERFORM c.card_number, c.account_id, c.customer_id, a.current_balance, a.credit_limit
    FROM cards c
    JOIN accounts a ON c.account_id = a.account_id AND c.customer_id = a.customer_id
    WHERE c.card_number LIKE '345678901234%';
    
    v_test_end := clock_timestamp();
    
    IF v_test_passed THEN
        CALL validation_test.log_test_result(
            'Card-Account-Customer Cross-Reference Validation',
            'Cross-Reference Validation',
            'PASS',
            'All ' || v_total_cards || ' cards have valid cross-reference relationships',
            v_test_end - v_test_start,
            ('{"total_cards": ' || v_total_cards || ', "invalid_relationships": ' || v_invalid_relationships || '}')::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Card-Account-Customer Cross-Reference Validation',
            'Cross-Reference Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            ('{"total_cards": ' || v_total_cards || ', "invalid_relationships": ' || v_invalid_relationships || '}')::jsonb
        );
    END IF;
    
    -- Clean up test data
    DELETE FROM cards WHERE card_number LIKE '345678901234%';
    DELETE FROM accounts WHERE account_id LIKE '100000000%';
    DELETE FROM customers WHERE customer_id LIKE '100000010' OR customer_id LIKE '100000011' OR customer_id LIKE '100000012';
END $$;

-- Test 2.2: Multi-Table Relationship Validation
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_test_passed BOOLEAN := TRUE;
    v_error_message TEXT := '';
    v_orphaned_accounts INTEGER := 0;
    v_orphaned_cards INTEGER := 0;
BEGIN
    -- Check for orphaned accounts (accounts without valid customers)
    SELECT COUNT(*) INTO v_orphaned_accounts
    FROM accounts a
    WHERE NOT EXISTS (
        SELECT 1 FROM customers c WHERE c.customer_id = a.customer_id
    );
    
    -- Check for orphaned cards (cards without valid accounts or customers)
    SELECT COUNT(*) INTO v_orphaned_cards
    FROM cards c
    WHERE NOT EXISTS (
        SELECT 1 FROM accounts a WHERE a.account_id = c.account_id
    ) OR NOT EXISTS (
        SELECT 1 FROM customers cu WHERE cu.customer_id = c.customer_id
    );
    
    IF v_orphaned_accounts > 0 OR v_orphaned_cards > 0 THEN
        v_test_passed := FALSE;
        v_error_message := 'Found orphaned records: ' || v_orphaned_accounts || ' accounts, ' || v_orphaned_cards || ' cards';
    END IF;
    
    v_test_end := clock_timestamp();
    
    IF v_test_passed THEN
        CALL validation_test.log_test_result(
            'Multi-Table Relationship Validation',
            'Cross-Reference Validation',
            'PASS',
            'No orphaned records found',
            v_test_end - v_test_start,
            ('{"orphaned_accounts": ' || v_orphaned_accounts || ', "orphaned_cards": ' || v_orphaned_cards || '}')::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Multi-Table Relationship Validation',
            'Cross-Reference Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            ('{"orphaned_accounts": ' || v_orphaned_accounts || ', "orphaned_cards": ' || v_orphaned_cards || '}')::jsonb
        );
    END IF;
END $$;

-- ============================================================================
-- SECTION 3: CASCADE BEHAVIOR VALIDATION
-- ============================================================================

-- Test 3.1: Customer Update Cascade Validation
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_test_passed BOOLEAN := TRUE;
    v_error_message TEXT := '';
    v_updated_accounts INTEGER := 0;
    v_updated_cards INTEGER := 0;
BEGIN
    -- Setup test data
    INSERT INTO customers (customer_id, first_name, last_name, address_line_1, 
                         address_state, address_country, address_zip, ssn, 
                         government_id, date_of_birth, fico_credit_score)
    VALUES ('100000020', 'Test', 'User', '123 Cascade St', 'CA', 'USA', '90210', 
            '555555555', 'DL555555555', '1985-01-01', 750);
    
    INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                        credit_limit, cash_credit_limit, open_date, group_id)
    VALUES ('10000000020', '100000020', 'Y', 1000.00, 5000.00, 1000.00, 
            CURRENT_DATE, 'GROUP00020');
    
    INSERT INTO cards (card_number, account_id, customer_id, cvv_code, embossed_name, 
                      expiration_date, active_status)
    VALUES ('4567890123456789', '10000000020', '100000020', '555', 'TEST USER', 
            CURRENT_DATE + INTERVAL '2 years', 'Y');
    
    -- Test customer ID update cascade (should update related accounts and cards)
    UPDATE customers SET customer_id = '100000021' WHERE customer_id = '100000020';
    
    -- Check if cascade worked
    SELECT COUNT(*) INTO v_updated_accounts FROM accounts WHERE customer_id = '100000021';
    SELECT COUNT(*) INTO v_updated_cards FROM cards WHERE customer_id = '100000021';
    
    IF v_updated_accounts = 0 OR v_updated_cards = 0 THEN
        v_test_passed := FALSE;
        v_error_message := 'Cascade update failed - accounts: ' || v_updated_accounts || ', cards: ' || v_updated_cards;
    END IF;
    
    v_test_end := clock_timestamp();
    
    IF v_test_passed THEN
        CALL validation_test.log_test_result(
            'Customer Update Cascade Validation',
            'Cascade Behavior Validation',
            'PASS',
            'Cascade update successful - accounts: ' || v_updated_accounts || ', cards: ' || v_updated_cards,
            v_test_end - v_test_start,
            ('{"updated_accounts": ' || v_updated_accounts || ', "updated_cards": ' || v_updated_cards || '}')::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Customer Update Cascade Validation',
            'Cascade Behavior Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            ('{"updated_accounts": ' || v_updated_accounts || ', "updated_cards": ' || v_updated_cards || '}')::jsonb
        );
    END IF;
    
    -- Clean up test data
    DELETE FROM cards WHERE card_number = '4567890123456789';
    DELETE FROM accounts WHERE account_id = '10000000020';
    DELETE FROM customers WHERE customer_id IN ('100000020', '100000021');
END $$;

-- Test 3.2: Customer Delete Restrict Validation
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_error_caught BOOLEAN := FALSE;
    v_error_message TEXT;
BEGIN
    -- Setup test data
    INSERT INTO customers (customer_id, first_name, last_name, address_line_1, 
                         address_state, address_country, address_zip, ssn, 
                         government_id, date_of_birth, fico_credit_score)
    VALUES ('100000030', 'Delete', 'Test', '123 Restrict St', 'CA', 'USA', '90210', 
            '666666666', 'DL666666666', '1985-01-01', 750);
    
    INSERT INTO accounts (account_id, customer_id, active_status, current_balance, 
                        credit_limit, cash_credit_limit, open_date, group_id)
    VALUES ('10000000030', '100000030', 'Y', 1000.00, 5000.00, 1000.00, 
            CURRENT_DATE, 'GROUP00030');
    
    -- Test customer deletion with existing accounts (should fail due to RESTRICT)
    BEGIN
        DELETE FROM customers WHERE customer_id = '100000030';
        v_error_caught := FALSE;
        v_error_message := 'Delete restriction not enforced - customer with accounts was deleted';
    EXCEPTION
        WHEN foreign_key_violation THEN
            v_error_caught := TRUE;
            v_error_message := 'Delete restriction properly enforced';
        WHEN OTHERS THEN
            v_error_caught := FALSE;
            v_error_message := 'Unexpected error: ' || SQLERRM;
    END;
    
    v_test_end := clock_timestamp();
    
    IF v_error_caught THEN
        CALL validation_test.log_test_result(
            'Customer Delete Restrict Validation',
            'Cascade Behavior Validation',
            'PASS',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_type": "RESTRICT", "expected_behavior": "PREVENT_DELETE"}'::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Customer Delete Restrict Validation',
            'Cascade Behavior Validation',
            'FAIL',
            v_error_message,
            v_test_end - v_test_start,
            '{"constraint_type": "RESTRICT", "expected_behavior": "PREVENT_DELETE"}'::jsonb
        );
    END IF;
    
    -- Clean up test data
    DELETE FROM accounts WHERE account_id = '10000000030';
    DELETE FROM customers WHERE customer_id = '100000030';
END $$;

-- ============================================================================
-- SECTION 4: PERFORMANCE VALIDATION
-- ============================================================================

-- Test 4.1: Cross-Reference Query Performance Validation
DO $$
DECLARE
    v_test_start TIMESTAMP := clock_timestamp();
    v_test_end TIMESTAMP;
    v_test_passed BOOLEAN := TRUE;
    v_error_message TEXT := '';
    v_query_time INTERVAL;
    v_threshold INTERVAL := '200 milliseconds';
BEGIN
    -- Test cross-reference query performance (equivalent to VSAM XREF lookup)
    PERFORM c.card_number, c.account_id, c.customer_id, 
            a.current_balance, a.credit_limit, a.active_status,
            cu.first_name, cu.last_name, cu.fico_credit_score
    FROM cards c
    JOIN accounts a ON c.account_id = a.account_id AND c.customer_id = a.customer_id
    JOIN customers cu ON c.customer_id = cu.customer_id
    WHERE c.active_status = 'Y'
    AND a.active_status = 'Y'
    ORDER BY c.card_number;
    
    v_test_end := clock_timestamp();
    v_query_time := v_test_end - v_test_start;
    
    IF v_query_time > v_threshold THEN
        v_test_passed := FALSE;
        v_error_message := 'Query performance exceeds threshold: ' || v_query_time || ' > ' || v_threshold;
    END IF;
    
    IF v_test_passed THEN
        CALL validation_test.log_test_result(
            'Cross-Reference Query Performance',
            'Performance Validation',
            'PASS',
            'Query completed in ' || v_query_time || ' (threshold: ' || v_threshold || ')',
            v_query_time,
            ('{"query_time": "' || v_query_time || '", "threshold": "' || v_threshold || '"}')::jsonb
        );
    ELSE
        CALL validation_test.log_test_result(
            'Cross-Reference Query Performance',
            'Performance Validation',
            'FAIL',
            v_error_message,
            v_query_time,
            ('{"query_time": "' || v_query_time || '", "threshold": "' || v_threshold || '"}')::jsonb
        );
    END IF;
END $$;

-- ============================================================================
-- SECTION 5: VALIDATION SUMMARY AND REPORTING
-- ============================================================================

-- Generate validation summary report
SELECT 
    '========================================================================================' AS separator
UNION ALL
SELECT 
    'REFERENTIAL INTEGRITY VALIDATION SUMMARY REPORT' AS separator
UNION ALL
SELECT 
    '========================================================================================' AS separator
UNION ALL
SELECT 
    'Test Category: ' || test_category || ' | Status: ' || test_status || ' | Count: ' || COUNT(*)
FROM validation_test.referential_integrity_results
GROUP BY test_category, test_status
ORDER BY test_category, test_status;

-- Generate detailed test results
SELECT 
    test_name,
    test_category,
    test_status,
    CASE 
        WHEN test_status = 'PASS' THEN '✓'
        WHEN test_status = 'FAIL' THEN '✗'
        ELSE '⚠'
    END AS status_icon,
    execution_time,
    COALESCE(error_message, 'No errors') AS message,
    test_timestamp
FROM validation_test.referential_integrity_results
ORDER BY test_category, test_name;

-- Generate compliance report
WITH test_summary AS (
    SELECT 
        test_category,
        COUNT(*) as total_tests,
        SUM(CASE WHEN test_status = 'PASS' THEN 1 ELSE 0 END) as passed_tests,
        SUM(CASE WHEN test_status = 'FAIL' THEN 1 ELSE 0 END) as failed_tests
    FROM validation_test.referential_integrity_results
    GROUP BY test_category
)
SELECT 
    'COMPLIANCE REPORT:' AS report_section,
    test_category,
    total_tests,
    passed_tests,
    failed_tests,
    ROUND((passed_tests::decimal / total_tests::decimal) * 100, 2) as success_rate_percent,
    CASE 
        WHEN failed_tests = 0 THEN 'COMPLIANT'
        ELSE 'NON-COMPLIANT'
    END AS compliance_status
FROM test_summary
ORDER BY test_category;

-- Generate final validation result
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM validation_test.referential_integrity_results WHERE test_status = 'FAIL') 
        THEN 'VALIDATION FAILED - REFERENTIAL INTEGRITY ISSUES DETECTED'
        ELSE 'VALIDATION PASSED - ALL REFERENTIAL INTEGRITY CONSTRAINTS WORKING CORRECTLY'
    END AS final_validation_result,
    COUNT(*) as total_tests_executed,
    SUM(CASE WHEN test_status = 'PASS' THEN 1 ELSE 0 END) as total_passed,
    SUM(CASE WHEN test_status = 'FAIL' THEN 1 ELSE 0 END) as total_failed,
    CURRENT_TIMESTAMP as validation_completed_at
FROM validation_test.referential_integrity_results;

-- Export ReferentialIntegrityValidationResults (Default Export)
SELECT 
    jsonb_build_object(
        'validation_summary', jsonb_build_object(
            'total_tests', COUNT(*),
            'passed_tests', SUM(CASE WHEN test_status = 'PASS' THEN 1 ELSE 0 END),
            'failed_tests', SUM(CASE WHEN test_status = 'FAIL' THEN 1 ELSE 0 END),
            'success_rate', ROUND((SUM(CASE WHEN test_status = 'PASS' THEN 1 ELSE 0 END)::decimal / COUNT(*)::decimal) * 100, 2),
            'validation_timestamp', CURRENT_TIMESTAMP
        ),
        'foreign_key_constraint_validation_results', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'execution_time', execution_time,
                'details', details
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_category = 'Foreign Key Validation'
        ),
        'customer_account_relationship_verification', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'message', error_message,
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Customer-Account%'
        ),
        'account_card_relationship_verification', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'message', error_message,
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Account-Card%'
        ),
        'cross_reference_functionality_test_results', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'message', error_message,
                'details', details
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_category = 'Cross-Reference Validation'
        ),
        'vsam_xref_equivalence_validation', (
            SELECT jsonb_build_object(
                'cross_reference_integrity_verified', NOT EXISTS (
                    SELECT 1 FROM validation_test.referential_integrity_results 
                    WHERE test_category = 'Cross-Reference Validation' AND test_status = 'FAIL'
                ),
                'performance_requirement_met', NOT EXISTS (
                    SELECT 1 FROM validation_test.referential_integrity_results 
                    WHERE test_category = 'Performance Validation' AND test_status = 'FAIL'
                ),
                'equivalent_functionality_status', CASE 
                    WHEN NOT EXISTS (
                        SELECT 1 FROM validation_test.referential_integrity_results 
                        WHERE test_category IN ('Cross-Reference Validation', 'Performance Validation') 
                        AND test_status = 'FAIL'
                    ) THEN 'EQUIVALENT'
                    ELSE 'NOT_EQUIVALENT'
                END
            )
        ),
        'orphaned_record_detection_results', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'orphaned_records_found', COALESCE(details->>'orphaned_accounts', '0')::integer + COALESCE(details->>'orphaned_cards', '0')::integer,
                'details', details
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Orphaned%' OR test_name LIKE '%Multi-Table%'
        ),
        'cascade_behavior_validation_results', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'message', error_message,
                'cascade_type', CASE 
                    WHEN test_name LIKE '%Update%' THEN 'CASCADE'
                    WHEN test_name LIKE '%Delete%' THEN 'RESTRICT'
                    ELSE 'UNKNOWN'
                END
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_category = 'Cascade Behavior Validation'
        ),
        'referential_integrity_compliance_report', (
            SELECT jsonb_build_object(
                'overall_compliance', CASE 
                    WHEN NOT EXISTS (
                        SELECT 1 FROM validation_test.referential_integrity_results 
                        WHERE test_status = 'FAIL'
                    ) THEN 'COMPLIANT'
                    ELSE 'NON-COMPLIANT'
                END,
                'compliance_percentage', ROUND((SUM(CASE WHEN test_status = 'PASS' THEN 1 ELSE 0 END)::decimal / COUNT(*)::decimal) * 100, 2),
                'critical_failures', SUM(CASE WHEN test_status = 'FAIL' AND test_category = 'Foreign Key Validation' THEN 1 ELSE 0 END),
                'performance_issues', SUM(CASE WHEN test_status = 'FAIL' AND test_category = 'Performance Validation' THEN 1 ELSE 0 END)
            )
            FROM validation_test.referential_integrity_results
        )
    ) AS ReferentialIntegrityValidationResults
FROM validation_test.referential_integrity_results;

-- Export CrossReferenceValidationTests (Named Export)
SELECT 
    jsonb_build_object(
        'customer_account_foreign_key_constraint_tests', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'constraint_name', details->>'constraint_name',
                'expected_behavior', details->>'expected_behavior',
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Customer-Account%'
        ),
        'account_card_foreign_key_constraint_tests', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'constraint_name', details->>'constraint_name',
                'expected_behavior', details->>'expected_behavior',
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Account-Card%'
        ),
        'card_customer_foreign_key_constraint_tests', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'constraint_name', details->>'constraint_name',
                'expected_behavior', details->>'expected_behavior',
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Customer-Card%'
        ),
        'multi_table_relationship_validation', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'orphaned_accounts', COALESCE(details->>'orphaned_accounts', '0')::integer,
                'orphaned_cards', COALESCE(details->>'orphaned_cards', '0')::integer,
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Multi-Table%'
        ),
        'vsam_cross_reference_file_equivalent_testing', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'total_cards', COALESCE(details->>'total_cards', '0')::integer,
                'invalid_relationships', COALESCE(details->>'invalid_relationships', '0')::integer,
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_name LIKE '%Cross-Reference%'
        ),
        'data_consistency_validation_across_related_tables', (
            SELECT jsonb_build_object(
                'consistency_check_status', CASE 
                    WHEN NOT EXISTS (
                        SELECT 1 FROM validation_test.referential_integrity_results 
                        WHERE test_category = 'Cross-Reference Validation' AND test_status = 'FAIL'
                    ) THEN 'CONSISTENT'
                    ELSE 'INCONSISTENT'
                END,
                'failed_consistency_tests', (
                    SELECT COUNT(*) FROM validation_test.referential_integrity_results 
                    WHERE test_category = 'Cross-Reference Validation' AND test_status = 'FAIL'
                )
            )
        ),
        'referential_action_testing', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'action_type', CASE 
                    WHEN test_name LIKE '%Update%' THEN 'CASCADE'
                    WHEN test_name LIKE '%Delete%' THEN 'RESTRICT'
                    ELSE 'UNKNOWN'
                END,
                'execution_time', execution_time
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_category = 'Cascade Behavior Validation'
        ),
        'foreign_key_index_performance_validation', (
            SELECT jsonb_agg(jsonb_build_object(
                'test_name', test_name,
                'status', test_status,
                'query_time', details->>'query_time',
                'threshold', details->>'threshold',
                'performance_met', test_status = 'PASS'
            ))
            FROM validation_test.referential_integrity_results 
            WHERE test_category = 'Performance Validation'
        )
    ) AS CrossReferenceValidationTests;

-- Clean up validation test schema
DROP SCHEMA validation_test CASCADE;

-- Final status message
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM validation_test.referential_integrity_results WHERE test_status = 'FAIL') 
        THEN 'VALIDATION COMPLETED WITH FAILURES - INVESTIGATE REFERENTIAL INTEGRITY ISSUES'
        ELSE 'VALIDATION COMPLETED SUCCESSFULLY - ALL REFERENTIAL INTEGRITY CONSTRAINTS VALIDATED'
    END AS final_status;

-- ============================================================================
-- End of Referential Integrity Validation Script
-- ============================================================================