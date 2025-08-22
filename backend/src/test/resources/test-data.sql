-- CardDemo Test Data for H2 Database
-- Sample data for testing COBOL-to-Java migration functionality
-- This data supports comprehensive unit and integration testing

-- Clear all existing test data to avoid unique constraint violations
-- Delete in reverse dependency order to avoid foreign key constraint violations
DELETE FROM archive;
DELETE FROM account_closure;
DELETE FROM audit_log;
DELETE FROM fee_schedule;
DELETE FROM settlement;
DELETE FROM authorization_data;
DELETE FROM daily_transactions;
DELETE FROM dispute;
DELETE FROM report;
DELETE FROM notification;
DELETE FROM fee;
DELETE FROM statement;
DELETE FROM interest_rate;
DELETE FROM disclosure_groups;
DELETE FROM card_xref;
DELETE FROM transaction_category_balance;
DELETE FROM transaction_data;
DELETE FROM user_data;
DELETE FROM user_security;
DELETE FROM card_data;
DELETE FROM account_data;
DELETE FROM customer_data;
DELETE FROM configuration WHERE environment = 'TEST';

INSERT INTO configuration (environment, name, config_key, category, config_value, description, version, active, requires_validation) VALUES
('TEST', 'Database Connection Pool Size', 'db.pool.size', 'DATABASE', '10', 'Maximum database connection pool size', 1, true, false),
('TEST', 'Session Timeout Minutes', 'session.timeout.minutes', 'SECURITY', '30', 'User session timeout in minutes', 1, true, false),
('TEST', 'Interest Rate Default', 'interest.rate.default', 'BUSINESS', '18.5', 'Default interest rate for new accounts', 1, true, true),
('TEST', 'Maximum Transaction Amount', 'transaction.max.amount', 'BUSINESS', '10000.00', 'Maximum allowed transaction amount', 1, true, true),
('TEST', 'Batch Processing Schedule', 'batch.processing.schedule', 'BATCH', '0 2 * * *', 'Cron expression for batch processing schedule', 1, true, false);

-- Insert test customers
INSERT INTO customer_data (customer_id, first_name, middle_name, last_name, address_line_1, address_line_2, 
    state_code, country_code, zip_code, phone_number_1, ssn, date_of_birth, fico_score, primary_card_holder_indicator, 
    credit_limit, last_update_timestamp, created_timestamp) VALUES
