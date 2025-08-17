package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for card authorization responses sent back to payment processors and merchants.
 * Contains authorization decision, codes, timestamps, and decline reasons matching original COBOL 
 * authorization response formats.
 * 
 * This DTO maintains compatibility with the original CICS authorization response structures while
 * providing modern JSON serialization capabilities for REST API responses.
 * 
 * Performance target: Support sub-200ms authorization response times as required by payment 
 * processing standards.
 */
public class AuthorizationResponse {

    @JsonProperty("authorization_code")
    private String authorizationCode;

    @JsonProperty("approval_status")
    private String approvalStatus;

    @JsonProperty("decline_reason_code")
    private String declineReasonCode;

    @JsonProperty("response_timestamp")
    private LocalDateTime responseTimestamp;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("available_credit")
    private BigDecimal availableCredit;

    @JsonProperty("account_balance")
    private BigDecimal accountBalance;

    @JsonProperty("processing_time")
    private Long processingTime;

    @JsonProperty("fraud_score")
    private Integer fraudScore;

    @JsonProperty("velocity_check_result")
    private String velocityCheckResult;

    /**
     * Default constructor required for Jackson deserialization.
     */
    public AuthorizationResponse() {
        this.responseTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor for creating authorization responses with core data.
     * 
     * @param authorizationCode Authorization code for approved transactions
     * @param approvalStatus Status of the authorization (APPROVED, DECLINED, etc.)
     * @param transactionId Unique transaction identifier
     * @param availableCredit Available credit on the account
     * @param accountBalance Current account balance
     */
    public AuthorizationResponse(String authorizationCode, String approvalStatus, 
                               String transactionId, BigDecimal availableCredit, 
                               BigDecimal accountBalance) {
        this.authorizationCode = authorizationCode;
        this.approvalStatus = approvalStatus;
        this.transactionId = transactionId;
        this.availableCredit = availableCredit;
        this.accountBalance = accountBalance;
        this.responseTimestamp = LocalDateTime.now();
    }

    /**
     * Complete constructor for all authorization response fields.
     * 
     * @param authorizationCode Authorization code for approved transactions
     * @param approvalStatus Status of the authorization
     * @param declineReasonCode Reason code for declined transactions
     * @param transactionId Unique transaction identifier
     * @param availableCredit Available credit on the account
     * @param accountBalance Current account balance
     * @param processingTime Processing time in milliseconds
     * @param fraudScore Fraud detection score (0-100)
     * @param velocityCheckResult Result of velocity checking
     */
    public AuthorizationResponse(String authorizationCode, String approvalStatus,
                               String declineReasonCode, String transactionId,
                               BigDecimal availableCredit, BigDecimal accountBalance,
                               Long processingTime, Integer fraudScore,
                               String velocityCheckResult) {
        this.authorizationCode = authorizationCode;
        this.approvalStatus = approvalStatus;
        this.declineReasonCode = declineReasonCode;
        this.transactionId = transactionId;
        this.availableCredit = availableCredit;
        this.accountBalance = accountBalance;
        this.processingTime = processingTime;
        this.fraudScore = fraudScore;
        this.velocityCheckResult = velocityCheckResult;
        this.responseTimestamp = LocalDateTime.now();
    }

    // Getter methods as required by exports schema

    /**
     * Gets the authorization code for approved transactions.
     * 
     * @return Authorization code string (typically 6-digit alphanumeric)
     */
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    /**
     * Gets the approval status of the authorization request.
     * 
     * @return Approval status (APPROVED, DECLINED, PENDING, ERROR)
     */
    public String getApprovalStatus() {
        return approvalStatus;
    }

    /**
     * Gets the decline reason code for rejected transactions.
     * 
     * @return Decline reason code or null for approved transactions
     */
    public String getDeclineReasonCode() {
        return declineReasonCode;
    }

    /**
     * Gets the processing time for the authorization request.
     * 
     * @return Processing time in milliseconds
     */
    public Long getProcessingTime() {
        return processingTime;
    }

    /**
     * Gets the timestamp when the authorization response was generated.
     * 
     * @return Response timestamp using LocalDateTime.now()
     */
    public LocalDateTime getResponseTimestamp() {
        return responseTimestamp;
    }

    /**
     * Gets the unique transaction identifier.
     * 
     * @return Transaction ID string
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the available credit on the account after authorization.
     * 
     * @return Available credit amount with precise decimal handling
     */
    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }

