package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * JPA entity representing the transaction_types table in PostgreSQL database.
 * Maps to the VSAM TRANTYPE reference file for transaction type classification.
 * 
 * This entity provides lookup data for transaction type validation and categorization,
 * supporting transaction processing operations with debit/credit flag determination.
 * 
 * Based on COBOL copybook app/cpy/CVTRA03Y.cpy with TRAN-TYPE-RECORD structure.
 * Maintains compatibility with original VSAM TRANTYPE file organization.
 * 
 * Primary Key: transaction_type_code (2-character code)
 * Key Features:
 * - Transaction type classification (Purchase, Payment, Fee, etc.)
 * - Debit/credit flag for accounting purposes ('D' for debit, 'C' for credit)
 * - Reference data for transaction validation
 * - Cache-friendly structure for high-frequency lookups
 * 
 * @author Blitzy CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "transaction_types", 
       indexes = {
           @Index(name = "idx_transaction_types_debit_credit_flag", columnList = "debit_credit_flag"),
           @Index(name = "idx_transaction_types_description", columnList = "type_description")
       })
public class TransactionType {

    /**
     * Primary key - unique 2-character transaction type code.
     * Examples: "01" = Purchase, "02" = Payment, "03" = Fee Assessment, etc.
     * Maps to TRAN-TYPE-CD in COBOL copybook.
     */
    @Id
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    @NotNull(message = "Transaction type code is required")
    @Size(min = 2, max = 2, message = "Transaction type code must be exactly 2 characters")
    @Pattern(regexp = "^[0-9A-Z]{2}$", message = "Transaction type code must contain only digits and uppercase letters")
    private String transactionTypeCode;

    /**
     * Descriptive name for the transaction type.
     * Used for display purposes and administrative functions.
     * Maps to TRAN-TYPE-DESC in COBOL copybook.
     */
    @Column(name = "type_description", length = 50, nullable = false)
    @NotNull(message = "Transaction type description is required")
    @Size(min = 1, max = 50, message = "Transaction type description must be between 1 and 50 characters")
    private String typeDescription;

    /**
     * Single character flag indicating debit ('D') or credit ('C') classification.
     * Used for accounting operations and balance calculations.
     * Maps to TRAN-TYPE-DR-CR in COBOL copybook.
     */
    @Column(name = "debit_credit_flag", length = 1, nullable = false)
    @NotNull(message = "Debit/credit flag is required")
    @Pattern(regexp = "^[DC]$", message = "Debit/credit flag must be 'D' for debit or 'C' for credit")
    private String debitCreditFlag;

    /**
     * Default constructor for JPA.
     */
    public TransactionType() {
    }

    /**
     * Constructor with all required fields.
     * 
     * @param transactionTypeCode 2-character transaction type code
     * @param typeDescription descriptive name for the transaction type
     * @param debitCreditFlag 'D' for debit, 'C' for credit
     */
    public TransactionType(String transactionTypeCode, String typeDescription, String debitCreditFlag) {
        this.transactionTypeCode = transactionTypeCode;
        this.typeDescription = typeDescription;
        this.debitCreditFlag = debitCreditFlag;
    }

    // Getters and Setters

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getDebitCreditFlag() {
        return debitCreditFlag;
    }

    public void setDebitCreditFlag(String debitCreditFlag) {
        this.debitCreditFlag = debitCreditFlag;
    }

    // Utility Methods

    /**
     * Checks if this transaction type represents a debit operation.
     * 
     * @return true if debitCreditFlag is 'D', false otherwise
     */
    public boolean isDebit() {
        return "D".equals(debitCreditFlag);
    }

    /**
     * Checks if this transaction type represents a credit operation.
     * 
     * @return true if debitCreditFlag is 'C', false otherwise
     */
    public boolean isCredit() {
        return "C".equals(debitCreditFlag);
    }

    /**
     * Returns a formatted display string for UI purposes.
     * 
     * @return formatted string with code and description
     */
    public String getDisplayText() {
        return String.format("%s - %s", transactionTypeCode, typeDescription);
    }

    /**
     * Returns the accounting classification as a descriptive string.
     * 
     * @return "Debit" or "Credit" based on debitCreditFlag
     */
    public String getAccountingClassification() {
        return isDebit() ? "Debit" : "Credit";
    }

    // Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TransactionType that = (TransactionType) o;
        return transactionTypeCode != null ? transactionTypeCode.equals(that.transactionTypeCode) : that.transactionTypeCode == null;
    }

    @Override
    public int hashCode() {
        return transactionTypeCode != null ? transactionTypeCode.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("TransactionType{transactionTypeCode='%s', typeDescription='%s', debitCreditFlag='%s'}", 
                           transactionTypeCode, typeDescription, debitCreditFlag);
    }
}