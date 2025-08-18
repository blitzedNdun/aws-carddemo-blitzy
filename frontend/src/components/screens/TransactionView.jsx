/**
 * TransactionView.jsx - React component for Transaction Detail View screen (COTRN01)
 *
 * Comprehensive React component that converts the COTRN01.bms BMS mapset to a modern
 * single-page application interface while maintaining 100% functional parity with the
 * original COBOL CICS program COTRN01C.cbl.
 *
 * This component provides:
 * - Read-only transaction detail display with comprehensive field layout
 * - Transaction lookup by Transaction ID with real-time validation
 * - Material-UI form components matching original BMS field positioning
 * - PF-key navigation (F3=Back, F4=Clear, F5=Browse) via keyboard event handlers
 * - Error message display area matching original COBOL error handling
 * - 3270 terminal screen layout replication with responsive design
 * - Spring Boot backend integration via REST API calls
 *
 * Maps to:
 * - BMS Mapset: COTRN01.bms (COTRN1A map)
 * - COBOL Program: COTRN01C.cbl
 * - Transaction Code: CT01
 * - CICS Dataset: TRANSACT (via Spring Boot REST API)
 *
 * Screen Layout (converted from 3270 terminal format):
 * - Header: Transaction name (CT01), program name (COTRN01C), title, date/time
 * - Input: Transaction ID field with validation
 * - Display: Comprehensive transaction details in read-only format
 * - Footer: PF-key commands and error message area
 */

// External imports
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box } from '@mui/material';

// Internal imports - ONLY from depends_on_files
import { getTransactionDetail } from '../../services/api.js';
import Header from '../common/Header.jsx';
import { formatCurrency } from '../../utils/CobolDataConverter.js';

/**
 * TransactionView functional component - Default export
 * 
 * Replicates the COTRN01 transaction detail view screen with complete
 * functionality matching the original COBOL CICS implementation.
 *
 * @returns {JSX.Element} Complete transaction view screen component
 */
