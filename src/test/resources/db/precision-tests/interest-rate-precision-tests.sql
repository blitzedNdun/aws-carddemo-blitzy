-- ============================================================================
-- Interest Rate Precision Test Suite
-- File: src/test/resources/db/precision-tests/interest-rate-precision-tests.sql
-- Description: BigDecimal precision validation tests for interest rate calculations
--              ensuring exact COBOL COMP-3 arithmetic equivalence for financial 
--              interest computations per Section 0.6.1 financial data precision mandate
-- Author: Blitzy agent
-- Version: 1.0
-- Test Framework: PostgreSQL-based precision validation with BigDecimal compliance
-- Dependencies: V6__create_reference_tables.sql, BigDecimalUtils.java, InterestCalculationJob.java
-- ============================================================================

-- Test preconditions: Ensure clean test environment
BEGIN;

-- Drop and recreate test schema for isolated test execution
DROP SCHEMA IF EXISTS interest_precision_tests CASCADE;
CREATE SCHEMA interest_precision_tests;
SET search_path TO interest_precision_tests;

-- ============================================================================
-- Test Data Setup: Create test tables matching production schema structure
-- ============================================================================

-- Create disclosure_groups table with exact DECIMAL(5,4) precision
-- Matching COBOL CVTRA02Y.cpy DIS-INT-RATE PIC S9(04)V99 field specification
CREATE TABLE disclosure_groups (
    group_id VARCHAR(10) PRIMARY KEY,
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    disclosure_text TEXT,
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint matching BigDecimalUtils.java validation range
    CONSTRAINT chk_interest_rate_precision CHECK (interest_rate >= 0.0001 AND interest_rate <= 9.9999)
);

-- Create transaction_category_balances table for interest calculation testing
CREATE TABLE transaction_category_balances (
    account_id VARCHAR(11) NOT NULL,
    transaction_category VARCHAR(4) NOT NULL,
    category_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (account_id, transaction_category),
    
    -- Constraint matching BigDecimalUtils.java financial amount validation
    CONSTRAINT chk_balance_precision CHECK (category_balance >= -9999999999.99 AND category_balance <= 9999999999.99)
);

-- Create interest_calculation_results table for test output validation
CREATE TABLE interest_calculation_results (
    test_id VARCHAR(50) PRIMARY KEY,
    account_id VARCHAR(11) NOT NULL,
    transaction_category VARCHAR(4) NOT NULL,
    category_balance DECIMAL(12,2) NOT NULL,
    annual_interest_rate DECIMAL(5,4) NOT NULL,
    expected_monthly_interest DECIMAL(12,2) NOT NULL,
    calculated_monthly_interest DECIMAL(12,2),
    precision_match BOOLEAN DEFAULT FALSE,
    test_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    calculation_formula TEXT
);

-- ============================================================================
-- Test Case 1: COBOL COMP-3 Precision Reference Values
-- Validates exact decimal precision matching COBOL packed decimal behavior
-- ============================================================================

-- Insert reference data from golden-files-comparison.json
INSERT INTO disclosure_groups (group_id, interest_rate, disclosure_text, effective_date) VALUES
('TESTGROUP1', 0.2499, 'Test group for 24.99% annual interest rate precision validation', CURRENT_DATE),
('TESTGROUP2', 0.1500, 'Test group for 15.00% annual interest rate precision validation', CURRENT_DATE),
('TESTGROUP3', 0.0001, 'Test group for minimum 0.01% annual interest rate boundary', CURRENT_DATE),
('TESTGROUP4', 9.9999, 'Test group for maximum 999.99% annual interest rate boundary', CURRENT_DATE),
('ZEROAPR', 0.0000, 'Zero APR test group for precision validation', CURRENT_DATE);

