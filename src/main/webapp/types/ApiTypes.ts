/**
 * ApiTypes.ts
 * 
 * TypeScript interface definitions for REST API communication including standardized 
 * request/response structures, error handling, pagination support, and business status 
 * code mapping from original COMMAREA patterns.
 * 
 * This file provides comprehensive API type definitions that enable consistent communication
 * between React frontend components and Spring Boot microservices through Spring Cloud Gateway.
 * All interfaces preserve COMMAREA-equivalent status field semantics while implementing
 * modern REST API patterns with JWT authentication and distributed session management.
 * 
 * The API structure replaces traditional COBOL COMMAREA data transfer with JSON DTOs
 * while maintaining identical business status codes, error categorization, and response
 * patterns from the original CICS transaction processing environment.
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/COMMAREA → React/Spring Boot/REST
 */

import { BaseScreenData } from './CommonTypes';

/**
 * Business Status Code Type Definition
 * 
 * Maps traditional COBOL/CICS COMMAREA status codes to modern API responses.
 * These codes maintain exact compatibility with original mainframe transaction
 * processing status reporting while enabling standardized HTTP response handling.
 * 
 * Status Code Mapping from Original COMMAREA Patterns:
 * - '00' = Successful operation completion
 * - '01' = Warning condition with successful completion
 * - '10' = Informational message (data found with conditions)
 * - '96' = End of data reached (for pagination and search)
 * - '97' = Duplicate record detected
 * - '98' = Invalid data format or constraint violation
 * - '99' = System error or unexpected failure
 */
export type BusinessStatusCode = 
  | '00'  // Success - operation completed successfully
  | '01'  // Warning - operation completed with advisory message
  | '10'  // Info - data retrieved with informational notice
  | '96'  // End of data - pagination or search reached end
  | '97'  // Duplicate - record already exists
  | '98'  // Invalid data - validation or format error
  | '99'; // System error - internal processing failure

/**
 * Standard API Request Headers Interface
 * 
 * Defines consistent header structure for all REST API requests including
 * JWT authentication, correlation tracking, API versioning, and content
 * type specification required for microservice communication.
 */
export interface ApiRequestHeaders {
  /** JWT bearer token for authentication (required for all authenticated endpoints) */
  authorization: string;
  
  /** Unique correlation ID for distributed tracing and request tracking */
  correlationId: string;
  
  /** API version for backward compatibility and contract management */
  apiVersion: string;
  
  /** Content type specification (typically 'application/json') */
  contentType: string;
  
  /** User agent identification for client tracking and analytics */
  userAgent: string;
}

/**
 * Generic API Request Interface
 * 
 * Standardized request wrapper that encapsulates all REST API calls with
 * consistent header management, correlation tracking, session identification,
 * and data payload structure for reliable microservice communication.
 */
export interface ApiRequest<T = any> {
  /** Request headers including authentication and metadata */
  headers: ApiRequestHeaders;
  
  /** Unique correlation ID for distributed tracing (duplicated for convenience) */
  correlationId: string;
  
  /** Request timestamp for audit and performance tracking */
  timestamp: Date;
  
  /** Session identifier for Redis-backed session management */
  sessionId: string;
  
  /** Request payload data (generic type for flexibility) */
  data: T;
}

/**
 * Generic API Response Interface
 * 
 * Standardized response wrapper for all REST API endpoints providing consistent
 * data structure, business status reporting, messaging, and correlation tracking.
 * This interface replaces COMMAREA response patterns while maintaining identical
 * business semantics and error handling capabilities.
 * 
 * Includes BaseScreenData members for maintaining BMS header field compatibility
 * across all screen-based API responses, ensuring seamless integration with
 * React components that expect traditional screen metadata.
 */
export interface ApiResponse<T = any> {
  /** Response payload data (generic type for flexibility) */
  data: T;
  
