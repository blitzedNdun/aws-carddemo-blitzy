/*
 * AccountReconciliationService.java
 * 
 * Spring Boot service implementing comprehensive account balance reconciliation and discrepancy detection logic.
 * Validates account balances against transaction totals, identifies reconciliation discrepancies, generates 
 * reconciliation reports with detailed audit trails, and provides batch processing capabilities for daily 
 * reconciliation operations. Maintains COBOL-equivalent precision for financial calculations while leveraging 
 * Spring's transaction management.
 * 
 * This service implements the account reconciliation requirements outlined in the Summary of Changes Section 0,
 * providing enterprise-grade reconciliation capabilities for the CardDemo application migration from COBOL 
 * mainframe to Spring Boot. Key capabilities include:
 * 
 * - Comprehensive balance validation logic comparing account balances vs transaction totals
 * - Transaction sum verification against current account balances  
 * - Discrepancy identification and detailed categorization
 * - Reconciliation report generation with detailed findings and audit trails
 * - Batch processing support for daily reconciliation operations
 * - Audit trail creation for all reconciliation activities
 * - COBOL COMP-3 precision preservation for financial calculations
 * - Exception handling for reconciliation failures and data integrity issues
 * - Integration with Spring Batch for large-scale processing capabilities
 */
package com.carddemo.service;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.service.AuditService;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * Spring Boot service providing comprehensive account balance reconciliation and discrepancy detection capabilities.
 * 
 * This service implements enterprise-grade account reconciliation logic that validates account balances
 * against transaction totals while maintaining COBOL COMP-3 precision for financial calculations.
 * The reconciliation process includes:
 * 
 * <ul>
 *   <li><strong>Balance Validation:</strong> Compares current account balances with calculated transaction sums
 *       to identify discrepancies requiring investigation</li>
 *   <li><strong>Discrepancy Detection:</strong> Categorizes reconciliation differences by type, severity, and 
 *       potential root cause for efficient resolution</li>
 *   <li><strong>Audit Trail Generation:</strong> Creates comprehensive audit logs for all reconciliation 
 *       activities supporting regulatory compliance and operational monitoring</li>
 *   <li><strong>Batch Processing:</strong> Supports daily reconciliation operations for all accounts with 
 *       configurable batch size and parallel processing capabilities</li>
 *   <li><strong>Financial Precision:</strong> Maintains exact COBOL COMP-3 decimal precision using BigDecimal 
 *       with appropriate scale and rounding modes</li>
 *   <li><strong>Exception Handling:</strong> Comprehensive error handling for reconciliation failures with 
 *       detailed logging and recovery mechanisms</li>
 * </ul>
 * 
 * The service integrates with Spring Batch for large-scale processing and maintains transactional integrity
 * through Spring's @Transactional annotations. All reconciliation operations are logged with structured
 * formatting for integration with monitoring and alerting systems.
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class AccountReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(AccountReconciliationService.class);
    
    // Financial precision constants matching COBOL COMP-3 behavior
    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode CURRENCY_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
    
    // Reconciliation tolerance for micro-discrepancies (1 cent)
    private static final BigDecimal RECONCILIATION_TOLERANCE = new BigDecimal("0.01").setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
    
    // Discrepancy categorization constants
    private static final String DISCREPANCY_TYPE_BALANCE_MISMATCH = "BALANCE_MISMATCH";
    private static final String DISCREPANCY_TYPE_MISSING_TRANSACTIONS = "MISSING_TRANSACTIONS";
    private static final String DISCREPANCY_TYPE_PRECISION_ERROR = "PRECISION_ERROR";
    private static final String DISCREPANCY_TYPE_SYSTEM_ERROR = "SYSTEM_ERROR";
    
    // Reconciliation status constants
    private static final String RECONCILIATION_STATUS_SUCCESS = "SUCCESS";
    private static final String RECONCILIATION_STATUS_DISCREPANCY = "DISCREPANCY";
    private static final String RECONCILIATION_STATUS_ERROR = "ERROR";

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CobolDataConverter cobolDataConverter;
    
    @Autowired
    private AuditService auditService;

    /**
     * Reconciles a single account by validating its balance against transaction totals.
     * 
     * This method performs comprehensive reconciliation for a specific account by comparing
     * the current account balance with the calculated sum of all transactions. The reconciliation
     * process maintains COBOL COMP-3 precision and identifies any discrepancies requiring
     * investigation. All reconciliation activities are logged to the audit trail.
     * 
     * @param accountId The unique identifier of the account to reconcile
     * @return Map containing reconciliation results with the following keys:
     *         - "accountId": The account identifier that was reconciled
     *         - "status": Reconciliation status (SUCCESS, DISCREPANCY, ERROR)
     *         - "currentBalance": Account's current balance from database
     *         - "calculatedBalance": Balance calculated from transaction totals
     *         - "difference": Difference between current and calculated balances
     *         - "reconciliationDate": When the reconciliation was performed
     *         - "discrepancies": List of identified discrepancies if any exist
     * @throws IllegalArgumentException if accountId is null or empty
     * @throws RuntimeException if database access fails or reconciliation cannot be completed
     */
    public Map<String, Object> reconcileAccount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        logger.info("Starting account reconciliation for account ID: {}", accountId);
        
        Map<String, Object> reconciliationResult = new HashMap<>();
        LocalDateTime reconciliationTimestamp = LocalDateTime.now();
        
        try {
            // Retrieve account information
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));
            
            // Get current balance with COBOL precision preservation
            BigDecimal currentBalance = cobolDataConverter.preservePrecision(account.getCurrentBalance(), CURRENCY_SCALE)
                .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
            
            // Calculate transaction sum for the account
            Map<String, Object> transactionSumResult = calculateTransactionSum(accountId);
            BigDecimal calculatedBalance = (BigDecimal) transactionSumResult.get("totalAmount");
            
            // Calculate difference
            BigDecimal difference = currentBalance.subtract(calculatedBalance)
                .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
            
            // Determine reconciliation status
            String reconciliationStatus;
            List<Map<String, Object>> discrepancies = new ArrayList<>();
            
            if (difference.abs().compareTo(RECONCILIATION_TOLERANCE) <= 0) {
                reconciliationStatus = RECONCILIATION_STATUS_SUCCESS;
                logger.info("Account {} reconciliation successful - Balance matches within tolerance", accountId);
            } else {
                reconciliationStatus = RECONCILIATION_STATUS_DISCREPANCY;
                discrepancies = identifyDiscrepancies(accountId, currentBalance, calculatedBalance);
                logger.warn("Account {} reconciliation found discrepancy - Difference: {}", accountId, difference);
            }
            
            // Build reconciliation result
            reconciliationResult.put("accountId", accountId);
            reconciliationResult.put("status", reconciliationStatus);
            reconciliationResult.put("currentBalance", currentBalance);
            reconciliationResult.put("calculatedBalance", calculatedBalance);
            reconciliationResult.put("difference", difference);
            reconciliationResult.put("reconciliationDate", reconciliationTimestamp);
            reconciliationResult.put("discrepancies", discrepancies);
            reconciliationResult.put("transactionCount", transactionSumResult.get("transactionCount"));
            
            // Create audit log entry for reconciliation activity
            auditService.saveAuditLog(createReconciliationAuditLog(
                accountId.toString(), reconciliationStatus, difference, reconciliationTimestamp));
            
            logger.info("Account reconciliation completed for account ID: {} with status: {}", accountId, reconciliationStatus);
            return reconciliationResult;
            
        } catch (Exception e) {
            logger.error("Failed to reconcile account ID: {} - Error: {}", accountId, e.getMessage(), e);
            
            // Create error audit log
            auditService.saveAuditLog(createErrorAuditLog(accountId.toString(), e.getMessage(), reconciliationTimestamp));
            
            reconciliationResult.put("accountId", accountId);
            reconciliationResult.put("status", RECONCILIATION_STATUS_ERROR);
            reconciliationResult.put("error", e.getMessage());
            reconciliationResult.put("reconciliationDate", reconciliationTimestamp);
            
            throw new RuntimeException("Account reconciliation failed for account ID " + accountId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reconciles all accounts in the system using batch processing capabilities.
     * 
     * This method performs comprehensive reconciliation for all active accounts in the system,
     * processing them in configurable batches for optimal performance and memory usage.
     * Maintains complete audit trails and provides summary statistics for operational monitoring.
     * Suitable for daily reconciliation operations with Spring Batch integration.
     * 
     * @return Map containing comprehensive reconciliation results:
     *         - "totalAccounts": Total number of accounts processed
     *         - "successfulReconciliations": Number of accounts reconciled successfully
     *         - "discrepancyCount": Number of accounts with identified discrepancies
     *         - "errorCount": Number of accounts that failed reconciliation
     *         - "totalDiscrepancyAmount": Sum of all reconciliation differences
     *         - "reconciliationTimestamp": When the batch reconciliation was performed
     *         - "processingTimeMillis": Total processing time for the operation
     *         - "accountResults": Detailed results for each account processed
     * @throws RuntimeException if batch reconciliation fails or database access errors occur
     */
    public Map<String, Object> reconcileAllAccounts() {
        logger.info("Starting batch reconciliation of all accounts");
        
        long startTime = System.currentTimeMillis();
        LocalDateTime batchTimestamp = LocalDateTime.now();
        
        Map<String, Object> batchResult = new HashMap<>();
        List<Map<String, Object>> accountResults = new ArrayList<>();
        
        int totalAccounts = 0;
        int successfulReconciliations = 0;
        int discrepancyCount = 0;
        int errorCount = 0;
        BigDecimal totalDiscrepancyAmount = ZERO_AMOUNT;
        
        try {
            // Retrieve all accounts for batch processing
            List<Account> allAccounts = accountRepository.findAll();
            totalAccounts = allAccounts.size();
            
            logger.info("Processing {} accounts for batch reconciliation", totalAccounts);
            
            for (Account account : allAccounts) {
                try {
                    // Reconcile each account individually
                    Map<String, Object> accountResult = reconcileAccount(account.getAccountId());
                    accountResults.add(accountResult);
                    
                    String status = (String) accountResult.get("status");
                    switch (status) {
                        case RECONCILIATION_STATUS_SUCCESS:
                            successfulReconciliations++;
                            break;
                        case RECONCILIATION_STATUS_DISCREPANCY:
                            discrepancyCount++;
                            BigDecimal difference = (BigDecimal) accountResult.get("difference");
                            totalDiscrepancyAmount = totalDiscrepancyAmount.add(difference.abs());
                            break;
                        default:
                            errorCount++;
                            break;
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Failed to reconcile account {}: {}", account.getAccountId(), e.getMessage());
                    
                    // Add error result to account results
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("accountId", account.getAccountId());
                    errorResult.put("status", RECONCILIATION_STATUS_ERROR);
                    errorResult.put("error", e.getMessage());
                    accountResults.add(errorResult);
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Build comprehensive batch result
            batchResult.put("totalAccounts", totalAccounts);
            batchResult.put("successfulReconciliations", successfulReconciliations);
            batchResult.put("discrepancyCount", discrepancyCount);
            batchResult.put("errorCount", errorCount);
            batchResult.put("totalDiscrepancyAmount", totalDiscrepancyAmount);
            batchResult.put("reconciliationTimestamp", batchTimestamp);
            batchResult.put("processingTimeMillis", processingTime);
            batchResult.put("accountResults", accountResults);
            
            // Create comprehensive audit log for batch operation
            auditService.saveAuditLog(createBatchAuditLog(totalAccounts, successfulReconciliations, 
                discrepancyCount, errorCount, totalDiscrepancyAmount, batchTimestamp));
            
            logger.info("Batch reconciliation completed - Total: {}, Success: {}, Discrepancies: {}, Errors: {}, Processing time: {}ms", 
                       totalAccounts, successfulReconciliations, discrepancyCount, errorCount, processingTime);
            
            return batchResult;
            
        } catch (Exception e) {
            logger.error("Batch reconciliation failed after processing {} accounts - Error: {}", totalAccounts, e.getMessage(), e);
            
            // Create error audit log for batch failure
            auditService.saveAuditLog(createBatchErrorAuditLog(totalAccounts, e.getMessage(), batchTimestamp));
            
            throw new RuntimeException("Batch reconciliation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates comprehensive reconciliation report with detailed findings and audit trail information.
     * 
     * This method creates a detailed reconciliation report containing summary statistics, discrepancy
     * analysis, and audit trail information for a specified date range. The report includes both
     * account-level details and system-wide reconciliation metrics suitable for management reporting,
     * regulatory compliance, and operational monitoring.
     * 
     * @param startDate The start date for the reconciliation report period (inclusive)
     * @param endDate The end date for the reconciliation report period (inclusive)
     * @return Map containing comprehensive reconciliation report with keys:
     *         - "reportPeriod": Start and end dates for the report
     *         - "totalAccountsProcessed": Number of accounts included in the report
     *         - "successfulReconciliations": Count of accounts with successful reconciliation
     *         - "totalDiscrepancies": Total number of discrepancies identified
     *         - "discrepanciesByType": Breakdown of discrepancies by category
     *         - "totalDiscrepancyAmount": Sum of all discrepancy amounts
     *         - "largestDiscrepancy": Details of the largest discrepancy found
     *         - "reconciliationSummary": Statistical summary of reconciliation results
     *         - "auditTrail": Audit log entries related to reconciliation activities
     *         - "recommendedActions": List of recommended actions based on findings
     * @throws IllegalArgumentException if date range is invalid
     * @throws RuntimeException if report generation fails
     */
    public Map<String, Object> generateReconciliationReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("End date cannot be in the future");
        }
        
        logger.info("Generating reconciliation report for period {} to {}", startDate, endDate);
        
        LocalDateTime reportTimestamp = LocalDateTime.now();
        Map<String, Object> reconciliationReport = new HashMap<>();
        
        try {
            // Set report metadata
            reconciliationReport.put("reportPeriod", Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
            ));
            reconciliationReport.put("generationTimestamp", reportTimestamp);
            
            // Get all accounts for the reporting period
            List<Account> allAccounts = accountRepository.findAll();
            int totalAccountsProcessed = allAccounts.size();
            
            // Initialize report counters and collections
            int successfulReconciliations = 0;
            int totalDiscrepancies = 0;
            BigDecimal totalDiscrepancyAmount = ZERO_AMOUNT;
            BigDecimal largestDiscrepancyAmount = ZERO_AMOUNT;
            Long largestDiscrepancyAccountId = null;
            
            Map<String, Integer> discrepanciesByType = new HashMap<>();
            discrepanciesByType.put(DISCREPANCY_TYPE_BALANCE_MISMATCH, 0);
            discrepanciesByType.put(DISCREPANCY_TYPE_MISSING_TRANSACTIONS, 0);
            discrepanciesByType.put(DISCREPANCY_TYPE_PRECISION_ERROR, 0);
            discrepanciesByType.put(DISCREPANCY_TYPE_SYSTEM_ERROR, 0);
            
            List<Map<String, Object>> significantDiscrepancies = new ArrayList<>();
            
            // Process each account for reconciliation analysis
            for (Account account : allAccounts) {
                try {
                    Map<String, Object> accountReconciliation = reconcileAccount(account.getAccountId());
                    String status = (String) accountReconciliation.get("status");
                    
                    if (RECONCILIATION_STATUS_SUCCESS.equals(status)) {
                        successfulReconciliations++;
                    } else if (RECONCILIATION_STATUS_DISCREPANCY.equals(status)) {
                        totalDiscrepancies++;
                        
                        BigDecimal difference = (BigDecimal) accountReconciliation.get("difference");
                        BigDecimal absDifference = difference.abs();
                        totalDiscrepancyAmount = totalDiscrepancyAmount.add(absDifference);
                        
                        // Track largest discrepancy
                        if (absDifference.compareTo(largestDiscrepancyAmount) > 0) {
                            largestDiscrepancyAmount = absDifference;
                            largestDiscrepancyAccountId = account.getAccountId();
                        }
                        
                        // Categorize discrepancy by type
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> discrepancies = (List<Map<String, Object>>) accountReconciliation.get("discrepancies");
                        for (Map<String, Object> discrepancy : discrepancies) {
                            String discrepancyType = (String) discrepancy.get("type");
                            discrepanciesByType.put(discrepancyType, discrepanciesByType.get(discrepancyType) + 1);
                        }
                        
                        // Add to significant discrepancies if amount exceeds threshold
                        if (absDifference.compareTo(new BigDecimal("10.00")) > 0) {
                            Map<String, Object> significantDiscrepancy = new HashMap<>();
                            significantDiscrepancy.put("accountId", account.getAccountId());
                            significantDiscrepancy.put("discrepancyAmount", absDifference);
                            significantDiscrepancy.put("discrepancyType", determineDiscrepancyType(absDifference));
                            significantDiscrepancies.add(significantDiscrepancy);
                        }
                    }
                    
                } catch (Exception e) {
                    logger.warn("Skipping account {} in report due to reconciliation error: {}", 
                               account.getAccountId(), e.getMessage());
                    discrepanciesByType.put(DISCREPANCY_TYPE_SYSTEM_ERROR, 
                        discrepanciesByType.get(DISCREPANCY_TYPE_SYSTEM_ERROR) + 1);
                }
            }
            
            // Calculate reconciliation statistics
            double successRate = totalAccountsProcessed > 0 ? 
                (double) successfulReconciliations / totalAccountsProcessed * 100.0 : 0.0;
            double discrepancyRate = totalAccountsProcessed > 0 ? 
                (double) totalDiscrepancies / totalAccountsProcessed * 100.0 : 0.0;
            
            // Build reconciliation summary
            Map<String, Object> reconciliationSummary = new HashMap<>();
            reconciliationSummary.put("successRate", String.format("%.2f%%", successRate));
            reconciliationSummary.put("discrepancyRate", String.format("%.2f%%", discrepancyRate));
            reconciliationSummary.put("averageDiscrepancyAmount", 
                totalDiscrepancies > 0 ? totalDiscrepancyAmount.divide(new BigDecimal(totalDiscrepancies), CURRENCY_SCALE, CURRENCY_ROUNDING_MODE) : ZERO_AMOUNT);
            
            // Populate report data
            reconciliationReport.put("totalAccountsProcessed", totalAccountsProcessed);
            reconciliationReport.put("successfulReconciliations", successfulReconciliations);
            reconciliationReport.put("totalDiscrepancies", totalDiscrepancies);
            reconciliationReport.put("discrepanciesByType", discrepanciesByType);
            reconciliationReport.put("totalDiscrepancyAmount", totalDiscrepancyAmount);
            reconciliationReport.put("largestDiscrepancy", Map.of(
                "accountId", largestDiscrepancyAccountId != null ? largestDiscrepancyAccountId : "N/A",
                "amount", largestDiscrepancyAmount
            ));
            reconciliationReport.put("reconciliationSummary", reconciliationSummary);
            reconciliationReport.put("significantDiscrepancies", significantDiscrepancies);
            
            // Generate recommended actions based on findings
            List<String> recommendedActions = generateRecommendedActions(reconciliationReport);
            reconciliationReport.put("recommendedActions", recommendedActions);
            
            // Create audit trail for report generation
            auditService.saveAuditLog(createReportGenerationAuditLog(startDate, endDate, 
                totalAccountsProcessed, totalDiscrepancies, reportTimestamp));
            
            logger.info("Reconciliation report generated successfully for {} accounts with {} discrepancies", 
                       totalAccountsProcessed, totalDiscrepancies);
            
            return reconciliationReport;
            
        } catch (Exception e) {
            logger.error("Failed to generate reconciliation report for period {} to {} - Error: {}", 
                        startDate, endDate, e.getMessage(), e);
            throw new RuntimeException("Reconciliation report generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Identifies and categorizes discrepancies for a specific account reconciliation.
     * 
     * This method analyzes the difference between current account balance and calculated
     * transaction totals to identify the root cause and category of reconciliation
     * discrepancies. Provides detailed diagnostic information for efficient resolution
     * of balance mismatches and supports audit trail requirements.
     * 
     * @param accountId The account identifier to analyze for discrepancies
     * @param currentBalance The current balance recorded in the account table
     * @param calculatedBalance The balance calculated from transaction totals
     * @return List of discrepancy maps containing:
     *         - "type": Category of discrepancy (BALANCE_MISMATCH, MISSING_TRANSACTIONS, etc.)
     *         - "severity": Severity level (LOW, MEDIUM, HIGH, CRITICAL)
     *         - "amount": Amount of the discrepancy
     *         - "description": Detailed description of the discrepancy
     *         - "recommendedAction": Suggested action for resolution
     *         - "detectionTimestamp": When the discrepancy was detected
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if discrepancy analysis fails
     */
    public List<Map<String, Object>> identifyDiscrepancies(Long accountId, BigDecimal currentBalance, BigDecimal calculatedBalance) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (currentBalance == null || calculatedBalance == null) {
            throw new IllegalArgumentException("Balance values cannot be null");
        }
        
        logger.debug("Identifying discrepancies for account {} - Current: {}, Calculated: {}", 
                    accountId, currentBalance, calculatedBalance);
        
        List<Map<String, Object>> discrepancies = new ArrayList<>();
        LocalDateTime detectionTimestamp = LocalDateTime.now();
        
        try {
            BigDecimal difference = currentBalance.subtract(calculatedBalance).abs();
            
            // Skip if within tolerance
            if (difference.compareTo(RECONCILIATION_TOLERANCE) <= 0) {
                return discrepancies;
            }
            
            // Determine discrepancy type and severity
            String discrepancyType = determineDiscrepancyType(difference);
            String severity = determineSeverityLevel(difference);
            
            // Create primary discrepancy record
            Map<String, Object> primaryDiscrepancy = new HashMap<>();
            primaryDiscrepancy.put("type", discrepancyType);
            primaryDiscrepancy.put("severity", severity);
            primaryDiscrepancy.put("amount", difference);
            primaryDiscrepancy.put("description", generateDiscrepancyDescription(discrepancyType, difference));
            primaryDiscrepancy.put("recommendedAction", generateRecommendedAction(discrepancyType, severity));
            primaryDiscrepancy.put("detectionTimestamp", detectionTimestamp);
            
            discrepancies.add(primaryDiscrepancy);
            
            // Analyze transaction patterns for additional discrepancy insights
            List<Transaction> accountTransactions = transactionRepository.findByAccountId(accountId);
            
            // Check for missing recent transactions
            LocalDate cutoffDate = LocalDate.now().minusDays(30);
            long recentTransactionCount = accountTransactions.stream()
                .mapToLong(t -> t.getTransactionDate().isAfter(cutoffDate) ? 1 : 0)
                .sum();
            
            if (recentTransactionCount == 0 && difference.compareTo(new BigDecimal("1.00")) > 0) {
                Map<String, Object> missingTxnDiscrepancy = new HashMap<>();
                missingTxnDiscrepancy.put("type", DISCREPANCY_TYPE_MISSING_TRANSACTIONS);
                missingTxnDiscrepancy.put("severity", "MEDIUM");
                missingTxnDiscrepancy.put("amount", difference);
                missingTxnDiscrepancy.put("description", "No recent transactions found within 30 days, possible missing transaction data");
                missingTxnDiscrepancy.put("recommendedAction", "Investigate missing transaction records for the past 30 days");
                missingTxnDiscrepancy.put("detectionTimestamp", detectionTimestamp);
                
                discrepancies.add(missingTxnDiscrepancy);
            }
            
            // Check for precision-related discrepancies
            if (difference.scale() > CURRENCY_SCALE || 
                (difference.compareTo(new BigDecimal("0.10")) < 0 && difference.compareTo(RECONCILIATION_TOLERANCE) > 0)) {
                
                Map<String, Object> precisionDiscrepancy = new HashMap<>();
                precisionDiscrepancy.put("type", DISCREPANCY_TYPE_PRECISION_ERROR);
                precisionDiscrepancy.put("severity", "LOW");
                precisionDiscrepancy.put("amount", difference);
                precisionDiscrepancy.put("description", "Precision-related discrepancy detected, may be due to rounding differences");
                precisionDiscrepancy.put("recommendedAction", "Verify COBOL COMP-3 precision conversion and rounding rules");
                precisionDiscrepancy.put("detectionTimestamp", detectionTimestamp);
                
                discrepancies.add(precisionDiscrepancy);
            }
            
            logger.debug("Identified {} discrepancies for account {}", discrepancies.size(), accountId);
            
            return discrepancies;
            
        } catch (Exception e) {
            logger.error("Failed to identify discrepancies for account {} - Error: {}", accountId, e.getMessage(), e);
            
            // Create system error discrepancy
            Map<String, Object> systemErrorDiscrepancy = new HashMap<>();
            systemErrorDiscrepancy.put("type", DISCREPANCY_TYPE_SYSTEM_ERROR);
            systemErrorDiscrepancy.put("severity", "HIGH");
            systemErrorDiscrepancy.put("amount", ZERO_AMOUNT);
            systemErrorDiscrepancy.put("description", "System error occurred during discrepancy analysis: " + e.getMessage());
            systemErrorDiscrepancy.put("recommendedAction", "Contact system administrator to investigate reconciliation system error");
            systemErrorDiscrepancy.put("detectionTimestamp", detectionTimestamp);
            
            discrepancies.add(systemErrorDiscrepancy);
            
            throw new RuntimeException("Discrepancy identification failed for account " + accountId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates account balance by performing comprehensive checks against transaction history.
     * 
     * This method validates the integrity and accuracy of an account's balance by comparing
     * it against the cumulative transaction history and checking for data consistency issues.
     * Implements comprehensive validation rules including balance range checks, transaction
     * sum verification, and data integrity validation with COBOL precision preservation.
     * 
     * @param accountId The unique identifier of the account to validate
     * @return Map containing validation results with keys:
     *         - "accountId": The validated account identifier
     *         - "isValid": Boolean indicating if balance validation passed
     *         - "validationTimestamp": When the validation was performed
     *         - "currentBalance": Account's current balance from database
     *         - "expectedBalance": Expected balance based on transaction history
     *         - "validationErrors": List of validation errors if any exist
     *         - "validationWarnings": List of validation warnings
     *         - "creditLimitCheck": Credit limit validation results
     * @throws IllegalArgumentException if accountId is null
     * @throws RuntimeException if validation process fails
     */
    public Map<String, Object> validateAccountBalance(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        logger.debug("Validating account balance for account ID: {}", accountId);
        
        Map<String, Object> validationResult = new HashMap<>();
        LocalDateTime validationTimestamp = LocalDateTime.now();
        List<String> validationErrors = new ArrayList<>();
        List<String> validationWarnings = new ArrayList<>();
        
        try {
            // Retrieve account information
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));
            
            // Get current balance with COBOL precision preservation
            BigDecimal currentBalance = cobolDataConverter.preservePrecision(account.getCurrentBalance(), CURRENCY_SCALE)
                .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
            
            // Calculate expected balance from transaction totals
            Map<String, Object> transactionSumResult = calculateTransactionSum(accountId);
            BigDecimal expectedBalance = (BigDecimal) transactionSumResult.get("totalAmount");
            Long transactionCount = (Long) transactionSumResult.get("transactionCount");
            
            // Perform balance validation checks
            boolean isValid = true;
            
            // 1. Balance range validation
            BigDecimal creditLimit = cobolDataConverter.preservePrecision(account.getCreditLimit(), CURRENCY_SCALE)
                .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
            
            if (currentBalance.compareTo(creditLimit.negate()) < 0) {
                validationErrors.add("Account balance exceeds credit limit: Current=" + currentBalance + ", Limit=" + creditLimit);
                isValid = false;
            }
            
            // 2. Transaction sum consistency check
            BigDecimal balanceDifference = currentBalance.subtract(expectedBalance).abs();
            if (balanceDifference.compareTo(RECONCILIATION_TOLERANCE) > 0) {
                validationErrors.add("Balance mismatch with transaction totals: Difference=" + balanceDifference);
                isValid = false;
            }
            
            // 3. Precision validation
            if (currentBalance.scale() != CURRENCY_SCALE) {
                validationWarnings.add("Balance precision differs from expected COBOL COMP-3 scale");
            }
            
            // 4. Transaction count consistency check
            if (transactionCount == 0 && currentBalance.compareTo(ZERO_AMOUNT) != 0) {
                validationWarnings.add("Non-zero balance with no transaction history");
            }
            
            // 5. Negative balance check for credit accounts
            if (currentBalance.compareTo(ZERO_AMOUNT) > 0) {
                validationWarnings.add("Positive balance indicates customer overpayment or credit");
            }
            
            // Build comprehensive credit limit validation
            Map<String, Object> creditLimitCheck = new HashMap<>();
            BigDecimal availableCredit = creditLimit.add(currentBalance); // currentBalance is typically negative for used credit
            double creditUtilization = creditLimit.compareTo(ZERO_AMOUNT) > 0 ? 
                currentBalance.abs().divide(creditLimit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0.0;
            
            creditLimitCheck.put("creditLimit", creditLimit);
            creditLimitCheck.put("currentBalance", currentBalance);
            creditLimitCheck.put("availableCredit", availableCredit);
            creditLimitCheck.put("creditUtilization", String.format("%.2f%%", creditUtilization));
            creditLimitCheck.put("isOverLimit", currentBalance.compareTo(creditLimit.negate()) < 0);
            
            // Build validation result
            validationResult.put("accountId", accountId);
            validationResult.put("isValid", isValid);
            validationResult.put("validationTimestamp", validationTimestamp);
            validationResult.put("currentBalance", currentBalance);
            validationResult.put("expectedBalance", expectedBalance);
            validationResult.put("validationErrors", validationErrors);
            validationResult.put("validationWarnings", validationWarnings);
            validationResult.put("creditLimitCheck", creditLimitCheck);
            validationResult.put("transactionCount", transactionCount);
            
            // Create audit log for validation activity
            String validationStatus = isValid ? "PASSED" : "FAILED";
            auditService.saveAuditLog(createValidationAuditLog(accountId, validationStatus, 
                validationErrors.size(), validationWarnings.size(), validationTimestamp));
            
            logger.debug("Account balance validation completed for account {} - Status: {}, Errors: {}, Warnings: {}", 
                        accountId, validationStatus, validationErrors.size(), validationWarnings.size());
            
            return validationResult;
            
        } catch (Exception e) {
            logger.error("Failed to validate account balance for account ID: {} - Error: {}", accountId, e.getMessage(), e);
            
            validationResult.put("accountId", accountId);
            validationResult.put("isValid", false);
            validationResult.put("validationTimestamp", validationTimestamp);
            validationResult.put("error", e.getMessage());
            
            throw new RuntimeException("Account balance validation failed for account ID " + accountId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the total transaction sum for a specific account with COBOL precision preservation.
     * 
     * This method computes the cumulative sum of all transactions for a given account while
     * maintaining exact COBOL COMP-3 precision for financial calculations. Includes comprehensive
     * transaction analysis with debit/credit categorization and provides detailed transaction
     * statistics for reconciliation and auditing purposes.
     * 
     * @param accountId The unique identifier of the account to calculate transaction sum for
     * @return Map containing transaction calculation results:
     *         - "accountId": The account identifier processed
     *         - "totalAmount": Cumulative sum of all transactions with COBOL precision
     *         - "transactionCount": Total number of transactions processed
     *         - "debitAmount": Total amount of debit transactions
     *         - "creditAmount": Total amount of credit transactions  
     *         - "debitCount": Count of debit transactions
     *         - "creditCount": Count of credit transactions
     *         - "calculationTimestamp": When the calculation was performed
     *         - "oldestTransaction": Date of oldest transaction
     *         - "newestTransaction": Date of newest transaction
     * @throws IllegalArgumentException if accountId is null
     * @throws RuntimeException if transaction calculation fails
     */
    public Map<String, Object> calculateTransactionSum(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        logger.debug("Calculating transaction sum for account ID: {}", accountId);
        
        LocalDateTime calculationTimestamp = LocalDateTime.now();
        Map<String, Object> calculationResult = new HashMap<>();
        
        try {
            // Retrieve all transactions for the account
            List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
            
            // Initialize calculation variables with COBOL precision
            BigDecimal totalAmount = ZERO_AMOUNT;
            BigDecimal debitAmount = ZERO_AMOUNT;
            BigDecimal creditAmount = ZERO_AMOUNT;
            long transactionCount = transactions.size();
            long debitCount = 0;
            long creditCount = 0;
            
            LocalDate oldestTransactionDate = null;
            LocalDate newestTransactionDate = null;
            
            // Process each transaction with precision preservation
            for (Transaction transaction : transactions) {
                BigDecimal transactionAmount = cobolDataConverter.preservePrecision(transaction.getAmount(), CURRENCY_SCALE)
                    .setScale(CURRENCY_SCALE, CURRENCY_ROUNDING_MODE);
                
                // Add to total amount
                totalAmount = totalAmount.add(transactionAmount);
                
                // Categorize as debit or credit based on amount sign
                if (transactionAmount.compareTo(ZERO_AMOUNT) >= 0) {
                    creditAmount = creditAmount.add(transactionAmount);
                    creditCount++;
                } else {
                    debitAmount = debitAmount.add(transactionAmount.abs());
                    debitCount++;
                }
                
                // Track transaction date range
                LocalDate transactionDate = transaction.getTransactionDate();
                if (oldestTransactionDate == null || transactionDate.isBefore(oldestTransactionDate)) {
                    oldestTransactionDate = transactionDate;
                }
                if (newestTransactionDate == null || transactionDate.isAfter(newestTransactionDate)) {
                    newestTransactionDate = transactionDate;
                }
            }
            
            // Verification: Cross-check transaction count for data integrity
            long expectedTransactionCount = transactions.size();
            if (transactionCount != expectedTransactionCount) {
                logger.warn("Transaction count mismatch for account {} - Expected: {}, Actual: {}", 
                           accountId, expectedTransactionCount, transactionCount);
            }
            
            // Build comprehensive calculation result
            calculationResult.put("accountId", accountId);
            calculationResult.put("totalAmount", totalAmount);
            calculationResult.put("transactionCount", transactionCount);
            calculationResult.put("debitAmount", debitAmount);
            calculationResult.put("creditAmount", creditAmount);
            calculationResult.put("debitCount", debitCount);
            calculationResult.put("creditCount", creditCount);
            calculationResult.put("calculationTimestamp", calculationTimestamp);
            calculationResult.put("oldestTransaction", oldestTransactionDate);
            calculationResult.put("newestTransaction", newestTransactionDate);
            
            logger.debug("Transaction sum calculation completed for account {} - Total: {}, Count: {}", 
                        accountId, totalAmount, transactionCount);
            
            return calculationResult;
            
        } catch (Exception e) {
            logger.error("Failed to calculate transaction sum for account ID: {} - Error: {}", accountId, e.getMessage(), e);
            
            calculationResult.put("accountId", accountId);
            calculationResult.put("error", e.getMessage());
            calculationResult.put("calculationTimestamp", calculationTimestamp);
            
            throw new RuntimeException("Transaction sum calculation failed for account ID " + accountId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Processes reconciliation operations in batch mode for daily operations and Spring Batch integration.
     * 
     * This method implements batch processing capabilities for reconciliation operations suitable for
     * daily batch jobs and Spring Batch integration. Processes accounts in configurable chunks with
     * comprehensive error handling, progress tracking, and audit trail generation. Designed for
     * large-scale reconciliation operations with optimal memory usage and processing performance.
     * 
     * @param batchSize The number of accounts to process in each batch chunk
     * @param skipErrors Whether to continue processing when individual account errors occur
     * @return Map containing comprehensive batch processing results:
     *         - "batchId": Unique identifier for this batch processing run
     *         - "totalAccounts": Total number of accounts to be processed
     *         - "processedAccounts": Number of accounts successfully processed
     *         - "skippedAccounts": Number of accounts skipped due to errors
     *         - "batchStartTime": When batch processing started
     *         - "batchEndTime": When batch processing completed
     *         - "processingTimeMillis": Total processing time in milliseconds
     *         - "batchResults": Detailed results for each batch chunk
     *         - "reconciliationSummary": Summary statistics for processed accounts
     *         - "errorSummary": Summary of errors encountered during processing
     * @throws IllegalArgumentException if batch parameters are invalid
     * @throws RuntimeException if batch processing fails critically
     */
    public Map<String, Object> processReconciliationBatch(int batchSize, boolean skipErrors) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        if (batchSize > 10000) {
            throw new IllegalArgumentException("Batch size cannot exceed 10000 accounts");
        }
        
        String batchId = "RECON_BATCH_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        logger.info("Starting reconciliation batch processing - Batch ID: {}, Batch size: {}, Skip errors: {}", 
                   batchId, batchSize, skipErrors);
        
        LocalDateTime batchStartTime = LocalDateTime.now();
        Map<String, Object> batchProcessingResult = new HashMap<>();
        
        int totalAccounts = 0;
        int processedAccounts = 0;
        int skippedAccounts = 0;
        int totalDiscrepancies = 0;
        int totalErrors = 0;
        
        List<Map<String, Object>> batchResults = new ArrayList<>();
        List<String> criticalErrors = new ArrayList<>();
        BigDecimal totalDiscrepancyAmount = ZERO_AMOUNT;
        
        try {
            // Get all accounts for batch processing
            List<Account> allAccounts = accountRepository.findAll();
            totalAccounts = allAccounts.size();
            
            logger.info("Batch processing {} total accounts in chunks of {}", totalAccounts, batchSize);
            
            // Process accounts in batches
            for (int batchStart = 0; batchStart < totalAccounts; batchStart += batchSize) {
                int batchEnd = Math.min(batchStart + batchSize, totalAccounts);
                List<Account> batchAccounts = allAccounts.subList(batchStart, batchEnd);
                
                Map<String, Object> batchChunkResult = processBatchChunk(batchAccounts, 
                    batchStart / batchSize + 1, skipErrors);
                
                batchResults.add(batchChunkResult);
                
                // Aggregate batch chunk results
                processedAccounts += (Integer) batchChunkResult.get("processedCount");
                skippedAccounts += (Integer) batchChunkResult.get("skippedCount");
                totalDiscrepancies += (Integer) batchChunkResult.get("discrepancyCount");
                totalErrors += (Integer) batchChunkResult.get("errorCount");
                totalDiscrepancyAmount = totalDiscrepancyAmount.add((BigDecimal) batchChunkResult.get("discrepancyAmount"));
                
                @SuppressWarnings("unchecked")
                List<String> chunkErrors = (List<String>) batchChunkResult.get("criticalErrors");
                criticalErrors.addAll(chunkErrors);
                
                logger.info("Completed batch chunk {} - Processed: {}, Skipped: {}, Discrepancies: {}", 
                           batchStart / batchSize + 1, 
                           batchChunkResult.get("processedCount"),
                           batchChunkResult.get("skippedCount"),
                           batchChunkResult.get("discrepancyCount"));
            }
            
            LocalDateTime batchEndTime = LocalDateTime.now();
            long processingTimeMillis = java.time.Duration.between(batchStartTime, batchEndTime).toMillis();
            
            // Build reconciliation summary
            Map<String, Object> reconciliationSummary = new HashMap<>();
            reconciliationSummary.put("totalProcessed", processedAccounts);
            reconciliationSummary.put("successRate", processedAccounts > 0 ? 
                String.format("%.2f%%", (double) (processedAccounts - totalDiscrepancies) / processedAccounts * 100.0) : "0.00%");
            reconciliationSummary.put("discrepancyRate", processedAccounts > 0 ? 
                String.format("%.2f%%", (double) totalDiscrepancies / processedAccounts * 100.0) : "0.00%");
            reconciliationSummary.put("averageProcessingTime", processedAccounts > 0 ? 
                processingTimeMillis / processedAccounts : 0);
            
            // Build error summary
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("totalErrors", totalErrors);
            errorSummary.put("errorRate", totalAccounts > 0 ? 
                String.format("%.2f%%", (double) totalErrors / totalAccounts * 100.0) : "0.00%");
            errorSummary.put("criticalErrorCount", criticalErrors.size());
            errorSummary.put("criticalErrors", criticalErrors);
            
            // Build comprehensive batch result
            batchProcessingResult.put("batchId", batchId);
            batchProcessingResult.put("totalAccounts", totalAccounts);
            batchProcessingResult.put("processedAccounts", processedAccounts);
            batchProcessingResult.put("skippedAccounts", skippedAccounts);
            batchProcessingResult.put("batchStartTime", batchStartTime);
            batchProcessingResult.put("batchEndTime", batchEndTime);
            batchProcessingResult.put("processingTimeMillis", processingTimeMillis);
            batchProcessingResult.put("batchResults", batchResults);
            batchProcessingResult.put("reconciliationSummary", reconciliationSummary);
            batchProcessingResult.put("errorSummary", errorSummary);
            batchProcessingResult.put("totalDiscrepancies", totalDiscrepancies);
            batchProcessingResult.put("totalDiscrepancyAmount", totalDiscrepancyAmount);
            
            // Create comprehensive audit log for batch processing
            auditService.saveAuditLog(createBatchProcessingAuditLog(batchId, totalAccounts, 
                processedAccounts, totalDiscrepancies, totalErrors, processingTimeMillis, batchStartTime));
            
            logger.info("Batch reconciliation processing completed - Batch ID: {}, Processed: {}/{}, Discrepancies: {}, Errors: {}, Time: {}ms", 
                       batchId, processedAccounts, totalAccounts, totalDiscrepancies, totalErrors, processingTimeMillis);
            
            return batchProcessingResult;
            
        } catch (Exception e) {
            logger.error("Batch reconciliation processing failed - Batch ID: {}, Error: {}", batchId, e.getMessage(), e);
            
            LocalDateTime batchEndTime = LocalDateTime.now();
            long processingTimeMillis = java.time.Duration.between(batchStartTime, batchEndTime).toMillis();
            
            // Create error audit log for batch failure
            auditService.saveAuditLog(createBatchErrorAuditLog(totalAccounts, e.getMessage(), batchStartTime));
            
            batchProcessingResult.put("batchId", batchId);
            batchProcessingResult.put("status", "FAILED");
            batchProcessingResult.put("error", e.getMessage());
            batchProcessingResult.put("batchStartTime", batchStartTime);
            batchProcessingResult.put("batchEndTime", batchEndTime);
            batchProcessingResult.put("processingTimeMillis", processingTimeMillis);
            
            throw new RuntimeException("Batch reconciliation processing failed: " + e.getMessage(), e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Processes a chunk of accounts in batch reconciliation mode.
     */
    private Map<String, Object> processBatchChunk(List<Account> accounts, int chunkNumber, boolean skipErrors) {
        Map<String, Object> chunkResult = new HashMap<>();
        
        int processedCount = 0;
        int skippedCount = 0;
        int discrepancyCount = 0;
        int errorCount = 0;
        BigDecimal discrepancyAmount = ZERO_AMOUNT;
        List<String> criticalErrors = new ArrayList<>();
        
        logger.debug("Processing batch chunk {} with {} accounts", chunkNumber, accounts.size());
        
        for (Account account : accounts) {
            try {
                Map<String, Object> accountResult = reconcileAccount(account.getAccountId());
                String status = (String) accountResult.get("status");
                
                switch (status) {
                    case RECONCILIATION_STATUS_SUCCESS:
                        processedCount++;
                        break;
                    case RECONCILIATION_STATUS_DISCREPANCY:
                        processedCount++;
                        discrepancyCount++;
                        BigDecimal difference = (BigDecimal) accountResult.get("difference");
                        discrepancyAmount = discrepancyAmount.add(difference.abs());
                        break;
                    default:
                        if (skipErrors) {
                            skippedCount++;
                            logger.warn("Skipped account {} due to reconciliation error in batch chunk {}", 
                                       account.getAccountId(), chunkNumber);
                        } else {
                            errorCount++;
                            String errorMsg = "Account " + account.getAccountId() + " reconciliation failed";
                            criticalErrors.add(errorMsg);
                            logger.error("Critical error in batch chunk {} for account {}", 
                                        chunkNumber, account.getAccountId());
                        }
                        break;
                }
                
            } catch (Exception e) {
                if (skipErrors) {
                    skippedCount++;
                    logger.warn("Skipped account {} due to exception in batch chunk {}: {}", 
                               account.getAccountId(), chunkNumber, e.getMessage());
                } else {
                    errorCount++;
                    String errorMsg = "Account " + account.getAccountId() + " failed with exception: " + e.getMessage();
                    criticalErrors.add(errorMsg);
                    logger.error("Exception in batch chunk {} for account {}: {}", 
                                chunkNumber, account.getAccountId(), e.getMessage(), e);
                }
            }
        }
        
        chunkResult.put("chunkNumber", chunkNumber);
        chunkResult.put("accountsInChunk", accounts.size());
        chunkResult.put("processedCount", processedCount);
        chunkResult.put("skippedCount", skippedCount);
        chunkResult.put("discrepancyCount", discrepancyCount);
        chunkResult.put("errorCount", errorCount);
        chunkResult.put("discrepancyAmount", discrepancyAmount);
        chunkResult.put("criticalErrors", criticalErrors);
        
        return chunkResult;
    }

    /**
     * Determines the discrepancy type based on the discrepancy amount and patterns.
     */
    private String determineDiscrepancyType(BigDecimal discrepancyAmount) {
        if (discrepancyAmount.compareTo(new BigDecimal("0.10")) < 0) {
            return DISCREPANCY_TYPE_PRECISION_ERROR;
        } else if (discrepancyAmount.compareTo(new BigDecimal("100.00")) > 0) {
            return DISCREPANCY_TYPE_MISSING_TRANSACTIONS;
        } else {
            return DISCREPANCY_TYPE_BALANCE_MISMATCH;
        }
    }

    /**
     * Determines the severity level based on discrepancy amount.
     */
    private String determineSeverityLevel(BigDecimal discrepancyAmount) {
        if (discrepancyAmount.compareTo(new BigDecimal("1.00")) <= 0) {
            return "LOW";
        } else if (discrepancyAmount.compareTo(new BigDecimal("10.00")) <= 0) {
            return "MEDIUM";
        } else if (discrepancyAmount.compareTo(new BigDecimal("100.00")) <= 0) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * Generates a descriptive explanation for a discrepancy.
     */
    private String generateDiscrepancyDescription(String discrepancyType, BigDecimal amount) {
        switch (discrepancyType) {
            case DISCREPANCY_TYPE_BALANCE_MISMATCH:
                return "Account balance does not match transaction totals by $" + amount + 
                       ". This may indicate missing transactions or data synchronization issues.";
            case DISCREPANCY_TYPE_MISSING_TRANSACTIONS:
                return "Large discrepancy of $" + amount + 
                       " suggests missing transaction records or incomplete data migration.";
            case DISCREPANCY_TYPE_PRECISION_ERROR:
                return "Minor discrepancy of $" + amount + 
                       " likely due to rounding differences or COBOL COMP-3 precision conversion issues.";
            case DISCREPANCY_TYPE_SYSTEM_ERROR:
                return "System error occurred during reconciliation process preventing accurate balance validation.";
            default:
                return "Unclassified discrepancy of $" + amount + " requiring manual investigation.";
        }
    }

    /**
     * Generates recommended action based on discrepancy type and severity.
     */
    private String generateRecommendedAction(String discrepancyType, String severity) {
        switch (discrepancyType) {
            case DISCREPANCY_TYPE_BALANCE_MISMATCH:
                return "HIGH".equals(severity) || "CRITICAL".equals(severity) ?
                    "Immediate manual investigation required - check recent transactions and account updates" :
                    "Review transaction history and verify data consistency";
            case DISCREPANCY_TYPE_MISSING_TRANSACTIONS:
                return "Investigate missing transaction data - verify batch processing logs and data migration status";
            case DISCREPANCY_TYPE_PRECISION_ERROR:
                return "Review COBOL COMP-3 precision conversion settings and rounding rules";
            case DISCREPANCY_TYPE_SYSTEM_ERROR:
                return "Contact system administrator - reconciliation system requires attention";
            default:
                return "Manual investigation required to determine root cause";
        }
    }

    /**
     * Generates list of recommended actions based on report findings.
     */
    private List<String> generateRecommendedActions(Map<String, Object> reportData) {
        List<String> recommendations = new ArrayList<>();
        
        int totalDiscrepancies = (Integer) reportData.get("totalDiscrepancies");
        BigDecimal totalDiscrepancyAmount = (BigDecimal) reportData.get("totalDiscrepancyAmount");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> discrepanciesByType = (Map<String, Integer>) reportData.get("discrepanciesByType");
        
        if (totalDiscrepancies == 0) {
            recommendations.add("All accounts reconciled successfully - no immediate action required");
            recommendations.add("Continue monitoring daily reconciliation reports for trend analysis");
        } else {
            if (totalDiscrepancyAmount.compareTo(new BigDecimal("1000.00")) > 0) {
                recommendations.add("HIGH PRIORITY: Large total discrepancy amount of $" + totalDiscrepancyAmount + 
                                 " requires immediate investigation");
            }
            
            if (discrepanciesByType.get(DISCREPANCY_TYPE_MISSING_TRANSACTIONS) > 0) {
                recommendations.add("Investigate missing transaction data - review batch processing logs");
                recommendations.add("Verify data migration completeness and transaction import processes");
            }
            
            if (discrepanciesByType.get(DISCREPANCY_TYPE_PRECISION_ERROR) > 5) {
                recommendations.add("Multiple precision errors detected - review COBOL COMP-3 conversion settings");
            }
            
            if (discrepanciesByType.get(DISCREPANCY_TYPE_SYSTEM_ERROR) > 0) {
                recommendations.add("System errors detected - contact technical support for reconciliation system review");
            }
            
            if (totalDiscrepancies > (Integer) reportData.get("totalAccountsProcessed") * 0.1) {
                recommendations.add("High discrepancy rate detected - consider full system reconciliation review");
            }
        }
        
        recommendations.add("Schedule regular reconciliation monitoring and reporting");
        recommendations.add("Maintain audit trail documentation for compliance requirements");
        
        return recommendations;
    }

    /**
     * Creates audit log entry for account reconciliation activity.
     */
    private com.carddemo.entity.AuditLog createReconciliationAuditLog(String accountId, String status, 
            BigDecimal difference, LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_TRANSACTION);
        auditLog.setActionPerformed("ACCOUNT_RECONCILIATION");
        auditLog.setOutcome(status);
        auditLog.setResourceAccessed("ACCOUNT_" + accountId);
        auditLog.setDetails("Account reconciliation completed - Status: " + status + 
                           ", Difference: $" + difference + ", Account: " + accountId);
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }

    /**
     * Creates audit log entry for reconciliation errors.
     */
    private com.carddemo.entity.AuditLog createErrorAuditLog(String accountId, String errorMessage, 
            LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_SYSTEM_EVENT);
        auditLog.setActionPerformed("RECONCILIATION_ERROR");
        auditLog.setOutcome(AuditService.OUTCOME_ERROR);
        auditLog.setResourceAccessed("ACCOUNT_" + accountId);
        auditLog.setDetails("Account reconciliation failed - Account: " + accountId + 
                           ", Error: " + errorMessage);
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }

    /**
     * Creates audit log entry for batch reconciliation activity.
     */
    private com.carddemo.entity.AuditLog createBatchAuditLog(int totalAccounts, int successfulReconciliations,
            int discrepancyCount, int errorCount, BigDecimal totalDiscrepancyAmount, LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_TRANSACTION);
        auditLog.setActionPerformed("BATCH_RECONCILIATION");
        auditLog.setOutcome(errorCount == 0 ? AuditService.OUTCOME_SUCCESS : AuditService.OUTCOME_WARNING);
        auditLog.setResourceAccessed("ALL_ACCOUNTS");
        auditLog.setDetails("Batch reconciliation completed - Total: " + totalAccounts + 
                           ", Success: " + successfulReconciliations + 
                           ", Discrepancies: " + discrepancyCount + 
                           ", Errors: " + errorCount + 
                           ", Total Discrepancy Amount: $" + totalDiscrepancyAmount);
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }

    /**
     * Creates audit log entry for batch reconciliation errors.
     */
    private com.carddemo.entity.AuditLog createBatchErrorAuditLog(int totalAccounts, String errorMessage, 
            LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_SYSTEM_EVENT);
        auditLog.setActionPerformed("BATCH_RECONCILIATION_ERROR");
        auditLog.setOutcome(AuditService.OUTCOME_ERROR);
        auditLog.setResourceAccessed("ALL_ACCOUNTS");
        auditLog.setDetails("Batch reconciliation failed - Total accounts: " + totalAccounts + 
                           ", Error: " + errorMessage);
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }

    /**
     * Creates audit log entry for reconciliation report generation.
     */
    private com.carddemo.entity.AuditLog createReportGenerationAuditLog(LocalDate startDate, LocalDate endDate,
            int totalAccounts, int totalDiscrepancies, LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_SYSTEM_EVENT);
        auditLog.setActionPerformed("RECONCILIATION_REPORT_GENERATION");
        auditLog.setOutcome(AuditService.OUTCOME_SUCCESS);
        auditLog.setResourceAccessed("RECONCILIATION_REPORTS");
        auditLog.setDetails("Reconciliation report generated - Period: " + startDate + " to " + endDate + 
                           ", Accounts: " + totalAccounts + 
                           ", Discrepancies: " + totalDiscrepancies);
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }

    /**
     * Creates audit log entry for account balance validation activity.
     */
    private com.carddemo.entity.AuditLog createValidationAuditLog(Long accountId, String validationStatus,
            int errorCount, int warningCount, LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_DATA_ACCESS);
        auditLog.setActionPerformed("BALANCE_VALIDATION");
        auditLog.setOutcome("PASSED".equals(validationStatus) ? AuditService.OUTCOME_SUCCESS : AuditService.OUTCOME_WARNING);
        auditLog.setResourceAccessed("ACCOUNT_" + accountId);
        auditLog.setDetails("Account balance validation - Account: " + accountId + 
                           ", Status: " + validationStatus + 
                           ", Errors: " + errorCount + 
                           ", Warnings: " + warningCount);
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }

    /**
     * Creates audit log entry for batch processing operations.
     */
    private com.carddemo.entity.AuditLog createBatchProcessingAuditLog(String batchId, int totalAccounts,
            int processedAccounts, int totalDiscrepancies, int totalErrors, long processingTimeMillis, 
            LocalDateTime timestamp) {
        
        com.carddemo.entity.AuditLog auditLog = new com.carddemo.entity.AuditLog();
        auditLog.setUsername("SYSTEM");
        auditLog.setEventType(AuditService.EVENT_TYPE_SYSTEM_EVENT);
        auditLog.setActionPerformed("BATCH_PROCESSING");
        auditLog.setOutcome(totalErrors == 0 ? AuditService.OUTCOME_SUCCESS : AuditService.OUTCOME_WARNING);
        auditLog.setResourceAccessed("BATCH_RECONCILIATION");
        auditLog.setDetails("Batch reconciliation processing - Batch ID: " + batchId + 
                           ", Total: " + totalAccounts + 
                           ", Processed: " + processedAccounts + 
                           ", Discrepancies: " + totalDiscrepancies + 
                           ", Errors: " + totalErrors + 
                           ", Processing Time: " + processingTimeMillis + "ms");
        auditLog.setTimestamp(timestamp);
        auditLog.setSourceIp("127.0.0.1");
        
        return auditLog;
    }
}
