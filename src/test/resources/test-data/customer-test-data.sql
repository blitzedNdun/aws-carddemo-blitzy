-- Test data for CustomerRepositoryTest

-- Insert required disclosure groups first (foreign key dependency)
INSERT INTO PUBLIC.disclosure_groups (group_id, disclosure_text, interest_rate, effective_date) VALUES
('DEFAULT', 'Standard interest rate terms and conditions apply', 0.1299, '2020-01-01 00:00:00'),
('PREMIUM', 'Premium account with preferential interest rate terms', 0.0999, '2020-01-01 00:00:00');

-- Insert test customers (including government_id and middle_name for test compatibility)
INSERT INTO PUBLIC.customers (customer_id, first_name, middle_name, last_name, ssn, government_id, date_of_birth, fico_credit_score, primary_cardholder_indicator, phone_number_1, address_line_1, state_code, zip_code, country_code) VALUES
('100000001', 'John', 'M', 'Smith', '123456789', 'DL-NY-123456789', '1980-01-15', 750, 'Y', '555-0101', '123 Main St', 'NY', '10001', 'USA'),
('100000002', 'Jane', 'A', 'Johnson', '987654321', 'DL-CA-987654321', '1985-06-20', 680, 'N', '555-0102', '456 Oak Ave', 'CA', '90210', 'USA'),
('100000003', 'Bob', 'R', 'Smithson', '456789123', 'DL-TX-456789123', '1975-12-10', 720, 'Y', '555-0103', '789 Pine Rd', 'TX', '77001', 'USA');

-- Insert test accounts - Customer 100000001 gets 2 accounts, others get none to match test expectations
INSERT INTO PUBLIC.accounts (account_id, customer_id, active_status, credit_limit, cash_credit_limit, current_balance, open_date, expiration_date, address_zip, group_id) VALUES
('12345678901', '100000001', 'Y', 5000.00, 1000.00, 1500.75, '2020-01-01', '2025-12-31', '10001', 'DEFAULT'),
('12345678902', '100000001', 'Y', 10000.00, 2000.00, 2750.50, '2021-03-15', '2026-03-31', '10001', 'PREMIUM');