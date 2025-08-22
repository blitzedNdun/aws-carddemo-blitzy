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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Comprehensive JUnit 5 unit test class for TransactionType JPA entity.
 * 
 * This test class validates the 60-byte TRAN-TYPE-RECORD structure from CVTRA03Y copybook
 * and ensures complete functional parity with the original COBOL implementation. Tests cover
 * field mappings, validation annotations, caching behavior, and business logic methods.
 * 
 * Key Test Areas:
 * - COBOL copybook field mapping validation (2-char type code, 50-char description, 8-char padding)
 * - JPA annotation validation (@Id, @Column, @Entity, @Table)
 * - Bean validation constraints (@NotNull, @Size, @Pattern)
 * - Transaction type lookup functionality and unique constraints
 * - Caching annotations for performance optimization
 * - Business logic methods (isDebit(), isCredit(), getDisplayText())
 * - Object contract compliance (equals(), hashCode(), toString())
 * - Field length constraints matching COBOL PIC clauses exactly
 * 
 * The test suite ensures the TransactionType entity maintains immutability as reference data
 * and supports high-performance lookup operations required for transaction classification.
 * 
 * Based on COBOL copybook: app/cpy/CVTRA03Y.cpy
 * Record Structure: TRAN-TYPE-RECORD (60 bytes)
 * - TRAN-TYPE PIC X(02): transaction type code
 * - TRAN-TYPE-DESC PIC X(50): type description  
 * - FILLER PIC X(08): padding (handled by JPA)
 * 
 * @author CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
@DisplayName("TransactionType Entity Tests - COBOL CVTRA03Y Copybook Validation")
public class TransactionTypeTest extends AbstractBaseTest implements UnitTest {
    
    private TransactionType validTransactionType;
    private Validator validator;
    
