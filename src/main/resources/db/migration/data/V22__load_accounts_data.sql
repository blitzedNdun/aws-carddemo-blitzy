-- ============================================================================
-- Liquibase Migration: V22__load_accounts_data.sql
-- Description: Load account data from acctdata.txt ASCII source file
-- Author: Blitzy agent
-- Version: 22.0
-- Migration Type: Data Loading Script
-- Source: app/data/ASCII/acctdata.txt (delimited records with "{" separator)
-- Target: accounts table with precise financial field mapping and foreign key relationships
-- Dependencies: V3__create_accounts_table.sql, V21__load_customers_data.sql, V6__create_reference_tables.sql
-- ============================================================================

-- ============================================================================
-- SECTION 1: MIGRATION METADATA AND CONFIGURATION
-- ============================================================================

-- Liquibase changeset for account data loading
-- This migration populates the accounts table with data from acctdata.txt
-- Preserves exact VSAM ACCTDAT record structure with proper data type conversion
-- Implements comprehensive data validation and error handling for financial precision

-- ============================================================================
-- SECTION 2: ACCOUNT DATA LOADING FROM ACCTDATA.TXT
-- ============================================================================

-- Load account data with exact field parsing from acctdata.txt
-- Each record contains account information including:
-- - 11-digit account_id (positions 1-11) 
-- - Account active status (Y/N indicator)
-- - 9-digit customer_id (foreign key reference)
-- - Financial fields with DECIMAL(12,2) precision (current_balance, credit_limit, cash_credit_limit)
-- - Account lifecycle dates (open_date, expiration_date, reissue_date)
-- - Current cycle financial data (current_cycle_credit, current_cycle_debit)
-- - Disclosure group association (group_id)

