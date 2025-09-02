/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity representing account closure requests and tracking data, mapped to account_closure PostgreSQL table.
 * Contains closure request ID, account ID, closure reason codes, closure dates, final balance validation, 
 * closure status tracking, and regulatory notification flags. Used by account closure batch processing 
 * to track closure workflow and maintain audit trail for closed accounts.
 * 
 * This entity supports the account closure workflow by tracking each step of the closure process from
 * initial request through final completion, ensuring regulatory compliance and comprehensive audit trails.
 * The entity maintains relationships with Account entities to provide complete account lifecycle management.
 * 
 * Field mappings for account closure tracking:
 * - closure_request_id (BIGINT) → Primary key for unique closure request identification
 * - account_id (BIGINT) → Foreign key linking to Account entity for account association
 * - closure_reason_code (VARCHAR(4)) → Business reason code for account closure
 * - requested_date (DATE) → Date when closure was initially requested
 * - closure_date (DATE) → Date when closure was completed (null if pending)
 * - final_balance (NUMERIC(12,2)) → Final account balance at closure with COBOL COMP-3 precision
 * - closure_status (VARCHAR(1)) → Current status of closure process ('P'=Pending, 'C'=Complete, 'R'=Rejected)
 * - notification_sent (VARCHAR(1)) → Flag indicating if regulatory notifications were sent ('Y'/'N')
 * 
 * Relationships:
 * - @ManyToOne with Account entity (account_id foreign key)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "account_closure")
public class AccountClosure {

    // Constants for field constraints and business rules
    private static final String MAX_MONETARY_VALUE = "9999999999.99"; // Maximum balance for COMP-3 precision
    private static final int CLOSURE_REASON_CODE_LENGTH = 4;
    private static final int CLOSURE_STATUS_LENGTH = 1;
    private static final int NOTIFICATION_SENT_LENGTH = 1;
    
    /**
     * Closure request ID - Primary key.
     * Unique identifier for each account closure request in the system.
     * Generated automatically using database identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closure_request_id", nullable = false)
    private Long closureRequestId;
    
    /**
     * Account ID for the account being closed.
     * Foreign key linking to the Account entity to establish account relationship.
     * Required field for all closure requests.
     */
    @Column(name = "account_id", nullable = false)
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    /**
     * Closure reason code indicating business reason for account closure.
     * Standard business codes used for reporting and regulatory compliance.
     * Examples: 'CUST' (customer request), 'INAC' (inactive), 'FRAU' (fraud)
     */
    @Column(name = "closure_reason_code", length = CLOSURE_REASON_CODE_LENGTH, nullable = false)
    @NotNull(message = "Closure reason code is required")
    @Size(max = CLOSURE_REASON_CODE_LENGTH, message = "Closure reason code cannot exceed " + CLOSURE_REASON_CODE_LENGTH + " characters")
    private String closureReasonCode;
    
    /**
     * Date when account closure was requested.
     * Recorded when closure process is initiated, used for audit trail and reporting.
     * Required field for tracking closure timeline.
     */
    @Column(name = "requested_date", nullable = false)
    @NotNull(message = "Requested date is required")
    private LocalDate requestedDate;
    
    /**
     * Date when account closure was completed.
     * Null until closure process is finalized, then populated with completion date.
     * Used for regulatory reporting and audit trail completion.
     */
    @Column(name = "closure_date")
    private LocalDate closureDate;
    
