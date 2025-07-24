package com.carddemo.batch;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object representing a card record from the ASCII file.
 * 
 * Maps directly to the COBOL CVACT02Y copybook structure with JSR-303
 * validation annotations for Spring Batch field-level validation.
 */
public class CardRecord {
    
    @NotNull(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private String cardNumber;
    
    @NotNull(message = "Account ID is required")
    @Pattern(regexp = "\\d{11}", message = "Account ID must be exactly 11 digits")
    private String accountId;
    
    @NotNull(message = "CVV code is required")
    @Pattern(regexp = "\\d{3}", message = "CVV code must be exactly 3 digits")
    private String cvvCode;
    
    @NotNull(message = "Embossed name is required")
    @Size(min = 1, max = 50, message = "Embossed name must be 1-50 characters")
    private String embossedName;
    
    @NotNull(message = "Expiration date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Expiration date must be YYYY-MM-DD format")
    private String expirationDate;
    
    @NotNull(message = "Active status is required")
    @Pattern(regexp = "[YNyn]", message = "Active status must be Y or N")
    private String activeStatus;
    
    // Default constructor for Spring Batch bean mapping
    public CardRecord() {}
    
    // Getters and setters for all fields
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Derives customer ID from account ID using the composite foreign key relationship.
     * 
     * Based on the database schema, customer_id is derived from the first 9 digits
     * of the account_id to maintain referential integrity with the accounts table.
     * 
     * @return the customer ID derived from account ID
     */
    public String getCustomerId() {
        if (accountId == null || accountId.length() < 9) {
            return null;
        }
        // Customer ID is first 9 digits of account ID per database schema
        return accountId.substring(0, 9);
    }
    
    public String getCvvCode() {
        return cvvCode;
    }
    
    public void setCvvCode(String cvvCode) {
        this.cvvCode = cvvCode;
    }
    
    public String getEmbossedName() {
        return embossedName;
    }
    
    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }
    
    public String getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public String getActiveStatus() {
        return activeStatus;
    }
    
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
    
    @Override
    public String toString() {
        return "CardRecord{" +
               "cardNumber='" + (cardNumber != null ? "****" + cardNumber.substring(Math.max(0, cardNumber.length() - 4)) : "null") + "'" +
               ", accountId='" + accountId + "'" +
               ", customerId='" + getCustomerId() + "'" +
               ", cvvCode='***'" +
               ", embossedName='" + embossedName + "'" +
               ", expirationDate='" + expirationDate + "'" +
               ", activeStatus='" + activeStatus + "'" +
               '}';
    }
}