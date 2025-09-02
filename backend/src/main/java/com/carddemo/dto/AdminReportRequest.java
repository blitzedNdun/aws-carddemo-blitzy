package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data Transfer Object for administrative report generation requests.
 * Contains parameters for report type selection, date range filtering, 
 * pagination settings, and output format specification.
 * 
 * Supports validation constraints and serialization for REST API communication.
 * Implements builder pattern for flexible object construction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = AdminReportRequest.Builder.class)
public class AdminReportRequest {
    
    /**
     * Enumeration of available report types for administrative reporting.
     */
    public enum ReportType {
        USER_ACTIVITY("User Activity Report"),
        AUDIT_LOG("Audit Log Report"), 
        TRANSACTION_VOLUME("Transaction Volume Report"),
        SYSTEM_USAGE("System Usage Report");
        
        private final String displayName;
        
        ReportType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Enumeration of supported output formats for report generation.
     */
    public enum OutputFormat {
        JSON("application/json"),
        CSV("text/csv"),
        PDF("application/pdf");
        
        private final String mimeType;
        
        OutputFormat(String mimeType) {
            this.mimeType = mimeType;
        }
        
        public String getMimeType() {
            return mimeType;
        }
    }
    
    /**
     * Type of report to generate.
     */
    @NotNull(message = "Report type is required")
    private final ReportType reportType;
    
    /**
     * Start date for report data range.
     */
    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime startDate;
    
    /**
     * End date for report data range.
     */
    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime endDate;
    
    /**
     * Page number for pagination (0-based).
     */
    @Min(value = 0, message = "Page number must be 0 or greater")
    private final int pageNumber;
    
    /**
     * Number of records per page.
     */
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 1000, message = "Page size cannot exceed 1000")
    private final int pageSize;
    
    /**
     * Output format for the generated report.
     */
    @NotNull(message = "Output format is required")
    private final OutputFormat outputFormat;
    
    /**
     * Optional user ID filter for user-specific reports.
     */
    private final String userId;
    
    /**
     * Optional transaction type filter for transaction reports.
     */
    private final String transactionType;
    
    /**
     * Optional severity level filter for audit log reports.
     */
    private final String severityLevel;
    
    /**
     * Private constructor for builder pattern.
     */
    private AdminReportRequest(Builder builder) {
        this.reportType = builder.reportType;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.pageNumber = builder.pageNumber;
        this.pageSize = builder.pageSize;
        this.outputFormat = builder.outputFormat;
        this.userId = builder.userId;
        this.transactionType = builder.transactionType;
        this.severityLevel = builder.severityLevel;
    }
    
    /**
     * Gets the report type.
     * 
     * @return the report type
     */
    public ReportType getReportType() {
        return reportType;
    }
    
    /**
     * Gets the start date for the report data range.
     * 
     * @return the start date
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    /**
     * Gets the end date for the report data range.
     * 
     * @return the end date
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    /**
     * Gets the page number for pagination.
     * 
     * @return the page number (0-based)
     */
    public int getPageNumber() {
        return pageNumber;
    }
    
    /**
     * Gets the page size for pagination.
     * 
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }
    
    /**
     * Gets the output format for the report.
     * 
     * @return the output format
     */
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }
    
    /**
     * Gets the optional user ID filter.
     * 
     * @return the user ID filter or null if not specified
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the optional transaction type filter.
     * 
     * @return the transaction type filter or null if not specified
     */
    public String getTransactionType() {
        return transactionType;
    }
    
    /**
     * Gets the optional severity level filter.
     * 
     * @return the severity level filter or null if not specified
     */
    public String getSeverityLevel() {
        return severityLevel;
    }
    
    /**
     * Creates a new builder instance for constructing AdminReportRequest objects.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminReportRequest that = (AdminReportRequest) o;
        return pageNumber == that.pageNumber &&
               pageSize == that.pageSize &&
               reportType == that.reportType &&
               Objects.equals(startDate, that.startDate) &&
               Objects.equals(endDate, that.endDate) &&
               outputFormat == that.outputFormat &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(transactionType, that.transactionType) &&
               Objects.equals(severityLevel, that.severityLevel);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(reportType, startDate, endDate, pageNumber, pageSize, 
                           outputFormat, userId, transactionType, severityLevel);
    }
    
    @Override
    public String toString() {
        return "AdminReportRequest{" +
               "reportType=" + reportType +
               ", startDate=" + startDate +
               ", endDate=" + endDate +
               ", pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               ", outputFormat=" + outputFormat +
               ", userId='" + userId + '\'' +
               ", transactionType='" + transactionType + '\'' +
               ", severityLevel='" + severityLevel + '\'' +
               '}';
    }
    
    /**
     * Builder class for constructing AdminReportRequest instances.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ReportType reportType;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int pageNumber = 0;
        private int pageSize = 50;
        private OutputFormat outputFormat = OutputFormat.JSON;
        private String userId;
        private String transactionType;
        private String severityLevel;
        
        /**
         * Sets the report type.
         * 
         * @param reportType the report type
         * @return this builder instance
         */
        public Builder reportType(ReportType reportType) {
            this.reportType = reportType;
            return this;
        }
        
        /**
         * Sets the start date for the report data range.
         * 
         * @param startDate the start date
         * @return this builder instance
         */
        public Builder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }
        
        /**
         * Sets the end date for the report data range.
         * 
         * @param endDate the end date
         * @return this builder instance
         */
        public Builder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }
        
        /**
         * Sets the page number for pagination.
         * 
         * @param pageNumber the page number (0-based)
         * @return this builder instance
         */
        public Builder pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }
        
        /**
         * Sets the page size for pagination.
         * 
         * @param pageSize the page size
         * @return this builder instance
         */
        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }
        
        /**
         * Sets the output format for the report.
         * 
         * @param outputFormat the output format
         * @return this builder instance
         */
        public Builder outputFormat(OutputFormat outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }
        
        /**
         * Sets the optional user ID filter.
         * 
         * @param userId the user ID filter
         * @return this builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        /**
         * Sets the optional transaction type filter.
         * 
         * @param transactionType the transaction type filter
         * @return this builder instance
         */
        public Builder transactionType(String transactionType) {
            this.transactionType = transactionType;
            return this;
        }
        
        /**
         * Sets the optional severity level filter.
         * 
         * @param severityLevel the severity level filter
         * @return this builder instance
         */
        public Builder severityLevel(String severityLevel) {
            this.severityLevel = severityLevel;
            return this;
        }
        
        /**
         * Builds the AdminReportRequest instance.
         * 
         * @return a new AdminReportRequest instance
         */
        public AdminReportRequest build() {
            return new AdminReportRequest(this);
        }
    }
}