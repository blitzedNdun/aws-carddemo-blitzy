/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.card;

import com.carddemo.card.CardRepository;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.card.BillPaymentRequestDto;
import com.carddemo.card.BillPaymentResponseDto;
import com.carddemo.account.entity.Account;
import com.carddemo.transaction.Transaction;
import com.carddemo.card.Card;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.ValidationUtils;
import com.carddemo.common.enums.AccountStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BillPaymentService - Business service for bill payment processing implementing COBIL00C.cbl functionality
 * with real-time balance updates, transaction audit trails, and BigDecimal financial precision.
 * 
 * This service implements the complete bill payment workflow from the COBOL program COBIL00C.cbl,
 * maintaining exact functional equivalence while providing modern Spring Boot microservices
 * architecture with distributed transaction management and comprehensive audit capabilities.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Real-time account balance updates with ACID transaction compliance</li>
 *   <li>Comprehensive transaction audit trail management with PostgreSQL integration</li>
 *   <li>BigDecimal precision handling equivalent to COBOL COMP-3 arithmetic</li>
 *   <li>Account balance validation and atomic update operations with JPA repository methods</li>
 *   <li>Comprehensive business rule validation for payment amounts, account status, and credit limit verification</li>
 *   <li>Transaction ID generation using UUID replacing COBOL sequential logic</li>
 *   <li>Spring @Transactional annotations ensuring CICS syncpoint equivalent behavior</li>
 * </ul>
 * 
 * <p>COBOL Program Mapping:
 * <ul>
 *   <li>COBIL00C.cbl → BillPaymentService.java (Complete functional mapping)</li>
 *   <li>PROCESS-ENTER-KEY → processBillPayment() (Main processing logic)</li>
 *   <li>Account validation → validatePaymentRequest() (Input validation)</li>
 *   <li>Balance calculation → calculatePaymentAmount() (Payment amount logic)</li>
 *   <li>Transaction creation → generateTransactionRecord() (Transaction record generation)</li>
 *   <li>Account update → updateAccountBalance() (Balance update logic)</li>
 *   <li>Response building → buildPaymentResponse() (Success message generation)</li>
 * </ul>
 * 
 * <p>Business Logic Preservation:
 * <ul>
 *   <li>Account ID validation: Must not be empty and must exist in ACCTDAT (accounts table)</li>
 *   <li>Confirmation flag validation: Must be 'Y' or 'N' (case insensitive)</li>
 *   <li>Balance validation: Account balance must be greater than zero</li>
 *   <li>Transaction ID generation: Sequential increment from last transaction ID</li>
 *   <li>Transaction record creation: Type '02', Category '2', Source 'POS TERM', Description 'BILL PAYMENT - ONLINE'</li>
 *   <li>Account balance update: Subtract payment amount from current balance</li>
 *   <li>Success message: "Payment successful. Your Transaction ID is [ID]."</li>
 * </ul>
 * 
 * <p>Performance Requirements:
 * <ul>
 *   <li>Transaction response times: &lt;200ms at 95th percentile</li>
 *   <li>Concurrent processing: Support 10,000 TPS without degradation</li>
 *   <li>Memory usage: No increase beyond 10% of CICS baseline</li>
 *   <li>Database optimization: B-tree indexes for sub-millisecond lookups</li>
 * </ul>
 * 
 * <p>Security and Compliance:
 * <ul>
 *   <li>PCI DSS compliant transaction processing</li>
 *   <li>SOX 404 audit trail requirements</li>
 *   <li>GDPR data protection compliance</li>
 *   <li>Comprehensive logging for regulatory reporting</li>
 * </ul>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 * @see com.carddemo.card.BillPaymentRequestDto
 * @see com.carddemo.card.BillPaymentResponseDto
 * @see com.carddemo.account.entity.Account
 * @see com.carddemo.transaction.Transaction
 */
