package com.pnc.masters.notification;

import com.pnc.masters.notification.api.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional
public class NotificationService implements AppNotifier {

    static final int VISIBLE_DAYS = 3;
    static final int RETAIN_MONTHS = 1;

    private final NotificationRepository repository;
    private final NotificationDelivery delivery;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, NotificationDelivery delivery, Clock clock) {
        this.repository = repository;
        this.delivery = delivery;
        this.clock = clock;
    }

    @Override
    public void notifyUsers(Collection<Long> userIds, String eventType, String title, String body, String route) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long userId : new LinkedHashSet<>(userIds)) {
            if (userId == null) {
                continue;
            }
            Notification row = new Notification();
            row.setUserId(userId);
            row.setEventType(trim(eventType, 40));
            row.setTitle(trim(title, 200));
            row.setBody(trim(body, 500));
            row.setRoute(trim(route, 200));
            row.setCreatedAt(now);
            Notification saved = repository.save(row);
            delivery.push(userId, toResponse(saved));
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return repository
                .findTop50ByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userId, visibleSince())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNullAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                userId, visibleSince());
    }

    public void markRead(Long userId, Long notificationId) {
        repository.findByNotificationIdAndUserId(notificationId, userId).ifPresent(row -> {
            if (row.getReadAt() == null) {
                row.setReadAt(LocalDateTime.now(clock));
                repository.save(row);
            }
        });
    }

    /** Hide notices older than three days, then drop those older than a month. */
    public void purge() {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.softDeleteOlderThan(now.minusDays(VISIBLE_DAYS), now);
        repository.deleteCreatedBefore(now.minusMonths(RETAIN_MONTHS));
    }

    private LocalDateTime visibleSince() {
        return LocalDateTime.now(clock).minusDays(VISIBLE_DAYS);
    }

    private NotificationResponse toResponse(Notification row) {
        return new NotificationResponse(
                row.getNotificationId(),
                row.getEventType(),
                row.getTitle(),
                row.getBody(),
                row.getRoute(),
                row.getReadAt() != null,
                row.getCreatedAt()
        );
    }

    private static String trim(String value, int max) {
        String text = value == null ? "" : value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
