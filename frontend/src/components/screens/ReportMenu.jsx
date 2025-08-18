/**
 * ReportMenu.jsx - React component for Report Menu screen (CORPT00)
 *
 * Migrated from COBOL program CORPT00C and BMS mapset CORPT00.bms.
 * Provides report generation options with three report types: Monthly, Yearly, and Custom date range.
 * Implements comprehensive form validation, PF-key navigation, and seamless REST API integration
 * for report generation requests to the Spring Boot backend.
 *
 * Key Features:
 * - Radio button selection for report types (Monthly, Yearly, Custom)
 * - Date range validation for custom reports with MM/DD/YYYY format
 * - Print confirmation checkbox matching COBOL Y/N field validation
 * - PF-key event handling (F3=Exit, Enter=Generate Report)
 * - Real-time form validation using Formik and Yup schemas
 * - Error message display with Material-UI Alert components
 * - COBOL-compatible date formatting and validation
 *
 * Original COBOL Logic:
 * - CORPT00C program handled report type validation and JCL job submission
 * - Custom date range validation using CSUTLDTC utility for leap year calculations
 * - COMMAREA-based session state management for report parameters
 * - Transient Data Queue (JOBS) submission for batch report generation
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useFormik } from 'formik';
import * as yup from 'yup';
import { Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';

// Internal imports from depends_on_files
import { getReports } from '../../services/api.js';
import { validateDate } from '../../utils/validation.js';
import { displayFormat } from '../../utils/CobolDataConverter.js';
import Header from '../common/Header.jsx';

/**
 * ReportMenu Component - Provides report generation interface matching CORPT00 functionality
 *
 * Converts BMS mapset CORPT00 to React component with form validation, PF-key navigation,
 * and REST API integration for report generation. Maintains exact functional parity with
 * original COBOL program CORPT00C.
 */
