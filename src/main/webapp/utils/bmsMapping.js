/**
 * CardDemo - BMS to Material-UI Mapping Utilities
 * 
 * Utility functions that translate BMS mapset definitions and attribute bytes to Material-UI
 * component properties, ensuring pixel-perfect preservation of original screen layouts and
 * field behaviors in React components.
 * 
 * Converts BMS DFHMDF field definitions from 3270 terminal format to modern Material-UI
 * components while maintaining exact functional equivalence with original COBOL/CICS behavior.
 * 
 * Key mapping functions:
 * - BMS attribute bytes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, DRK) to Material-UI props
 * - BMS color attributes (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL) to theme palette
 * - BMS POS coordinates (24x80 terminal grid) to CSS Grid Layout properties
 * - BMS LENGTH and INITIAL values to React component defaults and validation constraints
 * - BMS HILIGHT attributes (UNDERLINE, BLINK, REVERSE) to Material-UI styling
 * - BMS PICIN patterns to Yup validation schemas for React Hook Form
 * 
 * @author Blitzy agent
 * @version 1.0.0
 */

import React from 'react';
import { Grid, ThemeOptions, PaletteOptions, TypographyOptions } from '@mui/material';
import { GridProps, GridSize, GridSpacing } from '@mui/material';
import { Theme } from '@mui/material/styles';
import { ComponentProps, CSSProperties, ReactElement } from 'react';
import * as yup from 'yup';
import { string, number, object, ValidationSchema } from 'yup';

import { ThemeConstants } from '../constants/ThemeConstants';
import { FieldConstants } from '../constants/FieldConstants';
import { ValidationConstants } from '../constants/ValidationConstants';
import { CommonTypes } from '../types/CommonTypes';

const {
  BMS_COLORS,
  DISPLAY_ATTRIBUTES,
  HIGHLIGHT_STYLES,
  TYPOGRAPHY_VARIANTS
} = ThemeConstants;

const {
  BMS_ATTRIBUTES,
  FIELD_POSITIONING,
  FIELD_LENGTHS,
  ATTRIBUTE_MAPPINGS
} = FieldConstants;

const {
  PICIN_PATTERNS,
  VALIDATION_RULES,
  INPUT_MASKS,
  FIELD_CONSTRAINTS
} = ValidationConstants;

const {
  FormFieldAttributes,
  BaseScreenData,
  BmsFieldDefinition,
  MaterialUIProps
} = CommonTypes;

/**
 * Converts BMS attribute bytes to Material-UI TextField properties
 * 
 * Maps BMS field attributes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, DRK) to
 * corresponding Material-UI component properties maintaining exact field behavior
 * from original 3270 terminal screens.
 * 
 * @param {string[]} bmsAttributes - Array of BMS attribute bytes (e.g., ['ASKIP', 'NORM'])
 * @returns {object} Material-UI TextField properties object
 * 
 * @example
 * // Convert BMS protected field attributes
 * const muiProps = convertBmsAttributesToMuiProps(['ASKIP', 'NORM']);
 * // Returns: { inputProps: { readOnly: true, tabIndex: -1 }, sx: { ... } }
 */
