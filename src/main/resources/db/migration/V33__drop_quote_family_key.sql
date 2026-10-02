-- Compare and revision numbering group by NC#, so the copy-chain key is unused.
ALTER TABLE tblquotes
    DROP INDEX idx_quotes_family_key,
    DROP COLUMN family_key;
