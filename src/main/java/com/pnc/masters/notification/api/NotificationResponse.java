package com.pnc.masters.notification.api;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String eventType,
        String title,
        String body,
        String route,
        boolean read,
        LocalDateTime createdAt
) {
}
