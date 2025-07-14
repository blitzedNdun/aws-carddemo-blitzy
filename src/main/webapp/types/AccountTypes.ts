/**
 * AccountTypes.ts
 * 
 * TypeScript interface definitions for account management screens (COACTVW/COACTUP)
 * including account data structure, customer details, financial field precision types,
 * and form validation schemas matching the original BMS mapset layouts.
 * 
 * This file provides comprehensive type definitions for the Account View and Account Update
 * React components, ensuring exact functional equivalence with the original COBOL/BMS
 * implementation while supporting modern TypeScript development with financial precision.
 * 
 * Key Features:
 * - Exact BMS field mapping with preserved field lengths and validation rules
 * - BigDecimal-equivalent types for financial calculations matching PostgreSQL NUMERIC(12,2)
 * - Customer details nested structure replicating COBOL copybook layouts
 * - Form validation schemas with BMS PICIN and VALIDN attribute preservation
 * - Support for date field decomposition as used in COACTUP update forms
 * - SSN and phone number structured types for input validation
 * 
 * BMS Source Files:
 * - COACTVW.bms: Account View screen definition
 * - COACTUP.bms: Account Update screen definition
 * - COACTVW.CPY: Account View copybook structure
 * - COACTUP.CPY: Account Update copybook structure
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { FormFieldAttributes, BaseScreenData } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';

/**
 * Account ID Type
 * 
 * Represents the account identifier as used in ACCTSID field.
 * Length: 11 characters with PICIN='99999999999' validation.
 * Maps to PostgreSQL CHAR(11) field with numeric validation.
 */
export type AccountId = string;

/**
 * Account Amount Type
 * 
 * Represents monetary amounts with exact decimal precision matching COBOL COMP-3
 * and PostgreSQL NUMERIC(12,2) fields. Used for all financial calculations
 * including credit limits, balances, and transaction amounts.
 * 
 * This type ensures BigDecimal-equivalent precision for financial calculations
 * that must produce identical results to the original COBOL implementation.
 */
export type AccountAmount = string; // Stored as string to preserve exact precision

/**
 * Account Status Type
 * 
 * Represents the account status as a single character field matching
 * ACSTTUS field in BMS definition. Values: 'Y' (Active) or 'N' (Inactive).
 */
export type AccountStatus = 'Y' | 'N';

/**
 * Date Field Interface
 * 
 * Represents date components as used in COACTUP update form where dates
 * are decomposed into separate year, month, and day fields for user input.
 * Matches the BMS date field structure with validation and formatting.
 */
export interface DateField {
  /** Year component (4 digits) */
  year: string;
  /** Month component (2 digits, 01-12) */
  month: string;
  /** Day component (2 digits, 01-31) */
  day: string;
  /** Validation flag indicating if date is valid */
  isValid: boolean;
  /** Formatted date string for display (YYYY-MM-DD) */
  formattedDate: string;
}

/**
 * SSN (Social Security Number) Interface
 * 
 * Represents SSN as three separate components matching the COACTUP form
 * structure where SSN is input as three separate fields: ACTSSN1, ACTSSN2, ACTSSN3.
 * Provides validation and formatting for the complete SSN.
 */
export interface SSN {
  /** First part of SSN (3 digits) */
  part1: string;
  /** Second part of SSN (2 digits) */
  part2: string;
  /** Third part of SSN (4 digits) */
  part3: string;
  /** Formatted SSN string (XXX-XX-XXXX) */
  formattedSSN: string;
  /** Validation flag indicating if SSN is valid */
  isValid: boolean;
}

/**
 * Phone Number Interface
 * 
 * Represents phone number as three separate components matching the COACTUP form
 * structure where phone numbers are input as area code, exchange, and number.
 * Matches ACSPH1A/ACSPH1B/ACSPH1C and ACSPH2A/ACSPH2B/ACSPH2C fields.
 */
