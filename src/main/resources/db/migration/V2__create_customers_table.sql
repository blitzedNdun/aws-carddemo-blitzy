-- =====================================================================================
-- Liquibase Migration: V2__create_customers_table.sql
-- Description: Creates customers table from VSAM CUSTDAT dataset with normalized 
--              address structure, PII protection, and FICO credit score validation
-- Author: Blitzy Agent  
-- Date: 2024
-- Version: 2.0
-- =====================================================================================

-- changeset blitzy:V2-create-customers-table
-- comment: Create customers table migrated from CUSTDAT VSAM dataset preserving exact field layouts and data types

-- Create customers table with all required fields from VSAM CUSTDAT structure
CREATE TABLE customers (
    -- Primary key: customer_id as VARCHAR(9) matching fixed-width 9-digit record structure
    customer_id VARCHAR(9) NOT NULL,
    
    -- Customer personal information fields
    first_name VARCHAR(25) NOT NULL,
    middle_name VARCHAR(25),
    last_name VARCHAR(25) NOT NULL,
    
    -- Normalized address structure supporting multi-line address storage
    address_line_1 VARCHAR(50) NOT NULL,
    address_line_2 VARCHAR(50),
    address_line_3 VARCHAR(50),
    address_state VARCHAR(2) NOT NULL,
    address_country VARCHAR(3) NOT NULL DEFAULT 'USA',
    address_zip VARCHAR(10) NOT NULL,
    
    -- Phone number fields supporting dual phone storage with formatted validation
    phone_home VARCHAR(14),
    phone_work VARCHAR(14),
    
    -- PII fields requiring encryption and access controls
    ssn VARCHAR(9) NOT NULL,
    government_id VARCHAR(18) NOT NULL,
    
    -- Customer demographics and risk assessment
    date_of_birth DATE NOT NULL,
    fico_credit_score INTEGER NOT NULL,
    
    -- EFT and cardholder information
    eft_account_id VARCHAR(10),
    primary_cardholder_indicator BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit and tracking fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Primary key constraint
    CONSTRAINT customers_pkey PRIMARY KEY (customer_id)
);

-- Create indexes for optimized query performance
CREATE INDEX idx_customers_ssn ON customers(ssn);
CREATE INDEX idx_customers_name ON customers(last_name, first_name);
CREATE INDEX idx_customers_address_zip ON customers(address_zip);
CREATE INDEX idx_customers_fico_score ON customers(fico_credit_score);
CREATE INDEX idx_customers_date_of_birth ON customers(date_of_birth);

