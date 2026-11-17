INSERT INTO tblmodules (module_code, module_name) VALUES
    ('DEVELOPMENT_TRACK', 'Development Track');

INSERT INTO tblrole_module_access (role_id, module_id, access_level)
SELECT r.role_id, m.module_id, 'WRITE'
FROM tblroles r
JOIN tblmodules m ON m.module_code = 'DEVELOPMENT_TRACK'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');
