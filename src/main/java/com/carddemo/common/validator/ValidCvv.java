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

package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for validating CVV (Card Verification Value) codes.
 * 
 * <p>This annotation validates that a CVV code meets the following criteria:
 * <ul>
 *   <li>Must be exactly 3 digits long</li>
 *   <li>Must contain only numeric characters (0-9)</li>
 *   <li>Cannot be null when validation is applied</li>
 *   <li>Cannot be empty or blank</li>
 * </ul>
 * 
 * <p>The validation is based on the COBOL CVV processing logic from the legacy CardDemo
 * system where CVV codes are defined as PIC 9(03) - exactly 3 numeric digits as specified
 * in the COCRDUPC.cbl program.
 * 
 * <p>Usage examples:
 * <pre>
 * public class Card {
 *     {@literal @}ValidCvv
 *     private String cvvCode;
 *     
 *     // For optional CVV updates
 *     {@literal @}ValidCvv(groups = UpdateValidation.class, 
 *                message = "CVV must be 3 digits when provided")
 *     private String newCvvCode;
 * }
 * </pre>
 * 
 * <p>This annotation supports validation groups for different card operation scenarios,
 * allowing for context-specific validation rules and error messages.
 * 
 * @author CardDemo Migration Team
 * @since 1.0.0
 * @see jakarta.validation.constraints.Pattern
 * @see jakarta.validation.constraints.Size
 */
@Documented
@Constraint(validatedBy = {CvvValidator.class})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCvv {

    /**
     * The error message to be displayed when validation fails.
     * 
     * <p>Default message indicates that the CVV must be exactly 3 digits.
     * This message can be overridden for specific validation contexts or
     * localized for different languages.
     * 
     * <p>Message template supports parameter substitution:
     * <ul>
     *   <li>{validatedValue} - the actual value being validated</li>
     *   <li>{min} - minimum length (always 3)</li>
     *   <li>{max} - maximum length (always 3)</li>
     * </ul>
     * 
     * @return the error message template
     */
    String message() default "CVV code must be exactly 3 digits";

    /**
     * The groups the constraint belongs to.
     * 
     * <p>Validation groups allow for conditional validation based on different
     * scenarios, such as:
     * <ul>
     *   <li>Card creation - CVV is required</li>
     *   <li>Card update - CVV may be optional</li>
     *   <li>Transaction processing - CVV validation may be context-dependent</li>
     * </ul>
     * 
     * <p>Default group is used when no specific group is specified.
     * 
     * @return the groups this constraint belongs to
     */
    Class<?>[] groups() default {Default.class};

    /**
     * The payload associated with the constraint.
     * 
     * <p>Payloads are typically used by validation clients to associate metadata
     * with a given constraint declaration. This can be useful for:
     * <ul>
     *   <li>Error severity levels</li>
     *   <li>Custom error handling logic</li>
     *   <li>Integration with external validation frameworks</li>
     * </ul>
     * 
     * <p>The payload is not used by the Jakarta Bean Validation API itself.
     * 
     * @return the payload associated with the constraint
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Validation group for card creation operations.
     * 
     * <p>This group should be used when validating CVV codes during card creation
     * where the CVV is mandatory and must be provided.
     */
    interface Creation {}

    /**
     * Validation group for card update operations.
     * 
     * <p>This group should be used when validating CVV codes during card updates
     * where the CVV might be optional depending on the specific update scenario.
     */
    interface Update {}

    /**
     * Validation group for transaction processing operations.
     * 
     * <p>This group should be used when validating CVV codes during transaction
     * processing where additional security validation may be required.
     */
    interface Transaction {}
}