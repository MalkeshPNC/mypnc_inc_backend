package com.pnc.masters.chat.api;

public class ChatConversationNotFoundException extends RuntimeException {

    public ChatConversationNotFoundException(Long id) {
        super("Chat conversation " + id + " was not found");
    }
}
