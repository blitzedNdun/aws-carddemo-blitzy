/**
 * BMS Mapping Utility Functions
 * 
 * This file provides comprehensive utility functions for translating BMS (Basic Mapping Support)
 * mapset definitions and attribute bytes to Material-UI component properties. These functions
 * ensure pixel-perfect preservation of original screen layouts and field behaviors while
 * transforming 3270 terminal interfaces to modern React components.
 * 
 * Key Functionality:
 * - Converts BMS attribute bytes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, DRK) to Material-UI properties
 * - Maps BMS colors (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL) to Material-UI theme palette
 * - Translates BMS DFHMDF field definitions to React component configurations
 * - Converts BMS screen positioning (POS coordinates) to CSS Grid Layout properties
 * - Transforms BMS LENGTH and INITIAL values to React component defaults and validation
 * - Provides BMS HILIGHT attributes (UNDERLINE, BLINK, REVERSE) to Material-UI styling
 * 
 * Copyright (c) 2023 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import React from 'react';
import { Grid, Theme, GridProps, GridSize, GridSpacing, ThemeOptions, PaletteOptions, TypographyOptions } from '@mui/material';
import { ComponentProps, CSSProperties, ReactElement } from 'react';
import yup from 'yup';

// Import constants and types from dependency files
import { ThemeConstants } from '../constants/ThemeConstants';
import { FieldConstants } from '../constants/FieldConstants';
import { ValidationConstants } from '../constants/ValidationConstants';
import { 
  CommonTypes, 
  FormFieldAttributes, 
  BaseScreenData
} from '../types/CommonTypes';

// Additional type definitions for this module
export interface BmsFieldDefinition {
  /** Field name identifier from BMS mapset */
  name: string;
  
  /** BMS attribute bytes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, DRK) */
  attrb: string[];
  
  /** BMS color attribute (BLUE, YELLOW, GREEN, RED, TURQUOISE, NEUTRAL) */
  color: string;
  
  /** BMS highlight attribute (OFF, UNDERLINE, BLINK, REVERSE) */
  hilight?: string;
  
  /** Field length from BMS LENGTH attribute */
  length: number;
  
  /** Field position from BMS POS attribute */
  pos: { row: number; col: number };
  
  /** Initial value from BMS INITIAL attribute */
  initial?: string;
  
  /** Input picture format from BMS PICIN attribute */
  picin?: string;
  
  /** Validation requirements from BMS VALIDN attribute */
  validn?: string;
  
  /** Output picture format from BMS PICOUT attribute */
  picout?: string;
  
  /** Text justification from BMS JUSTIFY attribute */
  justify?: 'LEFT' | 'RIGHT' | 'CENTER';
}

export interface MaterialUIProps {
  /** TextField variant (outlined, filled, standard) */
  variant?: 'outlined' | 'filled' | 'standard';
  
  /** TextField color theme */
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
  
  /** Field label text */
  label?: string;
  
  /** Default value for the field */
  defaultValue?: string;
  
  /** Input type (text, number, password, etc.) */
  type?: string;
  
  /** Field disabled state */
  disabled?: boolean;
  
  /** Field required state */
  required?: boolean;
  
  /** Auto focus on field */
  autoFocus?: boolean;
  
  /** Input properties */
  InputProps?: {
    readOnly?: boolean;
    inputMode?: string;
    pattern?: string;
  };
  
  /** Maximum input length */
  maxLength?: number;
  
  /** CSS styling properties */
  sx?: CSSProperties;
  
  /** CSS class name */
  className?: string;
  
  /** Validation schema */
  validationSchema?: yup.Schema<any>;
  
  /** Grid positioning properties */
  gridProps?: {
    gridRow?: number;
    gridColumn?: number;
    gridRowSpan?: number;
    gridColumnSpan?: number;
  };
}

/**
 * Converts BMS attribute bytes to Material-UI TextField properties
 * 
 * This function translates BMS ATTRB definitions (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, DRK)
 * into equivalent Material-UI component properties while preserving the exact field behavior
 * and user interaction patterns from the original 3270 terminal interface.
 * 
 * @param {string[]} attributes - Array of BMS attribute bytes from DFHMDF ATTRB parameter
 * @returns {object} Material-UI TextField properties object
 * 
 * @example
 * // Convert BMS login field: ATTRB=(FSET,IC,NORM,UNPROT)
 * const userIdProps = convertBmsAttributesToMuiProps(['FSET', 'IC', 'NORM', 'UNPROT']);
 * // Returns: { variant: 'outlined', autoFocus: true, disabled: false, InputProps: { readOnly: false } }
 */
