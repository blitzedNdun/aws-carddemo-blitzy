/**
 * MainMenu.jsx - Main Menu Screen for Regular Users
 *
 * React component implementing the Main Menu screen (COMEN01) that serves as the
 * primary navigation hub for regular users. Converted from COBOL program COMEN01C
 * and BMS mapset COMEN01.
 *
 * This component replicates the exact functionality of the mainframe CICS transaction
 * CM00, maintaining identical screen layout, validation rules, and navigation patterns
 * while providing a modern React-based user interface.
 *
 * Key Features:
 * - Displays numbered menu options (1-12) with role-based availability
 * - Handles keyboard navigation for option selection
 * - Implements PF-key functions (F3=Exit, Enter=Select)
 * - Maintains session context and routes to selected business functions
 * - Preserves COBOL validation logic and error handling patterns
 *
 * Converted from:
 * - COBOL Program: COMEN01C.cbl (transaction CM00)
 * - BMS Mapset: COMEN01.bms
 * - Working Storage: Menu option arrays and validation logic
 */

// External imports - React core functionality
import { TextField, List, ListItem, Box, Typography } from '@mui/material';
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

// External imports - Material-UI components for consistent styling

// Internal imports - API services and validation utilities
import { apiService } from '../../services/api.js';

/**
 * Main Menu Data Structure
 * Replicates CDEMO-MENU-OPT data from COMEN02Y copybook
 * Each menu option contains number, description, target program, and access level
 */
const MENU_OPTIONS = [
  {
    number: 1,
    name: 'Account View',
    description: 'View Account Information',
    programName: 'COACTVW',
    routePath: '/account/view',
    userType: 'U', // U=User, A=Admin
  },
  {
    number: 2,
    name: 'Account Update',
    description: 'Update Account Information',
    programName: 'COACTUP',
    routePath: '/account/update',
    userType: 'U',
  },
  {
    number: 3,
    name: 'Card List',
    description: 'List Credit Cards',
    programName: 'COCRDLI',
    routePath: '/card/list',
    userType: 'U',
  },
  {
    number: 4,
    name: 'Card View',
    description: 'View Credit Card Information',
    programName: 'COCRDSL',
    routePath: '/card/view',
    userType: 'U',
  },
  {
    number: 5,
    name: 'Card Update',
    description: 'Update Credit Card Information',
    programName: 'COCRDUP',
    routePath: '/card/update',
    userType: 'U',
  },
  {
    number: 6,
    name: 'Transaction List',
    description: 'List Transactions',
    programName: 'COTRN00',
    routePath: '/transaction/list',
    userType: 'U',
  },
  {
    number: 7,
    name: 'Transaction Detail',
    description: 'View Transaction Details',
    programName: 'COTRN01',
    routePath: '/transaction/detail',
    userType: 'U',
  },
  {
    number: 8,
    name: 'Add Transaction',
    description: 'Add New Transaction',
    programName: 'COTRN02',
    routePath: '/transaction/add',
    userType: 'U',
  },
  {
    number: 9,
    name: 'Bill Payment',
    description: 'Process Bill Payment',
    programName: 'COBIL00',
    routePath: '/billing/payment',
    userType: 'U',
  },
  {
    number: 10,
    name: 'User Management',
    description: 'User Administration',
    programName: 'COUSR00',
    routePath: '/admin/users',
    userType: 'A', // Admin only
  },
  {
    number: 11,
    name: 'Reports',
    description: 'Generate Reports',
    programName: 'CORPT00',
    routePath: '/reports',
    userType: 'U',
  },
  {
    number: 12,
    name: 'System Admin',
    description: 'System Administration',
    programName: 'COADM01',
    routePath: '/admin/system',
    userType: 'A', // Admin only
  },
];

/**
 * MainMenu Component
 *
 * Functional React component that implements the Main Menu screen functionality.
 * Manages state for option selection, error handling, and user interaction.
 *
 * @returns {JSX.Element} Main menu screen component
 */
