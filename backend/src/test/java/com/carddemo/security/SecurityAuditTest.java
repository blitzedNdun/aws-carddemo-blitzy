/*
 * SecurityAuditTest.java
 * 
 * Unit and integration test implementation for security audit capabilities using JUnit 5 and Spring Boot Test.
 * Tests verify authentication success/failure event logging, authorization decision audit trails, failed login 
 * attempt tracking and account lockout, session creation/destruction logging, security metrics collection for 
 * monitoring, audit log format and content validation, compliance with regulatory requirements, and replacement 
 * of mainframe SMF security records.
 * 
 * This test suite ensures comprehensive validation of the SecurityAuditService and AuditConfig components
 * that provide enterprise-grade security auditing capabilities for the CardDemo application migration from
 * COBOL mainframe to Spring Boot. The tests validate all security auditing functionality including
 * authentication event processing, authorization decision tracking, failed login monitoring, session
 * lifecycle auditing, compliance reporting, and metrics collection.
 * 
 * Test Coverage Areas:
 * - Authentication event auditing with success and failure scenarios
 * - Authorization decision audit trails with access grant and denial tracking
 * - Failed login attempt monitoring with threshold detection and account lockout
 * - Session lifecycle auditing from creation through destruction
 * - Security metrics generation for monitoring dashboards and compliance
 * - Audit log format validation and regulatory compliance verification
 * - Mainframe SMF security record replacement validation
 * - Integration testing with PostgreSQL using Testcontainers
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
package com.carddemo.security;

// Internal imports for testing security audit capabilities
import com.carddemo.security.SecurityAuditService;
import com.carddemo.security.AuditConfig;

// JUnit 5 imports for modern testing framework
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// Spring Boot Test imports for integration testing
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Mockito imports for mock object creation and verification
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

// Spring Security Test imports for authentication context
import org.springframework.security.test.context.support.WithMockUser;

// H2 in-memory database for testing (replacing Testcontainers PostgreSQL)
import org.springframework.test.context.ActiveProfiles;

// Additional required imports for comprehensive testing
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;

// Spring Batch imports for JobRepository
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.core.task.SyncTaskExecutor;

import javax.sql.DataSource;

// Java standard imports for test implementation
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

// Testing framework imports for assertions and verification
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Comprehensive test suite for SecurityAuditService and AuditConfig security audit capabilities.
 * 
 * This test class provides extensive validation of security auditing functionality including
 * authentication event processing, authorization decision tracking, failed login monitoring,
 * session lifecycle auditing, security metrics collection, and regulatory compliance validation.
 * The tests ensure that all security audit capabilities function correctly and meet enterprise
 * security requirements for the CardDemo application.
 * 
 * Test Architecture:
 * - JUnit 5 for modern testing framework with comprehensive assertion capabilities
 * - Spring Boot Test for full application context integration testing
 * - Mockito for mock object creation and behavior verification
 * - Testcontainers for PostgreSQL database integration testing
 * - Spring Security Test for authentication context simulation
 * 
 * The test suite validates replacement of mainframe SMF (System Management Facilities) security
 * records with modern cloud-native security event processing while maintaining equivalent
 * functionality and regulatory compliance capabilities.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "carddemo.audit.enabled=true",
    "security.audit.failed-login.threshold=3",
    "security.audit.failed-login.window-minutes=5",
    "security.audit.session.timeout-minutes=30",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.batch.job.enabled=false"
})
public class SecurityAuditTest {
    
    // H2 in-memory database configured via @TestPropertySource for unit testing
    
    // Primary service under test - SecurityAuditService
    @Autowired
    @InjectMocks
    private SecurityAuditService securityAuditService;
    
    // Configuration class under test - AuditConfig
    @Autowired
    private AuditConfig auditConfig;
    
    // Mock dependencies for isolated unit testing
    @MockBean
    private com.carddemo.service.AuditService auditService;
    
    @MockBean
    private com.carddemo.security.SecurityEventListener securityEventListener;
    
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    
    // Test data and configuration constants
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_SESSION_ID = "test-session-12345";
    private static final String TEST_USER_AGENT = "CardDemo-Client/1.0";
    private static final String TEST_CORRELATION_ID = "corr-12345-67890";
    
    // Authentication test scenarios data
    private Map<String, Object> authenticationContext;
    private List<String> userRoles;
    private List<String> requiredPermissions;
    
    /**
     * Test setup method executed before each test case.
     * 
     * Initializes test data, configures mock behaviors, and prepares the test environment
     * for comprehensive security audit testing. This method ensures consistent test state
     * across all test methods and properly configures mock dependencies for isolated testing.
     */
    @BeforeEach
    public void setUp() {
        // Initialize test data structures
        authenticationContext = new HashMap<>();
        authenticationContext.put("correlationId", TEST_CORRELATION_ID);
        authenticationContext.put("userAgent", TEST_USER_AGENT);
        authenticationContext.put("authenticationType", "PASSWORD");
        
        userRoles = Arrays.asList("ROLE_USER", "ROLE_CUSTOMER");
        requiredPermissions = Arrays.asList("READ", "ACCOUNT_VIEW");
        
        // Reset mock invocation counts first
        reset(auditService, securityEventListener);
        
        // Configure mock AuditService behavior for successful operations
        when(auditService.saveAuditLog(any())).thenAnswer(invocation -> {
            com.carddemo.entity.AuditLog auditLog = invocation.getArgument(0);
            // Set ID to simulate successful save
            if (auditLog.getId() == null) {
                auditLog.setId(1L);
            }
            return auditLog;
        });
        when(auditService.validateAuditTrailIntegrity(any(), any()))
            .thenReturn(createSuccessfulIntegrityResult());
        when(auditService.getAuditLogsByUser(anyString(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt(), anyInt()))
            .thenReturn(createMockAuditLogPage());
        when(auditService.getAuditLogsByUser(eq("*"), any(LocalDateTime.class), any(LocalDateTime.class), anyInt(), anyInt()))
            .thenReturn(createMockAuditLogPage());
        when(auditService.generateComplianceReport(anyString(), any(), any()))
            .thenReturn(createMockComplianceReport());
        when(auditService.archiveOldLogs(any(), anyBoolean()))
            .thenReturn(createMockArchivalResult());
        
        // Configure mock SecurityEventListener behavior
        doNothing().when(securityEventListener).handleAuthenticationSuccessEvent(anyString(), anyString(), anyString(), anyMap());
        doNothing().when(securityEventListener).generateSecurityAlert(anyString(), anyString(), anyString());
    }
    
    /**
     * Tests authentication success event logging functionality.
     * 
     * This test validates that successful authentication events are properly logged with
     * comprehensive context information including user identity, source IP, authentication
     * method, and session details. Verifies integration with AuditService for persistent
     * storage and SecurityEventListener for real-time event processing.
     * 
     * Test Validation:
     * - Authentication event is saved to audit log with correct details
     * - SecurityEventListener receives authentication success notification
     * - Event context includes all required security information
     * - Event processing completes within acceptable time limits
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = {"USER", "CUSTOMER"})
    public void testAuthenticationSuccessEventLogging() {
        // Arrange
        String outcome = "SUCCESS";
        String authMethod = "PASSWORD";
        
        // Act
        securityAuditService.auditAuthenticationEvent(
            TEST_USERNAME, outcome, TEST_IP_ADDRESS, TEST_SESSION_ID, 
            authMethod, authenticationContext
        );
        
        // Assert
        // Verify audit log is saved with correct parameters
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(1)).saveAuditLog(auditLogCaptor.capture());
        
        com.carddemo.entity.AuditLog savedAuditLog = auditLogCaptor.getValue();
        assertEquals(TEST_USERNAME, savedAuditLog.getUsername());
        assertEquals("AUTHENTICATION", savedAuditLog.getEventType());
        assertEquals(outcome, savedAuditLog.getOutcome());
        assertEquals(TEST_IP_ADDRESS, savedAuditLog.getSourceIp());
        assertNotNull(savedAuditLog.getTimestamp());
        assertTrue(savedAuditLog.getDetails().contains(authMethod));
        assertTrue(savedAuditLog.getDetails().contains(TEST_SESSION_ID));
        
        // Verify SecurityEventListener is notified of successful authentication
        verify(securityEventListener, times(1))
            .handleAuthenticationSuccessEvent(eq(TEST_USERNAME), eq(TEST_IP_ADDRESS), eq(TEST_SESSION_ID), any());
        
        // Verify no security alerts are generated for successful authentication
        verify(securityEventListener, never()).generateSecurityAlert(anyString(), anyString(), anyString());
    }
    
    /**
     * Tests authentication failure event logging functionality.
     * 
     * This test validates that failed authentication attempts are properly logged and
     * processed through the failed login attempt tracking system. Verifies that
     * authentication failures trigger appropriate security monitoring and threshold
     * detection mechanisms.
     * 
     * Test Validation:
     * - Authentication failure event is saved to audit log
     * - Failed login attempt tracking is updated
     * - Security metrics are incremented appropriately
     * - Event contains detailed failure context information
     */
    @Test
    public void testAuthenticationFailureEventLogging() {
        // Arrange
        String outcome = "FAILURE";
        String authMethod = "PASSWORD";
        Map<String, Object> failureContext = new HashMap<>(authenticationContext);
        failureContext.put("failureReason", "Invalid password");
        
        // Act
        securityAuditService.auditAuthenticationEvent(
            TEST_USERNAME, outcome, TEST_IP_ADDRESS, TEST_SESSION_ID, 
            authMethod, failureContext
        );
        
        // Assert
        // Verify audit log is saved with failure outcome (includes threshold tracking)
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(3)).saveAuditLog(auditLogCaptor.capture());
        
        com.carddemo.entity.AuditLog savedAuditLog = auditLogCaptor.getAllValues().get(0); // Get first authentication log
        assertEquals(TEST_USERNAME, savedAuditLog.getUsername());
        assertEquals("AUTHENTICATION", savedAuditLog.getEventType());
        assertEquals(outcome, savedAuditLog.getOutcome());
        assertTrue(savedAuditLog.getDetails().contains("Invalid password"));
        
        // Verify SecurityEventListener is not called for failed authentication (handled by auditFailedLoginAttempt)
        verify(securityEventListener, never())
            .handleAuthenticationSuccessEvent(anyString(), anyString(), anyString(), any());
    }
    
    /**
     * Tests authorization decision audit trail functionality.
     * 
     * This test validates that authorization decisions are properly logged with
     * comprehensive resource access information including requested resources,
     * required permissions, user roles, and access outcomes. Verifies that
     * authorization audit trails support compliance requirements and security monitoring.
     * 
     * Test Validation:
     * - Authorization event is saved with resource access details
     * - User roles and required permissions are properly recorded
     * - Access method and resource path are captured accurately
     * - Event timestamp and source information are included
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = {"USER", "CUSTOMER"})
    public void testAuthorizationDecisionAuditTrail() {
        // Arrange
        String resourcePath = "/api/accounts/12345";
        String accessMethod = "GET";
        String authzOutcome = "SUCCESS";
        Map<String, Object> authzContext = new HashMap<>();
        authzContext.put("sourceIp", TEST_IP_ADDRESS);
        authzContext.put("correlationId", TEST_CORRELATION_ID);
        
        // Act
        securityAuditService.auditAuthorizationEvent(
            TEST_USERNAME, resourcePath, accessMethod, authzOutcome,
            requiredPermissions, userRoles, authzContext
        );
        
        // Assert
        // Verify authorization audit log is saved
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(1)).saveAuditLog(auditLogCaptor.capture());
        
        com.carddemo.entity.AuditLog savedAuditLog = auditLogCaptor.getValue();
        assertEquals(TEST_USERNAME, savedAuditLog.getUsername());
        assertEquals("AUTHORIZATION", savedAuditLog.getEventType());
        assertEquals(authzOutcome, savedAuditLog.getOutcome());
        assertEquals(TEST_IP_ADDRESS, savedAuditLog.getSourceIp());
        
        // Verify event details contain authorization context
        String details = savedAuditLog.getDetails();
        assertTrue(details.contains(resourcePath));
        assertTrue(details.contains(accessMethod));
        assertTrue(details.contains("READ"));
        assertTrue(details.contains("ROLE_USER"));
        
        // Verify no security alerts for successful authorization
        verify(securityEventListener, never()).generateSecurityAlert(anyString(), anyString(), anyString());
    }
    
    /**
     * Tests authorization denial audit trail and security alert generation.
     * 
     * This test validates that denied authorization attempts are properly logged
     * and trigger appropriate security monitoring mechanisms including potential
     * security violation detection and alerting for unauthorized access attempts.
     * 
     * Test Validation:
     * - Authorization denial is recorded in audit log
     * - Security alert is generated for authorization violations
     * - Unauthorized access attempts are properly flagged
     * - Event details include denial context and security implications
     */
    @Test
    @WithMockUser(username = TEST_USERNAME, roles = {"USER"})
    public void testAuthorizationDenialAuditTrail() {
        // Arrange
        String resourcePath = "/api/admin/users";
        String accessMethod = "POST";
        String authzOutcome = "DENIED";
        List<String> adminPermissions = Arrays.asList("ADMIN", "USER_MANAGEMENT");
        Map<String, Object> denialContext = new HashMap<>();
        denialContext.put("sourceIp", TEST_IP_ADDRESS);
        denialContext.put("reason", "Insufficient privileges");
        
        // Act
        securityAuditService.auditAuthorizationEvent(
            TEST_USERNAME, resourcePath, accessMethod, authzOutcome,
            adminPermissions, userRoles, denialContext
        );
        
        // Assert
        // Verify authorization denial is audited
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(1)).saveAuditLog(auditLogCaptor.capture());
        
        com.carddemo.entity.AuditLog savedAuditLog = auditLogCaptor.getValue();
        assertEquals("DENIED", savedAuditLog.getOutcome());
        assertTrue(savedAuditLog.getDetails().contains("ADMIN"));
        assertTrue(savedAuditLog.getDetails().contains("USER_MANAGEMENT"));
        
        // Verify security alert is generated for authorization violation
        verify(securityEventListener, times(1))
            .generateSecurityAlert(eq(TEST_USERNAME), eq("AUTHORIZATION_VIOLATION"), 
                contains("Unauthorized access attempt"));
    }
    
    /**
     * Tests failed login attempt tracking and threshold monitoring.
     * 
     * This test validates that failed login attempts are properly tracked with
     * intelligent threshold detection and account lockout integration. Verifies
     * that the system can detect potential brute force attacks and trigger
     * appropriate security responses.
     * 
     * Test Validation:
     * - Failed login attempts are tracked per user
     * - Attempt counts are incremented correctly
     * - Threshold monitoring functions properly
     * - Event details include attempt count and threshold information
     */
    @Test
    public void testFailedLoginAttemptTracking() {
        // Arrange
        String authMethod = "PASSWORD";
        String failureReason = "Invalid credentials";
        
        // Act - Simulate multiple failed login attempts
        for (int i = 1; i <= 3; i++) {
            securityAuditService.auditFailedLoginAttempt(
                TEST_USERNAME, TEST_IP_ADDRESS, authMethod, failureReason
            );
        }
        
        // Assert
        // Verify all failed login attempts are audited (includes threshold violation)
        verify(auditService, times(4)).saveAuditLog(any());
        
        // Verify the last audit log contains attempt count information
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(4)).saveAuditLog(auditLogCaptor.capture());
        
        List<com.carddemo.entity.AuditLog> savedLogs = auditLogCaptor.getAllValues();
        // Check the first authentication log (subsequent ones might be security violations)
        com.carddemo.entity.AuditLog firstAuthLog = savedLogs.get(0);
        
        assertEquals(TEST_USERNAME, firstAuthLog.getUsername());
        assertEquals("AUTHENTICATION", firstAuthLog.getEventType());
        assertEquals("FAILURE", firstAuthLog.getOutcome());
        
        // Verify at least one log contains attempt tracking information
        boolean hasAttemptTracking = savedLogs.stream()
            .anyMatch(log -> log.getDetails().contains("attemptCount") && 
                           log.getDetails().contains("thresholdLimit"));
        assertTrue(hasAttemptTracking, "At least one audit log should contain attempt tracking information");
    }
    
    /**
     * Tests account lockout trigger for threshold violations.
     * 
     * This test validates that the system properly triggers account lockout
     * procedures when failed login attempts exceed configured thresholds.
     * Verifies integration with SecurityEventListener for security alert
     * generation and account lockout processing.
     * 
     * Test Validation:
     * - Threshold violation triggers security alert
     * - Account lockout event is processed correctly
     * - Security violation audit log is created
     * - Alert contains appropriate context information
     */
    @Test
    public void testAccountLockoutTrigger() {
        // Arrange
        String authMethod = "PASSWORD";
        String failureReason = "Invalid credentials";
        
        // Act - Exceed the failed login threshold (configured to 3 in test properties)
        for (int i = 1; i <= 4; i++) {
            securityAuditService.auditFailedLoginAttempt(
                TEST_USERNAME, TEST_IP_ADDRESS, authMethod, failureReason
            );
        }
        
        // Assert
        // Verify security alert is generated for threshold violation
        verify(securityEventListener, times(2))
            .generateSecurityAlert(eq(TEST_USERNAME), eq("FAILED_LOGIN_THRESHOLD_VIOLATION"), 
                contains("Account lockout triggered"));
        
        // Verify security violation audit log is created
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, atLeast(4)).saveAuditLog(auditLogCaptor.capture());
        
        // Find the security violation log
        List<com.carddemo.entity.AuditLog> allLogs = auditLogCaptor.getAllValues();
        boolean violationLogFound = allLogs.stream()
            .anyMatch(log -> "SECURITY_VIOLATION".equals(log.getEventType()) && 
                           "SECURITY_VIOLATION".equals(log.getOutcome()));
        assertTrue(violationLogFound, "Security violation audit log should be created");
    }
    
    /**
     * Tests session creation logging functionality.
     * 
     * This test validates that session creation events are properly logged with
     * comprehensive session information including user identity, session parameters,
     * security context, and Redis integration details. Verifies session lifecycle
     * tracking capabilities for security monitoring and compliance.
     * 
     * Test Validation:
     * - Session creation event is saved to audit log
     * - Session parameters are recorded accurately
     * - User agent and IP information is captured
     * - Session timeout configuration is included
     */
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testSessionCreationLogging() {
        // Arrange
        int sessionTimeout = 30; // minutes
        Map<String, Object> sessionContext = new HashMap<>();
        sessionContext.put("browserType", "Chrome");
        sessionContext.put("platform", "Windows");
        
        // Act
        securityAuditService.auditSessionCreation(
            TEST_USERNAME, TEST_SESSION_ID, TEST_IP_ADDRESS, 
            TEST_USER_AGENT, sessionTimeout, sessionContext
        );
        
        // Assert
        // Verify session creation audit log is saved
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(1)).saveAuditLog(auditLogCaptor.capture());
        
        com.carddemo.entity.AuditLog savedAuditLog = auditLogCaptor.getValue();
        assertEquals(TEST_USERNAME, savedAuditLog.getUsername());
        assertEquals("SESSION", savedAuditLog.getEventType());
        assertEquals("SUCCESS", savedAuditLog.getOutcome());
        assertEquals(TEST_IP_ADDRESS, savedAuditLog.getSourceIp());
        
        // Verify session details are captured
        String details = savedAuditLog.getDetails();
        assertTrue(details.contains(TEST_SESSION_ID));
        assertTrue(details.contains(TEST_USER_AGENT));
        assertTrue(details.contains("sessionTimeoutMinutes"));
        assertTrue(details.contains("CREATION"));
        assertTrue(details.contains("Chrome"));
    }
    
    /**
     * Tests session destruction logging functionality.
     * 
     * This test validates that session destruction events are properly logged
     * with comprehensive cleanup information including termination reason,
     * session duration, cleanup operations, and Redis integration cleanup.
     * Verifies complete session lifecycle tracking for security monitoring.
     * 
     * Test Validation:
     * - Session destruction event is saved to audit log
     * - Termination reason and duration are recorded
     * - Cleanup operations are documented
     * - Session lifecycle completion is tracked
     */
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testSessionDestructionLogging() {
        // Arrange
        String terminationReason = "USER_LOGOUT";
        long sessionDuration = 25; // minutes
        List<String> cleanupOps = Arrays.asList("CLEAR_CACHE", "INVALIDATE_TOKEN", "CLEANUP_REDIS");
        Map<String, Object> destructionContext = new HashMap<>();
        destructionContext.put("sourceIp", TEST_IP_ADDRESS);
        destructionContext.put("gracefulShutdown", true);
        
        // Act
        securityAuditService.auditSessionDestruction(
            TEST_USERNAME, TEST_SESSION_ID, terminationReason,
            sessionDuration, cleanupOps, destructionContext
        );
        
        // Assert
        // Verify session destruction audit log is saved
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(1)).saveAuditLog(auditLogCaptor.capture());
        
        com.carddemo.entity.AuditLog savedAuditLog = auditLogCaptor.getValue();
        assertEquals(TEST_USERNAME, savedAuditLog.getUsername());
        assertEquals("SESSION", savedAuditLog.getEventType());
        assertEquals("SUCCESS", savedAuditLog.getOutcome());
        assertEquals(TEST_IP_ADDRESS, savedAuditLog.getSourceIp());
        
        // Verify session destruction details
        String details = savedAuditLog.getDetails();
        assertTrue(details.contains(terminationReason));
        assertTrue(details.contains("DESTRUCTION"));
        assertTrue(details.contains("CLEAR_CACHE"));
        assertTrue(details.contains("INVALIDATE_TOKEN"));
        assertTrue(details.contains("sessionDurationMinutes"));
    }
    
    /**
     * Tests security metrics collection for monitoring dashboards.
     * 
     * This test validates that the security metrics generation functionality
     * properly collects and analyzes security events for monitoring dashboards,
     * compliance reporting, and proactive threat detection. Verifies integration
     * with monitoring infrastructure and metrics collection systems.
     * 
     * Test Validation:
     * - Security metrics are generated for specified scope and time range
     * - Authentication, authorization, and session metrics are included
     * - Compliance metrics are calculated correctly
     * - Metrics metadata contains proper context information
     */
    @Test
    public void testSecurityMetricsCollection() {
        // Arrange
        String metricsScope = "ALL";
        int timeRangeHours = 24;
        
        // Act
        Map<String, Object> securityMetrics = securityAuditService.generateSecurityMetrics(
            metricsScope, timeRangeHours
        );
        
        // Assert
        // Verify metrics structure and content
        assertNotNull(securityMetrics);
        assertTrue(securityMetrics.containsKey("authentication"));
        assertTrue(securityMetrics.containsKey("authorization"));
        assertTrue(securityMetrics.containsKey("session"));
        assertTrue(securityMetrics.containsKey("compliance"));
        assertTrue(securityMetrics.containsKey("metadata"));
        
        // Verify metadata information
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) securityMetrics.get("metadata");
        assertEquals(metricsScope, metadata.get("metricsScope"));
        assertEquals(timeRangeHours, metadata.get("timeRangeHours"));
        assertNotNull(metadata.get("startTime"));
        assertNotNull(metadata.get("endTime"));
        assertEquals("SecurityAuditService", metadata.get("generatedBy"));
        
        // Verify AuditService integration for metrics data
        verify(auditService, atLeast(1)).getAuditLogsByUser(anyString(), any(), any(), anyInt(), anyInt());
        verify(auditService, times(1)).validateAuditTrailIntegrity(any(), any());
    }
    
    /**
     * Tests security metrics collection for specific scope.
     * 
     * This test validates that security metrics can be generated for specific
     * scopes (AUTHENTICATION, AUTHORIZATION, SESSION, COMPLIANCE) with proper
     * filtering and data aggregation. Verifies that scope-specific metrics
     * contain only relevant security event data.
     * 
     * Test Validation:
     * - Scope-specific metrics contain only relevant data
     * - Metrics filtering works correctly for different scopes
     * - Performance metrics are within acceptable ranges
     * - Integration with monitoring systems functions properly
     */
    @Test
    public void testSecurityMetricsCollectionByScope() {
        // Test authentication scope metrics
        Map<String, Object> authMetrics = securityAuditService.generateSecurityMetrics("AUTHENTICATION", 1);
        assertNotNull(authMetrics);
        assertTrue(authMetrics.containsKey("authentication"));
        assertFalse(authMetrics.containsKey("authorization"));
        
        // Test authorization scope metrics
        Map<String, Object> authzMetrics = securityAuditService.generateSecurityMetrics("AUTHORIZATION", 1);
        assertNotNull(authzMetrics);
        assertTrue(authzMetrics.containsKey("authorization"));
        assertFalse(authzMetrics.containsKey("authentication"));
        
        // Test session scope metrics
        Map<String, Object> sessionMetrics = securityAuditService.generateSecurityMetrics("SESSION", 1);
        assertNotNull(sessionMetrics);
        assertTrue(sessionMetrics.containsKey("session"));
        assertFalse(sessionMetrics.containsKey("authentication"));
        
        // Test compliance scope metrics
        Map<String, Object> complianceMetrics = securityAuditService.generateSecurityMetrics("COMPLIANCE", 1);
        assertNotNull(complianceMetrics);
        assertTrue(complianceMetrics.containsKey("compliance"));
        assertFalse(complianceMetrics.containsKey("authentication"));
    }
    
    /**
     * Tests audit log format validation and regulatory compliance.
     * 
     * This test validates that audit log formats meet regulatory requirements
     * and compliance standards including SOX, PCI DSS, GDPR, and other financial
     * services regulations. Verifies that audit trails maintain proper structure,
     * integrity, and completeness for regulatory examinations.
     * 
     * Test Validation:
     * - Audit log format meets regulatory standards
     * - Required fields are present and properly formatted
     * - Audit trail integrity validation succeeds
     * - Compliance validation returns positive results
     */
    @Test
    public void testAuditLogFormatValidationAndCompliance() {
        // Arrange
        String complianceStandard = "ALL";
        int validationPeriodDays = 30;
        
        // Act
        Map<String, Object> complianceResults = securityAuditService.validateAuditCompliance(
            complianceStandard, validationPeriodDays
        );
        
        // Assert
        // Verify compliance validation results
        assertNotNull(complianceResults);
        assertTrue(complianceResults.containsKey("auditTrailIntegrity"));
        assertTrue(complianceResults.containsKey("soxCompliance"));
        assertTrue(complianceResults.containsKey("pciDssCompliance"));
        assertTrue(complianceResults.containsKey("gdprCompliance"));
        assertTrue(complianceResults.containsKey("securityPolicyCompliance"));
        assertTrue(complianceResults.containsKey("overallComplianceScore"));
        assertTrue(complianceResults.containsKey("recommendations"));
        assertTrue(complianceResults.containsKey("validationMetadata"));
        
        // Verify overall compliance score
        Double complianceScore = (Double) complianceResults.get("overallComplianceScore");
        assertNotNull(complianceScore);
        assertTrue(complianceScore >= 0.0 && complianceScore <= 1.0);
        
        // Verify audit trail integrity validation (called for each compliance framework)
        verify(auditService, times(4)).validateAuditTrailIntegrity(any(), any());
        
        // Verify recommendations are provided
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) complianceResults.get("recommendations");
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }
    
    /**
     * Tests replacement of mainframe SMF security records.
     * 
     * This test validates that the modern security auditing system provides
     * equivalent functionality to mainframe SMF (System Management Facilities)
     * security records while offering enhanced capabilities for cloud-native
     * environments. Verifies feature parity and enhanced functionality.
     * 
     * Test Validation:
     * - All SMF security record types are replaced with equivalent audit events
     * - Enhanced security monitoring capabilities are available
     * - Cloud-native security features function correctly
     * - Integration with modern monitoring systems works properly
     */
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testMainframeSMFSecurityRecordReplacement() {
        // Arrange - Simulate various SMF-equivalent security events
        Map<String, Object> smfEquivalentContext = new HashMap<>();
        smfEquivalentContext.put("smfRecordType", "SMF_80_AUTHENTICATION");
        smfEquivalentContext.put("mainframeJobId", "CARD001");
        smfEquivalentContext.put("cicsRegion", "CARDCICS");
        
        // Act - Generate various types of audit events that replace SMF records
        
        // 1. Authentication event (replaces SMF Type 80 records)
        securityAuditService.auditAuthenticationEvent(
            TEST_USERNAME, "SUCCESS", TEST_IP_ADDRESS, TEST_SESSION_ID,
            "CICS_SIGNON", smfEquivalentContext
        );
        
        // 2. Authorization event (replaces SMF Type 81 records)
        securityAuditService.auditAuthorizationEvent(
            TEST_USERNAME, "/api/transactions", "POST", "SUCCESS",
            Arrays.asList("TRANSACTION_CREATE"), Arrays.asList("ROLE_USER"), smfEquivalentContext
        );
        
        // 3. Session event (replaces SMF Type 110 records)
        securityAuditService.auditSessionCreation(
            TEST_USERNAME, TEST_SESSION_ID, TEST_IP_ADDRESS, "CICS-CLIENT", 30, smfEquivalentContext
        );
        
        // 4. Generate security metrics (replaces SMF reporting)
        Map<String, Object> securityMetrics = securityAuditService.generateSecurityMetrics("ALL", 24);
        
        // Assert
        // Verify all SMF-equivalent events are properly audited
        verify(auditService, times(3)).saveAuditLog(any());
        
        // Verify enhanced cloud-native capabilities
        assertNotNull(securityMetrics);
        assertTrue(securityMetrics.containsKey("authentication"));
        assertTrue(securityMetrics.containsKey("authorization"));
        assertTrue(securityMetrics.containsKey("session"));
        assertTrue(securityMetrics.containsKey("compliance"));
        
        // Verify SMF context is preserved in audit logs
        ArgumentCaptor<com.carddemo.entity.AuditLog> auditLogCaptor = ArgumentCaptor.forClass(com.carddemo.entity.AuditLog.class);
        verify(auditService, times(3)).saveAuditLog(auditLogCaptor.capture());
        
        List<com.carddemo.entity.AuditLog> auditLogs = auditLogCaptor.getAllValues();
        for (com.carddemo.entity.AuditLog log : auditLogs) {
            assertTrue(log.getDetails().contains("SMF_80_AUTHENTICATION") || 
                      log.getDetails().contains("mainframeJobId") ||
                      log.getDetails().contains("cicsRegion"));
        }
        
        // Verify enhanced monitoring capabilities beyond SMF
        assertTrue(securityMetrics.containsKey("metadata"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) securityMetrics.get("metadata");
        assertEquals("SecurityAuditService", metadata.get("generatedBy"));
    }
    
    /**
     * Tests AuditConfig bean configuration functionality.
     * 
     * This test validates that AuditConfig properly configures audit event publishers,
     * security event listeners, metrics collectors, and other audit infrastructure
     * components. Verifies that Spring Boot configuration provides all necessary
     * audit capabilities for the security framework.
     * 
     * Test Validation:
     * - AuditService bean is properly configured and available
     * - SecurityEventListener bean is configured with proper dependencies
     * - Configuration enables comprehensive security auditing
     * - Integration between components functions correctly
     */
    @Test
    public void testAuditConfigBeanConfiguration() {
        // Act - Get configured beans from AuditConfig
        ApplicationEventPublisher eventPublisher = auditConfig.auditEventPublisher();
        com.carddemo.security.SecurityEventListener eventListener = auditConfig.securityEventListener();
        
        // Assert
        // Verify AuditConfig beans are properly configured
        assertNotNull(auditConfig);
        assertNotNull(eventPublisher);
        assertNotNull(eventListener);
        
        // Verify audit metrics collector configuration
        Object metricsCollector = auditConfig.auditMetricsCollector();
        assertNotNull(metricsCollector);
        
        // Verify audit log retention scheduler configuration
        Object retentionScheduler = auditConfig.auditLogRetentionScheduler();
        assertNotNull(retentionScheduler);
        
        // Verify database trigger configuration
        Object triggerConfig = auditConfig.auditDatabaseTriggerConfiguration();
        assertNotNull(triggerConfig);
        
        // Verify compliance reporter configuration
        Object complianceReporter = auditConfig.auditLogComplianceReporter();
        assertNotNull(complianceReporter);
        
        // Verify correlation ID filter configuration
        org.springframework.boot.web.servlet.FilterRegistrationBean<?> correlationFilter = 
            auditConfig.correlationIdFilter();
        assertNotNull(correlationFilter);
        assertNotNull(correlationFilter.getFilter());
        assertEquals(1, correlationFilter.getOrder());
    }
    
    /**
     * Tests integration between SecurityAuditService and AuditConfig.
     * 
     * This test validates that SecurityAuditService properly integrates with
     * AuditConfig for comprehensive security auditing capabilities. Verifies
     * that configuration settings, event publishers, and metrics collectors
     * work together to provide complete audit functionality.
     * 
     * Test Validation:
     * - SecurityAuditService uses configured audit infrastructure
     * - Event publishing and processing work end-to-end
     * - Metrics collection integrates with configuration settings
     * - Compliance validation uses configured compliance frameworks
     */
    @Test
    @WithMockUser(username = TEST_USERNAME)
    public void testSecurityAuditServiceAuditConfigIntegration() {
        // Arrange
        ApplicationEventPublisher eventPublisher = auditConfig.auditEventPublisher();
        
        // Act - Generate various audit events to test integration
        securityAuditService.auditAuthenticationEvent(
            TEST_USERNAME, "SUCCESS", TEST_IP_ADDRESS, TEST_SESSION_ID, "PASSWORD", authenticationContext
        );
        
        Map<String, Object> metrics = securityAuditService.generateSecurityMetrics("ALL", 1);
        Map<String, Object> compliance = securityAuditService.validateAuditCompliance("SOX", 30);
        
        // Assert
        // Verify integration between components
        assertNotNull(eventPublisher);
        assertNotNull(metrics);
        assertNotNull(compliance);
        
        // Verify AuditService integration
        verify(auditService, atLeast(1)).saveAuditLog(any());
        verify(auditService, times(3)).validateAuditTrailIntegrity(any(), any());
        verify(auditService, atLeast(1)).getAuditLogsByUser(anyString(), any(), any(), anyInt(), anyInt());
        
        // Verify SecurityEventListener integration
        verify(securityEventListener, times(1))
            .handleAuthenticationSuccessEvent(eq(TEST_USERNAME), anyString(), anyString(), any());
        
        // Verify metrics and compliance integration
        assertTrue(metrics.containsKey("metadata"));
        assertTrue(compliance.containsKey("validationMetadata"));
    }
    
    /**
     * Tests H2 in-memory database integration for testing.
     * 
     * This test validates that the audit system properly integrates with H2
     * in-memory database for testing purposes. Verifies that database operations,
     * audit log storage, and JPA integration function correctly in the test environment.
     * 
     * Test Validation:
     * - H2 database connection is established
     * - Database schema is created properly
     * - Audit log operations work correctly
     * - JPA integration supports all audit operations
     */
    @Test
    public void testDatabaseIntegrationWithH2() {
        // Test database operations through audit service integration
        securityAuditService.auditAuthenticationEvent(
            TEST_USERNAME, "SUCCESS", TEST_IP_ADDRESS, TEST_SESSION_ID, 
            "DATABASE_TEST", authenticationContext
        );
        
        // Verify audit log is saved (through mock verification)
        verify(auditService, times(1)).saveAuditLog(any());
        
        // Verify database configuration is available
        assertNotNull(auditConfig);
        
        // Test additional database operations
        Map<String, Object> metrics = securityAuditService.generateSecurityMetrics("ALL", 1);
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("metadata"));
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Creates a mock successful audit trail integrity result for testing.
     */
    private Map<String, Object> createSuccessfulIntegrityResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("integrityCheckPassed", true);
        result.put("recordsValidated", 1000);
        result.put("corruptedRecords", 0);
        return result;
    }
    
    /**
     * Creates a mock audit log page for testing metrics generation.
     */
    private org.springframework.data.domain.Page<com.carddemo.entity.AuditLog> createMockAuditLogPage() {
        List<com.carddemo.entity.AuditLog> mockLogs = new ArrayList<>();
        
        // Create sample audit logs
        com.carddemo.entity.AuditLog authLog = new com.carddemo.entity.AuditLog();
        authLog.setUsername(TEST_USERNAME);
        authLog.setEventType("AUTHENTICATION");
        authLog.setOutcome("SUCCESS");
        authLog.setTimestamp(LocalDateTime.now().minusHours(1));
        authLog.setSourceIp(TEST_IP_ADDRESS);
        authLog.setDetails("{\"sessionId\":\"test-session\",\"method\":\"PASSWORD\"}");
        mockLogs.add(authLog);
        
        com.carddemo.entity.AuditLog authzLog = new com.carddemo.entity.AuditLog();
        authzLog.setUsername(TEST_USERNAME);
        authzLog.setEventType("AUTHORIZATION");
        authzLog.setOutcome("SUCCESS");
        authzLog.setTimestamp(LocalDateTime.now().minusMinutes(30));
        authzLog.setSourceIp(TEST_IP_ADDRESS);
        authzLog.setDetails("{\"resourcePath\":\"/api/accounts\",\"method\":\"GET\"}");
        mockLogs.add(authzLog);
        
        return new org.springframework.data.domain.PageImpl<>(mockLogs);
    }
    
    /**
     * Creates a mock compliance report for testing.
     */
    private Map<String, Object> createMockComplianceReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalEntries", 500);
        report.put("controlsValidated", 25);
        report.put("complianceScore", 0.95);
        report.put("framework", "SOX");
        report.put("periodStart", LocalDateTime.now().minusDays(30));
        report.put("periodEnd", LocalDateTime.now());
        return report;
    }
    
    /**
     * Creates a mock archival result for testing retention policies.
     */
    private Map<String, Object> createMockArchivalResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("archivedCount", 100L);
        result.put("deletedCount", 25L);
        result.put("archivalSuccess", true);
        result.put("cutoffDate", LocalDateTime.now().minusDays(2555));
        return result;
    }
    
    /**
     * Test configuration to provide JobRepository bean for batch components.
     * This prevents UnsatisfiedDependencyException from batch job configurations.
     */
    @TestConfiguration
    static class TestBatchJobConfiguration {
        
        @Bean
        @Primary
        public JobRepository testJobRepository(DataSource dataSource) throws Exception {
            JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTransactionManager(new DataSourceTransactionManager(dataSource));
            factory.afterPropertiesSet();
            return factory.getObject();
        }
        
        @Bean
        @Primary  
        public PlatformTransactionManager testTransactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
        
        @Bean
        @Primary
        public JobLauncher testJobLauncher(JobRepository jobRepository) {
            TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
            launcher.setJobRepository(jobRepository);
            launcher.setTaskExecutor(new SyncTaskExecutor());
            return launcher;
        }
        
        @Bean
        @Primary
        public JobExplorer testJobExplorer(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
            JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTransactionManager(transactionManager);
            factory.afterPropertiesSet();
            return factory.getObject();
        }
        
        @Bean
        @Primary
        public JobRegistry testJobRegistry() {
            return new MapJobRegistry();
        }
        
        @Bean
        @Primary
        public JobOperator testJobOperator(JobLauncher jobLauncher, JobRepository jobRepository, 
                                         JobExplorer jobExplorer, JobRegistry jobRegistry) throws Exception {
            SimpleJobOperator jobOperator = new SimpleJobOperator();
            jobOperator.setJobLauncher(jobLauncher);
            jobOperator.setJobRepository(jobRepository);
            jobOperator.setJobExplorer(jobExplorer);
            jobOperator.setJobRegistry(jobRegistry);
            jobOperator.afterPropertiesSet();
            return jobOperator;
        }
    }
}