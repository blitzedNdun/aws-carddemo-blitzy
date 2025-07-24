/**
 * LoginTypes.ts
 * 
 * TypeScript interface definitions for the COSGN00 login screen including form data 
 * structure, validation schemas, and authentication response types that map directly 
 * from the original BMS mapset and COBOL copybook definitions.
 * 
 * This file implements the BMS Maps → React Components transformation requirements 
 * for the login screen, providing strongly-typed interfaces for JWT token-based 
 * authentication through Spring Security integration with the AuthenticationService.
 * 
 * Key Features:
 * - Precise field mapping from COSGN00.bms and COSGN00.CPY to TypeScript interfaces
 * - BMS field attributes preservation including MUSTFILL, length, and validation rules
 * - JWT authentication response structure with token management capabilities
 * - Form validation schemas matching original BMS field behavior and constraints
 * - Complete screen state management for React component integration
 * 
 * Original BMS Field Mappings:
 * - USERID (8 chars, UNPROT, IC) → LoginFormData.userid
 * - PASSWD (8 chars, DRK, UNPROT) → LoginFormData.passwd  
 * - ERRMSG (78 chars, ASKIP, BRT, RED) → LoginScreenData.errorMessage
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { ErrorMessage } from './CommonTypes';
import { ApiResponse } from './ApiTypes';

/**
 * Authentication Status Type Definition
 * Maps authentication result states for login process flow control
 * Enables consistent authentication status handling across React components
 */
export type AuthenticationStatus = 
  | 'idle'           // Initial state, no authentication attempted
  | 'authenticating' // Authentication request in progress
  | 'authenticated'  // Successfully authenticated with valid JWT token
  | 'failed'         // Authentication failed due to invalid credentials
  | 'expired'        // JWT token expired, requires re-authentication
  | 'locked'         // Account locked due to multiple failed attempts
  | 'disabled';      // User account disabled by administrator

/**
 * Login Form Data Interface
 * Represents user input data from COSGN00 login screen form fields
 * Maps directly to BMS fields USERID and PASSWD with exact field constraints
 * 
 * Field Mappings from COSGN00.bms:
 * - userid: USERID field (POS=(19,43), LENGTH=8, ATTRB=(FSET,IC,NORM,UNPROT))
 * - passwd: PASSWD field (POS=(20,43), LENGTH=8, ATTRB=(DRK,FSET,UNPROT))
 */
export interface LoginFormData {
  /** User identification (8 characters maximum, from USERID BMS field) */
  userid: string;
  /** User password (8 characters maximum, from PASSWD BMS field) */
  passwd: string;
  /** BMS field attributes for form rendering and validation behavior */
  fieldAttributes: LoginFieldAttributes;
}

/**
 * Login Response Data Interface
 * Defines JWT authentication response structure from AuthenticationService
 * Contains all necessary token information for session management and authorization
 * 
 * Supports Spring Security JWT authentication with role-based authorization
 * including admin (ROLE_ADMIN) and user (ROLE_USER) role mappings
 */
export interface LoginResponseData {
  /** JWT access token for API authentication (Base64 encoded) */
  token: string;
  /** JWT refresh token for token renewal without re-authentication */
  refreshToken: string;
  /** Token type specification (typically 'Bearer' for JWT tokens) */
  tokenType: string;
  /** Token expiration time in seconds (default 30 minutes = 1800 seconds) */
  expiresIn: number;
  /** User identifier from successful authentication (8 characters) */
  userId: string;
  /** User role for authorization (ADMIN or USER from Spring Security) */
  userRole: 'ADMIN' | 'USER';
  /** Unique session identifier for Redis session management */
  sessionId: string;
  /** Authentication timestamp for audit logging and session tracking */
  timestamp: Date;
}

/**
 * Login Screen Data Interface
 * Comprehensive state management for COSGN00 login screen React component
 * Maintains complete UI state including form data, loading states, messages, and validation
 * 
 * Replaces CICS pseudo-conversational state with React state management patterns
 * while preserving exact BMS screen behavior and field interaction patterns
 */
export interface LoginScreenData {
  /** Common BMS screen header data (transaction name, date, time, etc.) */
  baseScreenData: {
    /** Transaction name (4 characters) - 'CC00' for login screen */
    trnname: string;
    /** Program name (8 characters) - 'COSGN00C' for login program */
    pgmname: string;
    /** Current date (8 characters) - format: mm/dd/yy from CURDATE field */
    curdate: string;
    /** Current time (9 characters) - format: hh:mm:ss from CURTIME field */
    curtime: string;
    /** Primary screen title (40 characters) from TITLE01 field */
    title01: string;
    /** Secondary screen title (40 characters) from TITLE02 field */
    title02: string;
  };
  /** User input form data with validation attributes */
  formData: LoginFormData;
  /** Current error message from authentication failures or validation errors */
  errorMessage: ErrorMessage | null;
  /** Success message for successful authentication confirmations */
  successMessage: string | null;
  /** Loading state during authentication API calls */
  isLoading: boolean;
  /** Form submission state to prevent duplicate requests */
  isSubmitting: boolean;
  /** Field-level validation errors for real-time validation feedback */
  validationErrors: Record<string, string>;
  /** BMS field attributes for maintaining original field behavior */
  fieldAttributes: LoginFieldAttributes;
}

