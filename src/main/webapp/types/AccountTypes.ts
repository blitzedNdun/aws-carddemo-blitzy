/**
 * AccountTypes.ts
 * 
 * TypeScript interface definitions for account management screens (COACTVW/COACTUP) including
 * account data structure, customer details, financial field precision types, and form validation
 * schemas matching the original BMS mapset layouts.
 * 
 * This file maintains exact functional equivalence with original COBOL BMS definitions from:
 * - app/bms/COACTVW.bms (Account View Screen)
 * - app/bms/COACTUP.bms (Account Update Screen)
 * - app/cpy-bms/COACTVW.CPY (Account View Copybook)
 * - app/cpy-bms/COACTUP.CPY (Account Update Copybook)
 * 
 * Key mappings preserve:
 * - ACCTSID (11-char account number) → AccountId with validation
 * - Financial fields with COMP-3 precision → AccountAmount with BigDecimal equivalence
 * - Customer nested structure → CustomerDetailsData interface
 * - BMS field attributes → FormFieldAttributes integration
 * - Date field decomposition → DateField interface for year/month/day
 * - Phone number breakdown → PhoneNumber interface for area/exchange/number
 * - SSN segmentation → SSN interface for 3-part structure
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { FormFieldAttributes, BaseScreenData } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';

/**
 * Account ID Type Definition
 * Maps BMS ACCTSID field with 11-character numeric account identifier
 * Based on COACTVW.bms ACCTSID field: LENGTH=11, PICIN='99999999999', VALIDN=(MUSTFILL)
 * Ensures type safety for account identification across all account-related operations
 */
export type AccountId = string;

/**
 * Account Amount Type Definition
 * Financial amount type with BigDecimal precision matching PostgreSQL NUMERIC(12,2)
 * Preserves exact COBOL COMP-3 decimal arithmetic precision from original system
 * Based on BMS financial fields: ACRDLIM, ACSHLIM, ACURBAL, ACRCYCR, ACRCYDB
 * All use LENGTH=15 with PICOUT='+ZZZ,ZZZ,ZZZ.99' for display formatting
 */
export type AccountAmount = string;

/**
 * Account Status Type Definition
 * Maps BMS ACSTTUS field for account active/inactive status
 * Based on COACTVW.bms ACSTTUS field: LENGTH=1, possible values 'Y' or 'N'
 * Maintains exact business logic from original COBOL validation
 */
export type AccountStatus = 'Y' | 'N' | 'A' | 'I' | 'C' | 'S';

/**
 * Date Field Interface
 * Handles BMS date field decomposition from single 10-character field to year/month/day components
 * Based on COACTUP.bms date field breakdown:
 * - OPNYEAR/EXPYEAR/RISYEAR: LENGTH=4 (YYYY format)
 * - OPNMON/EXPMON/RISMON: LENGTH=2 (MM format)
 * - OPNDAY/EXPDAY/RISDAY: LENGTH=2 (DD format)
 * Provides validation and formatted display capabilities
 */
export interface DateField {
  /** Year component (4 digits, YYYY format) */
  year: string;
  /** Month component (2 digits, MM format, 01-12 range) */
  month: string;
  /** Day component (2 digits, DD format, 01-31 range) */
  day: string;
  /** Computed validation status for the complete date */
  isValid: boolean;
  /** Formatted date string for display (YYYY-MM-DD or MM/DD/YYYY) */
  formattedDate: string;
}

/**
 * Phone Number Interface
 * Handles BMS phone number field decomposition from single 13-character field to area/exchange/number
 * Based on COACTUP.bms phone field breakdown:
 * - ACSPH1A/ACSPH2A: LENGTH=3 (area code)
 * - ACSPH1B/ACSPH2B: LENGTH=3 (exchange code)
 * - ACSPH1C/ACSPH2C: LENGTH=4 (number)
 * Provides formatting and validation for North American phone numbers
 */
export interface PhoneNumber {
  /** Area code (3 digits) */
  areaCode: string;
  /** Exchange code (3 digits) */
  exchangeCode: string;
  /** Number (4 digits) */
  number: string;
  /** Formatted phone number for display ((###) ###-####) */
  formattedNumber: string;
  /** Validation status for the complete phone number */
  isValid: boolean;
}

/**
 * SSN Interface
 * Handles BMS SSN field decomposition from single 12-character field to 3-part structure
 * Based on COACTUP.bms SSN field breakdown:
 * - ACTSSN1: LENGTH=3 (first 3 digits)
 * - ACTSSN2: LENGTH=2 (middle 2 digits)
 * - ACTSSN3: LENGTH=4 (last 4 digits)
 * Provides formatting and validation for Social Security Numbers
 */
