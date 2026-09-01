CREATE TABLE IF NOT EXISTS tbldocuments (
    document_id VARCHAR(36) NOT NULL,
    owner_type VARCHAR(100) NOT NULL,
    owner_id VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (document_id),
    UNIQUE KEY uk_documents_storage_key (storage_key),
    KEY idx_documents_owner_active (owner_type, owner_id, deleted_at)
);