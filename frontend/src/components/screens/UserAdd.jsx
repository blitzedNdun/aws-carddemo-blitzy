/**
 * UserAdd Component - React component for the Add User screen (COUSR01)
 *
 * Converts COBOL/BMS COUSR01 mapset and COUSR01C program to React component.
 * Enables new user account creation with role assignment, providing form fields
 * for user details with validation, role selection (admin/regular), initial
 * password assignment, and security setup. Enforces administrative access requirements.
 *
 * Maps BMS fields to React form elements:
 * - FNAME (First Name): 20 chars max, initial cursor position
 * - LNAME (Last Name): 20 chars max
 * - USERID (User ID): 8 chars max, alphanumeric starting with letter
 * - PASSWD (Password): 8 chars, hidden input
 * - USRTYPE (User Type): A=Admin, U=User dropdown selection
 *
 * Replicates COBOL transaction processing:
 * - Enter: Validate and create user (PROCESS-ENTER-KEY)
 * - F3: Return to admin menu (COADM01C)
 * - F4: Clear form (CLEAR-CURRENT-SCREEN)
 * - F12: Exit application
 * - Other keys: Invalid key error
 */

import {
  TextField,
  Button,
  Select,
  FormControl,
  Box,
  Alert,
  MenuItem,
  InputLabel,
} from '@mui/material';
import { Formik } from 'formik';
import { useState, useEffect } from 'react';
import * as yup from 'yup';

// Internal imports - ONLY from depends_on_files
import Header from '../../components/common/Header.jsx';
import { createUser } from '../../services/api.js';
import { validateUserID } from '../../utils/validation.js';

/**
 * UserAdd Component
 * Default export function component that handles user creation form
 * Implements complete COUSR01 BMS screen functionality with modern React patterns
 */
