package com.carddemo.batch.dto;

import com.carddemo.transaction.Transaction;
import com.carddemo.common.util.BigDecimalUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Data Transfer Object for transaction report output containing structured report data
 * including transaction details, account grouping, totals, and report metadata for 
 * Spring Batch FlatFileItemWriter output formatting.
 * 
 * This DTO replicates the exact format and structure of the original COBOL CBTRN03C 
 * report layout as defined in CVTRA07Y.cpy copybook, ensuring identical output 
 * formatting while leveraging modern Java capabilities for precise financial 
 * calculations and report generation.
 * 
 * Key Features:
 * - Exact field correspondence to COBOL report layout structures
 * - Support for account-level grouping and totals calculation as per original logic
 * - BigDecimal precision for financial amounts matching COBOL COMP-3 arithmetic
 * - Formatted text output compatible with Spring Batch FlatFileItemWriter
 * - Report metadata fields including date range, generation timestamp, and page information
 * - Column alignment and decimal precision utilities for consistent formatting
 * 
 * Report Line Format (133 characters total):
 * - Transaction ID: 16 characters
 * - Account ID: 11 characters  
 * - Transaction Type: 2 characters + 15 character description
 * - Transaction Category: 4 digits + 29 character description
 * - Transaction Source: 10 characters
 * - Transaction Amount: Formatted currency (-ZZZ,ZZZ,ZZZ.ZZ format)
 * 
 * @author CardDemo Migration Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
public class TransactionReportDTO {

    /**
     * Date formatter for report header dates (YYYY-MM-DD format).
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Report line width constant matching COBOL FD-REPTFILE-REC PIC X(133).
     */
    private static final int REPORT_LINE_WIDTH = 133;
    
    /**
     * Short name for the report header matching COBOL REPT-SHORT-NAME.
     */
    private static final String REPORT_SHORT_NAME = "DALYREPT";
    
    /**
     * Long name for the report header matching COBOL REPT-LONG-NAME.
     */
    private static final String REPORT_LONG_NAME = "Daily Transaction Report";

    // Transaction Detail Fields
    
    /**
     * Transaction identifier serving as primary key.
     * Maps to TRAN-REPORT-TRANS-ID from COBOL copybook (PIC X(16)).
     */
    private String transactionId;
    
    /**
     * Account identifier associated with this transaction.
     * Maps to TRAN-REPORT-ACCOUNT-ID from COBOL copybook (PIC X(11)).
     */
    private String accountId;
    
    /**
     * Transaction type code for categorization.
     * Maps to TRAN-REPORT-TYPE-CD from COBOL copybook (PIC X(02)).
     */
    private String transactionType;
    
    /**
     * Transaction type description for display.
     * Maps to TRAN-REPORT-TYPE-DESC from COBOL copybook (PIC X(15)).
     */
    private String transactionTypeDescription;
    
    /**
     * Transaction category code for business logic classification.
     * Maps to TRAN-REPORT-CAT-CD from COBOL copybook (PIC 9(04)).
     */
    private String transactionCategory;
    
    /**
     * Transaction category description for display.
     * Maps to TRAN-REPORT-CAT-DESC from COBOL copybook (PIC X(29)).
     */
    private String transactionCategoryDescription;
    
    /**
     * Transaction amount with exact BigDecimal precision.
     * Maps to TRAN-REPORT-AMT from COBOL copybook (PIC -ZZZ,ZZZ,ZZZ.ZZ).
     * Uses BigDecimal to maintain COBOL COMP-3 decimal precision.
     */
    private BigDecimal amount;
    
    /**
     * Transaction source indicating origin (POS, ATM, WEB, etc.).
     * Maps to TRAN-REPORT-SOURCE from COBOL copybook (PIC X(10)).
     */
    private String source;
    
    /**
     * Transaction description for audit and display purposes.
     * Maps to transaction description field for report details.
     */
    private String description;

    // Report Metadata Fields
    
    /**
     * Report start date for date range filtering.
     * Maps to REPT-START-DATE from COBOL copybook (PIC X(10)).
     */
    private LocalDateTime startDate;
    
