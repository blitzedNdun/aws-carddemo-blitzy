/**
 * TransactionList.jsx - React Component for Transaction List Screen (COTRN00)
 *
 * Converts BMS mapset COTRN00 to React component with Material-UI DataGrid for transaction
 * list display. Shows 10 rows per page with selection checkboxes (SEL0001-SEL0010).
 * Displays transaction fields: ID, date, description, amount. Implements search by
 * transaction ID with real-time filtering. Adds pagination controls with PF-key mapping
 * (F7=Previous, F8=Next, F3=Exit). Shows page number indicator.
 *
 * Maps COBOL program COTRN00C business logic:
 * - Cursor-based pagination (STARTBR/READNEXT/READPREV equivalent)
 * - Transaction selection with 'S' for details navigation
 * - Search functionality with numeric validation
 * - PF-key navigation matching original CICS behavior
 * - Error handling and message display
 * - Page tracking and boundary detection
 *
 * Key COBOL Program Flow Replication:
 * - PROCESS-ENTER-KEY: Selection handling and search processing
 * - PROCESS-PF7-KEY: Previous page navigation
 * - PROCESS-PF8-KEY: Next page navigation
 * - POPULATE-TRAN-DATA: Data formatting and display
 * - STARTBR-TRANSACT-FILE: API pagination initiation
 */

import { Box } from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

// Internal imports - ONLY from depends_on_files
import { apiService } from '../../services/api.js';
import { formatCurrency } from '../../utils/CobolDataConverter.js';
import Header from '../common/Header.jsx';

/**
 * TransactionList Component - Maps to CICS transaction CT00 (COTRN00C program)
 *
 * Replicates BMS mapset COTRN00 functionality with modern React/Material-UI interface:
 * - Header with transaction info (CT00, COTRN00C, "List Transactions")
 * - Search field for transaction ID filtering (TRNIDIN equivalent)
 * - DataGrid with 10 rows per page showing transaction data
 * - Selection handling for navigation to transaction details
 * - PF-key navigation support (F3, F7, F8)
 * - Page number display and boundary handling
 * - Error message display area (ERRMSG equivalent)
 */
