package com.carddemo.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for bill payment operations in the CardDemo application.
 * 
 * This class represents the response data for bill payment transactions,
 * mapping to the COBIL00 BMS output fields and preserving COBOL data 
 * precision requirements. It contains transaction details, updated balance
 * information, and status messages for client communication.
 * 
 * Maps to the following COBOL/BMS structures:
 * - TRNID (transaction ID) from COBIL0AO structure
 * - ACCTBAL (account balance) from COBIL0AO structure  
 * - ACTMSG (action message) from COBIL0AO structure
 * - Success/error status derived from WS-ERR-FLG logic
 * 
 * Financial amounts are handled using BigDecimal to maintain exact
 * precision matching COBOL COMP-3 packed decimal format.
 */
public class BillPaymentResponse {

    /**
     * Transaction ID generated for the bill payment operation.
     * Maps to TRAN-ID field from COBOL program (PIC 9(16)).
     * Contains the unique identifier for the payment transaction.
     */
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Current account balance after the bill payment transaction.
     * Maps to ACCT-CURR-BAL field from COBOL program (PIC +9999999999.99).
     * Uses BigDecimal to preserve COBOL COMP-3 decimal precision (scale=2).
     */
    @JsonProperty("currentBalance") 
    private BigDecimal currentBalance;

    /**
     * Success message displayed when bill payment completes successfully.
     * Maps to WS-MESSAGE field when WS-ERR-FLG is 'N' (no error).
     * Contains confirmation details including transaction ID.
     */
    @JsonProperty("successMessage")
    private String successMessage;

    /**
     * Error message displayed when bill payment fails or encounters issues.
     * Maps to WS-MESSAGE field when WS-ERR-FLG is 'Y' (error condition).
     * Contains detailed error information for user display.
     */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /**
     * Boolean flag indicating overall success status of the bill payment operation.
     * Derived from COBOL WS-ERR-FLG logic: true when ERR-FLG-OFF, false when ERR-FLG-ON.
     * Used by client applications to determine response handling.
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * Default constructor for Jackson serialization and Spring framework usage.
     */
    public BillPaymentResponse() {
        // Initialize with default values
        this.transactionId = null;
        this.currentBalance = BigDecimal.ZERO.setScale(2);
        this.successMessage = null;
        this.errorMessage = null;
        this.success = false;
    }

    /**
     * Parameterized constructor for convenient object creation with all fields.
     * 
     * @param transactionId The unique transaction identifier for the bill payment
     * @param currentBalance The updated account balance after payment (with 2 decimal precision)
     * @param successMessage Success message to display to user (null if error occurred)
     * @param errorMessage Error message to display to user (null if successful)
     * @param success Boolean flag indicating overall operation success status
     */
    public BillPaymentResponse(String transactionId, BigDecimal currentBalance, 
                              String successMessage, String errorMessage, boolean success) {
        this.transactionId = transactionId;
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.successMessage = successMessage;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    /**
     * Gets the transaction ID for the bill payment operation.
     * 
     * @return String containing the unique transaction identifier (maps to TRAN-ID)
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID for the bill payment operation.
     * 
     * @param transactionId The unique transaction identifier to set
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets the current account balance after the bill payment transaction.
     * 
     * @return BigDecimal containing the updated balance with 2 decimal precision
     *         (maps to ACCT-CURR-BAL with COBOL COMP-3 precision)
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Sets the current account balance after the bill payment transaction.
     * Automatically applies scale of 2 with HALF_UP rounding to match COBOL behavior.
     * 
     * @param currentBalance The updated account balance to set
     */
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? 
            currentBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    /**
     * Gets the success message for successful bill payment operations.
     * 
     * @return String containing success message (null if error occurred)
     */
    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Sets the success message for successful bill payment operations.
     * 
     * @param successMessage The success message to set
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    /**
     * Gets the error message for failed bill payment operations.
     * 
     * @return String containing error message (null if successful)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for failed bill payment operations.
     * 
     * @param errorMessage The error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the success status flag for the bill payment operation.
     * 
     * @return boolean true if operation succeeded, false if failed
     *         (derived from COBOL WS-ERR-FLG logic)
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status flag for the bill payment operation.
     * 
     * @param success The success status to set (true for success, false for failure)
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Provides string representation of the BillPaymentResponse for debugging and logging.
     * 
     * @return String representation including all fields (balance formatted with precision)
     */
    @Override
    public String toString() {
        return "BillPaymentResponse{" +
                "transactionId='" + transactionId + '\'' +
                ", currentBalance=" + currentBalance +
                ", successMessage='" + successMessage + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", success=" + success +
                '}';
    }

    /**
     * Checks equality based on transaction ID and success status.
     * 
     * @param obj Object to compare with
     * @return boolean true if objects are equal based on key fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BillPaymentResponse that = (BillPaymentResponse) obj;
        
        if (success != that.success) return false;
        if (transactionId != null ? !transactionId.equals(that.transactionId) : that.transactionId != null) return false;
        if (currentBalance != null ? currentBalance.compareTo(that.currentBalance) != 0 : that.currentBalance != null) return false;
        if (successMessage != null ? !successMessage.equals(that.successMessage) : that.successMessage != null) return false;
        return errorMessage != null ? errorMessage.equals(that.errorMessage) : that.errorMessage == null;
    }

    /**
     * Generates hash code based on key fields for proper hash-based collection usage.
     * 
     * @return int hash code calculated from transaction ID and success status
     */
    @Override
    public int hashCode() {
        int result = transactionId != null ? transactionId.hashCode() : 0;
        result = 31 * result + (currentBalance != null ? currentBalance.hashCode() : 0);
        result = 31 * result + (successMessage != null ? successMessage.hashCode() : 0);
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        result = 31 * result + (success ? 1 : 0);
        return result;
    }
}