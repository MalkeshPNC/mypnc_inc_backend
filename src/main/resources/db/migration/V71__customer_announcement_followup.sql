ALTER TABLE tblcustomers
    ADD COLUMN announcement_followup_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_announcement DATETIME NULL;

CREATE TABLE tblreminder_batch (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind VARCHAR(20) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    html_body LONGTEXT NOT NULL,
    cc_recipients VARCHAR(3000) NULL,
    status VARCHAR(16) NOT NULL,
    started_by_user_id BIGINT NOT NULL,
    started_by_name VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_reminder_batch_kind (kind, id)
);

CREATE TABLE tblreminder_recipient (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    contact_id BIGINT NULL,
    customer_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(200) NULL,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    INDEX idx_reminder_recipient_status (status, id),
    INDEX idx_reminder_recipient_batch (batch_id, id)
);

CREATE TABLE tblreminder_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    content LONGBLOB NOT NULL,
    INDEX idx_reminder_attachment_batch (batch_id)
);
