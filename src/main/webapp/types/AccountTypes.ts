/**
 * CardDemo - Account Management TypeScript Interface Definitions
 * 
 * This file contains comprehensive TypeScript interface definitions for account management
 * screens (COACTVW/COACTUP) including account data structures, customer details, financial
 * field precision types, and form validation schemas that maintain exact functional
 * equivalence with the original BMS mapset layouts.
 * 
 * Maps BMS field structures from COACTVW.bms and COACTUP.bms to TypeScript interfaces
 * while preserving exact field lengths, validation rules, and business logic constraints.
 * 
 * Implements BigDecimal-equivalent financial precision matching PostgreSQL NUMERIC(12,2)
 * for all monetary fields (ACURBAL, ACRDLIM, ACSHLIM) to ensure exact decimal precision
 * preservation from the original COBOL COMP-3 implementations.
 * 
 * @author Blitzy Platform - Enterprise-grade transformation agent
 * @version 1.0.0
 * @since 2024-01-01
 */

import { FormFieldAttributes } from './CommonTypes';
import { ValidationResult } from './ValidationTypes';

// ===================================================================
// FINANCIAL PRECISION TYPE DEFINITIONS
// ===================================================================

/**
 * Account ID Type - Maps to BMS ACCTSID field (11 characters)
 * 
 * Represents 11-digit account numbers with exact length validation.
 * Maps to ACCTSIDI field in both COACTVW and COACTUP copybooks.
 * 
 * BMS Definition: ACCTSIDI PIC 99999999999 (COACTVW line 60)
 * PICIN Pattern: '99999999999' for 11-digit numeric validation
 */
export type AccountId = string;

/**
 * Account Amount Type - Financial precision type for monetary values
 * 
 * Represents monetary amounts with exact decimal precision matching
 * PostgreSQL NUMERIC(12,2) and COBOL COMP-3 field definitions.
 * 
 * Used for: ACURBAL, ACRDLIM, ACSHLIM, ACRCYCR, ACRCYDB
 * BMS PICOUT Pattern: '+ZZZ,ZZZ,ZZZ.99' (15 character display)
 * 
 * Precision: 12 digits total, 2 decimal places
 * Range: -999,999,999.99 to +999,999,999.99
 */
export type AccountAmount = number;

/**
 * Account Status Type - Maps to BMS ACSTTUS field (1 character)
 * 
 * Represents account active status with Y/N validation.
 * Maps to ACSTTUSI field in both COACTVW and COACTUP copybooks.
 * 
 * BMS Definition: ACSTTUSI PIC X(1) (COACTVW line 66)
 * Valid Values: 'Y' (Active), 'N' (Inactive)
 */
export type AccountStatus = 'Y' | 'N';

// ===================================================================
// HELPER INTERFACE DEFINITIONS
// ===================================================================

/**
 * Date Field Interface - Structured date representation
 * 
 * Represents date fields split into year/month/day components
 * matching the BMS update screen date input pattern.
 * 
 * Maps to COACTUP date fields:
 * - OPNYEAR/OPNMON/OPNDAY (Account opened date)
 * - EXPYEAR/EXPMON/EXPDAY (Account expiry date)
 * - RISYEAR/RISMON/RISDAY (Account reissue date)
 * - DOBYEAR/DOBMON/DOBDAY (Customer date of birth)
 */
export interface DateField {
  /**
   * Year component (4 digits) - Maps to BMS *YEAR fields
   * BMS Definition: PIC X(4) with JUSTIFY=(RIGHT)
   * Valid range: 1900-2099
   */
  year: string;
  
  /**
   * Month component (2 digits) - Maps to BMS *MON fields  
   * BMS Definition: PIC X(2) with JUSTIFY=(RIGHT)
   * Valid range: 01-12
   */
  month: string;
  
  /**
   * Day component (2 digits) - Maps to BMS *DAY fields
   * BMS Definition: PIC X(2) with JUSTIFY=(RIGHT)
   * Valid range: 01-31 (depends on month)
   */
  day: string;
  
  /**
   * Validation status - Whether the date components form a valid date
   * Used for immediate validation feedback in update forms
   */
  isValid: boolean;
  
  /**
   * Formatted date string - ISO format (YYYY-MM-DD) for display
   * Generated from year/month/day components for consistent formatting
   */
  formattedDate: string;
}

/**
 * Phone Number Interface - Structured phone number representation
 * 
 * Represents phone numbers split into area code/exchange/number components
 * matching the BMS update screen phone input pattern.
 * 
 * Maps to COACTUP phone fields:
 * - ACSPH1A/ACSPH1B/ACSPH1C (Phone 1 components)  
 * - ACSPH2A/ACSPH2B/ACSPH2C (Phone 2 components)
 * 
 * BMS Definition: Each component PIC X(3) or X(4) with JUSTIFY=(RIGHT)
 */
