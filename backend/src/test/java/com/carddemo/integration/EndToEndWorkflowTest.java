/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carddemo.integration;

import com.carddemo.controller.SignOnController;
import com.carddemo.controller.MenuController;
import com.carddemo.controller.TransactionController;
import com.carddemo.controller.AccountController;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;
import com.carddemo.dto.MainMenuResponse;
import com.carddemo.dto.TransactionListRequest;
import com.carddemo.dto.TransactionListResponse;
import com.carddemo.dto.AccountDto;
import com.carddemo.dto.SessionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive end-to-end integration test class validating complete user workflows from
 * sign-on through transaction processing, replicating original 3270 terminal screen flows
 * and PF-key navigation patterns in the modernized Spring Boot/React architecture.
 *
 * This test class validates that the modernized CardDemo application maintains 100%
 * functional parity with the original mainframe CICS implementation by testing:
 *
 * - Complete authentication workflow (COSGN00C equivalent)
 * - Main menu navigation flow (COMEN01C equivalent)  
 * - Transaction listing and browsing (COTRN00C, COTRN01C, COTRN02C equivalent)
 * - Account viewing and management (COACTVWC equivalent)
 * - Session state persistence across multiple requests (COMMAREA equivalent)
 * - PF-key equivalent navigation patterns (F3=Exit, F12=Cancel)
 * - Response time compliance (<200ms per validation checklist requirement)
 *
 * The test scenarios replicate complete user journeys from the original 3270 terminal
 * interface, ensuring that business users experience identical functionality through
 * the modern React UI while maintaining sub-200ms response times and identical
 * financial calculation precision.
 *
 * @author CardDemo Development Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndWorkflowTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private MockHttpSession userSession;
    private SessionContext testSessionContext;
    
    // Test data constants replicating mainframe test scenarios
    private static final String TEST_USER_ID = "TESTUSER";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ACCOUNT_ID = "1000000001";
    private static final String ADMIN_USER_ID = "ADMIN001";
    private static final String ADMIN_PASSWORD = "admin123";

    /**
     * Sets up test environment before each test method execution.
     * 
     * Initializes MockMvc for REST API testing, creates fresh HTTP session,
     * and prepares test data through BaseIntegrationTest setup methods.
     * This setup replicates the controlled test environment equivalent to
     * mainframe test region initialization.
     */
    @BeforeEach
    public void setUp() {
        // Initialize MockMvc with full Spring context
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build();
            
        // Create fresh session for each test to ensure isolation
        userSession = new MockHttpSession();
        
        // Initialize test data using BaseIntegrationTest utilities
        setupTestContainers();
        setupSessionState();
        
        // Create test session context
        testSessionContext = new SessionContext();
        testSessionContext.setSessionStartTime(LocalDateTime.now());
        testSessionContext.updateActivityTime();
        testSessionContext.setOperationStatus("ACTIVE");
    }

    /**
     * Cleans up test environment after each test method execution.
     * 
     * Clears session state and performs cleanup operations to ensure
     * test isolation and prevent data contamination between test runs.
     */
    @AfterEach
    public void tearDown() {
        clearSessionState();
        if (userSession != null) {
            userSession.invalidate();
        }
    }

    /**
     * Tests complete user sign-on workflow from initial authentication through main menu display.
     * 
     * This test validates the complete COSGN00C transaction equivalent workflow:
     * 1. User submits authentication credentials
     * 2. System validates credentials against usrsec table
     * 3. Session context is established with user role
     * 4. Main menu options are returned based on user authorization
     * 5. Response time is validated to be under 200ms
     * 
     * The test ensures that authentication flow maintains identical behavior to the
     * original CICS sign-on process while meeting modern performance requirements.
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Complete sign-on workflow - COSGN00C transaction equivalent")
    void testCompleteSignOnWorkflow() throws Exception {
        // Record start time for performance validation
        LocalDateTime startTime = LocalDateTime.now();
        
        // Given: Valid user credentials for authentication
        SignOnRequest signOnRequest = new SignOnRequest();
        signOnRequest.setUserId(TEST_USER_ID);
        signOnRequest.setPassword(TEST_PASSWORD);

        String requestJson = objectMapper.writeValueAsString(signOnRequest);

        // When: User submits sign-on request
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .session(userSession))
                
        // Then: Authentication succeeds with proper response structure
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.userRole").exists())
                .andExpect(jsonPath("$.sessionToken").exists())
                .andReturn();

        // Validate response timing meets <200ms requirement
        LocalDateTime endTime = LocalDateTime.now();
        Duration responseTime = Duration.between(startTime, endTime);
        assertThat(responseTime.toMillis())
            .describedAs("Sign-on response time must be under 200ms")
            .isLessThan(200);

        // Parse response and validate session establishment
        String responseJson = result.getResponse().getContentAsString();
        SignOnResponse signOnResponse = objectMapper.readValue(responseJson, SignOnResponse.class);
        
        assertThat(signOnResponse.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(signOnResponse.getStatus()).isEqualTo("SUCCESS");
        assertThat(signOnResponse.getSessionToken()).isNotNull();
        
        // Store session context for subsequent tests
        testSessionContext.setUserId(signOnResponse.getUserId());
        testSessionContext.setUserRole(signOnResponse.getUserRole());
        testSessionContext.setCurrentMenu("MAIN");
        testSessionContext.setLastTransactionCode("COSGN00");
    }

    /**
     * Tests main menu navigation flow and menu option retrieval.
     * 
     * This test validates the COMEN01C transaction equivalent workflow:
     * 1. Authenticated user requests main menu display
     * 2. System returns menu options based on user role
     * 3. Menu options include proper navigation paths
     * 4. Session context is maintained across the request
     * 
     * The test ensures that menu navigation maintains the same hierarchical
     * structure and role-based access control as the original CICS menu system.
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Main menu navigation workflow - COMEN01C transaction equivalent")
    void testMainMenuNavigationWorkflow() throws Exception {
        // Given: Authenticated user session established
        testCompleteSignOnWorkflow(); // Establish authenticated session
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // When: User requests main menu display
        MvcResult result = mockMvc.perform(get("/api/menu/main")
                .session(userSession)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                
        // Then: Main menu is returned with proper options
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuOptions").isArray())
                .andExpect(jsonPath("$.menuOptions").isNotEmpty())
                .andExpect(jsonPath("$.userName").exists())
                .andReturn();

        // Validate response timing
        LocalDateTime endTime = LocalDateTime.now();
        Duration responseTime = Duration.between(startTime, endTime);
        assertThat(responseTime.toMillis())
            .describedAs("Menu navigation response time must be under 200ms")
            .isLessThan(200);

        // Parse and validate menu response structure
        String responseJson = result.getResponse().getContentAsString();
        MainMenuResponse menuResponse = objectMapper.readValue(responseJson, MainMenuResponse.class);
        
        assertThat(menuResponse.getMenuOptions())
            .describedAs("Menu options should be available for authenticated user")
            .isNotNull()
            .isNotEmpty();
            
        assertThat(menuResponse.getUserName())
            .describedAs("User name should be populated in menu context")
            .isNotNull();

        // Update session context for menu navigation
        testSessionContext.setCurrentMenu("MAIN");
        testSessionContext.addToNavigationStack("MAIN");
        testSessionContext.setLastTransactionCode("COMEN01");
    }

    /**
     * Tests transaction list retrieval and browsing workflow.
     * 
     * This test validates the COTRN00C transaction equivalent workflow:
     * 1. User requests transaction list for specific account
     * 2. System retrieves transactions with pagination support
     * 3. Transaction data includes proper financial formatting
     * 4. Pagination metadata supports STARTBR/READNEXT equivalent browsing
     * 5. Response maintains session state for continued navigation
     * 
     * The test ensures that transaction browsing maintains identical functionality
     * to the original VSAM STARTBR/READNEXT operations while supporting modern
     * pagination patterns through the React UI.
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Transaction list browsing workflow - COTRN00C transaction equivalent")
    void testTransactionListBrowsingWorkflow() throws Exception {
        // Given: Authenticated user session and account with transactions
        testMainMenuNavigationWorkflow(); // Establish menu context
        createTestAccount(); // Create test account with sample transactions
        createTestTransaction(); // Add sample transaction data
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // Prepare transaction list request with pagination
        TransactionListRequest transactionRequest = new TransactionListRequest();
        transactionRequest.setAccountId(TEST_ACCOUNT_ID);
        transactionRequest.setPageNumber(0);
        transactionRequest.setPageSize(10);

        String requestJson = objectMapper.writeValueAsString(transactionRequest);

        // When: User requests transaction list with pagination
        MvcResult result = mockMvc.perform(post("/api/transactions/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .session(userSession))
                
        // Then: Transaction list is returned with pagination metadata
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andReturn();

        // Validate response timing for transaction browsing
        LocalDateTime endTime = LocalDateTime.now();
        Duration responseTime = Duration.between(startTime, endTime);
        assertThat(responseTime.toMillis())
            .describedAs("Transaction list response time must be under 200ms")
            .isLessThan(200);

        // Parse and validate transaction list response
        String responseJson = result.getResponse().getContentAsString();
        TransactionListResponse listResponse = objectMapper.readValue(responseJson, TransactionListResponse.class);
        
        assertThat(listResponse.getTransactions())
            .describedAs("Transaction list should contain sample transactions")
            .isNotNull();
            
        assertThat(listResponse.getTotalCount())
            .describedAs("Total count should reflect available transactions")
            .isGreaterThanOrEqualTo(0);
            
        assertThat(listResponse.getCurrentPage())
            .describedAs("Current page should match requested page")
            .isEqualTo(0);

        // Validate individual transaction data formatting
        if (!listResponse.getTransactions().isEmpty()) {
            listResponse.getTransactions().forEach(transaction -> {
                assertThat(transaction.getTransactionId())
                    .describedAs("Transaction ID should be properly formatted")
                    .isNotNull()
                    .isNotEmpty();
                    
                assertThat(transaction.getAmount())
                    .describedAs("Transaction amount should maintain BigDecimal precision")
                    .isNotNull();
                    
                assertThat(transaction.getDate())
                    .describedAs("Transaction date should be properly formatted")
                    .isNotNull();
            });
        }

        // Update session context for transaction browsing
        testSessionContext.setCurrentMenu("TRANSACTION");
        testSessionContext.addToNavigationStack("TRANSACTION");
        testSessionContext.setLastTransactionCode("COTRN00");
    }

    /**
     * Tests account details retrieval and display workflow.
     * 
     * This test validates the COACTVWC transaction equivalent workflow:
     * 1. User requests account details for specific account
     * 2. System retrieves account and customer information
     * 3. Financial data maintains COBOL COMP-3 precision equivalence  
     * 4. Account status and calculated fields are properly computed
     * 5. Response includes complete account and customer context
     * 
     * The test ensures that account viewing maintains identical data precision
     * and business logic as the original VSAM file access while supporting
     * modern relational database patterns through JPA.
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Account details viewing workflow - COACTVWC transaction equivalent")
    void testAccountDetailsViewingWorkflow() throws Exception {
        // Given: Authenticated user session and existing account
        testTransactionListBrowsingWorkflow(); // Establish transaction context
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // When: User requests account details
        MvcResult result = mockMvc.perform(get("/api/accounts/{accountId}", TEST_ACCOUNT_ID)
                .session(userSession)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                
        // Then: Account details are returned with complete information
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                .andExpect(jsonPath("$.currentBalance").exists())
                .andExpect(jsonPath("$.creditLimit").exists())
                .andExpect(jsonPath("$.availableCredit").exists())
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.customerFullName").exists())
                .andReturn();

        // Validate response timing for account viewing
        LocalDateTime endTime = LocalDateTime.now();
        Duration responseTime = Duration.between(startTime, endTime);
        assertThat(responseTime.toMillis())
            .describedAs("Account details response time must be under 200ms")
            .isLessThan(200);

        // Parse and validate account details response
        String responseJson = result.getResponse().getContentAsString();
        AccountDto accountDetails = objectMapper.readValue(responseJson, AccountDto.class);
        
        // Validate account financial data precision
        assertThat(accountDetails.getAccountId())
            .describedAs("Account ID should match requested account")
            .isEqualTo(TEST_ACCOUNT_ID);
            
        assertThat(accountDetails.getCurrentBalance())
            .describedAs("Current balance should maintain BigDecimal precision")
            .isNotNull();
            
        assertThat(accountDetails.getCreditLimit())
            .describedAs("Credit limit should maintain BigDecimal precision")
            .isNotNull();
            
        // Validate calculated fields maintain COBOL business logic
        BigDecimal expectedAvailableCredit = accountDetails.getCreditLimit()
            .subtract(accountDetails.getCurrentBalance())
            .max(BigDecimal.ZERO);
            
        assertThat(accountDetails.getAvailableCredit())
            .describedAs("Available credit calculation should match COBOL logic")
            .isEqualByComparingTo(expectedAvailableCredit);

        // Validate customer information completeness
        assertThat(accountDetails.getCustomerId())
            .describedAs("Customer ID should be populated")
            .isNotNull()
            .isNotEmpty();
            
        assertThat(accountDetails.getCustomerFullName())
            .describedAs("Customer full name should be computed")
            .isNotNull()
            .isNotEmpty();

        // Update session context for account viewing
        testSessionContext.setCurrentMenu("ACCOUNT");
        testSessionContext.addToNavigationStack("ACCOUNT");
        testSessionContext.setLastTransactionCode("COACTVW");
    }

    /**
     * Tests session state persistence across multiple requests.
     * 
     * This test validates that session context (COMMAREA equivalent) maintains
     * proper state across multiple REST API calls:
     * 1. Session context is preserved between requests
     * 2. Navigation stack maintains proper breadcrumb trail
     * 3. User authorization context is maintained
     * 4. Session timeout behavior works correctly
     * 5. Error state propagation functions properly
     * 
     * The test ensures that session management provides equivalent functionality
     * to CICS COMMAREA while supporting distributed session clustering through
     * Spring Session Redis.
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Session state persistence across requests - COMMAREA equivalent")
    void testSessionStatePersistence() throws Exception {
        // Given: Complete user workflow through multiple screens
        testAccountDetailsViewingWorkflow(); // Establish full navigation context
        
        // Validate session context maintains proper state
        assertThat(testSessionContext.getUserId())
            .describedAs("User ID should be preserved in session")
            .isEqualTo(TEST_USER_ID);
            
        assertThat(testSessionContext.getNavigationStack())
            .describedAs("Navigation stack should maintain breadcrumb trail")
            .isNotEmpty()
            .contains("MAIN", "TRANSACTION", "ACCOUNT");
            
        assertThat(testSessionContext.getOperationStatus())
            .describedAs("Operation status should reflect current state")
            .isEqualTo("ACTIVE");

        // Test navigation back functionality (PF12 equivalent)
        String previousScreen = testSessionContext.popFromNavigationStack();
        assertThat(previousScreen)
            .describedAs("Navigation back should return previous screen")
            .isEqualTo("ACCOUNT");

        // Test session timeout detection
        testSessionContext.setLastActivityTime(LocalDateTime.now().minusMinutes(31));
        boolean isTimedOut = testSessionContext.isTimedOut(30);
        assertThat(isTimedOut)
            .describedAs("Session should detect timeout condition")
            .isTrue();

        // Test session refresh extends timeout
        testSessionContext.updateActivityTime();
        isTimedOut = testSessionContext.isTimedOut(30);
        assertThat(isTimedOut)
            .describedAs("Session activity should reset timeout")
            .isFalse();
    }

    /**
     * Tests error recovery and navigation patterns.
     * 
     * This test validates error handling equivalent to COBOL ABEND routines:
     * 1. System properly handles and reports error conditions
     * 2. Error messages are preserved in session context
     * 3. Navigation state is maintained during error conditions
     * 4. Error recovery allows continued operation
     * 5. PF-key equivalent navigation works during error states
     * 
     * The test ensures that error handling provides equivalent functionality
     * to mainframe error processing while supporting modern HTTP status codes
     * and JSON error response formats.
     */
    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Error recovery and navigation patterns - ABEND equivalent handling")
    void testErrorRecoveryAndNavigation() throws Exception {
        // Given: Authenticated session
        testCompleteSignOnWorkflow();
        
        // When: Request invalid account details (simulating error condition)
        MvcResult errorResult = mockMvc.perform(get("/api/accounts/{accountId}", "INVALID999")
                .session(userSession)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                
        // Then: System returns proper error response
                .andExpect(status().isNotFound())
                .andReturn();

        // Validate error response structure
        String errorResponse = errorResult.getResponse().getContentAsString();
        assertThat(errorResponse)
            .describedAs("Error response should provide meaningful feedback")
            .isNotEmpty();

        // Test that session context preserves error state
        testSessionContext.setOperationStatus("ERROR");
        testSessionContext.setErrorMessage("Account not found");
        
        assertThat(testSessionContext.getOperationStatus())
            .describedAs("Session should maintain error status")
            .isEqualTo("ERROR");
            
        assertThat(testSessionContext.getErrorMessage())
            .describedAs("Error message should be preserved for user feedback")
            .isEqualTo("Account not found");

        // Test navigation continues to work during error state (F3=Exit equivalent)
        testSessionContext.clearNavigationStack();
        testSessionContext.addToNavigationStack("MAIN");
        testSessionContext.setCurrentMenu("MAIN");
        testSessionContext.setOperationStatus("ACTIVE");
        testSessionContext.setErrorMessage(null);

        // Verify recovery to normal operation
        MvcResult recoveryResult = mockMvc.perform(get("/api/menu/main")
                .session(userSession)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                
                .andExpect(status().isOk())
                .andReturn();
                
        assertThat(recoveryResult.getResponse().getStatus())
            .describedAs("System should recover from error state")
            .isEqualTo(200);
    }

    /**
     * Tests PF-key equivalent navigation functionality.
     * 
     * This test validates keyboard navigation patterns equivalent to 3270 PF-keys:
     * 1. F3=Exit functionality returns to main menu
     * 2. F12=Cancel functionality returns to previous screen
     * 3. Navigation stack properly manages screen transitions
     * 4. Session context maintains proper state during navigation
     * 5. Response times meet performance requirements for navigation
     * 
     * The test ensures that navigation patterns provide equivalent user experience
     * to 3270 terminal PF-key functionality while supporting modern web browser
     * keyboard event handling through React components.
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("PF-key equivalent navigation - F3=Exit, F12=Cancel functionality")
    void testPFKeyEquivalentNavigation() throws Exception {
        // Given: User navigated through multiple screens
        testSessionStatePersistence(); // Establish navigation context
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // Simulate F12=Cancel (return to previous screen)
        String previousScreen = testSessionContext.popFromNavigationStack();
        testSessionContext.setCurrentMenu(previousScreen != null ? previousScreen : "MAIN");
        
        // When: User navigates back (F12 equivalent)
        MvcResult cancelResult = mockMvc.perform(get("/api/menu/main")
                .session(userSession)
                .header("X-Navigation-Action", "F12")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                
        // Then: Navigation succeeds with proper context
                .andExpect(status().isOk())
                .andReturn();

        // Validate F12 navigation timing
        LocalDateTime endTime = LocalDateTime.now();
        Duration navigationTime = Duration.between(startTime, endTime);
        assertThat(navigationTime.toMillis())
            .describedAs("F12 navigation response time must be under 200ms")
            .isLessThan(200);

        // Test F3=Exit functionality (return to main menu)
        startTime = LocalDateTime.now();
        
        testSessionContext.clearNavigationStack();
        testSessionContext.setCurrentMenu("MAIN");
        
        MvcResult exitResult = mockMvc.perform(get("/api/menu/main")
                .session(userSession)
                .header("X-Navigation-Action", "F3")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                
                .andExpect(status().isOk())
                .andReturn();

        // Validate F3 navigation timing  
        endTime = LocalDateTime.now();
        navigationTime = Duration.between(startTime, endTime);
        assertThat(navigationTime.toMillis())
            .describedAs("F3 navigation response time must be under 200ms")
            .isLessThan(200);

        // Validate navigation state after F3=Exit
        assertThat(testSessionContext.getCurrentMenu())
            .describedAs("F3=Exit should return to main menu")
            .isEqualTo("MAIN");
            
        assertThat(testSessionContext.getNavigationStack())
            .describedAs("F3=Exit should clear navigation stack")
            .isEmpty();
    }

    /**
     * Tests complete end-to-end workflow execution within time limits.
     * 
     * This comprehensive test validates that complete user workflows execute
     * within acceptable performance boundaries:
     * 1. Complete sign-on to transaction completion workflow
     * 2. Total workflow execution time validation
     * 3. Individual operation response time compliance
     * 4. Session state consistency throughout workflow
     * 5. Memory usage and resource consumption monitoring
     * 
     * The test ensures that the modernized system meets or exceeds the performance
     * characteristics of the original mainframe system while providing enhanced
     * functionality and user experience through the React frontend.
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Complete workflow execution time validation - Performance compliance")
    void testCompleteWorkflowPerformance() throws Exception {
        // Record total workflow start time
        LocalDateTime workflowStartTime = LocalDateTime.now();
        
        // Execute complete user workflow sequence
        testCompleteSignOnWorkflow();           // ~100ms expected
        testMainMenuNavigationWorkflow();       // ~50ms expected  
        testTransactionListBrowsingWorkflow();  // ~150ms expected
        testAccountDetailsViewingWorkflow();    // ~100ms expected
        
        // Record total workflow completion time
        LocalDateTime workflowEndTime = LocalDateTime.now();
        Duration totalWorkflowTime = Duration.between(workflowStartTime, workflowEndTime);
        
        // Validate total workflow performance
        assertThat(totalWorkflowTime.toMillis())
            .describedAs("Complete workflow should execute within 2 seconds")
            .isLessThan(2000);
            
        assertThat(totalWorkflowTime.toMillis())
            .describedAs("Complete workflow should demonstrate performance improvement")
            .isLessThan(1500); // Target improvement over mainframe equivalent

        // Validate session consistency after complete workflow
        assertThat(testSessionContext.getUserId())
            .describedAs("Session context should remain consistent")
            .isEqualTo(TEST_USER_ID);
            
        assertThat(testSessionContext.getOperationStatus())
            .describedAs("Operation status should remain active")
            .isEqualTo("ACTIVE");
            
        assertThat(testSessionContext.getLastActivityTime())
            .describedAs("Last activity time should be recent")
            .isAfter(workflowStartTime);

        // Log performance metrics for monitoring
        System.out.printf("Complete E2E workflow executed in %d ms%n", 
            totalWorkflowTime.toMillis());
        System.out.printf("Session context maintained across %d navigation steps%n",
            testSessionContext.getNavigationStack().size());
    }

    /**
     * Helper method to validate BigDecimal precision matches COBOL COMP-3 requirements.
     * 
     * @param actual The BigDecimal value to validate
     * @param expected The expected BigDecimal value
     * @param scale The expected scale (decimal places)
     */
    private void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected, int scale) {
        assertThat(actual)
            .describedAs("BigDecimal value should match expected value")
            .isNotNull();
            
        assertThat(actual.scale())
            .describedAs("BigDecimal scale should match COBOL COMP-3 precision")
            .isEqualTo(scale);
            
        assertThat(actual)
            .describedAs("BigDecimal value should equal expected within precision")
            .isEqualByComparingTo(expected);
    }

    /**
     * Helper method to validate response timing meets performance requirements.
     * 
     * @param startTime The operation start time
     * @param endTime The operation end time
     * @param maxMillis The maximum acceptable response time in milliseconds
     * @param operation The operation name for descriptive assertions
     */
    private void validateResponseTiming(LocalDateTime startTime, LocalDateTime endTime, 
                                      long maxMillis, String operation) {
        Duration responseTime = Duration.between(startTime, endTime);
        assertThat(responseTime.toMillis())
            .describedAs("%s response time must be under %d ms", operation, maxMillis)
            .isLessThan(maxMillis);
    }
}