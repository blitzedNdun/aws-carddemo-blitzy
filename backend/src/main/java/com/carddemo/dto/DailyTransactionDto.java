package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Daily Transaction Data Transfer Object
 * 
 * Represents transaction data for daily processing and reporting operations.
 * Translated from COBOL batch program CBTRN03C.cbl transaction record structure.
 * This DTO provides a clean API interface for the daily transaction controller
 * while maintaining compatibility with the internal service layer data structures.
 * 
 * Field mappings preserve COBOL data precision and validation rules:
 * - Transaction ID: COBOL TRANSACTION-ID (PIC X(12))
 * - Account ID: COBOL ACCOUNT-ID (PIC X(11)) 
 * - Card Number: COBOL CARD-NUMBER (PIC X(16))
 * - Type Code: COBOL TRAN-TYPE-CD (PIC X(02))
 * - Category Code: COBOL TRAN-CAT-CD (PIC X(04))
 * - Amount: COBOL TRAN-AMT (PIC S9(9)V99 COMP-3)
 * - Timestamps: COBOL processing timestamps with precision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTransactionDto {

    /**
     * Unique transaction identifier.
     * Maps to TRANSACTION-ID from COBOL program CBTRN03C.
     */
    @NotNull(message = "Transaction ID is required")
    @Size(max = 12, message = "Transaction ID cannot exceed 12 characters")
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Account identifier for the transaction.
     * Maps to ACCOUNT-ID from COBOL program.
     */
    @NotNull(message = "Account ID is required")
    @Size(max = 11, message = "Account ID cannot exceed 11 characters")
    @JsonProperty("accountId")
    private String accountId;

    /**
     * Card number associated with the transaction.
     * Maps to CARD-NUMBER from COBOL program.
     */
    @NotNull(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 characters")
    @Pattern(regexp = "\\d{16}", message = "Card number must contain only digits")
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * Transaction type code.
     * Maps to TRAN-TYPE-CD from COBOL program.
     */
    @NotNull(message = "Transaction type code is required")
    @Size(max = 2, message = "Transaction type code cannot exceed 2 characters")
    @JsonProperty("typeCode")
    private String typeCode;

    /**
     * Transaction category code.
     * Maps to TRAN-CAT-CD from COBOL program.
     */
    @NotNull(message = "Transaction category code is required")
    @Size(max = 4, message = "Transaction category code cannot exceed 4 characters")
    @JsonProperty("categoryCode")
    private String categoryCode;

    /**
     * Transaction amount with COBOL COMP-3 precision.
     * Maps to TRAN-AMT from COBOL program.
     */
    @NotNull(message = "Transaction amount is required")
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Processing timestamp formatted as string for API consistency.
     * Maps to COBOL processing timestamp fields.
     * Format: "yyyy-MM-dd HH:mm:ss" matching COBOL timestamp format.
     */
    @NotNull(message = "Processing timestamp is required")
    @JsonProperty("procTimestamp")
    private String procTimestamp;

    /**
     * Transaction source identifier.
     * Optional field for tracking transaction origin.
     */
    @JsonProperty("source")
    private String source;

    /**
     * Merchant identifier for the transaction.
     * Optional field for merchant tracking.
     */
    @JsonProperty("merchantId")
    private String merchantId;

    /**
     * Merchant name for display purposes.
     * Optional field for user-friendly merchant identification.
     */
    @JsonProperty("merchantName")
    private String merchantName;

    /**
     * Transaction description.
     * Optional field for additional transaction details.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Constructor from LocalDateTime for processing timestamp.
     * Converts LocalDateTime to string format expected by controller.
     * 
     * @param transactionId Transaction ID
     * @param accountId Account ID
     * @param cardNumber Card number
     * @param typeCode Transaction type code
     * @param categoryCode Transaction category code
     * @param amount Transaction amount
     * @param processTimestamp Processing timestamp
     */
    public DailyTransactionDto(String transactionId, String accountId, String cardNumber,
                              String typeCode, String categoryCode, BigDecimal amount,
                              LocalDateTime processTimestamp) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
        this.amount = amount;
        this.procTimestamp = processTimestamp != null ? 
            processTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    /**
     * Get formatted processing timestamp.
     * This method ensures compatibility with controller code that calls getProcTimestamp().
     * 
     * @return Processing timestamp as formatted string
     */
    public String getProcTimestamp() {
        return this.procTimestamp;
    }

    /**
     * Set processing timestamp from LocalDateTime.
     * Utility method for converting from service layer LocalDateTime to string format.
     * 
     * @param processTimestamp LocalDateTime to convert
     */
    public void setProcTimestamp(LocalDateTime processTimestamp) {
        this.procTimestamp = processTimestamp != null ? 
            processTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    /**
     * Utility method to create DTO from service layer DailyTransactionDetail.
     * Provides mapping between internal service objects and external API objects.
     * 
     * @param detail DailyTransactionDetail from service layer
     * @return DailyTransactionDto for API response
     */
    public static DailyTransactionDto fromServiceDetail(Object detail) {
        // This method will be implemented to map from DailyTransactionDetail
        // when the service layer is updated to return the correct type
        return new DailyTransactionDto();
    }

    /**
     * Validate card number format.
     * Implements COBOL card number validation logic.
     * 
     * @return true if card number is valid, false otherwise
     */
    public boolean isValidCardNumber() {
        return cardNumber != null && 
               cardNumber.length() == 16 && 
               cardNumber.matches("\\d{16}");
    }

    /**
     * Get display format for amount.
     * Formats amount for display with 2 decimal places matching COBOL COMP-3 display.
     * 
     * @return Formatted amount string
     */
    public String getFormattedAmount() {
        return amount != null ? String.format("$%.2f", amount) : "$0.00";
    }

    /**
     * Get masked card number for security.
     * Returns card number with middle digits masked, matching COBOL security logic.
     * 
     * @return Masked card number (e.g., "1234********5678")
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "****************";
        }
        return cardNumber.substring(0, 4) + "********" + cardNumber.substring(12);
    }
}