const TransactionView = () => {
  // React Router navigation hook for PF-key navigation
  const navigate = useNavigate();

  // Component state management matching COBOL working storage
  const [transactionId, setTransactionId] = useState('');
  const [transactionData, setTransactionData] = useState({
    transactionId: '',
    cardNumber: '',
    typeCode: '',
    categoryCode: '',
    source: '',
    description: '',
    amount: '',
    originalDate: '',
    processDate: '',
    merchantId: '',
    merchantName: '',
    merchantCity: '',
    merchantZip: '',
  });
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [hasData, setHasData] = useState(false);

  /**
   * Clear all transaction data and reset form to initial state
   * Replicates COBOL CLEAR-CURRENT-SCREEN and INITIALIZE-ALL-FIELDS paragraphs
   */
  const clearTransactionData = useCallback(() => {
    setTransactionData({
      transactionId: '',
      cardNumber: '',
      typeCode: '',
      categoryCode: '',
      source: '',
      description: '',
      amount: '',
      originalDate: '',
      processDate: '',
      merchantId: '',
      merchantName: '',
      merchantCity: '',
      merchantZip: '',
    });
    setHasData(false);
    setErrorMessage('');
  }, []);

  /**
   * Fetch transaction details from backend
   * Replicates COBOL READ-TRANSACT-FILE paragraph with comprehensive error handling
   * 
   * @param {string} tranId - Transaction ID to fetch
   */
  const fetchTransactionDetail = useCallback(async (tranId) => {
    if (!tranId || tranId.trim() === '') {
      setErrorMessage('Tran ID can NOT be empty...');
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const response = await getTransactionDetail(tranId.trim());

      if (response.success && response.data) {
        const data = response.data;
        
        // Map backend data to component state matching COBOL field assignments
        setTransactionData({
          transactionId: data.transactionId || data.id || '',
          cardNumber: data.cardNumber || '',
          typeCode: data.typeCode || data.transactionType || '',
          categoryCode: data.categoryCode || '',
          source: data.source || '',
          description: data.description || '',
          amount: data.amount ? formatCurrency(data.amount) : '',
          originalDate: data.originalDate || data.transactionDate || '',
          processDate: data.processDate || data.processingDate || '',
          merchantId: data.merchantId || '',
          merchantName: data.merchantName || '',
          merchantCity: data.merchantCity || '',
          merchantZip: data.merchantZip || '',
        });
        
        setHasData(true);
        setErrorMessage('');
      } else {
        // Handle API error responses matching COBOL error handling
        if (response.status === 404) {
          setErrorMessage('Transaction ID NOT found...');
        } else {
          setErrorMessage(response.error || 'Unable to lookup Transaction...');
        }
        clearTransactionData();
      }
    } catch (error) {
      console.error('Error fetching transaction detail:', error);
      setErrorMessage('Unable to lookup Transaction...');
      clearTransactionData();
    } finally {
      setIsLoading(false);
    }
  }, [clearTransactionData]);

  /**
   * Handle ENTER key processing for transaction lookup
   * Replicates COBOL PROCESS-ENTER-KEY paragraph
   */
  const handleEnterKey = useCallback(() => {
    if (transactionId.trim() === '') {
      setErrorMessage('Tran ID can NOT be empty...');
      return;
    }
    
    fetchTransactionDetail(transactionId);
  }, [transactionId, fetchTransactionDetail]);

  /**
   * Handle PF-key navigation matching original COBOL CICS program flow
   * Replicates COBOL MAIN-PARA EVALUATE EIBAID logic
   * 
   * @param {string} pfKey - PF-key pressed (F3, F4, F5)
   */
  const handlePFKey = useCallback((pfKey) => {
    switch (pfKey) {
      case 'F3':
        // PF3: Return to previous screen (COTRN00C transaction list)
        navigate('/transactions');
        break;
      case 'F4':
        // PF4: Clear current screen
        setTransactionId('');
        clearTransactionData();
        break;
      case 'F5':
        // PF5: Browse transactions (go to transaction list)
        navigate('/transactions');
        break;
      default:
        setErrorMessage('Invalid key pressed');
    }
  }, [navigate, clearTransactionData]);

  /**
   * Global keyboard event handler for PF-key navigation
   * Implements 3270 terminal PF-key functionality in React
   */
  const handleKeyDown = useCallback((event) => {
    // Handle PF-keys (F3, F4, F5) and ENTER
    switch (event.key) {
      case 'F3':
        event.preventDefault();
        handlePFKey('F3');
        break;
      case 'F4':
        event.preventDefault();
        handlePFKey('F4');
        break;
      case 'F5':
        event.preventDefault();
        handlePFKey('F5');
        break;
      case 'Enter':
        if (event.target.name === 'transactionId') {
          event.preventDefault();
          handleEnterKey();
        }
        break;
    }
  }, [handlePFKey, handleEnterKey]);

  /**
   * Set up keyboard event listeners for PF-key functionality
   * Replicates 3270 terminal keyboard handling
   */
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  /**
   * Handle transaction ID input change with validation
   * Replicates COBOL field input processing
   */
  const handleTransactionIdChange = (event) => {
    const value = event.target.value;
    setTransactionId(value);
    setErrorMessage(''); // Clear error when user starts typing
  };

  /**
   * Handle form submission (ENTER key processing)
   */
  const handleSubmit = (event) => {
    event.preventDefault();
    handleEnterKey();
  };

  return (
    <Box 
      component="div"
      sx={{
        width: '100%',
        height: '100vh',
        backgroundColor: '#000000', // 3270 terminal black background
        color: '#00FF00', // 3270 terminal green text
        fontFamily: 'monospace',
        fontSize: '14px',
        overflow: 'hidden',
      }}
    >
      {/* BMS Header - Replicates COTRN01.bms header section */}
      <Header
        transactionId="CT01"
        programName="COTRN01C"
        title="View Transaction"
      />

      {/* Main screen content */}
      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{
          padding: '16px',
          height: 'calc(100vh - 80px)', // Adjust for header height
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {/* Screen title */}
        <Box
          sx={{
            textAlign: 'center',
            color: '#FFFFFF', // Neutral color matching BMS
            fontWeight: 'bold',
            marginBottom: '16px',
            fontSize: '16px',
          }}
        >
          View Transaction
        </Box>

        {/* Transaction ID input section */}
        <Box
          sx={{
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Box
            component="label"
            sx={{
              color: '#4FC3F7', // Turquoise matching BMS TURQUOISE attribute
              minWidth: '140px',
            }}
          >
            Enter Tran ID:
          </Box>
          <Box
            component="input"
            type="text"
            name="transactionId"
            value={transactionId}
            onChange={handleTransactionIdChange}
            maxLength={16}
            autoFocus
            sx={{
              backgroundColor: 'transparent',
              border: 'none',
              borderBottom: '1px solid #00FF00',
              color: '#00FF00', // Green matching BMS GREEN attribute
              fontFamily: 'monospace',
              fontSize: '14px',
              width: '160px',
              padding: '2px 4px',
              outline: 'none',
              '&:focus': {
                borderBottom: '2px solid #00FF00',
              },
            }}
          />
        </Box>

        {/* Separator line */}
        <Box
          sx={{
            color: '#C0C0C0', // Neutral color
            marginBottom: '16px',
            fontSize: '12px',
          }}
        >
          {'─'.repeat(70)}
        </Box>

        {/* Transaction details section - Read-only fields */}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '8px 16px',
            flex: 1,
          }}
        >
          {/* Row 1: Transaction ID and Card Number */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '120px' }}>
              Transaction ID:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7', // Blue matching BMS BLUE attribute
                fontFamily: 'monospace',
                minWidth: '160px',
              }}
            >
              {transactionData.transactionId}
            </Box>
          </Box>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '100px' }}>
              Card Number:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '160px',
              }}
            >
              {transactionData.cardNumber}
            </Box>
          </Box>

          {/* Row 2: Type Code, Category Code, and Source */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '80px' }}>
              Type CD:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '40px',
                marginRight: '16px',
              }}
            >
              {transactionData.typeCode}
            </Box>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '100px' }}>
              Category CD:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '60px',
              }}
            >
              {transactionData.categoryCode}
            </Box>
          </Box>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '60px' }}>
              Source:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '100px',
              }}
            >
              {transactionData.source}
            </Box>
          </Box>

          {/* Row 3: Description (spans both columns) */}
          <Box 
            sx={{ 
              gridColumn: '1 / -1',
              display: 'flex', 
              alignItems: 'center', 
              gap: '8px',
              marginTop: '8px',
            }}
          >
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '100px' }}>
              Description:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                flex: 1,
              }}
            >
              {transactionData.description}
            </Box>
          </Box>

          {/* Row 4: Amount, Original Date, and Process Date */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '60px' }}>
              Amount:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '120px',
                textAlign: 'right',
              }}
            >
              {transactionData.amount}
            </Box>
          </Box>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '80px' }}>
              Orig Date:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '100px',
                marginRight: '16px',
              }}
            >
              {transactionData.originalDate}
            </Box>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '80px' }}>
              Proc Date:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '100px',
              }}
            >
              {transactionData.processDate}
            </Box>
          </Box>

          {/* Row 5: Merchant Information */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '100px' }}>
              Merchant ID:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '100px',
              }}
            >
              {transactionData.merchantId}
            </Box>
          </Box>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '120px' }}>
              Merchant Name:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                flex: 1,
              }}
            >
              {transactionData.merchantName}
            </Box>
          </Box>

          {/* Row 6: Merchant City and ZIP */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '120px' }}>
              Merchant City:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '200px',
              }}
            >
              {transactionData.merchantCity}
            </Box>
          </Box>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px' }}>
            <Box component="label" sx={{ color: '#4FC3F7', minWidth: '100px' }}>
              Merchant Zip:
            </Box>
            <Box
              sx={{
                color: '#4FC3F7',
                fontFamily: 'monospace',
                minWidth: '100px',
              }}
            >
              {transactionData.merchantZip}
            </Box>
          </Box>
        </Box>

        {/* Error message area - Matching BMS ERRMSG field */}
        <Box
          sx={{
            marginTop: 'auto',
            minHeight: '20px',
            color: '#FF0000', // Red color matching BMS RED attribute
            fontWeight: 'bold',
            marginBottom: '8px',
          }}
        >
          {errorMessage}
        </Box>

        {/* Footer with PF-key commands - Matching BMS footer */}
        <Box
          sx={{
            color: '#FFEB3B', // Yellow color matching BMS YELLOW attribute
            fontSize: '12px',
            borderTop: '1px solid #444',
            paddingTop: '8px',
          }}
        >
          ENTER=Fetch  F3=Back  F4=Clear  F5=Browse Tran.
        </Box>
      </Box>
    </Box>
  );
};

export default TransactionView;