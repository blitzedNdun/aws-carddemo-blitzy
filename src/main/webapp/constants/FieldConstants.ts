/**
 * CardDemo Field Constants
 * 
 * TypeScript constants file containing all field-related definitions extracted from BMS maps
 * including field lengths, validation rules, positioning coordinates, attribute mappings,
 * and format patterns. Provides centralized field configuration for React components
 * ensuring exact preservation of original BMS field behaviors while enabling modern
 * TypeScript type checking.
 * 
 * Generated from BMS maps: COSGN00, COMEN01, COACTVW, COACTUP, COCRDLI, COCRDSL,
 * COCRDUP, COTRN00, COTRN01, COTRN02, COBIL00, CORPT00, COADM01, COUSR00-03
 * 
 * @author Blitzy agent
 * @version 1.0.0
 */

/**
 * BMS Attributes enum mapping original BMS attribute bytes to TypeScript constants
 * Preserves exact BMS field behavior while enabling TypeScript type checking
 */
export const BMS_ATTRIBUTES = {
  /** Auto-skip protected field - field is read-only and cursor skips over it */
  ASKIP: 'ASKIP' as const,
  
  /** Unprotected field - allows user input and modification */
  UNPROT: 'UNPROT' as const,
  
  /** Protected field - prevents user modification, field is disabled */
  PROT: 'PROT' as const,
  
  /** Numeric field - only numeric input allowed, triggers numeric keyboard on mobile */
  NUM: 'NUM' as const,
  
  /** Initial cursor - field receives focus when screen loads */
  IC: 'IC' as const,
  
  /** Field set - tracks field modifications for change detection */
  FSET: 'FSET' as const,
  
  /** Normal intensity - standard field display */
  NORM: 'NORM' as const,
  
  /** Bright intensity - emphasized field display */
  BRT: 'BRT' as const,
  
  /** Dark intensity - dimmed field display (used for password fields) */
  DRK: 'DRK' as const,
  
  /** Must fill - field is required and must contain data */
  MUSTFILL: 'MUSTFILL' as const
} as const;

/**
 * Field positioning constants extracted from BMS POS attributes
 * Converts 24x80 terminal coordinates to CSS Grid positioning
 */