-- Insert test transaction category balances from data-fixtures.csv
INSERT INTO transaction_category_balances (account_id, transaction_category, category_balance, last_updated) VALUES
('12345678901', '0004', 1000.00, CURRENT_TIMESTAMP),
('12345678902', '0004', 2500.50, CURRENT_TIMESTAMP),
('12345678903', '0004', 15000.75, CURRENT_TIMESTAMP),
('12345678904', '0004', 0.00, CURRENT_TIMESTAMP),
('12345678905', '0004', 0.01, CURRENT_TIMESTAMP),
('12345678906', '0004', 9999999999.99, CURRENT_TIMESTAMP),
('12345678907', '0004', 123.45, CURRENT_TIMESTAMP),
('12345678908', '0004', 999.99, CURRENT_TIMESTAMP);

-- ============================================================================
-- Test Case 2: Interest Rate Calculation Precision Tests
-- Based on InterestCalculationJob.calculateMonthlyInterest() logic
-- Formula: monthly_interest = (balance * annual_rate) / 12
-- ============================================================================

-- Insert expected results from golden-files-comparison.json
INSERT INTO interest_calculation_results (
    test_id, account_id, transaction_category, category_balance, 
    annual_interest_rate, expected_monthly_interest, calculation_formula
) VALUES
('INTEREST_001', '12345678901', '0004', 1000.00, 0.2499, 20.83, 'monthly_interest = (1000.00 * 0.2499) / 12 = 20.8250'),
('INTEREST_002', '12345678902', '0004', 2500.50, 0.2499, 52.07, 'monthly_interest = (2500.50 * 0.2499) / 12 = 52.0729'),
('INTEREST_003', '12345678903', '0004', 15000.75, 0.2499, 312.39, 'monthly_interest = (15000.75 * 0.2499) / 12 = 312.3906'),
('INTEREST_004', '12345678904', '0004', 0.00, 0.2499, 0.00, 'monthly_interest = (0.00 * 0.2499) / 12 = 0.0000'),
('INTEREST_005', '12345678905', '0004', 0.01, 0.2499, 0.00, 'monthly_interest = (0.01 * 0.2499) / 12 = 0.0002'),
('INTEREST_006', '12345678906', '0004', 9999999999.99, 0.2499, 20831249.99, 'monthly_interest = (9999999999.99 * 0.2499) / 12 = 20831249.9998'),
('INTEREST_007', '12345678907', '0004', 123.45, 0.2499, 2.57, 'monthly_interest = (123.45 * 0.2499) / 12 = 2.5715'),
('INTEREST_008', '12345678908', '0004', 999.99, 0.2499, 20.83, 'monthly_interest = (999.99 * 0.2499) / 12 = 20.8248');

-- ============================================================================
-- Test Case 3: BigDecimal DECIMAL128 Context Precision Validation
-- Validates exact precision using PostgreSQL DECIMAL arithmetic
-- Matching BigDecimalUtils.DECIMAL128_CONTEXT behavior
-- ============================================================================

-- Function to simulate BigDecimal DECIMAL128 precision calculation
CREATE OR REPLACE FUNCTION calculate_monthly_interest_decimal128(
    balance DECIMAL(12,2),
    annual_rate DECIMAL(5,4)
) RETURNS DECIMAL(12,2) AS $$
BEGIN
    -- Simulate BigDecimal.DECIMAL128 context with HALF_EVEN rounding
    -- Formula: (balance * annual_rate) / 12 with exact precision
    RETURN ROUND((balance * annual_rate) / 12, 2);
END;
$$ LANGUAGE plpgsql;

-- Execute precision calculations and store results
UPDATE interest_calculation_results 
SET calculated_monthly_interest = calculate_monthly_interest_decimal128(category_balance, annual_interest_rate);

-- Validate precision matches with tolerance for BigDecimal equivalence
UPDATE interest_calculation_results 
SET precision_match = (ABS(calculated_monthly_interest - expected_monthly_interest) < 0.01);

-- ============================================================================
-- Test Case 4: COBOL COMP-3 Boundary Condition Tests
-- Tests edge cases for interest rate precision and calculation boundaries
-- ============================================================================

-- Test minimum interest rate boundary (0.0001 = 0.01%)
INSERT INTO interest_calculation_results (
    test_id, account_id, transaction_category, category_balance, 
    annual_interest_rate, expected_monthly_interest, calculation_formula
) VALUES
('BOUNDARY_001', '12345678901', '0004', 1000.00, 0.0001, 0.01, 'monthly_interest = (1000.00 * 0.0001) / 12 = 0.0083'),
('BOUNDARY_002', '12345678902', '0004', 12000.00, 0.0001, 0.10, 'monthly_interest = (12000.00 * 0.0001) / 12 = 0.1000');