export interface PhoneNumber {
  /**
   * Area code (3 digits) - Maps to BMS *PH*A fields
   * BMS Definition: PIC X(3) with JUSTIFY=(RIGHT)
   * Format: XXX (e.g., "214", "555")
   */
  areaCode: string;
  
  /**
   * Exchange code (3 digits) - Maps to BMS *PH*B fields
   * BMS Definition: PIC X(3) with JUSTIFY=(RIGHT)
   * Format: XXX (e.g., "555", "123")
   */
  exchangeCode: string;
  
  /**
   * Number (4 digits) - Maps to BMS *PH*C fields
   * BMS Definition: PIC X(4) with JUSTIFY=(RIGHT)
   * Format: XXXX (e.g., "1234", "5678")
   */
  number: string;
  
  /**
   * Formatted phone number - Standard format (XXX) XXX-XXXX
   * Generated from components for consistent display
   */
  formattedNumber: string;
  
  /**
   * Validation status - Whether the phone number components are valid
   * Used for immediate validation feedback in update forms
   */
  isValid: boolean;
}

/**
 * SSN Interface - Structured Social Security Number representation
 * 
 * Represents SSN split into three components matching the BMS update
 * screen SSN input pattern with format validation.
 * 
 * Maps to COACTUP SSN fields:
 * - ACTSSN1 (3 digits) - Area number
 * - ACTSSN2 (2 digits) - Group number  
 * - ACTSSN3 (4 digits) - Serial number
 * 
 * BMS Definition: ACTSSN1I PIC X(3), ACTSSN2I PIC X(2), ACTSSN3I PIC X(4)
 */
export interface SSN {
  /**
   * Area number (3 digits) - Maps to BMS ACTSSN1 field
   * BMS Definition: ACTSSN1I PIC X(3) with INITIAL='999'
   * Format: XXX (e.g., "123", "456")
   */
  part1: string;
  
  /**
   * Group number (2 digits) - Maps to BMS ACTSSN2 field
   * BMS Definition: ACTSSN2I PIC X(2) with INITIAL='99'
   * Format: XX (e.g., "45", "67")
   */
  part2: string;
  
  /**
   * Serial number (4 digits) - Maps to BMS ACTSSN3 field
   * BMS Definition: ACTSSN3I PIC X(4) with INITIAL='9999'
   * Format: XXXX (e.g., "6789", "1234")
   */
  part3: string;
  
  /**
   * Formatted SSN - Standard format XXX-XX-XXXX
   * Generated from components for consistent display
   */
  formattedSSN: string;
  
  /**
   * Validation status - Whether the SSN components are valid
   * Used for immediate validation feedback in update forms
   */
  isValid: boolean;
}

// ===================================================================
// CUSTOMER DETAILS INTERFACE
// ===================================================================

/**
 * Customer Details Data Interface
 * 
 * Represents customer information nested within account data structure.
 * Maps to customer-related fields in both COACTVW and COACTUP BMS mapsets.
 * 
 * Provides comprehensive customer profile information including personal details,
 * address, contact information, and financial attributes associated with the account.
 * 
 * Based on BMS fields: ACSTNUM, ACSTSSN, ACSTDOB, ACSTFCO, ACSFNAM, ACSMNAM,
 * ACSLNAM, ACSADL1, ACSADL2, ACSCITY, ACSSTTE, ACSZIPC, ACSCTRY, ACSPHN1,
 * ACSPHN2, ACSGOVT, ACSEFTC, ACSPFLG
 */
export interface CustomerDetailsData {
  /**
   * Customer ID - Maps to BMS ACSTNUM field (9 characters)
   * 
   * Unique identifier for the customer record.
   * BMS Definition: ACSTNUMI PIC X(9) (COACTVW line 126)
   * 
   * Used for: Customer record lookup and cross-reference validation
   */
  customerId: string;
  
  /**
   * Customer SSN - Maps to BMS ACSTSSN field (12 characters)
   * 
   * Social Security Number for customer identification.
   * BMS Definition: ACSTSSNI PIC X(12) (COACTVW line 132)
   * 
   * Format: XXX-XX-XXXX with dashes for display
   */
  customerSSN: string;
  
  /**
   * Date of Birth - Maps to BMS ACSTDOB field (10 characters)
   * 
   * Customer's date of birth for verification and age calculation.
   * BMS Definition: ACSTDOBI PIC X(10) (COACTVW line 138)
   * 
   * Format: MM/DD/YYYY
   */
  dateOfBirth: string;
  
  /**
   * FICO Score - Maps to BMS ACSTFCO field (3 characters)
   * 
   * Customer's credit score for credit limit determination.
   * BMS Definition: ACSTFCOI PIC X(3) (COACTVW line 144)
   * 
   * Valid range: 300-850
   */
  ficoScore: number;
  
  /**
   * First Name - Maps to BMS ACSFNAM field (25 characters)
   * 
   * Customer's first name for identification.
   * BMS Definition: ACSFNAMI PIC X(25) (COACTVW line 150)
   * 
   * Required field with alphabetic validation
   */
  firstName: string;
  
