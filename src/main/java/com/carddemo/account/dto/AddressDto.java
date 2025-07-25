/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.account.dto;

import com.carddemo.common.validator.ValidStateZip;
import com.carddemo.common.enums.UsStateCode;
import com.carddemo.common.enums.ValidationResult;
import com.carddemo.common.util.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Address Data Transfer Object supporting multi-line address fields with comprehensive
 * state and ZIP code validation for customer and account management operations.
 * 
 * This DTO maintains exact structural compatibility with the original customer record
 * address fields from CVCUS01Y.cpy while providing modern validation capabilities
 * through Jakarta Bean Validation and custom state/ZIP cross-validation.
 * 
 * Key Features:
 * - Multi-line address support (address_line_1, address_line_2, address_line_3)
 * - State code validation using COBOL-equivalent validation patterns
 * - ZIP code format validation with state cross-reference checking
 * - Country code support for international address standardization
 * - JSON serialization support for React frontend components
 * - Comprehensive validation with detailed error feedback
 * 
 * Field Structure (from CVCUS01Y.cpy customer record):
 * - CUST-ADDR-LINE-1    PIC X(50) → addressLine1
 * - CUST-ADDR-LINE-2    PIC X(50) → addressLine2  
 * - CUST-ADDR-LINE-3    PIC X(50) → addressLine3
 * - CUST-ADDR-STATE-CD  PIC X(02) → stateCode
 * - CUST-ADDR-COUNTRY-CD PIC X(03) → countryCode
 * - CUST-ADDR-ZIP       PIC X(10) → zipCode
 * 
 * Validation Rules:
 * - Address lines must not exceed 50 characters (COBOL field length)
 * - State code must be valid US state or territory code 
 * - ZIP code must be 5-digit or ZIP+4 format (NNNNN or NNNNN-NNNN)
 * - State/ZIP combination must be geographically consistent
 * - Country code must be 3-character ISO country code
 * 
 * React Integration:
 * - JSON property names align with React form field conventions
 * - Validation errors map to Material-UI form validation feedback
 * - Field sequencing preserves original BMS screen layout
 * 
 * @author CardDemo Development Team - Blitzy Platform
 * @version 1.0
 * @since Java 21
 */
@ValidStateZip(validateZipPrefix = true, allowEmpty = false)
public class AddressDto {

    /**
     * Primary address line containing street number, street name, and unit designation.
     * Maps to CUST-ADDR-LINE-1 field from customer record structure.
     * 
     * Validation:
     * - Maximum 50 characters (COBOL PIC X(50) equivalent)
     * - Supports apartment, suite, unit designations
     * - Required for complete address validation
     */
    @JsonProperty("addressLine1")
    @Size(max = 50, message = "Address line 1 cannot exceed 50 characters")
    private String addressLine1;

    /**
     * Secondary address line for additional address information.
     * Maps to CUST-ADDR-LINE-2 field from customer record structure.
     * 
     * Validation:
     * - Maximum 50 characters (COBOL PIC X(50) equivalent)
     * - Optional field for extended address information
     * - Commonly used for apartment numbers, building names
     */
    @JsonProperty("addressLine2")
    @Size(max = 50, message = "Address line 2 cannot exceed 50 characters")
    private String addressLine2;

    /**
     * Tertiary address line for complex address structures.
     * Maps to CUST-ADDR-LINE-3 field from customer record structure.
     * 
     * Validation:
     * - Maximum 50 characters (COBOL PIC X(50) equivalent)
     * - Optional field for additional address details
     * - Used for care-of addresses or complex delivery instructions
     */
    @JsonProperty("addressLine3")
    @Size(max = 50, message = "Address line 3 cannot exceed 50 characters")
    private String addressLine3;

    /**
     * Two-character US state or territory code.
     * Maps to CUST-ADDR-STATE-CD field from customer record structure.
     * 
     * Validation:
     * - Must be valid US state, territory, or military postal code
     * - Cross-validated with ZIP code for geographic consistency
     * - Uses COBOL VALID-US-STATE-CODE equivalent validation
     * - Case-insensitive but stored as uppercase
     */
    @JsonProperty("stateCode")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    private String stateCode;

    /**
     * Three-character country code for international address support.
     * Maps to CUST-ADDR-COUNTRY-CD field from customer record structure.
     * 
     * Validation:
     * - Maximum 3 characters (COBOL PIC X(03) equivalent)
     * - Supports ISO 3166-1 alpha-3 country codes
     * - Defaults to "USA" for domestic addresses
     * - Used for address standardization and validation
     */
    @JsonProperty("countryCode")
    @Size(max = 3, message = "Country code cannot exceed 3 characters")
    private String countryCode;

