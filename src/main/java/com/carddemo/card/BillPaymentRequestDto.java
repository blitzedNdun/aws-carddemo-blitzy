/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.card;

import com.carddemo.common.dto.BaseRequestDto;
import com.carddemo.common.validator.ValidPaymentAmount;
import com.carddemo.common.validator.ValidAccountId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Request DTO for bill payment operations with BigDecimal precision, comprehensive validation, 
 * and audit correlation supporting COBIL00C.cbl functionality through REST API payment processing.
 * 
 * This DTO implements the bill payment request structure from the COBOL program COBIL00C.cbl,
 * maintaining exact functional equivalence while providing modern REST API capabilities.
 * The bill payment process validates account existence, current balance, and processes 
 * payment transactions with full audit trail support.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>BigDecimal payment amounts with COBOL COMP-3 precision (S9(09)V99)</li>
 *   <li>Account ID validation matching COBOL 11-digit format requirements</li>
 *   <li>Confirmation flag validation supporting Y/N workflow from COBOL</li>
 *   <li>Card number validation for payment processing integration</li>
 *   <li>Payment description and due date support for transaction records</li>
 *   <li>Comprehensive Jakarta Bean Validation annotations</li>
 *   <li>JSON serialization with null value handling</li>
 *   <li>Audit correlation through BaseRequestDto inheritance</li>
 * </ul>
 * 
 * <p>COBOL Program Mapping:
 * <ul>
 *   <li>ACCT-ID → accountId (11-digit account identifier)</li>
 *   <li>CONFIRMI → confirmationFlag (Y/N confirmation for payment)</li>
 *   <li>TRAN-AMT → paymentAmount (BigDecimal with 2 decimal places)</li>
 *   <li>TRAN-CARD-NUM → cardNumber (16-digit card number)</li>
 *   <li>TRAN-DESC → paymentDescription (transaction description)</li>
 * </ul>
 * 
 * <p>Business Rules:
 * <ul>
 *   <li>Account ID must be exactly 11 digits and reference valid account</li>
 *   <li>Payment amount must be positive and within transaction limits</li>
 *   <li>Confirmation flag must be 'Y' or 'N' (case insensitive)</li>
 *   <li>Card number must be exactly 16 digits if provided</li>
 *   <li>Payment description must not exceed 100 characters</li>
 *   <li>Payment due date must be in the future if provided</li>
 * </ul>
 * 
 * <p>Usage Example:
 * <pre>
 * BillPaymentRequestDto request = new BillPaymentRequestDto();
 * request.setAccountId("12345678901");
 * request.setPaymentAmount(new BigDecimal("150.25"));
 * request.setConfirmationFlag("Y");
 * request.setCardNumber("1234567890123456");
 * request.setPaymentDescription("Monthly bill payment");
 * request.setPaymentDueDate(LocalDate.now().plusDays(30));
 * </pre>
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 * @see com.carddemo.common.dto.BaseRequestDto
 * @see com.carddemo.common.validator.ValidPaymentAmount
 * @see com.carddemo.common.validator.ValidAccountId
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillPaymentRequestDto extends BaseRequestDto {

    /**
     * Account identifier for bill payment processing.
     * Mapped from COBOL: ACCT-ID PIC X(11)
     * 
     * This field identifies the account for which the bill payment is being processed.
     * It must be exactly 11 digits and reference a valid account in the system.
     * The validation matches the COBOL program logic where account existence is
     * verified before processing payment transactions.
     * 
     * Business rules:
     * - Must be exactly 11 digits
     * - Must reference existing account
     * - Cannot be null or empty
     * - Must pass ValidAccountId validation with BILL_PAYMENT context
     */
    @JsonProperty("accountId")
    @NotBlank(message = "Account ID is required for bill payment processing")
    @ValidAccountId(
        message = "Account ID must be exactly 11 digits and reference a valid account",
        context = ValidAccountId.ValidationContext.BILL_PAYMENT
    )
    private String accountId;

    /**
     * Payment amount with BigDecimal precision for exact financial calculations.
     * Mapped from COBOL: TRAN-AMT PIC S9(09)V99
     * 
     * This field represents the payment amount with exact decimal precision
     * equivalent to COBOL COMP-3 arithmetic. The amount is validated to ensure
     * it falls within acceptable payment limits and maintains financial accuracy.
     * 
     * Business rules:
     * - Must be positive (greater than zero)
     * - Maximum value: 999,999,999.99 (COBOL S9(09)V99 limit)
     * - Must have exactly 2 decimal places
     * - Cannot be null
     * - Must pass ValidPaymentAmount validation
     */
    @JsonProperty("paymentAmount")
    @NotNull(message = "Payment amount is required for bill payment processing")
    @ValidPaymentAmount(
        message = "Payment amount must be valid for bill payment processing",
        allowZero = false,
        minAmount = "0.01",
        maxAmount = "999999999.99",
        decimalPlaces = 2,
        strictDecimalPrecision = true,
        enforceCobolFormat = true
    )
    private BigDecimal paymentAmount;

    /**
     * Confirmation flag for payment processing workflow.
     * Mapped from COBOL: CONFIRMI PIC X(01)
     * 
     * This field implements the confirmation workflow from the COBOL program
     * where users must confirm payment processing before transaction execution.
     * The flag supports both upper and lower case Y/N values.
     * 
     * Business rules:
     * - Must be 'Y', 'y', 'N', or 'n'
     * - Case insensitive validation
     * - Cannot be null or empty
     * - 'Y' confirms payment processing
     * - 'N' cancels payment processing
     */
    @JsonProperty("confirmationFlag")
    @NotBlank(message = "Confirmation flag is required for bill payment processing")
    @Pattern(
        regexp = "^[YyNn]$",
        message = "Confirmation flag must be Y or N (case insensitive)"
    )
    private String confirmationFlag;

    /**
     * Card number for payment processing integration.
     * Mapped from COBOL: TRAN-CARD-NUM PIC X(16)
     * 
     * This field identifies the card to be used for payment processing.
     * It must be exactly 16 digits and reference a valid, active card
     * associated with the specified account.
     * 
     * Business rules:
     * - Must be exactly 16 digits if provided
     * - Must reference existing card
     * - Card must be active and associated with account
     * - Optional field (null allowed)
     */
    @JsonProperty("cardNumber")
    @Pattern(
        regexp = "^\\d{16}$",
        message = "Card number must be exactly 16 digits"
    )
    private String cardNumber;

    /**
     * Payment description for transaction record.
     * Mapped from COBOL: TRAN-DESC PIC X(100)
     * 
     * This field provides a description for the payment transaction that
     * will be recorded in the transaction history. It supports up to 100
     * characters to match the COBOL field length limitations.
     * 
     * Business rules:
     * - Maximum length: 100 characters
     * - Optional field (null allowed)
     * - Will default to "BILL PAYMENT - ONLINE" if not provided
     */
    @JsonProperty("paymentDescription")
    @Size(max = 100, message = "Payment description must not exceed 100 characters")
    private String paymentDescription;

    /**
     * Payment due date for scheduling purposes.
     * 
     * This field supports future payment scheduling and provides context
     * for the payment transaction. It must be a future date if provided
     * to ensure logical payment processing.
     * 
     * Business rules:
     * - Must be in the future if provided
     * - Optional field (null allowed)
     * - Used for audit trail and payment scheduling
     */
    @JsonProperty("paymentDueDate")
    @Future(message = "Payment due date must be in the future")
    private LocalDate paymentDueDate;

    /**
     * Derived confirmation status for convenience methods.
     * This field provides a boolean representation of the confirmation flag
     * for easier programmatic access and validation.
     */
    private Boolean confirmed;

    /**
     * Default constructor for JSON deserialization.
     * Initializes the payment description to the default COBOL value.
     */
    public BillPaymentRequestDto() {
        super();
        this.paymentDescription = "BILL PAYMENT - ONLINE";
    }

    /**
     * Constructor with required fields for programmatic instantiation.
     * 
     * @param correlationId Unique correlation identifier for request tracking
     * @param userId User identifier for audit trail and authorization
     * @param sessionId Session identifier for distributed session management
     * @param accountId Account identifier for bill payment processing
     * @param paymentAmount Payment amount with BigDecimal precision
     * @param confirmationFlag Confirmation flag for payment processing workflow
     */
    public BillPaymentRequestDto(String correlationId, String userId, String sessionId,
                                String accountId, BigDecimal paymentAmount, String confirmationFlag) {
        super(correlationId, userId, sessionId);
        this.accountId = accountId;
        this.paymentAmount = paymentAmount;
        this.confirmationFlag = confirmationFlag;
        this.paymentDescription = "BILL PAYMENT - ONLINE";
        this.updateConfirmedStatus();
    }

    /**
     * Gets the account identifier for bill payment processing.
     * 
     * @return the account identifier as an 11-digit string
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier for bill payment processing.
     * 
     * @param accountId the account identifier as an 11-digit string
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the payment amount with BigDecimal precision.
     * 
     * @return the payment amount with exact decimal precision
     */
    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    /**
     * Sets the payment amount with BigDecimal precision.
     * 
     * @param paymentAmount the payment amount with exact decimal precision
     */
    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    /**
     * Gets the confirmation flag for payment processing workflow.
     * 
     * @return the confirmation flag as 'Y' or 'N'
     */
    public String getConfirmationFlag() {
        return confirmationFlag;
    }

    /**
     * Sets the confirmation flag for payment processing workflow.
     * Updates the derived confirmed status based on the flag value.
     * 
     * @param confirmationFlag the confirmation flag as 'Y' or 'N'
     */
    public void setConfirmationFlag(String confirmationFlag) {
        this.confirmationFlag = confirmationFlag;
        this.updateConfirmedStatus();
    }

    /**
     * Gets the card number for payment processing integration.
     * 
     * @return the card number as a 16-digit string
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number for payment processing integration.
     * 
     * @param cardNumber the card number as a 16-digit string
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Gets the payment description for transaction record.
     * 
     * @return the payment description (max 100 characters)
     */
    public String getPaymentDescription() {
        return paymentDescription;
    }

    /**
     * Sets the payment description for transaction record.
     * 
     * @param paymentDescription the payment description (max 100 characters)
     */
    public void setPaymentDescription(String paymentDescription) {
        this.paymentDescription = paymentDescription;
    }

    /**
     * Gets the payment due date for scheduling purposes.
     * 
     * @return the payment due date
     */
    public LocalDate getPaymentDueDate() {
        return paymentDueDate;
    }

    /**
     * Sets the payment due date for scheduling purposes.
     * 
     * @param paymentDueDate the payment due date
     */
    public void setPaymentDueDate(LocalDate paymentDueDate) {
        this.paymentDueDate = paymentDueDate;
    }

    /**
     * Gets the derived confirmation status for convenience access.
     * 
     * @return true if payment is confirmed, false otherwise
     */
    public boolean isConfirmed() {
        return confirmed != null && confirmed;
    }

    /**
     * Sets the derived confirmation status and updates the confirmation flag.
     * 
     * @param confirmed true to confirm payment, false to cancel
     */
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
        this.confirmationFlag = confirmed ? "Y" : "N";
    }

    /**
     * Updates the derived confirmed status based on the confirmation flag.
     * This method maintains consistency between the string flag and boolean status.
     */
    private void updateConfirmedStatus() {
        if (confirmationFlag != null) {
            this.confirmed = "Y".equalsIgnoreCase(confirmationFlag) || "y".equals(confirmationFlag);
        } else {
            this.confirmed = false;
        }
    }

    /**
     * Validates the payment request for business rule compliance.
     * This method performs comprehensive validation beyond basic field validation
     * to ensure the payment request meets all business requirements.
     * 
     * @return true if the payment request is valid, false otherwise
     */
    public boolean isValidPaymentRequest() {
        return isValidRequestContext() &&
               accountId != null && accountId.matches("^\\d{11}$") &&
               paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0 &&
               confirmationFlag != null && confirmationFlag.matches("^[YyNn]$") &&
               (cardNumber == null || cardNumber.matches("^\\d{16}$")) &&
               (paymentDescription == null || paymentDescription.length() <= 100) &&
               (paymentDueDate == null || paymentDueDate.isAfter(LocalDate.now()));
    }

    /**
     * Gets the payment amount using BigDecimal.valueOf for scale consistency.
     * This method ensures consistent scale handling for financial calculations.
     * 
     * @return the payment amount with consistent scale
     */
    public BigDecimal getPaymentAmountWithScale() {
        return paymentAmount != null ? 
               BigDecimal.valueOf(paymentAmount.doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP) : 
               null;
    }

    /**
     * Validates the payment amount scale for COBOL compatibility.
     * This method ensures the payment amount has exactly 2 decimal places
     * to match COBOL COMP-3 precision requirements.
     * 
     * @return true if scale is valid, false otherwise
     */
    public boolean isValidPaymentAmountScale() {
        return paymentAmount != null && paymentAmount.scale() == 2;
    }

    /**
     * Creates a transaction correlation key for audit purposes.
     * This method generates a unique key that can be used to correlate
     * the payment request with transaction records and audit logs.
     * 
     * @return transaction correlation key
     */
    public String getTransactionCorrelationKey() {
        return String.format("BILL_PAYMENT_%s_%s_%s", 
                           getCorrelationId(), 
                           accountId, 
                           System.currentTimeMillis());
    }

    /**
     * Creates a payment summary for audit logging.
     * This method generates a comprehensive summary of the payment request
     * that supports audit trail requirements and compliance tracking.
     * 
     * @return payment summary for audit logging
     */
    public String getPaymentSummary() {
        return String.format(
            "BillPayment[correlationId=%s, accountId=%s, amount=%s, confirmed=%s, cardNumber=%s, description=%s, dueDate=%s]",
            getCorrelationId(),
            accountId,
            paymentAmount,
            isConfirmed(),
            cardNumber != null ? cardNumber.substring(0, 4) + "****" + cardNumber.substring(12) : null,
            paymentDescription,
            paymentDueDate
        );
    }

    /**
     * Equality comparison based on account ID, payment amount, and correlation ID.
     * This method supports request deduplication and ensures proper handling
     * of duplicate payment requests in distributed systems.
     * 
     * @param obj the object to compare
     * @return true if payment requests are equal, false otherwise
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
     * Hash code generation based on account ID, payment amount, and correlation ID.
     * This method supports efficient collections handling and request correlation
     * tracking in distributed payment processing systems.
     * 
     * @return hash code for the payment request
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountId, paymentAmount, confirmationFlag, cardNumber);
    }

    /**
     * String representation for debugging and logging purposes.
     * This method provides a comprehensive string representation that supports
     * debugging while maintaining security best practices for financial data.
     * 
     * @return string representation of the payment request
     */
    @Override
    public String toString() {
        return getPaymentSummary();
    }
}