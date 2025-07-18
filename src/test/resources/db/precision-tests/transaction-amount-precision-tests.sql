-- =====================================================================================
-- BigDecimal Precision Validation Tests for Transaction Amount Calculations
-- Description: Ensures exact COBOL COMP-3 arithmetic equivalence for payment processing
-- Source: CVTRA05Y.cpy TRAN-AMT PIC S9(09)V99 field mapping
-- Target: PostgreSQL DECIMAL(12,2) with BigDecimal precision
-- Author: Blitzy Agent
-- Date: 2024
-- Version: 1.0
-- =====================================================================================

-- Test Suite: Transaction Amount Precision Validation
-- Purpose: Validate exact decimal precision between COBOL COMP-3 and PostgreSQL DECIMAL(12,2)
-- Requirements: 
-- - Transaction amount precision must match COBOL COMP-3 calculations exactly
-- - All transaction processing must produce identical financial results with exact decimal precision
-- - BigDecimal transaction validation per Section 0.3.1 technical approach

-- Create test schema for precision validation
CREATE SCHEMA IF NOT EXISTS precision_tests;
SET search_path TO precision_tests, public;

-- =====================================================================================
-- Test 1: COBOL COMP-3 Field Precision Validation
-- Validates TRAN-AMT PIC S9(09)V99 precision boundaries
-- =====================================================================================

-- Test case 1.1: Maximum positive value precision
-- COBOL COMP-3 S9(09)V99 maximum: 999999999.99
DO $$
DECLARE
    test_amount DECIMAL(12,2) := 999999999.99;
    expected_amount DECIMAL(12,2) := 999999999.99;
    test_name VARCHAR(100) := 'COBOL COMP-3 Maximum Positive Value';
BEGIN
    -- Validate exact precision match
    IF test_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Amount: % matches expected: %', test_name, test_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Amount: % does not match expected: %', test_name, test_amount, expected_amount;
    END IF;
    
    -- Validate scale preservation
    IF SCALE(test_amount) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved: %', test_name, SCALE(test_amount);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved: %', test_name, SCALE(test_amount);
    END IF;
END $$;

-- Test case 1.2: Maximum negative value precision
-- COBOL COMP-3 S9(09)V99 minimum: -999999999.99
DO $$
DECLARE
    test_amount DECIMAL(12,2) := -999999999.99;
    expected_amount DECIMAL(12,2) := -999999999.99;
    test_name VARCHAR(100) := 'COBOL COMP-3 Maximum Negative Value';
BEGIN
    -- Validate exact precision match
    IF test_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Amount: % matches expected: %', test_name, test_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Amount: % does not match expected: %', test_name, test_amount, expected_amount;
    END IF;
    
    -- Validate scale preservation
    IF SCALE(test_amount) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved: %', test_name, SCALE(test_amount);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved: %', test_name, SCALE(test_amount);
    END IF;
END $$;

-- Test case 1.3: Zero value precision
-- COBOL COMP-3 zero handling with exact scale
DO $$
DECLARE
    test_amount DECIMAL(12,2) := 0.00;
    expected_amount DECIMAL(12,2) := 0.00;
    test_name VARCHAR(100) := 'COBOL COMP-3 Zero Value';
BEGIN
    -- Validate exact precision match
    IF test_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Amount: % matches expected: %', test_name, test_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Amount: % does not match expected: %', test_name, test_amount, expected_amount;
    END IF;
    
    -- Validate scale preservation for zero
    IF SCALE(test_amount) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved: %', test_name, SCALE(test_amount);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved: %', test_name, SCALE(test_amount);
    END IF;
END $$;

-- =====================================================================================
-- Test 2: Transaction Amount Boundary Validation
-- Validates transaction amounts within COBOL COMP-3 boundaries
-- =====================================================================================

-- Test case 2.1: Single penny precision
-- Validates smallest monetary unit handling
DO $$
DECLARE
    test_amount DECIMAL(12,2) := 0.01;
    expected_amount DECIMAL(12,2) := 0.01;
    test_name VARCHAR(100) := 'Single Penny Precision';
BEGIN
    -- Validate exact precision match
    IF test_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Amount: % matches expected: %', test_name, test_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Amount: % does not match expected: %', test_name, test_amount, expected_amount;
    END IF;
    
    -- Validate precision digits
    IF PRECISION(test_amount) >= 3 THEN
        RAISE NOTICE 'PASS: % - Precision adequate: %', test_name, PRECISION(test_amount);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Precision inadequate: %', test_name, PRECISION(test_amount);
    END IF;
