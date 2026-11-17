ALTER TABLE tbldocuments
    ADD COLUMN customer_id BIGINT NULL AFTER document_id,
    ADD COLUMN category VARCHAR(80) NOT NULL DEFAULT 'Other' AFTER customer_id;

UPDATE tbldocuments
SET customer_id = CASE
        WHEN LOWER(owner_type) IN ('customer', 'customers') AND owner_id REGEXP '^[0-9]+$'
            THEN CAST(owner_id AS UNSIGNED)
        ELSE NULL
    END,
    category = CASE
        WHEN owner_type IS NULL OR TRIM(owner_type) = '' THEN 'Other'
        WHEN LOWER(owner_type) IN ('customer', 'customers') THEN 'Other'
        ELSE LEFT(TRIM(owner_type), 80)
    END;

ALTER TABLE tbldocuments
    DROP INDEX idx_documents_owner_active;

ALTER TABLE tbldocuments
    DROP COLUMN owner_type,
    DROP COLUMN owner_id;

ALTER TABLE tbldocuments
    MODIFY category VARCHAR(80) NOT NULL;

ALTER TABLE tbldocuments
    ADD KEY idx_documents_customer_category (customer_id, category, deleted_at);
