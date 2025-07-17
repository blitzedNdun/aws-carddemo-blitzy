/**
 * CardDemo - User Management TypeScript Type Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for user management 
 * screens (COUSR00-03) including user list data, create/update forms, search criteria,
 * and role-based access control types matching Spring Security requirements.
 * 
 * Transforms BMS screen definitions to React TypeScript components while preserving
 * exact field structures, validation patterns, and business logic from the original
 * COBOL/BMS implementation.
 * 
 * Maps directly to Spring Security role-based authorization model with ROLE_ADMIN
 * and ROLE_USER authorities, enabling comprehensive user management capabilities
 * through modern React components with type safety.
 */

import { BaseScreenData, FormFieldAttributes } from './CommonTypes';
import { FormValidationSchema } from './ValidationTypes';

// ===================================================================
// USER TYPE DEFINITIONS
// ===================================================================

/**
 * User Type - Maps to Spring Security role-based authorization
 * 
 * Based on analysis of user management BMS screens and Spring Security integration.
 * Defines the two primary user roles supported by the CardDemo application.
 */
export type UserType = 'A' | 'U';  // A = Admin (ROLE_ADMIN), U = User (ROLE_USER)

// ===================================================================
// USER LIST DATA INTERFACE (COUSR00)
// ===================================================================

/**
 * User List Data Interface - COUSR00 Screen
 * 
 * Represents the user list screen with pagination, search capabilities, and
 * role-based filtering. Maps directly to COUSR00.bms field structure while
 * providing modern React component data management.
 * 
 * Supports administrative user management operations including user selection,
 * search functionality, and navigation through paginated user records.
 * 
 * Based on BMS field analysis:
 * - PAGENUM: Page number display (8 characters)
 * - USRIDIN: Search user ID field (8 characters)
 * - SEL0001-SEL0010: Selection fields for 10 user rows
 * - USRID01-USRID10: User ID display fields
 * - FNAME01-FNAME10: First name display fields
 * - LNAME01-LNAME10: Last name display fields
 * - UTYPE01-UTYPE10: User type display fields
 * - ERRMSG: Error message display field (78 characters)
 */
export interface UserListData {
  /**
   * Base screen data - Common BMS header fields
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Page number - Maps to BMS PAGENUM field
   * Current page number in paginated user list display
   */
  pageNumber: number;
  
  /**
   * Search user ID - Maps to BMS USRIDIN field
   * User ID search criterion for filtering user list
   */
  searchUserId: string;
  
  /**
   * User rows - Array of user data for display
   * Contains up to 10 user records per page matching BMS screen layout
   */
  userRows: UserRowData[];
  
  /**
   * Total users - Total number of users matching search criteria
   * Enables pagination calculation and user count display
   */
  totalUsers: number;
  
  /**
   * Has more pages - Indicates if additional pages are available
   * Controls forward navigation button availability
   */
  hasMorePages: boolean;
  
  /**
   * Selected users - Array of selected user IDs
   * Tracks user selections for bulk operations (Update/Delete)
   */
  selectedUsers: string[];
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * System or validation error message display
   */
  errorMessage: string;
  
  /**
   * Field attributes - BMS field attribute mapping
   * Controls field display characteristics and validation behavior
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * User Row Data Interface
 * 
 * Represents individual user record data displayed in the user list.
 * Maps to BMS field groups (USRID01/FNAME01/LNAME01/UTYPE01, etc.)
 * with selection capability for administrative operations.
 */
export interface UserRowData {
  /**
   * Selection - Maps to BMS SEL0001-SEL0010 fields
   * User selection indicator for Update/Delete operations
   */
  selection: string;
  
  /**
   * User ID - Maps to BMS USRID01-USRID10 fields
   * Unique user identifier (8 characters)
   */
  userId: string;
  
  /**
   * First name - Maps to BMS FNAME01-FNAME10 fields
   * User's first name (20 characters)
   */
  firstName: string;
  
  /**
   * Last name - Maps to BMS LNAME01-LNAME10 fields
   * User's last name (20 characters)
   */
  lastName: string;
  
  /**
   * User type - Maps to BMS UTYPE01-UTYPE10 fields
   * User role classification (A=Admin, U=User)
   */
  userType: UserType;
  
