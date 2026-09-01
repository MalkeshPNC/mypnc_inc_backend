package com.pnc.masters.document.api;

import com.pnc.masters.document.Document;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.document.application.DocumentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentStorage storage;

    public DocumentController(DocumentService documentService, DocumentStorage storage) {
        this.documentService = documentService;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam String category,
            @RequestParam(required = false) Long customerId,
            @RequestParam MultipartFile file) {
        DocumentResponse response = documentService.upload(category, customerId, file);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.documentId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<DocumentResponse> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String category) {
        return documentService.list(customerId, category);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) throws IOException {
        Document document = documentService.getActive(id);
        InputStreamResource resource = new InputStreamResource(storage.open(document.getStorageKey()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(document.getOriginalFilename()).build());
        headers.setContentLength(document.getFileSize());
        headers.setContentType(MediaType.parseMediaType(document.getContentType()));
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
