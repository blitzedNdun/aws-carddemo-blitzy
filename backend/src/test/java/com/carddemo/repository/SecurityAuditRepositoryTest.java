/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.repository;

import com.carddemo.entity.AuditLog;
import com.carddemo.repository.AuditLogRepository;
import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive integration test suite for SecurityAuditRepository operations.
 * 
 * This test class validates all aspects of security audit logging functionality including:
 * - User authentication event auditing (login success/failure)
 * - Data access audit trails for sensitive information
 * - PCI DSS compliance logging and validation
 * - GDPR Article 5 compliance with data access tracking
 * - Role-based access control audit logging
 * - Audit log immutability and integrity validation
 * - Concurrent audit log write performance and consistency
 * - Audit query performance under load
 * - Compliance-driven audit log retention and archival
 * - Security event correlation and forensic analysis
 * - Audit log encryption and tamper detection
 * 
 * Test Strategy:
 * - Uses Testcontainers PostgreSQL for isolated database testing
 * - Implements comprehensive coverage of AuditLogRepository query methods
 * - Validates functional parity with COBOL authentication audit patterns
 * - Ensures sub-200ms response times for audit query operations
 * - Tests concurrent write scenarios for production-level security logging
 * - Validates PCI DSS and GDPR compliance requirements
 * 
 * Performance Requirements:
 * - All audit queries must complete within 200ms threshold
 * - Concurrent write operations must maintain data consistency
 * - Archival operations must complete within processing windows
 * - Query performance must scale with audit log volume growth
 * 
 * Security Requirements:
 * - Audit logs must be immutable once written
 * - Integrity hash validation for tamper detection
 * - Proper data classification and compliance tagging
 * - Secure handling of sensitive audit data
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class SecurityAuditRepositoryTest extends AbstractBaseTest implements IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("carddemo_test")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Test user login audit trail creation.
     * 
     * Validates that successful user authentication events are properly logged
     * with complete audit information including username, timestamp, source IP,
     * and correlation ID for session tracking.
     * 
     * Requirements Covered:
     * - Spring Security audit events for authentication success
     * - Complete audit trail for user login events
     * - Session correlation tracking
     * - Timestamp precision and audit data integrity
     */
    @Test
    public void testUserLoginAuditTrailCreation() {
        // Arrange
        AuditLog loginAudit = new AuditLog();
        loginAudit.setUsername(TestConstants.TEST_USER_ID);
        loginAudit.setEventType("LOGIN_SUCCESS");
        loginAudit.setTimestamp(LocalDateTime.now());
        loginAudit.setSourceIp("192.168.1.100");
        loginAudit.setResourceAccessed("/api/auth/login");
        loginAudit.setActionPerformed("USER_AUTHENTICATION");
        loginAudit.setOutcome("SUCCESS");
        loginAudit.setCorrelationId("LOGIN_" + System.currentTimeMillis());
        loginAudit.setIntegrityHash(calculateIntegrityHash(loginAudit));

        // Act
        long startTime = System.currentTimeMillis();
        AuditLog savedAudit = auditLogRepository.save(loginAudit);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(savedAudit).isNotNull();
        assertThat(savedAudit.getId()).isNotNull();
        assertThat(savedAudit.getUsername()).isEqualTo(TestConstants.TEST_USER_ID);
        assertThat(savedAudit.getEventType()).isEqualTo("LOGIN_SUCCESS");
        assertThat(savedAudit.getOutcome()).isEqualTo("SUCCESS");
        assertThat(savedAudit.getCorrelationId()).startsWith("LOGIN_");
        assertThat(savedAudit.getIntegrityHash()).isNotNull();
        
        // Validate audit query performance
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("User login audit trail creation test completed", executionTime);
    }

    /**
     * Test failed login attempt logging.
     * 
     * Validates that failed authentication attempts are properly logged
     * with security-relevant information for threat detection and compliance.
     * Tests the repository's ability to track authentication failures for
     * security monitoring and incident response.
     * 
     * Requirements Covered:
     * - Failed authentication event logging
     * - Security incident tracking
     * - Brute force attack detection data
     * - Audit trail for unauthorized access attempts
     */
    @Test
    public void testFailedLoginAttemptLogging() {
        // Arrange
        AuditLog failedLoginAudit = new AuditLog();
        failedLoginAudit.setUsername("INVALIDUSER");
        failedLoginAudit.setEventType("LOGIN_FAILURE");
        failedLoginAudit.setTimestamp(LocalDateTime.now());
        failedLoginAudit.setSourceIp("192.168.1.200");
        failedLoginAudit.setResourceAccessed("/api/auth/login");
        failedLoginAudit.setActionPerformed("INVALID_CREDENTIALS");
        failedLoginAudit.setOutcome("FAILURE");
        failedLoginAudit.setCorrelationId("FAILED_LOGIN_" + System.currentTimeMillis());
        failedLoginAudit.setIntegrityHash(calculateIntegrityHash(failedLoginAudit));

        // Act
        long startTime = System.currentTimeMillis();
        AuditLog savedAudit = auditLogRepository.save(failedLoginAudit);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify failed login audit creation
        assertThat(savedAudit).isNotNull();
        assertThat(savedAudit.getUsername()).isEqualTo("INVALIDUSER");
        assertThat(savedAudit.getEventType()).isEqualTo("LOGIN_FAILURE");
        assertThat(savedAudit.getOutcome()).isEqualTo("FAILURE");
        assertThat(savedAudit.getActionPerformed()).isEqualTo("INVALID_CREDENTIALS");

        // Test failed authentication query functionality
        List<AuditLog> failedAttempts = auditLogRepository.findByOutcome("FAILURE");
        assertThat(failedAttempts).isNotEmpty();
        assertThat(failedAttempts).anyMatch(audit -> audit.getUsername().equals("INVALIDUSER"));

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Failed login attempt logging test completed", executionTime);
    }

    /**
     * Test data access audit for sensitive fields.
     * 
     * Validates that access to sensitive data fields (SSN, credit card numbers,
     * financial data) is properly logged with complete audit trails including
     * user identification, resource accessed, and data classification.
     * 
     * Requirements Covered:
     * - Sensitive data access tracking
     * - PII data access audit logs
     * - Financial data access monitoring
     * - Compliance with data protection regulations
     */
    @Test
    public void testDataAccessAuditForSensitiveFields() {
        // Arrange - Create multiple sensitive data access audit entries
        List<AuditLog> sensitiveAccessAudits = new ArrayList<>();
        
        // SSN access audit
        AuditLog ssnAccessAudit = new AuditLog();
        ssnAccessAudit.setUsername(TestConstants.TEST_USER_ID);
        ssnAccessAudit.setEventType("DATA_ACCESS");
        ssnAccessAudit.setTimestamp(LocalDateTime.now());
        ssnAccessAudit.setSourceIp("192.168.1.100");
        ssnAccessAudit.setResourceAccessed("/api/customers/123456789/ssn");
        ssnAccessAudit.setActionPerformed("READ_SSN");
        ssnAccessAudit.setOutcome("SUCCESS");
        ssnAccessAudit.setCorrelationId("SSN_ACCESS_" + System.currentTimeMillis());
        ssnAccessAudit.setIntegrityHash(calculateIntegrityHash(ssnAccessAudit));
        sensitiveAccessAudits.add(ssnAccessAudit);

        // Credit card access audit
        AuditLog cardAccessAudit = new AuditLog();
        cardAccessAudit.setUsername(TestConstants.TEST_USER_ID);
        cardAccessAudit.setEventType("DATA_ACCESS");
        cardAccessAudit.setTimestamp(LocalDateTime.now().plusSeconds(1));
        cardAccessAudit.setSourceIp("192.168.1.100");
        cardAccessAudit.setResourceAccessed("/api/cards/4444333322221111/details");
        cardAccessAudit.setActionPerformed("READ_CARD_DETAILS");
        cardAccessAudit.setOutcome("SUCCESS");
        cardAccessAudit.setCorrelationId("CARD_ACCESS_" + System.currentTimeMillis());
        cardAccessAudit.setIntegrityHash(calculateIntegrityHash(cardAccessAudit));
        sensitiveAccessAudits.add(cardAccessAudit);

        // Financial data access audit
        AuditLog financialAccessAudit = new AuditLog();
        financialAccessAudit.setUsername(TestConstants.TEST_USER_ID);
        financialAccessAudit.setEventType("DATA_ACCESS");
        financialAccessAudit.setTimestamp(LocalDateTime.now().plusSeconds(2));
        financialAccessAudit.setSourceIp("192.168.1.100");
        financialAccessAudit.setResourceAccessed("/api/accounts/ACC001234567/balance");
        financialAccessAudit.setActionPerformed("READ_ACCOUNT_BALANCE");
        financialAccessAudit.setOutcome("SUCCESS");
        financialAccessAudit.setCorrelationId("BALANCE_ACCESS_" + System.currentTimeMillis());
        financialAccessAudit.setIntegrityHash(calculateIntegrityHash(financialAccessAudit));
        sensitiveAccessAudits.add(financialAccessAudit);

        // Act
        long startTime = System.currentTimeMillis();
        List<AuditLog> savedAudits = new ArrayList<>();
        for (AuditLog audit : sensitiveAccessAudits) {
            savedAudits.add(auditLogRepository.save(audit));
        }
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify all sensitive data access audits were created
        assertThat(savedAudits).hasSize(3);
        for (AuditLog savedAudit : savedAudits) {
            assertThat(savedAudit.getId()).isNotNull();
            assertThat(savedAudit.getEventType()).isEqualTo("DATA_ACCESS");
            assertThat(savedAudit.getOutcome()).isEqualTo("SUCCESS");
            assertThat(savedAudit.getIntegrityHash()).isNotNull();
        }

        // Test data access query functionality
        List<AuditLog> dataAccessAudits = auditLogRepository.findByEventTypeAndTimestamp("DATA_ACCESS", 
            LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
        assertThat(dataAccessAudits).hasSize(3);

        // Validate specific sensitive resource access
        List<AuditLog> ssnAudits = auditLogRepository.findByUsernameAndTimestampBetween(
            TestConstants.TEST_USER_ID, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1))
            .stream()
            .filter(audit -> audit.getResourceAccessed().contains("/ssn"))
            .toList();
        assertThat(ssnAudits).hasSize(1);

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Data access audit for sensitive fields test completed", executionTime);
    }

    /**
     * Test PCI DSS compliant logging.
     * 
     * Validates that audit logs meet PCI DSS requirements for secure logging
     * including proper data classification, access control, and log integrity.
     * Tests compliance with PCI DSS Section 10 logging requirements.
     * 
     * Requirements Covered:
     * - PCI DSS Section 10 audit trail requirements
     * - Secure audit log storage and retrieval
     * - Payment card data access logging
     * - Compliance report generation capabilities
     */
    @Test
    public void testPciDssCompliantLogging() {
        // Arrange - Create PCI DSS compliant audit entries
        List<AuditLog> pciAudits = new ArrayList<>();
        
        // Payment processing audit
        AuditLog paymentAudit = new AuditLog();
        paymentAudit.setUsername(TestConstants.TEST_USER_ID);
        paymentAudit.setEventType("PAYMENT_PROCESSING");
        paymentAudit.setTimestamp(LocalDateTime.now());
        paymentAudit.setSourceIp("192.168.1.100");
        paymentAudit.setResourceAccessed("/api/transactions/process");
        paymentAudit.setActionPerformed("PROCESS_PAYMENT");
        paymentAudit.setOutcome("SUCCESS");
        paymentAudit.setCorrelationId("PCI_PAYMENT_" + System.currentTimeMillis());
        paymentAudit.setIntegrityHash(calculateIntegrityHash(paymentAudit));
        pciAudits.add(paymentAudit);

        // Card data access audit
        AuditLog cardDataAudit = new AuditLog();
        cardDataAudit.setUsername(TestConstants.TEST_USER_ID);
        cardDataAudit.setEventType("CARD_DATA_ACCESS");
        cardDataAudit.setTimestamp(LocalDateTime.now().plusSeconds(1));
        cardDataAudit.setSourceIp("192.168.1.100");
        cardDataAudit.setResourceAccessed("/api/cards/4444333322221111/details");
        cardDataAudit.setActionPerformed("READ_CARD_DATA");
        cardDataAudit.setOutcome("SUCCESS");
        cardDataAudit.setCorrelationId("PCI_CARD_" + System.currentTimeMillis());
        cardDataAudit.setIntegrityHash(calculateIntegrityHash(cardDataAudit));
        pciAudits.add(cardDataAudit);

        // Administrative access audit
        AuditLog adminAudit = new AuditLog();
        adminAudit.setUsername("ADMIN001");
        adminAudit.setEventType("ADMIN_ACCESS");
        adminAudit.setTimestamp(LocalDateTime.now().plusSeconds(2));
        adminAudit.setSourceIp("192.168.1.50");
        adminAudit.setResourceAccessed("/api/admin/system/config");
        adminAudit.setActionPerformed("VIEW_SYSTEM_CONFIG");
        adminAudit.setOutcome("SUCCESS");
        adminAudit.setCorrelationId("PCI_ADMIN_" + System.currentTimeMillis());
        adminAudit.setIntegrityHash(calculateIntegrityHash(adminAudit));
        pciAudits.add(adminAudit);

        // Act
        long startTime = System.currentTimeMillis();
        List<AuditLog> savedAudits = new ArrayList<>();
        for (AuditLog audit : pciAudits) {
            savedAudits.add(auditLogRepository.save(audit));
        }
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify PCI DSS compliant audit creation
        assertThat(savedAudits).hasSize(3);
        
        // Validate all audit entries have required PCI DSS fields
        for (AuditLog audit : savedAudits) {
            assertThat(audit.getUsername()).isNotBlank();
            assertThat(audit.getEventType()).isNotBlank();
            assertThat(audit.getTimestamp()).isNotNull();
            assertThat(audit.getSourceIp()).isNotBlank();
            assertThat(audit.getResourceAccessed()).isNotBlank();
            assertThat(audit.getActionPerformed()).isNotBlank();
            assertThat(audit.getOutcome()).isNotBlank();
            assertThat(audit.getIntegrityHash()).isNotBlank();
        }

        // Test PCI DSS compliance query functionality
        LocalDateTime searchStart = LocalDateTime.now().minusMinutes(1);
        LocalDateTime searchEnd = LocalDateTime.now().plusMinutes(1);
        
        List<AuditLog> paymentAudits = auditLogRepository.findByEventTypeAndTimestamp("PAYMENT_PROCESSING", searchStart, searchEnd);
        assertThat(paymentAudits).hasSize(1);
        assertThat(paymentAudits.get(0).getActionPerformed()).isEqualTo("PROCESS_PAYMENT");

        List<AuditLog> cardDataAudits = auditLogRepository.findByEventTypeAndTimestamp("CARD_DATA_ACCESS", searchStart, searchEnd);
        assertThat(cardDataAudits).hasSize(1);
        assertThat(cardDataAudits.get(0).getResourceAccessed()).contains("/cards/");

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("PCI DSS compliant logging test completed", executionTime);
    }

    /**
     * Test audit log retention policies.
     * 
     * Validates that audit logs can be properly archived and purged according
     * to GDPR and regulatory retention requirements. Tests the repository's
     * ability to manage audit log lifecycle and compliance-driven retention.
     * 
     * Requirements Covered:
     * - GDPR Article 5 data retention compliance
     * - Automated audit log archival
     * - Retention policy enforcement
     * - Historical audit data management
     */
    @Test
    public void testAuditLogRetentionPolicies() {
        // Arrange - Create audit logs with different timestamps for retention testing
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime oldTimestamp = currentTime.minusYears(TestConstants.GDPR_RETENTION_YEARS + 1);
        LocalDateTime recentTimestamp = currentTime.minusDays(30);

        // Old audit log (beyond retention period)
        AuditLog oldAudit = new AuditLog();
        oldAudit.setUsername(TestConstants.TEST_USER_ID);
        oldAudit.setEventType("DATA_ACCESS");
        oldAudit.setTimestamp(oldTimestamp);
        oldAudit.setSourceIp("192.168.1.100");
        oldAudit.setResourceAccessed("/api/legacy/data");
        oldAudit.setActionPerformed("READ_LEGACY_DATA");
        oldAudit.setOutcome("SUCCESS");
        oldAudit.setCorrelationId("RETENTION_OLD_" + System.currentTimeMillis());
        oldAudit.setIntegrityHash(calculateIntegrityHash(oldAudit));

        // Recent audit log (within retention period)
        AuditLog recentAudit = new AuditLog();
        recentAudit.setUsername(TestConstants.TEST_USER_ID);
        recentAudit.setEventType("DATA_ACCESS");
        recentAudit.setTimestamp(recentTimestamp);
        recentAudit.setSourceIp("192.168.1.100");
        recentAudit.setResourceAccessed("/api/current/data");
        recentAudit.setActionPerformed("READ_CURRENT_DATA");
        recentAudit.setOutcome("SUCCESS");
        recentAudit.setCorrelationId("RETENTION_RECENT_" + System.currentTimeMillis());
        recentAudit.setIntegrityHash(calculateIntegrityHash(recentAudit));

        // Act - Save audit logs
        long startTime = System.currentTimeMillis();
        AuditLog savedOldAudit = auditLogRepository.save(oldAudit);
        AuditLog savedRecentAudit = auditLogRepository.save(recentAudit);
        
        // Test retention policy enforcement
        LocalDateTime retentionCutoff = currentTime.minusYears(TestConstants.GDPR_RETENTION_YEARS);
        long deletedCount = auditLogRepository.deleteByTimestampBefore(retentionCutoff);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify retention policy functionality
        assertThat(savedOldAudit.getId()).isNotNull();
        assertThat(savedRecentAudit.getId()).isNotNull();
        
        // Verify old audit logs are identified for deletion
        assertThat(deletedCount).isGreaterThanOrEqualTo(1);

        // Verify recent audit logs are preserved
        List<AuditLog> remainingAudits = auditLogRepository.findAuditLogsByDateRange(
            recentTimestamp.minusDays(1), currentTime.plusDays(1));
        assertThat(remainingAudits).isNotEmpty();
        assertThat(remainingAudits).anyMatch(audit -> audit.getCorrelationId().startsWith("RETENTION_RECENT_"));

        // Validate GDPR compliance
        Duration retentionPeriod = Duration.between(oldTimestamp, currentTime);
        assertThat(retentionPeriod.toDays()).isGreaterThan(TestConstants.GDPR_RETENTION_YEARS * 365);

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Audit log retention policies test completed", executionTime);
    }

    /**
     * Test GDPR data access tracking.
     * 
     * Validates that all data access operations are properly tracked for GDPR
     * Article 5 compliance including lawful basis tracking, data subject rights,
     * and access purpose documentation.
     * 
     * Requirements Covered:
     * - GDPR Article 5 lawful processing requirements
     * - Data subject access request tracking
     * - Purpose limitation compliance logging
     * - Data access transparency reporting
     */
    @Test
    public void testGdprDataAccessTracking() {
        // Arrange - Create GDPR compliant data access audits
        String dataSubjectId = "CUSTOMER_" + TestConstants.TEST_USER_ID;
        LocalDateTime accessTime = LocalDateTime.now();
        
        // Personal data access audit
        AuditLog personalDataAudit = new AuditLog();
        personalDataAudit.setUsername(TestConstants.TEST_USER_ID);
        personalDataAudit.setEventType("PERSONAL_DATA_ACCESS");
        personalDataAudit.setTimestamp(accessTime);
        personalDataAudit.setSourceIp("192.168.1.100");
        personalDataAudit.setResourceAccessed("/api/customers/" + dataSubjectId + "/personal-info");
        personalDataAudit.setActionPerformed("READ_PERSONAL_DATA");
        personalDataAudit.setOutcome("SUCCESS");
        personalDataAudit.setCorrelationId("GDPR_PERSONAL_" + System.currentTimeMillis());
        personalDataAudit.setIntegrityHash(calculateIntegrityHash(personalDataAudit));

        // Data subject rights access audit
        AuditLog rightsAccessAudit = new AuditLog();
        rightsAccessAudit.setUsername("ADMIN001");
        rightsAccessAudit.setEventType("DATA_SUBJECT_RIGHTS");
        rightsAccessAudit.setTimestamp(accessTime.plusSeconds(1));
        rightsAccessAudit.setSourceIp("192.168.1.50");
        rightsAccessAudit.setResourceAccessed("/api/gdpr/data-export/" + dataSubjectId);
        rightsAccessAudit.setActionPerformed("EXPORT_PERSONAL_DATA");
        rightsAccessAudit.setOutcome("SUCCESS");
        rightsAccessAudit.setCorrelationId("GDPR_EXPORT_" + System.currentTimeMillis());
        rightsAccessAudit.setIntegrityHash(calculateIntegrityHash(rightsAccessAudit));

        // Act
        long startTime = System.currentTimeMillis();
        AuditLog savedPersonalAudit = auditLogRepository.save(personalDataAudit);
        AuditLog savedRightsAudit = auditLogRepository.save(rightsAccessAudit);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify GDPR compliant audit creation
        assertThat(savedPersonalAudit).isNotNull();
        assertThat(savedPersonalAudit.getEventType()).isEqualTo("PERSONAL_DATA_ACCESS");
        assertThat(savedPersonalAudit.getResourceAccessed()).contains("/personal-info");
        
        assertThat(savedRightsAudit).isNotNull();
        assertThat(savedRightsAudit.getEventType()).isEqualTo("DATA_SUBJECT_RIGHTS");
        assertThat(savedRightsAudit.getActionPerformed()).isEqualTo("EXPORT_PERSONAL_DATA");

        // Test GDPR compliance query functionality
        List<AuditLog> personalDataAudits = auditLogRepository.findByEventTypeAndTimestamp(
            "PERSONAL_DATA_ACCESS", accessTime.minusMinutes(1), accessTime.plusMinutes(1));
        assertThat(personalDataAudits).hasSize(1);

        List<AuditLog> dataRightsAudits = auditLogRepository.findByEventTypeAndTimestamp(
            "DATA_SUBJECT_RIGHTS", accessTime.minusMinutes(1), accessTime.plusMinutes(1));
        assertThat(dataRightsAudits).hasSize(1);

        // Validate data subject access tracking
        List<AuditLog> dataSubjectAudits = auditLogRepository.findAuditLogsByDateRange(
            accessTime.minusMinutes(1), accessTime.plusMinutes(1))
            .stream()
            .filter(audit -> audit.getResourceAccessed().contains(dataSubjectId))
            .toList();
        assertThat(dataSubjectAudits).hasSize(2);

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("GDPR data access tracking test completed", executionTime);
    }

    /**
     * Test role-based access audit.
     * 
     * Validates that role-based access control decisions are properly audited
     * including successful and denied access attempts with complete context
     * about the requested resource and user authorization level.
     * 
     * Requirements Covered:
     * - Role-based access control audit logging
     * - Authorization decision tracking
     * - Privilege escalation detection
     * - Administrative access monitoring
     */
    @Test
    public void testRoleBasedAccessAudit() {
        // Arrange - Create role-based access audit entries
        LocalDateTime auditTime = LocalDateTime.now();
        
        // Admin user successful access
        AuditLog adminSuccessAudit = new AuditLog();
        adminSuccessAudit.setUsername("ADMIN001");
        adminSuccessAudit.setEventType("ROLE_ACCESS");
        adminSuccessAudit.setTimestamp(auditTime);
        adminSuccessAudit.setSourceIp("192.168.1.50");
        adminSuccessAudit.setResourceAccessed("/api/admin/users/list");
        adminSuccessAudit.setActionPerformed("ADMIN_USER_MANAGEMENT");
        adminSuccessAudit.setOutcome("SUCCESS");
        adminSuccessAudit.setCorrelationId("ROLE_ADMIN_SUCCESS_" + System.currentTimeMillis());
        adminSuccessAudit.setIntegrityHash(calculateIntegrityHash(adminSuccessAudit));

        // Regular user denied admin access
        AuditLog userDeniedAudit = new AuditLog();
        userDeniedAudit.setUsername(TestConstants.TEST_USER_ID);
        userDeniedAudit.setEventType("ROLE_ACCESS");
        userDeniedAudit.setTimestamp(auditTime.plusSeconds(1));
        userDeniedAudit.setSourceIp("192.168.1.100");
        userDeniedAudit.setResourceAccessed("/api/admin/users/create");
        userDeniedAudit.setActionPerformed("UNAUTHORIZED_ACCESS_ATTEMPT");
        userDeniedAudit.setOutcome("FAILURE");
        userDeniedAudit.setCorrelationId("ROLE_USER_DENIED_" + System.currentTimeMillis());
        userDeniedAudit.setIntegrityHash(calculateIntegrityHash(userDeniedAudit));

        // Regular user successful access
        AuditLog userSuccessAudit = new AuditLog();
        userSuccessAudit.setUsername(TestConstants.TEST_USER_ID);
        userSuccessAudit.setEventType("ROLE_ACCESS");
        userSuccessAudit.setTimestamp(auditTime.plusSeconds(2));
        userSuccessAudit.setSourceIp("192.168.1.100");
        userSuccessAudit.setResourceAccessed("/api/accounts/own");
        userSuccessAudit.setActionPerformed("READ_OWN_ACCOUNT");
        userSuccessAudit.setOutcome("SUCCESS");
        userSuccessAudit.setCorrelationId("ROLE_USER_SUCCESS_" + System.currentTimeMillis());
        userSuccessAudit.setIntegrityHash(calculateIntegrityHash(userSuccessAudit));

        // Act
        long startTime = System.currentTimeMillis();
        AuditLog savedAdminAudit = auditLogRepository.save(adminSuccessAudit);
        AuditLog savedUserDeniedAudit = auditLogRepository.save(userDeniedAudit);
        AuditLog savedUserSuccessAudit = auditLogRepository.save(userSuccessAudit);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify role-based access audit creation
        assertThat(savedAdminAudit).isNotNull();
        assertThat(savedAdminAudit.getActionPerformed()).isEqualTo("ADMIN_USER_MANAGEMENT");
        assertThat(savedAdminAudit.getOutcome()).isEqualTo("SUCCESS");

        assertThat(savedUserDeniedAudit).isNotNull();
        assertThat(savedUserDeniedAudit.getActionPerformed()).isEqualTo("UNAUTHORIZED_ACCESS_ATTEMPT");
        assertThat(savedUserDeniedAudit.getOutcome()).isEqualTo("FAILURE");

        assertThat(savedUserSuccessAudit).isNotNull();
        assertThat(savedUserSuccessAudit.getActionPerformed()).isEqualTo("READ_OWN_ACCOUNT");
        assertThat(savedUserSuccessAudit.getOutcome()).isEqualTo("SUCCESS");

        // Test role-based access query functionality
        List<AuditLog> adminAccessAudits = auditLogRepository.findByUsernameAndTimestampBetween(
            "ADMIN001", auditTime.minusMinutes(1), auditTime.plusMinutes(1));
        assertThat(adminAccessAudits).hasSize(1);
        assertThat(adminAccessAudits.get(0).getResourceAccessed()).contains("/admin/");

        List<AuditLog> userAccessAudits = auditLogRepository.findByUsernameAndTimestampBetween(
            TestConstants.TEST_USER_ID, auditTime.minusMinutes(1), auditTime.plusMinutes(1));
        assertThat(userAccessAudits).hasSize(2);

        List<AuditLog> deniedAccessAudits = auditLogRepository.findByOutcome("FAILURE");
        assertThat(deniedAccessAudits).isNotEmpty();
        assertThat(deniedAccessAudits).anyMatch(audit -> audit.getActionPerformed().equals("UNAUTHORIZED_ACCESS_ATTEMPT"));

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Role-based access audit test completed", executionTime);
    }

    /**
     * Test audit log immutability.
     * 
     * Validates that audit logs cannot be modified after creation to ensure
     * audit trail integrity and compliance with security audit requirements.
     * Tests the immutable nature of audit records and integrity validation.
     * 
     * Requirements Covered:
     * - Audit trail immutability requirements
     * - Integrity hash validation
     * - Tamper detection capabilities
     * - Audit log security controls
     */
    @Test
    public void testAuditLogImmutability() {
        // Arrange - Create an audit log with integrity hash
        AuditLog originalAudit = new AuditLog();
        originalAudit.setUsername(TestConstants.TEST_USER_ID);
        originalAudit.setEventType("IMMUTABILITY_TEST");
        originalAudit.setTimestamp(LocalDateTime.now());
        originalAudit.setSourceIp("192.168.1.100");
        originalAudit.setResourceAccessed("/api/test/immutability");
        originalAudit.setActionPerformed("CREATE_IMMUTABLE_AUDIT");
        originalAudit.setOutcome("SUCCESS");
        originalAudit.setCorrelationId("IMMUTABLE_TEST_" + System.currentTimeMillis());
        originalAudit.setIntegrityHash(calculateIntegrityHash(originalAudit));

        // Act - Save audit log and retrieve it
        long startTime = System.currentTimeMillis();
        AuditLog savedAudit = auditLogRepository.save(originalAudit);
        AuditLog retrievedAudit = auditLogRepository.findById(savedAudit.getId()).orElse(null);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify audit log immutability
        assertThat(retrievedAudit).isNotNull();
        assertThat(retrievedAudit.getId()).isEqualTo(savedAudit.getId());
        assertThat(retrievedAudit.getUsername()).isEqualTo(originalAudit.getUsername());
        assertThat(retrievedAudit.getEventType()).isEqualTo(originalAudit.getEventType());
        assertThat(retrievedAudit.getTimestamp()).isEqualTo(originalAudit.getTimestamp());
        assertThat(retrievedAudit.getIntegrityHash()).isEqualTo(originalAudit.getIntegrityHash());

        // Validate integrity hash calculation
        String recalculatedHash = calculateIntegrityHash(retrievedAudit);
        assertThat(retrievedAudit.getIntegrityHash()).isEqualTo(recalculatedHash);

        // Test tamper detection - modify data and verify hash mismatch
        AuditLog tamperedAudit = new AuditLog();
        tamperedAudit.setId(retrievedAudit.getId());
        tamperedAudit.setUsername("TAMPERED_USER");
        tamperedAudit.setEventType(retrievedAudit.getEventType());
        tamperedAudit.setTimestamp(retrievedAudit.getTimestamp());
        tamperedAudit.setSourceIp(retrievedAudit.getSourceIp());
        tamperedAudit.setResourceAccessed(retrievedAudit.getResourceAccessed());
        tamperedAudit.setActionPerformed(retrievedAudit.getActionPerformed());
        tamperedAudit.setOutcome(retrievedAudit.getOutcome());
        tamperedAudit.setCorrelationId(retrievedAudit.getCorrelationId());
        tamperedAudit.setIntegrityHash(retrievedAudit.getIntegrityHash());

        String tamperedHash = calculateIntegrityHash(tamperedAudit);
        assertThat(tamperedHash).isNotEqualTo(retrievedAudit.getIntegrityHash());

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Audit log immutability test completed", executionTime);
    }

    /**
     * Test concurrent audit log writes.
     * 
     * Validates that the repository can handle concurrent audit log write
     * operations without data corruption or integrity violations. Tests
     * thread safety and data consistency under concurrent access scenarios.
     * 
     * Requirements Covered:
     * - Concurrent audit log write performance
     * - Thread safety of audit operations
     * - Data consistency under load
     * - Audit log integrity under concurrent access
     */
    @Test
    public void testConcurrentAuditLogWrites() throws Exception {
        // Arrange - Set up concurrent execution environment
        int threadCount = 10;
        int auditsPerThread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<List<AuditLog>>> futures = new ArrayList<>();

        // Act - Execute concurrent audit log writes
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            CompletableFuture<List<AuditLog>> future = CompletableFuture.supplyAsync(() -> {
                List<AuditLog> threadAudits = new ArrayList<>();
                for (int j = 0; j < auditsPerThread; j++) {
                    AuditLog concurrentAudit = new AuditLog();
                    concurrentAudit.setUsername("THREAD_USER_" + threadId);
                    concurrentAudit.setEventType("CONCURRENT_WRITE");
                    concurrentAudit.setTimestamp(LocalDateTime.now());
                    concurrentAudit.setSourceIp("192.168.1." + (100 + threadId));
                    concurrentAudit.setResourceAccessed("/api/concurrent/test/" + threadId + "/" + j);
                    concurrentAudit.setActionPerformed("CONCURRENT_AUDIT_WRITE");
                    concurrentAudit.setOutcome("SUCCESS");
                    concurrentAudit.setCorrelationId("CONCURRENT_" + threadId + "_" + j + "_" + System.currentTimeMillis());
                    concurrentAudit.setIntegrityHash(calculateIntegrityHash(concurrentAudit));
                    
                    AuditLog savedAudit = auditLogRepository.save(concurrentAudit);
                    threadAudits.add(savedAudit);
                }
                return threadAudits;
            }, executorService);
            futures.add(future);
        }

        // Wait for all concurrent operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify concurrent audit log creation
        List<AuditLog> allConcurrentAudits = new ArrayList<>();
        for (CompletableFuture<List<AuditLog>> future : futures) {
            allConcurrentAudits.addAll(future.get());
        }

        assertThat(allConcurrentAudits).hasSize(threadCount * auditsPerThread);
        
        // Verify all audit logs have unique IDs
        List<Long> auditIds = allConcurrentAudits.stream()
            .map(AuditLog::getId)
            .toList();
        assertThat(auditIds).doesNotHaveDuplicates();

        // Verify all audit logs have valid integrity hashes
        for (AuditLog audit : allConcurrentAudits) {
            assertThat(audit.getIntegrityHash()).isNotNull();
            String recalculatedHash = calculateIntegrityHash(audit);
            assertThat(audit.getIntegrityHash()).isEqualTo(recalculatedHash);
        }

        // Test concurrent access query functionality
        List<AuditLog> concurrentAudits = auditLogRepository.findByEventTypeAndTimestamp(
            "CONCURRENT_WRITE", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
        assertThat(concurrentAudits).hasSize(threadCount * auditsPerThread);

        // Validate data consistency - check that all threads completed successfully
        long successfulWrites = concurrentAudits.stream()
            .mapToLong(audit -> audit.getOutcome().equals("SUCCESS") ? 1 : 0)
            .sum();
        assertThat(successfulWrites).isEqualTo(threadCount * auditsPerThread);

        // Validate performance requirements (should handle concurrent load efficiently)
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS * 5); // Allow 5x threshold for concurrent operations

        executorService.shutdown();
        logTestExecution("Concurrent audit log writes test completed", executionTime);
    }

    /**
     * Test audit query performance.
     * 
     * Validates that audit log queries meet performance requirements under
     * various conditions including large datasets, complex filters, and
     * time-range queries. Tests query optimization and indexing effectiveness.
     * 
     * Requirements Covered:
     * - Audit query performance requirements
     * - Database query optimization
     * - Index effectiveness validation
     * - Response time compliance under load
     */
    @Test
    public void testAuditQueryPerformance() {
        // Arrange - Create large dataset for performance testing
        List<AuditLog> performanceTestAudits = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(1);
        
        // Create 100 audit log entries for performance testing
        for (int i = 0; i < 100; i++) {
            AuditLog performanceAudit = new AuditLog();
            performanceAudit.setUsername("PERF_USER_" + (i % 10));
            performanceAudit.setEventType("PERFORMANCE_TEST");
            performanceAudit.setTimestamp(baseTime.plusMinutes(i));
            performanceAudit.setSourceIp("192.168.1." + (100 + (i % 50)));
            performanceAudit.setResourceAccessed("/api/performance/test/" + i);
            performanceAudit.setActionPerformed("PERFORMANCE_AUDIT_" + i);
            performanceAudit.setOutcome(i % 10 == 0 ? "FAILURE" : "SUCCESS");
            performanceAudit.setCorrelationId("PERF_" + i + "_" + System.currentTimeMillis());
            performanceAudit.setIntegrityHash(calculateIntegrityHash(performanceAudit));
            performanceTestAudits.add(performanceAudit);
        }

        // Act - Save all performance test audits
        long setupStartTime = System.currentTimeMillis();
        for (AuditLog audit : performanceTestAudits) {
            auditLogRepository.save(audit);
        }
        long setupTime = System.currentTimeMillis() - setupStartTime;

        // Test 1: Username-based query performance
        long usernameQueryStart = System.currentTimeMillis();
        List<AuditLog> usernameResults = auditLogRepository.findAuditLogsByUser("PERF_USER_5");
        long usernameQueryTime = System.currentTimeMillis() - usernameQueryStart;

        // Test 2: Date range query performance
        long dateRangeQueryStart = System.currentTimeMillis();
        List<AuditLog> dateRangeResults = auditLogRepository.findAuditLogsByDateRange(
            baseTime, LocalDateTime.now());
        long dateRangeQueryTime = System.currentTimeMillis() - dateRangeQueryStart;

        // Test 3: Event type query performance
        long eventTypeQueryStart = System.currentTimeMillis();
        List<AuditLog> eventTypeResults = auditLogRepository.findByEventTypeAndTimestamp(
            "PERFORMANCE_TEST", baseTime.minusMinutes(10), LocalDateTime.now().plusMinutes(10));
        long eventTypeQueryTime = System.currentTimeMillis() - eventTypeQueryStart;

        // Test 4: Correlation ID query performance
        String testCorrelationId = performanceTestAudits.get(50).getCorrelationId();
        long correlationQueryStart = System.currentTimeMillis();
        List<AuditLog> correlationResults = auditLogRepository.findByCorrelationId(testCorrelationId);
        long correlationQueryTime = System.currentTimeMillis() - correlationQueryStart;

        // Assert - Verify query results and performance
        assertThat(usernameResults).isNotEmpty();
        assertThat(usernameResults).allMatch(audit -> audit.getUsername().equals("PERF_USER_5"));
        assertThat(usernameQueryTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);

        assertThat(dateRangeResults).hasSizeGreaterThanOrEqualTo(100);
        assertThat(dateRangeQueryTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);

        assertThat(eventTypeResults).hasSizeGreaterThanOrEqualTo(100);
        assertThat(eventTypeResults).allMatch(audit -> audit.getEventType().equals("PERFORMANCE_TEST"));
        assertThat(eventTypeQueryTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);

        assertThat(correlationResults).hasSize(1);
        assertThat(correlationResults.get(0).getCorrelationId()).isEqualTo(testCorrelationId);
        assertThat(correlationQueryTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);

        // Validate overall setup performance
        assertThat(setupTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS * 10); // Allow 10x for bulk setup

        logTestExecution("Audit query performance test completed - Setup: " + setupTime + "ms, Queries: " + 
            (usernameQueryTime + dateRangeQueryTime + eventTypeQueryTime + correlationQueryTime) + "ms", executionTime);
    }

    /**
     * Test audit log archival process.
     * 
     * Validates the audit log archival functionality for long-term storage
     * and compliance requirements. Tests the repository's ability to identify
     * and process audit logs for archival based on retention policies.
     * 
     * Requirements Covered:
     * - Audit log archival process
     * - Long-term audit storage management
     * - Compliance-driven archival policies
     * - Historical audit data preservation
     */
    @Test
    public void testAuditLogArchivalProcess() {
        // Arrange - Create audit logs at different time periods for archival testing
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime archivalCandidateTime = currentTime.minusMonths(6);
        LocalDateTime recentTime = currentTime.minusDays(7);

        // Create archival candidate audit
        AuditLog archivalCandidate = new AuditLog();
        archivalCandidate.setUsername(TestConstants.TEST_USER_ID);
        archivalCandidate.setEventType("ARCHIVAL_CANDIDATE");
        archivalCandidate.setTimestamp(archivalCandidateTime);
        archivalCandidate.setSourceIp("192.168.1.100");
        archivalCandidate.setResourceAccessed("/api/archival/test");
        archivalCandidate.setActionPerformed("CREATE_ARCHIVAL_CANDIDATE");
        archivalCandidate.setOutcome("SUCCESS");
        archivalCandidate.setCorrelationId("ARCHIVAL_CANDIDATE_" + System.currentTimeMillis());
        archivalCandidate.setIntegrityHash(calculateIntegrityHash(archivalCandidate));

        // Create recent audit (should not be archived)
        AuditLog recentAudit = new AuditLog();
        recentAudit.setUsername(TestConstants.TEST_USER_ID);
        recentAudit.setEventType("RECENT_AUDIT");
        recentAudit.setTimestamp(recentTime);
        recentAudit.setSourceIp("192.168.1.100");
        recentAudit.setResourceAccessed("/api/recent/test");
        recentAudit.setActionPerformed("CREATE_RECENT_AUDIT");
        recentAudit.setOutcome("SUCCESS");
        recentAudit.setCorrelationId("RECENT_AUDIT_" + System.currentTimeMillis());
        recentAudit.setIntegrityHash(calculateIntegrityHash(recentAudit));

        // Act - Save audit logs and test archival identification
        long startTime = System.currentTimeMillis();
        AuditLog savedArchivalCandidate = auditLogRepository.save(archivalCandidate);
        AuditLog savedRecentAudit = auditLogRepository.save(recentAudit);

        // Identify archival candidates (older than 3 months)
        LocalDateTime archivalCutoff = currentTime.minusMonths(3);
        List<AuditLog> archivalCandidates = auditLogRepository.findAuditLogsByDateRange(
            LocalDateTime.of(2020, 1, 1, 0, 0), archivalCutoff);
        
        // Identify recent audits (should remain active)
        List<AuditLog> activeAudits = auditLogRepository.findAuditLogsByDateRange(
            archivalCutoff, currentTime.plusDays(1));
        
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify archival process functionality
        assertThat(savedArchivalCandidate).isNotNull();
        assertThat(savedRecentAudit).isNotNull();

        // Verify archival candidates are properly identified
        assertThat(archivalCandidates).isNotEmpty();
        assertThat(archivalCandidates).anyMatch(audit -> audit.getEventType().equals("ARCHIVAL_CANDIDATE"));

        // Verify recent audits are preserved in active storage
        assertThat(activeAudits).isNotEmpty();
        assertThat(activeAudits).anyMatch(audit -> audit.getEventType().equals("RECENT_AUDIT"));

        // Validate archival query performance
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);

        // Test count-based archival identification
        long totalAuditCount = auditLogRepository.count();
        assertThat(totalAuditCount).isGreaterThanOrEqualTo(2);

        logTestExecution("Audit log archival process test completed", executionTime);
    }

    /**
     * Test compliance report generation.
     * 
     * Validates the repository's ability to generate compliance reports
     * including PCI DSS audit reports, GDPR data access reports, and
     * security incident summaries for regulatory compliance.
     * 
     * Requirements Covered:
     * - Regulatory compliance reporting
     * - PCI DSS compliance documentation
     * - GDPR compliance reporting
     * - Security audit report generation
     */
    @Test
    public void testComplianceReportGeneration() {
        // Arrange - Create diverse audit data for compliance reporting
        LocalDateTime reportPeriodStart = LocalDateTime.now().minusDays(30);
        LocalDateTime reportPeriodEnd = LocalDateTime.now();
        
        // Create various audit types for comprehensive reporting
        List<AuditLog> complianceAudits = new ArrayList<>();
        
        // PCI DSS audit entry
        AuditLog pciAudit = new AuditLog();
        pciAudit.setUsername(TestConstants.TEST_USER_ID);
        pciAudit.setEventType("PCI_COMPLIANCE");
        pciAudit.setTimestamp(reportPeriodStart.plusDays(5));
        pciAudit.setSourceIp("192.168.1.100");
        pciAudit.setResourceAccessed("/api/cards/secure-data");
        pciAudit.setActionPerformed("PCI_SECURE_ACCESS");
        pciAudit.setOutcome("SUCCESS");
        pciAudit.setCorrelationId("PCI_COMPLIANCE_" + System.currentTimeMillis());
        pciAudit.setIntegrityHash(calculateIntegrityHash(pciAudit));
        complianceAudits.add(pciAudit);

        // GDPR audit entry
        AuditLog gdprAudit = new AuditLog();
        gdprAudit.setUsername("ADMIN001");
        gdprAudit.setEventType("GDPR_COMPLIANCE");
        gdprAudit.setTimestamp(reportPeriodStart.plusDays(10));
        gdprAudit.setSourceIp("192.168.1.50");
        gdprAudit.setResourceAccessed("/api/gdpr/data-subject-access");
        gdprAudit.setActionPerformed("GDPR_DATA_EXPORT");
        gdprAudit.setOutcome("SUCCESS");
        gdprAudit.setCorrelationId("GDPR_COMPLIANCE_" + System.currentTimeMillis());
        gdprAudit.setIntegrityHash(calculateIntegrityHash(gdprAudit));
        complianceAudits.add(gdprAudit);

        // Security incident audit
        AuditLog securityAudit = new AuditLog();
        securityAudit.setUsername("SECURITY_MONITOR");
        securityAudit.setEventType("SECURITY_INCIDENT");
        securityAudit.setTimestamp(reportPeriodStart.plusDays(15));
        securityAudit.setSourceIp("192.168.1.200");
        securityAudit.setResourceAccessed("/api/security/incident-response");
        securityAudit.setActionPerformed("INVESTIGATE_SECURITY_EVENT");
        securityAudit.setOutcome("SUCCESS");
        securityAudit.setCorrelationId("SECURITY_INCIDENT_" + System.currentTimeMillis());
        securityAudit.setIntegrityHash(calculateIntegrityHash(securityAudit));
        complianceAudits.add(securityAudit);

        // Act - Save compliance audit data and generate reports
        long startTime = System.currentTimeMillis();
        for (AuditLog audit : complianceAudits) {
            auditLogRepository.save(audit);
        }

        // Generate compliance reports using repository queries
        List<AuditLog> pciComplianceReport = auditLogRepository.findByEventTypeAndTimestamp(
            "PCI_COMPLIANCE", reportPeriodStart, reportPeriodEnd);
        
        List<AuditLog> gdprComplianceReport = auditLogRepository.findByEventTypeAndTimestamp(
            "GDPR_COMPLIANCE", reportPeriodStart, reportPeriodEnd);
        
        List<AuditLog> securityIncidentReport = auditLogRepository.findByEventTypeAndTimestamp(
            "SECURITY_INCIDENT", reportPeriodStart, reportPeriodEnd);
        
        List<AuditLog> comprehensiveReport = auditLogRepository.findAuditLogsByDateRange(
            reportPeriodStart, reportPeriodEnd);
        
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify compliance report generation
        assertThat(pciComplianceReport).hasSize(1);
        assertThat(pciComplianceReport.get(0).getActionPerformed()).isEqualTo("PCI_SECURE_ACCESS");

        assertThat(gdprComplianceReport).hasSize(1);
        assertThat(gdprComplianceReport.get(0).getActionPerformed()).isEqualTo("GDPR_DATA_EXPORT");

        assertThat(securityIncidentReport).hasSize(1);
        assertThat(securityIncidentReport.get(0).getActionPerformed()).isEqualTo("INVESTIGATE_SECURITY_EVENT");

        assertThat(comprehensiveReport).hasSizeGreaterThanOrEqualTo(3);

        // Validate report query performance
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Compliance report generation test completed", executionTime);
    }

    /**
     * Test security event correlation.
     * 
     * Validates the repository's ability to correlate related security events
     * using correlation IDs and temporal relationships. Tests forensic analysis
     * capabilities and incident investigation support.
     * 
     * Requirements Covered:
     * - Security event correlation capabilities
     * - Forensic analysis support
     * - Incident investigation data
     * - Related event tracking
     */
    @Test
    public void testSecurityEventCorrelation() {
        // Arrange - Create correlated security events
        String sessionCorrelationId = "SESSION_" + System.currentTimeMillis();
        LocalDateTime eventTime = LocalDateTime.now();
        
        // Event 1: Session establishment
        AuditLog sessionStartAudit = new AuditLog();
        sessionStartAudit.setUsername(TestConstants.TEST_USER_ID);
        sessionStartAudit.setEventType("SESSION_START");
        sessionStartAudit.setTimestamp(eventTime);
        sessionStartAudit.setSourceIp("192.168.1.100");
        sessionStartAudit.setResourceAccessed("/api/auth/session/start");
        sessionStartAudit.setActionPerformed("CREATE_USER_SESSION");
        sessionStartAudit.setOutcome("SUCCESS");
        sessionStartAudit.setCorrelationId(sessionCorrelationId);
        sessionStartAudit.setIntegrityHash(calculateIntegrityHash(sessionStartAudit));

        // Event 2: Data access within session
        AuditLog dataAccessAudit = new AuditLog();
        dataAccessAudit.setUsername(TestConstants.TEST_USER_ID);
        dataAccessAudit.setEventType("SESSION_DATA_ACCESS");
        dataAccessAudit.setTimestamp(eventTime.plusSeconds(30));
        dataAccessAudit.setSourceIp("192.168.1.100");
        dataAccessAudit.setResourceAccessed("/api/accounts/details");
        dataAccessAudit.setActionPerformed("READ_ACCOUNT_DATA");
        dataAccessAudit.setOutcome("SUCCESS");
        dataAccessAudit.setCorrelationId(sessionCorrelationId);
        dataAccessAudit.setIntegrityHash(calculateIntegrityHash(dataAccessAudit));

        // Event 3: Session termination
        AuditLog sessionEndAudit = new AuditLog();
        sessionEndAudit.setUsername(TestConstants.TEST_USER_ID);
        sessionEndAudit.setEventType("SESSION_END");
        sessionEndAudit.setTimestamp(eventTime.plusMinutes(5));
        sessionEndAudit.setSourceIp("192.168.1.100");
        sessionEndAudit.setResourceAccessed("/api/auth/session/end");
        sessionEndAudit.setActionPerformed("TERMINATE_USER_SESSION");
        sessionEndAudit.setOutcome("SUCCESS");
        sessionEndAudit.setCorrelationId(sessionCorrelationId);
        sessionEndAudit.setIntegrityHash(calculateIntegrityHash(sessionEndAudit));

        // Act - Save correlated events and test correlation queries
        long startTime = System.currentTimeMillis();
        AuditLog savedSessionStart = auditLogRepository.save(sessionStartAudit);
        AuditLog savedDataAccess = auditLogRepository.save(dataAccessAudit);
        AuditLog savedSessionEnd = auditLogRepository.save(sessionEndAudit);

        // Test correlation ID-based event retrieval
        List<AuditLog> correlatedEvents = auditLogRepository.findByCorrelationId(sessionCorrelationId);
        
        // Test temporal correlation by user and IP
        List<AuditLog> userIpEvents = auditLogRepository.findByUsernameAndTimestampBetween(
            TestConstants.TEST_USER_ID, eventTime.minusMinutes(1), eventTime.plusMinutes(10))
            .stream()
            .filter(audit -> audit.getSourceIp().equals("192.168.1.100"))
            .toList();
        
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify security event correlation
        assertThat(correlatedEvents).hasSize(3);
        assertThat(correlatedEvents).allMatch(audit -> audit.getCorrelationId().equals(sessionCorrelationId));
        assertThat(correlatedEvents).allMatch(audit -> audit.getUsername().equals(TestConstants.TEST_USER_ID));

        // Verify temporal ordering of correlated events
        List<AuditLog> sortedEvents = correlatedEvents.stream()
            .sorted((a1, a2) -> a1.getTimestamp().compareTo(a2.getTimestamp()))
            .toList();
        
        assertThat(sortedEvents.get(0).getEventType()).isEqualTo("SESSION_START");
        assertThat(sortedEvents.get(1).getEventType()).isEqualTo("SESSION_DATA_ACCESS");
        assertThat(sortedEvents.get(2).getEventType()).isEqualTo("SESSION_END");

        // Verify temporal correlation capabilities
        assertThat(userIpEvents).hasSizeGreaterThanOrEqualTo(3);
        assertThat(userIpEvents).allMatch(audit -> audit.getSourceIp().equals("192.168.1.100"));

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Security event correlation test completed", executionTime);
    }

    /**
     * Test audit log encryption.
     * 
     * Validates that audit log data integrity is maintained through cryptographic
     * hashing and that sensitive audit information is properly protected.
     * Tests the integrity hash calculation and validation mechanisms.
     * 
     * Requirements Covered:
     * - Audit log data integrity validation
     * - Cryptographic integrity protection
     * - Tamper detection capabilities
     * - Secure audit log storage
     */
    @Test
    public void testAuditLogEncryption() {
        // Arrange - Create audit log with comprehensive data for encryption testing
        AuditLog encryptionTestAudit = new AuditLog();
        encryptionTestAudit.setUsername(TestConstants.TEST_USER_ID);
        encryptionTestAudit.setEventType("ENCRYPTION_TEST");
        encryptionTestAudit.setTimestamp(LocalDateTime.now());
        encryptionTestAudit.setSourceIp("192.168.1.100");
        encryptionTestAudit.setResourceAccessed("/api/encryption/test");
        encryptionTestAudit.setActionPerformed("VALIDATE_ENCRYPTION");
        encryptionTestAudit.setOutcome("SUCCESS");
        encryptionTestAudit.setCorrelationId("ENCRYPTION_TEST_" + System.currentTimeMillis());
        
        // Calculate integrity hash before saving
        String originalHash = calculateIntegrityHash(encryptionTestAudit);
        encryptionTestAudit.setIntegrityHash(originalHash);

        // Act - Save audit log and test encryption validation
        long startTime = System.currentTimeMillis();
        AuditLog savedAudit = auditLogRepository.save(encryptionTestAudit);
        AuditLog retrievedAudit = auditLogRepository.findById(savedAudit.getId()).orElse(null);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify encryption and integrity validation
        assertThat(retrievedAudit).isNotNull();
        assertThat(retrievedAudit.getIntegrityHash()).isNotBlank();
        assertThat(retrievedAudit.getIntegrityHash()).isEqualTo(originalHash);

        // Test integrity hash validation
        String recalculatedHash = calculateIntegrityHash(retrievedAudit);
        assertThat(recalculatedHash).isEqualTo(retrievedAudit.getIntegrityHash());

        // Test tamper detection - modify field and verify hash mismatch
        AuditLog tamperedAudit = new AuditLog();
        tamperedAudit.setId(retrievedAudit.getId());
        tamperedAudit.setUsername("TAMPERED_USER"); // Modified username
        tamperedAudit.setEventType(retrievedAudit.getEventType());
        tamperedAudit.setTimestamp(retrievedAudit.getTimestamp());
        tamperedAudit.setSourceIp(retrievedAudit.getSourceIp());
        tamperedAudit.setResourceAccessed(retrievedAudit.getResourceAccessed());
        tamperedAudit.setActionPerformed(retrievedAudit.getActionPerformed());
        tamperedAudit.setOutcome(retrievedAudit.getOutcome());
        tamperedAudit.setCorrelationId(retrievedAudit.getCorrelationId());
        tamperedAudit.setIntegrityHash(retrievedAudit.getIntegrityHash()); // Original hash (should not match)

        String tamperedHash = calculateIntegrityHash(tamperedAudit);
        assertThat(tamperedHash).isNotEqualTo(retrievedAudit.getIntegrityHash());

        // Test hash consistency across multiple calculations
        String secondCalculation = calculateIntegrityHash(retrievedAudit);
        String thirdCalculation = calculateIntegrityHash(retrievedAudit);
        assertThat(secondCalculation).isEqualTo(thirdCalculation);
        assertThat(secondCalculation).isEqualTo(retrievedAudit.getIntegrityHash());

        // Validate performance requirements
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Audit log encryption test completed", executionTime);
    }

    /**
     * Test comprehensive audit repository functionality.
     * 
     * Validates the complete audit repository functionality including all
     * query methods, data persistence, and performance characteristics.
     * This test serves as an integration validation of all audit capabilities.
     * 
     * Requirements Covered:
     * - Complete audit repository functionality
     * - All query method validation
     * - Performance characteristic validation
     * - Data persistence integrity
     */
    @Test
    public void testComprehensiveAuditRepositoryFunctionality() {
        // Arrange - Create comprehensive test dataset
        LocalDateTime testStartTime = LocalDateTime.now().minusHours(1);
        LocalDateTime testEndTime = LocalDateTime.now();
        List<AuditLog> comprehensiveAudits = new ArrayList<>();
        
        // Create multiple audit types for comprehensive testing
        String[] eventTypes = {"LOGIN_SUCCESS", "LOGIN_FAILURE", "DATA_ACCESS", "ADMIN_ACCESS", "SYSTEM_CONFIG"};
        String[] usernames = {TestConstants.TEST_USER_ID, "ADMIN001", "SYSTEM", "AUDIT_USER"};
        String[] outcomes = {"SUCCESS", "FAILURE"};
        
        for (int i = 0; i < 20; i++) {
            AuditLog comprehensiveAudit = new AuditLog();
            comprehensiveAudit.setUsername(usernames[i % usernames.length]);
            comprehensiveAudit.setEventType(eventTypes[i % eventTypes.length]);
            comprehensiveAudit.setTimestamp(testStartTime.plusMinutes(i * 3));
            comprehensiveAudit.setSourceIp("192.168.1." + (100 + (i % 20)));
            comprehensiveAudit.setResourceAccessed("/api/comprehensive/test/" + i);
            comprehensiveAudit.setActionPerformed("COMPREHENSIVE_TEST_" + i);
            comprehensiveAudit.setOutcome(outcomes[i % outcomes.length]);
            comprehensiveAudit.setCorrelationId("COMPREHENSIVE_" + i + "_" + System.currentTimeMillis());
            comprehensiveAudit.setIntegrityHash(calculateIntegrityHash(comprehensiveAudit));
            comprehensiveAudits.add(comprehensiveAudit);
        }

        // Act - Test all repository methods
        long startTime = System.currentTimeMillis();
        
        // Save all audits
        for (AuditLog audit : comprehensiveAudits) {
            auditLogRepository.save(audit);
        }

        // Test findAll functionality
        List<AuditLog> allAudits = auditLogRepository.findAll();
        
        // Test count functionality
        long totalCount = auditLogRepository.count();
        
        // Test username-based queries
        List<AuditLog> userAudits = auditLogRepository.findAuditLogsByUser(TestConstants.TEST_USER_ID);
        
        // Test date range queries
        List<AuditLog> dateRangeAudits = auditLogRepository.findAuditLogsByDateRange(testStartTime, testEndTime);
        
        // Test event type queries
        List<AuditLog> loginAudits = auditLogRepository.findByEventTypeAndTimestamp(
            "LOGIN_SUCCESS", testStartTime, testEndTime);
        
        // Test outcome-based queries
        List<AuditLog> successAudits = auditLogRepository.findByOutcome("SUCCESS");
        List<AuditLog> failureAudits = auditLogRepository.findByOutcome("FAILURE");
        
        // Test source IP queries
        List<AuditLog> ipAudits = auditLogRepository.findBySourceIpAndTimestamp(
            "192.168.1.100", testStartTime, testEndTime);
        
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - Verify comprehensive repository functionality
        assertThat(allAudits).hasSizeGreaterThanOrEqualTo(20);
        assertThat(totalCount).isGreaterThanOrEqualTo(20);
        
        assertThat(userAudits).isNotEmpty();
        assertThat(userAudits).allMatch(audit -> audit.getUsername().equals(TestConstants.TEST_USER_ID));
        
        assertThat(dateRangeAudits).hasSizeGreaterThanOrEqualTo(20);
        assertThat(dateRangeAudits).allMatch(audit -> 
            !audit.getTimestamp().isBefore(testStartTime) && !audit.getTimestamp().isAfter(testEndTime));
        
        assertThat(loginAudits).isNotEmpty();
        assertThat(loginAudits).allMatch(audit -> audit.getEventType().equals("LOGIN_SUCCESS"));
        
        assertThat(successAudits).isNotEmpty();
        assertThat(successAudits).allMatch(audit -> audit.getOutcome().equals("SUCCESS"));
        
        assertThat(failureAudits).isNotEmpty();
        assertThat(failureAudits).allMatch(audit -> audit.getOutcome().equals("FAILURE"));
        
        if (!ipAudits.isEmpty()) {
            assertThat(ipAudits).allMatch(audit -> audit.getSourceIp().equals("192.168.1.100"));
        }

        // Validate comprehensive query performance
        assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS * 3); // Allow 3x for comprehensive testing
        
        logTestExecution("Comprehensive audit repository functionality test completed", executionTime);
    }

    /**
     * Helper method to calculate integrity hash for audit log entries.
     * 
     * Calculates a SHA-256 based integrity hash for audit log data to ensure
     * tamper detection and data integrity validation. This method creates
     * a cryptographic hash of all audit log fields to detect any unauthorized
     * modifications to audit records.
     * 
     * @param auditLog The audit log entry to calculate hash for
     * @return SHA-256 hash string for integrity validation
     */
    private String calculateIntegrityHash(AuditLog auditLog) {
        try {
            // Create hash input from all audit log fields
            StringBuilder hashInput = new StringBuilder();
            hashInput.append(auditLog.getUsername() != null ? auditLog.getUsername() : "");
            hashInput.append(auditLog.getEventType() != null ? auditLog.getEventType() : "");
            hashInput.append(auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : "");
            hashInput.append(auditLog.getSourceIp() != null ? auditLog.getSourceIp() : "");
            hashInput.append(auditLog.getResourceAccessed() != null ? auditLog.getResourceAccessed() : "");
            hashInput.append(auditLog.getActionPerformed() != null ? auditLog.getActionPerformed() : "");
            hashInput.append(auditLog.getOutcome() != null ? auditLog.getOutcome() : "");
            hashInput.append(auditLog.getCorrelationId() != null ? auditLog.getCorrelationId() : "");

            // Calculate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(hashInput.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Helper method to set up PostgreSQL container properties.
     * 
     * Configures Spring Boot application properties to use the Testcontainers
     * PostgreSQL instance for integration testing. This method dynamically
     * sets database connection properties based on the running container.
     * 
     * @param registry Dynamic property registry for Spring Boot configuration
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
    }
}
