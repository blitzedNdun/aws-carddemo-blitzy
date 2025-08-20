-- Test Schema for BaseTestConfig H2 Database
-- Created for ad-hoc testing purposes

-- Simple test table to verify H2 database initialization
CREATE TABLE IF NOT EXISTS test_config_validation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    test_status VARCHAR(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert basic test data
INSERT INTO test_config_validation (test_name, test_status) VALUES
('BaseTestConfig Validation', 'ACTIVE'),
('H2 Database Connection', 'ACTIVE'),
('Spring Boot Test Configuration', 'ACTIVE');

-- Test-specific sequence
CREATE SEQUENCE IF NOT EXISTS test_id_sequence START WITH 1000 INCREMENT BY 1;

-- Comment for verification
-- This schema file ensures BaseTestConfig H2 data source can initialize properly