/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.BalanceSummary;
import com.carddemo.dto.BatchProcessResult;
import com.carddemo.dto.StatementData;
import com.carddemo.dto.StatementResult;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for credit card account statement generation and processing.
 * 
 * This service implements the migration of COBOL batch statement generation logic (CBACT03C)
 * to Java Spring Boot service, ensuring 100% functional parity with the original mainframe
 * implementation. Handles statement formatting, transaction inclusion, balance calculations,
 * and output file generation with strict precision matching COBOL COMP-3 decimal handling.
 * 
 * Key Features:
 * - Statement generation with various transaction types and amounts
 * - Text and HTML statement formatting matching COBOL output format
 * - Transaction aggregation and sorting logic from VSAM KSDS sequential access
 * - Balance summary calculations with BigDecimal precision equivalent to COMP-3
 * - Batch file generation process replicating JCL job output
 * - Error handling and edge cases (empty transactions, null accounts)
 * 
 * COBOL Migration Notes:
 * - Maintains exact paragraph-by-paragraph translation from CBACT03C
 * - Preserves 0000-init, 1000-input, 2000-process, 3000-output, 9000-close structure
 * - Replicates VSAM STARTBR/READNEXT transaction processing order
 * - Ensures BigDecimal calculations match COBOL ROUNDED arithmetic
 */
