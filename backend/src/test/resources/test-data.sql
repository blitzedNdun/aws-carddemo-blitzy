-- Test Data for BaseTestConfig H2 Database
-- Created for ad-hoc testing purposes

-- Insert test data into test_config_validation table
INSERT INTO test_config_validation (test_name, test_status) VALUES
('Unit Test Data Load', 'ACTIVE'),
('H2 Database Initialization', 'SUCCESSFUL'),
('Spring Boot Test Data Source', 'CONFIGURED');

-- Additional test data for validation
INSERT INTO test_config_validation (test_name, test_status) VALUES
('BaseTestConfig Validation Complete', 'SUCCESS');

-- Comment for verification
-- This data file ensures BaseTestConfig H2 data source loads test data properly