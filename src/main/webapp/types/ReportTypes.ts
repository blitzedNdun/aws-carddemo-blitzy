/**
 * ReportTypes.ts
 * 
 * TypeScript interface definitions for report generation screen (CORPT00) including 
 * report parameters, batch job configuration, result metadata, and scheduled 
 * generation types for the CardDemo system.
 * 
 * This file provides comprehensive type definitions for the report generation 
 * functionality that replaces traditional mainframe report processing with 
 * modern Spring Batch job execution and React-based user interface components.
 * 
 * Key Features:
 * - BMS CORPT00 screen mapping to React component props
 * - Spring Batch job parameter type definitions
 * - Report result metadata and download management
 * - Scheduled report generation configuration
 * - Form validation schema for report parameters
 * - Integration with CommonTypes for field attributes
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { FormFieldAttributes } from './CommonTypes';
import { FormValidationSchema } from './ValidationTypes';

/**
 * Report type enumeration matching business requirements
 * Maps to the three report generation options from CORPT00 BMS screen
 */
export type ReportType = 
  | 'MONTHLY'     // Monthly report for current month
  | 'YEARLY'      // Yearly report for current year
  | 'CUSTOM';     // Custom date range report

/**
 * Report generation status enumeration for tracking batch job execution
 * Used to monitor Spring Batch job lifecycle and user feedback
 */
export type ReportGenerationStatus = 
  | 'PENDING'     // Report generation queued
  | 'RUNNING'     // Spring Batch job executing
  | 'COMPLETED'   // Report successfully generated
  | 'FAILED'      // Report generation failed
  | 'CANCELLED';  // Report generation cancelled

/**
 * Report Parameters Data Interface
 * 
 * Maps directly to the CORPT00 BMS screen fields, preserving exact field
 * structure and validation requirements from the original mainframe screen.
 * This interface represents the user input data collected from the React
 * component that replaces the BMS mapset.
 */
export interface ReportParametersData {
  /** Monthly report selection flag (equivalent to MONTHLY field, PIC X(1)) */
  monthly: string;
  
  /** Yearly report selection flag (equivalent to YEARLY field, PIC X(1)) */
  yearly: string;
  
  /** Custom date range selection flag (equivalent to CUSTOM field, PIC X(1)) */
  custom: string;
  
  /** Start date month (equivalent to SDTMM field, PIC X(2)) */
  startDateMM: string;
  
  /** Start date day (equivalent to SDTDD field, PIC X(2)) */
  startDateDD: string;
  
  /** Start date year (equivalent to SDTYYYY field, PIC X(4)) */
  startDateYYYY: string;
  
  /** End date month (equivalent to EDTMM field, PIC X(2)) */
  endDateMM: string;
  
  /** End date day (equivalent to EDTDD field, PIC X(2)) */
  endDateDD: string;
  
  /** End date year (equivalent to EDTYYYY field, PIC X(4)) */
  endDateYYYY: string;
  
  /** Confirmation flag (equivalent to CONFIRM field, PIC X(1)) */
  confirmation: string;
  
  /** Error message display (equivalent to ERRMSG field, PIC X(78)) */
  errorMessage: string;
  
  /** Field attributes for BMS-equivalent form rendering and validation */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Report Generation Request Interface
 * 
 * Represents the structured request data sent to the Spring Batch job launcher
 * for report generation processing. This interface bridges the React frontend
 * form data with the backend batch processing requirements.
 */
export interface ReportGenerationRequest {
  /** Type of report to generate (monthly, yearly, or custom) */
  reportType: ReportType;
  
  /** Report start date in ISO format (for custom reports) */
  startDate: string;
  
  /** Report end date in ISO format (for custom reports) */
  endDate: string;
  
  /** Output format for the generated report (PDF, Excel, CSV) */
  outputFormat: 'PDF' | 'EXCEL' | 'CSV';
  
  /** Spring Batch job parameters for execution configuration */
  batchJobParameters: Record<string, any>;
  
  /** Unique request identifier for tracking and correlation */
  requestId: string;
  
  /** User ID of the requestor for audit and authorization */
  userId: string;
  
  /** Correlation ID for distributed tracing and logging */
  correlationId: string;
}

/**
 * Report Result Data Interface
 * 
 * Contains metadata and access information for successfully generated reports.
 * This interface provides all necessary information for users to download,
 * view, and manage their generated reports through the React frontend.
 */
export interface ReportResultData {
  /** Unique identifier for the generated report */
  reportId: string;
  
  /** Type of report that was generated */
  reportType: ReportType;
  
  /** Current status of the report generation process */
  generationStatus: ReportGenerationStatus;
  
  /** Secure URL for downloading the generated report file */
  downloadUrl: string;
  
  /** Original filename of the generated report */
  fileName: string;
  
  /** File size in bytes for download progress indication */
  fileSize: number;
  
  /** Timestamp when the report was created */
  createdDate: string;
  
  /** Timestamp when the report download link expires */
  expirationDate: string;
  
  /** Error message if report generation failed */
  errorMessage?: string;
  
