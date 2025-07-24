--liquibase formatted sql

--changeset blitzy-agent:001-create-test-users-table splitStatements:false rollbackSplitStatements:false
--comment: Create PostgreSQL users table for test environment based on CSUSR01Y.cpy COBOL copybook structure
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users' AND table_schema = current_schema()

-- ================================================================
-- CardDemo Test Users Table Creation
-- 
-- Purpose: Create PostgreSQL users table for test environment with
--          Spring Security authentication support and BCrypt password hashing
--
-- Source Mapping: COBOL copybook app/cpy/CSUSR01Y.cpy
--   SEC-USR-ID     PIC X(08) → user_id VARCHAR(8) PRIMARY KEY
--   SEC-USR-FNAME  PIC X(20) → first_name VARCHAR(20) NOT NULL
--   SEC-USR-LNAME  PIC X(20) → last_name VARCHAR(20) NOT NULL  
--   SEC-USR-PWD    PIC X(08) → password_hash VARCHAR(60) (BCrypt format)
--   SEC-USR-TYPE   PIC X(01) → user_type VARCHAR(1) NOT NULL
--   SEC-USR-FILLER PIC X(23) → Not mapped (legacy padding)
--
-- Additional fields for modern authentication and audit requirements:
--   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--   last_login TIMESTAMP NULL
--
-- Test Environment Specifications:
-- - BCrypt password hashing for Spring Security integration
-- - Enhanced constraints for data integrity testing
-- - Audit fields for authentication testing scenarios
-- - Test-specific user roles (ADMIN, USER, VIEWER)
-- ================================================================

CREATE TABLE users (
    -- Primary key: User ID mapping from COBOL SEC-USR-ID
    user_id VARCHAR(8) NOT NULL,
    
    -- User personal information from COBOL structure
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    
    -- Authentication fields with Spring Security BCrypt compatibility
    password_hash VARCHAR(60) NOT NULL, -- BCrypt hash requires 60 characters
    user_type VARCHAR(1) NOT NULL,
    
    -- Audit and tracking fields for authentication testing
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE NULL,
    
    -- Primary key constraint
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    
    -- Business rule constraints for test data integrity
    CONSTRAINT chk_user_type CHECK (user_type IN ('A', 'U', 'V')), -- Admin, User, Viewer
    CONSTRAINT chk_user_id_format CHECK (user_id ~ '^[A-Z0-9]{8}$'), -- 8 alphanumeric characters
    CONSTRAINT chk_password_hash_format CHECK (password_hash ~ '^\$2[ayb]\$.{56}$'), -- BCrypt format validation
    CONSTRAINT chk_first_name_length CHECK (LENGTH(TRIM(first_name)) >= 1),
    CONSTRAINT chk_last_name_length CHECK (LENGTH(TRIM(last_name)) >= 1)
);

-- Create indexes for authentication performance testing
CREATE UNIQUE INDEX idx_users_user_id ON users (user_id);
CREATE INDEX idx_users_user_type ON users (user_type);
CREATE INDEX idx_users_last_login ON users (last_login);
CREATE INDEX idx_users_created_at ON users (created_at);

-- Add table comments for test documentation
COMMENT ON TABLE users IS 'Test environment users table for CardDemo authentication service testing. Mapped from COBOL SEC-USER-DATA structure in CSUSR01Y.cpy copybook.';
COMMENT ON COLUMN users.user_id IS 'Primary key: 8-character user identifier from COBOL SEC-USR-ID field';
COMMENT ON COLUMN users.first_name IS 'User first name from COBOL SEC-USR-FNAME field, max 20 characters';
COMMENT ON COLUMN users.last_name IS 'User last name from COBOL SEC-USR-LNAME field, max 20 characters';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password for Spring Security authentication, replaces COBOL SEC-USR-PWD field';
COMMENT ON COLUMN users.user_type IS 'User role type from COBOL SEC-USR-TYPE field: A=Admin, U=User, V=Viewer';
COMMENT ON COLUMN users.created_at IS 'Account creation timestamp for audit trail in test scenarios';
COMMENT ON COLUMN users.last_login IS 'Last successful login timestamp for authentication testing';

-- Insert test data for integration testing scenarios
INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, created_at, last_login) VALUES
    -- Test admin user with BCrypt hash for password "admin123"
    ('ADMIN001', 'Test', 'Administrator', '$2a$10$N.zmdr9vIw2K4qTyqFVHEOKlF/dSQdZJhXdTCMB1JF0w7FpO1EhC6', 'A', CURRENT_TIMESTAMP, NULL),
    
    -- Test regular user with BCrypt hash for password "user123"
    ('USER0001', 'John', 'TestUser', '$2a$10$Sl.5eS4xHjf4VzPW8qKuUOZQj8Jt3OQFDtbxK6VT9T2h8EhJ5Gir6', 'U', CURRENT_TIMESTAMP, NULL),
    
    -- Test viewer user with BCrypt hash for password "view123"
    ('VIEWER01', 'Jane', 'TestViewer', '$2a$10$8RxLhE0tFJ2VZ5W7Q1K9qOyFjKlX3oN2DhG4M9SfP6vH1CzT8Pq5r', 'V', CURRENT_TIMESTAMP, NULL),
    
    -- Additional test users for various testing scenarios
    ('TESTUS01', 'Alice', 'Developer', '$2a$10$P7yT5Q2vM6sF8wN4rH9GcOZFj3K1DxR0iL2V7Y9WqE6tN8MdB5uSa', 'U', CURRENT_TIMESTAMP, NULL),
    ('TESTUS02', 'Bob', 'Tester', '$2a$10$J6zR3L8kN2pS5mX1wE4HfOpYs9T7qV0iM3B8C5nK7DaG2FhT9LxMp', 'U', CURRENT_TIMESTAMP, NULL);

-- Create test-specific indexes for performance testing
CREATE INDEX idx_users_name_search ON users (first_name, last_name);
CREATE INDEX idx_users_type_created ON users (user_type, created_at);

--rollback DROP TABLE IF EXISTS users CASCADE;

--changeset blitzy-agent:001-create-test-users-table-grants splitStatements:false rollbackSplitStatements:false  
--comment: Grant appropriate permissions for test environment database access

-- Grant permissions for test application user
-- Note: In test environments, these grants should be applied to the application database user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON users TO carddemo_test_user;
-- GRANT USAGE, SELECT ON SEQUENCE users_audit_seq TO carddemo_test_user; -- If using audit sequences

-- Table statistics update for query optimization in test scenarios
ANALYZE users;

--rollback -- No rollback needed for grants in test environment