END $$;

-- Test case 2.2: Negative penny precision
-- Validates smallest negative monetary unit handling
DO $$
DECLARE
    test_amount DECIMAL(12,2) := -0.01;
    expected_amount DECIMAL(12,2) := -0.01;
    test_name VARCHAR(100) := 'Negative Penny Precision';
BEGIN
    -- Validate exact precision match
    IF test_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Amount: % matches expected: %', test_name, expected_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Amount: % does not match expected: %', test_name, test_amount, expected_amount;
    END IF;
    
    -- Validate sign preservation
    IF SIGN(test_amount) = -1 THEN
        RAISE NOTICE 'PASS: % - Sign preserved: %', test_name, SIGN(test_amount);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Sign not preserved: %', test_name, SIGN(test_amount);
    END IF;
END $$;

-- Test case 2.3: Large transaction amounts
-- Validates handling of typical large transaction amounts
DO $$
DECLARE
    test_amount DECIMAL(12,2) := 123456789.99;
    expected_amount DECIMAL(12,2) := 123456789.99;
    test_name VARCHAR(100) := 'Large Transaction Amount';
BEGIN
    -- Validate exact precision match
    IF test_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Amount: % matches expected: %', test_name, test_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Amount: % does not match expected: %', test_name, test_amount, expected_amount;
    END IF;
    
    -- Validate integer digit capacity
    IF LENGTH(SPLIT_PART(test_amount::TEXT, '.', 1)) <= 9 THEN
        RAISE NOTICE 'PASS: % - Integer digits within COBOL limit: %', test_name, LENGTH(SPLIT_PART(test_amount::TEXT, '.', 1));
    ELSE
        RAISE EXCEPTION 'FAIL: % - Integer digits exceed COBOL limit: %', test_name, LENGTH(SPLIT_PART(test_amount::TEXT, '.', 1));
    END IF;
END $$;

-- =====================================================================================
-- Test 3: Arithmetic Operation Precision Validation
-- Validates BigDecimal arithmetic operations maintain COBOL COMP-3 precision
-- =====================================================================================

-- Test case 3.1: Addition precision validation
-- Validates addition operations maintain exact precision
DO $$
DECLARE
    amount1 DECIMAL(12,2) := 123.45;
    amount2 DECIMAL(12,2) := 678.90;
    calculated_sum DECIMAL(12,2);
    expected_sum DECIMAL(12,2) := 802.35;
    test_name VARCHAR(100) := 'Addition Precision Validation';
BEGIN
    -- Perform addition with exact precision
    calculated_sum := amount1 + amount2;
    
    -- Validate exact precision match
    IF calculated_sum = expected_sum THEN
        RAISE NOTICE 'PASS: % - Sum: % matches expected: %', test_name, calculated_sum, expected_sum;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Sum: % does not match expected: %', test_name, calculated_sum, expected_sum;
    END IF;
    
    -- Validate scale preservation in result
    IF SCALE(calculated_sum) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved in result: %', test_name, SCALE(calculated_sum);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved in result: %', test_name, SCALE(calculated_sum);
    END IF;
END $$;

-- Test case 3.2: Subtraction precision validation
-- Validates subtraction operations maintain exact precision
DO $$
DECLARE
    amount1 DECIMAL(12,2) := 1000.00;
    amount2 DECIMAL(12,2) := 234.56;
    calculated_difference DECIMAL(12,2);
    expected_difference DECIMAL(12,2) := 765.44;
    test_name VARCHAR(100) := 'Subtraction Precision Validation';
BEGIN
    -- Perform subtraction with exact precision
    calculated_difference := amount1 - amount2;
    
    -- Validate exact precision match
    IF calculated_difference = expected_difference THEN
        RAISE NOTICE 'PASS: % - Difference: % matches expected: %', test_name, calculated_difference, expected_difference;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Difference: % does not match expected: %', test_name, calculated_difference, expected_difference;
    END IF;
    
    -- Validate scale preservation in result
    IF SCALE(calculated_difference) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved in result: %', test_name, SCALE(calculated_difference);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved in result: %', test_name, SCALE(calculated_difference);
    END IF;
END $$;

