package com.carddemo.common.enums;

/**
 * Account Status Enumeration
 * 
 * Converts COBOL ACCT-ACTIVE-STATUS field validation to Java enum maintaining
 * identical business logic as original COBOL COMP-3 field validation.
 * 
 * Original COBOL Implementation:
 * - Field: ACCT-ACTIVE-STATUS PIC X(01) from CVACT01Y.cpy
 * - Validation: 1220-EDIT-YESNO paragraph in COACTUPC.cbl
 * - Valid values: 'Y' (Active), 'N' (Inactive)
 * - Business rule: Account lifecycle management with PostgreSQL constraint validation
 * 
 * This enum supports:
 * - JPA entity mapping for accounts table active_status VARCHAR(1) column
 * - PostgreSQL database constraint validation via CHECK constraints
 * - Account lifecycle operations in AccountViewService and AccountUpdateService
 * - Business logic preservation from COBOL 88-level conditions
 * 
 * Performance Requirements:
 * - Validation operations must complete within sub-millisecond timeframe
 * - Database constraint integration supports 10,000+ TPS throughput
 * - Memory usage optimized for high-frequency account status checks
 * 
 * @author Blitzy Development Team
 * @version 1.0
 * @since 2024-01-01
 */
public enum AccountStatus {
    
    /**
     * Active Account Status
     * Corresponds to COBOL value 'Y' indicating an active account that can
     * process transactions and maintain normal account operations.
     * 
     * Business Rules:
     * - Account can accept new transactions
     * - Credit limits are enforced
     * - Interest calculations are performed
     * - Statement generation is enabled
     */
    ACTIVE("Y", "Active", "Account is active and fully operational"),
    
    /**
     * Inactive Account Status
     * Corresponds to COBOL value 'N' indicating an inactive account with
     * restricted functionality and suspended transaction processing.
     * 
     * Business Rules:
     * - New transactions are blocked
     * - Account balance is preserved
     * - Limited administrative operations allowed
     * - Statement generation may be suspended
     */
    INACTIVE("N", "Inactive", "Account is inactive with restricted functionality");
    
    // Instance fields matching COBOL field characteristics
    private final String code;            // Single character code matching COBOL PIC X(01)
    private final String displayName;     // Human-readable status name
    private final String description;     // Detailed business description
    
    /**
     * Private constructor for enum values
     * 
     * @param code Single character code matching COBOL ACCT-ACTIVE-STATUS values
     * @param displayName Human-readable name for UI display
     * @param description Detailed business description of the status
     */
    private AccountStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the single character code for database storage
     * 
     * Replicates the original COBOL field value exactly:
     * - 'Y' for ACTIVE status
     * - 'N' for INACTIVE status
     * 
     * Used by JPA converters and database constraint validation.
     * Performance: O(1) constant time operation.
     * 
     * @return Single character string representing the account status code
     */
    public String getCode() {
        return this.code;
    }
    
