/**
 * UserDelete Component - React component for the Delete User screen (COUSR03)
 * 
 * Converts BMS mapset COUSR03 to React component for user deletion functionality.
 * Provides user account deletion with two-stage confirmation workflow, displays
 * user details for verification, and maintains audit trail. Requires administrative
 * privileges and prevents self-deletion.
 * 
 * BMS Mapset: COUSR03.bms -> UserDelete.jsx
 * COBOL Program: COUSR03C.cbl -> UserDelete component logic
 * Transaction: CU03 -> User Delete functionality
 * 
 * Key Features:
 * - User ID input with validation
 * - Display user details in read-only format (ID, first name, last name, user type)
 * - Two-stage confirmation with explicit delete confirmation
 * - PF-key handlers (F3=Cancel, F5=Delete, Enter=Fetch User)
 * - Warning messages about permanent deletion
 * - Prevention of admin self-deletion
 */

// External imports
import React, { useState, useEffect } from 'react';
import { Alert } from '@mui/material';

// Internal imports - ONLY from depends_on_files
import { getUsers } from '../../services/api.js';
import Header from '../common/Header.jsx';
import { validateUserID } from '../../utils/validation.js';

/**
 * UserDelete Component - Maps to CICS transaction CU03 and BMS mapset COUSR03
 * Implements user deletion functionality with confirmation workflow
 * 
 * @returns {JSX.Element} Complete delete user screen component
 */
