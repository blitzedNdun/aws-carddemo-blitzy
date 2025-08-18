/**
 * AdminMenu.jsx - Admin Menu Screen Component
 *
 * React component for the Admin Menu screen (COADM01), providing administrative function access
 * for privileged users. Displays enhanced menu options including user management functions,
 * verifies administrative role access, handles option selection via keyboard input, and implements
 * PF-key navigation (F3=Exit, Enter=Select).
 *
 * Converted from mainframe COBOL program COADM01C.cbl and BMS mapset COADM01.bms
 * Maps to CICS transaction code CA00
 *
 * Key Features:
 * - Administrative role verification required
 * - Menu options with numeric selection (1-4)
 * - Keyboard event handlers for PF-keys and option selection
 * - Material-UI components for consistent styling
 * - Error message display area for validation feedback
 * - Screen flow matching original 3270 navigation
 */

// External imports
import { TextField } from '@mui/material';
import { Formik } from 'formik';
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import * as yup from 'yup';

// Internal imports - ONLY from depends_on_files
import { apiService } from '../../services/api.js';

// =============================================================================
// COMPONENT CONSTANTS AND CONFIGURATION
// =============================================================================

/**
 * Admin menu options configuration
 * Maps to CDEMO-ADMIN-OPT arrays from COBOL program COADM02Y copybook
 */
const ADMIN_MENU_OPTIONS = [
  { num: '1', name: 'User List', pgmName: 'COUSR00C', route: '/admin/users' },
  { num: '2', name: 'Add User', pgmName: 'COUSR01C', route: '/admin/users/add' },
  { num: '3', name: 'Update User', pgmName: 'COUSR02C', route: '/admin/users/update' },
  { num: '4', name: 'Admin Reports', pgmName: 'CORPT00C', route: '/admin/reports' },
];

/**
 * Transaction and program identification
 * Maps to COBOL program variables WS-TRANID and WS-PGMNAME
 */
const TRANSACTION_ID = 'CA00';
const PROGRAM_NAME = 'COADM01C';
const SCREEN_TITLE = 'Admin Menu';

/**
 * Validation schema for option selection
 * Matches COBOL validation logic from PROCESS-ENTER-KEY paragraph
 */
const validationSchema = yup.object({
  option: yup
    .number()
    .required('Please enter a valid option number...')
    .integer('Option must be a whole number')
    .min(1, 'Please enter a valid option number...')
    .max(ADMIN_MENU_OPTIONS.length, 'Please enter a valid option number...'),
});

// =============================================================================
// MAIN COMPONENT IMPLEMENTATION
// =============================================================================

/**
 * AdminMenu Component - Main admin menu screen
 * Implements COBOL program COADM01C logic with React patterns
 */
