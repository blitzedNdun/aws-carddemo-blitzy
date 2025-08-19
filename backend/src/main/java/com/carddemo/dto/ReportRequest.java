/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Request DTO for report generation mapping the CORPT00 BMS screen functionality.
 * This class translates the COBOL BMS map structure to a modern Java DTO while preserving
 * all original field validation rules and business logic.
 * 
 * Supports three report types:
 * - MONTHLY: Current month transaction report
 * - YEARLY: Current year transaction report  
 * - CUSTOM: User-defined date range report
 * 
 * Date components are captured separately (month/day/year) to match the original
 * BMS field structure where users enter MM/DD/YYYY values in separate fields.
 * 
 * Includes comprehensive validation for date ranges and report type selection,
 * maintaining exact compatibility with COBOL validation logic from CORPT00 screen.
 */
@NoArgsConstructor
public class ReportRequest {

    /**
     * Enumeration of supported report types mapping to COBOL BMS options.
     * Corresponds to MONTHLY, YEARLY, and CUSTOM selection fields in CORPT00.bms.
     */
    public enum ReportType {
        /**
         * Monthly report for current month transactions.
         */
        @JsonProperty("MONTHLY")
        MONTHLY,
        
        /**
         * Yearly report for current year transactions.
         */
        @JsonProperty("YEARLY") 
        YEARLY,
        
        /**
         * Custom date range report using start and end dates.
         */
        @JsonProperty("CUSTOM")
        CUSTOM
    }

    /**
     * The selected report type (MONTHLY, YEARLY, or CUSTOM).
     * Maps to the MONTHLY, YEARLY, CUSTOM single-character fields in CORPT00.bms.
     */
    @JsonProperty("reportType")
    private ReportType reportType;

    /**
     * Start date month component (1-12).
     * Maps to SDTMM field in CORPT00.bms with NUM attribute and 2-character length.
     */
    @JsonProperty("startMonth")
    @Max(value = 12, message = "Start month must be between 1 and 12")
    private Integer startMonth;

    /**
     * Start date day component (1-31).
     * Maps to SDTDD field in CORPT00.bms with NUM attribute and 2-character length.
     */
    @JsonProperty("startDay")
    @Max(value = 31, message = "Start day must be between 1 and 31")
    private Integer startDay;

    /**
     * Start date year component (CCYY format).
     * Maps to SDTYYYY field in CORPT00.bms with NUM attribute and 4-character length.
     */
    @JsonProperty("startYear")
    private Integer startYear;

    /**
     * End date month component (1-12).
     * Maps to EDTMM field in CORPT00.bms with NUM attribute and 2-character length.
     */
    @JsonProperty("endMonth")
    @Max(value = 12, message = "End month must be between 1 and 12")
    private Integer endMonth;

    /**
     * End date day component (1-31).
     * Maps to EDTDD field in CORPT00.bms with NUM attribute and 2-character length.
     */
    @JsonProperty("endDay")
    @Max(value = 31, message = "End day must be between 1 and 31")
    private Integer endDay;

    /**
     * End date year component (CCYY format).
     * Maps to EDTYYYY field in CORPT00.bms with NUM attribute and 4-character length.
     */
    @JsonProperty("endYear")
    private Integer endYear;

    /**
     * Print confirmation flag.
     * Maps to CONFIRM field in CORPT00.bms for user confirmation (Y/N).
     */
    @JsonProperty("confirmPrint")
    private Boolean confirmPrint;

    // Getter and Setter methods

    /**
     * Gets the selected report type.
     * 
     * @return the report type (MONTHLY, YEARLY, or CUSTOM)
     */
    public ReportType getReportType() {
        return reportType;
    }

    /**
     * Sets the report type.
     * 
     * @param reportType the report type to set
     */
    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    /**
     * Gets the start date month component.
     * 
     * @return the start month (1-12)
     */
    public Integer getStartMonth() {
        return startMonth;
    }

    /**
     * Sets the start date month component.
     * 
     * @param startMonth the start month to set (1-12)
     */
    public void setStartMonth(Integer startMonth) {
        this.startMonth = startMonth;
    }

    /**
     * Gets the start date day component.
     * 
     * @return the start day (1-31)
     */
    public Integer getStartDay() {
        return startDay;
    }

    /**
     * Sets the start date day component.
     * 
     * @param startDay the start day to set (1-31)
     */
    public void setStartDay(Integer startDay) {
        this.startDay = startDay;
    }

    /**
     * Gets the start date year component.
     * 
     * @return the start year (CCYY format)
     */
    public Integer getStartYear() {
        return startYear;
    }

    /**
     * Sets the start date year component.
     * 
     * @param startYear the start year to set (CCYY format)
     */
    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    /**
     * Gets the end date month component.
     * 
     * @return the end month (1-12)
     */
    public Integer getEndMonth() {
        return endMonth;
    }

    /**
     * Sets the end date month component.
     * 
     * @param endMonth the end month to set (1-12)
     */
    public void setEndMonth(Integer endMonth) {
        this.endMonth = endMonth;
    }

    /**
     * Gets the end date day component.
     * 
     * @return the end day (1-31)
     */
    public Integer getEndDay() {
        return endDay;
    }

    /**
     * Sets the end date day component.
     * 
     * @param endDay the end day to set (1-31)
     */
    public void setEndDay(Integer endDay) {
        this.endDay = endDay;
    }