export function convertBmsAttributesToMuiProps(attributes) {
  const muiProps = {
    variant: 'outlined',
    disabled: false,
    required: false,
    autoFocus: false,
    type: 'text',
    InputProps: {
      readOnly: false
    },
    sx: {}
  };

  // Process each BMS attribute
  attributes.forEach(attr => {
    switch (attr) {
      case FieldConstants.BMS_ATTRIBUTES.ASKIP:
        // ASKIP: Auto-skip field - display only, cursor skips over
        muiProps.InputProps.readOnly = true;
        muiProps.variant = 'filled';
        muiProps.disabled = false; // Not disabled for accessibility
        muiProps.sx = { ...muiProps.sx, pointerEvents: 'none' };
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.UNPROT:
        // UNPROT: Unprotected field - allows user input
        muiProps.InputProps.readOnly = false;
        muiProps.variant = 'outlined';
        muiProps.disabled = false;
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.PROT:
        // PROT: Protected field - prevents user modification
        muiProps.disabled = true;
        muiProps.variant = 'filled';
        muiProps.InputProps.readOnly = true;
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.NUM:
        // NUM: Numeric only input validation
        muiProps.type = 'number';
        muiProps.InputProps.inputMode = 'numeric';
        muiProps.InputProps.pattern = '[0-9]*';
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.IC:
        // IC: Initial cursor - field receives focus on load
        muiProps.autoFocus = true;
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.BRT:
        // BRT: Bright intensity - bold font weight
        muiProps.sx = { ...muiProps.sx, fontWeight: 'bold' };
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.NORM:
        // NORM: Normal intensity - standard appearance
        muiProps.sx = { ...muiProps.sx, fontWeight: 'normal' };
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.DRK:
        // DRK: Dark intensity - used for password fields
        muiProps.type = 'password';
        muiProps.sx = { ...muiProps.sx, opacity: 0.7 };
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.MUSTFILL:
        // MUSTFILL: Required field validation
        muiProps.required = true;
        break;
        
      case FieldConstants.BMS_ATTRIBUTES.FSET:
        // FSET: Field set flag for change detection (no UI impact)
        break;
        
      default:
        console.warn(`Unknown BMS attribute: ${attr}`);
    }
  });

  return muiProps;
}

/**
 * Maps BMS color attributes to Material-UI theme palette colors
 * 
 * This function converts BMS COLOR definitions (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL)
 * to Material-UI theme palette colors while maintaining WCAG 2.1 AA accessibility compliance
 * and preserving the original visual hierarchy and semantic meaning.
 * 
 * @param {string} bmsColor - BMS color attribute from DFHMDF COLOR parameter
 * @returns {object} Material-UI theme color configuration
 * 
 * @example
 * // Convert BMS error message color: COLOR=RED
 * const errorColor = mapBmsColorsToTheme('RED');
 * // Returns: { color: 'error', palette: { main: '#d32f2f', contrastText: '#ffffff' } }
 */
export function mapBmsColorsToTheme(bmsColor) {
  const colorMapping = {
    GREEN: {
      color: 'success',
      palette: ThemeConstants.BMS_COLORS.GREEN,
      semanticMeaning: 'input fields and user-editable data'
    },
    RED: {
      color: 'error', 
      palette: ThemeConstants.BMS_COLORS.RED,
      semanticMeaning: 'error messages and validation failures'
    },
    YELLOW: {
      color: 'warning',
      palette: ThemeConstants.BMS_COLORS.YELLOW,
      semanticMeaning: 'page titles and navigation elements'
    },
    BLUE: {
      color: 'primary',
      palette: ThemeConstants.BMS_COLORS.BLUE,
      semanticMeaning: 'system information and data display'
    },
    TURQUOISE: {
      color: 'info',
      palette: ThemeConstants.BMS_COLORS.TURQUOISE,
      semanticMeaning: 'prompts and field labels'
    },
    NEUTRAL: {
      color: 'inherit',
      palette: ThemeConstants.BMS_COLORS.NEUTRAL,
      semanticMeaning: 'section headers and informational content'
    }
  };

  const mapping = colorMapping[bmsColor];
  if (!mapping) {
    console.warn(`Unknown BMS color: ${bmsColor}, defaulting to NEUTRAL`);
    return colorMapping.NEUTRAL;
  }

  return mapping;
}

