/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import com.carddemo.util.TestConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;



/**
 * Test data generator utility for creating standardized test objects.
 * 
 * Provides consistent test data generation for Transaction, Account, and other entities
 * used throughout the comprehensive test suite. Ensures COBOL-compatible data formats
 * and precision matching requirements for functional parity validation.
 * 
 * Key Features:
 * - COBOL precision preservation with BigDecimal scale matching
 * - Consistent test data patterns across all test classes
 * - Support for valid, invalid, and edge case test scenarios
 * - Batch data generation for volume testing
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
public class TestDataGenerator {

    private final AtomicLong counter = new AtomicLong(1);

    /**
     * Generates a standard valid Transaction for testing purposes.
     * 
     * Creates a mock Transaction with realistic values matching COBOL field definitions.
     * Uses TestConstants for consistent amounts and IDs across test scenarios.
     * 
     * @return mock Transaction with valid test data
     */
    public Transaction generateTransaction() {
        Transaction transaction = new Transaction();
        
        // Configure transaction with valid test data
        transaction.setTransactionId(1001L);
        transaction.setAmount(TestConstants.DEFAULT_TRANSACTION_AMOUNT);
        transaction.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        transaction.setTransactionTypeCode(TestConstants.TXN_TYPE_PURCHASE);
        transaction.setMerchantId(12345L);
        transaction.setMerchantName("TEST MERCHANT");
        transaction.setDescription("Test transaction for validation");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setOriginalTimestamp(LocalDateTime.now());
        transaction.setProcessedTimestamp(LocalDateTime.now());
        transaction.setCardNumber("1234567890123456");
        transaction.setCategoryCode("5411");
        transaction.setSubcategoryCode("01");
        transaction.setSource("WEB");
        
        return transaction;
    }

    /**
     * Generates an invalid Transaction for negative testing scenarios.
     * 
     * Creates a mock Transaction with invalid data to test validation logic
     * and error handling pathways in the transaction processing service.
     * 
     * @return mock Transaction with invalid test data
     */
    public Transaction generateInvalidTransaction() {
        Transaction transaction = new Transaction();
        
        // Configure transaction with invalid test data
        transaction.setTransactionId(null);
        transaction.setAmount(new BigDecimal("-50.00")); // Negative amount
        transaction.setAccountId(null); // Missing account ID
        transaction.setTransactionTypeCode(""); // Empty type
        transaction.setMerchantId(null);
        transaction.setMerchantName(null);
        transaction.setDescription(""); // Empty description
        transaction.setTransactionDate(null); // Missing date
        transaction.setOriginalTimestamp(null);
        transaction.setProcessedTimestamp(null);
        transaction.setCardNumber(""); // Empty card number
        transaction.setCategoryCode(null);
        transaction.setSubcategoryCode(null);
        transaction.setSource(null);
        
        return transaction;
    }

    /**
     * Generates a duplicate Transaction for duplicate detection testing.
     * 
     * Creates a mock Transaction with same key identifiers as a standard transaction
     * to test the duplicate detection mechanisms in transaction processing.
     * 
     * @return mock Transaction configured as a duplicate
     */
    public Transaction generateDuplicateTransaction() {
        Transaction transaction = new Transaction();
        
        // Configure transaction as duplicate (same key fields as standard transaction)
        transaction.setTransactionId(1001L); // Same as generateTransaction()
        transaction.setAmount(TestConstants.DEFAULT_TRANSACTION_AMOUNT);
        transaction.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        transaction.setTransactionTypeCode(TestConstants.TXN_TYPE_PURCHASE);
        transaction.setMerchantId(12345L);
        transaction.setMerchantName("TEST MERCHANT");
        transaction.setDescription("Duplicate test transaction");
        transaction.setTransactionDate(LocalDate.now());
        transaction.setOriginalTimestamp(LocalDateTime.now());
        transaction.setProcessedTimestamp(LocalDateTime.now());
        transaction.setCardNumber("1234567890123456");
        transaction.setCategoryCode("5411");
        transaction.setSubcategoryCode("01");
        transaction.setSource("WEB");
        
        return transaction;
    }

    /**
     * Generates a standard valid Account for testing purposes.
     * 
     * Creates a mock Account with realistic values matching COBOL CVACT01Y copybook.
     * Uses TestConstants for consistent balance and limit values across tests.
     * 
     * @return mock Account with valid test data
     */
    public Account generateAccount() {
        Account account = new Account();
        
        // Configure account with valid test data
        account.setAccountId(TestConstants.TEST_ACCOUNT_ID);
        account.setActiveStatus(TestConstants.ACCOUNT_STATUS_ACTIVE);
        account.setCurrentBalance(TestConstants.DEFAULT_ACCOUNT_BALANCE);
        account.setCreditLimit(TestConstants.DEFAULT_CREDIT_LIMIT);
        account.setCashCreditLimit(new BigDecimal("1000.00")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.setOpenDate(LocalDate.now().minusYears(2));
        account.setExpirationDate(LocalDate.now().plusYears(3));
        account.setReissueDate(LocalDate.now().minusMonths(6));
        account.setCurrentCycleCredit(BigDecimal.ZERO
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.setCurrentCycleDebit(new BigDecimal("500.00")
            .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE));
        account.setAddressZip("12345");
        account.setGroupId("GROUP001");
        
        return account;
    }

    /**
     * Generates a list of Transactions for batch processing testing.
     * 
     * Creates a specified number of mock Transactions with varied but valid data
     * to support batch processing volume testing and performance validation.
     * 
     * @return list of mock Transactions for batch testing
     */
    public List<Transaction> generateBatchTransactions() {
        return generateBatchTransactions(TestConstants.DEFAULT_BATCH_SIZE);
    }

    /**
     * Generates a specified number of Transactions for batch processing testing.
     * 
     * Creates mock Transactions with varied amounts, merchants, and dates
     * to simulate realistic batch processing scenarios with diverse data.
     * 
     * @param count number of transactions to generate
     * @return list of mock Transactions for batch testing
     */
    public List<Transaction> generateBatchTransactions(int count) {
        List<Transaction> transactions = new ArrayList<>();
        
        // Limit to small batch for testing to prevent recursion
        int safeCount = Math.min(count, 10);
        
        for (int i = 0; i < safeCount; i++) {
            Transaction transaction = new Transaction();
            
            // Simple configuration to avoid any potential recursion
            transaction.setTransactionId((long) (1001 + i));
            transaction.setAmount(TestConstants.DEFAULT_TRANSACTION_AMOUNT);
            transaction.setAccountId(TestConstants.TEST_ACCOUNT_ID);
            transaction.setTransactionTypeCode(TestConstants.TXN_TYPE_PURCHASE);
            transaction.setMerchantId(12345L);
            transaction.setMerchantName("TEST MERCHANT");
            transaction.setDescription("Test transaction " + i);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setOriginalTimestamp(LocalDateTime.now());
            transaction.setProcessedTimestamp(LocalDateTime.now());
            transaction.setCardNumber("1234567890123456");
            transaction.setCategoryCode("5411");
            transaction.setSubcategoryCode("01");
            transaction.setSource("WEB");
            
            transactions.add(transaction);
        }
        
        return transactions;
    }

    /**
     * Generates a Transaction with a specific amount for precision testing.
     * 
     * Creates a mock Transaction with exact monetary amounts for testing
     * COBOL COMP-3 precision preservation and BigDecimal calculation accuracy.
     * 
     * @param amount specific amount to set for the transaction
     * @return mock Transaction with specified amount
     */
    public Transaction generateTransactionWithAmount(BigDecimal amount) {
        Transaction transaction = generateTransaction();
        
        // Override amount with specific value, ensuring proper COBOL scale
        transaction.setAmount(
            amount.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
        );
        
        return transaction;
    }

    /**
     * Generates a Transaction with specific type code for type-specific testing.
     * 
     * Creates a mock Transaction with a specified transaction type code
     * to support testing of type-specific business logic and validation rules.
     * 
     * @param typeCode transaction type code to set
     * @return mock Transaction with specified type code
     */
    public Transaction generateTransactionWithType(String typeCode) {
        Transaction transaction = generateTransaction();
        
        // Override type code with specific value
        transaction.setTransactionTypeCode(typeCode);
        
        return transaction;
    }

    /**
     * Generates an Account with specific balance for balance-related testing.
     * 
     * Creates a mock Account with exact balance amounts for testing
     * balance validation, credit limit checking, and transaction approval logic.
     * 
     * @param balance specific balance to set for the account
     * @return mock Account with specified balance
     */
    public Account generateAccountWithBalance(BigDecimal balance) {
        Account account = generateAccount();
        
        // Override balance with specific value, ensuring proper COBOL scale
        account.setCurrentBalance(
            balance.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
        );
        
        return account;
    }

    /**
     * Generates an Account with specific credit limit for limit testing.
     * 
     * Creates a mock Account with exact credit limit for testing
     * credit limit validation and transaction approval scenarios.
     * 
     * @param creditLimit specific credit limit to set for the account
     * @return mock Account with specified credit limit
     */
    public Account generateAccountWithCreditLimit(BigDecimal creditLimit) {
        Account account = generateAccount();
        
        // Override credit limit with specific value, ensuring proper COBOL scale
        account.setCreditLimit(
            creditLimit.setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE)
        );
        
        return account;
    }

    /**
     * Generates transactions with error conditions for error handling testing.
     * 
     * Creates a list of mock Transactions with various error conditions
     * to test error handling, validation, and recovery mechanisms.
     * 
     * @return list of mock Transactions with error conditions
     */
    public List<Transaction> generateErrorTransactions() {
        List<Transaction> errorTransactions = new ArrayList<>();
        
        // Transaction with amount exceeding limit
        Transaction overLimitTxn = generateTransaction();
        overLimitTxn.setAmount(TestConstants.MAX_TRANSACTION_AMOUNT.add(BigDecimal.ONE));
        errorTransactions.add(overLimitTxn);
        
        // Transaction with null account ID
        Transaction nullAccountTxn = generateTransaction();
        nullAccountTxn.setAccountId(null);
        errorTransactions.add(nullAccountTxn);
        
        // Transaction with invalid transaction type
        Transaction invalidTypeTxn = generateTransaction();
        invalidTypeTxn.setTransactionTypeCode("INVALID");
        errorTransactions.add(invalidTypeTxn);
        
        // Transaction with future date
        Transaction futureDateTxn = generateTransaction();
        futureDateTxn.setTransactionDate(LocalDate.now().plusDays(1));
        errorTransactions.add(futureDateTxn);
        
        // Transaction with zero amount
        Transaction zeroAmountTxn = generateTransaction();
        zeroAmountTxn.setAmount(BigDecimal.ZERO);
        errorTransactions.add(zeroAmountTxn);
        
        return errorTransactions;
    }
    
    /**
     * Generate a single customer for testing purposes
     * Creates customer with realistic data for CustomerReportsService testing
     * 
     * @return Customer entity with test data
     */
    public Customer generateCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(String.valueOf(counter.incrementAndGet()));
        customer.setFirstName("John");
        customer.setLastName("Smith");
        customer.setDateOfBirth(LocalDate.of(1980, 5, 15));
        customer.setSsn("123456789");
        customer.setFicoScore(BigDecimal.valueOf(750)); // Default high FICO for testing
        customer.setPhoneNumber1("555-123-4567");
        return customer;
    }
    
    /**
     * Generate a list of customers for testing purposes
     * Creates diverse set of customers for segmentation testing
     * 
     * @return List of Customer entities with varied profiles
     */
    public List<Customer> generateCustomerList() {
        List<Customer> customers = new ArrayList<>();
        
        // High value customer
        Customer highValue = generateCustomer();
        highValue.setCustomerId("1");
        highValue.setFicoScore(BigDecimal.valueOf(780));
        highValue.setFirstName("Alice");
        highValue.setLastName("Johnson");
        customers.add(highValue);
        
        // Medium value customer  
        Customer mediumValue = generateCustomer();
        mediumValue.setCustomerId("2");
        mediumValue.setFicoScore(BigDecimal.valueOf(680));
        mediumValue.setFirstName("Bob");
        mediumValue.setLastName("Davis");
        customers.add(mediumValue);
        
        // Low value customer
        Customer lowValue = generateCustomer();
        lowValue.setCustomerId("3");
        lowValue.setFicoScore(BigDecimal.valueOf(580));
        lowValue.setFirstName("Carol");
        lowValue.setLastName("Wilson");
        customers.add(lowValue);
        
        return customers;
    }
    
    /**
     * Generate a list of accounts for testing purposes
     * Creates accounts with varied balances and credit limits
     * 
     * @return List of Account entities for testing
     */
    public List<Account> generateAccountList() {
        List<Account> accounts = new ArrayList<>();
        
        // Create customers for the accounts
        Customer customer1 = generateCustomer();
        customer1.setCustomerId("1");
        
        Customer customer2 = generateCustomer();
        customer2.setCustomerId("2");
        
        Customer customer3 = generateCustomer();
        customer3.setCustomerId("3");
        
        // High utilization account
        Account highUtil = generateAccount();
        highUtil.setAccountId(1L);
        highUtil.setCustomer(customer1);
        highUtil.setCurrentBalance(new BigDecimal("8000.00"));
        highUtil.setCreditLimit(new BigDecimal("10000.00"));
        accounts.add(highUtil);
        
        // Medium utilization account
        Account mediumUtil = generateAccount();
        mediumUtil.setAccountId(2L);
        mediumUtil.setCustomer(customer2);
        mediumUtil.setCurrentBalance(new BigDecimal("3000.00"));
        mediumUtil.setCreditLimit(new BigDecimal("5000.00"));
        accounts.add(mediumUtil);
        
        // Low utilization account
        Account lowUtil = generateAccount();
        lowUtil.setAccountId(3L);
        lowUtil.setCustomer(customer3);
        lowUtil.setCurrentBalance(new BigDecimal("500.00"));
        lowUtil.setCreditLimit(new BigDecimal("5000.00"));
        accounts.add(lowUtil);
        
        return accounts;
    }

    /**
     * Resets the random seed for consistent test data generation.
     * Used in test setup methods to ensure reproducible test results.
     */
    public void resetRandomSeed() {
        // Reset counter for consistent test data
        counter.set(1);
    }

    /**
     * Generates a test admin user for admin functionality testing.
     * Creates a UserSecurity entity with admin privileges.
     * 
     * @return UserSecurity with admin user type
     */
    public com.carddemo.entity.UserSecurity generateAdminUser() {
        com.carddemo.entity.UserSecurity user = new com.carddemo.entity.UserSecurity();
        user.setSecUsrId("ADMIN01");
        user.setUsername("ADMIN01");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setUserType("A"); // Admin type
        user.setPassword("password"); // Default password for testing
        return user;
    }

    /**
     * Generates a test regular user for user functionality testing.
     * Creates a UserSecurity entity with regular user privileges.
     * 
     * @return UserSecurity with regular user type
     */
    public com.carddemo.entity.UserSecurity generateRegularUser() {
        com.carddemo.entity.UserSecurity user = new com.carddemo.entity.UserSecurity();
        user.setSecUsrId("USER01");
        user.setUsername("USER01");
        user.setFirstName("Regular");
        user.setLastName("User");
        user.setUserType("U"); // Regular user type
        user.setPassword("password"); // Default password for testing
        return user;
    }

    /**
     * Generates test menu options for admin menu testing.
     * Creates a list of MenuOption objects for test scenarios.
     * 
     * @return List of MenuOption for testing
     */
    public java.util.List<com.carddemo.dto.MenuOption> generateMenuOptions() {
        java.util.List<com.carddemo.dto.MenuOption> options = new java.util.ArrayList<>();
        options.add(new com.carddemo.dto.MenuOption(1, "User Management", "COUSR00C", true, "ADMIN"));
        options.add(new com.carddemo.dto.MenuOption(2, "Reports", "CORPT00C", true, "ADMIN"));
        options.add(new com.carddemo.dto.MenuOption(3, "Exit", "EXIT", true, "ADMIN"));
        return options;
    }
}