-- ========================================================================
-- PostgreSQL Expected Data After VSAM Migration
-- ========================================================================
-- 
-- This script contains expected PostgreSQL INSERT statements demonstrating
-- proper COBOL-to-PostgreSQL data type conversions for migration validation.
-- 
-- Key Conversion Patterns:
-- - COBOL PIC S9(10)V99 COMP-3 → PostgreSQL NUMERIC(12,2) (monetary values)
-- - COBOL PIC 9(11) → PostgreSQL BIGINT (account IDs)
-- - COBOL PIC 9(09) → PostgreSQL BIGINT (customer IDs)
-- - COBOL PIC X(n) → PostgreSQL VARCHAR(n) (text fields)
-- - COBOL PIC X(10) dates → PostgreSQL DATE
-- - COBOL PIC X(26) timestamps → PostgreSQL TIMESTAMP
-- 
-- Financial Precision Requirements:
-- - All monetary amounts use NUMERIC(12,2) to preserve COMP-3 decimal precision
-- - Scale of 2 decimal places maintains penny-level accuracy
-- - Precision of 12 supports account balances up to 999,999,999.99
-- 
-- ========================================================================

-- ========================================================================
-- USER SECURITY DATA (USRSEC → user_security)
-- Source: CSUSR01Y.cpy equivalent structure
-- ========================================================================

INSERT INTO user_security (
    user_id,
    user_name,
    password_hash,
    user_type,
    first_name,
    last_name,
    active_status,
    created_date,
    last_modified_date
) VALUES 
-- Regular user accounts
(1000000001, 'admin01', '$2a$12$kB.QO8K.X9r7eL.vN.fP8uF1QjCK.H3.ZM.nR.Lq.P9.S', 'A', 'System', 'Administrator', 'Y', '2024-01-01', '2024-01-01'),
(1000000002, 'user01', '$2a$12$mC.RT9L.Y0s8gM.wO.gQ9vG2RjDL.I4.AN.oS.Mr.Q0.T', 'U', 'John', 'Smith', 'Y', '2024-01-01', '2024-01-01'),
(1000000003, 'user02', '$2a$12$nD.SU0M.Z1t9hN.xP.hR0wH3SkEM.J5.BO.pT.Ns.R1.U', 'U', 'Jane', 'Doe', 'Y', '2024-01-01', '2024-01-01'),
-- Test user for validation
(1000000004, 'testuser', '$2a$12$oE.TV1N.A2u0iO.yQ.iS1xI4TlFN.K6.CP.qU.Ot.S2.V', 'U', 'Test', 'User', 'Y', '2024-01-01', '2024-01-01');

-- ========================================================================
-- CUSTOMER DATA (CUSTDAT → customer_data)
-- Source: CUSTREC.cpy - Customer record structure (RECLN 500)
-- ========================================================================

INSERT INTO customer_data (
    customer_id,
    first_name,
    middle_name,
    last_name,
    address_line_1,
    address_line_2,
    address_line_3,
    state_code,
    country_code,
    zip_code,
    phone_number_1,
    phone_number_2,
    ssn,
    government_id,
    date_of_birth,
    eft_account_id,
    primary_card_holder,
    fico_score,
    created_date,
    last_modified_date
) VALUES 
-- Customer 1: Standard profile with complete data
(1000000001, 'John', 'Michael', 'Smith', '123 Main Street', 'Apt 4B', '', 'TX', 'USA', '75201-1234', '214-555-0001', '214-555-0002', '123456789', 'DL-TX-12345678', '1980-05-15', 'EFT1001', 'Y', 750, '2024-01-01', '2024-01-01'),

-- Customer 2: Premium customer with high credit score
(1000000002, 'Jane', 'Elizabeth', 'Doe', '456 Oak Avenue', 'Suite 200', 'Building A', 'CA', 'USA', '90210-5678', '310-555-0003', '310-555-0004', '987654321', 'DL-CA-87654321', '1975-12-22', 'EFT1002', 'Y', 820, '2024-01-01', '2024-01-01'),

-- Customer 3: Basic profile for testing minimal data
(1000000003, 'Robert', '', 'Johnson', '789 Pine Street', '', '', 'NY', 'USA', '10001-0000', '212-555-0005', '', '456789123', 'DL-NY-45678912', '1990-03-10', 'EFT1003', 'Y', 680, '2024-01-01', '2024-01-01'),

