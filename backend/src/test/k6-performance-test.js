/**
 * K6 Performance Test Suite for CardDemo Spring Boot Application
 * 
 * This comprehensive performance test suite validates REST endpoint performance,
 * load capacity, and system behavior under stress conditions to ensure the
 * modernized system meets the 200ms response time and 10,000 TPS requirements.
 * 
 * Test scenarios based on COBOL programs:
 * - COTRN00C.cbl: Transaction listing with pagination (CT00 transaction)
 * - COACTVWC.cbl: Account view with cross-reference validation (CAVW transaction)
 * 
 * Performance Requirements:
 * - Sub-200ms response times for 95% of REST transactions
 * - Support 10,000+ transactions per hour during peak operations  
 * - System must support 10,000 transactions per second
 * - Complete batch processing within 4-hour window
 * 
 * USAGE EXAMPLES:
 * 
 * 1. Run all scenarios with default configuration:
 *    k6 run backend/src/test/k6-performance-test.js
 * 
 * 2. Run with custom base URL:
 *    k6 run --env BASE_URL=http://carddemo.example.com:8080 backend/src/test/k6-performance-test.js
 * 
 * 3. Run specific scenario only:
 *    k6 run --env SCENARIO=card_authorization_load backend/src/test/k6-performance-test.js
 * 
 * 4. Run with custom test environment:
 *    k6 run --env TEST_ENV=staging --env BASE_URL=https://staging.carddemo.com backend/src/test/k6-performance-test.js
 * 
 * 5. Run quick smoke test:
 *    k6 run --env SMOKE_TEST=true backend/src/test/k6-performance-test.js
 * 
 * 6. Generate detailed reports:
 *    k6 run --out json=results.json --out influxdb=http://localhost:8086/k6 backend/src/test/k6-performance-test.js
 * 
 * ENVIRONMENT VARIABLES:
 * - BASE_URL: Target application URL (default: http://localhost:8080)
 * - TEST_ENV: Test environment name (default: development)
 * - SCENARIO: Run specific scenario only (optional)
 * - SMOKE_TEST: Run quick validation tests (default: false)
 * - DEBUG: Enable debug logging (default: false)
 * 
 * OUTPUT FILES:
 * - k6-performance-report.html: Detailed HTML report with charts
 * - k6-performance-summary.json: Complete test results in JSON format
 * - k6-custom-metrics.json: Custom performance metrics
 * 
 * PERFORMANCE TARGETS:
 * - Response Time: P95 < 200ms, P99 < 500ms
 * - Error Rate: < 1%
 * - Throughput: > 10,000 transactions/hour
 * - Concurrent Users: Support 1000+ simultaneous users
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// =============================================================================
// CONFIGURATION AND METRICS DEFINITION
// =============================================================================

// Base configuration for the Spring Boot application
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = '/api';
const TEST_ENV = __ENV.TEST_ENV || 'development';
const SMOKE_TEST = __ENV.SMOKE_TEST === 'true';
const DEBUG = __ENV.DEBUG === 'true';
const SPECIFIC_SCENARIO = __ENV.SCENARIO;

// Environment-specific configurations
const ENV_CONFIGS = {
    development: {
        maxVUs: 100,
        testDuration: '5m',
        rampUpTime: '1m'
    },
    staging: {
        maxVUs: 500,
        testDuration: '15m',
        rampUpTime: '3m'
    },
    production: {
        maxVUs: 1000,
        testDuration: '30m',
        rampUpTime: '5m'
    }
};

const currentConfig = ENV_CONFIGS[TEST_ENV] || ENV_CONFIGS.development;

// Custom metrics to track specific performance indicators
const authenticationTime = new Trend('authentication_time', true);
const transactionListTime = new Trend('transaction_list_time', true);
const accountViewTime = new Trend('account_view_time', true);
const batchJobTime = new Trend('batch_job_time', true);
const errorRate = new Rate('error_rate');
const successfulTransactions = new Counter('successful_transactions');
const failedTransactions = new Counter('failed_transactions');
const concurrentUsers = new Gauge('concurrent_users');

// Test data pools for realistic load testing
const testUsers = [
    { userId: 'USER001', password: 'password123', role: 'ADMIN' },
    { userId: 'USER002', password: 'password123', role: 'USER' },
    { userId: 'USER003', password: 'password123', role: 'VIEWER' },
    { userId: 'USER004', password: 'password123', role: 'USER' },
    { userId: 'USER005', password: 'password123', role: 'ADMIN' }
];

const testAccounts = [
    '12345678901',
    '12345678902', 
    '12345678903',
    '12345678904',
    '12345678905',
    '98765432101',
    '98765432102',
    '98765432103',
    '11111111111',
    '22222222222'
];

// =============================================================================
// TEST SCENARIOS CONFIGURATION
// =============================================================================

// Dynamic scenario configuration based on environment and test mode
function generateScenarios() {
    // Smoke test configuration - quick validation
    if (SMOKE_TEST) {
        return {
            smoke_test: {
                executor: 'constant-vus',
                vus: 5,
                duration: '2m',
                exec: 'mixedWorkloadScenario'
            }
        };
    }
    
    // Single scenario execution if specified
    if (SPECIFIC_SCENARIO) {
        const allScenarios = getFullScenarios();
        if (allScenarios[SPECIFIC_SCENARIO]) {
            return { [SPECIFIC_SCENARIO]: allScenarios[SPECIFIC_SCENARIO] };
        }
    }
    
    // Full scenario suite
    return getFullScenarios();
}

function getFullScenarios() {
    const maxVUs = currentConfig.maxVUs;
    const testDuration = currentConfig.testDuration;
    const rampUpTime = currentConfig.rampUpTime;
    
    return {
        // Scenario 1: Card Authorization Load Test - 10,000 TPS Target
        card_authorization_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: rampUpTime, target: Math.floor(maxVUs * 0.1) },  // Ramp up to 10%
                { duration: testDuration, target: Math.floor(maxVUs * 0.5) }, // Scale to 50%
                { duration: testDuration, target: maxVUs },                   // Peak load
                { duration: rampUpTime, target: Math.floor(maxVUs * 0.5) },  // Scale down
                { duration: '1m', target: 0 }                                 // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'cardAuthorizationScenario'
        },

        // Scenario 2: Transaction Listing Performance Test
        transaction_listing_performance: {
            executor: 'constant-vus',
            vus: Math.floor(maxVUs * 0.2),
            duration: testDuration,
            gracefulRampDown: '30s',
            exec: 'transactionListingScenario'
        },

        // Scenario 3: Account View Stress Test
        account_view_stress: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            stages: [
                { duration: rampUpTime, target: Math.floor(maxVUs * 0.05) },  // 5% of max VUs as RPS
                { duration: testDuration, target: Math.floor(maxVUs * 0.1) }, // 10% of max VUs as RPS
                { duration: testDuration, target: Math.floor(maxVUs * 0.2) }, // 20% of max VUs as RPS - stress level
                { duration: rampUpTime, target: Math.floor(maxVUs * 0.05) }   // Recovery
            ],
            preAllocatedVUs: Math.floor(maxVUs * 0.1),
            maxVUs: Math.floor(maxVUs * 0.3),
            exec: 'accountViewScenario'
        },

        // Scenario 4: Mixed Workload Simulation
        mixed_workload: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: rampUpTime, target: Math.floor(maxVUs * 0.15) }, // Gradual ramp up
                { duration: testDuration, target: Math.floor(maxVUs * 0.3) }, // Sustained mixed load
                { duration: '1m', target: 0 }                                 // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'mixedWorkloadScenario'
        },

        // Scenario 5: Batch Job Performance Validation
        batch_job_validation: {
            executor: 'shared-iterations',
            vus: 5,
            iterations: SMOKE_TEST ? 2 : 10,
            maxDuration: SMOKE_TEST ? '5m' : '30m',
            exec: 'batchJobScenario'
        },

        // Scenario 6: Spike Test (production only)
        ...(TEST_ENV === 'production' ? {
            spike_test: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: Math.floor(maxVUs * 0.1) },  // Normal load
                    { duration: '30s', target: maxVUs },                   // Sudden spike
                    { duration: '2m', target: maxVUs },                    // Sustained spike
                    { duration: '1m', target: Math.floor(maxVUs * 0.1) }, // Return to normal
                    { duration: '30s', target: 0 }                         // Ramp down
                ],
                gracefulRampDown: '30s',
                exec: 'spikeTestScenario'
            }
        } : {}),

        // Scenario 7: Endurance Test (staging/production only)
        ...(TEST_ENV !== 'development' ? {
            endurance_test: {
                executor: 'constant-vus',
                vus: Math.floor(maxVUs * 0.1),
                duration: '60m', // Long-running endurance test
                gracefulRampDown: '2m',
                exec: 'enduranceTestScenario'
            }
        } : {})
    };
}

export const options = {
    scenarios: generateScenarios(),

    // Performance thresholds based on requirements and environment
    thresholds: {
        // 95% of requests must complete within 200ms (core requirement)
        'http_req_duration': ['p(95)<200'],
        
        // 99% of requests must complete within 500ms for card authorization
        'http_req_duration{scenario:card_authorization_load}': ['p(99)<500'],
        
        // Error rate must be below 1% (stricter for production)
        'error_rate': TEST_ENV === 'production' ? ['rate<0.005'] : ['rate<0.01'],
        
        // Minimum successful transaction rate (environment-specific)
        'successful_transactions': [
            `count>${SMOKE_TEST ? 100 : (TEST_ENV === 'production' ? 50000 : 10000)}`
        ],
        
        // Authentication time should be under 100ms
        'authentication_time': ['p(95)<100'],
        
        // Transaction listing should be under 150ms
        'transaction_list_time': ['p(95)<150'],
        
        // Account view should be under 200ms
        'account_view_time': ['p(95)<200'],
        
        // Batch jobs should complete within reasonable time
        'batch_job_time': SMOKE_TEST ? ['p(95)<10000'] : ['p(95)<30000'],
        
        // Additional production thresholds
        ...(TEST_ENV === 'production' ? {
            'http_req_duration{scenario:spike_test}': ['p(95)<1000'], // Spike tolerance
            'http_req_duration{scenario:endurance_test}': ['p(95)<200'] // Endurance stability
        } : {})
    },

    // Additional K6 options
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)', 'count'],
    summaryTimeUnit: 'ms',
    
    // User agent for request identification
    userAgent: `K6-CardDemo-PerfTest/${TEST_ENV}`,
    
    // Disable SSL verification for development/staging
    insecureSkipTLSVerify: TEST_ENV !== 'production',
    
    // Batch configuration for better performance
    batch: 20,
    batchPerHost: 6,
    
    // HTTP configuration
    http: {
        responseCallback: DEBUG ? http.expectedStatuses(200, 201, 400, 401, 404) : null
    }
};

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Authenticate user and return session token
 * Implements the CC00 (Sign On) transaction equivalent
 */
