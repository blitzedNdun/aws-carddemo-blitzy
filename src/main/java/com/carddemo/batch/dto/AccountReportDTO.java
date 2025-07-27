package com.carddemo.batch.dto;

import com.carddemo.common.entity.Account;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.DateUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for account report output containing structured report data including
 * account details, financial information, and formatted output lines for Spring Batch
 * FlatFileItemWriter report generation equivalent to COBOL CBACT01C display operations.
 * 
 * <p>This DTO maintains exact functional equivalence with the original COBOL CBACT01C.cbl
 * batch program display logic, preserving field ordering, formatting, and precision as
 * defined in the ACCOUNT-RECORD structure from CVACT01Y.cpy copybook.</p>
 * 
 * <h3>COBOL Source Mapping:</h3>
 * <ul>
 *   <li>CBACT01C.cbl: Lines 118-131 (1100-DISPLAY-ACCT-RECORD paragraph)</li>
 *   <li>CVACT01Y.cpy: Lines 4-16 (ACCOUNT-RECORD structure definition)</li>
 *   <li>Field precision maintained using BigDecimal for COMP-3 financial amounts</li>
 *   <li>Date formatting preserved for COBOL PIC X(10) date fields</li>
 * </ul>
 * 
 * <h3>Report Format Specifications:</h3>
 * <ul>
 *   <li>Header line: Column headers with fixed-width positioning</li>
 *   <li>Detail line: Account data formatted with exact field alignment</li>
 *   <li>Summary line: Aggregate totals with proper decimal formatting</li>
 *   <li>Separator lines: Dash separators matching COBOL display format</li>
 * </ul>
 * 
 * <h3>Financial Precision Requirements:</h3>
 * <ul>
 *   <li>All monetary amounts use BigDecimal with MathContext.DECIMAL128</li>
 *   <li>Currency formatting maintains 2 decimal places (COBOL V99 equivalent)</li>
 *   <li>No rounding errors or precision loss from original COBOL calculations</li>
 * </ul>
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class AccountReportDTO {

    // =======================================================================
    // CORE ACCOUNT DATA FIELDS (from CVACT01Y.cpy ACCOUNT-RECORD)
    // =======================================================================

    /**
     * Account ID - Primary identifier
     * Mapped from COBOL: ACCT-ID PIC 9(11)
     */
    private String accountId;

    /**
     * Account Active Status
     * Mapped from COBOL: ACCT-ACTIVE-STATUS PIC X(01)
     */
    private String activeStatus;

    /**
     * Current Account Balance
     * Mapped from COBOL: ACCT-CURR-BAL PIC S9(10)V99 COMP-3
     */
    private BigDecimal currentBalance;

    /**
     * Credit Limit
     * Mapped from COBOL: ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     */
    private BigDecimal creditLimit;

    /**
     * Cash Credit Limit
     * Mapped from COBOL: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3
     */
    private BigDecimal cashCreditLimit;

    /**
     * Account Open Date
     * Mapped from COBOL: ACCT-OPEN-DATE PIC X(10)
     */
    private LocalDate openDate;

    /**
     * Account Expiration Date
     * Mapped from COBOL: ACCT-EXPIRAION-DATE PIC X(10)
     * Note: Preserves original COBOL typo in field name
     */
    private LocalDate expirationDate;

    /**
     * Account Reissue Date
     * Mapped from COBOL: ACCT-REISSUE-DATE PIC X(10)
     */
    private LocalDate reissueDate;

    /**
     * Current Cycle Credit
     * Mapped from COBOL: ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3
     */
    private BigDecimal currentCycleCredit;

    /**
     * Current Cycle Debit
     * Mapped from COBOL: ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3
     */
    private BigDecimal currentCycleDebit;

    /**
     * Account Group ID
     * Mapped from COBOL: ACCT-GROUP-ID PIC X(10)
     */
    private String groupId;

    /**
     * Account Address ZIP Code
     * Mapped from COBOL: ACCT-ADDR-ZIP PIC X(10)
     */
    private String addressZip;

    // =======================================================================
    // REPORT METADATA FIELDS
    // =======================================================================

    /**
     * Report generation timestamp for audit trail and tracking
     */
    private LocalDateTime generationTimestamp;

    /**
     * Report line number for sequential processing tracking
     */
    private Long reportLineNumber;

    /**
     * Record count information for batch processing validation
     */
    private Long recordCount;

    // =======================================================================
    // FORMAT CONSTANTS FOR COBOL-EQUIVALENT OUTPUT
    // =======================================================================

    /**
     * Field width constants matching COBOL display format requirements
     */
    private static final int ACCOUNT_ID_WIDTH = 15;
    private static final int STATUS_WIDTH = 8;
    private static final int CURRENCY_WIDTH = 15;
    private static final int DATE_WIDTH = 12;
    private static final int GROUP_ID_WIDTH = 12;
    private static final int ZIP_WIDTH = 12;

    /**
     * Separator line constant matching COBOL dash display
     */
    private static final String SEPARATOR_LINE = "-------------------------------------------------";

    // =======================================================================
    // CONSTRUCTORS
    // =======================================================================

    /**
     * Default constructor for Spring Batch initialization
     */
    public AccountReportDTO() {
        this.generationTimestamp = LocalDateTime.now();
        this.currentBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.cashCreditLimit = BigDecimal.ZERO;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
    }

    /**
     * Constructor from Account entity for report generation
     * Maps all fields from Account JPA entity to report DTO structure
     * 
     * @param account The Account entity to convert to report format
     */
    public AccountReportDTO(Account account) {
        this();
        if (account != null) {
            this.accountId = account.getAccountId();
            this.activeStatus = Boolean.TRUE.equals(account.getActiveStatus()) ? "Y" : "N";
            this.currentBalance = account.getCurrentBalance();
            this.creditLimit = account.getCreditLimit();
            this.cashCreditLimit = account.getCashCreditLimit();
            this.openDate = account.getOpenDate();
            this.expirationDate = account.getExpirationDate();
            this.reissueDate = account.getReissueDate();
            this.currentCycleCredit = account.getCurrentCycleCredit();
            this.currentCycleDebit = account.getCurrentCycleDebit();
            this.groupId = account.getGroupId();
            this.addressZip = account.getAddressZip();
        }
    }

    // =======================================================================
    // GETTER AND SETTER METHODS
    // =======================================================================

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDate getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }

    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit;
    }

    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }

    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }

    public Long getReportLineNumber() {
        return reportLineNumber;
    }

    public void setReportLineNumber(Long reportLineNumber) {
        this.reportLineNumber = reportLineNumber;
    }

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    // =======================================================================
    // REPORT FORMATTING METHODS (COBOL DISPLAY EQUIVALENT)
    // =======================================================================

    /**
     * Generates the primary report line equivalent to COBOL DISPLAY ACCOUNT-RECORD.
     * This method provides the main formatted output for Spring Batch FlatFileItemWriter
     * matching the exact display format from CBACT01C.cbl lines 78 and 119-130.
     * 
     * @return Formatted account record string for report output
     */
    public String getReportLine() {
        StringBuilder reportLine = new StringBuilder();
        
        // Format account details matching COBOL 1100-DISPLAY-ACCT-RECORD paragraph
        reportLine.append(formatAccountField("ACCT-ID", accountId));
        reportLine.append(formatAccountField("ACCT-ACTIVE-STATUS", activeStatus));
        reportLine.append(formatAccountField("ACCT-CURR-BAL", formatCurrency(currentBalance)));
        reportLine.append(formatAccountField("ACCT-CREDIT-LIMIT", formatCurrency(creditLimit)));
        reportLine.append(formatAccountField("ACCT-CASH-CREDIT-LIMIT", formatCurrency(cashCreditLimit)));
        reportLine.append(formatAccountField("ACCT-OPEN-DATE", formatDate(openDate)));
        reportLine.append(formatAccountField("ACCT-EXPIRAION-DATE", formatDate(expirationDate)));
        reportLine.append(formatAccountField("ACCT-REISSUE-DATE", formatDate(reissueDate)));
        reportLine.append(formatAccountField("ACCT-CURR-CYC-CREDIT", formatCurrency(currentCycleCredit)));
        reportLine.append(formatAccountField("ACCT-CURR-CYC-DEBIT", formatCurrency(currentCycleDebit)));
        reportLine.append(formatAccountField("ACCT-GROUP-ID", groupId));
        reportLine.append(SEPARATOR_LINE);
        
        return reportLine.toString();
    }

    /**
     * Formats report header line with column headers for tabular display.
     * Creates fixed-width column headers matching COBOL report formatting standards.
     * 
     * @return Formatted header line string with column titles
     */
    public String formatAsHeaderLine() {
        StringBuilder header = new StringBuilder();
        
        // Create column headers with proper spacing for tabular format
        header.append(String.format("%-" + ACCOUNT_ID_WIDTH + "s", "ACCOUNT-ID"));
        header.append(String.format("%-" + STATUS_WIDTH + "s", "STATUS"));
        header.append(String.format("%" + CURRENCY_WIDTH + "s", "CURRENT-BAL"));
        header.append(String.format("%" + CURRENCY_WIDTH + "s", "CREDIT-LIMIT"));
        header.append(String.format("%" + CURRENCY_WIDTH + "s", "CASH-LIMIT"));
        header.append(String.format("%-" + DATE_WIDTH + "s", "OPEN-DATE"));
        header.append(String.format("%-" + DATE_WIDTH + "s", "EXPIRE-DATE"));
        header.append(String.format("%-" + DATE_WIDTH + "s", "REISSUE-DATE"));
        header.append(String.format("%" + CURRENCY_WIDTH + "s", "CYC-CREDIT"));
        header.append(String.format("%" + CURRENCY_WIDTH + "s", "CYC-DEBIT"));
        header.append(String.format("%-" + GROUP_ID_WIDTH + "s", "GROUP-ID"));
        header.append(String.format("%-" + ZIP_WIDTH + "s", "ZIP-CODE"));
        header.append("\n");
        
        // Add separator line under headers
        header.append("=".repeat(ACCOUNT_ID_WIDTH + STATUS_WIDTH + (CURRENCY_WIDTH * 4) + 
                     (DATE_WIDTH * 3) + GROUP_ID_WIDTH + ZIP_WIDTH + 11)); // 11 for spaces
        
        return header.toString();
    }

    /**
     * Formats account data as detail line for tabular report display.
     * Creates fixed-width formatted line with all account information aligned to columns.
     * 
     * @return Formatted detail line string with account data
     */
    public String formatAsDetailLine() {
        StringBuilder detail = new StringBuilder();
        
        // Format all fields in tabular columns with proper alignment
        detail.append(String.format("%-" + ACCOUNT_ID_WIDTH + "s", 
            truncateField(accountId, ACCOUNT_ID_WIDTH)));
        detail.append(String.format("%-" + STATUS_WIDTH + "s", 
            truncateField(activeStatus, STATUS_WIDTH)));
        detail.append(String.format("%" + CURRENCY_WIDTH + "s", 
            formatCurrency(currentBalance)));
        detail.append(String.format("%" + CURRENCY_WIDTH + "s", 
            formatCurrency(creditLimit)));
        detail.append(String.format("%" + CURRENCY_WIDTH + "s", 
            formatCurrency(cashCreditLimit)));
        detail.append(String.format("%-" + DATE_WIDTH + "s", 
            formatDate(openDate)));
        detail.append(String.format("%-" + DATE_WIDTH + "s", 
            formatDate(expirationDate)));
        detail.append(String.format("%-" + DATE_WIDTH + "s", 
            formatDate(reissueDate)));
        detail.append(String.format("%" + CURRENCY_WIDTH + "s", 
            formatCurrency(currentCycleCredit)));
        detail.append(String.format("%" + CURRENCY_WIDTH + "s", 
            formatCurrency(currentCycleDebit)));
        detail.append(String.format("%-" + GROUP_ID_WIDTH + "s", 
            truncateField(groupId, GROUP_ID_WIDTH)));
        detail.append(String.format("%-" + ZIP_WIDTH + "s", 
            truncateField(addressZip, ZIP_WIDTH)));
        
        return detail.toString();
    }

    /**
     * Formats summary line with aggregate financial totals.
     * Creates summary footer line with computed totals and report metadata.
     * 
     * @return Formatted summary line string with totals and statistics
     */
    public String formatAsSummaryLine() {
        StringBuilder summary = new StringBuilder();
        
        // Add separator before summary
        summary.append("-".repeat(120)).append("\n");
        
        // Format summary information
        summary.append("ACCOUNT REPORT SUMMARY").append("\n");
        summary.append("Generated: ").append(DateUtils.formatDateForDisplay(LocalDate.now())).append("\n");
        
        if (recordCount != null) {
            summary.append("Total Records: ").append(recordCount).append("\n");
        }
        
        // Add financial summary if this represents aggregated data
        if (currentBalance != null) {
            summary.append("Account Balance: ").append(formatCurrency(currentBalance)).append("\n");
        }
        if (creditLimit != null) {
            summary.append("Credit Limit: ").append(formatCurrency(creditLimit)).append("\n");
        }
        
        // Add available credit calculation
        if (currentBalance != null && creditLimit != null) {
            BigDecimal availableCredit = BigDecimalUtils.subtract(creditLimit, currentBalance);
            summary.append("Available Credit: ").append(formatCurrency(availableCredit)).append("\n");
        }
        
        return summary.toString();
    }

    // =======================================================================
    // PRIVATE FORMATTING UTILITY METHODS
    // =======================================================================

    /**
     * Formats a field with its label matching COBOL DISPLAY statement format.
     * Replicates the exact format from CBACT01C.cbl DISPLAY statements (lines 119-130).
     * 
     * @param label The field label (e.g., "ACCT-ID")
     * @param value The field value to display
     * @return Formatted field string with label and value
     */
    private String formatAccountField(String label, String value) {
        return String.format("%-25s: %s%n", label, value != null ? value : "");
    }

    /**
     * Formats BigDecimal currency values with exact COBOL precision.
     * Uses BigDecimalUtils to maintain COMP-3 equivalent precision and formatting.
     * 
     * @param amount The BigDecimal amount to format
     * @return Formatted currency string or empty string if null
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return BigDecimalUtils.formatCurrency(amount);
    }

    /**
     * Formats LocalDate values for report display.
     * Uses DateUtils to maintain COBOL date formatting consistency.
     * 
     * @param date The LocalDate to format
     * @return Formatted date string or empty string if null
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DateUtils.formatDateForDisplay(date);
    }

    /**
     * Truncates field values to fit within specified column widths.
     * Prevents field overflow in fixed-width report formatting.
     * 
     * @param value The field value to truncate
     * @param maxWidth The maximum width allowed
     * @return Truncated field value or empty string if null
     */
    private String truncateField(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        return value.length() > maxWidth ? value.substring(0, maxWidth) : value;
    }

    // =======================================================================
    // STANDARD OBJECT METHODS
    // =======================================================================

    @Override
    public String toString() {
        return "AccountReportDTO{" +
                "accountId='" + accountId + '\'' +
                ", activeStatus='" + activeStatus + '\'' +
                ", currentBalance=" + currentBalance +
                ", creditLimit=" + creditLimit +
                ", cashCreditLimit=" + cashCreditLimit +
                ", openDate=" + openDate +
                ", expirationDate=" + expirationDate +
                ", reissueDate=" + reissueDate +
                ", currentCycleCredit=" + currentCycleCredit +
                ", currentCycleDebit=" + currentCycleDebit +
                ", groupId='" + groupId + '\'' +
                ", addressZip='" + addressZip + '\'' +
                ", generationTimestamp=" + generationTimestamp +
                ", reportLineNumber=" + reportLineNumber +
                ", recordCount=" + recordCount +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AccountReportDTO that = (AccountReportDTO) obj;
        return accountId != null && accountId.equals(that.accountId);
    }

    @Override
    public int hashCode() {
        return accountId != null ? accountId.hashCode() : 0;
    }
}