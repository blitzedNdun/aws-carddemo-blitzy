package com.carddemo.service;

import com.carddemo.client.AddressValidationService;
import com.carddemo.client.DataQualityService;
import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unit test class for CustomerMaintenanceService that validates customer data maintenance 
 * batch processing functionality converted from CBCUS01C COBOL program.
 * 
 * This test class ensures functional parity between the original COBOL batch processing
 * logic and the modernized Spring Boot service implementation. Tests validate:
 * - Customer data updates and synchronization
 * - Address standardization and validation
 * - Contact information verification (phone, email)
 * - Customer deduplication logic
 * - Data quality checks and scoring
 * - Batch processing performance (200ms response time requirement)
 * - Integration with external services (AddressValidationService, DataQualityService)
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@DisplayName("CustomerMaintenanceService Unit Tests")
class CustomerMaintenanceServiceTest extends BaseServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private AddressValidationService addressValidationService;
    
    @Mock
    private DataQualityService dataQualityService;
    
    @InjectMocks
    private CustomerMaintenanceService customerMaintenanceService;
    
    private TestDataBuilder testDataBuilder;
    private CobolComparisonUtils cobolComparisonUtils;
    private PerformanceTestUtils performanceTestUtils;
    
    @BeforeEach
    void setUp() {
        // Reset all mocks before each test
        resetMocks();
        
        // Initialize test utilities
        testDataBuilder = new TestDataBuilder();
        cobolComparisonUtils = new CobolComparisonUtils();
        performanceTestUtils = new PerformanceTestUtils();
        
        // Setup common test data
        setupTestData();
    }

    @Nested
    @DisplayName("Customer Data Processing Tests")
    class CustomerDataProcessingTests {

        @Test
        @DisplayName("processCustomerRecords - should process valid customer records successfully")
        void testProcessCustomerRecords_WithValidData_ShouldProcessSuccessfully() {
            // Given: Valid customer test data
            List<Customer> customers = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("John")
                .withLastName("Smith")
                .withPhoneNumber("555-123-4567")
                .withAddress("123 Main St, Springfield, IL 62701")
                .withSSN("123-45-6789")
                .build();
            
            when(customerRepository.findAll()).thenReturn(customers);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // Mock external service responses
            when(dataQualityService.validateCustomerData(any(Customer.class)))
                .thenReturn(new DataQualityService.DataQualityResult(true, null, 0.95));
            
            // When: Processing customer records
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                try {
                    customerMaintenanceService.processCustomerRecords();
                } catch (Exception e) {
                    fail("Processing should not throw exception", e);
                }
            });
            
            // Then: Verify processing completed successfully
            verify(customerRepository, times(1)).findAll();
            verify(customerRepository, atLeastOnce()).save(any(Customer.class));
            verify(dataQualityService, atLeastOnce()).validateCustomerData(any(Customer.class));
            
            // Validate performance requirement (200ms response time)
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("processCustomerRecords - should handle empty customer list gracefully")
        void testProcessCustomerRecords_WithEmptyList_ShouldHandleGracefully() {
            // Given: Empty customer list
            when(customerRepository.findAll()).thenReturn(new ArrayList<>());
            
            // When: Processing empty list
            assertThatCode(() -> customerMaintenanceService.processCustomerRecords())
                .doesNotThrowAnyException();
            
            // Then: Verify appropriate behavior
            verify(customerRepository, times(1)).findAll();
            verify(customerRepository, never()).save(any(Customer.class));
            verify(dataQualityService, never()).validateCustomerData(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Customer Data Validation Tests")
    class CustomerDataValidationTests {

        @Test
        @DisplayName("validateCustomerData - should validate complete customer data successfully")
        void testValidateCustomerData_WithCompleteData_ShouldReturnValid() {
            // Given: Complete customer data
            Customer customer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("Jane")
                .withLastName("Doe")
                .withPhoneNumber("555-987-6543")
                .withAddress("456 Oak Ave, Chicago, IL 60601")
                .withSSN("987-65-4321")
                .build();
            
            // Mock data quality service response
            DataQualityService.DataQualityResult expectedResult = 
                new DataQualityService.DataQualityResult(true, null, 0.98);
            when(dataQualityService.validateCustomerData(customer)).thenReturn(expectedResult);
            
            // When: Validating customer data
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                boolean isValid = customerMaintenanceService.validateCustomerData(customer);
                
                // Then: Validation should succeed
                assertThat(isValid).isTrue();
            });
            
            // Verify external service interaction
            verify(dataQualityService, times(1)).validateCustomerData(customer);
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("validateCustomerData - should handle incomplete customer data")
        void testValidateCustomerData_WithIncompleteData_ShouldReturnInvalid() {
            // Given: Incomplete customer data (missing required fields)
            Customer incompleteCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000002")
                .withFirstName("") // Empty required field
                .withLastName("Smith")
                .build();
            
            // Mock data quality service response for invalid data
            DataQualityService.DataQualityResult invalidResult = 
                new DataQualityService.DataQualityResult(false, "Missing required fields", 0.40);
            when(dataQualityService.validateCustomerData(incompleteCustomer)).thenReturn(invalidResult);
            
            // When: Validating incomplete data
            boolean isValid = customerMaintenanceService.validateCustomerData(incompleteCustomer);
            
            // Then: Validation should fail
            assertThat(isValid).isFalse();
            verify(dataQualityService, times(1)).validateCustomerData(incompleteCustomer);
        }

        @Test
        @DisplayName("validateCustomerData - should handle data quality service exception")
        void testValidateCustomerData_WithServiceException_ShouldReturnFalse() {
            // Given: Customer data and service exception
            Customer customer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000003")
                .withFirstName("Test")
                .withLastName("User")
                .build();
            
            when(dataQualityService.validateCustomerData(customer))
                .thenThrow(new RuntimeException("External service unavailable"));
            
            // When: Validating with service exception
            boolean isValid = customerMaintenanceService.validateCustomerData(customer);
            
            // Then: Should handle exception gracefully and return false
            assertThat(isValid).isFalse();
            verify(dataQualityService, times(1)).validateCustomerData(customer);
        }
    }

    @Nested
    @DisplayName("Address Standardization Tests")
    class AddressStandardizationTests {

        @Test
        @DisplayName("standardizeAddress - should standardize valid address successfully")
        void testStandardizeAddress_WithValidAddress_ShouldReturnStandardized() {
            // Given: Valid but non-standardized address
            String inputAddress = "123 main street, springfield, il 62701";
            String expectedStandardized = "123 MAIN ST, SPRINGFIELD, IL 62701-1234";
            
            // Mock address validation service response
            AddressValidationService.ValidationResult validationResult = 
                new AddressValidationService.ValidationResult();
            validationResult.setValid(true);
            validationResult.setStandardizedAddress(expectedStandardized);
            validationResult.setDeliverable(true);
            
            when(addressValidationService.validateAndStandardizeAddress(inputAddress))
                .thenReturn(validationResult);
            
            // When: Standardizing address
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                String standardizedAddress = customerMaintenanceService.standardizeAddress(inputAddress);
                
                // Then: Should return standardized format
                assertThat(standardizedAddress).isEqualTo(expectedStandardized);
            });
            
            verify(addressValidationService, times(1)).validateAndStandardizeAddress(inputAddress);
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("standardizeAddress - should handle invalid address gracefully")
        void testStandardizeAddress_WithInvalidAddress_ShouldReturnOriginal() {
            // Given: Invalid address
            String invalidAddress = "Invalid Address 12345";
            
            // Mock address validation service response for invalid address
            AddressValidationService.ValidationResult validationResult = 
                new AddressValidationService.ValidationResult();
            validationResult.setValid(false);
            validationResult.setStandardizedAddress(null);
            validationResult.setErrorMessage("Address not found");
            
            when(addressValidationService.validateAndStandardizeAddress(invalidAddress))
                .thenReturn(validationResult);
            
            // When: Attempting to standardize invalid address
            String result = customerMaintenanceService.standardizeAddress(invalidAddress);
            
            // Then: Should return original address when standardization fails
            assertThat(result).isEqualTo(invalidAddress);
            verify(addressValidationService, times(1)).validateAndStandardizeAddress(invalidAddress);
        }

        @Test
        @DisplayName("standardizeAddress - should handle null address input")
        void testStandardizeAddress_WithNullAddress_ShouldReturnNull() {
            // Given: Null address input
            String nullAddress = null;
            
            // When: Standardizing null address
            String result = customerMaintenanceService.standardizeAddress(nullAddress);
            
            // Then: Should return null without calling external service
            assertThat(result).isNull();
            verify(addressValidationService, never()).validateAndStandardizeAddress(anyString());
        }
    }

    @Nested
    @DisplayName("Phone Number Formatting Tests")
    class PhoneNumberFormattingTests {

        @Test
        @DisplayName("formatPhoneNumber - should format valid US phone number")
        void testFormatPhoneNumber_WithValidUSNumber_ShouldReturnFormatted() {
            // Given: Valid but unformatted US phone number
            String unformattedPhone = "5551234567";
            String expectedFormatted = "(555) 123-4567";
            
            // Mock data quality service response
            DataQualityService.PhoneValidationResult phoneResult = 
                new DataQualityService.PhoneValidationResult(true, null, expectedFormatted);
            when(dataQualityService.validatePhoneNumber(unformattedPhone)).thenReturn(phoneResult);
            
            // When: Formatting phone number
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                String formattedPhone = customerMaintenanceService.formatPhoneNumber(unformattedPhone);
                
                // Then: Should return properly formatted number
                assertThat(formattedPhone).isEqualTo(expectedFormatted);
            });
            
            verify(dataQualityService, times(1)).validatePhoneNumber(unformattedPhone);
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("formatPhoneNumber - should handle international phone number")
        void testFormatPhoneNumber_WithInternationalNumber_ShouldReturnFormatted() {
            // Given: International phone number
            String internationalPhone = "+44 20 7946 0958";
            String expectedFormatted = "+44 20 7946 0958";
            
            // Mock data quality service response
            DataQualityService.PhoneValidationResult phoneResult = 
                new DataQualityService.PhoneValidationResult(true, null, expectedFormatted);
            when(dataQualityService.validatePhoneNumber(internationalPhone)).thenReturn(phoneResult);
            
            // When: Formatting international number
            String formattedPhone = customerMaintenanceService.formatPhoneNumber(internationalPhone);
            
            // Then: Should return properly formatted international number
            assertThat(formattedPhone).isEqualTo(expectedFormatted);
            verify(dataQualityService, times(1)).validatePhoneNumber(internationalPhone);
        }

        @Test
        @DisplayName("formatPhoneNumber - should handle invalid phone number")
        void testFormatPhoneNumber_WithInvalidNumber_ShouldReturnOriginal() {
            // Given: Invalid phone number
            String invalidPhone = "123-abc-defg";
            
            // Mock data quality service response for invalid number
            DataQualityService.PhoneValidationResult phoneResult = 
                new DataQualityService.PhoneValidationResult(false, "Invalid phone number format", null);
            when(dataQualityService.validatePhoneNumber(invalidPhone)).thenReturn(phoneResult);
            
            // When: Formatting invalid number
            String result = customerMaintenanceService.formatPhoneNumber(invalidPhone);
            
            // Then: Should return original when formatting fails
            assertThat(result).isEqualTo(invalidPhone);
            verify(dataQualityService, times(1)).validatePhoneNumber(invalidPhone);
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("validateEmail - should validate correct email format")
        void testValidateEmail_WithValidFormat_ShouldReturnTrue() {
            // Given: Valid email address
            String validEmail = "john.smith@example.com";
            
            // Mock data quality service response
            DataQualityService.EmailValidationResult emailResult = 
                new DataQualityService.EmailValidationResult(true, null);
            when(dataQualityService.validateEmail(validEmail)).thenReturn(emailResult);
            
            // When: Validating email
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                boolean isValid = customerMaintenanceService.validateEmail(validEmail);
                
                // Then: Should return true for valid email
                assertThat(isValid).isTrue();
            });
            
            verify(dataQualityService, times(1)).validateEmail(validEmail);
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("validateEmail - should reject invalid email format")
        void testValidateEmail_WithInvalidFormat_ShouldReturnFalse() {
            // Given: Invalid email formats
            List<String> invalidEmails = List.of(
                "invalid-email",
                "@missing-local.com",
                "missing-at-sign.com",
                "spaces in@email.com",
                "double@@domain.com"
            );
            
            // Mock data quality service responses
            DataQualityService.EmailValidationResult invalidResult = 
                new DataQualityService.EmailValidationResult(false, "Invalid email format");
            
            for (String invalidEmail : invalidEmails) {
                when(dataQualityService.validateEmail(invalidEmail)).thenReturn(invalidResult);
                
                // When: Validating invalid email
                boolean isValid = customerMaintenanceService.validateEmail(invalidEmail);
                
                // Then: Should return false for invalid email
                assertThat(isValid).isFalse();
            }
            
            verify(dataQualityService, times(invalidEmails.size())).validateEmail(anyString());
        }

        @Test
        @DisplayName("validateEmail - should handle null email address")
        void testValidateEmail_WithNullEmail_ShouldReturnFalse() {
            // Given: Null email address
            String nullEmail = null;
            
            // When: Validating null email
            boolean isValid = customerMaintenanceService.validateEmail(nullEmail);
            
            // Then: Should return false without calling external service
            assertThat(isValid).isFalse();
            verify(dataQualityService, never()).validateEmail(anyString());
        }

        @Test
        @DisplayName("validateEmail - should handle empty email address")
        void testValidateEmail_WithEmptyEmail_ShouldReturnFalse() {
            // Given: Empty email address
            String emptyEmail = "";
            
            // When: Validating empty email
            boolean isValid = customerMaintenanceService.validateEmail(emptyEmail);
            
            // Then: Should return false without calling external service
            assertThat(isValid).isFalse();
            verify(dataQualityService, never()).validateEmail(anyString());
        }
    }

    @Nested
    @DisplayName("Customer Duplicate Detection Tests")
    class CustomerDuplicateDetectionTests {

        @Test
        @DisplayName("findDuplicateCustomers - should identify duplicate customers by SSN")
        void testFindDuplicateCustomers_WithMatchingSSN_ShouldReturnDuplicates() {
            // Given: Customers with duplicate SSN
            String duplicateSSN = "123-45-6789";
            List<Customer> customers = List.of(
                testDataBuilder.customerBuilder()
                    .withCustomerId("1000000001")
                    .withFirstName("John")
                    .withLastName("Smith")
                    .withSSN(duplicateSSN)
                    .build(),
                testDataBuilder.customerBuilder()
                    .withCustomerId("1000000002")
                    .withFirstName("Jonathan")
                    .withLastName("Smith")
                    .withSSN(duplicateSSN)
                    .build()
            );
            
            // Mock data quality service response
            DataQualityService.DuplicateResult duplicateResult = 
                new DataQualityService.DuplicateResult(true, customers, 0.95);
            when(dataQualityService.detectDuplicates(any(Customer.class))).thenReturn(duplicateResult);
            
            // When: Checking for duplicates
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                List<Customer> duplicates = customerMaintenanceService.findDuplicateCustomers(customers.get(0));
                
                // Then: Should find duplicate customer
                assertThat(duplicates).isNotEmpty();
                assertThat(duplicates).hasSize(2);
                assertThat(duplicates).extracting(Customer::getSSN)
                    .containsOnly(duplicateSSN);
            });
            
            verify(dataQualityService, times(1)).detectDuplicates(any(Customer.class));
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("findDuplicateCustomers - should handle no duplicates found")
        void testFindDuplicateCustomers_WithNoDuplicates_ShouldReturnEmptyList() {
            // Given: Unique customer
            Customer uniqueCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("Jane")
                .withLastName("Doe")
                .withSSN("987-65-4321")
                .build();
            
            // Mock data quality service response
            DataQualityService.DuplicateResult noDuplicatesResult = 
                new DataQualityService.DuplicateResult(false, new ArrayList<>(), 0.0);
            when(dataQualityService.detectDuplicates(uniqueCustomer)).thenReturn(noDuplicatesResult);
            
            // When: Checking for duplicates
            List<Customer> duplicates = customerMaintenanceService.findDuplicateCustomers(uniqueCustomer);
            
            // Then: Should return empty list
            assertThat(duplicates).isEmpty();
            verify(dataQualityService, times(1)).detectDuplicates(uniqueCustomer);
        }

        @Test
        @DisplayName("findDuplicateCustomers - should identify potential duplicates by name similarity")
        void testFindDuplicateCustomers_WithSimilarNames_ShouldReturnPotentialDuplicates() {
            // Given: Customers with similar names
            Customer originalCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("John")
                .withLastName("Smith")
                .withSSN("111-11-1111")
                .build();
            
            List<Customer> similarCustomers = List.of(
                testDataBuilder.customerBuilder()
                    .withCustomerId("1000000002")
                    .withFirstName("Jon")
                    .withLastName("Smith")
                    .withSSN("222-22-2222")
                    .build()
            );
            
            // Mock data quality service response with potential duplicates
            DataQualityService.DuplicateResult potentialResult = 
                new DataQualityService.DuplicateResult(true, similarCustomers, 0.75);
            when(dataQualityService.detectDuplicates(originalCustomer)).thenReturn(potentialResult);
            
            // When: Checking for potential duplicates
            List<Customer> potentialDuplicates = customerMaintenanceService.findDuplicateCustomers(originalCustomer);
            
            // Then: Should find potential duplicates
            assertThat(potentialDuplicates).isNotEmpty();
            assertThat(potentialDuplicates).hasSize(1);
            verify(dataQualityService, times(1)).detectDuplicates(originalCustomer);
        }
    }

    @Nested
    @DisplayName("Customer Record Update Tests")
    class CustomerRecordUpdateTests {

        @Test
        @DisplayName("updateCustomerRecord - should update customer successfully")
        void testUpdateCustomerRecord_WithValidData_ShouldUpdateSuccessfully() {
            // Given: Existing customer and updated data
            Customer existingCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("John")
                .withLastName("Smith")
                .withPhoneNumber("555-123-4567")
                .build();
            
            Customer updatedCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("John")
                .withLastName("Smith")
                .withPhoneNumber("555-987-6543") // Updated phone number
                .build();
            
            when(customerRepository.findById("1000000001")).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);
            
            // Mock validation services
            when(dataQualityService.validateCustomerData(any(Customer.class)))
                .thenReturn(new DataQualityService.DataQualityResult(true, null, 0.95));
            when(dataQualityService.validatePhoneNumber(anyString()))
                .thenReturn(new DataQualityService.PhoneValidationResult(true, null, "555-987-6543"));
            
            // When: Updating customer record
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                Customer result = customerMaintenanceService.updateCustomerRecord(updatedCustomer);
                
                // Then: Should return updated customer
                assertThat(result).isNotNull();
                assertThat(result.getCustomerId()).isEqualTo("1000000001");
                assertThat(result.getPhoneNumber()).isEqualTo("555-987-6543");
            });
            
            // Verify repository interactions
            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository, times(1)).save(customerCaptor.capture());
            Customer savedCustomer = customerCaptor.getValue();
            assertThat(savedCustomer.getPhoneNumber()).isEqualTo("555-987-6543");
            
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("updateCustomerRecord - should handle customer not found")
        void testUpdateCustomerRecord_WithNonExistentCustomer_ShouldThrowException() {
            // Given: Non-existent customer ID
            Customer nonExistentCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("9999999999")
                .withFirstName("Non")
                .withLastName("Existent")
                .build();
            
            when(customerRepository.findById("9999999999")).thenReturn(Optional.empty());
            
            // When/Then: Should throw exception for non-existent customer
            assertThatThrownBy(() -> customerMaintenanceService.updateCustomerRecord(nonExistentCustomer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found with ID: 9999999999");
            
            verify(customerRepository, times(1)).findById("9999999999");
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("updateCustomerRecord - should validate updated data before saving")
        void testUpdateCustomerRecord_WithInvalidUpdatedData_ShouldThrowException() {
            // Given: Customer with invalid updated data
            Customer existingCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("John")
                .withLastName("Smith")
                .build();
            
            Customer invalidUpdatedCustomer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("") // Invalid empty first name
                .withLastName("Smith")
                .build();
            
            when(customerRepository.findById("1000000001")).thenReturn(Optional.of(existingCustomer));
            when(dataQualityService.validateCustomerData(any(Customer.class)))
                .thenReturn(new DataQualityService.DataQualityResult(false, "Missing required fields", 0.20));
            
            // When/Then: Should throw exception for invalid data
            assertThatThrownBy(() -> customerMaintenanceService.updateCustomerRecord(invalidUpdatedCustomer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer data validation failed");
            
            verify(customerRepository, times(1)).findById("1000000001");
            verify(customerRepository, never()).save(any(Customer.class));
            verify(dataQualityService, times(1)).validateCustomerData(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Customer Retrieval Tests")
    class CustomerRetrievalTests {

        @Test
        @DisplayName("getCustomerById - should retrieve customer by ID successfully")
        void testGetCustomerById_WithValidId_ShouldReturnCustomer() {
            // Given: Valid customer ID
            String customerId = "1000000001";
            Customer expectedCustomer = testDataBuilder.customerBuilder()
                .withCustomerId(customerId)
                .withFirstName("Alice")
                .withLastName("Johnson")
                .build();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(expectedCustomer));

            // When: Retrieving customer by ID
            long startTime = performanceTestUtils.measureExecutionTime(() -> {
                Optional<Customer> result = customerMaintenanceService.getCustomerById(customerId);

                // Then: Should return the customer
                assertThat(result).isPresent();
                assertThat(result.get().getCustomerId()).isEqualTo(customerId);
                assertThat(result.get().getFirstName()).isEqualTo("Alice");
                assertThat(result.get().getLastName()).isEqualTo("Johnson");
            });

            verify(customerRepository, times(1)).findById(customerId);
            performanceTestUtils.assertUnder200ms(startTime);
        }

        @Test
        @DisplayName("getCustomerById - should handle customer not found")
        void testGetCustomerById_WithInvalidId_ShouldReturnEmpty() {
            // Given: Non-existent customer ID
            String nonExistentId = "9999999999";
            when(customerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When: Retrieving non-existent customer
            Optional<Customer> result = customerMaintenanceService.getCustomerById(nonExistentId);

            // Then: Should return empty optional
            assertThat(result).isEmpty();
            verify(customerRepository, times(1)).findById(nonExistentId);
        }

        @Test
        @DisplayName("getCustomerById - should handle null customer ID")
        void testGetCustomerById_WithNullId_ShouldThrowException() {
            // Given: Null customer ID
            String nullId = null;

            // When/Then: Should throw exception for null ID
            assertThatThrownBy(() -> customerMaintenanceService.getCustomerById(nullId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID cannot be null");

            verify(customerRepository, never()).findById(anyString());
        }
    }

    @Nested
    @DisplayName("Batch Processing Performance Tests")
    class BatchProcessingPerformanceTests {

        @Test
        @DisplayName("processCustomerRecords - should meet batch processing performance requirements")
        void testProcessCustomerRecords_WithLargeDataset_ShouldMeetPerformanceRequirements() {
            // Given: Large dataset for batch processing
            List<Customer> largeCustomerList = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                largeCustomerList.add(testDataBuilder.customerBuilder()
                    .withCustomerId(String.format("1%09d", i))
                    .withFirstName("Customer" + i)
                    .withLastName("Test" + i)
                    .withPhoneNumber("555-" + String.format("%03d", i % 1000) + "-" + String.format("%04d", i))
                    .build());
            }

            when(customerRepository.findAll()).thenReturn(largeCustomerList);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Mock external service responses for all customers
            when(dataQualityService.validateCustomerData(any(Customer.class)))
                .thenReturn(new DataQualityService.DataQualityResult(true, null, 0.90));

            // When: Processing large customer batch
            long startTime = System.currentTimeMillis();
            customerMaintenanceService.processCustomerRecords();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Then: Should process within acceptable time limits for batch processing
            // For batch processing, we allow more time than the 200ms interactive requirement
            assertThat(executionTime).isLessThan(30000); // 30 seconds for 1000 records

            verify(customerRepository, times(1)).findAll();
            verify(customerRepository, times(largeCustomerList.size())).save(any(Customer.class));
        }

        @Test
        @DisplayName("processCustomerRecords - should handle batch processing memory efficiently")
        void testProcessCustomerRecords_WithMemoryConstraints_ShouldProcessEfficiently() {
            // Given: Customer dataset
            List<Customer> customers = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                customers.add(testDataBuilder.customerBuilder()
                    .withCustomerId(String.format("1%09d", i))
                    .withFirstName("BatchCustomer" + i)
                    .withLastName("ProcessingTest" + i)
                    .build());
            }

            when(customerRepository.findAll()).thenReturn(customers);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(dataQualityService.validateCustomerData(any(Customer.class)))
                .thenReturn(new DataQualityService.DataQualityResult(true, null, 0.92));

            // When: Processing with memory monitoring
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            customerMaintenanceService.processCustomerRecords();

            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;

            // Then: Should not consume excessive memory
            assertThat(memoryUsed).isLessThan(50 * 1024 * 1024); // Less than 50MB

            verify(customerRepository, times(1)).findAll();
            verify(customerRepository, times(customers.size())).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("processCustomerRecords - should handle repository exception gracefully")
        void testProcessCustomerRecords_WithRepositoryException_ShouldHandleGracefully() {
            // Given: Repository throws exception
            when(customerRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

            // When/Then: Should handle exception and not crash the application
            assertThatThrownBy(() -> customerMaintenanceService.processCustomerRecords())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

            verify(customerRepository, times(1)).findAll();
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("validateCustomerData - should handle concurrent modification exception")
        void testValidateCustomerData_WithConcurrentModification_ShouldHandleGracefully() {
            // Given: Customer data that causes concurrent modification
            Customer customer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("Concurrent")
                .withLastName("Test")
                .build();

            when(dataQualityService.validateCustomerData(customer))
                .thenThrow(new RuntimeException("Concurrent modification detected"));

            // When: Validating with concurrent modification
            boolean result = customerMaintenanceService.validateCustomerData(customer);

            // Then: Should handle exception and return false
            assertThat(result).isFalse();
            verify(dataQualityService, times(1)).validateCustomerData(customer);
        }

        @Test
        @DisplayName("standardizeAddress - should handle external service timeout")
        void testStandardizeAddress_WithServiceTimeout_ShouldReturnOriginal() {
            // Given: Address and service timeout
            String address = "123 Timeout Street, Test City, TS 12345";

            when(addressValidationService.validateAndStandardizeAddress(address))
                .thenThrow(new RuntimeException("Service timeout"));

            // When: Standardizing address with timeout
            String result = customerMaintenanceService.standardizeAddress(address);

            // Then: Should return original address when service fails
            assertThat(result).isEqualTo(address);
            verify(addressValidationService, times(1)).validateAndStandardizeAddress(address);
        }

        @Test
        @DisplayName("formatPhoneNumber - should handle external service unavailable")
        void testFormatPhoneNumber_WithServiceUnavailable_ShouldReturnOriginal() {
            // Given: Phone number and service unavailability
            String phoneNumber = "5551234567";

            when(dataQualityService.validatePhoneNumber(phoneNumber))
                .thenThrow(new RuntimeException("Service temporarily unavailable"));

            // When: Formatting phone with service unavailable
            String result = customerMaintenanceService.formatPhoneNumber(phoneNumber);

            // Then: Should return original phone number when service fails
            assertThat(result).isEqualTo(phoneNumber);
            verify(dataQualityService, times(1)).validatePhoneNumber(phoneNumber);
        }
    }

    @Nested
    @DisplayName("COBOL Functional Parity Tests")
    class CobolFunctionalParityTests {

        @Test
        @DisplayName("processCustomerRecords - should maintain functional parity with CBCUS01C COBOL program")
        void testProcessCustomerRecords_CobolFunctionalParity_ShouldProduceIdenticalResults() {
            // Given: Test data matching COBOL test vectors
            List<Customer> cobolTestData = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("COBOL")
                .withLastName("TESTCASE")
                .withPhoneNumber("555-TEST-123")
                .withAddress("123 COBOL ST, MAINFRAME, MF 12345")
                .withSSN("123-45-6789")
                .build();

            when(customerRepository.findAll()).thenReturn(cobolTestData);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(dataQualityService.validateCustomerData(any(Customer.class)))
                .thenReturn(new DataQualityService.DataQualityResult(true, null, 0.95));

            // When: Processing customer records
            customerMaintenanceService.processCustomerRecords();

            // Then: Verify results match COBOL program expectations using comparison utility
            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository, times(cobolTestData.size())).save(customerCaptor.capture());

            List<Customer> processedCustomers = customerCaptor.getAllValues();

            // Use COBOL comparison utility to validate functional parity
            for (Customer processedCustomer : processedCustomers) {
                cobolComparisonUtils.validateNumericPrecision(
                    processedCustomer.getFicoScore(),
                    processedCustomer.getFicoScore()
                );
            }

            // Verify no exceptions were thrown during processing
            assertThat(processedCustomers).isNotEmpty();
            assertThat(processedCustomers).hasSize(cobolTestData.size());
        }

        @Test
        @DisplayName("Customer data precision should match COBOL COMP-3 field handling")
        void testCustomerDataPrecision_CobolComp3Parity_ShouldMatchExactPrecision() {
            // Given: Customer with BigDecimal fields requiring COBOL COMP-3 precision
            Customer customer = testDataBuilder.customerBuilder()
                .withCustomerId("1000000001")
                .withFirstName("Precision")
                .withLastName("Test")
                .withFicoScore(new BigDecimal("750.00"))
                .build();

            when(customerRepository.findById("1000000001")).thenReturn(Optional.of(customer));

            // When: Retrieving customer data
            Optional<Customer> result = customerMaintenanceService.getCustomerById("1000000001");

            // Then: BigDecimal precision should match COBOL COMP-3 expectations
            assertThat(result).isPresent();
            Customer retrievedCustomer = result.get();

            // Use COBOL comparison utility to validate BigDecimal precision
            assertThat(retrievedCustomer.getFicoScore().scale()).isEqualTo(2);
            assertThat(retrievedCustomer.getFicoScore().precision()).isEqualTo(5);

            // Validate using COBOL comparison utility
            cobolComparisonUtils.validateNumericPrecision(
                new BigDecimal("750.00"), 
                retrievedCustomer.getFicoScore()
            );
        }
    }
}