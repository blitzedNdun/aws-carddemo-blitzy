/**
 * BMS Mapping Utilities for CardDemo Application
 * 
 * This module provides comprehensive utility functions that translate BMS (Basic Mapping Support)
 * mapset definitions and attribute bytes to Material-UI component properties. These utilities
 * ensure pixel-perfect preservation of original screen layouts and field behaviors when
 * transforming 3270 terminal screens to modern React components.
 * 
 * The module handles all BMS attributes including:
 * - Field protection and input control (ASKIP, UNPROT, PROT)
 * - Display attributes (NORM, BRT, DRK)
 * - Color mappings (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL)
 * - Highlight attributes (UNDERLINE, BLINK, REVERSE, OFF)
 * - Field positioning (POS coordinates to CSS Grid)
 * - Validation mappings (PICIN patterns to Yup schemas)
 * 
 * Each function maintains exact functional equivalence with original BMS behavior
 * while providing modern React component integration capabilities.
 * 
 * @version 1.0.0
 * @since 2024
 */

import React from 'react';
import { Grid } from '@mui/material';
import { Theme } from '@mui/material/styles';
import * as yup from 'yup';

// Import dependency constants and types
import { ThemeConstants } from '../constants/ThemeConstants';
import { FieldConstants } from '../constants/FieldConstants';
import { ValidationConstants } from '../constants/ValidationConstants';
import { 
  FormFieldAttributes, 
  BaseScreenData, 
  BmsFieldDefinition, 
  MaterialUIProps 
} from '../types/CommonTypes';

// Destructure commonly used constants for cleaner code
const { BMS_COLORS, DISPLAY_ATTRIBUTES, HIGHLIGHT_STYLES } = ThemeConstants;
const { BMS_ATTRIBUTES, FIELD_POSITIONING, ATTRIBUTE_MAPPINGS } = FieldConstants;
const { PICIN_PATTERNS, VALIDATION_RULES, INPUT_MASKS } = ValidationConstants;

/**
 * Converts BMS field attributes to Material-UI TextField properties
 * 
 * This function translates BMS DFHMDF ATTRB values to their Material-UI equivalents,
 * preserving exact field behavior from the original 3270 terminal interface.
 * 
 * BMS Attribute Mappings:
 * - ASKIP: Read-only field with auto-skip behavior (user cannot focus)
 * - UNPROT: Editable input field allowing user modification
 * - PROT: Protected field (read-only but focusable)
 * - NUM: Numeric-only input with right alignment
 * - IC: Auto-focus field on screen load
 * - FSET: Field change detection enabled
 * - NORM/BRT/DRK: Display intensity mapping to font weights
 * 
 * @param {string|string[]} bmsAttributes - BMS attribute byte(s) from DFHMDF ATTRB
 * @returns {Object} Material-UI TextField component properties
 * 
 * @example
 * // BMS: DFHMDF ATTRB=(FSET,IC,NORM,UNPROT)
 * const props = convertBmsAttributesToMuiProps(['FSET', 'IC', 'NORM', 'UNPROT']);
 * // Returns: { variant: 'outlined', autoFocus: true, InputProps: {...} }
 */
