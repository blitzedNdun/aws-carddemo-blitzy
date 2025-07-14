/**
 * UserManagementTypes.ts
 * 
 * TypeScript interface definitions for user management screens (COUSR00-03) including
 * user list data, create/update forms, search criteria, and role-based access control 
 * types matching Spring Security requirements.
 * 
 * This file provides comprehensive TypeScript interfaces that replicate the original
 * BMS field structures from COUSR00.bms (User List), COUSR01.bms (Add User), 
 * COUSR02.bms (Update User), and COUSR03.bms (Delete User) screens while enabling
 * modern React component development with Material-UI integration and type safety.
 * 
 * Key Features:
 * - Exact BMS field structure preservation with TypeScript type safety
 * - Role-based access control integration with Spring Security
 * - Comprehensive validation schemas for user management forms
 * - Audit trail support for compliance and security tracking
 * - Search and pagination capabilities for user list management
 * - Optimistic locking support for concurrent user update operations
 * 
 * BMS Screen Mappings:
 * - COUSR00 (User List) → UserListData interface
 * - COUSR01 (Add User) → UserCreateFormData interface
 * - COUSR02 (Update User) → UserUpdateFormData interface
 * - COUSR03 (Delete User) → UserSearchCriteria interface
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { BaseScreenData } from './CommonTypes';
import { FormValidationSchema } from './ValidationTypes';

/**
 * User Type Definition
 * 
 * Maps to original BMS UTYPE field definition from COUSR00-03.bms
 * Supporting administrative and regular user roles as defined in Spring Security
 */
export type UserType = 'A' | 'U';  // A=Admin, U=User (matching original BMS validation)

/**
 * User Row Data Interface
 * 
 * Represents individual user data rows displayed in the user list screen (COUSR00).
 * Maps directly to the repeating field pattern (SEL0001-SEL0010, USRID01-USRID10, etc.)
 * from the original BMS definition, supporting up to 10 users per page display.
 */
export interface UserRowData {
  /** 
   * Selection indicator (1 character) - equivalent to SEL0001-SEL0010 fields
   * Used for row selection in user list operations (Update 'U' or Delete 'D')
   */
  selection: string;
  
  /** 
   * User ID (8 characters) - equivalent to USRID01-USRID10 fields
   * Primary key for user identification and authentication
   */
  userId: string;
  
  /** 
   * First name (20 characters) - equivalent to FNAME01-FNAME10 fields
   * User's first name for display and identification
   */
  firstName: string;
  
  /** 
   * Last name (20 characters) - equivalent to LNAME01-LNAME10 fields
   * User's last name for display and identification
   */
  lastName: string;
  
  /** 
   * User type (1 character) - equivalent to UTYPE01-UTYPE10 fields
   * Role designation: 'A' for Admin, 'U' for regular User
   */
  userType: UserType;
  
  /** 
   * Selection state indicator for React component state management
   * Tracks whether this row is currently selected by the user
   */
  isSelected: boolean;
  
  /** 
   * Visibility state indicator for conditional rendering
   * Controls whether this row should be displayed in the current view
   */
  isVisible: boolean;
}

/**
 * User List Data Interface
 * 
 * Complete interface for the User List screen (COUSR00) providing comprehensive
 * user management capabilities including search, pagination, and role-based filtering.
 * Replicates all BMS fields from COUSR00.bms while adding modern pagination and
 * search capabilities for enhanced user experience.
 */
export interface UserListData {
  /** 
   * Common BMS header fields (trnname, pgmname, curdate, curtime, title01, title02)
   * Inherited from BaseScreenData to maintain consistency across all screens
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * Current page number (8 characters) - equivalent to PAGENUM field
   * Supports pagination navigation with page tracking
   */
  pageNumber: string;
  
  /** 
   * Search user ID filter (8 characters) - equivalent to USRIDIN field
   * Enables user search functionality by user ID partial or exact match
   */
  searchUserId: string;
  