-- Customer 4: Test customer for validation scenarios
(1000000004, 'Maria', 'Carmen', 'Rodriguez', '321 Elm Drive', 'Unit 15', '', 'FL', 'USA', '33101-7890', '305-555-0006', '305-555-0007', '789123456', 'DL-FL-78912345', '1985-08-28', 'EFT1004', 'Y', 720, '2024-01-01', '2024-01-01');

-- ========================================================================
-- ACCOUNT DATA (ACCTDAT → account_data)
-- Source: CVACT01Y.cpy - Account record structure (RECLN 300)
-- CRITICAL: COMP-3 decimal conversion to NUMERIC(12,2)
-- ========================================================================

INSERT INTO account_data (
    account_id,
    customer_id,
    active_status,
    current_balance,           -- NUMERIC(12,2) from COBOL PIC S9(10)V99 COMP-3
    credit_limit,              -- NUMERIC(12,2) from COBOL PIC S9(10)V99 COMP-3  
    cash_credit_limit,         -- NUMERIC(12,2) from COBOL PIC S9(10)V99 COMP-3
    open_date,
    expiration_date,
    reissue_date,
    current_cycle_credit,      -- NUMERIC(12,2) from COBOL PIC S9(10)V99 COMP-3
    current_cycle_debit,       -- NUMERIC(12,2) from COBOL PIC S9(10)V99 COMP-3
    zip_code,
    group_id,
    disclosure_group_id,
    created_date,
    last_modified_date
) VALUES 
-- Account 1: Standard account with typical balances
(10000000001, 1000000001, 'Y', 1250.75, 5000.00, 1000.00, '2023-01-15', '2026-01-31', '2023-01-15', 850.25, 1100.50, '75201-1234', 'GROUP001', 1, '2024-01-01', '2024-01-01'),

-- Account 2: Premium account with high limits
(10000000002, 1000000002, 'Y', 3750.50, 25000.00, 5000.00, '2022-06-10', '2025-06-30', '2022-06-10', 2100.75, 1650.25, '90210-5678', 'GROUP002', 2, '2024-01-01', '2024-01-01'),

-- Account 3: Basic account with lower limits
(10000000003, 1000000003, 'Y', 425.25, 2000.00, 500.00, '2023-09-20', '2026-09-30', '2023-09-20', 325.00, 750.75, '10001-0000', 'GROUP001', 1, '2024-01-01', '2024-01-01'),

-- Account 4: Test account with zero balance
(10000000004, 1000000004, 'Y', 0.00, 3000.00, 750.00, '2024-01-01', '2027-01-31', '2024-01-01', 0.00, 0.00, '33101-7890', 'GROUP001', 1, '2024-01-01', '2024-01-01'),

-- Account 5: Inactive account for testing
(10000000005, 1000000001, 'N', 150.00, 1000.00, 250.00, '2021-03-15', '2024-03-31', '2021-03-15', 0.00, 150.00, '75201-1234', 'GROUP003', 3, '2024-01-01', '2024-01-01');

-- ========================================================================
-- CARD DATA (CARDDAT → card_data)
-- Composite primary key structure matching VSAM organization
-- ========================================================================

INSERT INTO card_data (
    card_number,
    account_id,
    customer_id,
    cvv_code,
    embossed_name,
    expiration_date,
    active_status,
    card_type,
    issue_date,
    created_date,
    last_modified_date
) VALUES 
-- Primary cards for each account
('4000000000000001', 10000000001, 1000000001, '123', 'JOHN M SMITH', '2026-01-31', 'Y', 'VISA', '2023-01-15', '2024-01-01', '2024-01-01'),
('4000000000000002', 10000000002, 1000000002, '456', 'JANE E DOE', '2025-06-30', 'Y', 'VISA', '2022-06-10', '2024-01-01', '2024-01-01'),
('4000000000000003', 10000000003, 1000000003, '789', 'ROBERT JOHNSON', '2026-09-30', 'Y', 'VISA', '2023-09-20', '2024-01-01', '2024-01-01'),
('4000000000000004', 10000000004, 1000000004, '012', 'MARIA C RODRIGUEZ', '2027-01-31', 'Y', 'VISA', '2024-01-01', '2024-01-01', '2024-01-01'),

-- Additional cards for testing multi-card scenarios
('4000000000000005', 10000000001, 1000000001, '345', 'JOHN M SMITH', '2026-01-31', 'Y', 'MSTR', '2023-01-15', '2024-01-01', '2024-01-01'),
('4000000000000006', 10000000005, 1000000001, '678', 'JOHN M SMITH', '2024-03-31', 'N', 'VISA', '2021-03-15', '2024-01-01', '2024-01-01');

