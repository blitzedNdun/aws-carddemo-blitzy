/*
 * CardDemo - Material-UI Theme Constants
 * 
 * Maps BMS color and display attributes to accessible Material-UI theme colors
 * while preserving original mainframe visual hierarchy and semantic meaning.
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

import { PaletteColor } from '@mui/material';
import { Theme } from '@mui/material/styles';

/**
 * BMS Color Mappings to Material-UI Theme Colors
 * 
 * Maps original BMS color attributes to accessible Material-UI palette colors
 * ensuring WCAG 2.1 AA compliance while maintaining semantic meaning:
 * 
 * - BLUE: Header information, borders, system labels
 * - YELLOW: Titles, function key instructions, warnings
 * - NEUTRAL: Descriptive text, informational content
 * - TURQUOISE: Instruction text, field labels, prompts
 * - GREEN: Input fields, data entry areas, success states
 * - RED: Error messages, alerts, critical information
 */
export const BMS_COLORS = {
  /**
   * BLUE - Header information, borders, navigation elements
   * Maps to Material-UI primary color for consistency
   */
  BLUE: {
    main: '#1976d2',        // Material-UI primary blue
    light: '#42a5f5',       // Lighter variant for hover states
    dark: '#1565c0',        // Darker variant for active states
    contrastText: '#ffffff'  // High contrast white text
  } as PaletteColor,

  /**
   * YELLOW - Titles, function key instructions, warnings
   * Maps to Material-UI warning color with enhanced accessibility
   */
  YELLOW: {
    main: '#ed6c02',        // Material-UI warning orange (more accessible than pure yellow)
    light: '#ff9800',       // Lighter variant for backgrounds
    dark: '#e65100',        // Darker variant for emphasis
    contrastText: '#ffffff'  // High contrast white text
  } as PaletteColor,

  /**
   * NEUTRAL - Descriptive text, informational content
   * Maps to Material-UI text secondary for readable content
   */
  NEUTRAL: {
    main: '#757575',        // Material-UI grey 600
    light: '#9e9e9e',       // Lighter variant for less emphasis
    dark: '#424242',        // Darker variant for better contrast
    contrastText: '#ffffff'  // High contrast white text
  } as PaletteColor,

  /**
   * TURQUOISE - Instruction text, field labels, prompts
   * Maps to Material-UI info color for instructional content
   */
  TURQUOISE: {
    main: '#0288d1',        // Material-UI info blue
    light: '#03a9f4',       // Lighter variant for backgrounds
    dark: '#01579b',        // Darker variant for emphasis
    contrastText: '#ffffff'  // High contrast white text
  } as PaletteColor,

  /**
   * GREEN - Input fields, data entry areas, success states
   * Maps to Material-UI success color for positive actions
   */
  GREEN: {
    main: '#2e7d32',        // Material-UI success green
    light: '#4caf50',       // Lighter variant for backgrounds
    dark: '#1b5e20',        // Darker variant for emphasis
    contrastText: '#ffffff'  // High contrast white text
  } as PaletteColor,

  /**
   * RED - Error messages, alerts, critical information
   * Maps to Material-UI error color for negative feedback
   */
  RED: {
    main: '#d32f2f',        // Material-UI error red
    light: '#f44336',       // Lighter variant for backgrounds
    dark: '#c62828',        // Darker variant for emphasis
    contrastText: '#ffffff'  // High contrast white text
  } as PaletteColor
} as const;

/**
 * BMS Display Attributes to Material-UI Typography and Styling
 * 
 * Converts BMS display attributes (BRT, NORM, DRK) to Material-UI
 * typography variants and styling constants with proper intensity mapping
 */
