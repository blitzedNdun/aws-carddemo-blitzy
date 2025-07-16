package com.carddemo.common.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Account Status enumeration for account lifecycle management.
 * 
 * Converts COBOL ACCT-ACTIVE-STATUS field validation to Java enum with
 * equivalent business logic preservation. Supports PostgreSQL database
 * constraint validation and JPA entity mapping.
 * 
 * Original COBOL field: ACCT-ACTIVE-STATUS PIC X(01)
 * Original validation: 88 FLG-ACCT-STATUS-ISVALID VALUES 'Y', 'N'
 * 
 * This enum maintains identical business logic as the original COBOL 
 * COMP-3 field validation while providing modern Java type safety
 * and integration with Spring Boot microservices architecture.
 */
public enum AccountStatus {
    
    /**
     * Active account status - equivalent to COBOL value 'Y'
     * Indicates the account is operational and available for transactions
     */
    ACTIVE("Y", "Active"),
    
    /**
     * Inactive account status - equivalent to COBOL value 'N'  
     * Indicates the account is suspended or closed for transactions
     */
    INACTIVE("N", "Inactive");
    
    private final String code;
    private final String description;
    
    /**
     * Constructor for AccountStatus enum values
     * 
     * @param code Single character code matching COBOL ACCT-ACTIVE-STATUS values
     * @param description Human-readable description of the status
     */
    AccountStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Get the single character code for database storage
     * 
     * @return Single character code ('Y' for ACTIVE, 'N' for INACTIVE)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description of the account status
     * 
     * @return Description string for display purposes
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Convert character code to AccountStatus enum
     * 
     * Equivalent to COBOL 88-level condition validation:
     * 88 FLG-ACCT-STATUS-ISVALID VALUES 'Y', 'N'
     * 
     * @param code Single character code ('Y' or 'N')
     * @return AccountStatus enum value, or null if invalid code
     */
    public static AccountStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        return Arrays.stream(AccountStatus.values())
                .filter(status -> status.code.equals(code.toUpperCase()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Validate if the provided code is a valid account status
     * 
     * Implements COBOL validation logic equivalent to:
     * IF FLG-ACCT-STATUS-ISVALID
     * 
     * @param code Character code to validate
     * @return true if code is valid ('Y' or 'N'), false otherwise
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
    
    /**
     * Check if this account status represents an active account
     * 
     * @return true if this is ACTIVE status, false otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Check if this account status represents an inactive account
     * 
     * @return true if this is INACTIVE status, false otherwise  
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }
    
    /**
     * String representation showing both code and description
     * 
     * @return Formatted string: "CODE - Description"
     */
    @Override
    public String toString() {
        return code + " - " + description;
    }
}