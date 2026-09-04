-- V1 used CREATE TABLE IF NOT EXISTS, so tblcustomers kept its pre-Flyway definition
-- where cust_entry_dt stayed nullable. Backfill the gaps and enforce the entity contract.
UPDATE tblcustomers
SET cust_entry_dt = CURRENT_TIMESTAMP
WHERE cust_entry_dt IS NULL;

ALTER TABLE tblcustomers
    MODIFY cust_entry_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
