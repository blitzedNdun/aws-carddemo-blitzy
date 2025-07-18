package com.carddemo.batch.dto;

import com.carddemo.transaction.Transaction;
import com.carddemo.common.util.BigDecimalUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object for transaction report output containing structured report data 
 * including transaction details, account grouping, totals, and report metadata for 
 * Spring Batch FlatFileItemWriter output formatting.
 * 
 * This DTO replicates the exact format and structure of the original COBOL CBTRN03C report 
 * with precise column alignment, decimal formatting, and grouping functionality. The class 
 * provides specialized formatting methods for different report line types including header, 
 * detail, and total records that match the original 133-character report layout.
 * 
 * Key Features:
 * - Exact COBOL report format replication with 133-character line width
 * - Account-level grouping and running totals calculation support
 * - Formatted text output compatible with Spring Batch FlatFileItemWriter
 * - Precise column alignment matching original BMS report layout
 * - BigDecimal precision for financial amounts using COBOL COMP-3 equivalency
 * - Report metadata fields for date range, generation timestamp, and pagination
 * 
 * Report Structure Mapping:
 * - REPORT-NAME-HEADER → formatAsHeaderLine() with report title and date range
 * - TRANSACTION-DETAIL-REPORT → formatAsDetailLine() with transaction data
 * - REPORT-PAGE-TOTALS → formatAsTotalLine() with page totals
 * - REPORT-ACCOUNT-TOTALS → formatAsTotalLine() with account totals  
 * - REPORT-GRAND-TOTALS → formatAsTotalLine() with grand totals
 * 
 * Column Layout (133 characters total):
 * - Transaction ID: positions 1-16 (PIC X(16))
 * - Account ID: positions 18-28 (PIC X(11))
 * - Transaction Type: positions 30-31 (PIC X(02))
 * - Type Description: positions 33-47 (PIC X(15))
 * - Category Code: positions 49-52 (PIC 9(04))
 * - Category Description: positions 54-82 (PIC X(29))
 * - Source: positions 84-93 (PIC X(10))
 * - Amount: positions 98-113 (PIC -ZZZ,ZZZ,ZZZ.ZZ)
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 */
public class TransactionReportDTO {
    
    /**
     * Report line width constant matching COBOL FD-REPTFILE-REC PIC X(133)
     */
    public static final int REPORT_LINE_WIDTH = 133;
    
    /**
     * Date formatter for report header dates matching COBOL date format
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Transaction identifier from source Transaction entity (TRAN-ID)
     */
    private String transactionId;
    
    /**
     * Account identifier from cross-reference lookup (XREF-ACCT-ID)
     */
    private String accountId;
    
    /**
     * Transaction type code from source Transaction entity (TRAN-TYPE-CD)
     */
    private String transactionType;
    
    /**
     * Transaction type description from TRANTYPE lookup (TRAN-TYPE-DESC)
     */
    private String transactionTypeDescription;
    
    /**
     * Transaction category code from source Transaction entity (TRAN-CAT-CD)
     */
    private String transactionCategory;
    
    /**
     * Transaction category description from TRANCATG lookup (TRAN-CAT-TYPE-DESC)
     */
    private String transactionCategoryDescription;
    
    /**
     * Transaction source from source Transaction entity (TRAN-SOURCE)
     */
    private String source;
    
    /**
     * Transaction amount with BigDecimal precision (TRAN-AMT)
     */
    private BigDecimal amount;
    
    /**
     * Transaction description from source Transaction entity (TRAN-DESC)
     */
    private String description;
    
    /**
     * Report start date parameter (WS-START-DATE)
     */
    private String startDate;
    
    /**
     * Report end date parameter (WS-END-DATE)
     */
    private String endDate;
    
    /**
     * Current page number for pagination (WS-PAGE-COUNTER)
     */
    private int pageNumber;
    
    /**
     * Report generation timestamp
     */
    private LocalDateTime reportTimestamp;
    
    /**
     * Running total for current page (WS-PAGE-TOTAL)
     */
    private BigDecimal pageTotal;
    
    /**
     * Running total for current account (WS-ACCOUNT-TOTAL)
     */
    private BigDecimal accountTotal;
    
    /**
     * Grand total for entire report (WS-GRAND-TOTAL)
     */
    private BigDecimal grandTotal;
    
    /**
     * Line type indicator for formatting purposes
     */
    private ReportLineType lineType;
    
    /**
     * Enumeration for different types of report lines
     */
    public enum ReportLineType {
        HEADER, DETAIL, PAGE_TOTAL, ACCOUNT_TOTAL, GRAND_TOTAL
    }
    
