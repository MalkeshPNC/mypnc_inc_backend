package com.pnc.masters.customer.reminder;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderBatchRequest;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderRecipientRequest;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderReviseRequest;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderActiveResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderAttachmentResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderBatchResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderCountsResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderLogDetailResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderLogItemResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderLogPageResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderRecipientResponse;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.quote.mail.QuoteEmailAddresses;
import com.pnc.masters.quote.mail.QuoteEmailFile;
import com.pnc.masters.quote.mail.QuoteEmailHtml;
import com.pnc.masters.quote.mail.QuoteEmailOutbox;
import com.pnc.masters.quote.mail.QuoteMailbox;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ReminderEmailService {

    static final int MAX_FILE_BYTES = 8 * 1024 * 1024;
    static final int MAX_FILES = 10;
    private static final int MAX_CC = 20;

    private final ReminderBatchRepository batches;
    private final ReminderRecipientRepository recipients;
    private final ReminderAttachmentRepository attachments;
    private final CustomerRepository customers;
    private final ContactRepository contacts;
    private final AppUserRepository users;
    private final ReminderAudienceLookup audience;
    private final QuoteMailbox mailbox;
    private final Clock clock;

    public ReminderEmailService(ReminderBatchRepository batches,
                                ReminderRecipientRepository recipients,
                                ReminderAttachmentRepository attachments,
                                CustomerRepository customers,
                                ContactRepository contacts,
                                AppUserRepository users,
                                ReminderAudienceLookup audience,
                                QuoteMailbox mailbox,
                                Clock clock) {
        this.batches = batches;
        this.recipients = recipients;
        this.attachments = attachments;
        this.customers = customers;
        this.contacts = contacts;
        this.users = users;
        this.audience = audience;
        this.mailbox = mailbox;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReminderActiveResponse active(String kind) {
        ReminderAudienceService.requireKind(kind);
        ReminderBatch open = unfinished(kind);
        return new ReminderActiveResponse(open == null ? null : toResponse(open));
    }

    @Transactional(readOnly = true)
    public ReminderBatchResponse get(Long id) {
        return toResponse(requireBatch(id));
    }

    @Transactional(readOnly = true)
    public ReminderLogPageResponse log(String kind, String status, int page, Integer size) {
        int pageSize = size == null || size < 1 || size > 50 ? 20 : size;
        int pageIndex = Math.max(page, 0);
        Page<Object[]> result = recipients.searchLog(blankToNull(kind), blankToNull(status), PageRequest.of(pageIndex, pageSize));
        List<ReminderLogItemResponse> items = result.getContent().stream().map(row -> logItem((ReminderRecipient) row[0], (ReminderBatch) row[1])).toList();
        return new ReminderLogPageResponse(items, result.getTotalElements(), pageIndex, pageSize);
    }

    @Transactional(readOnly = true)
    public ReminderLogDetailResponse logDetail(Long recipientId) {
        ReminderRecipient row = recipients.findById(recipientId)
                .orElseThrow(() -> new ReminderNotFoundException("That email was not found."));
        ReminderBatch batch = batches.findById(row.getBatchId())
                .orElseThrow(() -> new ReminderNotFoundException("That email was not found."));
        ReminderLogItemResponse item = logItem(row, batch);
        return new ReminderLogDetailResponse(
                item.id(), item.batchId(), item.kind(), item.subject(), batch.getHtmlBody(), item.cc(),
                item.startedBy(), item.createdAt(), item.customerName(), item.contactName(), item.email(),
                item.status(), item.errorMessage(), item.sentAt());
    }

    private ReminderLogItemResponse logItem(ReminderRecipient row, ReminderBatch batch) {
        return new ReminderLogItemResponse(
                row.getId(),
                batch.getId(),
                batch.getKind(),
                batch.getSubject(),
                batch.getCcRecipients(),
                batch.getStartedByName(),
                batch.getCreatedAt(),
                row.getCustomerName(),
                row.getContactName(),
                row.getEmail(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getSentAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String trimmed = value.trim();
        if ("no-lead".equals(trimmed) || "announcement".equals(trimmed)) {
            return trimmed;
        }
        if (ReminderRecipient.PENDING.equals(trimmed)
                || ReminderRecipient.SENDING.equals(trimmed)
                || ReminderRecipient.SENT.equals(trimmed)
                || ReminderRecipient.FAILED.equals(trimmed)
                || ReminderRecipient.NOT_CONFIRMED.equals(trimmed)) {
            return trimmed;
        }
        throw new ReminderValidationException("That filter is not recognized.");
    }

    @Transactional
    public ReminderBatchResponse create(Long userId, ReminderBatchRequest request, List<MultipartFile> files) {
        if (request == null) {
            throw new ReminderValidationException("Enter a message.");
        }
        String kind = request.kind();
        ReminderAudienceService.requireKind(kind);
        if (unfinished(kind) != null) {
            throw new ReminderConflictException("A " + label(kind) + " run is already open. Continue that one.");
        }
        Message message = message(request.subject(), request.body(), request.cc());
        List<ReminderRecipientRequest> chosen = deduped(request.recipients());
        if (chosen.isEmpty()) {
            throw new ReminderValidationException("Choose at least one contact with an email.");
        }
        Set<Long> allowed = audience.customerIds(kind);
        AppUser user = users.findById(userId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        ReminderBatch batch = new ReminderBatch();
        batch.setKind(kind);
        batch.setSubject(message.subject());
        batch.setHtmlBody(message.html());
        batch.setCcRecipients(message.cc());
        batch.setStatus(ReminderBatch.RUNNING);
        batch.setStartedByUserId(userId);
        batch.setStartedByName(displayName(user));
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        ReminderBatch saved = batches.save(batch);
        for (ReminderRecipientRequest item : chosen) {
            recipients.save(recipientRow(saved, item, allowed, now));
        }
        storeFiles(saved.getId(), files, 0);
        return toResponse(saved);
    }

    @Transactional
    public ReminderBatchResponse pause(Long id) {
        ReminderBatch batch = requireBatch(id);
        if (ReminderBatch.STOPPED.equals(batch.getStatus())) {
            throw new ReminderValidationException("This run is stopped. Use Send the rest to continue.");
        }
        if (!ReminderBatch.PAUSED.equals(batch.getStatus())) {
            batch.setStatus(ReminderBatch.PAUSED);
            batch.setUpdatedAt(LocalDateTime.now(clock));
            batches.save(batch);
        }
        return toResponse(batch);
    }

    @Transactional
    public ReminderBatchResponse resume(Long id) {
        ReminderBatch batch = requireBatch(id);
        if (ReminderBatch.STOPPED.equals(batch.getStatus())) {
            throw new ReminderValidationException("This run is stopped. Use Send the rest to continue.");
        }
        if (!ReminderBatch.RUNNING.equals(batch.getStatus())) {
            batch.setStatus(ReminderBatch.RUNNING);
            batch.setUpdatedAt(LocalDateTime.now(clock));
            batches.save(batch);
        }
        return toResponse(batch);
    }

    @Transactional
    public ReminderBatchResponse stop(Long id) {
        ReminderBatch batch = requireBatch(id);
        if (!ReminderBatch.STOPPED.equals(batch.getStatus())) {
            batch.setStatus(ReminderBatch.STOPPED);
            batch.setUpdatedAt(LocalDateTime.now(clock));
            batches.save(batch);
        }
        return toResponse(batch);
    }

    @Transactional
    public ReminderBatchResponse sendRest(Long id, ReminderReviseRequest request, List<MultipartFile> files) {
        ReminderBatch batch = requireEditable(id);
        applyRevision(batch, request, files);
        List<ReminderRecipient> rows = recipients.findByBatchIdOrderByIdAsc(batch.getId());
        boolean queued = false;
        for (ReminderRecipient row : rows) {
            if (ReminderRecipient.PENDING.equals(row.getStatus())) {
                queued = true;
            }
            if (ReminderRules.requeueForRest(row.getStatus())) {
                row.setStatus(ReminderRecipient.PENDING);
                row.setErrorMessage(null);
                recipients.save(row);
                queued = true;
            }
        }
        if (!queued) {
            throw new ReminderValidationException("There is nothing left to send. Sent addresses stay sent.");
        }
        batch.setStatus(ReminderBatch.RUNNING);
        batch.setUpdatedAt(LocalDateTime.now(clock));
        batches.save(batch);
        return toResponse(batch);
    }

    @Transactional
    public ReminderBatchResponse sendAgain(Long id, ReminderReviseRequest request, List<MultipartFile> files) {
        if (request == null || !Boolean.TRUE.equals(request.confirm())) {
            throw new ReminderValidationException("Confirm the second email before sending it again.");
        }
        ReminderBatch batch = requireEditable(id);
        applyRevision(batch, request, files);
        List<ReminderRecipient> rows = recipients.findByBatchIdOrderByIdAsc(batch.getId());
        boolean queued = false;
        for (ReminderRecipient row : rows) {
            if (ReminderRules.requeueForAgain(row.getStatus())) {
                row.setStatus(ReminderRecipient.PENDING);
                row.setErrorMessage(null);
                row.setSentAt(null);
                recipients.save(row);
                queued = true;
            }
        }
        if (!queued) {
            throw new ReminderValidationException("Nobody has received this email yet.");
        }
        batch.setStatus(ReminderBatch.RUNNING);
        batch.setUpdatedAt(LocalDateTime.now(clock));
        batches.save(batch);
        return toResponse(batch);
    }

    @Transactional
    public ReminderBatchResponse markSent(Long batchId, Long recipientId) {
        ReminderBatch batch = requireBatch(batchId);
        ReminderRecipient row = requireRecipient(batchId, recipientId);
        if (!ReminderRecipient.NOT_CONFIRMED.equals(row.getStatus())) {
            throw new ReminderValidationException("That address is not waiting for confirmation.");
        }
        markRowSent(batch, row);
        return toResponse(batch);
    }

    @Transactional
    public ReminderBatchResponse retry(Long batchId, Long recipientId) {
        ReminderBatch batch = requireBatch(batchId);
        ReminderRecipient row = requireRecipient(batchId, recipientId);
        if (!ReminderRecipient.NOT_CONFIRMED.equals(row.getStatus())) {
            throw new ReminderValidationException("That address is not waiting for confirmation.");
        }
        row.setStatus(ReminderRecipient.PENDING);
        row.setErrorMessage(null);
        recipients.save(row);
        if (ReminderBatch.STOPPED.equals(batch.getStatus())) {
            batch.setStatus(ReminderBatch.RUNNING);
            batch.setUpdatedAt(LocalDateTime.now(clock));
            batches.save(batch);
        }
        return toResponse(batch);
    }

    /** Rows still marked sending never reached a saved result. Leave them not confirmed. */
    @Transactional
    public int releaseStuck() {
        List<ReminderRecipient> stuck = recipients.findByStatus(ReminderRecipient.SENDING);
        if (stuck == null || stuck.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (ReminderRecipient row : stuck) {
            String next = ReminderRules.afterRestart(row.getStatus());
            if (!next.equals(row.getStatus())) {
                row.setStatus(next);
                recipients.save(row);
                changed++;
            }
        }
        return changed;
    }

    @Transactional
    public Long claimNext() {
        List<ReminderRecipient> rows = recipients.findPendingOnRunningBatches(PageRequest.of(0, 1));
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        ReminderRecipient row = rows.get(0);
        if (row == null || !ReminderRules.claimable(ReminderBatch.RUNNING, row.getStatus())) {
            return null;
        }
        return recipients.claim(row.getId()) == 1 ? row.getId() : null;
    }

    @Transactional
    public void dispatch(Long id) {
        ReminderRecipient row = recipients.findById(id).orElse(null);
        if (row == null || !ReminderRecipient.SENDING.equals(row.getStatus())) {
            return;
        }
        ReminderBatch batch = batches.findById(row.getBatchId()).orElse(null);
        if (batch == null) {
            row.setStatus(ReminderRecipient.FAILED);
            row.setErrorMessage("The reminder batch is missing.");
            recipients.save(row);
            return;
        }
        try {
            mailbox.send(toMessage(batch, row));
            markRowSent(batch, row);
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "The email could not be sent.";
            }
            row.setStatus(ReminderRecipient.FAILED);
            row.setErrorMessage(trim(message, 500));
            row.setSentAt(null);
            recipients.save(row);
        }
    }

    private void markRowSent(ReminderBatch batch, ReminderRecipient row) {
        row.setStatus(ReminderRecipient.SENT);
        row.setErrorMessage(null);
        row.setSentAt(LocalDateTime.now(clock));
        recipients.save(row);
        recordFollowUp(batch, row);
    }

    /**
     * One follow-up per customer for this batch. A second address of the same customer
     * updates the date and does not add another count. People who were not sent are skipped.
     */
    private void recordFollowUp(ReminderBatch batch, ReminderRecipient row) {
        if (row.getCustomerId() == null) {
            return;
        }
        long alreadySent = recipients.countByBatchIdAndCustomerIdAndStatusAndIdNot(
                batch.getId(), row.getCustomerId(), ReminderRecipient.SENT, row.getId());
        Customer customer = customers.findById(row.getCustomerId()).orElse(null);
        if (customer == null || customer.isDeleted()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (ReminderBatch.NO_LEAD.equals(batch.getKind())) {
            if (alreadySent == 0) {
                customer.setFollowupCount(customer.getFollowupCount() + 1);
            }
            customer.setLastReminder(now);
            customer.setFollowupBy(batch.getStartedByName());
        } else if (ReminderBatch.ANNOUNCEMENT.equals(batch.getKind())) {
            if (alreadySent == 0) {
                customer.setAnnouncementFollowupCount(customer.getAnnouncementFollowupCount() + 1);
            }
            customer.setLastAnnouncement(now);
        } else {
            return;
        }
        customers.save(customer);
    }

    private QuoteEmailOutbox toMessage(ReminderBatch batch, ReminderRecipient row) {
        QuoteEmailOutbox mail = new QuoteEmailOutbox();
        mail.setRecipients(row.getEmail());
        mail.setCcRecipients(ccWithout(batch.getCcRecipients(), row.getEmail()));
        mail.setSubject(batch.getSubject());
        mail.setHtmlBody(batch.getHtmlBody());
        mail.setAttachmentName("");
        List<QuoteEmailFile> files = new ArrayList<>();
        List<ReminderAttachment> stored = attachments.findByBatchIdOrderByIdAsc(batch.getId());
        if (stored != null) {
            for (ReminderAttachment file : stored) {
                QuoteEmailFile extra = new QuoteEmailFile();
                extra.setFileName(file.getFileName());
                extra.setContentType(file.getContentType());
                extra.setContent(file.getContent());
                files.add(extra);
            }
        }
        mail.setExtraFiles(files);
        return mail;
    }

    private ReminderBatch requireEditable(Long id) {
        ReminderBatch batch = requireBatch(id);
        if (ReminderBatch.RUNNING.equals(batch.getStatus())) {
            throw new ReminderValidationException("Pause or stop the run before changing the message.");
        }
        return batch;
    }

    private void applyRevision(ReminderBatch batch, ReminderReviseRequest request, List<MultipartFile> files) {
        if (request == null) {
            throw new ReminderValidationException("Enter a message.");
        }
        Message message = message(request.subject(), request.body(), request.cc());
        batch.setSubject(message.subject());
        batch.setHtmlBody(message.html());
        batch.setCcRecipients(message.cc());
        replaceFiles(batch.getId(), request.keepAttachmentIds(), files);
    }

    private ReminderRecipient recipientRow(ReminderBatch batch,
                                           ReminderRecipientRequest item,
                                           Set<Long> allowed,
                                           LocalDateTime now) {
        if (item == null || item.customerId() == null || item.contactId() == null) {
            return addedAddress(batch, item, now);
        }
        Customer customer = customers.findByCustIdAndIsDeletedFalse(item.customerId())
                .orElseThrow(() -> new ReminderValidationException("A selected customer was not found."));
        if (!allowed.contains(customer.getCustId())) {
            throw new ReminderValidationException(customer.getCustomer() + " is not in this list.");
        }
        Contact contact = contacts.findById(item.contactId())
                .orElseThrow(() -> new ReminderValidationException("A selected contact was not found."));
        if (contact.getCustomer() == null || !customer.getCustId().equals(contact.getCustomer().getCustId())) {
            throw new ReminderValidationException("A selected contact does not belong to that customer.");
        }
        String email = contact.getEmail() == null ? "" : contact.getEmail().trim().toLowerCase(Locale.ROOT);
        List<String> parsed = QuoteEmailAddresses.parse(email);
        if (parsed.size() != 1) {
            throw new ReminderValidationException("A selected contact has no email address.");
        }
        ReminderRecipient row = new ReminderRecipient();
        row.setBatchId(batch.getId());
        row.setCustomerId(customer.getCustId());
        row.setContactId(contact.getContId());
        row.setCustomerName(trim(customer.getCustomer(), 200));
        row.setContactName(trim(contactName(contact), 200));
        row.setEmail(parsed.get(0));
        row.setStatus(ReminderRecipient.PENDING);
        row.setCreatedAt(now);
        return row;
    }

    /** An address typed into To that is not one of the customer's contacts. */
    private ReminderRecipient addedAddress(ReminderBatch batch, ReminderRecipientRequest item, LocalDateTime now) {
        if (item == null || item.customerId() != null || item.contactId() != null) {
            throw new ReminderValidationException("Choose a contact from the list.");
        }
        List<String> parsed = oneAddress(item.email());
        ReminderRecipient row = new ReminderRecipient();
        row.setBatchId(batch.getId());
        row.setCustomerName("");
        row.setEmail(parsed.get(0));
        row.setStatus(ReminderRecipient.PENDING);
        row.setCreatedAt(now);
        return row;
    }

    private List<String> oneAddress(String email) {
        List<String> parsed;
        try {
            parsed = QuoteEmailAddresses.parse(email);
        } catch (QuoteValidationException exception) {
            throw new ReminderValidationException("Enter a valid email address.");
        }
        if (parsed.size() != 1) {
            throw new ReminderValidationException("Enter one email address.");
        }
        return parsed;
    }

    private List<ReminderRecipientRequest> deduped(List<ReminderRecipientRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        List<ReminderRecipientRequest> kept = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ReminderRecipientRequest item : requested) {
            if (item == null || item.email() == null || item.email().isBlank()) {
                continue;
            }
            String email = item.email().trim().toLowerCase(Locale.ROOT);
            if (seen.add(email)) {
                kept.add(item);
            }
        }
        return kept;
    }

    private void replaceFiles(Long batchId, List<Long> keepIds, List<MultipartFile> files) {
        List<ReminderAttachment> existing = attachments.findByBatchIdOrderByIdAsc(batchId);
        Set<Long> keep = keepIds == null
                ? existing.stream().map(ReminderAttachment::getId).collect(java.util.stream.Collectors.toSet())
                : new HashSet<>(keepIds);
        int kept = 0;
        if (existing != null) {
            for (ReminderAttachment file : existing) {
                if (keep.contains(file.getId())) {
                    kept++;
                } else {
                    attachments.delete(file);
                }
            }
        }
        storeFiles(batchId, files, kept);
    }

    private void storeFiles(Long batchId, List<MultipartFile> files, int alreadyKept) {
        List<MultipartFile> incoming = files == null ? List.of() : files.stream().filter(file -> file != null).toList();
        if (alreadyKept + incoming.size() > MAX_FILES) {
            throw new ReminderValidationException("A run can include at most 10 attachments.");
        }
        for (MultipartFile file : incoming) {
            byte[] content = read(file);
            ReminderAttachment stored = new ReminderAttachment();
            stored.setBatchId(batchId);
            stored.setFileName(fileName(file));
            stored.setContentType(contentType(file));
            stored.setContent(content);
            attachments.save(stored);
        }
    }

    private byte[] read(MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            if (content.length == 0) {
                throw new ReminderValidationException("A file is empty.");
            }
            if (content.length > MAX_FILE_BYTES) {
                throw new ReminderValidationException("Each attachment must be 8 MB or smaller.");
            }
            return content;
        } catch (IOException exception) {
            throw new ReminderValidationException("The attachment could not be read.");
        }
    }

    private Message message(String subject, String body, String cc) {
        String trimmed = subject == null ? "" : subject.trim();
        if (trimmed.isBlank()) {
            throw new ReminderValidationException("Enter a subject.");
        }
        if (trimmed.length() > 200) {
            throw new ReminderValidationException("The subject must be 200 characters or less.");
        }
        String html = QuoteEmailHtml.clean(body);
        if (QuoteEmailHtml.isBlank(html)) {
            throw new ReminderValidationException("Enter a message.");
        }
        List<String> copies = QuoteEmailAddresses.parse(cc);
        if (copies.size() > MAX_CC) {
            throw new ReminderValidationException("Enter at most 20 CC addresses.");
        }
        return new Message(trimmed, html, copies.isEmpty() ? null : String.join(", ", copies));
    }

    private String ccWithout(String cc, String recipient) {
        List<String> copies = QuoteEmailAddresses.parse(cc);
        List<String> kept = QuoteEmailAddresses.without(copies, List.of(recipient == null ? "" : recipient.toLowerCase(Locale.ROOT)));
        return kept.isEmpty() ? null : String.join(", ", kept);
    }

    private ReminderBatch unfinished(String kind) {
        List<ReminderBatch> found = batches.findByKindOrderByIdDesc(kind);
        if (found == null || found.isEmpty()) {
            return null;
        }
        List<Long> ids = found.stream().map(ReminderBatch::getId).toList();
        List<ReminderRecipient> rows = recipients.findByBatchIdIn(ids);
        Set<Long> work = new HashSet<>();
        if (rows != null) {
            for (ReminderRecipient row : rows) {
                if (ReminderRules.hasWork(row.getStatus())) {
                    work.add(row.getBatchId());
                }
            }
        }
        for (ReminderBatch batch : found) {
            if (work.contains(batch.getId())
                    && (ReminderBatch.RUNNING.equals(batch.getStatus())
                    || ReminderBatch.PAUSED.equals(batch.getStatus())
                    || ReminderBatch.STOPPED.equals(batch.getStatus()))) {
                return batch;
            }
        }
        return null;
    }

    private ReminderBatchResponse toResponse(ReminderBatch batch) {
        List<ReminderRecipient> rows = recipients.findByBatchIdOrderByIdAsc(batch.getId());
        if (rows == null) {
            rows = List.of();
        }
        List<ReminderAttachment> files = attachments.findByBatchIdOrderByIdAsc(batch.getId());
        if (files == null) {
            files = List.of();
        }
        int pending = 0;
        int sending = 0;
        int sent = 0;
        int failed = 0;
        int notConfirmed = 0;
        List<ReminderRecipientResponse> people = new ArrayList<>();
        for (ReminderRecipient row : rows) {
            switch (row.getStatus()) {
                case ReminderRecipient.PENDING -> pending++;
                case ReminderRecipient.SENDING -> sending++;
                case ReminderRecipient.SENT -> sent++;
                case ReminderRecipient.FAILED -> failed++;
                case ReminderRecipient.NOT_CONFIRMED -> notConfirmed++;
                default -> {
                }
            }
            people.add(new ReminderRecipientResponse(
                    row.getId(),
                    row.getCustomerId(),
                    row.getCustomerName(),
                    row.getContactId(),
                    row.getContactName(),
                    row.getEmail(),
                    row.getStatus(),
                    row.getErrorMessage(),
                    row.getSentAt()));
        }
        List<ReminderAttachmentResponse> fileRows = files.stream()
                .map(file -> new ReminderAttachmentResponse(file.getId(), file.getFileName(), file.getContentType()))
                .toList();
        return new ReminderBatchResponse(
                batch.getId(),
                batch.getKind(),
                batch.getSubject(),
                batch.getHtmlBody(),
                batch.getCcRecipients(),
                batch.getStatus(),
                batch.getStartedByName(),
                batch.getCreatedAt(),
                fileRows,
                people,
                new ReminderCountsResponse(pending, sending, sent, failed, notConfirmed));
    }

    private ReminderBatch requireBatch(Long id) {
        return batches.findById(id).orElseThrow(() -> new ReminderNotFoundException(id));
    }

    private ReminderRecipient requireRecipient(Long batchId, Long recipientId) {
        ReminderRecipient row = recipients.findById(recipientId)
                .orElseThrow(() -> new ReminderValidationException("That address is not in this run."));
        if (!batchId.equals(row.getBatchId())) {
            throw new ReminderValidationException("That address is not in this run.");
        }
        return row;
    }

    private static String displayName(AppUser user) {
        if (user == null) {
            return "User";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return trim(user.getDisplayName().trim(), 200);
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return trim(user.getUsername().trim(), 200);
        }
        return "User";
    }

    private static String contactName(Contact contact) {
        String name = (contact.getFirstName() == null ? "" : contact.getFirstName().trim() + " ")
                + (contact.getLastName() == null ? "" : contact.getLastName().trim());
        name = name.trim();
        if (!name.isBlank()) {
            return name;
        }
        if (contact.getContactPerson() != null && !contact.getContactPerson().isBlank()) {
            return contact.getContactPerson().trim();
        }
        return contact.getEmail();
    }

    private static String fileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "attachment";
        }
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isBlank()) {
            return "attachment";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private static String contentType(MultipartFile file) {
        String type = file.getContentType();
        if (type == null || type.isBlank()) {
            return "application/octet-stream";
        }
        type = type.trim();
        return type.length() > 255 ? type.substring(0, 255) : type;
    }

    private static String label(String kind) {
        return ReminderBatch.ANNOUNCEMENT.equals(kind) ? "announcement" : "no-lead reminder";
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private record Message(String subject, String html, String cc) {
    }
}
