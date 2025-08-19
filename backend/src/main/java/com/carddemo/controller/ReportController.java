package com.carddemo.controller;

import com.carddemo.dto.ReportRequestDto;
import com.carddemo.service.ReportService;
import com.carddemo.dto.ApiResponse;
import com.carddemo.dto.Message;
import com.carddemo.dto.ResponseStatus;
import com.carddemo.exception.ValidationException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for report generation handling CR00 transaction code.
 * 
 * This controller replaces the COBOL CORPT00C program functionality by providing
 * a REST endpoint for report generation requests. It maintains the same business
 * logic and validation patterns as the original COBOL program while adapting
 * to modern Spring Boot REST architecture.
 * 
 * Key functionality includes:
 * - Monthly report generation for current month transactions
 * - Yearly report generation for current year transactions 
 * - Custom date range report generation with comprehensive validation
 * - Job submission to Spring Batch (replacing CICS TDQ submission)
 * - Comprehensive error handling and validation messaging
 * 
 * Transaction Code Mapping:
 * - COBOL Transaction: CR00 (CORPT00C)
 * - REST Endpoint: POST /api/reports/generate
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    
    /**
     * Report service for handling report generation logic.
     * Injected Spring service that implements the business logic
     * translated from CORPT00C.cbl COBOL program.
     */
    @Autowired
    private ReportService reportService;
    
    /**
     * Date formatter for converting LocalDate to YYYYMMDD format
     * matching COBOL date handling patterns from CORPT00C.
     */
    private static final DateTimeFormatter YYYYMMDD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * Generate a report based on the provided request parameters.
     * 
     * This endpoint replaces the COBOL CORPT00C program functionality,
     * handling the same three report types with identical validation logic:
     * 
     * 1. MONTHLY: Generates report for current month (automatically calculated dates)
     * 2. YEARLY: Generates report for current year (automatically calculated dates)  
     * 3. CUSTOM: Generates report for user-specified date range (requires validation)
     * 
     * The method performs comprehensive validation matching the original COBOL
     * validation routines including:
     * - Report type selection validation
     * - Custom date range validation (when applicable)
     * - Date format and business rule validation
     * - Confirmation flag validation
     * 
     * On successful validation, the request is submitted to the ReportService
     * which handles Spring Batch job submission (replacing COBOL TDQ submission).
     * 
     * @param request The report generation request containing:
     *                - reportType: MONTHLY, YEARLY, or CUSTOM
     *                - startDate: Required for CUSTOM reports (YYYY-MM-DD format)
     *                - endDate: Required for CUSTOM reports (YYYY-MM-DD format)  
     *                - confirmationFlag: Required confirmation (true/false)
     * @return ApiResponse containing:
     *         - SUCCESS status with job submission details, or
     *         - ERROR status with validation error messages
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReport(
            @Valid @RequestBody ReportRequestDto request) {
        
        logger.info("Report generation request received - Type: {}, Confirmation: {}", 
                   request.getReportType(), request.isConfirmationFlag());
        
        // Create response wrapper matching CICS response patterns
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setTransactionCode("CR00");
        
        try {
            // Validate confirmation flag first (matching COBOL SUBMIT-JOB-TO-INTRDR logic)
            if (!request.isConfirmationFlag()) {
                String reportTypeDesc = getReportTypeDescription(request.getReportType());
                String confirmationMessage = String.format(
                    "Please confirm to print the %s report...", reportTypeDesc);
                
                response.setStatus(ResponseStatus.ERROR);
                response.addMessage(Message.error(confirmationMessage));
                
                logger.info("Report generation requires confirmation - Type: {}", request.getReportType());
                return ResponseEntity.badRequest().body(response);
            }
            
            // Perform additional validation for custom date ranges
            if (request.getReportType() == ReportRequestDto.ReportType.CUSTOM) {
                try {
                    request.validateDateRange();
                } catch (IllegalArgumentException e) {
                    response.setStatus(ResponseStatus.ERROR);
                    response.addMessage(Message.error(e.getMessage()));
                    
                    logger.warn("Custom date range validation failed: {}", e.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Calculate date range based on report type
            String reportTypeCode = convertReportTypeToCode(request.getReportType());
            String startDateStr = null;
            String endDateStr = null;
            
            if (request.getReportType() == ReportRequestDto.ReportType.CUSTOM) {
                // Use provided custom dates
                startDateStr = request.getStartDate().format(YYYYMMDD_FORMAT);
                endDateStr = request.getEndDate().format(YYYYMMDD_FORMAT);
            } else {
                // Calculate dates automatically for MONTHLY/YEARLY
                LocalDate calculatedStart = request.getCalculatedStartDate();
                LocalDate calculatedEnd = request.getCalculatedEndDate();
                startDateStr = calculatedStart.format(YYYYMMDD_FORMAT);
                endDateStr = calculatedEnd.format(YYYYMMDD_FORMAT);
            }
            
            logger.debug("Processing report request - Type: {}, Start: {}, End: {}", 
                        reportTypeCode, startDateStr, endDateStr);
            
            // Submit job to ReportService (replacing COBOL TDQ submission)
            Map<String, Object> jobResult = reportService.processReportRequest(
                reportTypeCode, startDateStr, endDateStr);
            
            if ((Boolean) jobResult.get("success")) {
                // Successful job submission
                response.setStatus(ResponseStatus.SUCCESS);
                response.setResponseData(jobResult);
                
                // Add success message matching COBOL success pattern
                String reportTypeDesc = getReportTypeDescription(request.getReportType());
                String successMessage = String.format("%s report submitted for printing...", reportTypeDesc);
                response.addMessage(Message.info(successMessage));
                
                logger.info("Report job submitted successfully - Job ID: {}, Type: {}", 
                           jobResult.get("jobId"), request.getReportType());
                
                return ResponseEntity.ok(response);
                
            } else {
                // Job submission failed
                response.setStatus(ResponseStatus.ERROR);
                response.addMessage(Message.error((String) jobResult.get("errorMessage")));
                
                logger.error("Report job submission failed: {}", jobResult.get("errorMessage"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (ValidationException e) {
            // Handle field-level validation errors
            response.setStatus(ResponseStatus.ERROR);
            
            // Add field-specific error messages
            Map<String, String> fieldErrors = e.getFieldErrors();
            for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
                response.addMessage(Message.error(entry.getValue()));
            }
            
            // Add general validation message if no field errors
            if (fieldErrors.isEmpty()) {
                response.addMessage(Message.error(e.getMessage()));
            }
            
            logger.warn("Validation error in report request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            // Handle unexpected system errors
            response.setStatus(ResponseStatus.ERROR);
            response.addMessage(Message.error("System error processing report request. Please try again."));
            
            logger.error("Unexpected error processing report request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Convert ReportType enum to single character code matching COBOL logic.
     * 
     * Maps the enum values to the single character codes used by the
     * ReportService, maintaining compatibility with the COBOL program
     * structure and backend processing requirements.
     * 
     * @param reportType The ReportType enum value
     * @return Single character code ("M", "Y", or "C")
     */
    private String convertReportTypeToCode(ReportRequestDto.ReportType reportType) {
        switch (reportType) {
            case MONTHLY:
                return "M";
            case YEARLY:
                return "Y";
            case CUSTOM:
                return "C";
            default:
                throw new IllegalArgumentException("Invalid report type: " + reportType);
        }
    }
    
    /**
     * Get human-readable description for report type.
     * 
     * Provides user-friendly descriptions matching the COBOL program
     * message patterns for use in success and error messages.
     * 
     * @param reportType The ReportType enum value
     * @return Human-readable description string
     */
    private String getReportTypeDescription(ReportRequestDto.ReportType reportType) {
        switch (reportType) {
            case MONTHLY:
                return "Monthly";
            case YEARLY:
                return "Yearly";
            case CUSTOM:
                return "Custom";
            default:
                return "Unknown";
        }
    }
}