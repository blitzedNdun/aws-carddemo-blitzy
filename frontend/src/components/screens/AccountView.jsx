/**
 * AccountView Component - React component for the Account View screen (COACTVW)
 * 
 * Displays comprehensive account information with real-time data from backend.
 * Provides 11-digit account ID search, displays account details including status, 
 * dates, credit limits, balances, and customer information. Implements read-only 
 * view with navigation to account update functionality via PF-keys.
 * 
 * Converted from COBOL program COACTVWC and BMS mapset COACTVW.
 * Maps CICS transaction CAVW to React component with Spring Boot backend integration.
 * 
 * Key Features:
 * - Account search with 11-digit validation (ACCTSID field)
 * - Display account fields: status, dates, credit limits, balances
 * - Display customer fields: ID, SSN, DOB, FICO score, name, address, phones
 * - PF-key navigation: F3=Back, F4=Clear, Enter=Search
 * - Error handling and loading states
 * - Real-time data formatting using COBOL data converters
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Alert, 
  Box, 
  TextField, 
  Typography, 
  Button, 
  Grid, 
  CircularProgress 
} from '@mui/material';

// Internal imports from dependency files
import { getAccount } from '../../services/api.js';
import { validateFICO } from '../../utils/validation.js';
import { displayFormat } from '../../utils/CobolDataConverter.js';
import Header from '../../components/common/Header.jsx';

/**
 * AccountView Component
 * 
 * React functional component that replaces the COACTVWC COBOL program.
 * Implements the CAVW transaction functionality for viewing account details.
 * 
 * @returns {JSX.Element} Rendered AccountView component
 */
