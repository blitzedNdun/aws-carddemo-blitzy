/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing financial transaction records, mapped to partitioned transactions PostgreSQL table.
 * 
 * This entity maps to the TRAN-RECORD structure from CVTRA05Y.cpy copybook, providing complete
 * transaction management functionality within the CardDemo system. The entity supports 
 * date-based table partitioning for efficient batch processing and maintains precise 
 * monetary calculations using BigDecimal.
 * 
 * Field mappings from COBOL copybook CVTRA05Y.cpy:
 * - TRAN-ID (PIC X(16)) → transactionId (VARCHAR(16))
 * - TRAN-TYPE-CD (PIC X(02)) → transactionTypeCode (VARCHAR(2))
 * - TRAN-CAT-CD (PIC 9(04)) → categoryCode (VARCHAR(4))
 * - TRAN-SOURCE (PIC X(10)) → source (VARCHAR(10))
 * - TRAN-DESC (PIC X(100)) → description (VARCHAR(100))
 * - TRAN-AMT (PIC S9(09)V99) → amount (NUMERIC(12,2))
 * - TRAN-MERCHANT-ID (PIC 9(09)) → merchantId (BIGINT)
 * - TRAN-MERCHANT-NAME (PIC X(50)) → merchantName (VARCHAR(50))
 * - TRAN-MERCHANT-CITY (PIC X(50)) → merchantCity (VARCHAR(50))
 * - TRAN-MERCHANT-ZIP (PIC X(10)) → merchantZip (VARCHAR(10))
 * - TRAN-CARD-NUM (PIC X(16)) → cardNumber (VARCHAR(16))
 * - TRAN-ORIG-TS (PIC X(26)) → originalTimestamp (TIMESTAMP)
 * - TRAN-PROC-TS (PIC X(26)) → processedTimestamp (TIMESTAMP)
 * 
 * Relationships:
 * - @ManyToOne with Account entity (account_id foreign key)
 * - @ManyToOne with Card entity (card_number foreign key)
 * - @ManyToOne with TransactionType entity (transaction_type_code foreign key)
 * - @ManyToOne with TransactionCategory entity (category_code foreign key)
 * 
 * Key Features:
 * - BigDecimal with scale=2 for transaction amounts to preserve COBOL COMP-3 precision
 * - Support for partitioned table structure through transaction_date field
 * - Foreign key relationships to Account, Card, TransactionType, and TransactionCategory
 * - Comprehensive validation annotations matching COBOL field definitions
 * - Support for both original and processed timestamps for audit trail
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"account", "card", "transactionType", "transactionCategory"})
public class Transaction {

    // Constants for field constraints (matching COBOL PIC clauses)
    private static final int TRANSACTION_ID_LENGTH = 16;
    private static final int TYPE_CODE_LENGTH = 2;
    private static final int CATEGORY_CODE_LENGTH = 4;
    private static final int SUBCATEGORY_CODE_LENGTH = 2;
    private static final int SOURCE_LENGTH = 10;
    private static final int DESCRIPTION_LENGTH = 100;
    private static final int MERCHANT_NAME_LENGTH = 50;
    private static final int MERCHANT_CITY_LENGTH = 50;
    private static final int MERCHANT_ZIP_LENGTH = 10;
    private static final int CARD_NUMBER_LENGTH = 16;

    /**
     * Transaction ID - Primary key.
     * Maps to TRAN-ID field from COBOL copybook (PIC X(16)).
     * Unique identifier for each transaction in the system.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    /**
     * Transaction amount with COBOL COMP-3 precision preservation.
     * Maps to TRAN-AMT field from COBOL copybook (PIC S9(09)V99).
     * Uses BigDecimal with scale=2 to maintain exact monetary calculations.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Transaction amount is required")
    private BigDecimal amount;

    /**
     * Account ID foreign key linking transaction to account.
     * Derived from account relationship for efficient querying.
     */
    @Column(name = "account_id", nullable = false)
    @NotNull(message = "Account ID is required")
    private Long accountId;

