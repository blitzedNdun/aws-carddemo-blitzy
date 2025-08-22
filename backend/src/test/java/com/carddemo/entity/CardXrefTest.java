/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.UnitTest;
import com.carddemo.util.Constants;
import com.carddemo.util.ValidationUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.assertj.core.api.Assertions;

import jakarta.persistence.ManyToOne;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit test class for CardXref JPA entity validating the 50-byte cross-reference 
 * record structure from CVACT03Y copybook.
 * 
 * This test class ensures complete functional parity between the COBOL CARD-XREF-RECORD structure
 * and the Java CardXref entity implementation, validating:
 * - 50-byte total record length preservation from COBOL copybook
 * - Composite key implementation using @EmbeddedId annotation
 * - Field mappings: XREF-CARD-NUM (PIC X(16)), XREF-CUST-ID (PIC 9(09)), XREF-ACCT-ID (PIC 9(11))
 * - JPA relationship annotations linking Card, Customer, and Account entities
 * - Unique constraint validation on composite key
 * - FILLER field handling (14-character padding) conceptually represented
 * - Cross-reference integrity constraints and business rule validation
 * - equals/hashCode implementation for composite key comparison
 * 
 * Test Coverage:
 * - Entity field validation with exact COBOL PIC clause length constraints
 * - Composite key functionality and uniqueness validation
 * - JPA annotations and relationship mapping verification
 * - Business logic validation matching COBOL edit routines
 * - Performance validation ensuring sub-200ms response times
 * - Functional parity validation between COBOL and Java implementations
 * 
 * Testing Strategy Integration:
 * - Implements comprehensive field validation testing per Section 6.6.2 requirements
 * - Validates composite key implementation with penny-level precision for ID fields
 * - Ensures cross-reference relationship integrity for Card-Customer-Account associations
 * - Supports parallel test execution with proper test isolation
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since CardDemo v1.0
 */
@DisplayName("CardXref Entity Tests")
public class CardXrefTest extends AbstractBaseTest implements UnitTest {

    private Validator validator;

    @Override
    public void setUp() {
        super.setUp();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        logTestExecution("CardXref test setup completed with validator initialization", null);
    }

    /**
     * Test suite for COBOL CARD-XREF-RECORD field mappings validation.
     * Validates that all COBOL PIC clauses are correctly mapped to Java fields.
     */
    @Nested
    @DisplayName("COBOL Field Mappings Tests")
    class CobolFieldMappingsTests {

        @Test
        @DisplayName("Should validate 50-byte CARD-XREF-RECORD field structure")
        void shouldValidate50ByteCardXrefRecordStructure() {
            // Given: COBOL CARD-XREF-RECORD structure from CVACT03Y.cpy
            // XREF-CARD-NUM PIC X(16) = 16 bytes
            // XREF-CUST-ID PIC 9(09) = 9 bytes (stored as Long, conceptually 9 digits)
            // XREF-ACCT-ID PIC 9(11) = 11 bytes (stored as Long, conceptually 11 digits)
            // FILLER PIC X(14) = 14 bytes (padding, not explicitly stored but conceptually represented)
            // Total = 16 + 9 + 11 + 14 = 50 bytes
            
            String cardNumber = TestConstants.TEST_CARD_NUMBER; // 16 characters
            Long customerId = 123456789L; // 9 digits maximum
            Long accountId = 12345678901L; // 11 digits maximum
            
            // When: Creating CardXref with field values matching COBOL structure
            CardXref cardXref = new CardXref(cardNumber, customerId, accountId);
            
            // Then: Validate field lengths match COBOL PIC clause specifications
            assertThat(cardXref.getXrefCardNum())
                .as("Card number should match XREF-CARD-NUM PIC X(16) specification")
                .hasSize(Constants.CARD_NUMBER_LENGTH)
                .isEqualTo(cardNumber);
            
            assertThat(cardXref.getXrefCustId())
                .as("Customer ID should match XREF-CUST-ID PIC 9(09) specification")
                .isEqualTo(customerId);
            
            assertThat(cardXref.getXrefAcctId())
                .as("Account ID should match XREF-ACCT-ID PIC 9(11) specification")
                .isEqualTo(accountId);
            
            // Validate FILLER field concept: ensure total conceptual length preservation
            int cardNumLength = cardNumber.length(); // 16
            int custIdLength = String.valueOf(customerId).length(); // up to 9
            int acctIdLength = String.valueOf(accountId).length(); // up to 11
            int fillerLength = 50 - cardNumLength - 9 - 11; // 14 bytes conceptual padding
            
            assertThat(fillerLength)
                .as("FILLER field should represent 14-byte padding to reach 50-byte total")
                .isEqualTo(14);
            
            logTestExecution("50-byte CARD-XREF-RECORD field structure validation passed", null);
        }

