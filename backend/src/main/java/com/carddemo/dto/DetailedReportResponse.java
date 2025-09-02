/*
 * DetailedReportResponse.java
 * 
 * Data Transfer Object for detailed report generation responses, containing 
 * formatted report data with control break processing, multi-level subtotals, 
 * grand totals, statistical analysis, and export capabilities. 
 * 
 * Mirrors COBOL report structure from CORPT00C.cbl while providing flexible 
 * formatting and pagination support for modern display and export requirements.
 * 
 * This class maintains exact columnar alignment and numeric formatting 
 * matching COBOL display patterns for seamless migration from mainframe 
 * report processing.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */
package com.carddemo.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DetailedReportResponse DTO for complex report generation responses.
 * 
 * This class encapsulates the output of detailed report services, providing
 * structured data for display, export, and further processing. It maintains
 * compatibility with COBOL report formatting while enabling modern pagination
 * and analysis capabilities.
 * 
 * Key Features:
 * - Control break processing with multi-level subtotals
 * - COBOL-style fixed-width formatted lines for display compatibility
 * - Statistical analysis including counts, averages, min/max calculations
 * - Pagination metadata for large dataset handling
 * - Export URL generation for download capabilities
 * - Comprehensive error tracking and validation messages
 * 
 * Derived from CORPT00C.cbl report generation logic, this DTO preserves
 * the exact data organization and formatting requirements of the original
 * mainframe implementation.
 */
public class DetailedReportResponse {

    /**
     * Unique identifier for the generated report.
     * Used for tracking, caching, and audit trail purposes.
     * Maps to WS-REPORT-NAME and job tracking from CORPT00C.cbl.
     */
    private String reportId;

    /**
     * Human-readable title for the report display header.
     * Corresponds to report type selection (Monthly, Yearly, Custom)
     * from CORPT00C.cbl processing logic.
     */
    private String reportTitle;

    /**
     * Timestamp when the report was generated.
     * Provides audit trail and freshness indicator for report data.
     * Replaces COBOL CURRENT-DATE processing.
     */
    private LocalDateTime reportDate;

    /**
     * Structured data rows for the report content.
     * Each Map represents a single data row with column name/value pairs.
     * Enables flexible processing while maintaining data integrity.
     * Replaces COBOL file record processing and data accumulation.
     */
    private List<Map<String, Object>> reportData;

    /**
     * COBOL-style fixed-width formatted lines for display.
     * Maintains exact columnar alignment and spacing as per original
     * mainframe report layouts. Each string represents one print line
     * with proper padding and formatting preserved.
     */
    private List<String> formattedLines;

    /**
     * Control break summary data organized by break levels.
     * Contains subtotal information for hierarchical reporting.
     * Key structure: "level_column" -> Map of aggregated values.
     * Implements COBOL control break processing patterns.
     */
    private Map<String, Map<String, Object>> controlBreakSummary;

    /**
     * Grand total values for all numeric columns in the report.
     * Provides overall aggregation across all data rows.
     * Maintains precision equivalent to COBOL COMP-3 calculations.
     */
    private Map<String, Object> grandTotal;

    /**
     * Statistical analysis data including counts, averages, min/max values.
     * Provides enhanced analytical capabilities beyond basic totaling.
     * Structure: "statistic_type" -> "column_name" -> calculated value.
     */
    private Map<String, Map<String, Object>> statisticalSummary;

    /**
     * Total number of records in the complete dataset.
     * Used for pagination metadata and progress indicators.
     * Corresponds to WS-REC-COUNT processing in COBOL.
     */
    private long totalRecords;

    /**
     * Current page number for pagination control.
     * Zero-based indexing for consistency with Spring Data pagination.
     */
    private int pageNumber;

    /**
     * Number of records per page for pagination control.
     * Configurable page size for performance optimization.
     */
    private int pageSize;

    /**
     * Total number of pages available for the complete dataset.
     * Calculated based on totalRecords and pageSize.
     */
    private int totalPages;

    /**
     * Column headers for display formatting and table generation.
     * Maintains column order and formatting specifications.
     * Enables dynamic table rendering in frontend components.
     */
    private List<String> columnHeaders;

    /**
     * URL for downloading the complete report in export format.
     * Supports various formats (CSV, PDF, Excel) based on requirements.
     * Enables asynchronous download processing for large reports.
     */
    private String exportUrl;

    /**
     * List of validation errors and processing messages.
     * Provides detailed error information for troubleshooting.
     * Corresponds to WS-MESSAGE and error handling in CORPT00C.cbl.
     */
    private List<String> errors;

