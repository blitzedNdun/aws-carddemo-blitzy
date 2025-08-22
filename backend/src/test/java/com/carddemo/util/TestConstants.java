/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Test constants for CardDemo application testing.
 * 
 * Provides centralized constants for COBOL precision matching, response time thresholds,
 * and other test configuration values used across the comprehensive test suite.
 * These constants ensure consistency in testing COBOL-to-Java migration requirements.
 * 
 * Key Areas:
 * - COBOL COMP-3 precision matching (scale and rounding)
 * - Performance testing thresholds
 * - Test data configuration
 * - Financial calculation constants
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public final class TestConstants {

    // Private constructor to prevent instantiation
    private TestConstants() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // COBOL COMP-3 Precision Constants
    /**
     * COBOL decimal scale for monetary amounts (2 decimal places for COMP-3 packed decimal).
     * Matches COBOL PIC S9(10)V99 field definitions for precise financial calculations.
     */
    public static final int COBOL_DECIMAL_SCALE = 2;

    /**
     * COBOL rounding mode matching COBOL ROUNDED clause behavior.
     * Uses HALF_UP rounding to replicate COBOL's default rounding behavior for monetary calculations.
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;

    // Performance Testing Thresholds
    /**
     * Response time threshold in milliseconds for transaction processing.
     * Matches requirement for sub-200ms response times for card authorization requests.
     */
    public static final long RESPONSE_TIME_THRESHOLD_MS = 200L;

    /**
     * Batch processing timeout threshold in milliseconds.
     * Based on 4-hour batch processing window requirement (14,400,000ms).
     */
    public static final long BATCH_PROCESSING_TIMEOUT_MS = 14_400_000L; // 4 hours

    /**
     * Maximum allowed memory usage in MB for batch processing tests.
     * Ensures memory efficiency during large volume transaction processing.
     */
    public static final long MAX_MEMORY_USAGE_MB = 512L;

    // Test Data Configuration
    /**
     * Default test transaction amount for standard testing scenarios.
     * Uses COBOL-compatible precision with proper scale.
     */
    public static final BigDecimal DEFAULT_TRANSACTION_AMOUNT = 
        new BigDecimal("150.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

    /**
     * Default test account balance for account validation scenarios.
     * Uses COBOL-compatible precision with proper scale.
     */
    public static final BigDecimal DEFAULT_ACCOUNT_BALANCE = 
        new BigDecimal("2500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

    /**
     * Default test credit limit for account validation scenarios.
     * Uses COBOL-compatible precision with proper scale.
     */
    public static final BigDecimal DEFAULT_CREDIT_LIMIT = 
        new BigDecimal("5000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

    /**
     * Maximum transaction amount for validation testing.
     * Matches COBOL PIC S9(09)V99 maximum value constraint.
     */
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = 
        new BigDecimal("9999999.99").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

    // Financial Calculation Constants
    /**
     * Interest rate divisor for monthly interest calculations.
     * Matches COBOL CBACT04C formula: (balance * rate) / 1200
     */
    public static final int MONTHLY_INTEREST_DIVISOR = 1200;

    /**
     * Annual percentage rate divisor for APR calculations.
     * Standard financial calculation for annual to monthly rate conversion.
     */
    public static final int ANNUAL_PERCENTAGE_DIVISOR = 100;

    /**
     * Daily rate divisor for daily interest calculations.
     * Standard financial calculation for annual to daily rate conversion.
     */
    public static final int DAILY_RATE_DIVISOR = 365;

    // Transaction Type Constants
    /**
     * Purchase transaction type code.
     * Matches COBOL TRAN-TYPE-CD field definition for purchase transactions.
     */
    public static final String TXN_TYPE_PURCHASE = "PUR";

    /**
     * Credit transaction type code.
     * Matches COBOL TRAN-TYPE-CD field definition for credit transactions.
     */
    public static final String TXN_TYPE_CREDIT = "CRE";

    /**
     * Cash advance transaction type code.
     * Matches COBOL TRAN-TYPE-CD field definition for cash advance transactions.
     */
    public static final String TXN_TYPE_CASH_ADVANCE = "CAS";

    /**
     * Payment transaction type code.
     * Matches COBOL TRAN-TYPE-CD field definition for payment transactions.
     */
    public static final String TXN_TYPE_PAYMENT = "PAY";

    // Account Status Constants
    /**
     * Active account status indicator.
     * Matches COBOL ACCT-ACTIVE-STATUS field definition.
     */
    public static final String ACCOUNT_STATUS_ACTIVE = "Y";

    /**
     * Inactive account status indicator.
     * Matches COBOL ACCT-ACTIVE-STATUS field definition.
     */
    public static final String ACCOUNT_STATUS_INACTIVE = "N";

    // Test Transaction IDs
    /**
     * Valid test transaction ID format.
     * Matches COBOL TRAN-ID field length requirement (16 characters).
     */
    public static final String TEST_TRANSACTION_ID = "TXN-001-12345678";

    /**
     * Invalid test transaction ID for negative testing.
     * Used to test transaction ID validation logic.
     */
    public static final String INVALID_TRANSACTION_ID = "INVALID-TXN";

    // Test Account and Merchant IDs
    /**
     * Valid test account ID for testing scenarios.
     * Matches COBOL account ID format requirements.
     */
    public static final String TEST_ACCOUNT_ID = "1000000001";

    /**
     * Valid test merchant ID for merchant validation testing.
     * Matches COBOL merchant ID format requirements.
     */
    public static final String TEST_MERCHANT_ID = "MERCHANT001";

    /**
     * Valid test authorization code for authorization testing.
     * Used in authorization verification test scenarios.
     */
    public static final String TEST_AUTH_CODE = "AUTH001";

    // Batch Processing Constants
    /**
     * Default batch size for testing batch transaction processing.
     * Represents typical volume for testing scenarios.
     */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * Large batch size for volume testing scenarios.
     * Used to test processing under high volume conditions.
     */
    public static final int LARGE_BATCH_SIZE = 10000;

    /**
     * Maximum allowed batch processing time in minutes.
     * Based on 4-hour processing window requirement.
     */
    public static final int MAX_BATCH_PROCESSING_MINUTES = 240; // 4 hours

    // Error Handling Constants
    /**
     * Standard error code for insufficient funds scenarios.
     * Matches COBOL error handling patterns.
     */
    public static final String ERROR_INSUFFICIENT_FUNDS = "ERR001";

    /**
     * Standard error code for invalid account scenarios.
     * Matches COBOL error handling patterns.
     */
    public static final String ERROR_INVALID_ACCOUNT = "ERR002";

    /**
     * Standard error code for duplicate transaction scenarios.
     * Matches COBOL error handling patterns.
     */
    public static final String ERROR_DUPLICATE_TRANSACTION = "ERR003";

    /**
     * Standard error code for invalid merchant scenarios.
     * Matches COBOL error handling patterns.
     */
    public static final String ERROR_INVALID_MERCHANT = "ERR004";

    // Validation Tolerances
    /**
     * Monetary calculation tolerance for BigDecimal comparisons.
     * Accounts for minimal rounding differences in financial calculations.
     */
    public static final BigDecimal MONETARY_TOLERANCE = 
        new BigDecimal("0.01").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

    /**
     * Percentage tolerance for rate calculations.
     * Used in interest rate and percentage-based validations.
     */
    public static final BigDecimal PERCENTAGE_TOLERANCE = 
        new BigDecimal("0.001").setScale(3, COBOL_ROUNDING_MODE);

    // COBOL Field Length Constants
    /**
     * Maximum transaction ID length from COBOL copybook.
     * Matches TRAN-ID field definition (PIC X(16)).
     */
    public static final int TRANSACTION_ID_MAX_LENGTH = 16;

    /**
     * Maximum transaction description length from COBOL copybook.
     * Matches TRAN-DESC field definition (PIC X(100)).
     */
    public static final int TRANSACTION_DESC_MAX_LENGTH = 100;

    /**
     * Maximum merchant name length from COBOL copybook.
     * Matches TRAN-MERCHANT-NAME field definition (PIC X(50)).
     */
    public static final int MERCHANT_NAME_MAX_LENGTH = 50;

    /**
     * Account ID length from COBOL copybook.
     * Matches ACCT-ID field definition (PIC 9(11)).
     */
    public static final int ACCOUNT_ID_LENGTH = 11;

    // Additional Test Data Constants for AbstractBaseTest compatibility
    /**
     * Test customer ID for testing scenarios.
     * Standard customer ID used across test cases.
     */
    public static final String TEST_CUSTOMER_ID = "1000000001";

    /**
     * Test user ID for authentication testing.
     * Standard user ID used in authentication test scenarios.
     */
    public static final String TEST_USER_ID = "TESTUSER";

    /**
     * Test user password for authentication testing.
     * Standard password used in authentication test scenarios.
     */
    public static final String TEST_USER_PASSWORD = "password123";

    /**
     * Test user role for role-based access control testing.
     * Standard user role used in authorization test scenarios.
     */
    public static final String TEST_USER_ROLE = "USER";

    /**
     * Test admin role for administrative function testing.
     * Standard admin role used in elevated permission test scenarios.
     */
    public static final String TEST_ADMIN_ROLE = "ADMIN";

    /**
     * Test card number for card-related testing.
     * Standard card number used across card test scenarios.
     */
    public static final String TEST_CARD_NUMBER = "1234567890123456";

    /**
     * Test transaction type code for transaction testing.
     * Standard transaction type code used in transaction test scenarios.
     */
    public static final String TEST_TRANSACTION_TYPE_CODE = "PUR";

    /**
     * Test transaction type description for transaction testing.
     * Standard description used in transaction type test scenarios.
     */
    public static final String TEST_TRANSACTION_TYPE_DESC = "Purchase";

    /**
     * Test transaction category code for categorization testing.
     * Standard category code used in transaction category test scenarios.
     */
    public static final String TEST_TRANSACTION_CATEGORY_CODE = "RETAIL";

    // Validation Thresholds Map
    /**
     * Validation thresholds for various test scenarios.
     * Contains decimal precision tolerance and other validation thresholds.
     */
    public static final java.util.Map<String, Object> VALIDATION_THRESHOLDS = 
        java.util.Map.of(
            "decimal_precision_tolerance", 0.01,
            "response_time_ms", RESPONSE_TIME_THRESHOLD_MS,
            "success_rate_threshold", 0.95,
            "availability_threshold", 0.999
        );

    // COBOL COMP-3 Patterns Map
    /**
     * COBOL COMP-3 patterns and configuration for precision testing.
     * Contains maximum precision, scale, and pattern definitions.
     */
    public static final java.util.Map<String, Object> COBOL_COMP3_PATTERNS = 
        java.util.Map.of(
            "max_precision", 18,
            "default_scale", COBOL_DECIMAL_SCALE,
            "rounding_mode", COBOL_ROUNDING_MODE.toString(),
            "numeric_pattern", "^[0-9]{1,16}$",
            "decimal_pattern", "^[0-9]{1,14}\\.[0-9]{2}$"
        );
}