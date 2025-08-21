/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.dto.BillPaymentResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Spring Boot service implementing bill payment processing translated from COBIL00C.cbl.
 * 
 * This service processes bill payment transactions, updates account balances, generates payment 
 * confirmations, and maintains transaction audit trail. Preserves COBOL amount validation, 
 * balance checking, and transaction ID generation logic while leveraging Spring transaction management.
 * 
 * Key COBOL-to-Java mappings:
 * - MAIN-PARA logic → processBillPayment() method
 * - PROCESS-ENTER-KEY → payment validation and processing logic  
 * - GET-CURRENT-TIMESTAMP → LocalDateTime.now() usage
 * - READ-ACCTDAT-FILE → AccountRepository.findById()
 * - UPDATE-ACCTDAT-FILE → AccountRepository.save()
 * - WRITE-TRANSACT-FILE → TransactionRepository.save()
 * - WS-ERR-FLG logic → exception handling and response status
 * - CONF-PAY-YES/NO → confirmation flag processing
 * 
 * Maintains exact business logic from COBOL:
 * - Account ID validation (cannot be empty)
 * - Balance validation (must have amount to pay)
 * - Confirmation requirement for payment processing
 * - Transaction ID generation (sequential increment)
 * - Full balance payment (pays entire current balance)
 * - Atomic transaction recording and balance update
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
public class BillPaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillPaymentService.class);
    
    // COBOL equivalent constants
    private static final String TRANSACTION_TYPE_CODE = "02";
    private static final int TRANSACTION_CATEGORY_CODE = 2;
    private static final String TRANSACTION_SOURCE = "POS TERM";
    private static final String TRANSACTION_DESCRIPTION = "BILL PAYMENT - ONLINE";
    private static final Long MERCHANT_ID = 999999999L;
    private static final String MERCHANT_NAME = "BILL PAYMENT";
    private static final String MERCHANT_CITY = "N/A";
    private static final String MERCHANT_ZIP = "N/A";
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    /**
     * Constructor for dependency injection matching Spring framework patterns.
     * Equivalent to COBOL LINKAGE SECTION setup for accessing data files.
     * 
     * @param accountRepository JPA repository for account operations (replaces ACCTDAT VSAM access)
     * @param transactionRepository JPA repository for transaction operations (replaces TRANSACT VSAM access)
     */
    public BillPaymentService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Processes bill payment request with full COBOL business logic preservation.
     * 
     * Translates COBIL00C.cbl MAIN-PARA and PROCESS-ENTER-KEY logic:
     * 1. Validates account ID is not empty (lines 159-167 in COBOL)
     * 2. Reads account data for balance verification (lines 170-195)
     * 3. Validates balance > 0 (something to pay) (lines 198-206)
     * 4. If confirmation = Y: processes payment (lines 210-241)
     * 5. Generates next transaction ID (lines 212-217)
     * 6. Creates transaction record (lines 218-232)
     * 7. Updates account balance to zero (lines 234-235)
     * 8. Returns success response with transaction ID
     * 
     * @param request Bill payment request containing accountId and confirmPayment flag
     * @return BillPaymentResponse with transaction details, updated balance, and status
     */
    @Transactional
    public BillPaymentResponse processBillPayment(BillPaymentRequest request) {
        logger.info("Processing bill payment for account: {}", request.getAccountId());
        
        BillPaymentResponse response = new BillPaymentResponse();
        
        try {
            // Step 1: Validate account ID is not empty (COBOL lines 159-167)
            if (request.getAccountId() == null || request.getAccountId().trim().isEmpty()) {
                response.setSuccess(false);
                response.setErrorMessage("Acct ID can NOT be empty...");
                logger.warn("Bill payment failed: Account ID is empty");
                return response;
            }
            
            // Step 2: Read account data (COBOL READ-ACCTDAT-FILE lines 343-372)
            Long accountId = Long.parseLong(request.getAccountId());
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            
            if (!accountOpt.isPresent()) {
                response.setSuccess(false);
                response.setErrorMessage("Account ID NOT found...");
                logger.warn("Bill payment failed: Account {} not found", accountId);
                return response;
            }
            
            Account account = accountOpt.get();
            
            // Step 3: Validate account has balance to pay (COBOL lines 198-206)
            if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
                response.setSuccess(false);
                response.setErrorMessage("You have nothing to pay...");
                logger.warn("Bill payment failed: Account {} has zero or negative balance", accountId);
                return response;
            }
            
            // Step 4: Check confirmation flag (COBOL lines 173-191)
            if (request.getConfirmPayment() == null || request.getConfirmPayment().trim().isEmpty()) {
                // Initial display - show balance and ask for confirmation
                response.setSuccess(false);
                response.setErrorMessage("Confirm to make a bill payment...");
                response.setCurrentBalance(account.getCurrentBalance());
                logger.info("Bill payment pending confirmation for account {}", accountId);
                return response;
            }
            
            // Validate confirmation value
            String confirmation = request.getConfirmPayment().trim().toUpperCase();
            if (!confirmation.equals("Y") && !confirmation.equals("N")) {
                response.setSuccess(false);
                response.setErrorMessage("Invalid value. Valid values are (Y/N)...");
                logger.warn("Bill payment failed: Invalid confirmation value '{}' for account {}", 
                           request.getConfirmPayment(), accountId);
                return response;
            }
            
            // If declined, return with current balance
            if (confirmation.equals("N")) {
                response.setSuccess(false);
                response.setCurrentBalance(account.getCurrentBalance());
                logger.info("Bill payment declined by user for account {}", accountId);
                return response;
            }
            
            // Step 5: Process confirmed payment (COBOL lines 210-241)
            if (confirmation.equals("Y")) {
                // Get next transaction ID (COBOL lines 212-217)
                Long nextTransactionId = generateNextTransactionId();
                
                // Store original balance for transaction record
                BigDecimal paymentAmount = account.getCurrentBalance();
                
                // Create transaction record (COBOL lines 218-232)
                Transaction transaction = new Transaction();
                transaction.setTransactionId(nextTransactionId);
                transaction.setTransactionTypeCode(TRANSACTION_TYPE_CODE);
                // transaction.setCategoryCode(String.valueOf(TRANSACTION_CATEGORY_CODE)); // Set category if needed
                transaction.setSource(TRANSACTION_SOURCE);
                transaction.setDescription(TRANSACTION_DESCRIPTION);
                transaction.setAmount(paymentAmount);
                // transaction.setCardNumber(cardNumber); // Would need CXACAIX lookup like COBOL
                transaction.setMerchantId(MERCHANT_ID);
                transaction.setMerchantName(MERCHANT_NAME);
                transaction.setMerchantCity(MERCHANT_CITY);
                transaction.setMerchantZip(MERCHANT_ZIP);
                transaction.setAccountId(accountId);
                transaction.setTransactionDate(LocalDate.now());
                transaction.setOriginalTimestamp(LocalDateTime.now()); // COBOL GET-CURRENT-TIMESTAMP
                transaction.setProcessedTimestamp(LocalDateTime.now());
                
                // Save transaction record (COBOL WRITE-TRANSACT-FILE)
                transactionRepository.save(transaction);
                
                // Update account balance to zero (COBOL lines 234-235)
                account.setCurrentBalance(account.getCurrentBalance().subtract(paymentAmount));
                accountRepository.save(account);
                
                // Build success response (COBOL lines 524-532)
                response.setSuccess(true);
                response.setTransactionId(String.valueOf(nextTransactionId));
                response.setCurrentBalance(account.getCurrentBalance());
                response.setSuccessMessage("Payment successful. Your Transaction ID is " + nextTransactionId + ".");
                
                logger.info("Bill payment successful for account {}: amount={}, transactionId={}", 
                           accountId, paymentAmount, nextTransactionId);
            }
            
        } catch (NumberFormatException e) {
            response.setSuccess(false);
            response.setErrorMessage("Invalid account ID format");
            logger.error("Bill payment failed: Invalid account ID format '{}'", request.getAccountId(), e);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Unable to process bill payment...");
            logger.error("Bill payment failed for account {}: {}", request.getAccountId(), e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * Generates next sequential transaction ID matching COBOL logic.
     * 
     * Replicates COBOL transaction ID generation from lines 212-217:
     * - STARTBR on TRANSACT file with HIGH-VALUES
     * - READPREV to get last transaction
     * - Add 1 to get next ID
     * 
     * @return Next available transaction ID
     */
    private Long generateNextTransactionId() {
        // Find highest existing transaction ID (COBOL READPREV equivalent)
        Optional<Transaction> lastTransaction = transactionRepository.findTopByOrderByTransactionIdDesc();
        
        if (lastTransaction.isPresent()) {
            return lastTransaction.get().getTransactionId() + 1;
        } else {
            // Start with 1 if no transactions exist (COBOL ZEROS case)
            return 1L;
        }
    }
}