    /**
     * Report end date for date range filtering.
     * Maps to REPT-END-DATE from COBOL copybook (PIC X(10)).
     */
    private LocalDateTime endDate;
    
    /**
     * Page number for report pagination.
     * Used for generating page-specific headers and totals.
     */
    private Integer pageNumber;
    
    /**
     * Generation timestamp for audit trail.
     * Indicates when the report was generated.
     */
    private LocalDateTime generationTimestamp;

    /**
     * Default constructor required for Spring Batch serialization.
     * Initializes generation timestamp to current time.
     */
    public TransactionReportDTO() {
        this.generationTimestamp = LocalDateTime.now();
        this.pageNumber = 1;
    }

    /**
     * Constructor with transaction data for building report DTOs.
     * 
     * @param transaction The Transaction entity used as source data
     */
    public TransactionReportDTO(Transaction transaction) {
        this();
        if (transaction != null) {
            this.transactionId = transaction.getTransactionId();
            this.accountId = transaction.getAccountId();
            this.transactionType = transaction.getTransactionType() != null ? 
                transaction.getTransactionType().name() : "";
            this.transactionCategory = transaction.getCategoryCode() != null ? 
                transaction.getCategoryCode().name() : "";
            this.amount = transaction.getAmount();
            this.source = transaction.getSource();
            this.description = transaction.getDescription();
        }
    }

    /**
     * Constructor with all transaction details for comprehensive report creation.
     * 
     * @param transactionId The unique transaction identifier
     * @param accountId The associated account identifier
     * @param transactionType The transaction type code
     * @param transactionTypeDescription The transaction type description
     * @param transactionCategory The transaction category code
     * @param transactionCategoryDescription The transaction category description
     * @param amount The transaction amount
     * @param source The transaction source
     * @param description The transaction description
     */
    public TransactionReportDTO(String transactionId, String accountId, String transactionType,
                               String transactionTypeDescription, String transactionCategory,
                               String transactionCategoryDescription, BigDecimal amount,
                               String source, String description) {
        this();
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.transactionTypeDescription = transactionTypeDescription;
        this.transactionCategory = transactionCategory;
        this.transactionCategoryDescription = transactionCategoryDescription;
        this.amount = amount != null ? BigDecimalUtils.roundToMonetary(amount) : BigDecimalUtils.ZERO_MONETARY;
        this.source = source;
        this.description = description;
    }

    // Report Line Formatting Methods

    /**
     * Formats the DTO as a report header line including report name and date range.
     * Replicates COBOL REPORT-NAME-HEADER structure from CVTRA07Y.cpy.
     * 
     * @return Formatted header line (133 characters)
     */
    public String formatAsHeaderLine() {
        StringBuilder header = new StringBuilder(REPORT_LINE_WIDTH);
        
        // REPT-SHORT-NAME (38 characters)
        header.append(String.format("%-38s", REPORT_SHORT_NAME));
        
        // REPT-LONG-NAME (41 characters)
        header.append(String.format("%-41s", REPORT_LONG_NAME));
        
        // REPT-DATE-HEADER (12 characters)
        header.append("Date Range: ");
        
        // REPT-START-DATE (10 characters)
        String startDateStr = startDate != null ? startDate.format(DATE_FORMATTER) : "          ";
        header.append(String.format("%-10s", startDateStr));
        
        // " to " (4 characters)
        header.append(" to ");
        
        // REPT-END-DATE (10 characters)
        String endDateStr = endDate != null ? endDate.format(DATE_FORMATTER) : "          ";
        header.append(String.format("%-10s", endDateStr));
        
        // Pad to full width
        while (header.length() < REPORT_LINE_WIDTH) {
            header.append(" ");
        }
        
        return header.substring(0, REPORT_LINE_WIDTH);
    }

