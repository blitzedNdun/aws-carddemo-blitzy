-- =====================================================================================
-- Liquibase Changeset: 006-load-test-reference-data.sql
-- Purpose: Load essential reference data for integration testing scenarios
-- Supporting: CardDemo Spring Boot microservices automated testing infrastructure
-- Based on: COBOL copybooks CVTRA03Y.cpy and CVTRA04Y.cpy
-- Environment: Test environment for integration testing and validation workflows
-- Dependencies: Requires 004-create-test-reference-tables.sql (reference table creation)
-- =====================================================================================

--liquibase formatted sql

--changeset liquibase:006-load-test-reference-data splitStatements:true endDelimiter:;

-- =====================================================================================
-- INTEGRATION TEST SPECIFIC REFERENCE DATA LOADING
-- Purpose: Provide minimal, focused reference data for automated integration testing
-- Supporting: Spring Boot microservices integration test scenarios
-- Scope: Essential lookup data for transaction validation workflows
-- Testing Strategy: Supporting section 6.6.7.5, 6.6.3.2, and 6.6.2.3 requirements
-- =====================================================================================

-- =====================================================================================
-- TEST-SPECIFIC TRANSACTION TYPES
-- Maps from: COBOL copybook CVTRA03Y.cpy (TRAN-TYPE-RECORD structure)
-- Purpose: Essential transaction types for integration testing scenarios
-- Testing Focus: Edge cases, validation workflows, and automated test execution
-- Data Strategy: Minimal dataset complementing comprehensive data in changeset 004
-- =====================================================================================

-- Insert test-specific transaction types for integration testing workflows
INSERT INTO transaction_types (transaction_type, type_description, debit_credit_flag, active_status)
VALUES 
    -- Edge case transaction types for integration testing
    ('TE', 'Test Edge Case Transaction', 'D', TRUE),
    ('TV', 'Test Validation Transaction', 'C', TRUE),
    ('TF', 'Test Failure Scenario', 'D', FALSE),
    ('TS', 'Test Success Scenario', 'C', TRUE),
    ('TB', 'Test Boundary Condition', 'D', TRUE),
    
    -- Integration test specific transaction types
    ('I1', 'Integration Test Type 1', 'D', TRUE),
    ('I2', 'Integration Test Type 2', 'C', TRUE),
    ('I3', 'Integration Test Type 3', 'D', TRUE),
    
    -- Automated test workflow transaction types
    ('A1', 'Automated Test Workflow 1', 'C', TRUE),
    ('A2', 'Automated Test Workflow 2', 'D', TRUE),
    ('A3', 'Automated Test Workflow 3', 'C', FALSE),
    
    -- Testcontainers specific transaction types
    ('TC', 'Testcontainers Test Type', 'D', TRUE),
    ('TM', 'Test Microservice Integration', 'C', TRUE),
    ('TR', 'Test Repository Validation', 'D', TRUE),
    ('TG', 'Test Gateway Routing', 'C', TRUE)
ON CONFLICT (transaction_type) DO NOTHING;

-- =====================================================================================
-- TEST-SPECIFIC TRANSACTION CATEGORIES  
-- Maps from: COBOL copybook CVTRA04Y.cpy (TRAN-CAT-RECORD structure)
-- Purpose: Transaction category classification for automated testing scenarios
-- Testing Focus: Complex validation scenarios and edge case handling
-- Integration Support: Spring Boot microservices and PostgreSQL testing workflows
-- =====================================================================================

