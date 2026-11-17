CREATE TABLE tblquote_attachments (
    attachment_id BIGINT NOT NULL AUTO_INCREMENT,
    qid BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    is_generated TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    created_by_user_id BIGINT NULL,
    PRIMARY KEY (attachment_id),
    UNIQUE KEY uk_quote_attachment_name (qid, original_filename),
    UNIQUE KEY uk_quote_attachment_key (storage_key),
    KEY idx_quote_attachments_qid (qid)
);
