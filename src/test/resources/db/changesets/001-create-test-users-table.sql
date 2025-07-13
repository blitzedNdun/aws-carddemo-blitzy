-- =====================================================================
-- Liquibase Changeset: Create Test Users Table
-- Description: PostgreSQL users table creation for test environment
-- Author: Blitzy agent
-- Version: CardDemo_v1.0-15-g27d6c6f-68
-- =====================================================================
--
-- Maps COBOL SEC-USER-DATA structure from CSUSR01Y.cpy to PostgreSQL:
-- 01 SEC-USER-DATA.
--   05 SEC-USR-ID      PIC X(08) -> user_id VARCHAR(8) PRIMARY KEY
--   05 SEC-USR-FNAME   PIC X(20) -> first_name VARCHAR(20)  
--   05 SEC-USR-LNAME   PIC X(20) -> last_name VARCHAR(20)
--   05 SEC-USR-PWD     PIC X(08) -> password_hash VARCHAR(60) (BCrypt)
--   05 SEC-USR-TYPE    PIC X(01) -> user_type VARCHAR(1)
--   05 SEC-USR-FILLER  PIC X(23) -> (not mapped - COBOL filler)
--
-- Spring Security Integration:
-- - BCrypt password hashing with 60-character hash storage
-- - User type mapping for role-based authentication (A=Admin, U=User)
-- - Created/last_login timestamps for session management
-- - Test environment optimizations for Testcontainers PostgreSQL
-- =====================================================================

-- liquibase formatted sql

-- changeset blitzy-agent:001-create-test-users-table
-- comment: Create users table for test environment with Spring Security BCrypt support

-- =============================================================================
-- Main Users Table Creation
-- =============================================================================
CREATE TABLE users (
    -- Primary key mapping from COBOL SEC-USR-ID (PIC X(08))
    user_id VARCHAR(8) NOT NULL,
    
    -- BCrypt password hash storage (Spring Security requirement)
    -- Maps from COBOL SEC-USR-PWD (PIC X(08)) but stores BCrypt hash
    -- BCrypt hashes are 60 characters: $2a$10$[22 chars salt][31 chars hash]
    password_hash VARCHAR(60) NOT NULL,
    
    -- User type for role-based authorization mapping
    -- Maps from COBOL SEC-USR-TYPE (PIC X(01))
    -- Values: 'A' = Admin, 'U' = Regular User
    user_type VARCHAR(1) NOT NULL,
    
    -- User name fields mapping from COBOL structure
    -- SEC-USR-FNAME (PIC X(20)) -> first_name
    first_name VARCHAR(20) NOT NULL,
    
    -- SEC-USR-LNAME (PIC X(20)) -> last_name  
    last_name VARCHAR(20) NOT NULL,
    
    -- Audit timestamps for session management and testing
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login TIMESTAMP DEFAULT NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    
    -- Check constraints for data integrity (test environment validation)
    CONSTRAINT chk_users_user_id_format CHECK (
        user_id ~ '^[A-Z0-9]{8}$' -- 8 alphanumeric characters, uppercase
    ),
    
    CONSTRAINT chk_users_user_type_valid CHECK (
        user_type IN ('A', 'U') -- Admin or User roles only
    ),
    
    CONSTRAINT chk_users_password_hash_bcrypt CHECK (
        password_hash ~ '^\$2[abyxz]\$[0-9]{2}\$[A-Za-z0-9./]{53}$' -- BCrypt format validation
    ),
    
    CONSTRAINT chk_users_first_name_not_empty CHECK (
        LENGTH(TRIM(first_name)) > 0
    ),
    
    CONSTRAINT chk_users_last_name_not_empty CHECK (
        LENGTH(TRIM(last_name)) > 0
    )
);

-- =============================================================================
-- Indexes for Performance Optimization (Test Environment)
-- =============================================================================

-- Primary access pattern: user authentication by user_id
-- B-tree index automatically created by PRIMARY KEY constraint

-- Secondary access pattern: user type filtering for role-based queries
CREATE INDEX idx_users_user_type 
ON users (user_type);

-- Index for audit queries (useful in test scenarios)
CREATE INDEX idx_users_created_at 
ON users (created_at);

-- Index for session management queries (last login tracking)
CREATE INDEX idx_users_last_login 
ON users (last_login) 
WHERE last_login IS NOT NULL;

