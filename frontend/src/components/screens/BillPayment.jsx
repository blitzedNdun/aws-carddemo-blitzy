/**
 * BillPayment Component - React implementation of COBIL00 BMS mapset
 *
 * Converts the COBOL COBIL00C program and COBIL00.bms mapset to a React component
 * for processing customer bill payments and updating account balances.
 *
 * This component replicates the exact functionality of the mainframe bill payment screen:
 * - Account ID validation and account data retrieval
 * - Payment amount entry with currency formatting
 * - Real-time balance calculation and display
 * - Two-stage confirmation workflow
 * - Payment transaction creation and account balance updates
 *
 * Maps to CICS Transaction: CB00
 * Original COBOL Program: COBIL00C
 * Original BMS Mapset: COBIL00.bms
 */

import {
  Box,
  TextField,
  Button,
  Typography,
  Paper,
  Container,
  Alert,
  Checkbox,
  FormControlLabel,
  Grid,
  CircularProgress,
} from '@mui/material';
import { useFormik } from 'formik';
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import * as yup from 'yup';

// Internal imports from depends_on_files
import { postTransaction, getAccount } from '../../services/api.js';
import { formatCurrency } from '../../utils/CobolDataConverter.js';
import { validateDate } from '../../utils/validation.js';
import Header from '../common/Header.jsx';

/**
 * Validation schema matching COBOL field validation from COBIL00C
 * Replicates BMS field rules and COBOL edit checks
 */
const validationSchema = yup.object({
  // Account ID validation - 11-digit format matching ACTIDIN field
  accountId: yup
    .string()
    .required('Account ID is required')
    .matches(/^\d{11}$/, 'Account ID must be exactly 11 digits'),

  // Payment amount validation - positive number with 2 decimal places
  paymentAmount: yup
    .string()
    .required('Payment amount is required')
    .matches(/^\d+(\.\d{1,2})?$/, 'Invalid payment amount format'),

  // Confirmation validation - must be 'Y' to proceed
  confirmation: yup
    .string()
    .oneOf(['Y'], 'You must confirm the payment to proceed'),
});

/**
 * Handle keyboard events for PF-key functionality
 * Maps function keys to their COBOL equivalents
 */
const useKeyboardHandlers = (navigate, formik, accountData, showConfirmation, handleAccountLookup) => {
  const handleKeyDown = useCallback((event) => {
    switch (event.key) {
      case 'F3':
        event.preventDefault();
        navigate(-1);
        break;
      case 'F5':
        event.preventDefault();
        if (formik.isValid && accountData) {
          formik.handleSubmit();
        }
        break;
      case 'Enter':
        if (!accountData && formik.values.accountId.length === 11) {
          handleAccountLookup(formik.values.accountId);
        } else if (accountData && formik.values.paymentAmount && !showConfirmation) {
          // This will be handled by the parent component
        }
        break;
    }
  }, [navigate, formik, accountData, showConfirmation, handleAccountLookup]);

  return handleKeyDown;
};

/**
 * Create payment submission handler
 * Extracts complex payment processing logic
 */
const createPaymentHandler = (params) => {
  const {
    accountData, showConfirmation, setShowConfirmation, setIsLoading, setError,
    setPaymentProcessed, setSuccess, setCurrentBalance, newBalance, formik,
  } = params;
  return async (values) => {
    if (!accountData) {
      setError('Please enter a valid account ID first');
      return;
    }

    if (!showConfirmation) {
      setShowConfirmation(true);
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const transactionData = {
        accountId: values.accountId,
        transactionType: 'PAYMENT',
        transactionAmount: parseFloat(values.paymentAmount),
        transactionDesc: 'Bill Payment',
        cardNumber: accountData.cardNumber || '0000000000000000',
      };

      const result = await postTransaction(transactionData);

      if (result.success) {
        setPaymentProcessed(true);
        setSuccess(`Payment processed successfully. Transaction ID: ${result.transactionId}`);
        setCurrentBalance(newBalance);
        setShowConfirmation(false);
        formik.setFieldValue('confirmation', '');
      } else {
        throw new Error(result.error || 'Payment processing failed');
      }
    } catch (err) {
      setError(err.message || 'Error processing payment');
      setShowConfirmation(false);
    } finally {
      setIsLoading(false);
    }
  };
};

/**
 * Custom hook for bill payment form management
 * Extracts complex form logic from main component
 */
