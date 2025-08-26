/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating COBOL-compliant test data for bill payment testing scenarios.
 * This class provides methods to create test data that maintains compatibility with legacy
 * COBOL data structures while supporting modern Java testing frameworks.
 * 
 * <p>The TestDataGenerator focuses on creating realistic test data that matches the precision
 * and format requirements of the original mainframe system, particularly for financial
 * calculations that must maintain COMP-3 packed decimal precision.</p>
 * 
 * <p>Key Features:
 * <ul>
 * <li>COBOL COMP-3 precision-compliant BigDecimal generation</li>
 * <li>Account and transaction data generation with realistic relationships</li>
 * <li>Bill payment scenario data creation for comprehensive testing</li>
 * <li>Date and timestamp generation matching COBOL date formats</li>
 * <li>Support for positive and negative test scenarios</li>
 * </ul>
 * 
 * <p>Usage in Testing:
 * <ul>
 * <li>Unit test data generation for BillPaymentService validation</li>
 * <li>Integration test scenario creation for payment processing</li>
 * <li>Performance test data generation for load testing scenarios</li>
 * <li>Edge case and boundary condition testing data</li>
 * <li>Regression test data for COBOL-Java equivalence validation</li>
 * </ul>
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Component
@Slf4j
public class TestDataGenerator {

    private static final Random random = new Random();
    
    /**
     * Reset the random seed for predictable test data generation.
     */
    public void resetRandomSeed() {
        random.setSeed(12345L);
    }
    
    // COBOL COMP-3 precision constants matching original system
    private static final int ACCOUNT_ID_LENGTH = 11;
    private static final int TRANSACTION_ID_LENGTH = 16;
    private static final int BALANCE_PRECISION = 7;
    private static final int BALANCE_SCALE = 2;
    private static final int AMOUNT_PRECISION = 9;
    private static final int AMOUNT_SCALE = 2;
    
    // Test data ranges for realistic financial amounts
    private static final BigDecimal MIN_BALANCE = new BigDecimal("0.00");
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999.99");
    private static final BigDecimal MIN_PAYMENT_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("9999.99");
    private static final BigDecimal MIN_CREDIT_LIMIT = new BigDecimal("500.00");
    private static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("50000.00");
    
    // Common transaction types for bill payment testing
    private static final String[] TRANSACTION_TYPES = {"BP", "PU", "CA", "CR", "FE", "IN"};
    private static final String[] CATEGORY_CODES = {"BILLPAY", "PURCHASE", "CASHADV", "CREDIT", "FEE", "INTEREST"};
    private static final String[] ACCOUNT_STATUSES = {"A", "C", "S"};
    
    /**
     * Generates a realistic Account entity for testing bill payment scenarios.
     * Creates accounts with COBOL-compatible field values and relationships.
     * 
     * @return a fully populated Account entity suitable for testing
     */
    public Account generateAccount() {
        Long accountId = generateAccountId();
        BigDecimal currentBalance = generateBalance();
        BigDecimal creditLimit = generateCreditLimit();
        
        return Account.builder()
            .accountId(accountId)
            .customer(generateCustomerForAccount())
            .activeStatus("Y")
            .currentBalance(currentBalance)
            .creditLimit(creditLimit)
            .cashCreditLimit(creditLimit.multiply(new BigDecimal("0.5")))
            .openDate(LocalDate.now().minusDays(random.nextInt(3650) + 365)) // 1-10 years ago
            .expirationDate(LocalDate.now().plusDays(random.nextInt(1825) + 365)) // 1-5 years from now
            .reissueDate(LocalDate.now().minusDays(random.nextInt(365) + 30)) // Last 1-2 years
            .currentCycleCredit(generateCurrentCycleCredit())
            .currentCycleDebit(generateCurrentCycleDebit())
            .addressZip(generateZipCode())
            .groupId(generateGroupId())
            .build();
    }

    /**
     * Generates a realistic Transaction entity for testing bill payment operations.
     * Creates transactions with proper relationships to accounts and realistic amounts.
     * 
     * @return a fully populated Transaction entity suitable for testing
     */
    public Transaction generateTransaction() {
        Long accountId = generateAccountId();
        BigDecimal amount = generateValidTransactionAmount();
        String typeCode = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];
        
