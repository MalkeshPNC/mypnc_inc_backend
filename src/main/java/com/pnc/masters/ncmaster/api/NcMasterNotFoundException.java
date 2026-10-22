package com.pnc.masters.ncmaster.api;

public class NcMasterNotFoundException extends RuntimeException {

    public NcMasterNotFoundException(Long id) {
        super("NC " + id + " was not found");
    }

    public NcMasterNotFoundException(String ncNumber) {
        super("NC " + ncNumber + " was not found");
    }
}
