package com.pnc.masters.customer.reminder;

import com.pnc.masters.contact.Contact;
import com.pnc.masters.contact.ContactRepository;
import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderBatchRequest;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderRecipientRequest;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderReviseRequest;
import com.pnc.masters.quote.mail.QuoteMailbox;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderEmailServiceTest {

    @Mock
    private ReminderBatchRepository batches;
    @Mock
    private ReminderRecipientRepository recipients;
    @Mock
    private ReminderAttachmentRepository attachments;
    @Mock
    private CustomerRepository customers;
    @Mock
    private ContactRepository contacts;
    @Mock
    private AppUserRepository users;
    @Mock
    private ReminderAudienceLookup audience;
    @Mock
    private QuoteMailbox mailbox;

    private ReminderEmailService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new ReminderEmailService(
                batches, recipients, attachments, customers, contacts, users, audience, mailbox, clock);
    }

    @Test
    void createStoresOnePendingRowPerAddressAndDoesNotSend() {
        AppUser user = new AppUser();
        user.setDisplayName("Ada Lovelace");
        Customer customer = customer(4L, "Acme");
        Contact contact = new Contact();
        contact.setContId(8L);
        contact.setEmail("pat@acme.com");
        contact.setFirstName("Pat");
        contact.setCustomer(customer);
        when(audience.customerIds(ReminderBatch.NO_LEAD)).thenReturn(Set.of(4L));
        when(users.findById(9L)).thenReturn(Optional.of(user));
        when(batches.save(any())).thenAnswer(invocation -> {
            ReminderBatch batch = invocation.getArgument(0);
            batch.setId(3L);
            return batch;
        });
        when(customers.findByCustIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(customer));
        when(contacts.findById(8L)).thenReturn(Optional.of(contact));
        when(recipients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(9L, new ReminderBatchRequest(
                ReminderBatch.NO_LEAD,
                "No leads",
                "<p>Hello</p>",
                "sales@pnc.com",
                List.of(new ReminderRecipientRequest(4L, 8L, "pat@acme.com", "Pat"))), null);

        ArgumentCaptor<ReminderBatch> batch = ArgumentCaptor.forClass(ReminderBatch.class);
        verify(batches).save(batch.capture());
        assertThat(batch.getValue().getStatus()).isEqualTo(ReminderBatch.RUNNING);
        assertThat(batch.getValue().getStartedByName()).isEqualTo("Ada Lovelace");
        assertThat(batch.getValue().getStartedByUserId()).isEqualTo(9L);
        ArgumentCaptor<ReminderRecipient> row = ArgumentCaptor.forClass(ReminderRecipient.class);
        verify(recipients).save(row.capture());
        assertThat(row.getValue().getStatus()).isEqualTo(ReminderRecipient.PENDING);
        assertThat(row.getValue().getEmail()).isEqualTo("pat@acme.com");
        verify(mailbox, never()).send(any());
    }

    @Test
    void createAcceptsAnAddressThatIsNotOnACustomer() {
        AppUser user = new AppUser();
        user.setDisplayName("Ada Lovelace");
        when(audience.customerIds(ReminderBatch.NO_LEAD)).thenReturn(Set.of());
        when(users.findById(9L)).thenReturn(Optional.of(user));
        when(batches.save(any())).thenAnswer(invocation -> {
            ReminderBatch batch = invocation.getArgument(0);
            batch.setId(3L);
            return batch;
        });
        when(recipients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(9L, new ReminderBatchRequest(
                ReminderBatch.NO_LEAD,
                "No leads",
                "<p>Hello</p>",
                "",
                List.of(new ReminderRecipientRequest(null, null, "guest@example.com", ""))), null);

        ArgumentCaptor<ReminderRecipient> row = ArgumentCaptor.forClass(ReminderRecipient.class);
        verify(recipients).save(row.capture());
        assertThat(row.getValue().getEmail()).isEqualTo("guest@example.com");
        assertThat(row.getValue().getCustomerId()).isNull();
        assertThat(row.getValue().getContactId()).isNull();
        verify(customers, never()).findByCustIdAndIsDeletedFalse(any());
    }

    @Test
    void claimNextTakesASinglePendingRow() {
        ReminderRecipient row = recipient(5L, ReminderRecipient.PENDING);
        when(recipients.findPendingOnRunningBatches(any())).thenReturn(List.of(row));
        when(recipients.claim(5L)).thenReturn(1);

        assertThat(service.claimNext()).isEqualTo(5L);
        verify(recipients).claim(5L);
    }

    @Test
    void claimNextClaimsNothingWhenAnotherWorkerAlreadyTookTheRow() {
        when(recipients.findPendingOnRunningBatches(any())).thenReturn(List.of(recipient(5L, ReminderRecipient.PENDING)));
        when(recipients.claim(5L)).thenReturn(0);

        assertThat(service.claimNext()).isNull();
    }

    @Test
    void claimNextClaimsNothingWhenNoRunningBatchHasAPendingRow() {
        when(recipients.findPendingOnRunningBatches(any())).thenReturn(List.of());

        assertThat(service.claimNext()).isNull();
        verify(recipients, never()).claim(any());
    }

    @Test
    void pauseAndStopDoNotChangeRecipientRows() {
        ReminderBatch batch = batch(ReminderBatch.RUNNING);
        when(batches.findById(3L)).thenReturn(Optional.of(batch));

        service.pause(3L);
        assertThat(batch.getStatus()).isEqualTo(ReminderBatch.PAUSED);

        service.stop(3L);
        assertThat(batch.getStatus()).isEqualTo(ReminderBatch.STOPPED);
        verify(recipients, never()).save(any());
    }

    @Test
    void sendTheRestRequeuesFailedAndLeavesSentAlone() {
        ReminderBatch batch = batch(ReminderBatch.STOPPED);
        ReminderRecipient sent = recipient(1L, ReminderRecipient.SENT);
        ReminderRecipient failed = recipient(2L, ReminderRecipient.FAILED);
        ReminderRecipient pending = recipient(3L, ReminderRecipient.PENDING);
        ReminderRecipient unconfirmed = recipient(4L, ReminderRecipient.NOT_CONFIRMED);
        when(batches.findById(3L)).thenReturn(Optional.of(batch));
        when(attachments.findByBatchIdOrderByIdAsc(3L)).thenReturn(List.of());
        when(recipients.findByBatchIdOrderByIdAsc(3L)).thenReturn(List.of(sent, failed, pending, unconfirmed));

        service.sendRest(3L, new ReminderReviseRequest("Corrected", "<p>Hello again</p>", "", List.of(), null), List.of());

        assertThat(sent.getStatus()).isEqualTo(ReminderRecipient.SENT);
        assertThat(failed.getStatus()).isEqualTo(ReminderRecipient.PENDING);
        assertThat(failed.getErrorMessage()).isNull();
        assertThat(pending.getStatus()).isEqualTo(ReminderRecipient.PENDING);
        assertThat(unconfirmed.getStatus()).isEqualTo(ReminderRecipient.NOT_CONFIRMED);
        assertThat(batch.getStatus()).isEqualTo(ReminderBatch.RUNNING);
        assertThat(batch.getSubject()).isEqualTo("Corrected");
        verify(mailbox, never()).send(any());
    }

    @Test
    void sendAgainRequeuesSentRowsOnlyAfterConfirmation() {
        ReminderBatch batch = batch(ReminderBatch.STOPPED);
        ReminderRecipient sent = recipient(1L, ReminderRecipient.SENT);
        ReminderRecipient failed = recipient(2L, ReminderRecipient.FAILED);
        when(batches.findById(3L)).thenReturn(Optional.of(batch));
        when(attachments.findByBatchIdOrderByIdAsc(3L)).thenReturn(List.of());
        when(recipients.findByBatchIdOrderByIdAsc(3L)).thenReturn(List.of(sent, failed));

        assertThatThrownBy(() -> service.sendAgain(
                3L, new ReminderReviseRequest("Corrected", "<p>Hello</p>", "", List.of(), false), List.of()))
                .isInstanceOf(ReminderValidationException.class);

        service.sendAgain(3L, new ReminderReviseRequest("Corrected", "<p>Hello</p>", "", List.of(), true), List.of());

        assertThat(sent.getStatus()).isEqualTo(ReminderRecipient.PENDING);
        assertThat(failed.getStatus()).isEqualTo(ReminderRecipient.FAILED);
    }

    @Test
    void releaseStuckMarksSendingAsNotConfirmed() {
        ReminderRecipient sending = recipient(7L, ReminderRecipient.SENDING);
        ReminderRecipient pending = recipient(8L, ReminderRecipient.PENDING);
        when(recipients.findByStatus(ReminderRecipient.SENDING)).thenReturn(List.of(sending));

        service.releaseStuck();

        assertThat(sending.getStatus()).isEqualTo(ReminderRecipient.NOT_CONFIRMED);
        assertThat(pending.getStatus()).isEqualTo(ReminderRecipient.PENDING);
        verify(recipients).save(sending);
        verify(recipients, never()).save(pending);
    }

    @Test
    void aSentNoLeadAddressIncrementsFollowUpOncePerCustomer() {
        ReminderBatch batch = batch(ReminderBatch.RUNNING);
        batch.setKind(ReminderBatch.NO_LEAD);
        batch.setStartedByName("Ada Lovelace");
        ReminderRecipient row = sendingRow();
        Customer customer = customer(4L, "Acme");
        customer.setFollowupCount(1);
        when(recipients.findById(8L)).thenReturn(Optional.of(row));
        when(batches.findById(3L)).thenReturn(Optional.of(batch));
        when(recipients.countByBatchIdAndCustomerIdAndStatusAndIdNot(3L, 4L, ReminderRecipient.SENT, 8L)).thenReturn(0L);
        when(customers.findById(4L)).thenReturn(Optional.of(customer));

        service.dispatch(8L);

        assertThat(row.getStatus()).isEqualTo(ReminderRecipient.SENT);
        assertThat(customer.getFollowupCount()).isEqualTo(2);
        assertThat(customer.getFollowupBy()).isEqualTo("Ada Lovelace");
        assertThat(customer.getLastReminder()).isEqualTo(LocalDateTime.of(2026, 10, 22, 10, 0));
        assertThat(customer.getAnnouncementFollowupCount()).isZero();
        verify(mailbox).send(any());
    }

    @Test
    void aSecondSentAddressForTheSameCustomerDoesNotCountAgain() {
        ReminderBatch batch = batch(ReminderBatch.RUNNING);
        batch.setKind(ReminderBatch.NO_LEAD);
        ReminderRecipient row = sendingRow();
        Customer customer = customer(4L, "Acme");
        customer.setFollowupCount(2);
        when(recipients.findById(8L)).thenReturn(Optional.of(row));
        when(batches.findById(3L)).thenReturn(Optional.of(batch));
        when(recipients.countByBatchIdAndCustomerIdAndStatusAndIdNot(3L, 4L, ReminderRecipient.SENT, 8L)).thenReturn(1L);
        when(customers.findById(4L)).thenReturn(Optional.of(customer));

        service.dispatch(8L);

        assertThat(customer.getFollowupCount()).isEqualTo(2);
        assertThat(customer.getLastReminder()).isEqualTo(LocalDateTime.of(2026, 10, 22, 10, 0));
    }

    @Test
    void aFailedSendDoesNotIncrementFollowUp() {
        ReminderBatch batch = batch(ReminderBatch.RUNNING);
        ReminderRecipient row = sendingRow();
        Customer customer = customer(4L, "Acme");
        when(recipients.findById(8L)).thenReturn(Optional.of(row));
        when(batches.findById(3L)).thenReturn(Optional.of(batch));
        org.mockito.Mockito.doThrow(new IllegalStateException("Mailbox down")).when(mailbox).send(any());

        service.dispatch(8L);

        assertThat(row.getStatus()).isEqualTo(ReminderRecipient.FAILED);
        assertThat(row.getErrorMessage()).isEqualTo("Mailbox down");
        assertThat(customer.getFollowupCount()).isZero();
        verify(customers, never()).save(any());
    }

    @Test
    void aSentAnnouncementUpdatesAnnouncementColumnsOnly() {
        ReminderBatch batch = batch(ReminderBatch.RUNNING);
        batch.setKind(ReminderBatch.ANNOUNCEMENT);
        ReminderRecipient row = sendingRow();
        Customer customer = customer(4L, "Acme");
        customer.setFollowupCount(4);
        when(recipients.findById(8L)).thenReturn(Optional.of(row));
        when(batches.findById(3L)).thenReturn(Optional.of(batch));
        when(recipients.countByBatchIdAndCustomerIdAndStatusAndIdNot(3L, 4L, ReminderRecipient.SENT, 8L)).thenReturn(0L);
        when(customers.findById(4L)).thenReturn(Optional.of(customer));

        service.dispatch(8L);

        assertThat(customer.getAnnouncementFollowupCount()).isEqualTo(1);
        assertThat(customer.getLastAnnouncement()).isEqualTo(LocalDateTime.of(2026, 10, 22, 10, 0));
        assertThat(customer.getFollowupCount()).isEqualTo(4);
        assertThat(customer.getLastReminder()).isNull();
        assertThat(customer.getFollowupBy()).isNull();
    }

    @Test
    void logReturnsOneRowPerAddressWithTheBatchSubject() {
        ReminderBatch batch = batch(ReminderBatch.STOPPED);
        batch.setCcRecipients("copy@pnc.com");
        batch.setCreatedAt(LocalDateTime.of(2026, 10, 22, 10, 0));
        ReminderRecipient row = recipient(8L, ReminderRecipient.SENT);
        row.setCustomerName("Acme");
        row.setSentAt(LocalDateTime.of(2026, 10, 22, 10, 5));
        when(recipients.searchLog(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.<Object[]>of(new Object[] { row, batch })));

        var page = service.log(null, "all", 0, 20);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).email()).isEqualTo("pat@acme.com");
        assertThat(page.items().get(0).customerName()).isEqualTo("Acme");
        assertThat(page.items().get(0).subject()).isEqualTo("Subject");
        assertThat(page.items().get(0).kind()).isEqualTo("no-lead");
        assertThat(page.items().get(0).status()).isEqualTo("sent");
    }

    private ReminderBatch batch(String status) {
        ReminderBatch batch = new ReminderBatch();
        batch.setId(3L);
        batch.setKind(ReminderBatch.NO_LEAD);
        batch.setStatus(status);
        batch.setSubject("Subject");
        batch.setHtmlBody("<p>Hello</p>");
        batch.setStartedByName("Ada Lovelace");
        return batch;
    }

    private ReminderRecipient recipient(Long id, String status) {
        ReminderRecipient row = new ReminderRecipient();
        row.setId(id);
        row.setBatchId(3L);
        row.setCustomerId(4L);
        row.setStatus(status);
        row.setEmail("pat@acme.com");
        row.setErrorMessage(ReminderRecipient.FAILED.equals(status) ? "nope" : null);
        return row;
    }

    private ReminderRecipient sendingRow() {
        ReminderRecipient row = recipient(8L, ReminderRecipient.SENDING);
        row.setContactId(8L);
        return row;
    }

    private static Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCustomer(name);
        return customer;
    }
}
