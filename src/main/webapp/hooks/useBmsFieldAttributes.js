/**
 * CardDemo - BMS Field Attributes Hook
 * 
 * Custom React hook implementing BMS mapset attribute byte translation to Material-UI 
 * component properties, providing field protection, color mapping, and input validation 
 * attributes that preserve exact mainframe field behavior while enabling modern 
 * Material-UI styling and interaction patterns.
 * 
 * Transforms legacy BMS attributes (ASKIP, UNPROT, PROT, NUM, IC, BRT, NORM, FSET) 
 * into corresponding Material-UI TextField properties and HTML input attributes 
 * maintaining identical field behavior, protection levels, and visual styling.
 * 
 * Based on analysis of 18 BMS mapsets including COSGN00, COACTVW, COACTUP, COCRDLI, 
 * COCRDUP and their corresponding copybook definitions preserving exact field 
 * sequencing, tab order, and validation patterns.
 * 
 * @author Blitzy agent
 * @version 1.0.0
 * @since 2024
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

import { useCallback } from 'react';
import { useTheme } from '@mui/material/styles';

// Internal imports for BMS attribute and theme mappings
import { 
  DISPLAY_ATTRIBUTES, 
  BRT, 
  NORM, 
  DRK, 
  INTENSITY_MAPPING 
} from '../constants/ThemeConstants';

import { 
  ATTRIBUTE_MAPPINGS,
  ASKIP_TO_READONLY,
  UNPROT_TO_EDITABLE,
  PROT_TO_DISABLED,
  NUM_TO_NUMERIC,
  IC_TO_AUTOFOCUS,
  MUSTFILL_TO_REQUIRED
} from '../constants/FieldConstants';

import { FormFieldAttributes } from '../types/CommonTypes';

/**
 * Custom React hook for translating BMS field attributes to Material-UI component properties.
 * 
 * Provides comprehensive attribute mapping functionality that preserves exact BMS field 
 * behavior including protection levels, color schemes, input validation, field 
 * sequencing, and visual styling while enabling modern responsive UI patterns.
 * 
 * Implements the core BMS to Material-UI translation requirements specified in 
 * Section 7.4.2 including:
 * - Field protection mapping (ASKIP→readonly, UNPROT→editable, PROT→disabled)
 * - Color theme translation (GREEN→text.primary, RED→error.main, etc.)
 * - Input validation attributes (NUM→numeric, MUSTFILL→required)
 * - Visual intensity mapping (BRT→bold, NORM→normal, DRK→dimmed)
 * - Tab order and focus management (IC→autoFocus)
 * 
 * @returns {Object} Hook interface containing attribute mapping functions
 * @returns {Function} returns.mapBmsAttributesToMuiProps - Maps BMS attributes to Material-UI properties
 * @returns {Function} returns.mapBmsColorToTheme - Maps BMS colors to theme colors
 * @returns {Function} returns.mapBmsProtectionToInput - Maps BMS protection to input properties
 * @returns {Function} returns.processFieldAttributes - Main processing function for complete field setup
 */
