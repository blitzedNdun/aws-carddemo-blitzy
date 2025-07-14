package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidPaymentAmount;
import com.carddemo.common.validator.ValidAccountId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Request DTO for bill payment operations supporting COBIL00C.cbl functionality.
 * 
 * This DTO provides comprehensive request structure for bill payment processing with
 * exact BigDecimal precision equivalent to COBOL COMP-3 arithmetic, comprehensive
 * validation, and audit correlation supporting REST API payment processing in the
 * CardDemo microservices architecture.
 * 
 * Design Principles:
 * - BigDecimal payment amounts with exact decimal precision (S9(09)V99 equivalent)
 * - Comprehensive business rule validation for payment amounts and account verification
 * - Audit correlation parameters for compliance tracking and transaction management
 * - Payment method validation including balance and credit limit checking
 * - COBOL-equivalent field validation with Spring Boot integration
 * 
 * COBOL Source Integration:
 * - Converted from COBIL00C.cbl bill payment transaction processing
 * - Maintains TRAN-AMT PIC S9(09)V99 precision through BigDecimal operations
 * - Preserves account ID validation from ACCT-ID field processing
 * - Implements confirmation flag logic from CONFIRMI field validation
 * - Supports card number association through XREF-CARD-NUM relationship
 * 
 * Business Rules Implemented:
 * - Payment amount validation with minimum/maximum thresholds
 * - Account balance verification and credit limit checking
 * - Account ID format validation (11-digit numeric format)
 * - Card number Luhn algorithm validation for payment processing
 * - Due date validation for payment scheduling
 * - Confirmation flag validation for payment authorization
 * 
 * JSON Serialization:
 * - Jackson annotations for clean API requests and optimal data transfer
 * - Null value exclusion for cleaner request payloads
 * - Property naming consistent with REST API conventions
 * 
 * @author Blitzy agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 2022-07-19
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillPaymentRequestDto extends BaseRequestDto {

    private static final long serialVersionUID = 1L;

    /**
     * Account ID for bill payment processing.
     * 
     * Converted from COBIL00C.cbl ACCT-ID field processing with comprehensive
     * validation equivalent to COBOL PIC 9(11) field validation. This field
     * undergoes account existence verification and balance validation before
     * payment processing authorization.
     * 
     * Validation Rules:
     * - Must be exactly 11 digits (0-9 only)
     * - Account must exist in accounts table
     * - Account must have sufficient balance for payment
     * - Account must be in active status for payment processing
     * 
     * Business Context:
     * - Used for account lookup and balance verification
     * - Cross-referenced with XREF-ACCT-ID for card association
     * - Validated against ACCT-CURR-BAL for payment authorization
     */
    @JsonProperty("account_id")
    @NotBlank(message = "Account ID cannot be empty")
    @ValidAccountId(message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Payment amount with exact BigDecimal precision.
     * 
     * Converted from COBIL00C.cbl TRAN-AMT field with PIC S9(09)V99 precision
     * equivalent to COBOL COMP-3 packed decimal arithmetic. This field ensures
     * exact financial calculations without floating-point precision errors.
     * 
     * Precision Requirements:
     * - Maximum 9 digits before decimal point
     * - Exactly 2 digits after decimal point
     * - Range: 0.01 to 999,999,999.99
     * - BigDecimal operations with DECIMAL128 context
     * 
     * Business Validation:
     * - Must be positive (greater than zero)
     * - Cannot exceed account current balance
     * - Must not exceed credit limit constraints
     * - Precision maintained for audit trail requirements
     */
    @JsonProperty("payment_amount")
    @NotNull(message = "Payment amount is required")
    @ValidPaymentAmount(
        message = "Payment amount must be valid for bill payment processing",
        minAmount = "0.01",
        maxAmount = "999999999.99",
        scale = 2,
        precision = 11
    )
    private BigDecimal paymentAmount;

    /**
     * Confirmation flag for payment authorization.
     * 
     * Converted from COBIL00C.cbl CONFIRMI field validation supporting Y/N
     * confirmation pattern equivalent to COBOL 88-level condition processing.
     * This flag controls payment processing authorization and user confirmation.
     * 
     * Validation Pattern:
     * - Accepts 'Y', 'y', 'N', 'n' characters only
     * - Case-insensitive validation for user convenience
     * - Empty/null values treated as pending confirmation
     * - Used for payment authorization gate control
     * 
     * Business Logic:
     * - 'Y'/'y': Payment confirmed and ready for processing
     * - 'N'/'n': Payment declined, clear screen and reset
     * - Empty/null: Display confirmation prompt to user
     */
    @JsonProperty("confirmation_flag")
    @Pattern(
        regexp = "^[YyNn]?$",
        message = "Confirmation flag must be Y, y, N, n, or empty"
    )
    private String confirmationFlag;

    /**
     * Credit card number for payment processing.
     * 
     * Converted from COBIL00C.cbl XREF-CARD-NUM association supporting card-based
     * payment processing with Luhn algorithm validation. This field enables
     * card-to-account relationship verification and payment method validation.
     * 
     * Validation Requirements:
     * - Must be exactly 16 digits (0-9 only)
     * - Must pass Luhn algorithm validation
     * - Must be associated with the specified account ID
     * - Card must be in active status for payment processing
     * - Card must not be expired for transaction authorization
     * 
     * Security Considerations:
     * - Card number masked in logs and audit trails
     * - Validated against cards table for existence and status
     * - Cross-referenced with account association for authorization
     */
    @JsonProperty("card_number")
    @Pattern(
        regexp = "\\d{16}",
        message = "Card number must be exactly 16 digits"
    )
    private String cardNumber;

    /**
     * Payment description for transaction recording.
     * 
     * Converted from COBIL00C.cbl TRAN-DESC field with default value
     * "BILL PAYMENT - ONLINE" matching original COBOL transaction description.
     * This field provides transaction context for audit trails and statements.
     * 
     * Default Value: "BILL PAYMENT - ONLINE"
     * Maximum Length: 100 characters (matching COBOL PIC X(100))
     * 
     * Business Context:
     * - Used for transaction history and statement generation
     * - Provides clear transaction identification for users
     * - Maintained for audit trail and compliance requirements
     */
    @JsonProperty("payment_description")
    @Size(max = 100, message = "Payment description cannot exceed 100 characters")
    private String paymentDescription = "BILL PAYMENT - ONLINE";

    /**
     * Payment due date for scheduling.
     * 
     * Optional field supporting future-dated payment scheduling with validation
     * to ensure reasonable payment timing. This field enables payment scheduling
     * functionality beyond immediate payment processing.
     * 
     * Validation Rules:
     * - Must be current date or future date
     * - Cannot be more than 30 days in the future
     * - Used for payment scheduling and batch processing
     * 
     * Business Logic:
     * - Null/empty: Immediate payment processing
     * - Future date: Schedule payment for specified date
     * - Past date: Rejected with validation error
     */
    @JsonProperty("payment_due_date")
    private LocalDate paymentDueDate;

    /**
     * Boolean confirmation status for business logic.
     * 
     * Derived from confirmationFlag for simplified business logic processing.
     * This field provides boolean convenience for payment authorization logic
     * while maintaining the original string flag for COBOL compatibility.
     * 
     * Derivation Logic:
     * - true: confirmationFlag is 'Y' or 'y'
     * - false: confirmationFlag is 'N', 'n', null, or empty
     * 
     * Business Usage:
     * - Used in service layer for authorization decisions
     * - Simplifies conditional logic in payment processing
     * - Maintains audit trail through original confirmationFlag
     */
    @JsonProperty("confirmed")
    private Boolean confirmed;

    /**
     * Default constructor for JSON deserialization.
     * 
     * Initializes DTO with default values and base request context.
     * The payment description is set to match COBOL default value
     * maintaining consistency with original transaction processing.
     */
    public BillPaymentRequestDto() {
        super();
        this.paymentDescription = "BILL PAYMENT - ONLINE";
        this.confirmed = false;
    }

    /**
     * Constructor with required payment information.
     * 
     * @param accountId Account ID for payment processing
     * @param paymentAmount Payment amount with exact precision
     * @param confirmationFlag User confirmation flag (Y/N)
     */
    public BillPaymentRequestDto(String accountId, BigDecimal paymentAmount, String confirmationFlag) {
        super();
        this.accountId = accountId;
        this.paymentAmount = paymentAmount;
        this.confirmationFlag = confirmationFlag;
        this.paymentDescription = "BILL PAYMENT - ONLINE";
        this.confirmed = determineConfirmationStatus(confirmationFlag);
    }

    /**
     * Full constructor with all payment details.
     * 
     * @param accountId Account ID for payment processing
     * @param paymentAmount Payment amount with exact precision
     * @param confirmationFlag User confirmation flag (Y/N)
     * @param cardNumber Credit card number for payment method
     * @param paymentDescription Custom payment description
     * @param paymentDueDate Payment due date for scheduling
     */
    public BillPaymentRequestDto(String accountId, BigDecimal paymentAmount, String confirmationFlag,
                                String cardNumber, String paymentDescription, LocalDate paymentDueDate) {
        super();
        this.accountId = accountId;
        this.paymentAmount = paymentAmount;
        this.confirmationFlag = confirmationFlag;
        this.cardNumber = cardNumber;
        this.paymentDescription = paymentDescription != null ? paymentDescription : "BILL PAYMENT - ONLINE";
        this.paymentDueDate = paymentDueDate;
        this.confirmed = determineConfirmationStatus(confirmationFlag);
    }

    /**
     * Gets the account ID for payment processing.
     * 
     * @return 11-digit account ID string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for payment processing.
     * 
     * @param accountId 11-digit account ID string
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the payment amount with exact BigDecimal precision.
     * 
     * @return Payment amount with COBOL COMP-3 equivalent precision
     */
    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    /**
     * Sets the payment amount with exact BigDecimal precision.
     * 
     * @param paymentAmount Payment amount with exact decimal precision
     */
    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    /**
     * Gets the confirmation flag for payment authorization.
     * 
     * @return Confirmation flag (Y/N or null)
     */
    public String getConfirmationFlag() {
        return confirmationFlag;
    }

    /**
     * Sets the confirmation flag for payment authorization.
     * 
     * Updates both the string flag and derived boolean confirmation status.
     * 
     * @param confirmationFlag Confirmation flag (Y/N or null)
     */
    public void setConfirmationFlag(String confirmationFlag) {
        this.confirmationFlag = confirmationFlag;
        this.confirmed = determineConfirmationStatus(confirmationFlag);
    }

    /**
     * Gets the credit card number for payment processing.
     * 
     * @return 16-digit credit card number
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the credit card number for payment processing.
     * 
     * @param cardNumber 16-digit credit card number
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the payment description for transaction recording.
     * 
     * @return Payment description string
     */
    public String getPaymentDescription() {
        return paymentDescription;
    }

    /**
     * Sets the payment description for transaction recording.
     * 
     * @param paymentDescription Payment description string
     */
    public void setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
    }

    /**
     * Gets the payment due date for scheduling.
     * 
     * @return Payment due date or null for immediate processing
     */
    public LocalDate getPaymentDueDate() {
        return paymentDueDate;
    }

    /**
     * Sets the payment due date for scheduling.
     * 
     * @param paymentDueDate Payment due date or null for immediate processing
     */
    public void setPaymentDueDate(LocalDate paymentDueDate) {
        this.paymentDueDate = paymentDueDate;
    }

    /**
     * Gets the boolean confirmation status for business logic.
     * 
     * @return true if payment is confirmed, false otherwise
     */
    public boolean isConfirmed() {
        return confirmed != null && confirmed;
    }

    /**
     * Sets the boolean confirmation status for business logic.
     * 
     * @param confirmed true if payment is confirmed, false otherwise
     */
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
        this.confirmationFlag = confirmed ? "Y" : "N";
    }

    /**
     * Determines boolean confirmation status from string flag.
     * 
     * Converts COBOL-style Y/N confirmation flag to boolean value
     * for simplified business logic processing.
     * 
     * @param flag Confirmation flag string (Y/N or null)
     * @return true if flag is 'Y' or 'y', false otherwise
     */
    private boolean determineConfirmationStatus(String flag) {
        return flag != null && (flag.equalsIgnoreCase("Y"));
    }

    /**
     * Validates payment amount against business rules.
     * 
     * Performs comprehensive validation including:
     * - Positive amount validation
     * - Precision and scale validation
     * - Range validation (0.01 to 999,999,999.99)
     * 
     * @return true if payment amount is valid, false otherwise
     */
    public boolean isValidPaymentAmount() {
        if (paymentAmount == null) {
            return false;
        }
        
        // Check positive amount
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Check maximum amount (999,999,999.99)
        BigDecimal maxAmount = new BigDecimal("999999999.99");
        if (paymentAmount.compareTo(maxAmount) > 0) {
            return false;
        }
        
        // Check scale (2 decimal places)
        if (paymentAmount.scale() > 2) {
            return false;
        }
        
        // Check precision (11 total digits)
        if (paymentAmount.precision() > 11) {
            return false;
        }
        
        return true;
    }

    /**
     * Validates account ID format and business rules.
     * 
     * Performs validation including:
     * - 11-digit numeric format validation
     * - Non-empty validation
     * - Format consistency with COBOL PIC 9(11) specification
     * 
     * @return true if account ID is valid format, false otherwise
     */
    public boolean isValidAccountId() {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }
        
        // Check 11-digit numeric format
        return accountId.matches("\\d{11}");
    }

    /**
     * Validates card number format and Luhn algorithm.
     * 
     * Performs validation including:
     * - 16-digit numeric format validation
     * - Luhn algorithm validation for card number integrity
     * - Non-empty validation when card number is provided
     * 
     * @return true if card number is valid or null, false if invalid
     */
    public boolean isValidCardNumber() {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return true; // Card number is optional
        }
        
        // Check 16-digit numeric format
        if (!cardNumber.matches("\\d{16}")) {
            return false;
        }
        
        // Luhn algorithm validation
        return isLuhnValid(cardNumber);
    }

    /**
     * Validates card number using Luhn algorithm.
     * 
     * Implements standard Luhn algorithm for credit card number validation
     * ensuring card number integrity and format compliance.
     * 
     * @param cardNumber 16-digit card number string
     * @return true if card number passes Luhn validation, false otherwise
     */
    private boolean isLuhnValid(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }

    /**
     * Validates payment due date business rules.
     * 
     * Performs validation including:
     * - Future date validation (cannot be past date)
     * - Reasonable date range (not more than 30 days future)
     * - Null handling for immediate payment processing
     * 
     * @return true if payment due date is valid or null, false otherwise
     */
    public boolean isValidPaymentDueDate() {
        if (paymentDueDate == null) {
            return true; // Null means immediate payment
        }
        
        LocalDate today = LocalDate.now();
        
        // Cannot be past date
        if (paymentDueDate.isBefore(today)) {
            return false;
        }
        
        // Cannot be more than 30 days in future
        LocalDate maxFutureDate = today.plusDays(30);
        if (paymentDueDate.isAfter(maxFutureDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Comprehensive validation of all request fields.
     * 
     * Validates all required fields and business rules for bill payment processing.
     * This method provides complete validation equivalent to COBOL field validation
     * and business rule enforcement.
     * 
     * @return true if all fields are valid for payment processing, false otherwise
     */
    public boolean isValidRequest() {
        return isValidRequestContext() &&
               isValidAccountId() &&
               isValidPaymentAmount() &&
               isValidCardNumber() &&
               isValidPaymentDueDate();
    }

    /**
     * Equality comparison based on payment details.
     * 
     * Compares payment requests based on account ID, payment amount, and
     * confirmation status for deduplication and caching purposes.
     * 
     * @param obj Object to compare for equality
     * @return true if objects represent the same payment request
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        BillPaymentRequestDto that = (BillPaymentRequestDto) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(paymentAmount, that.paymentAmount) &&
               Objects.equals(confirmationFlag, that.confirmationFlag) &&
               Objects.equals(cardNumber, that.cardNumber);
    }

    /**
     * Hash code based on payment details for consistent hashing.
     * 
     * @return Hash code based on payment request details
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountId, paymentAmount, confirmationFlag, cardNumber);
    }

    /**
     * String representation of payment request for logging and debugging.
     * 
     * Provides comprehensive request information while masking sensitive data
     * such as card numbers for security compliance.
     * 
     * @return String representation of payment request
     */
    @Override
    public String toString() {
        return String.format(
            "BillPaymentRequestDto{" +
            "accountId='%s', " +
            "paymentAmount=%s, " +
            "confirmationFlag='%s', " +
            "cardNumber='%s', " +
            "paymentDescription='%s', " +
            "paymentDueDate=%s, " +
            "confirmed=%s, " +
            "correlationId='%s'" +
            "}",
            accountId,
            paymentAmount,
            confirmationFlag,
            cardNumber != null ? maskCardNumber(cardNumber) : null,
            paymentDescription,
            paymentDueDate,
            confirmed,
            getCorrelationId()
        );
    }

    /**
     * Masks card number for secure logging and display.
     * 
     * Shows only the last 4 digits with asterisks masking the rest
     * for security compliance while maintaining audit trail capabilities.
     * 
     * @param cardNumber Original card number
     * @return Masked card number (e.g., "************1234")
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****************";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}