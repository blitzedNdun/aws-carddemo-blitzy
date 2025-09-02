/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing card authorization records for real-time authorization processing.
 * 
 * This entity class represents credit card authorization attempts, decisions, and processing
 * results within the CardDemo system. It stores authorization requests, approval/decline
 * decisions, authorization codes, transaction amounts, and risk evaluation results.
 * 
 * The entity is designed to support sub-200ms authorization processing requirements with
 * optimized indexes and efficient data access patterns for high-throughput transaction
 * processing environments.
 * 
 * Key Features:
 * - Real-time authorization processing and storage
 * - Authorization code generation and tracking  
 * - Approval/decline status management
 * - Fraud detection integration and scoring
 * - Velocity limit checking and reporting
 * - Processing time measurement and monitoring
 * - Decline reason code tracking for analysis
 * 
 * Database Mapping:
 * - Table: authorization_data
 * - Primary Key: authorization_id (BIGINT, auto-generated)
 * - Foreign Keys: card_number → card_data, account_id → account_data
 * - Indexes: auth_card_idx (card_number), auth_account_idx (account_id), auth_timestamp_idx (request_timestamp)
 * 
 * Performance Considerations:
 * - Optimized for high-frequency read/write operations
 * - Indexed for rapid authorization lookups
 * - BigDecimal precision for exact monetary calculations
 * - Timestamp tracking for performance monitoring
 * 
 * Security Compliance:
 * - Card number reference for transaction tracking
 * - Authorization code generation for merchant verification
 * - Fraud score calculation and storage
 * - Audit trail maintenance for regulatory compliance
 * 
 * This implementation supports the migration from COBOL authorization processing
 * while maintaining identical business logic and data precision requirements
 * for financial transaction authorization workflows.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Entity
@Table(name = "authorization_data", indexes = {
    @Index(name = "auth_card_idx", columnList = "card_number"),
    @Index(name = "auth_account_idx", columnList = "account_id"), 
    @Index(name = "auth_timestamp_idx", columnList = "request_timestamp"),
    @Index(name = "auth_status_idx", columnList = "approval_status"),
    @Index(name = "auth_merchant_idx", columnList = "merchant_id")
})
public class Authorization {

    /**
     * Authorization ID - Primary key field.
     * Unique identifier for each authorization attempt in the system.
     * Auto-generated using database sequence for high-performance inserts.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "authorization_id", nullable = false)
    private Long authorizationId;

    /**
     * Card number for the authorization request.
     * References the card_data table for card validation and lookup.
     * Must be exactly 16 digits for standard credit card format.
     */
    @Column(name = "card_number", length = 16, nullable = false)
    @NotNull(message = "Card number is required")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Account ID associated with the card for authorization processing.
     * Links to account_data table for credit limit and balance validation.
     */
    @Column(name = "account_id", nullable = false)
    @NotNull(message = "Account ID is required")
    private Long accountId;

    /**
     * Merchant ID for authorization tracking.
     * Identifies the merchant requesting authorization for fraud detection and reporting.
     */
    @Column(name = "merchant_id")
    private Long merchantId;