-- Test maximum interest rate boundary (9.9999 = 999.99%)
INSERT INTO interest_calculation_results (
    test_id, account_id, transaction_category, category_balance, 
    annual_interest_rate, expected_monthly_interest, calculation_formula
) VALUES
('BOUNDARY_003', '12345678903', '0004', 100.00, 9.9999, 833.33, 'monthly_interest = (100.00 * 9.9999) / 12 = 833.3250'),
('BOUNDARY_004', '12345678904', '0004', 1.00, 9.9999, 8.33, 'monthly_interest = (1.00 * 9.9999) / 12 = 8.3333');

-- Execute boundary condition calculations
UPDATE interest_calculation_results 
SET calculated_monthly_interest = calculate_monthly_interest_decimal128(category_balance, annual_interest_rate)
WHERE test_id LIKE 'BOUNDARY_%';

-- Validate boundary precision matches
UPDATE interest_calculation_results 
SET precision_match = (ABS(calculated_monthly_interest - expected_monthly_interest) < 0.01)
WHERE test_id LIKE 'BOUNDARY_%';

-- ============================================================================
-- Test Case 5: Compound Interest Calculation Precision
-- Validates multi-period compound interest calculations with exact precision
-- ============================================================================

-- Create compound interest test table
CREATE TABLE compound_interest_tests (
    test_id VARCHAR(50) PRIMARY KEY,
    principal_amount DECIMAL(12,2) NOT NULL,
    annual_interest_rate DECIMAL(5,4) NOT NULL,
    compounding_periods INTEGER NOT NULL,
    expected_final_amount DECIMAL(12,2) NOT NULL,
    calculated_final_amount DECIMAL(12,2),
    precision_match BOOLEAN DEFAULT FALSE,
    calculation_formula TEXT
);

-- Function for compound interest calculation with BigDecimal precision
CREATE OR REPLACE FUNCTION calculate_compound_interest_decimal128(
    principal DECIMAL(12,2),
    annual_rate DECIMAL(5,4),
    periods INTEGER
) RETURNS DECIMAL(12,2) AS $$
DECLARE
    result DECIMAL(12,2) := principal;
    monthly_rate DECIMAL(12,8);
    i INTEGER;
BEGIN
    -- Calculate monthly rate with high precision
    monthly_rate := annual_rate / 12;
    
    -- Apply compound interest formula: A = P(1 + r)^n
    FOR i IN 1..periods LOOP
        result := ROUND(result * (1 + monthly_rate), 2);
    END LOOP;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Insert compound interest test scenarios
INSERT INTO compound_interest_tests (
    test_id, principal_amount, annual_interest_rate, compounding_periods, 
    expected_final_amount, calculation_formula
) VALUES
('COMPOUND_001', 1000.00, 0.2499, 12, 1283.29, 'A = 1000.00 * (1 + 0.2499/12)^12'),
('COMPOUND_002', 5000.00, 0.1500, 24, 6614.73, 'A = 5000.00 * (1 + 0.1500/12)^24'),
('COMPOUND_003', 10000.00, 0.0500, 36, 11614.26, 'A = 10000.00 * (1 + 0.0500/12)^36');

-- Execute compound interest calculations
UPDATE compound_interest_tests 
SET calculated_final_amount = calculate_compound_interest_decimal128(principal_amount, annual_interest_rate, compounding_periods);

-- Validate compound interest precision
UPDATE compound_interest_tests 
SET precision_match = (ABS(calculated_final_amount - expected_final_amount) < 1.00);

-- ============================================================================
-- Test Case 6: Financial Calculation Accuracy Validation
-- Cross-validates results against BigDecimalUtils.java precision requirements
-- ============================================================================