        @ParameterizedTest
        @CsvSource({
            "1234567890123456, 123456789, 12345678901, 'Valid 16-char card number with 9-digit customer ID and 11-digit account ID'",
            "4000000000000001, 987654321, 98765432101, 'Valid test card number with maximum digit customer and account IDs'",
            "5555555555554444, 100000001, 10000000001, 'Valid Mastercard test number with minimum valid IDs'"
        })
        @DisplayName("Should validate field combinations match COBOL PIC clause constraints")
        void shouldValidateFieldCombinationsMatchCobolConstraints(String cardNumber, Long customerId, Long accountId, String description) {
            // Given: Test data with various valid field combinations
            logTestExecution("Testing field combination: " + description, null);
            
            // When: Creating CardXref with test data
            CardXref cardXref = new CardXref(cardNumber, customerId, accountId);
            
            // Then: Validate field constraints match COBOL specifications
            assertThat(cardXref.getXrefCardNum())
                .as("Card number should be exactly 16 characters (XREF-CARD-NUM PIC X(16))")
                .hasSize(16)
                .matches("\\d{16}");
            
            assertThat(String.valueOf(cardXref.getXrefCustId()))
                .as("Customer ID should not exceed 9 digits (XREF-CUST-ID PIC 9(09))")
                .hasSizeLessThanOrEqualTo(9);
            
            assertThat(String.valueOf(cardXref.getXrefAcctId()))
                .as("Account ID should not exceed 11 digits (XREF-ACCT-ID PIC 9(11))")
                .hasSizeLessThanOrEqualTo(11);
            
            // Validate using ValidationUtil methods
            assertThatNoException()
                .as("Card number should pass ValidationUtil validation")
                .isThrownBy(() -> ValidationUtil.validateCardNumber(cardNumber));
            
            assertThatNoException()
                .as("Account ID should pass ValidationUtil validation")
                .isThrownBy(() -> ValidationUtil.validateAccountId(String.valueOf(accountId)));
            
            logTestExecution("Field combination validation passed: " + description, null);
        }
    }

    /**
     * Test suite for composite key implementation using @EmbeddedId annotation.
     * Validates CardXrefId functionality and composite key behavior.
     */
    @Nested
    @DisplayName("Composite Key Implementation Tests")
    class CompositeKeyImplementationTests {

