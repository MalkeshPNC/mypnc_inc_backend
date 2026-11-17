ALTER TABLE tblquote_quantities
    ADD COLUMN commission_percentage DECIMAL(6, 2) NULL;

UPDATE tblquote_quantities q
    INNER JOIN tblquotes t ON t.qid = q.qid
    SET q.commission_percentage = t.commission_percentage;