  /**
   * Is selected - Selection state for UI management
   * Indicates whether this user row is currently selected
   */
  isSelected: boolean;
  
  /**
   * Is visible - Visibility state for filtering
   * Controls row display based on search criteria
   */
  isVisible: boolean;
}

// ===================================================================
// USER CREATE FORM DATA INTERFACE (COUSR01)
// ===================================================================

/**
 * User Create Form Data Interface - COUSR01 Screen
 * 
 * Represents the add user form with comprehensive validation and Spring Security
 * integration. Maps directly to COUSR01.bms field structure while providing
 * modern form management capabilities.
 * 
 * Supports user creation with password policy validation, role assignment,
 * and comprehensive error handling aligned with Spring Security authentication
 * requirements.
 * 
 * Based on BMS field analysis:
 * - FNAME: First name input field (20 characters)
 * - LNAME: Last name input field (20 characters)  
 * - USERID: User ID input field (8 characters)
 * - PASSWD: Password input field (8 characters, DRK attribute)
 * - USRTYPE: User type selection (1 character, A=Admin, U=User)
 * - ERRMSG: Error message display field (78 characters)
 */
export interface UserCreateFormData {
  /**
   * Base screen data - Common BMS header fields
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * First name - Maps to BMS FNAME field
   * User's first name with validation (20 character limit)
   */
  firstName: string;
  
  /**
   * Last name - Maps to BMS LNAME field
   * User's last name with validation (20 character limit)
   */
  lastName: string;
  
  /**
   * User ID - Maps to BMS USERID field
   * Unique user identifier (8 characters, alphanumeric)
   */
  userId: string;
  
  /**
   * Password - Maps to BMS PASSWD field
   * User password (8 characters, will be BCrypt hashed)
   */
  password: string;
  
  /**
   * User type - Maps to BMS USRTYPE field
   * Spring Security role assignment (A=Admin/ROLE_ADMIN, U=User/ROLE_USER)
   */
  userType: UserType;
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * Form validation or system error message display
   */
  errorMessage: string;
  
  /**
   * Validation result - Form validation state
   * Comprehensive validation results for all form fields
   */
  validationResult: {
    isValid: boolean;
    fieldErrors: Record<string, string>;
    generalErrors: string[];
  };
  
  /**
   * Field attributes - BMS field attribute mapping
   * Controls field display characteristics and validation behavior
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

// ===================================================================
// USER UPDATE FORM DATA INTERFACE (COUSR02)
// ===================================================================

/**
 * User Update Form Data Interface - COUSR02 Screen
 * 
 * Represents the update user form with search, modification capabilities,
 * and optimistic locking support. Maps directly to COUSR02.bms field structure
 * while providing modern form management and audit trail capabilities.
 * 
 * Supports user search, data modification, and comprehensive audit tracking
 * aligned with Spring Security authentication and authorization requirements.
 * 
 * Based on BMS field analysis:
 * - USRIDIN: Search user ID field (8 characters)
 * - FNAME: First name input field (20 characters)
 * - LNAME: Last name input field (20 characters)
 * - PASSWD: Password input field (8 characters, DRK attribute)
 * - USRTYPE: User type selection (1 character, A=Admin, U=User)
 * - ERRMSG: Error message display field (78 characters)
 */
export interface UserUpdateFormData {
  /**
   * Base screen data - Common BMS header fields
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Search user ID - Maps to BMS USRIDIN field
   * User ID for locating user record to update
   */
  searchUserId: string;
  
  /**
   * First name - Maps to BMS FNAME field
   * User's first name with validation (20 character limit)
   */
  firstName: string;
  
  /**
   * Last name - Maps to BMS LNAME field
   * User's last name with validation (20 character limit)
   */
  lastName: string;
  
  /**
   * Password - Maps to BMS PASSWD field
   * User password (8 characters, will be BCrypt hashed)
   */
  password: string;
  
  /**
   * User type - Maps to BMS USRTYPE field
   * Spring Security role assignment (A=Admin/ROLE_ADMIN, U=User/ROLE_USER)
   */
  userType: UserType;
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * Form validation or system error message display
   */
  errorMessage: string;
  
  /**
   * Validation result - Form validation state
   * Comprehensive validation results for all form fields
   */
  validationResult: {
    isValid: boolean;
    fieldErrors: Record<string, string>;
    generalErrors: string[];
  };
  
