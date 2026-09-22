package com.pnc.masters.chat;

import com.pnc.masters.chat.api.ChatMessageResponse;

public interface ChatMessagePublisher {

    void publish(Long userId, ChatMessageResponse message);
}
