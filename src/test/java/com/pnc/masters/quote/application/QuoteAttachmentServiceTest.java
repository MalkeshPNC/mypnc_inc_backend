package com.pnc.masters.quote.application;

import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteAttachment;
import com.pnc.masters.quote.QuoteAttachmentRepository;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteAttachmentServiceTest {

    @Mock
    private QuoteRepository quotes;
    @Mock
    private QuoteAttachmentRepository attachments;
    @Mock
    private DocumentStorage storage;
    @Mock
    private MultipartFile file;

    private QuoteAttachmentService service;

    @BeforeEach
    void setUp() {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxFileSize(10 * 1024 * 1024);
        service = new QuoteAttachmentService(quotes, attachments, storage, properties);
    }

    @Test
    void uploadStoresTheFileUnderTheQuoteFolder() throws Exception {
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        when(file.getOriginalFilename()).thenReturn("notes.pdf");
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("notes".getBytes()));
        when(storage.store(eq("quotes/4/p_notes_notes.pdf"), any()))
                .thenReturn(new DocumentStorage.StoredDocument(5, "abc"));
        when(attachments.findByQidAndCategoryAndOriginalFilenameIgnoreCase(4L, "p_notes", "notes.pdf"))
                .thenReturn(Optional.empty());
        when(attachments.save(any())).thenAnswer(invocation -> {
            QuoteAttachment row = invocation.getArgument(0);
            row.setAttachmentId(8L);
            return row;
        });

        var response = service.upload(4L, "p_notes", file, 7L);

        assertThat(response.attachmentId()).isEqualTo(8L);
        assertThat(response.category()).isEqualTo("p_notes");
        assertThat(response.originalFilename()).isEqualTo("notes.pdf");
        assertThat(response.generated()).isFalse();
    }

    @Test
    void uploadReplacesAFileWithTheSameName() throws Exception {
        QuoteAttachment existing = new QuoteAttachment();
        existing.setAttachmentId(8L);
        existing.setQid(4L);
        existing.setOriginalFilename("notes.pdf");
        existing.setGenerated(true);
        existing.setCategory("RFQ");
        existing.setStorageKey("quotes/4/notes.pdf");
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        when(file.getOriginalFilename()).thenReturn("notes.pdf");
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("notes".getBytes()));
        when(storage.store(eq("quotes/4/RFQ_notes.pdf"), any()))
                .thenReturn(new DocumentStorage.StoredDocument(5, "abc"));
        when(attachments.findByQidAndCategoryAndOriginalFilenameIgnoreCase(4L, "RFQ", "notes.pdf"))
                .thenReturn(Optional.of(existing));
        when(attachments.save(existing)).thenReturn(existing);

        service.upload(4L, "RFQ", file, 7L);

        assertThat(existing.getCategory()).isEqualTo("RFQ");
        assertThat(existing.getStorageKey()).isEqualTo("quotes/4/RFQ_notes.pdf");
        assertThat(existing.isGenerated()).isFalse();
        assertThat(existing.getAttachmentId()).isEqualTo(8L);
        verify(storage).delete("quotes/4/notes.pdf");
    }

    @Test
    void uploadRejectsAFileTypeOutsideTheList() {
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        when(file.getOriginalFilename()).thenReturn("notes.exe");

        assertThatThrownBy(() -> service.upload(4L, "p_notes", file, 7L))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessageContaining("PDF");
        verify(attachments, never()).save(any());
    }

    @Test
    void storeGeneratedPdfKeepsOneQuoteRow() throws Exception {
        Quote quote = quote();
        QuoteAttachment previous = new QuoteAttachment();
        previous.setAttachmentId(3L);
        previous.setOriginalFilename("Quote-OLD.pdf");
        previous.setStorageKey("quotes/4/Quote-OLD.pdf");
        previous.setGenerated(true);
        when(attachments.findByQidAndGeneratedTrue(4L)).thenReturn(List.of(previous));
        when(attachments.findByQidAndCategoryAndOriginalFilenameIgnoreCase(4L, "quote", "Quote-Q-100.pdf"))
                .thenReturn(Optional.empty());
        when(attachments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.storeGeneratedPdf(quote, "%PDF-1.4".getBytes());

        verify(storage).store(eq("quotes/4/quote_Quote-Q-100.pdf"), any());
        verify(storage).delete("quotes/4/Quote-OLD.pdf");
        verify(attachments).delete(previous);
        ArgumentCaptor<QuoteAttachment> saved = ArgumentCaptor.forClass(QuoteAttachment.class);
        verify(attachments).save(saved.capture());
        assertThat(saved.getValue().getCategory()).isEqualTo("quote");
        assertThat(saved.getValue().isGenerated()).isTrue();
        assertThat(saved.getValue().getOriginalFilename()).isEqualTo("Quote-Q-100.pdf");
    }

    @Test
    void deleteRemovesAnUploadedFile() throws Exception {
        QuoteAttachment row = new QuoteAttachment();
        row.setAttachmentId(8L);
        row.setStorageKey("quotes/4/notes.pdf");
        row.setGenerated(false);
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        when(attachments.findByAttachmentIdAndQid(8L, 4L)).thenReturn(Optional.of(row));

        service.delete(4L, 8L);

        verify(storage).delete("quotes/4/notes.pdf");
        verify(attachments).delete(row);
    }

    @Test
    void deleteLeavesTheGeneratedQuotePdf() {
        QuoteAttachment row = new QuoteAttachment();
        row.setAttachmentId(8L);
        row.setGenerated(true);
        when(quotes.findByQidAndIsDeletedFalse(4L)).thenReturn(Optional.of(quote()));
        when(attachments.findByAttachmentIdAndQid(8L, 4L)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.delete(4L, 8L))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessageContaining("cannot be deleted");
        verify(attachments, never()).delete(any());
    }

    private static Quote quote() {
        Quote quote = new Quote();
        quote.setQid(4L);
        quote.setQuoteNumber("Q-100");
        return quote;
    }
}