-- Insert test-specific transaction categories for integration testing scenarios
INSERT INTO transaction_categories (transaction_type, transaction_category, category_description, active_status, risk_level, processing_priority)
VALUES 
    -- Edge case testing categories
    ('TE', '0001', 'Edge Case - Minimum Amount', TRUE, 'L', 1),
    ('TE', '0002', 'Edge Case - Maximum Amount', TRUE, 'H', 1),
    ('TE', '0003', 'Edge Case - Zero Amount', TRUE, 'M', 2),
    ('TE', '0004', 'Edge Case - Negative Amount', FALSE, 'H', 1),
    
    -- Validation testing categories
    ('TV', '0001', 'Validation - Format Check', TRUE, 'L', 3),
    ('TV', '0002', 'Validation - Business Rule', TRUE, 'M', 3),
    ('TV', '0003', 'Validation - Data Integrity', TRUE, 'H', 2),
    ('TV', '0004', 'Validation - Constraint Check', TRUE, 'M', 3),
    
    -- Failure scenario testing categories
    ('TF', '0001', 'Failure - Database Error', FALSE, 'H', 1),
    ('TF', '0002', 'Failure - Network Timeout', FALSE, 'H', 1),
    ('TF', '0003', 'Failure - Invalid State', FALSE, 'M', 2),
    
    -- Success scenario testing categories
    ('TS', '0001', 'Success - Standard Path', TRUE, 'L', 5),
    ('TS', '0002', 'Success - Alternative Path', TRUE, 'L', 5),
    ('TS', '0003', 'Success - Recovery Path', TRUE, 'M', 4),
    
    -- Boundary condition testing categories
    ('TB', '0001', 'Boundary - Account Limit', TRUE, 'H', 2),
    ('TB', '0002', 'Boundary - Daily Limit', TRUE, 'M', 3),
    ('TB', '0003', 'Boundary - Transaction Count', TRUE, 'L', 4),
    
    -- Integration test specific categories
    ('I1', '0001', 'Integration - Service to Service', TRUE, 'L', 5),
    ('I1', '0002', 'Integration - Database Operations', TRUE, 'L', 4),
    ('I1', '0003', 'Integration - Cache Operations', TRUE, 'L', 6),
    
    ('I2', '0001', 'Integration - Authentication Flow', TRUE, 'M', 3),
    ('I2', '0002', 'Integration - Authorization Check', TRUE, 'M', 3),
    ('I2', '0003', 'Integration - Session Management', TRUE, 'L', 5),
    
    ('I3', '0001', 'Integration - Batch Processing', TRUE, 'L', 7),
    ('I3', '0002', 'Integration - Real-time Processing', TRUE, 'M', 4),
    ('I3', '0003', 'Integration - Event Processing', TRUE, 'M', 4),
    
    -- Automated test workflow categories
    ('A1', '0001', 'Automated - Happy Path', TRUE, 'L', 8),
    ('A1', '0002', 'Automated - Error Handling', TRUE, 'M', 6),
    ('A1', '0003', 'Automated - Retry Logic', TRUE, 'L', 7),
    
    ('A2', '0001', 'Automated - Load Testing', TRUE, 'L', 5),
    ('A2', '0002', 'Automated - Stress Testing', TRUE, 'H', 3),
    ('A2', '0003', 'Automated - Performance Testing', TRUE, 'M', 4),
    
    ('A3', '0001', 'Automated - Inactive Scenario', FALSE, 'L', 9),
    ('A3', '0002', 'Automated - Disabled Feature', FALSE, 'L', 9),
    
    -- Testcontainers specific categories
    ('TC', '0001', 'Testcontainers - PostgreSQL Test', TRUE, 'L', 6),
    ('TC', '0002', 'Testcontainers - Redis Test', TRUE, 'L', 6),
    ('TC', '0003', 'Testcontainers - Network Test', TRUE, 'M', 5),
    
    ('TM', '0001', 'Microservice - Authentication', TRUE, 'M', 4),
    ('TM', '0002', 'Microservice - Account Service', TRUE, 'L', 5),
    ('TM', '0003', 'Microservice - Transaction Service', TRUE, 'L', 5),
    ('TM', '0004', 'Microservice - Card Service', TRUE, 'L', 5),
    
    ('TR', '0001', 'Repository - CRUD Operations', TRUE, 'L', 6),
    ('TR', '0002', 'Repository - Query Performance', TRUE, 'M', 4),
    ('TR', '0003', 'Repository - Transaction Boundaries', TRUE, 'H', 3),
    
    ('TG', '0001', 'Gateway - Request Routing', TRUE, 'L', 5),
    ('TG', '0002', 'Gateway - Load Balancing', TRUE, 'M', 4),
    ('TG', '0003', 'Gateway - Circuit Breaker', TRUE, 'H', 3)
