/**
 * CardDemo Material-UI Theme Configuration
 * 
 * Implements BMS-to-React color palette mappings and component style overrides
 * required for preserving 3270 terminal visual behavior while enabling modern
 * responsive design. This theme configuration maintains exact functional
 * equivalence with original COBOL/CICS/BMS screen layouts.
 * 
 * Color Mappings (as specified in Summary of Changes):
 * - DFHGREEN → success.main (normal data display, field labels)
 * - DFHRED → error.main (error messages, validation failures)  
 * - DFHYELLOW → warning.main (highlighted fields, important notices)
 * - DFHBLUE → info.main (informational messages, help text)
 * 
 * Additional BMS Color Support:
 * - BLUE: Structure, labels, navigation elements
 * - TURQUOISE: Field prompts and instructions
 * - GREEN: Input fields and data entry
 * - NEUTRAL: Standard text content
 * 
 * Responsive Design: Configured for 24×80 character terminal structure
 * preservation across breakpoints (xs, sm, md, lg, xl) while maintaining
 * Material-UI responsive capabilities.
 * 
 * @version CardDemo_v1.0-15-g27d6c6f-68
 */

import { createTheme } from '@mui/material';
import { responsiveFontSizes } from '@mui/material/styles';

/**
 * Base theme configuration implementing BMS terminal color palette and
 * responsive breakpoints for 24×80 character layout preservation
 */
