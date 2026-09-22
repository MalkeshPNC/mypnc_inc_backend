package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompChatMessagePublisher implements ChatMessagePublisher {

    public static final String USER_QUEUE = "/queue/messages";

    private final SimpMessagingTemplate broker;

    public StompChatMessagePublisher(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @Override
    public void publish(Long userId, ChatMessageResponse message) {
        broker.convertAndSendToUser(String.valueOf(userId), USER_QUEUE, message);
    }
}
