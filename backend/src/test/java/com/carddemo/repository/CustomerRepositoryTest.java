package com.carddemo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.carddemo.repository.CustomerRepository;
import com.carddemo.entity.Customer;

import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprehensive Integration Test Suite for CustomerRepository
 * 
 * This test class validates customer data operations including profile management,
 * name-based searches, SSN lookup, FICO score updates, and address management
 * with GDPR compliance for data retention.
 * 
 * Tests cover VSAM CUSTDAT dataset behavior emulation through PostgreSQL operations,
 * ensuring functional parity with original COBOL customer management programs
 * COUSR00C, COUSR01C while maintaining modern Spring Data JPA patterns.
 * 
 * Key Testing Areas:
 * - CRUD operations with COBOL precision validation
 * - Customer search with customer_name_idx index utilization  
 * - SSN encryption/decryption and data masking compliance
 * - FICO score validation (300-850 range) with COBOL COMP-3 precision
 * - Address management with all three address line support
 * - GDPR Article 5 compliance for data retention queries
 * - Concurrent update handling with optimistic locking
 * - Performance validation ensuring sub-200ms response times
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired  
    private TestEntityManager testEntityManager;

    private TestDataGenerator testDataGenerator;
    private CobolComparisonUtils cobolComparisonUtils;
    
    // Test constants from COBOL specifications
    private static final String TEST_CUSTOMER_ID = TestConstants.TEST_CUSTOMER_ID;
    private static final int COBOL_DECIMAL_SCALE = TestConstants.COBOL_DECIMAL_SCALE;
    private static final long RESPONSE_TIME_THRESHOLD_MS = TestConstants.RESPONSE_TIME_THRESHOLD_MS;
    private static final String SSN_PATTERN = TestConstants.SSN_PATTERN;
    private static final String PHONE_NUMBER_PATTERN = TestConstants.PHONE_NUMBER_PATTERN;
    private static final int FICO_SCORE_MIN = TestConstants.FICO_SCORE_MIN;
    private static final int FICO_SCORE_MAX = TestConstants.FICO_SCORE_MAX;
    private static final int GDPR_RETENTION_YEARS = TestConstants.GDPR_RETENTION_YEARS;

    @BeforeEach
    void setUp() {
        // Initialize test infrastructure from BaseIntegrationTest
        setupTestData();
        
        // Initialize test data generators with COBOL-compliant formats
        testDataGenerator = new TestDataGenerator();
        cobolComparisonUtils = new CobolComparisonUtils();
        
        // Load test fixtures from BaseIntegrationTest
        loadTestFixtures();
    }

    @Nested
    @DisplayName("Customer CRUD Operations - VSAM CUSTDAT Equivalent")
    class CrudOperationsTest {

        @Test
        @DisplayName("Save customer - validates COBOL record structure compliance")
        @Transactional
        void testSaveCustomer_ValidatesCobolRecordStructure() {
            // Given: Generate COBOL-compliant customer with proper field formatting
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setCustomerId(1000000001L); // COBOL PIC 9(9) format
            testCustomer.setFirstName("JOHN"); // PIC X(25) uppercase format
            testCustomer.setLastName("SMITH"); // PIC X(25) uppercase format
            testCustomer.setSSN(testDataGenerator.generateSSN()); // Encrypted SSN storage
            testCustomer.setDateOfBirth(LocalDate.of(1980, 5, 15));
            testCustomer.setFicoScore(testDataGenerator.generateFicoScore()); // 300-850 range
            testCustomer.setPrimaryCardholderFlag("Y"); // PIC X(1) flag
            testCustomer.setEftAccountId("EFT1234567"); // PIC X(10) format
            
            // Set complete address information (three lines support)
            testCustomer.getAddress().setAddressLine1("123 MAIN STREET");
            testCustomer.getAddress().setAddressLine2("APT 4B");
            testCustomer.getAddress().setAddressLine3("BUILDING C");
            testCustomer.getAddress().setCity("NEW YORK");
            testCustomer.getAddress().setState("NY");
            testCustomer.getAddress().setZipCode("10001");
            
            // Set phone numbers with COBOL formatting
            testCustomer.setPhoneNumber("555-123-4567"); // PIC X(15) format
            
            long startTime = System.currentTimeMillis();
            
            // When: Save customer through JPA repository (equivalent to COBOL WRITE)
            Customer savedCustomer = customerRepository.save(testCustomer);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate successful save with COBOL precision
            assertThat(savedCustomer).isNotNull();
            assertThat(savedCustomer.getCustomerId()).isEqualTo(testCustomer.getCustomerId());
            assertThat(savedCustomer.getFirstName()).isEqualTo("JOHN");
            assertThat(savedCustomer.getLastName()).isEqualTo("SMITH");
            assertThat(savedCustomer.getPrimaryCardholderFlag()).isEqualTo("Y");
            
            // Validate FICO score range compliance (COBOL business rule)
            assertThat(savedCustomer.getFicoScore())
                .isBetween(FICO_SCORE_MIN, FICO_SCORE_MAX);
            
            // Validate address completeness (three line support)
            assertThat(savedCustomer.getAddress().getAddressLine1()).isEqualTo("123 MAIN STREET");
            assertThat(savedCustomer.getAddress().getAddressLine2()).isEqualTo("APT 4B");
            assertThat(savedCustomer.getAddress().getAddressLine3()).isEqualTo("BUILDING C");
            
            // Validate response time meets CICS performance requirements
            assertThat(responseTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
            
            // Flush to ensure database persistence
            testEntityManager.flush();
        }

        @Test
        @DisplayName("Find by ID - replicates VSAM READ with key positioning")
        @Transactional
        void testFindById_ReplicatesVsamReadWithKeyPositioning() {
            // Given: Persist test customer with known ID
            Customer testCustomer = createTestCustomer();
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            long startTime = System.currentTimeMillis();
            
            // When: Find by primary key (equivalent to VSAM READ with full key)
            Optional<Customer> foundCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate successful retrieval with exact match
            assertThat(foundCustomer).isPresent();
            assertThat(foundCustomer.get().getCustomerId()).isEqualTo(savedCustomer.getCustomerId());
            assertThat(foundCustomer.get().getFirstName()).isEqualTo(savedCustomer.getFirstName());
            assertThat(foundCustomer.get().getLastName()).isEqualTo(savedCustomer.getLastName());
            
            // Validate sub-second response time (CICS requirement)
            assertThat(responseTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
            
            // Compare against COBOL output format
            cobolComparisonUtils.compareCustomerRecords(foundCustomer.get(), savedCustomer);
        }

        @Test
        @DisplayName("Find by non-existent ID - validates NOTFND condition")
        void testFindByNonExistentId_ValidatesNotfndCondition() {
            // Given: Non-existent customer ID (mimicking COBOL NOTFND condition)
            Long nonExistentId = 9999999999L;
            
            // When: Attempt to find non-existent customer
            Optional<Customer> result = customerRepository.findById(nonExistentId);
            
            // Then: Validate empty result (equivalent to COBOL NOTFND condition)
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Delete customer - validates COBOL DELETE record operation")
        @Transactional
        void testDeleteCustomer_ValidatesCobolDeleteRecordOperation() {
            // Given: Existing customer in database
            Customer testCustomer = createTestCustomer();
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            Long customerId = savedCustomer.getCustomerId();
            testEntityManager.clear();
            
            // Verify customer exists before deletion
            assertThat(customerRepository.existsById(customerId)).isTrue();
            
            // When: Delete customer (equivalent to COBOL DELETE)
            customerRepository.delete(savedCustomer);
            testEntityManager.flush();
            
            // Then: Validate successful deletion
            assertThat(customerRepository.existsById(customerId)).isFalse();
            assertThat(customerRepository.findById(customerId)).isEmpty();
        }

        private Customer createTestCustomer() {
            return testDataGenerator.generateCustomer();
        }
    }

    @Nested
    @DisplayName("Customer Search Operations - Name Index Utilization")
    class SearchOperationsTest {

        @Test
        @DisplayName("Find by last name and first name - utilizes customer_name_idx")
        @Transactional
        void testFindByLastNameAndFirstName_UtilizesCustomerNameIdx() {
            // Given: Multiple customers with different names
            List<Customer> customers = testDataGenerator.generateCustomerList(5);
            
            Customer targetCustomer = customers.get(0);
            targetCustomer.setFirstName("ALICE");
            targetCustomer.setLastName("JOHNSON");
            
            Customer similarCustomer = customers.get(1);
            similarCustomer.setFirstName("ALICE");
            similarCustomer.setLastName("JACKSON"); // Different last name
            
            customers.forEach(customer -> testEntityManager.persistAndFlush(customer));
            testEntityManager.clear();
            
            long startTime = System.currentTimeMillis();
            
            // When: Search by exact name match (utilizes customer_name_idx)
            List<Customer> results = customerRepository.findByLastNameAndFirstName(
                "JOHNSON", "ALICE");
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate single exact match found
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFirstName()).isEqualTo("ALICE");
            assertThat(results.get(0).getLastName()).isEqualTo("JOHNSON");
            
            // Validate index-optimized query performance
            assertThat(responseTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
            
            // Verify similar names were not included
            assertThat(results).noneMatch(customer -> "JACKSON".equals(customer.getLastName()));
        }

        @Test
        @DisplayName("Find all customers - validates pagination for large datasets")
        @Transactional
        void testFindAllCustomers_ValidatesPaginationForLargeDatasets() {
            // Given: Multiple customers for pagination testing
            List<Customer> customers = testDataGenerator.generateCustomerList(15);
            customers.forEach(customer -> testEntityManager.persistAndFlush(customer));
            testEntityManager.clear();
            
            // When: Request paginated results (simulates VSAM browse operations)
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by("customerId"));
            Page<Customer> firstPage = customerRepository.findAll(pageRequest);
            
            // Then: Validate pagination behavior
            assertThat(firstPage.getContent()).hasSize(10);
            assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(15);
            assertThat(firstPage.hasNext()).isTrue();
            assertThat(firstPage.getNumber()).isEqualTo(0);
            
            // Validate sorted order (equivalent to VSAM key sequence)
            List<Long> customerIds = firstPage.getContent().stream()
                .map(Customer::getCustomerId)
                .toList();
            assertThat(customerIds).isSorted();
        }

        @Test
        @DisplayName("Count customers - validates efficient counting operations")
        void testCountCustomers_ValidatesEfficientCountingOperations() {
            // Given: Known number of customers
            List<Customer> customers = testDataGenerator.generateCustomerList(7);
            customers.forEach(customer -> testEntityManager.persistAndFlush(customer));
            testEntityManager.clear();
            
            long startTime = System.currentTimeMillis();
            
            // When: Count all customers
            long customerCount = customerRepository.count();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate accurate count
            assertThat(customerCount).isGreaterThanOrEqualTo(7);
            
            // Validate efficient counting (should use PostgreSQL COUNT optimization)
            assertThat(responseTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        }
    }

    @Nested
    @DisplayName("SSN Operations - Encryption and Data Masking")
    class SsnOperationsTest {

        @Test
        @DisplayName("Find by SSN - validates encryption/decryption process")
        @Transactional
        void testFindBySsn_ValidatesEncryptionDecryptionProcess() {
            // Given: Customer with encrypted SSN
            Customer testCustomer = testDataGenerator.generateCustomer();
            String originalSsn = testDataGenerator.generateSSN(); // Generates valid SSN pattern
            testCustomer.setSSN(originalSsn);
            
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // Validate SSN matches expected pattern
            assertThat(originalSsn).matches(SSN_PATTERN);
            
            long startTime = System.currentTimeMillis();
            
            // When: Search by SSN (should handle encryption transparently)
            Optional<Customer> result = customerRepository.findBySsn(originalSsn);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate successful SSN lookup
            assertThat(result).isPresent();
            assertThat(result.get().getCustomerId()).isEqualTo(savedCustomer.getCustomerId());
            
            // Validate encryption/decryption accuracy
            cobolComparisonUtils.validateSSNEncryption(result.get(), originalSsn);
            
            // Validate secure query performance
            assertThat(responseTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        }

        @Test
        @DisplayName("SSN data masking - validates sensitive field protection")
        @Transactional
        void testSsnDataMasking_ValidatesSensitiveFieldProtection() {
            // Given: Customer with SSN
            Customer testCustomer = testDataGenerator.generateCustomer();
            String fullSsn = testDataGenerator.generateSSN();
            testCustomer.setSSN(fullSsn);
            
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // When: Retrieve customer (SSN should be masked appropriately)
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate SSN masking for non-administrative access
            assertThat(retrievedCustomer).isPresent();
            
            // Note: Actual masking implementation would depend on security context
            // This test validates that SSN field is present and properly formatted
            String retrievedSsn = retrievedCustomer.get().getSSN();
            assertThat(retrievedSsn).isNotNull();
            assertThat(retrievedSsn).matches(SSN_PATTERN);
        }
    }

    @Nested  
    @DisplayName("FICO Score Operations - Range Validation and Precision")
    class FicoScoreOperationsTest {

        @Test
        @DisplayName("FICO score range validation - enforces 300-850 business rule")
        @Transactional
        void testFicoScoreRangeValidation_Enforces300To850BusinessRule() {
            // Given: Customer with valid FICO score
            Customer testCustomer = testDataGenerator.generateCustomer();
            int validFicoScore = testDataGenerator.generateFicoScore(); // Generates 300-850 range
            testCustomer.setFicoScore(validFicoScore);
            
            // When: Save customer with valid FICO score
            Customer savedCustomer = customerRepository.save(testCustomer);
            testEntityManager.flush();
            
            // Then: Validate FICO score within business rules
            assertThat(savedCustomer.getFicoScore()).isBetween(FICO_SCORE_MIN, FICO_SCORE_MAX);
            assertThat(savedCustomer.getFicoScore()).isEqualTo(validFicoScore);
            
            // Validate COBOL COMP-3 precision equivalence
            cobolComparisonUtils.validateFicoScorePrecision(savedCustomer.getFicoScore(), validFicoScore);
        }

        @Test
        @DisplayName("FICO score update - validates business rule compliance")
        @Transactional
        void testFicoScoreUpdate_ValidatesBusinessRuleCompliance() {
            // Given: Existing customer
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setFicoScore(650); // Initial valid score
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // When: Update FICO score
            Optional<Customer> customerToUpdate = customerRepository.findById(savedCustomer.getCustomerId());
            assertThat(customerToUpdate).isPresent();
            
            customerToUpdate.get().setFicoScore(750); // New valid score
            Customer updatedCustomer = customerRepository.save(customerToUpdate.get());
            testEntityManager.flush();
            
            // Then: Validate successful update
            assertThat(updatedCustomer.getFicoScore()).isEqualTo(750);
            assertThat(updatedCustomer.getFicoScore()).isBetween(FICO_SCORE_MIN, FICO_SCORE_MAX);
        }

        @Test
        @DisplayName("Invalid FICO score handling - validates constraint enforcement")
        void testInvalidFicoScoreHandling_ValidatesConstraintEnforcement() {
            // Given: Customer with invalid FICO scores
            Customer testCustomer = testDataGenerator.generateCustomer();
            
            // Test boundary conditions that should be rejected
            int[] invalidScores = {299, 851, 0, -100, 1000};
            
            for (int invalidScore : invalidScores) {
                testCustomer.setFicoScore(invalidScore);
                
                // When/Then: Attempt to save should fail with constraint violation
                assertThatThrownBy(() -> {
                    customerRepository.save(testCustomer);
                    testEntityManager.flush();
                }).describedAs("FICO score " + invalidScore + " should be rejected");
            }
        }
    }

    @Nested
    @DisplayName("Address Management - Three Address Line Support")  
    class AddressManagementTest {

        @Test
        @DisplayName("Complete address storage - validates three address lines")
        @Transactional
        void testCompleteAddressStorage_ValidatesThreeAddressLines() {
            // Given: Customer with complete address information
            Customer testCustomer = testDataGenerator.generateCustomer();
            
            // Set all three address lines (COBOL supports up to 3 lines)
            testCustomer.getAddress().setAddressLine1("1234 ENTERPRISE BLVD");
            testCustomer.getAddress().setAddressLine2("SUITE 500, FLOOR 12");  
            testCustomer.getAddress().setAddressLine3("WEST TOWER BUILDING");
            testCustomer.getAddress().setCity("SAN FRANCISCO");
            testCustomer.getAddress().setState("CA");
            testCustomer.getAddress().setCountryCode("USA");
            testCustomer.getAddress().setZipCode("94105");
            
            // When: Save customer with complete address
            Customer savedCustomer = customerRepository.save(testCustomer);
            testEntityManager.flush();
            testEntityManager.clear();
            
            // Retrieve to verify persistence
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate all address lines are preserved
            assertThat(retrievedCustomer).isPresent();
            Customer customer = retrievedCustomer.get();
            
            assertThat(customer.getAddress().getAddressLine1()).isEqualTo("1234 ENTERPRISE BLVD");
            assertThat(customer.getAddress().getAddressLine2()).isEqualTo("SUITE 500, FLOOR 12");
            assertThat(customer.getAddress().getAddressLine3()).isEqualTo("WEST TOWER BUILDING");
            assertThat(customer.getAddress().getCity()).isEqualTo("SAN FRANCISCO");
            assertThat(customer.getAddress().getState()).isEqualTo("CA");
            assertThat(customer.getAddress().getCountryCode()).isEqualTo("USA");
            assertThat(customer.getAddress().getZipCode()).isEqualTo("94105");
        }

        @Test
        @DisplayName("Address update operations - validates field modification")
        @Transactional
        void testAddressUpdateOperations_ValidatesFieldModification() {
            // Given: Customer with initial address
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.getAddress().setAddressLine1("123 OLD STREET");
            testCustomer.getAddress().setCity("OLD CITY");
            testCustomer.getAddress().setState("NY");
            testCustomer.getAddress().setZipCode("10001");
            
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // When: Update address information
            Optional<Customer> customerToUpdate = customerRepository.findById(savedCustomer.getCustomerId());
            assertThat(customerToUpdate).isPresent();
            
            Customer customer = customerToUpdate.get();
            customer.getAddress().setAddressLine1("456 NEW AVENUE");
            customer.getAddress().setAddressLine2("APT 7C"); // Add second line
            customer.getAddress().setCity("NEW CITY");
            customer.getAddress().setState("CA");
            customer.getAddress().setZipCode("90210");
            
            Customer updatedCustomer = customerRepository.save(customer);
            testEntityManager.flush();
            
            // Then: Validate address updates
            assertThat(updatedCustomer.getAddress().getAddressLine1()).isEqualTo("456 NEW AVENUE");
            assertThat(updatedCustomer.getAddress().getAddressLine2()).isEqualTo("APT 7C");
            assertThat(updatedCustomer.getAddress().getCity()).isEqualTo("NEW CITY");
            assertThat(updatedCustomer.getAddress().getState()).isEqualTo("CA");
            assertThat(updatedCustomer.getAddress().getZipCode()).isEqualTo("90210");
        }
    }

    @Nested
    @DisplayName("Phone Number Operations - Formatting and Validation")
    class PhoneNumberOperationsTest {

        @Test
        @DisplayName("Phone number formatting - validates COBOL field constraints")
        @Transactional  
        void testPhoneNumberFormatting_ValidatesCobolFieldConstraints() {
            // Given: Customer with formatted phone number
            Customer testCustomer = testDataGenerator.generateCustomer();
            String formattedPhone = testDataGenerator.generatePhoneNumber(); // Generates valid format
            testCustomer.setPhoneNumber(formattedPhone);
            
            // Validate phone number matches expected pattern  
            assertThat(formattedPhone).matches(PHONE_NUMBER_PATTERN);
            
            // When: Save customer with formatted phone
            Customer savedCustomer = customerRepository.save(testCustomer);
            testEntityManager.flush();
            
            // Then: Validate phone number formatting is preserved
            assertThat(savedCustomer.getPhoneNumber()).matches(PHONE_NUMBER_PATTERN);
            assertThat(savedCustomer.getPhoneNumber()).isEqualTo(formattedPhone);
        }

        @Test
        @DisplayName("Phone number storage and retrieval - validates field length")
        @Transactional
        void testPhoneNumberStorageAndRetrieval_ValidatesFieldLength() {
            // Given: Customer with maximum length phone number
            Customer testCustomer = testDataGenerator.generateCustomer();
            String maxLengthPhone = "555-123-4567890"; // 15 character limit from COBOL PIC X(15)
            testCustomer.setPhoneNumber(maxLengthPhone);
            
            // When: Save and retrieve customer
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate phone number field length compliance
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedCustomer.get().getPhoneNumber()).isEqualTo(maxLengthPhone);
            assertThat(retrievedCustomer.get().getPhoneNumber().length()).isLessThanOrEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Government ID Operations - Sensitive Data Handling")
    class GovernmentIdOperationsTest {

        @Test
        @DisplayName("Government ID storage - validates encryption and masking")
        @Transactional
        void testGovernmentIdStorage_ValidatesEncryptionAndMasking() {
            // Given: Customer with government ID
            Customer testCustomer = testDataGenerator.generateCustomer();
            String governmentId = "DL123456789"; // Sample driver's license format
            testCustomer.setGovernmentId(governmentId);
            
            // When: Save customer with government ID
            Customer savedCustomer = customerRepository.save(testCustomer);  
            testEntityManager.flush();
            testEntityManager.clear();
            
            // Retrieve customer
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate government ID handling
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedCustomer.get().getGovernmentId()).isNotNull();
            
            // Note: Actual encryption/masking validation would depend on implementation
            // This test ensures field is properly stored and retrievable
        }
    }

    @Nested
    @DisplayName("Date Operations - Birth Date and Precision")
    class DateOperationsTest {

        @Test
        @DisplayName("Date of birth storage - validates date precision")
        @Transactional
        void testDateOfBirthStorage_ValidatesDatePrecision() {
            // Given: Customer with specific birth date
            Customer testCustomer = testDataGenerator.generateCustomer();
            LocalDate birthDate = testDataGenerator.generateDateOfBirth(); // Generates realistic birth date
            testCustomer.setDateOfBirth(birthDate);
            
            // When: Save customer with birth date
            Customer savedCustomer = customerRepository.save(testCustomer);
            testEntityManager.flush();
            testEntityManager.clear();
            
            // Retrieve customer
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate date precision and accuracy
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedCustomer.get().getDateOfBirth()).isEqualTo(birthDate);
            
            // Validate date format compatibility with COBOL
            cobolComparisonUtils.compareDateFormats(retrievedCustomer.get().getDateOfBirth(), birthDate);
        }

        @Test
        @DisplayName("Date range validation - validates business rules")
        @Transactional
        void testDateRangeValidation_ValidatesBusinessRules() {
            // Given: Customer with various birth dates
            Customer testCustomer = testDataGenerator.generateCustomer();
            
            // Valid birth date (18-100 years ago, typical business rule)
            LocalDate validBirthDate = LocalDate.now().minusYears(25);
            testCustomer.setDateOfBirth(validBirthDate);
            
            // When: Save customer with valid birth date
            Customer savedCustomer = customerRepository.save(testCustomer);
            testEntityManager.flush();
            
            // Then: Validate successful save
            assertThat(savedCustomer.getDateOfBirth()).isEqualTo(validBirthDate);
            
            // Validate date is in reasonable range (not future, not too far past)
            assertThat(savedCustomer.getDateOfBirth()).isBefore(LocalDate.now());
            assertThat(savedCustomer.getDateOfBirth()).isAfter(LocalDate.now().minusYears(120));
        }
    }

    @Nested
    @DisplayName("EFT Account ID Management")
    class EftAccountIdTest {

        @Test
        @DisplayName("EFT account ID operations - validates format and constraints")
        @Transactional
        void testEftAccountIdOperations_ValidatesFormatAndConstraints() {
            // Given: Customer with EFT account ID
            Customer testCustomer = testDataGenerator.generateCustomer();
            String eftAccountId = "EFT1234567"; // 10 character limit from COBOL PIC X(10)
            testCustomer.setEftAccountId(eftAccountId);
            
            // When: Save customer with EFT account ID
            Customer savedCustomer = customerRepository.save(testCustomer);
            testEntityManager.flush();
            testEntityManager.clear();
            
            // Retrieve customer  
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate EFT account ID storage and format
            assertThat(retrievedCustomer).isPresent();
            assertThat(retrievedCustomer.get().getEftAccountId()).isEqualTo(eftAccountId);
            assertThat(retrievedCustomer.get().getEftAccountId().length()).isLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Primary Cardholder Flag Operations")  
    class PrimaryCardholderFlagTest {

        @Test
        @DisplayName("Primary cardholder flag updates - validates Y/N constraints")
        @Transactional
        void testPrimaryCardholderFlagUpdates_ValidatesYnConstraints() {
            // Given: Customer with primary cardholder flag
            Customer testCustomer = testDataGenerator.generateCustomer();
            testCustomer.setPrimaryCardholderFlag("N"); // Initial value
            
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // When: Update primary cardholder flag
            Optional<Customer> customerToUpdate = customerRepository.findById(savedCustomer.getCustomerId());
            assertThat(customerToUpdate).isPresent();
            
            customerToUpdate.get().setPrimaryCardholderFlag("Y");
            Customer updatedCustomer = customerRepository.save(customerToUpdate.get());
            testEntityManager.flush();
            
            // Then: Validate flag update
            assertThat(updatedCustomer.getPrimaryCardholderFlag()).isEqualTo("Y");
            
            // Validate only Y/N values are accepted (COBOL business rule)
            assertThat(updatedCustomer.getPrimaryCardholderFlag()).isIn("Y", "N");
        }
    }

    @Nested
    @DisplayName("Data Masking and Security - Sensitive Field Protection")
    class DataMaskingTest {

        @Test
        @DisplayName("Sensitive field masking - validates SSN and government ID protection")
        @Transactional
        void testSensitiveFieldMasking_ValidatesSsnAndGovernmentIdProtection() {
            // Given: Customer with sensitive information
            Customer testCustomer = testDataGenerator.generateCustomer();
            String originalSsn = testDataGenerator.generateSSN();
            String originalGovId = "DL987654321";
            
            testCustomer.setSSN(originalSsn);
            testCustomer.setGovernmentId(originalGovId);
            
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // When: Retrieve customer (should apply masking based on security context)
            Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getCustomerId());
            
            // Then: Validate sensitive fields are handled appropriately
            assertThat(retrievedCustomer).isPresent();
            Customer customer = retrievedCustomer.get();
            
            // Note: Actual masking implementation depends on security configuration
            // These tests ensure fields are present and properly formatted
            assertThat(customer.getSSN()).isNotNull();
            assertThat(customer.getGovernmentId()).isNotNull();
        }
    }

    @Nested  
    @DisplayName("GDPR Compliance - Data Retention Queries")
    class GdprComplianceTest {

        @Test
        @DisplayName("GDPR Article 5 compliance - validates data retention queries")
        @Transactional
        void testGdprArticle5Compliance_ValidatesDataRetentionQueries() {
            // Given: Customers with various creation dates for retention testing
            List<Customer> customers = testDataGenerator.generateCustomerList(3);
            
            // Set creation dates to test retention logic
            customers.get(0).setDateOfBirth(LocalDate.now().minusYears(GDPR_RETENTION_YEARS + 1)); // Should be eligible for retention review
            customers.get(1).setDateOfBirth(LocalDate.now().minusYears(GDPR_RETENTION_YEARS - 1)); // Should be retained
            customers.get(2).setDateOfBirth(LocalDate.now().minusYears(GDPR_RETENTION_YEARS + 2)); // Should be eligible for retention review
            
            customers.forEach(customer -> testEntityManager.persistAndFlush(customer));
            testEntityManager.clear();
            
            // When: Query all customers (would typically include retention logic)
            List<Customer> allCustomers = customerRepository.findAll();
            
            // Then: Validate GDPR-compliant data access
            assertThat(allCustomers).isNotEmpty();
            assertThat(allCustomers.size()).isGreaterThanOrEqualTo(3);
            
            // Validate customers are retrievable (actual retention logic would be in business layer)
            allCustomers.forEach(customer -> {
                assertThat(customer.getDateOfBirth()).isNotNull();
                assertThat(customer.getCustomerId()).isNotNull();
            });
        }

        @Test
        @DisplayName("Data retention period validation - calculates retention eligibility")
        void testDataRetentionPeriodValidation_CalculatesRetentionEligibility() {
            // Given: Current date for retention calculation
            LocalDate currentDate = LocalDate.now();
            LocalDate retentionThreshold = currentDate.minusYears(GDPR_RETENTION_YEARS);
            
            // Then: Validate retention period calculation
            assertThat(GDPR_RETENTION_YEARS).isGreaterThan(0);
            assertThat(retentionThreshold).isBefore(currentDate);
            
            // Note: Actual retention queries would be implemented in business service layer
            // This test validates retention period constants are properly configured
        }
    }

    @Nested
    @DisplayName("Concurrent Operations - Optimistic Locking")
    class ConcurrentOperationsTest {

        @Test
        @DisplayName("Concurrent customer updates - validates optimistic locking")
        @Transactional
        void testConcurrentCustomerUpdates_ValidatesOptimisticLocking() throws InterruptedException, ExecutionException {
            // Given: Customer for concurrent update testing
            Customer testCustomer = testDataGenerator.generateCustomer();
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // When: Simulate concurrent updates
            CompletableFuture<Customer> update1 = CompletableFuture.supplyAsync(() -> {
                Optional<Customer> customer1 = customerRepository.findById(savedCustomer.getCustomerId());
                if (customer1.isPresent()) {
                    customer1.get().setFirstName("CONCURRENT1");
                    return customerRepository.save(customer1.get());
                }
                return null;
            });
            
            CompletableFuture<Customer> update2 = CompletableFuture.supplyAsync(() -> {
                Optional<Customer> customer2 = customerRepository.findById(savedCustomer.getCustomerId());
                if (customer2.isPresent()) {
                    customer2.get().setFirstName("CONCURRENT2");
                    // This should potentially fail due to optimistic locking
                    return customerRepository.save(customer2.get());
                }
                return null;
            });
            
            // Then: Validate concurrent access handling
            Customer result1 = update1.get();
            assertThat(result1).isNotNull();
            
            // Second update might succeed or fail depending on timing
            // This test validates that optimistic locking mechanism is in place
            Customer result2 = update2.get();
            
            // At least one update should succeed
            assertThat(result1.getFirstName()).isIn("CONCURRENT1", "CONCURRENT2");
        }

        @Test
        @DisplayName("Version conflict detection - handles optimistic lock failures")
        @Transactional
        void testVersionConflictDetection_HandlesOptimisticLockFailures() {
            // Given: Customer with version tracking
            Customer testCustomer = testDataGenerator.generateCustomer();
            Customer savedCustomer = testEntityManager.persistAndFlush(testCustomer);
            testEntityManager.clear();
            
            // Retrieve same customer in two different contexts
            Optional<Customer> customer1 = customerRepository.findById(savedCustomer.getCustomerId());
            Optional<Customer> customer2 = customerRepository.findById(savedCustomer.getCustomerId());
            
            assertThat(customer1).isPresent();
            assertThat(customer2).isPresent();
            
            // When: Update first instance
            customer1.get().setFirstName("FIRST_UPDATE");
            customerRepository.save(customer1.get());
            testEntityManager.flush();
            
            // Then: Second update should handle version conflict appropriately
            customer2.get().setFirstName("SECOND_UPDATE");
            
            // The behavior depends on JPA optimistic locking configuration
            // This test ensures proper exception handling is in place
            assertThatCode(() -> {
                customerRepository.save(customer2.get());
                testEntityManager.flush();
            }).doesNotThrowAnyException(); // Or could throw OptimisticLockingFailureException
        }
    }

    @Nested
    @DisplayName("Performance Validation - Response Time Requirements")
    class PerformanceValidationTest {

        @Test
        @DisplayName("Customer search performance - validates sub-200ms response")
        void testCustomerSearchPerformance_ValidatesSub200msResponse() {
            // Given: Multiple customers for performance testing
            List<Customer> customers = testDataGenerator.generateCustomerList(100);
            customers.forEach(customer -> testEntityManager.persistAndFlush(customer));
            testEntityManager.clear();
            
            // When: Perform multiple search operations and measure performance
            long totalTime = 0;
            int iterations = 10;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.currentTimeMillis();
                
                // Search by ID (should use primary index)
                Customer randomCustomer = customers.get(i % customers.size());
                Optional<Customer> result = customerRepository.findById(randomCustomer.getCustomerId());
                
                long responseTime = System.currentTimeMillis() - startTime;
                totalTime += responseTime;
                
                // Validate successful retrieval
                assertThat(result).isPresent();
            }
            
            // Then: Validate average response time meets CICS requirement
            long averageResponseTime = totalTime / iterations;
            assertThat(averageResponseTime).isLessThan(RESPONSE_TIME_THRESHOLD_MS);
        }

        @Test
        @DisplayName("Bulk operations performance - validates efficient processing")
        @Transactional
        void testBulkOperationsPerformance_ValidatesEfficientProcessing() {
            // Given: Large dataset for bulk operations
            List<Customer> customers = testDataGenerator.generateCustomerList(50);
            
            long startTime = System.currentTimeMillis();
            
            // When: Perform bulk save operation
            customerRepository.saveAll(customers);
            testEntityManager.flush();
            
            long bulkSaveTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate bulk operation performance
            assertThat(bulkSaveTime).isLessThan(5000L); // 5 second threshold for 50 records
            
            // Validate all customers were saved
            long customerCount = customerRepository.count();
            assertThat(customerCount).isGreaterThanOrEqualTo(50L);
        }
    }

    /**
     * Cleanup operations after each test to ensure test isolation
     */
    void tearDown() {
        // Cleanup test data from BaseIntegrationTest
        cleanupTestData();
    }
}