export const convertBmsAttributesToMuiProps = (bmsAttributes) => {
  // Normalize input to array for consistent processing
  const attributes = Array.isArray(bmsAttributes) ? bmsAttributes : [bmsAttributes];
  
  // Initialize Material-UI props object
  let muiProps = {
    variant: 'outlined',
    size: 'small',
    fullWidth: false,
    InputProps: {},
    inputProps: {},
    sx: {}
  };

  // Process each BMS attribute
  attributes.forEach(attr => {
    switch (attr?.toUpperCase()) {
      case BMS_ATTRIBUTES.ASKIP:
        // Auto-skip fields become read-only with skip behavior
        muiProps = {
          ...muiProps,
          ...ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.props,
          InputProps: {
            ...muiProps.InputProps,
            ...ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.props.InputProps,
            readOnly: true
          },
          tabIndex: -1, // Prevent keyboard focus (auto-skip behavior)
          sx: {
            ...muiProps.sx,
            ...ATTRIBUTE_MAPPINGS.ASKIP_TO_READONLY.styles,
            '& .MuiInputBase-input': {
              cursor: 'default',
              backgroundColor: '#f5f5f5'
            }
          }
        };
        break;

      case BMS_ATTRIBUTES.UNPROT:
        // Unprotected fields become editable inputs
        muiProps = {
          ...muiProps,
          ...ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.props,
          InputProps: {
            ...muiProps.InputProps,
            readOnly: false
          },
          sx: {
            ...muiProps.sx,
            ...ATTRIBUTE_MAPPINGS.UNPROT_TO_EDITABLE.styles,
            '& .MuiInputBase-input': {
              backgroundColor: '#ffffff'
            }
          }
        };
        break;

      case BMS_ATTRIBUTES.PROT:
        // Protected fields become disabled/read-only but focusable
        muiProps = {
          ...muiProps,
          ...ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.props,
          InputProps: {
            ...muiProps.InputProps,
            readOnly: true
          },
          sx: {
            ...muiProps.sx,
            ...ATTRIBUTE_MAPPINGS.PROT_TO_DISABLED.styles,
            '& .MuiInputBase-input': {
              backgroundColor: '#e0e0e0',
              color: '#616161'
            }
          }
        };
        break;

      case BMS_ATTRIBUTES.NUM:
        // Numeric fields get number input type and right alignment
        muiProps = {
          ...muiProps,
          ...ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.props,
          inputProps: {
            ...muiProps.inputProps,
            pattern: '[0-9]*',
            inputMode: 'numeric'
          },
          sx: {
            ...muiProps.sx,
            ...ATTRIBUTE_MAPPINGS.NUM_TO_NUMERIC.styles,
            '& .MuiInputBase-input': {
              textAlign: 'right'
            }
          }
        };
        break;

      case BMS_ATTRIBUTES.IC:
        // Initial cursor fields get autofocus
        muiProps = {
          ...muiProps,
          ...ATTRIBUTE_MAPPINGS.IC_TO_AUTOFOCUS.props,
          autoFocus: true
        };
        break;

      case BMS_ATTRIBUTES.FSET:
        // Field set indicator enables change detection
        muiProps.inputProps = {
          ...muiProps.inputProps,
          'data-fset': true, // Flag for change detection
          onChange: (event) => {
            // Mark field as modified for change detection
            event.target.setAttribute('data-modified', 'true');
          }
        };
        break;

      // Display intensity attributes
      case BMS_ATTRIBUTES.NORM:
        muiProps.sx = {
          ...muiProps.sx,
          '& .MuiInputBase-input': {
            ...DISPLAY_ATTRIBUTES.NORM
          }
        };
        break;

      case BMS_ATTRIBUTES.BRT:
        muiProps.sx = {
          ...muiProps.sx,
          '& .MuiInputBase-input': {
            ...DISPLAY_ATTRIBUTES.BRT
          }
        };
        break;

      case BMS_ATTRIBUTES.DRK:
        muiProps.sx = {
          ...muiProps.sx,
          '& .MuiInputBase-input': {
            ...DISPLAY_ATTRIBUTES.DRK,
            // Special handling for password-like fields
            WebkitTextSecurity: attributes.includes('DRK') ? 'disc' : 'none'
          }
        };
        break;

      default:
        // Handle any unrecognized attributes gracefully
        console.warn(`Unrecognized BMS attribute: ${attr}`);
        break;
    }
  });

  return muiProps;
};

/**
 * Maps BMS color attributes to Material-UI theme palette colors
 * 
 * Converts BMS COLOR attribute values (GREEN, RED, YELLOW, BLUE, TURQUOISE, NEUTRAL)
 * to Material-UI theme-compatible color objects while maintaining semantic meaning
 * and ensuring WCAG 2.1 AA accessibility compliance.
 * 
 * Original BMS color meanings are preserved:
 * - GREEN: Input fields and data entry areas
 * - RED: Error messages and critical alerts  
 * - YELLOW: Titles and important notices
 * - BLUE: System information and headers
 * - TURQUOISE: Field labels and user prompts
 * - NEUTRAL: General text and section titles
 * 
 * @param {string} bmsColor - BMS color name from DFHMDF COLOR attribute
 * @param {Object} theme - Material-UI theme object for color context
 * @returns {Object} Color configuration with main, light, dark, and contrastText
 * 
 * @example
 * // BMS: DFHMDF COLOR=GREEN
 * const colorConfig = mapBmsColorsToTheme('GREEN', theme);
 * // Returns: { main: '#2e7d32', light: '#60ad5e', dark: '#1b5e20', contrastText: '#ffffff' }
 */
