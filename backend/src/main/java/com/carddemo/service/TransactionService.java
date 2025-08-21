/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified Spring Boot service class implementing transaction business logic translated 
 * from COTRN00C, COTRN01C, and COTRN02C COBOL programs. Provides transaction listing 
 * with cursor-based pagination, individual transaction detail retrieval, and new 
 * transaction creation with comprehensive validation.
 * 
 * This service maintains COBOL paragraph structure while leveraging Spring Boot service 
 * architecture for unified transaction operations. It replaces CICS transaction processing 
 * with Spring's @Transactional boundaries, VSAM file operations with JPA repositories, 
 * and BMS screen handling with REST-compatible data transfer patterns.
 * 
 * COBOL Program Migration:
 * - COTRN00C → listTransactions, getTransactionPage, processPageNavigation methods
 * - COTRN01C → getTransactionDetail method  
 * - COTRN02C → addTransaction, validateTransaction, processTransactionCreation methods
 * 
 * Key Migration Features:
 * - Cursor-based pagination replicating VSAM STARTBR/READNEXT/READPREV operations
 * - 10-record page size matching original COBOL implementation
 * - Transaction boundaries matching CICS SYNCPOINT behavior
 * - Comprehensive validation including amount limits, date validation, and cross-reference validation
 * - BigDecimal precision matching COBOL COMP-3 packed decimal calculations
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
@Transactional
public class TransactionService {

    private static final int PAGE_SIZE = 10; // Matches COBOL 10-record pages
    private static final AtomicLong transactionIdGenerator = new AtomicLong(1000000L);
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("999999.99");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardRepository cardRepository;

    // Transaction paging state variables (replicating COBOL working storage)
    private String firstTransactionId = "";
    private String lastTransactionId = "";
    private int currentPageNumber = 0;
    private boolean hasNextPage = false;
    private String errorMessage = "";
    private boolean errorFlag = false;

    /**
     * Lists transactions with pagination support, replicating COTRN00C PROCESS-ENTER-KEY 
     * and PROCESS-PAGE-FORWARD functionality. Implements cursor-based pagination to match 
     * VSAM STARTBR/READNEXT operations.
     * 
     * COBOL Source: COTRN00C.cbl lines 146-230 (PROCESS-ENTER-KEY)
     * Original Logic: Uses VSAM STARTBR with transaction ID cursor and READNEXT for pagination
     * 
     * @param accountId account ID to filter transactions (optional)
     * @param startingTransactionId transaction ID to start pagination from
     * @param pageNumber current page number (0-based)
     * @return Page of Transaction entities with pagination metadata
     */
    public Page<Transaction> listTransactions(Long accountId, String startingTransactionId, int pageNumber) {
        clearErrorState();
        this.currentPageNumber = pageNumber;
        
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("transactionId").ascending());
        
        Page<Transaction> transactionPage;
        if (accountId != null) {
            // Filter by account ID if provided (common use case from COBOL logic)
            transactionPage = transactionRepository.findByAccountId(accountId, pageable);
        } else {
            // Get all transactions with pagination
            transactionPage = transactionRepository.findAll(pageable);
        }
        
        // Update pagination state variables (replicating COBOL WS-VARIABLES)
        updatePaginationState(transactionPage);
        
