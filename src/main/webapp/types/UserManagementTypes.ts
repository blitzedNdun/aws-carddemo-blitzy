/**
 * UserManagementTypes.ts
 * 
 * TypeScript interface definitions for user management screens (COUSR00-03) including user list data,
 * create/update forms, search criteria, and role-based access control types matching Spring Security
 * requirements. These interfaces provide exact functional equivalence with original BMS mapsets while
 * enabling modern React component development with type safety.
 * 
 * Transformed from:
 * - COUSR00.bms (List Users) -> UserListData interface
 * - COUSR01.bms (Add User) -> UserCreateFormData interface  
 * - COUSR02.bms (Update User) -> UserUpdateFormData interface
 * - COUSR03.bms (Delete User) -> UserSearchCriteria interface
 * 
 * Maintains exact field structures, lengths, and validation rules from original COBOL BMS definitions
 * while providing React Hook Form integration and Material-UI compatibility.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { BaseScreenData } from './CommonTypes';
import { FormValidationSchema } from './ValidationTypes';

/**
 * User Type Definition
 * Maps to COBOL 88-level conditions for user types from original USRSEC file structure
 * Based on BMS analysis: A=Admin, U=User from COUSR01.bms USRTYPE field
 */
export type UserType = 'A' | 'U';

/**
 * User Row Data Interface
 * Represents individual user record in the list display (COUSR00.bms)
 * Maps to BMS fields: SEL, USRID, FNAME, LNAME, UTYPE (rows 01-10)
 * Maintains exact field lengths from original copybook structure
 */
export interface UserRowData {
  /** Selection field (1 character) - 'U' for Update, 'D' for Delete */
  selection: string;
  
  /** User ID (8 characters) - from USRID fields in BMS */
  userId: string;
  
  /** First name (20 characters) - from FNAME fields in BMS */
  firstName: string;
  
  /** Last name (20 characters) - from LNAME fields in BMS */
  lastName: string;
  
  /** User type (1 character) - 'A' for Admin, 'U' for User */
  userType: UserType;
  
  /** Indicates if row is selected for batch operations */
  isSelected: boolean;
  
  /** Controls row visibility for filtering operations */
  isVisible: boolean;
}

/**
 * User List Data Interface
 * Main interface for COUSR00.bms (List Users screen)
 * Provides user list display with pagination, search, and selection capabilities
 * Maintains exact BMS field structure with 10-row display limitation
 */
export interface UserListData {
  /** Common BMS header fields from BaseScreenData */
  baseScreenData: BaseScreenData;
  
  /** Current page number (8 characters) - from PAGENUM field in BMS */
  pageNumber: string;
  
  /** Search user ID input (8 characters) - from USRIDIN field in BMS */
  searchUserId: string;
  
  /** Array of user rows (maximum 10 rows) - from SEL/USRID/FNAME/LNAME/UTYPE pattern */
  userRows: UserRowData[];
  
  /** Total number of users in system for pagination calculation */
  totalUsers: number;
  
  /** Indicates if more pages are available for forward navigation */
  hasMorePages: boolean;
  
  /** Array of selected user IDs for batch operations */
  selectedUsers: string[];
  
  /** Error message display (78 characters) - from ERRMSG field in BMS */
  errorMessage: string;
  
  /** Field attributes for React component rendering and validation */
  fieldAttributes: {
    pageNumber: FormFieldAttributes;
    searchUserId: FormFieldAttributes;
    userRows: FormFieldAttributes[];
    errorMessage: FormFieldAttributes;
  };
}

/**
 * User Create Form Data Interface
 * Main interface for COUSR01.bms (Add User screen)
 * Provides complete user creation form with validation and password policy enforcement
 * Maintains exact BMS field structure and validation rules
 */
export interface UserCreateFormData {
  /** Common BMS header fields from BaseScreenData */
  baseScreenData: BaseScreenData;
  
  /** First name input (20 characters) - from FNAME field in BMS with UNPROT, IC attributes */
  firstName: string;
  
  /** Last name input (20 characters) - from LNAME field in BMS with UNPROT attributes */
  lastName: string;
  
  /** User ID input (8 characters) - from USERID field in BMS with UNPROT attributes */
  userId: string;
  
  /** Password input (8 characters) - from PASSWD field in BMS with DRK, UNPROT attributes */
  password: string;
  
  /** User type selection (1 character) - from USRTYPE field with A=Admin, U=User values */
  userType: UserType;
  
  /** Error message display (78 characters) - from ERRMSG field in BMS */
  errorMessage: string;
  
  /** Form validation result for comprehensive error handling */
  validationResult: ValidationResult;
  
  /** Field attributes for React component rendering and validation */
  fieldAttributes: {
    firstName: FormFieldAttributes;
    lastName: FormFieldAttributes;
    userId: FormFieldAttributes;
    password: FormFieldAttributes;
    userType: FormFieldAttributes;
    errorMessage: FormFieldAttributes;
  };
}

/**
 * User Audit Trail Interface
 * Tracks user record modifications for compliance and security auditing
 * Implements optimistic locking pattern for concurrent update protection
 */
export interface UserAuditTrail {
  /** User who created the record */
  createdBy: string;
  
  /** Timestamp when record was created */
  createdDate: Date;
  
  /** User who last modified the record */
  lastModifiedBy: string;
  
  /** Timestamp of last modification */
  lastModifiedDate: Date;
  
  /** Version number for optimistic locking */
  version: number;
  
  /** Array of change history entries for audit trail */
  changeHistory: Array<{
    timestamp: Date;
    userId: string;
    action: 'CREATE' | 'UPDATE' | 'DELETE';
    fieldChanges: Record<string, { oldValue: any; newValue: any }>;
  }>;
}