export const DISPLAY_ATTRIBUTES = {
  /**
   * BRT - Bright display for emphasis (error messages, warnings)
   * Maps to bold typography with increased emphasis
   */
  BRT: {
    fontWeight: 700,        // Bold weight for emphasis
    opacity: 1.0,           // Full opacity for maximum visibility
    fontSize: '1rem',       // Standard size with bold weight
    letterSpacing: '0.01em' // Slight letter spacing for readability
  },

  /**
   * NORM - Normal display for standard content
   * Maps to regular typography with standard emphasis
   */
  NORM: {
    fontWeight: 400,        // Regular weight for normal text
    opacity: 0.87,          // Material-UI standard text opacity
    fontSize: '0.875rem',   // Slightly smaller for regular content
    letterSpacing: '0.01em' // Standard letter spacing
  },

  /**
   * DRK - Dark display for sensitive content (passwords)
   * Maps to reduced opacity with special handling for security
   */
  DRK: {
    fontWeight: 400,        // Regular weight to avoid drawing attention
    opacity: 0.6,           // Reduced opacity for discrete display
    fontSize: '0.875rem',   // Standard size for consistency
    letterSpacing: '0.02em' // Slightly increased for masked content
  },

  /**
   * Intensity mapping for dynamic styling based on BMS attributes
   * Provides consistent mapping from BMS display attributes to CSS properties
   */
  INTENSITY_MAPPING: {
    BRT: {
      emphasis: 'high',
      zIndex: 3,
      textShadow: '0 0 1px rgba(0,0,0,0.3)' // Subtle shadow for emphasis
    },
    NORM: {
      emphasis: 'medium',
      zIndex: 2,
      textShadow: 'none'
    },
    DRK: {
      emphasis: 'low',
      zIndex: 1,
      textShadow: 'none'
    }
  }
} as const;

/**
 * BMS Highlight Styles to Material-UI Elevation and Emphasis
 * 
 * Maps BMS HILIGHT attributes to Material-UI styling for visual emphasis
 * while maintaining accessibility and modern design principles
 */
export const HIGHLIGHT_STYLES = {
  /**
   * UNDERLINE - Used for input fields and interactive elements
   * Maps to Material-UI underline styling with focus states
   */
  UNDERLINE: {
    borderBottom: '2px solid',
    borderBottomColor: 'primary.main',
    '&:hover': {
      borderBottomColor: 'primary.dark'
    },
    '&:focus': {
      borderBottomColor: 'primary.main',
      borderBottomWidth: '3px'
    },
    '&:active': {
      borderBottomColor: 'primary.dark',
      borderBottomWidth: '3px'
    }
  },

  /**
   * BLINK - For urgent notifications (converted to pulse animation)
   * Maps to CSS animation for attention-grabbing content
   */
  BLINK: {
    animation: 'pulse 1.5s ease-in-out infinite alternate',
    '@keyframes pulse': {
      from: { opacity: 1 },
      to: { opacity: 0.7 }
    }
  },

  /**
   * REVERSE - For selected or highlighted content
   * Maps to inverted colors with proper contrast
   */
  REVERSE: {
    backgroundColor: 'primary.main',
    color: 'primary.contrastText',
    '&:hover': {
      backgroundColor: 'primary.dark'
    },
    '&:active': {
      backgroundColor: 'primary.dark',
      transform: 'scale(0.98)'
    }
  },

  /**
   * OFF - No highlighting (default state)
   * Maps to standard styling without emphasis
   */
  OFF: {
    border: 'none',
    backgroundColor: 'transparent',
    animation: 'none',
    textDecoration: 'none'
  }
} as const;

/**
 * BMS Screen Positioning to Material-UI Spacing Constants
 * 
 * Derives spacing constants from BMS screen positioning (24x80 character grid)
 * to maintain visual consistency in responsive Material-UI layouts
 */
export const SPACING_CONSTANTS = {
  /**
   * Field spacing based on BMS character positioning
   * Maintains consistent spacing between form elements
   */
  FIELD_SPACING: {
    horizontal: 8,          // 1 character width equivalent
    vertical: 4,            // Half character height for tight spacing
    betweenFields: 12,      // 1.5 character widths for field separation
    labelToField: 4         // Minimal spacing between label and input
  },

  /**
   * Section spacing for logical groupings
   * Based on BMS screen section divisions
   */
  SECTION_SPACING: {
    betweenSections: 24,    // 3 character heights for section separation
    sectionPadding: 16,     // 2 character widths for internal padding
    headerSpacing: 20,      // 2.5 character heights for header separation
    footerSpacing: 16       // 2 character heights for footer separation
  },

  /**
   * Grid spacing for tabular data presentation
   * Maintains BMS column alignment in Material-UI grids
   */
  GRID_SPACING: {
    columnGap: 8,           // 1 character width between columns
    rowGap: 4,              // Minimal row separation
    headerRowGap: 8,        // Slightly more space after headers
    dataRowPadding: 6       // Internal padding for data cells
  },

  /**
   * Component margins for consistent layout
   * Based on BMS screen margins and positioning
   */
  COMPONENT_MARGINS: {
    page: 16,               // Overall page margin
    card: 12,               // Card component margins
    dialog: 8,              // Dialog internal margins
    button: 4               // Button spacing
  }
} as const;

