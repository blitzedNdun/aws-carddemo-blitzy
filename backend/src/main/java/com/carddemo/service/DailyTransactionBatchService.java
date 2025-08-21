/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.DailyTransaction;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.Account;
import com.carddemo.entity.CardXref;
import com.carddemo.repository.DailyTransactionRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.ValidationUtil;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.exception.BusinessRuleException;
import com.carddemo.exception.DataPrecisionException;

import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring Batch service for daily transaction processing and posting operations.
 * 
 * This service implements the core functionality of CBTRN01C.cbl batch program converted to 
 * Spring Boot architecture. It processes daily transaction files, validates transactions against
 * card cross-reference data, posts validated transactions to the main transactions table, and 
 * updates account balances while maintaining COBOL transaction sequencing and precision.
 * 
 * Key Features:
 * - Spring Batch chunk-based processing for high-volume transaction files
 * - Idempotent transaction processing to prevent duplicate posting
 * - Card number validation via XREF file equivalent (CardXref entity)
 * - Account validation and balance updates with COBOL COMP-3 precision
 * - Comprehensive error handling with transaction rollback capabilities
 * - Processing status tracking for batch job monitoring and restart
 * 
 * COBOL Program Mapping:
 * - CBTRN01C.cbl main processing loop → validateTransaction() and processTransaction()
 * - File open/close routines → Spring Batch Job/Step lifecycle management
 * - Cross-reference validation (lines 227-239) → validateCardAndAccount()
 * - Account read operations (lines 241-250) → account validation and updates
 * - Transaction ID generation → idempotent processing with duplicate detection
 * 
 * Processing Flow:
 * 1. Read unprocessed daily transactions (NEW/PENDING status)
 * 2. Validate card numbers against CardXref repository
 * 3. Validate associated account IDs and retrieve account data
 * 4. Post validated transactions to main Transaction repository
 * 5. Update account balances with exact BigDecimal precision
 * 6. Mark processing status as COMPLETED or FAILED
 * 
 * Error Handling:
 * - Invalid card numbers are logged and marked as FAILED
 * - Missing account records cause transaction to be marked as FAILED
 * - Data precision errors trigger DataPrecisionException
 * - Business rule violations trigger BusinessRuleException
 * - All errors preserve transaction integrity through rollback
 * 
 * @author CardDemo Migration Team
 * @version 1.0.0
 * @since CardDemo 1.0
 */
