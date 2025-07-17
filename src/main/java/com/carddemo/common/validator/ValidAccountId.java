/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Jakarta Bean Validation annotation for account ID format validation.
 * Validates 11-digit numeric format as specified in COBOL account records.
 * 
 * This annotation enforces the same validation rules as the original COBOL
 * program COBIL00C.cbl where account IDs are validated for:
 * - Exactly 11 digits (PIC 9(11) from CVACT01Y.cpy)
 * - Non-empty values
 * - Numeric format only
 * 
 * Usage:
 * - @ValidAccountId on fields that require account ID validation
 * - @ValidAccountId(groups = ValidationGroup.class) for validation groups
 * - @ValidAccountId(message = "Custom message") for custom error messages
 */
package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for account ID format validation.
 * 
 * Validates that the annotated field contains a valid 11-digit numeric account ID
 * as defined in the COBOL account record structure (CVACT01Y.cpy).
 * 
 * This constraint ensures:
 * - Account ID is exactly 11 digits
 * - Account ID contains only numeric characters
 * - Account ID is not null or empty
 * - Account ID does not contain leading/trailing whitespace
 * 
 * The validation logic matches the COBOL program COBIL00C.cbl validation
 * patterns and error message formatting for consistency with the original
 * mainframe application behavior.
 */
@Documented
@Constraint(validatedBy = AccountIdValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccountId {
    
    /**
     * Default error message for invalid account ID format.
     * Matches the COBOL validation message pattern from COBIL00C.cbl.
     */
    String message() default "Acct ID must be exactly 11 digits";
    
    /**
     * Validation groups for conditional validation scenarios.
     * Allows different validation rules for different contexts such as:
     * - Account creation validation
     * - Account update validation  
     * - Bill payment validation
     * - Account inquiry validation
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for extensibility and additional metadata.
     * Can be used to carry additional constraint metadata
     * such as severity levels or validation context information.
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Flag to enable strict validation mode.
     * When true, performs additional validation checks beyond format:
     * - Validates account exists in database
     * - Checks account active status
     * - Verifies account access permissions
     * 
     * Default is false for format-only validation.
     */
    boolean strict() default false;
    
    /**
     * Context-specific validation mode for different business scenarios.
     * Allows customization of validation behavior based on the operation context:
     * - BILL_PAYMENT: Validates for bill payment operations
     * - ACCOUNT_INQUIRY: Validates for account view operations
     * - ACCOUNT_UPDATE: Validates for account modification operations
     * - TRANSACTION_PROCESSING: Validates for transaction operations
     */
    ValidationContext context() default ValidationContext.GENERAL;
    
    /**
     * Enumeration of validation contexts for different business scenarios.
     * Each context may have specific validation rules and error messages.
     */
    enum ValidationContext {
        /**
         * General account ID validation for basic format checking.
         */
        GENERAL,
        
        /**
         * Bill payment specific validation matching COBIL00C.cbl logic.
         * Includes checks for account existence and payment eligibility.
         */
        BILL_PAYMENT,
        
        /**
         * Account inquiry validation for view operations.
         * Focuses on read access permissions and account visibility.
         */
        ACCOUNT_INQUIRY,
        
        /**
         * Account update validation for modification operations.
         * Includes write access permissions and account modification rules.
         */
        ACCOUNT_UPDATE,
        
        /**
         * Transaction processing validation for financial operations.
         * Includes account status checks and transaction authorization.
         */
        TRANSACTION_PROCESSING
    }
}