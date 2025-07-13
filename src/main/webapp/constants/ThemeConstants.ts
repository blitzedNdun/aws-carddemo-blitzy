/**
 * Theme Constants for CardDemo Application
 * 
 * Maps BMS (Basic Mapping Support) color and display attributes to Material-UI theme configuration
 * while maintaining WCAG 2.1 AA accessibility compliance and preserving original visual hierarchy
 * and semantic meaning from the mainframe application.
 * 
 * This file provides centralized theming constants for consistent styling across all React components,
 * ensuring that the transformation from mainframe BMS screens to modern web UI maintains the
 * original user experience while enhancing accessibility.
 */

import { PaletteColor } from '@mui/material';
import { Theme } from '@mui/material/styles';

/**
 * BMS Color Mappings to Material-UI Theme Colors
 * 
 * Maps original BMS color attributes to accessible Material-UI palette colors
 * while preserving semantic meaning and visual hierarchy from the mainframe application.
 * 
 * Original BMS Color Usage:
 * - BLUE: System information, labels, headers, data fields
 * - YELLOW: Page titles, function key descriptions, navigation
 * - GREEN: Input fields, user-editable data, form controls
 * - RED: Error messages, alerts, validation failures
 * - TURQUOISE: Prompts, instructions, field labels
 * - NEUTRAL: Section headers, descriptions, informational content
 */
export const BMS_COLORS = {
  /**
   * GREEN: Input fields and user-editable data
   * Maps to Material-UI success color with enhanced contrast for accessibility
   */
  GREEN: {
    main: '#2e7d32',      // Dark green for high contrast
    light: '#66bb6a',     // Light green for hover states
    dark: '#1b5e20',      // Very dark green for active states
    contrastText: '#ffffff'
  } as PaletteColor,

  /**
   * RED: Error messages and validation failures
   * Maps to Material-UI error color with WCAG AA compliant contrast
   */
  RED: {
    main: '#d32f2f',      // Red with sufficient contrast ratio
    light: '#f44336',     // Light red for secondary error states
    dark: '#c62828',      // Dark red for critical errors
    contrastText: '#ffffff'
  } as PaletteColor,

  /**
   * YELLOW: Page titles and navigation elements
   * Maps to Material-UI warning color with enhanced readability
   */
  YELLOW: {
    main: '#f57c00',      // Orange-yellow for better contrast than pure yellow
    light: '#ffb74d',     // Light orange for backgrounds
    dark: '#ef6c00',      // Dark orange for active states
    contrastText: '#000000'
  } as PaletteColor,

  /**
   * BLUE: System information and data display
   * Maps to Material-UI primary color with accessibility enhancements
   */
  BLUE: {
    main: '#1976d2',      // Material-UI blue with good contrast
    light: '#42a5f5',     // Light blue for secondary elements
    dark: '#1565c0',      // Dark blue for emphasis
    contrastText: '#ffffff'
  } as PaletteColor,

  /**
   * TURQUOISE: Prompts and field labels
   * Maps to Material-UI info color with accessibility compliance
   */
  TURQUOISE: {
    main: '#0288d1',      // Turquoise-blue with sufficient contrast
    light: '#29b6f6',     // Light turquoise for backgrounds
    dark: '#0277bd',      // Dark turquoise for active states
    contrastText: '#ffffff'
  } as PaletteColor,

  /**
   * NEUTRAL: Section headers and informational content
   * Maps to Material-UI secondary color with enhanced readability
   */
  NEUTRAL: {
    main: '#616161',      // Medium gray for informational text
    light: '#9e9e9e',     // Light gray for subtle elements
    dark: '#424242',      // Dark gray for emphasis
    contrastText: '#ffffff'
  } as PaletteColor
} as const;

/**
 * BMS Display Attribute Mappings to Material-UI Typography and Styling
 * 
 * Converts BMS display attributes (BRT, NORM, DRK) to Material-UI typography
 * and styling constants while maintaining original visual hierarchy.
 * 
 * Original BMS Display Attributes:
 * - BRT (Bright): High intensity display for emphasis and error messages
 * - NORM (Normal): Standard display intensity for regular content
 * - DRK (Dark): Low intensity display for secondary content and password fields
 */
