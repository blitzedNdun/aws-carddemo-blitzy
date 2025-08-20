/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Card;
import com.carddemo.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Iterator;

/**
 * Service class implementing COBOL CBACT02C batch card processing logic for indexed 
 * sequential VSAM file operations, providing card record retrieval by key, sequential 
 * processing, file status handling, and error recovery equivalent to the original 
 * mainframe implementation.
 * 
 * This service class provides a direct translation of the CBACT02C COBOL batch program
 * which reads and displays card data from an indexed sequential VSAM file. The implementation
 * preserves the exact COBOL program flow structure while leveraging modern Spring Boot
 * and JPA technologies for data access.
 * 
 * COBOL Program Structure Mapping:
 * - MAIN PROCEDURE → processCardFile() method
 * - 0000-CARDFILE-OPEN → cardFileOpen() method  
 * - 1000-CARDFILE-GET-NEXT → cardFileGetNext() method
 * - 9000-CARDFILE-CLOSE → cardFileClose() method
 * - 9910-DISPLAY-IO-STATUS → displayIOStatus() method
 * - 9999-ABEND-PROGRAM → abendProgram() method
 * 
 * Key Features:
 * - Indexed sequential file operations using JPA repositories
 * - Card record retrieval and sequential processing
 * - File status code handling (00=OK, 10=EOF, others=error)
 * - Console display formatting matching COBOL DISPLAY operations
 * - Abnormal termination handling equivalent to CEE3ABD
 * - Comprehensive error handling and logging
 * 
 * VSAM to JPA Migration:
 * - CARDFILE dataset → CardRepository.findAll() for sequential access
 * - FD-CARD-NUM primary key → Card.cardNumber field access
 * - CARD-RECORD structure → Card entity with all mapped fields
 * - File status codes → applicationResult status tracking
 * - DISPLAY operations → SLF4J logging at appropriate levels
 * 
 * This implementation maintains 100% functional parity with the original COBOL program
 * while providing modern exception handling, logging, and Spring framework integration.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
public class CardFileProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(CardFileProcessorService.class);

    @Autowired
    private CardRepository cardRepository;

    // State variables to track file processing status (equivalent to COBOL WORKING-STORAGE)
    private String fileStatus = "00";        // Equivalent to CARDFILE-STATUS
    private String ioStatus = "00";          // Equivalent to IO-STATUS  
    private int applicationResult = 0;        // Equivalent to APPL-RESULT
    private boolean endOfFile = false;       // Equivalent to END-OF-FILE flag
    private Iterator<Card> cardIterator;     // Iterator for sequential processing
    private List<Card> cardList;             // Complete list of cards for processing
    private long totalRecordsProcessed = 0;  // Count of records processed

    // Application result constants (equivalent to COBOL 88-level conditions)
    private static final int APPL_AOK = 0;   // Equivalent to APPL-AOK
    private static final int APPL_EOF = 16;  // Equivalent to APPL-EOF
    private static final int APPL_ERROR = 12; // File operation error
    
    // File status constants (equivalent to COBOL file status values)
    private static final String STATUS_OK = "00";     // Successful operation
    private static final String STATUS_EOF = "10";    // End of file reached
    private static final String STATUS_ERROR = "12";  // File operation error

    /**
     * Main processing method that orchestrates the complete card file processing workflow.
     * This method implements the main procedure logic from CBACT02C.cbl, coordinating
     * file open, sequential record processing, and file close operations.
     * 
     * Equivalent COBOL Logic:
     * - DISPLAY 'START OF EXECUTION OF PROGRAM CBACT02C'
     * - PERFORM 0000-CARDFILE-OPEN
     * - PERFORM UNTIL END-OF-FILE = 'Y'
     * - PERFORM 9000-CARDFILE-CLOSE  
     * - DISPLAY 'END OF EXECUTION OF PROGRAM CBACT02C'
     * - GOBACK
     * 
     * Processing Flow:
     * 1. Initialize processing and log start message
     * 2. Open card file (load all records from database)
     * 3. Process each card record sequentially with display
     * 4. Close file and cleanup resources
     * 5. Log completion message and summary statistics
     * 
     * Error Handling:
     * - File open errors result in program abend
     * - Record processing errors result in program abend
     * - File close errors result in program abend
     * - All errors are logged with appropriate detail
     * 
     * @throws RuntimeException if any file operation fails or data corruption detected
     */
    public void processCardFile() {
        try {
            // Start of execution message (equivalent to COBOL DISPLAY)
            logger.info("START OF EXECUTION OF PROGRAM CBACT02C");
            
            // Initialize processing state
            totalRecordsProcessed = 0;
            endOfFile = false;
            
            // Perform file open operations (0000-CARDFILE-OPEN)
            cardFileOpen();
            
            // Main processing loop (PERFORM UNTIL END-OF-FILE = 'Y')
            while (!endOfFile) {
                // Get next card record (1000-CARDFILE-GET-NEXT)
                cardFileGetNext();
                
                // If we successfully read a record and haven't reached EOF
                if (!endOfFile && applicationResult == APPL_AOK) {
                    // Note: Current card record display is handled within cardFileGetNext()
                    // to match the exact COBOL logic flow
                    totalRecordsProcessed++;
                }
            }
            
            // Perform file close operations (9000-CARDFILE-CLOSE)
            cardFileClose();
            
            // End of execution message with summary
            logger.info("END OF EXECUTION OF PROGRAM CBACT02C");
            logger.info("Total records processed: {}", totalRecordsProcessed);
            
        } catch (Exception e) {
            logger.error("Unexpected error during card file processing", e);
            abendProgram();
        }
    }

    /**
     * Opens the card file for sequential processing operations.
     * This method implements the 0000-CARDFILE-OPEN paragraph from CBACT02C.cbl,
     * performing file initialization and loading all card records for sequential access.
     * 
     * Equivalent COBOL Logic:
     * - MOVE 8 TO APPL-RESULT
     * - OPEN INPUT CARDFILE-FILE
     * - IF CARDFILE-STATUS = '00' MOVE 0 TO APPL-RESULT
     * - ELSE MOVE 12 TO APPL-RESULT
     * - IF APPL-AOK CONTINUE
     * - ELSE error handling and abend
     * 
     * JPA Implementation:
     * - Uses CardRepository.findAll() to retrieve all card records
     * - Initializes iterator for sequential processing
     * - Sets appropriate file status and application result codes
     * - Handles database access exceptions with error reporting
     * 
     * File Status Codes:
     * - "00" = Successful file open operation
     * - "12" = File operation error (database access failure)
     * 
     * Error Handling:
     * - Database connection failures result in file status "12"
     * - Empty result sets are handled as successful operations
     * - All errors are logged and trigger program abend
     * 
     * @throws RuntimeException if database access fails or file cannot be opened
     */
    public void cardFileOpen() {
        try {
            // Initialize application result to indicate file operation in progress
            applicationResult = 8;
            fileStatus = STATUS_ERROR;
            
            // Attempt to open file (retrieve all card records from database)
            logger.debug("Opening CARDFILE for sequential input processing");
            cardList = cardRepository.findAll();
            
            if (cardList != null) {
                // Successfully retrieved card list - initialize iterator
                cardIterator = cardList.iterator();
                fileStatus = STATUS_OK;
                applicationResult = APPL_AOK;
                
                logger.info("CARDFILE opened successfully. Total records available: {}", cardList.size());
            } else {
                // Null result from repository - treat as error
                fileStatus = STATUS_ERROR;
                applicationResult = APPL_ERROR;
                logger.error("Failed to retrieve card list from repository - null result");
            }
            
        } catch (Exception e) {
            // Database access exception - set error status
            fileStatus = STATUS_ERROR;
            applicationResult = APPL_ERROR;
            logger.error("Exception occurred while opening CARDFILE", e);
        }
        
        // Check operation result and handle errors
        if (applicationResult == APPL_AOK) {
            // Successful operation - continue processing
            logger.debug("CARDFILE open operation completed successfully");
        } else {
            // Error occurred - display status and abend
            logger.error("ERROR OPENING CARDFILE");
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Retrieves the next card record from the sequential file processing.
     * This method implements the 1000-CARDFILE-GET-NEXT paragraph from CBACT02C.cbl,
     * performing sequential record retrieval with end-of-file and error handling.
     * 
     * Equivalent COBOL Logic:
     * - READ CARDFILE-FILE INTO CARD-RECORD
     * - IF CARDFILE-STATUS = '00' MOVE 0 TO APPL-RESULT
     * - ELSE IF CARDFILE-STATUS = '10' MOVE 16 TO APPL-RESULT  
     * - ELSE MOVE 12 TO APPL-RESULT
     * - Handle APPL-AOK, APPL-EOF, and error conditions
     * 
     * JPA Implementation:
     * - Uses iterator.hasNext() and iterator.next() for sequential access
     * - Displays each card record using formatted output
     * - Sets appropriate file status and application result codes
     * - Handles end-of-file condition by setting END-OF-FILE flag
     * 
     * File Status Codes:
     * - "00" = Successful record read operation
     * - "10" = End of file reached (no more records)
     * - "12" = File operation error (iterator exception)
     * 
     * Error Handling:
     * - End of file sets endOfFile flag and APPL-EOF result
     * - Iterator exceptions result in file status "12" and program abend
     * - All errors are logged with appropriate detail
     * 
     * Record Display:
     * - Each successfully read card record is displayed using logger.info()
     * - Display format matches COBOL DISPLAY CARD-RECORD operation
     * - Card information is formatted for readability and debugging
     */
    public void cardFileGetNext() {
        try {
            // Check if iterator is available and has more records
            if (cardIterator != null && cardIterator.hasNext()) {
                // Read next card record (equivalent to READ CARDFILE-FILE INTO CARD-RECORD)
                Card currentCard = cardIterator.next();
                fileStatus = STATUS_OK;
                applicationResult = APPL_AOK;
                
                // Display the card record (equivalent to COBOL DISPLAY CARD-RECORD)
                displayCardRecord(currentCard);
                
            } else {
                // No more records available - end of file condition
                fileStatus = STATUS_EOF;
                applicationResult = APPL_EOF;
                logger.debug("End of file reached - no more card records");
            }
            
        } catch (Exception e) {
            // Iterator exception - treat as file operation error
            fileStatus = STATUS_ERROR;
            applicationResult = APPL_ERROR;
            logger.error("Exception occurred while reading next card record", e);
        }
        
        // Process the result based on application status
        if (applicationResult == APPL_AOK) {
            // Successful record read - continue processing
            // No additional action needed - record already displayed
            
        } else if (applicationResult == APPL_EOF) {
            // End of file reached - set EOF flag
            endOfFile = true;
            logger.debug("End-of-file condition detected");
            
        } else {
            // Error occurred during read operation
            logger.error("ERROR READING CARDFILE");
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Closes the card file and releases processing resources.
     * This method implements the 9000-CARDFILE-CLOSE paragraph from CBACT02C.cbl,
     * performing file cleanup and resource deallocation operations.
     * 
     * Equivalent COBOL Logic:
     * - ADD 8 TO ZERO GIVING APPL-RESULT
     * - CLOSE CARDFILE-FILE
     * - IF CARDFILE-STATUS = '00' SUBTRACT APPL-RESULT FROM APPL-RESULT
     * - ELSE ADD 12 TO ZERO GIVING APPL-RESULT
     * - Handle success and error conditions
     * 
     * JPA Implementation:
     * - Clears iterator and card list references
     * - Resets processing state variables
     * - Sets appropriate file status and application result codes
     * - Handles cleanup exceptions with error reporting
     * 
     * File Status Codes:
     * - "00" = Successful file close operation
     * - "12" = File operation error (cleanup failure)
     * 
     * Error Handling:
     * - Cleanup failures result in file status "12" and program abend
     * - All errors are logged with appropriate detail
     * - Resource cleanup is attempted even in error conditions
     * 
     * Resource Management:
     * - Iterator reference is cleared to prevent memory leaks
     * - Card list reference is cleared to release memory
     * - Processing state variables are reset to initial values
     */
    public void cardFileClose() {
        try {
            // Initialize result to indicate close operation in progress
            applicationResult = 8;
            fileStatus = STATUS_ERROR;
            
            // Perform file close operations (clear iterator and list references)
            logger.debug("Closing CARDFILE and releasing resources");
            
            if (cardIterator != null) {
                cardIterator = null;
            }
            
            if (cardList != null) {
                cardList.clear();
                cardList = null;
            }
            
            // Reset processing state
            endOfFile = false;
            
            // Set successful close status
            fileStatus = STATUS_OK;
            applicationResult = APPL_AOK;
            
            logger.info("CARDFILE closed successfully");
            
        } catch (Exception e) {
            // Exception during close operation
            fileStatus = STATUS_ERROR;
            applicationResult = APPL_ERROR;
            logger.error("Exception occurred while closing CARDFILE", e);
        }
        
        // Check operation result and handle errors
        if (applicationResult == APPL_AOK) {
            // Successful close operation - continue processing
            logger.debug("CARDFILE close operation completed successfully");
        } else {
            // Error occurred - display status and abend
            logger.error("ERROR CLOSING CARDFILE");
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Displays the current I/O status information for debugging and error reporting.
     * This method implements the 9910-DISPLAY-IO-STATUS paragraph from CBACT02C.cbl,
     * providing detailed file status information for troubleshooting operations.
     * 
     * Equivalent COBOL Logic:
     * - IF IO-STATUS NOT NUMERIC OR IO-STAT1 = '9'
     * - Complex status formatting and display logic
     * - DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04
     * 
     * Implementation Details:
     * - Formats I/O status codes for display consistency
     * - Handles both numeric and non-numeric status values
     * - Provides detailed status information for error analysis
     * - Uses logging framework for status display operations
     * 
     * Status Display Format:
     * - Numeric status: "FILE STATUS IS: 00XX"  
     * - Non-numeric status: "FILE STATUS IS: X###"
     * - Special handling for status codes beginning with '9'
     * 
     * Error Analysis:
     * - Status "00" indicates successful operation
     * - Status "10" indicates end-of-file condition
     * - Status "12" indicates file operation error
     * - Other status codes indicate specific error conditions
     */
    public void displayIOStatus() {
        try {
            String statusDisplay;
            
            // Check if ioStatus is numeric and doesn't start with '9'
            if (ioStatus != null && ioStatus.matches("\\d+") && !ioStatus.startsWith("9")) {
                // Numeric status - format as "00XX"
                String paddedStatus = String.format("%4s", ioStatus).replace(' ', '0');
                statusDisplay = "FILE STATUS IS: " + paddedStatus;
            } else {
                // Non-numeric status or starts with '9' - handle special formatting
                if (ioStatus != null && ioStatus.length() >= 2) {
                    char firstChar = ioStatus.charAt(0);
                    char secondChar = ioStatus.charAt(1);
                    
                    // Format as "X###" where X is first character and ### is numeric representation of second
                    int secondCharValue = (int) secondChar;
                    statusDisplay = String.format("FILE STATUS IS: %c%03d", firstChar, secondCharValue);
                } else {
                    // Fallback formatting for invalid status
                    statusDisplay = "FILE STATUS IS: " + (ioStatus != null ? ioStatus : "UNKNOWN");
                }
            }
            
            // Display the formatted status (equivalent to COBOL DISPLAY)
            logger.error(statusDisplay);
            
        } catch (Exception e) {
            // Exception during status display - log basic error information
            logger.error("Failed to display I/O status. Raw status: {}", ioStatus);
            logger.error("Exception during status display", e);
        }
    }

    /**
     * Performs abnormal program termination with error reporting.
     * This method implements the 9999-ABEND-PROGRAM paragraph from CBACT02C.cbl,
     * providing controlled program termination equivalent to CEE3ABD call.
     * 
     * Equivalent COBOL Logic:
     * - DISPLAY 'ABENDING PROGRAM'
     * - MOVE 0 TO TIMING
     * - MOVE 999 TO ABCODE  
     * - CALL 'CEE3ABD'
     * 
     * Implementation Details:
     * - Logs abend message for audit trail
     * - Sets appropriate error codes for debugging
     * - Throws RuntimeException to terminate processing
     * - Provides stack trace for error analysis
     * 
     * Error Codes:
     * - Abend code 999 indicates general program termination
     * - Timing value 0 indicates immediate termination
     * - Exception message includes termination reason
     * 
     * Termination Behavior:
     * - Immediate program termination via RuntimeException
     * - Error logging for audit and debugging purposes
     * - Stack trace preservation for error analysis
     * - Cleanup operations are handled by Spring framework
     * 
     * @throws RuntimeException always thrown to terminate program execution
     */
    public void abendProgram() {
        // Log abend message (equivalent to COBOL DISPLAY 'ABENDING PROGRAM')
        logger.error("ABENDING PROGRAM");
        
        // Set error codes (equivalent to COBOL MOVE operations)
        int timing = 0;    // MOVE 0 TO TIMING
        int abcode = 999;  // MOVE 999 TO ABCODE
        
        // Log error details for debugging
        logger.error("Program termination details - Timing: {}, Abend Code: {}", timing, abcode);
        logger.error("File Status: {}, Application Result: {}", fileStatus, applicationResult);
        
        // Perform abnormal termination (equivalent to CALL 'CEE3ABD')
        throw new RuntimeException("CBACT02C program abnormal termination - Abend Code: " + abcode);
    }

    /**
     * Displays a card record in formatted output matching COBOL DISPLAY operation.
     * This helper method formats and displays card information for debugging and
     * audit purposes, replicating the COBOL DISPLAY CARD-RECORD functionality.
     * 
     * @param card the Card entity to display
     */
    private void displayCardRecord(Card card) {
        if (card != null) {
            // Format card record display to match COBOL structure
            String cardDisplay = String.format(
                "CARD-RECORD: NUM=%s ACCT-ID=%d CVV=%s NAME='%s' EXP-DATE=%s STATUS=%s",
                card.getCardNumber(),
                card.getAccountId(), 
                card.getCvvCode(),
                card.getEmbossedName(),
                card.getExpirationDate(),
                card.getActiveStatus()
            );
            
            // Display the card record (equivalent to COBOL DISPLAY CARD-RECORD)
            logger.info(cardDisplay);
        } else {
            logger.warn("Attempted to display null card record");
        }
    }

}