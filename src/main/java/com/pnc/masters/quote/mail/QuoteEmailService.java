package com.pnc.masters.quote.mail;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteAttachment;
import com.pnc.masters.quote.QuoteAttachmentRepository;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteEmailLogDetailResponse;
import com.pnc.masters.quote.api.QuoteEmailLogNotFoundException;
import com.pnc.masters.quote.api.QuoteEmailLogPageResponse;
import com.pnc.masters.quote.api.QuoteEmailLogResponse;
import com.pnc.masters.quote.api.QuoteEmailPdfFile;
import com.pnc.masters.quote.api.QuoteEmailPdfResponse;
import com.pnc.masters.quote.api.QuoteEmailRequest;
import com.pnc.masters.quote.api.QuoteEmailSenderResponse;
import com.pnc.masters.quote.api.QuoteNotFoundException;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.quote.application.QuoteAttachmentService;
import com.pnc.masters.quote.application.QuoteGeneratedPdfStore;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.security.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuoteEmailService {

    static final int MAX_PDF_BYTES = 8 * 1024 * 1024;
    private static final int LOG_PAGE_SIZE = 20;

    private final QuoteRepository quotes;
    private final QuoteEmailOutboxRepository outbox;
    private final DocumentStorage storage;
    private final QuoteMailbox mailbox;
    private final AppNotifier notifier;
    private final MailSettings mailSettings;
    private final AppUserRepository users;
    private final QuoteGeneratedPdfStore attachments;
    private final QuoteAttachmentRepository quoteAttachments;
    private final QuoteEmailFileRepository emailFiles;
    private final Clock clock;

    public QuoteEmailService(QuoteRepository quotes,
                             QuoteEmailOutboxRepository outbox,
                             DocumentStorage storage,
                             QuoteMailbox mailbox,
                             AppNotifier notifier,
                             MailSettings mailSettings,
                             AppUserRepository users,
                             QuoteGeneratedPdfStore attachments,
                             QuoteAttachmentRepository quoteAttachments,
                             QuoteEmailFileRepository emailFiles,
                             Clock clock) {
        this.quotes = quotes;
        this.outbox = outbox;
        this.storage = storage;
        this.mailbox = mailbox;
        this.notifier = notifier;
        this.mailSettings = mailSettings;
        this.users = users;
        this.attachments = attachments;
        this.quoteAttachments = quoteAttachments;
        this.emailFiles = emailFiles;
        this.clock = clock;
    }

    public QuoteEmailPdfResponse store(Long qid, byte[] pdf) {
        Quote quote = requireQuote(qid);
        if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
            throw new QuoteValidationException("A PDF is required.");
        }
        if (pdf.length > MAX_PDF_BYTES) {
            throw new QuoteValidationException("The quote PDF must be 8 MB or smaller.");
        }
        attachments.storeGeneratedPdf(quote, pdf);
        return new QuoteEmailPdfResponse(QuotePdfNames.fileName(quote.getQuoteNumber(), quote.getQid()));
    }

    public QuoteEmailPdfFile open(Long qid) {
        Quote quote = requireQuote(qid);
        String fileName = QuotePdfNames.fileName(quote.getQuoteNumber(), quote.getQid());
        return new QuoteEmailPdfFile(fileName, readStored(quote));
    }

    @Transactional
    public int releaseStuck() {
        return outbox.releaseStuck();
    }

    @Transactional
    public void enqueue(Long qid, Long userId, QuoteEmailRequest request) {
        if (request == null) {
            throw new QuoteValidationException("Enter a message.");
        }
        Quote quote = requireQuote(qid);
        List<String> to = QuoteEmailAddresses.parse(request.to());
        List<String> cc = QuoteEmailAddresses.parse(request.cc());
        QuoteEmailAddresses.requireLimits(to, cc);
        String subject = request.subject() == null ? "" : request.subject().trim();
        if (subject.isBlank()) {
            throw new QuoteValidationException("Enter a subject.");
        }
        if (subject.length() > 200) {
            subject = subject.substring(0, 200);
        }
        String html = QuoteEmailHtml.clean(request.body());
        if (QuoteEmailHtml.isBlank(html)) {
            throw new QuoteValidationException("Enter a message.");
        }
        byte[] pdf = readStored(quote);
        List<QuoteEmailFile> notes = pNotesFor(quote);
        QuoteEmailOutbox row = new QuoteEmailOutbox();
        row.setQuoteId(quote.getQid());
        row.setQuoteNumber(quote.getQuoteNumber() == null ? "" : quote.getQuoteNumber());
        row.setUserId(userId);
        row.setFromAddress(mailSettings.getFromAddress() == null ? "" : mailSettings.getFromAddress().trim());
        row.setRecipients(String.join(", ", to));
        row.setCcRecipients(cc.isEmpty() ? null : String.join(", ", cc));
        row.setSubject(subject);
        row.setHtmlBody(html);
        row.setAttachmentName(QuotePdfNames.fileName(quote.getQuoteNumber(), quote.getQid()));
        row.setPdfContent(pdf);
        row.setStatus(QuoteEmailOutbox.QUEUED);
        row.setCreatedAt(LocalDateTime.now(clock));
        QuoteEmailOutbox saved = outbox.save(row);
        for (QuoteEmailFile file : notes) {
            file.setOutboxId(saved.getId());
            emailFiles.save(file);
        }
    }

    /** PNC Notes and Labor Only share the p_notes files, and those travel with the quote PDF. */
    private List<QuoteEmailFile> pNotesFor(Quote quote) {
        if (!quote.isPncNotes() && !quote.isLaborOnly()) {
            return List.of();
        }
        List<QuoteAttachment> rows = quoteAttachments.findByQidAndCategoryOrderByAttachmentIdAsc(
                quote.getQid(), QuoteAttachmentService.P_NOTES);
        if (rows.isEmpty()) {
            throw new QuoteValidationException("Upload a p_notes document before sending this quote.");
        }
        List<QuoteEmailFile> files = new ArrayList<>();
        for (QuoteAttachment row : rows) {
            QuoteEmailFile file = new QuoteEmailFile();
            file.setFileName(row.getOriginalFilename());
            file.setContentType(row.getContentType());
            file.setContent(readAttachment(row));
            files.add(file);
        }
        return files;
    }

    private byte[] readAttachment(QuoteAttachment row) {
        try (InputStream input = storage.open(row.getStorageKey())) {
            byte[] content = input.readAllBytes();
            if (content.length == 0) {
                throw new QuoteValidationException("A p_notes document could not be read.");
            }
            return content;
        } catch (IOException exception) {
            throw new QuoteValidationException("A p_notes document could not be read.");
        }
    }

    @Transactional
    public Long claimNext() {
        QuoteEmailOutbox next = outbox.findFirstByStatusOrderByIdAsc(QuoteEmailOutbox.QUEUED).orElse(null);
        if (next == null) {
            return null;
        }
        return outbox.claim(next.getId()) == 1 ? next.getId() : null;
    }

    @Transactional(readOnly = true)
    public QuoteEmailLogPageResponse log(Long callerId, Long senderId, int page, Integer size) {
        boolean superAdmin = isSuperAdmin(callerId);
        int pageSize = size == null || size < 1 || size > 50 ? LOG_PAGE_SIZE : size;
        int pageIndex = Math.max(page, 0);
        Long ownerId = superAdmin ? senderId : callerId;
        Page<QuoteEmailOutbox> result = ownerId == null
                ? outbox.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(pageIndex, pageSize))
                : outbox.findByUserIdOrderByCreatedAtDescIdDesc(ownerId, PageRequest.of(pageIndex, pageSize));
        Map<Long, AppUser> senders = superAdmin ? loadUsers(result.getContent().stream().map(QuoteEmailOutbox::getUserId).distinct().toList()) : Map.of();
        List<QuoteEmailLogResponse> items = result.getContent().stream()
                .map(row -> toLogRow(row, superAdmin ? senders.get(row.getUserId()) : null))
                .toList();
        List<QuoteEmailSenderResponse> senderChoices = superAdmin ? senderChoices() : List.of();
        return new QuoteEmailLogPageResponse(items, result.getTotalElements(), pageIndex, pageSize, senderChoices);
    }

    @Transactional(readOnly = true)
    public QuoteEmailLogDetailResponse logDetail(Long callerId, Long id) {
        QuoteEmailOutbox row = outbox.findById(id).orElseThrow(() -> new QuoteEmailLogNotFoundException(id));
        if (!isSuperAdmin(callerId) && !callerId.equals(row.getUserId())) {
            throw new QuoteEmailLogNotFoundException(id);
        }
        AppUser sender = isSuperAdmin(callerId) ? users.findById(row.getUserId()).orElse(null) : null;
        return new QuoteEmailLogDetailResponse(
                row.getId(),
                row.getQuoteId(),
                row.getQuoteNumber(),
                row.getFromAddress(),
                row.getRecipients(),
                row.getCcRecipients(),
                row.getSubject(),
                row.getAttachmentName(),
                row.getHtmlBody(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getCreatedAt(),
                row.getSentAt(),
                sender == null ? null : sender.getUserId(),
                sender == null ? null : sender.getDisplayName(),
                sender == null ? null : sender.getEmail());
    }

    private List<QuoteEmailSenderResponse> senderChoices() {
        Map<Long, AppUser> found = loadUsers(outbox.findDistinctUserIds());
        return found.values().stream()
                .sorted(Comparator.comparing(AppUser::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new QuoteEmailSenderResponse(user.getUserId(), user.getDisplayName(), user.getEmail()))
                .toList();
    }

    private Map<Long, AppUser> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(AppUser::getUserId, Function.identity()));
    }

    private QuoteEmailLogResponse toLogRow(QuoteEmailOutbox row, AppUser sender) {
        return new QuoteEmailLogResponse(
                row.getId(),
                row.getQuoteId(),
                row.getQuoteNumber(),
                row.getRecipients(),
                row.getCcRecipients(),
                row.getSubject(),
                row.getAttachmentName(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getCreatedAt(),
                row.getSentAt(),
                sender == null ? null : sender.getUserId(),
                sender == null ? null : sender.getDisplayName(),
                sender == null ? null : sender.getEmail());
    }

    private boolean isSuperAdmin(Long userId) {
        return users.findById(userId)
                .map(user -> user.getRoles().stream().anyMatch(role -> AuthService.ROLE_SUPER_ADMIN.equals(role.getRoleCode())))
                .orElse(false);
    }

    @Transactional
    public void dispatch(Long id) {
        QuoteEmailOutbox row = outbox.findById(id).orElse(null);
        if (row == null || !QuoteEmailOutbox.SENDING.equals(row.getStatus())) {
            return;
        }
        String route = "/quote-systems/quotes/" + row.getQuoteId() + "/pdf";
        try {
            row.setExtraFiles(emailFiles.findByOutboxIdOrderByFileIdAsc(row.getId()));
            mailbox.send(row);
            finish(row, QuoteEmailOutbox.SENT, null);
            notifier.notifyUsers(
                    List.of(row.getUserId()),
                    "quote-email",
                    "Quote email sent to " + row.getRecipients(),
                    "Sent: " + row.getSubject(),
                    route);
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "The quote email could not be sent.";
            }
            finish(row, QuoteEmailOutbox.FAILED, message);
            notifier.notifyUsers(List.of(row.getUserId()), "quote-email", "Quote email failed", message, route);
        }
    }

    private void finish(QuoteEmailOutbox row, String status, String error) {
        row.setStatus(status);
        row.setPdfContent(null);
        emailFiles.deleteByOutboxId(row.getId());
        row.setSentAt(LocalDateTime.now(clock));
        row.setErrorMessage(trim(error, 500));
        outbox.save(row);
    }

    private byte[] readStored(Quote quote) {
        String fileName = QuotePdfNames.fileName(quote.getQuoteNumber(), quote.getQid());
        String[] keys = {
                QuoteAttachmentService.storageKey(quote.getQid(), QuoteAttachmentService.QUOTE_CATEGORY, fileName),
                "quotes/" + quote.getQid() + "/" + fileName,
                QuotePdfNames.storageKey(quote.getQuoteNumber(), quote.getQid())
        };
        for (String key : keys) {
            try {
                return readKey(key);
            } catch (NoSuchFileException missing) {
                // The next location is an older place this PDF may have been saved.
            }
        }
        throw new QuoteValidationException("Save the quote PDF before sending the email.");
    }

    private byte[] readKey(String key) throws NoSuchFileException {
        try (InputStream input = storage.open(key)) {
            byte[] pdf = input.readAllBytes();
            if (pdf.length == 0) {
                throw new QuoteValidationException("Save the quote PDF before sending the email.");
            }
            if (pdf.length > MAX_PDF_BYTES) {
                throw new QuoteValidationException("The quote PDF must be 8 MB or smaller.");
            }
            return pdf.clone();
        } catch (NoSuchFileException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new QuoteValidationException("The quote PDF could not be read.");
        }
    }

    private Quote requireQuote(Long qid) {
        return quotes.findByQidAndIsDeletedFalse(qid).orElseThrow(() -> new QuoteNotFoundException(qid));
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