export interface PhoneNumber {
  /** Area code (3 digits) */
  areaCode: string;
  /** Exchange code (3 digits) */
  exchangeCode: string;
  /** Number (4 digits) */
  number: string;
  /** Formatted phone number string ((XXX) XXX-XXXX) */
  formattedNumber: string;
  /** Validation flag indicating if phone number is valid */
  isValid: boolean;
}

/**
 * Customer Details Data Interface
 * 
 * Represents the customer information section within the account data structure.
 * Maps to all customer-related fields from the BMS definition (ACSTNUM through ACSPFLG)
 * and preserves the exact field lengths and validation rules.
 */
export interface CustomerDetailsData {
  /** Customer ID (9 characters) - Maps to ACSTNUM */
  customerId: string;
  
  /** Customer SSN (12 characters formatted) - Maps to ACSTSSN */
  customerSSN: string;
  
  /** Date of birth (10 characters, YYYY-MM-DD) - Maps to ACSTDOB */
  dateOfBirth: string;
  
  /** FICO Score (3 characters) - Maps to ACSTFCO */
  ficoScore: string;
  
  /** First name (25 characters) - Maps to ACSFNAM */
  firstName: string;
  
  /** Middle name (25 characters) - Maps to ACSMNAM */
  middleName: string;
  
  /** Last name (25 characters) - Maps to ACSLNAM */
  lastName: string;
  
  /** Address line 1 (50 characters) - Maps to ACSADL1 */
  addressLine1: string;
  
  /** Address line 2 (50 characters) - Maps to ACSADL2 */
  addressLine2: string;
  
  /** City (50 characters) - Maps to ACSCITY */
  city: string;
  
  /** State (2 characters) - Maps to ACSSTTE */
  state: string;
  
  /** ZIP code (5 characters) - Maps to ACSZIPC */
  zipCode: string;
  
  /** Country (3 characters) - Maps to ACSCTRY */
  country: string;
  
  /** Phone number 1 (13 characters formatted) - Maps to ACSPHN1 */
  phoneNumber1: string;
  
  /** Phone number 2 (13 characters formatted) - Maps to ACSPHN2 */
  phoneNumber2: string;
  
  /** Government issued ID reference (20 characters) - Maps to ACSGOVT */
  governmentId: string;
  
  /** EFT account ID (10 characters) - Maps to ACSEFTC */
  eftAccountId: string;
  
  /** Primary card holder flag (1 character, Y/N) - Maps to ACSPFLG */
  isPrimaryCardHolder: AccountStatus;
}

/**
 * Account View Data Interface
 * 
 * Represents the complete data structure for the Account View screen (COACTVW).
 * Maps all fields from the BMS definition and preserves exact field lengths
 * and data types. Used by the Account View Service for displaying account
 * information in read-only format.
 */
export interface AccountViewData {
  /** Base screen data common to all BMS screens */
  baseScreenData: BaseScreenData;
  
  /** Account ID (11 characters) - Maps to ACCTSID */
  accountId: AccountId;
  
  /** Account status (1 character, Y/N) - Maps to ACSTTUS */
  accountStatus: AccountStatus;
  
  /** Date opened (10 characters, YYYY-MM-DD) - Maps to ADTOPEN */
  dateOpened: string;
  
  /** Expiry date (10 characters, YYYY-MM-DD) - Maps to AEXPDT */
  expiryDate: string;
  
  /** Reissue date (10 characters, YYYY-MM-DD) - Maps to AREISDT */
  reissueDate: string;
  
  /** Credit limit (15 characters, +ZZZ,ZZZ,ZZZ.99) - Maps to ACRDLIM */
  creditLimit: AccountAmount;
  
  /** Cash credit limit (15 characters, +ZZZ,ZZZ,ZZZ.99) - Maps to ACSHLIM */
  cashCreditLimit: AccountAmount;
  
  /** Current balance (15 characters, +ZZZ,ZZZ,ZZZ.99) - Maps to ACURBAL */
  currentBalance: AccountAmount;
  