  /** BMS header fields for screen-based responses (optional) */
  screenData?: {
    /** Transaction name (4 characters) - equivalent to BMS TRNNAME field */
    trnname: string;
    
    /** Program name (8 characters) - equivalent to BMS PGMNAME field */
    pgmname: string;
    
    /** Current date (8 characters, format: mm/dd/yy) - equivalent to BMS CURDATE field */
    curdate: string;
    
    /** Current time (9 characters, format: hh:mm:ss) - equivalent to BMS CURTIME field */
    curtime: string;
    
    /** Primary title line (40 characters) - equivalent to BMS TITLE01 field */
    title01: string;
    
    /** Secondary title line (40 characters) - equivalent to BMS TITLE02 field */
    title02: string;
  };
  
  /** Business status code mapping COMMAREA status patterns */
  status: BusinessStatusCode;
  
  /** Human-readable status message for user display */
  message: string;
  
  /** Response timestamp for audit and performance tracking */
  timestamp: Date;
  
  /** Correlation ID matching the original request for tracing */
  correlationId: string;
}

/**
 * Error Response Interface
 * 
 * Comprehensive error response structure that maps COMMAREA error fields
 * to modern HTTP error responses with detailed error categorization,
 * field-level validation messages, and correlation tracking for debugging.
 * 
 * This interface ensures consistent error handling across all microservices
 * while preserving the detailed error reporting capabilities of the original
 * CICS transaction processing environment.
 */
export interface ErrorResponse {
  /** Structured error information object */
  error: {
    /** Error type classification (validation, authentication, system, etc.) */
    type: 'validation' | 'authentication' | 'authorization' | 'not_found' | 'conflict' | 'system' | 'timeout';
    
    /** Technical error code for logging and debugging */
    code: string;
    
    /** Detailed error description for developers */
    details: string;
    
    /** Field-specific validation errors (for form validation) */
    fieldErrors?: Array<{
      /** Field name that caused the error */
      field: string;
      
      /** Field-specific error message */
      message: string;
      
      /** Rejected value that caused the error */
      rejectedValue?: any;
    }>;
    
    /** Stack trace or additional debug information (development only) */
    debugInfo?: string;
  };
  
  /** Business error code mapping to COMMAREA status patterns */
  code: BusinessStatusCode;
  
  /** User-friendly error message for display */
  message: string;
  
  /** Additional error context and troubleshooting information */
  details: string;
  
  /** Error timestamp for audit and correlation */
  timestamp: Date;
}

/**
 * Pagination Information Interface
 * 
 * Comprehensive pagination metadata for large dataset handling including
 * current page tracking, total record counts, navigation state, and page
 * size configuration for optimal data loading and user experience.
 */
export interface PaginationInfo {
  /** Current page number (1-based indexing) */
  currentPage: number;
  
  /** Total number of available pages */
  totalPages: number;
  
  /** Number of records per page */
  pageSize: number;
  
  /** Total number of records available */
  totalRecords: number;
  
  /** Indicates if next page is available */
  hasNext: boolean;
  
  /** Indicates if previous page is available */
  hasPrevious: boolean;
}

/**
 * Paginated API Response Interface
 * 
 * Specialized response wrapper for paginated data sets such as transaction
 * history, card listings, and account searches. Extends the standard API
 * response with comprehensive pagination metadata and navigation capabilities.
 * 
 * This interface supports efficient data loading patterns and enables
 * implementation of pagination controls equivalent to F7/F8 forward/backward
 * navigation from the original BMS screen interfaces.
 */
export interface PaginatedResponse<T = any> extends ApiResponse<T[]> {
  /** Array of data items for the current page */
  data: T[];
  
  /** Comprehensive pagination metadata */
  pagination: PaginationInfo;
}

/**
 * Common API Type Utilities and Constants
 */

/**
 * Standard HTTP Status Code Mapping to Business Status Codes
 * 
 * Provides mapping between HTTP status codes and business status codes
 * for consistent error handling and status reporting across all API endpoints.
 */
