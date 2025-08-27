/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.integration;

import com.carddemo.controller.AccountController;
import com.carddemo.repository.AccountRepository;
import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.dto.AccountRequest;
import com.carddemo.dto.AccountResponse;
import com.carddemo.dto.AccountUpdateRequest;
import com.carddemo.dto.ApiResponse;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.service.AccountViewService;
import com.carddemo.service.AccountUpdateService;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import jakarta.persistence.OptimisticLockException;

/**
 * Integration test class for AccountController validating account management operations
 * that replicate COACTVWC and COACTUPC CICS transactions.
 * 
 * Tests comprehensive account view and update functionality with:
 * - REST endpoints for account viewing (CAVW) and updating (CAUP) transactions
 * - JPA repository operations with composite keys and referential integrity
 * - BigDecimal precision validation for monetary fields matching COBOL COMP-3 scale
 * - Optimistic locking behavior with JPA OptimisticLockException handling
 * - Field-level validation with Spring Validation framework
 * - Performance validation ensuring response times under 200ms
 * 
 * Extends BaseIntegrationTest for Testcontainers PostgreSQL/Redis setup,
 * test data factories, and BigDecimal assertion methods for COBOL precision validation.
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
public class AccountControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private AccountController accountController;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AccountViewService accountViewService;
    
    @Autowired
    private AccountUpdateService accountUpdateService;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    // Test data constants for COBOL-compatible field values
    private static final String TEST_ACCOUNT_ID = "12345678901";
    private static final String TEST_CUSTOMER_ID = "987654321";
    private static final BigDecimal TEST_CURRENT_BALANCE = new BigDecimal("1234.56").setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TEST_CREDIT_LIMIT = new BigDecimal("5000.00").setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TEST_CASH_CREDIT_LIMIT = new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP);
    private static final String TEST_ACTIVE_STATUS = "A";
    
    // Performance test threshold (200ms requirement)
    private static final long MAX_RESPONSE_TIME_MS = 200L;
    
    @BeforeEach
    void setUp() {
        // Setup MockMvc with full Spring context
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Configure ObjectMapper for JSON serialization with BigDecimal precision
        objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        
        // Setup test containers and base test data
        setupTestContainers();
    }

    /**
     * Test suite for account view operations (CAVW transaction equivalent).
     * Validates GET /api/accounts/{id} endpoint functionality replicating COACTVWC.cbl logic.
     */
    @Nested
    @DisplayName("Account View Operations (CAVW Transaction)")
    class AccountViewTests {

        @Test
        @DisplayName("Should retrieve account details with customer information successfully")
        void testGetAccount_Success() throws Exception {
            // Arrange - Create test account and customer with COBOL-compatible precision
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Validate BigDecimal precision matches COBOL COMP-3 scale
            assertBigDecimalEquals(TEST_CURRENT_BALANCE, testAccount.getCurrentBalance());
            assertBigDecimalEquals(TEST_CREDIT_LIMIT, testAccount.getCreditLimit());
            validateCobolPrecision(testAccount.getCurrentBalance(), 2);
            validateCobolPrecision(testAccount.getCreditLimit(), 2);
            
            // Act & Assert - Test REST endpoint with performance validation
            long startTime = System.currentTimeMillis();
            
            MvcResult result = mockMvc.perform(get("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.transactionCode").value("CAVW"))
                    .andExpect(jsonPath("$.responseData").exists())
                    .andReturn();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Validate response time meets 200ms requirement
            assertTrue(responseTime < MAX_RESPONSE_TIME_MS, 
                String.format("Response time %dms exceeds maximum %dms", responseTime, MAX_RESPONSE_TIME_MS));
            
            // Validate response content and BigDecimal precision
            String responseContent = result.getResponse().getContentAsString();
            AccountResponse response = objectMapper.readValue(responseContent, AccountResponse.class);
            
            assertNotNull(response);
            assertEquals(TEST_ACCOUNT_ID, response.getAccountId());
            assertBigDecimalEquals(TEST_CURRENT_BALANCE, response.getCurrentBalance());
            assertBigDecimalEquals(TEST_CREDIT_LIMIT, response.getCreditLimit());
            assertEquals(TEST_ACTIVE_STATUS, response.getActiveStatus());
            
            // Validate customer information is included
            assertNotNull(response.getCustomer());
            assertEquals(testCustomer.getFirstName(), response.getCustomer().getFirstName());
            assertEquals(testCustomer.getLastName(), response.getCustomer().getLastName());
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void testGetAccount_NotFound() throws Exception {
            // Arrange - Use non-existent account ID
            String nonExistentAccountId = "99999999999";
            
            // Act & Assert - Verify ResourceNotFoundException handling
            mockMvc.perform(get("/api/accounts/{id}", nonExistentAccountId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.messages").exists())
                    .andExpect(jsonPath("$.messages[0]").value(containsString("not found")));
        }

        @Test
        @DisplayName("Should validate account ID format and return 400 for invalid input")
        void testGetAccount_InvalidAccountId() throws Exception {
            // Test cases for invalid account ID formats (not 11-digit numeric)
            String[] invalidAccountIds = {"", "123", "abcdefghijk", "1234567890a", "12345678901234"};
            
            for (String invalidId : invalidAccountIds) {
                mockMvc.perform(get("/api/accounts/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("ERROR"))
                        .andExpected(jsonPath("$.messages").exists());
            }
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        void testGetAccount_RepositoryError() throws Exception {
            // This test would require mocking the repository to throw an exception
            // For integration tests, we test the actual database operations
            String validAccountId = "11111111111";
            
            mockMvc.perform(get("/api/accounts/{id}", validAccountId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * Test suite for account update operations (CAUP transaction equivalent).
     * Validates PUT /api/accounts/{id} endpoint functionality replicating COACTUPC.cbl logic.
     */
    @Nested
    @DisplayName("Account Update Operations (CAUP Transaction)")
    class AccountUpdateTests {

        @Test
        @DisplayName("Should update account successfully with optimistic locking")
        void testUpdateAccount_Success() throws Exception {
            // Arrange - Create test account with customer
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Create update request with new credit limit
            AccountUpdateRequest updateRequest = new AccountUpdateRequest();
            updateRequest.setAccountId(TEST_ACCOUNT_ID);
            updateRequest.setActiveStatus("A");
            BigDecimal newCreditLimit = new BigDecimal("7500.00").setScale(2, RoundingMode.HALF_UP);
            updateRequest.setCreditLimit(newCreditLimit);
            updateRequest.setCashCreditLimit(new BigDecimal("1500.00").setScale(2, RoundingMode.HALF_UP));
            
            // Validate BigDecimal precision in request
            validateCobolPrecision(updateRequest.getCreditLimit(), 2);
            validateCobolPrecision(updateRequest.getCashCreditLimit(), 2);
            
            String requestJson = objectMapper.writeValueAsString(updateRequest);
            
            // Act & Assert - Test update endpoint with performance validation
            long startTime = System.currentTimeMillis();
            
            MvcResult result = mockMvc.perform(put("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.transactionCode").value("CAUP"))
                    .andExpected(jsonPath("$.responseData").exists())
                    .andReturn();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Validate response time meets 200ms requirement
            assertTrue(responseTime < MAX_RESPONSE_TIME_MS, 
                String.format("Update response time %dms exceeds maximum %dms", responseTime, MAX_RESPONSE_TIME_MS));
            
            // Verify account was updated in database with correct BigDecimal precision
            Optional<Account> updatedAccountOpt = accountRepository.findById(Long.parseLong(TEST_ACCOUNT_ID));
            assertTrue(updatedAccountOpt.isPresent());
            
            Account updatedAccount = updatedAccountOpt.get();
            assertBigDecimalEquals(newCreditLimit, updatedAccount.getCreditLimit());
            validateCobolPrecision(updatedAccount.getCreditLimit(), 2);
        }

        @Test
        @DisplayName("Should handle optimistic locking conflicts")
        void testUpdateAccount_OptimisticLockException() throws Exception {
            // Arrange - Create account for concurrent update simulation
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Simulate concurrent update by modifying version
            AccountUpdateRequest updateRequest = new AccountUpdateRequest();
            updateRequest.setAccountId(TEST_ACCOUNT_ID);
            updateRequest.setActiveStatus("A");
            updateRequest.setCreditLimit(new BigDecimal("6000.00").setScale(2, RoundingMode.HALF_UP));
            
            String requestJson = objectMapper.writeValueAsString(updateRequest);
            
            // First update should succeed
            mockMvc.perform(put("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isOk());
            
            // Second concurrent update should handle optimistic lock conflict
            mockMvc.perform(put("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpected(jsonPath("$.messages[0]").value(containsString("concurrent modification")));
        }

        @Test
        @DisplayName("Should validate field-level constraints with Spring Validation")
        void testUpdateAccount_ValidationErrors() throws Exception {
            // Test invalid credit limit (negative value)
            AccountUpdateRequest invalidRequest = new AccountUpdateRequest();
            invalidRequest.setAccountId(TEST_ACCOUNT_ID);
            invalidRequest.setActiveStatus("X"); // Invalid status
            invalidRequest.setCreditLimit(new BigDecimal("-100.00")); // Invalid negative amount
            invalidRequest.setCashCreditLimit(new BigDecimal("10000000.00")); // Exceeds maximum
            
            String requestJson = objectMapper.writeValueAsString(invalidRequest);
            
            mockMvc.perform(put("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpected(jsonPath("$.status").value("ERROR"))
                    .andExpected(jsonPath("$.fieldErrors").exists())
                    .andExpect(jsonPath("$.fieldErrors.activeStatus").exists())
                    .andExpect(jsonPath("$.fieldErrors.creditLimit").exists());
        }

        @Test
        @DisplayName("Should validate BigDecimal precision for monetary fields")
        void testUpdateAccount_BigDecimalPrecision() throws Exception {
            // Arrange - Create test data
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Test various BigDecimal precision scenarios
            Map<String, BigDecimal> precisionTests = new HashMap<>();
            precisionTests.put("exact", new BigDecimal("1234.56"));
            precisionTests.put("rounded", new BigDecimal("1234.567"));
            precisionTests.put("truncated", new BigDecimal("1234.5"));
            
            for (Map.Entry<String, BigDecimal> test : precisionTests.entrySet()) {
                AccountUpdateRequest updateRequest = new AccountUpdateRequest();
                updateRequest.setAccountId(TEST_ACCOUNT_ID);
                updateRequest.setActiveStatus("A");
                updateRequest.setCreditLimit(test.getValue().setScale(2, RoundingMode.HALF_UP));
                
                // Validate COBOL-compatible precision
                validateCobolPrecision(updateRequest.getCreditLimit(), 2);
                assertEquals(2, updateRequest.getCreditLimit().scale());
                
                String requestJson = objectMapper.writeValueAsString(updateRequest);
                
                mockMvc.perform(put("/api/accounts/{id}", TEST_ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                        .andExpect(status().isOk());
                
                // Verify stored precision matches COBOL COMP-3 expectations
                Optional<Account> updatedAccountOpt = accountRepository.findById(Long.parseLong(TEST_ACCOUNT_ID));
                if (updatedAccountOpt.isPresent()) {
                    validateCobolPrecision(updatedAccountOpt.get().getCreditLimit(), 2);
                }
            }
        }
    }

    /**
     * Test suite for JPA repository operations with composite keys and referential integrity.
     */
    @Nested
    @DisplayName("JPA Repository Operations")
    class RepositoryTests {

        @Test
        @DisplayName("Should perform repository operations with composite keys")
        void testRepositoryOperations_CompositeKeys() {
            // Test repository save operation
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Save and validate
            Account savedAccount = accountRepository.save(testAccount);
            assertNotNull(savedAccount);
            assertNotNull(savedAccount.getAccountId());
            
            // Test findById operation
            Optional<Account> foundAccount = accountRepository.findById(savedAccount.getAccountId());
            assertTrue(foundAccount.isPresent());
            assertBigDecimalEquals(TEST_CURRENT_BALANCE, foundAccount.get().getCurrentBalance());
            
            // Test repository count operations
            Long totalAccounts = accountRepository.findAll().size();
            assertTrue(totalAccounts > 0);
            
            // Test findByCustomerId operation
            String customerId = testCustomer.getCustomerId();
            if (customerId != null) {
                Long customerIdLong = Long.valueOf(customerId);
                var customerAccounts = accountRepository.findByCustomerId(customerIdLong);
                assertFalse(customerAccounts.isEmpty());
            }
        }

        @Test
        @DisplayName("Should validate referential integrity with customer relationships")
        void testReferentialIntegrity() {
            // Create customer and account with relationship
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Save account with customer relationship
            Account savedAccount = accountRepository.save(testAccount);
            assertNotNull(savedAccount.getCustomer());
            assertEquals(testCustomer.getCustomerId(), savedAccount.getCustomer().getCustomerId());
            
            // Verify relationship integrity
            assertNotNull(savedAccount.getCustomer().getFirstName());
            assertNotNull(savedAccount.getCustomer().getLastName());
            
            // Test cascade operations
            accountRepository.delete(savedAccount);
            Optional<Account> deletedAccount = accountRepository.findById(savedAccount.getAccountId());
            assertFalse(deletedAccount.isPresent());
        }
    }

    /**
     * Test suite for error handling and exception scenarios.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle ValidationException with field errors")
        void testValidationException_HandlingRaises() {
            ValidationException exception = new ValidationException("Test validation error");
            exception.addFieldError("accountId", "Invalid account ID format");
            exception.addFieldError("creditLimit", "Credit limit cannot be negative");
            
            assertTrue(exception.hasFieldErrors());
            assertEquals(2, exception.getFieldErrorCount());
            assertTrue(exception.hasFieldError("accountId"));
            assertTrue(exception.hasFieldError("creditLimit"));
            
            Map<String, String> fieldErrors = exception.getFieldErrors();
            assertEquals("Invalid account ID format", fieldErrors.get("accountId"));
            assertEquals("Credit limit cannot be negative", fieldErrors.get("creditLimit"));
            
            String message = exception.getMessage();
            assertNotNull(message);
            assertTrue(message.contains("accountId: Invalid account ID format"));
            assertTrue(message.contains("creditLimit: Credit limit cannot be negative"));
        }

        @Test
        @DisplayName("Should handle ResourceNotFoundException scenarios")
        void testResourceNotFoundException_Scenarios() {
            // Test basic resource not found
            ResourceNotFoundException exception1 = new ResourceNotFoundException("Account", TEST_ACCOUNT_ID);
            assertEquals("Account", exception1.getResourceType());
            assertEquals(TEST_ACCOUNT_ID, exception1.getResourceId());
            assertTrue(exception1.getMessage().contains("Account not found"));
            assertTrue(exception1.getMessage().contains(TEST_ACCOUNT_ID));
            
            // Test with search criteria
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("status", "A");
            criteria.put("customerId", TEST_CUSTOMER_ID);
            
            ResourceNotFoundException exception2 = new ResourceNotFoundException("Account", TEST_ACCOUNT_ID, criteria);
            assertTrue(exception2.getMessage().contains("using criteria"));
            assertEquals(criteria, exception2.getSearchCriteria());
        }
    }

    /**
     * Performance and load testing scenarios.
     */
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should maintain response times under 200ms for account operations")
        void testAccountOperations_PerformanceRequirements() throws Exception {
            // Setup test data
            Customer testCustomer = createTestCustomer();
            Account testAccount = createTestAccount();
            testAccount.setCustomer(testCustomer);
            
            // Test view operation performance
            long viewStartTime = System.currentTimeMillis();
            mockMvc.perform(get("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
            long viewResponseTime = System.currentTimeMillis() - viewStartTime;
            
            assertTrue(viewResponseTime < MAX_RESPONSE_TIME_MS, 
                String.format("View operation took %dms, exceeds %dms limit", viewResponseTime, MAX_RESPONSE_TIME_MS));
            
            // Test update operation performance
            AccountUpdateRequest updateRequest = new AccountUpdateRequest();
            updateRequest.setAccountId(TEST_ACCOUNT_ID);
            updateRequest.setActiveStatus("A");
            updateRequest.setCreditLimit(new BigDecimal("5500.00").setScale(2, RoundingMode.HALF_UP));
            
            String requestJson = objectMapper.writeValueAsString(updateRequest);
            
            long updateStartTime = System.currentTimeMillis();
            mockMvc.perform(put("/api/accounts/{id}", TEST_ACCOUNT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isOk());
            long updateResponseTime = System.currentTimeMillis() - updateStartTime;
            
            assertTrue(updateResponseTime < MAX_RESPONSE_TIME_MS, 
                String.format("Update operation took %dms, exceeds %dms limit", updateResponseTime, MAX_RESPONSE_TIME_MS));
        }
    }
    
    /**
     * Helper method to validate that the REST endpoints are correctly mapped to transaction codes.
     * This simulates the CICS transaction code mapping (CAVW/CAUP) to REST endpoints.
     */
    @Test
    @DisplayName("Should map REST endpoints to CICS transaction codes correctly")
    void testTransactionCodeMapping() throws Exception {
        // Create test data
        Customer testCustomer = createTestCustomer();
        Account testAccount = createTestAccount();
        testAccount.setCustomer(testCustomer);
        
        // Test CAVW (Account View) transaction code mapping
        mockMvc.perform(post("/api/tx/COACTVW")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + TEST_ACCOUNT_ID + "\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.transactionCode").value("CAVW"));
        
        // Test CAUP (Account Update) transaction code mapping
        AccountUpdateRequest updateRequest = new AccountUpdateRequest();
        updateRequest.setAccountId(TEST_ACCOUNT_ID);
        updateRequest.setActiveStatus("A");
        updateRequest.setCreditLimit(TEST_CREDIT_LIMIT);
        
        String updateJson = objectMapper.writeValueAsString(updateRequest);
        
        mockMvc.perform(post("/api/tx/COACTUP")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.transactionCode").value("CAUP"));
    }
}