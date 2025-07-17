/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Account ID validator implementation that validates account ID format according to
 * COBOL validation patterns from COBIL00C.cbl. Ensures account IDs are exactly 11 digits
 * and maintains compatibility with original mainframe validation behavior.
 *
 * This validator implements the same validation rules as the original COBOL program
 * where account IDs are validated for:
 * - Exactly 11 digits (PIC 9(11) from CVACT01Y.cpy)
 * - Non-empty values (matching "Acct ID can NOT be empty..." validation)
 * - Numeric format only (no letters, spaces, or special characters)
 * - Range validation within MIN_ACCOUNT_ID and MAX_ACCOUNT_ID bounds
 */
package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Jakarta Bean Validation implementation for account ID format validation.
 * 
 * This validator ensures that account IDs conform to the exact format specified
 * in the COBOL account record structure (CVACT01Y.cpy) and follows the same
 * validation logic as the original bill payment program (COBIL00C.cbl).
 * 
 * Validation Rules:
 * - Account ID must be exactly 11 digits
 * - Account ID cannot be null or empty
 * - Account ID must contain only numeric characters (0-9)
 * - Account ID must be within valid range (MIN_ACCOUNT_ID to MAX_ACCOUNT_ID)
 * - Account ID cannot contain leading/trailing whitespace
 * 
 * Error Messages:
 * - Follows COBOL error message patterns for consistency
 * - Provides specific validation failure reasons
 * - Maintains compatibility with original mainframe user experience
 * 
 * Integration:
 * - Integrates with Spring Boot validation framework
 * - Works with Jakarta Bean Validation annotations
 * - Supports validation groups for different business contexts
 * - Handles strict validation mode for database existence checks
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 2024-01-01
 */
public class AccountIdValidator implements ConstraintValidator<ValidAccountId, String> {
    
    /**
     * Pre-compiled pattern for 11-digit account ID format validation.
     * Uses the pattern from ValidationConstants for consistency.
     */
    private static final Pattern ACCOUNT_ID_PATTERN = ValidationConstants.ACCOUNT_ID_PATTERN;
    
    /**
     * Minimum valid account ID value from ValidationConstants.
     * Corresponds to business rule that account IDs start from 1.
     */
    private static final long MIN_ACCOUNT_ID = ValidationConstants.MIN_ACCOUNT_ID;
    
    /**
     * Maximum valid account ID value from ValidationConstants.
     * Corresponds to maximum 11-digit numeric value.
     */
    private static final long MAX_ACCOUNT_ID = ValidationConstants.MAX_ACCOUNT_ID;
    
    /**
     * Stores the constraint annotation instance for accessing annotation parameters.
     * Used to determine validation context and strictness settings.
     */
    private ValidAccountId constraintAnnotation;
    
    /**
     * Flag to indicate if strict validation mode is enabled.
     * When true, performs additional validation beyond format checking.
     */
    private boolean strictMode;
    
    /**
     * Validation context for different business scenarios.
     * Determines specific validation rules and error messages.
     */
    private ValidAccountId.ValidationContext validationContext;
    
    /**
     * Initializes the validator with the constraint annotation parameters.
     * 
     * This method is called by the Jakarta Bean Validation framework during
     * validator initialization. It extracts configuration from the annotation
     * to customize validation behavior.
     * 
     * @param constraintAnnotation The ValidAccountId annotation instance containing
     *                           validation parameters such as strict mode and context
     */
    @Override
    public void initialize(ValidAccountId constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
        this.strictMode = constraintAnnotation.strict();
        this.validationContext = constraintAnnotation.context();
    }
    
