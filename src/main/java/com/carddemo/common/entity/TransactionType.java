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
 * Mapped from original COBOL copybook: app/cpy/CVTRA03Y.cpy
 * - TRAN-TYPE PIC X(02) → transaction_type VARCHAR(2)
 * - TRAN-TYPE-DESC PIC X(50) → type_description VARCHAR(60) (expanded)
 * - Added debit_credit_indicator BOOLEAN for transaction direction classification
 * 
 * Cache Configuration:
 * - Redis reference cache with daily refresh frequency
 * - TTL: 24 hours for static reference data
 * - Cache key: "transactionTypes"
 * - Supports Spring Cache abstraction with @Cacheable annotation
 */
@Entity
@Table(name = "transaction_types")
@Cacheable("transactionTypes")
public class TransactionType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Transaction type code - 2-character identifier for transaction classification.
     * Primary key field mapping from COBOL TRAN-TYPE PIC X(02).
     * Used as foreign key reference in transactions table.
     */
    @Id
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotBlank(message = "Transaction type code cannot be blank")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    private String transactionType;

    /**
     * Transaction type description - descriptive name for the transaction type.
     * Mapped from COBOL TRAN-TYPE-DESC PIC X(50), expanded to 60 characters
     * for enhanced description capabilities in the modernized system.
     */
    @Column(name = "type_description", length = 60, nullable = false)
    @NotBlank(message = "Transaction type description cannot be blank")
    @Size(min = 1, max = 60, message = "Transaction type description must be between 1 and 60 characters")
    private String typeDescription;

    /**
     * Debit/Credit indicator - boolean flag indicating transaction direction.
     * Added field for transaction classification enhancement:
     * - true: Credit transaction (increases account balance)
     * - false: Debit transaction (decreases account balance)
     * 
     * This field enables automated transaction processing validation
     * and supports proper accounting logic in the microservices architecture.
     */
    @Column(name = "debit_credit_indicator", nullable = false)
    @NotNull(message = "Debit credit indicator cannot be null")
    private Boolean debitCreditIndicator;

    /**
     * Default constructor required by JPA specification.
     * Initializes entity with default values for all fields.
     */
    public TransactionType() {
        // Default constructor for JPA
    }

    /**
     * Constructor with all required fields for creating new TransactionType instances.
     * 
     * @param transactionType 2-character transaction type code
     * @param typeDescription descriptive name for the transaction type
     * @param debitCreditIndicator true for credit transactions, false for debit transactions
     */
    public TransactionType(String transactionType, String typeDescription, Boolean debitCreditIndicator) {
        this.transactionType = transactionType;
        this.typeDescription = typeDescription;
        this.debitCreditIndicator = debitCreditIndicator;
    }

    /**
     * Gets the 2-character transaction type code.
     * This code serves as the primary key and is used as a foreign key
     * reference in the transactions table for transaction classification.
     * 
     * @return transaction type code (2 characters)
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the 2-character transaction type code.
     * Must be exactly 2 characters in length and cannot be null or blank.
     * 
     * @param transactionType transaction type code (2 characters)
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the descriptive name for the transaction type.
     * Provides human-readable description for transaction classification
     * and reporting purposes.
     * 
     * @return transaction type description (up to 60 characters)
     */
    public String getTypeDescription() {
        return typeDescription;
    }

    /**
     * Sets the descriptive name for the transaction type.
     * Description must be between 1 and 60 characters in length.
     * 
     * @param typeDescription transaction type description
     */
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Gets the debit/credit indicator for transaction direction classification.
     * Used in transaction processing to determine account balance impact:
     * - true: Credit transaction (positive account impact)
     * - false: Debit transaction (negative account impact)
     * 
     * @return true for credit transactions, false for debit transactions
     */
    public Boolean getDebitCreditIndicator() {
        return debitCreditIndicator;
    }

    /**
     * Sets the debit/credit indicator for transaction direction classification.
     * This flag is used in automated transaction processing and validation logic.
     * 
     * @param debitCreditIndicator true for credit transactions, false for debit transactions
     */
    public void setDebitCreditIndicator(Boolean debitCreditIndicator) {
        this.debitCreditIndicator = debitCreditIndicator;
    }

    /**
     * Indicates whether this transaction type represents a credit transaction.
     * Convenience method for readable transaction direction checking.
     * 
     * @return true if this is a credit transaction type
     */
    public boolean isCredit() {
        return Boolean.TRUE.equals(debitCreditIndicator);
    }

    /**
     * Indicates whether this transaction type represents a debit transaction.
     * Convenience method for readable transaction direction checking.
     * 
     * @return true if this is a debit transaction type
     */
    public boolean isDebit() {
        return Boolean.FALSE.equals(debitCreditIndicator);
    }

    /**
     * Compares this TransactionType with another object for equality.
     * Two TransactionType entities are considered equal if they have the same
     * transaction type code (primary key).
     * 
     * This method is essential for proper JPA entity identity operations
     * and Redis cache key consistency in the distributed caching architecture.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TransactionType that = (TransactionType) obj;
        return Objects.equals(transactionType, that.transactionType);
    }

    /**
     * Returns a hash code value for this TransactionType entity.
     * The hash code is based on the transaction type code (primary key)
     * to ensure consistency with the equals() method.
     * 
     * This method is essential for proper HashMap/HashSet operations
     * and Redis cache key generation in the Spring Cache abstraction.
     * 
     * @return hash code value for this entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionType);
    }

    /**
     * Returns a string representation of this TransactionType entity.
     * Includes all key fields for debugging and logging purposes.
     * 
     * Format: TransactionType{code='XX', description='...', credit=true/false}
     * 
     * @return string representation of the entity
     */
    @Override
    public String toString() {
        return String.format("TransactionType{code='%s', description='%s', credit=%s}",
                transactionType, typeDescription, debitCreditIndicator);
    }
}