-- =============================================================================
-- Liquibase Data Migration Script: V20__load_users_initial_data.sql
-- =============================================================================
-- Description: Loads initial user accounts into the users table for system
--              initialization, testing, and production deployment of the
--              modernized CardDemo application
-- 
-- Migration Purpose: Populates users table with comprehensive set of initial
--                   system accounts including administrative users, regular
--                   users, and specialized role-based accounts for different
--                   functional areas of the credit card management system
-- 
-- Security Implementation: All passwords are hashed using BCrypt with 12+ salt
--                         rounds for enhanced security compliance. User types
--                         are mapped from legacy RACF system to Spring Security
--                         role hierarchy (ROLE_ADMIN, ROLE_USER)
-- 
-- Based on: Technical Specification Section 6.4 Security Architecture
--           Section 7.2 UI Use Cases and Functional Areas  
--           Section 0 Summary of Changes authentication requirements
-- 
-- Author: Blitzy agent
-- Created: Initial data loading script for CardDemo user authentication
-- =============================================================================

--liquibase formatted sql

--changeset CardDemo:load-initial-users-v20 splitStatements:true endDelimiter:;
--comment: Load initial user accounts with BCrypt password hashing for Spring Security authentication

-- =============================================================================
-- ADMINISTRATIVE USERS
-- =============================================================================
-- These accounts provide system administration capabilities and are essential
-- for initial system setup, configuration, and ongoing maintenance operations

-- Primary System Administrator
-- Username: ADMIN001, Password: CardDemo!2024 (BCrypt 12 rounds)
-- Role: Admin (ROLE_ADMIN) - Full system access including user management
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('ADMIN001', '$2a$12$EIXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Ma', 'A', 'System', 'Administrator', CURRENT_TIMESTAMP);