const baseTheme = createTheme({
  /**
   * Color palette implementing exact BMS terminal color mappings
   * Preserves 3270 terminal visual hierarchy and contrast ratios
   */
  palette: {
    // Primary brand colors for application identity
    primary: {
      main: '#1976d2', // Material-UI blue for navigation and primary actions
      light: '#42a5f5',
      dark: '#1565c0',
      contrastText: '#ffffff',
    },
    
    // Secondary accent colors for complementary elements
    secondary: {
      main: '#9c27b0', // Material-UI purple for secondary actions
      light: '#ba68c8',
      dark: '#7b1fa2',
      contrastText: '#ffffff',
    },
    
    // BMS color mappings as specified in Summary of Changes
    success: {
      main: '#00ff00', // DFHGREEN → success.main (terminal green)
      light: '#66ff66',
      dark: '#00cc00',
      contrastText: '#000000',
    },
    
    error: {
      main: '#ff0000', // DFHRED → error.main (terminal red)
      light: '#ff6666',
      dark: '#cc0000',
      contrastText: '#ffffff',
    },
    
    warning: {
      main: '#ffff00', // DFHYELLOW → warning.main (terminal yellow)
      light: '#ffff66',
      dark: '#cccc00',
      contrastText: '#000000',
    },
    
    info: {
      main: '#00ffff', // DFHBLUE → info.main (terminal cyan/turquoise)
      light: '#66ffff',
      dark: '#00cccc',
      contrastText: '#000000',
    },
    
    // Text colors matching terminal display characteristics
    text: {
      primary: '#ffffff', // White text on dark background (terminal style)
      secondary: '#cccccc', // Light gray for secondary text
      disabled: '#888888',
    },
    
    // Background colors maintaining terminal appearance
    background: {
      default: '#000000', // Black background (terminal style)
      paper: '#1a1a1a', // Dark gray for elevated surfaces
    },
    
    // Divider and border colors
    divider: '#333333',
    
    // Additional BMS-specific colors for component styling
    custom: {
      terminalBlue: '#0000ff', // BMS BLUE for structure and labels
      terminalTurquoise: '#00ffff', // BMS TURQUOISE for prompts
      terminalGreen: '#00ff00', // BMS GREEN for input fields
      terminalNeutral: '#ffffff', // BMS NEUTRAL for standard text
    },
  },
  
  /**
   * Responsive breakpoints configured for 24×80 character terminal
   * structure preservation while enabling modern responsive design
   */
  breakpoints: {
    values: {
      xs: 320,  // Mobile portrait (minimum viable width)
      sm: 640,  // Terminal-equivalent width (80 chars × 8px)
      md: 960,  // Tablet landscape
      lg: 1280, // Desktop standard
      xl: 1920, // Large desktop/4K displays
    },
  },
  
  /**
   * Typography system maintaining terminal character spacing and
   * readability for financial data display consistency
   */
  typography: {
    fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    fontSize: 14, // Base font size for terminal character equivalence
    
    // Heading styles preserving terminal hierarchy
    h1: {
      fontSize: '2.125rem', // 34px
      fontWeight: 700,
      lineHeight: 1.2,
      letterSpacing: '0.00735em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    h2: {
      fontSize: '1.75rem', // 28px
      fontWeight: 700,
      lineHeight: 1.3,
      letterSpacing: '0.00735em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    h3: {
      fontSize: '1.5rem', // 24px
      fontWeight: 600,
      lineHeight: 1.4,
      letterSpacing: '0.0075em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    h4: {
      fontSize: '1.25rem', // 20px
      fontWeight: 600,
      lineHeight: 1.4,
      letterSpacing: '0.00735em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    h5: {
      fontSize: '1.125rem', // 18px
      fontWeight: 600,
      lineHeight: 1.5,
      letterSpacing: '0.0075em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    h6: {
      fontSize: '1rem', // 16px
      fontWeight: 600,
      lineHeight: 1.5,
      letterSpacing: '0.0075em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    
    // Body text styles for data display
    body1: {
      fontSize: '1rem', // 16px
      fontWeight: 400,
      lineHeight: 1.5,
      letterSpacing: '0.00938em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    body2: {
      fontSize: '0.875rem', // 14px
      fontWeight: 400,
      lineHeight: 1.43,
      letterSpacing: '0.01071em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    
    // Subtitle styles for field labels
    subtitle1: {
      fontSize: '1rem', // 16px
      fontWeight: 500,
      lineHeight: 1.75,
      letterSpacing: '0.00938em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
    subtitle2: {
      fontSize: '0.875rem', // 14px
      fontWeight: 500,
      lineHeight: 1.57,
      letterSpacing: '0.00714em',
      fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
    },
  },
  
  /**
   * Spacing configuration for consistent layout
   */
  spacing: 8, // Base spacing unit (8px)
  
  /**
   * Shape configuration for component borders
   */
  shape: {
    borderRadius: 4, // Minimal border radius for terminal-like appearance
  },
  
  /**
   * Z-index configuration for layered components
   */
  zIndex: {
    appBar: 1100,
    drawer: 1200,
    modal: 1300,
    snackbar: 1400,
    tooltip: 1500,
  },
  
  /**
   * Transition configuration for smooth animations
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
   * Component style overrides for BMS attribute replication
   * Enables Material-UI components to maintain terminal field behavior
   */
  components: {
    /**
     * TextField overrides for BMS field attribute replication
     * Implements protected/unprotected field behavior and terminal styling
     */
    MuiTextField: {
      styleOverrides: {
        root: {
          // Terminal-style input field styling
          '& .MuiOutlinedInput-root': {
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            '& fieldset': {
              borderColor: '#333333',
            },
            '&:hover fieldset': {
              borderColor: '#666666',
            },
            '&.Mui-focused fieldset': {
              borderColor: '#00ffff', // Terminal turquoise for focus
            },
            '&.Mui-error fieldset': {
              borderColor: '#ff0000', // Terminal red for errors
            },
          },
          // Protected field styling (ASKIP attribute)
          '&.protected-field': {
            '& .MuiOutlinedInput-root': {
              backgroundColor: 'transparent',
              '& input': {
                color: '#cccccc',
                cursor: 'default',
              },
            },
          },
          // Input field styling (UNPROT attribute)
          '&.input-field': {
            '& .MuiOutlinedInput-root': {
              '& input': {
                color: '#00ff00', // Terminal green for input
                fontWeight: 'bold',
              },
            },
          },
          // Highlighted field styling (BRT attribute)
          '&.highlighted-field': {
            '& .MuiOutlinedInput-root': {
              '& input': {
                fontWeight: 'bold',
                textShadow: '0 0 2px currentColor',
              },
            },
          },
        },
      },
    },
    
    /**
     * Button overrides for terminal-style function key appearance
     */
    MuiButton: {
      styleOverrides: {
        root: {
          fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
          textTransform: 'none', // Preserve original case
          borderRadius: '2px', // Minimal rounding for terminal appearance
          // Function key styling
          '&.function-key': {
            backgroundColor: '#333333',
            color: '#ffffff',
            border: '1px solid #666666',
            minWidth: '80px',
            '&:hover': {
              backgroundColor: '#555555',
            },
            '&:active': {
              backgroundColor: '#222222',
            },
          },
          // Primary action button styling
          '&.primary-action': {
            backgroundColor: '#0000ff', // Terminal blue
            color: '#ffffff',
            '&:hover': {
              backgroundColor: '#0033cc',
            },
          },
        },
      },
    },
    
    /**
     * DataGrid overrides for terminal-style data display
     */
    MuiDataGrid: {
      styleOverrides: {
        root: {
          backgroundColor: '#000000',
          color: '#ffffff',
          fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
          border: '1px solid #333333',
          // Header styling
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: '#1a1a1a',
            color: '#00ffff', // Terminal turquoise for headers
            borderBottom: '1px solid #333333',
          },
          // Row styling
          '& .MuiDataGrid-row': {
            borderBottom: '1px solid #222222',
            '&:hover': {
              backgroundColor: '#1a1a1a',
            },
            '&.Mui-selected': {
              backgroundColor: '#333333',
            },
          },
          // Cell styling
          '& .MuiDataGrid-cell': {
            borderRight: '1px solid #222222',
            padding: '4px 8px',
          },
        },
      },
    },
    
    /**
     * AppBar overrides for terminal-style header display
     */
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: '#1a1a1a',
          color: '#ffffff',
          boxShadow: '0 1px 0 #333333',
          // Terminal header styling
          '& .terminal-header': {
            fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
            fontSize: '14px',
            fontWeight: 'normal',
          },
          // Transaction info styling
          '& .transaction-info': {
            color: '#0000ff', // Terminal blue for transaction codes
          },
          // Title styling
          '& .screen-title': {
            color: '#ffff00', // Terminal yellow for titles
            fontWeight: 'bold',
          },
          // Date/time styling
          '& .datetime-display': {
            color: '#0000ff', // Terminal blue for system info
            fontSize: '12px',
          },
        },
      },
    },
    
    /**
     * Tooltip overrides for function key guidance
     */
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: '#1a1a1a',
          color: '#ffffff',
          border: '1px solid #333333',
          fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
          fontSize: '12px',
          maxWidth: '300px',
        },
        arrow: {
          color: '#1a1a1a',
          '&::before': {
            border: '1px solid #333333',
          },
        },
      },
    },
    
    /**
     * Chip overrides for status indicators and function key display
     */
    MuiChip: {
      styleOverrides: {
        root: {
          fontFamily: '"Courier New", "Monaco", "Lucida Console", monospace',
          fontSize: '12px',
          // Function key chip styling
          '&.function-key-chip': {
            backgroundColor: '#333333',
            color: '#ffffff',
            border: '1px solid #666666',
          },
          // Status chip styling
          '&.status-chip': {
            fontWeight: 'bold',
            '&.active': {
              backgroundColor: '#00ff00',
              color: '#000000',
            },
            '&.inactive': {
              backgroundColor: '#ff0000',
              color: '#ffffff',
            },
            '&.pending': {
              backgroundColor: '#ffff00',
              color: '#000000',
            },
          },
        },
      },
    },
  },
});

/**
 * Apply responsive font sizing to ensure consistent typography
 * across all breakpoints while maintaining terminal character spacing
 */
const theme = responsiveFontSizes(baseTheme, {
  breakpoints: ['xs', 'sm', 'md', 'lg', 'xl'],
  factor: 2, // Conservative scaling factor to preserve readability
});

export default theme;