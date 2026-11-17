package com.pnc.masters.chat.api;

public class ChatForbiddenException extends RuntimeException {

    public ChatForbiddenException() {
        super("You are not part of this conversation.");
    }
}
