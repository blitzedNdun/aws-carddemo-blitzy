-- ==============================================================================
-- Liquibase Migration: V2__create_customers_table.sql
-- Description: Creates customers table migrated from VSAM CUSTDAT dataset
-- Author: Blitzy agent
-- Version: 2.0
-- Migration Type: CREATE TABLE with comprehensive constraints and indexes
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:create-customers-table-v2
--comment: Create customers table with normalized address structure, PII protection, and FICO validation

-- Create customers table preserving VSAM CUSTDAT record layout with modern PostgreSQL enhancements
CREATE TABLE customers (
    -- Primary key: 9-digit customer identifier matching VSAM CUSTDAT key structure
    customer_id VARCHAR(9) NOT NULL,
    
    -- Customer name fields with exact field lengths from CUSTDAT record structure
    first_name VARCHAR(20) NOT NULL,
    middle_name VARCHAR(20),
    last_name VARCHAR(20) NOT NULL,
    
    -- Normalized address structure supporting international compatibility
    -- Expanded from COBOL fixed-width to flexible VARCHAR with proper constraints
    address_line_1 VARCHAR(50) NOT NULL,
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    state_code VARCHAR(2) NOT NULL,
    country_code VARCHAR(3) NOT NULL DEFAULT 'USA',
    zip_code VARCHAR(10) NOT NULL,
    
    -- Dual phone number support with formatted validation constraints
    phone_number_1 VARCHAR(15),
    phone_number_2 VARCHAR(15),
    
    -- Personal identifiable information (PII) fields requiring encryption and access controls
    -- SSN field maintains COBOL 9-digit format with additional protection annotations
    ssn VARCHAR(9) NOT NULL,
    -- Government ID field supporting various ID types with 20-character capacity
    government_id VARCHAR(20) NOT NULL,
    
    -- Date of birth with appropriate constraints for customer validation
    date_of_birth DATE NOT NULL,
    
    -- Electronic Funds Transfer account identifier from CUSTDAT position 287-296
    eft_account_id VARCHAR(10),
    
    -- Primary cardholder indicator from CUSTDAT position 297 (Y/N flag)
    primary_cardholder_indicator VARCHAR(1) NOT NULL DEFAULT 'Y',
    
    -- FICO credit score with strict range validation (300-850 per industry standard)
    fico_credit_score NUMERIC(3) NOT NULL,
    
    -- Audit fields for data management and compliance
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    
    -- Business validation constraints
    -- Note: H2 uses REGEXP_LIKE instead of ~ operator
    CONSTRAINT chk_customer_id_format CHECK (REGEXP_LIKE(customer_id, '^[0-9]{9}$')),
    CONSTRAINT chk_first_name_not_empty CHECK (LENGTH(TRIM(first_name)) > 0),
    CONSTRAINT chk_last_name_not_empty CHECK (LENGTH(TRIM(last_name)) > 0),
    CONSTRAINT chk_address_line_1_not_empty CHECK (LENGTH(TRIM(address_line_1)) > 0),
    CONSTRAINT chk_state_code_format CHECK (REGEXP_LIKE(state_code, '^[A-Z]{2}$')),
    CONSTRAINT chk_country_code_format CHECK (REGEXP_LIKE(country_code, '^[A-Z]{3}$')),
    CONSTRAINT chk_zip_code_not_empty CHECK (LENGTH(TRIM(zip_code)) > 0),
    
    -- Phone number format validation (allows various standard formats)
    CONSTRAINT chk_phone_number_1_format CHECK (
        phone_number_1 IS NULL OR 
        REGEXP_LIKE(phone_number_1, '^\([0-9]{3}\)[0-9]{3}-[0-9]{4}$|^[0-9]{3}-[0-9]{3}-[0-9]{4}$|^[0-9]{10}$')
    ),
    CONSTRAINT chk_phone_number_2_format CHECK (
        phone_number_2 IS NULL OR 
        REGEXP_LIKE(phone_number_2, '^\([0-9]{3}\)[0-9]{3}-[0-9]{4}$|^[0-9]{3}-[0-9]{3}-[0-9]{4}$|^[0-9]{10}$')
    ),
    
    -- PII field validation constraints
    CONSTRAINT chk_ssn_format CHECK (REGEXP_LIKE(ssn, '^[0-9]{9}$')),
    CONSTRAINT chk_government_id_not_empty CHECK (LENGTH(TRIM(government_id)) > 0),
    
    -- Date of birth business rules (reasonable age range for customers)
    -- Note: H2 uses DATEADD function instead of INTERVAL syntax
    CONSTRAINT chk_date_of_birth_range CHECK (
        date_of_birth >= DATE '1900-01-01' AND 
        date_of_birth <= DATEADD(YEAR, -13, CURRENT_DATE)
    ),
    
    -- Primary cardholder indicator validation (Y/N values only)
    CONSTRAINT chk_primary_cardholder_format CHECK (REGEXP_LIKE(primary_cardholder_indicator, '^[YN]$')),
    
    -- FICO credit score validation with industry standard range (300-850)
    CONSTRAINT chk_fico_score_range CHECK (
        fico_credit_score >= 300 AND 
        fico_credit_score <= 850
    ),
    
    -- EFT account ID format validation when provided
    CONSTRAINT chk_eft_account_format CHECK (
        eft_account_id IS NULL OR 
        LENGTH(TRIM(eft_account_id)) > 0
    )
);