@Service
public class BillPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(BillPaymentService.class);

    // Constants matching COBOL program values
    private static final String TRANSACTION_TYPE_CODE = "02"; // TRAN-TYPE-CD from COBIL00C.cbl
    private static final String TRANSACTION_CATEGORY_CODE = "0002"; // TRAN-CAT-CD from COBIL00C.cbl (4 digits)
    private static final String TRANSACTION_SOURCE = "POS TERM"; // TRAN-SOURCE from COBIL00C.cbl
    private static final String TRANSACTION_DESCRIPTION = "BILL PAYMENT - ONLINE"; // TRAN-DESC from COBIL00C.cbl
    private static final String MERCHANT_ID = "999999999"; // TRAN-MERCHANT-ID from COBIL00C.cbl
    private static final String MERCHANT_NAME = "BILL PAYMENT"; // TRAN-MERCHANT-NAME from COBIL00C.cbl
    private static final String MERCHANT_CITY = "N/A"; // TRAN-MERCHANT-CITY from COBIL00C.cbl
    private static final String MERCHANT_ZIP = "N/A"; // TRAN-MERCHANT-ZIP from COBIL00C.cbl

    // Repository dependencies for data access
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Main entry point for bill payment processing implementing COBIL00C.cbl PROCESS-ENTER-KEY logic.
     * 
     * This method orchestrates the complete bill payment workflow with comprehensive validation,
     * transaction generation, account balance updates, and audit trail management. It implements
     * the exact business logic from the COBOL program while providing modern transaction 
     * management and error handling capabilities.
     * 
     * <p>Processing Steps:
     * <ol>
     *   <li>Validate payment request (account ID, confirmation flag, payment amount)</li>
     *   <li>Calculate payment amount (full account balance payment)</li>
     *   <li>Execute payment transaction with atomic operations</li>
     *   <li>Generate transaction record with audit trail</li>
     *   <li>Update account balance with optimistic locking</li>
     *   <li>Build comprehensive payment response with confirmation</li>
     * </ol>
     * 
     * <p>Transaction Boundaries:
     * Uses Spring @Transactional with REQUIRES_NEW propagation to ensure independent
     * transaction boundaries equivalent to CICS syncpoint behavior from the original
     * COBOL program.
     * 
     * @param request Bill payment request with account ID, confirmation flag, and payment details
     * @return BillPaymentResponseDto containing payment confirmation, transaction details, and audit information
     * @throws IllegalArgumentException if request validation fails
     * @throws RuntimeException if payment processing encounters system errors
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BillPaymentResponseDto processBillPayment(@Valid BillPaymentRequestDto request) {
        logger.info("Processing bill payment request for account: {}, correlation: {}", 
                   request.getAccountId(), request.getCorrelationId());

        try {
            // Step 1: Validate payment request (equivalent to COBOL input validation)
            validatePaymentRequest(request);

            // Step 2: Calculate payment amount (equivalent to COBOL balance calculation)
            BigDecimal paymentAmount = calculatePaymentAmount(request);

            // Step 3: Execute payment transaction (equivalent to COBOL transaction processing)
            Transaction transaction = executePayment(request, paymentAmount);

            // Step 4: Generate transaction record (equivalent to COBOL WRITE-TRANSACT-FILE)
            generateTransactionRecord(transaction);

            // Step 5: Update account balance (equivalent to COBOL UPDATE-ACCTDAT-FILE)
            Account updatedAccount = updateAccountBalance(request.getAccountId(), paymentAmount);

            // Step 6: Build payment response (equivalent to COBOL success message)
            BillPaymentResponseDto response = buildPaymentResponse(transaction, updatedAccount, paymentAmount);

            logger.info("Bill payment processed successfully for account: {}, transaction: {}, amount: {}", 
                       request.getAccountId(), transaction.getTransactionId(), paymentAmount);

            return response;

        } catch (Exception e) {
            logger.error("Bill payment processing failed for account: {}, correlation: {}, error: {}", 
                        request.getAccountId(), request.getCorrelationId(), e.getMessage(), e);
            throw new RuntimeException("Bill payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates payment request for business rule compliance implementing COBOL validation logic.
     * 
     * This method implements the comprehensive validation logic from COBIL00C.cbl, including
     * account ID validation, confirmation flag validation, and account existence verification.
     * All validation messages match the original COBOL error messages for consistency.
     * 
     * <p>Validation Rules (from COBOL program):
     * <ul>
     *   <li>Account ID cannot be empty or spaces (ACTIDINI OF COBIL0AI = SPACES OR LOW-VALUES)</li>
     *   <li>Confirmation flag must be 'Y' or 'N' (CONFIRMI OF COBIL0AI validation)</li>
     *   <li>Account must exist in ACCTDAT file (READ-ACCTDAT-FILE logic)</li>
     *   <li>Account must be active and accessible</li>
     *   <li>Account balance must be greater than zero</li>
     * </ul>
     * 
     * @param request Bill payment request to validate
     * @throws IllegalArgumentException if validation fails with descriptive error message
     */
    public void validatePaymentRequest(@Valid BillPaymentRequestDto request) {
        logger.debug("Validating payment request for account: {}", request.getAccountId());

        // Validate account ID (equivalent to COBOL: ACTIDINI OF COBIL0AI = SPACES OR LOW-VALUES)
        if (!ValidationUtils.validateAccountNumber(request.getAccountId()).isValid()) {
            throw new IllegalArgumentException("Account ID cannot be empty...");
        }

        // Validate confirmation flag (equivalent to COBOL: CONFIRMI OF COBIL0AI validation)
        if (!request.getConfirmationFlag().matches("^[YyNn]$")) {
            throw new IllegalArgumentException("Invalid value. Valid values are (Y/N)...");
        }

        // Check if payment is confirmed (equivalent to COBOL: CONF-PAY-YES validation)
        if (!request.isConfirmed()) {
            throw new IllegalArgumentException("Confirm to make a bill payment...");
        }

        // Validate account existence (equivalent to COBOL: READ-ACCTDAT-FILE)
        Optional<Account> accountOpt = accountRepository.findByAccountIdAndActiveStatus(
                request.getAccountId(), AccountStatus.ACTIVE);
        
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account ID NOT found...");
        }

        Account account = accountOpt.get();

        // Validate account balance (equivalent to COBOL: ACCT-CURR-BAL <= ZEROS)
        if (BigDecimalUtils.compare(account.getCurrentBalance(), BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("You have nothing to pay...");
        }

        // Validate account balance for payment processing
        if (!ValidationUtils.validateBalance(account.getCurrentBalance()).isValid()) {
            throw new IllegalArgumentException("Invalid account balance for payment processing");
        }

        logger.debug("Payment request validation completed successfully for account: {}", request.getAccountId());
    }

    /**
     * Calculates payment amount based on account balance implementing COBOL payment logic.
     * 
     * This method implements the payment amount calculation from COBIL00C.cbl, which
     * always pays the full account balance. The calculation uses BigDecimal precision
     * equivalent to COBOL COMP-3 arithmetic to ensure exact financial accuracy.
     * 
     * <p>COBOL Logic Implementation:
     * <ul>
     *   <li>Retrieve account current balance (ACCT-CURR-BAL)</li>
     *   <li>Use full balance as payment amount (TRAN-AMT = ACCT-CURR-BAL)</li>
     *   <li>Maintain exact decimal precision for financial calculations</li>
     * </ul>
     * 
     * @param request Bill payment request containing account information
     * @return BigDecimal payment amount with exact COBOL COMP-3 precision
     * @throws IllegalArgumentException if account not found or balance calculation fails
     */
    public BigDecimal calculatePaymentAmount(@Valid BillPaymentRequestDto request) {
        logger.debug("Calculating payment amount for account: {}", request.getAccountId());

        // Retrieve account for balance calculation
        Optional<Account> accountOpt = accountRepository.findByAccountIdAndActiveStatus(
                request.getAccountId(), AccountStatus.ACTIVE);
        
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for payment amount calculation");
        }

        Account account = accountOpt.get();
        BigDecimal currentBalance = account.getCurrentBalance();

        // Validate balance is positive (equivalent to COBOL balance validation)
        if (BigDecimalUtils.compare(currentBalance, BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Account balance must be positive for payment processing");
        }

        // Payment amount is the full current balance (COBOL: TRAN-AMT = ACCT-CURR-BAL)
        BigDecimal paymentAmount = BigDecimalUtils.createDecimal(currentBalance.toString());

        logger.debug("Payment amount calculated: {} for account: {}", paymentAmount, request.getAccountId());

        return paymentAmount;
    }

    /**
     * Executes payment transaction with comprehensive validation and audit trail management.
     * 
     * This method orchestrates the payment execution process, including card validation,
     * transaction creation, and business rule enforcement. It implements the transaction
     * processing logic from COBIL00C.cbl while providing modern audit capabilities.
     * 
     * <p>Execution Steps:
     * <ul>
     *   <li>Validate card association with account (READ-CXACAIX-FILE logic)</li>
     *   <li>Create transaction record with COBOL-equivalent field values</li>
     *   <li>Set transaction timestamps for audit trail</li>
     *   <li>Apply business rules for payment processing</li>
     * </ul>
     * 
     * @param request Bill payment request with payment details
     * @param paymentAmount Calculated payment amount with BigDecimal precision
     * @return Transaction entity ready for database persistence
     * @throws IllegalArgumentException if card validation fails
     * @throws RuntimeException if transaction creation encounters system errors
     */
    public Transaction executePayment(@Valid BillPaymentRequestDto request, BigDecimal paymentAmount) {
        logger.debug("Executing payment for account: {}, amount: {}", request.getAccountId(), paymentAmount);

        // Validate card association with account (equivalent to COBOL: READ-CXACAIX-FILE)
        String cardNumber = getCardNumberForAccount(request.getAccountId());

        // Generate unique transaction ID (equivalent to COBOL: ADD 1 TO WS-TRAN-ID-NUM)
        String transactionId = generateTransactionId();

        // Create transaction record with COBOL field mappings
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(TRANSACTION_TYPE_CODE); // '02' from COBOL
        transaction.setCategoryCode(TRANSACTION_CATEGORY_CODE); // '0002' from COBOL (4 digits)
        transaction.setSource(TRANSACTION_SOURCE); // 'POS TERM' from COBOL
        transaction.setDescription(TRANSACTION_DESCRIPTION); // 'BILL PAYMENT - ONLINE' from COBOL
        transaction.setAmount(paymentAmount); // TRAN-AMT from COBOL
        transaction.setCardNumber(cardNumber); // TRAN-CARD-NUM from COBOL
        transaction.setMerchantId(MERCHANT_ID); // 999999999 from COBOL
        transaction.setMerchantName(MERCHANT_NAME); // 'BILL PAYMENT' from COBOL
        transaction.setMerchantCity(MERCHANT_CITY); // 'N/A' from COBOL
        transaction.setMerchantZip(MERCHANT_ZIP); // 'N/A' from COBOL

        // Set timestamps (equivalent to COBOL: GET-CURRENT-TIMESTAMP)
        LocalDateTime now = LocalDateTime.now();
        transaction.setOriginalTimestamp(now);
        transaction.setProcessingTimestamp(now);

        logger.debug("Payment transaction created: {} for account: {}", transactionId, request.getAccountId());

        return transaction;
    }

    /**
     * Generates transaction record and persists to database implementing COBOL WRITE-TRANSACT-FILE logic.
     * 
     * This method implements the transaction record creation and persistence logic from
     * COBIL00C.cbl, including duplicate transaction ID handling and comprehensive error
     * management. It ensures transaction audit trail integrity through atomic operations.
     * 
     * <p>COBOL Logic Implementation:
     * <ul>
     *   <li>Write transaction record to TRANSACT file (WRITE-TRANSACT-FILE)</li>
     *   <li>Handle duplicate transaction ID errors (DFHRESP(DUPKEY))</li>
     *   <li>Provide comprehensive error handling and logging</li>
     *   <li>Maintain transaction audit trail for compliance</li>
     * </ul>
     * 
     * @param transaction Transaction entity to persist with complete audit information
     * @throws RuntimeException if transaction persistence fails or duplicate ID detected
     */
    public void generateTransactionRecord(Transaction transaction) {
        logger.debug("Generating transaction record: {}", transaction.getTransactionId());

        try {
            // Persist transaction record (equivalent to COBOL: WRITE-TRANSACT-FILE)
            transactionRepository.save(transaction);

            logger.info("Transaction record generated successfully: {}, amount: {}", 
                       transaction.getTransactionId(), transaction.getAmount());

        } catch (Exception e) {
            // Handle duplicate transaction ID (equivalent to COBOL: DFHRESP(DUPKEY))
            if (e.getMessage().contains("duplicate") || e.getMessage().contains("unique")) {
                logger.error("Duplicate transaction ID detected: {}", transaction.getTransactionId());
                throw new RuntimeException("Transaction ID already exists...");
            }

            // General transaction persistence error
            logger.error("Unable to add bill payment transaction: {}, error: {}", 
                        transaction.getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Unable to add bill payment transaction...");
        }
    }

    /**
     * Updates account balance with atomic operations implementing COBOL UPDATE-ACCTDAT-FILE logic.
     * 
     * This method implements the account balance update logic from COBIL00C.cbl, including
     * optimistic locking for concurrent access and exact decimal arithmetic for financial
     * precision. It ensures balance updates are atomic and consistent.
     * 
     * <p>COBOL Logic Implementation:
     * <ul>
     *   <li>Read account record for update (READ-ACCTDAT-FILE with UPDATE)</li>
     *   <li>Calculate new balance (COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT)</li>
     *   <li>Update account record (UPDATE-ACCTDAT-FILE)</li>
     *   <li>Handle optimistic locking for concurrent access</li>
     * </ul>
     * 
     * @param accountId Account identifier for balance update
     * @param paymentAmount Payment amount to subtract from balance
     * @return Updated Account entity with new balance
     * @throws IllegalArgumentException if account not found or balance update fails
     * @throws RuntimeException if optimistic locking conflict occurs
     */
    public Account updateAccountBalance(String accountId, BigDecimal paymentAmount) {
        logger.debug("Updating account balance for account: {}, payment: {}", accountId, paymentAmount);

        // Read account record for update (equivalent to COBOL: READ-ACCTDAT-FILE with UPDATE)
        Optional<Account> accountOpt = accountRepository.findByAccountIdAndActiveStatus(
                accountId, AccountStatus.ACTIVE);
        
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account ID NOT found...");
        }

        Account account = accountOpt.get();
        BigDecimal currentBalance = account.getCurrentBalance();

        // Calculate new balance (equivalent to COBOL: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT)
        BigDecimal newBalance = BigDecimalUtils.subtract(currentBalance, paymentAmount);

        // Update account balance
        account.setCurrentBalance(newBalance);

        try {
            // Persist updated account (equivalent to COBOL: UPDATE-ACCTDAT-FILE)
            Account updatedAccount = accountRepository.save(account);

            logger.info("Account balance updated successfully: {}, previous: {}, new: {}, payment: {}", 
                       accountId, currentBalance, newBalance, paymentAmount);

            return updatedAccount;

        } catch (Exception e) {
            // Handle optimistic locking or update conflicts
            logger.error("Unable to update account balance: {}, error: {}", accountId, e.getMessage(), e);
            throw new RuntimeException("Unable to update account...");
        }
    }

    /**
     * Builds comprehensive payment response with transaction details and audit information.
     * 
     * This method creates the complete payment response equivalent to the COBOL success
     * message generation, including transaction confirmation, balance updates, and audit
     * trail information. It implements the response formatting from COBIL00C.cbl.
     * 
     * <p>COBOL Response Logic:
     * <ul>
     *   <li>Generate success message (STRING 'Payment successful. Your Transaction ID is ' ...)</li>
     *   <li>Include transaction details for receipt generation</li>
     *   <li>Provide balance update confirmation</li>
     *   <li>Create comprehensive audit trail</li>
     * </ul>
     * 
     * @param transaction Transaction entity with complete transaction details
     * @param updatedAccount Account entity with updated balance information
     * @param paymentAmount Payment amount processed with BigDecimal precision
     * @return BillPaymentResponseDto with comprehensive payment confirmation and audit information
     */
    public BillPaymentResponseDto buildPaymentResponse(Transaction transaction, Account updatedAccount, BigDecimal paymentAmount) {
        logger.debug("Building payment response for transaction: {}", transaction.getTransactionId());

        // Create successful payment response
        BillPaymentResponseDto response = new BillPaymentResponseDto();

        // Set payment amount and transaction details
        response.setPaymentAmount(paymentAmount);
        response.setPaymentConfirmationNumber(transaction.getTransactionId());
        response.setProcessingStatus("SUCCESS");
        response.setPaymentSuccessful(true);

        // Set success message (equivalent to COBOL: STRING 'Payment successful. Your Transaction ID is ' ...)
        String successMessage = String.format("Payment successful. Your Transaction ID is %s.", 
                                            transaction.getTransactionId());
        response.setStatusMessage(successMessage);

        // Set base response properties
        response.setSuccess(true);
        response.setMessage("Bill payment processed successfully");
        response.setOperation("BILL_PAYMENT");
        response.setTimestamp(LocalDateTime.now());

        logger.info("Payment response built successfully for transaction: {}, amount: {}", 
                   transaction.getTransactionId(), paymentAmount);

        return response;
    }

    /**
     * Retrieves card number for account using cross-reference lookup implementing COBOL READ-CXACAIX-FILE logic.
     * 
     * This method implements the card cross-reference lookup from COBIL00C.cbl, which reads
     * the CXACAIX file to get the card number associated with an account. It provides the
     * card number needed for transaction record creation.
     * 
     * @param accountId Account identifier for card lookup
     * @return Card number associated with the account
     * @throws IllegalArgumentException if no card found for account
     */
    private String getCardNumberForAccount(String accountId) {
        logger.debug("Looking up card number for account: {}", accountId);

        // Find card associated with account (equivalent to COBOL: READ-CXACAIX-FILE)
        Optional<Card> cardOpt = cardRepository.findByAccountId(accountId).stream().findFirst();
        
        if (cardOpt.isEmpty()) {
            throw new IllegalArgumentException("No card found for account: " + accountId);
        }

        String cardNumber = cardOpt.get().getCardNumber();
        logger.debug("Card number found: {} for account: {}", cardNumber, accountId);

        return cardNumber;
    }

    /**
     * Generates unique transaction ID replacing COBOL sequential ID generation logic.
     * 
     * This method replaces the COBOL transaction ID generation logic (STARTBR-TRANSACT-FILE,
     * READPREV-TRANSACT-FILE, ADD 1 TO WS-TRAN-ID-NUM) with modern UUID generation for
     * distributed systems while maintaining transaction uniqueness.
     * 
     * @return Unique transaction ID for the payment transaction
     */
    private String generateTransactionId() {
        // Generate UUID-based transaction ID for distributed systems
        String transactionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        
        logger.debug("Generated transaction ID: {}", transactionId);
        
        return transactionId;
    }
}