    /**
     * Transaction date for partitioning and date-range queries.
     * Supports partition-aware queries through PostgreSQL range partitioning.
     */
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    /**
     * Transaction description.
     * Maps to TRAN-DESC field from COBOL copybook (PIC X(100)).
     */
    @Column(name = "description", length = DESCRIPTION_LENGTH)
    @Size(max = DESCRIPTION_LENGTH, message = "Description cannot exceed " + DESCRIPTION_LENGTH + " characters")
    private String description;

    /**
     * Merchant ID for transaction tracking.
     * Maps to TRAN-MERCHANT-ID field from COBOL copybook (PIC 9(09)).
     */
    @Column(name = "merchant_id")
    private Long merchantId;

    /**
     * Merchant name.
     * Maps to TRAN-MERCHANT-NAME field from COBOL copybook (PIC X(50)).
     */
    @Column(name = "merchant_name", length = MERCHANT_NAME_LENGTH)
    @Size(max = MERCHANT_NAME_LENGTH, message = "Merchant name cannot exceed " + MERCHANT_NAME_LENGTH + " characters")
    private String merchantName;

    /**
     * Merchant city.
     * Maps to TRAN-MERCHANT-CITY field from COBOL copybook (PIC X(50)).
     */
    @Column(name = "merchant_city", length = MERCHANT_CITY_LENGTH)
    @Size(max = MERCHANT_CITY_LENGTH, message = "Merchant city cannot exceed " + MERCHANT_CITY_LENGTH + " characters")
    private String merchantCity;

    /**
     * Merchant ZIP code.
     * Maps to TRAN-MERCHANT-ZIP field from COBOL copybook (PIC X(10)).
     */
    @Column(name = "merchant_zip", length = MERCHANT_ZIP_LENGTH)
    @Size(max = MERCHANT_ZIP_LENGTH, message = "Merchant ZIP cannot exceed " + MERCHANT_ZIP_LENGTH + " characters")
    private String merchantZip;

    /**
     * Card number used for the transaction.
     * Maps to TRAN-CARD-NUM field from COBOL copybook (PIC X(16)).
     */
    @Column(name = "card_number", length = CARD_NUMBER_LENGTH)
    @Size(max = CARD_NUMBER_LENGTH, message = "Card number cannot exceed " + CARD_NUMBER_LENGTH + " characters")
    private String cardNumber;

    /**
     * Original transaction timestamp.
     * Maps to TRAN-ORIG-TS field from COBOL copybook (PIC X(26)).
     */
    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp;

    /**
     * Processed transaction timestamp.
     * Maps to TRAN-PROC-TS field from COBOL copybook (PIC X(26)).
     */
    @Column(name = "processed_timestamp")
    private LocalDateTime processedTimestamp;

    /**
     * Transaction category code for classification.
     * Maps to TRAN-CAT-CD field from COBOL copybook (PIC 9(04)).
     */
    @Column(name = "category_code", length = CATEGORY_CODE_LENGTH)
    @Size(max = CATEGORY_CODE_LENGTH, message = "Category code cannot exceed " + CATEGORY_CODE_LENGTH + " characters")
    private String categoryCode;

    /**
     * Transaction subcategory code for detailed classification.
     * Maps to TRAN-SUBCAT-CD field from COBOL copybook (PIC X(02)).
     */
    @Column(name = "subcategory_code", length = 2)
    @Size(max = 2, message = "Subcategory code cannot exceed 2 characters")
    private String subcategoryCode;

    /**
     * Transaction source indicator.
     * Maps to TRAN-SOURCE field from COBOL copybook (PIC X(10)).
     */
    @Column(name = "source", length = SOURCE_LENGTH)
    @Size(max = SOURCE_LENGTH, message = "Source cannot exceed " + SOURCE_LENGTH + " characters")
    private String source;

    /**
     * Transaction type code for classification.
     * Maps to TRAN-TYPE-CD field from COBOL copybook (PIC X(02)).
     */
    @Column(name = "transaction_type_code", length = TYPE_CODE_LENGTH)
    @Size(max = TYPE_CODE_LENGTH, message = "Transaction type code cannot exceed " + TYPE_CODE_LENGTH + " characters")
    private String transactionTypeCode;

    /**
     * Authorization code for transaction approval.
     * Maps to TRAN-AUTH-CD field from COBOL copybook (PIC X(06)).
     * Used for transaction authorization verification and approval tracking.
     */
    @Column(name = "authorization_code", length = 6)
    @Size(max = 6, message = "Authorization code cannot exceed 6 characters")
    private String authorizationCode;

