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

import com.carddemo.util.Constants;

/**
 * Data Transfer Object for transaction detail response providing comprehensive individual transaction information.
 * 
 * This DTO specifically supports the CT01 transaction detail functionality, translating from the
 * COTRN01C.cbl COBOL program to provide detailed transaction information for individual transaction
 * examination. Maps the TRAN-RECORD structure from CVTRA05Y copybook to REST API responses for
 * detailed transaction view screens.
 * 
 * Key Features:
 * - Complete mapping of essential COBOL TRAN-RECORD fields for transaction detail display
 * - BigDecimal precision for financial amounts matching COBOL S9(09)V99 COMP-3 specification
 * - ISO-8601 timestamp formatting for REST API compatibility and audit trails
 * - Field validation matching COBOL field length constraints
 * - Jackson JSON serialization with custom property names for frontend integration
 * - Lombok-generated getters, setters, equals, hashCode, and toString for efficiency
 * 
 * Field Mappings from CVTRA05Y copybook (COTRN01C.cbl translation):
 * - TRAN-ID (PIC X(16)) → transactionId (TRNIDI field mapping)
 * - TRAN-AMT (PIC S9(09)V99 COMP-3) → amount (TRNAMTI field mapping)
 * - TRAN-DESC (PIC X(100)) → description (TDESCI field mapping)
 * - TRAN-MERCHANT-NAME (PIC X(50)) → merchantName (MNAMEI field mapping)
 * - TRAN-MERCHANT-CITY (PIC X(50)) → merchantCity (MCITYI field mapping)
 * - TRAN-MERCHANT-ZIP (PIC X(10)) → merchantZip (MZIPI field mapping)
 * - TRAN-ORIG-TS (PIC X(26)) → origTimestamp (TORIGDTI field mapping)
 * - TRAN-PROC-TS (PIC X(26)) → procTimestamp (TPROCDTI field mapping)
 * 
 * Usage in REST Controllers:
 * - Transaction detail endpoint (GET /api/transactions/{id})
 * - Individual transaction lookup results
 * - Transaction examination by customer service
 * 
 * COBOL Translation Context:
 * - Replaces BMS map COTRN01 screen data structure
 * - Maintains field formatting and display logic from PROCESS-ENTER-KEY paragraph
 * - Preserves data validation rules from original COBOL implementation
 * - Ensures identical business behavior for transaction detail display
 * 
 * Security Considerations:
 * - Contains detailed merchant information for authorized transaction examination
 * - Timestamps preserve complete audit trail information for compliance
 * - Field-level access control applied at service layer before DTO population
 * 
 * Performance Considerations:
 * - Lightweight DTO optimized for single transaction detail responses
 * - BigDecimal arithmetic preserves exact financial precision without rounding
 * - LocalDateTime provides efficient temporal operations and timezone handling
 * 
 * @author CardDemo Migration Team  
 * @version 1.0
 * @since CardDemo v1.0
 * @see COTRN01C.cbl - Original COBOL program for transaction detail display
 * @see TransactionDetailService - Service class using this DTO for transaction detail operations
 */
@Data
public class TransactionDetailDto {

    /**
     * Transaction ID - unique identifier for the individual transaction being examined.
     * Maps to TRAN-ID field (PIC X(16)) from CVTRA05Y copybook and TRNIDI in COTRN01 BMS map.
     * Used for transaction lookup, correlation, and display in transaction detail screen.
     */
    @JsonProperty("transactionId")
    @Size(max = Constants.TRANSACTION_ID_LENGTH, message = "Transaction ID cannot exceed {max} characters")
    private String transactionId;

    /**
     * Transaction amount with exact decimal precision for financial accuracy.
     * Maps to TRAN-AMT field (PIC S9(09)V99 COMP-3) from CVTRA05Y copybook and TRNAMTI in BMS map.
     * Uses BigDecimal to preserve exact monetary precision matching COBOL COMP-3 packed decimal behavior.
     * Scale is set to 2 decimal places with HALF_UP rounding matching COBOL ROUNDED clause.
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Transaction description providing detailed transaction information for user examination.
     * Maps to TRAN-DESC field (PIC X(100)) from CVTRA05Y copybook and TDESCI in BMS map.
     * Contains comprehensive transaction type description, merchant details, or transaction classification.
     */
    @JsonProperty("description")
    @Size(max = Constants.DESCRIPTION_LENGTH, message = "Description cannot exceed {max} characters")
    private String description;

    /**
     * Merchant name for detailed transaction examination and customer service review.
     * Maps to TRAN-MERCHANT-NAME field (PIC X(50)) from CVTRA05Y copybook and MNAMEI in BMS map.
     * Business name where the transaction occurred, essential for transaction verification.
     */
    @JsonProperty("merchantName")
    @Size(max = Constants.MERCHANT_NAME_LENGTH, message = "Merchant name cannot exceed {max} characters")  
    private String merchantName;

