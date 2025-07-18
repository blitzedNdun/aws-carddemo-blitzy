package com.carddemo.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Response DTO for transaction addition operations providing comprehensive status reporting 
 * and confirmation details with complete audit trail and user feedback capabilities.
 * 
 * This response DTO maintains complete functional equivalence with the original COBOL 
 * COTRN02C program response patterns, ensuring seamless migration from mainframe 
 * transaction processing to modern Spring Boot REST API architecture.
 * 
 * Key Features:
 * - Complete transaction confirmation with generated transaction ID
 * - Account balance updates with before/after audit trail
 * - Comprehensive error reporting with field-level validation details
 * - HTTP status code mapping for proper REST API response handling
 * - Jackson JSON serialization for consistent React frontend integration
 * - Audit timestamp tracking for compliance and monitoring
 * 
 * Response Patterns from COBOL COTRN02C:
 * - Success: "Transaction added successfully. Your Tran ID is {id}."
 * - Validation errors with specific field-level messages and cursor positioning
 * - Account/Card lookup errors with appropriate error codes
 * - Data format validation errors with clear user guidance
 * - Confirmation requirements with Y/N validation
 * 
 * Field Mapping to COBOL Response Variables:
 * - success → ERR-FLG-OFF condition (inverted logic)
 * - message → WS-MESSAGE content
 * - transactionId → TRAN-ID generated value
 * - confirmationTimestamp → system timestamp equivalent
 * - transaction → complete TRAN-RECORD structure
 * - previousBalance/currentBalance → account balance audit trail
 * - httpStatus → HTTP response code mapping
 * - errorCode → CICS RESP-CD equivalent
 * - validationErrors → field-level error collection
 * 
 * Performance Requirements:
 * - Supports 10,000+ TPS transaction processing with sub-200ms response times
 * - Optimized JSON serialization for high-volume API operations
 * - Memory-efficient structure suitable for microservices architecture
 * - Integration with Spring Boot monitoring and observability
 * 
 * Technical Implementation:
 * - JSON property naming consistent with React frontend expectations
 * - BigDecimal precision for exact financial calculations
 * - LocalDateTime for precise audit timestamp requirements
 * - List-based validation error collection for comprehensive feedback
 * - HTTP status code integration for proper REST API semantics
 * 
 * Based on COBOL sources:
 * - COTRN02C.cbl: Transaction addition program with validation patterns
 * - COCOM01Y.cpy: Common communication area structure
 * - CSMSG01Y.cpy: Message handling copybook
 * - CVTRA05Y.cpy: Transaction record structure definition
 * 
 * @author Blitzy Platform
 * @version 1.0
 * @since 2024-01-01
 */
public class AddTransactionResponse {

    /**
     * Success indicator - true if transaction was successfully added
     * Maps to inverted COBOL ERR-FLG-OFF condition
     * Controls overall response flow and client-side processing
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * Primary status message providing user feedback
     * Maps to COBOL WS-MESSAGE variable content
     * Contains success confirmation or primary error description
     */
    @JsonProperty("message")
    private String message;

    /**
     * Generated transaction ID for successful transactions
     * Maps to COBOL TRAN-ID field (16 characters)
     * Unique identifier for audit trail and user reference
     */
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Timestamp when transaction confirmation was generated
     * Provides precise audit trail for transaction completion
     * Used for compliance reporting and monitoring
     */
    @JsonProperty("confirmationTimestamp")
    private LocalDateTime confirmationTimestamp;

    /**
     * Complete transaction details for user confirmation
     * Maps to COBOL TRAN-RECORD structure
     * Includes all transaction fields for verification
     */
    @JsonProperty("transaction")
    private TransactionDTO transaction;

    /**
     * Account balance before transaction processing
     * Maps to COBOL account balance inquiry result
     * Provides audit trail for balance verification
     */
    @JsonProperty("previousBalance")
    private BigDecimal previousBalance;

    /**
     * Account balance after transaction processing
     * Maps to COBOL updated account balance
     * Confirms successful balance update
     */
    @JsonProperty("currentBalance")
    private BigDecimal currentBalance;

    /**
     * HTTP status code for REST API response
     * Maps transaction processing result to proper HTTP semantics
     * Enables proper client-side error handling
     */
    @JsonProperty("httpStatus")
    private Integer httpStatus;

