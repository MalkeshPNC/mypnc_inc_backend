-- Legacy company logos live in this column. The app only reads it for preview
-- when company_logo is empty, so creating it is safe when a database already has it.
SET @company_blob_logo_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tblcustomers'
      AND COLUMN_NAME = 'company_blob_logo'
);
SET @company_blob_logo_sql = IF(
    @company_blob_logo_exists = 0,
    'ALTER TABLE tblcustomers ADD COLUMN company_blob_logo LONGBLOB NULL AFTER company_logo',
    'SELECT 1'
);
PREPARE company_blob_logo_stmt FROM @company_blob_logo_sql;
EXECUTE company_blob_logo_stmt;
DEALLOCATE PREPARE company_blob_logo_stmt;
