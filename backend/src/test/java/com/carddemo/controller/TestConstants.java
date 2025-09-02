package com.carddemo.controller;

import java.math.RoundingMode;

/**
 * Test constants for performance testing and validation
 * Provides critical performance testing constants including response time thresholds,
 * TPS targets, COBOL decimal precision settings, and validation parameters essential
 * for performance test setup and result validation.
 */
public final class TestConstants {

    // Performance Testing Constants
    
    /**
     * Response time threshold in milliseconds for SLA validation
     * Based on requirement: sub-200ms response times
     */
    public static final long RESPONSE_TIME_THRESHOLD_MS = 200L;
    
    /**
     * Target transactions per second for load testing
     * Based on requirement: 10,000 TPS performance target
     */
    public static final int TARGET_TPS = 10000;
    
    /**
     * Maximum acceptable error rate percentage during load testing
     */
    public static final double MAX_ERROR_RATE_PERCENT = 1.0;
    
    /**
     * Batch processing window in hours for performance validation
     * Based on requirement: Complete batch processing within 4-hour window
     */
    public static final int BATCH_PROCESSING_WINDOW_HOURS = 4;
    
    // COBOL Data Precision Constants
    
    /**
     * Decimal scale for COBOL COMP-3 equivalent precision
     * Ensures BigDecimal calculations match mainframe precision
     */
    public static final int COBOL_DECIMAL_SCALE = 2;
    
    /**
     * Rounding mode matching COBOL ROUNDED clause behavior
     * HALF_UP provides equivalent rounding to COBOL operations
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // Authentication Test Constants
    
    /**
     * Standard test user ID for performance testing scenarios
     */
    public static final String TEST_USER_ID = "TESTUSER";
    
    /**
     * Standard test password for authentication testing (PIC X(8) compatible)
     */
    public static final String TEST_USER_PASSWORD = "testpass";
    
    /**
     * Standard test user role for security testing scenarios
     * 'U' represents regular user matching COBOL SEC-USR-TYPE logic
     */
    public static final String TEST_USER_ROLE = "U";
    
    /**
     * Standard test admin role for security testing scenarios  
     * 'A' represents admin user matching COBOL SEC-USR-TYPE logic
     */
    public static final String TEST_ADMIN_ROLE = "A";
    
    /**
     * Standard test account ID for integration testing scenarios
     */
    public static final String TEST_ACCOUNT_ID = "0000000001";
    
    /**
     * Standard test transaction ID for integration testing scenarios
     */
    public static final String TEST_TRANSACTION_ID = "T000000001";
    
    /**
     * Standard test card number for integration testing scenarios
     */
    public static final String TEST_CARD_NUMBER = "4532123456789012";
    
    // Performance Test Data Constants
    
    /**
     * Standard performance test data configuration
     */
    public static final class PERFORMANCE_TEST_DATA {
        public static final String ACCOUNT_ID = "0000000001";
        public static final String CARD_NUMBER = "4532123456789012";
        public static final String CUSTOMER_ID = "0000000001";
        public static final String TRANSACTION_ID = "T000000001";
        public static final String TRANSACTION_CATEGORY_CODE = "5411";
        public static final double DEFAULT_AMOUNT = 100.00;
        
        private PERFORMANCE_TEST_DATA() {}
    }
    
    // Validation Threshold Constants
    
    /**
     * Validation thresholds for different performance metrics
     */
    public static final class VALIDATION_THRESHOLDS {
        public static final long WARMUP_DURATION_MS = 5000L;
        public static final long COOLDOWN_DURATION_MS = 2000L;
        public static final int MIN_REQUESTS_FOR_VALID_TEST = 100;
        public static final double MEMORY_PRESSURE_THRESHOLD = 0.85;
        public static final double CONNECTION_PRESSURE_THRESHOLD = 0.80;
        public static final int DEFAULT_TEST_DURATION = 300; // 5 minutes for test transactions
        
        private VALIDATION_THRESHOLDS() {}
    }
    
    // JMeter Integration Constants
    
    /**
     * JMeter test configuration defaults
     */
    public static final class JMETER_CONFIG {
        public static final String DEFAULT_TEST_PLAN = "CardDemo-Performance-Test";
        public static final int DEFAULT_THREAD_COUNT = 50;
        public static final int DEFAULT_TEST_DURATION = 300; // 5 minutes
        public static final int DEFAULT_RAMP_UP_TIME = 30;
        
        private JMETER_CONFIG() {}
    }
    
    // Test Environment Constants
    
    /**
     * Test environment configuration values
     */
    public static final class TEST_ENV {
        public static final String LOCAL_HOST = "localhost";
        public static final int DEFAULT_PORT = 8080;
        public static final String TEST_CONTEXT_PATH = "/api";
        public static final String HEALTH_CHECK_ENDPOINT = "/actuator/health";
        
        private TEST_ENV() {}
    }
    
    private TestConstants() {
        // Utility class - prevent instantiation
    }
}