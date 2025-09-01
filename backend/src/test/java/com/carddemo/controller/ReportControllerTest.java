package com.carddemo.controller;

import com.carddemo.dto.ReportRequestDto;
import com.carddemo.dto.ApiResponse;
import com.carddemo.dto.ResponseStatus;
import com.carddemo.service.ReportService;
import com.carddemo.batch.BatchJobLauncher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit tests for ReportController implementing COBOL CORPT00C functional parity.
 * 
 * This test class validates the following COBOL CORPT00C equivalent functionality:
 * - Report type selection (Monthly, Yearly, Custom) mapped from COBOL 1000-REPORT-MENU
 * - Date range validation using CSUTLDTC equivalent logic
 * - Spring Batch job submission replacing JCL/TDQ generation  
 * - BMS map equivalent JSON request/response processing
 * - Performance requirements maintaining sub-200ms response times
 * - Error handling matching COBOL 9000-ERROR-HANDLING patterns
 */
@ExtendWith(MockitoExtension.class)
public class ReportControllerTest {

    @InjectMocks
    private ReportController reportController;

    @Mock
    private ReportService reportService;

    @Mock
    private BatchJobLauncher batchJobLauncher;

    @BeforeEach
    void setUp() {
        reset(reportService, batchJobLauncher);
    }

    @Nested
    @DisplayName("Report Type Selection Tests - Maps to COBOL 1000-REPORT-MENU")
    class ReportTypeTests {

        @Test
        @DisplayName("Monthly Report Generation - Equivalent to COBOL M option")
        void testGenerateMonthlyReport_Success() {
            // Arrange - Mock successful monthly report processing matching COBOL logic
            Map<String, Object> serviceResult = new HashMap<>();
            serviceResult.put("success", true);
            serviceResult.put("jobId", "M202412001"); // Format matching JCL job naming
            serviceResult.put("message", "Monthly report submitted for printing");
            serviceResult.put("reportType", "MONTHLY");
            
            when(reportService.processReportRequest(eq("M"), anyString(), anyString()))
                .thenReturn(serviceResult);

            // Create valid monthly report request
            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.MONTHLY);
            request.setConfirmationFlag(true);

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
            assertEquals(true, response.getBody().getResponseData().get("success"));
            assertEquals("M202412001", response.getBody().getResponseData().get("jobId"));
            assertEquals("MONTHLY", response.getBody().getResponseData().get("reportType"));

            // Verify service was called with correct parameters (M = Monthly)
            verify(reportService, times(1)).processReportRequest(eq("M"), anyString(), anyString());
        }

        @Test
        @DisplayName("Yearly Report Generation - Equivalent to COBOL Y option")
        void testGenerateYearlyReport_Success() {
            // Arrange
            Map<String, Object> serviceResult = new HashMap<>();
            serviceResult.put("success", true);
            serviceResult.put("jobId", "Y202412001");
            serviceResult.put("reportType", "YEARLY");
            
            when(reportService.processReportRequest(eq("Y"), anyString(), anyString()))
                .thenReturn(serviceResult);

            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.YEARLY);
            request.setConfirmationFlag(true);

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
            assertEquals("Y202412001", response.getBody().getResponseData().get("jobId"));
            
