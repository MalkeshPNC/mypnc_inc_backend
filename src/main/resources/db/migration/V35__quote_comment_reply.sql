ALTER TABLE tblquote_comments
    ADD COLUMN reply_to_comment_id BIGINT NULL AFTER body;
