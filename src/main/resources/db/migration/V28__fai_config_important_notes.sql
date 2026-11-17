ALTER TABLE tblquote_fai_config
    ADD COLUMN important_notes TEXT NULL AFTER heading_notes;

UPDATE tblquote_fai_config
SET important_notes = 'Important Notes : Quoated with ENIG surface finish.'
WHERE fai_id = 1;