const UserDelete = () => {
  // Component state management matching BMS screen fields
  const [userIdInput, setUserIdInput] = useState(''); // Maps to USRIDIN field
  const [userDetails, setUserDetails] = useState({
    firstName: '',   // Maps to FNAME field
    lastName: '',    // Maps to LNAME field  
    userType: ''     // Maps to USRTYPE field
  });
  const [errorMessage, setErrorMessage] = useState(''); // Maps to ERRMSG field
  const [successMessage, setSuccessMessage] = useState('');
  const [isUserFetched, setIsUserFetched] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);
  const [currentUserId, setCurrentUserId] = useState('');

  // Get current user session data to prevent self-deletion
  useEffect(() => {
    const sessionUserId = sessionStorage.getItem('userId');
    if (sessionUserId) {
      setCurrentUserId(sessionUserId);
    }
  }, []);

  /**
   * Handle keyboard events for PF-key functionality
   * Maps BMS PF-key definitions to React keyboard handlers
   * 
   * @param {KeyboardEvent} event - Keyboard event object
   */
  const handleKeyPress = (event) => {
    // Clear any existing messages on new key press
    setErrorMessage('');
    setSuccessMessage('');

    switch (event.key) {
      case 'Enter':
        event.preventDefault();
        handleFetchUser(); // Maps to PROCESS-ENTER-KEY in COBOL
        break;
      case 'F3':
        event.preventDefault();
        handleCancel(); // Maps to PF3 logic in COBOL
        break;
      case 'F4':
        event.preventDefault();
        handleClear(); // Maps to PF4 logic (CLEAR-CURRENT-SCREEN)
        break;
      case 'F5':
        event.preventDefault();
        handleDeleteUser(); // Maps to PF5 logic (DELETE-USER-INFO)
        break;
      case 'F12':
        event.preventDefault();
        handleCancel(); // Maps to PF12 logic in COBOL
        break;
      default:
        // Invalid key - show error message matching COBOL logic
        setErrorMessage('Invalid key pressed. Use ENTER, F3, F4, F5, or F12.');
        break;
    }
  };

  /**
   * Attach keyboard event listeners for PF-key handling
   * Implements global keyboard navigation matching 3270 terminal behavior
   */
  useEffect(() => {
    const handleGlobalKeyPress = (event) => {
      // Only handle function keys globally, let ENTER be handled by forms
      if (event.key.startsWith('F') && ['F3', 'F4', 'F5', 'F12'].includes(event.key)) {
        handleKeyPress(event);
      }
    };

    // Attach global keyboard listener
    document.addEventListener('keydown', handleGlobalKeyPress);

    // Cleanup on component unmount
    return () => {
      document.removeEventListener('keydown', handleGlobalKeyPress);
    };
  }, [userIdInput, isUserFetched]); // Dependencies for keyboard handler state

  /**
   * Handle ENTER key - Fetch user details
   * Maps to PROCESS-ENTER-KEY paragraph in COUSR03C.cbl
   */
  const handleFetchUser = async () => {
    // Clear previous messages and user details
    setErrorMessage('');
    setSuccessMessage('');
    setUserDetails({ firstName: '', lastName: '', userType: '' });
    setIsUserFetched(false);
    setShowDeleteConfirmation(false);

    // Validate user ID input - maps to COBOL validation logic
    if (!userIdInput || userIdInput.trim() === '') {
      setErrorMessage('User ID can NOT be empty...');
      return;
    }

    // Validate user ID format using validation utility
    const userIdValidation = validateUserID(userIdInput);
    if (!userIdValidation.isValid) {
      setErrorMessage(userIdValidation.errorMessage);
      return;
    }

    try {
      // Fetch user data from API - maps to READ-USER-SEC-FILE in COBOL
      const response = await getUsers({ 
        searchTerm: userIdInput.toUpperCase(),
        pageSize: 1,
        pageNumber: 1 
      });

      if (response.success && response.data && response.data.users && response.data.users.length > 0) {
        // User found - display details
        const user = response.data.users[0];
        
        // Exact match check (case-insensitive)
        if (user.userId.toUpperCase() === userIdInput.toUpperCase()) {
          setUserDetails({
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            userType: user.userType || ''
          });
          setIsUserFetched(true);
          setSuccessMessage('Press PF5 key to delete this user ...');
        } else {
          setErrorMessage('User ID NOT found...');
        }
      } else {
        // User not found - maps to DFHRESP(NOTFND) logic in COBOL
        setErrorMessage('User ID NOT found...');
      }
    } catch (error) {
      // API error - maps to error handling in COBOL
      console.error('Error fetching user:', error);
      setErrorMessage('Unable to lookup User...');
    }
  };

  /**
   * Handle F5 key - Delete user
   * Maps to DELETE-USER-INFO paragraph in COUSR03C.cbl
   */
  const handleDeleteUser = async () => {
    // Clear previous messages
    setErrorMessage('');
    setSuccessMessage('');

    // Validate user ID input
    if (!userIdInput || userIdInput.trim() === '') {
      setErrorMessage('User ID can NOT be empty...');
      return;
    }

    // Check if user details have been fetched
    if (!isUserFetched) {
      setErrorMessage('Please fetch user details first by pressing ENTER.');
      return;
    }

    // Prevent self-deletion
    if (userIdInput.toUpperCase() === currentUserId.toUpperCase()) {
      setErrorMessage('Cannot delete your own user account.');
      return;
    }

    // Show confirmation dialog for deletion
    if (!showDeleteConfirmation) {
      setShowDeleteConfirmation(true);
      setErrorMessage('WARNING: This will permanently delete the user. Press F5 again to confirm deletion.');
      return;
    }

    try {
      // Call delete user API - maps to DELETE-USER-SEC-FILE in COBOL
      const deleteResponse = await fetch(`/api/users/${userIdInput.toUpperCase()}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // Include session cookies
      });

      if (deleteResponse.ok) {
        // Successful deletion - maps to DFHRESP(NORMAL) logic
        setSuccessMessage(`User ${userIdInput.toUpperCase()} has been deleted ...`);
        
        // Clear form after successful deletion - maps to INITIALIZE-ALL-FIELDS
        handleClear();
        setShowDeleteConfirmation(false);
      } else if (deleteResponse.status === 404) {
        // User not found
        setErrorMessage('User ID NOT found...');
        setShowDeleteConfirmation(false);
      } else {
        // Other error
        setErrorMessage('Unable to delete User...');
        setShowDeleteConfirmation(false);
      }
    } catch (error) {
      console.error('Error deleting user:', error);
      setErrorMessage('Unable to delete User...');
      setShowDeleteConfirmation(false);
    }
  };

  /**
   * Handle F3/F12 keys - Cancel/Exit
   * Maps to PF3 and PF12 logic in COUSR03C.cbl (RETURN-TO-PREV-SCREEN)
   */
  const handleCancel = () => {
    // Clear any messages
    setErrorMessage('');
    setSuccessMessage('');
    
    // Navigate back to previous screen or admin menu
    // This would be handled by React Router in the actual implementation
    window.history.back();
  };

  /**
   * Handle F4 key - Clear screen
   * Maps to CLEAR-CURRENT-SCREEN and INITIALIZE-ALL-FIELDS in COUSR03C.cbl
   */
  const handleClear = () => {
    // Clear all form fields and state
    setUserIdInput('');
    setUserDetails({ firstName: '', lastName: '', userType: '' });
    setErrorMessage('');
    setSuccessMessage('');
    setIsUserFetched(false);
    setShowDeleteConfirmation(false);
  };

  /**
   * Handle user ID input change with validation
   * 
   * @param {Event} event - Input change event
   */
  const handleUserIdChange = (event) => {
    const value = event.target.value.toUpperCase(); // Convert to uppercase like mainframe
    
    // Limit to 8 characters matching BMS field definition
    if (value.length <= 8) {
      setUserIdInput(value);
      
      // Clear user details when user ID changes
      if (isUserFetched) {
        setUserDetails({ firstName: '', lastName: '', userType: '' });
        setIsUserFetched(false);
        setShowDeleteConfirmation(false);
      }
      
      // Clear any existing messages when user starts typing
      setErrorMessage('');
      setSuccessMessage('');
    }
  };

  /**
   * Handle form submission (ENTER key on form)
   * 
   * @param {Event} event - Form submit event
   */
  const handleFormSubmit = (event) => {
    event.preventDefault();
    handleFetchUser();
  };

  return (
    <div style={{ 
      fontFamily: 'monospace',
      backgroundColor: '#000000',
      color: '#00FF00',
      minHeight: '100vh',
      padding: 0,
      margin: 0
    }}>
      {/* Header Component - Maps to BMS header fields */}
      <Header 
        transactionId="CU03"
        programName="COUSR03C" 
        title="Delete User"
      />

      {/* Main Screen Content */}
      <div style={{ padding: '20px' }}>
        
        {/* Screen Title */}
        <div style={{ 
          textAlign: 'center', 
          marginBottom: '20px',
          color: '#FFFFFF',
          fontWeight: 'bold',
          fontSize: '16px'
        }}>
          Delete User
        </div>

        {/* User ID Input Section */}
        <form onSubmit={handleFormSubmit} style={{ marginBottom: '30px' }}>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center',
            marginBottom: '20px',
            marginLeft: '40px'  // Indent to match BMS layout
          }}>
            <label style={{ 
              color: '#00FF00',
              marginRight: '10px',
              minWidth: '120px'
            }}>
              Enter User ID:
            </label>
            <input
              type="text"
              value={userIdInput}
              onChange={handleUserIdChange}
              maxLength={8}
              style={{
                backgroundColor: 'transparent',
                border: 'none',
                borderBottom: '1px solid #00FF00',
                color: '#00FF00',
                fontFamily: 'monospace',
                fontSize: '14px',
                padding: '2px 5px',
                width: '120px',
                outline: 'none'
              }}
              autoFocus
            />
          </div>
        </form>

        {/* Separator Line */}
        <div style={{
          color: '#FFEB3B',
          textAlign: 'center',
          margin: '20px 0',
          letterSpacing: '2px'
        }}>
          ***********************************************************************
        </div>

        {/* User Details Display Section */}
        {isUserFetched && (
          <div style={{ marginLeft: '40px', marginBottom: '30px' }}>
            
            {/* First Name */}
            <div style={{ 
              display: 'flex', 
              alignItems: 'center',
              marginBottom: '15px'
            }}>
              <label style={{ 
                color: '#4FC3F7',
                marginRight: '10px',
                minWidth: '120px'
              }}>
                First Name:
              </label>
              <span style={{
                color: '#4FC3F7',
                textDecoration: 'underline',
                fontFamily: 'monospace',
                minWidth: '200px'
              }}>
                {userDetails.firstName}
              </span>
            </div>

            {/* Last Name */}
            <div style={{ 
              display: 'flex', 
              alignItems: 'center',
              marginBottom: '15px'
            }}>
              <label style={{ 
                color: '#4FC3F7',
                marginRight: '10px',
                minWidth: '120px'
              }}>
                Last Name:
              </label>
              <span style={{
                color: '#4FC3F7',
                textDecoration: 'underline',
                fontFamily: 'monospace',
                minWidth: '200px'
              }}>
                {userDetails.lastName}
              </span>
            </div>

            {/* User Type */}
            <div style={{ 
              display: 'flex', 
              alignItems: 'center',
              marginBottom: '15px'
            }}>
              <label style={{ 
                color: '#4FC3F7',
                marginRight: '10px',
                minWidth: '120px'
              }}>
                User Type:
              </label>
              <span style={{
                color: '#4FC3F7',
                textDecoration: 'underline',
                fontFamily: 'monospace',
                marginRight: '10px'
              }}>
                {userDetails.userType}
              </span>
              <span style={{ color: '#4FC3F7' }}>
                (A=Admin, U=User)
              </span>
            </div>
          </div>
        )}

        {/* Error/Success Message Display */}
        {errorMessage && (
          <div style={{ marginBottom: '20px' }}>
            <Alert 
              severity="error" 
              sx={{ 
                backgroundColor: 'transparent',
                color: '#FF6B6B',
                border: '1px solid #FF6B6B',
                fontFamily: 'monospace'
              }}
            >
              {errorMessage}
            </Alert>
          </div>
        )}

        {successMessage && (
          <div style={{ marginBottom: '20px' }}>
            <Alert 
              severity="success" 
              sx={{ 
                backgroundColor: 'transparent',
                color: '#4CAF50',
                border: '1px solid #4CAF50',
                fontFamily: 'monospace'
              }}
            >
              {successMessage}
            </Alert>
          </div>
        )}

        {/* PF-Key Instructions */}
        <div style={{
          position: 'absolute',
          bottom: '20px',
          left: '20px',
          color: '#FFEB3B',
          fontFamily: 'monospace',
          fontSize: '12px'
        }}>
          ENTER=Fetch  F3=Back  F4=Clear  F5=Delete
        </div>

      </div>
    </div>
  );
};

export default UserDelete;