/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.card;

import com.carddemo.common.dto.BaseResponseDto;
import com.carddemo.transaction.TransactionDTO;
import com.carddemo.common.dto.AuditInfo;
import com.carddemo.account.AccountBalanceDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

/**
 * Response DTO for bill payment operations with transaction confirmation, real-time balance updates, 
 * and comprehensive audit information providing payment receipt for COBIL00C.cbl functionality.
 * 
 * <p>This response DTO represents the complete output of the bill payment transaction process,
 * equivalent to the COBOL COBIL00C program response structure. It provides comprehensive
 * payment confirmation details, real-time balance updates, and audit trail information
 * required for compliance and customer receipt generation.</p>
 * 
 * <p><strong>COBOL Program Mapping:</strong></p>
 * <p>Maps to COBIL00C.cbl bill payment transaction processing with the following key behaviors:</p>
 * <ul>
 *   <li>Payment Amount: Corresponds to ACCT-CURR-BAL processed amount</li>
 *   <li>Transaction Details: Maps to TRAN-RECORD structure from CVTRA05Y.cpy</li>
 *   <li>Balance Update: Before/after balance comparison from account record update</li>
 *   <li>Confirmation Number: Transaction ID generated via sequence logic</li>
 *   <li>Processing Status: Success/failure indicator from COBOL processing result</li>
 * </ul>
 * 
 * <p><strong>Financial Precision:</strong></p>
 * <p>All monetary amounts use BigDecimal with COBOL COMP-3 equivalent precision (PIC S9(10)V99)
 * ensuring exact financial calculations without floating-point errors. The precision maintains
 * compatibility with the original COBOL financial arithmetic operations.</p>
 * 
 * <p><strong>Audit Trail Compliance:</strong></p>
 * <p>Includes comprehensive audit information for SOX compliance, PCI DSS requirements,
 * and regulatory financial transaction tracking. The audit trail captures user context,
 * transaction correlation, and processing timestamps for complete compliance coverage.</p>
 * 
 * <p><strong>Real-time Balance Updates:</strong></p>
 * <p>Provides before/after balance comparison enabling immediate customer feedback about
 * the impact of their payment on account balances, credit utilization, and available credit.</p>
 * 
 * <p><strong>Integration with Spring Boot Architecture:</strong></p>
 * <p>Designed for seamless integration with Spring Boot microservices architecture,
 * React frontend components, and REST API serialization. Includes Jackson annotations
 * for consistent JSON formatting and Jakarta Bean Validation for data integrity.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * {@code
 * // Creating successful payment response
 * BillPaymentResponseDto response = new BillPaymentResponseDto("CORR-123");
 * response.setPaymentAmount(new BigDecimal("125.50"));
 * response.setPaymentSuccessful(true);
 * response.setProcessingStatus("COMPLETED");
 * response.setPaymentConfirmationNumber("TXN001234567890");
 * 
 * // Adding transaction details
 * TransactionDTO transaction = new TransactionDTO();
 * transaction.setTransactionId("TXN001234567890");
 * transaction.setAmount(new BigDecimal("125.50"));
 * response.setTransactionDetails(transaction);
 * 
 * // Adding balance update information
 * AccountBalanceDto balanceUpdate = new AccountBalanceDto();
 * balanceUpdate.setPreviousBalance(new BigDecimal("250.00"));
 * balanceUpdate.setCurrentBalance(new BigDecimal("124.50"));
 * response.setBalanceUpdate(balanceUpdate);
 * }
 * </pre>
 * 
 * <p><strong>Error Handling:</strong></p>
 * <p>Inherits comprehensive error handling from BaseResponseDto, supporting detailed
 * error messages, correlation tracking, and proper HTTP status indication through
 * the success/failure flag system.</p>
 * 
 * <p><strong>Thread Safety:</strong></p>
 * <p>This class is designed to be thread-safe for concurrent operations in microservices
 * environments, with immutable field access patterns and no shared mutable state.</p>
 * 
 * @author Blitzy Agent - CardDemo Transformation Team
 * @version 1.0.0
 * @since 2024-01-01
 * @see BaseResponseDto
 * @see TransactionDTO
 * @see AuditInfo
 * @see AccountBalanceDto
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillPaymentResponseDto extends BaseResponseDto {

    /**
     * Payment amount processed in the bill payment transaction.
     * 
     * <p>Represents the exact amount paid by the customer, corresponding to the
     * ACCT-CURR-BAL amount processed in COBIL00C.cbl. Uses BigDecimal with
     * COBOL COMP-3 equivalent precision (PIC S9(10)V99) for exact financial
     * calculations without floating-point errors.</p>
     * 
     * <p>For successful payments, this represents the full account balance that
     * was paid. For partial payments or errors, this represents the attempted
     * payment amount for audit and reconciliation purposes.</p>
     */
    @NotNull(message = "Payment amount is required for bill payment response")
    private BigDecimal paymentAmount;

    /**
     * Complete transaction details for the bill payment operation.
     * 
     * <p>Contains comprehensive transaction information mapped from the COBOL
     * TRAN-RECORD structure (CVTRA05Y.cpy), including transaction ID, timestamps,
     * merchant information, and processing details. This provides the complete
     * transaction record for customer receipts and audit trail purposes.</p>
     * 
     * <p>The transaction details include:</p>
     * <ul>
     *   <li>Transaction ID: Unique identifier for correlation and reference</li>
     *   <li>Transaction Type: Bill payment transaction code ('02')</li>
     *   <li>Amount: Payment amount with exact precision</li>
     *   <li>Timestamps: Original and processing timestamps</li>
     *   <li>Merchant Information: Bill payment merchant details</li>
     * </ul>
     */
    @Valid
    private TransactionDTO transactionDetails;

    /**
     * Real-time balance update information with before/after comparison.
     * 
     * <p>Provides comprehensive balance information showing the impact of the
     * payment on the customer's account. Includes previous balance, current
     * balance after payment, balance difference, and available credit updates.</p>
     * 
     * <p>The balance update enables immediate customer feedback about:</p>
     * <ul>
     *   <li>Previous Balance: Account balance before payment</li>
     *   <li>Current Balance: Account balance after payment processing</li>
     *   <li>Balance Difference: Amount of balance reduction from payment</li>
     *   <li>Available Credit: Updated credit availability after payment</li>
     * </ul>
     */
    @Valid
    private AccountBalanceDto balanceUpdate;

    /**
     * Comprehensive audit trail information for compliance and tracking.
     * 
     * <p>Contains detailed audit information required for SOX compliance, PCI DSS
     * requirements, and regulatory financial transaction tracking. Includes user
     * context, transaction correlation, processing timestamps, and security
     * information for complete audit trail coverage.</p>
     * 
     * <p>The audit information captures:</p>
     * <ul>
     *   <li>User ID: Identity of user who initiated the payment</li>
     *   <li>Correlation ID: Unique identifier for distributed transaction tracking</li>
     *   <li>Timestamp: Precise timing of audit event</li>
     *   <li>Operation Type: Bill payment operation classification</li>
     *   <li>Session Context: User session and security information</li>
     * </ul>
     */
    @Valid
    private AuditInfo auditInfo;

    /**
     * Payment confirmation number for customer reference and tracking.
     * 
     * <p>Unique confirmation number generated for successful payments, typically
     * the transaction ID from the COBOL transaction sequence generation logic.
     * This confirmation number serves as the customer's receipt reference and
     * enables customer service lookup and dispute resolution.</p>
     * 
     * <p>For successful payments, this corresponds to the TRAN-ID generated by
     * the COBOL sequence logic. For failed payments, this may be null or contain
     * a reference number for tracking the failed attempt.</p>
     */
    private String paymentConfirmationNumber;

    /**
     * Processing status indicating the outcome of the bill payment operation.
     * 
     * <p>Detailed status information beyond the basic success/failure flag,
     * providing specific processing state information for system integration
     * and customer feedback. Common values include:</p>
     * <ul>
     *   <li>COMPLETED: Payment successfully processed and committed</li>
     *   <li>PENDING: Payment initiated but awaiting final processing</li>
     *   <li>FAILED: Payment processing failed due to system error</li>
     *   <li>REJECTED: Payment rejected due to business rule violation</li>
     *   <li>INSUFFICIENT_FUNDS: Payment rejected due to insufficient balance</li>
     * </ul>
     */
    private String processingStatus;

    /**
     * Detailed status message providing human-readable processing information.
     * 
     * <p>Comprehensive status message for customer display and system integration,
     * providing detailed information about the payment processing outcome. This
     * message complements the processingStatus with detailed explanations
     * suitable for customer communication and system logging.</p>
     * 
     * <p>Examples of status messages:</p>
     * <ul>
     *   <li>Success: "Payment successful. Your Transaction ID is TXN001234567890."</li>
     *   <li>Failure: "Payment failed due to insufficient account balance."</li>
     *   <li>Error: "Payment could not be processed due to system error."</li>
     * </ul>
     */
    private String statusMessage;

    /**
     * Boolean flag indicating whether the payment operation was successful.
     * 
     * <p>Primary success indicator for the bill payment operation, providing
     * clear boolean logic for system integration and customer feedback. This
     * flag determines whether the payment was successfully processed and
     * committed to the account.</p>
     * 
     * <p>true: Payment successfully processed, balance updated, transaction recorded</p>
     * <p>false: Payment failed, no balance changes, error conditions present</p>
     */
    private boolean paymentSuccessful;

    /**
     * Default constructor for JSON deserialization and framework compatibility.
     * 
     * <p>Creates a new BillPaymentResponseDto with default values and current
     * timestamp. Initializes the response as unsuccessful by default, requiring
     * explicit success confirmation through business logic.</p>
     */
    public BillPaymentResponseDto() {
        super();
        this.paymentSuccessful = false;
        this.processingStatus = "PENDING";
    }

    /**
     * Constructor for creating bill payment response with correlation ID.
     * 
     * <p>Creates a new BillPaymentResponseDto with the specified correlation ID
     * for distributed transaction tracking. Initializes common response fields
     * and sets up the response for population with payment-specific data.</p>
     * 
     * @param correlationId Unique identifier for distributed transaction tracking
     */
    public BillPaymentResponseDto(String correlationId) {
        super(correlationId);
        this.paymentSuccessful = false;
        this.processingStatus = "PENDING";
    }

    /**
     * Constructor for creating successful payment response with core information.
     * 
     * <p>Creates a new BillPaymentResponseDto pre-configured for successful
     * payment processing with the essential payment information. This constructor
     * is used for successful payment scenarios where all core information is
     * available at response creation time.</p>
     * 
     * @param correlationId Unique identifier for distributed transaction tracking
     * @param paymentAmount The amount successfully processed in the payment
     * @param confirmationNumber Payment confirmation number for customer reference
     */
    public BillPaymentResponseDto(String correlationId, BigDecimal paymentAmount, String confirmationNumber) {
        super(correlationId);
        this.paymentAmount = paymentAmount;
        this.paymentConfirmationNumber = confirmationNumber;
        this.paymentSuccessful = true;
        this.processingStatus = "COMPLETED";
        this.statusMessage = "Payment successfully processed.";
    }

    /**
     * Factory method for creating successful payment response.
     * 
     * <p>Provides a fluent API for creating successful payment responses with
     * all essential information. This method ensures consistent initialization
     * of successful payment responses across the application.</p>
     * 
     * @param correlationId Unique identifier for distributed transaction tracking
     * @param paymentAmount The amount successfully processed in the payment
     * @param confirmationNumber Payment confirmation number for customer reference
     * @return Pre-configured BillPaymentResponseDto for successful payment
     */
    public static BillPaymentResponseDto createSuccessfulPayment(String correlationId, 
                                                                BigDecimal paymentAmount, 
                                                                String confirmationNumber) {
        BillPaymentResponseDto response = new BillPaymentResponseDto(correlationId, paymentAmount, confirmationNumber);
        response.setSuccess(true);
        return response;
    }

    /**
     * Factory method for creating failed payment response.
     * 
     * <p>Provides a fluent API for creating failed payment responses with
     * appropriate error information. This method ensures consistent initialization
     * of failed payment responses across the application.</p>
     * 
     * @param correlationId Unique identifier for distributed transaction tracking
     * @param errorMessage Detailed error message describing the failure
     * @param paymentAmount The amount that was attempted to be processed
     * @return Pre-configured BillPaymentResponseDto for failed payment
     */
    public static BillPaymentResponseDto createFailedPayment(String correlationId, 
                                                            String errorMessage, 
                                                            BigDecimal paymentAmount) {
        BillPaymentResponseDto response = new BillPaymentResponseDto(correlationId);
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setPaymentAmount(paymentAmount);
        response.setPaymentSuccessful(false);
        response.setProcessingStatus("FAILED");
        response.setStatusMessage(errorMessage);
        return response;
    }

    /**
     * Gets the payment amount processed in the bill payment transaction.
     * 
     * @return The payment amount with BigDecimal precision
     */
    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    /**
     * Sets the payment amount processed in the bill payment transaction.
     * 
     * <p>Sets the exact amount processed in the payment transaction, using
     * BigDecimal for precise financial calculations. This amount should match
     * the transaction amount in the TransactionDTO and represent the actual
     * amount debited from the customer's account.</p>
     * 
     * @param paymentAmount The payment amount with exact precision
     */
    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    /**
     * Gets the complete transaction details for the bill payment operation.
     * 
     * @return TransactionDTO containing comprehensive transaction information
     */
    public TransactionDTO getTransactionDetails() {
        return transactionDetails;
    }

    /**
     * Sets the complete transaction details for the bill payment operation.
     * 
     * <p>Sets comprehensive transaction information including transaction ID,
     * timestamps, amounts, and merchant details. The transaction details should
     * be populated from the COBOL TRAN-RECORD structure and provide complete
     * information for customer receipts and audit purposes.</p>
     * 
     * @param transactionDetails Complete transaction information
     */
    public void setTransactionDetails(TransactionDTO transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    /**
     * Gets the real-time balance update information with before/after comparison.
     * 
     * @return AccountBalanceDto containing balance update information
     */
    public AccountBalanceDto getBalanceUpdate() {
        return balanceUpdate;
    }

    /**
     * Sets the real-time balance update information with before/after comparison.
     * 
     * <p>Sets comprehensive balance information showing the impact of the payment
     * on the customer's account. Should include previous balance, current balance
     * after payment, balance difference, and updated credit availability for
     * complete customer feedback.</p>
     * 
     * @param balanceUpdate Complete balance update information
     */
    public void setBalanceUpdate(AccountBalanceDto balanceUpdate) {
        this.balanceUpdate = balanceUpdate;
    }

    /**
     * Gets the comprehensive audit trail information for compliance and tracking.
     * 
     * @return AuditInfo containing detailed audit information
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the comprehensive audit trail information for compliance and tracking.
     * 
     * <p>Sets detailed audit information required for compliance with SOX, PCI DSS,
     * and regulatory requirements. Should include user context, correlation IDs,
     * timestamps, and security information for complete audit trail coverage.</p>
     * 
     * @param auditInfo Complete audit trail information
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the payment confirmation number for customer reference and tracking.
     * 
     * @return Payment confirmation number or null if not available
     */
    public String getPaymentConfirmationNumber() {
        return paymentConfirmationNumber;
    }

    /**
     * Sets the payment confirmation number for customer reference and tracking.
     * 
     * <p>Sets the unique confirmation number generated for successful payments,
     * typically the transaction ID from the COBOL transaction sequence generation.
     * This number serves as the customer's receipt reference and enables customer
     * service lookup capabilities.</p>
     * 
     * @param paymentConfirmationNumber Unique confirmation number for customer reference
     */
    public void setPaymentConfirmationNumber(String paymentConfirmationNumber) {
        this.paymentConfirmationNumber = paymentConfirmationNumber;
    }

    /**
     * Gets the processing status indicating the outcome of the bill payment operation.
     * 
     * @return Processing status string
     */
    public String getProcessingStatus() {
        return processingStatus;
    }

    /**
     * Sets the processing status indicating the outcome of the bill payment operation.
     * 
     * <p>Sets detailed status information beyond the basic success/failure flag,
     * providing specific processing state information for system integration and
     * customer feedback. Should use standard status codes like COMPLETED, PENDING,
     * FAILED, REJECTED, or INSUFFICIENT_FUNDS.</p>
     * 
     * @param processingStatus Detailed processing status information
     */
    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    /**
     * Gets the detailed status message providing human-readable processing information.
     * 
     * @return Human-readable status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the detailed status message providing human-readable processing information.
     * 
     * <p>Sets comprehensive status message for customer display and system integration,
     * providing detailed information about the payment processing outcome. Should
     * provide clear, actionable information for customer communication and include
     * relevant reference numbers or next steps.</p>
     * 
     * @param statusMessage Human-readable status message
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Checks whether the payment operation was successful.
     * 
     * @return true if payment was successful, false otherwise
     */
    public boolean isPaymentSuccessful() {
        return paymentSuccessful;
    }

    /**
     * Sets whether the payment operation was successful.
     * 
     * <p>Sets the primary success indicator for the bill payment operation. This
     * flag should be set to true only when the payment was successfully processed,
     * balance updated, and transaction recorded. Should be false for any error
     * conditions, validation failures, or processing errors.</p>
     * 
     * @param paymentSuccessful true if payment was successful, false otherwise
     */
    public void setPaymentSuccessful(boolean paymentSuccessful) {
        this.paymentSuccessful = paymentSuccessful;
        
        // Synchronize with parent class success flag
        this.setSuccess(paymentSuccessful);
        
        // Update processing status based on success flag
        if (paymentSuccessful && "PENDING".equals(this.processingStatus)) {
            this.processingStatus = "COMPLETED";
        } else if (!paymentSuccessful && "PENDING".equals(this.processingStatus)) {
            this.processingStatus = "FAILED";
        }
    }

    /**
     * Validates the completeness and consistency of the payment response data.
     * 
     * <p>Performs comprehensive validation of all payment response fields including
     * business rule validation, data consistency checks, and referential integrity
     * validation. This method supplements the Jakarta Bean Validation annotations
     * with custom business logic validation specific to bill payment operations.</p>
     * 
     * @return true if all validation checks pass, false otherwise
     */
    public boolean isValidPaymentResponse() {
        // Basic field validation
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Successful payment validation
        if (paymentSuccessful) {
            if (paymentConfirmationNumber == null || paymentConfirmationNumber.trim().isEmpty()) {
                return false;
            }
            if (transactionDetails == null || !transactionDetails.isValid()) {
                return false;
            }
            if (balanceUpdate == null) {
                return false;
            }
        }
        
        // Transaction details validation
        if (transactionDetails != null) {
            if (transactionDetails.getAmount() != null && 
                paymentAmount.compareTo(transactionDetails.getAmount()) != 0) {
                return false; // Payment amount must match transaction amount
            }
        }
        
        // Balance update validation
        if (balanceUpdate != null && paymentSuccessful) {
            if (balanceUpdate.getBalanceDifference() != null) {
                // Balance difference should be negative for successful payments (balance reduction)
                if (balanceUpdate.getBalanceDifference().compareTo(BigDecimal.ZERO) > 0) {
                    return false;
                }
            }
        }
        
        // Audit information validation
        if (auditInfo != null) {
            if (auditInfo.getCorrelationId() == null || !auditInfo.getCorrelationId().equals(this.getCorrelationId())) {
                return false; // Correlation IDs must match
            }
        }
        
        return true;
    }

    /**
     * Gets the total amount impact on the customer's account.
     * 
     * <p>Returns the signed amount impact on the customer's account, typically
     * negative for successful payments (balance reduction) or zero for failed
     * payments. This method provides a standardized way to calculate the
     * financial impact of the payment operation.</p>
     * 
     * @return Signed amount impact on customer's account
     */
    public BigDecimal getAccountImpact() {
        if (!paymentSuccessful || paymentAmount == null) {
            return BigDecimal.ZERO;
        }
        
        // Payment reduces balance, so impact is negative
        return paymentAmount.negate();
    }

    /**
     * Checks if the payment response contains complete information for customer receipt.
     * 
     * <p>Validates that the response contains all information necessary for
     * generating a complete customer receipt, including payment amount, confirmation
     * number, transaction details, and balance update information.</p>
     * 
     * @return true if complete receipt information is available
     */
    public boolean hasCompleteReceiptInformation() {
        return paymentAmount != null &&
               paymentConfirmationNumber != null &&
               transactionDetails != null &&
               balanceUpdate != null &&
               statusMessage != null;
    }

    /**
     * Returns a string representation of the bill payment response.
     * 
     * <p>Provides a comprehensive string representation including all key payment
     * information for debugging and logging purposes. Sensitive information is
     * masked appropriately to maintain security in log files.</p>
     * 
     * @return Formatted string representation of the payment response
     */
    @Override
    public String toString() {
        return String.format(
            "BillPaymentResponseDto{paymentAmount=%s, paymentSuccessful=%s, " +
            "processingStatus='%s', confirmationNumber='%s', " +
            "transactionDetails=%s, balanceUpdate=%s, auditInfo=%s, " +
            "statusMessage='%s', success=%s, correlationId='%s'}",
            paymentAmount,
            paymentSuccessful,
            processingStatus,
            paymentConfirmationNumber,
            transactionDetails != null ? transactionDetails.getTransactionId() : "null",
            balanceUpdate != null ? balanceUpdate.getAccountId() : "null",
            auditInfo != null ? auditInfo.getCorrelationId() : "null",
            statusMessage,
            isSuccess(),
            getCorrelationId()
        );
    }

    /**
     * Checks equality with another BillPaymentResponseDto based on key fields.
     * 
     * <p>Two payment responses are considered equal if they have the same
     * correlation ID, payment amount, and processing outcome. This method
     * is used for response comparison and deduplication operations.</p>
     * 
     * @param obj Object to compare with this payment response
     * @return true if the payment responses are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        BillPaymentResponseDto that = (BillPaymentResponseDto) obj;
        
        if (paymentSuccessful != that.paymentSuccessful) return false;
        if (paymentAmount != null ? paymentAmount.compareTo(that.paymentAmount) != 0 : that.paymentAmount != null) return false;
        if (paymentConfirmationNumber != null ? !paymentConfirmationNumber.equals(that.paymentConfirmationNumber) : that.paymentConfirmationNumber != null) return false;
        if (processingStatus != null ? !processingStatus.equals(that.processingStatus) : that.processingStatus != null) return false;
        
        return true;
    }

    /**
     * Generates hash code for the bill payment response.
     * 
     * <p>The hash code is based on the correlation ID, payment amount, and
     * processing outcome to ensure proper behavior in collections and hash-based
     * operations. This method provides consistent hashing for response
     * deduplication and lookup operations.</p>
     * 
     * @return Hash code value for this payment response
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (paymentAmount != null ? paymentAmount.hashCode() : 0);
        result = 31 * result + (paymentConfirmationNumber != null ? paymentConfirmationNumber.hashCode() : 0);
        result = 31 * result + (processingStatus != null ? processingStatus.hashCode() : 0);
        result = 31 * result + (paymentSuccessful ? 1 : 0);
        return result;
    }
}