    /**
     * System error code for technical error classification
     * Maps to COBOL RESP-CD/REAS-CD values
     * Provides detailed error classification for debugging
     */
    @JsonProperty("errorCode")
    private String errorCode;

    /**
     * Collection of field-level validation errors
     * Maps to COBOL field-level error processing
     * Provides comprehensive validation feedback
     */
    @JsonProperty("validationErrors")
    private List<String> validationErrors;

    /**
     * Default constructor for AddTransactionResponse
     */
    public AddTransactionResponse() {
        this.confirmationTimestamp = LocalDateTime.now();
        this.success = false;
        this.httpStatus = 500; // Default to internal server error
    }

    /**
     * Constructor for successful transaction responses
     * 
     * @param transactionId generated transaction ID
     * @param transaction complete transaction details
     * @param previousBalance account balance before transaction
     * @param currentBalance account balance after transaction
     */
    public AddTransactionResponse(String transactionId, TransactionDTO transaction, 
                                 BigDecimal previousBalance, BigDecimal currentBalance) {
        this();
        this.success = true;
        this.transactionId = transactionId;
        this.transaction = transaction;
        this.previousBalance = previousBalance;
        this.currentBalance = currentBalance;
        this.httpStatus = 201; // Created
        this.message = String.format("Transaction added successfully. Your Tran ID is %s.", transactionId);
    }

    /**
     * Constructor for error responses
     * 
     * @param message error message
     * @param errorCode system error code
     * @param httpStatus HTTP status code
     */
    public AddTransactionResponse(String message, String errorCode, Integer httpStatus) {
        this();
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Constructor for validation error responses
     * 
     * @param message primary error message
     * @param validationErrors list of validation errors
     * @param httpStatus HTTP status code
     */
    public AddTransactionResponse(String message, List<String> validationErrors, Integer httpStatus) {
        this();
        this.success = false;
        this.message = message;
        this.validationErrors = validationErrors;
        this.httpStatus = httpStatus;
    }

    /**
     * Get the success indicator
     * 
     * @return true if transaction was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Set the success indicator
     * 
     * @param success true if transaction was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get the primary status message
     * 
     * @return status message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the primary status message
     * 
     * @param message status message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the generated transaction ID
     * 
     * @return transaction ID string
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Set the generated transaction ID
     * 
     * @param transactionId transaction ID to set
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Get the confirmation timestamp
     * 
     * @return LocalDateTime of confirmation
     */
    public LocalDateTime getConfirmationTimestamp() {
        return confirmationTimestamp;
    }

    /**
     * Set the confirmation timestamp
     * 
     * @param confirmationTimestamp LocalDateTime to set
     */
    public void setConfirmationTimestamp(LocalDateTime confirmationTimestamp) {
        this.confirmationTimestamp = confirmationTimestamp;
    }

    /**
     * Get the complete transaction details
     * 
     * @return TransactionDTO with complete transaction information
     */
    public TransactionDTO getTransaction() {
        return transaction;
    }

    /**
     * Set the complete transaction details
     * 
     * @param transaction TransactionDTO to set
     */
    public void setTransaction(TransactionDTO transaction) {
        this.transaction = transaction;
    }

    /**
     * Get the account balance before transaction
     * 
     * @return BigDecimal previous balance
     */
    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    /**
     * Set the account balance before transaction
     * 
     * @param previousBalance BigDecimal previous balance to set
     */
    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance;
    }

    /**
     * Get the account balance after transaction
     * 
     * @return BigDecimal current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Set the account balance after transaction
     * 
     * @param currentBalance BigDecimal current balance to set
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    /**
     * Get the HTTP status code
     * 
     * @return Integer HTTP status code
     */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /**
     * Set the HTTP status code
     * 
     * @param httpStatus Integer HTTP status code to set
     */
    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Get the system error code
     * 
     * @return error code string
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Set the system error code
     * 
     * @param errorCode error code string to set
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Get the list of validation errors
     * 
     * @return List of validation error strings
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Set the list of validation errors
     * 
     * @param validationErrors List of validation error strings to set
     */
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Check if this response represents a successful transaction
     * 
     * @return true if success flag is set and transaction ID is present
     */
    public boolean isTransactionSuccessful() {
        return success && transactionId != null && !transactionId.trim().isEmpty();
    }

    /**
     * Check if this response has validation errors
     * 
     * @return true if validation errors list is not empty
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Check if balance information is available
     * 
     * @return true if both previous and current balances are present
     */
    public boolean hasBalanceInformation() {
        return previousBalance != null && currentBalance != null;
    }