        return Transaction.builder()
            .transactionId(generateTransactionId())
            .accountId(accountId)
            .transactionDate(LocalDate.now().minusDays(random.nextInt(30)))
            .amount(amount)
            .transactionTypeCode(typeCode)
            .categoryCode(CATEGORY_CODES[random.nextInt(CATEGORY_CODES.length)])
            .description(generateTransactionDescription(typeCode))
            .merchantName(generateMerchantName())
            .processedTimestamp(LocalDateTime.now().minusDays(random.nextInt(30)))
            .build();
    }

    /**
     * Generates a BigDecimal value with COBOL COMP-3 precision compliance.
     * Ensures proper scale and rounding to match mainframe decimal handling.
     * 
     * @param precision total number of digits
     * @param scale number of decimal places
     * @return BigDecimal with COBOL COMP-3 compatible precision
     */
    public BigDecimal generateComp3BigDecimal(int precision, int scale) {
        // Generate a random value within the precision limits
        int maxIntegerDigits = precision - scale;
        long maxValue = (long) Math.pow(10, maxIntegerDigits) - 1;
        long integerPart = ThreadLocalRandom.current().nextLong(0, maxValue + 1);
        
        // Generate fractional part
        int maxFractionalValue = (int) Math.pow(10, scale) - 1;
        int fractionalPart = ThreadLocalRandom.current().nextInt(0, maxFractionalValue + 1);
        
        // Combine integer and fractional parts
        String valueString = String.format("%d.%0" + scale + "d", integerPart, fractionalPart);
        
        return new BigDecimal(valueString).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Generates a valid account ID matching COBOL account ID format.
     * Creates 11-digit account numbers with proper check digit validation.
     * 
     * @return a valid account ID as Long
     */
    public Long generateAccountId() {
        // Generate 10-digit base account number
        long baseNumber = ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L);
        
        // Calculate check digit (simple modulo 10 algorithm)
        int checkDigit = calculateCheckDigit(baseNumber);
        
        // Combine base number with check digit
        return baseNumber * 10 + checkDigit;
    }

    /**
     * Generates a valid transaction amount for bill payment testing.
     * Ensures amounts are within realistic ranges and maintain COMP-3 precision.
     * 
     * @return a valid transaction amount as BigDecimal
     */
    public BigDecimal generateValidTransactionAmount() {
        return generateComp3BigDecimal(AMOUNT_PRECISION, AMOUNT_SCALE)
            .max(MIN_PAYMENT_AMOUNT)
            .min(MAX_PAYMENT_AMOUNT);
    }

    /**
     * Generates a realistic account balance with COBOL precision.
     * Creates balances that are appropriate for credit card accounts.
     * 
     * @return account balance as BigDecimal with COMP-3 precision
     */
    public BigDecimal generateBalance() {
        return generateComp3BigDecimal(BALANCE_PRECISION, BALANCE_SCALE)
            .max(MIN_BALANCE)
            .min(MAX_BALANCE);
    }

    /**
     * Generates a bill payment transaction for specific testing scenarios.
     * 
     * @param accountId the account ID for the payment
     * @param amount the payment amount
     * @return a Transaction entity representing a bill payment
     */
    public Transaction generateBillPaymentTransaction(Long accountId, BigDecimal amount) {
        return Transaction.builder()
            .transactionId(generateTransactionId())
            .accountId(accountId)
            .cardNumber(generateCardNumber(accountId))
            .transactionDate(LocalDate.now())
            .amount(amount.negate()) // Bill payments are negative amounts (credits)
            .transactionTypeCode("BP")
            .categoryCode("BILLPAY")
            .description("Bill Payment - Online")
            .merchantName("CARDDEMO BILL PAYMENT")
            .originalTimestamp(LocalDateTime.now())
            .processedTimestamp(LocalDateTime.now())

            .authorizationCode(generateAuthorizationCode())


            .build();
    }

    /**
     * Generates an account with insufficient funds for negative testing scenarios.
     * 
     * @param attemptedPayment the payment amount that should exceed available funds
     * @return an Account with insufficient available credit
     */
    public Account generateInsufficientFundsAccount(BigDecimal attemptedPayment) {
        BigDecimal lowCreditLimit = attemptedPayment.subtract(new BigDecimal("100.00"));
        BigDecimal highBalance = lowCreditLimit.subtract(new BigDecimal("50.00"));
        
        Long accountId = generateAccountId();
        return Account.builder()
            .accountId(accountId)
            .customer(generateCustomerForAccount())
            .activeStatus("Y")
            .creditLimit(lowCreditLimit)
            .currentBalance(highBalance)
            .cashCreditLimit(lowCreditLimit.multiply(new BigDecimal("0.5")))
            .openDate(LocalDate.now().minusDays(random.nextInt(3650) + 365))
            .expirationDate(LocalDate.now().plusDays(random.nextInt(1825) + 365))
            .reissueDate(LocalDate.now().minusDays(random.nextInt(365) + 30))
            .currentCycleCredit(generateCurrentCycleCredit())
            .currentCycleDebit(generateCurrentCycleDebit())
            .addressZip(generateZipCode())
            .groupId(generateGroupId())
            .build();
    }

    /**
     * Generates test data for edge case scenarios.
     * 
     * @return an Account with edge case values for boundary testing
     */
    public Account generateEdgeCaseAccount() {
        return Account.builder()
            .accountId(generateAccountId())
            .customer(generateCustomerForAccount())
            .activeStatus("Y")
            .currentBalance(new BigDecimal("0.01")) // Minimum balance
            .creditLimit(new BigDecimal("99999.99")) // Maximum credit limit
            .cashCreditLimit(new BigDecimal("49999.99"))
            .openDate(LocalDate.now().minusYears(10))
            .expirationDate(LocalDate.now().plusYears(5))
            .reissueDate(LocalDate.now().minusDays(30))
            .currentCycleCredit(new BigDecimal("0.00"))
            .currentCycleDebit(new BigDecimal("0.01"))
            .addressZip("12345")
            .groupId("EDGE")
            .build();
    }

    // Private utility methods

    private Long generateTransactionId() {
        return ThreadLocalRandom.current().nextLong(1000000000000000L, 9999999999999999L);
    }

    private Long generateCustomerId() {
        return ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
    }

    private String generateCardNumber(Long accountId) {
        // Generate a 16-digit card number based on account ID
        String accountStr = String.valueOf(accountId);
        return "4532" + accountStr.substring(0, Math.min(8, accountStr.length())) 
               + String.format("%04d", random.nextInt(10000));
    }

    private BigDecimal generateCreditLimit() {
        return generateComp3BigDecimal(7, 2)
            .max(MIN_CREDIT_LIMIT)
            .min(MAX_CREDIT_LIMIT);
    }

    private BigDecimal generateMinimumPayment(BigDecimal balance) {
        // Minimum payment is typically 2% of balance or $25, whichever is greater
        BigDecimal percentagePayment = balance.multiply(new BigDecimal("0.02"));
        BigDecimal minimumPayment = new BigDecimal("25.00");
        return percentagePayment.max(minimumPayment).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal generateInterestRate() {
        // Generate interest rate between 15.99% and 29.99%
        double rate = 15.99 + (29.99 - 15.99) * random.nextDouble();
        return new BigDecimal(String.format("%.2f", rate));
    }

    private String generateTransactionDescription(String typeCode) {
        return switch (typeCode) {
            case "BP" -> "Bill Payment - Online";
            case "PU" -> "Purchase - " + generateMerchantName();
            case "CA" -> "Cash Advance - ATM";
            case "CR" -> "Credit Adjustment";
            case "FE" -> "Service Fee";
            case "IN" -> "Interest Charge";
            default -> "Transaction";
        };
    }

    private String generateMerchantName() {
        String[] merchants = {
            "ACME GROCERY", "FUEL STOP 123", "ONLINE RETAILER", "RESTAURANT ABC",
            "PHARMACY PLUS", "DEPARTMENT STORE", "GAS STATION", "COFFEE SHOP",
            "ELECTRONICS STORE", "CLOTHING OUTLET"
        };
        return merchants[random.nextInt(merchants.length)];
    }


    private String generateAuthorizationCode() {
        // Generate 6-character authorization code (alphanumeric)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private int calculateCheckDigit(long number) {
        int sum = 0;
        String numberStr = String.valueOf(number);
        for (int i = 0; i < numberStr.length(); i++) {
            int digit = Character.getNumericValue(numberStr.charAt(i));
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Generates a Customer entity for account relationships.
     * 
     * @return a fully populated Customer entity
     */
    private Customer generateCustomerForAccount() {
        Long customerId = generateCustomerId();
        return Customer.builder()
            .customerId(customerId)
            .firstName("John")
            .lastName("Doe")
            .ssn("123456789")
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .phoneNumber1("555-1234")
            .addressLine1("123 Main St")
            .addressLine2("Anytown")
            .stateCode("NY")
            .zipCode("12345")
            .ficoScore(new BigDecimal("750"))
            .build();
    }

    /**
     * Generates current cycle credit amount.
     * 
     * @return BigDecimal representing current cycle credit
     */
    private BigDecimal generateCurrentCycleCredit() {
        return new BigDecimal(random.nextInt(500000) + 10000).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Generates current cycle debit amount.
     * 
     * @return BigDecimal representing current cycle debit
     */
    private BigDecimal generateCurrentCycleDebit() {
        return new BigDecimal(random.nextInt(300000) + 5000).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Generates a random ZIP code.
     * 
     * @return String representing ZIP code
     */
    private String generateZipCode() {
        return String.format("%05d", random.nextInt(99999));
    }

    /**
     * Generates a random group ID.
     * 
     * @return String representing group ID
     */
    private String generateGroupId() {
        String[] groups = {"GOLD", "SILVER", "BRONZE", "PLATINUM", "BASIC"};
        return groups[random.nextInt(groups.length)];
    }
}