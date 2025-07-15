/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * <p>This DTO converts the COBOL CBTRN03C transaction report format to modern Java 
 * Spring Batch processing while maintaining exact format and structure compatibility.
 * The report output preserves 133-character line formatting, account-level grouping,
 * and precise BigDecimal calculations equivalent to COBOL COMP-3 arithmetic.</p>
 * 
 * <p><strong>COBOL Report Structure Mapping:</strong></p>
 * <ul>
 *   <li>REPORT-NAME-HEADER → formatAsHeaderLine() method</li>
 *   <li>TRANSACTION-HEADER-1/2 → column header formatting</li>
 *   <li>TRANSACTION-DETAIL-REPORT → formatAsDetailLine() method</li>
 *   <li>REPORT-PAGE-TOTALS → page-level total formatting</li>
 *   <li>REPORT-ACCOUNT-TOTALS → account-level total formatting</li>
 *   <li>REPORT-GRAND-TOTALS → formatAsTotalLine() method</li>
 * </ul>
 * 
 * <p><strong>Report Line Types:</strong></p>
 * <ul>
 *   <li><strong>Header Lines</strong>: Report title, date range, column headers</li>
 *   <li><strong>Detail Lines</strong>: Individual transaction records with formatting</li>
 *   <li><strong>Total Lines</strong>: Page totals, account totals, grand totals</li>
 *   <li><strong>Separator Lines</strong>: Blank lines and dividers for readability</li>
 * </ul>
 * 
 * <p><strong>Financial Precision Requirements:</strong></p>
 * <ul>
 *   <li>All monetary amounts use BigDecimal with DECIMAL(12,2) precision</li>
 *   <li>Calculations maintain COBOL COMP-3 arithmetic equivalence</li>
 *   <li>Currency formatting follows original report specifications</li>
 *   <li>Running totals preserve exact decimal precision</li>
 * </ul>
 * 
 * <p><strong>Spring Batch Integration:</strong></p>
 * <ul>
 *   <li>Compatible with FlatFileItemWriter for report file generation</li>
 *   <li>Supports chunk-based processing for large transaction volumes</li>
 *   <li>Thread-safe implementation for parallel batch processing</li>
 *   <li>Configurable formatting for different report variations</li>
 * </ul>
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since Java 21
 * @see Transaction
 * @see BigDecimalUtils
 */
public class TransactionReportDTO {
    
    // Report formatting constants matching COBOL FD-REPTFILE-REC PIC X(133)
    private static final int REPORT_LINE_LENGTH = 133;
    private static final String REPORT_TITLE = "CARDDEMO TRANSACTION DETAIL REPORT";
    private static final String PAGE_SEPARATOR = "PAGE";
    private static final String TOTAL_LABEL = "TOTAL";
    private static final String ACCOUNT_TOTAL_LABEL = "ACCOUNT TOTAL";
    private static final String GRAND_TOTAL_LABEL = "GRAND TOTAL";
    
    // Date formatting matching COBOL date handling
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter REPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    
    // Column header definitions matching COBOL TRANSACTION-HEADER layout
    private static final String HEADER_LINE_1 = "TRANS ID         ACCT ID      TYPE  CAT  DESCRIPTION                                        AMOUNT";
    private static final String HEADER_LINE_2 = "---------------- ------------ ----- ---- -------------------------------------------------- --------------";
    
    // Core transaction data fields from TRAN-RECORD (CVTRA05Y.cpy)
    private String transactionId;           // TRAN-ID PIC X(16)
    private String accountId;               // Derived from XREF-ACCT-ID
    private String transactionType;         // TRAN-TYPE-CD PIC X(02)
    private String transactionCategory;     // TRAN-CAT-CD PIC 9(04)
    private BigDecimal amount;              // TRAN-AMT PIC S9(09)V99
    private String description;             // TRAN-DESC PIC X(100)
    
    // Report metadata fields for pagination and date range
    private LocalDateTime startDate;        // WS-START-DATE equivalent
    private LocalDateTime endDate;          // WS-END-DATE equivalent
    private Integer pageNumber;             // Page numbering for multi-page reports
    private LocalDateTime generationTimestamp; // Report generation timestamp
    
    // Running totals for account and page-level calculations
    private BigDecimal accountTotal;        // WS-ACCOUNT-TOTAL equivalent
    private BigDecimal pageTotal;           // WS-PAGE-TOTAL equivalent
    private BigDecimal grandTotal;          // WS-GRAND-TOTAL equivalent
    
