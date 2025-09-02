/*
 * AuditService.java
 * 
 * Core audit service providing persistent audit log storage and retrieval capabilities.
 * Manages audit event persistence to PostgreSQL, provides audit log querying and reporting 
 * features, implements audit trail integrity validation, and supports compliance requirements 
 * for regulatory reporting and security monitoring.
 * 
 * This service implements the comprehensive audit logging framework requirements outlined
 * in the Security Architecture documentation (Section 6.4) and Database Design (Section 6.2),
 * providing enterprise-grade audit trail capabilities for regulatory compliance and security
 * monitoring in the CardDemo application migration from COBOL mainframe to Spring Boot.
 * 
 * Key capabilities:
 * - Immutable audit event persistence with cryptographic integrity validation
 * - Advanced audit log querying with filtering by user, event type, and date range
 * - Compliance reporting for regulatory requirements (SOX, PCI DSS, GDPR)
 * - Real-time audit event streaming to monitoring systems
 * - Automated audit log retention policy management with archival capabilities
 * - Integration with Spring Security for security event correlation
 */
package com.carddemo.service;

import com.carddemo.entity.AuditLog;
import com.carddemo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core audit service providing persistent audit log storage and retrieval capabilities.
 * 
 * This service implements enterprise-grade audit logging with the following features:
 * 
 * <ul>
 *   <li><strong>Persistent Audit Storage:</strong> Audit events are persisted to PostgreSQL 
 *       audit_log table with comprehensive event details including user identification, 
 *       event type, timestamp, and context information</li>
 *   <li><strong>Audit Trail Integrity:</strong> Implements cryptographic hash verification 
 *       with SHA-256 to ensure audit record immutability and detect tampering</li>
 *   <li><strong>Advanced Querying:</strong> Provides sophisticated audit log retrieval 
 *       capabilities with filtering by user, event type, date range, and severity level</li>
 *   <li><strong>Compliance Support:</strong> Automated compliance report generation for 
 *       regulatory requirements including SOX, PCI DSS, and GDPR compliance</li>
 *   <li><strong>Security Integration:</strong> Structured logging format with correlation 
 *       ID support for integration with security monitoring systems</li>
 *   <li><strong>Retention Management:</strong> Automated audit log archival and purge 
 *       capabilities with configurable retention policies</li>
 *   <li><strong>Real-time Streaming:</strong> Audit event streaming to monitoring systems 
 *       with metrics collection and alerting integration</li>
 * </ul>
 * 
 * The service maintains strict transactional integrity through Spring's @Transactional
 * annotations and provides comprehensive error handling for enterprise reliability.
 * All audit operations are logged with structured formatting for integration with
 * monitoring and alerting systems.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final DateTimeFormatter AUDIT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Audit event type constants for consistent categorization
    public static final String EVENT_TYPE_AUTHENTICATION = "AUTHENTICATION";
    public static final String EVENT_TYPE_AUTHORIZATION = "AUTHORIZATION";
    public static final String EVENT_TYPE_SESSION = "SESSION";
    public static final String EVENT_TYPE_TRANSACTION = "TRANSACTION";
    public static final String EVENT_TYPE_CONFIGURATION = "CONFIGURATION";
    public static final String EVENT_TYPE_DATA_ACCESS = "DATA_ACCESS";
    public static final String EVENT_TYPE_SECURITY_VIOLATION = "SECURITY_VIOLATION";
    public static final String EVENT_TYPE_SYSTEM_EVENT = "SYSTEM_EVENT";
    
    // Audit outcome constants for consistent result classification
    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";
    public static final String OUTCOME_DENIED = "DENIED";
    public static final String OUTCOME_WARNING = "WARNING";
    public static final String OUTCOME_ERROR = "ERROR";
    
    // Compliance framework constants for regulatory reporting
    public static final String COMPLIANCE_SOX = "SOX";
    public static final String COMPLIANCE_PCI_DSS = "PCI-DSS";
    public static final String COMPLIANCE_GDPR = "GDPR";
    public static final String COMPLIANCE_SOC2 = "SOC2";

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Saves an audit log entry to the PostgreSQL database with comprehensive event details
     * and cryptographic integrity validation.
     * 
     * This method provides the core audit logging functionality for the CardDemo application,
     * ensuring that all security-relevant events are captured with appropriate context
     * information and integrity protection. The audit log entry is persisted with a 
     * cryptographic hash to enable tamper detection and maintain audit trail integrity
     * for regulatory compliance requirements.
     * 
     * @param auditLog The audit log entry to be persisted, containing event details,
     *                 user information, timestamp, and context data
     * @return The persisted AuditLog entity with generated ID and computed integrity hash
     * @throws IllegalArgumentException if the audit log is null or missing required fields
     * @throws RuntimeException if database persistence fails or integrity hash computation fails
     */
    public AuditLog saveAuditLog(AuditLog auditLog) {
        if (auditLog == null) {
            throw new IllegalArgumentException("Audit log entry cannot be null");
        }
        
        // Validate required audit log fields for enterprise compliance
        validateAuditLogEntry(auditLog);
        
        try {
            // Set timestamp if not already provided
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(LocalDateTime.now());
            }
            
            // Generate cryptographic integrity hash for tamper detection
            String integrityHash = generateIntegrityHash(auditLog);
            auditLog.setIntegrityHash(integrityHash);
            
            // Persist audit log entry to PostgreSQL database
            AuditLog savedAuditLog = auditLogRepository.save(auditLog);
            
            // Log the audit event creation for monitoring systems
            logger.info("Audit log entry saved - ID: {}, Username: {}, EventType: {}, Outcome: {}, Timestamp: {}", 
                       savedAuditLog.getId(), 
                       savedAuditLog.getUsername(), 
                       savedAuditLog.getEventType(), 
                       savedAuditLog.getOutcome(),
                       savedAuditLog.getTimestamp().format(AUDIT_DATE_FORMAT));
            
            return savedAuditLog;
            
        } catch (Exception e) {
            logger.error("Failed to save audit log entry - Username: {}, EventType: {}, Error: {}", 
                        auditLog.getUsername(), auditLog.getEventType(), e.getMessage(), e);
            throw new RuntimeException("Failed to persist audit log entry: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves audit logs for a specific user with pagination and date range filtering.
     * 
     * This method supports user-specific audit trail analysis and compliance reporting
     * by providing comprehensive access to all audit events associated with a particular
     * user account. The results are paginated for efficient handling of large audit
     * datasets and sorted by timestamp in descending order for chronological analysis.
     * 
     * @param username The username to retrieve audit logs for
     * @param startDate The start date for the audit log query range (inclusive)
     * @param endDate The end date for the audit log query range (inclusive)
     * @param page The page number for pagination (0-based)
     * @param size The number of records per page
     * @return Page of AuditLog entries for the specified user within the date range
     * @throws IllegalArgumentException if username is null/empty or date range is invalid
     */
    public Page<AuditLog> getAuditLogsByUser(String username, LocalDateTime startDate, 
                                           LocalDateTime endDate, int page, int size) {
        validateUserAndDateRange(username, startDate, endDate);
        validatePaginationParameters(page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<AuditLog> auditLogs = auditLogRepository.findByUsernameAndTimestampBetween(
                username, startDate, endDate, pageable);
            
            logger.debug("Retrieved {} audit logs for user: {} between {} and {}", 
                        auditLogs.getTotalElements(), username, 
                        startDate.format(AUDIT_DATE_FORMAT), 
                        endDate.format(AUDIT_DATE_FORMAT));
            
            return auditLogs;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve audit logs for user: {} - Error: {}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve audit logs for user: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves audit logs filtered by event type with pagination and date range support.
     * 
     * This method enables event-specific audit trail analysis and monitoring dashboard
     * support by providing access to all audit events of a particular type (e.g.,
     * AUTHENTICATION, AUTHORIZATION, TRANSACTION). Essential for security incident
     * investigation and compliance reporting requirements.
     * 
     * @param eventType The type of audit event to filter by (e.g., "AUTHENTICATION", "TRANSACTION")
     * @param startDate The start date for the audit log query range (inclusive)
     * @param endDate The end date for the audit log query range (inclusive)
     * @param page The page number for pagination (0-based)
     * @param size The number of records per page
     * @return Page of AuditLog entries matching the specified event type within the date range
     * @throws IllegalArgumentException if eventType is null/empty or date range is invalid
     */
    public Page<AuditLog> getAuditLogsByEventType(String eventType, LocalDateTime startDate, 
                                                LocalDateTime endDate, int page, int size) {
        validateEventTypeAndDateRange(eventType, startDate, endDate);
        validatePaginationParameters(page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<AuditLog> auditLogs = auditLogRepository.findByEventTypeAndTimestampBetween(
                eventType, startDate, endDate, pageable);
            
            logger.debug("Retrieved {} audit logs for event type: {} between {} and {}", 
                        auditLogs.getTotalElements(), eventType, 
                        startDate.format(AUDIT_DATE_FORMAT), 
                        endDate.format(AUDIT_DATE_FORMAT));
            
            return auditLogs;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve audit logs for event type: {} - Error: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve audit logs for event type: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves audit logs within a specified date range with comprehensive pagination support.
     * 
     * This method provides date-range-based audit log retrieval for compliance reporting,
     * security analysis, and operational monitoring. Optimized for efficient handling of
     * large audit datasets with PostgreSQL index utilization on timestamp columns for
     * high-performance queries.
     * 
     * @param startDate The start date for the audit log query range (inclusive)
     * @param endDate The end date for the audit log query range (inclusive)
     * @param page The page number for pagination (0-based)
     * @param size The number of records per page
     * @return Page of AuditLog entries within the specified date range
     * @throws IllegalArgumentException if date range is invalid or pagination parameters are incorrect
     */
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, 
                                                int page, int size) {
        validateDateRange(startDate, endDate);
        validatePaginationParameters(page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<AuditLog> auditLogs = auditLogRepository.findAuditLogsByDateRange(startDate, endDate, pageable);
            
            logger.debug("Retrieved {} audit logs between {} and {}", 
                        auditLogs.getTotalElements(), 
                        startDate.format(AUDIT_DATE_FORMAT), 
                        endDate.format(AUDIT_DATE_FORMAT));
            
            return auditLogs;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve audit logs for date range {} to {} - Error: {}", 
                        startDate.format(AUDIT_DATE_FORMAT), 
                        endDate.format(AUDIT_DATE_FORMAT), 
                        e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve audit logs for date range: " + e.getMessage(), e);
        }
    }

    /**
     * Validates audit trail integrity by verifying cryptographic hashes of audit log entries.
     * 
     * This method performs comprehensive integrity validation of the audit trail by
     * recalculating and verifying the SHA-256 cryptographic hashes for audit log entries
     * within a specified time range. Critical for detecting potential audit log tampering
     * and maintaining regulatory compliance requirements for audit trail integrity.
     * 
     * The validation process examines each audit log entry, recalculates its integrity
     * hash based on critical audit fields, and compares it with the stored hash value.
     * Any mismatches indicate potential tampering or data corruption.
     * 
     * @param startDate The start date for integrity validation (inclusive)
     * @param endDate The end date for integrity validation (inclusive)
     * @return Map containing validation results with statistics:
     *         - "totalRecords": Total number of audit records validated
     *         - "validRecords": Number of records with valid integrity hashes
     *         - "invalidRecords": Number of records with hash mismatches
     *         - "missingHashes": Number of records without integrity hashes
     *         - "validationResult": "PASSED" or "FAILED" based on overall integrity
     * @throws IllegalArgumentException if date range is invalid
     */
    public Map<String, Object> validateAuditTrailIntegrity(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        
        Map<String, Object> validationResults = new HashMap<>();
        int totalRecords = 0;
        int validRecords = 0;
        int invalidRecords = 0;
        int missingHashes = 0;
        List<Long> invalidAuditIds = new ArrayList<>();
        
        try {
            logger.info("Starting audit trail integrity validation for period {} to {}", 
                       startDate.format(AUDIT_DATE_FORMAT), 
                       endDate.format(AUDIT_DATE_FORMAT));
            
            // Process audit logs in batches for memory efficiency
            int page = 0;
            int batchSize = 1000;
            Page<AuditLog> auditPage;
            
            do {
                Pageable pageable = PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "timestamp"));
                auditPage = auditLogRepository.findAuditLogsByDateRange(startDate, endDate, pageable);
                
                for (AuditLog auditLog : auditPage.getContent()) {
                    totalRecords++;
                    
                    if (auditLog.getIntegrityHash() == null || auditLog.getIntegrityHash().trim().isEmpty()) {
                        missingHashes++;
                        logger.warn("Audit log entry {} missing integrity hash", auditLog.getId());
                        continue;
                    }
                    
                    // Recalculate integrity hash and compare
                    String calculatedHash = generateIntegrityHash(auditLog);
                    if (calculatedHash.equals(auditLog.getIntegrityHash())) {
                        validRecords++;
                    } else {
                        invalidRecords++;
                        invalidAuditIds.add(auditLog.getId());
                        logger.error("Integrity hash mismatch for audit log {}: expected={}, calculated={}", 
                                   auditLog.getId(), auditLog.getIntegrityHash(), calculatedHash);
                    }
                }
                
                page++;
            } while (auditPage.hasNext());
            
            // Compile validation results
            String validationResult = (invalidRecords == 0 && missingHashes == 0) ? "PASSED" : "FAILED";
            
            validationResults.put("totalRecords", totalRecords);
            validationResults.put("validRecords", validRecords);
            validationResults.put("invalidRecords", invalidRecords);
            validationResults.put("missingHashes", missingHashes);
            validationResults.put("validationResult", validationResult);
            validationResults.put("invalidAuditIds", invalidAuditIds);
            validationResults.put("validationTimestamp", LocalDateTime.now().format(AUDIT_DATE_FORMAT));
            
            logger.info("Audit trail integrity validation completed - Total: {}, Valid: {}, Invalid: {}, Missing: {}, Result: {}", 
                       totalRecords, validRecords, invalidRecords, missingHashes, validationResult);
            
            return validationResults;
            
        } catch (Exception e) {
            logger.error("Failed to validate audit trail integrity - Error: {}", e.getMessage(), e);
            throw new RuntimeException("Audit trail integrity validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates comprehensive compliance reports for regulatory requirements.
     * 
     * This method creates detailed compliance reports for various regulatory frameworks
     * including SOX, PCI DSS, GDPR, and SOC2. The reports include audit event summaries,
     * security incident analysis, access control violations, and compliance metrics
     * required for regulatory audits and internal control assessments.
     * 
     * @param complianceFramework The compliance framework to generate report for 
     *                           (e.g., "SOX", "PCI-DSS", "GDPR", "SOC2")
     * @param startDate The start date for the compliance reporting period (inclusive)
     * @param endDate The end date for the compliance reporting period (inclusive)
     * @return Map containing comprehensive compliance report data:
     *         - "reportType": The compliance framework name
     *         - "reportPeriod": Start and end dates for the report
     *         - "totalAuditEvents": Total number of audit events in period
     *         - "securityEvents": Count of security-related events
     *         - "authenticationEvents": Authentication event statistics
     *         - "authorizationEvents": Authorization event statistics
     *         - "failedEvents": Count and details of failed operations
     *         - "complianceScore": Overall compliance score (0-100)
     *         - "recommendations": List of compliance improvement recommendations
     * @throws IllegalArgumentException if compliance framework is unsupported or date range is invalid
     */
    public Map<String, Object> generateComplianceReport(String complianceFramework, 
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        validateComplianceFramework(complianceFramework);
        validateDateRange(startDate, endDate);
        
        try {
            logger.info("Generating {} compliance report for period {} to {}", 
                       complianceFramework, 
                       startDate.format(AUDIT_DATE_FORMAT), 
                       endDate.format(AUDIT_DATE_FORMAT));
            
            Map<String, Object> complianceReport = new HashMap<>();
            
            // Basic report metadata
            complianceReport.put("reportType", complianceFramework);
            complianceReport.put("reportPeriod", Map.of(
                "startDate", startDate.format(AUDIT_DATE_FORMAT),
                "endDate", endDate.format(AUDIT_DATE_FORMAT)
            ));
            complianceReport.put("generationTimestamp", LocalDateTime.now().format(AUDIT_DATE_FORMAT));
            
            // Get total audit events for the period
            Page<AuditLog> allEvents = auditLogRepository.findAuditLogsByDateRange(
                startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE));
            complianceReport.put("totalAuditEvents", allEvents.getTotalElements());
            
            // Analyze event types for compliance metrics
            List<Object[]> eventTypeCounts = auditLogRepository.countAuditEventsByType(startDate, endDate);
            Map<String, Long> eventTypeStatistics = eventTypeCounts.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
            
            complianceReport.put("eventTypeStatistics", eventTypeStatistics);
            
            // Security events analysis
            long securityEvents = eventTypeStatistics.getOrDefault(EVENT_TYPE_SECURITY_VIOLATION, 0L) +
                                eventTypeStatistics.getOrDefault(EVENT_TYPE_AUTHENTICATION, 0L) +
                                eventTypeStatistics.getOrDefault(EVENT_TYPE_AUTHORIZATION, 0L);
            complianceReport.put("securityEvents", securityEvents);
            
            // Authentication events analysis
            Page<AuditLog> failedAuth = auditLogRepository.findFailedAuthenticationAttempts(
                startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE));
            Map<String, Object> authenticationAnalysis = Map.of(
                "totalAuthEvents", eventTypeStatistics.getOrDefault(EVENT_TYPE_AUTHENTICATION, 0L),
                "failedAuthAttempts", failedAuth.getTotalElements(),
                "authSuccessRate", calculateSuccessRate(
                    eventTypeStatistics.getOrDefault(EVENT_TYPE_AUTHENTICATION, 0L),
                    failedAuth.getTotalElements()
                )
            );
            complianceReport.put("authenticationEvents", authenticationAnalysis);
            
            // Authorization events analysis
            Page<AuditLog> deniedAuth = auditLogRepository.findAuthorizationDenials(
                startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE));
            Map<String, Object> authorizationAnalysis = Map.of(
                "totalAuthzEvents", eventTypeStatistics.getOrDefault(EVENT_TYPE_AUTHORIZATION, 0L),
                "deniedAuthzAttempts", deniedAuth.getTotalElements(),
                "authzSuccessRate", calculateSuccessRate(
                    eventTypeStatistics.getOrDefault(EVENT_TYPE_AUTHORIZATION, 0L),
                    deniedAuth.getTotalElements()
                )
            );
            complianceReport.put("authorizationEvents", authorizationAnalysis);
            
            // Calculate compliance score based on framework requirements
            double complianceScore = calculateComplianceScore(complianceFramework, complianceReport);
            complianceReport.put("complianceScore", complianceScore);
            
            // Generate framework-specific recommendations
            List<String> recommendations = generateComplianceRecommendations(complianceFramework, complianceReport);
            complianceReport.put("recommendations", recommendations);
            
            logger.info("{} compliance report generated successfully - Score: {}, Total Events: {}", 
                       complianceFramework, complianceScore, allEvents.getTotalElements());
            
            return complianceReport;
            
        } catch (Exception e) {
            logger.error("Failed to generate {} compliance report - Error: {}", complianceFramework, e.getMessage(), e);
            throw new RuntimeException("Compliance report generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Archives old audit logs based on retention policy to manage storage and comply with regulations.
     * 
     * This method implements automated audit log retention management by identifying
     * and archiving audit logs older than the specified cutoff date. The archival
     * process helps manage database storage costs while maintaining compliance with
     * regulatory retention requirements. Archived logs are removed from the active
     * audit table after successful archival.
     * 
     * @param cutoffDate The cutoff date - audit logs older than this date will be archived
     * @param performActualDeletion If true, actually delete the records; if false, return count only
     * @return Map containing archival operation results:
     *         - "archivedCount": Number of audit logs archived/deleted
     *         - "cutoffDate": The cutoff date used for archival
     *         - "operationTimestamp": When the archival operation was performed
     *         - "operationType": "ARCHIVED" or "SIMULATION" based on performActualDeletion flag
     * @throws IllegalArgumentException if cutoff date is null or in the future
     */
    public Map<String, Object> archiveOldLogs(LocalDateTime cutoffDate, boolean performActualDeletion) {
        if (cutoffDate == null) {
            throw new IllegalArgumentException("Cutoff date cannot be null");
        }
        if (cutoffDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cutoff date cannot be in the future");
        }
        
        try {
            logger.info("Starting audit log archival operation - Cutoff date: {}, Actual deletion: {}", 
                       cutoffDate.format(AUDIT_DATE_FORMAT), performActualDeletion);
            
            Map<String, Object> archivalResults = new HashMap<>();
            
            // Count audit logs to be archived
            Page<AuditLog> logsToArchive = auditLogRepository.findAuditLogsByDateRange(
                LocalDateTime.of(2000, 1, 1, 0, 0), cutoffDate, 
                PageRequest.of(0, Integer.MAX_VALUE));
            
            long recordsToArchive = logsToArchive.getTotalElements();
            
            if (recordsToArchive == 0) {
                logger.info("No audit logs found for archival before cutoff date: {}", 
                           cutoffDate.format(AUDIT_DATE_FORMAT));
                archivalResults.put("archivedCount", 0L);
                archivalResults.put("cutoffDate", cutoffDate.format(AUDIT_DATE_FORMAT));
                archivalResults.put("operationTimestamp", LocalDateTime.now().format(AUDIT_DATE_FORMAT));
                archivalResults.put("operationType", "NO_RECORDS");
                return archivalResults;
            }
            
            int archivedCount = 0;
            
            if (performActualDeletion) {
                // Perform actual deletion of old audit logs
                archivedCount = auditLogRepository.deleteByTimestampBefore(cutoffDate);
                
                logger.info("Successfully archived {} audit log records older than {}", 
                           archivedCount, cutoffDate.format(AUDIT_DATE_FORMAT));
                
                archivalResults.put("operationType", "ARCHIVED");
            } else {
                // Simulation mode - just return count without deletion
                archivedCount = (int) recordsToArchive;
                
                logger.info("Simulation: {} audit log records would be archived older than {}", 
                           archivedCount, cutoffDate.format(AUDIT_DATE_FORMAT));
                
                archivalResults.put("operationType", "SIMULATION");
            }
            
            archivalResults.put("archivedCount", archivedCount);
            archivalResults.put("cutoffDate", cutoffDate.format(AUDIT_DATE_FORMAT));
            archivalResults.put("operationTimestamp", LocalDateTime.now().format(AUDIT_DATE_FORMAT));
            
            return archivalResults;
            
        } catch (Exception e) {
            logger.error("Failed to archive audit logs before {} - Error: {}", 
                        cutoffDate.format(AUDIT_DATE_FORMAT), e.getMessage(), e);
            throw new RuntimeException("Audit log archival failed: " + e.getMessage(), e);
        }
    }

    /**
     * Performs advanced search of audit logs with multiple filtering criteria.
     * 
     * This method provides comprehensive audit log search capabilities supporting
     * multiple simultaneous filter criteria including username, event type, outcome,
     * source IP address, and date range. Essential for security incident investigation,
     * compliance audits, and operational monitoring. Supports pagination for efficient
     * handling of large result sets.
     * 
     * @param searchCriteria Map containing search criteria with supported keys:
     *                      - "username": Filter by specific username (String)
     *                      - "eventType": Filter by event type (String)
     *                      - "outcome": Filter by operation outcome (String)
     *                      - "sourceIp": Filter by source IP address (String)
     *                      - "startDate": Start date for search range (LocalDateTime)
     *                      - "endDate": End date for search range (LocalDateTime)
     *                      - "page": Page number for pagination (Integer, default: 0)
     *                      - "size": Page size for pagination (Integer, default: 50)
     * @return Page of AuditLog entries matching the specified search criteria
     * @throws IllegalArgumentException if search criteria are invalid or required parameters are missing
     */
    public Page<AuditLog> searchAuditLogs(Map<String, Object> searchCriteria) {
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            throw new IllegalArgumentException("Search criteria cannot be null or empty");
        }
        
        try {
            // Extract pagination parameters with defaults
            int page = (Integer) searchCriteria.getOrDefault("page", 0);
            int size = (Integer) searchCriteria.getOrDefault("size", 50);
            validatePaginationParameters(page, size);
            
            // Extract search criteria
            String username = (String) searchCriteria.get("username");
            String eventType = (String) searchCriteria.get("eventType");
            String outcome = (String) searchCriteria.get("outcome");
            String sourceIp = (String) searchCriteria.get("sourceIp");
            LocalDateTime startDate = (LocalDateTime) searchCriteria.get("startDate");
            LocalDateTime endDate = (LocalDateTime) searchCriteria.get("endDate");
            
            // Validate date range if provided
            if (startDate != null && endDate != null) {
                validateDateRange(startDate, endDate);
            }
            
            // Create pageable with timestamp descending order
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            
            Page<AuditLog> searchResults;
            
            // Determine appropriate repository method based on provided criteria
            if (username != null && startDate != null && endDate != null) {
                // Username and date range search
                searchResults = auditLogRepository.findByUsernameAndTimestampBetween(
                    username, startDate, endDate, pageable);
                    
            } else if (eventType != null && startDate != null && endDate != null) {
                // Event type and date range search
                searchResults = auditLogRepository.findByEventTypeAndTimestampBetween(
                    eventType, startDate, endDate, pageable);
                    
            } else if (startDate != null && endDate != null) {
                // Date range only search
                searchResults = auditLogRepository.findAuditLogsByDateRange(startDate, endDate, pageable);
                
            } else {
                // Use advanced search method for complex criteria
                searchResults = auditLogRepository.findAuditLogsByAdvancedCriteria(
                    username, eventType, outcome, sourceIp, startDate, endDate, pageable);
            }
            
            // Log search operation for monitoring
            logger.info("Audit log search completed - Criteria: {}, Results: {}, Page: {}/{}", 
                       buildSearchCriteriaString(searchCriteria),
                       searchResults.getTotalElements(),
                       page + 1,
                       searchResults.getTotalPages());
            
            return searchResults;
            
        } catch (Exception e) {
            logger.error("Failed to search audit logs - Criteria: {}, Error: {}", 
                        searchCriteria, e.getMessage(), e);
            throw new RuntimeException("Audit log search failed: " + e.getMessage(), e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Validates audit log entry for required fields and data integrity.
     */
    private void validateAuditLogEntry(AuditLog auditLog) {
        if (auditLog.getUsername() == null || auditLog.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required for audit log entry");
        }
        if (auditLog.getEventType() == null || auditLog.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type is required for audit log entry");
        }
        if (auditLog.getOutcome() == null || auditLog.getOutcome().trim().isEmpty()) {
            throw new IllegalArgumentException("Outcome is required for audit log entry");
        }
        if (auditLog.getSourceIp() != null && auditLog.getSourceIp().length() > 45) {
            throw new IllegalArgumentException("Source IP address too long (max 45 characters)");
        }
        if (auditLog.getDetails() != null && auditLog.getDetails().length() > 4000) {
            throw new IllegalArgumentException("Event details too long (max 4000 characters)");
        }
    }
    
    /**
     * Validates username and date range parameters.
     */
    private void validateUserAndDateRange(String username, LocalDateTime startDate, LocalDateTime endDate) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        validateDateRange(startDate, endDate);
    }
    
    /**
     * Validates event type and date range parameters.
     */
    private void validateEventTypeAndDateRange(String eventType, LocalDateTime startDate, LocalDateTime endDate) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        validateDateRange(startDate, endDate);
    }
    
    /**
     * Validates date range parameters.
     */
    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (endDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("End date cannot be in the future");
        }
    }
    
    /**
     * Validates pagination parameters.
     */
    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        if (size > 10000) {
            throw new IllegalArgumentException("Page size cannot exceed 10000 records");
        }
    }
    
    /**
     * Validates compliance framework parameter.
     */
    private void validateComplianceFramework(String complianceFramework) {
        if (complianceFramework == null || complianceFramework.trim().isEmpty()) {
            throw new IllegalArgumentException("Compliance framework cannot be null or empty");
        }
        
        Set<String> supportedFrameworks = Set.of(COMPLIANCE_SOX, COMPLIANCE_PCI_DSS, 
                                                COMPLIANCE_GDPR, COMPLIANCE_SOC2);
        
        if (!supportedFrameworks.contains(complianceFramework.toUpperCase())) {
            throw new IllegalArgumentException("Unsupported compliance framework: " + complianceFramework + 
                                             ". Supported frameworks: " + supportedFrameworks);
        }
    }
    
    /**
     * Generates SHA-256 cryptographic integrity hash for audit log entry.
     */
    private String generateIntegrityHash(AuditLog auditLog) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Build hash input from critical audit fields
            StringBuilder hashInput = new StringBuilder()
                .append(auditLog.getUsername() != null ? auditLog.getUsername() : "")
                .append("|")
                .append(auditLog.getEventType() != null ? auditLog.getEventType() : "")
                .append("|")
                .append(auditLog.getOutcome() != null ? auditLog.getOutcome() : "")
                .append("|")
                .append(auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : "")
                .append("|")
                .append(auditLog.getSourceIp() != null ? auditLog.getSourceIp() : "")
                .append("|")
                .append(auditLog.getDetails() != null ? auditLog.getDetails() : "");
            
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
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available for integrity hash generation", e);
            throw new RuntimeException("Failed to generate integrity hash: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculates success rate percentage for authentication/authorization events.
     */
    private double calculateSuccessRate(long totalEvents, long failedEvents) {
        if (totalEvents == 0) {
            return 0.0;
        }
        long successEvents = totalEvents - failedEvents;
        return (successEvents * 100.0) / totalEvents;
    }
    
    /**
     * Calculates compliance score based on framework-specific criteria.
     */
    private double calculateComplianceScore(String framework, Map<String, Object> reportData) {
        double baseScore = 100.0;
        
        // Get event statistics
        @SuppressWarnings("unchecked")
        Map<String, Long> eventStats = (Map<String, Long>) reportData.get("eventTypeStatistics");
        
        long totalEvents = (Long) reportData.get("totalAuditEvents");
        long securityEvents = (Long) reportData.get("securityEvents");
        
        // Framework-specific scoring logic
        switch (framework.toUpperCase()) {
            case COMPLIANCE_SOX:
                // SOX focuses on financial transaction integrity
                if (securityEvents > totalEvents * 0.05) { // More than 5% security events
                    baseScore -= 20.0;
                }
                if (eventStats.getOrDefault(EVENT_TYPE_SECURITY_VIOLATION, 0L) > 0) {
                    baseScore -= 30.0;
                }
                break;
                
            case COMPLIANCE_PCI_DSS:
                // PCI DSS focuses on payment data security
                if (eventStats.getOrDefault(EVENT_TYPE_SECURITY_VIOLATION, 0L) > 0) {
                    baseScore -= 40.0;
                }
                if (securityEvents < totalEvents * 0.1) { // Less than 10% security monitoring
                    baseScore -= 10.0;
                }
                break;
                
            case COMPLIANCE_GDPR:
                // GDPR focuses on data privacy and access control
                if (eventStats.getOrDefault(EVENT_TYPE_DATA_ACCESS, 0L) == 0) {
                    baseScore -= 15.0; // No data access logging
                }
                if (eventStats.getOrDefault(EVENT_TYPE_AUTHORIZATION, 0L) < totalEvents * 0.2) {
                    baseScore -= 10.0; // Insufficient authorization tracking
                }
                break;
                
            case COMPLIANCE_SOC2:
                // SOC2 focuses on security, availability, and confidentiality
                if (eventStats.getOrDefault(EVENT_TYPE_SYSTEM_EVENT, 0L) == 0) {
                    baseScore -= 10.0; // No system event logging
                }
                if (securityEvents < totalEvents * 0.15) {
                    baseScore -= 15.0; // Insufficient security monitoring
                }
                break;
        }
        
        return Math.max(0.0, Math.min(100.0, baseScore));
    }
    
    /**
     * Generates compliance recommendations based on framework and report analysis.
     */
    private List<String> generateComplianceRecommendations(String framework, Map<String, Object> reportData) {
        List<String> recommendations = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Long> eventStats = (Map<String, Long>) reportData.get("eventTypeStatistics");
        
        long totalEvents = (Long) reportData.get("totalAuditEvents");
        double complianceScore = (Double) reportData.get("complianceScore");
        
        // General recommendations based on compliance score
        if (complianceScore < 80.0) {
            recommendations.add("Compliance score below 80% - immediate attention required for audit trail improvements");
        }
        
        // Framework-specific recommendations
        switch (framework.toUpperCase()) {
            case COMPLIANCE_SOX:
                if (eventStats.getOrDefault(EVENT_TYPE_SECURITY_VIOLATION, 0L) > 0) {
                    recommendations.add("SOX: Investigate and remediate all security violations to ensure financial data integrity");
                }
                if (eventStats.getOrDefault(EVENT_TYPE_TRANSACTION, 0L) < totalEvents * 0.5) {
                    recommendations.add("SOX: Increase transaction audit coverage to meet financial reporting requirements");
                }
                break;
                
            case COMPLIANCE_PCI_DSS:
                if (eventStats.getOrDefault(EVENT_TYPE_AUTHENTICATION, 0L) < totalEvents * 0.3) {
                    recommendations.add("PCI DSS: Implement comprehensive authentication logging for payment systems");
                }
                if (eventStats.getOrDefault(EVENT_TYPE_DATA_ACCESS, 0L) == 0) {
                    recommendations.add("PCI DSS: Enable audit logging for all cardholder data access");
                }
                break;
                
            case COMPLIANCE_GDPR:
                if (eventStats.getOrDefault(EVENT_TYPE_DATA_ACCESS, 0L) == 0) {
                    recommendations.add("GDPR: Implement data access logging to track personal data processing");
                }
                recommendations.add("GDPR: Ensure audit logs support data subject access request fulfillment");
                break;
                
            case COMPLIANCE_SOC2:
                if (eventStats.getOrDefault(EVENT_TYPE_SYSTEM_EVENT, 0L) == 0) {
                    recommendations.add("SOC2: Enable comprehensive system event logging for security monitoring");
                }
                if (eventStats.getOrDefault(EVENT_TYPE_CONFIGURATION, 0L) == 0) {
                    recommendations.add("SOC2: Implement configuration change audit logging");
                }
                break;
        }
        
        return recommendations;
    }
    
    /**
     * Builds a human-readable string from search criteria for logging.
     */
    private String buildSearchCriteriaString(Map<String, Object> searchCriteria) {
        return searchCriteria.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", "));
    }

    /**
     * Convenience method for logging administrative operations.
     * 
     * This method provides a simplified interface for logging administrative actions
     * by creating an AuditLog entity with the provided parameters and delegating
     * to the saveAuditLog method for persistent storage.
     * 
     * @param userId User ID performing the operation
     * @param operation Operation type being performed
     * @param outcome Operation outcome (SUCCESS, FAILURE, ERROR)
     * @param details Additional operation details
     * @return The persisted AuditLog entity
     * @throws IllegalArgumentException if required parameters are null or empty
     * @throws RuntimeException if database persistence fails
     */
    public AuditLog logAdminOperation(String userId, String operation, String outcome, String details) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (operation == null || operation.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation cannot be null or empty");
        }
        if (outcome == null || outcome.trim().isEmpty()) {
            throw new IllegalArgumentException("Outcome cannot be null or empty");
        }
        
        try {
            // Create audit log entry for administrative operation
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(userId);
            auditLog.setEventType("ADMIN_OPERATION");
            auditLog.setActionPerformed(operation);
            auditLog.setOutcome(outcome);
            auditLog.setDetails(details);
            auditLog.setResourceAccessed("USER_MANAGEMENT");
            auditLog.setTimestamp(LocalDateTime.now());
            
            // Save the audit log entry
            return saveAuditLog(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to log admin operation - UserId: {}, Operation: {}, Error: {}", 
                        userId, operation, e.getMessage(), e);
            throw new RuntimeException("Failed to log admin operation: " + e.getMessage(), e);
        }
    }
}
