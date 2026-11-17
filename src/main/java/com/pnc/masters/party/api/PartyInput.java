package com.pnc.masters.party.api;

public interface PartyInput {
    String code();
    String name();
    Long typeEntryId();
    Long subtypeEntryId();
    String contactName();
    String contactAddress();
    String contactEmail();
    String contactPhone();
}
