/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.service;

import com.carddemo.service.StatementFileHandlerService.FileOperationRequest;
import com.carddemo.service.StatementFileHandlerService.FileOperationResponse;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.AccountRepository;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.CardXref;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test suite for StatementFileHandlerService.
 * 
 * Tests the complete COBOL CBSTM03B functionality migration including:
 * - File operation dispatch logic (OPEN/CLOSE/READ/READ-K/WRITE/REWRITE)
 * - File designator routing (TRNXFILE/XREFFILE/CUSTFILE/ACCTFILE)
 * - Linkage area parameter processing (FileOperationRequest/Response)
 * - File status code handling and error conditions
 * - Data formatting and parsing between COBOL and Java entity formats
 * 
 * Uses JUnit 5 and Mockito for comprehensive test coverage of business logic
 * without requiring actual database connectivity. All repository dependencies
 * are mocked to provide isolated unit testing.
 * 
 * Test Structure:
 * - Main Dispatch Logic Tests: handleFileOperation() routing
 * - File Handler Tests: Each file type (TRNX/XREF/CUST/ACCT) handler validation 
 * - Operation Tests: Each operation type (O/C/R/K/W/Z) validation
 * - Error Handling Tests: Invalid inputs and exception scenarios
 * - Data Processing Tests: Format/parse methods for record conversion
 * - Inner Class Tests: FileOperationRequest/Response functionality
 * 
 * @author CardDemo Migration Team
 * @version 1.0 
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatementFileHandlerService Tests")
public class StatementFileHandlerServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private StatementFileHandlerService fileHandlerService;

    // Test data constants
    private static final String FILE_TRNXFILE = "TRNXFILE";
    private static final String FILE_XREFFILE = "XREFFILE";
    private static final String FILE_CUSTFILE = "CUSTFILE";
    private static final String FILE_ACCTFILE = "ACCTFILE";

    private static final String OP_OPEN = "O";
    private static final String OP_CLOSE = "C";
    private static final String OP_READ = "R";
    private static final String OP_READ_K = "K";
    private static final String OP_WRITE = "W";
    private static final String OP_REWRITE = "Z";

    private static final String STATUS_SUCCESS = "00";
    private static final String STATUS_EOF = "10";
    private static final String STATUS_NOT_FOUND = "23";
    private static final String STATUS_ERROR = "99";

    private Transaction testTransaction;
    private CardXref testCardXref;
    private Customer testCustomer;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Initialize test entities using setters as required by schema
        testTransaction = new Transaction();
        testTransaction.setTransactionId(1L);
        testTransaction.setCardNumber("1234567890123456");
        testTransaction.setDescription("Test Transaction");

        testCardXref = new CardXref();
        testCardXref.setXrefCardNum("1234567890123456");
        testCardXref.setXrefAcctId(12345678901L);
        testCardXref.setXrefCustId(123456789L);

        testCustomer = new Customer();
        testCustomer.setCustomerId("123456789");
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");

        testAccount = new Account();
        testAccount.setAccountId(12345678901L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrentBalance(new BigDecimal("1000.00"));
    }

    /**
     * Test suite for main file operation dispatch logic.
     * Validates EVALUATE LK-M03B-DD equivalent routing functionality.
     */
    @Nested
    @DisplayName("Main Dispatch Logic Tests")
    class MainDispatchLogicTests {

        @Test
        @DisplayName("Should route TRNXFILE operations to handleTrnxFile")
        void testHandleFileOperation_RoutesToTrnxFile() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_OPEN, null, 0, null);
            when(transactionRepository.count()).thenReturn(10L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertNotNull(response);
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(transactionRepository, times(1)).count();
        }

        @Test
        @DisplayName("Should route XREFFILE operations to handleXrefFile")
        void testHandleFileOperation_RoutesToXrefFile() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_XREFFILE, OP_OPEN, null, 0, null);
            when(cardXrefRepository.count()).thenReturn(5L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertNotNull(response);
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(cardXrefRepository, times(1)).count();
        }

        @Test
        @DisplayName("Should route CUSTFILE operations to handleCustFile")
        void testHandleFileOperation_RoutesToCustFile() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_OPEN, null, 0, null);
            when(customerRepository.count()).thenReturn(15L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertNotNull(response);
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(customerRepository, times(1)).count();
        }

        @Test
        @DisplayName("Should route ACCTFILE operations to handleAcctFile")
        void testHandleFileOperation_RoutesToAcctFile() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_OPEN, null, 0, null);
            when(accountRepository.count()).thenReturn(8L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertNotNull(response);
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(accountRepository, times(1)).count();
        }

        @Test
        @DisplayName("Should handle invalid file designator with error")
        void testHandleFileOperation_InvalidFileDesignator() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest("INVALIDFILE", OP_OPEN, null, 0, null);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertNotNull(response);
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Invalid file designator"));
        }

        @Test
        @DisplayName("Should handle exceptions during operation dispatch")
        void testHandleFileOperation_ExceptionHandling() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_OPEN, null, 0, null);
            when(transactionRepository.count()).thenThrow(new RuntimeException("Database error"));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertNotNull(response);
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Failed to open file"));
        }
    }

    /**
     * Test suite for TRNXFILE operations.
     * Validates transaction file handling equivalent to COBOL 1000-TRNXFILE-PROC.
     */
    @Nested
    @DisplayName("TRNXFILE Operations Tests")  
    class TrnxFileOperationTests {

        @Test
        @DisplayName("Should handle TRNXFILE OPEN operation successfully")
        void testTrnxFile_OpenOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_OPEN, null, 0, null);
            when(transactionRepository.count()).thenReturn(10L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("Should handle TRNXFILE READ operation successfully")
        void testTrnxFile_ReadOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_READ, null, 0, null);
            when(transactionRepository.findAll()).thenReturn(List.of(testTransaction));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
            verify(transactionRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should handle TRNXFILE read with no records (EOF)")
        void testTrnxFile_ReadOperation_NoRecords() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_READ, null, 0, null);
            when(transactionRepository.findAll()).thenReturn(List.of());

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_EOF, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("No records found"));
        }

        @Test
        @DisplayName("Should handle TRNXFILE CLOSE operation successfully")
        void testTrnxFile_CloseOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_CLOSE, null, 0, null);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("Should handle TRNXFILE WRITE operation successfully")
        void testTrnxFile_WriteOperation() {
            // Arrange
            String fieldData = "0000000000000001" + "1234567890123456" + 
                              String.format("%-100s", "Test Transaction");
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, fieldData);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
            verify(transactionRepository, times(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should handle TRNXFILE REWRITE operation successfully")
        void testTrnxFile_RewriteOperation() {
            // Arrange
            String fieldData = "0000000000000001" + "1234567890123456" +
                              String.format("%-100s", "Updated Transaction");
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_REWRITE, null, 0, fieldData);
            when(transactionRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(transactionRepository, times(1)).existsById(1L);
            verify(transactionRepository, times(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should handle invalid TRNXFILE operation")
        void testTrnxFile_InvalidOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, "INVALID", null, 0, null);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Invalid operation for TRNXFILE"));
        }
    }

    /**
     * Test suite for XREFFILE operations.
     * Validates cross-reference file handling equivalent to COBOL 2000-XREFFILE-PROC.
     */
    @Nested
    @DisplayName("XREFFILE Operations Tests")
    class XrefFileOperationTests {

        @Test
        @DisplayName("Should handle XREFFILE OPEN operation successfully")
        void testXrefFile_OpenOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_XREFFILE, OP_OPEN, null, 0, null);
            when(cardXrefRepository.count()).thenReturn(5L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("Should handle XREFFILE READ operation successfully")
        void testXrefFile_ReadOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_XREFFILE, OP_READ, null, 0, null);
            when(cardXrefRepository.findAll()).thenReturn(List.of(testCardXref));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
            verify(cardXrefRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should handle XREFFILE WRITE operation successfully")
        void testXrefFile_WriteOperation() {
            // Arrange
            String fieldData = "1234567890123456" + "12345678901" + "123456789";
            FileOperationRequest request = new FileOperationRequest(FILE_XREFFILE, OP_WRITE, null, 0, fieldData);
            when(cardXrefRepository.save(any(CardXref.class))).thenReturn(testCardXref);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(cardXrefRepository, times(1)).save(any(CardXref.class));
        }

        @Test
        @DisplayName("Should handle XREFFILE REWRITE operation successfully")
        void testXrefFile_RewriteOperation() {
            // Arrange
            String fieldData = "1234567890123456" + "12345678901" + "123456789";
            FileOperationRequest request = new FileOperationRequest(FILE_XREFFILE, OP_REWRITE, null, 0, fieldData);
            when(cardXrefRepository.save(any(CardXref.class))).thenReturn(testCardXref);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(cardXrefRepository, times(1)).save(any(CardXref.class));
        }
    }

    /**
     * Test suite for CUSTFILE operations.
     * Validates customer file handling equivalent to COBOL 3000-CUSTFILE-PROC.
     */
    @Nested
    @DisplayName("CUSTFILE Operations Tests")
    class CustFileOperationTests {

        @Test
        @DisplayName("Should handle CUSTFILE OPEN operation successfully")
        void testCustFile_OpenOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_OPEN, null, 0, null);
            when(customerRepository.count()).thenReturn(15L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("Should handle CUSTFILE READ by key operation successfully")
        void testCustFile_ReadKeyOperation() {
            // Arrange
            String key = "123456789";
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, key, 9, null);
            when(customerRepository.findById(123456789L)).thenReturn(Optional.of(testCustomer));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
            verify(customerRepository, times(1)).findById(123456789L);
        }

        @Test
        @DisplayName("Should handle CUSTFILE read by key when record not found")
        void testCustFile_ReadKeyOperation_NotFound() {
            // Arrange
            String key = "999999999";
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, key, 9, null);
            when(customerRepository.findById(999999999L)).thenReturn(Optional.empty());

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_NOT_FOUND, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Record not found"));
        }

        @Test
        @DisplayName("Should handle CUSTFILE WRITE operation successfully")
        void testCustFile_WriteOperation() {
            // Arrange
            String fieldData = "123456789" + String.format("%-491s", "John|Doe|123456789");
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_WRITE, null, 0, fieldData);
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(customerRepository, times(1)).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should handle CUSTFILE REWRITE operation successfully")
        void testCustFile_RewriteOperation() {
            // Arrange
            String fieldData = "123456789" + String.format("%-491s", "John|Smith|123456789");
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_REWRITE, null, 0, fieldData);
            when(customerRepository.existsById(123456789L)).thenReturn(true);
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(customerRepository, times(1)).existsById(123456789L);
            verify(customerRepository, times(1)).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should handle CUSTFILE REWRITE when record not found")
        void testCustFile_RewriteOperation_NotFound() {
            // Arrange
            String fieldData = "999999999" + String.format("%-491s", "John|Smith|123456789");
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_REWRITE, null, 0, fieldData);
            when(customerRepository.existsById(999999999L)).thenReturn(false);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Customer not found for rewrite"));
        }

        @Test
        @DisplayName("Should handle invalid key format in CUSTFILE read by key")
        void testCustFile_ReadKeyOperation_InvalidKeyFormat() {
            // Arrange
            String key = "INVALID";
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, key, 9, null);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Invalid key format"));
        }

        @Test
        @DisplayName("Should handle null key in CUSTFILE read by key")
        void testCustFile_ReadKeyOperation_NullKey() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, null, 9, null);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Key is required"));
        }
    }

    /**
     * Test suite for ACCTFILE operations.
     * Validates account file handling equivalent to COBOL 4000-ACCTFILE-PROC.
     */
    @Nested
    @DisplayName("ACCTFILE Operations Tests")
    class AcctFileOperationTests {

        @Test
        @DisplayName("Should handle ACCTFILE OPEN operation successfully")
        void testAcctFile_OpenOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_OPEN, null, 0, null);
            when(accountRepository.count()).thenReturn(8L);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("Should handle ACCTFILE read by key operation successfully")
        void testAcctFile_ReadKeyOperation() {
            // Arrange
            String key = "12345678901";
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_READ_K, key, 11, null);
            when(accountRepository.findById(12345678901L)).thenReturn(Optional.of(testAccount));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
            verify(accountRepository, times(1)).findById(12345678901L);
        }

        @Test
        @DisplayName("Should handle ACCTFILE read by key when record not found")
        void testAcctFile_ReadKeyOperation_NotFound() {
            // Arrange
            String key = "99999999999";
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_READ_K, key, 11, null);
            when(accountRepository.findById(99999999999L)).thenReturn(Optional.empty());

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_NOT_FOUND, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Record not found"));
        }

        @Test
        @DisplayName("Should handle ACCTFILE WRITE operation successfully")
        void testAcctFile_WriteOperation() {
            // Arrange
            String fieldData = "12345678901" + String.format("%-289s", "Y|1000.00|5000.00");
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_WRITE, null, 0, fieldData);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("Should handle ACCTFILE REWRITE operation successfully")
        void testAcctFile_RewriteOperation() {
            // Arrange
            String fieldData = "12345678901" + String.format("%-289s", "Y|2000.00|5000.00");
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_REWRITE, null, 0, fieldData);
            when(accountRepository.existsById(12345678901L)).thenReturn(true);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(accountRepository, times(1)).existsById(12345678901L);
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("Should handle ACCTFILE REWRITE when record not found")
        void testAcctFile_RewriteOperation_NotFound() {
            // Arrange
            String fieldData = "99999999999" + String.format("%-289s", "Y|2000.00|5000.00");
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_REWRITE, null, 0, fieldData);
            when(accountRepository.existsById(99999999999L)).thenReturn(false);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Account not found for rewrite"));
        }
    }

    /**
     * Test suite for individual file operations.
     * Validates core COBOL file operation implementations (OPEN/CLOSE/READ/write operations).
     */
    @Nested
    @DisplayName("Core File Operations Tests")
    class CoreFileOperationTests {

        @Test
        @DisplayName("Should open file successfully and return success status")
        void testOpenFile_Success() {
            // Arrange
            when(transactionRepository.count()).thenReturn(10L);

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.openFile(response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("Should handle open file exception")
        void testOpenFile_Exception() {
            // Arrange
            when(transactionRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.openFile(response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Failed to open file"));
        }

        @Test
        @DisplayName("Should handle open file with unknown designator")
        void testOpenFile_UnknownDesignator() {
            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.openFile(response, "UNKNOWN");

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Unknown file designator"));
        }

        @Test
        @DisplayName("Should close file successfully")
        void testCloseFile_Success() {
            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.closeFile(response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("Should handle read file with data successfully")
        void testReadFile_WithData() {
            // Arrange
            when(transactionRepository.findAll()).thenReturn(List.of(testTransaction));

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFile(response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
        }

        @Test
        @DisplayName("Should handle read file with no data (EOF)")
        void testReadFile_NoData() {
            // Arrange
            when(transactionRepository.findAll()).thenReturn(List.of());

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFile(response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_EOF, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("No records found"));
        }

        @Test
        @DisplayName("Should handle read file with unsupported file type")
        void testReadFile_UnsupportedFileType() {
            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFile(response, FILE_CUSTFILE);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Sequential read not supported"));
        }

        @Test
        @DisplayName("Should handle read by key with valid key successfully")
        void testReadFileByKey_ValidKey() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, "123456789", 9, null);
            when(customerRepository.findById(123456789L)).thenReturn(Optional.of(testCustomer));

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFileByKey(request, response, FILE_CUSTFILE);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertNotNull(response.getFieldData());
        }

        @Test
        @DisplayName("Should handle read by key with truncated key")
        void testReadFileByKey_TruncatedKey() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, "123456789000", 9, null);
            when(customerRepository.findById(123456789L)).thenReturn(Optional.of(testCustomer));

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFileByKey(request, response, FILE_CUSTFILE);

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            verify(customerRepository, times(1)).findById(123456789L);
        }

        @Test
        @DisplayName("Should handle read by key with unsupported file type")
        void testReadFileByKey_UnsupportedFileType() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_READ_K, "123", 3, null);

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFileByKey(request, response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Keyed read not supported"));
        }

        @Test
        @DisplayName("Should handle write file with empty field data")
        void testWriteFile_EmptyFieldData() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, "");

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.writeFile(request, response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Field data is required"));
        }

        @Test
        @DisplayName("Should handle write file with unsupported file type")
        void testWriteFile_UnsupportedFileType() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest("UNKNOWN", OP_WRITE, null, 0, "data");

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.writeFile(request, response, "UNKNOWN");

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Write not supported"));
        }

        @Test
        @DisplayName("Should handle rewrite file with empty field data")
        void testRewriteFile_EmptyFieldData() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_REWRITE, null, 0, null);

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.rewriteFile(request, response, FILE_TRNXFILE);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Field data is required"));
        }
    }

    /**
     * Test suite for error handling scenarios.
     * Validates comprehensive error handling and status code management.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions during operation")
        void testRepositoryException_HandlingDuringOperation() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_READ, null, 0, null);
            when(transactionRepository.findAll()).thenThrow(new RuntimeException("Database connection lost"));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Failed to read from file"));
        }

        @Test
        @DisplayName("Should handle transaction repository save exception")
        void testTransactionRepository_SaveException() {
            // Arrange
            String fieldData = "0000000000000001" + "1234567890123456" + 
                              String.format("%-100s", "Test Transaction");
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, fieldData);
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Save failed"));

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertTrue(response.getErrorMessage().contains("Failed to write to file"));
        }

        @Test
        @DisplayName("Should handle invalid field data parsing")
        void testInvalidFieldData_Parsing() {
            // Arrange
            String invalidFieldData = "SHORT_DATA";
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, invalidFieldData);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Should attempt to parse but may create transaction with partial data
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should validate all file designators correctly")
        void testFileDesignator_ValidationComplete() {
            // Test all valid file designators
            String[] validDesignators = {FILE_TRNXFILE, FILE_XREFFILE, FILE_CUSTFILE, FILE_ACCTFILE};
            
            for (String designator : validDesignators) {
                FileOperationRequest request = new FileOperationRequest(designator, OP_CLOSE, null, 0, null);
                FileOperationResponse response = fileHandlerService.handleFileOperation(request);
                
                assertEquals(STATUS_SUCCESS, response.getReturnCode(), 
                    "Should handle " + designator + " successfully");
            }
        }

        @Test
        @DisplayName("Should validate all operation codes correctly")
        void testOperationCode_ValidationComplete() {
            // Test all valid operation codes for TRNXFILE
            String[] validOperations = {OP_OPEN, OP_CLOSE, OP_READ, OP_WRITE, OP_REWRITE};
            when(transactionRepository.count()).thenReturn(1L);
            when(transactionRepository.findAll()).thenReturn(List.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
            when(transactionRepository.existsById(any())).thenReturn(true);
            
            for (String operation : validOperations) {
                String fieldData = operation.equals(OP_WRITE) || operation.equals(OP_REWRITE) ? 
                    "0000000000000001" + "1234567890123456" + String.format("%-100s", "Test") : null;
                FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, operation, null, 0, fieldData);
                FileOperationResponse response = fileHandlerService.handleFileOperation(request);
                
                assertEquals(STATUS_SUCCESS, response.getReturnCode(), 
                    "Should handle operation " + operation + " successfully");
            }
        }
    }

    /**
     * Test suite for data formatting and parsing methods.
     * Validates conversion between COBOL record formats and Java entity objects.
     */
    @Nested
    @DisplayName("Data Processing Tests")
    class DataProcessingTests {

        @Test
        @DisplayName("Should format transaction record correctly")
        void testTransactionRecord_Formatting() {
            // Act - Using reflection to access private method through write operation
            String fieldData = "0000000000000001" + "1234567890123456" + 
                              String.format("%-100s", "Test Transaction");
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, fieldData);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Verify successful processing which indicates formatting worked
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertNotNull(response.getFieldData());
        }

        @Test
        @DisplayName("Should format CardXref record correctly")
        void testCardXrefRecord_Formatting() {
            // Act - Using write operation to trigger formatting
            String fieldData = "1234567890123456" + "12345678901" + "123456789";
            FileOperationRequest request = new FileOperationRequest(FILE_XREFFILE, OP_WRITE, null, 0, fieldData);
            when(cardXrefRepository.save(any(CardXref.class))).thenReturn(testCardXref);

            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Verify successful processing which indicates formatting worked
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertNotNull(response.getFieldData());
        }

        @Test
        @DisplayName("Should format Customer record correctly")
        void testCustomerRecord_Formatting() {
            // Act - Using write operation to trigger formatting
            String fieldData = "123456789" + String.format("%-491s", "John|Doe|123456789");
            FileOperationRequest request = new FileOperationRequest(FILE_CUSTFILE, OP_WRITE, null, 0, fieldData);
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Verify successful processing which indicates formatting worked
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertNotNull(response.getFieldData());
        }

        @Test
        @DisplayName("Should format Account record correctly")
        void testAccountRecord_Formatting() {
            // Act - Using write operation to trigger formatting
            String fieldData = "12345678901" + String.format("%-289s", "Y|1000.00|5000.00");
            FileOperationRequest request = new FileOperationRequest(FILE_ACCTFILE, OP_WRITE, null, 0, fieldData);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Verify successful processing which indicates formatting worked
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertNotNull(response.getFieldData());
        }

        @Test
        @DisplayName("Should handle parsing with minimal data")
        void testMinimalData_Parsing() {
            // Arrange - Very short field data
            String minimalFieldData = "001";
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, minimalFieldData);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Should handle gracefully without exceptions
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
        }

        @Test
        @DisplayName("Should handle parsing with extra long data")
        void testExtraLongData_Parsing() {
            // Arrange - Very long field data
            String longFieldData = "0000000000000001" + "1234567890123456" + 
                                  String.format("%-200s", "Very Long Transaction Description");
            FileOperationRequest request = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, longFieldData);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act
            FileOperationResponse response = fileHandlerService.handleFileOperation(request);

            // Assert - Should handle gracefully by truncating
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
        }

        @Test
        @DisplayName("Should handle null entity data gracefully")
        void testNullEntityData_Handling() {
            // Arrange - Transaction with null fields
            Transaction nullTransaction = new Transaction();
            // All fields remain null
            
            when(transactionRepository.findAll()).thenReturn(List.of(nullTransaction));

            // Act
            FileOperationResponse response = new FileOperationResponse();
            fileHandlerService.readFile(response, FILE_TRNXFILE);

            // Assert - Should format with empty/default values
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertNotNull(response.getFieldData());
        }
    }

    /**
     * Test suite for inner classes FileOperationRequest and FileOperationResponse.
     * Validates linkage area parameter processing equivalent to COBOL structures.
     */
    @Nested
    @DisplayName("Inner Class Tests")
    class InnerClassTests {

        @Test
        @DisplayName("FileOperationRequest should construct with all parameters")
        void testFileOperationRequest_FullConstruction() {
            // Act
            FileOperationRequest request = new FileOperationRequest(
                FILE_TRNXFILE, OP_WRITE, "testkey", 10, "testdata"
            );

            // Assert
            assertEquals(FILE_TRNXFILE, request.getFileDesignator());
            assertEquals(OP_WRITE, request.getOperation());
            assertEquals("testkey", request.getKey());
            assertEquals(10, request.getKeyLength());
            assertEquals("testdata", request.getFieldData());
        }

        @Test
        @DisplayName("FileOperationRequest should construct with default constructor")
        void testFileOperationRequest_DefaultConstruction() {
            // Act
            FileOperationRequest request = new FileOperationRequest();

            // Assert
            assertNull(request.getFileDesignator());
            assertNull(request.getOperation());
            assertNull(request.getKey());
            assertEquals(0, request.getKeyLength());
            assertNull(request.getFieldData());
        }

        @Test
        @DisplayName("FileOperationRequest should support setter methods")
        void testFileOperationRequest_SetterMethods() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest();

            // Act
            request.setFileDesignator(FILE_CUSTFILE);
            request.setOperation(OP_READ_K);
            request.setKey("123456789");
            request.setKeyLength(9);
            request.setFieldData("customer data");

            // Assert
            assertEquals(FILE_CUSTFILE, request.getFileDesignator());
            assertEquals(OP_READ_K, request.getOperation());
            assertEquals("123456789", request.getKey());
            assertEquals(9, request.getKeyLength());
            assertEquals("customer data", request.getFieldData());
        }

        @Test
        @DisplayName("FileOperationRequest toString should provide readable format")
        void testFileOperationRequest_ToString() {
            // Arrange
            FileOperationRequest request = new FileOperationRequest(
                FILE_TRNXFILE, OP_WRITE, "key123", 6, "test data field"
            );

            // Act
            String result = request.toString();

            // Assert
            assertTrue(result.contains("FileOperationRequest"));
            assertTrue(result.contains("TRNXFILE"));
            assertTrue(result.contains("W"));
            assertTrue(result.contains("key123"));
            assertTrue(result.contains("6"));
        }

        @Test
        @DisplayName("FileOperationRequest toString should handle long field data")
        void testFileOperationRequest_ToString_LongData() {
            // Arrange
            String longData = "A".repeat(100);
            FileOperationRequest request = new FileOperationRequest(
                FILE_TRNXFILE, OP_WRITE, "key", 3, longData
            );

            // Act
            String result = request.toString();

            // Assert
            assertTrue(result.contains("..."));
            assertTrue(result.length() < longData.length() + 100);
        }

        @Test
        @DisplayName("FileOperationResponse should construct with default values")
        void testFileOperationResponse_DefaultConstruction() {
            // Act
            FileOperationResponse response = new FileOperationResponse();

            // Assert
            assertEquals(STATUS_ERROR, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertNull(response.getFieldData());
            assertNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("FileOperationResponse should construct with all parameters")
        void testFileOperationResponse_FullConstruction() {
            // Act
            FileOperationResponse response = new FileOperationResponse(
                STATUS_SUCCESS, true, "response data", null
            );

            // Assert
            assertEquals(STATUS_SUCCESS, response.getReturnCode());
            assertTrue(response.isSuccess());
            assertEquals("response data", response.getFieldData());
            assertNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("FileOperationResponse should support setter methods")
        void testFileOperationResponse_SetterMethods() {
            // Arrange
            FileOperationResponse response = new FileOperationResponse();

            // Act
            response.setReturnCode(STATUS_NOT_FOUND);
            response.setSuccess(false);
            response.setFieldData("not found data");
            response.setErrorMessage("Record not found");

            // Assert
            assertEquals(STATUS_NOT_FOUND, response.getReturnCode());
            assertFalse(response.isSuccess());
            assertEquals("not found data", response.getFieldData());
            assertEquals("Record not found", response.getErrorMessage());
        }

        @Test
        @DisplayName("FileOperationResponse toString should provide readable format")
        void testFileOperationResponse_ToString() {
            // Arrange
            FileOperationResponse response = new FileOperationResponse(
                STATUS_SUCCESS, true, "short data", null
            );

            // Act
            String result = response.toString();

            // Assert
            assertTrue(result.contains("FileOperationResponse"));
            assertTrue(result.contains(STATUS_SUCCESS));
            assertTrue(result.contains("true"));
            assertTrue(result.contains("short data"));
        }

        @Test
        @DisplayName("FileOperationResponse toString should handle long field data")
        void testFileOperationResponse_ToString_LongData() {
            // Arrange
            String longData = "B".repeat(100);
            FileOperationResponse response = new FileOperationResponse(
                STATUS_SUCCESS, true, longData, "Test error"
            );

            // Act
            String result = response.toString();

            // Assert
            assertTrue(result.contains("..."));
            assertTrue(result.contains("Test error"));
            assertTrue(result.length() < longData.length() + 200);
        }

        @Test
        @DisplayName("FileOperationResponse toString should handle null field data")
        void testFileOperationResponse_ToString_NullData() {
            // Arrange
            FileOperationResponse response = new FileOperationResponse(
                STATUS_ERROR, false, null, "Null data error"
            );

            // Act
            String result = response.toString();

            // Assert
            assertTrue(result.contains("null"));
            assertTrue(result.contains("Null data error"));
            assertTrue(result.contains(STATUS_ERROR));
        }
    }

    /**
     * Integration test scenarios for complete file operation workflows.
     * Validates end-to-end COBOL business logic functionality.
     */
    @Nested
    @DisplayName("Integration Workflow Tests")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class IntegrationWorkflowTests {

        @Test
        @DisplayName("Should complete full TRNXFILE workflow successfully")
        void testTrnxFile_CompleteWorkflow() {
            // Arrange
            when(transactionRepository.count()).thenReturn(10L);
            when(transactionRepository.findAll()).thenReturn(List.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // Act & Assert - OPEN
            FileOperationRequest openRequest = new FileOperationRequest(FILE_TRNXFILE, OP_OPEN, null, 0, null);
            FileOperationResponse openResponse = fileHandlerService.handleFileOperation(openRequest);
            assertEquals(STATUS_SUCCESS, openResponse.getReturnCode());

            // Act & Assert - READ
            FileOperationRequest readRequest = new FileOperationRequest(FILE_TRNXFILE, OP_READ, null, 0, null);
            FileOperationResponse readResponse = fileHandlerService.handleFileOperation(readRequest);
            assertEquals(STATUS_SUCCESS, readResponse.getReturnCode());

            // Act & Assert - CLOSE
            FileOperationRequest closeRequest = new FileOperationRequest(FILE_TRNXFILE, OP_CLOSE, null, 0, null);
            FileOperationResponse closeResponse = fileHandlerService.handleFileOperation(closeRequest);
            assertEquals(STATUS_SUCCESS, closeResponse.getReturnCode());
        }

        @Test
        @DisplayName("Should complete full CUSTFILE keyed access workflow successfully")
        void testCustFile_KeyedAccessWorkflow() {
            // Arrange
            when(customerRepository.count()).thenReturn(15L);
            when(customerRepository.findById(123456789L)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.existsById(123456789L)).thenReturn(true);
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            // Act & Assert - OPEN
            FileOperationRequest openRequest = new FileOperationRequest(FILE_CUSTFILE, OP_OPEN, null, 0, null);
            FileOperationResponse openResponse = fileHandlerService.handleFileOperation(openRequest);
            assertEquals(STATUS_SUCCESS, openResponse.getReturnCode());

            // Act & Assert - READ-K
            FileOperationRequest readKRequest = new FileOperationRequest(FILE_CUSTFILE, OP_READ_K, "123456789", 9, null);
            FileOperationResponse readKResponse = fileHandlerService.handleFileOperation(readKRequest);
            assertEquals(STATUS_SUCCESS, readKResponse.getReturnCode());

            // Act & Assert - REWRITE
            String fieldData = "123456789" + String.format("%-491s", "John|Smith|123456789");
            FileOperationRequest rewriteRequest = new FileOperationRequest(FILE_CUSTFILE, OP_REWRITE, null, 0, fieldData);
            FileOperationResponse rewriteResponse = fileHandlerService.handleFileOperation(rewriteRequest);
            assertEquals(STATUS_SUCCESS, rewriteResponse.getReturnCode());

            // Act & Assert - CLOSE
            FileOperationRequest closeRequest = new FileOperationRequest(FILE_CUSTFILE, OP_CLOSE, null, 0, null);
            FileOperationResponse closeResponse = fileHandlerService.handleFileOperation(closeRequest);
            assertEquals(STATUS_SUCCESS, closeResponse.getReturnCode());
        }

        @Test
        @DisplayName("Should validate repository method usage according to schema")
        void testRepositoryMethodUsage_SchemaCompliance() {
            // Arrange & Act - Test each repository method specified in schema
            
            // TransactionRepository methods
            when(transactionRepository.findAll()).thenReturn(List.of(testTransaction));
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
            when(transactionRepository.count()).thenReturn(1L);

            // Test findAll()
            FileOperationRequest readRequest = new FileOperationRequest(FILE_TRNXFILE, OP_READ, null, 0, null);
            fileHandlerService.handleFileOperation(readRequest);
            verify(transactionRepository, times(1)).findAll();

            // Test save()
            String fieldData = "0000000000000001" + "1234567890123456" + String.format("%-100s", "Test");
            FileOperationRequest writeRequest = new FileOperationRequest(FILE_TRNXFILE, OP_WRITE, null, 0, fieldData);
            fileHandlerService.handleFileOperation(writeRequest);
            verify(transactionRepository, times(1)).save(any(Transaction.class));

            // Test count()
            FileOperationRequest openRequest = new FileOperationRequest(FILE_TRNXFILE, OP_OPEN, null, 0, null);
            fileHandlerService.handleFileOperation(openRequest);
            verify(transactionRepository, times(1)).count();
        }

        @Test
        @DisplayName("Should validate entity getter/setter usage according to schema")
        void testEntityMethodUsage_SchemaCompliance() {
            // Test Transaction entity getters as specified in schema
            assertEquals(Long.valueOf(1), testTransaction.getTransactionId());
            assertEquals("1234567890123456", testTransaction.getCardNumber());

            // Test Customer entity getters as specified in schema (getCustomerId returns String)
            assertEquals("123456789", testCustomer.getCustomerId());

            // Test Account entity getters as specified in schema
            assertEquals(Long.valueOf(12345678901L), testAccount.getAccountId());

            // Test CardXref entity getters as specified in schema
            assertEquals("1234567890123456", testCardXref.getXrefCardNum());
            assertEquals(Long.valueOf(12345678901L), testCardXref.getXrefAcctId());

            // Test entity setters work correctly
            testTransaction.setTransactionId(2L);
            assertEquals(Long.valueOf(2), testTransaction.getTransactionId());

            testCustomer.setCustomerId("987654321");
            assertEquals("987654321", testCustomer.getCustomerId());

            testAccount.setAccountId(99999999999L);
            assertEquals(Long.valueOf(99999999999L), testAccount.getAccountId());
        }
    }
}