CREATE TABLE tblsub_config_types (
    type_id BIGINT NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(80) NOT NULL,
    type_name VARCHAR(120) NOT NULL,
    field_1_label VARCHAR(80) NULL,
    field_2_label VARCHAR(80) NULL,
    field_3_label VARCHAR(80) NULL,
    field_4_label VARCHAR(80) NULL,
    field_5_label VARCHAR(80) NULL,
    field_6_label VARCHAR(80) NULL,
    field_7_label VARCHAR(80) NULL,
    field_8_label VARCHAR(80) NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (type_id),
    UNIQUE KEY uk_sub_config_type_code (type_code)
);

CREATE TABLE tblsub_config_entries (
    entry_id BIGINT NOT NULL AUTO_INCREMENT,
    type_id BIGINT NOT NULL,
    field_1 VARCHAR(255) NULL,
    field_2 VARCHAR(255) NULL,
    field_3 VARCHAR(255) NULL,
    field_4 VARCHAR(255) NULL,
    field_5 VARCHAR(255) NULL,
    field_6 VARCHAR(255) NULL,
    field_7 VARCHAR(255) NULL,
    field_8 VARCHAR(255) NULL,
    created_by VARCHAR(200) NOT NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (entry_id),
    KEY idx_sub_config_entry_type (type_id),
    CONSTRAINT fk_sub_config_entry_type FOREIGN KEY (type_id) REFERENCES tblsub_config_types (type_id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_config_entry_user FOREIGN KEY (created_by_user_id) REFERENCES tblusers (user_id) ON DELETE SET NULL
);
