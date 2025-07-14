/*
 * Copyright 2024 CardDemo Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.common.enums;

import com.carddemo.common.util.ValidationUtils;

import jakarta.validation.Valid;

import java.util.Optional;

/**
 * Enumeration defining transaction categories for financial transaction processing.
 * 
 * <p>This enum provides transaction categorization functionality equivalent to the original
 * COBOL TRAN-CAT-CD field validation and processing. It supports 4-digit category codes
 * for transaction classification, balance management, and Spring Batch processing in the
 * CardDemo microservices architecture.</p>
 * 
 * <p>The transaction categories are used throughout the system for:</p>
 * <ul>
 *   <li>Transaction classification and validation in transaction processing services</li>
 *   <li>Balance calculations and category-specific balance management</li>
 *   <li>Interest calculation jobs and batch processing operations</li>
 *   <li>PostgreSQL TRANCATG reference table integration</li>
 *   <li>Spring Batch transaction categorization for balance updates</li>
 * </ul>
 * 
 * <p>COBOL Transformation Details:</p>
 * <ul>
 *   <li>Original COBOL field: TRAN-CAT-CD PIC 9(04)</li>
 *   <li>Validation maintains exact 4-digit numeric format requirements</li>
 *   <li>Category codes preserve original COBOL validation patterns</li>
 *   <li>BigDecimal arithmetic precision maintained for balance calculations</li>
 * </ul>
 * 
 * <p>Performance Requirements:</p>
 * <ul>
 *   <li>Supports 10,000+ TPS transaction processing with category validation</li>
 *   <li>Optimized for Spring Batch balance calculation jobs within 4-hour window</li>
 *   <li>Integration with PostgreSQL TRANCATG reference table for category lookups</li>
 * </ul>
 * 
 * @author Blitzy Platform  
 * @version 1.0
 * @since Java 21
 */
public enum TransactionCategory {
    
    /**
     * General purchases and retail transactions.
     * Used for standard retail purchases and merchandise transactions.
     */
    GENERAL_PURCHASES("0001", "General Purchases", true),
    
    /**
     * Cash advances and cash-equivalent transactions.
     * Used for ATM cash withdrawals and cash advance transactions.
     */
    CASH_ADVANCES("0002", "Cash Advances", true),
    
    /**
     * Balance transfers between accounts.
     * Used for balance transfer transactions and account-to-account transfers.
     */
    BALANCE_TRANSFERS("0003", "Balance Transfers", true),
    
    /**
     * Interest charges and fee assessments.
     * Used for interest calculations and various fee assessments.
     */
    INTEREST_CHARGES("0004", "Interest Charges", true),
    
    /**
     * Payment transactions and credits.
     * Used for payment processing and credit postings to accounts.
     */
    PAYMENTS("0005", "Payments", true),
    
    /**
     * Fee transactions and service charges.
     * Used for various fees including annual fees, late fees, and service charges.
     */
    FEES("0006", "Fees", true),
    
    /**
     * Refunds and credit adjustments.
     * Used for transaction refunds and positive balance adjustments.
     */
    REFUNDS("0007", "Refunds", true),
    
    /**
     * Disputed transactions and chargebacks.
     * Used for disputed charge processing and chargeback management.
     */
    DISPUTES("0008", "Disputes", true),
    
    /**
     * Rewards and cashback transactions.
     * Used for reward redemptions and cashback credit postings.
     */
    REWARDS("0009", "Rewards", true),
    
    /**
     * Promotional transactions and special offers.
     * Used for promotional balance transfers and special offer transactions.
     */
    PROMOTIONAL("0010", "Promotional", true),
    
    /**
     * Foreign transaction processing.
     * Used for international transactions and foreign currency processing.
     */
    FOREIGN_TRANSACTIONS("0011", "Foreign Transactions", true),
    
    /**
     * Online and digital transactions.
     * Used for e-commerce and digital payment processing.
     */
    DIGITAL_TRANSACTIONS("0012", "Digital Transactions", true),
    
    /**
     * Recurring payment transactions.
     * Used for subscription payments and recurring billing.
     */
    RECURRING_PAYMENTS("0013", "Recurring Payments", true),
    
    /**
     * Adjustment transactions for corrections.
     * Used for transaction corrections and balance adjustments.
     */
    ADJUSTMENTS("0014", "Adjustments", true),
    
    /**
     * Reversal transactions for corrections.
     * Used for transaction reversals and voided transactions.
     */
    REVERSALS("0015", "Reversals", true),
    
    /**
     * Test transactions for system validation.
     * Used for system testing and validation transactions (inactive by default).
     */
    TEST_TRANSACTIONS("9999", "Test Transactions", false);
    
    // Enum instance fields
    private final String code;
    private final String description;
    private final boolean active;
    
    /**
     * Constructor for TransactionCategory enum instances.
     * 
     * @param code The 4-digit transaction category code
     * @param description The human-readable description of the category
     * @param active Whether the category is currently active for processing
     */
    TransactionCategory(String code, String description, boolean active) {
        this.code = code;
        this.description = description;
        this.active = active;
    }
    
    /**
     * Gets the 4-digit transaction category code.
     * 
     * @return The category code as a 4-digit string
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the human-readable description of the transaction category.
     * 
     * @return The category description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the transaction category is currently active.
     * 
     * @return true if the category is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Finds a TransactionCategory by its 4-digit code.
     * 
     * <p>This method provides case-insensitive lookup of transaction categories
     * by their 4-digit code, supporting both padded and unpadded input formats.</p>
     * 
     * @param code The 4-digit transaction category code to look up
     * @return Optional containing the matching TransactionCategory, or empty if not found
     */
    public static Optional<TransactionCategory> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Sanitize and pad the input code to 4 digits
        String cleanCode = ValidationUtils.sanitizeInput(code);
        if (cleanCode.length() < 4) {
            cleanCode = String.format("%04d", Integer.parseInt(cleanCode));
        }
        
