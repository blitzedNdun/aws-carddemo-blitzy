package com.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Provides shared test constants including COBOL decimal precision settings, response time thresholds (200ms),
 * performance targets (10,000 TPS), session parameters, and validation thresholds for functional parity testing.
 */
public final class TestConstants {

    // Prevent instantiation
    private TestConstants() {
        throw new AssertionError("TestConstants should not be instantiated");
    }

    // ===== PERFORMANCE THRESHOLDS =====
    
    /**
     * Maximum allowed response time for REST API calls (200ms requirement)
     */
    public static final long RESPONSE_TIME_THRESHOLD_MS = 200L;
    
    /**
     * Target throughput for performance testing (10,000 TPS requirement)
     */
    public static final int THROUGHPUT_TPS_TARGET = 10000;
    public static final int TARGET_TPS = 10000; // Alias for PerformanceValidationTest compatibility
    
    /**
     * Maximum allowed batch processing time (4 hours requirement)
     */
    public static final Duration BATCH_PROCESSING_MAX_TIME = Duration.ofHours(4);

    // ===== COBOL PRECISION SETTINGS =====
    
    /**
     * COBOL COMP-3 decimal scale for currency amounts (2 decimal places)
     */
    public static final int COBOL_DECIMAL_SCALE = 2;
    
    /**
     * COBOL COMP-3 decimal scale for interest rates (4 decimal places)
     */
    public static final int COBOL_RATE_SCALE = 4;
    
    /**
     * COBOL ROUNDED clause equivalent rounding mode
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Precision tolerance for COBOL-Java comparison
     */
    public static final BigDecimal COBOL_PRECISION_TOLERANCE = new BigDecimal("0.001");

    // ===== VALIDATION THRESHOLDS =====
    
    /**
     * Minimum test coverage threshold
     */
    public static final double MIN_TEST_COVERAGE = 0.90; // 90%
    
    /**
     * Maximum allowed memory usage during testing
     */
    public static final double MAX_MEMORY_USAGE = 0.80; // 80%
    
    /**
     * Maximum allowed CPU usage during testing
     */
    public static final double MAX_CPU_USAGE = 0.75; // 75%

    // ===== FUNCTIONAL PARITY RULES =====
    
    /**
     * Rules for validating functional parity between COBOL and Java
     */
    public static final class FUNCTIONAL_PARITY_RULES {
        /**
         * Transaction amounts must match to the penny
         */
        public static final boolean EXACT_AMOUNT_MATCH_REQUIRED = true;
        
        /**
         * Interest calculations must match to 4 decimal places
         */
        public static final boolean EXACT_INTEREST_MATCH_REQUIRED = true;
        
        /**
         * Balance updates must be identical
         */
        public static final boolean EXACT_BALANCE_MATCH_REQUIRED = true;
        
        /**
         * Date formats must be equivalent
         */
        public static final boolean DATE_FORMAT_VALIDATION_REQUIRED = true;
    }

    // ===== TRANSACTION LIMITS =====
    
    /**
     * Minimum transaction amount
     */
    public static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    
    /**
     * Maximum transaction amount
     */
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("10000.00");
    
    /**
     * Default transaction amount for testing
     */
    public static final BigDecimal DEFAULT_TRANSACTION_AMOUNT = new BigDecimal("100.00");

    // ===== ACCOUNT LIMITS =====
    
    /**
     * Minimum account balance
     */
    public static final BigDecimal MIN_ACCOUNT_BALANCE = new BigDecimal("0.00");
    
    /**
     * Default credit limit
     */
    public static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("2000.00");
    
    /**
     * Maximum credit limit
     */
    public static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("50000.00");

    // ===== SESSION PARAMETERS =====
    
    /**
     * Default session timeout in minutes
     */
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * Maximum concurrent sessions for testing
     */
    public static final int MAX_CONCURRENT_SESSIONS = 1000;
    
    /**
     * Session validation interval in seconds
     */
    public static final int SESSION_VALIDATION_INTERVAL_SECONDS = 60;

    // ===== TEST DATA PATTERNS =====
    
    /**
     * Valid account number patterns (COBOL equivalent)
     */
    public static final String[] VALID_ACCOUNT_PATTERNS = {
        "1000######", // Checking accounts
        "2000######", // Savings accounts  
        "3000######", // Credit card accounts
        "4000######", // Loan accounts
        "5000######"  // Special accounts
    };
    
    /**
     * Valid card number patterns (COBOL equivalent)
     */
    public static final String[] VALID_CARD_PATTERNS = {
        "4532########", // Visa test cards
        "5555########", // MasterCard test cards
        "4000########", // Visa test cards
        "3782########"  // American Express test cards
    };

    // ===== TEST DATA CONSTANTS =====
    
    /**
     * Test account ID for unit testing
     */
    public static final Long TEST_ACCOUNT_ID = 1000000001L;
    
    /**
     * Test customer ID for unit testing
     */
    public static final String TEST_CUSTOMER_ID = "TESTCUST01";
    
    /**
     * Test card number for unit testing
     */
    public static final String TEST_CARD_NUMBER = "4532123456789012";
    
    /**
     * Monetary tolerance for financial calculations (alias for COBOL_PRECISION_TOLERANCE)
     */
    public static final BigDecimal MONETARY_TOLERANCE = COBOL_PRECISION_TOLERANCE;

    // ===== TRANSACTION TYPE CODES =====
    
    /**
     * Purchase transaction type code
     */
    public static final String TXN_TYPE_PURCHASE = "PUR";
    
