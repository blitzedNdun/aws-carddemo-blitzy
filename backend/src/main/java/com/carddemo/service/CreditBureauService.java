/*
 * CreditBureauService.java
 * 
 * Credit Bureau Integration Service for CardDemo Application
 * 
 * This service implements credit bureau functionality including FICO score
 * updates, validation, and credit report retrieval. Provides mock implementation
 * for external credit bureau API calls while preserving COBOL credit processing
 * patterns from CBCUS01C.cbl.
 * 
 * Replaces COBOL paragraph-based processing with Spring Boot service methods
 * while maintaining identical business logic flow and error handling patterns.
 */

package com.carddemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * CreditBureauService
 * 
 * Service for credit bureau integration to update customer FICO scores and 
 * credit information. Implements credit score retrieval, validation, and 
 * update logic that preserves COBOL credit processing patterns.
 * 
 * Provides mock implementation for external credit bureau API calls while
 * maintaining identical validation rules and processing sequences from the
 * original COBOL credit processing logic.
 */
@Slf4j
@Service
public class CreditBureauService {

    // Constants for FICO score validation (matching COBOL business rules)
    private static final int MIN_FICO_SCORE = 300;
    private static final int MAX_FICO_SCORE = 850;
    private static final String CREDIT_BUREAU_SYSTEM = "EQUIFAX_MOCK";
    
    // Mock data storage for demonstration (in real implementation, this would be external API calls)
    private final Map<Long, CreditScoreRecord> creditScoreCache = new HashMap<>();
    private final Random random = new Random();