const useBmsFieldAttributes = () => {
  const theme = useTheme();

  /**
   * Maps BMS field protection attributes to Material-UI component properties.
   * 
   * Implements exact BMS protection behavior through Material-UI TextField properties:
   * - ASKIP: Field is automatically skipped by cursor, displayed as read-only
   * - UNPROT: Field allows user input and modification
   * - PROT: Field is protected from user modification, displayed as disabled
   * 
   * @param {Array<string>} attributes - Array of BMS attribute strings
   * @returns {Object} Material-UI component properties for field protection
   */
  const mapBmsProtectionToInput = useCallback((attributes) => {
    const attrs = Array.isArray(attributes) ? attributes : [];
    let inputProps = {};
    let componentProps = {};
    let styleProps = {};

    // Process protection attributes in order of precedence
    if (attrs.includes('ASKIP')) {
      // ASKIP - Auto-skip protected field (read-only display with tab skip)
      const askipMapping = ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY;
      inputProps = { ...inputProps, ...askipMapping.inputProps };
      styleProps = { ...styleProps, ...askipMapping.sx };
    } else if (attrs.includes('PROT')) {
      // PROT - Protected field (disabled, prevents user modification)
      const protMapping = ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED;
      componentProps.disabled = protMapping.disabled;
      styleProps = { ...styleProps, ...protMapping.sx };
    } else if (attrs.includes('UNPROT')) {
      // UNPROT - Unprotected field (allows user input and editing)
      const unprotMapping = ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE;
      inputProps = { ...inputProps, ...unprotMapping.inputProps };
      styleProps = { ...styleProps, ...unprotMapping.sx };
    }

    // Handle special input type attributes
    if (attrs.includes('NUM')) {
      // NUM - Numeric field (numeric keyboard on mobile, right-aligned)
      const numMapping = ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC;
      inputProps = { ...inputProps, ...numMapping.inputProps };
      styleProps = { 
        ...styleProps, 
        ...numMapping.sx,
        '& .MuiInputBase-input': {
          ...styleProps['& .MuiInputBase-input'],
          ...numMapping.sx['& .MuiInputBase-input']
        }
      };
    }

    // Handle cursor positioning
    if (attrs.includes('IC')) {
      // IC - Initial cursor (field receives focus when screen loads)
      const icMapping = ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS;
      componentProps.autoFocus = icMapping.autoFocus;
      styleProps = { 
        ...styleProps, 
        ...icMapping.sx,
        '& .MuiInputBase-input': {
          ...styleProps['& .MuiInputBase-input'],
          ...icMapping.sx['& .MuiInputBase-input']
        }
      };
    }

    return {
      inputProps,
      ...componentProps,
      sx: styleProps
    };
  }, []);

  /**
   * Maps BMS color attributes to Material-UI theme-consistent colors.
   * 
   * Implements the exact color mapping specified in Section 7.4.2:
   * - GREEN → theme.palette.text.primary (normal data display)
   * - RED → theme.palette.error.main (error messages and validation failures)
   * - YELLOW → theme.palette.warning.main (highlighted fields and warnings)
   * - BLUE → theme.palette.info.main (informational messages and help text)
   * - TURQUOISE → theme.palette.info.light (field labels and instruction text)
   * - NEUTRAL → theme.palette.text.secondary (general text and descriptions)
   * 
   * @param {string} bmsColor - BMS color name (BLUE, RED, YELLOW, GREEN, TURQUOISE, NEUTRAL)
   * @returns {Object} Material-UI color properties with theme integration
   */
  const mapBmsColorToTheme = useCallback((bmsColor) => {
    const colorMapping = ATTRIBUTE_MAPPINGS.COLOR_MAPPINGS;
    let colorValue;

    switch (bmsColor) {
      case 'GREEN':
        colorValue = theme.palette.text.primary;
        break;
      case 'RED':
        colorValue = theme.palette.error.main;
        break;
      case 'YELLOW':
        colorValue = theme.palette.warning.main;
        break;
      case 'BLUE':
        colorValue = theme.palette.info.main;
        break;
      case 'TURQUOISE':
        colorValue = theme.palette.info.light;
        break;
      case 'NEUTRAL':
      default:
        colorValue = theme.palette.text.secondary;
        break;
    }

    return {
      sx: {
        '& .MuiInputBase-input': {
          color: colorValue
        },
        '& .MuiInputLabel-root': {
          color: colorValue
        },
        '& .MuiOutlinedInput-notchedOutline': {
          borderColor: colorValue
        }
      }
    };
  }, [theme]);

  /**
   * Maps complete BMS field attributes to comprehensive Material-UI component properties.
   * 
   * Processes all BMS attribute combinations to create a complete set of Material-UI 
   * TextField properties that preserve exact field behavior including:
   * - Protection levels and input capabilities
   * - Visual styling and intensity (BRT, NORM, DRK)
   * - Validation requirements (MUSTFILL, format patterns)
   * - Change tracking (FSET for modification detection)
   * - Tab order and field sequencing
   * 
   * @param {FormFieldAttributes} fieldAttributes - Complete BMS field attribute object
   * @returns {Object} Comprehensive Material-UI TextField properties
   */
  const mapBmsAttributesToMuiProps = useCallback((fieldAttributes) => {
    if (!fieldAttributes || !fieldAttributes.attrb) {
      return {};
    }

    const { attrb, color, hilight, length, pos, initial, picin, validn } = fieldAttributes;
    
    // Get base protection and input properties
    const protectionProps = mapBmsProtectionToInput(attrb);
    
    // Get color styling properties
    const colorProps = mapBmsColorToTheme(color);
    
    // Process display intensity attributes
    let intensityStyle = {};
    if (attrb.includes('BRT')) {
      // BRT - Bright intensity for emphasis (bold weight)
      const brtAttrs = DISPLAY_ATTRIBUTES.BRT;
      intensityStyle = {
        fontWeight: brtAttrs.fontWeight,
        opacity: brtAttrs.opacity,
        fontSize: brtAttrs.fontSize,
        letterSpacing: brtAttrs.letterSpacing
      };
    } else if (attrb.includes('DRK')) {
      // DRK - Dark intensity for sensitive fields (passwords)
      const drkAttrs = DISPLAY_ATTRIBUTES.DRK;
      intensityStyle = {
        fontWeight: drkAttrs.fontWeight,
        opacity: drkAttrs.opacity,
        fontSize: drkAttrs.fontSize,
        letterSpacing: drkAttrs.letterSpacing
      };
    } else {
      // NORM - Normal intensity for standard display
      const normAttrs = DISPLAY_ATTRIBUTES.NORM;
      intensityStyle = {
        fontWeight: normAttrs.fontWeight,
        opacity: normAttrs.opacity,
        fontSize: normAttrs.fontSize,
        letterSpacing: normAttrs.letterSpacing
      };
    }

    // Handle validation requirements
    let validationProps = {};
    if (validn && validn.some(rule => rule.type === 'MUSTFILL' || rule.required)) {
      const mustfillMapping = ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED;
      validationProps.required = mustfillMapping.required;
    }

    // Handle highlight attributes
    let highlightStyle = {};
    switch (hilight) {
      case 'UNDERLINE':
        highlightStyle = {
          borderBottom: '2px solid',
          borderBottomColor: 'primary.main'
        };
        break;
      case 'REVERSE':
        highlightStyle = {
          backgroundColor: 'primary.main',
          color: 'primary.contrastText'
        };
        break;
      case 'BLINK':
        highlightStyle = {
          animation: 'pulse 1.5s ease-in-out infinite alternate'
        };
        break;
      case 'OFF':
      default:
        // No highlighting
        break;
    }

    // Handle change tracking
    let changeTrackingProps = {};
    if (attrb.includes('FSET')) {
      // FSET - Field set attribute for change detection
      changeTrackingProps.onChange = true;
    }

    // Combine all styling properties
    const combinedStyle = {
      ...protectionProps.sx,
      ...colorProps.sx,
      '& .MuiInputBase-input': {
        ...protectionProps.sx?.['& .MuiInputBase-input'],
        ...colorProps.sx?.['& .MuiInputBase-input'],
        ...intensityStyle
      },
      ...highlightStyle
    };

    // Handle field length constraints
    const lengthProps = length ? {
      inputProps: {
        ...protectionProps.inputProps,
        maxLength: length
      }
    } : { inputProps: protectionProps.inputProps };

    // Handle initial values
    const initialProps = initial ? {
      defaultValue: initial,
      placeholder: initial
    } : {};

    // Handle input format patterns
    const formatProps = picin ? {
      inputProps: {
        ...lengthProps.inputProps,
        pattern: picin
      }
    } : lengthProps;

    // Return comprehensive Material-UI properties
    return {
      ...validationProps,
      ...changeTrackingProps,
      ...initialProps,
      ...formatProps,
      autoFocus: protectionProps.autoFocus,
      disabled: protectionProps.disabled,
      sx: combinedStyle
    };
  }, [mapBmsProtectionToInput, mapBmsColorToTheme]);

  /**
   * Main processing function for complete field attribute setup.
   * 
   * Orchestrates the complete transformation of BMS field attributes into 
   * Material-UI component properties including field positioning, tab order 
   * preservation, and responsive layout considerations.
   * 
   * Implements field sequencing requirements from Section 7.4.2 ensuring 
   * original BMS field positioning and navigation flow is maintained in 
   * the React component hierarchy.
   * 
   * @param {FormFieldAttributes} fieldAttributes - Complete BMS field definition
   * @param {Object} options - Additional processing options
   * @param {boolean} options.preserveTabOrder - Whether to maintain original tab sequence
   * @param {boolean} options.enableResponsive - Whether to apply responsive layout adjustments
   * @returns {Object} Complete Material-UI field configuration with positioning
   */
  const processFieldAttributes = useCallback((fieldAttributes, options = {}) => {
    const { preserveTabOrder = true, enableResponsive = true } = options;
    
    if (!fieldAttributes) {
      return {};
    }

    // Get base Material-UI properties
    const muiProps = mapBmsAttributesToMuiProps(fieldAttributes);
    
    // Handle field positioning for tab order preservation
    let positioningProps = {};
    if (preserveTabOrder && fieldAttributes.pos) {
      const { row, column } = fieldAttributes.pos;
      
      // Calculate tab index based on BMS position (row-major order)
      // This preserves the original 24x80 terminal field navigation sequence
      const tabIndex = fieldAttributes.attrb.includes('ASKIP') ? -1 : 
                      ((row - 1) * 80) + (column - 1);
      
      positioningProps = {
        inputProps: {
          ...muiProps.inputProps,
          tabIndex
        }
      };

      // Add responsive grid positioning if enabled
      if (enableResponsive) {
        const gridRow = Math.ceil(row / 3); // Convert 24 rows to 8 responsive rows
        const gridCol = Math.min(Math.ceil(column / 6.67), 12); // Convert 80 cols to 12-column grid
        
        positioningProps.sx = {
          ...muiProps.sx,
          gridRow,
          gridColumn: `span ${Math.min(Math.ceil(fieldAttributes.length / 6.67), 12)}`
        };
      }
    }

    // Handle validation metadata
    let validationMeta = {};
    if (fieldAttributes.validn) {
      validationMeta = {
        'data-validation-rules': JSON.stringify(fieldAttributes.validn),
        'data-field-length': fieldAttributes.length,
        'data-field-format': fieldAttributes.picin || ''
      };
    }

    // Return complete field configuration
    return {
      ...muiProps,
      ...positioningProps,
      inputProps: {
        ...muiProps.inputProps,
        ...positioningProps.inputProps,
        ...validationMeta
      },
      // Add metadata for debugging and testing
      'data-bms-attributes': fieldAttributes.attrb?.join(',') || '',
      'data-bms-color': fieldAttributes.color || '',
      'data-bms-position': fieldAttributes.pos ? 
        `${fieldAttributes.pos.row},${fieldAttributes.pos.column}` : ''
    };
  }, [mapBmsAttributesToMuiProps]);

  // Return hook interface with all mapping functions
  return {
    mapBmsAttributesToMuiProps,
    mapBmsColorToTheme,
    mapBmsProtectionToInput,
    processFieldAttributes
  };
};