    /**
     * Gets the current account balance.
     * 
     * @return Account balance with COBOL COMP-3 compatible precision
     */
    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    /**
     * Gets the fraud detection score.
     * 
     * @return Fraud score (0-100, where higher values indicate higher risk)
     */
    public Integer getFraudScore() {
        return fraudScore;
    }

    /**
     * Gets the velocity check result.
     * 
     * @return Velocity check result (PASS, FAIL, WARNING)
     */
    public String getVelocityCheckResult() {
        return velocityCheckResult;
    }

    // Helper methods as required by exports schema

    /**
     * Determines if the authorization was approved.
     * 
     * @return true if the transaction was approved, false otherwise
     */
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(approvalStatus);
    }

    /**
     * Determines if the authorization was declined.
     * 
     * @return true if the transaction was declined, false otherwise
     */
    public boolean isDeclined() {
        return "DECLINED".equalsIgnoreCase(approvalStatus);
    }

    // Setter methods for complete DTO functionality

    /**
     * Sets the authorization code.
     * 
     * @param authorizationCode Authorization code for approved transactions
     */
    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    /**
     * Sets the approval status.
     * 
     * @param approvalStatus Status of the authorization
     */
    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    /**
     * Sets the decline reason code.
     * 
     * @param declineReasonCode Reason code for declined transactions
     */
    public void setDeclineReasonCode(String declineReasonCode) {
        this.declineReasonCode = declineReasonCode;
    }

    /**
     * Sets the response timestamp.
     * 
     * @param responseTimestamp Timestamp when response was generated
     */
    public void setResponseTimestamp(LocalDateTime responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    /**
     * Sets the transaction ID.
     * 
     * @param transactionId Unique transaction identifier
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Sets the available credit amount.
     * 
     * @param availableCredit Available credit with precise decimal handling
     */
    public void setAvailableCredit(BigDecimal availableCredit) {
        this.availableCredit = availableCredit;
    }

    /**
     * Sets the account balance.
     * 
     * @param accountBalance Current account balance
     */
    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    /**
     * Sets the processing time.
     * 
     * @param processingTime Processing time in milliseconds
     */
    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    /**
     * Sets the fraud detection score.
     * 
     * @param fraudScore Fraud score (0-100)
     */
    public void setFraudScore(Integer fraudScore) {
        this.fraudScore = fraudScore;
    }

    /**
     * Sets the velocity check result.
     * 
     * @param velocityCheckResult Result of velocity checking
     */
    public void setVelocityCheckResult(String velocityCheckResult) {
        this.velocityCheckResult = velocityCheckResult;
    }

    /**
     * Provides a string representation of the authorization response for logging and debugging.
     * Excludes sensitive information like authorization codes.
     * 
     * @return String representation of the authorization response
     */
    @Override
    public String toString() {
        return "AuthorizationResponse{" +
                "approvalStatus='" + approvalStatus + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", responseTimestamp=" + responseTimestamp +
                ", processingTime=" + processingTime +
                ", fraudScore=" + fraudScore +
                ", velocityCheckResult='" + velocityCheckResult + '\'' +
                '}';
    }

    /**
     * Generates hash code for the authorization response based on transaction ID and timestamp.
     * 
     * @return Hash code for this authorization response
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
        result = prime * result + ((responseTimestamp == null) ? 0 : responseTimestamp.hashCode());
        return result;
    }

    /**
     * Compares this authorization response with another for equality based on transaction ID and timestamp.
     * 
     * @param obj Object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AuthorizationResponse that = (AuthorizationResponse) obj;
        
        if (transactionId != null ? !transactionId.equals(that.transactionId) : that.transactionId != null)
            return false;
        return responseTimestamp != null ? responseTimestamp.equals(that.responseTimestamp) : that.responseTimestamp == null;
    }
}