ALTER TABLE tblquote_email_outbox
    ADD COLUMN quote_number VARCHAR(80) NULL,
    ADD COLUMN from_address VARCHAR(200) NULL;

UPDATE tblquote_email_outbox
SET quote_number = ''
WHERE quote_number IS NULL;

UPDATE tblquote_email_outbox
SET from_address = ''
WHERE from_address IS NULL;

ALTER TABLE tblquote_email_outbox
    MODIFY quote_number VARCHAR(80) NOT NULL,
    MODIFY from_address VARCHAR(200) NOT NULL;

CREATE INDEX idx_quote_email_outbox_user_created ON tblquote_email_outbox (user_id, created_at);
CREATE INDEX idx_quote_email_outbox_created ON tblquote_email_outbox (created_at);
