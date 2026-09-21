CREATE TABLE tblquote_fai_config (
    fai_id BIGINT NOT NULL,
    heading_notes TEXT NULL,
    pnc_fai_pcb DECIMAL(12,2) NOT NULL,
    pnc_fai_pcba DECIMAL(12,2) NOT NULL,
    as9102_fai_pcb DECIMAL(12,2) NOT NULL,
    as9102_fai_pcba DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    updated_at DATETIME NULL,
    updated_by VARCHAR(200) NULL,
    PRIMARY KEY (fai_id)
);

INSERT INTO tblquote_fai_config (
    fai_id,
    heading_notes,
    pnc_fai_pcb,
    pnc_fai_pcba,
    as9102_fai_pcb,
    as9102_fai_pcba,
    created_at,
    created_by
) VALUES (
    1,
    'Optional Reports/Paperwork: Add pricing(if needed per Drawing/Quality Clauses). Unpriced PO will be considered as waived.',
    300.00,
    210.00,
    450.00,
    315.00,
    CURRENT_TIMESTAMP,
    'system'
);
