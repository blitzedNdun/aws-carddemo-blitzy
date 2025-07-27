/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * Jakarta Bean Validation annotation for phone number validation with area code verification.
 * 
 * <p>This validator implements the North American Numbering Plan (NANP) area code validation
 * as specified in the COBOL copybook CSLKPCDY.cpy. It validates phone numbers to ensure they:
 * 
 * <ul>
 *   <li>Conform to North American phone number format patterns</li>
 *   <li>Have valid area codes from the official NANP area code lookup table</li>
 *   <li>Follow proper digit restrictions for exchange and line numbers</li>
 *   <li>Support multiple common formatting styles (with/without separators)</li>
 * </ul>
 * 
 * <p>Supported formats include:
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
 * <p>The validator ensures that:
 * <ul>
 *   <li>Area codes (first 3 digits) are valid according to NANP registry</li>
 *   <li>Exchange codes (middle 3 digits) follow NANP rules (first digit 2-9, second digit 0-9)</li>
 *   <li>Line numbers (last 4 digits) are valid (0000-9999)</li>
 *   <li>No easily recognizable codes (like 555, 800, etc.) are used for regular numbers</li>
 * </ul>
 * 
 * <p>This annotation directly corresponds to the COBOL validation logic in CSLKPCDY.cpy
 * where the VALID-PHONE-AREA-CODE condition validates against the comprehensive area code list
 * obtained from the North America Numbering Plan Administrator (NANPA).
 * 
 * <p>Usage example:
 * <pre>
 * public class Customer {
 *     &#64;ValidPhoneNumber(message = "Phone number must be a valid North American number")
 *     private String phoneNumber1;
 *     
 *     &#64;ValidPhoneNumber(allowEmpty = true)
 *     private String phoneNumber2;
 * }
 * </pre>
 * 
 * @see com.carddemo.common.validator.PhoneNumberValidator
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {

    /**
     * Default validation error message.
     * 
     * @return the error message template
     */
    String message() default "Invalid phone number format or area code";

    /**
     * Validation groups for conditional validation.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for carrying additional metadata.
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow empty or null phone numbers.
     * When true, null and empty strings are considered valid.
     * When false, null and empty strings fail validation.
     * 
     * @return true if empty values are allowed, false otherwise
     */
    boolean allowEmpty() default false;

    /**
     * Whether to allow international formats (with country codes).
     * When true, accepts formats like +1-123-456-7890.
     * When false, only accepts North American domestic formats.
     * 
     * @return true if international formats are allowed, false otherwise
     */
    boolean allowInternational() default true;

    /**
     * Whether to perform strict area code validation.
     * When true, validates area codes against the official NANP registry.
     * When false, only validates format structure.
     * 
     * @return true for strict validation, false for format-only validation
     */
    boolean strictAreaCodeValidation() default true;

    /**
     * Whether to allow easily recognizable area codes (like 555, 800, etc.).
     * When true, accepts test numbers and service codes.
     * When false, rejects commonly used test/service numbers.
     * 
     * @return true to allow recognizable codes, false to reject them
     */
    boolean allowRecognizableCodes() default false;

    /**
     * Custom error message for invalid area code.
     * Used when strictAreaCodeValidation is true and area code is not found
     * in the NANP registry.
     * 
     * @return the area code error message template
     */
    String invalidAreaCodeMessage() default "Phone number contains invalid area code";

    /**
     * Custom error message for invalid format.
     * Used when the phone number doesn't match any supported format pattern.
     * 
     * @return the format error message template
     */
    String invalidFormatMessage() default "Phone number format is not supported";

    /**
     * Custom error message for invalid exchange code.
     * Used when the exchange code (middle 3 digits) doesn't follow NANP rules.
     * 
     * @return the exchange code error message template
     */
    String invalidExchangeMessage() default "Phone number contains invalid exchange code";
}