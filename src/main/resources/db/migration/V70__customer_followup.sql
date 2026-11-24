ALTER TABLE tblcustomers
    ADD COLUMN followup_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_reminder DATETIME NULL,
    ADD COLUMN followup_by VARCHAR(200) NULL;
