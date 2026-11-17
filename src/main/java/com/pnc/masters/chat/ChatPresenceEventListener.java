package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatPresenceEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class ChatPresenceEventListener {

    public static final String PRESENCE_TOPIC = "/topic/presence";

    private final ChatPresenceRegistry presence;
    private final SimpMessagingTemplate broker;

    public ChatPresenceEventListener(ChatPresenceRegistry presence, SimpMessagingTemplate broker) {
        this.presence = presence;
        this.broker = broker;
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        Long userId = userId(event.getUser());
        String sessionId = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (userId == null || sessionId == null) {
            return;
        }
        if (presence.connect(userId, sessionId)) {
            publish(userId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long userId = userId(event.getUser());
        String sessionId = event.getSessionId();
        if (userId == null || sessionId == null) {
            return;
        }
        if (presence.disconnect(userId, sessionId)) {
            publish(userId);
        }
    }

    public void publish(Long userId) {
        broker.convertAndSend(PRESENCE_TOPIC, new ChatPresenceEvent(userId, presence.status(userId), presence.lastSeenAt(userId)));
    }

    private static Long userId(Principal principal) {
        if (principal instanceof ChatPrincipal chat) {
            return chat.userId();
        }
        if (principal == null || principal.getName() == null) {
            return null;
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