/**
 * Converts BMS POS coordinates to CSS Grid Layout properties
 * 
 * This function translates BMS POS(row,col) positioning to CSS Grid properties
 * while maintaining exact field relationships and supporting responsive design.
 * The original 3270 terminal uses 24 rows × 80 columns grid system.
 * 
 * @param {number} row - BMS row position (1-24)
 * @param {number} col - BMS column position (1-80)
 * @param {number} [length] - Field length for calculating span
 * @returns {object} CSS Grid properties for Material-UI Grid component
 * 
 * @example
 * // Convert BMS user ID field position: POS=(19,43) LENGTH=8
 * const gridProps = convertPosToGridProperties(19, 43, 8);
 * // Returns: { gridRow: 19, gridColumn: 43, gridColumnSpan: 8 }
 */
export function convertPosToGridProperties(row, col, length = 1) {
  // Validate position is within terminal boundaries
  if (!FieldConstants.FIELD_POSITIONING.LAYOUT_UTILS.validatePosition(row, col)) {
    console.warn(`Invalid BMS position: row=${row}, col=${col}. Using defaults.`);
    row = Math.max(1, Math.min(24, row));
    col = Math.max(1, Math.min(80, col));
  }

  // Calculate field span based on length
  const fieldSpan = FieldConstants.FIELD_POSITIONING.LAYOUT_UTILS.calculateFieldSpan(length);
  
  // Ensure span doesn't exceed terminal width
  const maxSpan = 80 - col + 1;
  const actualSpan = Math.min(fieldSpan, maxSpan);

  return {
    gridRow: row,
    gridColumn: col,
    gridColumnSpan: actualSpan,
    gridRowSpan: 1,
    // CSS Grid area string for direct CSS application
    gridArea: `${row} / ${col} / ${row + 1} / ${col + actualSpan}`,
    // Material-UI Grid item properties
    xs: 12, // Full width on mobile
    sm: actualSpan > 40 ? 12 : 6, // Responsive breakpoints
    md: actualSpan > 60 ? 12 : actualSpan > 40 ? 8 : 4,
    lg: actualSpan > 60 ? 12 : actualSpan > 40 ? 6 : 3,
    // Grid container properties for precise positioning
    container: false,
    item: true,
    spacing: 0
  };
}

/**
 * Maps BMS LENGTH and INITIAL values to React component defaults and validation
 * 
 * This function converts BMS LENGTH and INITIAL attributes to Material-UI TextField
 * properties including maxLength constraints, default values, and placeholder text.
 * 
 * @param {number} length - BMS LENGTH attribute value
 * @param {string} [initial] - BMS INITIAL attribute value
 * @returns {object} Material-UI TextField properties for length and initial values
 * 
 * @example
 * // Convert BMS password field: LENGTH=8 INITIAL='________'
 * const lengthProps = mapLengthAndInitialValues(8, '________');
 * // Returns: { maxLength: 8, defaultValue: '________', placeholder: 'Enter up to 8 characters' }
 */
export function mapLengthAndInitialValues(length, initial) {
  const props = {
    maxLength: length,
    inputProps: {
      maxLength: length
    }
  };

  // Set default value if initial is provided and not just underscores (password placeholder)
  if (initial && !initial.match(/^_+$/)) {
    props.defaultValue = initial;
  }

  // Set placeholder text based on length
  if (length <= 1) {
    props.placeholder = 'Enter 1 character';
  } else if (length <= 10) {
    props.placeholder = `Enter up to ${length} characters`;
  } else {
    props.placeholder = `Enter up to ${length} characters (${length} char limit)`;
  }

  // Add helper text for longer fields
  if (length > 50) {
    props.helperText = `Maximum ${length} characters allowed`;
  }

  return props;
}

/**
 * Converts BMS HILIGHT attributes to Material-UI styling
 * 
 * This function transforms BMS HILIGHT attributes (UNDERLINE, BLINK, REVERSE, OFF)
 * to equivalent Material-UI styling properties while maintaining accessibility
 * and respecting user motion preferences.
 * 
 * @param {string} [highlight] - BMS HILIGHT attribute value
 * @returns {object} Material-UI sx styling properties
 * 
 * @example
 * // Convert BMS underlined input field: HILIGHT=UNDERLINE
 * const highlightStyle = convertHighlightToStyling('UNDERLINE');
 * // Returns: { sx: { textDecoration: 'underline', textDecorationColor: 'currentColor' } }
 */