    /**
     * Default constructor for Spring Batch item processing
     */
    public TransactionReportDTO() {
        this.amount = BigDecimal.ZERO;
        this.pageTotal = BigDecimal.ZERO;
        this.accountTotal = BigDecimal.ZERO;
        this.grandTotal = BigDecimal.ZERO;
        this.reportTimestamp = LocalDateTime.now();
        this.lineType = ReportLineType.DETAIL;
    }
    
    /**
     * Constructor with Transaction entity for detail line creation
     * 
     * @param transaction Source Transaction entity
     * @param accountId Account identifier from cross-reference lookup
     * @param transactionTypeDescription Transaction type description from lookup
     * @param transactionCategoryDescription Transaction category description from lookup
     */
    public TransactionReportDTO(Transaction transaction, String accountId, 
                               String transactionTypeDescription, String transactionCategoryDescription) {
        this();
        this.transactionId = transaction.getTransactionId();
        this.accountId = accountId;
        this.transactionType = transaction.getTransactionType();
        this.transactionTypeDescription = transactionTypeDescription;
        this.transactionCategory = transaction.getCategoryCode();
        this.transactionCategoryDescription = transactionCategoryDescription;
        this.source = transaction.getSource();
        this.amount = transaction.getAmount();
        this.description = transaction.getDescription();
        this.lineType = ReportLineType.DETAIL;
    }
    
    /**
     * Gets the transaction identifier
     * 
     * @return Transaction ID as 16-character string
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Sets the transaction identifier
     * 
     * @param transactionId Transaction ID as 16-character string
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    /**
     * Gets the account identifier
     * 
     * @return Account ID as 11-character string
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account identifier
     * 
     * @param accountId Account ID as 11-character string
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the transaction type code
     * 
     * @return Transaction type as 2-character string
     */
    public String getTransactionType() {
        return transactionType;
    }
    
    /**
     * Sets the transaction type code
     * 
     * @param transactionType Transaction type as 2-character string
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    /**
     * Gets the transaction category code
     * 
     * @return Transaction category as 4-character string
     */
    public String getTransactionCategory() {
        return transactionCategory;
    }
    
    /**
     * Sets the transaction category code
     * 
     * @param transactionCategory Transaction category as 4-character string
     */
    public void setTransactionCategory(String transactionCategory) {
        this.transactionCategory = transactionCategory;
    }
    
    /**
     * Gets the transaction amount
     * 
     * @return Transaction amount as BigDecimal with exact precision
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Sets the transaction amount
     * 
     * @param amount Transaction amount as BigDecimal with exact precision
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * Gets the transaction description
     * 
     * @return Transaction description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the transaction description
     * 
     * @param description Transaction description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the transaction source
     * 
     * @return Transaction source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Sets the transaction source
     * 
     * @param source Transaction source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Gets the transaction type description
     * 
     * @return Transaction type description
     */
    public String getTransactionTypeDescription() {
        return transactionTypeDescription;
    }
    
    /**
     * Sets the transaction type description
     * 
     * @param transactionTypeDescription Transaction type description
     */
    public void setTransactionTypeDescription(String transactionTypeDescription) {
        this.transactionTypeDescription = transactionTypeDescription;
    }
    
    /**
     * Gets the transaction category description
     * 
     * @return Transaction category description
     */
    public String getTransactionCategoryDescription() {
        return transactionCategoryDescription;
    }
    
    /**
     * Sets the transaction category description
     * 
     * @param transactionCategoryDescription Transaction category description
     */
    public void setTransactionCategoryDescription(String transactionCategoryDescription) {
        this.transactionCategoryDescription = transactionCategoryDescription;
    }
    
    /**
     * Gets the report start date
     * 
     * @return Report start date as string
     */
    public String getStartDate() {
        return startDate;
    }
    
    /**
     * Sets the report start date
     * 
     * @param startDate Report start date as string
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    /**
     * Gets the report end date
     * 
     * @return Report end date as string
     */
    public String getEndDate() {
        return endDate;
    }
    
    /**
     * Sets the report end date
     * 
     * @param endDate Report end date as string
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    
    /**
     * Gets the current page number
     * 
     * @return Current page number
     */
    public int getPageNumber() {
        return pageNumber;
    }
    
