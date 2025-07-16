/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.enums;

import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;

/**
 * Enumeration defining user types for the CardDemo application.
 * 
 * This enum converts the COBOL SEC-USR-TYPE field from the legacy mainframe system
 * to Java enum values with Spring Security integration for role-based access control.
 * 
 * Original COBOL mappings from COCOM01Y.cpy:
 * - CDEMO-USRTYP-ADMIN (VALUE 'A'): Administrative user with full system access
 * - CDEMO-USRTYP-USER (VALUE 'U'): Standard user with restricted transaction access
 * 
 * Spring Security Integration:
 * - Each enum value implements GrantedAuthority for seamless role-based authorization
 * - Support for @PreAuthorize annotations on service methods
 * - JWT token claim integration for stateless authentication
 * 
 * @author AWS CardDemo Migration Team
 * @since 1.0.0
 */
public enum UserType {
    
    /**
     * Administrative user type with full system privileges.
     * 
     * Corresponds to COBOL condition: CDEMO-USRTYP-ADMIN VALUE 'A'
     * Spring Security Role: ROLE_ADMIN
     * 
     * Capabilities:
     * - Full user management operations (create, read, update, delete)
     * - System configuration and administration
     * - Access to all transaction processing functions
     * - Audit trail and reporting access
     * - Administrative menu and functionality
     */
    ADMIN("A", "Administrator", "ROLE_ADMIN"),
    
    /**
     * Standard user type with restricted transaction access.
     * 
     * Corresponds to COBOL condition: CDEMO-USRTYP-USER VALUE 'U'
     * Spring Security Role: ROLE_USER
     * 
     * Capabilities:
     * - Account viewing and management
     * - Transaction processing and history
     * - Card management operations
     * - Bill payment functionality
     * - Standard user menu access
     */
    USER("U", "User", "ROLE_USER");
    
    // Private fields for enum properties
    private final String code;
    private final String description;
    private final String springSecurityRole;
    
    /**
     * Constructor for UserType enum values.
     * 
     * @param code Single character code matching COBOL SEC-USR-TYPE field
     * @param description Human-readable description of the user type
     * @param springSecurityRole Spring Security role name for authorization
     */
    UserType(String code, String description, String springSecurityRole) {
        this.code = code;
        this.description = description;
        this.springSecurityRole = springSecurityRole;
    }
    
    /**
     * Gets the single character code for this user type.
     * 
     * This code matches the original COBOL SEC-USR-TYPE field values
     * and is used for database storage and legacy system compatibility.
     * 
     * @return Single character code ('A' for ADMIN, 'U' for USER)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the human-readable description of this user type.
     * 
     * @return Description string for display purposes
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Converts this UserType to a Spring Security GrantedAuthority.
     * 
     * This method enables seamless integration with Spring Security's
     * authentication and authorization framework, allowing UserType
     * enum values to be used directly in security contexts.
     * 
     * Example usage in JWT token claims:
     * <pre>
     * Collection&lt;GrantedAuthority&gt; authorities = 
     *     Collections.singletonList(userType.asGrantedAuthority());
     * </pre>
     * 
     * @return GrantedAuthority instance with the Spring Security role
     */
    public GrantedAuthority asGrantedAuthority() {
        return () -> springSecurityRole;
    }
    
    /**
     * Parses a single character code to the corresponding UserType enum value.
     * 
     * This method provides null-safe parsing of user type codes from
     * database records, request parameters, or legacy system data.
     * 
     * Supported codes:
     * - 'A' or 'a' → ADMIN
     * - 'U' or 'u' → USER
     * 
     * @param code Single character code to parse (case insensitive)
     * @return Optional containing the UserType if valid, empty otherwise
     */
    public static Optional<UserType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedCode = code.trim().toUpperCase();
        
        for (UserType userType : values()) {
            if (userType.code.equals(normalizedCode)) {
                return Optional.of(userType);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validates if this user type represents an administrative user.
     * 
     * This method replicates the COBOL condition logic:
     * IF CDEMO-USRTYP-ADMIN
     * 
     * @return true if this is an ADMIN user type, false otherwise
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Validates if this user type represents a standard user.
     * 
     * This method replicates the COBOL condition logic:
     * IF CDEMO-USRTYP-USER
     * 
     * @return true if this is a USER user type, false otherwise
     */
    public boolean isUser() {
        return this == USER;
    }
    
    /**
     * Returns the Spring Security role name for this user type.
     * 
     * This method provides access to the Spring Security role name
     * for use in authorization configurations and security contexts.
     * 
     * @return Spring Security role name (e.g., "ROLE_ADMIN", "ROLE_USER")
     */
    public String getSpringSecurityRole() {
        return springSecurityRole;
    }
    
    /**
     * Returns a string representation of this UserType.
     * 
     * The format includes both the code and description for
     * debugging and logging purposes.
     * 
     * @return String representation in format "CODE - Description"
     */
    @Override
    public String toString() {
        return code + " - " + description;
    }
}