package com.pnc.masters.devtrack.api;

public class DevTrackNotFoundException extends RuntimeException {

    public DevTrackNotFoundException(Long trackId) {
        super("Development track " + trackId + " was not found");
    }

    public DevTrackNotFoundException(String message) {
        super(message);
    }
}
