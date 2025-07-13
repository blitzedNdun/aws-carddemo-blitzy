-- ============================================================================
-- Liquibase Migration: V2__create_customers_table.sql
-- Description: Create customers table migrated from VSAM CUSTDAT dataset
-- Author: Blitzy agent
-- Version: 2.0
-- ============================================================================

-- Create customers table with comprehensive field mapping from CUSTDAT VSAM dataset
-- Implements normalized address structure with PII protection and FICO validation
CREATE TABLE customers (
    -- Primary identifier: 9-digit customer ID matching VSAM CUSTDAT key structure
    customer_id VARCHAR(9) NOT NULL,
    
    -- Customer name fields preserving exact COBOL record layout
    first_name VARCHAR(20) NOT NULL,
    middle_name VARCHAR(20),
    last_name VARCHAR(20) NOT NULL,
    
    -- Normalized address structure supporting international multi-line addresses
    address_line_1 VARCHAR(50) NOT NULL,
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    address_state VARCHAR(2) NOT NULL,
    address_country VARCHAR(3) NOT NULL DEFAULT 'USA',
    address_zip VARCHAR(12) NOT NULL,
    
    -- Phone number fields supporting dual phone storage with formatted validation
    phone_home VARCHAR(15),
    phone_work VARCHAR(15),
    
    -- Personal identifiable information (PII) fields requiring encryption and access controls
    -- SSN field requires special protection per PCI DSS compliance requirements
    ssn VARCHAR(9) NOT NULL,
    
    -- Government ID field for compliance with banking regulations
    government_id VARCHAR(20) NOT NULL,
    
    -- Date of birth with appropriate constraints for age validation
    date_of_birth DATE NOT NULL,
    
    -- FICO credit score with enforced range validation (300-850)
    fico_credit_score NUMERIC(3) NOT NULL,
    
    -- Audit fields for tracking record lifecycle
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_customers PRIMARY KEY (customer_id)
);

-- Create check constraints for business rule validation
-- FICO credit score must be within valid range (300-850)
ALTER TABLE customers ADD CONSTRAINT chk_customers_fico_score_range 
    CHECK (fico_credit_score >= 300 AND fico_credit_score <= 850);

-- Customer ID must be exactly 9 numeric digits
ALTER TABLE customers ADD CONSTRAINT chk_customers_id_format 
    CHECK (customer_id ~ '^[0-9]{9}$');

-- SSN must be exactly 9 numeric digits
ALTER TABLE customers ADD CONSTRAINT chk_customers_ssn_format 
    CHECK (ssn ~ '^[0-9]{9}$');

-- State code must be 2 uppercase letters
ALTER TABLE customers ADD CONSTRAINT chk_customers_state_format 
    CHECK (address_state ~ '^[A-Z]{2}$');

-- Country code must be 2-3 uppercase letters
ALTER TABLE customers ADD CONSTRAINT chk_customers_country_format 
    CHECK (address_country ~ '^[A-Z]{2,3}$');

-- Phone number format validation (allows (XXX)XXX-XXXX or XXXXXXXXXX format)
ALTER TABLE customers ADD CONSTRAINT chk_customers_phone_home_format 
    CHECK (phone_home IS NULL OR phone_home ~ '^(\([0-9]{3}\)[0-9]{3}-[0-9]{4}|[0-9]{10})$');

ALTER TABLE customers ADD CONSTRAINT chk_customers_phone_work_format 
    CHECK (phone_work IS NULL OR phone_work ~ '^(\([0-9]{3}\)[0-9]{3}-[0-9]{4}|[0-9]{10})$');

