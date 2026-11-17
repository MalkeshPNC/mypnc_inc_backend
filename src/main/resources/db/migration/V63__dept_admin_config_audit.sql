INSERT INTO tblmodules (module_code, module_name) VALUES
    ('CONFIG_SETTINGS', 'App Settings'),
    ('CONFIG_FAI', 'FAI Settings'),
    ('CONFIG_MASTERS', 'Masters Data');

INSERT INTO tblrole_module_access (role_id, module_id, access_level)
SELECT src.role_id, page.module_id, src.access_level
FROM tblrole_module_access src
JOIN tblroles r ON r.role_id = src.role_id
JOIN tblmodules config ON config.module_id = src.module_id AND config.module_code = 'CONFIGURATION'
JOIN tblmodules page ON page.module_code IN ('CONFIG_SETTINGS', 'CONFIG_FAI', 'CONFIG_MASTERS')
WHERE r.role_code <> 'DEPT_ADMIN'
  AND src.access_level <> 'NONE';

INSERT INTO tblroles (role_code, role_name, is_system)
SELECT 'DEPT_ADMIN', 'Dept. Admin', 0
WHERE NOT EXISTS (SELECT 1 FROM tblroles WHERE role_code = 'DEPT_ADMIN');

INSERT INTO tblrole_module_access (role_id, module_id, access_level)
SELECT r.role_id, m.module_id, 'WRITE'
FROM tblroles r
JOIN tblmodules m ON m.module_code IN ('CONFIG_FAI', 'CONFIG_MASTERS')
WHERE r.role_code = 'DEPT_ADMIN'
ON DUPLICATE KEY UPDATE access_level = 'WRITE';

DELETE access
FROM tblrole_module_access access
JOIN tblroles r ON r.role_id = access.role_id AND r.role_code = 'DEPT_ADMIN'
JOIN tblmodules m ON m.module_id = access.module_id AND m.module_code IN ('CONFIG_SETTINGS', 'CONFIGURATION');

ALTER TABLE tblsub_config_types
    ADD COLUMN updated_by VARCHAR(200) NULL,
    ADD COLUMN updated_by_user_id BIGINT NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD CONSTRAINT fk_sub_config_type_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES tblusers (user_id) ON DELETE SET NULL;

ALTER TABLE tblsub_config_entries
    ADD COLUMN updated_by VARCHAR(200) NULL,
    ADD COLUMN updated_by_user_id BIGINT NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD CONSTRAINT fk_sub_config_entry_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES tblusers (user_id) ON DELETE SET NULL;

ALTER TABLE tblapp_configurations
    ADD COLUMN updated_by VARCHAR(200) NULL,
    ADD COLUMN updated_by_user_id BIGINT NULL,
    ADD CONSTRAINT fk_app_config_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES tblusers (user_id) ON DELETE SET NULL;
