/*
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carddemo.common.enums;

import org.springframework.security.core.GrantedAuthority;
import java.util.Optional;

/**
 * UserType enumeration defining user classification types converted from COBOL SEC-USR-TYPE field.
 * 
 * This enum replaces the original COBOL 88-level conditions for user type validation with 
 * Spring Security integration for role-based access control and JWT token generation.
 * 
 * Original COBOL field: SEC-USR-TYPE PIC X(01) from CSUSR01Y.cpy
 * Original 88-level conditions:
 * - CDEMO-USRTYP-ADMIN for administrative users (code 'A')
 * - CDEMO-USRTYP-USER for regular users (code 'R') 
 * 
 * Implementation provides:
 * - Spring Security GrantedAuthority integration for role-based access control
 * - Validation methods replicating original COBOL conditional logic  
 * - JWT token role mapping support for authentication service
 * - Type-safe parsing with null-safe validation handling
 * 
 * Usage patterns:
 * - Authentication service for JWT token generation with role claims
 * - Spring Security @PreAuthorize method-level authorization
 * - Menu navigation service for role-based UI component rendering
 * - User management service for administrative access control
 * 
 * @author AWS CardDemo Migration Team
 * @version 1.0 - Converted from COBOL SEC-USR-TYPE validation logic
 * @since Java 21, Spring Boot 3.2.x
 */
public enum UserType implements GrantedAuthority {
    
    /**
     * Administrative user type - equivalent to COBOL 88-level condition CDEMO-USRTYP-ADMIN.
     * 
     * Provides full system access including:
     * - User management operations (CRUD)
     * - System configuration access
     * - Administrative reporting and audit access
     * - All regular user functionality (inherited access)
     * 
     * Maps to Spring Security role: ROLE_ADMIN
     * Original COBOL code: 'A'
     */
    ADMIN("A", "Administrator", "ROLE_ADMIN"),
    
    /**
     * Regular user type - equivalent to COBOL 88-level condition for standard users.
     * 
     * Provides standard transaction processing access:
     * - Account viewing and management
     * - Transaction processing and history
     * - Card management operations  
     * - Bill payment functionality
     * 
     * Maps to Spring Security role: ROLE_USER
     * Original COBOL code: 'R' (Regular)
     */
    USER("R", "Regular User", "ROLE_USER");
    
    // Instance fields matching COBOL field structure and Spring Security requirements
    private final String code;              // Single character code from SEC-USR-TYPE field
    private final String description;       // Human-readable description for UI display
    private final String springRole;        // Spring Security role mapping for authorization
    
    /**
     * Private constructor initializing enum constants with COBOL field values and Spring Security mappings.
     * 
     * @param code Single character code matching original COBOL SEC-USR-TYPE values
     * @param description Human-readable description for user interface display
     * @param springRole Spring Security role name for authorization framework integration
     */
    UserType(String code, String description, String springRole) {
        this.code = code;
        this.description = description;
        this.springRole = springRole;
    }
    
    /**
     * Static factory method for parsing user type codes with null-safe validation.
     * 
     * Replicates COBOL conditional logic for user type classification with enhanced
     * error handling through Optional return type for robust validation.
     * 
     * @param code Single character user type code from database or input validation
     * @return Optional containing UserType if valid code found, empty Optional otherwise
     * 
     * Examples:
     * - fromCode("A") returns Optional.of(UserType.ADMIN)
     * - fromCode("R") returns Optional.of(UserType.USER)  
     * - fromCode("X") returns Optional.empty()
     * - fromCode(null) returns Optional.empty()
     */
    public static Optional<UserType> fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Convert to uppercase for case-insensitive matching (following COBOL convention)
        String normalizedCode = code.trim().toUpperCase();
        
        for (UserType userType : values()) {
            if (userType.code.equals(normalizedCode)) {
                return Optional.of(userType);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Returns the single character code matching original COBOL SEC-USR-TYPE field values.
     * 
     * Used for:
     * - Database persistence in PostgreSQL users table user_type column
     * - JWT token claims for client-side role determination
     * - Legacy system integration maintaining COBOL data format compatibility
     * 
     * @return Single character code ('A' for ADMIN, 'R' for USER)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns human-readable description for user interface display and logging.
     * 
     * Used for:
     * - React component user type display in administrative interfaces
     * - Audit logging with readable role descriptions
     * - Error messages and validation feedback to end users
     * 
     * @return Descriptive text for the user type classification
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Spring Security GrantedAuthority implementation returning role name for authorization framework.
     * 
     * Enables direct integration with Spring Security authorization mechanisms:
     * - @PreAuthorize("hasRole('ADMIN')") method-level security annotations
     * - JWT token role claims for stateless authentication
     * - SecurityContext role population for request processing
     * - API Gateway authorization filters and route-level security
     * 
     * @return Spring Security role name (ROLE_ADMIN or ROLE_USER)
     */
    @Override
    public String getAuthority() {
        return springRole;
    }
    
    /**
     * Convenience method creating GrantedAuthority instance for Spring Security integration.
     * 
     * Provides type-safe GrantedAuthority creation for authentication service JWT token
     * generation and Spring Security context population during request processing.
     * 
     * @return GrantedAuthority instance with role name for Spring Security framework
     */
    public GrantedAuthority asGrantedAuthority() {
        return this;
    }
    
    /**
     * Administrative privilege validation method replicating COBOL CDEMO-USRTYP-ADMIN condition.
     * 
     * Equivalent to original COBOL logic:
     * IF CDEMO-USRTYP-ADMIN
     *     PERFORM ADMIN-FUNCTION
     * END-IF
     * 
     * Used for:
     * - Method-level authorization in user management services
     * - UI component conditional rendering for administrative features
     * - Business logic branching for privilege-dependent operations
     * 
     * @return true if this user type has administrative privileges, false otherwise
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Regular user privilege validation method for standard transaction processing access.
     * 
     * Equivalent to COBOL logic checking for non-administrative user types:
     * IF NOT CDEMO-USRTYP-ADMIN
     *     PERFORM USER-FUNCTION  
     * END-IF
     * 
     * Used for:
     * - Standard transaction processing authorization
     * - UI menu generation for regular user functionality
     * - Access control for customer-facing operations
     * 
     * @return true if this user type has regular user privileges, false otherwise
     */
    public boolean isUser() {
        return this == USER;
    }
    
    /**
     * Static values() method override for enhanced enum iteration support.
     * 
     * Provides array of all UserType enum constants for:
     * - Dynamic UI dropdown population in administrative interfaces
     * - Validation logic iteration over all possible user types
     * - Configuration and testing scenarios requiring complete enum coverage
     * 
     * @return Array containing all UserType enum constants in declaration order
     */
    public static UserType[] values() {
        return UserType.values();
    }
    
    /**
     * String representation for logging, debugging, and display purposes.
     * 
     * Format: "UserType{code='A', description='Administrator', role='ROLE_ADMIN'}"
     * 
     * Used for:
     * - Structured logging in authentication and authorization events
     * - Debug output during development and troubleshooting
     * - Audit trail generation with readable user type information
     * 
     * @return Formatted string containing all enum instance properties
     */
    @Override
    public String toString() {
        return String.format("UserType{code='%s', description='%s', role='%s'}", 
                           code, description, springRole);
    }
}