/*
 * ValidPhoneNumber.java
 * 
 * Jakarta Bean Validation annotation for phone number validation with area code verification.
 * This annotation validates North American phone numbers using the area code lookup table
 * from COBOL copybook CSLKPCDY.cpy to ensure phone numbers have valid area codes and proper formatting.
 * 
 * Implements North American Numbering Plan (NANP) validation with comprehensive error reporting
 * to maintain exact functional equivalence with the original COBOL validation logic.
 * 
 * Part of CardDemo mainframe modernization - COBOL to Java transformation.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
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
 * Jakarta Bean Validation annotation for North American phone number validation.
 * 
 * This annotation validates phone numbers according to the North American Numbering Plan (NANP)
 * using the area code lookup table from COBOL copybook CSLKPCDY.cpy. The validation ensures
 * that phone numbers have valid area codes and proper formatting.
 * 
 * <p>Supported phone number formats:
 * <ul>
 *   <li>(###) ###-####</li>
 *   <li>###-###-####</li>
 *   <li>###.###.####</li>
 *   <li>### ### ####</li>
 *   <li>##########</li>
 *   <li>+1##########</li>
 * </ul>
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>Area code must be a valid North American area code from CSLKPCDY.cpy</li>
 *   <li>Exchange code (first 3 digits after area code) cannot start with 0 or 1</li>
 *   <li>Exchange code cannot be N11 (where N is 2-9)</li>
 *   <li>Number must be exactly 10 digits (excluding formatting characters)</li>
 *   <li>Area code cannot be easily recognizable codes (555, 800, 888, etc.)</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * public class CustomerDTO {
 *     {@literal @}ValidPhoneNumber(message = "Invalid phone number format or area code")
 *     private String phoneNumber;
 * }
 * </pre>
 * 
 * @see PhoneNumberValidator
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    
    /**
     * The error message to display when validation fails.
     * 
     * @return the error message
     */
    String message() default "Invalid phone number format or area code not recognized";
    
    /**
     * The validation groups to which this constraint belongs.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * The payload associated with this constraint.
     * Can be used to carry additional metadata about the constraint.
     * 
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Indicates whether to allow null values.
     * When set to true, null values will pass validation.
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default true;
    
    /**
     * Indicates whether to allow empty strings.
     * When set to true, empty strings will pass validation.
     * 
     * @return true if empty strings are allowed, false otherwise
     */
    boolean allowEmpty() default true;
    
    /**
     * Indicates whether to allow easily recognizable area codes.
     * When set to false, area codes like 555, 800, 888, etc. will be rejected.
     * 
     * @return true if easily recognizable area codes are allowed, false otherwise
     */
    boolean allowEasyRecognizableAreaCodes() default false;
    
    /**
     * Indicates whether to perform strict formatting validation.
     * When set to true, only standard formats will be accepted.
     * When set to false, more flexible parsing will be applied.
     * 
     * @return true for strict formatting, false for flexible parsing
     */
    boolean strictFormatting() default false;
    
    /**
     * The maximum length for the phone number string (including formatting characters).
     * This corresponds to the COBOL CUST-PHONE-NUM-1 and CUST-PHONE-NUM-2 fields
     * which are PIC X(15).
     * 
     * @return the maximum length allowed
     */
    int maxLength() default 15;
    
    /**
     * Indicates whether to validate against general purpose area codes only.
     * When set to true, only area codes from the VALID-GENERAL-PURP-CODE list
     * in CSLKPCDY.cpy will be accepted.
     * 
     * @return true to validate against general purpose codes only, false otherwise
     */
    boolean generalPurposeOnly() default false;
    
    /**
     * Custom context for providing additional error information.
     * This allows for more detailed error messages indicating specific validation failures.
     * 
     * @return the context
     */
    String context() default "";
}