        return transactionPage;
    }

    /**
     * Retrieves detailed information for a specific transaction, replicating COTRN01C 
     * transaction detail view functionality.
     * 
     * COBOL Source: COTRN01C.cbl transaction detail retrieval logic
     * Original Logic: EXEC CICS READ DATASET('TRANSACT') INTO(TRAN-RECORD)
     * 
     * @param transactionId the unique transaction ID to retrieve
     * @return Optional containing Transaction entity if found, empty otherwise
     */
    public Optional<Transaction> getTransactionDetail(String transactionId) {
        clearErrorState();
        
        if (transactionId == null || transactionId.trim().isEmpty()) {
            setErrorState("Transaction ID must not be empty");
            return Optional.empty();
        }
        
        // Validate that transaction ID is numeric (matching COBOL validation)
        if (!isNumeric(transactionId)) {
            setErrorState("Transaction ID must be numeric");
            return Optional.empty();
        }
        
        // Convert String transactionId to Long for database lookup
        try {
            Long transactionIdLong = Long.parseLong(transactionId);
            return transactionRepository.findById(transactionIdLong);
        } catch (NumberFormatException e) {
            setErrorState("Invalid transaction ID format");
            return Optional.empty();
        }
    }

    /**
     * Adds a new transaction with comprehensive validation, replicating COTRN02C 
     * transaction creation functionality including all business rules and validations.
     * 
     * COBOL Source: COTRN02C.cbl transaction addition and validation logic
     * Original Logic: Comprehensive validation followed by EXEC CICS WRITE DATASET('TRANSACT')
     * 
     * @param transaction the Transaction entity to add
     * @return the saved Transaction entity with generated ID
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Transaction addTransaction(Transaction transaction) {
        clearErrorState();
        
        // Generate new transaction ID (replicating COTRN02C ID generation logic)
        Long newTransactionId = generateTransactionIdLong();
        transaction.setTransactionId(newTransactionId);
        
        // Comprehensive validation (replicating COTRN02C validation paragraphs)
        validateTransaction(transaction);
        
        if (errorFlag) {
            throw new IllegalArgumentException("Transaction validation failed: " + errorMessage);
        }
        
        // Set transaction timestamp
        transaction.setOriginalTimestamp(LocalDateTime.now());
        
        // Process transaction creation (replicating COTRN02C creation logic)
        return processTransactionCreation(transaction);
    }

    /**
     * Comprehensive transaction validation replicating all COTRN02C validation rules
     * including amount validation, account validation, card validation, and merchant validation.
     * 
     * COBOL Source: COTRN02C.cbl validation paragraphs
     * Original Logic: Multiple validation paragraphs checking business rules
     * 
     * @param transaction the Transaction entity to validate
     */
    public void validateTransaction(Transaction transaction) {
        clearErrorState();
        
        // Validate transaction amount (COTRN02C amount validation)
        validateTransactionAmount(transaction.getAmount());
        if (errorFlag) return;
        
        // Validate account exists and is active
        validateAccountForTransaction(transaction.getAccountId());
        if (errorFlag) return;
        
        // Validate card if provided
        if (transaction.getCardNumber() != null) {
            validateCardForTransaction(transaction.getCardNumber(), transaction.getAccountId());
            if (errorFlag) return;
        }
        
        // Validate merchant information
        validateMerchantInfo(transaction.getMerchantName(), transaction.getMerchantCity());
        if (errorFlag) return;
        
        // Validate transaction type and category codes
        validateTransactionCodes(transaction.getTransactionTypeCode(), transaction.getCategoryCode());
    }

    /**
     * Processes page navigation for transaction listing, replicating COTRN00C 
     * PROCESS-PF7-KEY and PROCESS-PF8-KEY functionality for backward and forward navigation.
     * 
     * COBOL Source: COTRN00C.cbl lines 234-274 (PROCESS-PF7-KEY, PROCESS-PF8-KEY)
     * Original Logic: VSAM cursor positioning with STARTBR/READNEXT/READPREV
     * 
     * @param direction navigation direction ("forward", "backward") 
     * @param accountId account ID for filtering (optional)
     * @param currentCursor current cursor position (transaction ID)
     * @return Page of Transaction entities for the requested page
     */
    public Page<Transaction> processPageNavigation(String direction, Long accountId, String currentCursor) {
        clearErrorState();
        
        if ("forward".equals(direction)) {
            return processPageForward(accountId, currentCursor);
        } else if ("backward".equals(direction)) {
            return processPageBackward(accountId, currentCursor);
        } else {
            setErrorState("Invalid navigation direction. Use 'forward' or 'backward'");
            return Page.empty();
        }
    }

    /**
     * Retrieves a specific page of transactions with metadata, combining pagination 
     * functionality from COTRN00C with Spring Data JPA Page support.
     * 
     * COBOL Source: COTRN00C.cbl PROCESS-PAGE-FORWARD and PROCESS-PAGE-BACKWARD
     * Original Logic: VSAM pagination with 10-record pages
     * 
     * @param pageNumber zero-based page number
     * @param accountId account ID for filtering (optional)
     * @return Page of Transaction entities with pagination metadata
     */
    public Page<Transaction> getTransactionPage(int pageNumber, Long accountId) {
        clearErrorState();
        this.currentPageNumber = pageNumber;
        
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("transactionId").ascending());
        
        Page<Transaction> page;
        if (accountId != null) {
            page = transactionRepository.findByAccountId(accountId, pageable);
        } else {
            page = transactionRepository.findAll(pageable);
        }
        
        updatePaginationState(page);
        return page;
    }

    /**
     * Returns the current page number from service state, replicating COBOL 
     * WS-PAGE-NUM variable functionality.
     * 
     * COBOL Source: COTRN00C.cbl WS-PAGE-NUM variable (line 54)
     * 
     * @return current zero-based page number
     */
    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    /**
     * Calculates total page count for given account filter, supporting pagination 
     * navigation that replicates COBOL page counting logic.
     * 
     * COBOL Source: COTRN00C.cbl pagination logic with NEXT-PAGE-YES/NO flags
     * 
     * @param accountId account ID for filtering (optional)
     * @return total number of pages available
     */
    public int getPageCount(Long accountId) {
        long totalRecords;
        if (accountId != null) {
            totalRecords = transactionRepository.countByAccountId(accountId);
        } else {
            totalRecords = transactionRepository.count();
        }
        
        return (int) Math.ceil((double) totalRecords / PAGE_SIZE);
    }

    /**
     * Filters transactions based on various criteria, supporting advanced search 
     * functionality that extends basic COBOL transaction lookup operations.
     * 
     * COBOL Source: Extension of COTRN00C.cbl transaction lookup logic
     * Original Logic: VSAM key-based access with GTEQ positioning
     * 
     * @param accountId account ID filter (optional)
     * @param startDate start date for date range filtering (optional)
     * @param endDate end date for date range filtering (optional)
     * @param minAmount minimum transaction amount (optional)
     * @param maxAmount maximum transaction amount (optional)
     * @param pageNumber zero-based page number
     * @return Page of filtered Transaction entities
     */
    public Page<Transaction> filterTransactions(Long accountId, LocalDateTime startDate, 
                                              LocalDateTime endDate, BigDecimal minAmount, 
                                              BigDecimal maxAmount, int pageNumber) {
        clearErrorState();
        this.currentPageNumber = pageNumber;
        
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("transactionId").ascending());
        
        // Use repository method for date range filtering when available
        if (accountId != null && startDate != null && endDate != null) {
            return transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate, pageable);
        } else if (accountId != null) {
            return transactionRepository.findByAccountId(accountId, pageable);
        } else {
            return transactionRepository.findAll(pageable);
        }
    }

    /**
     * Sorts transactions by specified criteria, providing flexible sorting options 
     * that extend basic COBOL transaction ordering.
     * 
     * COBOL Source: COTRN00C.cbl transaction ordering by transaction ID
     * Original Logic: VSAM key sequence provides natural ordering
     * 
     * @param accountId account ID filter (optional)
     * @param sortField field to sort by ("transactionId", "amount", "date")
     * @param sortDirection sort direction ("asc" or "desc")
     * @param pageNumber zero-based page number
     * @return Page of sorted Transaction entities
     */
    public Page<Transaction> sortTransactions(Long accountId, String sortField, 
                                            String sortDirection, int pageNumber) {
        clearErrorState();
        this.currentPageNumber = pageNumber;
        
        // Build sort specification
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
                                 Sort.Direction.DESC : Sort.Direction.ASC;
        
        Sort sort;
        switch (sortField.toLowerCase()) {
            case "amount":
                sort = Sort.by(direction, "amount");
                break;
            case "date":
                sort = Sort.by(direction, "transactionOrigTs");
                break;
            case "transactionid":
            default:
                sort = Sort.by(direction, "transactionId");
                break;
        }
        
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, sort);
        
        if (accountId != null) {
            return transactionRepository.findByAccountId(accountId, pageable);
        } else {
            return transactionRepository.findAll(pageable);
        }
    }

    /**
     * Generates unique transaction ID as Long replicating COTRN02C transaction ID creation logic.
     * 
     * COBOL Source: COTRN02C.cbl transaction ID generation (inferred from logic)
     * Original Logic: Sequential ID assignment with uniqueness validation
     * 
     * @return unique transaction ID as Long
     */
    public Long generateTransactionIdLong() {
        return transactionIdGenerator.getAndIncrement();
    }

    /**
     * Generates unique transaction ID replicating COTRN02C transaction ID creation logic.
     * 
     * COBOL Source: COTRN02C.cbl transaction ID generation (inferred from logic)
     * Original Logic: Sequential ID assignment with uniqueness validation
     * 
     * @return unique transaction ID as String (for backward compatibility)
     */
    public String generateTransactionId() {
        return String.format("%016d", transactionIdGenerator.getAndIncrement());
    }

    /**
     * Validates transaction amount according to business rules from COTRN02C, 
     * ensuring amounts are within acceptable limits and properly formatted.
     * 
     * COBOL Source: COTRN02C.cbl amount validation logic
     * Original Logic: COBOL COMP-3 field validation with range checking
     * 
     * @param amount transaction amount to validate
     * @return true if amount is valid, false otherwise
     */
    public boolean validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            setErrorState("Transaction amount is required");
            return false;
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            setErrorState("Transaction amount must be positive");
            return false;
        }
        
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            setErrorState("Transaction amount exceeds maximum limit");
            return false;
        }
        
        // Validate decimal precision (matching COBOL COMP-3 precision)
        if (amount.scale() > 2) {
            setErrorState("Transaction amount cannot have more than 2 decimal places");
            return false;
        }
        
        return true;
    }

    /**
     * Validates merchant information according to COTRN02C business rules,
     * ensuring merchant name and city meet format requirements.
     * 
     * COBOL Source: COTRN02C.cbl merchant validation paragraphs
     * Original Logic: Field length and format validation for merchant data
     * 
     * @param merchantName merchant name to validate
     * @param merchantCity merchant city to validate
     * @return true if merchant information is valid, false otherwise
     */
    public boolean validateMerchantInfo(String merchantName, String merchantCity) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            setErrorState("Merchant name is required");
            return false;
        }
        
        if (merchantName.length() > 50) {
            setErrorState("Merchant name cannot exceed 50 characters");
            return false;
        }
        
        if (merchantCity == null || merchantCity.trim().isEmpty()) {
            setErrorState("Merchant city is required");
            return false;
        }
        
        if (merchantCity.length() > 50) {
            setErrorState("Merchant city cannot exceed 50 characters");
            return false;
        }
        
        return true;
    }

    /**
     * Processes transaction creation with all business logic from COTRN02C,
     * including final validation, balance checking, and database persistence.
     * 
     * COBOL Source: COTRN02C.cbl transaction creation paragraphs
     * Original Logic: Final validation, balance update, and VSAM WRITE operation
     * 
     * @param transaction the validated Transaction entity to create
     * @return the saved Transaction entity
     */
    @Transactional
    public Transaction processTransactionCreation(Transaction transaction) {
        // Final validation before creation
        Optional<Account> accountOpt = accountRepository.findById(transaction.getAccountId());
        if (!accountOpt.isPresent()) {
            setErrorState("Invalid account ID");
            throw new IllegalArgumentException("Account not found: " + transaction.getAccountId());
        }
        
        Account account = accountOpt.get();
        
        // Check if transaction amount exceeds available credit
        BigDecimal newBalance = account.getCurrentBalance().add(transaction.getAmount());
        if (newBalance.compareTo(account.getCreditLimit()) > 0) {
            setErrorState("Transaction would exceed credit limit");
            throw new IllegalArgumentException("Transaction exceeds available credit");
        }
        
        // Save transaction (replicating CICS WRITE DATASET operation)
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Update account balance (replicating COBOL balance update logic)
        account.setCurrentBalance(newBalance);
        accountRepository.save(account);
        
        return savedTransaction;
    }

    // ============================================================================
    // PRIVATE HELPER METHODS - Replicating COBOL paragraph structure
    // ============================================================================

    /**
     * Processes forward page navigation replicating COTRN00C PROCESS-PAGE-FORWARD.
     * 
     * COBOL Source: COTRN00C.cbl lines 279-328 (PROCESS-PAGE-FORWARD)
     */
    private Page<Transaction> processPageForward(Long accountId, String currentCursor) {
        int nextPage = currentPageNumber + 1;
        Pageable pageable = PageRequest.of(nextPage, PAGE_SIZE, Sort.by("transactionId").ascending());
        
        Page<Transaction> page;
        if (accountId != null) {
            page = transactionRepository.findByAccountId(accountId, pageable);
        } else {
            page = transactionRepository.findAll(pageable);
        }
        
        if (page.hasContent()) {
            currentPageNumber = nextPage;
            updatePaginationState(page);
        } else {
            setErrorState("You are already at the bottom of the page...");
        }
        
        return page;
    }

    /**
     * Processes backward page navigation replicating COTRN00C PROCESS-PAGE-BACKWARD.
     * 
     * COBOL Source: COTRN00C.cbl lines 333-376 (PROCESS-PAGE-BACKWARD)
     */
    private Page<Transaction> processPageBackward(Long accountId, String currentCursor) {
        if (currentPageNumber <= 0) {
            setErrorState("You are already at the top of the page...");
            return getTransactionPage(0, accountId);
        }
        
        int prevPage = currentPageNumber - 1;
        Pageable pageable = PageRequest.of(prevPage, PAGE_SIZE, Sort.by("transactionId").ascending());
        
        Page<Transaction> page;
        if (accountId != null) {
            page = transactionRepository.findByAccountId(accountId, pageable);
        } else {
            page = transactionRepository.findAll(pageable);
        }
        
        currentPageNumber = prevPage;
        updatePaginationState(page);
        
        return page;
    }

    /**
     * Updates pagination state variables replicating COBOL working storage variables.
     * 
     * COBOL Source: COTRN00C.cbl pagination state management
     */
    private void updatePaginationState(Page<Transaction> page) {
        if (page.hasContent()) {
            List<Transaction> content = page.getContent();
            firstTransactionId = content.get(0).getTransactionId().toString();
            lastTransactionId = content.get(content.size() - 1).getTransactionId().toString();
            hasNextPage = page.hasNext();
        } else {
            firstTransactionId = "";
            lastTransactionId = "";
            hasNextPage = false;
        }
    }

    /**
     * Validates account for transaction processing replicating COTRN02C account validation.
     * 
     * COBOL Source: COTRN02C.cbl account validation logic
     */
    private void validateAccountForTransaction(Long accountId) {
        if (accountId == null) {
            setErrorState("Account ID is required");
            return;
        }
        
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            setErrorState("Invalid account ID");
            return;
        }
        
        Account account = accountOpt.get();
        if (account.getCurrentBalance() == null || account.getCreditLimit() == null) {
            setErrorState("Account has invalid balance or credit limit");
        }
    }

    /**
     * Validates card for transaction processing replicating COTRN02C card validation logic.
     * 
     * COBOL Source: COTRN02C.cbl card validation paragraphs
     */
    private void validateCardForTransaction(String cardNumber, Long accountId) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            setErrorState("Card number is required");
            return;
        }
        
        Optional<Card> cardOpt = cardRepository.findById(cardNumber);
        if (!cardOpt.isPresent()) {
            setErrorState("Invalid card number");
            return;
        }
        
        Card card = cardOpt.get();
        
        // Validate card is active
        if (!"Y".equals(card.getActiveStatus())) {
            setErrorState("Card is not active");
            return;
        }
        
        // Validate card belongs to the account
        List<Card> accountCards = cardRepository.findByAccountId(accountId);
        boolean cardBelongsToAccount = accountCards.stream()
                .anyMatch(c -> c.getCardNumber().equals(cardNumber));
        
        if (!cardBelongsToAccount) {
            setErrorState("Card does not belong to the specified account");
            return;
        }
        
        // Validate card expiration date
        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDateTime.now().toLocalDate())) {
            setErrorState("Card has expired");
        }
    }

    /**
     * Validates transaction type and category codes replicating COTRN02C code validation.
     * 
     * COBOL Source: COTRN02C.cbl transaction code validation logic
     */
    private void validateTransactionCodes(String transactionTypeCode, String transactionCatCode) {
        // Validate transaction type code
        if (transactionTypeCode == null || transactionTypeCode.trim().isEmpty()) {
            setErrorState("Transaction type code is required");
            return;
        }
        
        // Valid transaction type codes (matching COBOL validation)
        if (!isValidTransactionTypeCode(transactionTypeCode)) {
            setErrorState("Invalid transaction type code");
            return;
        }
        
        // Validate transaction category code
        if (transactionCatCode == null || transactionCatCode.trim().isEmpty()) {
            setErrorState("Transaction category code is required");
            return;
        }
        
        // Valid category codes (matching COBOL validation)
        if (!isValidTransactionCategoryCode(transactionCatCode)) {
            setErrorState("Invalid transaction category code");
        }
    }

    /**
     * Validates transaction type codes against allowed values from COTRN02C logic.
     */
    private boolean isValidTransactionTypeCode(String code) {
        // Valid codes from COBOL validation logic
        return "01".equals(code) || "02".equals(code) || "03".equals(code) || 
               "04".equals(code) || "05".equals(code);
    }

    /**
     * Validates transaction category codes against allowed values from COTRN02C logic.
     */
    private boolean isValidTransactionCategoryCode(String code) {
        // Valid category codes from COBOL validation logic
        return "01".equals(code) || "02".equals(code) || "03".equals(code) || 
               "04".equals(code) || "05".equals(code) || "06".equals(code);
    }

    /**
     * Checks if a string is numeric, replicating COBOL NUMERIC test functionality.
     * 
     * COBOL Source: COTRN00C.cbl line 209 (IF TRNIDINI OF COTRN0AI IS NUMERIC)
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Clears error state replicating COBOL error flag initialization.
     * 
     * COBOL Source: COTRN00C.cbl line 97 (SET ERR-FLG-OFF TO TRUE)
     */
    private void clearErrorState() {
        this.errorFlag = false;
        this.errorMessage = "";
    }

    /**
     * Sets error state with message replicating COBOL error handling.
     * 
     * COBOL Source: COTRN00C.cbl error handling paragraphs
     */
    private void setErrorState(String message) {
        this.errorFlag = true;
        this.errorMessage = message;
    }

    /**
     * Gets current error flag state replicating COBOL WS-ERR-FLG variable.
     * 
     * @return true if error occurred, false otherwise
     */
    public boolean hasError() {
        return errorFlag;
    }

    /**
     * Gets current error message replicating COBOL WS-MESSAGE variable.
     * 
     * @return current error message or empty string if no error
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
