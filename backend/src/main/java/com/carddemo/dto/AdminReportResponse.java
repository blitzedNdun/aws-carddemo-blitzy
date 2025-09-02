package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object for administrative report generation responses.
 * Provides comprehensive reporting structure for admin dashboard integration
 * with support for various report formats, pagination, and error handling.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminReportResponse {

    @JsonProperty("reportData")
    private List<Object> reportData;

    @JsonProperty("metadata")
    private ReportMetadata metadata;

    @JsonProperty("paginationInfo")
    private PaginationInfo paginationInfo;

    @JsonProperty("summaryStatistics")
    private SummaryStatistics summaryStatistics;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("errorMessages")
    private List<String> errorMessages;

    @JsonProperty("executionDuration")
    private Long executionDuration;

    /**
     * Default constructor for AdminReportResponse
     */
    public AdminReportResponse() {
        this.reportData = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
        this.status = Status.SUCCESS;
    }

    /**
     * Constructor with builder
     */
    private AdminReportResponse(Builder builder) {
        this.reportData = builder.reportData != null ? builder.reportData : new ArrayList<>();
        this.metadata = builder.metadata;
        this.paginationInfo = builder.paginationInfo;
        this.summaryStatistics = builder.summaryStatistics;
        this.status = builder.status != null ? builder.status : Status.SUCCESS;
        this.errorMessages = builder.errorMessages != null ? builder.errorMessages : new ArrayList<>();
        this.executionDuration = builder.executionDuration;
    }

    /**
     * Gets the report data as a generic list supporting flexible content types
     *
     * @return List of report data objects
     */
    public List<Object> getReportData() {
        return reportData;
    }

    /**
     * Sets the report data
     *
     * @param reportData List of report data objects
     */
    public void setReportData(List<Object> reportData) {
        this.reportData = reportData;
    }

    /**
     * Gets the report metadata including type, generation timestamp, and record count
     *
     * @return ReportMetadata object
     */
    public ReportMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the report metadata
     *
     * @param metadata ReportMetadata object
     */
    public void setMetadata(ReportMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets pagination information including current page and navigation indicators
     *
     * @return PaginationInfo object
     */
    public PaginationInfo getPaginationInfo() {
        return paginationInfo;
    }

    /**
     * Sets pagination information
     *
     * @param paginationInfo PaginationInfo object
     */
    public void setPaginationInfo(PaginationInfo paginationInfo) {
        this.paginationInfo = paginationInfo;
    }

    /**
     * Gets summary statistics including aggregated totals and KPIs
     *
     * @return SummaryStatistics object
     */
    public SummaryStatistics getSummaryStatistics() {
        return summaryStatistics;
    }

    /**
     * Sets summary statistics
     *
     * @param summaryStatistics SummaryStatistics object
     */
    public void setSummaryStatistics(SummaryStatistics summaryStatistics) {
        this.summaryStatistics = summaryStatistics;
    }

    /**
     * Gets the execution status of the report generation
     *
     * @return Status enumeration value
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the execution status
     *
     * @param status Status enumeration value
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets list of error messages encountered during report generation
     *
     * @return List of error messages
     */
    public List<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * Sets error messages
     *
     * @param errorMessages List of error messages
     */
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    /**
     * Gets the execution duration in milliseconds
     *
     * @return Execution duration in milliseconds
     */
    public Long getExecutionDuration() {
        return executionDuration;
    }

    /**
     * Sets the execution duration
     *
     * @param executionDuration Duration in milliseconds
     */
    public void setExecutionDuration(Long executionDuration) {
        this.executionDuration = executionDuration;
    }

    /**
     * Creates a new builder instance for AdminReportResponse
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Status enumeration for report execution results
     */
    public enum Status {
        @JsonProperty("SUCCESS")
        SUCCESS("SUCCESS", "Report generated successfully"),

        @JsonProperty("PARTIAL")
        PARTIAL("PARTIAL", "Report generated with some warnings"),

        @JsonProperty("ERROR")
        ERROR("ERROR", "Report generation failed");

        private final String code;
        private final String description;

        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Report metadata containing type, generation details, and record counts
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReportMetadata {
        @JsonProperty("reportType")
        private String reportType;

        @JsonProperty("generationTimestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime generationTimestamp;

        @JsonProperty("totalRecordCount")
        private Integer totalRecordCount;

        @JsonProperty("reportParameters")
        private Map<String, Object> reportParameters;

        @JsonProperty("generatedBy")
        private String generatedBy;

        @JsonProperty("reportTitle")
        private String reportTitle;

        @JsonProperty("reportDescription")
        private String reportDescription;

        public ReportMetadata() {
            this.reportParameters = new HashMap<>();
            this.generationTimestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getReportType() {
            return reportType;
        }

        public void setReportType(String reportType) {
            this.reportType = reportType;
        }

        public LocalDateTime getGenerationTimestamp() {
            return generationTimestamp;
        }

        public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
            this.generationTimestamp = generationTimestamp;
        }

        public Integer getTotalRecordCount() {
            return totalRecordCount;
        }

        public void setTotalRecordCount(Integer totalRecordCount) {
            this.totalRecordCount = totalRecordCount;
        }

        public Map<String, Object> getReportParameters() {
            return reportParameters;
        }

        public void setReportParameters(Map<String, Object> reportParameters) {
            this.reportParameters = reportParameters;
        }

        public String getGeneratedBy() {
            return generatedBy;
        }

        public void setGeneratedBy(String generatedBy) {
            this.generatedBy = generatedBy;
        }

        public String getReportTitle() {
            return reportTitle;
        }

        public void setReportTitle(String reportTitle) {
            this.reportTitle = reportTitle;
        }

        public String getReportDescription() {
            return reportDescription;
        }

        public void setReportDescription(String reportDescription) {
            this.reportDescription = reportDescription;
        }
    }

    /**
     * Pagination information with current page and navigation indicators
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        @JsonProperty("currentPage")
        private Integer currentPage;

        @JsonProperty("totalPages")
        private Integer totalPages;

        @JsonProperty("pageSize")
        private Integer pageSize;

        @JsonProperty("hasNextPage")
        private Boolean hasNextPage;

        @JsonProperty("hasPreviousPage")
        private Boolean hasPreviousPage;

        @JsonProperty("totalElements")
        private Long totalElements;

        @JsonProperty("numberOfElements")
        private Integer numberOfElements;

        @JsonProperty("first")
        private Boolean first;

        @JsonProperty("last")
        private Boolean last;

        public PaginationInfo() {
            this.currentPage = 0;
            this.totalPages = 0;
            this.pageSize = 50;
            this.hasNextPage = false;
            this.hasPreviousPage = false;
            this.totalElements = 0L;
            this.numberOfElements = 0;
            this.first = true;
            this.last = true;
        }

        // Getters and setters
        public Integer getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(Integer currentPage) {
            this.currentPage = currentPage;
        }

        public Integer getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public Boolean getHasNextPage() {
            return hasNextPage;
        }

        public void setHasNextPage(Boolean hasNextPage) {
            this.hasNextPage = hasNextPage;
        }

        public Boolean getHasPreviousPage() {
            return hasPreviousPage;
        }

        public void setHasPreviousPage(Boolean hasPreviousPage) {
            this.hasPreviousPage = hasPreviousPage;
        }

        public Long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(Long totalElements) {
            this.totalElements = totalElements;
        }

        public Integer getNumberOfElements() {
            return numberOfElements;
        }

        public void setNumberOfElements(Integer numberOfElements) {
            this.numberOfElements = numberOfElements;
        }

        public Boolean getFirst() {
            return first;
        }

        public void setFirst(Boolean first) {
            this.first = first;
        }

        public Boolean getLast() {
            return last;
        }

        public void setLast(Boolean last) {
            this.last = last;
        }
    }

    /**
     * Summary statistics with aggregated totals, averages, and KPIs
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SummaryStatistics {
        @JsonProperty("aggregatedTotals")
        private Map<String, BigDecimal> aggregatedTotals;

        @JsonProperty("averages")
        private Map<String, BigDecimal> averages;

        @JsonProperty("keyPerformanceIndicators")
        private Map<String, Object> keyPerformanceIndicators;

        @JsonProperty("counts")
        private Map<String, Long> counts;

        @JsonProperty("percentages")
        private Map<String, BigDecimal> percentages;

        @JsonProperty("trends")
        private Map<String, Object> trends;

        public SummaryStatistics() {
            this.aggregatedTotals = new HashMap<>();
            this.averages = new HashMap<>();
            this.keyPerformanceIndicators = new HashMap<>();
            this.counts = new HashMap<>();
            this.percentages = new HashMap<>();
            this.trends = new HashMap<>();
        }

        // Getters and setters
        public Map<String, BigDecimal> getAggregatedTotals() {
            return aggregatedTotals;
        }

        public void setAggregatedTotals(Map<String, BigDecimal> aggregatedTotals) {
            this.aggregatedTotals = aggregatedTotals;
        }

        public Map<String, BigDecimal> getAverages() {
            return averages;
        }

        public void setAverages(Map<String, BigDecimal> averages) {
            this.averages = averages;
        }

        public Map<String, Object> getKeyPerformanceIndicators() {
            return keyPerformanceIndicators;
        }

        public void setKeyPerformanceIndicators(Map<String, Object> keyPerformanceIndicators) {
            this.keyPerformanceIndicators = keyPerformanceIndicators;
        }

        public Map<String, Long> getCounts() {
            return counts;
        }

        public void setCounts(Map<String, Long> counts) {
            this.counts = counts;
        }

        public Map<String, BigDecimal> getPercentages() {
            return percentages;
        }

        public void setPercentages(Map<String, BigDecimal> percentages) {
            this.percentages = percentages;
        }

        public Map<String, Object> getTrends() {
            return trends;
        }

        public void setTrends(Map<String, Object> trends) {
            this.trends = trends;
        }
    }

    /**
     * Builder pattern implementation for AdminReportResponse construction
     */
    public static class Builder {
        private List<Object> reportData;
        private ReportMetadata metadata;
        private PaginationInfo paginationInfo;
        private SummaryStatistics summaryStatistics;
        private Status status;
        private List<String> errorMessages;
        private Long executionDuration;

        public Builder() {
            // Initialize collections to prevent null pointer exceptions
            this.reportData = new ArrayList<>();
            this.errorMessages = new ArrayList<>();
        }

        public Builder reportData(List<Object> reportData) {
            this.reportData = reportData;
            return this;
        }

        public Builder metadata(ReportMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder paginationInfo(PaginationInfo paginationInfo) {
            this.paginationInfo = paginationInfo;
            return this;
        }

        public Builder summaryStatistics(SummaryStatistics summaryStatistics) {
            this.summaryStatistics = summaryStatistics;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder errorMessages(List<String> errorMessages) {
            this.errorMessages = errorMessages;
            return this;
        }

        public Builder addErrorMessage(String errorMessage) {
            if (this.errorMessages == null) {
                this.errorMessages = new ArrayList<>();
            }
            this.errorMessages.add(errorMessage);
            return this;
        }

        public Builder executionDuration(Long executionDuration) {
            this.executionDuration = executionDuration;
            return this;
        }

        public AdminReportResponse build() {
            return new AdminReportResponse(this);
        }
    }
}