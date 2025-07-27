/**
 * useBmsFieldAttributes.js
 * 
 * Custom React hook implementing BMS mapset attribute byte translation to Material-UI 
 * component properties. Provides field protection, color mapping, input validation 
 * attributes, and tab order management that preserve exact mainframe field behavior 
 * while enabling modern Material-UI styling and interaction patterns.
 * 
 * This hook processes BMS DFHMDF attributes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, 
 * FSET) and converts them to equivalent Material-UI TextField properties, HTML input 
 * attributes, and React component props that maintain identical field behavior from 
 * the original 3270 terminal interface.
 * 
 * Supports all 18 BMS maps in the CardDemo application with exact preservation of:
 * - Field protection states (read-only, disabled, editable)
 * - Input validation rules (numeric, required, format patterns)
 * - Display attributes (color, intensity, highlighting)
 * - Navigation behavior (tab order, initial cursor positioning)
 * - Change detection (FSET attribute tracking)
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

import { useCallback } from 'react';
import { useTheme } from '@mui/material/styles';
import { DISPLAY_ATTRIBUTES } from '../constants/ThemeConstants';
import { ATTRIBUTE_MAPPINGS } from '../constants/FieldConstants';
import { FormFieldAttributes } from '../types/CommonTypes';

/**
 * Maps BMS color attributes to Material-UI theme palette colors
 * Preserves semantic meaning of original BMS colors while ensuring WCAG 2.1 AA compliance
 * 
 * @param {string} bmsColor - BMS color attribute (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL)
 * @param {object} theme - Material-UI theme object from useTheme hook
 * @returns {string} Material-UI theme color value
 */
export const mapBmsColorToTheme = (bmsColor, theme) => {
  // BMS color to Material-UI theme palette mapping
  const colorMapping = {
    GREEN: theme.palette.text.primary,      // Normal data display and field labels
    RED: theme.palette.error.main,          // Error messages and validation failures
    YELLOW: theme.palette.warning.main,     // Highlighted fields and important notices
    BLUE: theme.palette.info.main,          // Informational messages and help text
    TURQUOISE: theme.palette.info.light,    // Interactive field labels and prompts
    NEUTRAL: theme.palette.text.secondary,  // Standard text display
  };

  return colorMapping[bmsColor] || theme.palette.text.primary;
};

/**
 * Maps BMS field protection attributes to HTML input properties
 * Converts BMS protection states (ASKIP, UNPROT, PROT) to equivalent HTML attributes
 * that control field editability and user interaction behavior
 * 
 * @param {string|string[]} attrb - BMS attribute or array of attributes
 * @returns {object} HTML input properties object
 */
export const mapBmsProtectionToInput = (attrb) => {
  const attributes = Array.isArray(attrb) ? attrb : [attrb];
  const inputProps = {};

  // Process each BMS attribute and set corresponding HTML properties
  attributes.forEach(attr => {
    switch (attr) {
      case 'ASKIP':
        // Auto-skip fields: read-only, cursor skips over field
        inputProps.readOnly = true;
        inputProps.tabIndex = -1;
        break;
        
      case 'UNPROT':
        // Unprotected fields: user can input, normal tab behavior
        inputProps.readOnly = false;
        inputProps.disabled = false;
        break;
        
      case 'PROT': 
        // Protected fields: read-only but cursor can focus
        inputProps.disabled = true;
        break;
        
      case 'NUM':
        // Numeric fields: restrict to numeric input, right-align text
        inputProps.inputMode = 'numeric';
        inputProps.pattern = '[0-9]*';
        break;
        
      case 'IC':
        // Initial cursor: set autofocus for first field in tab order
        inputProps.autoFocus = true;
        break;
        
      case 'DRK':
        // Dark attribute: password-style hidden input
        inputProps.type = 'password';
        break;
        
      default:
        // No special processing for other attributes (NORM, BRT, FSET)
        break;
    }
  });

  return inputProps;
};

