package com.pnc.masters.chat.api;

public class ChatPeerNotFoundException extends RuntimeException {

    public ChatPeerNotFoundException() {
        super("That user is not available.");
    }
}
