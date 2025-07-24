/**
 * ReportTypes.ts
 * 
 * TypeScript interface definitions for report generation screen (CORPT00) including report parameters,
 * batch job configuration, result metadata, and scheduled generation types.
 * 
 * This file transforms the CORPT00.bms report generation screen into TypeScript interfaces while
 * maintaining exact functional equivalence with the original COBOL BMS field validation and
 * Spring Batch integration requirements.
 * 
 * Original BMS Map: app/bms/CORPT00.bms
 * Original Copybook: app/cpy-bms/CORPT00.CPY
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { FormFieldAttributes } from './CommonTypes';
import { FormValidationSchema } from './ValidationTypes';

/**
 * Report Type Union Definition
 * Represents the three report generation options from CORPT00.bms
 * Maps to MONTHLY, YEARLY, and CUSTOM radio button fields
 */
export type ReportType = 
  | 'MONTHLY'   // Monthly (Current Month) - maps to MONTHLY field
  | 'YEARLY'    // Yearly (Current Year) - maps to YEARLY field  
  | 'CUSTOM';   // Custom (Date Range) - maps to CUSTOM field

/**
 * Report Generation Status Union Definition
 * Tracks the current status of batch report generation jobs
 * Used for Spring Batch job status monitoring and user feedback
 */
export type ReportGenerationStatus = 
  | 'PENDING'     // Report generation requested but not started
  | 'PROCESSING'  // Report generation job is currently running
  | 'COMPLETED'   // Report generation completed successfully
  | 'FAILED'      // Report generation failed with errors
  | 'CANCELLED'   // Report generation was cancelled by user or system
  | 'EXPIRED';    // Generated report has expired and is no longer available

/**
 * Report Parameters Data Interface
 * Maps directly to CORPT00.bms field structure for report parameter form
 * Maintains exact field mapping from BMS copybook CORPT0AI structure
 * 
 * Field Mappings from CORPT00.CPY:
 * - MONTHLYI PIC X(1) → monthly: string
 * - YEARLYI PIC X(1) → yearly: string  
 * - CUSTOMI PIC X(1) → custom: string
 * - SDTMMI PIC X(2) → startDateMM: string
 * - SDTDDI PIC X(2) → startDateDD: string
 * - SDTYYYYI PIC X(4) → startDateYYYY: string
 * - EDTMMI PIC X(2) → endDateMM: string
 * - EDTDDI PIC X(2) → endDateDD: string
 * - EDTYYYYI PIC X(4) → endDateYYYY: string
 * - CONFIRMI PIC X(1) → confirmation: string
 * - ERRMSGI PIC X(78) → errorMessage: string
 */
export interface ReportParametersData {
  /** Monthly report selection (1 character) - maps to MONTHLY field with UNPROT, IC attributes */
  monthly: string;
  
  /** Yearly report selection (1 character) - maps to YEARLY field with UNPROT attributes */
  yearly: string;
  
  /** Custom date range selection (1 character) - maps to CUSTOM field with UNPROT attributes */
  custom: string;
  
  /** Start date month (2 characters) - maps to SDTMM field with NUM, UNPROT attributes */
  startDateMM: string;
  
  /** Start date day (2 characters) - maps to SDTDD field with NUM, UNPROT attributes */
  startDateDD: string;
  
  /** Start date year (4 characters) - maps to SDTYYYY field with NUM, UNPROT attributes */
  startDateYYYY: string;
  
  /** End date month (2 characters) - maps to EDTMM field with NUM, UNPROT attributes */
  endDateMM: string;
  
  /** End date day (2 characters) - maps to EDTDD field with NUM, UNPROT attributes */
  endDateDD: string;
  
  /** End date year (4 characters) - maps to EDTYYYY field with NUM, UNPROT attributes */
  endDateYYYY: string;
  
  /** Confirmation field (1 character Y/N) - maps to CONFIRM field with UNPROT attributes */
  confirmation: string;
  
  /** Error message display (78 characters) - maps to ERRMSG field with ASKIP, BRT, FSET attributes */
  errorMessage: string;
  
  /** BMS field attributes for all form fields - enables React component BMS behavior replication */
  fieldAttributes: {
    monthly: FormFieldAttributes;
    yearly: FormFieldAttributes;
    custom: FormFieldAttributes;
    startDateMM: FormFieldAttributes;
    startDateDD: FormFieldAttributes;
    startDateYYYY: FormFieldAttributes;
    endDateMM: FormFieldAttributes;
    endDateDD: FormFieldAttributes;
    endDateYYYY: FormFieldAttributes;
    confirmation: FormFieldAttributes;
    errorMessage: FormFieldAttributes;
  };
}