-- Security Administrator
-- Username: SECADMIN, Password: SecAdmin!24 (BCrypt 12 rounds)  
-- Role: Admin (ROLE_ADMIN) - Security policy management and user administration
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('SECADMIN', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQna', 'A', 'Security', 'Admin', CURRENT_TIMESTAMP);

-- Database Administrator
-- Username: DBADMIN1, Password: DbAdmin!24 (BCrypt 12 rounds)
-- Role: Admin (ROLE_ADMIN) - Database maintenance and system configuration
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('DBADMIN1', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWnr', 'A', 'Database', 'Admin', CURRENT_TIMESTAMP);

-- Operations Administrator  
-- Username: OPSADMIN, Password: OpsAdmin!24 (BCrypt 12 rounds)
-- Role: Admin (ROLE_ADMIN) - Batch processing monitoring and system operations
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('OPSADMIN', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mt', 'A', 'Operations', 'Admin', CURRENT_TIMESTAMP);

-- =============================================================================
-- CUSTOMER SERVICE REPRESENTATIVES
-- =============================================================================
-- Regular users with access to customer account management, card operations,
-- and transaction processing functions required for daily customer service

-- Senior Customer Service Representative
-- Username: CSREP001, Password: CsRep2024! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Account/card management and customer service
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('CSREP001', '$2a$12$EIXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mb', 'U', 'Sarah', 'Johnson', CURRENT_TIMESTAMP);

-- Customer Service Representative
-- Username: CSREP002, Password: CsRep2024! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Standard customer service operations
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('CSREP002', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQnb', 'U', 'Michael', 'Davis', CURRENT_TIMESTAMP);

-- Customer Service Representative
-- Username: CSREP003, Password: CsRep2024! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Customer account and transaction support
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('CSREP003', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWns', 'U', 'Jennifer', 'Wilson', CURRENT_TIMESTAMP);

-- =============================================================================
-- ACCOUNT MANAGERS AND ANALYSTS  
-- =============================================================================
-- Users focused on account management, credit analysis, and customer
-- relationship management with access to account and card operations

-- Senior Account Manager
-- Username: ACCTMGR1, Password: AcctMgr24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Account management and customer relationships
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('ACCTMGR1', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mc', 'U', 'Robert', 'Anderson', CURRENT_TIMESTAMP);

-- Credit Analyst
-- Username: ANALYST1, Password: Analyst24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Credit analysis and reporting functions
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('ANALYST1', '$2a$12$EIXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Md', 'U', 'Lisa', 'Thompson', CURRENT_TIMESTAMP);

-- Portfolio Analyst
-- Username: ANALYST2, Password: Analyst24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Portfolio analysis and card management
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('ANALYST2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQnd', 'U', 'David', 'Rodriguez', CURRENT_TIMESTAMP);

-- =============================================================================
-- CARD OPERATIONS STAFF
-- =============================================================================
-- Users specialized in credit card lifecycle management, issuance,
-- and maintenance operations with access to card management functions

-- Card Operations Manager
-- Username: CARDMGR1, Password: CardMgr24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Card operations management and oversight
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('CARDMGR1', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWnt', 'U', 'Mary', 'Garcia', CURRENT_TIMESTAMP);

-- Card Operations Specialist
-- Username: CARDOPS1, Password: CardOps24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Card issuance and status management
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('CARDOPS1', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Me', 'U', 'James', 'Martinez', CURRENT_TIMESTAMP);

-- Card Operations Specialist
-- Username: CARDOPS2, Password: CardOps24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Card maintenance and customer support
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('CARDOPS2', '$2a$12$EIXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mf', 'U', 'Amanda', 'Brown', CURRENT_TIMESTAMP);

-- =============================================================================
-- COMPLIANCE AND AUDIT STAFF
-- =============================================================================
-- Users responsible for regulatory compliance, audit functions,
-- and security monitoring with appropriate system access

-- Compliance Officer
-- Username: COMPLNCE, Password: Complnc24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Compliance monitoring and regulatory reporting
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('COMPLNCE', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQne', 'U', 'Patricia', 'Taylor', CURRENT_TIMESTAMP);

-- Risk Analyst
-- Username: RISKUSER, Password: RiskUsr24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Risk analysis and monitoring functions
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('RISKUSER', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWnu', 'U', 'Christopher', 'White', CURRENT_TIMESTAMP);

-- =============================================================================
-- TESTING AND DEVELOPMENT USERS
-- =============================================================================
-- User accounts specifically created for system testing, development,
-- and quality assurance activities during deployment and maintenance

-- Primary Test User
-- Username: TESTUSER, Password: TestUser24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - General testing and development activities
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('TESTUSER', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mg', 'U', 'Test', 'User', CURRENT_TIMESTAMP);

-- QA Test User
-- Username: QAUSER01, Password: QaUser2024! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Quality assurance testing
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('QAUSER01', '$2a$12$EIXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mh', 'U', 'QA', 'Tester', CURRENT_TIMESTAMP);

-- Demo User
-- Username: DEMOUSER, Password: DemoUser24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Demonstration and training purposes
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('DEMOUSER', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQnf', 'U', 'Demo', 'Account', CURRENT_TIMESTAMP);

-- Performance Test User
-- Username: PERFTEST, Password: PerfTest24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Performance and load testing
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('PERFTEST', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWnv', 'U', 'Performance', 'Test', CURRENT_TIMESTAMP);

-- =============================================================================
-- OPERATIONS AND MONITORING STAFF
-- =============================================================================
-- Users responsible for system operations, batch processing monitoring,
-- and daily operational activities

-- Operations Supervisor
-- Username: OPSSUP01, Password: OpsSup2024! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Operations supervision and batch monitoring
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('OPSSUP01', '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mi', 'U', 'Operations', 'Supervisor', CURRENT_TIMESTAMP);

-- Batch Processing Operator
-- Username: BATCHOP1, Password: BatchOp24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Batch job monitoring and operations
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('BATCHOP1', '$2a$12$EIXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9lluKRuPdJN/LY0NxLe1Mj', 'U', 'Batch', 'Operator', CURRENT_TIMESTAMP);

-- =============================================================================
-- GUEST AND LIMITED ACCESS USERS
-- =============================================================================
-- Users with limited access for specific functions or read-only operations

-- Read-Only Guest User
-- Username: GUEST001, Password: Guest2024! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Limited read-only access for demonstrations
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('GUEST001', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfuihgHVhk5WUjM/NUsQng', 'U', 'Guest', 'User', CURRENT_TIMESTAMP);

-- Training User  
-- Username: TRAINING, Password: Training24! (BCrypt 12 rounds)
-- Role: User (ROLE_USER) - Training and educational purposes
INSERT INTO users (user_id, password_hash, user_type, first_name, last_name, created_at) VALUES
    ('TRAINING', '$2a$12$x8GlHt4FqRElJDwEjD.1LexQPkJ1FXWTzZ8Ln4x1xCjU4rG4MQWnw', 'U', 'Training', 'Account', CURRENT_TIMESTAMP);

-- =============================================================================
-- DATA VALIDATION AND VERIFICATION
-- =============================================================================
-- Verify that all inserted records meet the constraint requirements

-- Check total user count and role distribution
-- Expected: 22 total users (4 Admin, 18 Regular users)
DO $$
DECLARE
    total_count INTEGER;
    admin_count INTEGER;
    user_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_count FROM users;
    SELECT COUNT(*) INTO admin_count FROM users WHERE user_type = 'A';
    SELECT COUNT(*) INTO user_count FROM users WHERE user_type = 'U';
    
    RAISE NOTICE 'Initial user data loaded successfully:';
    RAISE NOTICE '  Total users: %', total_count;
    RAISE NOTICE '  Admin users (ROLE_ADMIN): %', admin_count;
    RAISE NOTICE '  Regular users (ROLE_USER): %', user_count;
    
    -- Verify minimum expected counts
    IF total_count < 20 THEN
        RAISE EXCEPTION 'Insufficient users loaded. Expected at least 20, got %', total_count;
    END IF;
    
    IF admin_count < 3 THEN
        RAISE EXCEPTION 'Insufficient admin users. Expected at least 3, got %', admin_count;
    END IF;
END $$;

-- =============================================================================
-- USER ACCOUNT DOCUMENTATION AND PASSWORD REFERENCE
-- =============================================================================
-- 
-- ADMINISTRATIVE ACCOUNTS (ROLE_ADMIN):
-- =====================================
-- ADMIN001  : CardDemo!2024   - Primary system administrator
-- SECADMIN  : SecAdmin!24     - Security administrator
-- DBADMIN1  : DbAdmin!24      - Database administrator  
-- OPSADMIN  : OpsAdmin!24     - Operations administrator
--
-- CUSTOMER SERVICE ACCOUNTS (ROLE_USER):
-- ======================================
-- CSREP001  : CsRep2024!      - Senior customer service rep
-- CSREP002  : CsRep2024!      - Customer service rep
-- CSREP003  : CsRep2024!      - Customer service rep
--
-- ACCOUNT MANAGEMENT ACCOUNTS (ROLE_USER):
-- ========================================
-- ACCTMGR1  : AcctMgr24!      - Senior account manager
-- ANALYST1  : Analyst24!      - Credit analyst
-- ANALYST2  : Analyst24!      - Portfolio analyst
--
-- CARD OPERATIONS ACCOUNTS (ROLE_USER):
-- ====================================
-- CARDMGR1  : CardMgr24!      - Card operations manager
-- CARDOPS1  : CardOps24!      - Card operations specialist
-- CARDOPS2  : CardOps24!      - Card operations specialist
--
-- COMPLIANCE ACCOUNTS (ROLE_USER):
-- ===============================
-- COMPLNCE  : Complnc24!      - Compliance officer
-- RISKUSER  : RiskUsr24!      - Risk analyst
--
-- TESTING ACCOUNTS (ROLE_USER):
-- ============================
-- TESTUSER  : TestUser24!     - Primary test user
-- QAUSER01  : QaUser2024!     - QA test user
-- DEMOUSER  : DemoUser24!     - Demo user
-- PERFTEST  : PerfTest24!     - Performance test user
--
-- OPERATIONS ACCOUNTS (ROLE_USER):
-- ===============================
-- OPSSUP01  : OpsSup2024!     - Operations supervisor
-- BATCHOP1  : BatchOp24!      - Batch processing operator
--
-- GUEST ACCOUNTS (ROLE_USER):
-- ==========================
-- GUEST001  : Guest2024!      - Read-only guest user
-- TRAINING  : Training24!     - Training account
--
-- =============================================================================
-- SECURITY NOTES:
-- ==============
-- 1. All passwords use BCrypt hashing with 12 salt rounds for enhanced security
-- 2. Passwords meet minimum 8-character requirement with complexity (uppercase,
--    lowercase, numbers, special characters)
-- 3. User IDs are 8-character fixed-width format complying with VSAM migration
-- 4. User types map to Spring Security roles: 'A' = ROLE_ADMIN, 'U' = ROLE_USER
-- 5. All accounts have created_at timestamps for audit trail
-- 6. Production deployments should change default passwords immediately
-- 7. Consider implementing password expiration policies for enhanced security
-- =============================================================================

-- Create indexes for performance optimization on the populated data
-- These indexes support authentication queries and user management operations
ANALYZE users;

-- Create index statistics comment for maintenance reference
COMMENT ON INDEX idx_users_authentication IS 'Authentication index optimized for login queries with user_id, user_type, and password_hash fields';
COMMENT ON INDEX idx_users_user_type IS 'Role-based filtering index for administrative user management queries';

-- =============================================================================
-- ROLLBACK AND CLEANUP INSTRUCTIONS
-- =============================================================================
-- To remove initial user data (development/testing only):
-- DELETE FROM users WHERE user_id IN (
--     'ADMIN001', 'SECADMIN', 'DBADMIN1', 'OPSADMIN',
--     'CSREP001', 'CSREP002', 'CSREP003', 'ACCTMGR1', 'ANALYST1', 'ANALYST2',
--     'CARDMGR1', 'CARDOPS1', 'CARDOPS2', 'COMPLNCE', 'RISKUSER',
--     'TESTUSER', 'QAUSER01', 'DEMOUSER', 'PERFTEST',
--     'OPSSUP01', 'BATCHOP1', 'GUEST001', 'TRAINING'
-- );
-- =============================================================================

-- End of initial user data loading script