ALTER TABLE tbltasks
    ADD COLUMN qid BIGINT NULL,
    ADD INDEX idx_tasks_qid (qid);