ON CONFLICT (transaction_type, transaction_category) DO NOTHING;

-- =====================================================================================
-- INTEGRATION TEST DATA VALIDATION
-- Purpose: Ensure data quality and consistency for automated testing
-- Supporting: Spring Boot microservices integration testing workflows
-- Validation: Reference data completeness and relationship integrity
-- =====================================================================================

-- Validate that all test transaction types have corresponding categories
DO $$
DECLARE
    missing_categories INTEGER;
    test_record RECORD;
BEGIN
    -- Check for transaction types without categories
    SELECT COUNT(*) INTO missing_categories
    FROM transaction_types tt
    LEFT JOIN transaction_categories tc ON tt.transaction_type = tc.transaction_type
    WHERE tt.transaction_type LIKE 'T%' OR tt.transaction_type LIKE 'I%' OR tt.transaction_type LIKE 'A%'
    AND tc.transaction_type IS NULL;
    
    IF missing_categories > 0 THEN
        RAISE NOTICE 'Warning: % test transaction types found without corresponding categories', missing_categories;
    ELSE
        RAISE NOTICE 'All test transaction types have corresponding categories';
    END IF;
    
    -- Log test data statistics
    SELECT COUNT(*) INTO missing_categories FROM transaction_types WHERE transaction_type LIKE 'T%' OR transaction_type LIKE 'I%' OR transaction_type LIKE 'A%';
    RAISE NOTICE 'Loaded % test-specific transaction types for integration testing', missing_categories;
    
    SELECT COUNT(*) INTO missing_categories FROM transaction_categories WHERE transaction_type LIKE 'T%' OR transaction_type LIKE 'I%' OR transaction_type LIKE 'A%';
    RAISE NOTICE 'Loaded % test-specific transaction categories for integration testing', missing_categories;
END $$;

-- =====================================================================================
-- TEST REFERENCE DATA INDEXES
-- Purpose: Optimize lookup performance for integration testing scenarios
-- Supporting: Sub-200ms response time requirements for test execution
-- Focus: Test-specific query patterns and automated test performance
-- =====================================================================================

-- Index for test transaction type lookups (pattern matching)
CREATE INDEX IF NOT EXISTS idx_transaction_types_test_pattern 
    ON transaction_types(transaction_type) 
    WHERE transaction_type ~ '^[TIA][0-9A-Z]$';

-- Index for test category lookups with risk level filtering
CREATE INDEX IF NOT EXISTS idx_transaction_categories_test_risk 
    ON transaction_categories(transaction_type, risk_level) 
    WHERE transaction_type ~ '^[TIA][0-9A-Z]$' AND active_status = TRUE;

-- Index for automated test workflow priorities
CREATE INDEX IF NOT EXISTS idx_transaction_categories_automation_priority 
    ON transaction_categories(processing_priority, transaction_type) 
    WHERE transaction_type LIKE 'A%' AND active_status = TRUE;

-- Index for integration test scenarios
CREATE INDEX IF NOT EXISTS idx_transaction_categories_integration_test 
    ON transaction_categories(transaction_type, transaction_category) 
    WHERE transaction_type LIKE 'I%' OR transaction_type LIKE 'T%';

-- =====================================================================================
-- TEST DATA UTILITY FUNCTIONS
-- Purpose: Support automated integration testing workflows
-- Supporting: Spring Boot microservices testing infrastructure
-- Functions: Test data validation, cleanup, and scenario management
-- =====================================================================================

