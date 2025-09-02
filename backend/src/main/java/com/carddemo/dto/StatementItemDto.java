package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO representing individual transaction line items within monthly statements.
 * Contains transaction details, amounts, dates, descriptions, and formatting 
 * information for both plain text and HTML statement generation.
 * 
 * This class supports the conversion from COBOL statement processing programs
 * (CBSTM03A/CBSTM03B) maintaining exact precision for financial calculations
 * and preserving all transaction formatting requirements.
 */
public class StatementItemDto {

    @NotBlank(message = "Transaction ID cannot be blank")
    @Size(max = 16, message = "Transaction ID cannot exceed 16 characters")
    private String transactionId;

    @NotNull(message = "Transaction date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    @NotNull(message = "Post date cannot be null")  
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate postDate;

    @NotBlank(message = "Transaction type cannot be blank")
    @Size(max = 50, message = "Transaction type cannot exceed 50 characters")
    private String transactionType;

    @NotBlank(message = "Transaction type code cannot be blank")
    @Size(max = 4, message = "Transaction type code cannot exceed 4 characters")
    private String transactionTypeCode;

    @Size(max = 100, message = "Merchant name cannot exceed 100 characters")
    private String merchantName;

    @NotBlank(message = "Transaction description cannot be blank")
    @Size(max = 200, message = "Transaction description cannot exceed 200 characters")
    private String transactionDescription;

    @NotNull(message = "Transaction amount cannot be null")
    private BigDecimal amount;

    @NotNull(message = "Running balance cannot be null")
    private BigDecimal runningBalance;

    @Size(max = 10, message = "Authorization code cannot exceed 10 characters")
    private String authCode;

    @Size(max = 20, message = "Reference number cannot exceed 20 characters")
    private String referenceNumber;

    // Formatting flags for different display formats
    private boolean isDebit;
    private boolean isCredit;
    private boolean showInPlainText;
    private boolean showInHtml;

    /**
     * Default constructor
     */
    public StatementItemDto() {
        // Set default scale for monetary amounts to match COBOL COMP-3 precision
        this.amount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.runningBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.showInPlainText = true;
        this.showInHtml = true;
    }

    /**
     * Full constructor with all fields
     */
    public StatementItemDto(String transactionId, LocalDate transactionDate, LocalDate postDate,
                           String transactionType, String transactionTypeCode, String merchantName,
                           String transactionDescription, BigDecimal amount, BigDecimal runningBalance,
                           String authCode, String referenceNumber, boolean isDebit, boolean isCredit) {
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.postDate = postDate;
        this.transactionType = transactionType;
        this.transactionTypeCode = transactionTypeCode;
        this.merchantName = merchantName;
        this.transactionDescription = transactionDescription;
        // Ensure proper scale for monetary amounts matching COBOL COMP-3 behavior
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.runningBalance = runningBalance != null ? runningBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.authCode = authCode;
        this.referenceNumber = referenceNumber;
        this.isDebit = isDebit;
        this.isCredit = isCredit;
        this.showInPlainText = true;
        this.showInHtml = true;
    }

    // Getter methods (required by export schema)
    
    public String getTransactionId() {
        return transactionId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public LocalDate getPostDate() {
        return postDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getTransactionDescription() {
        return transactionDescription;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public boolean isDebit() {
        return isDebit;
    }

    public boolean isCredit() {
        return isCredit;
    }

    public boolean isShowInPlainText() {
        return showInPlainText;
    }

    public boolean isShowInHtml() {
        return showInHtml;
    }

    // Setter methods (some required by export schema)

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setPostDate(LocalDate postDate) {
        this.postDate = postDate;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = transactionDescription;
    }

    public void setAmount(BigDecimal amount) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        // Ensure proper scale matching COBOL COMP-3 precision
        this.runningBalance = runningBalance != null ? runningBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public void setDebit(boolean debit) {
        this.isDebit = debit;
        if (debit) {
            this.isCredit = false; // Ensure mutual exclusivity
        }
    }

    public void setCredit(boolean credit) {
        this.isCredit = credit;
        if (credit) {
            this.isDebit = false; // Ensure mutual exclusivity
        }
    }

    public void setShowInPlainText(boolean showInPlainText) {
        this.showInPlainText = showInPlainText;
    }

    public void setShowInHtml(boolean showInHtml) {
        this.showInHtml = showInHtml;
    }

    /**
     * Convenience setter method for description (required by export schema)
     */
    public void setDescription(String description) {
        this.transactionDescription = description;
    }

    /**
     * Formats the amount for display with proper sign handling matching COBOL output
     */
    public String getFormattedAmount() {
        if (amount == null) {
            return "$0.00";
        }
        
        String sign = "";
        BigDecimal displayAmount = amount;
        
        if (isDebit && amount.compareTo(BigDecimal.ZERO) > 0) {
            sign = "-";
        } else if (isCredit && amount.compareTo(BigDecimal.ZERO) < 0) {
            displayAmount = amount.negate();
        }
        
        return String.format("%s$%,.2f", sign, displayAmount);
    }

    /**
     * Formats the running balance for display with proper sign handling
     */
    public String getFormattedRunningBalance() {
        if (runningBalance == null) {
            return "$0.00";
        }
        
        String sign = runningBalance.compareTo(BigDecimal.ZERO) < 0 ? "-" : "";
        BigDecimal displayBalance = runningBalance.abs();
        
        return String.format("%s$%,.2f", sign, displayBalance);
    }

    /**
     * Returns string representation matching COBOL statement line format
     */
    @Override
    public String toString() {
        return String.format("%-16s %-49s %13s", 
            transactionId != null ? transactionId : "",
            transactionDescription != null ? transactionDescription : "",
            getFormattedAmount());
    }

    /**
     * Equals method for proper comparison (required by export schema)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StatementItemDto that = (StatementItemDto) obj;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(transactionDate, that.transactionDate) &&
               Objects.equals(transactionDescription, that.transactionDescription) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(runningBalance, that.runningBalance);
    }

    /**
     * Hash code method (required by export schema)  
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, transactionDate, transactionDescription, amount, runningBalance);
    }

    // Static utility functions (required by export schema)

    /**
     * Static utility function to set transaction ID on a StatementItemDto instance
     * @param dto The StatementItemDto instance to modify
     * @param transactionId The transaction ID to set
     */
    public static void setTransactionId(StatementItemDto dto, String transactionId) {
        if (dto != null) {
            dto.setTransactionId(transactionId);
        }
    }

    /**
     * Static utility function to set amount on a StatementItemDto instance
     * @param dto The StatementItemDto instance to modify  
     * @param amount The amount to set with proper COBOL COMP-3 precision
     */
    public static void setAmount(StatementItemDto dto, BigDecimal amount) {
        if (dto != null) {
            dto.setAmount(amount);
        }
    }

    /**
     * Static utility function to set transaction date on a StatementItemDto instance
     * @param dto The StatementItemDto instance to modify
     * @param transactionDate The transaction date to set
     */
    public static void setTransactionDate(StatementItemDto dto, LocalDate transactionDate) {
        if (dto != null) {
            dto.setTransactionDate(transactionDate);
        }
    }

    /**
     * Static utility function to set description on a StatementItemDto instance
     * @param dto The StatementItemDto instance to modify
     * @param description The transaction description to set
     */
    public static void setDescription(StatementItemDto dto, String description) {
        if (dto != null) {
            dto.setDescription(description);
        }
    }
}