export const convertBmsAttributesToMuiProps = (bmsAttributes) => {
  const muiProps = {
    inputProps: {},
    sx: {
      '& .MuiInputBase-input': {}
    }
  };

  // Process each BMS attribute and apply corresponding Material-UI properties
  bmsAttributes.forEach(attribute => {
    switch (attribute) {
      case BMS_ATTRIBUTES.ASKIP:
        // Auto-skip protected field - read-only, not focusable
        Object.assign(muiProps.inputProps, ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.inputProps);
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.sx);
        break;

      case BMS_ATTRIBUTES.UNPROT:
        // Unprotected field - allows user input
        Object.assign(muiProps.inputProps, ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.inputProps);
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.sx);
        break;

      case BMS_ATTRIBUTES.PROT:
        // Protected field - disabled input
        muiProps.disabled = ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.disabled;
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.sx);
        break;

      case BMS_ATTRIBUTES.NUM:
        // Numeric-only field
        Object.assign(muiProps.inputProps, ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.inputProps);
        Object.assign(muiProps.sx['& .MuiInputBase-input'], ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.sx['& .MuiInputBase-input']);
        break;

      case BMS_ATTRIBUTES.IC:
        // Initial cursor - field gets focus on load
        muiProps.autoFocus = ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS.autoFocus;
        Object.assign(muiProps.sx['& .MuiInputBase-input'], ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS.sx['& .MuiInputBase-input']);
        break;

      case BMS_ATTRIBUTES.BRT:
        // Bright intensity - bold styling
        Object.assign(muiProps.sx['& .MuiInputBase-input'], ATTRIBUTE_MAPPINGS.BRT_TO_BOLD.sx['& .MuiInputBase-input']);
        break;

      case BMS_ATTRIBUTES.NORM:
        // Normal intensity - regular styling
        Object.assign(muiProps.sx['& .MuiInputBase-input'], ATTRIBUTE_MAPPINGS.NORM_TO_NORMAL.sx['& .MuiInputBase-input']);
        break;

      case BMS_ATTRIBUTES.DRK:
        // Dark/hidden field - password type
        muiProps.type = ATTRIBUTE_MAPPINGS.DRK_TO_DIMMED.type;
        Object.assign(muiProps.sx['& .MuiInputBase-input'], ATTRIBUTE_MAPPINGS.DRK_TO_DIMMED.sx['& .MuiInputBase-input']);
        break;

      case BMS_ATTRIBUTES.FSET:
        // Field set - change tracking enabled
        muiProps.onChange = ATTRIBUTE_MAPPINGS.FSET_TO_CHANGE_TRACKING.onChange;
        Object.assign(muiProps.sx['& .MuiInputBase-input'], ATTRIBUTE_MAPPINGS.FSET_TO_CHANGE_TRACKING.sx['& .MuiInputBase-input']);
        break;

      case BMS_ATTRIBUTES.MUSTFILL:
        // Required field
        muiProps.required = ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED.required;
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED.sx);
        break;

      default:
        // Unknown attribute - log warning but continue processing
        console.warn(`Unknown BMS attribute: ${attribute}`);
        break;
    }
  });

  return muiProps;
};

/**
 * Maps BMS color attributes to Material-UI theme palette colors
 * 
 * Converts BMS color specifications (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL)
 * to Material-UI theme palette colors maintaining visual consistency with original
 * 3270 terminal appearance while ensuring WCAG 2.1 AA accessibility compliance.
 * 
 * @param {string} bmsColor - BMS color name (e.g., 'GREEN', 'RED')
 * @param {object} theme - Material-UI theme object for context-aware color mapping
 * @returns {object} Material-UI color object with main, light, dark, and contrastText properties
 * 
 * @example
 * // Map BMS GREEN color to theme palette
 * const colorProps = mapBmsColorsToTheme('GREEN', theme);
 * // Returns: { main: '#2e7d32', light: '#4caf50', dark: '#1b5e20', contrastText: '#ffffff' }
 */
export const mapBmsColorsToTheme = (bmsColor, theme) => {
  // Direct mapping from BMS color names to Material-UI theme colors
  const colorMapping = {
    'GREEN': BMS_COLORS.GREEN,
    'RED': BMS_COLORS.RED,
    'YELLOW': BMS_COLORS.YELLOW,
    'BLUE': BMS_COLORS.BLUE,
    'TURQUOISE': BMS_COLORS.TURQUOISE,
    'NEUTRAL': BMS_COLORS.NEUTRAL
  };

  // Return the mapped color object or default to neutral if not found
  return colorMapping[bmsColor] || BMS_COLORS.NEUTRAL;
};

/**
 * Converts BMS POS coordinates to CSS Grid Layout properties
 * 
 * Transforms BMS POS=(row,col) positioning from 24x80 terminal grid coordinates
 * to Material-UI Grid component properties enabling responsive layout while
 * preserving exact field relationships from original BMS screen positioning.
 * 
 * @param {number} row - BMS row position (1-24)
 * @param {number} col - BMS column position (1-80)
 * @param {number} length - Field length for calculating grid span
 * @returns {object} Material-UI Grid properties for responsive positioning
 * 
 * @example
 * // Convert BMS POS=(19,43) to Grid properties
 * const gridProps = convertPosToGridProperties(19, 43, 8);
 * // Returns: { xs: 2, md: 2, lg: 2, item: true, style: { gridRow: 19, gridColumn: 43 } }
 */
