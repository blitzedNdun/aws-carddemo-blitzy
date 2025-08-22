/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Application-wide constants for the CardDemo system.
 * Contains field length and validation constants matching COBOL PIC clause specifications.
 * 
 * These constants ensure consistent field validations across DTOs and maintain
 * compatibility with the original COBOL implementation during the modernization.
 */
public final class Constants {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Constants() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    // Field Length Constants matching COBOL PIC clauses
    
    /**
     * Maximum length for user ID fields.
     * Maps to COBOL PIC X(8) specification.
     */
    public static final int USER_ID_LENGTH = 8;

    /**
     * Maximum length for user name fields (first name, last name).
     * Maps to COBOL PIC X(20) specification.
     */
    public static final int USER_NAME_LENGTH = 20;

    /**
     * Maximum length for user type field.
     * Maps to COBOL PIC X(1) specification for single character types (A=Admin, U=User).
     */
    public static final int USER_TYPE_LENGTH = 1;

    /**
     * Maximum length for account number fields.
     * Maps to COBOL PIC X(11) specification.
     */
    public static final int ACCOUNT_NUMBER_LENGTH = 11;

    /**
     * Maximum length for account ID fields.
     * Maps to COBOL PIC X(11) specification.
     */
    public static final int ACCOUNT_ID_LENGTH = 11;

    /**
     * Maximum length for card number fields.
     * Maps to COBOL PIC X(16) specification for standard credit card numbers.
     */
    public static final int CARD_NUMBER_LENGTH = 16;

    /**
     * Maximum length for customer ID fields.
     * Maps to COBOL PIC 9(9) specification for 9-digit customer IDs.
     */
    public static final int CUSTOMER_ID_LENGTH = 9;

    /**
     * Maximum length for SSN fields including dashes.
     * Maps to COBOL PIC X(11) specification for XXX-XX-XXXX format.
     */
    public static final int SSN_LENGTH = 11;

    /**
     * Maximum length for phone number fields.
     * Maps to COBOL PIC X(14) specification for (XXX) XXX-XXXX format.
     */
    public static final int PHONE_NUMBER_LENGTH = 14;

    /**
     * Maximum length for ZIP code fields.
     * Maps to COBOL PIC X(5) specification for 5-digit ZIP codes.
     */
    public static final int ZIP_CODE_LENGTH = 5;

    /**
     * Maximum length for group ID fields.
     * Maps to COBOL PIC X(10) specification for account group identifiers.
     */
    public static final int GROUP_ID_LENGTH = 10;

    /**
     * Default country code for accounts.
     * Standard two-character ISO country code for United States.
     */
    public static final String DEFAULT_COUNTRY_CODE = "US";

    /**
     * Default currency code for accounts.
     * Standard three-character ISO currency code for US Dollar.
     */
    public static final String DEFAULT_CURRENCY_CODE = "USD";

    /**
     * Maximum length for transaction ID fields.
     * Maps to COBOL PIC X(16) specification.
     */
    public static final int TRANSACTION_ID_LENGTH = 16;

    /**
     * Maximum length for transaction type code fields.
     * Maps to COBOL PIC X(02) specification from CVTRA03Y copybook.
     */
    public static final int TYPE_CODE_LENGTH = 2;

    /**
     * Maximum length for transaction category code fields.
     * Maps to COBOL PIC X(4) specification from COTRN02 BMS map.
     */
    public static final int CATEGORY_CODE_LENGTH = 4;

    /**
     * Maximum length for transaction source fields.
     * Maps to COBOL PIC X(10) specification from COTRN02 BMS map.
     */
    public static final int SOURCE_LENGTH = 10;

    /**
     * Maximum length for merchant name fields.
     * Maps to COBOL PIC X(30) specification from COTRN02 BMS map.
     */
    public static final int MERCHANT_NAME_LENGTH = 30;

    /**
     * Maximum length for transaction type description fields.
     * Maps to COBOL PIC X(60) specification from COTRN02 BMS map.
     */
    public static final int DESCRIPTION_LENGTH = 60;

    /**
     * Maximum number of users displayed per page in list views.
     * Matches COBOL screen layout for COUSR00 BMS map supporting 10 users per page.
     */
    public static final int USERS_PER_PAGE = 10;

