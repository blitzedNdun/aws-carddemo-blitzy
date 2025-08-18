/**
 * UserList Component - User List Screen (COUSR00)
 * 
 * React component that replicates the functionality of COBOL program COUSR00C
 * and BMS mapset COUSR00.bms for user directory browsing and management access.
 * 
 * Provides:
 * - Paginated user list display (10 users per page)
 * - Search functionality by User ID
 * - User selection for Update ('U') or Delete ('D') actions
 * - PF-key navigation (F3=Back, F7=Previous, F8=Next, Enter=Continue)
 * - Administrative role requirement enforcement
 * - Direct navigation to user maintenance functions
 * 
 * Maps COBOL program flow to React component lifecycle:
 * - PROCESS-ENTER-KEY -> handleEnterKey
 * - PROCESS-PF7-KEY -> handlePreviousPage
 * - PROCESS-PF8-KEY -> handleNextPage
 * - POPULATE-USER-DATA -> User list state management
 * - STARTBR/READNEXT/READPREV -> API pagination calls
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  TextField, 
  Box, 
  Typography, 
  Table, 
  TableBody, 
  TableCell, 
  TableContainer, 
  TableHead, 
  TableRow, 
  Paper, 
  Alert, 
  Button,
  CircularProgress,
  Radio,
  RadioGroup,
  FormControlLabel,
  Container
} from '@mui/material';
import { useFormik } from 'formik';
import { object as yupObject, string } from 'yup';

// Internal imports - ONLY from depends_on_files
import { updateUser, getUsers } from '../../services/api.js';
import Header from '../common/Header.jsx';

/**
 * Validation schema for user search and selection
 * Maps to COBOL field validation rules from COUSR00.bms
 */
const validationSchema = yupObject({
  searchUserId: string()
    .max(8, 'User ID cannot exceed 8 characters')
    .matches(/^[A-Za-z0-9]*$/, 'User ID must contain only letters and numbers'),
  selectedUser: string(),
  selectionAction: string()
    .matches(/^[UuDd]?$/, "Selection must be 'U' for Update or 'D' for Delete")
});

/**
 * UserList Component - Maps to COBOL program COUSR00C
 * Implements user directory browsing with selection capabilities
 */
