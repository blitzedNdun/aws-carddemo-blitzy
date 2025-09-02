package com.carddemo.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * Response DTO for report menu display and operations from CORPT00 BMS screen.
 * Contains report menu options, validation results, success confirmation, error messages, 
 * and system context information. Supports Monthly, Yearly, and Custom date range report 
 * selections with comprehensive date validation feedback.
 * 
 * This DTO maps directly to the CORPT00 BMS screen structure and provides all necessary
 * data for rendering the report menu interface, handling validation errors, and displaying
 * system information matching the original COBOL implementation patterns.
 * 
 * @author Blitzy Agent
 * @version 1.0
 * @since 2024-01-01
 */
public class ReportMenuResponse {

    /**
     * Available report menu options corresponding to BMS screen choices
     */
    public enum ReportMenuOption {
        MONTHLY("Monthly", "Monthly (Current Month)"),
        YEARLY("Yearly", "Yearly (Current Year)"),
        CUSTOM("Custom", "Custom (Date Range)");

        private final String code;
        private final String description;

        ReportMenuOption(String code, String description) {
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
     * Report processing status enumeration
     */
    public enum ReportStatus {
        INITIAL("INITIAL", "Initial state - no report selected"),
        VALIDATING("VALIDATING", "Validating input parameters"),
        CONFIRMED("CONFIRMED", "Report confirmed and ready for submission"),
        SUBMITTED("SUBMITTED", "Report submitted for processing"),
        ERROR("ERROR", "Error occurred during processing");

        private final String code;
        private final String description;

        ReportStatus(String code, String description) {
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
     * System information container matching BMS header fields
     */
    public static class SystemInfo {
        private String currentDate;
        private String currentTime;
        private String programName;
        private String transactionId;

        public SystemInfo() {
        }

        public SystemInfo(String currentDate, String currentTime, String programName, String transactionId) {
            this.currentDate = currentDate;
            this.currentTime = currentTime;
            this.programName = programName;
            this.transactionId = transactionId;
        }

        public String getCurrentDate() {
            return currentDate;
        }

        public void setCurrentDate(String currentDate) {
            this.currentDate = currentDate;
        }

        public String getCurrentTime() {
            return currentTime;
        }

        public void setCurrentTime(String currentTime) {
            this.currentTime = currentTime;
        }

        public String getProgramName() {
            return programName;
        }

        public void setProgramName(String programName) {
            this.programName = programName;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }
    }

    /**
     * Available report menu options (Monthly, Yearly, Custom)
     */
    private List<ReportMenuOption> reportMenuOptions;

    /**
     * Field-level validation errors for custom date range inputs
     * Maps to specific BMS field validation failures from COBOL program
     */
    private List<ValidationError> validationErrors;

    /**
     * General error message for system-level or processing errors
     */
    private String errorMessage;

    /**
     * Success confirmation message when report is submitted
     */
    private String successMessage;

    /**
     * Current report processing status
     */
    private ReportStatus reportStatus;

    /**
     * Type of report that was submitted (Monthly, Yearly, Custom)
     */
    private String submittedReportType;

    /**
     * System context information (date, time, program name, transaction ID)
     */
    private SystemInfo systemInfo;

    /**
     * Default constructor initializing collections and default values
     */
    public ReportMenuResponse() {
        this.reportMenuOptions = new ArrayList<>();
        this.validationErrors = new ArrayList<>();
        this.reportStatus = ReportStatus.INITIAL;
        this.systemInfo = new SystemInfo();
        
        // Initialize available report menu options
        this.reportMenuOptions.add(ReportMenuOption.MONTHLY);
        this.reportMenuOptions.add(ReportMenuOption.YEARLY);
        this.reportMenuOptions.add(ReportMenuOption.CUSTOM);
    }

    /**
     * Gets the list of validation errors for field-level validation failures
     * 
     * @return list of validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Sets the list of validation errors
     * 
     * @param validationErrors list of validation errors to set
     */
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
    }

    /**
     * Gets the general error message
     * 
     * @return error message string
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the general error message
     * 
     * @param errorMessage error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the success confirmation message
     * 
     * @return success message string
     */
    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Sets the success confirmation message
     * 
     * @param successMessage success message to set
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    /**
     * Gets the current report processing status
     * 
     * @return current report status
     */
    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    /**
     * Sets the report processing status
     * 
     * @param reportStatus status to set
     */
    public void setReportStatus(ReportStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    /**
     * Gets the available report menu options
     * 
     * @return list of report menu options
     */
    public List<ReportMenuOption> getReportMenuOptions() {
        return reportMenuOptions;
    }

    /**
     * Gets the system context information
     * 
     * @return system information object
     */
    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    /**
     * Sets the system context information
     * 
     * @param systemInfo system information to set
     */
    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo != null ? systemInfo : new SystemInfo();
    }

    /**
     * Gets the type of report that was submitted
     * 
     * @return submitted report type
     */
    public String getSubmittedReportType() {
        return submittedReportType;
    }

    /**
     * Sets the type of report that was submitted
     * 
     * @param submittedReportType report type to set
     */
    public void setSubmittedReportType(String submittedReportType) {
        this.submittedReportType = submittedReportType;
    }

    /**
     * Adds a validation error to the list
     * 
     * @param validationError error to add
     */
    public void addValidationError(ValidationError validationError) {
        if (validationError != null) {
            this.validationErrors.add(validationError);
        }
    }

    /**
     * Adds a validation error with field, code, and message
     * 
     * @param field field name that failed validation
     * @param code error code
     * @param message error message
     */
    public void addValidationError(String field, String code, String message) {
        this.validationErrors.add(new ValidationError(field, code, message));
    }

    /**
     * Adds a validation error with field, code, message, and suggested correction
     * 
     * @param field field name that failed validation
     * @param code error code
     * @param message error message
     * @param suggestedCorrection suggested correction
     */
    public void addValidationError(String field, String code, String message, String suggestedCorrection) {
        this.validationErrors.add(new ValidationError(field, code, message, suggestedCorrection));
    }

    /**
     * Checks if there are any validation errors
     * 
     * @return true if validation errors exist, false otherwise
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Checks if there is an error message
     * 
     * @return true if error message exists, false otherwise
     */
    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    /**
     * Checks if there is a success message
     * 
     * @return true if success message exists, false otherwise
     */
    public boolean hasSuccessMessage() {
        return successMessage != null && !successMessage.trim().isEmpty();
    }

    /**
     * Checks if any errors exist (validation errors or general error message)
     * 
     * @return true if any errors exist, false otherwise
     */
    public boolean hasAnyErrors() {
        return hasValidationErrors() || hasErrorMessage();
    }

    /**
     * Clears all validation errors
     */
    public void clearValidationErrors() {
        this.validationErrors.clear();
    }

    /**
     * Clears the general error message
     */
    public void clearErrorMessage() {
        this.errorMessage = null;
    }

    /**
     * Clears the success message
     */
    public void clearSuccessMessage() {
        this.successMessage = null;
    }

    /**
     * Clears all messages (validation errors, error message, success message)
     */
    public void clearAllMessages() {
        clearValidationErrors();
        clearErrorMessage();
        clearSuccessMessage();
    }

    /**
     * Gets validation error for a specific field
     * 
     * @param fieldName name of the field to find error for
     * @return validation error for the field, or null if not found
     */
    public ValidationError getValidationErrorForField(String fieldName) {
        if (fieldName == null || validationErrors == null) {
            return null;
        }
        
        return validationErrors.stream()
                .filter(error -> fieldName.equals(error.getField()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a specific field has a validation error
     * 
     * @param fieldName name of the field to check
     * @return true if field has validation error, false otherwise
     */
    public boolean hasValidationErrorForField(String fieldName) {
        return getValidationErrorForField(fieldName) != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReportMenuResponse{");
        sb.append("reportMenuOptions=").append(reportMenuOptions);
        sb.append(", validationErrors=").append(validationErrors);
        sb.append(", errorMessage='").append(errorMessage).append('\'');
        sb.append(", successMessage='").append(successMessage).append('\'');
        sb.append(", reportStatus=").append(reportStatus);
        sb.append(", submittedReportType='").append(submittedReportType).append('\'');
        sb.append(", systemInfo=").append(systemInfo);
        sb.append('}');
        return sb.toString();
    }
}