    /**
     * Get the balance change amount
     * 
     * @return BigDecimal difference between current and previous balance, or null if balance info unavailable
     */
    public BigDecimal getBalanceChange() {
        if (!hasBalanceInformation()) {
            return null;
        }
        return currentBalance.subtract(previousBalance);
    }

    /**
     * Create a formatted confirmation message including balance information
     * 
     * @return formatted confirmation message with balance details
     */
    public String getFormattedConfirmationMessage() {
        if (!isTransactionSuccessful()) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        
        if (hasBalanceInformation()) {
            sb.append(" Account balance updated from $")
              .append(previousBalance.toString())
              .append(" to $")
              .append(currentBalance.toString())
              .append(".");
        }
        
        if (confirmationTimestamp != null) {
            sb.append(" Processed at ")
              .append(confirmationTimestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
              .append(".");
        }
        
        return sb.toString();
    }

    /**
     * Get the number of validation errors
     * 
     * @return count of validation errors, or 0 if none
     */
    public int getValidationErrorCount() {
        return hasValidationErrors() ? validationErrors.size() : 0;
    }

    /**
     * Check if this is a client error response (4xx HTTP status)
     * 
     * @return true if HTTP status is in 400-499 range
     */
    public boolean isClientError() {
        return httpStatus != null && httpStatus >= 400 && httpStatus < 500;
    }

    /**
     * Check if this is a server error response (5xx HTTP status)
     * 
     * @return true if HTTP status is in 500-599 range
     */
    public boolean isServerError() {
        return httpStatus != null && httpStatus >= 500 && httpStatus < 600;
    }

    /**
     * Create a success response with complete transaction details
     * 
     * @param transactionId generated transaction ID
     * @param transaction complete transaction details
     * @param previousBalance account balance before transaction
     * @param currentBalance account balance after transaction
     * @return AddTransactionResponse configured for success
     */
    public static AddTransactionResponse createSuccessResponse(String transactionId, TransactionDTO transaction,
                                                               BigDecimal previousBalance, BigDecimal currentBalance) {
        return new AddTransactionResponse(transactionId, transaction, previousBalance, currentBalance);
    }

    /**
     * Create an error response with system error details
     * 
     * @param message error message
     * @param errorCode system error code
     * @param httpStatus HTTP status code
     * @return AddTransactionResponse configured for error
     */
    public static AddTransactionResponse createErrorResponse(String message, String errorCode, Integer httpStatus) {
        return new AddTransactionResponse(message, errorCode, httpStatus);
    }

    /**
     * Create a validation error response with detailed field errors
     * 
     * @param message primary error message
     * @param validationErrors list of field-level validation errors
     * @return AddTransactionResponse configured for validation errors
     */
    public static AddTransactionResponse createValidationErrorResponse(String message, List<String> validationErrors) {
        return new AddTransactionResponse(message, validationErrors, 400);
    }

    /**
     * Equals method for response comparison
     * 
     * @param obj object to compare
     * @return true if responses are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AddTransactionResponse that = (AddTransactionResponse) obj;
        return success == that.success &&
               Objects.equals(message, that.message) &&
               Objects.equals(transactionId, that.transactionId) &&
               Objects.equals(confirmationTimestamp, that.confirmationTimestamp) &&
               Objects.equals(transaction, that.transaction) &&
               Objects.equals(previousBalance, that.previousBalance) &&
               Objects.equals(currentBalance, that.currentBalance) &&
               Objects.equals(httpStatus, that.httpStatus) &&
               Objects.equals(errorCode, that.errorCode) &&
               Objects.equals(validationErrors, that.validationErrors);
    }

    /**
     * Hash code method for response hashing
     * 
     * @return hash code based on response fields
     */
    @Override
    public int hashCode() {
        return Objects.hash(success, message, transactionId, confirmationTimestamp, transaction,
                           previousBalance, currentBalance, httpStatus, errorCode, validationErrors);
    }

    /**
     * String representation of the response
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "AddTransactionResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", confirmationTimestamp=" + confirmationTimestamp +
                ", transaction=" + transaction +
                ", previousBalance=" + previousBalance +
                ", currentBalance=" + currentBalance +
                ", httpStatus=" + httpStatus +
                ", errorCode='" + errorCode + '\'' +
                ", validationErrors=" + validationErrors +
                '}';
    }
}