export const FIELD_POSITIONING = {
  /** Direct position mapping from BMS POS=(row, col) to CSS Grid coordinates */
  POSITION_MAPPING: {
    // Login Screen (COSGN00) field positions
    USERID: { row: 19, col: 43, gridRow: 19, gridCol: 43 },
    PASSWD: { row: 20, col: 43, gridRow: 20, gridCol: 43 },
    TRNNAME: { row: 1, col: 8, gridRow: 1, gridCol: 8 },
    TITLE01: { row: 1, col: 21, gridRow: 1, gridCol: 21 },
    TITLE02: { row: 2, col: 21, gridRow: 2, gridCol: 21 },
    CURDATE: { row: 1, col: 71, gridRow: 1, gridCol: 71 },
    CURTIME: { row: 2, col: 71, gridRow: 2, gridCol: 71 },
    PGMNAME: { row: 2, col: 8, gridRow: 2, gridCol: 8 },
    APPLID: { row: 3, col: 8, gridRow: 3, gridCol: 8 },
    SYSID: { row: 3, col: 71, gridRow: 3, gridCol: 71 },
    ERRMSG: { row: 23, col: 1, gridRow: 23, gridCol: 1 },
    
    // Main Menu (COMEN01) field positions
    OPTION: { row: 20, col: 41, gridRow: 20, gridCol: 41 },
    OPTN001: { row: 6, col: 20, gridRow: 6, gridCol: 20 },
    OPTN002: { row: 7, col: 20, gridRow: 7, gridCol: 20 },
    OPTN003: { row: 8, col: 20, gridRow: 8, gridCol: 20 },
    OPTN004: { row: 9, col: 20, gridRow: 9, gridCol: 20 },
    OPTN005: { row: 10, col: 20, gridRow: 10, gridCol: 20 },
    OPTN006: { row: 11, col: 20, gridRow: 11, gridCol: 20 },
    OPTN007: { row: 12, col: 20, gridRow: 12, gridCol: 20 },
    OPTN008: { row: 13, col: 20, gridRow: 13, gridCol: 20 },
    OPTN009: { row: 14, col: 20, gridRow: 14, gridCol: 20 },
    OPTN010: { row: 15, col: 20, gridRow: 15, gridCol: 20 },
    OPTN011: { row: 16, col: 20, gridRow: 16, gridCol: 20 },
    OPTN012: { row: 17, col: 20, gridRow: 17, gridCol: 20 },
    
    // Account View (COACTVW) field positions
    ACCTSID: { row: 5, col: 38, gridRow: 5, gridCol: 38 },
    ACSTTUS: { row: 5, col: 70, gridRow: 5, gridCol: 70 },
    ADTOPEN: { row: 6, col: 17, gridRow: 6, gridCol: 17 },
    ACRDLIM: { row: 6, col: 61, gridRow: 6, gridCol: 61 },
    AEXPDT: { row: 7, col: 17, gridRow: 7, gridCol: 17 },
    ACSHLIM: { row: 7, col: 61, gridRow: 7, gridCol: 61 },
    AREISDT: { row: 8, col: 17, gridRow: 8, gridCol: 17 },
    ACURBAL: { row: 8, col: 61, gridRow: 8, gridCol: 61 },
    ACRCYCR: { row: 9, col: 61, gridRow: 9, gridCol: 61 },
    AADDGRP: { row: 10, col: 23, gridRow: 10, gridCol: 23 },
    ACRCYDB: { row: 10, col: 61, gridRow: 10, gridCol: 61 },
    ACSTNUM: { row: 12, col: 23, gridRow: 12, gridCol: 23 },
    ACSTSSN: { row: 12, col: 54, gridRow: 12, gridCol: 54 },
    ACSTDOB: { row: 13, col: 23, gridRow: 13, gridCol: 23 },
    ACSTFCO: { row: 13, col: 61, gridRow: 13, gridCol: 61 },
    ACSFNAM: { row: 15, col: 1, gridRow: 15, gridCol: 1 },
    ACSMNAM: { row: 15, col: 28, gridRow: 15, gridCol: 28 },
    ACSLNAM: { row: 15, col: 55, gridRow: 15, gridCol: 55 },
    ACSADL1: { row: 16, col: 10, gridRow: 16, gridCol: 10 },
    ACSADL2: { row: 17, col: 10, gridRow: 17, gridCol: 10 },
    ACSSTTE: { row: 16, col: 73, gridRow: 16, gridCol: 73 },
    ACSZIPC: { row: 17, col: 73, gridRow: 17, gridCol: 73 },
    ACSCITY: { row: 18, col: 10, gridRow: 18, gridCol: 10 },
    ACSCTRY: { row: 18, col: 73, gridRow: 18, gridCol: 73 },
    ACSPHN1: { row: 19, col: 10, gridRow: 19, gridCol: 10 },
    ACSPHN2: { row: 20, col: 10, gridRow: 20, gridCol: 10 },
    ACSGOVT: { row: 19, col: 58, gridRow: 19, gridCol: 58 },
    ACSEFTC: { row: 20, col: 41, gridRow: 20, gridCol: 41 },
    ACSPFLG: { row: 20, col: 78, gridRow: 20, gridCol: 78 },
    INFOMSG: { row: 22, col: 23, gridRow: 22, gridCol: 23 },
    
    // Card List (COCRDLI) field positions
    PAGENO: { row: 4, col: 76, gridRow: 4, gridCol: 76 },
    
    // Transaction List (COTRN00) field positions
    PAGENUM: { row: 4, col: 71, gridRow: 4, gridCol: 71 },
    TRNIDIN: { row: 6, col: 21, gridRow: 6, gridCol: 21 },
    SEL0001: { row: 10, col: 3, gridRow: 10, gridCol: 3 },
    SEL0002: { row: 11, col: 3, gridRow: 11, gridCol: 3 },
    SEL0003: { row: 12, col: 3, gridRow: 12, gridCol: 3 },
    SEL0004: { row: 13, col: 3, gridRow: 13, gridCol: 3 },
    SEL0005: { row: 14, col: 3, gridRow: 14, gridCol: 3 },
    SEL0006: { row: 15, col: 3, gridRow: 15, gridCol: 3 },
    SEL0007: { row: 16, col: 3, gridRow: 16, gridCol: 3 },
    SEL0008: { row: 17, col: 3, gridRow: 17, gridCol: 3 },
    SEL0009: { row: 18, col: 3, gridRow: 18, gridCol: 3 },
    SEL0010: { row: 19, col: 3, gridRow: 19, gridCol: 3 },
    TRNID01: { row: 10, col: 8, gridRow: 10, gridCol: 8 },
    TRNID02: { row: 11, col: 8, gridRow: 11, gridCol: 8 },
    TRNID03: { row: 12, col: 8, gridRow: 12, gridCol: 8 },
    TRNID04: { row: 13, col: 8, gridRow: 13, gridCol: 8 },
    TRNID05: { row: 14, col: 8, gridRow: 14, gridCol: 8 },
    TRNID06: { row: 15, col: 8, gridRow: 15, gridCol: 8 },
    TRNID07: { row: 16, col: 8, gridRow: 16, gridCol: 8 },
    TRNID08: { row: 17, col: 8, gridRow: 17, gridCol: 8 },
    TRNID09: { row: 18, col: 8, gridRow: 18, gridCol: 8 },
    TRNID10: { row: 19, col: 8, gridRow: 19, gridCol: 8 },
    TDATE01: { row: 10, col: 27, gridRow: 10, gridCol: 27 },
    TDATE02: { row: 11, col: 27, gridRow: 11, gridCol: 27 },
    TDATE03: { row: 12, col: 27, gridRow: 12, gridCol: 27 },
    TDATE04: { row: 13, col: 27, gridRow: 13, gridCol: 27 },
    TDATE05: { row: 14, col: 27, gridRow: 14, gridCol: 27 },
    TDATE06: { row: 15, col: 27, gridRow: 15, gridCol: 27 },
    TDATE07: { row: 16, col: 27, gridRow: 16, gridCol: 27 },
    TDATE08: { row: 17, col: 27, gridRow: 17, gridCol: 27 },
    TDATE09: { row: 18, col: 27, gridRow: 18, gridCol: 27 },
    TDATE10: { row: 19, col: 27, gridRow: 19, gridCol: 27 },
    TDESC01: { row: 10, col: 38, gridRow: 10, gridCol: 38 },
    TDESC02: { row: 11, col: 38, gridRow: 11, gridCol: 38 },
    TDESC03: { row: 12, col: 38, gridRow: 12, gridCol: 38 },
    TDESC04: { row: 13, col: 38, gridRow: 13, gridCol: 38 },
    TDESC05: { row: 14, col: 38, gridRow: 14, gridCol: 38 },
    TDESC06: { row: 15, col: 38, gridRow: 15, gridCol: 38 },
    TDESC07: { row: 16, col: 38, gridRow: 16, gridCol: 38 },
    TDESC08: { row: 17, col: 38, gridRow: 17, gridCol: 38 },
    TDESC09: { row: 18, col: 38, gridRow: 18, gridCol: 38 },
    TDESC10: { row: 19, col: 38, gridRow: 19, gridCol: 38 },
    TAMT001: { row: 10, col: 67, gridRow: 10, gridCol: 67 },
    TAMT002: { row: 11, col: 67, gridRow: 11, gridCol: 67 },
    TAMT003: { row: 12, col: 67, gridRow: 12, gridCol: 67 },
    TAMT004: { row: 13, col: 67, gridRow: 13, gridCol: 67 },
    TAMT005: { row: 14, col: 67, gridRow: 14, gridCol: 67 },
    TAMT006: { row: 15, col: 67, gridRow: 15, gridCol: 67 },
    TAMT007: { row: 16, col: 67, gridRow: 16, gridCol: 67 },
    TAMT008: { row: 17, col: 67, gridRow: 17, gridCol: 67 },
    TAMT009: { row: 18, col: 67, gridRow: 18, gridCol: 67 },
    TAMT010: { row: 19, col: 67, gridRow: 19, gridCol: 67 },
    
    // Bill Payment (COBIL00) field positions
    ACTIDIN: { row: 6, col: 21, gridRow: 6, gridCol: 21 },
    CURBAL: { row: 11, col: 32, gridRow: 11, gridCol: 32 },
    CONFIRM: { row: 15, col: 60, gridRow: 15, gridCol: 60 }
  },
  
  /** CSS Grid coordinate system for responsive layout */
  GRID_COORDINATES: {
    /** Terminal dimensions - 24 rows x 80 columns */
    TERMINAL_ROWS: 24,
    TERMINAL_COLS: 80,
    
    /** Header section - rows 1-3 for system information */
    HEADER_ROWS: [1, 2, 3],
    
    /** Content section - rows 4-21 for business data */
    CONTENT_ROWS: [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21],
    
    /** Footer section - rows 22-24 for messages and function keys */
    FOOTER_ROWS: [22, 23, 24],
    
    /** Standard column breakpoints for responsive layout */
    BREAKPOINTS: {
      xs: 12,  // Mobile
      sm: 12,  // Tablet
      md: 12,  // Desktop
      lg: 12,  // Large desktop
      xl: 12   // Extra large desktop
    }
  },
  
  /** Layout utility functions for position calculations */
  LAYOUT_UTILS: {
    /** Convert BMS row/col to CSS Grid coordinates */
    toGridCoordinates: (row: number, col: number) => ({
      gridRow: row,
      gridColumn: col
    }),
    
    /** Convert BMS position to Material-UI Grid props */
    toMuiGridProps: (row: number, col: number, length: number) => ({
      xs: Math.min(Math.ceil((length / 80) * 12), 12),
      md: Math.min(Math.ceil((length / 80) * 12), 12),
      lg: Math.min(Math.ceil((length / 80) * 12), 12)
    }),
    
    /** Calculate field span based on length */
    calculateSpan: (length: number) => Math.max(1, Math.min(12, Math.ceil(length / 6.67)))
  }
} as const;