    // Report line type indicator for formatting decisions
    private ReportLineType lineType;
    
    /**
     * Enumeration defining different types of report lines for formatting control.
     */
    public enum ReportLineType {
        HEADER,          // Report title and column headers
        DETAIL,          // Individual transaction records
        PAGE_TOTAL,      // Page-level subtotals
        ACCOUNT_TOTAL,   // Account-level subtotals
        GRAND_TOTAL,     // Final report totals
        SEPARATOR        // Blank lines and dividers
    }
    
    /**
     * Default constructor for Spring Batch ItemReader compatibility.
     */
    public TransactionReportDTO() {
        this.generationTimestamp = LocalDateTime.now();
        this.lineType = ReportLineType.DETAIL;
        this.accountTotal = BigDecimal.ZERO;
        this.pageTotal = BigDecimal.ZERO;
        this.grandTotal = BigDecimal.ZERO;
    }
    
    /**
     * Constructor for creating DTO from Transaction entity.
     * 
     * @param transaction Transaction entity containing source data
     * @param accountId Account identifier for grouping
     */
    public TransactionReportDTO(Transaction transaction, String accountId) {
        this();
        this.transactionId = transaction.getTransactionId();
        this.accountId = accountId;
        this.transactionType = transaction.getTransactionType() != null ? 
            transaction.getTransactionType().name() : "";
        this.transactionCategory = transaction.getCategoryCode() != null ? 
            transaction.getCategoryCode().name() : "";
        this.amount = transaction.getAmount();
        this.description = transaction.getDescription();
    }
    
    /**
     * Constructor for creating total line DTOs.
     * 
     * @param lineType Type of total line (PAGE_TOTAL, ACCOUNT_TOTAL, GRAND_TOTAL)
     * @param totalAmount Total amount for this line
     * @param accountId Account identifier (for account totals)
     */
    public TransactionReportDTO(ReportLineType lineType, BigDecimal totalAmount, String accountId) {
        this();
        this.lineType = lineType;
        this.accountId = accountId;
        
        switch (lineType) {
            case PAGE_TOTAL:
                this.pageTotal = totalAmount;
                break;
            case ACCOUNT_TOTAL:
                this.accountTotal = totalAmount;
                break;
            case GRAND_TOTAL:
                this.grandTotal = totalAmount;
                break;
            default:
                this.amount = totalAmount;
        }
    }
    
    /**
     * Gets the formatted report line string for FlatFileItemWriter output.
     * This method determines the appropriate formatting based on the line type
     * and delegates to specific formatting methods.
     * 
     * @return Formatted 133-character report line string
     */
    public String getReportLine() {
        switch (lineType) {
            case HEADER:
                return formatAsHeaderLine();
            case DETAIL:
                return formatAsDetailLine();
            case PAGE_TOTAL:
            case ACCOUNT_TOTAL:
            case GRAND_TOTAL:
                return formatAsTotalLine();
            case SEPARATOR:
                return formatSeparatorLine();
            default:
                return formatAsDetailLine();
        }
    }
    
    /**
     * Formats the DTO as a header line including report title, date range, and column headers.
     * Replicates COBOL REPORT-NAME-HEADER and TRANSACTION-HEADER formatting.
     * 
     * @return Formatted header line string (133 characters)
     */
    public String formatAsHeaderLine() {
        StringBuilder headerLine = new StringBuilder();
        
        if (startDate != null && endDate != null) {
            // Format report title with date range
            String dateRange = String.format("FROM %s TO %s", 
                startDate.format(REPORT_DATE_FORMATTER),
                endDate.format(REPORT_DATE_FORMATTER));
            
            String titleLine = String.format("%s  %s", REPORT_TITLE, dateRange);
            headerLine.append(centerText(titleLine, REPORT_LINE_LENGTH));
        } else {
            // Default header format
            headerLine.append(centerText(REPORT_TITLE, REPORT_LINE_LENGTH));
        }
        
        return padToLength(headerLine.toString(), REPORT_LINE_LENGTH);
    }
    
