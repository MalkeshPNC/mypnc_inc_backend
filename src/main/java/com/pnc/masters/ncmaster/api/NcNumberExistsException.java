package com.pnc.masters.ncmaster.api;

public class NcNumberExistsException extends RuntimeException {

    public NcNumberExistsException(String ncNumber) {
        super("NC number " + ncNumber + " already exists");
    }
}