  /** 
   * User data rows (up to 10 per page) - equivalent to SEL0001-SEL0010 + USRID01-USRID10 + FNAME01-FNAME10 + LNAME01-LNAME10 + UTYPE01-UTYPE10 field groups
   * Array of UserRowData objects representing paginated user list display
   */
  userRows: UserRowData[];
  
  /** 
   * Total user count for pagination calculation
   * Provides information for pagination controls and user count display
   */
  totalUsers: number;
  
  /** 
   * Has more pages indicator for pagination controls
   * Indicates whether forward pagination (F8) is available
   */
  hasMorePages: boolean;
  
  /** 
   * Selected users array for batch operations
   * Tracks multiple user selections for bulk update/delete operations
   */
  selectedUsers: string[];
  
  /** 
   * Error message (78 characters) - equivalent to ERRMSG field
   * Displays validation errors, system messages, and user feedback
   */
  errorMessage: string;
  
  /** 
   * BMS field attributes mapping for form field behavior control
   * Maps BMS attributes (ASKIP, UNPROT, FSET, etc.) to React component properties
   */
  fieldAttributes: {
    pageNumber: { attrb: 'ASKIP' | 'FSET' | 'NORM' };
    searchUserId: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    userRows: { attrb: 'ASKIP' | 'FSET' | 'NORM' };
    errorMessage: { attrb: 'ASKIP' | 'BRT' | 'FSET' };
  };
}

/**
 * User Create Form Data Interface
 * 
 * Complete interface for the Add User screen (COUSR01) providing comprehensive
 * user creation capabilities with validation, role assignment, and security integration.
 * Replicates all BMS fields from COUSR01.bms while adding modern form validation
 * and security features for enhanced user management.
 */
export interface UserCreateFormData {
  /** 
   * Common BMS header fields (trnname, pgmname, curdate, curtime, title01, title02)
   * Inherited from BaseScreenData to maintain consistency across all screens
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * First name (20 characters) - equivalent to FNAME field
   * User's first name with input validation and formatting
   */
  firstName: string;
  
  /** 
   * Last name (20 characters) - equivalent to LNAME field
   * User's last name with input validation and formatting
   */
  lastName: string;
  
  /** 
   * User ID (8 characters) - equivalent to USERID field
   * Unique identifier for user authentication and system access
   */
  userId: string;
  
  /** 
   * Password (8 characters) - equivalent to PASSWD field
   * User's initial password with masked display (DRK attribute)
   */
  password: string;
  
  /** 
   * User type (1 character) - equivalent to USRTYPE field
   * Role designation: 'A' for Admin, 'U' for regular User
   */
  userType: UserType;
  
  /** 
   * Error message (78 characters) - equivalent to ERRMSG field
   * Displays validation errors, system messages, and user feedback
   */
  errorMessage: string;
  
  /** 
   * Validation result for form submission
   * Contains validation status and field-specific error information
   */
  validationResult: {
    isValid: boolean;
    fieldErrors: Record<string, string>;
    formErrors: string[];
  };
  
  /** 
   * BMS field attributes mapping for form field behavior control
   * Maps BMS attributes (ASKIP, UNPROT, FSET, etc.) to React component properties
   */
  fieldAttributes: {
    firstName: { attrb: 'FSET' | 'IC' | 'NORM' | 'UNPROT' };
    lastName: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    userId: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    password: { attrb: 'DRK' | 'FSET' | 'UNPROT' };
    userType: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    errorMessage: { attrb: 'ASKIP' | 'BRT' | 'FSET' };
  };
}

/**
 * User Update Form Data Interface
 * 
 * Complete interface for the Update User screen (COUSR02) providing comprehensive
 * user modification capabilities with optimistic locking, audit trail, and security
 * integration. Replicates all BMS fields from COUSR02.bms while adding modern
 * concurrency control and audit features for enhanced user management.
 */
export interface UserUpdateFormData {
  /** 
   * Common BMS header fields (trnname, pgmname, curdate, curtime, title01, title02)
   * Inherited from BaseScreenData to maintain consistency across all screens
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * Search user ID (8 characters) - equivalent to USRIDIN field
   * User ID for lookup and fetching existing user information
   */
  searchUserId: string;
  