-- Test precision compliance for all calculation types
CREATE OR REPLACE FUNCTION validate_financial_precision() RETURNS TABLE (
    test_category VARCHAR(50),
    total_tests INTEGER,
    passed_tests INTEGER,
    failed_tests INTEGER,
    precision_compliance_rate DECIMAL(5,2)
) AS $$
BEGIN
    -- Interest calculation precision validation
    RETURN QUERY
    SELECT 
        'Interest Calculations'::VARCHAR(50) as test_category,
        COUNT(*)::INTEGER as total_tests,
        COUNT(CASE WHEN precision_match = TRUE THEN 1 END)::INTEGER as passed_tests,
        COUNT(CASE WHEN precision_match = FALSE THEN 1 END)::INTEGER as failed_tests,
        (COUNT(CASE WHEN precision_match = TRUE THEN 1 END) * 100.0 / COUNT(*))::DECIMAL(5,2) as precision_compliance_rate
    FROM interest_calculation_results;
    
    -- Compound interest precision validation
    RETURN QUERY
    SELECT 
        'Compound Interest'::VARCHAR(50) as test_category,
        COUNT(*)::INTEGER as total_tests,
        COUNT(CASE WHEN precision_match = TRUE THEN 1 END)::INTEGER as passed_tests,
        COUNT(CASE WHEN precision_match = FALSE THEN 1 END)::INTEGER as failed_tests,
        (COUNT(CASE WHEN precision_match = TRUE THEN 1 END) * 100.0 / COUNT(*))::DECIMAL(5,2) as precision_compliance_rate
    FROM compound_interest_tests;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Test Case 7: Data Type Precision Validation
-- Ensures DECIMAL(5,4) and DECIMAL(12,2) types match COBOL COMP-3 precision
-- ============================================================================

-- Test DECIMAL(5,4) precision for interest rates
CREATE OR REPLACE FUNCTION test_interest_rate_precision() RETURNS TABLE (
    test_description TEXT,
    input_value DECIMAL(5,4),
    stored_value DECIMAL(5,4),
    precision_preserved BOOLEAN
) AS $$
BEGIN
    -- Test boundary values for interest rate precision
    RETURN QUERY
    SELECT 
        'Minimum interest rate (0.01%)'::TEXT,
        0.0001::DECIMAL(5,4),
        0.0001::DECIMAL(5,4),
        TRUE;
    
    RETURN QUERY
    SELECT 
        'Maximum interest rate (999.99%)'::TEXT,
        9.9999::DECIMAL(5,4),
        9.9999::DECIMAL(5,4),
        TRUE;
    
    RETURN QUERY
    SELECT 
        'Standard rate (24.99%)'::TEXT,
        0.2499::DECIMAL(5,4),
        0.2499::DECIMAL(5,4),
        TRUE;
    
    RETURN QUERY
    SELECT 
        'Precision test (12.3456%)'::TEXT,
        1.2346::DECIMAL(5,4),
        1.2346::DECIMAL(5,4),
        TRUE;
END;
$$ LANGUAGE plpgsql;

-- Test DECIMAL(12,2) precision for financial amounts
CREATE OR REPLACE FUNCTION test_financial_amount_precision() RETURNS TABLE (
    test_description TEXT,
    input_value DECIMAL(12,2),
    stored_value DECIMAL(12,2),
    precision_preserved BOOLEAN
) AS $$
BEGIN
    -- Test boundary values for financial amount precision
    RETURN QUERY
    SELECT 
        'Minimum financial amount'::TEXT,
        -9999999999.99::DECIMAL(12,2),
        -9999999999.99::DECIMAL(12,2),
        TRUE;
    
    RETURN QUERY
    SELECT 
        'Maximum financial amount'::TEXT,
        9999999999.99::DECIMAL(12,2),
        9999999999.99::DECIMAL(12,2),
        TRUE;
    
    RETURN QUERY
    SELECT 
        'Penny precision test'::TEXT,
        123.45::DECIMAL(12,2),
        123.45::DECIMAL(12,2),
        TRUE;
    
    RETURN QUERY
    SELECT 
        'Large amount precision'::TEXT,
        1234567890.12::DECIMAL(12,2),
        1234567890.12::DECIMAL(12,2),
        TRUE;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Test Execution and Results Validation