(1000000001, 'John', 'A', 'Doe', '123 Main Street', 'Apt 4B', 'NY', 'USA', '10001', '555-123-4567', 
    '123-45-6789', '1985-06-15', 720, 'Y', 25000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1000000002, 'Jane', 'B', 'Smith', '456 Oak Avenue', NULL, 'CA', 'USA', '90210', '555-987-6543', 
    '987-65-4321', '1990-03-22', 680, 'Y', 30000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1000000003, 'Robert', 'C', 'Johnson', '789 Pine Road', 'Unit 12', 'TX', 'USA', '75201', '555-456-7890', 
    '456-78-9012', '1982-11-30', 750, 'Y', 20000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1000000004, 'Mary', 'D', 'Williams', '321 Elm Street', NULL, 'FL', 'USA', '33101', '555-234-5678', 
    '234-56-7890', '1988-09-07', 640, 'Y', 35000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1000000005, 'Michael', 'E', 'Brown', '654 Maple Drive', 'Suite 200', 'IL', 'USA', '60601', '555-345-6789', 
    '345-67-8901', '1975-12-14', 780, 'Y', 40000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test accounts
INSERT INTO account_data (account_id, customer_id, active_status, current_balance, credit_limit, 
    cash_credit_limit, open_date, expiration_date, group_id) VALUES
(12345678901, 1000000001, 'Y', 1250.75, 5000.00, 1000.00, '2022-01-15', '2026-01-31', 'STANDARD'),
(12345678902, 1000000002, 'Y', 2345.50, 10000.00, 2000.00, '2021-06-20', '2025-06-30', 'PREMIUM'),
(12345678903, 1000000003, 'Y', 567.25, 3000.00, 500.00, '2023-03-10', '2027-03-31', 'BASIC'),
(12345678904, 1000000004, 'Y', 4123.88, 15000.00, 3000.00, '2020-11-05', '2024-11-30', 'PLATINUM'),
(12345678905, 1000000005, 'Y', 890.33, 7500.00, 1500.00, '2022-08-25', '2026-08-31', 'GOLD');

-- Insert test cards
INSERT INTO card_data (card_number, account_id, customer_id, cvv_code, embossed_name, expiration_date, active_status) VALUES
('4000123456789001', 12345678901, 1000000001, '123', 'JOHN A DOE', '2026-01-31', 'Y'),
('4000123456789002', 12345678902, 1000000002, '456', 'JANE B SMITH', '2025-06-30', 'Y'),
('4000123456789003', 12345678903, 1000000003, '789', 'ROBERT C JOHNSON', '2027-03-31', 'Y'),
('4000123456789004', 12345678904, 1000000004, '012', 'MARY D WILLIAMS', '2024-11-30', 'Y'),
('4000123456789005', 12345678905, 1000000005, '345', 'MICHAEL E BROWN', '2026-08-31', 'Y');

-- Insert test users for security
INSERT INTO user_security (sec_usr_id, username, password_hash, first_name, last_name, sec_usr_type, 
    enabled, account_non_expired, account_non_locked, credentials_non_expired, failed_login_attempts) VALUES
('ADMIN01', 'ADMIN01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFe5KuEty6iXPLJ.T4RsKtS', 'System', 'Administrator', 'A', 
    true, true, true, true, 0),
('USER001', 'USER001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFe5KuEty6iXPLJ.T4RsKtS', 'Regular', 'User', 'U', 
    true, true, true, true, 0),
('TELLER1', 'TELLER1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFe5KuEty6iXPLJ.T4RsKtS', 'Bank', 'Teller', 'U', 
    true, true, true, true, 0),
('MANAGER', 'MANAGER', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFe5KuEty6iXPLJ.T4RsKtS', 'Branch', 'Manager', 'A', 
    true, true, true, true, 0),
('AUDITOR', 'AUDITOR', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFe5KuEty6iXPLJ.T4RsKtS', 'Internal', 'Auditor', 'A', 
    true, true, true, true, 0);

-- Insert test business users
INSERT INTO user_data (user_id, first_name, last_name, email, phone, status, user_type) VALUES
('USR001', 'John', 'Administrator', 'admin@carddemo.com', '555-001-0001', 'ACTIVE', 'ADMIN'),
('USR002', 'Jane', 'Manager', 'manager@carddemo.com', '555-001-0002', 'ACTIVE', 'MANAGER'),
('USR003', 'Bob', 'Analyst', 'analyst@carddemo.com', '555-001-0003', 'ACTIVE', 'ANALYST'),
('USR004', 'Alice', 'Operator', 'operator@carddemo.com', '555-001-0004', 'ACTIVE', 'OPERATOR'),
('USR005', 'Mike', 'Support', 'support@carddemo.com', '555-001-0005', 'ACTIVE', 'SUPPORT');

-- Insert sample transactions
INSERT INTO transaction_data (transaction_id, account_id, card_number, type_code, category_code, 
    subcategory_code, source, description, amount, merchant_name, merchant_city, transaction_date) VALUES
('TX0000000000001', 12345678901, '4000123456789001', '01', '0100', '01', 'MERCHANT', 
    'GROCERY STORE PURCHASE', 85.67, 'SAFEWAY STORES', 'NEW YORK', CURRENT_DATE - 5),
('TX0000000000002', 12345678901, '4000123456789001', '01', '0100', '02', 'ONLINE', 
    'AMAZON PURCHASE', 129.99, 'AMAZON.COM', 'SEATTLE', CURRENT_DATE - 3),
('TX0000000000003', 12345678902, '4000123456789002', '02', '0200', '01', 'ATM', 
    'CASH ADVANCE', 200.00, 'CHASE BANK ATM', 'LOS ANGELES', CURRENT_DATE - 7),
('TX0000000000004', 12345678903, '4000123456789003', '01', '0100', '01', 'MERCHANT', 
    'GAS STATION', 45.23, 'SHELL OIL', 'DALLAS', CURRENT_DATE - 2),
('TX0000000000005', 12345678904, '4000123456789004', '03', '0300', '01', 'ELECTRONIC', 
    'PAYMENT RECEIVED', -500.00, 'ONLINE BANKING', 'MIAMI', CURRENT_DATE - 1);

-- Insert transaction category balances
INSERT INTO transaction_category_balance (account_id, category_code, balance_date, balance) VALUES
(12345678901, '0100', CURRENT_DATE, 215.66),
(12345678901, '0200', CURRENT_DATE, 0.00),
(12345678902, '0100', CURRENT_DATE, 1845.50),
(12345678902, '0200', CURRENT_DATE, 200.00),
(12345678903, '0100', CURRENT_DATE, 567.25),
(12345678904, '0100', CURRENT_DATE, 3623.88),
(12345678905, '0100', CURRENT_DATE, 890.33);

-- Insert card cross-reference data
INSERT INTO card_xref (xref_card_num, xref_cust_id, xref_acct_id) VALUES
('4000123456789001', 1000000001, 12345678901),
('4000123456789002', 1000000002, 12345678902),
('4000123456789003', 1000000003, 12345678903),
('4000123456789004', 1000000004, 12345678904),
('4000123456789005', 1000000005, 12345678905);

-- Insert disclosure groups
INSERT INTO disclosure_groups (account_group_id, transaction_type_code, interest_rate, 
    terms_text, disclosure_group_name) VALUES
('STANDARD', '01', 19.9900, 'Standard purchase APR terms and conditions', 'Standard Rate Group'),
('PREMIUM', '01', 16.9900, 'Premium purchase APR terms and conditions', 'Premium Rate Group'),
('BASIC', '01', 22.9900, 'Basic purchase APR terms and conditions', 'Basic Rate Group'),
('PLATINUM', '01', 14.9900, 'Platinum purchase APR terms and conditions', 'Platinum Rate Group'),
('GOLD', '01', 17.9900, 'Gold purchase APR terms and conditions', 'Gold Rate Group');

-- Insert interest rates
INSERT INTO interest_rate (account_group_id, transaction_type_code, current_apr, effective_date, daily_rate) VALUES
('STANDARD', '01', 19.9900, '2024-01-01', 0.00054767),
('PREMIUM', '01', 16.9900, '2024-01-01', 0.00046548),
('BASIC', '01', 22.9900, '2024-01-01', 0.00062986),
('PLATINUM', '01', 14.9900, '2024-01-01', 0.00041068),
('GOLD', '01', 17.9900, '2024-01-01', 0.00049260);

-- Insert test statements
INSERT INTO statement (account_id, statement_date, due_date, previous_balance, current_balance, minimum_payment) VALUES
(12345678901, CURRENT_DATE - 30, CURRENT_DATE + 25, 1125.75, 1250.75, 25.00),
(12345678902, CURRENT_DATE - 30, CURRENT_DATE + 25, 2145.50, 2345.50, 47.00),
(12345678903, CURRENT_DATE - 30, CURRENT_DATE + 25, 445.25, 567.25, 15.00),
(12345678904, CURRENT_DATE - 30, CURRENT_DATE + 25, 3623.88, 4123.88, 82.48),
(12345678905, CURRENT_DATE - 30, CURRENT_DATE + 25, 765.33, 890.33, 18.00);

-- Insert test fees
INSERT INTO fee (account_id, fee_type, fee_amount, assessment_date, fee_status, description) VALUES
(12345678901, 'LATE_PAYMENT', 35.00, CURRENT_DATE - 10, 'POSTED', 'Late payment fee for overdue balance'),
(12345678904, 'OVER_LIMIT', 25.00, CURRENT_DATE - 5, 'ASSESSED', 'Over credit limit fee'),
(12345678905, 'ANNUAL', 95.00, CURRENT_DATE - 365, 'POSTED', 'Annual membership fee');

-- Insert test notifications
INSERT INTO notification (customer_id, notification_type, channel_address, template_id, 
    delivery_status, priority) VALUES
(1000000001, 'EMAIL', 'john.doe@email.com', 'STMT_READY', 'DELIVERED', 'NORMAL'),
(1000000002, 'SMS', '555-987-6543', 'PMT_DUE', 'SENT', 'HIGH'),
(1000000003, 'EMAIL', 'robert.johnson@email.com', 'OVERLIMIT', 'PENDING', 'HIGH'),
(1000000004, 'EMAIL', 'mary.williams@email.com', 'LATE_FEE', 'DELIVERED', 'HIGH'),
(1000000005, 'SMS', '555-345-6789', 'FRAUD_ALERT', 'FAILED', 'URGENT');

-- Insert test reports
INSERT INTO report (report_type, start_date, end_date, status, format, user_id) VALUES
('TRANSACTION_DETAIL', CURRENT_DATE - 30, CURRENT_DATE, 'COMPLETED', 'PDF', 'USR001'),
('DAILY_SUMMARY', CURRENT_DATE - 1, CURRENT_DATE - 1, 'COMPLETED', 'CSV', 'USR002'),
('AUDIT', CURRENT_DATE - 7, CURRENT_DATE, 'PENDING', 'PDF', 'USR005'),
('COMPLIANCE', CURRENT_DATE - 30, CURRENT_DATE, 'COMPLETED', 'TEXT', 'USR001');

-- Insert test disputes
INSERT INTO dispute (transaction_id, account_id, dispute_type, status, reason_code, description, 
    provisional_credit_amount, created_date) VALUES
('TX0000000000002', 12345678901, 'FRAUD', 'OPEN', 'FR01', 'Unauthorized transaction', 129.99, CURRENT_DATE - 5),
('TX0000000000003', 12345678902, 'BILLING_ERROR', 'INVESTIGATION', 'BE02', 'Incorrect amount charged', 200.00, CURRENT_DATE - 10);

-- Insert test daily transactions for batch processing
INSERT INTO daily_transactions (transaction_id, account_id, type_code, category_code, source, 
    description, amount, transaction_date, processing_status) VALUES
('DT000000000001', 12345678901, '01', '0100', 'MERCHANT', 'PENDING GROCERY PURCHASE', 67.89, CURRENT_DATE, 'PENDING'),
('DT000000000002', 12345678902, '01', '0100', 'ONLINE', 'PENDING ONLINE PURCHASE', 234.50, CURRENT_DATE, 'VALIDATED'),
('DT000000000003', 12345678903, '03', '0300', 'ELECTRONIC', 'PENDING PAYMENT', -150.00, CURRENT_DATE, 'POSTED');

-- Insert test authorizations
INSERT INTO authorization_data (card_number, account_id, transaction_amount, authorization_code, 
    approval_status, request_timestamp, response_timestamp, processing_time) VALUES
('4000123456789001', 12345678901, 67.89, 'A12345', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 150),
('4000123456789002', 12345678902, 234.50, 'A12346', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 98),
('4000123456789003', 12345678903, 5000.00, NULL, 'DECLINED', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, 75);

-- Insert test settlements
INSERT INTO settlement (transaction_id, authorization_id, merchant_id, merchant_name, 
    settlement_amount, settlement_date, settlement_status) VALUES
('TX0000000000001', 1, 'MERCH001', 'SAFEWAY STORES', 85.67, CURRENT_DATE - 1, 'PROCESSED'),
('TX0000000000004', 3, 'MERCH002', 'SHELL OIL', 45.23, CURRENT_DATE, 'PENDING');

-- Insert test fee schedules
INSERT INTO fee_schedule (fee_type, account_type, fee_amount, effective_date) VALUES
('LATE_PAYMENT', 'ALL', 35.00, '2024-01-01'),
('OVER_LIMIT', 'ALL', 25.00, '2024-01-01'),
('ANNUAL', 'PREMIUM', 95.00, '2024-01-01'),
('ANNUAL', 'PLATINUM', 0.00, '2024-01-01'),
('FOREIGN_TRANSACTION', 'ALL', NULL, '2024-01-01'); -- Percentage based

-- Insert test audit logs
INSERT INTO audit_log (username, event_type, source_ip, resource_accessed, action_performed, outcome) VALUES
('ADMIN01', 'LOGIN', '192.168.1.100', '/login', 'USER_AUTHENTICATION', 'SUCCESS'),
('USER001', 'TRANSACTION_VIEW', '192.168.1.101', '/transactions/TX0000000000001', 'DATA_ACCESS', 'SUCCESS'),
('TELLER1', 'ACCOUNT_UPDATE', '192.168.1.102', '/accounts/12345678901', 'DATA_MODIFICATION', 'SUCCESS'),
('MANAGER', 'REPORT_GENERATION', '192.168.1.103', '/reports/daily-summary', 'REPORT_ACCESS', 'SUCCESS'),
('AUDITOR', 'SYSTEM_CONFIG', '192.168.1.104', '/config/interest-rates', 'CONFIG_CHANGE', 'SUCCESS');

-- Insert test account closures
INSERT INTO account_closure (account_id, closure_reason_code, requested_date, closure_status) VALUES
(12345678903, 'CUST_REQ', CURRENT_DATE - 15, 'PENDING'),
(12345678905, 'INACTIVE', CURRENT_DATE - 30, 'APPROVED');

-- Insert test archives
INSERT INTO archive (data_type, archive_date, retention_period, storage_path, compression_type) VALUES
('TRANSACTIONS', CURRENT_DATE - 365, 2555, '/archive/2023/transactions.gz', 'GZIP'),
('STATEMENTS', CURRENT_DATE - 90, 2555, '/archive/2024/statements.zip', 'ZIP'),
('AUDIT_LOGS', CURRENT_DATE - 30, 2555, '/archive/2024/audit.tar.gz', 'TAR_GZIP');

COMMIT;