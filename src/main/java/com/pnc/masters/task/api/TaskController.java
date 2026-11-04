package com.pnc.masters.task.api;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.task.TaskFile;
import com.pnc.masters.task.application.TaskService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService tasks;
    private final DocumentStorage storage;

    public TaskController(TaskService tasks, DocumentStorage storage) {
        this.tasks = tasks;
        this.storage = storage;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) Long qid) {
        return qid == null ? tasks.list() : tasks.listByQuote(qid);
    }

    @GetMapping("/people")
    public List<TaskPersonResponse> people() {
        return tasks.people();
    }

    @GetMapping("/options")
    public TaskOptionsResponse options() {
        return tasks.options();
    }

    @PostMapping("/{id}/progress")
    public TaskResponse progress(@PathVariable Long id,
                                 @RequestBody TaskProgressRequest request,
                                 Authentication authentication) {
        return tasks.progress(id, userId(authentication), request);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@RequestBody TaskCreateRequest request, Authentication authentication) {
        TaskResponse response = tasks.create(userId(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.taskId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @RequestBody TaskUpdateRequest request,
                               Authentication authentication) {
        return tasks.update(id, userId(authentication), request);
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaskFileResponse upload(@PathVariable Long id,
                                   @RequestParam MultipartFile file,
                                   Authentication authentication) {
        return tasks.addFile(id, userId(authentication), file);
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id,
                                           @PathVariable Long fileId,
                                           Authentication authentication) {
        tasks.deleteFile(id, userId(authentication), fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/logs")
    public List<TaskLogResponse> logs(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new TaskForbiddenException("Only an administrator can view task logs");
        }
        return tasks.logs(id);
    }

    @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, @PathVariable Long fileId) throws IOException {
        TaskFile file = tasks.openFile(id, fileId);
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

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
                return true;
            }
        }
        return false;
    }
}
