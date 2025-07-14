/**
 * LoginTypes.ts
 * 
 * TypeScript interface definitions for the COSGN00 login screen including form data structure,
 * validation schemas, and authentication response types that map directly from the original
 * BMS mapset and COBOL copybook definitions.
 * 
 * This file provides comprehensive type definitions for the login screen (COSGN00) that replicate
 * the original BMS field attributes, validation patterns, and data structures while enabling
 * modern React component development with Material-UI integration and JWT-based authentication.
 * 
 * The interfaces ensure exact functional equivalence with the original COBOL/CICS application
 * while providing enhanced type safety and validation capabilities for the React frontend.
 * 
 * Original BMS Map Reference: app/bms/COSGN00.bms
 * Original Copybook Reference: app/cpy-bms/COSGN00.CPY
 * 
 * Key Field Mappings:
 * - USERID: 8-character user identification field with IC (initial cursor)
 * - PASSWD: 8-character password field with DRK (dark/masked) attribute  
 * - ERRMSG: 78-character error message field with BRT (bright) attribute
 * - Header fields: TRNNAME, PGMNAME, CURDATE, CURTIME, TITLE01, TITLE02
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → React/Spring Boot/JWT
 */

import { ErrorMessage } from './CommonTypes';
import { ApiResponse } from './ApiTypes';

/**
 * Login Form Data Interface
 * 
 * Represents the user input fields from the COSGN00 login screen, mapping directly
 * to the BMS mapset field definitions while providing modern form validation support.
 * 
 * Field Mappings from COSGN00.bms:
 * - userid: Maps to USERID field (LENGTH=8, ATTRB=FSET,IC,NORM,UNPROT)
 * - passwd: Maps to PASSWD field (LENGTH=8, ATTRB=DRK,FSET,UNPROT)
 * - fieldAttributes: Additional BMS attributes for Material-UI integration
 */
export interface LoginFormData {
  /** User identification field (8 characters maximum) - equivalent to BMS USERID field */
  userid: string;
  
  /** Password field (8 characters maximum) - equivalent to BMS PASSWD field with DRK attribute */
  passwd: string;
  
  /** BMS field attributes for Material-UI component integration */
  fieldAttributes: LoginFieldAttributes;
}

/**
 * Login Response Data Interface
 * 
 * Represents the JWT authentication response structure from the Spring Boot
 * AuthenticationService, implementing modern OAuth2-style token authentication
 * while maintaining compatibility with the original CICS session management.
 * 
 * This interface replaces the COMMAREA response pattern with structured JSON
 * while preserving equivalent authentication context and user session information.
 */
export interface LoginResponseData {
  /** JWT access token for API authentication - HS256 signed with 30-minute expiration */
  token: string;
  
  /** JWT refresh token for token renewal - longer expiration for session persistence */
  refreshToken: string;
  
  /** Token type specification (typically "Bearer" for JWT tokens) */
  tokenType: string;
  
  /** Token expiration time in seconds (default 1800 seconds = 30 minutes) */
  expiresIn: number;
  
  /** User ID from PostgreSQL users table - equivalent to original USERID field */
  userId: string;
  
  /** User role for authorization - maps CARDDEMO.ADMIN/CARDDEMO.USER to ROLE_ADMIN/ROLE_USER */
  userRole: string;
  
  /** Redis session identifier for distributed session management */
  sessionId: string;
  
  /** Authentication timestamp for audit logging and session tracking */
  timestamp: Date;
}

/**
 * Login Screen Data Interface
 * 
 * Comprehensive screen state management interface that maintains all data required
 * for the login screen rendering, validation, and user interaction. This interface
 * preserves the BMS screen data structure while adding modern React state management.
 * 
 * Includes base screen data (transaction name, program name, date/time) that appears
 * consistently across all BMS screens, form data for user input, and message handling
 * for error and success notifications.
 */
