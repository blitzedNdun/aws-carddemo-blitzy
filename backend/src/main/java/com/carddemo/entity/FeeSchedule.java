package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity representing fee schedule configuration including fee rates, waiver conditions, 
 * and assessment rules. Defines fee calculation parameters for different account types and 
 * fee categories. Supports versioning and effective date ranges for fee schedule changes 
 * and regulatory compliance.
 * 
 * This entity replaces VSAM DISCGRP file from the original COBOL system,
 * providing equivalent functionality for fee schedule management and rate lookups.
 * 
 * Based on COBOL programs: CBACT04C (Interest Calculator)
 * Data structure: CVTRA02Y (Disclosure Group Record)
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024
 */
@Entity
@Table(name = "fee_schedules", indexes = {
    @Index(name = "idx_fee_schedule_type_account", columnList = "fee_type, account_type"),
    @Index(name = "idx_fee_schedule_effective_date", columnList = "effective_date"),
    @Index(name = "idx_fee_schedule_account_type", columnList = "account_type"),
    @Index(name = "idx_fee_schedule_active", columnList = "effective_date, expiration_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeSchedule {

    /**
     * Primary key for fee schedule record.
     * Auto-generated Long ID for unique identification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_schedule_id")
    private Long feeScheduleId;

    /**
     * Fee type code identifying the category of fee.
     * Examples: ANNUAL, LATE_PAYMENT, OVER_LIMIT, FOREIGN_TRANSACTION, MAINTENANCE
     * Maps to DIS-TRAN-TYPE-CD from COBOL CVTRA02Y structure.
     */
    @NotNull
    @Size(min = 2, max = 20, message = "Fee type must be between 2 and 20 characters")
    @Column(name = "fee_type", length = 20, nullable = false)
    private String feeType;

    /**
     * Account type code for targeted fee application.
     * Maps to DIS-ACCT-GROUP-ID from COBOL CVTRA02Y structure.
     * Examples: STANDARD, PREMIUM, REWARDS, STUDENT
     */
    @NotNull
    @Size(min = 1, max = 10, message = "Account type must be between 1 and 10 characters")
    @Column(name = "account_type", length = 10, nullable = false)
    private String accountType;

    /**
     * Fixed fee amount for flat fee assessments.
     * Uses BigDecimal with scale=2 to preserve COBOL COMP-3 packed decimal precision.
     * Null if percentage-based fee is used instead.
     */
    @DecimalMin(value = "0.00", message = "Fee amount must not be negative")
    @DecimalMax(value = "9999.99", message = "Fee amount must not exceed 9999.99")
    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    /**
     * Percentage rate for percentage-based fee calculations.
     * Uses BigDecimal with scale=4 for precise percentage calculations.
     * Null if fixed amount fee is used instead.
     */
    @DecimalMin(value = "0.0000", message = "Fee percentage must not be negative")
    @DecimalMax(value = "100.0000", message = "Fee percentage must not exceed 100%")
    @Column(name = "fee_percentage", precision = 8, scale = 4)
    private BigDecimal feePercentage;

    /**
     * Overall fee rate for calculation purposes.
     * Supports both fixed amounts and percentages in unified field.
     * Maps to DIS-INT-RATE from COBOL structure (repurposed for fee rates).
     */
    @NotNull
    @DecimalMin(value = "0.00", message = "Fee rate must not be negative")
    @DecimalMax(value = "9999.99", message = "Fee rate must not exceed 9999.99")
    @Column(name = "fee_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal feeRate;

    /**
     * Minimum balance threshold for fee waiver conditions.
     * If account balance is above this threshold, fee may be waived.
     */
    @DecimalMin(value = "0.00", message = "Minimum balance must not be negative")
    @Column(name = "minimum_balance", precision = 12, scale = 2)
    private BigDecimal minimumBalance;

    /**
     * Date when this fee schedule becomes effective.
     * Used for fee schedule versioning and historical tracking.
     */
    @NotNull
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Date when this fee schedule expires and is no longer applicable.
     * Null means no expiration (indefinite validity).
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Flag indicating whether this fee schedule has waiver conditions.
     * Used for quick filtering of fee schedules with special conditions.
     */
    @Builder.Default
    @Column(name = "has_waiver_conditions", nullable = false)
    private Boolean hasWaiverConditions = false;

    /**
     * Assessment frequency for recurring fees.
     * Examples: MONTHLY, ANNUALLY, PER_TRANSACTION
     */
    @Size(max = 20, message = "Assessment frequency must not exceed 20 characters")
    @Column(name = "assessment_frequency", length = 20)
    private String assessmentFrequency;

    /**
     * Version number for fee schedule versioning and audit tracking.
     * Supports rollback capabilities and change history.
     */
    @Min(value = 1, message = "Version must be at least 1")
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * Textual description of the fee schedule for administrative purposes.
     */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description")
    private String description;

    /**
     * JSON field for storing additional waiver condition parameters.
     * Flexible storage for complex waiver rules and thresholds.
     */
    @Column(name = "waiver_conditions", columnDefinition = "TEXT")
    private String waiverConditions;

    /**
     * Timestamp when the fee schedule record was created.
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    /**
     * Timestamp when the fee schedule record was last modified.
     */
    @Column(name = "last_modified_date")
    private LocalDate lastModifiedDate;

    /**
     * Pre-persist callback to set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDate.now();
        this.lastModifiedDate = LocalDate.now();
    }

    /**
     * Pre-update callback to update modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedDate = LocalDate.now();
    }

    /**
     * Determines if this fee schedule is currently active based on effective and expiration dates.
     * 
     * @param asOfDate the date to check against (typically current date)
     * @return true if the fee schedule is active on the given date
     */
    public boolean isActiveAsOf(LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        
        boolean afterEffective = !asOfDate.isBefore(effectiveDate);
        boolean beforeExpiration = (expirationDate == null) || asOfDate.isBefore(expirationDate);
        
        return afterEffective && beforeExpiration;
    }

    /**
     * Checks if this fee schedule has expired as of the given date.
     * 
     * @param asOfDate the date to check against
     * @return true if the fee schedule has expired
     */
    public boolean isExpiredAsOf(LocalDate asOfDate) {
        if (expirationDate == null) {
            return false;
        }
        return asOfDate.isAfter(expirationDate) || asOfDate.isEqual(expirationDate);
    }

    /**
     * Calculates the applicable fee amount based on the fee schedule configuration.
     * 
     * @param baseAmount the base amount for percentage calculations (e.g., transaction amount)
     * @return the calculated fee amount
     */
    public BigDecimal calculateFeeAmount(BigDecimal baseAmount) {
        if (baseAmount == null) {
            baseAmount = BigDecimal.ZERO;
        }

        // If fixed fee amount is specified, use it
        if (feeAmount != null) {
            return feeAmount;
        }

        // If percentage is specified, calculate percentage of base amount
        if (feePercentage != null) {
            return baseAmount.multiply(feePercentage).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        }

        // Fall back to fee rate
        return feeRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeeSchedule that = (FeeSchedule) o;
        return Objects.equals(feeScheduleId, that.feeScheduleId) &&
               Objects.equals(feeType, that.feeType) &&
               Objects.equals(accountType, that.accountType) &&
               Objects.equals(effectiveDate, that.effectiveDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeScheduleId, feeType, accountType, effectiveDate);
    }

    @Override
    public String toString() {
        return "FeeSchedule{" +
                "feeScheduleId=" + feeScheduleId +
                ", feeType='" + feeType + '\'' +
                ", accountType='" + accountType + '\'' +
                ", feeRate=" + feeRate +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                ", hasWaiverConditions=" + hasWaiverConditions +
                ", version=" + version +
                '}';
    }
}