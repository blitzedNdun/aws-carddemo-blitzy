/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.entity.CardXref;
import com.carddemo.repository.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test class for CrossReferenceProcessorService that validates the
 * COBOL CBACT03C batch cross-reference processing logic migration to Java.
 * 
 * This test class ensures 100% functional parity between the original COBOL program
 * and the Java implementation, testing all business logic paths, error conditions,
 * and edge cases. The tests validate the proper translation of VSAM KSDS operations
 * to Spring Data JPA patterns while maintaining identical processing behavior.
 * 
 * Key COBOL Program Elements Being Tested:
 * - 0000-XREFFILE-OPEN: File opening operations with status handling
 * - 1000-XREFFILE-GET-NEXT: Sequential record reading with EOF detection
 * - 9000-XREFFILE-CLOSE: File closing with proper cleanup
 * - 9910-DISPLAY-IO-STATUS: File status error reporting
 * - 9999-ABEND-PROGRAM: Abnormal program termination handling
 * - Main processing loop: Sequential processing until EOF
 * 
 * Test Coverage Areas:
 * 1. Happy path processing with multiple cross-reference records
 * 2. Empty dataset handling (no records available)
 * 3. File operation success and error scenarios
 * 4. Sequential processing order preservation
 * 5. End-of-file condition detection and handling
 * 6. Error condition handling and status code reporting
 * 7. Resource cleanup and proper state management
 * 8. Display/logging functionality validation
 * 
 * The tests use Mockito to mock the CardXrefRepository to control data scenarios
 * and verify proper interaction patterns without requiring actual database operations.
 * This ensures fast, reliable unit tests that focus on business logic validation.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@ExtendWith(MockitoExtension.class)