INSERT INTO accounts (
    account_id,
    customer_id,
    active_status,
    current_balance,
    credit_limit,
    cash_credit_limit,
    open_date,
    expiration_date,
    reissue_date,
    current_cycle_credit,
    current_cycle_debit,
    group_id
) VALUES
    -- Account ID: 00000000001, Customer ID: 00000001940, Active: Y
    -- Financial data: Balance=202.00, Credit Limit=1020.00, Cash Credit Limit=102.00
    -- Dates: Open=2014-11-20, Expiration=2025-05-20, Reissue=2025-05-20
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000001', '00000001940', TRUE, 202.00, 1020.00, 102.00, '2014-11-20', '2025-05-20', '2025-05-20', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000002, Customer ID: 00000001580, Active: Y
    -- Financial data: Balance=613.00, Credit Limit=544.80, Cash Credit Limit=613.00
    -- Dates: Open=2013-06-19, Expiration=2024-08-11, Reissue=2024-08-11
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000002', '00000001580', TRUE, 613.00, 544.80, 613.00, '2013-06-19', '2024-08-11', '2024-08-11', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000003, Customer ID: 00000001470, Active: Y
    -- Financial data: Balance=490.90, Credit Limit=53.80, Cash Credit Limit=490.90
    -- Dates: Open=2013-08-23, Expiration=2024-01-10, Reissue=2024-01-10
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000003', '00000001470', TRUE, 490.90, 53.80, 490.90, '2013-08-23', '2024-01-10', '2024-01-10', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000004, Customer ID: 00000000400, Active: Y
    -- Financial data: Balance=350.30, Credit Limit=278.90, Cash Credit Limit=350.30
    -- Dates: Open=2012-11-17, Expiration=2023-12-16, Reissue=2023-12-16
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000004', '00000000400', TRUE, 350.30, 278.90, 350.30, '2012-11-17', '2023-12-16', '2023-12-16', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000005, Customer ID: 00000003450, Active: Y
    -- Financial data: Balance=381.90, Credit Limit=243.00, Cash Credit Limit=381.90
    -- Dates: Open=2012-10-03, Expiration=2025-03-09, Reissue=2025-03-09
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000005', '00000003450', TRUE, 381.90, 243.00, 381.90, '2012-10-03', '2025-03-09', '2025-03-09', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000006, Customer ID: 00000002180, Active: Y
    -- Financial data: Balance=358.40, Credit Limit=294.80, Cash Credit Limit=358.40
    -- Dates: Open=2017-12-23, Expiration=2025-10-08, Reissue=2025-10-08
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000006', '00000002180', TRUE, 358.40, 294.80, 358.40, '2017-12-23', '2025-10-08', '2025-10-08', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000007, Customer ID: 00000001930, Active: Y
    -- Financial data: Balance=206.50, Credit Limit=26.40, Cash Credit Limit=206.50
    -- Dates: Open=2012-10-12, Expiration=2024-12-13, Reissue=2024-12-13
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000007', '00000001930', TRUE, 206.50, 26.40, 206.50, '2012-10-12', '2024-12-13', '2024-12-13', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000008, Customer ID: 00000006050, Active: Y
    -- Financial data: Balance=610.40, Credit Limit=131.80, Cash Credit Limit=610.40
    -- Dates: Open=2012-01-04, Expiration=2024-05-20, Reissue=2024-05-20
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000008', '00000006050', TRUE, 610.40, 131.80, 610.40, '2012-01-04', '2024-05-20', '2024-05-20', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000009, Customer ID: 00000005600, Active: Y
    -- Financial data: Balance=820.10, Credit Limit=206.50, Cash Credit Limit=820.10
    -- Dates: Open=2016-08-27, Expiration=2024-12-27, Reissue=2024-12-27
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000009', '00000005600', TRUE, 820.10, 206.50, 820.10, '2016-08-27', '2024-12-27', '2024-12-27', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000010, Customer ID: 00000001590, Active: Y
    -- Financial data: Balance=540.10, Credit Limit=444.20, Cash Credit Limit=540.10
    -- Dates: Open=2015-09-13, Expiration=2023-01-27, Reissue=2023-01-27
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000010', '00000001590', TRUE, 540.10, 444.20, 540.10, '2015-09-13', '2023-01-27', '2023-01-27', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000011, Customer ID: 00000002120, Active: Y
    -- Financial data: Balance=499.80, Credit Limit=317.50, Cash Credit Limit=499.80
    -- Dates: Open=2014-09-12, Expiration=2025-03-12, Reissue=2025-03-12
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000011', '00000002120', TRUE, 499.80, 317.50, 499.80, '2014-09-12', '2025-03-12', '2025-03-12', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000012, Customer ID: 00000001760, Active: Y
    -- Financial data: Balance=463.60, Credit Limit=38.80, Cash Credit Limit=463.60
    -- Dates: Open=2009-06-17, Expiration=2023-07-07, Reissue=2023-07-07
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000012', '00000001760', TRUE, 463.60, 38.80, 463.60, '2009-06-17', '2023-07-07', '2023-07-07', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000013, Customer ID: 00000000410, Active: Y
    -- Financial data: Balance=754.20, Credit Limit=492.20, Cash Credit Limit=754.20
    -- Dates: Open=2017-10-01, Expiration=2024-08-04, Reissue=2024-08-04
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000013', '00000000410', TRUE, 754.20, 492.20, 754.20, '2017-10-01', '2024-08-04', '2024-08-04', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000014, Customer ID: 00000000150, Active: Y
    -- Financial data: Balance=225.40, Credit Limit=21.20, Cash Credit Limit=225.40
    -- Dates: Open=2010-12-04, Expiration=2025-12-11, Reissue=2025-12-11
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000014', '00000000150', TRUE, 225.40, 21.20, 225.40, '2010-12-04', '2025-12-11', '2025-12-11', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000015, Customer ID: 00000004890, Active: Y
    -- Financial data: Balance=844.10, Credit Limit=383.30, Cash Credit Limit=844.10
    -- Dates: Open=2009-10-06, Expiration=2025-06-09, Reissue=2025-06-09
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000015', '00000004890', TRUE, 844.10, 383.30, 844.10, '2009-10-06', '2025-06-09', '2025-06-09', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000016, Customer ID: 00000007330, Active: Y
    -- Financial data: Balance=892.20, Credit Limit=263.20, Cash Credit Limit=892.20
    -- Dates: Open=2014-09-11, Expiration=2024-01-25, Reissue=2024-01-25
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000016', '00000007330', TRUE, 892.20, 263.20, 892.20, '2014-09-11', '2024-01-25', '2024-01-25', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000017, Customer ID: 00000000330, Active: Y
    -- Financial data: Balance=56.80, Credit Limit=51.00, Cash Credit Limit=56.80
    -- Dates: Open=2014-05-17, Expiration=2025-03-01, Reissue=2025-03-01
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000017', '00000000330', TRUE, 56.80, 51.00, 56.80, '2014-05-17', '2025-03-01', '2025-03-01', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000018, Customer ID: 00000001440, Active: Y
    -- Financial data: Balance=290.30, Credit Limit=149.60, Cash Credit Limit=290.30
    -- Dates: Open=2018-11-15, Expiration=2023-09-10, Reissue=2023-09-10
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000018', '00000001440', TRUE, 290.30, 149.60, 290.30, '2018-11-15', '2023-09-10', '2023-09-10', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000019, Customer ID: 00000004800, Active: Y
    -- Financial data: Balance=698.60, Credit Limit=372.30, Cash Credit Limit=698.60
    -- Dates: Open=2011-12-14, Expiration=2025-07-23, Reissue=2025-07-23
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000019', '00000004800', TRUE, 698.60, 372.30, 698.60, '2011-12-14', '2025-07-23', '2025-07-23', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000020, Customer ID: 00000003690, Active: Y
    -- Financial data: Balance=376.70, Credit Limit=104.00, Cash Credit Limit=376.70
    -- Dates: Open=2014-02-27, Expiration=2024-03-13, Reissue=2024-03-13
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000020', '00000003690', TRUE, 376.70, 104.00, 376.70, '2014-02-27', '2024-03-13', '2024-03-13', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000021, Customer ID: 00000001120, Active: Y
    -- Financial data: Balance=126.40, Credit Limit=18.00, Cash Credit Limit=126.40
    -- Dates: Open=2011-10-19, Expiration=2023-01-06, Reissue=2023-01-06
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000021', '00000001120', TRUE, 126.40, 18.00, 126.40, '2011-10-19', '2023-01-06', '2023-01-06', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000022, Customer ID: 00000000550, Active: Y
    -- Financial data: Balance=859.90, Credit Limit=471.20, Cash Credit Limit=859.90
    -- Dates: Open=2016-11-21, Expiration=2025-12-28, Reissue=2025-12-28
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000022', '00000000550', TRUE, 859.90, 471.20, 859.90, '2016-11-21', '2025-12-28', '2025-12-28', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000023, Customer ID: 00000001040, Active: Y
    -- Financial data: Balance=337.70, Credit Limit=290.40, Cash Credit Limit=337.70
    -- Dates: Open=2012-03-15, Expiration=2025-03-18, Reissue=2025-03-18
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000023', '00000001040', TRUE, 337.70, 290.40, 337.70, '2012-03-15', '2025-03-18', '2025-03-18', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000024, Customer ID: 00000004000, Active: Y
    -- Financial data: Balance=517.40, Credit Limit=412.90, Cash Credit Limit=517.40
    -- Dates: Open=2015-08-08, Expiration=2025-02-11, Reissue=2025-02-11
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000024', '00000004000', TRUE, 517.40, 412.90, 517.40, '2015-08-08', '2025-02-11', '2025-02-11', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000025, Customer ID: 00000000610, Active: Y
    -- Financial data: Balance=819.40, Credit Limit=658.20, Cash Credit Limit=819.40
    -- Dates: Open=2012-10-26, Expiration=2025-07-10, Reissue=2025-07-10
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000025', '00000000610', TRUE, 819.40, 658.20, 819.40, '2012-10-26', '2025-07-10', '2025-07-10', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000026, Customer ID: 00000000460, Active: Y
    -- Financial data: Balance=218.10, Credit Limit=137.50, Cash Credit Limit=218.10
    -- Dates: Open=2009-04-20, Expiration=2024-12-19, Reissue=2024-12-19
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000026', '00000000460', TRUE, 218.10, 137.50, 218.10, '2009-04-20', '2024-12-19', '2024-12-19', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000027, Customer ID: 00000002840, Active: Y
    -- Financial data: Balance=557.20, Credit Limit=207.50, Cash Credit Limit=557.20
    -- Dates: Open=2012-09-30, Expiration=2025-07-13, Reissue=2025-07-13
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000027', '00000002840', TRUE, 557.20, 207.50, 557.20, '2012-09-30', '2025-07-13', '2025-07-13', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000028, Customer ID: 00000000680, Active: Y
    -- Financial data: Balance=86.80, Credit Limit=54.70, Cash Credit Limit=86.80
    -- Dates: Open=2015-05-20, Expiration=2024-05-09, Reissue=2024-05-09
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000028', '00000000680', TRUE, 86.80, 54.70, 86.80, '2015-05-20', '2024-05-09', '2024-05-09', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000029, Customer ID: 00000003390, Active: Y
    -- Financial data: Balance=551.10, Credit Limit=436.10, Cash Credit Limit=551.10
    -- Dates: Open=2015-11-03, Expiration=2024-06-04, Reissue=2024-06-04
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000029', '00000003390', TRUE, 551.10, 436.10, 551.10, '2015-11-03', '2024-06-04', '2024-06-04', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000030, Customer ID: 00000000020, Active: Y
    -- Financial data: Balance=12.00, Credit Limit=9.30, Cash Credit Limit=12.00
    -- Dates: Open=2011-08-26, Expiration=2024-06-27, Reissue=2024-06-27
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000030', '00000000020', TRUE, 12.00, 9.30, 12.00, '2011-08-26', '2024-06-27', '2024-06-27', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000031, Customer ID: 00000000310, Active: Y
    -- Financial data: Balance=114.00, Credit Limit=107.70, Cash Credit Limit=114.00
    -- Dates: Open=2017-02-25, Expiration=2025-06-08, Reissue=2025-06-08
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000031', '00000000310', TRUE, 114.00, 107.70, 114.00, '2017-02-25', '2025-06-08', '2025-06-08', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000032, Customer ID: 00000000300, Active: Y
    -- Financial data: Balance=117.50, Credit Limit=84.60, Cash Credit Limit=117.50
    -- Dates: Open=2013-11-10, Expiration=2025-05-19, Reissue=2025-05-19
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000032', '00000000300', TRUE, 117.50, 84.60, 117.50, '2013-11-10', '2025-05-19', '2025-05-19', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000033, Customer ID: 00000004100, Active: Y
    -- Financial data: Balance=640.40, Credit Limit=95.10, Cash Credit Limit=640.40
    -- Dates: Open=2012-10-11, Expiration=2025-10-07, Reissue=2025-10-07
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000033', '00000004100', TRUE, 640.40, 95.10, 640.40, '2012-10-11', '2025-10-07', '2025-10-07', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000034, Customer ID: 00000002530, Active: Y
    -- Financial data: Balance=364.20, Credit Limit=277.00, Cash Credit Limit=364.20
    -- Dates: Open=2009-05-10, Expiration=2025-10-06, Reissue=2025-10-06
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000034', '00000002530', TRUE, 364.20, 277.00, 364.20, '2009-05-10', '2025-10-06', '2025-10-06', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000035, Customer ID: 00000001660, Active: Y
    -- Financial data: Balance=194.70, Credit Limit=152.50, Cash Credit Limit=194.70
    -- Dates: Open=2018-02-02, Expiration=2025-09-23, Reissue=2025-09-23
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000035', '00000001660', TRUE, 194.70, 152.50, 194.70, '2018-02-02', '2025-09-23', '2025-09-23', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000036, Customer ID: 00000001100, Active: Y
    -- Financial data: Balance=332.80, Credit Limit=83.90, Cash Credit Limit=332.80
    -- Dates: Open=2018-07-18, Expiration=2024-12-23, Reissue=2024-12-23
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000036', '00000001100', TRUE, 332.80, 83.90, 332.80, '2018-07-18', '2024-12-23', '2024-12-23', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000037, Customer ID: 00000000070, Active: Y
    -- Financial data: Balance=44.60, Credit Limit=16.60, Cash Credit Limit=44.60
    -- Dates: Open=2016-09-10, Expiration=2023-10-24, Reissue=2023-10-24
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000037', '00000000070', TRUE, 44.60, 16.60, 44.60, '2016-09-10', '2023-10-24', '2023-10-24', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000038, Customer ID: 00000006120, Active: Y
    -- Financial data: Balance=650.50, Credit Limit=347.60, Cash Credit Limit=650.50
    -- Dates: Open=2010-08-12, Expiration=2023-07-23, Reissue=2023-07-23
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000038', '00000006120', TRUE, 650.50, 347.60, 650.50, '2010-08-12', '2023-07-23', '2023-07-23', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000039, Customer ID: 00000008430, Active: Y
    -- Financial data: Balance=975.00, Credit Limit=621.20, Cash Credit Limit=975.00
    -- Dates: Open=2018-08-26, Expiration=2025-09-08, Reissue=2025-09-08
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000039', '00000008430', TRUE, 975.00, 621.20, 975.00, '2018-08-26', '2025-09-08', '2025-09-08', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000040, Customer ID: 00000000430, Active: Y
    -- Financial data: Balance=582.30, Credit Limit=167.40, Cash Credit Limit=582.30
    -- Dates: Open=2010-02-13, Expiration=2023-10-27, Reissue=2023-10-27
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000040', '00000000430', TRUE, 582.30, 167.40, 582.30, '2010-02-13', '2023-10-27', '2023-10-27', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000041, Customer ID: 00000003750, Active: Y
    -- Financial data: Balance=672.10, Credit Limit=342.90, Cash Credit Limit=672.10
    -- Dates: Open=2015-02-07, Expiration=2023-04-24, Reissue=2023-04-24
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000041', '00000003750', TRUE, 672.10, 342.90, 672.10, '2015-02-07', '2023-04-24', '2023-04-24', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000042, Customer ID: 00000003020, Active: Y
    -- Financial data: Balance=656.30, Credit Limit=510.30, Cash Credit Limit=656.30
    -- Dates: Open=2016-09-19, Expiration=2025-09-19, Reissue=2025-09-19
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000042', '00000003020', TRUE, 656.30, 510.30, 656.30, '2016-09-19', '2025-09-19', '2025-09-19', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000043, Customer ID: 00000006100, Active: Y
    -- Financial data: Balance=616.80, Credit Limit=120.60, Cash Credit Limit=616.80
    -- Dates: Open=2012-04-09, Expiration=2025-08-29, Reissue=2025-08-29
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000043', '00000006100', TRUE, 616.80, 120.60, 616.80, '2012-04-09', '2025-08-29', '2025-08-29', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000044, Customer ID: 00000002630, Active: Y
    -- Financial data: Balance=689.90, Credit Limit=443.20, Cash Credit Limit=689.90
    -- Dates: Open=2018-12-01, Expiration=2024-01-17, Reissue=2024-01-17
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000044', '00000002630', TRUE, 689.90, 443.20, 689.90, '2018-12-01', '2024-01-17', '2024-01-17', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000045, Customer ID: 00000001860, Active: Y
    -- Financial data: Balance=271.90, Credit Limit=68.80, Cash Credit Limit=271.90
    -- Dates: Open=2010-12-31, Expiration=2025-07-09, Reissue=2025-07-09
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000045', '00000001860', TRUE, 271.90, 68.80, 271.90, '2010-12-31', '2025-07-09', '2025-07-09', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000046, Customer ID: 00000003960, Active: Y
    -- Financial data: Balance=700.70, Credit Limit=543.80, Cash Credit Limit=700.70
    -- Dates: Open=2013-09-06, Expiration=2025-06-20, Reissue=2025-06-20
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000046', '00000003960', TRUE, 700.70, 543.80, 700.70, '2013-09-06', '2025-06-20', '2025-06-20', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000047, Customer ID: 00000000320, Active: Y
    -- Financial data: Balance=233.80, Credit Limit=15.90, Cash Credit Limit=233.80
    -- Dates: Open=2014-04-03, Expiration=2025-08-23, Reissue=2025-08-23
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000047', '00000000320', TRUE, 233.80, 15.90, 233.80, '2014-04-03', '2025-08-23', '2025-08-23', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000048, Customer ID: 00000002260, Active: Y
    -- Financial data: Balance=230.60, Credit Limit=61.20, Cash Credit Limit=230.60
    -- Dates: Open=2017-03-18, Expiration=2025-02-06, Reissue=2025-02-06
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000048', '00000002260', TRUE, 230.60, 61.20, 230.60, '2017-03-18', '2025-02-06', '2025-02-06', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000049, Customer ID: 00000001000, Active: Y
    -- Financial data: Balance=904.80, Credit Limit=480.70, Cash Credit Limit=904.80
    -- Dates: Open=2019-04-06, Expiration=2023-09-17, Reissue=2023-09-17
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000049', '00000001000', TRUE, 904.80, 480.70, 904.80, '2019-04-06', '2023-09-17', '2023-09-17', 0.00, 0.00, 'A000000000'),
    
    -- Account ID: 00000000050, Customer ID: 00000004920, Active: Y
    -- Financial data: Balance=616.90, Credit Limit=458.70, Cash Credit Limit=616.90
    -- Dates: Open=2011-04-22, Expiration=2023-03-09, Reissue=2023-03-09
    -- Cycle data: Credit=0.00, Debit=0.00, Group=A000000000
    ('00000000050', '00000004920', TRUE, 616.90, 458.70, 616.90, '2011-04-22', '2023-03-09', '2023-03-09', 0.00, 0.00, 'A000000000');

