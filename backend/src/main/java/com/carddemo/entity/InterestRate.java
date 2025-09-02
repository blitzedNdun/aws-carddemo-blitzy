package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA Entity representing Interest Rate configuration and historical tracking.
 * 
 * This entity manages interest rates for different account groups and transaction categories,
 * supporting both regular APR and promotional rates with effective date ranges. 
 * It provides precise financial calculations compatible with COBOL COMP-3 behavior
 * and maintains audit history for rate changes.
 * 
 * Based on COBOL structure CVTRA02Y (DIS-GROUP-RECORD) and used by interest 
 * calculation program CBACT04C for monthly interest computations.
 */
@Entity
@Table(name = "interest_rates", 
       indexes = {
           @Index(name = "idx_interest_rate_business_key", 
                  columnList = "account_group_id, transaction_type_code, transaction_category_code"),
           @Index(name = "idx_interest_rate_effective_date", 
                  columnList = "effective_date, expiration_date"),
           @Index(name = "idx_interest_rate_active", 
                  columnList = "effective_date, expiration_date")
       })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRate implements Serializable {

    private static final long serialVersionUID = 1L;
    
    /**
     * Scale for financial calculations matching COBOL COMP-3 S9(04)V99 precision
     */
    private static final int FINANCIAL_SCALE = 2;
    
    /**
     * Scale for percentage rates (APR) - 4 decimal places for precise rate calculations
     */
    private static final int RATE_SCALE = 4;
    
    /**
     * Days in year for daily rate calculations
     */
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id")
    private Long rateId;

    /**
     * Account Group ID - corresponds to DIS-ACCT-GROUP-ID in COBOL copybook
     * Maximum 10 characters as per COBOL PIC X(10) definition
     */
    @NotBlank(message = "Account Group ID is required")
    @Size(max = 10, message = "Account Group ID must not exceed 10 characters")
    @Column(name = "account_group_id", length = 10, nullable = false)
    private String accountGroupId;

    /**
     * Transaction Type Code - corresponds to DIS-TRAN-TYPE-CD in COBOL copybook
     * Exactly 2 characters as per COBOL PIC X(02) definition
     */
    @NotBlank(message = "Transaction Type Code is required")
    @Size(min = 2, max = 2, message = "Transaction Type Code must be exactly 2 characters")
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    private String transactionTypeCode;

    /**
     * Transaction Category Code - corresponds to DIS-TRAN-CAT-CD in COBOL copybook
     * 4-digit code as per COBOL PIC 9(04) definition
     */
    @NotNull(message = "Transaction Category Code is required")
    @Min(value = 1, message = "Transaction Category Code must be positive")
    @Max(value = 9999, message = "Transaction Category Code must not exceed 4 digits")
    @Column(name = "transaction_category_code", nullable = false)
    private Integer transactionCategoryCode;

    /**
     * Current Annual Percentage Rate (APR) 
     * Stored with 4 decimal places precision for accurate financial calculations
     * Range: 0.0000% to 99.9999% (0.0000 to 0.9999 as decimal)
     */
    @NotNull(message = "Current APR is required")
    @DecimalMin(value = "0.0000", message = "Current APR must be non-negative")
    @DecimalMax(value = "0.9999", message = "Current APR must not exceed 99.99%")
    @Column(name = "current_apr", precision = 6, scale = 4, nullable = false)
    private BigDecimal currentApr;

    /**
     * Promotional Rate (if applicable)
     * Null if no promotional rate is active
     * Same precision as current APR for consistency
     */
    @DecimalMin(value = "0.0000", message = "Promotional rate must be non-negative")
    @DecimalMax(value = "0.9999", message = "Promotional rate must not exceed 99.99%")
    @Column(name = "promotional_rate", precision = 6, scale = 4)
    private BigDecimal promotionalRate;

    /**
     * Effective Date when this rate configuration becomes active
     */
    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Expiration Date when this rate configuration expires
     * Can be null for indefinite rates
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Pre-calculated daily rate for efficient interest calculations
     * Automatically computed from current APR divided by 365 days
     * Scale matches COBOL financial precision requirements
     */
    @Column(name = "daily_rate", precision = 10, scale = 6)
    private BigDecimal dailyRate;

    // Getters
    public Long getRateId() {
        return rateId;
    }

    public String getAccountGroupId() {
        return accountGroupId;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public Integer getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    public BigDecimal getCurrentApr() {
        return currentApr;
    }

    public BigDecimal getPromotionalRate() {
        return promotionalRate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    // Setters with automatic daily rate calculation
    public void setCurrentApr(BigDecimal currentApr) {
        this.currentApr = currentApr;
        this.dailyRate = calculateDailyRate(currentApr);
    }

    public void setPromotionalRate(BigDecimal promotionalRate) {
        this.promotionalRate = promotionalRate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Calculate daily rate from APR using COBOL-compatible precision
     * Replicates the calculation logic from CBACT04C where monthly rate = annual / 12
     * Daily rate = annual / 365 with proper rounding for financial accuracy
     */
    private BigDecimal calculateDailyRate(BigDecimal apr) {
        if (apr == null) {
            return null;
        }
        return apr.divide(DAYS_IN_YEAR, 6, RoundingMode.HALF_UP);
    }

    /**
     * Get the effective rate to use for calculations, considering promotional rates
     * @return promotional rate if active and valid, otherwise current APR
     */
    public BigDecimal getEffectiveRate() {
        LocalDate today = LocalDate.now();
        
        // Check if promotional rate is active
        if (promotionalRate != null && 
            (effectiveDate == null || !today.isBefore(effectiveDate)) &&
            (expirationDate == null || !today.isAfter(expirationDate))) {
            return promotionalRate;
        }
        
        return currentApr;
    }

    /**
     * Get the effective daily rate for interest calculations
     * @return daily rate based on effective rate (promotional or current)
     */
    public BigDecimal getEffectiveDailyRate() {
        BigDecimal effectiveRate = getEffectiveRate();
        return calculateDailyRate(effectiveRate);
    }

    /**
     * Check if this interest rate configuration is currently active
     * @return true if current date is within effective date range
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        boolean afterEffective = effectiveDate == null || !today.isBefore(effectiveDate);
        boolean beforeExpiration = expirationDate == null || !today.isAfter(expirationDate);
        return afterEffective && beforeExpiration;
    }

    /**
     * Check if promotional rate is currently active
     * @return true if promotional rate exists and is within valid date range
     */
    public boolean isPromotionalRateActive() {
        return promotionalRate != null && isActive();
    }

    /**
     * Calculate monthly interest using COBOL algorithm from CBACT04C
     * Formula: (balance * rate) / 1200 (12 months * 100 for percentage)
     * 
     * @param balance the account balance to calculate interest on
     * @return monthly interest amount with COBOL-compatible precision
     */
    public BigDecimal calculateMonthlyInterest(BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal effectiveRate = getEffectiveRate();
        if (effectiveRate == null) {
            return BigDecimal.ZERO;
        }
        
        // Convert APR percentage to basis points for COBOL compatibility
        // COBOL stores as S9(04)V99, so 18.25% = 1825 (basis points) / 100 = 18.25
        BigDecimal rateInBasisPoints = effectiveRate.multiply(new BigDecimal("10000"));
        
        // Apply COBOL formula: (balance * rate) / 1200
        // 1200 = 12 months * 100 (to convert from basis points)
        BigDecimal monthlyInterest = balance
            .multiply(rateInBasisPoints)
            .divide(new BigDecimal("1200"), FINANCIAL_SCALE, RoundingMode.HALF_UP);
            
        return monthlyInterest;
    }

    /**
     * JPA lifecycle callback to ensure daily rate is calculated before persistence
     */
    @PrePersist
    @PreUpdate
    protected void calculateDailyRateOnSave() {
        if (currentApr != null) {
            this.dailyRate = calculateDailyRate(currentApr);
        }
    }

    /**
     * Create a new builder instance for fluent object construction
     * @return new InterestRateBuilder instance
     */
    public static InterestRateBuilder builder() {
        return new InterestRateBuilder();
    }

    /**
     * Custom builder class for InterestRate entity with validation
     */
    public static class InterestRateBuilder {
        private Long rateId;
        private String accountGroupId;
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
        private BigDecimal currentApr;
        private BigDecimal promotionalRate;
        private LocalDate effectiveDate;
        private LocalDate expirationDate;
        private BigDecimal dailyRate;

        public InterestRateBuilder rateId(Long rateId) {
            this.rateId = rateId;
            return this;
        }

        public InterestRateBuilder accountGroupId(String accountGroupId) {
            this.accountGroupId = accountGroupId;
            return this;
        }

        public InterestRateBuilder transactionTypeCode(String transactionTypeCode) {
            this.transactionTypeCode = transactionTypeCode;
            return this;
        }

        public InterestRateBuilder transactionCategoryCode(Integer transactionCategoryCode) {
            this.transactionCategoryCode = transactionCategoryCode;
            return this;
        }

        public InterestRateBuilder currentApr(BigDecimal currentApr) {
            this.currentApr = currentApr;
            return this;
        }

        public InterestRateBuilder promotionalRate(BigDecimal promotionalRate) {
            this.promotionalRate = promotionalRate;
            return this;
        }

        public InterestRateBuilder effectiveDate(LocalDate effectiveDate) {
            this.effectiveDate = effectiveDate;
            return this;
        }

        public InterestRateBuilder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public InterestRate build() {
            InterestRate interestRate = new InterestRate();
            interestRate.rateId = this.rateId;
            interestRate.accountGroupId = this.accountGroupId;
            interestRate.transactionTypeCode = this.transactionTypeCode;
            interestRate.transactionCategoryCode = this.transactionCategoryCode;
            interestRate.currentApr = this.currentApr;
            interestRate.promotionalRate = this.promotionalRate;
            interestRate.effectiveDate = this.effectiveDate;
            interestRate.expirationDate = this.expirationDate;
            
            // Calculate daily rate if APR is provided
            if (this.currentApr != null) {
                interestRate.dailyRate = interestRate.calculateDailyRate(this.currentApr);
            }
            
            return interestRate;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterestRate that = (InterestRate) o;
        return Objects.equals(rateId, that.rateId) &&
               Objects.equals(accountGroupId, that.accountGroupId) &&
               Objects.equals(transactionTypeCode, that.transactionTypeCode) &&
               Objects.equals(transactionCategoryCode, that.transactionCategoryCode) &&
               Objects.equals(currentApr, that.currentApr) &&
               Objects.equals(promotionalRate, that.promotionalRate) &&
               Objects.equals(effectiveDate, that.effectiveDate) &&
               Objects.equals(expirationDate, that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rateId, accountGroupId, transactionTypeCode, 
                           transactionCategoryCode, currentApr, promotionalRate, 
                           effectiveDate, expirationDate);
    }

    @Override
    public String toString() {
        return "InterestRate{" +
                "rateId=" + rateId +
                ", accountGroupId='" + accountGroupId + '\'' +
                ", transactionTypeCode='" + transactionTypeCode + '\'' +
                ", transactionCategoryCode=" + transactionCategoryCode +
                ", currentApr=" + currentApr +
                ", promotionalRate=" + promotionalRate +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                ", dailyRate=" + dailyRate +
                ", isActive=" + isActive() +
                ", isPromotionalActive=" + isPromotionalRateActive() +
                '}';
    }
}