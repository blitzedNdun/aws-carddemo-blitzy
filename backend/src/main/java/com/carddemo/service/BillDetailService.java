/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.BillDetailResponse;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.ValidationUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service class for handling bill detail inquiries and calculations.
 * 
 * This service implements the business logic from COBOL COBIL00C.cbl program, providing
 * bill detail retrieval, interest charge calculations, minimum payment calculations,
 * payment history retrieval, and due date determination with full COBOL functional parity.
 * 
 * Key Features:
 * - Bill itemization display with transaction categorization
 * - Interest charge calculations with COBOL COMP-3 precision
 * - Payment history retrieval and display formatting
 * - Minimum payment calculation using business rules
 * - Due date determination based on billing cycles
 * - Charge breakdown and categorization logic
 * 
 * COBOL Program Mapping:
 * - Original COBOL: app/cbl/COBIL00C.cbl
 * - Copybook: CVTRA05Y.cpy (Transaction structure)
 * - Copybook: CVACT01Y.cpy (Account structure)
 * 
 * Business Rules:
 * - Minimum payment: Greater of 2% of balance or $10.00 floor
 * - Interest rate: 1.25% monthly on unpaid balance
 * - Due date: 25 days from statement date, adjusted for weekends
 * - Payment history: Last 12 months of payment transactions
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
@Transactional(readOnly = true)
public class BillDetailService {

    // Business rule constants matching COBOL program
    private static final BigDecimal MINIMUM_PAYMENT_RATE = BigDecimal.valueOf(0.02); // 2% of balance
    private static final BigDecimal MINIMUM_PAYMENT_FLOOR = new BigDecimal("10.00"); // $10.00 minimum with scale 2
    private static final BigDecimal INTEREST_RATE_MONTHLY = BigDecimal.valueOf(0.0125); // 1.25% monthly
    private static final int PAYMENT_DUE_DAYS = 25; // Payment due in 25 days from statement
    private static final int COBOL_DECIMAL_SCALE = 2; // COMP-3 decimal scale
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP; // COBOL ROUNDED clause

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Retrieves complete bill detail information for an account.
     * 
     * COBOL Paragraph Mapping: 0000-MAIN-PROCESSING
     * 
     * This method orchestrates the complete bill detail retrieval process,
     * including account validation, balance calculations, interest charges,
     * minimum payment calculation, and payment history retrieval.
     * 
     * @param accountId the account ID for bill detail retrieval
     * @return BillDetailResponse containing complete billing information
     * @throws IllegalArgumentException if accountId is null or invalid
     */
    public BillDetailResponse getBillDetail(Long accountId) {
        // Validate account ID parameter
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        if (accountId <= 0) {
            throw new IllegalArgumentException("Invalid account ID");
        }

        // Retrieve account information
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            throw new IllegalArgumentException("Account not found");
        }

        Account account = accountOpt.get();

        // Calculate interest charges
        BigDecimal interestCharges = calculateInterestCharges(account.getCurrentBalance());

        // Calculate minimum payment
        BigDecimal minimumPayment = calculateMinimumPayment(account.getCurrentBalance());

        // Calculate due date
        LocalDate statementDate = LocalDate.now();
        LocalDate dueDate = getDueDate(statementDate);

        // Get payment history
        List<Transaction> paymentHistory = getPaymentHistory(accountId);

        // Build response
        BillDetailResponse response = new BillDetailResponse();
        response.setAccountId(String.valueOf(accountId));
        response.setCurrentBalance(account.getCurrentBalance());
        response.setMinimumPayment(minimumPayment);
        response.setPaymentDueDate(dueDate);
        response.setInterestCharges(interestCharges);
        response.setStatementDate(statementDate);
        response.setPaymentHistory(paymentHistory);

