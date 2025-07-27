/*
 * Copyright (c) 2024 CardDemo Application
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
 * This DTO serves as the complete response structure for bill payment transactions, providing
 * all necessary information for payment confirmation, audit trails, and balance updates.
 * It extends BaseResponseDto to inherit common response metadata while adding bill payment
 * specific data structures.
 * 
 * Key Features:
 * - Real-time balance update confirmation with before/after comparison
 * - Comprehensive transaction audit trail with correlation tracking
 * - Payment confirmation with unique reference numbers
 * - Processing status and detailed error information
 * - Full integration with COBOL COBIL00C.cbl payment logic patterns
 * 
 * COBOL Program Mapping:
 * This DTO maps to the successful completion response from COBIL00C.cbl which:
 * 1. Validates account existence and balance (READ-ACCTDAT-FILE)
 * 2. Confirms payment authorization (CONF-PAY-YES logic)
 * 3. Creates transaction record (WRITE-TRANSACT-FILE)
 * 4. Updates account balance to zero (UPDATE-ACCTDAT-FILE)
 * 5. Returns success message with transaction ID
 * 
 * The response includes all information necessary for the React frontend to display
 * payment confirmation equivalent to the COBOL success screen output with:
 * - "Payment successful. Your Transaction ID is {TRAN-ID}."
 * - Updated current balance display
 * - Transaction details for receipt generation
 * 
 * Data Precision:
 * All monetary amounts maintain exact BigDecimal precision equivalent to COBOL COMP-3
 * arithmetic operations, ensuring identical financial calculation results across
 * the modernized system architecture.
 * 
 * @author Blitzy Agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillPaymentResponseDto extends BaseResponseDto {

    /**
     * Payment amount that was processed - corresponds to TRAN-AMT in COBOL transaction record.
     * 
     * This represents the exact amount paid, which in COBIL00C.cbl is the full current
     * account balance (ACCT-CURR-BAL). The amount uses BigDecimal with COBOL COMP-3
     * precision (PIC S9(09)V99) to ensure exact financial arithmetic matching the
     * original mainframe calculations.
     * 
     * The payment amount is captured when the transaction record is created:
     * MOVE ACCT-CURR-BAL TO TRAN-AMT
     * 
     * After successful payment, the account balance is zeroed:
     * COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT
     */
    @NotNull(message = "Payment amount is required for bill payment confirmation")
    private BigDecimal paymentAmount;

    /**
     * Complete transaction details including transaction ID, timestamps, and merchant information.
     * 
     * Maps to the TRAN-RECORD structure created in COBIL00C.cbl with the following key fields:
     * - TRAN-ID: Generated transaction identifier (WS-TRAN-ID-NUM + 1)
     * - TRAN-TYPE-CD: Set to '02' for bill payment transactions
     * - TRAN-CAT-CD: Set to 2 for payment category
     * - TRAN-SOURCE: Set to 'POS TERM' indicating point-of-sale terminal
     * - TRAN-DESC: Set to 'BILL PAYMENT - ONLINE'
     * - TRAN-AMT: Current account balance being paid
     * - TRAN-CARD-NUM: Associated card number from XREF-CARD-NUM
     * - TRAN-MERCHANT-ID: Set to 999999999 for bill payment
     * - TRAN-MERCHANT-NAME: Set to 'BILL PAYMENT'
     * - TRAN-ORIG-TS and TRAN-PROC-TS: Current timestamp
     * 
     * This provides complete transaction documentation for payment receipt generation
     * and regulatory compliance requirements.
     */
    @Valid
    @NotNull(message = "Transaction details are required for payment confirmation")
    private TransactionDTO transactionDetails;

    /**
     * Account balance information showing before/after payment comparison.
     * 
     * Provides comprehensive balance update confirmation with:
     * - Previous balance: The amount that was owed before payment (ACCT-CURR-BAL before update)
     * - Current balance: Always zero after successful bill payment (ACCT-CURR-BAL after update)
     * - Balance difference: The payment amount (negative indicating payment/credit)
     * - Available credit: Updated available credit after payment
     * 
     * This corresponds to the balance update logic in COBIL00C.cbl:
     * MOVE ACCT-CURR-BAL TO WS-CURR-BAL (capture original balance)
     * COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - TRAN-AMT (zero the balance)
     * 
     * Real-time balance updates ensure immediate reflection of payment processing
     * for customer account management and credit availability calculations.
     */
    @Valid
    @NotNull(message = "Balance update information is required for payment confirmation")
    private AccountBalanceDto balanceUpdate;

    /**
     * Comprehensive audit trail information for compliance and tracking purposes.
     * 
     * Captures complete audit context including:
     * - User ID: Authenticated user performing the payment
     * - Operation type: 'BILL_PAYMENT' for audit classification
     * - Timestamp: Exact payment processing time
     * - Correlation ID: Request tracking across microservices
     * - Session ID: User session for security analysis
     * - Source system: 'BillPaymentService' identifier
     * - IP address: Client IP for fraud prevention
     * - User agent: Browser/client information
     * 
     * Supports SOX compliance, PCI DSS requirements, and comprehensive security
     * audit trails for regulatory examination and incident investigation.
     * Essential for tracking payment authorization and processing activities.
     */
    @Valid
    @NotNull(message = "Audit information is required for compliance tracking")
    private AuditInfo auditInfo;

    /**
     * Unique payment confirmation number for customer reference and tracking.
     * 
     * Generated confirmation number combining transaction ID with additional
     * verification digits for customer service reference. Format typically:
     * BP-{TRAN-ID}-{CHECKSUM} where:
     * - BP indicates Bill Payment
     * - TRAN-ID: Transaction identifier from COBOL (WS-TRAN-ID-NUM)
     * - CHECKSUM: Verification digits for fraud prevention
     * 
     * This confirmation number is provided to customers for:
     * - Payment receipt documentation
     * - Customer service inquiries
     * - Dispute resolution reference
     * - Regulatory audit trail correlation
     * 
     * Maps to the success message in COBIL00C.cbl:
     * "Payment successful. Your Transaction ID is {TRAN-ID}."
     */
    @NotNull(message = "Payment confirmation number is required for customer reference")
    private String paymentConfirmationNumber;

    /**
     * Processing status indicating the outcome of payment processing.
     * 
     * Status values:
     * - COMPLETED: Payment successfully processed and account updated
     * - PENDING: Payment submitted but awaiting final processing
     * - FAILED: Payment processing failed due to business rules
     * - ERROR: System error during payment processing
     * - CANCELLED: Payment cancelled by user or system
     * 
     * For successful COBIL00C.cbl processing, status is always COMPLETED
     * when the WRITE-TRANSACT-FILE and UPDATE-ACCTDAT-FILE operations
     * complete successfully without CICS response code errors.
     * 
     * Status drives frontend display logic and customer communication.
     */
    @NotNull(message = "Processing status is required for payment workflow")
    private String processingStatus;

    /**
     * Detailed status message providing human-readable payment result information.
     * 
     * Contains descriptive text about payment processing outcome:
     * - Success: "Payment successful. Your Transaction ID is {ID}."
     * - Validation errors: Account-specific error messages from COBOL logic
     * - System errors: Technical error descriptions for troubleshooting
     * 
     * Maps directly to WS-MESSAGE variable in COBIL00C.cbl which contains:
     * - Success messages built using STRING command with transaction ID
     * - Error messages for various validation failures:
     *   - "Acct ID can NOT be empty..."
     *   - "Account ID NOT found..."
     *   - "You have nothing to pay..."
     *   - "Invalid value. Valid values are (Y/N)..."
     * 
     * Provides consistent user feedback matching original COBOL screen messages.
     */
    private String statusMessage;

    /**
     * Boolean flag indicating overall payment success for client processing logic.
     * 
     * Simple true/false indicator for payment completion:
     * - true: Payment processed successfully, account balance updated, transaction recorded
     * - false: Payment failed due to validation errors, system errors, or business rules
     * 
     * Corresponds to the ERR-FLG-OFF condition in COBIL00C.cbl indicating
     * successful completion of all payment processing steps without errors.
     * When ERR-FLG-OFF is true, the payment workflow completes successfully.
     * 
     * Used by React frontend for:
     * - Conditional rendering of success/error UI components
     * - Navigation flow control after payment attempts
     * - Analytics tracking of payment success rates
     */
    private boolean paymentSuccessful;

    /**
     * Default constructor initializing payment response with base response metadata.
     * 
     * Sets up the response with automatic timestamp generation and default success status.
     * Initializes all monetary amounts to zero with proper BigDecimal scale for
     * financial precision equivalent to COBOL COMP-3 arithmetic.
     */
    public BillPaymentResponseDto() {
        super();
        this.paymentAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN);
        this.paymentSuccessful = false; // Default to false until explicitly set to true
        this.processingStatus = "PENDING"; // Default status until processing completes
    }

    /**
     * Constructor for successful payment responses with essential information.
     * 
     * Creates a successful payment response with correlation tracking and
     * core payment information. Used when payment processing completes
     * successfully and all required information is available.
     * 
     * @param paymentAmount Amount that was successfully paid
     * @param transactionDetails Complete transaction information
     * @param balanceUpdate Account balance before/after comparison
     * @param confirmationNumber Unique payment confirmation reference
     * @param correlationId Request correlation identifier
     */
    public BillPaymentResponseDto(BigDecimal paymentAmount, TransactionDTO transactionDetails,
                                 AccountBalanceDto balanceUpdate, String confirmationNumber,
                                 String correlationId) {
        super(correlationId);
        this.paymentAmount = paymentAmount != null ? 
            paymentAmount.setScale(2, java.math.RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN);
        this.transactionDetails = transactionDetails;
        this.balanceUpdate = balanceUpdate;
        this.paymentConfirmationNumber = confirmationNumber;
        this.paymentSuccessful = true;
        this.processingStatus = "COMPLETED";
        this.statusMessage = String.format("Payment successful. Your Transaction ID is %s.", 
            transactionDetails != null ? transactionDetails.getTransactionId() : confirmationNumber);
    }

    /**
     * Constructor for error payment responses with detailed failure information.
     * 
     * Creates an error response for failed payment processing with comprehensive
     * error context and correlation tracking. Used when payment validation
     * fails or system errors occur during processing.
     * 
     * @param errorMessage Detailed error description
     * @param processingStatus Specific processing status (FAILED/ERROR/CANCELLED)
     * @param correlationId Request correlation identifier
     */
    public BillPaymentResponseDto(String errorMessage, String processingStatus, String correlationId) {
        super(errorMessage, correlationId);
        this.paymentAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN);
        this.paymentSuccessful = false;
        this.processingStatus = processingStatus != null ? processingStatus : "FAILED";
        this.statusMessage = errorMessage;
    }

    /**
     * Gets the payment amount that was processed.
     * 
     * @return BigDecimal payment amount with exact decimal precision
     */
    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    /**
     * Sets the payment amount with proper financial precision.
     * 
     * Ensures the amount is set with proper scale (2 decimal places) and rounding
     * to maintain COBOL COMP-3 arithmetic equivalence for financial calculations.
     * 
     * @param paymentAmount Amount that was paid (must be positive for successful payments)
     */
    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount != null ? 
            paymentAmount.setScale(2, java.math.RoundingMode.HALF_EVEN) : 
            BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    /**
     * Gets the complete transaction details for the payment.
     * 
     * @return TransactionDTO containing full transaction information
     */
    public TransactionDTO getTransactionDetails() {
        return transactionDetails;
    }

    /**
     * Sets the transaction details for the payment.
     * 
     * @param transactionDetails Complete transaction information including ID, timestamps, amounts
     */
    public void setTransactionDetails(TransactionDTO transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    /**
     * Gets the account balance update information showing before/after comparison.
     * 
     * @return AccountBalanceDto with previous and current balance information
     */
    public AccountBalanceDto getBalanceUpdate() {
        return balanceUpdate;
    }

    /**
     * Sets the account balance update information.
     * 
     * @param balanceUpdate Balance information with before/after payment comparison
     */
    public void setBalanceUpdate(AccountBalanceDto balanceUpdate) {
        this.balanceUpdate = balanceUpdate;
    }

    /**
     * Gets the comprehensive audit information for compliance tracking.
     * 
     * @return AuditInfo containing user context, timestamps, and correlation data
     */
    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    /**
     * Sets the audit information for compliance and tracking purposes.
     * 
     * @param auditInfo Complete audit context including user, session, and system information
     */
    public void setAuditInfo(AuditInfo auditInfo) {
        this.auditInfo = auditInfo;
    }

    /**
     * Gets the unique payment confirmation number for customer reference.
     * 
     * @return String confirmation number for customer service and tracking
     */
    public String getPaymentConfirmationNumber() {
        return paymentConfirmationNumber;
    }

    /**
     * Sets the payment confirmation number.
     * 
     * @param paymentConfirmationNumber Unique confirmation reference for the payment
     */
    public void setPaymentConfirmationNumber(String paymentConfirmationNumber) {
        this.paymentConfirmationNumber = paymentConfirmationNumber;
    }

    /**
     * Gets the processing status of the payment operation.
     * 
     * @return String status indicating payment processing outcome
     */
    public String getProcessingStatus() {
        return processingStatus;
    }

    /**
     * Sets the processing status with automatic success flag management.
     * 
     * Automatically updates the payment success flag based on the processing status.
     * COMPLETED status sets success to true, while other statuses set success to false.
     * 
     * @param processingStatus Status indicating payment processing result
     */
    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
        // Automatically update success flag based on processing status
        this.paymentSuccessful = "COMPLETED".equalsIgnoreCase(processingStatus);
    }

    /**
     * Gets the detailed status message for the payment operation.
     * 
     * @return String message providing human-readable payment result information
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the status message for the payment operation.
     * 
     * @param statusMessage Human-readable message describing payment processing outcome
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Checks if the payment was processed successfully.
     * 
     * @return true if payment completed successfully, false otherwise
     */
    public boolean isPaymentSuccessful() {
        return paymentSuccessful;
    }

    /**
     * Sets the payment success flag with automatic status message management.
     * 
     * When setting success to false, automatically updates the base response
     * error status to ensure consistent response state management.
     * 
     * @param paymentSuccessful true for successful payments, false for failed payments
     */
    public void setPaymentSuccessful(boolean paymentSuccessful) {
        this.paymentSuccessful = paymentSuccessful;
        // Ensure base response success flag is consistent
        if (!paymentSuccessful && isSuccess()) {
            setSuccess(false);
        }
    }

    /**
     * Utility method to create a successful payment response with all required information.
     * 
     * Factory method providing fluent API for creating successful payment responses
     * with automatic confirmation number generation and status message formatting.
     * 
     * @param paymentAmount Amount that was successfully paid
     * @param transactionDetails Complete transaction information
     * @param balanceUpdate Account balance update information
     * @param auditInfo Comprehensive audit trail information
     * @param correlationId Request correlation identifier
     * @return BillPaymentResponseDto configured for successful payment
     */
    public static BillPaymentResponseDto success(BigDecimal paymentAmount, 
                                               TransactionDTO transactionDetails,
                                               AccountBalanceDto balanceUpdate, 
                                               AuditInfo auditInfo,
                                               String correlationId) {
        String confirmationNumber = generateConfirmationNumber(
            transactionDetails != null ? transactionDetails.getTransactionId() : null);
        
        BillPaymentResponseDto response = new BillPaymentResponseDto(
            paymentAmount, transactionDetails, balanceUpdate, confirmationNumber, correlationId);
        
        response.setAuditInfo(auditInfo);
        return response;
    }

    /**
     * Utility method to create an error payment response with detailed failure information.
     * 
     * Factory method providing fluent API for creating error payment responses
     * with proper error context and correlation tracking for debugging and audit purposes.
     * 
     * @param errorMessage Detailed error description
     * @param processingStatus Specific processing status for the failure
     * @param auditInfo Audit information for error tracking
     * @param correlationId Request correlation identifier
     * @return BillPaymentResponseDto configured for payment failure
     */
    public static BillPaymentResponseDto error(String errorMessage, String processingStatus,
                                             AuditInfo auditInfo, String correlationId) {
        BillPaymentResponseDto response = new BillPaymentResponseDto(errorMessage, processingStatus, correlationId);
        response.setAuditInfo(auditInfo);
        return response;
    }

    /**
     * Generates a unique payment confirmation number based on transaction ID.
     * 
     * Creates a formatted confirmation number combining bill payment prefix
     * with transaction identifier and verification checksum for customer reference.
     * 
     * @param transactionId Base transaction identifier
     * @return Formatted confirmation number (e.g., "BP-1234567890123456-89")
     */
    private static String generateConfirmationNumber(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return "BP-" + System.currentTimeMillis() + "-00";
        }
        
        // Generate simple checksum for verification
        int checksum = transactionId.hashCode() % 100;
        if (checksum < 0) checksum = -checksum;
        
        return String.format("BP-%s-%02d", transactionId, checksum);
    }

    /**
     * Validates that all required fields are present for a successful payment response.
     * 
     * Comprehensive validation ensuring payment response contains all necessary
     * information for customer confirmation, audit trails, and regulatory compliance.
     * 
     * @return true if all required fields are valid and present
     */
    public boolean isCompleteResponse() {
        return paymentAmount != null && 
               paymentAmount.compareTo(BigDecimal.ZERO) > 0 &&
               transactionDetails != null && 
               transactionDetails.isValid() &&
               balanceUpdate != null &&
               auditInfo != null &&
               paymentConfirmationNumber != null && 
               !paymentConfirmationNumber.trim().isEmpty() &&
               processingStatus != null && 
               !processingStatus.trim().isEmpty();
    }

    /**
     * Returns formatted string representation of the payment response for logging and debugging.
     * 
     * Provides comprehensive string representation including key payment information
     * while maintaining security by masking sensitive details. Essential for
     * debugging payment workflows and audit trail generation.
     * 
     * @return Formatted string containing key payment response information
     */
    @Override
    public String toString() {
        return String.format(
            "BillPaymentResponseDto{success=%s, paymentAmount=%s, confirmationNumber='%s', " +
            "processingStatus='%s', paymentSuccessful=%s, transactionId='%s', correlationId='%s'}",
            isSuccess(), paymentAmount, paymentConfirmationNumber, processingStatus, 
            paymentSuccessful, 
            transactionDetails != null ? transactionDetails.getTransactionId() : "null",
            getCorrelationId()
        );
    }

    /**
     * Equals method for payment response comparison in tests and business logic.
     * 
     * Implements proper equality comparison based on payment confirmation number
     * and correlation ID as unique identifiers for payment responses.
     * 
     * @param obj Object to compare
     * @return true if objects represent the same payment response
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        BillPaymentResponseDto that = (BillPaymentResponseDto) obj;
        
        return paymentSuccessful == that.paymentSuccessful &&
               java.util.Objects.equals(paymentConfirmationNumber, that.paymentConfirmationNumber) &&
               java.util.Objects.equals(processingStatus, that.processingStatus) &&
               compareBigDecimals(paymentAmount, that.paymentAmount);
    }

    /**
     * Hash code method for payment response consistent with equals implementation.
     * 
     * @return computed hash code for the payment response
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), paymentAmount, paymentConfirmationNumber, 
                                    processingStatus, paymentSuccessful);
    }

    /**
     * Helper method for BigDecimal comparison in equals method.
     * 
     * @param bd1 First BigDecimal to compare
     * @param bd2 Second BigDecimal to compare
     * @return true if BigDecimal values are equal
     */
    private boolean compareBigDecimals(BigDecimal bd1, BigDecimal bd2) {
        if (bd1 == null && bd2 == null) return true;
        if (bd1 == null || bd2 == null) return false;
        return bd1.compareTo(bd2) == 0;
    }
}