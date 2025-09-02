package com.carddemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for card deletion requests containing card number, deletion reason, 
 * and confirmation flags. Supports business validation rules for card cancellation operations 
 * and maintains compatibility with COCRDDLC COBOL program input parameters.
 * 
 * This DTO supports:
 * - Card number validation with 16-digit constraint matching COBOL PIC 9(16)
 * - Required deletion reason for cancellation tracking and audit trail
 * - Confirmation flags for safety checks and balance override scenarios
 * - User identification for audit and security purposes
 * - Validation constraints matching COBOL field definitions
 * 
 * Mapped from COCRDDLC.cbl card deletion transaction requirements.
 */
@Data
public class CardDeletionRequest {

    /**
     * Credit card number to be deleted - must be exactly 16 digits to match COBOL PIC 9(16) constraint.
     * Maps to CARD-CARD-NUM field from COBOL processing structure.
     * This field is required for card identification and deletion operations.
     * 
     * Validation ensures numeric format and proper length for card number standards.
     */
    @NotBlank(message = "Card number cannot be blank")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must contain only digits")
    private String cardNumber;

    /**
     * Required reason code for card deletion to support cancellation tracking and audit trail.
     * Maps to business requirement for deletion reason documentation.
     * Common values: LOST, STOLEN, EXPIRED, CUSTOMER_REQUEST, FRAUD, ACCOUNT_CLOSED
     * 
     * This field is mandatory to ensure proper audit trail and compliance with
     * card management regulations.
     */
    @NotBlank(message = "Deletion reason is required")
    @Size(max = 50, message = "Deletion reason must not exceed 50 characters")
    private String deletionReason;

    /**
     * Confirmation flag indicating user has confirmed the deletion operation.
     * Provides safety mechanism to prevent accidental card deletions.
     * Must be explicitly set to true to proceed with deletion.
     * 
     * Maps to COBOL confirmation logic for critical operations.
     */
    @NotNull(message = "Confirmation is required")
    private Boolean confirmDeletion;

    /**
     * Force deletion flag to override outstanding balance checks.
     * When true, allows deletion even if the card has outstanding balance.
     * When false (default), deletion will be rejected if balance exists.
     * 
     * Provides administrative override capability for special circumstances
     * while maintaining financial control by default.
     */
    private Boolean forceDelete = false;

    /**
     * User ID of the person requesting the card deletion.
     * Required for audit trail and security tracking purposes.
     * Maps to COBOL user identification fields for transaction logging.
     * 
     * Must match authenticated user session for security validation.
     */
    @NotBlank(message = "Requesting user ID is required")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String requestedBy;

    /**
     * Validates that confirmation deletion flag is explicitly set to true.
     * Ensures users have actively confirmed the deletion operation rather than
     * relying on default values, providing additional safety for destructive operations.
     * 
     * @return true if deletion is confirmed, false otherwise
     */
    public boolean isDeletionConfirmed() {
        return Boolean.TRUE.equals(confirmDeletion);
    }

    /**
     * Validates that force delete flag is explicitly set to true.
     * Determines if deletion should proceed despite outstanding balances or other warnings.
     * 
     * @return true if force deletion is requested, false otherwise (default)
     */
    public boolean isForceDeleteRequested() {
        return Boolean.TRUE.equals(forceDelete);
    }

    /**
     * Validates that all required fields are present and the request is properly formed.
     * Performs comprehensive validation beyond individual field constraints to ensure
     * the deletion request meets business rules.
     * 
     * @return true if all required fields are present and valid, false otherwise
     */
    public boolean isValidRequest() {
        return cardNumber != null && !cardNumber.trim().isEmpty() &&
               deletionReason != null && !deletionReason.trim().isEmpty() &&
               requestedBy != null && !requestedBy.trim().isEmpty() &&
               isDeletionConfirmed();
    }

    /**
     * Gets masked card number for logging and display purposes.
     * Returns only the last 4 digits of the card number with the rest masked with asterisks.
     * Provides secure representation for audit logs and user confirmations.
     * 
     * @return masked card number in format ************1234, or empty string if card number is null
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return "";
        }
        return "************" + cardNumber.substring(12);
    }

    /**
     * Validates card number format using Luhn algorithm check.
     * Provides additional validation beyond format checking to ensure card number
     * meets industry standard checksum requirements.
     * 
     * @return true if card number passes Luhn algorithm validation, false otherwise
     */
    public boolean isValidCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16 || !cardNumber.matches("^[0-9]{16}$")) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}