-- ============================================================================
-- SECTION 3: DATA VALIDATION AND INTEGRITY CHECKS
-- ============================================================================

-- Validate that all account records were inserted successfully
-- Expected count: 50 accounts loaded from acctdata.txt
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM accounts;
    
    IF record_count != 50 THEN
        RAISE EXCEPTION 'Account data loading failed: Expected 50 records, found %', record_count;
    END IF;
    
    RAISE NOTICE 'Account data loading completed successfully: % records inserted', record_count;
END $$;

-- Validate foreign key relationships with customers table
-- Ensure all customer_id references exist in customers table
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count 
    FROM accounts a
    LEFT JOIN customers c ON a.customer_id = c.customer_id
    WHERE c.customer_id IS NULL;
    
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'Foreign key validation failed: % accounts have invalid customer_id references', orphan_count;
    END IF;
    
    RAISE NOTICE 'Foreign key validation completed: All customer_id references are valid';
END $$;

-- Validate disclosure group relationships
-- Ensure all group_id references exist in disclosure_groups table  
DO $$
DECLARE
    orphan_group_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_group_count 
    FROM accounts a
    LEFT JOIN disclosure_groups dg ON a.group_id = dg.group_id
    WHERE dg.group_id IS NULL;
    
    IF orphan_group_count > 0 THEN
        RAISE EXCEPTION 'Disclosure group validation failed: % accounts have invalid group_id references', orphan_group_count;
    END IF;
    
    RAISE NOTICE 'Disclosure group validation completed: All group_id references are valid';