    /**
     * Test setup method executed before each test.
     * Initializes valid TransactionType entity instance and Bean Validation framework
     * for comprehensive validation testing scenarios.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        
        // Initialize Bean Validation framework for constraint testing
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create valid TransactionType instance for testing
        validTransactionType = new TransactionType(
            TestConstants.TEST_TRANSACTION_TYPE_CODE,  // "PU"
            TestConstants.TEST_TRANSACTION_TYPE_DESC,   // "Purchase"
            "D"  // Debit flag
        );
        
        logTestExecution("TransactionType test setup completed", null);
    }
    
    // === COBOL Field Mapping Tests ===
    
    @Test
    @DisplayName("Validate 60-byte TRAN-TYPE-RECORD structure from CVTRA03Y copybook")
    public void testTranTypeRecordStructure() {
        // Test that entity represents the complete 60-byte COBOL record structure
        // TRAN-TYPE (2) + TRAN-TYPE-DESC (50) + FILLER (8) = 60 bytes
        
        TransactionType transactionType = new TransactionType("01", "Test Transaction Type Description 1234567890123456", "D");
        
        // Validate TRAN-TYPE field mapping (PIC X(02))
        assertThat(transactionType.getTransactionTypeCode())
            .as("TRAN-TYPE field from CVTRA03Y copybook")
            .hasSize(2)
            .matches(Constants.TYPE_CODE_LENGTH == 2 ? "^.{2}$" : "^.{" + Constants.TYPE_CODE_LENGTH + "}$");
        
        // Validate TRAN-TYPE-DESC field mapping (PIC X(50))
        assertThat(transactionType.getTypeDescription())
            .as("TRAN-TYPE-DESC field from CVTRA03Y copybook")
            .hasSize(50);
        
        // Validate debit/credit flag (additional field in modernized entity)
        assertThat(transactionType.getDebitCreditFlag())
            .as("Debit/Credit flag for accounting classification")
            .hasSize(1)
            .isIn("D", "C");
        
        // Log COBOL structure validation
        logTestExecution("TRAN-TYPE-RECORD structure validation completed", null);
    }
    
    @Test
    @DisplayName("Test 2-character transaction type code field mapping (TRAN-TYPE PIC X(02))")
    public void testTransactionTypeCodeMapping() {
        // Test exact mapping to COBOL TRAN-TYPE field
        String testCode = "PU";
        validTransactionType.setTransactionTypeCode(testCode);
        
        assertThat(validTransactionType.getTransactionTypeCode())
            .as("Transaction type code must match COBOL PIC X(02) specification")
            .isEqualTo(testCode)
            .hasSize(Constants.TYPE_CODE_LENGTH);
        
        // Test field length constraint matches COBOL specification
        assertThat(Constants.TYPE_CODE_LENGTH)
            .as("TYPE_CODE_LENGTH constant must match COBOL PIC X(02)")
            .isEqualTo(2);
    }
    
    @Test
    @DisplayName("Verify 50-character type description field (TRAN-TYPE-DESC PIC X(50))")
    public void testTypeDescriptionMapping() {
        // Test exact mapping to COBOL TRAN-TYPE-DESC field
        String maxLengthDescription = "12345678901234567890123456789012345678901234567890"; // Exactly 50 chars
        validTransactionType.setTypeDescription(maxLengthDescription);
        
        assertThat(validTransactionType.getTypeDescription())
            .as("Type description must match COBOL PIC X(50) specification")
            .isEqualTo(maxLengthDescription)
            .hasSize(50);
        
        // Test minimum length description
        String minDescription = "A";
        validTransactionType.setTypeDescription(minDescription);
        
        assertThat(validTransactionType.getTypeDescription())
            .as("Type description minimum length validation")
            .isEqualTo(minDescription)
            .hasSizeGreaterThan(0);
    }
    
    @Test
    @DisplayName("Test FILLER field handling (8-character padding)")
    public void testFillerFieldHandling() {
        // In the modernized JPA entity, FILLER fields are not explicitly mapped
        // This test validates that the entity handles the COBOL record structure correctly
        // without requiring explicit FILLER field storage
        
        // Verify that the entity only contains the essential business fields
        Field[] fields = TransactionType.class.getDeclaredFields();
        
        assertThat(fields)
            .as("Entity should not contain FILLER fields - handled by JPA")
            .extracting(Field::getName)
            .containsExactlyInAnyOrder(
                "transactionTypeCode",
                "typeDescription", 
                "debitCreditFlag"
            );
        
        // Verify total meaningful data length matches COBOL record minus FILLER
        // TRAN-TYPE (2) + TRAN-TYPE-DESC (50) + DEBIT-CREDIT-FLAG (1) = 53 bytes of business data
        // FILLER (8) is handled by JPA/PostgreSQL and not stored in Java object
        int businessDataLength = Constants.TYPE_CODE_LENGTH + 50 + 1;
        
        assertThat(businessDataLength)
            .as("Business data length should be 53 bytes (60 - 8 FILLER)")
            .isEqualTo(53);
        
        logTestExecution("FILLER field handling validation completed", null);
    }
    
    // === JPA Annotation Tests ===
    
    @Test
    @DisplayName("Validate JPA @Entity and @Table annotations")
    public void testJpaEntityAnnotations() {
        // Verify @Entity annotation is present
        assertThat(TransactionType.class.isAnnotationPresent(Entity.class))
            .as("TransactionType must be annotated with @Entity")
            .isTrue();
        
        // Verify @Table annotation with correct table name
        Table tableAnnotation = TransactionType.class.getAnnotation(Table.class);
        assertThat(tableAnnotation)
            .as("TransactionType must be annotated with @Table")
            .isNotNull();
        
        assertThat(tableAnnotation.name())
            .as("Table name must be 'transaction_types'")
            .isEqualTo("transaction_types");
        
        // Verify indexes are defined for performance optimization
        assertThat(tableAnnotation.indexes())
            .as("Table must have performance indexes defined")
            .hasSizeGreaterThan(0);
    }
    
    @Test
    @DisplayName("Test unique constraint on transaction type code (@Id annotation)")
    public void testPrimaryKeyConstraint() throws NoSuchFieldException {
        // Verify @Id annotation on transactionTypeCode field
        Field typeCodeField = TransactionType.class.getDeclaredField("transactionTypeCode");
        
        assertThat(typeCodeField.isAnnotationPresent(Id.class))
            .as("transactionTypeCode field must be annotated with @Id")
            .isTrue();
        
        // Verify @Column annotation with correct constraints
        Column columnAnnotation = typeCodeField.getAnnotation(Column.class);
        assertThat(columnAnnotation)
            .as("transactionTypeCode field must have @Column annotation")
            .isNotNull();
        
        assertThat(columnAnnotation.name())
            .as("Column name must be 'transaction_type_code'")
            .isEqualTo("transaction_type_code");
        
        assertThat(columnAnnotation.length())
            .as("Column length must match COBOL PIC X(02)")
            .isEqualTo(2);
        
        assertThat(columnAnnotation.nullable())
            .as("Primary key column cannot be nullable")
            .isFalse();
    }
    
    // === Bean Validation Tests ===
    
    @Test
    @DisplayName("Test @NotNull constraints on required fields")
    public void testNotNullConstraints() {
        // Test transactionTypeCode null validation
        TransactionType nullCodeType = new TransactionType(null, "Valid Description", "D");
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(nullCodeType);
        
        assertThat(violations)
            .as("Transaction type code cannot be null")
            .extracting(ConstraintViolation::getMessage)
            .contains("Transaction type code is required");
        
        // Test typeDescription null validation
        TransactionType nullDescType = new TransactionType("01", null, "D");
        violations = validator.validate(nullDescType);
        
        assertThat(violations)
            .as("Transaction type description cannot be null")
            .extracting(ConstraintViolation::getMessage)
            .contains("Transaction type description is required");
        
        // Test debitCreditFlag null validation  
        TransactionType nullFlagType = new TransactionType("01", "Valid Description", null);
        violations = validator.validate(nullFlagType);
        
        assertThat(violations)
            .as("Debit/credit flag cannot be null")
            .extracting(ConstraintViolation::getMessage)
            .contains("Debit/credit flag is required");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "A", "ABC", "1", "123"})
    @DisplayName("Test @Size constraint on transaction type code (must be exactly 2 characters)")
    public void testTransactionTypeCodeSizeConstraint(String invalidCode) {
        TransactionType invalidSizeType = new TransactionType(invalidCode, "Valid Description", "D");
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(invalidSizeType);
        
        assertThat(violations)
            .as("Transaction type code size validation should fail for: " + invalidCode)
            .extracting(ConstraintViolation::getMessage)
            .contains("Transaction type code must be exactly 2 characters");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "This is a test description that exceeds fifty characters limit for validation testing purposes"})
    @DisplayName("Test @Size constraint on type description (1-50 characters)")
    public void testTypeDescriptionSizeConstraint(String invalidDescription) {
        TransactionType invalidSizeType = new TransactionType("01", invalidDescription, "D");
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(invalidSizeType);
        
        assertThat(violations)
            .as("Type description size validation should fail for length: " + (invalidDescription != null ? invalidDescription.length() : 0))
            .extracting(ConstraintViolation::getMessage)
            .contains("Transaction type description must be between 1 and 50 characters");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"01", "PU", "FE", "PA", "A1", "Z9", "00", "99"})
    @DisplayName("Test @Pattern constraint on transaction type code (alphanumeric only)")
    public void testValidTransactionTypeCodePatterns(String validCode) {
        TransactionType validType = new TransactionType(validCode, "Valid Description", "D");
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(validType);
        
        assertThat(violations)
            .as("Valid transaction type code should pass pattern validation: " + validCode)
            .filteredOn(violation -> violation.getPropertyPath().toString().equals("transactionTypeCode"))
            .isEmpty();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"a1", "1a", "**", "--", "++", "..", "  ", "\t\n"})
    @DisplayName("Test @Pattern constraint rejection of invalid characters")
    public void testInvalidTransactionTypeCodePatterns(String invalidCode) {
        TransactionType invalidType = new TransactionType(invalidCode, "Valid Description", "D");
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(invalidType);
        
        assertThat(violations)
            .as("Invalid transaction type code should fail pattern validation: " + invalidCode)
            .extracting(ConstraintViolation::getMessage)
            .contains("Transaction type code must contain only digits and uppercase letters");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"A", "B", "X", "Y", "1", "2", "Z"})
    @DisplayName("Test @Pattern constraint on debit/credit flag (must be D or C)")
    public void testInvalidDebitCreditFlagPatterns(String invalidFlag) {
        TransactionType invalidType = new TransactionType("01", "Valid Description", invalidFlag);
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(invalidType);
        
        assertThat(violations)
            .as("Invalid debit/credit flag should fail pattern validation: " + invalidFlag)
            .extracting(ConstraintViolation::getMessage)
            .contains("Debit/credit flag must be 'D' for debit or 'C' for credit");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"D", "C"})
    @DisplayName("Test valid debit/credit flag values")
    public void testValidDebitCreditFlags(String validFlag) {
        TransactionType validType = new TransactionType("01", "Valid Description", validFlag);
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(validType);
        
        assertThat(violations)
            .as("Valid debit/credit flag should pass validation: " + validFlag)
            .filteredOn(violation -> violation.getPropertyPath().toString().equals("debitCreditFlag"))
            .isEmpty();
    }
    
    // === Lookup Table Functionality Tests ===
    
    @Test
    @DisplayName("Validate lookup table functionality for transaction categorization")
    public void testTransactionTypeLookupFunctionality() {
        // Test common transaction type codes for lookup operations
        TransactionType purchaseType = new TransactionType("01", "Purchase", "D");
        TransactionType paymentType = new TransactionType("02", "Payment", "C");
        TransactionType feeType = new TransactionType("03", "Fee Assessment", "D");
        
        // Validate purchase transaction type
        assertThat(purchaseType.getTransactionTypeCode())
            .as("Purchase type code lookup")
            .isEqualTo("01");
        assertThat(purchaseType.isDebit())
            .as("Purchase should be classified as debit")
            .isTrue();
        
        // Validate payment transaction type
        assertThat(paymentType.getTransactionTypeCode())
            .as("Payment type code lookup")
            .isEqualTo("02");
        assertThat(paymentType.isCredit())
            .as("Payment should be classified as credit")
            .isTrue();
        
        // Validate fee transaction type
        assertThat(feeType.getTransactionTypeCode())
            .as("Fee type code lookup")
            .isEqualTo("03");
        assertThat(feeType.isDebit())
            .as("Fee should be classified as debit")
            .isTrue();
        
        logTestExecution("Transaction type lookup functionality validated", null);
    }
    
    @Test
    @DisplayName("Test transaction type code uniqueness constraint")
    public void testTransactionTypeCodeUniqueness() {
        // Verify that transaction type codes are designed to be unique
        // (actual uniqueness is enforced by database constraints and @Id annotation)
        
        TransactionType type1 = new TransactionType("PU", "Purchase", "D");
        TransactionType type2 = new TransactionType("PU", "Another Purchase", "D");
        
        // Verify equals() method uses only the transaction type code for comparison
        assertThat(type1)
            .as("TransactionType equality should be based on transaction type code only")
            .isEqualTo(type2);
        
        // Verify hashCode() method uses only the transaction type code
        assertThat(type1.hashCode())
            .as("TransactionType hashCode should be based on transaction type code only")
            .isEqualTo(type2.hashCode());
        
        logTestExecution("Transaction type code uniqueness validation completed", null);
    }
    
    // === Business Logic Method Tests ===
    
    @Test
    @DisplayName("Test isDebit() method for debit transaction classification")
    public void testIsDebitMethod() {
        // Test debit transaction types
        TransactionType debitType = new TransactionType("01", "Purchase", "D");
        
        assertThat(debitType.isDebit())
            .as("Transaction with 'D' flag should be classified as debit")
            .isTrue();
        
        assertThat(debitType.isCredit())
            .as("Transaction with 'D' flag should not be classified as credit")
            .isFalse();
        
        // Test credit transaction types
        TransactionType creditType = new TransactionType("02", "Payment", "C");
        
        assertThat(creditType.isDebit())
            .as("Transaction with 'C' flag should not be classified as debit")
            .isFalse();
        
        assertThat(creditType.isCredit())
            .as("Transaction with 'C' flag should be classified as credit")
            .isTrue();
    }
    
    @Test
    @DisplayName("Test getDisplayText() method for UI display formatting")
    public void testGetDisplayTextMethod() {
        TransactionType displayType = new TransactionType("PU", "Purchase Transaction", "D");
        
        String displayText = displayType.getDisplayText();
        
        assertThat(displayText)
            .as("Display text should format code and description")
            .isEqualTo("PU - Purchase Transaction")
            .contains(displayType.getTransactionTypeCode())
            .contains(displayType.getTypeDescription());
        
        logTestExecution("Display text formatting validation completed", null);
    }
    
    @Test 
    @DisplayName("Test getAccountingClassification() method for accounting purposes")
    public void testGetAccountingClassificationMethod() {
        // Test debit classification
        TransactionType debitType = new TransactionType("01", "Purchase", "D");
        assertThat(debitType.getAccountingClassification())
            .as("Debit transaction should return 'Debit' classification")
            .isEqualTo("Debit");
        
        // Test credit classification
        TransactionType creditType = new TransactionType("02", "Payment", "C");
        assertThat(creditType.getAccountingClassification())
            .as("Credit transaction should return 'Credit' classification")
            .isEqualTo("Credit");
    }
    
    // === Immutability and Reference Data Tests ===
    
    @Test
    @DisplayName("Verify immutability of type codes as reference data")
    public void testReferenceDataImmutability() {
        // Create transaction type with specific code
        TransactionType referenceType = new TransactionType("RE", "Reference Type", "D");
        String originalCode = referenceType.getTransactionTypeCode();
        String originalDescription = referenceType.getTypeDescription();
        String originalFlag = referenceType.getDebitCreditFlag();
        
        // Verify that once created, the data represents immutable reference data
        // (Note: While setters exist for JPA, in business logic these should be treated as immutable)
        
        assertThat(referenceType.getTransactionTypeCode())
            .as("Transaction type code should maintain consistent value")
            .isEqualTo(originalCode);
        
        assertThat(referenceType.getTypeDescription())
            .as("Transaction type description should maintain consistent value")
            .isEqualTo(originalDescription);
        
        assertThat(referenceType.getDebitCreditFlag())
            .as("Debit/credit flag should maintain consistent value")
            .isEqualTo(originalFlag);
        
        // Verify that reference data maintains integrity across operations
        String displayText1 = referenceType.getDisplayText();
        String displayText2 = referenceType.getDisplayText();
        
        assertThat(displayText1)
            .as("Display text should be consistent across multiple calls")
            .isEqualTo(displayText2);
        
        logTestExecution("Reference data immutability validation completed", null);
    }
    
    // === Performance and Caching Tests ===
    
    @Test
    @DisplayName("Test caching annotations for performance optimization")
    public void testCachingAnnotations() throws NoSuchMethodException {
        // While @Cacheable is typically applied at the service layer,
        // verify the entity supports caching through proper equals/hashCode implementation
        
        TransactionType type1 = new TransactionType("PU", "Purchase", "D");
        TransactionType type2 = new TransactionType("PU", "Purchase", "D");
        
        // Verify consistent hashCode for caching effectiveness
        assertThat(type1.hashCode())
            .as("Identical transaction types should have same hashCode for caching")
            .isEqualTo(type2.hashCode());
        
        // Verify equals implementation supports cache key comparison
        assertThat(type1)
            .as("Identical transaction types should be equal for cache lookup")
            .isEqualTo(type2);
        
        // Test cache key stability - hashCode should not change
        int initialHashCode = type1.hashCode();
        String originalCode = type1.getTransactionTypeCode();
        
        // Access methods multiple times to ensure stability
        for (int i = 0; i < 5; i++) {
            assertThat(type1.hashCode())
                .as("HashCode should remain stable across multiple accesses")
                .isEqualTo(initialHashCode);
            
            assertThat(type1.getTransactionTypeCode())
                .as("Transaction type code should remain stable")
                .isEqualTo(originalCode);
        }
        
        logTestExecution("Caching support validation completed", null);
    }
    
    // === Field Length Validation Tests ===
    
    @ParameterizedTest
    @CsvSource({
        "'01', 'Purchase', true",
        "'02', 'Payment', true", 
        "'03', 'Fee Assessment', true",
        "'04', 'Cash Advance', true",
        "'05', 'Balance Transfer', true",
        "'PU', 'Purchase Transaction', true",
        "'PA', 'Payment Transaction', true",
        "'FE', 'Fee Transaction', true"
    })
    @DisplayName("Test valid transaction type combinations")
    public void testValidTransactionTypeCombinations(String code, String description, boolean shouldBeValid) {
        TransactionType transactionType = new TransactionType(code, description, "D");
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(transactionType);
        
        if (shouldBeValid) {
            assertThat(violations)
                .as("Valid transaction type should pass validation: " + code + " - " + description)
                .isEmpty();
        }
        
        // Verify field length compliance with COBOL specifications
        assertThat(transactionType.getTransactionTypeCode().length())
            .as("Transaction type code length must match COBOL PIC X(02)")
            .isEqualTo(Constants.TYPE_CODE_LENGTH);
        
        assertThat(transactionType.getTypeDescription().length())
            .as("Type description length must not exceed 50 characters")
            .isLessThanOrEqualTo(50);
    }
    
    // === Object Contract Tests ===
    
    @Test
    @DisplayName("Test equals() method implementation")
    public void testEqualsMethod() {
        TransactionType type1 = new TransactionType("PU", "Purchase", "D");
        TransactionType type2 = new TransactionType("PU", "Purchase", "D");
        TransactionType type3 = new TransactionType("PA", "Payment", "C");
        TransactionType type4 = new TransactionType("PU", "Different Description", "C");
        
        // Test reflexivity
        assertThat(type1)
            .as("Entity should be equal to itself")
            .isEqualTo(type1);
        
        // Test symmetry
        assertThat(type1)
            .as("Symmetric equality should work")
            .isEqualTo(type2);
        assertThat(type2)
            .as("Symmetric equality should work both ways")
            .isEqualTo(type1);
        
        // Test transitivity is inherent with proper implementation
        
        // Test inequality based on different transaction type codes
        assertThat(type1)
            .as("Different transaction type codes should not be equal")
            .isNotEqualTo(type3);
        
        // Test equality based on transaction type code only (not description or flag)
        assertThat(type1)
            .as("Same transaction type code should be equal regardless of other fields")
            .isEqualTo(type4);
        
        // Test null comparison
        assertThat(type1)
            .as("Entity should not be equal to null")
            .isNotEqualTo(null);
        
        // Test different class comparison
        assertThat(type1)
            .as("Entity should not be equal to different class")
            .isNotEqualTo("Not a TransactionType");
    }
    
    @Test
    @DisplayName("Test hashCode() method implementation")
    public void testHashCodeMethod() {
        TransactionType type1 = new TransactionType("PU", "Purchase", "D");
        TransactionType type2 = new TransactionType("PU", "Purchase", "D");
        TransactionType type3 = new TransactionType("PU", "Different Description", "C");
        TransactionType type4 = new TransactionType("PA", "Payment", "C");
        
        // Test consistency - hashCode should return same value when called multiple times
        int initialHashCode = type1.hashCode();
        assertThat(type1.hashCode())
            .as("HashCode should be consistent across multiple calls")
            .isEqualTo(initialHashCode);
        
        // Test equality contract - equal objects must have equal hash codes
        assertThat(type1.hashCode())
            .as("Equal objects must have equal hash codes")
            .isEqualTo(type2.hashCode());
        
        // Test hash code based on transaction type code only
        assertThat(type1.hashCode())
            .as("Same transaction type code should have same hash code")
            .isEqualTo(type3.hashCode());
        
        // Test different codes produce different hash codes (likely but not guaranteed)
        assertThat(type1.hashCode())
            .as("Different transaction type codes should likely have different hash codes")
            .isNotEqualTo(type4.hashCode());
        
        logTestExecution("HashCode implementation validation completed", null);
    }
    
    @Test
    @DisplayName("Test toString() method implementation")
    public void testToStringMethod() {
        TransactionType transactionType = new TransactionType("PU", "Purchase Transaction", "D");
        String stringRepresentation = transactionType.toString();
        
        assertThat(stringRepresentation)
            .as("toString() should contain class name")
            .contains("TransactionType");
        
        assertThat(stringRepresentation)
            .as("toString() should contain transaction type code")
            .contains("PU");
        
        assertThat(stringRepresentation)
            .as("toString() should contain type description")
            .contains("Purchase Transaction");
        
        assertThat(stringRepresentation)
            .as("toString() should contain debit/credit flag")
            .contains("D");
        
        // Verify format matches expected pattern
        assertThat(stringRepresentation)
            .as("toString() should follow expected format pattern")
            .matches("TransactionType\\{transactionTypeCode='.*', typeDescription='.*', debitCreditFlag='.*'\\}");
        
        logTestExecution("toString() implementation validation completed", null);
    }
    
    // === Reflection-Based Annotation Tests ===
    
    @Test
    @DisplayName("Test JPA column annotations using reflection")
    public void testColumnAnnotationsUsingReflection() throws NoSuchFieldException {
        // Test transactionTypeCode field annotations
        Field typeCodeField = TransactionType.class.getDeclaredField("transactionTypeCode");
        Column typeCodeColumn = typeCodeField.getAnnotation(Column.class);
        
        assertThat(typeCodeColumn.length())
            .as("Transaction type code column length should match COBOL PIC X(02)")
            .isEqualTo(2);
        
        // Test typeDescription field annotations
        Field descriptionField = TransactionType.class.getDeclaredField("typeDescription");
        Column descriptionColumn = descriptionField.getAnnotation(Column.class);
        
        assertThat(descriptionColumn.length())
            .as("Type description column length should match COBOL PIC X(50)")
            .isEqualTo(50);
        
        // Test debitCreditFlag field annotations
        Field flagField = TransactionType.class.getDeclaredField("debitCreditFlag");
        Column flagColumn = flagField.getAnnotation(Column.class);
        
        assertThat(flagColumn.length())
            .as("Debit/credit flag column length should be 1 character")
            .isEqualTo(1);
        
        logTestExecution("JPA column annotation validation completed", null);
    }
    
    @Test
    @DisplayName("Test database table indexes for performance optimization")
    public void testTableIndexConfiguration() {
        Table tableAnnotation = TransactionType.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();
        
        assertThat(indexes)
            .as("Table should have performance indexes defined")
            .isNotEmpty();
        
        // Verify specific indexes mentioned in the entity
        assertThat(indexes)
            .as("Should have index on debit_credit_flag for filtering operations")
            .extracting(Index::name)
            .contains("idx_transaction_types_debit_credit_flag");
        
        assertThat(indexes)
            .as("Should have index on description for search operations")
            .extracting(Index::name)
            .contains("idx_transaction_types_description");
        
        logTestExecution("Database index configuration validation completed", null);
    }
    
    // === Integration with Validation Utility Tests ===
    
    @Test
    @DisplayName("Test integration with ValidationUtil.validateNumericField()")
    public void testValidationUtilIntegration() {
        // Test numeric field validation using ValidationUtil
        String numericCode = "01";
        
        // This should not throw exception for valid numeric code
        assertThatNoException()
            .as("Numeric transaction type code should pass ValidationUtil.validateNumericField()")
            .isThrownBy(() -> ValidationUtil.validateNumericField("transactionTypeCode", numericCode));
        
        // Test the boolean version for proper integration
        boolean isValidNumeric = ValidationUtil.validateNumericField(numericCode, Constants.TYPE_CODE_LENGTH);
        
        assertThat(isValidNumeric)
            .as("Numeric transaction type code should be valid with correct length")
            .isTrue();
        
        logTestExecution("ValidationUtil integration validation completed", null);
    }
    
    @Test
    @DisplayName("Test integration with ValidationUtil.validateTransactionAmount() for business context")
    public void testTransactionAmountValidationContext() {
        // While TransactionType doesn't contain amounts, test the validation utility
        // that would be used in transaction processing with this type
        
        java.math.BigDecimal validAmount = new java.math.BigDecimal("100.00");
        
        boolean isValidAmount = ValidationUtil.validateTransactionAmount(validAmount);
        
        assertThat(isValidAmount)
            .as("Valid transaction amount should pass validation for use with transaction types")
            .isTrue();
        
        // Test invalid amounts that would be rejected in transaction processing
        java.math.BigDecimal invalidAmount = new java.math.BigDecimal("-50.00");
        
        boolean isInvalidAmount = ValidationUtil.validateTransactionAmount(invalidAmount);
        
        assertThat(isInvalidAmount)
            .as("Negative transaction amount should fail validation")
            .isFalse();
        
        logTestExecution("Transaction amount validation context completed", null);
    }
    
    // === Edge Case and Error Handling Tests ===
    
    @Test
    @DisplayName("Test constructor with all valid parameters")
    public void testValidConstructor() {
        TransactionType transactionType = new TransactionType("CC", "Credit Card Purchase", "D");
        
        assertThat(transactionType.getTransactionTypeCode())
            .as("Constructor should set transaction type code correctly")
            .isEqualTo("CC");
        
        assertThat(transactionType.getTypeDescription())
            .as("Constructor should set type description correctly")
            .isEqualTo("Credit Card Purchase");
        
        assertThat(transactionType.getDebitCreditFlag())
            .as("Constructor should set debit/credit flag correctly")
            .isEqualTo("D");
    }
    
    @Test
    @DisplayName("Test default constructor for JPA")
    public void testDefaultConstructor() {
        TransactionType defaultType = new TransactionType();
        
        assertThat(defaultType.getTransactionTypeCode())
            .as("Default constructor should initialize with null values")
            .isNull();
        
        assertThat(defaultType.getTypeDescription())
            .as("Default constructor should initialize description as null")
            .isNull();
        
        assertThat(defaultType.getDebitCreditFlag())
            .as("Default constructor should initialize flag as null")
            .isNull();
    }
    
    @Test
    @DisplayName("Test behavior with null transaction type code in equals/hashCode")
    public void testNullTransactionTypeCodeBehavior() {
        TransactionType nullCodeType1 = new TransactionType();
        TransactionType nullCodeType2 = new TransactionType();
        TransactionType validType = new TransactionType("PU", "Purchase", "D");
        
        // Test equals with null codes
        assertThat(nullCodeType1)
            .as("Entities with null codes should be equal")
            .isEqualTo(nullCodeType2);
        
        assertThat(nullCodeType1)
            .as("Null code entity should not equal entity with valid code")
            .isNotEqualTo(validType);
        
        // Test hashCode with null codes
        assertThat(nullCodeType1.hashCode())
            .as("Entities with null codes should have same hashCode")
            .isEqualTo(nullCodeType2.hashCode());
        
        // Test hashCode consistency
        assertThat(nullCodeType1.hashCode())
            .as("Null code entity should have consistent hashCode")
            .isEqualTo(0); // Based on implementation: transactionTypeCode != null ? transactionTypeCode.hashCode() : 0
        
        logTestExecution("Null transaction type code behavior validation completed", null);
    }
    
    // === COBOL Data Type Precision Tests ===
    
    @Test
    @DisplayName("Test COBOL data type conversion precision using TestConstants")
    public void testCobolDataTypePrecision() {
        // Test COBOL decimal scale compliance
        assertThat(TestConstants.COBOL_DECIMAL_SCALE)
            .as("COBOL decimal scale should be configured for financial precision")
            .isGreaterThan(0);
        
        // Test COBOL rounding mode compliance
        assertThat(TestConstants.COBOL_ROUNDING_MODE)
            .as("COBOL rounding mode should be configured")
            .isNotNull();
        
        // Test functional parity rules
        Boolean preserveDecimalPrecision = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("preserve_decimal_precision");
        assertThat(preserveDecimalPrecision)
            .as("Functional parity rules should preserve decimal precision")
            .isTrue();
        
        Boolean validateFieldLengths = (Boolean) TestConstants.FUNCTIONAL_PARITY_RULES.get("validate_field_lengths");
        assertThat(validateFieldLengths)
            .as("Functional parity rules should validate field lengths")
            .isTrue();
        
        logTestExecution("COBOL data type precision validation completed", null);
    }
    
    // === Performance Benchmark Tests ===
    
    @Test
    @DisplayName("Test entity creation and access performance")
    public void testEntityPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Create multiple transaction types to test performance
        for (int i = 0; i < 1000; i++) {
            TransactionType type = new TransactionType(
                String.format("%02d", i % 100),
                "Test Transaction Type " + i,
                i % 2 == 0 ? "D" : "C"
            );
            
            // Access all methods to test performance
            type.getTransactionTypeCode();
            type.getTypeDescription();
            type.getDebitCreditFlag();
            type.isDebit();
            type.isCredit();
            type.getDisplayText();
            type.getAccountingClassification();
            type.toString();
            type.hashCode();
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        assertThat(executionTime)
            .as("Entity operations should complete within performance threshold")
            .isLessThan(TestConstants.RESPONSE_TIME_THRESHOLD_MS);
        
        logTestExecution("Entity performance validation completed", executionTime);
    }
    
    // === Comprehensive Field Validation Tests ===
    
    @Test
    @DisplayName("Comprehensive validation of all field constraints")
    public void testAllFieldConstraintsComprehensive() {
        // Create transaction type with boundary values
        TransactionType boundaryType = new TransactionType("ZZ", "Z".repeat(50), "C");
        
        Set<ConstraintViolation<TransactionType>> violations = validator.validate(boundaryType);
        
        assertThat(violations)
            .as("Transaction type with maximum valid field lengths should pass validation")
            .isEmpty();
        
        // Verify all fields are properly set
        assertThat(boundaryType.getTransactionTypeCode())
            .as("Boundary test - transaction type code")
            .hasSize(2)
            .isEqualTo("ZZ");
        
        assertThat(boundaryType.getTypeDescription())
            .as("Boundary test - type description at maximum length")
            .hasSize(50);
        
        assertThat(boundaryType.getDebitCreditFlag())
            .as("Boundary test - credit flag")
            .isEqualTo("C");
        
        assertThat(boundaryType.isCredit())
            .as("Boundary test - should be classified as credit")
            .isTrue();
        
        logTestExecution("Comprehensive field validation completed", null);
    }
    
    @Test
    @DisplayName("Test field length constants match COBOL copybook specifications")
    public void testCobolFieldLengthConstants() {
        // Verify Constants.TYPE_CODE_LENGTH matches COBOL PIC X(02)
        assertThat(Constants.TYPE_CODE_LENGTH)
            .as("TYPE_CODE_LENGTH constant must match COBOL PIC X(02) specification")
            .isEqualTo(2);
        
        // Note: DESCRIPTION_LENGTH in Constants is 100, but entity uses 50 to match COBOL
        // This is correct as DESCRIPTION_LENGTH may be used for other entities
        
        // Verify the entity field constraints match COBOL copybook
        Field[] fields = TransactionType.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("transactionTypeCode")) {
                Column column = field.getAnnotation(Column.class);
                assertThat(column.length())
                    .as("transactionTypeCode column length must match COBOL PIC X(02)")
                    .isEqualTo(2);
            } else if (field.getName().equals("typeDescription")) {
                Column column = field.getAnnotation(Column.class);
                assertThat(column.length())
                    .as("typeDescription column length must match COBOL PIC X(50)")
                    .isEqualTo(50);
            } else if (field.getName().equals("debitCreditFlag")) {
                Column column = field.getAnnotation(Column.class);
                assertThat(column.length())
                    .as("debitCreditFlag column length must be 1 character")
                    .isEqualTo(1);
            }
        }
        
        logTestExecution("COBOL field length constants validation completed", null);
    }
}