export interface SSN {
  /** First part of SSN (3 digits) */
  part1: string;
  /** Middle part of SSN (2 digits) */
  part2: string;
  /** Last part of SSN (4 digits) */
  part3: string;
  /** Formatted SSN for display (###-##-####) */
  formattedSSN: string;
  /** Validation status for the complete SSN */
  isValid: boolean;
}

/**
 * Customer Details Data Interface
 * Nested customer information structure within account data
 * Maps all customer-related fields from COACTVW.bms customer section:
 * - Personal information (names, SSN, DOB, FICO)
 * - Address information (street, city, state, ZIP, country)
 * - Contact information (phone numbers, government ID)
 * - Account relationship (EFT account, primary cardholder flag)
 * 
 * Field mappings preserve exact BMS lengths and validation:
 * - Names: 25 characters each (ACSFNAM, ACSMNAM, ACSLNAM)
 * - Address lines: 50 characters each (ACSADL1, ACSADL2)
 * - City: 50 characters (ACSCITY)
 * - State: 2 characters (ACSSTTE)
 * - ZIP: 5 characters (ACSZIPC)
 * - Country: 3 characters (ACSCTRY)
 */
export interface CustomerDetailsData {
  /** Customer ID (9 characters) - ACSTNUM field */
  customerId: string;
  /** Customer SSN with 3-part structure - ACSTSSN decomposition */
  customerSSN: SSN;
  /** Date of birth with year/month/day breakdown - ACSTDOB field */
  dateOfBirth: DateField;
  /** FICO credit score (3 digits) - ACSTFCO field */
  ficoScore: string;
  /** First name (25 characters) - ACSFNAM field */
  firstName: string;
  /** Middle name (25 characters) - ACSMNAM field */
  middleName: string;
  /** Last name (25 characters) - ACSLNAM field */
  lastName: string;
  /** Address line 1 (50 characters) - ACSADL1 field */
  addressLine1: string;
  /** Address line 2 (50 characters) - ACSADL2 field */
  addressLine2: string;
  /** City (50 characters) - ACSCITY field */
  city: string;
  /** State (2 characters) - ACSSTTE field */
  state: string;
  /** ZIP code (5 characters) - ACSZIPC field */
  zipCode: string;
  /** Country code (3 characters) - ACSCTRY field */
  country: string;
  /** Primary phone number with area/exchange/number breakdown - ACSPHN1 field */
  phoneNumber1: PhoneNumber;
  /** Secondary phone number with area/exchange/number breakdown - ACSPHN2 field */
  phoneNumber2: PhoneNumber;
  /** Government issued ID reference (20 characters) - ACSGOVT field */
  governmentId: string;
  /** EFT account ID (10 characters) - ACSEFTC field */
  eftAccountId: string;
  /** Primary cardholder flag (Y/N) - ACSPFLG field */
  isPrimaryCardHolder: boolean;
}

/**
 * Account View Data Interface
 * Complete account information structure for the COACTVW screen
 * Maps all fields from COACTVW.bms preserving exact field structure and validation
 * 
 * Key financial fields maintain BigDecimal precision:
 * - creditLimit: ACRDLIM with PICOUT='+ZZZ,ZZZ,ZZZ.99'
 * - cashCreditLimit: ACSHLIM with PICOUT='+ZZZ,ZZZ,ZZZ.99'
 * - currentBalance: ACURBAL with PICOUT='+ZZZ,ZZZ,ZZZ.99'
 * - currentCycleCredit: ACRCYCR with PICOUT='+ZZZ,ZZZ,ZZZ.99'
 * - currentCycleDebit: ACRCYDB with PICOUT='+ZZZ,ZZZ,ZZZ.99'
 * 
 * Date fields use formatted display:
 * - dateOpened: ADTOPEN 10-character date
 * - expiryDate: AEXPDT 10-character date
 * - reissueDate: AREISDT 10-character date
 */
