ALTER TABLE tblnotifications
    ADD COLUMN deleted_at DATETIME NULL AFTER created_at;
