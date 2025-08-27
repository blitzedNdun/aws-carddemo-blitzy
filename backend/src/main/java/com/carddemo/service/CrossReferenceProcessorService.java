/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.CardXref;
import com.carddemo.repository.CardXrefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Spring Boot service implementing the CBACT03C COBOL batch cross-reference processing logic.
 * 
 * This service provides a direct translation of the CBACT03C COBOL program which reads all
 * card cross-reference records sequentially and processes them in the same manner as the
 * original COBOL program. The service maintains the same processing flow and error handling
 * patterns as the original mainframe batch program.
 * 
 * Original COBOL Program Structure:
 * - 0000-XREFFILE-OPEN: Opens the VSAM KSDS cross-reference file
 * - 1000-XREFFILE-GET-NEXT: Reads the next cross-reference record sequentially
 * - 9000-XREFFILE-CLOSE: Closes the cross-reference file
 * - 9910-DISPLAY-IO-STATUS: Displays file status information for debugging
 * - 9999-ABEND-PROGRAM: Handles abnormal program termination
 * - Main processing loop: Sequential reading and displaying of all records
 * 
 * Java Translation Strategy:
 * - VSAM KSDS operations → Spring Data JPA repository methods
 * - COBOL DISPLAY statements → SLF4J logger output
 * - COBOL file status codes → Java exception handling
 * - Sequential record reading → Iterator pattern over JPA findAll() result
 * - Resource cleanup → proper Java try-with-resources patterns
 * 
 * Key Features:
 * - Sequential reading of all CardXref records using Spring Data JPA
 * - File status handling equivalent to COBOL file operations
 * - Proper resource management and cleanup
 * - Error handling matching COBOL ABEND patterns
 * - Display/logging functionality equivalent to COBOL DISPLAY statements
 * - Batch processing structure maintaining the same flow as original COBOL program
 * 
 * The service preserves all business logic and processing patterns from the original
 * COBOL program while leveraging modern Java frameworks for enhanced reliability
 * and maintainability.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@Service