const useBillPaymentForm = (navigate) => {
  const [accountData, setAccountData] = useState(null);
  const [currentBalance, setCurrentBalance] = useState(0);
  const [newBalance, setNewBalance] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [paymentProcessed, setPaymentProcessed] = useState(false);

  // Create payment handler with proper dependencies
  const handlePayment = createPaymentHandler({
    accountData, setError, showConfirmation, setShowConfirmation,
    setIsLoading, setSuccess, setNewBalance, setPaymentProcessed,
  });

  const formik = useFormik({
    initialValues: {
      accountId: '',
      paymentAmount: '',
      confirmation: '',
    },
    validationSchema,
    onSubmit: handlePayment,
  });

  /**
   * Handle account ID lookup - replicates COBOL 1100-GET-ACCT-DATA paragraph
   * Fetches account data when valid account ID is entered
   */
  const handleAccountLookup = useCallback(async (accountId) => {
    if (!accountId || accountId.length !== 11) {
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      // Use the API service to fetch account data
      const result = await getAccount(accountId);

      if (result.success) {
        setAccountData(result.data);
        setCurrentBalance(result.data.balance || 0);

        // Calculate new balance if payment amount exists
        if (formik.values.paymentAmount) {
          const paymentAmount = parseFloat(formik.values.paymentAmount) || 0;
          setNewBalance(result.data.balance - paymentAmount);
        }
      } else {
        throw new Error(result.error || 'Account not found or invalid account ID');
      }
    } catch (err) {
      setError(err.message || 'Error retrieving account information');
      setAccountData(null);
      setCurrentBalance(0);
      setNewBalance(0);
    } finally {
      setIsLoading(false);
    }
  }, [formik.values.paymentAmount, setAccountData, setCurrentBalance, setError, setIsLoading, setNewBalance]);

  /**
   * Calculate new balance when payment amount changes
   * Replicates COBOL balance calculation logic
   */
  const calculateNewBalance = useCallback(() => {
    if (accountData && formik.values.paymentAmount) {
      const paymentAmount = parseFloat(formik.values.paymentAmount) || 0;
      const newBal = currentBalance - paymentAmount;
      setNewBalance(newBal);
    } else {
      setNewBalance(currentBalance);
    }
  }, [accountData, currentBalance, formik.values.paymentAmount, setNewBalance]);

  // Handle Enter key for confirmation
  const handleEnterKey = useCallback(() => {
    if (accountData && formik.values.paymentAmount && !showConfirmation) {
      setShowConfirmation(true);
    }
  }, [accountData, formik.values.paymentAmount, showConfirmation, setShowConfirmation]);

  // Use keyboard handlers hook
  const handleKeyDown = useKeyboardHandlers(navigate, formik, accountData, showConfirmation, handleAccountLookup);

  // Effect for account lookup when account ID changes
  useEffect(() => {
    if (formik.values.accountId.length === 11) {
      handleAccountLookup(formik.values.accountId);
    }
  }, [formik.values.accountId, handleAccountLookup]);

  // Effect for balance calculation when payment amount changes
  useEffect(() => {
    calculateNewBalance();
  }, [calculateNewBalance]);

  // Effect for keyboard event listeners
  useEffect(() => {
    const keyHandler = (event) => {
      handleKeyDown(event);
      if (event.key === 'Enter' && accountData && formik.values.paymentAmount && !showConfirmation) {
        handleEnterKey();
      }
    };

    window.addEventListener('keydown', keyHandler);
    return () => {
      window.removeEventListener('keydown', keyHandler);
    };
  }, [handleKeyDown, handleEnterKey, accountData, formik.values.paymentAmount, showConfirmation]);

  // Effect for date validation - ensure current date is valid for transaction processing
  useEffect(() => {
    const currentDate = new Date();
    const dateString = currentDate.toLocaleDateString('en-US', {
      month: '2-digit',
      day: '2-digit',
      year: '2-digit',
    });

    // Validate current date format for transaction timestamp
    if (!validateDate(dateString)) {
      console.warn('Date validation warning: Invalid date format for transaction processing');
    }
  }, []);

  return {
    accountData, setAccountData, currentBalance, setCurrentBalance,
    newBalance, setNewBalance, isLoading, setIsLoading, error, setError,
    success, setSuccess, showConfirmation, setShowConfirmation,
    paymentProcessed, setPaymentProcessed, formik, handlePayment,
  };
};

/**
 * BillPayment Component
 *
 * Implements the bill payment screen functionality with the following features:
 * - Account ID input and validation
 * - Current balance display
 * - Payment amount entry
 * - New balance calculation
 * - Payment confirmation
 * - Transaction processing
 * - PF-key navigation support
 */
