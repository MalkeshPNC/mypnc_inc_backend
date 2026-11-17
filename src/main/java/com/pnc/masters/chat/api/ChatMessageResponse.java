package com.pnc.masters.chat.api;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long conversationId,
        Long senderId,
        String body,
        LocalDateTime createdAt
) {
}
