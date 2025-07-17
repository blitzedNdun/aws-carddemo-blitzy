/**
 * CardDemo - API TypeScript Interface Definitions
 * 
 * This file contains standardized TypeScript interface definitions for REST API communication
 * between React frontend components and Spring Boot microservices. These interfaces ensure
 * consistent request/response structures while mapping original COMMAREA status codes and
 * BMS screen metadata to modern HTTP API patterns.
 * 
 * Key Features:
 * - Generic ApiResponse<T> wrapper for all 18 REST endpoint interactions
 * - ErrorResponse interface mapping COMMAREA status fields to HTTP response structures  
 * - PaginatedResponse<T> for card list and transaction history pagination support
 * - Business status code mapping from original mainframe transaction responses
 * - Preservation of BMS screen header metadata for UI consistency
 * 
 * Transformation Notes:
 * - Maintains exact functional equivalence with original CICS transaction processing
 * - Maps COMMAREA status codes to HTTP status patterns for proper error handling
 * - Preserves BMS screen header fields (trnname, pgmname, etc.) for audit and debugging
 * - Supports pagination patterns from original transaction list displays (COTRN00.bms)
 */

import { BaseScreenData } from './CommonTypes';

// ===================================================================
// BUSINESS STATUS CODE MAPPING - COMMAREA to HTTP Response Patterns
// ===================================================================

/**
 * Business Status Code Type
 * 
 * Maps original COBOL COMMAREA status codes to TypeScript literals for consistent
 * business logic response handling. Preserves exact status code values from 
 * mainframe transaction processing while enabling type-safe HTTP responses.
 * 
 * Based on analysis of COBOL programs and COMMAREA status field patterns:
 * - '00': Successful completion (HTTP 200)
 * - '01': Warning condition with data returned (HTTP 200 with warning)
 * - '02': Validation error or business rule violation (HTTP 400)
 * - '03': Resource not found (HTTP 404)
 * - '04': Unauthorized access or security violation (HTTP 403)
 * - '05': System error or processing failure (HTTP 500)
 * - '06': Service temporarily unavailable (HTTP 503)
 * - '99': Unexpected error condition (HTTP 500)
 */
export type BusinessStatusCode = 
  | '00'  // Success - Operation completed successfully
  | '01'  // Warning - Operation completed with warnings
  | '02'  // Validation Error - Business rule violation or invalid data
  | '03'  // Not Found - Requested resource does not exist
  | '04'  // Forbidden - Insufficient privileges or security violation
  | '05'  // Server Error - System error during processing
  | '06'  // Service Unavailable - Temporary service interruption
  | '99'; // Unknown Error - Unexpected error condition

// ===================================================================
// CORE API INTERFACE DEFINITIONS
// ===================================================================

/**
 * API Request Headers Interface
 * 
 * Standardized HTTP headers for all REST API requests to Spring Boot microservices.
 * Provides security, tracing, and version control for consistent API communication.
 * Replaces CICS transaction header information with modern HTTP header patterns.
 */
export interface ApiRequestHeaders {
  /**
   * Authorization header - JWT token for Spring Security authentication
   * Replaces RACF user identification from original CICS environment
   */
  authorization?: string;
  
  /**
   * Correlation ID - Unique identifier for request tracing across microservices
   * Enables distributed tracing equivalent to CICS transaction ID tracking
   */
  correlationId: string;
  
  /**
   * API version - Semantic version for endpoint compatibility management
   * Supports API evolution while maintaining backward compatibility
   */
  apiVersion: string;
  
  /**
   * Content type - MIME type for request payload
   * Standard HTTP content type specification for JSON payloads
   */
  contentType: string;
  
  /**
   * User agent - Client application identification
   * Identifies the React frontend version for support and debugging
   */
  userAgent?: string;
}

/**
 * Generic API Request Interface
 * 
 * Standardized request structure for all Spring Boot REST API calls.
 * Provides consistent request format with security, tracing, and session management.
 * Maps CICS COMMAREA input patterns to modern HTTP request structure.
 */
export interface ApiRequest<T = any> {
  /**
   * Request headers - HTTP headers for security and tracing
   * Contains authentication, correlation, and version information
   */
  headers: ApiRequestHeaders;
  
  /**
   * Correlation ID - Request tracking identifier
   * Enables end-to-end transaction tracing across microservices
   */
  correlationId: string;
  
  /**
   * Request timestamp - When the request was initiated
   * Provides request timing for performance monitoring and debugging
   */
  timestamp: string;
  
