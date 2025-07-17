/**
 * CardDemo - Report Generation TypeScript Type Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for the report
 * generation screen (CORPT00) including report parameters, batch job configuration,
 * result metadata, and scheduled generation types.
 * 
 * Maps BMS mapset CORPT00 fields to modern React component types while preserving
 * exact field structures and validation patterns from the original COBOL/BMS 
 * implementation including report type selection, date range configuration, and
 * Spring Batch job parameter specification.
 * 
 * Implements type-safe report generation patterns that maintain functional equivalence
 * with the original mainframe report processing while providing enhanced user experience
 * through modern React Hook Form integration and comprehensive validation.
 */

import { FormFieldAttributes } from './CommonTypes';
import { FormValidationSchema } from './ValidationTypes';

// ===================================================================
// REPORT TYPE DEFINITIONS
// ===================================================================

/**
 * Report Type Union - Defines available report types for generation
 * Maps to BMS CORPT00 report selection fields (MONTHLY, YEARLY, CUSTOM)
 */
export type ReportType = 
  | 'MONTHLY'  // Monthly report for current month
  | 'YEARLY'   // Yearly report for current year  
  | 'CUSTOM';  // Custom date range report

/**
 * Report Generation Status - Tracks Spring Batch job execution status
 * Provides comprehensive status tracking for asynchronous report generation
 */
export type ReportGenerationStatus = 
  | 'PENDING'    // Job queued for execution
  | 'RUNNING'    // Job currently executing
  | 'COMPLETED'  // Job completed successfully
  | 'FAILED'     // Job failed with errors
  | 'CANCELLED'; // Job cancelled by user or system

// ===================================================================
// CORE REPORT INTERFACES
// ===================================================================

/**
 * Report Parameters Data Interface
 * 
 * Maps directly to BMS CORPT00 mapset fields preserving exact field behavior
 * and validation patterns from the original COBOL implementation.
 * 
 * Based on analysis of CORPT00.bms field definitions including report type
 * selection, custom date range specification, and confirmation handling.
 */
export interface ReportParametersData {
  /**
   * Monthly report selection - Maps to BMS MONTHLY field
   * Single character flag indicating monthly report selection
   */
  monthly: string;
  
  /**
   * Yearly report selection - Maps to BMS YEARLY field
   * Single character flag indicating yearly report selection
   */
  yearly: string;
  
  /**
   * Custom report selection - Maps to BMS CUSTOM field
   * Single character flag indicating custom date range report selection
   */
  custom: string;
  
  /**
   * Start date month - Maps to BMS SDTMM field
   * Two-digit month (01-12) for custom date range start
   */
  startDateMM: string;
  
  /**
   * Start date day - Maps to BMS SDTDD field
   * Two-digit day (01-31) for custom date range start
   */
  startDateDD: string;
  
  /**
   * Start date year - Maps to BMS SDTYYYY field
   * Four-digit year for custom date range start
   */
  startDateYYYY: string;
  
  /**
   * End date month - Maps to BMS EDTMM field
   * Two-digit month (01-12) for custom date range end
   */
  endDateMM: string;
  
  /**
   * End date day - Maps to BMS EDTDD field
   * Two-digit day (01-31) for custom date range end
   */
  endDateDD: string;
  
  /**
   * End date year - Maps to BMS EDTYYYY field
   * Four-digit year for custom date range end
   */
  endDateYYYY: string;
  
  /**
   * Report generation confirmation - Maps to BMS CONFIRM field
   * Single character confirmation (Y/N) for report submission
   */
  confirmation: string;
  
  /**
   * Error message display - Maps to BMS ERRMSG field
   * Error message text for validation failures and system errors
   */
  errorMessage: string;
  
  /**
   * BMS field attributes - Form field attribute mappings
   * Preserves BMS field definition attributes for React component rendering
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Report Generation Request Interface
 * 
 * Defines comprehensive request structure for Spring Batch job execution
 * including report parameters, output format specification, and batch job
 * configuration for asynchronous report generation processing.
 * 
 * Integrates with Spring Batch framework for robust job execution and
 * monitoring capabilities equivalent to JCL-based batch processing.
 */
export interface ReportGenerationRequest {
  /**
   * Report type - Selected report type for generation
   * Determines the report logic and data selection criteria
   */
  reportType: ReportType;
  
  /**
   * Report start date - Formatted start date for report data selection
   * ISO 8601 format (YYYY-MM-DD) for consistent date handling
   */
  startDate: string;
  
  /**
   * Report end date - Formatted end date for report data selection
   * ISO 8601 format (YYYY-MM-DD) for consistent date handling
   */
  endDate: string;
  
  /**
   * Output format - Desired report output format
   * Supports multiple formats for different use cases
   */
  outputFormat: 'PDF' | 'CSV' | 'EXCEL' | 'HTML';
  
  /**
   * Batch job parameters - Spring Batch job configuration
   * Contains job-specific parameters for execution control
   */
  batchJobParameters: Record<string, any>;
  
  /**
   * Request ID - Unique identifier for tracking request
   * Generated UUID for request correlation and tracking
   */
  requestId: string;
  
  /**
   * User ID - Requesting user identifier
   * Links report generation to specific user for audit purposes
   */
  userId: string;
  
