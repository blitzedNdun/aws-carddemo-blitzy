package com.carddemo.test;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.dto.AddTransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test data generator utility for creating realistic test data that follows COBOL data structure patterns.
 * Ensures tests use valid account numbers, transaction amounts, and date formats matching VSAM record layouts.
 */
@Component
public class TestDataGenerator {

    private static final Random RANDOM = new Random();
    
    // COBOL-equivalent account number patterns
    private static final String[] VALID_ACCOUNT_PREFIXES = {"1000", "2000", "3000", "4000", "5000"};
    
    // COBOL-equivalent card number patterns (16 digits)
    private static final String[] CARD_PREFIXES = {"4532", "5555", "4000", "3782"};

    /**
     * Generates a valid Account entity with COBOL-equivalent data patterns
     */
    public Account generateValidAccount() {
        Account account = new Account();
        account.setAccountId(generateAccountNumber());
        account.setCustomerId(generateCustomerId());
        account.setCurrentBalance(generateBalance());
        account.setCreditLimit(generateCreditLimit());
        account.setActiveStatus("ACTIVE");
        account.setAccountType("CREDIT");
        account.setOpenDate(LocalDateTime.now().minusYears(1));
        return account;
    }

    /**
     * Generates a valid Account with specific balance and credit limit
     */
    public Account generateValidAccount(BigDecimal balance, BigDecimal creditLimit) {
        Account account = generateValidAccount();
        account.setCurrentBalance(balance);
        account.setCreditLimit(creditLimit);
        return account;
    }

    /**
     * Generates realistic transaction data matching VSAM record layouts
     */
    public Transaction generateTransactionData() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setAccountId(generateAccountNumber());
        transaction.setAmount(generateTransactionAmount());
        transaction.setTransactionType("PURCHASE");
        transaction.setCategoryCode("5411"); // Grocery stores
        transaction.setDescription("Test Transaction");
        transaction.setMerchantName("Test Merchant");
        transaction.setMerchantCity("New York");
        transaction.setMerchantZip("10001");
        transaction.setTransactionDate(LocalDateTime.now());
        return transaction;
    }

    /**
     * Generates transaction data with specific amount
     */
    public Transaction generateTransactionData(BigDecimal amount) {
        Transaction transaction = generateTransactionData();
        transaction.setAmount(amount);
        return transaction;
    }

    /**
     * Generates a valid 16-digit card number with check digit
     */
    public String generateCardNumber() {
        String prefix = CARD_PREFIXES[RANDOM.nextInt(CARD_PREFIXES.length)];
        StringBuilder cardNumber = new StringBuilder(prefix);
        
        // Generate 12 more digits
        for (int i = 0; i < 12; i++) {
            cardNumber.append(RANDOM.nextInt(10));
        }
        
        return cardNumber.toString();
    }

    /**
     * Generates transaction amounts following COBOL COMP-3 precision patterns
     */
    public BigDecimal generateTransactionAmount() {
        // Generate amounts between $1.00 and $500.00 with 2 decimal places
        double amount = 1.00 + (RANDOM.nextDouble() * 499.00);
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generates transaction amount within specified range
     */
    public BigDecimal generateTransactionAmount(BigDecimal min, BigDecimal max) {
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        double amount = minVal + (RANDOM.nextDouble() * (maxVal - minVal));
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generates valid date in COBOL-compatible format
     */
    public LocalDateTime generateValidDate() {
        return LocalDateTime.now().minusDays(RANDOM.nextInt(365));
    }

    /**
     * Generates invalid transaction data for negative testing
     */
    public AddTransactionRequest generateInvalidTransactionData() {
        AddTransactionRequest request = new AddTransactionRequest();
        request.setAccountId("INVALID"); // Invalid account ID
        request.setCardNumber("123"); // Invalid card number (too short)
        request.setAmount(new BigDecimal("-10.00")); // Negative amount
        request.setTypeCode("INVALID");
        request.setCategoryCode("");
        request.setDescription("");
        request.setMerchantName("");
        request.setTransactionDate(LocalDateTime.now().plusDays(1)); // Future date
        return request;
    }

    /**
     * Generates a list of test accounts with various scenarios
     */
    public List<Account> generateAccountTestScenarios() {
        List<Account> accounts = new ArrayList<>();
        
        // Normal account with available credit
        accounts.add(generateValidAccount(
            new BigDecimal("500.00"), 
            new BigDecimal("2000.00")
        ));
        
        // Account near credit limit
        accounts.add(generateValidAccount(
            new BigDecimal("1950.00"), 
            new BigDecimal("2000.00")
        ));
        
        // Account at credit limit
        accounts.add(generateValidAccount(
            new BigDecimal("2000.00"), 
            new BigDecimal("2000.00")
        ));
        
        // Zero balance account
        accounts.add(generateValidAccount(
            new BigDecimal("0.00"), 
            new BigDecimal("1000.00")
        ));
        
        return accounts;
    }

    /**
     * Generates a complete AddTransactionRequest for testing
     */
    public AddTransactionRequest generateValidTransactionRequest(String accountId) {
        AddTransactionRequest request = new AddTransactionRequest();
        request.setAccountId(accountId);
        request.setCardNumber(generateCardNumber());
        request.setAmount(generateTransactionAmount());
        request.setTypeCode("PURCHASE");
        request.setCategoryCode("5411");
        request.setDescription("Test Purchase Transaction");
        request.setMerchantName("Test Merchant Store");
        request.setMerchantCity("New York");
        request.setMerchantZip("10001");
        request.setTransactionDate(LocalDateTime.now());
        return request;
    }

    // Private helper methods
    private String generateAccountNumber() {
        String prefix = VALID_ACCOUNT_PREFIXES[RANDOM.nextInt(VALID_ACCOUNT_PREFIXES.length)];
        return prefix + String.format("%06d", RANDOM.nextInt(1000000));
    }

    private String generateCustomerId() {
        return String.format("CUST%08d", RANDOM.nextInt(100000000));
    }

    private BigDecimal generateBalance() {
        double balance = RANDOM.nextDouble() * 2000.00;
        return new BigDecimal(balance).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal generateCreditLimit() {
        double[] limits = {500.00, 1000.00, 2000.00, 5000.00, 10000.00};
        double limit = limits[RANDOM.nextInt(limits.length)];
        return new BigDecimal(limit).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateTransactionId() {
        return String.format("TXN%010d", RANDOM.nextInt(Integer.MAX_VALUE));
    }
}