-- ========================================================================
-- TRANSACTION DATA (TRANSACT → transactions)
-- Source: CVTRA05Y.cpy - Transaction record structure (RECLN 350)
-- Partitioned by transaction_date for performance optimization
-- ========================================================================

INSERT INTO transactions (
    transaction_id,
    account_id,
    card_number,
    transaction_date,
    transaction_time,
    amount,                    -- NUMERIC(11,2) from COBOL PIC S9(09)V99 COMP-3
    transaction_type_code,
    category_code,
    source,
    description,
    merchant_id,
    merchant_name,
    merchant_city,
    merchant_zip,
    original_timestamp,
    processed_timestamp,
    created_date,
    last_modified_date
) VALUES 
-- Recent transactions for Account 1
('T2024010100000001', 10000000001, '4000000000000001', '2024-01-01', '10:30:00', -45.67, 'PU', '5411', 'POS', 'GROCERY STORE PURCHASE', 123456789, 'WHOLE FOODS MARKET', 'DALLAS', '75201', '2024-01-01 10:30:00', '2024-01-01 10:30:15', '2024-01-01', '2024-01-01'),
('T2024010100000002', 10000000001, '4000000000000001', '2024-01-01', '14:15:30', -89.23, 'PU', '5812', 'POS', 'RESTAURANT PURCHASE', 987654321, 'ITALIAN BISTRO', 'DALLAS', '75201', '2024-01-01 14:15:30', '2024-01-01 14:15:45', '2024-01-01', '2024-01-01'),
('T2024010200000003', 10000000001, '4000000000000001', '2024-01-02', '09:45:00', 1500.00, 'CR', '0000', 'PAY', 'PAYMENT RECEIVED', 0, 'ONLINE PAYMENT', '', '', '2024-01-02 09:45:00', '2024-01-02 09:45:05', '2024-01-02', '2024-01-02'),

-- Transactions for Account 2 with higher amounts
('T2024010100000004', 10000000002, '4000000000000002', '2024-01-01', '16:20:00', -234.56, 'PU', '4511', 'POS', 'AIRLINE TICKET PURCHASE', 456789123, 'AMERICAN AIRLINES', 'LOS ANGELES', '90210', '2024-01-01 16:20:00', '2024-01-01 16:20:10', '2024-01-01', '2024-01-01'),
('T2024010200000005', 10000000002, '4000000000000002', '2024-01-02', '11:30:00', -1250.75, 'PU', '5999', 'POS', 'ELECTRONICS PURCHASE', 789123456, 'BEST BUY', 'BEVERLY HILLS', '90210', '2024-01-02 11:30:00', '2024-01-02 11:30:20', '2024-01-02', '2024-01-02'),

-- Cash advance transactions
('T2024010300000006', 10000000003, '4000000000000003', '2024-01-03', '13:45:00', -300.00, 'CA', '6011', 'ATM', 'CASH ADVANCE', 0, 'ATM WITHDRAWAL', 'NEW YORK', '10001', '2024-01-03 13:45:00', '2024-01-03 13:45:05', '2024-01-03', '2024-01-03'),

-- Balance transfer transaction
('T2024010400000007', 10000000004, '4000000000000004', '2024-01-04', '08:00:00', -500.00, 'BT', '0000', 'TRF', 'BALANCE TRANSFER', 0, 'BALANCE TRANSFER', '', '', '2024-01-04 08:00:00', '2024-01-04 08:00:10', '2024-01-04', '2024-01-04');

-- ========================================================================
-- REFERENCE DATA TABLES
-- ========================================================================

-- Transaction Types (TRANTYPE → transaction_types)
INSERT INTO transaction_types (
    transaction_type_code,
    description,
    debit_credit_flag,
    created_date,
    last_modified_date
) VALUES 
('PU', 'Purchase Transaction', 'D', '2024-01-01', '2024-01-01'),
('CR', 'Credit/Payment', 'C', '2024-01-01', '2024-01-01'), 
('CA', 'Cash Advance', 'D', '2024-01-01', '2024-01-01'),
('BT', 'Balance Transfer', 'D', '2024-01-01', '2024-01-01'),
('FE', 'Fee Transaction', 'D', '2024-01-01', '2024-01-01'),
('IN', 'Interest Charge', 'D', '2024-01-01', '2024-01-01'),
('AD', 'Adjustment', 'C', '2024-01-01', '2024-01-01');

