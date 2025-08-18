/**
 * Credit Card Update Screen (COCRDUP) - React Component
 * 
 * Replicates the COBOL program COCRDUPC and BMS mapset COCRDUP for credit card information
 * maintenance and status management. Provides editable form fields for card details,
 * controlled status transitions, expiry date validation, and account-card relationship
 * management with business rule enforcement.
 * 
 * Original CICS Transaction: CCUP
 * Original Program: COCRDUPC
 * Original Mapset: COCRDUP
 * 
 * Business Rules (from COBOL):
 * - Account number (11 digits) and card number (16 digits) are read-only
 * - Card name: alphanumeric + spaces, max 25 characters
 * - Status: Y (Active) or N (Inactive) only
 * - Expiry month: 1-12
 * - Expiry year: 1950-2099, must be future date
 * - Status transitions follow business rules for account safety
 * 
 * PF-Key Mapping:
 * - F3=Cancel (exit to card list)
 * - F5=Save (save without exiting)
 * - F12=Cancel (same as F3)
 * - Enter=Submit (validate and save)
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Formik, Form, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  FormControl,
  FormLabel,
  Select,
  MenuItem,
  Alert,
  Grid,
  InputLabel,
  FormHelperText,
} from '@mui/material';
import { styled } from '@mui/material/styles';

// Internal dependencies (matching schema requirements)
import { apiService } from '../../services/api.js';
import { createDateValidationSchema } from '../../utils/validation.js';
import { displayFormat } from '../../utils/CobolDataConverter.js';
import Header from '../../components/common/Header.jsx';

// Styled components for 3270 terminal appearance
const TerminalCard = styled(Card)(({ theme }) => ({
  backgroundColor: '#000000',
  color: '#00FF00',
  fontFamily: 'monospace',
  border: '2px solid #00FF00',
  borderRadius: 0,
  minHeight: '600px',
}));

const TerminalTextField = styled(TextField)(({ theme }) => ({
  '& .MuiInputBase-root': {
    backgroundColor: '#000000',
    color: '#00FF00',
    fontFamily: 'monospace',
    fontSize: '14px',
    border: '1px solid #00FF00',
    borderRadius: 0,
  },
  '& .MuiInputLabel-root': {
    color: '#4FC3F7',
    fontFamily: 'monospace',
    fontSize: '14px',
  },
  '& .MuiFormHelperText-root': {
    color: '#FF5722',
    fontFamily: 'monospace',
    fontSize: '12px',
  },
  '& .MuiInputBase-input': {
    padding: '8px 12px',
  },
}));

const TerminalSelect = styled(Select)(({ theme }) => ({
  '& .MuiSelect-select': {
    backgroundColor: '#000000',
    color: '#00FF00',
    fontFamily: 'monospace',
    fontSize: '14px',
    border: '1px solid #00FF00',
    borderRadius: 0,
    padding: '8px 12px',
  },
  '& .MuiInputLabel-root': {
    color: '#4FC3F7',
    fontFamily: 'monospace',
    fontSize: '14px',
  },
}));

/**
 * Credit Card Update Component
 * Main component function implementing the COCRDUP screen functionality
 */