    /**
     * Final account balance at time of closure.
     * Uses BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
     * Must be zero or negative (credit balance) for successful closure.
     * Supports exact monetary calculations matching mainframe precision.
     */
    @Column(name = "final_balance", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Final balance is required")
    @DecimalMax(value = MAX_MONETARY_VALUE, message = "Final balance cannot exceed " + MAX_MONETARY_VALUE)
    private BigDecimal finalBalance;
    
    /**
     * Current status of closure process.
     * Tracks closure workflow state: 'P' (Pending), 'C' (Complete), 'R' (Rejected)
     * Required for closure processing and audit trail management.
     */
    @Column(name = "closure_status", length = CLOSURE_STATUS_LENGTH, nullable = false)
    @NotNull(message = "Closure status is required")
    @Size(max = CLOSURE_STATUS_LENGTH, message = "Closure status cannot exceed " + CLOSURE_STATUS_LENGTH + " character")
    private String closureStatus;
    
    /**
     * Regulatory notification sent flag.
     * Indicates whether required regulatory notifications have been sent.
     * 'Y' = notifications sent, 'N' = notifications pending
     * Critical for compliance tracking and audit requirements.
     */
    @Column(name = "notification_sent", length = NOTIFICATION_SENT_LENGTH, nullable = false)
    @NotNull(message = "Notification sent flag is required")
    @Size(max = NOTIFICATION_SENT_LENGTH, message = "Notification sent flag cannot exceed " + NOTIFICATION_SENT_LENGTH + " character")
    private String notificationSent;
    
    /**
     * Account relationship.
     * @ManyToOne relationship with Account entity using account_id foreign key.
     * Links closure request to specific account for complete closure workflow tracking.
     */
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    @NotNull(message = "Account is required")
    private Account account;
    
    /**
     * Default constructor for JPA.
     */
    public AccountClosure() {
    }
    
    /**
     * Constructor with all required fields for account closure creation.
     * 
     * @param accountId the account ID to be closed
     * @param closureReasonCode the business reason code for closure
     * @param requestedDate the date closure was requested
     * @param finalBalance the final balance at closure
     * @param closureStatus the current closure status
     * @param notificationSent the regulatory notification flag
     */
    public AccountClosure(Long accountId, String closureReasonCode, LocalDate requestedDate, 
                         BigDecimal finalBalance, String closureStatus, String notificationSent) {
        this.accountId = accountId;
        this.closureReasonCode = closureReasonCode;
        this.requestedDate = requestedDate;
        this.finalBalance = finalBalance;
        this.closureStatus = closureStatus;
        this.notificationSent = notificationSent;
    }
    
    /**
     * JPA lifecycle callback for validation before persisting a new closure request.
     * Performs comprehensive field validation and ensures data consistency.
     * Initializes default values and ensures proper field formatting.
     */
    @PrePersist
    public void validateBeforeInsert() {
        performClosureValidation();
        initializeDefaults();
        formatFields();
    }
    
    /**
     * JPA lifecycle callback for validation before updating an existing closure request.
     * Performs comprehensive field validation and ensures data consistency.
     * Ensures business rule compliance for closure status transitions.
     */
    @PreUpdate
    public void validateBeforeUpdate() {
        performClosureValidation();
        formatFields();
    }
    
    /**
     * Performs comprehensive closure request validation using business rules.
     * Validates status codes, balance requirements, dates, and regulatory compliance.
     * Ensures data integrity matching closure processing requirements.
     */
    private void performClosureValidation() {
        // Validate closure status values
        if (closureStatus != null && !closureStatus.equals("P") && 
            !closureStatus.equals("C") && !closureStatus.equals("R")) {
            throw new IllegalArgumentException("Closure status must be 'P', 'C', or 'R'");
        }
        
        // Validate notification sent flag values
        if (notificationSent != null && !notificationSent.equals("Y") && !notificationSent.equals("N")) {
            throw new IllegalArgumentException("Notification sent flag must be 'Y' or 'N'");
        }
        
        // Validate final balance constraints
        if (finalBalance != null && finalBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Final balance must be zero or negative for account closure");
        }
        
        // Validate date relationships
        if (requestedDate != null && closureDate != null && requestedDate.isAfter(closureDate)) {
            throw new IllegalArgumentException("Requested date cannot be after closure date");
        }
        
        // Validate completion requirements
        if ("C".equals(closureStatus)) {
            if (closureDate == null) {
                throw new IllegalArgumentException("Closure date is required when status is Complete");
            }
            if (finalBalance != null && finalBalance.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Final balance must be zero when closure is complete");
            }
        }
        
        // Validate pending requirements
        if ("P".equals(closureStatus) && closureDate != null) {
            throw new IllegalArgumentException("Closure date must be null when status is Pending");
        }
    }
    
    /**
     * Initializes default values for new closure request records.
     * Sets appropriate defaults matching closure processing patterns.
     */
    private void initializeDefaults() {
        if (closureStatus == null) {
            closureStatus = "P"; // Default to Pending
        }
        
        if (notificationSent == null) {
            notificationSent = "N"; // Default to not sent
        }
        
        if (finalBalance == null) {
            finalBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        
        if (requestedDate == null) {
            requestedDate = LocalDate.now();
        }
    }
    
    /**
     * Formats closure fields to ensure consistent data formats.
     * Applies proper scaling for BigDecimal amounts and standardizes string fields.
     */
    private void formatFields() {
        // Ensure proper scale for monetary BigDecimal field (scale=2 for COMP-3 precision)
        if (finalBalance != null) {
            finalBalance = finalBalance.setScale(2, RoundingMode.HALF_UP);
        }
        
        // Format string fields to uppercase and trim whitespace
        if (closureReasonCode != null) {
            closureReasonCode = closureReasonCode.trim().toUpperCase();
        }
        
        if (closureStatus != null) {
            closureStatus = closureStatus.trim().toUpperCase();
        }
        
        if (notificationSent != null) {
            notificationSent = notificationSent.trim().toUpperCase();
        }
    }
    
    // Getter and setter methods
    
    /**
     * Gets the closure request ID.
     * 
     * @return the closure request ID
     */
    public Long getClosureRequestId() {
        return closureRequestId;
    }
    
    /**
     * Sets the closure request ID.
     * 
     * @param closureRequestId the closure request ID to set
     */
    public void setClosureRequestId(Long closureRequestId) {
        this.closureRequestId = closureRequestId;
    }
    
    /**
     * Gets the account ID.
     * 
     * @return the account ID
     */
    public Long getAccountId() {
        return accountId;
    }
    
    /**
     * Sets the account ID.
     * 
     * @param accountId the account ID to set
     */
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    /**
     * Gets the closure reason code.
     * 
     * @return the closure reason code
     */
    public String getClosureReasonCode() {
        return closureReasonCode;
    }
    
    /**
     * Sets the closure reason code.
     * 
     * @param closureReasonCode the closure reason code to set
     */
    public void setClosureReasonCode(String closureReasonCode) {
        this.closureReasonCode = closureReasonCode;
    }
    
    /**
     * Gets the requested date.
     * 
     * @return the requested date
     */
    public LocalDate getRequestedDate() {
        return requestedDate;
    }
    
    /**
     * Sets the requested date.
     * 
     * @param requestedDate the requested date to set
     */
    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }
    
    /**
     * Gets the closure date.
     * 
     * @return the closure date
     */
    public LocalDate getClosureDate() {
        return closureDate;
    }
    
    /**
     * Sets the closure date.
     * 
     * @param closureDate the closure date to set
     */
    public void setClosureDate(LocalDate closureDate) {
        this.closureDate = closureDate;
    }
    
    /**
     * Gets the final balance.
     * 
     * @return the final balance
     */
    public BigDecimal getFinalBalance() {
        return finalBalance;
    }
    
    /**
     * Sets the final balance.
     * 
     * @param finalBalance the final balance to set
     */
    public void setFinalBalance(BigDecimal finalBalance) {
        this.finalBalance = finalBalance;
    }
    
    /**
     * Gets the closure status.
     * 
     * @return the closure status
     */
    public String getClosureStatus() {
        return closureStatus;
    }
    
    /**
     * Sets the closure status.
     * 
     * @param closureStatus the closure status to set
     */
    public void setClosureStatus(String closureStatus) {
        this.closureStatus = closureStatus;
    }
    
    /**
     * Gets the notification sent flag.
     * 
     * @return the notification sent flag
     */
    public String getNotificationSent() {
        return notificationSent;
    }
    
    /**
     * Sets the notification sent flag.
     * 
     * @param notificationSent the notification sent flag to set
     */
    public void setNotificationSent(String notificationSent) {
        this.notificationSent = notificationSent;
    }
    
    /**
     * Gets the associated account.
     * 
     * @return the account entity
     */
    public Account getAccount() {
        return account;
    }
    
    /**
     * Sets the associated account.
     * 
     * @param account the account entity to set
     */
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
    
    /**
     * Checks if the closure request is complete.
     * 
     * @return true if closure status is 'C', false otherwise
     */
    public boolean isComplete() {
        return "C".equals(closureStatus);
    }
    
    /**
     * Checks if the closure request is pending.
     * 
     * @return true if closure status is 'P', false otherwise
     */
    public boolean isPending() {
        return "P".equals(closureStatus);
    }
    
    /**
     * Checks if the closure request is rejected.
     * 
     * @return true if closure status is 'R', false otherwise
     */
    public boolean isRejected() {
        return "R".equals(closureStatus);
    }
    
    /**
     * Checks if regulatory notifications have been sent.
     * 
     * @return true if notification sent flag is 'Y', false otherwise
     */
    public boolean isNotificationSent() {
        return "Y".equals(notificationSent);
    }
    
    /**
     * Uses Account entity methods to access current account information.
     * Demonstrates usage of members_accessed from internal imports schema.
     * 
     * @return current account balance if account is available, null otherwise
     */
    public BigDecimal getCurrentAccountBalance() {
        if (account != null) {
            return account.getCurrentBalance();
        }
        return null;
    }
    
    /**
     * Uses Account entity methods to check account status.
     * Demonstrates usage of members_accessed from internal imports schema.
     * 
     * @return current account active status if account is available, null otherwise
     */
    public String getCurrentAccountStatus() {
        if (account != null) {
            return account.getActiveStatus();
        }
        return null;
    }
    
    /**
     * Validates that final balance matches current account balance.
     * Uses BigDecimal.compareTo() method from external imports schema.
     * 
     * @return true if final balance matches current account balance, false otherwise
     */
    public boolean validateFinalBalance() {
        if (account != null && finalBalance != null) {
            BigDecimal currentBalance = account.getCurrentBalance();
            if (currentBalance != null) {
                return finalBalance.compareTo(currentBalance) == 0;
            }
        }
        return false;
    }
    
    /**
     * Sets final balance to current account balance with proper scaling.
     * Uses BigDecimal.setScale() and valueOf() methods from external imports schema.
     */
    public void setFinalBalanceFromAccount() {
        if (account != null) {
            BigDecimal currentBalance = account.getCurrentBalance();
            if (currentBalance != null) {
                this.finalBalance = currentBalance.setScale(2, RoundingMode.HALF_UP);
            } else {
                this.finalBalance = BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
    
    /**
     * Custom equals method to properly compare AccountClosure entities.
     * Uses closure request ID as the primary comparison field with proper null handling.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AccountClosure that = (AccountClosure) o;
        
        // Primary comparison by closure request ID
        return Objects.equals(closureRequestId, that.closureRequestId);
    }
    
    /**
     * Custom hash code method using Objects.hash() for consistency with equals().
     * 
     * @return hash code for the AccountClosure entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(closureRequestId);
    }
    
    /**
     * Custom toString method providing detailed closure request information.
     * Includes all key fields while maintaining data security for sensitive information.
     * 
     * @return string representation of the AccountClosure entity
     */
    @Override
    public String toString() {
        return "AccountClosure{" +
                "closureRequestId=" + closureRequestId +
                ", accountId=" + accountId +
                ", closureReasonCode='" + closureReasonCode + '\'' +
                ", requestedDate=" + requestedDate +
                ", closureDate=" + closureDate +
                ", finalBalance=" + finalBalance +
                ", closureStatus='" + closureStatus + '\'' +
                ", notificationSent='" + notificationSent + '\'' +
                ", accountActiveStatus='" + getCurrentAccountStatus() + '\'' +
                '}';
    }
}