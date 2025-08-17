package com.carddemo.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object for file upload operation responses containing upload success status,
 * number of records processed, validation errors, and processing metadata. Used by FileController 
 * to return standardized responses for file upload operations in the CardDemo application.
 * 
 * This class supports file-based interfaces with external systems by providing detailed 
 * validation feedback and processing statistics. It maintains compatibility with the 
 * modernized Spring Boot architecture while preserving the structured response patterns
 * from the original COBOL batch processing system.
 * 
 * Key Features:
 * - Success/failure status indication
 * - Record count tracking for processed records
 * - Comprehensive validation error collection
 * - Processing time metrics and file metadata
 * - Support for different file format results
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class FileUploadResponse {

    /**
     * Indicates whether the file upload operation completed successfully
     */
    private boolean success;

    /**
     * Number of records successfully processed from the uploaded file
     */
    private long recordCount;

    /**
     * List of validation errors encountered during file processing
     */
    private List<ValidationError> validationErrors;

    /**
     * Total time taken to process the file upload (in milliseconds)
     */
    private long processingTime;

    /**
     * Original name of the uploaded file
     */
    private String fileName;

    /**
     * Size of the uploaded file in bytes
     */
    private long fileSize;

    /**
     * Timestamp when the file processing started
     */
    private LocalDateTime processedAt;

    /**
     * Number of records that failed validation
     */
    private long errorCount;

    /**
     * File format type (e.g., "FIXED_WIDTH", "CSV", "DELIMITED")
     */
    private String fileFormat;

    /**
     * Default constructor for FileUploadResponse
     */
    public FileUploadResponse() {
        this.validationErrors = new ArrayList<>();
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Constructor for successful file upload response
     * 
     * @param recordCount number of successfully processed records
     * @param processingTime time taken to process the file
     * @param fileName original name of the uploaded file
     * @param fileSize size of the uploaded file in bytes
     */
    public FileUploadResponse(long recordCount, long processingTime, String fileName, long fileSize) {
        this();
        this.success = true;
        this.recordCount = recordCount;
        this.processingTime = processingTime;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.errorCount = 0;
    }

    /**
     * Constructor for failed file upload response with validation errors
     * 
     * @param validationErrors list of validation errors encountered
     * @param processingTime time taken before failure
     * @param fileName original name of the uploaded file
     * @param fileSize size of the uploaded file in bytes
     */
    public FileUploadResponse(List<ValidationError> validationErrors, long processingTime, String fileName, long fileSize) {
        this();
        this.success = false;
        this.recordCount = 0;
        this.validationErrors = validationErrors != null ? new ArrayList<>(validationErrors) : new ArrayList<>();
        this.processingTime = processingTime;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.errorCount = this.validationErrors.size();
    }

    /**
     * Gets the success status of the file upload operation
     * 
     * @return true if upload was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the file upload operation
     * 
     * @param success true if upload was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the number of records successfully processed from the uploaded file
     * 
     * @return number of processed records
     */
    public long getRecordCount() {
        return recordCount;
    }

    /**
     * Sets the number of records successfully processed
     * 
     * @param recordCount number of processed records
     */
    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }

    /**
     * Gets the list of validation errors encountered during file processing
     * 
     * @return list of validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the list of validation errors
     * 
     * @param validationErrors list of validation errors
     */
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors != null ? new ArrayList<>(validationErrors) : new ArrayList<>();
        this.errorCount = this.validationErrors.size();
    }

    /**
     * Gets the total time taken to process the file upload (in milliseconds)
     * 
     * @return processing time in milliseconds
     */
    public long getProcessingTime() {
        return processingTime;
    }

    /**
     * Sets the processing time for the file upload
     * 
     * @param processingTime processing time in milliseconds
     */
    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    /**
     * Gets the original name of the uploaded file
     * 
     * @return original file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the original name of the uploaded file
     * 
     * @param fileName original file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Gets the size of the uploaded file in bytes
     * 
     * @return file size in bytes
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Sets the size of the uploaded file
     * 
     * @param fileSize file size in bytes
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Gets the timestamp when the file processing started
     * 
     * @return processing start timestamp
     */
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    /**
     * Sets the timestamp when the file processing started
     * 
     * @param processedAt processing start timestamp
     */
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    /**
     * Gets the number of records that failed validation
     * 
     * @return number of error records
     */
    public long getErrorCount() {
        return errorCount;
    }

    /**
     * Sets the number of records that failed validation
     * 
     * @param errorCount number of error records
     */
    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * Gets the file format type
     * 
     * @return file format (e.g., "FIXED_WIDTH", "CSV", "DELIMITED")
     */
    public String getFileFormat() {
        return fileFormat;
    }

    /**
     * Sets the file format type
     * 
     * @param fileFormat file format type
     */
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    /**
     * Adds a validation error to the error list
     * 
     * @param validationError the validation error to add
     */
    public void addValidationError(ValidationError validationError) {
        if (validationError != null) {
            this.validationErrors.add(validationError);
            this.errorCount = this.validationErrors.size();
            // If there are validation errors, the upload is considered unsuccessful
            if (!this.validationErrors.isEmpty()) {
                this.success = false;
            }
        }
    }

    /**
     * Adds a validation error using individual components
     * 
     * @param field the field name that failed validation
     * @param code the error code
     * @param message the error message
     */
    public void addValidationError(String field, String code, String message) {
        ValidationError error = new ValidationError();
        error.setField(field);
        error.setCode(code);
        error.setMessage(message);
        addValidationError(error);
    }

    /**
     * Checks if the upload has any validation errors
     * 
     * @return true if there are validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Gets the success rate as a percentage
     * 
     * @return success rate percentage (0.0 to 100.0)
     */
    public double getSuccessRate() {
        long totalRecords = recordCount + errorCount;
        if (totalRecords == 0) {
            return 100.0;
        }
        return (double) recordCount / totalRecords * 100.0;
    }

    /**
     * Creates a summary string of the upload results
     * 
     * @return summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("File Upload Result: ");
        summary.append(success ? "SUCCESS" : "FAILED");
        summary.append(" | Records: ").append(recordCount);
        summary.append(" | Errors: ").append(errorCount);
        summary.append(" | Time: ").append(processingTime).append("ms");
        summary.append(" | File: ").append(fileName);
        summary.append(" (").append(fileSize).append(" bytes)");
        return summary.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileUploadResponse{");
        sb.append("success=").append(success);
        sb.append(", recordCount=").append(recordCount);
        sb.append(", errorCount=").append(errorCount);
        sb.append(", processingTime=").append(processingTime);
        sb.append(", fileName='").append(fileName).append('\'');
        sb.append(", fileSize=").append(fileSize);
        sb.append(", fileFormat='").append(fileFormat).append('\'');
        sb.append(", processedAt=").append(processedAt);
        if (hasValidationErrors()) {
            sb.append(", validationErrorsCount=").append(validationErrors.size());
        }
        sb.append('}');
        return sb.toString();
    }
}