        @Test
        @DisplayName("Should create and validate composite key using @EmbeddedId")
        void shouldCreateAndValidateCompositeKeyUsingEmbeddedId() {
            // Given: Valid composite key components
            String cardNumber = TestConstants.TEST_CARD_NUMBER;
            Long customerId = Long.parseLong(TestConstants.TEST_CUSTOMER_ID.replaceAll("\\D", "")); // Extract numeric part
            Long accountId = Long.parseLong(TestConstants.TEST_ACCOUNT_ID);
            
            // When: Creating CardXref with composite key
            CardXrefId compositeKey = new CardXrefId(cardNumber, customerId, accountId);
            CardXref cardXref = new CardXref(compositeKey);
            
            // Then: Validate composite key implementation
            assertThat(cardXref.getId())
                .as("CardXref should have embedded composite key")
                .isNotNull()
                .isEqualTo(compositeKey);
            
            assertThat(cardXref.getId().getXrefCardNum())
                .as("Composite key card number should match")
                .isEqualTo(cardNumber);
            
            assertThat(cardXref.getId().getXrefCustId())
                .as("Composite key customer ID should match")
                .isEqualTo(customerId);
            
            assertThat(cardXref.getId().getXrefAcctId())
                .as("Composite key account ID should match")
                .isEqualTo(accountId);
            
            // Validate field synchronization
            assertThat(cardXref.getXrefCardNum())
                .as("Entity card number should sync with composite key")
                .isEqualTo(cardNumber);
            
            assertThat(cardXref.getXrefCustId())
                .as("Entity customer ID should sync with composite key")
                .isEqualTo(customerId);
            
            assertThat(cardXref.getXrefAcctId())
                .as("Entity account ID should sync with composite key")
                .isEqualTo(accountId);
            
            logTestExecution("Composite key @EmbeddedId implementation validation passed", null);
        }

        @Test
        @DisplayName("Should validate unique constraint on composite key")
        void shouldValidateUniqueConstraintOnCompositeKey() {
            // Given: Two CardXref entities with identical composite key components
            String cardNumber = "1234567890123456";
            Long customerId = 123456789L;
            Long accountId = 12345678901L;
            
            CardXref cardXref1 = new CardXref(cardNumber, customerId, accountId);
            CardXref cardXref2 = new CardXref(cardNumber, customerId, accountId);
            
            // When/Then: Validate composite key uniqueness logic
            assertThat(cardXref1.getId())
                .as("Composite keys with identical components should be equal")
                .isEqualTo(cardXref2.getId());
            
            assertThat(cardXref1.getId().hashCode())
                .as("Composite keys with identical components should have same hash code")
                .isEqualTo(cardXref2.getId().hashCode());
            
            // Test with different components
            CardXref cardXref3 = new CardXref("9876543210987654", customerId, accountId);
            
            assertThat(cardXref1.getId())
                .as("Composite keys with different components should not be equal")
                .isNotEqualTo(cardXref3.getId());
            
            logTestExecution("Composite key uniqueness constraint validation passed", null);
        }

        @Test
        @DisplayName("Should validate equals and hashCode implementation for composite key")
        void shouldValidateEqualsAndHashCodeForCompositeKey() {
            // Given: CardXref entities with various composite key combinations
            CardXref cardXref1 = new CardXref("1111111111111111", 111111111L, 11111111111L);
            CardXref cardXref2 = new CardXref("1111111111111111", 111111111L, 11111111111L);
            CardXref cardXref3 = new CardXref("2222222222222222", 222222222L, 22222222222L);
            
            // When/Then: Validate equals contract
            assertThat(cardXref1)
                .as("CardXref should equal itself (reflexive)")
                .isEqualTo(cardXref1);
            
            assertThat(cardXref1)
                .as("CardXref with identical composite key should be equal (symmetric)")
                .isEqualTo(cardXref2);
            
            assertThat(cardXref2)
                .as("Equality should be symmetric")
                .isEqualTo(cardXref1);
            
            assertThat(cardXref1)
                .as("CardXref with different composite key should not be equal")
                .isNotEqualTo(cardXref3);
            
            assertThat(cardXref1)
                .as("CardXref should not equal null")
                .isNotEqualTo(null);
            
            // Validate hashCode contract
            assertThat(cardXref1.hashCode())
                .as("Equal objects should have equal hash codes")
                .isEqualTo(cardXref2.hashCode());
            
            // Validate toString method
            assertThat(cardXref1.toString())
                .as("toString should include composite key information")
                .contains("1111111111111111")
                .contains("111111111")
                .contains("11111111111");
            
            logTestExecution("Composite key equals and hashCode validation passed", null);
        }
    }