export const mapBmsColorsToTheme = (bmsColor, theme) => {
  // Validate input parameters
  if (!bmsColor || typeof bmsColor !== 'string') {
    console.warn('Invalid BMS color provided, defaulting to NEUTRAL');
    return BMS_COLORS.NEUTRAL;
  }

  // Convert to uppercase for case-insensitive matching
  const colorKey = bmsColor.toUpperCase();

  // Return corresponding Material-UI color configuration
  switch (colorKey) {
    case 'GREEN':
      return {
        ...BMS_COLORS.GREEN,
        // Add theme-aware variations if theme is provided
        ...(theme && {
          main: theme.palette.success?.main || BMS_COLORS.GREEN.main,
          light: theme.palette.success?.light || BMS_COLORS.GREEN.light,
          dark: theme.palette.success?.dark || BMS_COLORS.GREEN.dark
        })
      };

    case 'RED':
      return {
        ...BMS_COLORS.RED,
        ...(theme && {
          main: theme.palette.error?.main || BMS_COLORS.RED.main,
          light: theme.palette.error?.light || BMS_COLORS.RED.light,
          dark: theme.palette.error?.dark || BMS_COLORS.RED.dark
        })
      };

    case 'YELLOW':
    case 'AMBER': // Handle both YELLOW and AMBER variants
      return {
        ...BMS_COLORS.YELLOW,
        ...(theme && {
          main: theme.palette.warning?.main || BMS_COLORS.YELLOW.main,
          light: theme.palette.warning?.light || BMS_COLORS.YELLOW.light,
          dark: theme.palette.warning?.dark || BMS_COLORS.YELLOW.dark
        })
      };

    case 'BLUE':
      return {
        ...BMS_COLORS.BLUE,
        ...(theme && {
          main: theme.palette.info?.main || BMS_COLORS.BLUE.main,
          light: theme.palette.info?.light || BMS_COLORS.BLUE.light,
          dark: theme.palette.info?.dark || BMS_COLORS.BLUE.dark
        })
      };

    case 'TURQUOISE':
    case 'TEAL': // Handle both TURQUOISE and TEAL variants
      return BMS_COLORS.TURQUOISE;

    case 'NEUTRAL':
    case 'DEFAULT':
    case 'GRAY':
    case 'GREY':
      return {
        ...BMS_COLORS.NEUTRAL,
        ...(theme && {
          main: theme.palette.text?.primary || BMS_COLORS.NEUTRAL.main,
          light: theme.palette.text?.secondary || BMS_COLORS.NEUTRAL.light,
          dark: theme.palette.text?.primary || BMS_COLORS.NEUTRAL.dark
        })
      };

    default:
      console.warn(`Unrecognized BMS color: ${bmsColor}, defaulting to NEUTRAL`);
      return BMS_COLORS.NEUTRAL;
  }
};

/**
 * Converts BMS POS coordinates to CSS Grid Layout properties
 * 
 * Transforms BMS POS=(row,column) positioning to Material-UI Grid component
 * properties while preserving exact field relationships and maintaining
 * responsive design principles.
 * 
 * BMS uses 1-based coordinates on a 24x80 character grid:
 * - Rows: 1-24 (terminal height)
 * - Columns: 1-80 (terminal width)
 * 
 * The conversion maintains proportional positioning while enabling
 * responsive behavior for modern screen sizes.
 * 
 * @param {Object} position - BMS position object with row and column
 * @param {number} position.row - Row position (1-24)
 * @param {number} position.col - Column position (1-80)
 * @param {number} fieldLength - Field length for span calculation
 * @returns {Object} Material-UI Grid component properties
 * 
 * @example
 * // BMS: DFHMDF POS=(19,43), LENGTH=8
 * const gridProps = convertPosToGridProperties({row: 19, col: 43}, 8);
 * // Returns: { xs: 12, sm: 6, md: 3, style: { gridArea: '19 / 43 / 20 / 51' } }
 */