export const convertPosToGridProperties = (row, col, length) => {
  const { LAYOUT_UTILS } = FIELD_POSITIONING;
  
  // Calculate Material-UI Grid size based on field length
  const gridSize = LAYOUT_UTILS.calculateSpan(length);
  
  // Convert BMS coordinates to CSS Grid coordinates
  const gridCoordinates = LAYOUT_UTILS.toGridCoordinates(row, col);
  
  // Create Material-UI Grid properties for responsive layout
  const gridProps = {
    xs: gridSize,
    md: gridSize,
    lg: gridSize,
    item: true,
    style: {
      gridRow: gridCoordinates.gridRow,
      gridColumn: gridCoordinates.gridColumn,
      gridColumnEnd: `span ${Math.ceil(length / 10)}`, // Approximate span based on character width
      alignSelf: 'center'
    }
  };

  return gridProps;
};

/**
 * Maps BMS LENGTH and INITIAL values to React component defaults and validation constraints
 * 
 * Converts BMS LENGTH attribute and INITIAL value to Material-UI TextField properties
 * including maxLength constraints, placeholder text, and default values maintaining
 * exact field behavior from original BMS definitions.
 * 
 * @param {number} length - BMS LENGTH attribute value
 * @param {string} initialValue - BMS INITIAL attribute value (optional)
 * @param {string} fieldName - Field name for length constraint lookup
 * @returns {object} Material-UI TextField properties with length constraints and defaults
 * 
 * @example
 * // Map BMS LENGTH=8 INITIAL='USERNAME' to TextField properties
 * const fieldProps = mapLengthAndInitialValues(8, 'USERNAME', 'USERID');
 * // Returns: { inputProps: { maxLength: 8 }, defaultValue: 'USERNAME', placeholder: '8 characters' }
 */
export const mapLengthAndInitialValues = (length, initialValue, fieldName) => {
  const fieldProps = {
    inputProps: {
      maxLength: length
    }
  };

  // Set default value if INITIAL attribute is provided
  if (initialValue && initialValue.trim() !== '') {
    fieldProps.defaultValue = initialValue;
  }

  // Set placeholder text based on field length
  fieldProps.placeholder = `${length} characters`;

  // Apply specific length constraints for known field types
  const lengthConstraints = FIELD_LENGTHS.LENGTH_CONSTRAINTS;
  if (lengthConstraints[fieldName]) {
    fieldProps.inputProps.maxLength = lengthConstraints[fieldName];
  }

  // Add helper text for field length indication
  fieldProps.helperText = `Maximum ${length} characters`;

  return fieldProps;
};

/**
 * Converts BMS HILIGHT attributes to Material-UI styling
 * 
 * Maps BMS HILIGHT attributes (UNDERLINE, BLINK, REVERSE) to Material-UI
 * styling objects maintaining visual emphasis from original 3270 terminal
 * while adapting to modern web accessibility standards.
 * 
 * @param {string} highlightType - BMS HILIGHT attribute value
 * @returns {object} Material-UI sx styling object for visual emphasis
 * 
 * @example
 * // Convert BMS HILIGHT=UNDERLINE to Material-UI styling
 * const styling = convertHighlightToStyling('UNDERLINE');
 * // Returns: { borderBottom: '2px solid', borderBottomColor: 'primary.main', ... }
 */
export const convertHighlightToStyling = (highlightType) => {
  const highlightMapping = {
    'UNDERLINE': HIGHLIGHT_STYLES.UNDERLINE,
    'BLINK': HIGHLIGHT_STYLES.BLINK,
    'REVERSE': HIGHLIGHT_STYLES.REVERSE,
    'OFF': HIGHLIGHT_STYLES.OFF
  };

  return highlightMapping[highlightType] || HIGHLIGHT_STYLES.OFF;
};

