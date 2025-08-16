package com.carddemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing report metadata and parameters for transaction reports,
 * daily summaries, audit reports, and regulatory compliance reports.
 * 
 * This entity supports the reporting functionality converted from CBTRN03C and CORPT00C 
 * COBOL programs, providing comprehensive report lifecycle management with date ranges, 
 * report types, and generation status tracking.
 */
@Entity
@Table(name = "report", indexes = {
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_user_id", columnList = "user_id"),
    @Index(name = "idx_report_created_at", columnList = "created_at"),
    @Index(name = "idx_report_date_range", columnList = "start_date, end_date")
})
public class Report {

    /**
     * Enumeration of supported report types based on COBOL CORPT00C functionality.
     * Maps to the monthly, yearly, and custom report options from the original system.
     */
    public enum ReportType {
        TRANSACTION_DETAIL("TRAN_DETAIL", "Transaction Detail Report"),
        DAILY_SUMMARY("DAILY_SUM", "Daily Summary Report"),
        MONTHLY("MONTHLY", "Monthly Report"),
        YEARLY("YEARLY", "Yearly Report"),
        CUSTOM("CUSTOM", "Custom Date Range Report"),
        AUDIT("AUDIT", "Audit Report"),
        COMPLIANCE("COMPLIANCE", "Regulatory Compliance Report");

        private final String code;
        private final String description;

        ReportType(String code, String description) {
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
     * Enumeration of report generation status values.
     * Tracks the lifecycle from request through completion or failure.
     */
    public enum Status {
        REQUESTED("REQUESTED", "Report generation requested"),
        QUEUED("QUEUED", "Report queued for processing"),
        PROCESSING("PROCESSING", "Report generation in progress"),
        COMPLETED("COMPLETED", "Report generation completed successfully"),
        FAILED("FAILED", "Report generation failed"),
        CANCELLED("CANCELLED", "Report generation cancelled"),
        EXPIRED("EXPIRED", "Report file has expired");

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
     * Enumeration of supported report output formats.
     * Based on the file generation capabilities from CBTRN03C batch program.
     */
    public enum Format {
        PDF("PDF", "Portable Document Format", "application/pdf"),
        CSV("CSV", "Comma Separated Values", "text/csv"),
        TEXT("TEXT", "Plain Text", "text/plain"),
        EXCEL("EXCEL", "Microsoft Excel", "application/vnd.ms-excel");

        private final String code;
        private final String description;
        private final String mimeType;

        Format(String code, String description, String mimeType) {
            this.code = code;
            this.description = description;
            this.mimeType = mimeType;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 20, nullable = false)
    private ReportType reportType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    private Status status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 10, nullable = false)
    private Format format;

    @Size(max = 500)
    @Column(name = "file_path", length = 500)
    private String filePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Size(max = 50)
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Size(max = 1000)
    @Column(name = "parameters", length = 1000)
    private String parameters;

    @Size(max = 500)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    // Default constructor
    public Report() {
        this.status = Status.REQUESTED;
        this.format = Format.PDF;
    }

    // Constructor with required fields
    public Report(ReportType reportType, String userId, Format format) {
        this();
        this.reportType = reportType;
        this.userId = userId;
        this.format = format;
    }

    // Constructor with date range
    public Report(ReportType reportType, LocalDate startDate, LocalDate endDate, 
                  String userId, Format format) {
        this(reportType, userId, format);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Gets the unique identifier for this report.
     * @return the report ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this report.
     * @param id the report ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the type of this report.
     * @return the report type
     */
    public ReportType getReportType() {
        return reportType;
    }

    /**
     * Sets the type of this report.
     * @param reportType the report type
     */
    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    /**
     * Gets the start date for the report data range.
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date for the report data range.
     * @param startDate the start date
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the end date for the report data range.
     * @return the end date
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date for the report data range.
     * @param endDate the end date
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the current status of the report generation.
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the current status of the report generation.
     * @param status the status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the output format for this report.
     * @return the format
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the output format for this report.
     * @param format the format
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Gets the file path where the generated report is stored.
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the file path where the generated report is stored.
     * @param filePath the file path
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Gets the timestamp when this report was created/requested.
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this report was created/requested.
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the user ID who requested this report.
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID who requested this report.
     * @param userId the user ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets additional parameters for report generation.
     * @return the parameters as JSON string
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Sets additional parameters for report generation.
     * @param parameters the parameters as JSON string
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /**
     * Gets the error message if report generation failed.
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message if report generation failed.
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the number of records included in the generated report.
     * @return the record count
     */
    public Long getRecordCount() {
        return recordCount;
    }

    /**
     * Sets the number of records included in the generated report.
     * @param recordCount the record count
     */
    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    /**
     * Gets the size of the generated report file in bytes.
     * @return the file size in bytes
     */
    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    /**
     * Sets the size of the generated report file in bytes.
     * @param fileSizeBytes the file size in bytes
     */
    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    /**
     * Gets the timestamp when report processing started.
     * @return the processing start timestamp
     */
    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    /**
     * Sets the timestamp when report processing started.
     * @param processingStartedAt the processing start timestamp
     */
    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    /**
     * Gets the timestamp when report processing completed.
     * @return the processing completion timestamp
     */
    public LocalDateTime getProcessingCompletedAt() {
        return processingCompletedAt;
    }

    /**
     * Sets the timestamp when report processing completed.
     * @param processingCompletedAt the processing completion timestamp
     */
    public void setProcessingCompletedAt(LocalDateTime processingCompletedAt) {
        this.processingCompletedAt = processingCompletedAt;
    }

    /**
     * Convenience method to check if the report has completed successfully.
     * @return true if the report is completed, false otherwise
     */
    public boolean isCompleted() {
        return Status.COMPLETED.equals(this.status);
    }

    /**
     * Convenience method to check if the report has failed.
     * @return true if the report failed, false otherwise
     */
    public boolean isFailed() {
        return Status.FAILED.equals(this.status);
    }

    /**
     * Convenience method to check if the report is currently processing.
     * @return true if the report is processing, false otherwise
     */
    public boolean isProcessing() {
        return Status.PROCESSING.equals(this.status) || Status.QUEUED.equals(this.status);
    }

    /**
     * Marks the report as started processing and sets the processing start timestamp.
     */
    public void startProcessing() {
        this.status = Status.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    /**
     * Marks the report as completed successfully and sets the completion timestamp.
     * @param filePath the path to the generated report file
     * @param recordCount the number of records in the report
     * @param fileSizeBytes the size of the generated file
     */
    public void markCompleted(String filePath, Long recordCount, Long fileSizeBytes) {
        this.status = Status.COMPLETED;
        this.processingCompletedAt = LocalDateTime.now();
        this.filePath = filePath;
        this.recordCount = recordCount;
        this.fileSizeBytes = fileSizeBytes;
        this.errorMessage = null;
    }

    /**
     * Marks the report as failed and sets the error message.
     * @param errorMessage the error message describing the failure
     */
    public void markFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.processingCompletedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", reportType=" + reportType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", format=" + format +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        
        Report report = (Report) o;
        return id != null && id.equals(report.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}