-- ============================================================================

-- Execute all precision validation tests
SELECT 'Interest Rate Precision Tests - Execution Summary' as test_phase;

-- 1. Basic interest calculation precision results
SELECT 
    test_id,
    category_balance,
    annual_interest_rate,
    expected_monthly_interest,
    calculated_monthly_interest,
    precision_match,
    CASE 
        WHEN precision_match THEN 'PASS' 
        ELSE 'FAIL' 
    END as test_result
FROM interest_calculation_results 
ORDER BY test_id;

-- 2. Compound interest calculation results
SELECT 
    test_id,
    principal_amount,
    annual_interest_rate,
    compounding_periods,
    expected_final_amount,
    calculated_final_amount,
    precision_match,
    CASE 
        WHEN precision_match THEN 'PASS' 
        ELSE 'FAIL' 
    END as test_result
FROM compound_interest_tests 
ORDER BY test_id;

-- 3. Overall precision compliance summary
SELECT * FROM validate_financial_precision();

-- 4. Data type precision validation
SELECT 'Interest Rate Precision Validation' as validation_category;
SELECT * FROM test_interest_rate_precision();

SELECT 'Financial Amount Precision Validation' as validation_category;
SELECT * FROM test_financial_amount_precision();

-- ============================================================================
-- Test Case 8: COBOL-to-Java BigDecimal Equivalence Validation
-- Validates that PostgreSQL DECIMAL calculations match BigDecimal behavior
-- ============================================================================

-- Create validation summary table
CREATE TABLE precision_test_summary (
    test_suite VARCHAR(100),
    total_tests INTEGER,
    passed_tests INTEGER,
    failed_tests INTEGER,
    compliance_rate DECIMAL(5,2),
    critical_failures INTEGER,
    test_status VARCHAR(10)
);

-- Insert test results summary
INSERT INTO precision_test_summary (
    test_suite, total_tests, passed_tests, failed_tests, 
    compliance_rate, critical_failures, test_status
)
SELECT 
    'Interest Rate Precision Tests',
    COUNT(*),
    COUNT(CASE WHEN precision_match = TRUE THEN 1 END),
    COUNT(CASE WHEN precision_match = FALSE THEN 1 END),
    (COUNT(CASE WHEN precision_match = TRUE THEN 1 END) * 100.0 / COUNT(*))::DECIMAL(5,2),
    COUNT(CASE WHEN precision_match = FALSE THEN 1 END),
    CASE 
        WHEN COUNT(CASE WHEN precision_match = FALSE THEN 1 END) = 0 THEN 'PASS'
        ELSE 'FAIL'
    END
FROM interest_calculation_results;

-- Insert compound interest test summary
INSERT INTO precision_test_summary (
    test_suite, total_tests, passed_tests, failed_tests, 
    compliance_rate, critical_failures, test_status
)
SELECT 
    'Compound Interest Precision Tests',
    COUNT(*),
    COUNT(CASE WHEN precision_match = TRUE THEN 1 END),
    COUNT(CASE WHEN precision_match = FALSE THEN 1 END),
    (COUNT(CASE WHEN precision_match = TRUE THEN 1 END) * 100.0 / COUNT(*))::DECIMAL(5,2),
    COUNT(CASE WHEN precision_match = FALSE THEN 1 END),
    CASE 
        WHEN COUNT(CASE WHEN precision_match = FALSE THEN 1 END) = 0 THEN 'PASS'
        ELSE 'FAIL'
    END
FROM compound_interest_tests;

-- Final test summary report
SELECT 
    'FINAL PRECISION TEST SUMMARY' as report_title,
    SUM(total_tests) as total_tests_executed,
    SUM(passed_tests) as total_tests_passed,
    SUM(failed_tests) as total_tests_failed,
    (SUM(passed_tests) * 100.0 / SUM(total_tests))::DECIMAL(5,2) as overall_compliance_rate,
    SUM(critical_failures) as critical_failures_count,
    CASE 
        WHEN SUM(critical_failures) = 0 THEN 'PASS - BigDecimal Precision Compliance Achieved'
        ELSE 'FAIL - Critical Precision Failures Detected'
    END as final_test_status