  /** Current cycle credit (15 characters, +ZZZ,ZZZ,ZZZ.99) - Maps to ACRCYCR */
  currentCycleCredit: AccountAmount;
  
  /** Current cycle debit (15 characters, +ZZZ,ZZZ,ZZZ.99) - Maps to ACRCYDB */
  currentCycleDebit: AccountAmount;
  
  /** Account group (10 characters) - Maps to AADDGRP */
  accountGroup: string;
  
  /** Complete customer details structure */
  customerDetails: CustomerDetailsData;
  
  /** Information message (45 characters) - Maps to INFOMSG */
  informationMessage?: string;
  
  /** Error message (78 characters) - Maps to ERRMSG */
  errorMessage?: string;
  
  /** Field attributes for dynamic form behavior */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Account Update Form Data Interface
 * 
 * Represents the complete data structure for the Account Update screen (COACTUP).
 * Maps all editable fields from the BMS definition with support for field-level
 * validation and change tracking. Used by the Account Update Service for
 * processing account modifications.
 */
export interface AccountUpdateFormData {
  /** Base screen data common to all BMS screens */
  baseScreenData: BaseScreenData;
  
  /** Account ID (11 characters) - Maps to ACCTSID */
  accountId: AccountId;
  
  /** Account status (1 character, Y/N) - Maps to ACSTTUS */
  accountStatus: AccountStatus;
  
  /** Open date year (4 digits) - Maps to OPNYEAR */
  openYear: string;
  
  /** Open date month (2 digits) - Maps to OPNMON */
  openMonth: string;
  
  /** Open date day (2 digits) - Maps to OPNDAY */
  openDay: string;
  
  /** Expiry date year (4 digits) - Maps to EXPYEAR */
  expiryYear: string;
  
  /** Expiry date month (2 digits) - Maps to EXPMON */
  expiryMonth: string;
  
  /** Expiry date day (2 digits) - Maps to EXPDAY */
  expiryDay: string;
  
  /** Reissue date year (4 digits) - Maps to RISYEAR */
  reissueYear: string;
  
  /** Reissue date month (2 digits) - Maps to RISMON */
  reissueMonth: string;
  
  /** Reissue date day (2 digits) - Maps to RISDAY */
  reissueDay: string;
  
  /** Credit limit (15 characters) - Maps to ACRDLIM */
  creditLimit: AccountAmount;
  
  /** Cash credit limit (15 characters) - Maps to ACSHLIM */
  cashCreditLimit: AccountAmount;
  
  /** Current balance (15 characters) - Maps to ACURBAL */
  currentBalance: AccountAmount;
  
  /** Current cycle credit (15 characters) - Maps to ACRCYCR */
  currentCycleCredit: AccountAmount;
  
  /** Current cycle debit (15 characters) - Maps to ACRCYDB */
  currentCycleDebit: AccountAmount;
  
  /** Account group (10 characters) - Maps to AADDGRP */
  accountGroup: string;
  
  /** Customer ID (9 characters) - Maps to ACSTNUM */
  customerId: string;
  
  /** SSN part 1 (3 digits) - Maps to ACTSSN1 */
  ssnPart1: string;
  
  /** SSN part 2 (2 digits) - Maps to ACTSSN2 */
  ssnPart2: string;
  
  /** SSN part 3 (4 digits) - Maps to ACTSSN3 */
  ssnPart3: string;
  
  /** Date of birth year (4 digits) - Maps to DOBYEAR */
  dobYear: string;
  
  /** Date of birth month (2 digits) - Maps to DOBMON */
  dobMonth: string;
  
  /** Date of birth day (2 digits) - Maps to DOBDAY */
  dobDay: string;
  
  /** FICO score (3 characters) - Maps to ACSTFCO */
  ficoScore: string;
  
  /** First name (25 characters) - Maps to ACSFNAM */
  firstName: string;
  
  /** Middle name (25 characters) - Maps to ACSMNAM */
  middleName: string;
  
