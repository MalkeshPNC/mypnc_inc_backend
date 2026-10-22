package com.pnc.masters.quote.api;

import com.pnc.masters.quote.mail.QuoteEmailService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteEmailController {

    private final QuoteEmailService emails;

    public QuoteEmailController(QuoteEmailService emails) {
        this.emails = emails;
    }

    @PostMapping(value = "/{id}/email-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuoteEmailPdfResponse storePdf(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws IOException {
        return emails.store(id, file.getBytes());
    }

    @GetMapping(value = "/{id}/email-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> openPdf(@PathVariable Long id) {
        QuoteEmailPdfFile pdf = emails.open(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(pdf.fileName()).build());
        headers.setContentLength(pdf.content().length);
        return ResponseEntity.ok().headers(headers).body(pdf.content());
    }

    @PostMapping("/{id}/email")
    public ResponseEntity<Void> enqueue(@PathVariable Long id,
                                        @RequestBody QuoteEmailRequest request,
                                        Authentication authentication) {
        emails.enqueue(id, (Long) authentication.getPrincipal(), request);
        return ResponseEntity.accepted().build();
    }
}
