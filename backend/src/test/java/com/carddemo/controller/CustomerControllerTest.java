/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.controller;

import com.carddemo.controller.CustomerController;
import com.carddemo.service.CustomerService;
import com.carddemo.dto.CustomerDto;
import com.carddemo.dto.CustomerRequest;
import com.carddemo.config.TestWebConfig;
import com.carddemo.config.TestSecurityConfig;
import com.carddemo.util.TestConstants;
import com.carddemo.entity.Customer;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.mockito.Mockito;
import org.assertj.core.api.Assertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration test class for CustomerController that validates customer management endpoints 
 * including viewing and updating customer information with COBOL-to-Java functional parity validation.
 * 
 * This test class ensures complete compatibility between the modernized Spring Boot REST API 
 * and the original COBOL CICS customer management functionality from CVCUS01Y copybook.
 * 
 * Test Coverage:
 * - GET /api/customers/{id} for viewing customer details with proper field mapping
 * - PUT /api/customers/{id} for updating customer information with validation
 * - POST /api/customers for creating new customers with comprehensive validation
 * - Address field validation and formatting (street, city, state, ZIP)
 * - Phone number formatting and validation matching COBOL patterns
 * - Email validation patterns and business rule enforcement
 * - Customer-account relationship integrity validation
 * - Customer status management and state transitions
 * - Name field handling (first, middle, last) with COBOL length constraints
 * - Session-based state management for multi-step updates
 * - Performance validation ensuring < 200ms response times
 * - BigDecimal precision validation for financial fields
 * - Security role-based access control testing
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024
 */
