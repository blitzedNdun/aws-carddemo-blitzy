/**
 * useBmsFieldAttributes.js
 * 
 * Custom React hook implementing BMS (Basic Mapping Support) mapset attribute byte translation
 * to Material-UI component properties, providing field protection, color mapping, and input
 * validation attributes that preserve exact mainframe field behavior while enabling modern
 * Material-UI styling and interaction patterns.
 * 
 * This hook replicates the exact behavior of BMS DFHMDF attributes (ASKIP, UNPROT, PROT, NUM,
 * IC, BRT, NORM, FSET) through Material-UI TextField properties and React state management,
 * ensuring functional equivalence with the original COBOL/CICS/BMS application.
 * 
 * Key Features:
 * - Direct BMS attribute to Material-UI property mapping
 * - Color theme mapping preserving original visual hierarchy
 * - Field protection and validation implementation
 * - Tab order and field sequencing preservation
 * - Real-time validation with BMS-equivalent error handling
 * 
 * @version 1.0.0
 * @since 2024-01-15
 * 
 * Copyright (c) 2024 CardDemo Application
 * Technology transformation: COBOL/CICS/BMS → Java/Spring Boot/React
 */

import { useCallback } from 'react';
import { useTheme } from '@mui/material/styles';

// Import BMS constants and mappings from project dependencies
import { DISPLAY_ATTRIBUTES } from '../constants/ThemeConstants';
import { ATTRIBUTE_MAPPINGS } from '../constants/FieldConstants';
import { FormFieldAttributes } from '../types/CommonTypes';

/**
 * Maps BMS color attributes to Material-UI theme color palette
 * Preserves original visual hierarchy while ensuring WCAG 2.1 AA accessibility compliance
 * 
 * @param {string} bmsColor - BMS color attribute (BLUE, YELLOW, GREEN, RED, TURQUOISE, NEUTRAL)
 * @param {Object} theme - Material-UI theme object
 * @returns {string} Material-UI theme color value
 */
export const mapBmsColorToTheme = (bmsColor, theme) => {
  // BMS color to Material-UI theme mapping per Section 7.4.2 specification
  const colorMapping = {
    GREEN: theme.palette.text.primary,        // Normal data display and field labels
    RED: theme.palette.error.main,           // Error messages and validation failures
    YELLOW: theme.palette.warning.main,      // Highlighted fields and important notices
    BLUE: theme.palette.info.main,           // Informational messages and help text
    TURQUOISE: theme.palette.info.light,     // Secondary informational content
    NEUTRAL: theme.palette.text.secondary    // Standard text content
  };
  
  return colorMapping[bmsColor] || theme.palette.text.primary;
};

/**
 * Maps BMS field protection attributes to Material-UI input properties
 * Implements exact BMS field behavior through Material-UI component properties
 * 
 * @param {string} bmsAttribute - BMS attribute (ASKIP, UNPROT, PROT, NUM, IC, etc.)
 * @param {Object} theme - Material-UI theme object
 * @returns {Object} Material-UI TextField properties
 */
