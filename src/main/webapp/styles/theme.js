/*
 * CardDemo Material-UI Theme Configuration
 * 
 * This file implements the BMS-to-React color palette mappings and component style overrides
 * required for preserving 3270 terminal visual behavior while enabling modern responsive design.
 * 
 * Color mappings preserve the original CICS terminal color semantics:
 * - DFHGREEN → success.main (normal data display, field labels)
 * - DFHRED → error.main (error messages, validation failures)
 * - DFHYELLOW → warning.main (highlighted fields, important notices)
 * - DFHBLUE → info.main (informational messages, help text)
 * - TURQUOISE → primary.main (user prompts, navigation elements)
 * - NEUTRAL → text.primary (standard display text)
 * 
 * Responsive breakpoints maintain 24×80 character terminal structure while supporting
 * modern device sizes and orientations.
 */

import { createTheme } from '@mui/material';
import { responsiveFontSizes } from '@mui/material/styles';

// Base theme configuration with BMS color palette mappings
const baseTheme = createTheme({
  palette: {
    // Primary color for application branding and navigation
    primary: {
      main: '#00ACC1', // TURQUOISE - user prompts, navigation elements
      light: '#5DDEF4',
      dark: '#007C91',
      contrastText: '#FFFFFF',
    },
    // Secondary color for subtle UI elements
    secondary: {
      main: '#37474F', // Blue-grey for secondary actions
      light: '#62727B',
      dark: '#102027',
      contrastText: '#FFFFFF',
    },
    // BMS color mapping: DFHGREEN → success.main (normal data display, field labels)
    success: {
      main: '#00C853', // Bright green for normal data display
      light: '#5EFC82',
      dark: '#009624',
      contrastText: '#000000',
    },
    // BMS color mapping: DFHRED → error.main (error messages, validation failures)
    error: {
      main: '#D32F2F', // Standard red for error messages
      light: '#EF5350',
      dark: '#C62828',
      contrastText: '#FFFFFF',
    },
    // BMS color mapping: DFHYELLOW → warning.main (highlighted fields, important notices)
    warning: {
      main: '#FFA000', // Amber for highlighted fields and warnings
      light: '#FFB74D',
      dark: '#F57C00',
      contrastText: '#000000',
    },
    // BMS color mapping: DFHBLUE → info.main (informational messages, help text)
    info: {
      main: '#1976D2', // Blue for informational messages
      light: '#42A5F5',
      dark: '#1565C0',
      contrastText: '#FFFFFF',
    },
    // Text colors for terminal-like display
    text: {
      primary: '#212121', // NEUTRAL - standard display text
      secondary: '#757575', // Muted text for secondary information
      disabled: '#BDBDBD',
    },
    // Background colors maintaining terminal contrast
    background: {
      default: '#FAFAFA', // Light grey background for main content
      paper: '#FFFFFF', // White background for cards and forms
    },
    // Divider color for visual separation
    divider: '#E0E0E0',
  },
  
  // Responsive breakpoints for 24×80 character terminal structure preservation
  breakpoints: {
    values: {
      xs: 0,    // Extra small devices (phones)
      sm: 600,  // Small devices (tablets)
      md: 960,  // Medium devices (laptops) - optimal for 80-character width
      lg: 1280, // Large devices (desktops)
      xl: 1920, // Extra large devices (wide screens)
    },
  },
  
  // Typography system matching terminal character spacing and readability
  typography: {
    fontFamily: [
      '"Roboto Mono"',    // Primary monospace font for data display
      '"Courier New"',    // Fallback monospace font
      'monospace',        // System monospace fallback
    ].join(','),
    fontSize: 14,         // Base font size for terminal readability
    
    // Typography variants for different content types
    h1: {
      fontSize: '2.125rem',
      fontWeight: 500,
      lineHeight: 1.2,
      letterSpacing: '-0.00833em',
    },
    h2: {
      fontSize: '1.5rem',
      fontWeight: 500,
      lineHeight: 1.2,
      letterSpacing: '0em',
    },
    h3: {
      fontSize: '1.25rem',
      fontWeight: 500,
      lineHeight: 1.2,
      letterSpacing: '0.00735em',
    },
    h4: {
      fontSize: '1.125rem',
      fontWeight: 500,
      lineHeight: 1.2,
      letterSpacing: '0.00735em',
    },
    h5: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.2,
      letterSpacing: '0em',
    },
    h6: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.2,
      letterSpacing: '0.00735em',
    },
    // Body text for forms and data display
    body1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.5,
      letterSpacing: '0.00938em',
    },
    body2: {
      fontSize: '0.875rem',
      fontWeight: 400,
      lineHeight: 1.43,
      letterSpacing: '0.01071em',
    },
    // Subtitle variants for labels and secondary text
    subtitle1: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.75,
      letterSpacing: '0.00938em',
    },
    subtitle2: {
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.57,
      letterSpacing: '0.00714em',
    },
  },
  
  // Spacing system for consistent layout
  spacing: 8, // 8px base spacing unit
  
  // Shape configuration for consistent borders
  shape: {
    borderRadius: 4,
  },
  
  // Z-index configuration for proper layering
  zIndex: {
    tooltip: 1500,
    modal: 1300,
    appBar: 1100,
  },
  
  // Transition configuration for smooth animations
  transitions: {
    duration: {
      shortest: 150,
      shorter: 200,
      short: 250,
      standard: 300,
      complex: 375,
      enteringScreen: 225,
      leavingScreen: 195,
    },
    easing: {
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
    },
  },
  
  // Component style overrides for BMS attribute replication
  components: {
    // TextField overrides for BMS field attribute behavior
    MuiTextField: {
      styleOverrides: {
        root: {
          // Monospace font for data consistency
          fontFamily: '"Roboto Mono", "Courier New", monospace',
          
          // Standard field spacing
          marginBottom: '16px',
          
          // Terminal-like field appearance
          '& .MuiOutlinedInput-root': {
            backgroundColor: '#FFFFFF',
            
            // Focus state similar to terminal cursor
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
              borderColor: '#00ACC1',
              borderWidth: '2px',
            },
            
            // Error state for validation failures
            '&.Mui-error .MuiOutlinedInput-notchedOutline': {
              borderColor: '#D32F2F',
              borderWidth: '2px',
            },
            
            // Read-only field styling (ASKIP attribute)
            '&.Mui-readOnly': {
              backgroundColor: '#F5F5F5',
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#E0E0E0',
              },
            },
            
            // Disabled field styling (PROT attribute)
            '&.Mui-disabled': {
              backgroundColor: '#FAFAFA',
              '& .MuiOutlinedInput-notchedOutline': {
                borderColor: '#E0E0E0',
              },
            },
          },
          
          // Helper text styling for field guidance
          '& .MuiFormHelperText-root': {
            fontFamily: '"Roboto Mono", "Courier New", monospace',
            fontSize: '0.75rem',
            marginTop: '4px',
          },
          
          // Label styling for field identification
          '& .MuiInputLabel-root': {
            fontFamily: '"Roboto Mono", "Courier New", monospace',
            fontSize: '0.875rem',
            fontWeight: 500,
          },
        },
      },
    },
    
    // Button overrides for function key equivalent behavior
    MuiButton: {
      styleOverrides: {
        root: {
          fontFamily: '"Roboto Mono", "Courier New", monospace',
          fontSize: '0.875rem',
          fontWeight: 500,
          textTransform: 'none', // Preserve original case
          minWidth: '80px',
          padding: '8px 16px',
          
          // Function key button styling
          '&.function-key': {
            backgroundColor: '#37474F',
            color: '#FFFFFF',
            border: '1px solid #37474F',
            
            '&:hover': {
              backgroundColor: '#455A64',
              borderColor: '#455A64',
            },
            
            '&:active': {
              backgroundColor: '#263238',
              borderColor: '#263238',
            },
          },
          
          // Primary action button styling
          '&.primary-action': {
            backgroundColor: '#00ACC1',
            color: '#FFFFFF',
            
            '&:hover': {
              backgroundColor: '#00838F',
            },
          },
          
          // Secondary action button styling
          '&.secondary-action': {
            backgroundColor: 'transparent',
            color: '#37474F',
            border: '1px solid #37474F',
            
            '&:hover': {
              backgroundColor: '#37474F',
              color: '#FFFFFF',
            },
          },
        },
      },
    },
    
    // DataGrid overrides for tabular data display
    MuiDataGrid: {
      styleOverrides: {
        root: {
          fontFamily: '"Roboto Mono", "Courier New", monospace',
          fontSize: '0.875rem',
          
          // Terminal-like table appearance
          '& .MuiDataGrid-cell': {
            borderColor: '#E0E0E0',
            padding: '8px',
            
            // Numeric cell alignment
            '&.numeric-cell': {
              textAlign: 'right',
            },
            
            // Status cell styling
            '&.status-cell': {
              fontWeight: 500,
            },
          },
          
          // Header styling
          '& .MuiDataGrid-columnHeader': {
            backgroundColor: '#F5F5F5',
            fontWeight: 600,
            borderColor: '#E0E0E0',
          },
          
          // Row styling
          '& .MuiDataGrid-row': {
            '&:hover': {
              backgroundColor: '#F9F9F9',
            },
            
            '&.Mui-selected': {
              backgroundColor: '#E3F2FD',
            },
          },
          
          // Footer styling
          '& .MuiDataGrid-footer': {
            borderColor: '#E0E0E0',
            backgroundColor: '#FAFAFA',
          },
        },
      },
    },
    
    // AppBar overrides for header component
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: '#37474F',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
          
          // Terminal-like header appearance
          '& .MuiToolbar-root': {
            minHeight: '56px',
            padding: '0 16px',
            
            // Title styling
            '& .MuiTypography-h6': {
              fontFamily: '"Roboto Mono", "Courier New", monospace',
              fontSize: '1.125rem',
              fontWeight: 500,
              color: '#FFFFFF',
            },
            
            // Transaction code styling
            '& .transaction-code': {
              fontFamily: '"Roboto Mono", "Courier New", monospace',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: '#00ACC1',
              marginRight: '16px',
            },
            
            // Timestamp styling
            '& .timestamp': {
              fontFamily: '"Roboto Mono", "Courier New", monospace',
              fontSize: '0.75rem',
              color: '#B0BEC5',
            },
          },
        },
      },
    },
    
    // Tooltip overrides for help text display
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          fontFamily: '"Roboto Mono", "Courier New", monospace',
          fontSize: '0.75rem',
          backgroundColor: '#37474F',
          color: '#FFFFFF',
          maxWidth: '300px',
          padding: '8px 12px',
        },
        arrow: {
          color: '#37474F',
        },
      },
    },
    
    // Chip overrides for status indicators and function key labels
    MuiChip: {
      styleOverrides: {
        root: {
          fontFamily: '"Roboto Mono", "Courier New", monospace',
          fontSize: '0.75rem',
          height: '24px',
          
          // Function key chip styling
          '&.function-key-chip': {
            backgroundColor: '#37474F',
            color: '#FFFFFF',
            
            '&:hover': {
              backgroundColor: '#455A64',
            },
          },
          
          // Status chip styling
          '&.status-chip': {
            fontWeight: 500,
          },
        },
      },
    },
  },
});

// Apply responsive font sizing while maintaining terminal character spacing
const theme = responsiveFontSizes(baseTheme, {
  breakpoints: ['sm', 'md', 'lg'],
  factor: 1.2,
});

export default theme;