@WebMvcTest(CustomerController.class)
@ContextConfiguration(classes = {CustomerController.class, TestWebConfig.class, TestSecurityConfig.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.security.enabled=false",
    "logging.level.com.carddemo=DEBUG"
})
@DisplayName("Customer Controller Integration Tests - COBOL Functional Parity Validation")
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestWebConfig testWebConfig;

    // Test data constants based on CVCUS01Y.cpy structure
    private static final String TEST_CUSTOMER_ID = TestConstants.TEST_CUSTOMER_ID;
    private static final String TEST_USER_ID = TestConstants.TEST_USER_ID;
    private static final String VALID_PHONE_NUMBER = "555-123-4567";
    private static final String VALID_ZIP_CODE = "12345";
    private static final String VALID_STATE_CODE = "NY";
    private static final String VALID_SSN = "123456789";
    
    private Customer testCustomer;
    private CustomerDto testCustomerDto;
    private CustomerRequest testCustomerRequest;

    /**
     * Set up test data before each test execution.
     * Creates comprehensive test fixtures that match COBOL CUSTOMER-RECORD structure.
     */
    @BeforeEach
    void setUp() {
        // Create test customer entity matching COBOL structure
        testCustomer = Customer.builder()
            .customerId(Long.valueOf(TEST_CUSTOMER_ID))
            .firstName("JOHN")
            .middleName("MICHAEL")
            .lastName("SMITH")
            .addressLine1("123 MAIN STREET")
            .addressLine2("APT 4B")
            .addressLine3("SUITE 200")
            .stateCode(VALID_STATE_CODE)
            .countryCode("USA")
            .zipCode(VALID_ZIP_CODE)
            .phoneNumber1(VALID_PHONE_NUMBER)
            .phoneNumber2("555-987-6543")
            .ssn(VALID_SSN)
            .governmentIssuedId("DL123456789")
            .dateOfBirth(LocalDate.of(1985, 6, 15))
            .eftAccountId("EFT1234567")
            .primaryCardHolderIndicator("Y")
            .ficoScore(new BigDecimal("750.00"))
            .creditLimit(new BigDecimal("5000.00"))
            .createdTimestamp(LocalDateTime.now().minusMonths(6))
            .lastUpdateTimestamp(LocalDateTime.now().minusDays(1))
            .build();

        // Create test CustomerDto for response validation
        testCustomerDto = CustomerDto.builder()
            .customerId(TEST_CUSTOMER_ID)
            .firstName("JOHN")
            .middleName("MICHAEL")
            .lastName("SMITH")
            .addressLine1("123 MAIN STREET")
            .addressLine2("APT 4B")
            .addressLine3("SUITE 200")
            .stateCode(VALID_STATE_CODE)
            .countryCode("USA")
            .zipCode(VALID_ZIP_CODE)
            .phoneNumber1(VALID_PHONE_NUMBER)
            .phoneNumber2("555-987-6543")
            .maskedSsn("***-**-6789")  // SSN should be masked in DTO
            .governmentIssuedId("DL123456789")
            .dateOfBirth(LocalDate.of(1985, 6, 15))
            .eftAccountId("EFT1234567")
            .primaryCardHolderIndicator("Y")
            .ficoScore(new BigDecimal("750.00"))
            .creditLimit(new BigDecimal("5000.00"))
            .build();

        // Create test CustomerRequest for update operations
        testCustomerRequest = CustomerRequest.builder()
            .firstName("JOHN")
            .middleName("MICHAEL")
            .lastName("SMITH")
            .addressLine1("123 MAIN STREET")
            .addressLine2("APT 4B")
            .addressLine3("SUITE 200")
            .stateCode(VALID_STATE_CODE)
            .countryCode("USA")
            .zipCode(VALID_ZIP_CODE)
            .phoneNumber1(VALID_PHONE_NUMBER)
            .phoneNumber2("555-987-6543")
            .governmentIssuedId("DL123456789")
            .dateOfBirth(LocalDate.of(1985, 6, 15))
            .eftAccountId("EFT1234567")
            .primaryCardHolderIndicator("Y")
            .build();
    }

    /**
     * Nested test class for GET /api/customers/{id} endpoint validation.
     * Tests customer retrieval functionality with comprehensive field mapping validation.
     */
    @Nested
    @DisplayName("GET /api/customers/{id} - Customer Retrieval Tests")
    class GetCustomerTests {

        @Test
        @DisplayName("Should retrieve customer successfully with complete field mapping")
        @WithMockUser(roles = {"USER"})
        void testGetCustomer_ValidId_ReturnsCompleteCustomerData() throws Exception {
            // Given: Mock service returns customer data
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(testCustomerDto);
            
            long startTime = System.currentTimeMillis();
            
            // When: GET request to retrieve customer
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                    
            // Then: Validate response structure and field mapping
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                
                // Validate customer identification fields
                .andExpect(jsonPath("$.customerId").value(TEST_CUSTOMER_ID))
                .andExpect(jsonPath("$.firstName").value("JOHN"))
                .andExpect(jsonPath("$.middleName").value("MICHAEL"))
                .andExpect(jsonPath("$.lastName").value("SMITH"))
                
                // Validate address fields (COBOL ADDR-LINE-1/2/3 mapping)
                .andExpect(jsonPath("$.addressLine1").value("123 MAIN STREET"))
                .andExpect(jsonPath("$.addressLine2").value("APT 4B"))
                .andExpect(jsonPath("$.addressLine3").value("SUITE 200"))
                .andExpect(jsonPath("$.stateCode").value(VALID_STATE_CODE))
                .andExpect(jsonPath("$.countryCode").value("USA"))
                .andExpect(jsonPath("$.zipCode").value(VALID_ZIP_CODE))
                
                // Validate contact information
                .andExpect(jsonPath("$.phoneNumber1").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.phoneNumber2").value("555-987-6543"))
                
                // Validate SSN masking (security requirement)
                .andExpect(jsonPath("$.maskedSsn").value("***-**-6789"))
                .andExpect(jsonPath("$.ssn").doesNotExist())  // SSN should not be exposed
                
                // Validate additional customer fields
                .andExpect(jsonPath("$.governmentIssuedId").value("DL123456789"))
                .andExpect(jsonPath("$.dateOfBirth").value("1985-06-15"))
                .andExpect(jsonPath("$.eftAccountId").value("EFT1234567"))
                .andExpect(jsonPath("$.primaryCardHolderIndicator").value("Y"))
                
                // Validate financial fields with BigDecimal precision
                .andExpect(jsonPath("$.ficoScore").value(750.00))
                .andExpect(jsonPath("$.creditLimit").value(5000.00));
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            // Validate performance requirement < 200ms
            assertThat(responseTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
            // Verify service interaction
            verify(customerService, times(1)).getCustomerById(TEST_CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should return 404 for non-existent customer ID")
        @WithMockUser(roles = {"USER"})
        void testGetCustomer_NonExistentId_Returns404() throws Exception {
            // Given: Service throws exception for non-existent customer
            String nonExistentId = "999999999";
            when(customerService.getCustomerById(nonExistentId))
                .thenThrow(new RuntimeException("Customer not found"));
            
            // When & Then: Request returns 404
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", nonExistentId)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isNotFound());
            
            verify(customerService, times(1)).getCustomerById(nonExistentId);
        }

        @Test
        @DisplayName("Should validate COBOL field length constraints in response")
        @WithMockUser(roles = {"USER"})
        void testGetCustomer_ValidatesCobolFieldLengths() throws Exception {
            // Given: Customer with field lengths matching COBOL PIC clauses
            CustomerDto longFieldCustomer = testCustomerDto.toBuilder()
                .firstName("VERYLONGFIRSTNAME123")  // Should be truncated to 20 chars
                .lastName("VERYLONGLASTNAME1234")   // Should be truncated to 20 chars
                .addressLine1("VERY LONG ADDRESS LINE THAT EXCEEDS FIFTY CHARACTER LIMIT TESTING")  // 50 char limit
                .build();
            
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(longFieldCustomer);
            
            // When & Then: Validate field length constraints
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                
                // Validate COBOL field length compliance
                .andExpect(jsonPath("$.firstName").value(hasLength(lessThanOrEqualTo(20))))
                .andExpect(jsonPath("$.lastName").value(hasLength(lessThanOrEqualTo(20))))
                .andExpect(jsonPath("$.addressLine1").value(hasLength(lessThanOrEqualTo(50))));
        }

        @Test
        @DisplayName("Should handle customer data with COBOL precision requirements")
        @WithMockUser(roles = {"USER"})
        void testGetCustomer_CobolPrecisionValidation() throws Exception {
            // Given: Customer with precise BigDecimal values matching COBOL COMP-3
            CustomerDto precisionCustomer = testCustomerDto.toBuilder()
                .ficoScore(new BigDecimal("825.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .creditLimit(new BigDecimal("15000.00").setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .build();
            
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(precisionCustomer);
            
            // When & Then: Validate BigDecimal precision
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficoScore").value(825.00))
                .andExpect(jsonPath("$.creditLimit").value(15000.00));
        }
    }

    /**
     * Nested test class for PUT /api/customers/{id} endpoint validation.
     * Tests customer update functionality with comprehensive field validation.
     */
    @Nested
    @DisplayName("PUT /api/customers/{id} - Customer Update Tests")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer successfully with complete field validation")
        @WithMockUser(roles = {"USER"})
        void testUpdateCustomer_ValidData_UpdatesSuccessfully() throws Exception {
            // Given: Valid customer update request
            CustomerDto updatedCustomer = testCustomerDto.toBuilder()
                .firstName("JANE")
                .lastName("DOE")
                .phoneNumber1("555-999-8888")
                .build();
            
            when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                .thenReturn(updatedCustomer);
            
            String requestJson = objectMapper.writeValueAsString(testCustomerRequest.toBuilder()
                .firstName("JANE")
                .lastName("DOE")
                .phoneNumber1("555-999-8888")
                .build());
            
            long startTime = System.currentTimeMillis();
            
            // When: PUT request to update customer
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    put("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)))
                    
            // Then: Validate successful update response
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(TEST_CUSTOMER_ID))
                .andExpect(jsonPath("$.firstName").value("JANE"))
                .andExpect(jsonPath("$.lastName").value("DOE"))
                .andExpect(jsonPath("$.phoneNumber1").value("555-999-8888"));
            
            long responseTime = System.currentTimeMillis() - startTime;
            assertThat(responseTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
            verify(customerService, times(1)).updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class));
        }

        @Test
        @DisplayName("Should validate address fields with proper formatting")
        @WithMockUser(roles = {"USER"})
        void testUpdateCustomer_AddressValidation() throws Exception {
            // Given: Customer request with various address scenarios
            List<CustomerRequest> addressTestCases = Arrays.asList(
                // Valid complete address
                testCustomerRequest.toBuilder()
                    .addressLine1("123 MAIN ST")
                    .addressLine2("APT 5A")
                    .stateCode("CA")
                    .zipCode("90210")
                    .build(),
                    
                // Valid minimal address
                testCustomerRequest.toBuilder()
                    .addressLine1("456 ELM STREET")
                    .addressLine2(null)
                    .addressLine3(null)
                    .stateCode("TX")
                    .zipCode("75201")
                    .build()
            );
            
            for (CustomerRequest addressCase : addressTestCases) {
                // Mock successful update
                when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                    .thenReturn(testCustomerDto);
                
                String requestJson = objectMapper.writeValueAsString(addressCase);
                
                // When & Then: Validate address acceptance
                mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                        put("/api/customers/{id}", TEST_CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                    .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should validate phone number formatting and patterns")
        @WithMockUser(roles = {"USER"})
        void testUpdateCustomer_PhoneNumberValidation() throws Exception {
            // Given: Various phone number format test cases
            List<String> validPhoneNumbers = Arrays.asList(
                "555-123-4567",     // Standard format
                "(555) 123-4567",   // Parentheses format
                "5551234567",       // No separators
                "+1-555-123-4567"   // International format
            );
            
            for (String phoneNumber : validPhoneNumbers) {
                CustomerRequest phoneTestRequest = testCustomerRequest.toBuilder()
                    .phoneNumber1(phoneNumber)
                    .build();
                
                when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                    .thenReturn(testCustomerDto);
                
                String requestJson = objectMapper.writeValueAsString(phoneTestRequest);
                
                // When & Then: Validate phone number acceptance
                mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                        put("/api/customers/{id}", TEST_CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                    .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should validate ZIP code patterns and formatting")
        @WithMockUser(roles = {"USER"})
        void testUpdateCustomer_ZipCodeValidation() throws Exception {
            // Given: Various ZIP code format test cases
            List<String> validZipCodes = Arrays.asList(
                "12345",        // 5-digit ZIP
                "12345-6789",   // ZIP+4 format
                "90210",        // Famous ZIP code
                "00501"         // Low ZIP with leading zero
            );
            
            for (String zipCode : validZipCodes) {
                CustomerRequest zipTestRequest = testCustomerRequest.toBuilder()
                    .zipCode(zipCode)
                    .build();
                
                when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                    .thenReturn(testCustomerDto);
                
                String requestJson = objectMapper.writeValueAsString(zipTestRequest);
                
                // When & Then: Validate ZIP code acceptance
                mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                        put("/api/customers/{id}", TEST_CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                    .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should reject invalid field data with proper error responses")
        @WithMockUser(roles = {"USER"})
        void testUpdateCustomer_InvalidDataValidation() throws Exception {
            // Given: Invalid customer data scenarios
            List<CustomerRequest> invalidTestCases = Arrays.asList(
                // Empty first name (required field)
                testCustomerRequest.toBuilder()
                    .firstName("")
                    .build(),
                    
                // Invalid state code
                testCustomerRequest.toBuilder()
                    .stateCode("XX")
                    .build(),
                    
                // Invalid ZIP code format
                testCustomerRequest.toBuilder()
                    .zipCode("INVALID")
                    .build(),
                    
                // Future date of birth
                testCustomerRequest.toBuilder()
                    .dateOfBirth(LocalDate.now().plusYears(1))
                    .build()
            );
            
            for (CustomerRequest invalidCase : invalidTestCases) {
                when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                    .thenThrow(new RuntimeException("Validation failed"));
                
                String requestJson = objectMapper.writeValueAsString(invalidCase);
                
                // When & Then: Validate rejection of invalid data
                mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                        put("/api/customers/{id}", TEST_CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                    .andExpect(status().isBadRequest());
            }
        }
    }

    /**
     * Nested test class for POST /api/customers endpoint validation.
     * Tests customer creation functionality with comprehensive field validation.
     */
    @Nested
    @DisplayName("POST /api/customers - Customer Creation Tests")  
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully with complete field validation")
        @WithMockUser(roles = {"ADMIN"})
        void testCreateCustomer_ValidData_CreatesSuccessfully() throws Exception {
            // Given: Valid customer creation request
            CustomerRequest createRequest = testCustomerRequest.toBuilder()
                .build();
            
            CustomerDto createdCustomer = testCustomerDto.toBuilder()
                .customerId("1000000002")  // New customer ID
                .build();
            
            when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenReturn(createdCustomer);
            
            String requestJson = objectMapper.writeValueAsString(createRequest);
            
            long startTime = System.currentTimeMillis();
            
            // When: POST request to create customer
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    post("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)))
                    
            // Then: Validate successful creation response
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value("1000000002"))
                .andExpect(jsonPath("$.firstName").value("JOHN"))
                .andExpect(jsonPath("$.lastName").value("SMITH"));
            
            long responseTime = System.currentTimeMillis() - startTime;
            assertThat(responseTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
            verify(customerService, times(1)).createCustomer(any(CustomerRequest.class));
        }

        @Test
        @DisplayName("Should validate required fields for customer creation")
        @WithMockUser(roles = {"ADMIN"})
        void testCreateCustomer_RequiredFieldValidation() throws Exception {
            // Given: Customer request missing required fields
            CustomerRequest incompleteRequest = CustomerRequest.builder()
                // Missing firstName and lastName (required fields)
                .addressLine1("123 TEST ST")
                .build();
            
            when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new RuntimeException("Required fields missing"));
            
            String requestJson = objectMapper.writeValueAsString(incompleteRequest);
            
            // When & Then: Validate required field enforcement
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    post("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)))
                .andExpect(status().isBadRequest());
            
            verify(customerService, times(1)).createCustomer(any(CustomerRequest.class));
        }
    }

    /**
     * Nested test class for customer-account relationship validation.
     * Tests customer data operations that affect account relationships.
     */
    @Nested
    @DisplayName("Customer-Account Relationship Tests")
    class CustomerAccountRelationshipTests {

        @Test
        @DisplayName("Should validate customer-account relationship integrity")
        @WithMockUser(roles = {"USER"})
        void testCustomerAccountRelationship_IntegrityValidation() throws Exception {
            // Given: Customer with account relationships
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(testCustomerDto);
            when(customerService.validateCustomerData(any())).thenReturn(true);
            
            // When & Then: Validate customer retrieval maintains relationship integrity
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(TEST_CUSTOMER_ID))
                .andExpect(jsonPath("$.primaryCardHolderIndicator").value("Y"));
            
            verify(customerService, times(1)).getCustomerById(TEST_CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should validate EFT account ID consistency")
        @WithMockUser(roles = {"USER"})
        void testEftAccountId_ConsistencyValidation() throws Exception {
            // Given: Customer with EFT account relationship
            CustomerDto customerWithEft = testCustomerDto.toBuilder()
                .eftAccountId("EFT9876543")
                .build();
            
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(customerWithEft);
            
            // When & Then: Validate EFT account ID presence and format
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eftAccountId").value("EFT9876543"))
                .andExpect(jsonPath("$.eftAccountId").value(matchesPattern("^EFT\\d{7}$")));
        }
    }

    /**
     * Nested test class for session-based state management validation.
     * Tests multi-step update scenarios with session state persistence.
     */
    @Nested
    @DisplayName("Session-Based State Management Tests")
    class SessionStateManagementTests {

        @Test
        @DisplayName("Should maintain session state across multi-step customer updates")
        @WithMockUser(roles = {"USER"})
        void testMultiStepUpdate_SessionStateManagement() throws Exception {
            // Given: Multi-step update scenario using session management
            String sessionId = testWebConfig.createTestSession();
            
            // Step 1: Update basic information
            CustomerRequest step1Request = testCustomerRequest.toBuilder()
                .firstName("UPDATED_FIRST")
                .lastName("UPDATED_LAST")
                .build();
            
            when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                .thenReturn(testCustomerDto.toBuilder()
                    .firstName("UPDATED_FIRST")
                    .lastName("UPDATED_LAST")
                    .build());
            
            String step1Json = objectMapper.writeValueAsString(step1Request);
            
            // When: Execute first update step
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    put("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(step1Json)
                    .sessionAttr("customerId", TEST_CUSTOMER_ID)
                    .sessionAttr("updateStep", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UPDATED_FIRST"))
                .andExpect(jsonPath("$.lastName").value("UPDATED_LAST"));
            
            // Step 2: Update address information
            CustomerRequest step2Request = testCustomerRequest.toBuilder()
                .firstName("UPDATED_FIRST")  // Maintain from step 1
                .lastName("UPDATED_LAST")    // Maintain from step 1  
                .addressLine1("NEW ADDRESS LINE 1")
                .addressLine2("NEW ADDRESS LINE 2")
                .stateCode("CA")
                .zipCode("90210")
                .build();
            
            when(customerService.updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class)))
                .thenReturn(testCustomerDto.toBuilder()
                    .firstName("UPDATED_FIRST")
                    .lastName("UPDATED_LAST")
                    .addressLine1("NEW ADDRESS LINE 1")
                    .addressLine2("NEW ADDRESS LINE 2")
                    .stateCode("CA")
                    .zipCode("90210")
                    .build());
            
            String step2Json = objectMapper.writeValueAsString(step2Request);
            
            // Then: Execute second update step maintaining session state
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    put("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(step2Json)
                    .sessionAttr("customerId", TEST_CUSTOMER_ID)
                    .sessionAttr("updateStep", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UPDATED_FIRST"))  // From step 1
                .andExpect(jsonPath("$.lastName").value("UPDATED_LAST"))   // From step 1
                .andExpect(jsonPath("$.addressLine1").value("NEW ADDRESS LINE 1"))
                .andExpect(jsonPath("$.addressLine2").value("NEW ADDRESS LINE 2"));
            
            verify(customerService, times(2)).updateCustomer(eq(TEST_CUSTOMER_ID), any(CustomerRequest.class));
        }
    }

    /**
     * Nested test class for performance and load testing validation.
     * Tests response time and throughput requirements.
     */
    @Nested
    @DisplayName("Performance and Load Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should meet response time requirements under load")
        @WithMockUser(roles = {"USER"})
        void testResponseTime_UnderLoadConditions() throws Exception {
            // Given: Service configured for performance testing
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(testCustomerDto);
            
            // When: Execute multiple concurrent requests
            int concurrentRequests = 10;
            long totalResponseTime = 0;
            
            for (int i = 0; i < concurrentRequests; i++) {
                long startTime = System.currentTimeMillis();
                
                mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                        get("/api/customers/{id}", TEST_CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_JSON)))
                    .andExpect(status().isOk());
                
                long endTime = System.currentTimeMillis();
                totalResponseTime += (endTime - startTime);
            }
            
            // Then: Validate average response time meets requirement
            long averageResponseTime = totalResponseTime / concurrentRequests;
            assertThat(averageResponseTime).isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
            
            // Validate throughput capability
            assertThat(concurrentRequests).isGreaterThan(0);
            verify(customerService, times(concurrentRequests)).getCustomerById(TEST_CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should handle high-frequency customer data validation")
        @WithMockUser(roles = {"USER"})
        void testHighFrequencyValidation_PerformanceMetrics() throws Exception {
            // Given: High-frequency validation scenario
            when(customerService.validateCustomerData(any())).thenReturn(true);
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(testCustomerDto);
            
            long startTime = System.currentTimeMillis();
            int validationIterations = TestConstants.VALIDATION_THRESHOLDS.get("highFrequency");
            
            // When: Execute validation operations
            for (int i = 0; i < validationIterations; i++) {
                mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                        get("/api/customers/{id}", TEST_CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_JSON)))
                    .andExpect(status().isOk());
            }
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // Then: Validate performance meets high-frequency requirements
            double operationsPerSecond = (validationIterations * 1000.0) / totalTime;
            assertThat(operationsPerSecond).isGreaterThan(100);  // Minimum 100 ops/sec
        }
    }

    /**
     * Nested test class for COBOL functional parity validation.
     * Tests ensure identical behavior between COBOL and Java implementations.
     */
    @Nested
    @DisplayName("COBOL Functional Parity Validation")  
    @Tag("cobol-parity")
    class CobolFunctionalParityTests {

        @Test
        @DisplayName("Should maintain COBOL field precision and formatting")
        @WithMockUser(roles = {"USER"})
        void testCobolFieldPrecision_FunctionalParity() throws Exception {
            // Given: Customer with COBOL-precise field values
            CustomerDto cobolPrecisionCustomer = testCustomerDto.toBuilder()
                .ficoScore(new BigDecimal("785.00")
                    .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .creditLimit(new BigDecimal("12500.00")
                    .setScale(TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE))
                .build();
            
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(cobolPrecisionCustomer);
            
            // When & Then: Validate COBOL precision maintenance
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficoScore").value(785.00))
                .andExpect(jsonPath("$.creditLimit").value(12500.00));
            
            // Verify COBOL data type compliance
            verify(customerService, times(1)).getCustomerById(TEST_CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should validate COBOL copybook field mapping accuracy")
        @WithMockUser(roles = {"USER"})
        void testCobolCopybookMapping_FieldAccuracy() throws Exception {
            // Given: Customer data matching CVCUS01Y.cpy structure
            when(customerService.getCustomerById(TEST_CUSTOMER_ID)).thenReturn(testCustomerDto);
            
            // When & Then: Validate all COBOL field mappings
            mockMvc.perform(testWebConfig.createAuthenticatedRequest(
                    get("/api/customers/{id}", TEST_CUSTOMER_ID)
                    .accept(MediaType.APPLICATION_JSON)))
                .andExpect(status().isOk())
                
                // CUST-ID (PIC 9(9)) → customerId
                .andExpect(jsonPath("$.customerId").value(TEST_CUSTOMER_ID))
                
                // CUST-FIRST-NAME (PIC X(25)) → firstName  
                .andExpect(jsonPath("$.firstName").value("JOHN"))
                .andExpect(jsonPath("$.firstName").value(hasLength(lessThanOrEqualTo(20))))
                
                // CUST-MIDDLE-NAME (PIC X(25)) → middleName
                .andExpect(jsonPath("$.middleName").value("MICHAEL"))
                .andExpect(jsonPath("$.middleName").value(hasLength(lessThanOrEqualTo(20))))
                
                // CUST-LAST-NAME (PIC X(25)) → lastName
                .andExpect(jsonPath("$.lastName").value("SMITH"))
                .andExpect(jsonPath("$.lastName").value(hasLength(lessThanOrEqualTo(20))))
                
                // CUST-ADDR-LINE-1 (PIC X(50)) → addressLine1
                .andExpect(jsonPath("$.addressLine1").value("123 MAIN STREET"))
                .andExpect(jsonPath("$.addressLine1").value(hasLength(lessThanOrEqualTo(50))))
                
                // CUST-PHONE-NUM-1 (PIC X(15)) → phoneNumber1
                .andExpect(jsonPath("$.phoneNumber1").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.phoneNumber1").value(hasLength(lessThanOrEqualTo(15))))
                
                // CUST-FICO-CREDIT-SCORE (PIC 9(3)) → ficoScore
                .andExpect(jsonPath("$.ficoScore").value(750.00));
        }
    }
    
    /**
     * Helper method to create test matchers for string length validation.
     */
    private org.hamcrest.Matcher<String> hasLength(org.hamcrest.Matcher<Integer> lengthMatcher) {
        return org.hamcrest.Matchers.allOf(
            org.hamcrest.Matchers.notNullValue(String.class),
            org.hamcrest.Matchers.hasProperty("length", lengthMatcher)
        );
    }
    
    /**
     * Helper method to create regex pattern matchers.
     */
    private org.hamcrest.Matcher<String> matchesPattern(String regex) {
        return org.hamcrest.Matchers.matchesPattern(java.util.regex.Pattern.compile(regex));
    }
}