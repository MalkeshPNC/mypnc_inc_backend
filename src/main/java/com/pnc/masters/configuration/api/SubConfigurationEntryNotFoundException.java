package com.pnc.masters.configuration.api;

public class SubConfigurationEntryNotFoundException extends RuntimeException {

    public SubConfigurationEntryNotFoundException(Long entryId) {
        super("Sub configuration entry " + entryId + " was not found");
    }
}