    /**
     * Credit transaction type code
     */
    public static final String TXN_TYPE_CREDIT = "CRD";
    
    /**
     * Debit transaction type code
     */
    public static final String TXN_TYPE_DEBIT = "DEB";
    
    /**
     * Payment transaction type code
     */
    public static final String TXN_TYPE_PAYMENT = "PAY";
    
    /**
     * Refund transaction type code
     */
    public static final String TXN_TYPE_REFUND = "REF";

    // ===== ERROR CODES =====
    
    /**
     * Success response code
     */
    public static final String SUCCESS_CODE = "00";
    
    /**
     * Invalid transaction error code
     */
    public static final String INVALID_TRANSACTION_CODE = "01";
    
    /**
     * Insufficient funds error code
     */
    public static final String INSUFFICIENT_FUNDS_CODE = "02";
    
    /**
     * Invalid account error code
     */
    public static final String INVALID_ACCOUNT_CODE = "03";
    
    /**
     * System error code
     */
    public static final String SYSTEM_ERROR_CODE = "99";

    // ===== DATE/TIME PATTERNS =====
    
    /**
     * COBOL date format pattern
     */
    public static final String COBOL_DATE_PATTERN = "yyyy-MM-dd";
    
    /**
     * COBOL timestamp format pattern
     */
    public static final String COBOL_TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * COBOL time format pattern
     */
    public static final String COBOL_TIME_PATTERN = "HH:mm:ss";

    // ===== UTILITY CONSTANTS =====
    
    /**
     * Default character set for COBOL data conversion
     */
    public static final String COBOL_CHARSET = "UTF-8";
    
    /**
     * COBOL record separator
     */
    public static final String COBOL_RECORD_SEPARATOR = "\n";
    
    /**
     * COBOL field padding character
     */
    public static final char COBOL_PADDING_CHAR = ' ';

    // ===== TEST ENVIRONMENT SETTINGS =====
    
    /**
     * Test database schema name
     */
    public static final String TEST_SCHEMA_NAME = "carddemo_test";
    
    /**
     * Test Redis database index
     */
    public static final int TEST_REDIS_DB_INDEX = 1;
    
    /**
     * Test server port
     */
    public static final int TEST_SERVER_PORT = 8080;

    // ===== VALIDATION METHODS =====
    
    /**
     * Validates that a response time meets the threshold
     */
    public static boolean isWithinResponseTimeThreshold(long responseTimeMs) {
        return responseTimeMs <= RESPONSE_TIME_THRESHOLD_MS;
    }
    
    /**
     * Validates that a BigDecimal has COBOL-compatible precision
     */
    public static boolean hasCobolPrecision(BigDecimal value) {
        return value != null && value.scale() <= COBOL_DECIMAL_SCALE;
    }
    
    /**
     * Validates that a throughput meets the target
     */
    public static boolean meetsThroughputTarget(int actualTps) {
        return actualTps >= THROUGHPUT_TPS_TARGET;
    }
    
    /**
     * Creates a BigDecimal with COBOL precision
     */
    public static BigDecimal createCobolDecimal(String value) {
        return new BigDecimal(value).setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
    }
    
    /**
     * Creates a BigDecimal with specific COBOL scale
     */
    public static BigDecimal createCobolDecimal(String value, int scale) {
        return new BigDecimal(value).setScale(scale, COBOL_ROUNDING_MODE);
    }

    // ===== ADDITIONAL TEST CONSTANTS =====
    
    /**
     * Additional test constants for various test scenarios
     */
    public static final String TEST_DESCRIPTION = "Test Description";
    public static final String TEST_MERCHANT_NAME = "Test Merchant";
    public static final String TEST_MERCHANT_CITY = "Test City";
    public static final String TEST_MERCHANT_ZIP = "12345";
    
    // Account and user constants
    public static final String ACCOUNT_STATUS_ACTIVE = "Y";
    public static final BigDecimal DEFAULT_ACCOUNT_BALANCE = new BigDecimal("1000.00");
    public static final String TEST_USER_ID = "TESTUSER";
    public static final String TEST_USER_PASSWORD = "password123";
    public static final BigDecimal TEST_TRANSACTION_AMOUNT = new BigDecimal("100.00");
    
    // Field length constants
    public static final int TRANSACTION_ID_MAX_LENGTH = 16;
    public static final int TRANSACTION_DESC_MAX_LENGTH = 26;
    public static final int MERCHANT_NAME_MAX_LENGTH = 25;
    
    // Batch processing constants
    public static final int DEFAULT_BATCH_SIZE = 100;
    public static final long BATCH_PROCESSING_TIMEOUT_MS = 300000L; // 5 minutes
    public static final long MAX_MEMORY_USAGE_MB = 512L;
    public static final int BATCH_PROCESSING_WINDOW_HOURS = 4; // 4-hour batch processing window requirement
    
    // Additional validation constants
    public static final String INVALID_CARD_NUMBER = "0000000000000000";
    public static final String EXPIRED_CARD_NUMBER = "4532999999999999";
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    public static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-50.00");
    
    // Category and type codes
    public static final String CATEGORY_CODE_GAS = "GAS";
    public static final String CATEGORY_CODE_GROCERY = "GRO";
    public static final String CATEGORY_CODE_RESTAURANT = "RST";
    
    // Additional test data
    public static final String TEST_TRANSACTION_ID = "TXN123456789";
    public static final String TEST_AUTH_CODE = "AUTH123";
    public static final String TEST_REFERENCE_NUMBER = "REF123456";
}