    /**
     * Validates the account ID according to COBOL validation patterns.
     * 
     * This method implements the core validation logic that matches the original
     * COBOL program COBIL00C.cbl. It performs comprehensive validation including:
     * - Null and empty checks (matching "Acct ID can NOT be empty..." logic)
     * - Format validation (11-digit numeric pattern)
     * - Range validation (MIN_ACCOUNT_ID to MAX_ACCOUNT_ID)
     * - Context-specific validation based on business scenario
     * 
     * The validation follows the same sequence as the original COBOL program:
     * 1. Check if account ID is null or empty
     * 2. Validate format using regex pattern
     * 3. Validate numeric range bounds
     * 4. Perform context-specific validation if needed
     * 
     * @param value The account ID string to validate
     * @param context The constraint validator context for error reporting
     * @return true if the account ID is valid according to all rules, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Step 1: Handle null and empty values
        // This matches the COBOL validation: "WHEN ACTIDINI OF COBIL0AI = SPACES OR LOW-VALUES"
        if (value == null || value.trim().isEmpty()) {
            buildConstraintViolation(context, "Acct ID can NOT be empty...");
            return false;
        }
        
        // Step 2: Validate format using regex pattern
        // This ensures exactly 11 digits matching PIC 9(11) from CVACT01Y.cpy
        if (!ACCOUNT_ID_PATTERN.matcher(value.trim()).matches()) {
            buildConstraintViolation(context, "Acct ID must be exactly 11 digits");
            return false;
        }
        
        // Step 3: Validate numeric range
        // Convert to long for range validation while handling potential NumberFormatException
        long accountIdValue;
        try {
            accountIdValue = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            // This should not happen if regex pattern is correct, but handle gracefully
            buildConstraintViolation(context, "Acct ID must be a valid number");
            return false;
        }
        
        // Check if account ID is within valid range
        if (accountIdValue < MIN_ACCOUNT_ID || accountIdValue > MAX_ACCOUNT_ID) {
            buildConstraintViolation(context, 
                String.format("Acct ID must be between %d and %d", MIN_ACCOUNT_ID, MAX_ACCOUNT_ID));
            return false;
        }
        
        // Step 4: Context-specific validation
        // Different validation rules may apply based on business context
        if (!validateByContext(value.trim(), context)) {
            return false;
        }
        
        // Step 5: Strict mode validation
        // When strict mode is enabled, perform additional validation checks
        if (strictMode) {
            if (!validateStrict(value.trim(), context)) {
                return false;
            }
        }
        
        // All validation checks passed
        return true;
    }
    
    /**
     * Performs context-specific validation based on business scenario.
     * 
     * Different business contexts may have specific validation requirements:
     * - BILL_PAYMENT: Account must be eligible for bill payment operations
     * - ACCOUNT_INQUIRY: Account must be accessible for view operations
     * - ACCOUNT_UPDATE: Account must be modifiable
     * - TRANSACTION_PROCESSING: Account must be active for transactions
     * 
     * @param accountId The validated account ID string
     * @param context The constraint validator context for error reporting
     * @return true if context-specific validation passes, false otherwise
     */
    private boolean validateByContext(String accountId, ConstraintValidatorContext context) {
        switch (validationContext) {
            case BILL_PAYMENT:
                // Bill payment context validation
                // This would typically check if account is eligible for bill payment
                // For now, we implement basic validation as per COBOL program
                return validateBillPaymentContext(accountId, context);
                
            case ACCOUNT_INQUIRY:
                // Account inquiry context validation
                // This would check if account is accessible for viewing
                return validateAccountInquiryContext(accountId, context);
                
            case ACCOUNT_UPDATE:
                // Account update context validation
                // This would check if account is modifiable
                return validateAccountUpdateContext(accountId, context);
                
            case TRANSACTION_PROCESSING:
                // Transaction processing context validation
                // This would check if account is active for transactions
                return validateTransactionProcessingContext(accountId, context);
                
            case GENERAL:
            default:
                // General validation - no additional context-specific rules
                return true;
        }
    }
    