export function convertHighlightToStyling(highlight) {
  if (!highlight || highlight === 'OFF') {
    return {
      sx: ThemeConstants.HIGHLIGHT_STYLES.OFF
    };
  }

  const highlightStyles = {
    UNDERLINE: {
      sx: ThemeConstants.HIGHLIGHT_STYLES.UNDERLINE
    },
    BLINK: {
      sx: ThemeConstants.HIGHLIGHT_STYLES.BLINK
    },
    REVERSE: {
      sx: ThemeConstants.HIGHLIGHT_STYLES.REVERSE
    }
  };

  const style = highlightStyles[highlight];
  if (!style) {
    console.warn(`Unknown BMS highlight: ${highlight}, defaulting to OFF`);
    return { sx: ThemeConstants.HIGHLIGHT_STYLES.OFF };
  }

  return style;
}

/**
 * Converts complete BMS DFHMDF field definition to React component properties
 * 
 * This is the comprehensive function that combines all BMS field attributes into
 * a complete Material-UI TextField configuration. It processes all DFHMDF parameters
 * and produces a ready-to-use React component property object.
 * 
 * @param {BmsFieldDefinition} fieldDef - Complete BMS field definition
 * @returns {MaterialUIProps} Complete Material-UI component properties
 * 
 * @example
 * // Convert complete BMS user ID field definition
 * const userIdField = {
 *   name: 'USERID',
 *   attrb: ['FSET', 'IC', 'NORM', 'UNPROT'],
 *   color: 'GREEN',
 *   hilight: 'OFF',
 *   length: 8,
 *   pos: { row: 19, col: 43 },
 *   initial: undefined
 * };
 * const reactProps = convertDfhmdfToReactProps(userIdField);
 */
export function convertDfhmdfToReactProps(fieldDef) {
  // Start with base properties
  const reactProps = {
    name: fieldDef.name,
    key: fieldDef.name,
    id: fieldDef.name.toLowerCase()
  };

  // Convert BMS attributes to Material-UI properties
  const attributeProps = convertBmsAttributesToMuiProps(fieldDef.attrb);
  Object.assign(reactProps, attributeProps);

  // Apply color mapping
  const colorMapping = mapBmsColorsToTheme(fieldDef.color);
  reactProps.color = colorMapping.color;

  // Apply highlight styling
  const highlightStyle = convertHighlightToStyling(fieldDef.hilight);
  reactProps.sx = { ...reactProps.sx, ...highlightStyle.sx };

  // Apply length and initial value constraints
  const lengthProps = mapLengthAndInitialValues(fieldDef.length, fieldDef.initial);
  Object.assign(reactProps, lengthProps);

  // Apply positioning for grid layout
  const gridProps = convertPosToGridProperties(fieldDef.pos.row, fieldDef.pos.col, fieldDef.length);
  reactProps.gridProps = gridProps;

  // Apply text justification if specified
  if (fieldDef.justify) {
    const justifyStyles = {
      LEFT: { textAlign: 'left' },
      RIGHT: { textAlign: 'right' },
      CENTER: { textAlign: 'center' }
    };
    reactProps.sx = { ...reactProps.sx, ...justifyStyles[fieldDef.justify] };
  }

  // Apply validation schema if PICIN is specified
  if (fieldDef.picin) {
    reactProps.validationSchema = convertPicinToValidation(fieldDef.picin);
  }

  // Apply output formatting if PICOUT is specified
  if (fieldDef.picout) {
    reactProps.formatPattern = fieldDef.picout;
  }

  return reactProps;
}

/**
 * Creates a field mapper function for processing multiple BMS fields
 * 
 * This function returns a mapper function that can process arrays of BMS field
 * definitions and convert them to React component properties. It's useful for
 * batch processing of entire BMS mapsets.
 * 
 * @param {object} [options] - Configuration options for the mapper
 * @returns {function} Mapper function that converts BMS fields to React props
 * 
 * @example
 * // Create a mapper for login screen fields
 * const loginMapper = createBmsFieldMapper({ screenName: 'COSGN00' });
 * const reactFields = loginFields.map(loginMapper);
 */