END $$;

-- Validate financial data precision and ranges
-- Ensure all DECIMAL(12,2) fields maintain proper precision
DO $$
DECLARE
    precision_error_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO precision_error_count 
    FROM accounts 
    WHERE current_balance::text ~ '\.\d{3,}' 
       OR credit_limit::text ~ '\.\d{3,}' 
       OR cash_credit_limit::text ~ '\.\d{3,}' 
       OR current_cycle_credit::text ~ '\.\d{3,}' 
       OR current_cycle_debit::text ~ '\.\d{3,}';
    
    IF precision_error_count > 0 THEN
        RAISE EXCEPTION 'Financial precision validation failed: % accounts have invalid decimal precision', precision_error_count;
    END IF;
    
    RAISE NOTICE 'Financial precision validation completed: All decimal fields maintain DECIMAL(12,2) precision';
END $$;

-- Validate account lifecycle date consistency
-- Ensure expiration and reissue dates are consistent with open dates
DO $$
DECLARE
    date_error_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO date_error_count 
    FROM accounts 
    WHERE expiration_date IS NOT NULL AND expiration_date < open_date
       OR reissue_date IS NOT NULL AND reissue_date < open_date;
    
    IF date_error_count > 0 THEN
        RAISE EXCEPTION 'Date validation failed: % accounts have invalid date relationships', date_error_count;
    END IF;
    
    RAISE NOTICE 'Date validation completed: All lifecycle dates are consistent';
