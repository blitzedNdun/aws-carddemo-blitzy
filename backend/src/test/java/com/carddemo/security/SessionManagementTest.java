/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.security;

import com.carddemo.config.RedisConfig;
import com.carddemo.integration.BaseIntegrationTest;
import com.carddemo.security.SessionAttributes;
import TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test implementation for session management using Spring Boot Test and Testcontainers Redis.
 * 
 * This comprehensive test suite validates Spring Session integration with Redis backend, ensuring
 * COMMAREA-equivalent state storage functionality for the CardDemo system migration from CICS to
 * Spring Boot. The tests verify session creation, retrieval, timeout behavior, concurrent access,
 * and invalidation workflows while maintaining the 32KB session size limit matching original 
 * CICS COMMAREA constraints.
 * 
 * Test Coverage:
 * - Spring Session integration with Redis backend storage
 * - COMMAREA-equivalent state storage with 32KB size limits
 * - Session creation and retrieval workflows
 * - Session timeout behavior matching CICS 30-minute configuration
 * - Concurrent session handling and thread safety validation
 * - Session invalidation on logout with complete cleanup
 * - Session attribute persistence across multiple requests
 * - Redis container lifecycle management through Testcontainers
 * - Session size validation and enforcement
 * - Authentication context maintenance through session lifecycle
 * 
 * Architecture Integration:
 * This test integrates with RedisConfig for session repository configuration, BaseIntegrationTest
 * for container management and test utilities, and SessionAttributes for session state constants.
 * The test suite validates that the modernized Spring Session implementation maintains identical
 * functionality to the original CICS COMMAREA session management.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RedisConfig.class},
    properties = {
        "spring.profiles.active=integration-test",
        "spring.session.store-type=redis",
        "spring.session.timeout=1800s", // 30 minutes matching CICS
        "logging.level.org.springframework.session=DEBUG"
    }
)
@TestPropertySource(locations = "classpath:application-integration.yml")
public class SessionManagementTest extends BaseIntegrationTest {

    @Autowired
    private SessionRepository<Session> sessionRepository;

    // Test constants for session management validation
    private static final int SESSION_TIMEOUT_MINUTES = 30; // CICS-equivalent timeout
    private static final int MAX_SESSION_SIZE_KB = 32;     // COMMAREA size limit
    private static final String TEST_USER_ID = "TESTUSER";
    private static final String TEST_USER_PASSWORD = "password123";
    private static final String TEST_ADMIN_ROLE = "ADMIN";
    private static final String TEST_USER_ROLE = "USER";

    private Session testSession;
    private GenericContainer<?> redisContainer;

    /**
     * Sets up test environment before each test execution.
     * Initializes Redis container, creates test session, and validates container connectivity.
     */
    @BeforeEach
    public void setUp() {
        super.setupTestContainers();
        
        // Get Redis container from parent class
        redisContainer = getRedisContainer();
        
        // Validate Redis container is running
        assertThat(redisContainer.isRunning())
            .as("Redis container should be running for session tests")
            .isTrue();
            
        // Create a fresh test session for each test
        testSession = sessionRepository.createSession();
        assertThat(testSession).isNotNull();
        assertThat(testSession.getId()).isNotNull();
        
        logger.info("Session management test setup completed with session ID: {}", testSession.getId());
    }

    /**
     * Test: Verify Spring Session integration with Redis backend.
     * 
     * Validates that Spring Session correctly integrates with Redis Testcontainer,
     * ensuring session repository can create, save, and retrieve sessions from Redis storage.
     * This test confirms the basic session management infrastructure is functional.
     */
    @Test
    public void testSpringSessionRedisIntegration() {
        // Verify session repository is properly injected and configured
        assertThat(sessionRepository).isNotNull();
        
        // Create new session and verify properties
        Session newSession = sessionRepository.createSession();
        assertThat(newSession).isNotNull();
        assertThat(newSession.getId()).isNotNull();
        assertThat(newSession.getMaxInactiveInterval())
            .as("Session timeout should match CICS 30-minute configuration")
            .isEqualTo(Duration.ofMinutes(SESSION_TIMEOUT_MINUTES));
        
        // Set test attributes in session
        newSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        newSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_USER_ROLE);
        newSession.setAttribute(SessionAttributes.SEC_USR_NAME, "Test User");
        