            // Verify service was called with correct parameters (Y = Yearly)
            verify(reportService, times(1)).processReportRequest(eq("Y"), anyString(), anyString());
        }

        @Test
        @DisplayName("Custom Report Generation - Equivalent to COBOL C option")
        void testGenerateCustomReport_Success() {
            // Arrange
            Map<String, Object> serviceResult = new HashMap<>();
            serviceResult.put("success", true);
            serviceResult.put("jobId", "C202412001");
            serviceResult.put("reportType", "CUSTOM");
            
            when(reportService.processReportRequest(eq("C"), eq("20240101"), eq("20240131")))
                .thenReturn(serviceResult);

            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.CUSTOM);
            request.setStartDate(LocalDate.of(2024, 1, 1));
            request.setEndDate(LocalDate.of(2024, 1, 31));
            request.setConfirmationFlag(true);

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
            assertEquals("C202412001", response.getBody().getResponseData().get("jobId"));
            
            // Verify service was called with correct date parameters (YYYYMMDD format)
            verify(reportService, times(1)).processReportRequest(eq("C"), eq("20240101"), eq("20240131"));
        }
    }

    @Nested
    @DisplayName("Date Validation Tests - Maps to CSUTLDTC date validation logic")
    class DateValidationTests {

        @Test
        @DisplayName("Invalid Date Range - Start after End")
        void testInvalidDateRange_StartAfterEnd() {
            // Arrange - Invalid date range should cause validation error at controller level
            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.CUSTOM);
            request.setStartDate(LocalDate.of(2024, 12, 31));
            request.setEndDate(LocalDate.of(2024, 1, 1));
            request.setConfirmationFlag(true);

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert - Should return error due to invalid date range
            assertEquals(400, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.ERROR, response.getBody().getStatus());
            
            // Verify service was NOT called due to controller-level validation
            verify(reportService, never()).processReportRequest(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Missing Start Date for Custom Report")
        void testMissingStartDate_CustomReport() {
            // Arrange
            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.CUSTOM);
            request.setEndDate(LocalDate.of(2024, 1, 31));
            request.setConfirmationFlag(true);

            // Act - Controller should validate and reject
            try {
                ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);
                // If no exception, check response for error
                assertTrue(response.getStatusCodeValue() >= 400 || 
                          response.getBody().getStatus() == ResponseStatus.ERROR);
            } catch (Exception e) {
                // Exception is acceptable for validation error
                assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException);
            }
        }

        @Test
        @DisplayName("Missing End Date for Custom Report")
        void testMissingEndDate_CustomReport() {
            // Arrange
            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.CUSTOM);
            request.setStartDate(LocalDate.of(2024, 1, 1));
            request.setConfirmationFlag(true);

            // Act - Controller should validate and reject
            try {
                ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);
                // If no exception, check response for error
                assertTrue(response.getStatusCodeValue() >= 400 || 
                          response.getBody().getStatus() == ResponseStatus.ERROR);
            } catch (Exception e) {
                // Exception is acceptable for validation error
                assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException);
            }
        }
    }

    @Nested
    @DisplayName("Confirmation Validation Tests - Maps to COBOL BMS confirm screen logic")
    class ConfirmationTests {

        @Test
        @DisplayName("Missing Confirmation Flag")
        void testMissingConfirmation_Error() {
            // Arrange
            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.MONTHLY);
            request.setConfirmationFlag(false); // Not confirmed

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert - Should return error due to missing confirmation
            assertEquals(400, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.ERROR, response.getBody().getStatus());
            
            // Verify service was NOT called since confirmation failed
            verify(reportService, never()).processReportRequest(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Valid Confirmation Flag")
        void testValidConfirmation_Success() {
            // Arrange
            Map<String, Object> serviceResult = new HashMap<>();
            serviceResult.put("success", true);
            serviceResult.put("jobId", "M202412001");
            
            when(reportService.processReportRequest(eq("M"), anyString(), anyString()))
                .thenReturn(serviceResult);

            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.MONTHLY);
            request.setConfirmationFlag(true); // Properly confirmed

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
            
            verify(reportService, times(1)).processReportRequest(eq("M"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests - Maps to COBOL 9000-ERROR-HANDLING")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Service Exception Handling")
        void testServiceException_ErrorResponse() {
            // Arrange - Service throws exception
            when(reportService.processReportRequest(eq("Y"), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.YEARLY);
            request.setConfirmationFlag(true);

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);

            // Assert - Should return error from service
            assertEquals(500, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.ERROR, response.getBody().getStatus());
            assertFalse(response.getBody().getMessages().isEmpty());
            
            // Verify service was called but failed
            verify(reportService, times(1)).processReportRequest(eq("Y"), anyString(), anyString());
        }

        @Test
        @DisplayName("Null Request Handling")
        void testNullRequest_ErrorResponse() {
            // Act - Controller throws NullPointerException for null request
            assertThrows(NullPointerException.class, () -> {
                reportController.generateReport(null);
            });
            
            // Verify service was not called
            verify(reportService, never()).processReportRequest(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Performance Requirements Tests - Sub-200ms Response Time")
    class PerformanceTests {

        @Test
        @DisplayName("Response Time Validation")
        void testResponseTime_Under200ms() {
            // Arrange
            Map<String, Object> serviceResult = new HashMap<>();
            serviceResult.put("success", true);
            serviceResult.put("jobId", "M202412001");
            
            when(reportService.processReportRequest(eq("M"), anyString(), anyString()))
                .thenReturn(serviceResult);

            ReportRequestDto request = new ReportRequestDto();
            request.setReportType(ReportRequestDto.ReportType.MONTHLY);
            request.setConfirmationFlag(true);

            // Act with timing
            long startTime = System.currentTimeMillis();
            ResponseEntity<ApiResponse<Map<String, Object>>> response = reportController.generateReport(request);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            // Assert - Response time should be well under 200ms for unit test
            assertTrue(responseTime < 200, "Response time " + responseTime + "ms should be under 200ms");
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
            assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        }
    }
}