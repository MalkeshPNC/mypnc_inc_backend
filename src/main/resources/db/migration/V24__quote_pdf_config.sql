ALTER TABLE tblapp_configurations
    MODIFY config_value TEXT NOT NULL;

INSERT INTO tblapp_configurations (config_key, config_value, description, updated_at)
VALUES
    (
        'quote.pdf.headerImage',
        '/quote-pdf/header.svg',
        'Quote PDF header image URL',
        CURRENT_TIMESTAMP
    ),
    (
        'quote.pdf.signatureImage',
        '/quote-pdf/signature.svg',
        'Quote PDF signature image URL',
        CURRENT_TIMESTAMP
    ),
    (
        'quote.pdf.termsText',
        'Terms and conditions placeholder. Replace this text in Configuration.',
        'Quote PDF terms block',
        CURRENT_TIMESTAMP
    );
