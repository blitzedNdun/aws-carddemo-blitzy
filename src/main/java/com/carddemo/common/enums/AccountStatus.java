package com.carddemo.common.enums;

/**
 * Enumeration for account status values converted from COBOL ACCT-ACTIVE-STATUS field.
 * 
 * This enum maintains identical business logic as the original COBOL validation patterns
 * in COACTUPC.cbl where account status was validated using 88-level conditions:
 * - FLG-ACCT-STATUS-ISVALID VALUES 'Y', 'N'
 * 
 * The enum supports PostgreSQL database constraint validation and JPA entity mapping
 * while preserving the original VSAM ACCTDAT record structure requirements.
 *
 * @author Blitzy CardDemo Migration Team
 * @version 1.0
 * @since 2024-01-01
 */
public enum AccountStatus {
    
    /**
     * Active account status corresponding to COBOL value 'Y'
     */
    ACTIVE("Y", "Active"),
    
    /**
     * Inactive account status corresponding to COBOL value 'N'
     */
    INACTIVE("N", "Inactive");
    
    private final String code;
    private final String description;
    
    /**
     * Private constructor for enum values
     * 
     * @param code The single character code ('Y' or 'N') matching original COBOL validation
     * @param description Human-readable description of the account status
     */
    private AccountStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Get the single character code for this account status.
     * This code matches the original COBOL PIC X(1) field values.
     * 
     * @return The character code ('Y' for Active, 'N' for Inactive)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description of this account status.
     * 
     * @return The description string ("Active" or "Inactive")
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Convert a character code to the corresponding AccountStatus enum value.
     * 
     * This method replicates the original COBOL validation logic:
     * - 88 FLG-ACCT-STATUS-ISVALID VALUES 'Y', 'N'
     * 
     * @param code The single character code to convert
     * @return The corresponding AccountStatus enum value
     * @throws IllegalArgumentException if the code is not valid ('Y' or 'N')
     */
    public static AccountStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Account status code cannot be null or empty");
        }
        
        String trimmedCode = code.trim().toUpperCase();
        
        for (AccountStatus status : AccountStatus.values()) {
            if (status.code.equals(trimmedCode)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Invalid account status code: " + code + 
            ". Valid codes are 'Y' (Active) or 'N' (Inactive)");
    }
    
    /**
     * Validate if the provided code represents a valid account status.
     * 
     * This method implements the equivalent of the original COBOL validation:
     * - FLG-ACCT-STATUS-ISVALID VALUES 'Y', 'N'
     * - FLG-ACCT-STATUS-NOT-OK VALUE '0'
     * - FLG-ACCT-STATUS-BLANK VALUE 'B'
     * 
     * @param code The code to validate
     * @return true if the code is valid ('Y' or 'N'), false otherwise
     */
    public static boolean isValid(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCode = code.trim().toUpperCase();
        return "Y".equals(trimmedCode) || "N".equals(trimmedCode);
    }
    
    /**
     * Check if this account status represents an active account.
     * 
     * @return true if this is ACTIVE status, false otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Check if this account status represents an inactive account.
     * 
     * @return true if this is INACTIVE status, false otherwise
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }
    
    /**
     * String representation of the account status.
     * Returns the code for database storage compatibility.
     * 
     * @return The single character code
     */
    @Override
    public String toString() {
        return code;
    }
}