--rollback DROP TABLE customers CASCADE;

--changeset blitzy-agent:create-customers-table-indexes-v2
--comment: Create indexes for customers table optimizing query performance

-- Primary index on customer_id (automatically created with PRIMARY KEY)
-- Additional indexes for common query patterns and foreign key relationships

-- Index for customer name searches (supporting full-text customer lookup)
CREATE INDEX idx_customers_name_search ON customers (last_name, first_name, middle_name);

-- Index for address-based queries (supporting geographic analysis)
CREATE INDEX idx_customers_address_location ON customers (state_code, country_code, zip_code);

-- Index for phone number lookups (supporting customer service operations)
CREATE INDEX idx_customers_phone_lookup ON customers (phone_number_1, phone_number_2) WHERE phone_number_1 IS NOT NULL OR phone_number_2 IS NOT NULL;

-- Index for date of birth queries (supporting age-based analysis)
CREATE INDEX idx_customers_birth_date ON customers (date_of_birth);

-- Index for FICO score analysis (supporting credit analysis)
CREATE INDEX idx_customers_fico_score ON customers (fico_credit_score);

-- Index for EFT account relationships (supporting payment processing)
CREATE INDEX idx_customers_eft_account ON customers (eft_account_id) WHERE eft_account_id IS NOT NULL;

-- Composite index for cardholder status queries
CREATE INDEX idx_customers_cardholder_status ON customers (primary_cardholder_indicator, fico_credit_score);

--rollback DROP INDEX IF EXISTS idx_customers_name_search;
--rollback DROP INDEX IF EXISTS idx_customers_address_location;
--rollback DROP INDEX IF EXISTS idx_customers_phone_lookup;
--rollback DROP INDEX IF EXISTS idx_customers_birth_date;
--rollback DROP INDEX IF EXISTS idx_customers_fico_score;
--rollback DROP INDEX IF EXISTS idx_customers_eft_account;
--rollback DROP INDEX IF EXISTS idx_customers_cardholder_status;

--changeset blitzy-agent:create-customers-table-triggers-v2
--comment: Create triggers for customers table audit trail and data integrity

-- Trigger function for automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION update_customers_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at on row modifications
CREATE TRIGGER trg_customers_update_timestamp
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_customers_updated_at();

--rollbook DROP TRIGGER IF EXISTS trg_customers_update_timestamp ON customers;
--rollback DROP FUNCTION IF EXISTS update_customers_updated_at();

--changeset blitzy-agent:create-customers-table-comments-v2
--comment: Add comprehensive table and column documentation

-- Table-level documentation
COMMENT ON TABLE customers IS 'Customer master data table migrated from VSAM CUSTDAT dataset. Contains comprehensive customer profile information including personal data, address details, contact information, and credit scoring. Supports normalized address structure for international compatibility and includes PII protection requirements for regulatory compliance.';

-- Column-level documentation with VSAM field mapping references
COMMENT ON COLUMN customers.customer_id IS 'Primary key: 9-digit customer identifier. Maps to CUSTDAT positions 1-9. Must be unique and follow numeric format validation.';
COMMENT ON COLUMN customers.first_name IS 'Customer first name. Maps to CUSTDAT positions 10-30. Required field with length validation.';
COMMENT ON COLUMN customers.middle_name IS 'Customer middle name. Maps to CUSTDAT positions 31-51. Optional field for full name support.';
COMMENT ON COLUMN customers.last_name IS 'Customer last name. Maps to CUSTDAT positions 52-72. Required field with length validation.';
COMMENT ON COLUMN customers.address_line_1 IS 'Primary address line. Maps to CUSTDAT positions 73-115. Required field supporting up to 50 characters for international addresses.';
COMMENT ON COLUMN customers.address_line_2 IS 'Secondary address line. Maps to CUSTDAT positions 116-158. Optional field for apartment, suite, or unit information.';
COMMENT ON COLUMN customers.address_line_3 IS 'Additional address line. Maps to CUSTDAT positions 159-201. Optional field for additional address details or city information.';
COMMENT ON COLUMN customers.state_code IS 'State or province code. Maps to CUSTDAT positions 202-203. Required 2-character state abbreviation.';
COMMENT ON COLUMN customers.country_code IS 'Country code. Maps to CUSTDAT positions 204-206. Required 3-character ISO country code, defaults to USA.';
COMMENT ON COLUMN customers.zip_code IS 'Postal or ZIP code. Maps to CUSTDAT positions 207-217. Required field supporting various postal code formats.';
COMMENT ON COLUMN customers.phone_number_1 IS 'Primary phone number. Maps to CUSTDAT positions 218-232. Optional field with format validation for US phone numbers.';
COMMENT ON COLUMN customers.phone_number_2 IS 'Secondary phone number. Maps to CUSTDAT positions 233-247. Optional field with format validation for US phone numbers.';
COMMENT ON COLUMN customers.ssn IS 'Social Security Number. Maps to CUSTDAT positions 248-256. Required PII field requiring encryption and access controls per PCI DSS compliance.';
COMMENT ON COLUMN customers.government_id IS 'Government-issued identification number. Maps to CUSTDAT positions 257-276. Required field supporting various ID types for regulatory compliance.';
COMMENT ON COLUMN customers.date_of_birth IS 'Customer date of birth. Maps to CUSTDAT positions 277-286. Required field with age validation constraints.';
COMMENT ON COLUMN customers.eft_account_id IS 'Electronic Funds Transfer account reference. Maps to CUSTDAT positions 287-296. Optional field linking to external payment systems.';
COMMENT ON COLUMN customers.primary_cardholder_indicator IS 'Primary cardholder flag. Maps to CUSTDAT position 297. Y/N indicator matching legacy system format.';
COMMENT ON COLUMN customers.fico_credit_score IS 'FICO credit score. Maps to CUSTDAT positions 298-300. Required field with industry-standard range validation (300-850).';
COMMENT ON COLUMN customers.created_at IS 'Record creation timestamp. Audit field for data lifecycle management and compliance reporting.';
COMMENT ON COLUMN customers.updated_at IS 'Record last modification timestamp. Automatically updated via trigger for audit trail maintenance.';

