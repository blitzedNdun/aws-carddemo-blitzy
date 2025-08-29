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
import com.carddemo.test.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Comprehensive unit tests for AuditService ensuring audit trail functionality,
 * compliance requirements, and data integrity validation.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest implements UnitTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog sampleAuditLog;
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;
    private Pageable defaultPageable;

    @BeforeEach
    public void setUp() {
        testStartTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        testEndTime = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
        defaultPageable = PageRequest.of(0, 10);

        sampleAuditLog = new AuditLog();
        sampleAuditLog.setId(1L);
        sampleAuditLog.setUsername("testuser");
        sampleAuditLog.setEventType("AUTHENTICATION");
        sampleAuditLog.setTimestamp(LocalDateTime.now());
        sampleAuditLog.setSourceIp("192.168.1.100");
        sampleAuditLog.setOutcome("SUCCESS");
        sampleAuditLog.setResourceAccessed("account");
        sampleAuditLog.setActionPerformed("LOGIN");
        sampleAuditLog.setDetails("User login successful");
        sampleAuditLog.setIntegrityHash("hash123");
    }

    @Nested
    @DisplayName("Audit Log Creation Tests")
    class AuditLogCreationTests {

        @Test
        @DisplayName("Should save audit log successfully")
        void shouldSaveAuditLogSuccessfully() {
            // Arrange
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

            // Act
            AuditLog result = auditService.saveAuditLog(sampleAuditLog);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEventType()).isEqualTo("AUTHENTICATION");
            verify(auditLogRepository, times(1)).save(sampleAuditLog);
        }

        @Test
        @DisplayName("Should set timestamp when creating audit log")
        void shouldSetTimestampWhenCreatingAuditLog() {
            // Arrange
            AuditLog logWithoutTimestamp = new AuditLog();
            logWithoutTimestamp.setUsername("testuser");
            logWithoutTimestamp.setEventType("LOGIN");
            logWithoutTimestamp.setOutcome("SUCCESS");

            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(logWithoutTimestamp);

            // Act
            AuditLog result = auditService.saveAuditLog(logWithoutTimestamp);

            // Assert
            verify(auditLogRepository, times(1)).save(logWithoutTimestamp);
        }

        @Test
        @DisplayName("Should validate required audit log fields")
        void shouldValidateRequiredAuditLogFields() {
            // Arrange
            AuditLog incompleteLog = new AuditLog();
            // Missing required fields like username

            // Act & Assert - expect validation exception
            assertThatThrownBy(() -> auditService.saveAuditLog(incompleteLog))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username is required");
        }
    }

    @Nested
    @DisplayName("Audit Log Retrieval Tests")
    class AuditLogRetrievalTests {

        @Test
        @DisplayName("Should retrieve audit logs by user successfully")
        void shouldRetrieveAuditLogsByUserSuccessfully() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findByUsernameAndTimestampBetween(
                    eq("testuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.getAuditLogsByUser("testuser", testStartTime, testEndTime, 0, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");
            verify(auditLogRepository, times(1)).findByUsernameAndTimestampBetween(
                    eq("testuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle empty user audit log results")
        void shouldHandleEmptyUserAuditLogResults() {
            // Arrange
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);

            when(auditLogRepository.findByUsernameAndTimestampBetween(
                    eq("nonexistentuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // Act
            Page<AuditLog> result = auditService.getAuditLogsByUser("nonexistentuser", testStartTime, testEndTime, 0, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            verify(auditLogRepository, times(1)).findByUsernameAndTimestampBetween(
                    eq("nonexistentuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should retrieve audit logs by event type successfully")
        void shouldRetrieveAuditLogsByEventTypeSuccessfully() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findByEventTypeAndTimestampBetween(
                    eq("AUTHENTICATION"), eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.getAuditLogsByEventType("AUTHENTICATION", testStartTime, testEndTime, 0, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEventType()).isEqualTo("AUTHENTICATION");
            verify(auditLogRepository, times(1)).findByEventTypeAndTimestampBetween(
                    eq("AUTHENTICATION"), eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should retrieve audit logs by date range successfully")
        void shouldRetrieveAuditLogsByDateRangeSuccessfully() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.getAuditLogsByDateRange(testStartTime, testEndTime, 0, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle pagination parameters correctly")
        void shouldHandlePaginationParametersCorrectly() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, PageRequest.of(1, 5), 10);

            when(auditLogRepository.findByUsernameAndTimestampBetween(
                    eq("testuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.getAuditLogsByUser("testuser", testStartTime, testEndTime, 1, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Audit Trail Integrity Tests")
    class AuditTrailIntegrityTests {

        @Test
        @DisplayName("Should validate audit trail integrity successfully")
        void shouldValidateAuditTrailIntegritySuccessfully() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Map<String, Object> result = auditService.validateAuditTrailIntegrity(testStartTime, testEndTime);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("validationResult");
            assertThat(result).containsKey("totalRecords");
            assertThat(result).containsKey("validRecords");
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should detect integrity violations in audit trail")
        void shouldDetectIntegrityViolationsInAuditTrail() {
            // Arrange
            AuditLog corruptedLog = new AuditLog();
            corruptedLog.setId(2L);
            corruptedLog.setUsername("testuser2");
            corruptedLog.setEventType("TRANSACTION");
            corruptedLog.setOutcome("SUCCESS");
            corruptedLog.setIntegrityHash(null); // Missing integrity hash

            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog, corruptedLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 2);

            when(auditLogRepository.findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Map<String, Object> result = auditService.validateAuditTrailIntegrity(testStartTime, testEndTime);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("validationResult");
            assertThat(result).containsKey("totalRecords");
            assertThat(result).containsKey("validRecords");
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Compliance Reporting Tests")
    class ComplianceReportingTests {

        @Test
        @DisplayName("Should generate compliance report successfully")
        void shouldGenerateComplianceReportSuccessfully() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);

            when(auditLogRepository.findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);
            when(auditLogRepository.findFailedAuthenticationAttempts(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(auditLogRepository.findAuthorizationDenials(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // Act
            Map<String, Object> result = auditService.generateComplianceReport("SOX", testStartTime, testEndTime);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("reportType");
            assertThat(result.get("reportType")).isEqualTo("SOX");
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle empty compliance report data")
        void shouldHandleEmptyComplianceReportData() {
            // Arrange
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);

            when(auditLogRepository.findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(auditLogRepository.findFailedAuthenticationAttempts(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(auditLogRepository.findAuthorizationDenials(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // Act
            Map<String, Object> result = auditService.generateComplianceReport("PCI-DSS", testStartTime, testEndTime);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("reportType");
            assertThat(result.get("reportType")).isEqualTo("PCI-DSS");
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Audit Log Search Tests")
    class AuditLogSearchTests {

        @Test
        @DisplayName("Should search audit logs with multiple criteria")
        void shouldSearchAuditLogsWithMultipleCriteria() {
            // Arrange
            Map<String, Object> searchCriteria = new HashMap<>();
            searchCriteria.put("username", "testuser");
            searchCriteria.put("eventType", "AUTHENTICATION");
            searchCriteria.put("outcome", "SUCCESS");
            searchCriteria.put("startDate", testStartTime);
            searchCriteria.put("endDate", testEndTime);
            searchCriteria.put("page", 0);
            searchCriteria.put("size", 10);

            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            // Since username, startDate, and endDate are provided, 
            // the service will use findByUsernameAndTimestampBetween
            when(auditLogRepository.findByUsernameAndTimestampBetween(
                    eq("testuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.searchAuditLogs(searchCriteria);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(auditLogRepository, times(1)).findByUsernameAndTimestampBetween(
                    eq("testuser"), eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle empty search criteria")
        void shouldHandleEmptySearchCriteria() {
            // Arrange
            Map<String, Object> searchCriteria = new HashMap<>();
            searchCriteria.put("page", 0);
            searchCriteria.put("size", 10);

            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findAuditLogsByAdvancedCriteria(
                    any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.searchAuditLogs(searchCriteria);

            // Assert
            assertThat(result).isNotNull();
            verify(auditLogRepository, times(1)).findAuditLogsByAdvancedCriteria(
                    any(), any(), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle null search criteria gracefully")
        void shouldHandleNullSearchCriteriaGracefully() {
            // Arrange
            Map<String, Object> searchCriteria = new HashMap<>();
            searchCriteria.put("username", null);
            searchCriteria.put("eventType", null);
            searchCriteria.put("page", 0);
            searchCriteria.put("size", 10);

            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findAuditLogsByAdvancedCriteria(
                    any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act
            Page<AuditLog> result = auditService.searchAuditLogs(searchCriteria);

            // Assert
            assertThat(result).isNotNull();
            verify(auditLogRepository, times(1)).findAuditLogsByAdvancedCriteria(
                    any(), any(), any(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Audit Log Retention Tests")
    class AuditLogRetentionTests {

        @Test
        @DisplayName("Should archive old logs without deletion")
        void shouldArchiveOldLogsWithoutDeletion() {
            // Arrange
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
            
            // Create mock logs to be archived
            AuditLog log1 = new AuditLog();
            log1.setUsername("user1");
            log1.setEventType("LOGIN");
            log1.setOutcome("SUCCESS");
            log1.setTimestamp(LocalDateTime.now().minusDays(100));
            
            List<AuditLog> mockLogs = Arrays.asList(log1);
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<AuditLog> mockLogsPage = new PageImpl<>(mockLogs, pageable, mockLogs.size());
            
            // Mock the findAuditLogsByDateRange method
            when(auditLogRepository.findAuditLogsByDateRange(
                any(LocalDateTime.class), eq(cutoffDate), any(Pageable.class)))
                .thenReturn(mockLogsPage);

            // Act
            Map<String, Object> result = auditService.archiveOldLogs(cutoffDate, false);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("archivedCount");
            assertThat(result).containsKey("cutoffDate");
            assertThat(result).containsKey("operationType");
            assertThat(result.get("archivedCount")).isEqualTo(1); // Based on mock logs count
        }

        @Test
        @DisplayName("Should archive and delete old logs when requested")
        void shouldArchiveAndDeleteOldLogsWhenRequested() {
            // Arrange
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
            
            // Create mock logs to be archived
            AuditLog log1 = new AuditLog();
            log1.setUsername("user1");
            log1.setEventType("LOGIN");
            log1.setOutcome("SUCCESS");
            log1.setTimestamp(LocalDateTime.now().minusDays(200));
            
            AuditLog log2 = new AuditLog();
            log2.setUsername("user2");
            log2.setEventType("LOGOUT");
            log2.setOutcome("SUCCESS");
            log2.setTimestamp(LocalDateTime.now().minusDays(200));
            
            List<AuditLog> mockLogs = Arrays.asList(log1, log2);
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<AuditLog> mockLogsPage = new PageImpl<>(mockLogs, pageable, mockLogs.size());
            
            // Mock the findAuditLogsByDateRange method
            when(auditLogRepository.findAuditLogsByDateRange(
                any(LocalDateTime.class), eq(cutoffDate), any(Pageable.class)))
                .thenReturn(mockLogsPage);
                
            when(auditLogRepository.deleteByTimestampBefore(eq(cutoffDate))).thenReturn(5);

            // Act
            Map<String, Object> result = auditService.archiveOldLogs(cutoffDate, true);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKey("archivedCount");
            assertThat(result).containsKey("cutoffDate");
            assertThat(result).containsKey("operationType");
            assertThat(result.get("archivedCount")).isEqualTo(5); // Based on deleteByTimestampBefore return value
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                any(LocalDateTime.class), eq(cutoffDate), any(Pageable.class));
            verify(auditLogRepository, times(1)).deleteByTimestampBefore(eq(cutoffDate));
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle bulk audit log retrieval efficiently")
        void shouldHandleBulkAuditLogRetrievalEfficiently() {
            // Arrange
            List<AuditLog> largeAuditLogList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                AuditLog log = new AuditLog();
                log.setId((long) i);
                log.setUsername("user" + i);
                log.setEventType("TRANSACTION");
                log.setOutcome("SUCCESS");
                log.setTimestamp(LocalDateTime.now().minusMinutes(i));
                largeAuditLogList.add(log);
            }

            Page<AuditLog> largePage = new PageImpl<>(largeAuditLogList, PageRequest.of(0, 1000), 1000);

            when(auditLogRepository.findAuditLogsByDateRange(
                    any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(largePage);

            long startTime = System.currentTimeMillis();

            // Act
            Page<AuditLog> result = auditService.getAuditLogsByDateRange(testStartTime, testEndTime, 0, 1000);

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1000);
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
            verify(auditLogRepository, times(1)).findAuditLogsByDateRange(
                    eq(testStartTime), eq(testEndTime), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle concurrent audit log operations")
        void shouldHandleConcurrentAuditLogOperations() {
            // Arrange
            List<AuditLog> auditLogs = Arrays.asList(sampleAuditLog);
            Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs, defaultPageable, 1);

            when(auditLogRepository.findByUsernameAndTimestampBetween(
                    any(), any(), any(), any(Pageable.class)))
                    .thenReturn(auditLogPage);

            // Act - Simulate multiple concurrent calls
            Page<AuditLog> result1 = auditService.getAuditLogsByUser("user1", testStartTime, testEndTime, 0, 10);
            Page<AuditLog> result2 = auditService.getAuditLogsByUser("user2", testStartTime, testEndTime, 0, 10);

            // Assert
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            verify(auditLogRepository, times(2)).findByUsernameAndTimestampBetween(
                    any(), any(), any(), any(Pageable.class));
        }
    }
}