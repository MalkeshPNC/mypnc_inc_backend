ALTER TABLE tblapp_configurations
    ADD COLUMN value_type VARCHAR(16) NOT NULL DEFAULT 'text' AFTER use_editor;

UPDATE tblapp_configurations
   SET value_type = 'image'
 WHERE config_key IN ('quote.pdf.headerImage', 'quote.pdf.signatureImage');

-- V24 seeded paths that never existed in the built frontend; only correct rows still holding them.
UPDATE tblapp_configurations
   SET config_value = '/assets/images/quote-pdf/header_svg.svg'
 WHERE config_key = 'quote.pdf.headerImage'
   AND config_value = '/quote-pdf/header.svg';

UPDATE tblapp_configurations
   SET config_value = '/assets/images/quote-pdf/signature_svg.svg'
 WHERE config_key = 'quote.pdf.signatureImage'
   AND config_value = '/quote-pdf/signature.svg';
