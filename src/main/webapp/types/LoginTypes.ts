/**
 * CardDemo - Login Screen TypeScript Interface Definitions
 * 
 * This file contains TypeScript interface definitions for the login screen (COSGN00)
 * including form data structure, validation schemas, and authentication response types 
 * that map directly from the original BMS mapset and COBOL copybook definitions.
 * 
 * Source Files:
 * - app/bms/COSGN00.bms: BMS mapset defining screen layout and field attributes
 * - app/cpy-bms/COSGN00.CPY: COBOL copybook defining data structures
 * 
 * Key Features:
 * - LoginFormData interface mapping USERID and PASSWD fields from COSGN00.bms
 * - LoginResponseData interface for JWT authentication results
 * - LoginScreenData interface for complete screen state management
 * - LoginValidationSchema interface for form validation rules
 * - LoginFieldAttributes interface for BMS field attribute mapping
 * - Authentication status types for login flow management
 * 
 * BMS Field Mapping:
 * - USERID: PIC X(8), LENGTH=8, ATTRB=(FSET,IC,NORM,UNPROT), COLOR=GREEN, POS=(19,43)
 * - PASSWD: PIC X(8), LENGTH=8, ATTRB=(DRK,FSET,UNPROT), COLOR=GREEN, POS=(20,43)
 * - ERRMSG: PIC X(78), LENGTH=78, ATTRB=(ASKIP,BRT,FSET), COLOR=RED, POS=(23,1)
 * 
 * Transformation Notes:
 * - Preserves exact field lengths and validation constraints from BMS definition
 * - Maps BMS attribute bytes to TypeScript validation schemas
 * - Maintains original terminal field positioning for UI consistency
 * - Supports JWT token-based authentication replacing RACF authentication
 */

import { ErrorMessage } from './CommonTypes';
import { ApiResponse } from './ApiTypes';

// ===================================================================
// AUTHENTICATION STATUS TYPES
// ===================================================================

/**
 * Authentication Status Type
 * 
 * Represents the current state of the authentication process.
 * Maps to original CICS transaction status patterns while supporting
 * modern JWT token-based authentication flow.
 */
export type AuthenticationStatus = 
  | 'IDLE'           // Initial state - no authentication attempt
  | 'AUTHENTICATING' // Authentication in progress
  | 'AUTHENTICATED'  // Successfully authenticated
  | 'FAILED'         // Authentication failed
  | 'EXPIRED'        // JWT token expired
  | 'LOCKED'         // Account locked due to failed attempts
  | 'DISABLED';      // Account disabled by administrator

// ===================================================================
// FORM DATA INTERFACES
// ===================================================================

/**
 * Login Form Data Interface
 * 
 * Maps directly to BMS COSGN00 mapset input fields preserving exact field
 * characteristics and validation constraints from the original implementation.
 * 
 * Field Mapping:
 * - userid: Maps to USERID field (PIC X(8), LENGTH=8, UNPROT)
 * - passwd: Maps to PASSWD field (PIC X(8), LENGTH=8, UNPROT, DRK)
 * - fieldAttributes: BMS field attribute metadata for UI rendering
 */
export interface LoginFormData {
  /**
   * User ID field - Maps to BMS USERID field
   * COBOL: USERIDI PIC X(8)
   * BMS: LENGTH=8, ATTRB=(FSET,IC,NORM,UNPROT), COLOR=GREEN
   * Validation: Required, 8 character maximum, alphanumeric
   */
  userid: string;
  
  /**
   * Password field - Maps to BMS PASSWD field  
   * COBOL: PASSWDI PIC X(8)
   * BMS: LENGTH=8, ATTRB=(DRK,FSET,UNPROT), COLOR=GREEN
   * Validation: Required, 8 character maximum, masked display
   */
  passwd: string;
  
  /**
   * Field attributes metadata - BMS field attribute information
   * Contains field-specific attributes for UI rendering and validation
   */
  fieldAttributes: LoginFieldAttributes;
}

/**
 * Login Response Data Interface
 * 
 * Defines the structure of successful authentication responses from the
 * AuthenticationService REST endpoint. Maps RACF authentication results
 * to modern JWT token-based authentication patterns.
 */
export interface LoginResponseData {
  /**
   * JWT access token - Primary authentication token
   * Used for subsequent API requests to Spring Boot microservices
   */
  token: string;
  
  /**
   * JWT refresh token - Token refresh capability
   * Used to obtain new access tokens when current token expires
   */
  refreshToken: string;
  
  /**
   * Token type - Authentication token type specification
   * Standard OAuth2 token type (typically "Bearer")
   */
  tokenType: string;
  