// Export individual mapping functions for modular usage
export { useBmsFieldAttributes as default };
export { useBmsFieldAttributes };

/**
 * Standalone function for mapping BMS attributes to Material-UI properties.
 * 
 * Provides direct access to attribute mapping without hook context for use in 
 * utility functions, testing, or non-component contexts.
 * 
 * @param {FormFieldAttributes} fieldAttributes - BMS field attributes
 * @param {Object} theme - Material-UI theme object
 * @returns {Object} Material-UI component properties
 */
export const mapBmsAttributesToMuiProps = (fieldAttributes, theme) => {
  // Implementation mirrors the hook version but operates on passed theme
  if (!fieldAttributes || !fieldAttributes.attrb) {
    return {};
  }

  const { attrb, color, hilight, length, initial, picin, validn } = fieldAttributes;
  
  // Apply protection mappings
  let props = {};
  if (attrb.includes('ASKIP')) {
    props = { ...props, ...ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY };
  } else if (attrb.includes('PROT')) {
    props = { ...props, ...ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED };
  } else if (attrb.includes('UNPROT')) {
    props = { ...props, ...ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE };
  }

  // Apply input type mappings
  if (attrb.includes('NUM')) {
    props = { ...props, ...ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC };
  }
  if (attrb.includes('IC')) {
    props = { ...props, ...ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS };
  }

  // Apply validation mappings
  if (validn && validn.some(rule => rule.type === 'MUSTFILL' || rule.required)) {
    props = { ...props, ...ATTRIBUTE_MAPPINGS.MUSTFILL_TO_REQUIRED };
  }

  return props;
};

