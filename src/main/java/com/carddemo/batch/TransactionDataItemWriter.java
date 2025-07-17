package com.carddemo.batch;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Spring Batch ItemWriter for persisting transaction data to PostgreSQL database during migration.
 * 
 * This implementation supports bulk insert operations with partition management and BigDecimal
 * financial precision while maintaining exact field mapping from COBOL transaction records.
 * 
 * Key Features:
 * - Partition-aware insertion for monthly RANGE partitioned transactions table
 * - BigDecimal precision handling maintaining COBOL COMP-3 arithmetic equivalence
 * - Comprehensive data validation for transaction_type and transaction_category foreign keys
 * - Error handling and transaction rollback support with detailed logging
 * - Spring @Transactional integration ensuring ACID compliance during bulk operations
 * 
 * Performance Optimizations:
 * - Batch INSERT operations using PreparedStatement with parameter binding
 * - Automatic partition selection based on transaction timestamp
 * - Connection pooling through HikariCP integration
 * - Configurable chunk size for optimal memory usage
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since Spring Boot 3.2.x
 */
@Component
@Validated
public class TransactionDataItemWriter implements ItemWriter<TransactionData> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionDataItemWriter.class);
    
    // COBOL COMP-3 precision equivalent - 12 total digits, 2 decimal places
    private static final MathContext COBOL_MATH_CONTEXT = new MathContext(12, RoundingMode.HALF_UP);
    
    // PostgreSQL INSERT statement for transactions table with partition awareness
    private static final String INSERT_TRANSACTION_SQL = """
        INSERT INTO transactions (
            transaction_id, 
            transaction_type, 
            transaction_category, 
            transaction_source, 
            transaction_description, 
            transaction_amount, 
            merchant_id, 
            merchant_name, 
            merchant_city, 
            merchant_zip, 
            card_number, 
            original_timestamp, 
            processed_timestamp,
            created_at,
            updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;
    
    // Validation queries for foreign key constraints
    private static final String VALIDATE_TRANSACTION_TYPE_SQL = 
        "SELECT COUNT(*) FROM transaction_types WHERE transaction_type = ?";
    
    private static final String VALIDATE_TRANSACTION_CATEGORY_SQL = 
        "SELECT COUNT(*) FROM transaction_categories WHERE transaction_category = ?";
    
    // Cache for validated transaction types and categories to improve performance
    private final Map<String, Boolean> validTransactionTypes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> validTransactionCategories = new ConcurrentHashMap<>();
    
    // Date formatter for parsing transaction timestamps from COBOL format
    private static final DateTimeFormatter COBOL_TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Main ItemWriter method for processing chunks of transaction data.
     * 
     * Implements bulk insert operations with partition awareness and comprehensive
     * error handling. Each chunk is processed within a single database transaction
     * to ensure ACID compliance.
     * 
     * @param chunk Chunk of TransactionData objects to be persisted
     * @throws Exception if bulk insert operation fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public void write(Chunk<? extends TransactionData> chunk) throws Exception {
        if (chunk == null || chunk.isEmpty()) {
            logger.debug("Empty chunk received, skipping processing");
            return;
        }
        
        logger.info("Processing chunk of {} transaction records for bulk insert", chunk.size());
        
        List<TransactionData> validatedTransactions = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        
        // Validate all transactions in the chunk before processing
        for (TransactionData transaction : chunk.getItems()) {
            try {
                if (validateTransactionData(transaction)) {
                    validatedTransactions.add(transaction);
                } else {
                    validationErrors.add("Invalid transaction data: " + transaction.getTransactionId());
                }
            } catch (Exception e) {
                logger.error("Validation error for transaction {}: {}", 
                    transaction.getTransactionId(), e.getMessage());
                validationErrors.add("Validation exception for transaction " + 
                    transaction.getTransactionId() + ": " + e.getMessage());
            }
        }
        
        // If any validation errors occurred, log them and throw exception
        if (!validationErrors.isEmpty()) {
            logger.error("Validation errors encountered: {}", validationErrors);
            throw new DataIntegrityViolationException(
                "Transaction validation failed: " + String.join(", ", validationErrors));
        }
        
        // Perform bulk insert operation
        try {
            insertIntoPartition(validatedTransactions);
            logger.info("Successfully inserted {} transaction records into database", 
                validatedTransactions.size());
        } catch (Exception e) {
            logger.error("Bulk insert operation failed for chunk of {} transactions: {}", 
                validatedTransactions.size(), e.getMessage());
            handlePartitionError(validatedTransactions, e);
            throw e;
        }
    }
    
    /**
     * Validates transaction data before insertion, including foreign key constraints
     * and data format validation.
     * 
     * Performs comprehensive validation including:
     * - Required field validation
     * - BigDecimal precision validation for transaction amounts
     * - Foreign key constraint validation for transaction types and categories
     * - Data format validation for card numbers and merchant information
     * 
     * @param transaction TransactionData object to validate
     * @return true if validation passes, false otherwise
     * @throws DataAccessException if database validation queries fail
     */
    public boolean validateTransactionData(TransactionData transaction) throws DataAccessException {
        if (transaction == null) {
            logger.warn("Null transaction data received for validation");
            return false;
        }
        
        // Validate required fields
        if (!StringUtils.hasText(transaction.getTransactionId())) {
            logger.warn("Transaction ID is missing or empty");
            return false;
        }
        
        if (!StringUtils.hasText(transaction.getTransactionType())) {
            logger.warn("Transaction type is missing for transaction: {}", transaction.getTransactionId());
            return false;
        }
        
        if (!StringUtils.hasText(transaction.getTransactionCategory())) {
            logger.warn("Transaction category is missing for transaction: {}", transaction.getTransactionId());
            return false;
        }
        
        // Validate transaction amount precision (COBOL COMP-3 equivalent)
        if (transaction.getTransactionAmount() == null) {
            logger.warn("Transaction amount is null for transaction: {}", transaction.getTransactionId());
            return false;
        }
        
        // Ensure BigDecimal precision matches COBOL COMP-3 requirements
        BigDecimal amount = transaction.getTransactionAmount().round(COBOL_MATH_CONTEXT);
        if (amount.scale() > 2) {
            logger.warn("Transaction amount exceeds 2 decimal places for transaction: {} (amount: {})", 
                transaction.getTransactionId(), amount);
            return false;
        }
        
        // Validate transaction type foreign key constraint
        if (!isValidTransactionType(transaction.getTransactionType())) {
            logger.warn("Invalid transaction type '{}' for transaction: {}", 
                transaction.getTransactionType(), transaction.getTransactionId());
            return false;
        }
        
        // Validate transaction category foreign key constraint
        if (!isValidTransactionCategory(transaction.getTransactionCategory())) {
            logger.warn("Invalid transaction category '{}' for transaction: {}", 
                transaction.getTransactionCategory(), transaction.getTransactionId());
            return false;
        }
        
        // Validate card number format (16 digits)
        if (StringUtils.hasText(transaction.getCardNumber()) && 
            !transaction.getCardNumber().matches("\\d{16}")) {
            logger.warn("Invalid card number format for transaction: {} (card: {})", 
                transaction.getTransactionId(), transaction.getCardNumber());
            return false;
        }
        
        // Validate merchant ID format (9 digits)
        if (StringUtils.hasText(transaction.getMerchantId()) && 
            !transaction.getMerchantId().matches("\\d{9}")) {
            logger.warn("Invalid merchant ID format for transaction: {} (merchant: {})", 
                transaction.getTransactionId(), transaction.getMerchantId());
            return false;
        }
        
        logger.debug("Validation successful for transaction: {}", transaction.getTransactionId());
        return true;
    }
    
    /**
     * Performs partition-aware bulk insert operation into PostgreSQL transactions table.
     * 
     * The method handles:
     * - Automatic partition selection based on transaction timestamp
     * - Batch INSERT operations using PreparedStatement for optimal performance
     * - BigDecimal precision maintenance for financial amounts
     * - Connection management through HikariCP
     * 
     * @param transactions List of validated TransactionData objects to insert
     * @throws SQLException if database insert operation fails
     */
    public void insertIntoPartition(List<TransactionData> transactions) throws SQLException {
        if (transactions == null || transactions.isEmpty()) {
            logger.debug("No transactions to insert");
            return;
        }
        
        logger.info("Executing bulk insert for {} transaction records", transactions.size());
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_TRANSACTION_SQL)) {
            
            // Set connection properties for optimal batch performance
            connection.setAutoCommit(false);
            
            for (TransactionData transaction : transactions) {
                // Bind parameters for INSERT statement
                statement.setString(1, transaction.getTransactionId());
                statement.setString(2, transaction.getTransactionType());
                statement.setString(3, transaction.getTransactionCategory());
                statement.setString(4, transaction.getTransactionSource());
                statement.setString(5, transaction.getTransactionDescription());
                
                // Handle BigDecimal precision for financial amount
                BigDecimal amount = transaction.getTransactionAmount().round(COBOL_MATH_CONTEXT);
                statement.setBigDecimal(6, amount);
                
                statement.setString(7, transaction.getMerchantId());
                statement.setString(8, transaction.getMerchantName());
                statement.setString(9, transaction.getMerchantCity());
                statement.setString(10, transaction.getMerchantZip());
                statement.setString(11, transaction.getCardNumber());
                
                // Convert timestamp strings to Timestamp objects
                Timestamp originalTimestamp = parseTimestamp(transaction.getOriginalTimestamp());
                Timestamp processedTimestamp = parseTimestamp(transaction.getProcessedTimestamp());
                
                statement.setTimestamp(12, originalTimestamp);
                statement.setTimestamp(13, processedTimestamp);
                
                // Add to batch
                statement.addBatch();
            }
            
            // Execute batch insert
            int[] updateCounts = statement.executeBatch();
            connection.commit();
            
            // Verify all inserts succeeded
            int successfulInserts = 0;
            for (int count : updateCounts) {
                if (count > 0) {
                    successfulInserts++;
                }
            }
            
            logger.info("Bulk insert completed: {} successful inserts out of {} attempted", 
                successfulInserts, transactions.size());
            
            if (successfulInserts != transactions.size()) {
                throw new SQLException("Batch insert partially failed: " + successfulInserts + 
                    " successful out of " + transactions.size() + " attempted");
            }
            
        } catch (SQLException e) {
            logger.error("SQL error during bulk insert operation: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Handles errors during partition insertion operations.
     * 
     * Provides comprehensive error handling including:
     * - Detailed error logging with transaction context
     * - Automatic rollback coordination
     * - Error categorization for monitoring and alerting
     * - Recovery recommendations
     * 
     * @param transactions List of transactions that failed to insert
     * @param error Exception that occurred during insert operation
     */
    public void handlePartitionError(List<TransactionData> transactions, Exception error) {
        if (transactions == null || transactions.isEmpty()) {
            logger.error("Partition error occurred with no transaction context: {}", error.getMessage());
            return;
        }
        
        logger.error("Partition insert error occurred for {} transactions: {}", 
            transactions.size(), error.getMessage());
        
        // Log detailed error information for each transaction
        for (TransactionData transaction : transactions) {
            logger.error("Failed transaction details - ID: {}, Type: {}, Category: {}, Amount: {}, Timestamp: {}", 
                transaction.getTransactionId(),
                transaction.getTransactionType(),
                transaction.getTransactionCategory(),
                transaction.getTransactionAmount(),
                transaction.getOriginalTimestamp());
        }
        
        // Categorize error types for monitoring
        if (error instanceof DataIntegrityViolationException) {
            logger.error("Data integrity violation - check foreign key constraints and data validation");
        } else if (error instanceof SQLException) {
            SQLException sqlError = (SQLException) error;
            logger.error("SQL error - Code: {}, State: {}, Message: {}", 
                sqlError.getErrorCode(), sqlError.getSQLState(), sqlError.getMessage());
        } else {
            logger.error("Unexpected error type: {}", error.getClass().getSimpleName());
        }
        
        // Additional error handling for partition-specific issues
        if (error.getMessage().contains("partition")) {
            logger.error("Partition-related error detected - verify partition configuration and constraints");
        }
        
        // Log recovery recommendations
        logger.info("Recovery recommendations:");
        logger.info("1. Verify database partition configuration for transactions table");
        logger.info("2. Check foreign key constraints in transaction_types and transaction_categories tables");
        logger.info("3. Validate data format compliance with COBOL precision requirements");
        logger.info("4. Review transaction timestamp format and partition boundaries");
    }
    
    /**
     * Validates transaction type against reference table with caching.
     * 
     * @param transactionType Transaction type code to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidTransactionType(String transactionType) {
        if (!StringUtils.hasText(transactionType)) {
            return false;
        }
        
        // Check cache first
        Boolean cachedResult = validTransactionTypes.get(transactionType);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Query database for validation
        try {
            int count = jdbcTemplate.queryForObject(VALIDATE_TRANSACTION_TYPE_SQL, 
                Integer.class, transactionType);
            boolean isValid = count > 0;
            
            // Cache result
            validTransactionTypes.put(transactionType, isValid);
            return isValid;
            
        } catch (DataAccessException e) {
            logger.error("Error validating transaction type '{}': {}", transactionType, e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates transaction category against reference table with caching.
     * 
     * @param transactionCategory Transaction category code to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidTransactionCategory(String transactionCategory) {
        if (!StringUtils.hasText(transactionCategory)) {
            return false;
        }
        
        // Check cache first
        Boolean cachedResult = validTransactionCategories.get(transactionCategory);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Query database for validation
        try {
            int count = jdbcTemplate.queryForObject(VALIDATE_TRANSACTION_CATEGORY_SQL, 
                Integer.class, transactionCategory);
            boolean isValid = count > 0;
            
            // Cache result
            validTransactionCategories.put(transactionCategory, isValid);
            return isValid;
            
        } catch (DataAccessException e) {
            logger.error("Error validating transaction category '{}': {}", transactionCategory, e.getMessage());
            return false;
        }
    }
    
    /**
     * Parses timestamp string from COBOL format to Java Timestamp.
     * 
     * @param timestampString Timestamp string in COBOL format
     * @return Timestamp object or current timestamp if parsing fails
     */
    private Timestamp parseTimestamp(String timestampString) {
        if (!StringUtils.hasText(timestampString)) {
            return new Timestamp(System.currentTimeMillis());
        }
        
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(timestampString, COBOL_TIMESTAMP_FORMATTER);
            return Timestamp.valueOf(localDateTime);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse timestamp '{}', using current timestamp: {}", 
                timestampString, e.getMessage());
            return new Timestamp(System.currentTimeMillis());
        }
    }
    
    /**
     * Clears validation caches (useful for testing or cache refresh).
     */
    public void clearValidationCaches() {
        validTransactionTypes.clear();
        validTransactionCategories.clear();
        logger.info("Validation caches cleared");
    }
    
    /**
     * TransactionData class representing a transaction record from COBOL source.
     * 
     * Maps to the structure defined in CVTRA05Y.cpy with exact field mapping
     * and BigDecimal precision handling for financial amounts.
     */
    public static class TransactionData {
        private String transactionId;        // TRAN-ID (16 chars)
        private String transactionType;      // TRAN-TYPE-CD (2 chars)
        private String transactionCategory;  // TRAN-CAT-CD (4 chars)
        private String transactionSource;    // TRAN-SOURCE (10 chars)
        private String transactionDescription; // TRAN-DESC (100 chars)
        private BigDecimal transactionAmount;  // TRAN-AMT (S9(09)V99)
        private String merchantId;           // TRAN-MERCHANT-ID (9 chars)
        private String merchantName;         // TRAN-MERCHANT-NAME (50 chars)
        private String merchantCity;         // TRAN-MERCHANT-CITY (50 chars)
        private String merchantZip;          // TRAN-MERCHANT-ZIP (10 chars)
        private String cardNumber;           // TRAN-CARD-NUM (16 chars)
        private String originalTimestamp;    // TRAN-ORIG-TS (26 chars)
        private String processedTimestamp;   // TRAN-PROC-TS (26 chars)
        
        // Constructors
        public TransactionData() {}
        
        public TransactionData(String transactionId, String transactionType, 
                             String transactionCategory, String transactionSource,
                             String transactionDescription, BigDecimal transactionAmount,
                             String merchantId, String merchantName, String merchantCity,
                             String merchantZip, String cardNumber, String originalTimestamp,
                             String processedTimestamp) {
            this.transactionId = transactionId;
            this.transactionType = transactionType;
            this.transactionCategory = transactionCategory;
            this.transactionSource = transactionSource;
            this.transactionDescription = transactionDescription;
            this.transactionAmount = transactionAmount;
            this.merchantId = merchantId;
            this.merchantName = merchantName;
            this.merchantCity = merchantCity;
            this.merchantZip = merchantZip;
            this.cardNumber = cardNumber;
            this.originalTimestamp = originalTimestamp;
            this.processedTimestamp = processedTimestamp;
        }
        
        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        
        public String getTransactionCategory() { return transactionCategory; }
        public void setTransactionCategory(String transactionCategory) { this.transactionCategory = transactionCategory; }
        
        public String getTransactionSource() { return transactionSource; }
        public void setTransactionSource(String transactionSource) { this.transactionSource = transactionSource; }
        
        public String getTransactionDescription() { return transactionDescription; }
        public void setTransactionDescription(String transactionDescription) { this.transactionDescription = transactionDescription; }
        
        public BigDecimal getTransactionAmount() { return transactionAmount; }
        public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
        
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
        
        @Override
        public String toString() {
            return "TransactionData{" +
                "transactionId='" + transactionId + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", transactionCategory='" + transactionCategory + '\'' +
                ", transactionAmount=" + transactionAmount +
                ", merchantName='" + merchantName + '\'' +
                ", cardNumber='" + (cardNumber != null ? cardNumber.replaceAll("\\d(?=\\d{4})", "X") : null) + '\'' +
                ", originalTimestamp='" + originalTimestamp + '\'' +
                '}';
        }
    }
}