export const mapBmsProtectionToInput = (bmsAttribute, theme) => {
  const { BRT, NORM, DRK, INTENSITY_MAPPING } = DISPLAY_ATTRIBUTES;
  const {
    ASKIP_TO_READONLY,
    UNPROT_TO_EDITABLE,
    PROT_TO_DISABLED,
    NUM_TO_NUMERIC,
    IC_TO_AUTOFOCUS,
    MUSTFILL_TO_REQUIRED
  } = ATTRIBUTE_MAPPINGS;
  
  // Base properties for all fields
  const baseProps = {
    variant: 'outlined',
    size: 'small',
    fullWidth: true
  };
  
  // Apply BMS attribute-specific properties
  switch (bmsAttribute) {
    case 'ASKIP':
      // Auto-skip protected field (read-only display)
      return {
        ...baseProps,
        ...ASKIP_TO_READONLY.materialUIProps,
        sx: {
          '& .MuiInputBase-input': {
            cursor: 'default'
          }
        }
      };
      
    case 'UNPROT':
      // Unprotected field allowing user input
      return {
        ...baseProps,
        ...UNPROT_TO_EDITABLE.materialUIProps,
        sx: {
          '& .MuiInputBase-input': {
            cursor: 'text'
          }
        }
      };
      
    case 'PROT':
      // Protected field preventing user modification
      return {
        ...baseProps,
        ...PROT_TO_DISABLED.materialUIProps,
        sx: {
          '& .MuiInputBase-input': {
            cursor: 'not-allowed'
          }
        }
      };
      
    case 'NUM':
      // Numeric-only input validation
      return {
        ...baseProps,
        ...NUM_TO_NUMERIC.materialUIProps,
        sx: {
          '& .MuiInputBase-input': {
            textAlign: 'right' // Numeric fields right-aligned like mainframe
          }
        }
      };
      
    case 'IC':
      // Initial cursor positioning
      return {
        ...baseProps,
        ...IC_TO_AUTOFOCUS.materialUIProps
      };
      
    case 'BRT':
      // Bright intensity for emphasis
      return {
        ...baseProps,
        sx: {
          '& .MuiInputBase-input': {
            ...BRT,
            fontWeight: 'bold'
          }
        }
      };
      
    case 'NORM':
      // Normal intensity for standard display
      return {
        ...baseProps,
        sx: {
          '& .MuiInputBase-input': {
            ...NORM,
            fontWeight: 'normal'
          }
        }
      };
      
    case 'DRK':
      // Dark attribute for masked input (passwords)
      return {
        ...baseProps,
        type: 'password',
        sx: {
          '& .MuiInputBase-input': {
            ...DRK,
            fontWeight: 'lighter'
          }
        }
      };
      
    case 'FSET':
      // Field set attribute for change detection
      return {
        ...baseProps,
        onChange: (event) => {
          // Mark field as modified for change tracking
          event.target.setAttribute('data-modified', 'true');
        }
      };
      
    case 'MUSTFILL':
      // Required field validation
      return {
        ...baseProps,
        ...MUSTFILL_TO_REQUIRED.materialUIProps,
        sx: {
          '& .MuiInputLabel-root': {
            '&:after': {
              content: '" *"',
              color: theme.palette.error.main
            }
          }
        }
      };
      
    default:
      return baseProps;
  }
};

/**
 * Maps complete BMS field attributes to Material-UI component properties
 * Combines all BMS attributes into a single comprehensive property object
 * 
 * @param {FormFieldAttributes} fieldAttributes - BMS field attributes object
 * @param {Object} theme - Material-UI theme object
 * @returns {Object} Complete Material-UI TextField properties
 */
export const mapBmsAttributesToMuiProps = (fieldAttributes, theme) => {
  const { attrb, color, hilight, length, pos, initial, picin, validn } = fieldAttributes;
  
  // Get base properties from protection attribute
  const baseProps = mapBmsProtectionToInput(attrb, theme);
  
  // Apply color mapping
  const colorValue = mapBmsColorToTheme(color, theme);
  
  // Apply highlight styles
  const highlightStyles = {
    UNDERLINE: {
      textDecoration: 'underline',
      textDecorationColor: colorValue,
      textUnderlineOffset: '2px'
    },
    BLINK: {
      animation: 'blink 1.5s infinite',
      '@media (prefers-reduced-motion: reduce)': {
        animation: 'none',
        fontWeight: 'bold'
      }
    },
    REVERSE: {
      backgroundColor: colorValue,
      color: theme.palette.getContrastText(colorValue),
      borderRadius: '2px',
      padding: '2px 4px'
    },
    OFF: {
      textDecoration: 'none',
      backgroundColor: 'transparent'
    }
  };
  
  // Build comprehensive Material-UI properties
  const muiProps = {
    ...baseProps,
    
    // Length restriction
    inputProps: {
      ...baseProps.inputProps,
      maxLength: length,
      'data-bms-length': length
    },
    
    // Position for CSS Grid layout
    sx: {
      ...baseProps.sx,
      
      // Grid positioning from BMS POS attribute
      gridRow: pos.row,
      gridColumn: pos.column,
      
      // Color application
      '& .MuiInputBase-input': {
        ...baseProps.sx?.['& .MuiInputBase-input'],
        color: colorValue,
        ...highlightStyles[hilight] || highlightStyles.OFF
      },
      
      // Label styling
      '& .MuiInputLabel-root': {
        color: colorValue,
        '&.Mui-focused': {
          color: colorValue
        }
      },
      
      // Border styling
      '& .MuiOutlinedInput-root': {
        '& fieldset': {
          borderColor: theme.palette.divider
        },
        '&:hover fieldset': {
          borderColor: colorValue
        },
        '&.Mui-focused fieldset': {
          borderColor: colorValue
        }
      }
    },
    
    // Initial value
    defaultValue: initial || '',
    
    // Input pattern validation
    ...(picin && {
      inputProps: {
        ...baseProps.inputProps,
        pattern: picin,
        'data-bms-picin': picin
      }
    }),
    
    // Validation requirements
    ...(validn && validn.includes('MUSTFILL') && {
      required: true,
      helperText: 'This field is required'
    }),
    
    // Tab order based on field position
    tabIndex: pos.row * 100 + pos.column,
    
    // Accessibility attributes
    'aria-label': `Field at row ${pos.row}, column ${pos.column}`,
    'data-bms-attribute': attrb,
    'data-bms-color': color,
    'data-bms-highlight': hilight
  };
  
  return muiProps;
};

