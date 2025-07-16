package com.carddemo.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Bean Validation annotation for currency amount validation with BigDecimal precision.
 * 
 * <p>This annotation ensures monetary fields maintain exact financial precision equivalent to 
 * COBOL COMP-3 calculations while supporting configurable range validation. It validates that 
 * BigDecimal values conform to the financial precision requirements specified in the CardDemo 
 * application's database schema.</p>
 * 
 * <p>The annotation supports:</p>
 * <ul>
 *   <li>DECIMAL(12,2) format validation with exact financial precision matching</li>
 *   <li>Configurable minimum and maximum values for different currency contexts</li>
 *   <li>Precision and scale validation equivalent to COBOL COMP-3 field specifications</li>
 *   <li>Range validation for financial amounts from -9999999999.99 to 9999999999.99</li>
 * </ul>
 * 
 * <p>Usage Examples:</p>
 * <pre>
 * // Default validation for standard financial amounts (DECIMAL(12,2))
 * {@literal @}ValidCurrency
 * private BigDecimal accountBalance;
 * 
 * // Custom range validation for credit limits
 * {@literal @}ValidCurrency(min = "0.00", max = "999999.99", message = "Credit limit must be between $0.00 and $999,999.99")
 * private BigDecimal creditLimit;
 * 
 * // Interest rate validation with higher precision
 * {@literal @}ValidCurrency(precision = 5, scale = 4, min = "0.0001", max = "9.9999")
 * private BigDecimal interestRate;
 * 
 * // Transaction amount validation with specific context
 * {@literal @}ValidCurrency(min = "-99999.99", max = "99999.99", message = "Transaction amount exceeds allowed range")
 * private BigDecimal transactionAmount;
 * </pre>
 * 
 * <p>The validation logic ensures:</p>
 * <ul>
 *   <li>BigDecimal precision maintains exact financial calculations without floating-point errors</li>
 *   <li>Scale validation matches PostgreSQL DECIMAL column definitions</li>
 *   <li>Range validation prevents invalid financial amounts</li>
 *   <li>Null values are treated as valid (use {@code @NotNull} for null validation)</li>
 * </ul>
 * 
 * <p>This annotation is designed to maintain compatibility with the legacy COBOL COMP-3 
 * packed decimal format while providing modern Java BigDecimal validation capabilities.</p>
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since CardDemo v1.0-15-g27d6c6f-68
 * @see java.math.BigDecimal
 * @see jakarta.validation.constraints.DecimalMin
 * @see jakarta.validation.constraints.DecimalMax
 * @see jakarta.validation.constraints.Digits
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCurrencyValidator.class)
@Documented
public @interface ValidCurrency {

    /**
     * The error message template for validation failures.
     * 
     * <p>Default message provides comprehensive information about the validation failure,
     * including the expected range and precision requirements.</p>
     * 
     * @return the error message template
     */
    String message() default "Currency amount must be a valid BigDecimal with precision {precision}, scale {scale}, and range [{min}, {max}]";

    /**
     * The validation groups to which this constraint belongs.
     * 
     * <p>Groups allow for conditional validation based on different use cases
     * (e.g., account creation vs. transaction processing).</p>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * The payload associated with this constraint.
     * 
     * <p>Payloads can be used to associate metadata with validation constraints
     * for custom validation frameworks or reporting systems.</p>
     * 
     * @return the constraint payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * The minimum allowed value for the currency amount.
     * 
     * <p>This value is specified as a string to maintain exact precision without
     * floating-point representation issues. The default value accommodates the
     * full range of DECIMAL(12,2) negative values.</p>
     * 
     * <p>For different currency contexts:</p>
     * <ul>
     *   <li>Account balances: typically {@code "-999999999.99"}</li>
     *   <li>Credit limits: typically {@code "0.00"}</li>
     *   <li>Transaction amounts: typically {@code "-99999.99"}</li>
     *   <li>Interest rates: typically {@code "0.0001"}</li>
     * </ul>
     * 
     * @return the minimum value as a string representation
     */
    String min() default "-9999999999.99";

    /**
     * The maximum allowed value for the currency amount.
     * 
     * <p>This value is specified as a string to maintain exact precision without
     * floating-point representation issues. The default value accommodates the
     * full range of DECIMAL(12,2) positive values.</p>
     * 
     * <p>For different currency contexts:</p>
     * <ul>
     *   <li>Account balances: typically {@code "999999999.99"}</li>
     *   <li>Credit limits: typically {@code "999999.99"}</li>
     *   <li>Transaction amounts: typically {@code "99999.99"}</li>
     *   <li>Interest rates: typically {@code "9.9999"}</li>
     * </ul>
     * 
     * @return the maximum value as a string representation
     */
    String max() default "9999999999.99";

