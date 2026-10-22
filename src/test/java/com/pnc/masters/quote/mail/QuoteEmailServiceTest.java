package com.pnc.masters.quote.mail;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteEmailRequest;
import com.pnc.masters.quote.api.QuoteValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.file.NoSuchFileException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteEmailServiceTest {

    @Mock
    private QuoteRepository quotes;
    @Mock
    private QuoteEmailOutboxRepository outbox;
    @Mock
    private DocumentStorage storage;
    @Mock
    private QuoteMailbox mailbox;
    @Mock
    private AppNotifier notifier;

    private QuoteEmailService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);
        service = new QuoteEmailService(quotes, outbox, storage, mailbox, notifier, clock);
    }

    @Test
    void storesThePreviewUnderTheQuoteFolder() throws Exception {
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));

        var response = service.store(4L, "%PDF-1.4 preview".getBytes());

        assertThat(response.fileName()).isEqualTo("Quote-Q-100.pdf");
        verify(storage).store(eq("quote-pdf/Q-100/Quote-Q-100.pdf"), any());
    }

    @Test
    void rejectsAPdfOverEightMegabytes() {
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        byte[] pdf = new byte[QuoteEmailService.MAX_PDF_BYTES + 1];
        pdf[0] = '%';
        pdf[1] = 'P';
        pdf[2] = 'D';
        pdf[3] = 'F';

        assertThatThrownBy(() -> service.store(4L, pdf))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessage("The quote PDF must be 8 MB or smaller.");
    }

    @Test
    void queuesACopyOfTheStoredPdf() throws Exception {
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        byte[] stored = "%PDF-1.4 stored".getBytes();
        when(storage.open("quote-pdf/Q-100/Quote-Q-100.pdf")).thenReturn(new ByteArrayInputStream(stored));
        when(outbox.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueue(4L, 9L, new QuoteEmailRequest(
                "buyer@example.com",
                "sales@example.com",
                "A100 Q-100",
                "<p>Hello</p><script>alert(1)</script>"));

        ArgumentCaptor<QuoteEmailOutbox> saved = ArgumentCaptor.forClass(QuoteEmailOutbox.class);
        verify(outbox).save(saved.capture());
        QuoteEmailOutbox row = saved.getValue();
        assertThat(row.getStatus()).isEqualTo(QuoteEmailOutbox.QUEUED);
        assertThat(row.getUserId()).isEqualTo(9L);
        assertThat(row.getRecipients()).isEqualTo("buyer@example.com");
        assertThat(row.getCcRecipients()).isEqualTo("sales@example.com");
        assertThat(row.getAttachmentName()).isEqualTo("Quote-Q-100.pdf");
        assertThat(row.getPdfContent()).containsExactly(stored);
        assertThat(row.getHtmlBody()).doesNotContain("script");
        assertThat(row.getHtmlBody()).contains("Hello");
    }

    @Test
    void rejectsASendWhenTheStoredPdfIsMissing() throws Exception {
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        when(storage.open("quote-pdf/Q-100/Quote-Q-100.pdf")).thenThrow(new NoSuchFileException("missing"));

        assertThatThrownBy(() -> service.enqueue(4L, 9L, new QuoteEmailRequest(
                "buyer@example.com", "", "A100 Q-100", "<p>Hello</p>")))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessage("Save the quote PDF before sending the email.");
        verify(outbox, never()).save(any());
    }

    @Test
    void marksAClaimedMessageSentAndClearsThePdfCopy() {
        QuoteEmailOutbox row = sending();
        when(outbox.findById(7L)).thenReturn(Optional.of(row));
        when(outbox.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.dispatch(7L);

        assertThat(row.getStatus()).isEqualTo(QuoteEmailOutbox.SENT);
        assertThat(row.getPdfContent()).isNull();
        verify(mailbox).send(row);
        verify(notifier).notifyUsers(
                org.mockito.ArgumentMatchers.anyCollection(),
                eq("quote-email"),
                eq("Quote email sent"),
                eq("Sent: A100 Q-100"),
                eq("/quote-systems/quotes/4/pdf"));
    }

    @Test
    void reportsAFailedSendWhenMailIsNotConfigured() {
        QuoteEmailOutbox row = sending();
        when(outbox.findById(7L)).thenReturn(Optional.of(row));
        when(outbox.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new IllegalStateException("Mail is not configured.")).when(mailbox).send(row);

        service.dispatch(7L);

        assertThat(row.getStatus()).isEqualTo(QuoteEmailOutbox.FAILED);
        assertThat(row.getPdfContent()).isNull();
        assertThat(row.getErrorMessage()).isEqualTo("Mail is not configured.");
        verify(notifier).notifyUsers(
                org.mockito.ArgumentMatchers.anyCollection(),
                eq("quote-email"),
                eq("Quote email failed"),
                eq("Mail is not configured."),
                eq("/quote-systems/quotes/4/pdf"));
    }

    private static Quote quote() {
        Quote quote = new Quote();
        quote.setQid(4L);
        quote.setQuoteNumber("Q-100");
        return quote;
    }

    private static QuoteEmailOutbox sending() {
        QuoteEmailOutbox row = new QuoteEmailOutbox();
        row.setId(7L);
        row.setQuoteId(4L);
        row.setUserId(9L);
        row.setStatus(QuoteEmailOutbox.SENDING);
        row.setSubject("A100 Q-100");
        row.setPdfContent(new byte[] {1, 2, 3});
        return row;
    }
}