    /**
     * Merchant city location for geographic transaction verification and fraud analysis.
     * Maps to TRAN-MERCHANT-CITY field (PIC X(50)) from CVTRA05Y copybook and MCITYI in BMS map.
     * City where the merchant business is located, used for location-based transaction validation.
     */
    @JsonProperty("merchantCity")
    @Size(max = 50, message = "Merchant city cannot exceed 50 characters")
    private String merchantCity;

    /**
     * Merchant ZIP code for precise location verification and compliance reporting.
     * Maps to TRAN-MERCHANT-ZIP field (PIC X(10)) from CVTRA05Y copybook and MZIPI in BMS map.
     * Postal code of merchant location, essential for geographic fraud detection.
     */
    @JsonProperty("merchantZip")
    @Size(max = 10, message = "Merchant ZIP cannot exceed 10 characters")
    private String merchantZip;

    /**
     * Original transaction timestamp when transaction was first initiated by the customer.
     * Maps to TRAN-ORIG-TS field (PIC X(26)) from CVTRA05Y copybook and TORIGDTI in BMS map.
     * Formatted as ISO-8601 for REST API compatibility and provides customer-facing transaction time.
     */
    @JsonProperty("origTimestamp")
    private LocalDateTime origTimestamp;

    /**
     * Processing timestamp when transaction was completed and processed by the system.
     * Maps to TRAN-PROC-TS field (PIC X(26)) from CVTRA05Y copybook and TPROCDTI in BMS map.
     * Formatted as ISO-8601 for REST API compatibility and provides audit trail for processing time.
     */
    @JsonProperty("procTimestamp")
    private LocalDateTime procTimestamp;

    /**
     * Card number associated with the transaction for identification and verification.
     * Maps to TRAN-CARD-NUM field (PIC X(16)) from CVTRA05Y copybook.
     * Used for transaction correlation and customer service reference.
     */
    @JsonProperty("cardNumber")
    @Size(max = 16, message = "Card number cannot exceed 16 characters")
    private String cardNumber;

    /**
     * Alias method for getting original timestamp to support legacy test compatibility.
     * 
     * @return original transaction timestamp
     */
    public LocalDateTime getOriginalTimestamp() {
        return this.origTimestamp;
    }

    /**
     * Alias method for setting original timestamp to support legacy test compatibility.
     * 
     * @param originalTimestamp the original timestamp to set
     */
    public void setOriginalTimestamp(LocalDateTime originalTimestamp) {
        this.origTimestamp = originalTimestamp;
    }

    /**
     * Alias method for getting processed timestamp to support legacy test compatibility.
     * 
     * @return processed transaction timestamp
     */
    public LocalDateTime getProcessedTimestamp() {
        return this.procTimestamp;
    }

    /**
     * Alias method for setting processed timestamp to support legacy test compatibility.
     * 
     * @param processedTimestamp the processed timestamp to set
     */
    public void setProcessedTimestamp(LocalDateTime processedTimestamp) {
        this.procTimestamp = processedTimestamp;
    }

    /**
     * Sets the transaction amount with proper scale for exact financial precision.
     * 
     * Ensures the amount is set with exactly 2 decimal places matching COBOL
     * S9(09)V99 COMP-3 specification. Uses HALF_UP rounding mode to match COBOL
     * ROUNDED clause behavior for consistent financial calculations across the system.
     * 
     * @param amount the transaction amount to set with proper decimal scale
     */
    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.amount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Returns the transaction amount, ensuring proper scale is always maintained.
     * 
     * Guarantees that returned amount always has exactly 2 decimal places
     * for consistent financial precision across all system components and UI display.
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
     * Validates that all essential transaction detail fields are populated for complete display.
     * 
     * Ensures that the core transaction information required for detail view is present,
     * matching the validation logic from original COBOL PROCESS-ENTER-KEY paragraph.
     * 
     * @return true if transaction ID, amount, and timestamps are all present, false otherwise
     */
    public boolean hasCompleteTransactionDetails() {
        return this.transactionId != null && !this.transactionId.trim().isEmpty() &&
               this.amount != null &&
               this.origTimestamp != null && 
               this.procTimestamp != null;
    }

    /**
     * Validates that merchant information is complete for proper transaction verification.
     * 
     * Ensures that essential merchant details are present for customer service
     * transaction examination and fraud prevention review processes.
     * 
     * @return true if merchant name is present, false otherwise
     */
    public boolean hasCompleteMerchantInfo() {
        return this.merchantName != null && !this.merchantName.trim().isEmpty();
    }
}