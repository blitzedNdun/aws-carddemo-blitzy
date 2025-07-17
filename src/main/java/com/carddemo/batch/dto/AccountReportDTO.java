package com.carddemo.batch.dto;

import com.carddemo.account.entity.Account;
import com.carddemo.common.util.BigDecimalUtils;
import com.carddemo.common.util.DateUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object for account report output containing structured report data
 * including account details, financial information, and formatted output lines for
 * Spring Batch FlatFileItemWriter report generation equivalent to COBOL CBACT01C display operations.
 * 
 * This DTO implements the exact structure and formatting logic from the original COBOL 
 * CBACT01C.cbl program, providing formatted report lines that match the original 
 * mainframe output format while supporting modern Spring Batch file generation.
 * 
 * Key Features:
 * - Exact field mapping from COBOL ACCOUNT-RECORD structure (CVACT01Y.cpy)
 * - Report line formatting methods for header, detail, and summary records
 * - BigDecimal precision equivalent to COBOL COMP-3 arithmetic for financial fields
 * - Compatible with Spring Batch FlatFileItemWriter for report file generation
 * - Maintains original COBOL field positioning and alignment
 * 
 * COBOL Source References:
 * - CBACT01C.cbl: Original batch program for account listing reports
 * - CVACT01Y.cpy: Account record structure definition
 * - 1100-DISPLAY-ACCT-RECORD procedure: Display formatting logic
 * 
 * Report Format Equivalent:
 * - Header line: Column headers for account report
 * - Detail line: Individual account information formatted for display
 * - Summary line: Report totals and statistics
 * 
 * Technical Implementation:
 * - Uses BigDecimalUtils for exact financial formatting maintaining COBOL COMP-3 precision
 * - Uses DateUtils for date formatting consistent with COBOL date handling
 * - Supports generation timestamp and record count metadata
 * - Implements complete getter/setter pattern for all fields
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public class AccountReportDTO {
    
    // ===========================
    // COBOL ACCOUNT-RECORD FIELDS
    // ===========================
    
    /**
     * Account ID from COBOL ACCT-ID PIC 9(11)
     * 11-digit account identifier matching original COBOL structure
     */
    private String accountId;
    
    /**
     * Account active status from COBOL ACCT-ACTIVE-STATUS PIC X(01)
     * Single character status indicator ('Y' for active, 'N' for inactive)
     */
    private String activeStatus;
    
    /**
     * Current balance from COBOL ACCT-CURR-BAL PIC S9(10)V99
     * Financial amount with exact BigDecimal precision matching COBOL COMP-3
     */
    private BigDecimal currentBalance;
    
    /**
     * Credit limit from COBOL ACCT-CREDIT-LIMIT PIC S9(10)V99
     * Financial amount with exact BigDecimal precision matching COBOL COMP-3
     */
    private BigDecimal creditLimit;
    
    /**
     * Cash credit limit from COBOL ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
     * Financial amount with exact BigDecimal precision matching COBOL COMP-3
     */
    private BigDecimal cashCreditLimit;
    
    /**
     * Account open date from COBOL ACCT-OPEN-DATE PIC X(10)
     * Date field matching original COBOL date format
     */
    private LocalDate openDate;
    
    /**
     * Account expiration date from COBOL ACCT-EXPIRAION-DATE PIC X(10)
     * Date field matching original COBOL date format (note: preserves original typo)
     */
    private LocalDate expirationDate;
    
    /**
     * Account reissue date from COBOL ACCT-REISSUE-DATE PIC X(10)
     * Date field matching original COBOL date format
     */
    private LocalDate reissueDate;
    
    /**
     * Current cycle credit from COBOL ACCT-CURR-CYC-CREDIT PIC S9(10)V99
     * Financial amount with exact BigDecimal precision matching COBOL COMP-3
     */
    private BigDecimal currentCycleCredit;
    
    /**
     * Current cycle debit from COBOL ACCT-CURR-CYC-DEBIT PIC S9(10)V99
     * Financial amount with exact BigDecimal precision matching COBOL COMP-3
     */
    private BigDecimal currentCycleDebit;
    
    /**
     * Group ID from COBOL ACCT-GROUP-ID PIC X(10)
     * Group identifier for account categorization
     */
    private String groupId;
    
    /**
     * Address ZIP code from COBOL ACCT-ADDR-ZIP PIC X(10)
     * ZIP code field for geographical identification
     */
    private String addressZip;
    
    // ===========================
    // REPORT METADATA FIELDS
    // ===========================
    
    /**
     * Report generation timestamp for audit trail
     * Added for modern report generation requirements
     */
    private LocalDateTime generationTimestamp;
    
    /**
     * Record sequence number for report ordering
     * Added for Spring Batch processing requirements
     */
    private Long recordSequence;
    
    /**
     * Total record count for report statistics
     * Added for report summary generation
     */
    private Long totalRecordCount;
    
    // ===========================
    // FORMATTING CONSTANTS
    // ===========================
    
    /**
     * Column width for account ID display
     * Matches original COBOL field width
     */
    private static final int ACCOUNT_ID_WIDTH = 11;
    
    /**
     * Column width for status display
     * Matches original COBOL field width
     */
    private static final int STATUS_WIDTH = 1;
    
    /**
     * Column width for monetary amount display
     * Supports currency formatting with precision
     */
    private static final int MONETARY_WIDTH = 15;
    
    /**
     * Column width for date display
     * Matches original COBOL date format
     */
    private static final int DATE_WIDTH = 10;
    
    /**
     * Column width for text fields
     * Matches original COBOL field width
     */
    private static final int TEXT_WIDTH = 10;
    
    /**
     * Report line separator matching original COBOL output
     */
    private static final String LINE_SEPARATOR = "-------------------------------------------------";
    
    /**
     * Date formatter for report date display
     */
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    /**
     * Timestamp formatter for report generation timestamp
     */
    private static final DateTimeFormatter REPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ===========================
    // CONSTRUCTORS
    // ===========================
    
    /**
     * Default constructor for Spring Batch and serialization
     */
    public AccountReportDTO() {
        this.generationTimestamp = LocalDateTime.now();
        this.recordSequence = 0L;
        this.totalRecordCount = 0L;
    }
    
    /**
     * Constructor that populates DTO from Account entity
     * Implements exact field mapping from COBOL ACCOUNT-RECORD structure
     * 
     * @param account Account entity containing source data
     */
    public AccountReportDTO(Account account) {
        this();
        
        if (account != null) {
            this.accountId = account.getAccountId();
            this.activeStatus = account.getActiveStatus() != null ? account.getActiveStatus().name() : "N";
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
    
    // ===========================
    // REPORT FORMATTING METHODS
    // ===========================
    
    /**
     * Formats account data as header line for report output
     * Implements header generation matching original COBOL report format
     * 
     * @return formatted header line with column titles
     */
    public String formatAsHeaderLine() {
        StringBuilder header = new StringBuilder();
        
        // Build header line with proper column alignment
        header.append(String.format("%-" + ACCOUNT_ID_WIDTH + "s", "ACCOUNT-ID"));
        header.append(" ");
        header.append(String.format("%-" + STATUS_WIDTH + "s", "S"));
        header.append(" ");
        header.append(String.format("%" + MONETARY_WIDTH + "s", "CURRENT-BALANCE"));
        header.append(" ");
        header.append(String.format("%" + MONETARY_WIDTH + "s", "CREDIT-LIMIT"));
        header.append(" ");
        header.append(String.format("%" + MONETARY_WIDTH + "s", "CASH-LIMIT"));
        header.append(" ");
        header.append(String.format("%-" + DATE_WIDTH + "s", "OPEN-DATE"));
        header.append(" ");
        header.append(String.format("%-" + DATE_WIDTH + "s", "EXP-DATE"));
        header.append(" ");
        header.append(String.format("%-" + TEXT_WIDTH + "s", "GROUP-ID"));
        header.append(System.lineSeparator());
        
        // Add separator line
        header.append(LINE_SEPARATOR);
        
        return header.toString();
    }
    
    /**
     * Formats account data as detail line for report output
     * Implements exact display logic from COBOL 1100-DISPLAY-ACCT-RECORD procedure
     * 
     * @return formatted detail line with account information
     */
    public String formatAsDetailLine() {
        StringBuilder detail = new StringBuilder();
        
        // Format account ID with proper padding
        detail.append(String.format("%-" + ACCOUNT_ID_WIDTH + "s", 
                safeString(accountId, ACCOUNT_ID_WIDTH)));
        detail.append(" ");
        
        // Format active status
        detail.append(String.format("%-" + STATUS_WIDTH + "s", 
                safeString(activeStatus, STATUS_WIDTH)));
        detail.append(" ");
        
        // Format current balance with currency formatting
        detail.append(String.format("%" + MONETARY_WIDTH + "s", 
                formatCurrency(currentBalance)));
        detail.append(" ");
        
        // Format credit limit with currency formatting
        detail.append(String.format("%" + MONETARY_WIDTH + "s", 
                formatCurrency(creditLimit)));
        detail.append(" ");
        
        // Format cash credit limit with currency formatting
        detail.append(String.format("%" + MONETARY_WIDTH + "s", 
                formatCurrency(cashCreditLimit)));
        detail.append(" ");
        
        // Format open date
        detail.append(String.format("%-" + DATE_WIDTH + "s", 
                formatDate(openDate)));
        detail.append(" ");
        
        // Format expiration date
        detail.append(String.format("%-" + DATE_WIDTH + "s", 
                formatDate(expirationDate)));
        detail.append(" ");
        
        // Format group ID
        detail.append(String.format("%-" + TEXT_WIDTH + "s", 
                safeString(groupId, TEXT_WIDTH)));
        
        return detail.toString();
    }
    
    /**
     * Formats account data as summary line for report output
     * Implements summary generation with totals and statistics
     * 
     * @return formatted summary line with report totals
     */
    public String formatAsSummaryLine() {
        StringBuilder summary = new StringBuilder();
        
        // Add separator line
        summary.append(LINE_SEPARATOR);
        summary.append(System.lineSeparator());
        
        // Add generation timestamp
        summary.append("Report Generated: ");
        summary.append(generationTimestamp.format(REPORT_TIMESTAMP_FORMATTER));
        summary.append(System.lineSeparator());
        
        // Add record count
        summary.append("Total Records: ");
        summary.append(totalRecordCount);
        summary.append(System.lineSeparator());
        
        // Add additional summary information
        summary.append("END OF ACCOUNT REPORT");
        
        return summary.toString();
    }
    
    /**
     * Gets the complete report line for Spring Batch FlatFileItemWriter
     * Returns formatted detail line as the primary output format
     * 
     * @return formatted report line for file output
     */
    public String getReportLine() {
        return formatAsDetailLine();
    }
    
    // ===========================
    // FORMATTING UTILITY METHODS
    // ===========================
    
    /**
     * Safely formats a string with proper padding and truncation
     * 
     * @param value the string value to format
     * @param maxLength maximum length for the field
     * @return formatted and safe string
     */
    private String safeString(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        
        return value;
    }
    
    /**
     * Formats a BigDecimal value as currency using BigDecimalUtils
     * Maintains exact COBOL COMP-3 precision for financial display
     * 
     * @param value the BigDecimal value to format
     * @return formatted currency string
     */
    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return String.format("%" + MONETARY_WIDTH + "s", "$0.00");
        }
        
        try {
            String formatted = BigDecimalUtils.formatCurrency(value);
            return String.format("%" + MONETARY_WIDTH + "s", formatted);
        } catch (Exception e) {
            return String.format("%" + MONETARY_WIDTH + "s", "$0.00");
        }
    }
    
    /**
     * Formats a LocalDate value using DateUtils
     * Maintains consistent date formatting across the report
     * 
     * @param date the LocalDate value to format
     * @return formatted date string
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return String.format("%-" + DATE_WIDTH + "s", "");
        }
        
        try {
            return DateUtils.formatDateForDisplay(date);
        } catch (Exception e) {
            return String.format("%-" + DATE_WIDTH + "s", "");
        }
    }
    
    // ===========================
    // GETTER AND SETTER METHODS
    // ===========================
    
    /**
     * Gets the account ID
     * 
     * @return the account ID
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the active status
     * 
     * @return the active status
     */
    public String getActiveStatus() {
        return activeStatus;
    }
    
    /**
     * Sets the active status
     * 
     * @param activeStatus the active status to set
     */
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
    
    /**
     * Gets the current balance
     * 
     * @return the current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    /**
     * Sets the current balance
     * 
     * @param currentBalance the current balance to set
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }
    
    /**
     * Gets the credit limit
     * 
     * @return the credit limit
     */
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
    
    /**
     * Sets the credit limit
     * 
     * @param creditLimit the credit limit to set
     */
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }
    
    /**
     * Gets the cash credit limit
     * 
     * @return the cash credit limit
     */
    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }
    
    /**
     * Sets the cash credit limit
     * 
     * @param cashCreditLimit the cash credit limit to set
     */
    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }
    
    /**
     * Gets the open date
     * 
     * @return the open date
     */
    public LocalDate getOpenDate() {
        return openDate;
    }
    
    /**
     * Sets the open date
     * 
     * @param openDate the open date to set
     */
    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }
    
    /**
     * Gets the expiration date
     * 
     * @return the expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    /**
     * Sets the expiration date
     * 
     * @param expirationDate the expiration date to set
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    /**
     * Gets the reissue date
     * 
     * @return the reissue date
     */
    public LocalDate getReissueDate() {
        return reissueDate;
    }
    
    /**
     * Sets the reissue date
     * 
     * @param reissueDate the reissue date to set
     */
    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }
    
    /**
     * Gets the current cycle credit
     * 
     * @return the current cycle credit
     */
    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }
    
    /**
     * Sets the current cycle credit
     * 
     * @param currentCycleCredit the current cycle credit to set
     */
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = currentCycleCredit;
    }
    
    /**
     * Gets the current cycle debit
     * 
     * @return the current cycle debit
     */
    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }
    
    /**
     * Sets the current cycle debit
     * 
     * @param currentCycleDebit the current cycle debit to set
     */
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = currentCycleDebit;
    }
    
    /**
     * Gets the group ID
     * 
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }
    
    /**
     * Sets the group ID
     * 
     * @param groupId the group ID to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    /**
     * Gets the address ZIP code
     * 
     * @return the address ZIP code
     */
    public String getAddressZip() {
        return addressZip;
    }
    
    /**
     * Sets the address ZIP code
     * 
     * @param addressZip the address ZIP code to set
     */
    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }
    
    /**
     * Gets the generation timestamp
     * 
     * @return the generation timestamp
     */
    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }
    
    /**
     * Sets the generation timestamp
     * 
     * @param generationTimestamp the generation timestamp to set
     */
    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }
    
    /**
     * Gets the record sequence number
     * 
     * @return the record sequence number
     */
    public Long getRecordSequence() {
        return recordSequence;
    }
    
    /**
     * Sets the record sequence number
     * 
     * @param recordSequence the record sequence number to set
     */
    public void setRecordSequence(Long recordSequence) {
        this.recordSequence = recordSequence;
    }
    
    /**
     * Gets the total record count
     * 
     * @return the total record count
     */
    public Long getTotalRecordCount() {
        return totalRecordCount;
    }
    
    /**
     * Sets the total record count
     * 
     * @param totalRecordCount the total record count to set
     */
    public void setTotalRecordCount(Long totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }
    
    // ===========================
    // OBJECT METHODS
    // ===========================
    
    /**
     * String representation of the AccountReportDTO
     * 
     * @return string representation
     */
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
                ", totalRecordCount=" + totalRecordCount +
                '}';
    }
    
    /**
     * Equality comparison based on account ID
     * 
     * @param obj object to compare with
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AccountReportDTO that = (AccountReportDTO) obj;
        return accountId != null ? accountId.equals(that.accountId) : that.accountId == null;
    }
    
    /**
     * Hash code based on account ID
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        return accountId != null ? accountId.hashCode() : 0;
    }
}