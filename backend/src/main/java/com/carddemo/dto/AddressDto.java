package com.carddemo.dto;

import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Data Transfer Object for address information.
 * 
 * This DTO represents address components extracted from the COBOL CUSTOMER-RECORD
 * structure, providing a structured representation for address data in REST APIs
 * and embedded usage within customer and account DTOs.
 * 
 * Field mappings from COBOL CUSTREC copybook:
 * - addressLine1 ← CUST-ADDR-LINE-1 (PIC X(50))
 * - addressLine2 ← CUST-ADDR-LINE-2 (PIC X(50))
 * - addressLine3 ← CUST-ADDR-LINE-3 (PIC X(50))
 * - stateCode ← CUST-ADDR-STATE-CD (PIC X(02))
 * - countryCode ← CUST-ADDR-COUNTRY-CD (PIC X(03))
 * - zipCode ← CUST-ADDR-ZIP (PIC X(10))
 * 
 * Jakarta Bean Validation annotations ensure field length constraints match
 * the original COBOL picture clauses for data integrity and compatibility.
 */
@Data
public class AddressDto {

    /**
     * First line of the address.
     * Maps to CUST-ADDR-LINE-1 from CUSTOMER-RECORD (PIC X(50)).
     */
    @Size(max = 50, message = "Address line 1 must not exceed 50 characters")
    @JsonProperty("addressLine1")
    private String addressLine1;

    /**
     * Second line of the address.
     * Maps to CUST-ADDR-LINE-2 from CUSTOMER-RECORD (PIC X(50)).
     */
    @Size(max = 50, message = "Address line 2 must not exceed 50 characters")
    @JsonProperty("addressLine2")
    private String addressLine2;

    /**
     * Third line of the address.
     * Maps to CUST-ADDR-LINE-3 from CUSTOMER-RECORD (PIC X(50)).
     */
    @Size(max = 50, message = "Address line 3 must not exceed 50 characters")
    @JsonProperty("addressLine3")
    private String addressLine3;

    /**
     * State or province code.
     * Maps to CUST-ADDR-STATE-CD from CUSTOMER-RECORD (PIC X(02)).
     */
    @Size(max = 2, message = "State code must not exceed 2 characters")
    @JsonProperty("stateCode")
    private String stateCode;

    /**
     * Country code (ISO 3166-1 alpha-3 format).
     * Maps to CUST-ADDR-COUNTRY-CD from CUSTOMER-RECORD (PIC X(03)).
     */
    @Size(max = 3, message = "Country code must not exceed 3 characters")
    @JsonProperty("countryCode")
    private String countryCode;

    /**
     * ZIP or postal code.
     * Maps to CUST-ADDR-ZIP from CUSTOMER-RECORD (PIC X(10)).
     */
    @Size(max = 10, message = "ZIP code must not exceed 10 characters")
    @JsonProperty("zipCode")
    private String zipCode;
}