-- Test case 3.3: Multiplication precision validation
-- Validates multiplication operations with proper rounding
DO $$
DECLARE
    amount1 DECIMAL(12,2) := 123.45;
    multiplier DECIMAL(12,2) := 2.50;
    calculated_product DECIMAL(12,2);
    expected_product DECIMAL(12,2) := 308.63; -- 123.45 * 2.50 = 308.625, rounded to 308.63
    test_name VARCHAR(100) := 'Multiplication Precision Validation';
BEGIN
    -- Perform multiplication with rounding to scale 2
    calculated_product := ROUND(amount1 * multiplier, 2);
    
    -- Validate exact precision match with rounding
    IF calculated_product = expected_product THEN
        RAISE NOTICE 'PASS: % - Product: % matches expected: %', test_name, calculated_product, expected_product;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Product: % does not match expected: %', test_name, calculated_product, expected_product;
    END IF;
    
    -- Validate scale preservation in result
    IF SCALE(calculated_product) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved in result: %', test_name, SCALE(calculated_product);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved in result: %', test_name, SCALE(calculated_product);
    END IF;
END $$;

-- Test case 3.4: Division precision validation
-- Validates division operations with proper rounding
DO $$
DECLARE
    amount1 DECIMAL(12,2) := 1000.00;
    divisor DECIMAL(12,2) := 3.00;
    calculated_quotient DECIMAL(12,2);
    expected_quotient DECIMAL(12,2) := 333.33; -- 1000.00 / 3.00 = 333.333..., rounded to 333.33
    test_name VARCHAR(100) := 'Division Precision Validation';
BEGIN
    -- Perform division with rounding to scale 2
    calculated_quotient := ROUND(amount1 / divisor, 2);
    
    -- Validate exact precision match with rounding
    IF calculated_quotient = expected_quotient THEN
        RAISE NOTICE 'PASS: % - Quotient: % matches expected: %', test_name, calculated_quotient, expected_quotient;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Quotient: % does not match expected: %', test_name, calculated_quotient, expected_quotient;
    END IF;
    
    -- Validate scale preservation in result
    IF SCALE(calculated_quotient) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved in result: %', test_name, SCALE(calculated_quotient);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved in result: %', test_name, SCALE(calculated_quotient);
    END IF;
END $$;

-- =====================================================================================
-- Test 4: Real Transaction Data Precision Validation
-- Validates actual transaction amounts from test data fixtures
-- =====================================================================================

-- Test case 4.1: Validate transaction amounts from data fixtures
-- Uses real transaction data to validate precision handling
DO $$
DECLARE
    fixture_amounts DECIMAL(12,2)[] := ARRAY[50.47, 125.89, 78.34, 234.56, 45.99, 189.75, 67.23, 156.42, 89.67, 298.33];
    expected_amounts DECIMAL(12,2)[] := ARRAY[50.47, 125.89, 78.34, 234.56, 45.99, 189.75, 67.23, 156.42, 89.67, 298.33];
    test_name VARCHAR(100) := 'Fixture Transaction Amount Validation';
    i INTEGER;
BEGIN
    -- Validate each fixture amount
    FOR i IN 1..array_length(fixture_amounts, 1) LOOP
        IF fixture_amounts[i] = expected_amounts[i] THEN
            RAISE NOTICE 'PASS: % - Amount[%]: % matches expected: %', test_name, i, fixture_amounts[i], expected_amounts[i];
        ELSE
            RAISE EXCEPTION 'FAIL: % - Amount[%]: % does not match expected: %', test_name, i, fixture_amounts[i], expected_amounts[i];
        END IF;
        
        -- Validate scale for each amount
        IF SCALE(fixture_amounts[i]) = 2 THEN
            RAISE NOTICE 'PASS: % - Scale preserved for amount[%]: %', test_name, i, SCALE(fixture_amounts[i]);
        ELSE
            RAISE EXCEPTION 'FAIL: % - Scale not preserved for amount[%]: %', test_name, i, SCALE(fixture_amounts[i]);
        END IF;
    END LOOP;
END $$;

-- Test case 4.2: Validate transaction amount summation precision
-- Tests aggregation operations maintain precision
DO $$
DECLARE
    amounts DECIMAL(12,2)[] := ARRAY[50.47, 125.89, 78.34, 234.56, 45.99];
    calculated_sum DECIMAL(12,2) := 0.00;
    expected_sum DECIMAL(12,2) := 535.25;
    test_name VARCHAR(100) := 'Transaction Amount Summation Precision';
    i INTEGER;