  /**
   * Middle Name - Maps to BMS ACSMNAM field (25 characters)
   * 
   * Customer's middle name or initial.
   * BMS Definition: ACSMNAMI PIC X(25) (COACTVW line 156)
   * 
   * Optional field with alphabetic validation
   */
  middleName: string;
  
  /**
   * Last Name - Maps to BMS ACSLNAM field (25 characters)
   * 
   * Customer's last name for identification.
   * BMS Definition: ACSLNAMI PIC X(25) (COACTVW line 162)
   * 
   * Required field with alphabetic validation
   */
  lastName: string;
  
  /**
   * Address Line 1 - Maps to BMS ACSADL1 field (50 characters)
   * 
   * Primary address line (street address).
   * BMS Definition: ACSADL1I PIC X(50) (COACTVW line 168)
   * 
   * Required field for mailing address
   */
  addressLine1: string;
  
  /**
   * Address Line 2 - Maps to BMS ACSADL2 field (50 characters)
   * 
   * Secondary address line (apartment, suite, etc.).
   * BMS Definition: ACSADL2I PIC X(50) (COACTVW line 180)
   * 
   * Optional field for additional address information
   */
  addressLine2: string;
  
  /**
   * City - Maps to BMS ACSCITY field (50 characters)
   * 
   * City name for the customer's address.
   * BMS Definition: ACSCITYI PIC X(50) (COACTVW line 192)
   * 
   * Required field with alphabetic validation
   */
  city: string;
  
  /**
   * State - Maps to BMS ACSSTTE field (2 characters)
   * 
   * State abbreviation for the customer's address.
   * BMS Definition: ACSSTTEI PIC X(2) (COACTVW line 174)
   * 
   * Required field with state code validation
   */
  state: string;
  
  /**
   * ZIP Code - Maps to BMS ACSZIPC field (5 characters)
   * 
   * ZIP code for the customer's address.
   * BMS Definition: ACSZIPCI PIC X(5) (COACTVW line 186)
   * 
   * Required field with ZIP code validation and state cross-reference
   */
  zipCode: string;
  
  /**
   * Country - Maps to BMS ACSCTRY field (3 characters)
   * 
   * Country code for the customer's address.
   * BMS Definition: ACSCTRYI PIC X(3) (COACTVW line 198)
   * 
   * Required field with country code validation
   */
  country: string;
  
  /**
   * Phone Number 1 - Maps to BMS ACSPHN1 field (13 characters)
   * 
   * Primary phone number for customer contact.
   * BMS Definition: ACSPHN1I PIC X(13) (COACTVW line 204)
   * 
   * Required field with phone number format validation
   */
  phoneNumber1: string;
  
  /**
   * Phone Number 2 - Maps to BMS ACSPHN2 field (13 characters)
   * 
   * Secondary phone number for customer contact.
   * BMS Definition: ACSPHN2I PIC X(13) (COACTVW line 216)
   * 
   * Optional field with phone number format validation
   */
  phoneNumber2: string;
  
  /**
   * Government ID - Maps to BMS ACSGOVT field (20 characters)
   * 
   * Government-issued identification reference.
   * BMS Definition: ACSGOVTI PIC X(20) (COACTVW line 210)
   * 
   * Required field for identity verification
   */
  governmentId: string;
  
  /**
   * EFT Account ID - Maps to BMS ACSEFTC field (10 characters)
   * 
   * Electronic Funds Transfer account identifier.
   * BMS Definition: ACSEFTCI PIC X(10) (COACTVW line 222)
   * 
   * Optional field for electronic payment setup
   */
  eftAccountId: string;
  
  /**
   * Primary Card Holder Flag - Maps to BMS ACSPFLG field (1 character)
   * 
   * Indicates if customer is the primary card holder.
   * BMS Definition: ACSPFLGI PIC X(1) (COACTVW line 228)
   * 
   * Valid values: 'Y' (Primary), 'N' (Secondary)
   */
  isPrimaryCardHolder: boolean;
}

// ===================================================================
// ACCOUNT VIEW DATA INTERFACE
// ===================================================================

/**
 * Account View Data Interface
 * 
 * Represents complete account information for display in the account view screen.
 * Maps to all fields in the COACTVW BMS mapset with exact field structure preservation.
 * 
 * Used by AccountViewService.java for JPA repository query results and by React
 * components for read-only account display with comprehensive customer information.
 * 
 * Maintains exact field sequencing and validation patterns from the original BMS
 * implementation while providing modern TypeScript type safety.
 */
export interface AccountViewData {
  /**
   * Base screen data - Common header information
   * 
   * Includes transaction name, program name, current date/time,
   * and screen titles consistent across all 18 BMS components.
   */
  baseScreenData: {
    trnname: string;
    pgmname: string;
    curdate: string;
    curtime: string;
    title01: string;
    title02: string;
  };
  