/**
 * Report Generation Request Interface
 * Defines the structure for submitting report generation requests to Spring Batch
 * Transforms CORPT00 form data into batch job parameters for asynchronous processing
 * 
 * Integrates with Spring Batch architecture by providing:
 * - Job parameter specification for batch job execution
 * - Output format selection for generated reports
 * - Correlation tracking for request/response matching
 * - User context for security and audit trail
 */
export interface ReportGenerationRequest {
  /** Report type selection from form - determines batch job configuration */
  reportType: ReportType;
  
  /** Report start date in ISO format - converted from CORPT00 date fields */
  startDate: string;
  
  /** Report end date in ISO format - converted from CORPT00 date fields */
  endDate: string;
  
  /** Output format for generated report (PDF, CSV, XML) */
  outputFormat: 'PDF' | 'CSV' | 'XML';
  
  /** Spring Batch job parameters for batch execution configuration */
  batchJobParameters: {
    /** Job execution ID for tracking and monitoring */
    jobExecutionId?: number;
    /** Batch job name based on report type */
    jobName: string;
    /** Job parameters map for Spring Batch parameter passing */
    parameters: Record<string, string | number | Date>;
    /** Job restart parameters for failed job recovery */
    restartParameters?: Record<string, any>;
  };
  
  /** Unique request identifier for correlation and tracking */
  requestId: string;
  
  /** User ID from authentication context for security and audit */
  userId: string;
  
  /** Correlation ID for distributed tracing and request tracking */
  correlationId: string;
}

/**
 * Report Result Data Interface
 * Contains metadata and download information for generated reports
 * Provides comprehensive report status and access information for user interface
 * 
 * Supports:
 * - Report generation status tracking and monitoring
 * - Secure download URL generation with expiration
 * - File metadata for download progress and validation
 * - Error handling for failed report generation
 * - Audit trail with creation and expiration timestamps
 */
export interface ReportResultData {
  /** Unique report identifier for tracking and reference */
  reportId: string;
  
  /** Report type that was generated - matches original request */
  reportType: ReportType;
  
  /** Current status of report generation process */
  generationStatus: ReportGenerationStatus;
  
  /** Secure download URL for completed reports - expires after time limit */
  downloadUrl?: string;
  
  /** Generated report file name with appropriate extension */
  fileName?: string;
  
  /** File size in bytes for download progress indication */
  fileSize?: number;
  
  /** Report creation timestamp for audit and sorting */
  createdDate: string;
  
  /** Download URL expiration timestamp for security */
  expirationDate?: string;
  
  /** Error message for failed report generation - displayed to user */
  errorMessage?: string;
  
  /** Additional report metadata for enhanced functionality */
  metadata: {
    /** Total number of records included in the report */
    recordCount?: number;
    /** Report generation duration in milliseconds */
    generationTime?: number;
    /** Report data source timestamp range */
    dataRange?: {
      startDate: string;
      endDate: string;
    };
    /** User who requested the report for audit trail */
    requestedBy: string;
    /** Original batch job execution details */
    batchJobExecution?: {
      jobExecutionId: number;
      jobInstanceId: number;
      startTime: string;
      endTime?: string;
      status: 'STARTED' | 'COMPLETED' | 'FAILED' | 'STOPPED';
    };
  };
}

/**
 * Report Schedule Data Interface
 * Configuration for automated report generation using Spring Batch scheduler
 * Enables recurring report generation with cron-based scheduling
 * 
 * Supports:
 * - Flexible scheduling with cron expressions
 * - Batch job configuration for automated execution
 * - Schedule management (activate/deactivate)
 * - Parameter inheritance from original report requests
 * - Next run calculation and execution tracking
 */
export interface ReportScheduleData {
  /** Unique schedule identifier for management and tracking */
  scheduleId: string;
  
  /** Report type to be generated on schedule */
  reportType: ReportType;
  
  /** Schedule frequency type for user interface display */
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY' | 'CUSTOM';
  
  /** Cron expression for precise schedule definition */
  cronExpression: string;
  
  /** Next scheduled execution date/time */
  nextRunDate: string;
  
  /** Last execution date/time for audit and monitoring */
  lastRunDate?: string;
  
  /** Spring Batch job configuration for scheduled execution */
  batchJobConfig: {
    /** Batch job name for scheduled execution */
    jobName: string;
    /** Job parameters for scheduled report generation */
    jobParameters: Record<string, string | number | Date>;
    /** Job execution timeout in minutes */
    timeoutMinutes: number;
    /** Maximum retry attempts for failed jobs */
    maxRetryAttempts: number;
    /** Job notification configuration */
    notifications?: {
      /** Email notification on completion */
      emailOnCompletion?: boolean;
      /** Email notification on failure */
      emailOnFailure?: boolean;
      /** Notification recipients */
      recipients?: string[];
    };
  };
  