    /**
     * Gets the human-readable display name
     * 
     * Provides user-friendly status names for UI components:
     * - "Active" for operational accounts
     * - "Inactive" for suspended accounts
     * 
     * Used by React frontend components and reporting systems.
     * Performance: O(1) constant time operation.
     * 
     * @return Display name suitable for user interfaces
     */
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Gets the detailed business description
     * 
     * Provides comprehensive status explanation including business rules
     * and operational implications. Used for:
     * - Administrative interfaces
     * - Audit trail descriptions
     * - Business documentation
     * 
     * Performance: O(1) constant time operation.
     * 
     * @return Detailed description of the account status and its implications
     */
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Creates AccountStatus from single character code
     * 
     * Replicates COBOL 88-level condition validation logic from COACTUPC.cbl:
     * - Validates input against FLG-YES-NO-ISVALID conditions
     * - Supports case-insensitive matching (COBOL FUNCTION UPPER-CASE equivalent)
     * - Throws IllegalArgumentException for invalid codes (COBOL ABEND equivalent)
     * 
     * Error Handling:
     * - Null input: throws IllegalArgumentException
     * - Invalid code: throws IllegalArgumentException with COBOL error message
     * - Empty string: throws IllegalArgumentException
     * 
     * Performance: O(1) constant time lookup with enum values() iteration.
     * 
     * @param code Single character code ('Y', 'N', 'y', 'n')
     * @return AccountStatus enum corresponding to the code
     * @throws IllegalArgumentException if code is null, empty, or invalid
     */
    public static AccountStatus fromCode(String code) {
        // Validate input parameter (equivalent to COBOL null/space check)
        if (code == null) {
            throw new IllegalArgumentException("Account status code cannot be null");
        }
        
        if (code.trim().isEmpty()) {
            throw new IllegalArgumentException("Account status code cannot be empty");
        }
        
        // Normalize to uppercase for case-insensitive comparison
        // Replicates COBOL FUNCTION UPPER-CASE logic from COACTUPC.cbl line 1650
        String normalizedCode = code.trim().toUpperCase();
        
        // Validate single character requirement (COBOL PIC X(01) constraint)
        if (normalizedCode.length() != 1) {
            throw new IllegalArgumentException(
                "Account status code must be exactly one character, received: '" + code + "'"
            );
        }
        
        // Search enum values for matching code
        // Equivalent to COBOL 88-level condition evaluation
        for (AccountStatus status : AccountStatus.values()) {
            if (status.code.equals(normalizedCode)) {
                return status;
            }
        }
        
        // Generate error message matching COBOL COACTUPC.cbl line 503-504
        throw new IllegalArgumentException(
            "Account Active Status must be Y or N, received: '" + code + "'"
        );
    }
    
    /**
     * Validates if a code represents a valid account status
     * 
     * Provides boolean validation equivalent to COBOL FLG-YES-NO-ISVALID
     * condition from the 1220-EDIT-YESNO paragraph. Used for:
     * - Pre-validation before database operations
     * - Form validation in React components
     * - Batch processing validation logic
     * 
     * Performance: O(1) constant time validation using exception handling.
     * Alternative to exception-based fromCode() for boolean checks.
     * 
     * @param code Single character code to validate
     * @return true if code is valid ('Y', 'N', case-insensitive), false otherwise
     */
    public static boolean isValid(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Checks if current status is active
     * 
     * Convenience method for business logic checking equivalent to
     * COBOL condition "ACCT-ACTIVE-STATUS = 'Y'". Used extensively in:
     * - Transaction processing validation
     * - Account operation authorization
     * - Credit limit enforcement
     * - Interest calculation eligibility
     * 
     * Performance: O(1) constant time comparison.
     * 
     * @return true if account status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if current status is inactive
     * 
     * Convenience method for business logic checking equivalent to
     * COBOL condition "ACCT-ACTIVE-STATUS = 'N'". Used for:
     * - Transaction blocking logic
     * - Account restriction enforcement
     * - Administrative operation validation
     * - Reporting and audit purposes
     * 
     * Performance: O(1) constant time comparison.
     * 
     * @return true if account status is INACTIVE, false otherwise
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }
    
    /**
     * String representation for logging and debugging
     * 
     * Provides comprehensive string representation including:
     * - Enum name for Java debugging
     * - COBOL code for mainframe compatibility
     * - Display name for readability
     * 
     * Format: "ACTIVE(Y: Active)" or "INACTIVE(N: Inactive)"
     * 
     * Used by:
     * - Spring Boot logging frameworks
     * - Debugging and diagnostic tools
     * - Audit trail generation
     * - JSON serialization (when configured)
     * 
     * Performance: O(1) string concatenation operation.
     * 
     * @return Formatted string representation of the account status
     */
    @Override
    public String toString() {
        return String.format("%s(%s: %s)", name(), code, displayName);
    }
}