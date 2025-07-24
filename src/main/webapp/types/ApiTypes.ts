/**
 * ApiTypes.ts
 * 
 * TypeScript interface definitions for REST API communication including standardized 
 * request/response structures, error handling, pagination support, and business status 
 * code mapping from original COMMAREA patterns.
 * 
 * This file provides comprehensive API type definitions for all 18 REST endpoint 
 * interactions, enabling consistent communication between React frontend components 
 * and Spring Boot microservices through Spring Cloud Gateway integration.
 * 
 * Key Features:
 * - Generic ApiResponse<T> wrapper for consistent response structure
 * - COMMAREA status code mapping to HTTP response patterns
 * - Pagination support for large data sets (card lists, transaction history)
 * - Standardized error handling with detailed error information
 * - JWT authentication and request correlation support
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { BaseScreenData } from './CommonTypes';

/**
 * Business Status Code Type Definition
 * Maps original COMMAREA status values to standardized business outcome codes
 * maintaining exact functional equivalence with CICS transaction status reporting
 * 
 * Based on BMS ERRMSG field analysis and COMMAREA status patterns:
 * - SUCCESS: Normal completion equivalent to RESP(NORMAL)
 * - WARNING: Warning condition requiring user attention  
 * - ERROR: Business rule violation or validation failure
 * - SYSTEM_ERROR: Technical error requiring system intervention
 * - NOT_FOUND: Requested resource does not exist
 * - UNAUTHORIZED: Authentication or authorization failure
 * - CONFLICT: Optimistic locking or duplicate key conflict
 * - VALIDATION_ERROR: Input validation failure with field-specific details
 */
export type BusinessStatusCode = 
  | 'SUCCESS'           // Operation completed successfully (HTTP 200)
  | 'WARNING'           // Operation completed with warnings (HTTP 200)
  | 'ERROR'             // Business logic error (HTTP 400)
  | 'SYSTEM_ERROR'      // Technical system error (HTTP 500)
  | 'NOT_FOUND'         // Resource not found (HTTP 404)
  | 'UNAUTHORIZED'      // Authentication failure (HTTP 401)
  | 'FORBIDDEN'         // Authorization failure (HTTP 403)
  | 'CONFLICT'          // Data conflict or optimistic locking (HTTP 409)
  | 'VALIDATION_ERROR'  // Input validation failure (HTTP 422)
  | 'SERVICE_UNAVAILABLE'; // Circuit breaker open (HTTP 503)

/**
 * API Request Headers Interface
 * Standardizes HTTP headers for all REST API requests including JWT authentication,
 * request correlation, and API versioning support
 * 
 * Maintains compatibility with Spring Cloud Gateway request filtering and 
 * distributed tracing requirements across all microservice interactions
 */
export interface ApiRequestHeaders {
  /** JWT Bearer token for authentication and authorization */
  authorization: string;
  /** Unique correlation ID for distributed tracing and request tracking */
  correlationId: string;
  /** API version for backward compatibility (e.g., 'v1', 'v2') */
  apiVersion: string;
  /** Content type specification (typically 'application/json') */
  contentType: string;
  /** User agent identification for client tracking and analytics */
  userAgent: string;
}

/**
 * Generic API Request Interface
 * Standardizes request structure for all REST API calls with session management,
 * correlation tracking, and data payload wrapper
 * 
 * Enables consistent request handling across Spring Boot microservices while
 * maintaining pseudo-conversational behavior through Redis session tokens
 */
export interface ApiRequest<T = any> {
  /** Standardized request headers including JWT authentication */
  headers: ApiRequestHeaders;
  /** Request correlation ID for distributed tracing */
  correlationId: string;
  /** Request timestamp for audit logging and timeout management */
  timestamp: Date;
  /** Session ID for Redis-backed pseudo-conversational state */
  sessionId: string;
  /** Request payload data with generic typing for flexibility */
  data: T;
}

/**
 * Generic API Response Interface
 * Provides standardized response wrapper for all REST API endpoints maintaining
 * consistent structure across all 18 microservice interactions
 * 
 * Incorporates BaseScreenData for BMS header field compatibility while adding
 * modern API response capabilities including business status codes, correlation 
 * tracking, and timestamp information
 * 
 * @template T The data payload type for the specific response
 */
export interface ApiResponse<T = any> {
  /** Response data payload with generic typing for specific endpoint responses */
  data: T & BaseScreenData;
  /** Business status code indicating operation outcome */
  status: BusinessStatusCode;
  /** Human-readable status message for user display or logging */
  message: string;
  /** Response timestamp for audit logging and client-side caching */
  timestamp: Date;
  /** Request correlation ID for distributed tracing and debugging */
  correlationId: string;
}

/**
 * Pagination Information Interface
 * Provides comprehensive pagination metadata for large data set responses
 * including navigation state and record counting information
 * 
 * Based on COTRN00.bms pagination patterns with PAGENUM field and forward/backward
 * navigation support through F7/F8 function keys
 */