END $$;

-- Validate credit limit relationships
-- Ensure cash credit limits do not exceed general credit limits
DO $$
DECLARE
    credit_limit_error_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO credit_limit_error_count 
    FROM accounts 
    WHERE cash_credit_limit > credit_limit;
    
    IF credit_limit_error_count > 0 THEN
        RAISE EXCEPTION 'Credit limit validation failed: % accounts have cash credit limits exceeding general credit limits', credit_limit_error_count;
    END IF;
    
    RAISE NOTICE 'Credit limit validation completed: All credit limit relationships are valid';
END $$;

-- ============================================================================
-- SECTION 4: PERFORMANCE OPTIMIZATION AND INDEXING
-- ============================================================================

-- Analyze table statistics for query optimization
-- Update table statistics after bulk data loading
ANALYZE accounts;

-- Vacuum table to optimize storage and performance
VACUUM accounts;

-- ============================================================================
-- SECTION 5: MIGRATION COMPLETION AND VERIFICATION
-- ============================================================================

-- Display summary statistics for verification
SELECT 
    'Account Data Loading Summary' AS summary_type,
    COUNT(*) AS total_records,
    COUNT(DISTINCT customer_id) AS unique_customers,
    COUNT(DISTINCT group_id) AS unique_disclosure_groups,
    SUM(current_balance) AS total_current_balance,
    SUM(credit_limit) AS total_credit_limit,
    SUM(cash_credit_limit) AS total_cash_credit_limit,
    MIN(open_date) AS earliest_open_date,
    MAX(open_date) AS latest_open_date,
    COUNT(CASE WHEN active_status = TRUE THEN 1 END) AS active_accounts,
    COUNT(CASE WHEN active_status = FALSE THEN 1 END) AS inactive_accounts