    /**
     * Gets the end date year component.
     * 
     * @return the end year (CCYY format)
     */
    public Integer getEndYear() {
        return endYear;
    }

    /**
     * Sets the end date year component.
     * 
     * @param endYear the end year to set (CCYY format)
     */
    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    /**
     * Gets the print confirmation flag.
     * 
     * @return true if print is confirmed, false otherwise
     */
    public Boolean getConfirmPrint() {
        return confirmPrint;
    }

    /**
     * Sets the print confirmation flag.
     * 
     * @param confirmPrint the confirmation flag to set
     */
    public void setConfirmPrint(Boolean confirmPrint) {
        this.confirmPrint = confirmPrint;
    }

    // Business Logic Methods

    /**
     * Validates the date range ensuring start date is before end date.
     * Uses DateConversionUtil and ValidationUtil for comprehensive validation
     * matching COBOL date validation logic from CORPT00 screen processing.
     * 
     * @throws IllegalArgumentException if date range validation fails
     */
    public void validateDateRange() {
        // For CUSTOM reports, validate date range
        if (reportType == ReportType.CUSTOM) {
            // Validate required fields using ValidationUtil
            ValidationUtil.validateRequiredField("startMonth", startMonth != null ? startMonth.toString() : null);
            ValidationUtil.validateRequiredField("startDay", startDay != null ? startDay.toString() : null);
            ValidationUtil.validateRequiredField("startYear", startYear != null ? startYear.toString() : null);
            ValidationUtil.validateRequiredField("endMonth", endMonth != null ? endMonth.toString() : null);
            ValidationUtil.validateRequiredField("endDay", endDay != null ? endDay.toString() : null);
            ValidationUtil.validateRequiredField("endYear", endYear != null ? endYear.toString() : null);

            // Validate numeric fields using ValidationUtil
            ValidationUtil.validateNumericField("startMonth", startMonth.toString());
            ValidationUtil.validateNumericField("startDay", startDay.toString());
            ValidationUtil.validateNumericField("startYear", startYear.toString());
            ValidationUtil.validateNumericField("endMonth", endMonth.toString());
            ValidationUtil.validateNumericField("endDay", endDay.toString());
            ValidationUtil.validateNumericField("endYear", endYear.toString());

            // Build date strings in CCYYMMDD format for validation
            String startDateStr = String.format("%04d%02d%02d", startYear, startMonth, startDay);
            String endDateStr = String.format("%04d%02d%02d", endYear, endMonth, endDay);

            // Use DateConversionUtil to validate individual dates
            if (!DateConversionUtil.validateDate(startDateStr)) {
                throw new IllegalArgumentException("Invalid start date: " + startDateStr);
            }
            
            if (!DateConversionUtil.validateDate(endDateStr)) {
                throw new IllegalArgumentException("Invalid end date: " + endDateStr);
            }

            // Validate that start date is before or equal to end date
            LocalDate startDate = DateConversionUtil.parseDate(startDateStr);
            LocalDate endDate = DateConversionUtil.parseDate(endDateStr);
            
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        }
    }

    /**
     * Gets the start date as a LocalDate object.
     * Converts month/day/year components to LocalDate using DateConversionUtil.
     * 
     * @return the start date as LocalDate, or null if components are missing
     */
    public LocalDate getStartDate() {
        if (startYear != null && startMonth != null && startDay != null) {
            String dateStr = String.format("%04d%02d%02d", startYear, startMonth, startDay);
            return DateConversionUtil.parseDate(dateStr);
        }
        return null;
    }

    /**
     * Gets the end date as a LocalDate object.
     * Converts month/day/year components to LocalDate using DateConversionUtil.
     * 
     * @return the end date as LocalDate, or null if components are missing
     */
    public LocalDate getEndDate() {
        if (endYear != null && endMonth != null && endDay != null) {
            String dateStr = String.format("%04d%02d%02d", endYear, endMonth, endDay);
            return DateConversionUtil.parseDate(dateStr);
        }
        return null;
    }

    // Object methods

    /**
     * Checks equality of ReportRequest objects.
     * Two ReportRequest objects are considered equal if all their fields match.
     * 
     * @param obj the object to compare
     * @return true if objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ReportRequest that = (ReportRequest) obj;
        return Objects.equals(reportType, that.reportType) &&
               Objects.equals(startMonth, that.startMonth) &&
               Objects.equals(startDay, that.startDay) &&
               Objects.equals(startYear, that.startYear) &&
               Objects.equals(endMonth, that.endMonth) &&
               Objects.equals(endDay, that.endDay) &&
               Objects.equals(endYear, that.endYear) &&
               Objects.equals(confirmPrint, that.confirmPrint);
    }

    /**
     * Generates hash code for ReportRequest objects.
     * Uses all fields to generate a consistent hash code.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(reportType, startMonth, startDay, startYear, 
                          endMonth, endDay, endYear, confirmPrint);
    }

    /**
     * Returns string representation of ReportRequest.
     * Includes all field values for debugging and logging purposes.
     * 
     * @return string representation of the object
     */
    @Override
    public String toString() {
        return "ReportRequest{" +
               "reportType=" + reportType +
               ", startMonth=" + startMonth +
               ", startDay=" + startDay +
               ", startYear=" + startYear +
               ", endMonth=" + endMonth +
               ", endDay=" + endDay +
               ", endYear=" + endYear +
               ", confirmPrint=" + confirmPrint +
               '}';
    }
}