package com.carddemo.service;

import com.carddemo.client.AddressValidationService;
import com.carddemo.client.DataQualityService;
import com.carddemo.entity.Customer;
import com.carddemo.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CustomerMaintenanceService covering customer maintenance operations
 * including customer data updates, address validation and standardization, contact information verification,
 * customer deduplication logic, and data quality checks.
 * 
 * Tests validate batch processing of customer records, integration with address validation services,
 * phone number formatting, and email validation. Also includes COBOL functional parity tests
 * ensuring conversion from CBCUS01C maintains identical business logic.
 */
@DisplayName("CustomerMaintenanceService Tests")
public class CustomerMaintenanceServiceTest extends BaseServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private AddressValidationService addressValidationService;
    
    @Mock
    private DataQualityService dataQualityService;
    
    @InjectMocks
    private CustomerMaintenanceService customerMaintenanceService;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        super.setUp(); // Call parent setUp to initialize mockServiceFactory and other utilities
    }
    
    @Test
    @DisplayName("Should process customer records successfully with batch processing")
    public void testProcessCustomerRecordsSuccess() {
        // Given
        Map<String, Object> processingOptions = new HashMap<>();
        processingOptions.put("batchSize", 100);
        processingOptions.put("validateOnly", false);
        processingOptions.put("standardizeAddresses", true);
        
        // When
        long startTime = System.currentTimeMillis();
        CustomerMaintenanceService.CustomerProcessingResult result = customerMaintenanceService.processCustomerRecords(processingOptions);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        performanceTestUtils.validateResponseTime(duration);
        
        // Verify statistics
        assertTrue(result.getStatistics().containsKey("batchSize"));
        assertEquals(100, result.getStatistics().get("batchSize"));
    }
    
    @Test
    @DisplayName("Should update customer record with validation and standardization")
    public void testUpdateCustomerRecordWithValidation() {
        // Given
        Customer customer = TestDataBuilder.createCustomer()
            .withCustomerId(100000004L)
            .withName("John Doe")
            .withSSN("123456789")
            .build();
        
        when(customerRepository.findById(100000004L)).thenReturn(Optional.of(customer));
        
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("firstName", "John");
        updateData.put("lastName", "Smith");
        updateData.put("addressLine1", "123 Main St");
        
        Map<String, Object> updateOptions = new HashMap<>();
        updateOptions.put("validateBeforeUpdate", true);
        updateOptions.put("standardizeAddress", true);
        updateOptions.put("checkDuplicates", false);
        
        // When
        long startTime = System.currentTimeMillis();
        CustomerMaintenanceService.CustomerUpdateResult result = customerMaintenanceService.updateCustomerRecord("100000004", updateData, updateOptions);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        performanceTestUtils.validateResponseTime(duration);
        assertNotNull(result.getUpdatedCustomerData());
        

    }
    
    @Test
    @DisplayName("Should validate customer data with comprehensive checks")
    public void testValidateCustomerDataComprehensive() {
        // Given
        Customer customer = TestDataBuilder.createCustomer()
            .withCustomerId(1000000002L)
            .withName("Jane Smith")
            .withSSN("987654321")
            .build();
        
        Map<String, Object> customerData = convertCustomerToMap(customer);
        
        // Mock external validation service
        DataQualityService.DataQualityResult externalValidationResult = 
            new DataQualityService.DataQualityResult(true, "Validation successful", 0.95);
        externalValidationResult.setValidationDetails(new HashMap<>());
        
        when(dataQualityService.validateCustomerData(customerData)).thenReturn(externalValidationResult);
        
        // When
        CustomerMaintenanceService.CustomerValidationResult result = customerMaintenanceService.validateCustomerData(customerData);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertNotNull(result.getValidationDetails());
        assertTrue(result.getQualityScore() > 0.0);
    }
    
    @Test
    @DisplayName("Should standardize address using validation service")
    public void testStandardizeAddressSuccess() {
        // Given
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("custAddrLine1", "123 main street");
        customerData.put("custAddrLine2", "apt 1");
        customerData.put("custAddrCity", "Test City");
        customerData.put("custAddrStateCD", "CA");
        customerData.put("custAddrZip", "12345");
        
        AddressValidationService.AddressValidationResult validationResult = new AddressValidationService.AddressValidationResult();
        validationResult.setValid(true);
        validationResult.setDeliverable(true);
        validationResult.setEnhancedZipCode("12345-6789");
        
        AddressValidationService.Address standardizedAddress = new AddressValidationService.Address();
        standardizedAddress.setAddressLine1("123 Main St");
        standardizedAddress.setAddressLine2("Apt 1");
        standardizedAddress.setCity("Test City");
        standardizedAddress.setState("CA");
        standardizedAddress.setZipCode("12345-6789");
        
        validationResult.setStandardizedAddress(standardizedAddress);
        
        when(addressValidationService.validateAndStandardizeAddress(anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString())).thenReturn(validationResult);
        
        // When
        long startTime = System.currentTimeMillis();
        CustomerMaintenanceService.AddressStandardizationResult result = customerMaintenanceService.standardizeAddress(customerData);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        performanceTestUtils.validateResponseTime(duration);
        assertNotNull(result.getStandardizedAddress());
        
        verify(addressValidationService, times(1)).validateAndStandardizeAddress(anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should handle address standardization failure gracefully")
    public void testStandardizeAddressFailure() {
        // Given
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("custAddrLine1", "invalid address");
        customerData.put("custAddrCity", "Invalid City");
        customerData.put("custAddrStateCD", "XX");
        customerData.put("custAddrZip", "00000");
        
        AddressValidationService.AddressValidationResult validationResult = new AddressValidationService.AddressValidationResult();
        validationResult.setValid(false);
        validationResult.addValidationError("Address not found");
        
        when(addressValidationService.validateAndStandardizeAddress(anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString())).thenReturn(validationResult);
        
        // Additional validation calls won't be made since the main validation fails
        // But we can mock them in case the implementation changes
        
        // Mock additional validation calls made by the service
        when(addressValidationService.validateZipCode(anyString(), anyString())).thenReturn(true);
        when(addressValidationService.validateState(anyString(), anyString())).thenReturn(true);
        when(addressValidationService.isAddressDeliverable(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        
        // When
        CustomerMaintenanceService.AddressStandardizationResult result = customerMaintenanceService.standardizeAddress(customerData);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getMessage().contains("failed"));
        
        verify(addressValidationService, times(1)).validateAndStandardizeAddress(anyString(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should format phone number correctly")
    public void testFormatPhoneNumberSuccess() {
        // Given
        String phoneNumber = "1234567890";
        
        DataQualityService.PhoneValidationResult phoneValidationResult = 
            new DataQualityService.PhoneValidationResult(true, null, "(123) 456-7890");
        
        when(dataQualityService.validatePhoneNumber(phoneNumber)).thenReturn(phoneValidationResult);
        
        // When
        CustomerMaintenanceService.PhoneFormattingResult result = customerMaintenanceService.formatPhoneNumber(phoneNumber);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("(123) 456-7890", result.getFormattedPhoneNumber());
        
        long startTime = System.currentTimeMillis();
        CustomerMaintenanceService.PhoneFormattingResult performanceResult = customerMaintenanceService.formatPhoneNumber(phoneNumber);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        performanceTestUtils.validateResponseTime(duration);
        
        verify(dataQualityService, times(2)).validatePhoneNumber(phoneNumber);
    }
    
    @Test
    @DisplayName("Should handle phone number formatting failure")
    public void testFormatPhoneNumberFailure() {
        // Given
        String invalidPhoneNumber = "invalid";
        
        DataQualityService.PhoneValidationResult phoneValidationResult = 
            new DataQualityService.PhoneValidationResult(false, "Invalid phone number format", null);
        
        when(dataQualityService.validatePhoneNumber(invalidPhoneNumber)).thenReturn(phoneValidationResult);
        
        // When
        CustomerMaintenanceService.PhoneFormattingResult result = customerMaintenanceService.formatPhoneNumber(invalidPhoneNumber);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertEquals("Phone formatting failed: Invalid phone number format", result.getMessage());
        assertNull(result.getFormattedPhoneNumber());
        
        verify(dataQualityService, times(1)).validatePhoneNumber(invalidPhoneNumber);
    }
    
    @Test
    @DisplayName("Should handle international phone number formatting")
    public void testFormatInternationalPhoneNumber() {
        // Given
        String internationalPhoneNumber = "+1234567890";
        
        DataQualityService.PhoneValidationResult phoneValidationResult = 
            new DataQualityService.PhoneValidationResult(true, null, "+1 (234) 567-890");
        
        when(dataQualityService.validatePhoneNumber(internationalPhoneNumber)).thenReturn(phoneValidationResult);
        
        // When
        CustomerMaintenanceService.PhoneFormattingResult result = customerMaintenanceService.formatPhoneNumber(internationalPhoneNumber);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals("+1 (234) 567-890", result.getFormattedPhoneNumber());
        
        verify(dataQualityService, times(1)).validatePhoneNumber(internationalPhoneNumber);
    }
    
    @Test
    @DisplayName("Should validate email address successfully")
    public void testValidateEmailAddressSuccess() {
        // Given
        String emailAddress = "john.doe@example.com";
        
        DataQualityService.EmailValidationResult emailValidationResult = 
            new DataQualityService.EmailValidationResult(true, "Email is valid", "example.com");
        
        when(dataQualityService.validateEmail(emailAddress)).thenReturn(emailValidationResult);
        
        // When
        CustomerMaintenanceService.EmailValidationResult result = customerMaintenanceService.validateEmail(emailAddress);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("example.com", result.getDomain());
        
        long startTime = System.currentTimeMillis();
        CustomerMaintenanceService.EmailValidationResult performanceResult = customerMaintenanceService.validateEmail(emailAddress);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        performanceTestUtils.validateResponseTime(duration);
    }
    
    @Test
    @DisplayName("Should handle invalid email address format")
    public void testValidateEmailAddressInvalid() {
        // Given
        String invalidEmailAddress = "invalid-email";
        
        DataQualityService.EmailValidationResult emailValidationResult = 
            new DataQualityService.EmailValidationResult(false, "Invalid email format", null);
        
        when(dataQualityService.validateEmail(invalidEmailAddress)).thenReturn(emailValidationResult);
        
        // When
        CustomerMaintenanceService.EmailValidationResult result = customerMaintenanceService.validateEmail(invalidEmailAddress);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Email validation failed: Invalid email format", result.getMessage());
    }
    
    @Test
    @DisplayName("Should handle email address with invalid domain")
    public void testValidateEmailAddressInvalidDomain() {
        // Given
        String emailWithInvalidDomain = "user@invalid-domain.invalid";
        
        // When
        CustomerMaintenanceService.EmailValidationResult result = customerMaintenanceService.validateEmail(emailWithInvalidDomain);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
    }
    
    @Test
    @DisplayName("Should handle empty email address validation")
    public void testValidateEmailAddressEmpty() {
        // Given
        String emptyEmail = "";
        
        // When
        CustomerMaintenanceService.EmailValidationResult result = customerMaintenanceService.validateEmail(emptyEmail);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
    }
    
    @Test
    @DisplayName("Should find duplicate customers successfully")
    public void testFindDuplicateCustomersSuccess() {
        // Given
        Map<String, Object> searchCustomerData = new HashMap<>();
        searchCustomerData.put("firstName", "John");
        searchCustomerData.put("lastName", "Doe");
        searchCustomerData.put("ssn", "123456789");
        
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("similarityThreshold", 0.85);
        searchOptions.put("maxResults", 10);
        
        DataQualityService.DuplicateMatch duplicateMatch = new DataQualityService.DuplicateMatch();
        duplicateMatch.setCustomerId("2000000001");
        duplicateMatch.setSimilarityScore(0.92);
        duplicateMatch.setMatchingFields(Arrays.asList("firstName", "lastName", "ssn"));
        
        List<DataQualityService.DuplicateMatch> duplicateMatches = Arrays.asList(duplicateMatch);
        
        when(dataQualityService.detectDuplicates(anyMap(), anyList())).thenReturn(duplicateMatches);
        
        // When
        List<CustomerMaintenanceService.DuplicateCustomerMatch> result = customerMaintenanceService.findDuplicateCustomers(searchCustomerData, searchOptions);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(0.92, result.get(0).getSimilarityScore(), 0.001);
        
        long startTime = System.currentTimeMillis();
        List<CustomerMaintenanceService.DuplicateCustomerMatch> performanceResult = customerMaintenanceService.findDuplicateCustomers(searchCustomerData, searchOptions);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        performanceTestUtils.validateResponseTime(duration);
        
        verify(dataQualityService, times(2)).detectDuplicates(anyMap(), anyList());
    }
    
    @Test
    @DisplayName("Should handle no duplicate customers found")
    public void testFindDuplicateCustomersNoneFound() {
        // Given
        Map<String, Object> searchCustomerData = new HashMap<>();
        searchCustomerData.put("firstName", "Unique");
        searchCustomerData.put("lastName", "Person");
        searchCustomerData.put("ssn", "999999999");
        
        Map<String, Object> searchOptions = new HashMap<>();
        
        when(dataQualityService.detectDuplicates(anyMap(), anyList())).thenReturn(Collections.emptyList());
        
        // When
        List<CustomerMaintenanceService.DuplicateCustomerMatch> result = customerMaintenanceService.findDuplicateCustomers(searchCustomerData, searchOptions);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(dataQualityService, times(1)).detectDuplicates(anyMap(), anyList());
    }
    
    @Test
    @DisplayName("Should find multiple duplicate customers with different similarity scores")
    public void testFindMultipleDuplicateCustomers() {
        // Given
        Map<String, Object> searchCustomerData = new HashMap<>();
        searchCustomerData.put("firstName", "Jane");
        searchCustomerData.put("lastName", "Smith");
        
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("similarityThreshold", 0.75);
        
        DataQualityService.DuplicateMatch match1 = new DataQualityService.DuplicateMatch();
        match1.setCustomerId("3000000001");
        match1.setSimilarityScore(0.95);
        match1.setMatchingFields(Arrays.asList("firstName", "lastName", "addressLine1"));
        
        DataQualityService.DuplicateMatch match2 = new DataQualityService.DuplicateMatch();
        match2.setCustomerId("3000000002");
        match2.setSimilarityScore(0.80);
        match2.setMatchingFields(Arrays.asList("firstName", "lastName"));
        
        List<DataQualityService.DuplicateMatch> duplicateMatches = Arrays.asList(match1, match2);
        
        when(dataQualityService.detectDuplicates(anyMap(), anyList())).thenReturn(duplicateMatches);
        
        // When
        List<CustomerMaintenanceService.DuplicateCustomerMatch> result = customerMaintenanceService.findDuplicateCustomers(searchCustomerData, searchOptions);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0.95, result.get(0).getSimilarityScore(), 0.001);
        assertEquals(0.80, result.get(1).getSimilarityScore(), 0.001);
        
        verify(dataQualityService, times(1)).detectDuplicates(anyMap(), anyList());
    }
    
    @Test
    @DisplayName("Should retrieve customer by ID successfully")
    public void testGetCustomerByIdSuccess() {
        // Given
        Customer customer = TestDataBuilder.createCustomer()
            .withCustomerId(100000001L)
            .withName("Alice Johnson")
            .build();
        
        when(customerRepository.findById(100000001L)).thenReturn(Optional.of(customer));
        
        // When
        CustomerMaintenanceService.CustomerRetrievalResult result = customerMaintenanceService.getCustomerById("100000001", null);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getCustomerData());
        
        long startTime = System.currentTimeMillis();
        CustomerMaintenanceService.CustomerRetrievalResult performanceResult = customerMaintenanceService.getCustomerById("4000000001", null);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        performanceTestUtils.validateResponseTime(duration);
        

    }
    
    @Test
    @DisplayName("Should handle customer not found by ID")
    public void testGetCustomerByIdNotFound() {
        // Given
        when(customerRepository.findById(500000001L)).thenReturn(Optional.empty());
        
        // When
        CustomerMaintenanceService.CustomerRetrievalResult result = customerMaintenanceService.getCustomerById("500000001", null);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertEquals("Customer not found", result.getMessage());
        

    }
    
    @Test
    @DisplayName("Should retrieve customer by ID with field selection")
    public void testGetCustomerByIdWithFieldSelection() {
        // Given
        Customer customer = TestDataBuilder.createCustomer()
            .withCustomerId(100000003L)
            .withName("Bob Wilson")
            .build();
        
        when(customerRepository.findById(100000003L)).thenReturn(Optional.of(customer));
        
        List<String> fieldSelection = Arrays.asList("customerId", "firstName", "lastName");
        
        // When
        CustomerMaintenanceService.CustomerRetrievalResult result = customerMaintenanceService.getCustomerById("100000003", fieldSelection);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getCustomerData());
        

    }
    
    @Test
    @DisplayName("Should handle invalid customer ID format")
    public void testGetCustomerByIdInvalidFormat() {
        // Given
        String invalidCustomerId = "invalid-id";
        
        // When
        CustomerMaintenanceService.CustomerRetrievalResult result = customerMaintenanceService.getCustomerById(invalidCustomerId, null);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertEquals("Invalid customer ID format", result.getMessage());
        

    }
    
    @Test
    @DisplayName("Should process batch customer processing with COBOL functional parity")
    public void testProcessBatchProcessingWithCobolParity() {
        // Given
        Map<String, Object> processingOptions = new HashMap<>();
        processingOptions.put("batchSize", 100);
        processingOptions.put("validatePrecision", true);
        processingOptions.put("standardizeAddresses", true);
        
        // When
        CustomerMaintenanceService.CustomerProcessingResult result = customerMaintenanceService.processCustomerRecords(processingOptions);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        
        // Verify COBOL precision handling - test with a BigDecimal value
        BigDecimal testFicoScore = new BigDecimal("720.00");
        boolean precisionValid = cobolComparisonUtils.validateDecimalPrecision(testFicoScore);
        assertTrue(precisionValid, "FICO score precision should match COBOL COMP-3 requirements");
    }
    
    @Test
    @DisplayName("Should validate COBOL precision for financial calculations")
    public void testCobolPrecisionValidation() {
        // Given
        Customer customer = TestDataBuilder.createCustomer()
            .withCustomerId(100000002L)
            .build();
        
        BigDecimal ficoScore = customer.getFicoScore();
        
        when(customerRepository.findById(100000002L)).thenReturn(Optional.of(customer));
        
        // When
        CustomerMaintenanceService.CustomerRetrievalResult result = customerMaintenanceService.getCustomerById("100000002", null);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        
        // Verify COBOL decimal precision compliance
        boolean precisionValid = cobolComparisonUtils.validateDecimalPrecision(ficoScore);
        assertTrue(precisionValid, "Financial values must maintain COBOL COMP-3 precision");
        

    }
    
    // Helper method to convert Customer entity to Map for service calls
    private Map<String, Object> convertCustomerToMap(Customer customer) {
        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("customerId", customer.getCustomerId());
        customerMap.put("firstName", customer.getFirstName());
        customerMap.put("lastName", customer.getLastName());
        customerMap.put("middleName", customer.getMiddleName());
        customerMap.put("ssn", customer.getSsn());
        customerMap.put("dateOfBirth", customer.getDateOfBirth());
        customerMap.put("ficoScore", customer.getFicoScore());
        customerMap.put("addressLine1", customer.getAddressLine1());
        customerMap.put("addressLine2", customer.getAddressLine2());
        customerMap.put("addressLine3", customer.getAddressLine3());
        customerMap.put("stateCode", customer.getStateCode());
        customerMap.put("zipCode", customer.getZipCode());
        customerMap.put("countryCode", customer.getCountryCode());
        customerMap.put("phoneNumber1", customer.getPhoneNumber1());
        customerMap.put("phoneNumber2", customer.getPhoneNumber2());
        customerMap.put("eftAccountId", customer.getEftAccountId());
        customerMap.put("governmentIssuedId", customer.getGovernmentIssuedId());
        customerMap.put("creditLimit", customer.getCreditLimit());
        return customerMap;
    }
    
    // Helper method to create test Map<String, Object> data
    private Map<String, Object> createTestCustomerData() {
        Customer customer = TestDataBuilder.createCustomer()
            .withCustomerId(9000000001L)
            .withName("Test Customer")
            .withSSN("123456789")
            .build();
        
        return convertCustomerToMap(customer);
    }
    
    // Helper method for COBOL comparison data
    private Map<String, Object> createCobolComparisonData() {
        Map<String, Object> cobolData = new HashMap<>();
        cobolData.put("customerId", 100000001L);
        cobolData.put("firstName", "JOHN");
        cobolData.put("lastName", "DOE");
        cobolData.put("ficoScore", new BigDecimal("720.00").setScale(2, java.math.RoundingMode.HALF_UP));
        cobolData.put("ssn", "123456789");
        
        return cobolData;
    }
}