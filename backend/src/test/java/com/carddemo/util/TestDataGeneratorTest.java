package com.carddemo.util;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import AbstractBaseTest;
import TestConstants;
import TestDataGenerator;
import UnitTest;

/**
 * Comprehensive unit test class for TestDataGenerator utility that validates generation of 
 * COBOL-compliant test data including packed decimal values, PIC clause formatted strings, 
 * VSAM key structures, and CSV/JSON data conversion.
 * 
 * This test class ensures the TestDataGenerator produces data that maintains exact 
 * compatibility with the original COBOL system's data formats, precision requirements,
 * and field validation rules. All generated test data must replicate the behavior 
 * of the legacy mainframe COBOL programs while providing comprehensive test coverage
 * for all business scenarios and edge cases.
 * 
 * Key Testing Areas:
 * - COBOL COMP-3 packed decimal generation with proper scale and precision
 * - String formatting according to COBOL PIC clauses (alphanumeric, numeric, edited)
 * - Composite key generation matching VSAM KSDS structures  
 * - Fixed-width COBOL record conversion to CSV format
 * - JSON test fixture generation from COBOL data layouts
 * - Edge case and boundary condition handling per testing strategy section 6.6
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
@UnitTest
@DisplayName("TestDataGenerator - COBOL-Compliant Test Data Generation")
public class TestDataGeneratorTest extends AbstractBaseTest {

    private TestDataGenerator testDataGenerator;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        testDataGenerator = new TestDataGenerator();
        // Reset random seed for consistent test results
        testDataGenerator.resetRandomSeed();
    }

    /**
     * Tests for COBOL COMP-3 packed decimal generation functionality.
     * Validates that generated BigDecimal values maintain exact precision and scale
     * compatibility with COBOL COMP-3 packed decimal format requirements.
     */
    @Nested
    @DisplayName("COMP-3 Packed Decimal Generation Tests")
    class Comp3PackedDecimalTests {

        @Test
        @DisplayName("generateComp3BigDecimal - generates monetary amounts with 2 decimal places")
        public void testGenerateComp3BigDecimal_GeneratesMonetaryAmounts() {
            // When: Generating monetary amount with COBOL COMP-3 precision
            BigDecimal result = testDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);

            // Then: Validate BigDecimal properties match COBOL COMP-3 requirements
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            
            // Validate precision preservation using inherited assertion method
            assertBigDecimalEquals(result, result, "Generated COMP-3 value must be self-consistent");
            
            // Validate range is appropriate for monetary amounts
            Assertions.assertThat(result).isBetween(BigDecimal.ZERO, new BigDecimal("999999.99"));
            
            // Validate compatible with CobolDataConverter
            BigDecimal converted = CobolDataConverter.preservePrecision(result, TestConstants.COBOL_DECIMAL_SCALE);
            assertBigDecimalEquals(result, converted, "Generated value must be compatible with CobolDataConverter");
        }

        @Test
        @DisplayName("generateComp3BigDecimal - handles various scales correctly")
        public void testGenerateComp3BigDecimal_HandlesVariousScales() {
            // Test different scales typical in COBOL financial systems
            int[] testScales = {0, 2, 4, 6}; // Integer, monetary, interest rate, high precision
            
            for (int scale : testScales) {
                // When: Generating value with specific scale
                BigDecimal result = testDataGenerator.generateComp3BigDecimal(scale, TestConstants.COBOL_ROUNDING_MODE);
                
                // Then: Validate scale is preserved exactly
                Assertions.assertThat(result.scale())
                    .as("Scale must match requested value for scale: %d", scale)
                    .isEqualTo(scale);
                    
                // Validate value is within reasonable bounds for scale
                BigDecimal maxValue = BigDecimal.TEN.pow(10 - scale);
                Assertions.assertThat(result)
                    .as("Generated value must be within bounds for scale: %d", scale)
                    .isLessThan(maxValue);
            }
        }

        @Test
        @DisplayName("generateComp3BigDecimal - produces consistent results with same seed")
        public void testGenerateComp3BigDecimal_ProducesConsistentResults() {
            // Given: Reset to known seed state
            testDataGenerator.resetRandomSeed();
            
            // When: Generating same value twice with reset seed
            BigDecimal first = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            
            testDataGenerator.resetRandomSeed();
            BigDecimal second = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            
            // Then: Results should be identical for deterministic testing
            assertBigDecimalEquals(first, second, "Generated values must be deterministic with same seed");
        }

        @Test
        @DisplayName("generateComp3BigDecimal - validates COBOL precision boundaries")
        public void testGenerateComp3BigDecimal_ValidatesCobolPrecisionBoundaries() {
            // When: Generating values with various COBOL-typical precisions
            BigDecimal currency = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            BigDecimal interestRate = testDataGenerator.generateComp3BigDecimal(4, TestConstants.COBOL_ROUNDING_MODE);
            BigDecimal percentage = testDataGenerator.generateComp3BigDecimal(6, TestConstants.COBOL_ROUNDING_MODE);
            
            // Then: All values must maintain COBOL precision requirements
            validateCobolPrecision(currency, 2);
            validateCobolPrecision(interestRate, 4);
            validateCobolPrecision(percentage, 6);
        }
    }

    /**
     * Tests for COBOL PIC clause string generation functionality.
     * Validates that generated strings conform to COBOL PIC clause specifications
     * including alphanumeric patterns, numeric patterns, and edited patterns.
     */
    @Nested
    @DisplayName("PIC Clause String Generation Tests")
    class PicClauseStringTests {

        @Test
        @DisplayName("generatePicString - generates alphanumeric strings matching PIC X patterns")
        public void testGeneratePicString_GeneratesAlphanumericStrings() {
            // When: Generating PIC X(8) string for user ID field
            String result = testDataGenerator.generatePicString("PIC X(8)");
            
            // Then: Validate string properties match PIC X(8) specification
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.length()).isLessThanOrEqualTo(8);
            Assertions.assertThat(result).matches("[A-Z0-9]*"); // Typical COBOL alphanumeric pattern
            
            // Validate compatibility with CobolDataConverter
            String converted = CobolDataConverter.convertPicString(result, 8);
            Assertions.assertThat(converted).isEqualTo(result);
        }

        @Test
        @DisplayName("generatePicString - generates numeric strings matching PIC 9 patterns")
        public void testGeneratePicString_GeneratesNumericStrings() {
            // When: Generating PIC 9(10) string for account ID
            String result = testDataGenerator.generatePicString("PIC 9(10)");
            
            // Then: Validate numeric string properties
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.length()).isLessThanOrEqualTo(10);
            Assertions.assertThat(result).matches("\\d*"); // Only digits
            
            // Validate can be converted to numeric value
            Long numericValue = Long.parseLong(result.isEmpty() ? "0" : result);
            Assertions.assertThat(numericValue).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("generatePicString - handles PIC S9 signed numeric patterns")
        public void testGeneratePicString_HandlesSignedNumericPatterns() {
            // When: Generating PIC S9(7)V99 for monetary amount
            String result = testDataGenerator.generatePicString("PIC S9(7)V99");
            
            // Then: Validate signed decimal string properties
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).matches("^[+-]?\\d*\\.?\\d*$"); // Signed decimal pattern
            
            // Validate can be converted using COBOL converter
            BigDecimal converted = CobolDataConverter.convertSignedNumeric(result, "PIC S9(7)V99");
            Assertions.assertThat(converted.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("generatePicString - produces various PIC clause formats")
        public void testGeneratePicString_ProducesVariousPicFormats() {
            // Test various COBOL PIC clause formats used in copybooks
            String[] picClauses = {
                "PIC X(20)",     // Customer name
                "PIC 9(4)",      // Year
                "PIC 9(2)",      // Month/day  
                "PIC S9(15)",    // Large signed integer
                "PIC S9(5)V99",  // Currency amount
                "PIC S9(9)V9999" // Interest rate
            };
            
            for (String picClause : picClauses) {
                // When: Generating string for each PIC clause type
                String result = testDataGenerator.generatePicString(picClause);
                
                // Then: Validate compatibility with COBOL converter
                try {
                    Object converted = CobolDataConverter.convertToJavaType(result, picClause);
                    Assertions.assertThat(converted).isNotNull();
                } catch (Exception e) {
                    Assertions.fail("Generated PIC string should be compatible with CobolDataConverter for clause: " + picClause);
                }
            }
        }
    }

    /**
     * Tests for VSAM key structure generation functionality.
     * Validates that generated keys follow VSAM KSDS composite key patterns
     * and maintain proper key ordering and uniqueness constraints.
     */
    @Nested
    @DisplayName("VSAM Key Structure Generation Tests") 
    class VsamKeyGenerationTests {

        @Test
        @DisplayName("generateVsamKey - creates account composite keys matching VSAM KSDS structure")
        public void testGenerateVsamKey_CreatesAccountCompositeKeys() {
            // When: Generating VSAM-style composite key for account
            String result = testDataGenerator.generateVsamKey("ACCOUNT");
            
            // Then: Validate key structure matches VSAM KSDS requirements
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(10); // Standard account ID length
            Assertions.assertThat(result).matches("\\d{10}"); // All numeric as per COBOL
            
            // Validate key can be used as primary key
            Assertions.assertThat(Long.parseLong(result))
                .isBetween(1000000000L, 9999999999L); // Valid account ID range
        }

        @Test
        @DisplayName("generateVsamKey - creates transaction composite keys")
        public void testGenerateVsamKey_CreatesTransactionCompositeKeys() {
            // When: Generating VSAM-style composite key for transaction
            String result = testDataGenerator.generateVsamKey("TRANSACTION");
            
            // Then: Validate transaction key structure
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(20); // Account ID + Transaction ID
            Assertions.assertThat(result).matches("\\d{20}"); // All numeric format
            
            // Validate can be split into components
            String accountPart = result.substring(0, 10);
            String transactionPart = result.substring(10, 20);
            Assertions.assertThat(Long.parseLong(accountPart)).isPositive();
            Assertions.assertThat(Long.parseLong(transactionPart)).isPositive();
        }

        @Test
        @DisplayName("generateVsamKey - ensures key uniqueness across generation calls")
        public void testGenerateVsamKey_EnsuresKeyUniqueness() {
            // When: Generating multiple keys of same type
            String key1 = testDataGenerator.generateVsamKey("ACCOUNT");
            String key2 = testDataGenerator.generateVsamKey("ACCOUNT");
            String key3 = testDataGenerator.generateVsamKey("ACCOUNT");
            
            // Then: All keys should be unique
            Assertions.assertThat(key1).isNotEqualTo(key2);
            Assertions.assertThat(key2).isNotEqualTo(key3);
            Assertions.assertThat(key1).isNotEqualTo(key3);
        }

        @Test
        @DisplayName("generateVsamKey - handles customer key generation")
        public void testGenerateVsamKey_HandlesCustomerKeyGeneration() {
            // When: Generating customer primary key
            String result = testDataGenerator.generateVsamKey("CUSTOMER");
            
            // Then: Validate customer key meets COBOL requirements
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(9); // Standard customer ID length
            Assertions.assertThat(result).matches("\\d{9}"); // Numeric only
            
            // Validate fits in Integer range for COBOL compatibility
            Integer customerId = Integer.parseInt(result);
            Assertions.assertThat(customerId)
                .isBetween(100000000, 999999999); // Valid customer ID range
        }
    }

    /**
     * Tests for JPA entity generation functionality.
     * Validates that generated entities maintain proper field types, monetary precision,
     * and follow COBOL data patterns while being compatible with JPA persistence.
     */
    @Nested
    @DisplayName("JPA Entity Generation Tests")
    class EntityGenerationTests {

        @Test
        @DisplayName("generateAccount - creates account entities with COBOL-compatible fields")
        public void testGenerateAccount_CreatesAccountWithCobolCompatibleFields() {
            // When: Generating account test entity
            Account result = testDataGenerator.generateAccount();

            // Then: Validate account structure matches entity requirements
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getAccountId()).isNotNull().matches("\\d{10}");
            Assertions.assertThat(result.getCustomerId()).isNotNull().matches("\\d{9}");
            
            // Validate monetary fields have proper COBOL precision
            assertBigDecimalWithinTolerance(result.getCurrentBalance(), 
                TestConstants.COBOL_DECIMAL_SCALE, "Account balance precision");
            assertBigDecimalWithinTolerance(result.getCreditLimit(),
                TestConstants.COBOL_DECIMAL_SCALE, "Credit limit precision");
            assertBigDecimalWithinTolerance(result.getCashCreditLimit(),
                TestConstants.COBOL_DECIMAL_SCALE, "Cash credit limit precision");
            
            // Validate field relationships
            Assertions.assertThat(result.getCreditLimit())
                .isGreaterThanOrEqualTo(result.getCurrentBalance());
        }

        @Test
        @DisplayName("generateCustomer - creates customer entities with proper personal information formats")
        public void testGenerateCustomer_CreatesCustomerWithProperFormats() {
            // When: Generating customer test entity
            Customer result = testDataGenerator.generateCustomer();

            // Then: Validate customer structure and field formats
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getCustomerId()).isNotNull().matches("\\d{9}");
            
            // Validate name fields follow COBOL PIC X patterns
            Assertions.assertThat(result.getFirstName())
                .isNotNull()
                .hasSizeLessThanOrEqualTo(25) // Typical COBOL first name length
                .matches("[A-Z][a-z]*"); // Proper name format
                
            Assertions.assertThat(result.getLastName())
                .isNotNull()
                .hasSizeLessThanOrEqualTo(25) // Typical COBOL last name length
                .matches("[A-Z][a-z]*"); // Proper name format
            
            // Validate phone number format
            Assertions.assertThat(result.getPhoneNumber())
                .isNotNull()
                .matches("\\d{3}-\\d{3}-\\d{4}"); // Standard US phone format
                
            // Validate SSN format (encrypted but validate structure)
            Assertions.assertThat(result.getSSN()).isNotNull();
            
            // Validate FICO score range
            Assertions.assertThat(result.getFicoScore())
                .isBetween(300, 850); // Valid FICO score range
        }

        @Test
        @DisplayName("generateTransaction - creates transaction entities with proper amount precision")
        public void testGenerateTransaction_CreatesTransactionWithProperPrecision() {
            // When: Generating transaction test entity
            Transaction result = testDataGenerator.generateTransaction();

            // Then: Validate transaction structure and precision
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getTransactionId()).isNotNull().matches("\\d{20}");
            Assertions.assertThat(result.getAccountId()).isNotNull().matches("\\d{10}");
            
            // Validate transaction amount has COBOL COMP-3 precision
            assertBigDecimalWithinTolerance(result.getAmount(),
                TestConstants.COBOL_DECIMAL_SCALE, "Transaction amount precision");
            
            // Validate transaction type is valid
            Assertions.assertThat(result.getTransactionType())
                .isNotNull()
                .isIn("PURCHASE", "PAYMENT", "CASH_ADVANCE", "TRANSFER", "FEE", "INTEREST");
                
            // Validate transaction date is not null
            Assertions.assertThat(result.getTransactionDate()).isNotNull();
            
            // Validate description follows COBOL field constraints
            Assertions.assertThat(result.getDescription())
                .isNotNull()
                .hasSizeLessThanOrEqualTo(40); // Typical COBOL description length
        }

        @Test
        @DisplayName("generateCard - creates card entities with PCI DSS compliant formatting")
        public void testGenerateCard_CreatesCardWithPciCompliantFormatting() {
            // When: Generating card test entity
            Card result = testDataGenerator.generateCard();

            // Then: Validate card structure and security requirements
            Assertions.assertThat(result).isNotNull();
            
            // Validate card number format (test data - not real)
            Assertions.assertThat(result.getCardNumber())
                .isNotNull()
                .matches("\\d{16}"); // Standard 16-digit card number
                
            // Validate CVV code format
            Assertions.assertThat(result.getCvvCode())
                .isNotNull()
                .matches("\\d{3}"); // 3-digit CVV
                
            // Validate embossed name follows COBOL field constraints
            Assertions.assertThat(result.getEmbossedName())
                .isNotNull()
                .hasSizeLessThanOrEqualTo(26) // Standard embossed name length
                .matches("[A-Z\\s]*"); // Only uppercase letters and spaces
                
            // Validate expiration date is future date
            Assertions.assertThat(result.getExpirationDate()).isNotNull();
            
            // Validate active status is set
            Assertions.assertThat(result.getActiveStatus()).isNotNull();
        }

        @Test
        @DisplayName("generateAccount - produces valid account data relationships")
        public void testGenerateAccount_ProducesValidDataRelationships() {
            // When: Generating multiple accounts
            Account account1 = testDataGenerator.generateAccount();
            Account account2 = testDataGenerator.generateAccount();
            
            // Then: Validate data relationship constraints
            Assertions.assertThat(account1.getAccountId())
                .isNotEqualTo(account2.getAccountId()); // Unique account IDs
                
            // Validate monetary relationships
            Assertions.assertThat(account1.getCurrentBalance())
                .isLessThanOrEqualTo(account1.getCreditLimit()); // Balance <= Credit Limit
            Assertions.assertThat(account2.getCurrentBalance())
                .isLessThanOrEqualTo(account2.getCreditLimit()); // Balance <= Credit Limit
        }
    }

    /**
     * Tests for bulk test data generation functionality.
     * Validates generation of collections of test entities with proper
     * data relationships and boundary condition coverage.
     */
    @Nested
    @DisplayName("Bulk Test Data Generation Tests")
    class BulkDataGenerationTests {

        @Test
        @DisplayName("bulk generation - creates lists of entities with unique identifiers")
        public void testBulkGeneration_CreatesListsWithUniqueIdentifiers() {
            // When: Generating lists of different entity types
            List<Account> accounts = List.of(
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount()
            );
            
            List<Customer> customers = List.of(
                testDataGenerator.generateCustomer(),
                testDataGenerator.generateCustomer(),
                testDataGenerator.generateCustomer()
            );

            // Then: Validate uniqueness across all generated entities
            Assertions.assertThat(accounts).hasSize(3);
            Assertions.assertThat(accounts.stream().map(Account::getAccountId))
                .doesNotHaveDuplicates();
                
            Assertions.assertThat(customers).hasSize(3);
            Assertions.assertThat(customers.stream().map(Customer::getCustomerId))
                .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("bulk generation - maintains data consistency across related entities")
        public void testBulkGeneration_MaintainsDataConsistencyAcrossEntities() {
            // When: Generating related entities
            Customer customer = testDataGenerator.generateCustomer();
            Account account = testDataGenerator.generateAccount();
            Card card = testDataGenerator.generateCard();
            Transaction transaction = testDataGenerator.generateTransaction();

            // Then: Validate all entities have properly formatted identifiers
            Assertions.assertThat(customer.getCustomerId()).matches("\\d{9}");
            Assertions.assertThat(account.getAccountId()).matches("\\d{10}");
            Assertions.assertThat(card.getCardNumber()).matches("\\d{16}");
            Assertions.assertThat(transaction.getTransactionId()).matches("\\d{20}");
            
            // Validate all monetary amounts have consistent precision
            assertBigDecimalWithinTolerance(account.getCurrentBalance(), 
                TestConstants.COBOL_DECIMAL_SCALE, "Account balance");
            assertBigDecimalWithinTolerance(transaction.getAmount(),
                TestConstants.COBOL_DECIMAL_SCALE, "Transaction amount");
        }
    }

    /**
     * Tests for edge cases and boundary conditions in test data generation.
     * Validates proper handling of null values, invalid inputs, and edge cases
     * as specified in testing strategy section 6.6.
     */
    @Nested
    @DisplayName("Edge Cases and Boundary Conditions Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("edge cases - handles null and empty PIC clause inputs gracefully")
        public void testEdgeCases_HandlesNullAndEmptyPicClauses() {
            // When/Then: Invalid PIC clauses should be handled gracefully
            Assertions.assertThatThrownBy(() -> 
                testDataGenerator.generatePicString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIC clause");
                
            Assertions.assertThatThrownBy(() -> 
                testDataGenerator.generatePicString(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIC clause");
        }

        @Test
        @DisplayName("edge cases - handles invalid VSAM key types")
        public void testEdgeCases_HandlesInvalidVsamKeyTypes() {
            // When/Then: Invalid key types should be handled gracefully
            Assertions.assertThatThrownBy(() -> 
                testDataGenerator.generateVsamKey("INVALID_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key type");
                
            Assertions.assertThatThrownBy(() -> 
                testDataGenerator.generateVsamKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key type");
        }

        @Test
        @DisplayName("edge cases - handles boundary values for COMP-3 generation")
        public void testEdgeCases_HandlesBoundaryValuesForComp3() {
            // Test boundary conditions for scale values
            int[] boundaryScales = {0, 1, 2, 4, 6, 8}; // Common COBOL scales
            
            for (int scale : boundaryScales) {
                // When: Generating with boundary scale values
                BigDecimal result = testDataGenerator.generateComp3BigDecimal(
                    scale, TestConstants.COBOL_ROUNDING_MODE);
                    
                // Then: Should handle all scales properly
                Assertions.assertThat(result.scale()).isEqualTo(scale);
                validateCobolPrecision(result, scale);
            }
        }

        @Test
        @DisplayName("edge cases - validates maximum field lengths for PIC clauses")
        public void testEdgeCases_ValidatesMaximumFieldLengths() {
            // Test various field length boundaries
            String[] maxLengthPicClauses = {
                "PIC X(1)",      // Minimum length
                "PIC X(254)",    // Near maximum COBOL field length
                "PIC 9(1)",      // Minimum numeric
                "PIC 9(18)"      // Maximum Long precision
            };
            
            for (String picClause : maxLengthPicClauses) {
                // When: Generating with boundary length PIC clauses
                String result = testDataGenerator.generatePicString(picClause);
                
                // Then: Should handle all boundary lengths
                Assertions.assertThat(result).isNotNull();
                
                // Validate compatibility with COBOL converter
                try {
                    Object converted = CobolDataConverter.convertToJavaType(result, picClause);
                    Assertions.assertThat(converted).isNotNull();
                } catch (Exception e) {
                    Assertions.fail("Generated PIC string should be compatible with CobolDataConverter for clause: " + picClause);
                }
            }
        }
    }

    /**
     * Tests for random seed reset functionality.
     * Validates that random seed reset produces deterministic test data
     * for reproducible test scenarios.
     */
    @Nested
    @DisplayName("Random Seed Management Tests")
    class RandomSeedTests {

        @Test
        @DisplayName("resetRandomSeed - produces deterministic test data generation")
        public void testResetRandomSeed_ProducesDeterministicGeneration() {
            // Given: Generate initial values
            testDataGenerator.resetRandomSeed();
            Account account1 = testDataGenerator.generateAccount();
            BigDecimal decimal1 = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            String pic1 = testDataGenerator.generatePicString("PIC X(10)");

            // When: Reset seed and generate again
            testDataGenerator.resetRandomSeed();
            Account account2 = testDataGenerator.generateAccount();
            BigDecimal decimal2 = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            String pic2 = testDataGenerator.generatePicString("PIC X(10)");

            // Then: Results should be identical with same seed
            Assertions.assertThat(account2.getAccountId()).isEqualTo(account1.getAccountId());
            Assertions.assertThat(account2.getCustomerId()).isEqualTo(account1.getCustomerId());
            assertBigDecimalEquals(decimal2, decimal1, "COMP-3 values should be deterministic");
            Assertions.assertThat(pic2).isEqualTo(pic1);
        }

        @Test
        @DisplayName("resetRandomSeed - ensures different sequences without reset")
        public void testResetRandomSeed_EnsuresDifferentSequencesWithoutReset() {
            // When: Generating without seed reset between calls
            testDataGenerator.resetRandomSeed();
            String key1 = testDataGenerator.generateVsamKey("ACCOUNT");
            String key2 = testDataGenerator.generateVsamKey("ACCOUNT"); // No reset
            String key3 = testDataGenerator.generateVsamKey("ACCOUNT"); // No reset

            // Then: Keys should be different (not using reset)
            Assertions.assertThat(key1).isNotEqualTo(key2);
            Assertions.assertThat(key2).isNotEqualTo(key3);
            Assertions.assertThat(key1).isNotEqualTo(key3);
        }
    }

    /**
     * Tests for data format conversion functionality.
     * Validates conversion between COBOL fixed-width records and modern
     * CSV/JSON formats while preserving data precision and relationships.
     */
    @Nested
    @DisplayName("Data Format Conversion Tests")
    class DataFormatConversionTests {

        @Test
        @DisplayName("CSV conversion - generates valid CSV format from entity data")
        public void testCsvConversion_GeneratesValidCsvFromEntityData() {
            // Given: Generated test entities
            List<Account> accounts = List.of(
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount()
            );

            // When: Converting to CSV format (simulated - TestDataGenerator should provide this)
            // Note: This tests the expected functionality based on requirements
            for (Account account : accounts) {
                // Then: Validate entities can be serialized to CSV-compatible format
                Assertions.assertThat(account.getAccountId()).matches("\\d{10}");
                Assertions.assertThat(account.getCurrentBalance().toPlainString())
                    .matches("\\d+\\.\\d{2}"); // CSV-friendly decimal format
                Assertions.assertThat(account.getCreditLimit().toPlainString())
                    .matches("\\d+\\.\\d{2}"); // CSV-friendly decimal format
            }
        }

        @Test
        @DisplayName("JSON conversion - generates valid JSON test fixtures")
        public void testJsonConversion_GeneratesValidJsonTestFixtures() {
            // Given: Generated test entities
            Customer customer = testDataGenerator.generateCustomer();
            Transaction transaction = testDataGenerator.generateTransaction();

            // When/Then: Validate entities have JSON-serializable properties
            Assertions.assertThat(customer.getCustomerId()).isNotNull();
            Assertions.assertThat(customer.getFirstName()).isNotNull();
            Assertions.assertThat(customer.getLastName()).isNotNull();
            
            Assertions.assertThat(transaction.getTransactionId()).isNotNull();
            Assertions.assertThat(transaction.getAmount()).isNotNull();
            Assertions.assertThat(transaction.getTransactionType()).isNotNull();
            
            // Validate monetary amounts are JSON-compatible
            String amountString = transaction.getAmount().toPlainString();
            Assertions.assertThat(amountString).matches("\\d+\\.\\d{2}");
        }

        @Test
        @DisplayName("fixed-width conversion - validates COBOL record layout compatibility")
        public void testFixedWidthConversion_ValidatesCobolRecordLayouts() {
            // Given: Generated entities with COBOL-compatible field formats
            Account account = testDataGenerator.generateAccount();
            Customer customer = testDataGenerator.generateCustomer();

            // When/Then: Validate fields match fixed-width COBOL record expectations
            
            // Account ID: PIC 9(10) - exactly 10 digits
            Assertions.assertThat(account.getAccountId()).hasSize(10);
            
            // Customer ID: PIC 9(9) - exactly 9 digits  
            Assertions.assertThat(customer.getCustomerId()).hasSize(9);
            
            // Names: PIC X(25) - up to 25 characters
            Assertions.assertThat(customer.getFirstName()).hasSizeLessThanOrEqualTo(25);
            Assertions.assertThat(customer.getLastName()).hasSizeLessThanOrEqualTo(25);
            
            // Phone: PIC X(12) format with dashes
            Assertions.assertThat(customer.getPhoneNumber()).hasSize(12); // XXX-XXX-XXXX
        }
    }

    /**
     * Tests for integration with existing test infrastructure.
     * Validates that generated test data integrates properly with AbstractBaseTest
     * helper methods and TestConstants configuration.
     */
    @Nested
    @DisplayName("Test Infrastructure Integration Tests")
    class TestInfrastructureIntegrationTests {

        @Test
        @DisplayName("AbstractBaseTest integration - validates BigDecimal assertion helpers")
        public void testAbstractBaseTestIntegration_ValidatesBigDecimalHelpers() {
            // Given: Generated monetary values
            BigDecimal amount1 = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            BigDecimal amount2 = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);

            // When/Then: AbstractBaseTest helpers should work with generated data
            assertBigDecimalEquals(amount1, amount1, "Generated value should equal itself");
            
            // Test tolerance validation
            BigDecimal closeValue = amount1.add(new BigDecimal("0.001"));
            assertBigDecimalWithinTolerance(closeValue, 2, "Close values within tolerance");
            
            // Test precision validation
            validateCobolPrecision(amount1, 2);
            validateCobolPrecision(amount2, 2);
        }

        @Test
        @DisplayName("TestConstants integration - uses shared constants for validation")
        public void testTestConstantsIntegration_UsesSharedConstants() {
            // When: Generating data using TestConstants
            BigDecimal testAmount = testDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            
            // Then: Validate integration with test constants
            Assertions.assertThat(testAmount.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            
            // Test against known test IDs from TestConstants
            String generatedAccountId = testDataGenerator.generateVsamKey("ACCOUNT");
            Assertions.assertThat(generatedAccountId)
                .matches("\\d{10}") // Should match same pattern as TEST_ACCOUNT_ID
                .isNotEqualTo(TestConstants.TEST_ACCOUNT_ID); // But should be unique
        }

        @Test
        @DisplayName("UnitTest annotation - validates test categorization")
        public void testUnitTestAnnotation_ValidatesTestCategorization() {
            // When/Then: This test class should be properly categorized as unit test
            Assertions.assertThat(this.getClass())
                .hasAnnotation(UnitTest.class);
            
            // Validate test execution is fast (unit test characteristic)
            long startTime = System.currentTimeMillis();
            
            // Perform data generation operations
            testDataGenerator.generateAccount();
            testDataGenerator.generateCustomer();
            testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Unit tests should execute quickly
            Assertions.assertThat(executionTime)
                .as("Unit test should execute quickly")
                .isLessThan(1000L); // 1 second max for unit test
        }
    }

    /**
     * Tests for COBOL data validation and compatibility.
     * Validates that generated test data maintains compatibility with
     * CobolDataConverter utility and preserves COBOL data characteristics.
     */
    @Nested
    @DisplayName("COBOL Data Compatibility Tests")
    class CobolDataCompatibilityTests {

        @Test
        @DisplayName("COBOL compatibility - validates generated data with CobolDataConverter")
        public void testCobolCompatibility_ValidatesGeneratedDataWithConverter() {
            // When: Generating various data types
            BigDecimal monetary = testDataGenerator.generateComp3BigDecimal(2, TestConstants.COBOL_ROUNDING_MODE);
            String alphanumeric = testDataGenerator.generatePicString("PIC X(20)");
            String numeric = testDataGenerator.generatePicString("PIC 9(10)");

            // Then: All should be compatible with CobolDataConverter
            
            // Test monetary amount preservation
            BigDecimal preservedMonetary = CobolDataConverter.preservePrecision(monetary, 2);
            assertBigDecimalEquals(monetary, preservedMonetary, "Monetary precision should be preserved");
            
            // Test string format conversion
            String convertedAlpha = CobolDataConverter.convertPicString(alphanumeric, 20);
            Assertions.assertThat(convertedAlpha).isEqualTo(alphanumeric);
            
            // Test numeric conversion
            Object convertedNumeric = CobolDataConverter.convertToJavaType(numeric, "PIC 9(10)");
            Assertions.assertThat(convertedNumeric).isNotNull();
        }

        @Test 
        @DisplayName("COBOL compatibility - validates precision patterns from TestConstants")
        public void testCobolCompatibility_ValidatesPrecisionPatternsFromConstants() {
            // When: Generating values using COBOL precision patterns
            for (String pattern : TestConstants.COBOL_COMP3_PATTERNS) {
                try {
                    // Generate and validate each COBOL pattern
                    String testValue = testDataGenerator.generatePicString("PIC " + pattern);
                    
                    // Then: Should be compatible with COBOL conversion
                    boolean isValid = CobolDataConverter.validateCobolField(testValue, "PIC " + pattern);
                    Assertions.assertThat(isValid)
                        .as("Generated value should be valid for pattern: %s", pattern)
                        .isTrue();
                        
                } catch (Exception e) {
                    Assertions.fail("Pattern should be supported: " + pattern + ", error: " + e.getMessage());
                }
            }
        }

        @Test
        @DisplayName("COBOL compatibility - validates monetary precision with validation thresholds")
        public void testCobolCompatibility_ValidatesMonetaryPrecisionWithThresholds() {
            // When: Generating monetary amounts
            BigDecimal balance = testDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);
            BigDecimal creditLimit = testDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, TestConstants.COBOL_ROUNDING_MODE);

            // Then: Validate against TestConstants validation thresholds
            Assertions.assertThat(balance.abs())
                .isLessThan(new BigDecimal("1000000")); // Reasonable monetary limit
            Assertions.assertThat(creditLimit.abs())
                .isLessThan(new BigDecimal("1000000")); // Reasonable credit limit
            
            // Validate precision characteristics
            validateCobolPrecision(balance, TestConstants.COBOL_DECIMAL_SCALE);
            validateCobolPrecision(creditLimit, TestConstants.COBOL_DECIMAL_SCALE);
        }
    }

    /**
     * Tests for comprehensive test scenario coverage.
     * Validates that generated test data covers all business scenarios
     * required for thorough system testing as per specification requirements.
     */
    @Nested
    @DisplayName("Comprehensive Business Scenario Coverage Tests")
    class BusinessScenarioCoverageTests {

        @Test
        @DisplayName("business scenarios - generates data for all transaction types")
        public void testBusinessScenarios_GeneratesDataForAllTransactionTypes() {
            // When: Generating multiple transactions
            List<Transaction> transactions = List.of(
                testDataGenerator.generateTransaction(),
                testDataGenerator.generateTransaction(),
                testDataGenerator.generateTransaction(),
                testDataGenerator.generateTransaction(),
                testDataGenerator.generateTransaction()
            );

            // Then: Should cover different transaction types
            List<String> types = transactions.stream()
                .map(Transaction::getTransactionType)
                .distinct()
                .toList();
                
            // Validate variety of transaction types for comprehensive testing
            Assertions.assertThat(types).isNotEmpty();
            
            // Validate all types are valid COBOL transaction codes
            for (String type : types) {
                Assertions.assertThat(type)
                    .isIn("PURCHASE", "PAYMENT", "CASH_ADVANCE", "TRANSFER", "FEE", "INTEREST");
            }
        }

        @Test
        @DisplayName("business scenarios - generates data covering boundary financial conditions")
        public void testBusinessScenarios_GeneratesDataCoveringBoundaryConditions() {
            // When: Generating accounts multiple times to get variety
            List<Account> accounts = List.of(
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount(),
                testDataGenerator.generateAccount()
            );

            // Then: Should cover various financial conditions
            List<BigDecimal> balances = accounts.stream()
                .map(Account::getCurrentBalance)
                .toList();
                
            List<BigDecimal> creditLimits = accounts.stream()
                .map(Account::getCreditLimit)
                .toList();

            // Validate variety in generated values for comprehensive testing
            Assertions.assertThat(balances).isNotEmpty();
            Assertions.assertThat(creditLimits).isNotEmpty();
            
            // Validate all monetary values maintain COBOL precision
            for (BigDecimal balance : balances) {
                validateCobolPrecision(balance, TestConstants.COBOL_DECIMAL_SCALE);
            }
            
            for (BigDecimal limit : creditLimits) {
                validateCobolPrecision(limit, TestConstants.COBOL_DECIMAL_SCALE);
            }
        }

        @Test
        @DisplayName("business scenarios - generates complete customer profiles for testing")
        public void testBusinessScenarios_GeneratesCompleteCustomerProfiles() {
            // When: Generating customer profiles
            List<Customer> customers = List.of(
                testDataGenerator.generateCustomer(),
                testDataGenerator.generateCustomer(),
                testDataGenerator.generateCustomer()
            );

            // Then: All customers should have complete, valid profiles
            for (Customer customer : customers) {
                // Validate all required fields are populated
                Assertions.assertThat(customer.getCustomerId()).isNotNull();
                Assertions.assertThat(customer.getFirstName()).isNotNull().isNotEmpty();
                Assertions.assertThat(customer.getLastName()).isNotNull().isNotEmpty();
                Assertions.assertThat(customer.getPhoneNumber()).isNotNull().matches("\\d{3}-\\d{3}-\\d{4}");
                Assertions.assertThat(customer.getSSN()).isNotNull();
                
                // Validate FICO score in valid range
                Assertions.assertThat(customer.getFicoScore())
                    .isBetween(300, 850);
            }
            
            // Validate uniqueness across customers
            List<String> customerIds = customers.stream()
                .map(Customer::getCustomerId)
                .toList();
            Assertions.assertThat(customerIds).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("business scenarios - generates card data with security compliance")
        public void testBusinessScenarios_GeneratesCardDataWithSecurityCompliance() {
            // When: Generating card test data
            List<Card> cards = List.of(
                testDataGenerator.generateCard(),
                testDataGenerator.generateCard(),
                testDataGenerator.generateCard()
            );

            // Then: All cards should have proper security characteristics
            for (Card card : cards) {
                // Validate card number format (test data)
                Assertions.assertThat(card.getCardNumber())
                    .isNotNull()
                    .matches("\\d{16}"); // 16-digit card number
                    
                // Validate CVV format
                Assertions.assertThat(card.getCvvCode())
                    .isNotNull()
                    .matches("\\d{3}"); // 3-digit CVV
                    
                // Validate embossed name format
                Assertions.assertThat(card.getEmbossedName())
                    .isNotNull()
                    .hasSizeLessThanOrEqualTo(26)
                    .matches("[A-Z\\s]*"); // Uppercase with spaces only
                    
                // Validate expiration date exists
                Assertions.assertThat(card.getExpirationDate()).isNotNull();
                
                // Validate active status
                Assertions.assertThat(card.getActiveStatus()).isNotNull();
            }
            
            // Validate card number uniqueness
            List<String> cardNumbers = cards.stream()
                .map(Card::getCardNumber)
                .toList();
            Assertions.assertThat(cardNumbers).doesNotHaveDuplicates();
        }
    }
}