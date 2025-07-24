/**
 * Material-UI Theme Configuration for CardDemo Application
 * 
 * This theme configuration implements BMS-to-React color palette mappings
 * and component style overrides required for preserving 3270 terminal visual
 * behavior while enabling modern responsive design.
 * 
 * Color Mappings:
 * - DFHGREEN → success.main (normal data display, field labels)
 * - DFHRED → error.main (error messages, validation failures)
 * - DFHYELLOW → warning.main (highlighted fields, important notices)
 * - DFHBLUE → info.main (informational messages, help text)
 * 
 * Technical Specification Reference: Section 7.7.2
 * Repository: CardDemo_v1.0-15-g27d6c6f-68
 */

import { createTheme } from '@mui/material';
import { responsiveFontSizes } from '@mui/material/styles';

/**
 * 3270 Terminal Color Palette - Exact hex values for terminal color preservation
 * These colors maintain the visual hierarchy and contrast ratios from the original
 * mainframe application while ensuring WCAG 2.1 AA accessibility compliance.
 */
const terminalColors = {
  // Primary BMS color mappings
  dfhGreen: '#00FF00',      // DFHGREEN - Normal data display, field labels
  dfhRed: '#FF0000',        // DFHRED - Error messages, validation failures  
  dfhYellow: '#FFFF00',     // DFHYELLOW - Highlighted fields, important notices
  dfhBlue: '#0080FF',       // DFHBLUE - Informational messages, help text
  dfhTurquoise: '#00FFFF',  // Terminal turquoise - Instructions, prompts
  dfhNeutral: '#C0C0C0',    // Terminal neutral - Standard display text
  
  // Enhanced color variants for modern UI
  greenDark: '#00CC00',     // Darker green for better contrast
  redDark: '#CC0000',       // Darker red for accessibility
  yellowDark: '#CCCC00',    // Darker yellow for readability
  blueDark: '#0066CC',      // Darker blue for contrast
  
  // Terminal background and text colors
  terminalBlack: '#000000', // Terminal background equivalent
  terminalWhite: '#FFFFFF', // Terminal foreground text
  terminalGray: '#808080',  // Terminal protected field color
};

/**
 * Base Material-UI theme configuration implementing BMS visual behavior
 * Maintains 24x80 character terminal layout structure through responsive breakpoints
 */
