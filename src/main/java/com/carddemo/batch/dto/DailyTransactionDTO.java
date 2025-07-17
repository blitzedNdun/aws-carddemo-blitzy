package com.carddemo.batch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Data Transfer Object representing daily transaction input record structure from DALYTRAN-RECORD COBOL structure.
 * This DTO is used by Spring Batch ItemReader for processing daily transaction files with exact field correspondence
 * and comprehensive validation annotations.
 * 
 * <p>Maps to CVTRA06Y.cpy DALYTRAN-RECORD structure with 350-byte record length:</p>
 * <ul>
 *   <li>DALYTRAN-ID: PIC X(16) - Transaction identifier</li>
 *   <li>DALYTRAN-TYPE-CD: PIC X(02) - Transaction type code</li>
 *   <li>DALYTRAN-CAT-CD: PIC 9(04) - Transaction category code</li>
 *   <li>DALYTRAN-SOURCE: PIC X(10) - Transaction source</li>
 *   <li>DALYTRAN-DESC: PIC X(100) - Transaction description</li>
 *   <li>DALYTRAN-AMT: PIC S9(09)V99 - Transaction amount with COMP-3 precision</li>
 *   <li>DALYTRAN-MERCHANT-ID: PIC 9(09) - Merchant identifier</li>
 *   <li>DALYTRAN-MERCHANT-NAME: PIC X(50) - Merchant name</li>
 *   <li>DALYTRAN-MERCHANT-CITY: PIC X(50) - Merchant city</li>
 *   <li>DALYTRAN-MERCHANT-ZIP: PIC X(10) - Merchant ZIP code</li>
 *   <li>DALYTRAN-CARD-NUM: PIC X(16) - Card number</li>
 *   <li>DALYTRAN-ORIG-TS: PIC X(26) - Original timestamp</li>
 *   <li>DALYTRAN-PROC-TS: PIC X(26) - Processing timestamp</li>
 * </ul>
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public class DailyTransactionDTO {
    
    /**
     * COBOL timestamp format pattern for parsing DALYTRAN-ORIG-TS and DALYTRAN-PROC-TS fields.
     * Format: YYYY-MM-DD HH:MM:SS.ssssss
     */
    private static final DateTimeFormatter COBOL_TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    /**
     * Alternative timestamp format for ISO-8601 compatibility.
     * Format: YYYY-MM-DDTHH:MM:SS.ssssss
     */
    private static final DateTimeFormatter ISO_TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    
    /**
     * Transaction identifier - maps to DALYTRAN-ID PIC X(16).
     * Primary key for transaction identification.
     */
    @NotBlank(message = "Transaction ID cannot be blank")
    @Size(min = 1, max = 16, message = "Transaction ID must be between 1 and 16 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Transaction ID must contain only alphanumeric characters, hyphens, and underscores")
    @JsonProperty("dalytran_id")
    private String transactionId;
    
    /**
     * Transaction type code - maps to DALYTRAN-TYPE-CD PIC X(02).
     * Identifies the type of transaction being processed.
     */
    @NotBlank(message = "Transaction type code cannot be blank")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    @Pattern(regexp = "^[A-Z0-9]{2}$", message = "Transaction type code must be 2 uppercase alphanumeric characters")
    @JsonProperty("dalytran_type_cd")
    private String transactionTypeCode;
    
    /**
     * Transaction category code - maps to DALYTRAN-CAT-CD PIC 9(04).
     * Categorizes the transaction for reporting and processing.
     */
    @NotBlank(message = "Transaction category code cannot be blank")
    @Size(min = 4, max = 4, message = "Transaction category code must be exactly 4 characters")
    @Pattern(regexp = "^[0-9]{4}$", message = "Transaction category code must be 4 numeric digits")
    @JsonProperty("dalytran_cat_cd")
    private String transactionCategoryCode;
    
    /**
     * Transaction source - maps to DALYTRAN-SOURCE PIC X(10).
     * Identifies the source system or channel for the transaction.
     */
    @NotBlank(message = "Transaction source cannot be blank")
    @Size(min = 1, max = 10, message = "Transaction source must be between 1 and 10 characters")
    @JsonProperty("dalytran_source")
    private String transactionSource;
    
    /**
     * Transaction description - maps to DALYTRAN-DESC PIC X(100).
     * Detailed description of the transaction.
     */
    @NotBlank(message = "Transaction description cannot be blank")
    @Size(min = 1, max = 100, message = "Transaction description must be between 1 and 100 characters")
    @JsonProperty("dalytran_desc")
    private String transactionDescription;
    
    /**
     * Transaction amount - maps to DALYTRAN-AMT PIC S9(09)V99.
     * Financial amount with exact COBOL COMP-3 precision using BigDecimal.
     * Supports values from -999,999,999.99 to +999,999,999.99.
     */
    @NotNull(message = "Transaction amount cannot be null")
    @DecimalMin(value = "-999999999.99", message = "Transaction amount cannot be less than -999,999,999.99")
    @DecimalMax(value = "999999999.99", message = "Transaction amount cannot be greater than 999,999,999.99")
    @Digits(integer = 9, fraction = 2, message = "Transaction amount must have at most 9 integer digits and 2 decimal places")
    @JsonProperty("dalytran_amt")
    private BigDecimal transactionAmount;
    
    /**
     * Merchant identifier - maps to DALYTRAN-MERCHANT-ID PIC 9(09).
     * Unique identifier for the merchant.
     */
    @NotBlank(message = "Merchant ID cannot be blank")
    @Size(min = 1, max = 9, message = "Merchant ID must be between 1 and 9 characters")
    @Pattern(regexp = "^[0-9]{1,9}$", message = "Merchant ID must be numeric")
    @JsonProperty("dalytran_merchant_id")
    private String merchantId;
    
    /**
     * Merchant name - maps to DALYTRAN-MERCHANT-NAME PIC X(50).
     * Name of the merchant where the transaction occurred.
     */
    @NotBlank(message = "Merchant name cannot be blank")
    @Size(min = 1, max = 50, message = "Merchant name must be between 1 and 50 characters")
    @JsonProperty("dalytran_merchant_name")
    private String merchantName;
    
    /**
     * Merchant city - maps to DALYTRAN-MERCHANT-CITY PIC X(50).
     * City where the merchant is located.
     */
    @NotBlank(message = "Merchant city cannot be blank")
    @Size(min = 1, max = 50, message = "Merchant city must be between 1 and 50 characters")
    @JsonProperty("dalytran_merchant_city")
    private String merchantCity;
    
    /**
     * Merchant ZIP code - maps to DALYTRAN-MERCHANT-ZIP PIC X(10).
     * ZIP code of the merchant location.
     */
    @NotBlank(message = "Merchant ZIP code cannot be blank")
    @Size(min = 5, max = 10, message = "Merchant ZIP code must be between 5 and 10 characters")
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Merchant ZIP code must be in format 12345 or 12345-6789")
    @JsonProperty("dalytran_merchant_zip")
    private String merchantZipCode;
    
    /**
     * Card number - maps to DALYTRAN-CARD-NUM PIC X(16).
     * Credit card number used for the transaction.
     */
    @NotBlank(message = "Card number cannot be blank")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 characters")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 numeric digits")
    @JsonProperty("dalytran_card_num")
    private String cardNumber;
    
    /**
     * Original timestamp - maps to DALYTRAN-ORIG-TS PIC X(26).
     * Timestamp when the transaction was originally initiated.
     */
    @NotNull(message = "Original timestamp cannot be null")
    @JsonProperty("dalytran_orig_ts")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime originalTimestamp;
    
    /**
     * Processing timestamp - maps to DALYTRAN-PROC-TS PIC X(26).
     * Timestamp when the transaction was processed by the system.
     */
    @NotNull(message = "Processing timestamp cannot be null")
    @JsonProperty("dalytran_proc_ts")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime processingTimestamp;
    
    /**
     * Default constructor for Spring Batch and Jackson deserialization.
     */
    public DailyTransactionDTO() {
        // Default constructor for framework usage
    }
    
    /**
     * Constructor with all required fields for programmatic creation.
     * 
     * @param transactionId Transaction identifier
     * @param transactionTypeCode Transaction type code
     * @param transactionCategoryCode Transaction category code
     * @param transactionSource Transaction source
     * @param transactionDescription Transaction description
     * @param transactionAmount Transaction amount
     * @param merchantId Merchant identifier
     * @param merchantName Merchant name
     * @param merchantCity Merchant city
     * @param merchantZipCode Merchant ZIP code
     * @param cardNumber Card number
     * @param originalTimestamp Original timestamp
     * @param processingTimestamp Processing timestamp
     */
    public DailyTransactionDTO(String transactionId, String transactionTypeCode, 
                             String transactionCategoryCode, String transactionSource, 
                             String transactionDescription, BigDecimal transactionAmount,
                             String merchantId, String merchantName, String merchantCity,
                             String merchantZipCode, String cardNumber, 
                             LocalDateTime originalTimestamp, LocalDateTime processingTimestamp) {
        this.transactionId = transactionId;
        this.transactionTypeCode = transactionTypeCode;
        this.transactionCategoryCode = transactionCategoryCode;
        this.transactionSource = transactionSource;
        this.transactionDescription = transactionDescription;
        this.transactionAmount = transactionAmount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZipCode = merchantZipCode;
        this.cardNumber = cardNumber;
        this.originalTimestamp = originalTimestamp;
        this.processingTimestamp = processingTimestamp;
    }
    
    /**
     * Utility method to parse COBOL timestamp format to LocalDateTime.
     * Handles both COBOL format (YYYY-MM-DD HH:MM:SS.ssssss) and ISO format.
     * 
     * @param timestampString String representation of timestamp
     * @return Parsed LocalDateTime object
     * @throws DateTimeParseException if timestamp format is invalid
     */
    public static LocalDateTime parseCobolTimestamp(String timestampString) {
        if (timestampString == null || timestampString.trim().isEmpty()) {
            return null;
        }
        
        String normalizedTimestamp = timestampString.trim();
        
        try {
            // Try COBOL format first
            return LocalDateTime.parse(normalizedTimestamp, COBOL_TIMESTAMP_FORMAT);
        } catch (DateTimeParseException e) {
            try {
                // Try ISO format as fallback
                return LocalDateTime.parse(normalizedTimestamp, ISO_TIMESTAMP_FORMAT);
            } catch (DateTimeParseException ex) {
                throw new DateTimeParseException(
                    "Unable to parse timestamp: " + timestampString + 
                    ". Expected format: YYYY-MM-DD HH:MM:SS.ssssss or YYYY-MM-DDTHH:MM:SS.ssssss", 
                    timestampString, 0);
            }
        }
    }
    
    /**
     * Utility method to format LocalDateTime to COBOL timestamp format.
     * 
     * @param timestamp LocalDateTime to format
     * @return Formatted timestamp string
     */
    public static String formatCobolTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.format(COBOL_TIMESTAMP_FORMAT);
    }
    
    // Getters and Setters
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }
    
    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }
    
    public String getTransactionCategoryCode() {
        return transactionCategoryCode;
    }
    
    public void setTransactionCategoryCode(String transactionCategoryCode) {
        this.transactionCategoryCode = transactionCategoryCode;
    }
    
    public String getTransactionSource() {
        return transactionSource;
    }
    
    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }
    
    public String getTransactionDescription() {
        return transactionDescription;
    }
    
    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = transactionDescription;
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
        this.merchantId = merchantId;
    }
    
    public String getMerchantName() {
        return merchantName;
    }
    
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    public String getMerchantCity() {
        return merchantCity;
    }
    
    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }
    
    public String getMerchantZipCode() {
        return merchantZipCode;
    }
    
    public void setMerchantZipCode(String merchantZipCode) {
        this.merchantZipCode = merchantZipCode;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public LocalDateTime getOriginalTimestamp() {
        return originalTimestamp;
    }
    
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }
    
    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
    
    /**
     * Setter for original timestamp from string input.
     * Automatically parses COBOL timestamp format.
     * 
     * @param originalTimestampString String representation of timestamp
     */
    public void setOriginalTimestampFromString(String originalTimestampString) {
        this.originalTimestamp = parseCobolTimestamp(originalTimestampString);
    }
    
    /**
     * Setter for processing timestamp from string input.
     * Automatically parses COBOL timestamp format.
     * 
     * @param processingTimestampString String representation of timestamp
     */
    public void setProcessingTimestampFromString(String processingTimestampString) {
        this.processingTimestamp = parseCobolTimestamp(processingTimestampString);
    }
    
    /**
     * Calculates the total record length to verify 350-byte COBOL equivalence.
     * 
     * @return Calculated record length in bytes
     */
    public int calculateRecordLength() {
        int length = 0;
        length += (transactionId != null ? transactionId.length() : 16);           // 16 bytes
        length += (transactionTypeCode != null ? transactionTypeCode.length() : 2); // 2 bytes
        length += (transactionCategoryCode != null ? transactionCategoryCode.length() : 4); // 4 bytes
        length += (transactionSource != null ? transactionSource.length() : 10);   // 10 bytes
        length += (transactionDescription != null ? transactionDescription.length() : 100); // 100 bytes
        length += 11; // Transaction amount (S9(09)V99) = 11 bytes
        length += (merchantId != null ? merchantId.length() : 9);                 // 9 bytes
        length += (merchantName != null ? merchantName.length() : 50);           // 50 bytes
        length += (merchantCity != null ? merchantCity.length() : 50);           // 50 bytes
        length += (merchantZipCode != null ? merchantZipCode.length() : 10);     // 10 bytes
        length += (cardNumber != null ? cardNumber.length() : 16);               // 16 bytes
        length += 26; // Original timestamp = 26 bytes
        length += 26; // Processing timestamp = 26 bytes
        length += 20; // FILLER = 20 bytes
        return length;
    }
    
    /**
     * Validates that all required fields are present and conform to COBOL constraints.
     * 
     * @return true if all validations pass, false otherwise
     */
    @JsonIgnore
    public boolean isValid() {
        return transactionId != null && !transactionId.trim().isEmpty() &&
               transactionTypeCode != null && transactionTypeCode.length() == 2 &&
               transactionCategoryCode != null && transactionCategoryCode.length() == 4 &&
               transactionSource != null && !transactionSource.trim().isEmpty() &&
               transactionDescription != null && !transactionDescription.trim().isEmpty() &&
               transactionAmount != null &&
               merchantId != null && !merchantId.trim().isEmpty() &&
               merchantName != null && !merchantName.trim().isEmpty() &&
               merchantCity != null && !merchantCity.trim().isEmpty() &&
               merchantZipCode != null && !merchantZipCode.trim().isEmpty() &&
               cardNumber != null && cardNumber.length() == 16 &&
               originalTimestamp != null &&
               processingTimestamp != null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DailyTransactionDTO that = (DailyTransactionDTO) obj;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(transactionTypeCode, that.transactionTypeCode) &&
               Objects.equals(transactionCategoryCode, that.transactionCategoryCode) &&
               Objects.equals(transactionSource, that.transactionSource) &&
               Objects.equals(transactionDescription, that.transactionDescription) &&
               Objects.equals(transactionAmount, that.transactionAmount) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(merchantName, that.merchantName) &&
               Objects.equals(merchantCity, that.merchantCity) &&
               Objects.equals(merchantZipCode, that.merchantZipCode) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(originalTimestamp, that.originalTimestamp) &&
               Objects.equals(processingTimestamp, that.processingTimestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, transactionTypeCode, transactionCategoryCode,
                           transactionSource, transactionDescription, transactionAmount,
                           merchantId, merchantName, merchantCity, merchantZipCode,
                           cardNumber, originalTimestamp, processingTimestamp);
    }
    
    /**
     * String representation for logging and debugging during batch processing.
     * Masks sensitive data (card number) for security.
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
               ", transactionDescription='" + transactionDescription + '\'' +
               ", transactionAmount=" + transactionAmount +
               ", merchantId='" + merchantId + '\'' +
               ", merchantName='" + merchantName + '\'' +
               ", merchantCity='" + merchantCity + '\'' +
               ", merchantZipCode='" + merchantZipCode + '\'' +
               ", cardNumber='" + maskCardNumber(cardNumber) + '\'' +
               ", originalTimestamp=" + originalTimestamp +
               ", processingTimestamp=" + processingTimestamp +
               ", recordLength=" + calculateRecordLength() +
               '}';
    }
    
    /**
     * Masks card number for secure logging.
     * Shows only first 4 and last 4 digits.
     * 
     * @param cardNumber Full card number
     * @return Masked card number
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "********" + cardNumber.substring(12);
    }
}