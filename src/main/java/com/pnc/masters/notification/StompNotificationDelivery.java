package com.pnc.masters.notification;

import com.pnc.masters.notification.api.NotificationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompNotificationDelivery implements NotificationDelivery {

    static final String USER_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate broker;

    public StompNotificationDelivery(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @Override
    public void push(Long userId, NotificationResponse notice) {
        broker.convertAndSendToUser(String.valueOf(userId), USER_QUEUE, notice);
    }
}