/**
 * Maps complete BMS field attributes to Material-UI component properties
 * Combines protection, validation, styling, and behavior attributes into a comprehensive
 * Material-UI TextField configuration that preserves exact BMS field behavior
 * 
 * @param {FormFieldAttributes} fieldAttributes - Complete BMS field attribute definition
 * @param {object} theme - Material-UI theme object
 * @returns {object} Complete Material-UI TextField properties object
 */
export const mapBmsAttributesToMuiProps = (fieldAttributes, theme) => {
  const { attrb, color, hilight, length, pos, initial, picin, validn } = fieldAttributes;
  
  // Start with base TextField properties
  const muiProps = {
    variant: 'outlined',
    size: 'small',
    fullWidth: false,
    InputProps: {},
    inputProps: {},
    FormHelperTextProps: {},
    sx: {},
  };

  // Apply BMS attribute mappings from FieldConstants
  const attributes = Array.isArray(attrb) ? attrb : [attrb];
  
  // Process protection and input behavior attributes
  const inputProperties = mapBmsProtectionToInput(attrb);
  Object.assign(muiProps.inputProps, inputProperties);
  
  // Apply pre-configured attribute mappings
  attributes.forEach(attr => {
    switch (attr) {
      case 'ASKIP':
        Object.assign(muiProps, ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.props);
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.styles);
        break;
        
      case 'UNPROT':
        Object.assign(muiProps, ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.props);
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.styles);
        break;
        
      case 'PROT':
        Object.assign(muiProps, ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.props);
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.styles);
        break;
        
      case 'NUM':
        Object.assign(muiProps, ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.props);
        Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.styles);
        break;
        
      case 'IC':
        Object.assign(muiProps, ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS.props);
        break;
        
      default:
        break;
    }
  });

  // Apply validation rules from MUSTFILL and validn attributes
  if (validn === 'MUSTFILL' || attributes.includes('MUSTFILL')) {
    Object.assign(muiProps, ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED.props);
    Object.assign(muiProps.sx, ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED.styles);
  }

  // Set field length constraint from BMS LENGTH attribute
  if (length) {
    muiProps.inputProps.maxLength = length;
    // Set minimum width based on field length for proper display
    muiProps.sx.minWidth = Math.max(length * 8, 80) + 'px';
  }

  // Apply BMS color mapping to Material-UI theme colors
  if (color) {
    const themeColor = mapBmsColorToTheme(color, theme);
    muiProps.sx.color = themeColor;
    
    // Apply appropriate variant based on color semantic meaning
    if (color === 'RED') {
      muiProps.error = true;
    } else if (color === 'YELLOW') {
      muiProps.color = 'warning';
    } else if (color === 'BLUE') {
      muiProps.color = 'info';
    } else if (color === 'GREEN') {
      muiProps.color = 'success';
    }
  }

  // Apply BMS display attributes (BRT, NORM, DRK) to Material-UI styling
  if (attributes.includes('BRT')) {
    Object.assign(muiProps.sx, DISPLAY_ATTRIBUTES.BRT);
  } else if (attributes.includes('DRK')) {
    Object.assign(muiProps.sx, DISPLAY_ATTRIBUTES.DRK);
  } else {
    Object.assign(muiProps.sx, DISPLAY_ATTRIBUTES.NORM);
  }

  // Apply BMS highlight attributes to Material-UI styling
  if (hilight && hilight !== 'OFF') {
    switch (hilight) {
      case 'UNDERLINE':
        muiProps.sx.textDecoration = 'underline';
        muiProps.sx.textUnderlineOffset = '2px';
        break;
        
      case 'BLINK':
        // Convert BLINK to enhanced visual emphasis (no animation for accessibility)
        muiProps.sx.backgroundColor = 'rgba(255, 193, 7, 0.1)';
        muiProps.sx.border = '1px dashed #ff9800';
        muiProps.sx.borderRadius = '2px';
        muiProps.sx.fontWeight = 600;
        break;
        
      case 'REVERSE':
        // Reverse video effect using color inversion
        muiProps.sx.backgroundColor = 'currentColor';
        muiProps.sx.color = 'white';
        muiProps.sx.filter = 'invert(1)';
        break;
        
      default:
        break;
    }
  }

  // Set initial value from BMS INITIAL attribute
  if (initial !== undefined && initial !== null) {
    muiProps.defaultValue = initial;
  }

  // Apply input picture format from BMS PICIN attribute
  if (picin) {
    // Convert BMS PICIN to HTML pattern attribute
    // Example: '99999999999' becomes pattern for 11 digits
    const digitCount = (picin.match(/9/g) || []).length;
    if (digitCount > 0) {
      muiProps.inputProps.pattern = `\\d{${digitCount}}`;
      muiProps.inputProps.title = `Enter exactly ${digitCount} digits`;
    }
  }

  // Set CSS Grid positioning from BMS POS attribute
  if (pos) {
    muiProps.sx.gridColumn = pos.column;
    muiProps.sx.gridRow = pos.row;
  }

  return muiProps;
};

