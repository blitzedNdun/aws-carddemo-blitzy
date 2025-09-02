package com.carddemo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for detailed report generation requests.
 * 
 * This DTO contains comprehensive filtering criteria, date ranges, account selections, 
 * pagination settings, and control break configuration for multi-level report processing.
 * Specifically designed for CORPT01C-equivalent detailed transaction and account 
 * reporting functionality.
 * 
 * Supports various report types with flexible filtering options, pagination controls,
 * and configurable output formats. The control break fields enable hierarchical 
 * reporting with subtotals and grand totals matching COBOL control break logic.
 */
public class DetailedReportRequest {

    /**
     * Report category selection - specifies the type of detailed report to generate.
     * Examples: "TRANSACTION_DETAIL", "ACCOUNT_SUMMARY", "CUSTOMER_ACTIVITY"
     */
    private String reportType;

    /**
     * List of account IDs for filtering the report to specific accounts.
     * When empty or null, includes all accounts based on other filter criteria.
     */
    private List<Long> accountIds;

    /**
     * List of customer IDs for filtering the report to specific customers.
     * When empty or null, includes all customers based on other filter criteria.
     */
    private List<Long> customerIds;

    /**
     * Start date for the report date range filtering (inclusive).
     * Used to filter transactions and activities from this date forward.
     */
    private LocalDate startDate;

    /**
     * End date for the report date range filtering (inclusive).
     * Used to filter transactions and activities up to this date.
     */
    private LocalDate endDate;

    /**
     * List of transaction types for category filtering.
     * Examples: "PURCHASE", "PAYMENT", "CASH_ADVANCE", "FEE"
     * When empty or null, includes all transaction types.
     */
    private List<String> transactionTypes;

    /**
     * List of field names for multi-level control break configuration.
     * Defines the hierarchy for subtotal grouping in the report.
     * Examples: ["CUSTOMER_ID", "ACCOUNT_ID", "TRANSACTION_TYPE"]
     */
    private List<String> controlBreakFields;

    /**
     * Flag to include subtotals in the report output.
     * When true, generates subtotals at each control break level.
     */
    private boolean includeSubtotals;

    /**
     * Flag to include grand total in the report output.
     * When true, generates a grand total at the end of the report.
     */
    private boolean includeGrandTotal;

    /**
     * List of field names for report ordering/sorting.
     * Defines the sort order for the report output.
     * Examples: ["TRANSACTION_DATE", "AMOUNT", "CUSTOMER_NAME"]
     */
    private List<String> sortFields;

    /**
     * Page number for pagination (1-based).
     * Used for paginated report output to manage large result sets.
     */
    private int pageNumber;

    /**
     * Number of records per page for pagination.
     * Controls the size of each page in paginated report output.
     */
    private int pageSize;

    /**
     * Export format specification for the report output.
     * Supported formats: "CSV", "PDF", "EXCEL", "JSON"
     */
    private String exportFormat;

    /**
     * Custom title for the report header.
     * When provided, overrides the default report title.
     */
    private String reportTitle;

    /**
     * Flag to include statistical analysis in the report.
     * When true, adds summary statistics like averages, counts, and totals.
     */
    private boolean includeSummaryStats;

    /**
     * Default constructor.
     */
    public DetailedReportRequest() {
        // Initialize with default values
        this.pageNumber = 1;
        this.pageSize = 50;
        this.includeSubtotals = false;
        this.includeGrandTotal = true;
        this.includeSummaryStats = false;
        this.exportFormat = "JSON";
    }

    /**
     * Gets the report type.
     * 
     * @return the report category selection
     */
    public String getReportType() {
        return reportType;
    }

    /**
     * Sets the report type.
     * 
     * @param reportType the report category selection
     */
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    /**
     * Gets the list of account IDs for filtering.
     * 
     * @return the list of account IDs
     */
    public List<Long> getAccountIds() {
        return accountIds;
    }

