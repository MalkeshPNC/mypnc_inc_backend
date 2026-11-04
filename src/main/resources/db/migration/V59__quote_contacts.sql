CREATE TABLE tblquote_contacts (
    quote_id BIGINT NOT NULL,
    -- tblcontacts.cont_id is BIGINT UNSIGNED, so this column has to match.
    cont_id BIGINT UNSIGNED NOT NULL,
    is_primary TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (quote_id, cont_id),
    CONSTRAINT fk_quote_contacts_quote FOREIGN KEY (quote_id) REFERENCES tblquotes (qid) ON DELETE CASCADE,
    CONSTRAINT fk_quote_contacts_contact FOREIGN KEY (cont_id) REFERENCES tblcontacts (cont_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tblquote_contacts (quote_id, cont_id, is_primary)
SELECT qid, cont_id, 1
FROM tblquotes
WHERE cont_id IS NOT NULL;