/**
 * Main React hook for BMS field attribute processing
 * Provides memoized functions for converting BMS field definitions to Material-UI 
 * component properties, with automatic theme integration and performance optimization
 * 
 * @returns {object} Hook interface with attribute processing functions
 */
const useBmsFieldAttributes = () => {
  const theme = useTheme();

  // Memoized function to process complete BMS field attributes
  const processFieldAttributes = useCallback((fieldAttributes) => {
    return mapBmsAttributesToMuiProps(fieldAttributes, theme);
  }, [theme]);

  // Memoized function to map BMS colors to theme colors
  const mapColorToTheme = useCallback((bmsColor) => {
    return mapBmsColorToTheme(bmsColor, theme);
  }, [theme]);

  // Memoized function to map protection attributes to input properties
  const mapProtectionToInput = useCallback((attrb) => {
    return mapBmsProtectionToInput(attrb);
  }, []);

  // Memoized function to create tab order array from field positions
  const createTabOrder = useCallback((fieldsArray) => {
    // Sort fields by BMS position (row first, then column) to preserve tab order
    return fieldsArray
      .filter(field => !field.attrb.includes('ASKIP')) // Skip ASKIP fields in tab order
      .sort((a, b) => {
        if (a.pos.row !== b.pos.row) {
          return a.pos.row - b.pos.row;
        }
        return a.pos.column - b.pos.column;
      })
      .map((field, index) => ({
        ...field,
        tabIndex: field.attrb.includes('IC') ? 0 : index + 1, // IC gets first focus
      }));
  }, []);

  // Memoized function to validate field attributes against BMS rules
  const validateFieldAttributes = useCallback((fieldAttributes) => {
    const errors = [];
    const { attrb, length, pos, picin } = fieldAttributes;

    // Validate BMS attribute combinations
    const attributes = Array.isArray(attrb) ? attrb : [attrb];
    
    // Check for conflicting protection attributes
    const protectionAttrs = attributes.filter(attr => 
      ['ASKIP', 'UNPROT', 'PROT'].includes(attr)
    );
    if (protectionAttrs.length > 1) {
      errors.push('Conflicting protection attributes detected');
    }

    // Validate field length constraints
    if (length && (length < 1 || length > 255)) {
      errors.push('Field length must be between 1 and 255 characters');
    }

    // Validate BMS position coordinates
    if (pos && (pos.row < 1 || pos.row > 24 || pos.column < 1 || pos.column > 80)) {
      errors.push('Field position must be within 24x80 screen boundaries');
    }

    // Validate PICIN format for numeric fields
    if (picin && attributes.includes('NUM')) {
      const isValidPicin = /^9+$/.test(picin); // Should be all 9's for numeric
      if (!isValidPicin) {
        errors.push('PICIN format invalid for numeric field');
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }, []);

  // Return hook interface with all processing functions
  return {
    processFieldAttributes,
    mapColorToTheme,
    mapProtectionToInput,
    createTabOrder,
    validateFieldAttributes,
    theme,
  };
};

export default useBmsFieldAttributes;