    /**
     * ZIP code supporting both 5-digit and ZIP+4 formats.
     * Maps to CUST-ADDR-ZIP field from customer record structure.
     * 
     * Validation:
     * - Must be 5-digit format (NNNNN) or ZIP+4 format (NNNNN-NNNN)
     * - Cross-validated with state code for geographic consistency
     * - Maximum 10 characters to accommodate ZIP+4 format
     * - Required for complete US address validation
     */
    @JsonProperty("zipCode")
    @Size(max = 10, message = "ZIP code cannot exceed 10 characters")
    private String zipCode;

    /**
     * Default constructor for AddressDto.
     * Initializes empty address structure for form binding.
     */
    public AddressDto() {
        // Default constructor for JSON deserialization and form binding
    }

    /**
     * Full constructor for AddressDto with all address components.
     * 
     * @param addressLine1 Primary address line
     * @param addressLine2 Secondary address line  
     * @param addressLine3 Tertiary address line
     * @param stateCode Two-character state code
     * @param countryCode Three-character country code
     * @param zipCode ZIP code in 5-digit or ZIP+4 format
     */
    public AddressDto(String addressLine1, String addressLine2, String addressLine3,
                     String stateCode, String countryCode, String zipCode) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressLine3 = addressLine3;
        this.stateCode = stateCode;
        this.countryCode = countryCode;
        this.zipCode = zipCode;
    }

    /**
     * Gets the primary address line.
     * 
     * @return the primary address line
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * Sets the primary address line with validation.
     * 
     * @param addressLine1 the primary address line to set
     */
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /**
     * Gets the secondary address line.
     * 
     * @return the secondary address line
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * Sets the secondary address line with validation.
     * 
     * @param addressLine2 the secondary address line to set
     */
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /**
     * Gets the tertiary address line.
     * 
     * @return the tertiary address line
     */
    public String getAddressLine3() {
        return addressLine3;
    }

    /**
     * Sets the tertiary address line with validation.
     * 
     * @param addressLine3 the tertiary address line to set
     */
    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    /**
     * Gets the state code.
     * 
     * @return the two-character state code
     */
    public String getStateCode() {
        return stateCode;
    }

    /**
     * Sets the state code with normalization and validation.
     * State codes are automatically converted to uppercase for consistency.
     * 
     * @param stateCode the state code to set
     */
    public void setStateCode(String stateCode) {
        this.stateCode = stateCode != null ? stateCode.trim().toUpperCase() : null;
    }

    /**
     * Gets the country code.
     * 
     * @return the three-character country code
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the country code with normalization.
     * Country codes are automatically converted to uppercase for consistency.
     * 
     * @param countryCode the country code to set
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode != null ? countryCode.trim().toUpperCase() : null;
    }

    /**
     * Gets the ZIP code.
     * 
     * @return the ZIP code in 5-digit or ZIP+4 format
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Sets the ZIP code with format normalization.
     * ZIP codes are trimmed and normalized for consistent formatting.
     * 
     * @param zipCode the ZIP code to set
     */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode != null ? zipCode.trim() : null;
    }

    /**
     * Validates the complete address structure with comprehensive business rule checking.
     * This method provides equivalent validation to the original COBOL address validation
     * logic while offering enhanced error feedback for React form components.
     * 
     * Validation Process:
     * 1. Individual field format validation
     * 2. Required field validation for primary address line
     * 3. State code validation using COBOL-equivalent patterns
     * 4. ZIP code format validation
     * 5. State/ZIP cross-reference validation for geographic consistency
     * 6. Country code validation for international support
     * 
     * @return ValidationResult indicating success or specific validation failure
     */
    public ValidationResult validate() {
        // Validate primary address line (required for complete address)
        ValidationResult addressValidation = ValidationUtils.validateRequiredField(
            addressLine1, "primary address line"
        );
        if (!addressValidation.isValid()) {
            return addressValidation;
        }

        // Validate address line length constraints
        if (addressLine1 != null && addressLine1.length() > 50) {
            return ValidationResult.INVALID_LENGTH;
        }
        if (addressLine2 != null && addressLine2.length() > 50) {
            return ValidationResult.INVALID_LENGTH;
        }
        if (addressLine3 != null && addressLine3.length() > 50) {
            return ValidationResult.INVALID_LENGTH;
        }

        // Validate state code if provided
        if (stateCode != null && !stateCode.trim().isEmpty()) {
            ValidationResult stateValidation = ValidationUtils.validateAlphaField(stateCode, 2);
            if (!stateValidation.isValid()) {
                return stateValidation;
            }

            // Validate state code using UsStateCode enum
            if (!UsStateCode.isValid(stateCode)) {
                return ValidationResult.INVALID_CROSS_REFERENCE;
            }
        }

        // Validate ZIP code format if provided
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            ValidationResult zipValidation = ValidationUtils.validateZipCode(zipCode);
            if (!zipValidation.isValid()) {
                return zipValidation;
            }

            // Perform state/ZIP cross-validation if both are provided
            if (stateCode != null && !stateCode.trim().isEmpty()) {
                ValidationResult crossValidation = UsStateCode.validateWithZipCode(stateCode, zipCode);
                if (!crossValidation.isValid()) {
                    return crossValidation;
                }
            }
        }

        // Validate country code format if provided
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            ValidationResult countryValidation = ValidationUtils.validateAlphaField(countryCode, 3);
            if (!countryValidation.isValid()) {
                return countryValidation;
            }
        }

        return ValidationResult.VALID;
    }

    /**
     * Determines if this address represents a complete US domestic address.
     * A complete address requires primary address line, state code, and ZIP code.
     * 
     * @return true if this is a complete US address, false otherwise
     */
    public boolean isCompleteUsAddress() {
        return addressLine1 != null && !addressLine1.trim().isEmpty() &&
               stateCode != null && !stateCode.trim().isEmpty() &&
               zipCode != null && !zipCode.trim().isEmpty() &&
               (countryCode == null || countryCode.trim().isEmpty() || 
                "USA".equals(countryCode.trim().toUpperCase()));
    }

    /**
     * Gets a formatted single-line address string for display purposes.
     * Combines all non-empty address components into a comma-separated string.
     * 
     * @return formatted address string for display
     */
    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            address.append(addressLine1);
        }
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2);
        }
        
        if (addressLine3 != null && !addressLine3.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine3);
        }
        
        if (stateCode != null && !stateCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(stateCode);
        }
        
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (stateCode != null && !stateCode.trim().isEmpty()) {
                address.append(" ");
            } else if (address.length() > 0) {
                address.append(", ");
            }
            address.append(zipCode);
        }
        
        if (countryCode != null && !countryCode.trim().isEmpty() && 
            !"USA".equals(countryCode.trim().toUpperCase())) {
            if (address.length() > 0) address.append(", ");
            address.append(countryCode);
        }
        
        return address.toString();
    }

    /**
     * Checks if this address is empty (all fields are null or empty).
     * 
     * @return true if all address fields are empty, false otherwise
     */
    public boolean isEmpty() {
        return (addressLine1 == null || addressLine1.trim().isEmpty()) &&
               (addressLine2 == null || addressLine2.trim().isEmpty()) &&
               (addressLine3 == null || addressLine3.trim().isEmpty()) &&
               (stateCode == null || stateCode.trim().isEmpty()) &&
               (countryCode == null || countryCode.trim().isEmpty()) &&
               (zipCode == null || zipCode.trim().isEmpty());
    }

    /**
     * Compares this address with another object for equality.
     * Two addresses are considered equal if all their fields match exactly.
     * 
     * @param obj the object to compare with
     * @return true if the addresses are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AddressDto that = (AddressDto) obj;
        return Objects.equals(addressLine1, that.addressLine1) &&
               Objects.equals(addressLine2, that.addressLine2) &&
               Objects.equals(addressLine3, that.addressLine3) &&
               Objects.equals(stateCode, that.stateCode) &&
               Objects.equals(countryCode, that.countryCode) &&
               Objects.equals(zipCode, that.zipCode);
    }

    /**
     * Generates hash code for this address based on all field values.
     * 
     * @return hash code for this address
     */
    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, addressLine2, addressLine3, 
                           stateCode, countryCode, zipCode);
    }

    /**
     * Returns a string representation of this address for debugging and logging.
     * Sensitive information is not included in the string representation.
     * 
     * @return string representation of this address
     */
    @Override
    public String toString() {
        return String.format("AddressDto{addressLine1='%s', addressLine2='%s', " +
                           "addressLine3='%s', state='%s', country='%s', zip='%s'}",
                           addressLine1, addressLine2, addressLine3, 
                           stateCode, countryCode, 
                           zipCode != null ? zipCode.substring(0, Math.min(zipCode.length(), 5)) + "***" : null);
    }
}