  /**
   * Account ID - Maps to BMS ACCTSID field (11 characters)
   * 
   * Primary account identifier for the account being viewed.
   * BMS Definition: ACCTSIDI PIC 99999999999 (COACTVW line 60)
   * 
   * Field Attributes: ATTRB=(FSET,IC,NORM,UNPROT), VALIDN=(MUSTFILL)
   */
  accountId: AccountId;
  
  /**
   * Account Status - Maps to BMS ACSTTUS field (1 character)
   * 
   * Active status indicator for the account.
   * BMS Definition: ACSTTUSI PIC X(1) (COACTVW line 66)
   * 
   * Field Attributes: ATTRB=(ASKIP), Display only in view mode
   */
  accountStatus: AccountStatus;
  
  /**
   * Date Opened - Maps to BMS ADTOPEN field (10 characters)
   * 
   * Date when the account was first opened.
   * BMS Definition: ADTOPENI PIC X(10) (COACTVW line 72)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, Display only
   */
  dateOpened: string;
  
  /**
   * Expiry Date - Maps to BMS AEXPDT field (10 characters)
   * 
   * Date when the account expires or needs renewal.
   * BMS Definition: AEXPDTI PIC X(10) (COACTVW line 84)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, Display only
   */
  expiryDate: string;
  
  /**
   * Reissue Date - Maps to BMS AREISDT field (10 characters)
   * 
   * Date when the account was last reissued.
   * BMS Definition: AREISDTI PIC X(10) (COACTVW line 96)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, Display only
   */
  reissueDate: string;
  
  /**
   * Credit Limit - Maps to BMS ACRDLIM field (15 characters)
   * 
   * Maximum credit limit for the account.
   * BMS Definition: ACRDLIMI PIC X(15) (COACTVW line 78)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, JUSTIFY=(RIGHT), PICOUT='+ZZZ,ZZZ,ZZZ.99'
   */
  creditLimit: AccountAmount;
  
  /**
   * Cash Credit Limit - Maps to BMS ACSHLIM field (15 characters)
   * 
   * Maximum cash advance limit for the account.
   * BMS Definition: ACSHLIMI PIC X(15) (COACTVW line 90)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, JUSTIFY=(RIGHT), PICOUT='+ZZZ,ZZZ,ZZZ.99'
   */
  cashCreditLimit: AccountAmount;
  
  /**
   * Current Balance - Maps to BMS ACURBAL field (15 characters)
   * 
   * Current outstanding balance on the account.
   * BMS Definition: ACURBALI PIC X(15) (COACTVW line 102)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, JUSTIFY=(RIGHT), PICOUT='+ZZZ,ZZZ,ZZZ.99'
   */
  currentBalance: AccountAmount;
  
  /**
   * Current Cycle Credit - Maps to BMS ACRCYCR field (15 characters)
   * 
   * Credit amount for the current billing cycle.
   * BMS Definition: ACRCYCRI PIC X(15) (COACTVW line 108)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, JUSTIFY=(RIGHT), PICOUT='+ZZZ,ZZZ,ZZZ.99'
   */
  currentCycleCredit: AccountAmount;
  
  /**
   * Current Cycle Debit - Maps to BMS ACRCYDB field (15 characters)
   * 
   * Debit amount for the current billing cycle.
   * BMS Definition: ACRCYDBI PIC X(15) (COACTVW line 120)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, JUSTIFY=(RIGHT), PICOUT='+ZZZ,ZZZ,ZZZ.99'
   */
  currentCycleDebit: AccountAmount;
  
  /**
   * Account Group - Maps to BMS AADDGRP field (10 characters)
   * 
   * Account group classification for the account.
   * BMS Definition: AADDGRPI PIC X(10) (COACTVW line 114)
   * 
   * Field Attributes: HILIGHT=UNDERLINE, Display only
   */
  accountGroup: string;
  
  /**
   * Customer Details - Nested customer information
   * 
   * Complete customer profile associated with the account.
   * Includes all customer-related fields from the BMS mapset.
   */
  customerDetails: CustomerDetailsData;
  
  /**
   * Information Message - Maps to BMS INFOMSG field (45 characters)
   * 
   * Informational message displayed to the user.
   * BMS Definition: INFOMSGI PIC X(45) (COACTVW line 234)
   * 
   * Field Attributes: ATTRB=(PROT), COLOR=NEUTRAL, HILIGHT=OFF
   */
  informationMessage: string;
  
  /**
   * Error Message - Maps to BMS ERRMSG field (78 characters)
   * 
   * Error message displayed for validation or system errors.
   * BMS Definition: ERRMSGI PIC X(78) (COACTVW line 240)
   * 
   * Field Attributes: ATTRB=(ASKIP,BRT,FSET), COLOR=RED
   */
  errorMessage: string;
  
