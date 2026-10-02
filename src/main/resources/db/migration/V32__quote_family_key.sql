-- Copy chain id. A later-year copy keeps this key so compare still groups it
-- with the original, even though its quote number uses the new year.
ALTER TABLE tblquotes
    ADD COLUMN family_key VARCHAR(80) NULL AFTER quote_number;

UPDATE tblquotes
SET family_key = quote_number;

-- Strip one trailing .digits revision, matching QuoteService.quoteFamily.
UPDATE tblquotes
SET family_key = LEFT(
        quote_number,
        CHAR_LENGTH(quote_number) - CHAR_LENGTH(SUBSTRING_INDEX(quote_number, '.', -1)) - 1
    )
WHERE quote_number REGEXP '[.][0-9]+$';

ALTER TABLE tblquotes
    MODIFY family_key VARCHAR(80) NOT NULL;

CREATE INDEX idx_quotes_family_key ON tblquotes (family_key);