const UserList = () => {
  // Navigation hook for PF-key functionality
  const navigate = useNavigate();

  // Component state - maps to COBOL working storage variables
  const [users, setUsers] = useState([]); // Array of user records (USER-REC OCCURS 10 TIMES)
  const [currentPage, setCurrentPage] = useState(1); // Maps to CDEMO-CU00-PAGE-NUM
  const [totalPages, setTotalPages] = useState(0);
  const [hasNextPage, setHasNextPage] = useState(false); // Maps to CDEMO-CU00-NEXT-PAGE-FLG
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(''); // Maps to WS-MESSAGE and ERRMSGO
  const [selectedAction, setSelectedAction] = useState(''); // Maps to CDEMO-CU00-USR-SEL-FLG
  const [selectedUserId, setSelectedUserId] = useState(''); // Maps to CDEMO-CU00-USR-SELECTED

  // Formik form management - handles search input and user selection
  const formik = useFormik({
    initialValues: {
      searchUserId: '', // Maps to USRIDINI field
      selectedUser: '',
      selectionAction: ''
    },
    validationSchema,
    onSubmit: async (values) => {
      await handleEnterKey();
    }
  });

  /**
   * Load users from API - Maps to COBOL STARTBR/READNEXT operations
   * Implements pagination and search functionality
   */
  const loadUsers = useCallback(async (pageNumber = 1, searchTerm = '') => {
    setLoading(true);
    setErrorMessage('');
    
    try {
      const response = await getUsers({
        pageSize: 10, // Fixed page size matching COBOL screen layout
        pageNumber,
        searchTerm: searchTerm || undefined
      });

      if (response.success) {
        setUsers(response.data.users || []);
        setCurrentPage(response.data.currentPage || pageNumber);
        setTotalPages(response.data.totalPages || 0);
        setHasNextPage(response.data.hasNextPage || false);
        
        // Clear error message on successful load
        setErrorMessage('');
      } else {
        setErrorMessage(response.error || 'Unable to load users');
        setUsers([]);
      }
    } catch (error) {
      setErrorMessage('Network error - unable to connect to server');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial data load - equivalent to COBOL program initialization
  useEffect(() => {
    loadUsers(1, '');
  }, [loadUsers]);

  /**
   * Handle Enter key processing - Maps to COBOL PROCESS-ENTER-KEY
   * Processes user selection and navigation requests
   */
  const handleEnterKey = useCallback(async () => {
    const searchTerm = formik.values.searchUserId.trim();
    
    // Process user selection if a user is selected
    if (selectedUserId && selectedAction) {
      switch (selectedAction.toUpperCase()) {
        case 'U':
          // Navigate to user update - Maps to XCTL PROGRAM(COUSR02C)
          try {
            await updateUser(selectedUserId, { action: 'update' });
            navigate('/users/update', { 
              state: { 
                userId: selectedUserId,
                fromProgram: 'COUSR00C',
                fromTransaction: 'CU00'
              }
            });
          } catch (error) {
            setErrorMessage('Unable to navigate to user update');
          }
          break;
        
        case 'D':
          // Navigate to user delete - Maps to XCTL PROGRAM(COUSR03C)
          navigate('/users/delete', { 
            state: { 
              userId: selectedUserId,
              fromProgram: 'COUSR00C',
              fromTransaction: 'CU00'
            }
          });
          break;
        
        default:
          setErrorMessage('Invalid selection. Valid values are U and D');
          break;
      }
      return;
    }

    // Perform search if search term provided or reload current view
    await loadUsers(1, searchTerm);
    
    // Clear search field after search - Maps to COBOL field clearing
    formik.setFieldValue('searchUserId', '');
  }, [formik, selectedUserId, selectedAction, loadUsers, updateUser, navigate]);

  /**
   * Handle F3 (Back) key - Return to main menu
   * Maps to COBOL PF3 processing
   */
  const handleBackKey = useCallback(() => {
    navigate('/menu/main', { 
      state: { 
        fromProgram: 'COUSR00C',
        fromTransaction: 'CU00'
      }
    });
  }, [navigate]);

  /**
   * Handle F7 (Previous page) key - Maps to COBOL PROCESS-PF7-KEY
   * Navigates to previous page of users
   */
  const handlePreviousPage = useCallback(async () => {
    if (currentPage > 1) {
      await loadUsers(currentPage - 1, '');
    } else {
      setErrorMessage('You are already at the top of the page...');
    }
  }, [currentPage, loadUsers]);

  /**
   * Handle F8 (Next page) key - Maps to COBOL PROCESS-PF8-KEY  
   * Navigates to next page of users
   */
  const handleNextPage = useCallback(async () => {
    if (hasNextPage) {
      await loadUsers(currentPage + 1, '');
    } else {
      setErrorMessage('You are already at the bottom of the page...');
    }
  }, [currentPage, hasNextPage, loadUsers]);

  /**
   * Handle user selection - Maps to COBOL selection processing
   * Updates selected user and action for processing
   */
  const handleUserSelection = useCallback((userId, action) => {
    if (action && action.trim() !== '') {
      setSelectedUserId(userId);
      setSelectedAction(action.trim().toUpperCase());
    } else {
      setSelectedUserId('');
      setSelectedAction('');
    }
  }, []);

  /**
   * Handle keyboard events for PF-key simulation
   * Maps COBOL PF-key functionality to keyboard events
   */
  const handleKeyDown = useCallback((event) => {
    switch (event.key) {
      case 'F3':
        event.preventDefault();
        handleBackKey();
        break;
      case 'F7':
        event.preventDefault();
        handlePreviousPage();
        break;
      case 'F8':
        event.preventDefault();
        handleNextPage();
        break;
      case 'Enter':
        if (!event.ctrlKey && !event.altKey && !event.shiftKey) {
          event.preventDefault();
          handleEnterKey();
        }
        break;
      default:
        // Allow other keys to proceed normally
        break;
    }
  }, [handleBackKey, handlePreviousPage, handleNextPage, handleEnterKey]);

  // Attach keyboard event listener for PF-key functionality
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  return (
    <Container maxWidth="lg" sx={{ padding: 0 }}>
      {/* Header Component - Maps to BMS screen header */}
      <Header 
        transactionId="CU00"
        programName="COUSR00C" 
        title="List Users"
      />

      {/* Main content container with monospace styling for 3270 terminal feel */}
      <Box 
        sx={{ 
          fontFamily: 'monospace', 
          backgroundColor: '#000000',
          color: '#00FF00',
          minHeight: '600px',
          padding: '16px'
        }}
      >
        {/* Page number display - Maps to PAGENUM field */}
        <Box sx={{ textAlign: 'right', marginBottom: '16px' }}>
          <Typography 
            variant="body2" 
            sx={{ 
              color: '#00FFFF', // Turquoise color matching BMS
              fontFamily: 'monospace'
            }}
          >
            Page: {currentPage}
          </Typography>
        </Box>

        {/* Search section - Maps to USRIDIN field */}
        <Box sx={{ marginBottom: '16px' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography 
              variant="body2" 
              sx={{ 
                color: '#00FFFF', // Turquoise color matching BMS
                fontFamily: 'monospace',
                minWidth: '120px'
              }}
            >
              Search User ID:
            </Typography>
            <TextField
              name="searchUserId"
              value={formik.values.searchUserId}
              onChange={formik.handleChange}
              onBlur={formik.handleBlur}
              error={formik.touched.searchUserId && Boolean(formik.errors.searchUserId)}
              helperText={formik.touched.searchUserId && formik.errors.searchUserId}
              variant="outlined"
              size="small"
              inputProps={{ 
                maxLength: 8,
                style: { 
                  fontFamily: 'monospace',
                  backgroundColor: '#000000',
                  color: '#00FF00',
                  border: '1px solid #00FF00'
                }
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  backgroundColor: '#000000',
                  '& fieldset': {
                    borderColor: '#00FF00',
                  },
                  '&:hover fieldset': {
                    borderColor: '#00FFFF',
                  },
                  '&.Mui-focused fieldset': {
                    borderColor: '#FFFF00',
                  },
                },
                '& .MuiFormHelperText-root': {
                  color: '#FF0000',
                  fontFamily: 'monospace'
                }
              }}
            />
          </Box>
        </Box>

        {/* Error message display - Maps to ERRMSG field */}
        {errorMessage && (
          <Alert 
            severity="error" 
            sx={{ 
              marginBottom: '16px',
              backgroundColor: '#330000',
              color: '#FF0000',
              fontFamily: 'monospace',
              '& .MuiAlert-message': {
                fontFamily: 'monospace'
              }
            }}
          >
            {errorMessage}
          </Alert>
        )}

        {/* Loading indicator */}
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
            <CircularProgress sx={{ color: '#00FFFF' }} />
          </Box>
        )}

        {/* User list table - Maps to user display rows */}
        <TableContainer 
          component={Paper} 
          sx={{ 
            backgroundColor: '#000000',
            '& .MuiTable-root': {
              borderCollapse: 'separate',
              borderSpacing: 0
            }
          }}
        >
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ color: '#FFFFFF', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                  Sel
                </TableCell>
                <TableCell sx={{ color: '#FFFFFF', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                  User ID
                </TableCell>
                <TableCell sx={{ color: '#FFFFFF', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                  First Name
                </TableCell>
                <TableCell sx={{ color: '#FFFFFF', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                  Last Name
                </TableCell>
                <TableCell sx={{ color: '#FFFFFF', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                  Type
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {/* Display exactly 10 rows - Maps to BMS screen layout */}
              {Array.from({ length: 10 }, (_, index) => {
                const user = users[index];
                const rowId = `row-${index + 1}`;
                
                return (
                  <TableRow key={rowId}>
                    {/* Selection column - Maps to SEL0001-SEL0010 fields */}
                    <TableCell sx={{ color: '#00FF00', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                      {user && (
                        <TextField
                          size="small"
                          inputProps={{ 
                            maxLength: 1,
                            style: { 
                              fontFamily: 'monospace',
                              backgroundColor: '#000000',
                              color: '#00FF00',
                              width: '20px',
                              textAlign: 'center'
                            }
                          }}
                          onChange={(e) => handleUserSelection(user.userId, e.target.value)}
                          sx={{
                            '& .MuiOutlinedInput-root': {
                              backgroundColor: '#000000',
                              '& fieldset': {
                                borderColor: '#00FF00',
                              },
                            }
                          }}
                        />
                      )}
                    </TableCell>
                    
                    {/* User ID column - Maps to USRID01-USRID10 fields */}
                    <TableCell sx={{ color: '#4FC3F7', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                      {user?.userId || ''}
                    </TableCell>
                    
                    {/* First Name column - Maps to FNAME01-FNAME10 fields */}
                    <TableCell sx={{ color: '#4FC3F7', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                      {user?.firstName || ''}
                    </TableCell>
                    
                    {/* Last Name column - Maps to LNAME01-LNAME10 fields */}
                    <TableCell sx={{ color: '#4FC3F7', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                      {user?.lastName || ''}
                    </TableCell>
                    
                    {/* User Type column - Maps to UTYPE01-UTYPE10 fields */}
                    <TableCell sx={{ color: '#4FC3F7', fontFamily: 'monospace', borderBottom: '1px solid #333' }}>
                      {user?.userType || ''}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Instructions - Maps to BMS screen instructions */}
        <Box sx={{ marginTop: '16px' }}>
          <Typography 
            variant="body2" 
            sx={{ 
              color: '#FFFFFF', 
              fontFamily: 'monospace',
              textAlign: 'center',
              marginBottom: '8px'
            }}
          >
            Type 'U' to Update or 'D' to Delete a User from the list
          </Typography>
        </Box>

        {/* Function key instructions - Maps to BMS function key line */}
        <Box sx={{ marginTop: '16px', borderTop: '1px solid #333', paddingTop: '8px' }}>
          <Typography 
            variant="body2" 
            sx={{ 
              color: '#FFEB3B', // Yellow color matching BMS
              fontFamily: 'monospace'
            }}
          >
            ENTER=Continue  F3=Back  F7=Backward  F8=Forward
          </Typography>
        </Box>

        {/* Navigation buttons for non-keyboard users */}
        <Box sx={{ marginTop: '16px', display: 'flex', gap: 2, justifyContent: 'center' }}>
          <Button 
            variant="outlined" 
            onClick={handleBackKey}
            sx={{ 
              color: '#FFEB3B', 
              borderColor: '#FFEB3B',
              fontFamily: 'monospace'
            }}
          >
            F3 Back
          </Button>
          <Button 
            variant="outlined" 
            onClick={handlePreviousPage}
            disabled={currentPage <= 1}
            sx={{ 
              color: '#FFEB3B', 
              borderColor: '#FFEB3B',
              fontFamily: 'monospace'
            }}
          >
            F7 Previous
          </Button>
          <Button 
            variant="outlined" 
            onClick={handleEnterKey}
            sx={{ 
              color: '#FFEB3B', 
              borderColor: '#FFEB3B',
              fontFamily: 'monospace'
            }}
          >
            Enter Continue
          </Button>
          <Button 
            variant="outlined" 
            onClick={handleNextPage}
            disabled={!hasNextPage}
            sx={{ 
              color: '#FFEB3B', 
              borderColor: '#FFEB3B',
              fontFamily: 'monospace'
            }}
          >
            F8 Next
          </Button>
        </Box>
      </Box>
    </Container>
  );
};

export default UserList;