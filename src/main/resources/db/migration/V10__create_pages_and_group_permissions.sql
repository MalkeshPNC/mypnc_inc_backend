CREATE TABLE tblpages (
    page_code VARCHAR(80) NOT NULL,
    page_name VARCHAR(100) NOT NULL,
    module VARCHAR(80) NOT NULL,
    PRIMARY KEY (page_code)
);

CREATE TABLE tblgroup_permissions (
    permission_id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    page_code VARCHAR(80) NOT NULL,
    access VARCHAR(10) NOT NULL,
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_group_page (role_id, page_code),
    CONSTRAINT fk_gp_role FOREIGN KEY (role_id) REFERENCES tblroles (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_gp_page FOREIGN KEY (page_code) REFERENCES tblpages (page_code),
    CONSTRAINT chk_gp_access CHECK (access IN ('READ', 'WRITE'))
);

INSERT INTO tblpages (page_code, page_name, module) VALUES
    ('quote.dashboard', 'Dashboard', 'Quote System'),
    ('quote.customers', 'Customers', 'Quote System'),
    ('quote.salespersons', 'Salespersons', 'Quote System'),
    ('security.groups', 'Groups', 'Security'),
    ('security.users', 'Users', 'Security'),
    ('system.configuration', 'Configuration', 'System');

INSERT INTO tblgroup_permissions (role_id, page_code, access)
SELECT r.role_id, p.page_code, 'WRITE'
FROM tblroles r
CROSS JOIN tblpages p
WHERE r.role_code = 'ADMIN';

INSERT INTO tblgroup_permissions (role_id, page_code, access)
SELECT r.role_id, p.page_code, 'WRITE'
FROM tblroles r
CROSS JOIN tblpages p
WHERE r.role_code = 'USER'
  AND p.page_code IN ('quote.dashboard', 'quote.customers', 'quote.salespersons');
