ALTER TABLE tbltasks
    ADD COLUMN reviewer_id BIGINT NULL,
    ADD COLUMN reviewer_comment TEXT NULL,
    ADD COLUMN reviewer_comment_at DATETIME NULL;
