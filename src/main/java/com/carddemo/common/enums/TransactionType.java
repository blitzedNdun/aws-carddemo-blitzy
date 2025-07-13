package com.carddemo.common.enums;

import java.util.Optional;
import jakarta.validation.Valid;

/**
 * TransactionType enumeration defining transaction types converted from COBOL TRAN-TYPE-CD field
 * for transaction processing validation and PostgreSQL reference table integration.
 * 
 * This enum provides comprehensive transaction type classification maintaining identical 
 * validation logic as original CICS transaction processing while supporting modern
 * Spring Boot microservices architecture with PostgreSQL foreign key constraints.
 * 
 * Implementation preserves exact COBOL arithmetic behavior and financial calculations
 * through BigDecimal precision handling in transaction processing services.
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 1.0
 */
public enum TransactionType {
    
    /**
     * Purchase Transaction - Standard card purchase (debit to customer account)
     */
    PU("PU", "Purchase Transaction", true, "Standard credit card purchase or sale transaction"),
    
    /**
     * Refund Transaction - Credit back to customer account
     */
    RF("RF", "Refund Transaction", false, "Credit refund transaction returning funds to customer"),
    
    /**
     * Cash Advance - Cash withdrawal from credit line (debit to customer account)
     */
    CA("CA", "Cash Advance", true, "Cash advance transaction from credit line"),
    
    /**
     * Payment Transaction - Payment toward account balance (credit to customer account)
     */
    PM("PM", "Payment Transaction", false, "Payment transaction reducing account balance"),
    
    /**
     * Authorization - Pre-authorization transaction (debit to customer account)
     */
    AU("AU", "Authorization", true, "Pre-authorization transaction for purchase validation"),
    
    /**
     * Void Transaction - Cancellation of previous transaction
     */
    VD("VD", "Void Transaction", false, "Void transaction canceling previous transaction"),
    
    /**
     * Adjustment - Balance adjustment transaction (can be debit or credit)
     */
    AD("AD", "Adjustment", true, "Balance adjustment transaction for account corrections"),
    
    /**
     * Interest Charge - Interest charge on account balance (debit to customer account)
     */
    IN("IN", "Interest Charge", true, "Interest charge transaction on outstanding balance"),
    
    /**
     * Fee Transaction - Fee charge to customer account (debit to customer account)
     */
    FE("FE", "Fee Transaction", true, "Fee transaction for account maintenance or service fees"),
    
    /**
     * Balance Transfer - Transfer from another account (credit to customer account)
     */
    BT("BT", "Balance Transfer", false, "Balance transfer transaction from external account"),
    
    /**
     * Chargeback - Disputed transaction reversal (credit to customer account)
     */
    CB("CB", "Chargeback", false, "Chargeback transaction for disputed purchase"),
    
    /**
     * Reversal - Transaction reversal (opposite of original transaction)
     */
    RV("RV", "Reversal", false, "Reversal transaction undoing previous transaction");
    
    private final String code;
    private final String description;
    private final boolean debitCreditIndicator;
    private final String businessDescription;
    
    /**
     * Constructor for TransactionType enum values
     * 
     * @param code 2-character transaction type code matching PostgreSQL TRANTYPE table
     * @param description Human-readable description of transaction type
     * @param debitCreditIndicator true for debit (increases customer balance), false for credit (decreases customer balance)
     * @param businessDescription Detailed business description of transaction type purpose
     */
    TransactionType(String code, String description, boolean debitCreditIndicator, String businessDescription) {
        this.code = code;
        this.description = description;
        this.debitCreditIndicator = debitCreditIndicator;
        this.businessDescription = businessDescription;
    }
    
    /**
     * Get the 2-character transaction type code
     * 
     * @return transaction type code for PostgreSQL foreign key reference
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description
     * 
     * @return transaction type description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the debit/credit indicator
     * 
     * @return true for debit transactions (increase customer balance), false for credit transactions (decrease customer balance)
     */
    public boolean getDebitCreditIndicator() {
        return debitCreditIndicator;
    }
    