export function createBmsFieldMapper(options = {}) {
  return function mapBmsField(fieldDef) {
    try {
      // Convert the field definition to React props
      const reactProps = convertDfhmdfToReactProps(fieldDef);
      
      // Apply any global options
      if (options.screenName) {
        reactProps.className = `${reactProps.className || ''} ${options.screenName}-field`.trim();
      }
      
      if (options.theme) {
        // Apply theme-specific styling
        const themeColors = mapBmsColorsToTheme(fieldDef.color);
        reactProps.sx = {
          ...reactProps.sx,
          ...options.theme.palette[themeColors.color]
        };
      }
      
      if (options.accessibility) {
        // Enhanced accessibility properties
        reactProps['aria-label'] = fieldDef.name;
        reactProps['aria-describedby'] = `${fieldDef.name}-help`;
      }
      
      return reactProps;
    } catch (error) {
      console.error(`Error converting BMS field ${fieldDef.name}:`, error);
      
      // Return minimal safe props on error
      return {
        name: fieldDef.name,
        key: fieldDef.name,
        id: fieldDef.name.toLowerCase(),
        variant: 'outlined',
        error: true,
        helperText: 'Field conversion error'
      };
    }
  };
}

/**
 * Converts BMS PICIN patterns to Yup validation schemas
 * 
 * This function transforms BMS PICIN input patterns into Yup validation schemas
 * for use with React Hook Form. It maintains exact COBOL validation logic
 * while providing modern client-side validation capabilities.
 * 
 * @param {string} picinPattern - BMS PICIN pattern string
 * @returns {yup.Schema} Yup validation schema
 * 
 * @example
 * // Convert BMS account number pattern: PICIN='99999999999'
 * const accountSchema = convertPicinToValidation('99999999999');
 * // Returns: yup.string().matches(/^\d{11}$/, 'Must be exactly 11 digits').required()
 */
export function convertPicinToValidation(picinPattern) {
  if (!picinPattern) {
    return yup.string();
  }

  // Find matching pattern in validation constants
  const validationPattern = ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER;
  
  // Handle different PICIN pattern types
  if (picinPattern.match(/^9+$/)) {
    // Pure numeric pattern like '99999999999'
    const digitCount = picinPattern.length;
    return yup.string()
      .matches(new RegExp(`^\\d{${digitCount}}$`), `Must be exactly ${digitCount} digits`)
      .required('This field is required');
  }
  
  if (picinPattern.match(/^X+$/)) {
    // Alphanumeric pattern like 'XXXXXXXX'
    const charCount = picinPattern.length;
    return yup.string()
      .max(charCount, `Must be no more than ${charCount} characters`)
      .required('This field is required');
  }
  
  if (picinPattern.includes('-')) {
    // Date or formatted pattern like '99-99-9999'
    const pattern = picinPattern.replace(/9/g, '\\d').replace(/X/g, '[A-Z0-9]');
    return yup.string()
      .matches(new RegExp(`^${pattern}$`), `Must match format: ${picinPattern}`)
      .required('This field is required');
  }
  
  // Handle specific known patterns
  const knownPatterns = {
    '99999999999': ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER,
    '9999999999999999': ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER,
    '999999999': ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.CUSTOMER_ID,
    '999': ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.FICO_SCORE,
    '99999': ValidationConstants.PICIN_PATTERNS.NUMERIC_PATTERNS.ZIP_CODE
  };
  
  const knownPattern = knownPatterns[picinPattern];
  if (knownPattern) {
    let schema = yup.string()
      .matches(knownPattern.pattern, knownPattern.message)
      .required('This field is required');
    
    // Add numeric constraints if available
    if (knownPattern.min !== undefined) {
      schema = schema.test('min-value', knownPattern.message, function(value) {
        const numValue = parseInt(value);
        return numValue >= knownPattern.min && numValue <= knownPattern.max;
      });
    }
    
    return schema;
  }
  
  // Default validation for unknown patterns
  console.warn(`Unknown PICIN pattern: ${picinPattern}, applying default validation`);
  return yup.string()
    .max(picinPattern.length, `Must be no more than ${picinPattern.length} characters`)
    .required('This field is required');
}

// Export default object containing all mapping functions
export default {
  convertBmsAttributesToMuiProps,
  mapBmsColorsToTheme,
  convertPosToGridProperties,
  mapLengthAndInitialValues,
  convertHighlightToStyling,
  convertDfhmdfToReactProps,
  createBmsFieldMapper,
  convertPicinToValidation
};