/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

/**
 * Constants for test configuration and validation, providing shared values
 * for performance thresholds, data validation, and test setup parameters.
 * Ensures COBOL compatibility and provides comprehensive test data constants.
 */
public final class TestConstants {
    
    // Performance testing constants
    public static final long EXPECTED_MAX_RESPONSE_TIME_MS = 200L;
    public static final long RESPONSE_TIME_THRESHOLD_MS = 200L;
    public static final int CONCURRENT_USERS = 10;
    public static final int THREAD_POOL_SIZE = 20;
    public static final long TIMEOUT_SECONDS = 30L;
    public static final int MAX_SESSION_SIZE_KB = 32; // CICS COMMAREA equivalent (32KB)
    
    // COBOL compatibility constants - matching COMP-3 precision
    public static final int COBOL_DECIMAL_SCALE = 2;
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final int COBOL_MAX_PRECISION = 11; // Max from S9(9)V99 pattern
    
    // Data validation constants
    public static final int DECIMAL_SCALE = 2;
    public static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("999999.99");
    public static final BigDecimal MIN_BALANCE = new BigDecimal("-999999.99");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("999999.99");
    
    // Test data ID constants - compatible with both String and Long usage
    public static final String TEST_CUSTOMER_ID = "100000001";
    public static final Long TEST_CUSTOMER_ID_LONG = 100000001L;
    public static final Long VALID_CUSTOMER_ID_LONG = TEST_CUSTOMER_ID_LONG; // Alias for validation tests
    public static final String VALID_CUSTOMER_ID = TEST_CUSTOMER_ID; // Alias for service tests
    public static final Long TEST_ACCOUNT_ID = 40000000001L; // Fixed to 11 digits to match COBOL PIC X(11)
    public static final String TEST_ACCOUNT_ID_STRING = "40000000001";
    public static final String TEST_TRANSACTION_ID = "TXN0000001";
    public static final String TEST_CARD_NUMBER = "4000000000000001";
    
    // Test data prefixes and patterns
    public static final String TEST_CUSTOMER_ID_PREFIX = "TST";
    public static final String TEST_ACCOUNT_ID_PREFIX = "ACC";
    public static final int TEST_SSN_LENGTH = 9;
    public static final int TEST_PHONE_LENGTH = 10;
    public static final int ACCOUNT_ID_LENGTH = 11;
    public static final int CUSTOMER_ID_LENGTH = 9;
    
    // COBOL field length constants matching PIC clauses
    public static final int FIRST_NAME_LENGTH = 25;
    public static final int LAST_NAME_LENGTH = 25;
    public static final int ADDRESS_LINE_LENGTH = 50;
    public static final int CITY_LENGTH = 25;
    public static final int STATE_LENGTH = 2;
    public static final int ZIP_CODE_LENGTH = 10;
    
    // Transaction type constants
    public static final String TRANSACTION_TYPE_SALE = "01";
    public static final String TRANSACTION_TYPE_CREDIT = "02";
    public static final String TRANSACTION_TYPE_PAYMENT = "03";
    public static final String TRANSACTION_TYPE_CASH_ADVANCE = "04";
    public static final String TEST_TRANSACTION_TYPE_CODE = "01";
    public static final String TEST_TRANSACTION_TYPE_DESC = "Purchase";
    public static final String TEST_TRANSACTION_CATEGORY_CODE = "5411";
    
    // Account status constants
    public static final String ACCOUNT_STATUS_ACTIVE = "A";
    public static final String ACCOUNT_STATUS_CLOSED = "C";
    public static final String ACCOUNT_STATUS_SUSPENDED = "S";
    
    // Security test constants
    public static final String TEST_USER_ID = "testuser";
    public static final String TEST_USER_PASSWORD = "password";
    public static final String TEST_ADMIN_ROLE = "ADMIN";
    public static final String TEST_USER_ROLE = "USER";
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    
    // FICO score validation constants (COBOL business rule: 300-850 range)
    public static final int FICO_SCORE_MIN = 300;
    public static final int FICO_SCORE_MAX = 850;
    
    // Data format validation patterns
    public static final String SSN_PATTERN = "^\\d{9}$";
    public static final String PHONE_NUMBER_PATTERN = "^\\d{3}-\\d{3}-\\d{4}$";
    
    // Batch processing constants
    public static final int BATCH_CHUNK_SIZE = 1000;
    public static final long BATCH_PROCESSING_WINDOW_HOURS = 4L;
    
    // COBOL COMP-3 patterns for data validation
    public static final Map<String, Object> COBOL_COMP3_PATTERNS = new HashMap<String, Object>() {{
        put("BALANCE", "S9(9)V99");
        put("CREDIT_LIMIT", "S9(9)V99");
        put("CASH_LIMIT", "S9(7)V99");
        put("AMOUNT", "S9(7)V99");
        put("INTEREST_RATE", "S9(3)V99");
    }};
    
    // Validation thresholds for business logic testing
    public static final Map<String, Object> VALIDATION_THRESHOLDS = new HashMap<String, Object>() {{
        put("MAX_CREDIT_LIMIT", new BigDecimal("999999.99"));
        put("MIN_BALANCE", new BigDecimal("-999999.99"));
        put("MAX_DAILY_TRANSACTIONS", 100);
        put("MAX_TRANSACTION_AMOUNT", new BigDecimal("50000.00"));
        put("ACCOUNT_NUMBER_LENGTH", 11);
        put("CUSTOMER_ID_LENGTH", 9);
        put("decimal_precision_tolerance", 0.01); // Added missing tolerance for BigDecimal assertions
        put("numeric_overflow_check", true); // Added missing numeric overflow validation flag
    }};
    
    // Functional parity rules for COBOL-to-Java validation
    public static final Map<String, Object> FUNCTIONAL_PARITY_RULES = new HashMap<String, Object>() {{
        put("PRESERVE_DECIMAL_PRECISION", true);
        put("MAINTAIN_FIELD_LENGTHS", true);
        put("ENFORCE_COBOL_ROUNDING", true);
        put("VALIDATE_COMP3_FORMAT", true);
        put("CHECK_SIGN_HANDLING", true);
        put("VERIFY_PADDING_BEHAVIOR", true);
        // Additional keys for CardXrefTest compatibility  
        put("validate_field_lengths", true);
        put("check_overflow_handling", true);
        put("verify_error_messages", true);
        // Additional keys for TransactionTypeTest compatibility
        put("preserve_decimal_precision", true);
    }};
    
    // Performance testing additional constants
    public static final int TARGET_TPS = 1000;
    public static final long CACHE_PERFORMANCE_THRESHOLD_MS = 50L;
    
    // Performance test data settings
    public static final Map<String, Object> PERFORMANCE_TEST_DATA = new HashMap<String, Object>() {{
        put("TEST_ITERATIONS", 1000);
        put("CONCURRENT_THREADS", 10);
        put("WARM_UP_ITERATIONS", 100);
        put("MAX_RESPONSE_TIME_MS", 200L);
        put("MIN_TPS", 100);
        put("MAX_MEMORY_MB", 512);
        put("CONNECTION_POOL_SIZE", 20);
        // Additional keys for MainframeBenchmarkTest compatibility
        put("concurrent_users", 50); // Minimum viable concurrent users for testing
        put("test_duration_minutes", 5L);
    }};
    
    // Date format constants
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String COBOL_DATE_FORMAT = "yyyyMMdd";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    
    // Compliance and regulatory constants
    public static final int GDPR_RETENTION_YEARS = 7;
    
    private TestConstants() {
        // Utility class - no instantiation
    }
}