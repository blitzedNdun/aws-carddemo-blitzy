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
 * Jakarta Bean Validation annotation for validating US state codes and cross-validating
 * state/ZIP code combinations for geographic consistency.
 * 
 * <p>This validation annotation performs two types of validation:</p>
 * <ul>
 *   <li><strong>State Code Validation</strong>: Validates that state codes are valid US states, 
 *       territories, or military postal codes based on the lookup table from CSLKPCDY.cpy copybook</li>
 *   <li><strong>State-ZIP Cross-Validation</strong>: Ensures state and ZIP code combinations are 
 *       geographically consistent by validating the first 2 digits of the ZIP code against 
 *       the state code using the state-ZIP prefix lookup table</li>
 * </ul>
 * 
 * <p><strong>Valid State Codes:</strong></p>
 * <p>All 50 US states (AL, AK, AZ, AR, CA, CO, CT, DE, FL, GA, HI, ID, IL, IN, IA, KS, KY, LA, ME, MD, 
 * MA, MI, MN, MS, MO, MT, NE, NV, NH, NJ, NM, NY, NC, ND, OH, OK, OR, PA, RI, SC, SD, TN, TX, UT, VT, 
 * VA, WA, WV, WI, WY), District of Columbia (DC), and US territories (AS, GU, MP, PR, VI).</p>
 * 
 * <p><strong>Geographic Validation:</strong></p>
 * <p>The annotation validates that ZIP code prefixes are consistent with the state code. For example:</p>
 * <ul>
 *   <li>CA with ZIP codes starting with 90-96 (valid)</li>
 *   <li>NY with ZIP codes starting with 10-14 (valid)</li>
 *   <li>CA with ZIP codes starting with 10 (invalid - NY ZIP prefix)</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * 
 * <p><em>Class-level validation (recommended for cross-field validation):</em></p>
 * <pre>{@code
 * @ValidStateZip
 * public class CustomerAddress {
 *     @NotBlank
 *     private String stateCode;
 *     
 *     @NotBlank
 *     private String zipCode;
 *     
 *     // getters and setters...
 * }
 * }</pre>
 * 
 * <p><em>Field-level validation (state code only):</em></p>
 * <pre>{@code
 * public class Address {
 *     @ValidStateZip
 *     @NotBlank
 *     private String state;
 *     
 *     // other fields...
 * }
 * }</pre>
 * 
 * <p><strong>Error Messages:</strong></p>
 * <p>The validator provides detailed error messages for different validation failures:</p>
 * <ul>
 *   <li>"Invalid state code '{stateCode}'. Must be a valid US state, territory, or military postal code"</li>
 *   <li>"State code '{stateCode}' and ZIP code '{zipPrefix}' combination is not geographically valid"</li>
 *   <li>"State code is required for state-ZIP validation"</li>
 *   <li>"ZIP code is required for state-ZIP validation"</li>
 * </ul>
 * 
 * <p><strong>Implementation Notes:</strong></p>
 * <ul>
 *   <li>When applied at class level, the validator looks for fields named 'stateCode', 'state', 
 *       or 'custAddrStateCd' for the state value</li>
 *   <li>When applied at class level, the validator looks for fields named 'zipCode', 'zip', 
 *       or 'custAddrZip' for the ZIP code value</li>
 *   <li>When applied at field level, only state code validation is performed</li>
 *   <li>ZIP code validation uses only the first 2 digits for geographic matching</li>
 *   <li>State codes are validated case-insensitively but should be uppercase per postal standards</li>
 *   <li>Based on COBOL copybook CSLKPCDY.cpy validation rules preserving exact business logic</li>
 * </ul>
 * 
 * <p><strong>COBOL Integration:</strong></p>
 * <p>This annotation replicates the validation logic from the original COBOL system:</p>
 * <ul>
 *   <li>VALID-US-STATE-CODE condition from CSLKPCDY.cpy (lines 1013-1069)</li>
 *   <li>VALID-US-STATE-ZIP-CD2-COMBO condition from CSLKPCDY.cpy (lines 1073-1313)</li>
 *   <li>Customer record validation from CUSTREC.cpy structure</li>
 * </ul>
 * 
 * @since 1.0
 * @see ValidStateZipValidator The validator implementation class
 */
@Documented
@Constraint(validatedBy = ValidStateZipValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStateZip {
    
    /**
     * The error message template for validation failures.
     * The actual message will be determined by the validator based on the specific validation failure.
     * 
     * @return the error message template
     */
    String message() default "Invalid state or state-ZIP code combination";
    
    /**
     * The validation groups to which this constraint belongs.
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * The payload associated to the constraint.
     * This can be used by clients of the Bean Validation API to assign custom payload objects
     * to a constraint. This attribute is not used by the API itself.
     * 
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Defines whether to perform strict ZIP code prefix validation.
     * When true (default), the validator checks that the first 2 digits of the ZIP code
     * are valid for the given state based on the COBOL lookup table.
     * When false, only state code validation is performed.
     * 
     * @return true to enable strict ZIP code prefix validation, false otherwise
     */
    boolean validateZipPrefix() default true;
    
    /**
     * Defines whether empty or null values should be considered valid.
     * When true (default), null or empty state codes are considered valid,
     * allowing other annotations like @NotNull or @NotBlank to handle null/empty validation.
     * When false, null or empty values will trigger validation failure.
     * 
     * @return true to allow null/empty values, false to reject them
     */
    boolean allowEmpty() default true;
}