  /** 
   * First name (20 characters) - equivalent to FNAME field
   * User's first name with input validation and formatting
   */
  firstName: string;
  
  /** 
   * Last name (20 characters) - equivalent to LNAME field
   * User's last name with input validation and formatting
   */
  lastName: string;
  
  /** 
   * Password (8 characters) - equivalent to PASSWD field
   * User's password with masked display (DRK attribute)
   */
  password: string;
  
  /** 
   * User type (1 character) - equivalent to USRTYPE field
   * Role designation: 'A' for Admin, 'U' for regular User
   */
  userType: UserType;
  
  /** 
   * Error message (78 characters) - equivalent to ERRMSG field
   * Displays validation errors, system messages, and user feedback
   */
  errorMessage: string;
  
  /** 
   * Validation result for form submission
   * Contains validation status and field-specific error information
   */
  validationResult: {
    isValid: boolean;
    fieldErrors: Record<string, string>;
    formErrors: string[];
  };
  
  /** 
   * User found indicator for search operations
   * Indicates whether the searched user ID exists in the system
   */
  isUserFound: boolean;
  
  /** 
   * Optimistic locking version for concurrent update control
   * Prevents lost updates in multi-user environment
   */
  optimisticLockVersion: number;
  
  /** 
   * Audit trail information for change tracking
   * Maintains compliance and security audit requirements
   */
  auditTrail: UserAuditTrail;
  
  /** 
   * BMS field attributes mapping for form field behavior control
   * Maps BMS attributes (ASKIP, UNPROT, FSET, etc.) to React component properties
   */
  fieldAttributes: {
    searchUserId: { attrb: 'FSET' | 'IC' | 'NORM' | 'UNPROT' };
    firstName: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    lastName: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    password: { attrb: 'DRK' | 'FSET' | 'UNPROT' };
    userType: { attrb: 'FSET' | 'NORM' | 'UNPROT' };
    errorMessage: { attrb: 'ASKIP' | 'BRT' | 'FSET' };
  };
}

/**
 * User Search Criteria Interface
 * 
 * Complete interface for the Delete User screen (COUSR03) providing comprehensive
 * user search and identification capabilities with read-only display and confirmation
 * features. Replicates all BMS fields from COUSR03.bms while adding modern search
 * capabilities and security features for enhanced user management.
 */
export interface UserSearchCriteria {
  /** 
   * Common BMS header fields (trnname, pgmname, curdate, curtime, title01, title02)
   * Inherited from BaseScreenData to maintain consistency across all screens
   */
  baseScreenData: BaseScreenData;
  
  /** 
   * Search user ID (8 characters) - equivalent to USRIDIN field
   * User ID for lookup and fetching existing user information
   */
  searchUserId: string;
  
  /** 
   * First name (20 characters) - equivalent to FNAME field
   * User's first name displayed in read-only mode (ASKIP attribute)
   */
  firstName: string;
  
  /** 
   * Last name (20 characters) - equivalent to LNAME field
   * User's last name displayed in read-only mode (ASKIP attribute)
   */
  lastName: string;
  
  /** 
   * User type (1 character) - equivalent to USRTYPE field
   * Role designation displayed in read-only mode (ASKIP attribute)
   */
  userType: UserType;
  
  /** 
   * Error message (78 characters) - equivalent to ERRMSG field
   * Displays validation errors, system messages, and user feedback
   */
  errorMessage: string;
  
  /** 
   * User found indicator for search operations
   * Indicates whether the searched user ID exists in the system
   */
  isUserFound: boolean;
  
  /** 
   * Read-only mode indicator for display control
   * Controls whether fields are displayed in read-only mode (ASKIP)
   */
  readOnlyMode: boolean;
  
  /** 
   * BMS field attributes mapping for form field behavior control
   * Maps BMS attributes (ASKIP, UNPROT, FSET, etc.) to React component properties
   */
  fieldAttributes: {
    searchUserId: { attrb: 'FSET' | 'IC' | 'NORM' | 'UNPROT' };
    firstName: { attrb: 'ASKIP' | 'FSET' | 'NORM' };
    lastName: { attrb: 'ASKIP' | 'FSET' | 'NORM' };
    userType: { attrb: 'ASKIP' | 'FSET' | 'NORM' };
    errorMessage: { attrb: 'ASKIP' | 'BRT' | 'FSET' };
  };
}

