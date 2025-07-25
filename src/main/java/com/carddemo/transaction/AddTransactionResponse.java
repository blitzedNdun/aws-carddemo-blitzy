package com.carddemo.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Response DTO for transaction addition operations providing comprehensive confirmation 
 * details and status reporting.
 * 
 * This response DTO maintains exact correspondence with the COBOL COTRN02C transaction
 * processing program COMMAREA response structure, ensuring complete compatibility with
 * legacy transaction flow patterns while supporting modern REST API responses.
 * 
 * The response structure supports both success confirmations (with complete transaction 
 * details, balance updates, and confirmation timestamps) and detailed error reporting 
 * (with field-level validation failures and categorized error codes) equivalent to the
 * original CICS COMMAREA response patterns.
 * 
 * Key Features:
 * - Success/failure status reporting with detailed messages
 * - Generated transaction ID return for successful operations
 * - Confirmation timestamp for audit trail and user feedback
 * - Complete transaction details via embedded TransactionDTO
 * - Balance update confirmation (previous and current balance)
 * - HTTP status code mapping for REST API compliance
 * - Detailed validation error reporting for field-level failures
 * - Error categorization codes for systematic error handling
 * 
 * COBOL Program Correspondence:
 * - WS-ERR-FLG → success field (inverted logic: ERR-FLG-OFF = success = true)
 * - WS-MESSAGE → message field (user-facing status messages)
 * - TRAN-ID → transactionId field (newly generated transaction ID)
 * - Success message pattern → formatted confirmation messages
 * - Field validation errors → validationErrors list
 * - Account balance operations → previousBalance/currentBalance fields
 * 
 * @author Blitzy Agent
 * @version CardDemo_v1.0-15-g27d6c6f-68
 * @since 1.0
 */
public class AddTransactionResponse {

    /**
     * Indicates whether the transaction addition operation was successful.
     * Maps to inverted COBOL WS-ERR-FLG logic where ERR-FLG-OFF means success.
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * User-facing message providing operation status and details.
     * Corresponds to COBOL WS-MESSAGE field for user feedback.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Generated transaction identifier for successful operations.
     * Maps to COBOL TRAN-ID field assigned during ADD-TRANSACTION paragraph.
     */
    @JsonProperty("transaction_id")
    private String transactionId;

    /**
     * Timestamp when the transaction confirmation was generated.
     * Provides audit trail and user feedback for operation completion time.
     */
    @JsonProperty("confirmation_timestamp")
    private LocalDateTime confirmationTimestamp;

    /**
     * Complete transaction details for successful operations.
     * Includes all transaction data equivalent to COBOL TRAN-RECORD structure.
     */
    @JsonProperty("transaction")
    private TransactionDTO transaction;

    /**
     * Account balance before the transaction processing.
     * Provides balance confirmation and audit trail for transaction impact.
     */
    @JsonProperty("previous_balance")
    private BigDecimal previousBalance;

    /**
     * Account balance after the transaction processing.
     * Confirms successful balance update and provides current account state.
     */
    @JsonProperty("current_balance")
    private BigDecimal currentBalance;

    /**
     * HTTP status code for REST API response classification.
     * Enables proper HTTP response handling in client applications.
     */
    @JsonProperty("http_status")
    private int httpStatus;

    /**
     * Error categorization code for systematic error handling.
     * Provides structured error classification beyond simple success/failure.
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * Detailed validation error messages for field-level failures.
     * Equivalent to COBOL field validation error handling with cursor positioning.
     */
    @JsonProperty("validation_errors")
    private List<String> validationErrors;

    /**
     * Default constructor for AddTransactionResponse.
     * Initializes response with default values and empty validation error list.
     */
    public AddTransactionResponse() {
        this.validationErrors = new ArrayList<>();
        this.confirmationTimestamp = LocalDateTime.now();
        this.success = false;
        this.httpStatus = 200;
    }

    /**
     * Constructor for successful transaction addition response.
     * Creates a success response with transaction details and balance confirmation.
     * 
     * @param transactionId Generated transaction identifier
     * @param transaction Complete transaction details
     * @param previousBalance Account balance before transaction
     * @param currentBalance Account balance after transaction
     * @param message Success confirmation message
     */
    public AddTransactionResponse(String transactionId, TransactionDTO transaction, 
                                BigDecimal previousBalance, BigDecimal currentBalance, 
                                String message) {
        this();
        this.success = true;
        this.transactionId = transactionId;
        this.transaction = transaction;
        this.previousBalance = previousBalance;
        this.currentBalance = currentBalance;
        this.message = message;
        this.httpStatus = 201; // Created
        this.confirmationTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for error response with validation failures.
     * Creates an error response with detailed validation error information.
     * 
     * @param message Error message for user feedback
     * @param errorCode Categorized error code
     * @param validationErrors List of field validation errors
     * @param httpStatus HTTP status code for the error
     */
    public AddTransactionResponse(String message, String errorCode, 
                                List<String> validationErrors, int httpStatus) {
        this();
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
        this.httpStatus = httpStatus;
        this.confirmationTimestamp = LocalDateTime.now();
    }

    /**
     * Get success status of the transaction addition operation.
     * 
     * @return true if transaction was added successfully, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Set success status of the transaction addition operation.
     * 
     * @param success true for successful operation, false for failure
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get user-facing message for operation status.
     * 
     * @return String message providing operation feedback
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set user-facing message for operation status.
     * 
     * @param message Operation status message for user feedback
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get generated transaction identifier.
     * 
     * @return String transaction ID for successful operations, null for failures
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Set generated transaction identifier.
     * 
     * @param transactionId Unique transaction identifier
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Get confirmation timestamp for the operation.
     * 
     * @return LocalDateTime when the response was generated
     */
    public LocalDateTime getConfirmationTimestamp() {
        return confirmationTimestamp;
    }

    /**
     * Set confirmation timestamp for the operation.
     * 
     * @param confirmationTimestamp Timestamp of operation completion
     */
    public void setConfirmationTimestamp(LocalDateTime confirmationTimestamp) {
        this.confirmationTimestamp = confirmationTimestamp;
    }

    /**
     * Get complete transaction details.
     * 
     * @return TransactionDTO with full transaction information, null for failed operations
     */
    public TransactionDTO getTransaction() {
        return transaction;
    }

    /**
     * Set complete transaction details.
     * 
     * @param transaction Complete transaction data transfer object
     */
    public void setTransaction(TransactionDTO transaction) {
        this.transaction = transaction;
    }

    /**
     * Get account balance before transaction processing.
     * 
     * @return BigDecimal previous balance with COBOL COMP-3 precision
     */
    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Set account balance before transaction processing.
     * 
     * @param previousBalance Account balance before transaction
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance;
    }

