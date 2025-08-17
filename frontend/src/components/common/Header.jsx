/**
 * Header Component - Reusable React header that replicates BMS screen header layout
 * 
 * Replicates the standard BMS screen header layout found in all mapsets (COSGN00, COACTVW, COMEN01).
 * Displays transaction name, program name, current date/time, and screen title.
 * Provides consistent header structure across all 18 React screen components, maintaining 
 * the original 3270 terminal header format with responsive design.
 * 
 * BMS Header Pattern:
 * Row 1: "Tran:" + TRNNAME (4 chars) + TITLE01 (40 chars) + "Date:" + CURDATE (mm/dd/yy)
 * Row 2: "Prog:" + PGMNAME (8 chars) + TITLE02 (40 chars) + "Time:" + CURTIME (hh:mm:ss)
 * 
 * Colors: Labels and data in BLUE, titles in YELLOW (matching BMS attributes)
 */

import React, { useState, useEffect } from 'react';
import { Typography } from '@mui/material';
import { validateDate } from '../../utils/validation';

/**
 * Header Component Props
 * @param {string} transactionId - Transaction ID (4 characters, maps to TRNNAME)
 * @param {string} programName - Program name (8 characters, maps to PGMNAME) 
 * @param {string} title - Screen title (spans TITLE01 and TITLE02, up to 80 chars)
 */
const Header = ({ transactionId = '', programName = '', title = '' }) => {
  // State for real-time date and time updates
  const [currentDateTime, setCurrentDateTime] = useState(new Date());

  // Update date/time every second for real-time display
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentDateTime(new Date());
    }, 1000);

    // Cleanup timer on component unmount
    return () => clearInterval(timer);
  }, []);

  /**
   * Format current date as MM/DD/YY to match BMS CURDATE format
   * @param {Date} date - Date object to format
   * @returns {string} Formatted date string (MM/DD/YY)
   */
  const formatDate = (date) => {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0'); 
    const year = String(date.getFullYear()).slice(-2);
    return `${month}/${day}/${year}`;
  };

  /**
   * Format current time as HH:MM:SS to match BMS CURTIME format
   * @param {Date} date - Date object to format
   * @returns {string} Formatted time string (HH:MM:SS)
   */
  const formatTime = (date) => {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
  };

  // Split title into two lines (40 chars each) to match BMS TITLE01/TITLE02
  const title1 = title.substring(0, 40);
  const title2 = title.substring(40, 80);

  // Format current date and time 
  const formattedDate = formatDate(currentDateTime);
  const formattedTime = formatTime(currentDateTime);

  // Truncate/pad fields to match BMS field lengths
  const displayTransactionId = transactionId.substring(0, 4).padEnd(4, ' ');
  const displayProgramName = programName.substring(0, 8).padEnd(8, ' ');

  return (
    <div
      style={{
        width: '100%',
        fontFamily: 'monospace', // Monospace font to replicate 3270 terminal
        backgroundColor: '#000000', // Black background like 3270 terminal
        color: '#00FF00', // Default green text
        padding: '4px 8px',
        fontSize: '14px',
        lineHeight: '1.2',
      }}
    >
      {/* Row 1: Transaction Name, Title (Part 1), Date */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          minHeight: '20px',
        }}
      >
        {/* Left section: Tran: + Transaction Name */}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace',
              fontSize: 'inherit',
              marginRight: '4px',
            }}
          >
            Tran:
          </Typography>
          <Typography
            variant="body2"
            component="span" 
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace',
              fontSize: 'inherit',
              minWidth: '32px', // 4 chars * 8px per char
            }}
          >
            {displayTransactionId}
          </Typography>
        </div>

        {/* Center section: Title Part 1 */}
        <div style={{ flex: 1, textAlign: 'center', margin: '0 16px' }}>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#FFEB3B', // Yellow color matching BMS YELLOW attribute  
              fontFamily: 'monospace',
              fontSize: 'inherit',
            }}
          >
            {title1}
          </Typography>
        </div>

        {/* Right section: Date: + Current Date */}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace', 
              fontSize: 'inherit',
              marginRight: '4px',
            }}
          >
            Date:
          </Typography>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace',
              fontSize: 'inherit',
              minWidth: '64px', // 8 chars * 8px per char
            }}
          >
            {formattedDate}
          </Typography>
        </div>
      </div>

      {/* Row 2: Program Name, Title (Part 2), Time */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          minHeight: '20px',
        }}
      >
        {/* Left section: Prog: + Program Name */}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace',
              fontSize: 'inherit',
              marginRight: '4px',
            }}
          >
            Prog:
          </Typography>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace', 
              fontSize: 'inherit',
              minWidth: '64px', // 8 chars * 8px per char
            }}
          >
            {displayProgramName}
          </Typography>
        </div>

        {/* Center section: Title Part 2 */}
        <div style={{ flex: 1, textAlign: 'center', margin: '0 16px' }}>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#FFEB3B', // Yellow color matching BMS YELLOW attribute
              fontFamily: 'monospace',
              fontSize: 'inherit',
            }}
          >
            {title2}
          </Typography>
        </div>

        {/* Right section: Time: + Current Time */}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace',
              fontSize: 'inherit', 
              marginRight: '4px',
            }}
          >
            Time:
          </Typography>
          <Typography
            variant="body2"
            component="span"
            sx={{
              color: '#4FC3F7', // Blue color matching BMS BLUE attribute
              fontFamily: 'monospace',
              fontSize: 'inherit',
              minWidth: '72px', // 9 chars * 8px per char (HH:MM:SS)
            }}
          >
            {formattedTime}
          </Typography>
        </div>
      </div>
    </div>
  );
};

export default Header;