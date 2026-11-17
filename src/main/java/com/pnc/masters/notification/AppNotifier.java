package com.pnc.masters.notification;

import java.util.Collection;

/** Writes one in-app notification per user and pushes it on the live socket. */
public interface AppNotifier {

    void notifyUsers(Collection<Long> userIds, String eventType, String title, String body, String route);
}