FROM accounts;

-- Migration completion notice
SELECT 
    'V22__load_accounts_data.sql' AS migration_file,
    'COMPLETED' AS status,
    '50 account records loaded from acctdata.txt' AS details,
    'DECIMAL(12,2) precision maintained for all financial fields' AS precision_status,
    'Foreign key relationships validated successfully' AS integrity_status,
    CURRENT_TIMESTAMP AS completion_time;

-- ============================================================================
-- ROLLBACK INSTRUCTIONS
-- ============================================================================

-- To rollback this migration, execute the following commands:
-- DELETE FROM accounts WHERE account_id IN (SELECT account_id FROM accounts);
-- This will remove all account data loaded by this migration
-- Note: Ensure proper backup procedures are in place before executing rollback

-- ============================================================================
-- MAINTENANCE NOTES
-- ============================================================================

-- 1. Regular monitoring of account data integrity
-- 2. Periodic validation of foreign key relationships
-- 3. Monthly analysis of financial data precision
-- 4. Quarterly review of account lifecycle dates
-- 5. Annual audit of credit limit relationships
-- 6. Database performance monitoring for account queries
-- 7. Backup verification of account data
-- 8. Security audit of account access patterns

-- ============================================================================
-- END OF MIGRATION: V22__load_accounts_data.sql
-- ============================================================================