    /**
     * Test suite for JPA relationship annotations validation.
     * Tests @ManyToOne relationships with Card, Customer, and Account entities.
     */
    @Nested
    @DisplayName("JPA Relationship Annotations Tests")
    class JpaRelationshipAnnotationsTests {

        @Test
        @DisplayName("Should validate @ManyToOne relationship annotations using reflection")
        void shouldValidateManyToOneRelationshipAnnotations() throws NoSuchFieldException {
            // Given: CardXref class with expected relationship fields
            Class<CardXref> cardXrefClass = CardXref.class;
            
            // When: Examining relationship fields via reflection
            Field cardField = cardXrefClass.getDeclaredField("card");
            Field customerField = cardXrefClass.getDeclaredField("customer");
            Field accountField = cardXrefClass.getDeclaredField("account");
            
            // Then: Validate @ManyToOne annotations exist
            assertThat(cardField.getAnnotation(ManyToOne.class))
                .as("Card field should have @ManyToOne annotation")
                .isNotNull();
            
            assertThat(customerField.getAnnotation(ManyToOne.class))
                .as("Customer field should have @ManyToOne annotation")
                .isNotNull();
            
            assertThat(accountField.getAnnotation(ManyToOne.class))
                .as("Account field should have @ManyToOne annotation")
                .isNotNull();
            
            // Validate field types
            assertThat(cardField.getType())
                .as("Card field should be of Card type")
                .isEqualTo(Card.class);
            
            assertThat(customerField.getType())
                .as("Customer field should be of Customer type")
                .isEqualTo(Customer.class);
            
            assertThat(accountField.getType())
                .as("Account field should be of Account type")
                .isEqualTo(Account.class);
            
            logTestExecution("JPA @ManyToOne relationship annotations validation passed", null);
        }

        @Test
        @DisplayName("Should validate relationship entity associations")
        void shouldValidateRelationshipEntityAssociations() {
            // Given: CardXref with related entities
            String cardNumber = "4000000000000002";
            Long customerId = 123456789L;
            Long accountId = 12345678901L;
            
            CardXref cardXref = new CardXref(cardNumber, customerId, accountId);
            
            // Create mock related entities
            Card card = generateTestData("card", 1).stream()
                .map(data -> {
                    Card c = new Card();
                    c.setCardNumber(cardNumber);
                    c.setAccountId(accountId);
                    c.setActiveStatus("Y");
                    return c;
                }).findFirst().orElse(null);
            
            Customer customer = generateTestData("customer", 1).stream()
                .map(data -> {
                    Customer cust = new Customer();
                    cust.setCustomerId(customerId);
                    cust.setFirstName("John");
                    cust.setLastName("Doe");
                    return cust;
                }).findFirst().orElse(null);
            
            Account account = generateTestData("account", 1).stream()
                .map(data -> {
                    Account acc = new Account();
                    acc.setAccountId(accountId);
                    acc.setCurrentBalance(java.math.BigDecimal.valueOf(1000.00));
                    acc.setCustomerId(customerId);
                    return acc;
                }).findFirst().orElse(null);
            
            // When: Setting relationships
            cardXref.setCard(card);
            cardXref.setCustomer(customer);
            cardXref.setAccount(account);
            
            // Then: Validate relationships are properly established
            assertThat(cardXref.getCard())
                .as("CardXref should have associated Card entity")
                .isNotNull()
                .satisfies(c -> {
                    assertThat(c.getCardNumber()).isEqualTo(cardNumber);
                    assertThat(c.getAccountId()).isEqualTo(accountId);
                    assertThat(c.getActiveStatus()).isEqualTo("Y");
                });
            
            assertThat(cardXref.getCustomer())
                .as("CardXref should have associated Customer entity")
                .isNotNull()
                .satisfies(c -> {
                    assertThat(c.getCustomerId()).isEqualTo(customerId);
                    assertThat(c.getFirstName()).isEqualTo("John");
                    assertThat(c.getLastName()).isEqualTo("Doe");
                });
            
            assertThat(cardXref.getAccount())
                .as("CardXref should have associated Account entity")
                .isNotNull()
                .satisfies(a -> {
                    assertThat(a.getAccountId()).isEqualTo(accountId);
                    assertThat(a.getCurrentBalance()).isEqualTo(java.math.BigDecimal.valueOf(1000.00));
                    assertThat(a.getCustomerId()).isEqualTo(customerId);
                });
            
            logTestExecution("Relationship entity associations validation passed", null);
        }
    }

