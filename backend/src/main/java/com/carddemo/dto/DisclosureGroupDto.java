/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Disclosure Group DTO mapping to CVTRA02Y DIS-GROUP-RECORD from COBOL copybook.
 * 
 * This DTO represents the disclosure group information used for interest rate calculations
 * and regulatory compliance in the credit card management system. The disclosure group
 * determines the applicable interest rate based on account group, transaction type, and
 * category code combination.
 * 
 * Field mappings from COBOL copybook CVTRA02Y.cpy:
 * - DIS-ACCT-GROUP-ID PIC X(10) → accountGroupId
 * - DIS-TRAN-TYPE-CD PIC X(02) → transactionTypeCode  
 * - DIS-TRAN-CAT-CD PIC 9(04) → categoryCode
 * - DIS-INT-RATE PIC S9(04)V99 → interestRate
 * 
 * The interestRate field uses BigDecimal with scale 2 to replicate COBOL COMP-3
 * precision behavior, ensuring exact monetary calculations and avoiding floating-point
 * rounding errors in interest rate calculations.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisclosureGroupDto {

    /**
     * Field length constants based on COBOL PIC clauses from CVTRA02Y.cpy
     */
    public static final int GROUP_ID_LENGTH = 10;
    public static final int CATEGORY_CODE_LENGTH = 4;

    /**
     * Account Group ID - 10 character alphanumeric identifier.
     * Maps to DIS-ACCT-GROUP-ID PIC X(10) from CVTRA02Y copybook.
     * Used to categorize accounts for interest rate determination.
     */
    @JsonProperty("accountGroupId")
    private String accountGroupId;

    /**
     * Transaction Type Code - 2 character alphanumeric code.
     * Maps to DIS-TRAN-TYPE-CD PIC X(02) from CVTRA02Y copybook.
     * Identifies the type of transaction for interest calculation purposes.
     */
    @JsonProperty("transactionTypeCode")
    private String transactionTypeCode;

    /**
     * Category Code - 4 digit numeric code.
     * Maps to DIS-TRAN-CAT-CD PIC 9(04) from CVTRA02Y copybook.
     * Provides transaction categorization for regulatory compliance.
     */
    @JsonProperty("categoryCode")
    @Pattern(regexp = "^\\d{4}$", message = "Category code must be exactly 4 digits")
    private String categoryCode;

    /**
     * Interest Rate - decimal value with 2 decimal places.
     * Maps to DIS-INT-RATE PIC S9(04)V99 from CVTRA02Y copybook.
     * 
     * Uses BigDecimal with scale 2 and HALF_UP rounding to replicate
     * COBOL COMP-3 packed decimal precision behavior. This ensures
     * exact monetary calculations without floating-point errors.
     */
    @JsonProperty("interestRate")
    private BigDecimal interestRate;

    /**
     * Validates all fields in this DisclosureGroupDto according to COBOL business rules.
     * Uses ValidationUtil methods to ensure data integrity and compliance.
     * 
     * @throws com.carddemo.exception.ValidationException if any validation fails
     */
    public void validate() {
        // Validate required fields
        ValidationUtil.validateRequiredField("accountGroupId", this.accountGroupId);
        ValidationUtil.validateRequiredField("transactionTypeCode", this.transactionTypeCode);
        ValidationUtil.validateRequiredField("categoryCode", this.categoryCode);
        
        // Validate field lengths
        ValidationUtil.validateFieldLength("accountGroupId", this.accountGroupId, GROUP_ID_LENGTH);
        ValidationUtil.validateFieldLength("transactionTypeCode", this.transactionTypeCode, Constants.TYPE_CODE_LENGTH);
        ValidationUtil.validateFieldLength("categoryCode", this.categoryCode, CATEGORY_CODE_LENGTH);
        
        // Validate numeric field format for category code (must be exactly 4 digits)
        ValidationUtil.validateNumericField("categoryCode", this.categoryCode);
        
        // Additional check to ensure it's exactly 4 digits (matching @Pattern annotation)
        if (this.categoryCode != null && !this.categoryCode.trim().isEmpty()) {
            String trimmedCode = this.categoryCode.trim();
            if (!trimmedCode.matches("^\\d{4}$")) {
                throw new IllegalArgumentException("Category code must be exactly 4 digits");
            }
        }
        
        // Validate interest rate is not null and is properly scaled
        if (this.interestRate == null) {
            throw new IllegalArgumentException("Interest rate must be supplied.");
        }
        
        // Ensure interest rate has proper scale (2 decimal places) matching COBOL PIC S9(04)V99
        if (this.interestRate.scale() > 2) {
            this.interestRate = this.interestRate.setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Creates a new DisclosureGroupDto with validated field values.
     * 
     * @param accountGroupId the account group identifier (10 chars max)
     * @param transactionTypeCode the transaction type code (2 chars max) 
     * @param categoryCode the transaction category code (4 digits)
     * @param interestRate the interest rate with 2 decimal precision
     * @return validated DisclosureGroupDto instance
     */
    public static DisclosureGroupDto createValidated(String accountGroupId, String transactionTypeCode, 
                                                   String categoryCode, BigDecimal interestRate) {
        DisclosureGroupDto dto = new DisclosureGroupDto(accountGroupId, transactionTypeCode, categoryCode, interestRate);
        dto.validate();
        return dto;
    }

    /**
     * Calculates compound interest using this disclosure group's interest rate.
     * Implements financial calculation logic for interest computation.
     * 
     * @param principal the principal amount
     * @param timeInYears the time period in years
     * @return the calculated interest amount with 2 decimal precision
     */
    public BigDecimal calculateInterest(BigDecimal principal, BigDecimal timeInYears) {
        if (principal == null || timeInYears == null || this.interestRate == null) {
            throw new IllegalArgumentException("Principal, time, and interest rate must all be supplied for calculation.");
        }
        
        // Simple interest calculation: principal * rate * time
        // Rate is stored as percentage, so divide by 100
        BigDecimal rateDecimal = this.interestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal interest = principal.multiply(rateDecimal).multiply(timeInYears);
        
        // Return with 2 decimal places matching COBOL monetary precision
        return interest.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if this disclosure group has a valid interest rate for calculations.
     * 
     * @return true if interest rate is valid (not null and non-negative)
     */
    public boolean hasValidInterestRate() {
        return this.interestRate != null && this.interestRate.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Gets the interest rate scaled to 2 decimal places for display purposes.
     * Ensures consistent formatting for UI display and reporting.
     * 
     * @return interest rate formatted to 2 decimal places
     */
    public BigDecimal getFormattedInterestRate() {
        if (this.interestRate == null) {
            return null;
        }
        return this.interestRate.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sets the interest rate with proper scale validation.
     * Ensures the rate is scaled to 2 decimal places matching COBOL precision.
     * 
     * @param interestRate the interest rate to set
     */
    public void setInterestRate(BigDecimal interestRate) {
        if (interestRate != null) {
            this.interestRate = interestRate.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.interestRate = null;
        }
    }

    /**
     * Compares this DisclosureGroupDto with another for equality.
     * Uses BigDecimal.compareTo for accurate decimal comparison.
     * 
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DisclosureGroupDto that = (DisclosureGroupDto) obj;
        
        return Objects.equals(accountGroupId, that.accountGroupId) &&
               Objects.equals(transactionTypeCode, that.transactionTypeCode) &&
               Objects.equals(categoryCode, that.categoryCode) &&
               (interestRate == null ? that.interestRate == null : 
                interestRate.compareTo(that.interestRate) == 0);
    }

    /**
     * Generates hash code for this DisclosureGroupDto.
     * Uses BigDecimal.doubleValue() for consistent hashing.
     * 
     * @return hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountGroupId, transactionTypeCode, categoryCode, 
                          interestRate != null ? interestRate.doubleValue() : null);
    }

    /**
     * Returns string representation of this DisclosureGroupDto.
     * Includes all field values formatted for debugging and logging.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "DisclosureGroupDto{" +
               "accountGroupId='" + accountGroupId + '\'' +
               ", transactionTypeCode='" + transactionTypeCode + '\'' +
               ", categoryCode='" + categoryCode + '\'' +
               ", interestRate=" + (interestRate != null ? interestRate.toPlainString() : "null") +
               '}';
    }
}