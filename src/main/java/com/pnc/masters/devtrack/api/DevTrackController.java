package com.pnc.masters.devtrack.api;

import com.pnc.masters.devtrack.DevTrackFile;
import com.pnc.masters.devtrack.application.DevTrackService;
import com.pnc.masters.document.DocumentStorage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/development-track")
public class DevTrackController {

    private final DevTrackService tracks;
    private final DocumentStorage storage;

    public DevTrackController(DevTrackService tracks, DocumentStorage storage) {
        this.tracks = tracks;
        this.storage = storage;
    }

    @GetMapping
    public List<DevTrackResponse> list() {
        return tracks.list();
    }

    @GetMapping("/{id}")
    public DevTrackResponse get(@PathVariable Long id) {
        return tracks.get(id);
    }

    @PostMapping
    public ResponseEntity<DevTrackResponse> create(@RequestBody DevTrackWriteRequest request,
                                                   Authentication authentication) {
        DevTrackResponse response = tracks.create(userId(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.trackId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public DevTrackResponse update(@PathVariable Long id,
                                   @RequestBody DevTrackWriteRequest request,
                                   Authentication authentication) {
        return tracks.update(id, userId(authentication), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        tracks.delete(id, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public DevTrackResponse comment(@PathVariable Long id,
                                    @RequestBody DevTrackCommentRequest request,
                                    Authentication authentication) {
        return tracks.comment(id, userId(authentication), request);
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DevTrackResponse upload(@PathVariable Long id,
                                   @RequestParam MultipartFile file,
                                   Authentication authentication) {
        return tracks.addFile(id, userId(authentication), file);
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public DevTrackResponse deleteFile(@PathVariable Long id,
                                       @PathVariable Long fileId,
                                       Authentication authentication) {
        return tracks.deleteFile(id, userId(authentication), fileId);
    }

    @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id,
                                                        @PathVariable Long fileId) throws IOException {
        DevTrackFile file = tracks.openFile(id, fileId);
        MediaType type;
        try {
            type = MediaType.parseMediaType(file.getContentType());
        } catch (InvalidMediaTypeException exception) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getOriginalFilename())
                        .build()
                        .toString())
                .contentType(type)
                .contentLength(file.getFileSize())
                .body(new InputStreamResource(storage.open(file.getStorageKey())));
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
