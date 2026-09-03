package com.pnc.masters.configuration.api;

public class SubConfigurationTypeNotFoundException extends RuntimeException {

    public SubConfigurationTypeNotFoundException(Long typeId) {
        super("Sub configuration type " + typeId + " was not found");
    }
}