    /**
     * Formats the DTO as a transaction detail line with proper column alignment.
     * Replicates COBOL TRANSACTION-DETAIL-REPORT structure from CVTRA07Y.cpy.
     * 
     * @return Formatted detail line (133 characters)
     */
    public String formatAsDetailLine() {
        StringBuilder detail = new StringBuilder(REPORT_LINE_WIDTH);
        
        // TRAN-REPORT-TRANS-ID (16 characters)
        detail.append(String.format("%-16s", truncateOrPad(transactionId, 16)));
        
        // Filler (1 space)
        detail.append(" ");
        
        // TRAN-REPORT-ACCOUNT-ID (11 characters)
        detail.append(String.format("%-11s", truncateOrPad(accountId, 11)));
        
        // Filler (1 space)
        detail.append(" ");
        
        // TRAN-REPORT-TYPE-CD (2 characters)
        detail.append(String.format("%-2s", truncateOrPad(transactionType, 2)));
        
        // Filler (1 character: '-')
        detail.append("-");
        
        // TRAN-REPORT-TYPE-DESC (15 characters)
        detail.append(String.format("%-15s", truncateOrPad(transactionTypeDescription, 15)));
        
        // Filler (1 space)
        detail.append(" ");
        
        // TRAN-REPORT-CAT-CD (4 characters)
        detail.append(String.format("%4s", truncateOrPad(transactionCategory, 4)));
        
        // Filler (1 character: '-')
        detail.append("-");
        
        // TRAN-REPORT-CAT-DESC (29 characters)
        detail.append(String.format("%-29s", truncateOrPad(transactionCategoryDescription, 29)));
        
        // Filler (1 space)
        detail.append(" ");
        
        // TRAN-REPORT-SOURCE (10 characters)
        detail.append(String.format("%-10s", truncateOrPad(source, 10)));
        
        // Filler (4 spaces)
        detail.append("    ");
        
        // TRAN-REPORT-AMT (formatted currency with -ZZZ,ZZZ,ZZZ.ZZ pattern)
        String formattedAmount = formatCurrencyAmount(amount);
        detail.append(String.format("%15s", formattedAmount));
        
        // Filler (2 spaces)
        detail.append("  ");
        
        // Pad to full width
        while (detail.length() < REPORT_LINE_WIDTH) {
            detail.append(" ");
        }
        
        return detail.substring(0, REPORT_LINE_WIDTH);
    }

    /**
     * Formats the DTO as a total line for page, account, or grand totals.
     * Replicates COBOL total line structures from CVTRA07Y.cpy.
     * 
     * @param totalType The type of total ("Page Total", "Account Total", "Grand Total")
     * @param totalAmount The total amount to display
     * @return Formatted total line (133 characters)
     */
    public String formatAsTotalLine(String totalType, BigDecimal totalAmount) {
        StringBuilder total = new StringBuilder(REPORT_LINE_WIDTH);
        
        // Total type label (varies by type)
        total.append(String.format("%-13s", totalType));
        
        // Dots filler to match COBOL ALL '.' pattern
        int dotsCount = REPORT_LINE_WIDTH - 13 - 15 - 2; // 13 for label, 15 for amount, 2 for spacing
        for (int i = 0; i < dotsCount; i++) {
            total.append(".");
        }
        
        // Formatted total amount
        String formattedTotal = formatCurrencyAmount(totalAmount);
        total.append(String.format("%15s", formattedTotal));
        
        // Ensure exact width
        return total.substring(0, REPORT_LINE_WIDTH);
    }

    /**
     * Gets the complete report line formatted for FlatFileItemWriter output.
     * This method provides the primary interface for Spring Batch file writing.
     * 
     * @return Formatted report line ready for file output
     */
    public String getReportLine() {
        return formatAsDetailLine();
    }

    // Utility Methods

    /**
     * Formats a BigDecimal amount as currency using COBOL-equivalent formatting.
     * Replicates the -ZZZ,ZZZ,ZZZ.ZZ format from COBOL report layouts.
     * 
     * @param amount The amount to format
     * @return Formatted currency string
     */
    private String formatCurrencyAmount(BigDecimal amount) {
        if (amount == null) {
            return "           0.00";
        }
        
        // Use BigDecimalUtils for consistent formatting
        String formatted = BigDecimalUtils.formatCurrency(amount);
        
        // Remove the $ symbol to match COBOL format
        if (formatted.startsWith("$")) {
            formatted = formatted.substring(1);
        }
        
        // Handle negative amounts with proper sign placement
        if (amount.compareTo(BigDecimal.ZERO) < 0 && !formatted.startsWith("-")) {
            formatted = "-" + formatted;
        }
        
        return formatted;
    }

