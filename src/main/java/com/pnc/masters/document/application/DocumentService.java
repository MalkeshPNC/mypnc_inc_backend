package com.pnc.masters.document.application;

import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import com.pnc.masters.document.Document;
import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentRepository;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.document.api.DocumentNotFoundException;
import com.pnc.masters.document.api.DocumentResponse;
import com.pnc.masters.document.api.DocumentValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository repository;
    private final DocumentStorage storage;
    private final DocumentProperties properties;
    private final CustomerRepository customerRepository;

    public DocumentService(DocumentRepository repository, DocumentStorage storage,
                           DocumentProperties properties, CustomerRepository customerRepository) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
        this.customerRepository = customerRepository;
    }

    public DocumentResponse upload(String category, Long customerId, MultipartFile file) {
        String normalizedCategory = normalizeCategory(category);
        validate(normalizedCategory, file);
        if (customerId != null) {
            customerRepository.findByCustIdAndIsDeletedFalse(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
        }

        String documentId = UUID.randomUUID().toString();
        String filename = sanitizeFilename(file.getOriginalFilename());
        String storageKey = buildStorageKey(customerId, normalizedCategory, documentId, filename);
        try (InputStream input = file.getInputStream()) {
            DocumentStorage.StoredDocument stored = storage.store(storageKey, input);
            Document document = new Document(documentId, customerId, normalizedCategory, filename,
                    contentType(file), stored.size(), stored.checksumSha256(), storageKey,
                    "local", LocalDateTime.now());
            return toResponse(repository.save(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store document", exception);
        } catch (RuntimeException exception) {
            try {
                storage.delete(storageKey);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long customerId, String category) {
        String normalizedCategory = category == null || category.isBlank() ? null : normalizeCategory(category);
        return repository.search(customerId, normalizedCategory).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Document getActive(String documentId) {
        return repository.findByDocumentIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public void delete(String documentId) {
        Document document = getActive(documentId);
        document.setDeletedAt(LocalDateTime.now());
    }

    static String buildStorageKey(Long customerId, String category, String documentId, String originalFilename) {
        String owner = customerId == null ? "library" : String.valueOf(customerId);
        String safeCategory = sanitizeSegment(category);
        String shortId = documentId.replace("-", "");
        if (shortId.length() > 8) {
            shortId = shortId.substring(0, 8);
        }
        return owner + "/" + safeCategory + "/" + owner + "_" + safeCategory + "_" + shortId + "_"
                + sanitizeFilename(originalFilename);
    }

    static String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }
        String trimmed = category.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    static String sanitizeFilename(String filename) {
        String value = filename == null ? "file" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isBlank() || value.equals(".") || value.equals("..")) {
            return "file";
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    static String sanitizeSegment(String value) {
        String sanitized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        sanitized = sanitized.replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "other" : sanitized;
    }

    private void validate(String category, MultipartFile file) {
        if (category.isBlank()) {
            throw new DocumentValidationException("category is required");
        }
        if (file == null || file.isEmpty()) {
            throw new DocumentValidationException("file must not be empty");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new DocumentValidationException("file exceeds the maximum allowed size");
        }
        String contentType = contentType(file);
        if (!properties.getAllowedContentTypes().isEmpty()
                && !properties.getAllowedContentTypes().contains(contentType)) {
            throw new DocumentValidationException("file type is not allowed");
        }
    }

    private static String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream" : file.getContentType();
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getDocumentId(),
                document.getCustomerId(),
                document.getCategory(),
                document.getOriginalFilename(),
                document.getStoredFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getChecksumSha256(),
                document.getCreatedAt());
    }
}
