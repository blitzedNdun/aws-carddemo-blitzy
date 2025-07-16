package com.carddemo.common.enums;

import java.util.Optional;

/**
 * TransactionType enumeration defining transaction types converted from COBOL TRAN-TYPE-CD field
 * for transaction processing validation and PostgreSQL reference table integration.
 * 
 * This enum provides comprehensive transaction type classification with debit/credit indicators
 * maintaining identical validation logic as original CICS transaction processing.
 * 
 * Based on COBOL source files:
 * - app/cpy/CVTRA05Y.cpy: TRAN-TYPE-CD field definition (PIC X(02))
 * - app/cbl/COTRN02C.cbl: Transaction type validation logic
 * - app/cbl/COTRN00C.cbl: Transaction listing with type display
 * - app/cbl/COTRN01C.cbl: Transaction viewing with type information
 * 
 * PostgreSQL Integration:
 * - Maps to TRANTYPE reference table with 2-character transaction_type primary key
 * - Supports JPA entity relationship mapping for transaction classification
 * - Enables foreign key constraints for transaction validation
 * 
 * Performance Requirements:
 * - Validation methods must support sub-200ms transaction processing
 * - Enum lookups optimized for 10,000+ TPS throughput capacity
 * - Memory-efficient static initialization for high-volume operations
 * 
 * Financial Data Integrity:
 * - Debit/credit indicators preserve exact COBOL arithmetic behavior
 * - Transaction type validation maintains CICS business rule compliance
 * - Supports BigDecimal financial calculations with proper sign handling
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public enum TransactionType {
    
    // Purchase and Payment Transactions
    PURCHASE("01", "Purchase Transaction", false, "Regular purchase transaction"),
    PAYMENT("02", "Payment Transaction", true, "Credit card payment"),
    CASH_ADVANCE("03", "Cash Advance", false, "Cash advance transaction"),
    REFUND("04", "Refund Transaction", true, "Purchase refund"),
    
    // Fee and Charge Transactions
    ANNUAL_FEE("05", "Annual Fee", false, "Annual card fee"),
    LATE_FEE("06", "Late Payment Fee", false, "Late payment penalty"),
    OVERLIMIT_FEE("07", "Over Limit Fee", false, "Over credit limit fee"),
    FOREIGN_TRANS_FEE("08", "Foreign Transaction Fee", false, "International transaction fee"),
    
    // Interest and Finance Transactions
    INTEREST_CHARGE("09", "Interest Charge", false, "Interest on outstanding balance"),
    FINANCE_CHARGE("10", "Finance Charge", false, "Finance charge on cash advance"),
    
    // Adjustment Transactions
    CREDIT_ADJUSTMENT("11", "Credit Adjustment", true, "Manual credit adjustment"),
    DEBIT_ADJUSTMENT("12", "Debit Adjustment", false, "Manual debit adjustment"),
    
    // Balance Transfer Transactions
    BALANCE_TRANSFER("13", "Balance Transfer", false, "Balance transfer from another card"),
    BALANCE_TRANSFER_FEE("14", "Balance Transfer Fee", false, "Fee for balance transfer"),
    
    // Dispute and Chargeback Transactions
    CHARGEBACK("15", "Chargeback", true, "Chargeback credit"),
    CHARGEBACK_REVERSAL("16", "Chargeback Reversal", false, "Chargeback reversal debit"),
    
    // Statement and Service Transactions
    STATEMENT_CREDIT("17", "Statement Credit", true, "Statement credit adjustment"),
    SERVICE_CHARGE("18", "Service Charge", false, "Account service charge"),
    
    // Promotional and Reward Transactions
    PROMOTIONAL_CREDIT("19", "Promotional Credit", true, "Promotional credit offer"),
    REWARD_REDEMPTION("20", "Reward Redemption", true, "Reward points redemption"),
    
    // ATM and Electronic Transactions
    ATM_WITHDRAWAL("21", "ATM Withdrawal", false, "ATM cash withdrawal"),
    ATM_FEE("22", "ATM Fee", false, "ATM usage fee"),
    ELECTRONIC_PAYMENT("23", "Electronic Payment", true, "Electronic payment credit"),
    
    // Return and NSF Transactions
    RETURNED_PAYMENT("24", "Returned Payment", false, "Returned payment charge"),
    NSF_FEE("25", "NSF Fee", false, "Non-sufficient funds fee"),
    
    // Maintenance and Other Transactions
    ACCOUNT_MAINTENANCE("26", "Account Maintenance", false, "Account maintenance fee"),
    CARD_REPLACEMENT("27", "Card Replacement Fee", false, "Card replacement fee"),
    EXPEDITED_PAYMENT("28", "Expedited Payment Fee", false, "Expedited payment fee"),
    
    // System and Processing Transactions
    SYSTEM_ADJUSTMENT("29", "System Adjustment", true, "System processing adjustment"),
    PROCESSING_FEE("30", "Processing Fee", false, "Transaction processing fee");
    
    private final String code;
    private final String description;
    private final boolean debitCreditIndicator; // true = credit, false = debit
    private final String businessDescription;
    
    /**
     * Constructor for TransactionType enum values.
     * 
     * @param code 2-character transaction type code matching COBOL TRAN-TYPE-CD field
     * @param description Human-readable description of the transaction type
     * @param debitCreditIndicator true for credit transactions, false for debit transactions
     * @param businessDescription Business context description for the transaction type
     */
    TransactionType(String code, String description, boolean debitCreditIndicator, String businessDescription) {
        this.code = code;
        this.description = description;
        this.debitCreditIndicator = debitCreditIndicator;
        this.businessDescription = businessDescription;
    }
    
    /**
     * Get the 2-character transaction type code.
     * 
     * @return transaction type code matching PostgreSQL TRANTYPE.transaction_type field
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description of the transaction type.
     * 
     * @return transaction type description matching PostgreSQL TRANTYPE.type_description field
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the debit/credit indicator for the transaction type.
     * 
     * This method preserves exact COBOL arithmetic behavior by providing the proper
     * sign indication for financial calculations using BigDecimal operations.
     * 
     * @return true for credit transactions (increase account balance), 
     *         false for debit transactions (decrease account balance)
     */
    public boolean getDebitCreditIndicator() {
        return debitCreditIndicator;
    }
    
    /**
     * Get the business context description for the transaction type.
     * 
     * @return detailed business description of the transaction type usage
     */
    public String getBusinessDescription() {
        return businessDescription;
    }
    
    /**
     * Check if this transaction type is a credit transaction.
     * 
     * @return true if this is a credit transaction, false otherwise
     */
    public boolean isCredit() {
        return debitCreditIndicator;
    }
    
    /**
     * Check if this transaction type is a debit transaction.
     * 
     * @return true if this is a debit transaction, false otherwise
     */
    public boolean isDebit() {
        return !debitCreditIndicator;
    }
    
    /**
     * Validate that this transaction type instance represents a valid transaction type.
     * 
     * This method provides comprehensive validation equivalent to COBOL business rules
     * for transaction type verification in transaction processing services.
     * 
     * @return true if the transaction type is valid for processing, false otherwise
     */
    public boolean isValid() {
        // All enum values are considered valid by definition
        // Additional business rule validation can be added here if needed
        return code != null && !code.trim().isEmpty() && 
               description != null && !description.trim().isEmpty();
    }
    
    /**
     * Create a TransactionType from a 2-character code with null-safe processing.
     * 
     * This method provides robust error handling for transaction type parsing
     * enabling graceful handling of invalid or missing transaction type codes
     * in REST API transaction processing validation.
     * 
     * @param code 2-character transaction type code (may be null or empty)
     * @return Optional containing the TransactionType if valid, empty if invalid
     */
    public static Optional<TransactionType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String trimmedCode = code.trim();
        
        // Validate code format (2 characters, numeric as per COBOL validation)
        if (trimmedCode.length() != 2) {
            return Optional.empty();
        }
        
        // Check if code is numeric (matching COBOL TTYPCDI validation in COTRN02C.cbl)
        if (!trimmedCode.matches("\\d{2}")) {
            return Optional.empty();
        }
        
        // Find matching enum value
        for (TransactionType type : TransactionType.values()) {
            if (type.getCode().equals(trimmedCode)) {
                return Optional.of(type);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Static validation method for transaction type codes.
     * 
     * This method provides fast validation for transaction type codes without
     * creating enum instances, optimized for high-volume transaction processing.
     * 
     * @param code 2-character transaction type code to validate
     * @return true if the code represents a valid transaction type, false otherwise
     */
    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
    
    /**
     * Get all available transaction type codes.
     * 
     * This method provides access to all valid transaction type codes for
     * validation purposes in transaction processing services and REST API endpoints.
     * 
     * @return array of all valid 2-character transaction type codes
     */
    public static String[] getValidCodes() {
        return java.util.Arrays.stream(TransactionType.values())
                .map(TransactionType::getCode)
                .toArray(String[]::new);
    }
    
    /**
     * Get transaction types by debit/credit indicator.
     * 
     * This method enables filtering transaction types by their debit/credit nature
     * for business logic that needs to handle debits and credits differently.
     * 
     * @param isCredit true to get credit transaction types, false for debit types
     * @return array of TransactionType values matching the debit/credit indicator
     */
    public static TransactionType[] getByDebitCreditIndicator(boolean isCredit) {
        return java.util.Arrays.stream(TransactionType.values())
                .filter(type -> type.getDebitCreditIndicator() == isCredit)
                .toArray(TransactionType[]::new);
    }
    
    /**
     * Get string representation of the transaction type.
     * 
     * @return formatted string with code and description
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", code, description, 
                           debitCreditIndicator ? "Credit" : "Debit");
    }
}