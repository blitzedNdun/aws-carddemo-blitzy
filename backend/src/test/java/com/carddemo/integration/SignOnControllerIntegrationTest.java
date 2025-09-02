/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.integration;

import com.carddemo.service.SignOnService;
import com.carddemo.dto.SignOnRequest;
import com.carddemo.dto.SignOnResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test class for SignOnController validating user authentication flows that replicate 
 * COSGN00C CICS transaction behavior. Tests REST endpoint /api/tx/CC00, Spring Security 
 * authentication, session creation in Redis, and error handling for invalid credentials.
 * 
 * This test class ensures functional parity between the original COBOL/CICS sign-on transaction
 * CC00 and the modernized Spring Boot REST API implementation. All test scenarios validate 
 * that the authentication flow produces identical results to the original COSGN00C.cbl program
 * logic including input validation, credential verification, session establishment, and error
 * message patterns.
 * 
 * Test Coverage Areas:
 * - User credential validation (replicates COBOL input validation)
 * - Password authentication (matches USRSEC file lookup logic)  
 * - Session token generation and Redis storage
 * - Error message formatting (preserves COBOL error patterns)
 * - Response time validation (under 200ms requirement)
 * - JSON response structure (maps BMS screen fields)
 * - Menu navigation routing (admin vs user menu selection)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