-- =============================================================================
-- Comments for Documentation and Maintenance
-- =============================================================================

COMMENT ON TABLE users IS 
'CardDemo users table for authentication and authorization. Maps COBOL SEC-USER-DATA structure from CSUSR01Y.cpy to PostgreSQL schema with Spring Security BCrypt integration. Test environment optimized for Testcontainers integration testing.';

COMMENT ON COLUMN users.user_id IS 
'Primary key. Maps from COBOL SEC-USR-ID (PIC X(08)). 8-character unique user identifier.';

COMMENT ON COLUMN users.password_hash IS 
'BCrypt password hash. Maps from COBOL SEC-USR-PWD (PIC X(08)) but stores 60-character BCrypt hash for Spring Security integration.';

COMMENT ON COLUMN users.user_type IS 
'User role type. Maps from COBOL SEC-USR-TYPE (PIC X(01)). Values: A=Admin, U=Regular User.';

COMMENT ON COLUMN users.first_name IS 
'User first name. Maps from COBOL SEC-USR-FNAME (PIC X(20)).';

COMMENT ON COLUMN users.last_name IS 
'User last name. Maps from COBOL SEC-USR-LNAME (PIC X(20)).';

COMMENT ON COLUMN users.created_at IS 
'Account creation timestamp. Added for audit trail and session management in Spring Security.';

COMMENT ON COLUMN users.last_login IS 
'Last successful login timestamp. Used for session management and security auditing.';

-- =============================================================================
-- Test Data Seeding for Integration Testing
-- =============================================================================

-- Test admin user with BCrypt hash for password "admin123"
-- BCrypt rounds: 10, Salt: $2a$10$N9qo8uLOickgx2ZMRZoMye
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES 
('ADMIN001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIX6HBCOQEwz0GkbWLPH.Jd.H4vX8Bqu', 'A', 'System', 'Administrator', '2024-01-01 09:00:00');

-- Test regular user with BCrypt hash for password "user123"
-- BCrypt rounds: 10, Salt: $2a$10$92IXUNpkjO0rOQ5byMi.Ye  
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES 
('USER0001', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'U', 'John', 'Doe', '2024-01-01 10:00:00');

-- Additional test user for role testing
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES 
('USER0002', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'U', 'Jane', 'Smith', '2024-01-01 11:00:00');

-- Test admin user for elevated privilege testing
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES 
('ADMIN002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIX6HBCOQEwz0GkbWLPH.Jd.H4vX8Bqu', 'A', 'Test', 'Admin', '2024-01-01 12:00:00');

-- =============================================================================
-- Grant Permissions for Test Environment
-- =============================================================================

-- Grant necessary permissions for application user (typically configured in test properties)
-- Note: In actual test environment, these would be handled by Testcontainers or test configuration

-- Permissions for application to read/write user data
-- GRANT SELECT, INSERT, UPDATE, DELETE ON users TO carddemo_app_user;

-- Permissions for monitoring/auditing (if applicable in test environment)  
-- GRANT SELECT ON users TO carddemo_readonly_user;

-- =============================================================================
-- Rollback SQL (for Liquibase rollback capability)
-- =============================================================================

--rollback DROP TABLE IF EXISTS users CASCADE;

-- =============================================================================
-- Changeset Validation and Testing Notes
-- =============================================================================

-- Test SQL Validation Queries (for manual verification):
-- 
-- 1. Verify table structure:
-- SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
-- FROM information_schema.columns 
-- WHERE table_name = 'users' AND table_schema = 'public'
-- ORDER BY ordinal_position;
--
-- 2. Verify constraints:  
-- SELECT constraint_name, constraint_type
-- FROM information_schema.table_constraints
-- WHERE table_name = 'users' AND table_schema = 'public';
--
-- 3. Verify indexes:
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'users' AND schemaname = 'public';
--
-- 4. Test authentication query pattern:
-- SELECT user_id, password_hash, user_type, first_name, last_name
-- FROM users 
-- WHERE user_id = 'USER0001';
--
-- 5. Test role-based query pattern:
-- SELECT user_id, first_name, last_name
-- FROM users 
-- WHERE user_type = 'A';

-- End of changeset: 001-create-test-users-table