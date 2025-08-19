/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.Constants;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import jakarta.persistence.Column;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit test class for Customer JPA entity validating field mappings,
 * data type conversions from COBOL CUSTREC copybook, BigDecimal precision for FICO scores,
 * and JPA annotations including composite keys and column constraints.
 * 
 * These tests ensure 100% functional parity with the original COBOL implementation
 * by validating:
 * - Field lengths match COBOL PIC clauses exactly (e.g., CUST-ID 9(09))
 * - BigDecimal precision for FICO score matching COBOL COMP-3 packed decimal behavior
 * - Date format conversions (DOB from COBOL format)
 * - JPA annotations for database mapping
 * - Entity identity operations (equals/hashCode)
 * - Validation logic using utility classes
 */
@DisplayName("Customer Entity Tests")
class CustomerTest {

    private Customer customer;
    private static final String VALID_CUSTOMER_ID = "123456789";
    private static final String VALID_SSN = "123456789";
    private static final String VALID_FIRST_NAME = "John";
    private static final String VALID_LAST_NAME = "Doe";
    private static final String VALID_PHONE = "5551234567";
    private static final String VALID_ADDRESS_1 = "123 Main St";
    private static final String VALID_CITY = "Anytown";
    private static final String VALID_STATE = "NY";
    private static final String VALID_ZIP = "12345";
    private static final LocalDate VALID_DOB = LocalDate.of(1985, 6, 15);
    private static final BigDecimal VALID_FICO_SCORE = new BigDecimal("750");

    @BeforeEach
    void setUp() {
        customer = new Customer();
    }

    @Nested
    @DisplayName("Field Validation Tests")
    class FieldValidationTests {