  /** Schedule active status - controls execution */
  isActive: boolean;
  
  /** User who created the schedule for audit trail */
  userId: string;
  
  /** Report generation parameters for scheduled execution */
  parameters: {
    /** Date range calculation for dynamic dates (e.g., "current month") */
    dateRangeType: 'FIXED' | 'RELATIVE';
    /** Fixed start date (ISO format) - used when dateRangeType is FIXED */
    startDate?: string;
    /** Fixed end date (ISO format) - used when dateRangeType is FIXED */
    endDate?: string;
    /** Relative date calculation (e.g., "CURRENT_MONTH", "LAST_30_DAYS") */
    relativeDateRange?: string;
    /** Output format for scheduled reports */
    outputFormat: 'PDF' | 'CSV' | 'XML';
    /** Report delivery method */
    deliveryMethod: 'DOWNLOAD' | 'EMAIL' | 'FTP';
    /** Delivery configuration */
    deliveryConfig?: {
      /** Email delivery configuration */
      email?: {
        recipients: string[];
        subject: string;
        body: string;
      };
      /** FTP delivery configuration */
      ftp?: {
        server: string;
        directory: string;
        credentials: string;
      };
    };
  };
}

/**
 * Report Validation Schema Interface
 * Comprehensive validation rules for CORPT00 report parameters form
 * Extends FormValidationSchema with report-specific validation logic
 * 
 * Implements BMS field validation patterns:
 * - Radio button selection validation (one of three report types)
 * - Date range validation with MM/DD/YYYY format
 * - Cross-field validation for custom date ranges
 * - Confirmation field validation (Y/N values only)
 * - Business rule validation (end date after start date)
 */
export interface ReportValidationSchema extends FormValidationSchema<ReportParametersData> {
  /**
   * Report type selection validation
   * Ensures exactly one report type is selected (MONTHLY, YEARLY, or CUSTOM)
   * Maps to BMS radio button validation logic from CORPT00.bms
   */
  reportTypeValidation: {
    /** Validates that exactly one report type radio button is selected */
    validateSingleSelection: (data: ReportParametersData) => boolean | string;
    /** Error message for invalid report type selection */
    errorMessage: string;
  };
  
  /**
   * Date range validation for custom reports
   * Validates MM/DD/YYYY format and logical date ranges
   * Implements BMS NUM attribute validation for date fields
   */
  dateRangeValidation: {
    /** Validates start date format and value ranges */
    validateStartDate: (mm: string, dd: string, yyyy: string) => boolean | string;
    /** Validates end date format and value ranges */
    validateEndDate: (mm: string, dd: string, yyyy: string) => boolean | string;
    /** Validates that end date is after start date */
    validateDateSequence: (startDate: string, endDate: string) => boolean | string;
    /** Date format validation regex patterns */
    patterns: {
      month: RegExp;
      day: RegExp;
      year: RegExp;
    };
  };
  
  /**
   * Confirmation field validation
   * Validates Y/N confirmation input matching BMS behavior
   * Maps to CONFIRM field with UNPROT attribute from CORPT00.bms
   */
  confirmationValidation: {
    /** Validates confirmation field accepts only Y or N values */
    validateConfirmation: (value: string) => boolean | string;
    /** Valid confirmation values */
    validValues: ['Y', 'N'];
    /** Error message for invalid confirmation */
    errorMessage: string;
  };
  
  /**
   * Custom date field conditional validation
   * Validates date fields only when CUSTOM report type is selected
   * Implements conditional validation based on report type selection
   */
  customDateValidation: {
    /** Determines if custom date fields should be validated */
    isCustomDateRequired: (data: ReportParametersData) => boolean;
    /** Validates all custom date fields when required */
    validateCustomDateFields: (data: ReportParametersData) => boolean | string;
    /** Error message for missing custom date fields */
    errorMessage: string;
  };
  
  /**
   * Cross-field validation rules
   * Implements complex business rules across multiple form fields
   * Ensures data consistency and business logic compliance
   */
  crossFieldValidation: {
    /** Validates date range consistency when custom dates are provided */
    validateDateRangeConsistency: (data: ReportParametersData) => boolean | string;
    /** Validates that future dates are not selected for historical reports */
    validateHistoricalDateConstraints: (data: ReportParametersData) => boolean | string;
    /** Validates maximum date range limits (e.g., no more than 2 years) */
    validateDateRangeLimits: (data: ReportParametersData) => boolean | string;
    /** Error messages for cross-field validation failures */
    errorMessages: {
      dateRangeConsistency: string;
      historicalDateConstraints: string;
      dateRangeLimits: string;
    };
  };
}