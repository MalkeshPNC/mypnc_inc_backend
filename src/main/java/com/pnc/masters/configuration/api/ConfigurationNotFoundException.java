package com.pnc.masters.configuration.api;

public class ConfigurationNotFoundException extends RuntimeException {

    public ConfigurationNotFoundException(String key) {
        super("Configuration with key " + key + " was not found");
    }
}
