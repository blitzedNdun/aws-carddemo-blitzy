/**
 * ValidAccountId annotation for Jakarta Bean Validation
 * 
 * This annotation validates account IDs to ensure they conform to the 11-digit numeric
 * format as specified in the COBOL CVACT01Y copybook (ACCT-ID PIC 9(11)).
 * 
 * Validation Rules:
 * - Must be exactly 11 digits
 * - Must contain only numeric characters (0-9)
 * - Cannot be null or empty
 * - Cannot contain leading/trailing spaces
 * 
 * Error messages match COBOL validation patterns from COBIL00C program for consistency.
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for account ID format validation.
 * 
 * Validates that an account ID conforms to the 11-digit numeric format
 * as specified in the COBOL account record structure (CVACT01Y.cpy).
 * 
 * This annotation can be applied to:
 * - String fields containing account IDs
 * - Method parameters requiring account ID validation
 * - Method return values that should be validated as account IDs
 * 
 * Usage Examples:
 * <pre>
 * {@code
 * public class AccountRequest {
 *     @ValidAccountId
 *     private String accountId;
 * }
 * 
 * public class BillPaymentRequest {
 *     @ValidAccountId(message = "Invalid account ID for bill payment")
 *     private String paymentAccountId;
 * }
 * 
 * public class AccountService {
 *     public Account findAccount(@ValidAccountId String accountId) {
 *         // Method implementation
 *     }
 * }
 * }
 * </pre>
 * 
 * Validation Groups:
 * - Can be used with validation groups for different validation scenarios
 * - Supports conditional validation based on business context
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = ValidAccountIdValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, 
          ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidAccountId.List.class)
public @interface ValidAccountId {

    /**
     * Error message to be displayed when validation fails.
     * 
     * Default message matches COBOL validation pattern from COBIL00C program.
     * Can be overridden for specific validation contexts.
     * 
     * Message interpolation supports:
     * - {validatedValue} - The actual value being validated
     * - Custom message parameters for different contexts
     * 
     * @return the error message template
     */
    String message() default "Account ID must be 11 digits";

    /**
     * Validation groups for conditional validation.
     * 
     * Allows different validation rules to be applied based on the validation context.
     * Common groups might include:
     * - Creation.class - For new account creation
     * - Update.class - For account updates
     * - BillPayment.class - For bill payment operations
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for validation metadata.
     * 
     * Can be used to carry additional metadata about the validation constraint.
     * Useful for providing severity levels or additional validation context.
     * 
     * @return the validation payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Defines whether empty or null values should be considered valid.
     * 
     * When set to true, null or empty values will pass validation.
     * When set to false (default), null or empty values will fail validation
     * matching the COBOL behavior from COBIL00C where empty account IDs
     * trigger "Acct ID can NOT be empty..." error.
     * 
     * @return true if empty values are allowed, false otherwise
     */
    boolean allowEmpty() default false;

    /**
     * Defines whether to perform strict numeric validation.
     * 
     * When set to true (default), only pure numeric strings are accepted.
     * When set to false, strings that can be parsed as numbers are accepted.
     * 
     * @return true for strict numeric validation, false for lenient parsing
     */
    boolean strictNumeric() default true;

    /**
     * Custom message for empty account ID validation.
     * 
     * This message is used when an account ID is null or empty,
     * matching the COBOL validation message pattern.
     * 
     * @return the empty account ID error message
     */
    String emptyMessage() default "Acct ID can NOT be empty...";

    /**
     * Custom message for invalid format validation.
     * 
     * This message is used when an account ID doesn't match the 11-digit format,
     * providing clear guidance on the expected format.
     * 
     * @return the invalid format error message
     */
    String formatMessage() default "Account ID must be exactly 11 numeric digits";

    /**
     * Defines several {@code @ValidAccountId} constraints on the same element.
     * 
     * This allows multiple ValidAccountId constraints to be applied to the same
     * field or method, each with different validation parameters or groups.
     * 
     * Usage Example:
     * <pre>
     * {@code
     * @ValidAccountId.List({
     *     @ValidAccountId(groups = Creation.class, message = "Invalid account ID for creation"),
     *     @ValidAccountId(groups = Update.class, allowEmpty = true, message = "Invalid account ID for update")
     * })
     * private String accountId;
     * }
     * </pre>
     */
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, 
              ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * Array of ValidAccountId constraints.
         * 
         * @return array of ValidAccountId annotations
         */
        ValidAccountId[] value();
    }
}