package com.carddemo.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.Objects;

/**
 * JPA TransactionType reference entity mapping COBOL transaction type structure
 * to PostgreSQL transaction_types table with 2-character type codes, descriptions,
 * debit/credit indicators, and Redis caching support for transaction classification.
 * 
 * This entity serves as a reference table for transaction classification per Section 6.2.1.2
 * and supports Redis caching for frequently accessed reference data per Section 6.2.4.2.
 * 
 * Original COBOL structure from CVTRA03Y.cpy:
 * - TRAN-TYPE (PIC X(02)) -> transaction_type VARCHAR(2)
 * - TRAN-TYPE-DESC (PIC X(50)) -> type_description VARCHAR(60) [expanded]
 * - Added debit_credit_indicator BOOLEAN for transaction direction classification
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transaction_types")
@Cacheable(value = "transactionTypes", cacheManager = "redisCacheManager")
public class TransactionType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - 2-character transaction type code for classification.
     * Maps to TRAN-TYPE from COBOL copybook CVTRA03Y.cpy.
     * 
     * Examples: "01" = Purchase, "02" = Cash Advance, "03" = Payment
     */
    @Id
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotBlank(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type must be exactly 2 characters")
    private String transactionType;

    /**
     * Descriptive name for the transaction type.
     * Maps to TRAN-TYPE-DESC from COBOL copybook CVTRA03Y.cpy.
     * Extended from 50 to 60 characters for PostgreSQL implementation.
     * 
     * Examples: "Purchase Transaction", "Cash Advance", "Payment Credit"
     */
    @Column(name = "type_description", length = 60, nullable = false)
    @NotBlank(message = "Type description is required")
    @Size(min = 1, max = 60, message = "Type description must be between 1 and 60 characters")
    private String typeDescription;

    /**
     * Debit/Credit indicator for transaction direction classification.
     * Added for PostgreSQL implementation to support transaction processing workflows.
     * 
     * true = Debit transaction (reduces account balance)
     * false = Credit transaction (increases account balance)
     */
    @Column(name = "debit_credit_indicator", nullable = false)
    @NotNull(message = "Debit/Credit indicator is required")
    private Boolean debitCreditIndicator;

    /**
     * Default constructor for JPA entity instantiation.
     */
    public TransactionType() {
        // Default constructor required by JPA
    }

    /**
     * Constructor with all required fields for entity creation.
     * 
     * @param transactionType 2-character transaction type code
     * @param typeDescription descriptive name for the transaction type
     * @param debitCreditIndicator debit/credit direction indicator
     */
    public TransactionType(String transactionType, String typeDescription, Boolean debitCreditIndicator) {
        this.transactionType = transactionType;
        this.typeDescription = typeDescription;
        this.debitCreditIndicator = debitCreditIndicator;
    }

    /**
     * Gets the transaction type code.
     * 
     * @return 2-character transaction type code
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type code.
     * 
     * @param transactionType 2-character transaction type code
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction type description.
     * 
     * @return descriptive name for the transaction type
     */
    public String getTypeDescription() {
        return typeDescription;
    }

    /**
     * Sets the transaction type description.
     * 
     * @param typeDescription descriptive name for the transaction type
     */
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Gets the debit/credit indicator.
     * 
     * @return true for debit transactions, false for credit transactions
     */
    public Boolean getDebitCreditIndicator() {
        return debitCreditIndicator;
    }

    /**
     * Sets the debit/credit indicator.
     * 
     * @param debitCreditIndicator true for debit transactions, false for credit transactions
     */
    public void setDebitCreditIndicator(Boolean debitCreditIndicator) {
        this.debitCreditIndicator = debitCreditIndicator;
    }

    /**
     * Determines if this transaction type represents a debit transaction.
     * 
     * @return true if this is a debit transaction type
     */
    public boolean isDebitTransaction() {
        return debitCreditIndicator != null && debitCreditIndicator;
    }

    /**
     * Determines if this transaction type represents a credit transaction.
     * 
     * @return true if this is a credit transaction type
     */
    public boolean isCreditTransaction() {
        return debitCreditIndicator != null && !debitCreditIndicator;
    }

    /**
     * Compares this TransactionType with another object for equality.
     * Two TransactionType objects are considered equal if they have the same transaction type code.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransactionType that = (TransactionType) obj;
        return Objects.equals(transactionType, that.transactionType);
    }

    /**
     * Returns the hash code for this TransactionType.
     * The hash code is based on the transaction type code.
     * 
     * @return hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionType);
    }

    /**
     * Returns a string representation of this TransactionType.
     * 
     * @return string representation including all fields
     */
    @Override
    public String toString() {
        return "TransactionType{" +
                "transactionType='" + transactionType + '\'' +
                ", typeDescription='" + typeDescription + '\'' +
                ", debitCreditIndicator=" + debitCreditIndicator +
                '}';
    }
}