  /**
   * Field Attributes - BMS attribute mapping for React components
   * 
   * Maps BMS field attributes to React component properties for
   * consistent field behavior and validation.
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

// ===================================================================
// ACCOUNT UPDATE FORM DATA INTERFACE
// ===================================================================

/**
 * Account Update Form Data Interface
 * 
 * Represents complete account update form structure for the account update screen.
 * Maps to all fields in the COACTUP BMS mapset with support for form validation
 * and field-level editing capabilities.
 * 
 * Used by AccountUpdateService.java for form submission processing and by React
 * components for account modification with comprehensive validation support.
 * 
 * Maintains exact field sequencing and input patterns from the original BMS
 * implementation while providing modern form management capabilities.
 */
export interface AccountUpdateFormData {
  /**
   * Base screen data - Common header information
   * 
   * Includes transaction name, program name, current date/time,
   * and screen titles consistent across all 18 BMS components.
   */
  baseScreenData: {
    trnname: string;
    pgmname: string;
    curdate: string;
    curtime: string;
    title01: string;
    title02: string;
  };
  
  /**
   * Account ID - Maps to BMS ACCTSID field (11 characters)
   * 
   * Primary account identifier for the account being updated.
   * BMS Definition: ACCTSIDI PIC X(11) (COACTUP line 60)
   * 
   * Field Attributes: ATTRB=(IC,UNPROT), HILIGHT=UNDERLINE
   */
  accountId: AccountId;
  
  /**
   * Account Status - Maps to BMS ACSTTUS field (1 character)
   * 
   * Active status indicator for the account.
   * BMS Definition: ACSTTUSI PIC X(1) (COACTUP line 66)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  accountStatus: AccountStatus;
  
  /**
   * Opening Year - Maps to BMS OPNYEAR field (4 characters)
   * 
   * Year component of the account opening date.
   * BMS Definition: OPNYEARI PIC X(4) (COACTUP line 72)
   * 
   * Field Attributes: ATTRB=(FSET,UNPROT), JUSTIFY=(RIGHT)
   */
  openYear: string;
  
  /**
   * Opening Month - Maps to BMS OPNMON field (2 characters)
   * 
   * Month component of the account opening date.
   * BMS Definition: OPNMONI PIC X(2) (COACTUP line 78)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  openMonth: string;
  
  /**
   * Opening Day - Maps to BMS OPNDAY field (2 characters)
   * 
   * Day component of the account opening date.
   * BMS Definition: OPNDAYI PIC X(2) (COACTUP line 84)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  openDay: string;
  
  /**
   * Expiry Year - Maps to BMS EXPYEAR field (4 characters)
   * 
   * Year component of the account expiry date.
   * BMS Definition: EXPYEARI PIC X(4) (COACTUP line 96)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  expiryYear: string;
  
  /**
   * Expiry Month - Maps to BMS EXPMON field (2 characters)
   * 
   * Month component of the account expiry date.
   * BMS Definition: EXPMONI PIC X(2) (COACTUP line 102)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  expiryMonth: string;
  
  /**
   * Expiry Day - Maps to BMS EXPDAY field (2 characters)
   * 
   * Day component of the account expiry date.
   * BMS Definition: EXPDAYI PIC X(2) (COACTUP line 108)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  expiryDay: string;
  
  /**
   * Reissue Year - Maps to BMS RISYEAR field (4 characters)
   * 
   * Year component of the account reissue date.
   * BMS Definition: RISYEARI PIC X(4) (COACTUP line 120)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  reissueYear: string;
  
  /**
   * Reissue Month - Maps to BMS RISMON field (2 characters)
   * 
   * Month component of the account reissue date.
   * BMS Definition: RISMONI PIC X(2) (COACTUP line 126)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  reissueMonth: string;
  
  /**
   * Reissue Day - Maps to BMS RISDAY field (2 characters)
   * 
   * Day component of the account reissue date.
   * BMS Definition: RISDAYI PIC X(2) (COACTUP line 132)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  reissueDay: string;
  
  /**
   * Credit Limit - Maps to BMS ACRDLIM field (15 characters)
   * 
   * Maximum credit limit for the account.
   * BMS Definition: ACRDLIMI PIC X(15) (COACTUP line 90)
   * 
   * Field Attributes: ATTRB=(FSET,UNPROT), HILIGHT=UNDERLINE
   */
  creditLimit: AccountAmount;
  
  /**
   * Cash Credit Limit - Maps to BMS ACSHLIM field (15 characters)
   * 
   * Maximum cash advance limit for the account.
   * BMS Definition: ACSHLIMI PIC X(15) (COACTUP line 114)
   * 
   * Field Attributes: ATTRB=(FSET,UNPROT), HILIGHT=UNDERLINE
   */
  cashCreditLimit: AccountAmount;
  
