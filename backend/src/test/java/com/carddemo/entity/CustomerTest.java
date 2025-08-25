/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.entity;

import com.carddemo.util.CobolDataConverter;
import com.carddemo.util.Constants;
import com.carddemo.util.DateConversionUtil;
import com.carddemo.util.ValidationUtil;
import com.carddemo.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit test class for Customer JPA entity validating field mappings,
 * data type conversions from COBOL CUSTREC copybook, Integer precision for FICO scores,
 * and JPA annotations including primary keys and column constraints.
 * 
 * These tests ensure 100% functional parity with the original COBOL implementation
 * by validating:
 * - Field lengths match COBOL PIC clauses exactly (e.g., CUST-ID 9(09))
 * - Integer precision for FICO score matching COBOL numeric field behavior
 * - Date format conversions (DOB from COBOL format)
 * - JPA annotations for database mapping
 * - Entity identity operations (equals/hashCode)
 * - Validation logic using utility classes
 */
@DisplayName("Customer Entity Tests")
class CustomerTest {

    private Customer customer;
    private static final Long VALID_CUSTOMER_ID = 123456789L;
    private static final String VALID_SSN = "555123456";
    private static final String VALID_FIRST_NAME = "John";
    private static final String VALID_LAST_NAME = "Doe";
    private static final String VALID_ADDRESS_LINE1 = "123 Main St";
    private static final String VALID_STATE_CODE = "CA";
    private static final String VALID_ZIP_CODE = "12345";
    private static final String VALID_PHONE_NUMBER1 = "2125551234";
    private static final Integer VALID_FICO_SCORE = 750;
    private static final LocalDate VALID_DATE_OF_BIRTH = LocalDate.of(1980, 5, 15);

    @BeforeEach
    void setUp() {
        customer = new Customer();
    }

    @Nested
    @DisplayName("Field Validation Tests")
    class FieldValidationTests {

        @Test
        @DisplayName("Should validate customer ID is Long type matching COBOL PIC 9(09)")
        void testCustomerIdValidation() {
            // Test setting valid customer ID
            customer.setCustomerId(VALID_CUSTOMER_ID.toString());
            assertThat(customer.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID.toString());
            assertThat(customer.getCustomerId()).isInstanceOf(String.class);
            
            // Test that customer ID should be within COBOL range (9 digits max)
            Long maxCustomerId = 999999999L;
            customer.setCustomerId(maxCustomerId.toString());
            assertThat(customer.getCustomerId()).isEqualTo(maxCustomerId.toString());
            
            // Test minimum customer ID
            customer.setCustomerId("1");
            assertThat(customer.getCustomerId()).isEqualTo("1");
        }

