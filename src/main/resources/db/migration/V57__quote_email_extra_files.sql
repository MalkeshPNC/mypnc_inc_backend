CREATE TABLE tblquote_email_files (
    file_id BIGINT NOT NULL AUTO_INCREMENT,
    outbox_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    content LONGBLOB NULL,
    PRIMARY KEY (file_id),
    KEY idx_quote_email_files_outbox (outbox_id)
);