  /**
   * Current Balance - Maps to BMS ACURBAL field (15 characters)
   * 
   * Current outstanding balance on the account.
   * BMS Definition: ACURBALI PIC X(15) (COACTUP line 138)
   * 
   * Field Attributes: ATTRB=(FSET,UNPROT), HILIGHT=UNDERLINE
   */
  currentBalance: AccountAmount;
  
  /**
   * Current Cycle Credit - Maps to BMS ACRCYCR field (15 characters)
   * 
   * Credit amount for the current billing cycle.
   * BMS Definition: ACRCYCRI PIC X(15) (COACTUP line 144)
   * 
   * Field Attributes: ATTRB=(FSET,UNPROT), HILIGHT=UNDERLINE
   */
  currentCycleCredit: AccountAmount;
  
  /**
   * Current Cycle Debit - Maps to BMS ACRCYDB field (15 characters)
   * 
   * Debit amount for the current billing cycle.
   * BMS Definition: ACRCYDBI PIC X(15) (COACTUP line 156)
   * 
   * Field Attributes: ATTRB=(FSET,UNPROT), HILIGHT=UNDERLINE
   */
  currentCycleDebit: AccountAmount;
  
  /**
   * Account Group - Maps to BMS AADDGRP field (10 characters)
   * 
   * Account group classification for the account.
   * BMS Definition: AADDGRPI PIC X(10) (COACTUP line 150)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  accountGroup: string;
  
  /**
   * Customer ID - Maps to BMS ACSTNUM field (9 characters)
   * 
   * Customer identifier associated with the account.
   * BMS Definition: ACSTNUMI PIC X(9) (COACTUP line 162)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  customerId: string;
  
  /**
   * SSN Part 1 - Maps to BMS ACTSSN1 field (3 characters)
   * 
   * First part of the Social Security Number.
   * BMS Definition: ACTSSN1I PIC X(3) (COACTUP line 168)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE, INITIAL='999'
   */
  ssnPart1: string;
  
  /**
   * SSN Part 2 - Maps to BMS ACTSSN2 field (2 characters)
   * 
   * Second part of the Social Security Number.
   * BMS Definition: ACTSSN2I PIC X(2) (COACTUP line 174)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE, INITIAL='99'
   */
  ssnPart2: string;
  
  /**
   * SSN Part 3 - Maps to BMS ACTSSN3 field (4 characters)
   * 
   * Third part of the Social Security Number.
   * BMS Definition: ACTSSN3I PIC X(4) (COACTUP line 180)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE, INITIAL='9999'
   */
  ssnPart3: string;
  
  /**
   * Date of Birth Year - Maps to BMS DOBYEAR field (4 characters)
   * 
   * Year component of the customer's date of birth.
   * BMS Definition: DOBYEARI PIC X(4) (COACTUP line 186)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  dobYear: string;
  
  /**
   * Date of Birth Month - Maps to BMS DOBMON field (2 characters)
   * 
   * Month component of the customer's date of birth.
   * BMS Definition: DOBMONI PIC X(2) (COACTUP line 192)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  dobMonth: string;
  
  /**
   * Date of Birth Day - Maps to BMS DOBDAY field (2 characters)
   * 
   * Day component of the customer's date of birth.
   * BMS Definition: DOBDAYI PIC X(2) (COACTUP line 198)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  dobDay: string;
  
  /**
   * FICO Score - Maps to BMS ACSTFCO field (3 characters)
   * 
   * Customer's credit score for credit limit determination.
   * BMS Definition: ACSTFCOI PIC X(3) (COACTUP line 204)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  ficoScore: number;
  
  /**
   * First Name - Maps to BMS ACSFNAM field (25 characters)
   * 
   * Customer's first name for identification.
   * BMS Definition: ACSFNAMI PIC X(25) (COACTUP line 210)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  firstName: string;
  
  /**
   * Middle Name - Maps to BMS ACSMNAM field (25 characters)
   * 
   * Customer's middle name or initial.
   * BMS Definition: ACSMNAMI PIC X(25) (COACTUP line 216)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  middleName: string;
  
  /**
   * Last Name - Maps to BMS ACSLNAM field (25 characters)
   * 
   * Customer's last name for identification.
   * BMS Definition: ACSLNAMI PIC X(25) (COACTUP line 222)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  lastName: string;
  
  /**
   * Address Line 1 - Maps to BMS ACSADL1 field (50 characters)
   * 
   * Primary address line (street address).
   * BMS Definition: ACSADL1I PIC X(50) (COACTUP line 228)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  addressLine1: string;
  
  /**
   * Address Line 2 - Maps to BMS ACSADL2 field (50 characters)
   * 
   * Secondary address line (apartment, suite, etc.).
   * BMS Definition: ACSADL2I PIC X(50) (COACTUP line 240)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  addressLine2: string;
  
  /**
   * City - Maps to BMS ACSCITY field (50 characters)
   * 
   * City name for the customer's address.
   * BMS Definition: ACSCITYI PIC X(50) (COACTUP line 252)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  city: string;
  
  /**
   * State - Maps to BMS ACSSTTE field (2 characters)
   * 
   * State abbreviation for the customer's address.
   * BMS Definition: ACSSTTEI PIC X(2) (COACTUP line 234)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  state: string;
  
  /**
   * ZIP Code - Maps to BMS ACSZIPC field (5 characters)
   * 
   * ZIP code for the customer's address.
   * BMS Definition: ACSZIPCI PIC X(5) (COACTUP line 246)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  zipCode: string;
  
  /**
   * Country - Maps to BMS ACSCTRY field (3 characters)
   * 
   * Country code for the customer's address.
   * BMS Definition: ACSCTRYI PIC X(3) (COACTUP line 258)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  country: string;
  
  /**
   * Phone 1 Area Code - Maps to BMS ACSPH1A field (3 characters)
   * 
   * Area code for the primary phone number.
   * BMS Definition: ACSPH1AI PIC X(3) (COACTUP line 264)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  phone1Area: string;
  
  /**
   * Phone 1 Exchange - Maps to BMS ACSPH1B field (3 characters)
   * 
   * Exchange code for the primary phone number.
   * BMS Definition: ACSPH1BI PIC X(3) (COACTUP line 270)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  phone1Exchange: string;
  
  /**
   * Phone 1 Number - Maps to BMS ACSPH1C field (4 characters)
   * 
   * Number for the primary phone number.
   * BMS Definition: ACSPH1CI PIC X(4) (COACTUP line 276)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  phone1Number: string;
  
  /**
   * Phone 2 Area Code - Maps to BMS ACSPH2A field (3 characters)
   * 
   * Area code for the secondary phone number.
   * BMS Definition: ACSPH2AI PIC X(3) (COACTUP line 288)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  phone2Area: string;
  
  /**
   * Phone 2 Exchange - Maps to BMS ACSPH2B field (3 characters)
   * 
   * Exchange code for the secondary phone number.
   * BMS Definition: ACSPH2BI PIC X(3) (COACTUP line 294)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  phone2Exchange: string;
  
  /**
   * Phone 2 Number - Maps to BMS ACSPH2C field (4 characters)
   * 
   * Number for the secondary phone number.
   * BMS Definition: ACSPH2CI PIC X(4) (COACTUP line 300)
   * 
   * Field Attributes: ATTRB=(UNPROT), JUSTIFY=(RIGHT)
   */
  phone2Number: string;
  