    /**
     * Update customer credit score from credit bureau
     * 
     * Implements credit score update processing following COBOL paragraph structure:
     * - 0000-INIT: Initialize processing 
     * - 1000-PROCESS: Execute credit score update
     * - 9000-CLOSE: Complete processing and logging
     * 
     * @param customerId Customer ID for credit score update
     * @return Updated credit score
     * @throws CreditBureauException if update fails validation
     */
    public int updateCreditScore(Long customerId) {
        log.info("START OF EXECUTION OF CREDIT SCORE UPDATE FOR CUSTOMER: {}", customerId);
        
        try {
            // 0000-INIT: Initialize credit score update processing
            if (customerId == null || customerId <= 0) {
                log.error("ERROR: INVALID CUSTOMER ID PROVIDED: {}", customerId);
                throw new CreditBureauException("Invalid customer ID for credit score update");
            }
            
            // 1000-PROCESS: Execute credit score retrieval and update
            int newCreditScore = performCreditScoreUpdate(customerId);
            
            // Validate the retrieved score
            validateCreditScore(newCreditScore);
            
            // Track the credit score change for audit purposes
            trackCreditScoreChange(customerId, newCreditScore, "BUREAU_UPDATE");
            
            // 9000-CLOSE: Complete processing
            log.info("CREDIT SCORE UPDATE COMPLETED SUCCESSFULLY FOR CUSTOMER: {} NEW SCORE: {}", 
                    customerId, newCreditScore);
            
            return newCreditScore;
            
        } catch (Exception e) {
            log.error("ERROR UPDATING CREDIT SCORE FOR CUSTOMER: {} ERROR: {}", customerId, e.getMessage());
            throw new CreditBureauException("Credit score update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate credit score range (300-850)
     * 
     * Implements COBOL validation rules for FICO score range checking.
     * Preserves exact validation logic and error handling patterns.
     * 
     * @param creditScore Credit score to validate
     * @return true if valid, false otherwise
     */
    public boolean validateCreditScore(int creditScore) {
        log.debug("VALIDATING CREDIT SCORE: {}", creditScore);
        
        if (creditScore < MIN_FICO_SCORE || creditScore > MAX_FICO_SCORE) {
            log.error("INVALID CREDIT SCORE: {} VALID RANGE: {}-{}", 
                    creditScore, MIN_FICO_SCORE, MAX_FICO_SCORE);
            return false;
        }
        
        log.debug("CREDIT SCORE VALIDATION SUCCESSFUL: {}", creditScore);
        return true;
    }

    /**
     * Retrieve credit report from credit bureau
     * 
     * Implements credit report retrieval logic following COBOL file processing
     * patterns with proper error handling and data structure management.
     * 
     * @param customerId Customer ID for credit report retrieval
     * @return Credit report data map
     */
    public Map<String, Object> retrieveCreditReport(Long customerId) {
        log.info("START OF CREDIT REPORT RETRIEVAL FOR CUSTOMER: {}", customerId);
        
        try {
            // Initialize credit report processing (COBOL 0000- paragraph equivalent)
            if (customerId == null || customerId <= 0) {
                log.error("ERROR: INVALID CUSTOMER ID FOR CREDIT REPORT: {}", customerId);
                throw new CreditBureauException("Invalid customer ID for credit report retrieval");
            }
            
            // Process credit report retrieval (COBOL 1000- paragraph equivalent)
            Map<String, Object> creditReport = generateMockCreditReport(customerId);
            
            // Log successful retrieval (COBOL display equivalent)
            log.info("CREDIT REPORT RETRIEVED SUCCESSFULLY FOR CUSTOMER: {}", customerId);
            
            return creditReport;
            
        } catch (Exception e) {
            log.error("ERROR RETRIEVING CREDIT REPORT FOR CUSTOMER: {} ERROR: {}", customerId, e.getMessage());
            throw new CreditBureauException("Credit report retrieval failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get current credit score for customer
     * 
     * Implements COBOL record retrieval pattern with sequential file processing
     * logic and error handling matching original mainframe patterns.
     * 
     * @param customerId Customer ID for credit score lookup
     * @return Current credit score
     */
    public int getCreditScore(Long customerId) {
        log.info("RETRIEVING CURRENT CREDIT SCORE FOR CUSTOMER: {}", customerId);
        
        try {
            // Validate customer ID (COBOL validation pattern)
            if (customerId == null || customerId <= 0) {
                log.error("ERROR: INVALID CUSTOMER ID: {}", customerId);
                throw new CreditBureauException("Invalid customer ID for credit score retrieval");
            }
            
            // Retrieve credit score record (COBOL file read equivalent)
            CreditScoreRecord record = creditScoreCache.get(customerId);
            
            if (record == null) {
                // Generate initial score if not found (mock external bureau call)
                int initialScore = generateInitialCreditScore(customerId);
                record = new CreditScoreRecord(customerId, initialScore, LocalDateTime.now());
                creditScoreCache.put(customerId, record);
                
                log.info("INITIAL CREDIT SCORE GENERATED FOR CUSTOMER: {} SCORE: {}", 
                        customerId, initialScore);
            }
            
            log.info("CREDIT SCORE RETRIEVED FOR CUSTOMER: {} SCORE: {}", 
                    customerId, record.getCreditScore());
            
            return record.getCreditScore();
            
        } catch (Exception e) {
            log.error("ERROR GETTING CREDIT SCORE FOR CUSTOMER: {} ERROR: {}", customerId, e.getMessage());
            throw new CreditBureauException("Credit score retrieval failed: " + e.getMessage(), e);
        }
    }

    /**
     * Track credit score change for audit logging
     * 
     * Implements COBOL audit trail functionality with comprehensive logging
     * and change tracking matching mainframe audit requirements.
     * 
     * @param customerId Customer ID for change tracking
     * @param newScore New credit score value
     * @param changeReason Reason for score change
     */
    public void trackCreditScoreChange(Long customerId, int newScore, String changeReason) {
        log.info("TRACKING CREDIT SCORE CHANGE FOR CUSTOMER: {}", customerId);
        
        try {
            // Validate input parameters (COBOL validation pattern)
            if (customerId == null || customerId <= 0) {
                log.error("ERROR: INVALID CUSTOMER ID FOR CHANGE TRACKING: {}", customerId);
                throw new CreditBureauException("Invalid customer ID for change tracking");
            }
            
            if (!validateCreditScore(newScore)) {
                log.error("ERROR: INVALID CREDIT SCORE FOR CHANGE TRACKING: {}", newScore);
                throw new CreditBureauException("Invalid credit score for change tracking");
            }
            
            // Get previous score for comparison
            CreditScoreRecord previousRecord = creditScoreCache.get(customerId);
            int previousScore = (previousRecord != null) ? previousRecord.getCreditScore() : 0;
            
            // Create audit record (COBOL record creation pattern)
            CreditScoreChangeAudit auditRecord = CreditScoreChangeAudit.builder()
                    .customerId(customerId)
                    .previousScore(previousScore)
                    .newScore(newScore)
                    .changeReason(changeReason)
                    .changeDate(LocalDateTime.now())
                    .changeSystem(CREDIT_BUREAU_SYSTEM)
                    .build();
            
            // Update credit score cache with new value
            CreditScoreRecord newRecord = new CreditScoreRecord(customerId, newScore, LocalDateTime.now());
            creditScoreCache.put(customerId, newRecord);
            
            // Log audit information (COBOL display statements equivalent)
            log.info("CREDIT SCORE CHANGE TRACKED - CUSTOMER: {} PREVIOUS: {} NEW: {} REASON: {}", 
                    customerId, previousScore, newScore, changeReason);
            
            // In production, this would write to audit database table
            log.debug("AUDIT RECORD CREATED: {}", auditRecord);
            
        } catch (Exception e) {
            log.error("ERROR TRACKING CREDIT SCORE CHANGE FOR CUSTOMER: {} ERROR: {}", 
                    customerId, e.getMessage());
            throw new CreditBureauException("Credit score change tracking failed: " + e.getMessage(), e);
        }
    }

    // Private helper methods (equivalent to COBOL internal procedures)
    
    /**
     * Perform actual credit score update processing
     * Simulates external credit bureau API call
     */
    private int performCreditScoreUpdate(Long customerId) {
        log.debug("PERFORMING CREDIT SCORE UPDATE PROCESSING FOR CUSTOMER: {}", customerId);
        
        // Mock credit bureau API call - in production this would be actual external service
        // Generate score between 300-850 based on customer ID (for consistency)
        int baseScore = (int) (400 + (customerId % 400));
        int variation = random.nextInt(51) - 25; // +/- 25 points variation
        int newScore = Math.max(MIN_FICO_SCORE, Math.min(MAX_FICO_SCORE, baseScore + variation));
        
        log.debug("CREDIT BUREAU API SIMULATION - CUSTOMER: {} NEW SCORE: {}", customerId, newScore);
        
        return newScore;
    }
    
    /**
     * Generate mock credit report data
     * Simulates external credit bureau report retrieval
     */
    private Map<String, Object> generateMockCreditReport(Long customerId) {
        log.debug("GENERATING MOCK CREDIT REPORT FOR CUSTOMER: {}", customerId);
        
        Map<String, Object> creditReport = new HashMap<>();
        
        // Basic credit information
        creditReport.put("customerId", customerId);
        creditReport.put("reportDate", LocalDate.now());
        creditReport.put("bureau", CREDIT_BUREAU_SYSTEM);
        creditReport.put("creditScore", getCreditScore(customerId));
        
        // Credit account summary (mock data)
        creditReport.put("totalAccounts", random.nextInt(10) + 1);
        creditReport.put("openAccounts", random.nextInt(5) + 1);
        creditReport.put("totalCreditLimit", new BigDecimal(random.nextInt(50000) + 10000));
        creditReport.put("totalBalance", new BigDecimal(random.nextInt(20000)));
        
        // Payment history (mock percentages)
        creditReport.put("onTimePaymentRate", 95 + random.nextInt(5)); // 95-99%
        creditReport.put("delinquencyRate", random.nextInt(5)); // 0-4%
        
        // Credit utilization
        BigDecimal creditLimit = (BigDecimal) creditReport.get("totalCreditLimit");
        BigDecimal balance = (BigDecimal) creditReport.get("totalBalance");
        double utilizationRate = balance.divide(creditLimit, 4, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
        creditReport.put("creditUtilizationRate", utilizationRate);
        
        log.debug("MOCK CREDIT REPORT GENERATED FOR CUSTOMER: {}", customerId);
        
        return creditReport;
    }
    
    /**
     * Generate initial credit score for new customer
     * Uses deterministic algorithm for consistency
     */
    private int generateInitialCreditScore(Long customerId) {
        // Generate consistent initial score based on customer ID
        int baseScore = 500 + (int) (customerId % 300); // Range 500-799
        return Math.max(MIN_FICO_SCORE, Math.min(MAX_FICO_SCORE, baseScore));
    }

    // Inner classes for data structures
    
    /**
     * Credit Score Record structure
     * Represents customer credit score data
     */
    private static class CreditScoreRecord {
        private final Long customerId;
        private final int creditScore;
        private final LocalDateTime lastUpdated;
        
        public CreditScoreRecord(Long customerId, int creditScore, LocalDateTime lastUpdated) {
            this.customerId = customerId;
            this.creditScore = creditScore;
            this.lastUpdated = lastUpdated;
        }
        
        public Long getCustomerId() { return customerId; }
        public int getCreditScore() { return creditScore; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }
    
    /**
     * Credit Score Change Audit record
     * Tracks credit score changes for audit purposes
     */
    private static class CreditScoreChangeAudit {
        private final Long customerId;
        private final int previousScore;
        private final int newScore;
        private final String changeReason;
        private final LocalDateTime changeDate;
        private final String changeSystem;
        
        private CreditScoreChangeAudit(Builder builder) {
            this.customerId = builder.customerId;
            this.previousScore = builder.previousScore;
            this.newScore = builder.newScore;
            this.changeReason = builder.changeReason;
            this.changeDate = builder.changeDate;
            this.changeSystem = builder.changeSystem;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        @Override
        public String toString() {
            return String.format("CreditScoreChangeAudit{customerId=%d, previousScore=%d, newScore=%d, " +
                    "changeReason='%s', changeDate=%s, changeSystem='%s'}", 
                    customerId, previousScore, newScore, changeReason, changeDate, changeSystem);
        }
        
        public static class Builder {
            private Long customerId;
            private int previousScore;
            private int newScore;
            private String changeReason;
            private LocalDateTime changeDate;
            private String changeSystem;
            
            public Builder customerId(Long customerId) {
                this.customerId = customerId;
                return this;
            }
            
            public Builder previousScore(int previousScore) {
                this.previousScore = previousScore;
                return this;
            }
            
            public Builder newScore(int newScore) {
                this.newScore = newScore;
                return this;
            }
            
            public Builder changeReason(String changeReason) {
                this.changeReason = changeReason;
                return this;
            }
            
            public Builder changeDate(LocalDateTime changeDate) {
                this.changeDate = changeDate;
                return this;
            }
            
            public Builder changeSystem(String changeSystem) {
                this.changeSystem = changeSystem;
                return this;
            }
            
            public CreditScoreChangeAudit build() {
                return new CreditScoreChangeAudit(this);
            }
        }
    }
    
    /**
     * Credit Bureau Exception
     * Custom exception for credit bureau processing errors
     */
    public static class CreditBureauException extends RuntimeException {
        public CreditBureauException(String message) {
            super(message);
        }
        
        public CreditBureauException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}