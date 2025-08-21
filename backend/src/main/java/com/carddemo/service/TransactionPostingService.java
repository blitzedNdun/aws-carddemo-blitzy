package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot service implementing daily transaction posting business logic 
 * translated from COBOL CBTRN02C.cbl. Validates transactions including credit 
 * limits and expiration date checks, performs cross-reference and account lookups, 
 * posts transactions to indexed files, updates category balances, generates reject 
 * records with validation trailers, formats DB2-style timestamps, and returns 
 * condition codes (0 for success, 4 for rejects). Maintains exact COBOL posting 
 * logic and validation rules.
 */
@Service
@Transactional
public class TransactionPostingService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPostingService.class);

    // Constants for validation failure reasons (matching COBOL logic)
    private static final int INVALID_CARD_NUMBER = 100;
    private static final int ACCOUNT_NOT_FOUND = 101;
    private static final int OVERLIMIT_TRANSACTION = 102;
    private static final int TRANSACTION_AFTER_EXPIRATION = 103;
    private static final int ACCOUNT_UPDATE_FAILED = 109;

    // Condition codes (matching COBOL return codes)
    private static final int SUCCESS_CODE = 0;
    private static final int REJECT_CODE = 4;

    // Counters (matching COBOL working storage)
    private int transactionCount = 0;
    private int rejectCount = 0;
    private int conditionCode = SUCCESS_CODE;

    // Working storage fields
    private int validationFailReason = 0;
    private String validationFailDescription = "";
    private BigDecimal tempBalance = BigDecimal.ZERO;
    private boolean createTranscatRecord = false;

    /**
     * Data structure representing daily transaction record from DALYTRAN-RECORD
     * Equivalent to the COBOL copybook structure used in CBTRN02C
     */
    public static class DailyTransactionRecord {
        private String transactionId;          // DALYTRAN-ID PIC X(16)
        private String cardNumber;             // DALYTRAN-CARD-NUM PIC X(16)
        private String typeCode;               // DALYTRAN-TYPE-CD PIC X(02)
        private int categoryCode;              // DALYTRAN-CAT-CD PIC 9(04)
        private String source;                 // DALYTRAN-SOURCE PIC X(10)
        private String description;            // DALYTRAN-DESC PIC X(26)
        private BigDecimal amount;             // DALYTRAN-AMT PIC S9(09)V99 COMP-3
        private String merchantId;             // DALYTRAN-MERCHANT-ID PIC X(15)
        private String merchantName;           // DALYTRAN-MERCHANT-NAME PIC X(50)
        private String merchantCity;           // DALYTRAN-MERCHANT-CITY PIC X(50)
        private String merchantZip;            // DALYTRAN-MERCHANT-ZIP PIC X(10)
        private String originalTimestamp;      // DALYTRAN-ORIG-TS PIC X(26)

        // Constructors, getters, and setters
        public DailyTransactionRecord() {}

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

        public int getCategoryCode() { return categoryCode; }
        public void setCategoryCode(int categoryCode) { this.categoryCode = categoryCode; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

        public String getMerchantCity() { return merchantCity; }
        public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }

        public String getMerchantZip() { return merchantZip; }
        public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }

        public String getOriginalTimestamp() { return originalTimestamp; }
        public void setOriginalTimestamp(String originalTimestamp) { this.originalTimestamp = originalTimestamp; }
    }

    /**
     * Data structure representing cross-reference record from CARD-XREF-RECORD
     * Equivalent to the COBOL copybook structure
     */
    public static class CrossReferenceRecord {
        private String cardNumber;     // XREF-CARD-NUM PIC X(16)
        private long accountId;        // XREF-ACCT-ID PIC 9(11)

        public CrossReferenceRecord() {}

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public long getAccountId() { return accountId; }
        public void setAccountId(long accountId) { this.accountId = accountId; }
    }

    /**
     * Data structure representing account record from ACCOUNT-RECORD
     * Equivalent to the COBOL copybook structure
     */
    public static class AccountRecord {
        private long accountId;                    // ACCT-ID PIC 9(11)
        private BigDecimal creditLimit;            // ACCT-CREDIT-LIMIT PIC S9(09)V99 COMP-3
        private BigDecimal currentBalance;         // ACCT-CURR-BAL PIC S9(09)V99 COMP-3
        private BigDecimal currentCycleCredit;     // ACCT-CURR-CYC-CREDIT PIC S9(09)V99 COMP-3
        private BigDecimal currentCycleDebit;      // ACCT-CURR-CYC-DEBIT PIC S9(09)V99 COMP-3
        private String expirationDate;             // ACCT-EXPIRAION-DATE PIC X(10)

        public AccountRecord() {}

        public long getAccountId() { return accountId; }
        public void setAccountId(long accountId) { this.accountId = accountId; }

        public BigDecimal getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

        public BigDecimal getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

        public BigDecimal getCurrentCycleCredit() { return currentCycleCredit; }
        public void setCurrentCycleCredit(BigDecimal currentCycleCredit) { this.currentCycleCredit = currentCycleCredit; }

        public BigDecimal getCurrentCycleDebit() { return currentCycleDebit; }
        public void setCurrentCycleDebit(BigDecimal currentCycleDebit) { this.currentCycleDebit = currentCycleDebit; }

        public String getExpirationDate() { return expirationDate; }
        public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    }

    /**
     * Data structure representing transaction category balance record from TRAN-CAT-BAL-RECORD
     * Equivalent to the COBOL copybook structure
     */
    public static class TransactionCategoryBalanceRecord {
        private long accountId;            // TRANCAT-ACCT-ID PIC 9(11)
        private String typeCode;           // TRANCAT-TYPE-CD PIC X(02)
        private int categoryCode;          // TRANCAT-CD PIC 9(04)
        private BigDecimal balance;        // TRAN-CAT-BAL PIC S9(09)V99 COMP-3

        public TransactionCategoryBalanceRecord() {}

        public long getAccountId() { return accountId; }
        public void setAccountId(long accountId) { this.accountId = accountId; }

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

        public int getCategoryCode() { return categoryCode; }
        public void setCategoryCode(int categoryCode) { this.categoryCode = categoryCode; }

        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
    }

    /**
     * Data structure representing transaction record from TRAN-RECORD
     * Equivalent to the COBOL copybook structure for posted transactions
     */
    public static class TransactionRecord {
        private String transactionId;          // TRAN-ID PIC X(16)
        private String typeCode;               // TRAN-TYPE-CD PIC X(02)
        private int categoryCode;              // TRAN-CAT-CD PIC 9(04)
        private String source;                 // TRAN-SOURCE PIC X(10)
        private String description;            // TRAN-DESC PIC X(26)
        private BigDecimal amount;             // TRAN-AMT PIC S9(09)V99 COMP-3
        private String merchantId;             // TRAN-MERCHANT-ID PIC X(15)
        private String merchantName;           // TRAN-MERCHANT-NAME PIC X(50)
        private String merchantCity;           // TRAN-MERCHANT-CITY PIC X(50)
        private String merchantZip;            // TRAN-MERCHANT-ZIP PIC X(10)
        private String cardNumber;             // TRAN-CARD-NUM PIC X(16)
        private String originalTimestamp;      // TRAN-ORIG-TS PIC X(26)
        private String processedTimestamp;     // TRAN-PROC-TS PIC X(26)

        public TransactionRecord() {}

        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

        public int getCategoryCode() { return categoryCode; }
        public void setCategoryCode(int categoryCode) { this.categoryCode = categoryCode; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

        public String getMerchantCity() { return merchantCity; }
        public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }

        public String getMerchantZip() { return merchantZip; }
        public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getOriginalTimestamp() { return originalTimestamp; }
        public void setOriginalTimestamp(String originalTimestamp) { this.originalTimestamp = originalTimestamp; }

        public String getProcessedTimestamp() { return processedTimestamp; }
        public void setProcessedTimestamp(String processedTimestamp) { this.processedTimestamp = processedTimestamp; }
    }

    /**
     * Data structure representing reject record from REJECT-RECORD
     * Equivalent to the COBOL copybook structure
     */
    public static class RejectRecord {
        private String transactionData;        // REJECT-TRAN-DATA PIC X(350)
        private int validationFailReason;      // WS-VALIDATION-FAIL-REASON PIC 9(04)
        private String validationFailDescription; // WS-VALIDATION-FAIL-REASON-DESC PIC X(76)

        public RejectRecord() {}

        public String getTransactionData() { return transactionData; }
        public void setTransactionData(String transactionData) { this.transactionData = transactionData; }

        public int getValidationFailReason() { return validationFailReason; }
        public void setValidationFailReason(int validationFailReason) { this.validationFailReason = validationFailReason; }

        public String getValidationFailDescription() { return validationFailDescription; }
        public void setValidationFailDescription(String validationFailDescription) { this.validationFailDescription = validationFailDescription; }
    }

    // Storage for simulation purposes (in real implementation these would be repositories)
    private final List<DailyTransactionRecord> dailyTransactions = new ArrayList<>();
    private final List<CrossReferenceRecord> crossReferences = new ArrayList<>();
    private final List<AccountRecord> accounts = new ArrayList<>();
    private final List<TransactionCategoryBalanceRecord> categoryBalances = new ArrayList<>();
    private final List<TransactionRecord> postedTransactions = new ArrayList<>();
    private final List<RejectRecord> rejectedTransactions = new ArrayList<>();

    /**
     * Main entry point for posting daily transactions - equivalent to COBOL main procedure
     * Processes all daily transactions, validates them, and either posts or rejects them
     * Returns condition code: 0 for success, 4 if any transactions were rejected
     */
    public int postDailyTransactions() {
        logger.info("START OF EXECUTION OF PROGRAM TransactionPostingService");
        
        // Initialize counters (equivalent to COBOL working storage initialization)
        transactionCount = 0;
        rejectCount = 0;
        conditionCode = SUCCESS_CODE;

        // Process each daily transaction (equivalent to COBOL main loop)
        for (DailyTransactionRecord dailyTransaction : dailyTransactions) {
            transactionCount++;
            
            // Reset validation fields for each transaction
            validationFailReason = 0;
            validationFailDescription = "";
            
            // Validate transaction (equivalent to PERFORM 1500-VALIDATE-TRAN)
            validateTransaction(dailyTransaction);
            
            if (validationFailReason == 0) {
                // Post valid transaction (equivalent to PERFORM 2000-POST-TRANSACTION)
                processTransaction(dailyTransaction);
            } else {
                // Reject invalid transaction (equivalent to PERFORM 2500-WRITE-REJECT-REC)
                rejectCount++;
                generateRejectRecord(dailyTransaction);
            }
        }

        // Set condition code based on rejects (matching COBOL logic)
        if (rejectCount > 0) {
            conditionCode = REJECT_CODE;
        }

        logger.info("TRANSACTIONS PROCESSED: {}", transactionCount);
        logger.info("TRANSACTIONS REJECTED: {}", rejectCount);
        logger.info("END OF EXECUTION OF PROGRAM TransactionPostingService");

        return conditionCode;
    }

    /**
     * Validates a transaction - equivalent to COBOL 1500-VALIDATE-TRAN paragraph
     * Performs cross-reference lookup and account validation
     */
    public boolean validateTransaction(DailyTransactionRecord transaction) {
        // Reset validation state
        validationFailReason = 0;
        validationFailDescription = "";

        // Perform cross-reference lookup (equivalent to PERFORM 1500-A-LOOKUP-XREF)
        CrossReferenceRecord xrefRecord = lookupCrossReference(transaction.getCardNumber());
        
        if (validationFailReason == 0 && xrefRecord != null) {
            // Perform account lookup and validation (equivalent to PERFORM 1500-B-LOOKUP-ACCT)
            AccountRecord accountRecord = lookupAccount(xrefRecord.getAccountId());
            
            if (validationFailReason == 0 && accountRecord != null) {
                // Validate credit limit (equivalent to COBOL credit limit check)
                validateCreditLimit(accountRecord, transaction.getAmount());
                
                if (validationFailReason == 0) {
                    // Validate expiration date (equivalent to COBOL expiration check)
                    validateExpirationDate(accountRecord, transaction.getOriginalTimestamp());
                }
            }
        }

        return validationFailReason == 0;
    }

    /**
     * Processes a valid transaction - equivalent to COBOL 2000-POST-TRANSACTION paragraph
     * Creates transaction record, updates balances, and posts to transaction file
     */
    public void processTransaction(DailyTransactionRecord dailyTransaction) {
        // Create transaction record (equivalent to COBOL move operations)
        TransactionRecord transactionRecord = new TransactionRecord();
        transactionRecord.setTransactionId(dailyTransaction.getTransactionId());
        transactionRecord.setTypeCode(dailyTransaction.getTypeCode());
        transactionRecord.setCategoryCode(dailyTransaction.getCategoryCode());
        transactionRecord.setSource(dailyTransaction.getSource());
        transactionRecord.setDescription(dailyTransaction.getDescription());
        transactionRecord.setAmount(dailyTransaction.getAmount());
        transactionRecord.setMerchantId(dailyTransaction.getMerchantId());
        transactionRecord.setMerchantName(dailyTransaction.getMerchantName());
        transactionRecord.setMerchantCity(dailyTransaction.getMerchantCity());
        transactionRecord.setMerchantZip(dailyTransaction.getMerchantZip());
        transactionRecord.setCardNumber(dailyTransaction.getCardNumber());
        transactionRecord.setOriginalTimestamp(dailyTransaction.getOriginalTimestamp());
        
        // Set processing timestamp (equivalent to PERFORM Z-GET-DB2-FORMAT-TIMESTAMP)
        transactionRecord.setProcessedTimestamp(formatTimestamp());

        // Update category balance (equivalent to PERFORM 2700-UPDATE-TCATBAL)
        CrossReferenceRecord xref = lookupCrossReference(dailyTransaction.getCardNumber());
        if (xref != null) {
            updateCategoryBalance(xref.getAccountId(), dailyTransaction.getTypeCode(), 
                                dailyTransaction.getCategoryCode(), dailyTransaction.getAmount());
            
            // Update account record (equivalent to PERFORM 2800-UPDATE-ACCOUNT-REC)
            updateAccountRecord(xref.getAccountId(), dailyTransaction.getAmount());
        }

        // Write transaction to file (equivalent to PERFORM 2900-WRITE-TRANSACTION-FILE)
        postTransaction(transactionRecord);
    }

    /**
     * Generates reject record for invalid transaction - equivalent to COBOL 2500-WRITE-REJECT-REC
     */
    public void generateRejectRecord(DailyTransactionRecord transaction) {
        RejectRecord rejectRecord = new RejectRecord();
        
        // Convert transaction to string representation (equivalent to COBOL move to REJECT-TRAN-DATA)
        StringBuilder transData = new StringBuilder();
        transData.append(String.format("%-16s", transaction.getTransactionId() != null ? transaction.getTransactionId() : ""));
        transData.append(String.format("%-16s", transaction.getCardNumber() != null ? transaction.getCardNumber() : ""));
        transData.append(String.format("%-2s", transaction.getTypeCode() != null ? transaction.getTypeCode() : ""));
        transData.append(String.format("%04d", transaction.getCategoryCode()));
        transData.append(String.format("%-10s", transaction.getSource() != null ? transaction.getSource() : ""));
        transData.append(String.format("%-26s", transaction.getDescription() != null ? transaction.getDescription() : ""));
        transData.append(String.format("%012.2f", transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO));
        
        rejectRecord.setTransactionData(transData.toString());
        rejectRecord.setValidationFailReason(validationFailReason);
        rejectRecord.setValidationFailDescription(validationFailDescription);

        writeRejectRecord(rejectRecord);
    }

    /**
     * Updates category balance - equivalent to COBOL 2700-UPDATE-TCATBAL paragraph
     * Creates new balance record if not found, otherwise updates existing balance
     */
    public void updateCategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal amount) {
        // Look for existing category balance record
        TransactionCategoryBalanceRecord balanceRecord = null;
        for (TransactionCategoryBalanceRecord record : categoryBalances) {
            if (record.getAccountId() == accountId && 
                typeCode.equals(record.getTypeCode()) && 
                record.getCategoryCode() == categoryCode) {
                balanceRecord = record;
                break;
            }
        }

        if (balanceRecord == null) {
            // Create new category balance record (equivalent to PERFORM 2700-A-CREATE-TCATBAL-REC)
            createTranscatRecord = true;
            balanceRecord = new TransactionCategoryBalanceRecord();
            balanceRecord.setAccountId(accountId);
            balanceRecord.setTypeCode(typeCode);
            balanceRecord.setCategoryCode(categoryCode);
            balanceRecord.setBalance(amount);
            categoryBalances.add(balanceRecord);
            
            logger.debug("TCATBAL record not found for key: {}-{}-{}.. Creating.", accountId, typeCode, categoryCode);
        } else {
            // Update existing balance (equivalent to PERFORM 2700-B-UPDATE-TCATBAL-REC)
            BigDecimal newBalance = balanceRecord.getBalance().add(amount);
            balanceRecord.setBalance(newBalance);
            
            logger.debug("Updated TCATBAL record for key: {}-{}-{}, new balance: {}", 
                        accountId, typeCode, categoryCode, newBalance);
        }
    }

    /**
     * Formats current timestamp in DB2 format - equivalent to COBOL Z-GET-DB2-FORMAT-TIMESTAMP
     * Returns timestamp in format: YYYY-MM-DD-HH.MM.SS.NN0000
     */
    public String formatTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        
        // Format according to DB2 timestamp format used in COBOL
        // Format: YYYY-MM-DD-HH.MM.SS.NN0000
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS");
        String formatted = now.format(formatter) + "0000";
        
        logger.debug("DB2-TIMESTAMP = {}", formatted);
        return formatted;
    }

    /**
     * Returns current condition code - 0 for success, 4 for rejects
     */
    public int getConditionCode() {
        return conditionCode;
    }

    /**
     * Processes transaction file - main entry point that can be called from batch jobs
     * Returns processing results summary
     */
    public ProcessingResult processTransactionFile() {
        int result = postDailyTransactions();
        
        ProcessingResult processingResult = new ProcessingResult();
        processingResult.setConditionCode(result);
        processingResult.setTransactionCount(transactionCount);
        processingResult.setRejectCount(rejectCount);
        processingResult.setSuccessfulTransactions(transactionCount - rejectCount);
        
        return processingResult;
    }

    /**
     * Validates credit limit - equivalent to COBOL credit limit validation logic
     */
    public boolean validateCreditLimit(AccountRecord account, BigDecimal transactionAmount) {
        // Calculate temporary balance (equivalent to COBOL COMPUTE WS-TEMP-BAL)
        // Current balance = debits minus credits (charges minus payments)
        // New balance = current balance + transaction amount
        tempBalance = account.getCurrentCycleDebit()
                     .subtract(account.getCurrentCycleCredit())
                     .add(transactionAmount);

        // Check if credit limit is exceeded (equivalent to COBOL IF ACCT-CREDIT-LIMIT < WS-TEMP-BAL)
        if (account.getCreditLimit().compareTo(tempBalance) < 0) {
            validationFailReason = OVERLIMIT_TRANSACTION;
            validationFailDescription = "OVERLIMIT TRANSACTION";
            
            logger.warn("Credit limit exceeded for account {}: limit={}, new balance would be={}", 
                       account.getAccountId(), account.getCreditLimit(), tempBalance);
            return false;
        }

        return true;
    }

    /**
     * Validates expiration date - equivalent to COBOL expiration date validation
     */
    public boolean validateExpirationDate(AccountRecord account, String transactionTimestamp) {
        // Extract date portion from transaction timestamp (first 10 characters: YYYY-MM-DD)
        String transactionDate = transactionTimestamp.length() >= 10 ? 
                                transactionTimestamp.substring(0, 10) : transactionTimestamp;

        // Compare with account expiration date (equivalent to COBOL IF ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS)
        if (account.getExpirationDate().compareTo(transactionDate) < 0) {
            validationFailReason = TRANSACTION_AFTER_EXPIRATION;
            validationFailDescription = "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
            
            logger.warn("Transaction after expiration for account {}: expiration={}, transaction date={}", 
                       account.getAccountId(), account.getExpirationDate(), transactionDate);
            return false;
        }

        return true;
    }

    /**
     * Looks up account record - equivalent to COBOL 1500-B-LOOKUP-ACCT
     */
    public AccountRecord lookupAccount(long accountId) {
        for (AccountRecord account : accounts) {
            if (account.getAccountId() == accountId) {
                logger.debug("Account record found for ID: {}", accountId);
                return account;
            }
        }

        // Account not found (equivalent to COBOL INVALID KEY)
        validationFailReason = ACCOUNT_NOT_FOUND;
        validationFailDescription = "ACCOUNT RECORD NOT FOUND";
        
        logger.warn("Account record not found for ID: {}", accountId);
        return null;
    }

    /**
     * Looks up cross-reference record - equivalent to COBOL 1500-A-LOOKUP-XREF
     */
    public CrossReferenceRecord lookupCrossReference(String cardNumber) {
        for (CrossReferenceRecord xref : crossReferences) {
            if (cardNumber.equals(xref.getCardNumber())) {
                logger.debug("Cross-reference record found for card: {}", cardNumber);
                return xref;
            }
        }

        // Card number not found (equivalent to COBOL INVALID KEY)
        validationFailReason = INVALID_CARD_NUMBER;
        validationFailDescription = "INVALID CARD NUMBER FOUND";
        
        logger.warn("Cross-reference record not found for card: {}", cardNumber);
        return null;
    }

    /**
     * Posts transaction to transaction file - equivalent to COBOL 2900-WRITE-TRANSACTION-FILE
     */
    public void postTransaction(TransactionRecord transaction) {
        postedTransactions.add(transaction);
        logger.debug("Transaction posted: ID={}, Amount={}", 
                    transaction.getTransactionId(), transaction.getAmount());
    }

    /**
     * Writes reject record to reject file - equivalent to COBOL 2500-WRITE-REJECT-REC
     */
    public void writeRejectRecord(RejectRecord rejectRecord) {
        rejectedTransactions.add(rejectRecord);
        logger.warn("Transaction rejected: Reason={}, Description={}", 
                   rejectRecord.getValidationFailReason(), rejectRecord.getValidationFailDescription());
    }

    /**
     * Updates account record balances - equivalent to COBOL 2800-UPDATE-ACCOUNT-REC
     */
    private void updateAccountRecord(long accountId, BigDecimal transactionAmount) {
        AccountRecord account = null;
        for (AccountRecord acc : accounts) {
            if (acc.getAccountId() == accountId) {
                account = acc;
                break;
            }
        }

        if (account != null) {
            // Update current balance (equivalent to ADD DALYTRAN-AMT TO ACCT-CURR-BAL)
            BigDecimal newBalance = account.getCurrentBalance().add(transactionAmount);
            account.setCurrentBalance(newBalance);

            // Update cycle balances based on transaction amount sign
            if (transactionAmount.compareTo(BigDecimal.ZERO) >= 0) {
                // Credit transaction (equivalent to ADD DALYTRAN-AMT TO ACCT-CURR-CYC-CREDIT)
                BigDecimal newCredit = account.getCurrentCycleCredit().add(transactionAmount);
                account.setCurrentCycleCredit(newCredit);
            } else {
                // Debit transaction (equivalent to ADD DALYTRAN-AMT TO ACCT-CURR-CYC-DEBIT)
                BigDecimal newDebit = account.getCurrentCycleDebit().add(transactionAmount);
                account.setCurrentCycleDebit(newDebit);
            }

            logger.debug("Account {} updated: new balance={}, credit={}, debit={}", 
                        accountId, newBalance, account.getCurrentCycleCredit(), account.getCurrentCycleDebit());
        } else {
            validationFailReason = ACCOUNT_UPDATE_FAILED;
            validationFailDescription = "ACCOUNT RECORD NOT FOUND";
            logger.error("Failed to update account record for ID: {}", accountId);
        }
    }

    /**
     * Data structure for processing results
     */
    public static class ProcessingResult {
        private int conditionCode;
        private int transactionCount;
        private int rejectCount;
        private int successfulTransactions;

        public ProcessingResult() {}

        public int getConditionCode() { return conditionCode; }
        public void setConditionCode(int conditionCode) { this.conditionCode = conditionCode; }

        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

        public int getRejectCount() { return rejectCount; }
        public void setRejectCount(int rejectCount) { this.rejectCount = rejectCount; }

        public int getSuccessfulTransactions() { return successfulTransactions; }
        public void setSuccessfulTransactions(int successfulTransactions) { this.successfulTransactions = successfulTransactions; }
    }

    // Utility methods for testing and data setup

    /**
     * Adds a daily transaction for processing (for testing purposes)
     */
    public void addDailyTransaction(DailyTransactionRecord transaction) {
        dailyTransactions.add(transaction);
    }

    /**
     * Adds a cross-reference record (for testing purposes)
     */
    public void addCrossReference(CrossReferenceRecord xref) {
        crossReferences.add(xref);
    }

    /**
     * Adds an account record (for testing purposes)
     */
    public void addAccount(AccountRecord account) {
        accounts.add(account);
    }

    /**
     * Gets all posted transactions (for verification purposes)
     */
    public List<TransactionRecord> getPostedTransactions() {
        return new ArrayList<>(postedTransactions);
    }

    /**
     * Gets all rejected transactions (for verification purposes)
     */
    public List<RejectRecord> getRejectedTransactions() {
        return new ArrayList<>(rejectedTransactions);
    }

    /**
     * Gets current transaction count
     */
    public int getTransactionCount() {
        return transactionCount;
    }

    /**
     * Gets current reject count
     */
    public int getRejectCount() {
        return rejectCount;
    }

    /**
     * Clears all data (for testing purposes)
     */
    public void clearAllData() {
        dailyTransactions.clear();
        crossReferences.clear();
        accounts.clear();
        categoryBalances.clear();
        postedTransactions.clear();
        rejectedTransactions.clear();
        transactionCount = 0;
        rejectCount = 0;
        conditionCode = SUCCESS_CODE;
    }
}