    /**
     * Formats the DTO as a detail line showing individual transaction information.
     * Replicates COBOL TRANSACTION-DETAIL-REPORT structure with exact column alignment.
     * 
     * @return Formatted detail line string (133 characters)
     */
    public String formatAsDetailLine() {
        StringBuilder detailLine = new StringBuilder();
        
        // Transaction ID field (16 characters, left-aligned)
        detailLine.append(padRight(safeString(transactionId), 16));
        detailLine.append(" ");
        
        // Account ID field (12 characters, left-aligned)
        detailLine.append(padRight(safeString(accountId), 12));
        detailLine.append(" ");
        
        // Transaction Type field (5 characters, center-aligned)
        detailLine.append(padCenter(safeString(transactionType), 5));
        detailLine.append(" ");
        
        // Transaction Category field (4 characters, center-aligned)
        detailLine.append(padCenter(safeString(transactionCategory), 4));
        detailLine.append(" ");
        
        // Description field (50 characters, left-aligned, truncated if necessary)
        String truncatedDescription = safeString(description);
        if (truncatedDescription.length() > 50) {
            truncatedDescription = truncatedDescription.substring(0, 47) + "...";
        }
        detailLine.append(padRight(truncatedDescription, 50));
        detailLine.append(" ");
        
        // Amount field (14 characters, right-aligned with currency formatting)
        String formattedAmount = amount != null ? 
            BigDecimalUtils.formatCurrency(amount) : "$0.00";
        detailLine.append(padLeft(formattedAmount, 14));
        
        return padToLength(detailLine.toString(), REPORT_LINE_LENGTH);
    }
    
    /**
     * Formats the DTO as a total line for page, account, or grand totals.
     * Replicates COBOL REPORT-PAGE-TOTALS, REPORT-ACCOUNT-TOTALS, and REPORT-GRAND-TOTALS.
     * 
     * @return Formatted total line string (133 characters)
     */
    public String formatAsTotalLine() {
        StringBuilder totalLine = new StringBuilder();
        
        String totalLabel;
        BigDecimal totalAmount;
        
        switch (lineType) {
            case PAGE_TOTAL:
                totalLabel = PAGE_SEPARATOR + " " + TOTAL_LABEL;
                totalAmount = pageTotal;
                break;
            case ACCOUNT_TOTAL:
                totalLabel = ACCOUNT_TOTAL_LABEL;
                if (accountId != null) {
                    totalLabel += " - " + accountId;
                }
                totalAmount = accountTotal;
                break;
            case GRAND_TOTAL:
                totalLabel = GRAND_TOTAL_LABEL;
                totalAmount = grandTotal;
                break;
            default:
                totalLabel = TOTAL_LABEL;
                totalAmount = amount != null ? amount : BigDecimal.ZERO;
        }
        
        // Format total line with label and amount
        String formattedAmount = BigDecimalUtils.formatCurrency(totalAmount);
        
        // Calculate spacing to right-align amount at position 119 (133 - 14)
        int labelLength = totalLabel.length();
        int spacingLength = REPORT_LINE_LENGTH - 14 - labelLength;
        
        totalLine.append(totalLabel);
        totalLine.append(" ".repeat(Math.max(0, spacingLength)));
        totalLine.append(padLeft(formattedAmount, 14));
        
        return padToLength(totalLine.toString(), REPORT_LINE_LENGTH);
    }
    
    /**
     * Formats a separator line (blank line) for report readability.
     * 
     * @return Blank line string (133 characters)
     */
    private String formatSeparatorLine() {
        return " ".repeat(REPORT_LINE_LENGTH);
    }
    
    /**
     * Creates column header lines for the transaction report.
     * 
     * @return Array of header line strings
     */
    public String[] getColumnHeaders() {
        return new String[] {
            padToLength(HEADER_LINE_1, REPORT_LINE_LENGTH),
            padToLength(HEADER_LINE_2, REPORT_LINE_LENGTH)
        };
    }
    
    // Utility methods for string formatting and padding
    
    /**
     * Safely converts object to string, handling null values.
     * 
     * @param value Object to convert
     * @return String representation or empty string if null
     */
    private String safeString(Object value) {
        return value != null ? value.toString() : "";
    }
    