    /**
     * The total number of digits allowed in the currency amount.
     * 
     * <p>This represents the total precision of the BigDecimal value, including
     * both integer and fractional digits. The default value of 12 corresponds
     * to the PostgreSQL DECIMAL(12,2) column type used for financial amounts.</p>
     * 
     * <p>Common precision values:</p>
     * <ul>
     *   <li>Financial amounts: 12 digits (DECIMAL(12,2))</li>
     *   <li>Interest rates: 5 digits (DECIMAL(5,4))</li>
     *   <li>Credit scores: 3 digits (DECIMAL(3,0))</li>
     * </ul>
     * 
     * @return the maximum number of digits
     */
    int precision() default 12;

    /**
     * The number of digits allowed after the decimal point.
     * 
     * <p>This represents the scale of the BigDecimal value, determining the
     * number of fractional digits. The default value of 2 corresponds to
     * standard currency representation (cents precision).</p>
     * 
     * <p>Common scale values:</p>
     * <ul>
     *   <li>Financial amounts: 2 decimal places (currency cents)</li>
     *   <li>Interest rates: 4 decimal places (basis points)</li>
     *   <li>Credit scores: 0 decimal places (whole numbers)</li>
     * </ul>
     * 
     * @return the number of fractional digits
     */
    int scale() default 2;

    /**
     * Whether to allow null values.
     * 
     * <p>When true, null values are considered valid and no validation is performed.
     * When false, null values trigger a validation error. This provides flexibility
     * for optional vs. required currency fields.</p>
     * 
     * <p>Note: For required fields, it's recommended to use {@code @NotNull} in
     * combination with this annotation for clearer validation semantics.</p>
     * 
     * @return true if null values are allowed, false otherwise
     */
    boolean allowNull() default true;

    /**
     * Whether to enforce strict precision matching.
     * 
     * <p>When true, the BigDecimal value must have exactly the specified scale
     * (e.g., 123.45 for scale=2). When false, values with fewer decimal places
     * are accepted (e.g., 123.4 or 123 for scale=2).</p>
     * 
     * <p>Strict precision is typically required for:</p>
     * <ul>
     *   <li>Financial calculations that require exact decimal representation</li>
     *   <li>Database storage where the column scale is fixed</li>
     *   <li>Regulatory compliance requiring specific precision</li>
     * </ul>
     * 
     * @return true if strict precision matching is required, false otherwise
     */
    boolean strictPrecision() default false;

    /**
     * The rounding mode to use for precision adjustments.
     * 
     * <p>This setting determines how values are rounded when they exceed the
     * specified precision or scale. The default value matches the COBOL COMP-3
     * rounding behavior for financial calculations.</p>
     * 
     * <p>Common rounding modes:</p>
     * <ul>
     *   <li>{@code HALF_EVEN}: Banker's rounding (default for financial calculations)</li>
     *   <li>{@code HALF_UP}: Traditional rounding (0.5 rounds up)</li>
     *   <li>{@code DOWN}: Truncation (no rounding)</li>
     * </ul>
     * 
     * @return the rounding mode name
     */
    String roundingMode() default "HALF_EVEN";

    /**
     * Custom validation context for specialized currency types.
     * 
     * <p>This allows for different validation rules based on the currency context,
     * such as account balances, transaction amounts, or interest rates. The validator
     * can use this context to apply appropriate business rules.</p>
     * 
     * <p>Predefined contexts:</p>
     * <ul>
     *   <li>{@code ACCOUNT_BALANCE}: Account balance validation</li>
     *   <li>{@code TRANSACTION_AMOUNT}: Transaction amount validation</li>
     *   <li>{@code CREDIT_LIMIT}: Credit limit validation</li>
     *   <li>{@code INTEREST_RATE}: Interest rate validation</li>
     *   <li>{@code GENERAL}: General currency validation (default)</li>
     * </ul>
     * 
     * @return the validation context
     */
    String context() default "GENERAL";

    /**
     * Whether to validate against COBOL COMP-3 equivalent behavior.
     * 
     * <p>When true, the validator ensures that the BigDecimal value maintains
     * exact equivalence with COBOL COMP-3 packed decimal calculations. This
     * includes specific rounding behavior and precision requirements.</p>
     * 
     * <p>COBOL COMP-3 validation ensures:</p>
     * <ul>
     *   <li>Exact decimal precision without floating-point errors</li>
     *   <li>Proper handling of signed values</li>
     *   <li>Consistent arithmetic behavior across calculations</li>
     *   <li>Compliance with legacy financial processing requirements</li>
     * </ul>
     * 
     * @return true if COBOL COMP-3 compliance is required, false otherwise
     */
    boolean cobolCompatible() default true;

    /**
     * Defines several {@code @ValidCurrency} annotations on the same element.
     * 
     * <p>This allows for multiple currency validation rules to be applied to the
     * same field, enabling complex validation scenarios such as different rules
     * for different validation groups.</p>
     * 
     * @see ValidCurrency
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidCurrency[] value();
    }
}