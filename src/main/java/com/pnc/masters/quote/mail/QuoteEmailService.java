package com.pnc.masters.quote.mail;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteEmailPdfFile;
import com.pnc.masters.quote.api.QuoteEmailPdfResponse;
import com.pnc.masters.quote.api.QuoteEmailRequest;
import com.pnc.masters.quote.api.QuoteNotFoundException;
import com.pnc.masters.quote.api.QuoteValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuoteEmailService {

    static final int MAX_PDF_BYTES = 8 * 1024 * 1024;

    private final QuoteRepository quotes;
    private final QuoteEmailOutboxRepository outbox;
    private final DocumentStorage storage;
    private final QuoteMailbox mailbox;
    private final AppNotifier notifier;
    private final Clock clock;

    public QuoteEmailService(QuoteRepository quotes,
                             QuoteEmailOutboxRepository outbox,
                             DocumentStorage storage,
                             QuoteMailbox mailbox,
                             AppNotifier notifier,
                             Clock clock) {
        this.quotes = quotes;
        this.outbox = outbox;
        this.storage = storage;
        this.mailbox = mailbox;
        this.notifier = notifier;
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
        String key = QuotePdfNames.storageKey(quote.getQuoteNumber(), quote.getQid());
        try {
            storage.store(key, new ByteArrayInputStream(pdf));
        } catch (IOException exception) {
            throw new QuoteValidationException("The quote PDF could not be saved.");
        }
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
        QuoteEmailOutbox row = new QuoteEmailOutbox();
        row.setQuoteId(quote.getQid());
        row.setUserId(userId);
        row.setRecipients(String.join(", ", to));
        row.setCcRecipients(cc.isEmpty() ? null : String.join(", ", cc));
        row.setSubject(subject);
        row.setHtmlBody(html);
        row.setAttachmentName(QuotePdfNames.fileName(quote.getQuoteNumber(), quote.getQid()));
        row.setPdfContent(pdf);
        row.setStatus(QuoteEmailOutbox.QUEUED);
        row.setCreatedAt(LocalDateTime.now(clock));
        outbox.save(row);
    }

    @Transactional
    public Long claimNext() {
        QuoteEmailOutbox next = outbox.findFirstByStatusOrderByIdAsc(QuoteEmailOutbox.QUEUED).orElse(null);
        if (next == null) {
            return null;
        }
        return outbox.claim(next.getId()) == 1 ? next.getId() : null;
    }

    public void dispatch(Long id) {
        QuoteEmailOutbox row = outbox.findById(id).orElse(null);
        if (row == null || !QuoteEmailOutbox.SENDING.equals(row.getStatus())) {
            return;
        }
        String route = "/quote-systems/quotes/" + row.getQuoteId() + "/pdf";
        try {
            mailbox.send(row);
            finish(row, QuoteEmailOutbox.SENT, null);
            notifier.notifyUsers(
                    List.of(row.getUserId()),
                    "quote-email",
                    "Quote email sent",
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
        row.setSentAt(LocalDateTime.now(clock));
        row.setErrorMessage(trim(error, 500));
        outbox.save(row);
    }

    private byte[] readStored(Quote quote) {
        String key = QuotePdfNames.storageKey(quote.getQuoteNumber(), quote.getQid());
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
            throw new QuoteValidationException("Save the quote PDF before sending the email.");
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
