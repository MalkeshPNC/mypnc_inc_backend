INSERT INTO tblmodules (module_code, module_name) VALUES
    ('TASKS', 'Tasks');

INSERT INTO tblrole_module_access (role_id, module_id, access_level)
SELECT r.role_id, m.module_id, 'WRITE'
FROM tblroles r
JOIN tblmodules m ON m.module_code = 'TASKS'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');

CREATE TABLE tbltasks (
    task_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    assigned_to BIGINT NULL,
    assigned_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_tasks_status_updated (status, updated_at),
    INDEX idx_tasks_updated (updated_at, task_id)
);

CREATE TABLE tbltask_files (
    file_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_task_files_task (task_id)
);