    /**
     * Get the detailed business description
     * 
     * @return detailed business description of transaction type purpose
     */
    public String getBusinessDescription() {
        return businessDescription;
    }
    
    /**
     * Convert transaction type code to enum value with validation
     * Preserves exact COBOL validation logic for transaction type verification
     * 
     * @param code 2-character transaction type code from TRAN-TYPE-CD field
     * @return Optional containing TransactionType enum if valid, empty if invalid
     */
    public static Optional<TransactionType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedCode = code.trim().toUpperCase();
        
        // Validate code format - must be exactly 2 characters
        if (normalizedCode.length() != 2) {
            return Optional.empty();
        }
        
        // Find matching transaction type
        for (TransactionType type : values()) {
            if (type.getCode().equals(normalizedCode)) {
                return Optional.of(type);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate transaction type code format and value
     * Implements comprehensive validation logic matching original COBOL business rules
     * 
     * @param code transaction type code to validate
     * @return true if code is valid transaction type, false otherwise
     */
    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
    
    /**
     * Validate transaction type instance
     * Provides validation for transaction type operations in REST API endpoints
     * 
     * @param transactionType transaction type to validate
     * @return true if transaction type is valid, false otherwise
     */
    public static boolean isValid(TransactionType transactionType) {
        return transactionType != null;
    }
    
    /**
     * Get all available transaction type codes
     * Supports REST API transaction processing validation in TransactionService
     * 
     * @return array of all valid transaction type codes
     */
    public static String[] getAllCodes() {
        return java.util.Arrays.stream(values())
                .map(TransactionType::getCode)
                .toArray(String[]::new);
    }
    
    /**
     * Get all transaction types for a specific debit/credit indicator
     * Supports business logic validation for transaction processing
     * 
     * @param isDebit true for debit transactions, false for credit transactions
     * @return array of transaction types matching the debit/credit indicator
     */
    public static TransactionType[] getByDebitCreditIndicator(boolean isDebit) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.getDebitCreditIndicator() == isDebit)
                .toArray(TransactionType[]::new);
    }
    
    /**
     * Check if transaction type is a debit transaction
     * 
     * @return true if transaction type increases customer balance (debit), false otherwise
     */
    public boolean isDebit() {
        return debitCreditIndicator;
    }
    
    /**
     * Check if transaction type is a credit transaction
     * 
     * @return true if transaction type decreases customer balance (credit), false otherwise
     */
    public boolean isCredit() {
        return !debitCreditIndicator;
    }
    
    /**
     * Get formatted display string for transaction type
     * Used in REST API responses and React UI components
     * 
     * @return formatted string containing code and description
     */
    public String getDisplayString() {
        return String.format("%s - %s", code, description);
    }
    
    /**
     * Validate transaction type for specific business operations
     * Implements business rules validation matching original COBOL transaction processing
     * 
     * @param operationType type of operation being performed
     * @return true if transaction type is valid for the operation, false otherwise
     */
    public boolean isValidForOperation(String operationType) {
        if (operationType == null || operationType.trim().isEmpty()) {
            return false;
        }
        
        String operation = operationType.trim().toUpperCase();
        
        switch (operation) {
            case "PURCHASE":
                return this == PU || this == CA || this == AU;
            case "REFUND":
                return this == RF || this == RV;
            case "PAYMENT":
                return this == PM || this == BT;
            case "ADJUSTMENT":
                return this == AD || this == CB;
            case "MAINTENANCE":
                return this == FE || this == IN || this == VD;
            default:
                return true; // Allow all transaction types for unspecified operations
        }
    }
    
    /**
     * Get transaction type impact on account balance
     * Supports financial calculations in AddTransactionService
     * 
     * @return 1 for debit transactions (increase balance), -1 for credit transactions (decrease balance)
     */
    public int getBalanceImpact() {
        return debitCreditIndicator ? 1 : -1;
    }
    
    /**
     * String representation of transaction type
     * 
     * @return transaction type code for logging and debugging
     */
    @Override
    public String toString() {
        return code;
    }
}