    /**
     * Sets the list of account IDs for filtering.
     * 
     * @param accountIds the list of account IDs
     */
    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }

    /**
     * Gets the list of customer IDs for filtering.
     * 
     * @return the list of customer IDs
     */
    public List<Long> getCustomerIds() {
        return customerIds;
    }

    /**
     * Sets the list of customer IDs for filtering.
     * 
     * @param customerIds the list of customer IDs
     */
    public void setCustomerIds(List<Long> customerIds) {
        this.customerIds = customerIds;
    }

    /**
     * Gets the start date for date range filtering.
     * 
     * @return the start date (inclusive)
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date for date range filtering.
     * 
     * @param startDate the start date (inclusive)
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the end date for date range filtering.
     * 
     * @return the end date (inclusive)
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date for date range filtering.
     * 
     * @param endDate the end date (inclusive)
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the list of transaction types for filtering.
     * 
     * @return the list of transaction types
     */
    public List<String> getTransactionTypes() {
        return transactionTypes;
    }

    /**
     * Sets the list of transaction types for filtering.
     * 
     * @param transactionTypes the list of transaction types
     */
    public void setTransactionTypes(List<String> transactionTypes) {
        this.transactionTypes = transactionTypes;
    }

    /**
     * Gets the list of control break fields for multi-level subtotals.
     * 
     * @return the list of control break field names
     */
    public List<String> getControlBreakFields() {
        return controlBreakFields;
    }

    /**
     * Sets the list of control break fields for multi-level subtotals.
     * 
     * @param controlBreakFields the list of control break field names
     */
    public void setControlBreakFields(List<String> controlBreakFields) {
        this.controlBreakFields = controlBreakFields;
    }

    /**
     * Gets the flag for including subtotals.
     * 
     * @return true if subtotals should be included
     */
    public boolean getIncludeSubtotals() {
        return includeSubtotals;
    }

    /**
     * Sets the flag for including subtotals.
     * 
     * @param includeSubtotals true to include subtotals
     */
    public void setIncludeSubtotals(boolean includeSubtotals) {
        this.includeSubtotals = includeSubtotals;
    }

    /**
     * Gets the flag for including grand total.
     * 
     * @return true if grand total should be included
     */
    public boolean getIncludeGrandTotal() {
        return includeGrandTotal;
    }

    /**
     * Sets the flag for including grand total.
     * 
     * @param includeGrandTotal true to include grand total
     */
    public void setIncludeGrandTotal(boolean includeGrandTotal) {
        this.includeGrandTotal = includeGrandTotal;
    }

    /**
     * Gets the list of sort fields for report ordering.
     * 
     * @return the list of sort field names
     */
    public List<String> getSortFields() {
        return sortFields;
    }

    /**
     * Sets the list of sort fields for report ordering.
     * 
     * @param sortFields the list of sort field names
     */
    public void setSortFields(List<String> sortFields) {
        this.sortFields = sortFields;
    }

    /**
     * Gets the page number for pagination.
     * 
     * @return the page number (1-based)
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Sets the page number for pagination.
     * 
     * @param pageNumber the page number (1-based)
     */
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Gets the page size for pagination.
     * 
     * @return the number of records per page
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Sets the page size for pagination.
     * 
     * @param pageSize the number of records per page
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the export format for the report output.
     * 
     * @return the export format specification
     */
    public String getExportFormat() {
        return exportFormat;
    }

    /**
     * Sets the export format for the report output.
     * 
     * @param exportFormat the export format specification
     */
    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    /**
     * Gets the custom report title.
     * 
     * @return the custom report title
     */
    public String getReportTitle() {
        return reportTitle;
    }

    /**
     * Sets the custom report title.
     * 
     * @param reportTitle the custom report title
     */
    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    /**
     * Gets the flag for including summary statistics.
     * 
     * @return true if summary statistics should be included
     */
    public boolean getIncludeSummaryStats() {
        return includeSummaryStats;
    }

    /**
     * Sets the flag for including summary statistics.
     * 
     * @param includeSummaryStats true to include summary statistics
     */
    public void setIncludeSummaryStats(boolean includeSummaryStats) {
        this.includeSummaryStats = includeSummaryStats;
    }

    /**
     * Returns a string representation of the DetailedReportRequest.
     * 
     * @return string representation for debugging and logging
     */
    @Override
    public String toString() {
        return "DetailedReportRequest{" +
                "reportType='" + reportType + '\'' +
                ", accountIds=" + accountIds +
                ", customerIds=" + customerIds +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", transactionTypes=" + transactionTypes +
                ", controlBreakFields=" + controlBreakFields +
                ", includeSubtotals=" + includeSubtotals +
                ", includeGrandTotal=" + includeGrandTotal +
                ", sortFields=" + sortFields +
                ", pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", exportFormat='" + exportFormat + '\'' +
                ", reportTitle='" + reportTitle + '\'' +
                ", includeSummaryStats=" + includeSummaryStats +
                '}';
    }
}