-- V32__add_transaction_type_to_category_balances.sql
-- Migration to add missing transaction_type column to transaction_category_balances table
-- This resolves JPA entity schema mismatch where entity expects 3-part composite key but table has 2-part key

-- Step 1: Add transaction_type column (allow NULL initially for data population)
ALTER TABLE transaction_category_balances 
ADD COLUMN transaction_type VARCHAR(2);

-- Step 2: Set default transaction_type based on business logic
-- For existing records, use '01' (Purchase) as default transaction type
-- This maintains data integrity while allowing proper entity mapping
UPDATE transaction_category_balances 
SET transaction_type = '01' 
WHERE transaction_type IS NULL;

-- Step 3: Make transaction_type NOT NULL after data population
ALTER TABLE transaction_category_balances 
ALTER COLUMN transaction_type SET NOT NULL;

-- Step 4: Drop existing primary key constraint
ALTER TABLE transaction_category_balances 
DROP CONSTRAINT pk_transaction_category_balances;

-- Step 5: Create new composite primary key with transaction_type included
ALTER TABLE transaction_category_balances 
ADD CONSTRAINT pk_transaction_category_balances 
PRIMARY KEY (account_id, transaction_type, transaction_category);

-- Step 6: Add foreign key constraint to transaction_types table
ALTER TABLE transaction_category_balances 
ADD CONSTRAINT fk_tcatbal_transaction_type 
FOREIGN KEY (transaction_type) REFERENCES transaction_types(transaction_type) 
ON UPDATE CASCADE ON DELETE RESTRICT;

-- Step 7: Add index for transaction_type for performance
CREATE INDEX idx_tcatbal_transaction_type ON transaction_category_balances(transaction_type);

-- Step 8: Update existing indexes to include transaction_type where appropriate
-- Recreate composite index for better query performance
DROP INDEX IF EXISTS idx_tcatbal_balance_lookup;
CREATE INDEX idx_tcatbal_balance_lookup ON transaction_category_balances 
USING btree (account_id, transaction_type, transaction_category) 
INCLUDE (category_balance, last_updated, version_number);

-- Step 9: Add validation constraint for transaction_type format
ALTER TABLE transaction_category_balances 
ADD CONSTRAINT chk_tcatbal_transaction_type_format 
CHECK (transaction_type ~ '^[0-9]{2}$');

-- Step 10: Update any existing views that depend on this table
-- Recreate the view to include transaction_type in the composite key structure
DROP VIEW IF EXISTS v_card_account_customer_xref;
CREATE VIEW v_card_account_customer_xref AS
SELECT 
    c.card_number,
    c.account_id,
    c.customer_id,
    a.account_status,
    a.account_type,
    a.current_balance,
    a.available_credit,
    cust.first_name,
    cust.last_name,
    cust.fico_credit_score,
    c.card_status,
    c.expiry_date
FROM cards c
INNER JOIN accounts a ON c.account_id = a.account_id
INNER JOIN customers cust ON c.customer_id = cust.customer_id;

COMMENT ON COLUMN transaction_category_balances.transaction_type IS 'Transaction type code (2 characters) - part of composite primary key with account_id and transaction_category';