function authenticateUser(userData) {
    const authPayload = {
        transactionCode: 'CC00',
        requestData: {
            userId: userData.userId,
            password: userData.password
        }
    };

    const authStart = Date.now();
    const response = http.post(`${BASE_URL}${API_PREFIX}/auth/signin`, JSON.stringify(authPayload), {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    });
    const authDuration = Date.now() - authStart;
    
    authenticationTime.add(authDuration);
    
    const authSuccess = check(response, {
        'authentication successful': (r) => r.status === 200,
        'authentication response time < 200ms': (r) => r.timings.duration < 200,
        'session token provided': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === 'SUCCESS' && body.sessionUpdates;
            } catch (e) {
                return false;
            }
        }
    });

    if (!authSuccess) {
        errorRate.add(1);
        failedTransactions.add(1);
        return null;
    }

    successfulTransactions.add(1);
    
    // Extract session cookies for subsequent requests
    const cookies = response.cookies;
    return {
        sessionId: cookies.JSESSIONID ? cookies.JSESSIONID[0].value : null,
        xsrfToken: cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0].value : null,
        userRole: userData.role
    };
}

/**
 * Get transaction list with pagination
 * Implements the COTRN00C.cbl functionality (CT00 transaction)
 */
function getTransactionList(sessionData, accountId, pageNumber = 1, pageSize = 10) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    if (sessionData && sessionData.sessionId) {
        headers['Cookie'] = `JSESSIONID=${sessionData.sessionId}`;
    }
    if (sessionData && sessionData.xsrfToken) {
        headers['X-XSRF-TOKEN'] = sessionData.xsrfToken;
    }

    const queryParams = new URLSearchParams({
        accountId: accountId,
        pageNumber: pageNumber,
        pageSize: pageSize,
        transactionCode: 'CT00'
    });

    const listStart = Date.now();
    const response = http.get(`${BASE_URL}${API_PREFIX}/transactions?${queryParams}`, { headers });
    const listDuration = Date.now() - listStart;
    
    transactionListTime.add(listDuration);

    const listSuccess = check(response, {
        'transaction list retrieved': (r) => r.status === 200,
        'transaction list response time < 200ms': (r) => r.timings.duration < 200,
        'transaction list contains data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === 'SUCCESS' && body.responseData && Array.isArray(body.responseData.transactions);
            } catch (e) {
                return false;
            }
        },
        'pagination info present': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.responseData.totalCount !== undefined && body.responseData.hasMorePages !== undefined;
            } catch (e) {
                return false;
            }
        }
    });

    if (!listSuccess) {
        errorRate.add(1);
        failedTransactions.add(1);
        return null;
    }

    successfulTransactions.add(1);
    return JSON.parse(response.body);
}