/**
 * Main React hook for BMS field attributes processing
 * 
 * Provides optimized callback functions for converting BMS field attributes
 * to Material-UI component properties while maintaining exact mainframe behavior.
 * 
 * @returns {Object} Hook functions for BMS attribute processing
 */
const useBmsFieldAttributes = () => {
  const theme = useTheme();
  
  // Memoized callback for color mapping to prevent unnecessary re-renders
  const mapColorToTheme = useCallback((bmsColor) => {
    return mapBmsColorToTheme(bmsColor, theme);
  }, [theme]);
  
  // Memoized callback for protection mapping
  const mapProtectionToInput = useCallback((bmsAttribute) => {
    return mapBmsProtectionToInput(bmsAttribute, theme);
  }, [theme]);
  
  // Memoized callback for complete attribute mapping
  const mapAttributesToProps = useCallback((fieldAttributes) => {
    return mapBmsAttributesToMuiProps(fieldAttributes, theme);
  }, [theme]);
  
  // Utility function to validate BMS field attributes
  const validateBmsAttributes = useCallback((fieldAttributes) => {
    const { attrb, color, hilight, length, pos } = fieldAttributes;
    
    // Validate required properties
    if (!attrb || !color || !hilight || !length || !pos) {
      throw new Error('Invalid BMS field attributes: missing required properties');
    }
    
    // Validate position boundaries (24x80 terminal)
    if (pos.row < 1 || pos.row > 24 || pos.column < 1 || pos.column > 80) {
      throw new Error(`Invalid field position: row ${pos.row}, column ${pos.column}`);
    }
    
    // Validate length
    if (length < 1 || length > 78) {
      throw new Error(`Invalid field length: ${length}`);
    }
    
    return true;
  }, []);
  
  // Field sequencing utility for tab order management
  const getFieldSequence = useCallback((fieldAttributes) => {
    const { pos, attrb } = fieldAttributes;
    
    // Skip fields with ASKIP attribute in tab order
    if (attrb === 'ASKIP') {
      return -1;
    }
    
    // Calculate tab sequence based on position (left-to-right, top-to-bottom)
    return pos.row * 100 + pos.column;
  }, []);
  
  // Utility to extract BMS validation rules
  const extractValidationRules = useCallback((fieldAttributes) => {
    const { attrb, validn, picin, length } = fieldAttributes;
    
    const rules = [];
    
    // Required field validation
    if (validn && validn.includes('MUSTFILL')) {
      rules.push({
        type: 'required',
        message: 'This field is required'
      });
    }
    
    // Length validation
    if (length) {
      rules.push({
        type: 'maxLength',
        value: length,
        message: `Maximum length is ${length} characters`
      });
    }
    
    // Numeric validation
    if (attrb === 'NUM') {
      rules.push({
        type: 'pattern',
        value: /^[0-9]*$/,
        message: 'Only numeric characters are allowed'
      });
    }
    
    // Picture input validation
    if (picin) {
      rules.push({
        type: 'pattern',
        value: new RegExp(picin),
        message: 'Invalid format'
      });
    }
    
    return rules;
  }, []);
  
  return {
    // Primary mapping functions
    mapColorToTheme,
    mapProtectionToInput,
    mapAttributesToProps,
    
    // Utility functions
    validateBmsAttributes,
    getFieldSequence,
    extractValidationRules,
    
    // Direct access to mapping functions for external use
    mapBmsColorToTheme: mapColorToTheme,
    mapBmsProtectionToInput: mapProtectionToInput,
    mapBmsAttributesToMuiProps: mapAttributesToProps,
    
    // Theme reference
    theme
  };
};

export default useBmsFieldAttributes;