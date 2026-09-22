package com.pnc.masters.chat.api;

public record ChatConversationResponse(
        Long conversationId,
        ChatPersonResponse peer,
        ChatMessageResponse lastMessage,
        long unreadCount
) {
}
