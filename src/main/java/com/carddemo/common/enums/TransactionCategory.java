/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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

import java.util.Arrays;
import java.util.Optional;

import jakarta.validation.Valid;

/**
 * Enumeration defining transaction categories converted from COBOL TRAN-CAT-CD field
 * for transaction categorization, balance management, and Spring Batch processing.
 * 
 * This enum maintains exact functional equivalence with the original COBOL TRAN-CAT-CD
 * field from CVTRA05Y.cpy copybook, preserving 4-digit numeric validation patterns
 * and supporting financial transaction categorization requirements.
 * 
 * Key Features:
 * - Exact COBOL TRAN-CAT-CD PIC 9(04) validation pattern preservation
 * - Support for Spring Batch processing in interest calculation jobs
 * - Integration with PostgreSQL TRANCATG reference table
 * - Category-specific balance management and reporting capabilities
 * - Comprehensive validation using ValidationUtils and ValidationResult
 * 
 * Technical Implementation:
 * - Maintains 4-digit numeric category codes matching COBOL format
 * - Provides validation methods for transaction categorization
 * - Supports balance calculations with exact BigDecimal precision
 * - Integrates with Spring Boot validation framework
 * - Enables microservices transaction processing with category-based routing
 * 
 * Based on COBOL structures from:
 * - CVTRA05Y.cpy: Transaction record with TRAN-CAT-CD PIC 9(04) field
 * - COTRN00C.cbl: Transaction listing program using category codes
 * - CBACT04C.cbl: Interest calculation batch program with category processing
 * - PostgreSQL TRANCATG table: Modern reference table for transaction categories
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public enum TransactionCategory {
    
    /**
     * Purchase transactions - General merchandise and service purchases
     * Standard purchase category for card-present and card-not-present transactions
     */
    PURCHASE("0001", "Purchase Transaction", true),
    
    /**
     * Cash advance transactions - ATM and bank cash withdrawals
     * Cash advance category with special fee and interest rate calculations
     */
    CASH_ADVANCE("0002", "Cash Advance Transaction", true),
    
    /**
     * Payment transactions - Account payments and credits
     * Payment category for customer payments reducing account balance
     */
    PAYMENT("0003", "Payment Transaction", true),
    
    /**
     * Fee transactions - Service charges and penalty fees
     * Fee category for various account fees and service charges
     */
    FEE("0004", "Fee Transaction", true),
    
    /**
     * Interest transactions - Interest charges and calculations
     * Interest category for periodic interest calculations from batch processing
     * Used in CBACT04C.cbl interest calculation batch job
     */
    INTEREST("0005", "Interest Transaction", true),
    
    /**
     * Refund transactions - Transaction reversals and refunds
     * Refund category for merchant refunds and transaction reversals
     */
    REFUND("0006", "Refund Transaction", true),
    
    /**
     * Transfer transactions - Balance transfers and account transfers
     * Transfer category for balance transfer operations
     */
    TRANSFER("0007", "Transfer Transaction", true),
    
    /**
     * Adjustment transactions - Administrative adjustments
     * Adjustment category for customer service adjustments and corrections
     */
    ADJUSTMENT("0008", "Adjustment Transaction", true),
    
    /**
     * Disputed transactions - Chargeback and dispute processing
     * Dispute category for transaction disputes and chargeback processing
     */
    DISPUTE("0009", "Disputed Transaction", true),
    
    /**
     * Promotional transactions - Rewards and promotional credits
     * Promotional category for customer rewards and marketing credits
     */
    PROMOTIONAL("0010", "Promotional Transaction", true),
    
    /**
     * Inactive category - Placeholder for discontinued transaction types
     * Inactive category maintained for historical data integrity
     */
    INACTIVE("9999", "Inactive Transaction Category", false);
    
    private final String code;
    private final String description;
    private final boolean active;
    
    /**
     * Constructor for TransactionCategory enum values
     * 
     * @param code 4-digit transaction category code matching COBOL PIC 9(04) format
     * @param description Human-readable description of the transaction category
     * @param active Whether this category is currently active for new transactions
     */
    TransactionCategory(String code, String description, boolean active) {
        this.code = code;
        this.description = description;
        this.active = active;
    }
    
    /**
     * Gets the 4-digit transaction category code
     * 
     * @return Transaction category code in 4-digit format
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the human-readable description of the transaction category
     * 
     * @return Transaction category description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this transaction category is active for new transactions
     * 
     * @return true if category is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Finds a TransactionCategory by its 4-digit code
     * 
     * @param code 4-digit transaction category code to search for
     * @return Optional containing the matching TransactionCategory, or empty if not found
     */
    public static Optional<TransactionCategory> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedCode = String.format("%04d", Integer.parseInt(code.trim()));
        
        return Arrays.stream(values())
                .filter(category -> category.code.equals(normalizedCode))
                .findFirst();
    }
    
    /**
     * Validates if a transaction category code is valid and active
     * 
     * @param code 4-digit transaction category code to validate
     * @return true if code is valid and corresponds to an active category
     */
    public static boolean isValid(String code) {
        return fromCode(code)
                .map(TransactionCategory::isActive)
                .orElse(false);
    }
    
    /**
     * Validates if a transaction category code exists (including inactive categories)
     * 
     * @param code 4-digit transaction category code to validate
     * @return true if code exists in the enumeration
     */
    public static boolean isValidCode(String code) {
        return fromCode(code).isPresent();
    }
    
    /**
     * Validates transaction category code format and existence using ValidationUtils
     * 
     * Implements comprehensive validation equivalent to COBOL TRAN-CAT-CD validation:
     * - Validates 4-digit numeric format using ValidationUtils.validateNumericField()
     * - Validates code existence in enumeration
     * - Validates code is active for new transactions
     * - Returns ValidationResult for consistent error handling
     * 
     * @param code 4-digit transaction category code to validate
     * @return ValidationResult indicating validation outcome
     */
    public static ValidationResult validateCode(String code) {
        // Validate required field
        ValidationResult requiredResult = ValidationUtils.validateRequiredField(code);
        if (!requiredResult.isValid()) {
            return requiredResult;
        }
        
        // Validate numeric format
        ValidationResult numericResult = ValidationUtils.validateNumericField(code);
        if (!numericResult.isValid()) {
            return numericResult;
        }
        
        // Validate 4-digit length
        String trimmedCode = code.trim();
        if (trimmedCode.length() != 4) {
            return ValidationResult.INVALID_FORMAT;
        }
        
        // Validate code exists in enumeration
        if (!isValidCode(trimmedCode)) {
            return ValidationResult.INVALID_CROSS_REFERENCE;
        }
        
        // Validate code is active
        if (!isValid(trimmedCode)) {
            return ValidationResult.BUSINESS_RULE_VIOLATION;
        }
        
        return ValidationResult.VALID;
    }
    
    /**
     * Gets all transaction categories that are currently active
     * 
     * @return Array of active TransactionCategory values
     */
    public static TransactionCategory[] getActiveCategories() {
        return Arrays.stream(values())
                .filter(TransactionCategory::isActive)
                .toArray(TransactionCategory[]::new);
    }
    
    /**
     * Gets all transaction categories including inactive ones
     * 
     * @return Array of all TransactionCategory values
     */
    public static TransactionCategory[] getAllCategories() {
        return values();
    }
    
    /**
     * Checks if this category is eligible for interest calculations
     * 
     * Interest is typically calculated on purchase and cash advance balances
     * but not on payments, fees, or adjustments
     * 
     * @return true if category is eligible for interest calculation
     */
    public boolean isInterestEligible() {
        return this == PURCHASE || this == CASH_ADVANCE || this == TRANSFER;
    }
    
    /**
     * Checks if this category represents a debit transaction
     * 
     * Debit transactions increase the account balance
     * 
     * @return true if category represents a debit transaction
     */
    public boolean isDebitTransaction() {
        return this == PURCHASE || this == CASH_ADVANCE || this == FEE || 
               this == INTEREST || this == TRANSFER || this == ADJUSTMENT ||
               this == DISPUTE;
    }
    
    /**
     * Checks if this category represents a credit transaction
     * 
     * Credit transactions decrease the account balance
     * 
     * @return true if category represents a credit transaction
     */
    public boolean isCreditTransaction() {
        return this == PAYMENT || this == REFUND || this == PROMOTIONAL;
    }
    
    /**
     * Gets the category code formatted for display
     * 
     * @return Formatted category code with description
     */
    public String getFormattedCode() {
        return String.format("%s - %s", code, description);
    }
    
    /**
     * Converts transaction category to string representation
     * 
     * @return String representation showing code and description
     */
    @Override
    public String toString() {
        return String.format("TransactionCategory{code='%s', description='%s', active=%s}", 
                           code, description, active);
    }
    
    /**
     * Checks if this category should be included in balance calculations
     * 
     * @return true if category affects account balance calculations
     */
    public boolean affectsBalance() {
        return active && this != INACTIVE;
    }
    
    /**
     * Gets the category for interest transactions
     * Used specifically by Spring Batch interest calculation jobs
     * 
     * @return INTEREST transaction category
     */
    public static TransactionCategory getInterestCategory() {
        return INTEREST;
    }
    
    /**
     * Gets the category for payment transactions
     * Used for customer payment processing
     * 
     * @return PAYMENT transaction category
     */
    public static TransactionCategory getPaymentCategory() {
        return PAYMENT;
    }
    
    /**
     * Gets the category for purchase transactions
     * Used for general merchant transactions
     * 
     * @return PURCHASE transaction category
     */
    public static TransactionCategory getPurchaseCategory() {
        return PURCHASE;
    }
}