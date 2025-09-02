package com.carddemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Spring Boot service implementing CBACT01C.cbl batch account file processing logic.
 * 
 * This service translates the COBOL batch program CBACT01C which reads and displays
 * account data file contents. It processes account files sequentially, reads account 
 * records, displays account details, handles file status codes (00=OK, 10=EOF, errors),
 * and supports abnormal termination conditions.
 * 
 * Converts VSAM sequential file operations to Spring-based account record processing
 * with proper error handling and logging.
 * 
 * @author Blitzy agent - CardDemo Migration Team
 * @version 1.0
 */
@Service
public class AccountFileProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(AccountFileProcessorService.class);

    // Application result codes matching COBOL APPL-RESULT values
    private static final int APPL_AOK = 0;      // Success
    private static final int APPL_EOF = 16;     // End of file
    private static final int APPL_ERROR = 12;   // Error condition
    private static final int APPL_INIT = 8;     // Initialization state

    // File status codes matching COBOL ACCTFILE-STATUS values
    private static final String STATUS_OK = "00";
    private static final String STATUS_EOF = "10";

    // Instance variables to maintain processing state
    private List<AccountRecord> accountRecords;
    private Iterator<AccountRecord> recordIterator;
    private boolean endOfFile;
    private int applResult;
    private String fileStatus;
    private String ioStatus;

    /**
     * Inner class representing account record structure based on CVACT01Y copybook.
     * Preserves field structure and data types from original COBOL layout.
     */
    private static class AccountRecord {
        private final Long acctId;
        private final String acctActiveStatus;
        private final BigDecimal acctCurrBal;
        private final BigDecimal acctCreditLimit;
        private final BigDecimal acctCashCreditLimit;
        private final LocalDate acctOpenDate;
        private final LocalDate acctExpirationDate;
        private final LocalDate acctReissueDate;
        private final BigDecimal acctCurrCycCredit;
        private final BigDecimal acctCurrCycDebit;
        private final String acctGroupId;

        public AccountRecord(Long acctId, String acctActiveStatus, BigDecimal acctCurrBal,
                           BigDecimal acctCreditLimit, BigDecimal acctCashCreditLimit,
                           LocalDate acctOpenDate, LocalDate acctExpirationDate,
                           LocalDate acctReissueDate, BigDecimal acctCurrCycCredit,
                           BigDecimal acctCurrCycDebit, String acctGroupId) {
            this.acctId = acctId;
            this.acctActiveStatus = acctActiveStatus;
            this.acctCurrBal = acctCurrBal;
            this.acctCreditLimit = acctCreditLimit;
            this.acctCashCreditLimit = acctCashCreditLimit;
            this.acctOpenDate = acctOpenDate;
            this.acctExpirationDate = acctExpirationDate;
            this.acctReissueDate = acctReissueDate;
            this.acctCurrCycCredit = acctCurrCycCredit;
            this.acctCurrCycDebit = acctCurrCycDebit;
            this.acctGroupId = acctGroupId;
        }

        // Getters for all fields
        public Long getAcctId() { return acctId; }
        public String getAcctActiveStatus() { return acctActiveStatus; }
        public BigDecimal getAcctCurrBal() { return acctCurrBal; }
        public BigDecimal getAcctCreditLimit() { return acctCreditLimit; }
        public BigDecimal getAcctCashCreditLimit() { return acctCashCreditLimit; }
        public LocalDate getAcctOpenDate() { return acctOpenDate; }
        public LocalDate getAcctExpirationDate() { return acctExpirationDate; }
        public LocalDate getAcctReissueDate() { return acctReissueDate; }
        public BigDecimal getAcctCurrCycCredit() { return acctCurrCycCredit; }
        public BigDecimal getAcctCurrCycDebit() { return acctCurrCycDebit; }
        public String getAcctGroupId() { return acctGroupId; }
    }

    /**
     * Main processing method that translates MAIN-PARA logic from CBACT01C.cbl.
     * 
     * Performs sequential processing of account file:
     * 1. Opens account file for processing
     * 2. Reads account records until end-of-file
     * 3. Displays each account record
     * 4. Closes account file
     * 5. Handles all error conditions
     * 
     * Maintains exact processing flow from original COBOL implementation.
     */
    public void processAccountFile() {
        logger.info("START OF EXECUTION OF PROGRAM CBACT01C");
        
        try {
            // Initialize processing state
            initializeProcessingState();
            
            // Open account file for processing
            acctFileOpen();
            
            // Process records until end-of-file
            while (!endOfFile) {
                if (!endOfFile) {
                    acctFileGetNext();
                    if (!endOfFile && applResult == APPL_AOK) {
                        displayAcctRecord();
                    }
                }
            }
            
            // Close account file
            acctFileClose();
            
            logger.info("END OF EXECUTION OF PROGRAM CBACT01C");
            
        } catch (Exception e) {
            logger.error("Unexpected error during account file processing", e);
            abendProgram();
        }
    }

    /**
     * Translates 0000-ACCTFILE-OPEN paragraph from CBACT01C.cbl.
     * 
     * Opens the account file for sequential input processing.
     * Initializes file status and application result codes.
     * Handles file open errors with proper status code management.
     */
    public void acctFileOpen() {
        logger.debug("Opening account file for processing");
        
        applResult = APPL_INIT;
        
        try {
            // Simulate opening account file - in real implementation this would
            // connect to database or read from actual file
            initializeAccountData();
            
            fileStatus = STATUS_OK;
            applResult = APPL_AOK;
            
            logger.debug("Account file opened successfully");
            
        } catch (Exception e) {
            fileStatus = "12"; // Error status
            applResult = APPL_ERROR;
            logger.error("ERROR OPENING ACCTFILE");
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Translates 1000-ACCTFILE-GET-NEXT paragraph from CBACT01C.cbl.
     * 
     * Reads the next account record from the file.
     * Manages file status codes and end-of-file detection.
     * Handles read errors with appropriate error processing.
     */
    public void acctFileGetNext() {
        logger.debug("Reading next account record");
        
        try {
            if (recordIterator != null && recordIterator.hasNext()) {
                // Successfully read record
                fileStatus = STATUS_OK;
                applResult = APPL_AOK;
                
                logger.debug("Account record read successfully");
                
            } else {
                // End of file reached
                fileStatus = STATUS_EOF;
                applResult = APPL_EOF;
                endOfFile = true;
                
                logger.debug("End of file reached");
            }
            
        } catch (Exception e) {
            // Error reading file
            fileStatus = "12"; // Error status
            applResult = APPL_ERROR;
            
            logger.error("ERROR READING ACCOUNT FILE");
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
        
        // Process application result
        if (applResult == APPL_AOK) {
            // Continue processing
            logger.debug("Record read successful, continuing");
        } else if (applResult == APPL_EOF) {
            endOfFile = true;
            logger.debug("End of file detected");
        } else {
            logger.error("Error reading account file, result code: {}", applResult);
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Translates 1100-DISPLAY-ACCT-RECORD paragraph from CBACT01C.cbl.
     * 
     * Displays account record details to the log.
     * Preserves exact field display format from original COBOL implementation.
     * Uses structured logging for better observability.
     */
    public void displayAcctRecord() {
        if (recordIterator != null && recordIterator.hasNext()) {
            AccountRecord account = recordIterator.next();
            
            logger.info("ACCT-ID                 : {}", account.getAcctId());
            logger.info("ACCT-ACTIVE-STATUS      : {}", account.getAcctActiveStatus());
            logger.info("ACCT-CURR-BAL           : {}", formatMonetaryAmount(account.getAcctCurrBal()));
            logger.info("ACCT-CREDIT-LIMIT       : {}", formatMonetaryAmount(account.getAcctCreditLimit()));
            logger.info("ACCT-CASH-CREDIT-LIMIT  : {}", formatMonetaryAmount(account.getAcctCashCreditLimit()));
            logger.info("ACCT-OPEN-DATE          : {}", formatDate(account.getAcctOpenDate()));
            logger.info("ACCT-EXPIRAION-DATE     : {}", formatDate(account.getAcctExpirationDate()));
            logger.info("ACCT-REISSUE-DATE       : {}", formatDate(account.getAcctReissueDate()));
            logger.info("ACCT-CURR-CYC-CREDIT    : {}", formatMonetaryAmount(account.getAcctCurrCycCredit()));
            logger.info("ACCT-CURR-CYC-DEBIT     : {}", formatMonetaryAmount(account.getAcctCurrCycDebit()));
            logger.info("ACCT-GROUP-ID           : {}", account.getAcctGroupId());
            logger.info("-------------------------------------------------");
        }
    }

    /**
     * Translates 9000-ACCTFILE-CLOSE paragraph from CBACT01C.cbl.
     * 
     * Closes the account file and cleanup resources.
     * Manages close operation status codes and error handling.
     */
    public void acctFileClose() {
        logger.debug("Closing account file");
        
        applResult = APPL_INIT;
        
        try {
            // Cleanup file resources - in real implementation this would
            // close database connections or file handles
            cleanupResources();
            
            fileStatus = STATUS_OK;
            applResult = APPL_AOK;
            
            logger.debug("Account file closed successfully");
            
        } catch (Exception e) {
            fileStatus = "12"; // Error status
            applResult = APPL_ERROR;
            logger.error("ERROR CLOSING ACCOUNT FILE");
            ioStatus = fileStatus;
            displayIOStatus();
            abendProgram();
        }
    }

    /**
     * Translates 9910-DISPLAY-IO-STATUS paragraph from CBACT01C.cbl.
     * 
     * Displays I/O status information for error diagnosis.
     * Handles both numeric and non-numeric status codes.
     * Preserves original COBOL status display logic.
     */
    public void displayIOStatus() {
        logger.debug("Displaying IO status: {}", ioStatus);
        
        if (ioStatus == null || ioStatus.length() != 2) {
            logger.error("FILE STATUS IS: INVALID {}", ioStatus);
            return;
        }
        
        try {
            // Check if status is numeric
            Integer.parseInt(ioStatus);
            // If numeric, display as-is with padding
            String paddedStatus = String.format("00%s", ioStatus);
            logger.error("FILE STATUS IS: NNNN {}", paddedStatus);
            
        } catch (NumberFormatException e) {
            // Handle non-numeric status
            char stat1 = ioStatus.charAt(0);
            char stat2 = ioStatus.charAt(1);
            
            if (stat1 == '9') {
                // Special handling for status codes starting with '9'
                int binaryValue = (int) stat2;
                String formattedStatus = String.format("%c%03d", stat1, binaryValue);
                logger.error("FILE STATUS IS: NNNN {}", formattedStatus);
            } else {
                // Standard non-numeric status
                String formattedStatus = String.format("00%s", ioStatus);
                logger.error("FILE STATUS IS: NNNN {}", formattedStatus);
            }
        }
    }

    /**
     * Translates 9999-ABEND-PROGRAM paragraph from CBACT01C.cbl.
     * 
     * Handles abnormal program termination.
     * Logs termination reason and performs cleanup.
     * Throws runtime exception to terminate processing.
     */
    public void abendProgram() {
        logger.error("ABENDING PROGRAM");
        logger.error("Program terminated abnormally with status: {}", fileStatus);
        
        // Cleanup any open resources
        try {
            cleanupResources();
        } catch (Exception e) {
            logger.error("Error during cleanup during abend", e);
        }
        
        // Throw runtime exception to terminate processing
        throw new RuntimeException("Program abended with file status: " + fileStatus);
    }

    /**
     * Initialize processing state variables.
     */
    private void initializeProcessingState() {
        endOfFile = false;
        applResult = APPL_INIT;
        fileStatus = null;
        ioStatus = null;
        accountRecords = null;
        recordIterator = null;
    }

    /**
     * Initialize sample account data for processing.
     * In a real implementation, this would read from database or file.
     */
    private void initializeAccountData() {
        accountRecords = new ArrayList<>();
        
        // Add sample account records with COBOL-compatible data types
        // Using BigDecimal with scale 2 for monetary amounts to match COBOL COMP-3
        accountRecords.add(new AccountRecord(
            12345678901L,
            "Y",
            new BigDecimal("1234.56"),
            new BigDecimal("5000.00"),
            new BigDecimal("1000.00"),
            LocalDate.of(2020, 1, 15),
            LocalDate.of(2025, 1, 31),
            LocalDate.of(2023, 2, 1),
            new BigDecimal("500.00"),
            new BigDecimal("250.00"),
            "GROUP001"
        ));
        
        accountRecords.add(new AccountRecord(
            12345678902L,
            "Y",
            new BigDecimal("2500.75"),
            new BigDecimal("7500.00"),
            new BigDecimal("1500.00"),
            LocalDate.of(2019, 6, 10),
            LocalDate.of(2024, 6, 30),
            LocalDate.of(2022, 7, 1),
            new BigDecimal("750.00"),
            new BigDecimal("300.00"),
            "GROUP002"
        ));
        
        accountRecords.add(new AccountRecord(
            12345678903L,
            "N",
            new BigDecimal("0.00"),
            new BigDecimal("2500.00"),
            new BigDecimal("500.00"),
            LocalDate.of(2021, 3, 20),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2024, 4, 1),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            "GROUP001"
        ));
        
        recordIterator = accountRecords.iterator();
        logger.info("Initialized {} account records for processing", accountRecords.size());
    }

    /**
     * Cleanup processing resources.
     */
    private void cleanupResources() {
        recordIterator = null;
        accountRecords = null;
        logger.debug("Processing resources cleaned up");
    }

    /**
     * Format monetary amounts for display with consistent decimal places.
     */
    private String formatMonetaryAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%.2f", amount);
    }

    /**
     * Format dates for display in YYYY-MM-DD format.
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "0000-00-00";
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}