  /**
   * Is user found - Search result indicator
   * Indicates whether the searched user was found in the system
   */
  isUserFound: boolean;
  
  /**
   * Optimistic lock version - Concurrency control
   * Database version field for optimistic locking in update operations
   */
  optimisticLockVersion: number;
  
  /**
   * Audit trail - Change tracking information
   * Comprehensive audit information for compliance and security tracking
   */
  auditTrail: UserAuditTrail;
  
  /**
   * Field attributes - BMS field attribute mapping
   * Controls field display characteristics and validation behavior
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

// ===================================================================
// USER SEARCH CRITERIA INTERFACE (COUSR03)
// ===================================================================

/**
 * User Search Criteria Interface - COUSR03 Screen
 * 
 * Represents the delete user form with search and confirmation capabilities.
 * Maps directly to COUSR03.bms field structure while providing read-only
 * user display and deletion confirmation functionality.
 * 
 * Supports user search for deletion operations with comprehensive validation
 * and role-based access control through Spring Security authorization.
 * 
 * Based on BMS field analysis:
 * - USRIDIN: Search user ID field (8 characters)
 * - FNAME: First name display field (20 characters, read-only)
 * - LNAME: Last name display field (20 characters, read-only)
 * - USRTYPE: User type display field (1 character, read-only)
 * - ERRMSG: Error message display field (78 characters)
 */
export interface UserSearchCriteria {
  /**
   * Base screen data - Common BMS header fields
   * Includes transaction name, program name, date, time, and titles
   */
  baseScreenData: BaseScreenData;
  
  /**
   * Search user ID - Maps to BMS USRIDIN field
   * User ID for locating user record to delete
   */
  searchUserId: string;
  
  /**
   * First name - Maps to BMS FNAME field (read-only)
   * User's first name displayed for confirmation
   */
  firstName: string;
  
  /**
   * Last name - Maps to BMS LNAME field (read-only)
   * User's last name displayed for confirmation
   */
  lastName: string;
  
  /**
   * User type - Maps to BMS USRTYPE field (read-only)
   * User role classification displayed for confirmation
   */
  userType: UserType;
  
  /**
   * Error message - Maps to BMS ERRMSG field
   * Search validation or system error message display
   */
  errorMessage: string;
  
  /**
   * Is user found - Search result indicator
   * Indicates whether the searched user was found in the system
   */
  isUserFound: boolean;
  
  /**
   * Read-only mode - Field protection indicator
   * Indicates that user data fields are display-only for confirmation
   */
  readOnlyMode: boolean;
  
  /**
   * Field attributes - BMS field attribute mapping
   * Controls field display characteristics and validation behavior
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

// ===================================================================
// USER AUDIT TRAIL INTERFACE
// ===================================================================

/**
 * User Audit Trail Interface
 * 
 * Comprehensive audit information for user management operations.
 * Supports compliance requirements and security monitoring through
 * detailed change tracking and version management.
 * 
 * Integrates with Spring Security authentication context and Spring Boot
 * Actuator audit events for complete audit trail management.
 */
export interface UserAuditTrail {
  /**
   * Created by - User who created the record
   * Maps to Spring Security authentication context
   */
  createdBy: string;
  
  /**
   * Created date - Record creation timestamp
   * ISO 8601 formatted date/time string
   */
  createdDate: string;
  
  /**
   * Last modified by - User who last modified the record
   * Maps to Spring Security authentication context
   */
  lastModifiedBy: string;
  
  /**
   * Last modified date - Record modification timestamp
   * ISO 8601 formatted date/time string
   */
  lastModifiedDate: string;
  
  /**
   * Version - Optimistic locking version number
   * Incremented with each update for concurrency control
   */
  version: number;
  
  /**
   * Change history - Detailed modification history
   * Array of change records for audit trail compliance
   */
  changeHistory: UserChangeRecord[];
}

/**
 * User Change Record Interface
 * 
 * Individual change record for detailed audit trail tracking.
 * Captures field-level changes with context information.
 */
export interface UserChangeRecord {
  /**
   * Change date - When the change occurred
   * ISO 8601 formatted date/time string
   */
  changeDate: string;
  
  /**
   * Changed by - User who made the change
   * Maps to Spring Security authentication context
   */
  changedBy: string;
  