export const convertPosToGridProperties = (position, fieldLength = 1) => {
  // Validate input parameters
  if (!position || typeof position.row !== 'number' || typeof position.col !== 'number') {
    console.warn('Invalid position provided for grid conversion');
    return { xs: 12 }; // Default to full width
  }

  const { row, col } = position;
  
  // Validate BMS coordinate ranges
  if (row < 1 || row > 24 || col < 1 || col > 80) {
    console.warn(`Invalid BMS coordinates: row=${row}, col=${col}`);
    return { xs: 12 };
  }

  // Calculate field span (minimum 1, maximum remaining width)
  const calculatedSpan = Math.max(1, Math.min(fieldLength, 80 - col + 1));
  
  // Calculate responsive grid sizes based on field position and length
  const responsiveBreakpoints = {
    xs: 12, // Full width on extra small screens
    sm: calculateSmallScreenColumns(col, calculatedSpan),
    md: calculateMediumScreenColumns(col, calculatedSpan),
    lg: calculateLargeScreenColumns(col, calculatedSpan),
    xl: calculateExtraLargeScreenColumns(col, calculatedSpan)
  };

  // Create CSS Grid area definition for precise positioning
  const gridAreaStyle = {
    gridArea: `${row} / ${col} / ${row + 1} / ${col + calculatedSpan}`
  };

  // Add positioning utilities from FieldConstants
  const positionUtils = {
    ...FIELD_POSITIONING.LAYOUT_UTILS.getFieldPosition(row, col),
    ...FIELD_POSITIONING.LAYOUT_UTILS.getFieldDimensions(calculatedSpan, 1)
  };

  return {
    ...responsiveBreakpoints,
    style: {
      ...gridAreaStyle,
      // Add responsive positioning hints
      '--bms-row': row,
      '--bms-col': col,
      '--bms-length': calculatedSpan,
      // Maintain aspect ratio for consistent layout
      aspectRatio: `${calculatedSpan} / 1`
    },
    // Add position metadata for debugging and testing
    'data-bms-position': `${row},${col}`,
    'data-bms-length': calculatedSpan,
    // Include position utilities for advanced usage
    positionUtils
  };
};

/**
 * Maps BMS LENGTH and INITIAL values to React component properties
 * 
 * Converts BMS DFHMDF LENGTH and INITIAL attributes to React component
 * default values and validation constraints, ensuring exact character
 * count matching and proper field initialization.
 * 
 * @param {number} length - BMS LENGTH attribute value
 * @param {string} initialValue - BMS INITIAL attribute value
 * @returns {Object} React component properties with validation constraints
 * 
 * @example
 * // BMS: DFHMDF LENGTH=8, INITIAL='________'
 * const props = mapLengthAndInitialValues(8, '________');
 * // Returns: { maxLength: 8, defaultValue: '________', inputProps: {...} }
 */
export const mapLengthAndInitialValues = (length, initialValue) => {
  // Validate length parameter
  const validatedLength = typeof length === 'number' && length > 0 ? length : 1;
  
  // Process initial value, handling BMS-specific formatting
  let processedInitialValue = '';
  if (initialValue) {
    // Handle common BMS initial value patterns
    if (initialValue === 'mm/dd/yy') {
      processedInitialValue = new Date().toLocaleDateString('en-US', {
        month: '2-digit',
        day: '2-digit',
        year: '2-digit'
      });
    } else if (initialValue === 'hh:mm:ss' || initialValue === 'Ahh:mm:ss') {
      processedInitialValue = new Date().toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } else if (initialValue.includes('_')) {
      // Underscore placeholders become empty string for better UX
      processedInitialValue = '';
    } else {
      processedInitialValue = initialValue;
    }
  }

  // Build component properties
  const componentProps = {
    // Field length constraints
    maxLength: validatedLength,
    defaultValue: processedInitialValue,
    
    // Input properties for HTML input elements
    inputProps: {
      maxLength: validatedLength,
      'data-original-length': validatedLength,
      'data-original-initial': initialValue
    },

    // Validation properties
    validation: {
      maxLength: {
        value: validatedLength,
        message: `Field cannot exceed ${validatedLength} characters`
      }
    },

    // Character counter for long fields
    ...(validatedLength > 20 && {
      helperText: `Maximum ${validatedLength} characters`,
      FormHelperTextProps: {
        sx: { textAlign: 'right' }
      }
    }),

    // Special handling for specific field lengths from FieldConstants
    ...(FIELD_LENGTHS.LENGTH_CONSTRAINTS.USER_ID === validatedLength && {
      autoComplete: 'username',
      'data-field-type': 'user-id'
    }),
    ...(FIELD_LENGTHS.LENGTH_CONSTRAINTS.PASSWORD === validatedLength && {
      type: 'password',
      autoComplete: 'current-password',
      'data-field-type': 'password'
    }),
    ...(FIELD_LENGTHS.LENGTH_CONSTRAINTS.ACCOUNT_NUMBER === validatedLength && {
      'data-field-type': 'account-number',
      inputMode: 'numeric'
    }),
    ...(FIELD_LENGTHS.LENGTH_CONSTRAINTS.CARD_NUMBER === validatedLength && {
      'data-field-type': 'card-number',
      inputMode: 'numeric'
    })
  };

  return componentProps;
};