/**
 * Field length constants extracted from BMS LENGTH attributes
 * Maintains exact character counts for Material-UI TextField components
 */
export const FIELD_LENGTHS = {
  /** Length constraints for individual fields */
  LENGTH_CONSTRAINTS: {
    // Authentication fields
    USERID: 8,
    PASSWD: 8,
    
    // System fields  
    TRNNAME: 4,
    PGMNAME: 8,
    APPLID: 8,
    SYSID: 8,
    TITLE01: 40,
    TITLE02: 40,
    CURDATE: 8,
    CURTIME: 9,
    
    // Account fields
    ACCTSID: 11,
    ACSTTUS: 1,
    ADTOPEN: 10,
    ACRDLIM: 15,
    AEXPDT: 10,
    ACSHLIM: 15,
    AREISDT: 10,
    ACURBAL: 15,
    ACRCYCR: 15,
    AADDGRP: 10,
    ACRCYDB: 15,
    ACSTNUM: 9,
    ACSTSSN: 12,
    ACSTDOB: 10,
    ACSTFCO: 3,
    ACSFNAM: 25,
    ACSMNAM: 25,
    ACSLNAM: 25,
    ACSADL1: 50,
    ACSADL2: 50,
    ACSSTTE: 2,
    ACSZIPC: 5,
    ACSCITY: 50,
    ACSCTRY: 3,
    ACSPHN1: 13,
    ACSPHN2: 13,
    ACSGOVT: 20,
    ACSEFTC: 10,
    ACSPFLG: 1,
    
    // Menu fields
    OPTION: 2,
    OPTN001: 40,
    OPTN002: 40,
    OPTN003: 40,
    OPTN004: 40,
    OPTN005: 40,
    OPTN006: 40,
    OPTN007: 40,
    OPTN008: 40,
    OPTN009: 40,
    OPTN010: 40,
    OPTN011: 40,
    OPTN012: 40,
    
    // Transaction fields
    TRNIDIN: 16,
    PAGENUM: 8,
    PAGENO: 3,
    SEL0001: 1,
    SEL0002: 1,
    SEL0003: 1,
    SEL0004: 1,
    SEL0005: 1,
    SEL0006: 1,
    SEL0007: 1,
    SEL0008: 1,
    SEL0009: 1,
    SEL0010: 1,
    TRNID01: 16,
    TRNID02: 16,
    TRNID03: 16,
    TRNID04: 16,
    TRNID05: 16,
    TRNID06: 16,
    TRNID07: 16,
    TRNID08: 16,
    TRNID09: 16,
    TRNID10: 16,
    TDATE01: 8,
    TDATE02: 8,
    TDATE03: 8,
    TDATE04: 8,
    TDATE05: 8,
    TDATE06: 8,
    TDATE07: 8,
    TDATE08: 8,
    TDATE09: 8,
    TDATE10: 8,
    TDESC01: 26,
    TDESC02: 26,
    TDESC03: 26,
    TDESC04: 26,
    TDESC05: 26,
    TDESC06: 26,
    TDESC07: 26,
    TDESC08: 26,
    TDESC09: 26,
    TDESC10: 26,
    TAMT001: 12,
    TAMT002: 12,
    TAMT003: 12,
    TAMT004: 12,
    TAMT005: 12,
    TAMT006: 12,
    TAMT007: 12,
    TAMT008: 12,
    TAMT009: 12,
    TAMT010: 12,
    
    // Bill payment fields
    ACTIDIN: 11,
    CURBAL: 14,
    CONFIRM: 1,
    
    // System messages
    ERRMSG: 78,
    INFOMSG: 45
  },
  
  /** Maximum field lengths by category */
  MAX_LENGTHS: {
    ID_FIELDS: 16,          // Transaction IDs, user IDs
    ACCOUNT_FIELDS: 11,     // Account numbers
    NAME_FIELDS: 25,        // Names (first, middle, last)
    ADDRESS_FIELDS: 50,     // Address lines
    PHONE_FIELDS: 13,       // Phone numbers
    AMOUNT_FIELDS: 15,      // Currency amounts
    CODE_FIELDS: 4,         // System codes
    DESCRIPTION_FIELDS: 40, // Menu options and descriptions
    MESSAGE_FIELDS: 78,     // Error and info messages
    FLAG_FIELDS: 1,         // Single character flags
    DATE_FIELDS: 10,        // Date fields
    TITLE_FIELDS: 40        // Screen titles
  },
  
  /** Default field sizes for common field types */
  DEFAULT_SIZES: {
    SMALL: 8,     // User ID, passwords, system codes
    MEDIUM: 16,   // Transaction IDs, search fields
    LARGE: 25,    // Name fields
    XLARGE: 50,   // Address fields, descriptions
    AMOUNT: 15,   // Currency amounts
    FLAG: 1,      // Y/N fields
    DATE: 10,     // Date fields
    MESSAGE: 78   // Error/info messages
  }
} as const;

