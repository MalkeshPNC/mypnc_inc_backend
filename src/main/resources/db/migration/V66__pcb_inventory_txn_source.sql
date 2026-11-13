UPDATE tblpcb_inventory_txn
SET source_name = NULLIF(TRIM(details), '');

ALTER TABLE tblpcb_inventory_txn
    MODIFY COLUMN source_name TEXT NULL,
    DROP COLUMN details,
    DROP COLUMN initial_name;