/**
 * Converts BMS HILIGHT attributes to Material-UI styling properties
 * 
 * Transforms BMS HILIGHT attribute values (UNDERLINE, BLINK, REVERSE, OFF)
 * to Material-UI sx styling objects for modern web presentation while
 * maintaining visual emphasis and accessibility compliance.
 * 
 * Note: BLINK is converted to static emphasis for accessibility compliance.
 * 
 * @param {string} highlightType - BMS HILIGHT attribute value
 * @returns {Object} Material-UI sx styling object
 * 
 * @example
 * // BMS: DFHMDF HILIGHT=UNDERLINE
 * const styling = convertHighlightToStyling('UNDERLINE');
 * // Returns: { textDecoration: 'underline', textUnderlineOffset: '2px', ... }
 */
export const convertHighlightToStyling = (highlightType) => {
  // Validate input parameter
  if (!highlightType || typeof highlightType !== 'string') {
    return HIGHLIGHT_STYLES.OFF;
  }

  // Convert to uppercase for case-insensitive matching
  const highlight = highlightType.toUpperCase();

  // Return corresponding Material-UI styling
  switch (highlight) {
    case 'UNDERLINE':
      return {
        ...HIGHLIGHT_STYLES.UNDERLINE,
        // Add additional accessibility support
        '&:focus': {
          textDecorationThickness: '2px',
          textDecorationColor: 'currentColor'
        }
      };

    case 'BLINK':
      // Convert blink to static emphasis for accessibility
      return {
        ...HIGHLIGHT_STYLES.BLINK,
        // Add subtle animation alternative that respects prefers-reduced-motion
        '@media (prefers-reduced-motion: no-preference)': {
          animation: 'none' // Still no blinking for accessibility
        },
        // Enhanced visual emphasis without animation
        boxShadow: '0 0 0 1px rgba(255, 152, 0, 0.5)',
        '&:focus': {
          boxShadow: '0 0 0 2px rgba(255, 152, 0, 0.8)'
        }
      };

    case 'REVERSE':
      return {
        ...HIGHLIGHT_STYLES.REVERSE,
        // Ensure proper contrast in reverse mode
        '&:focus': {
          outline: '2px solid white',
          outlineOffset: '2px'
        }
      };

    case 'OFF':
    case 'NONE':
    default:
      return HIGHLIGHT_STYLES.OFF;
  }
};

/**
 * Converts complete BMS DFHMDF definition to React component properties
 * 
 * This comprehensive function processes a complete BMS field definition
 * (DFHMDF) and returns a fully configured React component properties object
 * suitable for Material-UI TextField components.
 * 
 * Handles all BMS attributes in combination:
 * - ATTRB: Field behavior and protection
 * - COLOR: Display colors and theming
 * - HILIGHT: Visual emphasis
 * - LENGTH: Field size constraints
 * - POS: Screen positioning
 * - INITIAL: Default values
 * - PICIN: Input formatting
 * - VALIDN: Validation rules
 * 
 * @param {Object} bmsFieldDef - Complete BMS field definition object
 * @returns {Object} Complete React component properties
 * 
 * @example
 * // BMS: USERID DFHMDF ATTRB=(FSET,IC,NORM,UNPROT), COLOR=GREEN, 
 * //              HILIGHT=OFF, LENGTH=8, POS=(19,43)
 * const fieldDef = {
 *   name: 'USERID',
 *   attrb: ['FSET', 'IC', 'NORM', 'UNPROT'],
 *   color: 'GREEN',
 *   hilight: 'OFF',
 *   length: 8,
 *   pos: { row: 19, col: 43 }
 * };
 * const reactProps = convertDfhmdfToReactProps(fieldDef);
 */
