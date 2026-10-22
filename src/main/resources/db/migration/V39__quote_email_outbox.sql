CREATE TABLE tblquote_email_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    recipients VARCHAR(3000) NOT NULL,
    cc_recipients VARCHAR(3000) NULL,
    subject VARCHAR(200) NOT NULL,
    html_body LONGTEXT NOT NULL,
    attachment_name VARCHAR(200) NOT NULL,
    pdf_content LONGBLOB NULL,
    status VARCHAR(16) NOT NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    INDEX idx_quote_email_outbox_status_id (status, id)
);
