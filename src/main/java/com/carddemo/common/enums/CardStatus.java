package com.carddemo.common.enums;

/**
 * Card Status enumeration for credit card lifecycle management.
 * 
 * Defines the valid states for credit card status with support for:
 * - Card authorization validation
 * - Transaction processing validation
 * - PostgreSQL constraint validation
 * - JPA entity mapping
 * - Spring Boot microservices integration
 * 
 * This enum replaces the original COBOL field validation logic while maintaining
 * identical business rules for card authorization and transaction processing.
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 2024-01-01
 */
public enum CardStatus {
    /**
     * Active card status - Card is enabled for all transactions
     * Maps to database value 'A'
     */
    ACTIVE("A", "Active", "Card is active and can be used for transactions"),
    
    /**
     * Inactive card status - Card is temporarily disabled
     * Maps to database value 'I'
     */
    INACTIVE("I", "Inactive", "Card is temporarily inactive and cannot be used"),
    
    /**
     * Blocked card status - Card is permanently blocked
     * Maps to database value 'B'
     */
    BLOCKED("B", "Blocked", "Card is blocked and cannot be used for transactions");
    
    /**
     * The single-character code stored in the database
     */
    private final String code;
    
    /**
     * The human-readable description of the status
     */
    private final String description;
    
    /**
     * The detailed explanation of the status
     */
    private final String detailedDescription;
    
    /**
     * Constructor for CardStatus enum values
     * 
     * @param code The single-character database code
     * @param description The human-readable description
     * @param detailedDescription The detailed explanation
     */
    CardStatus(String code, String description, String detailedDescription) {
        this.code = code;
        this.description = description;
        this.detailedDescription = detailedDescription;
    }
    
    /**
     * Gets the database code for this status
     * 
     * @return The single-character code ('A', 'I', or 'B')
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the human-readable description
     * 
     * @return The description string
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the detailed description
     * 
     * @return The detailed description string
     */
    public String getDetailedDescription() {
        return detailedDescription;
    }
    
    /**
     * Converts a database code to a CardStatus enum value
     * 
     * @param code The single-character database code
     * @return The corresponding CardStatus enum value
     * @throws IllegalArgumentException if the code is not valid
     */
    public static CardStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Card status code cannot be null or empty");
        }
        
        String upperCode = code.trim().toUpperCase();
        
        for (CardStatus status : CardStatus.values()) {
            if (status.code.equals(upperCode)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Invalid card status code: " + code + 
            ". Valid codes are: A (Active), I (Inactive), B (Blocked)");
    }
    
    /**
     * Validates if a given code is a valid card status
     * 
     * @param code The code to validate
     * @return true if the code is valid, false otherwise
     */
    public static boolean isValid(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        String upperCode = code.trim().toUpperCase();
        
        for (CardStatus status : CardStatus.values()) {
            if (status.code.equals(upperCode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if this card status allows transactions
     * 
     * @return true if transactions are allowed, false otherwise
     */
    public boolean isTransactionAllowed() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if this card status allows authorization
     * 
     * @return true if authorization is allowed, false otherwise
     */
    public boolean isAuthorizationAllowed() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if this card status is active
     * 
     * @return true if the card is active, false otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if this card status is inactive
     * 
     * @return true if the card is inactive, false otherwise
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }
    
    /**
     * Checks if this card status is blocked
     * 
     * @return true if the card is blocked, false otherwise
     */
    public boolean isBlocked() {
        return this == BLOCKED;
    }
    
    /**
     * Validates card status transition rules
     * 
     * @param newStatus The new status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(CardStatus newStatus) {
        if (newStatus == null) {
            return false;
        }
        
        // All transitions are allowed except blocked to active (requires manual review)
        if (this == BLOCKED && newStatus == ACTIVE) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets all valid card status codes as a comma-separated string
     * 
     * @return String containing all valid codes
     */
    public static String getValidCodes() {
        StringBuilder codes = new StringBuilder();
        for (CardStatus status : CardStatus.values()) {
            if (codes.length() > 0) {
                codes.append(", ");
            }
            codes.append(status.code);
        }
        return codes.toString();
    }
    
    /**
     * Creates a CardStatus from a legacy COBOL value
     * This method provides backward compatibility with the original COBOL system
     * that used 'Y' for active and 'N' for inactive
     * 
     * @param legacyCode The legacy COBOL code ('Y' or 'N')
     * @return The corresponding CardStatus enum value
     * @throws IllegalArgumentException if the legacy code is not valid
     */
    public static CardStatus fromLegacyCode(String legacyCode) {
        if (legacyCode == null || legacyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Legacy card status code cannot be null or empty");
        }
        
        String upperCode = legacyCode.trim().toUpperCase();
        
        switch (upperCode) {
            case "Y":
                return ACTIVE;
            case "N":
                return INACTIVE;
            default:
                throw new IllegalArgumentException("Invalid legacy card status code: " + legacyCode + 
                    ". Valid legacy codes are: Y (Active), N (Inactive)");
        }
    }
    
    /**
     * Converts this CardStatus to a legacy COBOL value
     * 
     * @return The legacy COBOL code ('Y' for active, 'N' for inactive/blocked)
     */
    public String toLegacyCode() {
        return this == ACTIVE ? "Y" : "N";
    }
    
    /**
     * Returns the string representation of this CardStatus
     * 
     * @return The code and description
     */
    @Override
    public String toString() {
        return code + " - " + description;
    }
}