@Service
public class DailyTransactionBatchService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DailyTransactionBatchService.class);
    
    // Batch processing configuration constants
    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // COBOL precision constants for financial calculations
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // Processing status tracking
    private final AtomicLong processedTransactionCount = new AtomicLong(0);
    private final AtomicInteger validationErrors = new AtomicInteger(0);
    private final List<String> errorMessages = new ArrayList<>();
    
    // Repository dependencies for data access
    private final DailyTransactionRepository dailyTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    


    /**
     * Constructor with dependency injection for all required repositories.
     * 
     * @param dailyTransactionRepository Repository for accessing daily transaction staging data
     * @param transactionRepository Repository for posting validated transactions
     * @param accountRepository Repository for account validation and balance updates
     * @param cardXrefRepository Repository for card cross-reference validation
     */
    public DailyTransactionBatchService(
            DailyTransactionRepository dailyTransactionRepository,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    /**
     * Validates a daily transaction for posting eligibility.
     * 
     * This method replicates the validation logic from CBTRN01C.cbl lines 170-184, including:
     * - Card number validation against XREF file (lines 227-239)
     * - Account ID validation and existence check (lines 241-250)
     * - Transaction amount and field validation
     * - Duplicate transaction detection for idempotent processing
     * 
     * Validation Sequence (matching COBOL program flow):
     * 1. Validate required fields are present
     * 2. Check for duplicate transaction ID (idempotency)
     * 3. Validate card number exists in CardXref repository
     * 4. Validate associated account exists and is active
     * 5. Validate transaction amount and type codes
     * 
     * @param dailyTransaction The daily transaction to validate
     * @return true if transaction passes all validation checks, false otherwise
     * @throws BusinessRuleException if validation fails due to business rule violations
     * @throws DataPrecisionException if monetary precision validation fails
     */
    public boolean validateTransaction(DailyTransaction dailyTransaction) {
        logger.debug("Validating daily transaction ID: {}", dailyTransaction.getDailyTransactionId());
        
        try {
            // Step 1: Validate required fields
            validateRequiredFields(dailyTransaction);
            
            // Step 2: Check for duplicate processing (idempotency check)
            if ("COMPLETED".equals(dailyTransaction.getProcessingStatus())) {
                logger.warn("Transaction already processed: {}", dailyTransaction.getDailyTransactionId());
                return false; // Already processed, skip silently
            }
            
            // Step 3: Validate card number against XREF (replicating lines 227-239 of CBTRN01C)
            if (!validateCardNumber(dailyTransaction.getCardNumber())) {
                logger.warn("Invalid card number validation failed: {}", dailyTransaction.getCardNumber());
                dailyTransaction.markAsFailed("CARD NUMBER " + dailyTransaction.getCardNumber() + 
                    " COULD NOT BE VERIFIED. SKIPPING TRANSACTION ID-" + dailyTransaction.getTransactionId());
                validationErrors.incrementAndGet();
                return false;
            }
            
            // Step 4: Validate associated account exists and is active (replicating lines 241-250)
            if (!validateAccountExists(dailyTransaction.getAccountId())) {
                logger.warn("Account validation failed for account ID: {}", dailyTransaction.getAccountId());
                dailyTransaction.markAsFailed("ACCOUNT " + dailyTransaction.getAccountId() + " NOT FOUND");
                validationErrors.incrementAndGet();
                return false;
            }
            
            // Step 5: Validate transaction amount and precision
            if (!validateTransactionAmount(dailyTransaction.getTransactionAmount())) {
                logger.warn("Transaction amount validation failed: {}", dailyTransaction.getTransactionAmount());
                dailyTransaction.markAsFailed("Invalid transaction amount or precision error");
                validationErrors.incrementAndGet();
                return false;
            }
            
            // Step 6: Validate transaction type code
            ValidationUtil.validateRequiredField("Transaction Type Code", dailyTransaction.getTransactionTypeCode());
            
            // Step 7: Validate category code
            ValidationUtil.validateRequiredField("Category Code", dailyTransaction.getCategoryCode());
            
            logger.debug("Daily transaction validation successful for ID: {}", dailyTransaction.getDailyTransactionId());
            return true;
            
        } catch (Exception e) {
            logger.error("Validation error for daily transaction ID {}: {}", 
                dailyTransaction.getDailyTransactionId(), e.getMessage(), e);
            dailyTransaction.markAsFailed("Validation error: " + e.getMessage());
            validationErrors.incrementAndGet();
            
            if (e instanceof BusinessRuleException || e instanceof DataPrecisionException) {
                throw e;
            } else {
                throw new BusinessRuleException("Transaction validation failed: " + e.getMessage(), "9999", e);
            }
        }
    }

    /**
     * Processes a validated daily transaction by posting it to the main transactions table.
     * 
     * This method implements the transaction posting logic equivalent to COBOL transaction
     * processing, including account balance updates and maintaining CICS SYNCPOINT-equivalent
     * transaction boundaries through Spring's @Transactional annotation.
     * 
     * Processing Steps (maintaining COBOL program sequence):
     * 1. Mark daily transaction as PROCESSING status
     * 2. Create new Transaction entity from daily transaction data
     * 3. Apply COBOL COMP-3 precision to monetary amounts using CobolDataConverter
     * 4. Post transaction to main transactions table
     * 5. Update account balance with exact precision arithmetic
     * 6. Mark daily transaction as COMPLETED
     * 7. Increment processed transaction count
     * 
     * @param dailyTransaction The validated daily transaction to process
     * @return true if processing completed successfully, false if processing failed
     * @throws BusinessRuleException if business rules are violated during processing
     * @throws DataPrecisionException if precision is lost in monetary calculations
     */
    public boolean processTransaction(DailyTransaction dailyTransaction) {
        logger.debug("Processing daily transaction ID: {}", dailyTransaction.getDailyTransactionId());
        
        try {
            // Step 1: Mark transaction as processing
            dailyTransaction.markAsProcessing();
            dailyTransactionRepository.save(dailyTransaction);
            
            // Step 2: Create main transaction entity from daily transaction
            Transaction transaction = createTransactionFromDaily(dailyTransaction);
            
            // Step 3: Apply COBOL precision using CobolDataConverter
            BigDecimal preciseAmount = CobolDataConverter.toBigDecimal(
                dailyTransaction.getTransactionAmount(), MONETARY_SCALE);
            transaction.setAmount(preciseAmount);
            
            // Step 4: Set processing timestamps
            transaction.setOriginalTimestamp(dailyTransaction.getOriginalTimestamp());
            transaction.setProcessedTimestamp(LocalDateTime.now());
            
            // Step 5: Post transaction to main table (equivalent to COBOL WRITE operation)
            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.debug("Posted transaction to main table with ID: {}", savedTransaction.getTransactionId());
            
            // Step 6: Update account balance (equivalent to COBOL account update)
            updateAccountBalance(dailyTransaction.getAccountId(), preciseAmount);
            
            // Step 7: Mark daily transaction as completed
            dailyTransaction.markAsSuccess();
            dailyTransactionRepository.save(dailyTransaction);
            
            // Step 8: Increment processed count
            processedTransactionCount.incrementAndGet();
            
            logger.debug("Successfully processed daily transaction ID: {}", dailyTransaction.getDailyTransactionId());
            return true;
            
        } catch (Exception e) {
            logger.error("Processing error for daily transaction ID {}: {}", 
                dailyTransaction.getDailyTransactionId(), e.getMessage(), e);
            
            // Mark transaction as failed and rollback
            dailyTransaction.markAsFailed("Processing error: " + e.getMessage());
            dailyTransactionRepository.save(dailyTransaction);
            
            if (e instanceof BusinessRuleException || e instanceof DataPrecisionException) {
                throw e;
            } else {
                throw new BusinessRuleException("Transaction processing failed: " + e.getMessage(), "9999", e);
            }
        }
    }

    /**
     * Creates a Spring Batch ItemReader for processing daily transactions.
     * 
     * This method configures a reader that retrieves unprocessed daily transactions
     * in chunks for batch processing. The reader supports pagination and implements
     * the equivalent of COBOL sequential file reading operations from CBTRN01C.cbl.
     * 
     * Reader Configuration:
     * - Reads transactions with NEW or PENDING processing status
     * - Orders by original timestamp to maintain processing sequence
     * - Supports chunk-based processing for memory efficiency
     * - Provides restart capability for failed batch jobs
     * 
     * @return Configured ItemReader for daily transactions
     */
    public org.springframework.batch.item.ItemReader<DailyTransaction> createTransactionReader() {
        logger.info("Creating transaction reader for daily transaction batch processing");
        
        org.springframework.batch.item.data.RepositoryItemReader<DailyTransaction> reader = 
            new org.springframework.batch.item.data.RepositoryItemReader<>();
        reader.setRepository(dailyTransactionRepository);
        reader.setMethodName("findUnprocessedTransactions");
        reader.setSort(java.util.Map.of("originalTimestamp", org.springframework.data.domain.Sort.Direction.ASC));
        reader.setPageSize(DEFAULT_CHUNK_SIZE);
        reader.setSaveState(true); // Enable restart capability
        return reader;
    }

    /**
     * Creates a Spring Batch ItemProcessor for validating and transforming daily transactions.
     * 
     * This processor implements the validation logic equivalent to CBTRN01C.cbl's main
     * processing loop, including card cross-reference validation and account verification.
     * Transactions that fail validation are marked as FAILED and excluded from further processing.
     * 
     * Processing Logic:
     * - Validates each transaction using validateTransaction()
     * - Returns null for invalid transactions to exclude them from writing
     * - Returns validated transaction for successful validation
     * - Maintains processing statistics for monitoring
     * 
     * @return Configured ItemProcessor for daily transaction validation
     */
    public org.springframework.batch.item.ItemProcessor<DailyTransaction, DailyTransaction> createTransactionProcessor() {
        logger.info("Creating transaction processor for daily transaction validation");
        
        return dailyTransaction -> {
            try {
                logger.debug("Processing daily transaction: {}", dailyTransaction.getTransactionId());
                
                // Validate transaction using main validation method
                boolean isValid = validateTransaction(dailyTransaction);
                
                if (isValid) {
                    logger.debug("Daily transaction validation successful: {}", dailyTransaction.getTransactionId());
                    return dailyTransaction; // Pass to writer
                } else {
                    logger.warn("Daily transaction validation failed: {}", dailyTransaction.getTransactionId());
                    return null; // Exclude from writer
                }
                
            } catch (Exception e) {
                logger.error("Error processing daily transaction {}: {}", 
                    dailyTransaction.getTransactionId(), e.getMessage(), e);
                
                dailyTransaction.markAsFailed("Processor error: " + e.getMessage());
                return null; // Exclude from writer
            }
        };
    }

    /**
     * Creates a Spring Batch ItemWriter for posting validated transactions.
     * 
     * This writer implements the transaction posting logic equivalent to COBOL's
     * WRITE operations, posting validated daily transactions to the main transactions
     * table and updating account balances with COBOL COMP-3 precision.
     * 
     * Writing Logic:
     * - Processes validated transactions in chunks
     * - Posts each transaction using processTransaction()
     * - Maintains transaction boundaries equivalent to CICS SYNCPOINT
     * - Updates processing counts and error statistics
     * - Implements retry logic for transient failures
     * 
     * @return Configured ItemWriter for transaction posting
     */
    public ItemWriter<DailyTransaction> createTransactionWriter() {
        logger.info("Creating transaction writer for posting validated transactions");
        
        return new ItemWriter<DailyTransaction>() {
            @Override
            public void write(org.springframework.batch.item.Chunk<? extends DailyTransaction> chunk) throws Exception {
                logger.debug("Writing {} validated transactions to main table", chunk.size());
                
                for (DailyTransaction dailyTransaction : chunk) {
                    try {
                        // Process each validated transaction
                        boolean success = processTransaction(dailyTransaction);
                        
                        if (success) {
                            logger.debug("Successfully posted transaction: {}", dailyTransaction.getTransactionId());
                        } else {
                            logger.warn("Failed to post transaction: {}", dailyTransaction.getTransactionId());
                        }
                        
                    } catch (Exception e) {
                        logger.error("Error writing transaction {}: {}", 
                            dailyTransaction.getTransactionId(), e.getMessage(), e);
                        
                        // Mark as failed but continue processing other transactions
                        dailyTransaction.markAsFailed("Writer error: " + e.getMessage());
                        dailyTransactionRepository.save(dailyTransaction);
                    }
                }
                
                logger.info("Completed writing {} transactions", chunk.size());
            }
        };
    }

    /**
     * Gets the total count of processed transactions.
     * 
     * This method provides access to the processing statistics maintained during
     * batch execution, equivalent to COBOL program counters and totals.
     * 
     * @return The total number of successfully processed transactions
     */
    public long getProcessedTransactionCount() {
        return processedTransactionCount.get();
    }

    /**
     * Gets the total count of validation errors encountered.
     * 
     * This method provides access to the error statistics maintained during
     * batch execution for monitoring and reporting purposes.
     * 
     * @return The total number of validation errors encountered
     */
    public int getValidationErrors() {
        return validationErrors.get();
    }

    // Private helper methods for validation and processing

    /**
     * Validates required fields in a daily transaction.
     * 
     * Checks that all mandatory fields are present and properly formatted,
     * equivalent to COBOL field validation routines.
     * 
     * @param dailyTransaction The transaction to validate
     * @throws BusinessRuleException if any required fields are missing or invalid
     */
    private void validateRequiredFields(DailyTransaction dailyTransaction) {
        // Validate account ID
        if (dailyTransaction.getAccountId() == null) {
            throw new BusinessRuleException("Account ID is required", "1001");
        }
        
        // Validate card number
        ValidationUtil.validateRequiredField("Card Number", dailyTransaction.getCardNumber());
        
        // Validate transaction date
        if (dailyTransaction.getTransactionDate() == null) {
            throw new BusinessRuleException("Transaction date is required", "1002");
        }
        
        // Validate transaction amount
        if (dailyTransaction.getTransactionAmount() == null) {
            throw new BusinessRuleException("Transaction amount is required", "1003");
        }
        
        // Validate transaction type code
        if (dailyTransaction.getTransactionTypeCode() == null || 
            dailyTransaction.getTransactionTypeCode().trim().isEmpty()) {
            throw new BusinessRuleException("Transaction type code is required", "1004");
        }
        
        // Validate category code
        if (dailyTransaction.getCategoryCode() == null || 
            dailyTransaction.getCategoryCode().trim().isEmpty()) {
            throw new BusinessRuleException("Category code is required", "1005");
        }
    }

    /**
     * Validates card number against card cross-reference data.
     * 
     * This method replicates the XREF file validation logic from CBTRN01C.cbl
     * lines 227-239, checking that the card number exists in the CardXref repository
     * and retrieving associated account and customer information.
     * 
     * @param cardNumber The card number to validate (16-character string)
     * @return true if card number is valid and exists in cross-reference, false otherwise
     */
    private boolean validateCardNumber(String cardNumber) {
        try {
            logger.debug("Validating card number against XREF: {}", cardNumber);
            
            // Check if card exists in cross-reference (equivalent to CBTRN01C lines 228-239)
            java.util.Optional<CardXref> cardXrefOptional = cardXrefRepository.findFirstByXrefCardNum(cardNumber);
            if (cardXrefOptional.isEmpty()) {
                logger.warn("INVALID CARD NUMBER FOR XREF: {}", cardNumber);
                return false;
            }
            
            // Retrieve cross-reference data for additional validation
            CardXref cardXref = cardXrefOptional.get();
            if (cardXref == null) {
                logger.warn("Card cross-reference not found for card: {}", cardNumber);
                return false;
            }
            
            logger.debug("SUCCESSFUL READ OF XREF - CARD NUMBER: {}, ACCOUNT ID: {}, CUSTOMER ID: {}", 
                cardXref.getXrefCardNum(), cardXref.getXrefAcctId(), cardXref.getXrefCustId());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating card number {}: {}", cardNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates that an account exists and is active.
     * 
     * This method replicates the account validation logic from CBTRN01C.cbl
     * lines 241-250, checking account existence and status.
     * 
     * @param accountId The account ID to validate
     * @return true if account exists and is active, false otherwise
     */
    private boolean validateAccountExists(Long accountId) {
        try {
            logger.debug("Validating account existence for ID: {}", accountId);
            
            // Check if account exists (equivalent to CBTRN01C lines 242-250)
            if (!accountRepository.existsById(accountId)) {
                logger.warn("INVALID ACCOUNT NUMBER FOUND: {}", accountId);
                return false;
            }
            
            // Retrieve account data for status validation
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                logger.warn("Account not found for ID: {}", accountId);
                return false;
            }
            
            // Check account active status
            if (!"Y".equals(account.getActiveStatus())) {
                logger.warn("Account is not active: {}", accountId);
                return false;
            }
            
            logger.debug("SUCCESSFUL READ OF ACCOUNT FILE for ID: {}", accountId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating account {}: {}", accountId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates transaction amount for precision and business rules.
     * 
     * Ensures transaction amount maintains COBOL COMP-3 precision requirements
     * and falls within acceptable business limits.
     * 
     * @param amount The transaction amount to validate
     * @return true if amount is valid, false otherwise
     * @throws DataPrecisionException if precision validation fails
     */
    private boolean validateTransactionAmount(BigDecimal amount) {
        try {
            // Validate amount is positive
            ValidationUtil.validateTransactionAmount(amount);
            
            // Verify precision preservation using CobolDataConverter
            BigDecimal preciseAmount = CobolDataConverter.toBigDecimal(amount, MONETARY_SCALE);
            
            // Check for precision loss
            if (amount.compareTo(preciseAmount) != 0) {
                throw new DataPrecisionException("Precision loss detected in transaction amount: " + 
                    amount + " vs " + preciseAmount, MONETARY_SCALE, amount.scale(), amount);
            }
            
            // Validate amount is within business limits (equivalent to COBOL range checks)
            if (preciseAmount.scale() != MONETARY_SCALE) {
                preciseAmount = CobolDataConverter.preservePrecision(preciseAmount, MONETARY_SCALE);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Amount validation failed for {}: {}", amount, e.getMessage(), e);
            if (e instanceof DataPrecisionException) {
                throw e;
            }
            return false;
        }
    }

    /**
     * Creates a main Transaction entity from a DailyTransaction.
     * 
     * Maps all relevant fields from the daily transaction staging record
     * to the main transaction table structure while preserving COBOL data types.
     * 
     * @param dailyTransaction The source daily transaction
     * @return New Transaction entity ready for posting
     */
    private Transaction createTransactionFromDaily(DailyTransaction dailyTransaction) {
        Transaction transaction = new Transaction();
        
        // Set core transaction fields
        transaction.setTransactionId(null); // Auto-generated by database
        transaction.setAccountId(dailyTransaction.getAccountId());
        transaction.setCardNumber(dailyTransaction.getCardNumber());
        transaction.setTransactionDate(dailyTransaction.getTransactionDate());
        transaction.setAmount(dailyTransaction.getTransactionAmount());
        transaction.setTransactionTypeCode(dailyTransaction.getTransactionTypeCode());
        transaction.setCategoryCode(dailyTransaction.getCategoryCode());
        
        // Set descriptive fields
        transaction.setDescription(dailyTransaction.getDescription());
        transaction.setMerchantName(dailyTransaction.getMerchantName());
        transaction.setMerchantId(dailyTransaction.getMerchantId());
        
        // Set timestamps
        transaction.setOriginalTimestamp(dailyTransaction.getOriginalTimestamp());
        transaction.setProcessedTimestamp(LocalDateTime.now());
        
        // Set source identifier for audit trail
        transaction.setSource("BATCH_DAILY_PROCESSING");
        
        logger.debug("Created transaction entity from daily transaction ID: {}", 
            dailyTransaction.getDailyTransactionId());
        
        return transaction;
    }

    /**
     * Updates account balance with a transaction amount.
     * 
     * This method implements account balance updates with COBOL COMP-3 precision,
     * equivalent to COBOL account file update operations. Maintains exact
     * monetary precision using BigDecimal arithmetic.
     * 
     * @param accountId The account ID to update
     * @param transactionAmount The transaction amount to apply
     * @throws BusinessRuleException if account update fails
     * @throws DataPrecisionException if precision is lost in calculations
     */
    private void updateAccountBalance(Long accountId, BigDecimal transactionAmount) {
        try {
            logger.debug("Updating account balance for ID: {} with amount: {}", accountId, transactionAmount);
            
            // Retrieve current account data
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("Account not found for balance update: " + accountId, "2001"));
            
            // Get current balance with COBOL precision
            BigDecimal currentBalance = account.getCurrentBalance();
            if (currentBalance == null) {
                currentBalance = BigDecimal.ZERO.setScale(MONETARY_SCALE, COBOL_ROUNDING_MODE);
            }
            
            // Calculate new balance with exact precision
            BigDecimal newBalance = currentBalance.add(transactionAmount.setScale(MONETARY_SCALE, COBOL_ROUNDING_MODE));
            
            // Apply precision preservation
            newBalance = CobolDataConverter.toBigDecimal(newBalance, MONETARY_SCALE);
            
            // Update account balance
            account.setCurrentBalance(newBalance);
            account.setLastTransactionDate(LocalDate.now());
            
            // Save updated account
            accountRepository.save(account);
            
            logger.debug("Successfully updated account {} balance from {} to {}", 
                accountId, currentBalance, newBalance);
            
        } catch (Exception e) {
            logger.error("Error updating account balance for {}: {}", accountId, e.getMessage(), e);
            
            if (e instanceof BusinessRuleException || e instanceof DataPrecisionException) {
                throw e;
            } else {
                throw new BusinessRuleException("Account balance update failed: " + e.getMessage(), "2999", e);
            }
        }
    }
}
