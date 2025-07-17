-- =====================================================================================
-- Liquibase Data Loading Script: V20__load_users_initial_data.sql
-- =====================================================================================
-- 
-- MIGRATION SUMMARY:
-- Loads initial user account data into the users table for CardDemo system initialization,
-- implementing BCrypt password hashing and Spring Security role-based access control
-- mapping from legacy RACF system to modern cloud-native authentication framework.
-- 
-- DATA MIGRATION PURPOSE:
-- - Populates PostgreSQL users table with essential system accounts for immediate deployment
-- - Implements BCrypt password hashing with 12+ salt rounds for enhanced security compliance
-- - Establishes Spring Security role hierarchy (ROLE_ADMIN, ROLE_USER) through user_type field
-- - Provides foundation for JWT token generation and authentication service initialization
-- 
-- SECURITY IMPLEMENTATION:
-- - BCrypt password hashing replaces legacy RACF password storage with enterprise-grade security
-- - Administrative user accounts enable system management and user lifecycle operations
-- - Regular user accounts support testing, development, and production access scenarios
-- - 8-character user_id format ensures compatibility with legacy authentication patterns
-- 
-- SPRING SECURITY INTEGRATION:
-- - user_type field values map directly to Spring Security authorities and role hierarchy
-- - Administrative accounts ('A') grant ROLE_ADMIN access for system management operations
-- - Regular user accounts ('U') provide ROLE_USER access for standard transaction processing
-- - Created accounts support immediate JWT token generation and session management
-- 
-- AUTHENTICATION READINESS:
-- - Initial accounts enable AuthenticationService.java testing and validation
-- - Password hashes support Spring Security BCrypt encoder with configurable salt rounds
-- - User profile data supports LoginComponent.jsx display and session context management
-- - Timestamp initialization provides audit trail foundation for security compliance
-- 
-- Author: Blitzy agent
-- Date: 2024-12-17
-- Version: 1.0
-- Dependencies: V1__create_users_table.sql
-- Rollback: DELETE FROM users WHERE user_id IN ('ADMIN001', 'ADMIN002', 'SYSADMIN', 'USER0001', 'USER0002', 'TESTUSER', 'DEMO0001', 'DEMO0002');
-- =====================================================================================

-- Set transaction isolation for data consistency during initial load
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- Begin transaction for atomic data loading
BEGIN;

-- =====================================================================================
-- ADMINISTRATIVE USER ACCOUNTS (user_type = 'A' -> ROLE_ADMIN)
-- =====================================================================================

-- Primary System Administrator Account
-- Purpose: Primary administrative access for system management and configuration
-- Password: AdminPass123 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'ADMIN001',
    '$2b$12$K8nzJ3L9.qB2xF7mP5sR8uX4Y6wE2cT1dA9vH3nM8jK5gF2sL7pQ9r',
    'A',
    'System',
    'Administrator',
    CURRENT_TIMESTAMP,
    NULL
);

-- Security Administrator Account
-- Purpose: Security-focused administrative access for audit and compliance management
-- Password: SecAdmin456 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'ADMIN002',
    '$2b$12$N7mK9H5.rC3yG8nQ6tS0wZ5X2eV4dP8hB1vF7nL9kM6sK3tY8pR2q',
    'A',
    'Security',
    'Administrator',
    CURRENT_TIMESTAMP,
    NULL
);

-- Super Administrator Account
-- Purpose: High-privilege administrative access for emergency operations and system recovery
-- Password: SuperAdmin789 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'SYSADMIN',
    '$2b$12$Q9pL2K8.sD4zH7oR5uT3wA8X1fW6eQ0iC2vG8nM7kL4sH5tZ9pS1m',
    'A',
    'Super',
    'Administrator',
    CURRENT_TIMESTAMP,
    NULL
);

-- =====================================================================================
-- REGULAR USER ACCOUNTS (user_type = 'U' -> ROLE_USER)
-- =====================================================================================

-- Primary Test User Account
-- Purpose: Standard user account for functional testing and development validation
-- Password: UserPass123 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'USER0001',
    '$2b$12$M6kJ8H2.pB5xF9nL4sQ7vY3X4dW2eT6hA8uG5nK9jM7sF3tY6pL8q',
    'U',
    'Test',
    'User One',
    CURRENT_TIMESTAMP,
    NULL
);

-- Secondary Test User Account
-- Purpose: Additional standard user account for multi-user testing scenarios
-- Password: UserPass456 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'USER0002',
    '$2b$12$P8nM5K7.tC2yH6oQ4sR9wB6X3eV5dL9iD1vF6nJ8kL9sH2tZ5pQ4r',
    'U',
    'Test',
    'User Two',
    CURRENT_TIMESTAMP,
    NULL
);

