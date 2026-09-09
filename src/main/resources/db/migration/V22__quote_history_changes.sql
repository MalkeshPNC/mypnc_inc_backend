ALTER TABLE tblquote_history
    ADD COLUMN change_summary VARCHAR(120) NULL AFTER action;