  /**
   * Correlation ID - Request correlation identifier
   * Used for distributed tracing and request correlation
   */
  correlationId: string;
}

/**
 * Report Result Data Interface
 * 
 * Contains comprehensive metadata and download information for generated reports
 * including file location, size, creation timestamp, and expiration handling.
 * 
 * Provides complete report lifecycle management from generation to cleanup
 * with proper audit trail and user access control.
 */
export interface ReportResultData {
  /**
   * Report ID - Unique identifier for generated report
   * Primary key for report tracking and retrieval
   */
  reportId: string;
  
  /**
   * Report type - Type of report that was generated
   * Maintains link to original request parameters
   */
  reportType: ReportType;
  
  /**
   * Generation status - Current status of report generation
   * Tracks Spring Batch job execution progress
   */
  generationStatus: ReportGenerationStatus;
  
  /**
   * Download URL - Secure URL for report download
   * Time-limited signed URL for secure file access
   */
  downloadUrl: string;
  
  /**
   * File name - Generated report file name
   * Descriptive filename including timestamp and format
   */
  fileName: string;
  
  /**
   * File size - Report file size in bytes
   * Used for download progress and storage management
   */
  fileSize: number;
  
  /**
   * Creation date - When the report was generated
   * ISO 8601 timestamp for audit and lifecycle management
   */
  createdDate: string;
  
  /**
   * Expiration date - When the report download expires
   * ISO 8601 timestamp for automatic cleanup scheduling
   */
  expirationDate: string;
  
  /**
   * Error message - Generation error details if failed
   * Detailed error information for troubleshooting
   */
  errorMessage?: string;
  
  /**
   * Report metadata - Additional report information
   * Contains generation statistics and configuration details
   */
  metadata: {
    recordCount: number;
    processingTime: number;
    parameters: Record<string, any>;
    version: string;
  };
}

/**
 * Report Schedule Data Interface
 * 
 * Defines configuration for automated report generation scheduling
 * including cron expressions, batch job configuration, and lifecycle
 * management for recurring report generation tasks.
 * 
 * Integrates with Spring Batch scheduler for automated execution
 * equivalent to JCL scheduled job processing with enhanced monitoring.
 */
export interface ReportScheduleData {
  /**
   * Schedule ID - Unique identifier for scheduled report
   * Primary key for schedule management and tracking
   */
  scheduleId: string;
  
  /**
   * Report type - Type of report to generate on schedule
   * Determines the report logic and parameters for scheduled execution
   */
  reportType: ReportType;
  
  /**
   * Schedule frequency - Human-readable schedule frequency
   * Descriptive text for user interface display
   */
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY' | 'CUSTOM';
  
  /**
   * Cron expression - Detailed schedule specification
   * Standard cron format for precise scheduling control
   */
  cronExpression: string;
  
  /**
   * Next run date - When the schedule will next execute
   * ISO 8601 timestamp for next execution planning
   */
  nextRunDate: string;
  
  /**
   * Last run date - When the schedule last executed
   * ISO 8601 timestamp for execution history tracking
   */
  lastRunDate?: string;
  
  /**
   * Batch job configuration - Spring Batch job parameters
   * Contains job-specific configuration for scheduled execution
   */
  batchJobConfig: {
    jobName: string;
    parameters: Record<string, any>;
    restartable: boolean;
    incrementer: string;
  };
  
  /**
   * Schedule active status - Whether the schedule is currently active
   * Controls automatic execution of scheduled reports
   */
  isActive: boolean;
  
  /**
   * User ID - Owner of the scheduled report
   * Links schedule to specific user for access control
   */
  userId: string;
  
  /**
   * Schedule parameters - Fixed parameters for scheduled execution
   * Contains static configuration that doesn't change between runs
   */
  parameters: {
    outputFormat: 'PDF' | 'CSV' | 'EXCEL' | 'HTML';
    emailNotification: boolean;
    retentionDays: number;
    priority: 'LOW' | 'NORMAL' | 'HIGH';
  };
}

// ===================================================================
// VALIDATION SCHEMA INTERFACE
// ===================================================================

/**
 * Report Validation Schema Interface
 * 
 * Provides comprehensive validation rules for report generation forms
 * including report type selection, date range validation, confirmation
 * handling, and cross-field validation for business rule enforcement.
 * 
 * Implements type-safe validation patterns that maintain CICS transaction
 * validation behavior while providing modern React Hook Form integration.
 */
export interface ReportValidationSchema extends FormValidationSchema<ReportParametersData> {
  /**
   * Report type validation - Validates report type selection
   * Ensures exactly one report type is selected (MONTHLY, YEARLY, or CUSTOM)
   */
  reportTypeValidation: (data: ReportParametersData) => string | undefined;
  
  /**
   * Date range validation - Validates custom date range specification
   * Ensures valid date format, logical date ranges, and business rule compliance
   */
  dateRangeValidation: (data: ReportParametersData) => string | undefined;
  
  /**
   * Confirmation validation - Validates report generation confirmation
   * Ensures proper Y/N confirmation before report submission
   */
  confirmationValidation: (data: ReportParametersData) => string | undefined;
  
  /**
   * Custom date validation - Validates individual date field components
   * Ensures proper MM/DD/YYYY format and valid date values
   */
  customDateValidation: (mm: string, dd: string, yyyy: string) => string | undefined;
  
  /**
   * Cross-field validation - Validates field relationships and business rules
   * Implements complex validation logic spanning multiple form fields
   */
  crossFieldValidation: (data: ReportParametersData) => string | undefined;
}