package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA entity class representing fee records for credit card accounts.
 * This entity supports multiple fee types including late fees, over-limit fees,
 * annual fees, and foreign transaction fees with fee calculation and posting
 * tracking capabilities.
 * 
 * Migrated from COBOL fee processing logic found in CBACT04C.cbl
 * where fee computation is implemented in the 1400-COMPUTE-FEES section.
 */
@Entity
@Table(name = "fees", indexes = {
    @Index(name = "idx_fee_account_id", columnList = "account_id"),
    @Index(name = "idx_fee_assessment_date", columnList = "assessment_date"),
    @Index(name = "idx_fee_status", columnList = "fee_status")
})
public class Fee {

    /**
     * Enumeration for fee types supported by the system.
     * Maps to COBOL fee type codes in the original batch processing.
     */
    public enum FeeType {
        LATE_PAYMENT,
        OVER_LIMIT,
        ANNUAL,
        FOREIGN_TRANSACTION
    }

    /**
     * Enumeration for fee status tracking through the fee lifecycle.
     * Supports fee dispute and reversal operations as required.
     */
    public enum FeeStatus {
        ASSESSED,
        POSTED,
        WAIVED,
        DISPUTED,
        REVERSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_id")
    private Long id;

    @NotNull(message = "Account ID is required")
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @NotNull(message = "Fee type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.00", message = "Fee amount must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Fee amount must have at most 10 integer digits and 2 decimal places")
    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @NotNull(message = "Assessment date is required")
    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @NotNull(message = "Fee status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_status", nullable = false, length = 15)
    private FeeStatus feeStatus;

    @Size(max = 100, message = "Calculation reference must not exceed 100 characters")
    @Column(name = "calculation_reference", length = 100)
    private String calculationReference;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Default constructor required by JPA.
     */
    public Fee() {
        this.feeStatus = FeeStatus.ASSESSED; // Default status when fee is created
    }

    /**
     * Constructor for creating a new fee with basic required fields.
     * 
     * @param accountId The account ID to which this fee applies
     * @param feeType The type of fee being assessed
     * @param feeAmount The monetary amount of the fee
     * @param assessmentDate The date when the fee was assessed
     */
    public Fee(Long accountId, FeeType feeType, BigDecimal feeAmount, LocalDate assessmentDate) {
        this();
        this.accountId = accountId;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
        this.assessmentDate = assessmentDate;
    }

    // Getter and Setter methods for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public FeeStatus getFeeStatus() {
        return feeStatus;
    }

    public void setFeeStatus(FeeStatus feeStatus) {
        this.feeStatus = feeStatus;
    }

    public String getCalculationReference() {
        return calculationReference;
    }

    public void setCalculationReference(String calculationReference) {
        this.calculationReference = calculationReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Business method to mark a fee as posted.
     * Updates the fee status and sets the posting date.
     * 
     * @param postingDate The date when the fee was posted
     */
    public void markAsPosted(LocalDate postingDate) {
        this.feeStatus = FeeStatus.POSTED;
        this.postingDate = postingDate;
    }

    /**
     * Business method to waive a fee.
     * Updates the fee status to waived.
     */
    public void waiveFee() {
        this.feeStatus = FeeStatus.WAIVED;
    }

    /**
     * Business method to dispute a fee.
     * Updates the fee status to disputed.
     */
    public void disputeFee() {
        this.feeStatus = FeeStatus.DISPUTED;
    }

    /**
     * Business method to reverse a fee.
     * Updates the fee status to reversed.
     */
    public void reverseFee() {
        this.feeStatus = FeeStatus.REVERSED;
    }

    /**
     * Checks if the fee has been posted.
     * 
     * @return true if the fee status is POSTED, false otherwise
     */
    public boolean isPosted() {
        return this.feeStatus == FeeStatus.POSTED;
    }

    /**
     * Checks if the fee is in a final state (posted, waived, or reversed).
     * 
     * @return true if the fee is in a final state, false otherwise
     */
    public boolean isFinalState() {
        return this.feeStatus == FeeStatus.POSTED || 
               this.feeStatus == FeeStatus.WAIVED || 
               this.feeStatus == FeeStatus.REVERSED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fee fee = (Fee) o;
        return Objects.equals(id, fee.id) &&
               Objects.equals(accountId, fee.accountId) &&
               feeType == fee.feeType &&
               Objects.equals(feeAmount, fee.feeAmount) &&
               Objects.equals(assessmentDate, fee.assessmentDate) &&
               Objects.equals(postingDate, fee.postingDate) &&
               feeStatus == fee.feeStatus &&
               Objects.equals(calculationReference, fee.calculationReference) &&
               Objects.equals(description, fee.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountId, feeType, feeAmount, assessmentDate, 
                           postingDate, feeStatus, calculationReference, description);
    }

    @Override
    public String toString() {
        return "Fee{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", feeType=" + feeType +
                ", feeAmount=" + feeAmount +
                ", assessmentDate=" + assessmentDate +
                ", postingDate=" + postingDate +
                ", feeStatus=" + feeStatus +
                ", calculationReference='" + calculationReference + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}