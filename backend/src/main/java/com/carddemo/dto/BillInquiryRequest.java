package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * Request DTO for bill inquiry operations containing account ID and optional date range parameters 
 * for retrieving billing statements and payment history. Maps to COBIL01C input requirements with 
 * validation matching COBOL picture clauses.
 * 
 * This DTO supports:
 * - Account validation with 11-digit account ID constraint
 * - Specific statement lookup via statementDate
 * - Historical statement retrieval via date range (startDate/endDate)
 * - REST API serialization with proper JSON date formatting
 */
@Data
public class BillInquiryRequest {

    /**
     * Account ID for bill inquiry - must be exactly 11 digits to match COBOL PIC 9(11) constraint.
     * Maps to ACTIDINI field from COBIL00 BMS map structure.
     * This field is required for all bill inquiry operations.
     */
    @Size(min = 11, max = 11, message = "Account ID must be exactly 11 digits")
    private String accountId;

    /**
     * Optional statement date for retrieving a specific billing statement.
     * Used when looking up a particular statement period rather than a date range.
     * Formatted as ISO-8601 date (yyyy-MM-dd) for REST API consistency.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statementDate;

    /**
     * Optional start date for historical statement retrieval.
     * Used in conjunction with endDate to define a date range for bill inquiry.
     * Formatted as ISO-8601 date (yyyy-MM-dd) for REST API consistency.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * Optional end date for historical statement retrieval.
     * Used in conjunction with startDate to define a date range for bill inquiry.
     * Formatted as ISO-8601 date (yyyy-MM-dd) for REST API consistency.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Validates that if both startDate and endDate are provided, startDate is not after endDate.
     * This business rule ensures logical date range constraints for bill inquiry operations.
     * 
     * @return true if date range is valid or not fully specified, false otherwise
     */
    public boolean isValidDateRange() {
        if (startDate != null && endDate != null) {
            return !startDate.isAfter(endDate);
        }
        return true;
    }

    /**
     * Determines if this request is for a specific statement lookup (statementDate provided)
     * versus a date range query (startDate/endDate provided).
     * 
     * @return true if requesting a specific statement, false for date range or no date criteria
     */
    public boolean isSpecificStatementRequest() {
        return statementDate != null;
    }

    /**
     * Determines if this request is for a date range query.
     * 
     * @return true if both startDate and endDate are provided, false otherwise
     */
    public boolean isDateRangeRequest() {
        return startDate != null && endDate != null;
    }

    /**
     * Gets the effective start date for the query, which is either the statementDate
     * (for specific statement requests) or the startDate (for range requests).
     * 
     * @return the effective start date for the query, or null if no date criteria specified
     */
    public LocalDate getEffectiveStartDate() {
        if (statementDate != null) {
            return statementDate;
        }
        return startDate;
    }

    /**
     * Gets the effective end date for the query, which is either the statementDate
     * (for specific statement requests) or the endDate (for range requests).
     * 
     * @return the effective end date for the query, or null if no date criteria specified
     */
    public LocalDate getEffectiveEndDate() {
        if (statementDate != null) {
            return statementDate;
        }
        return endDate;
    }
}