export const convertDfhmdfToReactProps = (bmsFieldDef) => {
  // Validate input parameter
  if (!bmsFieldDef || typeof bmsFieldDef !== 'object') {
    console.warn('Invalid BMS field definition provided');
    return {};
  }

  // Extract BMS field properties with defaults
  const {
    name = '',
    attrb = [],
    color = 'NEUTRAL',
    hilight = 'OFF',
    length = 1,
    pos = { row: 1, col: 1 },
    initial = '',
    picin = '',
    validn = ''
  } = bmsFieldDef;

  // Generate base component properties from individual converters
  const attributeProps = convertBmsAttributesToMuiProps(attrb);
  const colorConfig = mapBmsColorsToTheme(color);
  const gridProps = convertPosToGridProperties(pos, length);
  const lengthProps = mapLengthAndInitialValues(length, initial);
  const highlightStyles = convertHighlightToStyling(hilight);

  // Build comprehensive component properties
  const componentProps = {
    // Basic field properties
    name,
    id: name.toLowerCase(),
    
    // Merge all attribute-based properties
    ...attributeProps,
    ...lengthProps,
    
    // Apply color theming
    sx: {
      ...attributeProps.sx,
      ...highlightStyles,
      // Color application
      '& .MuiInputBase-input': {
        ...attributeProps.sx?.['& .MuiInputBase-input'],
        color: colorConfig.main
      },
      '& .MuiOutlinedInput-root': {
        '& fieldset': {
          borderColor: colorConfig.light
        },
        '&:hover fieldset': {
          borderColor: colorConfig.main
        },
        '&.Mui-focused fieldset': {
          borderColor: colorConfig.dark
        }
      },
      '& .MuiInputLabel-root': {
        color: colorConfig.main,
        '&.Mui-focused': {
          color: colorConfig.dark
        }
      }
    },

    // Grid positioning properties
    gridProps,
    
    // Validation properties (if PICIN or VALIDN specified)
    ...(picin && {
      validation: {
        ...lengthProps.validation,
        pattern: convertPicinToValidation(picin)
      }
    }),
    
    // Required field handling (if VALIDN includes MUSTFILL)
    ...(validn?.includes('MUSTFILL') && {
      required: true,
      validation: {
        ...lengthProps.validation,
        required: {
          value: true,
          message: VALIDATION_RULES.MUSTFILL.errorMessage
        }
      }
    }),

    // Field metadata for debugging and testing
    'data-bms-field': name,
    'data-bms-color': color,
    'data-bms-attributes': Array.isArray(attrb) ? attrb.join(',') : attrb,
    'data-bms-highlight': hilight,
    'data-bms-validation': validn
  };

  return componentProps;
};

/**
 * Creates a comprehensive BMS field mapping utility
 * 
 * Returns a configured field mapper function that can process multiple
 * BMS field definitions and return a complete screen mapping suitable
 * for React component rendering.
 * 
 * The mapper handles:
 * - Field grouping and organization
 * - Cross-field relationships
 * - Screen-level properties
 * - Responsive layout configuration
 * 
 * @param {Object} screenConfig - Screen-level configuration options
 * @returns {Function} Field mapper function
 * 
 * @example
 * const fieldMapper = createBmsFieldMapper({
 *   screenSize: { width: 80, height: 24 },
 *   responsive: true,
 *   theme: muiTheme
 * });
 * 
 * const screenMapping = fieldMapper(bmsFieldDefinitions);
 */
