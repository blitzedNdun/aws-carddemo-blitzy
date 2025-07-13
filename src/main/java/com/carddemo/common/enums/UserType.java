/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.enums;

import org.springframework.security.core.GrantedAuthority;
import java.util.Optional;

/**
 * User type enumeration converted from COBOL SEC-USR-TYPE field validation.
 * 
 * This enum implements the original COBOL 88-level conditions:
 * - CDEMO-USRTYP-ADMIN VALUE 'A' (Admin user type)
 * - CDEMO-USRTYP-USER VALUE 'U' (Regular user type)
 * 
 * Integrated with Spring Security for role-based access control and JWT token generation.
 * Maintains exact functional equivalence with original CICS RACF user profile patterns.
 * 
 * Original COBOL reference:
 * - SEC-USR-TYPE PIC X(01) from CSUSR01Y.cpy
 * - 88-level conditions in COCOM01Y.cpy
 * 
 * @author Blitzy agent
 * @version 1.0
 * @since 2024-01-01
 */
public enum UserType implements GrantedAuthority {
    
    /**
     * Administrator user type - equivalent to CDEMO-USRTYP-ADMIN VALUE 'A'
     * Provides full system access including user management and administrative operations.
     * Maps to Spring Security ROLE_ADMIN authority.
     */
    ADMIN("A", "Administrator", "ROLE_ADMIN"),
    
    /**
     * Regular user type - equivalent to CDEMO-USRTYP-USER VALUE 'U'
     * Provides standard transaction processing access with restricted administrative functions.
     * Maps to Spring Security ROLE_USER authority.
     */
    USER("U", "Regular User", "ROLE_USER");
    
    // Private fields matching COBOL structure
    private final String code;
    private final String description;
    private final String springRole;
    
    /**
     * Constructor for UserType enum values.
     * 
     * @param code Single character code matching COBOL SEC-USR-TYPE field
     * @param description Human-readable description of the user type
     * @param springRole Spring Security role authority string
     */
    UserType(String code, String description, String springRole) {
        this.code = code;
        this.description = description;
        this.springRole = springRole;
    }
    
    /**
     * Parse user type from COBOL character code with null-safe handling.
     * Replicates original COBOL conditional logic for user role authorization.
     * 
     * @param code Single character code ('A' for Admin, 'U' for User)
     * @return Optional containing the matching UserType, or empty if invalid
     */
    public static Optional<UserType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Convert to uppercase to match COBOL behavior
        String upperCode = code.trim().toUpperCase();
        
        for (UserType userType : values()) {
            if (userType.code.equals(upperCode)) {
                return Optional.of(userType);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the COBOL-compatible single character code.
     * 
     * @return Single character code ('A' for Admin, 'U' for User)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get human-readable description of the user type.
     * 
     * @return Description string for display purposes
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get Spring Security GrantedAuthority for this user type.
     * Enables integration with Spring Security role-based access control.
     * 
     * @return GrantedAuthority instance for JWT token generation and authorization
     */
    public GrantedAuthority asGrantedAuthority() {
        return new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return springRole;
            }
        };
    }
    
    /**
     * Check if this user type represents an administrator.
     * Equivalent to COBOL: IF CDEMO-USRTYP-ADMIN
     * 
     * @return true if this is an admin user type, false otherwise
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Check if this user type represents a regular user.
     * Equivalent to COBOL: IF CDEMO-USRTYP-USER
     * 
     * @return true if this is a regular user type, false otherwise
     */
    public boolean isUser() {
        return this == USER;
    }
    
    /**
     * Implementation of GrantedAuthority interface for Spring Security integration.
     * Enables direct use of UserType enum in Spring Security contexts.
     * 
     * @return Spring Security role authority string
     */
    @Override
    public String getAuthority() {
        return springRole;
    }
    
    /**
     * Validate user type code and convert to UserType enum.
     * Provides explicit validation with error handling for invalid codes.
     * 
     * @param code Character code to validate
     * @return UserType enum value
     * @throws IllegalArgumentException if code is null, empty, or invalid
     */
    public static UserType validateAndConvert(String code) {
        Optional<UserType> result = fromCode(code);
        if (result.isPresent()) {
            return result.get();
        }
        
        throw new IllegalArgumentException(
            "Invalid user type code: '" + code + "'. Expected 'A' for Admin or 'U' for User."
        );
    }
    
    /**
     * Check if the provided code represents an admin user type.
     * Utility method for direct code validation without enum conversion.
     * 
     * @param code Character code to check
     * @return true if code represents admin user type, false otherwise
     */
    public static boolean isAdminCode(String code) {
        return fromCode(code).map(UserType::isAdmin).orElse(false);
    }
    
    /**
     * Check if the provided code represents a regular user type.
     * Utility method for direct code validation without enum conversion.
     * 
     * @param code Character code to check
     * @return true if code represents regular user type, false otherwise
     */
    public static boolean isUserCode(String code) {
        return fromCode(code).map(UserType::isUser).orElse(false);
    }
    
    /**
     * Get Spring Security role name for the given code.
     * Convenience method for JWT token generation and security context setup.
     * 
     * @param code Character code to convert
     * @return Spring Security role name, or null if invalid code
     */
    public static String getSpringRoleForCode(String code) {
        return fromCode(code).map(userType -> userType.springRole).orElse(null);
    }
    
    /**
     * String representation showing both code and description.
     * Useful for logging and debugging purposes.
     * 
     * @return String in format "CODE: Description"
     */
    @Override
    public String toString() {
        return code + ": " + description;
    }
}