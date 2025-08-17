package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * DTO containing comprehensive monthly statement data including account information,
 * customer details, billing cycle dates, transaction summaries, balance calculations,
 * and statement formatting parameters for both plain text and HTML generation.
 * 
 * This class supports the conversion from COBOL statement processing programs
 * (CBSTM03A/CBSTM03B) maintaining exact formatting and precision requirements
 * for financial statement generation while providing modern REST API compatibility.
 */
public class StatementDto {

    // Basic Account Information
    @NotBlank(message = "Account ID cannot be blank")
    @Size(max = 20, message = "Account ID cannot exceed 20 characters")
    private String accountId;

    @NotBlank(message = "Customer ID cannot be blank")
    @Size(max = 9, message = "Customer ID cannot exceed 9 characters")
    private String customerId;

    // Customer Details
    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = 75, message = "Customer name cannot exceed 75 characters")
    private String customerName;

    @Size(max = 50, message = "Address line 1 cannot exceed 50 characters")
    private String customerAddressLine1;

    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String customerAddressLine2;

    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String customerAddressLine3;

    @Size(max = 2, message = "State code cannot exceed 2 characters")
    private String customerStateCode;

    @Size(max = 3, message = "Country code cannot exceed 3 characters")
    private String customerCountryCode;

    @Size(max = 10, message = "ZIP code cannot exceed 10 characters")
    private String customerZipCode;

    @Size(max = 20, message = "FICO score field cannot exceed 20 characters")
    private String ficoScore;

    // Statement Period Information
    @NotNull(message = "Statement date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementDate;

    @NotNull(message = "Statement period start cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementPeriodStart;

    @NotNull(message = "Statement period end cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementPeriodEnd;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDueDate;

    // Balance Information (using BigDecimal with scale 2 for COBOL COMP-3 precision)
    @NotNull(message = "Previous balance cannot be null")
    private BigDecimal previousBalance;

    @NotNull(message = "Current balance cannot be null")
    private BigDecimal currentBalance;

    @NotNull(message = "Credit limit cannot be null")
    private BigDecimal creditLimit;

    @NotNull(message = "Minimum payment cannot be null")
    private BigDecimal minimumPayment;

    // Transaction Summary Totals
    @NotNull(message = "Total credits cannot be null")
    private BigDecimal totalCredits;

    @NotNull(message = "Total debits cannot be null")
    private BigDecimal totalDebits;

    @NotNull(message = "Total fees cannot be null")
    private BigDecimal totalFees;

    @NotNull(message = "Total interest cannot be null")
    private BigDecimal totalInterest;

    // Transaction Count and Details
    private int transactionCount;

    @Valid
    @NotNull(message = "Transaction summary list cannot be null")
    private List<StatementItemDto> transactionSummary;

    // Formatting Parameters
    private boolean generatePlainText;
    private boolean generateHtml;
    private String plainTextFormat;
    private String htmlFormat;

    /**
     * Default constructor initializing all BigDecimal fields with proper scale
     * and setting up default formatting options.
     */
    public StatementDto() {
        // Initialize all monetary fields with scale 2 for COBOL COMP-3 precision
        this.previousBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currentBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.creditLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.minimumPayment = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalCredits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalDebits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalFees = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        
        // Initialize transaction list
        this.transactionSummary = new ArrayList<>();
        this.transactionCount = 0;
        
        // Set default formatting options
        this.generatePlainText = true;
        this.generateHtml = true;
        this.plainTextFormat = "standard";
        this.htmlFormat = "table";
    }

    // Getter methods (required by export schema)

    public String getAccountId() {
        return accountId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerAddressLine1() {
        return customerAddressLine1;
    }

    public String getCustomerAddressLine2() {
        return customerAddressLine2;
    }

    public String getCustomerAddressLine3() {
        return customerAddressLine3;
    }

    public String getCustomerStateCode() {
        return customerStateCode;
    }

    public String getCustomerCountryCode() {
        return customerCountryCode;
    }

    public String getCustomerZipCode() {
        return customerZipCode;
    }

    public String getFicoScore() {
        return ficoScore;
    }

    public LocalDate getStatementDate() {
        return statementDate;
    }

    public LocalDate getStatementPeriodStart() {
        return statementPeriodStart;
    }

    public LocalDate getStatementPeriodEnd() {
        return statementPeriodEnd;
    }

    public LocalDate getPaymentDueDate() {
        return paymentDueDate;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getMinimumPayment() {
        return minimumPayment;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public List<StatementItemDto> getTransactionSummary() {
        return transactionSummary;
    }

    public boolean isGeneratePlainText() {
        return generatePlainText;
    }

    public boolean isGenerateHtml() {
        return generateHtml;
    }

    public String getPlainTextFormat() {
        return plainTextFormat;
    }

    public String getHtmlFormat() {
        return htmlFormat;
    }

    // Setter methods (some required by export schema)

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setCustomerAddressLine1(String customerAddressLine1) {
        this.customerAddressLine1 = customerAddressLine1;
    }

    public void setCustomerAddressLine2(String customerAddressLine2) {
        this.customerAddressLine2 = customerAddressLine2;
    }

    public void setCustomerAddressLine3(String customerAddressLine3) {
        this.customerAddressLine3 = customerAddressLine3;
    }

    public void setCustomerStateCode(String customerStateCode) {
        this.customerStateCode = customerStateCode;
    }

    public void setCustomerCountryCode(String customerCountryCode) {
        this.customerCountryCode = customerCountryCode;
    }

    public void setCustomerZipCode(String customerZipCode) {
        this.customerZipCode = customerZipCode;
    }

    public void setFicoScore(String ficoScore) {
        this.ficoScore = ficoScore;
    }

    public void setStatementDate(LocalDate statementDate) {
        this.statementDate = statementDate;
    }

    public void setStatementPeriodStart(LocalDate statementPeriodStart) {
        this.statementPeriodStart = statementPeriodStart;
    }

    public void setStatementPeriodEnd(LocalDate statementPeriodEnd) {
        this.statementPeriodEnd = statementPeriodEnd;
    }

    public void setPaymentDueDate(LocalDate paymentDueDate) {
        this.paymentDueDate = paymentDueDate;
    }

    public void setPreviousBalance(BigDecimal previousBalance) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.previousBalance = previousBalance != null ? 
            previousBalance.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.creditLimit = creditLimit != null ? 
            creditLimit.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setMinimumPayment(BigDecimal minimumPayment) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.minimumPayment = minimumPayment != null ? 
            minimumPayment.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.totalCredits = totalCredits != null ? 
            totalCredits.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.totalDebits = totalDebits != null ? 
            totalDebits.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setTotalFees(BigDecimal totalFees) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.totalFees = totalFees != null ? 
            totalFees.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.totalInterest = totalInterest != null ? 
            totalInterest.setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO.setScale(2);
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public void setTransactionSummary(List<StatementItemDto> transactionSummary) {
        this.transactionSummary = transactionSummary != null ? transactionSummary : new ArrayList<>();
        // Update transaction count automatically
        this.transactionCount = this.transactionSummary.size();
    }

    public void setGeneratePlainText(boolean generatePlainText) {
        this.generatePlainText = generatePlainText;
    }

    public void setGenerateHtml(boolean generateHtml) {
        this.generateHtml = generateHtml;
    }

    public void setPlainTextFormat(String plainTextFormat) {
        this.plainTextFormat = plainTextFormat;
    }

    public void setHtmlFormat(String htmlFormat) {
        this.htmlFormat = htmlFormat;
    }

    /**
     * Convenience method to add a transaction item to the summary
     * and automatically update the transaction count.
     */
    public void addTransactionItem(StatementItemDto item) {
        if (item != null) {
            this.transactionSummary.add(item);
            this.transactionCount = this.transactionSummary.size();
        }
    }

    /**
     * Convenience method to clear all transaction items
     */
    public void clearTransactionItems() {
        this.transactionSummary.clear();
        this.transactionCount = 0;
    }

    /**
     * Calculates and returns the available credit based on current balance and credit limit
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currentBalance == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return creditLimit.subtract(currentBalance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Formats the customer address as a single string, matching COBOL ST-ADD3 pattern
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        
        if (customerAddressLine1 != null && !customerAddressLine1.trim().isEmpty()) {
            address.append(customerAddressLine1.trim());
        }
        
        if (customerAddressLine2 != null && !customerAddressLine2.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(customerAddressLine2.trim());
        }
        
        if (customerAddressLine3 != null && !customerAddressLine3.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(customerAddressLine3.trim());
        }
        
        // Add state, country, and ZIP following COBOL pattern
        StringBuilder locationPart = new StringBuilder();
        if (customerStateCode != null && !customerStateCode.trim().isEmpty()) {
            locationPart.append(customerStateCode.trim());
        }
        
        if (customerCountryCode != null && !customerCountryCode.trim().isEmpty()) {
            if (locationPart.length() > 0) locationPart.append(" ");
            locationPart.append(customerCountryCode.trim());
        }
        
        if (customerZipCode != null && !customerZipCode.trim().isEmpty()) {
            if (locationPart.length() > 0) locationPart.append(" ");
            locationPart.append(customerZipCode.trim());
        }
        
        if (locationPart.length() > 0) {
            if (address.length() > 0) address.append(", ");
            address.append(locationPart.toString());
        }
        
        return address.toString();
    }

    /**
     * Returns string representation matching COBOL statement format.
     * This method is required by the export schema.
     */
    @Override
    public String toString() {
        StringBuilder statement = new StringBuilder();
        
        // Header section matching COBOL ST-LINE patterns
        statement.append("*******************START OF STATEMENT*******************\n");
        statement.append(String.format("%-75s%5s\n", customerName != null ? customerName : "", ""));
        statement.append(String.format("%-50s%30s\n", customerAddressLine1 != null ? customerAddressLine1 : "", ""));
        statement.append(String.format("%-50s%30s\n", customerAddressLine2 != null ? customerAddressLine2 : "", ""));
        statement.append(String.format("%-80s\n", getFormattedAddress()));
        statement.append("--------------------------------------------------------------------------------\n");
        statement.append(String.format("%33s%14s%33s\n", "", "Basic Details", ""));
        statement.append("--------------------------------------------------------------------------------\n");
        statement.append(String.format("Account ID         : %-20s%40s\n", accountId != null ? accountId : "", ""));
        
        // Format current balance to match COBOL PIC 9(9).99- pattern
        String balanceStr = currentBalance != null ? 
            String.format("%12.2f", currentBalance.doubleValue()) : "0.00";
        statement.append(String.format("Current Balance    : %s%7s%40s\n", balanceStr, "", ""));
        
        statement.append(String.format("FICO Score         : %-20s%40s\n", ficoScore != null ? ficoScore : "", ""));
        statement.append("--------------------------------------------------------------------------------\n");
        statement.append(String.format("%30s%20s%30s\n", "", "TRANSACTION SUMMARY", ""));
        statement.append("--------------------------------------------------------------------------------\n");
        statement.append(String.format("%-16s%-51s%13s\n", "Tran ID", "Tran Details", "Tran Amount"));
        statement.append("--------------------------------------------------------------------------------\n");
        
        // Add transaction items
        if (transactionSummary != null) {
            for (StatementItemDto item : transactionSummary) {
                statement.append(item.toString()).append("\n");
            }
        }
        
        // Total section matching COBOL ST-LINE14A pattern
        String totalStr = totalDebits != null ? 
            String.format("%12.2f", totalDebits.doubleValue()) : "0.00";
        statement.append(String.format("Total EXP:%56s$%s\n", "", totalStr));
        statement.append("******************END OF STATEMENT******************\n");
        
        return statement.toString();
    }

    /**
     * Equals method for proper comparison. This method is required by the export schema.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StatementDto that = (StatementDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(statementDate, that.statementDate) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(previousBalance, that.previousBalance) &&
               Objects.equals(transactionSummary, that.transactionSummary);
    }

    /**
     * Hash code method. This method is required by the export schema.
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountId, customerId, statementDate, currentBalance, 
                           previousBalance, transactionSummary);
    }
    // Static utility functions (required by export schema)

    /**
     * Static utility function to set account ID on a StatementDto instance
     * @param dto The StatementDto instance to modify
     * @param accountId The account ID to set
     */
    public static void setAccountId(StatementDto dto, String accountId) {
        if (dto != null) {
            dto.setAccountId(accountId);
        }
    }

    /**
     * Static utility function to set customer ID on a StatementDto instance
     * @param dto The StatementDto instance to modify
     * @param customerId The customer ID to set
     */
    public static void setCustomerId(StatementDto dto, String customerId) {
        if (dto != null) {
            dto.setCustomerId(customerId);
        }
    }

    /**
     * Static utility function to set current balance on a StatementDto instance
     * @param dto The StatementDto instance to modify
     * @param currentBalance The current balance to set with proper COBOL COMP-3 precision
     */
    public static void setCurrentBalance(StatementDto dto, BigDecimal currentBalance) {
        if (dto != null) {
            dto.setCurrentBalance(currentBalance);
        }
    }

    /**
     * Static utility function to set previous balance on a StatementDto instance
     * @param dto The StatementDto instance to modify
     * @param previousBalance The previous balance to set with proper COBOL COMP-3 precision
     */
    public static void setPreviousBalance(StatementDto dto, BigDecimal previousBalance) {
        if (dto != null) {
            dto.setPreviousBalance(previousBalance);
        }
    }
}