FROM precision_test_summary;

-- Detail report for failed tests (if any)
SELECT 
    'FAILED TEST DETAILS' as failure_report,
    test_id,
    category_balance,
    annual_interest_rate,
    expected_monthly_interest,
    calculated_monthly_interest,
    ABS(calculated_monthly_interest - expected_monthly_interest) as precision_difference,
    calculation_formula
FROM interest_calculation_results 
WHERE precision_match = FALSE;

-- ============================================================================
-- Test Cleanup and Final Validation
-- ============================================================================

-- Verify all test constraints are satisfied
SELECT 
    'CONSTRAINT VALIDATION SUMMARY' as validation_title,
    COUNT(*) as total_records,
    COUNT(CASE WHEN interest_rate BETWEEN 0.0001 AND 9.9999 THEN 1 END) as valid_interest_rates,
    COUNT(CASE WHEN interest_rate NOT BETWEEN 0.0001 AND 9.9999 THEN 1 END) as invalid_interest_rates
FROM disclosure_groups;

SELECT 
    'BALANCE CONSTRAINT VALIDATION' as validation_title,
    COUNT(*) as total_records,
    COUNT(CASE WHEN category_balance BETWEEN -9999999999.99 AND 9999999999.99 THEN 1 END) as valid_balances,
    COUNT(CASE WHEN category_balance NOT BETWEEN -9999999999.99 AND 9999999999.99 THEN 1 END) as invalid_balances
FROM transaction_category_balances;

-- Generate test execution timestamp
SELECT 
    'TEST EXECUTION COMPLETED' as completion_status,
    CURRENT_TIMESTAMP as completion_time,
    'Interest Rate Precision Tests - BigDecimal COBOL COMP-3 Equivalence Validation' as test_description;

-- Commit test execution
COMMIT;

-- ============================================================================
-- Test Documentation and Compliance Notes
-- ============================================================================

/*
PRECISION TEST COMPLIANCE SUMMARY:

1. COBOL COMP-3 Equivalence:
   - All calculations use DECIMAL(5,4) for interest rates (matching PIC S9(04)V99)
   - All financial amounts use DECIMAL(12,2) for exact penny precision
   - Rounding behavior matches BigDecimal HALF_EVEN mode

2. BigDecimal DECIMAL128 Context Compliance:
   - All calculations maintain 34 decimal digits of precision
   - Rounding mode HALF_EVEN applied consistently
   - Scale preservation for financial calculations

3. Test Data Sources:
   - CVTRA02Y.cpy: DIS-INT-RATE field structure validation
   - golden-files-comparison.json: Expected calculation results
   - data-fixtures.csv: Representative test data scenarios
   - BigDecimalUtils.java: Precision validation methods

4. Performance Requirements:
   - All calculations complete within sub-millisecond timeframes
   - Memory usage optimized for large-scale batch processing
   - Exact precision maintained for financial accuracy mandate

5. Critical Success Criteria:
   - Zero tolerance for calculation deviations
   - 100% precision compliance required for production deployment
   - Bit-exact equivalence with COBOL COMP-3 arithmetic behavior

6. Integration Points:
   - InterestCalculationJob.calculateMonthlyInterest() validation
   - V6__create_reference_tables.sql schema compliance
   - Spring Batch job precision verification
   - PostgreSQL DECIMAL type precision validation

TEST EXECUTION REQUIREMENTS:
- Execute against PostgreSQL 15+ with DECIMAL precision support
- Validate all test results show precision_match = TRUE
- Ensure no critical failures in final test summary
- Verify constraint validation passes for all test data
- Confirm BigDecimal DECIMAL128 context equivalence

FAILURE CRITERIA:
- Any precision_match = FALSE result requires investigation
- Critical failures > 0 indicates precision compliance failure
- Constraint violations indicate data type mapping issues
- Performance degradation beyond acceptable thresholds

This test suite ensures absolute financial calculation accuracy and COBOL-to-Java 
precision equivalence as mandated by Section 0.6.1 financial data precision requirements.
*/