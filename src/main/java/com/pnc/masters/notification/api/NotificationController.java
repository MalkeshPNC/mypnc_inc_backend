package com.pnc.masters.notification.api;

import com.pnc.masters.notification.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication authentication) {
        return notifications.list(userId(authentication));
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication authentication) {
        return notifications.unreadCount(userId(authentication));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id, Authentication authentication) {
        notifications.markRead(userId(authentication), id);
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