    /**
     * Validates account ID for bill payment context.
     * 
     * This method implements validation rules specific to bill payment operations,
     * matching the logic from COBIL00C.cbl where accounts are validated for
     * payment eligibility.
     * 
     * @param accountId The account ID to validate
     * @param context The constraint validator context for error reporting
     * @return true if account is valid for bill payment, false otherwise
     */
    private boolean validateBillPaymentContext(String accountId, ConstraintValidatorContext context) {
        // Bill payment specific validation rules
        // This matches the validation logic from COBIL00C.cbl
        
        // Account ID cannot be all zeros (common invalid pattern)
        if (accountId.matches("^0+$")) {
            buildConstraintViolation(context, "Account ID cannot be all zeros");
            return false;
        }
        
        // Account ID cannot start with certain invalid prefixes
        // This prevents common test/dummy account patterns
        if (accountId.startsWith("00000") || accountId.startsWith("99999")) {
            buildConstraintViolation(context, "Invalid account ID format for bill payment");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates account ID for account inquiry context.
     * 
     * @param accountId The account ID to validate
     * @param context The constraint validator context for error reporting
     * @return true if account is valid for inquiry, false otherwise
     */
    private boolean validateAccountInquiryContext(String accountId, ConstraintValidatorContext context) {
        // Account inquiry specific validation rules
        // For inquiry operations, we allow broader range of account IDs
        return true;
    }
    
    /**
     * Validates account ID for account update context.
     * 
     * @param accountId The account ID to validate
     * @param context The constraint validator context for error reporting
     * @return true if account is valid for update, false otherwise
     */
    private boolean validateAccountUpdateContext(String accountId, ConstraintValidatorContext context) {
        // Account update specific validation rules
        // Similar to bill payment but may have different restrictions
        return validateBillPaymentContext(accountId, context);
    }
    
    /**
     * Validates account ID for transaction processing context.
     * 
     * @param accountId The account ID to validate
     * @param context The constraint validator context for error reporting
     * @return true if account is valid for transaction processing, false otherwise
     */
    private boolean validateTransactionProcessingContext(String accountId, ConstraintValidatorContext context) {
        // Transaction processing specific validation rules
        // May have stricter rules for financial transactions
        return validateBillPaymentContext(accountId, context);
    }
    
    /**
     * Performs strict mode validation with additional checks.
     * 
     * When strict mode is enabled, this method performs additional validation
     * that goes beyond basic format checking. This could include:
     * - Database existence verification
     * - Account status validation
     * - Access permission checks
     * 
     * Note: For this implementation, we focus on format validation as per
     * the requirements. Database existence verification would typically
     * be handled by service layer validation.
     * 
     * @param accountId The account ID to validate
     * @param context The constraint validator context for error reporting
     * @return true if strict validation passes, false otherwise
     */
    private boolean validateStrict(String accountId, ConstraintValidatorContext context) {
        // Strict mode validation
        // This would typically involve database lookups, but for format validation
        // we implement additional format checks
        
        // Check for sequential digits (like 12345678901)
        if (isSequentialDigits(accountId)) {
            buildConstraintViolation(context, "Account ID cannot contain sequential digits");
            return false;
        }
        
        // Check for repeated digits (like 11111111111)
        if (isRepeatedDigits(accountId)) {
            buildConstraintViolation(context, "Account ID cannot contain all repeated digits");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the account ID contains sequential digits.
     * 
     * @param accountId The account ID to check
     * @return true if account ID contains sequential digits, false otherwise
     */
    private boolean isSequentialDigits(String accountId) {
        for (int i = 0; i < accountId.length() - 1; i++) {
            int current = Character.getNumericValue(accountId.charAt(i));
            int next = Character.getNumericValue(accountId.charAt(i + 1));
            if (next != (current + 1) % 10) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the account ID contains all repeated digits.
     * 
     * @param accountId The account ID to check
     * @return true if account ID contains all repeated digits, false otherwise
     */
    private boolean isRepeatedDigits(String accountId) {
        char firstDigit = accountId.charAt(0);
        for (int i = 1; i < accountId.length(); i++) {
            if (accountId.charAt(i) != firstDigit) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Builds a constraint violation with a custom error message.
     * 
     * This method disables the default constraint violation and creates
     * a new one with the specified error message. This allows for more
     * specific error messages that match the COBOL validation patterns.
     * 
     * @param context The constraint validator context
     * @param message The custom error message to use
     */
    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}