    /**
     * Default constructor.
     * Initializes all collection fields to prevent null pointer exceptions.
     */
    public DetailedReportResponse() {
        this.reportData = new ArrayList<>();
        this.formattedLines = new ArrayList<>();
        this.controlBreakSummary = new HashMap<>();
        this.grandTotal = new HashMap<>();
        this.statisticalSummary = new HashMap<>();
        this.columnHeaders = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    /**
     * Gets the unique report identifier.
     * 
     * @return String containing the unique report ID
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * Sets the unique report identifier.
     * 
     * @param reportId The unique identifier for this report
     */
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    /**
     * Gets the report title for display purposes.
     * 
     * @return String containing the human-readable report title
     */
    public String getReportTitle() {
        return reportTitle;
    }

    /**
     * Sets the report title for display purposes.
     * 
     * @param reportTitle The human-readable title for this report
     */
    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    /**
     * Gets the report generation timestamp.
     * 
     * @return LocalDateTime when the report was generated
     */
    public LocalDateTime getReportDate() {
        return reportDate;
    }

    /**
     * Sets the report generation timestamp.
     * 
     * @param reportDate The timestamp when this report was generated
     */
    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    /**
     * Gets the structured report data rows.
     * 
     * @return List of Maps containing the report data rows
     */
    public List<Map<String, Object>> getReportData() {
        return reportData;
    }

    /**
     * Sets the structured report data rows.
     * 
     * @param reportData List of Maps containing the report data
     */
    public void setReportData(List<Map<String, Object>> reportData) {
        this.reportData = reportData != null ? reportData : new ArrayList<>();
    }

    /**
     * Gets the COBOL-style formatted lines for display.
     * 
     * @return List of formatted strings for display output
     */
    public List<String> getFormattedLines() {
        return formattedLines;
    }

    /**
     * Sets the COBOL-style formatted lines for display.
     * 
     * @param formattedLines List of formatted strings maintaining COBOL layout
     */
    public void setFormattedLines(List<String> formattedLines) {
        this.formattedLines = formattedLines != null ? formattedLines : new ArrayList<>();
    }

    /**
     * Gets the control break summary data.
     * 
     * @return Map containing hierarchical subtotal information
     */
    public Map<String, Map<String, Object>> getControlBreakSummary() {
        return controlBreakSummary;
    }

    /**
     * Sets the control break summary data.
     * 
     * @param controlBreakSummary Map containing hierarchical subtotal data
     */
    public void setControlBreakSummary(Map<String, Map<String, Object>> controlBreakSummary) {
        this.controlBreakSummary = controlBreakSummary != null ? controlBreakSummary : new HashMap<>();
    }

    /**
     * Gets the grand total values for all numeric columns.
     * 
     * @return Map containing overall totals for the report
     */
    public Map<String, Object> getGrandTotal() {
        return grandTotal;
    }

    /**
     * Sets the grand total values for all numeric columns.
     * 
     * @param grandTotal Map containing overall totals with COBOL precision
     */
    public void setGrandTotal(Map<String, Object> grandTotal) {
        this.grandTotal = grandTotal != null ? grandTotal : new HashMap<>();
    }

    /**
     * Gets the statistical analysis summary.
     * 
     * @return Map containing statistical calculations (avg, min, max, count)
     */
    public Map<String, Map<String, Object>> getStatisticalSummary() {
        return statisticalSummary;
    }

    /**
     * Sets the statistical analysis summary.
     * 
     * @param statisticalSummary Map containing statistical calculations
     */
    public void setStatisticalSummary(Map<String, Map<String, Object>> statisticalSummary) {
        this.statisticalSummary = statisticalSummary != null ? statisticalSummary : new HashMap<>();
    }

    /**
     * Gets the total number of records in the complete dataset.
     * 
     * @return Long value representing total record count
     */
    public long getTotalRecords() {
        return totalRecords;
    }

    /**
     * Sets the total number of records in the complete dataset.
     * 
     * @param totalRecords The total number of records available
     */
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    /**
     * Gets the current page number for pagination.
     * 
     * @return Integer representing the current page (zero-based)
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the current page number for pagination.
     * 
     * @param pageNumber The current page number (zero-based)
     */
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the page size for pagination control.
     * 
     * @return Integer representing records per page
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination control.
     * 
     * @param pageSize The number of records per page
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the total number of pages for the complete dataset.
     * 
     * @return Integer representing total pages available
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Sets the total number of pages for the complete dataset.
     * 
     * @param totalPages The total number of pages available
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * Gets the column headers for display formatting.
     * 
     * @return List of column header strings for table display
     */
    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    /**
     * Sets the column headers for display formatting.
     * 
     * @param columnHeaders List of column headers maintaining display order
     */
    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders != null ? columnHeaders : new ArrayList<>();
    }

