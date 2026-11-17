ALTER TABLE tbltask_assignees
    ADD COLUMN comment_at DATETIME NULL;

UPDATE tbltask_assignees
SET comment_at = updated_at
WHERE comment IS NOT NULL AND TRIM(comment) <> '';

CREATE TABLE tbltask_logs (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    field_name VARCHAR(40) NOT NULL,
    old_value VARCHAR(500) NULL,
    new_value VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (log_id),
    KEY idx_task_logs_task (task_id, log_id)
);