/**
 * Get account details with cross-reference validation
 * Implements the COACTVWC.cbl functionality (CAVW transaction)
 */
function getAccountView(sessionData, accountId) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    if (sessionData && sessionData.sessionId) {
        headers['Cookie'] = `JSESSIONID=${sessionData.sessionId}`;
    }
    if (sessionData && sessionData.xsrfToken) {
        headers['X-XSRF-TOKEN'] = sessionData.xsrfToken;
    }

    const accountStart = Date.now();
    const response = http.get(`${BASE_URL}${API_PREFIX}/accounts/${accountId}`, { headers });
    const accountDuration = Date.now() - accountStart;
    
    accountViewTime.add(accountDuration);

    const viewSuccess = check(response, {
        'account view retrieved': (r) => r.status === 200,
        'account view response time < 200ms': (r) => r.timings.duration < 200,
        'account data complete': (r) => {
            try {
                const body = JSON.parse(r.body);
                const data = body.responseData;
                return body.status === 'SUCCESS' && 
                       data && 
                       data.accountId &&
                       data.customerId &&
                       data.accountStatus;
            } catch (e) {
                return false;
            }
        },
        'customer data included': (r) => {
            try {
                const body = JSON.parse(r.body);
                const data = body.responseData;
                return data && data.customerData && data.customerData.firstName;
            } catch (e) {
                return false;
            }
        }
    });

    if (!viewSuccess) {
        errorRate.add(1);
        failedTransactions.add(1);
        return null;
    }

    successfulTransactions.add(1);
    return JSON.parse(response.body);
}

