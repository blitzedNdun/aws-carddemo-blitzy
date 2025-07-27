-- ==============================================================================
-- Liquibase Migration: V1_5__create_audit_tables.sql
-- Description: Creates audit and system log tables for data integrity monitoring
-- Author: Blitzy agent
-- Version: 1.5
-- Migration Type: CREATE TABLES for audit trail and system logging
-- ==============================================================================

--liquibase formatted sql

--changeset blitzy-agent:create-audit-log-table-v1_5
--comment: Create audit_log table for tracking data changes and financial operations

-- Create audit_log table for comprehensive audit trail
CREATE TABLE audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id VARCHAR(50) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_values JSON,
    new_values JSON,
    change_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE audit_log CASCADE;

--changeset blitzy-agent:create-system-log-table-v1_5
--comment: Create system_log table for application event logging

-- Create system_log table for system event tracking
CREATE TABLE system_log (
    log_id BIGSERIAL PRIMARY KEY,
    log_level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    error_details TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE system_log CASCADE;

--changeset blitzy-agent:create-audit-table-indexes-v1_5
--comment: Create indexes for audit and system log tables

-- Indexes for audit_log table
CREATE INDEX idx_audit_log_table_record ON audit_log (table_name, record_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log (change_timestamp);

-- Indexes for system_log table
CREATE INDEX idx_system_log_level ON system_log (log_level);
CREATE INDEX idx_system_log_timestamp ON system_log (timestamp);

--rollback DROP INDEX IF EXISTS idx_system_log_timestamp;
--rollback DROP INDEX IF EXISTS idx_system_log_level;
--rollback DROP INDEX IF EXISTS idx_audit_log_timestamp;
--rollback DROP INDEX IF EXISTS idx_audit_log_table_record;