    /**
     * Sets the current page number
     * 
     * @param pageNumber Current page number
     */
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    /**
     * Gets the formatted report line based on line type
     * 
     * @return Formatted report line string with exact 133-character width
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
            default:
                return formatAsDetailLine();
        }
    }
    
    /**
     * Formats the DTO as a header line matching COBOL REPORT-NAME-HEADER structure
     * 
     * This method creates a formatted header line with report name, title, and date range
     * exactly matching the original COBOL report header format with precise spacing
     * and alignment.
     * 
     * @return 133-character formatted header line
     */
    public String formatAsHeaderLine() {
        StringBuilder line = new StringBuilder();
        
        // REPT-SHORT-NAME (38 characters)
        line.append(String.format("%-38s", "DALYREPT"));
        
        // REPT-LONG-NAME (41 characters) 
        line.append(String.format("%-41s", "Daily Transaction Report"));
        
        // REPT-DATE-HEADER (12 characters)
        line.append(String.format("%-12s", "Date Range: "));
        
        // REPT-START-DATE (10 characters)
        String startDateStr = startDate != null ? startDate : "          ";
        line.append(String.format("%-10s", startDateStr));
        
        // " to " (4 characters)
        line.append(" to ");
        
        // REPT-END-DATE (10 characters)
        String endDateStr = endDate != null ? endDate : "          ";
        line.append(String.format("%-10s", endDateStr));
        
        // Pad to 133 characters
        return String.format("%-133s", line.toString());
    }
    
    /**
     * Formats the DTO as a detail line matching COBOL TRANSACTION-DETAIL-REPORT structure
     * 
     * This method creates a formatted detail line with transaction data exactly matching
     * the original COBOL report detail format with precise column alignment and spacing
     * equivalent to the BMS TRANSACTION-DETAIL-REPORT layout.
     * 
     * @return 133-character formatted detail line
     */
    public String formatAsDetailLine() {
        StringBuilder line = new StringBuilder();
        
        // TRAN-REPORT-TRANS-ID (16 characters)
        String transId = transactionId != null ? transactionId : "";
        line.append(String.format("%-16s", transId));
        
        // Filler space (1 character)
        line.append(" ");
        
        // TRAN-REPORT-ACCOUNT-ID (11 characters)
        String acctId = accountId != null ? accountId : "";
        line.append(String.format("%-11s", acctId));
        
        // Filler space (1 character)
        line.append(" ");
        
        // TRAN-REPORT-TYPE-CD (2 characters)
        String typeCode = transactionType != null ? transactionType : "";
        line.append(String.format("%-2s", typeCode));
        
        // Filler dash (1 character)
        line.append("-");
        
        // TRAN-REPORT-TYPE-DESC (15 characters)
        String typeDesc = transactionTypeDescription != null ? transactionTypeDescription : "";
        line.append(String.format("%-15s", typeDesc));
        
        // Filler space (1 character)
        line.append(" ");
        
        // TRAN-REPORT-CAT-CD (4 characters)
        String catCode = transactionCategory != null ? transactionCategory : "";
        line.append(String.format("%-4s", catCode));
        
        // Filler dash (1 character)
        line.append("-");
        
        // TRAN-REPORT-CAT-DESC (29 characters)
        String catDesc = transactionCategoryDescription != null ? transactionCategoryDescription : "";
        line.append(String.format("%-29s", catDesc));
        
        // Filler space (1 character)
        line.append(" ");
        
        // TRAN-REPORT-SOURCE (10 characters)
        String sourceStr = source != null ? source : "";
        line.append(String.format("%-10s", sourceStr));
        
        // Filler spaces (4 characters)
        line.append("    ");
        
        // TRAN-REPORT-AMT formatted as -ZZZ,ZZZ,ZZZ.ZZ (16 characters)
        String amountStr = formatAmount(amount);
        line.append(String.format("%16s", amountStr));
        
        // Filler spaces (2 characters)
        line.append("  ");
        
        // Pad to 133 characters
        return String.format("%-133s", line.toString());
    }
    
    /**
     * Formats the DTO as a total line matching COBOL report totals structures
     * 
     * This method creates formatted total lines for page totals, account totals, 
     * and grand totals exactly matching the original COBOL report format with 
     * precise alignment and decimal formatting.
     * 
     * @return 133-character formatted total line
     */
    public String formatAsTotalLine() {
        StringBuilder line = new StringBuilder();
        
        String label;
        BigDecimal totalAmount;
        
        // Determine label and amount based on line type
        switch (lineType) {
            case PAGE_TOTAL:
                label = "Page Total";
                totalAmount = pageTotal;
                break;
            case ACCOUNT_TOTAL:
                label = "Account Total";
                totalAmount = accountTotal;
                break;
            case GRAND_TOTAL:
                label = "Grand Total";
                totalAmount = grandTotal;
                break;
            default:
                label = "Page Total";
                totalAmount = pageTotal;
                break;
        }
        
        // Label (11-13 characters depending on type)
        line.append(String.format("%-13s", label));
        
        // Dots to fill space (84-86 characters depending on label length)
        int dotsLength = 97 - label.length();
        for (int i = 0; i < dotsLength; i++) {
            line.append(".");
        }
        
        // Total amount formatted as +ZZZ,ZZZ,ZZZ.ZZ (16 characters)
        String totalStr = formatAmount(totalAmount);
        line.append(String.format("%16s", totalStr));
        
        // Pad to 133 characters
        return String.format("%-133s", line.toString());
    }
    
