package com.carddemo.common.enums;

import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.util.ValidationUtils;
import jakarta.validation.Valid;
import java.util.Optional;

/**
 * TransactionCategory enumeration defining transaction categories converted from COBOL TRAN-CAT-CD field
 * for transaction categorization, balance management, and Spring Batch processing.
 * 
 * This enum maintains exact 4-digit numeric format validation patterns from the original COBOL implementation
 * while supporting modern Spring Boot transaction processing and PostgreSQL TRANCATG reference table integration.
 * 
 * Category codes must preserve original 4-digit numeric format validation patterns per Section 0.2.1 
 * microservices architecture transformation requirements.
 */
public enum TransactionCategory {
    
    // Standard transaction categories based on typical credit card operations
    // These align with TRANCATG reference table entries
    
    /**
     * Purchase transactions - general merchandise and services
     */
    PURCHASE("0001", "Purchase Transaction", true),
    
    /**
     * Cash advance transactions from ATM or counter
     */
    CASH_ADVANCE("0002", "Cash Advance", true),
    
    /**
     * Payment transactions - customer payments toward balance
     */
    PAYMENT("0003", "Payment Transaction", true),
    
    /**
     * Fee transactions - annual fees, late fees, overlimit fees
     */
    FEE("0004", "Fee Transaction", true),
    
    /**
     * Interest transactions - finance charges and interest calculations
     * This category is used by the batch interest calculation job (CBACT04C.cbl equivalent)
     */
    INTEREST("0005", "Interest Charge", true),
    
    /**
     * Credit adjustment transactions - returns, credits, adjustments
     */
    CREDIT_ADJUSTMENT("0006", "Credit Adjustment", true),
    
    /**
     * Debit adjustment transactions - corrections, chargebacks
     */
    DEBIT_ADJUSTMENT("0007", "Debit Adjustment", true),
    
    /**
     * Transfer transactions - balance transfers between accounts
     */
    TRANSFER("0008", "Transfer Transaction", true),
    
    /**
     * Promotional transactions - special promotional categories
     */
    PROMOTIONAL("0009", "Promotional Transaction", true),
    
    /**
     * Dispute transactions - disputed charges and related adjustments
     */
    DISPUTE("0010", "Dispute Transaction", true),
    
    /**
     * Unknown/unclassified transactions - fallback category
     */
    UNKNOWN("9999", "Unknown Transaction", false);
    
    private final String code;
    private final String description;
    private final boolean active;
    
    /**
     * Constructor for TransactionCategory enum values
     * 
     * @param code 4-digit numeric category code matching COBOL PIC 9(04) format
     * @param description Human-readable description of the transaction category
     * @param active Whether this category is currently active for new transactions
     */
    TransactionCategory(String code, String description, boolean active) {
        this.code = code;
        this.description = description;
        this.active = active;
    }
    
    /**
     * Get the 4-digit category code
     * 
     * @return String representation of the 4-digit category code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the category description
     * 
     * @return Human-readable description of the transaction category
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this category is active for new transactions
     * 
     * @return true if category is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Create TransactionCategory from 4-digit category code with validation
     * Supports Spring Batch processing for transaction categorization operations
     * 
     * @param code 4-digit numeric category code string
     * @return Optional containing the matching TransactionCategory, or empty if invalid
     */
    public static Optional<TransactionCategory> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Validate the code format first
        ValidationResult validation = validateCode(code.trim());
        if (!validation.isValid()) {
            return Optional.empty();
        }
        
        // Find matching enum value
        for (TransactionCategory category : values()) {
            if (category.getCode().equals(code.trim())) {
                return Optional.of(category);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate if the category is valid for balance calculations and transaction processing
     * Category validation must maintain exact precision for balance calculations using BigDecimal arithmetic
     * 
     * @return true if this category is valid for transaction processing
     */
    public boolean isValid() {
        return this != UNKNOWN && active;
    }
    
    /**
     * Validate if a given code represents a valid transaction category code
     * Maintains original COBOL PICTURE clause validation behavior equivalent to PIC 9(04) validation
     * 
     * @param code Category code to validate
     * @return true if code is valid format, false otherwise
     */
    public static boolean isValidCode(String code) {
        return validateCode(code).isValid();
    }
    
    /**
     * Comprehensive validation of transaction category code format and content
     * Implements validation methods for transaction categorization matching original COBOL batch processing logic
     * 
     * @param code Category code to validate
     * @return ValidationResult containing validation status and error details
     */
    public static ValidationResult validateCode(String code) {
        // Check for null or empty code
        ValidationResult requiredCheck = ValidationUtils.validateRequiredField(code, "Transaction Category Code");
        if (!requiredCheck.isValid()) {
            return requiredCheck;
        }
        
        String trimmedCode = code.trim();
        
        // Validate numeric field format - must be exactly 4 digits
        ValidationResult numericCheck = ValidationUtils.validateNumericField(trimmedCode, 4);
        if (!numericCheck.isValid()) {
            return numericCheck;
        }
        
        // Ensure exactly 4 digits (not just maximum 4)
        if (trimmedCode.length() != 4) {
            return ValidationResult.INVALID_LENGTH;
        }
        
        // Additional range validation - codes should be within reasonable bounds
        try {
            int codeValue = Integer.parseInt(trimmedCode);
            if (codeValue < 0 || codeValue > 9999) {
                return ValidationResult.INVALID_RANGE;
            }
        } catch (NumberFormatException e) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Get all valid transaction categories for Spring Batch processing
     * Enum must support Spring Batch processing for transaction categorization and balance updates
     * 
     * @return Array of all valid TransactionCategory values (excluding UNKNOWN)
     */
    public static TransactionCategory[] getValidCategories() {
        return java.util.Arrays.stream(values())
            .filter(TransactionCategory::isValid)
            .toArray(TransactionCategory[]::new);
    }
    
    /**
     * Get all active transaction categories for balance management
     * Category codes must preserve original 4-digit numeric format validation patterns
     * 
     * @return Array of active TransactionCategory values
     */
    public static TransactionCategory[] getActiveCategories() {
        return java.util.Arrays.stream(values())
            .filter(category -> category.active)
            .toArray(TransactionCategory[]::new);
    }
    
    /**
     * Check if this category supports balance calculations
     * Required for PostgreSQL TRANCATG reference table integration and balance management
     * 
     * @return true if category supports balance tracking
     */
    public boolean supportsBalanceCalculations() {
        return this.active && this != UNKNOWN;
    }
    
    /**
     * Get the transaction category for interest calculations
     * Used by Spring Batch interest calculation jobs matching original COBOL batch processing logic
     * 
     * @return TransactionCategory for interest charges
     */
    public static TransactionCategory getInterestCategory() {
        return INTEREST;
    }
    
    /**
     * String representation of the transaction category
     * 
     * @return Formatted string with code and description
     */
    @Override
    public String toString() {
        return String.format("%s - %s", code, description);
    }
}