const MainMenu = () => {
  // Component state management - replicates COBOL working storage variables
  const [selectedOption, setSelectedOption] = useState(''); // WS-OPTION equivalent
  const [errorMessage, setErrorMessage] = useState(''); // WS-MESSAGE equivalent
  const [currentDateTime, setCurrentDateTime] = useState({
    date: '',
    time: '',
  });
  const [userRole, setUserRole] = useState('U'); // Default to regular user
  const [isLoading, setIsLoading] = useState(false);

  // Navigation hook for programmatic routing (replaces CICS XCTL)
  const navigate = useNavigate();

  /**
   * Initialize current date and time display
   * Replicates POPULATE-HEADER-INFO paragraph from COBOL
   */
  const updateDateTime = useCallback(() => {
    const now = new Date();

    // Format date as MM/DD/YY to match BMS CURDATE format
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const year = String(now.getFullYear()).slice(-2);
    const formattedDate = `${month}/${day}/${year}`;

    // Format time as HH:MM:SS to match BMS CURTIME format
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const formattedTime = `${hours}:${minutes}:${seconds}`;

    setCurrentDateTime({
      date: formattedDate,
      time: formattedTime,
    });
  }, []);

  /**
   * Load user role from session storage
   * Determines which menu options are available to the current user
   */
  const loadUserRole = useCallback(() => {
    try {
      const storedRole = sessionStorage.getItem('userRole');
      if (storedRole === 'ADMIN') {
        setUserRole('A');
      } else {
        setUserRole('U');
      }
    } catch (error) {
      console.warn('Failed to load user role from session:', error);
      setUserRole('U'); // Default to regular user
    }
  }, []);

  /**
   * Component initialization effect
   * Runs on component mount to set up initial state
   */
  useEffect(() => {
    updateDateTime();
    loadUserRole();

    // Update time every second for real-time display
    const timeInterval = setInterval(updateDateTime, 1000);

    // Cleanup interval on component unmount
    return () => clearInterval(timeInterval);
  }, [updateDateTime, loadUserRole]);

  /**
   * Handle option input change
   * Validates numeric input and updates selected option state
   */
  const handleOptionChange = (event) => {
    const value = event.target.value;

    // Only allow numeric input (matching COBOL NUM attribute)
    if (value === '' || /^\d{1,2}$/.test(value)) {
      setSelectedOption(value);
      setErrorMessage(''); // Clear any previous error messages
    }
  };

  /**
   * Navigate to selected menu option
   * Replaces CICS XCTL functionality with React Router navigation
   */
  const navigateToSelectedOption = useCallback((menuOption) => {
    setIsLoading(true);

    try {
      // Update session context (similar to COMMAREA updates)
      sessionStorage.setItem('lastTransaction', 'CM00');
      sessionStorage.setItem('lastProgram', 'COMEN01C');
      sessionStorage.setItem('selectedOption', menuOption.number.toString());
      sessionStorage.setItem('targetProgram', menuOption.programName);

      // Navigate to the selected route
      navigate(menuOption.routePath, {
        state: {
          fromMenu: true,
          selectedOption: menuOption.number,
          programName: menuOption.programName,
        },
      });
    } catch (error) {
      console.error('Navigation error:', error);
      setErrorMessage('System error - please try again...');
    } finally {
      setIsLoading(false);
    }
  }, [navigate]);

  /**
   * Process Enter key - validate and navigate to selected option
   * Replicates PROCESS-ENTER-KEY paragraph from COBOL
   */
  const handleEnterKey = useCallback(() => {
    // Clear any existing error messages
    setErrorMessage('');

    // Validate option selection (replicates COBOL validation logic)
    const optionNum = parseInt(selectedOption, 10);

    if (!selectedOption || selectedOption.trim() === '') {
      setErrorMessage('Please enter a valid option number...');
      return;
    }

    if (isNaN(optionNum) || optionNum < 1 || optionNum > MENU_OPTIONS.length) {
      setErrorMessage('Please enter a valid option number...');
      return;
    }

    // Find the selected menu option
    const selectedMenuOption = MENU_OPTIONS.find(option => option.number === optionNum);

    if (!selectedMenuOption) {
      setErrorMessage('Please enter a valid option number...');
      return;
    }

    // Role-based access control (replicates COBOL user type checking)
    if (userRole === 'U' && selectedMenuOption.userType === 'A') {
      setErrorMessage('No access - Admin Only option...');
      return;
    }

    // Navigate to selected function (replaces CICS XCTL)
    navigateToSelectedOption(selectedMenuOption);
  }, [selectedOption, userRole, navigateToSelectedOption]);

  /**
   * Handle exit to sign-on screen
   * Replicates RETURN-TO-SIGNON-SCREEN paragraph from COBOL
   */
  const handleExitToSignOn = useCallback(async () => {
    setIsLoading(true);

    try {
      // Sign out user (clears session data)
      await apiService.signOut();

      // Navigate to sign-on screen
      navigate('/signin', { replace: true });
    } catch (error) {
      console.error('Sign-out error:', error);
      // Even if sign-out fails, still navigate to sign-on
      navigate('/signin', { replace: true });
    } finally {
      setIsLoading(false);
    }
  }, [navigate]);

  /**
   * Keyboard event handler for PF-key simulation
   * Replicates CICS PF-key functionality (F3=Exit, Enter=Process)
   */
  const handleKeyDown = useCallback((event) => {
    switch (event.key) {
      case 'F3':
      case 'Escape':
        // F3 key - return to sign-on screen (COSGN00C)
        event.preventDefault();
        handleExitToSignOn();
        break;
      case 'Enter':
        // Enter key - process selected option
        event.preventDefault();
        handleEnterKey();
        break;
      default:
        // Allow normal key processing
        break;
    }
  }, [handleEnterKey, handleExitToSignOn]);

  /**
   * Attach keyboard event listeners
   */
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  /**
   * Handle menu option click
   * Provides mouse-based alternative to keyboard entry
   */
  const handleOptionClick = (optionNumber) => {
    setSelectedOption(optionNumber.toString());
    setErrorMessage('');
  };

  /**
   * Filter menu options based on user role
   * Returns only options accessible to the current user
   */
  const getAccessibleMenuOptions = () => {
    if (userRole === 'A') {
      // Admin users can see all options
      return MENU_OPTIONS;
    } else {
      // Regular users can only see non-admin options
      return MENU_OPTIONS.filter(option => option.userType === 'U');
    }
  };

  // Render the main menu component
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        maxWidth: '1200px',
        margin: '0 auto',
        padding: '1rem',
        fontFamily: 'monospace',
        backgroundColor: '#f5f5f5',
      }}
    >
      {/* Screen Header - replicates BMS lines 1-2 */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#e0e0e0',
          padding: '0.75rem',
          borderBottom: '2px solid #333',
          marginBottom: '1rem',
        }}
      >
        {/* Left side - Transaction and Program info */}
        <Box sx={{ flex: 1 }}>
          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
            Tran: CM00
          </Typography>
          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
            Prog: COMEN01C
          </Typography>
        </Box>

        {/* Center - Title */}
        <Box sx={{ flex: 2, textAlign: 'center' }}>
          <Typography variant="h6" sx={{ fontFamily: 'monospace', fontWeight: 'bold', color: '#856404' }}>
            CREDIT CARD DEMO
          </Typography>
          <Typography variant="h6" sx={{ fontFamily: 'monospace', fontWeight: 'bold', color: '#856404' }}>
            MAIN MENU
          </Typography>
        </Box>

        {/* Right side - Date and Time */}
        <Box sx={{ flex: 1, textAlign: 'right' }}>
          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
            Date: {currentDateTime.date}
          </Typography>
          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
            Time: {currentDateTime.time}
          </Typography>
        </Box>
      </Box>

      {/* Main Content Area */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {/* Menu Title */}
        <Typography
          variant="h5"
          align="center"
          sx={{
            fontFamily: 'monospace',
            fontWeight: 'bold',
            color: '#333',
            marginBottom: '2rem',
          }}
        >
          Main Menu
        </Typography>

        {/* Menu Options List */}
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <List dense disablePadding sx={{ width: '100%', maxWidth: '600px' }}>
            {getAccessibleMenuOptions().map((option) => (
              <ListItem
                key={option.number}
                component="button"
                dense
                onClick={() => handleOptionClick(option.number)}
                sx={{
                  padding: '0.5rem 1rem',
                  cursor: 'pointer',
                  fontFamily: 'monospace',
                  backgroundColor: selectedOption === option.number.toString() ? '#d1ecf1' : 'transparent',
                  '&:hover': {
                    backgroundColor: '#f8f9fa',
                  },
                  borderRadius: '4px',
                  marginBottom: '0.25rem',
                }}
              >
                <Typography
                  variant="body1"
                  sx={{
                    fontFamily: 'monospace',
                    color: '#0c5460',
                    width: '100%',
                  }}
                >
                  {option.number}. {option.description}
                </Typography>
              </ListItem>
            ))}
          </List>
        </Box>

        {/* Option Selection Input */}
        <Box sx={{ display: 'flex', justifyContent: 'center', marginTop: '2rem' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <Typography
              variant="body1"
              sx={{ fontFamily: 'monospace', color: '#0c5460' }}
            >
              Please select an option:
            </Typography>
            <TextField
              variant="outlined"
              type="text"
              inputProps={{
                maxLength: 2,
                style: {
                  fontFamily: 'monospace',
                  textAlign: 'center',
                  fontSize: '1.1rem',
                },
              }}
              value={selectedOption}
              onChange={handleOptionChange}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  handleEnterKey();
                }
              }}
              error={!!errorMessage}
              helperText={errorMessage || ' '}
              sx={{
                width: '80px',
                '& .MuiOutlinedInput-root': {
                  backgroundColor: '#fff',
                },
              }}
              autoFocus
            />
          </Box>
        </Box>
      </Box>

      {/* Error Message Area removed to prevent duplicate display - using TextField helperText instead */}

      {/* Function Keys Footer - replicates BMS line 24 */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#e0e0e0',
          padding: '0.5rem',
          borderTop: '1px solid #333',
          marginTop: 'auto',
          fontFamily: 'monospace',
        }}
      >
        <Typography variant="body2" sx={{ color: '#856404' }}>
          ENTER=Continue F3=Exit
        </Typography>

        {isLoading && (
          <Typography variant="body2" sx={{ color: '#0c5460' }}>
            Processing...
          </Typography>
        )}
      </Box>
    </Box>
  );
};

// Default export for the MainMenu component
export default MainMenu;