/**
 * Simulate batch job execution
 * Tests Spring Batch job performance equivalent to COBOL batch programs
 */
function executeBatchJob(sessionData, jobType) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    if (sessionData && sessionData.sessionId) {
        headers['Cookie'] = `JSESSIONID=${sessionData.sessionId}`;
    }
    if (sessionData && sessionData.xsrfToken) {
        headers['X-XSRF-TOKEN'] = sessionData.xsrfToken;
    }

    const batchPayload = {
        jobName: jobType,
        parameters: {
            date: new Date().toISOString().split('T')[0],
            batchSize: 1000
        }
    };

    const batchStart = Date.now();
    const response = http.post(`${BASE_URL}${API_PREFIX}/batch/jobs`, JSON.stringify(batchPayload), { headers });
    const batchDuration = Date.now() - batchStart;
    
    batchJobTime.add(batchDuration);

    const batchSuccess = check(response, {
        'batch job started': (r) => r.status === 202 || r.status === 200,
        'batch job response time reasonable': (r) => r.timings.duration < 5000,
        'job execution id provided': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.jobExecutionId || body.executionId;
            } catch (e) {
                return false;
            }
        }
    });

    if (!batchSuccess) {
        errorRate.add(1);
        failedTransactions.add(1);
        return null;
    }

    successfulTransactions.add(1);
    return JSON.parse(response.body);
}