/**
 * User Update Form Data Interface
 * Main interface for COUSR02.bms (Update User screen)
 * Provides two-phase operation: search user, then update fields
 * Includes optimistic locking and audit trail support
 */
export interface UserUpdateFormData {
  /** Common BMS header fields from BaseScreenData */
  baseScreenData: BaseScreenData;
  
  /** Search user ID input (8 characters) - from USRIDIN field in BMS with IC attribute */
  searchUserId: string;
  
  /** First name input (20 characters) - from FNAME field, populated after search */
  firstName: string;
  
  /** Last name input (20 characters) - from LNAME field, populated after search */
  lastName: string;
  
  /** Password input (8 characters) - from PASSWD field with DRK attribute */
  password: string;
  
  /** User type selection (1 character) - from USRTYPE field */
  userType: UserType;
  
  /** Error message display (78 characters) - from ERRMSG field in BMS */
  errorMessage: string;
  
  /** Form validation result for comprehensive error handling */
  validationResult: ValidationResult;
  
  /** Indicates if user was found during search phase */
  isUserFound: boolean;
  
  /** Version number for optimistic locking protection */
  optimisticLockVersion: number;
  
  /** Complete audit trail for the user record */
  auditTrail: UserAuditTrail;
  
  /** Field attributes for React component rendering and validation */
  fieldAttributes: {
    searchUserId: FormFieldAttributes;
    firstName: FormFieldAttributes;
    lastName: FormFieldAttributes;
    password: FormFieldAttributes;
    userType: FormFieldAttributes;
    errorMessage: FormFieldAttributes;
  };
}

/**
 * User Search Criteria Interface
 * Main interface for COUSR03.bms (Delete User screen)
 * Provides user search and read-only display for deletion confirmation
 * Fields are populated as read-only after successful search
 */
export interface UserSearchCriteria {
  /** Common BMS header fields from BaseScreenData */
  baseScreenData: BaseScreenData;
  
  /** Search user ID input (8 characters) - from USRIDIN field in BMS */
  searchUserId: string;
  
  /** First name display (20 characters) - from FNAME field with ASKIP attribute */
  firstName: string;
  
  /** Last name display (20 characters) - from LNAME field with ASKIP attribute */
  lastName: string;
  
  /** User type display (1 character) - from USRTYPE field with ASKIP attribute */
  userType: UserType;
  
  /** Error message display (78 characters) - from ERRMSG field in BMS */
  errorMessage: string;
  
  /** Indicates if user was found during search operation */
  isUserFound: boolean;
  
  /** Controls form read-only mode (true for delete screen) */
  readOnlyMode: boolean;
  
  /** Field attributes for React component rendering and validation */
  fieldAttributes: {
    searchUserId: FormFieldAttributes;
    firstName: FormFieldAttributes;
    lastName: FormFieldAttributes;
    userType: FormFieldAttributes;
    errorMessage: FormFieldAttributes;
  };
}

/**
 * User Management Validation Schema Interface
 * Comprehensive validation schema for all user management forms
 * Implements password policy, business rules, and field-level validation
 * Integrates with React Hook Form and Yup validation library
 */
export interface UserManagementValidationSchema {
  /** User ID validation rules (8 characters, alphanumeric, required) */
  userIdValidation: {
    pattern: RegExp;
    required: boolean;
    minLength: number;
    maxLength: number;
    errorMessage: string;
  };
  
  /** First name validation rules (20 characters maximum, required) */
  firstNameValidation: {
    required: boolean;
    maxLength: number;
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** Last name validation rules (20 characters maximum, required) */
  lastNameValidation: {
    required: boolean;
    maxLength: number;
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** Password validation rules (8 characters exact, complexity requirements) */
  passwordValidation: {
    required: boolean;
    exactLength: number;
    pattern: RegExp;
    complexity: {
      requireUppercase: boolean;
      requireLowercase: boolean;
      requireNumbers: boolean;
      requireSpecialChars: boolean;
    };
    errorMessage: string;
  };
  
  /** User type validation rules (A or U only) */
  userTypeValidation: {
    required: boolean;
    allowedValues: UserType[];
    errorMessage: string;
  };
  
  /** Form-level validation schema combining all field rules */
  formValidation: FormValidationSchema<
    UserCreateFormData | UserUpdateFormData | UserSearchCriteria
  >;
}

// Re-export necessary types from dependencies for convenience
export type { BaseScreenData } from './CommonTypes';
export type { FormValidationSchema } from './ValidationTypes';

// Additional type definitions for form field attributes
interface FormFieldAttributes {
  attrb: BmsAttributeType | BmsAttributeType[];
  color: BmsColorType;
  hilight: BmsHighlightType;
  length: number;
  pos: FieldPosition;
  initial?: string;
  picin?: string;
  validn?: string;
}

interface FieldPosition {
  row: number;
  column: number;
}

type BmsAttributeType = 
  | 'ASKIP' | 'UNPROT' | 'PROT' | 'NORM' | 'BRT' 
  | 'FSET' | 'IC' | 'DRK' | 'NUM';

type BmsColorType = 
  | 'BLUE' | 'YELLOW' | 'GREEN' | 'RED' | 'TURQUOISE' | 'NEUTRAL';

type BmsHighlightType = 
  | 'OFF' | 'UNDERLINE' | 'BLINK' | 'REVERSE';

interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  fieldErrors: Record<string, string>;
  warnings: string[];
}

interface ValidationError {
  field: string;
  message: string;
  code: string;
}