/**
 * CreditCardDetail.jsx - Credit Card Detail Screen Component
 *
 * React component for the Credit Card Detail screen (COCRDSL), displaying comprehensive
 * card information. Shows card details including number, status, expiry date, account
 * linkage, and cardholder information. Provides read-only view with quick navigation
 * to update functionality via PF-keys.
 *
 * Maps to COBOL program COCRDSLC and BMS mapset COCRDSL
 * Transaction Code: CCDL
 *
 * This component implements the exact functionality of the COBOL COCRDSLC program:
 * - Displays card details in read-only format matching BMS field layout
 * - Shows account number (11 digits), card number (16 digits), cardholder name
 * - Displays card status (Y/N), expiry date (MM/YYYY format)
 * - Implements PF-key navigation (F3=Back, F4=Clear, Enter=Select)
 * - Includes informational and error message display areas
 * - Retrieves data via getCard API function replacing CICS READ operations
 */

import {
  Box,
  TextField,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Typography,
} from '@mui/material';
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

// Internal imports - ONLY from depends_on_files
import Header from '../../components/common/Header.jsx';
import { getCard } from '../../services/api.js';
import { displayFormat } from '../../utils/CobolDataConverter.js';

/**
 * CreditCardDetail Component
 *
 * Displays detailed credit card information in a read-only format,
 * replicating the COCRDSL BMS mapset layout and COCRDSLC program functionality.
 *
 * @returns {JSX.Element} Credit Card Detail screen component
 */