  /**
   * Token expiration time - Token validity period in seconds
   * Enables client-side token expiration handling
   */
  expiresIn: number;
  
  /**
   * User ID - Authenticated user identifier
   * Maps to original RACF user ID for audit trail consistency
   */
  userId: string;
  
  /**
   * User role - User authorization level
   * Maps to original RACF user roles (ADMIN, USER) for menu navigation
   */
  userRole: string;
  
  /**
   * Session ID - Pseudo-conversational session identifier
   * Enables session tracking equivalent to CICS pseudo-conversational processing
   */
  sessionId: string;
  
  /**
   * Authentication timestamp - When authentication occurred
   * ISO 8601 formatted timestamp for audit trail and session management
   */
  timestamp: string;
}

/**
 * Login Screen Data Interface
 * 
 * Complete screen state management interface that encompasses all data
 * required for the login screen component. Includes form data, validation
 * state, error handling, and loading indicators.
 */
export interface LoginScreenData {
  /**
   * Base screen data - Common BMS header fields
   * Maps to BMS common header fields (TRNNAME, PGMNAME, CURDATE, CURTIME, etc.)
   */
  baseScreenData: {
    trnname: string;    // Transaction name: 'CC00'
    pgmname: string;    // Program name: 'COSGN00C'
    curdate: string;    // Current date: 'mm/dd/yy'
    curtime: string;    // Current time: 'hh:mm:ss'
    title01: string;    // Screen title line 1
    title02: string;    // Screen title line 2
  };
  
  /**
   * Form data - User credential input data
   * Contains current form field values and validation state
   */
  formData: LoginFormData;
  
  /**
   * Error message - Current error state
   * Maps to BMS ERRMSG field for error display
   */
  errorMessage?: ErrorMessage;
  
  /**
   * Success message - Positive feedback message
   * Used for successful authentication confirmation
   */
  successMessage?: string;
  
  /**
   * Loading indicator - Form submission state
   * Indicates when authentication request is in progress
   */
  isLoading: boolean;
  
  /**
   * Submitting indicator - Form submission state
   * Prevents multiple simultaneous form submissions
   */
  isSubmitting: boolean;
  
  /**
   * Validation errors - Field-specific validation errors
   * Maps validation failures to specific form fields
   */
  validationErrors: Record<string, string[]>;
  
  /**
   * Field attributes - BMS field attribute metadata
   * Contains field-specific attributes for UI rendering
   */
  fieldAttributes: LoginFieldAttributes;
}

// ===================================================================
// VALIDATION SCHEMA INTERFACES
// ===================================================================

/**
 * Login Validation Schema Interface
 * 
 * Comprehensive validation rule definitions that map BMS field validation
 * attributes to TypeScript validation schemas. Preserves exact validation
 * behavior from original BMS mapset while enabling modern form validation.
 */
export interface LoginValidationSchema {
  /**
   * User ID validation rules
   * Maps BMS USERID field validation to React Hook Form validation
   */
  useridValidation: {
    required: boolean;           // MUSTFILL equivalent
    minLength: number;           // Minimum length requirement
    maxLength: number;           // Maximum length (8 characters)
    pattern: RegExp;             // Alphanumeric pattern validation
    errorMessages: {
      required: string;          // Required field error message
      minLength: string;         // Minimum length error message
      maxLength: string;         // Maximum length error message
      pattern: string;           // Pattern validation error message
    };
  };
  
  /**
   * Password validation rules
   * Maps BMS PASSWD field validation to React Hook Form validation
   */
  passwdValidation: {
    required: boolean;           // MUSTFILL equivalent
    minLength: number;           // Minimum length requirement
    maxLength: number;           // Maximum length (8 characters)
    pattern: RegExp;             // Password pattern validation
    errorMessages: {
      required: string;          // Required field error message
      minLength: string;         // Minimum length error message
      maxLength: string;         // Maximum length error message
      pattern: string;           // Pattern validation error message
    };
  };
  
  /**
   * Form-level validation rules
   * Cross-field validation and business rule validation
   */
  formValidation: {
    validateOnChange: boolean;    // Real-time validation setting
    validateOnBlur: boolean;      // Blur event validation setting
    validateOnSubmit: boolean;    // Submit validation setting
    reValidateMode: string;       // Re-validation mode for React Hook Form
  };
  
  /**
   * Must-fill validation rules
   * Maps BMS MUSTFILL attribute to validation constraints
   */
  mustfillValidation: {
    userid: boolean;             // USERID field must-fill requirement
    passwd: boolean;             // PASSWD field must-fill requirement
    errorMessage: string;        // Must-fill error message template
  };
  
