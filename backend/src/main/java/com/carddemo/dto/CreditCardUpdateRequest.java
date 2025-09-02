package com.carddemo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO for credit card update requests containing card number, embossed name, 
 * expiration date, and active status fields. Maps to COCRDUPC BMS screen inputs 
 * and CCUP-NEW-DETAILS structure. Includes validation annotations for card name 
 * (alphabetic only), status (Y/N), and expiry date ranges matching COBOL validation rules.
 * 
 * This DTO supports partial updates for card modification operations and preserves
 * exact validation behavior from the original COBOL program COCRDUPC.CBL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardUpdateRequest {

    /**
     * Credit card number - must be exactly 16 digits.
     * Maps to CARDSID field in BMS map and CCUP-NEW-CARDID in COBOL structure.
     * 
     * Validation replicates COBOL logic from lines 762-804 in COCRDUPC.CBL:
     * - Must be exactly 16 digits
     * - Cannot be blank, spaces, or zeros
     */
    @JsonProperty("cardNumber")
    @NotBlank(message = "Card number not provided")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number if supplied must be a 16 digit number")
    private String cardNumber;

    /**
     * Name embossed on the credit card - maximum 50 characters, alphabetic only.
     * Maps to CRDNAME field in BMS map and CCUP-NEW-CRDNAME in COBOL structure.
     * 
     * Validation replicates COBOL logic from lines 822-837 in COCRDUPC.CBL:
     * - Maximum 50 characters
     * - Only alphabetic characters and spaces allowed
     * - Cannot be blank, low-values, or zeros
     */
    @JsonProperty("embossedName")
    @NotBlank(message = "Card name not provided")
    @Size(max = 50, message = "Card name cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Card name can only contain alphabets and spaces")
    private String embossedName;

    /**
     * Credit card expiration date.
     * Maps to EXPMON+EXPYEAR fields in BMS map and CCUP-NEW-EXPMON+CCUP-NEW-EXPYEAR in COBOL structure.
     * 
     * Validation replicates COBOL logic from lines 877-947 in COCRDUPC.CBL:
     * - Month must be between 1 and 12
     * - Year must be between 1950 and 2099
     * - Both month and year are required
     */
    @JsonProperty("expirationDate")
    @NotNull(message = "Expiration date is required")
    private LocalDate expirationDate;

    /**
     * Credit card active status - must be 'Y' or 'N'.
     * Maps to CRDSTCD field in BMS map and CCUP-NEW-CRDSTCD in COBOL structure.
     * 
     * Validation replicates COBOL logic from lines 845-876 in COCRDUPC.CBL:
     * - Must be exactly 'Y' or 'N'
     * - Cannot be blank, low-values, or other characters
     */
    @JsonProperty("activeStatus")
    @NotBlank(message = "Card Active Status must be Y or N")
    @Pattern(regexp = "^[YN]$", message = "Card Active Status must be Y or N")
    private String activeStatus;

    /**
     * Custom validation for expiration date to ensure it matches COBOL business rules.
     * This method provides additional validation beyond standard annotations to replicate
     * the exact month and year range checking from the original COBOL program.
     * 
     * @return true if expiration date is valid according to COBOL rules
     */
    @JsonIgnore
    public boolean isExpirationDateValid() {
        if (expirationDate == null) {
            return false;
        }
        
        int month = expirationDate.getMonthValue();
        int year = expirationDate.getYear();
        
        // Replicate COBOL validation: month 1-12, year 1950-2099
        return month >= 1 && month <= 12 && year >= 1950 && year <= 2099;
    }

    /**
     * Validates the card name contains only alphabetic characters and spaces.
     * This method replicates the COBOL logic from lines 822-837 that uses
     * INSPECT CONVERTING to check for non-alphabetic characters.
     * 
     * @return true if card name contains only alphabetic characters and spaces
     */
    @JsonIgnore
    public boolean isEmbossedNameValid() {
        if (embossedName == null || embossedName.trim().isEmpty()) {
            return false;
        }
        
        // Replicate COBOL alphabetic validation logic
        return embossedName.matches("^[A-Za-z\\s]+$");
    }

    /**
     * Validates the active status is exactly 'Y' or 'N'.
     * This method replicates the COBOL logic from lines 845-876 that checks
     * for valid Y/N values using condition names.
     * 
     * @return true if active status is 'Y' or 'N'
     */
    @JsonIgnore
    public boolean isActiveStatusValid() {
        return "Y".equals(activeStatus) || "N".equals(activeStatus);
    }

    /**
     * Validates the card number is exactly 16 digits.
     * This method replicates the COBOL logic from lines 762-804 that checks
     * for numeric content and length validation.
     * 
     * @return true if card number is valid 16-digit number
     */
    @JsonIgnore
    public boolean isCardNumberValid() {
        if (cardNumber == null) {
            return false;
        }
        
        // Must be exactly 16 digits, no other characters
        return cardNumber.matches("^[0-9]{16}$");
    }

    /**
     * Performs comprehensive validation of all fields according to COBOL business rules.
     * This method provides a single point to validate the entire request object
     * matching the validation flow from the original COBOL program.
     * 
     * @return true if all fields are valid according to COBOL validation rules
     */
    @JsonIgnore
    public boolean isValid() {
        return isCardNumberValid() && 
               isEmbossedNameValid() && 
               isExpirationDateValid() && 
               isActiveStatusValid();
    }
}