/**
 * Login Validation Schema Interface
 * Defines validation rules matching original BMS field validation behavior
 * Supports React Hook Form integration with comprehensive field validation
 * 
 * Preserves COSGN00.bms validation requirements including MUSTFILL attributes
 * and length constraints for userid and passwd fields
 */
export interface LoginValidationSchema {
  /** User ID field validation rules (required, 8 character maximum) */
  useridValidation: {
    /** Indicates field is required (MUSTFILL equivalent) */
    required: boolean;
    /** Minimum length validation (1 character minimum) */
    minLength: number;
    /** Maximum length validation (8 characters from BMS LENGTH attribute) */
    maxLength: number;
    /** Regex pattern for valid user ID format (alphanumeric characters) */
    pattern: RegExp;
    /** Error message for validation failures */
    errorMessage: string;
  };
  /** Password field validation rules (required, 8 character maximum) */
  passwdValidation: {
    /** Indicates field is required (MUSTFILL equivalent) */
    required: boolean;
    /** Minimum length validation (1 character minimum) */
    minLength: number;
    /** Maximum length validation (8 characters from BMS LENGTH attribute) */
    maxLength: number;
    /** Error message for validation failures */
    errorMessage: string;
  };
  /** Complete form validation rules for cross-field validation */
  formValidation: {
    /** Validation function for complete form data */
    validateForm: (formData: LoginFormData) => Record<string, string>;
    /** Submit validation rules for final form submission */
    submitValidation: {
      /** Require both fields to be completed before submission */
      requireAllFields: boolean;
      /** Trim whitespace from field values before validation */
      trimWhitespace: boolean;
    };
  };
  /** MUSTFILL validation rules matching BMS MUSTFILL attributes */
  mustfillValidation: {
    /** Fields that must be filled (equivalent to BMS MUSTFILL) */
    requiredFields: ('userid' | 'passwd')[];
    /** Error message for MUSTFILL validation failures */
    mustfillErrorMessage: string;
  };
  /** Length validation rules matching BMS LENGTH attributes */
  lengthValidation: {
    /** Maximum field lengths from BMS field definitions */
    fieldLengths: {
      userid: 8;   // From USERID LENGTH=8
      passwd: 8;   // From PASSWD LENGTH=8
    };
    /** Error message for length validation failures */
    lengthErrorMessage: string;
  };
}

/**
 * Login Field Attributes Interface
 * Maps BMS DFHMDF attributes to TypeScript for maintaining exact field behavior
 * Preserves original 3270 terminal field characteristics in React components
 * 
 * Field attributes from COSGN00.bms analysis:
 * - USERID: ATTRB=(FSET,IC,NORM,UNPROT), COLOR=GREEN, LENGTH=8
 * - PASSWD: ATTRB=(DRK,FSET,UNPROT), COLOR=GREEN, LENGTH=8
 * - ERRMSG: ATTRB=(ASKIP,BRT,FSET), COLOR=RED, LENGTH=78
 */
export interface LoginFieldAttributes {
  /** User ID field attributes matching USERID BMS field definition */
  useridAttributes: {
    /** Field is editable (UNPROT attribute) */
    editable: boolean;
    /** Field receives initial cursor focus (IC attribute) */
    initialCursor: boolean;
    /** Field color from BMS COLOR attribute */
    color: 'GREEN';
    /** Field length from BMS LENGTH attribute */
    length: 8;
    /** Field position from BMS POS attribute */
    position: { row: 19; column: 43 };
    /** Field is required (MUSTFILL validation) */
    required: boolean;
    /** Field change detection (FSET attribute) */
    trackChanges: boolean;
  };
  /** Password field attributes matching PASSWD BMS field definition */
  passwdAttributes: {
    /** Field is editable (UNPROT attribute) */
    editable: boolean;
    /** Field content is hidden (DRK attribute) */
    hidden: boolean;
    /** Field color from BMS COLOR attribute */
    color: 'GREEN';
    /** Field length from BMS LENGTH attribute */
    length: 8;
    /** Field position from BMS POS attribute */
    position: { row: 20; column: 43 };
    /** Field is required (MUSTFILL validation) */
    required: boolean;
    /** Field change detection (FSET attribute) */
    trackChanges: boolean;
  };
  /** Error message field attributes matching ERRMSG BMS field definition */
  errmsgAttributes: {
    /** Field is read-only (ASKIP attribute) */
    readOnly: boolean;
    /** Field is bright/emphasized (BRT attribute) */
    bright: boolean;
    /** Field color from BMS COLOR attribute */
    color: 'RED';
    /** Field length from BMS LENGTH attribute */
    length: 78;
    /** Field position from BMS POS attribute */
    position: { row: 23; column: 1 };
    /** Field change detection (FSET attribute) */
    trackChanges: boolean;
  };
  /** Function key button attributes for ENTER and F3 keys */
  buttonAttributes: {
    /** ENTER key function for login submission */
    enterButton: {
      /** Button label text */
      label: 'ENTER=Sign-on';
      /** Button is enabled/disabled state */
      enabled: boolean;
      /** Button action function */
      action: 'SUBMIT_LOGIN';
    };
    /** F3 key function for application exit */
    f3Button: {
      /** Button label text */
      label: 'F3=Exit';
      /** Button is enabled/disabled state */
      enabled: boolean;
      /** Button action function */
      action: 'EXIT_APPLICATION';
    };
  };
}

