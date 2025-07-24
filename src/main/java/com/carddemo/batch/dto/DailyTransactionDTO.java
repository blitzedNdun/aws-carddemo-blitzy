package com.carddemo.batch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.*;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Data Transfer Object representing daily transaction input record structure 
 * from DALYTRAN-RECORD COBOL structure (CVTRA06Y.cpy).
 * 
 * This DTO is used by Spring Batch ItemReader for processing daily transaction 
 * files with exact field correspondence to the original COBOL record layout.
 * Total record length: 350 bytes (excluding 20-byte FILLER)
 * 
 * @author CardDemo Batch Processing Team
 * @version 1.0
 * @since 2024-01-01
 */
public class DailyTransactionDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    
    /**
     * Date/time formatter for COBOL timestamp format (YYYY-MM-DD-HH.MM.SS.SSSSSS)
     */
    private static final DateTimeFormatter COBOL_TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    
    /**
     * Alternative date/time formatter for ISO timestamp format
     */
    private static final DateTimeFormatter ISO_TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    /**
     * Transaction identifier - DALYTRAN-ID field
     * COBOL: PIC X(16)
     */
    @NotNull(message = "Transaction ID is required")
    @Size(min = 1, max = 16, message = "Transaction ID must be between 1 and 16 characters")
    @Pattern(regexp = "^[A-Za-z0-9]{1,16}$", message = "Transaction ID must contain only alphanumeric characters")
    @JsonProperty("transaction_id")
    private String transactionId;

    /**
     * Transaction type code - DALYTRAN-TYPE-CD field  
     * COBOL: PIC X(02)
     */
    @NotNull(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z0-9]{2}$", message = "Transaction type code must be 2 uppercase alphanumeric characters")
    @JsonProperty("transaction_type_code")
    private String transactionTypeCode;

    /**
     * Transaction category code - DALYTRAN-CAT-CD field
     * COBOL: PIC 9(04)
     */
    @NotNull(message = "Transaction category code is required")
    @Size(min = 4, max = 4, message = "Transaction category code must be exactly 4 digits")
    @Pattern(regexp = "^[0-9]{4}$", message = "Transaction category code must be 4 numeric digits")
    @JsonProperty("transaction_category_code")
    private String transactionCategoryCode;

    /**
     * Transaction source - DALYTRAN-SOURCE field
     * COBOL: PIC X(10)
     */
    @NotNull(message = "Transaction source is required")
    @Size(min = 1, max = 10, message = "Transaction source must be between 1 and 10 characters")
    @JsonProperty("transaction_source")
    private String transactionSource;

    /**
     * Transaction description - DALYTRAN-DESC field
     * COBOL: PIC X(100)
     */
    @NotNull(message = "Transaction description is required")
    @Size(min = 1, max = 100, message = "Transaction description must be between 1 and 100 characters")
    @JsonProperty("transaction_description")
    private String transactionDescription;

    /**
     * Transaction amount - DALYTRAN-AMT field
     * COBOL: PIC S9(09)V99 (signed, 9 digits + 2 decimal places)
     * Using BigDecimal to preserve exact COMP-3 arithmetic precision
     */
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "-999999999.99", message = "Transaction amount cannot be less than -999999999.99")
    @DecimalMax(value = "999999999.99", message = "Transaction amount cannot be greater than 999999999.99")
    @Digits(integer = 9, fraction = 2, message = "Transaction amount must have at most 9 integer digits and 2 decimal places")
    @JsonProperty("transaction_amount")
    private BigDecimal transactionAmount;

    /**
     * Merchant identifier - DALYTRAN-MERCHANT-ID field
     * COBOL: PIC 9(09)
     */
    @NotNull(message = "Merchant ID is required")
    @Size(min = 1, max = 9, message = "Merchant ID must be between 1 and 9 digits")
    @Pattern(regexp = "^[0-9]{1,9}$", message = "Merchant ID must contain only numeric digits")
    @JsonProperty("merchant_id")
    private String merchantId;

    /**
     * Merchant name - DALYTRAN-MERCHANT-NAME field
     * COBOL: PIC X(50)
     */
    @NotNull(message = "Merchant name is required")
    @Size(min = 1, max = 50, message = "Merchant name must be between 1 and 50 characters")
    @JsonProperty("merchant_name")
    private String merchantName;

    /**
     * Merchant city - DALYTRAN-MERCHANT-CITY field
     * COBOL: PIC X(50)
     */
    @NotNull(message = "Merchant city is required")
    @Size(min = 1, max = 50, message = "Merchant city must be between 1 and 50 characters")
    @JsonProperty("merchant_city")
    private String merchantCity;

    /**
     * Merchant ZIP code - DALYTRAN-MERCHANT-ZIP field
     * COBOL: PIC X(10)
     */
    @NotNull(message = "Merchant ZIP code is required")
    @Size(min = 5, max = 10, message = "Merchant ZIP code must be between 5 and 10 characters")
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Merchant ZIP code must be in format 12345 or 12345-6789")
    @JsonProperty("merchant_zip")
    private String merchantZip;

    /**
     * Card number - DALYTRAN-CARD-NUM field
     * COBOL: PIC X(16)
     */
    @NotNull(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 characters")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 numeric digits")
    @JsonProperty("card_number")
    private String cardNumber;

    /**
     * Original timestamp - DALYTRAN-ORIG-TS field
     * COBOL: PIC X(26) - Original transaction timestamp
     */
    @NotNull(message = "Original timestamp is required")
    @Size(min = 26, max = 26, message = "Original timestamp must be exactly 26 characters")
    @JsonProperty("original_timestamp")
    private String originalTimestamp;

    /**
     * Processing timestamp - DALYTRAN-PROC-TS field
     * COBOL: PIC X(26) - Processing timestamp  
     */
    @NotNull(message = "Processing timestamp is required")
    @Size(min = 26, max = 26, message = "Processing timestamp must be exactly 26 characters")
    @JsonProperty("processing_timestamp")
    private String processingTimestamp;

    /**
     * Default constructor for Spring Batch ItemReader compatibility
     */
    public DailyTransactionDTO() {
    }

    /**
     * Constructor with essential fields for transaction processing
     * 
     * @param transactionId The unique transaction identifier
     * @param transactionTypeCode The transaction type code
     * @param transactionAmount The transaction amount
     * @param cardNumber The card number used in transaction
     */
    public DailyTransactionDTO(String transactionId, String transactionTypeCode, 
                              BigDecimal transactionAmount, String cardNumber) {
        this.transactionId = transactionId;
        this.transactionTypeCode = transactionTypeCode;
        this.transactionAmount = transactionAmount;
        this.cardNumber = cardNumber;
    }

    // Getter and Setter methods

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = StringUtils.trim(transactionId);
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = StringUtils.trim(transactionTypeCode);
    }

    public String getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    public void setTransactionCategoryCode(String transactionCategoryCode) {
        this.transactionCategoryCode = StringUtils.trim(transactionCategoryCode);
    }

    public String getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(String transactionSource) {
        this.transactionSource = StringUtils.trim(transactionSource);
    }

    public String getTransactionDescription() {
        return transactionDescription;
    }

    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = StringUtils.trim(transactionDescription);
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = StringUtils.trim(merchantId);
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = StringUtils.trim(merchantName);
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public void setMerchantCity(String merchantCity) {
        this.merchantCity = StringUtils.trim(merchantCity);
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = StringUtils.trim(merchantZip);
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = StringUtils.trim(cardNumber);
    }

    public String getOriginalTimestamp() {
        return originalTimestamp;
    }

    public void setOriginalTimestamp(String originalTimestamp) {
        this.originalTimestamp = StringUtils.trim(originalTimestamp);
    }

    public String getProcessingTimestamp() {
        return processingTimestamp;
    }

    public void setProcessingTimestamp(String processingTimestamp) {
        this.processingTimestamp = StringUtils.trim(processingTimestamp);
    }

    // Utility methods for date parsing and formatting

    /**
     * Parses original timestamp string to LocalDateTime
     * Supports both COBOL format (YYYY-MM-DD-HH.MM.SS.SSSSSS) and ISO format
     * 
     * @return LocalDateTime representation of original timestamp
     * @throws DateTimeParseException if timestamp format is invalid
     */
    public LocalDateTime parseOriginalTimestamp() throws DateTimeParseException {
        if (StringUtils.isBlank(originalTimestamp)) {
            throw new DateTimeParseException("Original timestamp is null or empty", "", 0);
        }
        
        try {
            // Try COBOL timestamp format first
            return LocalDateTime.parse(originalTimestamp, COBOL_TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // Fallback to ISO timestamp format
                return LocalDateTime.parse(originalTimestamp, ISO_TIMESTAMP_FORMATTER);
            } catch (DateTimeParseException e2) {
                throw new DateTimeParseException(
                    "Unable to parse original timestamp: " + originalTimestamp + 
                    ". Expected format: YYYY-MM-DD-HH.MM.SS.SSSSSS or YYYY-MM-DD HH:MM:SS.SSSSSS", 
                    originalTimestamp, 0);
            }
        }
    }

    /**
     * Parses processing timestamp string to LocalDateTime
     * Supports both COBOL format (YYYY-MM-DD-HH.MM.SS.SSSSSS) and ISO format
     * 
     * @return LocalDateTime representation of processing timestamp
     * @throws DateTimeParseException if timestamp format is invalid
     */
    public LocalDateTime parseProcessingTimestamp() throws DateTimeParseException {
        if (StringUtils.isBlank(processingTimestamp)) {
            throw new DateTimeParseException("Processing timestamp is null or empty", "", 0);
        }
        
        try {
            // Try COBOL timestamp format first
            return LocalDateTime.parse(processingTimestamp, COBOL_TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // Fallback to ISO timestamp format
                return LocalDateTime.parse(processingTimestamp, ISO_TIMESTAMP_FORMATTER);
            } catch (DateTimeParseException e2) {
                throw new DateTimeParseException(
                    "Unable to parse processing timestamp: " + processingTimestamp + 
                    ". Expected format: YYYY-MM-DD-HH.MM.SS.SSSSSS or YYYY-MM-DD HH:MM:SS.SSSSSS", 
                    processingTimestamp, 0);
            }
        }
    }

    /**
     * Formats LocalDateTime to COBOL timestamp string format
     * 
     * @param dateTime LocalDateTime to format
     * @return Formatted timestamp string in COBOL format
     */
    public static String formatToCobolTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(COBOL_TIMESTAMP_FORMATTER);
    }

    /**
     * Validates that the transaction amount has proper precision for COBOL COMP-3 fields
     * 
     * @return true if amount precision is valid, false otherwise
     */
    public boolean isValidAmountPrecision() {
        if (transactionAmount == null) {
            return false;
        }
        
        // Check that scale (decimal places) is exactly 2
        if (transactionAmount.scale() != 2) {
            return false;
        }
        
        // Check that precision (total digits) does not exceed 11 (9 integer + 2 decimal)
        return transactionAmount.precision() <= 11;
    }

    /**
     * Creates a masked version of the card number for logging and debugging
     * Shows only first 4 and last 4 digits
     * 
     * @return Masked card number (e.g., "1234********5678")
     */
    public String getMaskedCardNumber() {
        if (StringUtils.isBlank(cardNumber) || cardNumber.length() != 16) {
            return "****************";
        }
        
        return cardNumber.substring(0, 4) + "********" + cardNumber.substring(12);
    }

    /**
     * Comprehensive toString method for logging and debugging during batch processing
     * Includes masked card number for security
     * 
     * @return String representation of the DTO
     */
    @Override
    public String toString() {
        return "DailyTransactionDTO{" +
                "transactionId='" + transactionId + '\'' +
                ", transactionTypeCode='" + transactionTypeCode + '\'' +
                ", transactionCategoryCode='" + transactionCategoryCode + '\'' +
                ", transactionSource='" + transactionSource + '\'' +
                ", transactionDescription='" + 
                    (transactionDescription != null && transactionDescription.length() > 50 ? 
                     transactionDescription.substring(0, 50) + "..." : transactionDescription) + '\'' +
                ", transactionAmount=" + transactionAmount +
                ", merchantId='" + merchantId + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", merchantCity='" + merchantCity + '\'' +
                ", merchantZip='" + merchantZip + '\'' +
                ", cardNumber='" + getMaskedCardNumber() + '\'' +
                ", originalTimestamp='" + originalTimestamp + '\'' +
                ", processingTimestamp='" + processingTimestamp + '\'' +
                '}';
    }

    /**
     * Equals method for DTO comparison based on transaction ID and card number
     * 
     * @param obj Object to compare
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DailyTransactionDTO that = (DailyTransactionDTO) obj;
        
        if (transactionId != null ? !transactionId.equals(that.transactionId) : that.transactionId != null)
            return false;
        return cardNumber != null ? cardNumber.equals(that.cardNumber) : that.cardNumber == null;
    }

    /**
     * HashCode method based on transaction ID and card number
     * 
     * @return Hash code for the object
     */
    @Override
    public int hashCode() {
        int result = transactionId != null ? transactionId.hashCode() : 0;
        result = 31 * result + (cardNumber != null ? cardNumber.hashCode() : 0);
        return result;
    }
}