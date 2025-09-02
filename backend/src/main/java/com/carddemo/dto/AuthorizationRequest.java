package com.carddemo.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for card authorization requests from payment processors and merchants.
 * 
 * This DTO contains transaction details including card number, amount, merchant information,
 * and risk indicators required for real-time authorization processing. Designed to maintain
 * compatibility with existing payment network interfaces while supporting modern REST API
 * integration patterns.
 * 
 * Field precision and validation rules preserve compatibility with original COBOL COMP-3
 * decimal handling and mainframe data validation patterns during the migration from
 * CICS transaction processing to Spring Boot REST architecture.
 */
public class AuthorizationRequest {

    /**
     * Card number for the authorization request.
     * Must be a valid 16-digit payment card number.
     */
    @NotNull(message = "Card number is required")
    @NotBlank(message = "Card number cannot be blank")
    @Size(min = 13, max = 19, message = "Card number must be between 13 and 19 digits")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must contain only digits")
    @JsonProperty("card_number")
    private String cardNumber;

    /**
     * Card expiration date in MMYY format.
     * Used for card validity verification during authorization.
     */
    @NotNull(message = "Expiration date is required")
    @NotBlank(message = "Expiration date cannot be blank")
    @Pattern(regexp = "^(0[1-9]|1[0-2])([0-9]{2})$", message = "Expiration date must be in MMYY format")
    @JsonProperty("expiration_date")
    private String expirationDate;

    /**
     * Card Verification Value (CVV) for enhanced security.
     * Typically a 3 or 4 digit security code.
     */
    @NotNull(message = "CVV is required")
    @NotBlank(message = "CVV cannot be blank")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    @JsonProperty("cvv")
    private String cvv;

    /**
     * Transaction amount using BigDecimal for precise financial calculations.
     * Maintains exact monetary precision matching COBOL COMP-3 packed decimal behavior
     * with proper scale and rounding for financial accuracy.
     */
    @NotNull(message = "Transaction amount is required")
    @JsonProperty("transaction_amount")
    private BigDecimal transactionAmount;

    /**
     * Merchant identifier for transaction processing.
     * Links transaction to specific merchant account for authorization and settlement.
     */
    @NotNull(message = "Merchant ID is required")
    @NotBlank(message = "Merchant ID cannot be blank")
    @JsonProperty("merchant_id")
    private String merchantId;

    /**
     * Merchant name for transaction display and reporting.
     * Human-readable merchant identification for transaction records.
     */
    @JsonProperty("merchant_name")
    private String merchantName;

    /**
     * Type of transaction being authorized.
     * Examples: PURCHASE, REFUND, CASH_ADVANCE, BALANCE_INQUIRY
     */
    @NotNull(message = "Transaction type is required")
    @NotBlank(message = "Transaction type cannot be blank")
    @JsonProperty("transaction_type")
    private String transactionType;

    /**
     * Terminal identifier where the transaction originated.
     * Used for risk assessment and transaction tracking.
     */
    @NotNull(message = "Terminal ID is required")
    @NotBlank(message = "Terminal ID cannot be blank")
    @JsonProperty("terminal_id")
    private String terminalId;

    /**
     * Acquirer reference number for transaction correlation.
     * Unique identifier provided by the acquiring financial institution.
     */
    @JsonProperty("acquirer_reference_number")
    private String acquirerReferenceNumber;

    /**
     * Point of Service entry mode indicating how card data was captured.
     * Examples: MANUAL, SWIPE, CHIP, CONTACTLESS, ONLINE
     */
    @JsonProperty("point_of_service_entry_mode")
    private String pointOfServiceEntryMode;

    /**
     * Default constructor for JSON deserialization and framework instantiation.
     */
    public AuthorizationRequest() {
    }

    /**
     * Constructor with all required fields for authorization processing.
     * 
     * @param cardNumber The payment card number
     * @param expirationDate Card expiration date in MMYY format
     * @param cvv Card verification value
     * @param transactionAmount Transaction amount with precise decimal handling
     * @param merchantId Merchant identifier
     * @param transactionType Type of transaction
     * @param terminalId Terminal identifier
     */
    public AuthorizationRequest(String cardNumber, String expirationDate, String cvv, 
                              BigDecimal transactionAmount, String merchantId, 
                              String transactionType, String terminalId) {
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.cvv = cvv;
        this.transactionAmount = transactionAmount;
        this.merchantId = merchantId;
        this.transactionType = transactionType;
        this.terminalId = terminalId;
    }

