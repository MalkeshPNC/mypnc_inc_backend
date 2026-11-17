-- tblcontacts also predates Flyway, so V2's CREATE TABLE IF NOT EXISTS left the
-- original column widths in place. Bring them in line with the V2 definition so a
-- database built from migrations matches an existing one.
ALTER TABLE tblcontacts
    MODIFY first_name VARCHAR(120) NULL,
    MODIFY last_name VARCHAR(120) NULL,
    MODIFY phone VARCHAR(40) NULL,
    MODIFY email VARCHAR(320) NULL;
