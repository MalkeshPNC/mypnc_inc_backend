package com.pnc.masters.party;

public enum PartyList {
    VENDOR("vendortype", "vendorsubtype", "Vendor Type", "Vendor Sub Type", true),
    MANUFACTURER("manufacturertype", "manufacturersubtype", "Manufacturer Type", "Manufacturer Sub Type", false);

    private final String typeCode;
    private final String subtypeCode;
    private final String typeLabel;
    private final String subtypeLabel;
    private final boolean subtypeRequired;

    PartyList(String typeCode, String subtypeCode, String typeLabel, String subtypeLabel, boolean subtypeRequired) {
        this.typeCode = typeCode;
        this.subtypeCode = subtypeCode;
        this.typeLabel = typeLabel;
        this.subtypeLabel = subtypeLabel;
        this.subtypeRequired = subtypeRequired;
    }

    public String typeCode() { return typeCode; }
    public String subtypeCode() { return subtypeCode; }
    public String typeLabel() { return typeLabel; }
    public String subtypeLabel() { return subtypeLabel; }
    public boolean subtypeRequired() { return subtypeRequired; }
}
