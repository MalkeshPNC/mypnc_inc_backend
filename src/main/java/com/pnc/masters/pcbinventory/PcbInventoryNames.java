package com.pnc.masters.pcbinventory;

import java.util.Locale;

final class PcbInventoryNames {

    private PcbInventoryNames() {
    }

    /**
     * Key for comparing a stored manufacturer or vendor name with the master
     * list. Stored names may be uppercase; master names are title case.
     * Extra spaces are ignored.
     */
    static String key(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