const CreditCardDetail = () => {
  // Navigation hook for PF-key routing
  const navigate = useNavigate();

  // Component state management
  const [cardData, setCardData] = useState({
    accountId: '',
    cardNumber: '',
    cardholderName: '',
    cardStatus: '',
    expiryMonth: '',
    expiryYear: '',
    expiryDate: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [searchCriteria, setSearchCriteria] = useState({
    accountId: '',
    cardNumber: '',
  });

  /**
   * Load card data using the getCard API function
   * Replicates COBOL 9000-READ-DATA paragraph functionality
   *
   * @param {string} cardNumber - 16-digit card number
   */
  const loadCardData = useCallback(async (cardNumber) => {
    if (!cardNumber || !/^\d{16}$/.test(cardNumber)) {
      setError('Card number must be exactly 16 digits');
      return;
    }

    setLoading(true);
    setError('');
    setInfoMessage('');

    try {
      const response = await getCard(cardNumber);

      if (response.success) {
        const card = response.data;

        setCardData({
          accountId: displayFormat(card.accountId || '', '9(11)'),
          cardNumber: displayFormat(card.cardNumber || '', '9(16)'),
          cardholderName: card.embossedName || card.cardholderName || '',
          cardStatus: card.activeStatus === 'Y' ? 'Y' : 'N',
          expiryMonth: card.expiryMonth ? String(card.expiryMonth).padStart(2, '0') : '',
          expiryYear: card.expiryYear ? String(card.expiryYear) : '',
          expiryDate: card.expiryDate || '',
        });

        setInfoMessage('   Displaying requested details');
      } else {
        // Handle API errors similar to COBOL error handling
        if (response.status === 404) {
          setError('Did not find cards for this search condition');
        } else {
          setError(response.error || 'Error reading Card Data File');
        }
      }
    } catch (err) {
      setError('Unexpected error occurred while reading card data');
      console.error('Card detail load error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Initialize component and load card data if coming from card list
   * Replicates COBOL 0000-MAIN initialization logic
   */
  useEffect(() => {
    // Get search parameters from URL or session storage
    const urlParams = new URLSearchParams(window.location.search);
    const accountParam = urlParams.get('accountId');
    const cardParam = urlParams.get('cardNumber');

    // Try to get from session storage if not in URL (coming from card list)
    const sessionAccountId = sessionStorage.getItem('selectedAccountId');
    const sessionCardNumber = sessionStorage.getItem('selectedCardNumber');

    const accountId = accountParam || sessionAccountId || '';
    const cardNumber = cardParam || sessionCardNumber || '';

    setSearchCriteria({
      accountId,
      cardNumber,
    });

    // If we have a card number, load the data automatically
    if (cardNumber && /^\d{16}$/.test(cardNumber)) {
      loadCardData(cardNumber);
    } else {
      setInfoMessage('Please enter Account and Card Number');
    }

    // Clear session storage after use
    if (sessionAccountId) {sessionStorage.removeItem('selectedAccountId');}
    if (sessionCardNumber) {sessionStorage.removeItem('selectedCardNumber');}
  }, [loadCardData]);

  /**
   * Handle search operation when user enters account and card criteria
   * Replicates COBOL 2000-PROCESS-INPUTS functionality
   */
  const handleSearch = useCallback(() => {
    setError('');
    setInfoMessage('');

    // Input validation matching COBOL edit routines
    const accountId = searchCriteria.accountId.trim();
    const cardNumber = searchCriteria.cardNumber.trim();

    if (!accountId || accountId === '' || !/^\d{11}$/.test(accountId)) {
      setError('Account number must be a non zero 11 digit number');
      return;
    }

    if (!cardNumber || cardNumber === '' || !/^\d{16}$/.test(cardNumber)) {
      setError('Card number if supplied must be a 16 digit number');
      return;
    }

    // Load card data with the provided criteria
    loadCardData(cardNumber);
  }, [searchCriteria, loadCardData]);

  /**
   * Handle PF-key navigation
   * Replicates COBOL PF-key handling and CICS XCTL operations
   */
  const handlePFKey = useCallback((key) => {
    switch (key) {
      case 'F3': {
        // Exit back to calling program or main menu
        const fromProgram = sessionStorage.getItem('fromProgram');
        if (fromProgram === 'COCRDLIC') {
          navigate('/cards');
        } else {
          navigate('/menu');
        }
        break;
      }

      case 'F4':
        // Clear screen data
        setCardData({
          accountId: '',
          cardNumber: '',
          cardholderName: '',
          cardStatus: '',
          expiryMonth: '',
          expiryYear: '',
          expiryDate: '',
        });
        setSearchCriteria({
          accountId: '',
          cardNumber: '',
        });
        setError('');
        setInfoMessage('Please enter Account and Card Number');
        break;

      case 'ENTER':
        // Execute search with current criteria
        handleSearch();
        break;

      default:
        break;
    }
  }, [navigate, handleSearch]);

  /**
   * Keyboard event handler for PF-key simulation
   * Maps function keys to PF-key actions
   */
  const handleKeyDown = useCallback((event) => {
    if (event.key === 'F3' || (event.key === 'Escape')) {
      event.preventDefault();
      handlePFKey('F3');
    } else if (event.key === 'F4') {
      event.preventDefault();
      handlePFKey('F4');
    } else if (event.key === 'Enter' && !loading) {
      event.preventDefault();
      handlePFKey('ENTER');
    }
  }, [handlePFKey, loading]);

  // Set up keyboard event listeners for PF-key handling
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  /**
   * Format expiry date for display (MM/YYYY format)
   * Replicates COBOL date formatting from CARD-EXPIRY-MONTH and CARD-EXPIRY-YEAR
   */
  const getFormattedExpiryDate = () => {
    if (cardData.expiryMonth && cardData.expiryYear) {
      return `${cardData.expiryMonth}/${cardData.expiryYear}`;
    }
    if (cardData.expiryDate) {
      // Parse from stored format if available
      return cardData.expiryDate;
    }
    return '';
  };

  return (
    <Box sx={{ width: '100%', minHeight: '100vh', backgroundColor: '#f5f5f5' }}>
      {/* Header matching BMS screen header layout */}
      <Header
        transactionId="CCDL"
        programName="COCRDSLC"
        title="View Credit Card Detail"
      />

      {/* Main content area */}
      <Box sx={{ padding: 3 }}>
        <Grid container spacing={3}>

          {/* Search criteria section */}
          <Grid item xs={12}>
            <Box sx={{
              backgroundColor: 'white',
              padding: 3,
              borderRadius: 1,
              border: '1px solid #e0e0e0',
            }}>
              <Typography variant="h6" gutterBottom sx={{ color: '#1976d2' }}>
                Search Criteria
              </Typography>

              <Grid container spacing={2} sx={{ marginTop: 1 }}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Account Number"
                    value={searchCriteria.accountId}
                    onChange={(e) => setSearchCriteria(prev => ({
                      ...prev,
                      accountId: e.target.value,
                    }))}
                    inputProps={{
                      maxLength: 11,
                      style: { fontFamily: 'monospace' },
                    }}
                    helperText="11-digit account number"
                    disabled={loading}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Card Number"
                    value={searchCriteria.cardNumber}
                    onChange={(e) => setSearchCriteria(prev => ({
                      ...prev,
                      cardNumber: e.target.value,
                    }))}
                    inputProps={{
                      maxLength: 16,
                      style: { fontFamily: 'monospace' },
                    }}
                    helperText="16-digit card number"
                    disabled={loading}
                  />
                </Grid>
              </Grid>
            </Box>
          </Grid>

          {/* Card details section - read-only display */}
          {(cardData.cardNumber || loading) && (
            <Grid item xs={12}>
              <Box sx={{
                backgroundColor: 'white',
                padding: 3,
                borderRadius: 1,
                border: '1px solid #e0e0e0',
              }}>
                <Typography variant="h6" gutterBottom sx={{ color: '#1976d2' }}>
                  Card Details
                </Typography>

                {loading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', padding: 4 }}>
                    <CircularProgress />
                  </Box>
                ) : (
                  <Grid container spacing={2} sx={{ marginTop: 1 }}>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Name on card"
                        value={cardData.cardholderName}
                        InputProps={{
                          readOnly: true,
                          style: { fontFamily: 'monospace' },
                        }}
                        variant="outlined"
                      />
                    </Grid>

                    <Grid item xs={12} md={3}>
                      <TextField
                        fullWidth
                        label="Card Active Y/N"
                        value={cardData.cardStatus}
                        InputProps={{
                          readOnly: true,
                          style: { fontFamily: 'monospace' },
                        }}
                        variant="outlined"
                      />
                    </Grid>

                    <Grid item xs={12} md={3}>
                      <TextField
                        fullWidth
                        label="Expiry Date"
                        value={getFormattedExpiryDate()}
                        InputProps={{
                          readOnly: true,
                          style: { fontFamily: 'monospace' },
                        }}
                        variant="outlined"
                        helperText="MM/YYYY format"
                      />
                    </Grid>
                  </Grid>
                )}
              </Box>
            </Grid>
          )}

          {/* Information and error message areas */}
          {infoMessage && (
            <Grid item xs={12}>
              <Alert severity="info" sx={{ fontFamily: 'monospace' }}>
                {infoMessage}
              </Alert>
            </Grid>
          )}

          {error && (
            <Grid item xs={12}>
              <Alert severity="error" sx={{ fontFamily: 'monospace' }}>
                {error}
              </Alert>
            </Grid>
          )}

          {/* PF-key action buttons */}
          <Grid item xs={12}>
            <Box sx={{
              backgroundColor: 'white',
              padding: 2,
              borderRadius: 1,
              border: '1px solid #e0e0e0',
              textAlign: 'center',
            }}>
              <Typography variant="body2" sx={{
                fontFamily: 'monospace',
                color: '#fbc02d',
                marginBottom: 2,
              }}>
                ENTER=Search Cards  F3=Exit  F4=Clear
              </Typography>

              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
                <Button
                  variant="contained"
                  onClick={() => handlePFKey('ENTER')}
                  disabled={loading}
                  sx={{ minWidth: 120 }}
                >
                  Search (Enter)
                </Button>

                <Button
                  variant="outlined"
                  onClick={() => handlePFKey('F4')}
                  disabled={loading}
                  sx={{ minWidth: 120 }}
                >
                  Clear (F4)
                </Button>

                <Button
                  variant="outlined"
                  onClick={() => handlePFKey('F3')}
                  disabled={loading}
                  sx={{ minWidth: 120 }}
                >
                  Exit (F3)
                </Button>
              </Box>
            </Box>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default CreditCardDetail;