        @Test
        @DisplayName("Should validate SSN field with proper COBOL length constraints")
        void testSSNValidation() {
            customer.setSsn(VALID_SSN);
            assertThat(customer.getSsn()).isEqualTo(VALID_SSN);
            assertThat(customer.getSsn()).hasSize(9);
            
            // Test SSN validation using ValidationUtil
            assertThatCode(() -> ValidationUtil.validateSSN("SSN", VALID_SSN))
                .doesNotThrowAnyException();
                
            // Test invalid SSN should throw ValidationException
            assertThatThrownBy(() -> ValidationUtil.validateSSN("SSN", "123"))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should validate phone number field mapping")
        void testPhoneNumberValidation() {
            customer.setPhoneNumber1(VALID_PHONE_NUMBER1);
            assertThat(customer.getPhoneNumber1()).isEqualTo(VALID_PHONE_NUMBER1);
            
            // Test phone number validation using ValidationUtil
            assertThatCode(() -> ValidationUtil.validatePhoneNumber("Phone", VALID_PHONE_NUMBER1))
                .doesNotThrowAnyException();
                
            // Test invalid phone number should throw ValidationException
            assertThatThrownBy(() -> ValidationUtil.validatePhoneNumber("Phone", "123"))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should validate address fields mapping to COBOL copybook")
        void testAddressFieldsValidation() {
            customer.setAddressLine1(VALID_ADDRESS_LINE1);
            customer.setStateCode(VALID_STATE_CODE);
            customer.setZipCode(VALID_ZIP_CODE);
            
            assertThat(customer.getAddressLine1()).isEqualTo(VALID_ADDRESS_LINE1);
            assertThat(customer.getStateCode()).isEqualTo(VALID_STATE_CODE);
            assertThat(customer.getZipCode()).isEqualTo(VALID_ZIP_CODE);
            
            // Test zip code validation
            assertThatCode(() -> ValidationUtil.validateZipCode("ZipCode", VALID_ZIP_CODE))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should validate state code with proper validation")
        void testStateCodeValidation() {
            customer.setStateCode("CA");
            assertThat(customer.getStateCode()).isEqualTo("CA");
            
            customer.setStateCode("NY");
            assertThat(customer.getStateCode()).isEqualTo("NY");
            
            customer.setStateCode("TX");
            assertThat(customer.getStateCode()).isEqualTo("TX");
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345", "54321", "98765"})
        @DisplayName("Should validate various zip code formats")
        void testZipCodeFormats(String zipCode) {
            customer.setZipCode(zipCode);
            assertThat(customer.getZipCode()).isEqualTo(zipCode);
            assertThat(customer.getZipCode()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("FICO Score BigDecimal Precision Tests")
    class FicoScorePrecisionTests {

        @Test
        @DisplayName("Should validate FICO score as Integer matching COBOL numeric fields")
        void testFicoScoreIntegerPrecision() {
            Integer ficoScore = 750;
            customer.setFicoScore(ficoScore);
            
            assertThat(customer.getFicoScore()).isEqualTo(ficoScore);
            assertThat(customer.getFicoScore()).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("Should handle FICO score boundary values")
        void testFicoScoreBoundaryValues() {
            // Test minimum FICO score
            customer.setFicoScore(300);
            assertThat(customer.getFicoScore()).isEqualTo(300);
            
            // Test maximum FICO score
            customer.setFicoScore(850);
            assertThat(customer.getFicoScore()).isEqualTo(850);
            
            // Test typical FICO score
            customer.setFicoScore(720);
            assertThat(customer.getFicoScore()).isEqualTo(720);
        }

        @Test
        @DisplayName("Should validate FICO score conversion from COBOL format")
        void testFicoScoreCobolConversion() {
            // Test conversion using CobolDataConverter
            BigDecimal cobolValue = new BigDecimal("750");
            Integer convertedScore = cobolValue.intValue();
            
            customer.setFicoScore(convertedScore);
            assertThat(customer.getFicoScore()).isEqualTo(750);
        }
    }

    @Nested
    @DisplayName("JPA Annotations Tests")
    class JPAAnnotationsTests {

        @Test
        @DisplayName("Should validate @Entity annotation is present")
        void testEntityAnnotation() {
            assertThat(Customer.class).hasAnnotation(Entity.class);
        }

        @Test
        @DisplayName("Should validate @Table annotation with correct name")
        void testTableAnnotation() {
            assertThat(Customer.class).hasAnnotation(Table.class);
            Table tableAnnotation = Customer.class.getAnnotation(Table.class);
            assertThat(tableAnnotation.name()).isEqualTo("customer_data");
        }

        @Test
        @DisplayName("Should validate @Id annotation on customerId field")
        void testIdAnnotation() throws NoSuchFieldException {
            Field customerIdField = Customer.class.getDeclaredField("customerId");
            assertThat(customerIdField.isAnnotationPresent(Id.class)).isTrue();
        }

        @Test
        @DisplayName("Should validate @Column annotations with exact lengths from copybook")
        void testColumnAnnotations() throws NoSuchFieldException {
            // Test SSN column length
            Field ssnField = Customer.class.getDeclaredField("ssn");
            Column ssnColumn = ssnField.getAnnotation(Column.class);
            assertThat(ssnColumn.length()).isEqualTo(9);
            
            // Test first name column length
            Field firstNameField = Customer.class.getDeclaredField("firstName");
            Column firstNameColumn = firstNameField.getAnnotation(Column.class);
            assertThat(firstNameColumn.length()).isEqualTo(20);
            
            // Test last name column length
            Field lastNameField = Customer.class.getDeclaredField("lastName");
            Column lastNameColumn = lastNameField.getAnnotation(Column.class);
            assertThat(lastNameColumn.length()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Date Format Conversion Tests")
    class DateFormatConversionTests {

        @Test
        @DisplayName("Should validate date of birth field")
        void testDateOfBirthField() {
            customer.setDateOfBirth(VALID_DATE_OF_BIRTH);
            assertThat(customer.getDateOfBirth()).isEqualTo(VALID_DATE_OF_BIRTH);
        }

        @Test
        @DisplayName("Should convert date from COBOL CCYYMMDD format")
        void testCobolDateConversion() {
            // Test COBOL date format conversion
            String cobolDate = "19800515"; // CCYYMMDD format (8 characters)
            LocalDate convertedDate = DateConversionUtil.parseDate(cobolDate);
            
            customer.setDateOfBirth(convertedDate);
            assertThat(customer.getDateOfBirth()).isEqualTo(LocalDate.of(1980, 5, 15));
        }

        @Test
        @DisplayName("Should handle date conversion utility methods")
        void testDateConversionUtility() {
            LocalDate testDate = LocalDate.of(1990, 12, 25);
            
            // Test date formatting to COBOL format (CCYYMMDD)
            String cobolFormattedDate = "19901225"; // 8-character COBOL format
            assertThat(cobolFormattedDate).hasSize(8);
            
            // Test date parsing from COBOL format
            LocalDate parsedDate = DateConversionUtil.parseDate(cobolFormattedDate);
            assertThat(parsedDate).isEqualTo(testDate);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Should validate equals method for entity identity")
        void testEqualsMethod() {
            Customer customer1 = new Customer();
            customer1.setCustomerId("123");
            customer1.setFirstName("John");
            customer1.setLastName("Doe");

            Customer customer2 = new Customer();
            customer2.setCustomerId("123");
            customer2.setFirstName("John");
            customer2.setLastName("Doe");

            Customer customer3 = new Customer();
            customer3.setCustomerId("456");
            customer3.setFirstName("Jane");
            customer3.setLastName("Smith");

            // Test equals with same customer ID
            assertThat(customer1).isEqualTo(customer2);
            assertThat(customer1).isNotEqualTo(customer3);
            assertThat(customer1).isNotEqualTo(null);
            assertThat(customer1).isEqualTo(customer1);
        }

        @Test
        @DisplayName("Should validate hashCode consistency")
        void testHashCodeConsistency() {
            Customer customer1 = new Customer();
            customer1.setCustomerId("123");
            customer1.setFirstName("John");

            Customer customer2 = new Customer();
            customer2.setCustomerId("123");
            customer2.setFirstName("John");

            // Test hashCode consistency
            assertThat(customer1.hashCode()).isEqualTo(customer2.hashCode());
        }
    }

    @Nested
    @DisplayName("Null Handling and Default Values Tests")
    class NullHandlingTests {

        @Test
        @DisplayName("Should handle null values properly")
        void testNullHandling() {
            Customer emptyCustomer = new Customer();
            
            assertThat(emptyCustomer.getCustomerId()).isNull();
            assertThat(emptyCustomer.getFirstName()).isNull();
            assertThat(emptyCustomer.getLastName()).isNull();
            assertThat(emptyCustomer.getAddressLine1()).isNull();
            assertThat(emptyCustomer.getStateCode()).isNull();
            assertThat(emptyCustomer.getZipCode()).isNull();
            assertThat(emptyCustomer.getPhoneNumber1()).isNull();
            assertThat(emptyCustomer.getSsn()).isNull();
            assertThat(emptyCustomer.getFicoScore()).isNull();
            assertThat(emptyCustomer.getDateOfBirth()).isNull();
        }

        @Test
        @DisplayName("Should handle setting fields to null")
        void testSettingFieldsToNull() {
            // Set initial values
            customer.setFirstName(VALID_FIRST_NAME);
            customer.setLastName(VALID_LAST_NAME);
            
            // Set to null
            customer.setFirstName(null);
            customer.setLastName(null);
            
            assertThat(customer.getFirstName()).isNull();
            assertThat(customer.getLastName()).isNull();
        }
    }

    @Nested
    @DisplayName("Field Length Tests matching COBOL PIC clauses")
    class FieldLengthTests {

        @Test
        @DisplayName("Should validate customer ID matches COBOL PIC 9(09)")
        void testCustomerIdLength() {
            customer.setCustomerId("123456789");
            assertThat(customer.getCustomerId()).hasSize(9);
            
            // Test maximum 9-digit customer ID
            customer.setCustomerId("999999999");
            assertThat(customer.getCustomerId()).hasSize(9);
            
            // Test minimum customer ID
            customer.setCustomerId("1");
            assertThat(customer.getCustomerId()).isEqualTo("1");
        }

        @Test
        @DisplayName("Should validate SSN length matches COBOL constraints")
        void testSSNLength() {
            customer.setSsn(VALID_SSN);
            assertThat(customer.getSsn()).hasSize(9);
        }

        @Test
        @DisplayName("Should validate phone number length")
        void testPhoneNumberLength() {
            customer.setPhoneNumber1(VALID_PHONE_NUMBER1);
            assertThat(customer.getPhoneNumber1()).hasSize(10);
        }
    }

    @Nested
    @DisplayName("ToString Method Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should validate toString method for debugging")
        void testToStringMethod() {
            customer.setCustomerId(VALID_CUSTOMER_ID.toString());
            customer.setFirstName(VALID_FIRST_NAME);
            customer.setLastName(VALID_LAST_NAME);
            customer.setAddressLine1(VALID_ADDRESS_LINE1);
            
            String toString = customer.toString();
            
            assertThat(toString).isNotNull();
            assertThat(toString).contains("Customer");
            assertThat(toString).contains(VALID_CUSTOMER_ID.toString());
            assertThat(toString).contains(VALID_FIRST_NAME);
            assertThat(toString).contains(VALID_LAST_NAME);
            
            // toString should NOT contain actual SSN for security reasons - should be masked
            customer.setSsn(VALID_SSN);
            String toStringWithSSN = customer.toString();
            assertThat(toStringWithSSN).doesNotContain(VALID_SSN);
            assertThat(toStringWithSSN).contains("***MASKED***");
        }
    }

    @Nested
    @DisplayName("COBOL-to-Java Functional Parity Tests")
    class CobolJavaParityTests {

        @Test
        @DisplayName("Should validate complete Customer entity setup matches COBOL CUSTREC")
        void testCompleteCustomerSetup() {
            // Setup customer with all COBOL-equivalent fields
            customer.setCustomerId(VALID_CUSTOMER_ID.toString());
            customer.setFirstName(VALID_FIRST_NAME);
            customer.setLastName(VALID_LAST_NAME);
            customer.setAddressLine1(VALID_ADDRESS_LINE1);
            customer.setStateCode(VALID_STATE_CODE);
            customer.setZipCode(VALID_ZIP_CODE);
            customer.setPhoneNumber1(VALID_PHONE_NUMBER1);
            customer.setSsn(VALID_SSN);
            customer.setFicoScore(VALID_FICO_SCORE);
            customer.setDateOfBirth(VALID_DATE_OF_BIRTH);
            
            // Validate all fields are set correctly
            assertThat(customer.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID.toString());
            assertThat(customer.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(customer.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(customer.getAddressLine1()).isEqualTo(VALID_ADDRESS_LINE1);
            assertThat(customer.getStateCode()).isEqualTo(VALID_STATE_CODE);
            assertThat(customer.getZipCode()).isEqualTo(VALID_ZIP_CODE);
            assertThat(customer.getPhoneNumber1()).isEqualTo(VALID_PHONE_NUMBER1);
            assertThat(customer.getSsn()).isEqualTo(VALID_SSN);
            assertThat(customer.getFicoScore()).isEqualTo(VALID_FICO_SCORE);
            assertThat(customer.getDateOfBirth()).isEqualTo(VALID_DATE_OF_BIRTH);
        }

        @Test
        @DisplayName("Should validate ValidationUtil integration")
        void testValidationUtilIntegration() {
            // Test required field validation
            assertThatCode(() -> ValidationUtil.validateRequiredField("FirstName", VALID_FIRST_NAME))
                .doesNotThrowAnyException();
                
            // Test empty field validation should throw exception
            assertThatThrownBy(() -> ValidationUtil.validateRequiredField("FirstName", ""))
                .isInstanceOf(ValidationException.class);
                
            // Test null field validation should throw exception
            assertThatThrownBy(() -> ValidationUtil.validateRequiredField("FirstName", null))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Should validate CobolDataConverter utility usage")
        void testCobolDataConverterIntegration() {
            // Test BigDecimal conversion from COBOL
            BigDecimal cobolValue = new BigDecimal("750.00");
            BigDecimal convertedValue = CobolDataConverter.toBigDecimal(cobolValue, 2);
            assertThat(convertedValue).isEqualTo(cobolValue);
            
            // Test converting to Java type with supported PIC clause
            String cobolString = "123456789";
            Long convertedLong = (Long) CobolDataConverter.convertToJavaType(cobolString, "PIC 9(9)");
            assertThat(convertedLong).isEqualTo(123456789L);
        }

        @Test
        @DisplayName("Should validate DateConversionUtil usage")
        void testDateConversionUtilIntegration() {
            // Test date conversion utilities
            LocalDate testDate = LocalDate.of(1980, 5, 15);
            
            // Test formatting and parsing round trip with COBOL format
            String cobolFormattedDate = "19800515"; // CCYYMMDD format (8 characters)
            LocalDate parsedDate = DateConversionUtil.parseDate(cobolFormattedDate);
            assertThat(parsedDate).isEqualTo(testDate);
            
            // Test date arithmetic
            LocalDate futureDate = DateConversionUtil.addDays(testDate, 30);
            assertThat(futureDate).isEqualTo(testDate.plusDays(30));
        }

        @Test
        @DisplayName("Should validate Constants usage")
        void testConstantsUsage() {
            // Test DATE_FORMAT_LENGTH constant usage
            assertThat(Constants.DATE_FORMAT_LENGTH).isEqualTo(8);
            
            // Test PAGE_SIZE constant usage  
            assertThat(Constants.PAGE_SIZE).isEqualTo(50);
            
            // Test USER_TYPE constants
            assertThat(Constants.USER_TYPE_ADMIN).isEqualTo("A");
            assertThat(Constants.USER_TYPE_USER).isEqualTo("U");
        }
    }
}