const baseTheme = createTheme({
  palette: {
    // Primary brand colors
    primary: {
      main: terminalColors.dfhBlue,
      dark: terminalColors.blueDark,
      light: '#4DA6FF',
      contrastText: terminalColors.terminalWhite,
    },
    
    // Secondary colors for accents
    secondary: {
      main: terminalColors.dfhTurquoise,
      dark: '#00CCCC',
      light: '#66FFFF',
      contrastText: terminalColors.terminalBlack,
    },
    
    // BMS color mappings as specified in technical requirements
    success: {
      main: terminalColors.dfhGreen,        // DFHGREEN mapping
      dark: terminalColors.greenDark,
      light: '#66FF66',
      contrastText: terminalColors.terminalBlack,
    },
    
    error: {
      main: terminalColors.dfhRed,          // DFHRED mapping
      dark: terminalColors.redDark,
      light: '#FF6666',
      contrastText: terminalColors.terminalWhite,
    },
    
    warning: {
      main: terminalColors.dfhYellow,       // DFHYELLOW mapping
      dark: terminalColors.yellowDark,
      light: '#FFFF66',
      contrastText: terminalColors.terminalBlack,
    },
    
    info: {
      main: terminalColors.dfhBlue,         // DFHBLUE mapping
      dark: terminalColors.blueDark,
      light: '#66B3FF',
      contrastText: terminalColors.terminalWhite,
    },
    
    // Text color definitions matching terminal behavior
    text: {
      primary: terminalColors.terminalBlack,
      secondary: terminalColors.terminalGray,
      disabled: '#CCCCCC',
    },
    
    // Background colors preserving terminal contrast
    background: {
      default: terminalColors.terminalWhite,
      paper: '#F5F5F5',
    },
    
    // Divider colors for section separation
    divider: terminalColors.terminalGray,
    
    // Action colors for interactive elements
    action: {
      active: terminalColors.dfhBlue,
      hover: '#E3F2FD',
      selected: '#BBDEFB',
      disabled: '#BDBDBD',
      disabledBackground: '#F5F5F5',
    },
  },
  
  /**
   * Responsive breakpoints preserving 24x80 character terminal structure
   * These breakpoints ensure terminal layout fidelity across devices
   */
  breakpoints: {
    values: {
      xs: 0,      // Mobile portrait - minimum viewport
      sm: 600,    // Mobile landscape and small tablets
      md: 960,    // Tablet and desktop - primary 80-character width
      lg: 1280,   // Desktop - wide terminal emulation
      xl: 1920,   // Large desktop - full terminal grid
    },
  },
  
  /**
   * Typography system maintaining terminal character spacing and readability
   * Monospace fonts ensure precise character alignment for financial data display
   */
  typography: {
    // Font family stack optimized for terminal character display
    fontFamily: [
      '"Courier New"',      // Primary monospace font
      'Monaco',             // macOS monospace fallback
      '"Lucida Console"',   // Windows monospace fallback
      'Consolas',           // Modern Windows monospace
      'monospace',          // System monospace fallback
    ].join(','),
    
    // Base font size matching terminal character dimensions
    fontSize: 14,
    
    // Typography scale preserving terminal text hierarchy
    h1: {
      fontSize: '2rem',
      fontWeight: 700,
      lineHeight: 1.2,
      letterSpacing: '0.05em',
    },
    h2: {
      fontSize: '1.75rem',
      fontWeight: 600,
      lineHeight: 1.3,
      letterSpacing: '0.04em',
    },
    h3: {
      fontSize: '1.5rem',
      fontWeight: 600,
      lineHeight: 1.4,
      letterSpacing: '0.03em',
    },
    h4: {
      fontSize: '1.25rem',
      fontWeight: 500,
      lineHeight: 1.4,
      letterSpacing: '0.02em',
    },
    h5: {
      fontSize: '1.125rem',
      fontWeight: 500,
      lineHeight: 1.5,
      letterSpacing: '0.01em',
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.5,
      letterSpacing: '0.005em',
    },
    
    // Body text for general content
    body1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.5,
      letterSpacing: '0.01em',
    },
    body2: {
      fontSize: '0.875rem',
      fontWeight: 400,
      lineHeight: 1.43,
      letterSpacing: '0.01em',
    },
    
    // Subtitle variants for section headers
    subtitle1: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.75,
      letterSpacing: '0.009em',
    },
    subtitle2: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.57,
      letterSpacing: '0.007em',
    },
  },
  
  /**
   * Spacing function for consistent layout measurements
   * Based on 8px grid system with terminal character width considerations
   */
  spacing: 8,
  
  /**
   * Shape configuration for consistent border radius
   * Minimal radius maintains terminal aesthetic while adding modern polish
   */
  shape: {
    borderRadius: 4,
  },
  
  /**
   * Z-index values for proper layering of UI elements
   * Ensures proper stacking order for modals, tooltips, and navigation
   */
  zIndex: {
    mobileStepper: 1000,
    fab: 1050,
    speedDial: 1050,
    appBar: 1100,
    drawer: 1200,
    modal: 1300,
    snackbar: 1400,
    tooltip: 1500,
  },
  
  /**
   * Transition settings for smooth animations
   * Maintains responsive feel while preserving terminal immediacy
   */
  transitions: {
    easing: {
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
    },
    duration: {
      shortest: 150,
      shorter: 200,
      short: 250,
      standard: 300,
      complex: 375,
      enteringScreen: 225,
      leavingScreen: 195,
    },
  },
  
  /**
   * Component style overrides implementing BMS attribute behavior
   * These overrides enable exact replication of terminal field attributes
   * through Material-UI component properties and styling
   */
  components: {
    // TextField overrides for BMS field attribute replication
    MuiTextField: {
      styleOverrides: {
        root: {
          // Terminal field spacing and alignment
          '& .MuiInputBase-root': {
            backgroundColor: terminalColors.terminalWhite,
            fontFamily: 'monospace',
            fontSize: '14px',
            letterSpacing: '0.05em',
          },
          
          // Protected field styling (ASKIP attribute)
          '&.protected-field .MuiInputBase-root': {
            backgroundColor: '#F0F0F0',
            color: terminalColors.terminalGray,
            cursor: 'default',
          },
          
          // Input field styling (UNPROT attribute)
          '&.input-field .MuiInputBase-root': {
            backgroundColor: terminalColors.terminalWhite,
            color: terminalColors.terminalBlack,
            borderBottom: `2px solid ${terminalColors.dfhGreen}`,
          },
          
          // Error field styling for validation failures
          '&.error-field .MuiInputBase-root': {
            backgroundColor: '#FFE5E5',
            borderColor: terminalColors.dfhRed,
          },
          
          // Highlighted field styling (BRT attribute)
          '&.highlighted-field .MuiInputBase-root': {
            backgroundColor: '#FFFFCC',
            fontWeight: 'bold',
            borderColor: terminalColors.dfhYellow,
          },
        },
      },
    },
    
    // Button overrides for consistent terminal-style actions
    MuiButton: {
      styleOverrides: {
        root: {
          fontFamily: 'monospace',
          fontWeight: 600,
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          minWidth: '120px',
          
          // Function key button styling
          '&.function-key': {
            backgroundColor: terminalColors.dfhBlue,
            color: terminalColors.terminalWhite,
            border: `1px solid ${terminalColors.blueDark}`,
            '&:hover': {
              backgroundColor: terminalColors.blueDark,
            },
          },
          
          // Submit button styling
          '&.submit-button': {
            backgroundColor: terminalColors.dfhGreen,
            color: terminalColors.terminalBlack,
            '&:hover': {
              backgroundColor: terminalColors.greenDark,
            },
          },
          
          // Cancel button styling
          '&.cancel-button': {
            backgroundColor: terminalColors.dfhRed,
            color: terminalColors.terminalWhite,
            '&:hover': {
              backgroundColor: terminalColors.redDark,
            },
          },
        },
      },
    },
    
    // DataGrid overrides for tabular data display
    MuiDataGrid: {
      styleOverrides: {
        root: {
          fontFamily: 'monospace',
          fontSize: '13px',
          backgroundColor: terminalColors.terminalWhite,
          
          // Header styling matching terminal column headers
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: terminalColors.dfhBlue,
            color: terminalColors.terminalWhite,
            fontWeight: 'bold',
            fontSize: '14px',
          },
          
          // Row styling for alternating colors
          '& .MuiDataGrid-row': {
            '&:nth-of-type(even)': {
              backgroundColor: '#F8F8F8',
            },
            '&:hover': {
              backgroundColor: '#E3F2FD',
            },
            '&.Mui-selected': {
              backgroundColor: terminalColors.dfhTurquoise,
              color: terminalColors.terminalBlack,
            },
          },
          
          // Cell styling for data alignment
          '& .MuiDataGrid-cell': {
            borderRight: `1px solid ${terminalColors.terminalGray}`,
            padding: '4px 8px',
            
            // Numeric cell styling
            '&.numeric-cell': {
              textAlign: 'right',
              fontVariantNumeric: 'tabular-nums',
            },
            
            // Currency cell styling
            '&.currency-cell': {
              textAlign: 'right',
              fontVariantNumeric: 'tabular-nums',
              color: terminalColors.dfhGreen,
              fontWeight: 'bold',
            },
          },
        },
      },
    },
    
    // AppBar overrides for application header
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: terminalColors.dfhBlue,
          color: terminalColors.terminalWhite,
          boxShadow: `0 2px 4px ${terminalColors.terminalGray}`,
          
          // Terminal header information styling
          '& .terminal-header': {
            fontFamily: 'monospace',
            fontSize: '14px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '8px 16px',
          },
          
          // Transaction identification styling
          '& .transaction-info': {
            color: terminalColors.dfhYellow,
            fontWeight: 'bold',
          },
          
          // Date/time display styling
          '& .datetime-info': {
            color: terminalColors.terminalWhite,
            fontSize: '12px',
          },
        },
      },
    },
    
    // Tooltip overrides for function key guidance
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: terminalColors.terminalBlack,
          color: terminalColors.dfhYellow,
          fontFamily: 'monospace',
          fontSize: '12px',
          padding: '8px 12px',
          maxWidth: '300px',
          
          // Function key tooltip styling
          '&.function-key-tooltip': {
            backgroundColor: terminalColors.dfhBlue,
            color: terminalColors.terminalWhite,
            fontWeight: 'bold',
          },
        },
        arrow: {
          color: terminalColors.terminalBlack,
        },
      },
    },
    
    // Chip overrides for status indicators and labels
    MuiChip: {
      styleOverrides: {
        root: {
          fontFamily: 'monospace',
          fontSize: '12px',
          fontWeight: 'bold',
          
          // Status chip variants
          '&.status-active': {
            backgroundColor: terminalColors.dfhGreen,
            color: terminalColors.terminalBlack,
          },
          '&.status-inactive': {
            backgroundColor: terminalColors.terminalGray,
            color: terminalColors.terminalWhite,
          },
          '&.status-error': {
            backgroundColor: terminalColors.dfhRed,
            color: terminalColors.terminalWhite,
          },
          '&.status-warning': {
            backgroundColor: terminalColors.dfhYellow,
            color: terminalColors.terminalBlack,
          },
          
          // Function key chip styling
          '&.function-key-chip': {
            backgroundColor: terminalColors.dfhBlue,
            color: terminalColors.terminalWhite,
            margin: '2px',
            minWidth: '60px',
          },
        },
      },
    },
  },
});

/**
 * Apply responsive font sizing to the theme
 * Ensures optimal readability across all device sizes while maintaining
 * terminal character proportions and financial data precision
 */
const theme = responsiveFontSizes(baseTheme, {
  breakpoints: ['sm', 'md', 'lg'],
  factor: 2,
  variants: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'subtitle1', 'subtitle2', 'body1', 'body2'],
});

export default theme;