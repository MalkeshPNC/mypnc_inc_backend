package com.pnc.masters.quote.application;

import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.quote.Quote;
import com.pnc.masters.quote.QuoteComment;
import com.pnc.masters.quote.QuoteCommentImage;
import com.pnc.masters.quote.QuoteCommentImageRepository;
import com.pnc.masters.quote.QuoteCommentMention;
import com.pnc.masters.quote.QuoteCommentMentionRepository;
import com.pnc.masters.quote.QuoteCommentRepository;
import com.pnc.masters.quote.QuoteRepository;
import com.pnc.masters.quote.api.QuoteCommentImageResponse;
import com.pnc.masters.quote.api.QuoteCommentResponse;
import com.pnc.masters.quote.api.QuoteNotFoundException;
import com.pnc.masters.quote.api.QuoteValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class QuoteCommentService {

    private static final int MAX_BODY = 4000;
    private static final int MAX_IMAGES = 4;
    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp");

    private final QuoteRepository quotes;
    private final QuoteCommentRepository comments;
    private final QuoteCommentMentionRepository mentions;
    private final QuoteCommentImageRepository images;
    private final AppUserRepository users;
    private final DocumentStorage storage;
    private final AppNotifier notifier;
    private final QuoteCommentEvents events;

    public QuoteCommentService(QuoteRepository quotes,
                               QuoteCommentRepository comments,
                               QuoteCommentMentionRepository mentions,
                               QuoteCommentImageRepository images,
                               AppUserRepository users,
                               DocumentStorage storage,
                               AppNotifier notifier,
                               QuoteCommentEvents events) {
        this.quotes = quotes;
        this.comments = comments;
        this.mentions = mentions;
        this.images = images;
        this.users = users;
        this.storage = storage;
        this.notifier = notifier;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<QuoteCommentResponse> list(Long qid) {
        requireQuote(qid);
        List<QuoteComment> rows = comments.findByQidOrderByCreatedAtAsc(qid);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(QuoteComment::getCommentId).toList();
        Map<Long, List<Long>> mentioned = new LinkedHashMap<>();
        for (QuoteCommentMention mention : mentions.findByIdCommentIdIn(ids)) {
            mentioned.computeIfAbsent(mention.getId().getCommentId(), key -> new ArrayList<>())
                    .add(mention.getId().getUserId());
        }
        Map<Long, List<QuoteCommentImage>> pictures = new LinkedHashMap<>();
        for (QuoteCommentImage image : images.findByCommentIdIn(ids)) {
            pictures.computeIfAbsent(image.getCommentId(), key -> new ArrayList<>()).add(image);
        }
        Set<Long> authorIds = new LinkedHashSet<>();
        rows.forEach(row -> authorIds.add(row.getAuthorUserId()));
        Map<Long, String> names = new LinkedHashMap<>();
        users.findAllById(authorIds).forEach(user -> names.put(user.getUserId(), display(user)));
        return rows.stream()
                .map(row -> toResponse(row, names.getOrDefault(row.getAuthorUserId(), "Unknown"),
                        mentioned.getOrDefault(row.getCommentId(), List.of()),
                        pictures.getOrDefault(row.getCommentId(), List.of())))
                .toList();
    }

    public QuoteCommentResponse create(Long qid, Long authorId, String rawBody, Long replyToCommentId,
                                       Collection<Long> mentionUserIds, List<MultipartFile> files) {
        Quote quote = requireQuote(qid);
        String body = rawBody == null ? "" : rawBody.trim();
        if (body.length() > MAX_BODY) {
            throw new QuoteValidationException("A comment can be at most " + MAX_BODY + " characters.");
        }
        List<MultipartFile> uploads = files == null ? List.of() : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (body.isEmpty() && uploads.isEmpty()) {
            throw new QuoteValidationException("Write a comment or attach an image.");
        }
        if (uploads.size() > MAX_IMAGES) {
            throw new QuoteValidationException("A comment can include at most " + MAX_IMAGES + " images.");
        }
        uploads.forEach(this::validateImage);
        List<Long> tagged = validMentions(authorId, mentionUserIds);
        if (replyToCommentId != null) {
            QuoteComment parent = comments.findById(replyToCommentId)
                    .filter(row -> qid.equals(row.getQid()))
                    .orElseThrow(() -> new QuoteValidationException("That comment is not on this quote."));
            replyToCommentId = parent.getCommentId();
        }

        QuoteComment comment = new QuoteComment();
        comment.setQid(qid);
        comment.setAuthorUserId(authorId);
        comment.setBody(body);
        comment.setReplyToCommentId(replyToCommentId);
        comment.setCreatedAt(LocalDateTime.now());
        QuoteComment saved = comments.saveAndFlush(comment);
        for (Long userId : tagged) {
            mentions.save(new QuoteCommentMention(saved.getCommentId(), userId));
        }
        List<QuoteCommentImage> stored = storeImages(qid, saved.getCommentId(), uploads);
        AppUser author = users.findById(authorId).orElse(null);
        String authorName = author == null ? "Someone" : display(author);
        String quoteNumber = quote.getQuoteNumber() == null || quote.getQuoteNumber().isBlank()
                ? "quote " + qid
                : quote.getQuoteNumber();
        if (!tagged.isEmpty()) {
            notifier.notifyUsers(
                    tagged,
                    "QUOTE_COMMENT",
                    authorName + " mentioned you on " + quoteNumber,
                    body.isEmpty() ? "Shared an image" : body,
                    "/quote-systems/quotes/" + qid
            );
        }
        QuoteCommentResponse response = toResponse(saved, authorName, tagged, stored);
        events.published(qid, response);
        return response;
    }

    @Transactional(readOnly = true)
    public QuoteCommentImage image(Long qid, Long imageId) {
        return images.findByImageIdAndQid(imageId, qid)
                .orElseThrow(() -> new QuoteValidationException("That image was not found."));
    }

    private List<QuoteCommentImage> storeImages(Long qid, Long commentId, List<MultipartFile> uploads) {
        List<QuoteCommentImage> stored = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        try {
            for (MultipartFile file : uploads) {
                String filename = sanitize(file.getOriginalFilename());
                String key = "quote-comments/" + qid + "/" + UUID.randomUUID() + "-" + filename;
                try (InputStream input = file.getInputStream()) {
                    DocumentStorage.StoredDocument saved = storage.store(key, input);
                    keys.add(key);
                    QuoteCommentImage image = new QuoteCommentImage();
                    image.setCommentId(commentId);
                    image.setQid(qid);
                    image.setOriginalFilename(filename);
                    image.setContentType(file.getContentType().toLowerCase(Locale.ROOT));
                    image.setFileSize(saved.size());
                    image.setStorageKey(key);
                    stored.add(images.save(image));
                }
            }
            return stored;
        } catch (IOException | RuntimeException exception) {
            for (String key : keys) {
                try {
                    storage.delete(key);
                } catch (IOException cleanup) {
                    exception.addSuppressed(cleanup);
                }
            }
            if (exception instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unable to store the comment image", exception);
        }
    }

    private void validateImage(MultipartFile file) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(type)) {
            throw new QuoteValidationException("Only png, jpg, gif, and webp images can be shared.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new QuoteValidationException("Each image must be 8 MB or smaller.");
        }
    }

    private List<Long> validMentions(Long authorId, Collection<Long> mentionUserIds) {
        if (mentionUserIds == null || mentionUserIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> wanted = new LinkedHashSet<>();
        for (Long userId : mentionUserIds) {
            if (userId != null && !userId.equals(authorId)) {
                wanted.add(userId);
            }
        }
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<Long> found = users.findAllById(wanted).stream()
                .filter(AppUser::isEnabled)
                .map(AppUser::getUserId)
                .toList();
        if (found.size() != wanted.size()) {
            throw new QuoteValidationException("One of the tagged people could not be found.");
        }
        return found;
    }

    private Quote requireQuote(Long qid) {
        return quotes.findByQidAndIsDeletedFalse(qid).orElseThrow(() -> new QuoteNotFoundException(qid));
    }

    private QuoteCommentResponse toResponse(QuoteComment row, String authorName, List<Long> mentionIds,
                                            List<QuoteCommentImage> pictures) {
        List<QuoteCommentImageResponse> imageResponses = pictures.stream()
                .map(image -> new QuoteCommentImageResponse(image.getImageId(), image.getOriginalFilename(), image.getContentType()))
                .toList();
        return new QuoteCommentResponse(
                row.getCommentId(),
                row.getQid(),
                row.getAuthorUserId(),
                authorName,
                row.getBody(),
                row.getReplyToCommentId(),
                row.getCreatedAt(),
                mentionIds,
                imageResponses
        );
    }

    private static String display(AppUser user) {
        String name = user.getDisplayName();
        return name == null || name.isBlank() ? "Unknown" : name.trim();
    }

    private static String sanitize(String filename) {
        String name = filename == null ? "image" : filename.trim();
        name = name.replaceAll("[\\\\/]+", "");
        if (name.isBlank()) {
            return "image";
        }
        return name.length() > 80 ? name.substring(name.length() - 80) : name;
    }
}
