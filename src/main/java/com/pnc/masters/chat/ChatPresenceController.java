package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatPresenceUpdateRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatPresenceController {

    private final ChatPresenceRegistry presence;
    private final ChatPresenceEventListener events;

    public ChatPresenceController(ChatPresenceRegistry presence, ChatPresenceEventListener events) {
        this.presence = presence;
        this.events = events;
    }

    @MessageMapping("/presence")
    public void update(ChatPresenceUpdateRequest request, Principal principal) {
        Long userId = userId(principal);
        if (userId == null || request == null) {
            return;
        }
        if (presence.setAvailability(userId, request.status())) {
            events.publish(userId);
        }
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