        @Test
        @DisplayName("Should validate customer ID length matches COBOL PIC 9(09)")
        void shouldValidateCustomerIdLength() {
            // Test valid 9-digit customer ID (COBOL PIC 9(09))
            customer.setCustomerId(VALID_CUSTOMER_ID);
            assertThat(customer.getCustomerId()).hasSize(9);
            assertThat(customer.getCustomerId()).matches("\\d{9}");

            // Test invalid lengths
            assertThatThrownBy(() -> customer.setCustomerId("12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID must be exactly 9 digits");

            assertThatThrownBy(() -> customer.setCustomerId("1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID must be exactly 9 digits");
        }

        @Test
        @DisplayName("Should validate SSN format and length matching COBOL PIC 9(09)")
        void shouldValidateSsnFormat() {
            // Test valid SSN
            customer.setSSN(VALID_SSN);
            assertThat(customer.getSSN()).hasSize(9);
            assertThat(ValidationUtil.validateSSN(customer.getSSN(), "SSN")).isTrue();

            // Test invalid SSN formats
            assertThat(ValidationUtil.validateSSN("12345678", "SSN")).isFalse();
            assertThat(ValidationUtil.validateSSN("1234567890", "SSN")).isFalse();
            assertThat(ValidationUtil.validateSSN("12345-678", "SSN")).isFalse();
        }

        @Test
        @DisplayName("Should validate name fields with proper length constraints")
        void shouldValidateNameFields() {
            // Test valid names
            customer.setFirstName(VALID_FIRST_NAME);
            customer.setLastName(VALID_LAST_NAME);
            
            assertThat(customer.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(customer.getLastName()).isEqualTo(VALID_LAST_NAME);

            // Test maximum length constraints (COBOL field lengths)
            String longName = "A".repeat(51); // Exceed typical 50-char limit
            assertThatThrownBy(() -> customer.setFirstName(longName))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should validate phone number format")
        void shouldValidatePhoneNumber() {
            // Test valid phone number
            customer.setPhoneNumber(VALID_PHONE);
            assertThat(ValidationUtil.validatePhoneNumber(customer.getPhoneNumber(), "Phone")).isTrue();

            // Test various invalid formats
            assertThat(ValidationUtil.validatePhoneNumber("555-123-4567", "Phone")).isFalse();
            assertThat(ValidationUtil.validatePhoneNumber("(555)1234567", "Phone")).isFalse();
            assertThat(ValidationUtil.validatePhoneNumber("12345", "Phone")).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345", "12345-6789", "123456789", "ABCDE"})
        @DisplayName("Should validate zip code formats")
        void shouldValidateZipCodeFormats(String zipCode) {
            customer.setAddress(VALID_ADDRESS_1);
            customer.setCity(VALID_CITY);
            customer.setState(VALID_STATE);
            customer.setZipCode(zipCode);

            boolean isValid = ValidationUtil.validateZipCode(zipCode, "Zip Code");
            if (zipCode.matches("\\d{5}") || zipCode.matches("\\d{5}-\\d{4}") || zipCode.matches("\\d{9}")) {
                assertThat(isValid).isTrue();
            } else {
                assertThat(isValid).isFalse();
            }
        }

        @Test
        @DisplayName("Should validate required fields are not null or empty")
        void shouldValidateRequiredFields() {
            // Test null values
            assertThat(ValidationUtil.validateRequiredField(null, "Customer ID")).isFalse();
            assertThat(ValidationUtil.validateRequiredField("", "Customer ID")).isFalse();
            assertThat(ValidationUtil.validateRequiredField("   ", "Customer ID")).isFalse();

            // Test valid value
            assertThat(ValidationUtil.validateRequiredField(VALID_CUSTOMER_ID, "Customer ID")).isTrue();
        }
    }

    @Nested
    @DisplayName("BigDecimal Precision Tests")
    class BigDecimalPrecisionTests {

        @Test
        @DisplayName("Should preserve FICO score precision matching COBOL COMP-3 behavior")
        void shouldPreserveFicoScorePrecision() {
            // Test FICO score as BigDecimal with exact precision
            customer.setFicoScore(VALID_FICO_SCORE);
            
            assertThat(customer.getFicoScore()).isEqualByComparingTo(VALID_FICO_SCORE);
            assertThat(customer.getFicoScore().scale()).isEqualTo(0); // No decimal places for FICO
            assertThat(customer.getFicoScore().precision()).isEqualTo(3); // 3 digits (PIC 9(03))
        }

        @Test
        @DisplayName("Should handle COBOL COMP-3 to BigDecimal conversion")
        void shouldHandleComp3Conversion() {
            // Simulate COMP-3 packed decimal data conversion
            byte[] comp3Data = {0x07, 0x50, 0x0C}; // Represents 750 in COMP-3 format
            
            BigDecimal convertedValue = CobolDataConverter.fromComp3(comp3Data, 0);
            customer.setFicoScore(convertedValue);
            
            assertThat(customer.getFicoScore()).isEqualByComparingTo(new BigDecimal("750"));
        }

        @Test
        @DisplayName("Should validate FICO score range constraints")
        void shouldValidateFicoScoreRange() {
            // Test valid FICO score range (typically 300-850)
            customer.setFicoScore(new BigDecimal("350"));
            assertThat(customer.getFicoScore()).isBetween(new BigDecimal("300"), new BigDecimal("850"));

            customer.setFicoScore(new BigDecimal("800"));
            assertThat(customer.getFicoScore()).isBetween(new BigDecimal("300"), new BigDecimal("850"));

            // Test edge cases
            customer.setFicoScore(new BigDecimal("300"));
            assertThat(customer.getFicoScore()).isEqualByComparingTo(new BigDecimal("300"));

            customer.setFicoScore(new BigDecimal("850"));
            assertThat(customer.getFicoScore()).isEqualByComparingTo(new BigDecimal("850"));
        }

        @Test
        @DisplayName("Should preserve precision using CobolDataConverter utility")
        void shouldPreservePrecisionUsingConverter() {
            BigDecimal originalValue = new BigDecimal("750.00");
            BigDecimal preservedValue = CobolDataConverter.preservePrecision(originalValue, 0);
            
            customer.setFicoScore(preservedValue);
            assertThat(customer.getFicoScore().scale()).isEqualTo(0);
            assertThat(customer.getFicoScore()).isEqualByComparingTo(new BigDecimal("750"));
        }
    }

    @Nested
    @DisplayName("JPA Annotation Tests")
    class JpaAnnotationTests {

        @Test
        @DisplayName("Should validate @Entity annotation is present")
        void shouldValidateEntityAnnotation() {
            assertThat(Customer.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isTrue();
        }

        @Test
        @DisplayName("Should validate @Table annotation with correct name")
        void shouldValidateTableAnnotation() {
            jakarta.persistence.Table tableAnnotation = Customer.class.getAnnotation(jakarta.persistence.Table.class);
            assertThat(tableAnnotation).isNotNull();
            assertThat(tableAnnotation.name()).isEqualTo("customer_data");
        }

        @Test
        @DisplayName("Should validate @Id annotation on customer ID field")
        void shouldValidateIdAnnotation() throws NoSuchFieldException {
            Field customerIdField = Customer.class.getDeclaredField("customerId");
            assertThat(customerIdField.isAnnotationPresent(jakarta.persistence.Id.class)).isTrue();
        }

        @Test
        @DisplayName("Should validate @Column annotations with exact lengths from copybook")
        void shouldValidateColumnAnnotations() throws NoSuchFieldException {
            // Test customer ID column
            Field customerIdField = Customer.class.getDeclaredField("customerId");
            Column customerIdColumn = customerIdField.getAnnotation(Column.class);
            assertThat(customerIdColumn).isNotNull();
            assertThat(customerIdColumn.name()).isEqualTo("customer_id");
            assertThat(customerIdColumn.nullable()).isFalse();

            // Test first name column length
            Field firstNameField = Customer.class.getDeclaredField("firstName");
            Column firstNameColumn = firstNameField.getAnnotation(Column.class);
            assertThat(firstNameColumn).isNotNull();
            assertThat(firstNameColumn.length()).isEqualTo(20); // COBOL PIC X(20)

            // Test last name column length
            Field lastNameField = Customer.class.getDeclaredField("lastName");
            Column lastNameColumn = lastNameField.getAnnotation(Column.class);
            assertThat(lastNameColumn).isNotNull();
            assertThat(lastNameColumn.length()).isEqualTo(20); // COBOL PIC X(20)

            // Test phone number column length
            Field phoneField = Customer.class.getDeclaredField("phoneNumber");
            Column phoneColumn = phoneField.getAnnotation(Column.class);
            assertThat(phoneColumn).isNotNull();
            assertThat(phoneColumn.length()).isEqualTo(15); // COBOL PIC X(15)
        }

        @Test
        @DisplayName("Should validate SSN field has proper security annotations")
        void shouldValidateSsnSecurityAnnotations() throws NoSuchFieldException {
            Field ssnField = Customer.class.getDeclaredField("ssn");
            
            // Should have @JsonIgnore for security
            assertThat(ssnField.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)).isTrue();
            
            // Should have @Column annotation
            Column ssnColumn = ssnField.getAnnotation(Column.class);
            assertThat(ssnColumn).isNotNull();
            assertThat(ssnColumn.length()).isEqualTo(9); // COBOL PIC 9(09)
        }
    }

    @Nested
    @DisplayName("Date Format Conversion Tests")
    class DateFormatConversionTests {

        @Test
        @DisplayName("Should convert date of birth from COBOL format (CCYYMMDD)")
        void shouldConvertDateOfBirthFromCobolFormat() {
            String cobolDate = "19850615"; // June 15, 1985 in CCYYMMDD format
            
            LocalDate convertedDate = DateConversionUtil.convertCobolDate(cobolDate);
            customer.setDateOfBirth(convertedDate);
            
            assertThat(customer.getDateOfBirth()).isEqualTo(VALID_DOB);
            assertThat(customer.getDateOfBirth().getYear()).isEqualTo(1985);
            assertThat(customer.getDateOfBirth().getMonthValue()).isEqualTo(6);
            assertThat(customer.getDateOfBirth().getDayOfMonth()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should validate date format using DateConversionUtil")
        void shouldValidateDateFormat() {
            String validDate = "19850615";
            String invalidDate = "19851301"; // Invalid month
            String invalidFormat = "85-06-15"; // Wrong format
            
            assertThat(DateConversionUtil.validateDate(validDate)).isTrue();
            assertThat(DateConversionUtil.validateDate(invalidDate)).isFalse();
            assertThat(DateConversionUtil.validateDate(invalidFormat)).isFalse();
        }

        @Test
        @DisplayName("Should convert LocalDate back to COBOL format")
        void shouldConvertLocalDateToCobolFormat() {
            customer.setDateOfBirth(VALID_DOB);
            
            String cobolFormat = DateConversionUtil.formatToCobol(customer.getDateOfBirth());
            assertThat(cobolFormat).isEqualTo("19850615");
        }

        @ParameterizedTest
        @ValueSource(strings = {"19700101", "19991231", "20001231", "20241231"})
        @DisplayName("Should handle various valid date formats")
        void shouldHandleValidDateFormats(String dateString) {
            LocalDate date = DateConversionUtil.convertCobolDate(dateString);
            customer.setDateOfBirth(date);
            
            assertThat(customer.getDateOfBirth()).isNotNull();
            String convertedBack = DateConversionUtil.formatToCobol(customer.getDateOfBirth());
            assertThat(convertedBack).isEqualTo(dateString);
        }
    }

    @Nested
    @DisplayName("Entity Identity Tests")
    class EntityIdentityTests {

        @Test
        @DisplayName("Should implement equals method correctly")
        void shouldImplementEqualsCorrectly() {
            Customer customer1 = createValidCustomer();
            Customer customer2 = createValidCustomer();
            Customer customer3 = createValidCustomer();
            customer3.setCustomerId("987654321");

            // Test reflexivity
            assertThat(customer1).isEqualTo(customer1);

            // Test symmetry
            assertThat(customer1).isEqualTo(customer2);
            assertThat(customer2).isEqualTo(customer1);

            // Test different customer IDs
            assertThat(customer1).isNotEqualTo(customer3);
            assertThat(customer3).isNotEqualTo(customer1);

            // Test null and different class
            assertThat(customer1).isNotEqualTo(null);
            assertThat(customer1).isNotEqualTo("Not a Customer");
        }

        @Test
        @DisplayName("Should implement hashCode method correctly")
        void shouldImplementHashCodeCorrectly() {
            Customer customer1 = createValidCustomer();
            Customer customer2 = createValidCustomer();

            // Equal objects must have equal hash codes
            assertThat(customer1.hashCode()).isEqualTo(customer2.hashCode());

            // Hash code should be consistent
            int initialHashCode = customer1.hashCode();
            assertThat(customer1.hashCode()).isEqualTo(initialHashCode);
        }

        @Test
        @DisplayName("Should implement toString method for debugging")
        void shouldImplementToStringForDebugging() {
            Customer customer = createValidCustomer();
            String toStringResult = customer.toString();

            assertThat(toStringResult).isNotNull();
            assertThat(toStringResult).contains("Customer");
            assertThat(toStringResult).contains(VALID_CUSTOMER_ID);
            // SSN should not appear in toString for security
            assertThat(toStringResult).doesNotContain(VALID_SSN);
        }
    }

    @Nested
    @DisplayName("Null Handling and Default Values Tests")
    class NullHandlingTests {

        @Test
        @DisplayName("Should handle null values matching COBOL FILLER fields")
        void shouldHandleNullValues() {
            Customer emptyCustomer = new Customer();

            // Optional fields should handle null gracefully
            assertThat(emptyCustomer.getMiddleName()).isNull();
            assertThat(emptyCustomer.getAddress2()).isNull();
            assertThat(emptyCustomer.getAddress3()).isNull();
            assertThat(emptyCustomer.getPhoneNumber2()).isNull();
        }

        @Test
        @DisplayName("Should validate default values for required fields")
        void shouldValidateDefaultValues() {
            Customer defaultCustomer = new Customer();
            
            // Country code should default to 'USA' (matching COBOL default)
            assertThat(defaultCustomer.getCountryCode()).isEqualTo(Constants.DEFAULT_COUNTRY_CODE);
        }

        @Test
        @DisplayName("Should handle empty string assignments")
        void shouldHandleEmptyStrings() {
            customer.setMiddleName("");
            customer.setAddress2("");
            
            // Empty strings should be converted to null for consistency
            assertThat(customer.getMiddleName()).isNull();
            assertThat(customer.getAddress2()).isNull();
        }
    }

    @Nested
    @DisplayName("Field Length Validation Tests")
    class FieldLengthValidationTests {

        @Test
        @DisplayName("Should validate customer ID length exactly matches COBOL PIC 9(09)")
        void shouldValidateCustomerIdExactLength() {
            // Test exact length (9 digits)
            customer.setCustomerId(VALID_CUSTOMER_ID);
            assertThat(customer.getCustomerId()).hasSize(Constants.CUSTOMER_ID_LENGTH);

            // Test shorter length
            assertThatThrownBy(() -> customer.setCustomerId("12345"))
                .isInstanceOf(IllegalArgumentException.class);

            // Test longer length
            assertThatThrownBy(() -> customer.setCustomerId("1234567890123"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should validate SSN length exactly matches COBOL PIC 9(09)")
        void shouldValidateSsnExactLength() {
            customer.setSSN(VALID_SSN);
            assertThat(customer.getSSN()).hasSize(Constants.SSN_LENGTH);
        }

        @Test
        @DisplayName("Should validate phone number length matches COBOL PIC X(15)")
        void shouldValidatePhoneNumberLength() {
            customer.setPhoneNumber(VALID_PHONE);
            assertThat(customer.getPhoneNumber()).hasSize(Constants.PHONE_NUMBER_LENGTH);
        }

        @Test
        @DisplayName("Should validate zip code length matches COBOL constraints")
        void shouldValidateZipCodeLength() {
            customer.setZipCode("12345");
            assertThat(customer.getZipCode()).hasSize(5);

            customer.setZipCode("12345-6789");
            assertThat(customer.getZipCode()).hasSize(10);

            customer.setZipCode("123456789");
            assertThat(customer.getZipCode()).hasSize(9);
        }
    }

    /**
     * Helper method to create a valid Customer instance for testing.
     */
    private Customer createValidCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(VALID_CUSTOMER_ID);
        customer.setFirstName(VALID_FIRST_NAME);
        customer.setLastName(VALID_LAST_NAME);
        customer.setPhoneNumber(VALID_PHONE);
        customer.setAddress(VALID_ADDRESS_1);
        customer.setCity(VALID_CITY);
        customer.setState(VALID_STATE);
        customer.setZipCode(VALID_ZIP);
        customer.setSSN(VALID_SSN);
        customer.setDateOfBirth(VALID_DOB);
        customer.setFicoScore(VALID_FICO_SCORE);
        return customer;
    }
}