/**
 * User Audit Trail Interface
 * 
 * Comprehensive audit trail tracking interface for user management operations
 * supporting compliance requirements and security monitoring. Provides detailed
 * change history and version tracking for all user management activities.
 */
export interface UserAuditTrail {
  /** 
   * User ID who created the record
   * Tracks original user creation for audit purposes
   */
  createdBy: string;
  
  /** 
   * Timestamp when record was created
   * Maintains creation time for audit trail and compliance
   */
  createdDate: Date;
  
  /** 
   * User ID who last modified the record
   * Tracks most recent modification for audit purposes
   */
  lastModifiedBy: string;
  
  /** 
   * Timestamp when record was last modified
   * Maintains modification time for audit trail and compliance
   */
  lastModifiedDate: Date;
  
  /** 
   * Current version number for optimistic locking
   * Supports concurrent access control and change tracking
   */
  version: number;
  
  /** 
   * Detailed change history for compliance and security
   * Maintains comprehensive audit trail of all modifications
   */
  changeHistory: {
    changeId: string;
    changedBy: string;
    changeDate: Date;
    changeType: 'CREATE' | 'UPDATE' | 'DELETE' | 'ROLE_CHANGE';
    fieldChanges: {
      fieldName: string;
      oldValue: string;
      newValue: string;
    }[];
    changeReason?: string;
  }[];
}

/**
 * User Management Validation Schema Interface
 * 
 * Comprehensive validation schema interface for all user management forms providing
 * type-safe validation rules, error handling, and integration with React Hook Form
 * and Yup validation libraries. Supports complex validation scenarios including
 * cross-field validation and business rule enforcement.
 */
export interface UserManagementValidationSchema {
  /** 
   * User ID validation rules
   * Enforces 8-character length, alphanumeric format, and uniqueness constraints
   */
  userIdValidation: {
    required: boolean;
    minLength: number;
    maxLength: number;
    pattern: string;
    uniqueCheck: boolean;
    errorMessages: {
      required: string;
      minLength: string;
      maxLength: string;
      pattern: string;
      notUnique: string;
    };
  };
  
  /** 
   * First name validation rules
   * Enforces 20-character maximum length and alphabetic character constraints
   */
  firstNameValidation: {
    required: boolean;
    minLength: number;
    maxLength: number;
    pattern: string;
    errorMessages: {
      required: string;
      minLength: string;
      maxLength: string;
      pattern: string;
    };
  };
  
  /** 
   * Last name validation rules
   * Enforces 20-character maximum length and alphabetic character constraints
   */
  lastNameValidation: {
    required: boolean;
    minLength: number;
    maxLength: number;
    pattern: string;
    errorMessages: {
      required: string;
      minLength: string;
      maxLength: string;
      pattern: string;
    };
  };
  
  /** 
   * Password validation rules
   * Enforces 8-character length, complexity requirements, and security constraints
   */
  passwordValidation: {
    required: boolean;
    minLength: number;
    maxLength: number;
    pattern: string;
    complexityRules: {
      requireUppercase: boolean;
      requireLowercase: boolean;
      requireNumbers: boolean;
      requireSpecialChars: boolean;
    };
    errorMessages: {
      required: string;
      minLength: string;
      maxLength: string;
      pattern: string;
      complexity: string;
    };
  };
  
  /** 
   * User type validation rules
   * Enforces valid role selection and authorization constraints
   */
  userTypeValidation: {
    required: boolean;
    validValues: UserType[];
    errorMessages: {
      required: string;
      invalidValue: string;
    };
  };
  
  /** 
   * Form-level validation schema
   * Integrates with FormValidationSchema for comprehensive form validation
   */
  formValidation: FormValidationSchema<UserCreateFormData | UserUpdateFormData>;
}