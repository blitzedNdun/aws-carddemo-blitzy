/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.dto;

import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * Report request DTO for CORPT00 screen functionality.
 * Contains report type selection (monthly, yearly, custom), date range parameters, 
 * and output format options. Supports various report generation requests with date 
 * validation and period selection logic.
 * 
 * This DTO translates the BMS screen fields from CORPT00.bms and CORPT00.CPY:
 * - MONTHLY/YEARLY/CUSTOM radio button selections -> ReportType enum
 * - SDTMM/SDTDD/SDTYYYY date fields -> startDate LocalDate
 * - EDTMM/EDTDD/EDTYYYY date fields -> endDate LocalDate  
 * - CONFIRM Y/N field -> confirmationFlag boolean
 * 
 * Maintains 100% functional parity with COBOL screen validation logic while
 * providing modern Java type safety and JSON serialization support.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Data
public class ReportRequestDto {

    /**
     * Report type enumeration matching COBOL screen options from CORPT00.bms.
     * Corresponds to the three radio button options on the report selection screen:
     * - MONTHLY: Generate report for current month
     * - YEARLY: Generate report for current year  
     * - CUSTOM: Generate report for user-specified date range
     */
    public enum ReportType {
        /**
         * Monthly report for the current month.
         * When selected, uses current date to determine month boundaries.
         */
        MONTHLY,
        
        /**
         * Yearly report for the current year.
         * When selected, uses current date to determine year boundaries.
         */
        YEARLY,
        
        /**
         * Custom date range report.
         * When selected, requires both startDate and endDate to be specified.
         */
        CUSTOM
    }

    /**
     * Selected report type from the BMS screen MONTHLY/YEARLY/CUSTOM fields.
     * Maps to the radio button selection where user chooses report period type.
     * 
     * Required field that determines whether date range fields are needed.
     */
    @NotNull(message = "Report type must be selected")
    private ReportType reportType;

    /**
     * Start date for custom date range reports.
     * Corresponds to SDTMM/SDTDD/SDTYYYY fields from BMS screen.
     * 
     * Only required when reportType is CUSTOM. For MONTHLY and YEARLY reports,
     * the start date is calculated automatically based on current date.
     * 
     * Stored in ISO date format for JSON serialization compatibility.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * End date for custom date range reports.
     * Corresponds to EDTMM/EDTDD/EDTYYYY fields from BMS screen.
     * 
     * Only required when reportType is CUSTOM. For MONTHLY and YEARLY reports,
     * the end date is calculated automatically based on current date.
     * 
     * Stored in ISO date format for JSON serialization compatibility.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * User confirmation flag from BMS CONFIRM field.
     * Corresponds to the Y/N prompt asking user to confirm report submission.
     * 
     * True indicates user confirmed report generation (Y response).
     * False indicates user did not confirm (N response or blank).
     */
    private boolean confirmationFlag;

    /**
     * Determines if custom date range is required based on selected report type.
     * Replicates COBOL logic where CUSTOM report type enables date fields,
     * while MONTHLY and YEARLY use predefined periods.
     * 
     * @return true if startDate and endDate are required (CUSTOM type), 
     *         false for MONTHLY and YEARLY types
     */
    public boolean isDateRangeRequired() {
        return reportType == ReportType.CUSTOM;
    }

    /**
     * Validates the date range for custom reports using COBOL validation logic.
     * Performs comprehensive validation including:
     * - Required field validation for custom report types
     * - Date format validation using DateConversionUtil
     * - Date range validation ensuring start date <= end date
     * - Future date validation matching COBOL business rules
     * 
     * This method replicates the date range validation from the original COBOL
     * program while using modern Java validation techniques.
     * 
     * @throws IllegalArgumentException if date range validation fails
     */
    public void validateDateRange() {
        // For MONTHLY and YEARLY reports, no custom date validation needed
        if (reportType == ReportType.MONTHLY || reportType == ReportType.YEARLY) {
            return;
        }
        
        // For CUSTOM reports, validate date range requirements
        if (reportType == ReportType.CUSTOM) {
            // Validate required fields using ValidationUtil
            ValidationUtil.validateRequiredField("startDate", 
                startDate != null ? DateConversionUtil.formatToCobol(startDate) : null);
            ValidationUtil.validateRequiredField("endDate", 
                endDate != null ? DateConversionUtil.formatToCobol(endDate) : null);
            
            // Validate individual date formats using DateConversionUtil
            if (startDate != null) {
                String startDateStr = DateConversionUtil.formatToCobol(startDate);
                if (!DateConversionUtil.validateDate(startDateStr)) {
                    throw new IllegalArgumentException("Start date format is invalid. Use YYYY-MM-DD format.");
                }
                
                // Validate start date using COBOL date of birth validation logic
                ValidationUtil.validateDateOfBirth("startDate", startDateStr);
            }
            
            if (endDate != null) {
                String endDateStr = DateConversionUtil.formatToCobol(endDate);
                if (!DateConversionUtil.validateDate(endDateStr)) {
                    throw new IllegalArgumentException("End date format is invalid. Use YYYY-MM-DD format.");
                }
                
                // Validate end date using COBOL date of birth validation logic  
                ValidationUtil.validateDateOfBirth("endDate", endDateStr);
            }
            
            // Validate date range relationship
            if (startDate != null && endDate != null) {
                if (startDate.isAfter(endDate)) {
                    throw new IllegalArgumentException("Start date must be before or equal to end date.");
                }
                
                // Additional business rule: date range cannot exceed 1 year
                if (startDate.plusYears(1).isBefore(endDate)) {
                    throw new IllegalArgumentException("Date range cannot exceed one year.");
                }
            }
        }
    }

    /**
     * Gets the calculated start date for the report based on report type.
     * For MONTHLY reports, returns the first day of current month.
     * For YEARLY reports, returns the first day of current year.
     * For CUSTOM reports, returns the user-specified startDate.
     * 
     * @return calculated start date for the report period
     */
    public LocalDate getCalculatedStartDate() {
        switch (reportType) {
            case MONTHLY:
                return LocalDate.now().withDayOfMonth(1);
            case YEARLY:
                return LocalDate.now().withDayOfYear(1);
            case CUSTOM:
                return startDate;
            default:
                throw new IllegalStateException("Invalid report type: " + reportType);
        }
    }

    /**
     * Gets the calculated end date for the report based on report type.
     * For MONTHLY reports, returns the last day of current month.
     * For YEARLY reports, returns the last day of current year.
     * For CUSTOM reports, returns the user-specified endDate.
     * 
     * @return calculated end date for the report period
     */
    public LocalDate getCalculatedEndDate() {
        switch (reportType) {
            case MONTHLY:
                return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            case YEARLY:
                return LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
            case CUSTOM:
                return endDate;
            default:
                throw new IllegalStateException("Invalid report type: " + reportType);
        }
    }

    /**
     * Gets a human-readable description of the report period.
     * Used for confirmation messages and report headers.
     * 
     * @return descriptive string of the report period
     */
    public String getReportPeriodDescription() {
        switch (reportType) {
            case MONTHLY:
                return "Monthly (Current Month)";
            case YEARLY:
                return "Yearly (Current Year)";
            case CUSTOM:
                if (startDate != null && endDate != null) {
                    return "Custom (" + startDate.toString() + " to " + endDate.toString() + ")";
                } else {
                    return "Custom (Date Range)";
                }
            default:
                return "Unknown Report Type";
        }
    }
}