/**
 * Attribute mapping constants converting BMS attributes to Material-UI component properties
 * Ensures exact preservation of original BMS field behaviors in React components
 */
export const ATTRIBUTE_MAPPINGS = {
  /** Maps BMS ASKIP attribute to Material-UI readonly property */
  ASKIP_TO_READONLY: {
    inputProps: {
      readOnly: true,
      tabIndex: -1
    },
    sx: {
      '& .MuiInputBase-input': {
        color: 'text.secondary',
        backgroundColor: 'grey.100'
      }
    }
  },
  
  /** Maps BMS UNPROT attribute to Material-UI editable property */
  UNPROT_TO_EDITABLE: {
    inputProps: {
      readOnly: false,
      tabIndex: 0
    },
    sx: {
      '& .MuiInputBase-input': {
        color: 'text.primary',
        backgroundColor: 'background.paper'
      }
    }
  },
  
  /** Maps BMS PROT attribute to Material-UI disabled property */
  PROT_TO_DISABLED: {
    disabled: true,
    sx: {
      '& .MuiInputBase-input': {
        color: 'text.disabled',
        backgroundColor: 'action.disabled'
      }
    }
  },
  
  /** Maps BMS NUM attribute to Material-UI numeric input properties */
  NUM_TO_NUMERIC: {
    inputProps: {
      inputMode: 'numeric' as const,
      pattern: '[0-9]*',
      type: 'text'
    },
    sx: {
      '& .MuiInputBase-input': {
        textAlign: 'right',
        fontFamily: 'monospace'
      }
    }
  },
  
  /** Maps BMS IC attribute to Material-UI autofocus property */
  IC_TO_AUTOFOCUS: {
    autoFocus: true,
    sx: {
      '& .MuiInputBase-input': {
        outline: '2px solid',
        outlineColor: 'primary.main'
      }
    }
  },
  
  /** Maps BMS MUSTFILL attribute to Material-UI required property */
  MUSTFILL_TO_REQUIRED: {
    required: true,
    sx: {
      '& .MuiInputLabel-root': {
        '&::after': {
          content: '" *"',
          color: 'error.main'
        }
      }
    }
  },
  
  /** Maps BMS BRT attribute to Material-UI bold styling */
  BRT_TO_BOLD: {
    sx: {
      '& .MuiInputBase-input': {
        fontWeight: 'bold'
      }
    }
  },
  
  /** Maps BMS NORM attribute to Material-UI normal styling */
  NORM_TO_NORMAL: {
    sx: {
      '& .MuiInputBase-input': {
        fontWeight: 'normal'
      }
    }
  },
  
  /** Maps BMS DRK attribute to Material-UI dimmed styling (for password fields) */
  DRK_TO_DIMMED: {
    type: 'password',
    sx: {
      '& .MuiInputBase-input': {
        color: 'text.secondary',
        fontFamily: 'monospace'
      }
    }
  },
  
  /** Maps BMS FSET attribute to Material-UI change tracking */
  FSET_TO_CHANGE_TRACKING: {
    onChange: true,
    sx: {
      '& .MuiInputBase-input': {
        '&:focus': {
          backgroundColor: 'action.selected'
        }
      }
    }
  },
  
  /** Color theme mappings from BMS colors to Material-UI palette */
  COLOR_MAPPINGS: {
    GREEN: 'text.primary',
    RED: 'error.main',
    YELLOW: 'warning.main',
    BLUE: 'info.main',
    NEUTRAL: 'text.secondary',
    TURQUOISE: 'info.light'
  },
  
  /** Field validation state mappings */
  VALIDATION_STATES: {
    VALID: {
      sx: {
        '& .MuiInputBase-input': {
          borderColor: 'success.main'
        }
      }
    },
    INVALID: {
      error: true,
      sx: {
        '& .MuiInputBase-input': {
          borderColor: 'error.main'
        }
      }
    },
    PENDING: {
      sx: {
        '& .MuiInputBase-input': {
          borderColor: 'warning.main'
        }
      }
    }
  }
} as const;