    /**
     * Get account balance after transaction processing.
     * 
     * @return BigDecimal current balance with COBOL COMP-3 precision
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Set account balance after transaction processing.
     * 
     * @param currentBalance Account balance after transaction
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    /**
     * Get HTTP status code for REST API response.
     * 
     * @return int HTTP status code (200, 201, 400, 500, etc.)
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Set HTTP status code for REST API response.
     * 
     * @param httpStatus HTTP response status code
     */
    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Get error categorization code.
     * 
     * @return String error code for systematic error handling
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Set error categorization code.
     * 
     * @param errorCode Categorized error code for error classification
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Get list of validation error messages.
     * 
     * @return List<String> field-level validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Set list of validation error messages.
     * 
     * @param validationErrors List of field validation errors
     */
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
    }

    /**
     * Add a single validation error to the errors list.
     * Convenience method for building validation error responses.
     * 
     * @param validationError Single field validation error message
     */
    public void addValidationError(String validationError) {
        if (this.validationErrors == null) {
            this.validationErrors = new ArrayList<>();
        }
        this.validationErrors.add(validationError);
    }

    /**
     * Check if the response has validation errors.
     * 
     * @return true if validation errors exist, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Calculate balance change from the transaction.
     * Provides the net change in account balance.
     * 
     * @return BigDecimal balance change (current - previous), null if balances not set
     */
    public BigDecimal getBalanceChange() {
        if (previousBalance != null && currentBalance != null) {
            return currentBalance.subtract(previousBalance);
        }
        return null;
    }

    /**
     * Check if this is a success response with complete transaction data.
     * 
     * @return true if successful with transaction details, false otherwise
     */
    public boolean isCompleteSuccess() {
        return success && transactionId != null && transaction != null;
    }

    /**
     * Create a formatted success message with transaction ID.
     * Matches the COBOL success message pattern from WRITE-TRANSACT-FILE paragraph.
     * 
     * @return String formatted success message with transaction ID
     */
    public String getFormattedSuccessMessage() {
        if (success && transactionId != null) {
            return String.format("Transaction added successfully. Your Tran ID is %s.", transactionId);
        }
        return message;
    }

    /**
     * Create a builder for constructing AddTransactionResponse instances.
     * Provides fluent API for response construction.
     * 
     * @return Builder instance for fluent response construction
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for fluent AddTransactionResponse construction.
     * Enables clean and readable response building in service classes.
     */
    public static class Builder {
        private AddTransactionResponse response;

        private Builder() {
            this.response = new AddTransactionResponse();
        }

        public Builder success(boolean success) {
            response.setSuccess(success);
            return this;
        }

        public Builder message(String message) {
            response.setMessage(message);
            return this;
        }

        public Builder transactionId(String transactionId) {
            response.setTransactionId(transactionId);
            return this;
        }

        public Builder transaction(TransactionDTO transaction) {
            response.setTransaction(transaction);
            return this;
        }

        public Builder previousBalance(BigDecimal previousBalance) {
            response.setPreviousBalance(previousBalance);
            return this;
        }

        public Builder currentBalance(BigDecimal currentBalance) {
            response.setCurrentBalance(currentBalance);
            return this;
        }

        public Builder httpStatus(int httpStatus) {
            response.setHttpStatus(httpStatus);
            return this;
        }

        public Builder errorCode(String errorCode) {
            response.setErrorCode(errorCode);
            return this;
        }

        public Builder validationErrors(List<String> validationErrors) {
            response.setValidationErrors(validationErrors);
            return this;
        }

        public Builder addValidationError(String validationError) {
            response.addValidationError(validationError);
            return this;
        }

        public AddTransactionResponse build() {
            return response;
        }
    }

    /**
     * String representation of AddTransactionResponse for logging and debugging.
     * Includes key response details while preserving data privacy.
     * 
     * @return String representation with key response information
     */
    @Override
    public String toString() {
        return String.format(
            "AddTransactionResponse{success=%s, transactionId='%s', httpStatus=%d, message='%s', hasValidationErrors=%s}",
            success, transactionId, httpStatus, message, hasValidationErrors()
        );
    }
}