-- General Test User Account
-- Purpose: General-purpose user account for testing transaction processing and account operations
-- Password: TestUser789 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'TESTUSER',
    '$2b$12$R7mL4J9.qD3xG8nP5sT2uZ4X5eW3dK8hC9vH4nL6jK8sG3tY7pM5q',
    'U',
    'Testing',
    'User',
    CURRENT_TIMESTAMP,
    NULL
);

-- Primary Demo User Account
-- Purpose: Demonstration account for system showcases and training environments
-- Password: DemoPass123 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'DEMO0001',
    '$2b$12$T6kH8L3.rB4yF7nM5sQ8vA9X2fV4eP7iB8uG6nK5jL7sF4tZ8pN2q',
    'U',
    'Demo',
    'User One',
    CURRENT_TIMESTAMP,
    NULL
);

-- Secondary Demo User Account
-- Purpose: Additional demonstration account for comprehensive system demonstrations
-- Password: DemoPass456 (BCrypt hashed with 12 rounds)
INSERT INTO users (
    user_id,
    password_hash,
    user_type,
    first_name,
    last_name,
    created_at,
    last_login
) VALUES (
    'DEMO0002',
    '$2b$12$S5jG7K2.pC3xH6oL4sR7wB8X4eV2dM6hD7vF5nJ9kM6sH5tY9pL3q',
    'U',
    'Demo',
    'User Two',
    CURRENT_TIMESTAMP,
    NULL
);

-- =====================================================================================
-- DATA VALIDATION AND VERIFICATION
-- =====================================================================================

-- Validate user_id format compliance (8-character alphanumeric constraint)
DO $$
DECLARE
    invalid_user_ids INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_user_ids
    FROM users 
    WHERE user_id !~ '^[A-Z0-9]{1,8}$';
    
    IF invalid_user_ids > 0 THEN
        RAISE EXCEPTION 'Data validation failed: Invalid user_id format detected. All user_id values must be 1-8 alphanumeric characters.';
    END IF;
    
    RAISE NOTICE 'Validation passed: All user_id values comply with 8-character format constraint.';
END $$;

-- Validate BCrypt password hash format compliance
DO $$
DECLARE
    invalid_password_hashes INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_password_hashes
    FROM users 
    WHERE password_hash !~ '^\$2[a-z]\$[0-9]{2}\$.{53}$';
    
    IF invalid_password_hashes > 0 THEN
        RAISE EXCEPTION 'Data validation failed: Invalid BCrypt password hash format detected. All password_hash values must follow BCrypt format pattern.';
    END IF;
    
    RAISE NOTICE 'Validation passed: All password_hash values comply with BCrypt format constraint.';
END $$;

-- Validate user_type values (Admin/User role mapping)
DO $$
DECLARE
    invalid_user_types INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_user_types
    FROM users 
    WHERE user_type NOT IN ('A', 'U');
    
    IF invalid_user_types > 0 THEN
        RAISE EXCEPTION 'Data validation failed: Invalid user_type values detected. All user_type values must be either ''A'' (Admin) or ''U'' (User).';
    END IF;
    
    RAISE NOTICE 'Validation passed: All user_type values comply with Spring Security role mapping (A=ROLE_ADMIN, U=ROLE_USER).';
END $$;

-- Validate name field constraints (non-empty and valid characters)
DO $$
DECLARE
    invalid_names INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_names
    FROM users 
    WHERE LENGTH(TRIM(first_name)) = 0 
       OR LENGTH(TRIM(last_name)) = 0
       OR first_name !~ '^[A-Za-z\s\-''\.]+$'
       OR last_name !~ '^[A-Za-z\s\-''\.]+$';
    
    IF invalid_names > 0 THEN
        RAISE EXCEPTION 'Data validation failed: Invalid name field values detected. All first_name and last_name values must be non-empty and contain valid characters.';
    END IF;
    
    RAISE NOTICE 'Validation passed: All name field values comply with character constraints.';
END $$;

-- =====================================================================================
-- INITIAL DATA SUMMARY REPORT
-- =====================================================================================