        return response;
    }

    /**
     * Calculates interest charges based on account balance.
     * 
     * COBOL Paragraph Mapping: 2000-CALCULATE-INTEREST
     * 
     * Applies the monthly interest rate to the current balance using
     * BigDecimal arithmetic to maintain COBOL COMP-3 precision.
     * 
     * @param accountBalance the current account balance
     * @return calculated interest charges with 2 decimal precision
     */
    public BigDecimal calculateInterestCharges(BigDecimal accountBalance) {
        if (accountBalance == null) {
            return BigDecimal.ZERO.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        }

        // Return zero interest for zero or negative balance
        if (accountBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        }

        // Calculate interest: balance * monthly rate
        BigDecimal interest = accountBalance.multiply(INTEREST_RATE_MONTHLY)
                .setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

        return interest;
    }

    /**
     * Retrieves payment history for the specified account.
     * 
     * COBOL Paragraph Mapping: 1000-GET-PAYMENT-HISTORY
     * 
     * Retrieves payment transactions (credits) for the account within
     * the last 12 months, ordered by transaction date descending.
     * 
     * @param accountId the account ID for payment history retrieval
     * @return list of payment transactions with proper date formatting
     */
    public List<Transaction> getPaymentHistory(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        LocalDate startDate = LocalDate.now().minusMonths(12);
        LocalDate endDate = LocalDate.now();

        // Retrieve payment transactions (credits to account)
        // Using specific transaction type query to find payment transactions
        return transactionRepository.findByAccountIdAndTransactionTypeAndDateRange(
            accountId, "Payment", startDate, endDate
        );
    }

    /**
     * Calculates minimum payment amount based on business rules.
     * 
     * COBOL Paragraph Mapping: 3000-CALCULATE-MIN-PAYMENT
     * 
     * Uses the greater of 2% of current balance or $10.00 minimum floor,
     * matching COBOL minimum payment calculation logic.
     * 
     * @param currentBalance the current account balance
     * @return calculated minimum payment with proper precision
     */
    public BigDecimal calculateMinimumPayment(BigDecimal currentBalance) {
        if (currentBalance == null) {
            return MINIMUM_PAYMENT_FLOOR.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        }

        // Return zero for negative balance
        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
        }

        // Calculate 2% of current balance
        BigDecimal calculatedMinimum = currentBalance.multiply(MINIMUM_PAYMENT_RATE)
                .setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

        // Return the greater of calculated amount or minimum floor
        return calculatedMinimum.compareTo(MINIMUM_PAYMENT_FLOOR) > 0 
               ? calculatedMinimum 
               : MINIMUM_PAYMENT_FLOOR.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);
    }

    /**
     * Calculates payment due date based on statement date.
     * 
     * COBOL Paragraph Mapping: 4000-CALCULATE-DUE-DATE
     * 
     * Calculates due date as 25 days from statement date, with
     * weekend adjustment logic to ensure due date falls on business day.
     * 
     * @param statementDate the statement generation date
     * @return payment due date adjusted for weekends
     */
    public LocalDate getDueDate(LocalDate statementDate) {
        if (statementDate == null) {
            throw new IllegalArgumentException("Statement date cannot be null");
        }

        // Calculate due date as 25 days from statement date
        LocalDate dueDate = statementDate.plusDays(PAYMENT_DUE_DAYS);

        // Adjust for weekends - move to next business day
        while (dueDate.getDayOfWeek() == DayOfWeek.SATURDAY || 
               dueDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dueDate = dueDate.plusDays(1);
        }

        return dueDate;
    }

    /**
     * Itemizes charges and returns transaction breakdown.
     * 
     * COBOL Paragraph Mapping: 5000-ITEMIZE-CHARGES
     * 
     * Retrieves and categorizes all transactions for the account
     * within the current billing cycle for charge itemization display.
     * 
     * @param accountId the account ID for charge itemization
     * @return list of transactions categorized by type and amount
     */
    public List<Transaction> itemizeCharges(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        LocalDate startDate = LocalDate.now().withDayOfMonth(1); // First day of current month
        LocalDate endDate = LocalDate.now();

        // Retrieve all transactions for current billing period
        return transactionRepository.findByAccountIdAndTransactionDateBetween(
            accountId, startDate, endDate
        );
    }
}