-- Transaction Categories (TRANCATG → transaction_categories)
INSERT INTO transaction_categories (
    category_code,
    subcategory_code,
    description,
    category_name,
    created_date,
    last_modified_date
) VALUES 
('5411', '01', 'Grocery Stores and Supermarkets', 'Groceries', '2024-01-01', '2024-01-01'),
('5812', '01', 'Eating Places and Restaurants', 'Dining', '2024-01-01', '2024-01-01'),
('4511', '01', 'Airlines and Air Carriers', 'Travel', '2024-01-01', '2024-01-01'),
('5999', '01', 'Miscellaneous Retail', 'Retail', '2024-01-01', '2024-01-01'),
('6011', '01', 'Automated Cash Disbursements', 'ATM', '2024-01-01', '2024-01-01'),
('0000', '01', 'Default Category', 'Other', '2024-01-01', '2024-01-01');

-- Disclosure Groups (DISCGRP → disclosure_groups)
INSERT INTO disclosure_groups (
    disclosure_group_id,
    group_name,
    interest_rate,              -- NUMERIC(5,4) for precise interest rates
    terms_text,
    created_date,
    last_modified_date
) VALUES 
(1, 'Standard Credit Cards', 18.9900, 'Standard terms and conditions apply for credit card accounts with variable APR based on creditworthiness.', '2024-01-01', '2024-01-01'),
(2, 'Premium Credit Cards', 15.9900, 'Premium account terms with lower APR for qualified customers with excellent credit history.', '2024-01-01', '2024-01-01'),
(3, 'Promotional Credit Cards', 0.0000, 'Promotional 0% APR for qualifying purchases during introductory period.', '2024-01-01', '2024-01-01');

-- Transaction Category Balances (TCATBAL → transaction_category_balance)
-- Composite key: (account_id, category_code, balance_date)
INSERT INTO transaction_category_balance (
    account_id,
    category_code,
    balance_date,
    balance,                   -- NUMERIC(12,2) from COBOL COMP-3 structure
    created_date,
    last_modified_date
) VALUES 
-- Account 1 category balances
(10000000001, '5411', '2024-01-31', 145.67, '2024-01-01', '2024-01-01'),
(10000000001, '5812', '2024-01-31', 289.23, '2024-01-01', '2024-01-01'),
(10000000001, '0000', '2024-01-31', 0.00, '2024-01-01', '2024-01-01'),

-- Account 2 category balances  
(10000000002, '4511', '2024-01-31', 234.56, '2024-01-01', '2024-01-01'),
(10000000002, '5999', '2024-01-31', 1250.75, '2024-01-01', '2024-01-01'),

-- Account 3 category balances
(10000000003, '6011', '2024-01-31', 300.00, '2024-01-01', '2024-01-01');

-- ========================================================================
-- DATA VALIDATION COMMENTS
-- ========================================================================

-- PRECISION VALIDATION:
-- 1. All monetary fields use NUMERIC(12,2) ensuring exact decimal precision
-- 2. Interest rates use NUMERIC(5,4) for basis point accuracy
-- 3. No floating point types used to prevent rounding errors
-- 
-- COMP-3 CONVERSION VALIDATION:
-- - COBOL PIC S9(10)V99 COMP-3 → PostgreSQL NUMERIC(12,2)
-- - COBOL PIC S9(09)V99 COMP-3 → PostgreSQL NUMERIC(11,2)
-- - Preserved sign handling and decimal positioning
-- 
-- DATE CONVERSION VALIDATION:
-- - COBOL PIC X(10) YYYYMMDD → PostgreSQL DATE with proper formatting
-- - COBOL PIC X(26) timestamp → PostgreSQL TIMESTAMP with time zones
-- 
-- FOREIGN KEY VALIDATION:
-- - All relationships maintained through proper constraint definitions
-- - Composite keys preserve VSAM access patterns
-- - Referential integrity enforced at database level
-- 
-- CHARACTER ENCODING VALIDATION:
-- - COBOL PIC X fields converted to appropriate VARCHAR lengths
-- - EBCDIC to UTF-8 conversion handled in application layer
-- - Preserved original field lengths for compatibility

-- ========================================================================
-- End of Expected PostgreSQL Migration Data
-- ========================================================================