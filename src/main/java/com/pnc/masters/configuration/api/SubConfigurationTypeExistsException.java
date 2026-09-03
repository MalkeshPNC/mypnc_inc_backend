package com.pnc.masters.configuration.api;

public class SubConfigurationTypeExistsException extends RuntimeException {

    public SubConfigurationTypeExistsException(String typeCode) {
        super("Sub configuration type " + typeCode + " already exists");
    }
}
