package com.carddemo.common.enums;

import java.util.Optional;
import jakarta.validation.Valid;

/**
 * TransactionType enumeration defining transaction types converted from COBOL TRAN-TYPE-CD field
 * for transaction processing validation and PostgreSQL reference table integration.
 * 
 * This enum preserves exact COBOL arithmetic behavior and financial calculations while supporting
 * PostgreSQL foreign key constraints and JPA entity relationship mapping. Type classification
 * maintains identical validation logic as original CICS transaction processing.
 * 
 * Corresponds to TRANTYPE reference table with 2-character transaction type codes.
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @author Blitzy Agent
 */
public enum TransactionType {

    // Purchase transactions (Debit)
    /**
     * Standard purchase transaction - merchant point of sale
     * Debit to account balance
     */
    PU("PU", "Purchase Transaction", true),
    
    /**
     * Online purchase transaction - e-commerce
     * Debit to account balance
     */
    ON("ON", "Online Purchase", true),
    
    /**
     * Recurring payment transaction - subscription/auto-pay
     * Debit to account balance
     */
    RP("RP", "Recurring Payment", true),
    
    // Cash advance transactions (Debit)
    /**
     * ATM cash advance transaction
     * Debit to account balance with cash advance fees
     */
    CA("CA", "Cash Advance", true),
    
    /**
     * Cash advance at bank branch
     * Debit to account balance with cash advance fees
     */
    CB("CB", "Cash Advance - Branch", true),
    
    // Credit transactions (Credit)
    /**
     * Payment received - customer payment
     * Credit to account balance
     */
    PM("PM", "Payment Received", false),
    
    /**
     * Credit adjustment - merchant refund
     * Credit to account balance
     */
    CR("CR", "Credit Adjustment", false),
    
    /**
     * Return transaction - purchase return
     * Credit to account balance
     */
    RT("RT", "Return/Refund", false),
    
    // Fee transactions (Debit)
    /**
     * Annual fee charge
     * Debit to account balance
     */
    AF("AF", "Annual Fee", true),
    
    /**
     * Late payment fee
     * Debit to account balance
     */
    LF("LF", "Late Fee", true),
    
    /**
     * Over-limit fee
     * Debit to account balance
     */
    OF("OF", "Over-Limit Fee", true),
    
    /**
     * Interest charge on balance
     * Debit to account balance
     */
    IN("IN", "Interest Charge", true),
    
    // Administrative transactions
    /**
     * Balance transfer transaction
     * Can be debit or credit depending on direction
     */
    BT("BT", "Balance Transfer", true),
    
    /**
     * Dispute adjustment transaction
     * Can be debit or credit depending on resolution
     */
    DA("DA", "Dispute Adjustment", false),
    
    /**
     * Administrative adjustment
     * Can be debit or credit depending on nature
     */
    AA("AA", "Administrative Adjustment", false);

    // Instance fields preserving COBOL TRAN-TYPE-CD structure
    private final String code;              // 2-character transaction type code (PIC X(02))
    private final String description;       // Transaction type description (up to 60 chars)
    private final boolean isDebit;         // Debit/Credit indicator (true = debit, false = credit)

    /**
     * Private constructor for enum values
     * Maintains COBOL field validation patterns
     * 
     * @param code 2-character transaction type code matching PostgreSQL TRANTYPE primary key
     * @param description Human-readable description of transaction type
     * @param isDebit true for debit transactions (increase balance), false for credit (decrease balance)
     */
    TransactionType(String code, String description, boolean isDebit) {
        this.code = code;
        this.description = description;
        this.isDebit = isDebit;
    }

    /**
     * Get the 2-character transaction type code
     * Corresponds to TRAN-TYPE-CD field in COBOL and transaction_type column in PostgreSQL
     * 
     * @return 2-character transaction type code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the transaction type description
     * Corresponds to type_description column in PostgreSQL TRANTYPE table
     * 
     * @return Human-readable transaction type description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the debit/credit indicator for transaction type
     * Preserves COBOL business logic for financial calculations
     * 
     * @return true if transaction type is a debit (increases balance owed), 
     *         false if credit (decreases balance owed)
     */
    public boolean getDebitCreditIndicator() {
        return isDebit;
    }

    /**
     * Check if this transaction type represents a debit transaction
     * Used for account balance calculation validation
     * 
     * @return true if this is a debit transaction type
     */
    public boolean isDebit() {
        return isDebit;
    }

    /**
     * Check if this transaction type represents a credit transaction
     * Used for account balance calculation validation
     * 
     * @return true if this is a credit transaction type
     */
    public boolean isCredit() {
        return !isDebit;
    }

    /**
     * Validate if this enum instance is valid (always true for enum values)
     * Provides consistent interface for validation frameworks
     * 
     * @return true (enum values are always valid)
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Lookup TransactionType by 2-character code
     * Provides safe conversion from COBOL TRAN-TYPE-CD field values
     * Supports null-safe processing for transaction type operations
     * 
     * @param code 2-character transaction type code (case-sensitive)
     * @return Optional<TransactionType> containing matching enum value or Optional.empty() if not found
     */
    public static Optional<TransactionType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Normalize code to uppercase for comparison
        String normalizedCode = code.trim().toUpperCase();
        
        for (TransactionType type : values()) {
            if (type.code.equals(normalizedCode)) {
                return Optional.of(type);
            }
        }
        
        return Optional.empty();
    }

    /**
     * Validate if a transaction type code is valid
     * Used by transaction processing services for input validation
     * 
     * @param code 2-character transaction type code to validate
     * @return true if code corresponds to a valid TransactionType, false otherwise
     */
    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }

    // Note: values() method is automatically provided by Java enum
    // Original COBOL equivalent: iterate through all valid transaction type codes
    // Usage: TransactionType.values() returns TransactionType[] array

    /**
     * String representation of transaction type
     * Format: "CODE - Description (Debit/Credit)"
     * Used for logging and debugging purposes
     * 
     * @return String representation including code, description, and debit/credit indicator
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", code, description, isDebit ? "Debit" : "Credit");
    }

    // Note: equals() and hashCode() methods are automatically provided by Java enum
    // Java enum equality is based on enum instance identity, which is appropriate
    // for transaction type comparison in business logic and hash-based collections
}