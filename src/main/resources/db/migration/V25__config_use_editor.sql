ALTER TABLE tblapp_configurations
    ADD COLUMN use_editor TINYINT(1) NOT NULL DEFAULT 0 AFTER description;

UPDATE tblapp_configurations
SET use_editor = 1
WHERE config_key = 'quote.pdf.termsText';
