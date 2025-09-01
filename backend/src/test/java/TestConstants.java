/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Test constants for CardDemo batch processing test suite providing centralized configuration
 * for concurrent batch execution testing, performance validation, and resource contention scenarios.
 * 
 * This class contains constants used across all batch processing test scenarios to ensure
 * consistency with mainframe processing windows, performance thresholds, and precision requirements.
 * 
 * Key Features:
 * - Batch processing window constraints matching JCL execution limits
 * - Financial precision constants ensuring COBOL COMP-3 equivalence
 * - Performance thresholds for response time validation (<200ms target)
 * - Concurrent execution limits and resource contention parameters
 * - Test data generation constants and boundary values
 * - Retry and recovery timeout configurations
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since 2024
 */
public final class TestConstants {

    // Private constructor to prevent instantiation
    private TestConstants() {
        throw new UnsupportedOperationException("TestConstants is a utility class and cannot be instantiated");
    }

    // ============================================================================
    // BATCH PROCESSING WINDOW CONSTANTS
    // ============================================================================
    
    /**
     * Maximum batch processing window in hours matching mainframe JCL execution limits.
     * Used to validate that Spring Batch jobs complete within acceptable timeframes.
     */
    public static final int BATCH_PROCESSING_WINDOW_HOURS = 4;
    
    /**
     * Batch processing window in milliseconds for timeout calculations.
     */
    public static final long BATCH_PROCESSING_WINDOW_MS = BATCH_PROCESSING_WINDOW_HOURS * 60 * 60 * 1000L;
    
    /**
     * Daily batch job start time offset in hours from midnight.
     */
    public static final int DAILY_BATCH_START_HOUR = 2;
    
    /**
     * Monthly batch job execution day of month.
     */
    public static final int MONTHLY_BATCH_EXECUTION_DAY = 1;
    
    /**
     * Maximum concurrent batch jobs allowed to execute simultaneously.
     */
    public static final int MAX_CONCURRENT_BATCH_JOBS = 3;
    
    /**
     * Batch job execution timeout in minutes for individual job steps.
     */
    public static final long BATCH_JOB_STEP_TIMEOUT_MINUTES = 30;

    // ============================================================================
    // PERFORMANCE TESTING CONSTANTS
    // ============================================================================
    
    /**
     * Target transactions per second (TPS) for load testing scenarios.
     * Based on mainframe CICS region capacity requirements.
     */
    public static final int TARGET_TPS = 10000;
    
    /**
     * Maximum acceptable response time in milliseconds for REST API endpoints.
     * Critical threshold for maintaining user experience parity with mainframe.
     */
    public static final long MAX_RESPONSE_TIME_MS = 200L;
    
    /**
     * Performance test duration in seconds for sustained load testing.
     */
    public static final long PERFORMANCE_TEST_DURATION_SECONDS = 300L;
    
    /**
     * Concurrent user simulation count for load testing scenarios.
     */
    public static final int CONCURRENT_USERS = 1000;
    
    /**
     * API endpoint warm-up requests before performance measurement begins.
     */
    public static final int WARMUP_REQUESTS = 100;
    
    /**
     * Performance test success rate threshold (95th percentile).
     */
    public static final double PERFORMANCE_SUCCESS_RATE_THRESHOLD = 0.95;

    // ============================================================================
    // FINANCIAL PRECISION CONSTANTS  
    // ============================================================================
    
    /**
     * COBOL COMP-3 decimal scale for financial calculations ensuring penny-level precision.
     * Matches original mainframe packed decimal precision requirements.
     */
    public static final int COBOL_DECIMAL_SCALE = 2;
    
    /**
     * Interest rate calculation decimal scale (basis points precision).
     */
    public static final int INTEREST_RATE_DECIMAL_SCALE = 4;
    
    /**
     * Maximum precision for financial calculations matching COBOL PIC clauses.
     */
    public static final int FINANCIAL_PRECISION = 15;
    
    /**
     * BigDecimal rounding mode matching COBOL ROUNDED modifier behavior.
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * Minimum transaction amount for testing (one cent).
     */
    public static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");
    
    /**
     * Maximum transaction amount for testing scenarios.
     */
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("999999.99");
    
