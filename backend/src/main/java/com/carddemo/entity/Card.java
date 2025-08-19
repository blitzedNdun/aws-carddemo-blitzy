/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.util.Constants;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.FormatUtil;
import com.carddemo.util.ValidationUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity representing credit card data, mapped to card_data PostgreSQL table.
 * 
 * This entity class represents the CARD-RECORD structure from CVACT02Y.cpy copybook,
 * providing complete credit card management functionality within the CardDemo system.
 * The entity maps directly to the card_data table and maintains relationships with
 * Account and Customer entities for comprehensive card-account-customer associations.
 * 
 * Key Features:
 * - PCI DSS compliant card data storage with field masking capabilities
 * - Card number validation and type determination
 * - Expiration date validation and business logic
 * - Account and customer relationship management
 * - Active status validation and management
 * - Comprehensive field validation matching COBOL edit routines
 * 
 * Database Mapping:
 * - Table: card_data
 * - Primary Key: card_number (VARCHAR(16))
 * - Foreign Keys: account_id, customer_id
 * - Indexes: card_account_idx (account_id)
 * 
 * Security Compliance:
 * - CVV code excluded from JSON serialization
 * - Card number masking for display purposes
 * - Validation for all sensitive fields
 * 
 * This implementation preserves all business logic from the original COBOL programs
 * (COCRDLIC.cbl, COCRDSLC.cbl, COCRDUPC.cbl) while leveraging modern JPA features
 * for enhanced data integrity and relationship management.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Entity
@Table(name = "card_data", indexes = {
    @Index(name = "card_account_idx", columnList = "account_id"),
    @Index(name = "card_customer_idx", columnList = "customer_id")
})
public class Card {

    /**
     * Credit card number - Primary key field.
     * Maps to CARD-NUM field from CVACT02Y.cpy (PIC X(16)).
     * 
     * Validation:
     * - Must be exactly 16 digits
     * - Numeric characters only
     * - Industry standard format validation
     */
    @Id
    @Column(name = "card_number", length = Constants.CARD_NUMBER_LENGTH, nullable = false)
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Account ID foreign key linking card to account.
     * Maps to CARD-ACCT-ID field from CVACT02Y.cpy (PIC 9(11)).
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /**
     * Customer ID foreign key linking card to customer.
     * Required for card-customer relationship management.
     */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /**
     * Card verification value (CVV) code.
     * Maps to CARD-CVV-CD field from CVACT02Y.cpy (PIC 9(03)).
     * 
     * Security:
     * - Excluded from JSON serialization for PCI DSS compliance
     * - Must be exactly 3 digits
     */
    @JsonIgnore
    @Column(name = "cvv_code", length = 3, nullable = false)
    @Pattern(regexp = "^\\d{3}$", message = "CVV code must be exactly 3 digits")
    private String cvvCode;

    /**
     * Embossed name on the card.
     * Maps to CARD-EMBOSSED-NAME field from CVACT02Y.cpy (PIC X(50)).
     */
    @Column(name = "embossed_name", length = 50, nullable = false)
    private String embossedName;

    /**
     * Card expiration date.
     * Maps to CARD-EXPIRAION-DATE field from CVACT02Y.cpy (PIC X(10)).
     */
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    /**
     * Card active status indicator.
     * Maps to CARD-ACTIVE-STATUS field from CVACT02Y.cpy (PIC X(01)).
     * Values: 'Y' = Active, 'N' = Inactive
     */
    @Column(name = "active_status", length = 1, nullable = false)
    @Pattern(regexp = "^[YN]$", message = "Active status must be 'Y' or 'N'")
    private String activeStatus;

    /**
     * Account entity relationship.
     * Many cards can belong to one account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Customer entity relationship.
     * Many cards can belong to one customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    /**
     * Default constructor for JPA.
     */
    public Card() {
    }

    /**
     * Constructor with required fields.
     * 
     * @param cardNumber the card number (16 digits)
     * @param accountId the account ID foreign key
     * @param customerId the customer ID foreign key
     * @param cvvCode the CVV code (3 digits)
     * @param embossedName the embossed name on card
     * @param expirationDate the card expiration date
     * @param activeStatus the active status ('Y' or 'N')
     */
    public Card(String cardNumber, Long accountId, Long customerId, String cvvCode, 
                String embossedName, LocalDate expirationDate, String activeStatus) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.customerId = customerId;
        this.cvvCode = cvvCode;
        this.embossedName = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus = activeStatus;
    }

    // Getters and Setters

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            this.customerId = customer.getCustomerId();
        }
    }

    // Business Methods

    /**
     * Returns masked card number showing only last 4 digits for PCI DSS compliance.
     * Uses FormatUtil.maskCardNumber() for consistent masking throughout the application.
     * 
     * @return masked card number (e.g., "****-****-****-1234")
     */
    public String getMaskedCardNumber() {
        return FormatUtil.maskCardNumber(this.cardNumber);
    }

    /**
     * Checks if the card is expired based on current date.
     * A card is considered expired if the expiration date is before the current date.
     * 
     * @return true if card is expired, false otherwise
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return true;
        }
        return expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if the card is active based on the active status flag.
     * 
     * @return true if active status is 'Y', false otherwise
     */
    public boolean isActive() {
        return "Y".equals(activeStatus);
    }

    /**
     * Validates the card using comprehensive validation rules.
     * Implements validation logic equivalent to COBOL edit routines.
     * 
     * @throws RuntimeException if validation fails with specific error messages
     */
    public void validateCard() {
        // Validate card number
        ValidationUtil.validateRequiredField("cardNumber", cardNumber);
        if (cardNumber != null) {
            ValidationUtil.FieldValidator validator = new ValidationUtil.FieldValidator();
            validator.validateCardNumber(cardNumber);
        }
        
        // Validate account ID
        ValidationUtil.validateRequiredField("accountId", accountId != null ? accountId.toString() : null);
        
        // Validate customer ID
        ValidationUtil.validateRequiredField("customerId", customerId != null ? customerId.toString() : null);
        
        // Validate CVV code
        ValidationUtil.validateRequiredField("cvvCode", cvvCode);
        ValidationUtil.validateFieldLength("cvvCode", cvvCode, 3);
        
        // Validate embossed name
        ValidationUtil.validateRequiredField("embossedName", embossedName);
        ValidationUtil.validateFieldLength("embossedName", embossedName, 50);
        
        // Validate expiration date
        if (expirationDate == null) {
            throw new RuntimeException("Expiration date must be supplied.");
        }
        
        // Validate expiration date is not in the past
        if (isExpired()) {
            throw new RuntimeException("Card expiration date cannot be in the past.");
        }
        
        // Validate active status
        ValidationUtil.validateRequiredField("activeStatus", activeStatus);
        if (activeStatus != null && !activeStatus.matches("^[YN]$")) {
            throw new RuntimeException("Active status must be 'Y' or 'N'.");
        }
    }

    /**
     * Determines the card type based on the card number prefix.
     * Uses ValidationUtil.determineCardType() for industry standard type detection.
     * 
     * @return card type ("VISA", "MASTERCARD", "AMEX", "DISCOVER", "UNKNOWN")
     */
    public String getCardType() {
        return ValidationUtil.determineCardType(this.cardNumber);
    }

    // Object Methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return Objects.equals(cardNumber, card.cardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber);
    }

    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId=" + accountId +
                ", customerId=" + customerId +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + expirationDate +
                ", activeStatus='" + activeStatus + '\'' +
                ", cardType='" + getCardType() + '\'' +
                ", expired=" + isExpired() +
                ", active=" + isActive() +
                '}';
    }
}