  /** Last name (25 characters) - Maps to ACSLNAM */
  lastName: string;
  
  /** Address line 1 (50 characters) - Maps to ACSADL1 */
  addressLine1: string;
  
  /** Address line 2 (50 characters) - Maps to ACSADL2 */
  addressLine2: string;
  
  /** City (50 characters) - Maps to ACSCITY */
  city: string;
  
  /** State (2 characters) - Maps to ACSSTTE */
  state: string;
  
  /** ZIP code (5 characters) - Maps to ACSZIPC */
  zipCode: string;
  
  /** Country (3 characters) - Maps to ACSCTRY */
  country: string;
  
  /** Phone 1 area code (3 digits) - Maps to ACSPH1A */
  phone1Area: string;
  
  /** Phone 1 exchange (3 digits) - Maps to ACSPH1B */
  phone1Exchange: string;
  
  /** Phone 1 number (4 digits) - Maps to ACSPH1C */
  phone1Number: string;
  
  /** Phone 2 area code (3 digits) - Maps to ACSPH2A */
  phone2Area: string;
  
  /** Phone 2 exchange (3 digits) - Maps to ACSPH2B */
  phone2Exchange: string;
  
  /** Phone 2 number (4 digits) - Maps to ACSPH2C */
  phone2Number: string;
  
  /** Government issued ID reference (20 characters) - Maps to ACSGOVT */
  governmentId: string;
  
  /** EFT account ID (10 characters) - Maps to ACSEFTC */
  eftAccountId: string;
  
  /** Primary card holder flag (1 character, Y/N) - Maps to ACSPFLG */
  isPrimaryCardHolder: AccountStatus;
  
  /** Information message (45 characters) - Maps to INFOMSG */
  informationMessage?: string;
  
  /** Error message (78 characters) - Maps to ERRMSG */
  errorMessage?: string;
  
  /** Validation result for form submission */
  validationResult?: ValidationResult;
  
  /** Field attributes for dynamic form behavior */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Account Validation Schema Interface
 * 
 * Defines comprehensive validation rules for account form fields matching
 * the original BMS validation behavior while providing enhanced client-side
 * validation capabilities for the React components.
 */
export interface AccountValidationSchema {
  /** Account ID validation rules */
  accountIdValidation: {
    required: boolean;
    length: { min: number; max: number };
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** Account status validation rules */
  accountStatusValidation: {
    required: boolean;
    allowedValues: AccountStatus[];
    errorMessage: string;
  };
  
  /** Credit limit validation rules */
  creditLimitValidation: {
    required: boolean;
    min: number;
    max: number;
    precision: number;
    scale: number;
    errorMessage: string;
  };
  
  /** Cash credit limit validation rules */
  cashCreditLimitValidation: {
    required: boolean;
    min: number;
    max: number;
    precision: number;
    scale: number;
    errorMessage: string;
  };
  
  /** Current balance validation rules */
  currentBalanceValidation: {
    required: boolean;
    precision: number;
    scale: number;
    errorMessage: string;
  };
  
  /** Customer ID validation rules */
  customerIdValidation: {
    required: boolean;
    length: { min: number; max: number };
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** SSN validation rules */
  ssnValidation: {
    required: boolean;
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** Name field validation rules */
  nameValidation: {
    required: boolean;
    length: { min: number; max: number };
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** Address validation rules */
  addressValidation: {
    required: boolean;
    length: { min: number; max: number };
    errorMessage: string;
  };
  
  /** Phone number validation rules */
  phoneValidation: {
    required: boolean;
    pattern: RegExp;
    errorMessage: string;
  };
  
  /** Date field validation rules */
  dateValidation: {
    required: boolean;
    minDate: Date;
    maxDate: Date;
    errorMessage: string;
  };
  
  /** Form-level validation rules */
  formValidation: {
    validateCreditLimits: boolean;
    validateDateSequence: boolean;
    validateCustomerData: boolean;
    errorMessage: string;
  };
}