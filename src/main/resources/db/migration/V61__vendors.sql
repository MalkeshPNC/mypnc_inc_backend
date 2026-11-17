INSERT INTO tblmodules (module_code, module_name) VALUES
    ('VENDORS', 'Vendors & Manufacturers');

INSERT INTO tblrole_module_access (role_id, module_id, access_level)
SELECT r.role_id, m.module_id, 'WRITE'
FROM tblroles r
JOIN tblmodules m ON m.module_code = 'VENDORS'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN');

CREATE TABLE tblvendors (
    vendor_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type_entry_id BIGINT NOT NULL,
    subtype_entry_id BIGINT NOT NULL,
    contact_name VARCHAR(200) NULL,
    contact_address VARCHAR(500) NULL,
    contact_email VARCHAR(320) NULL,
    contact_phone VARCHAR(40) NULL,
    created_by BIGINT NOT NULL,
    created_date DATETIME NOT NULL,
    updated_by BIGINT NOT NULL,
    updated_date DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (vendor_id),
    UNIQUE KEY uk_vendors_code (code),
    KEY idx_vendors_type_entry (type_entry_id),
    KEY idx_vendors_subtype_entry (subtype_entry_id),
    CONSTRAINT fk_vendors_type_entry FOREIGN KEY (type_entry_id) REFERENCES tblsub_config_entries (entry_id),
    CONSTRAINT fk_vendors_subtype_entry FOREIGN KEY (subtype_entry_id) REFERENCES tblsub_config_entries (entry_id)
);

CREATE TABLE tblmanufacturers (
    manufacturer_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type_entry_id BIGINT NOT NULL,
    subtype_entry_id BIGINT NULL,
    contact_name VARCHAR(200) NULL,
    contact_address VARCHAR(500) NULL,
    contact_email VARCHAR(320) NULL,
    contact_phone VARCHAR(40) NULL,
    created_by BIGINT NOT NULL,
    created_date DATETIME NOT NULL,
    updated_by BIGINT NOT NULL,
    updated_date DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (manufacturer_id),
    UNIQUE KEY uk_manufacturers_code (code),
    KEY idx_manufacturers_type_entry (type_entry_id),
    KEY idx_manufacturers_subtype_entry (subtype_entry_id),
    CONSTRAINT fk_manufacturers_type_entry FOREIGN KEY (type_entry_id) REFERENCES tblsub_config_entries (entry_id),
    CONSTRAINT fk_manufacturers_subtype_entry FOREIGN KEY (subtype_entry_id) REFERENCES tblsub_config_entries (entry_id)
);
