-- liquibase formatted sql

-- ============================================================================
-- CardDemo Test Users Table Creation
-- ============================================================================
-- Purpose: Create PostgreSQL users table for test environment based on 
--          CSUSR01Y.cpy COBOL copybook structure with Spring Security 
--          authentication support and BCrypt password hashing
-- Environment: Test environment with Testcontainers integration
-- Source: app/cpy/CSUSR01Y.cpy (SEC-USER-DATA structure)
-- ============================================================================

-- changeset carddemo:001-create-test-users-table
-- comment: Create users table for test environment with BCrypt password support
-- labels: test-environment, authentication, spring-security
-- preconditions: onFail:HALT onError:HALT
-- precondition-sql-check: SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users' AND table_schema = 'public';
-- expected-result: 0

CREATE TABLE users (
    -- Primary key - maps to SEC-USR-ID from CSUSR01Y.cpy
    user_id VARCHAR(8) NOT NULL,
    
    -- User identification fields - maps to SEC-USR-FNAME and SEC-USR-LNAME
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    
    -- BCrypt password hash - replaces SEC-USR-PWD (8 chars) with secure hash storage
    -- BCrypt hash requires 60 characters for proper Spring Security integration
    password_hash VARCHAR(60) NOT NULL,
    
    -- User type for Spring Security role mapping - maps to SEC-USR-TYPE
    -- 'A' = Admin (ROLE_ADMIN), 'U' = User (ROLE_USER)
    user_type VARCHAR(1) NOT NULL,
    
    -- Audit fields for test environment tracking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE,
    
    -- Test environment flags
    is_test_user BOOLEAN DEFAULT TRUE,
    test_scenario VARCHAR(50),
    
    -- Primary key constraint
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- ============================================================================
-- Test Environment Constraints and Indexes
-- ============================================================================

-- User type constraint for Spring Security role mapping
ALTER TABLE users ADD CONSTRAINT chk_user_type 
    CHECK (user_type IN ('A', 'U'));

-- Password hash constraint for BCrypt validation
ALTER TABLE users ADD CONSTRAINT chk_password_hash_length 
    CHECK (LENGTH(password_hash) = 60 AND password_hash ~ '^\$2[ayb]\$[0-9]{2}\$');

-- User ID format constraint (alphanumeric, 8 characters)
ALTER TABLE users ADD CONSTRAINT chk_user_id_format 
    CHECK (LENGTH(user_id) = 8 AND user_id ~ '^[A-Z0-9]+$');

-- Name constraints for data integrity
ALTER TABLE users ADD CONSTRAINT chk_first_name_not_empty 
    CHECK (LENGTH(TRIM(first_name)) > 0);

ALTER TABLE users ADD CONSTRAINT chk_last_name_not_empty 
    CHECK (LENGTH(TRIM(last_name)) > 0);

-- Test scenario constraint for test environment
ALTER TABLE users ADD CONSTRAINT chk_test_scenario 
    CHECK (test_scenario IS NULL OR LENGTH(test_scenario) <= 50);

-- ============================================================================
-- Indexes for Authentication Service Testing
-- ============================================================================

-- Unique index on user_id for authentication lookups
CREATE UNIQUE INDEX idx_users_user_id ON users (user_id);

-- Index for user type filtering (Spring Security role queries)
CREATE INDEX idx_users_user_type ON users (user_type);

-- Index for test scenario filtering in test environment
CREATE INDEX idx_users_test_scenario ON users (test_scenario) 
    WHERE test_scenario IS NOT NULL;

-- Composite index for authentication service testing
CREATE INDEX idx_users_auth_lookup ON users (user_id, user_type, password_hash);

-- ============================================================================
-- Comments for Test Environment Documentation
-- ============================================================================

COMMENT ON TABLE users IS 'Test environment users table for Spring Security authentication testing. Maps COBOL SEC-USER-DATA structure from CSUSR01Y.cpy to PostgreSQL with BCrypt password hashing.';

COMMENT ON COLUMN users.user_id IS 'Primary key - 8 character user identifier (maps to SEC-USR-ID)';
COMMENT ON COLUMN users.first_name IS 'User first name - 20 character limit (maps to SEC-USR-FNAME)';
COMMENT ON COLUMN users.last_name IS 'User last name - 20 character limit (maps to SEC-USR-LNAME)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password - 60 character hash (replaces SEC-USR-PWD for security)';
COMMENT ON COLUMN users.user_type IS 'Spring Security role type - A=Admin, U=User (maps to SEC-USR-TYPE)';
COMMENT ON COLUMN users.created_at IS 'Account creation timestamp for audit trail';
COMMENT ON COLUMN users.last_login IS 'Last successful authentication timestamp';
COMMENT ON COLUMN users.is_test_user IS 'Test environment flag - always TRUE for test users';
COMMENT ON COLUMN users.test_scenario IS 'Test scenario identifier for integration testing';

-- ============================================================================
-- Test Data Seeding for Authentication Service Testing
-- ============================================================================

-- Test admin user with BCrypt hash for "password123"
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, test_scenario)
VALUES ('TESTADM1', 'Test', 'Admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOY9KcJvvGr7qPDwYEtOhqaJ8/CKpwMnG', 'A', 'admin-authentication');

-- Test regular user with BCrypt hash for "userpass456"
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, test_scenario)
VALUES ('TESTUSER', 'Test', 'User', '$2a$12$eUTKdYTKcOzTNB3jDnlLxOyYgfgDZJ2kDIJJvFJ8xRTNKOO3q0OhG', 'U', 'user-authentication');

-- Test user for JWT token testing
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, test_scenario)
VALUES ('JWTTEST1', 'JWT', 'TestUser', '$2a$12$GJQqJXULkJwvJHgFLCGjBeXFjLzpqXkQzFLq8FT6MNIFtJXWyEJHe', 'U', 'jwt-token-testing');

-- Test user for Spring Security integration testing
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, test_scenario)
VALUES ('SPRINGSEC', 'Spring', 'SecurityTest', '$2a$12$WXRLzQyWYXOvQgHuQJBOYenZOgSGkZjQhJKF7PUyQBkEPQdpJXO7e', 'A', 'spring-security-test');

-- Test user for role-based access control testing
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, test_scenario)
VALUES ('ROLETEST', 'Role', 'TestUser', '$2a$12$HKpKHqkZOELKMLKqmQJwJe6sWsNZLyqrLwJJKNzfhWLKHfJyQXO0e', 'U', 'rbac-testing');

-- ============================================================================
-- Test Environment Validation
-- ============================================================================

-- Validate table creation
SELECT 
    table_name, 
    column_name, 
    data_type, 
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;

-- Validate constraints
SELECT 
    constraint_name, 
    constraint_type, 
    table_name
FROM information_schema.table_constraints 
WHERE table_name = 'users';

-- Validate indexes
SELECT 
    indexname, 
    indexdef
FROM pg_indexes 
WHERE tablename = 'users';

-- Validate test data insertion
SELECT 
    user_id, 
    first_name, 
    last_name, 
    user_type, 
    test_scenario,
    created_at
FROM users 
ORDER BY user_id;

-- rollback DROP TABLE users CASCADE;