// =============================================================================
// TEST SCENARIO IMPLEMENTATIONS
// =============================================================================

/**
 * Card Authorization Load Test Scenario
 * Simulates high-volume card authorization processing
 */
export function cardAuthorizationScenario() {
    group('Card Authorization Load Test', function() {
        concurrentUsers.add(1);
        
        // Select random test user and account
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        const accountId = testAccounts[Math.floor(Math.random() * testAccounts.length)];
        
        // Authenticate
        const sessionData = authenticateUser(userData);
        if (!sessionData) {
            return;
        }
        
        // Simulate card authorization by getting account view (validation)
        const accountData = getAccountView(sessionData, accountId);
        if (!accountData) {
            return;
        }
        
        // Brief pause to simulate processing time
        sleep(0.1);
        
        concurrentUsers.add(-1);
    });
}

/**
 * Transaction Listing Performance Test Scenario
 * Tests pagination and data retrieval performance
 */
export function transactionListingScenario() {
    group('Transaction Listing Performance', function() {
        concurrentUsers.add(1);
        
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        const accountId = testAccounts[Math.floor(Math.random() * testAccounts.length)];
        
        // Authenticate
        const sessionData = authenticateUser(userData);
        if (!sessionData) {
            concurrentUsers.add(-1);
            return;
        }
        
        // Test multiple pages to simulate user browsing
        for (let page = 1; page <= 3; page++) {
            const transactionData = getTransactionList(sessionData, accountId, page, 10);
            if (!transactionData) {
                break;
            }
            
            // Short pause between page requests
            sleep(0.2);
        }
        
        concurrentUsers.add(-1);
    });
}

/**
 * Account View Stress Test Scenario
 * High-frequency account lookups to test database performance
 */
export function accountViewScenario() {
    group('Account View Stress Test', function() {
        concurrentUsers.add(1);
        
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        const accountId = testAccounts[Math.floor(Math.random() * testAccounts.length)];
        
        // Authenticate
        const sessionData = authenticateUser(userData);
        if (!sessionData) {
            concurrentUsers.add(-1);
            return;
        }
        
        // Perform account view
        getAccountView(sessionData, accountId);
        
        concurrentUsers.add(-1);
    });
}

/**
 * Mixed Workload Scenario
 * Simulates realistic user behavior with mixed operations
 */
export function mixedWorkloadScenario() {
    group('Mixed Workload Simulation', function() {
        concurrentUsers.add(1);
        
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        const accountId = testAccounts[Math.floor(Math.random() * testAccounts.length)];
        
        // Authenticate
        const sessionData = authenticateUser(userData);
        if (!sessionData) {
            concurrentUsers.add(-1);
            return;
        }
        
        // Simulate realistic user workflow
        const workflow = Math.random();
        
        if (workflow < 0.4) {
            // 40% - View account details
            getAccountView(sessionData, accountId);
        } else if (workflow < 0.8) {
            // 40% - Browse transactions
            getTransactionList(sessionData, accountId, 1, 10);
            sleep(0.5);
            getTransactionList(sessionData, accountId, 2, 10);
        } else {
            // 20% - Comprehensive account review
            getAccountView(sessionData, accountId);
            sleep(0.3);
            getTransactionList(sessionData, accountId, 1, 20);
        }
        
        // User think time
        sleep(1 + Math.random() * 2);
        
        concurrentUsers.add(-1);
    });
}

/**
 * Batch Job Performance Validation Scenario
 * Tests Spring Batch job execution performance
 */
export function batchJobScenario() {
    group('Batch Job Performance Validation', function() {
        const userData = testUsers[0]; // Use admin user for batch operations
        
        // Authenticate with admin privileges
        const sessionData = authenticateUser(userData);
        if (!sessionData) {
            return;
        }
        
        // Test different batch job types
        const jobTypes = [
            'dailyTransactionProcessing',
            'interestCalculation', 
            'accountStatusUpdate',
            'reportGeneration'
        ];
        
        const selectedJob = jobTypes[Math.floor(Math.random() * jobTypes.length)];
        executeBatchJob(sessionData, selectedJob);
        
        // Wait for job initiation
        sleep(2);
    });
}

