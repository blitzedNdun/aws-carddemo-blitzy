/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import java.math.RoundingMode;
import java.time.Duration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Map;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.Constants;
import com.carddemo.entity.UserSecurity;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;

/**
 * Constants class defining shared test values, configuration parameters, and test data patterns 
 * used across all test types in the CardDemo application.
 * 
 * This class provides comprehensive testing constants for:
 * - Performance validation (response times, TPS targets)
 * - COBOL data type compatibility testing (COMP-3 patterns, BigDecimal precision)
 * - User authentication and authorization testing
 * - Batch processing window validation
 * - Functional parity testing between COBOL and Java implementations
 * 
 * All constants are designed to ensure 100% functional parity with the original COBOL
 * implementation while supporting modern testing frameworks and methodologies.
 * 
 * Key Features:
 * - Performance thresholds matching mainframe SLA requirements (sub-200ms response times)
 * - COBOL COMP-3 packed decimal precision preservation for financial calculations
 * - Test user credentials supporting role-based access control testing
 * - Batch processing validation constants for 4-hour processing windows
 * - Comprehensive validation patterns for data type conversion testing
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
public final class TestConstants {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TestConstants() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    // Performance Testing Constants

    /**
     * Response time threshold in milliseconds for performance validation.
     * All REST API endpoints must respond within this threshold to maintain
     * functional parity with mainframe CICS transaction response times.
     * Based on Section 0.2.1 requirement for sub-200ms response times.
     */
    public static final long RESPONSE_TIME_THRESHOLD_MS = 200L;

    /**
     * Target transactions per second for load testing validation.
     * System must support this TPS rate to meet mainframe performance parity.
     * Based on Section 0.2.1 requirement for 10,000 TPS capacity.
     */
    public static final int TARGET_TPS = 10000;

    // COBOL Data Type Compatibility Constants

    /**
     * Decimal scale for COBOL COMP-3 monetary field compatibility.
     * Ensures BigDecimal operations maintain identical precision to COBOL
     * PIC S9(10)V99 packed decimal fields for financial calculations.
     * Uses CobolDataConverter.MONETARY_SCALE for consistency.
     */
    public static final int COBOL_DECIMAL_SCALE = CobolDataConverter.MONETARY_SCALE;

    /**
     * Rounding mode matching COBOL ROUNDED clause behavior.
     * Ensures identical rounding results between COBOL and Java implementations
     * for all financial calculations and monetary field operations.
     * Uses CobolDataConverter.COBOL_ROUNDING_MODE for consistency.
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = CobolDataConverter.COBOL_ROUNDING_MODE;

    // Test User Authentication Constants

    /**
     * Test user ID for authentication testing.
     * Matches COBOL SEC-USR-ID field length from UserSecurity entity (8 characters).
     * Used for standard user authentication scenarios in integration tests.
     */
    public static final String TEST_USER_ID = "TESTUSER";

    /**
     * Test user password for authentication testing.
     * Used for standard user authentication scenarios in integration tests.
     * Should be BCrypt encoded in actual test data setup.
     */
    public static final String TEST_USER_PASSWORD = "testpass123";

    /**
     * Admin role authority for role-based access testing.
     * Matches Spring Security authority pattern for administrative users.
     * Used with UserSecurity.getAuthorities() for authorization testing.
     */
    public static final SimpleGrantedAuthority TEST_ADMIN_ROLE = new SimpleGrantedAuthority("ROLE_ADMIN");

    /**
     * Regular user role authority for role-based access testing.
     * Matches Spring Security authority pattern for regular users.
     * Used with UserSecurity.getAuthorities() for authorization testing.
     */
    public static final SimpleGrantedAuthority TEST_USER_ROLE = new SimpleGrantedAuthority("ROLE_USER");

    // Session and Timeout Constants

    /**
     * Session timeout in minutes for testing session management.
     * Ensures session timeout testing matches CICS session behavior.
     * Used with Duration.ofMinutes() for timeout configuration.
     */
    public static final long SESSION_TIMEOUT_MINUTES = 30L;

    /**
     * Batch processing window in hours for batch job validation.
     * Ensures batch jobs complete within the required 4-hour processing window
     * as specified in Section 0.5.1 requirements.
     */
    public static final long BATCH_PROCESSING_WINDOW_HOURS = 4L;

    /**
     * Maximum session size in kilobytes for COMMAREA compatibility testing.
     * Matches CICS COMMAREA size limit to ensure session state management
     * maintains identical capacity constraints as mainframe implementation.
     */
    public static final int MAX_SESSION_SIZE_KB = 32;

    // Data Format Constants

    /**
     * JSON date format pattern for REST API testing.
     * Ensures consistent date formatting across all REST endpoints
     * for integration testing and API contract validation.
     */
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd";

    // Test Data Identifiers (using Constants for field length validation)

    /**
     * Test account ID for account-related testing scenarios.
     * Length matches Constants.ACCOUNT_ID_LENGTH (11 characters) from COBOL specification.
     * Used with Account.getAccountId() for account entity testing.
     */
    public static final String TEST_ACCOUNT_ID = "00000000001";

    /**
     * Test transaction ID for transaction-related testing scenarios.
     * Length matches Constants.TRANSACTION_ID_LENGTH (16 characters) from COBOL specification.
     * Used with Transaction.getTransactionId() for transaction entity testing.
     */
    public static final String TEST_TRANSACTION_ID = "TXN0000000000001";

    /**
     * Test customer ID for customer-related testing scenarios.
     * Length derived from Constants field mappings for customer identification.
     * Used for customer entity and relationship testing.
     */
    public static final String TEST_CUSTOMER_ID = "CUST000001";

    /**
     * Test card number for card-related testing scenarios.
     * Length matches Constants.CARD_NUMBER_LENGTH (16 characters) from COBOL specification.
     * Used for card entity and transaction testing.
     */
    public static final String TEST_CARD_NUMBER = "1234567890123456";

    // Validation Threshold Collections

    /**
     * Validation thresholds for functional parity testing.
     * Contains threshold values for various validation scenarios to ensure
     * COBOL-to-Java conversion maintains identical behavior patterns.
     * Used with Map.of(), get(), containsKey() for threshold validation.
     */
    public static final Map<String, Object> VALIDATION_THRESHOLDS = Map.of(
        "decimal_precision_tolerance", 0.001,
        "string_length_variance", 0,
        "date_format_strict", true,
        "numeric_overflow_check", true,
        "response_time_ms", RESPONSE_TIME_THRESHOLD_MS,
        "batch_completion_hours", BATCH_PROCESSING_WINDOW_HOURS
    );

    /**
     * COBOL COMP-3 test patterns for data conversion validation.
     * Contains test patterns and expected results for COMP-3 packed decimal
     * conversion testing using CobolDataConverter.fromComp3() and preservePrecision().
     */
    public static final Map<String, Object> COBOL_COMP3_PATTERNS = Map.of(
        "monetary_scale", COBOL_DECIMAL_SCALE,
        "rounding_mode", COBOL_ROUNDING_MODE.toString(),
        "positive_pattern", "123.45",
        "negative_pattern", "-123.45",
        "zero_pattern", "0.00",
        "max_precision", CobolDataConverter.MAX_PRECISION
    );

    /**
     * Performance test data patterns for load and stress testing.
     * Contains configuration data for performance testing scenarios
     * including TPS targets, response time thresholds, and concurrency levels.
     */
    public static final Map<String, Object> PERFORMANCE_TEST_DATA = Map.of(
        "target_tps", TARGET_TPS,
        "response_threshold_ms", RESPONSE_TIME_THRESHOLD_MS,
        "concurrent_users", 100,
        "test_duration_minutes", Duration.ofMinutes(10).toMinutes(),
        "warmup_duration_minutes", Duration.ofMinutes(2).toMinutes(),
        "session_timeout_minutes", SESSION_TIMEOUT_MINUTES
    );

    /**
     * Functional parity validation rules for COBOL-Java comparison testing.
     * Contains rules and patterns for validating identical behavior between
     * COBOL and Java implementations across all business logic scenarios.
     */
    public static final Map<String, Object> FUNCTIONAL_PARITY_RULES = Map.of(
        "preserve_decimal_precision", true,
        "match_cobol_rounding", true,
        "validate_field_lengths", true,
        "check_overflow_handling", true,
        "verify_error_messages", true,
        "compare_calculation_results", true
    );

    // Individual Export Constants (as specified in schema)

    /**
     * Retry limit for batch job processing and error recovery testing.
     * Ensures batch jobs retry failed operations appropriately without
     * infinite loops or excessive resource consumption.
     */
    public static final int RETRY_LIMIT = 3;

    /**
     * Skip limit for batch processing error tolerance testing.
     * Allows batch jobs to skip a limited number of failed records
     * while continuing processing to maintain throughput requirements.
     */
    public static final int SKIP_LIMIT = 100;

    /**
     * Checkpoint interval for batch job progress tracking and restart capability.
     * Ensures batch jobs can restart from known checkpoint positions
     * maintaining data consistency and processing efficiency.
     */
    public static final int CHECKPOINT_INTERVAL = 1000;

    /**
     * Recovery timeout in milliseconds for batch job failure recovery testing.
     * Ensures batch jobs attempt recovery within reasonable time limits
     * without causing system resource exhaustion.
     */
    public static final long RECOVERY_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    /**
     * Social Security Number validation pattern for PII data testing.
     * Ensures SSN fields maintain proper format validation matching
     * COBOL field validation patterns and compliance requirements.
     */
    public static final String SSN_PATTERN = "\\d{3}-\\d{2}-\\d{4}";

    /**
     * Phone number validation pattern for contact data testing.
     * Ensures phone number fields maintain proper format validation
     * matching COBOL field validation patterns.
     */
    public static final String PHONE_NUMBER_PATTERN = "\\(\\d{3}\\) \\d{3}-\\d{4}";

    /**
     * Minimum FICO score for credit scoring validation testing.
     * Ensures credit scoring logic maintains proper range validation
     * matching business rules from COBOL implementation.
     */
    public static final int FICO_SCORE_MIN = 300;

    /**
     * Maximum FICO score for credit scoring validation testing.
     * Ensures credit scoring logic maintains proper range validation
     * matching business rules from COBOL implementation.
     */
    public static final int FICO_SCORE_MAX = 850;

    /**
     * GDPR data retention period in years for compliance testing.
     * Ensures data retention policies match regulatory requirements
     * and are properly implemented in data lifecycle management.
     */
    public static final int GDPR_RETENTION_YEARS = 7;

    /**
     * Test transaction type code for transaction processing testing.
     * Matches Constants.TYPE_CODE_LENGTH (2 characters) from COBOL specification.
     * Used with Transaction.getTransactionType() for transaction type validation.
     */
    public static final String TEST_TRANSACTION_TYPE_CODE = "PU";

    /**
     * Test transaction type description for transaction processing testing.
     * Used for transaction type validation and display testing scenarios.
     */
    public static final String TEST_TRANSACTION_TYPE_DESC = "Purchase";

    /**
     * Cache performance threshold in milliseconds for caching validation.
     * Ensures caching mechanisms provide appropriate performance improvement
     * over direct database access patterns.
     */
    public static final long CACHE_PERFORMANCE_THRESHOLD_MS = Duration.ofMillis(50).toMillis();

    /**
     * Test transaction category code for transaction classification testing.
     * Matches Constants.CATEGORY_CODE_LENGTH (4 characters) from COBOL specification.
     * Used for transaction category validation and processing testing.
     */
    public static final String TEST_TRANSACTION_CATEGORY_CODE = "5411";
}