  /**
   * Length validation rules
   * Maps BMS LENGTH attribute to validation constraints
   */
  lengthValidation: {
    useridMaxLength: number;     // USERID maximum length (8)
    passwdMaxLength: number;     // PASSWD maximum length (8)
    errorMessage: string;        // Length validation error message template
  };
}

/**
 * Login Field Attributes Interface
 * 
 * Maps BMS field attributes to TypeScript interface for UI rendering.
 * Preserves original BMS field behavior while enabling modern React
 * component attribute configuration.
 */
export interface LoginFieldAttributes {
  /**
   * User ID field attributes
   * Maps BMS USERID field attributes to React component props
   */
  useridAttributes: {
    attrb: string[];             // BMS ATTRB: ['FSET', 'IC', 'NORM', 'UNPROT']
    color: string;               // BMS COLOR: 'GREEN'
    length: number;              // BMS LENGTH: 8
    pos: { row: number; column: number }; // BMS POS: (19,43)
    initial: string;             // BMS INITIAL: '' (empty for input field)
    autoFocus: boolean;          // BMS IC attribute: true
    readOnly: boolean;           // BMS UNPROT attribute: false
    required: boolean;           // MUSTFILL equivalent: true
    maxLength: number;           // Input maxLength: 8
    placeholder: string;         // Input placeholder text
    inputMode: string;           // HTML input mode: 'text'
  };
  
  /**
   * Password field attributes
   * Maps BMS PASSWD field attributes to React component props
   */
  passwdAttributes: {
    attrb: string[];             // BMS ATTRB: ['DRK', 'FSET', 'UNPROT']
    color: string;               // BMS COLOR: 'GREEN'
    length: number;              // BMS LENGTH: 8
    pos: { row: number; column: number }; // BMS POS: (20,43)
    initial: string;             // BMS INITIAL: '________'
    autoFocus: boolean;          // BMS IC attribute: false
    readOnly: boolean;           // BMS UNPROT attribute: false
    required: boolean;           // MUSTFILL equivalent: true
    maxLength: number;           // Input maxLength: 8
    placeholder: string;         // Input placeholder text
    inputMode: string;           // HTML input mode: 'text'
    type: string;                // HTML input type: 'password'
    masked: boolean;             // BMS DRK attribute: true
  };
  
  /**
   * Error message field attributes
   * Maps BMS ERRMSG field attributes to React component props
   */
  errmsgAttributes: {
    attrb: string[];             // BMS ATTRB: ['ASKIP', 'BRT', 'FSET']
    color: string;               // BMS COLOR: 'RED'
    length: number;              // BMS LENGTH: 78
    pos: { row: number; column: number }; // BMS POS: (23,1)
    readOnly: boolean;           // BMS ASKIP attribute: true
    bright: boolean;             // BMS BRT attribute: true
    maxLength: number;           // Display maxLength: 78
  };
  
  /**
   * Button attributes
   * Submit button and navigation button attributes
   */
  buttonAttributes: {
    submitButton: {
      text: string;              // Button text: 'Sign On'
      key: string;               // Function key: 'ENTER'
      disabled: boolean;         // Button disabled state
      loading: boolean;          // Loading indicator state
    };
    exitButton: {
      text: string;              // Button text: 'Exit'
      key: string;               // Function key: 'F3'
      disabled: boolean;         // Button disabled state
    };
  };
}

// ===================================================================
// TYPE ALIASES AND UTILITY TYPES
// ===================================================================

/**
 * Login API Response Type
 * 
 * Type alias for standardized API response wrapping LoginResponseData.
 * Provides consistent response structure for authentication endpoints.
 */
export type LoginApiResponse = ApiResponse<LoginResponseData>;

// ===================================================================
// EXPORTED INTERFACES SUMMARY
// ===================================================================

/**
 * Export Summary:
 * 
 * This file exports the following interfaces and types for use in
 * the LoginComponent and related authentication functionality:
 * 
 * Interfaces:
 * - LoginFormData: Form input data structure
 * - LoginResponseData: Authentication response data structure
 * - LoginScreenData: Complete screen state management
 * - LoginValidationSchema: Validation rule definitions
 * - LoginFieldAttributes: BMS field attribute mappings
 * 
 * Types:
 * - AuthenticationStatus: Authentication state enumeration
 * - LoginApiResponse: API response type alias
 * 
 * All interfaces maintain exact compatibility with original BMS mapset
 * and COBOL copybook definitions while providing modern TypeScript
 * type safety and React component integration capabilities.
 */