export const createBmsFieldMapper = (screenConfig = {}) => {
  // Set default screen configuration
  const config = {
    screenSize: { width: 80, height: 24 },
    responsive: true,
    theme: null,
    preservePositioning: true,
    generateGridTemplate: true,
    ...screenConfig
  };

  // Return configured field mapper function
  return (bmsFieldDefinitions) => {
    // Validate input
    if (!Array.isArray(bmsFieldDefinitions)) {
      console.warn('Field definitions must be an array');
      return {};
    }

    // Process each field definition
    const mappedFields = {};
    const gridAreas = [];
    const fieldGroups = {
      header: [],
      content: [],
      footer: [],
      inputs: [],
      displays: [],
      errors: []
    };

    bmsFieldDefinitions.forEach(fieldDef => {
      try {
        // Convert field definition to React props
        const reactProps = convertDfhmdfToReactProps(fieldDef);
        
        // Store in mapped fields
        mappedFields[fieldDef.name] = reactProps;
        
        // Categorize field for layout purposes
        categorizeField(fieldDef, fieldGroups);
        
        // Collect grid area information
        if (fieldDef.pos && config.generateGridTemplate) {
          gridAreas.push({
            name: fieldDef.name,
            area: `${fieldDef.pos.row} / ${fieldDef.pos.col} / ${fieldDef.pos.row + 1} / ${fieldDef.pos.col + (fieldDef.length || 1)}`
          });
        }
      } catch (error) {
        console.error(`Error processing field ${fieldDef.name}:`, error);
      }
    });

    // Generate CSS Grid template if requested
    const gridTemplate = config.generateGridTemplate ? {
      display: 'grid',
      gridTemplateColumns: `repeat(${config.screenSize.width}, 1fr)`,
      gridTemplateRows: `repeat(${config.screenSize.height}, min-content)`,
      gap: '4px',
      padding: '8px',
      fontFamily: 'monospace' // Preserve character-based layout
    } : {};

    // Return complete screen mapping
    return {
      fields: mappedFields,
      fieldGroups,
      gridAreas,
      gridTemplate,
      screenConfig: config,
      // Utility functions for field access
      getField: (name) => mappedFields[name],
      getFieldsByGroup: (group) => fieldGroups[group] || [],
      getAllInputFields: () => fieldGroups.inputs,
      getAllDisplayFields: () => fieldGroups.displays,
      // Layout utilities
      generateGridStyle: () => gridTemplate,
      getFieldPosition: (name) => {
        const field = mappedFields[name];
        return field?.gridProps;
      }
    };
  };
};

/**
 * Converts BMS PICIN patterns to Yup validation schemas
 * 
 * Transforms BMS PICIN picture format definitions to JavaScript validation
 * schemas compatible with React Hook Form and Yup validation library.
 * 
 * Supported PICIN patterns:
 * - '99999999999': Numeric patterns (account numbers, etc.)
 * - 'X(n)': Alphanumeric patterns
 * - '9(n)': Pure numeric patterns
 * - 'A(n)': Alphabetic patterns
 * 
 * @param {string} picinPattern - BMS PICIN pattern string
 * @returns {Object} Yup validation schema configuration
 * 
 * @example
 * // BMS: DFHMDF PICIN='99999999999'
 * const validation = convertPicinToValidation('99999999999');
 * // Returns: { schema: yup.string().matches(/^[0-9]{11}$/), pattern: /^[0-9]{11}$/ }
 */