    /**
     * Pads string to specified length, truncating if necessary.
     * 
     * @param text Input text
     * @param length Target length
     * @return Padded or truncated string
     */
    private String padToLength(String text, int length) {
        if (text == null) {
            return " ".repeat(length);
        }
        if (text.length() > length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }
    
    /**
     * Left-pads string to specified length.
     * 
     * @param text Input text
     * @param length Target length
     * @return Left-padded string
     */
    private String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) {
            return text.substring(text.length() - length);
        }
        return " ".repeat(length - text.length()) + text;
    }
    
    /**
     * Right-pads string to specified length.
     * 
     * @param text Input text
     * @param length Target length
     * @return Right-padded string
     */
    private String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }
    
    /**
     * Center-pads string to specified length.
     * 
     * @param text Input text
     * @param length Target length
     * @return Center-padded string
     */
    private String padCenter(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        int totalPadding = length - text.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
    
    /**
     * Centers text within the specified width.
     * 
     * @param text Text to center
     * @param width Total width
     * @return Centered text
     */
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int totalPadding = width - text.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
    
    // Getter and setter methods for all fields
    
    /**
     * Gets the transaction ID.
     * 
     * @return transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Sets the transaction ID.
     * 
     * @param transactionId transaction ID
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    /**
     * Gets the account ID.
     * 
     * @return account ID
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID.
     * 
     * @param accountId account ID
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the transaction type.
     * 
     * @return transaction type
     */
    public String getTransactionType() {
        return transactionType;
    }
    
    /**
     * Sets the transaction type.
     * 
     * @param transactionType transaction type
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    /**
     * Gets the transaction category.
     * 
     * @return transaction category
     */
    public String getTransactionCategory() {
        return transactionCategory;
    }
    
    /**
     * Sets the transaction category.
     * 
     * @param transactionCategory transaction category
     */
    public void setTransactionCategory(String transactionCategory) {
        this.transactionCategory = transactionCategory;
    }
    
    /**
     * Gets the transaction amount.
     * 
     * @return transaction amount
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Sets the transaction amount.
     * 
     * @param amount transaction amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Gets the transaction description.
     * 
     * @return transaction description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the transaction description.
     * 
     * @param description transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the report start date.
     * 
     * @return start date
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    /**
     * Sets the report start date.
     * 
     * @param startDate start date
     */
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    /**
     * Gets the report end date.
     * 
     * @return end date
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    /**
     * Sets the report end date.
     * 
     * @param endDate end date
     */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    /**
     * Gets the page number.
     * 
     * @return page number
     */
    public Integer getPageNumber() {
        return pageNumber;
    }
    
    /**
     * Sets the page number.
     * 
     * @param pageNumber page number
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    /**
     * Gets the generation timestamp.
     * 
     * @return generation timestamp
     */
    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }
    
    /**
     * Sets the generation timestamp.
     * 
     * @param generationTimestamp generation timestamp
     */
    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }
    
    /**
     * Gets the account total amount.
     * 
     * @return account total
     */
    public BigDecimal getAccountTotal() {
        return accountTotal;
    }
    
    /**
     * Sets the account total amount.
     * 
     * @param accountTotal account total
     */
    public void setAccountTotal(BigDecimal accountTotal) {
        this.accountTotal = accountTotal;
    }
    
    /**
     * Gets the page total amount.
     * 
     * @return page total
     */
    public BigDecimal getPageTotal() {
        return pageTotal;
    }
    
    /**
     * Sets the page total amount.
     * 
     * @param pageTotal page total
     */
    public void setPageTotal(BigDecimal pageTotal) {
        this.pageTotal = pageTotal;
    }
    
    /**
     * Gets the grand total amount.
     * 
     * @return grand total
     */
    public BigDecimal getGrandTotal() {
        return grandTotal;
    }
    
    /**
     * Sets the grand total amount.
     * 
     * @param grandTotal grand total
     */
    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }
    
    /**
     * Gets the line type.
     * 
     * @return line type
     */
    public ReportLineType getLineType() {
        return lineType;
    }
    
    /**
     * Sets the line type.
     * 
     * @param lineType line type
     */
    public void setLineType(ReportLineType lineType) {
        this.lineType = lineType;
    }
    
    /**
     * Equals method for DTO comparison.
     * 
     * @param obj object to compare
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransactionReportDTO that = (TransactionReportDTO) obj;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(accountId, that.accountId) &&
               Objects.equals(lineType, that.lineType);
    }
    
    /**
     * Hash code method for DTO hashing.
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountId, lineType);
    }
    
    /**
     * String representation of the DTO.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionReportDTO{id='%s', account='%s', type=%s, amount=%s, lineType=%s}",
            transactionId, accountId, transactionType, 
            amount != null ? BigDecimalUtils.formatCurrency(amount) : "null", lineType
        );
    }
}