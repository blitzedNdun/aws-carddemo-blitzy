/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing credit card statement records, mapped to statements PostgreSQL table.
 * Corresponds to STATEMENT-RECORD structure from statement processing COBOL programs.
 * 
 * Contains statement identification, balance information, billing cycle dates, minimum payment
 * calculations, and archival metadata. Uses BigDecimal for monetary fields to preserve 
 * COBOL COMP-3 packed decimal precision. Implements relationships with Account entity 
 * through foreign keys.
 * 
 * Field mappings from COBOL statement processing programs CBSTM03A/CBSTM03B:
 * - STMT-ID (PIC 9(11)) → statementId (BIGINT)
 * - ACCT-ID (PIC 9(11)) → accountId (VARCHAR(20))
 * - STMT-DATE (PIC X(10)) → statementDate (DATE)
 * - STMT-CURR-BAL (PIC S9(10)V99) → currentBalance (NUMERIC(12,2))
 * - STMT-MIN-PAY-AMT (PIC S9(10)V99) → minimumPaymentAmount (NUMERIC(12,2))
 * - STMT-CYCLE-START (PIC X(10)) → cycleStartDate (DATE)
 * - STMT-CYCLE-END (PIC X(10)) → cycleEndDate (DATE)
 * - STMT-STATUS (PIC X(01)) → statementStatus (CHAR(1))
 * 
 * Relationships:
 * - @ManyToOne with Account entity (account_id foreign key)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "statements", indexes = {
    @Index(name = "idx_statements_account_id", columnList = "account_id"),
    @Index(name = "idx_statements_date", columnList = "statement_date"),
    @Index(name = "idx_statements_account_date", columnList = "account_id,statement_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statement {

    /**
     * Statement unique identifier (Primary Key)
     * Maps to STMT-ID from COBOL statement processing
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statement_id")
    private Long statementId;

    /**
     * Account identifier for this statement
     * Maps to ACCT-ID from COBOL statement processing
     */
    @NotNull
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /**
     * Statement generation date
     * Maps to STMT-DATE from COBOL statement processing
     */
    @NotNull
    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    /**
     * Current balance as of statement date with COMP-3 precision
     * Maps to STMT-CURR-BAL from COBOL statement processing
     */
    @NotNull
    @Column(name = "current_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentBalance;

    /**
     * Previous balance from last statement
     * Maps to STMT-PREV-BAL from COBOL statement processing
     */
    @Column(name = "previous_balance", precision = 12, scale = 2)
    private BigDecimal previousBalance;

    /**
     * Minimum payment amount calculated per COBOL rules
     * Maps to STMT-MIN-PAY-AMT from COBOL statement processing
     */
    @Column(name = "minimum_payment_amount", precision = 12, scale = 2)
    private BigDecimal minimumPaymentAmount;

    /**
     * Payment due date (typically 25 days from statement date)
     * Maps to STMT-DUE-DATE from COBOL statement processing
     */
    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    /**
     * Billing cycle start date
     * Maps to STMT-CYCLE-START from COBOL statement processing
     */
    @Column(name = "cycle_start_date")
    private LocalDate cycleStartDate;

    /**
     * Billing cycle end date
     * Maps to STMT-CYCLE-END from COBOL statement processing
     */
    @Column(name = "cycle_end_date")
    private LocalDate cycleEndDate;

    /**
     * Statement status (G=Generated, A=Archived, P=Paid)
     * Maps to STMT-STATUS from COBOL statement processing
     */
    @Size(max = 1)
    @Column(name = "statement_status", length = 1)
    private String statementStatus;

    /**
     * Total credits for this statement cycle
     * Calculated from transaction aggregation
     */
    @Column(name = "total_credits", precision = 12, scale = 2)
    private BigDecimal totalCredits;

    /**
     * Total debits for this statement cycle
     * Calculated from transaction aggregation
     */
    @Column(name = "total_debits", precision = 12, scale = 2)
    private BigDecimal totalDebits;

    /**
     * Interest charges for this statement period
     * Maps to interest calculation logic from COBOL
     */
    @Column(name = "interest_charges", precision = 12, scale = 2)
    private BigDecimal interestCharges;

    /**
     * Fees assessed during this statement period
     * Maps to fee calculation logic from COBOL
     */
    @Column(name = "fees", precision = 12, scale = 2)
    private BigDecimal fees;

    /**
     * Credit limit as of statement date
     * Maps to ACCT-CREDIT-LIMIT from related account
     */
    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    /**
     * Available credit as of statement date
     * Calculated as credit_limit - current_balance
     */
    @Column(name = "available_credit", precision = 12, scale = 2)
    private BigDecimal availableCredit;

    /**
     * Statement creation timestamp for audit trail
     */
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    /**
     * Last modification timestamp for audit trail
     */
    @Column(name = "last_updated_date")
    private LocalDateTime lastUpdatedDate;

    /**
     * User ID who generated this statement
     */
    @Size(max = 8)
    @Column(name = "created_by", length = 8)
    private String createdBy;

    /**
     * Statement file path for archived statement documents
     */
    @Size(max = 255)
    @Column(name = "statement_file_path", length = 255)
    private String statementFilePath;

    /**
     * Cash advance limit for the account
     * Maps to ACCT-CASH-CREDIT-LIMIT from COBOL account structure
     */
    @Column(name = "cash_advance_limit", precision = 12, scale = 2)
    private BigDecimal cashAdvanceLimit;

    /**
     * Current cash advance balance
     * Maps to ACCT-CASH-BALANCE from COBOL account structure  
     */
    @Column(name = "cash_advance_balance", precision = 12, scale = 2)
    private BigDecimal cashAdvanceBalance;

    /**
     * JPA lifecycle callback to set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastUpdatedDate = LocalDateTime.now();
        
        // Set default status if not provided
        if (statementStatus == null) {
            statementStatus = "G"; // Generated
        }
    }

    /**
     * JPA lifecycle callback to update modification timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedDate = LocalDateTime.now();
    }

    /**
     * Calculate available credit based on current balance and credit limit
     * Replicates COBOL calculation logic
     */
    public BigDecimal calculateAvailableCredit() {
        if (creditLimit != null && currentBalance != null) {
            return creditLimit.subtract(currentBalance);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Determine if minimum payment is due
     * Based on COBOL business rules
     */
    public boolean isMinimumPaymentDue() {
        return currentBalance != null && 
               currentBalance.compareTo(BigDecimal.ZERO) > 0 &&
               minimumPaymentAmount != null &&
               minimumPaymentAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get payments and credits amount 
     * Returns total credits for the statement period
     * Maps to COBOL statement processing logic
     */
    public BigDecimal getPaymentsCredits() {
        return totalCredits != null ? totalCredits : BigDecimal.ZERO;
    }

    /**
     * Get purchases and debits amount
     * Returns total debits for the statement period
     * Maps to COBOL statement processing logic
     */
    public BigDecimal getPurchasesDebits() {
        return totalDebits != null ? totalDebits : BigDecimal.ZERO;
    }

    /**
     * Get fees and charges amount
     * Returns total fees assessed during the statement period
     * Maps to COBOL fee calculation logic
     */
    public BigDecimal getFeesCharges() {
        return fees != null ? fees : BigDecimal.ZERO;
    }

    /**
     * Get cash advance limit
     * Returns the cash advance limit for the account
     * Maps to COBOL account structure
     */
    public BigDecimal getCashAdvanceLimit() {
        return cashAdvanceLimit != null ? cashAdvanceLimit : BigDecimal.ZERO;
    }

    /**
     * Get current cash advance balance
     * Returns the current cash advance balance
     * Maps to COBOL account structure
     */
    public BigDecimal getCashAdvanceBalance() {
        return cashAdvanceBalance != null ? cashAdvanceBalance : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statement statement = (Statement) o;
        return Objects.equals(statementId, statement.statementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statementId);
    }

    @Override
    public String toString() {
        return "Statement{" +
                "statementId=" + statementId +
                ", accountId='" + accountId + '\'' +
                ", statementDate=" + statementDate +
                ", currentBalance=" + currentBalance +
                ", statementStatus='" + statementStatus + '\'' +
                '}';
    }
}