    /**
     * Maximum number of transactions displayed per page in list views.
     * Matches COBOL screen layout for transaction list screens.
     */
    public static final int TRANSACTIONS_PER_PAGE = 10;

    /**
     * Maximum number of accounts displayed per page in list views.
     * Matches COBOL screen layout for account list screens.
     */
    public static final int ACCOUNTS_PER_PAGE = 10;

    // User Type Constants
    
    /**
     * Admin user type constant.
     * Matches COBOL user type field value for administrative users.
     */
    public static final String USER_TYPE_ADMIN = "A";

    /**
     * Regular user type constant.
     * Matches COBOL user type field value for regular users.
     */
    public static final String USER_TYPE_USER = "U";

    // Selection Flag Constants
    
    /**
     * Selection flag value indicating item is selected.
     * Used in list DTOs for marking selected items.
     */
    public static final String SELECTION_FLAG_SELECTED = "Y";

    /**
     * Selection flag value indicating item is not selected.
     * Used in list DTOs for marking unselected items.
     */
    public static final String SELECTION_FLAG_UNSELECTED = "N";

    /**
     * Selection flag value for empty/space selection.
     * Used in list DTOs when selection is not applicable.
     */
    public static final String SELECTION_FLAG_EMPTY = " ";

    // Date Format Constants
    
    /**
     * Standard date format length for CCYYMMDD format validation.
     * Maps to COBOL PIC X(8) specification for date fields.
     * Used by date conversion utilities to ensure consistent CCYYMMDD format length.
     */
    public static final int DATE_FORMAT_LENGTH = 8;

    // Batch Report Constants
    
    /**
     * Default number of lines per page for batch reports.
     * Matches COBOL batch report layout from CBSTM03A.CBL and CBTRN03C.cbl.
     */
    public static final int PAGE_SIZE = 50;
    
    /**
     * Report line width for batch output formatting.
     * Matches COBOL statement line length from CBSTM03A.CBL.
     */
    public static final int REPORT_WIDTH = 132;
    
    /**
     * Field length mappings for batch report formatting.
     * Maps field names to their corresponding lengths from COBOL copybooks.
     */
    public static final Map<String, Integer> FIELD_LENGTHS;
    
    /**
     * Format patterns for various data types in batch reports.
     * Maps format types to their corresponding pattern strings.
     */
    public static final Map<String, String> FORMAT_PATTERNS;
    
    static {
        // Initialize field lengths map
        FIELD_LENGTHS = new HashMap<>();
        FIELD_LENGTHS.put("ACCOUNT_ID", 11);
        FIELD_LENGTHS.put("TRANSACTION_ID", 16);
        FIELD_LENGTHS.put("CARD_NUMBER", 16);
        FIELD_LENGTHS.put("CUSTOMER_NAME", 30);
        FIELD_LENGTHS.put("MERCHANT_NAME", 30);
        FIELD_LENGTHS.put("AMOUNT", 15);
        FIELD_LENGTHS.put("DATE", 10);
        FIELD_LENGTHS.put("TIME", 8);
        FIELD_LENGTHS.put("DESCRIPTION", 60);
        FIELD_LENGTHS.put("TYPE_CODE", 2);
        FIELD_LENGTHS.put("CATEGORY_CODE", 4);
        FIELD_LENGTHS.put("STATUS", 10);
        
        // Initialize format patterns map
        FORMAT_PATTERNS = new HashMap<>();
        FORMAT_PATTERNS.put("CURRENCY", "###,##0.00");
        FORMAT_PATTERNS.put("DATE", "MM/dd/yyyy");
        FORMAT_PATTERNS.put("TIME", "HH:mm:ss");
        FORMAT_PATTERNS.put("TIMESTAMP", "MM/dd/yyyy HH:mm:ss");
        FORMAT_PATTERNS.put("ACCOUNT_NUMBER", "0000-0000-000");
        FORMAT_PATTERNS.put("CARD_NUMBER", "0000-0000-0000-0000");
        FORMAT_PATTERNS.put("PERCENTAGE", "##0.00%");
        FORMAT_PATTERNS.put("INTEGER", "###,##0");
        FORMAT_PATTERNS.put("DECIMAL", "###,##0.000");
    }
}