    /**
     * Truncates or pads a string to the specified length.
     * 
     * @param value The string value to process
     * @param length The target length
     * @return Processed string of exact length
     */
    private String truncateOrPad(String value, int length) {
        if (value == null) {
            value = "";
        }
        
        if (value.length() > length) {
            return value.substring(0, length);
        } else {
            return String.format("%-" + length + "s", value);
        }
    }

    // Getter and Setter Methods as required by the exports schema

    /**
     * Gets the transaction identifier.
     * 
     * @return The unique transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction identifier.
     * 
     * @param transactionId The unique transaction ID
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the account identifier.
     * 
     * @return The account ID
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier.
     * 
     * @param accountId The account ID
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the transaction type code.
     * 
     * @return The transaction type
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type code.
     * 
     * @param transactionType The transaction type
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction category code.
     * 
     * @return The transaction category
     */
    public String getTransactionCategory() {
        return transactionCategory;
    }

    /**
     * Sets the transaction category code.
     * 
     * @param transactionCategory The transaction category
     */
    public void setTransactionCategory(String transactionCategory) {
        this.transactionCategory = transactionCategory;
    }

    /**
     * Gets the transaction amount with exact BigDecimal precision.
     * 
     * @return The transaction amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount using BigDecimal for precise financial calculations.
     * 
     * @param amount The transaction amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? BigDecimalUtils.roundToMonetary(amount) : BigDecimalUtils.ZERO_MONETARY;
    }

    /**
     * Gets the transaction description.
     * 
     * @return The transaction description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description The transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the report start date.
     * 
     * @return The start date for the report range
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Sets the report start date.
     * 
     * @param startDate The start date for the report range
     */
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the report end date.
     * 
     * @return The end date for the report range
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }

    /**
     * Sets the report end date.
     * 
     * @param endDate The end date for the report range
     */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the page number.
     * 
     * @return The current page number
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number.
     * 
     * @param pageNumber The current page number
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the generation timestamp.
     * 
     * @return The timestamp when the report was generated
     */
    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }

    /**
     * Sets the generation timestamp.
     * 
     * @param generationTimestamp The timestamp when the report was generated
     */
    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }

    // Additional Methods for Enhanced Functionality

    /**
     * Gets the transaction type description.
     * 
     * @return The transaction type description
     */
    public String getTransactionTypeDescription() {
        return transactionTypeDescription;
    }

    /**
     * Sets the transaction type description.
     * 
     * @param transactionTypeDescription The transaction type description
     */
    public void setTransactionTypeDescription(String transactionTypeDescription) {
        this.transactionTypeDescription = transactionTypeDescription;
    }

    /**
     * Gets the transaction category description.
     * 
     * @return The transaction category description
     */
    public String getTransactionCategoryDescription() {
        return transactionCategoryDescription;
    }

    /**
     * Sets the transaction category description.
     * 
     * @param transactionCategoryDescription The transaction category description
     */
    public void setTransactionCategoryDescription(String transactionCategoryDescription) {
        this.transactionCategoryDescription = transactionCategoryDescription;
    }

    /**
     * Gets the transaction source.
     * 
     * @return The transaction source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the transaction source.
     * 
     * @param source The transaction source
     */
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TransactionReportDTO that = (TransactionReportDTO) obj;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(generationTimestamp, that.generationTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountId, generationTimestamp);
    }

    @Override
    public String toString() {
        return "TransactionReportDTO{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", transactionCategory='" + transactionCategory + '\'' +
                ", amount=" + amount +
                ", source='" + source + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", pageNumber=" + pageNumber +
                ", generationTimestamp=" + generationTimestamp +
                '}';
    }
}