/**
 * Converts BMS DFHMDF field definitions to React component property configurations
 * 
 * Comprehensive conversion function that processes complete BMS DFHMDF field definitions
 * into React component properties combining all BMS attributes (ATTRB, COLOR, LENGTH,
 * POS, INITIAL, HILIGHT, PICIN, VALIDN) into unified Material-UI component configuration.
 * 
 * @param {object} bmsFieldDef - Complete BMS DFHMDF field definition object
 * @returns {object} Complete React component property configuration
 * 
 * @example
 * // Convert complete BMS field definition
 * const bmsField = {
 *   attrb: ['UNPROT', 'IC', 'NORM'],
 *   color: 'GREEN',
 *   length: 8,
 *   pos: { row: 19, col: 43 },
 *   initial: '',
 *   hilight: 'OFF',
 *   picin: '99999999',
 *   validn: ['MUSTFILL']
 * };
 * const reactProps = convertDfhmdfToReactProps(bmsField);
 */
export const convertDfhmdfToReactProps = (bmsFieldDef) => {
  const reactProps = {
    // Base Material-UI TextField properties
    variant: 'outlined',
    size: 'small',
    fullWidth: false
  };

  // Process BMS attributes to Material-UI properties
  if (bmsFieldDef.attrb && bmsFieldDef.attrb.length > 0) {
    const attributeProps = convertBmsAttributesToMuiProps(bmsFieldDef.attrb);
    Object.assign(reactProps, attributeProps);
  }

  // Process BMS color to Material-UI color
  if (bmsFieldDef.color) {
    const colorProps = mapBmsColorsToTheme(bmsFieldDef.color);
    reactProps.sx = reactProps.sx || {};
    reactProps.sx['& .MuiInputBase-input'] = reactProps.sx['& .MuiInputBase-input'] || {};
    reactProps.sx['& .MuiInputBase-input'].color = colorProps.main;
  }

  // Process BMS position to Grid properties
  if (bmsFieldDef.pos && bmsFieldDef.length) {
    const gridProps = convertPosToGridProperties(
      bmsFieldDef.pos.row,
      bmsFieldDef.pos.column,
      bmsFieldDef.length
    );
    reactProps.gridProps = gridProps;
  }

  // Process BMS length and initial values
  if (bmsFieldDef.length) {
    const lengthProps = mapLengthAndInitialValues(
      bmsFieldDef.length,
      bmsFieldDef.initial,
      bmsFieldDef.fieldName
    );
    Object.assign(reactProps, lengthProps);
  }

  // Process BMS highlight to styling
  if (bmsFieldDef.hilight) {
    const highlightStyling = convertHighlightToStyling(bmsFieldDef.hilight);
    reactProps.sx = reactProps.sx || {};
    Object.assign(reactProps.sx, highlightStyling);
  }

  // Process BMS PICIN to validation
  if (bmsFieldDef.picin) {
    const validationProps = convertPicinToValidation(bmsFieldDef.picin);
    reactProps.validation = validationProps;
  }

  // Process BMS VALIDN attributes
  if (bmsFieldDef.validn && bmsFieldDef.validn.length > 0) {
    bmsFieldDef.validn.forEach(validationRule => {
      if (validationRule.type === 'MUSTFILL' || validationRule.required) {
        reactProps.required = true;
      }
    });
  }

  return reactProps;
};

/**
 * Creates a comprehensive BMS field mapper function
 * 
 * Factory function that creates a specialized field mapping function configured
 * for specific BMS mapset requirements. The returned function can efficiently
 * convert multiple BMS fields to React components with consistent configuration.
 * 
 * @param {object} mapsetConfig - Configuration object for BMS mapset processing
 * @returns {function} Configured field mapping function
 * 
 * @example
 * // Create mapper for login screen (COSGN00)
 * const loginMapper = createBmsFieldMapper({
 *   mapsetName: 'COSGN00',
 *   screenSize: { rows: 24, cols: 80 },
 *   theme: muiTheme
 * });
 * 
 * // Use mapper to convert field
 * const userIdProps = loginMapper(userIdBmsField);
 */