const BillPayment = () => {
  // Navigation hook for PF-key functionality
  const navigate = useNavigate();

  // Use comprehensive custom hook for all form management
  const {
    accountData, setAccountData, currentBalance, setCurrentBalance,
    newBalance, setNewBalance, isLoading, error,
    success, setSuccess, showConfirmation, setShowConfirmation,
    paymentProcessed, setPaymentProcessed, formik,
  } = useBillPaymentForm(navigate);

  return (
    <Container maxWidth="lg" sx={{ mt: 2, mb: 4 }}>
      {/* BMS Header replication - matches COBIL00.bms header layout */}
      <Header
        transactionId="CB00"
        programName="COBIL00C"
        title="Bill Payment"
      />

      <Paper elevation={3} sx={{ p: 4, mt: 3, fontFamily: 'monospace' }}>
        <form onSubmit={formik.handleSubmit}>
          <Grid container spacing={3}>
            {/* Account ID Input Field - maps to ACTIDIN from BMS */}
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                id="accountId"
                name="accountId"
                label="Account ID"
                value={formik.values.accountId}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.accountId && Boolean(formik.errors.accountId)}
                helperText={formik.touched.accountId && formik.errors.accountId}
                inputProps={{
                  maxLength: 11,
                  pattern: '[0-9]*',
                  style: { fontFamily: 'monospace' },
                }}
                sx={{ mb: 2 }}
                disabled={paymentProcessed}
              />
            </Grid>

            {/* Current Balance Display - maps to CURBAL from BMS */}
            {accountData && (
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Current Balance"
                  value={formatCurrency(currentBalance)}
                  InputProps={{
                    readOnly: true,
                    style: { fontFamily: 'monospace', color: '#2E7D32' },
                  }}
                  sx={{ mb: 2 }}
                />
              </Grid>
            )}

            {/* Payment Amount Input Field - maps to PAYAMT from BMS */}
            {accountData && !paymentProcessed && (
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  id="paymentAmount"
                  name="paymentAmount"
                  label="Payment Amount"
                  value={formik.values.paymentAmount}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.paymentAmount && Boolean(formik.errors.paymentAmount)}
                  helperText={formik.touched.paymentAmount && formik.errors.paymentAmount}
                  inputProps={{
                    pattern: '[0-9.]*',
                    style: { fontFamily: 'monospace' },
                  }}
                  sx={{ mb: 2 }}
                />
              </Grid>
            )}

            {/* New Balance Display - maps to NEWBAL from BMS */}
            {accountData && formik.values.paymentAmount && (
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="New Balance"
                  value={formatCurrency(newBalance)}
                  InputProps={{
                    readOnly: true,
                    style: {
                      fontFamily: 'monospace',
                      color: newBalance >= 0 ? '#2E7D32' : '#D32F2F',
                    },
                  }}
                  sx={{ mb: 2 }}
                />
              </Grid>
            )}

            {/* Confirmation Checkbox - maps to CONFIRM from BMS */}
            {showConfirmation && (
              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={formik.values.confirmation === 'Y'}
                      onChange={(e) => formik.setFieldValue('confirmation', e.target.checked ? 'Y' : '')}
                      sx={{ color: '#1976d2' }}
                    />
                  }
                  label="I confirm this payment is correct and authorize the transaction"
                  sx={{
                    fontFamily: 'monospace',
                    color: '#1976d2',
                    mb: 2,
                  }}
                />
              </Grid>
            )}

            {/* Loading Indicator */}
            {isLoading && (
              <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
                <CircularProgress
                  size={40}
                  sx={{ color: '#1976d2' }}
                />
              </Grid>
            )}

            {/* Error Message Display - maps to ERRMSGO from BMS */}
            {error && (
              <Grid item xs={12}>
                <Alert severity="error" sx={{ mb: 2, fontFamily: 'monospace' }}>
                  {error}
                </Alert>
              </Grid>
            )}

            {/* Success Message Display - maps to INFMSGO from BMS */}
            {success && (
              <Grid item xs={12}>
                <Alert severity="success" sx={{ mb: 2, fontFamily: 'monospace' }}>
                  {success}
                </Alert>
              </Grid>
            )}

            {/* Action Buttons */}
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-start' }}>
                {!paymentProcessed && (
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={!formik.isValid || !accountData || isLoading}
                    sx={{
                      fontFamily: 'monospace',
                      minWidth: '120px',
                    }}
                  >
                    {showConfirmation ? 'Process Payment (F5)' : 'Continue (Enter)'}
                  </Button>
                )}

                <Button
                  variant="outlined"
                  onClick={() => navigate(-1)}
                  sx={{
                    fontFamily: 'monospace',
                    minWidth: '120px',
                  }}
                >
                  Exit (F3)
                </Button>

                {paymentProcessed && (
                  <Button
                    variant="contained"
                    onClick={() => {
                      // Reset form for new payment
                      formik.resetForm();
                      setAccountData(null);
                      setCurrentBalance(0);
                      setNewBalance(0);
                      setPaymentProcessed(false);
                      setSuccess('');
                      setShowConfirmation(false);
                    }}
                    sx={{
                      fontFamily: 'monospace',
                      minWidth: '120px',
                    }}
                  >
                    New Payment
                  </Button>
                )}
              </Box>
            </Grid>
          </Grid>
        </form>

        {/* PF-Key Help Text */}
        <Box sx={{ mt: 4, p: 2, backgroundColor: '#f5f5f5', fontFamily: 'monospace' }}>
          <Typography variant="body2" sx={{ color: '#666' }}>
            Function Keys: F3=Exit | F5=Process Payment | Enter=Continue
          </Typography>
        </Box>
      </Paper>
    </Container>
  );
};

export default BillPayment;