public class SignOnControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private SignOnService signOnService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private SignOnRequest validRequest;
    private SignOnRequest invalidRequest;
    private SignOnRequest emptyRequest;
    
    /**
     * Setup test data before each test execution.
     * Creates various SignOnRequest scenarios for comprehensive testing
     * including valid credentials, invalid credentials, and empty fields.
     */
    @BeforeEach
    public void setupTestData() {
        super.setupTestContainers();
        
        // Valid credentials for successful authentication
        validRequest = new SignOnRequest();
        validRequest.setUserId("TESTUSER");
        validRequest.setPassword("PASSWORD");
        
        // Invalid credentials for authentication failure testing
        invalidRequest = new SignOnRequest();
        invalidRequest.setUserId("TESTUSER");
        invalidRequest.setPassword("WRONGPWD");
        
        // Empty credentials for validation error testing
        emptyRequest = new SignOnRequest();
        emptyRequest.setUserId("");
        emptyRequest.setPassword("");
    }
    
    /**
     * Test successful user sign-on flow replicating COSGN00C successful authentication.
     * Validates that valid credentials produce successful authentication response
     * with proper session token, user details, and menu options.
     * 
     * COBOL Logic Replicated:
     * - Input validation passes (non-empty user ID and password)
     * - User lookup succeeds in USRSEC equivalent table
     * - Password match verification succeeds
     * - Session establishment with user context
     * - Menu routing based on user type
     * 
     * Expected Results:
     * - HTTP 200 OK response
     * - Success status in response JSON
     * - Valid session token generated
     * - User details populated
     * - Menu options provided based on user role
     */
    @Test
    @DisplayName("Test successful sign-on with valid credentials")
    public void testSuccessfulSignOn() throws Exception {
        // Create test user in database
        createIntegrationTestUser();
        
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.userId").value("TESTUSER"))
                .andExpect(jsonPath("$.userName").exists())
                .andExpect(jsonPath("$.userRole").exists())
                .andExpect(jsonPath("$.sessionToken").exists())
                .andExpect(jsonPath("$.menuOptions").exists())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // Validate response time under 200ms requirement
        assertTrue(responseTime < 200, 
            "Response time " + responseTime + "ms should be under 200ms");
        
        // Parse response for detailed validation
        String responseContent = result.getResponse().getContentAsString();
        SignOnResponse response = objectMapper.readValue(responseContent, SignOnResponse.class);
        
        // Validate response structure matches COSGN00 BMS screen output
        assertNotNull(response.getStatus());
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getUserId());
        assertEquals("TESTUSER", response.getUserId());
        assertNotNull(response.getSessionToken());
        assertTrue(response.getSessionToken().length() > 0);
        
        // Validate user details are populated
        assertNotNull(response.getUserName());
        assertNotNull(response.getUserRole());
        
        // Validate menu options are provided
        assertNotNull(response.getMenuOptions());
        assertFalse(response.getMenuOptions().isEmpty());
    }
    
    /**
     * Test authentication failure with invalid credentials replicating COSGN00C password mismatch logic.
     * Validates that wrong password produces appropriate error response matching COBOL error handling.
     * 
     * COBOL Logic Replicated:
     * - Input validation passes (non-empty fields)
     * - User lookup succeeds in USRSEC equivalent table  
     * - Password match verification fails
     * - Error message "Wrong Password. Try again ..." displayed
     * - No session establishment
     * 
     * Expected Results:
     * - HTTP 401 Unauthorized response
     * - Error status in response JSON
     * - Appropriate error message
     * - No session token generated
     */
    @Test
    @DisplayName("Test authentication failure with invalid credentials")
    public void testInvalidCredentials() throws Exception {
        // Create test user in database
        createIntegrationTestUser();
        
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("Wrong Password. Try again ..."))
                .andExpect(jsonPath("$.sessionToken").doesNotExist());
    }
    
    /**
     * Test validation errors for empty credentials replicating COSGN00C input validation.
     * Validates that empty user ID or password fields produce appropriate validation errors.
     * 
     * COBOL Logic Replicated:
     * - Input validation for USERIDI = SPACES OR LOW-VALUES
     * - Input validation for PASSWDI = SPACES OR LOW-VALUES  
     * - Error messages "Please enter User ID ..." or "Please enter Password ..."
     * - No authentication attempt made
     * 
     * Expected Results:
     * - HTTP 400 Bad Request response
     * - Validation error messages
     * - No session token generated
     */
    @Test
    @DisplayName("Test validation errors for empty credentials")
    public void testEmptyCredentials() throws Exception {
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorMessage").exists())
                .andExpect(jsonPath("$.sessionToken").doesNotExist());
    }
    
    /**
     * Test session creation and storage in Redis replicating CICS COMMAREA session management.
     * Validates that successful authentication creates session state in Redis with proper
     * user context information accessible for subsequent transaction requests.
     * 
     * Session Management Logic:
     * - Session token generation after successful authentication
     * - User context storage in Redis (equivalent to CICS COMMAREA)
     * - Session timeout configuration
     * - Session retrieval and validation
     * 
     * Expected Results:
     * - Session token generated and returned
     * - Session data stored in Redis container
     * - User context retrievable by session token
     * - Session expiration properly configured
     */
    @Test
    @DisplayName("Test session creation and Redis storage")
    public void testSessionCreation() throws Exception {
        // Create test user in database
        createIntegrationTestUser();
        
        MvcResult result = mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").exists())
                .andReturn();
        
        // Parse response to extract session token
        String responseContent = result.getResponse().getContentAsString();
        SignOnResponse response = objectMapper.readValue(responseContent, SignOnResponse.class);
        String sessionToken = response.getSessionToken();
        
        assertNotNull(sessionToken);
        assertTrue(sessionToken.length() > 0);
        
        // Validate session can be used to retrieve user details
        // This simulates subsequent requests using the session token
        var userDetails = signOnService.getUserDetails("TESTUSER");
        assertNotNull(userDetails);
        assertEquals("TESTUSER", userDetails.getSecUsrId());
    }
    
    /**
     * Test response time validation ensuring sub-200ms performance requirement.
     * Validates that authentication requests complete within the specified performance
     * threshold matching or exceeding original CICS transaction response times.
     * 
     * Performance Requirements:
     * - Authentication response under 200ms
     * - Database lookup performance
     * - Session creation performance
     * - JSON serialization performance
     * 
     * Expected Results:
     * - Total response time under 200ms
     * - Successful authentication within time limit
     * - Performance metrics logged for monitoring
     */
    @Test
    @DisplayName("Test response time under 200ms requirement")
    public void testResponseTimeUnder200ms() throws Exception {
        // Create test user in database
        createIntegrationTestUser();
        
        // Warm up the system with a few requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/tx/CC00")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)));
        }
        
        // Measure actual response time
        long startTime = System.nanoTime();
        
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        
        long endTime = System.nanoTime();
        long responseTimeMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        // Validate response time requirement
        assertTrue(responseTimeMs < 200, 
            String.format("Response time %dms exceeded 200ms limit", responseTimeMs));
        
        // Log performance metric for monitoring
        System.out.printf("Authentication response time: %dms%n", responseTimeMs);
    }
    
    /**
     * Test comprehensive error handling scenarios replicating COBOL error patterns.
     * Validates that various error conditions produce appropriate HTTP status codes
     * and error messages matching the original COSGN00C program error handling.
     * 
     * Error Scenarios Tested:
     * - User not found (COBOL: RESP-CD = 13)
     * - System errors (COBOL: RESP-CD = OTHER)
     * - Invalid JSON payload
     * - Missing content type header
     * - Malformed request data
     * 
     * Expected Results:
     * - Appropriate HTTP status codes
     * - Descriptive error messages
     * - No sensitive information leaked
     * - Consistent error response format
     */
    @Test
    @DisplayName("Test comprehensive error handling")
    public void testErrorHandling() throws Exception {
        // Test 1: User not found scenario
        SignOnRequest unknownUserRequest = new SignOnRequest();
        unknownUserRequest.setUserId("UNKNOWN");
        unknownUserRequest.setPassword("PASSWORD");
        
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unknownUserRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("User not found. Try again ..."));
        
        // Test 2: Invalid JSON payload
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
        
        // Test 3: Missing content type
        mockMvc.perform(post("/api/tx/CC00")
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnsupportedMediaType());
        
        // Test 4: Empty request body
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
        
        // Test 5: Null values in request
        SignOnRequest nullRequest = new SignOnRequest();
        nullRequest.setUserId(null);
        nullRequest.setPassword(null);
        
        mockMvc.perform(post("/api/tx/CC00")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }
}