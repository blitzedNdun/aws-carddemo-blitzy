/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.carddemo.util.FormatUtil;
import com.carddemo.util.Constants;

/**
 * Data Transfer Object for transaction response containing complete transaction details.
 * 
 * This DTO maps the TRAN-RECORD structure from CVTRA05Y copybook to REST API responses,
 * providing comprehensive transaction information including merchant details and 
 * processing timestamps. Supports the CT00 transaction list functionality and 
 * transaction detail views in the React frontend.
 * 
 * Key Features:
 * - Complete mapping of COBOL TRAN-RECORD fields to Java properties
 * - BigDecimal precision for financial amounts matching COBOL S9(09)V99
 * - ISO-8601 timestamp formatting for REST API compatibility
 * - Card number masking for PCI DSS compliance
 * - Jakarta Bean Validation for field length constraints
 * - Jackson JSON serialization with custom property names
 * - Lombok-generated getters, setters, equals, hashCode, and toString
 * 
 * Field Mappings from CVTRA05Y copybook:
 * - TRAN-ID (PIC X(16)) → transactionId
 * - TRAN-TYPE-CD (PIC X(02)) → typeCode  
 * - TRAN-CAT-CD (PIC 9(04)) → categoryCode
 * - TRAN-SOURCE (PIC X(10)) → source
 * - TRAN-DESC (PIC X(100)) → description
 * - TRAN-AMT (PIC S9(09)V99) → amount
 * - TRAN-MERCHANT-ID (PIC 9(09)) → merchantId
 * - TRAN-MERCHANT-NAME (PIC X(50)) → merchantName
 * - TRAN-MERCHANT-CITY (PIC X(50)) → merchantCity
 * - TRAN-MERCHANT-ZIP (PIC X(10)) → merchantZip
 * - TRAN-CARD-NUM (PIC X(16)) → cardNumber (masked)
 * - TRAN-ORIG-TS (PIC X(26)) → origTimestamp
 * - TRAN-PROC-TS (PIC X(26)) → procTimestamp
 * 
 * Usage in REST Controllers:
 * - Transaction list endpoints (GET /api/transactions)
 * - Transaction detail endpoints (GET /api/transactions/{id})
 * - Transaction search results (POST /api/transactions/search)
 * 
 * Security Considerations:
 * - Card numbers are automatically masked showing only last 4 digits
 * - Sensitive merchant information is included for authorized views only
 * - Timestamps preserve audit trail information for compliance
 * 
 * Performance Considerations:
 * - Lightweight DTO suitable for list responses and caching
 * - BigDecimal arithmetic preserves financial precision
 * - LocalDateTime provides efficient temporal operations
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since CardDemo v1.0
 */
@Data
public class TransactionResponse {

    /**
     * Transaction ID - unique identifier for the transaction.
     * Maps to TRAN-ID field (PIC X(16)) from CVTRA05Y copybook.
     * Used for transaction lookup and correlation across system components.
     */
    @JsonProperty("transactionId")
    @Size(max = Constants.TRANSACTION_ID_LENGTH, message = "Transaction ID cannot exceed {max} characters")
    private String transactionId;

    /**
     * Transaction type code indicating the category of transaction.
     * Maps to TRAN-TYPE-CD field (PIC X(02)) from CVTRA05Y copybook.
     * Values include: 'DB' (Debit), 'CR' (Credit), 'FE' (Fee), etc.
     */
    @JsonProperty("typeCode")
    @Size(max = Constants.TYPE_CODE_LENGTH, message = "Type code cannot exceed {max} characters")
    private String typeCode;

    /**
     * Transaction category code providing additional classification.
     * Maps to TRAN-CAT-CD field (PIC 9(04)) from CVTRA05Y copybook.
     * Numeric codes categorizing transaction types for reporting and analysis.
     */
    @JsonProperty("categoryCode")
    @Size(max = Constants.CATEGORY_CODE_LENGTH, message = "Category code cannot exceed {max} characters")
    private String categoryCode;

    /**
     * Transaction source indicating origin of the transaction.
     * Maps to TRAN-SOURCE field (PIC X(10)) from CVTRA05Y copybook.
     * Values include: 'ONLINE', 'ATM', 'POS', 'PHONE', 'BRANCH', etc.
     */
    @JsonProperty("source")
    @Size(max = Constants.SOURCE_LENGTH, message = "Source cannot exceed {max} characters")
    private String source;

    /**
     * Transaction description providing human-readable transaction details.
     * Maps to TRAN-DESC field (PIC X(100)) from CVTRA05Y copybook.
     * Contains merchant name, location, or transaction type description.
     */
    @JsonProperty("description")
    @Size(max = Constants.DESCRIPTION_LENGTH, message = "Description cannot exceed {max} characters")
    private String description;

    /**
     * Transaction amount with exact decimal precision.
     * Maps to TRAN-AMT field (PIC S9(09)V99) from CVTRA05Y copybook.
     * Uses BigDecimal to preserve exact monetary precision and prevent rounding errors.
     * Scale is set to 2 decimal places matching COBOL V99 specification.
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Merchant identifier for transaction tracking and reconciliation.
     * Maps to TRAN-MERCHANT-ID field (PIC 9(09)) from CVTRA05Y copybook.
     * Numeric identifier linking transactions to merchant records.
     */
    @JsonProperty("merchantId")
    @Size(max = 9, message = "Merchant ID cannot exceed 9 characters")
    private String merchantId;

