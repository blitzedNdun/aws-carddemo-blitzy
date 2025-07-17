/*
 * Jakarta Bean Validation annotation for state and ZIP code cross-validation
 * 
 * This annotation validates state codes against the official US state codes from 
 * CSLKPCDY.cpy copybook and ensures state/ZIP code combinations are geographically 
 * consistent using lookup tables derived from COBOL validation logic.
 * 
 * Converted from COBOL validation patterns in CSLKPCDY.cpy:
 * - US-STATE-CODE-TO-EDIT validation (lines 1013-1069)
 * - US-STATE-ZIPCODE-TO-EDIT validation (lines 1073-1313)
 * 
 * Implementation preserves exact COBOL business logic while providing
 * modern Jakarta Bean Validation integration for Spring Boot microservices.
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
 * Jakarta Bean Validation annotation for cross-field validation of US state codes 
 * and ZIP code combinations, ensuring geographic consistency.
 * 
 * This annotation validates that:
 * 1. State codes are valid US state abbreviations (including territories)
 * 2. State and ZIP code combinations are geographically consistent
 * 3. Field combinations follow the same validation rules as COBOL CSLKPCDY.cpy
 * 
 * Based on COBOL copybook CSLKPCDY.cpy validation patterns:
 * - VALID-US-STATE-CODE condition (lines 1013-1069)
 * - VALID-US-STATE-ZIP-CD2-COMBO condition (lines 1073-1313)
 * 
 * Usage example:
 * <pre>
 * &#64;ValidStateZip(
 *     stateField = "custAddrStateCd",
 *     zipField = "custAddrZip",
 *     message = "Invalid state code or inconsistent state/ZIP combination"
 * )
 * public class CustomerDto {
 *     private String custAddrStateCd;  // 2-character state code
 *     private String custAddrZip;      // ZIP code (first 2 digits used for validation)
 *     // ... other fields
 * }
 * </pre>
 * 
 * The validator checks:
 * - State field contains a valid 2-character US state code (AL, AK, AZ, etc.)
 * - ZIP field first 2 digits are consistent with the state code
 * - Follows geographic ZIP code allocation patterns from USPS
 * 
 * Error messages provide specific details about validation failures:
 * - "Invalid state code: {stateCode}" for invalid state codes
 * - "State {stateCode} and ZIP {zipPrefix} combination is not geographically valid"
 * - "State code is required for ZIP code validation"
 * - "ZIP code is required for state validation"
 * 
 * This annotation preserves exact COBOL business logic while providing modern
 * Jakarta Bean Validation integration for Spring Boot microservices architecture.
 */
@Documented
@Constraint(validatedBy = StateZipValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStateZip {
    
    /**
     * Default error message for validation failures.
     * Can be overridden by specifying a custom message.
     * 
     * @return the error message template
     */
    String message() default "Invalid state code or inconsistent state/ZIP combination";
    
    /**
     * Validation groups for conditional validation.
     * Allows grouping related validations for different business scenarios.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for carrying additional metadata about the validation.
     * Used by validation frameworks for additional processing context.
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * The name of the field containing the state code.
     * Must be a String field containing a 2-character US state abbreviation.
     * 
     * Maps to COBOL field: CUST-ADDR-STATE-CD PIC X(02) from CUSTREC.cpy
     * 
     * @return the state field name
     */
    String stateField();
    
    /**
     * The name of the field containing the ZIP code.
     * Must be a String field containing a ZIP code (first 2 digits used for validation).
     * 
     * Maps to COBOL field: CUST-ADDR-ZIP PIC X(10) from CUSTREC.cpy
     * 
     * @return the ZIP field name
     */
    String zipField();
    
    /**
     * Whether to validate only the state code without cross-validation.
     * When true, only validates that the state code is in the valid US states list.
     * When false (default), performs both state validation and state/ZIP consistency check.
     * 
     * @return true to validate only state code, false for full cross-validation
     */
    boolean stateOnly() default false;
    
    /**
     * Whether to allow empty/null values for validation fields.
     * When true, null or empty state/ZIP fields are considered valid.
     * When false (default), both fields must have values for validation to pass.
     * 
     * @return true to allow null values, false to require non-null values
     */
    boolean allowEmpty() default false;
    
    /**
     * Custom error message for invalid state codes.
     * Used when state code is not in the valid US states list.
     * 
     * @return the invalid state error message template
     */
    String invalidStateMessage() default "Invalid state code: {stateCode}";
    
    /**
     * Custom error message for invalid state/ZIP combinations.
     * Used when state code is valid but state/ZIP combination is not geographically consistent.
     * 
     * @return the invalid combination error message template
     */
    String invalidCombinationMessage() default "State {stateCode} and ZIP {zipPrefix} combination is not geographically valid";
    
    /**
     * Custom error message for missing state field.
     * Used when state field is null or empty and allowEmpty is false.
     * 
     * @return the missing state error message template
     */
    String missingStateMessage() default "State code is required for ZIP code validation";
    
    /**
     * Custom error message for missing ZIP field.
     * Used when ZIP field is null or empty and allowEmpty is false.
     * 
     * @return the missing ZIP error message template
     */
    String missingZipMessage() default "ZIP code is required for state validation";
    
    /**
     * Defines several @ValidStateZip annotations on the same element.
     * Allows multiple state/ZIP validation rules on a single class.
     * 
     * Usage example:
     * <pre>
     * &#64;ValidStateZip.List({
     *     &#64;ValidStateZip(stateField = "mailingState", zipField = "mailingZip"),
     *     &#64;ValidStateZip(stateField = "billingState", zipField = "billingZip")
     * })
     * public class CustomerDto {
     *     // ... fields
     * }
     * </pre>
     */
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidStateZip[] value();
    }
}