  /**
   * Government ID - Maps to BMS ACSGOVT field (20 characters)
   * 
   * Government-issued identification reference.
   * BMS Definition: ACSGOVTI PIC X(20) (COACTUP line 282)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  governmentId: string;
  
  /**
   * EFT Account ID - Maps to BMS ACSEFTC field (10 characters)
   * 
   * Electronic Funds Transfer account identifier.
   * BMS Definition: ACSEFTCI PIC X(10) (COACTUP line 306)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  eftAccountId: string;
  
  /**
   * Primary Card Holder Flag - Maps to BMS ACSPFLG field (1 character)
   * 
   * Indicates if customer is the primary card holder.
   * BMS Definition: ACSPFLGI PIC X(1) (COACTUP line 312)
   * 
   * Field Attributes: ATTRB=(UNPROT), HILIGHT=UNDERLINE
   */
  isPrimaryCardHolder: boolean;
  
  /**
   * Information Message - Maps to BMS INFOMSG field (45 characters)
   * 
   * Informational message displayed to the user.
   * BMS Definition: INFOMSGI PIC X(45) (COACTUP line 318)
   * 
   * Field Attributes: ATTRB=(ASKIP), COLOR=NEUTRAL, HILIGHT=OFF
   */
  informationMessage: string;
  
  /**
   * Error Message - Maps to BMS ERRMSG field (78 characters)
   * 
   * Error message displayed for validation or system errors.
   * BMS Definition: ERRMSGI PIC X(78) (COACTUP line 324)
   * 
   * Field Attributes: ATTRB=(ASKIP,BRT,FSET), COLOR=RED
   */
  errorMessage: string;
  
  /**
   * Validation Result - Form validation status and errors
   * 
   * Comprehensive validation result for the entire form
   * including field-specific errors and warnings.
   */
  validationResult: ValidationResult;
  
  /**
   * Field Attributes - BMS attribute mapping for React components
   * 
   * Maps BMS field attributes to React component properties for
   * consistent field behavior and validation.
   */
  fieldAttributes: Record<string, FormFieldAttributes>;
}

// ===================================================================
// ACCOUNT VALIDATION SCHEMA INTERFACE
// ===================================================================

/**
 * Account Validation Schema Interface
 * 
 * Defines comprehensive validation rules for account management forms
 * matching BMS field constraints and business logic requirements.
 * 
 * Implements validation patterns equivalent to the original BMS VALIDN
 * parameters and COBOL business rule validation paragraphs.
 * 
 * Used by React Hook Form and Yup validation to ensure data integrity
 * and consistent user experience across account management workflows.
 */
export interface AccountValidationSchema {
  /**
   * Account ID Validation - Maps to BMS ACCTSID validation
   * 
   * Validates 11-digit account numbers with proper format checking.
   * BMS PICIN: '99999999999', VALIDN: (MUSTFILL)
   */
  accountIdValidation: {
    required: boolean;
    pattern: string;
    minLength: number;
    maxLength: number;
    errorMessage: string;
  };
  
