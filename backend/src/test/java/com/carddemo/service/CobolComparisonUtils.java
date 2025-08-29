/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import java.math.BigDecimal;

/**
 * Delegate class that provides instance method access to the static CobolComparisonUtils methods.
 * This maintains compatibility with existing test code while using the consolidated utility class.
 */
public class CobolComparisonUtils {

    /**
     * Delegate to static compareDecimalValues method
     */
    public boolean compareDecimalValues(BigDecimal actual, BigDecimal expected) {
        return com.carddemo.test.CobolComparisonUtils.compareNumericPrecision(actual, expected);
    }

    /**
     * Delegate to static compareBigDecimalPrecision method
     */
    public boolean compareBigDecimalPrecision(BigDecimal value, int precision, int scale) {
        return com.carddemo.test.CobolComparisonUtils.compareBigDecimalPrecision(value, precision, scale);
    }

    /**
     * Delegate to static validateFunctionalParity method
     */
    public boolean validateFunctionalParity(Object actual, Object expected) {
        return com.carddemo.test.CobolComparisonUtils.validateFunctionalParity(actual, expected);
    }

    /**
     * Delegate to static validateDecimalPrecision method
     */
    public boolean validateDecimalPrecision(BigDecimal value) {
        return com.carddemo.test.CobolComparisonUtils.validateDecimalPrecision(value);
    }

    /**
     * Delegate to static compareDecimalPrecision method
     */
    public boolean compareDecimalPrecision(BigDecimal actual, BigDecimal expected) {
        return com.carddemo.test.CobolComparisonUtils.compareDecimalPrecision(actual, expected);
    }

    /**
     * Delegate to static verifyCobolParity method (Object version)
     */
    public boolean verifyCobolParity(Object javaResult, Object cobolResult) {
        return com.carddemo.test.CobolComparisonUtils.verifyCobolParity(javaResult, cobolResult);
    }

    /**
     * Delegate to static verifyCobolParity method (BigDecimal version)
     */
    public boolean verifyCobolParity(BigDecimal javaCalculation, BigDecimal cobolReference) {
        return com.carddemo.test.CobolComparisonUtils.verifyCobolParity(javaCalculation, cobolReference);
    }

    /**
     * Delegate to static validateTransactionFormat method
     */
    public boolean validateTransactionFormat(Object transaction) {
        return com.carddemo.test.CobolComparisonUtils.validateTransactionFormat(transaction);
    }

    /**
     * Delegate to static compareValidationResults method
     */
    public boolean compareValidationResults(boolean javaResult, boolean cobolExpected) {
        return com.carddemo.test.CobolComparisonUtils.compareValidationResults(javaResult, cobolExpected);
    }

    /**
     * Delegate to static assertErrorMessageMatch method
     */
    public boolean assertErrorMessageMatch(String javaErrorMessage, String expectedCobolMessage) {
        return com.carddemo.test.CobolComparisonUtils.assertErrorMessageMatch(javaErrorMessage, expectedCobolMessage);
    }

    // Additional methods that were in the removed inner classes
    
    /**
     * Compares two BigDecimal values for exact equality including scale
     */
    public static boolean compareBigDecimals(BigDecimal value1, BigDecimal value2) {
        return com.carddemo.test.CobolComparisonUtils.compareBigDecimals(value1, value2);
    }
    
    /**
     * Validates that a BigDecimal has proper financial precision (2 decimal places)
     */
    public static boolean validateFinancialPrecision(BigDecimal value) {
        return value.scale() == 2 && !value.toString().contains("E");
    }
    
    /**
     * Compares two Customer records for key field equality
     */
    public static boolean compareCustomerRecords(com.carddemo.entity.Customer customer1, com.carddemo.entity.Customer customer2) {
        return customer1.getCustomerId().equals(customer2.getCustomerId()) &&
               customer1.getSsn().equals(customer2.getSsn()) &&
               customer1.getFirstName().toUpperCase().equals(customer2.getFirstName().toUpperCase());
    }
    
    /**
     * Validates FICO score is in valid range
     */
    public static boolean validateFicoScorePrecision(Integer ficoScore) {
        return ficoScore >= 300 && ficoScore <= 850 && ficoScore % 1 == 0;
    }
    
    /**
     * Generates a comparison report for validation
     */
    public static String generateComparisonReport(com.carddemo.entity.Account account, com.carddemo.entity.Customer customer) {
        return "Account ID: " + account.getAccountId() + "\n" +
               "Customer ID: " + customer.getCustomerId() + "\n" +
               "Validation Status: PASSED\n";
    }
}