/**
 * Standalone function for mapping BMS colors to theme colors.
 * 
 * @param {string} bmsColor - BMS color name
 * @param {Object} theme - Material-UI theme object
 * @returns {Object} Color properties for Material-UI styling
 */
export const mapBmsColorToTheme = (bmsColor, theme) => {
  const colorMapping = {
    'GREEN': theme.palette.text.primary,
    'RED': theme.palette.error.main,
    'YELLOW': theme.palette.warning.main,
    'BLUE': theme.palette.info.main,
    'TURQUOISE': theme.palette.info.light,
    'NEUTRAL': theme.palette.text.secondary
  };

  return {
    sx: {
      '& .MuiInputBase-input': {
        color: colorMapping[bmsColor] || theme.palette.text.secondary
      }
    }
  };
};

/**
 * Standalone function for mapping BMS protection to input properties.
 * 
 * @param {Array<string>} attributes - Array of BMS attribute strings
 * @returns {Object} Input protection properties for Material-UI components
 */
export const mapBmsProtectionToInput = (attributes) => {
  const attrs = Array.isArray(attributes) ? attributes : [];
  
  if (attrs.includes('ASKIP')) {
    return ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY;
  } else if (attrs.includes('PROT')) {
    return ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED;
  } else if (attrs.includes('UNPROT')) {
    return ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE;
  }
  
  return {};
};