--rollback COMMENT ON TABLE customers IS NULL;
--rollback COMMENT ON COLUMN customers.customer_id IS NULL;
--rollback COMMENT ON COLUMN customers.first_name IS NULL;
--rollback COMMENT ON COLUMN customers.middle_name IS NULL;
--rollback COMMENT ON COLUMN customers.last_name IS NULL;
--rollback COMMENT ON COLUMN customers.address_line_1 IS NULL;
--rollback COMMENT ON COLUMN customers.address_line_2 IS NULL;
--rollback COMMENT ON COLUMN customers.address_line_3 IS NULL;
--rollback COMMENT ON COLUMN customers.state_code IS NULL;
--rollback COMMENT ON COLUMN customers.country_code IS NULL;
--rollback COMMENT ON COLUMN customers.zip_code IS NULL;
--rollback COMMENT ON COLUMN customers.phone_number_1 IS NULL;
--rollback COMMENT ON COLUMN customers.phone_number_2 IS NULL;
--rollback COMMENT ON COLUMN customers.ssn IS NULL;
--rollback COMMENT ON COLUMN customers.government_id IS NULL;
--rollback COMMENT ON COLUMN customers.date_of_birth IS NULL;
--rollback COMMENT ON COLUMN customers.eft_account_id IS NULL;
--rollback COMMENT ON COLUMN customers.primary_cardholder_indicator IS NULL;
--rollback COMMENT ON COLUMN customers.fico_credit_score IS NULL;
--rollback COMMENT ON COLUMN customers.created_at IS NULL;
--rollback COMMENT ON COLUMN customers.updated_at IS NULL;

--changeset blitzy-agent:create-customers-table-security-policies-v2
--comment: Enable row-level security and create access policies for PII protection

-- Enable row-level security for the customers table
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

-- Create policy for read access - users can only access their own customer record or admin users can access all
CREATE POLICY customers_read_policy ON customers
    FOR SELECT
    USING (
        -- Allow access if user is admin or accessing their own customer record
        current_setting('app.user_type', true) = 'ADMIN' OR
        customer_id = current_setting('app.customer_id', true)
    );

-- Create policy for write access - only admin users can modify customer records
CREATE POLICY customers_write_policy ON customers
    FOR ALL
    USING (current_setting('app.user_type', true) = 'ADMIN')
    WITH CHECK (current_setting('app.user_type', true) = 'ADMIN');

--rollback DROP POLICY IF EXISTS customers_write_policy ON customers;
--rollback DROP POLICY IF EXISTS customers_read_policy ON customers;
--rollback ALTER TABLE customers DISABLE ROW LEVEL SECURITY;

--changeset blitzy-agent:grant-customers-table-permissions-v2
--comment: Grant appropriate permissions for application roles

-- Grant SELECT permission to application read role
GRANT SELECT ON customers TO carddemo_read_role;

-- Grant full permissions to application write role  
GRANT SELECT, INSERT, UPDATE, DELETE ON customers TO carddemo_write_role;

-- Grant full permissions to admin role
GRANT ALL PRIVILEGES ON customers TO carddemo_admin_role;

--rollback REVOKE ALL PRIVILEGES ON customers FROM carddemo_admin_role;
--rollback REVOKE SELECT, INSERT, UPDATE, DELETE ON customers FROM carddemo_write_role;
--rollback REVOKE SELECT ON customers FROM carddemo_read_role;