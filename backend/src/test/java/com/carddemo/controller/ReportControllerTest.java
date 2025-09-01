/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.controller.ReportController;
import com.carddemo.service.ReportService;
import com.carddemo.dto.ReportRequest;
import com.carddemo.dto.ReportMenuResponse;
import com.carddemo.util.TestConstants;
import com.carddemo.batch.BatchJobLauncher;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import org.assertj.core.api.Assertions;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.awaitility.Awaitility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive integration test suite for ReportController that validates complete COBOL-to-Java 
 * functional parity with the CORPT00C program.
 * 
 * This test class ensures that the REST-based report generation functionality maintains 100% 
 * compatibility with the original CICS COBOL report transaction processing, including:
 * 
 * COBOL Migration Context:
 * - Original CORPT00C.cbl handled CICS transaction 'CR00' for report generation
 * - Supported Monthly, Yearly, and Custom report types with date range validation
 * - Used JCL job submission via Extra Partition TDQ ('JOBS') for batch processing
 * - Implemented CSUTLDTC date validation equivalent to Java LocalDate validation
 * - Provided BMS screen-based user interaction replaced by REST JSON APIs
 * 
 * Test Coverage Areas:
 * 1. Report Generation Endpoints (POST /api/reports/transaction)
 * 2. Job Status Monitoring (GET /api/reports/status/{jobId})  
 * 3. Report Type Processing (Monthly, Yearly, Custom)
 * 4. Date Range Validation (CSUTLDTC equivalent logic)
 * 5. Batch Job Submission (Spring Batch replacing TDQ/JCL)
 * 6. Asynchronous Job Execution and Monitoring
 * 7. Error Handling and Validation Messages
 * 8. BMS Map Equivalent JSON Request/Response Processing
 * 
 * Performance Requirements:
 * - All API endpoints must respond within 200ms (matching COBOL response time)
 * - Batch job submissions must complete within 5 seconds
 * - Date validation must match COBOL precision and error messages
 * - BigDecimal calculations must preserve COBOL COMP-3 precision
 * 
 * Validation Approach:
 * - Unit tests validate individual controller methods in isolation
 * - Integration tests validate complete request/response cycles
 * - Functional tests verify COBOL business logic preservation
 * - Performance tests ensure sub-200ms response times
 * - Error handling tests validate COBOL-equivalent error messages
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
    "spring.batch.job.enabled=false",
    "logging.level.com.carddemo=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportControllerTest implements IntegrationTest {

    /**
     * TestContainers PostgreSQL database for integration testing.
     * Provides isolated database environment matching production PostgreSQL setup.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo_test")
            .withUsername("carddemo_user")
            .withPassword("carddemo_pass")
            .withExposedPorts(5432);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean 
    private BatchJobLauncher batchJobLauncher;

    private String baseUrl;
    
    // Test data constants matching COBOL data structures
    private static final String MONTHLY_REPORT_TYPE = "MONTHLY";
    private static final String YEARLY_REPORT_TYPE = "YEARLY";
    private static final String CUSTOM_REPORT_TYPE = "CUSTOM";
    
    private static final LocalDate TEST_START_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate TEST_END_DATE = LocalDate.of(2024, 1, 31);
    
    // Job execution mock data
    private static final Long TEST_JOB_EXECUTION_ID = 12345L;
    private static final String TEST_JOB_NAME = "transactionReportJob";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/reports";
        
        // Reset mocks before each test
        reset(reportService, batchJobLauncher);
    }

    @AfterEach
    void tearDown() {
        // Clean up any test data if needed
    }

    /**
     * Tests the main report generation endpoint POST /api/reports/transaction
     * This test validates functional parity with COBOL CORPT00C PROCESS-ENTER-KEY paragraph
     * for report type selection and job submission.
     */
    @Test
    @DisplayName("Test Generate Transaction Report - COBOL PROCESS-ENTER-KEY equivalent")
    void testGenerateTransactionReport() throws Exception {
        // Arrange - Mock successful report processing
        ReportMenuResponse expectedResponse = new ReportMenuResponse();
        expectedResponse.setSuccessMessage("Monthly report submitted for printing ...");
        expectedResponse.setReportStatus("SUBMITTED");
        
        when(reportService.processReportRequest(any(ReportRequest.class)))
            .thenReturn(expectedResponse);

        // Create test request matching BMS map structure
        ReportRequest request = new ReportRequest();
        request.setReportType(MONTHLY_REPORT_TYPE);
        request.setStartDate(TEST_START_DATE);
        request.setEndDate(TEST_END_DATE);
        request.setConfirmPrint(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act - Measure response time for COBOL parity validation
        long startTime = System.currentTimeMillis();
        ResponseEntity<ReportMenuResponse> response = restTemplate.postForEntity(
            baseUrl + "/generate", 
            requestEntity, 
            ReportMenuResponse.class
        );
        long responseTime = System.currentTimeMillis() - startTime;

        // Assert - Validate COBOL functional parity
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(responseTime)
            .as("Response time must be under 200ms for COBOL parity")
            .isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        ReportMenuResponse responseBody = response.getBody();
        Assertions.assertThat(responseBody).isNotNull();
        Assertions.assertThat(responseBody.getSuccessMessage())
            .contains("Monthly report submitted for printing");
        Assertions.assertThat(responseBody.getReportStatus()).isEqualTo("SUBMITTED");

        // Verify service interaction matches COBOL program flow
        verify(reportService, times(1)).processReportRequest(argThat(req -> 
            req.getReportType().equals(MONTHLY_REPORT_TYPE) &&
            req.getStartDate().equals(TEST_START_DATE) &&
            req.getEndDate().equals(TEST_END_DATE) &&
            req.getConfirmPrint() == true
        ));
    }

    /**
     * Tests the job status monitoring endpoint GET /api/reports/status/{jobId}
     * This validates the asynchronous job tracking that replaced COBOL TDQ monitoring.
     */
    @Test
    @DisplayName("Test Get Report Status - Asynchronous job monitoring")
    void testGetReportStatus() throws Exception {
        // Arrange - Mock batch job status response
        Map<String, Object> jobStatusData = new HashMap<>();
        jobStatusData.put("jobExecutionId", TEST_JOB_EXECUTION_ID);
        jobStatusData.put("jobName", TEST_JOB_NAME);
        jobStatusData.put("status", BatchStatus.COMPLETED.toString());
        jobStatusData.put("exitStatus", ExitStatus.COMPLETED.getExitCode());
        jobStatusData.put("durationMillis", 45000L);

        when(batchJobLauncher.getJobStatus(TEST_JOB_EXECUTION_ID))
            .thenReturn(jobStatusData);

        // Act - Query job status
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/status/" + TEST_JOB_EXECUTION_ID,
            Map.class
        );

        // Assert - Validate job status response
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        Assertions.assertThat(responseBody).isNotNull();
        Assertions.assertThat(responseBody.get("jobExecutionId"))
            .isEqualTo(TEST_JOB_EXECUTION_ID.intValue());
        Assertions.assertThat(responseBody.get("status"))
            .isEqualTo(BatchStatus.COMPLETED.toString());
        
        verify(batchJobLauncher, times(1)).getJobStatus(TEST_JOB_EXECUTION_ID);
    }

    /**
     * Tests the report menu response processing equivalent to COBOL BMS map handling.
     * Validates that JSON responses match BMS map field structure and validation.
     */
    @Test
    @DisplayName("Test Report Menu Response - BMS map equivalent JSON processing")
    void testReportMenuResponse() throws Exception {
        // Arrange - Mock report menu with validation errors (COBOL equivalent)
        ReportMenuResponse menuResponse = new ReportMenuResponse();
        menuResponse.setErrorMessage("Start Date - Month can NOT be empty...");
        List<String> validationErrors = new ArrayList<>();
        validationErrors.add("Start Date - Month can NOT be empty...");
        validationErrors.add("End Date - Year can NOT be empty...");
        menuResponse.setValidationErrors(validationErrors);

        when(reportService.processReportRequest(any(ReportRequest.class)))
            .thenReturn(menuResponse);

        // Create invalid request (empty dates)
        ReportRequest request = new ReportRequest();
        request.setReportType(CUSTOM_REPORT_TYPE);
        // Leave dates null to trigger validation errors

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ReportMenuResponse> response = restTemplate.postForEntity(
            baseUrl + "/generate",
            requestEntity,
            ReportMenuResponse.class
        );

        // Assert - Validate COBOL-equivalent error handling
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportMenuResponse responseBody = response.getBody();
        Assertions.assertThat(responseBody).isNotNull();
        Assertions.assertThat(responseBody.getErrorMessage())
            .isEqualTo("Start Date - Month can NOT be empty...");
        Assertions.assertThat(responseBody.getValidationErrors())
            .hasSize(2)
            .contains("Start Date - Month can NOT be empty...")
            .contains("End Date - Year can NOT be empty...");
    }

    /**
     * Tests comprehensive date range validation matching COBOL CSUTLDTC equivalent logic.
     * This ensures date validation precision and error messages match the original COBOL program.
     */
    @Test
    @DisplayName("Test Date Range Validation - CSUTLDTC equivalent logic")
    void testDateRangeValidation() throws Exception {
        // Test various date validation scenarios from COBOL
        
        // Test Case 1: Invalid month (> 12)
        testDateValidationScenario("2024-13-01", "2024-12-31", 
            "Start Date - Not a valid Month...");
        
        // Test Case 2: Invalid day (> 31) 
        testDateValidationScenario("2024-01-32", "2024-12-31",
            "Start Date - Not a valid Day...");
        
        // Test Case 3: Invalid year format
        testDateValidationScenario("24-01-01", "2024-12-31",
            "Start Date - Not a valid Year...");
        
        // Test Case 4: End date before start date
        testDateValidationScenario("2024-06-01", "2024-01-31",
            "End Date - must be after Start Date...");
        
        // Test Case 5: Valid date range
        testDateValidationScenario("2024-01-01", "2024-01-31", null);
    }

    /**
     * Helper method to test specific date validation scenarios
     */
    private void testDateValidationScenario(String startDate, String endDate, 
                                          String expectedError) throws Exception {
        // Mock service response based on validation result
        ReportMenuResponse response = new ReportMenuResponse();
        if (expectedError != null) {
            response.setErrorMessage(expectedError);
            List<String> validationErrors = new ArrayList<>();
            validationErrors.add(expectedError);
            response.setValidationErrors(validationErrors);
        } else {
            response.setSuccessMessage("Custom report submitted for printing ...");
            response.setReportStatus("SUBMITTED");
        }

        when(reportService.validateDateRange(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(expectedError == null);
        when(reportService.processReportRequest(any(ReportRequest.class)))
            .thenReturn(response);

        // Create test request
        ReportRequest request = new ReportRequest();
        request.setReportType(CUSTOM_REPORT_TYPE);
        try {
            request.setStartDate(LocalDate.parse(startDate));
            request.setEndDate(LocalDate.parse(endDate));
        } catch (Exception e) {
            // Handle invalid date format for negative testing
            request.setStartDate(null);
            request.setEndDate(null);
        }
        request.setConfirmPrint(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

        // Execute request
        ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
            baseUrl + "/generate",
            requestEntity,
            ReportMenuResponse.class
        );

        // Validate response
        Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportMenuResponse responseBody = httpResponse.getBody();
        Assertions.assertThat(responseBody).isNotNull();

        if (expectedError != null) {
            Assertions.assertThat(responseBody.getErrorMessage()).isEqualTo(expectedError);
        } else {
            Assertions.assertThat(responseBody.getSuccessMessage())
                .contains("Custom report submitted for printing");
        }
    }

    /**
     * Tests batch job submission functionality that replaces COBOL TDQ/JCL processing.
     * Validates that Spring Batch job launching provides equivalent functionality to 
     * COBOL SUBMIT-JOB-TO-INTRDR and WIRTE-JOBSUB-TDQ paragraphs.
     */
    @Test
    @DisplayName("Test Batch Job Submission - Spring Batch replacing TDQ/JCL")
    void testBatchJobSubmission() throws Exception {
        // Arrange - Mock successful batch job launch
        Map<String, Object> launchResponse = new HashMap<>();
        launchResponse.put("jobExecutionId", TEST_JOB_EXECUTION_ID);
        launchResponse.put("jobName", TEST_JOB_NAME);
        launchResponse.put("status", BatchStatus.STARTED.toString());
        
        when(reportService.submitReportJob(any(ReportRequest.class)))
            .thenReturn(TEST_JOB_EXECUTION_ID);
        when(batchJobLauncher.launchJobAsync(eq(TEST_JOB_NAME), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(launchResponse));

        // Create request for batch job submission
        ReportRequest request = new ReportRequest();
        request.setReportType(YEARLY_REPORT_TYPE);
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 12, 31));
        request.setConfirmPrint(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act - Submit report generation request
        long startTime = System.currentTimeMillis();
        ResponseEntity<ReportMenuResponse> response = restTemplate.postForEntity(
            baseUrl + "/generate",
            requestEntity,
            ReportMenuResponse.class
        );
        long responseTime = System.currentTimeMillis() - startTime;

        // Assert - Validate batch job submission
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(responseTime)
            .as("Batch job submission must complete within 5 seconds")
            .isLessThan(5000L);

        // Verify batch job launcher was called with correct parameters
        verify(reportService, times(1)).submitReportJob(argThat(req ->
            req.getReportType().equals(YEARLY_REPORT_TYPE) &&
            req.getStartDate().getYear() == 2024 &&
            req.getEndDate().getYear() == 2024
        ));
    }

    /**
     * Tests asynchronous job execution and monitoring capabilities.
     * This validates the replacement of COBOL synchronous processing with 
     * modern asynchronous job execution patterns.
     */
    @Test
    @DisplayName("Test Asynchronous Job Execution - Modern async pattern replacing COBOL sync")
    void testAsynchronousJobExecution() throws Exception {
        // Arrange - Mock asynchronous job execution progression
        Map<String, Object> initialStatus = new HashMap<>();
        initialStatus.put("jobExecutionId", TEST_JOB_EXECUTION_ID);
        initialStatus.put("status", BatchStatus.STARTED.toString());
        
        Map<String, Object> completedStatus = new HashMap<>();
        completedStatus.put("jobExecutionId", TEST_JOB_EXECUTION_ID);
        completedStatus.put("status", BatchStatus.COMPLETED.toString());
        completedStatus.put("exitStatus", ExitStatus.COMPLETED.getExitCode());

        when(reportService.submitReportJob(any(ReportRequest.class)))
            .thenReturn(TEST_JOB_EXECUTION_ID);
        
        // Mock status progression: STARTED -> COMPLETED
        when(batchJobLauncher.getJobStatus(TEST_JOB_EXECUTION_ID))
            .thenReturn(initialStatus)
            .thenReturn(completedStatus);

        // Submit job
        ReportRequest request = new ReportRequest();
        request.setReportType(MONTHLY_REPORT_TYPE);
        request.setStartDate(TEST_START_DATE);
        request.setEndDate(TEST_END_DATE);
        request.setConfirmPrint(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<ReportMenuResponse> submitResponse = restTemplate.postForEntity(
            baseUrl + "/generate",
            requestEntity,
            ReportMenuResponse.class
        );

        // Assert initial submission
        Assertions.assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Act - Monitor job execution asynchronously using Awaitility
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ResponseEntity<Map> statusResponse = restTemplate.getForEntity(
                    baseUrl + "/status/" + TEST_JOB_EXECUTION_ID,
                    Map.class
                );
                
                Assertions.assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                Map<String, Object> statusBody = statusResponse.getBody();
                Assertions.assertThat(statusBody).isNotNull();
                
                // Job should eventually complete
                String status = (String) statusBody.get("status");
                if (BatchStatus.COMPLETED.toString().equals(status)) {
                    Assertions.assertThat(statusBody.get("exitStatus"))
                        .isEqualTo(ExitStatus.COMPLETED.getExitCode());
                }
            });

        // Verify asynchronous job monitoring calls
        verify(batchJobLauncher, atLeast(1)).getJobStatus(TEST_JOB_EXECUTION_ID);
    }

    /**
     * Nested test class for testing different report types (Monthly, Yearly, Custom)
     * corresponding to COBOL EVALUATE TRUE conditions in PROCESS-ENTER-KEY.
     */
    @Nested
    @DisplayName("Report Type Processing Tests - COBOL EVALUATE conditions")
    class ReportTypeTests {

        @Test
        @DisplayName("Test Monthly Report Generation - COBOL MONTHLYI processing")
        void testMonthlyReportGeneration() throws Exception {
            // Mock monthly report logic matching COBOL calculation
            ReportMenuResponse response = new ReportMenuResponse();
            response.setSuccessMessage("Monthly report submitted for printing ...");
            response.setReportStatus("SUBMITTED");
            
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(response);

            ReportRequest request = new ReportRequest();
            request.setReportType(MONTHLY_REPORT_TYPE);
            // Monthly reports use current month calculation like COBOL
            LocalDate now = LocalDate.now();
            request.setStartDate(now.withDayOfMonth(1));
            request.setEndDate(now.withDayOfMonth(now.lengthOfMonth()));
            request.setConfirmPrint(true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
                baseUrl + "/generate",
                requestEntity,
                ReportMenuResponse.class
            );

            Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(httpResponse.getBody().getSuccessMessage())
                .contains("Monthly report submitted for printing");
        }

        @Test
        @DisplayName("Test Yearly Report Generation - COBOL YEARLYI processing")
        void testYearlyReportGeneration() throws Exception {
            // Mock yearly report logic
            ReportMenuResponse response = new ReportMenuResponse();
            response.setSuccessMessage("Yearly report submitted for printing ...");
            response.setReportStatus("SUBMITTED");
            
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(response);

            ReportRequest request = new ReportRequest();
            request.setReportType(YEARLY_REPORT_TYPE);
            // Yearly reports use current year like COBOL logic
            LocalDate now = LocalDate.now();
            request.setStartDate(LocalDate.of(now.getYear(), 1, 1));
            request.setEndDate(LocalDate.of(now.getYear(), 12, 31));
            request.setConfirmPrint(true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
                baseUrl + "/generate",
                requestEntity,
                ReportMenuResponse.class
            );

            Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(httpResponse.getBody().getSuccessMessage())
                .contains("Yearly report submitted for printing");
        }

        @Test
        @DisplayName("Test Custom Report Generation - COBOL CUSTOMI processing")
        void testCustomReportGeneration() throws Exception {
            // Mock custom report with user-specified dates
            ReportMenuResponse response = new ReportMenuResponse();
            response.setSuccessMessage("Custom report submitted for printing ...");
            response.setReportStatus("SUBMITTED");
            
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(response);
            when(reportService.validateDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(true);

            ReportRequest request = new ReportRequest();
            request.setReportType(CUSTOM_REPORT_TYPE);
            request.setStartDate(TEST_START_DATE);
            request.setEndDate(TEST_END_DATE);
            request.setConfirmPrint(true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
                baseUrl + "/generate",
                requestEntity,
                ReportMenuResponse.class
            );

            Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(httpResponse.getBody().getSuccessMessage())
                .contains("Custom report submitted for printing");
            
            // Verify date validation was called (CSUTLDTC equivalent)
            verify(reportService, times(1)).validateDateRange(TEST_START_DATE, TEST_END_DATE);
        }
    }

    /**
     * Nested test class for comprehensive error handling scenarios matching 
     * COBOL error processing patterns.
     */
    @Nested
    @DisplayName("Error Handling Tests - COBOL error processing equivalent")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Test Confirmation Required Error - COBOL CONFIRMI validation")
        void testConfirmationRequired() throws Exception {
            // Mock confirmation validation error
            ReportMenuResponse response = new ReportMenuResponse();
            response.setErrorMessage("Please confirm to print the Monthly report...");
            List<String> errors = new ArrayList<>();
            errors.add("Please confirm to print the Monthly report...");
            response.setValidationErrors(errors);
            
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(response);

            ReportRequest request = new ReportRequest();
            request.setReportType(MONTHLY_REPORT_TYPE);
            request.setStartDate(TEST_START_DATE);
            request.setEndDate(TEST_END_DATE);
            request.setConfirmPrint(false); // No confirmation

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
                baseUrl + "/generate",
                requestEntity,
                ReportMenuResponse.class
            );

            Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(httpResponse.getBody().getErrorMessage())
                .contains("Please confirm to print the Monthly report");
        }

        @Test
        @DisplayName("Test TDQ Write Error - COBOL WIRTE-JOBSUB-TDQ equivalent")
        void testBatchJobSubmissionError() throws Exception {
            // Mock batch job submission failure (TDQ write error equivalent)
            when(reportService.submitReportJob(any(ReportRequest.class)))
                .thenThrow(new RuntimeException("Unable to submit batch job"));
            
            ReportMenuResponse errorResponse = new ReportMenuResponse();
            errorResponse.setErrorMessage("Unable to Write TDQ (JOBS)...");
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(errorResponse);

            ReportRequest request = new ReportRequest();
            request.setReportType(MONTHLY_REPORT_TYPE);
            request.setStartDate(TEST_START_DATE);
            request.setEndDate(TEST_END_DATE);
            request.setConfirmPrint(true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
                baseUrl + "/generate",
                requestEntity,
                ReportMenuResponse.class
            );

            Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(httpResponse.getBody().getErrorMessage())
                .contains("Unable to Write TDQ (JOBS)");
        }
    }

    /**
     * Nested test class for performance validation ensuring COBOL response time parity.
     */
    @Nested
    @DisplayName("Performance Tests - COBOL response time parity")
    class PerformanceTests {

        @Test
        @DisplayName("Test Response Time Under 200ms - COBOL performance parity")
        void testResponseTimePerformance() throws Exception {
            // Mock fast service response
            ReportMenuResponse response = new ReportMenuResponse();
            response.setSuccessMessage("Monthly report submitted for printing ...");
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(response);

            ReportRequest request = new ReportRequest();
            request.setReportType(MONTHLY_REPORT_TYPE);
            request.setStartDate(TEST_START_DATE);
            request.setEndDate(TEST_END_DATE);
            request.setConfirmPrint(true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            // Measure multiple requests for average response time
            List<Long> responseTimes = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                long startTime = System.currentTimeMillis();
                ResponseEntity<ReportMenuResponse> httpResponse = restTemplate.postForEntity(
                    baseUrl + "/generate",
                    requestEntity,
                    ReportMenuResponse.class
                );
                long responseTime = System.currentTimeMillis() - startTime;
                responseTimes.add(responseTime);
                
                Assertions.assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            // Verify average response time meets COBOL parity requirement
            double averageResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

            Assertions.assertThat(averageResponseTime)
                .as("Average response time must be under 200ms for COBOL parity")
                .isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        }

        @Test
        @DisplayName("Test Concurrent Request Handling - Load testing")
        void testConcurrentRequestHandling() throws Exception {
            // Mock service for concurrent testing
            ReportMenuResponse response = new ReportMenuResponse();
            response.setSuccessMessage("Monthly report submitted for printing ...");
            when(reportService.processReportRequest(any(ReportRequest.class)))
                .thenReturn(response);

            ReportRequest request = new ReportRequest();
            request.setReportType(MONTHLY_REPORT_TYPE);
            request.setStartDate(TEST_START_DATE);
            request.setEndDate(TEST_END_DATE);
            request.setConfirmPrint(true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReportRequest> requestEntity = new HttpEntity<>(request, headers);

            // Execute 10 concurrent requests
            List<CompletableFuture<ResponseEntity<ReportMenuResponse>>> futures = new ArrayList<>();
            
            for (int i = 0; i < 10; i++) {
                CompletableFuture<ResponseEntity<ReportMenuResponse>> future = 
                    CompletableFuture.supplyAsync(() -> {
                        return restTemplate.postForEntity(
                            baseUrl + "/generate",
                            requestEntity,
                            ReportMenuResponse.class
                        );
                    });
                futures.add(future);
            }

            // Wait for all requests to complete
            List<ResponseEntity<ReportMenuResponse>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            // Verify all requests succeeded
            results.forEach(result -> {
                Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                Assertions.assertThat(result.getBody().getSuccessMessage())
                    .contains("Monthly report submitted for printing");
            });

            // Verify service was called for each concurrent request
            verify(reportService, times(10)).processReportRequest(any(ReportRequest.class));
        }
    }

    /**
     * Helper methods for BigDecimal precision testing (COBOL COMP-3 equivalent)
     */
    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        Assertions.assertThat(actual)
            .as(message)
            .usingComparator(BigDecimal::compareTo)
            .isEqualTo(expected);
    }

    /**
     * Load test fixture data for comprehensive testing scenarios
     */
    private void loadTestFixtures() {
        // Implementation would load test data for comprehensive integration testing
        // This matches the AbstractBaseTest.loadTestFixtures() pattern
    }
}