-- Function to validate test transaction combinations for integration testing
CREATE OR REPLACE FUNCTION validate_test_transaction_data(
    p_transaction_type VARCHAR(2),
    p_transaction_category VARCHAR(4) DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_is_test_type BOOLEAN;
    v_category_count INTEGER;
BEGIN
    -- Check if transaction type is a test type
    v_is_test_type := p_transaction_type ~ '^[TIA][0-9A-Z]$';
    
    IF NOT v_is_test_type THEN
        RETURN FALSE;
    END IF;
    
    -- If category is specified, validate the combination
    IF p_transaction_category IS NOT NULL THEN
        SELECT COUNT(*) INTO v_category_count
        FROM transaction_categories
        WHERE transaction_type = p_transaction_type
        AND transaction_category = p_transaction_category
        AND active_status = TRUE;
        
        RETURN v_category_count > 0;
    END IF;
    
    -- Just validate transaction type exists and is active
    SELECT COUNT(*) INTO v_category_count
    FROM transaction_types
    WHERE transaction_type = p_transaction_type
    AND active_status = TRUE;
    
    RETURN v_category_count > 0;
END;
$$ LANGUAGE plpgsql;

-- Function to get test scenarios for automated integration testing
CREATE OR REPLACE FUNCTION get_test_scenarios(
    p_scenario_type VARCHAR(10) DEFAULT 'ALL'
) RETURNS TABLE(
    transaction_type VARCHAR(2),
    transaction_category VARCHAR(4),
    category_description VARCHAR(50),
    risk_level CHAR(1),
    processing_priority INTEGER,
    scenario_group VARCHAR(20)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        tc.transaction_type,
        tc.transaction_category,
        tc.category_description,
        tc.risk_level,
        tc.processing_priority,
        CASE 
            WHEN tc.transaction_type LIKE 'TE%' THEN 'EDGE_CASE'
            WHEN tc.transaction_type LIKE 'TV%' THEN 'VALIDATION'
            WHEN tc.transaction_type LIKE 'TF%' THEN 'FAILURE'
            WHEN tc.transaction_type LIKE 'TS%' THEN 'SUCCESS'
            WHEN tc.transaction_type LIKE 'TB%' THEN 'BOUNDARY'
            WHEN tc.transaction_type LIKE 'I%' THEN 'INTEGRATION'
            WHEN tc.transaction_type LIKE 'A%' THEN 'AUTOMATION'
            WHEN tc.transaction_type LIKE 'TC%' THEN 'TESTCONTAINER'
            WHEN tc.transaction_type LIKE 'TM%' THEN 'MICROSERVICE'
            WHEN tc.transaction_type LIKE 'TR%' THEN 'REPOSITORY'
            WHEN tc.transaction_type LIKE 'TG%' THEN 'GATEWAY'
            ELSE 'OTHER'
        END as scenario_group
    FROM transaction_categories tc
    JOIN transaction_types tt ON tc.transaction_type = tt.transaction_type
    WHERE (p_scenario_type = 'ALL' OR 
           (p_scenario_type = 'ACTIVE' AND tc.active_status = TRUE AND tt.active_status = TRUE) OR
           (p_scenario_type = 'EDGE' AND tc.transaction_type LIKE 'T%') OR
           (p_scenario_type = 'INTEGRATION' AND tc.transaction_type LIKE 'I%') OR
           (p_scenario_type = 'AUTOMATION' AND tc.transaction_type LIKE 'A%'))
    ORDER BY tc.processing_priority, tc.transaction_type, tc.transaction_category;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup test data for integration testing
CREATE OR REPLACE FUNCTION cleanup_test_reference_data() RETURNS BOOLEAN AS $$
DECLARE
    deleted_categories INTEGER;
    deleted_types INTEGER;
BEGIN
    -- Delete test transaction categories
    DELETE FROM transaction_categories 
    WHERE transaction_type ~ '^[TIA][0-9A-Z]$';
    GET DIAGNOSTICS deleted_categories = ROW_COUNT;
    
    -- Delete test transaction types
    DELETE FROM transaction_types 
    WHERE transaction_type ~ '^[TIA][0-9A-Z]$';
    GET DIAGNOSTICS deleted_types = ROW_COUNT;
    
    RAISE NOTICE 'Cleaned up % test transaction categories and % test transaction types', 
                 deleted_categories, deleted_types;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================================
-- TEST DATA DOCUMENTATION AND COMMENTS
-- Purpose: Provide comprehensive documentation for test reference data
-- Supporting: Development team understanding and test maintenance
-- Documentation: Test scenario descriptions and usage guidelines
-- =====================================================================================

-- Table comments for test reference data
COMMENT ON FUNCTION validate_test_transaction_data(VARCHAR, VARCHAR) IS 'Validates test transaction type and category combinations for integration testing scenarios';
COMMENT ON FUNCTION get_test_scenarios(VARCHAR) IS 'Returns test scenarios grouped by type for automated integration testing workflows';
COMMENT ON FUNCTION cleanup_test_reference_data() IS 'Removes all test-specific reference data for clean test environment reset';

-- Test data usage documentation
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '=== CARDDEMO TEST REFERENCE DATA LOADED ===';
    RAISE NOTICE 'Changeset: 006-load-test-reference-data.sql';
    RAISE NOTICE 'Purpose: Essential reference data for integration testing';
    RAISE NOTICE '';
    RAISE NOTICE 'Test Transaction Type Patterns:';
    RAISE NOTICE '  T* = Test scenarios (TE=Edge, TV=Validation, TF=Failure, TS=Success, TB=Boundary)';
    RAISE NOTICE '  I* = Integration testing (I1=Service, I2=Auth, I3=Batch)';
    RAISE NOTICE '  A* = Automated testing (A1=Workflow, A2=Performance, A3=Inactive)';
    RAISE NOTICE '  TC/TM/TR/TG = Testcontainers/Microservice/Repository/Gateway testing';
    RAISE NOTICE '';
    RAISE NOTICE 'Usage Examples:';
    RAISE NOTICE '  SELECT * FROM get_test_scenarios(''INTEGRATION'');';
    RAISE NOTICE '  SELECT validate_test_transaction_data(''TE'', ''0001'');';
    RAISE NOTICE '  SELECT cleanup_test_reference_data();';
    RAISE NOTICE '';
    RAISE NOTICE 'Integration with Spring Boot Testing:';
    RAISE NOTICE '  - Testcontainers PostgreSQL integration';
    RAISE NOTICE '  - JUnit 5 parameterized test data source';
    RAISE NOTICE '  - @DataJpaTest repository validation';
    RAISE NOTICE '  - Automated integration test execution';
    RAISE NOTICE '=== TEST REFERENCE DATA LOADING COMPLETE ===';
    RAISE NOTICE '';
END $$;

-- Grant appropriate permissions for test environment access
GRANT SELECT ON transaction_types TO PUBLIC;
GRANT SELECT ON transaction_categories TO PUBLIC;
GRANT EXECUTE ON FUNCTION validate_test_transaction_data(VARCHAR, VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION get_test_scenarios(VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION cleanup_test_reference_data() TO PUBLIC;

-- Changeset completion marker for Liquibase tracking
SELECT 'Test reference data loaded successfully for CardDemo integration testing' AS status;

--rollback SELECT cleanup_test_reference_data();
--rollback DROP FUNCTION IF EXISTS cleanup_test_reference_data();
--rollback DROP FUNCTION IF EXISTS get_test_scenarios(VARCHAR);
--rollback DROP FUNCTION IF EXISTS validate_test_transaction_data(VARCHAR, VARCHAR);
--rollback DROP INDEX IF EXISTS idx_transaction_categories_integration_test;
--rollback DROP INDEX IF EXISTS idx_transaction_categories_automation_priority;
--rollback DROP INDEX IF EXISTS idx_transaction_categories_test_risk;
--rollback DROP INDEX IF EXISTS idx_transaction_types_test_pattern;
--rollback DELETE FROM transaction_categories WHERE transaction_type ~ '^[TIA][0-9A-Z]$';
--rollback DELETE FROM transaction_types WHERE transaction_type ~ '^[TIA][0-9A-Z]$';