package com.pnc.masters.pcbinventory.api;

public class PcbInventoryNotFoundException extends RuntimeException {

    public PcbInventoryNotFoundException(Long id) {
        super("PCB inventory item " + id + " was not found.");
    }
}
