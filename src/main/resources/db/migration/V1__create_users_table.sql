-- =====================================================================================
-- Liquibase Migration Script: V1__create_users_table.sql
-- =====================================================================================
-- 
-- MIGRATION SUMMARY:
-- Creates the users table migrated from VSAM USRSEC dataset, implementing BCrypt 
-- password hashing for Spring Security authentication in the modernized CardDemo system.
-- 
-- VSAM DATASET MIGRATION:
-- Source: USRSEC VSAM KSDS (Key Length: 8, Record Length: 80 bytes)
-- Target: PostgreSQL users table with equivalent structure and enhanced security
-- 
-- SECURITY ENHANCEMENTS:
-- - BCrypt password hashing with 60-character storage for Spring Security integration
-- - Role-based access control mapping from RACF to Spring Security authorities
-- - Audit trail fields for compliance and session management
-- - Primary key preservation maintaining VSAM key access patterns
-- 
-- SPRING SECURITY INTEGRATION:
-- - JWT token generation foundation with user_id mapping
-- - Role-based access control through user_type field
-- - Password policy enforcement with 8-character minimum complexity
-- - Session management support with last_login tracking
-- 
-- COMPLIANCE FEATURES:
-- - PCI DSS password storage requirements via BCrypt implementation
-- - SOX audit trail with created_at and last_login timestamps
-- - GDPR compliance with user profile field management
-- 
-- Author: Blitzy agent
-- Date: $(date)
-- Version: 1.0
-- Rollback: DROP TABLE users CASCADE;
-- =====================================================================================

