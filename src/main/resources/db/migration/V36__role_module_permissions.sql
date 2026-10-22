CREATE TABLE tblmodules (
    module_id BIGINT NOT NULL AUTO_INCREMENT,
    module_code VARCHAR(50) NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (module_id),
    UNIQUE KEY uk_modules_code (module_code)
);

CREATE TABLE tblrole_module_access (
    role_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    access_level VARCHAR(10) NOT NULL,
    PRIMARY KEY (role_id, module_id),
    CONSTRAINT fk_role_module_access_role FOREIGN KEY (role_id) REFERENCES tblroles (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_module_access_module FOREIGN KEY (module_id) REFERENCES tblmodules (module_id) ON DELETE CASCADE
);

INSERT INTO tblroles (role_code, role_name, is_system) VALUES
    ('SUPER_ADMIN', 'Super Admin', 1);

INSERT INTO tblmodules (module_code, module_name) VALUES
    ('QUOTE', 'Quote'),
    ('CUSTOMERS', 'Customers'),
    ('REPORTS', 'Reports'),
    ('CONFIGURATION', 'Configuration'),
    ('SECURITY', 'Security');

INSERT INTO tblrole_module_access (role_id, module_id, access_level)
SELECT r.role_id, m.module_id, 'WRITE'
FROM tblroles r
CROSS JOIN tblmodules m
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');

INSERT INTO tbluser_roles (user_id, role_id)
SELECT ur.user_id, sa.role_id
FROM tbluser_roles ur
JOIN tblroles admin ON admin.role_id = ur.role_id AND admin.role_code = 'ADMIN'
JOIN tblroles sa ON sa.role_code = 'SUPER_ADMIN';
