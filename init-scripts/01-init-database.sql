-- PostgreSQL initialization script for CardDemo application
-- Creates necessary schemas and basic configuration

-- Create application schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS public;

-- Set timezone to UTC
SET timezone = 'UTC';

-- Create extension for UUID generation if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON SCHEMA public TO carddemo_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO carddemo_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO carddemo_dev;

-- Enable required PostgreSQL extensions for performance
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Basic database configuration
ALTER DATABASE carddemo SET timezone TO 'UTC';

-- Log successful initialization
SELECT 'CardDemo database initialized successfully' AS status;
