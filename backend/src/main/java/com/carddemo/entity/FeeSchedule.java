package com.carddemo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * FeeSchedule Entity
 * 
 * JPA entity representing fee schedule configuration including fee rates, 
 * waiver conditions, and assessment rules. Defines fee calculation parameters 
 * for different account types and fee categories. Supports versioning and 
 * effective date ranges for fee schedule changes and regulatory compliance.
 * 
 * This entity supports the fee computation functionality referenced in CBACT04C.cbl
 * and provides structured fee configuration based on the disclosure group pattern
 * from CVTRA02Y.cpy.
 */
@Entity
@Table(name = "fee_schedule")
public class FeeSchedule {

    /**
     * Fee Type Classification enumeration
     * Defines the different types of fees that can be configured
     */
    public enum FeeType {
        ANNUAL,              // Annual membership fees
        LATE_PAYMENT,        // Late payment penalties
        OVER_LIMIT,          // Over credit limit fees
        FOREIGN_TRANSACTION, // Foreign transaction fees
        MAINTENANCE          // Account maintenance fees
    }

    /**
     * Assessment Frequency enumeration
     * Defines when and how often fees are assessed
     */
    public enum AssessmentFrequency {
        MONTHLY,        // Monthly assessment
        ANNUALLY,       // Annual assessment
        PER_TRANSACTION // Per transaction assessment
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_schedule_id")
    private Long feeScheduleId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @Size(max = 10)
    @Column(name = "account_type", length = 10)
    private String accountType;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "999999.99")
    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    @Column(name = "fee_percentage", precision = 5, scale = 4)
    private BigDecimal feePercentage;

    @Size(max = 500)
    @Column(name = "waiver_conditions", length = 500)
    private String waiverConditions;

    @NotNull
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_frequency", nullable = false, length = 15)
    private AssessmentFrequency assessmentFrequency;

    @DecimalMin(value = "0.00")
    @Column(name = "waiver_threshold", precision = 12, scale = 2)
    private BigDecimal waiverThreshold;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Size(max = 4)
    @Column(name = "transaction_category_code", length = 4)
    private String transactionCategoryCode;

    @Size(max = 2)
    @Column(name = "transaction_type_code", length = 2)
    private String transactionTypeCode;

    /**
     * Default constructor
     */
    public FeeSchedule() {
        this.isActive = true;
        this.effectiveDate = LocalDate.now();
    }

    /**
     * Constructor with required fields
     */
    public FeeSchedule(FeeType feeType, AssessmentFrequency assessmentFrequency, LocalDate effectiveDate) {
        this();
        this.feeType = feeType;
        this.assessmentFrequency = assessmentFrequency;
        this.effectiveDate = effectiveDate;
    }

    // Getters and Setters

    public Long getFeeScheduleId() {
        return feeScheduleId;
    }

    public void setFeeScheduleId(Long feeScheduleId) {
        this.feeScheduleId = feeScheduleId;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public void setFeePercentage(BigDecimal feePercentage) {
        this.feePercentage = feePercentage;
    }

    public String getWaiverConditions() {
        return waiverConditions;
    }

    public void setWaiverConditions(String waiverConditions) {
        this.waiverConditions = waiverConditions;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public AssessmentFrequency getAssessmentFrequency() {
        return assessmentFrequency;
    }

    public void setAssessmentFrequency(AssessmentFrequency assessmentFrequency) {
        this.assessmentFrequency = assessmentFrequency;
    }

    public BigDecimal getWaiverThreshold() {
        return waiverThreshold;
    }

    public void setWaiverThreshold(BigDecimal waiverThreshold) {
        this.waiverThreshold = waiverThreshold;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    public void setTransactionCategoryCode(String transactionCategoryCode) {
        this.transactionCategoryCode = transactionCategoryCode;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    /**
     * Business logic method to check if this fee schedule is currently active
     * and within its effective date range.
     * 
     * @param checkDate The date to check against
     * @return true if the fee schedule is active and effective on the given date
     */
    public boolean isEffectiveOn(LocalDate checkDate) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        
        if (effectiveDate != null && checkDate.isBefore(effectiveDate)) {
            return false;
        }
        
        if (expirationDate != null && checkDate.isAfter(expirationDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Business logic method to determine if waiver conditions are met
     * based on account balance or transaction amount.
     * 
     * @param checkAmount The amount to check against waiver threshold
     * @return true if fee should be waived based on threshold
     */
    public boolean isWaiverApplicable(BigDecimal checkAmount) {
        if (waiverThreshold == null || checkAmount == null) {
            return false;
        }
        
        return checkAmount.compareTo(waiverThreshold) >= 0;
    }

    /**
     * Calculate the applicable fee amount based on the fee configuration.
     * Supports both fixed amount and percentage-based fees.
     * 
     * @param baseAmount The base amount for percentage calculations
     * @return The calculated fee amount, or null if no fee applies
     */
    public BigDecimal calculateFeeAmount(BigDecimal baseAmount) {
        // Check if fee is waived
        if (isWaiverApplicable(baseAmount)) {
            return BigDecimal.ZERO;
        }
        
        // Return fixed fee amount if specified
        if (feeAmount != null) {
            return feeAmount;
        }
        
        // Calculate percentage-based fee
        if (feePercentage != null && baseAmount != null) {
            return baseAmount.multiply(feePercentage.divide(new BigDecimal("100")))
                           .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        FeeSchedule that = (FeeSchedule) obj;
        return Objects.equals(feeScheduleId, that.feeScheduleId) &&
               Objects.equals(feeType, that.feeType) &&
               Objects.equals(accountType, that.accountType) &&
               Objects.equals(effectiveDate, that.effectiveDate) &&
               Objects.equals(assessmentFrequency, that.assessmentFrequency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeScheduleId, feeType, accountType, effectiveDate, assessmentFrequency);
    }

    @Override
    public String toString() {
        return "FeeSchedule{" +
                "feeScheduleId=" + feeScheduleId +
                ", feeType=" + feeType +
                ", accountType='" + accountType + '\'' +
                ", feeAmount=" + feeAmount +
                ", feePercentage=" + feePercentage +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                ", assessmentFrequency=" + assessmentFrequency +
                ", isActive=" + isActive +
                '}';
    }
}