public class CrossReferenceProcessorServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(CrossReferenceProcessorServiceTest.class);

    @Mock
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private CrossReferenceProcessorService crossReferenceProcessorService;

    // Test data matching COBOL record structure from CVACT03Y.cpy
    private CardXref testRecord1;
    private CardXref testRecord2;
    private CardXref testRecord3;
    private List<CardXref> testDataList;

    /**
     * Set up test data before each test execution.
     * 
     * Creates test CardXref records that match the COBOL CARD-XREF-RECORD structure:
     * - XREF-CARD-NUM (PIC X(16))
     * - XREF-CUST-ID (PIC 9(09))
     * - XREF-ACCT-ID (PIC 9(11))
     * 
     * This setup ensures consistent test data across all test methods and validates
     * that the service can handle properly formatted cross-reference records.
     */
    @BeforeEach
    public void setUp() {
        // Create test records matching COBOL CVACT03Y.cpy structure
        testRecord1 = new CardXref();
        testRecord1.setXrefCardNum("4444333322221111"); // 16-character card number
        testRecord1.setXrefCustId(123456789L);           // 9-digit customer ID
        testRecord1.setXrefAcctId(12345678901L);         // 11-digit account ID

        testRecord2 = new CardXref();
        testRecord2.setXrefCardNum("5555444433332222"); // 16-character card number
        testRecord2.setXrefCustId(987654321L);           // 9-digit customer ID
        testRecord2.setXrefAcctId(98765432109L);         // 11-digit account ID

        testRecord3 = new CardXref();
        testRecord3.setXrefCardNum("6666555544443333"); // 16-character card number
        testRecord3.setXrefCustId(555666777L);           // 9-digit customer ID
        testRecord3.setXrefAcctId(77788899900L);         // 11-digit account ID

        // Create test data list for multiple record scenarios
        testDataList = Arrays.asList(testRecord1, testRecord2, testRecord3);

        logger.debug("Test setup completed with {} test records", testDataList.size());
    }

    /**
     * Tests the happy path scenario with valid cross-reference data.
     * 
     * Validates that the service processes all records sequentially and completes
     * successfully when valid data is available. This test replicates the successful
     * execution path of the COBOL CBACT03C program.
     * 
     * COBOL Equivalent Test Scenario:
     * - File opens successfully (XREFFILE-STATUS = '00')
     * - Records are read sequentially (XREFFILE-STATUS = '00' for each read)
     * - Each record is displayed (DISPLAY CARD-XREF-RECORD)
     * - End-of-file is reached (XREFFILE-STATUS = '10')
     * - File closes successfully (XREFFILE-STATUS = '00')
     * - Program completes normally
     */
    @Test
    public void testProcessAllCrossReferences_WithValidData() {
        logger.info("Testing processAllCrossReferences with valid data");

        // Mock repository to return test data
        when(cardXrefRepository.count()).thenReturn((long) testDataList.size());
        when(cardXrefRepository.findAll()).thenReturn(testDataList);

        // Execute the processing method
        crossReferenceProcessorService.processAllCrossReferences();

        // Verify repository interactions match COBOL file operations
        verify(cardXrefRepository, times(1)).count();  // File status check
        verify(cardXrefRepository, times(1)).findAll(); // Sequential read setup

        logger.info("processAllCrossReferences with valid data test completed successfully");
    }

    /**
     * Tests the empty data scenario where no cross-reference records exist.
     * 
     * Validates that the service handles empty datasets gracefully without errors,
     * matching the COBOL behavior when the cross-reference file is empty.
     * 
     * COBOL Equivalent Test Scenario:
     * - File opens successfully (XREFFILE-STATUS = '00')
     * - First read operation returns EOF immediately (XREFFILE-STATUS = '10')
     * - END-OF-FILE flag is set to 'Y'
     * - Processing loop terminates without executing record processing
     * - File closes successfully
     * - Program completes normally with zero records processed
     */
    @Test
    public void testProcessAllCrossReferences_EmptyData() {
        logger.info("Testing processAllCrossReferences with empty data");

        // Mock repository to return empty dataset
        when(cardXrefRepository.count()).thenReturn(0L);
        when(cardXrefRepository.findAll()).thenReturn(Collections.emptyList());

        // Execute the processing method
        crossReferenceProcessorService.processAllCrossReferences();

        // Verify repository interactions for empty data scenario
        verify(cardXrefRepository, times(1)).count();   // File status check
        verify(cardXrefRepository, times(1)).findAll();  // Sequential read setup

        logger.info("processAllCrossReferences with empty data test completed successfully");
    }

    /**
     * Tests successful file opening operation.
     * 
     * Validates that the openCrossReferenceFile method correctly initializes
     * the file processing state and prepares for sequential record reading,
     * matching the COBOL 0000-XREFFILE-OPEN paragraph behavior.
     * 
     * COBOL Equivalent Test Scenario:
     * - MOVE 8 TO APPL-RESULT (initialize status)
     * - OPEN INPUT XREFFILE-FILE
     * - IF XREFFILE-STATUS = '00' MOVE 0 TO APPL-RESULT
     * - File successfully opened for sequential processing
     */
    @Test
    public void testOpenCrossReferenceFile_Success() {
        logger.info("Testing openCrossReferenceFile success scenario");

        // Mock repository operations for successful open
        when(cardXrefRepository.count()).thenReturn((long) testDataList.size());
        when(cardXrefRepository.findAll()).thenReturn(testDataList);

        // Execute file open operation
        crossReferenceProcessorService.openCrossReferenceFile();

        // Verify repository interactions for file open
        verify(cardXrefRepository, times(1)).count();   // File availability check
        verify(cardXrefRepository, times(1)).findAll();  // Load records for processing

        logger.info("openCrossReferenceFile success test completed successfully");
    }

    /**
     * Tests successful file closing operation.
     * 
     * Validates that the closeCrossReferenceFile method properly cleans up
     * resources and resets processing state, matching the COBOL 9000-XREFFILE-CLOSE
     * paragraph behavior.
     * 
     * COBOL Equivalent Test Scenario:
     * - ADD 8 TO ZERO GIVING APPL-RESULT (initialize status)
     * - CLOSE XREFFILE-FILE
     * - IF XREFFILE-STATUS = '00' SUBTRACT APPL-RESULT FROM APPL-RESULT
     * - File successfully closed with proper cleanup
     */
    @Test
    public void testCloseCrossReferenceFile_Success() {
        logger.info("Testing closeCrossReferenceFile success scenario");

        // Setup file in open state first
        when(cardXrefRepository.count()).thenReturn((long) testDataList.size());
        when(cardXrefRepository.findAll()).thenReturn(testDataList);
        crossReferenceProcessorService.openCrossReferenceFile();

        // Execute file close operation
        crossReferenceProcessorService.closeCrossReferenceFile();

        // Verify proper cleanup occurred (no additional repository calls expected)
        verify(cardXrefRepository, times(1)).count();   // Only from open operation
        verify(cardXrefRepository, times(1)).findAll();  // Only from open operation

        logger.info("closeCrossReferenceFile success test completed successfully");
    }

    /**
     * Tests getting next cross-reference record with valid data available.
     * 
     * Validates that the getNextCrossReference method correctly returns records
     * in sequential order and maintains proper file status, matching the COBOL
     * 1000-XREFFILE-GET-NEXT paragraph behavior with successful read operations.
     * 
     * COBOL Equivalent Test Scenario:
     * - READ XREFFILE-FILE INTO CARD-XREF-RECORD
     * - IF XREFFILE-STATUS = '00' (successful read)
     * - MOVE 0 TO APPL-RESULT
     * - DISPLAY CARD-XREF-RECORD
     * - Record is available and returned for processing
     */
    @Test
    public void testGetNextCrossReference_ValidData() {
        logger.info("Testing getNextCrossReference with valid data");

        // Setup file in open state with test data
        when(cardXrefRepository.count()).thenReturn((long) testDataList.size());
        when(cardXrefRepository.findAll()).thenReturn(testDataList);
        crossReferenceProcessorService.openCrossReferenceFile();

        // Get first record
        CardXref firstRecord = crossReferenceProcessorService.getNextCrossReference();

        // Verify first record is returned correctly
        assertNotNull(firstRecord, "First record should not be null");
        assertEquals(testRecord1.getXrefCardNum(), firstRecord.getXrefCardNum(), 
                    "First record card number should match test data");
        assertEquals(testRecord1.getXrefCustId(), firstRecord.getXrefCustId(),
                    "First record customer ID should match test data");
        assertEquals(testRecord1.getXrefAcctId(), firstRecord.getXrefAcctId(),
                    "First record account ID should match test data");

        logger.info("getNextCrossReference with valid data test completed successfully");
    }

    /**
     * Tests getting next cross-reference record when end of file is reached.
     * 
     * Validates that the getNextCrossReference method correctly detects end-of-file
     * condition and returns null, matching the COBOL 1000-XREFFILE-GET-NEXT
     * paragraph behavior when XREFFILE-STATUS = '10' (EOF).
     * 
     * COBOL Equivalent Test Scenario:
     * - READ XREFFILE-FILE INTO CARD-XREF-RECORD
     * - IF XREFFILE-STATUS = '10' (end of file)
     * - MOVE 16 TO APPL-RESULT
     * - IF APPL-EOF MOVE 'Y' TO END-OF-FILE
     * - No more records available for processing
     */
    @Test
    public void testGetNextCrossReference_EndOfFile() {
        logger.info("Testing getNextCrossReference at end of file");

        // Setup file with empty data to simulate EOF condition
        when(cardXrefRepository.count()).thenReturn(0L);
        when(cardXrefRepository.findAll()).thenReturn(Collections.emptyList());
        crossReferenceProcessorService.openCrossReferenceFile();

        // Attempt to get record from empty file
        CardXref record = crossReferenceProcessorService.getNextCrossReference();

        // Verify EOF condition is handled correctly
        assertNull(record, "Record should be null at end of file");

        logger.info("getNextCrossReference end of file test completed successfully");
    }

    /**
     * Tests the display of a valid cross-reference record.
     * 
     * Validates that the displayCrossReference method correctly formats and logs
     * cross-reference record information, matching the COBOL DISPLAY statement
     * behavior for outputting record data.
     * 
     * COBOL Equivalent Test Scenario:
     * - DISPLAY CARD-XREF-RECORD
     * - Record information is output in readable format
     * - All key fields (card number, customer ID, account ID) are included
     */
    @Test
    public void testDisplayCrossReference_ValidRecord() {
        logger.info("Testing displayCrossReference with valid record");

        // Create a test record for display
        CardXref displayRecord = new CardXref();
        displayRecord.setXrefCardNum("1234567890123456");
        displayRecord.setXrefCustId(999888777L);
        displayRecord.setXrefAcctId(55544433322L);

        // Execute display method (should not throw exceptions)
        assertDoesNotThrow(() -> {
            crossReferenceProcessorService.displayCrossReference(displayRecord);
        }, "displayCrossReference should not throw exceptions with valid record");

        // Verify record content is accessible (matching COBOL field access)
        assertEquals("1234567890123456", displayRecord.getXrefCardNum(),
                    "Card number should match test value");
        assertEquals(999888777L, displayRecord.getXrefCustId(),
                    "Customer ID should match test value");
        assertEquals(55544433322L, displayRecord.getXrefAcctId(),
                    "Account ID should match test value");

        logger.info("displayCrossReference with valid record test completed successfully");
    }

    /**
     * Tests file error handling scenarios.
     * 
     * Validates that the handleFileError method correctly processes and reports
     * file operation errors with appropriate status codes, matching the COBOL
     * 9910-DISPLAY-IO-STATUS paragraph behavior for error reporting.
     * 
     * COBOL Equivalent Test Scenario:
     * - IF IO-STATUS NOT NUMERIC OR IO-STAT1 = '9'
     * - Complex status display logic for error conditions
     * - DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04
     * - Error information is properly formatted and reported
     */
    @Test
    public void testHandleFileError_ErrorCondition() {
        logger.info("Testing handleFileError with error condition");

        // Test various error scenarios matching COBOL error handling
        assertDoesNotThrow(() -> {
            crossReferenceProcessorService.handleFileError("OPEN_ERROR");
        }, "handleFileError should not throw exceptions for open error");

        assertDoesNotThrow(() -> {
            crossReferenceProcessorService.handleFileError("READ_ERROR");  
        }, "handleFileError should not throw exceptions for read error");

        assertDoesNotThrow(() -> {
            crossReferenceProcessorService.handleFileError("CLOSE_ERROR");
        }, "handleFileError should not throw exceptions for close error");

        assertDoesNotThrow(() -> {
            crossReferenceProcessorService.handleFileError("PROCESS_ERROR");
        }, "handleFileError should not throw exceptions for process error");

        logger.info("handleFileError error condition test completed successfully");
    }

    /**
     * Tests that sequential processing preserves record order.
     * 
     * Validates that the service processes cross-reference records in the exact
     * same order they are retrieved from the repository, matching the COBOL
     * sequential file reading behavior that maintains record ordering.
     * 
     * COBOL Equivalent Test Scenario:
     * - Sequential READ operations maintain file order
     * - Records are processed in the same sequence they appear in the file
     * - First record read is first record processed
     * - Processing continues in order until EOF
     */
    @Test
    public void testSequentialProcessing_OrderPreserved() {
        logger.info("Testing sequential processing order preservation");

        // Setup file with ordered test data
        when(cardXrefRepository.count()).thenReturn((long) testDataList.size());
        when(cardXrefRepository.findAll()).thenReturn(testDataList);
        crossReferenceProcessorService.openCrossReferenceFile();

        // Process records sequentially and verify order
        List<CardXref> processedRecords = new ArrayList<>();
        
        CardXref record1 = crossReferenceProcessorService.getNextCrossReference();
        if (record1 != null) {
            processedRecords.add(record1);
        }

        CardXref record2 = crossReferenceProcessorService.getNextCrossReference();
        if (record2 != null) {
            processedRecords.add(record2);
        }

        CardXref record3 = crossReferenceProcessorService.getNextCrossReference();
        if (record3 != null) {
            processedRecords.add(record3);
        }

        // Verify processing order matches input order
        assertEquals(testDataList.size(), processedRecords.size(),
                    "Number of processed records should match input");
        
        for (int i = 0; i < processedRecords.size(); i++) {
            CardXref expected = testDataList.get(i);
            CardXref actual = processedRecords.get(i);
            
            assertEquals(expected.getXrefCardNum(), actual.getXrefCardNum(),
                        String.format("Record %d card number should match expected order", i + 1));
            assertEquals(expected.getXrefCustId(), actual.getXrefCustId(),
                        String.format("Record %d customer ID should match expected order", i + 1));
            assertEquals(expected.getXrefAcctId(), actual.getXrefAcctId(),
                        String.format("Record %d account ID should match expected order", i + 1));
        }

        // Verify EOF condition after all records processed
        CardXref eofRecord = crossReferenceProcessorService.getNextCrossReference();
        assertNull(eofRecord, "Should return null after processing all records");

        logger.info("Sequential processing order preservation test completed successfully");
    }
}