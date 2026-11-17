CREATE TABLE tbltask_reviewers (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment TEXT NULL,
    comment_at DATETIME NULL,
    PRIMARY KEY (task_id, user_id)
);

INSERT INTO tbltask_reviewers (task_id, user_id, comment, comment_at)
SELECT task_id, reviewer_id, reviewer_comment, reviewer_comment_at
FROM tbltasks
WHERE reviewer_id IS NOT NULL;

ALTER TABLE tbltasks
    DROP COLUMN reviewer_id,
    DROP COLUMN reviewer_comment,
    DROP COLUMN reviewer_comment_at;

INSERT INTO tblroles (role_code, role_name, is_system)
SELECT 'QUOTE_REVIEWER', 'Quote Reviewer', 1
WHERE NOT EXISTS (SELECT 1 FROM tblroles WHERE role_code = 'QUOTE_REVIEWER');
