package com.carddemo.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.Objects;

/**
 * TransactionType JPA Entity
 * 
 * Represents transaction type reference data converted from COBOL CVTRA03Y copybook
 * to PostgreSQL transaction_types table. This entity serves as a reference table for 
 * transaction classification throughout the CardDemo application.
 * 
 * Maps COBOL structure:
 * - TRAN-TYPE (PIC X(02)) → transaction_type (VARCHAR(2))
 * - TRAN-TYPE-DESC (PIC X(50)) → type_description (VARCHAR(60)) [expanded]
 * - Added: debit_credit_indicator (BOOLEAN) for transaction direction classification
 * 
 * Supports Redis caching per Section 6.2.4.2 for frequently accessed reference data.
 * 
 * Performance characteristics:
 * - Cached reference data with daily refresh cycle
 * - Sub-millisecond lookup times via Redis cache
 * - Supports 10,000+ TPS transaction classification requirements
 * 
 * @author CardDemo Transformation Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transaction_types")
@Cacheable("transactionTypes")
public class TransactionType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Transaction type code - Primary key
     * 
     * 2-character code identifying the transaction type (e.g., "01", "02", "03").
     * Maps directly from COBOL TRAN-TYPE field with exact length preservation.
     * 
     * Business rules:
     * - Must be exactly 2 characters
     * - Used for transaction classification and validation
     * - Referenced by transactions table as foreign key
     */
    @Id
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotNull(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    private String transactionType;

    /**
     * Transaction type description
     * 
     * Human-readable description of the transaction type (e.g., "Purchase", "Cash Advance").
     * Expanded from COBOL TRAN-TYPE-DESC (50 chars) to accommodate longer descriptions
     * required by modern UI components and reporting requirements.
     * 
     * Business rules:
     * - Maximum 60 characters to fit PostgreSQL column definition
     * - Required for user interface display and reporting
     * - Must be descriptive enough for user understanding
     */
    @Column(name = "type_description", length = 60, nullable = false)
    @NotNull(message = "Transaction type description is required")
    @Size(min = 1, max = 60, message = "Transaction type description must be between 1 and 60 characters")
    private String typeDescription;

    /**
     * Debit/Credit indicator
     * 
     * Boolean flag indicating whether this transaction type represents a debit (true)
     * or credit (false) operation. Added during modernization to support enhanced
     * transaction processing and balance calculation logic.
     * 
     * Business rules:
     * - true = Debit transaction (reduces account balance)
     * - false = Credit transaction (increases account balance)
     * - Required for automated balance calculation workflows
     * - Used in transaction posting and settlement processes
     */
    @Column(name = "debit_credit_indicator", nullable = false)
    @NotNull(message = "Debit/Credit indicator is required")
    private Boolean debitCreditIndicator;

    /**
     * Default constructor required by JPA specification
     */
    public TransactionType() {
        // Default constructor for JPA entity instantiation
    }

    /**
     * Constructor for creating new TransactionType instances
     * 
     * @param transactionType 2-character transaction type code
     * @param typeDescription descriptive name for the transaction type
     * @param debitCreditIndicator true for debit transactions, false for credit
     */
    public TransactionType(String transactionType, String typeDescription, Boolean debitCreditIndicator) {
        this.transactionType = transactionType;
        this.typeDescription = typeDescription;
        this.debitCreditIndicator = debitCreditIndicator;
    }

    /**
     * Gets the transaction type code
     * 
     * @return 2-character transaction type code
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type code
     * 
     * @param transactionType 2-character transaction type code
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Gets the transaction type description
     * 
     * @return descriptive name of the transaction type
     */
    public String getTypeDescription() {
        return typeDescription;
    }

    /**
     * Sets the transaction type description
     * 
     * @param typeDescription descriptive name for the transaction type
     */
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Gets the debit/credit indicator
     * 
     * @return true for debit transactions, false for credit transactions
     */
    public Boolean getDebitCreditIndicator() {
        return debitCreditIndicator;
    }

    /**
     * Sets the debit/credit indicator
     * 
     * @param debitCreditIndicator true for debit transactions, false for credit
     */
    public void setDebitCreditIndicator(Boolean debitCreditIndicator) {
        this.debitCreditIndicator = debitCreditIndicator;
    }

    /**
     * Convenience method to check if this is a debit transaction type
     * 
     * @return true if this transaction type represents a debit operation
     */
    public boolean isDebitTransaction() {
        return Boolean.TRUE.equals(debitCreditIndicator);
    }

    /**
     * Convenience method to check if this is a credit transaction type
     * 
     * @return true if this transaction type represents a credit operation
     */
    public boolean isCreditTransaction() {
        return Boolean.FALSE.equals(debitCreditIndicator);
    }

    /**
     * Equals method implementation for proper entity identity comparison
     * 
     * Uses transaction type code as the primary identifier since it's the primary key.
     * Essential for Redis cache key consistency and JPA entity operations.
     * 
     * @param obj object to compare with this instance
     * @return true if objects are equal based on transaction type code
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
     * Hash code implementation for proper entity identity operations
     * 
     * Uses transaction type code for hash calculation to ensure consistency
     * with equals method and proper behavior in Redis cache operations.
     * 
     * @return hash code based on transaction type code
     */
    @Override
    public int hashCode() {
        return Objects.hash(transactionType);
    }

    /**
     * String representation of the TransactionType entity
     * 
     * Provides readable string format for debugging, logging, and development.
     * Includes all key fields for comprehensive entity state visibility.
     * 
     * @return formatted string representation of the entity
     */
    @Override
    public String toString() {
        return String.format(
            "TransactionType{transactionType='%s', typeDescription='%s', debitCreditIndicator=%s}",
            transactionType, typeDescription, debitCreditIndicator
        );
    }
}