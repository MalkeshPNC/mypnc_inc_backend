package com.pnc.masters.pcbinventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PcbInventoryNamesTest {

    @Test
    void uppercaseImportMatchesTitleCaseMaster() {
        assertEquals(PcbInventoryNames.key("CURRENT COMPOSITES"), PcbInventoryNames.key("Current Composites"));
        assertEquals(PcbInventoryNames.key("  isola   group "), PcbInventoryNames.key("Isola Group"));
        assertEquals(PcbInventoryNames.key("AGC"), PcbInventoryNames.key("Agc"));
    }
}
