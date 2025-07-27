-- Test data for CustomerRepositoryTest

-- Insert required disclosure groups first (foreign key dependency)
INSERT INTO PUBLIC.disclosure_groups (group_id, disclosure_text, interest_rate, effective_date) VALUES
('DEFAULT', 'Standard interest rate terms and conditions apply', 0.1299, '2020-01-01 00:00:00'),
('PREMIUM', 'Premium account with preferential interest rate terms', 0.0999, '2020-01-01 00:00:00');

-- Insert test customers
INSERT INTO PUBLIC.customers (customer_id, first_name, last_name, ssn, date_of_birth, fico_credit_score, primary_cardholder_indicator, phone_number_1, address_line_1, state_code, zip_code, country_code) VALUES
('100000001', 'John', 'Smith', '123456789', '1980-01-15', 750, 'Y', '555-0101', '123 Main St', 'NY', '10001', 'USA'),
('100000002', 'Jane', 'Johnson', '987654321', '1985-06-20', 680, 'N', '555-0102', '456 Oak Ave', 'CA', '90210', 'USA'),
('100000003', 'Bob', 'Smithson', '456789123', '1975-12-10', 720, 'Y', '555-0103', '789 Pine Rd', 'TX', '77001', 'USA');

-- Insert test accounts (referencing the disclosure groups above)
INSERT INTO PUBLIC.accounts (account_id, customer_id, active_status, credit_limit, cash_credit_limit, current_balance, open_date, expiration_date, address_zip, group_id) VALUES
('12345678901', '100000001', 'Y', 5000.00, 1000.00, 1500.75, '2020-01-01', '2025-12-31', '10001', 'DEFAULT'),
('98765432109', '100000002', 'Y', 3000.00, 500.00, 850.25, '2021-06-15', '2026-06-30', '90210', 'PREMIUM');