package com.pnc.masters.devtrack.application;

import com.pnc.masters.devtrack.DevTrack;
import com.pnc.masters.devtrack.DevTrackComment;
import com.pnc.masters.devtrack.DevTrackCommentRepository;
import com.pnc.masters.devtrack.DevTrackFile;
import com.pnc.masters.devtrack.DevTrackFileRepository;
import com.pnc.masters.devtrack.DevTrackRepository;
import com.pnc.masters.devtrack.api.DevTrackCommentRequest;
import com.pnc.masters.devtrack.api.DevTrackCommentResponse;
import com.pnc.masters.devtrack.api.DevTrackFileResponse;
import com.pnc.masters.devtrack.api.DevTrackForbiddenException;
import com.pnc.masters.devtrack.api.DevTrackNotFoundException;
import com.pnc.masters.devtrack.api.DevTrackResponse;
import com.pnc.masters.devtrack.api.DevTrackValidationException;
import com.pnc.masters.devtrack.api.DevTrackWriteRequest;
import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class DevTrackService {

    private static final Set<String> STATUSES = Set.of(DevTrack.CREATED, DevTrack.IN_PROCESS, DevTrack.DONE);
    private static final int NAME_LIMIT = 200;
    private static final int DESCRIPTION_LIMIT = 20_000;
    private static final int COMMENT_LIMIT = 20_000;

    private final DevTrackRepository tracks;
    private final DevTrackFileRepository files;
    private final DevTrackCommentRepository comments;
    private final AppUserRepository users;
    private final DocumentStorage storage;
    private final DocumentProperties documents;
    private final Clock clock;

    public DevTrackService(DevTrackRepository tracks,
                           DevTrackFileRepository files,
                           DevTrackCommentRepository comments,
                           AppUserRepository users,
                           DocumentStorage storage,
                           DocumentProperties documents,
                           Clock clock) {
        this.tracks = tracks;
        this.files = files;
        this.comments = comments;
        this.users = users;
        this.storage = storage;
        this.documents = documents;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DevTrackResponse> list() {
        List<DevTrack> rows = tracks.findByDeletedFalseOrderByCreatedAtDescTrackIdDesc();
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        Set<Long> userIds = new HashSet<>();
        for (DevTrack row : rows) {
            ids.add(row.getTrackId());
            userIds.add(row.getCreatedBy());
        }
        Map<Long, List<DevTrackFile>> filesByTrack = new HashMap<>();
        for (DevTrackFile file : files.findByTrackIdInOrderByFileIdAsc(ids)) {
            filesByTrack.computeIfAbsent(file.getTrackId(), key -> new ArrayList<>()).add(file);
        }
        Map<Long, List<DevTrackComment>> commentsByTrack = new HashMap<>();
        for (DevTrackComment comment : comments.findByTrackIdInOrderByCommentIdAsc(ids)) {
            commentsByTrack.computeIfAbsent(comment.getTrackId(), key -> new ArrayList<>()).add(comment);
            userIds.add(comment.getUserId());
        }
        Map<Long, String> names = names(userIds);
        List<DevTrackResponse> responses = new ArrayList<>();
        for (DevTrack row : rows) {
            responses.add(response(
                    row,
                    names,
                    filesByTrack.getOrDefault(row.getTrackId(), List.of()),
                    commentsByTrack.getOrDefault(row.getTrackId(), List.of())));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public DevTrackResponse get(Long trackId) {
        return response(requireTrack(trackId));
    }

    public DevTrackResponse create(Long userId, DevTrackWriteRequest request) {
        DevTrack row = new DevTrack();
        apply(row, request);
        LocalDateTime now = LocalDateTime.now(clock);
        row.setCreatedBy(userId);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setDeleted(false);
        return response(tracks.save(row));
    }

    public void delete(Long trackId, Long userId) {
        DevTrack row = requireTrack(trackId);
        requireCreator(row, userId);
        row.setDeleted(true);
        row.setUpdatedAt(LocalDateTime.now(clock));
        tracks.save(row);
    }

    public DevTrackResponse update(Long trackId, Long userId, DevTrackWriteRequest request) {
        DevTrack row = requireTrack(trackId);
        requireCreator(row, userId);
        apply(row, request);
        row.setUpdatedAt(LocalDateTime.now(clock));
        return response(tracks.save(row));
    }

    public DevTrackResponse comment(Long trackId, Long userId, DevTrackCommentRequest request) {
        DevTrack row = requireTrack(trackId);
        String text = clean(request == null ? null : request.comment(), COMMENT_LIMIT, "Comment");
        if (text == null) {
            throw new DevTrackValidationException("Comment is required");
        }
        DevTrackComment comment = new DevTrackComment();
        comment.setTrackId(trackId);
        comment.setUserId(userId);
        comment.setComment(text);
        comment.setCreatedAt(LocalDateTime.now(clock));
        comments.save(comment);
        row.setUpdatedAt(LocalDateTime.now(clock));
        tracks.save(row);
        return response(row);
    }

    public DevTrackResponse addFile(Long trackId, Long userId, MultipartFile file) {
        DevTrack row = requireTrack(trackId);
        requireCreator(row, userId);
        if (file == null || file.isEmpty()) {
            throw new DevTrackValidationException("Choose a file");
        }
        if (file.getSize() > documents.getMaxFileSize()) {
            throw new DevTrackValidationException("The file is larger than 10 MB.");
        }
        String original = displayName(file.getOriginalFilename());
        DevTrackFile storedFile = new DevTrackFile();
        storedFile.setTrackId(trackId);
        storedFile.setOriginalFilename(original);
        storedFile.setContentType(contentType(file));
        storedFile.setFileSize(file.getSize());
        storedFile.setStorageKey("pending");
        storedFile.setCreatedAt(LocalDateTime.now(clock));
        storedFile = files.saveAndFlush(storedFile);
        String key = storageKey(trackId, storedFile.getFileId(), original);
        try (InputStream input = file.getInputStream()) {
            DocumentStorage.StoredDocument stored = storage.store(key, input);
            storedFile.setStorageKey(key);
            storedFile.setFileSize(stored.size());
            files.save(storedFile);
            row.setUpdatedAt(LocalDateTime.now(clock));
            tracks.save(row);
            return response(row);
        } catch (IOException | RuntimeException exception) {
            files.delete(storedFile);
            try {
                storage.delete(key);
            } catch (IOException cleanup) {
                exception.addSuppressed(cleanup);
            }
            if (exception instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unable to store the file", exception);
        }
    }

    public DevTrackResponse deleteFile(Long trackId, Long userId, Long fileId) {
        DevTrack row = requireTrack(trackId);
        requireCreator(row, userId);
        DevTrackFile file = openFile(trackId, fileId);
        try {
            storage.delete(file.getStorageKey());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete the file", exception);
        }
        files.delete(file);
        row.setUpdatedAt(LocalDateTime.now(clock));
        tracks.save(row);
        return response(row);
    }

    @Transactional(readOnly = true)
    public DevTrackFile openFile(Long trackId, Long fileId) {
        requireTrack(trackId);
        DevTrackFile file = files.findById(fileId)
                .orElseThrow(() -> new DevTrackNotFoundException("That file was not found"));
        if (!trackId.equals(file.getTrackId())) {
            throw new DevTrackNotFoundException("That file was not found");
        }
        return file;
    }

    private DevTrackResponse response(DevTrack row) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(row.getCreatedBy());
        List<DevTrackComment> commentRows = comments.findByTrackIdOrderByCommentIdAsc(row.getTrackId());
        for (DevTrackComment comment : commentRows) {
            userIds.add(comment.getUserId());
        }
        return response(
                row,
                names(userIds),
                files.findByTrackIdOrderByFileIdAsc(row.getTrackId()),
                commentRows);
    }

    private DevTrackResponse response(DevTrack row,
                                      Map<Long, String> names,
                                      List<DevTrackFile> fileRows,
                                      List<DevTrackComment> commentRows) {
        List<DevTrackFileResponse> fileResponses = new ArrayList<>();
        for (DevTrackFile file : fileRows) {
            fileResponses.add(new DevTrackFileResponse(file.getFileId(), file.getOriginalFilename(), file.getFileSize()));
        }
        List<DevTrackCommentResponse> commentResponses = new ArrayList<>();
        for (DevTrackComment comment : commentRows) {
            commentResponses.add(new DevTrackCommentResponse(
                    comment.getCommentId(),
                    comment.getUserId(),
                    name(names, comment.getUserId()),
                    comment.getComment(),
                    comment.getCreatedAt()));
        }
        return new DevTrackResponse(
                row.getTrackId(),
                row.getName(),
                row.getDescription() == null ? "" : row.getDescription(),
                row.getEta(),
                row.getStatus(),
                row.getStatusUpdatedAt(),
                row.getCreatedBy(),
                name(names, row.getCreatedBy()),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                fileResponses,
                commentResponses);
    }

    private void apply(DevTrack row, DevTrackWriteRequest request) {
        if (request == null) {
            throw new DevTrackValidationException("Task name is required");
        }
        String name = clean(request.name(), NAME_LIMIT, "Task name");
        if (name == null) {
            throw new DevTrackValidationException("Task name is required");
        }
        if (request.eta() == null) {
            throw new DevTrackValidationException("ETA is required");
        }
        String status = request.status() == null ? "" : request.status().trim().toUpperCase(Locale.ROOT);
        if (status.isEmpty()) {
            status = DevTrack.CREATED;
        }
        if (!STATUSES.contains(status)) {
            throw new DevTrackValidationException("Status must be Created, In-Process, or Done");
        }
        row.setName(name);
        row.setDescription(clean(request.description(), DESCRIPTION_LIMIT, "Description"));
        row.setEta(request.eta());
        if (!status.equals(row.getStatus())) {
            row.setStatusUpdatedAt(LocalDateTime.now(clock));
        }
        row.setStatus(status);
    }

    private DevTrack requireTrack(Long trackId) {
        DevTrack row = tracks.findById(trackId).orElseThrow(() -> new DevTrackNotFoundException(trackId));
        if (row.isDeleted()) {
            throw new DevTrackNotFoundException(trackId);
        }
        return row;
    }

    private static void requireCreator(DevTrack row, Long userId) {
        if (!row.getCreatedBy().equals(userId)) {
            throw new DevTrackForbiddenException("Only the person who created this task can change it.");
        }
    }

    private Map<Long, String> names(Set<Long> userIds) {
        Map<Long, String> names = new HashMap<>();
        if (userIds.isEmpty()) {
            return names;
        }
        for (AppUser user : users.findAllById(userIds)) {
            String display = user.getDisplayName();
            names.put(user.getUserId(), display == null || display.isBlank() ? user.getEmail() : display.trim());
        }
        return names;
    }

    private static String name(Map<Long, String> names, Long userId) {
        return names.getOrDefault(userId, "Unknown");
    }

    private static String clean(String value, int limit, String label) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > limit) {
            throw new DevTrackValidationException(label + " is too long");
        }
        return text;
    }

    private static String contentType(MultipartFile file) {
        String type = file.getContentType();
        return type == null || type.isBlank() ? "application/octet-stream" : type;
    }

    static String displayName(String filename) {
        String value = filename == null ? "file" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
            return "file";
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    static String storageKey(Long trackId, Long fileId, String originalFilename) {
        String safe = displayName(originalFilename).replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank() || safe.chars().allMatch(ch -> ch == '_' || ch == '.')) {
            safe = "file";
        }
        String prefix = "dev-track/" + trackId + "/" + fileId + "-";
        int room = 255 - prefix.length();
        if (room < 1) {
            room = 1;
        }
        if (safe.length() > room) {
            safe = safe.substring(0, room);
        }
        return prefix + safe;
    }
}
