package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Response DTO for interest rate queries containing current interest rates by category and disclosure group.
 * Returns structured rate information matching VSAM DISCGRP record layout with BigDecimal precision 
 * for rate values. Maps to COBOL DIS-GROUP-RECORD structure from CVTRA02Y copybook.
 * 
 * This DTO supports the GET /api/interest/rates endpoint response body and maintains
 * exact precision matching COBOL COMP-3 packed decimal behavior for financial calculations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateResponse {

    /**
     * Transaction category code matching COBOL DIS-TRAN-CAT-CD (PIC 9(04)).
     * Represents the specific transaction category for interest rate calculation.
     */
    @JsonProperty("categoryCode")
    @NotNull(message = "Category code is required")
    @Size(max = 4, message = "Category code must not exceed 4 characters")
    private String categoryCode;

    /**
     * Transaction category description providing human-readable name for the category.
     * Derived from transaction category master data for display purposes.
     */
    @JsonProperty("categoryDesc")
    @Size(max = 50, message = "Category description must not exceed 50 characters")
    private String categoryDesc;

    /**
     * Base interest rate matching COBOL DIS-INT-RATE (PIC S9(04)V99).
     * Configured with scale 5,5 to maintain exact precision equivalent to COBOL packed decimal.
     * Used in monthly interest calculation: (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
     */
    @JsonProperty("baseInterestRate")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Digits(integer = 5, fraction = 5, message = "Base interest rate must have at most 5 integer and 5 fractional digits")
    private BigDecimal baseInterestRate;

    /**
     * Promotional interest rate for special offers or temporary rate adjustments.
     * Maintains same precision as base rate for consistent financial calculations.
     */
    @JsonProperty("promotionalRate")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Digits(integer = 5, fraction = 5, message = "Promotional rate must have at most 5 integer and 5 fractional digits")
    private BigDecimal promotionalRate;

    /**
     * Effective date when the interest rate becomes active.
     * Used for temporal rate management and historical tracking.
     */
    @JsonProperty("effectiveDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    /**
     * Expiration date when the interest rate is no longer valid.
     * Used for rate lifecycle management and automatic rate changes.
     */
    @JsonProperty("expirationDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    /**
     * Custom getter for categoryCode ensuring proper validation.
     * 
     * @return the category code as a String
     */
    public String getCategoryCode() {
        return categoryCode;
    }

    /**
     * Custom setter for categoryCode with validation and formatting.
     * Ensures the code is properly formatted to match COBOL field requirements.
     * 
     * @param categoryCode the category code to set
     */
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode != null ? categoryCode.trim() : null;
    }

    /**
     * Custom getter for categoryDesc ensuring proper formatting.
     * 
     * @return the category description as a String
     */
    public String getCategoryDesc() {
        return categoryDesc;
    }

    /**
     * Custom setter for categoryDesc with proper trimming.
     * 
     * @param categoryDesc the category description to set
     */
    public void setCategoryDesc(String categoryDesc) {
        this.categoryDesc = categoryDesc != null ? categoryDesc.trim() : null;
    }

    /**
     * Custom getter for baseInterestRate ensuring proper precision.
     * 
     * @return the base interest rate as BigDecimal
     */
    public BigDecimal getBaseInterestRate() {
        return baseInterestRate;
    }

    /**
     * Custom setter for baseInterestRate with precision validation.
     * Ensures the rate maintains COBOL COMP-3 equivalent precision.
     * 
     * @param baseInterestRate the base interest rate to set
     */
    public void setBaseInterestRate(BigDecimal baseInterestRate) {
        this.baseInterestRate = baseInterestRate;
    }

    /**
     * Custom getter for promotionalRate ensuring proper precision.
     * 
     * @return the promotional rate as BigDecimal
     */
    public BigDecimal getPromotionalRate() {
        return promotionalRate;
    }

    /**
     * Custom setter for promotionalRate with precision validation.
     * 
     * @param promotionalRate the promotional rate to set
     */
    public void setPromotionalRate(BigDecimal promotionalRate) {
        this.promotionalRate = promotionalRate;
    }

    /**
     * Custom getter for effectiveDate.
     * 
     * @return the effective date as LocalDate
     */
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    /**
     * Custom setter for effectiveDate.
     * 
     * @param effectiveDate the effective date to set
     */
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    /**
     * Custom getter for expirationDate.
     * 
     * @return the expiration date as LocalDate
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    /**
     * Custom setter for expirationDate.
     * 
     * @param expirationDate the expiration date to set
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Provides a string representation of the InterestRateResponse.
     * Includes all fields in a readable format for debugging and logging.
     * 
     * @return string representation of the object
     */
    @Override
    public String toString() {
        return "InterestRateResponse{" +
                "categoryCode='" + categoryCode + '\'' +
                ", categoryDesc='" + categoryDesc + '\'' +
                ", baseInterestRate=" + baseInterestRate +
                ", promotionalRate=" + promotionalRate +
                ", effectiveDate=" + effectiveDate +
                ", expirationDate=" + expirationDate +
                '}';
    }

    /**
     * Compares this InterestRateResponse with another object for equality.
     * Two responses are equal if all their fields are equal.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterestRateResponse that = (InterestRateResponse) o;
        return Objects.equals(categoryCode, that.categoryCode) &&
                Objects.equals(categoryDesc, that.categoryDesc) &&
                Objects.equals(baseInterestRate, that.baseInterestRate) &&
                Objects.equals(promotionalRate, that.promotionalRate) &&
                Objects.equals(effectiveDate, that.effectiveDate) &&
                Objects.equals(expirationDate, that.expirationDate);
    }

    /**
     * Generates a hash code for this InterestRateResponse.
     * The hash code is based on all fields for proper hash table behavior.
     * 
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(categoryCode, categoryDesc, baseInterestRate, 
                           promotionalRate, effectiveDate, expirationDate);
    }
}