  /** Additional metadata about the report (row count, processing time, etc.) */
  metadata: Record<string, any>;
}

/**
 * Report Schedule Data Interface
 * 
 * Configuration interface for automated report generation scheduling.
 * Supports recurring report generation through cron expressions and
 * Spring Batch job scheduling integration.
 */
export interface ReportScheduleData {
  /** Unique identifier for the scheduled report */
  scheduleId: string;
  
  /** Type of report to generate on schedule */
  reportType: ReportType;
  
  /** Frequency of report generation (DAILY, WEEKLY, MONTHLY, CUSTOM) */
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';
  
  /** Cron expression for custom scheduling (when frequency is CUSTOM) */
  cronExpression?: string;
  
  /** Next scheduled execution date and time */
  nextRunDate: string;
  
  /** Last execution date and time */
  lastRunDate?: string;
  
  /** Spring Batch job configuration for scheduled execution */
  batchJobConfig: Record<string, any>;
  
  /** Whether the schedule is currently active */
  isActive: boolean;
  
  /** User ID who created the schedule */
  userId: string;
  
  /** Report parameters to use for each scheduled execution */
  parameters: ReportParametersData;
}

/**
 * Report Validation Schema Interface
 * 
 * Comprehensive validation schema for report parameter forms using the
 * ValidationTypes framework. This interface ensures all report parameters
 * are properly validated before submission to the batch processing system.
 */
export interface ReportValidationSchema {
  /** Validation rules for report type selection */
  reportTypeValidation: {
    /** Ensures exactly one report type is selected */
    required: boolean;
    /** Custom validation message for report type selection */
    message: string;
    /** Validation function to check mutual exclusivity */
    validator: (monthly: string, yearly: string, custom: string) => boolean | string;
  };
  
  /** Validation rules for date range selection */
  dateRangeValidation: {
    /** Validates date format and range for custom reports */
    required: boolean;
    /** Date format validation pattern (MM/DD/YYYY) */
    pattern: string;
    /** Validation message for date range errors */
    message: string;
    /** Custom date range validation function */
    validator: (startDate: string, endDate: string) => boolean | string;
  };
  
  /** Validation rules for confirmation field */
  confirmationValidation: {
    /** Requires confirmation before report generation */
    required: boolean;
    /** Valid confirmation values (Y/N) */
    validValues: string[];
    /** Confirmation validation message */
    message: string;
  };
  
  /** Validation rules for custom date fields */
  customDateValidation: {
    /** Month validation (01-12) */
    monthValidation: {
      pattern: string;
      message: string;
    };
    /** Day validation (01-31) */
    dayValidation: {
      pattern: string;
      message: string;
    };
    /** Year validation (1900-2099) */
    yearValidation: {
      pattern: string;
      message: string;
    };
  };
  
  /** Cross-field validation rules */
  crossFieldValidation: {
    /** Validates that custom date fields are provided when custom report is selected */
    customDateRequired: (reportType: ReportType, startDate: string, endDate: string) => boolean | string;
    /** Validates that start date is before end date */
    dateOrderValidation: (startDate: string, endDate: string) => boolean | string;
    /** Validates that date range is within business rules (e.g., max 1 year) */
    dateRangeLimit: (startDate: string, endDate: string) => boolean | string;
  };
}

/**
 * Utility type for report parameter form state management
 * Used by React components to track form state and validation results
 */
export type ReportFormState = {
  /** Current form data */
  data: ReportParametersData;
  /** Validation errors by field name */
  errors: Record<string, string>;
  /** Whether form is currently submitting */
  isSubmitting: boolean;
  /** Whether form has been touched by user */
  isTouched: boolean;
  /** Whether form is valid for submission */
  isValid: boolean;
};

/**
 * Utility type for report generation progress tracking
 * Used to provide real-time feedback during batch job execution
 */
export type ReportGenerationProgress = {
  /** Current generation status */
  status: ReportGenerationStatus;
  /** Progress percentage (0-100) */
  progress: number;
  /** Current processing message */
  message: string;
  /** Estimated time remaining in seconds */
  estimatedTimeRemaining?: number;
  /** Number of records processed */
  recordsProcessed?: number;
  /** Total number of records to process */
  totalRecords?: number;
};

/**
 * Configuration constants for report generation
 * These constants define system limits and default values
 */
export const REPORT_CONSTANTS = {
  /** Maximum date range in days for custom reports */
  MAX_DATE_RANGE_DAYS: 366,
  
  /** Default report expiration time in hours */
  DEFAULT_EXPIRATION_HOURS: 24,
  
  /** Maximum file size for report downloads in bytes */
  MAX_FILE_SIZE_BYTES: 50 * 1024 * 1024, // 50MB
  
  /** Supported output formats */
  OUTPUT_FORMATS: ['PDF', 'EXCEL', 'CSV'] as const,
  
  /** Default batch job timeout in minutes */
  DEFAULT_JOB_TIMEOUT_MINUTES: 30,
  
  /** Report refresh interval in seconds for progress updates */
  PROGRESS_REFRESH_INTERVAL_SECONDS: 5,
} as const;

/**
 * Export all types for external consumption
 * Note: Types are already exported above with their declarations
 * This section is for documentation purposes only
 */