    /**
     * Interest rate precision tolerance for calculation comparisons.
     */
    public static final BigDecimal INTEREST_CALCULATION_TOLERANCE = new BigDecimal("0.0001");

    // ============================================================================
    // DATABASE AND REPOSITORY CONSTANTS
    // ============================================================================
    
    /**
     * Default page size for pagination queries matching COBOL screen display limits.
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    /**
     * Maximum page size for large result set processing.
     */
    public static final int MAX_PAGE_SIZE = 1000;
    
    /**
     * Database connection pool size for concurrent testing scenarios.
     */
    public static final int DB_CONNECTION_POOL_SIZE = 20;
    
    /**
     * Maximum database connection wait time in seconds.
     */
    public static final long DB_CONNECTION_TIMEOUT_SECONDS = 30L;
    
    /**
     * Transaction isolation timeout in seconds for deadlock prevention.
     */
    public static final long TRANSACTION_ISOLATION_TIMEOUT_SECONDS = 10L;
    
    /**
     * Optimistic locking retry attempts for concurrent access scenarios.
     */
    public static final int OPTIMISTIC_LOCK_RETRY_ATTEMPTS = 3;

    // ============================================================================
    // RETRY AND RECOVERY CONSTANTS
    // ============================================================================
    
    /**
     * Maximum retry attempts for failed batch job executions.
     */
    public static final int RETRY_LIMIT = 3;
    
    /**
     * Base delay between retry attempts in milliseconds.
     */
    public static final long RETRY_BASE_DELAY_MS = 1000L;
    
    /**
     * Maximum delay between retry attempts in milliseconds.
     */
    public static final long RETRY_MAX_DELAY_MS = 10000L;
    
    /**
     * Recovery timeout in milliseconds for system failure scenarios.
     */
    public static final long RECOVERY_TIMEOUT_MS = 30000L;
    
    /**
     * Circuit breaker failure threshold before opening circuit.
     */
    public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    
    /**
     * Circuit breaker timeout in milliseconds before attempting recovery.
     */
    public static final long CIRCUIT_BREAKER_TIMEOUT_MS = 60000L;

    // ============================================================================
    // CHECKPOINT AND COMMIT CONSTANTS
    // ============================================================================
    
    /**
     * Checkpoint interval for batch job processing (number of records).
     */
    public static final int CHECKPOINT_INTERVAL = 1000;
    
    /**
     * Commit interval for database transaction batching.
     */
    public static final int COMMIT_INTERVAL = 100;
    
    /**
     * Skip limit for batch job error handling.
     */
    public static final int SKIP_LIMIT = 10;
    
    /**
     * Chunk size for Spring Batch step processing.
     */
    public static final int CHUNK_SIZE = 100;

    // ============================================================================
    // CONCURRENT EXECUTION CONSTANTS
    // ============================================================================
    
    /**
     * Thread pool size for concurrent batch job testing.
     */
    public static final int CONCURRENT_THREAD_POOL_SIZE = 10;
    
    /**
     * Maximum wait time for concurrent job completion in seconds.
     */
    public static final long CONCURRENT_JOB_COMPLETION_WAIT_SECONDS = 300L;
    
    /**
     * Resource contention test timeout in seconds.
     */
    public static final long RESOURCE_CONTENTION_TIMEOUT_SECONDS = 60L;
    
    /**
     * Deadlock detection timeout in milliseconds.
     */
    public static final long DEADLOCK_DETECTION_TIMEOUT_MS = 5000L;
    
    /**
     * Thread safety validation iterations for race condition testing.
     */
    public static final int THREAD_SAFETY_ITERATIONS = 1000;

    // ============================================================================
    // TEST DATA GENERATION CONSTANTS
    // ============================================================================
    
    /**
     * Default number of test accounts to generate for batch processing tests.
     */
    public static final int TEST_ACCOUNT_COUNT = 100;
    
    /**
     * Default number of test transactions per account.
     */
    public static final int TEST_TRANSACTIONS_PER_ACCOUNT = 50;
    
    /**
     * Test customer ID prefix for generated test data.
     */
    public static final String TEST_CUSTOMER_ID_PREFIX = "TST";
    