export interface LoginScreenData {
  /** Base screen header data common to all BMS screens */
  baseScreenData: {
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
  
  /** Login form input data */
  formData: LoginFormData;
  
  /** Current error message to display - equivalent to BMS ERRMSG field */
  errorMessage?: ErrorMessage;
  
  /** Current success message for positive feedback */
  successMessage?: string;
  
  /** Loading state indicator during authentication processing */
  isLoading: boolean;
  
  /** Form submission state to prevent double-submission */
  isSubmitting: boolean;
  
  /** Field-level validation errors for real-time feedback */
  validationErrors: Record<string, string>;
  
  /** BMS field attributes for UI component rendering */
  fieldAttributes: LoginFieldAttributes;
}

/**
 * Login Validation Schema Interface
 * 
 * Defines comprehensive validation rules that replicate the original BMS validation
 * behavior while providing enhanced client-side validation capabilities. This interface
 * ensures that validation logic matches the original CICS/COBOL validation patterns
 * while enabling modern JavaScript validation libraries.
 */
export interface LoginValidationSchema {
  /** User ID field validation rules */
  useridValidation: {
    /** Required field indicator */
    required: boolean;
    
    /** Minimum length validation (must be non-empty) */
    minLength: number;
    
    /** Maximum length validation (8 characters from BMS LENGTH=8) */
    maxLength: number;
    
    /** Regular expression pattern for allowed characters */
    pattern: string;
    
    /** Validation error message for user feedback */
    message: string;
  };
  
  /** Password field validation rules */
  passwdValidation: {
    /** Required field indicator */
    required: boolean;
    
    /** Minimum length validation (must be non-empty) */
    minLength: number;
    
    /** Maximum length validation (8 characters from BMS LENGTH=8) */
    maxLength: number;
    
    /** Regular expression pattern for allowed characters */
    pattern: string;
    
    /** Validation error message for user feedback */
    message: string;
  };
  
  /** Form-level validation combining all field validations */
  formValidation: {
    /** All required fields must be completed */
    allFieldsRequired: boolean;
    
    /** Cross-field validation rules */
    crossFieldValidation: boolean;
    
    /** Form submission validation message */
    message: string;
  };
  
  /** BMS MUSTFILL equivalent validation for required fields */
  mustfillValidation: {
    /** Fields that must be filled before submission */
    requiredFields: string[];
    
    /** Validation message for missing required fields */
    message: string;
  };
  
  /** BMS LENGTH attribute validation for field length constraints */
  lengthValidation: {
    /** Maximum field lengths matching BMS definitions */
    fieldLengths: Record<string, number>;
    
    /** Validation message for length violations */
    message: string;
  };
}

/**
 * Login Field Attributes Interface
 * 
 * Maps BMS field attributes to Material-UI component properties, ensuring that
 * the React components maintain identical field behavior to the original BMS
 * screen while providing enhanced user experience through modern UI components.
 * 
 * This interface preserves the exact BMS attribute behavior while enabling
 * Material-UI integration and responsive design capabilities.
 */
export interface LoginFieldAttributes {
  /** User ID field attributes - equivalent to BMS USERID field */
  useridAttributes: {
    /** Field length from BMS LENGTH=8 */
    length: number;
    
    /** Field position equivalent to BMS POS=(19,43) */
    position: { row: number; column: number };
    
    /** BMS attributes: FSET,IC,NORM,UNPROT */
    attributes: string[];
    
    /** BMS color attribute (GREEN) */
    color: string;
    
    /** BMS highlight attribute (OFF) */
    highlight: string;
    
    /** Auto-focus for initial cursor (IC attribute) */
    autoFocus: boolean;
    
    /** Input mode for mobile keyboards */
    inputMode: string;
  };
  
  /** Password field attributes - equivalent to BMS PASSWD field */
  passwdAttributes: {
    /** Field length from BMS LENGTH=8 */
    length: number;
    
    /** Field position equivalent to BMS POS=(20,43) */
    position: { row: number; column: number };
    
    /** BMS attributes: DRK,FSET,UNPROT */
    attributes: string[];
    
    /** BMS color attribute (GREEN) */
    color: string;
    
    /** BMS highlight attribute (OFF) */
    highlight: string;
    
    /** Password masking for DRK attribute */
    masked: boolean;
    
    /** Input type for password fields */
    inputType: string;
  };
  
  /** Error message field attributes - equivalent to BMS ERRMSG field */
  errmsgAttributes: {
    /** Field length from BMS LENGTH=78 */
    length: number;
    
    /** Field position equivalent to BMS POS=(23,1) */
    position: { row: number; column: number };
    
    /** BMS attributes: ASKIP,BRT,FSET */
    attributes: string[];
    
    /** BMS color attribute (RED) */
    color: string;
    
    /** BMS highlight attribute (OFF) */
    highlight: string;
    
    /** Read-only display for ASKIP attribute */
    readOnly: boolean;
    
    /** Bright display for BRT attribute */
    bright: boolean;
  };
  