    /**
     * Transaction amount for authorization.
     * Uses BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
     * Supports exact monetary calculations for authorization processing.
     */
    @Column(name = "transaction_amount", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero")
    @DecimalMax(value = "9999999999.99", message = "Transaction amount exceeds maximum limit")
    private BigDecimal transactionAmount;

    /**
     * Authorization code generated for approved transactions.
     * 6-character alphanumeric code provided to merchants for transaction verification.
     * Null for declined transactions.
     */
    @Column(name = "authorization_code", length = 6)
    @Size(max = 6, message = "Authorization code cannot exceed 6 characters")
    @Pattern(regexp = "^[A-Z0-9]{6}$|^$", message = "Authorization code must be 6 alphanumeric characters or empty")
    private String authorizationCode;

    /**
     * Timestamp when the authorization request was received.
     * Used for processing time calculation and audit trail maintenance.
     */
    @Column(name = "request_timestamp", nullable = false)
    @NotNull(message = "Request timestamp is required")
    private LocalDateTime requestTimestamp;

    /**
     * Timestamp when the authorization response was generated.
     * Used for processing time calculation and performance monitoring.
     */
    @Column(name = "response_timestamp", nullable = false)
    @NotNull(message = "Response timestamp is required")
    private LocalDateTime responseTimestamp;

    /**
     * Approval status for the authorization request.
     * Values: 'APPROVED' for successful authorizations, 'DECLINED' for rejected requests.
     */
    @Column(name = "approval_status", length = 8, nullable = false)
    @NotNull(message = "Approval status is required")
    @Pattern(regexp = "^(APPROVED|DECLINED)$", message = "Approval status must be APPROVED or DECLINED")
    private String approvalStatus;

    /**
     * Decline reason code for rejected authorization requests.
     * Standardized codes for merchant and cardholder communication.
     * Null for approved transactions.
     */
    @Column(name = "decline_reason_code", length = 3)
    @Size(max = 3, message = "Decline reason code cannot exceed 3 characters")
    @Pattern(regexp = "^\\d{3}$|^$", message = "Decline reason code must be 3 digits or empty")
    private String declineReasonCode;

    /**
     * Velocity check result for transaction frequency validation.
     * Boolean flag indicating if velocity checks passed (true) or failed (false).
     */
    @Column(name = "velocity_check_result", nullable = false)
    @NotNull(message = "Velocity check result is required")
    private Boolean velocityCheckResult;

    /**
     * Fraud score calculated by the fraud detection system.
     * Range: 0-1000, where higher scores indicate higher fraud risk.
     * Used for authorization decision-making and risk management.
     */
    @Column(name = "fraud_score", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Fraud score is required")
    @DecimalMin(value = "0.00", message = "Fraud score cannot be negative")
    @DecimalMax(value = "999.99", message = "Fraud score cannot exceed 999.99")
    private BigDecimal fraudScore;

    /**
     * Processing time in milliseconds for the authorization request.
     * Measured from request receipt to response generation.
     * Used for performance monitoring and SLA compliance tracking.
     */
    @Column(name = "processing_time", nullable = false)
    @NotNull(message = "Processing time is required")
    @DecimalMin(value = "0", message = "Processing time cannot be negative")
    private Integer processingTime;

    /**
     * Card entity relationship.
     * Many-to-one relationship with Card entity for card information access.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;

    /**
     * Account entity relationship.
     * Many-to-one relationship with Account entity for account information access.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    /**
     * Default constructor for JPA.
     */
    public Authorization() {
    }

    /**
     * Constructor with required fields for authorization processing.
     * 
     * @param cardNumber the card number (16 digits)
     * @param accountId the account ID
     * @param merchantId the merchant ID
     * @param transactionAmount the transaction amount
     * @param requestTimestamp the request timestamp
     * @param responseTimestamp the response timestamp
     * @param approvalStatus the approval status (APPROVED/DECLINED)
     * @param velocityCheckResult the velocity check result
     * @param fraudScore the fraud score (0-999.99)
     * @param processingTime the processing time in milliseconds
     */
    public Authorization(String cardNumber, Long accountId, Long merchantId,
                        BigDecimal transactionAmount, LocalDateTime requestTimestamp,
                        LocalDateTime responseTimestamp, String approvalStatus,
                        Boolean velocityCheckResult, BigDecimal fraudScore, Integer processingTime) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.transactionAmount = transactionAmount;
        this.requestTimestamp = requestTimestamp;
        this.responseTimestamp = responseTimestamp;
        this.approvalStatus = approvalStatus;
        this.velocityCheckResult = velocityCheckResult;
        this.fraudScore = fraudScore;
        this.processingTime = processingTime;
    }

    // Getters and Setters

    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public LocalDateTime getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(LocalDateTime responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getDeclineReasonCode() {
        return declineReasonCode;
    }

    public void setDeclineReasonCode(String declineReasonCode) {
        this.declineReasonCode = declineReasonCode;
    }

    public Boolean getVelocityCheckResult() {
        return velocityCheckResult;
    }

    public void setVelocityCheckResult(Boolean velocityCheckResult) {
        this.velocityCheckResult = velocityCheckResult;
    }

    public BigDecimal getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(BigDecimal fraudScore) {
        this.fraudScore = fraudScore;
    }

    public Integer getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Integer processingTime) {
        this.processingTime = processingTime;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
        if (card != null) {
            this.cardNumber = card.getCardNumber();
        }
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }

    // Business Methods

    /**
     * Checks if the authorization was approved.
     * 
     * @return true if approval status is 'APPROVED', false otherwise
     */
    public boolean isApproved() {
        return "APPROVED".equals(approvalStatus);
    }

    /**
     * Checks if the authorization was declined.
     * 
     * @return true if approval status is 'DECLINED', false otherwise
     */
    public boolean isDeclined() {
        return "DECLINED".equals(approvalStatus);
    }

    /**
     * Checks if the velocity check passed.
     * 
     * @return true if velocity check result is 'PASS', false otherwise
     */
    public boolean passedVelocityCheck() {
        return "PASS".equals(velocityCheckResult);
    }

    /**
     * Checks if the authorization processing time meets SLA requirements.
     * SLA requirement: sub-200ms processing time for authorization requests.
     * 
     * @return true if processing time is under 200ms, false otherwise
     */
    public boolean meetsSLA() {
        return processingTime != null && processingTime < 200;
    }

    /**
     * Determines if the fraud score indicates high risk.
     * High risk threshold: fraud score >= 750.00.
     * 
     * @return true if fraud score indicates high risk, false otherwise
     */
    public boolean isHighRisk() {
        return fraudScore != null && fraudScore.compareTo(new BigDecimal("750.00")) >= 0;
    }

    /**
     * Calculates the processing duration from request to response.
     * 
     * @return processing duration in milliseconds, or null if timestamps are missing
     */
    public Long getProcessingDurationMs() {
        if (requestTimestamp != null && responseTimestamp != null) {
            return java.time.Duration.between(requestTimestamp, responseTimestamp).toMillis();
        }
        return null;
    }

    /**
     * Validates the authorization record for data consistency and business rules.
     * Performs comprehensive validation including field requirements, data formats,
     * and business logic constraints.
     * 
     * @throws RuntimeException if validation fails with specific error messages
     */
    public void validateAuthorization() {
        // Validate required fields
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new RuntimeException("Card number is required for authorization");
        }
        
        if (accountId == null) {
            throw new RuntimeException("Account ID is required for authorization");
        }
        
        if (transactionAmount == null) {
            throw new RuntimeException("Transaction amount is required for authorization");
        }
        
        if (requestTimestamp == null) {
            throw new RuntimeException("Request timestamp is required for authorization");
        }
        
        if (responseTimestamp == null) {
            throw new RuntimeException("Response timestamp is required for authorization");
        }
        
        if (approvalStatus == null || approvalStatus.isEmpty()) {
            throw new RuntimeException("Approval status is required for authorization");
        }
        
        // Validate card number format
        if (!cardNumber.matches("^\\d{16}$")) {
            throw new RuntimeException("Card number must be exactly 16 digits");
        }
        
        // Validate transaction amount
        if (transactionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transaction amount must be greater than zero");
        }
        
        if (transactionAmount.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new RuntimeException("Transaction amount exceeds maximum limit");
        }
        
        // Validate approval status
        if (!"APPROVED".equals(approvalStatus) && !"DECLINED".equals(approvalStatus)) {
            throw new RuntimeException("Approval status must be APPROVED or DECLINED");
        }
        
        // Validate authorization code for approved transactions
        if ("APPROVED".equals(approvalStatus)) {
            if (authorizationCode == null || authorizationCode.isEmpty()) {
                throw new RuntimeException("Authorization code is required for approved transactions");
            }
            if (!authorizationCode.matches("^[A-Z0-9]{6}$")) {
                throw new RuntimeException("Authorization code must be 6 alphanumeric characters");
            }
        }
        
        // Validate decline reason code for declined transactions
        if ("DECLINED".equals(approvalStatus)) {
            if (declineReasonCode == null || declineReasonCode.isEmpty()) {
                throw new RuntimeException("Decline reason code is required for declined transactions");
            }
            if (!declineReasonCode.matches("^\\d{3}$")) {
                throw new RuntimeException("Decline reason code must be exactly 3 digits");
            }
        }
        
        // Validate velocity check result
        if (velocityCheckResult == null) {
            throw new RuntimeException("Velocity check result is required");
        }
        
        // Validate fraud score
        if (fraudScore == null) {
            throw new RuntimeException("Fraud score is required");
        }
        if (fraudScore.compareTo(BigDecimal.ZERO) < 0 || fraudScore.compareTo(new BigDecimal("999.99")) > 0) {
            throw new RuntimeException("Fraud score must be between 0.00 and 999.99");
        }
        
        // Validate processing time
        if (processingTime == null) {
            throw new RuntimeException("Processing time is required");
        }
        if (processingTime < 0) {
            throw new RuntimeException("Processing time cannot be negative");
        }
        
        // Validate timestamp sequence
        if (responseTimestamp.isBefore(requestTimestamp)) {
            throw new RuntimeException("Response timestamp cannot be before request timestamp");
        }
    }

    // Object Methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Authorization auth = (Authorization) obj;
        return Objects.equals(authorizationId, auth.authorizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizationId);
    }

    @Override
    public String toString() {
        return "Authorization{" +
                "authorizationId=" + authorizationId +
                ", cardNumber='" + (cardNumber != null ? "****" + cardNumber.substring(12) : null) + '\'' +
                ", accountId=" + accountId +
                ", transactionAmount=" + transactionAmount +
                ", authorizationCode='" + authorizationCode + '\'' +
                ", requestTimestamp=" + requestTimestamp +
                ", responseTimestamp=" + responseTimestamp +
                ", approvalStatus='" + approvalStatus + '\'' +
                ", declineReasonCode='" + declineReasonCode + '\'' +
                ", velocityCheckResult=" + velocityCheckResult +
                ", fraudScore=" + fraudScore +
                ", processingTime=" + processingTime +
                ", isApproved=" + isApproved() +
                ", meetsSLA=" + meetsSLA() +
                ", isHighRisk=" + isHighRisk() +
                '}';
    }
}