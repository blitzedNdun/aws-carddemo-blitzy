package com.carddemo.common.enums;

import java.util.Arrays;

/**
 * Enumeration representing card status values for credit card lifecycle management.
 * 
 * This enum supports the modernized card status codes (A/I/B) while providing
 * backward compatibility with legacy COBOL Y/N values during the migration process.
 * It maintains the business logic validation rules from the original CICS card
 * operations while enabling PostgreSQL constraint validation and JPA entity mapping.
 * 
 * Card status transitions follow the original CICS security and authorization
 * behavior patterns, ensuring identical business rule enforcement in the Spring Boot
 * microservices architecture.
 * 
 * @since CardDemo v1.0-modernization
 */
public enum CardStatus {
    
    /**
     * Active card status - card is valid and can be used for transactions.
     * Equivalent to 'Y' (Yes/Active) in the legacy COBOL system.
     * 
     * Business Rules:
     * - Card can process authorization requests
     * - All transaction types are permitted
     * - Card appears in active card listings
     * - Account relationship is fully functional
     */
    ACTIVE("A", "Active", "Card is active and available for transactions"),
    
    /**
     * Inactive card status - card is temporarily disabled but not permanently blocked.
     * Equivalent to 'N' (No/Inactive) in the legacy COBOL system.
     * 
     * Business Rules:
     * - Card cannot process authorization requests
     * - Transactions are declined at authorization
     * - Card can be reactivated by updating status
     * - Card history and account relationship are preserved
     */
    INACTIVE("I", "Inactive", "Card is temporarily inactive and cannot process transactions"),
    
    /**
     * Blocked card status - card is permanently disabled due to security concerns.
     * New status code for enhanced security management in the modernized system.
     * 
     * Business Rules:
     * - Card cannot process any authorization requests
     * - All transactions are declined immediately
     * - Requires administrative intervention to change status
     * - May indicate fraud, theft, or security compromise
     */
    BLOCKED("B", "Blocked", "Card is blocked due to security concerns or administrative action");
    
    // Instance fields
    private final String code;
    private final String displayName;
    private final String description;
    
    /**
     * Private constructor for enum constants.
     * 
     * @param code Single character code stored in database (A/I/B)
     * @param displayName Human-readable name for UI display
     * @param description Detailed explanation of the status meaning
     */
    CardStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Returns the single character database code for this card status.
     * This code is stored in the PostgreSQL cards.active_status column
     * and used for JPA entity mapping.
     * 
     * @return Single character status code (A, I, or B)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns the human-readable display name for this card status.
     * Used in user interfaces and reporting for clear status indication.
     * 
     * @return Display-friendly status name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the detailed description explaining this card status.
     * Provides comprehensive information about the status meaning and
     * business implications for documentation and user help.
     * 
     * @return Detailed status description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Creates a CardStatus enum instance from a single character code.
     * Supports both modern status codes (A/I/B) and legacy COBOL codes (Y/N)
     * to ensure backward compatibility during the migration process.
     * 
     * Legacy COBOL Mapping:
     * - 'Y' maps to ACTIVE (preserving "Yes" active logic)
     * - 'N' maps to INACTIVE (preserving "No" inactive logic)
     * 
     * Modern Status Codes:
     * - 'A' maps to ACTIVE
     * - 'I' maps to INACTIVE  
     * - 'B' maps to BLOCKED
     * 
     * @param code Single character status code (A/I/B/Y/N)
     * @return CardStatus enum instance corresponding to the code
     * @throws IllegalArgumentException if the code is not recognized
     */
    public static CardStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Card status code cannot be null or empty");
        }
        
        String normalizedCode = code.trim().toUpperCase();
        
        // Handle modern status codes (A/I/B)
        for (CardStatus status : values()) {
            if (status.code.equals(normalizedCode)) {
                return status;
            }
        }
        
        // Handle legacy COBOL status codes (Y/N) for backward compatibility
        switch (normalizedCode) {
            case "Y":
                return ACTIVE;   // Legacy 'Y' (Yes) maps to Active
            case "N":
                return INACTIVE; // Legacy 'N' (No) maps to Inactive
            default:
                throw new IllegalArgumentException(
                    "Invalid card status code: '" + code + "'. " +
                    "Valid codes are: A (Active), I (Inactive), B (Blocked), " +
                    "or legacy codes Y (Active), N (Inactive)"
                );
        }
    }
    
    /**
     * Validates whether a given status code is valid for card status operations.
     * Supports both modern (A/I/B) and legacy (Y/N) status codes.
     * 
     * This method replicates the validation logic from the original COBOL
     * program COCRDUPC.cbl section 1240-EDIT-CARDSTATUS while extending
     * support for the enhanced status codes.
     * 
     * @param code Status code to validate
     * @return true if the code represents a valid card status, false otherwise
     */
    public static boolean isValid(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        String normalizedCode = code.trim().toUpperCase();
        
        // Check modern status codes
        boolean isModernCode = Arrays.stream(values())
            .anyMatch(status -> status.code.equals(normalizedCode));
        
        // Check legacy COBOL codes for backward compatibility
        boolean isLegacyCode = "Y".equals(normalizedCode) || "N".equals(normalizedCode);
        
        return isModernCode || isLegacyCode;
    }
    
    /**
     * Checks if this card status allows transaction processing.
     * Used by card authorization validation and transaction processing services
     * to determine if a card can be used for payments.
     * 
     * Business Logic:
     * - ACTIVE cards can process all transactions
     * - INACTIVE and BLOCKED cards cannot process transactions
     * 
     * @return true if card can process transactions, false otherwise
     */
    public boolean isTransactionAllowed() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if this card status can be changed to another status.
     * Implements business rules for valid status transitions based on
     * the original CICS card operations security model.
     * 
     * Business Rules:
     * - ACTIVE cards can be changed to INACTIVE or BLOCKED
     * - INACTIVE cards can be changed to ACTIVE or BLOCKED
     * - BLOCKED cards require administrative intervention (return false for automated changes)
     * 
     * @param newStatus The target status for transition
     * @return true if transition is allowed, false otherwise
     */
    public boolean canTransitionTo(CardStatus newStatus) {
        if (newStatus == null) {
            return false;
        }
        
        // Cards can always remain in their current status
        if (this == newStatus) {
            return true;
        }
        
        switch (this) {
            case ACTIVE:
                // Active cards can be made inactive or blocked
                return newStatus == INACTIVE || newStatus == BLOCKED;
                
            case INACTIVE:
                // Inactive cards can be activated or blocked
                return newStatus == ACTIVE || newStatus == BLOCKED;
                
            case BLOCKED:
                // Blocked cards require administrative intervention
                // This preserves the security model where blocked cards need special handling
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Returns a string representation of this card status.
     * Includes both the code and display name for comprehensive identification.
     * 
     * @return String in format "CODE - DisplayName"
     */
    @Override
    public String toString() {
        return code + " - " + displayName;
    }
}