/**
 * Accessibility Constants for WCAG 2.1 AA Compliance
 * 
 * Ensures all color combinations meet accessibility standards
 * while preserving original BMS visual hierarchy
 */
export const ACCESSIBILITY_CONSTANTS = {
  /**
   * Contrast ratios for WCAG 2.1 AA compliance
   * All color combinations must meet these minimum standards
   */
  CONTRAST_RATIOS: {
    normal: 4.5,            // Minimum contrast for normal text
    large: 3.0,             // Minimum contrast for large text (18pt+)
    graphical: 3.0,         // Minimum contrast for graphical elements
    focus: 3.0              // Minimum contrast for focus indicators
  },

  /**
   * Focus indicators for keyboard navigation
   * Provides clear visual feedback for accessibility
   */
  FOCUS_INDICATORS: {
    outlineColor: '#005fcc', // High contrast blue for focus outline
    outlineWidth: '2px',     // Visible outline width
    outlineOffset: '2px',    // Offset for better visibility
    outlineStyle: 'solid'    // Solid outline for clarity
  },

  /**
   * WCAG compliant color palette
   * Pre-validated color combinations for accessibility
   */
  WCAG_COMPLIANT_COLORS: {
    primaryOnLight: '#1976d2',   // Primary blue on light background
    primaryOnDark: '#90caf9',    // Light blue on dark background
    errorOnLight: '#d32f2f',     // Error red on light background
    errorOnDark: '#f44336',      // Light red on dark background
    successOnLight: '#2e7d32',   // Success green on light background
    successOnDark: '#4caf50',    // Light green on dark background
    warningOnLight: '#ed6c02',   // Warning orange on light background
    warningOnDark: '#ff9800'     // Light orange on dark background
  },

  /**
   * High contrast mode support
   * Enhanced colors for users requiring high contrast
   */
  HIGH_CONTRAST_MODE: {
    backgroundColor: '#000000',  // Pure black background
    textColor: '#ffffff',        // Pure white text
    borderColor: '#ffffff',      // White borders for definition
    focusColor: '#ffff00',       // Yellow focus indicators
    linkColor: '#00ffff',        // Cyan for links
    visitedLinkColor: '#ff00ff'  // Magenta for visited links
  }
} as const;

/**
 * Type definitions for theme usage
 * Provides TypeScript support for theme constants
 */
export type BMSColorName = keyof typeof BMS_COLORS;
export type DisplayAttributeName = keyof typeof DISPLAY_ATTRIBUTES;
export type HighlightStyleName = keyof typeof HIGHLIGHT_STYLES;

/**
 * Utility function to get theme-aware color based on BMS color name
 * @param colorName - BMS color name (BLUE, RED, etc.)
 * @param variant - Color variant (main, light, dark)
 * @returns Material-UI color value
 */
export const getBMSColor = (
  colorName: BMSColorName,
  variant: 'main' | 'light' | 'dark' = 'main'
): string => {
  return BMS_COLORS[colorName][variant];
};

/**
 * Utility function to get display attribute styling
 * @param attribute - BMS display attribute (BRT, NORM, DRK)
 * @returns CSS styling object
 */
export const getDisplayAttribute = (
  attribute: DisplayAttributeName
): typeof DISPLAY_ATTRIBUTES[DisplayAttributeName] => {
  return DISPLAY_ATTRIBUTES[attribute];
};

/**
 * Utility function to get highlight style
 * @param highlight - BMS highlight style (UNDERLINE, BLINK, etc.)
 * @returns CSS styling object
 */
export const getHighlightStyle = (
  highlight: HighlightStyleName
): typeof HIGHLIGHT_STYLES[HighlightStyleName] => {
  return HIGHLIGHT_STYLES[highlight];
};