package com.carddemo.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Data;

/**
 * Data Transfer Object for customer-related requests.
 * 
 * This DTO handles customer creation, updates, and profile management requests,
 * mapping to customer management screens and inputs from the COBOL BMS screens.
 * 
 * Field mappings from COBOL CUSTOMER-RECORD (CUSTREC.cpy):
 * - customerId ← CUST-ID (PIC 9(09))
 * - firstName ← CUST-FIRST-NAME (PIC X(25))
 * - middleName ← CUST-MIDDLE-NAME (PIC X(25))
 * - lastName ← CUST-LAST-NAME (PIC X(25))
 * - address ← Embedded AddressDto containing CUST-ADDR-* fields
 * - phoneNumber1 ← CUST-PHONE-NUM-1 (PIC X(15))
 * - phoneNumber2 ← CUST-PHONE-NUM-2 (PIC X(15))
 * - dateOfBirth ← CUST-DOB-YYYYMMDD (PIC X(10)) converted to LocalDate
 * 
 * Security exclusions:
 * - SSN (CUST-SSN) - excluded from request for security compliance
 * - Government ID (CUST-GOVT-ISSUED-ID) - excluded from request for security compliance
 * 
 * Jakarta Bean Validation annotations ensure field length constraints match
 * the original COBOL picture clauses for data integrity and system compatibility.
 * 
 * GDPR Article 5 compliance: Only collects necessary personal data fields
 * required for customer profile management, excluding sensitive identifiers.
 */
@Data
public class CustomerRequest {

    /**
     * Customer identifier - 9 digit numeric ID.
     * Maps to CUST-ID from CUSTOMER-RECORD (PIC 9(09)).
     * Required for customer identification and updates.
     */
    @Size(max = 9, message = "Customer ID must not exceed 9 digits")
    @JsonProperty("customerId")
    private String customerId;

    /**
     * Customer's first name.
     * Maps to CUST-FIRST-NAME from CUSTOMER-RECORD (PIC X(25)).
     */
    @Size(max = 25, message = "First name must not exceed 25 characters")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * Customer's middle name.
     * Maps to CUST-MIDDLE-NAME from CUSTOMER-RECORD (PIC X(25)).
     */
    @Size(max = 25, message = "Middle name must not exceed 25 characters")
    @JsonProperty("middleName")
    private String middleName;

    /**
     * Customer's last name.
     * Maps to CUST-LAST-NAME from CUSTOMER-RECORD (PIC X(25)).
     */
    @Size(max = 25, message = "Last name must not exceed 25 characters")
    @JsonProperty("lastName")
    private String lastName;

    /**
     * Customer's address information.
     * Embedded AddressDto containing structured address fields from
     * CUST-ADDR-LINE-1, CUST-ADDR-LINE-2, CUST-ADDR-LINE-3,
     * CUST-ADDR-STATE-CD, CUST-ADDR-COUNTRY-CD, and CUST-ADDR-ZIP.
     */
    @Valid
    @JsonProperty("address")
    private AddressDto address;

    /**
     * Primary phone number.
     * Maps to CUST-PHONE-NUM-1 from CUSTOMER-RECORD (PIC X(15)).
     */
    @Size(max = 15, message = "Phone number 1 must not exceed 15 characters")
    @JsonProperty("phoneNumber1")
    private String phoneNumber1;

    /**
     * Secondary phone number.
     * Maps to CUST-PHONE-NUM-2 from CUSTOMER-RECORD (PIC X(15)).
     */
    @Size(max = 15, message = "Phone number 2 must not exceed 15 characters")
    @JsonProperty("phoneNumber2")
    private String phoneNumber2;

    /**
     * Customer's date of birth.
     * Maps to CUST-DOB-YYYYMMDD from CUSTOMER-RECORD (PIC X(10)).
     * Converted from COBOL YYYYMMDD string format to Java LocalDate.
     */
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;
}