export interface PaginationInfo {
  /** Current page number (1-based indexing) */
  currentPage: number;
  /** Total number of pages available */
  totalPages: number;
  /** Number of records per page */
  pageSize: number;
  /** Total number of records across all pages */
  totalRecords: number;
  /** Indicates if there are more pages after current page */
  hasNext: boolean;
  /** Indicates if there are pages before current page */
  hasPrevious: boolean;
}

/**
 * Paginated API Response Interface
 * Extends standard ApiResponse with pagination support for endpoints returning
 * large data sets such as card lists and transaction history
 * 
 * Maintains compatibility with BMS screen pagination patterns while providing
 * modern REST API pagination capabilities with comprehensive metadata
 * 
 * @template T The data payload type for paginated records
 */
export interface PaginatedResponse<T = any> {
  /** Array of paginated data records with BaseScreenData integration */
  data: T[] & { screenData: BaseScreenData };
  /** Comprehensive pagination metadata */
  pagination: PaginationInfo;
  /** Business status code indicating operation outcome */
  status: BusinessStatusCode;
  /** Human-readable status message */
  message: string;
  /** Response timestamp for audit logging */
  timestamp: Date;
  /** Request correlation ID for distributed tracing */
  correlationId: string;
}

/**
 * Error Response Interface  
 * Provides standardized error information structure mapping COMMAREA status fields
 * to HTTP response patterns with detailed error categorization and debugging support
 * 
 * Replaces BMS ERRMSG field patterns with structured error information including
 * error codes, detailed messages, field-specific validation errors, and timestamps
 */
export interface ErrorResponse {
  /** Primary error information with categorization */
  error: {
    /** Error category for programmatic handling */
    category: 'VALIDATION' | 'BUSINESS' | 'SYSTEM' | 'SECURITY' | 'NETWORK';
    /** Error type for specific error identification */
    type: string;
    /** Severity level for error prioritization */
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  };
  /** Standardized error code for programmatic error handling */
  code: string;
  /** Human-readable error message for user display */
  message: string;
  /** Detailed error information including field-specific validation errors */
  details: {
    /** Field name associated with validation error (if applicable) */
    field?: string;
    /** Detailed error description for debugging and support */
    description: string;
    /** Suggested resolution or next steps for user */
    suggestion?: string;
    /** Additional context information for error analysis */
    context?: Record<string, any>;
  };
  /** Error timestamp for audit logging and debugging */
  timestamp: Date;
}

/**
 * Example Usage Documentation:
 * 
 * // Authentication Request
 * const loginRequest: ApiRequest<{userId: string, password: string}> = {
 *   headers: {
 *     authorization: 'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...',
 *     correlationId: 'req-12345-auth-login',
 *     apiVersion: 'v1',
 *     contentType: 'application/json',
 *     userAgent: 'CardDemo-React/1.0'
 *   },
 *   correlationId: 'req-12345-auth-login',
 *   timestamp: new Date(),
 *   sessionId: 'sess-67890-user-context',
 *   data: { userId: 'TESTUSER', password: 'password123' }
 * };
 * 
 * // Account View Response
 * const accountResponse: ApiResponse<AccountData> = {
 *   data: {
 *     accountId: '1234567890',
 *     currentBalance: 1500.75,
 *     // BaseScreenData fields
 *     trnname: 'CAVW',
 *     pgmname: 'COACTVWC',
 *     curdate: '12/15/23',
 *     curtime: '14:30:25',
 *     title01: 'CardDemo - Account View',
 *     title02: 'Account Information Display'
 *   },
 *   status: 'SUCCESS',
 *   message: 'Account information retrieved successfully',
 *   timestamp: new Date(),
 *   correlationId: 'req-12345-account-view'
 * };
 * 
 * // Transaction List with Pagination
 * const transactionListResponse: PaginatedResponse<TransactionData> = {
 *   data: [
 *     { transactionId: 'TXN001', amount: 25.50, description: 'Coffee Shop' },
 *     { transactionId: 'TXN002', amount: 150.00, description: 'Gas Station' }
 *   ] as TransactionData[] & { screenData: BaseScreenData },
 *   pagination: {
 *     currentPage: 1,
 *     totalPages: 5,
 *     pageSize: 10,
 *     totalRecords: 47,
 *     hasNext: true,
 *     hasPrevious: false
 *   },
 *   status: 'SUCCESS',
 *   message: 'Transaction history retrieved successfully',
 *   timestamp: new Date(),
 *   correlationId: 'req-12345-transaction-list'
 * };
 * 
 * // Error Response Example
 * const validationErrorResponse: ErrorResponse = {
 *   error: {
 *     category: 'VALIDATION',
 *     type: 'FIELD_VALIDATION_FAILED',
 *     severity: 'MEDIUM'
 *   },
 *   code: 'VALIDATION_ACCOUNT_ID_INVALID',
 *   message: 'Account ID must be 11 digits',
 *   details: {
 *     field: 'accountId',
 *     description: 'The provided account ID does not match the required format',
 *     suggestion: 'Please enter a valid 11-digit account number',
 *     context: { providedValue: '123', expectedFormat: '99999999999' }
 *   },
 *   timestamp: new Date()
 * };
 */