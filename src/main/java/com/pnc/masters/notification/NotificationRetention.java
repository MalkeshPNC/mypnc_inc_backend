package com.pnc.masters.notification;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetention {

    private final NotificationService notifications;

    public NotificationRetention(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Scheduled(cron = "0 20 2 * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void purge() {
        notifications.purge();
    }
}