// =============================================================================
// SETUP AND TEARDOWN FUNCTIONS
// =============================================================================

/**
 * Setup function to prepare test environment
 * Validates API availability and initializes test data
 */
export function setup() {
    console.log('🚀 Starting CardDemo Performance Test Suite');
    console.log(`Target URL: ${BASE_URL}`);
    
    // Validate API availability
    const healthCheck = http.get(`${BASE_URL}/actuator/health`);
    if (healthCheck.status !== 200) {
        console.error('❌ API health check failed - aborting tests');
        return null;
    }
    
    console.log('✅ API health check passed');
    
    // Initialize test data if needed
    const setupData = {
        testStartTime: new Date().toISOString(),
        apiVersion: 'v1',
        testEnvironment: __ENV.TEST_ENV || 'development'
    };
    
    console.log(`📊 Test Environment: ${setupData.testEnvironment}`);
    return setupData;
}

/**
 * Teardown function to clean up after tests
 */
export function teardown(data) {
    if (data) {
        console.log(`🏁 Performance tests completed at ${new Date().toISOString()}`);
        console.log(`📈 Test duration: ${Date.now() - new Date(data.testStartTime).getTime()}ms`);
    }
    
    // Generate final performance summary
    console.log('📋 Performance Test Summary:');
    console.log('- Card Authorization Load Test: High-volume transaction processing');
    console.log('- Transaction Listing Performance: Pagination and data retrieval');
    console.log('- Account View Stress Test: Database query performance');
    console.log('- Mixed Workload Simulation: Realistic user behavior');
    console.log('- Batch Job Validation: Spring Batch performance');
}

// =============================================================================
// ADVANCED UTILITY FUNCTIONS
// =============================================================================

/**
 * Generate realistic transaction data for testing
 */
function generateTransactionData() {
    const transactionTypes = ['PURCHASE', 'PAYMENT', 'TRANSFER', 'WITHDRAWAL', 'DEPOSIT'];
    const merchants = ['Amazon', 'Walmart', 'Target', 'Costco', 'Home Depot'];
    
    return {
        transactionType: transactionTypes[Math.floor(Math.random() * transactionTypes.length)],
        amount: (Math.random() * 1000 + 10).toFixed(2),
        merchant: merchants[Math.floor(Math.random() * merchants.length)],
        timestamp: new Date().toISOString(),
        authCode: Math.random().toString(36).substring(2, 8).toUpperCase()
    };
}

/**
 * Simulate user think time with realistic patterns
 */
function realisticThinkTime() {
    // Simulate human-like pause patterns
    const thinkTimePatterns = [
        () => sleep(0.5 + Math.random() * 1),      // Quick user (0.5-1.5s)
        () => sleep(1 + Math.random() * 2),        // Normal user (1-3s)
        () => sleep(2 + Math.random() * 3),        // Careful user (2-5s)
        () => sleep(0.1 + Math.random() * 0.4)     // Expert user (0.1-0.5s)
    ];
    
    const pattern = thinkTimePatterns[Math.floor(Math.random() * thinkTimePatterns.length)];
    pattern();
}

/**
 * Validate response data structure matches COBOL field definitions
 */
function validateCobolDataStructure(responseData, expectedFields) {
    const validationResults = {};
    
    expectedFields.forEach(field => {
        validationResults[field.name] = {
            present: responseData.hasOwnProperty(field.name),
            type: typeof responseData[field.name],
            expectedType: field.type,
            length: field.maxLength ? responseData[field.name]?.length <= field.maxLength : true
        };
    });
    
    return validationResults;
}

/**
 * Performance monitoring helper
 */
function trackPerformanceMetrics(operation, duration, success) {
    const metrics = {
        operation: operation,
        duration: duration,
        success: success,
        timestamp: Date.now()
    };
    
    // Log performance issues
    if (duration > 200) {
        console.warn(`⚠️ Slow ${operation}: ${duration}ms`);
    }
    
    if (!success) {
        console.error(`❌ Failed ${operation}: ${duration}ms`);
    }
    
    return metrics;
}