-- Add CHECK constraints for data validation and business rules
ALTER TABLE customers 
ADD CONSTRAINT customers_customer_id_check 
CHECK (customer_id ~ '^[0-9]{9}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_ssn_check 
CHECK (ssn ~ '^[0-9]{9}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_government_id_check 
CHECK (government_id ~ '^[0-9]{18}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_fico_score_check 
CHECK (fico_credit_score >= 300 AND fico_credit_score <= 850);

ALTER TABLE customers 
ADD CONSTRAINT customers_address_state_check 
CHECK (LENGTH(address_state) = 2 AND address_state ~ '^[A-Z]{2}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_address_country_check 
CHECK (LENGTH(address_country) = 3 AND address_country ~ '^[A-Z]{3}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_address_zip_check 
CHECK (address_zip ~ '^[0-9]{5}(-[0-9]{4})?$');

ALTER TABLE customers 
ADD CONSTRAINT customers_phone_home_check 
CHECK (phone_home IS NULL OR phone_home ~ '^\([0-9]{3}\)[0-9]{3}-[0-9]{4}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_phone_work_check 
CHECK (phone_work IS NULL OR phone_work ~ '^\([0-9]{3}\)[0-9]{3}-[0-9]{4}$');

ALTER TABLE customers 
ADD CONSTRAINT customers_date_of_birth_check 
CHECK (date_of_birth <= CURRENT_DATE AND date_of_birth >= '1900-01-01');

-- Add NOT NULL constraints for required fields
ALTER TABLE customers 
ADD CONSTRAINT customers_first_name_not_empty 
CHECK (LENGTH(TRIM(first_name)) > 0);

ALTER TABLE customers 
ADD CONSTRAINT customers_last_name_not_empty 
CHECK (LENGTH(TRIM(last_name)) > 0);

ALTER TABLE customers 
ADD CONSTRAINT customers_address_line_1_not_empty 
CHECK (LENGTH(TRIM(address_line_1)) > 0);

-- Create trigger function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_customers_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on row changes
CREATE TRIGGER customers_updated_at_trigger
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_customers_updated_at();

-- Add comments to table and columns for documentation
COMMENT ON TABLE customers IS 'Customer master data table migrated from VSAM CUSTDAT dataset with normalized address structure and PII protection';

COMMENT ON COLUMN customers.customer_id IS 'Primary key: 9-digit customer identifier matching VSAM record structure';
COMMENT ON COLUMN customers.first_name IS 'Customer first name (25 characters max)';
COMMENT ON COLUMN customers.middle_name IS 'Customer middle name (25 characters max, optional)';
COMMENT ON COLUMN customers.last_name IS 'Customer last name (25 characters max)';
COMMENT ON COLUMN customers.address_line_1 IS 'Primary address line (50 characters max)';
COMMENT ON COLUMN customers.address_line_2 IS 'Secondary address line (50 characters max, optional)';
COMMENT ON COLUMN customers.address_line_3 IS 'Additional address line (50 characters max, optional)';
COMMENT ON COLUMN customers.address_state IS '2-character state abbreviation';
COMMENT ON COLUMN customers.address_country IS '3-character country code (default: USA)';
COMMENT ON COLUMN customers.address_zip IS 'ZIP code in 5-digit or 5+4 format';
COMMENT ON COLUMN customers.phone_home IS 'Home phone number in (XXX)XXX-XXXX format';
COMMENT ON COLUMN customers.phone_work IS 'Work phone number in (XXX)XXX-XXXX format';
COMMENT ON COLUMN customers.ssn IS 'Social Security Number (9 digits) - PII field requiring encryption';
COMMENT ON COLUMN customers.government_id IS 'Government issued ID (18 digits) - PII field requiring encryption';
COMMENT ON COLUMN customers.date_of_birth IS 'Customer date of birth';
COMMENT ON COLUMN customers.fico_credit_score IS 'FICO credit score (valid range: 300-850)';
COMMENT ON COLUMN customers.eft_account_id IS 'Electronic Funds Transfer account identifier';
COMMENT ON COLUMN customers.primary_cardholder_indicator IS 'Flag indicating if customer is primary cardholder';
COMMENT ON COLUMN customers.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN customers.updated_at IS 'Timestamp when record was last updated';

-- Create materialized view for customer summary queries (optimized for cross-reference operations)
CREATE MATERIALIZED VIEW mv_customer_summary AS
SELECT 
    customer_id,
    CONCAT(first_name, ' ', COALESCE(middle_name || ' ', ''), last_name) AS full_name,
    CONCAT(address_line_1, 
           CASE WHEN address_line_2 IS NOT NULL THEN ', ' || address_line_2 ELSE '' END,
           CASE WHEN address_line_3 IS NOT NULL THEN ', ' || address_line_3 ELSE '' END) AS full_address,
    address_state,
    address_country,
    address_zip,
    COALESCE(phone_home, phone_work) AS primary_phone,
    fico_credit_score,
    CASE 
        WHEN fico_credit_score >= 800 THEN 'Excellent'
        WHEN fico_credit_score >= 740 THEN 'Very Good'
        WHEN fico_credit_score >= 670 THEN 'Good'
        WHEN fico_credit_score >= 580 THEN 'Fair'
        ELSE 'Poor'
    END AS credit_rating,
    EXTRACT(YEAR FROM AGE(date_of_birth)) AS age,
    primary_cardholder_indicator,
    created_at,
    updated_at
FROM customers;

-- Create indexes on materialized view for performance optimization
CREATE INDEX idx_mv_customer_summary_full_name ON mv_customer_summary(full_name);
CREATE INDEX idx_mv_customer_summary_credit_rating ON mv_customer_summary(credit_rating);
CREATE INDEX idx_mv_customer_summary_age ON mv_customer_summary(age);
CREATE INDEX idx_mv_customer_summary_primary_phone ON mv_customer_summary(primary_phone);

-- Add comment to materialized view
COMMENT ON MATERIALIZED VIEW mv_customer_summary IS 'Optimized customer summary view for cross-reference queries and reporting';

-- Create function to refresh materialized view (scheduled via cron or application)
CREATE OR REPLACE FUNCTION refresh_customer_summary_view()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_summary;
END;
$$ LANGUAGE plpgsql;

-- rollback changeset blitzy:V2-create-customers-table
-- DROP MATERIALIZED VIEW IF EXISTS mv_customer_summary CASCADE;
-- DROP FUNCTION IF EXISTS refresh_customer_summary_view();
-- DROP TRIGGER IF EXISTS customers_updated_at_trigger ON customers;
-- DROP FUNCTION IF EXISTS update_customers_updated_at();
-- DROP TABLE IF EXISTS customers CASCADE;