export const convertPicinToValidation = (picinPattern) => {
  // Validate input parameter
  if (!picinPattern || typeof picinPattern !== 'string') {
    console.warn('Invalid PICIN pattern provided');
    return { schema: yup.string(), pattern: null };
  }

  // Trim and normalize pattern
  const pattern = picinPattern.trim().toUpperCase();
  
  // Handle specific known patterns from ValidationConstants
  if (PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER.source === `^${pattern.replace(/9/g, '[0-9]')}$`) {
    return {
      schema: yup.string()
        .matches(PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER, 'Account number must be exactly 11 digits')
        .required('Account number is required'),
      pattern: PICIN_PATTERNS.NUMERIC_PATTERNS.ACCOUNT_NUMBER,
      message: 'Account number must be exactly 11 digits'
    };
  }

  // Pattern parsing and conversion
  let regexPattern = '';
  let yupSchema = yup.string();
  let validationMessage = 'Invalid format';
  
  // Count consecutive 9s for numeric patterns
  if (/^9+$/.test(pattern)) {
    const digitCount = pattern.length;
    regexPattern = `^[0-9]{${digitCount}}$`;
    yupSchema = yup.string()
      .matches(new RegExp(regexPattern), `Must be exactly ${digitCount} digits`)
      .required('This field is required');
    validationMessage = `Must be exactly ${digitCount} digits`;
  }
  // Handle X patterns for alphanumeric
  else if (pattern.startsWith('X(') && pattern.endsWith(')')) {
    const length = parseInt(pattern.slice(2, -1));
    if (!isNaN(length)) {
      regexPattern = `^[A-Za-z0-9]{1,${length}}$`;
      yupSchema = yup.string()
        .max(length, `Cannot exceed ${length} characters`)
        .matches(new RegExp(regexPattern), 'Only letters and numbers allowed');
      validationMessage = `Only letters and numbers allowed, maximum ${length} characters`;
    }
  }
  // Handle A patterns for alphabetic
  else if (pattern.startsWith('A(') && pattern.endsWith(')')) {
    const length = parseInt(pattern.slice(2, -1));
    if (!isNaN(length)) {
      regexPattern = `^[A-Za-z]{1,${length}}$`;
      yupSchema = yup.string()
        .max(length, `Cannot exceed ${length} characters`)
        .matches(new RegExp(regexPattern), 'Only letters allowed');
      validationMessage = `Only letters allowed, maximum ${length} characters`;
    }
  }
  // Handle mixed patterns (simplified approach)
  else {
    // Convert BMS picture format to regex
    let convertedPattern = pattern
      .replace(/9/g, '[0-9]')
      .replace(/X/g, '[A-Za-z0-9]')
      .replace(/A/g, '[A-Za-z]');
    
    regexPattern = `^${convertedPattern}$`;
    yupSchema = yup.string().matches(new RegExp(regexPattern), validationMessage);
  }

  return {
    schema: yupSchema,
    pattern: new RegExp(regexPattern),
    message: validationMessage,
    originalPicin: picinPattern
  };
};

// ============================================================================
// UTILITY HELPER FUNCTIONS
// ============================================================================

/**
 * Calculates responsive grid columns for small screens (sm breakpoint)
 */
const calculateSmallScreenColumns = (col, span) => {
  // On small screens, use larger grid units for better touch targets
  const smCols = Math.max(3, Math.min(12, Math.ceil(span / 2)));
  return smCols;
};

/**
 * Calculates responsive grid columns for medium screens (md breakpoint)  
 */
const calculateMediumScreenColumns = (col, span) => {
  // Medium screens can accommodate more precise positioning
  const mdCols = Math.max(2, Math.min(8, Math.ceil(span / 3)));
  return mdCols;
};

/**
 * Calculates responsive grid columns for large screens (lg breakpoint)
 */
const calculateLargeScreenColumns = (col, span) => {
  // Large screens maintain close to original proportions
  const lgCols = Math.max(1, Math.min(6, Math.ceil(span / 4)));
  return lgCols;
};

/**
 * Calculates responsive grid columns for extra large screens (xl breakpoint)
 */
const calculateExtraLargeScreenColumns = (col, span) => {
  // Extra large screens can maintain exact BMS proportions
  const xlCols = Math.max(1, Math.min(4, Math.ceil(span / 6)));
  return xlCols;
};

/**
 * Categorizes fields into logical groups for layout and processing
 */
const categorizeField = (fieldDef, fieldGroups) => {
  const { name, pos, attrb } = fieldDef;
  
  // Header fields (typically rows 1-3)
  if (pos.row <= 3) {
    fieldGroups.header.push(name);
  }
  // Footer fields (typically rows 22-24)
  else if (pos.row >= 22) {
    fieldGroups.footer.push(name);
  }
  // Content fields (rows 4-21)
  else {
    fieldGroups.content.push(name);
  }
  
  // Categorize by functionality
  const attributes = Array.isArray(attrb) ? attrb : [attrb];
  
  if (attributes.includes('UNPROT')) {
    fieldGroups.inputs.push(name);
  } else {
    fieldGroups.displays.push(name);
  }
  
  // Special case for error message fields
  if (name.toLowerCase().includes('err') || name.toLowerCase() === 'errmsg') {
    fieldGroups.errors.push(name);
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