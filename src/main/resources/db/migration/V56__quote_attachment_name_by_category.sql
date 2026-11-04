ALTER TABLE tblquote_attachments
    DROP INDEX uk_quote_attachment_name;

ALTER TABLE tblquote_attachments
    ADD UNIQUE KEY uk_quote_attachment_name (qid, category, original_filename);