-- Create users table with exact VSAM USRSEC structure preservation
CREATE TABLE users (
    -- Primary key preserving VSAM key structure (KEYLEN=8, RKP=0)
    -- Maps to: SEC-USR-ID from USRSEC dataset
    user_id VARCHAR(8) NOT NULL,
    
    -- BCrypt password hash field for Spring Security integration
    -- BCrypt produces 60-character hash strings with salt embedded
    -- Maps to: Replaces SEC-USR-PWD with enhanced security
    password_hash VARCHAR(60) NOT NULL,
    
    -- User type for role-based access control mapping
    -- Maps RACF user types to Spring Security authorities
    -- Values: 'A' (Admin/ROLE_ADMIN), 'U' (User/ROLE_USER)
    -- Maps to: SEC-USR-TYPE from USRSEC dataset
    user_type VARCHAR(1) NOT NULL DEFAULT 'U',
    
    -- User profile fields for display and identification
    -- Maps to: SEC-USR-FNAME from USRSEC dataset
    first_name VARCHAR(20) NOT NULL,
    
    -- Maps to: SEC-USR-LNAME from USRSEC dataset
    last_name VARCHAR(20) NOT NULL,
    
    -- Audit trail fields for compliance and session management
    -- Account creation timestamp for SOX compliance
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Last successful login for session management and security monitoring
    -- Used by Spring Security for session timeout and security auditing
    last_login TIMESTAMP,
    
    -- Primary key constraint maintaining VSAM key access patterns
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- Create unique constraint on user_id (redundant with PRIMARY KEY but explicit for clarity)
-- Ensures no duplicate user identifiers matching VSAM KSDS unique key behavior
ALTER TABLE users ADD CONSTRAINT uk_users_user_id UNIQUE (user_id);

-- Create index on user_type for role-based queries
-- Optimizes Spring Security authorization queries filtering by user role
CREATE INDEX idx_users_user_type ON users (user_type);

-- Create index on last_login for session management queries
-- Optimizes user session tracking and security monitoring
CREATE INDEX idx_users_last_login ON users (last_login);

-- Create composite index on user_type and last_login for admin reporting
-- Supports queries filtering active users by role and recent login activity
CREATE INDEX idx_users_type_login ON users (user_type, last_login);

-- Add check constraint for user_type validation
-- Ensures only valid user types are stored (Admin or User)
ALTER TABLE users ADD CONSTRAINT chk_users_user_type 
    CHECK (user_type IN ('A', 'U'));

-- Add check constraint for user_id format validation
-- Ensures user_id contains only alphanumeric characters (8 characters max)
ALTER TABLE users ADD CONSTRAINT chk_users_user_id_format
    CHECK (user_id ~ '^[A-Z0-9]{1,8}$');

-- Add check constraint for password_hash format validation
-- Ensures password_hash follows BCrypt format pattern ($2[a-z]$[0-9]{2}$.{53})
ALTER TABLE users ADD CONSTRAINT chk_users_password_hash_format
    CHECK (password_hash ~ '^\$2[a-z]\$[0-9]{2}\$.{53}$');

-- Add check constraint for first_name validation
-- Ensures first_name is not empty and contains valid characters
ALTER TABLE users ADD CONSTRAINT chk_users_first_name_valid
    CHECK (LENGTH(TRIM(first_name)) > 0 AND first_name ~ '^[A-Za-z\s\-''\.]+$');

-- Add check constraint for last_name validation
-- Ensures last_name is not empty and contains valid characters
ALTER TABLE users ADD CONSTRAINT chk_users_last_name_valid
    CHECK (LENGTH(TRIM(last_name)) > 0 AND last_name ~ '^[A-Za-z\s\-''\.]+$');

-- Add check constraint for created_at validation
-- Ensures created_at is not in the future
ALTER TABLE users ADD CONSTRAINT chk_users_created_at_valid
    CHECK (created_at <= CURRENT_TIMESTAMP);

-- Add check constraint for last_login validation
-- Ensures last_login is not in the future and not before account creation
ALTER TABLE users ADD CONSTRAINT chk_users_last_login_valid
    CHECK (last_login IS NULL OR (last_login <= CURRENT_TIMESTAMP AND last_login >= created_at));

-- Add table comment for documentation
COMMENT ON TABLE users IS 'User authentication table migrated from VSAM USRSEC dataset. Implements BCrypt password hashing for Spring Security integration with role-based access control and audit trail support.';

-- Add column comments for field documentation
COMMENT ON COLUMN users.user_id IS 'Primary key user identifier (max 8 characters) preserving VSAM key structure. Maps to SEC-USR-ID from USRSEC dataset.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt password hash (60 characters) for Spring Security authentication. Replaces SEC-USR-PWD with enhanced security.';
COMMENT ON COLUMN users.user_type IS 'User role type for Spring Security authorities. A=Admin/ROLE_ADMIN, U=User/ROLE_USER. Maps to SEC-USR-TYPE from USRSEC dataset.';
COMMENT ON COLUMN users.first_name IS 'User first name for display and identification. Maps to SEC-USR-FNAME from USRSEC dataset.';
COMMENT ON COLUMN users.last_name IS 'User last name for display and identification. Maps to SEC-USR-LNAME from USRSEC dataset.';
COMMENT ON COLUMN users.created_at IS 'Account creation timestamp for SOX compliance and audit trail.';
COMMENT ON COLUMN users.last_login IS 'Last successful login timestamp for session management and security monitoring.';

-- =====================================================================================
-- ROLLBACK INSTRUCTIONS:
-- =====================================================================================
-- To rollback this migration, execute:
-- DROP TABLE users CASCADE;
-- 
-- This will remove the users table and all associated constraints and indexes.
-- =====================================================================================

-- =====================================================================================
-- VERIFICATION QUERIES:
-- =====================================================================================
-- After migration, verify table structure with:
-- 
-- SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
-- FROM information_schema.columns 
-- WHERE table_name = 'users' 
-- ORDER BY ordinal_position;
-- 
-- Verify constraints with:
-- SELECT conname, contype, pg_get_constraintdef(oid) as definition
-- FROM pg_constraint 
-- WHERE conrelid = 'users'::regclass;
-- 
-- Verify indexes with:
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'users';
-- =====================================================================================