package com.pnc.masters.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbldocuments")
public class Document {

    @Id
    @Column(name = "document_id", length = 36)
    private String documentId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "storage_provider", nullable = false, length = 50)
    private String storageProvider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Document() {
    }

    public Document(String documentId, Long customerId, String category, String originalFilename,
                    String contentType, long fileSize, String checksumSha256, String storageKey,
                    String storageProvider, LocalDateTime createdAt) {
        this.documentId = documentId;
        this.customerId = customerId;
        this.category = category;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.checksumSha256 = checksumSha256;
        this.storageKey = storageKey;
        this.storageProvider = storageProvider;
        this.createdAt = createdAt;
    }

    public String getDocumentId() { return documentId; }
    public Long getCustomerId() { return customerId; }
    public String getCategory() { return category; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public String getChecksumSha256() { return checksumSha256; }
    public String getStorageKey() { return storageKey; }
    public String getStorageProvider() { return storageProvider; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getStoredFilename() {
        String key = storageKey == null ? "" : storageKey.replace('\\', '/');
        int slash = key.lastIndexOf('/');
        return slash >= 0 ? key.substring(slash + 1) : key;
    }
}