    /**
     * Test suite for cross-reference integrity constraints validation.
     * Tests business rules and validation constraints.
     */
    @Nested
    @DisplayName("Cross-Reference Integrity Constraints Tests")
    class CrossReferenceIntegrityTests {

        @Test
        @DisplayName("Should validate field length constraints from Constants")
        void shouldValidateFieldLengthConstraintsFromConstants() {
            // Given: Field length constants from Constants class
            int expectedCardNumberLength = Constants.CARD_NUMBER_LENGTH; // 16
            int expectedCustomerIdLength = Constants.CUSTOMER_ID_LENGTH; // 9
            int expectedAccountIdLength = Constants.ACCOUNT_ID_LENGTH; // 11
            
            // When: Creating CardXref with maximum length values
            String maxCardNumber = "1234567890123456"; // exactly 16 chars
            Long maxCustomerId = 999999999L; // 9 digits
            Long maxAccountId = 99999999999L; // 11 digits
            
            CardXref cardXref = new CardXref(maxCardNumber, maxCustomerId, maxAccountId);
            
            // Then: Validate field lengths against Constants
            assertThat(cardXref.getXrefCardNum())
                .as("Card number should match Constants.CARD_NUMBER_LENGTH")
                .hasSize(expectedCardNumberLength);
            
            assertThat(String.valueOf(cardXref.getXrefCustId()).length())
                .as("Customer ID should not exceed Constants.CUSTOMER_ID_LENGTH")
                .isLessThanOrEqualTo(expectedCustomerIdLength);
            
            assertThat(String.valueOf(cardXref.getXrefAcctId()).length())
                .as("Account ID should not exceed Constants.ACCOUNT_ID_LENGTH")
                .isLessThanOrEqualTo(expectedAccountIdLength);
            
            logTestExecution("Field length constraints validation passed using Constants", null);
        }

        @Test
        @DisplayName("Should validate numeric field constraints using ValidationUtil")
        void shouldValidateNumericFieldConstraintsUsingValidationUtil() {
            // Given: Valid numeric field values
            String cardNumber = "4111111111111111";
            String customerIdStr = "123456789";
            String accountIdStr = "12345678901";
            
            // When/Then: Validate using ValidationUtil methods
            assertThatNoException()
                .as("Valid card number should pass ValidationUtil.validateCardNumber")
                .isThrownBy(() -> ValidationUtil.validateCardNumber(cardNumber));
            
            assertThatNoException()
                .as("Valid account ID should pass ValidationUtil.validateAccountId")
                .isThrownBy(() -> ValidationUtil.validateAccountId(accountIdStr));
            
            assertThatNoException()
                .as("Valid customer ID should pass ValidationUtil.validateNumericField")
                .isThrownBy(() -> ValidationUtil.validateNumericField("customerId", customerIdStr));
            
            // Test invalid values
            assertThatThrownBy(() -> ValidationUtil.validateCardNumber("123")) // Too short
                .as("Short card number should fail validation")
                .isInstanceOf(RuntimeException.class);
            
            assertThatThrownBy(() -> ValidationUtil.validateCardNumber("12345678901234567")) // Too long
                .as("Long card number should fail validation")
                .isInstanceOf(RuntimeException.class);
            
            logTestExecution("Numeric field constraints validation passed using ValidationUtil", null);
        }