  /**
   * Session ID - User session identifier for pseudo-conversational flow
   * Replaces CICS pseudo-conversational session management with Redis-backed sessions
   */
  sessionId?: string;
  
  /**
   * Request data - Typed payload containing business data
   * Generic type T allows for strongly-typed request payloads per endpoint
   */
  data?: T;
}

/**
 * Generic API Response Interface
 * 
 * Standardized response wrapper for all Spring Boot REST API endpoints.
 * Preserves BMS screen header metadata while providing modern HTTP response structure.
 * Maps COMMAREA response patterns to consistent JSON response format.
 * 
 * Generic type T allows for strongly-typed response data per endpoint while maintaining
 * consistent response envelope structure across all 18 REST endpoint interactions.
 */
export interface ApiResponse<T = any> {
  /**
   * Response data - Typed payload containing business data
   * Generic type T provides compile-time type safety for endpoint-specific responses
   */
  data?: T;
  
  /**
   * Business status code - COMMAREA-equivalent status from business logic processing
   * Maps original COBOL status codes to TypeScript literals for consistent error handling
   */
  status: BusinessStatusCode;
  
  /**
   * Status message - Human-readable description of the response status
   * Provides clear status communication equivalent to original CICS message patterns
   */
  message: string;
  
  /**
   * Response timestamp - When the response was generated
   * ISO 8601 formatted timestamp for consistent time representation
   */
  timestamp: string;
  
  /**
   * Correlation ID - Request tracking identifier for distributed tracing
   * Links response back to originating request across microservice boundaries
   */
  correlationId: string;
}

/**
 * Error Response Interface
 * 
 * Specialized interface for error responses mapping COMMAREA error fields to HTTP error structure.
 * Provides comprehensive error information for proper client-side error handling and user feedback.
 * Maintains compatibility with original CICS error message patterns while enabling modern error processing.
 */
export interface ErrorResponse {
  /**
   * Error object - Structured error information
   * Contains detailed error data for troubleshooting and user guidance
   */
  error: {
    /**
     * Error code - System error code for technical reference
     * Maps to original COBOL error codes for compatibility and debugging
     */
    code: string;
    
    /**
     * Error type - Category of error for handling logic
     * Enables client-side error categorization and appropriate response
     */
    type: 'VALIDATION' | 'BUSINESS' | 'SYSTEM' | 'SECURITY' | 'NOT_FOUND';
    
    /**
     * Field errors - Field-specific validation errors
     * Enables precise error highlighting in React form components
     */
    fieldErrors?: Array<{
      field: string;
      message: string;
      code: string;
    }>;
  };
  
  /**
   * Business status code - Maps to COMMAREA error status codes
   * Provides business logic error classification equivalent to original processing
   */
  code: BusinessStatusCode;
  
  /**
   * Error message - User-friendly error description
   * Human-readable error text for display in React components
   */
  message: string;
  
  /**
   * Error details - Additional context information
   * Provides supplementary error information for debugging and user guidance
   */
  details?: string;
  
  /**
   * Error timestamp - When the error occurred
   * ISO 8601 formatted timestamp for error tracking and audit purposes
   */
  timestamp: string;
}

/**
 * Pagination Info Interface
 * 
 * Pagination metadata for paginated API responses supporting card list and transaction history.
 * Maps original BMS pagination patterns (like COTRN00.bms PAGENUM field) to modern pagination structure.
 * Provides complete pagination state for React component navigation controls.
 */
export interface PaginationInfo {
  /**
   * Current page number - Zero-based page index
   * Indicates the currently displayed page in the result set
   */
  currentPage: number;
  
  /**
   * Total pages - Total number of pages available
   * Enables pagination navigation control rendering
   */
  totalPages: number;
  
  /**
   * Page size - Number of records per page
   * Configurable page size for performance optimization
   */
  pageSize: number;
  
  /**
   * Total records - Total number of records in the complete result set
   * Provides result set size information for user awareness
   */
  totalRecords: number;
  
  /**
   * Has next page - Whether additional pages are available
   * Boolean flag for next page navigation control state
   */
  hasNext: boolean;
  
  /**
   * Has previous page - Whether previous pages are available
   * Boolean flag for previous page navigation control state
   */
  hasPrevious: boolean;
}

