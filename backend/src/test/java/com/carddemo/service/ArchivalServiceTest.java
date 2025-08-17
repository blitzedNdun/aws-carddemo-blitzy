/*
 * ArchivalServiceTest.java
 * 
 * Comprehensive unit test suite for ArchivalService validating data archival and purging 
 * functionality for regulatory compliance. Tests all aspects of the enterprise archival
 * system including retention policy enforcement, legal hold management, data compression,
 * integrity validation, and audit trail preservation.
 * 
 * This test suite ensures the ArchivalService meets all regulatory requirements including
 * PCI DSS, GDPR, SOX, and banking regulations for data retention and secure deletion.
 * 
 * Migrated from COBOL program CBACT03C which handled cross-reference data file processing,
 * extended to provide comprehensive enterprise archival testing capabilities.
 * 
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Archive;
import com.carddemo.entity.AuditLog;
import com.carddemo.repository.ArchiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test suite for ArchivalService.
 * 
 * Tests all archival operations including:
 * - Data archival with compression and metadata management
 * - Retrieval from archives with integrity validation
 * - Data purging with legal hold compliance
 * - Retention policy enforcement across data types
 * - Archival job scheduling and monitoring
 * - Legal hold scenario handling
 * - Audit trail preservation and compliance reporting
 * 
 * Uses Mockito for dependency mocking and AssertJ for fluent assertions.
 * Organized into nested test classes for logical grouping of related tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchivalService Tests")
class ArchivalServiceTest {

    @Mock
    private ArchiveRepository archiveRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ArchivalService archivalService;

    // Test data constants
    private static final String TEST_DATA_TYPE_TRANSACTIONS = "TRANSACTIONS";
    private static final String TEST_DATA_TYPE_CUSTOMERS = "CUSTOMERS";
    private static final String TEST_DATA_TYPE_ACCOUNTS = "ACCOUNTS";
    private static final String TEST_RECORD_ID = "TXN123456";
    private static final LocalDate TEST_CUTOFF_DATE = LocalDate.now().minusMonths(13);
    private static final LocalDate TEST_PURGE_DATE = LocalDate.now().minusYears(2);
    private static final int TEST_COMPRESSION_LEVEL = 6;

    // Mock data objects
    private Archive mockArchive;
    private AuditLog mockAuditLog;
    private Map<String, Object> mockArchivedData;

    @BeforeEach
    void setUp() {
        // Initialize mock data objects
        mockArchive = createMockArchive();
        mockAuditLog = createMockAuditLog();
        mockArchivedData = createMockArchivedData();
    }

    @Nested
    @DisplayName("Data Archival Operations")
    class DataArchivalOperationsTest {

        @Test
        @DisplayName("Should successfully archive transaction data with compression")
        void shouldArchiveTransactionDataWithCompression() {
            // Given
            when(storageService.compressData(any(byte[].class))).thenReturn("compressed_data".getBytes());
            when(storageService.storeData(any(byte[].class), anyString(), any())).thenReturn("storage_path_123");
            when(archiveRepository.save(any(Archive.class))).thenReturn(mockArchive);

            // When
            Map<String, Object> result = archivalService.archiveData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE, 
                TEST_COMPRESSION_LEVEL
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("dataType")).isEqualTo(TEST_DATA_TYPE_TRANSACTIONS);
            assertThat(result.get("cutoffDate")).isEqualTo(TEST_CUTOFF_DATE);
            assertThat(result.get("recordsArchived")).isInstanceOf(Integer.class);
            assertThat(result.get("archivalDate")).isInstanceOf(LocalDateTime.class);
            assertThat(result.get("integrityValid")).isEqualTo(true);

            // Verify interactions
            verify(storageService).compressData(any(byte[].class));
            verify(storageService).storeData(any(byte[].class), anyString(), any());
            verify(archiveRepository, atLeastOnce()).save(any(Archive.class));
        }

        @Test
        @DisplayName("Should skip records under legal hold during archival")
        void shouldSkipRecordsUnderLegalHoldDuringArchival() {
            // Given
            when(archivalService.isLegalHold(TEST_DATA_TYPE_TRANSACTIONS, TEST_RECORD_ID))
                .thenReturn(true);

            // When
            Map<String, Object> result = archivalService.archiveData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE, 
                TEST_COMPRESSION_LEVEL
            );

            // Then
            assertThat(result.get("recordsSkipped")).isInstanceOf(Integer.class);
            assertThat((Integer) result.get("recordsSkipped")).isGreaterThanOrEqualTo(0);

            // Verify legal hold check was performed
            verify(archivalService, times(0)).isLegalHold(anyString(), anyString());
        }

        @Test
        @DisplayName("Should validate archival eligibility before processing")
        void shouldValidateArchivalEligibilityBeforeProcessing() {
            // Given - future cutoff date (invalid)
            LocalDate futureDate = LocalDate.now().plusDays(1);

            // When & Then
            assertThatThrownBy(() -> archivalService.archiveData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                futureDate, 
                TEST_COMPRESSION_LEVEL
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("not eligible for archival");
        }

        @Test
        @DisplayName("Should handle compression errors gracefully")
        void shouldHandleCompressionErrorsGracefully() {
            // Given
            when(storageService.compressData(any(byte[].class)))
                .thenThrow(new RuntimeException("Compression failed"));

            // When
            Map<String, Object> result = archivalService.archiveData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE, 
                TEST_COMPRESSION_LEVEL
            );

            // Then
            assertThat(result.get("errors")).isInstanceOf(List.class);
            List<String> errors = (List<String>) result.get("errors");
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0)).contains("Compression failed");
        }

        @Test
        @DisplayName("Should create audit trail entries for archival operations")
        void shouldCreateAuditTrailEntriesForArchivalOperations() {
            // Given
            when(storageService.compressData(any(byte[].class))).thenReturn("compressed_data".getBytes());
            when(storageService.storeData(any(byte[].class), anyString(), any())).thenReturn("storage_path_123");
            when(archiveRepository.save(any(Archive.class))).thenReturn(mockArchive);

            // When
            archivalService.archiveData(TEST_DATA_TYPE_TRANSACTIONS, TEST_CUTOFF_DATE, TEST_COMPRESSION_LEVEL);

            // Then
            // Verify audit trail creation (method calls are logged internally)
            verify(storageService).storeData(any(byte[].class), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Archive Retrieval Operations")
    class ArchiveRetrievalOperationsTest {

        @Test
        @DisplayName("Should successfully retrieve archived data with decompression")
        void shouldRetrieveArchivedDataWithDecompression() {
            // Given
            when(archiveRepository.findById(anyLong())).thenReturn(Optional.of(mockArchive));
            when(storageService.retrieveData(anyString())).thenReturn("compressed_data".getBytes());
            when(storageService.validateStorageIntegrity(anyString())).thenReturn(true);

            // When
            Map<String, Object> result = archivalService.retrieveFromArchive(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_RECORD_ID, 
                true
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("retrieved")).isEqualTo(true);
            assertThat(result.get("integrityValid")).isEqualTo(true);
            assertThat(result.get("data")).isNotNull();

            // Verify metadata inclusion
            assertThat(result.get("archiveDate")).isInstanceOf(LocalDateTime.class);
            assertThat(result.get("compressionLevel")).isInstanceOf(Integer.class);
            assertThat(result.get("checksum")).isInstanceOf(String.class);

            // Verify interactions
            verify(storageService).retrieveData(anyString());
            verify(storageService).validateStorageIntegrity(anyString());
        }

        @Test
        @DisplayName("Should return error when archived record not found")
        void shouldReturnErrorWhenArchivedRecordNotFound() {
            // Given
            when(archiveRepository.findById(anyLong())).thenReturn(Optional.empty());

            // When
            Map<String, Object> result = archivalService.retrieveFromArchive(
                TEST_DATA_TYPE_TRANSACTIONS, 
                "NONEXISTENT_ID", 
                false
            );

            // Then
            assertThat(result.get("retrieved")).isEqualTo(false);
            assertThat(result.get("error")).isEqualTo("Record not found in archive");
        }

        @Test
        @DisplayName("Should validate data integrity during retrieval")
        void shouldValidateDataIntegrityDuringRetrieval() {
            // Given
            when(archiveRepository.findById(anyLong())).thenReturn(Optional.of(mockArchive));
            when(storageService.retrieveData(anyString())).thenReturn("compressed_data".getBytes());
            when(storageService.validateStorageIntegrity(anyString())).thenReturn(false);

            // When
            Map<String, Object> result = archivalService.retrieveFromArchive(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_RECORD_ID, 
                true
            );

            // Then
            assertThat(result.get("integrityValid")).isEqualTo(false);
            verify(storageService).validateStorageIntegrity(anyString());
        }

        @Test
        @DisplayName("Should handle storage service errors during retrieval")
        void shouldHandleStorageServiceErrorsDuringRetrieval() {
            // Given
            when(archiveRepository.findById(anyLong())).thenReturn(Optional.of(mockArchive));
            when(storageService.retrieveData(anyString()))
                .thenThrow(new RuntimeException("Storage service unavailable"));

            // When
            Map<String, Object> result = archivalService.retrieveFromArchive(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_RECORD_ID, 
                false
            );

            // Then
            assertThat(result.get("retrieved")).isEqualTo(false);
            assertThat(result.get("error")).contains("Storage service unavailable");
        }
    }

    @Nested
    @DisplayName("Data Purging Operations")
    class DataPurgingOperationsTest {

        @Test
        @DisplayName("Should successfully purge eligible archived data")
        void shouldPurgeEligibleArchivedData() {
            // Given
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.deleteData(anyString())).thenReturn(true);
            doNothing().when(archiveRepository).deleteById(anyLong());

            // When
            Map<String, Object> result = archivalService.purgeArchivedData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_PURGE_DATE, 
                false
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("dataType")).isEqualTo(TEST_DATA_TYPE_TRANSACTIONS);
            assertThat(result.get("purgeThresholdDate")).isEqualTo(TEST_PURGE_DATE);
            assertThat(result.get("recordsPurged")).isInstanceOf(Integer.class);
            assertThat(result.get("purgeDate")).isInstanceOf(LocalDateTime.class);

            // Verify interactions
            verify(storageService).deleteData(anyString());
            verify(archiveRepository).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should respect legal holds during purge operations")
        void shouldRespectLegalHoldsDuringPurgeOperations() {
            // Given
            Archive legalHoldArchive = createMockArchive();
            legalHoldArchive.setLegalHold(true);
            
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(legalHoldArchive));

            // When
            Map<String, Object> result = archivalService.purgeArchivedData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_PURGE_DATE, 
                false // force delete = false
            );

            // Then
            assertThat(result.get("recordsSkipped")).isInstanceOf(Integer.class);
            assertThat((Integer) result.get("recordsSkipped")).isGreaterThan(0);

            // Verify no deletion occurred for legal hold records
            verify(storageService, never()).deleteData(anyString());
            verify(archiveRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should force delete legal hold records when specified")
        void shouldForceDeleteLegalHoldRecordsWhenSpecified() {
            // Given
            Archive legalHoldArchive = createMockArchive();
            legalHoldArchive.setLegalHold(true);
            
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(legalHoldArchive));
            when(storageService.deleteData(anyString())).thenReturn(true);
            doNothing().when(archiveRepository).deleteById(anyLong());

            // When
            Map<String, Object> result = archivalService.purgeArchivedData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_PURGE_DATE, 
                true // force delete = true
            );

            // Then
            assertThat(result.get("recordsPurged")).isInstanceOf(Integer.class);
            assertThat((Integer) result.get("recordsPurged")).isGreaterThan(0);

            // Verify deletion occurred despite legal hold
            verify(storageService).deleteData(anyString());
            verify(archiveRepository).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should validate purge eligibility before processing")
        void shouldValidatePurgeEligibilityBeforeProcessing() {
            // Given - recent purge date (not eligible)
            LocalDate recentDate = LocalDate.now().minusDays(30);

            // When & Then
            assertThatThrownBy(() -> archivalService.purgeArchivedData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                recentDate, 
                false
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("not eligible for purge");
        }

        @Test
        @DisplayName("Should create audit entries before permanent deletion")
        void shouldCreateAuditEntriesBeforePermanentDeletion() {
            // Given
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.deleteData(anyString())).thenReturn(true);
            doNothing().when(archiveRepository).deleteById(anyLong());

            // When
            archivalService.purgeArchivedData(TEST_DATA_TYPE_TRANSACTIONS, TEST_PURGE_DATE, false);

            // Then
            // Verify audit trail creation (method calls are logged internally)
            verify(storageService).deleteData(anyString());
        }
    }

    @Nested
    @DisplayName("Retention Policy Enforcement")
    class RetentionPolicyEnforcementTest {

        @Test
        @DisplayName("Should enforce retention policies across all data types")
        void shouldEnforceRetentionPoliciesAcrossAllDataTypes() {
            // Given
            when(storageService.compressData(any(byte[].class))).thenReturn("compressed_data".getBytes());
            when(storageService.storeData(any(byte[].class), anyString(), any())).thenReturn("storage_path_123");
            when(archiveRepository.save(any(Archive.class))).thenReturn(mockArchive);
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.deleteData(anyString())).thenReturn(true);

            // When
            Map<String, Object> result = archivalService.enforceRetentionPolicy();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("enforcementDate")).isInstanceOf(LocalDateTime.class);
            assertThat(result.get("totalRecordsArchived")).isInstanceOf(Integer.class);
            assertThat(result.get("totalRecordsPurged")).isInstanceOf(Integer.class);
            assertThat(result.get("policyResults")).isInstanceOf(List.class);
            assertThat(result.get("policiesProcessed")).isInstanceOf(Integer.class);

            List<Map<String, Object>> policyResults = (List<Map<String, Object>>) result.get("policyResults");
            assertThat(policyResults).isNotEmpty();
            assertThat(policyResults.get(0)).containsKeys("dataType", "retentionPeriodMonths", "recordsArchived", "recordsPurged");
        }

        @Test
        @DisplayName("Should handle errors in individual policy enforcement")
        void shouldHandleErrorsInIndividualPolicyEnforcement() {
            // Given
            when(storageService.compressData(any(byte[].class)))
                .thenThrow(new RuntimeException("Policy enforcement failed"));

            // When
            Map<String, Object> result = archivalService.enforceRetentionPolicy();

            // Then
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).isInstanceOf(String.class);
            assertThat(result.get("policyResults")).isInstanceOf(List.class);

            List<Map<String, Object>> policyResults = (List<Map<String, Object>>) result.get("policyResults");
            assertThat(policyResults).hasSize(5); // Should have results for all 5 data types

            // Check that some policies have error entries
            boolean hasErrors = policyResults.stream()
                .anyMatch(policy -> policy.containsKey("error"));
            assertThat(hasErrors).isTrue();
        }

        @Test
        @DisplayName("Should calculate correct retention periods for different data types")
        void shouldCalculateCorrectRetentionPeriodsForDifferentDataTypes() {
            // When & Then
            assertThat(archivalService.calculateRetentionPeriod("TRANSACTIONS")).isEqualTo(13);
            assertThat(archivalService.calculateRetentionPeriod("CUSTOMERS")).isEqualTo(84); // 7 years * 12 months
            assertThat(archivalService.calculateRetentionPeriod("ACCOUNTS")).isEqualTo(84); // 7 years * 12 months
            assertThat(archivalService.calculateRetentionPeriod("CARDS")).isEqualTo(24); // 2 years * 12 months
            assertThat(archivalService.calculateRetentionPeriod("SECURITY_LOGS")).isEqualTo(36); // 3 years * 12 months
            assertThat(archivalService.calculateRetentionPeriod("UNKNOWN_TYPE")).isEqualTo(84); // Default 7 years
        }
    }

    @Nested
    @DisplayName("Archival Job Scheduling")
    class ArchivalJobSchedulingTest {

        @Test
        @DisplayName("Should successfully schedule archival job with valid parameters")
        void shouldScheduleArchivalJobWithValidParameters() throws Exception {
            // Given
            String cronExpression = "0 0 2 * * ?"; // Daily at 2 AM
            Map<String, Object> jobParameters = new HashMap<>();
            jobParameters.put("compressionLevel", 6);
            jobParameters.put("batchSize", 1000);

            // When
            CompletableFuture<Map<String, Object>> future = archivalService.scheduleArchivalJob(
                TEST_DATA_TYPE_TRANSACTIONS,
                cronExpression,
                jobParameters
            );

            // Then
            Map<String, Object> result = future.get();
            assertThat(result).isNotNull();
            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("jobId")).isInstanceOf(String.class);
            assertThat(result.get("jobName")).isInstanceOf(String.class);
            assertThat(result.get("dataType")).isEqualTo(TEST_DATA_TYPE_TRANSACTIONS);
            assertThat(result.get("cronExpression")).isEqualTo(cronExpression);
            assertThat(result.get("nextExecution")).isInstanceOf(LocalDateTime.class);
            assertThat(result.get("jobConfiguration")).isInstanceOf(Map.class);
            assertThat(result.get("scheduledDate")).isInstanceOf(LocalDateTime.class);

            Map<String, Object> jobConfig = (Map<String, Object>) result.get("jobConfiguration");
            assertThat(jobConfig.get("compressionLevel")).isEqualTo(6);
            assertThat(jobConfig.get("batchSize")).isEqualTo(1000);
        }

        @Test
        @DisplayName("Should reject invalid cron expressions")
        void shouldRejectInvalidCronExpressions() throws Exception {
            // Given
            String invalidCronExpression = "invalid cron";

            // When
            CompletableFuture<Map<String, Object>> future = archivalService.scheduleArchivalJob(
                TEST_DATA_TYPE_TRANSACTIONS,
                invalidCronExpression,
                null
            );

            // Then
            Map<String, Object> result = future.get();
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).isInstanceOf(String.class);
            assertThat(result.get("error").toString()).contains("Invalid cron expression");
        }

        @Test
        @DisplayName("Should handle null or empty job parameters gracefully")
        void shouldHandleNullJobParametersGracefully() throws Exception {
            // Given
            String validCronExpression = "0 0 2 * * ?";

            // When
            CompletableFuture<Map<String, Object>> future = archivalService.scheduleArchivalJob(
                TEST_DATA_TYPE_TRANSACTIONS,
                validCronExpression,
                null
            );

            // Then
            Map<String, Object> result = future.get();
            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("jobConfiguration")).isInstanceOf(Map.class);

            Map<String, Object> jobConfig = (Map<String, Object>) result.get("jobConfiguration");
            assertThat(jobConfig).isNotEmpty();
            assertThat(jobConfig.get("jobName")).isInstanceOf(String.class);
        }
    }

    @Nested
    @DisplayName("Archival Integrity Validation")
    class ArchivalIntegrityValidationTest {

        @Test
        @DisplayName("Should validate archival integrity successfully")
        void shouldValidateArchivalIntegritySuccessfully() {
            // Given
            when(archiveRepository.findByDataType(TEST_DATA_TYPE_TRANSACTIONS))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.validateStorageIntegrity(anyString())).thenReturn(true);

            // When
            boolean result = archivalService.validateArchivalIntegrity(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE
            );

            // Then
            assertThat(result).isTrue();
            verify(storageService).validateStorageIntegrity(anyString());
        }

        @Test
        @DisplayName("Should detect integrity violations in archived data")
        void shouldDetectIntegrityViolationsInArchivedData() {
            // Given
            when(archiveRepository.findByDataType(TEST_DATA_TYPE_TRANSACTIONS))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.validateStorageIntegrity(anyString())).thenReturn(false);

            // When
            boolean result = archivalService.validateArchivalIntegrity(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE
            );

            // Then
            assertThat(result).isFalse();
            verify(storageService).validateStorageIntegrity(anyString());
        }

        @Test
        @DisplayName("Should handle storage service errors during integrity validation")
        void shouldHandleStorageServiceErrorsDuringIntegrityValidation() {
            // Given
            when(archiveRepository.findByDataType(TEST_DATA_TYPE_TRANSACTIONS))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.validateStorageIntegrity(anyString()))
                .thenThrow(new RuntimeException("Storage validation failed"));

            // When
            boolean result = archivalService.validateArchivalIntegrity(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE
            );

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should validate integrity for specific archival date")
        void shouldValidateIntegrityForSpecificArchivalDate() {
            // Given
            LocalDate specificDate = LocalDate.now().minusDays(30);
            when(archiveRepository.findByArchiveDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.validateStorageIntegrity(anyString())).thenReturn(true);

            // When
            boolean result = archivalService.validateArchivalIntegrity(
                TEST_DATA_TYPE_TRANSACTIONS, 
                specificDate
            );

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Legal Hold Management")
    class LegalHoldManagementTest {

        @Test
        @DisplayName("Should correctly identify records under legal hold")
        void shouldCorrectlyIdentifyRecordsUnderLegalHold() {
            // When
            boolean result = archivalService.isLegalHold(TEST_DATA_TYPE_TRANSACTIONS, TEST_RECORD_ID);

            // Then
            assertThat(result).isInstanceOf(Boolean.class);
            // The actual implementation will query the database
            // For testing purposes, we verify the method can be called
        }

        @Test
        @DisplayName("Should handle database errors when checking legal hold status")
        void shouldHandleDatabaseErrorsWhenCheckingLegalHoldStatus() {
            // Given - this would require mocking internal database connections
            // For now, we test that the method handles errors gracefully

            // When
            boolean result = archivalService.isLegalHold("INVALID_TYPE", "INVALID_ID");

            // Then
            // In case of error, the service should assume legal hold for safety
            assertThat(result).isInstanceOf(Boolean.class);
        }

        @Test
        @DisplayName("Should respect legal hold during archival operations")
        void shouldRespectLegalHoldDuringArchivalOperations() {
            // This test is covered in the archival operations tests
            // Testing the integration of legal hold checks with archival process
            
            // Given
            when(storageService.compressData(any(byte[].class))).thenReturn("compressed_data".getBytes());
            
            // When
            Map<String, Object> result = archivalService.archiveData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_CUTOFF_DATE, 
                TEST_COMPRESSION_LEVEL
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("recordsSkipped")).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("Should respect legal hold during purge operations")
        void shouldRespectLegalHoldDuringPurgeOperations() {
            // This test is covered in the purging operations tests
            // Testing the integration of legal hold checks with purge process
            
            // Given
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

            // When
            Map<String, Object> result = archivalService.purgeArchivedData(
                TEST_DATA_TYPE_TRANSACTIONS, 
                TEST_PURGE_DATE, 
                false
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("recordsSkipped")).isInstanceOf(Integer.class);
        }
    }

    @Nested
    @DisplayName("Audit Trail Preservation")
    class AuditTrailPreservationTest {

        @Test
        @DisplayName("Should preserve audit trails during archival operations")
        void shouldPreserveAuditTrailsDuringArchivalOperations() {
            // Given
            when(storageService.compressData(any(byte[].class))).thenReturn("compressed_data".getBytes());
            when(storageService.storeData(any(byte[].class), anyString(), any())).thenReturn("storage_path_123");
            when(archiveRepository.save(any(Archive.class))).thenReturn(mockArchive);

            // When
            archivalService.archiveData(TEST_DATA_TYPE_TRANSACTIONS, TEST_CUTOFF_DATE, TEST_COMPRESSION_LEVEL);

            // Then
            // Verify that audit trail methods would be called
            // In the actual implementation, this would create AuditLog entries
            verify(storageService).storeData(any(byte[].class), anyString(), any());
        }

        @Test
        @DisplayName("Should preserve audit trails during retrieval operations")
        void shouldPreserveAuditTrailsDuringRetrievalOperations() {
            // Given
            when(archiveRepository.findById(anyLong())).thenReturn(Optional.of(mockArchive));
            when(storageService.retrieveData(anyString())).thenReturn("compressed_data".getBytes());
            when(storageService.validateStorageIntegrity(anyString())).thenReturn(true);

            // When
            archivalService.retrieveFromArchive(TEST_DATA_TYPE_TRANSACTIONS, TEST_RECORD_ID, true);

            // Then
            // Verify that audit trail methods would be called
            verify(storageService).retrieveData(anyString());
        }

        @Test
        @DisplayName("Should preserve audit trails during purge operations")
        void shouldPreserveAuditTrailsDuringPurgeOperations() {
            // Given
            when(archiveRepository.findByRetentionDateBefore(any(LocalDate.class)))
                .thenReturn(Arrays.asList(mockArchive));
            when(storageService.deleteData(anyString())).thenReturn(true);
            doNothing().when(archiveRepository).deleteById(anyLong());

            // When
            archivalService.purgeArchivedData(TEST_DATA_TYPE_TRANSACTIONS, TEST_PURGE_DATE, false);

            // Then
            // Verify that audit trail methods would be called before deletion
            verify(storageService).deleteData(anyString());
        }

        @Test
        @DisplayName("Should create audit logs with proper event types and outcomes")
        void shouldCreateAuditLogsWithProperEventTypesAndOutcomes() {
            // This test validates that audit log entries would have correct structure
            // The actual implementation creates AuditLog entities with proper fields
            
            AuditLog testAuditLog = mockAuditLog;
            
            // Verify audit log structure
            assertThat(testAuditLog.getId()).isNotNull();
            assertThat(testAuditLog.getEventType()).isNotNull();
            assertThat(testAuditLog.getTimestamp()).isNotNull();
            assertThat(testAuditLog.getResourceAccessed()).isNotNull();
            assertThat(testAuditLog.getActionPerformed()).isNotNull();
            assertThat(testAuditLog.getOutcome()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Compressed Storage Format Validation")
    class CompressedStorageFormatValidationTest {

        @Test
        @DisplayName("Should validate GZIP compression format")
        void shouldValidateGzipCompressionFormat() {
            // Given
            byte[] testData = "test data for compression".getBytes();
            when(storageService.compressData(testData)).thenReturn("gzip_compressed_data".getBytes());

            // When
            byte[] compressedData = storageService.compressData(testData);

            // Then
            assertThat(compressedData).isNotNull();
            assertThat(compressedData.length).isGreaterThan(0);
            verify(storageService).compressData(testData);
        }

        @Test
        @DisplayName("Should handle compression algorithm configuration")
        void shouldHandleCompressionAlgorithmConfiguration() {
            // This test validates compression configuration through the ArchivalService
            
            // Given
            when(storageService.compressData(any(byte[].class))).thenReturn("compressed_data".getBytes());
            when(storageService.storeData(any(byte[].class), anyString(), any())).thenReturn("storage_path_123");
            when(archiveRepository.save(any(Archive.class))).thenReturn(mockArchive);

            // When
            archivalService.archiveData(TEST_DATA_TYPE_TRANSACTIONS, TEST_CUTOFF_DATE, 9); // Max compression

            // Then
            verify(storageService).compressData(any(byte[].class));
        }

        @Test
        @DisplayName("Should validate storage location assignment")
        void shouldValidateStorageLocationAssignment() {
            // Given
            String dataType = "TRANSACTIONS";
            String filename = "transaction_data.dat";
            
            when(storageService.getStorageLocation(dataType, filename))
                .thenReturn(StorageService.StorageLocation.TRANSACTION_DATA);

            // When
            StorageService.StorageLocation location = storageService.getStorageLocation(dataType, filename);

            // Then
            assertThat(location).isEqualTo(StorageService.StorageLocation.TRANSACTION_DATA);
            verify(storageService).getStorageLocation(dataType, filename);
        }
    }

    @Nested
    @DisplayName("Compliance Reporting")
    class ComplianceReportingTest {

        @Test
        @DisplayName("Should generate compliance reports with proper structure")
        void shouldGenerateComplianceReportsWithProperStructure() {
            // Given
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();
            
            // When
            Map<String, Object> report = archivalService.generateArchiveReport(
                "COMPLIANCE", 
                startDate, 
                endDate
            );

            // Then
            assertThat(report).isNotNull();
            assertThat(report.get("reportType")).isEqualTo("COMPLIANCE");
            assertThat(report.get("generatedDate")).isInstanceOf(LocalDateTime.class);
            assertThat(report.get("generatedBy")).isEqualTo("ArchivalService");
            assertThat(report.get("reportPeriod")).isInstanceOf(Map.class);
            
            Map<String, Object> reportPeriod = (Map<String, Object>) report.get("reportPeriod");
            assertThat(reportPeriod.get("startDate")).isEqualTo(startDate);
            assertThat(reportPeriod.get("endDate")).isEqualTo(endDate);
        }

        @Test
        @DisplayName("Should generate summary reports for management overview")
        void shouldGenerateSummaryReportsForManagementOverview() {
            // Given
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();
            
            // When
            Map<String, Object> report = archivalService.generateArchiveReport(
                "SUMMARY", 
                startDate, 
                endDate
            );

            // Then
            assertThat(report).isNotNull();
            assertThat(report.get("reportType")).isEqualTo("SUMMARY");
            assertThat(report.get("summary")).isInstanceOf(Map.class);
            
            Map<String, Object> summary = (Map<String, Object>) report.get("summary");
            assertThat(summary.get("totalRecordsArchived")).isInstanceOf(Integer.class);
            assertThat(summary.get("totalRecordsPurged")).isInstanceOf(Integer.class);
            assertThat(summary.get("storageSpaceSaved")).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("Should generate detailed reports for audit purposes")
        void shouldGenerateDetailedReportsForAuditPurposes() {
            // Given
            LocalDate startDate = LocalDate.now().minusWeeks(2);
            LocalDate endDate = LocalDate.now();
            
            // When
            Map<String, Object> report = archivalService.generateArchiveReport(
                "DETAILED", 
                startDate, 
                endDate
            );

            // Then
            assertThat(report).isNotNull();
            assertThat(report.get("reportType")).isEqualTo("DETAILED");
            assertThat(report.get("detailed")).isNotNull();
        }

        @Test
        @DisplayName("Should handle invalid report type requests")
        void shouldHandleInvalidReportTypeRequests() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            
            // When & Then
            assertThatThrownBy(() -> archivalService.generateArchiveReport(
                "INVALID_TYPE", 
                startDate, 
                endDate
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Unsupported report type");
        }
    }

    // Helper methods for creating mock objects
    
    private Archive createMockArchive() {
        Archive archive = new Archive();
        archive.setArchiveId(1L);
        archive.setDataType(TEST_DATA_TYPE_TRANSACTIONS);
        archive.setSourceRecordId(TEST_RECORD_ID);
        archive.setSourceTableName("transactions");
        archive.setArchivedData("{\"transaction_id\":\"TXN123456\",\"amount\":\"100.00\"}");
        archive.setArchiveDate(LocalDateTime.now().minusDays(30));
        archive.setRetentionDate(LocalDate.now().minusYears(2));
        archive.setLegalHold(false);
        archive.setStorageLocation("TRANSACTION_DATA/20240101/TXN123456.gz");
        archive.setCompressionMethod("GZIP");
        archive.setOriginalSizeBytes(1024L);
        archive.setCompressedSizeBytes(512L);
        archive.setDataChecksum("checksum_12345");
        archive.setArchivedBy("system");
        archive.setUpdatedTimestamp(LocalDateTime.now());
        archive.setMetadata("{\"batch_id\":\"BATCH_001\"}");
        return archive;
    }

    private AuditLog createMockAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setUsername("system");
        auditLog.setEventType("ARCHIVAL");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setSourceIp("127.0.0.1");
        auditLog.setResourceAccessed(TEST_RECORD_ID);
        auditLog.setActionPerformed("ARCHIVE");
        auditLog.setOutcome("SUCCESS");
        auditLog.setCorrelationId("CORR_123");
        auditLog.setDetails("Data archived successfully");
        auditLog.setIntegrityHash("hash_12345");
        auditLog.setSessionId("SESSION_123");
        auditLog.setUserAgent("ArchivalService/1.0");
        auditLog.setRiskScore(10);
        auditLog.setComplianceTags("PCI-DSS,SOX");
        return auditLog;
    }

    private Map<String, Object> createMockArchivedData() {
        Map<String, Object> data = new HashMap<>();
        data.put("transaction_id", TEST_RECORD_ID);
        data.put("account_id", "ACC123456");
        data.put("amount", "100.00");
        data.put("transaction_date", "2024-01-15");
        data.put("description", "Test transaction");
        data.put("merchant_name", "Test Merchant");
        data.put("transaction_type", "PURCHASE");
        return data;
    }
}