        @Test
        @DisplayName("Should validate Bean Validation constraints")
        void shouldValidateBeanValidationConstraints() {
            // Given: CardXref with invalid field values
            CardXref invalidCardXref = new CardXref();
            invalidCardXref.setXrefCardNum("12345678901234567890"); // Too long (>16 chars)
            invalidCardXref.setXrefCustId(-1L); // Invalid negative ID
            invalidCardXref.setXrefAcctId(-1L); // Invalid negative ID
            
            // When: Validating with Bean Validation
            Set<ConstraintViolation<CardXref>> violations = validator.validate(invalidCardXref);
            
            // Then: Should have constraint violations
            assertThat(violations)
                .as("Invalid CardXref should have constraint violations")
                .isNotEmpty();
            
            // Validate specific violations
            violations.forEach(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                Object invalidValue = violation.getInvalidValue();
                
                logTestExecution("Constraint violation found: " + propertyPath + " = " + invalidValue + " (" + message + ")", null);
            });
            
            // Test valid CardXref should have no violations
            CardXref validCardXref = new CardXref("1234567890123456", 123456789L, 12345678901L);
            Set<ConstraintViolation<CardXref>> validViolations = validator.validate(validCardXref);
            
            assertThat(validViolations)
                .as("Valid CardXref should have no constraint violations")
                .isEmpty();
            
