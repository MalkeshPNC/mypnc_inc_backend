package com.pnc.masters.quote.application;

import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteAttachment;
import com.pnc.masters.quote.QuoteAttachmentRepository;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteAttachmentResponse;
import com.pnc.masters.quote.api.QuoteNotFoundException;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.quote.mail.QuotePdfNames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class QuoteAttachmentService implements QuoteGeneratedPdfStore {

    public static final String P_NOTES = "p_notes";
    public static final String QUOTE_CATEGORY = "quote";
    public static final List<String> CATEGORIES = List.of(P_NOTES, QUOTE_CATEGORY, "RFQ", "PCBQ");

    private static final Set<String> INLINE_TYPES = Set.of("application/pdf", "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final Map<String, String> TYPES_BY_EXTENSION = Map.of(
            "pdf", "application/pdf",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    private final QuoteRepository quotes;
    private final QuoteAttachmentRepository attachments;
    private final DocumentStorage storage;
    private final DocumentProperties properties;

    public QuoteAttachmentService(QuoteRepository quotes,
                                  QuoteAttachmentRepository attachments,
                                  DocumentStorage storage,
                                  DocumentProperties properties) {
        this.quotes = quotes;
        this.attachments = attachments;
        this.storage = storage;
        this.properties = properties;
    }

    /** Disk name is the category, then the file name, so a folder of files stays readable. */
    public static String storedFileName(String category, String fileName) {
        return category + "_" + fileName;
    }

    public static String storageKey(Long qid, String category, String fileName) {
        return "quotes/" + qid + "/" + storedFileName(category, fileName);
    }

    public static boolean inline(String contentType) {
        return contentType != null && INLINE_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public List<String> categories(Long qid) {
        return attachments.findCategories(qid);
    }

    @Transactional(readOnly = true)
    public boolean hasPNotes(Long qid) {
        return attachments.existsByQidAndCategory(qid, P_NOTES);
    }

    @Transactional(readOnly = true)
    public List<QuoteAttachmentResponse> list(Long qid) {
        requireQuote(qid);
        return attachments.findByQidOrderByCreatedAtDescAttachmentIdDesc(qid).stream()
                .map(QuoteAttachmentService::toResponse)
                .toList();
    }

    public QuoteAttachmentResponse upload(Long qid, String category, MultipartFile file, Long userId) {
        requireQuote(qid);
        String storedCategory = requireCategory(category);
        String fileName = fileName(file);
        String contentType = contentType(fileName);
        if (file == null || file.isEmpty()) {
            throw new QuoteValidationException("Choose a file.");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new QuoteValidationException("The file must be 10 MB or smaller.");
        }
        String key = storageKey(qid, storedCategory, fileName);
        long size = store(key, file);
        QuoteAttachment row = attachments.findByQidAndCategoryAndOriginalFilenameIgnoreCase(qid, storedCategory, fileName)
                .orElseGet(QuoteAttachment::new);
        String previousKey = row.getStorageKey();
        row.setQid(qid);
        row.setCategory(storedCategory);
        row.setOriginalFilename(fileName);
        row.setContentType(contentType);
        row.setFileSize(size);
        row.setStorageKey(key);
        row.setGenerated(false);
        row.setCreatedAt(LocalDateTime.now());
        row.setCreatedByUserId(userId);
        QuoteAttachment saved = attachments.save(row);
        if (previousKey != null && !previousKey.equals(key)) {
            deleteQuietly(previousKey);
        }
        return toResponse(saved);
    }

    /**
     * Writes the preview PDF into this quote's folder and keeps a single generated row.
     */
    @Override
    public void storeGeneratedPdf(Quote quote, byte[] pdf) {
        String fileName = QuotePdfNames.fileName(quote.getQuoteNumber(), quote.getQid());
        String key = storageKey(quote.getQid(), QUOTE_CATEGORY, fileName);
        try {
            storage.store(key, new ByteArrayInputStream(pdf));
        } catch (IOException exception) {
            throw new QuoteValidationException("The quote PDF could not be saved.");
        }
        for (QuoteAttachment previous : attachments.findByQidAndGeneratedTrue(quote.getQid())) {
            if (!fileName.equalsIgnoreCase(previous.getOriginalFilename())) {
                deleteQuietly(previous.getStorageKey());
                attachments.delete(previous);
            }
        }
        QuoteAttachment row = attachments.findByQidAndCategoryAndOriginalFilenameIgnoreCase(
                quote.getQid(), QUOTE_CATEGORY, fileName).orElseGet(QuoteAttachment::new);
        String previousKey = row.getStorageKey();
        row.setQid(quote.getQid());
        row.setCategory(QUOTE_CATEGORY);
        row.setOriginalFilename(fileName);
        row.setContentType("application/pdf");
        row.setFileSize(pdf.length);
        row.setStorageKey(key);
        row.setGenerated(true);
        row.setCreatedAt(LocalDateTime.now());
        attachments.save(row);
        if (previousKey != null && !previousKey.equals(key)) {
            deleteQuietly(previousKey);
        }
    }

    @Transactional(readOnly = true)
    public QuoteAttachment open(Long qid, Long attachmentId) {
        requireQuote(qid);
        return attachments.findByAttachmentIdAndQid(attachmentId, qid)
                .orElseThrow(() -> new QuoteValidationException("Attachment was not found."));
    }

    public void delete(Long qid, Long attachmentId) {
        QuoteAttachment row = open(qid, attachmentId);
        if (row.isGenerated()) {
            throw new QuoteValidationException("The generated quote PDF cannot be deleted.");
        }
        deleteQuietly(row.getStorageKey());
        attachments.delete(row);
    }

    private long store(String key, MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return storage.store(key, input).size();
        } catch (IOException exception) {
            throw new QuoteValidationException("The file could not be saved.");
        }
    }

    private void deleteQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            storage.delete(key);
        } catch (IOException exception) {
            throw new QuoteValidationException("The file could not be removed.");
        }
    }

    private Quote requireQuote(Long qid) {
        return quotes.findByQidAndIsDeletedFalse(qid).orElseThrow(() -> new QuoteNotFoundException(qid));
    }

    private static String requireCategory(String category) {
        String value = category == null ? "" : category.trim();
        if (!CATEGORIES.contains(value)) {
            throw new QuoteValidationException("Category must be p_notes, quote, RFQ, or PCBQ.");
        }
        return value;
    }

    private static String fileName(MultipartFile file) {
        String raw = file == null || file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        name = name.replaceAll("[\\r\\n\\t]", "");
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.indexOf('/') >= 0) {
            throw new QuoteValidationException("Enter a file name.");
        }
        if (name.length() > 200) {
            throw new QuoteValidationException("The file name must be 200 characters or fewer.");
        }
        return name;
    }

    private static String contentType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String type = TYPES_BY_EXTENSION.get(extension);
        if (type == null) {
            throw new QuoteValidationException("Upload a PDF, Excel, Word, or image file.");
        }
        return type;
    }

    private static QuoteAttachmentResponse toResponse(QuoteAttachment row) {
        return new QuoteAttachmentResponse(
                row.getAttachmentId(),
                row.getQid(),
                row.getCategory(),
                row.getOriginalFilename(),
                row.getContentType(),
                row.getFileSize(),
                row.isGenerated(),
                row.getCreatedAt());
    }
}
