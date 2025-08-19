/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Boot service class implementing CBCUS01C.cbl batch customer file processing logic.
 * 
 * This service processes customer files sequentially, reads customer records using CUST-ID key,
 * displays customer details, handles file status codes (00=OK, 10=EOF, errors), and supports
 * abnormal termination conditions. Translates VSAM indexed sequential file operations to 
 * Spring-based customer record processing with proper error handling and logging.
 * 
 * Original COBOL Program: CBCUS01C.cbl
 * Function: Read and print customer data file
 * Type: BATCH COBOL Program
 * 
 * COBOL Structure Translation:
 * - MAIN-PARA → processCustomerFile() method
 * - 0000-CUSTFILE-OPEN → custFileOpen() method
 * - 1000-CUSTFILE-GET-NEXT → custFileGetNext() method
 * - 9000-CUSTFILE-CLOSE → custFileClose() method
 * - Z-DISPLAY-IO-STATUS → displayIOStatus() method
 * - Z-ABEND-PROGRAM → abendProgram() method
 * 
 * Status Code Mapping:
 * - CUSTFILE-STATUS '00' → APPL_RESULT = 0 (Success)
 * - CUSTFILE-STATUS '10' → APPL_RESULT = 16 (EOF)
 * - Other CUSTFILE-STATUS → APPL_RESULT = 12 (Error)
 * - APPL-AOK → applResult == 0
 * - APPL-EOF → applResult == 16
 * 
 * File Operations Mapping:
 * - VSAM OPEN INPUT → CustomerRepository.findAll()
 * - VSAM READ INTO → Iterate through List<Customer>
 * - VSAM CLOSE → Clear resources, flush operations
 * - DISPLAY → Logger statements
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@Service
public class CustomerFileProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerFileProcessorService.class);

    // COBOL equivalent status codes
    private static final int APPL_AOK = 0;      // Successful operation
    private static final int APPL_EOF = 16;     // End of file reached
    private static final int APPL_ERROR = 12;   // Error condition
    private static final int APPL_INIT = 8;     // Initial state

    // COBOL equivalent file status codes
    private static final String CUSTFILE_STATUS_OK = "00";    // Successful operation
    private static final String CUSTFILE_STATUS_EOF = "10";   // End of file
    
    // Working storage variables (equivalent to COBOL WORKING-STORAGE)
    private int applResult;
    private String custFileStatus;
    private String endOfFile;
    private List<Customer> customerList;
    private int currentRecordIndex;
    private String ioStatus;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Main customer file processing method.
     * Equivalent to COBOL MAIN-PARA procedure.
     * 
     * Performs the complete customer file processing workflow:
     * 1. Display start of execution message
     * 2. Open customer file
     * 3. Process records sequentially until EOF
     * 4. Display each customer record
     * 5. Close customer file
     * 6. Display end of execution message
     * 
     * COBOL Logic Translation:
     * DISPLAY 'START OF EXECUTION OF PROGRAM CBCUS01C'.
     * PERFORM 0000-CUSTFILE-OPEN.
     * PERFORM UNTIL END-OF-FILE = 'Y'
     *     IF END-OF-FILE = 'N'
     *         PERFORM 1000-CUSTFILE-GET-NEXT
     *         IF END-OF-FILE = 'N'
     *             DISPLAY CUSTOMER-RECORD 
     *         END-IF
     *     END-IF
     * END-PERFORM.
     * PERFORM 9000-CUSTFILE-CLOSE.
     * DISPLAY 'END OF EXECUTION OF PROGRAM CBCUS01C'.
     */
    public void processCustomerFile() {
        logger.info("START OF EXECUTION OF PROGRAM CBCUS01C");

        try {
            // Initialize working storage variables
            endOfFile = "N";
            currentRecordIndex = 0;
            
            // Open customer file
            custFileOpen();

            // Process records until end of file
            while (!"Y".equals(endOfFile)) {
                if ("N".equals(endOfFile)) {
                    custFileGetNext();
                    
                    // Display customer record if not EOF
                    if ("N".equals(endOfFile) && currentRecordIndex > 0) {
                        Customer currentCustomer = customerList.get(currentRecordIndex - 1);
                        logger.info("Customer Record: {}", currentCustomer.toString());
                    }
                }
            }

            // Close customer file
            custFileClose();

        } catch (Exception e) {
            logger.error("Unexpected error during customer file processing", e);
            abendProgram();
        }

        logger.info("END OF EXECUTION OF PROGRAM CBCUS01C");
    }

    /**
     * Opens customer file for sequential processing.
     * Equivalent to COBOL 0000-CUSTFILE-OPEN paragraph.
     * 
     * Initializes the customer list by retrieving all customer records from the database
     * using CustomerRepository.findAll(). Sets application result based on success or failure.
     * 
     * COBOL Logic Translation:
     * MOVE 8 TO APPL-RESULT.
     * OPEN INPUT CUSTFILE-FILE
     * IF CUSTFILE-STATUS = '00'
     *     MOVE 0 TO APPL-RESULT
     * ELSE
     *     MOVE 12 TO APPL-RESULT
     * END-IF
     * IF APPL-AOK
     *     CONTINUE
     * ELSE
     *     DISPLAY 'ERROR OPENING CUSTFILE'
     *     MOVE CUSTFILE-STATUS TO IO-STATUS
     *     PERFORM Z-DISPLAY-IO-STATUS
     *     PERFORM Z-ABEND-PROGRAM
     * END-IF
     */
    public void custFileOpen() {
        logger.debug("Opening customer file for processing");
        
        // Initialize application result to initial state
        applResult = APPL_INIT;
        
        try {
            // Retrieve all customer records (equivalent to OPEN INPUT CUSTFILE-FILE)
            customerList = customerRepository.findAll();
            
            // Set successful status
            custFileStatus = CUSTFILE_STATUS_OK;
            applResult = APPL_AOK;
            
            logger.info("Customer file opened successfully. Total records: {}", customerList.size());
            
        } catch (Exception e) {
            // Set error status
            custFileStatus = "99"; // Generic error status
            applResult = APPL_ERROR;
            
            logger.error("ERROR OPENING CUSTFILE: {}", e.getMessage(), e);
            ioStatus = custFileStatus;
            displayIOStatus();
            abendProgram();
        }
        
        // Verify successful open
        if (applResult != APPL_AOK) {
            logger.error("ERROR OPENING CUSTFILE");
            ioStatus = custFileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Gets next customer record from the sequential file.
     * Equivalent to COBOL 1000-CUSTFILE-GET-NEXT paragraph.
     * 
     * Reads the next customer record from the list, handles EOF condition,
     * and sets appropriate application result codes.
     * 
     * COBOL Logic Translation:
     * READ CUSTFILE-FILE INTO CUSTOMER-RECORD.
     * IF CUSTFILE-STATUS = '00'
     *     MOVE 0 TO APPL-RESULT
     *     DISPLAY CUSTOMER-RECORD 
     * ELSE
     *     IF CUSTFILE-STATUS = '10'
     *         MOVE 16 TO APPL-RESULT
     *     ELSE
     *         MOVE 12 TO APPL-RESULT
     *     END-IF
     * END-IF
     * IF APPL-AOK
     *     CONTINUE
     * ELSE
     *     IF APPL-EOF
     *         MOVE 'Y' TO END-OF-FILE
     *     ELSE
     *         DISPLAY 'ERROR READING CUSTOMER FILE'
     *         MOVE CUSTFILE-STATUS TO IO-STATUS
     *         PERFORM Z-DISPLAY-IO-STATUS
     *         PERFORM Z-ABEND-PROGRAM
     *     END-IF
     * END-IF
     */
    public void custFileGetNext() {
        logger.debug("Reading next customer record. Current index: {}", currentRecordIndex);
        
        try {
            // Check if we have reached end of file
            if (customerList == null || customerList.isEmpty() || currentRecordIndex >= customerList.size()) {
                // End of file condition
                custFileStatus = CUSTFILE_STATUS_EOF;
                applResult = APPL_EOF;
                
                logger.debug("End of file reached. Total records processed: {}", currentRecordIndex);
            } else {
                // Read next record successfully
                Customer currentCustomer = customerList.get(currentRecordIndex);
                currentRecordIndex++;
                
                custFileStatus = CUSTFILE_STATUS_OK;
                applResult = APPL_AOK;
                
                // Display customer record (equivalent to DISPLAY CUSTOMER-RECORD in COBOL)
                logger.info("Customer Record Read: Customer ID={}, Name={} {}, Phone={}, FICO={}", 
                    currentCustomer.getCustomerId(),
                    currentCustomer.getFirstName(),
                    currentCustomer.getLastName(),
                    currentCustomer.getPhoneNumber1() != null ? currentCustomer.getPhoneNumber1() : "N/A",
                    currentCustomer.getFicoScore() != null ? currentCustomer.getFicoScore() : "N/A");
            }
            
        } catch (Exception e) {
            // Error reading record
            custFileStatus = "99"; // Generic error status
            applResult = APPL_ERROR;
            
            logger.error("Error reading customer record at index {}: {}", currentRecordIndex, e.getMessage(), e);
        }
        
        // Handle application result
        if (applResult == APPL_AOK) {
            // Continue processing - successful read
            return;
        } else if (applResult == APPL_EOF) {
            // Set end of file flag
            endOfFile = "Y";
            logger.info("End of file reached. Total customer records processed: {}", currentRecordIndex);
        } else {
            // Error condition - display error and abend
            logger.error("ERROR READING CUSTOMER FILE");
            ioStatus = custFileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Closes customer file and cleans up resources.
     * Equivalent to COBOL 9000-CUSTFILE-CLOSE paragraph.
     * 
     * Performs cleanup operations and flushes any pending database operations.
     * Sets application result based on success or failure.
     * 
     * COBOL Logic Translation:
     * ADD 8 TO ZERO GIVING APPL-RESULT.
     * CLOSE CUSTFILE-FILE
     * IF CUSTFILE-STATUS = '00'
     *     SUBTRACT APPL-RESULT FROM APPL-RESULT
     * ELSE
     *     ADD 12 TO ZERO GIVING APPL-RESULT
     * END-IF
     * IF APPL-AOK
     *     CONTINUE
     * ELSE
     *     DISPLAY 'ERROR CLOSING CUSTOMER FILE'
     *     MOVE CUSTFILE-STATUS TO IO-STATUS
     *     PERFORM Z-DISPLAY-IO-STATUS
     *     PERFORM Z-ABEND-PROGRAM
     * END-IF
     */
    public void custFileClose() {
        logger.debug("Closing customer file");
        
        // Initialize result to initial state (equivalent to ADD 8 TO ZERO GIVING APPL-RESULT)
        applResult = APPL_INIT;
        
        try {
            // Flush any pending operations (equivalent to CLOSE CUSTFILE-FILE)
            customerRepository.flush();
            
            // Clear customer list to free memory
            if (customerList != null) {
                customerList.clear();
                customerList = null;
            }
            
            // Reset index
            currentRecordIndex = 0;
            
            // Set successful status (equivalent to SUBTRACT APPL-RESULT FROM APPL-RESULT)
            custFileStatus = CUSTFILE_STATUS_OK;
            applResult = APPL_AOK;
            
            logger.info("Customer file closed successfully");
            
        } catch (Exception e) {
            // Set error status (equivalent to ADD 12 TO ZERO GIVING APPL-RESULT)
            custFileStatus = "99"; // Generic error status
            applResult = APPL_ERROR;
            
            logger.error("Error closing customer file: {}", e.getMessage(), e);
        }
        
        // Check for close errors
        if (applResult != APPL_AOK) {
            logger.error("ERROR CLOSING CUSTOMER FILE");
            ioStatus = custFileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Displays I/O status information for debugging and error reporting.
     * Equivalent to COBOL Z-DISPLAY-IO-STATUS paragraph.
     * 
     * Formats and displays file status codes in a format similar to COBOL output.
     * Handles both numeric and non-numeric status codes.
     * 
     * COBOL Logic Translation:
     * IF IO-STATUS NOT NUMERIC
     * OR IO-STAT1 = '9'
     *     MOVE IO-STAT1 TO IO-STATUS-04(1:1)
     *     MOVE 0        TO TWO-BYTES-BINARY
     *     MOVE IO-STAT2 TO TWO-BYTES-RIGHT
     *     MOVE TWO-BYTES-BINARY TO IO-STATUS-0403
     *     DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04 
     * ELSE
     *     MOVE '0000' TO IO-STATUS-04
     *     MOVE IO-STATUS TO IO-STATUS-04(3:2)
     *     DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04 
     * END-IF
     */
    public void displayIOStatus() {
        if (ioStatus == null || ioStatus.trim().isEmpty()) {
            logger.warn("FILE STATUS IS: 0000 (No status available)");
            return;
        }
        
        String ioStatus04;
        
        try {
            // Check if status is numeric and not starting with '9'
            if (ioStatus.matches("\\d+") && !ioStatus.startsWith("9")) {
                // Numeric status - pad to 4 digits
                ioStatus04 = String.format("%04d", Integer.parseInt(ioStatus));
            } else {
                // Non-numeric or starts with '9' - format as COBOL does
                if (ioStatus.length() >= 2) {
                    char ioStat1 = ioStatus.charAt(0);
                    char ioStat2 = ioStatus.charAt(1);
                    
                    // Convert second character to binary equivalent and format
                    int twoBytesBinary = (int) ioStat2;
                    ioStatus04 = String.format("%c%03d", ioStat1, twoBytesBinary);
                } else {
                    // Single character status
                    ioStatus04 = ioStatus + "000";
                }
            }
        } catch (NumberFormatException e) {
            // Handle non-numeric status
            ioStatus04 = ioStatus.length() >= 4 ? ioStatus.substring(0, 4) : 
                         String.format("%-4s", ioStatus).replace(' ', '0');
        }
        
        logger.error("FILE STATUS IS: {}", ioStatus04);
    }

    /**
     * Handles abnormal program termination.
     * Equivalent to COBOL Z-ABEND-PROGRAM paragraph.
     * 
     * Logs the abend condition and throws a runtime exception to terminate processing.
     * In the COBOL version, this would call CEE3ABD to abend the program.
     * 
     * COBOL Logic Translation:
     * DISPLAY 'ABENDING PROGRAM'
     * MOVE 0 TO TIMING
     * MOVE 999 TO ABCODE
     * CALL 'CEE3ABD'.
     */
    public void abendProgram() {
        logger.error("ABENDING PROGRAM");
        
        // Set abend code (equivalent to MOVE 999 TO ABCODE)
        int abCode = 999;
        int timing = 0;
        
        logger.error("Program terminated abnormally. ABCODE: {}, TIMING: {}", abCode, timing);
        
        // Throw runtime exception to terminate processing (equivalent to CALL 'CEE3ABD')
        throw new RuntimeException("Customer file processing abended with code: " + abCode);
    }
}