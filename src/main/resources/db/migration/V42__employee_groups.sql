CREATE TABLE tblemployee_groups (
    g_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    g_name VARCHAR(200) NOT NULL,
    g_type VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    is_delete TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_employee_groups_active (is_delete, created_at, g_id)
);

CREATE TABLE tblemployee_group_users (
    g_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (g_id, user_id),
    INDEX idx_employee_group_users_user (user_id)
);
