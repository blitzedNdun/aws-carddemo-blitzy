package com.carddemo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TransactionReportService implements COBOL CBTRN03C transaction detail report generation functionality.
 * This service handles date parameter processing, transaction file reading with date filtering,
 * cross-reference lookups for data enrichment, and formatted report output with totals calculation.
 * 
 * Converted from COBOL program CBTRN03C.cbl maintaining identical business logic and processing flow.
 * Key features:
 * - Date parameter file processing (DATEPARM equivalent)
 * - Sequential transaction file reading with date range filtering  
 * - Cross-reference lookups for card/account, transaction type, and category data
 * - Report formatting with headers, detail lines, page breaks
 * - Page-level and grand total accumulation
 * - Comprehensive error handling for I/O operations
 */
@Service
@Transactional
public class TransactionReportService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionReportService.class);
    
    // Report formatting constants matching COBOL working storage variables
    private static final int PAGE_SIZE = 20;
    private static final int LINE_LENGTH = 133;
    private static final String BLANK_LINE = " ".repeat(LINE_LENGTH);
    
    // Date format matching COBOL TRAN-PROC-TS format (YYYY-MM-DD)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Working storage variables equivalent to COBOL WS-REPORT-VARS
    private boolean firstTime;
    private int lineCounter;
    private BigDecimal pageTotal;
    private BigDecimal accountTotal;
    private BigDecimal grandTotal;
    private String currentCardNumber;
    
    // Date parameters equivalent to WS-DATEPARM-RECORD
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Report output buffer
    private List<String> reportLines;
    
    // Cross-reference data structures
    private Map<String, CardXrefData> cardXrefMap;
    private Map<String, TransactionTypeData> transactionTypeMap;
    private Map<String, TransactionCategoryData> transactionCategoryMap;

    /**
     * Main entry point for transaction detail report generation.
     * Orchestrates the complete report generation process matching COBOL main procedure division flow.
     * 
     * @param dateParameterFile Path to date parameter file (DATEPARM equivalent)
     * @param transactionFile Path to transaction data file (TRANFILE equivalent)
     * @param cardXrefFile Path to card cross-reference file (CARDXREF equivalent)
     * @param transactionTypeFile Path to transaction type file (TRANTYPE equivalent)
     * @param transactionCategoryFile Path to transaction category file (TRANCATG equivalent)
     * @return List of formatted report lines
     * @throws IOException If file I/O operations fail
     * @throws IllegalArgumentException If date parameters are invalid
     */
    public List<String> generateTransactionDetailReport(
            String dateParameterFile,
            String transactionFile, 
            String cardXrefFile,
            String transactionTypeFile,
            String transactionCategoryFile) throws IOException {
        
        logger.info("START OF EXECUTION OF TRANSACTION REPORT SERVICE");
        
        try {
            // Initialize working storage variables (equivalent to COBOL 0000-init)
            initializeWorkingStorage();
            
            // Process date parameters (equivalent to COBOL 0550-DATEPARM-READ)
            processDateParameters(dateParameterFile);
            
            // Load cross-reference data files
            loadCardXrefData(cardXrefFile);
            loadTransactionTypeData(transactionTypeFile);
            loadTransactionCategoryData(transactionCategoryFile);
            
            // Process transaction file with filtering and report generation
            processTransactionFile(transactionFile);
            
            // Write final totals
            if (!firstTime) {
                writeAccountTotals();
                writeGrandTotals();
            }
            
            logger.info("Transaction report generation completed successfully");
            logger.info("Total report lines generated: {}", reportLines.size());
            logger.info("Grand total amount: {}", grandTotal);
            
            return new ArrayList<>(reportLines);
            
        } catch (Exception e) {
            logger.error("ERROR DURING TRANSACTION REPORT GENERATION", e);
            throw new RuntimeException("Transaction report generation failed: " + e.getMessage(), e);
        } finally {
            logger.info("END OF EXECUTION OF TRANSACTION REPORT SERVICE");
        }
    }
    /**
     * Processes date parameter file to extract start and end dates for filtering.
     * Equivalent to COBOL 0550-DATEPARM-READ paragraph.
     * 
     * @param dateParameterFile Path to date parameter file
     * @throws IOException If file reading fails
     * @throws IllegalArgumentException If date format is invalid
     */
    public void processDateParameters(String dateParameterFile) throws IOException {
        logger.debug("Processing date parameters from file: {}", dateParameterFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(dateParameterFile))) {
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty()) {
                throw new IllegalArgumentException("Date parameter file is empty");
            }
            
            // Parse date parameter record matching COBOL WS-DATEPARM-RECORD format
            if (line.length() < 21) {
                throw new IllegalArgumentException("Invalid date parameter record length: " + line.length());
            }
            
            String startDateStr = line.substring(0, 10).trim();
            String endDateStr = line.substring(11, 21).trim();
            
            try {
                startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                
                if (startDate.isAfter(endDate)) {
                    throw new IllegalArgumentException("Start date cannot be after end date");
                }
                
                logger.info("Reporting from {} to {}", startDate, endDate);
                
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format in parameter file: " + e.getMessage(), e);
            }
            
        } catch (IOException e) {
            logger.error("ERROR READING DATE PARAMETER FILE: {}", e.getMessage());
            throw new IOException("Failed to read date parameter file: " + dateParameterFile, e);
        }
    }

    /**
     * Filters transaction records by date range using start and end dates.
     * Equivalent to COBOL date range filtering logic in main processing loop.
     * 
     * @param transactions List of transaction records to filter
     * @return Filtered list containing only transactions within date range
     */
    public List<TransactionRecord> filterTransactionsByDateRange(List<TransactionRecord> transactions) {
        if (startDate == null || endDate == null) {
            throw new IllegalStateException("Date parameters must be processed before filtering transactions");
        }
        
        logger.debug("Filtering {} transactions by date range {} to {}", 
                     transactions.size(), startDate, endDate);
        
        List<TransactionRecord> filtered = transactions.stream()
            .filter(transaction -> {
                try {
                    // Extract date from TRAN-PROC-TS (first 10 characters: YYYY-MM-DD)
                    String transactionDateStr = transaction.getTransactionProcessTimestamp().substring(0, 10);
                    LocalDate transactionDate = LocalDate.parse(transactionDateStr, DATE_FORMATTER);
                    
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                    
                } catch (Exception e) {
                    logger.warn("Invalid transaction date format, excluding record: {}", 
                                transaction.getTransactionId(), e);
                    return false;
                }
            })
            .collect(Collectors.toList());
        
        logger.debug("Filtered to {} transactions within date range", filtered.size());
        return filtered;
    }

    /**
     * Enriches transaction data with cross-reference information.
     * Performs lookups equivalent to COBOL 1500-A/B/C-LOOKUP paragraphs.
     * 
     * @param transaction Transaction record to enrich
     * @return Enriched transaction data
     * @throws IllegalArgumentException If required cross-reference data is not found
     */
    public EnrichedTransactionData enrichTransactionData(TransactionRecord transaction) {
        EnrichedTransactionData enriched = new EnrichedTransactionData();
        enriched.setTransaction(transaction);
        
        // Cross-reference lookup for card/account information (equivalent to 1500-A-LOOKUP-XREF)
        CardXrefData cardXref = cardXrefMap.get(transaction.getCardNumber());
        if (cardXref == null) {
            String errorMsg = "INVALID CARD NUMBER: " + transaction.getCardNumber();
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        enriched.setCardXref(cardXref);
        
        // Transaction type lookup (equivalent to 1500-B-LOOKUP-TRANTYPE)
        TransactionTypeData transactionType = transactionTypeMap.get(transaction.getTransactionTypeCode());
        if (transactionType == null) {
            String errorMsg = "INVALID TRANSACTION TYPE: " + transaction.getTransactionTypeCode();
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
    /**
     * Calculates and accumulates page-level totals.
     * Equivalent to COBOL WS-PAGE-TOTAL accumulation logic.
     * 
     * @param transactionAmount Amount to add to page total
     * @return Current page total after addition
     */
    public BigDecimal calculatePageTotals(BigDecimal transactionAmount) {
        if (transactionAmount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }
        
        pageTotal = pageTotal.add(transactionAmount);
        logger.debug("Added {} to page total, new page total: {}", transactionAmount, pageTotal);
        
        return pageTotal;
    }

    /**
     * Calculates and accumulates grand totals.
     * Equivalent to COBOL WS-GRAND-TOTAL accumulation logic.
     * 
     * @param pageAmount Page total amount to add to grand total
     * @return Current grand total after addition
     */
    public BigDecimal calculateGrandTotals(BigDecimal pageAmount) {
        if (pageAmount == null) {
            throw new IllegalArgumentException("Page amount cannot be null");
        }
        
        grandTotal = grandTotal.add(pageAmount);
        logger.debug("Added {} to grand total, new grand total: {}", pageAmount, grandTotal);
        
        return grandTotal;
    }

    /**
     * Formats and creates report header lines.
     * Equivalent to COBOL 1120-WRITE-HEADERS paragraph.
     * 
     * @return List of formatted header lines
     */
    public List<String> formatReportHeader() {
        List<String> headers = new ArrayList<>();
        
        // Report title header equivalent to REPORT-NAME-HEADER
        String titleHeader = String.format("%-40s%30s%30s", 
                                          "TRANSACTION DETAIL REPORT",
                                          "FROM: " + startDate.toString(),
                                          "TO: " + endDate.toString());
        headers.add(titleHeader);
        
        // Blank line
        headers.add(BLANK_LINE);
        
        // Column header line 1 equivalent to TRANSACTION-HEADER-1
        String columnHeader1 = String.format("%-16s %-10s %-6s %-50s %-10s %-15s %-10s",
                                            "TRANSACTION ID",
                                            "ACCOUNT ID", 
                                            "TYPE",
                                            "TYPE DESCRIPTION",
                                            "CATEGORY",
                                            "SOURCE",
                                            "AMOUNT");
        headers.add(columnHeader1);
        
        // Column header line 2 equivalent to TRANSACTION-HEADER-2 (separator line)
        String columnHeader2 = String.format("%-16s %-10s %-6s %-50s %-10s %-15s %-10s",
                                            "----------------",
                                            "----------",
                                            "------",
                                            "--------------------------------------------------",
                                            "----------",
                                            "---------------",
                                            "----------");
        headers.add(columnHeader2);
        
        logger.debug("Generated {} header lines", headers.size());
        return headers;
    }

    /**
     * Formats individual transaction detail line for report output.
     * Equivalent to COBOL 1120-WRITE-DETAIL paragraph.
     * 
     * @param enrichedData Enriched transaction data to format
     * @return Formatted detail line string
     */
    public String formatReportDetail(EnrichedTransactionData enrichedData) {
        if (enrichedData == null || enrichedData.getTransaction() == null) {
            throw new IllegalArgumentException("Enriched transaction data cannot be null");
        }
        
        TransactionRecord transaction = enrichedData.getTransaction();
        
        // Format detail line matching COBOL TRANSACTION-DETAIL-REPORT layout
        String detailLine = String.format("%-16s %-10s %-6s %-50s %-10s %-15s %10.2f",
                                         transaction.getTransactionId(),
                                         enrichedData.getCardXref().getAccountId(),
                                         transaction.getTransactionTypeCode(),
                                         enrichedData.getTransactionType().getDescription(),
                                         String.format("%04d", transaction.getTransactionCategoryCode()),
                                         transaction.getTransactionSource(),
                                         transaction.getTransactionAmount());
        
        logger.debug("Formatted detail line for transaction: {}", transaction.getTransactionId());
        return detailLine;
    }

    /**
     * Handles page break logic and header insertion.
     * Equivalent to COBOL page break checking and header writing logic.
     * 
     * @return True if page break was handled, false otherwise
     */
    public boolean handlePageBreak() {
        // Check if page break is needed (equivalent to COBOL MOD function check)
    // Private helper methods for internal operations
    
    /**
     * Initializes working storage variables equivalent to COBOL initialization.
     */
    private void initializeWorkingStorage() {
        firstTime = true;
        lineCounter = 0;
        pageTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        accountTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        grandTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        currentCardNumber = "";
        reportLines = new ArrayList<>();
        cardXrefMap = new HashMap<>();
        transactionTypeMap = new HashMap<>();
        transactionCategoryMap = new HashMap<>();
        
        logger.debug("Working storage variables initialized");
    }
    
    /**
     * Processes the main transaction file with filtering and report generation.
     * Equivalent to COBOL main processing loop.
     */
    private void processTransactionFile(String transactionFile) throws IOException {
        logger.debug("Processing transaction file: {}", transactionFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(transactionFile))) {
            String line;
            int totalRecords = 0;
            int processedRecords = 0;
            
            while ((line = reader.readLine()) != null) {
                totalRecords++;
                
                try {
                    TransactionRecord transaction = parseTransactionRecord(line);
                    
                    // Date range filtering equivalent to COBOL IF statement
                    String transactionDateStr = transaction.getTransactionProcessTimestamp().substring(0, 10);
                    LocalDate transactionDate = LocalDate.parse(transactionDateStr, DATE_FORMATTER);
                    
                    if (transactionDate.isBefore(startDate) || transactionDate.isAfter(endDate)) {
                        continue; // Skip records outside date range
                    }
                    
                    // Check for card number change (equivalent to COBOL card break logic)
                    if (!currentCardNumber.equals(transaction.getCardNumber())) {
                        if (!firstTime) {
                            writeAccountTotals();
                        }
                        currentCardNumber = transaction.getCardNumber();
                    }
                    
                    // Enrich transaction data with cross-references
                    EnrichedTransactionData enrichedData = enrichTransactionData(transaction);
                    
                    // Write transaction report line
                    writeTransactionReport(enrichedData);
                    processedRecords++;
                    
                } catch (Exception e) {
                    logger.warn("Error processing transaction record {}: {}", totalRecords, e.getMessage());
                }
            }
            
            logger.info("Processed {} of {} total transaction records", processedRecords, totalRecords);
            
        } catch (IOException e) {
            logger.error("ERROR READING TRANSACTION FILE: {}", e.getMessage());
            throw new IOException("Failed to read transaction file: " + transactionFile, e);
        }
    }
    
    /**
     * Writes transaction report line with totals accumulation.
     * Equivalent to COBOL 1100-WRITE-TRANSACTION-REPORT paragraph.
     */
    private void writeTransactionReport(EnrichedTransactionData enrichedData) {
        // Write headers on first time or after page break
        if (firstTime) {
            firstTime = false;
            List<String> headers = formatReportHeader();
            reportLines.addAll(headers);
            lineCounter += headers.size();
        }
        
        // Check for page break
        handlePageBreak();
        
        // Accumulate totals
        BigDecimal transactionAmount = enrichedData.getTransaction().getTransactionAmount();
        pageTotal = pageTotal.add(transactionAmount);
        accountTotal = accountTotal.add(transactionAmount);
        
        // Write detail line
        String detailLine = formatReportDetail(enrichedData);
        reportLines.add(detailLine);
        lineCounter++;
        
        logger.debug("Transaction report line written for transaction: {}", 
                     enrichedData.getTransaction().getTransactionId());
    }
    
    /**
     * Writes page totals line equivalent to COBOL 1110-WRITE-PAGE-TOTALS.
     */
    private void writePageTotals() {
        String pageTotalLine = String.format("%90s %10.2f", "PAGE TOTAL:", pageTotal);
        reportLines.add(pageTotalLine);
        
        grandTotal = grandTotal.add(pageTotal);
        pageTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        lineCounter++;
        
        // Add separator line
        reportLines.add(BLANK_LINE);
        lineCounter++;
        
        logger.debug("Page totals written: {}", pageTotal);
    }
    
    /**
     * Writes account totals line equivalent to COBOL 1120-WRITE-ACCOUNT-TOTALS.
     */
    private void writeAccountTotals() {
        String accountTotalLine = String.format("%90s %10.2f", "ACCOUNT TOTAL:", accountTotal);
    /**
     * Loads card cross-reference data from file.
     * Equivalent to COBOL CARDXREF file processing.
     */
    private void loadCardXrefData(String cardXrefFile) throws IOException {
        logger.debug("Loading card cross-reference data from: {}", cardXrefFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(cardXrefFile))) {
            String line;
            int recordCount = 0;
            
            while ((line = reader.readLine()) != null) {
                try {
                    CardXrefData cardXref = parseCardXrefRecord(line);
                    cardXrefMap.put(cardXref.getCardNumber(), cardXref);
                    recordCount++;
                } catch (Exception e) {
                    logger.warn("Error parsing card xref record: {}", e.getMessage());
                }
            }
            
            logger.info("Loaded {} card cross-reference records", recordCount);
            
        } catch (IOException e) {
            logger.error("ERROR LOADING CARD XREF FILE: {}", e.getMessage());
            throw new IOException("Failed to load card cross-reference file: " + cardXrefFile, e);
        }
    }
    
    /**
     * Loads transaction type data from file.
     * Equivalent to COBOL TRANTYPE file processing.
     */
    private void loadTransactionTypeData(String transactionTypeFile) throws IOException {
        logger.debug("Loading transaction type data from: {}", transactionTypeFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(transactionTypeFile))) {
            String line;
            int recordCount = 0;
            
            while ((line = reader.readLine()) != null) {
                try {
                    TransactionTypeData transactionType = parseTransactionTypeRecord(line);
                    transactionTypeMap.put(transactionType.getTypeCode(), transactionType);
                    recordCount++;
                } catch (Exception e) {
                    logger.warn("Error parsing transaction type record: {}", e.getMessage());
                }
            }
            
            logger.info("Loaded {} transaction type records", recordCount);
            
        } catch (IOException e) {
            logger.error("ERROR LOADING TRANSACTION TYPE FILE: {}", e.getMessage());
            throw new IOException("Failed to load transaction type file: " + transactionTypeFile, e);
        }
    }
    
    /**
     * Loads transaction category data from file.
     * Equivalent to COBOL TRANCATG file processing.
     */
    private void loadTransactionCategoryData(String transactionCategoryFile) throws IOException {
        logger.debug("Loading transaction category data from: {}", transactionCategoryFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(transactionCategoryFile))) {
            String line;
            int recordCount = 0;
            
            while ((line = reader.readLine()) != null) {
                try {
                    TransactionCategoryData transactionCategory = parseTransactionCategoryRecord(line);
                    String key = transactionCategory.getTypeCode() + 
                               String.format("%04d", transactionCategory.getCategoryCode());
                    transactionCategoryMap.put(key, transactionCategory);
                    recordCount++;
                } catch (Exception e) {
                    logger.warn("Error parsing transaction category record: {}", e.getMessage());
                }
            }
            
            logger.info("Loaded {} transaction category records", recordCount);
            
        } catch (IOException e) {
            logger.error("ERROR LOADING TRANSACTION CATEGORY FILE: {}", e.getMessage());
            throw new IOException("Failed to load transaction category file: " + transactionCategoryFile, e);
        }
    }
    
    /**
     * Parses card cross-reference record from file line.
     */
    private CardXrefData parseCardXrefRecord(String line) {
        if (line.length() < 50) {
            throw new IllegalArgumentException("Invalid card xref record length: " + line.length());
        }
        
        CardXrefData cardXref = new CardXrefData();
        cardXref.setCardNumber(line.substring(0, 16).trim());
        cardXref.setAccountId(line.substring(16, 32).trim());
        cardXref.setCustomerId(line.substring(32, 48).trim());
        
        return cardXref;
    }
    
    /**
     * Parses transaction type record from file line.
     */
    private TransactionTypeData parseTransactionTypeRecord(String line) {
        if (line.length() < 60) {
            throw new IllegalArgumentException("Invalid transaction type record length: " + line.length());
        }
        
        TransactionTypeData transactionType = new TransactionTypeData();
        transactionType.setTypeCode(line.substring(0, 2).trim());
        transactionType.setDescription(line.substring(2, 52).trim());
        
        return transactionType;
    }
    
    /**
     * Parses transaction category record from file line.
     */
    private TransactionCategoryData parseTransactionCategoryRecord(String line) {
        if (line.length() < 60) {
            throw new IllegalArgumentException("Invalid transaction category record length: " + line.length());
        }
        
        TransactionCategoryData transactionCategory = new TransactionCategoryData();
        transactionCategory.setTypeCode(line.substring(0, 2).trim());
        transactionCategory.setCategoryCode(Integer.parseInt(line.substring(2, 6).trim()));
        transactionCategory.setDescription(line.substring(6, 56).trim());
        
        return transactionCategory;
    }
    
    // Data structure classes matching COBOL record layouts
    
    /**
     * Transaction record data structure equivalent to COBOL TRAN-RECORD.
     */
    public static class TransactionRecord {
        private String transactionId;
        private String transactionTypeCode;
        private int transactionCategoryCode;
        private String transactionSource;
        private String transactionDescription;
        private BigDecimal transactionAmount;
        private Long merchantId;
        private String merchantName;
        private String merchantCity;
        private String merchantZip;
        private String cardNumber;
        private String transactionOriginalTimestamp;
        private String transactionProcessTimestamp;
        
        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getTransactionTypeCode() { return transactionTypeCode; }
        public void setTransactionTypeCode(String transactionTypeCode) { this.transactionTypeCode = transactionTypeCode; }
        
        public int getTransactionCategoryCode() { return transactionCategoryCode; }
        public void setTransactionCategoryCode(int transactionCategoryCode) { this.transactionCategoryCode = transactionCategoryCode; }
        
        public String getTransactionSource() { return transactionSource; }
        public void setTransactionSource(String transactionSource) { this.transactionSource = transactionSource; }
        
        public String getTransactionDescription() { return transactionDescription; }
        public void setTransactionDescription(String transactionDescription) { this.transactionDescription = transactionDescription; }
        
        public BigDecimal getTransactionAmount() { return transactionAmount; }
        public void setTransactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; }
        
        public Long getMerchantId() { return merchantId; }
        public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
        
        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        
        public String getMerchantCity() { return merchantCity; }
        public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
        
        public String getMerchantZip() { return merchantZip; }
        public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
        
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getTransactionOriginalTimestamp() { return transactionOriginalTimestamp; }
        public void setTransactionOriginalTimestamp(String transactionOriginalTimestamp) { this.transactionOriginalTimestamp = transactionOriginalTimestamp; }
        
        public String getTransactionProcessTimestamp() { return transactionProcessTimestamp; }
        public void setTransactionProcessTimestamp(String transactionProcessTimestamp) { this.transactionProcessTimestamp = transactionProcessTimestamp; }
    }
    
    /**
     * Card cross-reference data structure equivalent to COBOL CARD-XREF-RECORD.
     */
    public static class CardXrefData {
        private String cardNumber;
        private String accountId;
        private String customerId;
        
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
    }
    
    /**
     * Transaction type data structure equivalent to COBOL TRAN-TYPE-RECORD.
     */
    public static class TransactionTypeData {
        private String typeCode;
        private String description;
        
        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Transaction category data structure equivalent to COBOL TRAN-CAT-RECORD.
     */
    public static class TransactionCategoryData {
        private String typeCode;
        private int categoryCode;
        private String description;
        
        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        
        public int getCategoryCode() { return categoryCode; }
        public void setCategoryCode(int categoryCode) { this.categoryCode = categoryCode; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Enriched transaction data structure combining transaction with cross-reference data.
     */
    public static class EnrichedTransactionData {
        private TransactionRecord transaction;
        private CardXrefData cardXref;
        private TransactionTypeData transactionType;
        private TransactionCategoryData transactionCategory;
        
        public TransactionRecord getTransaction() { return transaction; }
        public void setTransaction(TransactionRecord transaction) { this.transaction = transaction; }
        
        public CardXrefData getCardXref() { return cardXref; }
        public void setCardXref(CardXrefData cardXref) { this.cardXref = cardXref; }
        
        public TransactionTypeData getTransactionType() { return transactionType; }
        public void setTransactionType(TransactionTypeData transactionType) { this.transactionType = transactionType; }
        
        public TransactionCategoryData getTransactionCategory() { return transactionCategory; }
        public void setTransactionCategory(TransactionCategoryData transactionCategory) { this.transactionCategory = transactionCategory; }
    }
}
        reportLines.add(accountTotalLine);
        
        accountTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        lineCounter++;
        
        // Add separator line
        reportLines.add(BLANK_LINE);
        lineCounter++;
        
        logger.debug("Account totals written");
    }
    
    /**
     * Writes grand totals line equivalent to COBOL 1110-WRITE-GRAND-TOTALS.
     */
    private void writeGrandTotals() {
        String grandTotalLine = String.format("%90s %10.2f", "GRAND TOTAL:", grandTotal);
        reportLines.add(grandTotalLine);
        lineCounter++;
        
        logger.info("Grand totals written: {}", grandTotal);
    }
    
    /**
     * Parses a transaction record from file line.
     * Equivalent to COBOL FD-TRANFILE-REC parsing.
     */
    private TransactionRecord parseTransactionRecord(String line) {
        if (line.length() < 350) {
            throw new IllegalArgumentException("Invalid transaction record length: " + line.length());
        }
        
        TransactionRecord transaction = new TransactionRecord();
        
        // Parse fields according to COBOL TRAN-RECORD layout
        transaction.setTransactionId(line.substring(0, 16).trim());
        transaction.setTransactionTypeCode(line.substring(16, 18).trim());
        transaction.setTransactionCategoryCode(Integer.parseInt(line.substring(18, 22).trim()));
        transaction.setTransactionSource(line.substring(22, 32).trim());
        transaction.setTransactionDescription(line.substring(32, 132).trim());
        
        // Parse packed decimal amount (COBOL S9(09)V99)
        String amountStr = line.substring(132, 143).trim();
        transaction.setTransactionAmount(new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP));
        
        transaction.setMerchantId(Long.parseLong(line.substring(143, 152).trim()));
        transaction.setMerchantName(line.substring(152, 202).trim());
        transaction.setMerchantCity(line.substring(202, 252).trim());
        transaction.setMerchantZip(line.substring(252, 262).trim());
        transaction.setCardNumber(line.substring(262, 278).trim());
        transaction.setTransactionOriginalTimestamp(line.substring(278, 304).trim());
        transaction.setTransactionProcessTimestamp(line.substring(304, 330).trim());
        
        return transaction;
    }
        if (lineCounter > 0 && lineCounter % PAGE_SIZE == 0) {
            
            // Write page totals before page break
            writePageTotals();
            
            // Write headers for new page
            List<String> headers = formatReportHeader();
            reportLines.addAll(headers);
            lineCounter += headers.size();
            
            logger.debug("Page break handled at line {}", lineCounter);
            return true;
        }
        
        return false;
    }
        }
        enriched.setTransactionType(transactionType);
        
        // Transaction category lookup (equivalent to 1500-C-LOOKUP-TRANCATG)
        String categoryKey = transaction.getTransactionTypeCode() + 
                           String.format("%04d", transaction.getTransactionCategoryCode());
        TransactionCategoryData transactionCategory = transactionCategoryMap.get(categoryKey);
        if (transactionCategory == null) {
            String errorMsg = "INVALID TRANSACTION CATEGORY KEY: " + categoryKey;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        enriched.setTransactionCategory(transactionCategory);
        
        return enriched;
    }