    /**
     * Gets the card number for authorization processing.
     * 
     * @return The payment card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number for authorization processing.
     * 
     * @param cardNumber The payment card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the card expiration date.
     * 
     * @return Card expiration date in MMYY format
     */
    public String getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the card expiration date.
     * 
     * @param expirationDate Card expiration date in MMYY format
     */
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Gets the card verification value.
     * 
     * @return CVV security code
     */
    public String getCvv() {
        return cvv;
    }

    /**
     * Sets the card verification value.
     * 
     * @param cvv CVV security code
     */
    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    /**
     * Gets the transaction amount with precise decimal handling.
     * Maintains COBOL COMP-3 precision compatibility for financial calculations.
     * 
     * @return Transaction amount as BigDecimal
     */
    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    /**
     * Sets the transaction amount with precise decimal handling.
     * 
     * @param transactionAmount Transaction amount as BigDecimal
     */
    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    /**
     * Gets the merchant identifier for transaction processing.
     * 
     * @return Merchant ID
     */
    public String getMerchantId() {
        return merchantId;
    }

    /**
     * Sets the merchant identifier for transaction processing.
     * 
     * @param merchantId Merchant ID
     */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /**
     * Gets the merchant name for display purposes.
     * 
     * @return Merchant name
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Sets the merchant name for display purposes.
     * 
     * @param merchantName Merchant name
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * Gets the type of transaction being authorized.
     * 
     * @return Transaction type
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the type of transaction being authorized.
     * 
     * @param transactionType Transaction type
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the terminal identifier where transaction originated.
     * 
     * @return Terminal ID
     */
    public String getTerminalId() {
        return terminalId;
    }

    /**
     * Sets the terminal identifier where transaction originated.
     * 
     * @param terminalId Terminal ID
     */
    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    /**
     * Gets the acquirer reference number for transaction correlation.
     * 
     * @return Acquirer reference number
     */
    public String getAcquirerReferenceNumber() {
        return acquirerReferenceNumber;
    }

    /**
     * Sets the acquirer reference number for transaction correlation.
     * 
     * @param acquirerReferenceNumber Acquirer reference number
     */
    public void setAcquirerReferenceNumber(String acquirerReferenceNumber) {
        this.acquirerReferenceNumber = acquirerReferenceNumber;
    }

    /**
     * Gets the point of service entry mode.
     * 
     * @return Point of service entry mode
     */
    public String getPointOfServiceEntryMode() {
        return pointOfServiceEntryMode;
    }

    /**
     * Sets the point of service entry mode.
     * 
     * @param pointOfServiceEntryMode Point of service entry mode
     */
    public void setPointOfServiceEntryMode(String pointOfServiceEntryMode) {
        this.pointOfServiceEntryMode = pointOfServiceEntryMode;
    }

    /**
     * Provides string representation of authorization request for logging and debugging.
     * Masks sensitive card information for security compliance.
     * 
     * @return String representation with masked sensitive data
     */
    @Override
    public String toString() {
        return "AuthorizationRequest{" +
                "cardNumber='" + (cardNumber != null ? maskCardNumber(cardNumber) : null) + '\'' +
                ", expirationDate='" + expirationDate + '\'' +
                ", cvv='***'" +
                ", transactionAmount=" + transactionAmount +
                ", merchantId='" + merchantId + '\'' +
                ", merchantName='" + merchantName + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", terminalId='" + terminalId + '\'' +
                ", acquirerReferenceNumber='" + acquirerReferenceNumber + '\'' +
                ", pointOfServiceEntryMode='" + pointOfServiceEntryMode + '\'' +
                '}';
    }

    /**
     * Masks card number for secure logging, showing only first 4 and last 4 digits.
     * 
     * @param cardNumber The card number to mask
     * @return Masked card number for safe logging
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "********" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Validates that all required fields for authorization are present.
     * This method can be used for additional business validation beyond annotation-based validation.
     * 
     * @return true if all required fields are present and valid
     */
    public boolean isValidForAuthorization() {
        return cardNumber != null && !cardNumber.trim().isEmpty() &&
               expirationDate != null && !expirationDate.trim().isEmpty() &&
               cvv != null && !cvv.trim().isEmpty() &&
               transactionAmount != null && transactionAmount.compareTo(BigDecimal.ZERO) > 0 &&
               merchantId != null && !merchantId.trim().isEmpty() &&
               transactionType != null && !transactionType.trim().isEmpty() &&
               terminalId != null && !terminalId.trim().isEmpty();
    }
}