const ReportMenu = () => {
  // Navigation hook for PF-key functionality
  const navigate = useNavigate();

  // Component state management
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState('');

  /**
   * Yup validation schema matching COBOL field validation from CORPT00C
   * Replicates original BMS field rules and CSUTLDTC date validation logic
   */
  const validationSchema = yup.object({
    reportType: yup
      .string()
      .required('Report type selection is required')
      .oneOf(['MONTHLY', 'YEARLY', 'CUSTOM'], 'Invalid report type selected'),
    
    startDate: yup
      .string()
      .when('reportType', {
        is: 'CUSTOM',
        then: (schema) => schema
          .required('Start date is required for custom reports')
          .test('valid-date', 'Invalid start date format (MM/DD/YYYY)', function(value) {
            if (!value) return false;
            const dateValidation = validateDate(value, 'MM/DD/YYYY');
            return dateValidation.isValid;
          })
          .test('date-range', 'Start date must be before end date', function(value) {
            if (!value || !this.parent.endDate) return true;
            const startDateObj = new Date(value);
            const endDateObj = new Date(this.parent.endDate);
            return startDateObj < endDateObj;
          }),
        otherwise: (schema) => schema.notRequired()
      }),
    
    endDate: yup
      .string()
      .when('reportType', {
        is: 'CUSTOM',
        then: (schema) => schema
          .required('End date is required for custom reports')
          .test('valid-date', 'Invalid end date format (MM/DD/YYYY)', function(value) {
            if (!value) return false;
            const dateValidation = validateDate(value, 'MM/DD/YYYY');
            return dateValidation.isValid;
          })
          .test('date-range', 'End date must be after start date', function(value) {
            if (!value || !this.parent.startDate) return true;
            const startDateObj = new Date(this.parent.startDate);
            const endDateObj = new Date(value);
            return endDateObj > startDateObj;
          })
          .test('max-range', 'Date range cannot exceed 366 days', function(value) {
            if (!value || !this.parent.startDate) return true;
            const startDateObj = new Date(this.parent.startDate);
            const endDateObj = new Date(value);
            const diffTime = Math.abs(endDateObj - startDateObj);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            return diffDays <= 366;
          }),
        otherwise: (schema) => schema.notRequired()
      }),
    
    printConfirm: yup
      .boolean()
      .required('Print confirmation is required')
  });

  /**
   * Formik form management with initial values matching BMS field defaults
   */
  const formik = useFormik({
    initialValues: {
      reportType: '',
      startDate: '',
      endDate: '',
      printConfirm: false
    },
    validationSchema,
    onSubmit: async (values) => {
      await handleSubmit(values);
    }
  });

  /**
   * Handle form submission - Generate report via REST API
   * Replicates COBOL CORPT00C report generation logic with JCL job submission
   */
  const handleSubmit = async (formValues) => {
    setIsLoading(true);
    setSubmitError('');
    setSubmitSuccess('');

    try {
      // Build report request matching COMMAREA structure from CORPT00C
      const reportRequest = {
        reportType: formValues.reportType,
        startDate: formValues.startDate || null,
        endDate: formValues.endDate || null,
        printConfirm: formValues.printConfirm ? 'Y' : 'N',
        transactionId: 'CR00',
        programName: 'CORPT00C'
      };

      // Call REST API (replaces CICS WRITEQ TD to JOBS queue)
      const response = await getReports(reportRequest);

      if (response.success) {
        setSubmitSuccess(`Report ${response.reportId} has been successfully generated and queued for processing.`);
        // Reset form after successful submission
        formik.resetForm();
      } else {
        setSubmitError(response.message || 'Report generation failed. Please try again.');
      }
    } catch (error) {
      console.error('Report generation error:', error);
      setSubmitError('System error occurred. Please contact support if the problem persists.');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * PF-Key event handler matching original 3270 terminal key mapping
   * F3=Exit, Enter=Generate Report, F12=Cancel
   */
  const handleKeyDown = useCallback((event) => {
    switch (event.key) {
      case 'F3':
        event.preventDefault();
        navigate(-1); // Go back to previous screen (Main Menu)
        break;
      case 'Enter':
        event.preventDefault();
        if (formik.isValid && !isLoading) {
          formik.handleSubmit();
        }
        break;
      case 'F12':
        event.preventDefault();
        formik.resetForm();
        break;
      default:
        break;
    }
  }, [navigate, formik, isLoading]);

  /**
   * Register PF-key event listeners on component mount
   * Replicates CICS PF-key handling from CORPT00C
   */
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  /**
   * Format date input display using COBOL-compatible formatting
   */
  const formatDateDisplay = (value, picClause = 'X(10)') => {
    return displayFormat(value || '', picClause);
  };

  /**
   * Handle report type change - Clear date fields when switching from custom
   */
  const handleReportTypeChange = (event) => {
    const newReportType = event.target.value;
    formik.setFieldValue('reportType', newReportType);
    
    // Clear date fields if not custom report type
    if (newReportType !== 'CUSTOM') {
      formik.setFieldValue('startDate', '');
      formik.setFieldValue('endDate', '');
    }
  };

  return (
    <div style={{ 
      fontFamily: 'monospace', 
      backgroundColor: '#000000', 
      color: '#00FF00', 
      minHeight: '100vh',
      padding: '0'
    }}>
      {/* Header component replicating BMS screen header layout */}
      <Header 
        transactionId="CR00"
        programName="CORPT00C"
        title="TRANSACTION REPORTS MENU"
      />

      {/* Main form content area */}
      <div style={{ padding: '20px' }}>
        {/* Success message display */}
        {submitSuccess && (
          <Alert 
            severity="success" 
            sx={{ 
              marginBottom: '16px',
              backgroundColor: '#1B5E20',
              color: '#4CAF50'
            }}
            onClose={() => setSubmitSuccess('')}
          >
            {submitSuccess}
          </Alert>
        )}

        {/* Error message display (replaces ERRMSG field from CORPT00.bms) */}
        {submitError && (
          <Alert 
            severity="error" 
            sx={{ 
              marginBottom: '16px',
              backgroundColor: '#B71C1C',
              color: '#F44336'
            }}
            onClose={() => setSubmitError('')}
          >
            {submitError}
          </Alert>
        )}

        {/* Report generation form */}
        <form onSubmit={formik.handleSubmit}>
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ 
              color: '#FFEB3B', 
              fontSize: '16px',
              marginBottom: '16px',
              fontFamily: 'monospace'
            }}>
              SELECT REPORT TYPE:
            </h3>

            {/* Report Type Selection - Radio buttons matching BMS field options */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '8px', color: '#4FC3F7' }}>
                <input
                  type="radio"
                  name="reportType"
                  value="MONTHLY"
                  checked={formik.values.reportType === 'MONTHLY'}
                  onChange={handleReportTypeChange}
                  style={{ marginRight: '8px' }}
                />
                MONTHLY - Generate current month transaction report
              </label>
              
              <label style={{ display: 'block', marginBottom: '8px', color: '#4FC3F7' }}>
                <input
                  type="radio"
                  name="reportType"
                  value="YEARLY"
                  checked={formik.values.reportType === 'YEARLY'}
                  onChange={handleReportTypeChange}
                  style={{ marginRight: '8px' }}
                />
                YEARLY - Generate current year transaction report
              </label>
              
              <label style={{ display: 'block', marginBottom: '8px', color: '#4FC3F7' }}>
                <input
                  type="radio"
                  name="reportType"
                  value="CUSTOM"
                  checked={formik.values.reportType === 'CUSTOM'}
                  onChange={handleReportTypeChange}
                  style={{ marginRight: '8px' }}
                />
                CUSTOM - Generate report for specific date range
              </label>
            </div>

            {/* Report type validation error */}
            {formik.touched.reportType && formik.errors.reportType && (
              <div style={{ color: '#F44336', fontSize: '14px', marginBottom: '8px' }}>
                {formik.errors.reportType}
              </div>
            )}
          </div>

          {/* Custom Date Range Fields - Only shown when CUSTOM is selected */}
          {formik.values.reportType === 'CUSTOM' && (
            <div style={{ marginBottom: '24px' }}>
              <h4 style={{ 
                color: '#FFEB3B', 
                fontSize: '14px',
                marginBottom: '12px',
                fontFamily: 'monospace'
              }}>
                CUSTOM DATE RANGE (MM/DD/YYYY):
              </h4>

              <div style={{ display: 'flex', gap: '20px', marginBottom: '16px' }}>
                {/* Start Date Input (SDTMM/SDTDD/SDTYYYY from BMS) */}
                <div>
                  <label style={{ 
                    display: 'block', 
                    marginBottom: '4px', 
                    color: '#4FC3F7',
                    fontSize: '14px'
                  }}>
                    Start Date:
                  </label>
                  <input
                    type="text"
                    name="startDate"
                    placeholder="MM/DD/YYYY"
                    value={formik.values.startDate}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    maxLength="10"
                    style={{
                      backgroundColor: '#000000',
                      color: '#00FF00',
                      border: '1px solid #4FC3F7',
                      padding: '4px 8px',
                      fontFamily: 'monospace',
                      fontSize: '14px',
                      width: '120px'
                    }}
                  />
                  {formik.touched.startDate && formik.errors.startDate && (
                    <div style={{ color: '#F44336', fontSize: '12px', marginTop: '4px' }}>
                      {formik.errors.startDate}
                    </div>
                  )}
                </div>

                {/* End Date Input (EDTMM/EDTDD/EDTYYYY from BMS) */}
                <div>
                  <label style={{ 
                    display: 'block', 
                    marginBottom: '4px', 
                    color: '#4FC3F7',
                    fontSize: '14px'
                  }}>
                    End Date:
                  </label>
                  <input
                    type="text"
                    name="endDate"
                    placeholder="MM/DD/YYYY"
                    value={formik.values.endDate}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    maxLength="10"
                    style={{
                      backgroundColor: '#000000',
                      color: '#00FF00',
                      border: '1px solid #4FC3F7',
                      padding: '4px 8px',
                      fontFamily: 'monospace',
                      fontSize: '14px',
                      width: '120px'
                    }}
                  />
                  {formik.touched.endDate && formik.errors.endDate && (
                    <div style={{ color: '#F44336', fontSize: '12px', marginTop: '4px' }}>
                      {formik.errors.endDate}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Print Confirmation Checkbox (CONFIRM field from BMS) */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{ 
              display: 'flex', 
              alignItems: 'center', 
              color: '#4FC3F7',
              fontSize: '14px'
            }}>
              <input
                type="checkbox"
                name="printConfirm"
                checked={formik.values.printConfirm}
                onChange={formik.handleChange}
                style={{ marginRight: '8px' }}
              />
              Print report immediately upon generation (Y/N)
            </label>
            {formik.touched.printConfirm && formik.errors.printConfirm && (
              <div style={{ color: '#F44336', fontSize: '12px', marginTop: '4px' }}>
                {formik.errors.printConfirm}
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div style={{ marginTop: '32px' }}>
            <button
              type="submit"
              disabled={isLoading || !formik.isValid}
              style={{
                backgroundColor: isLoading ? '#424242' : '#1976D2',
                color: '#FFFFFF',
                border: 'none',
                padding: '8px 16px',
                marginRight: '12px',
                fontFamily: 'monospace',
                fontSize: '14px',
                cursor: isLoading ? 'not-allowed' : 'pointer'
              }}
            >
              {isLoading ? 'GENERATING...' : 'GENERATE REPORT (Enter)'}
            </button>

            <button
              type="button"
              onClick={() => navigate(-1)}
              style={{
                backgroundColor: '#757575',
                color: '#FFFFFF',
                border: 'none',
                padding: '8px 16px',
                marginRight: '12px',
                fontFamily: 'monospace',
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              EXIT (F3)
            </button>

            <button
              type="button"
              onClick={() => formik.resetForm()}
              style={{
                backgroundColor: '#D32F2F',
                color: '#FFFFFF',
                border: 'none',
                padding: '8px 16px',
                fontFamily: 'monospace',
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              CLEAR (F12)
            </button>
          </div>
        </form>

        {/* PF-Key Instructions */}
        <div style={{ 
          marginTop: '32px', 
          borderTop: '1px solid #4FC3F7', 
          paddingTop: '16px',
          fontSize: '12px',
          color: '#4FC3F7'
        }}>
          <div>PF-KEY INSTRUCTIONS:</div>
          <div>F3=Exit   Enter=Generate Report   F12=Clear Form</div>
        </div>
      </div>
    </div>
  );
};

export default ReportMenu;