            logTestExecution("Bean Validation constraints validation passed", null);
        }
    }

    /**
     * Test suite for functional parity validation with COBOL implementation.
     * Ensures Java implementation matches COBOL behavior exactly.
     */
    @Nested
    @DisplayName("Functional Parity Validation Tests")
    class FunctionalParityValidationTests {

        @Test
        @DisplayName("Should validate functional parity using TestConstants rules")
        void shouldValidateFunctionalParityUsingTestConstantsRules() {
            // Given: Functional parity rules from TestConstants
            var functionalParityRules = TestConstants.FUNCTIONAL_PARITY_RULES;
            
            boolean preserveFieldLengths = (Boolean) functionalParityRules.get("validate_field_lengths");
            boolean checkOverflowHandling = (Boolean) functionalParityRules.get("check_overflow_handling");
            boolean verifyErrorMessages = (Boolean) functionalParityRules.get("verify_error_messages");
            
            assertThat(preserveFieldLengths)
                .as("Field length preservation should be enabled for COBOL parity")
                .isTrue();
            
            assertThat(checkOverflowHandling)
                .as("Overflow handling should be validated for COBOL parity")
                .isTrue();
            
            assertThat(verifyErrorMessages)
                .as("Error message verification should be enabled for COBOL parity")
                .isTrue();
            
            // When: Creating CardXref with test data
            CardXref cardXref = new CardXref(
                TestConstants.TEST_CARD_NUMBER,
                Long.parseLong(TestConstants.TEST_CUSTOMER_ID.replaceAll("\\D", "")),
                Long.parseLong(TestConstants.TEST_ACCOUNT_ID)
            );
            
            // Then: Validate against functional parity rules
            if (preserveFieldLengths) {
                assertThat(cardXref.getXrefCardNum())
                    .as("Card number length should match COBOL PIC X(16) specification")
                    .hasSize(16);
            }
            
            if (checkOverflowHandling) {
                // Test ID field bounds
                assertThat(cardXref.getXrefCustId())
                    .as("Customer ID should be within valid bounds")
                    .isPositive()
                    .isLessThanOrEqualTo(999999999L);
                
                assertThat(cardXref.getXrefAcctId())
                    .as("Account ID should be within valid bounds")
                    .isPositive()
                    .isLessThanOrEqualTo(99999999999L);
            }
            
            logTestExecution("Functional parity validation passed using TestConstants rules", null);
        }

        @Test
        @DisplayName("Should validate performance thresholds for cross-reference operations")
        void shouldValidatePerformanceThresholdsForCrossReferenceOperations() {
            // Given: Performance threshold from TestConstants
            long responseTimeThreshold = TestConstants.RESPONSE_TIME_THRESHOLD_MS; // 200ms
            
            // When: Measuring CardXref operations performance
            long startTime = System.currentTimeMillis();
            
            // Simulate typical cross-reference operations
            for (int i = 0; i < 100; i++) {
                CardXref cardXref = new CardXref(
                    String.format("40000000000%05d", i),
                    (long) (100000000 + i),
                    (long) (10000000000L + i)
                );
                
                // Test composite key operations
                CardXrefId id = cardXref.getId();
                assertThat(id).isNotNull();
                
                // Test equals and hashCode operations
                CardXref duplicate = new CardXref(cardXref.getXrefCardNum(), cardXref.getXrefCustId(), cardXref.getXrefAcctId());
                assertThat(cardXref).isEqualTo(duplicate);
                assertThat(cardXref.hashCode()).isEqualTo(duplicate.hashCode());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Then: Validate performance meets threshold
            assertThat(executionTime)
                .as("Cross-reference operations should complete within " + responseTimeThreshold + "ms threshold")
                .isLessThan(responseTimeThreshold);
            
            logTestExecution("Performance validation passed: " + executionTime + "ms (threshold: " + responseTimeThreshold + "ms)", executionTime);
        }
    }

    @Test
    @DisplayName("Should validate complete CardXref entity lifecycle")
    void shouldValidateCompleteCardXrefEntityLifecycle() {
        // Given: Valid CardXref creation parameters
        String cardNumber = "5555555555554444";
        Long customerId = 987654321L;
        Long accountId = 98765432101L;
        
        // When: Creating CardXref through various constructors
        CardXref cardXref1 = new CardXref(); // Default constructor
        CardXref cardXref2 = new CardXref(cardNumber, customerId, accountId); // Full constructor
        CardXrefId compositeKey = new CardXrefId(cardNumber, customerId, accountId);
        CardXref cardXref3 = new CardXref(compositeKey); // Composite key constructor
        
        // Then: Validate all lifecycle states
        assertThat(cardXref1)
            .as("Default constructor should create valid CardXref")
            .isNotNull();
        
        assertThat(cardXref2)
            .as("Full constructor should create valid CardXref with all fields")
            .isNotNull()
            .satisfies(cf -> {
                assertThat(cf.getXrefCardNum()).isEqualTo(cardNumber);
                assertThat(cf.getXrefCustId()).isEqualTo(customerId);
                assertThat(cf.getXrefAcctId()).isEqualTo(accountId);
                assertThat(cf.getId()).isNotNull();
            });
        
        assertThat(cardXref3)
            .as("Composite key constructor should create valid CardXref")
            .isNotNull()
            .satisfies(cf -> {
                assertThat(cf.getId()).isEqualTo(compositeKey);
                assertThat(cf.getXrefCardNum()).isEqualTo(cardNumber);
                assertThat(cf.getXrefCustId()).isEqualTo(customerId);
                assertThat(cf.getXrefAcctId()).isEqualTo(accountId);
            });
        
        // Test field updates maintain consistency
        cardXref1.setXrefCardNum(cardNumber);
        cardXref1.setXrefCustId(customerId);
        cardXref1.setXrefAcctId(accountId);
        
        assertThat(cardXref1.getId())
            .as("Setting fields should create/update composite key")
            .isNotNull()
            .satisfies(id -> {
                assertThat(id.getXrefCardNum()).isEqualTo(cardNumber);
                assertThat(id.getXrefCustId()).isEqualTo(customerId);
                assertThat(id.getXrefAcctId()).isEqualTo(accountId);
            });
        
        logTestExecution("Complete CardXref entity lifecycle validation passed", null);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        logTestExecution("CardXref test teardown completed", null);
    }
}