        for (TransactionCategory category : values()) {
            if (category.code.equals(cleanCode)) {
                return Optional.of(category);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validates if a transaction category code is valid and properly formatted.
     * 
     * <p>This method replicates COBOL validation patterns for the TRAN-CAT-CD field,
     * ensuring the code is exactly 4 digits and matches a valid category.</p>
     * 
     * @param code The transaction category code to validate
     * @return true if the code is valid, false otherwise
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        // Use ValidationUtils to validate the numeric format
        ValidationResult formatResult = ValidationUtils.validateNumericField(code, 4);
        if (!formatResult.isValid()) {
            return false;
        }
        
        // Check if the code exists in our enum
        return fromCode(code).isPresent();
    }
    
    /**
     * Validates a transaction category code and returns detailed validation results.
     * 
     * <p>This method provides comprehensive validation feedback for transaction category
     * codes, supporting integration with Spring Boot validation framework and REST API
     * error handling.</p>
     * 
     * @param code The transaction category code to validate
     * @return ValidationResult indicating the validation outcome
     */
    public static ValidationResult validateCode(String code) {
        // Check for required field
        ValidationResult requiredResult = ValidationUtils.validateRequiredField(code, "Transaction Category Code");
        if (!requiredResult.isValid()) {
            return requiredResult;
        }
        
        // Validate numeric format and length
        ValidationResult formatResult = ValidationUtils.validateNumericField(code, 4);
        if (!formatResult.isValid()) {
            return formatResult;
        }
        
        // Check if code exists in enum
        Optional<TransactionCategory> category = fromCode(code);
        if (category.isEmpty()) {
            return ValidationResult.INVALID_RANGE;
        }
        
        // Check if category is active
        if (!category.get().isActive()) {
            return ValidationResult.INVALID_RANGE;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Checks if this transaction category is valid for processing.
     * 
     * <p>This method supports business logic validation ensuring only active
     * categories are used for transaction processing and balance calculations.</p>
     * 
     * @return true if the category is valid for processing, false otherwise
     */
    public boolean isValid() {
        return active;
    }
    
    /**
     * Gets all active transaction categories.
     * 
     * <p>This method provides a filtered list of only active categories for
     * use in transaction processing and user interface selection lists.</p>
     * 
     * @return Array of active TransactionCategory values
     */
    public static TransactionCategory[] getActiveCategories() {
        return java.util.Arrays.stream(values())
                .filter(TransactionCategory::isActive)
                .toArray(TransactionCategory[]::new);
    }
    
    /**
     * Checks if this category is used for balance calculations.
     * 
     * <p>This method supports Spring Batch processing and balance management
     * operations by identifying categories that affect account balances.</p>
     * 
     * @return true if the category affects balance calculations, false otherwise
     */
    public boolean isBalanceAffecting() {
        return switch (this) {
            case GENERAL_PURCHASES, CASH_ADVANCES, BALANCE_TRANSFERS, 
                 INTEREST_CHARGES, PAYMENTS, FEES, REFUNDS, 
                 FOREIGN_TRANSACTIONS, DIGITAL_TRANSACTIONS, 
                 RECURRING_PAYMENTS, ADJUSTMENTS, REVERSALS -> true;
            case DISPUTES, REWARDS, PROMOTIONAL, TEST_TRANSACTIONS -> false;
        };
    }
    
    /**
     * Checks if this category represents a debit transaction.
     * 
     * <p>This method supports transaction processing logic and balance
     * calculations by identifying debit vs credit transaction categories.</p>
     * 
     * @return true if the category represents a debit transaction, false otherwise
     */
    public boolean isDebitTransaction() {
        return switch (this) {
            case GENERAL_PURCHASES, CASH_ADVANCES, BALANCE_TRANSFERS, 
                 INTEREST_CHARGES, FEES, FOREIGN_TRANSACTIONS, 
                 DIGITAL_TRANSACTIONS, RECURRING_PAYMENTS -> true;
            case PAYMENTS, REFUNDS, REWARDS, PROMOTIONAL, 
                 ADJUSTMENTS, REVERSALS, DISPUTES, TEST_TRANSACTIONS -> false;
        };
    }
    
    /**
     * Checks if this category supports international transactions.
     * 
     * <p>This method supports transaction processing logic for international
     * and foreign currency transactions requiring special handling.</p>
     * 
     * @return true if the category supports international transactions, false otherwise
     */
    public boolean isInternationalSupported() {
        return switch (this) {
            case GENERAL_PURCHASES, CASH_ADVANCES, FOREIGN_TRANSACTIONS, 
                 DIGITAL_TRANSACTIONS, PAYMENTS, REFUNDS -> true;
            case BALANCE_TRANSFERS, INTEREST_CHARGES, FEES, REWARDS, 
                 PROMOTIONAL, RECURRING_PAYMENTS, ADJUSTMENTS, 
                 REVERSALS, DISPUTES, TEST_TRANSACTIONS -> false;
        };
    }
    
    /**
     * Gets the display name for user interfaces.
     * 
     * <p>This method provides a formatted display name suitable for use in
     * React components and user interface elements.</p>
     * 
     * @return The formatted display name
     */
    public String getDisplayName() {
        return String.format("%s - %s", code, description);
    }
    
    /**
     * Returns a string representation of the TransactionCategory.
     * 
     * @return String representation including code and description
     */
    @Override
    public String toString() {
        return String.format("TransactionCategory{code='%s', description='%s', active=%s}", 
                code, description, active);
    }
}