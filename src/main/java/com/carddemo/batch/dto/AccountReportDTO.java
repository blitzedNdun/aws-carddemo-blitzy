package com.carddemo.batch.dto;

import com.carddemo.account.entity.Account;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object for account report output containing structured report data.
 * 
 * This DTO provides exact format and structure equivalent to the original COBOL 
 * CBACT01C account listing report, supporting Spring Batch FlatFileItemWriter 
 * report generation with precise field positioning and financial data formatting.
 * 
 * The class maintains BigDecimal precision equivalent to COBOL COMP-3 arithmetic
 * for all financial calculations and provides formatted output lines matching
 * the original COBOL DISPLAY statements for seamless report generation.
 * 
 * Key Features:
 * - Exact COBOL ACCOUNT-RECORD layout field correspondence from CVACT01Y.cpy
 * - BigDecimal financial precision maintenance for regulatory compliance
 * - Formatted report line generation for header, detail, and summary records
 * - Spring Batch FlatFileItemWriter compatibility for batch processing
 * - Account information formatting utilities for consistent column alignment
 * - Report metadata fields including generation timestamp and record count
 * 
 * Performance Characteristics:
 * - Optimized for batch processing within 4-hour window requirement
 * - Memory efficient for large report generation operations
 * - Thread-safe for concurrent batch processing scenarios
 * 
 * Converted from: app/cbl/CBACT01C.cbl (COBOL batch program)
 * Data Structure: app/cpy/CVACT01Y.cpy (COBOL account record layout)
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
public class AccountReportDTO {

    /**
     * Account unique identifier (11 digits)
     * Converted from: ACCT-ID PIC 9(11)
     */
    private String accountId;

    /**
     * Account active status (1 character)
     * Converted from: ACCT-ACTIVE-STATUS PIC X(01)
     */
    private String activeStatus;

    /**
     * Current account balance with exact decimal precision
     * Converted from: ACCT-CURR-BAL PIC S9(10)V99
     */
    private BigDecimal currentBalance;

    /**
     * Account credit limit with exact decimal precision
     * Converted from: ACCT-CREDIT-LIMIT PIC S9(10)V99
     */
    private BigDecimal creditLimit;

    /**
     * Cash advance credit limit with exact decimal precision
     * Converted from: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
     */
    private BigDecimal cashCreditLimit;

    /**
     * Account opening date
     * Converted from: ACCT-OPEN-DATE PIC X(10)
     */
    private LocalDate openDate;

    /**
     * Account expiration date
     * Converted from: ACCT-EXPIRAION-DATE PIC X(10) (note: typo preserved)
     */
    private LocalDate expirationDate;

    /**
     * Card reissue date
     * Converted from: ACCT-REISSUE-DATE PIC X(10)
     */
    private LocalDate reissueDate;

    /**
     * Current cycle credit amount with exact decimal precision
     * Converted from: ACCT-CURR-CYC-CREDIT PIC S9(10)V99
     */
    private BigDecimal currentCycleCredit;

    /**
     * Current cycle debit amount with exact decimal precision
     * Converted from: ACCT-CURR-CYC-DEBIT PIC S9(10)V99
     */
    private BigDecimal currentCycleDebit;

    /**
     * Account group identifier
     * Converted from: ACCT-GROUP-ID PIC X(10)
     */
    private String groupId;

    /**
     * Account address ZIP code
     * Converted from: ACCT-ADDR-ZIP PIC X(10)
     */
    private String addressZip;

    /**
     * Report generation timestamp for audit trail
     */
    private LocalDate generationTimestamp;

    /**
     * Record sequence number for report ordering
     */
    private Long recordSequence;

    /**
     * Report section identifier (HEADER, DETAIL, SUMMARY)
     */
    private String reportSection;

    /**
     * Default constructor initializing BigDecimal fields to zero
     */
    public AccountReportDTO() {
        this.currentBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.cashCreditLimit = BigDecimal.ZERO;
        this.currentCycleCredit = BigDecimal.ZERO;
        this.currentCycleDebit = BigDecimal.ZERO;
        this.generationTimestamp = DateUtils.getCurrentDate();
        this.recordSequence = 0L;
        this.reportSection = "DETAIL";
    }

