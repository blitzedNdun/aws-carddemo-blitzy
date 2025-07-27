package com.carddemo.common.enums;

/**
 * CardStatus enumeration defines the valid status values for credit card lifecycle management
 * and transaction authorization validation in the CardDemo system.
 * 
 * This enum replaces the original COBOL card status field validation logic found in COCRDUPC.cbl
 * and supports the PostgreSQL card entity mapping with proper constraint validation.
 * 
 * The enum values correspond to the CARD-ACTIVE-STATUS field from CVACT02Y.cpy copybook:
 * - 'A' represents Active cards that can process transactions
 * - 'I' represents Inactive cards that are temporarily disabled
 * - 'B' represents Blocked cards that are permanently disabled due to security concerns
 * 
 * This implementation maintains identical business rules from the original COBOL validation
 * while supporting modern Spring Boot microservices architecture with JPA entity mapping.
 * 
 * Performance Requirements:
 * - Card authorization validation must complete within 200ms at 95th percentile
 * - Status validation supports 10,000 TPS transaction processing volumes
 * 
 * Integration Points:
 * - Used by CardListService for card filtering and display operations
 * - Used by CardUpdateService for card status lifecycle management
 * - Supports transaction authorization validation in payment processing
 * - Integrates with PostgreSQL constraint validation via JPA annotations
 * 
 * @author CardDemo Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public enum CardStatus {
    
    /**
     * Active status - Card is active and can be used for transactions.
     * This corresponds to 'A' in the original VSAM CARDDAT file structure.
     * Active cards pass all authorization checks and can process payments.
     */
    ACTIVE("A", "Active", "Card is active and can process transactions"),
    
    /**
     * Inactive status - Card is temporarily inactive and cannot be used for transactions.
     * This corresponds to 'I' in the original VSAM CARDDAT file structure.
     * Inactive cards are temporarily disabled but can be reactivated.
     */
    INACTIVE("I", "Inactive", "Card is temporarily inactive and cannot process transactions"),
    
    /**
     * Blocked status - Card is permanently blocked due to security concerns.
     * This corresponds to 'B' in the original VSAM CARDDAT file structure.
     * Blocked cards cannot be used for transactions and require special handling.
     */
    BLOCKED("B", "Blocked", "Card is blocked due to security concerns and cannot process transactions");
    
    /**
     * Single character code used in database storage and COBOL compatibility.
     * This matches the PIC X(01) field definition from CVACT02Y.cpy copybook.
     */
    private final String code;
    
    /**
     * Human-readable display name for the status.
     * Used in user interfaces and reports.
     */
    private final String displayName;
    
    /**
     * Detailed description of the status for documentation and logging.
     * Provides clear explanation of business rules for each status.
     */
    private final String description;
    
    /**
     * Private constructor to initialize enum constants with their properties.
     * 
     * @param code Single character code for database storage
     * @param displayName Human-readable name for UI display
     * @param description Detailed explanation of the status
     */
    private CardStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Returns the single character code for database storage.
     * This method supports JPA entity mapping and PostgreSQL constraint validation.
     * 
     * The returned code matches the original COBOL field format:
     * - 'A' for Active cards
     * - 'I' for Inactive cards  
     * - 'B' for Blocked cards
     * 
     * @return Single character status code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns the human-readable display name for the status.
     * This method supports user interface components and reporting functions.
     * 
     * @return Display name suitable for user interfaces
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the detailed description of the status.
     * This method provides comprehensive explanation of business rules
     * and is used for documentation, logging, and error messages.
     * 
     * @return Detailed description of the status meaning and usage
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Creates a CardStatus enum from a single character code.
     * This method replaces the original COBOL validation logic from COCRDUPC.cbl
     * and supports conversion from database values to enum constants.
     * 
     * The method performs case-insensitive matching and handles null/empty inputs
     * gracefully by returning null, allowing calling code to handle invalid states.
     * 
     * Valid input codes:
     * - 'A' or 'a' returns CardStatus.ACTIVE
     * - 'I' or 'i' returns CardStatus.INACTIVE
     * - 'B' or 'b' returns CardStatus.BLOCKED
     * - null, empty, or invalid codes return null
     * 
     * @param code Single character status code from database or user input
     * @return CardStatus enum constant or null if code is invalid
     */
    public static CardStatus fromCode(String code) {
        // Handle null or empty input gracefully
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        // Convert to uppercase for case-insensitive comparison
        String normalizedCode = code.trim().toUpperCase();
        
        // Match against valid status codes
        switch (normalizedCode) {
            case "A":
                return ACTIVE;
            case "I":
                return INACTIVE;
            case "B":
                return BLOCKED;
            default:
                return null;
        }
    }
    
    /**
     * Validates if a given code represents a valid card status.
     * This method replaces the FLG-YES-NO-VALID validation logic from COCRDUPC.cbl
     * and provides comprehensive input validation for card status fields.
     * 
     * The validation logic performs:
     * - Null and empty string handling
     * - Case-insensitive comparison
     * - Exact match against valid status codes ('A', 'I', 'B')
     * 
     * @param code Single character status code to validate
     * @return true if code represents a valid card status, false otherwise
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
    
    /**
     * Determines if the card status allows transaction processing.
     * This method implements the business logic for card authorization validation
     * and supports real-time transaction processing decisions.
     * 
     * Authorization Rules:
     * - ACTIVE cards can process all transaction types
     * - INACTIVE cards cannot process any transactions
     * - BLOCKED cards cannot process any transactions
     * 
     * This method supports the 200ms authorization response time requirement
     * by providing O(1) lookup performance for transaction validation.
     * 
     * @return true if card can process transactions, false otherwise
     */
    public boolean canProcessTransactions() {
        return this == ACTIVE;
    }
    
    /**
     * Determines if the card status can be changed to another status.
     * This method implements card lifecycle management business rules
     * and supports CardUpdateService operations.
     * 
     * Status Transition Rules:
     * - ACTIVE cards can be changed to INACTIVE or BLOCKED
     * - INACTIVE cards can be changed to ACTIVE or BLOCKED
     * - BLOCKED cards cannot be changed to any other status (permanent)
     * 
     * @param newStatus Target status for the transition
     * @return true if status transition is allowed, false otherwise
     */
    public boolean canTransitionTo(CardStatus newStatus) {
        if (newStatus == null) {
            return false;
        }
        
        // Blocked cards cannot be transitioned to any other status
        if (this == BLOCKED) {
            return false;
        }
        
        // Active and Inactive cards can transition to any status
        return true;
    }
    
    /**
     * Returns a string representation of the card status.
     * This method provides a formatted display suitable for logging and debugging.
     * 
     * Format: "CardStatus{code='A', displayName='Active'}"
     * 
     * @return Formatted string representation of the status
     */
    @Override
    public String toString() {
        return String.format("CardStatus{code='%s', displayName='%s'}", code, displayName);
    }
}