    /**
     * Merchant name for display and reporting purposes.
     * Maps to TRAN-MERCHANT-NAME field (PIC X(50)) from CVTRA05Y copybook.
     * Business name where the transaction occurred.
     */
    @JsonProperty("merchantName")
    @Size(max = Constants.MERCHANT_NAME_LENGTH, message = "Merchant name cannot exceed {max} characters")  
    private String merchantName;

    /**
     * Merchant city location for geographic reporting.
     * Maps to TRAN-MERCHANT-CITY field (PIC X(50)) from CVTRA05Y copybook.
     * City where the merchant business is located.
     */
    @JsonProperty("merchantCity")
    @Size(max = 50, message = "Merchant city cannot exceed 50 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code for location verification and fraud detection.
     * Maps to TRAN-MERCHANT-ZIP field (PIC X(10)) from CVTRA05Y copybook.
     * Postal code of merchant location.
     */
    @JsonProperty("merchantZip")
    @Size(max = 10, message = "Merchant ZIP cannot exceed 10 characters")
    private String merchantZip;

    /**
     * Masked card number showing only last 4 digits for security.
     * Maps to TRAN-CARD-NUM field (PIC X(16)) from CVTRA05Y copybook.
     * Automatically masked using FormatUtil.maskCardNumber() for PCI DSS compliance.
     * Displays as "****-****-****-1234" format.
     */
    @JsonProperty("cardNumber")
    @Size(max = Constants.CARD_NUMBER_LENGTH, message = "Card number cannot exceed {max} characters")
    private String cardNumber;

    /**
     * Original transaction timestamp when transaction was initiated.
     * Maps to TRAN-ORIG-TS field (PIC X(26)) from CVTRA05Y copybook.
     * Formatted as ISO-8601 for REST API compatibility and timezone handling.
     */
    @JsonProperty("origTimestamp")
    private LocalDateTime origTimestamp;

    /**
     * Processing timestamp when transaction was completed by the system.
     * Maps to TRAN-PROC-TS field (PIC X(26)) from CVTRA05Y copybook.
     * Formatted as ISO-8601 for REST API compatibility and audit trails.
     */
    @JsonProperty("procTimestamp")
    private LocalDateTime procTimestamp;

    /**
     * Sets the card number with automatic masking for security compliance.
     * 
     * This method automatically applies card number masking using FormatUtil.maskCardNumber()
     * to ensure PCI DSS compliance by showing only the last 4 digits of the card number.
     * The masked format displays as "****-****-****-1234".
     * 
     * @param cardNumber the full card number to be masked and stored
     * @throws IllegalArgumentException if card number is invalid for masking
     */
    public void setCardNumber(String cardNumber) {
        if (cardNumber != null && !cardNumber.trim().isEmpty()) {
            this.cardNumber = FormatUtil.maskCardNumber(cardNumber);
        } else {
            this.cardNumber = "";
        }
    }

    /**
     * Sets the transaction amount with proper scale for financial precision.
     * 
     * Ensures the amount is set with exactly 2 decimal places matching COBOL
     * S9(09)V99 specification. Uses HALF_UP rounding mode to match COBOL
     * ROUNDED clause behavior for consistent financial calculations.
     * 
     * @param amount the transaction amount to set with proper scale
     */
    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.amount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Returns the transaction amount, ensuring proper scale is maintained.
     * 
     * Guarantees that returned amount always has exactly 2 decimal places
     * for consistent financial precision across all system components.
     * 
     * @return transaction amount with 2 decimal places, never null
     */
    public BigDecimal getAmount() {
        if (this.amount == null) {
            return BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return this.amount.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Returns a comparison result indicating if this amount is greater than the specified amount.
     * 
     * Provides convenient amount comparison functionality for transaction filtering
     * and sorting operations. Uses BigDecimal.compareTo() for exact decimal comparison.
     * 
     * @param compareAmount the amount to compare against
     * @return positive int if this amount is greater, negative if less, zero if equal
     */
    public int compareAmountTo(BigDecimal compareAmount) {
        if (compareAmount == null) {
            compareAmount = BigDecimal.ZERO;
        }
        return this.getAmount().compareTo(compareAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    /**
     * Validates that all timestamp fields have values for complete audit trail.
     * 
     * Ensures both original and processing timestamps are present for proper
     * transaction tracking and compliance with audit requirements.
     * 
     * @return true if both timestamps are present, false otherwise
     */
    public boolean hasCompleteTimestamps() {
        return this.origTimestamp != null && this.procTimestamp != null;
    }

    /**
     * Determines if this transaction occurred before the specified timestamp.
     * 
     * Provides convenient temporal comparison using the original timestamp
     * for transaction filtering and date range operations.
     * 
     * @param timestamp the timestamp to compare against
     * @return true if this transaction occurred before the specified time
     */
    public boolean occurredBefore(LocalDateTime timestamp) {
        if (this.origTimestamp == null || timestamp == null) {
            return false;
        }
        return this.origTimestamp.isBefore(timestamp);
    }

    /**
     * Determines if this transaction occurred after the specified timestamp.
     * 
     * Provides convenient temporal comparison using the original timestamp
     * for transaction filtering and date range operations.
     * 
     * @param timestamp the timestamp to compare against  
     * @return true if this transaction occurred after the specified time
     */
    public boolean occurredAfter(LocalDateTime timestamp) {
        if (this.origTimestamp == null || timestamp == null) {
            return false;
        }
        return this.origTimestamp.isAfter(timestamp);
    }
}