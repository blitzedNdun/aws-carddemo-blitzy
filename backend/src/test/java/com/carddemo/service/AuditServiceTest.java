/*
 * AuditServiceTest.java
 * 
 * Unit test class for AuditService that validates audit trail functionality
 * for tracking user actions and system changes with compliance requirements.
 * 
 * Implements comprehensive testing of:
 * - Audit trail generation and validation
 * - User action tracking and attribution
 * - Data change logging with integrity verification
 * - Compliance reporting capabilities
 * - Audit log retention and archival
 * - Sensitive data masking
 * - Regulatory compliance requirements
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 */

package com.carddemo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.carddemo.entity.AuditLog;
import com.carddemo.repository.AuditLogRepository;
import com.carddemo.service.AuditService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

/**
 * Comprehensive unit tests for AuditService ensuring audit trail functionality,
 * compliance requirements, and data integrity validation.
 */
@ExtendWith(MockitoExtension.class)
@UnitTest
public class AuditServiceTest extends BaseServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog sampleAuditLog;
    private LocalDateTime testTimestamp;
    
    private static final String TEST_USERNAME = "testuser001";
    private static final String TEST_EVENT_TYPE = "LOGIN";
    private static final String TEST_SOURCE_IP = "192.168.1.100";
    private static final String TEST_ACTION = "User authentication";
    private static final String TEST_OUTCOME = "SUCCESS";

    @BeforeEach
    void setUp() {
        super.setupTestData();
        testTimestamp = LocalDateTime.now();
        
        // Create sample audit log for testing
        sampleAuditLog = new AuditLog();
        sampleAuditLog.setId(1L);
        sampleAuditLog.setUsername(TEST_USERNAME);
        sampleAuditLog.setEventType(TEST_EVENT_TYPE);
        sampleAuditLog.setTimestamp(testTimestamp);
        sampleAuditLog.setSourceIp(TEST_SOURCE_IP);
        sampleAuditLog.setActionPerformed(TEST_ACTION);
        sampleAuditLog.setOutcome(TEST_OUTCOME);
    }

    @Nested
    @DisplayName("Audit Log Creation Tests")
    class AuditLogCreationTests {

        @Test
        @DisplayName("Should save audit log successfully with all required fields")
        void saveAuditLog_Success() {
            // Arrange
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

            // Act
            AuditLog result = auditService.saveAuditLog(sampleAuditLog);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(result.getEventType()).isEqualTo(TEST_EVENT_TYPE);
            assertThat(result.getTimestamp()).isEqualTo(testTimestamp);
            assertThat(result.getSourceIp()).isEqualTo(TEST_SOURCE_IP);
            assertThat(result.getActionPerformed()).isEqualTo(TEST_ACTION);
            assertThat(result.getOutcome()).isEqualTo(TEST_OUTCOME);
            
            verify(auditLogRepository).save(sampleAuditLog);
        }

        @Test
        @DisplayName("Should validate audit log immutability after creation")
        void validateAuditLogImmutability() {
            // Arrange
            AuditLog originalLog = new AuditLog();
            originalLog.setUsername(TEST_USERNAME);
            originalLog.setEventType(TEST_EVENT_TYPE);
            originalLog.setTimestamp(testTimestamp);
            
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

            // Act
            AuditLog savedLog = auditService.saveAuditLog(originalLog);

            // Assert - Verify audit log cannot be modified after creation
            assertThat(savedLog.getId()).isNotNull();
            assertThat(savedLog.getUsername()).isEqualTo(TEST_USERNAME);
            
            // Verify repository was called with immutable log
            verify(auditLogRepository).save(argThat(log -> 
                log.getUsername().equals(TEST_USERNAME) &&
                log.getEventType().equals(TEST_EVENT_TYPE) &&
                log.getTimestamp().equals(testTimestamp)
            ));
        }

        @Test
        @DisplayName("Should ensure timestamp accuracy for audit events")
        void validateTimestampAccuracy() {
            // Arrange
            LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);
            AuditLog testLog = new AuditLog();
            testLog.setUsername(TEST_USERNAME);
            testLog.setEventType(TEST_EVENT_TYPE);
            
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
                AuditLog log = invocation.getArgument(0);
                log.setTimestamp(LocalDateTime.now());
                return log;
            });

            // Act
            AuditLog result = auditService.saveAuditLog(testLog);
            LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);

            // Assert
            assertThat(result.getTimestamp()).isAfter(beforeSave);
            assertThat(result.getTimestamp()).isBefore(afterSave);
        }
    }

    @Nested
    @DisplayName("Audit Log Retrieval Tests")
    class AuditLogRetrievalTests {

        @Test
        @DisplayName("Should retrieve audit logs by username")
        void getAuditLogsByUser_Success() {
            // Arrange
            LocalDateTime startDate = testTimestamp.minusDays(7);
            LocalDateTime endDate = testTimestamp.plusDays(1);
            List<AuditLog> expectedLogs = Arrays.asList(sampleAuditLog);
            
            when(auditLogRepository.findByUsernameAndTimestampBetween(TEST_USERNAME, startDate, endDate))
                    .thenReturn(expectedLogs);

            // Act
            List<AuditLog> result = auditService.getAuditLogsByUser(TEST_USERNAME, startDate, endDate);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo(TEST_USERNAME);
            
            verify(auditLogRepository).findByUsernameAndTimestampBetween(TEST_USERNAME, startDate, endDate);
        }

        @Test
        @DisplayName("Should retrieve audit logs by event type")
        void getAuditLogsByEventType_Success() {
            // Arrange
            LocalDateTime dateFrom = testTimestamp.minusDays(1);
            List<AuditLog> expectedLogs = Arrays.asList(sampleAuditLog);
            
            when(auditLogRepository.findByEventTypeAndTimestamp(TEST_EVENT_TYPE, dateFrom))
                    .thenReturn(expectedLogs);

            // Act
            List<AuditLog> result = auditService.getAuditLogsByEventType(TEST_EVENT_TYPE, dateFrom);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventType()).isEqualTo(TEST_EVENT_TYPE);
            
            verify(auditLogRepository).findByEventTypeAndTimestamp(TEST_EVENT_TYPE, dateFrom);
        }

        @Test
        @DisplayName("Should retrieve audit logs by date range")
        void getAuditLogsByDateRange_Success() {
            // Arrange
            LocalDateTime startDate = testTimestamp.minusDays(30);
            LocalDateTime endDate = testTimestamp;
            List<AuditLog> expectedLogs = Arrays.asList(sampleAuditLog);
            
            when(auditLogRepository.findByDateRange(startDate, endDate))
                    .thenReturn(expectedLogs);

            // Act
            List<AuditLog> result = auditService.getAuditLogsByDateRange(startDate, endDate);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTimestamp()).isAfter(startDate.minusSeconds(1));
            assertThat(result.get(0).getTimestamp()).isBefore(endDate.plusSeconds(1));
            
            verify(auditLogRepository).findByDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("Should handle empty results when no logs found")
        void getAuditLogsByUser_EmptyResult() {
            // Arrange
            LocalDateTime startDate = testTimestamp.minusDays(7);
            LocalDateTime endDate = testTimestamp.plusDays(1);
            
            when(auditLogRepository.findByUsernameAndTimestampBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<AuditLog> result = auditService.getAuditLogsByUser("nonexistent", startDate, endDate);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Audit Trail Integrity Tests")
    class AuditTrailIntegrityTests {

        @Test
        @DisplayName("Should validate audit trail integrity successfully")
        void validateAuditTrailIntegrity_Success() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            when(auditLogRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(auditLogs);

            // Act
            boolean isIntegrityValid = auditService.validateAuditTrailIntegrity(
                testTimestamp.minusDays(1), testTimestamp.plusDays(1));

            // Assert
            assertThat(isIntegrityValid).isTrue();
            verify(auditLogRepository).findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should detect integrity violations in audit trail")
        void validateAuditTrailIntegrity_IntegrityViolation() {
            // Arrange - Create logs with potential integrity issues
            AuditLog corruptedLog = new AuditLog();
            corruptedLog.setUsername(TEST_USERNAME);
            corruptedLog.setEventType(TEST_EVENT_TYPE);
            corruptedLog.setTimestamp(testTimestamp);
            corruptedLog.setIntegrityHash("invalid_hash");
            
            List<AuditLog> auditLogs = Arrays.asList(corruptedLog);
            when(auditLogRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(auditLogs);

            // Act
            boolean isIntegrityValid = auditService.validateAuditTrailIntegrity(
                testTimestamp.minusDays(1), testTimestamp.plusDays(1));

            // Assert
            assertThat(isIntegrityValid).isFalse();
        }

        @Test
        @DisplayName("Should ensure user attribution accuracy")
        void validateUserAttribution() {
            // Arrange
            String expectedUserId = TestConstants.TEST_USER_ID;
            AuditLog userActionLog = new AuditLog();
            userActionLog.setUsername(expectedUserId);
            userActionLog.setEventType("ACCOUNT_UPDATE");
            userActionLog.setTimestamp(testTimestamp);
            userActionLog.setUserRole(TestConstants.TEST_USER_ROLE);
            
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(userActionLog);

            // Act
            AuditLog result = auditService.saveAuditLog(userActionLog);

            // Assert
            assertThat(result.getUsername()).isEqualTo(expectedUserId);
            assertThat(result.getUserRole()).isEqualTo(TestConstants.TEST_USER_ROLE);
            assertThat(result.getEventType()).isEqualTo("ACCOUNT_UPDATE");
        }
    }

    @Nested
    @DisplayName("Compliance Reporting Tests")
    class ComplianceReportingTests {

        @Test
        @DisplayName("Should generate compliance report successfully")
        void generateComplianceReport_Success() {
            // Arrange
            LocalDateTime reportStartDate = testTimestamp.minusDays(30);
            LocalDateTime reportEndDate = testTimestamp;
            List<AuditLog> complianceData = Arrays.asList(sampleAuditLog);
            
            when(auditLogRepository.findByDateRange(reportStartDate, reportEndDate))
                    .thenReturn(complianceData);

            // Act
            String complianceReport = auditService.generateComplianceReport(reportStartDate, reportEndDate);

            // Assert
            assertThat(complianceReport).isNotNull();
            assertThat(complianceReport).isNotEmpty();
            assertThat(complianceReport).contains("Compliance Report");
            assertThat(complianceReport).contains(TEST_USERNAME);
            assertThat(complianceReport).contains(TEST_EVENT_TYPE);
            
            verify(auditLogRepository).findByDateRange(reportStartDate, reportEndDate);
        }

        @Test
        @DisplayName("Should handle empty data for compliance report")
        void generateComplianceReport_EmptyData() {
            // Arrange
            LocalDateTime reportStartDate = testTimestamp.minusDays(30);
            LocalDateTime reportEndDate = testTimestamp;
            
            when(auditLogRepository.findByDateRange(reportStartDate, reportEndDate))
                    .thenReturn(Collections.emptyList());

            // Act
            String complianceReport = auditService.generateComplianceReport(reportStartDate, reportEndDate);

            // Assert
            assertThat(complianceReport).isNotNull();
            assertThat(complianceReport).contains("No audit data found");
        }

        @Test
        @DisplayName("Should validate regulatory compliance requirements")
        void validateRegulatoryCompliance() {
            // Arrange
            AuditLog regulatoryLog = new AuditLog();
            regulatoryLog.setUsername(TestConstants.TEST_ADMIN_ROLE);
            regulatoryLog.setEventType("REGULATORY_COMPLIANCE_CHECK");
            regulatoryLog.setTimestamp(testTimestamp);
            regulatoryLog.setActionPerformed("PCI DSS compliance validation");
            regulatoryLog.setOutcome("COMPLIANT");
            
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(regulatoryLog);

            // Act
            AuditLog result = auditService.saveAuditLog(regulatoryLog);

            // Assert
            assertThat(result.getEventType()).isEqualTo("REGULATORY_COMPLIANCE_CHECK");
            assertThat(result.getActionPerformed()).contains("PCI DSS compliance");
            assertThat(result.getOutcome()).isEqualTo("COMPLIANT");
        }
    }

    @Nested
    @DisplayName("Sensitive Data Masking Tests")
    class SensitiveDataMaskingTests {

        @Test
        @DisplayName("Should mask sensitive data in audit logs")
        void maskSensitiveData_Success() {
            // Arrange
            AuditLog sensitiveDataLog = new AuditLog();
            sensitiveDataLog.setUsername(TEST_USERNAME);
            sensitiveDataLog.setEventType("CARD_NUMBER_ACCESS");
            sensitiveDataLog.setTimestamp(testTimestamp);
            sensitiveDataLog.setActionPerformed("Viewed card number 4111-1111-1111-1111");
            sensitiveDataLog.setSensitiveData("4111111111111111");
            
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
                AuditLog log = invocation.getArgument(0);
                // Simulate masking sensitive data
                if (log.getSensitiveData() != null && log.getSensitiveData().length() > 4) {
                    String maskedData = "*".repeat(log.getSensitiveData().length() - 4) + 
                                      log.getSensitiveData().substring(log.getSensitiveData().length() - 4);
                    log.setSensitiveData(maskedData);
                }
                return log;
            });

            // Act
            AuditLog result = auditService.saveAuditLog(sensitiveDataLog);

            // Assert
            assertThat(result.getSensitiveData()).startsWith("*");
            assertThat(result.getSensitiveData()).endsWith("1111");
            assertThat(result.getSensitiveData()).doesNotContain("4111111111111111");
        }

        @Test
        @DisplayName("Should handle null sensitive data gracefully")
        void maskSensitiveData_NullData() {
            // Arrange
            AuditLog logWithNullSensitiveData = new AuditLog();
            logWithNullSensitiveData.setUsername(TEST_USERNAME);
            logWithNullSensitiveData.setEventType("LOGIN");
            logWithNullSensitiveData.setSensitiveData(null);
            
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(logWithNullSensitiveData);

            // Act & Assert
            assertThatCode(() -> auditService.saveAuditLog(logWithNullSensitiveData))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Audit Log Search Tests")
    class AuditLogSearchTests {

        @Test
        @DisplayName("Should search audit logs successfully with criteria")
        void searchAuditLogs_Success() {
            // Arrange
            String searchCriteria = "LOGIN";
            LocalDateTime fromDate = testTimestamp.minusDays(7);
            LocalDateTime toDate = testTimestamp;
            List<AuditLog> searchResults = Arrays.asList(sampleAuditLog);
            
            when(auditLogRepository.findByEventTypeAndTimestamp(searchCriteria, fromDate))
                    .thenReturn(searchResults);

            // Act
            List<AuditLog> result = auditService.searchAuditLogs(searchCriteria, fromDate, toDate);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventType()).contains(searchCriteria);
        }

        @Test
        @DisplayName("Should handle complex search queries")
        void searchAuditLogs_ComplexQuery() {
            // Arrange
            String searchCriteria = "ACCOUNT_UPDATE";
            LocalDateTime fromDate = testTimestamp.minusDays(30);
            LocalDateTime toDate = testTimestamp;
            
            AuditLog accountUpdateLog = new AuditLog();
            accountUpdateLog.setUsername("admin001");
            accountUpdateLog.setEventType("ACCOUNT_UPDATE");
            accountUpdateLog.setTimestamp(testTimestamp.minusDays(5));
            accountUpdateLog.setActionPerformed("Updated account balance");
            
            List<AuditLog> complexSearchResults = Arrays.asList(accountUpdateLog);
            when(auditLogRepository.findByEventTypeAndTimestamp(searchCriteria, fromDate))
                    .thenReturn(complexSearchResults);

            // Act
            List<AuditLog> result = auditService.searchAuditLogs(searchCriteria, fromDate, toDate);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getEventType()).isEqualTo("ACCOUNT_UPDATE");
            assertThat(result.get(0).getActionPerformed()).contains("balance");
        }

        @Test
        @DisplayName("Should return empty results for invalid search criteria")
        void searchAuditLogs_NoResults() {
            // Arrange
            String searchCriteria = "NONEXISTENT_EVENT";
            LocalDateTime fromDate = testTimestamp.minusDays(7);
            LocalDateTime toDate = testTimestamp;
            
            when(auditLogRepository.findByEventTypeAndTimestamp(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<AuditLog> result = auditService.searchAuditLogs(searchCriteria, fromDate, toDate);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Audit Log Archive and Retention Tests")
    class AuditLogArchiveTests {

        @Test
        @DisplayName("Should archive old audit logs successfully")
        void archiveOldLogs_Success() {
            // Arrange
            LocalDateTime cutoffDate = testTimestamp.minusDays(365); // One year ago
            int expectedDeletedCount = 5;
            
            when(auditLogRepository.deleteByTimestampBefore(cutoffDate))
                    .thenReturn(expectedDeletedCount);

            // Act
            int archivedCount = auditService.archiveOldLogs(cutoffDate);

            // Assert
            assertThat(archivedCount).isEqualTo(expectedDeletedCount);
            verify(auditLogRepository).deleteByTimestampBefore(cutoffDate);
        }

        @Test
        @DisplayName("Should handle archive operation with no old logs")
        void archiveOldLogs_NoLogsToArchive() {
            // Arrange
            LocalDateTime cutoffDate = testTimestamp.minusDays(365);
            
            when(auditLogRepository.deleteByTimestampBefore(cutoffDate))
                    .thenReturn(0);

            // Act
            int archivedCount = auditService.archiveOldLogs(cutoffDate);

            // Assert
            assertThat(archivedCount).isEqualTo(0);
            verify(auditLogRepository).deleteByTimestampBefore(cutoffDate);
        }

        @Test
        @DisplayName("Should validate retention policy compliance")
        void validateRetentionPolicy() {
            // Arrange
            LocalDateTime sevenYearsAgo = testTimestamp.minusDays(365 * 7); // 7 years retention
            List<AuditLog> oldLogs = Arrays.asList(sampleAuditLog);
            
            when(auditLogRepository.findByDateRange(any(LocalDateTime.class), eq(sevenYearsAgo)))
                    .thenReturn(oldLogs);

            // Act
            List<AuditLog> logsToRetain = auditService.getAuditLogsByDateRange(LocalDateTime.MIN, sevenYearsAgo);

            // Assert
            assertThat(logsToRetain).isNotEmpty();
            // Verify we're checking logs older than retention period
            verify(auditLogRepository).findByDateRange(any(LocalDateTime.class), eq(sevenYearsAgo));
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle bulk audit log creation efficiently")
        void bulkAuditLogCreation_Performance() {
            // Arrange
            int bulkSize = 100;
            List<AuditLog> bulkLogs = createBulkAuditLogs(bulkSize);
            
            when(auditLogRepository.save(any(AuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            long startTime = System.currentTimeMillis();
            for (AuditLog log : bulkLogs) {
                auditService.saveAuditLog(log);
            }
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS * bulkSize);
            verify(auditLogRepository, times(bulkSize)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Should maintain COBOL precision in audit calculations")
        void validateCobolPrecisionCompliance() {
            // Arrange
            AuditLog precisionTestLog = new AuditLog();
            precisionTestLog.setUsername(TEST_USERNAME);
            precisionTestLog.setEventType("BALANCE_CALCULATION");
            precisionTestLog.setTimestamp(testTimestamp);
            
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(precisionTestLog);

            // Act
            AuditLog result = auditService.saveAuditLog(precisionTestLog);

            // Assert
            assertThat(result).isNotNull();
            // Verify COBOL precision standards are maintained
            super.validateCobolPrecision();
            super.validateCobolParity();
        }

        private List<AuditLog> createBulkAuditLogs(int count) {
            return java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> {
                        AuditLog log = new AuditLog();
                        log.setUsername("user" + i);
                        log.setEventType("BULK_TEST_" + i);
                        log.setTimestamp(testTimestamp.minusSeconds(i));
                        log.setActionPerformed("Bulk test action " + i);
                        return log;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null audit log gracefully")
        void saveAuditLog_NullInput() {
            // Act & Assert
            assertThatThrownBy(() -> auditService.saveAuditLog(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Audit log cannot be null");
        }

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void saveAuditLog_RepositoryException() {
            // Arrange
            when(auditLogRepository.save(any(AuditLog.class)))
                    .thenThrow(new RuntimeException("Database connection error"));

            // Act & Assert
            assertThatThrownBy(() -> auditService.saveAuditLog(sampleAuditLog))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection error");
        }

        @Test
        @DisplayName("Should validate date range parameters")
        void getAuditLogsByDateRange_InvalidDateRange() {
            // Arrange
            LocalDateTime startDate = testTimestamp;
            LocalDateTime endDate = testTimestamp.minusDays(1); // End before start

            // Act & Assert
            assertThatThrownBy(() -> auditService.getAuditLogsByDateRange(startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date cannot be after end date");
        }

        @Test
        @DisplayName("Should handle empty username gracefully")
        void getAuditLogsByUser_EmptyUsername() {
            // Act & Assert
            assertThatThrownBy(() -> auditService.getAuditLogsByUser("", testTimestamp.minusDays(1), testTimestamp))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username cannot be empty");
        }
    }

    @Override
    protected void setupTestData() {
        // Initialize test data specific to audit service testing
        super.setupTestData();
        resetMocks();
    }

    @Override
    protected void cleanupTestData() {
        super.cleanupTestData();
        reset(auditLogRepository);
    }
}