package com.pnc.masters.chat.api;

public class ChatRateLimitedException extends RuntimeException {

    public ChatRateLimitedException() {
        super("Please wait a moment before sending more messages.");
    }
}