export interface AccountViewData {
  /** Base screen data (transaction, program, date, time, titles) */
  baseScreenData: BaseScreenData;
  /** Account ID (11 characters) - ACCTSID field with MUSTFILL validation */
  accountId: AccountId;
  /** Account status (Y/N/A/I/C/S) - ACSTTUS field */
  accountStatus: AccountStatus;
  /** Date account was opened (10 characters) - ADTOPEN field */
  dateOpened: string;
  /** Account expiry date (10 characters) - AEXPDT field */
  expiryDate: string;
  /** Card reissue date (10 characters) - AREISDT field */
  reissueDate: string;
  /** Credit limit with BigDecimal precision - ACRDLIM field */
  creditLimit: AccountAmount;
  /** Cash credit limit with BigDecimal precision - ACSHLIM field */
  cashCreditLimit: AccountAmount;
  /** Current balance with BigDecimal precision - ACURBAL field */
  currentBalance: AccountAmount;
  /** Current cycle credit with BigDecimal precision - ACRCYCR field */
  currentCycleCredit: AccountAmount;
  /** Current cycle debit with BigDecimal precision - ACRCYDB field */
  currentCycleDebit: AccountAmount;
  /** Account group (10 characters) - AADDGRP field */
  accountGroup: string;
  /** Nested customer details structure */
  customerDetails: CustomerDetailsData;
  /** Information message (45 characters) - INFOMSG field */
  informationMessage: string;
  /** Error message (78 characters) - ERRMSG field */
  errorMessage: string;
  /** BMS field attributes for form field rendering and validation */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Account Update Form Data Interface
 * Complete form data structure for the COACTUP screen with all editable fields
 * Maps all input fields from COACTUP.bms preserving UNPROT field structure
 * 
 * Key differences from AccountViewData:
 * - Date fields decomposed into year/month/day components for editing
 * - SSN broken down into 3-part structure for input validation
 * - Phone numbers decomposed into area/exchange/number for formatted input
 * - All financial fields maintain BigDecimal precision with validation
 * - Additional form validation and submission state management
 */
export interface AccountUpdateFormData {
  /** Base screen data (transaction, program, date, time, titles) */
  baseScreenData: BaseScreenData;
  /** Account ID (11 characters) - ACCTSID field, may be pre-filled */
  accountId: AccountId;
  /** Account status (Y/N/A/I/C/S) - ACSTTUS field with UNPROT attribute */
  accountStatus: AccountStatus;
  /** Account opened year (4 digits) - OPNYEAR field */
  openYear: string;
  /** Account opened month (2 digits) - OPNMON field */
  openMonth: string;
  /** Account opened day (2 digits) - OPNDAY field */
  openDay: string;
  /** Account expiry year (4 digits) - EXPYEAR field */
  expiryYear: string;
  /** Account expiry month (2 digits) - EXPMON field */
  expiryMonth: string;
  /** Account expiry day (2 digits) - EXPDAY field */
  expiryDay: string;
  /** Card reissue year (4 digits) - RISYEAR field */
  reissueYear: string;
  /** Card reissue month (2 digits) - RISMON field */
  reissueMonth: string;
  /** Card reissue day (2 digits) - RISDAY field */
  reissueDay: string;
  /** Credit limit with BigDecimal precision - ACRDLIM field with UNPROT */
  creditLimit: AccountAmount;
  /** Cash credit limit with BigDecimal precision - ACSHLIM field with UNPROT */
  cashCreditLimit: AccountAmount;
  /** Current balance with BigDecimal precision - ACURBAL field with UNPROT */
  currentBalance: AccountAmount;
  /** Current cycle credit with BigDecimal precision - ACRCYCR field with UNPROT */
  currentCycleCredit: AccountAmount;
  /** Current cycle debit with BigDecimal precision - ACRCYDB field with UNPROT */
  currentCycleDebit: AccountAmount;
  /** Account group (10 characters) - AADDGRP field with UNPROT */
  accountGroup: string;
  /** Customer ID (9 characters) - ACSTNUM field with UNPROT */
  customerId: string;
  /** SSN part 1 (3 digits) - ACTSSN1 field */
  ssnPart1: string;
  /** SSN part 2 (2 digits) - ACTSSN2 field */
  ssnPart2: string;
  /** SSN part 3 (4 digits) - ACTSSN3 field */
  ssnPart3: string;
  /** Date of birth year (4 digits) - DOBYEAR field */
  dobYear: string;
  /** Date of birth month (2 digits) - DOBMON field */
  dobMonth: string;
  /** Date of birth day (2 digits) - DOBDAY field */
  dobDay: string;
  /** FICO credit score (3 digits) - ACSTFCO field with UNPROT */
  ficoScore: string;
  /** First name (25 characters) - ACSFNAM field with UNPROT */
  firstName: string;
  /** Middle name (25 characters) - ACSMNAM field with UNPROT */
  middleName: string;
  /** Last name (25 characters) - ACSLNAM field with UNPROT */
  lastName: string;
  /** Address line 1 (50 characters) - ACSADL1 field with UNPROT */
  addressLine1: string;
  /** Address line 2 (50 characters) - ACSADL2 field with UNPROT */
  addressLine2: string;
  /** City (50 characters) - ACSCITY field with UNPROT */
  city: string;
  /** State (2 characters) - ACSSTTE field with UNPROT */
  state: string;
  /** ZIP code (5 characters) - ACSZIPC field with UNPROT */
  zipCode: string;
  /** Country code (3 characters) - ACSCTRY field with UNPROT */
  country: string;
  /** Phone 1 area code (3 digits) - ACSPH1A field */
  phone1Area: string;
  /** Phone 1 exchange (3 digits) - ACSPH1B field */
  phone1Exchange: string;
  /** Phone 1 number (4 digits) - ACSPH1C field */
  phone1Number: string;
  /** Phone 2 area code (3 digits) - ACSPH2A field */
  phone2Area: string;
  /** Phone 2 exchange (3 digits) - ACSPH2B field */
  phone2Exchange: string;
  /** Phone 2 number (4 digits) - ACSPH2C field */
  phone2Number: string;
  /** Government issued ID reference (20 characters) - ACSGOVT field with UNPROT */
  governmentId: string;
  /** EFT account ID (10 characters) - ACSEFTC field with UNPROT */
  eftAccountId: string;
  /** Primary cardholder flag (Y/N) - ACSPFLG field with UNPROT */
  isPrimaryCardHolder: boolean;
  /** Information message (45 characters) - INFOMSG field */
  informationMessage: string;
  /** Error message (78 characters) - ERRMSG field */
  errorMessage: string;
  /** Form validation result from React Hook Form integration */
  validationResult: ValidationResult;
  /** BMS field attributes for form field rendering and validation */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

/**
 * Account Validation Schema Interface
 * Comprehensive validation rules for account management forms
 * Implements all BMS validation constraints and business rules from original COBOL logic
 * 
 * Validation categories:
 * - Field-level validation (length, format, required)
 * - Cross-field validation (date ranges, balance limits)
 * - Business rule validation (credit limits, account status)
 * - Data integrity validation (customer references, account relationships)
 */
export interface AccountValidationSchema {
  /** Account ID validation rules (11-char numeric, MUSTFILL) */
  accountIdValidation: {
    required: boolean;
    pattern: RegExp;
    length: { min: number; max: number };
    errorMessage: string;
  };
  /** Account status validation rules (valid status codes) */
  accountStatusValidation: {
    required: boolean;
    allowedValues: AccountStatus[];
    errorMessage: string;
  };
  /** Credit limit validation rules (numeric, positive, within bounds) */
  creditLimitValidation: {
    required: boolean;
    min: number;
    max: number;
    precision: number;
    scale: number;
    errorMessage: string;
  };
  /** Cash credit limit validation rules (numeric, <= credit limit) */
  cashCreditLimitValidation: {
    required: boolean;
    min: number;
    max: number;
    precision: number;
    scale: number;
    mustBeLessThanOrEqualTo: string; // field reference
    errorMessage: string;
  };
  /** Current balance validation rules (numeric, can be negative) */
  currentBalanceValidation: {
    required: boolean;
    precision: number;
    scale: number;
    errorMessage: string;
  };
  /** Customer ID validation rules (9-char alphanumeric) */
  customerIdValidation: {
    required: boolean;
    pattern: RegExp;
    length: { min: number; max: number };
    errorMessage: string;
  };
  /** SSN validation rules (9-digit numeric with format ###-##-####) */
  ssnValidation: {
    required: boolean;
    pattern: RegExp;
    parts: {
      part1: { length: number; pattern: RegExp };
      part2: { length: number; pattern: RegExp };
      part3: { length: number; pattern: RegExp };
    };
    errorMessage: string;
  };
  /** Name field validation rules (25-char alphabetic with spaces) */
  nameValidation: {
    required: boolean;
    pattern: RegExp;
    length: { min: number; max: number };
    errorMessage: string;
  };
  /** Address field validation rules (50-char alphanumeric) */
  addressValidation: {
    required: boolean;
    length: { min: number; max: number };
    errorMessage: string;
  };
  /** Phone number validation rules (10-digit North American format) */
  phoneValidation: {
    required: boolean;
    pattern: RegExp;
    parts: {
      areaCode: { length: number; pattern: RegExp };
      exchangeCode: { length: number; pattern: RegExp };
      number: { length: number; pattern: RegExp };
    };
    errorMessage: string;
  };
  /** Date field validation rules (YYYY-MM-DD format with range checks) */
  dateValidation: {
    required: boolean;
    minDate: Date;
    maxDate: Date;
    parts: {
      year: { length: number; min: number; max: number };
      month: { length: number; min: number; max: number };
      day: { length: number; min: number; max: number };
    };
    errorMessage: string;
  };
  /** Form-level validation rules (cross-field dependencies) */
  formValidation: {
    /** Credit limit must be greater than or equal to cash credit limit */
    creditLimitRelationship: boolean;
    /** Account opening date must be before expiry date */
    dateSequenceValidation: boolean;
    /** State and ZIP code consistency validation */
    stateZipConsistency: boolean;
    /** Customer ID must exist in customer database */
    customerReferenceValidation: boolean;
  };
}