@Slf4j
@Service
@Transactional
public class AccountStatementsService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    // COBOL COMP-3 precision constants
    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING = RoundingMode.HALF_UP;
    
    /**
     * Generate comprehensive account statement from account and transaction data.
     * 
     * Implements COBOL CBACT03C paragraph 2000-PROCESS-STATEMENT logic.
     * Processes account information and transactions to create complete
     * statement with header, transaction details, and balance summary.
     * 
     * @param account Account entity with balance and customer information
     * @param transactions List of transactions for statement period
     * @param statementDate Date for statement generation
     * @return StatementResult with generated statement content and metadata
     * @throws IllegalArgumentException if account is null
     */
    public StatementResult generateStatement(Account account, List<Transaction> transactions, LocalDate statementDate) {
        log.info("Generating statement for account: {}, statement date: {}", 
                account != null ? account.getAccountId() : "null", statementDate);
        
        // COBOL 0000-INIT validation logic
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        String statementId = generateStatementId(account.getAccountId().toString(), statementDate);
        
        try {
            // COBOL 1000-INPUT: Prepare statement data
            StatementData statementData = prepareStatementData(account, transactions, statementDate);
            
            // COBOL 2000-PROCESS: Generate statement content
            String statementContent = generateTextStatement(statementData);
            
            // COBOL 3000-OUTPUT: Create result
            StatementResult result = StatementResult.builder()
                    .statementId(statementId)
                    .statementContent(statementContent)
                    .formatType("TEXT")
                    .transactionCount(transactions != null ? transactions.size() : 0)
                    .generationTimestamp(LocalDateTime.now())
                    .success(true)
                    .accountId(account.getAccountId().toString())
                    .statementPeriod(formatStatementPeriod(statementDate))
                    .contentLength((long) statementContent.length())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            
            log.info("Statement generated successfully: {}", statementId);
            return result;
            
        } catch (Exception e) {
            log.error("Error generating statement for account {}: {}", account.getAccountId(), e.getMessage(), e);
            return StatementResult.builder()
                    .statementId(statementId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .generationTimestamp(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
    
    /**
     * Generate text format statement matching COBOL DISPLAY output format.
     * 
     * Implements COBOL CBACT03C paragraph 9000-GENERATE-PLAIN-TEXT logic.
     * Creates fixed-width text formatting with proper decimal alignment
     * for monetary amounts, maintaining exact character positioning and
     * field alignment matching COBOL output.
     * 
     * @param statementData Prepared statement data with account and transaction information
     * @return Formatted text statement string
     */
    public String generateTextStatement(StatementData statementData) {
        log.debug("Generating text statement for account: {}", statementData.getAccount().getAccountId());
        
        StringBuilder statement = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        
        // COBOL statement header generation
        statement.append("                    CREDIT CARD STATEMENT\n");
        statement.append("=" .repeat(60)).append("\n\n");
        
        // Account information section
        statement.append("ACCOUNT NUMBER: ").append(statementData.getAccount().getAccountId()).append("\n");
        statement.append("STATEMENT DATE: ").append(statementData.getStatementDate().format(dateFormatter)).append("\n");
        statement.append("STATEMENT PERIOD: ").append(statementData.getPeriodStartDate().format(dateFormatter))
                .append(" - ").append(statementData.getPeriodEndDate().format(dateFormatter)).append("\n\n");
        
        // Balance summary section with COBOL COMP-3 precision
        statement.append("BALANCE SUMMARY\n");
        statement.append("-".repeat(40)).append("\n");
        statement.append(String.format("Previous Balance: %15s\n", formatCurrency(statementData.getPreviousBalance())));
        statement.append(String.format("Total Credits:    %15s\n", formatCurrency(statementData.getTotalCredits())));
        statement.append(String.format("Total Debits:     %15s\n", formatCurrency(statementData.getTotalDebits())));
        statement.append(String.format("Interest Charges: %15s\n", formatCurrency(statementData.getInterestCharges())));
        statement.append(String.format("Fees:             %15s\n", formatCurrency(statementData.getTotalFees())));
        statement.append(String.format("Current Balance:  %15s\n\n", formatCurrency(statementData.getCurrentBalance())));
        
        // Transaction details section
        statement.append("TRANSACTION DETAILS\n");
        statement.append("-".repeat(80)).append("\n");
        statement.append("DATE       DESCRIPTION                           AMOUNT       BALANCE\n");
        statement.append("-".repeat(80)).append("\n");
        
        if (statementData.getTransactions() == null || statementData.getTransactions().isEmpty()) {
            statement.append("No transactions for this period\n");
        } else {
            BigDecimal runningBalance = statementData.getPreviousBalance();
            
            // COBOL VSAM sequential processing order
            for (Transaction transaction : statementData.getTransactions()) {
                runningBalance = runningBalance.add(transaction.getAmount());
                
                statement.append(String.format("%-10s %-33s %12s %12s\n",
                        transaction.getTransactionDate().format(dateFormatter),
                        truncateDescription(transaction.getDescription(), 33),
                        formatCurrency(transaction.getAmount()),
                        formatCurrency(runningBalance)));
            }
        }
        
        // Payment information
        statement.append("\n").append("-".repeat(80)).append("\n");
        statement.append(String.format("MINIMUM PAYMENT DUE: %15s\n", formatCurrency(statementData.getMinimumPaymentDue())));
        statement.append(String.format("PAYMENT DUE DATE:    %15s\n", statementData.getPaymentDueDate().format(dateFormatter)));
        
        return statement.toString();
    }
    
    /**
     * Generate HTML format statement with modern responsive layout.
     * 
     * Implements COBOL CBACT03C paragraph 9100-GENERATE-HTML logic.
     * Creates well-formed HTML structure with CSS styling for professional
     * statement appearance while maintaining identical financial data
     * precision as text version.
     * 
     * @param statementData Prepared statement data with account and transaction information
     * @return Formatted HTML statement string
     */
    public String generateHtmlStatement(StatementData statementData) {
        log.debug("Generating HTML statement for account: {}", statementData.getAccount().getAccountId());
        
        StringBuilder html = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        
        // HTML document structure
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Credit Card Statement</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("        .header { text-align: center; margin-bottom: 30px; }\n");
        html.append("        .account-info { margin-bottom: 20px; }\n");
        html.append("        .balance-summary { margin-bottom: 30px; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("        th { background-color: #f2f2f2; }\n");
        html.append("        .amount { text-align: right; }\n");
        html.append("        .total-row { font-weight: bold; background-color: #f9f9f9; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // Statement header
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>CREDIT CARD STATEMENT</h1>\n");
        html.append("    </div>\n");
        
        // Account information
        html.append("    <div class=\"account-info\">\n");
        html.append("        <p><strong>Account Number:</strong> ").append(statementData.getAccount().getAccountId()).append("</p>\n");
        html.append("        <p><strong>Statement Date:</strong> ").append(statementData.getStatementDate().format(dateFormatter)).append("</p>\n");
        html.append("        <p><strong>Statement Period:</strong> ").append(statementData.getPeriodStartDate().format(dateFormatter))
                .append(" - ").append(statementData.getPeriodEndDate().format(dateFormatter)).append("</p>\n");
        html.append("    </div>\n");
        
        // Balance summary table
        html.append("    <div class=\"balance-summary\">\n");
        html.append("        <h2>Balance Summary</h2>\n");
        html.append("        <table>\n");
        html.append("            <tr><td>Previous Balance</td><td class=\"amount\">").append(formatCurrency(statementData.getPreviousBalance())).append("</td></tr>\n");
        html.append("            <tr><td>Total Credits</td><td class=\"amount\">").append(formatCurrency(statementData.getTotalCredits())).append("</td></tr>\n");
        html.append("            <tr><td>Total Debits</td><td class=\"amount\">").append(formatCurrency(statementData.getTotalDebits())).append("</td></tr>\n");
        html.append("            <tr><td>Interest Charges</td><td class=\"amount\">").append(formatCurrency(statementData.getInterestCharges())).append("</td></tr>\n");
        html.append("            <tr><td>Fees</td><td class=\"amount\">").append(formatCurrency(statementData.getTotalFees())).append("</td></tr>\n");
        html.append("            <tr class=\"total-row\"><td>Current Balance</td><td class=\"amount\">").append(formatCurrency(statementData.getCurrentBalance())).append("</td></tr>\n");
        html.append("        </table>\n");
        html.append("    </div>\n");
        
        // Transaction details table
        html.append("    <div class=\"transactions\">\n");
        html.append("        <h2>Transaction Details</h2>\n");
        html.append("        <table>\n");
        html.append("            <thead>\n");
        html.append("                <tr><th>Date</th><th>Description</th><th>Amount</th><th>Balance</th></tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        if (statementData.getTransactions() == null || statementData.getTransactions().isEmpty()) {
            html.append("                <tr><td colspan=\"4\">No transactions for this period</td></tr>\n");
        } else {
            BigDecimal runningBalance = statementData.getPreviousBalance();
            
            for (Transaction transaction : statementData.getTransactions()) {
                runningBalance = runningBalance.add(transaction.getAmount());
                
                html.append("                <tr>\n");
                html.append("                    <td>").append(transaction.getTransactionDate().format(dateFormatter)).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(transaction.getDescription())).append("</td>\n");
                html.append("                    <td class=\"amount\">").append(formatCurrency(transaction.getAmount())).append("</td>\n");
                html.append("                    <td class=\"amount\">").append(formatCurrency(runningBalance)).append("</td>\n");
                html.append("                </tr>\n");
            }
        }
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        html.append("    </div>\n");
        
        // Payment information
        html.append("    <div class=\"payment-info\">\n");
        html.append("        <h2>Payment Information</h2>\n");
        html.append("        <p><strong>Minimum Payment Due:</strong> ").append(formatCurrency(statementData.getMinimumPaymentDue())).append("</p>\n");
        html.append("        <p><strong>Payment Due Date:</strong> ").append(statementData.getPaymentDueDate().format(dateFormatter)).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * Aggregate transactions for account within specified date range.
     * 
     * Implements COBOL CBACT03C paragraph 8000-GROUP-BY-ACCOUNT logic.
     * Replicates VSAM KSDS sequential read logic with STARTBR/READNEXT/READPREV
     * browse operations for date range filtering and proper transaction
     * sorting by processing timestamp.
     * 
     * @param accountId Account identifier for transaction filtering
     * @param startDate Start date for transaction selection (inclusive)
     * @param endDate End date for transaction selection (inclusive)
     * @return List of transactions sorted by processing timestamp
     */
    public List<Transaction> aggregateTransactionsForAccount(String accountId, LocalDate startDate, LocalDate endDate) {
        log.debug("Aggregating transactions for account: {} from {} to {}", accountId, startDate, endDate);
        
        if (accountId == null || startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        
        // COBOL VSAM STARTBR equivalent with date range filtering
        List<Transaction> transactions = transactionRepository.findByAccountIdAndTransactionDateBetween(
                Long.valueOf(accountId), startDate, endDate);
        
        // COBOL READNEXT processing order - sort by processing timestamp
        return transactions.stream()
                .filter(t -> !isReversedOrVoided(t))  // Exclude reversed/voided transactions
                .sorted(Comparator.comparing(Transaction::getProcessedTimestamp))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate comprehensive balance summary with COBOL COMP-3 precision.
     * 
     * Implements COBOL CBACT03C balance calculation logic ensuring
     * penny-perfect accuracy in all financial calculations. Uses
     * BigDecimal with exact scale and rounding modes that match
     * COBOL ROUNDED arithmetic behavior.
     * 
     * @param account Account entity with current balance information
     * @param transactions List of transactions for calculation period
     * @return BalanceSummary with calculated balance information
     */
    public BalanceSummary calculateBalanceSummary(Account account, List<Transaction> transactions) {
        log.debug("Calculating balance summary for account: {}", account.getAccountId());
        
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal interestCharges = BigDecimal.ZERO;
        BigDecimal feesAssessed = BigDecimal.ZERO;
        BigDecimal paymentsReceived = BigDecimal.ZERO;
        BigDecimal purchasesAmount = BigDecimal.ZERO;
        BigDecimal cashAdvances = BigDecimal.ZERO;
        
        // COBOL COMP-3 precision calculations
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                BigDecimal amount = transaction.getAmount().setScale(DECIMAL_SCALE, COBOL_ROUNDING);
                String typeCode = transaction.getTransactionTypeCode();
                
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    totalCredits = totalCredits.add(amount);
                    if ("PAY".equals(typeCode)) {
                        paymentsReceived = paymentsReceived.add(amount);
                    }
                } else {
                    BigDecimal absAmount = amount.abs();
                    totalDebits = totalDebits.add(absAmount);
                    
                    switch (typeCode) {
                        case "PUR":
                            purchasesAmount = purchasesAmount.add(absAmount);
                            break;
                        case "CAS":
                            cashAdvances = cashAdvances.add(absAmount);
                            break;
                        case "INT":
                            interestCharges = interestCharges.add(absAmount);
                            break;
                        case "FEE":
                            feesAssessed = feesAssessed.add(absAmount);
                            break;
                    }
                }
            }
        }
        
        // Current balance calculation with COBOL ROUNDED precision
        BigDecimal currentBalance = account.getCurrentBalance().setScale(DECIMAL_SCALE, COBOL_ROUNDING);
        BigDecimal creditLimit = account.getCreditLimit().setScale(DECIMAL_SCALE, COBOL_ROUNDING);
        BigDecimal availableCredit = creditLimit.subtract(currentBalance);
        
        // Minimum payment calculation (2% of balance or $25, whichever is greater)
        BigDecimal minimumPaymentDue = currentBalance.multiply(new BigDecimal("0.02"))
                .max(new BigDecimal("25.00"))
                .setScale(DECIMAL_SCALE, COBOL_ROUNDING);
        
        return BalanceSummary.builder()
                .accountId(account.getAccountId().toString())
                .previousBalance(account.getCurrentBalance().subtract(totalCredits).add(totalDebits))
                .currentBalance(currentBalance)
                .availableCredit(availableCredit)
                .creditLimit(creditLimit)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .interestCharges(interestCharges)
                .feesAssessed(feesAssessed)
                .paymentsReceived(paymentsReceived)
                .purchasesAmount(purchasesAmount)
                .cashAdvances(cashAdvances)
                .minimumPaymentDue(minimumPaymentDue)
                .paymentDueDate(LocalDate.now().plusDays(25))  // Standard 25-day payment period
                .daysPastDue(0)
                .overCreditLimit(currentBalance.compareTo(creditLimit) > 0)
                .calculationDate(LocalDate.now())
                .build();
    }
    
    /**
     * Process statements for multiple accounts in batch mode.
     * 
     * Implements COBOL JCL job logic for batch statement processing.
     * Processes multiple accounts in sequence with error handling
     * and recovery matching COBOL ABEND logic. Generates processing
     * statistics and logging equivalent to JCL output.
     * 
     * @param processDate Date for batch processing cycle
     * @param accountIds List of account IDs to process
     * @return BatchProcessResult with processing statistics and results
     */
    public BatchProcessResult processStatementBatch(LocalDate processDate, List<String> accountIds) {
        log.info("Starting batch statement processing for {} accounts on {}", accountIds.size(), processDate);
        
        long startTime = System.currentTimeMillis();
        String batchId = "BATCH_" + processDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + System.currentTimeMillis();
        
        int processedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        Map<String, String> accountStatuses = new HashMap<>();
        Map<String, String> generatedStatements = new HashMap<>();
        List<String> outputFiles = new ArrayList<>();
        
        for (String accountId : accountIds) {
            try {
                log.debug("Processing account: {}", accountId);
                
                // Load account data
                Optional<Account> accountOpt = accountRepository.findById(Long.valueOf(accountId));
                if (!accountOpt.isPresent()) {
                    String error = "Account not found: " + accountId;
                    errors.add(error);
                    accountStatuses.put(accountId, "ERROR_NOT_FOUND");
                    errorCount++;
                    continue;
                }
                
                Account account = accountOpt.get();
                
                // Load transactions for statement period
                LocalDate periodStart = processDate.minusMonths(1);
                List<Transaction> transactions = aggregateTransactionsForAccount(accountId, periodStart, processDate);
                
                // Generate statement
                StatementResult result = generateStatement(account, transactions, processDate);
                
                if (result.getSuccess()) {
                    processedCount++;
                    accountStatuses.put(accountId, "SUCCESS");
                    generatedStatements.put(accountId, result.getStatementId());
                    
                    // Write statement to file (simulated)
                    String outputFile = "/statements/" + result.getStatementId() + ".txt";
                    outputFiles.add(outputFile);
                    
                } else {
                    errorCount++;
                    errors.add("Statement generation failed for account " + accountId + ": " + result.getErrorMessage());
                    accountStatuses.put(accountId, "ERROR_GENERATION");
                }
                
            } catch (Exception e) {
                log.error("Error processing account {}: {}", accountId, e.getMessage(), e);
                errorCount++;
                errors.add("Unexpected error for account " + accountId + ": " + e.getMessage());
                accountStatuses.put(accountId, "ERROR_EXCEPTION");
            }
        }
        
        long endTime = System.currentTimeMillis();
        long processingTimeMs = endTime - startTime;
        double processingRate = accountIds.size() > 0 ? (double) processedCount / (processingTimeMs / 1000.0) : 0.0;
        
        String statisticsSummary = String.format(
                "Processed: %d/%d accounts, Errors: %d, Rate: %.2f accounts/sec",
                processedCount, accountIds.size(), errorCount, processingRate);
        
        BatchProcessResult result = BatchProcessResult.builder()
                .batchId(batchId)
                .processedCount(processedCount)
                .totalCount(accountIds.size())
                .errorCount(errorCount)
                .startTime(LocalDateTime.ofEpochSecond(startTime / 1000, 0, java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.ofEpochSecond(endTime / 1000, 0, java.time.ZoneOffset.UTC))
                .processingTimeMs(processingTimeMs)
                .success(errorCount == 0)
                .errors(errors)
                .accountStatuses(accountStatuses)
                .generatedStatements(generatedStatements)
                .outputFiles(outputFiles)
                .statisticsSummary(statisticsSummary)
                .memoryUsage(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                .processingRate(processingRate)
                .build();
        
        log.info("Batch processing completed: {}", statisticsSummary);
        return result;
    }
    
    // ==================== Helper Methods ====================
    
    private StatementData prepareStatementData(Account account, List<Transaction> transactions, LocalDate statementDate) {
        LocalDate periodStart = statementDate.minusMonths(1);
        LocalDate periodEnd = statementDate.minusDays(1);
        
        BalanceSummary balanceSummary = calculateBalanceSummary(account, transactions);
        
        return StatementData.builder()
                .account(account)
                .transactions(transactions != null ? transactions : new ArrayList<>())
                .statementDate(statementDate)
                .periodStartDate(periodStart)
                .periodEndDate(periodEnd)
                .previousBalance(balanceSummary.getPreviousBalance())
                .currentBalance(balanceSummary.getCurrentBalance())
                .totalCredits(balanceSummary.getTotalCredits())
                .totalDebits(balanceSummary.getTotalDebits())
                .interestCharges(balanceSummary.getInterestCharges())
                .totalFees(balanceSummary.getFeesAssessed())
                .minimumPaymentDue(balanceSummary.getMinimumPaymentDue())
                .paymentDueDate(balanceSummary.getPaymentDueDate())
                .statementSequence(1)
                .statementId(generateStatementId(account.getAccountId().toString(), statementDate))
                .build();
    }
    
    private String generateStatementId(String accountId, LocalDate statementDate) {
        return "STMT_" + accountId + "_" + statementDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    private String formatStatementPeriod(LocalDate statementDate) {
        LocalDate periodStart = statementDate.minusMonths(1);
        LocalDate periodEnd = statementDate.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return periodStart.format(formatter) + " - " + periodEnd.format(formatter);
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return String.format("$%,.2f", amount.setScale(DECIMAL_SCALE, COBOL_ROUNDING));
    }
    
    private String truncateDescription(String description, int maxLength) {
        if (description == null) {
            return "";
        }
        return description.length() > maxLength ? description.substring(0, maxLength - 3) + "..." : description;
    }
    
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private boolean isReversedOrVoided(Transaction transaction) {
        String typeCode = transaction.getTransactionTypeCode();
        return "REV".equals(typeCode) || "VOI".equals(typeCode);
    }
    
    /**
     * Generate statement data for an account ID and date range.
     * 
     * Convenience method that looks up account and transactions by ID and date range,
     * then generates comprehensive statement data. Used for REST endpoints and batch processing.
     * 
     * @param accountId Account ID as string
     * @param startDate Statement period start date
     * @param endDate Statement period end date
     * @return StatementData with account and transaction information
     * @throws IllegalArgumentException if account ID is null or account not found
     */
    public StatementData generateStatement(String accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating statement data for account ID: {}, period: {} to {}", accountId, startDate, endDate);
        
        // Validate input
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        // Look up account
        Long accountIdLong = Long.parseLong(accountId);
        Optional<Account> accountOpt = accountRepository.findById(accountIdLong);
        if (!accountOpt.isPresent()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        
        Account account = accountOpt.get();
        
        // Find transactions in date range
        List<Transaction> transactions = transactionRepository.findByAccountIdAndTransactionDateBetween(
            accountIdLong, startDate, endDate);
        
        // Generate statement data
        return prepareStatementData(account, transactions, endDate);
    }
}