const CreditCardUpdate = () => {
  const navigate = useNavigate();
  const { cardNumber } = useParams();
  
  // State management
  const [cardData, setCardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Status code options (from COBOL validation - Y or N only)
  const statusOptions = [
    { value: 'Y', label: 'Y - Active' },
    { value: 'N', label: 'N - Inactive' }
  ];

  // Initial form values
  const initialValues = {
    accountId: '',
    cardNumber: '',
    cardName: '',
    status: '',
    expiryMonth: '',
    expiryYear: ''
  };

  // Validation schema (replicating COBOL validation rules)
  const validationSchema = Yup.object({
    cardName: Yup.string()
      .max(25, 'Card name cannot exceed 25 characters')
      .matches(/^[A-Za-z0-9\s]*$/, 'Card name can only contain letters, numbers, and spaces')
      .required('Card name is required'),
    status: Yup.string()
      .oneOf(['Y', 'N'], 'Status must be Y (Active) or N (Inactive)')
      .required('Status is required'),
    expiryMonth: Yup.number()
      .required('Expiry month is required')
      .min(1, 'Month must be between 1 and 12')
      .max(12, 'Month must be between 1 and 12')
      .integer('Month must be a whole number')
      .positive('Month must be positive'),
    expiryYear: Yup.number()
      .required('Expiry year is required')
      .min(1950, 'Year must be between 1950 and 2099')
      .max(2099, 'Year must be between 1950 and 2099')
      .integer('Year must be a whole number')
      .test('future-date', 'Expiry date must be in the future', function(value) {
        const { expiryMonth } = this.parent;
        if (!value || !expiryMonth) return true;
        
        const currentDate = new Date();
        const expiryDate = new Date(value, expiryMonth - 1, 1);
        const currentMonthStart = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
        
        return expiryDate >= currentMonthStart;
      })
  });

  /**
   * Load card data on component mount
   */
  useEffect(() => {
    const loadCardData = async () => {
      if (!cardNumber) {
        setError('Card number is required');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        
        const response = await apiService.getCard(cardNumber);
        setCardData(response.data);
      } catch (err) {
        const errorMessage = apiService.handleApiError(err);
        setError(errorMessage);
      } finally {
        setLoading(false);
      }
    };

    loadCardData();
  }, [cardNumber]);

  /**
   * Handle form submission (Enter key or Submit button)
   */
  const handleSubmit = useCallback(async (values, { setSubmitting, setFieldError }) => {
    try {
      setSubmitting(true);
      setError(null);
      setSaveSuccess(false);

      // Prepare update data (matching COBOL COMMAREA structure)
      const updateData = {
        cardName: values.cardName.trim().toUpperCase(),
        status: values.status,
        expiryMonth: parseInt(values.expiryMonth, 10),
        expiryYear: parseInt(values.expiryYear, 10)
      };

      await apiService.updateCard(cardNumber, updateData);
      setSaveSuccess(true);
      
      // Navigate back to card list after successful update (replicating CICS XCTL)
      setTimeout(() => {
        navigate('/cards');
      }, 2000);
      
    } catch (err) {
      const errorMessage = apiService.handleApiError(err);
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  }, [cardNumber, navigate]);

  /**
   * Handle save without exit (F5 key functionality)
   */
  const handleSave = useCallback(async (values, { setSubmitting }) => {
    try {
      setSubmitting(true);
      setError(null);
      setSaveSuccess(false);

      const updateData = {
        cardName: values.cardName.trim().toUpperCase(),
        status: values.status,
        expiryMonth: parseInt(values.expiryMonth, 10),
        expiryYear: parseInt(values.expiryYear, 10)
      };

      await apiService.updateCard(cardNumber, updateData);
      setSaveSuccess(true);
      
    } catch (err) {
      const errorMessage = apiService.handleApiError(err);
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  }, [cardNumber]);

  /**
   * Handle cancel/exit (F3 and F12 key functionality)
   */
  const handleCancel = useCallback(() => {
    navigate('/cards');
  }, [navigate]);

  /**
   * PF-Key event handler (replicating 3270 terminal PF-key functionality)
   */
  const handleKeyDown = useCallback((event) => {
    // F3 = Cancel/Exit
    if (event.key === 'F3' || (event.altKey && event.key === '3')) {
      event.preventDefault();
      handleCancel();
    }
    // F5 = Save
    else if (event.key === 'F5' || (event.altKey && event.key === '5')) {
      event.preventDefault();
      // Trigger save via form context if available
      const form = event.target.closest('form');
      if (form) {
        const submitButton = form.querySelector('[data-save-button]');
        if (submitButton) {
          submitButton.click();
        }
      }
    }
    // F12 = Cancel (same as F3)
    else if (event.key === 'F12' || (event.altKey && event.key === 'F12')) {
      event.preventDefault();
      handleCancel();
    }
  }, [handleCancel]);

  // Attach keyboard event listener
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  // Loading state
  if (loading) {
    return (
      <Box sx={{ p: 2 }}>
        <Header 
          transactionId="CCUP" 
          programName="COCRDUPC" 
          title="Credit Card Update - Loading..." 
        />
        <TerminalCard>
          <CardContent>
            <Typography variant="h6" sx={{ color: '#FFEB3B', textAlign: 'center', mt: 4 }}>
              Loading card data...
            </Typography>
          </CardContent>
        </TerminalCard>
      </Box>
    );
  }

  // Error state
  if (error && !cardData) {
    return (
      <Box sx={{ p: 2 }}>
        <Header 
          transactionId="CCUP" 
          programName="COCRDUPC" 
          title="Credit Card Update - Error" 
        />
        <TerminalCard>
          <CardContent>
            <Alert severity="error" sx={{ mb: 2, backgroundColor: '#FF5722', color: '#FFFFFF' }}>
              {error}
            </Alert>
            <Button 
              variant="contained" 
              onClick={handleCancel}
              sx={{ 
                backgroundColor: '#4FC3F7', 
                color: '#000000', 
                fontFamily: 'monospace',
                '&:hover': { backgroundColor: '#29B6F6' }
              }}
            >
              F3=Return to Card List
            </Button>
          </CardContent>
        </TerminalCard>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2 }}>
      <Header 
        transactionId="CCUP" 
        programName="COCRDUPC" 
        title="Credit Card Update - Maintain Card Information" 
      />
      
      <TerminalCard>
        <CardContent sx={{ p: 3 }}>
          {/* Success message */}
          {saveSuccess && (
            <Alert 
              severity="success" 
              sx={{ 
                mb: 3, 
                backgroundColor: '#4CAF50', 
                color: '#FFFFFF',
                fontFamily: 'monospace'
              }}
            >
              Credit card information updated successfully
            </Alert>
          )}

          {/* Error message */}
          {error && (
            <Alert 
              severity="error" 
              sx={{ 
                mb: 3, 
                backgroundColor: '#FF5722', 
                color: '#FFFFFF',
                fontFamily: 'monospace'
              }}
            >
              {error}
            </Alert>
          )}

          <Formik
            initialValues={{
              accountId: cardData?.accountId || '',
              cardNumber: cardData?.cardNumber || '',
              cardName: cardData?.cardName || '',
              status: cardData?.status || '',
              expiryMonth: cardData?.expiryMonth || '',
              expiryYear: cardData?.expiryYear || ''
            }}
            validationSchema={validationSchema}
            onSubmit={handleSubmit}
            enableReinitialize={true}
          >
            {({ values, errors, touched, isSubmitting, setFieldValue }) => (
              <Form>
                <Grid container spacing={3}>
                  {/* Read-only fields */}
                  <Grid item xs={12} md={6}>
                    <FormControl fullWidth>
                      <TerminalTextField
                        label="Account ID"
                        name="accountId"
                        value={displayFormat(values.accountId, '9(11)')}
                        disabled
                        size="small"
                        InputProps={{
                          readOnly: true,
                          style: { color: '#4FC3F7' } // Blue for protected fields
                        }}
                      />
                    </FormControl>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <FormControl fullWidth>
                      <TerminalTextField
                        label="Card Number"
                        name="cardNumber"
                        value={displayFormat(values.cardNumber, '9(16)')}
                        disabled
                        size="small"
                        InputProps={{
                          readOnly: true,
                          style: { color: '#4FC3F7' } // Blue for protected fields
                        }}
                      />
                    </FormControl>
                  </Grid>

                  {/* Editable fields */}
                  <Grid item xs={12}>
                    <FormControl fullWidth error={errors.cardName && touched.cardName}>
                      <Field name="cardName">
                        {({ field, meta }) => (
                          <TerminalTextField
                            {...field}
                            label="Card Holder Name"
                            size="small"
                            error={meta.touched && !!meta.error}
                            helperText={meta.touched && meta.error}
                            inputProps={{ maxLength: 25 }}
                            placeholder="Enter cardholder name (max 25 characters)"
                          />
                        )}
                      </Field>
                    </FormControl>
                  </Grid>

                  <Grid item xs={12} md={4}>
                    <FormControl fullWidth error={errors.status && touched.status}>
                      <InputLabel 
                        sx={{ 
                          color: '#4FC3F7', 
                          fontFamily: 'monospace',
                          fontSize: '14px'
                        }}
                      >
                        Card Status
                      </InputLabel>
                      <Field name="status">
                        {({ field, meta }) => (
                          <TerminalSelect
                            {...field}
                            size="small"
                            error={meta.touched && !!meta.error}
                          >
                            {statusOptions.map((option) => (
                              <MenuItem 
                                key={option.value} 
                                value={option.value}
                                sx={{
                                  backgroundColor: '#000000',
                                  color: '#00FF00',
                                  fontFamily: 'monospace',
                                  '&:hover': { backgroundColor: '#1B5E20' }
                                }}
                              >
                                {option.label}
                              </MenuItem>
                            ))}
                          </TerminalSelect>
                        )}
                      </Field>
                      {errors.status && touched.status && (
                        <FormHelperText sx={{ color: '#FF5722', fontFamily: 'monospace' }}>
                          {errors.status}
                        </FormHelperText>
                      )}
                    </FormControl>
                  </Grid>

                  <Grid item xs={12} md={4}>
                    <FormControl fullWidth error={errors.expiryMonth && touched.expiryMonth}>
                      <Field name="expiryMonth">
                        {({ field, meta }) => (
                          <TerminalTextField
                            {...field}
                            label="Expiry Month"
                            type="number"
                            size="small"
                            error={meta.touched && !!meta.error}
                            helperText={meta.touched && meta.error}
                            inputProps={{ min: 1, max: 12 }}
                            placeholder="1-12"
                          />
                        )}
                      </Field>
                    </FormControl>
                  </Grid>

                  <Grid item xs={12} md={4}>
                    <FormControl fullWidth error={errors.expiryYear && touched.expiryYear}>
                      <Field name="expiryYear">
                        {({ field, meta }) => (
                          <TerminalTextField
                            {...field}
                            label="Expiry Year"
                            type="number"
                            size="small"
                            error={meta.touched && !!meta.error}
                            helperText={meta.touched && meta.error}
                            inputProps={{ min: 1950, max: 2099 }}
                            placeholder="YYYY"
                          />
                        )}
                      </Field>
                    </FormControl>
                  </Grid>

                  {/* Action buttons (replicating PF-key functionality) */}
                  <Grid item xs={12}>
                    <Box sx={{ display: 'flex', gap: 2, mt: 3, flexWrap: 'wrap' }}>
                      <Button
                        type="submit"
                        variant="contained"
                        disabled={isSubmitting}
                        sx={{
                          backgroundColor: '#4CAF50',
                          color: '#000000',
                          fontFamily: 'monospace',
                          fontWeight: 'bold',
                          minWidth: '120px',
                          '&:hover': { backgroundColor: '#66BB6A' },
                          '&:disabled': { backgroundColor: '#616161' }
                        }}
                      >
                        {isSubmitting ? 'Updating...' : 'ENTER=Submit'}
                      </Button>

                      <Button
                        type="button"
                        variant="contained"
                        disabled={isSubmitting}
                        data-save-button
                        onClick={() => handleSave(values, { setSubmitting: () => {} })}
                        sx={{
                          backgroundColor: '#FF9800',
                          color: '#000000',
                          fontFamily: 'monospace',
                          fontWeight: 'bold',
                          minWidth: '120px',
                          '&:hover': { backgroundColor: '#FFB74D' },
                          '&:disabled': { backgroundColor: '#616161' }
                        }}
                      >
                        F5=Save
                      </Button>

                      <Button
                        type="button"
                        variant="contained"
                        onClick={handleCancel}
                        sx={{
                          backgroundColor: '#F44336',
                          color: '#000000',
                          fontFamily: 'monospace',
                          fontWeight: 'bold',
                          minWidth: '120px',
                          '&:hover': { backgroundColor: '#EF5350' }
                        }}
                      >
                        F3=Cancel
                      </Button>

                      <Button
                        type="button"
                        variant="contained"
                        onClick={handleCancel}
                        sx={{
                          backgroundColor: '#9E9E9E',
                          color: '#000000',
                          fontFamily: 'monospace',
                          fontWeight: 'bold',
                          minWidth: '120px',
                          '&:hover': { backgroundColor: '#BDBDBD' }
                        }}
                      >
                        F12=Cancel
                      </Button>
                    </Box>
                  </Grid>

                  {/* Instructions */}
                  <Grid item xs={12}>
                    <Typography 
                      variant="caption" 
                      sx={{ 
                        color: '#4FC3F7', 
                        fontFamily: 'monospace',
                        display: 'block',
                        mt: 2,
                        borderTop: '1px solid #4FC3F7',
                        pt: 2
                      }}
                    >
                      Instructions: Modify card information and press ENTER to submit changes, 
                      F5 to save without exiting, or F3/F12 to cancel and return to card list.
                    </Typography>
                  </Grid>
                </Grid>
              </Form>
            )}
          </Formik>
        </CardContent>
      </TerminalCard>
    </Box>
  );
};

export default CreditCardUpdate;