  /**
   * Field name - Which field was changed
   * Database column name or logical field identifier
   */
  fieldName: string;
  
  /**
   * Old value - Previous field value
   * Sensitive data (passwords) should be masked
   */
  oldValue: string;
  
  /**
   * New value - Current field value
   * Sensitive data (passwords) should be masked
   */
  newValue: string;
  
  /**
   * Change reason - Business reason for the change
   * Optional explanation for the modification
   */
  changeReason?: string;
}

// ===================================================================
// USER MANAGEMENT VALIDATION SCHEMA
// ===================================================================

/**
 * User Management Validation Schema Interface
 * 
 * Comprehensive validation schema for all user management forms.
 * Integrates with FormValidationSchema to provide type-safe validation
 * across all user management operations.
 * 
 * Maps BMS field validation rules to modern React Hook Form validation
 * while preserving exact business logic from original COBOL/BMS implementation.
 */
export interface UserManagementValidationSchema {
  /**
   * User ID validation - USERID field validation rules
   * 8-character alphanumeric identifier with uniqueness checking
   */
  userIdValidation: FormValidationSchema<{ userId: string }>;
  
  /**
   * First name validation - FNAME field validation rules
   * 20-character text field with required validation
   */
  firstNameValidation: FormValidationSchema<{ firstName: string }>;
  
  /**
   * Last name validation - LNAME field validation rules
   * 20-character text field with required validation
   */
  lastNameValidation: FormValidationSchema<{ lastName: string }>;
  
  /**
   * Password validation - PASSWD field validation rules
   * 8-character password with complexity requirements
   */
  passwordValidation: FormValidationSchema<{ password: string }>;
  
  /**
   * User type validation - USRTYPE field validation rules
   * Single character validation (A=Admin, U=User)
   */
  userTypeValidation: FormValidationSchema<{ userType: UserType }>;
  
  /**
   * Form validation - Complete form validation schema
   * Cross-field validation and business rule enforcement
   */
  formValidation: FormValidationSchema<{
    userId: string;
    firstName: string;
    lastName: string;
    password: string;
    userType: UserType;
  }>;
}

// ===================================================================
// UTILITY TYPES AND CONSTANTS
// ===================================================================

/**
 * User Management Operation Types
 * 
 * Defines the types of operations available in user management.
 * Maps to Spring Security method-level authorization requirements.
 */
export type UserManagementOperation = 
  | 'LIST'    // COUSR00 - List users (requires ROLE_ADMIN)
  | 'CREATE'  // COUSR01 - Add user (requires ROLE_ADMIN)
  | 'UPDATE'  // COUSR02 - Update user (requires ROLE_ADMIN)
  | 'DELETE'; // COUSR03 - Delete user (requires ROLE_ADMIN)

/**
 * User Management Constants
 * 
 * Field length constants matching BMS field definitions.
 * Ensures consistent validation across all user management components.
 */
export const USER_MANAGEMENT_CONSTANTS = {
  USER_ID_LENGTH: 8,
  FIRST_NAME_LENGTH: 20,
  LAST_NAME_LENGTH: 20,
  PASSWORD_LENGTH: 8,
  USER_TYPE_LENGTH: 1,
  ERROR_MESSAGE_LENGTH: 78,
  USERS_PER_PAGE: 10,
  SELECTION_FIELD_LENGTH: 1
} as const;

/**
 * User Type Display Labels
 * 
 * Human-readable labels for user types.
 * Maps internal user type codes to display strings.
 */
export const USER_TYPE_LABELS = {
  A: 'Admin',
  U: 'User'
} as const;

/**
 * User Management Error Codes
 * 
 * Standardized error codes for user management operations.
 * Provides consistent error handling across all user management screens.
 */
export const USER_MANAGEMENT_ERROR_CODES = {
  USER_NOT_FOUND: 'USR001',
  DUPLICATE_USER_ID: 'USR002',
  INVALID_USER_TYPE: 'USR003',
  PASSWORD_VALIDATION_FAILED: 'USR004',
  INSUFFICIENT_PRIVILEGES: 'USR005',
  OPTIMISTIC_LOCK_FAILURE: 'USR006',
  VALIDATION_ERROR: 'USR007',
  SYSTEM_ERROR: 'USR008'
} as const;