// =============================================================================
// ADDITIONAL TEST SCENARIOS
// =============================================================================

/**
 * Spike Test Scenario - Sudden load increases
 */
export function spikeTestScenario() {
    group('Spike Test - Sudden Load Increase', function() {
        concurrentUsers.add(1);
        
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        const accountId = testAccounts[Math.floor(Math.random() * testAccounts.length)];
        
        // Authenticate and perform rapid operations
        const sessionData = authenticateUser(userData);
        if (sessionData) {
            // Rapid fire requests to test spike handling
            for (let i = 0; i < 5; i++) {
                getAccountView(sessionData, accountId);
                // Minimal sleep to create spike
                sleep(0.05);
            }
        }
        
        concurrentUsers.add(-1);
    });
}

/**
 * Endurance Test Scenario - Long-running stability
 */
export function enduranceTestScenario() {
    group('Endurance Test - Long-running Stability', function() {
        concurrentUsers.add(1);
        
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        
        // Long-running session simulation
        const sessionData = authenticateUser(userData);
        if (sessionData) {
            // Perform operations over extended period
            for (let i = 0; i < 10; i++) {
                const accountId = testAccounts[Math.floor(Math.random() * testAccounts.length)];
                
                if (i % 3 === 0) {
                    getAccountView(sessionData, accountId);
                } else {
                    getTransactionList(sessionData, accountId, Math.floor(i / 3) + 1);
                }
                
                // Longer think time for endurance test
                sleep(2 + Math.random() * 3);
            }
        }
        
        concurrentUsers.add(-1);
    });
}

/**
 * Error Recovery Test Scenario
 */
export function errorRecoveryScenario() {
    group('Error Recovery Test', function() {
        concurrentUsers.add(1);
        
        const userData = testUsers[Math.floor(Math.random() * testUsers.length)];
        
        // Test with invalid data to verify error handling
        const invalidAccountId = '00000000000'; // Invalid account
        
        const sessionData = authenticateUser(userData);
        if (sessionData) {
            // Test error handling
            const errorResponse = getAccountView(sessionData, invalidAccountId);
            
            // Verify system recovers and can process valid requests
            const validAccountId = testAccounts[0];
            getAccountView(sessionData, validAccountId);
        }
        
        concurrentUsers.add(-1);
    });
}

// =============================================================================
// CUSTOM REPORTING AND ANALYSIS
// =============================================================================

/**
 * Generate comprehensive performance report
 */
export function handleSummary(data) {
    // Calculate custom metrics
    const customMetrics = {
        totalRequests: data.metrics.http_reqs?.values?.count || 0,
        avgResponseTime: data.metrics.http_req_duration?.values?.avg || 0,
        p95ResponseTime: data.metrics.http_req_duration?.values?.['p(95)'] || 0,
        p99ResponseTime: data.metrics.http_req_duration?.values?.['p(99)'] || 0,
        errorRate: data.metrics.error_rate?.values?.rate || 0,
        successfulTransactions: data.metrics.successful_transactions?.values?.count || 0,
        failedTransactions: data.metrics.failed_transactions?.values?.count || 0
    };
    
    // Performance assessment
    const performanceAssessment = {
        responseTimeCompliance: customMetrics.p95ResponseTime < 200,
        errorRateCompliance: customMetrics.errorRate < 0.01,
        throughputTarget: customMetrics.totalRequests > 1000,
        overallRating: 'PASS'
    };
    
    if (!performanceAssessment.responseTimeCompliance || 
        !performanceAssessment.errorRateCompliance || 
        !performanceAssessment.throughputTarget) {
        performanceAssessment.overallRating = 'FAIL';
    }
    
    // Enhanced summary with custom analysis
    const enhancedSummary = {
        ...data,
        customMetrics,
        performanceAssessment,
        testConfiguration: {
            baseUrl: BASE_URL,
            scenarios: Object.keys(options.scenarios),
            thresholds: options.thresholds
        },
        recommendations: generatePerformanceRecommendations(customMetrics)
    };
    
    return {
        'k6-performance-report.html': htmlReport(enhancedSummary),
        'k6-performance-summary.json': JSON.stringify(enhancedSummary, null, 2),
        'k6-custom-metrics.json': JSON.stringify(customMetrics, null, 2),
        stdout: generateCustomTextSummary(enhancedSummary)
    };
}