public class CrossReferenceProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(CrossReferenceProcessorService.class);

    private final CardXrefRepository cardXrefRepository;
    
    // Processing state variables matching COBOL working storage
    private boolean fileOpen = false;
    private boolean endOfFile = false;
    private int fileStatus = 0;
    private Iterator<CardXref> recordIterator;
    private List<CardXref> allRecords;
    private long totalRecords = 0;

    // File status codes matching COBOL patterns
    private static final int FILE_STATUS_OK = 0;
    private static final int FILE_STATUS_EOF = 16;
    private static final int FILE_STATUS_ERROR = 12;
    private static final int FILE_STATUS_OPEN_ERROR = 8;

    /**
     * Constructor with dependency injection.
     * 
     * @param cardXrefRepository the Spring Data JPA repository for CardXref operations
     */
    public CrossReferenceProcessorService(CardXrefRepository cardXrefRepository) {
        this.cardXrefRepository = cardXrefRepository;
    }

    /**
     * Main processing method that replicates the CBACT03C COBOL program's main logic.
     * 
     * This method follows the exact same structure as the original COBOL program:
     * 1. Display start message
     * 2. Open the cross-reference file
     * 3. Process all records in a loop until end-of-file
     * 4. Close the cross-reference file  
     * 5. Display end message
     * 
     * COBOL Equivalent:
     * DISPLAY 'START OF EXECUTION OF PROGRAM CBACT03C'.
     * PERFORM 0000-XREFFILE-OPEN.
     * PERFORM UNTIL END-OF-FILE = 'Y'
     *     IF END-OF-FILE = 'N'
     *         PERFORM 1000-XREFFILE-GET-NEXT
     *         IF END-OF-FILE = 'N'
     *             DISPLAY CARD-XREF-RECORD
     *         END-IF
     *     END-IF
     * END-PERFORM.
     * PERFORM 9000-XREFFILE-CLOSE.
     * DISPLAY 'END OF EXECUTION OF PROGRAM CBACT03C'.
     */
    public void processAllCrossReferences() {
        logger.info("START OF EXECUTION OF PROGRAM CBACT03C");
        
        try {
            // Open the cross-reference file
            openCrossReferenceFile();
            
            // Process all records until end of file
            while (!endOfFile) {
                if (!endOfFile) {
                    CardXref crossReferenceRecord = getNextCrossReference();
                    if (!endOfFile && crossReferenceRecord != null) {
                        displayCrossReference(crossReferenceRecord);
                    }
                }
            }
            
            // Close the cross-reference file
            closeCrossReferenceFile();
            
        } catch (Exception e) {
            logger.error("ERROR PROCESSING CROSS REFERENCES", e);
            handleFileError("PROCESS_ERROR");
            abendProgram();
        }
        
        logger.info("END OF EXECUTION OF PROGRAM CBACT03C");
    }

    /**
     * Opens the cross-reference file for processing.
     * 
     * Replicates the 0000-XREFFILE-OPEN paragraph from the COBOL program.
     * Initializes the JPA repository connection and prepares the record iterator
     * for sequential processing of all cross-reference records.
     * 
     * COBOL Equivalent:
     * 0000-XREFFILE-OPEN.
     *     MOVE 8 TO APPL-RESULT.
     *     OPEN INPUT XREFFILE-FILE
     *     IF XREFFILE-STATUS = '00'
     *         MOVE 0 TO APPL-RESULT
     *     ELSE
     *         MOVE 12 TO APPL-RESULT
     *     END-IF
     *     IF APPL-AOK
     *         CONTINUE
     *     ELSE
     *         DISPLAY 'ERROR OPENING XREFFILE'
     *         PERFORM 9910-DISPLAY-IO-STATUS
     *         PERFORM 9999-ABEND-PROGRAM
     *     END-IF
     */
    public void openCrossReferenceFile() {
        logger.debug("Opening cross-reference file");
        
        try {
            // Set initial status to indicate open attempt
            fileStatus = FILE_STATUS_OPEN_ERROR;
            
            // Get count of all records (equivalent to file availability check)
            totalRecords = cardXrefRepository.count();
            
            // Get all records for sequential processing (wrap in mutable list)
            allRecords = new ArrayList<>(cardXrefRepository.findAll());
            
            // Initialize iterator for sequential processing
            recordIterator = allRecords.iterator();
            
            // Set successful open status
            fileStatus = FILE_STATUS_OK;
            fileOpen = true;
            endOfFile = false;
            
            logger.info("Cross-reference file opened successfully. Total records: {}", totalRecords);
            
        } catch (Exception e) {
            fileStatus = FILE_STATUS_ERROR;
            fileOpen = false;
            logger.error("ERROR OPENING XREFFILE", e);
            handleFileError("OPEN_ERROR");
            abendProgram();
        }
    }

    /**
     * Closes the cross-reference file after processing.
     * 
     * Replicates the 9000-XREFFILE-CLOSE paragraph from the COBOL program.
     * Performs cleanup operations and releases resources used for record processing.
     * 
     * COBOL Equivalent:
     * 9000-XREFFILE-CLOSE.
     *     ADD 8 TO ZERO GIVING APPL-RESULT.
     *     CLOSE XREFFILE-FILE
     *     IF XREFFILE-STATUS = '00'
     *         SUBTRACT APPL-RESULT FROM APPL-RESULT
     *     ELSE
     *         ADD 12 TO ZERO GIVING APPL-RESULT
     *     END-IF
     *     IF APPL-AOK
     *         CONTINUE
     *     ELSE
     *         DISPLAY 'ERROR CLOSING XREFFILE'
     *         PERFORM 9910-DISPLAY-IO-STATUS
     *         PERFORM 9999-ABEND-PROGRAM
     *     END-IF
     */
    public void closeCrossReferenceFile() {
        logger.debug("Closing cross-reference file");
        
        try {
            // Set initial status to indicate close attempt
            fileStatus = FILE_STATUS_OPEN_ERROR;
            
            // Cleanup resources
            if (recordIterator != null) {
                recordIterator = null;
            }
            
            if (allRecords != null) {
                allRecords.clear();
                allRecords = null;
            }
            
            // Set successful close status
            fileStatus = FILE_STATUS_OK;
            fileOpen = false;
            
            logger.info("Cross-reference file closed successfully");
            
        } catch (Exception e) {
            fileStatus = FILE_STATUS_ERROR;
            logger.error("ERROR CLOSING XREFFILE", e);
            handleFileError("CLOSE_ERROR");
            abendProgram();
        }
    }

    /**
     * Gets the next cross-reference record from the file.
     * 
     * Replicates the 1000-XREFFILE-GET-NEXT paragraph from the COBOL program.
     * Uses the record iterator to sequentially read through all cross-reference
     * records, setting end-of-file flag when all records have been processed.
     * 
     * COBOL Equivalent:
     * 1000-XREFFILE-GET-NEXT.
     *     READ XREFFILE-FILE INTO CARD-XREF-RECORD.
     *     IF XREFFILE-STATUS = '00'
     *         MOVE 0 TO APPL-RESULT
     *         DISPLAY CARD-XREF-RECORD
     *     ELSE
     *         IF XREFFILE-STATUS = '10'
     *             MOVE 16 TO APPL-RESULT
     *         ELSE
     *             MOVE 12 TO APPL-RESULT
     *         END-IF
     *     END-IF
     * 
     * @return the next CardXref record, or null if end of file reached
     */
    public CardXref getNextCrossReference() {
        if (!fileOpen) {
            logger.error("Attempt to read from closed file");
            fileStatus = FILE_STATUS_ERROR;
            handleFileError("FILE_NOT_OPEN");
            abendProgram();
            return null;
        }
        
        try {
            if (recordIterator != null && recordIterator.hasNext()) {
                // Read next record
                CardXref record = recordIterator.next();
                fileStatus = FILE_STATUS_OK;
                
                logger.debug("Read cross-reference record: {}", record.toString());
                return record;
                
            } else {
                // End of file reached
                fileStatus = FILE_STATUS_EOF;
                endOfFile = true;
                logger.debug("End of file reached");
                return null;
            }
            
        } catch (Exception e) {
            fileStatus = FILE_STATUS_ERROR;
            logger.error("ERROR READING XREFFILE", e);
            handleFileError("READ_ERROR");
            abendProgram();
            return null;
        }
    }

    /**
     * Displays a cross-reference record.
     * 
     * Replicates the COBOL DISPLAY statements that output cross-reference record
     * information. Uses structured logging to provide the same information that
     * would be displayed in the original COBOL program.
     * 
     * COBOL Equivalent:
     * DISPLAY CARD-XREF-RECORD
     * 
     * @param crossReference the CardXref record to display
     */
    public void displayCrossReference(CardXref crossReference) {
        if (crossReference != null) {
            // Display record in same format as COBOL program
            String recordDisplay = String.format("CARD-XREF-RECORD: Card=%s, Customer=%d, Account=%d", 
                    crossReference.getXrefCardNum(), 
                    crossReference.getXrefCustId(), 
                    crossReference.getXrefAcctId());
            
            // Log at INFO level to match COBOL DISPLAY behavior
            logger.info(recordDisplay);
            
            // Also use the toString() method as specified in schema
            logger.debug("Full record: {}", crossReference.toString());
        }
    }

    /**
     * Handles file operation errors.
     * 
     * Replicates the 9910-DISPLAY-IO-STATUS paragraph from the COBOL program.
     * Provides detailed error information when file operations fail, including
     * status codes and error descriptions matching the original COBOL logic.
     * 
     * COBOL Equivalent:
     * 9910-DISPLAY-IO-STATUS.
     *     IF IO-STATUS NOT NUMERIC
     *     OR IO-STAT1 = '9'
     *         [complex status display logic]
     *     ELSE
     *         MOVE '0000' TO IO-STATUS-04
     *         MOVE IO-STATUS TO IO-STATUS-04(3:2)
     *         DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04
     *     END-IF
     * 
     * @param errorType the type of file error that occurred
     */
    public void handleFileError(String errorType) {
        String statusMessage;
        
        // Format status message based on file status code
        if (fileStatus == FILE_STATUS_OK) {
            statusMessage = "0000";
        } else if (fileStatus == FILE_STATUS_EOF) {
            statusMessage = "0010";
        } else if (fileStatus == FILE_STATUS_ERROR) {
            statusMessage = "0012";
        } else if (fileStatus == FILE_STATUS_OPEN_ERROR) {
            statusMessage = "0008";
        } else {
            statusMessage = String.format("%04d", fileStatus);
        }
        
        String fullErrorMessage = String.format("FILE STATUS IS: %s - ERROR TYPE: %s", statusMessage, errorType);
        logger.error(fullErrorMessage);
    }

    /**
     * Abnormally terminates the program.
     * 
     * Replicates the 9999-ABEND-PROGRAM paragraph from the COBOL program.
     * Handles abnormal program termination when critical errors occur that
     * prevent continued processing. In the Spring Boot context, this throws
     * a runtime exception to halt batch processing.
     * 
     * COBOL Equivalent:
     * 9999-ABEND-PROGRAM.
     *     DISPLAY 'ABENDING PROGRAM'
     *     MOVE 0 TO TIMING
     *     MOVE 999 TO ABCODE
     *     CALL 'CEE3ABD'.
     */
    public void abendProgram() {
        logger.error("ABENDING PROGRAM");
        
        // Cleanup any open resources - but avoid recursive calls during close errors
        if (fileOpen) {
            try {
                // Direct cleanup without calling closeCrossReferenceFile to avoid recursion
                if (recordIterator != null) {
                    recordIterator = null;
                }
                
                if (allRecords != null) {
                    try {
                        allRecords.clear();
                    } catch (UnsupportedOperationException e) {
                        // Handle immutable list case
                        logger.debug("Cannot clear immutable list, setting to null");
                    }
                    allRecords = null;
                }
                
                fileOpen = false;
                logger.debug("Emergency file cleanup completed during abend");
                
            } catch (Exception e) {
                logger.error("Error during emergency cleanup in abend", e);
            }
        }
        
        // In Spring Boot context, throw runtime exception to halt processing
        throw new RuntimeException("Cross-reference processor program abended with status: " + fileStatus);
    }
}