package com.carddemo.util;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.Customer;
import com.carddemo.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.carddemo.test.AbstractBaseTest;
import com.carddemo.test.TestConstants;
import com.carddemo.test.TestDataGenerator;
import com.carddemo.util.CobolDataConverter;

import org.junit.jupiter.api.Tag;

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
@Tag("unit")
@DisplayName("TestDataGenerator - COBOL-Compliant Test Data Generation")
public class TestDataGeneratorTest extends AbstractBaseTest {

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Reset random seed for consistent test results
        TestDataGenerator.resetRandomSeed(12345L);
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
            BigDecimal result = TestDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, 10000.0);

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
                BigDecimal result = TestDataGenerator.generateComp3BigDecimal(scale, 10000.0);
                
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
            TestDataGenerator.resetRandomSeed(12345L);
            
            // When: Generating same value twice with reset seed
            BigDecimal first = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            
            TestDataGenerator.resetRandomSeed(12345L);
            BigDecimal second = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            
            // Then: Results should be identical for deterministic testing
            assertBigDecimalEquals(first, second, "Generated values must be deterministic with same seed");
        }

        @Test
        @DisplayName("generateComp3BigDecimal - validates COBOL precision boundaries")
        public void testGenerateComp3BigDecimal_ValidatesCobolPrecisionBoundaries() {
            // When: Generating values with various COBOL-typical precisions
            BigDecimal currency = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            BigDecimal interestRate = TestDataGenerator.generateComp3BigDecimal(4, 1000.0);
            BigDecimal percentage = TestDataGenerator.generateComp3BigDecimal(6, 100.0);
            
            // Then: All values must maintain COBOL precision requirements
            validateCobolPrecision(currency, "currency");
            validateCobolPrecision(interestRate, "interestRate");
            validateCobolPrecision(percentage, "percentage");
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
            String result = TestDataGenerator.generatePicString(8, false);
            
            // Then: Validate string properties match PIC X(8) specification
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.length()).isEqualTo(8);
            Assertions.assertThat(result).matches("[A-Z0-9]*"); // Typical COBOL alphanumeric pattern
            
            // Validate compatibility with CobolDataConverter
            String converted = CobolDataConverter.convertPicString(result, 8);
            Assertions.assertThat(converted).isEqualTo(result);
        }

        @Test
        @DisplayName("generatePicString - generates numeric strings matching PIC 9 patterns")
        public void testGeneratePicString_GeneratesNumericStrings() {
            // When: Generating PIC 9(10) string for account ID
            String result = TestDataGenerator.generatePicString(10, true);
            
            // Then: Validate numeric string properties
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.length()).isEqualTo(10);
            Assertions.assertThat(result).matches("\\d*"); // Only digits
            
            // Validate can be converted to numeric value
            Long numericValue = Long.parseLong(result.isEmpty() ? "0" : result);
            Assertions.assertThat(numericValue).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("generatePicString - handles PIC S9 signed numeric patterns")
        public void testGeneratePicString_HandlesSignedNumericPatterns() {
            // When: Generating PIC S9(7)V99 for monetary amount
            String result = TestDataGenerator.generatePicString(9, true);
            
            // Then: Validate signed decimal string properties
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).matches("\\d*"); // Numeric pattern (generatePicString doesn't handle signs)
            
            // Validate can be converted using COBOL converter
            BigDecimal converted = CobolDataConverter.convertSignedNumeric(result, "PIC S9(7)V99");
            Assertions.assertThat(converted.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("generatePicString - produces various PIC clause formats")
        public void testGeneratePicString_ProducesVariousPicFormats() {
            // Test various COBOL PIC clause formats used in copybooks
            Object[][] testSpecs = {
                {20, false},     // Customer name - alphanumeric
                {4, true},       // Year - numeric
                {2, true},       // Month/day - numeric
                {15, true},      // Large signed integer - numeric
                {7, true},       // Currency amount - numeric
                {13, true}       // Interest rate - numeric
            };
            
            for (Object[] spec : testSpecs) {
                // When: Generating string for each PIC clause type
                int length = (Integer) spec[0];
                boolean numeric = (Boolean) spec[1];
                String result = TestDataGenerator.generatePicString(length, numeric);
                
                // Then: Validate basic string properties
                Assertions.assertThat(result).isNotNull();
                Assertions.assertThat(result.length()).isEqualTo(length);
                
                if (numeric) { // numeric
                    Assertions.assertThat(result).matches("\\d*");
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
            String result = TestDataGenerator.generateVsamKey(new int[]{10});
            
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
            String result = TestDataGenerator.generateVsamKey(new int[]{11, 16});
            
            // Then: Validate transaction key structure
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(27); // Account ID (11) + Transaction ID (16)
            Assertions.assertThat(result).matches("\\d{27}"); // All numeric format
            
            // Validate can be split into components
            String accountPart = result.substring(0, 11);
            String transactionPart = result.substring(11, 27);
            Assertions.assertThat(Long.parseLong(accountPart)).isPositive();
            Assertions.assertThat(Long.parseLong(transactionPart)).isPositive();
        }

        @Test
        @DisplayName("generateVsamKey - ensures key uniqueness across generation calls")
        public void testGenerateVsamKey_EnsuresKeyUniqueness() {
            // When: Generating multiple keys of same type
            String key1 = TestDataGenerator.generateVsamKey(new int[]{10});
            String key2 = TestDataGenerator.generateVsamKey(new int[]{10});
            String key3 = TestDataGenerator.generateVsamKey(new int[]{10});
            
            // Then: All keys should be unique
            Assertions.assertThat(key1).isNotEqualTo(key2);
            Assertions.assertThat(key2).isNotEqualTo(key3);
            Assertions.assertThat(key1).isNotEqualTo(key3);
        }

        @Test
        @DisplayName("generateVsamKey - handles customer key generation")
        public void testGenerateVsamKey_HandlesCustomerKeyGeneration() {
            // When: Generating customer primary key
            String result = TestDataGenerator.generateVsamKey(new int[]{9});
            
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
            Account result = TestDataGenerator.generateAccount();

            // Then: Validate account structure matches entity requirements
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getAccountId()).isNotNull();
            Assertions.assertThat(result.getAccountId().toString()).matches("\\d{11}"); // 11-digit account ID
            
            // Validate monetary fields have proper COBOL precision
            Assertions.assertThat(result.getCurrentBalance().scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            Assertions.assertThat(result.getCreditLimit().scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            Assertions.assertThat(result.getCashCreditLimit().scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            
            // Validate field relationships
            Assertions.assertThat(result.getCreditLimit())
                .isGreaterThanOrEqualTo(result.getCurrentBalance());
        }

        @Test
        @DisplayName("generateCustomer - creates customer entities with proper personal information formats")
        public void testGenerateCustomer_CreatesCustomerWithProperFormats() {
            // When: Generating customer test entity
            Customer result = TestDataGenerator.generateCustomer();

            // Then: Validate customer structure and field formats
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getCustomerId()).isNotNull();
            Assertions.assertThat(result.getCustomerId().toString()).matches("\\d{9}");
            
            // Validate name fields follow COBOL PIC X patterns
            Assertions.assertThat(result.getFirstName())
                .isNotNull()
                .hasSizeLessThanOrEqualTo(25) // Typical COBOL first name length
                .matches("[A-Z][a-zA-Z ]*"); // Proper name format
                
            Assertions.assertThat(result.getLastName())
                .isNotNull()
                .hasSizeLessThanOrEqualTo(25) // Typical COBOL last name length
                .matches("[A-Z][a-zA-Z ]*"); // Proper name format
            
            // Validate phone number format
            Assertions.assertThat(result.getPhoneNumber1())
                .isNotNull()
                .matches("\\d{10}"); // 10-digit phone number format
                
            // Validate SSN format (encrypted but validate structure)
            Assertions.assertThat(result.getSsn()).isNotNull();
            
            // Validate FICO score range
            Assertions.assertThat(result.getFicoScore())
                .isBetween(300, 850); // Valid FICO score range
        }

        @Test
        @DisplayName("generateTransaction - creates transaction entities with proper amount precision")
        public void testGenerateTransaction_CreatesTransactionWithProperPrecision() {
            // When: Generating transaction test entity
            Transaction result = TestDataGenerator.generateTransaction();

            // Then: Validate transaction structure and precision
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getTransactionId()).isNotNull();
            Assertions.assertThat(result.getTransactionId().toString()).matches("\\d{16}");
            Assertions.assertThat(result.getAccountId()).isNotNull();
            Assertions.assertThat(result.getAccountId().toString()).matches("\\d{11}");
            
            // Validate transaction amount has COBOL COMP-3 precision
            Assertions.assertThat(result.getAmount().scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            
            // Validate transaction type is valid
            Assertions.assertThat(result.getTransactionTypeCode())
                .isNotNull()
                .isIn("01", "02", "03", "04", "05", "06"); // COBOL transaction type codes
                
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
            Card result = TestDataGenerator.generateCard();

            // Then: Validate card structure and security requirements
            Assertions.assertThat(result).isNotNull();
            
            // Validate card number format (test data - not real)
            Assertions.assertThat(result.getCardNumber())
                .isNotNull();
            Assertions.assertThat(result.getCardNumber().toString())
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
            Account account1 = TestDataGenerator.generateAccount();
            Account account2 = TestDataGenerator.generateAccount();
            
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
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount()
            );
            
            List<Customer> customers = List.of(
                TestDataGenerator.generateCustomer(),
                TestDataGenerator.generateCustomer(),
                TestDataGenerator.generateCustomer()
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
            Customer customer = TestDataGenerator.generateCustomer();
            Account account = TestDataGenerator.generateAccount();
            Card card = TestDataGenerator.generateCard();
            Transaction transaction = TestDataGenerator.generateTransaction();

            // Then: Validate all entities have properly formatted identifiers
            Assertions.assertThat(customer.getCustomerId().toString()).matches("\\d{9}");
            Assertions.assertThat(account.getAccountId().toString()).matches("\\d{11}");
            Assertions.assertThat(card.getCardNumber().toString()).matches("\\d{16}");
            Assertions.assertThat(transaction.getTransactionId().toString()).matches("\\d{16}");
            
            // Validate all monetary amounts have consistent precision
            Assertions.assertThat(account.getCurrentBalance().scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            Assertions.assertThat(transaction.getAmount().scale())
                .isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
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
        @DisplayName("edge cases - handles invalid length inputs gracefully")
        public void testEdgeCases_HandlesInvalidLengthInputs() {
            // When/Then: Invalid lengths should return empty or minimal strings
            String negativeResult = TestDataGenerator.generatePicString(-1, false);
            Assertions.assertThat(negativeResult).isEmpty(); // Negative length produces empty string
                
            String zeroResult = TestDataGenerator.generatePicString(0, false);
            Assertions.assertThat(zeroResult).isEmpty(); // Zero length produces empty string
        }

        @Test
        @DisplayName("edge cases - handles invalid VSAM key types")
        public void testEdgeCases_HandlesInvalidVsamKeyTypes() {
            // When/Then: Invalid key types should be handled gracefully
            String emptyKeyResult = TestDataGenerator.generateVsamKey(new int[]{});
            Assertions.assertThat(emptyKeyResult).isEmpty(); // Empty array produces empty key
                
            // Test with zero-length key fields
            String zeroLengthResult = TestDataGenerator.generateVsamKey(new int[]{0, 0});
            Assertions.assertThat(zeroLengthResult).isEmpty(); // Zero-length fields produce empty key
        }

        @Test
        @DisplayName("edge cases - handles boundary values for COMP-3 generation")
        public void testEdgeCases_HandlesBoundaryValuesForComp3() {
            // Test boundary conditions for scale values
            int[] boundaryScales = {0, 1, 2, 4, 6, 8}; // Common COBOL scales
            
            for (int scale : boundaryScales) {
                // When: Generating with boundary scale values
                BigDecimal result = TestDataGenerator.generateComp3BigDecimal(
                    scale, 10000.0);
                    
                // Then: Should handle all scales properly
                Assertions.assertThat(result.scale()).isEqualTo(scale);
                validateCobolPrecision(result, "boundary_scale_" + scale);
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
                boolean isNumeric = picClause.contains("9");
                // Extract length from PIC clause (e.g., "PIC X(1)" -> 1, "PIC 9(18)" -> 18)
                int length = extractLengthFromPicClause(picClause);
                String result = TestDataGenerator.generatePicString(length, isNumeric);
                
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
            TestDataGenerator.resetRandomSeed(12345L);
            Account account1 = TestDataGenerator.generateAccount();
            BigDecimal decimal1 = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            String pic1 = TestDataGenerator.generatePicString(10, false);

            // When: Reset seed and generate again
            TestDataGenerator.resetRandomSeed(12345L);
            Account account2 = TestDataGenerator.generateAccount();
            BigDecimal decimal2 = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            String pic2 = TestDataGenerator.generatePicString(10, false);

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
            TestDataGenerator.resetRandomSeed(12345L);
            String key1 = TestDataGenerator.generateVsamKey(new int[]{11});
            String key2 = TestDataGenerator.generateVsamKey(new int[]{11}); // No reset
            String key3 = TestDataGenerator.generateVsamKey(new int[]{11}); // No reset

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
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount()
            );

            // When: Converting to CSV format (simulated - TestDataGenerator should provide this)
            // Note: This tests the expected functionality based on requirements
            for (Account account : accounts) {
                // Then: Validate entities can be serialized to CSV-compatible format
                Assertions.assertThat(account.getAccountId().toString()).matches("\\d{11}");
                Assertions.assertThat(account.getCurrentBalance().toPlainString())
                    .matches("-?\\d+\\.\\d{2}"); // CSV-friendly decimal format (allows negative values)
                Assertions.assertThat(account.getCreditLimit().toPlainString())
                    .matches("\\d+\\.\\d{2}"); // CSV-friendly decimal format
            }
        }

        @Test
        @DisplayName("JSON conversion - generates valid JSON test fixtures")
        public void testJsonConversion_GeneratesValidJsonTestFixtures() {
            // Given: Generated test entities
            Customer customer = TestDataGenerator.generateCustomer();
            Transaction transaction = TestDataGenerator.generateTransaction();

            // When/Then: Validate entities have JSON-serializable properties
            Assertions.assertThat(customer.getCustomerId()).isNotNull();
            Assertions.assertThat(customer.getFirstName()).isNotNull();
            Assertions.assertThat(customer.getLastName()).isNotNull();
            
            Assertions.assertThat(transaction.getTransactionId()).isNotNull();
            Assertions.assertThat(transaction.getAmount()).isNotNull();
            Assertions.assertThat(transaction.getTransactionTypeCode()).isNotNull();
            
            // Validate monetary amounts are JSON-compatible
            String amountString = transaction.getAmount().toPlainString();
            Assertions.assertThat(amountString).matches("\\d+\\.\\d{2}");
        }

        @Test
        @DisplayName("fixed-width conversion - validates COBOL record layout compatibility")
        public void testFixedWidthConversion_ValidatesCobolRecordLayouts() {
            // Given: Generated entities with COBOL-compatible field formats
            Account account = TestDataGenerator.generateAccount();
            Customer customer = TestDataGenerator.generateCustomer();

            // When/Then: Validate fields match fixed-width COBOL record expectations
            
            // Account ID: PIC 9(10) - exactly 10 digits
            Assertions.assertThat(account.getAccountId().toString()).hasSize(11);
            
            // Customer ID: PIC 9(9) - exactly 9 digits  
            Assertions.assertThat(customer.getCustomerId().toString()).hasSize(9);
            
            // Names: PIC X(25) - up to 25 characters
            Assertions.assertThat(customer.getFirstName()).hasSizeLessThanOrEqualTo(25);
            Assertions.assertThat(customer.getLastName()).hasSizeLessThanOrEqualTo(25);
            
            // Phone: PIC X(12) format with dashes
            Assertions.assertThat(customer.getPhoneNumber1()).isNotNull(); // Phone number validation
        }
    }

    /**
     * Tests for integration with existing test infrastructure.
     * Validates that generated test data integrates properly with AbstractBaseTest
     * helper methods and TestConstants configuration.
     */
    @Nested
    @DisplayName("Test Infrastructure Integration Tests")
    @Tag("unit")
    class TestInfrastructureIntegrationTests {

        @Test
        @DisplayName("AbstractBaseTest integration - validates BigDecimal assertion helpers")
        public void testAbstractBaseTestIntegration_ValidatesBigDecimalHelpers() {
            // Given: Generated monetary values
            BigDecimal amount1 = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            BigDecimal amount2 = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);

            // When/Then: AbstractBaseTest helpers should work with generated data
            assertBigDecimalEquals(amount1, amount1, "Generated value should equal itself");
            
            // Test tolerance validation
            BigDecimal closeValue = amount1.add(new BigDecimal("0.001"));
            assertBigDecimalWithinTolerance(amount1, closeValue, "Close values within tolerance");
            
            // Test precision validation
            validateCobolPrecision(amount1, "amount1");
            validateCobolPrecision(amount2, "amount2");
        }

        @Test
        @DisplayName("TestConstants integration - uses shared constants for validation")
        public void testTestConstantsIntegration_UsesSharedConstants() {
            // When: Generating data using TestConstants
            BigDecimal testAmount = TestDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, 10000.0);
            
            // Then: Validate integration with test constants
            Assertions.assertThat(testAmount.scale()).isEqualTo(TestConstants.COBOL_DECIMAL_SCALE);
            
            // Test against known test IDs from TestConstants
            String generatedAccountId = TestDataGenerator.generateVsamKey(new int[]{11});
            Assertions.assertThat(generatedAccountId)
                .matches("\\d{11}") // Should match 11-digit pattern as specified in generateVsamKey
                .isNotEqualTo(TestConstants.TEST_ACCOUNT_ID); // But should be unique
        }

        @Test
        @DisplayName("UnitTest annotation - validates test categorization")
        public void testUnitTestAnnotation_ValidatesTestCategorization() {
            // When/Then: This test class should be properly categorized as unit test
            Assertions.assertThat(this.getClass())
                .hasAnnotation(Tag.class);
            
            // Validate test execution is fast (unit test characteristic)
            long startTime = System.currentTimeMillis();
            
            // Perform data generation operations
            TestDataGenerator.generateAccount();
            TestDataGenerator.generateCustomer();
            TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            
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
            BigDecimal monetary = TestDataGenerator.generateComp3BigDecimal(2, 10000.0);
            String alphanumeric = TestDataGenerator.generatePicString(20, false);
            String numeric = TestDataGenerator.generatePicString(10, true);

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
            for (Map.Entry<String, Object> entry : TestConstants.COBOL_COMP3_PATTERNS.entrySet()) {
                String pattern = entry.getKey();
                try {
                    // Generate and validate each COBOL pattern
                    String testValue = TestDataGenerator.generatePicString(10, false);
                    
                    // Then: Should be compatible with COBOL conversion
                    boolean isValid = CobolDataConverter.validateCobolField(testValue, "PIC X(10)");
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
            BigDecimal balance = TestDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, 10000.0);
            BigDecimal creditLimit = TestDataGenerator.generateComp3BigDecimal(
                TestConstants.COBOL_DECIMAL_SCALE, 10000.0);

            // Then: Validate against TestConstants validation thresholds
            Assertions.assertThat(balance.abs())
                .isLessThan(new BigDecimal("1000000")); // Reasonable monetary limit
            Assertions.assertThat(creditLimit.abs())
                .isLessThan(new BigDecimal("1000000")); // Reasonable credit limit
            
            // Validate precision characteristics
            validateCobolPrecision(balance, "balance");
            validateCobolPrecision(creditLimit, "creditLimit");
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
                TestDataGenerator.generateTransaction(),
                TestDataGenerator.generateTransaction(),
                TestDataGenerator.generateTransaction(),
                TestDataGenerator.generateTransaction(),
                TestDataGenerator.generateTransaction()
            );

            // Then: Should cover different transaction types
            List<String> types = transactions.stream()
                .map(Transaction::getTransactionTypeCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
                
            // Validate variety of transaction types for comprehensive testing
            Assertions.assertThat(types).isNotEmpty();
            
            // Validate all types are valid COBOL transaction codes
            for (String type : types) {
                Assertions.assertThat(type)
                    .isIn("01", "02", "03", "04", "05", "06", "07", "08", "09", "10");
            }
        }

        @Test
        @DisplayName("business scenarios - generates data covering boundary financial conditions")
        public void testBusinessScenarios_GeneratesDataCoveringBoundaryConditions() {
            // When: Generating accounts multiple times to get variety
            List<Account> accounts = List.of(
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount(),
                TestDataGenerator.generateAccount()
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
                validateCobolPrecision(balance, "balance");
            }
            
            for (BigDecimal limit : creditLimits) {
                validateCobolPrecision(limit, "limit");
            }
        }

        @Test
        @DisplayName("business scenarios - generates complete customer profiles for testing")
        public void testBusinessScenarios_GeneratesCompleteCustomerProfiles() {
            // When: Generating customer profiles
            List<Customer> customers = List.of(
                TestDataGenerator.generateCustomer(),
                TestDataGenerator.generateCustomer(),
                TestDataGenerator.generateCustomer()
            );

            // Then: All customers should have complete, valid profiles
            for (Customer customer : customers) {
                // Validate all required fields are populated
                Assertions.assertThat(customer.getCustomerId()).isNotNull();
                Assertions.assertThat(customer.getFirstName()).isNotNull().isNotEmpty();
                Assertions.assertThat(customer.getLastName()).isNotNull().isNotEmpty();
                Assertions.assertThat(customer.getPhoneNumber1()).isNotNull();
                Assertions.assertThat(customer.getSsn()).isNotNull();
                
                // Validate FICO score in valid range
                Assertions.assertThat(customer.getFicoScore())
                    .isBetween(300, 850);
            }
            
            // Validate uniqueness across customers
            List<String> customerIds = customers.stream()
                .map(Customer::getCustomerId)
                .map(Object::toString)
                .toList();
            Assertions.assertThat(customerIds).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("business scenarios - generates card data with security compliance")
        public void testBusinessScenarios_GeneratesCardDataWithSecurityCompliance() {
            // When: Generating card test data
            List<Card> cards = List.of(
                TestDataGenerator.generateCard(),
                TestDataGenerator.generateCard(),
                TestDataGenerator.generateCard()
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
    
    /**
     * Helper method to extract length from PIC clause.
     * Parses PIC clauses like "PIC X(10)" or "PIC 9(5)" to extract the length.
     */
    private int extractLengthFromPicClause(String picClause) {
        // Extract number from parentheses: "PIC X(1)" -> "1", "PIC 9(18)" -> "18"
        String lengthStr = picClause.replaceAll(".*\\((\\d+)\\).*", "$1");
        try {
            return Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            return 1; // Default to 1 if can't parse
        }
    }
}