  /** Submit button attributes for form submission */
  buttonAttributes: {
    /** Button position on screen */
    position: { row: number; column: number };
    
    /** Button text and styling */
    text: string;
    
    /** Button color and appearance */
    color: string;
    
    /** Button enabled/disabled state */
    enabled: boolean;
  };
}

/**
 * Authentication Status Type
 * 
 * Enumeration of possible authentication states that correspond to the original
 * CICS authentication outcomes while providing modern authentication status tracking.
 * 
 * These statuses map to the original COMMAREA return codes and provide equivalent
 * authentication result information for the React frontend.
 */
export type AuthenticationStatus = 
  | 'IDLE'              // Initial state, no authentication attempted
  | 'AUTHENTICATING'    // Authentication in progress
  | 'AUTHENTICATED'     // Successful authentication - equivalent to COMMAREA status '00'
  | 'INVALID_USERID'    // Invalid user ID - equivalent to COMMAREA status '98'
  | 'INVALID_PASSWORD'  // Invalid password - equivalent to COMMAREA status '98'
  | 'ACCOUNT_LOCKED'    // Account locked due to failed attempts
  | 'SYSTEM_ERROR'      // System error during authentication - equivalent to COMMAREA status '99'
  | 'SESSION_EXPIRED'   // Session timeout or expiration
  | 'UNAUTHORIZED';     // General authorization failure

/**
 * Login API Response Type
 * 
 * Specialized API response type that wraps the LoginResponseData in the standard
 * API response structure, providing consistent response handling across all
 * authentication endpoints while maintaining compatibility with the ApiTypes framework.
 * 
 * This type ensures that login responses follow the same pattern as other API
 * responses while providing authentication-specific data structure.
 */
export type LoginApiResponse = ApiResponse<LoginResponseData>;

/**
 * Login Constants and Configuration
 * 
 * Constants that define the login screen behavior, validation rules, and
 * configuration parameters that correspond to the original BMS definitions
 * and CICS transaction processing characteristics.
 */

/** Field length constants from BMS definitions */
export const LOGIN_FIELD_LENGTHS = {
  USERID: 8,        // BMS USERID field LENGTH=8
  PASSWD: 8,        // BMS PASSWD field LENGTH=8
  ERRMSG: 78,       // BMS ERRMSG field LENGTH=78
  TRNNAME: 4,       // BMS TRNNAME field LENGTH=4
  PGMNAME: 8,       // BMS PGMNAME field LENGTH=8
} as const;

/** Login screen transaction constants */
export const LOGIN_TRANSACTION = {
  TRANSACTION_CODE: 'SGON',     // Original CICS transaction code
  PROGRAM_NAME: 'COSGN00C',     // Original COBOL program name
  SCREEN_TITLE: 'CardDemo Login', // Screen title for display
  SUCCESS_MESSAGE: 'Login successful', // Success confirmation message
} as const;

/** Authentication endpoint configurations */
export const LOGIN_ENDPOINTS = {
  LOGIN: '/api/auth/login',           // Primary authentication endpoint
  REFRESH: '/api/auth/refresh',       // Token refresh endpoint
  LOGOUT: '/api/auth/logout',         // Session termination endpoint
  VALIDATE: '/api/auth/validate',     // Token validation endpoint
} as const;

/** JWT token configuration constants */
export const JWT_CONFIG = {
  TOKEN_TYPE: 'Bearer',               // JWT token type for Authorization header
  DEFAULT_EXPIRATION: 1800,           // Default token expiration (30 minutes)
  REFRESH_THRESHOLD: 300,             // Refresh token when 5 minutes remaining
  STORAGE_KEY: 'carddemo_jwt_token',  // localStorage key for token storage
} as const;

/** Validation patterns for form fields */
export const VALIDATION_PATTERNS = {
  USERID: /^[A-Z0-9]{1,8}$/,         // Uppercase alphanumeric, 1-8 characters
  PASSWD: /^.{1,8}$/,                 // Any character, 1-8 characters
} as const;

/** Error messages for validation failures */
export const ERROR_MESSAGES = {
  USERID_REQUIRED: 'User ID is required',
  USERID_LENGTH: 'User ID must be 1-8 characters',
  USERID_INVALID: 'User ID must contain only letters and numbers',
  PASSWD_REQUIRED: 'Password is required',
  PASSWD_LENGTH: 'Password must be 1-8 characters',
  LOGIN_FAILED: 'Invalid User ID or Password',
  SYSTEM_ERROR: 'System error - please try again',
  SESSION_EXPIRED: 'Session expired - please login again',
} as const;

/**
 * Type Guards for Runtime Type Checking
 * 
 * Utility functions that provide runtime type validation for login-related
 * data structures, ensuring type safety when processing API responses and
 * user input data.
 */

/**
 * Type guard to check if a response is a valid LoginApiResponse
 * 
 * @param response - Response object to validate
 * @returns True if response is a valid LoginApiResponse
 */
export const isLoginApiResponse = (response: any): response is LoginApiResponse => {
  return response && 
         typeof response === 'object' &&
         'data' in response &&
         'status' in response &&
         'message' in response &&
         'timestamp' in response &&
         'correlationId' in response &&
         response.data &&
         typeof response.data.token === 'string' &&
         typeof response.data.userId === 'string' &&
         typeof response.data.userRole === 'string';
};

/**
 * Type guard to check if authentication status indicates success
 * 
 * @param status - Authentication status to check
 * @returns True if status indicates successful authentication
 */
export const isAuthenticationSuccess = (status: AuthenticationStatus): boolean => {
  return status === 'AUTHENTICATED';
};

/**
 * Type guard to check if authentication status indicates an error
 * 
 * @param status - Authentication status to check
 * @returns True if status indicates an authentication error
 */
export const isAuthenticationError = (status: AuthenticationStatus): boolean => {
  return status === 'INVALID_USERID' || 
         status === 'INVALID_PASSWORD' || 
         status === 'ACCOUNT_LOCKED' || 
         status === 'SYSTEM_ERROR' || 
         status === 'UNAUTHORIZED';
};

/**
 * Utility function to create a default LoginFormData object
 * 
 * @returns Default LoginFormData with empty values and proper field attributes
 */
export const createDefaultLoginFormData = (): LoginFormData => ({
  userid: '',
  passwd: '',
  fieldAttributes: {
    useridAttributes: {
      length: LOGIN_FIELD_LENGTHS.USERID,
      position: { row: 19, column: 43 },
      attributes: ['FSET', 'IC', 'NORM', 'UNPROT'],
      color: 'GREEN',
      highlight: 'OFF',
      autoFocus: true,
      inputMode: 'text',
    },
    passwdAttributes: {
      length: LOGIN_FIELD_LENGTHS.PASSWD,
      position: { row: 20, column: 43 },
      attributes: ['DRK', 'FSET', 'UNPROT'],
      color: 'GREEN',
      highlight: 'OFF',
      masked: true,
      inputType: 'password',
    },
    errmsgAttributes: {
      length: LOGIN_FIELD_LENGTHS.ERRMSG,
      position: { row: 23, column: 1 },
      attributes: ['ASKIP', 'BRT', 'FSET'],
      color: 'RED',
      highlight: 'OFF',
      readOnly: true,
      bright: true,
    },
    buttonAttributes: {
      position: { row: 24, column: 1 },
      text: 'Sign On',
      color: 'primary',
      enabled: true,
    },
  },
});

/**
 * Utility function to create a default LoginValidationSchema
 * 
 * @returns Default validation schema with BMS-equivalent validation rules
 */
export const createDefaultValidationSchema = (): LoginValidationSchema => ({
  useridValidation: {
    required: true,
    minLength: 1,
    maxLength: LOGIN_FIELD_LENGTHS.USERID,
    pattern: VALIDATION_PATTERNS.USERID.source,
    message: ERROR_MESSAGES.USERID_INVALID,
  },
  passwdValidation: {
    required: true,
    minLength: 1,
    maxLength: LOGIN_FIELD_LENGTHS.PASSWD,
    pattern: VALIDATION_PATTERNS.PASSWD.source,
    message: ERROR_MESSAGES.PASSWD_LENGTH,
  },
  formValidation: {
    allFieldsRequired: true,
    crossFieldValidation: false,
    message: 'Please complete all required fields',
  },
  mustfillValidation: {
    requiredFields: ['userid', 'passwd'],
    message: 'All fields are required',
  },
  lengthValidation: {
    fieldLengths: {
      userid: LOGIN_FIELD_LENGTHS.USERID,
      passwd: LOGIN_FIELD_LENGTHS.PASSWD,
    },
    message: 'Field exceeds maximum length',
  },
});