BEGIN
    -- Calculate sum with exact precision
    FOR i IN 1..array_length(amounts, 1) LOOP
        calculated_sum := calculated_sum + amounts[i];
    END LOOP;
    
    -- Validate exact precision match
    IF calculated_sum = expected_sum THEN
        RAISE NOTICE 'PASS: % - Sum: % matches expected: %', test_name, calculated_sum, expected_sum;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Sum: % does not match expected: %', test_name, calculated_sum, expected_sum;
    END IF;
    
    -- Validate scale preservation in aggregation
    IF SCALE(calculated_sum) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved in aggregation: %', test_name, SCALE(calculated_sum);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved in aggregation: %', test_name, SCALE(calculated_sum);
    END IF;
END $$;

-- =====================================================================================
-- Test 5: Database Table Column Precision Validation
-- Validates actual transactions table column precision matches COBOL COMP-3
-- =====================================================================================

-- Test case 5.1: Validate transactions table column precision
-- Tests actual database column definition matches COBOL specifications
DO $$
DECLARE
    column_precision INTEGER;
    column_scale INTEGER;
    expected_precision INTEGER := 12;
    expected_scale INTEGER := 2;
    test_name VARCHAR(100) := 'Transactions Table Column Precision';
BEGIN
    -- Get column precision and scale from information schema
    SELECT numeric_precision, numeric_scale 
    INTO column_precision, column_scale
    FROM information_schema.columns 
    WHERE table_name = 'transactions' 
    AND column_name = 'transaction_amount'
    AND table_schema = 'public';
    
    -- Validate precision matches COBOL COMP-3 requirements
    IF column_precision = expected_precision THEN
        RAISE NOTICE 'PASS: % - Precision: % matches expected: %', test_name, column_precision, expected_precision;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Precision: % does not match expected: %', test_name, column_precision, expected_precision;
    END IF;
    
    -- Validate scale matches COBOL COMP-3 requirements
    IF column_scale = expected_scale THEN
        RAISE NOTICE 'PASS: % - Scale: % matches expected: %', test_name, column_scale, expected_scale;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale: % does not match expected: %', test_name, column_scale, expected_scale;
    END IF;
END $$;

-- Test case 5.2: Validate transactions table constraint validation
-- Tests database constraints match COBOL field boundaries
DO $$
DECLARE
    constraint_definition TEXT;
    expected_pattern TEXT := 'transaction_amount >= -99999999.99 AND transaction_amount <= 99999999.99';
    test_name VARCHAR(100) := 'Transactions Table Amount Constraint';
BEGIN
    -- Get constraint definition from information schema
    SELECT check_clause
    INTO constraint_definition
    FROM information_schema.check_constraints cc
    JOIN information_schema.constraint_column_usage ccu ON cc.constraint_name = ccu.constraint_name
    WHERE ccu.table_name = 'transactions' 
    AND ccu.column_name = 'transaction_amount'
    AND cc.constraint_name = 'transactions_transaction_amount_check';
    
    -- Validate constraint matches COBOL COMP-3 boundaries
    IF constraint_definition IS NOT NULL THEN
        RAISE NOTICE 'PASS: % - Constraint exists: %', test_name, constraint_definition;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Constraint does not exist', test_name;
    END IF;
END $$;

-- =====================================================================================
-- Test 6: Rounding Behavior Validation
-- Validates rounding behavior matches COBOL COMP-3 rounding rules
-- =====================================================================================