/**
 * Login API Response Type Definition
 * Standardized API response wrapper for login authentication requests
 * Uses generic ApiResponse wrapper with LoginResponseData payload
 * 
 * Provides consistent response structure for AuthenticationService REST endpoint
 * with proper error handling and correlation tracking for distributed systems
 */
export type LoginApiResponse = ApiResponse<LoginResponseData>;

/**
 * Example Usage Documentation:
 * 
 * // Login Form Data Creation
 * const loginData: LoginFormData = {
 *   userid: 'TESTUSER',
 *   passwd: 'password',
 *   fieldAttributes: {
 *     useridAttributes: {
 *       editable: true,
 *       initialCursor: true,
 *       color: 'GREEN',
 *       length: 8,
 *       position: { row: 19, column: 43 },
 *       required: true,
 *       trackChanges: true
 *     },
 *     passwdAttributes: {
 *       editable: true,
 *       hidden: true,
 *       color: 'GREEN',
 *       length: 8,
 *       position: { row: 20, column: 43 },
 *       required: true,
 *       trackChanges: true
 *     },
 *     errmsgAttributes: {
 *       readOnly: true,
 *       bright: true,
 *       color: 'RED',
 *       length: 78,
 *       position: { row: 23, column: 1 },
 *       trackChanges: true
 *     },
 *     buttonAttributes: {
 *       enterButton: {
 *         label: 'ENTER=Sign-on',
 *         enabled: true,
 *         action: 'SUBMIT_LOGIN'
 *       },
 *       f3Button: {
 *         label: 'F3=Exit',
 *         enabled: true,
 *         action: 'EXIT_APPLICATION'
 *       }
 *     }
 *   }
 * };
 * 
 * // Authentication Response Processing
 * const handleLoginResponse = (response: LoginApiResponse) => {
 *   if (response.status === 'SUCCESS' && response.data) {
 *     const { token, userRole, sessionId } = response.data;
 *     
 *     // Store JWT token for API authentication
 *     localStorage.setItem('authToken', token);
 *     
 *     // Set session information
 *     localStorage.setItem('sessionId', sessionId);
 *     
 *     // Route based on user role
 *     if (userRole === 'ADMIN') {
 *       navigate('/admin');
 *     } else {
 *       navigate('/menu');
 *     }
 *   } else {
 *     // Handle authentication error
 *     setErrorMessage({
 *       message: response.message,
 *       code: 'AUTH_FAILED',
 *       severity: 'error',
 *       timestamp: new Date()
 *     });
 *   }
 * };
 * 
 * // Validation Schema Usage
 * const validationSchema: LoginValidationSchema = {
 *   useridValidation: {
 *     required: true,
 *     minLength: 1,
 *     maxLength: 8,
 *     pattern: /^[A-Z0-9]+$/,
 *     errorMessage: 'User ID must be 1-8 alphanumeric characters'
 *   },
 *   passwdValidation: {
 *     required: true,
 *     minLength: 1,
 *     maxLength: 8,
 *     errorMessage: 'Password must be 1-8 characters'
 *   },
 *   formValidation: {
 *     validateForm: (formData: LoginFormData) => {
 *       const errors: Record<string, string> = {};
 *       
 *       if (!formData.userid.trim()) {
 *         errors.userid = 'User ID is required';
 *       }
 *       
 *       if (!formData.passwd.trim()) {
 *         errors.passwd = 'Password is required';
 *       }
 *       
 *       return errors;
 *     },
 *     submitValidation: {
 *       requireAllFields: true,
 *       trimWhitespace: true
 *     }
 *   },
 *   mustfillValidation: {
 *     requiredFields: ['userid', 'passwd'],
 *     mustfillErrorMessage: 'All fields must be completed'
 *   },
 *   lengthValidation: {
 *     fieldLengths: {
 *       userid: 8,
 *       passwd: 8
 *     },
 *     lengthErrorMessage: 'Field exceeds maximum length'
 *   }
 * };
 */