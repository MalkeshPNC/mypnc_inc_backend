ALTER TABLE tblsub_config_types
    ADD COLUMN module_code VARCHAR(50) NULL;

UPDATE tblsub_config_types
SET module_code = 'QUOTE'
WHERE LOWER(type_code) IN (
    'quotetypes',
    'quotestatus',
    'quoteprogressstatus',
    'quoteservicelist',
    'pcblabortype',
    'assemblylabortype'
);

UPDATE tblsub_config_types
SET module_code = 'VENDORS'
WHERE LOWER(type_code) IN (
    'vendortype',
    'vendorsubtype',
    'manufacturertype',
    'manufacturersubtype'
);
