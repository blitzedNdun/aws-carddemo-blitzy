package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for Account ID format validation.
 * 
 * Validates account IDs to ensure they conform to the 11-digit numeric format
 * as specified in the original COBOL account record structure (CVACT01Y.cpy).
 * 
 * This annotation provides comprehensive account ID validation including:
 * - Format validation: Ensures exactly 11 numeric digits
 * - Empty/null validation: Rejects null, empty, or blank values
 * - Pattern matching: Uses regex pattern validation for strict format compliance
 * - COBOL-style error messages: Maintains consistent error message patterns
 * 
 * Usage Examples:
 * <pre>
 * // Basic validation for account ID field
 * {@code @ValidAccountId}
 * private String accountId;
 * 
 * // Custom error message for specific context
 * {@code @ValidAccountId(message = "Account ID must be 11 digits for bill payment operations")}
 * private String billPaymentAccountId;
 * 
 * // Validation group for account creation vs lookup
 * {@code @ValidAccountId(groups = {AccountCreation.class})}
 * private String newAccountId;
 * </pre>
 * 
 * Integration with Spring Boot microservices:
 * - Supports REST API request validation through @Valid annotation
 * - Integrates with Spring's MethodValidation for service layer validation
 * - Compatible with Spring Boot error handling and exception mapping
 * - Maintains exact validation behavior equivalent to COBOL field validation
 * 
 * Technical Implementation:
 * - Account ID pattern: Exactly 11 consecutive digits (0-9)
 * - Null safety: Handles null and empty string inputs appropriately
 * - Performance optimized: Uses compiled regex pattern for efficient validation
 * - Thread-safe: Stateless validator implementation for concurrent usage
 * 
 * @since CardDemo v1.0
 * @see com.carddemo.common.validator.AccountIdValidator
 */
@Documented
@Constraint(validatedBy = AccountIdValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccountId {

    /**
     * Default validation error message.
     * 
     * Uses COBOL-style error message format to maintain consistency with
     * the original application's user experience. The message pattern
     * follows the format used in COBIL00C.cbl for account validation errors.
     * 
     * @return the error message template
     */
    String message() default "Account ID must be exactly 11 digits";

    /**
     * Validation groups for conditional validation scenarios.
     * 
     * Allows different validation rules to be applied based on the business context:
     * - Account creation: May require additional existence checks
     * - Account lookup: Format validation only
     * - Bill payment operations: May require active account verification
     * - Transaction processing: May require balance validation integration
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for metadata and validation context.
     * 
     * Enables attachment of metadata to validation constraints for:
     * - Custom error handling strategies
     * - Validation severity levels
     * - Business rule context information
     * - Integration with audit and logging systems
     * 
     * @return the validation payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Custom error message for empty account ID validation.
     * 
     * Provides specific error message for null/empty/blank account IDs,
     * matching the COBOL validation message pattern from COBIL00C.cbl
     * line 161-162: "Acct ID can NOT be empty..."
     * 
     * @return the empty validation error message
     */
    String emptyMessage() default "Account ID cannot be empty";

    /**
     * Custom error message for invalid format validation.
     * 
     * Provides specific error message for account IDs that don't match
     * the required 11-digit numeric pattern, maintaining consistency
     * with COBOL field validation behavior.
     * 
     * @return the format validation error message
     */
    String formatMessage() default "Account ID must contain exactly 11 digits (0-9)";

    /**
     * Enable or disable leading zero validation.
     * 
     * Controls whether account IDs with leading zeros are considered valid:
     * - true: Accepts account IDs like "00000123456" (default COBOL behavior)
     * - false: Requires natural numeric format without leading zeros
     * 
     * This maintains compatibility with VSAM KSDS key structure where
     * account IDs are stored as fixed-length numeric strings with
     * potential leading zero padding.
     * 
     * @return true if leading zeros are allowed, false otherwise
     */
    boolean allowLeadingZeros() default true;

    /**
     * Enable strict COBOL field compatibility mode.
     * 
     * When enabled, applies additional validation rules that exactly
     * replicate COBOL PIC 9(11) field behavior:
     * - Enforces fixed 11-character length
     * - Validates numeric-only content
     * - Rejects any non-digit characters including spaces
     * - Maintains COMP-3 packed decimal compatibility for downstream processing
     * 
     * @return true for strict COBOL compatibility, false for relaxed validation
     */
    boolean strictCobolMode() default true;

    /**
     * Enable account existence validation.
     * 
     * When enabled, performs additional database lookup to verify that
     * the account ID exists in the accounts table. This feature:
     * - Integrates with Spring Data JPA repository layer
     * - Performs efficient database existence check
     * - Caches results for performance optimization
     * - Supports transaction-aware validation
     * 
     * Note: Existence validation requires database connectivity and
     * may impact validation performance. Use judiciously in high-throughput scenarios.
     * 
     * @return true to enable database existence validation, false for format-only validation
     */
    boolean checkExistence() default false;
}