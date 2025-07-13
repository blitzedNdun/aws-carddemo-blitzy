/*
 * ValidPhoneNumber.java
 * 
 * Jakarta Bean Validation annotation for North American phone number validation
 * with area code verification using NANP (North American Numbering Plan) area codes.
 * 
 * This annotation validates phone numbers against the area code lookup table
 * from COBOL copybook CSLKPCDY.cpy, ensuring compliance with the North American
 * Numbering Plan while supporting multiple phone number formats.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Jakarta Bean Validation annotation for validating North American phone numbers
 * with area code verification using the NANP (North American Numbering Plan) area codes.
 * 
 * This annotation validates phone numbers against the area code lookup table
 * from COBOL copybook CSLKPCDY.cpy (VALID-PHONE-AREA-CODE condition), ensuring
 * that only valid area codes are accepted for phone number validation.
 * 
 * <p>Supported Phone Number Formats:</p>
 * <ul>
 *   <li>(123) 456-7890</li>
 *   <li>123-456-7890</li>
 *   <li>123.456.7890</li>
 *   <li>123 456 7890</li>
 *   <li>1234567890</li>
 *   <li>+1 (123) 456-7890</li>
 *   <li>1-123-456-7890</li>
 * </ul>
 * 
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>Must be a valid North American phone number (10 digits optionally preceded by country code 1)</li>
 *   <li>Area code must be in the valid area code list from CSLKPCDY.cpy</li>
 *   <li>Area code cannot start with 0 or 1 (NANP rules)</li>
 *   <li>Exchange code (second set of 3 digits) cannot start with 0 or 1</li>
 *   <li>Null and empty strings are considered valid (use @NotNull/@NotBlank for required fields)</li>
 * </ul>
 * 
 * <p>Error Messages:</p>
 * The annotation provides specific error messages for different validation failure scenarios:
 * <ul>
 *   <li>Invalid format: "Phone number format is invalid"</li>
 *   <li>Invalid area code: "Area code {areaCode} is not a valid North American area code"</li>
 *   <li>Invalid exchange code: "Exchange code cannot start with 0 or 1"</li>
 *   <li>General validation failure: "Phone number is invalid"</li>
 * </ul>
 * 
 * <p>Usage Examples:</p>
 * <pre>
 * public class Customer {
 *     &#64;ValidPhoneNumber
 *     private String primaryPhone;
 *     
 *     &#64;ValidPhoneNumber(message = "Secondary phone number is invalid")
 *     private String secondaryPhone;
 *     
 *     &#64;ValidPhoneNumber
 *     &#64;NotBlank(message = "Phone number is required")
 *     private String requiredPhone;
 * }
 * </pre>
 * 
 * @see jakarta.validation.Constraint
 * @see PhoneNumberValidator
 * @since CardDemo v1.0
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    
    /**
     * The error message to display when validation fails.
     * 
     * @return the error message template
     */
    String message() default "Phone number is invalid";
    
    /**
     * The validation groups to which this constraint belongs.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * The payload associated with this constraint.
     * Can be used to carry metadata information about the constraint.
     * 
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Indicates whether to allow null values.
     * When true, null values are considered valid.
     * When false, null values fail validation.
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default true;
    
    /**
     * Indicates whether to allow empty strings.
     * When true, empty strings are considered valid.
     * When false, empty strings fail validation.
     * 
     * @return true if empty strings are allowed, false otherwise
     */
    boolean allowEmpty() default true;
    
    /**
     * Indicates whether to perform strict area code validation.
     * When true, only area codes from the CSLKPCDY.cpy lookup table are accepted.
     * When false, basic NANP area code rules are applied (cannot start with 0 or 1).
     * 
     * @return true for strict validation, false for basic validation
     */
    boolean strictAreaCodeValidation() default true;
    
    /**
     * Indicates whether to allow international format with country code.
     * When true, phone numbers with country code 1 (e.g., +1 123-456-7890) are accepted.
     * When false, only domestic format phone numbers are accepted.
     * 
     * @return true if international format is allowed, false otherwise
     */
    boolean allowInternationalFormat() default true;
    
    /**
     * Defines several @ValidPhoneNumber annotations on the same element.
     * 
     * @see ValidPhoneNumber
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidPhoneNumber[] value();
    }
}