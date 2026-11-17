package com.pnc.masters.quote.api;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.quote.QuoteCommentImage;
import com.pnc.masters.quote.application.QuoteCommentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes/{qid}/comments")
public class QuoteCommentController {

    private final QuoteCommentService comments;
    private final DocumentStorage storage;

    public QuoteCommentController(QuoteCommentService comments, DocumentStorage storage) {
        this.comments = comments;
        this.storage = storage;
    }

    @GetMapping
    public List<QuoteCommentResponse> list(@PathVariable Long qid) {
        return comments.list(qid);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuoteCommentResponse create(@PathVariable Long qid,
                                       @RequestParam(required = false) String body,
                                       @RequestParam(required = false) Long replyToCommentId,
                                       @RequestParam(required = false) List<Long> mentionUserIds,
                                       @RequestParam(required = false) List<MultipartFile> images,
                                       Authentication authentication) {
        return comments.create(qid, userId(authentication), body, replyToCommentId, mentionUserIds, images);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<InputStreamResource> image(@PathVariable Long qid, @PathVariable Long imageId) throws IOException {
        QuoteCommentImage image = comments.image(qid, imageId);
        InputStream input = storage.open(image.getStorageKey());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, image.getContentType())
                .contentLength(image.getFileSize())
                .body(new InputStreamResource(input));
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
