package com.pnc.masters.party.api;

public class PartyCodeExistsException extends RuntimeException {

    public PartyCodeExistsException(String message) {
        super(message);
    }
}