export const DISPLAY_ATTRIBUTES = {
  /**
   * BRT (Bright): High intensity display for emphasis
   * Maps to Material-UI typography with bold weight and enhanced contrast
   */
  BRT: {
    fontWeight: 700,
    opacity: 1.0,
    color: 'inherit',
    textShadow: 'none'
  },

  /**
   * NORM (Normal): Standard display intensity
   * Maps to Material-UI typography with regular weight and standard contrast
   */
  NORM: {
    fontWeight: 400,
    opacity: 0.87,
    color: 'inherit',
    textShadow: 'none'
  },

  /**
   * DRK (Dark): Low intensity display for secondary content
   * Maps to Material-UI typography with reduced opacity and lighter weight
   */
  DRK: {
    fontWeight: 300,
    opacity: 0.6,
    color: 'inherit',
    textShadow: 'none'
  },

  /**
   * Intensity mapping for programmatic access to display attributes
   * Provides easy lookup for converting BMS attributes to Material-UI styles
   */
  INTENSITY_MAPPING: {
    'BRT': 'bright',
    'NORM': 'normal',
    'DRK': 'dark'
  }
} as const;

/**
 * BMS Highlight Style Mappings to Material-UI Emphasis and Elevation
 * 
 * Maps BMS HILIGHT attributes to Material-UI elevation and emphasis styles
 * while maintaining accessibility and visual consistency.
 * 
 * Original BMS Highlight Attributes:
 * - UNDERLINE: Underlined text for input fields and interactive elements
 * - BLINK: Blinking text for attention (converted to pulse animation)
 * - REVERSE: Reverse video display for selection states
 * - OFF: No special highlighting (default state)
 */
export const HIGHLIGHT_STYLES = {
  /**
   * UNDERLINE: Input fields and interactive elements
   * Maps to Material-UI text decoration with focus indicators
   */
  UNDERLINE: {
    textDecoration: 'underline',
    textDecorationColor: 'currentColor',
    textDecorationThickness: '1px',
    textUnderlineOffset: '2px'
  },

  /**
   * BLINK: Attention-grabbing elements (converted to pulse animation)
   * Maps to Material-UI animation with accessibility considerations
   */
  BLINK: {
    animation: 'pulse 1.5s infinite',
    animationTimingFunction: 'ease-in-out',
    // Respects user's preference for reduced motion
    '@media (prefers-reduced-motion: reduce)': {
      animation: 'none',
      fontWeight: 'bold'
    }
  },

  /**
   * REVERSE: Selection states and active elements
   * Maps to Material-UI background and text color inversion
   */
  REVERSE: {
    backgroundColor: 'currentColor',
    color: 'background.paper',
    padding: '2px 4px',
    borderRadius: '2px'
  },

  /**
   * OFF: Default state with no special highlighting
   * Maps to Material-UI default text styling
   */
  OFF: {
    textDecoration: 'none',
    backgroundColor: 'transparent',
    animation: 'none'
  }
} as const;

/**
 * Spacing Constants Derived from BMS Screen Positioning
 * 
 * Defines spacing constants based on BMS screen positioning to maintain
 * visual consistency in responsive layout while adapting to modern web standards.
 * 
 * Original BMS screens used 80-column by 24-row character grid positioning.
 * These constants translate that grid system to flexible spacing units.
 */
export const SPACING_CONSTANTS = {
  /**
   * FIELD_SPACING: Spacing between form fields and labels
   * Based on BMS field positioning with modern accessibility enhancements
   */
  FIELD_SPACING: {
    horizontal: 8,        // 8px horizontal spacing between fields
    vertical: 16,         // 16px vertical spacing between fields
    labelToField: 4,      // 4px spacing between label and field
    fieldToHelp: 2        // 2px spacing between field and help text
  },

  /**
   * SECTION_SPACING: Spacing between major screen sections
   * Based on BMS screen layout with enhanced visual hierarchy
   */
  SECTION_SPACING: {
    small: 16,            // 16px for minor section breaks
    medium: 24,           // 24px for standard section separation
    large: 32,            // 32px for major section breaks
    xlarge: 48            // 48px for page-level section separation
  },

  /**
   * GRID_SPACING: Grid system spacing for responsive layouts
   * Maintains BMS grid alignment while supporting modern responsive design
   */
  GRID_SPACING: {
    container: 24,        // 24px container padding
    item: 8,              // 8px grid item spacing
    row: 16,              // 16px row spacing
    column: 12            // 12px column spacing
  },

  /**
   * COMPONENT_MARGINS: Standardized margins for consistent component spacing
   * Ensures uniform spacing throughout the application
   */
  COMPONENT_MARGINS: {
    small: 8,             // 8px for compact layouts
    medium: 16,           // 16px for standard layouts
    large: 24,            // 24px for spacious layouts
    xlarge: 32            // 32px for major separations
  }
} as const;

/**
 * Accessibility Constants for WCAG 2.1 AA Compliance
 * 
 * Provides accessibility constants ensuring WCAG 2.1 AA compliance
 * while maintaining the original visual hierarchy and semantic meaning
 * from the BMS screens.
 */