    // Relationship entities

    /**
     * Account relationship.
     * @ManyToOne relationship with Account entity using account_id foreign key.
     */
    @ManyToOne
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Card relationship.
     * @ManyToOne relationship with Card entity using card_number foreign key.
     */
    @ManyToOne
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;

    /**
     * Transaction type relationship.
     * @ManyToOne relationship with TransactionType entity using transaction_type_code foreign key.
     */
    @ManyToOne
    @JoinColumn(name = "transaction_type_code", insertable = false, updatable = false)
    private TransactionType transactionType;

    /**
     * Transaction category relationship.
     * @ManyToOne relationship with TransactionCategory entity using composite key.
     */
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "category_code", referencedColumnName = "category_code", insertable = false, updatable = false),
        @JoinColumn(name = "subcategory_code", referencedColumnName = "subcategory_code", insertable = false, updatable = false)
    })
    private TransactionCategory transactionCategory;



    // Getters and Setters - Required by exports schema

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        if (transactionType != null) {
            this.transactionTypeCode = transactionType.getTransactionTypeCode();
        }
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getProcessedTimestamp() {
        return processedTimestamp;
    }

    public void setProcessedTimestamp(LocalDateTime processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getSubcategoryCode() {
        return subcategoryCode;
    }

    public void setSubcategoryCode(String subcategoryCode) {
        this.subcategoryCode = subcategoryCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    // Additional getters/setters for relationship entities

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
        if (card != null) {
            this.cardNumber = card.getCardNumber();
        }
    }

    public TransactionCategory getTransactionCategory() {
        return transactionCategory;
    }

    public void setTransactionCategory(TransactionCategory transactionCategory) {
        this.transactionCategory = transactionCategory;
        if (transactionCategory != null) {
            this.categoryCode = transactionCategory.getCategoryCode();
            this.subcategoryCode = transactionCategory.getSubcategoryCode();
        }
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    // JPA lifecycle methods

    /**
     * JPA lifecycle callback for validation before persisting a new transaction.
     */
    @PrePersist
    public void validateBeforeInsert() {
        performTransactionValidation();
        initializeDefaults();
        formatFields();
    }

    /**
     * JPA lifecycle callback for validation before updating an existing transaction.
     */
    @PreUpdate
    public void validateBeforeUpdate() {
        performTransactionValidation();
        formatFields();
    }

    /**
     * Performs comprehensive transaction field validation using business rules.
     */
    private void performTransactionValidation() {
        // Validate required fields
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount is required");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
        
        // Validate amount precision (must be positive for most transaction types)
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Transaction amount cannot have more than 2 decimal places");
        }
        
        // Validate date is not in the future
        if (transactionDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }
    }

    /**
     * Initializes default values for new transaction records.
     */
    private void initializeDefaults() {
        if (originalTimestamp == null) {
            originalTimestamp = LocalDateTime.now();
        }
        
        if (processedTimestamp == null) {
            processedTimestamp = LocalDateTime.now();
        }
        
        if (source == null) {
            source = "WEB";
        }
    }

    /**
     * Formats transaction fields to ensure COBOL-compatible data formats.
     */
    private void formatFields() {
        // Ensure proper scale for amount (scale=2 for COBOL COMP-3 precision)
        if (amount != null) {
            amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        // Format string fields to uppercase and trim whitespace
        if (description != null) {
            description = description.trim();
        }
        
        if (merchantName != null) {
            merchantName = merchantName.trim();
        }
        
        if (merchantCity != null) {
            merchantCity = merchantCity.trim();
        }
        
        if (merchantZip != null) {
            merchantZip = merchantZip.trim();
        }
        
        if (source != null) {
            source = source.trim().toUpperCase();
        }
        
        if (transactionTypeCode != null) {
            transactionTypeCode = transactionTypeCode.trim().toUpperCase();
        }
        
        if (categoryCode != null) {
            categoryCode = categoryCode.trim();
        }
        
        if (subcategoryCode != null) {
            subcategoryCode = subcategoryCode.trim().toUpperCase();
        }
        
        if (authorizationCode != null) {
            authorizationCode = authorizationCode.trim().toUpperCase();
        }
    }

    // Business utility methods using members_accessed from dependencies

    /**
     * Validates transaction against account balance and limits.
     * Uses Account.getCurrentBalance() and Account.getCreditLimit().
     */
    public boolean isTransactionValid() {
        if (account == null || amount == null) {
            return false;
        }
        
        // Use members_accessed from Account
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal creditLimit = account.getCreditLimit();
        
        // Check if transaction would exceed credit limit
        BigDecimal potentialBalance = currentBalance.add(amount);
        return potentialBalance.compareTo(creditLimit) <= 0;
    }

    /**
     * Checks if the associated card is valid for this transaction.
     * Uses Card.getExpirationDate() and Card.getActiveStatus().
     */
    public boolean isCardValid() {
        if (card == null) {
            return false;
        }
        
        // Use members_accessed from Card
        LocalDate expirationDate = card.getExpirationDate();
        String activeStatus = card.getActiveStatus();
        
        // Check if card is active and not expired
        boolean isActive = "Y".equals(activeStatus);
        boolean isNotExpired = expirationDate == null || !expirationDate.isBefore(LocalDate.now());
        
        return isActive && isNotExpired;
    }

    /**
     * Determines transaction classification based on type.
     * Uses TransactionType.getDebitCreditFlag() and TransactionType.getTypeDescription().
     */
    public String getTransactionClassification() {
        if (transactionType == null) {
            return "UNKNOWN";
        }
        
        // Use members_accessed from TransactionType
        String debitCreditFlag = transactionType.getDebitCreditFlag();
        String typeDescription = transactionType.getTypeDescription();
        
        return String.format("%s (%s)", typeDescription, 
                           "D".equals(debitCreditFlag) ? "DEBIT" : "CREDIT");
    }

    /**
     * Gets category information for reporting.
     * Uses TransactionCategory.getCategoryDescription() and TransactionCategory.getSubcategoryCode().
     */
    public String getCategoryInfo() {
        if (transactionCategory == null) {
            return "UNCATEGORIZED";
        }
        
        // Use members_accessed from TransactionCategory
        String categoryDescription = transactionCategory.getCategoryDescription();
        String subcategoryCode = transactionCategory.getSubcategoryCode();
        
        return String.format("%s (Sub: %s)", categoryDescription, subcategoryCode);
    }

    /**
     * Custom equals method to properly compare Transaction entities.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Transaction that = (Transaction) o;
        
        // Primary comparison by transaction ID
        return Objects.equals(transactionId, that.transactionId);
    }

    /**
     * Custom hash code method using Objects.hash() for consistency with equals().
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    /**
     * Custom toString method providing detailed transaction information.
     */
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", amount=" + amount +
                ", accountId=" + accountId +
                ", transactionDate=" + transactionDate +
                ", description='" + description + '\'' +
                ", merchantId=" + merchantId +
                ", merchantName='" + merchantName + '\'' +
                ", merchantCity='" + merchantCity + '\'' +
                ", merchantZip='" + merchantZip + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", originalTimestamp=" + originalTimestamp +
                ", processedTimestamp=" + processedTimestamp +
                ", categoryCode='" + categoryCode + '\'' +
                ", subcategoryCode='" + subcategoryCode + '\'' +
                ", source='" + source + '\'' +
                ", transactionTypeCode='" + transactionTypeCode + '\'' +
                '}';
    }

    // Additional static utility methods - Required by exports schema

    /**
     * Sets the transaction type code directly.
     * Additional function export as specified in schema.
     * 
     * @param transaction the transaction to update
     * @param typeCode the transaction type code to set
     */
    public static void setTypeCode(Transaction transaction, String typeCode) {
        if (transaction != null) {
            transaction.setTransactionTypeCode(typeCode);
        }
    }

    /**
     * Sets the processing date for the transaction.
     * Additional function export as specified in schema.
     * 
     * @param transaction the transaction to update
     * @param processingDate the processing date to set
     */
    public static void setProcessingDate(Transaction transaction, LocalDate processingDate) {
        if (transaction != null) {
            transaction.setProcessedTimestamp(processingDate.atStartOfDay());
        }
    }
}