const TransactionList = () => {
  // ========================================================================
  // STATE MANAGEMENT - Replicating COBOL working storage variables
  // ========================================================================

  // Transaction data and pagination state (WS-variables equivalent)
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pageNumber, setPageNumber] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [hasPreviousPage, setHasPreviousPage] = useState(false);

  // Search and filtering state (TRNIDIN equivalent)
  const [searchTransactionId, setSearchTransactionId] = useState('');
  const [appliedSearchFilter, setAppliedSearchFilter] = useState('');

  // Selection state (SEL0001-SEL0010 equivalent)
  const [selectionModel, setSelectionModel] = useState([]);

  // Error handling state (ERRMSG equivalent)
  const [errorMessage, setErrorMessage] = useState('');

  // Navigation hook for PF-key functionality
  const navigate = useNavigate();

  // Constants replicating COBOL constants
  const PAGE_SIZE = 10; // Matches BMS mapset with 10 transaction rows
  const TRANSACTION_ID = 'CT00'; // WS-TRANID
  const PROGRAM_NAME = 'COTRN00C'; // WS-PGMNAME
  const SCREEN_TITLE = 'List Transactions'; // BMS title

  // ========================================================================
  // DATA FETCHING - Replicating COBOL file operations
  // ========================================================================

  /**
   * Fetch transactions from API - Replicates COBOL PROCESS-PAGE-FORWARD logic
   * Maps to STARTBR-TRANSACT-FILE and READNEXT-TRANSACT-FILE operations
   * Implements cursor-based pagination with search filtering
   */
  const fetchTransactions = useCallback(async (page = 1, searchFilter = '') => {
    setLoading(true);
    setErrorMessage('');

    try {
      // Build search criteria matching COBOL parameter passing
      const criteria = {
        pageNumber: page,
        pageSize: PAGE_SIZE,
        sortBy: 'transactionDate',
        sortOrder: 'desc',
      };

      // Add transaction ID filter if provided (TRNIDINI processing)
      if (searchFilter && searchFilter.trim()) {
        // Validate numeric input matching COBOL validation
        if (!/^\d+$/.test(searchFilter.trim())) {
          setErrorMessage('Tran ID must be Numeric ...');
          setLoading(false);
          return;
        }
        criteria.transactionId = searchFilter.trim();
      }

      // Call API service (replaces EXEC CICS STARTBR/READNEXT)
      const response = await apiService.getTransactions(criteria);

      if (response.success && response.data) {
        // Extract transaction data and pagination info
        const { transactions: transactionData, pagination } = response.data;

        // Format transaction data for DataGrid display
        const formattedTransactions = (transactionData || []).map((transaction, index) => ({
          id: transaction.transactionId || `row-${page}-${index}`, // DataGrid requires unique id
          transactionId: transaction.transactionId || '',
          date: formatTransactionDate(transaction.transactionDate || transaction.originalTimestamp),
          description: truncateDescription(transaction.description || ''),
          amount: formatCurrency(transaction.amount || 0),
          rawAmount: transaction.amount || 0,
          // Additional fields for navigation
          accountId: transaction.accountId,
          cardNumber: transaction.cardNumber,
        }));

        // Update state with fetched data
        setTransactions(formattedTransactions);
        setTotalCount(pagination?.totalCount || 0);
        setPageNumber(page);

        // Set pagination flags (replicates NEXT-PAGE-YES/NO logic)
        setHasNextPage(pagination?.hasNextPage || false);
        setHasPreviousPage(page > 1);

        // Clear any previous error messages
        setErrorMessage('');

      } else {
        // Handle API errors (replicates COBOL error handling)
        setErrorMessage(response.error || 'Unable to lookup transaction...');
        setTransactions([]);
        setTotalCount(0);
        setHasNextPage(false);
        setHasPreviousPage(false);
      }

    } catch (error) {
      // Handle network/system errors (replicates COBOL ABEND handling)
      console.error('Error fetching transactions:', error);
      setErrorMessage('Unable to lookup transaction...');
      setTransactions([]);
      setTotalCount(0);
      setHasNextPage(false);
      setHasPreviousPage(false);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Format transaction date for display - Replicates COBOL date formatting logic
   * Converts ISO timestamp to MM/DD/YY format matching BMS display
   */
  const formatTransactionDate = (dateString) => {
    if (!dateString) {return '';}

    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {return '';}

      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const year = String(date.getFullYear()).slice(-2);

      return `${month}/${day}/${year}`;
    } catch (error) {
      return '';
    }
  };

  /**
   * Truncate description to match BMS field length (26 characters)
   * Replicates COBOL field truncation behavior
   */
  const truncateDescription = (description) => {
    if (!description) {return '';}
    return description.length > 26 ? `${description.substring(0, 23)}...` : description;
  };

  // ========================================================================
  // EVENT HANDLERS - Replicating COBOL paragraph logic
  // ========================================================================

  /**
   * Handle search functionality - Replicates PROCESS-ENTER-KEY logic
   * Validates transaction ID input and applies search filter
   */
  const handleSearch = useCallback(() => {
    const searchValue = searchTransactionId.trim();

    // Reset to first page for new search
    setPageNumber(1);
    setAppliedSearchFilter(searchValue);

    // Fetch transactions with search filter
    fetchTransactions(1, searchValue);
  }, [searchTransactionId, fetchTransactions]);

  /**
   * Handle search input changes with real-time validation
   * Replicates COBOL field validation logic
   */
  const handleSearchInputChange = useCallback((event) => {
    const value = event.target.value;
    setSearchTransactionId(value);

    // Clear error message when user starts typing
    if (errorMessage) {
      setErrorMessage('');
    }
  }, [errorMessage]);

  /**
   * Handle Enter key press in search field - Replicates DFHENTER processing
   */
  const handleSearchKeyPress = useCallback((event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleSearch();
    }
  }, [handleSearch]);

  /**
   * Handle transaction detail navigation - Replicates COTRN01C transfer
   * Calls getTransactionDetail and navigates to detail screen
   */
  const handleTransactionDetail = useCallback(async (transactionId) => {
    try {
      // Call API service to validate transaction exists (optional validation)
      const response = await apiService.getTransactionDetail(transactionId);

      if (response.success) {
        // Navigate to transaction detail screen
        navigate(`/transaction-detail/${transactionId}`, {
          state: {
            fromScreen: 'transaction-list',
            returnPath: '/transaction-list',
            transactionId,
          },
        });
      } else {
        setErrorMessage('Invalid selection. Valid value is S');
      }
    } catch (error) {
      console.error('Error navigating to transaction detail:', error);
      setErrorMessage('Invalid selection. Valid value is S');
    }
  }, [navigate]);

  /**
   * Handle row selection - Replicates COBOL selection processing (SEL0001-SEL0010)
   * Processes transaction selection for navigation to details
   */
  const handleSelectionChange = useCallback((newSelection) => {
    if (newSelection.length > 0) {
      const selectedId = newSelection[0];
      const selectedTransaction = transactions.find(t => t.id === selectedId);

      if (selectedTransaction) {
        setSelectionModel([selectedId]);

        // Navigate to transaction details (replicates EXEC CICS XCTL to COTRN01C)
        const transactionId = selectedTransaction.transactionId;
        if (transactionId) {
          // Call API service for transaction detail navigation
          handleTransactionDetail(transactionId);
        }
      }
    } else {
      setSelectionModel([]);
    }
  }, [transactions, handleTransactionDetail]);

  /**
   * Handle page navigation - Replicates PROCESS-PF7-KEY and PROCESS-PF8-KEY
   */
  const handlePageChange = useCallback((newPage) => {
    if (newPage !== pageNumber) {
      fetchTransactions(newPage, appliedSearchFilter);
    }
  }, [pageNumber, appliedSearchFilter, fetchTransactions]);

  /**
   * Handle previous page - Replicates PROCESS-PF7-KEY logic
   */
  const handlePreviousPage = useCallback(() => {
    if (hasPreviousPage) {
      const prevPage = pageNumber - 1;
      handlePageChange(prevPage);
    } else {
      setErrorMessage('You are already at the top of the page...');
    }
  }, [pageNumber, hasPreviousPage, handlePageChange]);

  /**
   * Handle next page - Replicates PROCESS-PF8-KEY logic
   */
  const handleNextPage = useCallback(() => {
    if (hasNextPage) {
      const nextPage = pageNumber + 1;
      handlePageChange(nextPage);
    } else {
      setErrorMessage('You are already at the bottom of the page...');
    }
  }, [pageNumber, hasNextPage, handlePageChange]);

  /**
   * Handle back navigation - Replicates DFHPF3 processing (return to main menu)
   */
  const handleBackToMenu = useCallback(() => {
    navigate('/main-menu', {
      state: {
        fromScreen: 'transaction-list',
      },
    });
  }, [navigate]);

  // ========================================================================
  // KEYBOARD EVENT HANDLING - Replicating PF-key functionality
  // ========================================================================

  /**
   * Handle global keyboard events for PF-key navigation
   * Replicates CICS PF-key processing (F3, F7, F8)
   */
  const handleKeyDown = useCallback((event) => {
    switch (event.key) {
      case 'F3':
        event.preventDefault();
        handleBackToMenu();
        break;
      case 'F7':
        event.preventDefault();
        handlePreviousPage();
        break;
      case 'F8':
        event.preventDefault();
        handleNextPage();
        break;
      default:
        // Allow other keys to propagate normally
        break;
    }
  }, [handleBackToMenu, handlePreviousPage, handleNextPage]);

  // ========================================================================
  // EFFECTS - Component lifecycle and data loading
  // ========================================================================

  /**
   * Initial data load on component mount
   * Replicates COBOL program initialization
   */
  useEffect(() => {
    fetchTransactions(1, '');
  }, [fetchTransactions]);

  /**
   * Set up keyboard event listeners for PF-key support
   * Replicates CICS keyboard handling
   */
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  // ========================================================================
  // DATAGRID CONFIGURATION - Replicating BMS mapset layout
  // ========================================================================

  /**
   * DataGrid column configuration - Replicates BMS field layout
   * Maps to COTRN00 mapset columns: Sel, Transaction ID, Date, Description, Amount
   */
  const columns = useMemo(() => [
    {
      field: 'transactionId',
      headerName: 'Transaction ID',
      width: 160,
      sortable: false,
      renderCell: (params) => (
        <span style={{ fontFamily: 'monospace', fontSize: '13px' }}>
          {params.value || ''}
        </span>
      ),
    },
    {
      field: 'date',
      headerName: 'Date',
      width: 100,
      sortable: false,
      renderCell: (params) => (
        <span style={{ fontFamily: 'monospace', fontSize: '13px' }}>
          {params.value || ''}
        </span>
      ),
    },
    {
      field: 'description',
      headerName: 'Description',
      width: 280,
      sortable: false,
      renderCell: (params) => (
        <span style={{ fontFamily: 'monospace', fontSize: '13px' }}>
          {params.value || ''}
        </span>
      ),
    },
    {
      field: 'amount',
      headerName: 'Amount',
      width: 120,
      sortable: false,
      align: 'right',
      renderCell: (params) => (
        <span style={{ fontFamily: 'monospace', fontSize: '13px', textAlign: 'right' }}>
          {params.value || ''}
        </span>
      ),
    },
  ], []);

  // ========================================================================
  // RENDER - Component UI matching BMS screen layout
  // ========================================================================

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        backgroundColor: '#000000', // 3270 terminal background
        color: '#00FF00', // 3270 terminal text color
        fontFamily: 'monospace',
      }}
    >
      {/* Header Section - Replicates BMS header layout */}
      <Header
        transactionId={TRANSACTION_ID}
        programName={PROGRAM_NAME}
        title={SCREEN_TITLE}
      />

      {/* Main Content Area */}
      <Box
        sx={{
          flex: 1,
          padding: 2,
          backgroundColor: '#FFFFFF', // White background for data area
          color: '#000000', // Black text for data area
        }}
      >
        {/* Page Title Section */}
        <Box
          sx={{
            textAlign: 'center',
            marginBottom: 2,
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#000080', // Blue color matching BMS
          }}
        >
          List Transactions
          <Box component="span" sx={{ marginLeft: 4, fontSize: '14px', color: '#008080' }}>
            Page: {pageNumber}
          </Box>
        </Box>

        {/* Search Section - Replicates TRNIDIN field */}
        <Box
          sx={{
            marginBottom: 2,
            display: 'flex',
            alignItems: 'center',
            gap: 1,
          }}
        >
          <Box component="span" sx={{ color: '#008080', minWidth: '120px' }}>
            Search Tran ID:
          </Box>
          <input
            type="text"
            value={searchTransactionId}
            onChange={handleSearchInputChange}
            onKeyPress={handleSearchKeyPress}
            placeholder="Enter transaction ID"
            style={{
              border: '1px solid #008080',
              padding: '4px 8px',
              fontFamily: 'monospace',
              fontSize: '14px',
              width: '200px',
              backgroundColor: '#FFFFFF',
              color: '#000000',
            }}
            maxLength={16}
          />
          <button
            onClick={handleSearch}
            style={{
              padding: '4px 12px',
              backgroundColor: '#008080',
              color: '#FFFFFF',
              border: 'none',
              cursor: 'pointer',
              fontFamily: 'monospace',
              fontSize: '14px',
            }}
          >
            Search
          </button>
        </Box>

        {/* Error Message Display - Replicates ERRMSG field */}
        {errorMessage && (
          <Box
            sx={{
              marginBottom: 2,
              padding: 1,
              backgroundColor: '#FFE6E6',
              color: '#FF0000',
              border: '1px solid #FF0000',
              fontFamily: 'monospace',
              fontSize: '14px',
            }}
          >
            {errorMessage}
          </Box>
        )}

        {/* Transaction Data Grid - Replicates BMS transaction rows */}
        <Box sx={{ height: 450, width: '100%' }}>
          <DataGrid
            rows={transactions}
            columns={columns}
            pageSize={PAGE_SIZE}
            pagination={false} // Custom pagination controls
            checkboxSelection
            disableSelectionOnClick={false}
            onSelectionModelChange={handleSelectionChange}
            selectionModel={selectionModel}
            loading={loading}
            hideFooter // Custom footer with navigation controls
            sx={{
              '& .MuiDataGrid-root': {
                fontFamily: 'monospace',
              },
              '& .MuiDataGrid-columnHeaders': {
                backgroundColor: '#F0F0F0',
                color: '#000000',
                fontWeight: 'bold',
              },
              '& .MuiDataGrid-row': {
                '&:hover': {
                  backgroundColor: '#E6F3FF',
                },
              },
              '& .MuiDataGrid-cell': {
                fontFamily: 'monospace',
                fontSize: '13px',
              },
            }}
          />
        </Box>

        {/* Pagination Controls - Replicates PF-key navigation */}
        <Box
          sx={{
            marginTop: 2,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: 1,
            backgroundColor: '#F5F5F5',
            borderRadius: 1,
          }}
        >
          <Box sx={{ fontSize: '14px', color: '#000080' }}>
            Showing {transactions.length} of {totalCount} transactions
          </Box>

          <Box sx={{ display: 'flex', gap: 1 }}>
            <button
              onClick={handlePreviousPage}
              disabled={!hasPreviousPage}
              style={{
                padding: '6px 12px',
                backgroundColor: hasPreviousPage ? '#008080' : '#CCCCCC',
                color: '#FFFFFF',
                border: 'none',
                cursor: hasPreviousPage ? 'pointer' : 'not-allowed',
                fontFamily: 'monospace',
                fontSize: '12px',
              }}
            >
              F7=Backward
            </button>

            <button
              onClick={handleNextPage}
              disabled={!hasNextPage}
              style={{
                padding: '6px 12px',
                backgroundColor: hasNextPage ? '#008080' : '#CCCCCC',
                color: '#FFFFFF',
                border: 'none',
                cursor: hasNextPage ? 'pointer' : 'not-allowed',
                fontFamily: 'monospace',
                fontSize: '12px',
              }}
            >
              F8=Forward
            </button>
          </Box>
        </Box>

        {/* Instructions Section - Replicates BMS instructions */}
        <Box
          sx={{
            marginTop: 2,
            textAlign: 'center',
            fontSize: '14px',
            color: '#000080',
            fontFamily: 'monospace',
          }}
        >
          Type &apos;S&apos; to View Transaction details from the list
        </Box>

        {/* Function Keys Help - Replicates BMS function key display */}
        <Box
          sx={{
            marginTop: 1,
            textAlign: 'center',
            fontSize: '12px',
            color: '#808000',
            fontFamily: 'monospace',
            backgroundColor: '#FFFACD',
            padding: 1,
            borderRadius: 1,
          }}
        >
          ENTER=Continue  F3=Back  F7=Backward  F8=Forward
        </Box>
      </Box>
    </Box>
  );
};

export default TransactionList;
