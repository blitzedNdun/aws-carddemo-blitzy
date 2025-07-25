-- =============================================================================
-- Liquibase Migration Script: V1__create_users_table.sql
-- =============================================================================
-- Description: Creates the users table migrated from VSAM USRSEC dataset
--              Implements BCrypt password hashing for Spring Security authentication
--              in the modernized CardDemo system
-- 
-- Migration from: VSAM USRSEC dataset with 8-character key, 80-byte records
-- Target: PostgreSQL users table with Spring Security integration

--liquibase formatted sql
-- 
-- Author: Blitzy agent
-- Created: Migration script for CardDemo modernization
-- =============================================================================

--liquibase formatted sql

--changeset CardDemo:create-users-table-v1 splitStatements:true endDelimiter:;
--comment: Create users table migrated from VSAM USRSEC dataset with Spring Security integration

-- Drop table if exists (for development/testing purposes)
DROP TABLE IF EXISTS users CASCADE;

-- Create users table with complete VSAM USRSEC dataset migration
-- Preserves VSAM key structure and access patterns while enabling modern authentication
CREATE TABLE users (
    -- Primary key: 8-character user identifier (matches VSAM KEYLEN=8)
    -- Preserves VSAM access patterns and uniqueness constraints
    user_id VARCHAR(8) NOT NULL,
    
    -- BCrypt password hash field (60 characters required for BCrypt format)
    -- Implements Spring Security BCrypt encoder integration
    -- Replaces legacy RACF password storage with modern cryptographic hashing
    password_hash VARCHAR(60) NOT NULL,
    
    -- User type for role-based access control (Admin/Regular user flag)
    -- Maps RACF user types to Spring Security authorities
    -- 'A' = Admin user (ROLE_ADMIN), 'U' = Regular user (ROLE_USER)
    user_type VARCHAR(1) NOT NULL,
    
    -- User profile information for authentication and audit purposes
    -- Maintains equivalent field lengths from VSAM record structure
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    
    -- Audit trail timestamp fields for session management and compliance
    -- Supports Spring Security authentication events and audit requirements
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    
    -- Primary key constraint definition
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- Create additional constraints for data integrity and security
-- User type constraint: Only allow 'A' (Admin) or 'U' (User) values
ALTER TABLE users ADD CONSTRAINT chk_users_user_type 
    CHECK (user_type IN ('A', 'U'));

-- Password hash constraint: Ensure BCrypt format (starts with $2a$, $2b$, $2x$, or $2y$)
-- BCrypt hash format validation for Spring Security compatibility
-- Note: Using H2-compatible REGEXP function instead of PostgreSQL ~ operator
ALTER TABLE users ADD CONSTRAINT chk_users_password_hash_format
    CHECK (REGEXP_LIKE(password_hash, '^\$2[abxy]\$[0-9]{2}\$.{53}$'));

-- User ID format constraint: Ensure uppercase alphanumeric characters only
-- Maintains VSAM USRSEC dataset key format requirements
-- Note: Using H2-compatible REGEXP function instead of PostgreSQL ~ operator
ALTER TABLE users ADD CONSTRAINT chk_users_userid_format
    CHECK (REGEXP_LIKE(user_id, '^[A-Z0-9]{1,8}$'));

-- Name constraints: Ensure proper capitalization and non-empty values
ALTER TABLE users ADD CONSTRAINT chk_users_first_name_not_empty
    CHECK (TRIM(first_name) != '');
    
ALTER TABLE users ADD CONSTRAINT chk_users_last_name_not_empty
    CHECK (TRIM(last_name) != '');

-- Create indexes for performance optimization
-- Primary B-tree index on user_id (automatically created by PRIMARY KEY)
-- Additional composite index for authentication queries
CREATE INDEX idx_users_authentication ON users (user_id, user_type, password_hash);

-- Index for user type queries (role-based filtering)
CREATE INDEX idx_users_user_type ON users (user_type);

-- Index for audit and session management queries
-- Note: H2 doesn't support partial indexes, so removing WHERE clause for compatibility
CREATE INDEX idx_users_last_login ON users (last_login DESC);

-- Index for user profile searches (case-insensitive)
-- Note: H2 doesn't support function-based indexes, so creating simple indexes instead
CREATE INDEX idx_users_first_name ON users (first_name);
CREATE INDEX idx_users_last_name ON users (last_name);

-- Add table comments for documentation
COMMENT ON TABLE users IS 'User authentication and authorization table migrated from VSAM USRSEC dataset. Implements BCrypt password hashing for Spring Security integration in the modernized CardDemo system.';

COMMENT ON COLUMN users.user_id IS 'Primary key: 8-character user identifier matching VSAM USRSEC key structure';
COMMENT ON COLUMN users.password_hash IS 'BCrypt password hash (60 chars) for Spring Security authentication';
COMMENT ON COLUMN users.user_type IS 'User type for role-based access control: A=Admin (ROLE_ADMIN), U=User (ROLE_USER)';
COMMENT ON COLUMN users.first_name IS 'User first name for profile management and audit trail';
COMMENT ON COLUMN users.last_name IS 'User last name for profile management and audit trail';
COMMENT ON COLUMN users.created_at IS 'Account creation timestamp for audit trail and compliance';
COMMENT ON COLUMN users.last_login IS 'Last successful login timestamp for session management and security monitoring';

-- Grant appropriate permissions for application access
-- Note: Actual permission grants should be environment-specific
-- GRANT SELECT, INSERT, UPDATE ON users TO carddemo_app_role;
-- GRANT SELECT ON users TO carddemo_readonly_role;

-- Insert sample data for development and testing (optional)
-- This data should be replaced with proper user provisioning in production
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    -- Admin user: password 'ADMIN123' with BCrypt hash (12 rounds)
    ('ADMIN001', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQnq', 'A', 'System', 'Administrator', CURRENT_TIMESTAMP),
    
    -- Regular user: password 'USER123' with BCrypt hash (12 rounds)  
    ('USER0001', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1MG', 'U', 'Test', 'User', CURRENT_TIMESTAMP),
    
    -- Additional test users for development
    ('TESTUSER', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWnq', 'U', 'Test', 'Account', CURRENT_TIMESTAMP);

-- Create audit trigger for tracking user modifications (optional)
-- This implements comprehensive audit trail for security compliance
-- Note: Commented out for H2 compatibility - H2 doesn't support PL/pgSQL syntax
-- PostgreSQL-specific function - enable in production PostgreSQL environment
/*
CREATE OR REPLACE FUNCTION audit_users_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Log all user modifications for security audit
    IF TG_OP = 'UPDATE' THEN
        -- Log password changes specifically for security monitoring
        IF OLD.password_hash != NEW.password_hash THEN
            INSERT INTO user_audit_log (user_id, action, old_password_hash, new_password_hash, changed_by, change_timestamp)
            VALUES (NEW.user_id, 'PASSWORD_CHANGE', OLD.password_hash, NEW.password_hash, SESSION_USER, CURRENT_TIMESTAMP);
        END IF;
        
        -- Update last_login timestamp for authentication events
        IF OLD.last_login IS DISTINCT FROM NEW.last_login THEN
            -- This is handled by the application, not the trigger
            NULL;
        END IF;
        
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO user_audit_log (user_id, action, changed_by, change_timestamp)
        VALUES (NEW.user_id, 'USER_CREATED', SESSION_USER, CURRENT_TIMESTAMP);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO user_audit_log (user_id, action, changed_by, change_timestamp)
        VALUES (OLD.user_id, 'USER_DELETED', SESSION_USER, CURRENT_TIMESTAMP);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
*/

-- Note: Audit log table creation should be in a separate migration
-- CREATE TRIGGER users_audit_trigger
--     AFTER INSERT OR UPDATE OR DELETE ON users
--     FOR EACH ROW EXECUTE FUNCTION audit_users_changes();

-- Create sequence for automated user ID generation (if needed)
-- This maintains compatibility with existing VSAM key patterns
-- CREATE SEQUENCE users_id_seq
--     START WITH 1
--     INCREMENT BY 1
--     MINVALUE 1
--     MAXVALUE 99999999
--     CACHE 1;

-- Performance optimization: Analyze table statistics after creation
-- Note: Commented out for H2 compatibility - H2 doesn't support ANALYZE
-- ANALYZE users;

-- =============================================================================
-- Migration Verification Queries
-- =============================================================================
-- The following queries can be used to verify the migration was successful:
--
-- 1. Verify table structure:
--    SELECT column_name, data_type, character_maximum_length, is_nullable 
--    FROM information_schema.columns 
--    WHERE table_name = 'users' ORDER BY ordinal_position;
--
-- 2. Verify constraints:
--    SELECT constraint_name, constraint_type 
--    FROM information_schema.table_constraints 
--    WHERE table_name = 'users';
--
-- 3. Verify indexes:
--    SELECT indexname, indexdef 
--    FROM pg_indexes 
--    WHERE tablename = 'users';
--
-- 4. Test BCrypt password validation:
--    SELECT user_id, password_hash, user_type 
--    FROM users 
--    WHERE user_id = 'ADMIN001';
--
-- 5. Verify data integrity:
--    SELECT COUNT(*) as total_users,
--           COUNT(CASE WHEN user_type = 'A' THEN 1 END) as admin_users,
--           COUNT(CASE WHEN user_type = 'U' THEN 1 END) as regular_users
--    FROM users;
-- =============================================================================

-- Rollback instructions (for development purposes):
-- DROP TABLE IF EXISTS users CASCADE;
-- DROP FUNCTION IF EXISTS audit_users_changes();
-- DROP SEQUENCE IF EXISTS users_id_seq;

-- =============================================================================
-- End of Migration Script
-- =============================================================================