/**
 * Format patterns extracted from BMS PICIN/PICOUT definitions
 * Provides input validation and masking for React components
 */
export const FORMAT_PATTERNS = {
  /** Account number format - 11 digits */
  ACCOUNT_NUMBER: {
    pattern: /^\d{11}$/,
    mask: '99999999999',
    placeholder: '___________',
    validation: (value: string) => /^\d{11}$/.test(value)
  },
  
  /** Credit card number format - 16 digits */
  CARD_NUMBER: {
    pattern: /^\d{16}$/,
    mask: '9999999999999999',
    placeholder: '________________',
    validation: (value: string) => /^\d{16}$/.test(value)
  },
  
  /** SSN format - 9 digits with dashes */
  SSN: {
    pattern: /^\d{3}-\d{2}-\d{4}$/,
    mask: '999-99-9999',
    placeholder: '___-__-____',
    validation: (value: string) => /^\d{3}-\d{2}-\d{4}$/.test(value)
  },
  
  /** Phone number format - 10 digits with formatting */
  PHONE: {
    pattern: /^\(\d{3}\) \d{3}-\d{4}$/,
    mask: '(999) 999-9999',
    placeholder: '(___) ___-____',
    validation: (value: string) => /^\(\d{3}\) \d{3}-\d{4}$/.test(value)
  },
  
  /** Date format - MM/DD/YYYY */
  DATE: {
    pattern: /^(0[1-9]|1[0-2])\/(0[1-9]|[12]\d|3[01])\/\d{4}$/,
    mask: '99/99/9999',
    placeholder: '__/__/____',
    validation: (value: string) => /^(0[1-9]|1[0-2])\/(0[1-9]|[12]\d|3[01])\/\d{4}$/.test(value)
  },
  
  /** Time format - HH:MM:SS */
  TIME: {
    pattern: /^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/,
    mask: '99:99:99',
    placeholder: '__:__:__',
    validation: (value: string) => /^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/.test(value)
  },
  
  /** Currency amount format - up to 15 digits with 2 decimal places */
  CURRENCY: {
    pattern: /^\d{1,13}(\.\d{2})?$/,
    mask: '999,999,999.99',
    placeholder: '___,___,___.__ ',
    validation: (value: string) => /^\d{1,13}(\.\d{2})?$/.test(value)
  },
  
  /** ZIP code format - 5 digits */
  ZIP_CODE: {
    pattern: /^\d{5}$/,
    mask: '99999',
    placeholder: '_____',
    validation: (value: string) => /^\d{5}$/.test(value)
  },
  
  /** State code format - 2 letters */
  STATE_CODE: {
    pattern: /^[A-Z]{2}$/,
    mask: 'AA',
    placeholder: '__',
    validation: (value: string) => /^[A-Z]{2}$/.test(value)
  },
  
  /** FICO score format - 3 digits (300-850) */
  FICO_SCORE: {
    pattern: /^[3-8]\d{2}$/,
    mask: '999',
    placeholder: '___',
    validation: (value: string) => {
      const score = parseInt(value);
      return score >= 300 && score <= 850;
    }
  },
  
  /** Y/N flag format - single character */
  YN_FLAG: {
    pattern: /^[YN]$/,
    mask: 'A',
    placeholder: '_',
    validation: (value: string) => /^[YN]$/.test(value)
  },
  
  /** Numeric option format - 1-2 digits */
  NUMERIC_OPTION: {
    pattern: /^\d{1,2}$/,
    mask: '99',
    placeholder: '__',
    validation: (value: string) => /^\d{1,2}$/.test(value)
  },
  
  /** Transaction ID format - 16 alphanumeric characters */
  TRANSACTION_ID: {
    pattern: /^[A-Za-z0-9]{16}$/,
    mask: 'AAAAAAAAAAAAAAAA',
    placeholder: '________________',
    validation: (value: string) => /^[A-Za-z0-9]{16}$/.test(value)
  }
} as const;