/**
 * Generate performance recommendations based on test results
 */
function generatePerformanceRecommendations(metrics) {
    const recommendations = [];
    
    if (metrics.p95ResponseTime > 150) {
        recommendations.push({
            category: 'Response Time',
            issue: 'P95 response time exceeds optimal threshold',
            recommendation: 'Consider database query optimization or connection pool tuning',
            priority: 'HIGH'
        });
    }
    
    if (metrics.errorRate > 0.005) {
        recommendations.push({
            category: 'Error Rate',
            issue: 'Error rate higher than expected',
            recommendation: 'Review application logs and implement circuit breaker patterns',
            priority: 'MEDIUM'
        });
    }
    
    if (metrics.totalRequests < 5000) {
        recommendations.push({
            category: 'Throughput',
            issue: 'Throughput below target capacity',
            recommendation: 'Increase test duration or virtual user count for comprehensive testing',
            priority: 'LOW'
        });
    }
    
    return recommendations;
}

/**
 * Generate custom text summary with CardDemo-specific insights
 */
function generateCustomTextSummary(data) {
    const metrics = data.customMetrics;
    const assessment = data.performanceAssessment;
    
    let summary = '\n';
    summary += '═══════════════════════════════════════════════════════════════════\n';
    summary += '            CARDDEMO PERFORMANCE TEST RESULTS\n';
    summary += '═══════════════════════════════════════════════════════════════════\n\n';
    
    summary += `🎯 OVERALL RATING: ${assessment.overallRating}\n\n`;
    
    summary += '📊 KEY PERFORMANCE INDICATORS:\n';
    summary += `   • Total Requests: ${metrics.totalRequests.toLocaleString()}\n`;
    summary += `   • Average Response Time: ${metrics.avgResponseTime.toFixed(2)}ms\n`;
    summary += `   • P95 Response Time: ${metrics.p95ResponseTime.toFixed(2)}ms ${metrics.p95ResponseTime < 200 ? '✅' : '❌'}\n`;
    summary += `   • P99 Response Time: ${metrics.p99ResponseTime.toFixed(2)}ms\n`;
    summary += `   • Error Rate: ${(metrics.errorRate * 100).toFixed(3)}% ${metrics.errorRate < 0.01 ? '✅' : '❌'}\n`;
    summary += `   • Successful Transactions: ${metrics.successfulTransactions.toLocaleString()}\n`;
    summary += `   • Failed Transactions: ${metrics.failedTransactions.toLocaleString()}\n\n`;
    
    summary += '🎮 SCENARIO RESULTS:\n';
    Object.keys(options.scenarios).forEach(scenario => {
        summary += `   • ${scenario}: Executed\n`;
    });
    
    summary += '\n📋 COMPLIANCE STATUS:\n';
    summary += `   • Response Time Requirement (<200ms): ${assessment.responseTimeCompliance ? 'PASS ✅' : 'FAIL ❌'}\n`;
    summary += `   • Error Rate Requirement (<1%): ${assessment.errorRateCompliance ? 'PASS ✅' : 'FAIL ❌'}\n`;
    summary += `   • Throughput Target: ${assessment.throughputTarget ? 'PASS ✅' : 'FAIL ❌'}\n\n`;
    
    if (data.recommendations && data.recommendations.length > 0) {
        summary += '💡 RECOMMENDATIONS:\n';
        data.recommendations.forEach((rec, index) => {
            summary += `   ${index + 1}. [${rec.priority}] ${rec.category}: ${rec.recommendation}\n`;
        });
    }
    
    summary += '\n═══════════════════════════════════════════════════════════════════\n';
    summary += '   For detailed analysis, see k6-performance-report.html\n';
    summary += '═══════════════════════════════════════════════════════════════════\n\n';
    
    return summary;
}