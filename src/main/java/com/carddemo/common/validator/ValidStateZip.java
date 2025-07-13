/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
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
 * Jakarta Bean Validation annotation for state and ZIP code cross-validation.
 * <p>
 * This annotation validates that state codes are valid US state codes and ensures
 * that state/ZIP code combinations are geographically consistent using lookup tables
 * derived from the CSLKPCDY.cpy COBOL copybook.
 * </p>
 * <p>
 * The annotation performs two levels of validation:
 * </p>
 * <ul>
 *   <li>State code validation against the official list of valid US state codes
 *       from CSLKPCDY.cpy (including all 50 states, DC, and territories)</li>
 *   <li>Cross-validation of state and ZIP code combinations for geographic accuracy
 *       using the state+ZIP prefix lookup table from CSLKPCDY.cpy</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * {@code
 * @ValidStateZip
 * public class AddressDto {
 *     private String state;
 *     private String zipCode;
 *     // getters and setters
 * }
 * }
 * </pre>
 * <p>
 * The annotation expects the annotated class to have 'state' and 'zipCode' fields
 * or corresponding getter methods. The state field should contain a 2-character
 * state code (e.g., "CA", "NY", "TX") and the zipCode field should contain a
 * 5-digit or 9-digit ZIP code.
 * </p>
 * <p>
 * Error messages:
 * </p>
 * <ul>
 *   <li>Invalid state code: "State code '{state}' is not a valid US state code"</li>
 *   <li>Invalid state/ZIP combination: "ZIP code '{zipCode}' is not valid for state '{state}'"</li>
 *   <li>Missing fields: "State and ZIP code fields are required for validation"</li>
 * </ul>
 * <p>
 * Based on COBOL copybook CSLKPCDY.cpy validation rules:
 * </p>
 * <ul>
 *   <li>US-STATE-CODE-TO-EDIT with VALID-US-STATE-CODE condition (lines 1013-1069)</li>
 *   <li>US-STATE-ZIPCODE-TO-EDIT with VALID-US-STATE-ZIP-CD2-COMBO condition (lines 1073-1313)</li>
 * </ul>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = ValidStateZipValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStateZip {
    
    /**
     * Default error message for state/ZIP validation failures.
     * 
     * @return the error message template
     */
    String message() default "State and ZIP code combination is not valid";
    
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
     * Name of the state field in the annotated class.
     * Default is "state".
     * 
     * @return the state field name
     */
    String stateField() default "state";
    
    /**
     * Name of the ZIP code field in the annotated class.
     * Default is "zipCode".
     * 
     * @return the ZIP code field name
     */
    String zipCodeField() default "zipCode";
    
    /**
     * Whether to perform strict state/ZIP combination validation.
     * When true, validates that the ZIP code prefix matches the state.
     * When false, only validates that the state code is valid.
     * Default is true.
     * 
     * @return true for strict validation, false for state-only validation
     */
    boolean strictValidation() default true;
}