const AdminMenu = () => {
  // React hooks for state management
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  // =============================================================================
  // AUTHENTICATION AND ROLE VERIFICATION
  // =============================================================================

  /**
   * Handle exit to sign-on screen
   * Maps to COBOL RETURN-TO-SIGNON-SCREEN paragraph
   */
  const handleExitToSignOn = useCallback(async () => {
    try {
      setIsLoading(true);

      // Call signOut API method as required by schema
      await apiService.signOut();

      // Navigate back to sign-on screen
      // Maps to COBOL EXEC CICS XCTL PROGRAM('COSGN00C') logic
      navigate('/signin');

    } catch (error) {
      console.error('Error during exit to sign-on:', error);
      // Force navigation even if API call fails
      navigate('/signin');
    } finally {
      setIsLoading(false);
    }
  }, [navigate]);

  /**
   * Verify admin role access
   * Maps to COBOL logic checking CDEMO user role in COMMAREA
   */
  const verifyAdminAccess = useCallback(async () => {
    try {
      const userRole = sessionStorage.getItem('userRole');

      if (!userRole || userRole !== 'ADMIN') {
        setErrorMessage('Administrative access required - insufficient privileges');
        setTimeout(() => {
          handleExitToSignOn();
        }, 2000);
        return false;
      }

      return true;
    } catch (error) {
      console.error('Error verifying admin access:', error);
      setErrorMessage('Unable to verify administrative privileges');
      return false;
    }
  }, [handleExitToSignOn]);

  // =============================================================================
  // DATE/TIME FORMATTING
  // =============================================================================

  /**
   * Format current date for display
   * Maps to COBOL POPULATE-HEADER-INFO paragraph date formatting
   * Returns MM/DD/YY format matching BMS CURDATE field
   */
  const formatCurrentDate = useCallback(() => {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const year = String(now.getFullYear()).slice(-2);
    return `${month}/${day}/${year}`;
  }, []);

  /**
   * Format current time for display
   * Maps to COBOL POPULATE-HEADER-INFO paragraph time formatting
   * Returns HH:MM:SS format matching BMS CURTIME field
   */
  const formatCurrentTime = useCallback(() => {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
  }, []);

  // =============================================================================
  // NAVIGATION AND MENU HANDLING
  // =============================================================================

  /**
   * Handle option selection and navigation
   * Maps to COBOL PROCESS-ENTER-KEY paragraph logic
   */
  const handleOptionSelection = useCallback(async (values, { setSubmitting, setFieldError }) => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const selectedOption = parseInt(values.option, 10);
      const menuOption = ADMIN_MENU_OPTIONS.find(opt => parseInt(opt.num, 10) === selectedOption);

      if (!menuOption) {
        setFieldError('option', 'Please enter a valid option number...');
        return;
      }

      // Check if this is a "DUMMY" program (coming soon functionality)
      if (menuOption.pgmName.startsWith('DUMMY')) {
        setErrorMessage(`This option ${menuOption.name} is coming soon ...`);
        return;
      }

      // Navigate to the selected option
      // Maps to COBOL EXEC CICS XCTL logic
      if (menuOption.route) {
        navigate(menuOption.route);
      } else {
        setErrorMessage(`This option ${menuOption.name} is coming soon ...`);
      }

    } catch (error) {
      console.error('Error processing option selection:', error);
      setErrorMessage('Error processing your selection. Please try again.');
    } finally {
      setIsLoading(false);
      setSubmitting(false);
    }
  }, [navigate]);

  /**
   * Handle PF-key navigation
   * Maps to COBOL key processing logic from MAIN-PARA
   */
  const handleKeyDown = useCallback((event) => {
    switch (event.key) {
      case 'F3':
        event.preventDefault();
        handleExitToSignOn();
        break;
      case 'Enter':
        // Allow form to handle Enter key naturally
        break;
      default:
        // Other function keys not supported, show error
        if (event.key.startsWith('F')) {
          event.preventDefault();
          setErrorMessage('Invalid key pressed. Please use Enter to continue or F3 to exit.');
        }
        break;
    }
  }, [handleExitToSignOn]);

  // =============================================================================
  // COMPONENT LIFECYCLE
  // =============================================================================

  /**
   * Component initialization and role verification
   * Maps to COBOL MAIN-PARA initial processing logic
   */
  useEffect(() => {
    const initializeAdminMenu = async () => {
      // Verify admin access on component mount
      await verifyAdminAccess();
    };

    initializeAdminMenu();
  }, [verifyAdminAccess]);

  /**
   * Set up global keyboard event listeners
   * Maps to CICS attention key handling
   */
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  // =============================================================================
  // RENDER COMPONENT
  // =============================================================================

  return (
    <div style={{
      fontFamily: 'monospace',
      backgroundColor: '#000000',
      color: '#00ff00',
      padding: '10px',
      minHeight: '100vh',
      width: '80ch',
      margin: '0 auto',
    }}>
      {/* Header Section - Maps to BMS header fields */}
      <div style={{ marginBottom: '20px' }}>
        {/* Line 1: Transaction, Title, Date */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
          <span style={{ color: '#4a9eff' }}>
            Tran: <span style={{ color: '#4a9eff' }}>{TRANSACTION_ID}</span>
          </span>
          <span style={{ color: '#ffff00', textAlign: 'center', flex: 1 }}>
            CREDIT CARD DEMO
          </span>
          <span style={{ color: '#4a9eff' }}>
            Date: <span style={{ color: '#4a9eff' }}>{formatCurrentDate()}</span>
          </span>
        </div>

        {/* Line 2: Program, Title Line 2, Time */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
          <span style={{ color: '#4a9eff' }}>
            Prog: <span style={{ color: '#4a9eff' }}>{PROGRAM_NAME}</span>
          </span>
          <span style={{ color: '#ffff00', textAlign: 'center', flex: 1 }}>
            {/* Empty for second title line */}
          </span>
          <span style={{ color: '#4a9eff' }}>
            Time: <span style={{ color: '#4a9eff' }}>{formatCurrentTime()}</span>
          </span>
        </div>

        {/* Separator line */}
        <div style={{ borderTop: '1px solid #ffffff', margin: '10px 0' }} />

        {/* Screen Title */}
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <span style={{ color: '#ffffff', fontWeight: 'bold' }}>{SCREEN_TITLE}</span>
        </div>
      </div>

      {/* Menu Options Section - Maps to OPTN001-OPTN012 BMS fields */}
      <div style={{ marginBottom: '30px', marginLeft: '20ch' }}>
        {ADMIN_MENU_OPTIONS.map((option) => (
          <div key={option.num} style={{ marginBottom: '5px' }}>
            <span style={{ color: '#4a9eff' }}>
              {option.num}. {option.name}
            </span>
          </div>
        ))}
      </div>

      {/* Option Selection Form - Maps to BMS OPTION field and COBOL form processing */}
      <Formik
        initialValues={{ option: '' }}
        validationSchema={validationSchema}
        onSubmit={handleOptionSelection}
        validateOnChange
        validateOnBlur
      >
        {({ values, errors, touched, handleChange, handleBlur, handleSubmit }) => (
          <form onSubmit={handleSubmit}>
            <div style={{ marginLeft: '15ch', marginBottom: '20px' }}>
              <span style={{ color: '#40e0d0' }}>Please select an option : </span>
              <TextField
                name="option"
                type="text"
                variant="standard"
                inputProps={{
                  maxLength: 2,
                  style: {
                    color: '#00ff00',
                    backgroundColor: 'transparent',
                    borderBottom: '1px solid #00ff00',
                    fontFamily: 'monospace',
                    width: '3ch',
                    textAlign: 'right',
                  },
                }}
                value={values.option}
                onChange={handleChange}
                onBlur={handleBlur}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleSubmit();
                  }
                }}
                error={touched.option && Boolean(errors.option)}
                helperText={touched.option && errors.option ? errors.option : ''}
                disabled={isLoading}
                sx={{
                  '& .MuiInput-underline:before': {
                    borderBottomColor: '#00ff00',
                  },
                  '& .MuiInput-underline:hover:not(.Mui-disabled):before': {
                    borderBottomColor: '#00ff00',
                  },
                  '& .MuiInput-underline:after': {
                    borderBottomColor: '#00ff00',
                  },
                  '& .MuiFormHelperText-root': {
                    color: '#ff0000',
                    fontFamily: 'monospace',
                  },
                }}
              />
            </div>
          </form>
        )}
      </Formik>

      {/* Error Message Area - Maps to BMS ERRMSG field */}
      {errorMessage && (
        <div style={{
          marginTop: '20px',
          color: '#ff0000',
          fontWeight: 'bold',
          marginBottom: '10px',
        }}>
          {errorMessage}
        </div>
      )}

      {/* Footer Section - Maps to BMS footer instructions */}
      <div style={{
        position: 'fixed',
        bottom: '10px',
        left: '50%',
        transform: 'translateX(-50%)',
        color: '#ffff00',
      }}>
        ENTER=Continue  F3=Exit
      </div>
    </div>
  );
};

// Default export as specified in schema
export default AdminMenu;
