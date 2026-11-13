ALTER TABLE tblpcb_inventory_txn
    ADD COLUMN vendor_name VARCHAR(200) NULL AFTER vendor_id;
