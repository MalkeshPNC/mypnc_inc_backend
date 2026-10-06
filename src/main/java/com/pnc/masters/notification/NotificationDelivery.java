package com.pnc.masters.notification;

import com.pnc.masters.notification.api.NotificationResponse;

public interface NotificationDelivery {

    void push(Long userId, NotificationResponse notice);
}