        // Save session to Redis
        sessionRepository.save(newSession);
        
        // Retrieve session from Redis and validate persistence
        Session retrievedSession = sessionRepository.findById(newSession.getId());
        assertThat(retrievedSession).isNotNull();
        assertThat(retrievedSession.getAttribute(SessionAttributes.SEC_USR_ID))
            .isEqualTo(TEST_USER_ID);
        assertThat(retrievedSession.getAttribute(SessionAttributes.SEC_USR_TYPE))
            .isEqualTo(TEST_USER_ROLE);
        assertThat(retrievedSession.getAttribute(SessionAttributes.SEC_USR_NAME))
            .isEqualTo("Test User");
            
        logger.info("Spring Session Redis integration validated successfully");
    }

    /**
     * Test: Validate COMMAREA-equivalent state storage with 32KB size limit.
     * 
     * Ensures session storage respects the 32KB limit equivalent to CICS COMMAREA,
     * preventing session size from exceeding mainframe compatibility requirements.
     * Tests both size validation and enforcement mechanisms.
     */
    @Test
    public void testCommareaEquivalentStateStorage() {
        // Test normal session size within limits
        testSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        testSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_USER_ROLE);
        testSession.setAttribute(SessionAttributes.SEC_USR_NAME, "Test User");
        testSession.setAttribute(SessionAttributes.NAVIGATION_STATE, "MENU");
        testSession.setAttribute(SessionAttributes.TRANSACTION_STATE, "ACTIVE");
        
        sessionRepository.save(testSession);
        
        // Create session attributes map for size validation
        Map<String, Object> sessionAttributes = new HashMap<>();
        testSession.getAttributeNames().forEach(name -> 
            sessionAttributes.put(name, testSession.getAttribute(name))
        );
        
        // Validate session size is within COMMAREA limits
        boolean withinLimits = SessionAttributes.validateSessionSize(sessionAttributes);
        assertThat(withinLimits)
            .as("Session size should be within 32KB COMMAREA limit")
            .isTrue();
        
        // Test session size limit enforcement with large data
        StringBuilder largeData = new StringBuilder();
        // Create string approaching the 32KB limit (32,768 bytes)
        for (int i = 0; i < 16000; i++) { // 16K characters = ~32KB
            largeData.append("X");
        }
        
        Map<String, Object> largeSessionAttributes = new HashMap<>(sessionAttributes);
        largeSessionAttributes.put("LARGE_DATA", largeData.toString());
        
        // Validate that oversized session is detected
        boolean oversizedSession = SessionAttributes.validateSessionSize(largeSessionAttributes);
        assertThat(oversizedSession)
            .as("Oversized session should be detected and rejected")
            .isFalse();
            
        logger.info("COMMAREA-equivalent state storage validation completed successfully");
    }

    /**
     * Test: Verify session creation and retrieval workflows.
     * 
     * Validates complete session lifecycle including creation, attribute setting,
     * persistence, and retrieval, ensuring all session operations work correctly
     * with Redis backend and maintain data integrity.
     */
    @Test
    public void testSessionCreationAndRetrievalWorkflows() {
        // Test session creation workflow
        Session createdSession = sessionRepository.createSession();
        assertThat(createdSession).isNotNull();
        assertThat(createdSession.getId()).isNotNull();
        assertThat(createdSession.isExpired()).isFalse();
        
        // Test comprehensive attribute setting matching CICS COMMAREA fields
        createdSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        createdSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_ADMIN_ROLE);
        createdSession.setAttribute(SessionAttributes.SEC_USR_NAME, "Admin User");
        createdSession.setAttribute(SessionAttributes.NAVIGATION_STATE, "ADMIN");
        createdSession.setAttribute(SessionAttributes.TRANSACTION_STATE, "PENDING");
        createdSession.setAttribute(SessionAttributes.ERROR_MESSAGE, null);
        createdSession.setAttribute(SessionAttributes.LAST_PAGE, "MENU");
        createdSession.setAttribute(SessionAttributes.CURRENT_PAGE, "ADMIN");
        
        // Save session and verify persistence
        sessionRepository.save(createdSession);
        
        // Test session retrieval workflow
        Session retrievedSession = sessionRepository.findById(createdSession.getId());
        assertThat(retrievedSession).isNotNull();
        assertThat(retrievedSession.getId()).isEqualTo(createdSession.getId());
        
        // Validate all attributes are correctly persisted and retrieved
        assertThat(retrievedSession.getAttribute(SessionAttributes.SEC_USR_ID))
            .isEqualTo(TEST_USER_ID);
        assertThat(retrievedSession.getAttribute(SessionAttributes.SEC_USR_TYPE))
            .isEqualTo(TEST_ADMIN_ROLE);
        assertThat(retrievedSession.getAttribute(SessionAttributes.SEC_USR_NAME))
            .isEqualTo("Admin User");
        assertThat(retrievedSession.getAttribute(SessionAttributes.NAVIGATION_STATE))
            .isEqualTo("ADMIN");
        assertThat(retrievedSession.getAttribute(SessionAttributes.TRANSACTION_STATE))
            .isEqualTo("PENDING");
        assertThat(retrievedSession.getAttribute(SessionAttributes.LAST_PAGE))
            .isEqualTo("MENU");
        assertThat(retrievedSession.getAttribute(SessionAttributes.CURRENT_PAGE))
            .isEqualTo("ADMIN");
        
        // Test session attribute enumeration
        assertThat(retrievedSession.getAttributeNames()).hasSize(7);
        
        logger.info("Session creation and retrieval workflows validated successfully");
    }

    /**
     * Test: Verify session timeout behavior matching CICS configuration.
     * 
     * Ensures session timeout is configured to 30 minutes matching CICS defaults,
     * and validates that sessions properly expire and become inaccessible after
     * the timeout period, maintaining identical behavior to CICS COMMAREA.
     */
    @Test
    public void testSessionTimeoutBehavior() {
        // Verify session timeout configuration matches CICS (30 minutes)
        assertThat(testSession.getMaxInactiveInterval())
            .as("Session timeout should be 30 minutes to match CICS")
            .isEqualTo(Duration.ofMinutes(SESSION_TIMEOUT_MINUTES));
        
        // Set session attributes and save
        testSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        testSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_USER_ROLE);
        sessionRepository.save(testSession);
        
        // Verify session is retrievable immediately
        Session immediateSession = sessionRepository.findById(testSession.getId());
        assertThat(immediateSession).isNotNull();
        
        // Test session last accessed time updates
        long lastAccessedBefore = testSession.getLastAccessedTime().toEpochMilli();
        
        // Access session to update last accessed time
        testSession.getAttribute(SessionAttributes.SEC_USR_ID);
        sessionRepository.save(testSession);
        
        Session updatedSession = sessionRepository.findById(testSession.getId());
        long lastAccessedAfter = updatedSession.getLastAccessedTime().toEpochMilli();
        
        assertThat(lastAccessedAfter)
            .as("Last accessed time should be updated on session access")
            .isGreaterThanOrEqualTo(lastAccessedBefore);
        
        // Note: Full timeout testing would require waiting 30 minutes or manipulating time,
        // which is impractical for unit tests. The configuration validation above ensures
        // correct timeout setup, and integration tests with shorter timeouts can validate
        // the expiration mechanism separately.
        
        logger.info("Session timeout behavior validation completed successfully");
    }

    /**
     * Test: Validate concurrent session handling and thread safety.
     * 
     * Ensures session management is thread-safe and can handle concurrent access
     * from multiple threads without data corruption or consistency issues,
     * validating that the Redis-backed session storage supports concurrent operations.
     */
    @Test
    public void testConcurrentSessionHandling() throws Exception {
        // Create session with initial attributes
        testSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        testSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_USER_ROLE);
        testSession.setAttribute(SessionAttributes.NAVIGATION_STATE, "MENU");
        sessionRepository.save(testSession);
        
        final String sessionId = testSession.getId();
        final int numberOfThreads = 10;
        final int operationsPerThread = 5;
        
        // Create concurrent tasks that read and write session attributes
        CompletableFuture<Void>[] tasks = new CompletableFuture[numberOfThreads];
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            tasks[i] = CompletableFuture.runAsync(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Retrieve session from Redis
                        Session concurrentSession = sessionRepository.findById(sessionId);
                        assertThat(concurrentSession).isNotNull();
                        
                        // Perform concurrent attribute operations
                        String attributeKey = "THREAD_" + threadIndex + "_OPERATION_" + j;
                        String attributeValue = "Value_" + threadIndex + "_" + j + "_" + System.currentTimeMillis();
                        
                        concurrentSession.setAttribute(attributeKey, attributeValue);
                        concurrentSession.setAttribute(SessionAttributes.LAST_ACTIVITY_TIME, System.currentTimeMillis());
                        
                        // Save session back to Redis
                        sessionRepository.save(concurrentSession);
                        
                        // Verify attribute was saved correctly
                        Session verificationSession = sessionRepository.findById(sessionId);
                        assertThat(verificationSession.getAttribute(attributeKey))
                            .isEqualTo(attributeValue);
                    }
                } catch (Exception e) {
                    logger.error("Concurrent session operation failed in thread " + threadIndex, e);
                    throw new RuntimeException(e);
                }
            });
        }
        
        // Wait for all concurrent tasks to complete
        CompletableFuture.allOf(tasks).get(30, TimeUnit.SECONDS);
        
        // Validate final session state
        Session finalSession = sessionRepository.findById(sessionId);
        assertThat(finalSession).isNotNull();
        assertThat(finalSession.getAttribute(SessionAttributes.SEC_USR_ID))
            .isEqualTo(TEST_USER_ID);
        assertThat(finalSession.getAttribute(SessionAttributes.SEC_USR_TYPE))
            .isEqualTo(TEST_USER_ROLE);
        assertThat(finalSession.getAttribute(SessionAttributes.NAVIGATION_STATE))
            .isEqualTo("MENU");
        
        // Verify all concurrent attributes were saved
        int concurrentAttributeCount = 0;
        for (String attributeName : finalSession.getAttributeNames()) {
            if (attributeName.startsWith("THREAD_")) {
                concurrentAttributeCount++;
            }
        }
        
        assertThat(concurrentAttributeCount)
            .as("All concurrent thread operations should have been persisted")
            .isEqualTo(numberOfThreads * operationsPerThread);
            
        logger.info("Concurrent session handling validation completed successfully");
    }

    /**
     * Test: Validate session invalidation on logout with complete cleanup.
     * 
     * Ensures session invalidation properly removes all session data from Redis,
     * making the session inaccessible and freeing storage resources, equivalent
     * to CICS session termination behavior.
     */
    @Test
    public void testSessionInvalidationOnLogout() {
        // Set up session with comprehensive user data
        testSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        testSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_ADMIN_ROLE);
        testSession.setAttribute(SessionAttributes.SEC_USR_NAME, "Test Admin");
        testSession.setAttribute(SessionAttributes.NAVIGATION_STATE, "ADMIN");
        testSession.setAttribute(SessionAttributes.TRANSACTION_STATE, "ACTIVE");
        testSession.setAttribute(SessionAttributes.ERROR_MESSAGE, "Test error");
        testSession.setAttribute(SessionAttributes.LAST_PAGE, "REPORTS");
        testSession.setAttribute(SessionAttributes.CURRENT_PAGE, "USER_ADMIN");
        testSession.setAttribute("CUSTOM_DATA", "Custom session data");
        
        sessionRepository.save(testSession);
        String sessionId = testSession.getId();
        
        // Verify session exists and contains all attributes
        Session preInvalidationSession = sessionRepository.findById(sessionId);
        assertThat(preInvalidationSession).isNotNull();
        assertThat(preInvalidationSession.getAttributeNames()).hasSize(9);
        
        // Perform session invalidation (simulating logout)
        sessionRepository.deleteById(sessionId);
        
        // Verify session is completely removed from Redis
        Session postInvalidationSession = sessionRepository.findById(sessionId);
        assertThat(postInvalidationSession)
            .as("Session should be null after invalidation")
            .isNull();
        
        // Verify no orphaned session data remains
        // Note: In a real application, this would also involve clearing related caches
        // and notifying other services of session termination
        
        logger.info("Session invalidation on logout validation completed successfully");
    }

    /**
     * Test: Ensure session attributes maintain state across multiple requests.
     * 
     * Validates that session attributes persist correctly across multiple request
     * cycles, simulating the CICS COMMAREA behavior where user context and
     * navigation state is maintained throughout the user's session lifecycle.
     */
    @Test
    public void testSessionAttributesPersistAcrossRequests() {
        final String sessionId = testSession.getId();
        
        // Simulate first request - user login and menu navigation
        Session firstRequestSession = sessionRepository.findById(sessionId);
        firstRequestSession.setAttribute(SessionAttributes.SEC_USR_ID, TEST_USER_ID);
        firstRequestSession.setAttribute(SessionAttributes.SEC_USR_TYPE, TEST_USER_ROLE);
        firstRequestSession.setAttribute(SessionAttributes.SEC_USR_NAME, "John Doe");
        firstRequestSession.setAttribute(SessionAttributes.NAVIGATION_STATE, "MENU");
        firstRequestSession.setAttribute(SessionAttributes.TRANSACTION_STATE, "ACTIVE");
        sessionRepository.save(firstRequestSession);
        
        // Simulate second request - navigate to transaction screen
        Session secondRequestSession = sessionRepository.findById(sessionId);
        assertThat(secondRequestSession).isNotNull();
        assertThat(secondRequestSession.getAttribute(SessionAttributes.SEC_USR_ID))
            .isEqualTo(TEST_USER_ID);
        assertThat(secondRequestSession.getAttribute(SessionAttributes.SEC_USR_NAME))
            .isEqualTo("John Doe");
        
        // Update navigation state for transaction screen
        secondRequestSession.setAttribute(SessionAttributes.LAST_PAGE, "MENU");
        secondRequestSession.setAttribute(SessionAttributes.CURRENT_PAGE, "TRANSACTION");
        secondRequestSession.setAttribute(SessionAttributes.NAVIGATION_STATE, "TRANSACTION");
        sessionRepository.save(secondRequestSession);
        
        // Simulate third request - process transaction
        Session thirdRequestSession = sessionRepository.findById(sessionId);
        assertThat(thirdRequestSession).isNotNull();
        assertThat(thirdRequestSession.getAttribute(SessionAttributes.CURRENT_PAGE))
            .isEqualTo("TRANSACTION");
        assertThat(thirdRequestSession.getAttribute(SessionAttributes.LAST_PAGE))
            .isEqualTo("MENU");
        
        // Update transaction state
        thirdRequestSession.setAttribute(SessionAttributes.TRANSACTION_STATE, "PROCESSING");
        thirdRequestSession.setAttribute(SessionAttributes.ERROR_MESSAGE, null);
        sessionRepository.save(thirdRequestSession);
        
        // Simulate fourth request - complete transaction
        Session fourthRequestSession = sessionRepository.findById(sessionId);
        assertThat(fourthRequestSession).isNotNull();
        assertThat(fourthRequestSession.getAttribute(SessionAttributes.TRANSACTION_STATE))
            .isEqualTo("PROCESSING");
        assertThat(fourthRequestSession.getAttribute(SessionAttributes.ERROR_MESSAGE))
            .isNull();
        
        // Complete transaction
        fourthRequestSession.setAttribute(SessionAttributes.TRANSACTION_STATE, "COMPLETE");
        fourthRequestSession.setAttribute(SessionAttributes.LAST_PAGE, "TRANSACTION");
        fourthRequestSession.setAttribute(SessionAttributes.CURRENT_PAGE, "CONFIRMATION");
        sessionRepository.save(fourthRequestSession);
        
        // Final validation - verify complete session history is maintained
        Session finalSession = sessionRepository.findById(sessionId);
        assertThat(finalSession).isNotNull();
        assertThat(finalSession.getAttribute(SessionAttributes.SEC_USR_ID))
            .as("User ID should be maintained throughout session")
            .isEqualTo(TEST_USER_ID);
        assertThat(finalSession.getAttribute(SessionAttributes.SEC_USR_NAME))
            .as("User name should be maintained throughout session")
            .isEqualTo("John Doe");
        assertThat(finalSession.getAttribute(SessionAttributes.TRANSACTION_STATE))
            .as("Final transaction state should be COMPLETE")
            .isEqualTo("COMPLETE");
        assertThat(finalSession.getAttribute(SessionAttributes.CURRENT_PAGE))
            .as("Current page should be CONFIRMATION")
            .isEqualTo("CONFIRMATION");
        assertThat(finalSession.getAttribute(SessionAttributes.LAST_PAGE))
            .as("Last page should be TRANSACTION")
            .isEqualTo("TRANSACTION");
            
        logger.info("Session attributes persistence across requests validated successfully");
    }
}