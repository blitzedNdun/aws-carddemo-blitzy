package com.carddemo.batch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Data Transfer Object representing daily transaction input record structure from DALYTRAN-RECORD COBOL structure.
 * Used by Spring Batch ItemReader for processing daily transaction files with exact field correspondence
 * and comprehensive validation annotations.
 * 
 * This DTO maps exactly to the DALYTRAN-RECORD structure from CVTRA06Y.cpy:
 * - Total record length: 350 bytes
 * - Preserves COBOL field layouts and data types
 * - Supports Spring Batch ItemReader input processing
 * - Implements BigDecimal precision for financial amounts equivalent to COBOL COMP-3 arithmetic
 * - Handles date field conversion compatible with COBOL timestamp formats
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-15
 */
public class DailyTransactionDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // COBOL date format for timestamp parsing
    private static final DateTimeFormatter COBOL_TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    /**
     * DALYTRAN-ID - Transaction identifier (PIC X(16))
     * Maps to: DALYTRAN-ID from CVTRA06Y.cpy
     */
    @NotBlank(message = "Transaction ID cannot be blank")
    @Size(min = 16, max = 16, message = "Transaction ID must be exactly 16 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Transaction ID must contain only alphanumeric characters")
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * DALYTRAN-TYPE-CD - Transaction type code (PIC X(02))
     * Maps to: DALYTRAN-TYPE-CD from CVTRA06Y.cpy
     */
    @NotBlank(message = "Transaction type code cannot be blank")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    @JsonProperty("typeCode")
    private String typeCode;

    /**
     * DALYTRAN-CAT-CD - Transaction category code (PIC 9(04))
     * Maps to: DALYTRAN-CAT-CD from CVTRA06Y.cpy
     */
    @NotNull(message = "Transaction category code cannot be null")
    @Min(value = 0, message = "Transaction category code must be non-negative")
    @Max(value = 9999, message = "Transaction category code must not exceed 9999")
    @JsonProperty("categoryCode")
    private Integer categoryCode;

    /**
     * DALYTRAN-SOURCE - Transaction source (PIC X(10))
     * Maps to: DALYTRAN-SOURCE from CVTRA06Y.cpy
     */
    @NotBlank(message = "Transaction source cannot be blank")
    @Size(max = 10, message = "Transaction source must not exceed 10 characters")
    @JsonProperty("source")
    private String source;

    /**
     * DALYTRAN-DESC - Transaction description (PIC X(100))
     * Maps to: DALYTRAN-DESC from CVTRA06Y.cpy
     */
    @NotBlank(message = "Transaction description cannot be blank")
    @Size(max = 100, message = "Transaction description must not exceed 100 characters")
    @JsonProperty("description")
    private String description;

    /**
     * DALYTRAN-AMT - Transaction amount (PIC S9(09)V99)
     * Maps to: DALYTRAN-AMT from CVTRA06Y.cpy
     * Precision: 11 total digits (9 integer + 2 decimal places)
     * Equivalent to COBOL COMP-3 arithmetic with BigDecimal precision
     */
    @NotNull(message = "Transaction amount cannot be null")
    @Digits(integer = 9, fraction = 2, message = "Transaction amount must have at most 9 integer digits and 2 decimal places")
    @DecimalMin(value = "-999999999.99", message = "Transaction amount must be greater than or equal to -999999999.99")
    @DecimalMax(value = "999999999.99", message = "Transaction amount must be less than or equal to 999999999.99")
    @JsonProperty("amount")
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private BigDecimal amount;

    /**
     * DALYTRAN-MERCHANT-ID - Merchant identifier (PIC 9(09))
     * Maps to: DALYTRAN-MERCHANT-ID from CVTRA06Y.cpy
     */
    @NotNull(message = "Merchant ID cannot be null")
    @Min(value = 0, message = "Merchant ID must be non-negative")
    @Max(value = 999999999L, message = "Merchant ID must not exceed 999999999")
    @JsonProperty("merchantId")
    private Long merchantId;

    /**
     * DALYTRAN-MERCHANT-NAME - Merchant name (PIC X(50))
     * Maps to: DALYTRAN-MERCHANT-NAME from CVTRA06Y.cpy
     */
    @NotBlank(message = "Merchant name cannot be blank")
    @Size(max = 50, message = "Merchant name must not exceed 50 characters")
    @JsonProperty("merchantName")
    private String merchantName;

    /**
     * DALYTRAN-MERCHANT-CITY - Merchant city (PIC X(50))
     * Maps to: DALYTRAN-MERCHANT-CITY from CVTRA06Y.cpy
     */
    @NotBlank(message = "Merchant city cannot be blank")
    @Size(max = 50, message = "Merchant city must not exceed 50 characters")
    @JsonProperty("merchantCity")
    private String merchantCity;

    /**
     * DALYTRAN-MERCHANT-ZIP - Merchant ZIP code (PIC X(10))
     * Maps to: DALYTRAN-MERCHANT-ZIP from CVTRA06Y.cpy
     */
    @NotBlank(message = "Merchant ZIP code cannot be blank")
    @Size(max = 10, message = "Merchant ZIP code must not exceed 10 characters")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$|^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$", 
             message = "Merchant ZIP code must be valid US ZIP or Canadian postal code format")
    @JsonProperty("merchantZip")
    private String merchantZip;

    /**
     * DALYTRAN-CARD-NUM - Card number (PIC X(16))
     * Maps to: DALYTRAN-CARD-NUM from CVTRA06Y.cpy
     */
    @NotBlank(message = "Card number cannot be blank")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 characters")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must contain exactly 16 digits")
    @JsonProperty("cardNumber")
    private String cardNumber;

    /**
     * DALYTRAN-ORIG-TS - Original timestamp (PIC X(26))
     * Maps to: DALYTRAN-ORIG-TS from CVTRA06Y.cpy
     * Handles COBOL timestamp format conversion
     */
    @NotNull(message = "Original timestamp cannot be null")
    @JsonProperty("originalTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime originalTimestamp;

    /**
     * DALYTRAN-PROC-TS - Processing timestamp (PIC X(26))
     * Maps to: DALYTRAN-PROC-TS from CVTRA06Y.cpy
     * Handles COBOL timestamp format conversion
     */
    @NotNull(message = "Processing timestamp cannot be null")
    @JsonProperty("processingTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime processingTimestamp;

    /**
     * Default constructor for Jackson deserialization and Spring Batch ItemReader
     */
    public DailyTransactionDTO() {
        // Initialize BigDecimal with zero to ensure proper decimal precision
        this.amount = BigDecimal.ZERO.setScale(2);
    }

    /**
     * Full constructor for complete DTO initialization
     *
     * @param transactionId Transaction identifier
     * @param typeCode Transaction type code
     * @param categoryCode Transaction category code
     * @param source Transaction source
     * @param description Transaction description
     * @param amount Transaction amount with COBOL precision
     * @param merchantId Merchant identifier
     * @param merchantName Merchant name
     * @param merchantCity Merchant city
     * @param merchantZip Merchant ZIP code
     * @param cardNumber Card number
     * @param originalTimestamp Original timestamp
     * @param processingTimestamp Processing timestamp
     */
    public DailyTransactionDTO(String transactionId, String typeCode, Integer categoryCode, 
                              String source, String description, BigDecimal amount,
                              Long merchantId, String merchantName, String merchantCity, 
                              String merchantZip, String cardNumber, 
                              LocalDateTime originalTimestamp, LocalDateTime processingTimestamp) {
        this.transactionId = transactionId;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
        this.source = source;
        this.description = description;
        // Ensure BigDecimal maintains COBOL COMP-3 equivalent precision
        this.amount = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2);
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
        this.cardNumber = cardNumber;
        this.originalTimestamp = originalTimestamp;
        this.processingTimestamp = processingTimestamp;
    }

    // Getters and Setters with validation preservation

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public Integer getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(Integer categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets transaction amount with COBOL COMP-3 equivalent precision maintenance
     *
     * @param amount Transaction amount to set
     */
    public void setAmount(BigDecimal amount) {
        // Ensure precision matches COBOL PIC S9(09)V99 format
        this.amount = amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2);
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
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

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
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

    // Utility methods for COBOL timestamp conversion

    /**
     * Parses COBOL timestamp string to LocalDateTime
     * Handles COBOL PIC X(26) timestamp format conversion
     *
     * @param cobolTimestamp COBOL formatted timestamp string
     * @return LocalDateTime object or null if parsing fails
     */
    public static LocalDateTime parseCobolTimestamp(String cobolTimestamp) {
        if (cobolTimestamp == null || cobolTimestamp.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Handle COBOL timestamp format: "yyyy-MM-dd HH:mm:ss.SSSSSS"
            String cleanedTimestamp = cobolTimestamp.trim();
            if (cleanedTimestamp.length() > 26) {
                cleanedTimestamp = cleanedTimestamp.substring(0, 26);
            }
            return LocalDateTime.parse(cleanedTimestamp, COBOL_TIMESTAMP_FORMATTER);
        } catch (Exception e) {
            // Log parsing error and return null for invalid timestamps
            return null;
        }
    }

    /**
     * Formats LocalDateTime to COBOL timestamp string
     * Generates COBOL PIC X(26) compatible timestamp format
     *
     * @param dateTime LocalDateTime to format
     * @return COBOL formatted timestamp string or empty string if null
     */
    public static String formatToCobolTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(COBOL_TIMESTAMP_FORMATTER);
    }

    /**
     * Validates that all mandatory fields conform to COBOL record structure requirements
     *
     * @return true if all mandatory fields are valid, false otherwise
     */
    public boolean isValidCobolRecord() {
        return transactionId != null && transactionId.length() == 16 &&
               typeCode != null && typeCode.length() == 2 &&
               categoryCode != null && categoryCode >= 0 && categoryCode <= 9999 &&
               source != null && !source.trim().isEmpty() &&
               description != null && !description.trim().isEmpty() &&
               amount != null &&
               merchantId != null && merchantId >= 0 &&
               merchantName != null && !merchantName.trim().isEmpty() &&
               merchantCity != null && !merchantCity.trim().isEmpty() &&
               merchantZip != null && !merchantZip.trim().isEmpty() &&
               cardNumber != null && cardNumber.length() == 16 &&
               originalTimestamp != null &&
               processingTimestamp != null;
    }

    /**
     * Calculates total record size equivalent to COBOL 350-byte structure
     *
     * @return Estimated byte size of the record
     */
    public int getCobolRecordSize() {
        int size = 0;
        size += (transactionId != null ? transactionId.length() : 16);      // DALYTRAN-ID: 16 bytes
        size += (typeCode != null ? typeCode.length() : 2);                 // DALYTRAN-TYPE-CD: 2 bytes
        size += 4;                                                           // DALYTRAN-CAT-CD: 4 bytes (PIC 9(04))
        size += (source != null ? source.length() : 10);                    // DALYTRAN-SOURCE: 10 bytes
        size += (description != null ? description.length() : 100);         // DALYTRAN-DESC: 100 bytes
        size += 11;                                                          // DALYTRAN-AMT: 11 bytes (S9(09)V99)
        size += 9;                                                           // DALYTRAN-MERCHANT-ID: 9 bytes
        size += (merchantName != null ? merchantName.length() : 50);        // DALYTRAN-MERCHANT-NAME: 50 bytes
        size += (merchantCity != null ? merchantCity.length() : 50);        // DALYTRAN-MERCHANT-CITY: 50 bytes
        size += (merchantZip != null ? merchantZip.length() : 10);          // DALYTRAN-MERCHANT-ZIP: 10 bytes
        size += (cardNumber != null ? cardNumber.length() : 16);            // DALYTRAN-CARD-NUM: 16 bytes
        size += 26;                                                          // DALYTRAN-ORIG-TS: 26 bytes
        size += 26;                                                          // DALYTRAN-PROC-TS: 26 bytes
        size += 20;                                                          // FILLER: 20 bytes
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyTransactionDTO that = (DailyTransactionDTO) o;
        return Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(typeCode, that.typeCode) &&
               Objects.equals(categoryCode, that.categoryCode) &&
               Objects.equals(source, that.source) &&
               Objects.equals(description, that.description) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(merchantName, that.merchantName) &&
               Objects.equals(merchantCity, that.merchantCity) &&
               Objects.equals(merchantZip, that.merchantZip) &&
               Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(originalTimestamp, that.originalTimestamp) &&
               Objects.equals(processingTimestamp, that.processingTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, typeCode, categoryCode, source, description, 
                           amount, merchantId, merchantName, merchantCity, merchantZip, 
                           cardNumber, originalTimestamp, processingTimestamp);
    }

    /**
     * Comprehensive toString method for logging and debugging during batch processing
     * Includes all field values with COBOL structure mapping information
     *
     * @return Detailed string representation of the DTO
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DailyTransactionDTO{");
        sb.append("transactionId='").append(transactionId).append('\'');
        sb.append(", typeCode='").append(typeCode).append('\'');
        sb.append(", categoryCode=").append(categoryCode);
        sb.append(", source='").append(source).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", merchantId=").append(merchantId);
        sb.append(", merchantName='").append(merchantName).append('\'');
        sb.append(", merchantCity='").append(merchantCity).append('\'');
        sb.append(", merchantZip='").append(merchantZip).append('\'');
        sb.append(", cardNumber='").append(cardNumber != null ? maskCardNumber(cardNumber) : null).append('\'');
        sb.append(", originalTimestamp=").append(originalTimestamp);
        sb.append(", processingTimestamp=").append(processingTimestamp);
        sb.append(", cobolRecordSize=").append(getCobolRecordSize());
        sb.append(", isValidCobolRecord=").append(isValidCobolRecord());
        sb.append('}');
        return sb.toString();
    }

    /**
     * Masks card number for secure logging (shows only first 4 and last 4 digits)
     *
     * @param cardNumber Full card number
     * @return Masked card number for logging
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "****-****-****-****";
        }
        return cardNumber.substring(0, 4) + "-****-****-" + cardNumber.substring(12);
    }
}