    /**
     * Formats a BigDecimal amount using COBOL-style numeric formatting
     * 
     * This method formats monetary amounts with thousands separators and proper
     * sign positioning to match the original COBOL report format exactly.
     * 
     * @param amount BigDecimal amount to format
     * @return Formatted amount string with sign and separators
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "           0.00";
        }
        
        // Use BigDecimalUtils for consistent formatting
        String formatted = BigDecimalUtils.formatCurrency(amount);
        
        // Remove dollar sign and handle negative formatting
        if (formatted.startsWith("$")) {
            formatted = formatted.substring(1);
        } else if (formatted.startsWith("$-")) {
            formatted = "-" + formatted.substring(2);
        }
        
        return formatted;
    }
    
    /**
     * Sets the line type for formatting purposes
     * 
     * @param lineType Type of report line to format
     */
    public void setLineType(ReportLineType lineType) {
        this.lineType = lineType;
    }
    
    /**
     * Gets the line type
     * 
     * @return Current line type
     */
    public ReportLineType getLineType() {
        return lineType;
    }
    
    /**
     * Gets the page total
     * 
     * @return Page total as BigDecimal
     */
    public BigDecimal getPageTotal() {
        return pageTotal;
    }
    
    /**
     * Sets the page total
     * 
     * @param pageTotal Page total as BigDecimal
     */
    public void setPageTotal(BigDecimal pageTotal) {
        this.pageTotal = pageTotal;
    }
    
    /**
     * Gets the account total
     * 
     * @return Account total as BigDecimal
     */
    public BigDecimal getAccountTotal() {
        return accountTotal;
    }
    
    /**
     * Sets the account total
     * 
     * @param accountTotal Account total as BigDecimal
     */
    public void setAccountTotal(BigDecimal accountTotal) {
        this.accountTotal = accountTotal;
    }
    
    /**
     * Gets the grand total
     * 
     * @return Grand total as BigDecimal
     */
    public BigDecimal getGrandTotal() {
        return grandTotal;
    }
    
    /**
     * Sets the grand total
     * 
     * @param grandTotal Grand total as BigDecimal
     */
    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }
    
    /**
     * Gets the report generation timestamp
     * 
     * @return Report timestamp
     */
    public LocalDateTime getReportTimestamp() {
        return reportTimestamp;
    }
    
    /**
     * Sets the report generation timestamp
     * 
     * @param reportTimestamp Report timestamp
     */
    public void setReportTimestamp(LocalDateTime reportTimestamp) {
        this.reportTimestamp = reportTimestamp;
    }
    
    /**
     * Creates a header line DTO with report metadata
     * 
     * @param startDate Report start date
     * @param endDate Report end date
     * @param pageNumber Current page number
     * @return Header line DTO
     */
    public static TransactionReportDTO createHeaderLine(String startDate, String endDate, int pageNumber) {
        TransactionReportDTO dto = new TransactionReportDTO();
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setPageNumber(pageNumber);
        dto.setLineType(ReportLineType.HEADER);
        return dto;
    }
    
    /**
     * Creates a page total line DTO
     * 
     * @param pageTotal Page total amount
     * @return Page total line DTO
     */
    public static TransactionReportDTO createPageTotalLine(BigDecimal pageTotal) {
        TransactionReportDTO dto = new TransactionReportDTO();
        dto.setPageTotal(pageTotal);
        dto.setLineType(ReportLineType.PAGE_TOTAL);
        return dto;
    }
    
    /**
     * Creates an account total line DTO
     * 
     * @param accountTotal Account total amount
     * @return Account total line DTO
     */
    public static TransactionReportDTO createAccountTotalLine(BigDecimal accountTotal) {
        TransactionReportDTO dto = new TransactionReportDTO();
        dto.setAccountTotal(accountTotal);
        dto.setLineType(ReportLineType.ACCOUNT_TOTAL);
        return dto;
    }
    
    /**
     * Creates a grand total line DTO
     * 
     * @param grandTotal Grand total amount
     * @return Grand total line DTO
     */
    public static TransactionReportDTO createGrandTotalLine(BigDecimal grandTotal) {
        TransactionReportDTO dto = new TransactionReportDTO();
        dto.setGrandTotal(grandTotal);
        dto.setLineType(ReportLineType.GRAND_TOTAL);
        return dto;
    }
    
    /**
     * String representation of the DTO
     * 
     * @return String representation with key field values
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionReportDTO{transactionId='%s', accountId='%s', type='%s', amount=%s, lineType=%s}",
            transactionId, accountId, transactionType, amount, lineType
        );
    }
}