const UserAdd = () => {
  // State management for form submission and error handling
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState('');
  const [messageType, setMessageType] = useState('info'); // 'success', 'error', 'info'
  const [hasAdminAccess, setHasAdminAccess] = useState(true); // Initialize as true, should verify on mount

  // Verify admin privileges on component mount
  useEffect(() => {
    const checkAdminAccess = () => {
      try {
        // Check user role from session storage (set during sign-in)
        const userRole = sessionStorage.getItem('userRole');

        if (!userRole || userRole.toLowerCase() !== 'admin') {
          setHasAdminAccess(false);
          setSubmitMessage('Access denied - Administrative privileges required');
          setMessageType('error');
        } else {
          setHasAdminAccess(true);
        }
      } catch (error) {
        console.error('Error checking admin access:', error);
        setHasAdminAccess(false);
        setSubmitMessage('Access verification failed - Please sign in again');
        setMessageType('error');
      }
    };

    checkAdminAccess();
  }, []);

  // Initial form values matching BMS field defaults
  const initialValues = {
    firstName: '',
    lastName: '',
    userID: '',
    password: '',
    passwordConfirm: '',
    userType: '',
  };

  // Yup validation schema matching COBOL validation rules from COUSR01C
  const validationSchema = yup.object({
    firstName: yup
      .string()
      .required('First Name can NOT be empty...')
      .max(20, 'First Name cannot exceed 20 characters'),

    lastName: yup
      .string()
      .required('Last Name can NOT be empty...')
      .max(20, 'Last Name cannot exceed 20 characters'),

    userID: yup
      .string()
      .required('User ID can NOT be empty...')
      .max(8, 'User ID cannot exceed 8 characters')
      .test('user-id-format', 'User ID must start with a letter and contain only letters and numbers', (value) => {
        if (!value) {
          return false;
        }
        const validation = validateUserID(value);
        return validation.isValid;
      }),

    password: yup
      .string()
      .required('Password can NOT be empty...')
      .min(8, 'Password must be exactly 8 characters')
      .max(8, 'Password must be exactly 8 characters')
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Password must contain at least one uppercase letter, one lowercase letter, and one number'),

    passwordConfirm: yup
      .string()
      .required('Password confirmation is required')
      .oneOf([yup.ref('password'), null], 'Passwords must match'),

    userType: yup
      .string()
      .required('User Type can NOT be empty...')
      .oneOf(['A', 'U'], 'User Type must be A (Admin) or U (User)'),
  });

  // Handle keyboard events for PF key functionality (F3, F4, F12)
  useEffect(() => {
    const handleKeyDown = (event) => {
      switch (event.key) {
        case 'F3':
          event.preventDefault();
          handleBack();
          break;
        case 'F4':
          event.preventDefault();
          handleClear();
          break;
        case 'F12':
          event.preventDefault();
          handleExit();
          break;
        default:
          // Other function keys trigger invalid key error
          if (event.key.startsWith('F') && event.key !== 'F3' && event.key !== 'F4' && event.key !== 'F12') {
            event.preventDefault();
            setSubmitMessage('Invalid key pressed. Use ENTER, F3, F4, or F12.');
            setMessageType('error');
          }
          break;
      }
    };

    // Add keyboard event listener
    document.addEventListener('keydown', handleKeyDown);

    // Cleanup event listener on unmount
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  /**
   * Handle form submission - Maps to COBOL PROCESS-ENTER-KEY
   * Validates all fields and creates new user via API
   */
  const handleSubmit = async (values, { setFieldError, resetForm }) => {
    // Check admin access before processing
    if (!hasAdminAccess) {
      setSubmitMessage('Access denied - Administrative privileges required');
      setMessageType('error');
      return;
    }

    setIsSubmitting(true);
    setSubmitMessage('');
    setMessageType('info');

    try {
      // Prepare user data for API call
      const userData = {
        userId: values.userID.toUpperCase(), // Convert to uppercase for mainframe compatibility
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        password: values.password,
        role: values.userType === 'A' ? 'ADMIN' : 'USER',
        email: `${values.userID.toLowerCase()}@company.com`, // Generate default email
        status: 'ACTIVE',
      };

      // Call API to create user
      const response = await createUser(userData);

      if (response.success) {
        // Success - replicate COBOL success message pattern
        setSubmitMessage(`User ${values.userID.toUpperCase()} has been added ...`);
        setMessageType('success');

        // Clear form after successful creation (INITIALIZE-ALL-FIELDS)
        resetForm();
      } else {
        // Handle API errors
        if (response.error.includes('already exist') || response.error.includes('duplicate')) {
          setSubmitMessage('User ID already exist...');
          setFieldError('userID', 'User ID already exist...');
        } else {
          setSubmitMessage(response.error || 'Unable to Add User...');
        }
        setMessageType('error');
      }
    } catch (error) {
      // Handle unexpected errors
      console.error('Error creating user:', error);
      setSubmitMessage('Unable to Add User...');
      setMessageType('error');
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * Handle F3 key - Return to admin menu
   * Maps to COBOL DFHPF3 logic returning to COADM01C
   */
  const handleBack = () => {
    // In a real application, this would navigate to the admin menu
    // For now, we'll navigate back in browser history
    if (window.history.length > 1) {
      window.history.back();
    } else {
      // Fallback to admin menu route
      window.location.href = '/admin';
    }
  };

  /**
   * Handle F4 key - Clear form
   * Maps to COBOL CLEAR-CURRENT-SCREEN logic
   */
  const handleClear = () => {
    // Clear messages
    setSubmitMessage('');
    setMessageType('info');

    // Trigger form reset via custom event
    const clearEvent = new CustomEvent('clearForm');
    document.dispatchEvent(clearEvent);
  };

  /**
   * Handle F12 key - Exit application
   * Maps to COBOL exit logic
   */
  const handleExit = () => {
    // In a real application, this would return to sign-on screen
    // For now, navigate to home page
    window.location.href = '/';
  };

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        fontFamily: 'monospace',
        backgroundColor: '#000000',
        color: '#00FF00',
      }}
    >
      {/* Header matching BMS layout */}
      <Header
        transactionId="CU01"
        programName="COUSR01C"
        title="Add User"
      />

      {/* Main content area */}
      <Box sx={{ flex: 1, padding: '16px' }}>
        <Formik
          initialValues={initialValues}
          validationSchema={validationSchema}
          onSubmit={handleSubmit}
          enableReinitialize
        >
          {({ values, errors, touched, handleChange, handleBlur, handleSubmit, resetForm }) => {
            // Move form reset handling to a separate component to avoid hooks violation
            const FormContent = () => {
              useEffect(() => {
                const handleClearEvent = () => {
                  resetForm();
                };

                document.addEventListener('clearForm', handleClearEvent);
                return () => {
                  document.removeEventListener('clearForm', handleClearEvent);
                };
              }, [resetForm]);

              return null;
            };

            return (
              <Box>
                <FormContent />
                <form onSubmit={handleSubmit}>
                  <Box
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: '1fr',
                      gap: '16px',
                      maxWidth: '800px',
                      margin: '0 auto',
                    }}
                  >
                    {/* Title centered on screen */}
                    <Box sx={{ textAlign: 'center', marginBottom: '24px' }}>
                      <Box
                        sx={{
                          color: '#FFFFFF',
                          fontSize: '16px',
                          fontWeight: 'bold',
                          fontFamily: 'monospace',
                        }}
                      >
                        Add User
                      </Box>
                    </Box>

                    {/* Form fields in grid layout matching BMS positions */}
                    <Box
                      sx={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: '16px',
                        marginBottom: '24px',
                      }}
                    >
                      {/* First Name - Left side, initial cursor */}
                      <FormControl fullWidth>
                        <TextField
                          name="firstName"
                          label="First Name"
                          value={values.firstName}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          error={touched.firstName && Boolean(errors.firstName)}
                          helperText={touched.firstName && errors.firstName}
                          autoFocus // IC attribute - initial cursor
                          disabled={!hasAdminAccess}
                          inputProps={{
                            maxLength: 20,
                            style: {
                              fontFamily: 'monospace',
                              backgroundColor: hasAdminAccess ? '#1a1a1a' : '#333333',
                              color: hasAdminAccess ? '#00FF00' : '#666666',
                            },
                          }}
                          sx={{
                            '& .MuiInputLabel-root': {
                              color: '#4FC3F7', // Turquoise matching BMS
                            },
                            '& .MuiOutlinedInput-root': {
                              '& fieldset': {
                                borderColor: '#4FC3F7',
                              },
                              '&:hover fieldset': {
                                borderColor: '#00FF00',
                              },
                            },
                          }}
                        />
                      </FormControl>

                      {/* Last Name - Right side */}
                      <FormControl fullWidth>
                        <TextField
                          name="lastName"
                          label="Last Name"
                          value={values.lastName}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          error={touched.lastName && Boolean(errors.lastName)}
                          helperText={touched.lastName && errors.lastName}
                          disabled={!hasAdminAccess}
                          inputProps={{
                            maxLength: 20,
                            style: {
                              fontFamily: 'monospace',
                              backgroundColor: hasAdminAccess ? '#1a1a1a' : '#333333',
                              color: hasAdminAccess ? '#00FF00' : '#666666',
                            },
                          }}
                          sx={{
                            '& .MuiInputLabel-root': {
                              color: '#4FC3F7',
                            },
                            '& .MuiOutlinedInput-root': {
                              '& fieldset': {
                                borderColor: '#4FC3F7',
                              },
                              '&:hover fieldset': {
                                borderColor: '#00FF00',
                              },
                            },
                          }}
                        />
                      </FormControl>
                    </Box>

                    {/* User ID and Password row */}
                    <Box
                      sx={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: '16px',
                        marginBottom: '24px',
                      }}
                    >
                      {/* User ID - Left side with hint */}
                      <FormControl fullWidth>
                        <TextField
                          name="userID"
                          label="User ID"
                          value={values.userID}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          error={touched.userID && Boolean(errors.userID)}
                          helperText={touched.userID && errors.userID ? errors.userID : '(8 Char)'}
                          disabled={!hasAdminAccess}
                          inputProps={{
                            maxLength: 8,
                            style: {
                              fontFamily: 'monospace',
                              backgroundColor: hasAdminAccess ? '#1a1a1a' : '#333333',
                              color: hasAdminAccess ? '#00FF00' : '#666666',
                              textTransform: 'uppercase',
                            },
                          }}
                          sx={{
                            '& .MuiInputLabel-root': {
                              color: '#4FC3F7',
                            },
                            '& .MuiOutlinedInput-root': {
                              '& fieldset': {
                                borderColor: '#4FC3F7',
                              },
                              '&:hover fieldset': {
                                borderColor: '#00FF00',
                              },
                            },
                          }}
                        />
                      </FormControl>

                      {/* Password - Right side with hint */}
                      <FormControl fullWidth>
                        <TextField
                          name="password"
                          label="Password"
                          type="password"
                          value={values.password}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          error={touched.password && Boolean(errors.password)}
                          helperText={touched.password && errors.password ? errors.password : '(8 Char)'}
                          disabled={!hasAdminAccess}
                          inputProps={{
                            maxLength: 8,
                            style: {
                              fontFamily: 'monospace',
                              backgroundColor: hasAdminAccess ? '#1a1a1a' : '#333333',
                              color: hasAdminAccess ? '#00FF00' : '#666666',
                            },
                          }}
                          sx={{
                            '& .MuiInputLabel-root': {
                              color: '#4FC3F7',
                            },
                            '& .MuiOutlinedInput-root': {
                              '& fieldset': {
                                borderColor: '#4FC3F7',
                              },
                              '&:hover fieldset': {
                                borderColor: '#00FF00',
                              },
                            },
                          }}
                        />
                      </FormControl>
                    </Box>

                    {/* Password Confirmation and User Type row */}
                    <Box
                      sx={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: '16px',
                        marginBottom: '24px',
                      }}
                    >
                      {/* Password Confirmation */}
                      <FormControl fullWidth>
                        <TextField
                          name="passwordConfirm"
                          label="Confirm Password"
                          type="password"
                          value={values.passwordConfirm}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          error={touched.passwordConfirm && Boolean(errors.passwordConfirm)}
                          helperText={touched.passwordConfirm && errors.passwordConfirm}
                          disabled={!hasAdminAccess}
                          inputProps={{
                            maxLength: 8,
                            style: {
                              fontFamily: 'monospace',
                              backgroundColor: hasAdminAccess ? '#1a1a1a' : '#333333',
                              color: hasAdminAccess ? '#00FF00' : '#666666',
                            },
                          }}
                          sx={{
                            '& .MuiInputLabel-root': {
                              color: '#4FC3F7',
                            },
                            '& .MuiOutlinedInput-root': {
                              '& fieldset': {
                                borderColor: '#4FC3F7',
                              },
                              '&:hover fieldset': {
                                borderColor: '#00FF00',
                              },
                            },
                          }}
                        />
                      </FormControl>

                      {/* User Type - Dropdown with hint */}
                      <FormControl fullWidth error={touched.userType && Boolean(errors.userType)}>
                        <InputLabel
                          sx={{
                            color: '#4FC3F7',
                            '&.Mui-focused': {
                              color: '#00FF00',
                            },
                          }}
                        >
                        User Type
                        </InputLabel>
                        <Select
                          name="userType"
                          value={values.userType}
                          onChange={handleChange}
                          onBlur={handleBlur}
                          disabled={!hasAdminAccess}
                          sx={{
                            fontFamily: 'monospace',
                            backgroundColor: hasAdminAccess ? '#1a1a1a' : '#333333',
                            color: hasAdminAccess ? '#00FF00' : '#666666',
                            '& .MuiOutlinedInput-notchedOutline': {
                              borderColor: '#4FC3F7',
                            },
                            '&:hover .MuiOutlinedInput-notchedOutline': {
                              borderColor: hasAdminAccess ? '#00FF00' : '#4FC3F7',
                            },
                            '& .MuiSvgIcon-root': {
                              color: hasAdminAccess ? '#4FC3F7' : '#666666',
                            },
                          }}
                        >
                          <MenuItem value="">Select...</MenuItem>
                          <MenuItem value="A">A - Admin</MenuItem>
                          <MenuItem value="U">U - User</MenuItem>
                        </Select>
                        {touched.userType && errors.userType && (
                          <Box sx={{ color: '#f44336', fontSize: '0.75rem', marginTop: '4px' }}>
                            {errors.userType}
                          </Box>
                        )}
                        <Box sx={{ color: '#4FC3F7', fontSize: '0.75rem', marginTop: '4px' }}>
                        (A=Admin, U=User)
                        </Box>
                      </FormControl>
                    </Box>

                    {/* Submit button area */}
                    <Box sx={{ textAlign: 'center', marginTop: '32px' }}>
                      <Button
                        type="submit"
                        disabled={isSubmitting || !hasAdminAccess}
                        sx={{
                          backgroundColor: hasAdminAccess ? '#4FC3F7' : '#666666',
                          color: hasAdminAccess ? '#000000' : '#999999',
                          fontFamily: 'monospace',
                          fontWeight: 'bold',
                          padding: '8px 24px',
                          '&:hover': {
                            backgroundColor: hasAdminAccess ? '#00FF00' : '#666666',
                          },
                          '&:disabled': {
                            backgroundColor: '#666666',
                            color: '#999999',
                          },
                        }}
                      >
                        {isSubmitting ? 'Creating User...' : hasAdminAccess ? 'Add User (ENTER)' : 'Access Denied'}
                      </Button>
                    </Box>

                    {/* Additional action buttons */}
                    <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'center',
                        gap: '16px',
                        marginTop: '16px',
                      }}
                    >
                      <Button
                        type="button"
                        onClick={handleBack}
                        sx={{
                          backgroundColor: '#666666',
                          color: '#FFFFFF',
                          fontFamily: 'monospace',
                          padding: '6px 16px',
                          '&:hover': {
                            backgroundColor: '#888888',
                          },
                        }}
                      >
                      Back (F3)
                      </Button>

                      <Button
                        type="button"
                        onClick={handleClear}
                        sx={{
                          backgroundColor: '#666666',
                          color: '#FFFFFF',
                          fontFamily: 'monospace',
                          padding: '6px 16px',
                          '&:hover': {
                            backgroundColor: '#888888',
                          },
                        }}
                      >
                      Clear (F4)
                      </Button>

                      <Button
                        type="button"
                        onClick={handleExit}
                        sx={{
                          backgroundColor: '#666666',
                          color: '#FFFFFF',
                          fontFamily: 'monospace',
                          padding: '6px 16px',
                          '&:hover': {
                            backgroundColor: '#888888',
                          },
                        }}
                      >
                      Exit (F12)
                      </Button>
                    </Box>
                  </Box>
                </form>
              </Box>
            );
          }}
        </Formik>
      </Box>

      {/* Error/Success message area - matches BMS ERRMSG field */}
      {submitMessage && (
        <Box sx={{ padding: '8px 16px' }}>
          <Alert
            severity={messageType}
            sx={{
              fontFamily: 'monospace',
              backgroundColor: messageType === 'error' ? '#ffebee' : messageType === 'success' ? '#e8f5e8' : '#e3f2fd',
              color: messageType === 'error' ? '#d32f2f' : messageType === 'success' ? '#2e7d32' : '#1976d2',
              '& .MuiAlert-icon': {
                color: messageType === 'error' ? '#d32f2f' : messageType === 'success' ? '#2e7d32' : '#1976d2',
              },
            }}
          >
            {submitMessage}
          </Alert>
        </Box>
      )}

      {/* Function keys area - matches BMS line 24 */}
      <Box
        sx={{
          backgroundColor: '#2a2a2a',
          padding: '8px 16px',
          borderTop: '1px solid #4FC3F7',
          fontFamily: 'monospace',
          color: '#FFEB3B', // Yellow for function keys
          fontSize: '14px',
          textAlign: 'center',
        }}
      >
        ENTER=Add User  F3=Back  F4=Clear  F12=Exit
      </Box>
    </Box>
  );
};

export default UserAdd;