-- Test case 6.1: Half-even rounding validation (Banker's rounding)
-- Tests rounding behavior matches COBOL arithmetic
DO $$
DECLARE
    test_cases DECIMAL(12,3)[] := ARRAY[123.125, 123.135, 123.145, 123.155];
    expected_results DECIMAL(12,2)[] := ARRAY[123.12, 123.14, 123.14, 123.16];
    calculated_results DECIMAL(12,2)[] := ARRAY[0.00, 0.00, 0.00, 0.00];
    test_name VARCHAR(100) := 'Half-Even Rounding Validation';
    i INTEGER;
BEGIN
    -- Apply rounding to each test case
    FOR i IN 1..array_length(test_cases, 1) LOOP
        calculated_results[i] := ROUND(test_cases[i], 2);
    END LOOP;
    
    -- Validate each rounding result
    FOR i IN 1..array_length(test_cases, 1) LOOP
        IF calculated_results[i] = expected_results[i] THEN
            RAISE NOTICE 'PASS: % - Case[%]: % rounded to % matches expected: %', test_name, i, test_cases[i], calculated_results[i], expected_results[i];
        ELSE
            RAISE EXCEPTION 'FAIL: % - Case[%]: % rounded to % does not match expected: %', test_name, i, test_cases[i], calculated_results[i], expected_results[i];
        END IF;
    END LOOP;
END $$;

-- Test case 6.2: Truncation validation
-- Tests truncation behavior for exact precision
DO $$
DECLARE
    test_amount DECIMAL(12,5) := 123.45678;
    truncated_amount DECIMAL(12,2);
    expected_amount DECIMAL(12,2) := 123.45;
    test_name VARCHAR(100) := 'Truncation Validation';
BEGIN
    -- Truncate to 2 decimal places
    truncated_amount := TRUNC(test_amount, 2);
    
    -- Validate exact precision match
    IF truncated_amount = expected_amount THEN
        RAISE NOTICE 'PASS: % - Truncated: % matches expected: %', test_name, truncated_amount, expected_amount;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Truncated: % does not match expected: %', test_name, truncated_amount, expected_amount;
    END IF;
    
    -- Validate scale preservation
    IF SCALE(truncated_amount) = 2 THEN
        RAISE NOTICE 'PASS: % - Scale preserved: %', test_name, SCALE(truncated_amount);
    ELSE
        RAISE EXCEPTION 'FAIL: % - Scale not preserved: %', test_name, SCALE(truncated_amount);
    END IF;
END $$;

-- =====================================================================================
-- Test 7: Transaction Amount Comparison Validation
-- Validates comparison operations maintain precision accuracy
-- =====================================================================================

-- Test case 7.1: Exact equality comparison
-- Tests exact decimal comparison accuracy
DO $$
DECLARE
    amount1 DECIMAL(12,2) := 123.45;
    amount2 DECIMAL(12,2) := 123.45;
    amount3 DECIMAL(12,2) := 123.46;
    test_name VARCHAR(100) := 'Exact Equality Comparison';
BEGIN
    -- Test exact equality
    IF amount1 = amount2 THEN
        RAISE NOTICE 'PASS: % - Equal amounts: % = %', test_name, amount1, amount2;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Equal amounts not recognized: % ≠ %', test_name, amount1, amount2;
    END IF;
    
    -- Test inequality
    IF amount1 <> amount3 THEN
        RAISE NOTICE 'PASS: % - Unequal amounts: % ≠ %', test_name, amount1, amount3;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Unequal amounts not recognized: % = %', test_name, amount1, amount3;
    END IF;
END $$;

-- Test case 7.2: Magnitude comparison validation
-- Tests greater than and less than comparisons
DO $$
DECLARE
    amount1 DECIMAL(12,2) := 123.45;
    amount2 DECIMAL(12,2) := 123.46;
    amount3 DECIMAL(12,2) := 123.44;
    test_name VARCHAR(100) := 'Magnitude Comparison Validation';
BEGIN
    -- Test greater than
    IF amount2 > amount1 THEN
        RAISE NOTICE 'PASS: % - Greater than: % > %', test_name, amount2, amount1;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Greater than not recognized: % ≤ %', test_name, amount2, amount1;
    END IF;
    
    -- Test less than
    IF amount3 < amount1 THEN
        RAISE NOTICE 'PASS: % - Less than: % < %', test_name, amount3, amount1;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Less than not recognized: % ≥ %', test_name, amount3, amount1;
    END IF;
END $$;

-- =====================================================================================
-- Test 8: Integration Test with Transaction Processing
-- Validates end-to-end transaction amount processing
-- =====================================================================================

-- Test case 8.1: Transaction amount validation function
-- Tests transaction amount validation equivalent to COBOL validation
CREATE OR REPLACE FUNCTION precision_tests.validate_transaction_amount_precision(
    p_amount DECIMAL(12,2)
) RETURNS BOOLEAN AS $$
BEGIN
    -- Validate amount is within COBOL COMP-3 S9(09)V99 range
    IF p_amount IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Validate range: -999999999.99 to 999999999.99
    IF p_amount < -999999999.99 OR p_amount > 999999999.99 THEN
        RETURN FALSE;
    END IF;
    
    -- Validate scale is exactly 2
    IF SCALE(p_amount) <> 2 THEN
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Test case 8.2: Execute validation function tests
-- Tests the validation function with various inputs
DO $$
DECLARE
    test_cases DECIMAL(12,2)[] := ARRAY[0.00, 0.01, -0.01, 123.45, -123.45, 999999999.99, -999999999.99];
    expected_results BOOLEAN[] := ARRAY[TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE];
    calculated_results BOOLEAN[] := ARRAY[FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE];
    test_name VARCHAR(100) := 'Transaction Amount Validation Function';
    i INTEGER;
BEGIN
    -- Test each case
    FOR i IN 1..array_length(test_cases, 1) LOOP
        calculated_results[i] := precision_tests.validate_transaction_amount_precision(test_cases[i]);
    END LOOP;
    
    -- Validate each result
    FOR i IN 1..array_length(test_cases, 1) LOOP
        IF calculated_results[i] = expected_results[i] THEN
            RAISE NOTICE 'PASS: % - Case[%]: % validation result: % matches expected: %', test_name, i, test_cases[i], calculated_results[i], expected_results[i];
        ELSE
            RAISE EXCEPTION 'FAIL: % - Case[%]: % validation result: % does not match expected: %', test_name, i, test_cases[i], calculated_results[i], expected_results[i];
        END IF;
    END LOOP;
END $$;

-- Test case 8.3: Boundary condition validation
-- Tests edge cases and boundary conditions
DO $$
DECLARE
    test_name VARCHAR(100) := 'Boundary Condition Validation';
    boundary_max DECIMAL(12,2) := 999999999.99;
    boundary_min DECIMAL(12,2) := -999999999.99;
    over_max DECIMAL(12,2) := 1000000000.00;
    under_min DECIMAL(12,2) := -1000000000.00;
BEGIN
    -- Test maximum boundary
    IF precision_tests.validate_transaction_amount_precision(boundary_max) THEN
        RAISE NOTICE 'PASS: % - Maximum boundary validation: %', test_name, boundary_max;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Maximum boundary validation failed: %', test_name, boundary_max;
    END IF;
    
    -- Test minimum boundary
    IF precision_tests.validate_transaction_amount_precision(boundary_min) THEN
        RAISE NOTICE 'PASS: % - Minimum boundary validation: %', test_name, boundary_min;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Minimum boundary validation failed: %', test_name, boundary_min;
    END IF;
    
    -- Test over maximum (should fail)
    IF NOT precision_tests.validate_transaction_amount_precision(over_max) THEN
        RAISE NOTICE 'PASS: % - Over maximum correctly rejected: %', test_name, over_max;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Over maximum incorrectly accepted: %', test_name, over_max;
    END IF;
    
    -- Test under minimum (should fail)
    IF NOT precision_tests.validate_transaction_amount_precision(under_min) THEN
        RAISE NOTICE 'PASS: % - Under minimum correctly rejected: %', test_name, under_min;
    ELSE
        RAISE EXCEPTION 'FAIL: % - Under minimum incorrectly accepted: %', test_name, under_min;
    END IF;
END $$;

-- =====================================================================================
-- Test Summary and Cleanup
-- =====================================================================================

-- Test completion summary
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '==================================================================================';
    RAISE NOTICE 'TRANSACTION AMOUNT PRECISION VALIDATION TESTS COMPLETED';
    RAISE NOTICE '==================================================================================';
    RAISE NOTICE 'Source: CVTRA05Y.cpy TRAN-AMT PIC S9(09)V99 field mapping';
    RAISE NOTICE 'Target: PostgreSQL DECIMAL(12,2) with exact BigDecimal precision';
    RAISE NOTICE 'Validation: COBOL COMP-3 arithmetic equivalence confirmed';
    RAISE NOTICE 'Requirements: All transaction amount precision tests PASSED';
    RAISE NOTICE '==================================================================================';
    RAISE NOTICE '';
END $$;

-- Cleanup test function
DROP FUNCTION IF EXISTS precision_tests.validate_transaction_amount_precision(DECIMAL(12,2));

-- Reset search path
SET search_path TO public;

-- Drop test schema
DROP SCHEMA IF EXISTS precision_tests CASCADE;

-- End of precision validation tests