-- ZIP code format validation (supports XXXXX or XXXXX-XXXX format)
ALTER TABLE customers ADD CONSTRAINT chk_customers_zip_format 
    CHECK (address_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

-- Date of birth must be reasonable (not in future, not too old)
ALTER TABLE customers ADD CONSTRAINT chk_customers_birth_date_range 
    CHECK (date_of_birth <= CURRENT_DATE AND date_of_birth >= '1900-01-01');

-- Name fields cannot be empty strings
ALTER TABLE customers ADD CONSTRAINT chk_customers_first_name_not_empty 
    CHECK (LENGTH(TRIM(first_name)) > 0);

ALTER TABLE customers ADD CONSTRAINT chk_customers_last_name_not_empty 
    CHECK (LENGTH(TRIM(last_name)) > 0);

-- Address line 1 cannot be empty
ALTER TABLE customers ADD CONSTRAINT chk_customers_address_line_1_not_empty 
    CHECK (LENGTH(TRIM(address_line_1)) > 0);

-- Create indexes for performance optimization
-- Primary index on customer_id is automatically created with PRIMARY KEY

-- Index for name-based customer searches
CREATE INDEX idx_customers_name ON customers (last_name, first_name);

-- Index for address-based queries
CREATE INDEX idx_customers_location ON customers (address_state, address_zip);

-- Index for date of birth queries (age-based searches)
CREATE INDEX idx_customers_birth_date ON customers (date_of_birth);

-- Index for FICO credit score range queries
CREATE INDEX idx_customers_fico_score ON customers (fico_credit_score);

-- Partial index for active customers with valid SSN
CREATE INDEX idx_customers_ssn_valid ON customers (ssn) 
    WHERE LENGTH(ssn) = 9 AND ssn ~ '^[0-9]{9}$';

-- Create trigger for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_customers_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_customers_updated_at();

-- Add table and column comments for documentation
COMMENT ON TABLE customers IS 'Customer master data migrated from VSAM CUSTDAT dataset with normalized address structure, PII protection, and comprehensive validation constraints';

COMMENT ON COLUMN customers.customer_id IS 'Primary key: 9-digit customer identifier matching VSAM CUSTDAT key structure';
COMMENT ON COLUMN customers.first_name IS 'Customer first name, maximum 20 characters';
COMMENT ON COLUMN customers.middle_name IS 'Customer middle name, optional field, maximum 20 characters';
COMMENT ON COLUMN customers.last_name IS 'Customer last name, maximum 20 characters';
COMMENT ON COLUMN customers.address_line_1 IS 'Primary address line, required field, maximum 50 characters';
COMMENT ON COLUMN customers.address_line_2 IS 'Secondary address line (apartment, suite, etc.), optional, maximum 50 characters';
COMMENT ON COLUMN customers.address_line_3 IS 'Additional address line (city name), optional, maximum 50 characters';
COMMENT ON COLUMN customers.address_state IS 'State or province code, 2-character uppercase format';
COMMENT ON COLUMN customers.address_country IS 'Country code, 2-3 character uppercase format, defaults to USA';
COMMENT ON COLUMN customers.address_zip IS 'ZIP or postal code, supports XXXXX or XXXXX-XXXX format';
COMMENT ON COLUMN customers.phone_home IS 'Home phone number, supports (XXX)XXX-XXXX format, optional';
COMMENT ON COLUMN customers.phone_work IS 'Work phone number, supports (XXX)XXX-XXXX format, optional';
COMMENT ON COLUMN customers.ssn IS 'Social Security Number, 9-digit format, requires PII protection and encryption';
COMMENT ON COLUMN customers.government_id IS 'Government-issued ID number, requires data protection compliance';
COMMENT ON COLUMN customers.date_of_birth IS 'Customer date of birth, used for age verification and compliance';
COMMENT ON COLUMN customers.fico_credit_score IS 'FICO credit score, valid range 300-850, used for credit decisions';
COMMENT ON COLUMN customers.created_at IS 'Record creation timestamp with timezone';
COMMENT ON COLUMN customers.updated_at IS 'Record last modification timestamp with timezone, automatically updated';

-- Grant appropriate permissions for application access
-- Note: Specific role permissions should be configured based on deployment environment
-- Example permissions (adjust based on actual role names in deployment):
-- GRANT SELECT, INSERT, UPDATE ON customers TO carddemo_app_role;
-- GRANT SELECT ON customers TO carddemo_read_role;
-- GRANT ALL PRIVILEGES ON customers TO carddemo_admin_role;

-- Create row-level security policy for customer data access
-- Enable row-level security for the customers table
-- ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

-- Example RLS policy (uncomment and adjust based on security requirements):
-- CREATE POLICY customers_access_policy ON customers
--     FOR ALL TO carddemo_app_role
--     USING (true); -- Replace with appropriate user context check

-- Security note: For production deployment, implement additional security measures:
-- 1. Column-level encryption for SSN and government_id fields using pgcrypto
-- 2. Row-level security policies for customer data isolation
-- 3. Audit triggers for sensitive data access logging
-- 4. Regular security reviews and access audits

-- Performance optimization notes:
-- 1. Consider partitioning by geographic region for large datasets
-- 2. Monitor query patterns and add additional indexes as needed
-- 3. Regular VACUUM and ANALYZE operations for optimal performance
-- 4. Configure appropriate connection pooling settings