    /**
     * Gets the export URL for report download.
     * 
     * @return String containing the URL for downloading the complete report
     */
    public String getExportUrl() {
        return exportUrl;
    }

    /**
     * Sets the export URL for report download.
     * 
     * @param exportUrl The URL for downloading the complete report
     */
    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }

    /**
     * Gets the list of validation errors and processing messages.
     * 
     * @return List of error messages and validation failures
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Sets the list of validation errors and processing messages.
     * 
     * @param errors List of error messages for troubleshooting
     */
    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    /**
     * Utility method to add a single error message to the errors list.
     * 
     * @param errorMessage The error message to add
     */
    public void addError(String errorMessage) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(errorMessage);
    }

    /**
     * Utility method to check if the response contains any errors.
     * 
     * @return boolean indicating whether errors are present
     */
    public boolean hasErrors() {
        return this.errors != null && !this.errors.isEmpty();
    }

    /**
     * Utility method to add a formatted line to the display output.
     * 
     * @param formattedLine The formatted line maintaining COBOL spacing
     */
    public void addFormattedLine(String formattedLine) {
        if (this.formattedLines == null) {
            this.formattedLines = new ArrayList<>();
        }
        this.formattedLines.add(formattedLine);
    }

    /**
     * Utility method to add a data row to the report.
     * 
     * @param dataRow Map containing column name/value pairs for the row
     */
    public void addDataRow(Map<String, Object> dataRow) {
        if (this.reportData == null) {
            this.reportData = new ArrayList<>();
        }
        this.reportData.add(dataRow);
    }

    /**
     * Utility method to add a column header.
     * 
     * @param columnHeader The column header string
     */
    public void addColumnHeader(String columnHeader) {
        if (this.columnHeaders == null) {
            this.columnHeaders = new ArrayList<>();
        }
        this.columnHeaders.add(columnHeader);
    }

    /**
     * Calculates total pages based on totalRecords and pageSize.
     * This method should be called after setting totalRecords and pageSize.
     */
    public void calculateTotalPages() {
        if (this.pageSize > 0) {
            this.totalPages = (int) Math.ceil((double) this.totalRecords / this.pageSize);
        } else {
            this.totalPages = 0;
        }
    }

    /**
     * Validates that all required fields are present and properly formatted.
     * Implements business rule validation equivalent to CORPT00C.cbl error checking.
     * 
     * @return boolean indicating whether the response is valid
     */
    public boolean isValid() {
        // Clear any existing validation errors
        this.errors.clear();

        // Validate required fields
        if (this.reportId == null || this.reportId.trim().isEmpty()) {
            this.addError("Report ID is required and cannot be empty");
        }

        if (this.reportTitle == null || this.reportTitle.trim().isEmpty()) {
            this.addError("Report title is required and cannot be empty");
        }

        if (this.reportDate == null) {
            this.addError("Report generation date is required");
        }

        // Validate pagination consistency
        if (this.pageSize > 0 && this.totalRecords >= 0) {
            int expectedTotalPages = (int) Math.ceil((double) this.totalRecords / this.pageSize);
            if (this.totalPages != expectedTotalPages) {
                this.addError("Total pages calculation is inconsistent with record count and page size");
            }
        }

        if (this.pageNumber < 0) {
            this.addError("Page number cannot be negative");
        }

        if (this.pageSize < 0) {
            this.addError("Page size cannot be negative");
        }

        if (this.totalRecords < 0) {
            this.addError("Total records cannot be negative");
        }

        // Data consistency validation
        if (this.reportData != null && this.columnHeaders != null) {
            for (Map<String, Object> row : this.reportData) {
                if (row != null && !row.isEmpty()) {
                    // Validate that data rows contain expected columns
                    boolean hasValidColumns = row.keySet().stream()
                        .anyMatch(key -> this.columnHeaders.contains(key));
                    if (!hasValidColumns) {
                        this.addError("Data row contains columns not present in column headers");
                        break;
                    }
                }
            }
        }

        return !this.hasErrors();
    }

    @Override
    public String toString() {
        return String.format(
            "DetailedReportResponse{reportId='%s', reportTitle='%s', reportDate=%s, " +
            "totalRecords=%d, pageNumber=%d, pageSize=%d, totalPages=%d, " +
            "dataRows=%d, formattedLines=%d, controlBreaks=%d, errors=%d}",
            reportId, reportTitle, reportDate, totalRecords, pageNumber, pageSize, 
            totalPages, 
            (reportData != null ? reportData.size() : 0),
            (formattedLines != null ? formattedLines.size() : 0),
            (controlBreakSummary != null ? controlBreakSummary.size() : 0),
            (errors != null ? errors.size() : 0)
        );
    }
}