/**
 * Type definitions for field constants
 */
export type BMSAttribute = keyof typeof BMS_ATTRIBUTES;
export type FieldName = keyof typeof FIELD_LENGTHS.LENGTH_CONSTRAINTS;
export type FieldPosition = keyof typeof FIELD_POSITIONING.POSITION_MAPPING;
export type AttributeMapping = keyof typeof ATTRIBUTE_MAPPINGS;
export type FormatPattern = keyof typeof FORMAT_PATTERNS;

/**
 * Utility type for field configuration
 */
export interface FieldConfig {
  name: FieldName;
  length: number;
  position: { row: number; col: number; gridRow: number; gridCol: number };
  attributes: BMSAttribute[];
  formatPattern?: FormatPattern;
  validation?: (value: string) => boolean;
}

/**
 * Helper function to get complete field configuration
 */
export const getFieldConfig = (fieldName: FieldName): FieldConfig => {
  const length = FIELD_LENGTHS.LENGTH_CONSTRAINTS[fieldName];
  const position = FIELD_POSITIONING.POSITION_MAPPING[fieldName as FieldPosition];
  
  return {
    name: fieldName,
    length,
    position: position || { row: 0, col: 0, gridRow: 0, gridCol: 0 },
    attributes: [],
    validation: () => true
  };
};