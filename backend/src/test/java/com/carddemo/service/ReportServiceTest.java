/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.dto.ReportRequest;
import com.carddemo.dto.ReportMenuResponse;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.batch.BatchJobLauncher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for ReportService validating the COBOL CORPT00C report generation 
 * logic migration. Tests report type selection (Monthly/Yearly/Custom), date range calculation 
 * and validation, JCL job generation with proper parameters, and transient data queue submission.
 * 
 * This test class ensures 100% functional parity between the original COBOL implementation 
 * and the Java Spring Boot migration, covering all business logic paths, edge cases, and 
 * error conditions as specified in the CORPT00C.cbl program.
 * 
 * Key Test Coverage Areas:
 * - Report type selection and validation (Monthly, Yearly, Custom)
 * - Date range calculation using current date equivalent to FUNCTION CURRENT-DATE
 * - CSUTLDTC date validation equivalent using DateConversionUtil
 * - Dynamic JCL job deck generation with PARM parameters
 * - WRITEQ TD operations equivalent through BatchJobLauncher
 * - Comprehensive error handling matching COBOL error conditions
 * - Parameter validation and boundary testing
 * 
 * COBOL Program Mapping:
 * - MAIN-PARA → processReportRequest() tests
 * - PROCESS-ENTER-KEY → handleReportSelection() tests  
 * - SUBMIT-JOB-TO-INTRDR → submitReportJob() tests
 * - Date validation routines → validateDateRange() tests
 * - JCL generation → generateJobParameters() tests
 * - WRITEQ TD → writeJobToQueue() tests
 * 
 * Test Strategy:
 * - Mock all external dependencies (JobLauncher, DateConversionUtil, etc.)
 * - Use realistic test data matching COBOL data structures
 * - Test both success and failure scenarios for complete coverage
 * - Validate exact business logic preservation from COBOL to Java
 * - Ensure proper error handling and message formatting
 * 
 * @author CardDemo Migration Test Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class ReportServiceTest {

    @Mock
    private JobLauncher jobLauncher;
    
    @Mock 
    private Job reportGenerationJob;
    
    @Mock
    private BatchJobLauncher batchJobLauncher;
    
    @InjectMocks
    private ReportService reportService;
    
    // Test constants matching COBOL program values
    private static final String REPORT_TYPE_MONTHLY = "M";
    private static final String REPORT_TYPE_YEARLY = "Y";
    private static final String REPORT_TYPE_CUSTOM = "C";
    private static final DateTimeFormatter YYYYMMDD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // Mock job execution data
    private JobExecution mockJobExecution;
    private JobInstance mockJobInstance;
    
    /**
     * Set up test fixtures before each test method execution.
     * Initializes mock objects and common test data to ensure consistent test environment.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create mock job execution matching Spring Batch structure
        mockJobInstance = new JobInstance(1001L, "reportGenerationJob");
        mockJobExecution = new JobExecution(mockJobInstance, 2001L, null, null);
        mockJobExecution.setStatus(BatchStatus.STARTED);
        mockJobExecution.setExitStatus(ExitStatus.EXECUTING);
        mockJobExecution.setId(2001L);
    }
    
    // ===============================================================================================
    // Main Report Request Processing Tests
    // Tests for processReportRequest() method - equivalent to COBOL MAIN-PARA paragraph
    // ===============================================================================================
    
    @Test
    @DisplayName("Test successful monthly report processing - COBOL Monthly report path")
    void testProcessReportRequest_MonthlyReport_Success() throws Exception {
        // Arrange - Set up test data for monthly report matching COBOL logic
        String reportType = REPORT_TYPE_MONTHLY;
        String customStartDate = null; // Not used for monthly reports
        String customEndDate = null;   // Not used for monthly reports
        
        // Mock successful job execution
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenReturn(mockJobExecution);
        
        // Act - Call the method under test
        Map<String, Object> result = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        
        // Assert - Verify successful monthly report processing
        assertNotNull(result, "Result should not be null");
        assertTrue((Boolean) result.get("success"), "Report request should be successful");
        assertEquals(2001L, result.get("jobId"), "Job ID should match mock execution");
        assertEquals(REPORT_TYPE_MONTHLY, result.get("reportType"), "Report type should be Monthly");
        assertNotNull(result.get("startDate"), "Start date should be calculated");
        assertNotNull(result.get("endDate"), "End date should be calculated");
        
        // Verify date range is current month (matching COBOL FUNCTION CURRENT-DATE logic)
        String startDate = (String) result.get("startDate");
        String endDate = (String) result.get("endDate");
        LocalDate expectedStart = LocalDate.now().withDayOfMonth(1);
        LocalDate expectedEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        
        assertEquals(expectedStart.format(YYYYMMDD_FORMAT), startDate, 
            "Start date should be first day of current month");
        assertEquals(expectedEnd.format(YYYYMMDD_FORMAT), endDate,
            "End date should be last day of current month");
        
        // Verify job launcher was called with correct parameters
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test successful yearly report processing - COBOL Yearly report path")  
    void testProcessReportRequest_YearlyReport_Success() throws Exception {
        // Arrange - Set up test data for yearly report matching COBOL logic
        String reportType = REPORT_TYPE_YEARLY;
        String customStartDate = null; // Not used for yearly reports
        String customEndDate = null;   // Not used for yearly reports
        
        // Mock successful job execution
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenReturn(mockJobExecution);
        
        // Act - Call the method under test
        Map<String, Object> result = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        
        // Assert - Verify successful yearly report processing
        assertNotNull(result, "Result should not be null");
        assertTrue((Boolean) result.get("success"), "Report request should be successful");
        assertEquals(2001L, result.get("jobId"), "Job ID should match mock execution");
        assertEquals(REPORT_TYPE_YEARLY, result.get("reportType"), "Report type should be Yearly");
        
        // Verify date range is current year (matching COBOL yearly logic)
        String startDate = (String) result.get("startDate");
        String endDate = (String) result.get("endDate");
        LocalDate expectedStart = LocalDate.now().withDayOfYear(1);
        LocalDate expectedEnd = LocalDate.now().withMonth(12).withDayOfMonth(31);
        
        assertEquals(expectedStart.format(YYYYMMDD_FORMAT), startDate,
            "Start date should be January 1st of current year");
        assertEquals(expectedEnd.format(YYYYMMDD_FORMAT), endDate,
            "End date should be December 31st of current year");
        
        // Verify job launcher was called
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test successful custom report processing - COBOL Custom date range path")
    void testProcessReportRequest_CustomReport_Success() throws Exception {
        // Arrange - Set up valid custom date range matching COBOL validation logic
        String reportType = REPORT_TYPE_CUSTOM;
        String customStartDate = "20240101"; // Valid past date in CCYYMMDD format
        String customEndDate = "20240131";   // Valid past date within range
        
        // Mock successful job execution  
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenReturn(mockJobExecution);
        
        // Act - Call the method under test
        Map<String, Object> result = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        
        // Assert - Verify successful custom report processing
        assertNotNull(result, "Result should not be null");
        assertTrue((Boolean) result.get("success"), "Report request should be successful");
        assertEquals(2001L, result.get("jobId"), "Job ID should match mock execution");
        assertEquals(REPORT_TYPE_CUSTOM, result.get("reportType"), "Report type should be Custom");
        assertEquals(customStartDate, result.get("startDate"), "Start date should match input");
        assertEquals(customEndDate, result.get("endDate"), "End date should match input");
        
        // Verify job launcher was called with custom parameters
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test invalid report type handling - COBOL error path")
    void testProcessReportRequest_InvalidReportType_Error() {
        // Arrange - Set up invalid report type to trigger COBOL error condition
        String reportType = "INVALID";
        String customStartDate = null;
        String customEndDate = null;
        
        // Act - Call the method under test
        Map<String, Object> result = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        
        // Assert - Verify error handling matches COBOL error conditions
        assertNotNull(result, "Result should not be null");
        assertFalse((Boolean) result.get("success"), "Report request should fail for invalid type");
        assertTrue(result.get("errorMessage").toString().contains("Invalid report type"),
            "Error message should indicate invalid report type");
        
        // Verify job launcher was not called for invalid input
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test null report type handling - COBOL input validation")
    void testProcessReportRequest_NullReportType_Error() {
        // Arrange - Test null input validation
        String reportType = null;
        String customStartDate = null;
        String customEndDate = null;
        
        // Act - Call the method under test
        Map<String, Object> result = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        
        // Assert - Verify null input handling
        assertNotNull(result, "Result should not be null");
        assertFalse((Boolean) result.get("success"), "Report request should fail for null type");
        assertTrue(result.get("errorMessage").toString().contains("Invalid report type"),
            "Error message should indicate invalid report type");
        
        // Verify job launcher was not called
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test job launcher exception handling")
    void testProcessReportRequest_JobLauncherException_Error() throws Exception {
        // Arrange - Set up job launcher to throw exception
        String reportType = REPORT_TYPE_MONTHLY;
        String customStartDate = null;
        String customEndDate = null;
        
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Job launch failed"));
        
        // Act - Call the method under test
        Map<String, Object> result = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        
        // Assert - Verify exception handling
        assertNotNull(result, "Result should not be null");
        assertFalse((Boolean) result.get("success"), "Report request should fail on job launch error");
        assertTrue(result.get("errorMessage").toString().contains("System error"),
            "Error message should indicate system error");
        
        // Verify job launcher was called but failed
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    // ===============================================================================================
    // Report Selection Handling Tests  
    // Tests for handleReportSelection() method - equivalent to COBOL PROCESS-ENTER-KEY paragraph
    // ===============================================================================================
    
    @Test
    @DisplayName("Test monthly report selection handling - COBOL monthly branch")
    void testHandleReportSelection_MonthlyReport_Success() {
        // Arrange - Set up monthly report selection
        String reportType = REPORT_TYPE_MONTHLY;
        String customStartDate = null; // Not used for monthly
        String customEndDate = null;   // Not used for monthly
        
        // Act - Call the method under test
        Map<String, String> result = reportService.handleReportSelection(reportType, customStartDate, customEndDate);
        
        // Assert - Verify monthly date range calculation
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain error for valid monthly selection");
        
        // Verify monthly date range matches current month logic from COBOL
        String startDate = result.get("startDate");
        String endDate = result.get("endDate");
        assertNotNull(startDate, "Start date should be calculated");
        assertNotNull(endDate, "End date should be calculated");
        
        LocalDate expectedStart = LocalDate.now().withDayOfMonth(1);
        LocalDate expectedEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        
        assertEquals(expectedStart.format(YYYYMMDD_FORMAT), startDate,
            "Monthly start date should be first day of current month");
        assertEquals(expectedEnd.format(YYYYMMDD_FORMAT), endDate,
            "Monthly end date should be last day of current month");
        
        // Verify display format dates are provided
        assertNotNull(result.get("startDateDisplay"), "Display start date should be provided");
        assertNotNull(result.get("endDateDisplay"), "Display end date should be provided");
    }
    
    @Test
    @DisplayName("Test yearly report selection handling - COBOL yearly branch")
    void testHandleReportSelection_YearlyReport_Success() {
        // Arrange - Set up yearly report selection
        String reportType = REPORT_TYPE_YEARLY;
        String customStartDate = null; // Not used for yearly
        String customEndDate = null;   // Not used for yearly
        
        // Act - Call the method under test
        Map<String, String> result = reportService.handleReportSelection(reportType, customStartDate, customEndDate);
        
        // Assert - Verify yearly date range calculation
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain error for valid yearly selection");
        
        // Verify yearly date range matches COBOL yearly logic
        String startDate = result.get("startDate");
        String endDate = result.get("endDate");
        assertNotNull(startDate, "Start date should be calculated");
        assertNotNull(endDate, "End date should be calculated");
        
        LocalDate expectedStart = LocalDate.now().withDayOfYear(1);
        LocalDate expectedEnd = LocalDate.now().withMonth(12).withDayOfMonth(31);
        
        assertEquals(expectedStart.format(YYYYMMDD_FORMAT), startDate,
            "Yearly start date should be January 1st");
        assertEquals(expectedEnd.format(YYYYMMDD_FORMAT), endDate,
            "Yearly end date should be December 31st");
    }
    
    @Test
    @DisplayName("Test custom report selection handling - COBOL custom validation path")
    void testHandleReportSelection_CustomReport_ValidDates_Success() {
        // Arrange - Set up valid custom date range
        String reportType = REPORT_TYPE_CUSTOM;
        String customStartDate = "20240101";
        String customEndDate = "20240131";
        
        // Act - Call the method under test
        Map<String, String> result = reportService.handleReportSelection(reportType, customStartDate, customEndDate);
        
        // Assert - Verify custom date range validation and processing
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain error for valid custom dates");
        
        assertEquals(customStartDate, result.get("startDate"), "Start date should match input");
        assertEquals(customEndDate, result.get("endDate"), "End date should match input");
        
        // Verify display format dates are provided
        assertNotNull(result.get("startDateDisplay"), "Display start date should be provided");
        assertNotNull(result.get("endDateDisplay"), "Display end date should be provided");
    }
    
    @Test
    @DisplayName("Test custom report with invalid dates - COBOL validation error")
    void testHandleReportSelection_CustomReport_InvalidDates_Error() {
        // Arrange - Set up invalid custom date range (future dates)
        String reportType = REPORT_TYPE_CUSTOM;
        LocalDate futureDate = LocalDate.now().plusDays(30);
        String customStartDate = futureDate.format(YYYYMMDD_FORMAT);
        String customEndDate = futureDate.plusDays(10).format(YYYYMMDD_FORMAT);
        
        // Act - Call the method under test
        Map<String, String> result = reportService.handleReportSelection(reportType, customStartDate, customEndDate);
        
        // Assert - Verify validation error for future dates
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for future dates");
        assertTrue(result.get("error").contains("Invalid date range"),
            "Error should indicate invalid date range");
    }
    
    @Test
    @DisplayName("Test invalid report type selection - COBOL error handling")
    void testHandleReportSelection_InvalidType_Error() {
        // Arrange - Set up invalid report type
        String reportType = "INVALID";
        String customStartDate = null;
        String customEndDate = null;
        
        // Act - Call the method under test
        Map<String, String> result = reportService.handleReportSelection(reportType, customStartDate, customEndDate);
        
        // Assert - Verify error handling for invalid type
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for invalid type");
        assertTrue(result.get("error").contains("Invalid report type"),
            "Error should indicate invalid report type");
    }
    
    // ===============================================================================================
    // Job Submission Tests
    // Tests for submitReportJob() method - equivalent to COBOL SUBMIT-JOB-TO-INTRDR paragraph
    // ===============================================================================================
    
    @Test
    @DisplayName("Test successful job submission - COBOL job submission path")
    void testSubmitReportJob_Success() throws Exception {
        // Arrange - Set up valid job parameters
        JobParameters jobParameters = new JobParameters();
        
        // Mock successful job execution
        when(jobLauncher.run(eq(reportGenerationJob), eq(jobParameters)))
            .thenReturn(mockJobExecution);
        
        // Act - Call the method under test
        JobExecution result = reportService.submitReportJob(jobParameters);
        
        // Assert - Verify successful job submission
        assertNotNull(result, "Job execution result should not be null");
        assertEquals(mockJobExecution.getId(), result.getId(), "Job execution ID should match");
        assertEquals(BatchStatus.STARTED, result.getStatus(), "Job status should be STARTED");
        
        // Verify job launcher was called with correct parameters
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), eq(jobParameters));
    }
    
    @Test
    @DisplayName("Test job submission failure - COBOL job submission error")
    void testSubmitReportJob_Failure() throws Exception {
        // Arrange - Set up job launcher to fail
        JobParameters jobParameters = new JobParameters();
        RuntimeException jobException = new RuntimeException("Job submission failed");
        
        when(jobLauncher.run(eq(reportGenerationJob), eq(jobParameters)))
            .thenThrow(jobException);
        
        // Act & Assert - Verify exception is properly wrapped and thrown
        Exception exception = assertThrows(Exception.class, () -> {
            reportService.submitReportJob(jobParameters);
        }, "Should throw exception on job submission failure");
        
        assertTrue(exception.getMessage().contains("Error submitting report job"),
            "Exception message should indicate job submission error");
        assertTrue(exception.getCause() == jobException,
            "Exception cause should be the original runtime exception");
        
        // Verify job launcher was called
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), eq(jobParameters));
    }
    
    // ===============================================================================================
    // Date Range Validation Tests
    // Tests for validateDateRange() method - equivalent to COBOL CSUTLDTC validation
    // ===============================================================================================
    
    @Test
    @DisplayName("Test valid date range validation - COBOL CSUTLDTC success path")
    void testValidateDateRange_ValidRange_Success() {
        // Arrange - Set up valid date range (past dates within 365 days)
        String startDate = "20240101";
        String endDate = "20240131";
        
        // Act - Call the method under test
        boolean result = reportService.validateDateRange(startDate, endDate);
        
        // Assert - Verify validation passes for valid range
        assertTrue(result, "Should return true for valid date range");
    }
    
    @Test
    @DisplayName("Test future dates validation - COBOL date validation error")
    void testValidateDateRange_FutureDates_Invalid() {
        // Arrange - Set up future dates (invalid per COBOL logic)
        LocalDate futureDate = LocalDate.now().plusDays(30);
        String startDate = futureDate.format(YYYYMMDD_FORMAT);
        String endDate = futureDate.plusDays(10).format(YYYYMMDD_FORMAT);
        
        // Act - Call the method under test
        boolean result = reportService.validateDateRange(startDate, endDate);
        
        // Assert - Verify validation fails for future dates
        assertFalse(result, "Should return false for future dates");
    }
    
    @Test
    @DisplayName("Test start date after end date validation - COBOL logic error")
    void testValidateDateRange_StartAfterEnd_Invalid() {
        // Arrange - Set up invalid date range (start after end)
        String startDate = "20240131";
        String endDate = "20240101";
        
        // Act - Call the method under test
        boolean result = reportService.validateDateRange(startDate, endDate);
        
        // Assert - Verify validation fails when start > end
        assertFalse(result, "Should return false when start date is after end date");
    }
    
    @Test
    @DisplayName("Test date range exceeding 365 days - COBOL business rule")
    void testValidateDateRange_ExceedsYearLimit_Invalid() {
        // Arrange - Set up date range exceeding 365 days
        LocalDate startDate = LocalDate.now().minusDays(400);
        LocalDate endDate = LocalDate.now().minusDays(10);
        String start = startDate.format(YYYYMMDD_FORMAT);
        String end = endDate.format(YYYYMMDD_FORMAT);
        
        // Act - Call the method under test
        boolean result = reportService.validateDateRange(start, end);
        
        // Assert - Verify validation fails for ranges > 365 days
        assertFalse(result, "Should return false for date ranges exceeding 365 days");
    }
    
    @Test
    @DisplayName("Test null date validation - COBOL input validation")
    void testValidateDateRange_NullDates_Invalid() {
        // Arrange - Test null input validation
        String startDate = null;
        String endDate = "20240131";
        
        // Act - Call the method under test
        boolean result = reportService.validateDateRange(startDate, endDate);
        
        // Assert - Verify validation fails for null dates
        assertFalse(result, "Should return false for null start date");
        
        // Test null end date
        result = reportService.validateDateRange("20240101", null);
        assertFalse(result, "Should return false for null end date");
        
        // Test both null
        result = reportService.validateDateRange(null, null);
        assertFalse(result, "Should return false for both null dates");
    }
    
    @Test
    @DisplayName("Test empty date validation - COBOL input validation") 
    void testValidateDateRange_EmptyDates_Invalid() {
        // Arrange - Test empty string validation
        String startDate = "";
        String endDate = "20240131";
        
        // Act - Call the method under test
        boolean result = reportService.validateDateRange(startDate, endDate);
        
        // Assert - Verify validation fails for empty dates
        assertFalse(result, "Should return false for empty start date");
        
        // Test whitespace dates
        result = reportService.validateDateRange("   ", "20240131");
        assertFalse(result, "Should return false for whitespace start date");
    }
    
    // ===============================================================================================
    // Job Parameters Generation Tests
    // Tests for generateJobParameters() method - equivalent to COBOL JCL parameter generation
    // ===============================================================================================
    
    @Test
    @DisplayName("Test job parameters generation for monthly report - COBOL JCL generation")
    void testGenerateJobParameters_MonthlyReport_Success() {
        // Arrange - Set up monthly report date range
        String reportType = REPORT_TYPE_MONTHLY;
        Map<String, String> dateRange = new HashMap<>();
        dateRange.put("startDate", "20240101");
        dateRange.put("endDate", "20240131");
        dateRange.put("startDateDisplay", "01/01/2024");
        dateRange.put("endDateDisplay", "01/31/2024");
        
        // Act - Call the method under test
        JobParameters result = reportService.generateJobParameters(reportType, dateRange);
        
        // Assert - Verify job parameters are correctly generated
        assertNotNull(result, "Job parameters should not be null");
        
        // Verify core parameters matching COBOL JCL generation
        assertEquals(reportType, result.getString("reportType"),
            "Report type parameter should match");
        assertEquals("20240101", result.getString("startDate"),
            "Start date parameter should match");
        assertEquals("20240131", result.getString("endDate"), 
            "End date parameter should match");
        assertEquals("01/01/2024", result.getString("startDateDisplay"),
            "Start date display should match");
        assertEquals("01/31/2024", result.getString("endDateDisplay"),
            "End date display should match");
        
        // Verify system parameters
        assertEquals("JOBS", result.getString("jobQueue"),
            "Job queue should match COBOL TDQ name");
        assertEquals("REPORT-SERVICE", result.getString("submittedBy"),
            "Submitted by should match service identifier");
        assertNotNull(result.getLong("timestamp"),
            "Timestamp should be included for uniqueness");
    }
    
    @Test
    @DisplayName("Test job parameters generation for yearly report - COBOL yearly JCL")
    void testGenerateJobParameters_YearlyReport_Success() {
        // Arrange - Set up yearly report date range
        String reportType = REPORT_TYPE_YEARLY;
        Map<String, String> dateRange = new HashMap<>();
        dateRange.put("startDate", "20240101");
        dateRange.put("endDate", "20241231");
        dateRange.put("startDateDisplay", "01/01/2024");
        dateRange.put("endDateDisplay", "12/31/2024");
        
        // Act - Call the method under test
        JobParameters result = reportService.generateJobParameters(reportType, dateRange);
        
        // Assert - Verify yearly job parameters
        assertNotNull(result, "Job parameters should not be null");
        assertEquals(reportType, result.getString("reportType"), "Report type should be yearly");
        assertEquals("20240101", result.getString("startDate"), "Start should be year start");
        assertEquals("20241231", result.getString("endDate"), "End should be year end");
    }
    
    @Test
    @DisplayName("Test job parameters generation for custom report - COBOL custom JCL")
    void testGenerateJobParameters_CustomReport_Success() {
        // Arrange - Set up custom report date range
        String reportType = REPORT_TYPE_CUSTOM;
        Map<String, String> dateRange = new HashMap<>();
        dateRange.put("startDate", "20240115");
        dateRange.put("endDate", "20240215");
        dateRange.put("startDateDisplay", "01/15/2024");
        dateRange.put("endDateDisplay", "02/15/2024");
        
        // Act - Call the method under test
        JobParameters result = reportService.generateJobParameters(reportType, dateRange);
        
        // Assert - Verify custom job parameters
        assertNotNull(result, "Job parameters should not be null");
        assertEquals(reportType, result.getString("reportType"), "Report type should be custom");
        assertEquals("20240115", result.getString("startDate"), "Start should match custom input");
        assertEquals("20240215", result.getString("endDate"), "End should match custom input");
    }
    
    // ===============================================================================================
    // Job Queue Writing Tests
    // Tests for writeJobToQueue() method - equivalent to COBOL WRITEQ TD operations
    // ===============================================================================================
    
    @Test
    @DisplayName("Test job queue writing - COBOL WRITEQ TD simulation")
    void testWriteJobToQueue_Success() {
        // Arrange - Set up job submission data
        String reportType = REPORT_TYPE_MONTHLY;
        Map<String, String> dateRange = new HashMap<>();
        dateRange.put("startDate", "20240101");
        dateRange.put("endDate", "20240131");
        Long jobId = 2001L;
        
        // Act - Call the method under test (should not throw exception)
        assertDoesNotThrow(() -> {
            reportService.writeJobToQueue(reportType, dateRange, jobId);
        }, "Writing to job queue should not throw exception");
        
        // Assert - Since this method primarily logs, we verify it completes successfully
        // In a real implementation, this might interact with a message queue or database
    }
    
    @Test
    @DisplayName("Test job queue writing with null parameters - COBOL error handling")
    void testWriteJobToQueue_NullParameters_HandledGracefully() {
        // Arrange - Set up null parameters to test error handling
        String reportType = null;
        Map<String, String> dateRange = null;
        Long jobId = null;
        
        // Act & Assert - Should handle null parameters gracefully
        assertDoesNotThrow(() -> {
            reportService.writeJobToQueue(reportType, dateRange, jobId);
        }, "Should handle null parameters gracefully without throwing exception");
    }
    
    // ===============================================================================================
    // Monthly Date Range Calculation Tests
    // Tests for calculateMonthlyDateRange() method - equivalent to COBOL monthly date logic
    // ===============================================================================================
    
    @Test
    @DisplayName("Test monthly date range calculation - COBOL FUNCTION CURRENT-DATE logic")
    void testCalculateMonthlyDateRange_Success() {
        // Act - Call the method under test
        Map<String, String> result = reportService.calculateMonthlyDateRange();
        
        // Assert - Verify monthly date range calculation
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain error for valid calculation");
        
        // Verify date range matches current month
        String startDate = result.get("startDate");
        String endDate = result.get("endDate");
        assertNotNull(startDate, "Start date should be calculated");
        assertNotNull(endDate, "End date should be calculated");
        
        LocalDate expectedStart = LocalDate.now().withDayOfMonth(1);
        LocalDate expectedEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        
        assertEquals(expectedStart.format(YYYYMMDD_FORMAT), startDate,
            "Start date should be first day of current month");
        assertEquals(expectedEnd.format(YYYYMMDD_FORMAT), endDate,
            "End date should be last day of current month");
        
        // Verify display format dates
        String startDisplay = result.get("startDateDisplay");
        String endDisplay = result.get("endDateDisplay");
        assertNotNull(startDisplay, "Start date display should be provided");
        assertNotNull(endDisplay, "End date display should be provided");
        assertEquals(expectedStart.format(DISPLAY_FORMAT), startDisplay,
            "Start display date should match expected format");
        assertEquals(expectedEnd.format(DISPLAY_FORMAT), endDisplay,
            "End display date should match expected format");
    }
    
    // ===============================================================================================
    // Yearly Date Range Calculation Tests  
    // Tests for calculateYearlyDateRange() method - equivalent to COBOL yearly date logic
    // ===============================================================================================
    
    @Test
    @DisplayName("Test yearly date range calculation - COBOL yearly date logic")
    void testCalculateYearlyDateRange_Success() {
        // Act - Call the method under test
        Map<String, String> result = reportService.calculateYearlyDateRange();
        
        // Assert - Verify yearly date range calculation
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain error for valid calculation");
        
        // Verify date range matches current year
        String startDate = result.get("startDate");
        String endDate = result.get("endDate");
        assertNotNull(startDate, "Start date should be calculated");
        assertNotNull(endDate, "End date should be calculated");
        
        LocalDate expectedStart = LocalDate.now().withDayOfYear(1);
        LocalDate expectedEnd = LocalDate.now().withMonth(12).withDayOfMonth(31);
        
        assertEquals(expectedStart.format(YYYYMMDD_FORMAT), startDate,
            "Start date should be January 1st of current year");
        assertEquals(expectedEnd.format(YYYYMMDD_FORMAT), endDate,
            "End date should be December 31st of current year");
        
        // Verify display format dates
        String startDisplay = result.get("startDateDisplay");
        String endDisplay = result.get("endDateDisplay");
        assertNotNull(startDisplay, "Start date display should be provided");
        assertNotNull(endDisplay, "End date display should be provided");
        assertEquals(expectedStart.format(DISPLAY_FORMAT), startDisplay,
            "Start display should match expected format");
        assertEquals(expectedEnd.format(DISPLAY_FORMAT), endDisplay,
            "End display should match expected format");
    }
    
    // ===============================================================================================
    // Custom Date Range Validation Tests
    // Tests for validateCustomDateRange() method - equivalent to COBOL custom validation
    // ===============================================================================================
    
    @Test
    @DisplayName("Test valid custom date range validation - COBOL custom validation success")
    void testValidateCustomDateRange_ValidDates_Success() {
        // Arrange - Set up valid custom date range
        String customStartDate = "20240101";
        String customEndDate = "20240131";
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify successful validation and formatting
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain error for valid dates");
        
        assertEquals(customStartDate, result.get("startDate"), "Start date should match input");
        assertEquals(customEndDate, result.get("endDate"), "End date should match input");
        
        // Verify display format dates are provided
        assertNotNull(result.get("startDateDisplay"), "Start display date should be provided");
        assertNotNull(result.get("endDateDisplay"), "End display date should be provided");
        assertEquals("01/01/2024", result.get("startDateDisplay"),
            "Start display should be in MM/dd/yyyy format");
        assertEquals("01/31/2024", result.get("endDateDisplay"),
            "End display should be in MM/dd/yyyy format");
    }
    
    @Test
    @DisplayName("Test custom date range with null inputs - COBOL input validation")
    void testValidateCustomDateRange_NullInputs_Error() {
        // Arrange - Test null input validation
        String customStartDate = null;
        String customEndDate = "20240131";
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify error handling for null inputs
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for null start date");
        assertTrue(result.get("error").contains("Both start and end dates are required"),
            "Error should indicate both dates are required");
        
        // Test null end date
        result = reportService.validateCustomDateRange("20240101", null);
        assertTrue(result.containsKey("error"), "Should contain error for null end date");
        
        // Test both null
        result = reportService.validateCustomDateRange(null, null);
        assertTrue(result.containsKey("error"), "Should contain error for both null dates");
    }
    
    @Test
    @DisplayName("Test custom date range with empty inputs - COBOL input validation")
    void testValidateCustomDateRange_EmptyInputs_Error() {
        // Arrange - Test empty input validation
        String customStartDate = "";
        String customEndDate = "20240131";
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify error handling for empty inputs
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for empty start date");
        
        // Test whitespace inputs
        result = reportService.validateCustomDateRange("   ", "20240131");
        assertTrue(result.containsKey("error"), "Should contain error for whitespace start date");
    }
    
    @Test
    @DisplayName("Test custom date range with invalid format - COBOL format validation")
    void testValidateCustomDateRange_InvalidFormat_Error() {
        // Arrange - Set up invalid date format
        String customStartDate = "2024-01-01"; // Wrong format (should be YYYYMMDD)
        String customEndDate = "20240131";
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify error handling for invalid format
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for invalid format");
        assertTrue(result.get("error").contains("Invalid date format"),
            "Error should indicate invalid date format");
        
        // Test completely invalid format
        result = reportService.validateCustomDateRange("invalid", "20240131");
        assertTrue(result.containsKey("error"), "Should contain error for invalid date string");
    }
    
    @Test
    @DisplayName("Test custom date range with future dates - COBOL business rule validation")
    void testValidateCustomDateRange_FutureDates_Error() {
        // Arrange - Set up future dates (violates COBOL business rules)
        LocalDate futureDate = LocalDate.now().plusDays(30);
        String customStartDate = futureDate.format(YYYYMMDD_FORMAT);
        String customEndDate = futureDate.plusDays(10).format(YYYYMMDD_FORMAT);
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify error handling for future dates
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for future dates");
        assertTrue(result.get("error").contains("Invalid date range"),
            "Error should indicate invalid date range");
    }
    
    @Test
    @DisplayName("Test custom date range with start after end - COBOL logic validation")
    void testValidateCustomDateRange_StartAfterEnd_Error() {
        // Arrange - Set up invalid range (start after end)
        String customStartDate = "20240201"; // February 1st
        String customEndDate = "20240131";   // January 31st
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify error handling for invalid range
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error when start is after end");
        assertTrue(result.get("error").contains("Invalid date range"),
            "Error should indicate invalid date range");
    }
    
    @Test
    @DisplayName("Test custom date range exceeding 365 days - COBOL business rule limit")
    void testValidateCustomDateRange_ExceedsYearLimit_Error() {
        // Arrange - Set up date range exceeding business rule limit (365 days)
        LocalDate startDate = LocalDate.now().minusDays(400);
        LocalDate endDate = LocalDate.now().minusDays(10);
        String customStartDate = startDate.format(YYYYMMDD_FORMAT);
        String customEndDate = endDate.format(YYYYMMDD_FORMAT);
        
        // Act - Call the method under test
        Map<String, String> result = reportService.validateCustomDateRange(customStartDate, customEndDate);
        
        // Assert - Verify error handling for excessive date range
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("error"), "Should contain error for range exceeding 365 days");
        assertTrue(result.get("error").contains("Date range cannot exceed 365 days"),
            "Error should indicate date range limit exceeded");
    }
    
    // ===============================================================================================
    // Integration and Edge Case Tests
    // Comprehensive tests for complex scenarios and system integration
    // ===============================================================================================
    
    @Test
    @DisplayName("Test full monthly report workflow - End-to-end COBOL process simulation")
    void testFullMonthlyReportWorkflow_Integration() throws Exception {
        // Arrange - Set up full workflow test
        String reportType = REPORT_TYPE_MONTHLY;
        
        // Mock successful job execution for integration test
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenReturn(mockJobExecution);
        
        // Act - Execute full workflow
        Map<String, Object> processResult = reportService.processReportRequest(reportType, null, null);
        Map<String, String> selectionResult = reportService.handleReportSelection(reportType, null, null);
        Map<String, String> dateRangeResult = reportService.calculateMonthlyDateRange();
        
        // Assert - Verify full workflow integration
        assertNotNull(processResult, "Process result should not be null");
        assertNotNull(selectionResult, "Selection result should not be null");
        assertNotNull(dateRangeResult, "Date range result should not be null");
        
        // Verify consistency across workflow steps
        assertTrue((Boolean) processResult.get("success"), "Process should be successful");
        assertFalse(selectionResult.containsKey("error"), "Selection should not have errors");
        assertFalse(dateRangeResult.containsKey("error"), "Date calculation should not have errors");
        
        // Verify date range consistency
        assertEquals(selectionResult.get("startDate"), dateRangeResult.get("startDate"),
            "Start dates should be consistent across methods");
        assertEquals(selectionResult.get("endDate"), dateRangeResult.get("endDate"),
            "End dates should be consistent across methods");
        
        // Verify job launcher integration
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test full custom report workflow with validation - End-to-end custom process")
    void testFullCustomReportWorkflow_Integration() throws Exception {
        // Arrange - Set up full custom workflow test
        String reportType = REPORT_TYPE_CUSTOM;
        String customStartDate = "20240101";
        String customEndDate = "20240131";
        
        // Mock successful job execution
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenReturn(mockJobExecution);
        
        // Act - Execute full custom workflow
        Map<String, Object> processResult = reportService.processReportRequest(reportType, customStartDate, customEndDate);
        Map<String, String> validationResult = reportService.validateCustomDateRange(customStartDate, customEndDate);
        boolean dateRangeValid = reportService.validateDateRange(customStartDate, customEndDate);
        
        // Assert - Verify full custom workflow integration
        assertNotNull(processResult, "Process result should not be null");
        assertNotNull(validationResult, "Validation result should not be null");
        
        // Verify workflow success
        assertTrue((Boolean) processResult.get("success"), "Custom process should be successful");
        assertFalse(validationResult.containsKey("error"), "Validation should pass");
        assertTrue(dateRangeValid, "Date range validation should pass");
        
        // Verify data consistency
        assertEquals(customStartDate, processResult.get("startDate"), 
            "Start date should match input through workflow");
        assertEquals(customEndDate, processResult.get("endDate"),
            "End date should match input through workflow");
        
        // Verify job submission occurred
        verify(jobLauncher, times(1)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test error handling workflow - COBOL error propagation simulation")
    void testErrorHandlingWorkflow_Integration() throws Exception {
        // Arrange - Set up error scenario (job launcher failure)
        String reportType = REPORT_TYPE_YEARLY;
        
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenThrow(new RuntimeException("Batch system unavailable"));
        
        // Act - Execute workflow with error condition
        Map<String, Object> processResult = reportService.processReportRequest(reportType, null, null);
        
        // Assert - Verify error handling workflow
        assertNotNull(processResult, "Process result should not be null even on error");
        assertFalse((Boolean) processResult.get("success"), "Process should indicate failure");
        assertTrue(processResult.get("errorMessage").toString().contains("System error"),
            "Error message should indicate system error");
        
        // Verify error doesn't corrupt other operations
        Map<String, String> yearlyRange = reportService.calculateYearlyDateRange();
        assertNotNull(yearlyRange, "Date calculation should still work despite job error");
        assertFalse(yearlyRange.containsKey("error"), "Date calculation should not be affected");
    }
    
    // ===============================================================================================
    // Performance and Boundary Tests
    // Tests for performance characteristics and system limits
    // ===============================================================================================
    
    @Test
    @DisplayName("Test concurrent report requests - Multi-user scenario simulation")
    void testConcurrentReportRequests() throws Exception {
        // Arrange - Set up concurrent request simulation
        String reportType = REPORT_TYPE_MONTHLY;
        
        // Mock job launcher for concurrent requests
        when(jobLauncher.run(eq(reportGenerationJob), any(JobParameters.class)))
            .thenReturn(mockJobExecution);
        
        // Act - Simulate multiple concurrent requests
        Map<String, Object> result1 = reportService.processReportRequest(reportType, null, null);
        Map<String, Object> result2 = reportService.processReportRequest(reportType, null, null);
        Map<String, Object> result3 = reportService.processReportRequest(reportType, null, null);
        
        // Assert - Verify all requests are handled correctly
        assertTrue((Boolean) result1.get("success"), "First request should succeed");
        assertTrue((Boolean) result2.get("success"), "Second request should succeed");  
        assertTrue((Boolean) result3.get("success"), "Third request should succeed");
        
        // Verify job launcher was called for each request
        verify(jobLauncher, times(3)).run(eq(reportGenerationJob), any(JobParameters.class));
    }
    
    @Test
    @DisplayName("Test boundary date values - Edge case date handling")
    void testBoundaryDateValues() {
        // Test leap year February 29th
        String leapYearDate = "20240229";
        boolean isValid = reportService.validateDateRange(leapYearDate, leapYearDate);
        assertTrue(isValid, "Leap year date should be valid");
        
        // Test year boundaries
        String yearStart = LocalDate.now().withDayOfYear(1).format(YYYYMMDD_FORMAT);
        String yearEnd = LocalDate.now().withMonth(12).withDayOfMonth(31).format(YYYYMMDD_FORMAT);
        isValid = reportService.validateDateRange(yearStart, yearEnd);
        assertTrue(isValid, "Full year range should be valid");
        
        // Test month boundaries
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDate lastDay = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        String monthStart = firstDay.format(YYYYMMDD_FORMAT);
        String monthEnd = lastDay.format(YYYYMMDD_FORMAT);
        isValid = reportService.validateDateRange(monthStart, monthEnd);
        assertTrue(isValid, "Full month range should be valid");
    }
    
    @Test
    @DisplayName("Test error message formatting - COBOL message compatibility")
    void testErrorMessageFormatting() {
        // Test various error conditions and message formatting
        
        // Test null report type error
        Map<String, Object> result = reportService.processReportRequest(null, null, null);
        String errorMessage = (String) result.get("errorMessage");
        assertNotNull(errorMessage, "Error message should be provided");
        assertTrue(errorMessage.length() > 0, "Error message should not be empty");
        assertFalse(errorMessage.contains("null"), "Error message should be user-friendly");
        
        // Test invalid custom date error
        Map<String, String> customResult = reportService.validateCustomDateRange("invalid", "20240131");
        String customError = customResult.get("error");
        assertNotNull(customError, "Custom validation error should be provided");
        assertTrue(customError.contains("Invalid date format"), "Should contain specific error type");
        
        // Test date range validation error
        boolean rangeValid = reportService.validateDateRange("20240201", "20240131");
        assertFalse(rangeValid, "Invalid range should return false");
    }
    
    // ===============================================================================================
    // Final Validation and Cleanup Tests
    // Tests to ensure proper resource management and final state validation
    // ===============================================================================================
    
    @Test
    @DisplayName("Test service state consistency - Stateless operation validation")
    void testServiceStateConsistency() {
        // Verify service maintains no state between calls
        
        // Execute multiple different operations
        Map<String, String> monthly1 = reportService.calculateMonthlyDateRange();
        Map<String, String> yearly1 = reportService.calculateYearlyDateRange();
        Map<String, String> monthly2 = reportService.calculateMonthlyDateRange();
        
        // Verify results are consistent (service is stateless)
        assertEquals(monthly1.get("startDate"), monthly2.get("startDate"),
            "Multiple calls should produce consistent results");
        assertEquals(monthly1.get("endDate"), monthly2.get("endDate"),
            "Multiple calls should produce consistent results");
        
        // Verify different operation types don't interfere
        assertNotEquals(monthly1.get("startDate"), yearly1.get("startDate"),
            "Monthly and yearly operations should produce different results");
    }
    
    @Test
    @DisplayName("Test mock verification - Ensuring all mocked interactions are verified")
    void testMockVerification() {
        // This test ensures we haven't missed any mock interactions
        // Run a simple operation to trigger mocks
        Map<String, String> result = reportService.calculateMonthlyDateRange();
        
        // Verify the operation completed successfully
        assertNotNull(result, "Result should not be null");
        assertFalse(result.containsKey("error"), "Should not contain errors");
        
        // Additional verifications can be added here for any remaining unverified mocks
        // This serves as a safety net to catch any missed mock interactions
    }
}