    /**
     * Test account number starting value.
     */
    public static final long TEST_ACCOUNT_NUMBER_START = 1000000001L;
    
    /**
     * Test transaction ID starting value.
     */
    public static final long TEST_TRANSACTION_ID_START = 2000000001L;
    
    /**
     * Maximum test data generation batch size.
     */
    public static final int TEST_DATA_BATCH_SIZE = 1000;

    // ============================================================================
    // FIELD LENGTH CONSTANTS (from COBOL copybook equivalents)
    // ============================================================================
    
    /**
     * Maximum customer ID length matching COBOL PIC X(9) field.
     */
    public static final int CUSTOMER_ID_MAX_LENGTH = 9;
    
    /**
     * Maximum account number length matching COBOL PIC X(11) field.
     */
    public static final int ACCOUNT_NUMBER_MAX_LENGTH = 11;
    
    /**
     * Maximum card number length matching COBOL PIC X(16) field.
     */
    public static final int CARD_NUMBER_MAX_LENGTH = 16;
    
    /**
     * Maximum transaction type code length matching COBOL PIC X(2) field.
     */
    public static final int TRANSACTION_TYPE_CODE_MAX_LENGTH = 2;
    
    /**
     * Maximum transaction description length matching COBOL PIC X(50) field.
     */
    public static final int TRANSACTION_DESCRIPTION_MAX_LENGTH = 50;
    
    /**
     * Maximum merchant name length matching COBOL PIC X(25) field.
     */
    public static final int MERCHANT_NAME_MAX_LENGTH = 25;

    // ============================================================================
    // TIMEOUT DURATION OBJECTS
    // ============================================================================
    
    /**
     * Duration object for batch processing window timeout.
     */
    public static final Duration BATCH_PROCESSING_WINDOW_DURATION = 
        Duration.ofHours(BATCH_PROCESSING_WINDOW_HOURS);
    
    /**
     * Duration object for API response timeout.
     */
    public static final Duration API_RESPONSE_TIMEOUT_DURATION = 
        Duration.ofMillis(MAX_RESPONSE_TIME_MS);
    
    /**
     * Duration object for database connection timeout.
     */
    public static final Duration DB_CONNECTION_TIMEOUT_DURATION = 
        Duration.ofSeconds(DB_CONNECTION_TIMEOUT_SECONDS);
    
    /**
     * Duration object for recovery timeout.
     */
    public static final Duration RECOVERY_TIMEOUT_DURATION = 
        Duration.ofMillis(RECOVERY_TIMEOUT_MS);

    // ============================================================================
    // VALIDATION CONSTANTS
    // ============================================================================
    
    /**
     * Minimum required test coverage percentage for batch job tests.
     */
    public static final double MINIMUM_TEST_COVERAGE = 0.90;
    
    /**
     * Maximum acceptable test execution time in seconds.
     */
    public static final long MAX_TEST_EXECUTION_TIME_SECONDS = 300L;
    
    /**
     * Test assertion timeout in milliseconds for async operations.
     */
    public static final long TEST_ASSERTION_TIMEOUT_MS = 10000L;
    
    /**
     * Polling interval in milliseconds for test condition checks.
     */
    public static final long TEST_POLLING_INTERVAL_MS = 100L;

    // ============================================================================
    // MONITORING AND ALERTING CONSTANTS
    // ============================================================================
    
    /**
     * Metrics collection interval in seconds for performance monitoring.
     */
    public static final long METRICS_COLLECTION_INTERVAL_SECONDS = 15L;
    
    /**
     * Alert threshold for memory usage percentage.
     */
    public static final double MEMORY_USAGE_ALERT_THRESHOLD = 0.85;
    
    /**
     * Alert threshold for CPU usage percentage.
     */
    public static final double CPU_USAGE_ALERT_THRESHOLD = 0.80;
    
    /**
     * Maximum log file size in MB before rotation.
     */
    public static final int MAX_LOG_FILE_SIZE_MB = 100;
    
    /**
     * JVM heap memory alert threshold in percentage.
     */
    public static final double HEAP_MEMORY_ALERT_THRESHOLD = 0.90;
}