/**
 * Paginated Response Interface
 * 
 * Specialized response interface for paginated data endpoints like card lists and transaction history.
 * Extends the standard ApiResponse with pagination metadata while preserving BMS screen header information.
 * Maps original transaction list display patterns (COTRN00.bms) to modern paginated API responses.
 * 
 * Generic type T allows for strongly-typed paginated data (e.g., Card[], Transaction[]) while maintaining
 * consistent pagination structure across all paginated endpoints.
 */
export interface PaginatedResponse<T = any> {
  /**
   * Paginated data array - Array of typed business objects for current page
   * Generic type T provides compile-time type safety for paginated content
   */
  data: T[];
  
  /**
   * Pagination metadata - Complete pagination state information
   * Provides all necessary data for React pagination component rendering
   */
  pagination: PaginationInfo;
  
  /**
   * Business status code - COMMAREA-equivalent status from pagination processing
   * Maps original COBOL status codes for consistent pagination error handling
   */
  status: BusinessStatusCode;
  
  /**
   * Status message - Human-readable description of the pagination response status
   * Provides clear status communication for pagination operations
   */
  message: string;
  
  /**
   * Response timestamp - When the paginated response was generated
   * ISO 8601 formatted timestamp for consistent time representation
   */
  timestamp: string;
  
  /**
   * Correlation ID - Request tracking identifier for paginated request tracing
   * Links paginated response back to originating request for distributed tracing
   */
  correlationId: string;
}

// ===================================================================
// ENHANCED API INTERFACES WITH BMS SCREEN METADATA
// ===================================================================

/**
 * Extended API Response Interface with BMS Screen Data
 * 
 * Enhanced response interface that includes BMS screen header metadata for maintaining
 * UI consistency with original 3270 terminal display patterns. This interface ensures
 * that React components receive the same header information (transaction name, program name,
 * date/time, titles) that was displayed in the original BMS screens.
 * 
 * Used for endpoints that need to preserve complete screen context compatibility,
 * particularly for main navigation and form display screens.
 */
export interface ApiResponseWithScreenData<T = any> extends ApiResponse<T> {
  /**
   * BMS screen header data - Common header fields from BaseScreenData
   * Preserves original screen header information for UI consistency and audit trail
   * Maps directly to BMS header fields: trnname, pgmname, curdate, curtime, title01, title02
   */
  screenData: Pick<BaseScreenData, 'trnname' | 'pgmname' | 'curdate' | 'curtime' | 'title01' | 'title02'>;
  
  /**
   * Screen state metadata - Additional screen state information
   * Provides extended screen context for complex form interactions
   */
  screenMeta?: {
    /**
     * Screen mode - Current operational mode of the screen
     * Controls field protection and available operations
     */
    mode: 'VIEW' | 'EDIT' | 'ADD' | 'DELETE';
    
    /**
     * Form validation state - Overall form validation status
     * Indicates whether the form data meets all business rules
     */
    isValid: boolean;
    
    /**
     * Change indicator - Whether screen data has been modified
     * Enables change tracking and unsaved data warnings
     */
    hasChanges: boolean;
  };
}

/**
 * Extended Paginated Response Interface with BMS Screen Data
 * 
 * Enhanced paginated response interface that includes BMS screen header metadata for
 * maintaining UI consistency in paginated displays like transaction lists and card listings.
 * Preserves original BMS pagination patterns while providing modern pagination features.
 */
export interface PaginatedResponseWithScreenData<T = any> extends PaginatedResponse<T> {
  /**
   * BMS screen header data - Common header fields from BaseScreenData
   * Preserves original screen header information for paginated display consistency
   * Maps directly to BMS header fields: trnname, pgmname, curdate, curtime, title01, title02
   */
  screenData: Pick<BaseScreenData, 'trnname' | 'pgmname' | 'curdate' | 'curtime' | 'title01' | 'title02'>;
  
  /**
   * Search criteria - Current search/filter parameters applied to the paginated data
   * Enables search state preservation across pagination operations
   */
  searchCriteria?: {
    /**
     * Search term - Text search criteria applied to the result set
     * Maps to original BMS search field patterns (e.g., TRNIDIN from COTRN00.bms)
     */
    searchTerm?: string;
    
    /**
     * Filter parameters - Additional filtering criteria as key-value pairs
     * Supports complex filtering scenarios for paginated data
     */
    filters?: Record<string, any>;
    
    /**
     * Sort criteria - Sorting configuration for the result set
     * Defines ordering applied to paginated data
     */
    sortBy?: string;
    sortOrder?: 'ASC' | 'DESC';
  };
}