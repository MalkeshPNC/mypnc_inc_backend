package com.pnc.masters.configuration.api;

public class ConfigurationKeyExistsException extends RuntimeException {

    public ConfigurationKeyExistsException(String key) {
        super("Configuration with key " + key + " already exists");
    }
}
