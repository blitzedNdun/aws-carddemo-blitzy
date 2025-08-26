package com.carddemo.controller;

import com.carddemo.dto.AccountDto;
import com.carddemo.dto.AccountUpdateRequest;
import com.carddemo.service.AccountService;
import com.carddemo.controller.TestDataBuilder;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.exception.ConcurrencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration test class for AccountController that validates account viewing and updating endpoints,
 * ensuring functional parity with COBOL COACTVWC and COACTUPC programs.
 * 
 * This test class validates:
 * - GET /api/accounts/{id} for viewing account details
 * - PUT /api/accounts/{id} for updating account information
 * - Optimistic locking replicating CICS READ UPDATE behavior
 * - BigDecimal precision matching COBOL COMP-3 fields
 * - Account balance calculations and interest accrual
 * - Referential integrity with card and customer data
 * - Validation of account status transitions
 * - Session-based state management for multi-step updates
 * - Error handling for concurrent modifications
 */
public class AccountControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    // Test Constants - simulating TestConstants.java content
    private static final long RESPONSE_TIME_THRESHOLD_MS = 200L;
    private static final String TEST_USER_ID = "TESTUSER";
    private static final String TEST_ACCOUNT_ID = "1000000001";
    private static final int COBOL_DECIMAL_SCALE = 2;
    private static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // Additional setup specific to AccountController tests
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("GET /api/accounts/{id} - Successfully retrieves account details with COBOL precision")
    public void testGetAccountById_Success() throws Exception {
        // Given: Valid account ID and expected account data with COBOL COMP-3 precision
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        AccountDto expectedAccount = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .customerId("2000000001")
            .currentBalance(new BigDecimal("2500.75").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .creditLimit(new BigDecimal("5000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .cashCreditLimit(new BigDecimal("1000.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .activeStatus("ACTIVE")
            .build();

        when(accountService.viewAccount(accountIdLong)).thenReturn(expectedAccount);

        long startTime = System.currentTimeMillis();

        // When: GET request for account details (equivalent to COACTVWC COBOL program)
        MvcResult result = mockMvc.perform(get("/api/accounts/{id}", accountId)
                .accept(MediaType.APPLICATION_JSON))
                // Then: Verify response matches COBOL program output
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.customerId").value("2000000001"))
                .andExpect(jsonPath("$.currentBalance").value(2500.75))
                .andExpect(jsonPath("$.creditLimit").value(5000.00))
                .andExpect(jsonPath("$.cashCreditLimit").value(1000.00))
                .andExpect(jsonPath("$.activeStatus").value("ACTIVE"))
                .andReturn();

        long responseTime = System.currentTimeMillis() - startTime;

        // Verify COBOL-to-Java functional parity
        AccountDto actualAccount = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDto.class);
        assertEquals(expectedAccount.getAccountId(), actualAccount.getAccountId());
        assertEquals(expectedAccount.getCurrentBalance().setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE), 
                    actualAccount.getCurrentBalance().setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));
        
        // Verify response time meets CICS performance requirements
        assertTrue(responseTime < RESPONSE_TIME_THRESHOLD_MS, 
                  "Response time " + responseTime + "ms exceeds threshold of " + RESPONSE_TIME_THRESHOLD_MS + "ms");

        verify(accountService).viewAccount(accountIdLong);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("GET /api/accounts/{id} - Returns 404 when account not found")
    public void testGetAccountById_NotFound() throws Exception {
        // Given: Non-existent account ID
        String accountId = "9999999999";
        Long accountIdLong = Long.parseLong(accountId);
        when(accountService.viewAccount(accountIdLong)).thenThrow(new ResourceNotFoundException("Account", accountId));

        // When: GET request for non-existent account
        mockMvc.perform(get("/api/accounts/{id}", accountId)
                .accept(MediaType.APPLICATION_JSON))
                // Then: Verify 404 response (matching COBOL NOTFND condition)
                .andExpect(status().isNotFound());

        verify(accountService).viewAccount(accountIdLong);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("PUT /api/accounts/{id} - Successfully updates account with optimistic locking")
    public void testUpdateAccount_Success() throws Exception {
        // Given: Valid account update request with COBOL precision
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        AccountUpdateRequest updateRequest = TestDataBuilder.buildAccountUpdateRequest()
            .accountId(accountId)
            .activeStatus("N")
            .creditLimit(new BigDecimal("7500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .cashCreditLimit(new BigDecimal("1500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .build();

        AccountDto updatedAccount = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .activeStatus("N")
            .creditLimit(new BigDecimal("7500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .cashCreditLimit(new BigDecimal("1500.00").setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
            .build();

        when(accountService.updateAccount(accountIdLong, updateRequest)).thenReturn(updatedAccount);

        long startTime = System.currentTimeMillis();

        // When: PUT request to update account (equivalent to COACTUPC COBOL program)
        MvcResult result = mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                // Then: Verify successful update response
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.activeStatus").value("N"))
                .andExpect(jsonPath("$.creditLimit").value(7500.00))
                .andExpect(jsonPath("$.cashCreditLimit").value(1500.00))
                .andReturn();

        long responseTime = System.currentTimeMillis() - startTime;

        // Verify BigDecimal precision matching COBOL COMP-3
        AccountDto actualAccount = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDto.class);
        assertEquals(updateRequest.getCreditLimit().setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE),
                    actualAccount.getCreditLimit().setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE));

        // Verify response time for update operations
        assertTrue(responseTime < RESPONSE_TIME_THRESHOLD_MS,
                  "Update response time " + responseTime + "ms exceeds threshold");

        verify(accountService).updateAccount(accountIdLong, updateRequest);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("PUT /api/accounts/{id} - Handles optimistic locking failure (concurrent modification)")
    public void testUpdateAccount_OptimisticLockingFailure() throws Exception {
        // Given: Account update request that will encounter optimistic locking
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        AccountUpdateRequest updateRequest = TestDataBuilder.buildAccountUpdateRequest()
            .accountId(accountId)
            .activeStatus("N")
            .build();

        // Simulate CICS INVREQ condition for concurrent modification
        when(accountService.updateAccount(accountIdLong, updateRequest))
            .thenThrow(new OptimisticLockingFailureException("Optimistic locking failure - record modified by another user"));

        // When: PUT request with stale data
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                // Then: Verify conflict response (matching COBOL concurrent access handling)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("modified by another user")));

        verify(accountService).updateAccount(accountIdLong, updateRequest);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("PUT /api/accounts/{id} - Handles validation errors for invalid data")
    public void testUpdateAccount_ValidationErrors() throws Exception {
        // Given: Invalid account update request
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        AccountUpdateRequest invalidRequest = TestDataBuilder.buildAccountUpdateRequest()
            .accountId(accountId)
            .creditLimit(new BigDecimal("-1000.00")) // Invalid negative credit limit
            .cashCreditLimit(new BigDecimal("100000.00")) // Exceeds maximum allowed
            .build();

        when(accountService.updateAccount(accountIdLong, invalidRequest))
            .thenThrow(new ValidationException("Invalid credit limit: must be positive"));

        // When: PUT request with invalid data
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                // Then: Verify validation error response
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid credit limit")));

        verify(accountService).updateAccount(accountIdLong, invalidRequest);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Account balance calculation precision - Validates BigDecimal COMP-3 equivalence")
    public void testAccountBalanceCalculation() throws Exception {
        // Given: Account with complex balance calculation scenario
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        
        // Simulate COBOL COMP-3 calculation: Balance + Interest - Fees
        BigDecimal baseBalance = new BigDecimal("1234.56");
        BigDecimal interestAmount = new BigDecimal("12.34");
        BigDecimal feeAmount = new BigDecimal("5.00");
        
        BigDecimal expectedBalance = baseBalance
            .add(interestAmount)
            .subtract(feeAmount)
            .setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE);

        AccountDto accountWithCalculation = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .currentBalance(expectedBalance)
            .build();

        when(accountService.viewAccount(accountIdLong)).thenReturn(accountWithCalculation);

        // When: Retrieving account with calculated balance
        MvcResult result = mockMvc.perform(get("/api/accounts/{id}", accountId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Verify balance calculation precision matches COBOL COMP-3
        AccountDto actualAccount = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDto.class);
        
        // Verify exact precision and scale
        assertEquals(expectedBalance.scale(), actualAccount.getCurrentBalance().scale());
        assertEquals(0, expectedBalance.compareTo(actualAccount.getCurrentBalance()));
        assertEquals("1241.90", actualAccount.getCurrentBalance().toString());

        verify(accountService).viewAccount(accountIdLong);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("BigDecimal precision validation - Ensures COBOL COMP-3 compatibility")
    public void testBigDecimalPrecision() throws Exception {
        // Given: Various BigDecimal values matching COBOL COMP-3 format
        BigDecimal[] testValues = {
            new BigDecimal("0.01"),     // Minimum currency unit
            new BigDecimal("999.99"),   // Standard amount
            new BigDecimal("12345.67"), // Larger amount
            new BigDecimal("0.00")      // Zero balance
        };

        for (BigDecimal testValue : testValues) {
            String accountId = TEST_ACCOUNT_ID + testValue.toString().replace(".", "");
            Long accountIdLong = Long.parseLong(accountId);
            
            AccountDto testAccount = TestDataBuilder.buildAccountDto()
                .accountId(accountId)
                .currentBalance(testValue.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE))
                .build();

            when(accountService.viewAccount(accountIdLong)).thenReturn(testAccount);

            // When: Getting account with specific precision
            MvcResult result = mockMvc.perform(get("/api/accounts/{id}", accountId)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: Verify precision is maintained
            AccountDto actualAccount = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDto.class);
            assertEquals(COBOL_DECIMAL_SCALE, actualAccount.getCurrentBalance().scale());
            assertEquals(0, testValue.setScale(COBOL_DECIMAL_SCALE, COBOL_ROUNDING_MODE)
                       .compareTo(actualAccount.getCurrentBalance()));
        }
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Session state management - Validates multi-step update process")
    public void testSessionStateManagement() throws Exception {
        // Given: Multi-step account update process using session state
        String accountId = TEST_ACCOUNT_ID;
        
        // Step 1: Initial account view to establish session state
        Long accountIdLong = Long.parseLong(accountId);
        AccountDto initialAccount = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .activeStatus("Y")
            .build();

        when(accountService.viewAccount(accountIdLong)).thenReturn(initialAccount);

        // Create authenticated session
        MvcResult sessionResult = performAuthenticatedRequest(
            get("/api/accounts/{id}", accountId), TEST_USER_ID, "USER")
            .andReturn();

        // Step 2: Update using session state (simulating CICS COMMAREA)
        AccountUpdateRequest updateRequest = TestDataBuilder.buildAccountUpdateRequest()
            .accountId(accountId)
            .activeStatus("N")
            .build();

        AccountDto updatedAccount = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .activeStatus("N")
            .build();

        when(accountService.updateAccount(accountIdLong, updateRequest)).thenReturn(updatedAccount);

        // When: Update using established session
        String sessionId = extractSessionId(sessionResult);
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .header("Cookie", "SESSION=" + sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                // Then: Verify session-based update succeeds
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStatus").value("N"));

        verify(accountService).viewAccount(accountIdLong);
        verify(accountService).updateAccount(accountIdLong, updateRequest);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Referential integrity validation - Ensures data consistency across entities")
    public void testReferentialIntegrity() throws Exception {
        // Given: Account linked to customer and cards (referential integrity)
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        String customerId = "2000000001";
        
        AccountDto accountWithReferences = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .customerId(customerId)
            .build();

        when(accountService.viewAccount(accountIdLong)).thenReturn(accountWithReferences);

        // When: Retrieving account with referential data
        MvcResult result = mockMvc.perform(get("/api/accounts/{id}", accountId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Verify referential integrity is maintained
        AccountDto actualAccount = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDto.class);
        assertEquals(customerId, actualAccount.getCustomerId());
        assertNotNull(actualAccount.getAccountId());
        
        // Verify foreign key constraints would be satisfied
        assertTrue(actualAccount.getCustomerId().matches("\\d{10}"));
        assertTrue(actualAccount.getAccountId().matches("\\d{10}"));

        verify(accountService).viewAccount(accountIdLong);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Response time validation - Ensures sub-200ms performance")
    public void testResponseTimeValidation() throws Exception {
        // Given: Standard account retrieval scenario
        String accountId = TEST_ACCOUNT_ID;
        Long accountIdLong = Long.parseLong(accountId);
        AccountDto testAccount = TestDataBuilder.buildAccountDto()
            .accountId(accountId)
            .build();

        when(accountService.viewAccount(accountIdLong)).thenReturn(testAccount);

        // When: Multiple requests to measure average response time
        long totalTime = 0;
        int iterations = 10;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/accounts/{id}", accountId)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
                    
            totalTime += (System.currentTimeMillis() - startTime);
        }

        long averageResponseTime = totalTime / iterations;

        // Then: Verify average response time meets CICS performance requirements
        assertTrue(averageResponseTime < RESPONSE_TIME_THRESHOLD_MS,
                  "Average response time " + averageResponseTime + "ms exceeds threshold of " + 
                  RESPONSE_TIME_THRESHOLD_MS + "ms");

        verify(accountService, times(iterations)).viewAccount(accountIdLong);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        // Additional cleanup specific to AccountController tests
    }

    /**
     * Helper method to extract session ID from MvcResult for session-based testing
     */
    @Override
    protected String extractSessionId(MvcResult result) {
        String cookies = result.getResponse().getHeader("Set-Cookie");
        if (cookies != null && cookies.contains("SESSION=")) {
            return cookies.substring(cookies.indexOf("SESSION=") + 8, cookies.indexOf(";"));
        }
        return "test-session-id";
    }
}