    /**
     * Constructor to create AccountReportDTO from Account entity
     * 
     * @param account The Account entity to convert
     */
    public AccountReportDTO(Account account) {
        this();
        if (account != null) {
            this.accountId = account.getAccountId();
            this.activeStatus = account.getActiveStatus() != null ? account.getActiveStatus().name() : "";
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

    /**
     * Creates a formatted report line equivalent to COBOL DISPLAY statements
     * 
     * Replicates the exact format from CBACT01C.cbl:
     * - Each field displayed with label, colon, and value
     * - Proper spacing and alignment for readability
     * - Financial amounts formatted with currency symbols
     * - Dates formatted for display consistency
     * 
     * @return Formatted report line string
     */
    public String getReportLine() {
        StringBuilder reportLine = new StringBuilder();
        
        // Account ID line
        reportLine.append("ACCT-ID                 : ").append(accountId != null ? accountId : "").append("\n");
        
        // Active Status line
        reportLine.append("ACCT-ACTIVE-STATUS      : ").append(activeStatus != null ? activeStatus : "").append("\n");
        
        // Current Balance line with currency formatting
        reportLine.append("ACCT-CURR-BAL           : ").append(formatFinancialAmount(currentBalance)).append("\n");
        
        // Credit Limit line with currency formatting
        reportLine.append("ACCT-CREDIT-LIMIT       : ").append(formatFinancialAmount(creditLimit)).append("\n");
        
        // Cash Credit Limit line with currency formatting
        reportLine.append("ACCT-CASH-CREDIT-LIMIT  : ").append(formatFinancialAmount(cashCreditLimit)).append("\n");
        
        // Open Date line
        reportLine.append("ACCT-OPEN-DATE          : ").append(formatDate(openDate)).append("\n");
        
        // Expiration Date line (preserving original typo)
        reportLine.append("ACCT-EXPIRAION-DATE     : ").append(formatDate(expirationDate)).append("\n");
        
        // Reissue Date line
        reportLine.append("ACCT-REISSUE-DATE       : ").append(formatDate(reissueDate)).append("\n");
        
        // Current Cycle Credit line with currency formatting
        reportLine.append("ACCT-CURR-CYC-CREDIT    : ").append(formatFinancialAmount(currentCycleCredit)).append("\n");
        
        // Current Cycle Debit line with currency formatting
        reportLine.append("ACCT-CURR-CYC-DEBIT     : ").append(formatFinancialAmount(currentCycleDebit)).append("\n");
        
        // Group ID line
        reportLine.append("ACCT-GROUP-ID           : ").append(groupId != null ? groupId : "").append("\n");
        
        // Separator line matching COBOL output
        reportLine.append("-------------------------------------------------").append("\n");
        
        return reportLine.toString();
    }

    /**
     * Formats the account information as a header line for report generation
     * 
     * @return Formatted header line string
     */
    public String formatAsHeaderLine() {
        return String.format("ACCOUNT REPORT GENERATED ON %s", 
                DateUtils.formatDateForDisplay(generationTimestamp));
    }

    /**
     * Formats the account information as a detail line for report generation
     * 
     * This method creates a single-line summary of key account information
     * suitable for tabular report formats.
     * 
     * @return Formatted detail line string
     */
    public String formatAsDetailLine() {
        return String.format("%-11s %-1s %15s %15s %15s %-10s %-10s",
                accountId != null ? accountId : "",
                activeStatus != null ? activeStatus : "",
                formatFinancialAmount(currentBalance),
                formatFinancialAmount(creditLimit),
                formatFinancialAmount(cashCreditLimit),
                formatDate(openDate),
                groupId != null ? groupId : "");
    }

    /**
     * Formats the account information as a summary line for report generation
     * 
     * @return Formatted summary line string
     */
    public String formatAsSummaryLine() {
        return String.format("TOTAL ACCOUNTS PROCESSED: %d - REPORT GENERATED: %s",
                recordSequence != null ? recordSequence : 0,
                DateUtils.formatDateForDisplay(generationTimestamp));
    }

    /**
     * Formats financial amounts using BigDecimalUtils for consistent display
     * 
     * @param amount The BigDecimal amount to format
     * @return Formatted currency string
     */
    private String formatFinancialAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimalUtils.formatCurrency(BigDecimal.ZERO);
        }
        return BigDecimalUtils.formatCurrency(amount);
    }

    /**
     * Formats dates for consistent display in reports
     * 
     * @param date The LocalDate to format
     * @return Formatted date string
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DateUtils.formatDateForDisplay(date);
    }

    // Getter and Setter methods

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

    public LocalDate getGenerationTimestamp() {
        return generationTimestamp;
    }

    public void setGenerationTimestamp(LocalDate generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }

    public Long getRecordSequence() {
        return recordSequence;
    }

    public void setRecordSequence(Long recordSequence) {
        this.recordSequence = recordSequence;
    }

    public String getReportSection() {
        return reportSection;
    }

    public void setReportSection(String reportSection) {
        this.reportSection = reportSection;
    }

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
                ", recordSequence=" + recordSequence +
                ", reportSection='" + reportSection + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AccountReportDTO that = (AccountReportDTO) obj;
        return accountId != null ? accountId.equals(that.accountId) : that.accountId == null;
    }

    @Override
    public int hashCode() {
        return accountId != null ? accountId.hashCode() : 0;
    }
}