export const HTTP_TO_BUSINESS_STATUS: Record<number, BusinessStatusCode> = {
  200: '00', // OK - successful operation
  201: '00', // Created - successful creation
  202: '01', // Accepted - operation queued/processing
  204: '00', // No Content - successful deletion
  400: '98', // Bad Request - invalid data
  401: '99', // Unauthorized - authentication failure
  403: '99', // Forbidden - authorization failure
  404: '96', // Not Found - record not found
  409: '97', // Conflict - duplicate record
  422: '98', // Unprocessable Entity - validation error
  500: '99', // Internal Server Error - system error
  502: '99', // Bad Gateway - service unavailable
  503: '99', // Service Unavailable - temporary failure
  504: '99', // Gateway Timeout - timeout error
} as const;

/**
 * Default Pagination Configuration
 * 
 * Standard pagination settings matching original BMS screen display
 * patterns and optimal performance characteristics for web interfaces.
 */
export const DEFAULT_PAGINATION = {
  PAGE_SIZE: 10,           // Default records per page (matching BMS screen capacity)
  MAX_PAGE_SIZE: 100,      // Maximum allowed page size for performance
  DEFAULT_PAGE: 1,         // Default starting page (1-based indexing)
} as const;

/**
 * API Response Helper Functions
 */

/**
 * Creates a standardized API response with consistent structure
 * 
 * @param data Response payload data
 * @param status Business status code
 * @param message Status message
 * @param correlationId Request correlation ID
 * @returns Formatted API response
 */
export const createApiResponse = <T>(
  data: T,
  status: BusinessStatusCode = '00',
  message: string = 'Operation completed successfully',
  correlationId: string
): ApiResponse<T> => ({
  data,
  status,
  message,
  timestamp: new Date(),
  correlationId,
});

/**
 * Creates a standardized error response with consistent structure
 * 
 * @param type Error type classification
 * @param code Business error code
 * @param message User-friendly error message
 * @param details Technical error details
 * @returns Formatted error response
 */
export const createErrorResponse = (
  type: ErrorResponse['error']['type'],
  code: BusinessStatusCode,
  message: string,
  details: string
): ErrorResponse => ({
  error: {
    type,
    code: `CARD_DEMO_${code}`,
    details,
  },
  code,
  message,
  details,
  timestamp: new Date(),
});

/**
 * Creates pagination information object
 * 
 * @param currentPage Current page number
 * @param totalRecords Total number of records
 * @param pageSize Records per page
 * @returns Pagination information
 */
export const createPaginationInfo = (
  currentPage: number,
  totalRecords: number,
  pageSize: number = DEFAULT_PAGINATION.PAGE_SIZE
): PaginationInfo => {
  const totalPages = Math.ceil(totalRecords / pageSize);
  
  return {
    currentPage,
    totalPages,
    pageSize,
    totalRecords,
    hasNext: currentPage < totalPages,
    hasPrevious: currentPage > 1,
  };
};

/**
 * Type Guards for Runtime Type Checking
 */

/**
 * Type guard to check if a response is a paginated response
 * 
 * @param response API response to check
 * @returns True if response is paginated
 */
export const isPaginatedResponse = <T>(
  response: ApiResponse<T> | PaginatedResponse<T>
): response is PaginatedResponse<T> => {
  return 'pagination' in response;
};

/**
 * Type guard to check if a response is an error response
 * 
 * @param response Response to check
 * @returns True if response is an error response
 */
export const isErrorResponse = (
  response: any
): response is ErrorResponse => {
  return response && typeof response.error === 'object' && 'type' in response.error;
};

/**
 * Type guard to check if a status code indicates success
 * 
 * @param status Business status code
 * @returns True if status indicates success
 */
export const isSuccessStatus = (status: BusinessStatusCode): boolean => {
  return status === '00' || status === '01';
};

/**
 * Type guard to check if a status code indicates an error
 * 
 * @param status Business status code
 * @returns True if status indicates an error
 */
export const isErrorStatus = (status: BusinessStatusCode): boolean => {
  return !isSuccessStatus(status);
};