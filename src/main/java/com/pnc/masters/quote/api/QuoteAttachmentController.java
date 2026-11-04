package com.pnc.masters.quote.api;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.quote.QuoteAttachment;
import com.pnc.masters.quote.application.QuoteAttachmentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes/{qid}/attachments")
public class QuoteAttachmentController {

    private final QuoteAttachmentService attachments;
    private final DocumentStorage storage;

    public QuoteAttachmentController(QuoteAttachmentService attachments, DocumentStorage storage) {
        this.attachments = attachments;
        this.storage = storage;
    }

    @GetMapping
    public List<QuoteAttachmentResponse> list(@PathVariable Long qid) {
        return attachments.list(qid);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuoteAttachmentResponse upload(
            @PathVariable Long qid,
            @RequestParam String category,
            @RequestParam MultipartFile file,
            Authentication authentication
    ) {
        return attachments.upload(qid, category, file, userId(authentication));
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<InputStreamResource> content(
            @PathVariable Long qid,
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean download
    ) throws IOException {
        QuoteAttachment row = attachments.open(qid, attachmentId);
        boolean inline = !download && QuoteAttachmentService.inline(row.getContentType());
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(row.getOriginalFilename())
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentLength(row.getFileSize());
        headers.setContentType(MediaType.parseMediaType(row.getContentType()));
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(storage.open(row.getStorageKey())));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long qid, @PathVariable Long attachmentId) {
        attachments.delete(qid, attachmentId);
        return ResponseEntity.noContent().build();
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