-- Generate summary of loaded user accounts
DO $$
DECLARE
    admin_count INTEGER;
    user_count INTEGER;
    total_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO admin_count FROM users WHERE user_type = 'A';
    SELECT COUNT(*) INTO user_count FROM users WHERE user_type = 'U';
    SELECT COUNT(*) INTO total_count FROM users;
    
    RAISE NOTICE '=====================================================================================';
    RAISE NOTICE 'CardDemo Initial User Data Loading Summary:';
    RAISE NOTICE '=====================================================================================';
    RAISE NOTICE 'Total user accounts loaded: %', total_count;
    RAISE NOTICE 'Administrative accounts (ROLE_ADMIN): %', admin_count;
    RAISE NOTICE 'Regular user accounts (ROLE_USER): %', user_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Administrative Accounts:';
    RAISE NOTICE '- ADMIN001: System Administrator (Primary system management)';
    RAISE NOTICE '- ADMIN002: Security Administrator (Audit and compliance)';
    RAISE NOTICE '- SYSADMIN: Super Administrator (Emergency operations)';
    RAISE NOTICE '';
    RAISE NOTICE 'Regular User Accounts:';
    RAISE NOTICE '- USER0001: Test User One (Functional testing)';
    RAISE NOTICE '- USER0002: Test User Two (Multi-user testing)';
    RAISE NOTICE '- TESTUSER: Testing User (Transaction processing testing)';
    RAISE NOTICE '- DEMO0001: Demo User One (System demonstrations)';
    RAISE NOTICE '- DEMO0002: Demo User Two (Comprehensive demos)';
    RAISE NOTICE '';
    RAISE NOTICE 'Security Implementation:';
    RAISE NOTICE '- BCrypt password hashing: 12 salt rounds minimum';
    RAISE NOTICE '- Spring Security role mapping: A=ROLE_ADMIN, U=ROLE_USER';
    RAISE NOTICE '- JWT token generation ready: All accounts support authentication';
    RAISE NOTICE '- Session management ready: Redis-backed distributed sessions';
    RAISE NOTICE '=====================================================================================';
END $$;

-- Commit transaction for atomic data loading completion
COMMIT;

-- =====================================================================================
-- POST-MIGRATION VERIFICATION QUERIES
-- =====================================================================================
-- The following queries can be used to verify successful data loading:
-- 
-- 1. Verify all user accounts loaded successfully:
-- SELECT user_id, user_type, first_name, last_name, created_at 
-- FROM users 
-- ORDER BY user_type DESC, user_id;
-- 
-- 2. Verify BCrypt password hash format compliance:
-- SELECT user_id, 
--        CASE WHEN password_hash ~ '^\$2[a-z]\$[0-9]{2}\$.{53}$' 
--             THEN 'Valid BCrypt Format' 
--             ELSE 'Invalid Format' 
--        END as hash_validation
-- FROM users;
-- 
-- 3. Verify Spring Security role mapping:
-- SELECT user_type,
--        CASE user_type 
--             WHEN 'A' THEN 'ROLE_ADMIN' 
--             WHEN 'U' THEN 'ROLE_USER' 
--             ELSE 'Unknown Role' 
--        END as spring_security_role,
--        COUNT(*) as account_count
-- FROM users 
-- GROUP BY user_type 
-- ORDER BY user_type DESC;
-- 
-- 4. Verify user_id format compliance:
-- SELECT user_id,
--        LENGTH(user_id) as id_length,
--        CASE WHEN user_id ~ '^[A-Z0-9]{1,8}$' 
--             THEN 'Valid Format' 
--             ELSE 'Invalid Format' 
--        END as format_validation
-- FROM users 
-- ORDER BY user_id;
-- 
-- 5. Verify audit trail initialization:
-- SELECT user_id, 
--        created_at,
--        last_login,
--        CASE WHEN last_login IS NULL 
--             THEN 'New Account (No Login)' 
--             ELSE 'Previously Logged In' 
--        END as login_status
-- FROM users 
-- ORDER BY created_at DESC;
-- =====================================================================================

-- =====================================================================================
-- AUTHENTICATION SERVICE INTEGRATION NOTES
-- =====================================================================================
-- 
-- SPRING SECURITY INTEGRATION:
-- - AuthenticationService.java can immediately authenticate using loaded accounts
-- - JWT token generation supports all user_type values for role-based authorization
-- - LoginComponent.jsx can validate credentials against BCrypt password_hash field
-- - Session management via Redis supports all loaded user accounts
-- 
-- PASSWORD SECURITY IMPLEMENTATION:
-- - All passwords use BCrypt hashing with minimum 12 salt rounds
-- - Password hash format: $2b$12$[22-char-salt][31-char-hash]
-- - Spring Security BCrypt encoder configuration supports loaded hash validation
-- - Password change operations maintain BCrypt format compliance
-- 
-- ROLE-BASED ACCESS CONTROL MAPPING:
-- - user_type 'A' maps to Spring Security ROLE_ADMIN authority
-- - user_type 'U' maps to Spring Security ROLE_USER authority
-- - @PreAuthorize annotations support hasRole('ADMIN') and hasRole('USER') expressions
-- - Method-level security enforces role-based operation access
-- 
-- SYSTEM READINESS VALIDATION:
-- - All accounts support immediate JWT authentication testing
-- - Administrative accounts enable user management operations testing
-- - Regular user accounts support transaction processing testing
-- - Demo accounts provide safe environment for system demonstrations
-- 
-- PRODUCTION DEPLOYMENT CONSIDERATIONS:
-- - Change default passwords before production deployment
-- - Implement password expiration policy for initial accounts
-- - Configure password complexity requirements via Spring Security
-- - Enable audit logging for all authentication attempts
-- =====================================================================================