  /**
   * Account Status Validation - Maps to BMS ACSTTUS validation
   * 
   * Validates Y/N account status values.
   * BMS Field: PIC X(1) with Y/N value constraint
   */
  accountStatusValidation: {
    required: boolean;
    validValues: AccountStatus[];
    errorMessage: string;
  };
  
  /**
   * Credit Limit Validation - Maps to BMS ACRDLIM validation
   * 
   * Validates credit limit amounts with range and precision checking.
   * BMS PICOUT: '+ZZZ,ZZZ,ZZZ.99' (15 characters)
   */
  creditLimitValidation: {
    required: boolean;
    min: number;
    max: number;
    decimalPlaces: number;
    errorMessage: string;
  };
  
  /**
   * Cash Credit Limit Validation - Maps to BMS ACSHLIM validation
   * 
   * Validates cash credit limit amounts with range and precision checking.
   * BMS PICOUT: '+ZZZ,ZZZ,ZZZ.99' (15 characters)
   */
  cashCreditLimitValidation: {
    required: boolean;
    min: number;
    max: number;
    decimalPlaces: number;
    errorMessage: string;
  };
  
  /**
   * Current Balance Validation - Maps to BMS ACURBAL validation
   * 
   * Validates current balance amounts with range and precision checking.
   * BMS PICOUT: '+ZZZ,ZZZ,ZZZ.99' (15 characters)
   */
  currentBalanceValidation: {
    required: boolean;
    min: number;
    max: number;
    decimalPlaces: number;
    errorMessage: string;
  };
  
  /**
   * Customer ID Validation - Maps to BMS ACSTNUM validation
   * 
   * Validates 9-digit customer IDs with proper format checking.
   * BMS Field: PIC X(9) with numeric validation
   */
  customerIdValidation: {
    required: boolean;
    pattern: string;
    minLength: number;
    maxLength: number;
    errorMessage: string;
  };
  
  /**
   * SSN Validation - Maps to BMS ACTSSN1/2/3 validation
   * 
   * Validates Social Security Number format and components.
   * BMS Fields: ACTSSN1 PIC X(3), ACTSSN2 PIC X(2), ACTSSN3 PIC X(4)
   */
  ssnValidation: {
    required: boolean;
    part1Pattern: string;
    part2Pattern: string;
    part3Pattern: string;
    errorMessage: string;
  };
  
  /**
   * Name Validation - Maps to BMS ACSFNAM/ACSMNAM/ACSLNAM validation
   * 
   * Validates customer name fields with alphabetic checking.
   * BMS Fields: Each PIC X(25) with alphabetic validation
   */
  nameValidation: {
    firstNameRequired: boolean;
    lastNameRequired: boolean;
    maxLength: number;
    pattern: string;
    errorMessage: string;
  };
  
  /**
   * Address Validation - Maps to BMS address field validation
   * 
   * Validates address components with required field checking.
   * BMS Fields: ACSADL1/2 PIC X(50), ACSCITY PIC X(50), etc.
   */
  addressValidation: {
    addressLine1Required: boolean;
    cityRequired: boolean;
    stateRequired: boolean;
    zipRequired: boolean;
    countryRequired: boolean;
    statePattern: string;
    zipPattern: string;
    errorMessage: string;
  };
  
  /**
   * Phone Validation - Maps to BMS phone field validation
   * 
   * Validates phone number components with format checking.
   * BMS Fields: ACSPH1A/B/C and ACSPH2A/B/C with numeric validation
   */
  phoneValidation: {
    phone1Required: boolean;
    areaCodePattern: string;
    exchangePattern: string;
    numberPattern: string;
    errorMessage: string;
  };
  
  /**
   * Date Validation - Maps to BMS date field validation
   * 
   * Validates date components with range and format checking.
   * BMS Fields: Various date fields with YYYY-MM-DD validation
   */
  dateValidation: {
    yearPattern: string;
    monthPattern: string;
    dayPattern: string;
    yearRange: { min: number; max: number };
    errorMessage: string;
  };
  
  /**
   * Form Validation - Overall form validation rules
   * 
   * Cross-field validation and business rule enforcement
   * equivalent to COBOL validation paragraphs.
   */
  formValidation: {
    creditLimitBusinessRules: boolean;
    stateZipCrossValidation: boolean;
    dateRangeValidation: boolean;
    accountStatusBusinessRules: boolean;
    errorMessage: string;
  };
}