export const createBmsFieldMapper = (mapsetConfig) => {
  const { mapsetName, screenSize, theme } = mapsetConfig;

  return (bmsFieldDef) => {
    // Enhanced field processing with mapset-specific configuration
    const baseProps = convertDfhmdfToReactProps(bmsFieldDef);
    
    // Apply mapset-specific enhancements
    const enhancedProps = {
      ...baseProps,
      'data-mapset': mapsetName,
      'data-screen-size': `${screenSize.rows}x${screenSize.cols}`
    };

    // Apply theme-specific color mappings
    if (theme && bmsFieldDef.color) {
      const themeColorProps = mapBmsColorsToTheme(bmsFieldDef.color, theme);
      enhancedProps.sx = enhancedProps.sx || {};
      enhancedProps.sx['& .MuiInputBase-input'] = enhancedProps.sx['& .MuiInputBase-input'] || {};
      Object.assign(enhancedProps.sx['& .MuiInputBase-input'], {
        color: themeColorProps.main
      });
    }

    return enhancedProps;
  };
};

/**
 * Converts BMS PICIN patterns to Yup validation schemas
 * 
 * Transforms BMS PICIN input patterns (e.g., '99999999999' for account numbers)
 * to Yup validation schemas compatible with React Hook Form ensuring exact
 * validation behavior from original COBOL field validation logic.
 * 
 * @param {string} picinPattern - BMS PICIN pattern string
 * @returns {object} Yup validation schema object
 * 
 * @example
 * // Convert BMS PICIN='99999999999' to Yup schema
 * const schema = convertPicinToValidation('99999999999');
 * // Returns: yup.string().matches(/^\d{11}$/, 'Must be exactly 11 digits').required()
 */
export const convertPicinToValidation = (picinPattern) => {
  // Map common PICIN patterns to validation schemas
  const patternMappings = {
    // Account number - 11 digits
    '99999999999': yup.string()
      .matches(PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER.regex, 
               PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER.message)
      .required(),
    
    // Card number - 16 digits
    '9999999999999999': yup.string()
      .matches(PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER.regex,
               PICIN_PATTERNS.NUMERIC_PATTERNS.CARD_NUMBER.message)
      .required(),
    
    // Customer ID - 9 digits
    '999999999': yup.string()
      .matches(PICIN_PATTERNS.NUMERIC_PATTERNS.CUSTOMER_ID.regex,
               PICIN_PATTERNS.NUMERIC_PATTERNS.CUSTOMER_ID.message)
      .required(),
    
    // FICO Score - 3 digits
    '999': yup.number()
      .min(PICIN_PATTERNS.NUMERIC_PATTERNS.FICO_SCORE.min)
      .max(PICIN_PATTERNS.NUMERIC_PATTERNS.FICO_SCORE.max)
      .required(),
    
    // ZIP Code - 5 digits
    '99999': yup.string()
      .matches(PICIN_PATTERNS.NUMERIC_PATTERNS.ZIP_CODE.regex,
               PICIN_PATTERNS.NUMERIC_PATTERNS.ZIP_CODE.message)
      .required(),
    
    // Menu option - 1-2 digits
    '99': yup.string()
      .matches(PICIN_PATTERNS.NUMERIC_PATTERNS.MENU_OPTION.regex,
               PICIN_PATTERNS.NUMERIC_PATTERNS.MENU_OPTION.message)
      .required()
  };

  // Check for exact pattern match
  if (patternMappings[picinPattern]) {
    return patternMappings[picinPattern];
  }

  // Generate validation schema based on pattern analysis
  const digitCount = (picinPattern.match(/9/g) || []).length;
  const alphaCount = (picinPattern.match(/A/g) || []).length;
  const totalLength = picinPattern.length;

  if (digitCount === totalLength) {
    // Pure numeric pattern
    return yup.string()
      .matches(new RegExp(`^\\d{${digitCount}}$`), `Must be exactly ${digitCount} digits`)
      .required();
  } else if (alphaCount === totalLength) {
    // Pure alphabetic pattern
    return yup.string()
      .matches(new RegExp(`^[A-Za-z]{${alphaCount}}$`), `Must be exactly ${alphaCount} letters`)
      .required();
  } else {
    // Mixed pattern - default to string validation with length constraint
    return yup.string()
      .max(totalLength, `Maximum ${totalLength} characters`)
      .required();
  }
};

// Export all utility functions for external use
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