export const ACCESSIBILITY_CONSTANTS = {
  /**
   * CONTRAST_RATIOS: Minimum contrast ratios for accessibility compliance
   * Ensures all color combinations meet WCAG 2.1 AA standards
   */
  CONTRAST_RATIOS: {
    normal: 4.5,          // WCAG AA minimum for normal text
    large: 3.0,           // WCAG AA minimum for large text (18pt+ or 14pt+ bold)
    enhanced: 7.0,        // WCAG AAA standard for enhanced accessibility
    nonText: 3.0          // WCAG AA minimum for non-text elements
  },

  /**
   * FOCUS_INDICATORS: Focus indicator styles for keyboard navigation
   * Provides clear focus indicators for all interactive elements
   */
  FOCUS_INDICATORS: {
    outline: '2px solid',
    outlineColor: '#1976d2',
    outlineOffset: '2px',
    borderRadius: '2px',
    boxShadow: '0 0 0 2px rgba(25, 118, 210, 0.2)'
  },

  /**
   * WCAG_COMPLIANT_COLORS: Color palette ensuring accessibility compliance
   * All colors meet WCAG 2.1 AA contrast requirements against their backgrounds
   */
  WCAG_COMPLIANT_COLORS: {
    text: {
      primary: 'rgba(0, 0, 0, 0.87)',
      secondary: 'rgba(0, 0, 0, 0.60)',
      disabled: 'rgba(0, 0, 0, 0.38)'
    },
    background: {
      paper: '#ffffff',
      default: '#fafafa',
      input: '#ffffff'
    },
    error: {
      main: '#d32f2f',
      contrastText: '#ffffff'
    },
    success: {
      main: '#2e7d32',
      contrastText: '#ffffff'
    },
    warning: {
      main: '#f57c00',
      contrastText: '#000000'
    },
    info: {
      main: '#0288d1',
      contrastText: '#ffffff'
    }
  },

  /**
   * HIGH_CONTRAST_MODE: Enhanced contrast for accessibility needs
   * Provides high contrast alternatives for users with visual impairments
   */
  HIGH_CONTRAST_MODE: {
    enabled: false,       // Can be toggled based on user preference
    colors: {
      text: '#000000',
      background: '#ffffff',
      primary: '#0000ff',
      secondary: '#008000',
      error: '#ff0000',
      success: '#008000',
      warning: '#ff8c00',
      info: '#0000ff'
    },
    // Media query for users who prefer high contrast
    mediaQuery: '(prefers-contrast: high)'
  }
} as const;

/**
 * Type definitions for enhanced TypeScript support
 * Provides strong typing for theme constants usage throughout the application
 */
export type BmsColorKey = keyof typeof BMS_COLORS;
export type DisplayAttributeKey = keyof typeof DISPLAY_ATTRIBUTES;
export type HighlightStyleKey = keyof typeof HIGHLIGHT_STYLES;
export type SpacingConstantKey = keyof typeof SPACING_CONSTANTS;
export type AccessibilityConstantKey = keyof typeof ACCESSIBILITY_CONSTANTS;

/**
 * Utility function to get BMS color with fallback
 * Provides safe access to BMS colors with TypeScript support
 */
export const getBmsColor = (colorKey: BmsColorKey, shade: keyof PaletteColor = 'main'): string => {
  const color = BMS_COLORS[colorKey];
  return color[shade] || color.main;
};

/**
 * Utility function to get display attribute styles
 * Provides safe access to display attributes with TypeScript support
 */
export const getDisplayAttribute = (attribute: DisplayAttributeKey): object => {
  return DISPLAY_ATTRIBUTES[attribute] || DISPLAY_ATTRIBUTES.NORM;
};

/**
 * Utility function to get highlight styles
 * Provides safe access to highlight styles with TypeScript support
 */
export const getHighlightStyle = (highlight: HighlightStyleKey): object => {
  return HIGHLIGHT_STYLES[highlight] || HIGHLIGHT_STYLES.OFF;
};

/**
 * Utility function to get spacing value
 * Provides safe access to spacing constants with TypeScript support
 */
export const getSpacing = (category: SpacingConstantKey, size: string): number => {
  const spacingCategory = SPACING_CONSTANTS[category];
  return (spacingCategory as any)[size] || 8;
};

/**
 * CSS-in-JS keyframes for animations
 * Defines animations used in highlight styles
 */
export const animationKeyframes = `
  @keyframes pulse {
    0% {
      opacity: 1;
    }
    50% {
      opacity: 0.5;
    }
    100% {
      opacity: 1;
    }
  }
`;