/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Statement;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Account;
import com.carddemo.repository.StatementRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.AccountRepository;
import com.carddemo.exception.BusinessRuleException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Spring Boot service class implementing statement generation business logic
 * translated from CBSTM03A and CBSTM03B COBOL programs. Provides monthly statement
 * creation, transaction aggregation, balance calculations, and statement archival
 * functionality while maintaining COBOL precision and business rules.
 * 
 * This service translates the following COBOL programs:
 * - CBSTM03A: Monthly statement generation logic
 * - CBSTM03B: Statement transaction aggregation processing
 * - Statement date calculations and billing cycle management
 * - Minimum payment calculations per original business rules
 * - File output generation with identical record layouts
 * 
 * Key COBOL-to-Java translations:
 * 1. Statement cycle calculations preserve original date logic
 * 2. Transaction aggregation maintains COMP-3 decimal precision  
 * 3. Balance calculations use BigDecimal with COBOL rounding
 * 4. Minimum payment logic follows original percentage rules
 * 5. File output preserves COBOL record formatting
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
@Transactional
public class StatementGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(StatementGenerationService.class);

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired 
    private AccountRepository accountRepository;

    @Autowired
    private FileWriterService fileWriterService;

    // COBOL business rule constants
    private static final BigDecimal MINIMUM_PAYMENT_PERCENTAGE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal MINIMUM_PAYMENT_FLOOR = new BigDecimal("25.00");
    private static final int PAYMENT_DUE_DAYS = 25;

    /**
     * Generate monthly statements for all active accounts
     * Replicates CBSTM03A COBOL program main processing logic
     * 
     * @return list of generated statements
     */
    public List<Statement> generateMonthlyStatements() {
        logger.info("Starting monthly statement generation process");
        
        List<Statement> generatedStatements = new ArrayList<>();
        List<Account> allAccounts = accountRepository.findAll();
        
        LocalDate statementDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        LocalDate cycleStartDate = statementDate.with(TemporalAdjusters.firstDayOfMonth());
        
        for (Account account : allAccounts) {
            try {
                // Check if statement already exists for this account and date
                if (!statementRepository.existsByAccountIdAndStatementDate(
                        account.getAccountId(), statementDate)) {
                    
                    Statement statement = generateStatementForAccount(
                        account, cycleStartDate, statementDate);
                    
                    if (statement != null) {
                        generatedStatements.add(statement);
                        logger.debug("Generated statement for account: {}", account.getAccountId());
                    }
                }
                
            } catch (Exception e) {
                logger.error("Failed to generate statement for account {}: {}", 
                           account.getAccountId(), e.getMessage());
                // Continue processing other accounts
            }
        }
        
        logger.info("Completed statement generation. Generated {} statements", 
                   generatedStatements.size());
        return generatedStatements;
    }

    /**
     * Generate statement for a specific account
     * Implements COBOL statement creation logic
     */
    private Statement generateStatementForAccount(Account account, 
                                                 LocalDate cycleStartDate, 
                                                 LocalDate statementDate) {
        
        Long accountId = account.getAccountId();
        
        // Get transactions for billing cycle
        List<Transaction> cycleTransactions = aggregateTransactionsByCycle(
            accountId, cycleStartDate, statementDate);
        
        // Create statement entity
        Statement statement = Statement.builder()
            .accountId(accountId)
            .statementDate(statementDate)
            .cycleStartDate(cycleStartDate)
            .cycleEndDate(statementDate)
            .currentBalance(account.getCurrentBalance())
            .previousBalance(account.getCurrentBalance()) // Simplified for demo
            .creditLimit(account.getCreditLimit())
            .statementStatus("G") // Generated
            .build();
        
        // Calculate statement balances and totals
        statement = calculateStatementBalances(statement, cycleTransactions);
        
        // Calculate minimum payment
        BigDecimal minimumPayment = calculateMinimumPayment(statement.getCurrentBalance());
        statement.setMinimumPaymentAmount(minimumPayment);
        
        // Set payment due date
        statement.setPaymentDueDate(statementDate.plusDays(PAYMENT_DUE_DAYS));
        
        // Save statement
        Statement savedStatement = statementRepository.save(statement);
        
        return savedStatement;
    }

    /**
     * Aggregate transactions by billing cycle
     * Replicates CBSTM03B COBOL transaction processing logic
     * 
     * @param accountId account identifier
     * @param cycleStartDate billing cycle start date
     * @param cycleEndDate billing cycle end date
     * @return list of transactions for the cycle
     */
    public List<Transaction> aggregateTransactionsByCycle(Long accountId, 
                                                         LocalDate cycleStartDate, 
                                                         LocalDate cycleEndDate) {
        
        logger.debug("Aggregating transactions for account {} from {} to {}", 
                    accountId, cycleStartDate, cycleEndDate);
        
        List<Transaction> transactions = transactionRepository
            .findByAccountIdAndTransactionDateBetween(accountId, cycleStartDate, cycleEndDate);
        
        logger.debug("Found {} transactions for account {} in cycle", 
                    transactions.size(), accountId);
        
        return transactions != null ? transactions : new ArrayList<>();
    }

    /**
     * Calculate statement balances and totals
     * Implements COBOL balance calculation logic with COMP-3 precision
     * 
     * @param statement statement to calculate balances for
     * @param transactions list of transactions in cycle
     * @return updated statement with calculated balances
     */
    public Statement calculateStatementBalances(Statement statement, List<Transaction> transactions) {
        logger.debug("Calculating statement balances for account: {}", statement.getAccountId());
        
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal interestCharges = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        
        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                // Credit transaction (negative amount)
                totalCredits = totalCredits.add(amount.abs());
            } else {
                // Debit transaction (positive amount)
                totalDebits = totalDebits.add(amount);
                
                // Categorize as interest or fees based on transaction type
                // Simplified logic - in production would check transaction categories
                if (transaction.getDescription() != null && 
                    transaction.getDescription().toLowerCase().contains("interest")) {
                    interestCharges = interestCharges.add(amount);
                } else if (transaction.getDescription() != null && 
                          transaction.getDescription().toLowerCase().contains("fee")) {
                    fees = fees.add(amount);
                }
            }
        }
        
        // Set calculated totals with proper precision
        statement.setTotalCredits(totalCredits.setScale(2, RoundingMode.HALF_UP));
        statement.setTotalDebits(totalDebits.setScale(2, RoundingMode.HALF_UP));
        statement.setInterestCharges(interestCharges.setScale(2, RoundingMode.HALF_UP));
        statement.setFees(fees.setScale(2, RoundingMode.HALF_UP));
        
        // Calculate available credit
        if (statement.getCreditLimit() != null) {
            BigDecimal availableCredit = statement.getCreditLimit()
                .subtract(statement.getCurrentBalance());
            statement.setAvailableCredit(availableCredit.setScale(2, RoundingMode.HALF_UP));
        }
        
        return statement;
    }

    /**
     * Calculate minimum payment amount per COBOL business rules
     * Maintains original percentage and floor amount logic
     * 
     * @param currentBalance current account balance
     * @return minimum payment amount
     */
    public BigDecimal calculateMinimumPayment(BigDecimal currentBalance) {
        logger.debug("Calculating minimum payment for balance: {}", currentBalance);
        
        if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate percentage-based minimum payment
        BigDecimal percentagePayment = currentBalance
            .multiply(MINIMUM_PAYMENT_PERCENTAGE)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Apply minimum floor amount
        BigDecimal minimumPayment = percentagePayment.max(MINIMUM_PAYMENT_FLOOR);
        
        // Cap at current balance if balance is less than minimum
        if (minimumPayment.compareTo(currentBalance) > 0) {
            minimumPayment = currentBalance;
        }
        
        logger.debug("Calculated minimum payment: {}", minimumPayment);
        return minimumPayment;
    }

    /**
     * Format statement output for file generation
     * Preserves COBOL record layout formatting
     * 
     * @param statement statement to format
     * @param transactions associated transactions
     * @return formatted statement string
     */
    public String formatStatementOutput(Statement statement, List<Transaction> transactions) {
        logger.debug("Formatting statement output for account: {}", statement.getAccountId());
        
        StringBuilder output = new StringBuilder();
        
        // Statement header information
        output.append("STATEMENT FOR ACCOUNT: ").append(statement.getAccountId()).append("\n");
        output.append("STATEMENT DATE: ").append(statement.getStatementDate()).append("\n");
        output.append("CURRENT BALANCE: $").append(statement.getCurrentBalance()).append("\n");
        
        if (statement.getMinimumPaymentAmount() != null && 
            statement.getMinimumPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            output.append("MINIMUM PAYMENT: $").append(statement.getMinimumPaymentAmount()).append("\n");
        }
        
        // Transaction details
        if (transactions != null && !transactions.isEmpty()) {
            output.append("\nTRANSACTIONS:\n");
            for (Transaction transaction : transactions) {
                output.append("  ").append(transaction.getTransactionDate())
                      .append(" - ").append(transaction.getDescription())
                      .append(" - $").append(transaction.getAmount()).append("\n");
            }
        }
        
        return output.toString();
    }

    /**
     * Archive statement to repository
     * Implements COBOL statement archival logic
     * 
     * @param statement statement to archive
     * @return archived statement
     */
    public Statement archiveStatement(Statement statement) {
        logger.info("Archiving statement for account: {}", statement.getAccountId());
        
        statement.setStatementStatus("A"); // Archived
        Statement archivedStatement = statementRepository.save(statement);
        
        logger.debug("Successfully archived statement ID: {}", archivedStatement.getStatementId());
        return archivedStatement;
    }

    /**
     * Validate statement data for business rule compliance
     * Implements COBOL data validation logic
     * 
     * @param statement statement to validate
     * @return true if valid, false otherwise
     */
    public boolean validateStatementData(Statement statement) {
        logger.debug("Validating statement data for account: {}", statement.getAccountId());
        
        // Check required fields
        if (statement.getAccountId() == null) {
            logger.warn("Statement validation failed: missing account ID");
            return false;
        }
        
        if (statement.getStatementDate() == null) {
            logger.warn("Statement validation failed: missing statement date");
            return false;
        }
        
        if (statement.getCurrentBalance() == null) {
            logger.warn("Statement validation failed: missing current balance");
            return false;
        }
        
        // Validate balance precision (max 2 decimal places)
        if (statement.getCurrentBalance().scale() > 2) {
            logger.warn("Statement validation failed: balance precision exceeds 2 decimal places");
            return false;
        }
        
        logger.debug("Statement data validation passed for account: {}", statement.getAccountId());
        return true;
    }
}