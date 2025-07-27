-- Account test data for customer relationship tests
-- Links to customer test data (customer IDs: 100000001, 100000002, 100000003)

-- Customer 100000001 (John Smith) - should have 2 accounts for relationship tests
INSERT INTO PUBLIC.account_data (account_id, customer_id, active_status, current_balance, credit_limit, cash_credit_limit, current_cycle_credit, current_cycle_debit, open_date, address_zip, group_id, version) VALUES
('10000000101', '100000001', 'Y', 1500.00, 5000.00, 1000.00, 200.00, 1700.00, '2023-01-15', '10001', 'G001', 0),
('10000000102', '100000001', 'Y', 750.25, 3000.00, 500.00, 150.00, 900.25, '2023-02-20', '10001', 'G001', 0);

-- Customer 100000002 (Jane Johnson) - no accounts (for testing JOIN FETCH with no accounts)

-- Customer 100000003 (Bob Smithson) - single account
INSERT INTO PUBLIC.account_data (account_id, customer_id, active_status, current_balance, credit_limit, cash_credit_limit, current_cycle_credit, current_cycle_debit, open_date, address_zip, group_id, version) VALUES
('10000000301', '100000003', 'Y', 980.50, 4000.00, 800.00, 100.00, 1080.50, '2023-04-05', '77001', 'G001', 0);