package com.pnc.masters.party.api;

public class PartyNotFoundException extends RuntimeException {

    public PartyNotFoundException(String message) {
        super(message);
    }
}
