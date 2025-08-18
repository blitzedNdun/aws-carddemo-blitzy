/**
 * CreditCardList Component - React implementation of COCRDLI BMS mapset
 *
 * Replicates the Credit Card List screen functionality from the COBOL program COCRDLIC.
 * Provides paginated browsing of credit card portfolio with search capability.
 * Displays 7 cards per page with selection functionality and PF-key navigation.
 *
 * BMS Mapset Conversion: COCRDLI → CreditCardList.jsx
 * COBOL Program: COCRDLIC.cbl
 * Transaction: CCLI
 *
 * Features:
 * - Search by Account Number and/or Card Number
 * - Paginated display (7 cards per page)
 * - Card selection capability
 * - PF-key navigation (F3=Exit, F7=Previous Page, F8=Next Page)
 * - Standard BMS header layout
 * - Information and error message areas
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  TextField,
  Button,
  Typography,
} from '@mui/material';

// Internal imports matching schema requirements
import { listCards } from '../../services/api.js';
import { formatDecimal } from '../../utils/CobolDataConverter.js';
import Header from '../common/Header.jsx';

/**
 * CreditCardList Component - Main functional component
 * Maps to COCRDLI BMS mapset with 7-row card display and pagination
 */
const CreditCardList = () => {
  // Navigation hook for PF-key handling (CICS XCTL replacement)
  const navigate = useNavigate();

  // Component state management matching COBOL program data areas
  const [cards, setCards] = useState([]); // Card data array (7 rows max)
  const [currentPage, setCurrentPage] = useState(1); // Page number (PAGENO field)
  const [totalPages, setTotalPages] = useState(1); // Total pages calculated
  const [loading, setLoading] = useState(false); // Processing indicator
  const [selectedCards, setSelectedCards] = useState(new Set()); // Selected card tracking (CRDSEL1-7)
  
  // Search filter state (matching BMS input fields)
  const [accountNumberFilter, setAccountNumberFilter] = useState(''); // ACCTSID field
  const [cardNumberFilter, setCardNumberFilter] = useState(''); // CARDSID field
  
  // Message display state (matching BMS message fields)
  const [infoMessage, setInfoMessage] = useState(''); // INFOMSG field
  const [errorMessage, setErrorMessage] = useState(''); // ERRMSG field

  /**
   * Fetch credit cards data from backend API
   * Maps to COBOL STARTBR/READNEXT operations on CARDDAT file
   * 
   * @param {number} page - Page number to fetch (1-based)
   * @param {string} accountFilter - Account number filter
   * @param {string} cardFilter - Card number filter
   */
  const fetchCards = useCallback(async (page = 1, accountFilter = '', cardFilter = '') => {
    try {
      setLoading(true);
      setErrorMessage('');
      setInfoMessage('');

      // Prepare search parameters matching COBOL program logic
      const searchParams = {
        page: page,
        pageSize: 7, // BMS mapset shows 7 rows (CRDSEL1-CRDSEL7)
        accountNumber: accountFilter.trim() || undefined,
        cardNumber: cardFilter.trim() || undefined,
      };

      // Remove undefined parameters to match COBOL conditional logic
      Object.keys(searchParams).forEach(key => {
        if (searchParams[key] === undefined) {
          delete searchParams[key];
        }
      });

      // Call backend API (replaces CICS transaction CCLI)
      const response = await listCards(searchParams);

      if (response && response.success) {
        const cardData = response.data || [];
        setCards(cardData);
        setCurrentPage(page);
        setTotalPages(response.totalPages || 1);
        
        // Clear previous selections when loading new data
        setSelectedCards(new Set());
        
        // Set info message if no cards found
        if (cardData.length === 0) {
          setInfoMessage('No credit cards found matching your criteria');
        } else {
          setInfoMessage(`Displaying ${cardData.length} card(s) - Page ${page} of ${response.totalPages || 1}`);
        }
      } else {
        // Handle API error response
        setCards([]);
        setErrorMessage(response?.message || 'Failed to retrieve credit card data');
        setInfoMessage('');
      }
    } catch (error) {
      // Handle network or unexpected errors
      console.error('Error fetching credit cards:', error);
      setCards([]);
      setErrorMessage('System error occurred while retrieving credit card data');
      setInfoMessage('');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Initial data load on component mount
   * Replicates COBOL program initialization (0000-INIT paragraph)
   */
  useEffect(() => {
    fetchCards(1, accountNumberFilter, cardNumberFilter);
  }, [fetchCards, accountNumberFilter, cardNumberFilter]);

  /**
   * Handle search form submission
   * Maps to COBOL input validation and file positioning logic
   */
  const handleSearch = useCallback((event) => {
    if (event) {
      event.preventDefault();
    }
    
    // Reset to first page when searching
    fetchCards(1, accountNumberFilter, cardNumberFilter);
  }, [fetchCards, accountNumberFilter, cardNumberFilter]);

  /**
   * Handle card selection toggle
   * Maps to CRDSEL1-CRDSEL7 field handling in BMS mapset
   * 
   * @param {string} cardId - Card identifier for selection
   */
  const handleCardSelection = useCallback((cardId) => {
    setSelectedCards(prev => {
      const newSelection = new Set(prev);
      if (newSelection.has(cardId)) {
        newSelection.delete(cardId);
      } else {
        newSelection.add(cardId);
      }
      return newSelection;
    });
  }, []);

  /**
   * Navigate to previous page
   * Maps to PF7 key functionality (F7=Backward in BMS)
   */
  const handlePreviousPage = useCallback(() => {
    if (currentPage > 1) {
      const prevPage = currentPage - 1;
      fetchCards(prevPage, accountNumberFilter, cardNumberFilter);
    }
  }, [currentPage, fetchCards, accountNumberFilter, cardNumberFilter]);

  /**
   * Navigate to next page  
   * Maps to PF8 key functionality (F8=Forward in BMS)
   */
  const handleNextPage = useCallback(() => {
    if (currentPage < totalPages) {
      const nextPage = currentPage + 1;
      fetchCards(nextPage, accountNumberFilter, cardNumberFilter);
    }
  }, [currentPage, totalPages, fetchCards, accountNumberFilter, cardNumberFilter]);

  /**
   * Handle keyboard navigation for PF-key functionality
   * Replicates CICS PF-key processing in COBOL programs
   */
  const handleKeyDown = useCallback((event) => {
    // Handle function keys (F3, F7, F8)
    if (event.altKey) {
      switch (event.key) {
        case 'F3':
        case '3':
          // F3=Exit - Navigate to main menu (CICS XCTL COMEN01C)
          event.preventDefault();
          navigate('/menu');
          break;
        case 'F7':
        case '7':
          // F7=Backward - Previous page
          event.preventDefault();
          handlePreviousPage();
          break;
        case 'F8':
        case '8':
          // F8=Forward - Next page
          event.preventDefault();
          handleNextPage();
          break;
        default:
          break;
      }
    }
    
    // Handle Enter key for search
    if (event.key === 'Enter') {
      handleSearch(event);
    }
  }, [navigate, handlePreviousPage, handleNextPage, handleSearch]);

  /**
   * Set up keyboard event listeners for PF-key handling
   */
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  /**
   * Handle card detail navigation
   * Maps to CICS XCTL COCRDSLC (card detail program)
   * 
   * @param {object} card - Selected card data
   */
  const handleCardDetail = useCallback((card) => {
    if (card && card.cardNumber) {
      navigate(`/card/${card.cardNumber}`);
    }
  }, [navigate]);

  /**
   * Format account number for display
   * Uses COBOL data conversion utilities for consistent formatting
   * 
   * @param {string} accountNumber - Raw account number
   * @returns {string} Formatted account number
   */
  const formatAccountNumber = useCallback((accountNumber) => {
    if (!accountNumber) return '';
    
    // Use COBOL data converter to maintain formatting consistency
    return formatDecimal(accountNumber, 0);
  }, []);

  /**
   * Format card number for display with masking
   * Replicates COBOL card number masking logic
   * 
   * @param {string} cardNumber - Raw card number  
   * @returns {string} Masked card number (showing last 4 digits)
   */
  const formatCardNumber = useCallback((cardNumber) => {
    if (!cardNumber) return '';
    
    // Mask all but last 4 digits for security (COBOL masking logic)
    if (cardNumber.length > 4) {
      const masked = '*'.repeat(cardNumber.length - 4);
      const lastFour = cardNumber.slice(-4);
      return `${masked}${lastFour}`;
    }
    return cardNumber;
  }, []);

  /**
   * Format card status for display
   * Maps COBOL status codes to user-friendly display
   * 
   * @param {string} status - Raw status code
   * @returns {string} Formatted status display
   */
  const formatCardStatus = useCallback((status) => {
    if (!status) return '';
    
    // Convert COBOL status codes to display values
    switch (status.toUpperCase()) {
      case 'Y':
      case 'A':
      case 'ACTIVE':
        return 'Y';
      case 'N':
      case 'I':
      case 'INACTIVE':
        return 'N';
      default:
        return status.charAt(0).toUpperCase();
    }
  }, []);

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: '100vh',
        backgroundColor: '#000000', // 3270 terminal black background
        color: '#00FF00', // Default green text
        fontFamily: 'monospace',
        fontSize: '14px',
        padding: 0,
        margin: 0,
      }}
    >
      {/* BMS Header Section - Maps to COCRDLI header fields */}
      <Header
        transactionId="CCLI"
        programName="COCRDLIC"
        title="List Credit Cards"
      />

      {/* Main Content Area */}
      <Box sx={{ padding: '16px 8px' }}>
        {/* Search Filter Section - Maps to ACCTSID and CARDSID input fields */}
        <Box sx={{ marginBottom: '16px' }}>
          <form onSubmit={handleSearch}>
            <Box sx={{ display: 'flex', gap: '16px', alignItems: 'center', marginBottom: '8px' }}>
              {/* Account Number Filter - ACCTSID field */}
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Typography
                  sx={{
                    color: '#40E0D0', // Turquoise matching BMS TURQUOISE attribute
                    fontFamily: 'monospace',
                    fontSize: 'inherit',
                    marginRight: '8px',
                    minWidth: '140px',
                  }}
                >
                  Account Number:
                </Typography>
                <TextField
                  value={accountNumberFilter}
                  onChange={(e) => setAccountNumberFilter(e.target.value)}
                  placeholder="Enter account number"
                  variant="outlined"
                  size="small"
                  inputProps={{
                    maxLength: 11, // COBOL PIC 9(11) length
                    style: {
                      fontFamily: 'monospace',
                      fontSize: '14px',
                      color: '#00FF00',
                      backgroundColor: '#000000',
                    },
                  }}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      backgroundColor: '#000000',
                      '& fieldset': {
                        borderColor: '#00FF00',
                      },
                      '&:hover fieldset': {
                        borderColor: '#40E0D0',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#40E0D0',
                      },
                    },
                  }}
                />
              </Box>

              {/* Card Number Filter - CARDSID field */}
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Typography
                  sx={{
                    color: '#40E0D0', // Turquoise matching BMS TURQUOISE attribute
                    fontFamily: 'monospace',
                    fontSize: 'inherit',
                    marginRight: '8px',
                    minWidth: '140px',
                  }}
                >
                  Credit Card Number:
                </Typography>
                <TextField
                  value={cardNumberFilter}
                  onChange={(e) => setCardNumberFilter(e.target.value)}
                  placeholder="Enter card number"
                  variant="outlined"
                  size="small"
                  inputProps={{
                    maxLength: 16, // Standard credit card number length
                    style: {
                      fontFamily: 'monospace',
                      fontSize: '14px',
                      color: '#00FF00',
                      backgroundColor: '#000000',
                    },
                  }}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      backgroundColor: '#000000',
                      '& fieldset': {
                        borderColor: '#00FF00',
                      },
                      '&:hover fieldset': {
                        borderColor: '#40E0D0',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#40E0D0',
                      },
                    },
                  }}
                />
              </Box>

              {/* Search Button */}
              <Button
                type="submit"
                variant="outlined"
                disabled={loading}
                sx={{
                  color: '#00FF00',
                  borderColor: '#00FF00',
                  fontFamily: 'monospace',
                  '&:hover': {
                    borderColor: '#40E0D0',
                    color: '#40E0D0',
                  },
                }}
              >
                {loading ? 'Searching...' : 'Search'}
              </Button>
            </Box>
          </form>
        </Box>

        {/* Page Number Display - Maps to PAGENO field */}
        <Box sx={{ textAlign: 'right', marginBottom: '16px' }}>
          <Typography
            sx={{
              color: '#00FF00',
              fontFamily: 'monospace',
              fontSize: 'inherit',
            }}
          >
            Page {currentPage}
          </Typography>
        </Box>

        {/* Credit Card Data Table - Maps to 7-row display (CRDSEL1-7, ACCTNO1-7, CRDNUM1-7, CRDSTS1-7) */}
        <Box sx={{ marginBottom: '16px' }}>
          <Table
            sx={{
              width: '100%',
              '& .MuiTableCell-root': {
                borderColor: '#404040',
                padding: '4px 8px',
                fontFamily: 'monospace',
                fontSize: '14px',
              },
            }}
          >
            {/* Table Header */}
            <TableHead>
              <TableRow>
                <TableCell
                  sx={{
                    color: '#00FF00',
                    backgroundColor: '#000000',
                    width: '80px',
                  }}
                >
                  Select
                </TableCell>
                <TableCell
                  sx={{
                    color: '#00FF00',
                    backgroundColor: '#000000',
                    width: '140px',
                  }}
                >
                  Account Number
                </TableCell>
                <TableCell
                  sx={{
                    color: '#00FF00',
                    backgroundColor: '#000000',
                    width: '200px',
                  }}
                >
                  Card Number
                </TableCell>
                <TableCell
                  sx={{
                    color: '#00FF00',
                    backgroundColor: '#000000',
                    width: '80px',
                  }}
                >
                  Active
                </TableCell>
              </TableRow>
            </TableHead>

            {/* Table Body - 7 rows maximum matching BMS mapset */}
            <TableBody>
              {/* Render up to 7 card rows */}
              {Array.from({ length: 7 }, (_, index) => {
                const card = cards[index];
                const cardId = card ? card.cardNumber || `${card.accountNumber}-${index}` : null;
                const isSelected = cardId ? selectedCards.has(cardId) : false;

                return (
                  <TableRow
                    key={index}
                    sx={{
                      backgroundColor: card ? '#000000' : '#000000',
                      '&:hover': {
                        backgroundColor: card ? '#1a1a1a' : '#000000',
                      },
                    }}
                  >
                    {/* Selection Column - CRDSEL fields */}
                    <TableCell
                      sx={{
                        color: '#00FF00',
                        textAlign: 'center',
                        cursor: card ? 'pointer' : 'default',
                      }}
                      onClick={() => card && handleCardSelection(cardId)}
                    >
                      {card ? (isSelected ? 'X' : ' ') : ' '}
                    </TableCell>

                    {/* Account Number Column - ACCTNO fields */}
                    <TableCell
                      sx={{
                        color: '#00FF00',
                        cursor: card ? 'pointer' : 'default',
                      }}
                      onClick={() => card && handleCardDetail(card)}
                    >
                      {card ? formatAccountNumber(card.accountNumber) : ''}
                    </TableCell>

                    {/* Card Number Column - CRDNUM fields (masked) */}
                    <TableCell
                      sx={{
                        color: '#00FF00',
                        cursor: card ? 'pointer' : 'default',
                      }}
                      onClick={() => card && handleCardDetail(card)}
                    >
                      {card ? formatCardNumber(card.cardNumber) : ''}
                    </TableCell>

                    {/* Status Column - CRDSTS fields */}
                    <TableCell
                      sx={{
                        color: '#00FF00',
                        textAlign: 'center',
                      }}
                    >
                      {card ? formatCardStatus(card.cardStatus) : ''}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Box>

        {/* Information Message Area - Maps to INFOMSG field */}
        {infoMessage && (
          <Box sx={{ marginBottom: '8px' }}>
            <Typography
              sx={{
                color: '#00FF00', // Neutral color matching BMS NEUTRAL attribute
                fontFamily: 'monospace',
                fontSize: 'inherit',
                textAlign: 'center',
              }}
            >
              {infoMessage}
            </Typography>
          </Box>
        )}

        {/* Error Message Area - Maps to ERRMSG field */}
        {errorMessage && (
          <Box sx={{ marginBottom: '8px' }}>
            <Typography
              sx={{
                color: '#FF0000', // Red color matching BMS RED attribute
                fontFamily: 'monospace',
                fontSize: 'inherit',
                fontWeight: 'bold',
                textAlign: 'center',
              }}
            >
              {errorMessage}
            </Typography>
          </Box>
        )}

        {/* PF-Key Navigation Footer - Maps to BMS function key display */}
        <Box
          sx={{
            marginTop: '24px',
            padding: '8px',
            borderTop: '1px solid #404040',
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            {/* PF-Key Instructions */}
            <Typography
              sx={{
                color: '#40E0D0', // Turquoise matching BMS TURQUOISE attribute
                fontFamily: 'monospace',
                fontSize: 'inherit',
              }}
            >
              F3=Exit F7=Backward F8=Forward
            </Typography>

            {/* Navigation Buttons */}
            <Box sx={{ display: 'flex', gap: '8px' }}>
              <Button
                onClick={() => navigate('/menu')}
                variant="outlined"
                size="small"
                sx={{
                  color: '#00FF00',
                  borderColor: '#00FF00',
                  fontFamily: 'monospace',
                  minWidth: '80px',
                  '&:hover': {
                    borderColor: '#40E0D0',
                    color: '#40E0D0',
                  },
                }}
              >
                F3=Exit
              </Button>
              
              <Button
                onClick={handlePreviousPage}
                disabled={currentPage <= 1}
                variant="outlined"
                size="small"
                sx={{
                  color: currentPage <= 1 ? '#666666' : '#00FF00',
                  borderColor: currentPage <= 1 ? '#666666' : '#00FF00',
                  fontFamily: 'monospace',
                  minWidth: '120px',
                  '&:hover': {
                    borderColor: currentPage <= 1 ? '#666666' : '#40E0D0',
                    color: currentPage <= 1 ? '#666666' : '#40E0D0',
                  },
                }}
              >
                F7=Previous
              </Button>
              
              <Button
                onClick={handleNextPage}
                disabled={currentPage >= totalPages}
                variant="outlined"
                size="small"
                sx={{
                  color: currentPage >= totalPages ? '#666666' : '#00FF00',
                  borderColor: currentPage >= totalPages ? '#666666' : '#00FF00',
                  fontFamily: 'monospace',
                  minWidth: '100px',
                  '&:hover': {
                    borderColor: currentPage >= totalPages ? '#666666' : '#40E0D0',
                    color: currentPage >= totalPages ? '#666666' : '#40E0D0',
                  },
                }}
              >
                F8=Next
              </Button>
            </Box>
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default CreditCardList;