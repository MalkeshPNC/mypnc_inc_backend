ALTER TABLE tblquotes
    ADD COLUMN fai_req_pnc TINYINT(1) NOT NULL DEFAULT 0 AFTER pcb_origin,
    ADD COLUMN fai_pcb_wo DECIMAL(12,2) NULL AFTER fai_req_pnc,
    ADD COLUMN fai_pcba_wo DECIMAL(12,2) NULL AFTER fai_pcb_wo,
    ADD COLUMN fai_heading_pnc VARCHAR(1000) NULL AFTER fai_pcba_wo,
    ADD COLUMN fai_req_as9102 TINYINT(1) NOT NULL DEFAULT 0 AFTER fai_heading_pnc,
    ADD COLUMN fai_pcb_with DECIMAL(12,2) NULL AFTER fai_req_as9102,
    ADD COLUMN fai_pcba_with DECIMAL(12,2) NULL AFTER fai_pcb_with,
    ADD COLUMN fai_heading_as9102 VARCHAR(1000) NULL AFTER fai_pcba_with;
