package com.carddemo.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.io.Serializable;

/**
 * JPA Card entity mapping COBOL card record to PostgreSQL cards table
 * with card number validation, security fields, expiration tracking,
 * and composite foreign key relationships to accounts and customers
 */
@Entity
@Table(name = "cards")
public class Card implements Serializable {

    @Id
    @Column(name = "card_number", length = 16)
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 characters")
    private String cardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "cvv_code", length = 3)
    @Size(min = 3, max = 3, message = "CVV code must be exactly 3 characters")
    private String cvvCode;

    @Column(name = "embossed_name", length = 50)
    @Size(max = 50, message = "Embossed name must not exceed 50 characters")
    @NotBlank(message = "Embossed name is required")
    private String embossedName;

    @Column(name = "expiration_date")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1)
    @Size(min = 1, max = 1, message = "Active status must be exactly 1 character")
    @Pattern(regexp = "[YN]", message = "Active status must be Y or N")
    private String activeStatus;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    // Default constructor
    public Card() {
        this.activeStatus = "Y";
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
    }

    // Constructor with card number
    public Card(String cardNumber) {
        this();
        this.cardNumber = cardNumber;
    }

    // Getters and setters
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getAccountId() {
        return account != null ? account.getAccountId() : null;
    }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getCustomerId() {
        return customer != null ? customer.getCustomerId() : null;
    }

    // Additional setters for direct ID assignment (for backward compatibility)
    public void setAccountId(String accountId) {
        // Note: This method is for compatibility but doesn't actually set the account
        // The account should be set via setAccount() method
    }

    public void setCustomerId(String customerId) {
        // Note: This method is for compatibility but doesn't actually set the customer
        // The customer should be set via setCustomer() method
    }

    public String getCvvCode() { return cvvCode; }
    public void setCvvCode(String cvvCode) { this.cvvCode = cvvCode; }

    public String getEmbossedName() { return embossedName; }
    public void setEmbossedName(String embossedName) { this.embossedName = embossedName; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { 
        this.activeStatus = activeStatus; 
        this.modifiedDate = LocalDateTime.now();
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    // Utility method for masked card number
    public String getMaskedCardNumber() {
        if (cardNumber != null && cardNumber.length() >= 4) {
            return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        }
        return "****";
    }

    // Check if card is active
    public boolean isActive() {
        return "Y".equals(activeStatus);
    }

    // Check if card is expired
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
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
                ", embossedName='" + embossedName + '\'' +
                ", activeStatus='" + activeStatus + '\'' +
                ", expirationDate=" + expirationDate +
                '}';
    }
}