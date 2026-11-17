CREATE TABLE tblnc_masters (
    nc_id BIGINT NOT NULL AUTO_INCREMENT,
    nc_number VARCHAR(80) NOT NULL,
    pcb_part_number VARCHAR(120) NULL,
    pcb_rev VARCHAR(40) NULL,
    pcba_part_number VARCHAR(120) NULL,
    pcba_rev VARCHAR(40) NULL,
    pcba_alert TEXT NULL,
    notes TEXT NULL,
    nc_alert TEXT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (nc_id),
    UNIQUE KEY uk_nc_number (nc_number),
    CONSTRAINT fk_nc_created_by FOREIGN KEY (created_by_user_id) REFERENCES tblusers (user_id) ON DELETE SET NULL
);
