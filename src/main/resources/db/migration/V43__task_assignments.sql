ALTER TABLE tbltasks
    ADD COLUMN task_type VARCHAR(120) NULL,
    ADD COLUMN priority VARCHAR(20) NULL,
    ADD COLUMN job_number VARCHAR(80) NULL,
    ADD COLUMN nc_number VARCHAR(80) NULL;

CREATE TABLE tbltask_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NULL,
    comment TEXT NULL,
    assigned_by BIGINT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE tbltask_groups (
    task_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, group_id)
);

INSERT INTO tbltask_assignees (task_id, user_id, status, assigned_by, updated_at)
SELECT task_id, assigned_to, status, assigned_by, updated_at
FROM tbltasks
WHERE assigned_to IS NOT NULL;

ALTER TABLE tbltasks
    DROP COLUMN assigned_to,
    DROP COLUMN assigned_by;
