package com.carddemo.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.HashMap;

/**
 * Spring Boot service implementing the main report orchestration logic translated from CORPT00C.cbl.
 * Coordinates report type selection (Monthly/Yearly/Custom), date range validation, 
 * job parameter generation, and Spring Batch job submission.
 * 
 * Acts as a facade service that delegates to report generation components while handling 
 * the core job submission workflow that replaces the original TDQ (Transient Data Queue) 
 * job submission mechanism.
 * 
 * This service maintains the same business logic flow as the original COBOL program:
 * - MAIN-PARA -> processReportRequest()
 * - PROCESS-ENTER-KEY -> handleReportSelection() 
 * - SUBMIT-JOB-TO-INTRDR -> submitReportJob()
 * - Date validation and calculation logic preserved
 */
@Service
@org.springframework.context.annotation.Profile("!test")
public class ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    // Date formatters matching COBOL date handling
    private static final DateTimeFormatter YYYYMMDD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // Report type constants matching COBOL program values
    private static final String REPORT_TYPE_MONTHLY = "M";
    private static final String REPORT_TYPE_YEARLY = "Y"; 
    private static final String REPORT_TYPE_CUSTOM = "C";
    
    // Job queue simulation - replacing TDQ functionality
    private static final String JOB_QUEUE_NAME = "JOBS";
    
    @Autowired
    private JobLauncher jobLauncher;
    
    // Inject the appropriate batch job - this would be configured based on report type
    @Autowired
    @Qualifier("reportGenerationJob")
    private Job reportGenerationJob;

    /**
     * Main orchestration method translated from COBOL MAIN-PARA.
     * Processes report request based on user selection and initiates job submission.
     * 
     * @param reportType Report type indicator (M/Y/C)
     * @param customStartDate Optional start date for custom reports (YYYYMMDD format)
     * @param customEndDate Optional end date for custom reports (YYYYMMDD format)
     * @return Job execution result with status and job ID
     */
    public Map<String, Object> processReportRequest(String reportType, String customStartDate, String customEndDate) {
        logger.info("Processing report request - Type: {}, Start: {}, End: {}", 
                   reportType, customStartDate, customEndDate);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate report type selection
            if (!isValidReportType(reportType)) {
                result.put("success", false);
                result.put("errorMessage", "Invalid report type. Must be M (Monthly), Y (Yearly), or C (Custom)");
                return result;
            }
            
            // Handle report selection and date range calculation
            Map<String, String> dateRange = handleReportSelection(reportType, customStartDate, customEndDate);
            
            if (dateRange.containsKey("error")) {
                result.put("success", false);
                result.put("errorMessage", dateRange.get("error"));
                return result;
            }
            
            // Generate job parameters with calculated date range
            JobParameters jobParameters = generateJobParameters(reportType, dateRange);
            
            // Submit job to Spring Batch (replaces TDQ submission)
            JobExecution jobExecution = submitReportJob(jobParameters);
            
            // Write job submission details to queue simulation
            writeJobToQueue(reportType, dateRange, jobExecution.getId());
            
            result.put("success", true);
            result.put("jobId", jobExecution.getId());
            result.put("jobStatus", jobExecution.getStatus().toString());
            result.put("reportType", reportType);
            result.put("startDate", dateRange.get("startDate"));
            result.put("endDate", dateRange.get("endDate"));
            
            logger.info("Report job submitted successfully - Job ID: {}, Status: {}", 
                       jobExecution.getId(), jobExecution.getStatus());
            
        } catch (Exception e) {
            logger.error("Error processing report request", e);
            result.put("success", false);
            result.put("errorMessage", "System error processing report request: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Handles report type selection and date range determination.
     * Translated from COBOL PROCESS-ENTER-KEY paragraph.
     * 
     * @param reportType Report type indicator
     * @param customStartDate Custom start date for C type reports
     * @param customEndDate Custom end date for C type reports  
     * @return Map containing calculated start and end dates or error message
     */
    public Map<String, String> handleReportSelection(String reportType, String customStartDate, String customEndDate) {
        logger.debug("Handling report selection for type: {}", reportType);
        
        Map<String, String> dateRange = new HashMap<>();
        
        try {
            switch (reportType.toUpperCase()) {
                case REPORT_TYPE_MONTHLY:
                    dateRange = calculateMonthlyDateRange();
                    break;
                    
                case REPORT_TYPE_YEARLY:
                    dateRange = calculateYearlyDateRange();
                    break;
                    
                case REPORT_TYPE_CUSTOM:
                    dateRange = validateCustomDateRange(customStartDate, customEndDate);
                    break;
                    
                default:
                    dateRange.put("error", "Invalid report type selection");
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Error handling report selection", e);
            dateRange.put("error", "Error calculating date range: " + e.getMessage());
        }
        
        return dateRange;
    }

    /**
     * Submits report generation job to Spring Batch.
     * Translated from COBOL SUBMIT-JOB-TO-INTRDR paragraph.
     * 
     * @param jobParameters Job parameters including date range and report type
     * @return JobExecution instance with execution details
     */
    public JobExecution submitReportJob(JobParameters jobParameters) throws Exception {
        logger.info("Submitting report job with parameters: {}", jobParameters.getParameters());
        
        try {
            // Launch the Spring Batch job (replaces TDQ job submission)
            JobExecution jobExecution = jobLauncher.run(reportGenerationJob, jobParameters);
            
            logger.info("Report job launched successfully - Job ID: {}, Status: {}", 
                       jobExecution.getId(), jobExecution.getStatus());
            
            return jobExecution;
            
        } catch (Exception e) {
            logger.error("Failed to submit report job", e);
            throw new Exception("Error submitting report job: " + e.getMessage(), e);
        }
    }

    /**
     * Validates date range for report processing.
     * Implements comprehensive date validation logic from original COBOL program.
     * 
     * @param startDate Start date for validation
     * @param endDate End date for validation
     * @return true if date range is valid, false otherwise
     */
    public boolean validateDateRange(String startDate, String endDate) {
        try {
            if (startDate == null || endDate == null || startDate.trim().isEmpty() || endDate.trim().isEmpty()) {
                logger.warn("Date range validation failed - null or empty dates");
                return false;
            }
            
            LocalDate start = LocalDate.parse(startDate, YYYYMMDD_FORMAT);
            LocalDate end = LocalDate.parse(endDate, YYYYMMDD_FORMAT);
            LocalDate today = LocalDate.now();
            
            // Date range cannot be in the future
            if (start.isAfter(today) || end.isAfter(today)) {
                logger.warn("Date range validation failed - future dates not allowed");
                return false;
            }
            
            // Start date must be before or equal to end date
            if (start.isAfter(end)) {
                logger.warn("Date range validation failed - start date after end date");
                return false;
            }
            
            // Date range cannot exceed 1 year (365 days)
            if (start.plusDays(365).isBefore(end)) {
                logger.warn("Date range validation failed - exceeds 365 day limit");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating date range", e);
            return false;
        }
    }

    /**
     * Generates Spring Batch job parameters from report request.
     * Replaces COBOL JCL parameter generation logic.
     * 
     * @param reportType Type of report being generated
     * @param dateRange Date range for the report
     * @return JobParameters configured for batch processing
     */
    public JobParameters generateJobParameters(String reportType, Map<String, String> dateRange) {
        logger.debug("Generating job parameters for report type: {}", reportType);
        
        JobParametersBuilder builder = new JobParametersBuilder();
        
        // Add timestamp to ensure unique job executions
        builder.addLong("timestamp", System.currentTimeMillis());
        
        // Add report type parameter
        builder.addString("reportType", reportType);
        
        // Add date range parameters
        builder.addString("startDate", dateRange.get("startDate"));
        builder.addString("endDate", dateRange.get("endDate"));
        
        // Add formatted dates for display purposes
        builder.addString("startDateDisplay", dateRange.get("startDateDisplay"));
        builder.addString("endDateDisplay", dateRange.get("endDateDisplay"));
        
        // Add job identification parameters
        builder.addString("jobQueue", JOB_QUEUE_NAME);
        builder.addString("submittedBy", "REPORT-SERVICE");
        
        JobParameters jobParameters = builder.toJobParameters();
        
        logger.debug("Generated job parameters: {}", jobParameters.getParameters());
        
        return jobParameters;
    }

    /**
     * Simulates writing job submission details to transient data queue.
     * Replaces COBOL WRITEQ TD operations with logging and monitoring.
     * 
     * @param reportType Type of report submitted
     * @param dateRange Date range for the report
     * @param jobId Spring Batch job execution ID
     */
    public void writeJobToQueue(String reportType, Map<String, String> dateRange, Long jobId) {
        logger.info("Writing job submission to queue simulation - Job ID: {}, Type: {}, Range: {} to {}", 
                   jobId, reportType, dateRange.get("startDate"), dateRange.get("endDate"));
        
        // In the original COBOL program, this would write to TDQ 'JOBS'
        // In Spring Boot, we use structured logging for job tracking
        // and rely on Spring Batch's JobRepository for persistence
        
        try {
            // Create job submission record for monitoring
            Map<String, Object> jobRecord = new HashMap<>();
            jobRecord.put("jobId", jobId);
            jobRecord.put("reportType", reportType);
            jobRecord.put("startDate", dateRange.get("startDate"));
            jobRecord.put("endDate", dateRange.get("endDate"));
            jobRecord.put("queueName", JOB_QUEUE_NAME);
            jobRecord.put("submissionTime", System.currentTimeMillis());
            jobRecord.put("status", "SUBMITTED");
            
            // Log structured job submission data for monitoring systems
            logger.info("Job queue submission: {}", jobRecord);
            
            // Additional monitoring could include:
            // - Publishing to message queue for job tracking
            // - Updating job status in database
            // - Sending notifications to monitoring systems
            
        } catch (Exception e) {
            logger.error("Error writing job to queue simulation", e);
            // Non-critical error - job has already been submitted to Spring Batch
        }
    }

    /**
     * Calculates date range for monthly reports.
     * Replaces COBOL monthly date calculation logic.
     * 
     * @return Map containing start and end dates for current month
     */
    public Map<String, String> calculateMonthlyDateRange() {
        logger.debug("Calculating monthly date range");
        
        Map<String, String> dateRange = new HashMap<>();
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
            
            // Format dates for job parameters
            dateRange.put("startDate", startOfMonth.format(YYYYMMDD_FORMAT));
            dateRange.put("endDate", endOfMonth.format(YYYYMMDD_FORMAT));
            dateRange.put("startDateDisplay", startOfMonth.format(DISPLAY_FORMAT));
            dateRange.put("endDateDisplay", endOfMonth.format(DISPLAY_FORMAT));
            
            logger.debug("Monthly date range calculated: {} to {}", 
                        dateRange.get("startDate"), dateRange.get("endDate"));
            
        } catch (Exception e) {
            logger.error("Error calculating monthly date range", e);
            dateRange.put("error", "Error calculating monthly date range");
        }
        
        return dateRange;
    }

    /**
     * Calculates date range for yearly reports.
     * Replaces COBOL yearly date calculation logic.
     * 
     * @return Map containing start and end dates for current year
     */
    public Map<String, String> calculateYearlyDateRange() {
        logger.debug("Calculating yearly date range");
        
        Map<String, String> dateRange = new HashMap<>();
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfYear = today.with(TemporalAdjusters.firstDayOfYear());
            LocalDate endOfYear = today.with(TemporalAdjusters.lastDayOfYear());
            
            // Format dates for job parameters
            dateRange.put("startDate", startOfYear.format(YYYYMMDD_FORMAT));
            dateRange.put("endDate", endOfYear.format(YYYYMMDD_FORMAT));
            dateRange.put("startDateDisplay", startOfYear.format(DISPLAY_FORMAT));
            dateRange.put("endDateDisplay", endOfYear.format(DISPLAY_FORMAT));
            
            logger.debug("Yearly date range calculated: {} to {}", 
                        dateRange.get("startDate"), dateRange.get("endDate"));
            
        } catch (Exception e) {
            logger.error("Error calculating yearly date range", e);
            dateRange.put("error", "Error calculating yearly date range");
        }
        
        return dateRange;
    }

    /**
     * Validates and formats custom date range for reports.
     * Implements comprehensive date validation from original COBOL program.
     * 
     * @param customStartDate Start date in YYYYMMDD format
     * @param customEndDate End date in YYYYMMDD format
     * @return Map containing validated and formatted date range or error message
     */
    public Map<String, String> validateCustomDateRange(String customStartDate, String customEndDate) {
        logger.debug("Validating custom date range: {} to {}", customStartDate, customEndDate);
        
        Map<String, String> dateRange = new HashMap<>();
        
        try {
            // Validate input parameters
            if (customStartDate == null || customEndDate == null || 
                customStartDate.trim().isEmpty() || customEndDate.trim().isEmpty()) {
                dateRange.put("error", "Both start and end dates are required for custom reports");
                return dateRange;
            }
            
            // Validate date format and parse dates
            LocalDate startDate;
            LocalDate endDate;
            
            try {
                startDate = LocalDate.parse(customStartDate, YYYYMMDD_FORMAT);
                endDate = LocalDate.parse(customEndDate, YYYYMMDD_FORMAT);
            } catch (Exception e) {
                dateRange.put("error", "Invalid date format. Use YYYYMMDD format (e.g., 20240101)");
                return dateRange;
            }
            
            // Perform comprehensive validation using validateDateRange
            if (!validateDateRange(customStartDate, customEndDate)) {
                dateRange.put("error", "Invalid date range. Check dates are not in future, start <= end, and within 365 days");
                return dateRange;
            }
            
            // Format validated dates for job parameters
            dateRange.put("startDate", startDate.format(YYYYMMDD_FORMAT));
            dateRange.put("endDate", endDate.format(YYYYMMDD_FORMAT));
            dateRange.put("startDateDisplay", startDate.format(DISPLAY_FORMAT));
            dateRange.put("endDateDisplay", endDate.format(DISPLAY_FORMAT));
            
            logger.debug("Custom date range validated successfully: {} to {}", 
                        dateRange.get("startDate"), dateRange.get("endDate"));
            
        } catch (Exception e) {
            logger.error("Error validating custom date range", e);
            dateRange.put("error", "Error processing custom date range: " + e.getMessage());
        }
        
        return dateRange;
    }
    
    /**
     * Validates report type indicator.
     * Helper method for input validation.
     * 
     * @param reportType Report type to validate
     * @return true if valid report type, false otherwise
     */
    private boolean isValidReportType(String reportType) {
        if (reportType == null || reportType.trim().isEmpty()) {
            return false;
        }
        
        String upperType = reportType.toUpperCase();
        return REPORT_TYPE_MONTHLY.equals(upperType) || 
               REPORT_TYPE_YEARLY.equals(upperType) || 
               REPORT_TYPE_CUSTOM.equals(upperType);
    }
    
    /**
     * Gets list of available report types for admin menu display.
     * Used by AdminService to populate report options in admin menu.
     * 
     * @return List of available report type strings
     */
    public java.util.List<String> getAvailableReports() {
        return java.util.List.of("MONTHLY", "YEARLY", "CUSTOM", "STATEMENT");
    }
}