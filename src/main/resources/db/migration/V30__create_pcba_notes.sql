CREATE TABLE tblpcba_notes (
    pcba_note_id BIGINT NOT NULL AUTO_INCREMENT,
    pcba_part_number VARCHAR(120) NOT NULL,
    pcba_notes TEXT NULL,
    PRIMARY KEY (pcba_note_id),
    UNIQUE KEY uk_pcba_part_number (pcba_part_number)
);

INSERT INTO tblpcba_notes (pcba_part_number, pcba_notes)
SELECT nc.pcba_part_number, nc.pcba_alert
FROM tblnc_masters nc
INNER JOIN (
    SELECT pcba_part_number, MAX(nc_id) AS nc_id
    FROM tblnc_masters
    WHERE pcba_part_number IS NOT NULL
      AND CHAR_LENGTH(TRIM(pcba_part_number)) > 0
      AND pcba_alert IS NOT NULL
      AND CHAR_LENGTH(TRIM(pcba_alert)) > 0
    GROUP BY pcba_part_number
) latest ON latest.nc_id = nc.nc_id;

ALTER TABLE tblnc_masters DROP COLUMN pcba_alert;