const AccountView = () => {
  // Navigation hook for PF-key functionality (F3=Back)
  const navigate = useNavigate();

  // Component state management
  const [accountId, setAccountId] = useState('');
  const [accountData, setAccountData] = useState(null);
  const [customerData, setCustomerData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');

  /**
   * Handle account ID input change with validation
   * Replicates COBOL field validation for ACCTSID (11-digit numeric)
   * 
   * @param {Event} event - Input change event
   */
  const handleAccountIdChange = (event) => {
    const value = event.target.value;
    
    // Allow only numeric input and limit to 11 digits (matching BMS PICIN='99999999999')
    if (value === '' || (/^\d{0,11}$/.test(value))) {
      setAccountId(value);
      setError('');
      setInfoMessage('');
    }
  };

  /**
   * Search for account details using the entered account ID
   * Replicates COBOL 2000-SECT paragraph logic from COACTVWC program
   */
  const searchAccount = useCallback(async () => {
    // Validate account ID format (11 digits required - MUSTFILL validation)
    if (!accountId || accountId.length !== 11) {
      setError('Account number must be exactly 11 digits');
      return;
    }

    setLoading(true);
    setError('');
    setInfoMessage('');
    setAccountData(null);
    setCustomerData(null);

    try {
      // Call backend API - maps to CICS READ ACCTDAT and CUSTDAT operations
      const response = await getAccount(accountId);
      
      if (response.success && response.account) {
        setAccountData(response.account);
        setCustomerData(response.customer);
        setInfoMessage('Account information retrieved successfully');
      } else {
        setError(response.message || 'Account not found');
      }
    } catch (apiError) {
      console.error('Error fetching account data:', apiError);
      setError('Error retrieving account information. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [accountId]);

  /**
   * Handle Enter key press for search functionality
   * Replicates CICS RECEIVE MAP processing
   * 
   * @param {KeyboardEvent} event - Keyboard event
   */
  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !loading) {
      event.preventDefault();
      searchAccount();
    } else if (event.key === 'F3') {
      event.preventDefault();
      handleExitScreen();
    } else if (event.key === 'F4') {
      event.preventDefault();
      handleClearScreen();
    }
  };

  /**
   * Handle F3 (Exit) PF-key functionality
   * Maps to COBOL EXEC CICS XCTL PROGRAM('COMEN01')
   */
  const handleExitScreen = () => {
    navigate('/menu'); // Navigate back to main menu
  };

  /**
   * Handle F4 (Clear) PF-key functionality
   * Replicates BMS mapset clear functionality
   */
  const handleClearScreen = () => {
    setAccountId('');
    setAccountData(null);
    setCustomerData(null);
    setError('');
    setInfoMessage('');
  };

  /**
   * Format currency values for display
   * Uses COBOL COMP-3 precision formatting with proper scale
   * 
   * @param {number|string} value - Currency value to format
   * @returns {string} Formatted currency string
   */
  const formatCurrency = (value) => {
    if (value == null || value === '') return '$0.00';
    return displayFormat(value, 'S9(10)V99', { showDecimal: true, trimLeadingZeros: true });
  };

  /**
   * Format date values for display (MM/DD/YYYY format)
   * Matches BMS date display format
   * 
   * @param {string} dateValue - Date string to format
   * @returns {string} Formatted date string
   */
  const formatDate = (dateValue) => {
    if (!dateValue) return '';
    
    // Handle various date formats from backend
    try {
      const date = new Date(dateValue);
      if (isNaN(date.getTime())) return dateValue; // Return as-is if invalid
      
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const year = date.getFullYear();
      
      return `${month}/${day}/${year}`;
    } catch (error) {
      return dateValue; // Return original value if formatting fails
    }
  };

  /**
   * Format and validate FICO score display
   * Uses validation utility to ensure score is in valid range (300-850)
   * 
   * @param {number|string} score - FICO score to format and validate
   * @returns {string} Formatted FICO score
   */
  const formatFicoScore = (score) => {
    if (!score) return '';
    
    const isValid = validateFICO(score);
    return isValid ? String(score) : `${score} (Invalid)`;
  };

  // Register global keyboard event listeners for PF-keys
  useEffect(() => {
    const handleGlobalKeyDown = (event) => {
      // Handle F3 and F4 function keys globally
      if (event.code === 'F3') {
        event.preventDefault();
        handleExitScreen();
      } else if (event.code === 'F4') {
        event.preventDefault();
        handleClearScreen();
      }
    };

    document.addEventListener('keydown', handleGlobalKeyDown);
    
    return () => {
      document.removeEventListener('keydown', handleGlobalKeyDown);
    };
  }, []);

  return (
    <Box sx={{ 
      width: '100%', 
      maxWidth: '1200px', 
      margin: '0 auto', 
      backgroundColor: '#f5f5f5',
      minHeight: '100vh'
    }}>
      {/* BMS-style Header Component */}
      <Header 
        transactionId="CAVW"
        programName="COACTVWC" 
        title="View Account Details - Account Information Display"
      />
      
      {/* Main Content Area */}
      <Box sx={{ padding: 3 }}>
        {/* Screen Title */}
        <Typography 
          variant="h5" 
          sx={{ 
            textAlign: 'center', 
            mb: 3, 
            color: '#333',
            fontWeight: 'bold'
          }}
        >
          View Account
        </Typography>

        {/* Account Search Section */}
        <Grid container spacing={2} alignItems="center" sx={{ mb: 3 }}>
          <Grid item xs={12} sm={3}>
            <Typography variant="body1" sx={{ color: '#008080', fontWeight: 'bold' }}>
              Account Number:
            </Typography>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              value={accountId}
              onChange={handleAccountIdChange}
              onKeyDown={handleKeyDown}
              placeholder="Enter 11-digit account ID"
              variant="outlined"
              size="small"
              fullWidth
              inputProps={{ 
                maxLength: 11,
                pattern: '[0-9]*',
                'data-testid': 'account-id-input'
              }}
              sx={{
                '& .MuiOutlinedInput-input': {
                  fontFamily: 'monospace',
                  backgroundColor: accountId ? '#e8f5e8' : '#ffffff'
                }
              }}
            />
          </Grid>
          <Grid item xs={12} sm={2}>
            <Button
              onClick={searchAccount}
              disabled={loading || accountId.length !== 11}
              variant="contained"
              sx={{ 
                backgroundColor: '#1976d2',
                '&:hover': { backgroundColor: '#115293' }
              }}
            >
              {loading ? <CircularProgress size={20} /> : 'Search'}
            </Button>
          </Grid>
          <Grid item xs={12} sm={3}>
            <Typography variant="body2" sx={{ color: '#008080' }}>
              Active Y/N: {accountData ? (accountData.active ? 'Y' : 'N') : ''}
            </Typography>
          </Grid>
        </Grid>

        {/* Error Message Display */}
        {error && (
          <Alert 
            severity="error" 
            sx={{ mb: 2 }}
            onClose={() => setError('')}
          >
            {error}
          </Alert>
        )}

        {/* Info Message Display */}
        {infoMessage && (
          <Alert 
            severity="info" 
            sx={{ mb: 2 }}
            onClose={() => setInfoMessage('')}
          >
            {infoMessage}
          </Alert>
        )}

        {/* Account Details Section */}
        {accountData && (
          <Box sx={{ mt: 3 }}>
            {/* Account Information Grid */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
              {/* Left Column - Account Dates */}
              <Grid item xs={12} md={6}>
                <Typography variant="h6" sx={{ color: '#008080', mb: 2 }}>
                  Account Information
                </Typography>
                
                {/* Opened Date */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={4}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Opened:</Typography>
                  </Grid>
                  <Grid item xs={8}>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                      {formatDate(accountData.openDate)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Expiry Date */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={4}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Expiry:</Typography>
                  </Grid>
                  <Grid item xs={8}>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                      {formatDate(accountData.expiryDate)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Reissue Date */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={4}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Reissue:</Typography>
                  </Grid>
                  <Grid item xs={8}>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                      {formatDate(accountData.reissueDate)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Account Group */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={4}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Account Group:</Typography>
                  </Grid>
                  <Grid item xs={8}>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                      {accountData.accountGroup || ''}
                    </Typography>
                  </Grid>
                </Grid>
              </Grid>

              {/* Right Column - Financial Information */}
              <Grid item xs={12} md={6}>
                <Typography variant="h6" sx={{ color: '#008080', mb: 2 }}>
                  Financial Information
                </Typography>
                
                {/* Credit Limit */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Credit Limit:</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ 
                      fontFamily: 'monospace', 
                      textDecoration: 'underline',
                      textAlign: 'right'
                    }}>
                      {formatCurrency(accountData.creditLimit)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Cash Credit Limit */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Cash credit Limit:</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ 
                      fontFamily: 'monospace', 
                      textDecoration: 'underline',
                      textAlign: 'right'
                    }}>
                      {formatCurrency(accountData.cashCreditLimit)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Current Balance */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Current Balance:</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ 
                      fontFamily: 'monospace', 
                      textDecoration: 'underline',
                      textAlign: 'right'
                    }}>
                      {formatCurrency(accountData.currentBalance)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Current Cycle Credit */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Current Cycle Credit:</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ 
                      fontFamily: 'monospace', 
                      textDecoration: 'underline',
                      textAlign: 'right'
                    }}>
                      {formatCurrency(accountData.currentCycleCredit)}
                    </Typography>
                  </Grid>
                </Grid>

                {/* Current Cycle Debit */}
                <Grid container spacing={1} sx={{ mb: 1 }}>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ color: '#008080' }}>Current Cycle Debit:</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" sx={{ 
                      fontFamily: 'monospace', 
                      textDecoration: 'underline',
                      textAlign: 'right'
                    }}>
                      {formatCurrency(accountData.currentCycleDebit)}
                    </Typography>
                  </Grid>
                </Grid>
              </Grid>
            </Grid>

            {/* Customer Details Section */}
            {customerData && (
              <Box sx={{ mt: 4 }}>
                <Typography variant="h6" sx={{ 
                  color: '#333', 
                  textAlign: 'center', 
                  mb: 3,
                  fontWeight: 'bold'
                }}>
                  Customer Details
                </Typography>

                <Grid container spacing={2}>
                  {/* Customer ID and SSN */}
                  <Grid item xs={12} md={6}>
                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={5}>
                        <Typography variant="body2" sx={{ color: '#008080' }}>Customer id:</Typography>
                      </Grid>
                      <Grid item xs={7}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {customerData.customerId}
                        </Typography>
                      </Grid>
                    </Grid>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={3}>
                        <Typography variant="body2" sx={{ color: '#008080' }}>SSN:</Typography>
                      </Grid>
                      <Grid item xs={9}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {customerData.ssn}
                        </Typography>
                      </Grid>
                    </Grid>
                  </Grid>

                  {/* Date of Birth and FICO Score */}
                  <Grid item xs={12} md={6}>
                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={5}>
                        <Typography variant="body2" sx={{ color: '#008080' }}>Date of birth:</Typography>
                      </Grid>
                      <Grid item xs={7}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {formatDate(customerData.dateOfBirth)}
                        </Typography>
                      </Grid>
                    </Grid>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={5}>
                        <Typography variant="body2" sx={{ color: '#008080' }}>FICO Score:</Typography>
                      </Grid>
                      <Grid item xs={7}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {formatFicoScore(customerData.ficoScore)}
                        </Typography>
                      </Grid>
                    </Grid>
                  </Grid>

                  {/* Name Fields */}
                  <Grid item xs={12}>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" sx={{ color: '#008080', mb: 1 }}>First Name</Typography>
                        <Typography variant="body2" sx={{ 
                          fontFamily: 'monospace', 
                          textDecoration: 'underline',
                          minHeight: '20px'
                        }}>
                          {customerData.firstName}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" sx={{ color: '#008080', mb: 1 }}>Middle Name:</Typography>
                        <Typography variant="body2" sx={{ 
                          fontFamily: 'monospace', 
                          textDecoration: 'underline',
                          minHeight: '20px'
                        }}>
                          {customerData.middleName}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" sx={{ color: '#008080', mb: 1 }}>Last Name:</Typography>
                        <Typography variant="body2" sx={{ 
                          fontFamily: 'monospace', 
                          textDecoration: 'underline',
                          minHeight: '20px'
                        }}>
                          {customerData.lastName}
                        </Typography>
                      </Grid>
                    </Grid>
                  </Grid>

                  {/* Address Fields */}
                  <Grid item xs={12}>
                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={2}>
                        <Typography variant="body2" sx={{ color: '#008080' }}>Address:</Typography>
                      </Grid>
                      <Grid item xs={8}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {customerData.addressLine1}
                        </Typography>
                      </Grid>
                      <Grid item xs={2}>
                        <Grid container spacing={1}>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>State</Typography>
                          </Grid>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                              {customerData.state}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                    </Grid>

                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={2}>
                        <Typography variant="body2" sx={{ color: 'transparent' }}>Address:</Typography>
                      </Grid>
                      <Grid item xs={8}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {customerData.addressLine2}
                        </Typography>
                      </Grid>
                      <Grid item xs={2}>
                        <Grid container spacing={1}>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>Zip</Typography>
                          </Grid>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ 
                              fontFamily: 'monospace', 
                              textDecoration: 'underline',
                              textAlign: 'right'
                            }}>
                              {customerData.zipCode}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                    </Grid>

                    <Grid container spacing={1} sx={{ mb: 1 }}>
                      <Grid item xs={2}>
                        <Typography variant="body2" sx={{ color: '#008080' }}>City</Typography>
                      </Grid>
                      <Grid item xs={8}>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                          {customerData.city}
                        </Typography>
                      </Grid>
                      <Grid item xs={2}>
                        <Grid container spacing={1}>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>Country</Typography>
                          </Grid>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                              {customerData.country}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                    </Grid>
                  </Grid>

                  {/* Phone and Additional Information */}
                  <Grid item xs={12}>
                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <Grid container spacing={1} sx={{ mb: 1 }}>
                          <Grid item xs={4}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>Phone 1:</Typography>
                          </Grid>
                          <Grid item xs={8}>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                              {customerData.phoneNumber1}
                            </Typography>
                          </Grid>
                        </Grid>
                        <Grid container spacing={1} sx={{ mb: 1 }}>
                          <Grid item xs={4}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>Phone 2:</Typography>
                          </Grid>
                          <Grid item xs={8}>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                              {customerData.phoneNumber2}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                      <Grid item xs={12} sm={6}>
                        <Grid container spacing={1} sx={{ mb: 1 }}>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>Government Issued Id Ref:</Typography>
                          </Grid>
                          <Grid item xs={6}>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                              {customerData.governmentId}
                            </Typography>
                          </Grid>
                        </Grid>
                        <Grid container spacing={1} sx={{ mb: 1 }}>
                          <Grid item xs={5}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>EFT Account Id:</Typography>
                          </Grid>
                          <Grid item xs={4}>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', textDecoration: 'underline' }}>
                              {customerData.eftAccountId}
                            </Typography>
                          </Grid>
                          <Grid item xs={3}>
                            <Typography variant="body2" sx={{ color: '#008080' }}>
                              Primary Card Holder Y/N: 
                              <span style={{ fontFamily: 'monospace', textDecoration: 'underline', marginLeft: '4px' }}>
                                {customerData.primaryCardHolder ? 'Y' : 'N'}
                              </span>
                            </Typography>
                          </Grid>
                        </Grid>
                      </Grid>
                    </Grid>
                  </Grid>
                </Grid>
              </Box>
            )}
          </Box>
        )}

        {/* Action Buttons - PF Key Functions */}
        <Box sx={{ 
          mt: 4, 
          pt: 2, 
          borderTop: '1px solid #ccc',
          display: 'flex',
          gap: 2
        }}>
          <Button
            onClick={handleExitScreen}
            variant="outlined"
            sx={{ color: '#008080', borderColor: '#008080' }}
          >
            F3=Exit
          </Button>
          <Button
            onClick={handleClearScreen}
            variant="outlined"
            sx={{ color: '#008080', borderColor: '#008080' }}
          >
            F4=Clear
          </Button>
          <Typography variant="body2" sx={{ 
            color: '#008080', 
            alignSelf: 'center',
            ml: 'auto'
          }}>
            Enter=Search for account information
          </Typography>
        </Box>
      </Box>
    </Box>
  );
};

export default AccountView;